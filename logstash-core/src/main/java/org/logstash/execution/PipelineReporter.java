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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java pipeline reporter that creates state snapshots.
 * Contains no JRuby dependencies.
 */
public class PipelineReporter {

    /**
     * Immutable snapshot of pipeline state at a point in time.
     */
    public static class Snapshot {
        private final int inflightCount;
        private final Map<String, Object> stallingThreadsInfo;
        private final long eventsFiltered;
        private final long eventsConsumed;
        private final List<WorkerState> workerStates;
        private final List<OutputInfo> outputInfo;
        private final Instant createdAt;

        public Snapshot(final int inflightCount,
                        final Map<String, Object> stallingThreadsInfo,
                        final long eventsFiltered,
                        final long eventsConsumed,
                        final List<WorkerState> workerStates,
                        final List<OutputInfo> outputInfo) {
            this.inflightCount = inflightCount;
            this.stallingThreadsInfo = stallingThreadsInfo != null
                    ? Collections.unmodifiableMap(stallingThreadsInfo)
                    : Collections.emptyMap();
            this.eventsFiltered = eventsFiltered;
            this.eventsConsumed = eventsConsumed;
            this.workerStates = workerStates != null
                    ? Collections.unmodifiableList(workerStates)
                    : Collections.emptyList();
            this.outputInfo = outputInfo != null
                    ? Collections.unmodifiableList(outputInfo)
                    : Collections.emptyList();
            this.createdAt = Instant.now();
        }

        public int getInflightCount() {
            return inflightCount;
        }

        public Map<String, Object> getStallingThreadsInfo() {
            return stallingThreadsInfo;
        }

        public long getEventsFiltered() {
            return eventsFiltered;
        }

        public long getEventsConsumed() {
            return eventsConsumed;
        }

        public List<WorkerState> getWorkerStates() {
            return workerStates;
        }

        public List<OutputInfo> getOutputInfo() {
            return outputInfo;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        @Override
        public String toString() {
            return "Snapshot{" +
                    "inflightCount=" + inflightCount +
                    ", eventsFiltered=" + eventsFiltered +
                    ", eventsConsumed=" + eventsConsumed +
                    ", workerStates=" + workerStates.size() +
                    ", outputInfo=" + outputInfo.size() +
                    ", stallingThreadsInfo=" + stallingThreadsInfo +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }

    /**
     * Represents the state of a single pipeline worker thread.
     */
    public static class WorkerState {
        private final long threadId;
        private final boolean alive;
        private final int inflightCount;

        public WorkerState(final long threadId, final boolean alive, final int inflightCount) {
            this.threadId = threadId;
            this.alive = alive;
            this.inflightCount = inflightCount;
        }

        public long getThreadId() {
            return threadId;
        }

        public boolean isAlive() {
            return alive;
        }

        public int getInflightCount() {
            return inflightCount;
        }

        @Override
        public String toString() {
            return "WorkerState{" +
                    "threadId=" + threadId +
                    ", alive=" + alive +
                    ", inflightCount=" + inflightCount +
                    '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final WorkerState that = (WorkerState) o;
            return threadId == that.threadId
                    && alive == that.alive
                    && inflightCount == that.inflightCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(threadId, alive, inflightCount);
        }
    }

    /**
     * Represents information about a pipeline output plugin.
     */
    public static class OutputInfo {
        private final String type;
        private final String id;
        private final String concurrency;

        public OutputInfo(final String type, final String id, final String concurrency) {
            this.type = type;
            this.id = id;
            this.concurrency = concurrency;
        }

        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public String getConcurrency() {
            return concurrency;
        }

        @Override
        public String toString() {
            return "OutputInfo{" +
                    "type='" + type + '\'' +
                    ", id='" + id + '\'' +
                    ", concurrency='" + concurrency + '\'' +
                    '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final OutputInfo that = (OutputInfo) o;
            return Objects.equals(type, that.type)
                    && Objects.equals(id, that.id)
                    && Objects.equals(concurrency, that.concurrency);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id, concurrency);
        }
    }

    public PipelineReporter() {
        // Default constructor
    }

    /**
     * Factory method for creating snapshots from raw data.
     *
     * @param inflightCount       total number of events currently in-flight
     * @param stallingThreadsInfo information about threads that appear to be stalling
     * @param eventsFiltered      total number of events filtered
     * @param eventsConsumed      total number of events consumed
     * @param workerStates        list of worker thread states
     * @param outputInfo          list of output plugin information
     * @return an immutable Snapshot of the pipeline state
     */
    public static Snapshot createSnapshot(final int inflightCount,
                                          final Map<String, Object> stallingThreadsInfo,
                                          final long eventsFiltered,
                                          final long eventsConsumed,
                                          final List<WorkerState> workerStates,
                                          final List<OutputInfo> outputInfo) {
        return new Snapshot(inflightCount, stallingThreadsInfo, eventsFiltered,
                eventsConsumed, workerStates, outputInfo);
    }
}
