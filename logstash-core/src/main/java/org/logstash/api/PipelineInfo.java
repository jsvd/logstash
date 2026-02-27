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
 * Pure Java holder for Logstash pipeline information, used by the web API.
 * Uses the builder pattern for construction.
 */
public final class PipelineInfo {

    private final String pipelineId;
    private final String ephemeralId;
    private final String configHash;
    private final int workers;
    private final int batchSize;
    private final long batchDelay;
    private final boolean dlqEnabled;
    private final String dlqPath;
    private final boolean isSystemPipeline;
    private final boolean reloadable;

    private PipelineInfo(final Builder builder) {
        this.pipelineId = builder.pipelineId;
        this.ephemeralId = builder.ephemeralId;
        this.configHash = builder.configHash;
        this.workers = builder.workers;
        this.batchSize = builder.batchSize;
        this.batchDelay = builder.batchDelay;
        this.dlqEnabled = builder.dlqEnabled;
        this.dlqPath = builder.dlqPath;
        this.isSystemPipeline = builder.isSystemPipeline;
        this.reloadable = builder.reloadable;
    }

    public String getPipelineId() { return pipelineId; }
    public String getEphemeralId() { return ephemeralId; }
    public String getConfigHash() { return configHash; }
    public int getWorkers() { return workers; }
    public int getBatchSize() { return batchSize; }
    public long getBatchDelay() { return batchDelay; }
    public boolean isDlqEnabled() { return dlqEnabled; }
    public String getDlqPath() { return dlqPath; }
    public boolean isSystemPipeline() { return isSystemPipeline; }
    public boolean isReloadable() { return reloadable; }

    /**
     * Converts this PipelineInfo to a Map suitable for JSON serialization.
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("pipeline_id", pipelineId);
        map.put("ephemeral_id", ephemeralId);
        map.put("config_hash", configHash);
        map.put("workers", workers);
        map.put("batch_size", batchSize);
        map.put("batch_delay", batchDelay);
        map.put("dlq_enabled", dlqEnabled);
        map.put("dlq_path", dlqPath);
        map.put("is_system_pipeline", isSystemPipeline);
        map.put("reloadable", reloadable);
        return Collections.unmodifiableMap(map);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing PipelineInfo instances.
     */
    public static final class Builder {
        private String pipelineId;
        private String ephemeralId;
        private String configHash;
        private int workers = 1;
        private int batchSize = 125;
        private long batchDelay = 50;
        private boolean dlqEnabled = false;
        private String dlqPath;
        private boolean isSystemPipeline = false;
        private boolean reloadable = true;

        private Builder() {}

        public Builder pipelineId(final String pipelineId) { this.pipelineId = pipelineId; return this; }
        public Builder ephemeralId(final String ephemeralId) { this.ephemeralId = ephemeralId; return this; }
        public Builder configHash(final String configHash) { this.configHash = configHash; return this; }
        public Builder workers(final int workers) { this.workers = workers; return this; }
        public Builder batchSize(final int batchSize) { this.batchSize = batchSize; return this; }
        public Builder batchDelay(final long batchDelay) { this.batchDelay = batchDelay; return this; }
        public Builder dlqEnabled(final boolean dlqEnabled) { this.dlqEnabled = dlqEnabled; return this; }
        public Builder dlqPath(final String dlqPath) { this.dlqPath = dlqPath; return this; }
        public Builder isSystemPipeline(final boolean isSystemPipeline) { this.isSystemPipeline = isSystemPipeline; return this; }
        public Builder reloadable(final boolean reloadable) { this.reloadable = reloadable; return this; }

        public PipelineInfo build() {
            return new PipelineInfo(this);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PipelineInfo that = (PipelineInfo) o;
        return workers == that.workers
                && batchSize == that.batchSize
                && batchDelay == that.batchDelay
                && dlqEnabled == that.dlqEnabled
                && isSystemPipeline == that.isSystemPipeline
                && reloadable == that.reloadable
                && Objects.equals(pipelineId, that.pipelineId)
                && Objects.equals(ephemeralId, that.ephemeralId)
                && Objects.equals(configHash, that.configHash)
                && Objects.equals(dlqPath, that.dlqPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineId, ephemeralId, configHash, workers, batchSize,
                batchDelay, dlqEnabled, dlqPath, isSystemPipeline, reloadable);
    }

    @Override
    public String toString() {
        return "PipelineInfo{pipelineId='" + pipelineId + "', workers=" + workers +
                ", batchSize=" + batchSize + ", reloadable=" + reloadable + "}";
    }
}
