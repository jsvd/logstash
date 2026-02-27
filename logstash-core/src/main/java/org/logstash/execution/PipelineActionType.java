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
 * Defines the types of pipeline actions and their execution priorities.
 * <p>
 * Priority values match the Ruby implementation in {@code logstash/pipeline_action.rb}:
 * Create(100) &lt; Reload(200) &lt; Stop(300) &lt; StopAndDelete(350) &lt; Delete(400).
 * Lower values execute first.
 * </p>
 */
public enum PipelineActionType {
    CREATE(100),
    RELOAD(200),
    STOP(300),
    STOP_AND_DELETE(350),
    DELETE(400);

    private final int priority;

    PipelineActionType(int priority) {
        this.priority = priority;
    }

    /**
     * @return the execution priority for this action type
     */
    public int getPriority() {
        return priority;
    }
}
