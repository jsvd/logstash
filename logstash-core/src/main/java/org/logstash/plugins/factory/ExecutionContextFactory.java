package org.logstash.plugins.factory;

import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.DeadLetterQueueWriter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.logstash.RubyUtil;
import org.logstash.common.AbstractDeadLetterQueueWriterExt;
import org.logstash.common.DLQWriterAdapter;
import org.logstash.common.NullDeadLetterQueueWriter;
import org.logstash.execution.ExecutionContextExt;
import org.logstash.instrument.metrics.AbstractNamespacedMetricExt;
import org.logstash.plugins.ContextImpl;
import org.logstash.plugins.NamespacedMetricImpl;
import org.logstash.plugins.PluginLookup;

/**
 * Pure Java factory for creating plugin execution contexts.
 * Replaces the former JRuby extension class {@code ExecutionContextFactoryExt}.
 */
public final class ExecutionContextFactory {

    private final IRubyObject agent;
    private final IRubyObject pipeline;
    private final IRubyObject dlqWriter;

    public ExecutionContextFactory(final IRubyObject agent,
                                   final IRubyObject pipeline,
                                   final IRubyObject dlqWriter) {
        // Normalize Java null to Ruby nil for safe downstream usage.
        // When called from JRuby, Ruby nil may arrive as Java null for plain Java constructors.
        this.agent = agent != null ? agent : RubyUtil.RUBY.getNil();
        this.pipeline = pipeline != null ? pipeline : RubyUtil.RUBY.getNil();
        this.dlqWriter = dlqWriter != null ? dlqWriter : RubyUtil.RUBY.getNil();
    }

    /**
     * Convenience overload that obtains the current thread context automatically.
     * Suitable for callers (including Ruby) that do not have a ThreadContext readily available.
     *
     * @param id             the plugin id
     * @param classConfigName the plugin's config_name
     * @return a new ExecutionContextExt
     */
    public ExecutionContextExt create(final IRubyObject id,
                                      final IRubyObject classConfigName) {
        return create(RubyUtil.RUBY.getCurrentContext(), id, classConfigName);
    }

    /**
     * Creates an {@link ExecutionContextExt} for a Ruby plugin instance, wrapping the
     * dead-letter-queue writer with plugin-specific metadata.
     *
     * @param context        the JRuby thread context
     * @param id             the plugin id
     * @param classConfigName the plugin's config_name
     * @return a new ExecutionContextExt
     */
    public ExecutionContextExt create(final ThreadContext context, final IRubyObject id,
                                      final IRubyObject classConfigName) {
        final AbstractDeadLetterQueueWriterExt.PluginDeadLetterQueueWriterExt dlqWriterForInstance =
                new AbstractDeadLetterQueueWriterExt.PluginDeadLetterQueueWriterExt(
                        context.runtime, RubyUtil.PLUGIN_DLQ_WRITER_CLASS
                ).initialize(context, dlqWriter, id, classConfigName);

        return new ExecutionContextExt(
                context.runtime, RubyUtil.EXECUTION_CONTEXT_CLASS
        ).initialize(
                context, new IRubyObject[]{pipeline, agent, dlqWriterForInstance}
        );
    }

    /**
     * Creates a pure Java {@link Context} for use by Java plugins.
     *
     * @param pluginType the type of plugin
     * @param metric     the namespaced metric root
     * @return a new Context
     */
    Context toContext(PluginLookup.PluginType pluginType, AbstractNamespacedMetricExt metric) {
        DeadLetterQueueWriter dlq = NullDeadLetterQueueWriter.getInstance();
        if (dlqWriter instanceof AbstractDeadLetterQueueWriterExt.PluginDeadLetterQueueWriterExt) {
            IRubyObject innerWriter =
                    ((AbstractDeadLetterQueueWriterExt.PluginDeadLetterQueueWriterExt) dlqWriter)
                            .innerWriter(RubyUtil.RUBY.getCurrentContext());
            if (innerWriter != null) {
                if (org.logstash.common.io.DeadLetterQueueWriter.class.isAssignableFrom(innerWriter.getJavaClass())) {
                    dlq = new DLQWriterAdapter(innerWriter.toJava(org.logstash.common.io.DeadLetterQueueWriter.class));
                }
            }
        } else if (!dlqWriter.isNil() && dlqWriter.getJavaClass().equals(DeadLetterQueueWriter.class)) {
            dlq = dlqWriter.toJava(DeadLetterQueueWriter.class);
        }

        return new ContextImpl(dlq, new NamespacedMetricImpl(RubyUtil.RUBY.getCurrentContext(), metric));
    }

    IRubyObject getPipeline() {
        return pipeline.isNil() ? null : pipeline;
    }
}
