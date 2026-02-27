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

import java.time.Duration;

/**
 * Pure Java data holder for dead letter queue configuration values.
 * This class has no JRuby dependencies and can be used from pure Java code.
 */
public class DlqConfiguration {

    private final boolean enabled;
    private final String dlqPath;
    private final long maxBytes;
    private final Duration flushInterval;
    private final String storageType;

    /**
     * Creates a DlqConfiguration with all fields specified.
     *
     * @param enabled       whether the dead letter queue is enabled
     * @param dlqPath       the filesystem path for the dead letter queue
     * @param maxBytes      the maximum size in bytes for the dead letter queue
     * @param flushInterval the interval between flushes
     * @param storageType   the storage policy type (e.g., "drop_newer", "drop_older")
     */
    public DlqConfiguration(final boolean enabled,
                            final String dlqPath,
                            final long maxBytes,
                            final Duration flushInterval,
                            final String storageType) {
        this.enabled = enabled;
        this.dlqPath = dlqPath;
        this.maxBytes = maxBytes;
        this.flushInterval = flushInterval;
        this.storageType = storageType;
    }

    /**
     * Creates a disabled DlqConfiguration with default values.
     *
     * @return a DlqConfiguration representing a disabled dead letter queue
     */
    public static DlqConfiguration disabled() {
        return new DlqConfiguration(false, null, 0L, Duration.ZERO, null);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDlqPath() {
        return dlqPath;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public Duration getFlushInterval() {
        return flushInterval;
    }

    public String getStorageType() {
        return storageType;
    }
}
