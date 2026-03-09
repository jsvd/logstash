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

import java.util.Map;
import java.util.Optional;

/**
 * Pure Java interface for accessing Logstash settings.
 * Provides type-safe access without JRuby dependencies.
 */
public interface SettingsAccessor {

    /**
     * Returns the string value for the given setting name.
     * @param name the setting key
     * @return the string value
     * @throws IllegalArgumentException if the setting is not found
     */
    String getString(String name);

    /**
     * Returns the integer value for the given setting name.
     * @param name the setting key
     * @return the integer value
     * @throws IllegalArgumentException if the setting is not found
     */
    int getInt(String name);

    /**
     * Returns the long value for the given setting name.
     * @param name the setting key
     * @return the long value
     * @throws IllegalArgumentException if the setting is not found
     */
    long getLong(String name);

    /**
     * Returns the boolean value for the given setting name.
     * @param name the setting key
     * @return the boolean value
     * @throws IllegalArgumentException if the setting is not found
     */
    boolean getBoolean(String name);

    /**
     * Returns an optional string value for the given setting name.
     * @param name the setting key
     * @return an Optional containing the string value, or empty if not found
     */
    Optional<String> getOptionalString(String name);

    /**
     * Returns the string value for the given setting name, or the default if not found.
     * @param name the setting key
     * @param defaultValue the default value to return if setting is not found
     * @return the string value, or the default
     */
    String getStringOrDefault(String name, String defaultValue);

    /**
     * Returns the integer value for the given setting name, or the default if not found.
     * @param name the setting key
     * @param defaultValue the default value to return if setting is not found
     * @return the integer value, or the default
     */
    int getIntOrDefault(String name, int defaultValue);

    /**
     * Returns the long value for the given setting name, or the default if not found.
     * @param name the setting key
     * @param defaultValue the default value to return if setting is not found
     * @return the long value, or the default
     */
    long getLongOrDefault(String name, long defaultValue);

    /**
     * Returns the boolean value for the given setting name, or the default if not found.
     * @param name the setting key
     * @param defaultValue the default value to return if setting is not found
     * @return the boolean value, or the default
     */
    boolean getBooleanOrDefault(String name, boolean defaultValue);

    /**
     * Checks whether a setting with the given name exists.
     * @param name the setting key
     * @return true if the setting exists, false otherwise
     */
    boolean has(String name);

    /**
     * Returns all settings as an unmodifiable map.
     * @return a map of setting names to their values
     */
    Map<String, Object> toMap();
}
