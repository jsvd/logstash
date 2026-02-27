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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler that manages multiple {@link PollerTask} instances, running them
 * at configurable intervals using a {@link ScheduledExecutorService}.
 *
 * <p>Thread-safe: all public methods are synchronized.</p>
 */
public class PollerScheduler {

    private static final Logger LOGGER = LogManager.getLogger(PollerScheduler.class);
    private static final long DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(PollerTask.DEFAULT_TIMEOUT_SECONDS);

    private final List<PollerRegistration> registrations = new ArrayList<>();
    private final List<ScheduledFuture<?>> futures = new ArrayList<>();
    private ScheduledExecutorService executor;
    private volatile boolean running;

    /**
     * Register a poller to be run at the specified interval.
     * Must be called before {@link #start()}.
     *
     * @param poller          the poller task to register
     * @param intervalSeconds the interval in seconds between executions
     * @throws IllegalStateException if the scheduler is already running
     */
    public synchronized void addPoller(PollerTask poller, long intervalSeconds) {
        if (running) {
            throw new IllegalStateException("Cannot add pollers while scheduler is running");
        }
        registrations.add(new PollerRegistration(poller, intervalSeconds));
    }

    /**
     * Start all registered pollers. Each poller is scheduled at its configured
     * interval using a fixed-rate schedule.
     *
     * @throws IllegalStateException if the scheduler is already running
     */
    public synchronized void start() {
        if (running) {
            throw new IllegalStateException("Scheduler is already running");
        }
        if (registrations.isEmpty()) {
            LOGGER.warn("No pollers registered, nothing to start");
            return;
        }

        executor = Executors.newScheduledThreadPool(registrations.size(), r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("logstash-poller");
            return t;
        });

        for (PollerRegistration reg : registrations) {
            ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                    createSafeRunnable(reg.poller),
                    0,
                    reg.intervalSeconds,
                    TimeUnit.SECONDS
            );
            futures.add(future);
            LOGGER.debug("Scheduled poller '{}' with interval {} seconds",
                    reg.poller.getName(), reg.intervalSeconds);
        }

        running = true;
        LOGGER.info("Poller scheduler started with {} pollers", registrations.size());
    }

    /**
     * Gracefully stop all pollers using the default timeout.
     */
    public void stop() {
        stop(DEFAULT_SHUTDOWN_TIMEOUT_MILLIS);
    }

    /**
     * Gracefully stop all pollers with a custom timeout.
     *
     * @param timeoutMillis the maximum time in milliseconds to wait for
     *                      pollers to complete
     */
    public synchronized void stop(long timeoutMillis) {
        if (!running) {
            return;
        }

        LOGGER.info("Stopping poller scheduler (timeout: {} ms)", timeoutMillis);

        // Cancel all scheduled futures
        for (ScheduledFuture<?> future : futures) {
            future.cancel(false);
        }
        futures.clear();

        // Shutdown the executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Poller scheduler did not terminate within {} ms, forcing shutdown",
                        timeoutMillis);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for poller scheduler shutdown");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        running = false;
        LOGGER.info("Poller scheduler stopped");
    }

    /**
     * Returns whether the scheduler is currently running.
     *
     * @return {@code true} if the scheduler is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Wraps a poller's collect method in a safe runnable that catches and
     * logs all exceptions, preventing a single failing poller from
     * disrupting the scheduler.
     */
    private Runnable createSafeRunnable(PollerTask poller) {
        return () -> {
            try {
                poller.collect();
            } catch (Exception e) {
                LOGGER.error("Error in poller '{}': {}", poller.getName(), e.getMessage(), e);
            }
        };
    }

    /**
     * Internal registration record holding a poller and its interval.
     */
    private static class PollerRegistration {
        final PollerTask poller;
        final long intervalSeconds;

        PollerRegistration(PollerTask poller, long intervalSeconds) {
            this.poller = poller;
            this.intervalSeconds = intervalSeconds;
        }
    }
}
