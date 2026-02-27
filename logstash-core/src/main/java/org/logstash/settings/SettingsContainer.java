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

import co.elastic.logstash.api.DeprecationLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.logstash.log.DefaultDeprecationLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Pure Java container for Logstash settings, replacing the Ruby LogStash::Settings class.
 * Manages registration, access, YAML loading, validation, and placeholder substitution.
 */
public class SettingsContainer implements SettingsAccessor {

    private static final Logger LOGGER = LogManager.getLogger(SettingsContainer.class);
    private static final DeprecationLogger DEPRECATION_LOGGER = new DefaultDeprecationLogger(LOGGER);

    public static final Set<String> PIPELINE_SETTINGS_WHITE_LIST = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "config.debug",
            "config.support_escapes",
            "config.string",
            "dead_letter_queue.enable",
            "dead_letter_queue.flush_interval",
            "dead_letter_queue.max_bytes",
            "dead_letter_queue.storage_policy",
            "dead_letter_queue.retain.age",
            "metric.collect",
            "pipeline.plugin_classloaders",
            "path.config",
            "path.dead_letter_queue",
            "path.queue",
            "pipeline.batch.delay",
            "pipeline.batch.metrics.sampling_mode",
            "pipeline.batch.size",
            "pipeline.id",
            "pipeline.reloadable",
            "pipeline.system",
            "pipeline.workers",
            "pipeline.ordered",
            "pipeline.ecs_compatibility",
            "queue.checkpoint.acks",
            "queue.checkpoint.interval",
            "queue.checkpoint.writes",
            "queue.checkpoint.retry",
            "queue.compression",
            "queue.drain",
            "queue.max_bytes",
            "queue.max_events",
            "queue.page_capacity",
            "queue.type"
    )));

    public static final Set<String> DEPRECATED_PIPELINE_OVERRIDE_SETTINGS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "config.reload.automatic",
            "config.reload.interval"
    )));

    private final Map<String, Setting<?>> settings = new LinkedHashMap<>();
    private final Map<String, Object> transientSettings = new LinkedHashMap<>();
    private List<Consumer<SettingsContainer>> postProcessCallbacks;

    // Tracks whether we've initialized the global SubstitutionVariables secret store from our settings
    private volatile boolean secretStoreLoaded = false;

    public SettingsContainer() {
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public DeprecationLogger getDeprecationLogger() {
        return DEPRECATION_LOGGER;
    }

    // ========== Registration ==========

    @SuppressWarnings("unchecked")
    public void register(Object settingOrList) {
        if (settingOrList instanceof List) {
            for (Object s : (List<?>) settingOrList) {
                register(s);
            }
            return;
        }
        Setting<?> setting = (Setting<?>) settingOrList;
        if (settings.containsKey(setting.getName())) {
            throw new IllegalArgumentException(
                    "Setting \"" + setting.getName() + "\" has already been registered");
        }
        settings.put(setting.getName(), setting);
    }

    public void registerSetting(Setting<?> setting) {
        register(setting);
    }

    public boolean registered(String name) {
        return settings.containsKey(name);
    }

    // ========== Access ==========

    public Setting<?> getSetting(String name) {
        Setting<?> setting = settings.get(name);
        if (setting == null) {
            throw new IllegalArgumentException(
                    "Setting \"" + name + "\" doesn't exist. Please check if you haven't made a typo.");
        }
        return setting;
    }

    public Object getValue(String name) {
        return getSetting(name).value();
    }

    public Object get(String name) {
        return getValue(name);
    }

    public void setValue(String name, Object value) {
        setValue(name, value, false);
    }

    public void setValue(String name, Object value, boolean graceful) {
        try {
            getSetting(name).set(value);
        } catch (IllegalArgumentException e) {
            if (graceful) {
                transientSettings.put(name, value);
            } else {
                throw e;
            }
        }
    }

    public void set(String name, Object value) {
        setValue(name, value);
    }

    public boolean isSet(String name) {
        return getSetting(name).isSet();
    }

    public Object getDefault(String name) {
        return getSetting(name).getDefault();
    }

    // ========== Subsetting and Cloning ==========

    public SettingsContainer getSubset(String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        SettingsContainer subset = new SettingsContainer();
        for (Map.Entry<String, Setting<?>> entry : settings.entrySet()) {
            if (pattern.matcher(entry.getKey()).find()) {
                subset.register(cloneSetting(entry.getValue()));
            }
        }
        return subset;
    }

    @SuppressWarnings("unchecked")
    private static <T> Setting<T> cloneSetting(Setting<T> setting) {
        if (setting instanceof BaseSetting) {
            return ((BaseSetting<T>) setting).clone();
        }
        if (setting instanceof SettingDelegator) {
            // For delegators, we need the full clone chain
            // This is a best-effort approach
            return setting;
        }
        return setting;
    }

    public Set<String> names() {
        return new LinkedHashSet<>(settings.keySet());
    }

    @Override
    public SettingsContainer clone() {
        return getSubset(".*");
    }

    // ========== Hash and Merge ==========

    public Map<String, Object> toHash() {
        Map<String, Object> hash = new LinkedHashMap<>();
        for (Map.Entry<String, Setting<?>> entry : settings.entrySet()) {
            Setting<?> setting = entry.getValue();
            if (setting instanceof DeprecatedAlias) {
                continue;
            }
            hash.put(entry.getKey(), setting.value());
        }
        return hash;
    }

    public SettingsContainer merge(Map<String, Object> hash) {
        return merge(hash, false);
    }

    public SettingsContainer merge(Map<String, Object> hash, boolean graceful) {
        for (Map.Entry<String, Object> entry : hash.entrySet()) {
            setValue(entry.getKey(), entry.getValue(), graceful);
        }
        return this;
    }

    public SettingsContainer mergePipelineSettings(Map<String, Object> hash) {
        return mergePipelineSettings(hash, false);
    }

    public SettingsContainer mergePipelineSettings(Map<String, Object> hash, boolean graceful) {
        Iterator<Map.Entry<String, Object>> it = hash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String key = entry.getKey();
            if (DEPRECATED_PIPELINE_OVERRIDE_SETTINGS.contains(key)) {
                DEPRECATION_LOGGER.deprecated(
                        "Config option \"" + key + "\", set for pipeline \"" + hash.get("pipeline.id") +
                        "\", is deprecated as a pipeline override setting. Please only set it at the process level.");
                it.remove();
            } else if (!PIPELINE_SETTINGS_WHITE_LIST.contains(key)) {
                throw new IllegalArgumentException(
                        "Only pipeline related settings are expected. Received \"" + key +
                        "\". Allowed settings: " + PIPELINE_SETTINGS_WHITE_LIST);
            }
        }
        merge(hash, graceful);
        return this;
    }

    // ========== Format and Display ==========

    public List<String> formatSettings() {
        List<String> output = new ArrayList<>();
        output.add("-------- Logstash Settings (* means modified) ---------");
        for (Setting<?> setting : settings.values()) {
            setting.format(output);
        }
        output.add("--------------- Logstash Settings -------------------");
        return output;
    }

    // ========== Reset ==========

    public void reset() {
        for (Setting<?> setting : settings.values()) {
            setting.reset();
        }
    }

    // ========== YAML Loading ==========

    public SettingsContainer fromYaml(String yamlPath) {
        return fromYaml(yamlPath, "logstash.yml");
    }

    public SettingsContainer fromYaml(String yamlPath, String fileName) {
        Path filePath = Paths.get(yamlPath, fileName);
        Map<String, Object> yamlSettings = readYaml(filePath);
        Map<String, Object> flattened = flattenHash(yamlSettings, "", new LinkedHashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> replaced = (Map<String, Object>) deepReplace(flattened, true);
        merge(replaced, true);
        return this;
    }

    private Map<String, Object> readYaml(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            Yaml yaml = new Yaml();
            Map<String, Object> result = yaml.load(content);
            return result != null ? result : new LinkedHashMap<>();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read YAML file: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flattenHash(Object h, String prefix, Map<String, Object> result) {
        if (!(h instanceof Map)) {
            result.put(prefix, h);
            return result;
        }
        Map<String, Object> map = (Map<String, Object>) h;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            flattenHash(entry.getValue(), key, result);
        }
        return result;
    }

    // ========== Post-Processing ==========

    public void postProcess() {
        if (postProcessCallbacks != null) {
            for (Consumer<SettingsContainer> callback : postProcessCallbacks) {
                callback.accept(this);
            }
        }

        // Re-emit setter-related deprecations after post-processing
        for (Setting<?> setting : settings.values()) {
            setting.observePostProcess();
        }
    }

    public void onPostProcess(Consumer<SettingsContainer> callback) {
        if (postProcessCallbacks == null) {
            postProcessCallbacks = new ArrayList<>();
        }
        postProcessCallbacks.add(callback);
    }

    // ========== Validation ==========

    public void validateAll() {
        // Merge transient settings to see if new settings were added
        merge(new LinkedHashMap<>(transientSettings));

        for (Map.Entry<String, Setting<?>> entry : settings.entrySet()) {
            entry.getValue().validateValue();
        }
    }

    // ========== Substitution Variables ==========

    public Object deepReplace(Object value, boolean refine) {
        ensureSecretStoreInitialized();
        return SubstitutionVariables.deepReplace(value, refine);
    }

    public Object replacePlaceholders(Object value, boolean refine) {
        ensureSecretStoreInitialized();
        return SubstitutionVariables.replacePlaceholders(value, refine);
    }

    private void ensureSecretStoreInitialized() {
        if (!secretStoreLoaded) {
            synchronized (this) {
                if (!secretStoreLoaded) {
                    try {
                        Setting<?> keystoreFileSetting = settings.get("keystore.file");
                        Setting<?> keystoreClassSetting = settings.get("keystore.classname");
                        if (keystoreFileSetting != null && keystoreClassSetting != null) {
                            String keystoreFile = (String) keystoreFileSetting.value();
                            String keystoreClassname = (String) keystoreClassSetting.value();
                            SubstitutionVariables.initSecretStore(keystoreFile, keystoreClassname);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Could not load secret store", e);
                    }
                    secretStoreLoaded = true;
                }
            }
        }
    }

    /**
     * Reset the secret store so it will be re-loaded on next access.
     * Useful for testing.
     */
    public void resetSecretStore() {
        synchronized (this) {
            secretStoreLoaded = false;
            SubstitutionVariables.resetSecretStore();
        }
    }

    // ========== SettingsAccessor Implementation ==========

    @Override
    public String getString(String name) {
        Object value = getValue(name);
        return value != null ? value.toString() : null;
    }

    @Override
    public int getInt(String name) {
        Object value = getValue(name);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    @Override
    public long getLong(String name) {
        Object value = getValue(name);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    @Override
    public boolean getBoolean(String name) {
        Object value = getValue(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @Override
    public Optional<String> getOptionalString(String name) {
        try {
            return Optional.ofNullable(getString(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public String getStringOrDefault(String name, String defaultValue) {
        try {
            String value = getString(name);
            return value != null ? value : defaultValue;
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    @Override
    public int getIntOrDefault(String name, int defaultValue) {
        try {
            return getInt(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public long getLongOrDefault(String name, long defaultValue) {
        try {
            return getLong(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanOrDefault(String name, boolean defaultValue) {
        try {
            return getBoolean(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean has(String name) {
        return registered(name);
    }

    @Override
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(toHash());
    }

    // ========== Equality ==========

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SettingsContainer)) return false;
        return this.toHash().equals(((SettingsContainer) other).toHash());
    }

    @Override
    public int hashCode() {
        return toHash().hashCode();
    }
}
