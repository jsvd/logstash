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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java definition of a Logstash CLI option.
 * This mirrors the option declarations found in runner.rb using Clamp,
 * but is expressed entirely in Java with no JRuby dependencies.
 */
public final class CliOption {

    /**
     * Types of CLI option values.
     */
    public enum OptionType {
        STRING,
        INTEGER,
        BOOLEAN,
        PATH,
        PASSWORD,
        STRING_LIST
    }

    private final String longFlag;
    private final String shortFlag;
    private final String settingKey;
    private final String description;
    private final OptionType type;
    private final Object defaultValue;
    private final boolean required;
    private final boolean deprecated;
    private final String deprecatedAlias;

    private CliOption(Builder builder) {
        this.longFlag = Objects.requireNonNull(builder.longFlag, "longFlag must not be null");
        this.shortFlag = builder.shortFlag;
        this.settingKey = builder.settingKey;
        this.description = builder.description != null ? builder.description : "";
        this.type = builder.type != null ? builder.type : OptionType.STRING;
        this.defaultValue = builder.defaultValue;
        this.required = builder.required;
        this.deprecated = builder.deprecated;
        this.deprecatedAlias = builder.deprecatedAlias;
    }

    public String getLongFlag() {
        return longFlag;
    }

    public String getShortFlag() {
        return shortFlag;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public String getDescription() {
        return description;
    }

    public OptionType getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public String getDeprecatedAlias() {
        return deprecatedAlias;
    }

    /**
     * Returns true if this option takes a value argument (non-boolean types).
     */
    public boolean takesArgument() {
        return type != OptionType.BOOLEAN;
    }

    /**
     * Serializes this option to a map representation.
     *
     * @return a map containing all option properties
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("longFlag", longFlag);
        if (shortFlag != null) {
            map.put("shortFlag", shortFlag);
        }
        if (settingKey != null) {
            map.put("settingKey", settingKey);
        }
        map.put("description", description);
        map.put("type", type.name());
        if (defaultValue != null) {
            map.put("defaultValue", defaultValue);
        }
        map.put("required", required);
        map.put("deprecated", deprecated);
        if (deprecatedAlias != null) {
            map.put("deprecatedAlias", deprecatedAlias);
        }
        return map;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CliOption{");
        sb.append("longFlag='").append(longFlag).append('\'');
        if (shortFlag != null) {
            sb.append(", shortFlag='").append(shortFlag).append('\'');
        }
        if (settingKey != null) {
            sb.append(", settingKey='").append(settingKey).append('\'');
        }
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CliOption cliOption = (CliOption) o;
        return Objects.equals(longFlag, cliOption.longFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(longFlag);
    }

    /**
     * Creates a new builder for constructing a {@link CliOption}.
     *
     * @param longFlag the long flag name (e.g., "--pipeline.workers")
     * @return a new builder instance
     */
    public static Builder builder(String longFlag) {
        return new Builder(longFlag);
    }

    /**
     * Builder for {@link CliOption} instances.
     */
    public static final class Builder {
        private final String longFlag;
        private String shortFlag;
        private String settingKey;
        private String description;
        private OptionType type;
        private Object defaultValue;
        private boolean required;
        private boolean deprecated;
        private String deprecatedAlias;

        private Builder(String longFlag) {
            this.longFlag = longFlag;
        }

        public Builder shortFlag(String shortFlag) {
            this.shortFlag = shortFlag;
            return this;
        }

        public Builder settingKey(String settingKey) {
            this.settingKey = settingKey;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(OptionType type) {
            this.type = type;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder deprecatedAlias(String deprecatedAlias) {
            this.deprecatedAlias = deprecatedAlias;
            return this;
        }

        /**
         * Builds and returns the {@link CliOption}.
         *
         * @return a new CliOption instance
         */
        public CliOption build() {
            return new CliOption(this);
        }
    }
}
