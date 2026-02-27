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

package org.logstash.config.source;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PipelineConfigPartsTest {

    @Test
    public void testBasicConstruction() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "file", "/etc/logstash/conf.d/input.conf", "input { stdin {} }");
        Map<String, Object> settings = new HashMap<>();
        settings.put("pipeline.workers", 4);

        PipelineConfigParts pcp = new PipelineConfigParts("main", Collections.singletonList(part), settings);

        assertEquals("main", pcp.getPipelineId());
        assertEquals(1, pcp.getConfigParts().size());
        assertEquals(4, pcp.getSettings().get("pipeline.workers"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullPipelineIdThrows() {
        new PipelineConfigParts(null, Collections.emptyList(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullConfigPartsThrows() {
        new PipelineConfigParts("main", null, null);
    }

    @Test
    public void testNullSettingsResultsInEmptyMap() {
        PipelineConfigParts pcp = new PipelineConfigParts("main", Collections.emptyList(), null);
        assertNotNull(pcp.getSettings());
        assertTrue(pcp.getSettings().isEmpty());
    }

    @Test
    public void testGetConfigStringSinglePart() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "string", "inline", "input { stdin {} }");
        PipelineConfigParts pcp = new PipelineConfigParts("test", Collections.singletonList(part), null);

        assertEquals("input { stdin {} }", pcp.getConfigString());
    }

    @Test
    public void testGetConfigStringMultipleParts() {
        PipelineConfigParts.ConfigPart part1 = new PipelineConfigParts.ConfigPart(
                "file", "input.conf", "input { stdin {} }");
        PipelineConfigParts.ConfigPart part2 = new PipelineConfigParts.ConfigPart(
                "file", "output.conf", "output { stdout {} }");

        PipelineConfigParts pcp = new PipelineConfigParts("test",
                Arrays.asList(part1, part2), null);

        String configString = pcp.getConfigString();
        assertTrue(configString.contains("input { stdin {} }"));
        assertTrue(configString.contains("output { stdout {} }"));
    }

    @Test
    public void testGetConfigStringAddsNewlineBetweenParts() {
        PipelineConfigParts.ConfigPart part1 = new PipelineConfigParts.ConfigPart(
                "file", "a.conf", "input { stdin {} }");
        PipelineConfigParts.ConfigPart part2 = new PipelineConfigParts.ConfigPart(
                "file", "b.conf", "output { stdout {} }");

        PipelineConfigParts pcp = new PipelineConfigParts("test",
                Arrays.asList(part1, part2), null);

        // Since part1 doesn't end with \n, a newline should be inserted
        assertTrue(pcp.getConfigString().contains("}\n"));
    }

    @Test
    public void testGetConfigStringNoExtraNewlineWhenPartEndsWithNewline() {
        PipelineConfigParts.ConfigPart part1 = new PipelineConfigParts.ConfigPart(
                "file", "a.conf", "input { stdin {} }\n");
        PipelineConfigParts.ConfigPart part2 = new PipelineConfigParts.ConfigPart(
                "file", "b.conf", "output { stdout {} }");

        PipelineConfigParts pcp = new PipelineConfigParts("test",
                Arrays.asList(part1, part2), null);

        // Should not have double newline
        assertFalse(pcp.getConfigString().contains("\n\n"));
    }

    @Test
    public void testGetConfigHash() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "string", "inline", "input { stdin {} }");
        PipelineConfigParts pcp = new PipelineConfigParts("test",
                Collections.singletonList(part), null);

        String hash = pcp.getConfigHash();
        assertNotNull(hash);
        // SHA-256 produces a 64-character hex string
        assertEquals(64, hash.length());
        // Hash should be deterministic
        assertEquals(hash, pcp.getConfigHash());
    }

    @Test
    public void testDifferentConfigProducesDifferentHash() {
        PipelineConfigParts.ConfigPart part1 = new PipelineConfigParts.ConfigPart(
                "string", "a", "input { stdin {} }");
        PipelineConfigParts pcp1 = new PipelineConfigParts("test",
                Collections.singletonList(part1), null);

        PipelineConfigParts.ConfigPart part2 = new PipelineConfigParts.ConfigPart(
                "string", "b", "output { stdout {} }");
        PipelineConfigParts pcp2 = new PipelineConfigParts("test",
                Collections.singletonList(part2), null);

        assertNotEquals(pcp1.getConfigHash(), pcp2.getConfigHash());
    }

    @Test
    public void testEqualsAndHashCode() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "string", "inline", "input { stdin {} }");
        PipelineConfigParts pcp1 = new PipelineConfigParts("test",
                Collections.singletonList(part), null);
        PipelineConfigParts pcp2 = new PipelineConfigParts("test",
                Collections.singletonList(part), null);

        assertEquals(pcp1, pcp2);
        assertEquals(pcp1.hashCode(), pcp2.hashCode());
    }

    @Test
    public void testNotEqualsDifferentPipelineId() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "string", "inline", "input { stdin {} }");
        PipelineConfigParts pcp1 = new PipelineConfigParts("test-a",
                Collections.singletonList(part), null);
        PipelineConfigParts pcp2 = new PipelineConfigParts("test-b",
                Collections.singletonList(part), null);

        assertNotEquals(pcp1, pcp2);
    }

    @Test
    public void testConfigPartsListIsImmutable() {
        List<PipelineConfigParts.ConfigPart> mutableList = new ArrayList<>();
        mutableList.add(new PipelineConfigParts.ConfigPart("string", "a", "input {}"));

        PipelineConfigParts pcp = new PipelineConfigParts("test", mutableList, null);

        // Modify the original list
        mutableList.add(new PipelineConfigParts.ConfigPart("string", "b", "output {}"));

        // PipelineConfigParts should not be affected
        assertEquals(1, pcp.getConfigParts().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConfigPartsListCannotBeModifiedDirectly() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "string", "a", "input {}");
        PipelineConfigParts pcp = new PipelineConfigParts("test",
                Collections.singletonList(part), null);

        pcp.getConfigParts().add(new PipelineConfigParts.ConfigPart("string", "b", "output {}"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSettingsMapCannotBeModifiedDirectly() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("key", "value");
        PipelineConfigParts pcp = new PipelineConfigParts("test",
                Collections.emptyList(), settings);

        pcp.getSettings().put("new-key", "new-value");
    }

    @Test
    public void testSettingsMapIsDefensivelyCopied() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("key", "value");
        PipelineConfigParts pcp = new PipelineConfigParts("test",
                Collections.emptyList(), settings);

        // Modify original
        settings.put("key2", "value2");

        // Should not be affected
        assertEquals(1, pcp.getSettings().size());
        assertFalse(pcp.getSettings().containsKey("key2"));
    }

    @Test
    public void testToString() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "string", "inline", "input { stdin {} }");
        PipelineConfigParts pcp = new PipelineConfigParts("my-pipeline",
                Collections.singletonList(part), null);

        String str = pcp.toString();
        assertTrue(str.contains("my-pipeline"));
        assertTrue(str.contains("parts=1"));
    }

    // --- ConfigPart tests ---

    @Test
    public void testConfigPartConstruction() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "file", "/etc/logstash/input.conf", "input { stdin {} }", 5, 3);

        assertEquals("file", part.getProtocol());
        assertEquals("/etc/logstash/input.conf", part.getId());
        assertEquals("input { stdin {} }", part.getText());
        assertEquals(5, part.getLine());
        assertEquals(3, part.getColumn());
    }

    @Test
    public void testConfigPartConvenienceConstructor() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "string", "inline", "output { stdout {} }");

        assertEquals("string", part.getProtocol());
        assertEquals("inline", part.getId());
        assertEquals("output { stdout {} }", part.getText());
        assertEquals(0, part.getLine());
        assertEquals(0, part.getColumn());
    }

    @Test(expected = NullPointerException.class)
    public void testConfigPartNullProtocolThrows() {
        new PipelineConfigParts.ConfigPart(null, "id", "text");
    }

    @Test(expected = NullPointerException.class)
    public void testConfigPartNullIdThrows() {
        new PipelineConfigParts.ConfigPart("file", null, "text");
    }

    @Test(expected = NullPointerException.class)
    public void testConfigPartNullTextThrows() {
        new PipelineConfigParts.ConfigPart("file", "id", null);
    }

    @Test
    public void testConfigPartEquals() {
        PipelineConfigParts.ConfigPart part1 = new PipelineConfigParts.ConfigPart(
                "file", "a.conf", "text", 1, 0);
        PipelineConfigParts.ConfigPart part2 = new PipelineConfigParts.ConfigPart(
                "file", "a.conf", "text", 1, 0);

        assertEquals(part1, part2);
        assertEquals(part1.hashCode(), part2.hashCode());
    }

    @Test
    public void testConfigPartNotEquals() {
        PipelineConfigParts.ConfigPart part1 = new PipelineConfigParts.ConfigPart(
                "file", "a.conf", "text1");
        PipelineConfigParts.ConfigPart part2 = new PipelineConfigParts.ConfigPart(
                "file", "a.conf", "text2");

        assertNotEquals(part1, part2);
    }

    @Test
    public void testConfigPartToString() {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "file", "/etc/logstash/input.conf", "input {}", 5, 3);
        String str = part.toString();
        assertTrue(str.contains("file"));
        assertTrue(str.contains("/etc/logstash/input.conf"));
        assertTrue(str.contains("line=5"));
        assertTrue(str.contains("column=3"));
    }
}
