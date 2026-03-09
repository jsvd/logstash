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

package org.logstash.plugins.factory;

import org.logstash.common.SourceWithMetadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java data holder for plugin creation parameters.
 * This class has no JRuby dependencies and provides a structured way to pass
 * plugin creation parameters between components.
 *
 * <p>The arguments map is defensively copied at construction time, so modifications
 * to the original map do not affect this context.</p>
 */
public final class PluginCreationContext {

    private final String pluginName;
    private final String pluginId;
    private final String pluginType;
    private final Map<String, Object> arguments;
    private final SourceWithMetadata source;

    /**
     * Creates a new plugin creation context.
     *
     * @param pluginName the plugin name (e.g., "stdin", "json"), must not be {@code null}
     * @param pluginId   the unique plugin id, must not be {@code null}
     * @param pluginType the plugin type (e.g., "input", "output", "filter", "codec"), must not be {@code null}
     * @param arguments  configuration arguments for the plugin, must not be {@code null}
     * @param source     source metadata for the plugin definition, may be {@code null}
     * @throws NullPointerException if pluginName, pluginId, pluginType, or arguments is {@code null}
     */
    public PluginCreationContext(final String pluginName,
                                 final String pluginId,
                                 final String pluginType,
                                 final Map<String, Object> arguments,
                                 final SourceWithMetadata source) {
        this.pluginName = Objects.requireNonNull(pluginName, "pluginName must not be null");
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId must not be null");
        this.pluginType = Objects.requireNonNull(pluginType, "pluginType must not be null");
        this.arguments = Collections.unmodifiableMap(new HashMap<>(
                Objects.requireNonNull(arguments, "arguments must not be null")));
        this.source = source;
    }

    /**
     * @return the plugin name
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * @return the unique plugin id
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * @return the plugin type (e.g., "input", "output", "filter", "codec")
     */
    public String getPluginType() {
        return pluginType;
    }

    /**
     * Returns an unmodifiable view of the plugin arguments.
     * The returned map cannot be modified.
     *
     * @return the plugin configuration arguments
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * @return the source metadata, or {@code null} if not available
     */
    public SourceWithMetadata getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "PluginCreationContext{" +
                "pluginName='" + pluginName + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", pluginType='" + pluginType + '\'' +
                ", arguments=" + arguments +
                ", source=" + source +
                '}';
    }
}
