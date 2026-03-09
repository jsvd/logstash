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

import java.util.Map;
import java.util.Optional;

/**
 * Pure Java interface for loading pipeline configurations.
 * <p>
 * Implementations fetch the current set of pipeline configurations from
 * whatever source is configured (local files, central management, etc.).
 * This interface abstracts the configuration source so that the convergence
 * loop does not need to know where configs come from.
 * </p>
 */
public interface ConfigSource {

    /**
     * Fetches the current pipeline configurations.
     * <p>
     * Returns an {@link Optional} containing a map of pipeline ID to pipeline
     * configuration object. An empty Optional indicates that the fetch failed
     * (e.g., network error, parse error) and the caller should not act on the
     * result.
     * </p>
     *
     * @return a map of pipeline ID to config object, or empty if the fetch failed
     */
    Optional<Map<String, Object>> fetch();
}
