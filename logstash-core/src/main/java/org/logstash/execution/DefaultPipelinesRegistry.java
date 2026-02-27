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

package org.logstash.execution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Thread-safe registry for managing pipeline lifecycle.
 * <p>
 * Uses per-pipeline locking to allow concurrent operations on different pipelines
 * while serializing operations on the same pipeline. Pipeline state queries
 * (terminated, running, etc.) call back into Ruby pipeline objects via JRuby.
 * </p>
 */
public class DefaultPipelinesRegistry implements PipelinesRegistry {

    private static final Logger LOGGER = LogManager.getLogger(DefaultPipelinesRegistry.class);

    private final PipelineStates states = new PipelineStates();

    /**
     * Represents the state of a single pipeline in the registry.
     * Thread-safe via internal monitor lock.
     */
    public static class PipelineState {
        private final String pipelineId;
        private volatile Object pipeline;
        private final AtomicBoolean loading = new AtomicBoolean(false);
        private final Object monitor = new Object();

        public PipelineState(String pipelineId, Object pipeline) {
            this.pipelineId = pipelineId;
            this.pipeline = pipeline;
        }

        public boolean isTerminated() {
            synchronized (monitor) {
                return !loading.get() && callBooleanMethod(pipeline, "finished_execution?");
            }
        }

        public boolean isFinished() {
            synchronized (monitor) {
                return !loading.get() && callBooleanMethod(pipeline, "finished_run?");
            }
        }

        public boolean isCrashed() {
            synchronized (monitor) {
                return pipeline != null && callBooleanMethod(pipeline, "crashed?");
            }
        }

        public boolean isRunning() {
            synchronized (monitor) {
                return !loading.get() && !callBooleanMethod(pipeline, "finished_execution?");
            }
        }

        public boolean isLoading() {
            synchronized (monitor) {
                return loading.get();
            }
        }

        public void setLoading(boolean isLoading) {
            synchronized (monitor) {
                loading.set(isLoading);
            }
        }

        public void setPipeline(Object pipeline) {
            synchronized (monitor) {
                if (pipeline == null) {
                    throw new IllegalArgumentException("invalid nil pipeline");
                }
                this.pipeline = pipeline;
            }
        }

        public Object getPipeline() {
            synchronized (monitor) {
                return pipeline;
            }
        }

        public String getPipelineId() {
            return pipelineId;
        }

        public Object synchronize(Function<Object, Object> block) {
            synchronized (monitor) {
                return block.apply(this);
            }
        }

        private static boolean callBooleanMethod(Object obj, String method) {
            if (obj == null) return false;
            if (obj instanceof IRubyObject) {
                IRubyObject rubyObj = (IRubyObject) obj;
                return rubyObj.callMethod(
                        rubyObj.getRuntime().getCurrentContext(), method).isTrue();
            }
            return false;
        }
    }

    /**
     * Thread-safe collection of pipeline states with per-pipeline locking.
     */
    public static class PipelineStates {
        private final Map<String, PipelineState> states = new LinkedHashMap<>();
        private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
        private final Object lock = new Object();

        public PipelineState get(String pipelineId) {
            synchronized (lock) {
                return states.get(pipelineId);
            }
        }

        public void put(String pipelineId, PipelineState state) {
            synchronized (lock) {
                states.put(pipelineId, state);
            }
        }

        public void remove(String pipelineId) {
            synchronized (lock) {
                states.remove(pipelineId);
            }
        }

        public int size() {
            synchronized (lock) {
                return states.size();
            }
        }

        public boolean isEmpty() {
            synchronized (lock) {
                return states.isEmpty();
            }
        }

        public ReentrantLock getLock(String pipelineId) {
            return locks.computeIfAbsent(pipelineId, k -> new ReentrantLock());
        }

        /**
         * Returns a snapshot of the current states for safe iteration.
         */
        public Map<String, PipelineState> snapshot() {
            synchronized (lock) {
                return new LinkedHashMap<>(states);
            }
        }
    }

    // ========== State accessor ==========

    public PipelineStates getStates() {
        return states;
    }

    // ========== Pipeline lifecycle ==========

    @Override
    public Object createPipeline(String pipelineId, Object pipeline, Supplier<Object> createAction) {
        ReentrantLock lock = states.getLock(pipelineId);
        lock.lock();
        try {
            Object success = null;
            PipelineState state = states.get(pipelineId);

            if (state != null && !state.isTerminated()) {
                LOGGER.error("Attempted to create a pipeline that already exists: {}", pipelineId);
                return false;
            }

            if (state == null) {
                state = new PipelineState(pipelineId, pipeline);
                state.setLoading(true);
                states.put(pipelineId, state);
                try {
                    success = createAction.get();
                } finally {
                    state.setLoading(false);
                    if (!isTruthy(success)) {
                        states.remove(pipelineId);
                    }
                }
            } else {
                // Existing terminated pipeline — reuse state
                state.setLoading(true);
                state.setPipeline(pipeline);
                try {
                    success = createAction.get();
                } finally {
                    state.setLoading(false);
                }
            }

            return success;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void terminatePipeline(String pipelineId, Consumer<Object> shutdownAction) {
        ReentrantLock lock = states.getLock(pipelineId);
        lock.lock();
        try {
            PipelineState state = states.get(pipelineId);
            if (state == null) {
                LOGGER.error("Attempted to terminate a pipeline that does not exists: {}", pipelineId);
            } else {
                shutdownAction.accept(state.getPipeline());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object reloadPipeline(String pipelineId, Supplier<Object> reloadAction) {
        ReentrantLock lock = states.getLock(pipelineId);
        lock.lock();
        try {
            Object success = false;
            PipelineState state = states.get(pipelineId);

            if (state == null) {
                LOGGER.error("Attempted to reload a pipeline that does not exists: {}", pipelineId);
                return false;
            }

            state.setLoading(true);
            try {
                Object result = reloadAction.get();
                // Ruby blocks return [success, new_pipeline] tuple
                if (result instanceof List && ((List<?>) result).size() >= 2) {
                    List<?> tuple = (List<?>) result;
                    success = tuple.get(0);
                    Object newPipeline = tuple.get(1);
                    if (newPipeline != null) {
                        state.setPipeline(newPipeline);
                    }
                } else {
                    success = result;
                }
            } finally {
                state.setLoading(false);
            }

            return success;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean deletePipeline(String pipelineId) {
        ReentrantLock lock = states.getLock(pipelineId);
        lock.lock();
        try {
            PipelineState state = states.get(pipelineId);

            if (state == null) {
                LOGGER.error("Attempted to delete a pipeline that does not exists: {}", pipelineId);
                return false;
            }

            if (state.isTerminated()) {
                states.remove(pipelineId);
                LOGGER.info("Removed pipeline from registry successfully: {}", pipelineId);
                return true;
            } else {
                LOGGER.info("Attempted to delete a pipeline that is not terminated: {}", pipelineId);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    // ========== Query methods ==========

    @Override
    public Optional<Object> getPipeline(String pipelineId) {
        PipelineState state = states.get(pipelineId);
        return Optional.ofNullable(state != null ? state.getPipeline() : null);
    }

    /**
     * Gets a pipeline by ID, returning null if not found.
     * This method is called from Ruby as get_pipeline.
     */
    public Object getPipelineOrNull(String pipelineId) {
        PipelineState state = states.get(pipelineId);
        return state != null ? state.getPipeline() : null;
    }

    @Override
    public int size() {
        return states.size();
    }

    @Override
    public boolean isEmpty() {
        return states.isEmpty();
    }

    @Override
    public Map<String, Object> getRunningPipelines() {
        return getRunningPipelines(false);
    }

    @Override
    public Map<String, Object> getRunningPipelines(boolean includeLoading) {
        return selectPipelines(state ->
                state.isRunning() || (includeLoading && state.isLoading()));
    }

    @Override
    public Map<String, Object> getLoadingPipelines() {
        return selectPipelines(PipelineState::isLoading);
    }

    @Override
    public Map<String, Object> getLoadedPipelines() {
        return selectPipelines(state -> !state.isLoading());
    }

    @Override
    public Map<String, Object> getNonRunningPipelines() {
        return selectPipelines(PipelineState::isTerminated);
    }

    @Override
    public Map<String, Object> getRunningUserDefinedPipelines() {
        return selectPipelines(state ->
                !state.isTerminated() && !isSystemPipeline(state.getPipeline()));
    }

    // ========== Internal helpers ==========

    private Map<String, Object> selectPipelines(java.util.function.Predicate<PipelineState> filter) {
        Map<String, PipelineState> snapshot = states.snapshot();
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, PipelineState> entry : snapshot.entrySet()) {
            PipelineState state = entry.getValue();
            if (state != null) {
                synchronized (state.monitor) {
                    if (filter.test(state)) {
                        result.put(entry.getKey(), state.getPipeline());
                    }
                }
            }
        }
        return result;
    }

    private static boolean isSystemPipeline(Object pipeline) {
        if (pipeline == null) return false;
        if (pipeline instanceof IRubyObject) {
            return ((IRubyObject) pipeline).callMethod(
                    ((IRubyObject) pipeline).getRuntime().getCurrentContext(), "system?").isTrue();
        }
        return false;
    }

    private static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return true;
    }
}
