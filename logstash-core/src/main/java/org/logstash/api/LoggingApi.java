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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java logging management for the Logstash web API.
 * Provides operations to query and modify Log4j2 logger levels.
 * This is a pure Java class with no JRuby dependencies.
 */
public final class LoggingApi {

    private static final Object CONFIG_LOCK = new Object();

    /**
     * Valid log levels in order from most to least verbose.
     */
    private static final String[] VALID_LEVELS = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"};

    /**
     * Returns a map of all configured logger names to their current log levels.
     *
     * @return an unmodifiable map of logger name to level string
     */
    public Map<String, String> getLoggers() {
        final LoggerContext loggerContext = LoggerContext.getContext(false);
        final Configuration config = loggerContext.getConfiguration();
        final Map<String, String> loggers = new LinkedHashMap<>();

        // Add the root logger
        final LoggerConfig rootLogger = config.getRootLogger();
        loggers.put("root", rootLogger.getLevel().toString());

        // Add all configured loggers
        for (final Map.Entry<String, LoggerConfig> entry : config.getLoggers().entrySet()) {
            final String name = entry.getKey();
            if (!name.isEmpty()) {
                loggers.put(name, entry.getValue().getLevel().toString());
            }
        }

        return Collections.unmodifiableMap(loggers);
    }

    /**
     * Sets the log level for a specific logger.
     *
     * @param loggerName the name of the logger (null or empty for root logger)
     * @param level      the desired log level (e.g., "DEBUG", "INFO", "WARN")
     * @return true if the level was successfully changed, false if it was already at the requested level
     * @throws IllegalArgumentException if the level string is not a valid Log4j level
     */
    public boolean setLoggerLevel(final String loggerName, final String level) {
        Objects.requireNonNull(level, "level must not be null");
        final Level logLevel = parseLevel(level);

        synchronized (CONFIG_LOCK) {
            final LoggerContext loggerContext = LoggerContext.getContext(false);
            final Configuration config = loggerContext.getConfiguration();

            if (loggerName == null || loggerName.isEmpty()) {
                return setRootLevel(config, loggerContext, logLevel);
            } else {
                return setNamedLevel(config, loggerContext, loggerName, logLevel);
            }
        }
    }

    /**
     * Sets log levels for multiple loggers at once.
     *
     * @param loggerLevels a map of logger name to desired level
     * @return an unmodifiable map of logger name to boolean indicating whether the level was changed
     * @throws IllegalArgumentException if any level string is not a valid Log4j level
     */
    public Map<String, Boolean> setLoggerLevels(final Map<String, String> loggerLevels) {
        Objects.requireNonNull(loggerLevels, "loggerLevels must not be null");
        final Map<String, Boolean> results = new LinkedHashMap<>();

        synchronized (CONFIG_LOCK) {
            for (final Map.Entry<String, String> entry : loggerLevels.entrySet()) {
                final String loggerName = entry.getKey();
                final String level = entry.getValue();
                try {
                    final boolean changed = setLoggerLevelInternal(loggerName, level);
                    results.put(loggerName, changed);
                } catch (final IllegalArgumentException e) {
                    results.put(loggerName, false);
                }
            }
        }

        return Collections.unmodifiableMap(results);
    }

    /**
     * Resets all logger configurations to the root logger's level.
     * Removes all named logger configurations and updates the logging context.
     */
    public void resetLogging() {
        synchronized (CONFIG_LOCK) {
            final LoggerContext loggerContext = LoggerContext.getContext(false);
            final Configuration config = loggerContext.getConfiguration();
            final Level rootLevel = config.getRootLogger().getLevel();

            // Collect all non-root logger names to remove
            for (final String loggerName : config.getLoggers().keySet()) {
                if (!loggerName.isEmpty()) {
                    config.removeLogger(loggerName);
                }
            }

            // Reset root to INFO
            config.getRootLogger().setLevel(Level.INFO);
            loggerContext.updateLoggers();
        }
    }

    /**
     * Checks if a given level string is a valid Log4j log level.
     *
     * @param level the level string to check
     * @return true if the level is valid
     */
    public static boolean isValidLevel(final String level) {
        if (level == null) {
            return false;
        }
        for (final String validLevel : VALID_LEVELS) {
            if (validLevel.equalsIgnoreCase(level)) {
                return true;
            }
        }
        return false;
    }

    // --- Private helpers ---

    private boolean setLoggerLevelInternal(final String loggerName, final String level) {
        Objects.requireNonNull(level, "level must not be null");
        final Level logLevel = parseLevel(level);
        final LoggerContext loggerContext = LoggerContext.getContext(false);
        final Configuration config = loggerContext.getConfiguration();

        if (loggerName == null || loggerName.isEmpty()) {
            return setRootLevel(config, loggerContext, logLevel);
        } else {
            return setNamedLevel(config, loggerContext, loggerName, logLevel);
        }
    }

    private static boolean setRootLevel(final Configuration config, final LoggerContext loggerContext,
                                        final Level logLevel) {
        final LoggerConfig rootLogger = config.getRootLogger();
        if (rootLogger.getLevel() != logLevel) {
            rootLogger.setLevel(logLevel);
            loggerContext.updateLoggers();
            return true;
        }
        return false;
    }

    private static boolean setNamedLevel(final Configuration config, final LoggerContext loggerContext,
                                         final String loggerName, final Level logLevel) {
        final LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
        if (!loggerConfig.getName().equals(loggerName)) {
            // Logger doesn't have its own config yet, create one
            config.addLogger(loggerName, new LoggerConfig(loggerName, logLevel, true));
            loggerContext.updateLoggers();
            return true;
        } else if (loggerConfig.getLevel() != logLevel) {
            loggerConfig.setLevel(logLevel);
            loggerContext.updateLoggers();
            return true;
        }
        return false;
    }

    private static Level parseLevel(final String level) {
        try {
            return Level.valueOf(level.toUpperCase());
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid log level: " + level, e);
        }
    }
}
