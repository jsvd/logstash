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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class SettingsContainerTest {

    private SettingsContainer sut;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        sut = new SettingsContainer();
    }

    // ========== Registration ==========

    @Test
    public void registerAndRetrieveSetting() {
        StringSetting setting = new StringSetting("test.name", "default");
        sut.register(setting);
        assertTrue(sut.registered("test.name"));
        assertEquals("default", sut.getValue("test.name"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerDuplicateThrows() {
        sut.register(new StringSetting("test.name", "a"));
        sut.register(new StringSetting("test.name", "b"));
    }

    @Test
    public void registerList() {
        BooleanSetting setting = new BooleanSetting("test.bool", true);
        List<Setting<Boolean>> pair = setting.withDeprecatedAlias("test.bool.old");
        sut.register(pair);
        assertTrue(sut.registered("test.bool"));
        assertTrue(sut.registered("test.bool.old"));
    }

    @Test
    public void registeredReturnsFalseForUnknown() {
        assertFalse(sut.registered("nonexistent"));
    }

    // ========== Get/Set ==========

    @Test
    public void getAndSetValue() {
        sut.register(new StringSetting("test.key", "default"));
        assertEquals("default", sut.get("test.key"));
        sut.set("test.key", "new_value");
        assertEquals("new_value", sut.get("test.key"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getUnknownSettingThrows() {
        sut.getValue("nonexistent");
    }

    @Test
    public void isSetTracking() {
        sut.register(new StringSetting("test.key", "default"));
        assertFalse(sut.isSet("test.key"));
        sut.set("test.key", "value");
        assertTrue(sut.isSet("test.key"));
    }

    @Test
    public void getDefault() {
        sut.register(new StringSetting("test.key", "the_default"));
        assertEquals("the_default", sut.getDefault("test.key"));
    }

    @Test
    public void gracefulSetStoresTransient() {
        // Setting a value for an unregistered name with graceful=true shouldn't throw
        sut.register(new IntegerSetting("test.num", 42));
        // This should go to transient since "unknown" isn't registered
        sut.setValue("unknown.setting", "value", true);
        // No exception thrown
    }

    // ========== Subset and Clone ==========

    @Test
    public void getSubsetFiltersSettings() {
        sut.register(new StringSetting("pipeline.id", "main"));
        sut.register(new BooleanSetting("pipeline.system", false));
        sut.register(new StringSetting("api.host", "localhost"));

        SettingsContainer subset = sut.getSubset("pipeline\\.");
        assertTrue(subset.registered("pipeline.id"));
        assertTrue(subset.registered("pipeline.system"));
        assertFalse(subset.registered("api.host"));
    }

    @Test
    public void cloneCreatesIndependentCopy() {
        sut.register(new StringSetting("test.key", "original"));
        SettingsContainer cloned = sut.clone();

        cloned.set("test.key", "modified");
        assertEquals("original", sut.get("test.key"));
        assertEquals("modified", cloned.get("test.key"));
    }

    @Test
    public void namesReturnsAllRegistered() {
        sut.register(new StringSetting("a", "1"));
        sut.register(new StringSetting("b", "2"));
        sut.register(new StringSetting("c", "3"));

        Set<String> names = sut.names();
        assertEquals(3, names.size());
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
        assertTrue(names.contains("c"));
    }

    // ========== toHash ==========

    @Test
    public void toHashExcludesDeprecatedAliases() {
        BooleanSetting setting = new BooleanSetting("new.name", true);
        List<Setting<Boolean>> pair = setting.withDeprecatedAlias("old.name");
        sut.register(pair);

        Map<String, Object> hash = sut.toHash();
        assertTrue(hash.containsKey("new.name"));
        assertFalse(hash.containsKey("old.name"));
    }

    @Test
    public void toHashIncludesValues() {
        sut.register(new StringSetting("key1", "val1"));
        sut.register(new BooleanSetting("key2", true));

        Map<String, Object> hash = sut.toHash();
        assertEquals("val1", hash.get("key1"));
        assertEquals(true, hash.get("key2"));
    }

    // ========== Merge ==========

    @Test
    public void mergeUpdatesSettings() {
        sut.register(new StringSetting("a", "old"));
        sut.register(new StringSetting("b", "old"));

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("a", "new_a");
        updates.put("b", "new_b");
        sut.merge(updates);

        assertEquals("new_a", sut.get("a"));
        assertEquals("new_b", sut.get("b"));
    }

    @Test
    public void mergePipelineSettingsRejectsNonWhitelisted() {
        sut.register(new StringSetting("pipeline.id", "main"));
        sut.register(new StringSetting("api.host", "localhost"));

        Map<String, Object> hash = new LinkedHashMap<>();
        hash.put("pipeline.id", "test");
        hash.put("api.host", "remote");

        assertThrows(IllegalArgumentException.class,
                () -> sut.mergePipelineSettings(hash));
    }

    @Test
    public void mergePipelineSettingsAcceptsWhitelisted() {
        sut.register(new StringSetting("pipeline.id", "main"));
        sut.register(new IntegerSetting("pipeline.workers", 4));

        Map<String, Object> hash = new LinkedHashMap<>();
        hash.put("pipeline.id", "test");
        hash.put("pipeline.workers", 8);
        sut.mergePipelineSettings(hash);

        assertEquals("test", sut.get("pipeline.id"));
        assertEquals(8, sut.get("pipeline.workers"));
    }

    // ========== Format ==========

    @Test
    public void formatSettingsIncludesHeaderAndFooter() {
        sut.register(new StringSetting("test.key", "value"));
        List<String> output = sut.formatSettings();
        assertTrue(output.get(0).contains("Logstash Settings"));
        assertTrue(output.get(output.size() - 1).contains("Logstash Settings"));
    }

    // ========== Reset ==========

    @Test
    public void resetClearsAllValues() {
        sut.register(new StringSetting("a", "default_a"));
        sut.register(new StringSetting("b", "default_b"));

        sut.set("a", "modified_a");
        sut.set("b", "modified_b");
        sut.reset();

        assertEquals("default_a", sut.get("a"));
        assertEquals("default_b", sut.get("b"));
        assertFalse(sut.isSet("a"));
        assertFalse(sut.isSet("b"));
    }

    // ========== YAML Loading ==========

    @Test
    public void fromYamlLoadsFlatSettings() throws IOException {
        sut.register(new StringSetting("node.name", "default", false));
        sut.register(new IntegerSetting("pipeline.workers", 1));

        File yamlDir = tempFolder.newFolder("config");
        File yamlFile = new File(yamlDir, "logstash.yml");
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("node:\n  name: \"test-node\"\npipeline:\n  workers: 8\n");
        }

        sut.fromYaml(yamlDir.getAbsolutePath());
        assertEquals("test-node", sut.get("node.name"));
        assertEquals(8, sut.get("pipeline.workers"));
    }

    @Test
    public void fromYamlWithCustomFileName() throws IOException {
        sut.register(new StringSetting("test.key", "default", false));

        File yamlDir = tempFolder.newFolder("config");
        File yamlFile = new File(yamlDir, "custom.yml");
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("test:\n  key: custom_value\n");
        }

        sut.fromYaml(yamlDir.getAbsolutePath(), "custom.yml");
        assertEquals("custom_value", sut.get("test.key"));
    }

    // ========== Substitution Variables ==========

    @Test
    public void replacePlaceholdersWithEnvironmentVariable() {
        // Use a known environment variable
        String path = System.getenv("PATH");
        if (path != null) {
            Object result = sut.replacePlaceholders("${PATH}", false);
            assertEquals(path, result);
        }
    }

    @Test
    public void replacePlaceholdersWithDefaultValue() {
        Object result = sut.replacePlaceholders("${NONEXISTENT_VAR_12345:my_default}", false);
        assertEquals("my_default", result);
    }

    @Test
    public void replacePlaceholdersNoPlaceholder() {
        Object result = sut.replacePlaceholders("plain_string", false);
        assertEquals("plain_string", result);
    }

    @Test
    public void replacePlaceholdersNonStringPassesThrough() {
        Object result = sut.replacePlaceholders(42, false);
        assertEquals(42, result);
    }

    @Test
    public void deepReplaceHandlesMaps() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "${NONEXISTENT_VAR_12345:replaced}");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) sut.deepReplace(map, false);
        assertEquals("replaced", result.get("key"));
    }

    @Test
    public void deepReplaceHandlesLists() {
        List<Object> list = new ArrayList<>();
        list.add("${NONEXISTENT_VAR_12345:item1}");
        list.add("plain");

        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) sut.deepReplace(list, false);
        assertEquals("item1", result.get(0));
        assertEquals("plain", result.get(1));
    }

    @Test
    public void refineStripsQuotesAndParsesArrays() {
        // Test array parsing with refine
        Object result = sut.replacePlaceholders("${NONEXISTENT_VAR_12345:[a, b, c]}", true);
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) result;
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void refineStripsSingleQuotes() {
        Object result = sut.replacePlaceholders("${NONEXISTENT_VAR_12345:'quoted'}", true);
        assertEquals("quoted", result);
    }

    @Test
    public void refineStripsDoubleQuotes() {
        Object result = sut.replacePlaceholders("${NONEXISTENT_VAR_12345:\"quoted\"}", true);
        assertEquals("quoted", result);
    }

    // ========== Post-Processing ==========

    @Test
    public void postProcessRunsCallbacks() {
        sut.register(new StringSetting("test.key", "initial", false));
        boolean[] called = {false};
        sut.onPostProcess(settings -> {
            called[0] = true;
            settings.set("test.key", "post_processed");
        });
        sut.postProcess();
        assertTrue(called[0]);
        assertEquals("post_processed", sut.get("test.key"));
    }

    @Test
    public void postProcessWithNoCallbacksDoesNotThrow() {
        sut.postProcess(); // Should not throw
    }

    // ========== Validation ==========

    @Test
    public void validateAllPassesForValidSettings() {
        sut.register(new StringSetting("test.key", "valid", false));
        sut.validateAll();
    }

    // ========== SettingsAccessor Interface ==========

    @Test
    public void getStringViaAccessor() {
        sut.register(new StringSetting("test.key", "value"));
        assertEquals("value", sut.getString("test.key"));
    }

    @Test
    public void getIntViaAccessor() {
        sut.register(new IntegerSetting("test.num", 42));
        assertEquals(42, sut.getInt("test.num"));
    }

    @Test
    public void getBooleanViaAccessor() {
        sut.register(new BooleanSetting("test.flag", true));
        assertTrue(sut.getBoolean("test.flag"));
    }

    @Test
    public void hasReturnsTrueForRegistered() {
        sut.register(new StringSetting("test.key", "value"));
        assertTrue(sut.has("test.key"));
        assertFalse(sut.has("nonexistent"));
    }

    @Test
    public void toMapReturnsUnmodifiable() {
        sut.register(new StringSetting("test.key", "value"));
        Map<String, Object> map = sut.toMap();
        assertThrows(UnsupportedOperationException.class, () -> map.put("new", "value"));
    }

    @Test
    public void getStringOrDefaultReturnsDefault() {
        assertEquals("fallback", sut.getStringOrDefault("nonexistent", "fallback"));
    }

    @Test
    public void getIntOrDefaultReturnsDefault() {
        assertEquals(99, sut.getIntOrDefault("nonexistent", 99));
    }

    @Test
    public void getBooleanOrDefaultReturnsDefault() {
        assertTrue(sut.getBooleanOrDefault("nonexistent", true));
    }

    // ========== Constants ==========

    @Test
    public void pipelineWhiteListContainsExpectedSettings() {
        assertTrue(SettingsContainer.PIPELINE_SETTINGS_WHITE_LIST.contains("pipeline.id"));
        assertTrue(SettingsContainer.PIPELINE_SETTINGS_WHITE_LIST.contains("pipeline.workers"));
        assertTrue(SettingsContainer.PIPELINE_SETTINGS_WHITE_LIST.contains("queue.type"));
        assertFalse(SettingsContainer.PIPELINE_SETTINGS_WHITE_LIST.contains("api.host"));
    }

    @Test
    public void deprecatedOverrideSettingsContainsExpected() {
        assertTrue(SettingsContainer.DEPRECATED_PIPELINE_OVERRIDE_SETTINGS.contains("config.reload.automatic"));
        assertTrue(SettingsContainer.DEPRECATED_PIPELINE_OVERRIDE_SETTINGS.contains("config.reload.interval"));
    }
}
