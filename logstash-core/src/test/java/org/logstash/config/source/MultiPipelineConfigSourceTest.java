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
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MultiPipelineConfigSourceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testMatchesWhenFileExists() throws IOException {
        File yamlFile = tempFolder.newFile("pipelines.yml");
        Files.write(yamlFile.toPath(),
                "- pipeline.id: test\n  config.string: \"input { stdin {} }\"\n"
                        .getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        assertTrue(source.matches());
    }

    @Test
    public void testDoesNotMatchWhenFileMissing() {
        Path missing = Paths.get(tempFolder.getRoot().getAbsolutePath(), "nonexistent.yml");
        MultiPipelineConfigSource source = new MultiPipelineConfigSource(missing);
        assertFalse(source.matches());
    }

    @Test
    public void testNoConflict() throws IOException {
        File yamlFile = tempFolder.newFile("pipelines.yml");
        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        assertFalse(source.hasConflict());
        assertEquals("", source.getConflictMessage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPathThrows() {
        new MultiPipelineConfigSource(null);
    }

    // --- YAML parsing tests ---

    @Test
    public void testParseSinglePipeline() throws IOException {
        String yaml = "- pipeline.id: my-pipeline\n"
                + "  config.string: \"input { stdin {} } output { stdout {} }\"\n"
                + "  pipeline.workers: 4\n";

        File yamlFile = tempFolder.newFile("single.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<Map<String, String>> entries = source.parseYaml(yamlFile.toPath());

        assertEquals(1, entries.size());
        assertEquals("my-pipeline", entries.get(0).get("pipeline.id"));
        assertEquals("input { stdin {} } output { stdout {} }",
                entries.get(0).get("config.string"));
        assertEquals("4", entries.get(0).get("pipeline.workers"));
    }

    @Test
    public void testParseMultiplePipelines() throws IOException {
        String yaml = "- pipeline.id: pipeline-a\n"
                + "  config.string: \"input { stdin {} } output { stdout {} }\"\n"
                + "\n"
                + "- pipeline.id: pipeline-b\n"
                + "  config.string: \"input { generator {} } output { null {} }\"\n";

        File yamlFile = tempFolder.newFile("multi.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<Map<String, String>> entries = source.parseYaml(yamlFile.toPath());

        assertEquals(2, entries.size());
        assertEquals("pipeline-a", entries.get(0).get("pipeline.id"));
        assertEquals("pipeline-b", entries.get(1).get("pipeline.id"));
    }

    @Test
    public void testParseWithComments() throws IOException {
        String yaml = "# This is a comment\n"
                + "- pipeline.id: my-pipeline # inline comment\n"
                + "  config.string: \"input { stdin {} } output { stdout {} }\"\n"
                + "  # Another comment\n"
                + "  pipeline.workers: 2\n";

        File yamlFile = tempFolder.newFile("comments.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<Map<String, String>> entries = source.parseYaml(yamlFile.toPath());

        assertEquals(1, entries.size());
        assertEquals("my-pipeline", entries.get(0).get("pipeline.id"));
    }

    @Test
    public void testParseWithSingleQuotedValues() throws IOException {
        String yaml = "- pipeline.id: 'quoted-id'\n"
                + "  config.string: 'input { stdin {} }'\n";

        File yamlFile = tempFolder.newFile("quoted.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<Map<String, String>> entries = source.parseYaml(yamlFile.toPath());

        assertEquals(1, entries.size());
        assertEquals("quoted-id", entries.get(0).get("pipeline.id"));
        assertEquals("input { stdin {} }", entries.get(0).get("config.string"));
    }

    @Test
    public void testParseEmptyFile() throws IOException {
        File yamlFile = tempFolder.newFile("empty.yml");
        Files.write(yamlFile.toPath(), "".getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<Map<String, String>> entries = source.parseYaml(yamlFile.toPath());

        assertTrue(entries.isEmpty());
    }

    // --- loadPipelines() tests ---

    @Test
    public void testLoadPipelinesWithConfigString() throws IOException {
        String yaml = "- pipeline.id: string-pipeline\n"
                + "  config.string: \"input { stdin {} } output { stdout {} }\"\n";

        File yamlFile = tempFolder.newFile("string-pipeline.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<PipelineConfigParts> pipelines = source.loadPipelines();

        assertEquals(1, pipelines.size());
        assertEquals("string-pipeline", pipelines.get(0).getPipelineId());
        assertNotNull(pipelines.get(0).getConfigString());
        assertTrue(pipelines.get(0).getConfigString().contains("stdin"));
    }

    @Test
    public void testLoadPipelinesWithPathConfig() throws IOException {
        // Create a config file
        File confDir = tempFolder.newFolder("configs");
        File configFile = new File(confDir, "pipeline.conf");
        Files.write(configFile.toPath(),
                "input { stdin {} } output { stdout {} }".getBytes(StandardCharsets.UTF_8));

        String yaml = "- pipeline.id: file-pipeline\n"
                + "  path.config: " + configFile.getAbsolutePath() + "\n";

        File yamlFile = tempFolder.newFile("file-pipeline.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<PipelineConfigParts> pipelines = source.loadPipelines();

        assertEquals(1, pipelines.size());
        assertEquals("file-pipeline", pipelines.get(0).getPipelineId());
    }

    @Test
    public void testLoadPipelinesPreservesSettings() throws IOException {
        String yaml = "- pipeline.id: settings-test\n"
                + "  config.string: \"input { stdin {} } output { stdout {} }\"\n"
                + "  pipeline.workers: 8\n"
                + "  pipeline.batch.size: 500\n";

        File yamlFile = tempFolder.newFile("settings.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<PipelineConfigParts> pipelines = source.loadPipelines();

        assertEquals(1, pipelines.size());
        Map<String, Object> settings = pipelines.get(0).getSettings();
        assertEquals("8", settings.get("pipeline.workers"));
        assertEquals("500", settings.get("pipeline.batch.size"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicatePipelineIdsThrows() throws IOException {
        String yaml = "- pipeline.id: duplicate\n"
                + "  config.string: \"input { stdin {} } output { stdout {} }\"\n"
                + "- pipeline.id: duplicate\n"
                + "  config.string: \"input { generator {} } output { null {} }\"\n";

        File yamlFile = tempFolder.newFile("duplicate.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        source.loadPipelines();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingPipelineIdThrows() throws IOException {
        String yaml = "- config.string: \"input { stdin {} } output { stdout {} }\"\n";

        File yamlFile = tempFolder.newFile("no-id.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        source.loadPipelines();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingFileThrows() {
        Path missing = Paths.get(tempFolder.getRoot().getAbsolutePath(), "not-there.yml");
        MultiPipelineConfigSource source = new MultiPipelineConfigSource(missing);
        source.loadPipelines();
    }

    @Test
    public void testLoadMultiplePipelines() throws IOException {
        String yaml = "- pipeline.id: alpha\n"
                + "  config.string: \"input { stdin {} } output { stdout {} }\"\n"
                + "- pipeline.id: beta\n"
                + "  config.string: \"input { generator {} } output { null {} }\"\n";

        File yamlFile = tempFolder.newFile("multiple.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<PipelineConfigParts> pipelines = source.loadPipelines();

        assertEquals(2, pipelines.size());
        assertEquals("alpha", pipelines.get(0).getPipelineId());
        assertEquals("beta", pipelines.get(1).getPipelineId());
    }

    @Test
    public void testPipelineConfigsCallsLoadPipelines() throws IOException {
        String yaml = "- pipeline.id: delegate-test\n"
                + "  config.string: \"input { stdin {} } output { stdout {} }\"\n";

        File yamlFile = tempFolder.newFile("delegate.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));

        MultiPipelineConfigSource source = new MultiPipelineConfigSource(yamlFile.toPath());
        List<PipelineConfigParts> configs = source.pipelineConfigs();

        assertEquals(1, configs.size());
        assertEquals("delegate-test", configs.get(0).getPipelineId());
    }
}
