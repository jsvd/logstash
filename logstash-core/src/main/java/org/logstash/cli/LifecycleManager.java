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

package org.logstash.cli;

/**
 * Pure Java lifecycle management interface for Logstash.
 * Defines hooks for the various stages of the Logstash lifecycle,
 * from agent startup through shutdown.
 */
public interface LifecycleManager {

    /**
     * Called before the Agent is created and started.
     */
    void onBeforeAgent();

    /**
     * Called after the Agent has been created.
     */
    void onAfterAgent();

    /**
     * Called before shutdown hooks are executed.
     */
    void onBeforeShutdown();

    /**
     * Called after all shutdown hooks have completed.
     */
    void onAfterShutdown();

    /**
     * Registers a shutdown hook to be executed during the shutdown phase.
     *
     * @param hook the runnable to execute on shutdown
     */
    void addShutdownHook(Runnable hook);

    /**
     * Removes a previously registered shutdown hook.
     *
     * @param hook the runnable to remove
     */
    void removeShutdownHook(Runnable hook);

    /**
     * Requests a graceful shutdown.
     */
    void requestShutdown();

    /**
     * Returns whether a shutdown has been requested.
     *
     * @return true if shutdown has been requested
     */
    boolean isShutdownRequested();
}
