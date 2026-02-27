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
import java.util.stream.Collectors;

/**
 * Result of a single convergence cycle, including the actions that were
 * executed and their outcomes.
 * <p>
 * This is a richer wrapper around {@link ConvergeResult} that captures
 * additional context about the convergence cycle: what actions were resolved,
 * when the cycle ran, and whether the config fetch succeeded.
 * </p>
 */
public class ConvergenceResult {

    private final ConvergeResult convergeResult;
    private final List<PipelineAction> executedActions;
    private final Instant timestamp;
    private final boolean configFetchSucceeded;

    /**
     * Constructs a new ConvergenceResult.
     *
     * @param convergeResult       the underlying converge result with per-action outcomes,
     *                              or {@code null} if no actions were executed
     * @param executedActions       the list of actions that were resolved and executed
     * @param timestamp             the time at which this convergence cycle completed
     * @param configFetchSucceeded  whether the configuration fetch succeeded
     */
    public ConvergenceResult(ConvergeResult convergeResult,
                             List<PipelineAction> executedActions,
                             Instant timestamp,
                             boolean configFetchSucceeded) {
        this.convergeResult = convergeResult;
        this.executedActions = executedActions != null
                ? Collections.unmodifiableList(executedActions)
                : Collections.emptyList();
        this.timestamp = timestamp;
        this.configFetchSucceeded = configFetchSucceeded;
    }

    /**
     * Returns the underlying {@link ConvergeResult} containing per-action outcomes.
     *
     * @return the converge result, or {@code null} if no actions were executed
     */
    public ConvergeResult getConvergeResult() {
        return convergeResult;
    }

    /**
     * Returns the list of pipeline actions that were resolved and executed
     * during this convergence cycle.
     *
     * @return an unmodifiable list of executed actions
     */
    public List<PipelineAction> getExecutedActions() {
        return executedActions;
    }

    /**
     * Returns the timestamp when this convergence cycle completed.
     *
     * @return the completion timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns whether the configuration fetch succeeded.
     * <p>
     * When this returns {@code false}, the convergence cycle was skipped
     * because the config source could not provide valid configurations.
     * </p>
     *
     * @return {@code true} if the config fetch succeeded
     */
    public boolean isConfigFetchSucceeded() {
        return configFetchSucceeded;
    }

    /**
     * Returns whether the convergence cycle was successful overall.
     * <p>
     * A cycle is successful if the config fetch succeeded and all
     * actions completed successfully (or no actions were needed).
     * </p>
     *
     * @return {@code true} if the cycle was successful
     */
    public boolean isSuccess() {
        if (!configFetchSucceeded) {
            return false;
        }
        if (convergeResult == null) {
            // No actions to execute means success
            return true;
        }
        return convergeResult.isSuccess();
    }

    /**
     * Returns the actions that failed during this convergence cycle.
     *
     * @return a list of actions that failed, or an empty list if none failed
     */
    public List<PipelineAction> getFailedActions() {
        if (convergeResult == null) {
            return Collections.emptyList();
        }
        return executedActions.stream()
                .filter(action -> {
                    ConvergeResult.ActionResult result = convergeResult.failedActions().get(action);
                    return result != null;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the actions that succeeded during this convergence cycle.
     *
     * @return a list of actions that succeeded, or an empty list if none succeeded
     */
    public List<PipelineAction> getSuccessfulActions() {
        if (convergeResult == null) {
            return Collections.emptyList();
        }
        return executedActions.stream()
                .filter(action -> {
                    ConvergeResult.ActionResult result = convergeResult.successfulActions().get(action);
                    return result != null;
                })
                .collect(Collectors.toList());
    }
}
