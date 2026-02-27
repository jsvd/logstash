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

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Pure Java interface for pipeline state management.
 * <p>
 * Implementations must be thread-safe, using per-pipeline locking to allow
 * concurrent operations on different pipelines while serializing operations
 * on the same pipeline.
 * </p>
 */
public interface PipelinesRegistry {

    /**
     * Creates a new pipeline in the registry and executes the start action.
     * Returns the block's return value (truthy = success, falsy = failure).
     *
     * @param pipelineId   the pipeline identifier
     * @param pipeline     the pipeline object to register
     * @param createAction the action to execute; its return value indicates success
     * @return the result of the create action (truthy = success)
     */
    Object createPipeline(String pipelineId, Object pipeline, Supplier<Object> createAction);

    /**
     * Reloads an existing pipeline by executing the reload action.
     * The reload action must return a two-element list: [success, newPipeline].
     *
     * @param pipelineId   the pipeline identifier
     * @param reloadAction the action to execute for reloading
     * @return the success value from the reload action
     */
    Object reloadPipeline(String pipelineId, Supplier<Object> reloadAction);

    /**
     * Terminates a running pipeline by executing the shutdown action.
     *
     * @param pipelineId     the pipeline identifier
     * @param shutdownAction the action to execute, receives the pipeline object
     */
    void terminatePipeline(String pipelineId, Consumer<Object> shutdownAction);

    /**
     * Deletes a terminated pipeline from the registry.
     *
     * @param pipelineId the pipeline identifier
     * @return {@code true} if the pipeline was found and deleted
     */
    boolean deletePipeline(String pipelineId);

    /**
     * Gets a pipeline by its identifier.
     *
     * @param pipelineId the pipeline identifier
     * @return an Optional containing the pipeline, or empty if not found
     */
    Optional<Object> getPipeline(String pipelineId);

    /**
     * @return a map of pipeline ID to pipeline object for all running pipelines
     */
    Map<String, Object> getRunningPipelines();

    /**
     * @param includeLoading whether to include pipelines currently loading
     * @return a map of pipeline ID to pipeline object for running pipelines
     */
    Map<String, Object> getRunningPipelines(boolean includeLoading);

    /**
     * @return a map of pipeline ID to pipeline object for pipelines currently loading
     */
    Map<String, Object> getLoadingPipelines();

    /**
     * @return a map of pipeline ID to pipeline object for pipelines that have finished loading
     */
    Map<String, Object> getLoadedPipelines();

    /**
     * @return a map of pipeline ID to pipeline object for all non-running (terminated) pipelines
     */
    Map<String, Object> getNonRunningPipelines();

    /**
     * @return a map of pipeline ID to pipeline object for running non-system pipelines
     */
    Map<String, Object> getRunningUserDefinedPipelines();

    /**
     * @return the number of pipelines in the registry
     */
    int size();

    /**
     * @return {@code true} if the registry contains no pipelines
     */
    boolean isEmpty();
}
