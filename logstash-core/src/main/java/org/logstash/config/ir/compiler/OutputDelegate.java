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
 * Pure Java interface for output plugin delegation.
 * Both Ruby and Java output delegators implement this interface,
 * enabling a migration path away from JRuby dependencies.
 */
public interface OutputDelegate {

    /**
     * @return the unique identifier for this output plugin instance as a plain Java String
     */
    String getPluginId();

    /**
     * @return the configuration name of this output plugin as a plain Java String
     */
    String getPluginConfigName();

    /**
     * Sends a batch of events to this output.
     * Named {@code multiReceiveEvents} to avoid ambiguity with the JRuby
     * {@code multiReceive(IRubyObject)} when the runtime compiler resolves
     * overloaded methods on classes that extend both {@code AbstractOutputDelegatorExt}
     * and implement this interface.
     *
     * @param events the events to output
     */
    void multiReceiveEvents(Collection<Event> events);

    /**
     * @return true if this output supports reloading
     */
    boolean isReloadable();

    /**
     * @return the concurrency model string (e.g., "java", "shared", "single")
     */
    String getConcurrency();

    /**
     * Registers (initializes) this output plugin.
     */
    void register();

    /**
     * Closes this output plugin and releases any resources.
     */
    void close();
}
