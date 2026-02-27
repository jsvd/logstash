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
package org.logstash.api;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StatsInfoTest {

    // --- EventStats tests ---

    @Test
    public void testEventStatsConstruction() {
        final StatsInfo.EventStats stats = new StatsInfo.EventStats(100, 95, 90, 5000, 200);
        assertEquals(100, stats.getIn());
        assertEquals(95, stats.getFiltered());
        assertEquals(90, stats.getOut());
        assertEquals(5000, stats.getDurationMillis());
        assertEquals(200, stats.getQueuePushDurationMillis());
    }

    @Test
    public void testEventStatsToMap() {
        final StatsInfo.EventStats stats = new StatsInfo.EventStats(100, 95, 90, 5000, 200);
        final Map<String, Object> map = stats.toMap();
        assertEquals(100L, map.get("in"));
        assertEquals(95L, map.get("filtered"));
        assertEquals(90L, map.get("out"));
        assertEquals(5000L, map.get("duration_in_millis"));
        assertEquals(200L, map.get("queue_push_duration_in_millis"));
        assertEquals(5, map.size());
    }

    @Test
    public void testEventStatsEquality() {
        final StatsInfo.EventStats s1 = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo.EventStats s2 = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testEventStatsInequality() {
        final StatsInfo.EventStats s1 = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo.EventStats s2 = new StatsInfo.EventStats(20, 9, 8, 100, 20);
        assertNotEquals(s1, s2);
    }

    @Test
    public void testEventStatsZeroValues() {
        final StatsInfo.EventStats stats = new StatsInfo.EventStats(0, 0, 0, 0, 0);
        assertEquals(0, stats.getIn());
        assertEquals(0, stats.getFiltered());
        assertEquals(0, stats.getOut());
    }

    @Test
    public void testEventStatsToString() {
        final StatsInfo.EventStats stats = new StatsInfo.EventStats(100, 95, 90, 5000, 200);
        final String str = stats.toString();
        assertTrue(str.contains("100"));
        assertTrue(str.contains("95"));
        assertTrue(str.contains("90"));
    }

    @Test
    public void testEventStatsMapIsImmutable() {
        final StatsInfo.EventStats stats = new StatsInfo.EventStats(1, 1, 1, 1, 1);
        try {
            stats.toMap().put("extra", "value");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    // --- JvmStats tests ---

    @Test
    public void testJvmStatsConstruction() {
        final Map<String, Object> mem = Map.of("heap_used", 1024L, "heap_max", 4096L);
        final Map<String, Object> gc = Map.of("young_count", 10L, "old_count", 2L);
        final StatsInfo.JvmStats stats = new StatsInfo.JvmStats(300000L, mem, 50, 55, gc);

        assertEquals(300000L, stats.getUptimeMillis());
        assertEquals(1024L, stats.getMemoryUsage().get("heap_used"));
        assertEquals(50, stats.getThreadCount());
        assertEquals(55, stats.getPeakThreadCount());
        assertEquals(10L, stats.getGcStats().get("young_count"));
    }

    @Test
    public void testJvmStatsToMap() {
        final Map<String, Object> mem = Map.of("heap_used", 1024L);
        final Map<String, Object> gc = Map.of("collections", 5L);
        final StatsInfo.JvmStats stats = new StatsInfo.JvmStats(500000L, mem, 30, 40, gc);

        final Map<String, Object> map = stats.toMap();
        assertEquals(500000L, map.get("uptime_in_millis"));
        assertNotNull(map.get("mem"));
        assertNotNull(map.get("threads"));
        assertNotNull(map.get("gc"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> threads = (Map<String, Object>) map.get("threads");
        assertEquals(30, threads.get("count"));
        assertEquals(40, threads.get("peak_count"));
    }

    @Test
    public void testJvmStatsNullMapsDefaultToEmpty() {
        final StatsInfo.JvmStats stats = new StatsInfo.JvmStats(0L, null, 0, 0, null);
        assertNotNull(stats.getMemoryUsage());
        assertTrue(stats.getMemoryUsage().isEmpty());
        assertNotNull(stats.getGcStats());
        assertTrue(stats.getGcStats().isEmpty());
    }

    @Test
    public void testJvmStatsMemoryUsageIsImmutable() {
        final Map<String, Object> mem = new HashMap<>();
        mem.put("heap_used", 1024L);
        final StatsInfo.JvmStats stats = new StatsInfo.JvmStats(0L, mem, 0, 0, null);
        try {
            stats.getMemoryUsage().put("extra", "value");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testJvmStatsEquality() {
        final Map<String, Object> mem = Map.of("heap", 100L);
        final Map<String, Object> gc = Map.of("count", 5L);
        final StatsInfo.JvmStats s1 = new StatsInfo.JvmStats(1000L, mem, 10, 20, gc);
        final StatsInfo.JvmStats s2 = new StatsInfo.JvmStats(1000L, mem, 10, 20, gc);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testJvmStatsToString() {
        final StatsInfo.JvmStats stats = new StatsInfo.JvmStats(5000L, null, 10, 12, null);
        final String str = stats.toString();
        assertTrue(str.contains("5000"));
        assertTrue(str.contains("10"));
    }

    // --- ProcessStats tests ---

    @Test
    public void testProcessStatsConstruction() {
        final StatsInfo.ProcessStats stats = new StatsInfo.ProcessStats(
                256, 300, 65536, (short) 42, 1000000L, 8589934592L);

        assertEquals(256, stats.getOpenFileDescriptors());
        assertEquals(300, stats.getPeakOpenFileDescriptors());
        assertEquals(65536, stats.getMaxFileDescriptors());
        assertEquals(42, stats.getCpuPercent());
        assertEquals(1000000L, stats.getCpuTotalMillis());
        assertEquals(8589934592L, stats.getMemTotalVirtualBytes());
    }

    @Test
    public void testProcessStatsToMap() {
        final StatsInfo.ProcessStats stats = new StatsInfo.ProcessStats(
                100, 150, 1024, (short) 25, 50000L, 4294967296L);

        final Map<String, Object> map = stats.toMap();
        assertEquals(100L, map.get("open_file_descriptors"));
        assertEquals(150L, map.get("peak_open_file_descriptors"));
        assertEquals(1024L, map.get("max_file_descriptors"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> cpu = (Map<String, Object>) map.get("cpu");
        assertEquals((short) 25, cpu.get("percent"));
        assertEquals(50000L, cpu.get("total_in_millis"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> mem = (Map<String, Object>) map.get("mem");
        assertEquals(4294967296L, mem.get("total_virtual_in_bytes"));
    }

    @Test
    public void testProcessStatsEquality() {
        final StatsInfo.ProcessStats p1 = new StatsInfo.ProcessStats(10, 20, 30, (short) 5, 100L, 200L);
        final StatsInfo.ProcessStats p2 = new StatsInfo.ProcessStats(10, 20, 30, (short) 5, 100L, 200L);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testProcessStatsInequality() {
        final StatsInfo.ProcessStats p1 = new StatsInfo.ProcessStats(10, 20, 30, (short) 5, 100L, 200L);
        final StatsInfo.ProcessStats p2 = new StatsInfo.ProcessStats(99, 20, 30, (short) 5, 100L, 200L);
        assertNotEquals(p1, p2);
    }

    @Test
    public void testProcessStatsToString() {
        final StatsInfo.ProcessStats stats = new StatsInfo.ProcessStats(100, 150, 1024, (short) 42, 5000L, 8L);
        final String str = stats.toString();
        assertTrue(str.contains("100"));
        assertTrue(str.contains("42"));
    }

    @Test
    public void testProcessStatsMapIsImmutable() {
        final StatsInfo.ProcessStats stats = new StatsInfo.ProcessStats(1, 2, 3, (short) 4, 5, 6);
        try {
            stats.toMap().put("extra", "value");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    // --- FlowStats tests ---

    @Test
    public void testFlowStatsConstruction() {
        final Map<String, Object> input = Map.of("current", 100.5, "lifetime", 95.2);
        final Map<String, Object> filter = Map.of("current", 90.1, "lifetime", 88.0);
        final Map<String, Object> output = Map.of("current", 85.3, "lifetime", 82.7);
        final Map<String, Object> worker = Map.of("current", 2.5, "lifetime", 2.1);
        final Map<String, Object> backpressure = Map.of("current", 0.1, "lifetime", 0.05);

        final StatsInfo.FlowStats stats = new StatsInfo.FlowStats(input, filter, output, worker, backpressure);

        assertEquals(100.5, stats.getInputThroughput().get("current"));
        assertEquals(90.1, stats.getFilterThroughput().get("current"));
        assertEquals(85.3, stats.getOutputThroughput().get("current"));
        assertEquals(2.5, stats.getWorkerConcurrency().get("current"));
        assertEquals(0.1, stats.getQueueBackpressure().get("current"));
    }

    @Test
    public void testFlowStatsNullMapsDefaultToEmpty() {
        final StatsInfo.FlowStats stats = new StatsInfo.FlowStats(null, null, null, null, null);
        assertNotNull(stats.getInputThroughput());
        assertTrue(stats.getInputThroughput().isEmpty());
        assertNotNull(stats.getFilterThroughput());
        assertTrue(stats.getFilterThroughput().isEmpty());
        assertNotNull(stats.getOutputThroughput());
        assertTrue(stats.getOutputThroughput().isEmpty());
        assertNotNull(stats.getWorkerConcurrency());
        assertTrue(stats.getWorkerConcurrency().isEmpty());
        assertNotNull(stats.getQueueBackpressure());
        assertTrue(stats.getQueueBackpressure().isEmpty());
    }

    @Test
    public void testFlowStatsToMap() {
        final Map<String, Object> input = Map.of("current", 50.0);
        final Map<String, Object> filter = Map.of("current", 45.0);
        final Map<String, Object> output = Map.of("current", 40.0);
        final Map<String, Object> worker = Map.of("current", 1.5);
        final Map<String, Object> bp = Map.of("current", 0.01);

        final StatsInfo.FlowStats stats = new StatsInfo.FlowStats(input, filter, output, worker, bp);
        final Map<String, Object> map = stats.toMap();

        assertEquals(5, map.size());
        assertNotNull(map.get("input_throughput"));
        assertNotNull(map.get("filter_throughput"));
        assertNotNull(map.get("output_throughput"));
        assertNotNull(map.get("worker_concurrency"));
        assertNotNull(map.get("queue_backpressure"));
    }

    @Test
    public void testFlowStatsImmutability() {
        final Map<String, Object> input = new HashMap<>();
        input.put("current", 50.0);
        final StatsInfo.FlowStats stats = new StatsInfo.FlowStats(input, null, null, null, null);
        try {
            stats.getInputThroughput().put("extra", "value");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testFlowStatsEquality() {
        final Map<String, Object> input = Map.of("current", 50.0);
        final StatsInfo.FlowStats s1 = new StatsInfo.FlowStats(input, null, null, null, null);
        final StatsInfo.FlowStats s2 = new StatsInfo.FlowStats(input, null, null, null, null);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testFlowStatsToString() {
        final Map<String, Object> input = Map.of("current", 50.0);
        final Map<String, Object> filter = Map.of("current", 45.0);
        final StatsInfo.FlowStats stats = new StatsInfo.FlowStats(input, filter, null, null, null);
        final String str = stats.toString();
        assertTrue(str.contains("inputThroughput"));
        assertTrue(str.contains("filterThroughput"));
    }

    // --- StatsInfo top-level tests ---

    @Test
    public void testStatsInfoConstruction() {
        final StatsInfo.EventStats events = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo.JvmStats jvm = new StatsInfo.JvmStats(5000L, null, 30, 35, null);
        final StatsInfo.ProcessStats process = new StatsInfo.ProcessStats(100, 150, 1024, (short) 10, 50000L, 4L);
        final StatsInfo.FlowStats flow = new StatsInfo.FlowStats(null, null, null, null, null);

        final StatsInfo info = new StatsInfo(events, jvm, process, flow);
        assertSame(events, info.getEventStats());
        assertSame(jvm, info.getJvmStats());
        assertSame(process, info.getProcessStats());
        assertSame(flow, info.getFlowStats());
    }

    @Test
    public void testStatsInfoToMap() {
        final StatsInfo.EventStats events = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo.JvmStats jvm = new StatsInfo.JvmStats(5000L, null, 30, 35, null);
        final StatsInfo.ProcessStats process = new StatsInfo.ProcessStats(100, 150, 1024, (short) 10, 50000L, 4L);
        final StatsInfo.FlowStats flow = new StatsInfo.FlowStats(null, null, null, null, null);

        final StatsInfo info = new StatsInfo(events, jvm, process, flow);
        final Map<String, Object> map = info.toMap();

        assertTrue(map.containsKey("events"));
        assertTrue(map.containsKey("jvm"));
        assertTrue(map.containsKey("process"));
        assertTrue(map.containsKey("flow"));
        assertEquals(4, map.size());
    }

    @Test
    public void testStatsInfoToMapWithNulls() {
        final StatsInfo info = new StatsInfo(null, null, null, null);
        final Map<String, Object> map = info.toMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testStatsInfoToMapPartial() {
        final StatsInfo.EventStats events = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo info = new StatsInfo(events, null, null, null);
        final Map<String, Object> map = info.toMap();
        assertEquals(1, map.size());
        assertTrue(map.containsKey("events"));
    }

    @Test
    public void testStatsInfoEquality() {
        final StatsInfo.EventStats events = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo s1 = new StatsInfo(events, null, null, null);
        final StatsInfo s2 = new StatsInfo(events, null, null, null);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testStatsInfoInequality() {
        final StatsInfo.EventStats e1 = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo.EventStats e2 = new StatsInfo.EventStats(20, 19, 18, 200, 40);
        final StatsInfo s1 = new StatsInfo(e1, null, null, null);
        final StatsInfo s2 = new StatsInfo(e2, null, null, null);
        assertNotEquals(s1, s2);
    }

    @Test
    public void testStatsInfoToString() {
        final StatsInfo.EventStats events = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo.JvmStats jvm = new StatsInfo.JvmStats(5000L, null, 30, 35, null);
        final StatsInfo info = new StatsInfo(events, jvm, null, null);
        final String str = info.toString();
        assertTrue(str.contains("eventStats"));
        assertTrue(str.contains("jvmStats"));
    }

    @Test
    public void testStatsInfoMapIsImmutable() {
        final StatsInfo.EventStats events = new StatsInfo.EventStats(10, 9, 8, 100, 20);
        final StatsInfo info = new StatsInfo(events, null, null, null);
        try {
            info.toMap().put("extra", "value");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }
}
