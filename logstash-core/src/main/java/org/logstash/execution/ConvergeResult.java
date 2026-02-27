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

package org.logstash.execution;

import org.logstash.exceptions.LogstashException;

import org.logstash.Timestamp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Pure Java implementation of ConvergeResult, used by the agent to collect
 * the results of running pipeline actions (Create, Reload, Stop, Delete).
 */
public class ConvergeResult {

    private final int expectedActionsCount;
    private final ConcurrentHashMap<Object, ActionResult> actions = new ConcurrentHashMap<>();

    public ConvergeResult(int expectedActionsCount) {
        this.expectedActionsCount = expectedActionsCount;
    }

    public void add(Object action, Object result) {
        actions.put(action, ActionResult.fromResult(action, result));
    }

    public Map<Object, ActionResult> failedActions() {
        return actions.entrySet().stream()
                .filter(e -> !e.getValue().isSuccessful())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Object, ActionResult> successfulActions() {
        return actions.entrySet().stream()
                .filter(e -> e.getValue().isSuccessful())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isComplete() {
        return actions.size() == expectedActionsCount;
    }

    public int total() {
        return actions.size();
    }

    public boolean isSuccess() {
        return failedActions().isEmpty() && isComplete();
    }

    public int failsCount() {
        return failedActions().size();
    }

    public int successCount() {
        return successfulActions().size();
    }

    /**
     * Base class for all action results (Failed / Successful).
     */
    public static abstract class ActionResult {

        private final Timestamp executedAt;

        protected ActionResult() {
            this.executedAt = Timestamp.now();
        }

        public Timestamp getExecutedAt() {
            return executedAt;
        }

        public abstract boolean isSuccessful();

        /**
         * Factory method that converts various result types into an appropriate ActionResult.
         *
         * @param action       the action that was executed
         * @param actionResult the result of the action, can be an ActionResult, Boolean, or Exception
         * @return an ActionResult representing the outcome
         */
        public static ActionResult fromResult(Object action, Object actionResult) {
            if (actionResult instanceof ActionResult) {
                return (ActionResult) actionResult;
            } else if (Boolean.TRUE.equals(actionResult)) {
                return new SuccessfulAction();
            } else if (Boolean.FALSE.equals(actionResult)) {
                return FailedAction.fromAction(action, actionResult);
            } else if (actionResult instanceof Exception) {
                return FailedAction.fromException((Exception) actionResult);
            }
            throw new LogstashException(
                    String.format("Don't know how to handle `%s` for `%s`", actionResult, action)
            );
        }
    }

    /**
     * Successful result of running an action.
     */
    public static final class SuccessfulAction extends ActionResult {

        public SuccessfulAction() {
            super();
        }

        @Override
        public boolean isSuccessful() {
            return true;
        }
    }

    /**
     * Failed result of running an action.
     */
    public static final class FailedAction extends ActionResult {

        private final String message;
        private final String backtrace;

        public FailedAction(String message, String backtrace) {
            super();
            this.message = message;
            this.backtrace = backtrace;
        }

        public String getMessage() {
            return message;
        }

        public String getBacktrace() {
            return backtrace;
        }

        @Override
        public boolean isSuccessful() {
            return false;
        }

        /**
         * Creates a FailedAction from a Java Exception, capturing the stack trace.
         */
        public static FailedAction fromException(Exception exception) {
            final StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            return new FailedAction(exception.getMessage(), sw.toString());
        }

        /**
         * Creates a FailedAction from a failed action execution.
         */
        public static FailedAction fromAction(Object action, Object actionResult) {
            return new FailedAction(
                    String.format("Could not execute action: %s, action_result: %s", action, actionResult),
                    null
            );
        }
    }
}
