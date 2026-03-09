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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JVM metrics poller that collects heap, non-heap, GC, thread, process,
 * and uptime metrics using standard JMX MXBeans.
 */
public class JvmPoller implements PollerTask {

    private static final Logger LOGGER = LogManager.getLogger(JvmPoller.class);

    /** Collector names typically associated with young-generation GC. */
    private static final Set<String> YOUNG_GC_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "Copy", "PS Scavenge", "ParNew", "G1 Young Generation",
            "ZGC Minor Pauses", "ZGC Minor Cycles",
            "Shenandoah Pauses"
    )));

    /** Collector names typically associated with old-generation GC. */
    private static final Set<String> OLD_GC_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "MarkSweepCompact", "PS MarkSweep", "ConcurrentMarkSweep",
            "G1 Old Generation", "G1 Mixed Generation",
            "ZGC Major Pauses", "ZGC Major Cycles",
            "Shenandoah Cycles"
    )));

    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final RuntimeMXBean runtimeBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final OperatingSystemMXBean osBean;

    private volatile JvmMetrics lastMetrics;

    /**
     * Creates a JvmPoller using the default ManagementFactory MXBeans.
     */
    public JvmPoller() {
        this(ManagementFactory.getMemoryMXBean(),
             ManagementFactory.getThreadMXBean(),
             ManagementFactory.getRuntimeMXBean(),
             ManagementFactory.getGarbageCollectorMXBeans(),
             ManagementFactory.getOperatingSystemMXBean());
    }

    /**
     * Creates a JvmPoller with explicit MXBean dependencies (for testing).
     */
    JvmPoller(MemoryMXBean memoryBean, ThreadMXBean threadBean,
              RuntimeMXBean runtimeBean, List<GarbageCollectorMXBean> gcBeans,
              OperatingSystemMXBean osBean) {
        this.memoryBean = memoryBean;
        this.threadBean = threadBean;
        this.runtimeBean = runtimeBean;
        this.gcBeans = gcBeans;
        this.osBean = osBean;
    }

    @Override
    public void collect() {
        lastMetrics = collectMetrics();
        LOGGER.debug("JVM metrics collected successfully");
    }

    @Override
    public String getName() {
        return "jvm";
    }

    /**
     * Collects and returns an immutable snapshot of JVM metrics.
     *
     * @return the collected JVM metrics
     */
    public JvmMetrics collectMetrics() {
        HeapMetrics heap = collectHeapMetrics();
        NonHeapMetrics nonHeap = collectNonHeapMetrics();
        Map<String, GcMetrics> gc = collectGcMetrics();
        ThreadMetrics threads = collectThreadMetrics();
        ProcessMetrics process = collectProcessMetrics();
        long uptimeMillis = runtimeBean.getUptime();

        return new JvmMetrics(heap, nonHeap, gc, threads, process, uptimeMillis);
    }

    /**
     * Returns the most recently collected metrics, or {@code null} if
     * {@link #collect()} has not been called yet.
     *
     * @return the last collected metrics
     */
    public JvmMetrics getLastMetrics() {
        return lastMetrics;
    }

    /**
     * Classifies a GC collector name as "young" or "old".
     *
     * @param collectorName the JVM GC collector name
     * @return "young", "old", or "unknown"
     */
    public static String classifyGcCollector(String collectorName) {
        if (YOUNG_GC_NAMES.contains(collectorName)) {
            return "young";
        }
        if (OLD_GC_NAMES.contains(collectorName)) {
            return "old";
        }
        return "unknown";
    }

    private HeapMetrics collectHeapMetrics() {
        MemoryUsage usage = memoryBean.getHeapMemoryUsage();
        return new HeapMetrics(usage.getUsed(), usage.getCommitted(), usage.getMax());
    }

    private NonHeapMetrics collectNonHeapMetrics() {
        MemoryUsage usage = memoryBean.getNonHeapMemoryUsage();
        return new NonHeapMetrics(usage.getUsed(), usage.getCommitted(), usage.getMax());
    }

    private Map<String, GcMetrics> collectGcMetrics() {
        Map<String, GcMetrics> result = new HashMap<>();
        for (GarbageCollectorMXBean gc : gcBeans) {
            result.put(gc.getName(), new GcMetrics(gc.getCollectionCount(), gc.getCollectionTime()));
        }
        return Collections.unmodifiableMap(result);
    }

    private ThreadMetrics collectThreadMetrics() {
        return new ThreadMetrics(threadBean.getThreadCount(), threadBean.getPeakThreadCount());
    }

    private ProcessMetrics collectProcessMetrics() {
        long openFds = -1;
        long maxFds = -1;
        short cpuPercent = -1;
        long cpuTotalMillis = -1;
        long totalVirtualMemoryBytes = -1;

        try {
            if (osBean instanceof com.sun.management.UnixOperatingSystemMXBean) {
                com.sun.management.UnixOperatingSystemMXBean unixBean =
                        (com.sun.management.UnixOperatingSystemMXBean) osBean;
                openFds = unixBean.getOpenFileDescriptorCount();
                maxFds = unixBean.getMaxFileDescriptorCount();
                cpuTotalMillis = TimeUnit.MILLISECONDS.convert(
                        unixBean.getProcessCpuTime(), TimeUnit.NANOSECONDS);
                double cpuLoad = unixBean.getProcessCpuLoad();
                cpuPercent = cpuLoad >= 0 ? (short) (cpuLoad * 100) : -1;
                totalVirtualMemoryBytes = unixBean.getCommittedVirtualMemorySize();
            } else if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;
                cpuTotalMillis = TimeUnit.MILLISECONDS.convert(
                        sunBean.getProcessCpuTime(), TimeUnit.NANOSECONDS);
                double cpuLoad = sunBean.getProcessCpuLoad();
                cpuPercent = cpuLoad >= 0 ? (short) (cpuLoad * 100) : -1;
                totalVirtualMemoryBytes = sunBean.getCommittedVirtualMemorySize();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to collect process metrics: {}", e.getMessage());
        }

        return new ProcessMetrics(openFds, maxFds, cpuPercent, cpuTotalMillis, totalVirtualMemoryBytes);
    }

    // ----- Immutable data holder classes -----

    /**
     * Immutable snapshot of all JVM metrics.
     */
    public static final class JvmMetrics {
        private final HeapMetrics heap;
        private final NonHeapMetrics nonHeap;
        private final Map<String, GcMetrics> gc;
        private final ThreadMetrics threads;
        private final ProcessMetrics process;
        private final long uptimeMillis;

        public JvmMetrics(HeapMetrics heap, NonHeapMetrics nonHeap, Map<String, GcMetrics> gc,
                          ThreadMetrics threads, ProcessMetrics process, long uptimeMillis) {
            this.heap = heap;
            this.nonHeap = nonHeap;
            this.gc = gc;
            this.threads = threads;
            this.process = process;
            this.uptimeMillis = uptimeMillis;
        }

        public HeapMetrics getHeap() { return heap; }
        public NonHeapMetrics getNonHeap() { return nonHeap; }
        public Map<String, GcMetrics> getGc() { return gc; }
        public ThreadMetrics getThreads() { return threads; }
        public ProcessMetrics getProcess() { return process; }
        public long getUptimeMillis() { return uptimeMillis; }
    }

    /**
     * Immutable heap memory metrics.
     */
    public static final class HeapMetrics {
        private final long usedBytes;
        private final long committedBytes;
        private final long maxBytes;

        public HeapMetrics(long usedBytes, long committedBytes, long maxBytes) {
            this.usedBytes = usedBytes;
            this.committedBytes = committedBytes;
            this.maxBytes = maxBytes;
        }

        public long getUsedBytes() { return usedBytes; }
        public long getCommittedBytes() { return committedBytes; }
        public long getMaxBytes() { return maxBytes; }
    }

    /**
     * Immutable non-heap memory metrics.
     */
    public static final class NonHeapMetrics {
        private final long usedBytes;
        private final long committedBytes;
        private final long maxBytes;

        public NonHeapMetrics(long usedBytes, long committedBytes, long maxBytes) {
            this.usedBytes = usedBytes;
            this.committedBytes = committedBytes;
            this.maxBytes = maxBytes;
        }

        public long getUsedBytes() { return usedBytes; }
        public long getCommittedBytes() { return committedBytes; }
        public long getMaxBytes() { return maxBytes; }
    }

    /**
     * Immutable GC metrics for a single garbage collector.
     */
    public static final class GcMetrics {
        private final long collectionCount;
        private final long collectionTimeMillis;

        public GcMetrics(long collectionCount, long collectionTimeMillis) {
            this.collectionCount = collectionCount;
            this.collectionTimeMillis = collectionTimeMillis;
        }

        public long getCollectionCount() { return collectionCount; }
        public long getCollectionTimeMillis() { return collectionTimeMillis; }
    }

    /**
     * Immutable thread metrics.
     */
    public static final class ThreadMetrics {
        private final int count;
        private final int peakCount;

        public ThreadMetrics(int count, int peakCount) {
            this.count = count;
            this.peakCount = peakCount;
        }

        public int getCount() { return count; }
        public int getPeakCount() { return peakCount; }
    }

    /**
     * Immutable process metrics.
     */
    public static final class ProcessMetrics {
        private final long openFileDescriptors;
        private final long maxFileDescriptors;
        private final short cpuPercent;
        private final long cpuTotalMillis;
        private final long totalVirtualMemoryBytes;

        public ProcessMetrics(long openFileDescriptors, long maxFileDescriptors,
                              short cpuPercent, long cpuTotalMillis, long totalVirtualMemoryBytes) {
            this.openFileDescriptors = openFileDescriptors;
            this.maxFileDescriptors = maxFileDescriptors;
            this.cpuPercent = cpuPercent;
            this.cpuTotalMillis = cpuTotalMillis;
            this.totalVirtualMemoryBytes = totalVirtualMemoryBytes;
        }

        public long getOpenFileDescriptors() { return openFileDescriptors; }
        public long getMaxFileDescriptors() { return maxFileDescriptors; }
        public short getCpuPercent() { return cpuPercent; }
        public long getCpuTotalMillis() { return cpuTotalMillis; }
        public long getTotalVirtualMemoryBytes() { return totalVirtualMemoryBytes; }
    }
}
