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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the pure Java {@link ShutdownWatcher}.
 */
public class ShutdownWatcherTest {

    /**
     * Stub implementation of PipelineMonitor for testing.
     */
    private static class StubPipelineMonitor implements ShutdownWatcher.PipelineMonitor {
        private boolean finishedExecution = false;
        private boolean workersDraining = false;
        private String pipelineId = "test-pipeline";
        private PipelineReporter.Snapshot snapshot = null;

        @Override
        public boolean isFinishedExecution() {
            return finishedExecution;
        }

        @Override
        public boolean isWorkersDraining() {
            return workersDraining;
        }

        @Override
        public String getPipelineId() {
            return pipelineId;
        }

        @Override
        public PipelineReporter.Snapshot getSnapshot() {
            return snapshot;
        }

        public void setFinishedExecution(final boolean finished) {
            this.finishedExecution = finished;
        }

        public void setWorkersDraining(final boolean draining) {
            this.workersDraining = draining;
        }

        public void setPipelineId(final String id) {
            this.pipelineId = id;
        }

        public void setSnapshot(final PipelineReporter.Snapshot snapshot) {
            this.snapshot = snapshot;
        }
    }

    @Test
    public void testConstructionWithDefaults() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor);

        assertEquals(1000L, watcher.getCyclePeriodMs());
        assertEquals(5, watcher.getReportEvery());
        assertEquals(3, watcher.getAbortThreshold());
        assertFalse(watcher.isStopped());
        assertEquals(0, watcher.getAttemptsCount());
        assertSame(monitor, watcher.getPipeline());
    }

    @Test
    public void testConstructionWithCustomValues() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 500L, 3, 10);

        assertEquals(500L, watcher.getCyclePeriodMs());
        assertEquals(3, watcher.getReportEvery());
        assertEquals(10, watcher.getAbortThreshold());
    }

    @Test
    public void testStopAndIsStopped() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor);

        assertFalse("Watcher should not be stopped initially", watcher.isStopped());

        watcher.stop();

        assertTrue("Watcher should be stopped after stop()", watcher.isStopped());
    }

    @Test
    public void testStopIsIdempotent() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor);

        watcher.stop();
        assertTrue(watcher.isStopped());

        // Calling stop again should not throw or change state
        watcher.stop();
        assertTrue(watcher.isStopped());
    }

    @Test
    public void testAttemptsCountIncrement() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor);

        assertEquals(0, watcher.getAttemptsCount());

        assertEquals(1, watcher.incrementAndGetAttempts());
        assertEquals(1, watcher.getAttemptsCount());

        assertEquals(2, watcher.incrementAndGetAttempts());
        assertEquals(2, watcher.getAttemptsCount());

        assertEquals(3, watcher.incrementAndGetAttempts());
        assertEquals(3, watcher.getAttemptsCount());
    }

    @Test
    public void testAddReportMaintainsCircularBuffer() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 3, 3);

        // Add 3 reports (reportEvery = 3)
        final PipelineReporter.Snapshot snap1 = createSnapshot(10);
        final PipelineReporter.Snapshot snap2 = createSnapshot(8);
        final PipelineReporter.Snapshot snap3 = createSnapshot(5);

        watcher.addReport(snap1);
        watcher.addReport(snap2);
        watcher.addReport(snap3);

        // Add a 4th report, which should push out the first
        final PipelineReporter.Snapshot snap4 = createSnapshot(3);
        watcher.addReport(snap4);

        // The watcher should still have exactly 3 reports (reportEvery = 3)
        // We can verify this through the isShutdownStalled behavior
        // Since snap2(8) -> snap3(5) is decreasing, stalled should be false
        assertFalse(watcher.isShutdownStalled());
    }

    @Test
    public void testIsShutdownStalledReturnsFalseWhenNotEnoughReports() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 5, 3);

        // Add fewer than reportEvery (5) reports
        watcher.addReport(createSnapshot(10));
        watcher.addReport(createSnapshot(10));
        watcher.addReport(createSnapshot(10));

        assertFalse("Should return false when fewer than reportEvery reports collected",
                watcher.isShutdownStalled());
    }

    @Test
    public void testIsShutdownStalledReturnsFalseWithNoReports() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor);

        assertFalse("Should return false with no reports", watcher.isShutdownStalled());
    }

    @Test
    public void testIsShutdownStalledReturnsTrueWhenInflightNonDecreasingAndSameThreadInfo() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 5, 3);

        // All snapshots have same inflight count (non-decreasing) and same stalling info
        final Map<String, Object> stallingInfo = Collections.singletonMap("thread-1", "blocked");

        for (int i = 0; i < 5; i++) {
            watcher.addReport(createSnapshot(10, stallingInfo));
        }

        assertTrue("Should return true when inflight counts are non-decreasing and stalling info is identical",
                watcher.isShutdownStalled());
    }

    @Test
    public void testIsShutdownStalledReturnsTrueWhenInflightIncreasing() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 5, 3);

        // Inflight counts are increasing (also non-decreasing)
        final Map<String, Object> stallingInfo = Collections.singletonMap("thread-1", "blocked");

        for (int i = 0; i < 5; i++) {
            watcher.addReport(createSnapshot(10 + i, stallingInfo));
        }

        assertTrue("Should return true when inflight counts are increasing (non-decreasing)",
                watcher.isShutdownStalled());
    }

    @Test
    public void testIsShutdownStalledReturnsFalseWhenInflightDecreasing() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 5, 3);

        // Inflight counts are decreasing - making progress
        final Map<String, Object> stallingInfo = Collections.singletonMap("thread-1", "blocked");

        for (int i = 0; i < 5; i++) {
            watcher.addReport(createSnapshot(10 - i, stallingInfo));
        }

        assertFalse("Should return false when inflight counts are decreasing (making progress)",
                watcher.isShutdownStalled());
    }

    @Test
    public void testIsShutdownStalledReturnsFalseWhenStallingInfoDiffers() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 5, 3);

        // Same inflight count but different stalling thread info
        for (int i = 0; i < 5; i++) {
            final Map<String, Object> stallingInfo = new HashMap<>();
            stallingInfo.put("thread-" + i, "blocked");
            watcher.addReport(createSnapshot(10, stallingInfo));
        }

        assertFalse("Should return false when stalling thread info differs between snapshots",
                watcher.isShutdownStalled());
    }

    @Test
    public void testIsShutdownStalledWithEmptyStallingInfo() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 3, 3);

        // Same inflight and empty stalling info
        for (int i = 0; i < 3; i++) {
            watcher.addReport(createSnapshot(10, Collections.emptyMap()));
        }

        assertTrue("Should return true with non-decreasing inflight and identical empty stalling info",
                watcher.isShutdownStalled());
    }

    @Test
    public void testPipelineMonitorInterface() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();

        assertFalse(monitor.isFinishedExecution());
        assertFalse(monitor.isWorkersDraining());
        assertEquals("test-pipeline", monitor.getPipelineId());
        assertEquals(null, monitor.getSnapshot());

        monitor.setFinishedExecution(true);
        monitor.setWorkersDraining(true);
        monitor.setPipelineId("my-pipeline");

        assertTrue(monitor.isFinishedExecution());
        assertTrue(monitor.isWorkersDraining());
        assertEquals("my-pipeline", monitor.getPipelineId());
    }

    @Test
    public void testPipelineMonitorWithSnapshot() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final PipelineReporter.Snapshot snapshot = createSnapshot(5);

        monitor.setSnapshot(snapshot);

        assertNotNull(monitor.getSnapshot());
        assertEquals(5, monitor.getSnapshot().getInflightCount());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructionWithNullPipeline() {
        new ShutdownWatcher(null);
    }

    @Test
    public void testCircularBufferExactlyAtReportEvery() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 3, 3);

        final Map<String, Object> stallingInfo = Collections.singletonMap("t1", "blocked");

        // Add exactly reportEvery (3) reports with same inflight and stalling info
        watcher.addReport(createSnapshot(10, stallingInfo));
        watcher.addReport(createSnapshot(10, stallingInfo));
        watcher.addReport(createSnapshot(10, stallingInfo));

        assertTrue("Should detect stall with exactly reportEvery reports",
                watcher.isShutdownStalled());
    }

    @Test
    public void testCircularBufferOverflow() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 3, 3);

        final Map<String, Object> stallingInfo = Collections.singletonMap("t1", "blocked");

        // First add a decreasing snapshot, then 3 non-decreasing ones
        // The first one should be evicted from the circular buffer
        watcher.addReport(createSnapshot(20, stallingInfo)); // will be evicted
        watcher.addReport(createSnapshot(10, stallingInfo));
        watcher.addReport(createSnapshot(10, stallingInfo));
        watcher.addReport(createSnapshot(10, stallingInfo));

        // After adding 4 reports with buffer size 3, the first (20) should be gone
        // Remaining: 10, 10, 10 -> non-decreasing -> stalled
        assertTrue("Should detect stall after old report is evicted from circular buffer",
                watcher.isShutdownStalled());
    }

    @Test
    public void testPartiallyDecreasingInflight() {
        final StubPipelineMonitor monitor = new StubPipelineMonitor();
        final ShutdownWatcher watcher = new ShutdownWatcher(monitor, 1000L, 5, 3);

        final Map<String, Object> stallingInfo = Collections.singletonMap("t1", "blocked");

        // 10, 10, 10, 8, 10 - the decrease from 10 to 8 means it's not stalled
        watcher.addReport(createSnapshot(10, stallingInfo));
        watcher.addReport(createSnapshot(10, stallingInfo));
        watcher.addReport(createSnapshot(10, stallingInfo));
        watcher.addReport(createSnapshot(8, stallingInfo));
        watcher.addReport(createSnapshot(10, stallingInfo));

        assertFalse("Should not be stalled when any consecutive decrease exists",
                watcher.isShutdownStalled());
    }

    // --- Helper methods ---

    private static PipelineReporter.Snapshot createSnapshot(final int inflightCount) {
        return createSnapshot(inflightCount, Collections.emptyMap());
    }

    private static PipelineReporter.Snapshot createSnapshot(final int inflightCount,
                                                            final Map<String, Object> stallingInfo) {
        return new PipelineReporter.Snapshot(
                inflightCount, stallingInfo, 0L, 0L,
                Collections.emptyList(), Collections.emptyList()
        );
    }
}
