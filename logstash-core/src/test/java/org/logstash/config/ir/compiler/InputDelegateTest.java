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
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InputDelegateTest {

    /**
     * Stub implementation of InputDelegate for testing the interface contract.
     */
    static class StubInputDelegate implements InputDelegate {

        private final String id;
        private final String configName;
        private boolean started = false;
        private boolean stopped = false;
        private boolean registered = false;
        private boolean closed = false;
        private Consumer<Event> lastConsumer = null;

        StubInputDelegate(String id, String configName) {
            this.id = id;
            this.configName = configName;
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
        public void start(Consumer<Event> eventConsumer) {
            this.started = true;
            this.lastConsumer = eventConsumer;
        }

        @Override
        public void stop() {
            this.stopped = true;
        }

        @Override
        public boolean isReloadable() {
            return true;
        }

        @Override
        public boolean isThreadable() {
            return false;
        }

        @Override
        public void register() {
            this.registered = true;
        }

        @Override
        public void close() {
            this.closed = true;
        }

        /**
         * Simulates producing an event (for testing purposes).
         */
        void produceEvent(Event event) {
            if (lastConsumer != null) {
                lastConsumer.accept(event);
            }
        }
    }

    @Test
    public void testGetPluginId() {
        InputDelegate delegate = new StubInputDelegate("my-input-id", "my_input");
        assertEquals("my-input-id", delegate.getPluginId());
    }

    @Test
    public void testGetPluginConfigName() {
        InputDelegate delegate = new StubInputDelegate("my-input-id", "my_input");
        assertEquals("my_input", delegate.getPluginConfigName());
    }

    @Test
    public void testStartAndStop() {
        StubInputDelegate delegate = new StubInputDelegate("id1", "input1");
        assertFalse(delegate.started);
        assertFalse(delegate.stopped);

        List<Event> received = new ArrayList<>();
        delegate.start(received::add);
        assertTrue(delegate.started);
        assertFalse(delegate.stopped);

        delegate.stop();
        assertTrue(delegate.started);
        assertTrue(delegate.stopped);
    }

    @Test
    public void testStartDeliversEvents() {
        StubInputDelegate delegate = new StubInputDelegate("id1", "input1");
        List<Event> received = new ArrayList<>();
        delegate.start(received::add);

        Event event1 = new Event();
        event1.setField("message", "event-1");
        delegate.produceEvent(event1);

        Event event2 = new Event();
        event2.setField("message", "event-2");
        delegate.produceEvent(event2);

        assertEquals(2, received.size());
        assertEquals("event-1", received.get(0).getField("message"));
        assertEquals("event-2", received.get(1).getField("message"));
    }

    @Test
    public void testIsReloadable() {
        InputDelegate delegate = new StubInputDelegate("id1", "input1");
        assertTrue(delegate.isReloadable());
    }

    @Test
    public void testIsThreadable() {
        InputDelegate delegate = new StubInputDelegate("id1", "input1");
        assertFalse(delegate.isThreadable());
    }

    @Test
    public void testRegisterAndClose() {
        StubInputDelegate delegate = new StubInputDelegate("id1", "input1");
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
    public void testFullLifecycle() {
        StubInputDelegate delegate = new StubInputDelegate("lifecycle-id", "lifecycle_input");

        // Register
        delegate.register();
        assertTrue(delegate.registered);

        // Start
        List<Event> events = new ArrayList<>();
        delegate.start(events::add);
        assertTrue(delegate.started);

        // Produce events
        Event event = new Event();
        event.setField("message", "lifecycle-event");
        delegate.produceEvent(event);
        assertEquals(1, events.size());

        // Stop
        delegate.stop();
        assertTrue(delegate.stopped);

        // Close
        delegate.close();
        assertTrue(delegate.closed);
    }

    @Test
    public void testInterfacePolymorphism() {
        // Verify that InputDelegate can be used polymorphically
        InputDelegate delegate = new StubInputDelegate("poly-id", "poly_input");
        assertNotNull(delegate.getPluginId());
        assertNotNull(delegate.getPluginConfigName());
    }
}
