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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OsPoller}.
 */
public class OsPollerTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void getName() {
        OsPoller poller = new OsPoller();
        assertThat(poller.getName()).isEqualTo("os");
    }

    @Test
    public void collectMetricsReturnsNonNull() {
        OsPoller poller = new OsPoller();
        OsPoller.OsMetrics metrics = poller.collectMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.getLoadAverage()).isNotNull();
    }

    @Test
    public void collectStoresLastMetrics() {
        OsPoller poller = new OsPoller();
        assertThat(poller.getLastMetrics()).isNull();

        poller.collect();
        assertThat(poller.getLastMetrics()).isNotNull();
    }

    @Test
    public void genericPlatformLoadAverage() {
        // Test non-Linux path by explicitly setting isLinux to false
        OsPoller poller = new OsPoller(false, ManagementFactory.getOperatingSystemMXBean());
        OsPoller.OsMetrics metrics = poller.collectMetrics();

        assertThat(metrics.getLoadAverage()).isNotNull();
        // On macOS, the 1-minute load average should be available
        // On other platforms, it might be null/negative
        // The 5 and 15-minute values should be null for non-Linux
        assertThat(metrics.getLoadAverage().getFiveMinute()).isNull();
        assertThat(metrics.getLoadAverage().getFifteenMinute()).isNull();
    }

    @Test
    public void nonLinuxHasNoCgroupMetrics() {
        OsPoller poller = new OsPoller(false, ManagementFactory.getOperatingSystemMXBean());
        OsPoller.OsMetrics metrics = poller.collectMetrics();

        assertThat(metrics.getCgroupMetrics()).isNull();
    }

    @Test
    public void readLoadAverageFromFile() throws IOException {
        Path loadavg = tempDir.newFile("loadavg").toPath();
        Files.write(loadavg, "0.52 0.34 0.28 1/1234 56789".getBytes());

        OsPoller poller = new OsPoller();
        OsPoller.LoadAverage la = poller.readLoadAverageFromPath(loadavg);

        assertThat(la.getOneMinute()).isEqualTo(0.52);
        assertThat(la.getFiveMinute()).isEqualTo(0.34);
        assertThat(la.getFifteenMinute()).isEqualTo(0.28);
    }

    @Test
    public void readLoadAverageWithDifferentValues() throws IOException {
        Path loadavg = tempDir.newFile("loadavg2").toPath();
        Files.write(loadavg, "1.00 2.50 3.75 5/678 12345".getBytes());

        OsPoller poller = new OsPoller();
        OsPoller.LoadAverage la = poller.readLoadAverageFromPath(loadavg);

        assertThat(la.getOneMinute()).isEqualTo(1.00);
        assertThat(la.getFiveMinute()).isEqualTo(2.50);
        assertThat(la.getFifteenMinute()).isEqualTo(3.75);
    }

    @Test
    public void readLoadAverageFromInvalidFileThrows() throws IOException {
        Path loadavg = tempDir.newFile("bad-loadavg").toPath();
        Files.write(loadavg, "bad data".getBytes());

        OsPoller poller = new OsPoller();

        assertThatThrownBy(() -> poller.readLoadAverageFromPath(loadavg))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unexpected");
    }

    @Test
    public void findCpuControllerPathFromFile() throws IOException {
        Path cgroup = tempDir.newFile("cgroup").toPath();
        Files.write(cgroup, String.join("\n",
                "12:pids:/docker/abc123",
                "4:cpu,cpuacct:/docker/abc123",
                "1:name=systemd:/docker/abc123"
        ).getBytes());

        OsPoller poller = new OsPoller();
        String path = poller.findCpuControllerPath(cgroup);

        assertThat(path).isEqualTo("/docker/abc123");
    }

    @Test
    public void findCpuControllerPathNoCpu() throws IOException {
        Path cgroup = tempDir.newFile("cgroup-nocpu").toPath();
        Files.write(cgroup, String.join("\n",
                "12:pids:/docker/abc123",
                "1:name=systemd:/docker/abc123"
        ).getBytes());

        OsPoller poller = new OsPoller();
        String path = poller.findCpuControllerPath(cgroup);

        assertThat(path).isNull();
    }

    @Test
    public void findCpuControllerPathMissingFile() {
        OsPoller poller = new OsPoller();
        Path nonexistent = tempDir.getRoot().toPath().resolve("does-not-exist");
        String path = poller.findCpuControllerPath(nonexistent);

        assertThat(path).isNull();
    }

    @Test
    public void readCpuStat() throws IOException {
        Path cpuStat = tempDir.newFile("cpu.stat").toPath();
        Files.write(cpuStat, String.join("\n",
                "nr_periods 100",
                "nr_throttled 5",
                "throttled_time 1234567890"
        ).getBytes());

        OsPoller poller = new OsPoller();
        OsPoller.CpuStat stat = poller.readCpuStat(cpuStat);

        assertThat(stat.nrPeriods).isEqualTo(100);
        assertThat(stat.nrThrottled).isEqualTo(5);
        assertThat(stat.throttledTimeNanos).isEqualTo(1234567890L);
    }

    @Test
    public void readLongFromFile() throws IOException {
        Path file = tempDir.newFile("value").toPath();
        Files.write(file, "100000\n".getBytes());

        OsPoller poller = new OsPoller();
        long value = poller.readLongFromFile(file);

        assertThat(value).isEqualTo(100000L);
    }

    @Test
    public void readLongFromFileNegativeValue() throws IOException {
        Path file = tempDir.newFile("negative").toPath();
        Files.write(file, "-1\n".getBytes());

        OsPoller poller = new OsPoller();
        long value = poller.readLongFromFile(file);

        assertThat(value).isEqualTo(-1L);
    }

    @Test
    public void loadAverageImmutability() {
        OsPoller.LoadAverage la = new OsPoller.LoadAverage(1.0, 2.0, 3.0);

        assertThat(la.getOneMinute()).isEqualTo(1.0);
        assertThat(la.getFiveMinute()).isEqualTo(2.0);
        assertThat(la.getFifteenMinute()).isEqualTo(3.0);
    }

    @Test
    public void loadAverageNullValues() {
        OsPoller.LoadAverage la = new OsPoller.LoadAverage(null, null, null);

        assertThat(la.getOneMinute()).isNull();
        assertThat(la.getFiveMinute()).isNull();
        assertThat(la.getFifteenMinute()).isNull();
    }

    @Test
    public void cgroupMetricsImmutability() {
        OsPoller.CgroupMetrics cg = new OsPoller.CgroupMetrics(100000, 200000, 500000000, 50, 3, 1234567);

        assertThat(cg.getCpuCfsPeriodMicros()).isEqualTo(100000);
        assertThat(cg.getCpuCfsQuotaMicros()).isEqualTo(200000);
        assertThat(cg.getCpuUsageNanos()).isEqualTo(500000000);
        assertThat(cg.getNrPeriods()).isEqualTo(50);
        assertThat(cg.getNrThrottled()).isEqualTo(3);
        assertThat(cg.getThrottledTimeNanos()).isEqualTo(1234567);
    }

    @Test
    public void osMetricsImmutability() {
        OsPoller.LoadAverage la = new OsPoller.LoadAverage(1.5, 2.5, 3.5);
        OsPoller.CgroupMetrics cg = new OsPoller.CgroupMetrics(100, 200, 300, 10, 2, 500);
        OsPoller.OsMetrics metrics = new OsPoller.OsMetrics(la, cg);

        assertThat(metrics.getLoadAverage()).isSameAs(la);
        assertThat(metrics.getCgroupMetrics()).isSameAs(cg);
    }

    @Test
    public void osMetricsNullCgroup() {
        OsPoller.LoadAverage la = new OsPoller.LoadAverage(1.0, null, null);
        OsPoller.OsMetrics metrics = new OsPoller.OsMetrics(la, null);

        assertThat(metrics.getLoadAverage()).isNotNull();
        assertThat(metrics.getCgroupMetrics()).isNull();
    }
}
