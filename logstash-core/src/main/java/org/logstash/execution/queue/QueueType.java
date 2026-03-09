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

package org.logstash.execution.queue;

import org.logstash.exceptions.ConfigurationException;

/**
 * Supported queue types in Logstash.
 */
public enum QueueType {

    MEMORY("memory"),
    PERSISTED("persisted");

    private final String label;

    QueueType(String label) {
        this.label = label;
    }

    /**
     * Returns the configuration label for this queue type.
     *
     * @return the string label (e.g. "memory" or "persisted")
     */
    public String getLabel() {
        return label;
    }

    /**
     * Resolves a {@link QueueType} from its configuration string representation.
     *
     * @param type the queue type string (case-insensitive)
     * @return the matching {@link QueueType}
     * @throws ConfigurationException if the string does not match any known queue type
     */
    public static QueueType fromString(String type) {
        for (QueueType qt : values()) {
            if (qt.label.equalsIgnoreCase(type)) {
                return qt;
            }
        }
        throw new ConfigurationException(
            String.format("Invalid setting `%s` for `queue.type`, supported types are: 'memory' or 'persisted'", type)
        );
    }
}
