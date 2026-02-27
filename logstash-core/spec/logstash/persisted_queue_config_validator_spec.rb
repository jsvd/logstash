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

require "spec_helper"
require "tmpdir"
require 'securerandom'

java_import org.logstash.execution.PersistedQueueConfigValidator

describe PersistedQueueConfigValidator do
  let(:validator) { PersistedQueueConfigValidator.new }
  let(:queue_path) { Dir.mktmpdir }
  let(:pqc) { PersistedQueueConfigValidator::PipelineQueueConfig }

  after { FileUtils.rm_rf(queue_path) if File.exist?(queue_path) }

  describe ".checkPageCapacity" do
    it "adds error when page_capacity > max_bytes" do
      errors = java.util.ArrayList.new
      PersistedQueueConfigValidator.check_page_capacity(errors, "main", 512, 1024)
      expect(errors.size).to eq(1)
      expect(errors[0]).to include("'queue.page_capacity' must be less than or equal to 'queue.max_bytes'")
    end

    it "does not add error when page_capacity <= max_bytes" do
      errors = java.util.ArrayList.new
      PersistedQueueConfigValidator.check_page_capacity(errors, "main", 1024, 512)
      expect(errors.size).to eq(0)
    end
  end

  describe ".checkQueueUsage" do
    it "adds warning when used_bytes > max_bytes" do
      warnings = java.util.ArrayList.new
      PersistedQueueConfigValidator.check_queue_usage(warnings, "main", 100, 200)
      expect(warnings.size).to eq(1)
      expect(warnings[0]).to include("current queue size")
    end

    it "does not add warning when used_bytes <= max_bytes" do
      warnings = java.util.ArrayList.new
      PersistedQueueConfigValidator.check_queue_usage(warnings, "main", 200, 100)
      expect(warnings.size).to eq(0)
    end
  end

  describe ".getPageSize" do
    it "calculates total page file size" do
      pipeline_path = File.join(queue_path, "main")
      FileUtils.mkdir_p(pipeline_path)
      File.write(File.join(pipeline_path, "page.0"), "x" * 100)
      File.write(File.join(pipeline_path, "page.1"), "x" * 200)
      File.write(File.join(pipeline_path, "other.file"), "x" * 500) # should be ignored

      total = PersistedQueueConfigValidator.get_page_size(java.nio.file.Paths.get(pipeline_path))
      expect(total).to eq(300)
    end

    it "returns 0 for empty directory" do
      pipeline_path = File.join(queue_path, "empty")
      FileUtils.mkdir_p(pipeline_path)
      total = PersistedQueueConfigValidator.get_page_size(java.nio.file.Paths.get(pipeline_path))
      expect(total).to eq(0)
    end
  end

  describe "#check orchestration" do
    context "with memory queue type" do
      it "does not raise for memory queues" do
        configs = [pqc.new("main", "memory", 1024, 512, queue_path)]
        expect { validator.check({}, configs) }.to_not raise_error
      end
    end

    context "with max_bytes = 0" do
      it "skips validation" do
        configs = [pqc.new("main", "persisted", 0, 512, queue_path)]
        expect { validator.check({}, configs) }.to_not raise_error
      end
    end

    context "with page_capacity > max_bytes" do
      it "raises BootstrapCheckException" do
        configs = [pqc.new("main", "persisted", 512, 1024, queue_path)]
        expect { validator.check({}, configs) }.to raise_error(org.logstash.exceptions.BootstrapCheckException)
      end
    end
  end

  describe "#queueConfigsUpdate" do
    let(:config1) { pqc.new("main", "persisted", 1024, 512, queue_path) }

    it "returns false when configs match" do
      running = { "main" => config1 }
      new_configs = [pqc.new("main", "persisted", 1024, 512, queue_path)]
      expect(validator.queue_configs_update(running, new_configs)).to be false
    end

    it "returns true when a new pipeline is added" do
      running = { "main" => config1 }
      new_configs = [
        pqc.new("main", "persisted", 1024, 512, queue_path),
        pqc.new("second", "persisted", 1024, 512, queue_path)
      ]
      expect(validator.queue_configs_update(running, new_configs)).to be true
    end

    it "returns true when max_bytes changes" do
      running = { "main" => config1 }
      new_configs = [pqc.new("main", "persisted", 2048, 512, queue_path)]
      expect(validator.queue_configs_update(running, new_configs)).to be true
    end

    it "returns true when queue_type changes" do
      running = { "main" => config1 }
      new_configs = [pqc.new("main", "memory", 1024, 512, queue_path)]
      expect(validator.queue_configs_update(running, new_configs)).to be true
    end
  end
end
