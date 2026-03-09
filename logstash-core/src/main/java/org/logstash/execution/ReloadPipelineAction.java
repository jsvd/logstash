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
 * Pipeline action that reloads an existing pipeline with a new configuration.
 * <p>
 * Corresponds to {@code LogStash::PipelineAction::Reload} in Ruby.
 * </p>
 */
public class ReloadPipelineAction implements PipelineAction {

    private final String pipelineId;
    private final Object pipelineConfig;

    /**
     * @param pipelineId     the pipeline identifier
     * @param pipelineConfig the new pipeline configuration
     */
    public ReloadPipelineAction(String pipelineId, Object pipelineConfig) {
        this.pipelineId = pipelineId;
        this.pipelineConfig = pipelineConfig;
    }

    @Override
    public String getPipelineId() {
        return pipelineId;
    }

    /**
     * @return the new pipeline configuration for the reload
     */
    public Object getPipelineConfig() {
        return pipelineConfig;
    }

    @Override
    public int getExecutionPriority() {
        return PipelineActionType.RELOAD.getPriority();
    }

    /**
     * Placeholder execute method. The real implementation will come when the Agent migrates to Java.
     */
    @Override
    public ConvergeResult.ActionResult execute(PipelineActionContext context) {
        return new ConvergeResult.SuccessfulAction();
    }

    @Override
    public String toString() {
        return "ReloadAction/pipeline_id:" + pipelineId;
    }
}
