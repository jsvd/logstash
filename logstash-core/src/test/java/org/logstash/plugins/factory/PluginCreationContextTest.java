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

package org.logstash.plugins.factory;

import org.junit.Test;
import org.logstash.common.SourceWithMetadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for {@link PluginCreationContext}.
 */
public class PluginCreationContextTest {

    @Test
    public void testConstructionWithAllFields() throws Exception {
        final SourceWithMetadata source = new SourceWithMetadata("proto", "path", 1, 1, "input {}");
        final Map<String, Object> args = new HashMap<>();
        args.put("key1", "value1");
        args.put("key2", 42);

        final PluginCreationContext context = new PluginCreationContext(
                "stdin", "my-id", "input", args, source
        );

        assertEquals("stdin", context.getPluginName());
        assertEquals("my-id", context.getPluginId());
        assertEquals("input", context.getPluginType());
        assertEquals(2, context.getArguments().size());
        assertEquals("value1", context.getArguments().get("key1"));
        assertEquals(42, context.getArguments().get("key2"));
        assertSame(source, context.getSource());
    }

    @Test
    public void testGettersReturnCorrectValues() throws Exception {
        final SourceWithMetadata source = new SourceWithMetadata("file", "/etc/logstash/conf.d/test.conf", 10, 5, "filter { mutate {} }");
        final Map<String, Object> args = new HashMap<>();
        args.put("add_field", "test_field");

        final PluginCreationContext context = new PluginCreationContext(
                "mutate", "mutate-001", "filter", args, source
        );

        assertEquals("mutate", context.getPluginName());
        assertEquals("mutate-001", context.getPluginId());
        assertEquals("filter", context.getPluginType());
        assertEquals(Collections.singletonMap("add_field", "test_field"), context.getArguments());
        assertEquals(source, context.getSource());
    }

    @Test
    public void testWithNullSource() {
        final Map<String, Object> args = new HashMap<>();
        args.put("codec", "json");

        final PluginCreationContext context = new PluginCreationContext(
                "stdout", "stdout-001", "output", args, null
        );

        assertEquals("stdout", context.getPluginName());
        assertEquals("stdout-001", context.getPluginId());
        assertEquals("output", context.getPluginType());
        assertEquals(1, context.getArguments().size());
        assertNull(context.getSource());
    }

    @Test
    public void testWithEmptyArgsMap() {
        final PluginCreationContext context = new PluginCreationContext(
                "json", "json-001", "codec", Collections.emptyMap(), null
        );

        assertEquals("json", context.getPluginName());
        assertEquals("json-001", context.getPluginId());
        assertEquals("codec", context.getPluginType());
        assertTrue(context.getArguments().isEmpty());
        assertNull(context.getSource());
    }

    @Test
    public void testDefensiveCopyOfArgsMap() {
        final Map<String, Object> originalArgs = new HashMap<>();
        originalArgs.put("host", "localhost");
        originalArgs.put("port", 5044);

        final PluginCreationContext context = new PluginCreationContext(
                "beats", "beats-001", "input", originalArgs, null
        );

        // Modify the original map after construction
        originalArgs.put("host", "remotehost");
        originalArgs.put("new_key", "new_value");
        originalArgs.remove("port");

        // Context should still have the original values
        assertEquals("localhost", context.getArguments().get("host"));
        assertEquals(5044, context.getArguments().get("port"));
        assertNull(context.getArguments().get("new_key"));
        assertEquals(2, context.getArguments().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testArgumentsMapIsUnmodifiable() {
        final Map<String, Object> args = new HashMap<>();
        args.put("key", "value");

        final PluginCreationContext context = new PluginCreationContext(
                "test", "test-001", "input", args, null
        );

        // Attempting to modify the returned map should throw
        context.getArguments().put("another_key", "another_value");
    }

    @Test(expected = NullPointerException.class)
    public void testNullPluginNameThrows() {
        new PluginCreationContext(null, "id", "input", Collections.emptyMap(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullPluginIdThrows() {
        new PluginCreationContext("name", null, "input", Collections.emptyMap(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullPluginTypeThrows() {
        new PluginCreationContext("name", "id", null, Collections.emptyMap(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullArgumentsThrows() {
        new PluginCreationContext("name", "id", "input", null, null);
    }

    @Test
    public void testToStringContainsAllFields() throws Exception {
        final SourceWithMetadata source = new SourceWithMetadata("proto", "path", 1, 1, "input {}");
        final Map<String, Object> args = new HashMap<>();
        args.put("key", "value");

        final PluginCreationContext context = new PluginCreationContext(
                "stdin", "stdin-001", "input", args, source
        );

        final String str = context.toString();
        assertTrue(str.contains("stdin"));
        assertTrue(str.contains("stdin-001"));
        assertTrue(str.contains("input"));
        assertTrue(str.contains("key"));
    }
}
