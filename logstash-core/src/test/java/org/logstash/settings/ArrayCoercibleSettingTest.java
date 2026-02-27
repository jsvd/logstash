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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ArrayCoercibleSettingTest {

    @Test
    public void defaultValueIsReturnedWhenNotSet() {
        List<String> defaultVal = Arrays.asList("a", "b");
        ArrayCoercibleSetting sut = new ArrayCoercibleSetting("test.array", String.class, defaultVal);
        assertEquals(defaultVal, sut.value());
    }

    @Test
    public void setWithListValue() {
        ArrayCoercibleSetting sut = new ArrayCoercibleSetting("test.array", String.class, Collections.emptyList());
        List<String> newValue = Arrays.asList("x", "y", "z");
        sut.set(newValue);
        assertEquals(newValue, sut.value());
    }

    @Test
    public void singleValueIsWrappedInList() {
        ArrayCoercibleSetting sut = new ArrayCoercibleSetting("test.array", String.class, Collections.emptyList(), false);
        sut.set("single");
        List result = sut.value();
        assertEquals(1, result.size());
        assertEquals("single", result.get(0));
    }

    @Test
    public void nullValueCoercesToEmptyList() {
        ArrayCoercibleSetting sut = new ArrayCoercibleSetting("test.array", String.class, new ArrayList<>(), false);
        sut.set(null);
        assertTrue(sut.value().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongElementTypeThrows() {
        ArrayCoercibleSetting sut = new ArrayCoercibleSetting("test.array", String.class, Collections.emptyList());
        sut.set(Arrays.asList(1, 2, 3));
    }

    @Test
    public void emptyListIsValid() {
        ArrayCoercibleSetting sut = new ArrayCoercibleSetting("test.array", String.class, Collections.emptyList());
        sut.set(new ArrayList<>());
        assertTrue(sut.value().isEmpty());
    }

    @Test
    public void resetReturnsToDefault() {
        List<String> defaultVal = Arrays.asList("a", "b");
        ArrayCoercibleSetting sut = new ArrayCoercibleSetting("test.array", String.class, defaultVal);
        sut.set(Arrays.asList("x", "y"));
        sut.reset();
        assertEquals(defaultVal, sut.value());
    }

    @Test
    public void isSetTracking() {
        ArrayCoercibleSetting sut = new ArrayCoercibleSetting("test.array", String.class, Collections.emptyList());
        assertFalse(sut.isSet());
        sut.set(Arrays.asList("a"));
        assertTrue(sut.isSet());
    }
}
