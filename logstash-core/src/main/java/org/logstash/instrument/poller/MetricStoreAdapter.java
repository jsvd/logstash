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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory metric store implementation suitable for testing and lightweight
 * metric collection. Uses a {@link ConcurrentHashMap} for thread-safe storage.
 *
 * <p>Keys are constructed by joining namespace components and the metric key
 * with dots (e.g., namespace=["jvm","memory"], key="used" becomes "jvm.memory.used").</p>
 */
public class MetricStoreAdapter implements MetricSink {

    private final ConcurrentHashMap<String, Object> gauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public void gauge(String[] namespace, String key, Object value) {
        gauges.put(buildKey(namespace, key), value);
    }

    @Override
    public void increment(String[] namespace, String key, long amount) {
        String fullKey = buildKey(namespace, key);
        counters.computeIfAbsent(fullKey, k -> new AtomicLong(0)).addAndGet(amount);
    }

    /**
     * Retrieve a gauge value.
     *
     * @param namespace the metric namespace path
     * @param key       the metric key
     * @return the gauge value, or {@code null} if not set
     */
    public Object getGauge(String[] namespace, String key) {
        return gauges.get(buildKey(namespace, key));
    }

    /**
     * Retrieve a counter value.
     *
     * @param namespace the metric namespace path
     * @param key       the metric key
     * @return the counter value, or {@code null} if not set
     */
    public Long getCounter(String[] namespace, String key) {
        AtomicLong counter = counters.get(buildKey(namespace, key));
        return counter != null ? counter.get() : null;
    }

    /**
     * Returns the total number of stored metrics (gauges + counters).
     *
     * @return the number of stored metrics
     */
    public int size() {
        return gauges.size() + counters.size();
    }

    /**
     * Clears all stored metrics.
     */
    public void clear() {
        gauges.clear();
        counters.clear();
    }

    private static String buildKey(String[] namespace, String key) {
        StringBuilder sb = new StringBuilder();
        for (String ns : namespace) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(ns);
        }
        if (sb.length() > 0) {
            sb.append('.');
        }
        sb.append(key);
        return sb.toString();
    }
}
