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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.logstash.common.FsUtil;
import org.logstash.exceptions.BootstrapCheckException;
import org.logstash.util.ByteValue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates persistent queue configuration: page capacity vs max bytes,
 * current queue usage, and available disk space.
 * <p>
 * Handles the full orchestration including caching of previous check results
 * to avoid redundant validation of unchanged configurations.
 * </p>
 */
public class PersistedQueueConfigValidator {

    private static final Logger LOGGER = LogManager.getLogger(PersistedQueueConfigValidator.class);

    private List<PipelineQueueConfig> lastCheckPipelineConfigs = new ArrayList<>();
    private boolean lastCheckPass = false;

    /**
     * Represents the queue-relevant configuration for a single pipeline.
     */
    public static class PipelineQueueConfig {
        public final String pipelineId;
        public final String queueType;
        public final long maxBytes;
        public final long pageCapacity;
        public final String pathQueue;

        public PipelineQueueConfig(String pipelineId, String queueType, long maxBytes, long pageCapacity, String pathQueue) {
            this.pipelineId = pipelineId;
            this.queueType = queueType;
            this.maxBytes = maxBytes;
            this.pageCapacity = pageCapacity;
            this.pathQueue = pathQueue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PipelineQueueConfig)) return false;
            PipelineQueueConfig that = (PipelineQueueConfig) o;
            return maxBytes == that.maxBytes &&
                    pageCapacity == that.pageCapacity &&
                    Objects.equals(pipelineId, that.pipelineId) &&
                    Objects.equals(queueType, that.queueType) &&
                    Objects.equals(pathQueue, that.pathQueue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pipelineId, queueType, maxBytes, pageCapacity, pathQueue);
        }
    }

    /**
     * Full orchestration: checks if config has changed, validates all persistent queue settings.
     *
     * @param runningPipelines map of pipeline_id to PipelineQueueConfig for currently running pipelines
     * @param pipelineConfigs  list of PipelineQueueConfig for new/updated pipeline configs
     * @throws BootstrapCheckException if page_capacity exceeds max_bytes
     */
    public void check(Map<String, PipelineQueueConfig> runningPipelines, List<PipelineQueueConfig> pipelineConfigs) {
        boolean hasUpdate = queueConfigsUpdate(runningPipelines, pipelineConfigs) && cacheCheckFail(pipelineConfigs);
        lastCheckPipelineConfigs = new ArrayList<>(pipelineConfigs);
        if (!hasUpdate) {
            return;
        }

        List<String> warnMsg = new ArrayList<>();
        List<String> errMsg = new ArrayList<>();
        Map<String, String> queuePathFileSystem = new HashMap<>();
        Map<String, Long> requiredFreeBytes = new HashMap<>();
        Map<String, Long> currentUsageBytes = new HashMap<>();

        for (PipelineQueueConfig config : pipelineConfigs) {
            if (!"persisted".equals(config.queueType) || config.maxBytes == 0) {
                continue;
            }

            Path queuePath = Paths.get(config.pathQueue, config.pipelineId);
            createDirs(queuePath);
            long usedBytes = getPageSize(queuePath);

            String fileSystem;
            try {
                fileSystem = getFileSystem(queuePath);
            } catch (IOException e) {
                LOGGER.debug("Could not determine filesystem for {}: {}", queuePath, e.getMessage());
                continue;
            }

            checkPageCapacity(errMsg, config.pipelineId, config.maxBytes, config.pageCapacity);
            checkQueueUsage(warnMsg, config.pipelineId, config.maxBytes, usedBytes);

            queuePathFileSystem.put(queuePath.toString(), fileSystem);
            requiredFreeBytes.merge(fileSystem, config.maxBytes, Long::sum);
            currentUsageBytes.merge(fileSystem, usedBytes, Long::sum);
        }

        checkDiskSpace(warnMsg, queuePathFileSystem, requiredFreeBytes, currentUsageBytes);

        lastCheckPass = errMsg.isEmpty() && warnMsg.isEmpty();

        if (!warnMsg.isEmpty()) {
            LOGGER.warn(String.join(" ", warnMsg));
        }
        if (!errMsg.isEmpty()) {
            throw new BootstrapCheckException(String.join(" ", errMsg));
        }
    }

    /**
     * Checks if any pipeline queue config has changed compared to running pipelines.
     */
    public boolean queueConfigsUpdate(Map<String, PipelineQueueConfig> runningPipelines, List<PipelineQueueConfig> newPipelineConfigs) {
        for (PipelineQueueConfig newConfig : newPipelineConfigs) {
            PipelineQueueConfig existing = runningPipelines.get(newConfig.pipelineId);
            if (existing == null) {
                return true;
            }
            if (!Objects.equals(existing.queueType, newConfig.queueType) ||
                    existing.maxBytes != newConfig.maxBytes ||
                    existing.pageCapacity != newConfig.pageCapacity ||
                    !Objects.equals(existing.pathQueue, newConfig.pathQueue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cache check to prevent re-checking valid configs repeatedly.
     */
    public boolean cacheCheckFail(List<PipelineQueueConfig> pipelineConfigs) {
        Map<String, PipelineQueueConfig> lastConfigMap = new HashMap<>();
        for (PipelineQueueConfig pc : lastCheckPipelineConfigs) {
            lastConfigMap.put(pc.pipelineId, pc);
        }
        return queueConfigsUpdate(lastConfigMap, pipelineConfigs) || !lastCheckPass;
    }

    /**
     * Validates that page_capacity does not exceed max_bytes.
     */
    public static void checkPageCapacity(List<String> errors, String pipelineId, long maxBytes, long pageCapacity) {
        if (pageCapacity > maxBytes) {
            errors.add("Pipeline " + pipelineId + " 'queue.page_capacity' must be less than or equal to 'queue.max_bytes'.");
        }
    }

    /**
     * Checks if current queue usage exceeds max_bytes.
     */
    public static void checkQueueUsage(List<String> warnings, String pipelineId, long maxBytes, long usedBytes) {
        if (usedBytes > maxBytes) {
            warnings.add("Pipeline " + pipelineId + " current queue size (" + usedBytes +
                    ") is greater than 'queue.max_bytes' (" + maxBytes + ").");
        }
    }

    /**
     * Calculates total page file size in bytes for files matching the page.* glob.
     */
    public static long getPageSize(Path queuePath) {
        long total = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(queuePath, "page.*")) {
            for (Path entry : stream) {
                total += Files.size(entry);
            }
        } catch (IOException e) {
            LOGGER.debug("Could not read page files in {}: {}", queuePath, e.getMessage());
        }
        return total;
    }

    /**
     * Returns the filesystem name for the given path.
     */
    public static String getFileSystem(Path queuePath) throws IOException {
        return Files.getFileStore(queuePath).name();
    }

    /**
     * Creates the queue path directories if they don't exist.
     */
    public static void createDirs(Path queuePath) {
        try {
            if (!Files.exists(queuePath)) {
                Files.createDirectories(queuePath);
            }
        } catch (IOException e) {
            LOGGER.debug("Could not create directory {}: {}", queuePath, e.getMessage());
        }
    }

    /**
     * Checks if disk space is sufficient for all queues to reach their max bytes.
     */
    public static void checkDiskSpace(List<String> warnings,
                                       Map<String, String> queuePathFileSystem,
                                       Map<String, Long> requiredFreeBytes,
                                       Map<String, Long> currentUsageBytes) {
        // Group paths by filesystem
        Map<String, List<String>> pathsByFileSystem = new HashMap<>();
        for (Map.Entry<String, String> entry : queuePathFileSystem.entrySet()) {
            pathsByFileSystem.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        List<String> fsNeedingSpace = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : pathsByFileSystem.entrySet()) {
            String fileSystem = entry.getKey();
            List<String> paths = entry.getValue();
            long additionalNeeded = requiredFreeBytes.getOrDefault(fileSystem, 0L) -
                    currentUsageBytes.getOrDefault(fileSystem, 0L);
            if (!FsUtil.hasFreeSpace(Paths.get(paths.get(0)), additionalNeeded)) {
                fsNeedingSpace.add(fileSystem);
            }
        }

        if (fsNeedingSpace.isEmpty()) {
            return;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Persistent queues require more disk space than is available on ");
        msg.append(fsNeedingSpace.size() > 1 ? "multiple filesystems" : "a filesystem");
        msg.append(":\n\n");

        for (String fileSystem : fsNeedingSpace) {
            List<String> paths = pathsByFileSystem.get(fileSystem);
            long totalRequired = requiredFreeBytes.getOrDefault(fileSystem, 0L);
            long currentUsage = currentUsageBytes.getOrDefault(fileSystem, 0L);
            long additionalNeeded = totalRequired - currentUsage;

            long freeSpace;
            try {
                freeSpace = Files.getFileStore(Paths.get(paths.get(0))).getUsableSpace();
            } catch (IOException e) {
                freeSpace = -1;
            }

            msg.append("Filesystem '").append(fileSystem).append("':\n");
            msg.append("- Total space required: ").append(ByteValue.humanReadable(totalRequired)).append("\n");
            msg.append("- Currently free space: ").append(freeSpace >= 0 ? ByteValue.humanReadable(freeSpace) : "unknown").append("\n");
            msg.append("- Current PQ usage: ").append(ByteValue.humanReadable(currentUsage)).append("\n");
            msg.append("- Additional space needed: ").append(ByteValue.humanReadable(additionalNeeded)).append("\n\n");

            msg.append("Individual queue requirements:\n");
            for (String path : paths) {
                long used = getPageSize(Paths.get(path));
                msg.append("  ").append(path).append(":\n");
                msg.append("    Current size: ").append(ByteValue.humanReadable(used)).append("\n");
                msg.append("    Maximum size: ").append(ByteValue.humanReadable(totalRequired / paths.size())).append("\n");
            }
            msg.append("\n");
        }

        msg.append("Please either:\n");
        msg.append("1. Free up disk space\n");
        msg.append("2. Reduce queue.max_bytes in your pipeline configurations\n");
        msg.append("3. Move PQ storage to a filesystem with more available space\n");
        msg.append("Note: Logstash may fail to start if this is not resolved.\n");

        warnings.add(msg.toString());
    }
}
