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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for the {@link ConvergenceResult} class.
 */
public class ConvergenceResultTest {

    @Test
    public void testConstructionWithAllFields() {
        final ConvergeResult convergeResult = new ConvergeResult(2);
        final PipelineAction action1 = new CreatePipelineAction("p1", "config1");
        final PipelineAction action2 = new CreatePipelineAction("p2", "config2");
        final List<PipelineAction> actions = Arrays.asList(action1, action2);
        final Instant timestamp = Instant.now();

        final ConvergenceResult result = new ConvergenceResult(
                convergeResult, actions, timestamp, true);

        assertSame(convergeResult, result.getConvergeResult());
        assertEquals(2, result.getExecutedActions().size());
        assertSame(timestamp, result.getTimestamp());
        assertTrue(result.isConfigFetchSucceeded());
    }

    @Test
    public void testGettersWithNullConvergeResult() {
        final Instant timestamp = Instant.now();
        final ConvergenceResult result = new ConvergenceResult(
                null, Collections.emptyList(), timestamp, false);

        assertNull(result.getConvergeResult());
        assertTrue(result.getExecutedActions().isEmpty());
        assertSame(timestamp, result.getTimestamp());
        assertFalse(result.isConfigFetchSucceeded());
    }

    @Test
    public void testGettersWithNullActionsList() {
        final Instant timestamp = Instant.now();
        final ConvergenceResult result = new ConvergenceResult(
                null, null, timestamp, false);

        assertNull(result.getConvergeResult());
        assertTrue("null actions list should become empty", result.getExecutedActions().isEmpty());
    }

    @Test
    public void testIsSuccessDelegatesToConvergeResult() {
        final ConvergeResult convergeResult = new ConvergeResult(2);
        final PipelineAction action1 = new CreatePipelineAction("p1", "config1");
        final PipelineAction action2 = new CreatePipelineAction("p2", "config2");

        convergeResult.add(action1, new ConvergeResult.SuccessfulAction());
        convergeResult.add(action2, new ConvergeResult.SuccessfulAction());

        final ConvergenceResult result = new ConvergenceResult(
                convergeResult, Arrays.asList(action1, action2), Instant.now(), true);

        assertTrue("All actions succeeded so isSuccess should be true", result.isSuccess());
    }

    @Test
    public void testIsSuccessReturnsFalseWhenActionsFail() {
        final ConvergeResult convergeResult = new ConvergeResult(2);
        final PipelineAction action1 = new CreatePipelineAction("p1", "config1");
        final PipelineAction action2 = new CreatePipelineAction("p2", "config2");

        convergeResult.add(action1, new ConvergeResult.SuccessfulAction());
        convergeResult.add(action2, new ConvergeResult.FailedAction("error", null));

        final ConvergenceResult result = new ConvergenceResult(
                convergeResult, Arrays.asList(action1, action2), Instant.now(), true);

        assertFalse("Should not be successful when an action failed", result.isSuccess());
    }

    @Test
    public void testIsSuccessReturnsFalseWhenConfigFetchFailed() {
        final ConvergenceResult result = new ConvergenceResult(
                null, Collections.emptyList(), Instant.now(), false);

        assertFalse("Should not be successful when config fetch failed", result.isSuccess());
    }

    @Test
    public void testIsSuccessReturnsTrueWhenNoActionsNeeded() {
        final ConvergeResult convergeResult = new ConvergeResult(0);
        final ConvergenceResult result = new ConvergenceResult(
                convergeResult, Collections.emptyList(), Instant.now(), true);

        assertTrue("Should be successful when no actions are needed", result.isSuccess());
    }

    @Test
    public void testIsSuccessReturnsTrueWithNullConvergeResultAndConfigFetchSucceeded() {
        final ConvergenceResult result = new ConvergenceResult(
                null, Collections.emptyList(), Instant.now(), true);

        assertTrue("Null converge result with successful fetch should be success", result.isSuccess());
    }

    @Test
    public void testTimestampIsSet() {
        final Instant before = Instant.now();
        final ConvergenceResult result = new ConvergenceResult(
                null, Collections.emptyList(), Instant.now(), true);
        final Instant after = Instant.now();

        assertNotNull("Timestamp should not be null", result.getTimestamp());
        assertFalse("Timestamp should not be before test start",
                result.getTimestamp().isBefore(before));
        assertFalse("Timestamp should not be after test end",
                result.getTimestamp().isAfter(after));
    }

    @Test
    public void testConfigFetchSucceededFlag() {
        final ConvergenceResult successResult = new ConvergenceResult(
                null, Collections.emptyList(), Instant.now(), true);
        assertTrue(successResult.isConfigFetchSucceeded());

        final ConvergenceResult failResult = new ConvergenceResult(
                null, Collections.emptyList(), Instant.now(), false);
        assertFalse(failResult.isConfigFetchSucceeded());
    }

    @Test
    public void testGetFailedActions() {
        final ConvergeResult convergeResult = new ConvergeResult(3);
        final PipelineAction action1 = new CreatePipelineAction("p1", "config1");
        final PipelineAction action2 = new CreatePipelineAction("p2", "config2");
        final PipelineAction action3 = new CreatePipelineAction("p3", "config3");

        convergeResult.add(action1, new ConvergeResult.SuccessfulAction());
        convergeResult.add(action2, new ConvergeResult.FailedAction("error", null));
        convergeResult.add(action3, new ConvergeResult.FailedAction("error2", null));

        final ConvergenceResult result = new ConvergenceResult(
                convergeResult, Arrays.asList(action1, action2, action3),
                Instant.now(), true);

        final List<PipelineAction> failed = result.getFailedActions();
        assertEquals(2, failed.size());
        assertTrue(failed.contains(action2));
        assertTrue(failed.contains(action3));
    }

    @Test
    public void testGetSuccessfulActions() {
        final ConvergeResult convergeResult = new ConvergeResult(3);
        final PipelineAction action1 = new CreatePipelineAction("p1", "config1");
        final PipelineAction action2 = new CreatePipelineAction("p2", "config2");
        final PipelineAction action3 = new CreatePipelineAction("p3", "config3");

        convergeResult.add(action1, new ConvergeResult.SuccessfulAction());
        convergeResult.add(action2, new ConvergeResult.SuccessfulAction());
        convergeResult.add(action3, new ConvergeResult.FailedAction("error", null));

        final ConvergenceResult result = new ConvergenceResult(
                convergeResult, Arrays.asList(action1, action2, action3),
                Instant.now(), true);

        final List<PipelineAction> successful = result.getSuccessfulActions();
        assertEquals(2, successful.size());
        assertTrue(successful.contains(action1));
        assertTrue(successful.contains(action2));
    }

    @Test
    public void testGetFailedActionsWithNullConvergeResult() {
        final ConvergenceResult result = new ConvergenceResult(
                null, Collections.emptyList(), Instant.now(), false);

        assertTrue(result.getFailedActions().isEmpty());
    }

    @Test
    public void testGetSuccessfulActionsWithNullConvergeResult() {
        final ConvergenceResult result = new ConvergenceResult(
                null, Collections.emptyList(), Instant.now(), false);

        assertTrue(result.getSuccessfulActions().isEmpty());
    }

    @Test
    public void testExecutedActionsListIsUnmodifiable() {
        final PipelineAction action = new CreatePipelineAction("p1", "config1");
        final ConvergenceResult result = new ConvergenceResult(
                null, Collections.singletonList(action), Instant.now(), true);

        try {
            result.getExecutedActions().add(new CreatePipelineAction("p2", "config2"));
            fail("Should not be able to modify the executed actions list");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
