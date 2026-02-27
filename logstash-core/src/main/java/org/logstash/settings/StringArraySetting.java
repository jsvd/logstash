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
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public class StringArraySetting extends ArrayCoercibleSetting {

    private final List<String> possibleStrings;

    public StringArraySetting(String name, List<String> defaultValue, boolean strict, List<String> possibleStrings) {
        super(name, String.class, defaultValue, strict);
        this.possibleStrings = possibleStrings != null ? possibleStrings : Collections.emptyList();
    }

    public StringArraySetting(String name, List<String> defaultValue, boolean strict) {
        this(name, defaultValue, strict, Collections.emptyList());
    }

    @Override
    public void validate(List input) throws IllegalArgumentException {
        super.validate(input);
        if (possibleStrings == null || possibleStrings.isEmpty()) {
            return;
        }
        List<String> coerced = coerce(input);
        List<String> invalid = coerced.stream()
                .filter(val -> !possibleStrings.contains(val))
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException(
                    "Failed to validate the setting \"" + getName() + "\" value(s): " + formatStringList(invalid) +
                    ". Valid options are: " + formatStringList(possibleStrings));
        }
    }

    /**
     * Formats a list of strings with quotes around each element, matching Ruby's Array#inspect format.
     * e.g. ["foo", "bar"] instead of Java's default [foo, bar]
     */
    private static String formatStringList(List<String> list) {
        return "[" + list.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", ")) + "]";
    }
}
