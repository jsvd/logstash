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

package org.logstash.plugins;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a space to store key/value metadata about a plugin, typically metadata about
 * external resources that can be gleaned during plugin registration.
 * <p>
 * Data should not be persisted across pipeline reloads, and should be cleaned up after a pipeline reload.
 * <ul>
 *   <li>It MUST NOT be used to store processing state</li>
 *   <li>It SHOULD NOT be updated frequently</li>
 *   <li>Individual metadata keys SHOULD NOT be dynamically generated</li>
 * </ul>
 *
 * @since 7.1
 */
public class PluginMetadata {

    private static final Logger LOGGER = LogManager.getLogger(PluginMetadata.class);

    private static final ConcurrentHashMap<String, PluginMetadata> REGISTRY = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Object, Object> datastore = new ConcurrentHashMap<>();

    /**
     * Get the PluginMetadata object corresponding to the given plugin id.
     * If no metadata object exists, it will be created.
     *
     * @param pluginId the plugin id
     * @return the metadata object for the provided plugin id
     */
    public static PluginMetadata forPlugin(String pluginId) {
        return REGISTRY.computeIfAbsent(pluginId, k -> new PluginMetadata());
    }

    /**
     * Determine if we have an existing PluginMetadata object for the given plugin id.
     *
     * @param pluginId the plugin id
     * @return true if metadata exists for the plugin id
     */
    public static boolean exists(String pluginId) {
        return REGISTRY.containsKey(pluginId);
    }

    /**
     * Deletes and clears the contents of an existing PluginMetadata object for the given plugin id.
     *
     * @param pluginId the plugin id
     */
    public static void deleteForPlugin(String pluginId) {
        LOGGER.debug("Removing metadata for plugin {}", pluginId);
        PluginMetadata old = REGISTRY.remove(pluginId);
        if (old != null) {
            old.clear();
        }
    }

    /**
     * Clears all plugin metadata from the registry.
     */
    public static void reset() {
        REGISTRY.clear();
    }

    /**
     * Set the metadata key for this plugin, returning the previous value (if any).
     * If value is null, the key is deleted.
     *
     * @param key   the metadata key
     * @param value the metadata value, or null to delete
     * @return the previous value associated with the key, or null
     */
    public Object set(Object key, Object value) {
        if (value == null) {
            return datastore.remove(key);
        }
        return datastore.put(key, value);
    }

    /**
     * Get the metadata value for the given key.
     *
     * @param key the metadata key
     * @return the value associated with the key, or null
     */
    public Object get(Object key) {
        return datastore.get(key);
    }

    /**
     * Determine whether specific key/value metadata exists for this plugin.
     *
     * @param key the metadata key
     * @return true if the plugin includes metadata for the key
     */
    public boolean isSet(Object key) {
        return datastore.containsKey(key);
    }

    /**
     * Delete the metadata key for this plugin, returning the previous value (if any).
     *
     * @param key the metadata key
     * @return the previous value associated with the key, or null
     */
    public Object delete(Object key) {
        return datastore.remove(key);
    }

    /**
     * Clear all metadata keys for this plugin.
     */
    public void clear() {
        datastore.clear();
    }
}
