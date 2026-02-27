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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for pipeline action types, concrete action implementations, and ordering.
 */
public class PipelineActionTest {

    // --- PipelineActionType enum tests ---

    @Test
    public void testPipelineActionTypeValues() {
        assertEquals(5, PipelineActionType.values().length);
        assertNotNull(PipelineActionType.valueOf("CREATE"));
        assertNotNull(PipelineActionType.valueOf("RELOAD"));
        assertNotNull(PipelineActionType.valueOf("STOP"));
        assertNotNull(PipelineActionType.valueOf("STOP_AND_DELETE"));
        assertNotNull(PipelineActionType.valueOf("DELETE"));
    }

    @Test
    public void testPipelineActionTypePriorities() {
        assertEquals(100, PipelineActionType.CREATE.getPriority());
        assertEquals(200, PipelineActionType.RELOAD.getPriority());
        assertEquals(300, PipelineActionType.STOP.getPriority());
        assertEquals(350, PipelineActionType.STOP_AND_DELETE.getPriority());
        assertEquals(400, PipelineActionType.DELETE.getPriority());
    }

    @Test
    public void testPipelineActionTypePriorityOrdering() {
        assertTrue(PipelineActionType.CREATE.getPriority() < PipelineActionType.RELOAD.getPriority());
        assertTrue(PipelineActionType.RELOAD.getPriority() < PipelineActionType.STOP.getPriority());
        assertTrue(PipelineActionType.STOP.getPriority() < PipelineActionType.STOP_AND_DELETE.getPriority());
        assertTrue(PipelineActionType.STOP_AND_DELETE.getPriority() < PipelineActionType.DELETE.getPriority());
    }

    // --- CreatePipelineAction tests ---

    @Test
    public void testCreatePipelineActionConstruction() {
        final Object config = new Object();
        final CreatePipelineAction action = new CreatePipelineAction("main", config);

        assertEquals("main", action.getPipelineId());
        assertSame(config, action.getPipelineConfig());
        assertEquals(PipelineActionType.CREATE.getPriority(), action.getExecutionPriority());
    }

    @Test
    public void testCreatePipelineActionToString() {
        final CreatePipelineAction action = new CreatePipelineAction("main", null);
        assertEquals("CreateAction/pipeline_id:main", action.toString());
    }

    @Test
    public void testCreatePipelineActionExecuteReturnsSuccess() {
        final CreatePipelineAction action = new CreatePipelineAction("main", null);
        final ConvergeResult.ActionResult result = action.execute(null);
        assertTrue(result.isSuccessful());
        assertTrue(result instanceof ConvergeResult.SuccessfulAction);
    }

    // --- ReloadPipelineAction tests ---

    @Test
    public void testReloadPipelineActionConstruction() {
        final Object config = new Object();
        final ReloadPipelineAction action = new ReloadPipelineAction("main", config);

        assertEquals("main", action.getPipelineId());
        assertSame(config, action.getPipelineConfig());
        assertEquals(PipelineActionType.RELOAD.getPriority(), action.getExecutionPriority());
    }

    @Test
    public void testReloadPipelineActionToString() {
        final ReloadPipelineAction action = new ReloadPipelineAction("test-pipeline", null);
        assertEquals("ReloadAction/pipeline_id:test-pipeline", action.toString());
    }

    @Test
    public void testReloadPipelineActionExecuteReturnsSuccess() {
        final ReloadPipelineAction action = new ReloadPipelineAction("main", null);
        final ConvergeResult.ActionResult result = action.execute(null);
        assertTrue(result.isSuccessful());
    }

    // --- StopPipelineAction tests ---

    @Test
    public void testStopPipelineActionConstruction() {
        final StopPipelineAction action = new StopPipelineAction("main");

        assertEquals("main", action.getPipelineId());
        assertEquals(PipelineActionType.STOP.getPriority(), action.getExecutionPriority());
    }

    @Test
    public void testStopPipelineActionToString() {
        final StopPipelineAction action = new StopPipelineAction("my-pipeline");
        assertEquals("StopAction/pipeline_id:my-pipeline", action.toString());
    }

    @Test
    public void testStopPipelineActionExecuteReturnsSuccess() {
        final StopPipelineAction action = new StopPipelineAction("main");
        final ConvergeResult.ActionResult result = action.execute(null);
        assertTrue(result.isSuccessful());
    }

    // --- StopAndDeletePipelineAction tests ---

    @Test
    public void testStopAndDeletePipelineActionConstruction() {
        final StopAndDeletePipelineAction action = new StopAndDeletePipelineAction("main");

        assertEquals("main", action.getPipelineId());
        assertEquals(PipelineActionType.STOP_AND_DELETE.getPriority(), action.getExecutionPriority());
    }

    @Test
    public void testStopAndDeletePipelineActionToString() {
        final StopAndDeletePipelineAction action = new StopAndDeletePipelineAction("test");
        assertEquals("StopAndDeleteAction/pipeline_id:test", action.toString());
    }

    @Test
    public void testStopAndDeletePipelineActionExecuteReturnsSuccess() {
        final StopAndDeletePipelineAction action = new StopAndDeletePipelineAction("main");
        final ConvergeResult.ActionResult result = action.execute(null);
        assertTrue(result.isSuccessful());
    }

    // --- DeletePipelineAction tests ---

    @Test
    public void testDeletePipelineActionConstruction() {
        final DeletePipelineAction action = new DeletePipelineAction("main");

        assertEquals("main", action.getPipelineId());
        assertEquals(PipelineActionType.DELETE.getPriority(), action.getExecutionPriority());
    }

    @Test
    public void testDeletePipelineActionToString() {
        final DeletePipelineAction action = new DeletePipelineAction("old-pipeline");
        assertEquals("DeleteAction/pipeline_id:old-pipeline", action.toString());
    }

    @Test
    public void testDeletePipelineActionExecuteReturnsSuccess() {
        final DeletePipelineAction action = new DeletePipelineAction("main");
        final ConvergeResult.ActionResult result = action.execute(null);
        assertTrue(result.isSuccessful());
    }

    // --- Action ordering tests ---

    @Test
    public void testActionOrderingByPriority() {
        final List<PipelineAction> actions = new ArrayList<>();
        actions.add(new DeletePipelineAction("a"));
        actions.add(new CreatePipelineAction("a", null));
        actions.add(new StopAndDeletePipelineAction("a"));
        actions.add(new ReloadPipelineAction("a", null));
        actions.add(new StopPipelineAction("a"));

        Collections.sort(actions);

        assertTrue(actions.get(0) instanceof CreatePipelineAction);
        assertTrue(actions.get(1) instanceof ReloadPipelineAction);
        assertTrue(actions.get(2) instanceof StopPipelineAction);
        assertTrue(actions.get(3) instanceof StopAndDeletePipelineAction);
        assertTrue(actions.get(4) instanceof DeletePipelineAction);
    }

    @Test
    public void testActionOrderingByPipelineIdWhenSamePriority() {
        final List<PipelineAction> actions = new ArrayList<>();
        actions.add(new CreatePipelineAction("zebra", null));
        actions.add(new CreatePipelineAction("alpha", null));
        actions.add(new CreatePipelineAction("middle", null));

        Collections.sort(actions);

        assertEquals("alpha", actions.get(0).getPipelineId());
        assertEquals("middle", actions.get(1).getPipelineId());
        assertEquals("zebra", actions.get(2).getPipelineId());
    }

    @Test
    public void testActionOrderingMixedPriorityAndPipelineId() {
        final List<PipelineAction> actions = new ArrayList<>();
        actions.add(new DeletePipelineAction("b-pipeline"));
        actions.add(new CreatePipelineAction("b-pipeline", null));
        actions.add(new DeletePipelineAction("a-pipeline"));
        actions.add(new CreatePipelineAction("a-pipeline", null));

        Collections.sort(actions);

        // Creates come first (priority 100), sorted by pipeline ID
        assertEquals("a-pipeline", actions.get(0).getPipelineId());
        assertTrue(actions.get(0) instanceof CreatePipelineAction);
        assertEquals("b-pipeline", actions.get(1).getPipelineId());
        assertTrue(actions.get(1) instanceof CreatePipelineAction);

        // Deletes come last (priority 400), sorted by pipeline ID
        assertEquals("a-pipeline", actions.get(2).getPipelineId());
        assertTrue(actions.get(2) instanceof DeletePipelineAction);
        assertEquals("b-pipeline", actions.get(3).getPipelineId());
        assertTrue(actions.get(3) instanceof DeletePipelineAction);
    }

    @Test
    public void testCompareToReturnsZeroForSameTypeAndId() {
        final CreatePipelineAction a = new CreatePipelineAction("main", null);
        final CreatePipelineAction b = new CreatePipelineAction("main", null);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    public void testCompareToReturnsNegativeForLowerPriority() {
        final CreatePipelineAction create = new CreatePipelineAction("main", null);
        final DeletePipelineAction delete = new DeletePipelineAction("main");
        assertTrue(create.compareTo(delete) < 0);
    }

    @Test
    public void testCompareToReturnsPositiveForHigherPriority() {
        final DeletePipelineAction delete = new DeletePipelineAction("main");
        final CreatePipelineAction create = new CreatePipelineAction("main", null);
        assertTrue(delete.compareTo(create) > 0);
    }
}
