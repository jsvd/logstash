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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java holder for Logstash node information, used by the web API.
 */
public final class NodeInfo {

    private final String id;
    private final String name;
    private final String ephemeralId;
    private final String version;
    private final String httpAddress;
    private final String host;
    private final String status;
    private final OsInfo osInfo;
    private final JvmInfo jvmInfo;

    private NodeInfo(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.ephemeralId = builder.ephemeralId;
        this.version = builder.version;
        this.httpAddress = builder.httpAddress;
        this.host = builder.host;
        this.status = builder.status;
        this.osInfo = builder.osInfo;
        this.jvmInfo = builder.jvmInfo;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEphemeralId() { return ephemeralId; }
    public String getVersion() { return version; }
    public String getHttpAddress() { return httpAddress; }
    public String getHost() { return host; }
    public String getStatus() { return status; }
    public OsInfo getOsInfo() { return osInfo; }
    public JvmInfo getJvmInfo() { return jvmInfo; }

    /**
     * Converts this NodeInfo to a Map suitable for JSON serialization.
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("ephemeral_id", ephemeralId);
        map.put("version", version);
        map.put("http_address", httpAddress);
        map.put("host", host);
        map.put("status", status);
        if (osInfo != null) {
            map.put("os", osInfo.toMap());
        }
        if (jvmInfo != null) {
            map.put("jvm", jvmInfo.toMap());
        }
        return Collections.unmodifiableMap(map);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Factory method to create a NodeInfo from individual parameters.
     */
    public static NodeInfo from(final String id, final String name, final String ephemeralId,
                                final String version, final String httpAddress,
                                final String host, final String status,
                                final OsInfo osInfo, final JvmInfo jvmInfo) {
        return builder()
                .id(id).name(name).ephemeralId(ephemeralId)
                .version(version).httpAddress(httpAddress)
                .host(host).status(status)
                .osInfo(osInfo).jvmInfo(jvmInfo)
                .build();
    }

    // --- Builder ---

    public static final class Builder {
        private String id;
        private String name;
        private String ephemeralId;
        private String version;
        private String httpAddress;
        private String host;
        private String status;
        private OsInfo osInfo;
        private JvmInfo jvmInfo;

        private Builder() {}

        public Builder id(final String id) { this.id = id; return this; }
        public Builder name(final String name) { this.name = name; return this; }
        public Builder ephemeralId(final String ephemeralId) { this.ephemeralId = ephemeralId; return this; }
        public Builder version(final String version) { this.version = version; return this; }
        public Builder httpAddress(final String httpAddress) { this.httpAddress = httpAddress; return this; }
        public Builder host(final String host) { this.host = host; return this; }
        public Builder status(final String status) { this.status = status; return this; }
        public Builder osInfo(final OsInfo osInfo) { this.osInfo = osInfo; return this; }
        public Builder jvmInfo(final JvmInfo jvmInfo) { this.jvmInfo = jvmInfo; return this; }

        public NodeInfo build() {
            return new NodeInfo(this);
        }
    }

    // --- OsInfo ---

    /**
     * Operating system information.
     */
    public static final class OsInfo {
        private final String name;
        private final String arch;
        private final String version;
        private final int availableProcessors;

        public OsInfo(final String name, final String arch, final String version, final int availableProcessors) {
            this.name = name;
            this.arch = arch;
            this.version = version;
            this.availableProcessors = availableProcessors;
        }

        public String getName() { return name; }
        public String getArch() { return arch; }
        public String getVersion() { return version; }
        public int getAvailableProcessors() { return availableProcessors; }

        /**
         * Factory method to create OsInfo from individual parameters.
         */
        public static OsInfo from(final String name, final String arch, final String version, final int availableProcessors) {
            return new OsInfo(name, arch, version, availableProcessors);
        }

        public Map<String, Object> toMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", name);
            map.put("arch", arch);
            map.put("version", version);
            map.put("available_processors", availableProcessors);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final OsInfo osInfo = (OsInfo) o;
            return availableProcessors == osInfo.availableProcessors
                    && Objects.equals(name, osInfo.name)
                    && Objects.equals(arch, osInfo.arch)
                    && Objects.equals(version, osInfo.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arch, version, availableProcessors);
        }
    }

    // --- JvmInfo ---

    /**
     * JVM information including memory and GC details.
     */
    public static final class JvmInfo {
        private final long pid;
        private final String version;
        private final String vmVersion;
        private final String vmVendor;
        private final String vmName;
        private final long startTimeMillis;
        private final JvmMemInfo memInfo;
        private final List<String> gcCollectors;

        private JvmInfo(final JvmInfoBuilder builder) {
            this.pid = builder.pid;
            this.version = builder.version;
            this.vmVersion = builder.vmVersion;
            this.vmVendor = builder.vmVendor;
            this.vmName = builder.vmName;
            this.startTimeMillis = builder.startTimeMillis;
            this.memInfo = builder.memInfo;
            this.gcCollectors = builder.gcCollectors != null
                    ? Collections.unmodifiableList(builder.gcCollectors)
                    : Collections.emptyList();
        }

        public long getPid() { return pid; }
        public String getVersion() { return version; }
        public String getVmVersion() { return vmVersion; }
        public String getVmVendor() { return vmVendor; }
        public String getVmName() { return vmName; }
        public long getStartTimeMillis() { return startTimeMillis; }
        public JvmMemInfo getMemInfo() { return memInfo; }
        public List<String> getGcCollectors() { return gcCollectors; }

        /**
         * Factory method to create JvmInfo from individual parameters.
         */
        public static JvmInfo from(final long pid, final String version, final String vmVersion,
                                   final String vmVendor, final String vmName,
                                   final long startTimeMillis, final JvmMemInfo memInfo,
                                   final List<String> gcCollectors) {
            return builder()
                    .pid(pid).version(version).vmVersion(vmVersion)
                    .vmVendor(vmVendor).vmName(vmName)
                    .startTimeMillis(startTimeMillis)
                    .memInfo(memInfo).gcCollectors(gcCollectors)
                    .build();
        }

        public static JvmInfoBuilder builder() {
            return new JvmInfoBuilder();
        }

        public Map<String, Object> toMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("pid", pid);
            map.put("version", version);
            map.put("vm_version", vmVersion);
            map.put("vm_vendor", vmVendor);
            map.put("vm_name", vmName);
            map.put("start_time_in_millis", startTimeMillis);
            if (memInfo != null) {
                map.put("mem", memInfo.toMap());
            }
            map.put("gc_collectors", gcCollectors);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final JvmInfo jvmInfo = (JvmInfo) o;
            return pid == jvmInfo.pid
                    && startTimeMillis == jvmInfo.startTimeMillis
                    && Objects.equals(version, jvmInfo.version)
                    && Objects.equals(vmVersion, jvmInfo.vmVersion)
                    && Objects.equals(vmVendor, jvmInfo.vmVendor)
                    && Objects.equals(vmName, jvmInfo.vmName)
                    && Objects.equals(memInfo, jvmInfo.memInfo)
                    && Objects.equals(gcCollectors, jvmInfo.gcCollectors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pid, version, vmVersion, vmVendor, vmName, startTimeMillis, memInfo, gcCollectors);
        }

        public static final class JvmInfoBuilder {
            private long pid;
            private String version;
            private String vmVersion;
            private String vmVendor;
            private String vmName;
            private long startTimeMillis;
            private JvmMemInfo memInfo;
            private List<String> gcCollectors;

            private JvmInfoBuilder() {}

            public JvmInfoBuilder pid(final long pid) { this.pid = pid; return this; }
            public JvmInfoBuilder version(final String version) { this.version = version; return this; }
            public JvmInfoBuilder vmVersion(final String vmVersion) { this.vmVersion = vmVersion; return this; }
            public JvmInfoBuilder vmVendor(final String vmVendor) { this.vmVendor = vmVendor; return this; }
            public JvmInfoBuilder vmName(final String vmName) { this.vmName = vmName; return this; }
            public JvmInfoBuilder startTimeMillis(final long startTimeMillis) { this.startTimeMillis = startTimeMillis; return this; }
            public JvmInfoBuilder memInfo(final JvmMemInfo memInfo) { this.memInfo = memInfo; return this; }
            public JvmInfoBuilder gcCollectors(final List<String> gcCollectors) { this.gcCollectors = gcCollectors; return this; }

            public JvmInfo build() {
                return new JvmInfo(this);
            }
        }
    }

    // --- JvmMemInfo ---

    /**
     * JVM memory information.
     */
    public static final class JvmMemInfo {
        private final long heapInitBytes;
        private final long heapMaxBytes;
        private final long nonHeapInitBytes;
        private final long nonHeapMaxBytes;

        public JvmMemInfo(final long heapInitBytes, final long heapMaxBytes,
                          final long nonHeapInitBytes, final long nonHeapMaxBytes) {
            this.heapInitBytes = heapInitBytes;
            this.heapMaxBytes = heapMaxBytes;
            this.nonHeapInitBytes = nonHeapInitBytes;
            this.nonHeapMaxBytes = nonHeapMaxBytes;
        }

        public long getHeapInitBytes() { return heapInitBytes; }
        public long getHeapMaxBytes() { return heapMaxBytes; }
        public long getNonHeapInitBytes() { return nonHeapInitBytes; }
        public long getNonHeapMaxBytes() { return nonHeapMaxBytes; }

        /**
         * Factory method to create JvmMemInfo from individual parameters.
         */
        public static JvmMemInfo from(final long heapInitBytes, final long heapMaxBytes,
                                      final long nonHeapInitBytes, final long nonHeapMaxBytes) {
            return new JvmMemInfo(heapInitBytes, heapMaxBytes, nonHeapInitBytes, nonHeapMaxBytes);
        }

        public Map<String, Object> toMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("heap_init_in_bytes", heapInitBytes);
            map.put("heap_max_in_bytes", heapMaxBytes);
            map.put("non_heap_init_in_bytes", nonHeapInitBytes);
            map.put("non_heap_max_in_bytes", nonHeapMaxBytes);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final JvmMemInfo that = (JvmMemInfo) o;
            return heapInitBytes == that.heapInitBytes
                    && heapMaxBytes == that.heapMaxBytes
                    && nonHeapInitBytes == that.nonHeapInitBytes
                    && nonHeapMaxBytes == that.nonHeapMaxBytes;
        }

        @Override
        public int hashCode() {
            return Objects.hash(heapInitBytes, heapMaxBytes, nonHeapInitBytes, nonHeapMaxBytes);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NodeInfo nodeInfo = (NodeInfo) o;
        return Objects.equals(id, nodeInfo.id)
                && Objects.equals(name, nodeInfo.name)
                && Objects.equals(ephemeralId, nodeInfo.ephemeralId)
                && Objects.equals(version, nodeInfo.version)
                && Objects.equals(httpAddress, nodeInfo.httpAddress)
                && Objects.equals(host, nodeInfo.host)
                && Objects.equals(status, nodeInfo.status)
                && Objects.equals(osInfo, nodeInfo.osInfo)
                && Objects.equals(jvmInfo, nodeInfo.jvmInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, ephemeralId, version, httpAddress, host, status, osInfo, jvmInfo);
    }

    @Override
    public String toString() {
        return "NodeInfo{id='" + id + "', name='" + name + "', version='" + version + "'}";
    }
}
