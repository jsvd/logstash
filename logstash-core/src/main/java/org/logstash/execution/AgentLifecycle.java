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
 * Lifecycle interface for the Logstash agent.
 * <p>
 * Defines the contract that any agent implementation must satisfy.
 * The agent is the top-level orchestrator that manages the convergence
 * loop, pipeline lifecycle, and overall system state.
 * </p>
 */
public interface AgentLifecycle {

    /**
     * Starts the agent and blocks until it is shut down.
     * <p>
     * This method starts the convergence loop, which periodically fetches
     * pipeline configurations, resolves the required actions, and executes
     * them. The method blocks until {@link #shutdown()} is called from
     * another thread.
     * </p>
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void execute() throws InterruptedException;

    /**
     * Initiates a graceful shutdown of the agent.
     * <p>
     * Signals the convergence loop to stop and waits for in-progress
     * actions to complete. After this method returns, {@link #isRunning()}
     * will return {@code false}.
     * </p>
     */
    void shutdown();

    /**
     * Returns whether the agent is currently running.
     *
     * @return {@code true} if the agent is running, {@code false} otherwise
     */
    boolean isRunning();

    /**
     * Returns the time in milliseconds since the agent was started.
     *
     * @return uptime in milliseconds, or 0 if the agent has not been started
     */
    long uptimeMillis();

    /**
     * Returns the unique identifier for this agent instance.
     *
     * @return the agent identifier
     */
    String getId();
}
