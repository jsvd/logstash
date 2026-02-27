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

package org.logstash.config.ir.compiler;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Backward-compatibility shim for external gems (e.g., logstash-devutils) that
 * reference OutputStrategyExt inner classes by the old class names.
 * No instances of these classes are created at runtime; this exists solely so
 * that {@code field_reader :output} in logstash-devutils does not crash at load time.
 *
 * @deprecated Use {@link OutputStrategy} instead.
 */
@Deprecated
public final class OutputStrategyExt {

    private OutputStrategyExt() {
    }

    /**
     * Compatibility shim so that {@code org.logstash.config.ir.compiler.OutputStrategyExt::SimpleAbstractOutputStrategyExt}
     * resolves in JRuby and the {@code field_reader :output} call succeeds.
     * Actual output access at runtime uses {@link OutputStrategy.SimpleAbstractOutputStrategy#getOutput()}.
     *
     * @deprecated Use {@link OutputStrategy.SimpleAbstractOutputStrategy} instead.
     */
    @Deprecated
    public abstract static class SimpleAbstractOutputStrategyExt {
        @SuppressWarnings("unused")
        protected IRubyObject output;
    }
}
