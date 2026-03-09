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

package org.logstash.common;

import org.logstash.Event;
import org.logstash.common.io.DeadLetterQueueWriter;

import java.io.IOException;

/**
 * DLQ writer implementation that delegates to {@link DeadLetterQueueWriter}.
 */
public final class PluginDlqWriter implements DlqWriter {

    private final DeadLetterQueueWriter innerWriter;
    private final String pluginId;
    private final String pluginType;

    public PluginDlqWriter(DeadLetterQueueWriter innerWriter, String pluginId, String pluginType) {
        this.innerWriter = innerWriter;
        this.pluginId = pluginId;
        this.pluginType = pluginType;
    }

    @Override
    public boolean isOpen() {
        return innerWriter != null && innerWriter.isOpen();
    }

    @Override
    public String pluginId() {
        return pluginId;
    }

    @Override
    public String pluginType() {
        return pluginType;
    }

    @Override
    public void write(Event event, String reason) {
        if (isOpen()) {
            try {
                innerWriter.writeEntry(event, pluginType, pluginId, reason);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @Override
    public void close() {
        if (isOpen()) {
            innerWriter.close();
        }
    }
}
