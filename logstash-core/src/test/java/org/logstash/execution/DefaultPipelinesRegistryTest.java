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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class DefaultPipelinesRegistryTest {

    private DefaultPipelinesRegistry registry;

    @Before
    public void setUp() {
        registry = new DefaultPipelinesRegistry();
    }

    // ========== Initial state ==========

    @Test
    public void initialStateIsEmpty() {
        assertEquals(0, registry.size());
        assertTrue(registry.isEmpty());
        assertTrue(registry.getRunningPipelines().isEmpty());
        assertTrue(registry.getNonRunningPipelines().isEmpty());
        assertTrue(registry.getRunningUserDefinedPipelines().isEmpty());
    }

    // ========== createPipeline ==========

    @Test
    public void createPipelineReturnsBlockValue() {
        Object result = registry.createPipeline("test", "pipeline", () -> "dummy");
        assertEquals("dummy", result);
    }

    @Test
    public void createPipelineRegistersOnSuccess() {
        registry.createPipeline("test", "pipeline", () -> true);
        assertEquals("pipeline", registry.getPipelineOrNull("test"));
    }

    @Test
    public void createPipelineDoesNotRegisterOnFailure() {
        registry.createPipeline("test", "pipeline", () -> false);
        assertNull(registry.getPipelineOrNull("test"));
    }

    @Test
    public void createPipelineDoesNotRegisterOnNullReturn() {
        registry.createPipeline("test", "pipeline", () -> null);
        assertNull(registry.getPipelineOrNull("test"));
    }

    @Test
    public void createPipelineRemovesStateOnException() {
        try {
            registry.createPipeline("test", "pipeline", () -> {
                throw new RuntimeException("fail");
            });
        } catch (RuntimeException e) {
            // expected
        }
        assertNull(registry.getPipelineOrNull("test"));
    }

    // ========== terminatePipeline ==========

    @Test
    public void terminatePipelineCallsBlockWithPipeline() {
        registry.createPipeline("test", "pipeline", () -> true);
        AtomicReference<Object> received = new AtomicReference<>();
        registry.terminatePipeline("test", received::set);
        assertEquals("pipeline", received.get());
    }

    @Test
    public void terminatePipelineDoesNotCallBlockForMissing() {
        AtomicBoolean called = new AtomicBoolean(false);
        registry.terminatePipeline("nonexistent", p -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void terminatePipelineKeepsPipelineInRegistry() {
        registry.createPipeline("test", "pipeline", () -> true);
        registry.terminatePipeline("test", p -> {});
        assertEquals("pipeline", registry.getPipelineOrNull("test"));
    }

    // ========== reloadPipeline ==========

    @Test
    public void reloadPipelineReturnsSuccessValue() {
        registry.createPipeline("test", "pipeline", () -> true);
        Object result = registry.reloadPipeline("test", () -> Arrays.asList("dummy", "new_pipeline"));
        assertEquals("dummy", result);
    }

    @Test
    public void reloadPipelineUpdatesPipeline() {
        registry.createPipeline("test", "old_pipeline", () -> true);
        registry.reloadPipeline("test", () -> Arrays.asList(true, "new_pipeline"));
        assertEquals("new_pipeline", registry.getPipelineOrNull("test"));
    }

    @Test
    public void reloadPipelineReturnsFalseForMissing() {
        Object result = registry.reloadPipeline("nonexistent", () -> Arrays.asList(true, "pipeline"));
        assertEquals(false, result);
    }

    // ========== deletePipeline ==========

    @Test
    public void deletePipelineReturnsFalseForMissing() {
        assertFalse(registry.deletePipeline("nonexistent"));
    }

    // ========== PipelineState ==========

    @Test
    public void pipelineStateLoadingFlag() {
        registry.createPipeline("test", "pipeline", () -> true);
        DefaultPipelinesRegistry.PipelineState state = registry.getStates().get("test");
        assertNotNull(state);
        assertFalse(state.isLoading());
    }

    @Test
    public void pipelineStateSetPipelineRejectsNull() {
        DefaultPipelinesRegistry.PipelineState state =
                new DefaultPipelinesRegistry.PipelineState("test", "pipeline");
        try {
            state.setPipeline(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nil"));
        }
    }

    @Test
    public void pipelineStateGetPipeline() {
        DefaultPipelinesRegistry.PipelineState state =
                new DefaultPipelinesRegistry.PipelineState("test", "pipeline");
        assertEquals("pipeline", state.getPipeline());
    }

    @Test
    public void pipelineStatePipelineId() {
        DefaultPipelinesRegistry.PipelineState state =
                new DefaultPipelinesRegistry.PipelineState("test", "pipeline");
        assertEquals("test", state.getPipelineId());
    }

    // ========== Concurrency ==========

    @Test
    public void createPipelineSetsLoadingDuringBlock() throws Exception {
        CountDownLatch enteredBlock = new CountDownLatch(1);
        CountDownLatch exitBlock = new CountDownLatch(1);

        Thread t = new Thread(() -> {
            registry.createPipeline("test", "pipeline", () -> {
                enteredBlock.countDown();
                try {
                    exitBlock.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            });
        });
        t.start();

        assertTrue(enteredBlock.await(10, TimeUnit.SECONDS));

        // Pipeline should be in loading state
        DefaultPipelinesRegistry.PipelineState state = registry.getStates().get("test");
        assertNotNull(state);
        assertTrue(state.isLoading());
        assertEquals("pipeline", state.getPipeline());

        exitBlock.countDown();
        t.join(10000);

        assertFalse(state.isLoading());
    }

    // ========== PipelineStates ==========

    @Test
    public void pipelineStatesGetPutRemove() {
        DefaultPipelinesRegistry.PipelineStates ps = new DefaultPipelinesRegistry.PipelineStates();
        assertNull(ps.get("test"));
        assertTrue(ps.isEmpty());

        DefaultPipelinesRegistry.PipelineState state =
                new DefaultPipelinesRegistry.PipelineState("test", "pipeline");
        ps.put("test", state);

        assertSame(state, ps.get("test"));
        assertEquals(1, ps.size());
        assertFalse(ps.isEmpty());

        ps.remove("test");
        assertNull(ps.get("test"));
        assertTrue(ps.isEmpty());
    }

    @Test
    public void pipelineStatesSnapshot() {
        DefaultPipelinesRegistry.PipelineStates ps = new DefaultPipelinesRegistry.PipelineStates();
        ps.put("a", new DefaultPipelinesRegistry.PipelineState("a", "pipelineA"));
        ps.put("b", new DefaultPipelinesRegistry.PipelineState("b", "pipelineB"));

        Map<String, DefaultPipelinesRegistry.PipelineState> snapshot = ps.snapshot();
        assertEquals(2, snapshot.size());
        assertTrue(snapshot.containsKey("a"));
        assertTrue(snapshot.containsKey("b"));
    }
}
