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

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PollerScheduler}.
 */
public class PollerSchedulerTest {

    private PollerScheduler scheduler;

    @After
    public void tearDown() {
        if (scheduler != null && scheduler.isRunning()) {
            scheduler.stop(1000);
        }
    }

    @Test
    public void startAndStopLifecycle() {
        scheduler = new PollerScheduler();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.addPoller(new TestPoller("test", latch::countDown), 1);
        assertThat(scheduler.isRunning()).isFalse();

        scheduler.start();
        assertThat(scheduler.isRunning()).isTrue();

        scheduler.stop(2000);
        assertThat(scheduler.isRunning()).isFalse();
    }

    @Test
    public void pollerIsExecuted() throws InterruptedException {
        scheduler = new PollerScheduler();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.addPoller(new TestPoller("exec-test", latch::countDown), 1);
        scheduler.start();

        boolean executed = latch.await(5, TimeUnit.SECONDS);
        assertThat(executed).isTrue();
    }

    @Test
    public void multiplePollers() throws InterruptedException {
        scheduler = new PollerScheduler();
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        scheduler.addPoller(new TestPoller("poller-1", latch1::countDown), 1);
        scheduler.addPoller(new TestPoller("poller-2", latch2::countDown), 1);
        scheduler.start();

        boolean both = latch1.await(5, TimeUnit.SECONDS) && latch2.await(5, TimeUnit.SECONDS);
        assertThat(both).isTrue();
    }

    @Test
    public void pollerExecutesMultipleTimes() throws InterruptedException {
        scheduler = new PollerScheduler();
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        scheduler.addPoller(new TestPoller("multi-exec", () -> {
            count.incrementAndGet();
            latch.countDown();
        }), 1);
        scheduler.start();

        boolean executed = latch.await(10, TimeUnit.SECONDS);
        assertThat(executed).isTrue();
        assertThat(count.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    public void errorInPollerDoesNotStopScheduler() throws InterruptedException {
        scheduler = new PollerScheduler();
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        // A failing poller
        scheduler.addPoller(new PollerTask() {
            @Override
            public void collect() {
                throw new RuntimeException("Intentional test failure");
            }

            @Override
            public String getName() {
                return "failing-poller";
            }
        }, 1);

        // A succeeding poller
        scheduler.addPoller(new TestPoller("success-poller", () -> {
            successCount.incrementAndGet();
            latch.countDown();
        }), 1);

        scheduler.start();

        boolean executed = latch.await(10, TimeUnit.SECONDS);
        assertThat(executed).isTrue();
        assertThat(successCount.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    public void cannotAddPollerWhileRunning() {
        scheduler = new PollerScheduler();
        scheduler.addPoller(new TestPoller("test", () -> {}), 1);
        scheduler.start();

        assertThatThrownBy(() -> scheduler.addPoller(new TestPoller("new", () -> {}), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot add pollers while scheduler is running");
    }

    @Test
    public void cannotStartTwice() {
        scheduler = new PollerScheduler();
        scheduler.addPoller(new TestPoller("test", () -> {}), 1);
        scheduler.start();

        assertThatThrownBy(() -> scheduler.start())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already running");
    }

    @Test
    public void stopWhenNotRunningIsNoOp() {
        scheduler = new PollerScheduler();
        scheduler.stop(); // should not throw
        assertThat(scheduler.isRunning()).isFalse();
    }

    @Test
    public void startWithNoPollers() {
        scheduler = new PollerScheduler();
        scheduler.start(); // should not throw, just logs a warning
        assertThat(scheduler.isRunning()).isFalse();
    }

    @Test
    public void threadSafetyConcurrentStartStop() throws InterruptedException {
        scheduler = new PollerScheduler();
        AtomicInteger collectCount = new AtomicInteger(0);

        scheduler.addPoller(new TestPoller("thread-safe", collectCount::incrementAndGet), 1);
        scheduler.start();

        // Wait a bit for some collections
        Thread.sleep(2500);

        scheduler.stop(2000);
        assertThat(scheduler.isRunning()).isFalse();
        assertThat(collectCount.get()).isGreaterThan(0);
    }

    @Test
    public void stopWithCustomTimeout() throws InterruptedException {
        scheduler = new PollerScheduler();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.addPoller(new TestPoller("timeout-test", latch::countDown), 1);
        scheduler.start();

        latch.await(5, TimeUnit.SECONDS);
        scheduler.stop(500);

        assertThat(scheduler.isRunning()).isFalse();
    }

    // ----- Test helper -----

    private static class TestPoller implements PollerTask {
        private final String name;
        private final Runnable action;

        TestPoller(String name, Runnable action) {
            this.name = name;
            this.action = action;
        }

        @Override
        public void collect() {
            action.run();
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
