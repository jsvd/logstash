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

import org.logstash.config.ir.PipelineConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Determines what pipeline actions are needed to converge from current state to desired state.
 *
 * <p>Implements the resolution algorithm previously in Ruby {@code LogStash::StateResolver}:</p>
 * <ul>
 *   <li>For each desired config with no existing pipeline: create</li>
 *   <li>For each desired config with a changed existing pipeline: reload</li>
 *   <li>For running pipelines not in desired config: stop and delete</li>
 *   <li>For terminated pipelines not in desired config: delete</li>
 * </ul>
 * <p>Actions are returned sorted by execution priority (Create first, Delete last).</p>
 */
public class StateResolver {

    /**
     * Descriptor for a resolved pipeline action, pairing an action type with its target
     * pipeline ID and (for CREATE/RELOAD) the associated pipeline configuration.
     */
    public static class ActionDescriptor implements Comparable<ActionDescriptor> {
        private final PipelineActionType actionType;
        private final String pipelineId;
        private final PipelineConfig pipelineConfig;

        public ActionDescriptor(PipelineActionType actionType, String pipelineId, PipelineConfig pipelineConfig) {
            this.actionType = actionType;
            this.pipelineId = pipelineId;
            this.pipelineConfig = pipelineConfig;
        }

        public PipelineActionType getActionType() {
            return actionType;
        }

        public String getPipelineId() {
            return pipelineId;
        }

        public PipelineConfig getPipelineConfig() {
            return pipelineConfig;
        }

        @Override
        public int compareTo(ActionDescriptor other) {
            int order = Integer.compare(this.actionType.getPriority(), other.actionType.getPriority());
            return order != 0 ? order : this.pipelineId.compareTo(other.pipelineId);
        }

        @Override
        public String toString() {
            return "ActionDescriptor{" + actionType + ", " + pipelineId + "}";
        }
    }

    /**
     * Resolves the list of action descriptors needed to converge from current state to desired state.
     *
     * @param desiredConfigs         list of desired pipeline configurations
     * @param existingPipelineConfigs map of pipeline ID to its current PipelineConfig (for all known pipelines)
     * @param runningPipelineIds     set of pipeline IDs currently running (including loading)
     * @param nonRunningPipelineIds  set of pipeline IDs that are terminated but still in registry
     * @return sorted list of action descriptors
     */
    public List<ActionDescriptor> resolve(
            List<PipelineConfig> desiredConfigs,
            Map<String, PipelineConfig> existingPipelineConfigs,
            Set<String> runningPipelineIds,
            Set<String> nonRunningPipelineIds) {

        final List<ActionDescriptor> actions = new ArrayList<>();
        final Set<String> configuredIds = new HashSet<>();

        for (PipelineConfig config : desiredConfigs) {
            final String pipelineId = config.getPipelineId();
            configuredIds.add(pipelineId);

            final PipelineConfig existingConfig = existingPipelineConfigs.get(pipelineId);
            if (existingConfig == null
                    && !runningPipelineIds.contains(pipelineId)
                    && !nonRunningPipelineIds.contains(pipelineId)) {
                // Pipeline doesn't exist at all — create it
                actions.add(new ActionDescriptor(PipelineActionType.CREATE, pipelineId, config));
            } else if (existingConfig != null && !config.equals(existingConfig)) {
                // Pipeline exists but config changed — reload it
                actions.add(new ActionDescriptor(PipelineActionType.RELOAD, pipelineId, config));
            }
        }

        // Running pipelines not in desired config: stop and delete
        for (String runningId : runningPipelineIds) {
            if (!configuredIds.contains(runningId)) {
                actions.add(new ActionDescriptor(PipelineActionType.STOP_AND_DELETE, runningId, null));
            }
        }

        // Terminated pipelines not in desired config: delete from registry
        for (String nonRunningId : nonRunningPipelineIds) {
            if (!configuredIds.contains(nonRunningId)) {
                actions.add(new ActionDescriptor(PipelineActionType.DELETE, nonRunningId, null));
            }
        }

        Collections.sort(actions);
        return actions;
    }

    /**
     * Simplified resolve method for the ConvergenceLoop and testing.
     * Uses PipelinesRegistry directly with Object-typed configs.
     * Does not support reload detection (no config comparison).
     *
     * @param registry       the current pipeline states
     * @param desiredConfigs desired pipeline configurations mapped by pipeline ID
     * @return sorted list of pipeline actions to execute
     */
    public List<PipelineAction> resolve(PipelinesRegistry registry,
                                         Map<String, Object> desiredConfigs) {
        final List<PipelineAction> actions = new ArrayList<>();
        final Set<String> configuredIds = desiredConfigs.keySet();

        for (Map.Entry<String, Object> entry : desiredConfigs.entrySet()) {
            final String pipelineId = entry.getKey();
            final Object config = entry.getValue();
            final Optional<Object> existing = registry.getPipeline(pipelineId);

            if (existing.isEmpty()) {
                actions.add(new CreatePipelineAction(pipelineId, config));
            }
        }

        for (String runningId : registry.getRunningPipelines().keySet()) {
            if (!configuredIds.contains(runningId)) {
                actions.add(new StopAndDeletePipelineAction(runningId));
            }
        }

        for (String nonRunningId : registry.getNonRunningPipelines().keySet()) {
            if (!configuredIds.contains(nonRunningId)) {
                actions.add(new DeletePipelineAction(nonRunningId));
            }
        }

        Collections.sort(actions);
        return actions;
    }
}
