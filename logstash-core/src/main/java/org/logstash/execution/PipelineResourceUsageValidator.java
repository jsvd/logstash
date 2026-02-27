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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Validates that pipeline worker/batch settings don't consume too much heap memory.
 * <p>
 * Estimates heap usage based on a 2KB-per-event baseline and warns if the total
 * across all pipelines exceeds a configurable threshold (default 10%).
 * </p>
 */
public class PipelineResourceUsageValidator {

    private static final Logger LOGGER = LogManager.getLogger(PipelineResourceUsageValidator.class);
    private static final double WARN_HEAP_THRESHOLD = 10.0;

    private final long maxHeapSize;

    public PipelineResourceUsageValidator(long maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    /**
     * Checks heap usage estimates across all loaded pipelines.
     *
     * @param pipelineCount  total number of pipelines
     * @param maxEventCount  sum of (batch_size * workers) across all pipelines
     */
    public void check(int pipelineCount, long maxEventCount) {
        if (pipelineCount == 0) {
            return;
        }

        double percentageOfHeap = computePercentage(maxEventCount);

        if (percentageOfHeap >= WARN_HEAP_THRESHOLD) {
            LOGGER.warn("For a baseline of 2KB events, the maximum heap memory consumed across {} pipelines " +
                    "may reach up to {}% of the entire heap (more if the events are bigger). " +
                    "The recommended percentage is less than {}%. Consider reducing the number of pipelines, " +
                    "or the batch size and worker count per pipeline.",
                    pipelineCount, percentageOfHeap, (int) WARN_HEAP_THRESHOLD);
        } else {
            LOGGER.debug("For a baseline of 2KB events, the maximum heap memory consumed across {} pipelines " +
                    "may reach up to {}% of the entire heap (more if the events are bigger).",
                    pipelineCount, percentageOfHeap);
        }
    }

    /**
     * Computes estimated heap usage as a percentage.
     */
    public double computePercentage(long maxEventCount) {
        double estimatedHeapUsage = maxEventCount * 2.0 * 1024;
        double raw = (estimatedHeapUsage / maxHeapSize) * 100;
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
