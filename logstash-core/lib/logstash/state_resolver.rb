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

java_import org.logstash.execution.StateResolver
java_import org.logstash.execution.PipelineActionType

module LogStash
  class StateResolver
    def initialize(metric)
      @metric = metric
      @java_resolver = Java::OrgLogstashExecution::StateResolver.new
    end

    def resolve(pipelines_registry, pipeline_configs)
      existing_configs = java.util.HashMap.new
      running_ids = java.util.HashSet.new
      non_running_ids = java.util.HashSet.new

      pipelines_registry.running_pipelines(include_loading: true).each do |id, pipeline|
        id_str = id.to_s
        running_ids.add(id_str)
        existing_configs.put(id_str, pipeline.pipeline_config)
      end

      pipelines_registry.non_running_pipelines.each do |id, pipeline|
        id_str = id.to_s
        non_running_ids.add(id_str)
        existing_configs.put(id_str, pipeline.pipeline_config)
      end

      @java_resolver.resolve(pipeline_configs, existing_configs, running_ids, non_running_ids)
                     .map { |d| to_ruby_action(d) }
                     .sort
    end

    private

    def to_ruby_action(descriptor)
      case descriptor.getActionType
      when PipelineActionType::CREATE
        LogStash::PipelineAction::Create.new(descriptor.getPipelineConfig, @metric)
      when PipelineActionType::RELOAD
        LogStash::PipelineAction::Reload.new(descriptor.getPipelineConfig, @metric)
      when PipelineActionType::STOP_AND_DELETE
        LogStash::PipelineAction::StopAndDelete.new(descriptor.getPipelineId.to_sym)
      when PipelineActionType::DELETE
        LogStash::PipelineAction::Delete.new(descriptor.getPipelineId.to_sym)
      end
    end
  end
end
