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
 * No-op metric collector for when metrics are disabled.
 */
public final class NullMetricCollector implements MetricCollector {
    public static final NullMetricCollector INSTANCE = new NullMetricCollector();

    private NullMetricCollector() {}

    @Override
    public void increment(String[] namespace, String key, long amount) {}

    @Override
    public void decrement(String[] namespace, String key, long amount) {}

    @Override
    public void gauge(String[] namespace, String key, Object value) {}

    @Override
    public void reportTime(String[] namespace, String key, long durationNanos) {}
}
