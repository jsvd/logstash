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
require "logstash/settings"

describe LogStash::Setting::NumericSetting do
  describe "#value" do
    context "when using a default value" do
      context "when no value is set" do
        subject { described_class.new("number", 1) }
        it "should return the default value" do
          expect(subject.value).to eq(1)
        end
      end

      context "when a value is set" do
        subject { described_class.new("number", 1) }
        let(:new_value) { 2 }
        before :each do
          subject.set(new_value)
        end
        it "should return the set value" do
          expect(subject.value).to eq(new_value)
        end
      end
    end

    context "when not using a default value" do
      context "when no value is set" do
        subject { described_class.new("number", nil, false) }
        it "should return the default value" do
          expect(subject.value).to eq(nil)
        end
      end

      context "when a value is set" do
        subject { described_class.new("number", nil, false) }
        let(:new_value) { 2 }
        before :each do
          subject.set(new_value)
        end
        it "should return the set value" do
          expect(subject.value).to eq(new_value)
        end
      end
    end
  end

  describe "#set?" do
    context "when there is not value set" do
      subject { described_class.new("number", 1) }
      it "should return false" do
        expect(subject.set?).to be(false)
      end
    end
    context "when there is a value set" do
      subject { described_class.new("number", 1) }
      before :each do
        subject.set(2)
      end
      it "should return true" do
        expect(subject.set?).to be(true)
      end
    end
  end

  describe "#set" do
    subject { described_class.new("number", 1) }
    it "should change the value of a setting" do
      expect(subject.value).to eq(1)
      subject.set(4)
      expect(subject.value).to eq(4)
    end
    context "when executed for the first time" do
      it "should change the result of set?" do
        expect(subject.set?).to eq(false)
        subject.set(4)
        expect(subject.set?).to eq(true)
      end
    end
  end

  describe "#reset" do
    subject { described_class.new("number", 1) }
    context "if value is already set" do
      before :each do
        subject.set(2)
      end
      it "should reset value to default" do
        subject.reset
        expect(subject.value).to eq(1)
      end
      it "should reset set? to false" do
        expect(subject.set?).to eq(true)
        subject.reset
        expect(subject.set?).to eq(false)
      end
    end
  end
end
