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
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ArrayCoercibleSetting extends Coercible<List> {

    private final Class<?> elementClass;

    public ArrayCoercibleSetting(String name, Object elementClassObj, Object defaultValue) {
        this(name, elementClassObj, defaultValue, true);
    }

    @SuppressWarnings("this-escape")
    public ArrayCoercibleSetting(String name, Object elementClassObj, Object defaultValue, boolean strict) {
        super(name, coerceToList(defaultValue), strict, noValidator());
        this.elementClass = resolveClass(elementClassObj);
    }

    private static List coerceToList(Object value) {
        if (value instanceof List) return new ArrayList<>((List) value);
        if (value == null) return new ArrayList<>();
        return new ArrayList<>(Collections.singletonList(value));
    }

    /**
     * Resolves the element class from the given object.
     * Accepts java.lang.Class directly, or null/other types (e.g. JRuby proxy classes)
     * which result in null (disabling element type validation).
     */
    private static Class<?> resolveClass(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Class) return (Class<?>) obj;
        // For JRuby proxy classes (RubyClass), we can't directly convert,
        // so disable element type checking
        return null;
    }

    @Override
    public List coerce(Object obj) {
        if (obj instanceof List) {
            return (List) obj;
        }
        if (obj == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Collections.singletonList(obj));
    }

    @Override
    public void validate(List input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException("Setting \"" + getName() + "\" must be a List. Received: null");
        }
        // elementClass may be null during parent constructor initialization or when
        // passed a non-Class object (e.g. JRuby proxy class)
        if (elementClass != null) {
            for (Object el : input) {
                if (!elementClass.isInstance(el)) {
                    throw new IllegalArgumentException(
                            "Values of setting \"" + getName() + "\" must be " + elementClass.getName() +
                            ". Received: " + (el == null ? "null" : el.getClass().getName()));
                }
            }
        }
        super.validate(input);
    }
}
