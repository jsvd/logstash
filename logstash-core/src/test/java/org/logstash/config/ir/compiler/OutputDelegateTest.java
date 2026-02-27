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


package org.logstash.config.ir.compiler;

import org.junit.Test;
import org.logstash.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OutputDelegateTest {

    /**
     * Stub implementation of OutputDelegate for testing the interface contract.
     */
    static class StubOutputDelegate implements OutputDelegate {

        private final String id;
        private final String configName;
        private final String concurrency;
        private final List<Event> receivedEvents = new ArrayList<>();
        private boolean registered = false;
        private boolean closed = false;

        StubOutputDelegate(String id, String configName, String concurrency) {
            this.id = id;
            this.configName = configName;
            this.concurrency = concurrency;
        }

        StubOutputDelegate(String id, String configName) {
            this(id, configName, "java");
        }

        @Override
        public String getPluginId() {
            return id;
        }

        @Override
        public String getPluginConfigName() {
            return configName;
        }

        @Override
        public void multiReceiveEvents(Collection<Event> events) {
            receivedEvents.addAll(events);
        }

        @Override
        public boolean isReloadable() {
            return true;
        }

        @Override
        public String getConcurrency() {
            return concurrency;
        }

        @Override
        public void register() {
            registered = true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    public void testGetPluginId() {
        OutputDelegate delegate = new StubOutputDelegate("my-output-id", "my_output");
        assertEquals("my-output-id", delegate.getPluginId());
    }

    @Test
    public void testGetPluginConfigName() {
        OutputDelegate delegate = new StubOutputDelegate("my-output-id", "my_output");
        assertEquals("my_output", delegate.getPluginConfigName());
    }

    @Test
    public void testMultiReceiveWithEmptyCollection() {
        StubOutputDelegate delegate = new StubOutputDelegate("id1", "output1");
        delegate.multiReceiveEvents(Collections.emptyList());
        assertTrue(delegate.receivedEvents.isEmpty());
    }

    @Test
    public void testMultiReceiveWithEvents() {
        StubOutputDelegate delegate = new StubOutputDelegate("id1", "output1");
        Event event1 = new Event();
        event1.setField("message", "hello");
        Event event2 = new Event();
        event2.setField("message", "world");

        delegate.multiReceiveEvents(Arrays.asList(event1, event2));
        assertEquals(2, delegate.receivedEvents.size());
        assertEquals("hello", delegate.receivedEvents.get(0).getField("message"));
        assertEquals("world", delegate.receivedEvents.get(1).getField("message"));
    }

    @Test
    public void testMultiReceiveAccumulatesEvents() {
        StubOutputDelegate delegate = new StubOutputDelegate("id1", "output1");
        Event event1 = new Event();
        event1.setField("batch", 1);
        Event event2 = new Event();
        event2.setField("batch", 2);

        delegate.multiReceiveEvents(Collections.singletonList(event1));
        delegate.multiReceiveEvents(Collections.singletonList(event2));
        assertEquals(2, delegate.receivedEvents.size());
    }

    @Test
    public void testGetConcurrency() {
        OutputDelegate delegate = new StubOutputDelegate("id1", "output1", "java");
        assertEquals("java", delegate.getConcurrency());

        OutputDelegate shared = new StubOutputDelegate("id2", "output2", "shared");
        assertEquals("shared", shared.getConcurrency());
    }

    @Test
    public void testIsReloadable() {
        OutputDelegate delegate = new StubOutputDelegate("id1", "output1");
        assertTrue(delegate.isReloadable());
    }

    @Test
    public void testRegisterAndClose() {
        StubOutputDelegate delegate = new StubOutputDelegate("id1", "output1");
        assertFalse(delegate.registered);
        assertFalse(delegate.closed);

        delegate.register();
        assertTrue(delegate.registered);
        assertFalse(delegate.closed);

        delegate.close();
        assertTrue(delegate.registered);
        assertTrue(delegate.closed);
    }

    @Test
    public void testInterfacePolymorphism() {
        // Verify that OutputDelegate can be used polymorphically
        OutputDelegate delegate = new StubOutputDelegate("poly-id", "poly_output");
        assertNotNull(delegate.getPluginId());
        assertNotNull(delegate.getPluginConfigName());
        assertNotNull(delegate.getConcurrency());
        delegate.multiReceiveEvents(Collections.emptyList());
    }
}
