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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ThreadUtilTest {

    @SuppressWarnings("deprecation") // Thread.getId() deprecated in JDK 19
    @Test
    public void testGetThreadIdReturnsCurrentThreadId() {
        final Long id = ThreadUtil.getThreadId(Thread.currentThread());
        assertThat(id, is(equalTo(Thread.currentThread().getId())));
    }

    @Test
    public void testGetThreadNameReturnsCurrentThreadName() {
        final String name = ThreadUtil.getThreadName(Thread.currentThread());
        assertThat(name, is(equalTo(Thread.currentThread().getName())));
    }

    @Test
    public void testGetThreadIdReturnsNullForNullInput() {
        assertThat(ThreadUtil.getThreadId(null), is(nullValue()));
    }

    @Test
    public void testGetThreadNameReturnsNullForNullInput() {
        assertThat(ThreadUtil.getThreadName(null), is(nullValue()));
    }
}
