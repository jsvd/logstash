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

import java.util.function.Consumer;

/**
 * Pure Java interface for input plugin delegation.
 * Both Ruby and Java input delegators implement this interface,
 * enabling a migration path away from JRuby dependencies.
 */
public interface InputDelegate {

    /**
     * @return the unique identifier for this input plugin instance as a plain Java String
     */
    String getPluginId();

    /**
     * @return the configuration name of this input plugin as a plain Java String
     */
    String getPluginConfigName();

    /**
     * Starts the input plugin, delivering events to the provided consumer.
     *
     * @param eventConsumer consumer that accepts events produced by this input
     */
    void start(Consumer<Event> eventConsumer);

    /**
     * Stops the input plugin.
     */
    void stop();

    /**
     * @return true if this input supports reloading
     */
    boolean isReloadable();

    /**
     * @return true if this input supports running in multiple threads
     */
    boolean isThreadable();

    /**
     * Registers (initializes) this input plugin.
     */
    void register();

    /**
     * Closes this input plugin and releases any resources.
     */
    void close();
}
