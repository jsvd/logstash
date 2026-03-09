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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of fetching pipeline configurations from one or more sources.
 * <p>
 * Use the static factory methods {@link #success(List)} and {@link #failure(List)} to
 * create instances.
 * </p>
 */
public final class ConfigFetchResult {

    private final boolean success;
    private final List<PipelineConfigParts> configs;
    private final List<String> errors;

    private ConfigFetchResult(boolean success, List<PipelineConfigParts> configs,
                              List<String> errors) {
        this.success = success;
        this.configs = configs != null
                ? Collections.unmodifiableList(new ArrayList<>(configs))
                : Collections.emptyList();
        this.errors = errors != null
                ? Collections.unmodifiableList(new ArrayList<>(errors))
                : Collections.emptyList();
    }

    /**
     * Creates a successful fetch result.
     *
     * @param configs the list of pipeline configurations fetched
     * @return a successful ConfigFetchResult
     */
    public static ConfigFetchResult success(List<PipelineConfigParts> configs) {
        return new ConfigFetchResult(true, configs, Collections.emptyList());
    }

    /**
     * Creates a failed fetch result.
     *
     * @param errors the list of error messages describing what went wrong
     * @return a failed ConfigFetchResult
     */
    public static ConfigFetchResult failure(List<String> errors) {
        return new ConfigFetchResult(false, Collections.emptyList(), errors);
    }

    /**
     * Returns whether the fetch was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the list of pipeline configurations. Empty if the fetch failed.
     *
     * @return unmodifiable list of pipeline configs
     */
    public List<PipelineConfigParts> getConfigs() {
        return configs;
    }

    /**
     * Returns the list of error messages. Empty if the fetch succeeded.
     *
     * @return unmodifiable list of error strings
     */
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        if (success) {
            return "ConfigFetchResult{success=true, configs=" + configs.size() + "}";
        } else {
            return "ConfigFetchResult{success=false, errors=" + errors + "}";
        }
    }
}
