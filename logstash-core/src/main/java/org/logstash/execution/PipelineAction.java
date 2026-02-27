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
 * Pure Java interface for pipeline lifecycle actions.
 * <p>
 * Actions are comparable so they can be sorted by execution priority.
 * Lower priority values execute first (Create before Reload before Stop, etc.).
 * When priorities are equal, actions are sorted by pipeline ID.
 * </p>
 */
public interface PipelineAction extends Comparable<PipelineAction> {

    /**
     * @return the pipeline ID this action targets
     */
    String getPipelineId();

    /**
     * Execute this action within the given context.
     *
     * @param context the execution context providing access to the pipelines registry
     * @return the result of the action execution
     */
    ConvergeResult.ActionResult execute(PipelineActionContext context);

    /**
     * @return the execution priority for ordering (lower values execute first)
     */
    int getExecutionPriority();

    /**
     * Default comparison: first by execution priority, then by pipeline ID.
     */
    @Override
    default int compareTo(PipelineAction other) {
        int order = Integer.compare(this.getExecutionPriority(), other.getExecutionPriority());
        return order != 0 ? order : this.getPipelineId().compareTo(other.getPipelineId());
    }
}
