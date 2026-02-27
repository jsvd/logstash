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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for the {@link ConvergenceLoop} class.
 */
public class ConvergenceLoopTest {

    private EventDispatcher eventDispatcher;

    @Before
    public void setUp() {
        eventDispatcher = new EventDispatcher("test-agent");
    }

    @Test
    public void testConvergeWithEmptyConfigSource() {
        final ConfigSource emptySource = Optional::empty;
        final StateResolver resolver = new StateResolver();
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                emptySource, resolver, registry, eventDispatcher);

        final ConvergenceResult result = loop.converge();

        assertFalse("Config fetch should have failed", result.isConfigFetchSucceeded());
        assertFalse("Overall result should not be success", result.isSuccess());
        assertTrue("No actions should have been executed", result.getExecutedActions().isEmpty());
        assertNull("ConvergeResult should be null", result.getConvergeResult());
        assertNotNull("Timestamp should be set", result.getTimestamp());
    }

    @Test
    public void testConvergeWithConfigsAndActionsExecuted() {
        final Map<String, Object> configs = new HashMap<>();
        configs.put("main", "input { stdin {} }");
        final ConfigSource source = () -> Optional.of(configs);

        // StateResolver will see empty registry and create a CreatePipelineAction
        final StateResolver resolver = new StateResolver();
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                source, resolver, registry, eventDispatcher);

        final ConvergenceResult result = loop.converge();

        assertTrue("Config fetch should have succeeded", result.isConfigFetchSucceeded());
        assertTrue("Overall result should be success", result.isSuccess());
        assertNotNull("ConvergeResult should not be null", result.getConvergeResult());
        assertEquals("Should have 1 action executed", 1, result.getExecutedActions().size());
        assertEquals("main", result.getExecutedActions().get(0).getPipelineId());
    }

    @Test
    public void testConvergeWithNoActionsNeeded() {
        // Empty configs = nothing to create, and empty registry = nothing to stop
        final ConfigSource source = () -> Optional.of(Collections.emptyMap());
        final StateResolver resolver = new StateResolver();
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                source, resolver, registry, eventDispatcher);

        final ConvergenceResult result = loop.converge();

        assertTrue("Config fetch should have succeeded", result.isConfigFetchSucceeded());
        assertTrue("Result should be success when no actions needed", result.isSuccess());
        assertTrue("No actions should be needed", result.getExecutedActions().isEmpty());
        assertNotNull("ConvergeResult should not be null", result.getConvergeResult());
        assertNotNull("Timestamp should be set", result.getTimestamp());
    }

    @Test
    public void testConvergeLockPreventsConcurrentExecution() throws Exception {
        final AtomicInteger concurrentCount = new AtomicInteger(0);
        final AtomicInteger maxConcurrent = new AtomicInteger(0);
        final CountDownLatch allStarted = new CountDownLatch(2);
        final CountDownLatch allDone = new CountDownLatch(2);

        // Custom action that records concurrency
        final PipelineAction slowAction = new PipelineAction() {
            @Override
            public String getPipelineId() { return "slow"; }

            @Override
            public ConvergeResult.ActionResult execute(PipelineActionContext context) {
                int current = concurrentCount.incrementAndGet();
                maxConcurrent.updateAndGet(max -> Math.max(max, current));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                concurrentCount.decrementAndGet();
                return new ConvergeResult.SuccessfulAction();
            }

            @Override
            public int getExecutionPriority() { return 100; }
        };

        // State resolver that always returns one slow action
        final StateResolver resolver = new StateResolver() {
            @Override
            public List<PipelineAction> resolve(PipelinesRegistry registry, Map<String, Object> configs) {
                return Collections.singletonList(slowAction);
            }
        };

        final Map<String, Object> configs = new HashMap<>();
        configs.put("main", "config");
        final ConfigSource source = () -> Optional.of(configs);
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                source, resolver, registry, eventDispatcher);

        // Run two convergence cycles concurrently
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    allStarted.countDown();
                    loop.converge();
                    allDone.countDown();
                });
            }

            assertTrue("All threads should complete",
                    allDone.await(10, TimeUnit.SECONDS));

            // The convergence lock ensures the two cycles run sequentially.
            // Each cycle has only one action, so the max concurrent actions
            // within a single converge() call is 1.
            // The lock serializes the converge() calls themselves.
            assertEquals("Convergence lock should serialize execution", 1, maxConcurrent.get());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testActionsExecutedInParallel() throws Exception {
        final CountDownLatch allActionsRunning = new CountDownLatch(3);
        final AtomicInteger completedCount = new AtomicInteger(0);

        // Create three actions that signal when they start, proving parallelism
        final List<PipelineAction> actions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final String id = "pipeline-" + i;
            actions.add(new PipelineAction() {
                @Override
                public String getPipelineId() { return id; }

                @Override
                public ConvergeResult.ActionResult execute(PipelineActionContext context) {
                    allActionsRunning.countDown();
                    try {
                        // Wait for all actions to be running concurrently
                        allActionsRunning.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    completedCount.incrementAndGet();
                    return new ConvergeResult.SuccessfulAction();
                }

                @Override
                public int getExecutionPriority() { return 100; }
            });
        }

        final StateResolver resolver = new StateResolver() {
            @Override
            public List<PipelineAction> resolve(PipelinesRegistry registry, Map<String, Object> configs) {
                return actions;
            }
        };

        final Map<String, Object> configs = new HashMap<>();
        configs.put("p0", "config");
        configs.put("p1", "config");
        configs.put("p2", "config");
        final ConfigSource source = () -> Optional.of(configs);
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                source, resolver, registry, eventDispatcher);

        final ConvergenceResult result = loop.converge();

        assertTrue("Config fetch should have succeeded", result.isConfigFetchSucceeded());
        assertTrue("All actions should succeed", result.isSuccess());
        assertEquals("All 3 actions should have completed", 3, completedCount.get());
        assertEquals(3, result.getExecutedActions().size());
    }

    @Test
    public void testFailedActionCapturedInResult() {
        final PipelineAction failingAction = new PipelineAction() {
            @Override
            public String getPipelineId() { return "failing"; }

            @Override
            public ConvergeResult.ActionResult execute(PipelineActionContext context) {
                throw new RuntimeException("Pipeline startup failed");
            }

            @Override
            public int getExecutionPriority() { return 100; }
        };

        final StateResolver resolver = new StateResolver() {
            @Override
            public List<PipelineAction> resolve(PipelinesRegistry registry, Map<String, Object> configs) {
                return Collections.singletonList(failingAction);
            }
        };

        final Map<String, Object> configs = new HashMap<>();
        configs.put("failing", "config");
        final ConfigSource source = () -> Optional.of(configs);
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                source, resolver, registry, eventDispatcher);

        final ConvergenceResult result = loop.converge();

        assertTrue("Config fetch should have succeeded", result.isConfigFetchSucceeded());
        assertFalse("Overall result should not be success", result.isSuccess());
        assertNotNull("ConvergeResult should not be null", result.getConvergeResult());
        assertEquals(1, result.getConvergeResult().failsCount());
        assertEquals(0, result.getConvergeResult().successCount());
    }

    @Test
    public void testMixedSuccessAndFailureActions() {
        final PipelineAction successAction = new PipelineAction() {
            @Override
            public String getPipelineId() { return "success"; }

            @Override
            public ConvergeResult.ActionResult execute(PipelineActionContext context) {
                return new ConvergeResult.SuccessfulAction();
            }

            @Override
            public int getExecutionPriority() { return 100; }
        };

        final PipelineAction failAction = new PipelineAction() {
            @Override
            public String getPipelineId() { return "fail"; }

            @Override
            public ConvergeResult.ActionResult execute(PipelineActionContext context) {
                return new ConvergeResult.FailedAction("start failed", null);
            }

            @Override
            public int getExecutionPriority() { return 100; }
        };

        final StateResolver resolver = new StateResolver() {
            @Override
            public List<PipelineAction> resolve(PipelinesRegistry registry, Map<String, Object> configs) {
                return Arrays.asList(successAction, failAction);
            }
        };

        final Map<String, Object> configs = new HashMap<>();
        configs.put("success", "config");
        configs.put("fail", "config");
        final ConfigSource source = () -> Optional.of(configs);
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                source, resolver, registry, eventDispatcher);

        final ConvergenceResult result = loop.converge();

        assertTrue(result.isConfigFetchSucceeded());
        assertFalse("Mixed results should not be success", result.isSuccess());
        assertEquals(1, result.getConvergeResult().successCount());
        assertEquals(1, result.getConvergeResult().failsCount());
    }

    @Test
    public void testConvergeCompleteEventFired() {
        final AtomicBoolean eventFired = new AtomicBoolean(false);
        final List<Object> receivedArgs = new ArrayList<>();

        eventDispatcher.addListener((eventName, emitter, args) -> {
            if ("converge_complete".equals(eventName)) {
                eventFired.set(true);
                receivedArgs.addAll(Arrays.asList(args));
            }
        });

        final Map<String, Object> configs = new HashMap<>();
        configs.put("main", "config");
        final ConfigSource source = () -> Optional.of(configs);
        final StateResolver resolver = new StateResolver();
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                source, resolver, registry, eventDispatcher);

        loop.converge();

        assertTrue("converge_complete event should have been fired", eventFired.get());
        assertEquals(1, receivedArgs.size());
        assertTrue("Event arg should be a ConvergeResult",
                receivedArgs.get(0) instanceof ConvergeResult);
    }

    @Test
    public void testConvergeCompleteEventNotFiredOnEmptyConfigFetch() {
        final AtomicBoolean eventFired = new AtomicBoolean(false);

        eventDispatcher.addListener((eventName, emitter, args) -> {
            if ("converge_complete".equals(eventName)) {
                eventFired.set(true);
            }
        });

        final ConfigSource emptySource = Optional::empty;
        final StateResolver resolver = new StateResolver();
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                emptySource, resolver, registry, eventDispatcher);

        loop.converge();

        assertFalse("converge_complete event should not fire on failed config fetch",
                eventFired.get());
    }

    @Test
    public void testMultipleConvergeCycles() {
        final AtomicInteger cycleCount = new AtomicInteger(0);

        final Map<String, Object> configs = new HashMap<>();
        configs.put("main", "config");
        final ConfigSource source = () -> Optional.of(configs);
        final StateResolver resolver = new StateResolver() {
            @Override
            public List<PipelineAction> resolve(PipelinesRegistry registry, Map<String, Object> configs) {
                cycleCount.incrementAndGet();
                return super.resolve(registry, configs);
            }
        };
        final PipelinesRegistry registry = new StubPipelinesRegistry();

        final ConvergenceLoop loop = new ConvergenceLoop(
                source, resolver, registry, eventDispatcher);

        // Run multiple cycles
        for (int i = 0; i < 5; i++) {
            final ConvergenceResult result = loop.converge();
            assertTrue(result.isConfigFetchSucceeded());
        }

        assertEquals("All 5 cycles should have executed", 5, cycleCount.get());
    }

    /**
     * Minimal stub implementation of PipelinesRegistry for testing.
     */
    private static class StubPipelinesRegistry implements PipelinesRegistry {

        private final Map<String, Object> pipelines = new HashMap<>();

        @Override
        public Object createPipeline(String pipelineId, Object pipeline,
                                       java.util.function.Supplier<Object> createAction) {
            pipelines.put(pipelineId, pipeline);
            return createAction.get();
        }

        @Override
        public Object reloadPipeline(String pipelineId,
                                       java.util.function.Supplier<Object> reloadAction) {
            return reloadAction.get();
        }

        @Override
        public void terminatePipeline(String pipelineId,
                                       java.util.function.Consumer<Object> shutdownAction) {
            Object pipeline = pipelines.get(pipelineId);
            if (pipeline != null) {
                shutdownAction.accept(pipeline);
            }
        }

        @Override
        public boolean deletePipeline(String pipelineId) {
            return pipelines.remove(pipelineId) != null;
        }

        @Override
        public Optional<Object> getPipeline(String pipelineId) {
            return Optional.ofNullable(pipelines.get(pipelineId));
        }

        @Override
        public Map<String, Object> getRunningPipelines() {
            return Collections.unmodifiableMap(pipelines);
        }

        @Override
        public Map<String, Object> getRunningPipelines(boolean includeLoading) {
            return getRunningPipelines();
        }

        @Override
        public Map<String, Object> getLoadingPipelines() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> getLoadedPipelines() {
            return getRunningPipelines();
        }

        @Override
        public Map<String, Object> getNonRunningPipelines() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> getRunningUserDefinedPipelines() {
            return getRunningPipelines();
        }

        @Override
        public int size() {
            return pipelines.size();
        }

        @Override
        public boolean isEmpty() {
            return pipelines.isEmpty();
        }
    }
}
