# Licensed to Elasticsearch B.V. under one or more contributor
# license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright
# ownership. Elasticsearch B.V. licenses this file to you under
# the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

require "logstash/pipeline_action/base"

module LogStash module PipelineAction
  java_import org.logstash.execution.ConvergeResult
  java_import org.logstash.execution.StopAndDeletePipelineAction

  class StopAndDelete < Base
    attr_reader :pipeline_id

    def initialize(pipeline_id)
      @pipeline_id = pipeline_id
      @java_action = Java::OrgLogstashExecution::StopAndDeletePipelineAction.new(pipeline_id.to_s)
    end

    def execute(agent, pipelines_registry)
      pipelines_registry.terminate_pipeline(pipeline_id) do |pipeline|
        pipeline.shutdown
      end
      success = pipelines_registry.delete_pipeline(@pipeline_id)
      agent.health_observer.detach_pipeline_indicator(pipeline_id) if success
      ConvergeResult::ActionResult.from_result(self, success)
    end

    def execution_priority
      @java_action.getExecutionPriority
    end

    def to_s
      @java_action.toString
    end
  end
end end
