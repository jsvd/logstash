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

import static org.junit.Assert.*;

public class QueueConfigurationTest {

    @Test
    public void testConstructionWithAllFields() {
        final QueueConfiguration config = new QueueConfiguration(
                "persisted", "/var/logstash/queue", 125, 8
        );

        assertEquals("persisted", config.getQueueType());
        assertEquals("/var/logstash/queue", config.getQueuePath());
        assertEquals(125, config.getBatchSize());
        assertEquals(8, config.getWorkers());
    }

    @Test
    public void testMemoryQueueType() {
        final QueueConfiguration config = new QueueConfiguration(
                "memory", "/tmp/queue", 250, 4
        );

        assertEquals("memory", config.getQueueType());
        assertEquals("/tmp/queue", config.getQueuePath());
        assertEquals(250, config.getBatchSize());
        assertEquals(4, config.getWorkers());
    }

    @Test
    public void testGetters() {
        final QueueConfiguration config = new QueueConfiguration(
                "persisted", "/data/queue/main", 500, 16
        );

        assertEquals("persisted", config.getQueueType());
        assertEquals("/data/queue/main", config.getQueuePath());
        assertEquals(500, config.getBatchSize());
        assertEquals(16, config.getWorkers());
    }

    @Test
    public void testSingleWorker() {
        final QueueConfiguration config = new QueueConfiguration(
                "memory", "/queue", 1, 1
        );

        assertEquals(1, config.getBatchSize());
        assertEquals(1, config.getWorkers());
    }

    @Test
    public void testDefaultBatchSize() {
        // Logstash default batch size is 125
        final QueueConfiguration config = new QueueConfiguration(
                "memory", "/queue", 125, 1
        );

        assertEquals(125, config.getBatchSize());
    }
}
