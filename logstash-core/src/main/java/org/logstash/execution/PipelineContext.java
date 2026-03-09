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

import org.logstash.common.DlqWriter;
import org.logstash.common.NullDlqWriter;

/**
 * Pure Java execution context providing pipeline, agent, and DLQ writer references
 * to plugins during execution. Replaces the JRuby-dependent ExecutionContextExt.
 */
public class PipelineContext {
    private final String pipelineId;
    private final DlqWriter dlqWriter;

    public PipelineContext(String pipelineId, DlqWriter dlqWriter) {
        this.pipelineId = pipelineId;
        this.dlqWriter = dlqWriter != null ? dlqWriter : NullDlqWriter.INSTANCE;
    }

    public PipelineContext(String pipelineId) {
        this(pipelineId, NullDlqWriter.INSTANCE);
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public DlqWriter getDlqWriter() {
        return dlqWriter;
    }
}
