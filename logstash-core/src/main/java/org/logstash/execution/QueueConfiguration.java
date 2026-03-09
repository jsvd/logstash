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

/**
 * Pure Java data holder for queue configuration values.
 * This class has no JRuby dependencies and can be used from pure Java code.
 */
public class QueueConfiguration {

    private final String queueType;
    private final String queuePath;
    private final int batchSize;
    private final int workers;

    /**
     * Creates a QueueConfiguration with all fields specified.
     *
     * @param queueType the type of queue (e.g., "memory", "persisted")
     * @param queuePath the filesystem path for the queue
     * @param batchSize the batch size for event processing
     * @param workers   the number of pipeline workers
     */
    public QueueConfiguration(final String queueType,
                              final String queuePath,
                              final int batchSize,
                              final int workers) {
        this.queueType = queueType;
        this.queuePath = queuePath;
        this.batchSize = batchSize;
        this.workers = workers;
    }

    public String getQueueType() {
        return queueType;
    }

    public String getQueuePath() {
        return queuePath;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getWorkers() {
        return workers;
    }
}
