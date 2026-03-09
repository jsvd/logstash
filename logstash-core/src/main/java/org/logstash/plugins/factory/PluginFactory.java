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

import co.elastic.logstash.api.Codec;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.Input;
import co.elastic.logstash.api.Output;
import org.logstash.common.SourceWithMetadata;

import java.util.Map;

/**
 * Pure Java interface for creating plugin instances.
 * This interface uses only Java standard library types, {@code co.elastic.logstash.api} types,
 * and {@link SourceWithMetadata} -- no JRuby dependencies.
 *
 * <p>Implementations handle both Ruby and Java plugins, but callers of this interface
 * do not need to depend on JRuby.</p>
 */
public interface PluginFactory {

    /**
     * Creates an input plugin instance.
     *
     * @param name   the plugin name (e.g., "stdin", "beats")
     * @param id     the unique plugin id
     * @param args   configuration arguments for the plugin
     * @param source source metadata for the plugin definition, may be {@code null}
     * @return the created {@link Input} instance
     */
    Input buildJavaInput(String name, String id, Map<String, Object> args, SourceWithMetadata source);

    /**
     * Creates an output plugin instance.
     *
     * @param name   the plugin name (e.g., "stdout", "elasticsearch")
     * @param id     the unique plugin id
     * @param args   configuration arguments for the plugin
     * @param source source metadata for the plugin definition, may be {@code null}
     * @return the created {@link Output} instance
     */
    Output buildJavaOutput(String name, String id, Map<String, Object> args, SourceWithMetadata source);

    /**
     * Creates a filter plugin instance.
     *
     * @param name   the plugin name (e.g., "mutate", "grok")
     * @param id     the unique plugin id
     * @param args   configuration arguments for the plugin
     * @param source source metadata for the plugin definition, may be {@code null}
     * @return the created {@link Filter} instance
     */
    Filter buildJavaFilter(String name, String id, Map<String, Object> args, SourceWithMetadata source);

    /**
     * Creates a codec plugin instance.
     *
     * @param name   the plugin name (e.g., "json", "plain")
     * @param id     the unique plugin id
     * @param args   configuration arguments for the plugin
     * @param source source metadata for the plugin definition, may be {@code null}
     * @return the created {@link Codec} instance
     */
    Codec buildJavaCodec(String name, String id, Map<String, Object> args, SourceWithMetadata source);

    /**
     * Creates a default codec by name with no additional configuration.
     *
     * @param codecName the codec name (e.g., "json", "plain")
     * @return the created {@link Codec} instance
     */
    Codec buildDefaultCodec(String codecName);
}
