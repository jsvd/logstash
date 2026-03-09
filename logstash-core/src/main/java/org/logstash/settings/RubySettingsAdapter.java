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

package org.logstash.settings;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.logstash.RubyUtil;

import java.util.Map;
import java.util.Optional;

/**
 * Adapter that wraps a Ruby LogStash::Settings object and provides
 * the pure Java {@link SettingsAccessor} interface.
 *
 * <p>This is the bridge between the Ruby settings world and pure Java code.
 * It uses JRuby interop to call methods on the underlying Ruby settings object.</p>
 */
public class RubySettingsAdapter implements SettingsAccessor {

    private final IRubyObject rubySettings;

    /**
     * Creates a new adapter wrapping the given Ruby settings object.
     * @param rubySettings the Ruby LogStash::Settings object
     */
    public RubySettingsAdapter(IRubyObject rubySettings) {
        this.rubySettings = rubySettings;
    }

    @Override
    public String getString(String name) {
        return getSettingValue(name).asJavaString();
    }

    @Override
    public int getInt(String name) {
        return getSettingValue(name).convertToInteger().getIntValue();
    }

    @Override
    public long getLong(String name) {
        return getSettingValue(name).convertToInteger().getLongValue();
    }

    @Override
    public boolean getBoolean(String name) {
        return getSettingValue(name).isTrue();
    }

    @Override
    public Optional<String> getOptionalString(String name) {
        IRubyObject value = getSettingValue(name);
        if (value == null || value.isNil()) {
            return Optional.empty();
        }
        return Optional.of(value.asJavaString());
    }

    @Override
    public String getStringOrDefault(String name, String defaultValue) {
        return getOptionalString(name).orElse(defaultValue);
    }

    @Override
    public int getIntOrDefault(String name, int defaultValue) {
        try {
            return getInt(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public long getLongOrDefault(String name, long defaultValue) {
        try {
            return getLong(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanOrDefault(String name, boolean defaultValue) {
        try {
            return getBoolean(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean has(String name) {
        ThreadContext context = RubyUtil.RUBY.getCurrentContext();
        return rubySettings.callMethod(context, "registered?",
                context.runtime.newString(name)).isTrue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap() {
        ThreadContext context = RubyUtil.RUBY.getCurrentContext();
        IRubyObject hash = rubySettings.callMethod(context, "to_hash");
        return (Map<String, Object>) hash.toJava(Map.class);
    }

    /**
     * Returns the underlying Ruby settings object.
     * Useful for callers that still need Ruby interop during the migration period.
     * @return the wrapped Ruby settings object
     */
    public IRubyObject getRubySettings() {
        return rubySettings;
    }

    private IRubyObject getSettingValue(String name) {
        ThreadContext context = RubyUtil.RUBY.getCurrentContext();
        return rubySettings.callMethod(context, "get_value",
                context.runtime.newString(name));
    }
}
