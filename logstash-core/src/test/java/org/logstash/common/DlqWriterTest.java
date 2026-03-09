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

import org.junit.Test;
import org.logstash.Event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class DlqWriterTest {

    @Test
    public void testNullDlqWriterInstanceIsNotOpen() {
        assertFalse(NullDlqWriter.INSTANCE.isOpen());
    }

    @Test
    public void testNullDlqWriterWriteIsNoOp() {
        // should not throw
        NullDlqWriter.INSTANCE.write(new Event(), "some reason");
    }

    @Test
    public void testNullDlqWriterCloseIsNoOp() {
        // should not throw
        NullDlqWriter.INSTANCE.close();
    }

    @Test
    public void testNullDlqWriterPluginIdIsNull() {
        assertNull(NullDlqWriter.INSTANCE.pluginId());
    }

    @Test
    public void testNullDlqWriterPluginTypeIsNull() {
        assertNull(NullDlqWriter.INSTANCE.pluginType());
    }

    @Test
    public void testNullDlqWriterIsSingleton() {
        assertSame(NullDlqWriter.INSTANCE, NullDlqWriter.INSTANCE);
    }

    @Test
    public void testPluginDlqWriterAttributes() {
        final String pluginId = "my_plugin_id";
        final String pluginType = "output";
        PluginDlqWriter writer = new PluginDlqWriter(null, pluginId, pluginType);

        assertEquals(pluginId, writer.pluginId());
        assertEquals(pluginType, writer.pluginType());
    }

    @Test
    public void testPluginDlqWriterIsOpenReturnsFalseWhenWriterIsNull() {
        PluginDlqWriter writer = new PluginDlqWriter(null, "id", "type");
        assertFalse(writer.isOpen());
    }

    @Test
    public void testPluginDlqWriterWriteWithNullWriterIsNoOp() {
        PluginDlqWriter writer = new PluginDlqWriter(null, "id", "type");
        // should not throw - silently skips when writer is null (not open)
        writer.write(new Event(), "some reason");
    }

    @Test
    public void testPluginDlqWriterCloseWithNullWriterIsNoOp() {
        PluginDlqWriter writer = new PluginDlqWriter(null, "id", "type");
        // should not throw - silently skips when writer is null (not open)
        writer.close();
    }
}
