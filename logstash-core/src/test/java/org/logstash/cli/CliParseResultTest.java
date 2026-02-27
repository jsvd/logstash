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

package org.logstash.cli;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CliParseResult}.
 */
public final class CliParseResultTest {

    // -- getString --

    @Test
    public void testGetStringPresent() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("node.name", "mynode")
                .build();
        assertThat(result.getString("node.name").orElse(""), is(equalTo("mynode")));
    }

    @Test
    public void testGetStringAbsent() {
        final CliParseResult result = CliParseResult.builder().build();
        assertFalse(result.getString("node.name").isPresent());
    }

    @Test
    public void testGetStringFromIntegerValue() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("pipeline.workers", 4)
                .build();
        assertThat(result.getString("pipeline.workers").orElse(""), is(equalTo("4")));
    }

    @Test
    public void testGetStringFromBooleanValue() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("config.debug", true)
                .build();
        assertThat(result.getString("config.debug").orElse(""), is(equalTo("true")));
    }

    // -- getInt --

    @Test
    public void testGetIntPresent() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("pipeline.workers", 8)
                .build();
        assertThat(result.getInt("pipeline.workers").orElse(0), is(equalTo(8)));
    }

    @Test
    public void testGetIntAbsent() {
        final CliParseResult result = CliParseResult.builder().build();
        assertFalse(result.getInt("pipeline.workers").isPresent());
    }

    @Test
    public void testGetIntFromStringValue() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("pipeline.workers", "16")
                .build();
        assertThat(result.getInt("pipeline.workers").orElse(0), is(equalTo(16)));
    }

    // -- getBoolean --

    @Test
    public void testGetBooleanTrue() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("pipeline.unsafe_shutdown", true)
                .build();
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(false), is(true));
    }

    @Test
    public void testGetBooleanFalse() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("pipeline.unsafe_shutdown", false)
                .build();
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(true), is(false));
    }

    @Test
    public void testGetBooleanAbsent() {
        final CliParseResult result = CliParseResult.builder().build();
        assertFalse(result.getBoolean("pipeline.unsafe_shutdown").isPresent());
    }

    @Test
    public void testGetBooleanFromStringTrue() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("config.debug", "true")
                .build();
        assertThat(result.getBoolean("config.debug").orElse(false), is(true));
    }

    @Test
    public void testGetBooleanFromStringFalse() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("config.debug", "false")
                .build();
        assertThat(result.getBoolean("config.debug").orElse(true), is(false));
    }

    // -- getStringList --

    @Test
    public void testGetStringListPresent() {
        final CliParseResult result = CliParseResult.builder()
                .addToList("path.plugins", "/plugins/a")
                .addToList("path.plugins", "/plugins/b")
                .build();
        final List<String> list = result.getStringList("path.plugins");
        assertThat(list.size(), is(2));
        assertThat(list.get(0), is(equalTo("/plugins/a")));
        assertThat(list.get(1), is(equalTo("/plugins/b")));
    }

    @Test
    public void testGetStringListAbsent() {
        final CliParseResult result = CliParseResult.builder().build();
        final List<String> list = result.getStringList("path.plugins");
        assertTrue(list.isEmpty());
    }

    @Test
    public void testGetStringListSingleElement() {
        final CliParseResult result = CliParseResult.builder()
                .addToList("path.plugins", "/plugins/a")
                .build();
        final List<String> list = result.getStringList("path.plugins");
        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(equalTo("/plugins/a")));
    }

    @Test
    public void testGetStringListNonListValue() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("node.name", "mynode")
                .build();
        final List<String> list = result.getStringList("node.name");
        assertTrue(list.isEmpty());
    }

    // -- getPassthroughSettings --

    @Test
    public void testGetPassthroughSettings() {
        final CliParseResult result = CliParseResult.builder()
                .addPassthroughSetting("key1", "val1")
                .addPassthroughSetting("key2", "val2")
                .build();
        final Map<String, String> passthrough = result.getPassthroughSettings();
        assertThat(passthrough.size(), is(2));
        assertThat(passthrough.get("key1"), is(equalTo("val1")));
        assertThat(passthrough.get("key2"), is(equalTo("val2")));
    }

    @Test
    public void testGetPassthroughSettingsEmpty() {
        final CliParseResult result = CliParseResult.builder().build();
        assertTrue(result.getPassthroughSettings().isEmpty());
    }

    @Test
    public void testPassthroughSettingsUnmodifiable() {
        final CliParseResult result = CliParseResult.builder()
                .addPassthroughSetting("k", "v")
                .build();
        try {
            result.getPassthroughSettings().put("new", "val");
            assertThat("Should have thrown UnsupportedOperationException", false, is(true));
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    // -- hasOption --

    @Test
    public void testHasOptionTrue() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("node.name", "test")
                .build();
        assertTrue(result.hasOption("node.name"));
    }

    @Test
    public void testHasOptionFalse() {
        final CliParseResult result = CliParseResult.builder().build();
        assertFalse(result.hasOption("node.name"));
    }

    // -- isVersionRequested / isHelpRequested --

    @Test
    public void testVersionRequestedDefault() {
        final CliParseResult result = CliParseResult.builder().build();
        assertFalse(result.isVersionRequested());
    }

    @Test
    public void testVersionRequestedTrue() {
        final CliParseResult result = CliParseResult.builder()
                .versionRequested(true)
                .build();
        assertTrue(result.isVersionRequested());
    }

    @Test
    public void testHelpRequestedDefault() {
        final CliParseResult result = CliParseResult.builder().build();
        assertFalse(result.isHelpRequested());
    }

    @Test
    public void testHelpRequestedTrue() {
        final CliParseResult result = CliParseResult.builder()
                .helpRequested(true)
                .build();
        assertTrue(result.isHelpRequested());
    }

    // -- getUnknownFlags --

    @Test
    public void testGetUnknownFlagsEmpty() {
        final CliParseResult result = CliParseResult.builder().build();
        assertTrue(result.getUnknownFlags().isEmpty());
    }

    @Test
    public void testGetUnknownFlagsPresent() {
        final CliParseResult result = CliParseResult.builder()
                .addUnknownFlag("--unknown")
                .addUnknownFlag("-X")
                .build();
        final List<String> unknown = result.getUnknownFlags();
        assertThat(unknown.size(), is(2));
        assertTrue(unknown.contains("--unknown"));
        assertTrue(unknown.contains("-X"));
    }

    @Test
    public void testUnknownFlagsUnmodifiable() {
        final CliParseResult result = CliParseResult.builder()
                .addUnknownFlag("--flag")
                .build();
        try {
            result.getUnknownFlags().add("--new");
            assertThat("Should have thrown UnsupportedOperationException", false, is(true));
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    // -- toSettingsMap --

    @Test
    public void testToSettingsMapMergesPassthrough() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("node.name", "test")
                .setValue("pipeline.workers", 4)
                .addPassthroughSetting("custom.key", "custom.value")
                .build();
        final Map<String, Object> map = result.toSettingsMap();
        assertThat(map.get("node.name"), is(equalTo("test")));
        assertThat(map.get("pipeline.workers"), is(equalTo(4)));
        assertThat(map.get("custom.key"), is(equalTo("custom.value")));
    }

    @Test
    public void testToSettingsMapEmpty() {
        final CliParseResult result = CliParseResult.builder().build();
        final Map<String, Object> map = result.toSettingsMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToSettingsMapUnmodifiable() {
        final CliParseResult result = CliParseResult.builder()
                .setValue("node.name", "test")
                .build();
        try {
            result.toSettingsMap().put("new.key", "value");
            assertThat("Should have thrown UnsupportedOperationException", false, is(true));
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testToSettingsMapWithListValues() {
        final CliParseResult result = CliParseResult.builder()
                .addToList("path.plugins", "/a")
                .addToList("path.plugins", "/b")
                .build();
        final Map<String, Object> map = result.toSettingsMap();
        assertTrue(map.containsKey("path.plugins"));
        @SuppressWarnings("unchecked")
        final List<String> plugins = (List<String>) map.get("path.plugins");
        assertThat(plugins.size(), is(2));
    }
}
