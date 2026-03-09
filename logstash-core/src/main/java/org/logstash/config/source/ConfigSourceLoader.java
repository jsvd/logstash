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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages a collection of {@link PipelineConfigSource} implementations and
 * provides a unified fetch mechanism that collects pipeline configurations
 * from all matching sources.
 * <p>
 * Thread-safe via {@link ReentrantReadWriteLock}.
 * </p>
 */
public final class ConfigSourceLoader {

    private final List<PipelineConfigSource> sources;
    private final ReentrantReadWriteLock lock;

    public ConfigSourceLoader() {
        this.sources = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Adds a config source. Thread-safe.
     *
     * @param source the source to add
     * @throws IllegalArgumentException if source is null
     */
    public void addSource(PipelineConfigSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        lock.writeLock().lock();
        try {
            sources.add(source);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes all sources of the given class. Thread-safe.
     *
     * @param sourceClass the class of sources to remove
     * @return true if any sources were removed
     */
    public boolean removeSource(Class<? extends PipelineConfigSource> sourceClass) {
        if (sourceClass == null) {
            throw new IllegalArgumentException("sourceClass must not be null");
        }
        lock.writeLock().lock();
        try {
            return sources.removeIf(s -> sourceClass.isInstance(s));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a defensive copy of the current sources list. Thread-safe.
     *
     * @return list of registered config sources
     */
    public List<PipelineConfigSource> getSources() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(sources);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Fetches pipeline configurations from all matching sources.
     * <p>
     * Sources with conflicts are reported as errors. Duplicate pipeline IDs across
     * sources are also reported as errors.
     * </p>
     *
     * @return a {@link ConfigFetchResult} with collected configs or errors
     */
    public ConfigFetchResult fetch() {
        List<PipelineConfigSource> snapshot;
        lock.readLock().lock();
        try {
            snapshot = new ArrayList<>(sources);
        } finally {
            lock.readLock().unlock();
        }

        List<String> errors = new ArrayList<>();
        List<PipelineConfigParts> allConfigs = new ArrayList<>();
        Map<String, String> pipelineIdToSource = new HashMap<>();

        for (PipelineConfigSource source : snapshot) {
            if (!source.matches()) {
                continue;
            }

            if (source.hasConflict()) {
                errors.add(source.getConflictMessage());
                continue;
            }

            List<PipelineConfigParts> configs;
            try {
                configs = source.pipelineConfigs();
            } catch (Exception e) {
                errors.add("Error fetching from " + source.getClass().getSimpleName()
                        + ": " + e.getMessage());
                continue;
            }

            for (PipelineConfigParts config : configs) {
                String existing = pipelineIdToSource.get(config.getPipelineId());
                if (existing != null) {
                    errors.add("Duplicate pipeline ID '" + config.getPipelineId()
                            + "' found in sources: " + existing + " and "
                            + source.getClass().getSimpleName());
                } else {
                    pipelineIdToSource.put(config.getPipelineId(),
                            source.getClass().getSimpleName());
                    allConfigs.add(config);
                }
            }
        }

        if (!errors.isEmpty()) {
            return ConfigFetchResult.failure(errors);
        }

        return ConfigFetchResult.success(allConfigs);
    }
}
