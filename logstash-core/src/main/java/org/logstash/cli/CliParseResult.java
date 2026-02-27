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

package org.logstash.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the result of parsing CLI arguments.
 * Provides typed accessors for retrieving option values by their setting key.
 */
public final class CliParseResult {

    private final Map<String, Object> values;
    private final Map<String, String> passthroughSettings;
    private final List<String> unknownFlags;
    private final boolean versionRequested;
    private final boolean helpRequested;

    private CliParseResult(Builder builder) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(builder.values));
        this.passthroughSettings = Collections.unmodifiableMap(new LinkedHashMap<>(builder.passthroughSettings));
        this.unknownFlags = Collections.unmodifiableList(new ArrayList<>(builder.unknownFlags));
        this.versionRequested = builder.versionRequested;
        this.helpRequested = builder.helpRequested;
    }

    /**
     * Gets a string value by setting key.
     *
     * @param settingKey the setting key
     * @return an Optional containing the string value, or empty if not present
     */
    public Optional<String> getString(String settingKey) {
        final Object val = values.get(settingKey);
        if (val == null) {
            return Optional.empty();
        }
        return Optional.of(val.toString());
    }

    /**
     * Gets an integer value by setting key.
     *
     * @param settingKey the setting key
     * @return an Optional containing the integer value, or empty if not present
     */
    public Optional<Integer> getInt(String settingKey) {
        final Object val = values.get(settingKey);
        if (val == null) {
            return Optional.empty();
        }
        if (val instanceof Integer) {
            return Optional.of((Integer) val);
        }
        return Optional.of(Integer.parseInt(val.toString()));
    }

    /**
     * Gets a boolean value by setting key.
     *
     * @param settingKey the setting key
     * @return an Optional containing the boolean value, or empty if not present
     */
    public Optional<Boolean> getBoolean(String settingKey) {
        final Object val = values.get(settingKey);
        if (val == null) {
            return Optional.empty();
        }
        if (val instanceof Boolean) {
            return Optional.of((Boolean) val);
        }
        return Optional.of(Boolean.parseBoolean(val.toString()));
    }

    /**
     * Gets a string list value by setting key.
     *
     * @param settingKey the setting key
     * @return the list of string values, or an empty list if not present
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String settingKey) {
        final Object val = values.get(settingKey);
        if (val instanceof List) {
            return Collections.unmodifiableList((List<String>) val);
        }
        return Collections.emptyList();
    }

    /**
     * Returns all passthrough settings specified via -S/--setting key=value.
     *
     * @return an unmodifiable map of passthrough settings
     */
    public Map<String, String> getPassthroughSettings() {
        return passthroughSettings;
    }

    /**
     * Checks whether a given setting key was provided on the command line.
     *
     * @param settingKey the setting key
     * @return true if the option was specified
     */
    public boolean hasOption(String settingKey) {
        return values.containsKey(settingKey);
    }

    /**
     * Returns true if --version or -V was specified.
     */
    public boolean isVersionRequested() {
        return versionRequested;
    }

    /**
     * Returns true if --help or -h was specified.
     */
    public boolean isHelpRequested() {
        return helpRequested;
    }

    /**
     * Returns any flags that were not recognized by the parser.
     *
     * @return an unmodifiable list of unknown flag strings
     */
    public List<String> getUnknownFlags() {
        return unknownFlags;
    }

    /**
     * Converts all parsed values to a map suitable for applying to the Settings system.
     * Passthrough settings are merged into the result.
     *
     * @return a map of setting keys to their values
     */
    public Map<String, Object> toSettingsMap() {
        final Map<String, Object> result = new LinkedHashMap<>(values);
        result.putAll(passthroughSettings);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Creates a new builder for constructing a {@link CliParseResult}.
     *
     * @return a new builder instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CliParseResult} instances.
     */
    static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final Map<String, String> passthroughSettings = new LinkedHashMap<>();
        private final List<String> unknownFlags = new ArrayList<>();
        private boolean versionRequested;
        private boolean helpRequested;

        Builder() {
        }

        Builder setValue(String settingKey, Object value) {
            this.values.put(settingKey, value);
            return this;
        }

        @SuppressWarnings("unchecked")
        Builder addToList(String settingKey, String value) {
            final Object existing = this.values.get(settingKey);
            final List<String> list;
            if (existing instanceof List) {
                list = (List<String>) existing;
            } else {
                list = new ArrayList<>();
                this.values.put(settingKey, list);
            }
            list.add(value);
            return this;
        }

        Builder addPassthroughSetting(String key, String value) {
            this.passthroughSettings.put(key, value);
            return this;
        }

        Builder addUnknownFlag(String flag) {
            this.unknownFlags.add(flag);
            return this;
        }

        Builder versionRequested(boolean versionRequested) {
            this.versionRequested = versionRequested;
            return this;
        }

        Builder helpRequested(boolean helpRequested) {
            this.helpRequested = helpRequested;
            return this;
        }

        CliParseResult build() {
            return new CliParseResult(this);
        }
    }
}
