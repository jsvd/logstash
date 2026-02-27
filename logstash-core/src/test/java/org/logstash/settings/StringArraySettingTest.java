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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class StringArraySettingTest {

    @Test
    public void validValuesAccepted() {
        List<String> allowed = Arrays.asList("TLSv1.2", "TLSv1.3");
        StringArraySetting sut = new StringArraySetting("test.protocols", null, true, allowed);
        List<String> value = Arrays.asList("TLSv1.2", "TLSv1.3");
        sut.set(value);
        assertEquals(value, sut.value());
    }

    @Test
    public void invalidValueThrows() {
        List<String> allowed = Arrays.asList("TLSv1.2", "TLSv1.3");
        StringArraySetting sut = new StringArraySetting("test.protocols", null, false, allowed);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sut.set(Arrays.asList("TLSv1.0", "TLSv1.2")));
        assertThat(ex.getMessage(), containsString("Failed to validate"));
        assertThat(ex.getMessage(), containsString("TLSv1.0"));
    }

    @Test
    public void noPossibleStringsAllowsAnything() {
        StringArraySetting sut = new StringArraySetting("test.any", null, false);
        sut.set(Arrays.asList("anything", "goes"));
        assertEquals(Arrays.asList("anything", "goes"), sut.value());
    }

    @Test
    public void emptyPossibleStringsAllowsAnything() {
        StringArraySetting sut = new StringArraySetting("test.any", null, false, Collections.emptyList());
        sut.set(Arrays.asList("anything", "goes"));
        assertEquals(Arrays.asList("anything", "goes"), sut.value());
    }

    @Test
    public void defaultValueReturned() {
        List<String> defaultVal = Arrays.asList("a", "b");
        StringArraySetting sut = new StringArraySetting("test.arr", defaultVal, false);
        assertEquals(defaultVal, sut.value());
    }

    @Test
    public void emptyListIsValid() {
        List<String> allowed = Arrays.asList("TLSv1.2", "TLSv1.3");
        StringArraySetting sut = new StringArraySetting("test.protocols", null, false, allowed);
        sut.set(Collections.emptyList());
        assertTrue(sut.value().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongElementTypeThrows() {
        StringArraySetting sut = new StringArraySetting("test.arr", null, false);
        sut.set(Arrays.asList(1, 2, 3));
    }
}
