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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LocalConfigSourceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // --- matches() tests ---

    @Test
    public void testMatchesWithConfigPaths() {
        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList("/some/path"), null, "main");
        assertTrue(source.matches());
    }

    @Test
    public void testMatchesWithConfigString() {
        LocalConfigSource source = new LocalConfigSource(
                null, "input { stdin {} }", "main");
        assertTrue(source.matches());
    }

    @Test
    public void testDoesNotMatchWhenEmpty() {
        LocalConfigSource source = new LocalConfigSource(
                Collections.emptyList(), null, "main");
        assertFalse(source.matches());
    }

    @Test
    public void testDoesNotMatchWithNullAndEmpty() {
        LocalConfigSource source = new LocalConfigSource(null, "", "main");
        assertFalse(source.matches());
    }

    // --- hasConflict() tests ---

    @Test
    public void testHasConflictWhenBothSet() {
        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList("/some/path"), "input {}", "main");
        assertTrue(source.hasConflict());
    }

    @Test
    public void testNoConflictWithOnlyPaths() {
        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList("/some/path"), null, "main");
        assertFalse(source.hasConflict());
    }

    @Test
    public void testNoConflictWithOnlyString() {
        LocalConfigSource source = new LocalConfigSource(
                null, "input {}", "main");
        assertFalse(source.hasConflict());
    }

    @Test
    public void testConflictMessage() {
        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList("/some/path"), "input {}", "my-pipeline");
        String msg = source.getConflictMessage();
        assertTrue(msg.contains("my-pipeline"));
        assertTrue(msg.contains("Both config paths and config string"));
    }

    @Test
    public void testNoConflictMessageWhenNoConflict() {
        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList("/some/path"), null, "main");
        assertEquals("", source.getConflictMessage());
    }

    // --- loadFromPaths() tests ---

    @Test
    public void testLoadFromSingleFile() throws IOException {
        File configFile = tempFolder.newFile("input.conf");
        Files.write(configFile.toPath(), "input { stdin {} }".getBytes(StandardCharsets.UTF_8));

        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList(configFile.getAbsolutePath()), null, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromPaths(
                Collections.singletonList(configFile.getAbsolutePath()));

        assertEquals(1, parts.size());
        assertEquals("file", parts.get(0).getProtocol());
        assertEquals("input { stdin {} }", parts.get(0).getText());
    }

    @Test
    public void testLoadFromMultipleFiles() throws IOException {
        File file1 = tempFolder.newFile("01-input.conf");
        File file2 = tempFolder.newFile("02-output.conf");
        Files.write(file1.toPath(), "input { stdin {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(file2.toPath(), "output { stdout {} }".getBytes(StandardCharsets.UTF_8));

        LocalConfigSource source = new LocalConfigSource(
                Arrays.asList(file1.getAbsolutePath(), file2.getAbsolutePath()),
                null, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromPaths(
                Arrays.asList(file1.getAbsolutePath(), file2.getAbsolutePath()));

        assertEquals(2, parts.size());
    }

    @Test
    public void testLoadFromGlobPattern() throws IOException {
        File confDir = tempFolder.newFolder("conf.d");
        File file1 = new File(confDir, "input.conf");
        File file2 = new File(confDir, "output.conf");
        Files.write(file1.toPath(), "input { stdin {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(file2.toPath(), "output { stdout {} }".getBytes(StandardCharsets.UTF_8));

        String globPattern = confDir.getAbsolutePath() + "/*.conf";
        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList(globPattern), null, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromPaths(
                Collections.singletonList(globPattern));

        assertEquals(2, parts.size());
        // Files should be sorted alphabetically
        assertTrue(parts.get(0).getId().contains("input.conf"));
        assertTrue(parts.get(1).getId().contains("output.conf"));
    }

    @Test
    public void testSkipsTempFiles() throws IOException {
        File confDir = tempFolder.newFolder("configs");
        File normalFile = new File(confDir, "input.conf");
        File tempFile = new File(confDir, "input.conf~");
        Files.write(normalFile.toPath(), "input { stdin {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(tempFile.toPath(), "old content".getBytes(StandardCharsets.UTF_8));

        String globPattern = confDir.getAbsolutePath() + "/*";
        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList(globPattern), null, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromPaths(
                Collections.singletonList(globPattern));

        assertEquals(1, parts.size());
        assertTrue(parts.get(0).getId().contains("input.conf"));
        assertFalse(parts.get(0).getId().endsWith("~"));
    }

    @Test
    public void testFilesSortedAlphabetically() throws IOException {
        File confDir = tempFolder.newFolder("sorted");
        File fileC = new File(confDir, "c.conf");
        File fileA = new File(confDir, "a.conf");
        File fileB = new File(confDir, "b.conf");
        Files.write(fileC.toPath(), "filter { }".getBytes(StandardCharsets.UTF_8));
        Files.write(fileA.toPath(), "input { }".getBytes(StandardCharsets.UTF_8));
        Files.write(fileB.toPath(), "output { }".getBytes(StandardCharsets.UTF_8));

        String globPattern = confDir.getAbsolutePath() + "/*.conf";
        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList(globPattern), null, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromPaths(
                Collections.singletonList(globPattern));

        assertEquals(3, parts.size());
        assertTrue(parts.get(0).getId().endsWith("a.conf"));
        assertTrue(parts.get(1).getId().endsWith("b.conf"));
        assertTrue(parts.get(2).getId().endsWith("c.conf"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUtf8ThrowsException() throws IOException {
        File badFile = tempFolder.newFile("bad-encoding.conf");
        // Write invalid UTF-8 bytes
        byte[] invalidUtf8 = {(byte) 0xC0, (byte) 0xAF, (byte) 0xFF};
        Files.write(badFile.toPath(), invalidUtf8);

        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList(badFile.getAbsolutePath()), null, "main");

        source.loadFromPaths(Collections.singletonList(badFile.getAbsolutePath()));
    }

    @Test
    public void testLoadFromDirectory() throws IOException {
        File confDir = tempFolder.newFolder("dirtest");
        File file1 = new File(confDir, "input.conf");
        Files.write(file1.toPath(), "input { stdin {} }".getBytes(StandardCharsets.UTF_8));

        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList(confDir.getAbsolutePath()), null, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromPaths(
                Collections.singletonList(confDir.getAbsolutePath()));

        assertEquals(1, parts.size());
    }

    @Test
    public void testLoadFromEmptyPathList() {
        LocalConfigSource source = new LocalConfigSource(
                Collections.emptyList(), null, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromPaths(
                Collections.emptyList());

        assertTrue(parts.isEmpty());
    }

    // --- loadFromString() tests ---

    @Test
    public void testLoadFromStringComplete() {
        String config = "input { stdin {} } output { stdout {} }";
        LocalConfigSource source = new LocalConfigSource(null, config, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromString(config, "main");

        // Should have exactly 1 part (no defaults needed)
        assertEquals(1, parts.size());
        assertEquals("string", parts.get(0).getProtocol());
        assertEquals(config, parts.get(0).getText());
    }

    @Test
    public void testLoadFromStringAddsDefaultInput() {
        String config = "filter { mutate {} } output { stdout {} }";
        LocalConfigSource source = new LocalConfigSource(null, config, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromString(config, "main");

        assertEquals(2, parts.size());
        // First should be the default input
        assertEquals("default-input", parts.get(0).getId());
        assertEquals(ConfigDefaults.defaultInput(), parts.get(0).getText());
        // Second should be the actual config
        assertEquals(config, parts.get(1).getText());
    }

    @Test
    public void testLoadFromStringAddsDefaultOutput() {
        String config = "input { stdin {} } filter { mutate {} }";
        LocalConfigSource source = new LocalConfigSource(null, config, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromString(config, "main");

        assertEquals(2, parts.size());
        // First should be the actual config
        assertEquals(config, parts.get(0).getText());
        // Second should be the default output
        assertEquals("default-output", parts.get(1).getId());
        assertEquals(ConfigDefaults.defaultOutput(), parts.get(1).getText());
    }

    @Test
    public void testLoadFromStringAddsBothDefaults() {
        String config = "filter { mutate {} }";
        LocalConfigSource source = new LocalConfigSource(null, config, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromString(config, "main");

        assertEquals(3, parts.size());
        assertEquals("default-input", parts.get(0).getId());
        assertEquals(config, parts.get(1).getText());
        assertEquals("default-output", parts.get(2).getId());
    }

    @Test
    public void testLoadFromStringEmpty() {
        LocalConfigSource source = new LocalConfigSource(null, "", "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromString("", "main");
        assertTrue(parts.isEmpty());
    }

    @Test
    public void testLoadFromStringNull() {
        LocalConfigSource source = new LocalConfigSource(null, null, "main");

        List<PipelineConfigParts.ConfigPart> parts = source.loadFromString(null, "main");
        assertTrue(parts.isEmpty());
    }

    // --- pipelineConfigs() integration tests ---

    @Test
    public void testPipelineConfigsFromPaths() throws IOException {
        File configFile = tempFolder.newFile("pipeline.conf");
        Files.write(configFile.toPath(),
                "input { stdin {} } output { stdout {} }".getBytes(StandardCharsets.UTF_8));

        LocalConfigSource source = new LocalConfigSource(
                Collections.singletonList(configFile.getAbsolutePath()), null, "my-pipeline");

        List<PipelineConfigParts> configs = source.pipelineConfigs();

        assertEquals(1, configs.size());
        assertEquals("my-pipeline", configs.get(0).getPipelineId());
        assertNotNull(configs.get(0).getConfigString());
        assertTrue(configs.get(0).getConfigString().contains("stdin"));
    }

    @Test
    public void testPipelineConfigsFromString() {
        String config = "input { stdin {} } output { stdout {} }";
        LocalConfigSource source = new LocalConfigSource(null, config, "string-pipeline");

        List<PipelineConfigParts> configs = source.pipelineConfigs();

        assertEquals(1, configs.size());
        assertEquals("string-pipeline", configs.get(0).getPipelineId());
    }

    @Test
    public void testPipelineConfigsReturnsEmptyWhenNoConfig() {
        LocalConfigSource source = new LocalConfigSource(
                Collections.emptyList(), null, "main");

        List<PipelineConfigParts> configs = source.pipelineConfigs();
        assertTrue(configs.isEmpty());
    }

    @Test
    public void testDefaultPipelineId() {
        LocalConfigSource source = new LocalConfigSource(
                null, "input { stdin {} }", null);
        // Should default to "main"
        List<PipelineConfigParts> configs = source.pipelineConfigs();
        assertEquals(1, configs.size());
        assertEquals("main", configs.get(0).getPipelineId());
    }
}
