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

package org.logstash.config.source;

import java.util.List;

/**
 * Interface for sources that provide pipeline configuration data.
 * <p>
 * This is distinct from {@code org.logstash.execution.ConfigSource} which is a
 * higher-level fetch interface. This interface represents specific config sources
 * (files, strings, YAML, etc.) that produce {@link PipelineConfigParts}.
 * </p>
 */
public interface PipelineConfigSource {

    /**
     * Returns the pipeline configurations produced by this source.
     *
     * @return list of pipeline configuration parts
     */
    List<PipelineConfigParts> pipelineConfigs();

    /**
     * Returns whether this source applies to the current settings.
     * A source matches if it has been provided with non-empty configuration
     * (e.g., paths or config strings).
     *
     * @return true if this source should be used
     */
    boolean matches();

    /**
     * Returns whether the current settings have a conflict that prevents
     * this source from being used (e.g., both config paths and config string set).
     *
     * @return true if there is a conflict
     */
    boolean hasConflict();

    /**
     * Returns a human-readable message describing the conflict, if any.
     *
     * @return conflict description, or empty string if no conflict
     */
    String getConflictMessage();
}
