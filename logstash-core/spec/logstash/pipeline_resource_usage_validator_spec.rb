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

java_import org.logstash.execution.PipelineResourceUsageValidator

describe PipelineResourceUsageValidator do
  let(:max_heap_size) { 1 * 1024 * 1024 * 1024 } # 1 GB
  subject { PipelineResourceUsageValidator.new(max_heap_size) }

  context "when memory usage goes above 10% heap" do
    it "does not raise" do
      # 50000 events * 2KB = 100MB => ~9.77% of 1GB, need more
      # 60000 events * 2KB = 120MB => ~11.72% of 1GB
      expect { subject.check(10, 60000) }.to_not raise_error
    end
  end

  context "when memory usage is below 10% heap" do
    it "does not raise" do
      # 1000 events * 2KB = 2MB => ~0.2% of 1GB
      expect { subject.check(10, 1000) }.to_not raise_error
    end
  end

  context "when there are no pipelines" do
    it "does not raise" do
      expect { subject.check(0, 0) }.to_not raise_error
    end
  end

  describe "#compute_percentage" do
    it "returns correct percentage" do
      # 50000 events * 2KB * 1024 bytes = 102,400,000 bytes
      # 102,400,000 / 1,073,741,824 * 100 = 9.54%
      percentage = subject.compute_percentage(50000)
      expect(percentage).to be_within(0.1).of(9.54)
    end
  end
end
