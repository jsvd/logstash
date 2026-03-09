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
package org.logstash.api;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class LoggingApiTest {

    private LoggingApi loggingApi;
    private Level originalRootLevel;

    @Before
    public void setUp() {
        loggingApi = new LoggingApi();
        // Save the original root logger level so we can restore it after each test
        final LoggerContext ctx = LoggerContext.getContext(false);
        originalRootLevel = ctx.getConfiguration().getRootLogger().getLevel();
    }

    @After
    public void tearDown() {
        // Restore original state: remove any test loggers and restore root level
        final LoggerContext ctx = LoggerContext.getContext(false);
        final Configuration config = ctx.getConfiguration();

        // Remove test loggers
        for (final String name : config.getLoggers().keySet()) {
            if (name.startsWith("org.logstash.api.test")) {
                config.removeLogger(name);
            }
        }

        // Restore root level
        config.getRootLogger().setLevel(originalRootLevel);
        ctx.updateLoggers();
    }

    @Test
    public void testGetLoggersContainsRoot() {
        final Map<String, String> loggers = loggingApi.getLoggers();
        assertNotNull(loggers);
        assertTrue("Should contain root logger", loggers.containsKey("root"));
        assertNotNull(loggers.get("root"));
    }

    @Test
    public void testGetLoggersIsImmutable() {
        final Map<String, String> loggers = loggingApi.getLoggers();
        try {
            loggers.put("test", "DEBUG");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testSetLoggerLevelForNewLogger() {
        final String testLogger = "org.logstash.api.test.newlogger";
        final boolean changed = loggingApi.setLoggerLevel(testLogger, "DEBUG");
        assertTrue("New logger should report changed", changed);

        final Map<String, String> loggers = loggingApi.getLoggers();
        assertEquals("DEBUG", loggers.get(testLogger));
    }

    @Test
    public void testSetLoggerLevelSameLevel() {
        final String testLogger = "org.logstash.api.test.samelevel";
        loggingApi.setLoggerLevel(testLogger, "WARN");

        // Setting the same level again should return false
        final boolean changed = loggingApi.setLoggerLevel(testLogger, "WARN");
        assertFalse("Same level should not report changed", changed);
    }

    @Test
    public void testSetLoggerLevelDifferentLevel() {
        final String testLogger = "org.logstash.api.test.difflevel";
        loggingApi.setLoggerLevel(testLogger, "INFO");

        final boolean changed = loggingApi.setLoggerLevel(testLogger, "DEBUG");
        assertTrue("Different level should report changed", changed);

        final Map<String, String> loggers = loggingApi.getLoggers();
        assertEquals("DEBUG", loggers.get(testLogger));
    }

    @Test
    public void testSetRootLoggerLevel() {
        // Set root to a different level
        final boolean changed = loggingApi.setLoggerLevel(null, "TRACE");
        assertTrue(changed);

        final Map<String, String> loggers = loggingApi.getLoggers();
        assertEquals("TRACE", loggers.get("root"));
    }

    @Test
    public void testSetRootLoggerLevelWithEmptyString() {
        final boolean changed = loggingApi.setLoggerLevel("", "DEBUG");
        assertTrue(changed);

        final Map<String, String> loggers = loggingApi.getLoggers();
        assertEquals("DEBUG", loggers.get("root"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInvalidLevel() {
        loggingApi.setLoggerLevel("org.logstash.api.test.invalid", "NOT_A_LEVEL");
    }

    @Test(expected = NullPointerException.class)
    public void testSetNullLevel() {
        loggingApi.setLoggerLevel("org.logstash.api.test.nulllevel", null);
    }

    @Test
    public void testSetLoggerLevelsCaseInsensitive() {
        final String testLogger = "org.logstash.api.test.caseinsensitive";
        loggingApi.setLoggerLevel(testLogger, "debug");
        final Map<String, String> loggers = loggingApi.getLoggers();
        assertEquals("DEBUG", loggers.get(testLogger));
    }

    @Test
    public void testSetLoggerLevelsMultiple() {
        final Map<String, String> levels = new HashMap<>();
        levels.put("org.logstash.api.test.multi1", "DEBUG");
        levels.put("org.logstash.api.test.multi2", "WARN");
        levels.put("org.logstash.api.test.multi3", "ERROR");

        final Map<String, Boolean> results = loggingApi.setLoggerLevels(levels);

        assertEquals(3, results.size());
        assertTrue(results.get("org.logstash.api.test.multi1"));
        assertTrue(results.get("org.logstash.api.test.multi2"));
        assertTrue(results.get("org.logstash.api.test.multi3"));

        // Verify the levels were actually set
        final Map<String, String> loggers = loggingApi.getLoggers();
        assertEquals("DEBUG", loggers.get("org.logstash.api.test.multi1"));
        assertEquals("WARN", loggers.get("org.logstash.api.test.multi2"));
        assertEquals("ERROR", loggers.get("org.logstash.api.test.multi3"));
    }

    @Test
    public void testSetLoggerLevelsWithInvalidEntry() {
        final Map<String, String> levels = new HashMap<>();
        levels.put("org.logstash.api.test.valid", "DEBUG");
        levels.put("org.logstash.api.test.invalid", "BOGUS_LEVEL");

        final Map<String, Boolean> results = loggingApi.setLoggerLevels(levels);

        // Valid entry should succeed
        assertTrue(results.get("org.logstash.api.test.valid"));
        // Invalid entry should return false (not throw)
        assertFalse(results.get("org.logstash.api.test.invalid"));
    }

    @Test
    public void testSetLoggerLevelsIsImmutable() {
        final Map<String, String> levels = Map.of("org.logstash.api.test.imm", "DEBUG");
        final Map<String, Boolean> results = loggingApi.setLoggerLevels(levels);
        try {
            results.put("extra", true);
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test(expected = NullPointerException.class)
    public void testSetLoggerLevelsNullMap() {
        loggingApi.setLoggerLevels(null);
    }

    @Test
    public void testResetLogging() {
        // Set up some custom loggers
        loggingApi.setLoggerLevel("org.logstash.api.test.reset1", "TRACE");
        loggingApi.setLoggerLevel("org.logstash.api.test.reset2", "ERROR");

        // Verify they exist
        Map<String, String> loggers = loggingApi.getLoggers();
        assertTrue(loggers.containsKey("org.logstash.api.test.reset1"));
        assertTrue(loggers.containsKey("org.logstash.api.test.reset2"));

        // Reset all logging
        loggingApi.resetLogging();

        // After reset, custom loggers should be removed
        loggers = loggingApi.getLoggers();
        assertFalse("Custom logger should be removed after reset",
                loggers.containsKey("org.logstash.api.test.reset1"));
        assertFalse("Custom logger should be removed after reset",
                loggers.containsKey("org.logstash.api.test.reset2"));

        // Root should be reset to INFO
        assertEquals("INFO", loggers.get("root"));
    }

    // --- isValidLevel tests ---

    @Test
    public void testIsValidLevelTrue() {
        assertTrue(LoggingApi.isValidLevel("TRACE"));
        assertTrue(LoggingApi.isValidLevel("DEBUG"));
        assertTrue(LoggingApi.isValidLevel("INFO"));
        assertTrue(LoggingApi.isValidLevel("WARN"));
        assertTrue(LoggingApi.isValidLevel("ERROR"));
        assertTrue(LoggingApi.isValidLevel("FATAL"));
    }

    @Test
    public void testIsValidLevelCaseInsensitive() {
        assertTrue(LoggingApi.isValidLevel("trace"));
        assertTrue(LoggingApi.isValidLevel("Debug"));
        assertTrue(LoggingApi.isValidLevel("info"));
        assertTrue(LoggingApi.isValidLevel("Warn"));
    }

    @Test
    public void testIsValidLevelFalse() {
        assertFalse(LoggingApi.isValidLevel("VERBOSE"));
        assertFalse(LoggingApi.isValidLevel("CRITICAL"));
        assertFalse(LoggingApi.isValidLevel("NOT_A_LEVEL"));
        assertFalse(LoggingApi.isValidLevel(""));
    }

    @Test
    public void testIsValidLevelNull() {
        assertFalse(LoggingApi.isValidLevel(null));
    }

    @Test
    public void testSetLoggerLevelAllValidLevels() {
        final String[] levels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
        for (final String level : levels) {
            final String loggerName = "org.logstash.api.test.level." + level.toLowerCase();
            loggingApi.setLoggerLevel(loggerName, level);
            final Map<String, String> loggers = loggingApi.getLoggers();
            assertEquals(level, loggers.get(loggerName));
        }
    }
}
