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


package org.logstash.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pure Java shutdown watcher that monitors pipeline shutdown progress.
 * Detects stalled shutdowns by comparing consecutive snapshots.
 * Contains no JRuby dependencies.
 */
public class ShutdownWatcher {

    /**
     * Interface for the pipeline state that the watcher monitors.
     * Implementations provide access to pipeline execution status and snapshots.
     */
    public interface PipelineMonitor {
        /**
         * Returns true if the pipeline has finished execution.
         */
        boolean isFinishedExecution();

        /**
         * Returns true if workers are draining (e.g., persistent queue draining).
         */
        boolean isWorkersDraining();

        /**
         * Returns the pipeline identifier.
         */
        String getPipelineId();

        /**
         * Returns a snapshot of the current pipeline state.
         */
        PipelineReporter.Snapshot getSnapshot();
    }

    private static final long DEFAULT_CYCLE_PERIOD_MS = 1000L;
    private static final int DEFAULT_REPORT_EVERY = 5;
    private static final int DEFAULT_ABORT_THRESHOLD = 3;

    private final PipelineMonitor pipeline;
    private final long cyclePeriodMs;
    private final int reportEvery;
    private final int abortThreshold;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger attemptsCount = new AtomicInteger(0);
    private final List<PipelineReporter.Snapshot> reports = new ArrayList<>();

    /**
     * Constructs a ShutdownWatcher with default settings.
     *
     * @param pipeline the pipeline monitor to watch
     */
    public ShutdownWatcher(final PipelineMonitor pipeline) {
        this(pipeline, DEFAULT_CYCLE_PERIOD_MS, DEFAULT_REPORT_EVERY, DEFAULT_ABORT_THRESHOLD);
    }

    /**
     * Constructs a ShutdownWatcher with custom settings.
     *
     * @param pipeline       the pipeline monitor to watch
     * @param cyclePeriodMs  how often to check (in milliseconds)
     * @param reportEvery    how many snapshots to accumulate before reporting
     * @param abortThreshold how many stalled cycles before force-aborting
     */
    public ShutdownWatcher(final PipelineMonitor pipeline, final long cyclePeriodMs,
                           final int reportEvery, final int abortThreshold) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline monitor must not be null");
        this.cyclePeriodMs = cyclePeriodMs;
        this.reportEvery = reportEvery;
        this.abortThreshold = abortThreshold;
    }

    /**
     * Returns true if shutdown appears stalled based on recent snapshots.
     * A shutdown is considered stalled when:
     * <ul>
     *   <li>We have exactly {@code reportEvery} snapshots collected</li>
     *   <li>Inflight counts are non-decreasing across consecutive snapshots</li>
     *   <li>Stalling thread info is identical across all snapshots</li>
     * </ul>
     *
     * @return true if the shutdown appears to be stalled
     */
    public boolean isShutdownStalled() {
        if (reports.size() != reportEvery) {
            return false;
        }

        // Check if inflight counts are non-decreasing (not making progress)
        final int[] inflightCounts = reports.stream()
                .mapToInt(PipelineReporter.Snapshot::getInflightCount)
                .toArray();

        for (int i = 0; i < inflightCounts.length - 1; i++) {
            if (inflightCounts[i] > inflightCounts[i + 1]) {
                // Inflight count decreased, so progress is being made
                return false;
            }
        }

        // Inflight counts are non-decreasing; now check if stalling threads info is identical
        final Map<String, Object> firstStallingInfo = reports.get(0).getStallingThreadsInfo();
        for (int i = 1; i < reports.size(); i++) {
            final Map<String, Object> currentStallingInfo = reports.get(i).getStallingThreadsInfo();
            if (!Objects.equals(firstStallingInfo, currentStallingInfo)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Stops the watcher.
     */
    public void stop() {
        running.compareAndSet(true, false);
    }

    /**
     * Returns true if the watcher has been stopped.
     */
    public boolean isStopped() {
        return !running.get();
    }

    /**
     * Returns the current number of check attempts performed.
     */
    public int getAttemptsCount() {
        return attemptsCount.get();
    }

    /**
     * Returns the cycle period in milliseconds.
     */
    public long getCyclePeriodMs() {
        return cyclePeriodMs;
    }

    /**
     * Returns the number of reports to accumulate before checking for stalls.
     */
    public int getReportEvery() {
        return reportEvery;
    }

    /**
     * Returns the number of stalled cycles before force-aborting.
     */
    public int getAbortThreshold() {
        return abortThreshold;
    }

    /**
     * Returns the pipeline monitor.
     */
    public PipelineMonitor getPipeline() {
        return pipeline;
    }

    /**
     * Adds a snapshot report and maintains a circular buffer of at most
     * {@code reportEvery} entries.
     *
     * @param snapshot the pipeline state snapshot to add
     */
    public void addReport(final PipelineReporter.Snapshot snapshot) {
        reports.add(snapshot);
        if (reports.size() > reportEvery) {
            reports.remove(0);
        }
    }

    /**
     * Increments and returns the attempts count.
     *
     * @return the new attempts count after incrementing
     */
    public int incrementAndGetAttempts() {
        return attemptsCount.incrementAndGet();
    }
}
