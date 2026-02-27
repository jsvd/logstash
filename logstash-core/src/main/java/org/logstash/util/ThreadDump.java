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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Filters and iterates thread dump data, skipping idle/internal threads.
 * <p>
 * Corresponds to the Ruby {@code LogStash::Util::ThreadDump} class.
 * Takes a raw thread dump (list of maps from {@code ThreadsReport.generate()})
 * and provides filtered iteration over the top N non-idle threads.
 * </p>
 */
public class ThreadDump {

    private static final List<String> SKIPPED_THREADS = Arrays.asList(
            "Finalizer", "Reference Handler", "Signal Dispatcher"
    );
    public static final int THREADS_COUNT_DEFAULT = 10;
    public static final boolean IGNORE_IDLE_THREADS_DEFAULT = true;

    private static final Pattern JRUBY_JIT_PATTERN = Pattern.compile("Ruby-\\d+-JIT-\\d+");
    private static final Pattern POOL_THREAD_PATTERN = Pattern.compile("pool-\\d+-thread-\\d+");

    private final List<Map<String, Object>> dump;
    private final int topCount;
    private final boolean ignoreIdleThreads;

    public ThreadDump(List<Map<String, Object>> dump, int topCount, boolean ignoreIdleThreads) {
        this.dump = dump;
        this.topCount = topCount;
        this.ignoreIdleThreads = ignoreIdleThreads;
    }

    public List<Map<String, Object>> getDump() {
        return dump;
    }

    public int getTopCount() {
        return topCount;
    }

    public boolean isIgnoreIdleThreads() {
        return ignoreIdleThreads;
    }

    /**
     * Returns the filtered list of thread entries: up to {@code topCount} non-idle threads.
     */
    public List<Map<String, Object>> getFilteredThreads() {
        final List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> entry : dump) {
            if (result.size() >= topCount) {
                break;
            }
            final String threadName = (String) entry.get("thread.name");
            if (ignoreIdleThreads && isIdleThread(threadName, entry)) {
                continue;
            }
            result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Determines if a thread is idle based on its name and stack trace.
     */
    @SuppressWarnings("unchecked")
    public static boolean isIdleThread(String threadName, Map<String, Object> data) {
        if (SKIPPED_THREADS.contains(threadName)) {
            return true;
        }
        if (JRUBY_JIT_PATTERN.matcher(threadName).find()) {
            return true;
        }
        if (POOL_THREAD_PATTERN.matcher(threadName).find()) {
            return true;
        }
        Object stackTraceObj = data.get("thread.stacktrace");
        if (stackTraceObj instanceof List) {
            for (Object trace : (List<Object>) stackTraceObj) {
                if (trace instanceof String && ((String) trace).startsWith("java.util.concurrent.ThreadPoolExecutor.getTask")) {
                    return true;
                }
            }
        }
        return false;
    }
}
