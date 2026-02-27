package org.logstash.plugins.factory;

import co.elastic.logstash.api.*;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.logstash.RubyUtil;
import org.logstash.common.EnvironmentVariableProvider;
import org.logstash.common.SourceWithMetadata;
import org.logstash.exceptions.ConfigurationException;
import org.logstash.config.ir.PipelineIR;
import org.logstash.config.ir.compiler.*;
import org.logstash.config.ir.graph.Vertex;
import org.logstash.execution.ExecutionContextExt;
import org.logstash.instrument.metrics.AbstractMetricExt;
import org.logstash.instrument.metrics.AbstractNamespacedMetricExt;
import org.logstash.instrument.metrics.MetricKeys;
import org.logstash.plugins.ConfigVariableExpander;
import org.logstash.plugins.PluginLookup;
import org.logstash.plugins.discovery.PluginRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JRuby extension to implement the factory that create plugin instances
 * */
@JRubyClass(name = "PluginFactory")
public final class PluginFactoryExt extends RubyBasicObject
    implements RubyIntegration.PluginFactory, PluginFactory {

    /**
     * Definition of plugin resolver, maps plugin type and name to the plugin's class.
     * */
    @FunctionalInterface
    public interface PluginResolver {
        PluginLookup.PluginClass resolve(PluginLookup.PluginType type, String name);
    }

    private static final long serialVersionUID = 1L;

    private static final RubyString ID_KEY = RubyUtil.RUBY.newString("id");

    private final transient Collection<String> pluginsById = ConcurrentHashMap.newKeySet();

    private transient PipelineIR lir;

    private transient ExecutionContextFactory executionContextFactory;

    private PluginMetricsFactoryExt metrics;

    private RubyClass filterDelegatorClass;

    private transient ConfigVariableExpander configVariables;

    private transient PluginResolver pluginResolver;

    private final transient Map<PluginLookup.PluginType, AbstractPluginCreator<? extends Plugin>> pluginCreatorsRegistry = new HashMap<>(4);

    @JRubyMethod(name = "filter_delegator", meta = true, required = 5)
    public static IRubyObject filterDelegator(final ThreadContext context,
                                              final IRubyObject recv, final IRubyObject... args) {
        //  filterDelegatorClass, klass, rubyArgs, typeScopedMetric, executionCntx
        final RubyClass filterDelegatorClass = (RubyClass) args[0];
        final RubyClass klass = (RubyClass) args[1];
        final RubyHash arguments = (RubyHash) args[2];
        final AbstractMetricExt typeScopedMetric = (AbstractMetricExt) args[3];
        final ExecutionContextExt executionContext = (ExecutionContextExt) args[4];

        final IRubyObject filterInstance = ContextualizerExt.initializePlugin(context, executionContext, klass, arguments);

        final RubyString id = (RubyString) arguments.op_aref(context, ID_KEY);
        filterInstance.callMethod(
                context, "metric=",
                typeScopedMetric.namespace(context, id.intern())
        );

        return filterDelegatorClass.newInstance(context, filterInstance, id, Block.NULL_BLOCK);
    }

    public PluginFactoryExt(final Ruby runtime, final RubyClass metaClass) {
        this(runtime, metaClass, new PluginLookup(PluginRegistry.getInstance()));
    }

    PluginFactoryExt(final Ruby runtime, final RubyClass metaClass, PluginResolver pluginResolver) {
        super(runtime, metaClass);
        this.pluginResolver = pluginResolver;
    }

    public PluginFactoryExt init(final PipelineIR lir,
                                     final PluginMetricsFactoryExt metrics,
                                     final ExecutionContextFactory executionContextFactory,
                                     final RubyClass filterClass) {
        return this.init(lir, metrics, executionContextFactory, filterClass, EnvironmentVariableProvider.defaultProvider());
    }

    PluginFactoryExt init(final PipelineIR lir,
                          final PluginMetricsFactoryExt metrics,
                          final ExecutionContextFactory executionContextFactory,
                          final RubyClass filterClass,
                          final EnvironmentVariableProvider envVars) {
        this.lir = lir;
        this.metrics = metrics;
        this.executionContextFactory = executionContextFactory;
        this.filterDelegatorClass = filterClass;
        this.pluginCreatorsRegistry.put(PluginLookup.PluginType.INPUT, new InputPluginCreator(this));
        this.pluginCreatorsRegistry.put(PluginLookup.PluginType.CODEC, new CodecPluginCreator());
        this.pluginCreatorsRegistry.put(PluginLookup.PluginType.FILTER, new FilterPluginCreator());
        this.pluginCreatorsRegistry.put(PluginLookup.PluginType.OUTPUT, new OutputPluginCreator(this));
        this.configVariables = ConfigVariableExpander.withoutSecret(envVars);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IRubyObject buildInput(final RubyString name,
                                  final IRubyObject args,
                                  final SourceWithMetadata source) {
        return plugin(
                RubyUtil.RUBY.getCurrentContext(),
                PluginLookup.PluginType.INPUT,
                name.asJavaString(),
                (RubyHash) args,
                source
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public AbstractOutputDelegatorExt buildOutput(final RubyString name,
                                                  final IRubyObject args,
                                                  final SourceWithMetadata source) {
        return (AbstractOutputDelegatorExt) plugin(
                RubyUtil.RUBY.getCurrentContext(), PluginLookup.PluginType.OUTPUT, name.asJavaString(),
                (RubyHash) args, source
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public AbstractFilterDelegatorExt buildFilter(final RubyString name,
                                                  final IRubyObject args,
                                                  final SourceWithMetadata source) {
        return (AbstractFilterDelegatorExt) plugin(
                RubyUtil.RUBY.getCurrentContext(), PluginLookup.PluginType.FILTER, name.asJavaString(),
                (RubyHash) args, source
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public IRubyObject buildCodec(final RubyString name,
                                  final IRubyObject args,
                                  final SourceWithMetadata source) {
        return plugin(
                RubyUtil.RUBY.getCurrentContext(),
                PluginLookup.PluginType.CODEC,
                name.asJavaString(),
                (RubyHash) args,
                source
        );
    }

    @Override
    public Codec buildDefaultCodec(String codecName) {
        final IRubyObject pluginInstance = plugin(
                RubyUtil.RUBY.getCurrentContext(),
                PluginLookup.PluginType.CODEC,
                codecName,
                RubyHash.newHash(RubyUtil.RUBY),
                null
        );
        final Codec codec = (Codec) JavaUtil.unwrapJavaValue(pluginInstance);
        if (codec != null) {
            return codec;
        }

        // no unwrap is possible so this is a real Ruby instance
        return new RubyCodecDelegator(RubyUtil.RUBY.getCurrentContext(), pluginInstance);
    }

    @Override
    public Codec buildRubyCodecWrapper(RubyObject rubyCodec) {
        return new RubyCodecDelegator(RubyUtil.RUBY.getCurrentContext(), rubyCodec);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(required = 3, optional = 1)
    public IRubyObject plugin(final ThreadContext context, final IRubyObject[] args) {
        final SourceWithMetadata source = args.length > 3 ? (SourceWithMetadata) JavaUtil.unwrapIfJavaObject(args[3]) : null;

        return plugin(
                context,
                PluginLookup.PluginType.valueOf(args[0].asJavaString().toUpperCase(Locale.ENGLISH)),
                args[1].asJavaString(),
                (RubyHash) args[2],
                source
        );
    }

    @SuppressWarnings("unchecked")
    private IRubyObject plugin(final ThreadContext context,
                               final PluginLookup.PluginType type,
                               final String name,
                               final RubyHash args,
                               final SourceWithMetadata source) {
        final String id = generateOrRetrievePluginId(type, source, args);

        if (id == null) {
            throw new ConfigurationException(
                    String.format(
                            "Could not determine ID for %s/%s", type.rubyLabel().asJavaString(), name
                    )
            );
        }
        if (!pluginsById.add(id)) {
            throw new ConfigurationException(
                    String.format("Two plugins have the id '%s', please fix this conflict", id)
            );
        }

        final AbstractNamespacedMetricExt typeScopedMetric = metrics.create(context, type.rubyLabel());

        final PluginLookup.PluginClass pluginClass = pluginResolver.resolve(type, name);
        if (pluginClass.language() == PluginLookup.PluginLanguage.RUBY) {

            final Map<String, Object> newArgs = new HashMap<>(args);
            newArgs.put("id", id);
            final RubyClass klass = (RubyClass) pluginClass.klass();
            final ExecutionContextExt executionCntx = executionContextFactory.create(
                    context, RubyUtil.RUBY.newString(id), klass.callMethod(context, "config_name")
            );
            final RubyHash rubyArgs = RubyHash.newHash(context.runtime);
            rubyArgs.putAll(newArgs);
            if (type == PluginLookup.PluginType.OUTPUT) {
                return new OutputDelegatorExt(context.runtime, RubyUtil.RUBY_OUTPUT_DELEGATOR_CLASS).initialize(
                        context,
                        rubyArgs,
                        klass,
                        typeScopedMetric,
                        executionCntx
                );
            } else if (type == PluginLookup.PluginType.FILTER) {
                return filterDelegator(
                        context, null,
                        filterDelegatorClass, klass, rubyArgs, typeScopedMetric, executionCntx);
            } else {
                final IRubyObject pluginInstance = ContextualizerExt.initializePlugin(context, executionCntx, klass, rubyArgs);

                final AbstractNamespacedMetricExt scopedMetric = typeScopedMetric.namespace(context, RubyUtil.RUBY.newString(id).intern());
                scopedMetric.gauge(context, MetricKeys.NAME_KEY, pluginInstance.callMethod(context, "config_name"));
                pluginInstance.callMethod(context, "metric=", scopedMetric);
                return pluginInstance;
            }
        } else {
            AbstractPluginCreator<? extends Plugin> pluginCreator = pluginCreatorsRegistry.get(type);
            if (pluginCreator == null) {
                throw new IllegalStateException("Unable to create plugin: " + pluginClass.toReadableString());
            }

            Context contextWithMetrics = executionContextFactory.toContext(type, metrics.getRoot(context));
            return pluginCreator.createDelegator(name, convertToJavaCoercible(args), id, typeScopedMetric, pluginClass, contextWithMetrics);
        }
    }

    private Map<String, Object> convertToJavaCoercible(Map<String, Object> input) {
        final Map<String, Object> output = new HashMap<>(input);

        // Intercept Codecs
        for (final Map.Entry<String, Object> entry : input.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof IRubyObject) {
                final Object unwrapped = JavaUtil.unwrapJavaValue((IRubyObject) value);
                if (unwrapped instanceof Codec) {
                    output.put(key, unwrapped);
                }
            }
        }

        return output;
    }

    // TODO: caller seems to think that the args is `Map<String, IRubyObject>`, but
    //       at least any `id` present is actually a `String`.
    private String generateOrRetrievePluginId(final PluginLookup.PluginType type,
                                              final SourceWithMetadata source,
                                              final Map<String, ?> args) {
        final Optional<String> unprocessedId;
        if (source == null) {
            unprocessedId = extractId(() -> extractIdFromArgs(args),
                                      this::generateUUID);
        } else {
            unprocessedId = extractId(() -> extractIdFromLIR(source),
                                      () -> extractIdFromArgs(args),
                                      () -> generateUUIDForCodecs(type));
        }

        return unprocessedId
                .map(configVariables::expand)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse(null);
    }

    private Optional<String> extractId(final IdExtractor... extractors) {
        for (IdExtractor extractor : extractors) {
            final Optional<String> extracted = extractor.extract();
            if (extracted.isPresent()) {
                return extracted;
            }
        }
        return Optional.empty();
    }

    @FunctionalInterface
    interface IdExtractor {
        Optional<String> extract();
    }

    private Optional<String> extractIdFromArgs(final Map<String, ?> args) {
        if (!args.containsKey("id")) {
            return Optional.empty();
        }

        final Object explicitId = args.get("id");
        if (explicitId instanceof String) {
            return Optional.of((String) explicitId);
        } else if (explicitId instanceof RubyString) {
            return Optional.of(((RubyString) explicitId).asJavaString());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> generateUUID() {
        return Optional.of(UUID.randomUUID().toString());
    }

    private Optional<String> generateUUIDForCodecs(final PluginLookup.PluginType pluginType) {
        if (pluginType == PluginLookup.PluginType.CODEC) {
            return generateUUID();
        }
        return Optional.empty();
    }

    private Optional<String> extractIdFromLIR(final SourceWithMetadata source) {
        return lir.getGraph().vertices()
                .filter(v -> v.getSourceWithMetadata() != null
                        && v.getSourceWithMetadata().equalsWithoutText(source))
                .findFirst()
                .map(Vertex::getId);
    }

    ExecutionContextFactory getExecutionContextFactory() {
        return executionContextFactory;
    }

    // ---- PluginFactory (pure Java) interface implementation ----

    @Override
    public Input buildJavaInput(final String name, final String id,
                                final Map<String, Object> args, final SourceWithMetadata source) {
        return buildJavaPlugin(PluginLookup.PluginType.INPUT, name, id, args, source, Input.class);
    }

    @Override
    public Output buildJavaOutput(final String name, final String id,
                                  final Map<String, Object> args, final SourceWithMetadata source) {
        return buildJavaPlugin(PluginLookup.PluginType.OUTPUT, name, id, args, source, Output.class);
    }

    @Override
    public Filter buildJavaFilter(final String name, final String id,
                                  final Map<String, Object> args, final SourceWithMetadata source) {
        return buildJavaPlugin(PluginLookup.PluginType.FILTER, name, id, args, source, Filter.class);
    }

    @Override
    public Codec buildJavaCodec(final String name, final String id,
                                final Map<String, Object> args, final SourceWithMetadata source) {
        return buildJavaPlugin(PluginLookup.PluginType.CODEC, name, id, args, source, Codec.class);
    }

    /**
     * Returns this instance as a pure Java {@link PluginFactory}.
     * Since PluginFactoryExt implements PluginFactory directly, this simply returns {@code this}.
     *
     * @return this instance
     */
    public PluginFactory asPluginFactory() {
        return this;
    }

    /**
     * Creates a raw Java plugin instance without JRuby delegator wrapping.
     * This method resolves the plugin class, validates it is a Java plugin,
     * and instantiates it using the plugin creator infrastructure.
     *
     * @param type        the plugin type
     * @param name        the plugin name
     * @param id          the unique plugin id
     * @param args        configuration arguments
     * @param source      source metadata, may be {@code null}
     * @param pluginClass the expected Java plugin interface class
     * @param <T>         the plugin type
     * @return the raw Java plugin instance
     * @throws IllegalArgumentException if the resolved plugin is not a Java plugin
     * @throws IllegalStateException    if no plugin creator is registered for the type
     */
    @SuppressWarnings("unchecked")
    private <T extends Plugin> T buildJavaPlugin(final PluginLookup.PluginType type,
                                                  final String name,
                                                  final String id,
                                                  final Map<String, Object> args,
                                                  final SourceWithMetadata source,
                                                  final Class<T> pluginClass) {
        final PluginLookup.PluginClass resolvedClass = pluginResolver.resolve(type, name);
        if (resolvedClass.language() != PluginLookup.PluginLanguage.JAVA) {
            throw new IllegalArgumentException(
                    String.format("Plugin %s/%s is not a Java plugin. Use the Ruby-aware build methods instead.", type, name)
            );
        }

        final AbstractPluginCreator<? extends Plugin> pluginCreator = pluginCreatorsRegistry.get(type);
        if (pluginCreator == null) {
            throw new IllegalStateException("Unable to create plugin: " + resolvedClass.toReadableString());
        }

        final Map<String, Object> pluginArgs = new HashMap<>(args);
        pluginArgs.put("id", id);

        final ThreadContext context = RubyUtil.RUBY.getCurrentContext();
        final Context contextWithMetrics = executionContextFactory.toContext(type, metrics.getRoot(context));

        return (T) pluginCreator.createInstance(pluginArgs, id, contextWithMetrics, resolvedClass);
    }
}
