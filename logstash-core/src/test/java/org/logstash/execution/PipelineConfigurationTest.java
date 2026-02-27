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
import org.logstash.common.IncompleteSourceWithMetadataException;
import org.logstash.common.SourceWithMetadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PipelineConfigurationTest {

    @Test
    public void testConstructionWithAllFields() throws IncompleteSourceWithMetadataException {
        final SourceWithMetadata source = new SourceWithMetadata("file", "/tmp/test.conf", "input { stdin {} }");
        final List<SourceWithMetadata> configParts = Collections.singletonList(source);

        final PipelineConfiguration config = new PipelineConfiguration(
                "main", "input { stdin {} }", "test-ephemeral-id", configParts, false, true
        );

        assertEquals("main", config.getPipelineId());
        assertEquals("input { stdin {} }", config.getConfigString());
        assertEquals("test-ephemeral-id", config.getEphemeralId());
        assertEquals(1, config.getConfigParts().size());
        assertEquals(source, config.getConfigParts().get(0));
        assertFalse(config.isSystemPipeline());
        assertTrue(config.isReloadable());
    }

    @Test
    public void testConstructionWithSystemPipeline() throws IncompleteSourceWithMetadataException {
        final SourceWithMetadata source = new SourceWithMetadata("string", "config", "input { generator {} }");

        final PipelineConfiguration config = new PipelineConfiguration(
                ".monitoring", "input { generator {} }", "eph-123",
                Collections.singletonList(source), true, false
        );

        assertEquals(".monitoring", config.getPipelineId());
        assertTrue(config.isSystemPipeline());
        assertFalse(config.isReloadable());
    }

    @Test
    public void testDefaultEphemeralIdGeneration() {
        final PipelineConfiguration config = new PipelineConfiguration(
                "main", "input { stdin {} }", Collections.emptyList(), false, true
        );

        assertNotNull(config.getEphemeralId());
        assertFalse(config.getEphemeralId().isEmpty());
    }

    @Test
    public void testDefaultEphemeralIdIsUniqueAcrossInstances() {
        final PipelineConfiguration config1 = new PipelineConfiguration(
                "main", "input { stdin {} }", Collections.emptyList(), false, true
        );
        final PipelineConfiguration config2 = new PipelineConfiguration(
                "main", "input { stdin {} }", Collections.emptyList(), false, true
        );

        assertNotEquals(config1.getEphemeralId(), config2.getEphemeralId());
    }

    @Test
    public void testNullEphemeralIdGeneratesDefault() {
        final PipelineConfiguration config = new PipelineConfiguration(
                "main", "input { stdin {} }", null, Collections.emptyList(), false, true
        );

        assertNotNull(config.getEphemeralId());
        assertFalse(config.getEphemeralId().isEmpty());
    }

    @Test
    public void testConfigPartsIsImmutableCopy() throws IncompleteSourceWithMetadataException {
        final SourceWithMetadata source1 = new SourceWithMetadata("file", "/tmp/a.conf", "input { stdin {} }");
        final SourceWithMetadata source2 = new SourceWithMetadata("file", "/tmp/b.conf", "output { stdout {} }");
        final List<SourceWithMetadata> mutableList = new java.util.ArrayList<>(Arrays.asList(source1, source2));

        final PipelineConfiguration config = new PipelineConfiguration(
                "main", "input { stdin {} } output { stdout {} }", "eph-id", mutableList, false, true
        );

        // Modify the original list
        mutableList.add(new SourceWithMetadata("string", "extra", "filter {}"));

        // The PipelineConfiguration's list should not be affected
        assertEquals(2, config.getConfigParts().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConfigPartsCannotBeModified() throws IncompleteSourceWithMetadataException {
        final SourceWithMetadata source = new SourceWithMetadata("file", "/tmp/a.conf", "input { stdin {} }");
        final PipelineConfiguration config = new PipelineConfiguration(
                "main", "input { stdin {} }", "eph-id", Collections.singletonList(source), false, true
        );

        config.getConfigParts().add(new SourceWithMetadata("string", "extra", "filter {}"));
    }

    @Test
    public void testNullConfigPartsResultsInEmptyList() {
        final PipelineConfiguration config = new PipelineConfiguration(
                "main", "input { stdin {} }", "eph-id", null, false, true
        );

        assertNotNull(config.getConfigParts());
        assertTrue(config.getConfigParts().isEmpty());
    }

    @Test
    public void testGetters() throws IncompleteSourceWithMetadataException {
        final SourceWithMetadata source = new SourceWithMetadata("file", "/etc/logstash/conf.d/test.conf", "input { beats { port => 5044 } }");
        final PipelineConfiguration config = new PipelineConfiguration(
                "beats-pipeline",
                "input { beats { port => 5044 } }",
                "custom-eph-id",
                Collections.singletonList(source),
                false,
                true
        );

        assertEquals("beats-pipeline", config.getPipelineId());
        assertEquals("input { beats { port => 5044 } }", config.getConfigString());
        assertEquals("custom-eph-id", config.getEphemeralId());
        assertEquals(1, config.getConfigParts().size());
        assertFalse(config.isSystemPipeline());
        assertTrue(config.isReloadable());
    }
}
