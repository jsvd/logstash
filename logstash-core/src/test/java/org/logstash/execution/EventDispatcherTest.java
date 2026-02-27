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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the pure Java {@link EventDispatcher}.
 */
public final class EventDispatcherTest {

    private static final String EMITTER = "test-emitter";

    private EventDispatcher dispatcher;

    @Before
    public void setUp() {
        dispatcher = new EventDispatcher(EMITTER);
    }

    @Test
    public void testGetEmitter() {
        assertSame(EMITTER, dispatcher.getEmitter());
    }

    @Test
    public void testAddListenerReturnsTrueOnFirstAdd() {
        final EventDispatcher.Listener listener = (name, emitter, args) -> {};
        assertTrue("First add should return true", dispatcher.addListener(listener));
    }

    @Test
    public void testAddListenerReturnsFalseOnDuplicate() {
        final EventDispatcher.Listener listener = (name, emitter, args) -> {};
        dispatcher.addListener(listener);
        assertFalse("Duplicate add should return false", dispatcher.addListener(listener));
    }

    @Test
    public void testRemoveListenerReturnsTrueOnRemoval() {
        final EventDispatcher.Listener listener = (name, emitter, args) -> {};
        dispatcher.addListener(listener);
        assertTrue("Remove of existing listener should return true", dispatcher.removeListener(listener));
    }

    @Test
    public void testRemoveListenerReturnsFalseOnMissing() {
        final EventDispatcher.Listener listener = (name, emitter, args) -> {};
        assertFalse("Remove of non-existing listener should return false", dispatcher.removeListener(listener));
    }

    @Test
    public void testFireCallsAllListeners() {
        final AtomicInteger callCount = new AtomicInteger(0);
        dispatcher.addListener((name, emitter, args) -> callCount.incrementAndGet());
        dispatcher.addListener((name, emitter, args) -> callCount.incrementAndGet());
        dispatcher.addListener((name, emitter, args) -> callCount.incrementAndGet());

        dispatcher.fire("some_event");

        assertEquals("All three listeners should be called", 3, callCount.get());
    }

    @Test
    public void testFirePassesEmitterAndArgs() {
        final List<String> receivedEventNames = new ArrayList<>();
        final List<Object> receivedEmitters = new ArrayList<>();
        final List<Object[]> receivedArgs = new ArrayList<>();

        dispatcher.addListener((name, emitter, args) -> {
            receivedEventNames.add(name);
            receivedEmitters.add(emitter);
            receivedArgs.add(args);
        });

        dispatcher.fire("pipeline_started", "arg1", 42);

        assertEquals(1, receivedEventNames.size());
        assertEquals("pipeline_started", receivedEventNames.get(0));
        assertSame(EMITTER, receivedEmitters.get(0));
        assertArrayEquals(new Object[]{"arg1", 42}, receivedArgs.get(0));
    }

    @Test
    public void testFireWithNoListenersDoesNotThrow() {
        // Should complete without any exception
        dispatcher.fire("no_listeners_event", "arg1", "arg2");
    }

    @Test
    public void testFireWithNoArgsPassesEmptyArray() {
        final List<Object[]> receivedArgs = new ArrayList<>();

        dispatcher.addListener((name, emitter, args) -> receivedArgs.add(args));

        dispatcher.fire("simple_event");

        assertEquals(1, receivedArgs.size());
        assertEquals(0, receivedArgs.get(0).length);
    }

    @Test
    public void testListenerCount() {
        assertEquals(0, dispatcher.listenerCount());

        final EventDispatcher.Listener listener1 = (name, emitter, args) -> {};
        final EventDispatcher.Listener listener2 = (name, emitter, args) -> {};

        dispatcher.addListener(listener1);
        assertEquals(1, dispatcher.listenerCount());

        dispatcher.addListener(listener2);
        assertEquals(2, dispatcher.listenerCount());

        dispatcher.removeListener(listener1);
        assertEquals(1, dispatcher.listenerCount());

        dispatcher.removeListener(listener2);
        assertEquals(0, dispatcher.listenerCount());
    }

    @Test
    public void testListenerCountAfterDuplicateAdd() {
        final EventDispatcher.Listener listener = (name, emitter, args) -> {};
        dispatcher.addListener(listener);
        dispatcher.addListener(listener); // duplicate
        assertEquals("Duplicate add should not increase count", 1, dispatcher.listenerCount());
    }

    @Test
    public void testThreadSafetyAddDuringFire() throws Exception {
        final int threadCount = 10;
        final CountDownLatch allDone = new CountDownLatch(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount + 1);
        final AtomicInteger fireCallCount = new AtomicInteger(0);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        // Add an initial listener that is slow to process, giving time for concurrent adds
        dispatcher.addListener((name, emitter, args) -> {
            fireCallCount.incrementAndGet();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        final ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        try {
            // Half the threads fire events, the other half add listeners
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                exec.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        if (index % 2 == 0) {
                            dispatcher.fire("concurrent_event");
                        } else {
                            dispatcher.addListener((name, emitter, args) -> fireCallCount.incrementAndGet());
                        }
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        allDone.countDown();
                    }
                });
            }

            // Release all threads at once
            barrier.await(5, TimeUnit.SECONDS);
            assertTrue("All threads should complete within timeout", allDone.await(10, TimeUnit.SECONDS));
            assertTrue("No errors should occur during concurrent operations: " + errors, errors.isEmpty());
            assertTrue("Fire should have been called at least once", fireCallCount.get() > 0);
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testRemovedListenerDoesNotReceiveEvents() {
        final AtomicInteger callCount = new AtomicInteger(0);
        final EventDispatcher.Listener listener = (name, emitter, args) -> callCount.incrementAndGet();

        dispatcher.addListener(listener);
        dispatcher.fire("event1");
        assertEquals(1, callCount.get());

        dispatcher.removeListener(listener);
        dispatcher.fire("event2");
        assertEquals("Removed listener should not be called again", 1, callCount.get());
    }

    @Test
    public void testMultipleEventsDispatchedCorrectly() {
        final List<String> receivedEvents = new ArrayList<>();

        dispatcher.addListener((name, emitter, args) -> receivedEvents.add(name));

        dispatcher.fire("before_bootstrap_checks");
        dispatcher.fire("after_bootstrap_checks");
        dispatcher.fire("before_agent");
        dispatcher.fire("after_agent");

        assertEquals(4, receivedEvents.size());
        assertEquals("before_bootstrap_checks", receivedEvents.get(0));
        assertEquals("after_bootstrap_checks", receivedEvents.get(1));
        assertEquals("before_agent", receivedEvents.get(2));
        assertEquals("after_agent", receivedEvents.get(3));
    }
}
