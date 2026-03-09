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

package org.logstash.config.source;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigSourceLoaderTest {

    // --- Simple stub implementations for testing ---

    static class StubConfigSource implements PipelineConfigSource {
        private final String pipelineId;
        private final boolean matches;
        private final boolean hasConflict;
        private final String conflictMessage;

        StubConfigSource(String pipelineId, boolean matches) {
            this(pipelineId, matches, false, "");
        }

        StubConfigSource(String pipelineId, boolean matches, boolean hasConflict,
                         String conflictMessage) {
            this.pipelineId = pipelineId;
            this.matches = matches;
            this.hasConflict = hasConflict;
            this.conflictMessage = conflictMessage;
        }

        @Override
        public List<PipelineConfigParts> pipelineConfigs() {
            PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                    "string", pipelineId,
                    "input { stdin {} } output { stdout {} }");
            PipelineConfigParts config = new PipelineConfigParts(pipelineId,
                    Collections.singletonList(part), Collections.emptyMap());
            return Collections.singletonList(config);
        }

        @Override
        public boolean matches() {
            return matches;
        }

        @Override
        public boolean hasConflict() {
            return hasConflict;
        }

        @Override
        public String getConflictMessage() {
            return conflictMessage;
        }
    }

    static class ThrowingConfigSource implements PipelineConfigSource {
        @Override
        public List<PipelineConfigParts> pipelineConfigs() {
            throw new RuntimeException("Simulated failure");
        }

        @Override
        public boolean matches() {
            return true;
        }

        @Override
        public boolean hasConflict() {
            return false;
        }

        @Override
        public String getConflictMessage() {
            return "";
        }
    }

    // --- addSource / removeSource / getSources tests ---

    @Test
    public void testAddAndGetSources() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("a", true));
        loader.addSource(new StubConfigSource("b", true));

        assertEquals(2, loader.getSources().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullSourceThrows() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(null);
    }

    @Test
    public void testRemoveSource() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("a", true));
        loader.addSource(new ThrowingConfigSource());

        boolean removed = loader.removeSource(ThrowingConfigSource.class);
        assertTrue(removed);
        assertEquals(1, loader.getSources().size());
    }

    @Test
    public void testRemoveSourceReturnsFalseWhenNotFound() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("a", true));

        boolean removed = loader.removeSource(ThrowingConfigSource.class);
        assertFalse(removed);
        assertEquals(1, loader.getSources().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNullClassThrows() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.removeSource(null);
    }

    @Test
    public void testGetSourcesReturnsDefensiveCopy() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("a", true));

        List<PipelineConfigSource> copy = loader.getSources();
        copy.add(new StubConfigSource("b", true));

        // Original should not be affected
        assertEquals(1, loader.getSources().size());
    }

    // --- fetch() tests ---

    @Test
    public void testFetchWithSingleMatchingSource() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("my-pipeline", true));

        ConfigFetchResult result = loader.fetch();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getConfigs().size());
        assertEquals("my-pipeline", result.getConfigs().get(0).getPipelineId());
    }

    @Test
    public void testFetchWithMultipleMatchingSources() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("pipeline-a", true));
        loader.addSource(new StubConfigSource("pipeline-b", true));

        ConfigFetchResult result = loader.fetch();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getConfigs().size());
    }

    @Test
    public void testFetchSkipsNonMatchingSources() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("matching", true));
        loader.addSource(new StubConfigSource("not-matching", false));

        ConfigFetchResult result = loader.fetch();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getConfigs().size());
        assertEquals("matching", result.getConfigs().get(0).getPipelineId());
    }

    @Test
    public void testFetchDetectsConflicts() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("conflicting", true, true,
                "Both paths and string set"));

        ConfigFetchResult result = loader.fetch();

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Both paths and string set"));
    }

    @Test
    public void testFetchDetectsDuplicatePipelineIds() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("duplicate-id", true));
        loader.addSource(new StubConfigSource("duplicate-id", true));

        ConfigFetchResult result = loader.fetch();

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Duplicate pipeline ID"));
        assertTrue(result.getErrors().get(0).contains("duplicate-id"));
    }

    @Test
    public void testFetchHandlesExceptionsFromSources() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new ThrowingConfigSource());

        ConfigFetchResult result = loader.fetch();

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Simulated failure"));
    }

    @Test
    public void testFetchWithNoSources() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        ConfigFetchResult result = loader.fetch();

        assertTrue(result.isSuccess());
        assertTrue(result.getConfigs().isEmpty());
    }

    @Test
    public void testFetchWithAllNonMatchingSources() {
        ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("a", false));
        loader.addSource(new StubConfigSource("b", false));

        ConfigFetchResult result = loader.fetch();

        assertTrue(result.isSuccess());
        assertTrue(result.getConfigs().isEmpty());
    }

    // --- Thread safety tests ---

    @Test
    public void testConcurrentAddAndGet() throws Exception {
        final ConfigSourceLoader loader = new ConfigSourceLoader();
        final int numThreads = 10;
        final int sourcesPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    for (int i = 0; i < sourcesPerThread; i++) {
                        loader.addSource(new StubConfigSource(
                                "pipeline-" + threadId + "-" + i, true));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(numThreads * sourcesPerThread, loader.getSources().size());
    }

    @Test
    public void testConcurrentFetch() throws Exception {
        final ConfigSourceLoader loader = new ConfigSourceLoader();
        loader.addSource(new StubConfigSource("pipeline-1", true));
        loader.addSource(new StubConfigSource("pipeline-2", true));

        final int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        List<Future<ConfigFetchResult>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return loader.fetch();
            }));
        }

        for (Future<ConfigFetchResult> f : futures) {
            ConfigFetchResult result = f.get(10, TimeUnit.SECONDS);
            assertTrue(result.isSuccess());
            assertEquals(2, result.getConfigs().size());
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void testConcurrentAddAndFetch() throws Exception {
        final ConfigSourceLoader loader = new ConfigSourceLoader();
        final int numThreads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        // Half threads add sources, half fetch
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    if (threadId % 2 == 0) {
                        for (int i = 0; i < 20; i++) {
                            loader.addSource(new StubConfigSource(
                                    "p-" + threadId + "-" + i, true));
                        }
                    } else {
                        for (int i = 0; i < 20; i++) {
                            ConfigFetchResult result = loader.fetch();
                            assertNotNull(result);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify we have some sources (at least from the adding threads)
        assertTrue(loader.getSources().size() > 0);
    }
}
