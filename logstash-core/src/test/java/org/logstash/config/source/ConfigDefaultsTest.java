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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigDefaultsTest {

    @Test
    public void testDefaultInput() {
        String input = ConfigDefaults.defaultInput();
        assertEquals("input { stdin { type => stdin } }", input);
        assertTrue(ConfigDefaults.hasInputBlock(input));
    }

    @Test
    public void testDefaultOutput() {
        String output = ConfigDefaults.defaultOutput();
        assertEquals("output { stdout { codec => rubydebug } }", output);
        assertTrue(ConfigDefaults.hasOutputBlock(output));
    }

    @Test
    public void testHasInputBlockSimple() {
        assertTrue(ConfigDefaults.hasInputBlock("input { stdin {} }"));
    }

    @Test
    public void testHasInputBlockWithLeadingWhitespace() {
        assertTrue(ConfigDefaults.hasInputBlock("  input {"));
    }

    @Test
    public void testHasInputBlockMultiline() {
        String config = "filter { mutate {} }\ninput {\n  stdin {}\n}";
        assertTrue(ConfigDefaults.hasInputBlock(config));
    }

    @Test
    public void testHasInputBlockReturnsFalseWhenMissing() {
        assertFalse(ConfigDefaults.hasInputBlock("filter { mutate {} }"));
    }

    @Test
    public void testHasInputBlockReturnsFalseForNull() {
        assertFalse(ConfigDefaults.hasInputBlock(null));
    }

    @Test
    public void testHasInputBlockReturnsFalseForEmpty() {
        assertFalse(ConfigDefaults.hasInputBlock(""));
    }

    @Test
    public void testHasOutputBlockSimple() {
        assertTrue(ConfigDefaults.hasOutputBlock("output { stdout {} }"));
    }

    @Test
    public void testHasOutputBlockWithLeadingWhitespace() {
        assertTrue(ConfigDefaults.hasOutputBlock("  output {"));
    }

    @Test
    public void testHasOutputBlockMultiline() {
        String config = "filter { mutate {} }\noutput {\n  stdout {}\n}";
        assertTrue(ConfigDefaults.hasOutputBlock(config));
    }

    @Test
    public void testHasOutputBlockReturnsFalseWhenMissing() {
        assertFalse(ConfigDefaults.hasOutputBlock("filter { mutate {} }"));
    }

    @Test
    public void testHasOutputBlockReturnsFalseForNull() {
        assertFalse(ConfigDefaults.hasOutputBlock(null));
    }

    @Test
    public void testHasOutputBlockReturnsFalseForEmpty() {
        assertFalse(ConfigDefaults.hasOutputBlock(""));
    }

    @Test
    public void testHasInputBlockNotConfusedBySubstring() {
        // "input" appearing as a substring without the opening brace pattern
        assertFalse(ConfigDefaults.hasInputBlock("filter { mutate { add_field => { \"inputdata\" => \"value\" } } }"));
    }

    @Test
    public void testHasOutputBlockNotConfusedBySubstring() {
        assertFalse(ConfigDefaults.hasOutputBlock("filter { mutate { add_field => { \"outputdata\" => \"value\" } } }"));
    }

    @Test
    public void testHasInputBlockAtStartOfString() {
        assertTrue(ConfigDefaults.hasInputBlock("input { generator {} }"));
    }

    @Test
    public void testHasOutputBlockAtStartOfString() {
        assertTrue(ConfigDefaults.hasOutputBlock("output { elasticsearch {} }"));
    }
}
