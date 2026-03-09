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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link PipelineConfigSource} that loads multiple pipeline configurations
 * from a YAML file (typically {@code pipelines.yml}).
 * <p>
 * The YAML format is a list of maps with simple key-value pairs. This class
 * implements a lightweight YAML parser for this specific flat structure, so it
 * does not depend on SnakeYAML.
 * </p>
 * Expected format:
 * <pre>
 * - pipeline.id: my-pipeline
 *   path.config: /path/to/config
 *   pipeline.workers: 4
 * - pipeline.id: another-pipeline
 *   config.string: "input { stdin {} } output { stdout {} }"
 * </pre>
 */
public final class MultiPipelineConfigSource implements PipelineConfigSource {

    private final Path yamlPath;

    /**
     * Constructs a MultiPipelineConfigSource.
     *
     * @param yamlPath the path to the pipelines.yml file
     */
    public MultiPipelineConfigSource(Path yamlPath) {
        if (yamlPath == null) {
            throw new IllegalArgumentException("yamlPath must not be null");
        }
        this.yamlPath = yamlPath;
    }

    @Override
    public List<PipelineConfigParts> pipelineConfigs() {
        return loadPipelines();
    }

    @Override
    public boolean matches() {
        return Files.isRegularFile(yamlPath);
    }

    @Override
    public boolean hasConflict() {
        return false;
    }

    @Override
    public String getConflictMessage() {
        return "";
    }

    /**
     * Parses the pipelines.yml file and returns a list of pipeline configurations.
     *
     * @return list of pipeline config parts
     * @throws IllegalArgumentException if the file cannot be read or contains
     *         duplicate pipeline IDs
     */
    public List<PipelineConfigParts> loadPipelines() {
        if (!Files.isRegularFile(yamlPath)) {
            throw new IllegalArgumentException(
                    "Pipelines YAML file not found: " + yamlPath);
        }

        List<Map<String, String>> pipelineEntries;
        try {
            pipelineEntries = parseYaml(yamlPath);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to read pipelines YAML file: " + yamlPath, e);
        }

        // Validate unique pipeline IDs
        Set<String> seenIds = new HashSet<>();
        for (Map<String, String> entry : pipelineEntries) {
            String id = entry.get("pipeline.id");
            if (id != null && !seenIds.add(id)) {
                throw new IllegalArgumentException(
                        "Duplicate pipeline ID '" + id + "' in " + yamlPath);
            }
        }

        List<PipelineConfigParts> result = new ArrayList<>();
        for (Map<String, String> entry : pipelineEntries) {
            String pipelineId = entry.get("pipeline.id");
            if (pipelineId == null || pipelineId.isEmpty()) {
                throw new IllegalArgumentException(
                        "Pipeline entry missing 'pipeline.id' in " + yamlPath);
            }

            String pathConfig = entry.get("path.config");
            String configString = entry.get("config.string");

            // Build settings map
            Map<String, Object> settings = new HashMap<>(entry);

            // Build config parts
            List<PipelineConfigParts.ConfigPart> configParts = new ArrayList<>();

            if (pathConfig != null && !pathConfig.isEmpty()) {
                // Delegate to LocalConfigSource for file loading
                LocalConfigSource localSource = new LocalConfigSource(
                        Collections.singletonList(pathConfig), null, pipelineId);
                List<PipelineConfigParts.ConfigPart> fileParts =
                        localSource.loadFromPaths(Collections.singletonList(pathConfig));
                configParts.addAll(fileParts);
            } else if (configString != null && !configString.isEmpty()) {
                LocalConfigSource localSource = new LocalConfigSource(
                        null, configString, pipelineId);
                List<PipelineConfigParts.ConfigPart> stringParts =
                        localSource.loadFromString(configString, pipelineId);
                configParts.addAll(stringParts);
            }

            if (!configParts.isEmpty()) {
                result.add(new PipelineConfigParts(pipelineId, configParts, settings));
            }
        }

        return result;
    }

    /**
     * Simple YAML parser for the flat list-of-maps structure used in pipelines.yml.
     * <p>
     * Handles:
     * <ul>
     *   <li>List items starting with "- " at the top level</li>
     *   <li>Key-value pairs with "key: value" syntax</li>
     *   <li>Quoted string values (single and double quotes)</li>
     *   <li>Comments starting with #</li>
     *   <li>Empty lines</li>
     * </ul>
     * </p>
     *
     * @param path the YAML file path
     * @return list of maps, one per pipeline entry
     * @throws IOException if the file cannot be read
     */
    List<Map<String, String>> parseYaml(Path path) throws IOException {
        List<Map<String, String>> entries = new ArrayList<>();
        Map<String, String> currentEntry = null;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Strip trailing comment (but not within quoted strings - simplified)
                String stripped = stripComment(line);
                String trimmed = stripped.trim();

                // Skip empty lines
                if (trimmed.isEmpty()) {
                    continue;
                }

                // New list item
                if (trimmed.startsWith("- ")) {
                    currentEntry = new LinkedHashMap<>();
                    entries.add(currentEntry);
                    // Parse the key-value after "- "
                    String kvPart = trimmed.substring(2).trim();
                    if (!kvPart.isEmpty()) {
                        parseKeyValue(kvPart, currentEntry);
                    }
                } else if (currentEntry != null && trimmed.contains(":")) {
                    // Continuation key-value for current entry
                    parseKeyValue(trimmed, currentEntry);
                }
            }
        }

        return entries;
    }

    /**
     * Parses a "key: value" string and adds it to the map.
     */
    private void parseKeyValue(String line, Map<String, String> map) {
        int colonIdx = line.indexOf(':');
        if (colonIdx <= 0) {
            return;
        }
        String key = line.substring(0, colonIdx).trim();
        String value = "";
        if (colonIdx + 1 < line.length()) {
            value = line.substring(colonIdx + 1).trim();
        }
        // Remove surrounding quotes
        value = unquote(value);
        map.put(key, value);
    }

    /**
     * Removes surrounding single or double quotes from a value.
     */
    private String unquote(String value) {
        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * Strips inline comments (# and everything after) from a line,
     * unless the # is inside a quoted string.
     */
    private String stripComment(String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '#' && !inSingleQuote && !inDoubleQuote) {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
