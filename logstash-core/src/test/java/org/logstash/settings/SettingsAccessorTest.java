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
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SettingsAccessorTest {

    private SettingsAccessor accessor;

    @Before
    public void setUp() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("pipeline.id", "main");
        settings.put("pipeline.workers", 4);
        settings.put("pipeline.batch.size", 125L);
        settings.put("pipeline.unsafe_shutdown", true);
        settings.put("queue.type", "memory");
        settings.put("int.as.string", "42");
        settings.put("bool.as.string", "true");
        accessor = new MapSettingsAccessor(settings);
    }

    @Test
    public void testGetStringReturnsCorrectValue() {
        assertEquals("main", accessor.getString("pipeline.id"));
    }

    @Test
    public void testGetIntReturnsCorrectValue() {
        assertEquals(4, accessor.getInt("pipeline.workers"));
    }

    @Test
    public void testGetLongReturnsCorrectValue() {
        assertEquals(125L, accessor.getLong("pipeline.batch.size"));
    }

    @Test
    public void testGetBooleanReturnsCorrectValue() {
        assertTrue(accessor.getBoolean("pipeline.unsafe_shutdown"));
    }

    @Test
    public void testGetOptionalStringWithExistingKey() {
        Optional<String> result = accessor.getOptionalString("pipeline.id");
        assertTrue(result.isPresent());
        assertEquals("main", result.get());
    }

    @Test
    public void testGetOptionalStringWithMissingKeyReturnsEmpty() {
        Optional<String> result = accessor.getOptionalString("nonexistent.key");
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetStringOrDefaultWithExistingKey() {
        assertEquals("main", accessor.getStringOrDefault("pipeline.id", "fallback"));
    }

    @Test
    public void testGetStringOrDefaultWithMissingKeyReturnsDefault() {
        assertEquals("fallback", accessor.getStringOrDefault("nonexistent.key", "fallback"));
    }

    @Test
    public void testGetIntOrDefaultWithMissingKeyReturnsDefault() {
        assertEquals(99, accessor.getIntOrDefault("nonexistent.key", 99));
    }

    @Test
    public void testGetLongOrDefaultWithMissingKeyReturnsDefault() {
        assertEquals(999L, accessor.getLongOrDefault("nonexistent.key", 999L));
    }

    @Test
    public void testGetBooleanOrDefaultWithMissingKeyReturnsDefault() {
        assertTrue(accessor.getBooleanOrDefault("nonexistent.key", true));
        assertFalse(accessor.getBooleanOrDefault("nonexistent.key", false));
    }

    @Test
    public void testHasWithExistingKey() {
        assertTrue(accessor.has("pipeline.id"));
    }

    @Test
    public void testHasWithMissingKey() {
        assertFalse(accessor.has("nonexistent.key"));
    }

    @Test
    public void testToMapReturnsUnmodifiableCopy() {
        Map<String, Object> map = accessor.toMap();
        assertEquals("main", map.get("pipeline.id"));
        assertEquals(4, map.get("pipeline.workers"));
        try {
            map.put("new.key", "value");
            fail("Expected UnsupportedOperationException when modifying unmodifiable map");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStringThrowsForMissingKey() {
        accessor.getString("nonexistent.key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetIntThrowsForMissingKey() {
        accessor.getInt("nonexistent.key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetLongThrowsForMissingKey() {
        accessor.getLong("nonexistent.key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBooleanThrowsForMissingKey() {
        accessor.getBoolean("nonexistent.key");
    }

    @Test
    public void testGetIntWithStringValueCoercion() {
        assertEquals(42, accessor.getInt("int.as.string"));
    }

    @Test
    public void testGetLongWithStringValueCoercion() {
        assertEquals(42L, accessor.getLong("int.as.string"));
    }

    @Test
    public void testGetBooleanWithStringValueCoercion() {
        assertTrue(accessor.getBoolean("bool.as.string"));
    }

    @Test
    public void testGetIntFromLongValue() {
        // pipeline.batch.size is stored as Long 125L; getInt should still return 125
        assertEquals(125, accessor.getInt("pipeline.batch.size"));
    }

    @Test
    public void testGetLongFromIntValue() {
        // pipeline.workers is stored as Integer 4; getLong should return 4L
        assertEquals(4L, accessor.getLong("pipeline.workers"));
    }

    @Test
    public void testToMapIsolatedFromOriginal() {
        Map<String, Object> original = new HashMap<>();
        original.put("key", "value");
        SettingsAccessor isolated = new MapSettingsAccessor(original);

        // Modify the original map after construction
        original.put("key", "changed");
        original.put("new.key", "new");

        // The accessor should still see the original value
        assertEquals("value", isolated.getString("key"));
        assertFalse(isolated.has("new.key"));
    }
}
