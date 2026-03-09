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
package org.logstash.api;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class PipelineInfoTest {

    @Test
    public void testBuilderDefaults() {
        final PipelineInfo info = PipelineInfo.builder()
                .pipelineId("main")
                .build();

        assertEquals("main", info.getPipelineId());
        assertNull(info.getEphemeralId());
        assertNull(info.getConfigHash());
        assertEquals(1, info.getWorkers());
        assertEquals(125, info.getBatchSize());
        assertEquals(50, info.getBatchDelay());
        assertFalse(info.isDlqEnabled());
        assertNull(info.getDlqPath());
        assertFalse(info.isSystemPipeline());
        assertTrue(info.isReloadable());
    }

    @Test
    public void testBuilderAllFields() {
        final PipelineInfo info = PipelineInfo.builder()
                .pipelineId("main")
                .ephemeralId("eph-123")
                .configHash("sha256abc")
                .workers(4)
                .batchSize(250)
                .batchDelay(100)
                .dlqEnabled(true)
                .dlqPath("/var/logstash/dlq/main")
                .isSystemPipeline(false)
                .reloadable(true)
                .build();

        assertEquals("main", info.getPipelineId());
        assertEquals("eph-123", info.getEphemeralId());
        assertEquals("sha256abc", info.getConfigHash());
        assertEquals(4, info.getWorkers());
        assertEquals(250, info.getBatchSize());
        assertEquals(100, info.getBatchDelay());
        assertTrue(info.isDlqEnabled());
        assertEquals("/var/logstash/dlq/main", info.getDlqPath());
        assertFalse(info.isSystemPipeline());
        assertTrue(info.isReloadable());
    }

    @Test
    public void testSystemPipeline() {
        final PipelineInfo info = PipelineInfo.builder()
                .pipelineId(".monitoring-logstash")
                .isSystemPipeline(true)
                .reloadable(false)
                .build();

        assertTrue(info.isSystemPipeline());
        assertFalse(info.isReloadable());
    }

    @Test
    public void testToMap() {
        final PipelineInfo info = PipelineInfo.builder()
                .pipelineId("main")
                .ephemeralId("eph-456")
                .configHash("hash123")
                .workers(8)
                .batchSize(500)
                .batchDelay(25)
                .dlqEnabled(true)
                .dlqPath("/tmp/dlq")
                .isSystemPipeline(false)
                .reloadable(true)
                .build();

        final Map<String, Object> map = info.toMap();
        assertEquals("main", map.get("pipeline_id"));
        assertEquals("eph-456", map.get("ephemeral_id"));
        assertEquals("hash123", map.get("config_hash"));
        assertEquals(8, map.get("workers"));
        assertEquals(500, map.get("batch_size"));
        assertEquals(25L, map.get("batch_delay"));
        assertEquals(true, map.get("dlq_enabled"));
        assertEquals("/tmp/dlq", map.get("dlq_path"));
        assertEquals(false, map.get("is_system_pipeline"));
        assertEquals(true, map.get("reloadable"));
        assertEquals(10, map.size());
    }

    @Test
    public void testToMapIsImmutable() {
        final PipelineInfo info = PipelineInfo.builder().pipelineId("main").build();
        final Map<String, Object> map = info.toMap();
        try {
            map.put("extra", "value");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testEquality() {
        final PipelineInfo p1 = PipelineInfo.builder()
                .pipelineId("main").workers(4).batchSize(125).build();
        final PipelineInfo p2 = PipelineInfo.builder()
                .pipelineId("main").workers(4).batchSize(125).build();
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testInequalityPipelineId() {
        final PipelineInfo p1 = PipelineInfo.builder().pipelineId("main").build();
        final PipelineInfo p2 = PipelineInfo.builder().pipelineId("secondary").build();
        assertNotEquals(p1, p2);
    }

    @Test
    public void testInequalityWorkers() {
        final PipelineInfo p1 = PipelineInfo.builder().pipelineId("main").workers(4).build();
        final PipelineInfo p2 = PipelineInfo.builder().pipelineId("main").workers(8).build();
        assertNotEquals(p1, p2);
    }

    @Test
    public void testInequalityDlqEnabled() {
        final PipelineInfo p1 = PipelineInfo.builder().pipelineId("main").dlqEnabled(true).build();
        final PipelineInfo p2 = PipelineInfo.builder().pipelineId("main").dlqEnabled(false).build();
        assertNotEquals(p1, p2);
    }

    @Test
    public void testToString() {
        final PipelineInfo info = PipelineInfo.builder()
                .pipelineId("main").workers(4).batchSize(125).reloadable(true)
                .build();
        final String str = info.toString();
        assertTrue(str.contains("main"));
        assertTrue(str.contains("4"));
        assertTrue(str.contains("125"));
        assertTrue(str.contains("true"));
    }

    @Test
    public void testEqualsSameObject() {
        final PipelineInfo p = PipelineInfo.builder().pipelineId("main").build();
        assertEquals(p, p);
    }

    @Test
    public void testEqualsNull() {
        final PipelineInfo p = PipelineInfo.builder().pipelineId("main").build();
        assertNotEquals(null, p);
    }

    @Test
    public void testEqualsDifferentType() {
        final PipelineInfo p = PipelineInfo.builder().pipelineId("main").build();
        assertNotEquals("not a pipeline", p);
    }
}
