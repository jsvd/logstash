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

package org.logstash.cli;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link LifecycleManager} and {@link DefaultLifecycleManager}.
 */
public final class LifecycleManagerTest {

    // -- Basic lifecycle hooks --

    @Test
    public void testLifecycleHooksDoNotThrow() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        mgr.onBeforeAgent();
        mgr.onAfterAgent();
        mgr.onBeforeShutdown();
        mgr.onAfterShutdown();
        // No exception means pass
    }

    // -- Shutdown requested --

    @Test
    public void testInitialShutdownNotRequested() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        assertFalse(mgr.isShutdownRequested());
    }

    @Test
    public void testRequestShutdownSetsFlag() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        mgr.requestShutdown();
        assertTrue(mgr.isShutdownRequested());
    }

    @Test
    public void testRequestShutdownIdempotent() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        mgr.requestShutdown();
        mgr.requestShutdown();
        assertTrue(mgr.isShutdownRequested());
    }

    // -- Shutdown hooks --

    @Test
    public void testAddAndExecuteShutdownHook() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final AtomicBoolean executed = new AtomicBoolean(false);
        mgr.addShutdownHook(() -> executed.set(true));

        assertThat(mgr.getShutdownHookCount(), is(1));
        mgr.executeShutdownHooks();
        assertTrue(executed.get());
    }

    @Test
    public void testShutdownHooksExecuteInReverseOrder() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final List<Integer> executionOrder = Collections.synchronizedList(new ArrayList<>());

        mgr.addShutdownHook(() -> executionOrder.add(1));
        mgr.addShutdownHook(() -> executionOrder.add(2));
        mgr.addShutdownHook(() -> executionOrder.add(3));

        mgr.executeShutdownHooks();

        assertThat(executionOrder.size(), is(3));
        assertThat(executionOrder.get(0), is(equalTo(3)));
        assertThat(executionOrder.get(1), is(equalTo(2)));
        assertThat(executionOrder.get(2), is(equalTo(1)));
    }

    @Test
    public void testRemoveShutdownHook() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final AtomicBoolean executed = new AtomicBoolean(false);
        final Runnable hook = () -> executed.set(true);

        mgr.addShutdownHook(hook);
        assertThat(mgr.getShutdownHookCount(), is(1));

        mgr.removeShutdownHook(hook);
        assertThat(mgr.getShutdownHookCount(), is(0));

        mgr.executeShutdownHooks();
        assertFalse(executed.get());
    }

    @Test
    public void testRemoveNonExistentHookDoesNotThrow() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        mgr.removeShutdownHook(() -> {});
        // No exception means pass
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullShutdownHookThrows() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        mgr.addShutdownHook(null);
    }

    @Test
    public void testShutdownHookExceptionDoesNotStopOthers() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final AtomicBoolean hook1Executed = new AtomicBoolean(false);
        final AtomicBoolean hook3Executed = new AtomicBoolean(false);

        mgr.addShutdownHook(() -> hook1Executed.set(true));
        mgr.addShutdownHook(() -> { throw new RuntimeException("test exception"); });
        mgr.addShutdownHook(() -> hook3Executed.set(true));

        final List<Exception> exceptions = mgr.executeShutdownHooks();

        // All hooks should execute (in reverse order), and exceptions are collected
        assertTrue(hook1Executed.get());
        assertTrue(hook3Executed.get());
        assertThat(exceptions.size(), is(1));
    }

    @Test
    public void testMultipleExceptionsCollected() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();

        mgr.addShutdownHook(() -> { throw new RuntimeException("error 1"); });
        mgr.addShutdownHook(() -> { throw new RuntimeException("error 2"); });

        final List<Exception> exceptions = mgr.executeShutdownHooks();
        assertThat(exceptions.size(), is(2));
    }

    @Test
    public void testExecuteShutdownHooksWithNoHooks() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final List<Exception> exceptions = mgr.executeShutdownHooks();
        assertTrue(exceptions.isEmpty());
    }

    // -- requestShutdown triggers hooks --

    @Test
    public void testRequestShutdownExecutesHooks() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final AtomicBoolean executed = new AtomicBoolean(false);
        mgr.addShutdownHook(() -> executed.set(true));

        mgr.requestShutdown();
        assertTrue(executed.get());
        assertTrue(mgr.isShutdownRequested());
    }

    @Test
    public void testRequestShutdownOnlyExecutesOnce() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final AtomicInteger counter = new AtomicInteger(0);
        mgr.addShutdownHook(counter::incrementAndGet);

        mgr.requestShutdown();
        mgr.requestShutdown();
        mgr.requestShutdown();

        assertThat(counter.get(), is(1));
    }

    // -- Hook count --

    @Test
    public void testGetShutdownHookCountInitial() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        assertThat(mgr.getShutdownHookCount(), is(0));
    }

    @Test
    public void testGetShutdownHookCountMultiple() {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        mgr.addShutdownHook(() -> {});
        mgr.addShutdownHook(() -> {});
        mgr.addShutdownHook(() -> {});
        assertThat(mgr.getShutdownHookCount(), is(3));
    }

    // -- Thread safety --

    @Test
    public void testConcurrentShutdownRequests() throws InterruptedException {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final AtomicInteger counter = new AtomicInteger(0);
        mgr.addShutdownHook(counter::incrementAndGet);

        final int numThreads = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    mgr.requestShutdown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue("Threads did not complete in time", doneLatch.await(5, TimeUnit.SECONDS));

        // Hook should only execute once despite concurrent shutdown requests
        assertThat(counter.get(), is(1));
        assertTrue(mgr.isShutdownRequested());
    }

    @Test
    public void testConcurrentHookAddition() throws InterruptedException {
        final DefaultLifecycleManager mgr = new DefaultLifecycleManager();
        final int numThreads = 20;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    mgr.addShutdownHook(() -> {});
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue("Threads did not complete in time", doneLatch.await(5, TimeUnit.SECONDS));

        assertThat(mgr.getShutdownHookCount(), is(numThreads));
    }

    // -- Interface contract --

    @Test
    public void testDefaultLifecycleManagerImplementsInterface() {
        final LifecycleManager mgr = new DefaultLifecycleManager();
        mgr.onBeforeAgent();
        mgr.onAfterAgent();
        mgr.onBeforeShutdown();
        mgr.onAfterShutdown();
        mgr.addShutdownHook(() -> {});
        mgr.removeShutdownHook(() -> {});
        assertFalse(mgr.isShutdownRequested());
        mgr.requestShutdown();
        assertTrue(mgr.isShutdownRequested());
    }
}
