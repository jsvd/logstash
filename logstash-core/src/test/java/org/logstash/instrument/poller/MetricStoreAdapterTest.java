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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MetricStoreAdapter}.
 */
public class MetricStoreAdapterTest {

    private MetricStoreAdapter store;

    @Before
    public void setUp() {
        store = new MetricStoreAdapter();
    }

    @Test
    public void gaugeSetAndGet() {
        store.gauge(new String[]{"jvm", "memory", "heap"}, "used_in_bytes", 1024L);

        Object value = store.getGauge(new String[]{"jvm", "memory", "heap"}, "used_in_bytes");
        assertThat(value).isEqualTo(1024L);
    }

    @Test
    public void gaugeOverwritesPreviousValue() {
        String[] ns = {"jvm", "memory"};
        store.gauge(ns, "used", 100L);
        store.gauge(ns, "used", 200L);

        assertThat(store.getGauge(ns, "used")).isEqualTo(200L);
    }

    @Test
    public void gaugeWithStringValue() {
        store.gauge(new String[]{"pipeline"}, "status", "running");

        assertThat(store.getGauge(new String[]{"pipeline"}, "status")).isEqualTo("running");
    }

    @Test
    public void gaugeReturnsNullForMissing() {
        assertThat(store.getGauge(new String[]{"nonexistent"}, "key")).isNull();
    }

    @Test
    public void incrementCreatesCounter() {
        store.increment(new String[]{"stats"}, "events_in", 5);

        Long value = store.getCounter(new String[]{"stats"}, "events_in");
        assertThat(value).isEqualTo(5L);
    }

    @Test
    public void incrementAccumulates() {
        String[] ns = {"stats", "pipelines"};
        store.increment(ns, "events", 10);
        store.increment(ns, "events", 20);
        store.increment(ns, "events", 30);

        assertThat(store.getCounter(ns, "events")).isEqualTo(60L);
    }

    @Test
    public void counterReturnsNullForMissing() {
        assertThat(store.getCounter(new String[]{"nonexistent"}, "key")).isNull();
    }

    @Test
    public void sizeTracksGaugesAndCounters() {
        assertThat(store.size()).isEqualTo(0);

        store.gauge(new String[]{"a"}, "g1", 1);
        assertThat(store.size()).isEqualTo(1);

        store.increment(new String[]{"a"}, "c1", 1);
        assertThat(store.size()).isEqualTo(2);

        store.gauge(new String[]{"a"}, "g2", 2);
        assertThat(store.size()).isEqualTo(3);
    }

    @Test
    public void clearRemovesAll() {
        store.gauge(new String[]{"a"}, "key1", "val1");
        store.gauge(new String[]{"b"}, "key2", "val2");
        store.increment(new String[]{"c"}, "count", 10);

        assertThat(store.size()).isEqualTo(3);

        store.clear();
        assertThat(store.size()).isEqualTo(0);
        assertThat(store.getGauge(new String[]{"a"}, "key1")).isNull();
        assertThat(store.getCounter(new String[]{"c"}, "count")).isNull();
    }

    @Test
    public void emptyNamespaceWorksCorrectly() {
        store.gauge(new String[]{}, "key", "value");

        assertThat(store.getGauge(new String[]{}, "key")).isEqualTo("value");
    }

    @Test
    public void singleNamespaceComponent() {
        store.gauge(new String[]{"root"}, "name", "test");

        assertThat(store.getGauge(new String[]{"root"}, "name")).isEqualTo("test");
    }

    @Test
    public void deepNamespace() {
        String[] ns = {"a", "b", "c", "d", "e"};
        store.gauge(ns, "deep_key", 42);

        assertThat(store.getGauge(ns, "deep_key")).isEqualTo(42);
    }

    @Test
    public void differentNamespacesSameKeyAreSeparate() {
        store.gauge(new String[]{"ns1"}, "key", "value1");
        store.gauge(new String[]{"ns2"}, "key", "value2");

        assertThat(store.getGauge(new String[]{"ns1"}, "key")).isEqualTo("value1");
        assertThat(store.getGauge(new String[]{"ns2"}, "key")).isEqualTo("value2");
    }

    @Test
    public void implementsMetricSink() {
        assertThat(store).isInstanceOf(MetricSink.class);
    }

    @Test
    public void threadSafetyConcurrentIncrements() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 1000;
        String[] ns = {"concurrent"};
        String key = "counter";

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        store.increment(ns, key, 1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // start all threads at once
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(store.getCounter(ns, key)).isEqualTo((long) threadCount * incrementsPerThread);
    }

    @Test
    public void threadSafetyConcurrentGauges() throws InterruptedException {
        int threadCount = 10;
        int writesPerThread = 100;
        String[] ns = {"concurrent"};

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Exception> errors = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < writesPerThread; j++) {
                        store.gauge(ns, "gauge_" + threadId, j);
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(errors).isEmpty();

        // Each thread writes to its own key, last value should be writesPerThread - 1
        for (int t = 0; t < threadCount; t++) {
            assertThat(store.getGauge(ns, "gauge_" + t)).isEqualTo(writesPerThread - 1);
        }
    }
}
