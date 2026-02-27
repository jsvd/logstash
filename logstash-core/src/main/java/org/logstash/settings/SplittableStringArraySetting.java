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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SplittableStringArraySetting extends ArrayCoercibleSetting {

    public static final String DEFAULT_TOKEN = ",";

    private final String tokenizer;

    public SplittableStringArraySetting(String name, List<String> defaultValue, boolean strict) {
        this(name, defaultValue, strict, DEFAULT_TOKEN);
    }

    public SplittableStringArraySetting(String name, List<String> defaultValue, boolean strict, String tokenizer) {
        super(name, String.class, defaultValue, strict);
        this.tokenizer = tokenizer;
    }

    @Override
    public List coerce(Object obj) {
        if (obj instanceof List) {
            return (List) obj;
        }
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof String) {
            return Arrays.stream(((String) obj).split(tokenizer))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        return super.coerce(obj);
    }
}
