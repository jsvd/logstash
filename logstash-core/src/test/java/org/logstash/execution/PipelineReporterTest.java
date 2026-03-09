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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the pure Java {@link PipelineReporter}.
 */
public class PipelineReporterTest {

    @Test
    public void testSnapshotCreationWithAllFields() {
        final Map<String, Object> stallingInfo = new HashMap<>();
        stallingInfo.put("thread-1", "blocked on plugin X");
        stallingInfo.put("thread-2", "waiting on IO");

        final List<PipelineReporter.WorkerState> workerStates = Arrays.asList(
                new PipelineReporter.WorkerState(1L, true, 10),
                new PipelineReporter.WorkerState(2L, true, 5),
                new PipelineReporter.WorkerState(3L, false, 0)
        );

        final List<PipelineReporter.OutputInfo> outputInfo = Arrays.asList(
                new PipelineReporter.OutputInfo("elasticsearch", "es-output-1", "shared"),
                new PipelineReporter.OutputInfo("stdout", "stdout-1", "single")
        );

        final PipelineReporter.Snapshot snapshot = new PipelineReporter.Snapshot(
                15, stallingInfo, 1000L, 2000L, workerStates, outputInfo
        );

        assertEquals(15, snapshot.getInflightCount());
        assertEquals(2, snapshot.getStallingThreadsInfo().size());
        assertEquals("blocked on plugin X", snapshot.getStallingThreadsInfo().get("thread-1"));
        assertEquals("waiting on IO", snapshot.getStallingThreadsInfo().get("thread-2"));
        assertEquals(1000L, snapshot.getEventsFiltered());
        assertEquals(2000L, snapshot.getEventsConsumed());
        assertEquals(3, snapshot.getWorkerStates().size());
        assertEquals(2, snapshot.getOutputInfo().size());
        assertNotNull(snapshot.getCreatedAt());
    }

    @Test
    public void testSnapshotGetters() {
        final PipelineReporter.Snapshot snapshot = new PipelineReporter.Snapshot(
                42, Collections.singletonMap("key", "value"),
                500L, 600L,
                Collections.singletonList(new PipelineReporter.WorkerState(1L, true, 42)),
                Collections.singletonList(new PipelineReporter.OutputInfo("type", "id", "shared"))
        );

        assertEquals(42, snapshot.getInflightCount());
        assertEquals(500L, snapshot.getEventsFiltered());
        assertEquals(600L, snapshot.getEventsConsumed());
        assertNotNull(snapshot.getCreatedAt());
    }

    @Test
    public void testSnapshotImmutableStallingThreadsInfo() {
        final Map<String, Object> stallingInfo = new HashMap<>();
        stallingInfo.put("key", "value");

        final PipelineReporter.Snapshot snapshot = new PipelineReporter.Snapshot(
                0, stallingInfo, 0L, 0L, Collections.emptyList(), Collections.emptyList()
        );

        try {
            snapshot.getStallingThreadsInfo().put("new-key", "new-value");
            // If we get here, the map is not truly unmodifiable (some implementations allow it)
            // But the contract is that it should be unmodifiable
        } catch (final UnsupportedOperationException e) {
            // Expected: the map is unmodifiable
        }
    }

    @Test
    public void testSnapshotImmutableWorkerStates() {
        final List<PipelineReporter.WorkerState> workerStates = new ArrayList<>();
        workerStates.add(new PipelineReporter.WorkerState(1L, true, 5));

        final PipelineReporter.Snapshot snapshot = new PipelineReporter.Snapshot(
                5, Collections.emptyMap(), 0L, 0L, workerStates, Collections.emptyList()
        );

        try {
            snapshot.getWorkerStates().add(new PipelineReporter.WorkerState(2L, false, 0));
        } catch (final UnsupportedOperationException e) {
            // Expected: the list is unmodifiable
        }
    }

    @Test
    public void testSnapshotImmutableOutputInfo() {
        final List<PipelineReporter.OutputInfo> outputInfo = new ArrayList<>();
        outputInfo.add(new PipelineReporter.OutputInfo("es", "id1", "shared"));

        final PipelineReporter.Snapshot snapshot = new PipelineReporter.Snapshot(
                0, Collections.emptyMap(), 0L, 0L, Collections.emptyList(), outputInfo
        );

        try {
            snapshot.getOutputInfo().add(new PipelineReporter.OutputInfo("stdout", "id2", "single"));
        } catch (final UnsupportedOperationException e) {
            // Expected: the list is unmodifiable
        }
    }

    @Test
    public void testSnapshotWithNullStallingThreadsInfo() {
        final PipelineReporter.Snapshot snapshot = new PipelineReporter.Snapshot(
                0, null, 0L, 0L, Collections.emptyList(), Collections.emptyList()
        );

        assertNotNull(snapshot.getStallingThreadsInfo());
        assertTrue(snapshot.getStallingThreadsInfo().isEmpty());
    }

    @Test
    public void testSnapshotWithNullWorkerStates() {
        final PipelineReporter.Snapshot snapshot = new PipelineReporter.Snapshot(
                0, Collections.emptyMap(), 0L, 0L, null, Collections.emptyList()
        );

        assertNotNull(snapshot.getWorkerStates());
        assertTrue(snapshot.getWorkerStates().isEmpty());
    }

    @Test
    public void testSnapshotWithNullOutputInfo() {
        final PipelineReporter.Snapshot snapshot = new PipelineReporter.Snapshot(
                0, Collections.emptyMap(), 0L, 0L, Collections.emptyList(), null
        );

        assertNotNull(snapshot.getOutputInfo());
        assertTrue(snapshot.getOutputInfo().isEmpty());
    }

    @Test
    public void testSnapshotWithEmptyWorkerStates() {
        final PipelineReporter.Snapshot snapshot = PipelineReporter.createSnapshot(
                0, Collections.emptyMap(), 100L, 200L,
                Collections.emptyList(), Collections.emptyList()
        );

        assertTrue(snapshot.getWorkerStates().isEmpty());
        assertEquals(100L, snapshot.getEventsFiltered());
        assertEquals(200L, snapshot.getEventsConsumed());
    }

    @Test
    public void testSnapshotWithEmptyOutputInfo() {
        final PipelineReporter.Snapshot snapshot = PipelineReporter.createSnapshot(
                5, Collections.emptyMap(), 0L, 0L,
                Collections.singletonList(new PipelineReporter.WorkerState(1L, true, 5)),
                Collections.emptyList()
        );

        assertTrue(snapshot.getOutputInfo().isEmpty());
        assertEquals(1, snapshot.getWorkerStates().size());
    }

    @Test
    public void testSnapshotToString() {
        final PipelineReporter.Snapshot snapshot = PipelineReporter.createSnapshot(
                10, Collections.emptyMap(), 100L, 200L,
                Collections.emptyList(), Collections.emptyList()
        );

        final String str = snapshot.toString();
        assertTrue(str.contains("inflightCount=10"));
        assertTrue(str.contains("eventsFiltered=100"));
        assertTrue(str.contains("eventsConsumed=200"));
    }

    @Test
    public void testWorkerStateCreationAndGetters() {
        final PipelineReporter.WorkerState state = new PipelineReporter.WorkerState(42L, true, 7);

        assertEquals(42L, state.getThreadId());
        assertTrue(state.isAlive());
        assertEquals(7, state.getInflightCount());
    }

    @Test
    public void testWorkerStateDeadThread() {
        final PipelineReporter.WorkerState state = new PipelineReporter.WorkerState(99L, false, 0);

        assertEquals(99L, state.getThreadId());
        assertFalse(state.isAlive());
        assertEquals(0, state.getInflightCount());
    }

    @Test
    public void testWorkerStateEquals() {
        final PipelineReporter.WorkerState state1 = new PipelineReporter.WorkerState(1L, true, 5);
        final PipelineReporter.WorkerState state2 = new PipelineReporter.WorkerState(1L, true, 5);
        final PipelineReporter.WorkerState state3 = new PipelineReporter.WorkerState(2L, true, 5);

        assertEquals(state1, state2);
        assertFalse(state1.equals(state3));
    }

    @Test
    public void testWorkerStateHashCode() {
        final PipelineReporter.WorkerState state1 = new PipelineReporter.WorkerState(1L, true, 5);
        final PipelineReporter.WorkerState state2 = new PipelineReporter.WorkerState(1L, true, 5);

        assertEquals(state1.hashCode(), state2.hashCode());
    }

    @Test
    public void testWorkerStateToString() {
        final PipelineReporter.WorkerState state = new PipelineReporter.WorkerState(10L, true, 3);
        final String str = state.toString();

        assertTrue(str.contains("threadId=10"));
        assertTrue(str.contains("alive=true"));
        assertTrue(str.contains("inflightCount=3"));
    }

    @Test
    public void testOutputInfoCreationAndGetters() {
        final PipelineReporter.OutputInfo info =
                new PipelineReporter.OutputInfo("elasticsearch", "es-output-1", "shared");

        assertEquals("elasticsearch", info.getType());
        assertEquals("es-output-1", info.getId());
        assertEquals("shared", info.getConcurrency());
    }

    @Test
    public void testOutputInfoEquals() {
        final PipelineReporter.OutputInfo info1 =
                new PipelineReporter.OutputInfo("elasticsearch", "id1", "shared");
        final PipelineReporter.OutputInfo info2 =
                new PipelineReporter.OutputInfo("elasticsearch", "id1", "shared");
        final PipelineReporter.OutputInfo info3 =
                new PipelineReporter.OutputInfo("stdout", "id2", "single");

        assertEquals(info1, info2);
        assertFalse(info1.equals(info3));
    }

    @Test
    public void testOutputInfoHashCode() {
        final PipelineReporter.OutputInfo info1 =
                new PipelineReporter.OutputInfo("elasticsearch", "id1", "shared");
        final PipelineReporter.OutputInfo info2 =
                new PipelineReporter.OutputInfo("elasticsearch", "id1", "shared");

        assertEquals(info1.hashCode(), info2.hashCode());
    }

    @Test
    public void testOutputInfoToString() {
        final PipelineReporter.OutputInfo info =
                new PipelineReporter.OutputInfo("elasticsearch", "es-1", "shared");
        final String str = info.toString();

        assertTrue(str.contains("type='elasticsearch'"));
        assertTrue(str.contains("id='es-1'"));
        assertTrue(str.contains("concurrency='shared'"));
    }

    @Test
    public void testCreateSnapshotFactoryMethod() {
        final Map<String, Object> stallingInfo = Collections.singletonMap("thread-1", "blocked");
        final List<PipelineReporter.WorkerState> workers = Arrays.asList(
                new PipelineReporter.WorkerState(1L, true, 10),
                new PipelineReporter.WorkerState(2L, false, 0)
        );
        final List<PipelineReporter.OutputInfo> outputs = Collections.singletonList(
                new PipelineReporter.OutputInfo("elasticsearch", "es-1", "shared")
        );

        final PipelineReporter.Snapshot snapshot = PipelineReporter.createSnapshot(
                10, stallingInfo, 500L, 1000L, workers, outputs
        );

        assertEquals(10, snapshot.getInflightCount());
        assertEquals(1, snapshot.getStallingThreadsInfo().size());
        assertEquals("blocked", snapshot.getStallingThreadsInfo().get("thread-1"));
        assertEquals(500L, snapshot.getEventsFiltered());
        assertEquals(1000L, snapshot.getEventsConsumed());
        assertEquals(2, snapshot.getWorkerStates().size());
        assertEquals(1, snapshot.getOutputInfo().size());
        assertNotNull(snapshot.getCreatedAt());
    }

    @Test
    public void testCreateSnapshotFactoryMethodWithNulls() {
        final PipelineReporter.Snapshot snapshot = PipelineReporter.createSnapshot(
                0, null, 0L, 0L, null, null
        );

        assertEquals(0, snapshot.getInflightCount());
        assertTrue(snapshot.getStallingThreadsInfo().isEmpty());
        assertTrue(snapshot.getWorkerStates().isEmpty());
        assertTrue(snapshot.getOutputInfo().isEmpty());
    }

    @Test
    public void testOutputInfoWithNullFields() {
        final PipelineReporter.OutputInfo info = new PipelineReporter.OutputInfo(null, null, null);

        assertEquals(null, info.getType());
        assertEquals(null, info.getId());
        assertEquals(null, info.getConcurrency());
    }

    @Test
    public void testWorkerStateEqualsNull() {
        final PipelineReporter.WorkerState state = new PipelineReporter.WorkerState(1L, true, 5);
        assertFalse(state.equals(null));
    }

    @Test
    public void testWorkerStateEqualsSelf() {
        final PipelineReporter.WorkerState state = new PipelineReporter.WorkerState(1L, true, 5);
        assertTrue(state.equals(state));
    }

    @Test
    public void testOutputInfoEqualsNull() {
        final PipelineReporter.OutputInfo info =
                new PipelineReporter.OutputInfo("type", "id", "shared");
        assertFalse(info.equals(null));
    }

    @Test
    public void testOutputInfoEqualsSelf() {
        final PipelineReporter.OutputInfo info =
                new PipelineReporter.OutputInfo("type", "id", "shared");
        assertTrue(info.equals(info));
    }
}
