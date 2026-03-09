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


package org.logstash.instrument.metrics;

/**
 * Pure Java interface for metric collection operations.
 * Internal interface that decouples metric storage from JRuby infrastructure.
 */
public interface MetricCollector {
    /**
     * Increment a counter metric.
     * @param namespace the metric namespace path
     * @param key the metric key
     * @param amount the amount to increment by
     */
    void increment(String[] namespace, String key, long amount);

    /**
     * Decrement a counter metric.
     * @param namespace the metric namespace path
     * @param key the metric key
     * @param amount the amount to decrement by
     */
    void decrement(String[] namespace, String key, long amount);

    /**
     * Set a gauge metric value.
     * @param namespace the metric namespace path
     * @param key the metric key
     * @param value the gauge value
     */
    void gauge(String[] namespace, String key, Object value);

    /**
     * Report a timing measurement.
     * @param namespace the metric namespace path
     * @param key the metric key
     * @param durationNanos the duration in nanoseconds
     */
    void reportTime(String[] namespace, String key, long durationNanos);
}
