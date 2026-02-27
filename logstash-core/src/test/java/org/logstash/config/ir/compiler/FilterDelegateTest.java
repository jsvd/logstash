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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FilterDelegateTest {

    /**
     * Stub implementation of FilterDelegate for testing the interface contract.
     */
    static class StubFilterDelegate implements FilterDelegate {

        private final String id;
        private final String configName;
        private final boolean flushSupported;
        private boolean registered = false;
        private boolean closed = false;

        StubFilterDelegate(String id, String configName, boolean flushSupported) {
            this.id = id;
            this.configName = configName;
            this.flushSupported = flushSupported;
        }

        StubFilterDelegate(String id, String configName) {
            this(id, configName, false);
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
        public Collection<Event> multiFilter(Collection<Event> events) {
            // Pass-through filter: returns input events unchanged
            return new ArrayList<>(events);
        }

        @Override
        public Collection<Event> flush(boolean finalFlush) {
            if (flushSupported) {
                Event flushedEvent = new Event();
                flushedEvent.setField("flushed", true);
                flushedEvent.setField("final_flush", finalFlush);
                return Collections.singletonList(flushedEvent);
            }
            return Collections.emptyList();
        }

        @Override
        public boolean hasFlush() {
            return flushSupported;
        }

        @Override
        public boolean hasPeriodicFlush() {
            return false;
        }

        @Override
        public boolean isThreadsafe() {
            return true;
        }

        @Override
        public boolean isReloadable() {
            return true;
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
        FilterDelegate delegate = new StubFilterDelegate("my-filter-id", "my_filter");
        assertEquals("my-filter-id", delegate.getPluginId());
    }

    @Test
    public void testGetPluginConfigName() {
        FilterDelegate delegate = new StubFilterDelegate("my-filter-id", "my_filter");
        assertEquals("my_filter", delegate.getPluginConfigName());
    }

    @Test
    public void testMultiFilterWithEmptyCollection() {
        FilterDelegate delegate = new StubFilterDelegate("id1", "filter1");
        Collection<Event> result = delegate.multiFilter(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMultiFilterWithEvents() {
        FilterDelegate delegate = new StubFilterDelegate("id1", "filter1");
        Event event1 = new Event();
        event1.setField("message", "hello");
        Event event2 = new Event();
        event2.setField("message", "world");

        Collection<Event> input = Arrays.asList(event1, event2);
        Collection<Event> result = delegate.multiFilter(input);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testFlushWhenNotSupported() {
        FilterDelegate delegate = new StubFilterDelegate("id1", "filter1", false);
        assertFalse(delegate.hasFlush());
        Collection<Event> result = delegate.flush(false);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlushWhenSupported() {
        FilterDelegate delegate = new StubFilterDelegate("id1", "filter1", true);
        assertTrue(delegate.hasFlush());
        Collection<Event> result = delegate.flush(true);
        assertNotNull(result);
        assertEquals(1, result.size());
        Event flushedEvent = result.iterator().next();
        assertEquals(true, flushedEvent.getField("flushed"));
        assertEquals(true, flushedEvent.getField("final_flush"));
    }

    @Test
    public void testHasPeriodicFlush() {
        FilterDelegate delegate = new StubFilterDelegate("id1", "filter1");
        assertFalse(delegate.hasPeriodicFlush());
    }

    @Test
    public void testIsThreadsafe() {
        FilterDelegate delegate = new StubFilterDelegate("id1", "filter1");
        assertTrue(delegate.isThreadsafe());
    }

    @Test
    public void testIsReloadable() {
        FilterDelegate delegate = new StubFilterDelegate("id1", "filter1");
        assertTrue(delegate.isReloadable());
    }

    @Test
    public void testRegisterAndClose() {
        StubFilterDelegate delegate = new StubFilterDelegate("id1", "filter1");
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
        // Verify that FilterDelegate can be used polymorphically
        FilterDelegate delegate = new StubFilterDelegate("poly-id", "poly_filter");
        assertNotNull(delegate.getPluginId());
        assertNotNull(delegate.getPluginConfigName());
        assertNotNull(delegate.multiFilter(Collections.emptyList()));
        assertNotNull(delegate.flush(false));
    }
}
