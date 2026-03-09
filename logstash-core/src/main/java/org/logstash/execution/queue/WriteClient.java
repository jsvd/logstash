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

package org.logstash.execution.queue;

import org.logstash.Event;

import java.util.Collection;

/**
 * Extended write client interface that supports pushing events with metrics tracking.
 * This extends the basic {@link QueueWriter} interface with batch support and
 * {@link Event}-typed push operations.
 */
public interface WriteClient extends QueueWriter {

    /**
     * Pushes a single {@link Event} to the queue.
     *
     * @param event the event to push
     */
    void pushEvent(Event event);

    /**
     * Pushes a collection of {@link Event}s to the queue.
     *
     * @param events the events to push
     */
    void pushEvents(Collection<Event> events);
}
