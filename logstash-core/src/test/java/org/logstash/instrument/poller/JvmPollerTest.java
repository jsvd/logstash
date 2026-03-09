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

package org.logstash.instrument.poller;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JvmPoller}.
 */
public class JvmPollerTest {

    @Test
    public void getName() {
        JvmPoller poller = new JvmPoller();
        assertThat(poller.getName()).isEqualTo("jvm");
    }

    @Test
    public void collectMetricsReturnsNonNull() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.getHeap()).isNotNull();
        assertThat(metrics.getNonHeap()).isNotNull();
        assertThat(metrics.getGc()).isNotNull();
        assertThat(metrics.getThreads()).isNotNull();
        assertThat(metrics.getProcess()).isNotNull();
    }

    @Test
    public void heapUsedIsPositive() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        assertThat(metrics.getHeap().getUsedBytes()).isGreaterThan(0);
        assertThat(metrics.getHeap().getCommittedBytes()).isGreaterThan(0);
    }

    @Test
    public void heapUsedIsLessThanOrEqualToCommitted() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        assertThat(metrics.getHeap().getUsedBytes())
                .isLessThanOrEqualTo(metrics.getHeap().getCommittedBytes());
    }

    @Test
    public void nonHeapUsedIsPositive() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        assertThat(metrics.getNonHeap().getUsedBytes()).isGreaterThan(0);
        assertThat(metrics.getNonHeap().getCommittedBytes()).isGreaterThan(0);
    }

    @Test
    public void gcMetricsNotEmpty() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        assertThat(metrics.getGc()).isNotEmpty();

        for (Map.Entry<String, JvmPoller.GcMetrics> entry : metrics.getGc().entrySet()) {
            assertThat(entry.getKey()).isNotEmpty();
            assertThat(entry.getValue().getCollectionCount()).isGreaterThanOrEqualTo(0);
            assertThat(entry.getValue().getCollectionTimeMillis()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    public void threadCountIsPositive() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        assertThat(metrics.getThreads().getCount()).isGreaterThan(0);
        assertThat(metrics.getThreads().getPeakCount()).isGreaterThan(0);
        assertThat(metrics.getThreads().getPeakCount())
                .isGreaterThanOrEqualTo(metrics.getThreads().getCount());
    }

    @Test
    public void uptimeIsPositive() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        assertThat(metrics.getUptimeMillis()).isGreaterThan(0);
    }

    @Test
    public void processMetricsCollected() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        // Process CPU time should be collected on all platforms we support
        assertThat(metrics.getProcess().getCpuTotalMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void classifyGcCollectorYoung() {
        assertThat(JvmPoller.classifyGcCollector("Copy")).isEqualTo("young");
        assertThat(JvmPoller.classifyGcCollector("PS Scavenge")).isEqualTo("young");
        assertThat(JvmPoller.classifyGcCollector("ParNew")).isEqualTo("young");
        assertThat(JvmPoller.classifyGcCollector("G1 Young Generation")).isEqualTo("young");
        assertThat(JvmPoller.classifyGcCollector("ZGC Minor Pauses")).isEqualTo("young");
        assertThat(JvmPoller.classifyGcCollector("ZGC Minor Cycles")).isEqualTo("young");
        assertThat(JvmPoller.classifyGcCollector("Shenandoah Pauses")).isEqualTo("young");
    }

    @Test
    public void classifyGcCollectorOld() {
        assertThat(JvmPoller.classifyGcCollector("MarkSweepCompact")).isEqualTo("old");
        assertThat(JvmPoller.classifyGcCollector("PS MarkSweep")).isEqualTo("old");
        assertThat(JvmPoller.classifyGcCollector("ConcurrentMarkSweep")).isEqualTo("old");
        assertThat(JvmPoller.classifyGcCollector("G1 Old Generation")).isEqualTo("old");
        assertThat(JvmPoller.classifyGcCollector("G1 Mixed Generation")).isEqualTo("old");
        assertThat(JvmPoller.classifyGcCollector("ZGC Major Pauses")).isEqualTo("old");
        assertThat(JvmPoller.classifyGcCollector("ZGC Major Cycles")).isEqualTo("old");
        assertThat(JvmPoller.classifyGcCollector("Shenandoah Cycles")).isEqualTo("old");
    }

    @Test
    public void classifyGcCollectorUnknown() {
        assertThat(JvmPoller.classifyGcCollector("SomeFutureCollector")).isEqualTo("unknown");
        assertThat(JvmPoller.classifyGcCollector("")).isEqualTo("unknown");
    }

    @Test
    public void collectStoresLastMetrics() {
        JvmPoller poller = new JvmPoller();
        assertThat(poller.getLastMetrics()).isNull();

        poller.collect();
        assertThat(poller.getLastMetrics()).isNotNull();
        assertThat(poller.getLastMetrics().getHeap().getUsedBytes()).isGreaterThan(0);
    }

    @Test
    public void heapMetricsImmutability() {
        JvmPoller.HeapMetrics heap = new JvmPoller.HeapMetrics(100, 200, 300);

        assertThat(heap.getUsedBytes()).isEqualTo(100);
        assertThat(heap.getCommittedBytes()).isEqualTo(200);
        assertThat(heap.getMaxBytes()).isEqualTo(300);
    }

    @Test
    public void nonHeapMetricsImmutability() {
        JvmPoller.NonHeapMetrics nonHeap = new JvmPoller.NonHeapMetrics(50, 100, -1);

        assertThat(nonHeap.getUsedBytes()).isEqualTo(50);
        assertThat(nonHeap.getCommittedBytes()).isEqualTo(100);
        assertThat(nonHeap.getMaxBytes()).isEqualTo(-1);
    }

    @Test
    public void gcMetricsImmutability() {
        JvmPoller.GcMetrics gc = new JvmPoller.GcMetrics(42, 1234);

        assertThat(gc.getCollectionCount()).isEqualTo(42);
        assertThat(gc.getCollectionTimeMillis()).isEqualTo(1234);
    }

    @Test
    public void threadMetricsImmutability() {
        JvmPoller.ThreadMetrics threads = new JvmPoller.ThreadMetrics(10, 20);

        assertThat(threads.getCount()).isEqualTo(10);
        assertThat(threads.getPeakCount()).isEqualTo(20);
    }

    @Test
    public void processMetricsImmutability() {
        JvmPoller.ProcessMetrics process = new JvmPoller.ProcessMetrics(100, 1024, (short) 50, 5000, 1024000);

        assertThat(process.getOpenFileDescriptors()).isEqualTo(100);
        assertThat(process.getMaxFileDescriptors()).isEqualTo(1024);
        assertThat(process.getCpuPercent()).isEqualTo((short) 50);
        assertThat(process.getCpuTotalMillis()).isEqualTo(5000);
        assertThat(process.getTotalVirtualMemoryBytes()).isEqualTo(1024000);
    }

    @Test
    public void gcMetricsMapIsUnmodifiable() {
        JvmPoller poller = new JvmPoller();
        JvmPoller.JvmMetrics metrics = poller.collectMetrics();

        try {
            metrics.getGc().put("fake", new JvmPoller.GcMetrics(0, 0));
            // If we reach here, the map is not truly unmodifiable
            // (this should not happen)
            assertThat(false).as("Expected UnsupportedOperationException").isTrue();
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
