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

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * A {@link PipelineConfigSource} that loads pipeline configuration from local files
 * or from a provided config string.
 * <p>
 * File paths may contain glob patterns (e.g., {@code /etc/logstash/conf.d/*.conf}).
 * Temporary files (ending in {@code ~}) are skipped.
 * </p>
 */
public final class LocalConfigSource implements PipelineConfigSource {

    private final List<String> configPaths;
    private final String configString;
    private final String pipelineId;

    /**
     * Constructs a new LocalConfigSource.
     *
     * @param configPaths  list of file paths or glob patterns (may be null or empty)
     * @param configString inline config string (may be null)
     * @param pipelineId   the pipeline ID to assign to loaded configs
     */
    public LocalConfigSource(List<String> configPaths, String configString, String pipelineId) {
        this.configPaths = configPaths != null
                ? Collections.unmodifiableList(new ArrayList<>(configPaths))
                : Collections.emptyList();
        this.configString = configString;
        this.pipelineId = pipelineId != null ? pipelineId : "main";
    }

    @Override
    public List<PipelineConfigParts> pipelineConfigs() {
        List<PipelineConfigParts.ConfigPart> parts = new ArrayList<>();

        // Load from paths
        if (!configPaths.isEmpty()) {
            parts.addAll(loadFromPaths(configPaths));
        }

        // Load from string
        if (configString != null && !configString.isEmpty()) {
            parts.addAll(loadFromString(configString, pipelineId));
        }

        if (parts.isEmpty()) {
            return Collections.emptyList();
        }

        PipelineConfigParts config = new PipelineConfigParts(pipelineId, parts,
                Collections.emptyMap());
        return Collections.singletonList(config);
    }

    @Override
    public boolean matches() {
        return !configPaths.isEmpty()
                || (configString != null && !configString.isEmpty());
    }

    @Override
    public boolean hasConflict() {
        boolean hasPaths = !configPaths.isEmpty();
        boolean hasString = configString != null && !configString.isEmpty();
        return hasPaths && hasString;
    }

    @Override
    public String getConflictMessage() {
        if (hasConflict()) {
            return "Both config paths and config string are set for pipeline '"
                    + pipelineId + "'. Only one may be specified.";
        }
        return "";
    }

    /**
     * Loads config parts from file system paths, expanding glob patterns.
     * Skips temporary files (ending in ~). Files are sorted alphabetically.
     *
     * @param paths list of file paths or glob patterns
     * @return list of config parts loaded from the resolved files
     */
    public List<PipelineConfigParts.ConfigPart> loadFromPaths(List<String> paths) {
        TreeSet<Path> resolvedFiles = new TreeSet<>();

        for (String pathPattern : paths) {
            if (pathPattern == null || pathPattern.isEmpty()) {
                continue;
            }
            resolveGlob(pathPattern, resolvedFiles);
        }

        List<PipelineConfigParts.ConfigPart> parts = new ArrayList<>();
        for (Path file : resolvedFiles) {
            String fileName = file.toString();
            // Skip temp files
            if (fileName.endsWith("~")) {
                continue;
            }
            try {
                String content = readFileUtf8(file);
                parts.add(new PipelineConfigParts.ConfigPart("file", fileName, content));
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException(
                        "File " + fileName + " contains invalid UTF-8 encoding", e);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Failed to read config file: " + fileName, e);
            }
        }

        return parts;
    }

    /**
     * Loads config parts from an inline config string.
     * Adds default input/output if missing.
     *
     * @param config     the configuration string
     * @param pipelineId the pipeline ID for this config
     * @return list of config parts
     */
    public List<PipelineConfigParts.ConfigPart> loadFromString(String config, String pipelineId) {
        if (config == null || config.isEmpty()) {
            return Collections.emptyList();
        }

        List<PipelineConfigParts.ConfigPart> parts = new ArrayList<>();

        // Add default input if missing
        if (!ConfigDefaults.hasInputBlock(config)) {
            parts.add(new PipelineConfigParts.ConfigPart(
                    "string", "default-input", ConfigDefaults.defaultInput()));
        }

        // Add the main config
        parts.add(new PipelineConfigParts.ConfigPart("string", pipelineId, config));

        // Add default output if missing
        if (!ConfigDefaults.hasOutputBlock(config)) {
            parts.add(new PipelineConfigParts.ConfigPart(
                    "string", "default-output", ConfigDefaults.defaultOutput()));
        }

        return parts;
    }

    /**
     * Resolves a path that may contain glob patterns and adds matching files
     * to the provided set.
     */
    private void resolveGlob(String pathPattern, TreeSet<Path> results) {
        // Check if the pattern contains glob characters
        if (containsGlobChars(pathPattern)) {
            Path patternPath = Paths.get(pathPattern).toAbsolutePath().normalize();
            // Find the base directory (first directory without glob chars)
            Path baseDir = findGlobBase(patternPath);
            String globPattern = "glob:" + patternPath.toString();

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);

            if (Files.isDirectory(baseDir)) {
                try {
                    Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (matcher.matches(file.toAbsolutePath().normalize())) {
                                results.add(file.toAbsolutePath().normalize());
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Error reading glob pattern: " + pathPattern, e);
                }
            }
        } else {
            Path path = Paths.get(pathPattern).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                results.add(path);
            } else if (Files.isDirectory(path)) {
                // If a directory is given, read all files in it
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path entry : stream) {
                        if (Files.isRegularFile(entry)) {
                            results.add(entry.toAbsolutePath().normalize());
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Error reading directory: " + pathPattern, e);
                }
            }
        }
    }

    /**
     * Checks if a path pattern contains glob characters.
     */
    private boolean containsGlobChars(String pattern) {
        return pattern.contains("*") || pattern.contains("?")
                || pattern.contains("[") || pattern.contains("{");
    }

    /**
     * Finds the base directory for a glob pattern (i.e., the deepest directory
     * without any glob characters).
     */
    private Path findGlobBase(Path patternPath) {
        Path current = patternPath;
        while (current != null) {
            String segment = current.getFileName() != null
                    ? current.getFileName().toString() : "";
            if (!containsGlobChars(segment)) {
                if (Files.isDirectory(current)) {
                    return current;
                }
            }
            current = current.getParent();
        }
        return patternPath.getRoot() != null ? patternPath.getRoot() : Paths.get(".");
    }

    /**
     * Reads a file with strict UTF-8 validation.
     *
     * @param file the file to read
     * @return the file contents as a string
     * @throws IOException if the file cannot be read or contains invalid UTF-8
     */
    private String readFileUtf8(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }
}
