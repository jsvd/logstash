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
import java.util.List;
import java.util.UUID;

import org.logstash.common.SourceWithMetadata;

/**
 * Pure Java data holder for pipeline configuration values.
 * This class has no JRuby dependencies and can be used from pure Java code.
 */
public class PipelineConfiguration {

    private final String pipelineId;
    private final String configString;
    private final String ephemeralId;
    private final List<SourceWithMetadata> configParts;
    private final boolean systemPipeline;
    private final boolean reloadable;

    /**
     * Creates a PipelineConfiguration with all fields specified.
     *
     * @param pipelineId     the pipeline identifier
     * @param configString   the raw configuration string
     * @param ephemeralId    the ephemeral identifier (if null, a random UUID is generated)
     * @param configParts    the list of configuration source parts
     * @param systemPipeline whether this is a system pipeline
     * @param reloadable     whether this pipeline is configured as reloadable
     */
    public PipelineConfiguration(final String pipelineId,
                                 final String configString,
                                 final String ephemeralId,
                                 final List<SourceWithMetadata> configParts,
                                 final boolean systemPipeline,
                                 final boolean reloadable) {
        this.pipelineId = pipelineId;
        this.configString = configString;
        this.ephemeralId = ephemeralId != null ? ephemeralId : UUID.randomUUID().toString();
        this.configParts = configParts != null
                ? Collections.unmodifiableList(List.copyOf(configParts))
                : Collections.emptyList();
        this.systemPipeline = systemPipeline;
        this.reloadable = reloadable;
    }

    /**
     * Creates a PipelineConfiguration with a generated ephemeral ID.
     *
     * @param pipelineId     the pipeline identifier
     * @param configString   the raw configuration string
     * @param configParts    the list of configuration source parts
     * @param systemPipeline whether this is a system pipeline
     * @param reloadable     whether this pipeline is configured as reloadable
     */
    public PipelineConfiguration(final String pipelineId,
                                 final String configString,
                                 final List<SourceWithMetadata> configParts,
                                 final boolean systemPipeline,
                                 final boolean reloadable) {
        this(pipelineId, configString, null, configParts, systemPipeline, reloadable);
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getConfigString() {
        return configString;
    }

    public String getEphemeralId() {
        return ephemeralId;
    }

    public List<SourceWithMetadata> getConfigParts() {
        return configParts;
    }

    public boolean isSystemPipeline() {
        return systemPipeline;
    }

    public boolean isReloadable() {
        return reloadable;
    }
}
