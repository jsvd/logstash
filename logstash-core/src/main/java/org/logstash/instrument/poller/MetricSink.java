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

/**
 * Interface for metric storage that allows pollers to write metrics
 * without knowing the underlying storage details.
 */
public interface MetricSink {

    /**
     * Set a gauge metric value. Gauges represent a point-in-time measurement.
     *
     * @param namespace the metric namespace path (e.g., ["jvm", "memory", "heap"])
     * @param key       the metric key (e.g., "used_in_bytes")
     * @param value     the gauge value
     */
    void gauge(String[] namespace, String key, Object value);

    /**
     * Increment a counter metric by the specified amount.
     *
     * @param namespace the metric namespace path
     * @param key       the metric key
     * @param amount    the amount to increment by
     */
    void increment(String[] namespace, String key, long amount);
}
