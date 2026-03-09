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

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OS metrics poller that collects load average and cgroup metrics.
 *
 * <p>Load average is read from /proc/loadavg on Linux, or from
 * {@link OperatingSystemMXBean#getSystemLoadAverage()} on other platforms
 * (which only provides the 1-minute average).</p>
 *
 * <p>Cgroup v1 metrics are collected on Linux only, reading from
 * /sys/fs/cgroup/cpu/ and /sys/fs/cgroup/cpuacct/.</p>
 */
public class OsPoller implements PollerTask {

    private static final Logger LOGGER = LogManager.getLogger(OsPoller.class);

    static final Path PROC_LOADAVG = Paths.get("/proc/loadavg");
    static final Path PROC_SELF_CGROUP = Paths.get("/proc/self/cgroup");
    static final String CGROUP_CPU_BASE = "/sys/fs/cgroup/cpu";
    static final String CGROUP_CPUACCT_BASE = "/sys/fs/cgroup/cpuacct";

    private final boolean isLinux;
    private final OperatingSystemMXBean osBean;

    private volatile OsMetrics lastMetrics;

    /**
     * Creates an OsPoller with platform auto-detection.
     */
    public OsPoller() {
        this(System.getProperty("os.name", "").toLowerCase().contains("linux"),
             ManagementFactory.getOperatingSystemMXBean());
    }

    /**
     * Creates an OsPoller with explicit platform configuration (for testing).
     */
    OsPoller(boolean isLinux, OperatingSystemMXBean osBean) {
        this.isLinux = isLinux;
        this.osBean = osBean;
    }

    @Override
    public void collect() {
        lastMetrics = collectMetrics();
        LOGGER.debug("OS metrics collected successfully");
    }

    @Override
    public String getName() {
        return "os";
    }

    /**
     * Collects and returns an immutable snapshot of OS metrics.
     *
     * @return the collected OS metrics
     */
    public OsMetrics collectMetrics() {
        LoadAverage loadAverage = collectLoadAverage();
        CgroupMetrics cgroupMetrics = collectCgroupMetrics();
        return new OsMetrics(loadAverage, cgroupMetrics);
    }

    /**
     * Returns the most recently collected metrics, or {@code null} if
     * {@link #collect()} has not been called yet.
     *
     * @return the last collected metrics
     */
    public OsMetrics getLastMetrics() {
        return lastMetrics;
    }

    private LoadAverage collectLoadAverage() {
        if (isLinux) {
            return collectLinuxLoadAverage();
        }
        return collectGenericLoadAverage();
    }

    /**
     * Reads load average from /proc/loadavg on Linux.
     * Format: "0.00 0.01 0.05 1/234 56789"
     */
    private LoadAverage collectLinuxLoadAverage() {
        try {
            return readLoadAverageFromPath(PROC_LOADAVG);
        } catch (Exception e) {
            LOGGER.warn("Failed to read /proc/loadavg: {}", e.getMessage());
            return collectGenericLoadAverage();
        }
    }

    /**
     * Reads load average from a given file path (for testing).
     */
    LoadAverage readLoadAverageFromPath(Path path) throws IOException {
        String content = new String(Files.readAllBytes(path)).trim();
        String[] parts = content.split("\\s+");
        if (parts.length >= 3) {
            Double one = parseDoubleSafe(parts[0]);
            Double five = parseDoubleSafe(parts[1]);
            Double fifteen = parseDoubleSafe(parts[2]);
            return new LoadAverage(one, five, fifteen);
        }
        throw new IOException("Unexpected /proc/loadavg format: " + content);
    }

    private LoadAverage collectGenericLoadAverage() {
        double loadAvg = osBean.getSystemLoadAverage();
        if (loadAvg >= 0) {
            return new LoadAverage(loadAvg, null, null);
        }
        return new LoadAverage(null, null, null);
    }

    private CgroupMetrics collectCgroupMetrics() {
        if (!isLinux) {
            return null;
        }

        try {
            String cpuControllerPath = findCpuControllerPath();
            if (cpuControllerPath == null) {
                return null;
            }

            String cpuBase = CGROUP_CPU_BASE + cpuControllerPath;
            String cpuacctBase = CGROUP_CPUACCT_BASE + cpuControllerPath;

            long cfsPeriodMicros = readLongFromFile(Paths.get(cpuBase, "cpu.cfs_period_us"));
            long cfsQuotaMicros = readLongFromFile(Paths.get(cpuBase, "cpu.cfs_quota_us"));
            long cpuUsageNanos = readLongFromFile(Paths.get(cpuacctBase, "cpuacct.usage"));

            CpuStat cpuStat = readCpuStat(Paths.get(cpuBase, "cpu.stat"));

            return new CgroupMetrics(cfsPeriodMicros, cfsQuotaMicros, cpuUsageNanos,
                    cpuStat.nrPeriods, cpuStat.nrThrottled, cpuStat.throttledTimeNanos);
        } catch (Exception e) {
            LOGGER.debug("Cgroup metrics not available: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Finds the CPU controller path from /proc/self/cgroup.
     * Looks for lines like: "4:cpu,cpuacct:/docker/abc123"
     *
     * @return the cgroup path for the CPU controller, or null if not found
     */
    String findCpuControllerPath() {
        return findCpuControllerPath(PROC_SELF_CGROUP);
    }

    /**
     * Finds the CPU controller path from a given cgroup file (for testing).
     */
    String findCpuControllerPath(Path cgroupFile) {
        try (BufferedReader reader = Files.newBufferedReader(cgroupFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    String controllers = parts[1];
                    if (controllers.contains("cpu")) {
                        return parts[2];
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Cannot read cgroup file {}: {}", cgroupFile, e.getMessage());
        }
        return null;
    }

    long readLongFromFile(Path path) throws IOException {
        String content = new String(Files.readAllBytes(path)).trim();
        return Long.parseLong(content);
    }

    CpuStat readCpuStat(Path path) throws IOException {
        long nrPeriods = 0;
        long nrThrottled = 0;
        long throttledTime = 0;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    switch (parts[0]) {
                        case "nr_periods":
                            nrPeriods = Long.parseLong(parts[1]);
                            break;
                        case "nr_throttled":
                            nrThrottled = Long.parseLong(parts[1]);
                            break;
                        case "throttled_time":
                            throttledTime = Long.parseLong(parts[1]);
                            break;
                    }
                }
            }
        }
        return new CpuStat(nrPeriods, nrThrottled, throttledTime);
    }

    private static Double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ----- Internal helper class -----

    static class CpuStat {
        final long nrPeriods;
        final long nrThrottled;
        final long throttledTimeNanos;

        CpuStat(long nrPeriods, long nrThrottled, long throttledTimeNanos) {
            this.nrPeriods = nrPeriods;
            this.nrThrottled = nrThrottled;
            this.throttledTimeNanos = throttledTimeNanos;
        }
    }

    // ----- Immutable data holder classes -----

    /**
     * Immutable snapshot of all OS metrics.
     */
    public static final class OsMetrics {
        private final LoadAverage loadAverage;
        private final CgroupMetrics cgroupMetrics;

        public OsMetrics(LoadAverage loadAverage, CgroupMetrics cgroupMetrics) {
            this.loadAverage = loadAverage;
            this.cgroupMetrics = cgroupMetrics;
        }

        public LoadAverage getLoadAverage() { return loadAverage; }

        /**
         * Returns cgroup metrics, or {@code null} if not on Linux or cgroups
         * are not available.
         */
        public CgroupMetrics getCgroupMetrics() { return cgroupMetrics; }
    }

    /**
     * Immutable load average metrics. Values may be {@code null} if not
     * available on the platform.
     */
    public static final class LoadAverage {
        private final Double oneMinute;
        private final Double fiveMinute;
        private final Double fifteenMinute;

        public LoadAverage(Double oneMinute, Double fiveMinute, Double fifteenMinute) {
            this.oneMinute = oneMinute;
            this.fiveMinute = fiveMinute;
            this.fifteenMinute = fifteenMinute;
        }

        public Double getOneMinute() { return oneMinute; }
        public Double getFiveMinute() { return fiveMinute; }
        public Double getFifteenMinute() { return fifteenMinute; }
    }

    /**
     * Immutable cgroup v1 CPU metrics.
     */
    public static final class CgroupMetrics {
        private final long cpuCfsPeriodMicros;
        private final long cpuCfsQuotaMicros;
        private final long cpuUsageNanos;
        private final long nrPeriods;
        private final long nrThrottled;
        private final long throttledTimeNanos;

        public CgroupMetrics(long cpuCfsPeriodMicros, long cpuCfsQuotaMicros, long cpuUsageNanos,
                             long nrPeriods, long nrThrottled, long throttledTimeNanos) {
            this.cpuCfsPeriodMicros = cpuCfsPeriodMicros;
            this.cpuCfsQuotaMicros = cpuCfsQuotaMicros;
            this.cpuUsageNanos = cpuUsageNanos;
            this.nrPeriods = nrPeriods;
            this.nrThrottled = nrThrottled;
            this.throttledTimeNanos = throttledTimeNanos;
        }

        public long getCpuCfsPeriodMicros() { return cpuCfsPeriodMicros; }
        public long getCpuCfsQuotaMicros() { return cpuCfsQuotaMicros; }
        public long getCpuUsageNanos() { return cpuUsageNanos; }
        public long getNrPeriods() { return nrPeriods; }
        public long getNrThrottled() { return nrThrottled; }
        public long getThrottledTimeNanos() { return throttledTimeNanos; }
    }
}
