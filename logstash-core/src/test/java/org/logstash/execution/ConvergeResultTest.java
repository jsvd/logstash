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

import org.junit.Test;
import org.logstash.exceptions.LogstashException;

import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for the pure Java {@link ConvergeResult}.
 */
public class ConvergeResultTest {

    @Test
    public void testAddAndTotal() {
        final ConvergeResult result = new ConvergeResult(3);
        assertEquals(0, result.total());

        result.add("action1", new ConvergeResult.SuccessfulAction());
        assertEquals(1, result.total());

        result.add("action2", new ConvergeResult.SuccessfulAction());
        assertEquals(2, result.total());

        result.add("action3", new ConvergeResult.FailedAction("fail", null));
        assertEquals(3, result.total());
    }

    @Test
    public void testIsCompleteWhenNotAllActionsExecuted() {
        final ConvergeResult result = new ConvergeResult(2);
        assertFalse(result.isComplete());

        result.add("action1", new ConvergeResult.SuccessfulAction());
        assertFalse(result.isComplete());
    }

    @Test
    public void testIsCompleteWhenAllActionsExecuted() {
        final ConvergeResult result = new ConvergeResult(2);
        result.add("action1", new ConvergeResult.SuccessfulAction());
        result.add("action2", new ConvergeResult.SuccessfulAction());
        assertTrue(result.isComplete());
    }

    @Test
    public void testSuccessfulActions() {
        final ConvergeResult result = new ConvergeResult(3);
        result.add("success1", new ConvergeResult.SuccessfulAction());
        result.add("success2", new ConvergeResult.SuccessfulAction());
        result.add("fail1", new ConvergeResult.FailedAction("error", null));

        final Map<Object, ConvergeResult.ActionResult> successful = result.successfulActions();
        assertEquals(2, successful.size());
        assertTrue(successful.containsKey("success1"));
        assertTrue(successful.containsKey("success2"));
        assertFalse(successful.containsKey("fail1"));
    }

    @Test
    public void testFailedActions() {
        final ConvergeResult result = new ConvergeResult(3);
        result.add("success1", new ConvergeResult.SuccessfulAction());
        result.add("fail1", new ConvergeResult.FailedAction("error1", null));
        result.add("fail2", new ConvergeResult.FailedAction("error2", "trace"));

        final Map<Object, ConvergeResult.ActionResult> failed = result.failedActions();
        assertEquals(2, failed.size());
        assertTrue(failed.containsKey("fail1"));
        assertTrue(failed.containsKey("fail2"));
        assertFalse(failed.containsKey("success1"));
    }

    @Test
    public void testIsSuccessWhenAllSucceed() {
        final ConvergeResult result = new ConvergeResult(2);
        result.add("action1", new ConvergeResult.SuccessfulAction());
        result.add("action2", new ConvergeResult.SuccessfulAction());
        assertTrue(result.isSuccess());
    }

    @Test
    public void testIsSuccessWhenSomeFail() {
        final ConvergeResult result = new ConvergeResult(2);
        result.add("action1", new ConvergeResult.SuccessfulAction());
        result.add("action2", new ConvergeResult.FailedAction("error", null));
        assertFalse(result.isSuccess());
    }

    @Test
    public void testIsSuccessWhenIncomplete() {
        final ConvergeResult result = new ConvergeResult(2);
        result.add("action1", new ConvergeResult.SuccessfulAction());
        assertFalse(result.isSuccess());
    }

    @Test
    public void testFailsCount() {
        final ConvergeResult result = new ConvergeResult(3);
        result.add("success1", new ConvergeResult.SuccessfulAction());
        result.add("fail1", new ConvergeResult.FailedAction("error1", null));
        result.add("fail2", new ConvergeResult.FailedAction("error2", null));
        assertEquals(2, result.failsCount());
    }

    @Test
    public void testSuccessCount() {
        final ConvergeResult result = new ConvergeResult(3);
        result.add("success1", new ConvergeResult.SuccessfulAction());
        result.add("success2", new ConvergeResult.SuccessfulAction());
        result.add("fail1", new ConvergeResult.FailedAction("error", null));
        assertEquals(2, result.successCount());
    }

    @Test
    public void testSuccessfulActionIsSuccessful() {
        final ConvergeResult.SuccessfulAction action = new ConvergeResult.SuccessfulAction();
        assertTrue(action.isSuccessful());
        assertNotNull(action.getExecutedAt());
        assertTrue(action.getExecutedAt().toInstant().isBefore(java.time.Instant.now().plusSeconds(1)));
    }

    @Test
    public void testFailedActionIsNotSuccessful() {
        final ConvergeResult.FailedAction action = new ConvergeResult.FailedAction("msg", "trace");
        assertFalse(action.isSuccessful());
        assertEquals("msg", action.getMessage());
        assertEquals("trace", action.getBacktrace());
        assertNotNull(action.getExecutedAt());
    }

    @Test
    public void testFailedActionWithNullBacktrace() {
        final ConvergeResult.FailedAction action = new ConvergeResult.FailedAction("msg", null);
        assertFalse(action.isSuccessful());
        assertEquals("msg", action.getMessage());
        assertNull(action.getBacktrace());
    }

    @Test
    public void testFailedActionFromException() {
        final Exception exception = new RuntimeException("test error");
        final ConvergeResult.FailedAction action = ConvergeResult.FailedAction.fromException(exception);
        assertFalse(action.isSuccessful());
        assertEquals("test error", action.getMessage());
        assertNotNull(action.getBacktrace());
        assertTrue(action.getBacktrace().contains("RuntimeException"));
        assertTrue(action.getBacktrace().contains("test error"));
    }

    @Test
    public void testFailedActionFromAction() {
        final ConvergeResult.FailedAction action = ConvergeResult.FailedAction.fromAction("myAction", false);
        assertFalse(action.isSuccessful());
        assertTrue(action.getMessage().contains("Could not execute action: myAction"));
        assertTrue(action.getMessage().contains("action_result: false"));
        assertNull(action.getBacktrace());
    }

    @Test
    public void testActionResultFromResultWithTrue() {
        final ConvergeResult.ActionResult result = ConvergeResult.ActionResult.fromResult("action", Boolean.TRUE);
        assertTrue(result.isSuccessful());
        assertTrue(result instanceof ConvergeResult.SuccessfulAction);
    }

    @Test
    public void testActionResultFromResultWithFalse() {
        final ConvergeResult.ActionResult result = ConvergeResult.ActionResult.fromResult("action", Boolean.FALSE);
        assertFalse(result.isSuccessful());
        assertTrue(result instanceof ConvergeResult.FailedAction);
        final ConvergeResult.FailedAction failed = (ConvergeResult.FailedAction) result;
        assertTrue(failed.getMessage().contains("Could not execute action: action"));
    }

    @Test
    public void testActionResultFromResultWithException() {
        final Exception exception = new IllegalArgumentException("bad arg");
        final ConvergeResult.ActionResult result = ConvergeResult.ActionResult.fromResult("action", exception);
        assertFalse(result.isSuccessful());
        assertTrue(result instanceof ConvergeResult.FailedAction);
        final ConvergeResult.FailedAction failed = (ConvergeResult.FailedAction) result;
        assertEquals("bad arg", failed.getMessage());
        assertNotNull(failed.getBacktrace());
    }

    @Test
    public void testActionResultFromResultWithActionResult() {
        final ConvergeResult.SuccessfulAction existing = new ConvergeResult.SuccessfulAction();
        final ConvergeResult.ActionResult result = ConvergeResult.ActionResult.fromResult("action", existing);
        assertSame(existing, result);
    }

    @Test(expected = LogstashException.class)
    public void testActionResultFromResultWithUnknownType() {
        ConvergeResult.ActionResult.fromResult("action", "unexpected string");
    }

    @Test
    public void testEmptyConvergeResult() {
        final ConvergeResult result = new ConvergeResult(0);
        assertTrue(result.isComplete());
        assertTrue(result.isSuccess());
        assertEquals(0, result.total());
        assertEquals(0, result.failsCount());
        assertEquals(0, result.successCount());
        assertTrue(result.failedActions().isEmpty());
        assertTrue(result.successfulActions().isEmpty());
    }
}
