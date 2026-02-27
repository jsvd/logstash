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

import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MetricCollector}, {@link NullMetricCollector}, and {@link MetricSnapshot}.
 */
public class MetricCollectorTest {

    @Test
    public void nullMetricCollectorIncrementDoesNotThrow() {
        NullMetricCollector.INSTANCE.increment(new String[]{"stats", "pipelines"}, "events_in", 1);
    }

    @Test
    public void nullMetricCollectorDecrementDoesNotThrow() {
        NullMetricCollector.INSTANCE.decrement(new String[]{"stats", "pipelines"}, "events_in", 1);
    }

    @Test
    public void nullMetricCollectorGaugeDoesNotThrow() {
        NullMetricCollector.INSTANCE.gauge(new String[]{"stats"}, "uptime", 12345L);
    }

    @Test
    public void nullMetricCollectorReportTimeDoesNotThrow() {
        NullMetricCollector.INSTANCE.reportTime(new String[]{"stats"}, "duration", 500_000_000L);
    }

    @Test
    public void nullMetricCollectorInstanceIsSingleton() {
        assertThat(NullMetricCollector.INSTANCE).isSameAs(NullMetricCollector.INSTANCE);
    }

    @Test
    public void nullMetricCollectorImplementsMetricCollector() {
        assertThat(NullMetricCollector.INSTANCE).isInstanceOf(MetricCollector.class);
    }

    @Test
    public void metricSnapshotDefaultCreatedAtIsNow() {
        Instant before = Instant.now();
        MetricSnapshot snapshot = new MetricSnapshot();
        Instant after = Instant.now();

        assertThat(snapshot.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(snapshot.getCreatedAt()).isBeforeOrEqualTo(after);
    }

    @Test
    public void metricSnapshotExplicitCreatedAt() {
        Instant explicit = Instant.parse("2025-01-15T10:30:00Z");
        MetricSnapshot snapshot = new MetricSnapshot(explicit);

        assertThat(snapshot.getCreatedAt()).isEqualTo(explicit);
    }
}
