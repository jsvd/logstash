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

package org.logstash.cli;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link CliOptionRegistry}.
 */
public final class CliOptionRegistryTest {

    @Test
    public void testGetOptionsReturnsNonEmptyList() {
        final List<CliOption> options = CliOptionRegistry.getOptions();
        assertThat(options, is(notNullValue()));
        assertTrue("Expected at least 30 options, got " + options.size(), options.size() >= 30);
    }

    @Test
    public void testGetOptionsIsUnmodifiable() {
        final List<CliOption> options = CliOptionRegistry.getOptions();
        try {
            options.add(CliOption.builder("--bogus").build());
            assertThat("Should have thrown UnsupportedOperationException", false, is(true));
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    // -- findByFlag: long flag tests --

    @Test
    public void testFindByLongFlagNodeName() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--node.name");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("node.name")));
    }

    @Test
    public void testFindByLongFlagPathConfig() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--path.config");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("path.config")));
    }

    @Test
    public void testFindByLongFlagConfigString() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--config.string");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("config.string")));
    }

    @Test
    public void testFindByLongFlagPipelineId() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--pipeline.id");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("pipeline.id")));
    }

    @Test
    public void testFindByLongFlagPipelineWorkers() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--pipeline.workers");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("pipeline.workers")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.INTEGER)));
    }

    @Test
    public void testFindByLongFlagPipelineOrdered() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--pipeline.ordered");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("pipeline.ordered")));
    }

    @Test
    public void testFindByLongFlagPipelineBatchSize() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--pipeline.batch.size");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("pipeline.batch.size")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.INTEGER)));
    }

    @Test
    public void testFindByLongFlagPipelineBatchDelay() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--pipeline.batch.delay");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("pipeline.batch.delay")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.INTEGER)));
    }

    @Test
    public void testFindByLongFlagPipelineUnsafeShutdown() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--pipeline.unsafe_shutdown");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("pipeline.unsafe_shutdown")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.BOOLEAN)));
    }

    @Test
    public void testFindByLongFlagPathData() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--path.data");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("path.data")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.PATH)));
    }

    @Test
    public void testFindByLongFlagPathPlugins() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--path.plugins");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("path.plugins")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.STRING_LIST)));
    }

    @Test
    public void testFindByLongFlagPathLogs() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--path.logs");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("path.logs")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.PATH)));
    }

    @Test
    public void testFindByLongFlagLogLevel() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--log.level");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("log.level")));
    }

    @Test
    public void testFindByLongFlagLogFormat() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--log.format");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("log.format")));
    }

    @Test
    public void testFindByLongFlagConfigDebug() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--config.debug");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("config.debug")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.BOOLEAN)));
    }

    @Test
    public void testFindByLongFlagConfigTestAndExit() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--config.test_and_exit");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("config.test_and_exit")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.BOOLEAN)));
    }

    @Test
    public void testFindByLongFlagConfigReloadAutomatic() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--config.reload.automatic");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("config.reload.automatic")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.BOOLEAN)));
    }

    @Test
    public void testFindByLongFlagConfigReloadInterval() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--config.reload.interval");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("config.reload.interval")));
    }

    @Test
    public void testFindByLongFlagApiEnabled() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--api.enabled");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("api.enabled")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.BOOLEAN)));
    }

    @Test
    public void testFindByLongFlagApiHttpHost() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--api.http.host");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("api.http.host")));
    }

    @Test
    public void testFindByLongFlagApiHttpPort() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--api.http.port");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("api.http.port")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.INTEGER)));
    }

    @Test
    public void testFindByLongFlagFieldReferenceEscapeStyle() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--field-reference-escape-style");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("config.field_reference.escape_style")));
    }

    @Test
    public void testFindByLongFlagEnableLocalPluginDev() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--enable-local-plugin-development");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("enable-local-plugin-development")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.BOOLEAN)));
    }

    @Test
    public void testFindByLongFlagVersion() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--version");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.BOOLEAN)));
    }

    @Test
    public void testFindByLongFlagSetting() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--setting");
        assertTrue(opt.isPresent());
    }

    @Test
    public void testFindByLongFlagPathSettings() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--path.settings");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getSettingKey(), is(equalTo("path.settings")));
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.PATH)));
    }

    // -- findByFlag: short flag tests --

    @Test
    public void testFindByShortFlagN() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-n");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--node.name")));
    }

    @Test
    public void testFindByShortFlagF() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-f");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--path.config")));
    }

    @Test
    public void testFindByShortFlagE() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-e");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--config.string")));
    }

    @Test
    public void testFindByShortFlagW() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-w");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--pipeline.workers")));
    }

    @Test
    public void testFindByShortFlagB() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-b");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--pipeline.batch.size")));
    }

    @Test
    public void testFindByShortFlagU() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-u");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--pipeline.batch.delay")));
    }

    @Test
    public void testFindByShortFlagP() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-p");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--path.plugins")));
    }

    @Test
    public void testFindByShortFlagL() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-l");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--path.logs")));
    }

    @Test
    public void testFindByShortFlagV() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-V");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--version")));
    }

    @Test
    public void testFindByShortFlagT() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-t");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--config.test_and_exit")));
    }

    @Test
    public void testFindByShortFlagR() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-r");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--config.reload.automatic")));
    }

    @Test
    public void testFindByShortFlagS() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-S");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--setting")));
    }

    @Test
    public void testFindByShortFlagI() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-i");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--interactive")));
    }

    @Test
    public void testFindByShortFlagH() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("-h");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--help")));
    }

    // -- findByFlag: non-existent --

    @Test
    public void testFindByFlagNonExistent() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--does.not.exist");
        assertFalse(opt.isPresent());
    }

    // -- findBySettingKey tests --

    @Test
    public void testFindBySettingKeyNodeName() {
        final Optional<CliOption> opt = CliOptionRegistry.findBySettingKey("node.name");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--node.name")));
    }

    @Test
    public void testFindBySettingKeyPipelineWorkers() {
        final Optional<CliOption> opt = CliOptionRegistry.findBySettingKey("pipeline.workers");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--pipeline.workers")));
    }

    @Test
    public void testFindBySettingKeyPathConfig() {
        final Optional<CliOption> opt = CliOptionRegistry.findBySettingKey("path.config");
        assertTrue(opt.isPresent());
    }

    @Test
    public void testFindBySettingKeyApiHttpPort() {
        final Optional<CliOption> opt = CliOptionRegistry.findBySettingKey("api.http.port");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getType(), is(equalTo(CliOption.OptionType.INTEGER)));
    }

    @Test
    public void testFindBySettingKeyFieldReferenceEscapeStyle() {
        final Optional<CliOption> opt = CliOptionRegistry.findBySettingKey("config.field_reference.escape_style");
        assertTrue(opt.isPresent());
        assertThat(opt.get().getLongFlag(), is(equalTo("--field-reference-escape-style")));
    }

    @Test
    public void testFindBySettingKeyNonExistent() {
        final Optional<CliOption> opt = CliOptionRegistry.findBySettingKey("does.not.exist");
        assertFalse(opt.isPresent());
    }

    // -- Deprecated options --

    @Test
    public void testDeprecatedVerboseOption() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--verbose");
        assertTrue(opt.isPresent());
        assertThat(opt.get().isDeprecated(), is(true));
        assertThat(opt.get().getDeprecatedAlias(), is(equalTo("log.level")));
    }

    @Test
    public void testDeprecatedDebugOption() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--debug");
        assertTrue(opt.isPresent());
        assertThat(opt.get().isDeprecated(), is(true));
        assertThat(opt.get().getDeprecatedAlias(), is(equalTo("log.level")));
    }

    @Test
    public void testDeprecatedQuietOption() {
        final Optional<CliOption> opt = CliOptionRegistry.findByFlag("--quiet");
        assertTrue(opt.isPresent());
        assertThat(opt.get().isDeprecated(), is(true));
        assertThat(opt.get().getDeprecatedAlias(), is(equalTo("log.level")));
    }

    // -- Option uses SettingKeyDefinitions constants --

    @Test
    public void testUsesSettingKeyDefinitionsConstants() {
        // Verify that the registry uses the same constants as SettingKeyDefinitions
        final Optional<CliOption> pipelineId = CliOptionRegistry.findBySettingKey("pipeline.id");
        assertTrue(pipelineId.isPresent());

        final Optional<CliOption> pipelineWorkers = CliOptionRegistry.findBySettingKey("pipeline.workers");
        assertTrue(pipelineWorkers.isPresent());

        final Optional<CliOption> pipelineBatchSize = CliOptionRegistry.findBySettingKey("pipeline.batch.size");
        assertTrue(pipelineBatchSize.isPresent());
    }

    // -- All options have long flag --

    @Test
    public void testAllOptionsHaveLongFlag() {
        for (CliOption opt : CliOptionRegistry.getOptions()) {
            assertThat("Option missing long flag", opt.getLongFlag(), is(notNullValue()));
            assertTrue("Long flag should start with --: " + opt.getLongFlag(),
                    opt.getLongFlag().startsWith("--"));
        }
    }

    // -- All options with short flags have valid format --

    @Test
    public void testAllShortFlagsHaveValidFormat() {
        for (CliOption opt : CliOptionRegistry.getOptions()) {
            if (opt.getShortFlag() != null) {
                assertTrue("Short flag should start with -: " + opt.getShortFlag(),
                        opt.getShortFlag().startsWith("-"));
                assertFalse("Short flag should not start with --: " + opt.getShortFlag(),
                        opt.getShortFlag().startsWith("--"));
            }
        }
    }
}
