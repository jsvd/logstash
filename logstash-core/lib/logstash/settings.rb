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

require "fileutils"

# Ensure Java SubstitutionVariables uses JRuby's ENV for environment variable access
# (System.getenv() returns a cached snapshot that doesn't see runtime ENV changes)
Java::OrgLogstashSettings::SubstitutionVariables.set_environment_accessor(->(name) { ENV[name] })

module LogStash
  class Settings < Java::OrgLogstashSettings::SettingsContainer

    include LogStash::Util::Loggable

    PIPELINE_SETTINGS_WHITE_LIST = Java::org.logstash.settings.SettingsContainer::PIPELINE_SETTINGS_WHITE_LIST.to_a.freeze
    DEPRECATED_PIPELINE_OVERRIDE_SETTINGS = Java::org.logstash.settings.SettingsContainer::DEPRECATED_PIPELINE_OVERRIDE_SETTINGS.to_a.freeze

    # Override to accept Regexp arguments and return Settings (not SettingsContainer)
    def get_subset(setting_regexp)
      regexp = setting_regexp.is_a?(Regexp) ? setting_regexp : Regexp.new(setting_regexp)
      subset = self.class.new
      names.each do |name|
        next unless name.match(regexp)
        subset.register(get_setting(name).clone)
      end
      subset
    end

    def clone(*args)
      get_subset(".*")
    end
    alias_method :dup, :clone

    # Override to route through Ruby dispatch (enables RSpec mocking on settings)
    def get_default(setting_name)
      get_setting(setting_name).default
    end

    # Override to use Ruby deprecation_logger (from Loggable) for testability
    def merge_pipeline_settings(hash, graceful = false)
      hash.each do |key, _|
        if DEPRECATED_PIPELINE_OVERRIDE_SETTINGS.include?(key)
          deprecation_logger.deprecated("Config option \"#{key}\", set for pipeline \"#{hash['pipeline.id']}\", is " +
                                          "deprecated as a pipeline override setting. Please only set it at " +
                                          "the process level.")
          hash.delete(key)
        elsif !PIPELINE_SETTINGS_WHITE_LIST.include?(key)
          raise ArgumentError.new("Only pipeline related settings are expected. Received \"#{key}\". Allowed settings: #{PIPELINE_SETTINGS_WHITE_LIST}")
        end
      end
      merge(hash, graceful)
    end

    # Keep callbacks in Ruby for RSpec mockability (Java dispatch bypasses Ruby mocks)
    def on_post_process(&block)
      @post_process_callbacks ||= []
      @post_process_callbacks << block
    end

    def post_process
      if @post_process_callbacks
        @post_process_callbacks.each { |callback| callback.call(self) }
      end

      # Re-emit setter-related deprecations after post-processing
      names.each do |name|
        setting = get_setting(name)
        setting.observe_post_process if setting.respond_to?(:observe_post_process)
      end
    end

    # Override to use Ruby YAML.safe_load and static SubstitutionVariables
    # (Java fromYaml initializes secret store from instance settings, but the
    # keystore settings live on the global SETTINGS, not on per-pipeline instances)
    def from_yaml(yaml_path, file_name = "logstash.yml")
      settings = YAML.safe_load(IO.read(::File.join(yaml_path, file_name))) || {}
      # Lazily initialize secret store from wherever keystore settings are available
      # (this instance, or the global SETTINGS)
      ensure_secret_store
      self.merge(Java::OrgLogstashSettings::SubstitutionVariables.deep_replace(flatten_hash(settings), true), true)
      self
    end

    private

    def ensure_secret_store
      source = if registered("keystore.file") && registered("keystore.classname")
                 self
               elsif defined?(LogStash::SETTINGS) && LogStash::SETTINGS != self &&
                     LogStash::SETTINGS.registered("keystore.file") && LogStash::SETTINGS.registered("keystore.classname")
                 LogStash::SETTINGS
               end
      if source
        Java::OrgLogstashSettings::SubstitutionVariables.init_secret_store(
          source.get("keystore.file").to_s,
          source.get("keystore.classname").to_s
        )
      end
    end

    def flatten_hash(h, f = "", g = {})
      return g.update({ f => h }) unless h.is_a? Hash
      if f.empty?
        h.each { |k, r| flatten_hash(r, k, g) }
      else
        h.each { |k, r| flatten_hash(r, "#{f}.#{k}", g) }
      end
      g
    end
  end

  module Setting
    java_import org.logstash.settings.BaseSetting
    java_import org.logstash.settings.BooleanSetting
    java_import org.logstash.settings.NumericSetting
    java_import org.logstash.settings.IntegerSetting
    java_import org.logstash.settings.PositiveIntegerSetting
    java_import org.logstash.settings.PortSetting
    java_import org.logstash.settings.PortRangeSetting
    java_import org.logstash.settings.StringSetting
    java_import org.logstash.settings.NullableStringSetting
    java_import org.logstash.settings.PasswordSetting
    java_import org.logstash.settings.ValidatedPasswordSetting
    java_import org.logstash.settings.CoercibleStringSetting
    java_import org.logstash.settings.ExistingFilePathSetting
    java_import org.logstash.settings.WritableDirectorySetting
    java_import org.logstash.settings.BytesSetting
    java_import org.logstash.settings.NullableSetting
    java_import org.logstash.settings.TimeValueSetting
    java_import org.logstash.settings.ArrayCoercibleSetting
    java_import org.logstash.settings.SplittableStringArraySetting
    java_import org.logstash.settings.StringArraySetting
    java_import org.logstash.settings.SettingWithDeprecatedAlias
    java_import org.logstash.settings.DeprecatedAlias

    # Backward-compatible aliases
    TimeValue = TimeValueSetting
    ArrayCoercible = ArrayCoercibleSetting
    SplittableStringArray = SplittableStringArraySetting
    StringArray = StringArraySetting
    Nullable = NullableSetting
  end

  SETTINGS = Settings.new
end
