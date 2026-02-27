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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Pure Java event dispatcher implementing the Observer pattern.
 * Allows listeners to subscribe to lifecycle events and be notified
 * when those events are fired.
 */
public class EventDispatcher {

    /**
     * Listener interface for receiving lifecycle events.
     * Implementations can selectively handle events by inspecting the event name.
     */
    @FunctionalInterface
    public interface Listener {
        void onEvent(String eventName, Object emitter, Object... args);
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Object emitter;

    public EventDispatcher(final Object emitter) {
        this.emitter = emitter;
    }

    public Object getEmitter() {
        return emitter;
    }

    /**
     * Adds a listener to receive event notifications.
     * This operation is slow because we use a {@link CopyOnWriteArraySet},
     * but the majority of additions happen at bootstrap time so this
     * should not be called often at runtime.
     *
     * @param listener the listener to add
     * @return true if the listener was added, false if it was already present
     */
    public boolean addListener(final Listener listener) {
        return listeners.add(listener);
    }

    /**
     * Removes a listener so it no longer receives event notifications.
     *
     * @param listener the listener to remove
     * @return true if the listener was removed, false if it was not present
     */
    public boolean removeListener(final Listener listener) {
        return listeners.remove(listener);
    }

    /**
     * Fires an event to all registered listeners.
     *
     * @param eventName the name of the event (e.g. "after_initialize", "pipeline_started")
     * @param args additional arguments to pass to listeners
     */
    public void fire(final String eventName, final Object... args) {
        for (final Listener listener : listeners) {
            listener.onEvent(eventName, emitter, args);
        }
    }

    /**
     * Returns the number of currently registered listeners.
     *
     * @return the listener count
     */
    public int listenerCount() {
        return listeners.size();
    }
}
