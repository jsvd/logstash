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

import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.Before;
import org.junit.Test;
import org.logstash.RubyUtil;
import org.logstash.common.SourceWithMetadata;
import org.logstash.config.ir.PipelineConfig;
import org.logstash.config.ir.RubyEnvTestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests for the {@link StateResolver}.
 */
public class StateResolverTest extends RubyEnvTestCase {

    private StateResolver stateResolver;
    private RubyClass source;

    private static final RubyObject SETTINGS = (RubyObject) RubyUtil.RUBY.evalScriptlet(
            "require 'logstash/environment'\n" +
            "require 'logstash/settings'\n" +
            "LogStash::SETTINGS");

    @Before
    public void setUp() {
        stateResolver = new StateResolver();
        source = RubyUtil.RUBY.getClass("LogStash::Config::Source::Local");
    }

    private PipelineConfig createConfig(String pipelineId, String configText) {
        try {
            RubySymbol pipelineIdSym = RubyUtil.RUBY.newString(pipelineId).intern();
            SourceWithMetadata swm = new SourceWithMetadata("string", "config_string", 0, 0, configText);
            @SuppressWarnings("rawtypes")
            RubyArray configParts = RubyArray.newArray(RubyUtil.RUBY,
                    Collections.singletonList(RubyUtil.toRubyObject(swm)));
            return new PipelineConfig(source, pipelineIdSym, (RubyObject) configParts, SETTINGS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PipelineConfig for test", e);
        }
    }

    @Test
    public void testResolveWithEmptyRegistryAndOneConfig() {
        PipelineConfig config = createConfig("main", "input { stdin {} }");

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Collections.singletonList(config),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet()
        );

        assertEquals(1, actions.size());
        assertEquals(PipelineActionType.CREATE, actions.get(0).getActionType());
        assertEquals("main", actions.get(0).getPipelineId());
        assertSame(config, actions.get(0).getPipelineConfig());
    }

    @Test
    public void testResolveWithEmptyRegistryAndMultipleConfigs() {
        PipelineConfig configA = createConfig("pipeline-a", "input { stdin {} }");
        PipelineConfig configB = createConfig("pipeline-b", "input { generator {} }");

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Arrays.asList(configB, configA),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet()
        );

        assertEquals(2, actions.size());
        // Sorted by pipeline ID within same priority
        assertEquals(PipelineActionType.CREATE, actions.get(0).getActionType());
        assertEquals("pipeline-a", actions.get(0).getPipelineId());
        assertEquals(PipelineActionType.CREATE, actions.get(1).getActionType());
        assertEquals("pipeline-b", actions.get(1).getPipelineId());
    }

    @Test
    public void testResolveWithRunningPipelineNotInConfig() {
        Set<String> runningIds = new HashSet<>(Collections.singletonList("orphan-pipeline"));

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Collections.emptyList(),
                Collections.emptyMap(),
                runningIds,
                Collections.emptySet()
        );

        assertEquals(1, actions.size());
        assertEquals(PipelineActionType.STOP_AND_DELETE, actions.get(0).getActionType());
        assertEquals("orphan-pipeline", actions.get(0).getPipelineId());
    }

    @Test
    public void testResolveWithTerminatedPipelineNotInConfig() {
        Set<String> nonRunningIds = new HashSet<>(Collections.singletonList("dead-pipeline"));

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptySet(),
                nonRunningIds
        );

        assertEquals(1, actions.size());
        assertEquals(PipelineActionType.DELETE, actions.get(0).getActionType());
        assertEquals("dead-pipeline", actions.get(0).getPipelineId());
    }

    @Test
    public void testResolveWithMatchingConfigGeneratesNoAction() {
        PipelineConfig desiredConfig = createConfig("main", "input { stdin {} }");
        PipelineConfig existingConfig = createConfig("main", "input { stdin {} }");

        Map<String, PipelineConfig> existingConfigs = new HashMap<>();
        existingConfigs.put("main", existingConfig);
        Set<String> runningIds = new HashSet<>(Collections.singletonList("main"));

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Collections.singletonList(desiredConfig),
                existingConfigs,
                runningIds,
                Collections.emptySet()
        );

        assertEquals(0, actions.size());
    }

    @Test
    public void testResolveWithChangedConfigGeneratesReload() {
        PipelineConfig desiredConfig = createConfig("main", "input { generator {} }");
        PipelineConfig existingConfig = createConfig("main", "input { stdin {} }");

        Map<String, PipelineConfig> existingConfigs = new HashMap<>();
        existingConfigs.put("main", existingConfig);
        Set<String> runningIds = new HashSet<>(Collections.singletonList("main"));

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Collections.singletonList(desiredConfig),
                existingConfigs,
                runningIds,
                Collections.emptySet()
        );

        assertEquals(1, actions.size());
        assertEquals(PipelineActionType.RELOAD, actions.get(0).getActionType());
        assertEquals("main", actions.get(0).getPipelineId());
        assertSame(desiredConfig, actions.get(0).getPipelineConfig());
    }

    @Test
    public void testResolveWithEmptyRegistryAndEmptyConfig() {
        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet()
        );
        assertTrue(actions.isEmpty());
    }

    @Test
    public void testResolveActionsSortedByPriority() {
        PipelineConfig newConfig = createConfig("new-pipeline", "input { stdin {} }");
        Set<String> runningIds = new HashSet<>(Collections.singletonList("running-to-stop"));
        Set<String> nonRunningIds = new HashSet<>(Collections.singletonList("terminated-to-delete"));

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Collections.singletonList(newConfig),
                Collections.emptyMap(),
                runningIds,
                nonRunningIds
        );

        assertEquals(3, actions.size());
        // Create (100) < StopAndDelete (350) < Delete (400)
        assertEquals(PipelineActionType.CREATE, actions.get(0).getActionType());
        assertEquals("new-pipeline", actions.get(0).getPipelineId());
        assertEquals(PipelineActionType.STOP_AND_DELETE, actions.get(1).getActionType());
        assertEquals("running-to-stop", actions.get(1).getPipelineId());
        assertEquals(PipelineActionType.DELETE, actions.get(2).getActionType());
        assertEquals("terminated-to-delete", actions.get(2).getPipelineId());
    }

    @Test
    public void testResolveMultipleActionsOfSameTypeSortedByPipelineId() {
        Set<String> runningIds = new HashSet<>(Arrays.asList("z-pipeline", "a-pipeline"));

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Collections.emptyList(),
                Collections.emptyMap(),
                runningIds,
                Collections.emptySet()
        );

        assertEquals(2, actions.size());
        assertEquals(PipelineActionType.STOP_AND_DELETE, actions.get(0).getActionType());
        assertEquals("a-pipeline", actions.get(0).getPipelineId());
        assertEquals(PipelineActionType.STOP_AND_DELETE, actions.get(1).getActionType());
        assertEquals("z-pipeline", actions.get(1).getPipelineId());
    }

    @Test
    public void testResolveMixedScenario() {
        PipelineConfig existingConfig = createConfig("existing", "input { stdin {} }");
        PipelineConfig desiredExisting = createConfig("existing", "input { stdin {} }");

        PipelineConfig newA = createConfig("new-a", "input { generator {} }");
        PipelineConfig newB = createConfig("new-b", "output { stdout {} }");

        Map<String, PipelineConfig> existingConfigs = new HashMap<>();
        existingConfigs.put("existing", existingConfig);

        Set<String> runningIds = new HashSet<>(Arrays.asList("existing", "orphan"));
        Set<String> nonRunningIds = new HashSet<>(Collections.singletonList("terminated"));

        List<StateResolver.ActionDescriptor> actions = stateResolver.resolve(
                Arrays.asList(desiredExisting, newB, newA),
                existingConfigs,
                runningIds,
                nonRunningIds
        );

        // Expected: Create new-a, Create new-b, StopAndDelete orphan, Delete terminated
        assertEquals(4, actions.size());

        assertEquals(PipelineActionType.CREATE, actions.get(0).getActionType());
        assertEquals("new-a", actions.get(0).getPipelineId());
        assertEquals(PipelineActionType.CREATE, actions.get(1).getActionType());
        assertEquals("new-b", actions.get(1).getPipelineId());
        assertEquals(PipelineActionType.STOP_AND_DELETE, actions.get(2).getActionType());
        assertEquals("orphan", actions.get(2).getPipelineId());
        assertEquals(PipelineActionType.DELETE, actions.get(3).getActionType());
        assertEquals("terminated", actions.get(3).getPipelineId());
    }
}
