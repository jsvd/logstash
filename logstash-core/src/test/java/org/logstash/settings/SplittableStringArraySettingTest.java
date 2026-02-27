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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SplittableStringArraySettingTest {

    @Test
    public void splitsByCommaByDefault() {
        SplittableStringArraySetting sut = new SplittableStringArraySetting("test.split", null, false);
        sut.set("a,b,c");
        List result = sut.value();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void trimsWhitespace() {
        SplittableStringArraySetting sut = new SplittableStringArraySetting("test.split", null, false);
        sut.set("  a , b , c  ");
        List result = sut.value();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void customTokenizer() {
        SplittableStringArraySetting sut = new SplittableStringArraySetting("test.split", null, false, ";");
        sut.set("x;y;z");
        List result = sut.value();
        assertEquals(Arrays.asList("x", "y", "z"), result);
    }

    @Test
    public void listValuePassesThrough() {
        SplittableStringArraySetting sut = new SplittableStringArraySetting("test.split", null, false);
        List<String> input = Arrays.asList("a", "b");
        sut.set(input);
        assertEquals(input, sut.value());
    }

    @Test
    public void nullCoercesToEmptyList() {
        SplittableStringArraySetting sut = new SplittableStringArraySetting("test.split", null, false);
        sut.set(null);
        assertTrue(sut.value().isEmpty());
    }

    @Test
    public void singleStringWithNoDelimiter() {
        SplittableStringArraySetting sut = new SplittableStringArraySetting("test.split", null, false);
        sut.set("single");
        assertEquals(Collections.singletonList("single"), sut.value());
    }

    @Test
    public void defaultValueReturnedWhenNotSet() {
        List<String> defaultVal = Arrays.asList("x", "y");
        SplittableStringArraySetting sut = new SplittableStringArraySetting("test.split", defaultVal, false);
        assertEquals(defaultVal, sut.value());
    }
}
