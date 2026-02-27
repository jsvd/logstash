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

package org.logstash.execution;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Tests for the {@link ConfigSource} interface using simple stub implementations.
 */
public class ConfigSourceTest {

    @Test
    public void testFetchReturningConfigs() {
        final Map<String, Object> configs = new HashMap<>();
        configs.put("main", "input { stdin {} } output { stdout {} }");
        configs.put("secondary", "input { beats { port => 5044 } }");

        final ConfigSource source = () -> Optional.of(configs);

        final Optional<Map<String, Object>> result = source.fetch();
        assertTrue("fetch should return a present Optional", result.isPresent());
        assertEquals(2, result.get().size());
        assertEquals("input { stdin {} } output { stdout {} }", result.get().get("main"));
        assertEquals("input { beats { port => 5044 } }", result.get().get("secondary"));
    }

    @Test
    public void testFetchReturningEmpty() {
        final ConfigSource source = Optional::empty;

        final Optional<Map<String, Object>> result = source.fetch();
        assertFalse("fetch should return empty Optional on failure", result.isPresent());
    }

    @Test
    public void testFetchReturningEmptyMap() {
        final ConfigSource source = () -> Optional.of(new HashMap<>());

        final Optional<Map<String, Object>> result = source.fetch();
        assertTrue("fetch should return a present Optional", result.isPresent());
        assertTrue("the config map should be empty", result.get().isEmpty());
    }

    @Test
    public void testFetchReturningDifferentResultsOnSubsequentCalls() {
        final Map<String, Object> firstConfigs = new HashMap<>();
        firstConfigs.put("pipeline1", "config1");

        final Map<String, Object> secondConfigs = new HashMap<>();
        secondConfigs.put("pipeline1", "config1");
        secondConfigs.put("pipeline2", "config2");

        final boolean[] firstCall = {true};
        final ConfigSource source = () -> {
            if (firstCall[0]) {
                firstCall[0] = false;
                return Optional.of(firstConfigs);
            }
            return Optional.of(secondConfigs);
        };

        final Optional<Map<String, Object>> first = source.fetch();
        assertTrue(first.isPresent());
        assertEquals(1, first.get().size());

        final Optional<Map<String, Object>> second = source.fetch();
        assertTrue(second.isPresent());
        assertEquals(2, second.get().size());
    }

    @Test
    public void testLambdaImplementation() {
        // Verify that ConfigSource can be implemented as a lambda
        final ConfigSource source = () -> Optional.of(Map.of("test", "config"));
        assertTrue(source.fetch().isPresent());
    }
}
