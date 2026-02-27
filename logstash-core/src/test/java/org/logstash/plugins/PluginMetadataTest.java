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

import org.junit.After;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class PluginMetadataTest {

    @After
    public void tearDown() {
        PluginMetadata.reset();
    }

    // ========== Registry Tests ==========

    @Test
    public void forPluginReturnsSameInstanceForSameId() {
        String id = UUID.randomUUID().toString();
        assertSame(PluginMetadata.forPlugin(id), PluginMetadata.forPlugin(id));
    }

    @Test
    public void forPluginReturnsDifferentInstancesForDifferentIds() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        assertNotSame(PluginMetadata.forPlugin(id1), PluginMetadata.forPlugin(id2));
    }

    @Test
    public void existsReturnsFalseWhenNotRegistered() {
        assertFalse(PluginMetadata.exists(UUID.randomUUID().toString()));
    }

    @Test
    public void existsReturnsTrueWhenRegistered() {
        String id = UUID.randomUUID().toString();
        PluginMetadata.forPlugin(id).set("foo", "bar");
        assertTrue(PluginMetadata.exists(id));
    }

    @Test
    public void deleteForPluginRemovesFromRegistry() {
        String id = UUID.randomUUID().toString();
        PluginMetadata.forPlugin(id).set("foo", "bar");
        assertTrue(PluginMetadata.exists(id));
        PluginMetadata.deleteForPlugin(id);
        assertFalse(PluginMetadata.exists(id));
    }

    @Test
    public void deleteForPluginClearsDataInsideRegistry() {
        String id = UUID.randomUUID().toString();
        PluginMetadata pm = PluginMetadata.forPlugin(id);
        pm.set("foo", "bar");
        PluginMetadata.deleteForPlugin(id);
        assertFalse(pm.isSet("foo"));
    }

    @Test
    public void deleteForPluginDoesNotThrowForNonexistent() {
        PluginMetadata.deleteForPlugin("nonexistent");
    }

    @Test
    public void resetClearsAllEntries() {
        PluginMetadata.forPlugin("a").set("key", "val");
        PluginMetadata.forPlugin("b").set("key", "val");
        PluginMetadata.reset();
        assertFalse(PluginMetadata.exists("a"));
        assertFalse(PluginMetadata.exists("b"));
    }

    // ========== Instance Tests ==========

    @Test
    public void setSetsNewValue() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        pm.set("foo", "bar");
        assertEquals("bar", pm.get("foo"));
    }

    @Test
    public void setReturnsNilForNewKey() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        assertNull(pm.set("foo", "bar"));
    }

    @Test
    public void setReturnsPreviousValue() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        pm.set("foo", "bananas");
        assertEquals("bananas", pm.set("foo", "bar"));
    }

    @Test
    public void setWithNullValueUnsetsKey() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        pm.set("foo", "bar");
        pm.set("foo", null);
        assertFalse(pm.isSet("foo"));
    }

    @Test
    public void getReturnsValueWhenSet() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        pm.set("foo", "bananas");
        assertEquals("bananas", pm.get("foo"));
    }

    @Test
    public void getReturnsNullWhenNotSet() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        assertNull(pm.get("foo"));
    }

    @Test
    public void isSetReturnsTrueWhenKeyExists() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        pm.set("foo", "bananas");
        assertTrue(pm.isSet("foo"));
    }

    @Test
    public void isSetReturnsFalseWhenKeyDoesNotExist() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        assertFalse(pm.isSet("foo"));
    }

    @Test
    public void deleteReturnsValueWhenSet() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        pm.set("foo", "bananas");
        assertEquals("bananas", pm.delete("foo"));
    }

    @Test
    public void deleteRemovesKey() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        pm.set("foo", "bananas");
        pm.delete("foo");
        assertFalse(pm.isSet("foo"));
    }

    @Test
    public void deleteReturnsNullWhenNotSet() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        assertNull(pm.delete("foo"));
    }

    @Test
    public void clearRemovesAllKeys() {
        PluginMetadata pm = PluginMetadata.forPlugin(UUID.randomUUID().toString());
        pm.set("foo", "bananas");
        pm.set("bar", "more bananas");
        pm.clear();
        assertFalse(pm.isSet("foo"));
        assertFalse(pm.isSet("bar"));
    }
}
