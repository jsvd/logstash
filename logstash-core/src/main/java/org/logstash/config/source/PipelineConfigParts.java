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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java holder for pipeline configuration parts. Mirrors the data needed by
 * {@code PipelineConfig} but without any JRuby dependencies.
 * <p>
 * Instances of this class are immutable once constructed.
 * </p>
 */
public final class PipelineConfigParts {

    private final String pipelineId;
    private final List<ConfigPart> configParts;
    private final Map<String, Object> settings;
    private volatile String configString;
    private volatile String configHash;

    private static final String NEWLINE = "\n";

    /**
     * Constructs a new PipelineConfigParts instance.
     *
     * @param pipelineId  the unique pipeline identifier
     * @param configParts the list of config parts that compose this pipeline's configuration
     * @param settings    additional settings for the pipeline (defensive copy is taken)
     */
    public PipelineConfigParts(String pipelineId, List<ConfigPart> configParts,
                               Map<String, Object> settings) {
        Objects.requireNonNull(pipelineId, "pipelineId must not be null");
        Objects.requireNonNull(configParts, "configParts must not be null");
        this.pipelineId = pipelineId;
        this.configParts = Collections.unmodifiableList(new ArrayList<>(configParts));
        this.settings = settings != null
                ? Collections.unmodifiableMap(new HashMap<>(settings))
                : Collections.emptyMap();
    }

    /**
     * Returns the pipeline ID.
     *
     * @return the pipeline identifier
     */
    public String getPipelineId() {
        return pipelineId;
    }

    /**
     * Returns an unmodifiable view of the config parts.
     *
     * @return list of config parts
     */
    public List<ConfigPart> getConfigParts() {
        return configParts;
    }

    /**
     * Returns an unmodifiable view of the settings map.
     *
     * @return settings map
     */
    public Map<String, Object> getSettings() {
        return settings;
    }

    /**
     * Returns the merged configuration string from all config parts, joined by newlines.
     *
     * @return the merged configuration text
     */
    public String getConfigString() {
        if (configString == null) {
            synchronized (this) {
                if (configString == null) {
                    final StringBuilder sb = new StringBuilder();
                    for (ConfigPart part : configParts) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                            sb.append(NEWLINE);
                        }
                        sb.append(part.getText());
                    }
                    configString = sb.toString();
                }
            }
        }
        return configString;
    }

    /**
     * Returns a SHA-256 hex digest of the merged configuration string.
     *
     * @return hex-encoded SHA-256 hash
     */
    public String getConfigHash() {
        if (configHash == null) {
            synchronized (this) {
                if (configHash == null) {
                    configHash = sha256Hex(getConfigString());
                }
            }
        }
        return configHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PipelineConfigParts)) return false;
        PipelineConfigParts that = (PipelineConfigParts) o;
        return pipelineId.equals(that.pipelineId) && getConfigHash().equals(that.getConfigHash());
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineId, getConfigHash());
    }

    @Override
    public String toString() {
        return "PipelineConfigParts{pipelineId='" + pipelineId + "', parts=" + configParts.size()
                + ", hash=" + getConfigHash() + "}";
    }

    /**
     * Computes a SHA-256 hex digest of the given input string.
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Represents a single piece of configuration text with its source metadata.
     */
    public static final class ConfigPart {
        private final String protocol;
        private final String id;
        private final String text;
        private final int line;
        private final int column;

        /**
         * Constructs a new ConfigPart.
         *
         * @param protocol the source protocol (e.g., "file", "string")
         * @param id       the source identifier (e.g., file path)
         * @param text     the configuration text
         * @param line     the starting line number (0-based)
         * @param column   the starting column number (0-based)
         */
        public ConfigPart(String protocol, String id, String text, int line, int column) {
            Objects.requireNonNull(protocol, "protocol must not be null");
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(text, "text must not be null");
            this.protocol = protocol;
            this.id = id;
            this.text = text;
            this.line = line;
            this.column = column;
        }

        /**
         * Convenience constructor with line and column defaulting to 0.
         *
         * @param protocol the source protocol
         * @param id       the source identifier
         * @param text     the configuration text
         */
        public ConfigPart(String protocol, String id, String text) {
            this(protocol, id, text, 0, 0);
        }

        public String getProtocol() {
            return protocol;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConfigPart)) return false;
            ConfigPart that = (ConfigPart) o;
            return line == that.line && column == that.column
                    && protocol.equals(that.protocol)
                    && id.equals(that.id)
                    && text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(protocol, id, text, line, column);
        }

        @Override
        public String toString() {
            return "ConfigPart{protocol='" + protocol + "', id='" + id + "', line=" + line
                    + ", column=" + column + "}";
        }
    }
}
