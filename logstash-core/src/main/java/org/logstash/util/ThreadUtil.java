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

package org.logstash.util;

/**
 * Pure Java utility methods for working with threads.
 * No JRuby dependencies - can be used from any Java code.
 */
public final class ThreadUtil {

    private ThreadUtil() {
        // utility class, not instantiable
    }

    /**
     * Returns the thread ID of the given thread, or {@code null} if the thread is null
     * (e.g., a dead thread whose native thread has been garbage collected).
     *
     * @param thread the Java thread, may be null
     * @return the thread ID, or null if thread is null
     */
    @SuppressWarnings("deprecation") // Thread.getId() deprecated in JDK 19; use threadId() when targeting JDK 21+
    public static Long getThreadId(final Thread thread) {
        return thread == null ? null : thread.getId();
    }

    /**
     * Returns the name of the given thread, or {@code null} if the thread is null
     * (e.g., a dead thread whose native thread has been garbage collected).
     *
     * @param thread the Java thread, may be null
     * @return the thread name, or null if thread is null
     */
    public static String getThreadName(final Thread thread) {
        return thread == null ? null : thread.getName();
    }
}
