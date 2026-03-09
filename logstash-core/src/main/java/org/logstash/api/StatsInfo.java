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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java holder for Logstash statistics, used by the web API.
 * Contains inner classes for event, JVM, process, and flow statistics.
 */
public final class StatsInfo {

    private final EventStats eventStats;
    private final JvmStats jvmStats;
    private final ProcessStats processStats;
    private final FlowStats flowStats;

    public StatsInfo(final EventStats eventStats, final JvmStats jvmStats,
                     final ProcessStats processStats, final FlowStats flowStats) {
        this.eventStats = eventStats;
        this.jvmStats = jvmStats;
        this.processStats = processStats;
        this.flowStats = flowStats;
    }

    public EventStats getEventStats() { return eventStats; }
    public JvmStats getJvmStats() { return jvmStats; }
    public ProcessStats getProcessStats() { return processStats; }
    public FlowStats getFlowStats() { return flowStats; }

    /**
     * Converts this StatsInfo to a Map suitable for JSON serialization.
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<>();
        if (eventStats != null) {
            map.put("events", eventStats.toMap());
        }
        if (jvmStats != null) {
            map.put("jvm", jvmStats.toMap());
        }
        if (processStats != null) {
            map.put("process", processStats.toMap());
        }
        if (flowStats != null) {
            map.put("flow", flowStats.toMap());
        }
        return Collections.unmodifiableMap(map);
    }

    // --- EventStats ---

    /**
     * Event processing statistics.
     */
    public static final class EventStats {
        private final long in;
        private final long filtered;
        private final long out;
        private final long durationMillis;
        private final long queuePushDurationMillis;

        public EventStats(final long in, final long filtered, final long out,
                          final long durationMillis, final long queuePushDurationMillis) {
            this.in = in;
            this.filtered = filtered;
            this.out = out;
            this.durationMillis = durationMillis;
            this.queuePushDurationMillis = queuePushDurationMillis;
        }

        public long getIn() { return in; }
        public long getFiltered() { return filtered; }
        public long getOut() { return out; }
        public long getDurationMillis() { return durationMillis; }
        public long getQueuePushDurationMillis() { return queuePushDurationMillis; }

        public Map<String, Object> toMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("in", in);
            map.put("filtered", filtered);
            map.put("out", out);
            map.put("duration_in_millis", durationMillis);
            map.put("queue_push_duration_in_millis", queuePushDurationMillis);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final EventStats that = (EventStats) o;
            return in == that.in && filtered == that.filtered && out == that.out
                    && durationMillis == that.durationMillis
                    && queuePushDurationMillis == that.queuePushDurationMillis;
        }

        @Override
        public int hashCode() {
            return Objects.hash(in, filtered, out, durationMillis, queuePushDurationMillis);
        }

        @Override
        public String toString() {
            return "EventStats{in=" + in + ", filtered=" + filtered + ", out=" + out + "}";
        }
    }

    // --- JvmStats ---

    /**
     * JVM runtime statistics.
     */
    public static final class JvmStats {
        private final long uptimeMillis;
        private final Map<String, Object> memoryUsage;
        private final int threadCount;
        private final int peakThreadCount;
        private final Map<String, Object> gcStats;

        public JvmStats(final long uptimeMillis, final Map<String, Object> memoryUsage,
                        final int threadCount, final int peakThreadCount,
                        final Map<String, Object> gcStats) {
            this.uptimeMillis = uptimeMillis;
            this.memoryUsage = memoryUsage != null
                    ? Collections.unmodifiableMap(new LinkedHashMap<>(memoryUsage))
                    : Collections.emptyMap();
            this.threadCount = threadCount;
            this.peakThreadCount = peakThreadCount;
            this.gcStats = gcStats != null
                    ? Collections.unmodifiableMap(new LinkedHashMap<>(gcStats))
                    : Collections.emptyMap();
        }

        public long getUptimeMillis() { return uptimeMillis; }
        public Map<String, Object> getMemoryUsage() { return memoryUsage; }
        public int getThreadCount() { return threadCount; }
        public int getPeakThreadCount() { return peakThreadCount; }
        public Map<String, Object> getGcStats() { return gcStats; }

        public Map<String, Object> toMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("uptime_in_millis", uptimeMillis);
            map.put("mem", memoryUsage);
            map.put("threads", buildThreadMap());
            map.put("gc", gcStats);
            return Collections.unmodifiableMap(map);
        }

        private Map<String, Object> buildThreadMap() {
            final Map<String, Object> threadMap = new LinkedHashMap<>();
            threadMap.put("count", threadCount);
            threadMap.put("peak_count", peakThreadCount);
            return Collections.unmodifiableMap(threadMap);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final JvmStats that = (JvmStats) o;
            return uptimeMillis == that.uptimeMillis
                    && threadCount == that.threadCount
                    && peakThreadCount == that.peakThreadCount
                    && Objects.equals(memoryUsage, that.memoryUsage)
                    && Objects.equals(gcStats, that.gcStats);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uptimeMillis, memoryUsage, threadCount, peakThreadCount, gcStats);
        }

        @Override
        public String toString() {
            return "JvmStats{uptimeMillis=" + uptimeMillis + ", threads=" + threadCount + "}";
        }
    }

    // --- ProcessStats ---

    /**
     * OS process statistics.
     */
    public static final class ProcessStats {
        private final long openFileDescriptors;
        private final long peakOpenFileDescriptors;
        private final long maxFileDescriptors;
        private final short cpuPercent;
        private final long cpuTotalMillis;
        private final long memTotalVirtualBytes;

        public ProcessStats(final long openFileDescriptors, final long peakOpenFileDescriptors,
                            final long maxFileDescriptors, final short cpuPercent,
                            final long cpuTotalMillis, final long memTotalVirtualBytes) {
            this.openFileDescriptors = openFileDescriptors;
            this.peakOpenFileDescriptors = peakOpenFileDescriptors;
            this.maxFileDescriptors = maxFileDescriptors;
            this.cpuPercent = cpuPercent;
            this.cpuTotalMillis = cpuTotalMillis;
            this.memTotalVirtualBytes = memTotalVirtualBytes;
        }

        public long getOpenFileDescriptors() { return openFileDescriptors; }
        public long getPeakOpenFileDescriptors() { return peakOpenFileDescriptors; }
        public long getMaxFileDescriptors() { return maxFileDescriptors; }
        public short getCpuPercent() { return cpuPercent; }
        public long getCpuTotalMillis() { return cpuTotalMillis; }
        public long getMemTotalVirtualBytes() { return memTotalVirtualBytes; }

        public Map<String, Object> toMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("open_file_descriptors", openFileDescriptors);
            map.put("peak_open_file_descriptors", peakOpenFileDescriptors);
            map.put("max_file_descriptors", maxFileDescriptors);
            final Map<String, Object> cpuMap = new LinkedHashMap<>();
            cpuMap.put("percent", cpuPercent);
            cpuMap.put("total_in_millis", cpuTotalMillis);
            map.put("cpu", Collections.unmodifiableMap(cpuMap));
            final Map<String, Object> memMap = new LinkedHashMap<>();
            memMap.put("total_virtual_in_bytes", memTotalVirtualBytes);
            map.put("mem", Collections.unmodifiableMap(memMap));
            return Collections.unmodifiableMap(map);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ProcessStats that = (ProcessStats) o;
            return openFileDescriptors == that.openFileDescriptors
                    && peakOpenFileDescriptors == that.peakOpenFileDescriptors
                    && maxFileDescriptors == that.maxFileDescriptors
                    && cpuPercent == that.cpuPercent
                    && cpuTotalMillis == that.cpuTotalMillis
                    && memTotalVirtualBytes == that.memTotalVirtualBytes;
        }

        @Override
        public int hashCode() {
            return Objects.hash(openFileDescriptors, peakOpenFileDescriptors, maxFileDescriptors,
                    cpuPercent, cpuTotalMillis, memTotalVirtualBytes);
        }

        @Override
        public String toString() {
            return "ProcessStats{openFDs=" + openFileDescriptors + ", cpuPercent=" + cpuPercent + "}";
        }
    }

    // --- FlowStats ---

    /**
     * Flow metrics statistics for throughput, concurrency, and backpressure.
     */
    public static final class FlowStats {
        private final Map<String, Object> inputThroughput;
        private final Map<String, Object> filterThroughput;
        private final Map<String, Object> outputThroughput;
        private final Map<String, Object> workerConcurrency;
        private final Map<String, Object> queueBackpressure;

        public FlowStats(final Map<String, Object> inputThroughput,
                         final Map<String, Object> filterThroughput,
                         final Map<String, Object> outputThroughput,
                         final Map<String, Object> workerConcurrency,
                         final Map<String, Object> queueBackpressure) {
            this.inputThroughput = copyOrEmpty(inputThroughput);
            this.filterThroughput = copyOrEmpty(filterThroughput);
            this.outputThroughput = copyOrEmpty(outputThroughput);
            this.workerConcurrency = copyOrEmpty(workerConcurrency);
            this.queueBackpressure = copyOrEmpty(queueBackpressure);
        }

        private static Map<String, Object> copyOrEmpty(final Map<String, Object> source) {
            return source != null
                    ? Collections.unmodifiableMap(new LinkedHashMap<>(source))
                    : Collections.emptyMap();
        }

        public Map<String, Object> getInputThroughput() { return inputThroughput; }
        public Map<String, Object> getFilterThroughput() { return filterThroughput; }
        public Map<String, Object> getOutputThroughput() { return outputThroughput; }
        public Map<String, Object> getWorkerConcurrency() { return workerConcurrency; }
        public Map<String, Object> getQueueBackpressure() { return queueBackpressure; }

        public Map<String, Object> toMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("input_throughput", inputThroughput);
            map.put("filter_throughput", filterThroughput);
            map.put("output_throughput", outputThroughput);
            map.put("worker_concurrency", workerConcurrency);
            map.put("queue_backpressure", queueBackpressure);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final FlowStats that = (FlowStats) o;
            return Objects.equals(inputThroughput, that.inputThroughput)
                    && Objects.equals(filterThroughput, that.filterThroughput)
                    && Objects.equals(outputThroughput, that.outputThroughput)
                    && Objects.equals(workerConcurrency, that.workerConcurrency)
                    && Objects.equals(queueBackpressure, that.queueBackpressure);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inputThroughput, filterThroughput, outputThroughput,
                    workerConcurrency, queueBackpressure);
        }

        @Override
        public String toString() {
            return "FlowStats{inputThroughput=" + inputThroughput +
                    ", filterThroughput=" + filterThroughput + "}";
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final StatsInfo that = (StatsInfo) o;
        return Objects.equals(eventStats, that.eventStats)
                && Objects.equals(jvmStats, that.jvmStats)
                && Objects.equals(processStats, that.processStats)
                && Objects.equals(flowStats, that.flowStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventStats, jvmStats, processStats, flowStats);
    }

    @Override
    public String toString() {
        return "StatsInfo{eventStats=" + eventStats + ", jvmStats=" + jvmStats + "}";
    }
}
