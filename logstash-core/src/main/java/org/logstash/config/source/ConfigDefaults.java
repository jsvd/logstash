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

package org.logstash.config.source;

import java.util.regex.Pattern;

/**
 * Provides default configuration strings and utilities for detecting
 * input/output blocks in Logstash pipeline configurations.
 */
public final class ConfigDefaults {

    private ConfigDefaults() {
        // utility class
    }

    private static final String DEFAULT_INPUT = "input { stdin { type => stdin } }";
    private static final String DEFAULT_OUTPUT = "output { stdout { codec => rubydebug } }";

    /**
     * Pattern to detect an input block. Matches the word "input" (with word boundary)
     * followed by optional whitespace and an opening brace.
     */
    private static final Pattern INPUT_BLOCK_PATTERN =
            Pattern.compile("(?:^|\\s)input\\s*\\{", Pattern.MULTILINE);

    /**
     * Pattern to detect an output block. Matches the word "output" (with word boundary)
     * followed by optional whitespace and an opening brace.
     */
    private static final Pattern OUTPUT_BLOCK_PATTERN =
            Pattern.compile("(?:^|\\s)output\\s*\\{", Pattern.MULTILINE);

    /**
     * Returns the default input configuration string.
     *
     * @return default input config
     */
    public static String defaultInput() {
        return DEFAULT_INPUT;
    }

    /**
     * Returns the default output configuration string.
     *
     * @return default output config
     */
    public static String defaultOutput() {
        return DEFAULT_OUTPUT;
    }

    /**
     * Checks whether the given configuration string contains an input block.
     *
     * @param config the configuration string to check
     * @return true if an input block pattern is found
     */
    public static boolean hasInputBlock(String config) {
        if (config == null || config.isEmpty()) {
            return false;
        }
        return INPUT_BLOCK_PATTERN.matcher(config).find();
    }

    /**
     * Checks whether the given configuration string contains an output block.
     *
     * @param config the configuration string to check
     * @return true if an output block pattern is found
     */
    public static boolean hasOutputBlock(String config) {
        if (config == null || config.isEmpty()) {
            return false;
        }
        return OUTPUT_BLOCK_PATTERN.matcher(config).find();
    }
}
