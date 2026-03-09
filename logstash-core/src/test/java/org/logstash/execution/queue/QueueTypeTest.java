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

package org.logstash.execution.queue;

import org.junit.Test;
import org.logstash.exceptions.ConfigurationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class QueueTypeTest {

    @Test
    public void testFromStringMemory() {
        assertSame(QueueType.MEMORY, QueueType.fromString("memory"));
    }

    @Test
    public void testFromStringPersisted() {
        assertSame(QueueType.PERSISTED, QueueType.fromString("persisted"));
    }

    @Test(expected = ConfigurationException.class)
    public void testFromStringInvalidThrowsConfigurationException() {
        QueueType.fromString("invalid");
    }

    @Test
    public void testFromStringInvalidExceptionMessage() {
        try {
            QueueType.fromString("badvalue");
        } catch (ConfigurationException e) {
            assertEquals(
                "Invalid setting `badvalue` for `queue.type`, supported types are: 'memory' or 'persisted'",
                e.getMessage()
            );
        }
    }

    @Test
    public void testFromStringIsCaseInsensitive() {
        assertSame(QueueType.MEMORY, QueueType.fromString("MEMORY"));
        assertSame(QueueType.MEMORY, QueueType.fromString("Memory"));
        assertSame(QueueType.PERSISTED, QueueType.fromString("PERSISTED"));
        assertSame(QueueType.PERSISTED, QueueType.fromString("Persisted"));
    }

    @Test
    public void testGetLabelReturnsCorrectValues() {
        assertEquals("memory", QueueType.MEMORY.getLabel());
        assertEquals("persisted", QueueType.PERSISTED.getLabel());
    }
}
