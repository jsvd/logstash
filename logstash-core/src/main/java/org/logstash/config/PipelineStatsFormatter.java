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

package org.logstash.config;

import org.logstash.instrument.metrics.Metric;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for formatting pipeline statistics.
 * <p>
 * Extracted from Ruby {@code LogStash::Config::PipelinesInfo}.
 * Provides the {@code flattenMetrics} utility method that recursively flattens
 * a nested hash of metrics into dot-separated key paths.
 * </p>
 */
public final class PipelineStatsFormatter {

    private PipelineStatsFormatter() {}

    /**
     * Recursively flattens a nested map of metrics into dot-separated key paths.
     * <p>
     * For example, given: {@code {"events" => {"in" => 5, "out" => 3}}}
     * produces: {@code {"events.in" => 5, "events.out" => 3}}
     * </p>
     *
     * @param hashOrValue the value to flatten (may be a Map or a leaf value)
     * @param namespaces  the accumulated namespace path segments
     * @return a flat map of dot-separated keys to leaf values
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> flattenMetrics(Object hashOrValue, String[] namespaces) {
        Map<String, Object> result = new HashMap<>();

        if (hashOrValue instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) hashOrValue;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                String[] newNamespaces = new String[namespaces.length + 1];
                System.arraycopy(namespaces, 0, newNamespaces, 0, namespaces.length);
                newNamespaces[namespaces.length] = key;
                result.putAll(flattenMetrics(entry.getValue(), newNamespaces));
            }
        } else {
            result.put(String.join(".", namespaces), hashOrValue);
        }

        return result;
    }

    /**
     * Convenience overload that starts with an empty namespace.
     *
     * @param hashOrValue the value to flatten
     * @return a flat map of dot-separated keys to leaf values
     */
    public static Map<String, Object> flattenMetrics(Object hashOrValue) {
        return flattenMetrics(hashOrValue, new String[0]);
    }

    /**
     * Formats pipeline events stats by extracting metric values.
     * <p>
     * Takes a map of stage name to Metric counter and returns a map of
     * stage name (as String) to the metric's value.
     * </p>
     *
     * @param stats map of stage symbols/strings to Metric objects (may be null)
     * @return map of stage name strings to their values
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> formatPipelineEvents(Map<?, ?> stats) {
        Map<String, Object> result = new HashMap<>();
        if (stats == null) {
            return result;
        }
        for (Map.Entry<?, ?> entry : stats.entrySet()) {
            String key = entry.getKey().toString();
            Object val = entry.getValue();
            if (val instanceof Metric) {
                result.put(key, ((Metric<?>) val).getValue());
            } else {
                result.put(key, val);
            }
        }
        return result;
    }
}
