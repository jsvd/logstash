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

import java.time.Duration;

import static org.junit.Assert.*;

public class DlqConfigurationTest {

    @Test
    public void testConstructionWithAllFields() {
        final Duration flushInterval = Duration.ofMillis(5000);
        final DlqConfiguration config = new DlqConfiguration(
                true, "/var/logstash/dlq", 1073741824L, flushInterval, "drop_newer"
        );

        assertTrue(config.isEnabled());
        assertEquals("/var/logstash/dlq", config.getDlqPath());
        assertEquals(1073741824L, config.getMaxBytes());
        assertEquals(flushInterval, config.getFlushInterval());
        assertEquals("drop_newer", config.getStorageType());
    }

    @Test
    public void testConstructionWithDisabledState() {
        final DlqConfiguration config = new DlqConfiguration(
                false, null, 0L, Duration.ZERO, null
        );

        assertFalse(config.isEnabled());
        assertNull(config.getDlqPath());
        assertEquals(0L, config.getMaxBytes());
        assertEquals(Duration.ZERO, config.getFlushInterval());
        assertNull(config.getStorageType());
    }

    @Test
    public void testDisabledFactory() {
        final DlqConfiguration config = DlqConfiguration.disabled();

        assertFalse(config.isEnabled());
        assertNull(config.getDlqPath());
        assertEquals(0L, config.getMaxBytes());
        assertEquals(Duration.ZERO, config.getFlushInterval());
        assertNull(config.getStorageType());
    }

    @Test
    public void testGettersWithVariousValues() {
        final Duration flushInterval = Duration.ofSeconds(30);
        final DlqConfiguration config = new DlqConfiguration(
                true, "/tmp/dlq/main", 536870912L, flushInterval, "drop_older"
        );

        assertTrue(config.isEnabled());
        assertEquals("/tmp/dlq/main", config.getDlqPath());
        assertEquals(536870912L, config.getMaxBytes());
        assertEquals(Duration.ofSeconds(30), config.getFlushInterval());
        assertEquals("drop_older", config.getStorageType());
    }

    @Test
    public void testDifferentMaxBytesValues() {
        final DlqConfiguration smallConfig = new DlqConfiguration(
                true, "/dlq", 1024L, Duration.ofMillis(100), "drop_newer"
        );
        assertEquals(1024L, smallConfig.getMaxBytes());

        final DlqConfiguration largeConfig = new DlqConfiguration(
                true, "/dlq", Long.MAX_VALUE, Duration.ofMillis(100), "drop_newer"
        );
        assertEquals(Long.MAX_VALUE, largeConfig.getMaxBytes());
    }
}
