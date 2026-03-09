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


package org.logstash.config.ir.compiler;

import org.logstash.Event;

import java.util.Collection;

/**
 * Pure Java interface for filter plugin delegation.
 * Both Ruby and Java filter delegators implement this interface,
 * enabling a migration path away from JRuby dependencies.
 */
public interface FilterDelegate {

    /**
     * @return the unique identifier for this filter plugin instance as a plain Java String
     */
    String getPluginId();

    /**
     * @return the configuration name of this filter plugin as a plain Java String
     */
    String getPluginConfigName();

    /**
     * Filters a batch of events.
     *
     * @param events the input events to filter
     * @return the filtered output events (may include new or modified events)
     */
    Collection<Event> multiFilter(Collection<Event> events);

    /**
     * Flushes any pending events from the filter.
     *
     * @param finalFlush true if this is the final flush before shutdown
     * @return any events produced by the flush, or an empty collection
     */
    Collection<Event> flush(boolean finalFlush);

    /**
     * @return true if this filter supports flushing
     */
    boolean hasFlush();

    /**
     * @return true if this filter requires periodic flushing
     */
    boolean hasPeriodicFlush();

    /**
     * @return true if this filter is thread-safe
     */
    boolean isThreadsafe();

    /**
     * @return true if this filter supports reloading
     */
    boolean isReloadable();

    /**
     * Registers (initializes) this filter plugin.
     */
    void register();

    /**
     * Closes this filter plugin and releases any resources.
     */
    void close();
}
