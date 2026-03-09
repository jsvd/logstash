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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import co.elastic.logstash.api.Event;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the pure Java {@link OutputStrategy} interface.
 */
public class OutputStrategyTest {

    /**
     * Stub implementation of OutputStrategy for testing the interface contract.
     */
    private static class StubOutputStrategy implements OutputStrategy {
        private final ConcurrencyType concurrencyType;
        private boolean registered = false;
        private boolean closed = false;
        private final List<Collection<Event>> receivedBatches = new ArrayList<>();

        StubOutputStrategy(final ConcurrencyType concurrencyType) {
            this.concurrencyType = concurrencyType;
        }

        @Override
        public void multiReceive(final Collection<Event> events) {
            receivedBatches.add(events);
        }

        @Override
        public void register() {
            registered = true;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public ConcurrencyType getConcurrencyType() {
            return concurrencyType;
        }

        public boolean isRegistered() {
            return registered;
        }

        public boolean isClosed() {
            return closed;
        }

        public List<Collection<Event>> getReceivedBatches() {
            return receivedBatches;
        }
    }

    @Test
    public void testConcurrencyTypeEnumValues() {
        final OutputStrategy.ConcurrencyType[] values = OutputStrategy.ConcurrencyType.values();

        assertEquals(3, values.length);
        assertNotNull(OutputStrategy.ConcurrencyType.SHARED);
        assertNotNull(OutputStrategy.ConcurrencyType.SINGLE);
        assertNotNull(OutputStrategy.ConcurrencyType.LEGACY);
    }

    @Test
    public void testConcurrencyTypeValueOf() {
        assertEquals(OutputStrategy.ConcurrencyType.SHARED,
                OutputStrategy.ConcurrencyType.valueOf("SHARED"));
        assertEquals(OutputStrategy.ConcurrencyType.SINGLE,
                OutputStrategy.ConcurrencyType.valueOf("SINGLE"));
        assertEquals(OutputStrategy.ConcurrencyType.LEGACY,
                OutputStrategy.ConcurrencyType.valueOf("LEGACY"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConcurrencyTypeValueOfInvalid() {
        OutputStrategy.ConcurrencyType.valueOf("INVALID");
    }

    @Test
    public void testConcurrencyTypeOrdinals() {
        assertEquals(0, OutputStrategy.ConcurrencyType.SHARED.ordinal());
        assertEquals(1, OutputStrategy.ConcurrencyType.SINGLE.ordinal());
        assertEquals(2, OutputStrategy.ConcurrencyType.LEGACY.ordinal());
    }

    @Test
    public void testStubSharedStrategy() throws InterruptedException {
        final StubOutputStrategy strategy =
                new StubOutputStrategy(OutputStrategy.ConcurrencyType.SHARED);

        assertEquals(OutputStrategy.ConcurrencyType.SHARED, strategy.getConcurrencyType());

        strategy.register();
        assertTrue("Strategy should be registered", strategy.isRegistered());

        strategy.multiReceive(Collections.emptyList());
        assertEquals(1, strategy.getReceivedBatches().size());

        strategy.close();
        assertTrue("Strategy should be closed", strategy.isClosed());
    }

    @Test
    public void testStubSingleStrategy() throws InterruptedException {
        final StubOutputStrategy strategy =
                new StubOutputStrategy(OutputStrategy.ConcurrencyType.SINGLE);

        assertEquals(OutputStrategy.ConcurrencyType.SINGLE, strategy.getConcurrencyType());
    }

    @Test
    public void testStubLegacyStrategy() throws InterruptedException {
        final StubOutputStrategy strategy =
                new StubOutputStrategy(OutputStrategy.ConcurrencyType.LEGACY);

        assertEquals(OutputStrategy.ConcurrencyType.LEGACY, strategy.getConcurrencyType());
    }

    @Test
    public void testMultiReceiveWithEvents() throws InterruptedException {
        final StubOutputStrategy strategy =
                new StubOutputStrategy(OutputStrategy.ConcurrencyType.SHARED);

        // Create a simple event collection
        final Collection<Event> batch = Collections.emptyList();

        strategy.register();
        strategy.multiReceive(batch);
        strategy.multiReceive(batch);
        strategy.multiReceive(batch);

        assertEquals(3, strategy.getReceivedBatches().size());

        strategy.close();
    }

    @Test
    public void testRegisterCloseLifecycle() throws InterruptedException {
        final StubOutputStrategy strategy =
                new StubOutputStrategy(OutputStrategy.ConcurrencyType.SHARED);

        assertFalse(strategy.isRegistered());
        assertFalse(strategy.isClosed());

        strategy.register();
        assertTrue(strategy.isRegistered());
        assertFalse(strategy.isClosed());

        strategy.close();
        assertTrue(strategy.isRegistered());
        assertTrue(strategy.isClosed());
    }

    @Test
    public void testAllConcurrencyTypesAreRepresented() {
        final OutputStrategy.ConcurrencyType[] types = OutputStrategy.ConcurrencyType.values();
        final List<String> typeNames = new ArrayList<>();
        for (final OutputStrategy.ConcurrencyType type : types) {
            typeNames.add(type.name());
        }

        assertTrue(typeNames.contains("SHARED"));
        assertTrue(typeNames.contains("SINGLE"));
        assertTrue(typeNames.contains("LEGACY"));
    }

    // Helper to match JUnit 4 style
    private static void assertFalse(final boolean condition) {
        org.junit.Assert.assertFalse(condition);
    }
}
