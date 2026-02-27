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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default thread-safe implementation of {@link LifecycleManager}.
 *
 * <p>Shutdown hooks are stored in a thread-safe list and executed in reverse order
 * (LIFO) when shutdown is performed, consistent with typical shutdown hook semantics.
 */
public final class DefaultLifecycleManager implements LifecycleManager {

    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();

    @Override
    public void onBeforeAgent() {
        // Extension point: subclasses or decorators can add behavior
    }

    @Override
    public void onAfterAgent() {
        // Extension point: subclasses or decorators can add behavior
    }

    @Override
    public void onBeforeShutdown() {
        // Extension point: subclasses or decorators can add behavior
    }

    @Override
    public void onAfterShutdown() {
        // Extension point: subclasses or decorators can add behavior
    }

    @Override
    public void addShutdownHook(Runnable hook) {
        if (hook == null) {
            throw new IllegalArgumentException("Shutdown hook must not be null");
        }
        shutdownHooks.add(hook);
    }

    @Override
    public void removeShutdownHook(Runnable hook) {
        shutdownHooks.remove(hook);
    }

    @Override
    public void requestShutdown() {
        if (shutdownRequested.compareAndSet(false, true)) {
            executeShutdownHooks();
        }
    }

    @Override
    public boolean isShutdownRequested() {
        return shutdownRequested.get();
    }

    /**
     * Executes all registered shutdown hooks in reverse order (LIFO).
     * Each hook is executed even if a previous hook threw an exception.
     *
     * @return a list of exceptions thrown by hooks, if any
     */
    public List<Exception> executeShutdownHooks() {
        onBeforeShutdown();

        final List<Runnable> hooks = new ArrayList<>(shutdownHooks);
        Collections.reverse(hooks);
        final List<Exception> exceptions = new ArrayList<>();

        for (Runnable hook : hooks) {
            try {
                hook.run();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        onAfterShutdown();
        return Collections.unmodifiableList(exceptions);
    }

    /**
     * Returns the number of currently registered shutdown hooks.
     *
     * @return the hook count
     */
    public int getShutdownHookCount() {
        return shutdownHooks.size();
    }
}
