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

package org.logstash.exceptions;

import org.junit.Test;

import static org.junit.Assert.*;

public class LogstashExceptionTest {

    @Test
    public void testLogstashExceptionMessage() {
        final LogstashException ex = new LogstashException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void testLogstashExceptionWithCause() {
        final RuntimeException cause = new RuntimeException("root cause");
        final LogstashException ex = new LogstashException("test error", cause);
        assertEquals("test error", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testConfigurationException() {
        final ConfigurationException ex = new ConfigurationException("bad config");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("bad config", ex.getMessage());
    }

    @Test
    public void testBootstrapCheckException() {
        final BootstrapCheckException ex = new BootstrapCheckException("bootstrap failed");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("bootstrap failed", ex.getMessage());
    }

    @Test
    public void testConfigLoadingException() {
        final ConfigLoadingException ex = new ConfigLoadingException("cannot load config");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("cannot load config", ex.getMessage());
    }

    @Test
    public void testPluginLoadingException() {
        final PluginLoadingException ex = new PluginLoadingException("plugin not found");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("plugin not found", ex.getMessage());
    }

    @Test
    public void testParserException() {
        final ParserException ex = new ParserException("parse error");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("parse error", ex.getMessage());
    }

    @Test
    public void testGeneratorException() {
        final GeneratorException ex = new GeneratorException("generation error");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("generation error", ex.getMessage());
    }

    @Test
    public void testTimestampParserException() {
        final TimestampParserException ex = new TimestampParserException("bad timestamp");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("bad timestamp", ex.getMessage());
    }

    @Test
    public void testEnvironmentException() {
        final EnvironmentException ex = new EnvironmentException("env error");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("env error", ex.getMessage());
    }

    @Test
    public void testShutdownSignalException() {
        final ShutdownSignalException ex = new ShutdownSignalException("shutting down");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("shutting down", ex.getMessage());
    }

    @Test
    public void testBugException() {
        final BugException ex = new BugException("internal bug");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("internal bug", ex.getMessage());
    }

    @Test
    public void testInvalidSourceLoaderSettingException() {
        final InvalidSourceLoaderSettingException ex =
                new InvalidSourceLoaderSettingException("bad source loader");
        assertInstanceOf(LogstashException.class, ex);
        assertEquals("bad source loader", ex.getMessage());
    }

    @Test
    public void testExceptionHierarchyCatching() {
        // Verify that catching LogstashException catches all subtypes
        try {
            throw new ConfigurationException("test");
        } catch (LogstashException e) {
            assertEquals("test", e.getMessage());
        }

        // Verify that catching RuntimeException catches LogstashException
        try {
            throw new LogstashException("runtime test");
        } catch (RuntimeException e) {
            assertEquals("runtime test", e.getMessage());
        }
    }

    @Test
    public void testExceptionWithCauseChaining() {
        final RuntimeException root = new RuntimeException("root");
        final ConfigurationException mid = new ConfigurationException("mid", root);
        final LogstashException top = new LogstashException("top", mid);

        assertSame(mid, top.getCause());
        assertSame(root, top.getCause().getCause());
    }

    private static void assertInstanceOf(Class<?> expectedType, Object actual) {
        assertTrue(
                "Expected instance of " + expectedType.getName() + " but got " + actual.getClass().getName(),
                expectedType.isInstance(actual)
        );
    }
}
