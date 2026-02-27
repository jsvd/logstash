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

import java.util.Collection;

/**
 * Pure Java interface for output execution strategies.
 * Defines the contract for different output concurrency models.
 * Contains no JRuby dependencies.
 */
public interface OutputStrategy {

    /**
     * Concurrency types supported by output plugins.
     */
    enum ConcurrencyType {
        /**
         * Output is thread-safe, no synchronization needed.
         * Multiple worker threads can call the output simultaneously.
         */
        SHARED,

        /**
         * Single-threaded access, synchronized.
         * Only one worker thread can call the output at a time.
         */
        SINGLE,

        /**
         * Worker pool pattern.
         * Multiple output instances are created and distributed among workers.
         */
        LEGACY
    }

    /**
     * Send a batch of events to the output.
     *
     * @param events the collection of events to send
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void multiReceive(Collection<co.elastic.logstash.api.Event> events) throws InterruptedException;

    /**
     * Register the output plugin, performing any initialization needed
     * before events can be processed.
     */
    void register();

    /**
     * Close the output plugin, releasing any resources.
     */
    void close();

    /**
     * Returns the concurrency type of this strategy.
     *
     * @return the concurrency type
     */
    ConcurrencyType getConcurrencyType();
}
