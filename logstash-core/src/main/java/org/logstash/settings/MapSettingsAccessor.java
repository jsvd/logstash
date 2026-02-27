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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Pure Java settings accessor backed by a {@link Map}.
 * Useful for testing and for Java-only callers that do not need Ruby interop.
 *
 * <p>Values are stored as {@link Object} and coerced to the requested type on access.
 * Numeric strings are parsed, and boolean strings are interpreted via
 * {@link Boolean#parseBoolean(String)}.</p>
 */
public class MapSettingsAccessor implements SettingsAccessor {

    private final Map<String, Object> settings;

    /**
     * Creates a new accessor backed by a defensive copy of the given map.
     * @param settings the settings map
     */
    public MapSettingsAccessor(Map<String, Object> settings) {
        this.settings = new HashMap<>(settings);
    }

    @Override
    public String getString(String name) {
        Object value = settings.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Setting not found: " + name);
        }
        return value.toString();
    }

    @Override
    public int getInt(String name) {
        Object value = settings.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Setting not found: " + name);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    @Override
    public long getLong(String name) {
        Object value = settings.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Setting not found: " + name);
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    @Override
    public boolean getBoolean(String name) {
        Object value = settings.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Setting not found: " + name);
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @Override
    public Optional<String> getOptionalString(String name) {
        Object value = settings.get(name);
        return value == null ? Optional.empty() : Optional.of(value.toString());
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
        return settings.containsKey(name);
    }

    @Override
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new HashMap<>(settings));
    }
}
