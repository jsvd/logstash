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

import org.jruby.RubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.logstash.RubyUtil;

/**
 * Utility class to convert between pure Java exceptions and JRuby {@link RaiseException}s.
 * This allows Java code to throw pure Java exceptions while still being compatible
 * with Ruby's {@code rescue} blocks at the boundary.
 */
public final class LogstashRubyExceptions {

    private LogstashRubyExceptions() {
        // utility class
    }

    /**
     * Converts a pure Java {@link LogstashException} to a JRuby {@link RaiseException}
     * that can be caught by Ruby's rescue blocks.
     *
     * @param context the current JRuby thread context
     * @param exception the Java exception to convert
     * @return a JRuby RaiseException wrapping the appropriate Ruby error class
     */
    public static RaiseException toRubyException(final ThreadContext context,
                                                  final LogstashException exception) {
        final RubyClass rubyClass = resolveRubyClass(exception);
        final RaiseException raiseException = context.runtime.newRaiseException(
                rubyClass, exception.getMessage()
        );
        raiseException.initCause(exception);
        return raiseException;
    }

    /**
     * Resolves the appropriate Ruby exception class for a given Java exception type.
     */
    private static RubyClass resolveRubyClass(final LogstashException exception) {
        if (exception instanceof ConfigurationException) {
            return RubyUtil.CONFIGURATION_ERROR_CLASS;
        } else if (exception instanceof ParserException) {
            return RubyUtil.PARSER_ERROR;
        } else if (exception instanceof GeneratorException) {
            return RubyUtil.GENERATOR_ERROR;
        } else if (exception instanceof TimestampParserException) {
            return RubyUtil.TIMESTAMP_PARSER_ERROR;
        } else if (exception instanceof BugException) {
            return RubyUtil.BUG_CLASS;
        } else {
            return RubyUtil.LOGSTASH_ERROR;
        }
    }
}
