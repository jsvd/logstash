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

java_import org.logstash.execution.DefaultPipelinesRegistry

module LogStash
  class PipelinesRegistry < Java::OrgLogstashExecution::DefaultPipelinesRegistry
    include LogStash::Util::Loggable

    # Ruby keyword argument support for running_pipelines(include_loading: false)
    # Also symbolizes keys to match Ruby convention
    def running_pipelines(include_loading: false)
      symbolize_keys(get_running_pipelines(include_loading))
    end

    def non_running_pipelines
      symbolize_keys(getNonRunningPipelines())
    end

    def loading_pipelines
      symbolize_keys(getLoadingPipelines())
    end

    def loaded_pipelines
      symbolize_keys(getLoadedPipelines())
    end

    def running_user_defined_pipelines
      symbolize_keys(getRunningUserDefinedPipelines())
    end

    # Alias get_pipeline to return nil instead of Optional (Ruby convention)
    def get_pipeline(pipeline_id)
      get_pipeline_or_null(pipeline_id.to_s)
    end

    private

    def symbolize_keys(java_map)
      java_map.each_with_object({}) { |(k, v), h| h[k.to_sym] = v }
    end
  end
end
