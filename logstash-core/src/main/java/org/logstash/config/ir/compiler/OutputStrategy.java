/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.logstash.config.ir.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.logstash.execution.ExecutionContextExt;
import org.logstash.plugins.factory.ContextualizerExt;

/**
 * Pure Java replacement for OutputStrategyExt. Contains the output strategy
 * hierarchy and a registry for mapping concurrency types to strategy factories.
 */
public final class OutputStrategy {

    private OutputStrategy() {
        // Just a holder for the nested classes
    }

    /**
     * Functional interface for creating output strategy instances.
     */
    @FunctionalInterface
    public interface StrategyFactory {
        AbstractOutputStrategy create(
                ThreadContext context,
                RubyClass outputClass,
                IRubyObject metric,
                ExecutionContextExt executionContext,
                RubyHash pluginArgs
        );
    }

    /**
     * Registry that maps concurrency type names (shared, single, legacy) to strategy factories.
     * Pure Java replacement for OutputStrategyExt.OutputStrategyRegistryExt.
     */
    public static final class OutputStrategyRegistry {

        private static volatile OutputStrategyRegistry instance;

        private final Map<String, StrategyFactory> factories;

        private OutputStrategyRegistry() {
            final Map<String, StrategyFactory> map = new HashMap<>();
            map.put("shared", SharedOutputStrategy::new);
            map.put("single", SingleOutputStrategy::new);
            map.put("legacy", LegacyOutputStrategy::new);
            this.factories = Collections.unmodifiableMap(map);
        }

        public static OutputStrategyRegistry instance() {
            if (instance == null) {
                synchronized (OutputStrategyRegistry.class) {
                    if (instance == null) {
                        instance = new OutputStrategyRegistry();
                    }
                }
            }
            return instance;
        }

        public AbstractOutputStrategy create(
                final ThreadContext context,
                final String concurrencyType,
                final RubyClass outputClass,
                final IRubyObject metric,
                final ExecutionContextExt executionContext,
                final RubyHash pluginArgs) {
            final StrategyFactory factory = factories.get(concurrencyType);
            if (factory == null) {
                throw new IllegalArgumentException(
                        String.format(
                                "Could not find output delegator strategy of type '%s'. Valid strategies: %s",
                                concurrencyType,
                                factories.keySet().stream().sorted().collect(Collectors.joining(", "))
                        )
                );
            }
            return factory.create(context, outputClass, metric, executionContext, pluginArgs);
        }
    }

    /**
     * Abstract base for all output strategies. Provides the output method callsite
     * caching and the public API for register/close/multiReceive.
     * Pure Java replacement for OutputStrategyExt.AbstractOutputStrategyExt.
     */
    public abstract static class AbstractOutputStrategy {

        private DynamicMethod outputMethod;
        private RubyClass outputClass;

        public final void register(final ThreadContext context) {
            reg(context);
        }

        public final void doClose(final ThreadContext context) {
            close(context);
        }

        public final IRubyObject multiReceive(final ThreadContext context, final IRubyObject events)
                throws InterruptedException {
            return output(context, events);
        }

        protected final void initOutputCallsite(final RubyClass outputClass) {
            outputMethod = outputClass.searchMethod(AbstractOutputDelegatorExt.OUTPUT_METHOD_NAME);
            this.outputClass = outputClass;
        }

        protected final void invokeOutput(final ThreadContext context, final IRubyObject batch,
                                           final IRubyObject pluginInstance) {
            outputMethod.call(
                    context, pluginInstance, outputClass, AbstractOutputDelegatorExt.OUTPUT_METHOD_NAME,
                    batch
            );
        }

        protected abstract IRubyObject output(ThreadContext context, IRubyObject events)
                throws InterruptedException;

        protected abstract void close(ThreadContext context);

        protected abstract void reg(ThreadContext context);
    }

    /**
     * Abstract strategy for outputs that use a single plugin instance.
     * Pure Java replacement for OutputStrategyExt.SimpleAbstractOutputStrategyExt.
     */
    public abstract static class SimpleAbstractOutputStrategy extends AbstractOutputStrategy {

        protected IRubyObject output;

        protected SimpleAbstractOutputStrategy(
                final ThreadContext context,
                final RubyClass outputClass,
                final IRubyObject metric,
                final ExecutionContextExt executionContext,
                final RubyHash pluginArgs) {
            output = ContextualizerExt.initializePlugin(context, executionContext, outputClass, pluginArgs);
            initOutputCallsite(outputClass);
            output.callMethod(context, "metric=", metric);
        }

        @Override
        protected final void close(final ThreadContext context) {
            output.callMethod(context, "do_close");
        }

        @Override
        protected final void reg(final ThreadContext context) {
            output.callMethod(context, "register");
        }

        /**
         * Returns the underlying output plugin instance.
         * Exposed for JRuby interop ({@code strategy.to_java.output}).
         */
        public IRubyObject getOutput() {
            return output;
        }

        protected final IRubyObject doOutput(final ThreadContext context, final IRubyObject events) {
            invokeOutput(context, events, output);
            return context.nil;
        }
    }

    /**
     * Single-threaded output strategy. Synchronizes all output calls.
     * Pure Java replacement for OutputStrategyExt.SingleOutputStrategyExt.
     */
    public static final class SingleOutputStrategy extends SimpleAbstractOutputStrategy {

        public SingleOutputStrategy(
                final ThreadContext context,
                final RubyClass outputClass,
                final IRubyObject metric,
                final ExecutionContextExt executionContext,
                final RubyHash pluginArgs) {
            super(context, outputClass, metric, executionContext, pluginArgs);
        }

        @Override
        protected IRubyObject output(final ThreadContext context, final IRubyObject events) {
            synchronized (this) {
                return doOutput(context, events);
            }
        }
    }

    /**
     * Shared output strategy. Does not synchronize output calls.
     * Pure Java replacement for OutputStrategyExt.SharedOutputStrategyExt.
     */
    public static final class SharedOutputStrategy extends SimpleAbstractOutputStrategy {

        public SharedOutputStrategy(
                final ThreadContext context,
                final RubyClass outputClass,
                final IRubyObject metric,
                final ExecutionContextExt executionContext,
                final RubyHash pluginArgs) {
            super(context, outputClass, metric, executionContext, pluginArgs);
        }

        @Override
        protected IRubyObject output(final ThreadContext context, final IRubyObject events) {
            return doOutput(context, events);
        }
    }

    /**
     * Legacy output strategy that supports multiple worker instances with a blocking queue.
     * Pure Java replacement for OutputStrategyExt.LegacyOutputStrategyExt.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final class LegacyOutputStrategy extends AbstractOutputStrategy {

        private final BlockingQueue<IRubyObject> workerQueue;
        private final IRubyObject workerCount;
        private final RubyArray workers;

        public LegacyOutputStrategy(
                final ThreadContext context,
                final RubyClass outputClass,
                final IRubyObject metric,
                final ExecutionContextExt executionContext,
                final RubyHash pluginArgs) {
            IRubyObject wc = pluginArgs.op_aref(context, context.runtime.newString("workers"));
            if (wc.isNil()) {
                wc = RubyFixnum.one(context.runtime);
            }
            workerCount = wc;
            final int count = workerCount.convertToInteger().getIntValue();
            workerQueue = new ArrayBlockingQueue<>(count);
            workers = context.runtime.newArray(count);
            for (int i = 0; i < count; ++i) {
                final IRubyObject output = ContextualizerExt.initializePlugin(context, executionContext, outputClass, pluginArgs);
                initOutputCallsite(outputClass);
                output.callMethod(context, "metric=", metric);
                workers.append(output);
                workerQueue.add(output);
            }
        }

        public IRubyObject workerCount() {
            return workerCount;
        }

        public RubyArray workers() {
            return workers;
        }

        @Override
        protected IRubyObject output(final ThreadContext context, final IRubyObject events)
                throws InterruptedException {
            final IRubyObject worker = workerQueue.take();
            try {
                invokeOutput(context, events, worker);
                return context.nil;
            } finally {
                workerQueue.put(worker);
            }
        }

        @Override
        protected void close(final ThreadContext context) {
            workers.forEach(worker -> ((IRubyObject) worker).callMethod(context, "do_close"));
        }

        @Override
        protected void reg(final ThreadContext context) {
            workers.forEach(worker -> ((IRubyObject) worker).callMethod(context, "register"));
        }
    }
}
