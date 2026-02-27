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
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CliParser}.
 */
public final class CliParserTest {

    // -- Empty/null args --

    @Test
    public void testParseNullArgs() {
        final CliParseResult result = CliParser.parse(null);
        assertFalse(result.isVersionRequested());
        assertFalse(result.isHelpRequested());
        assertTrue(result.getUnknownFlags().isEmpty());
    }

    @Test
    public void testParseEmptyArgs() {
        final CliParseResult result = CliParser.parse(new String[]{});
        assertFalse(result.isVersionRequested());
        assertFalse(result.isHelpRequested());
        assertTrue(result.getUnknownFlags().isEmpty());
    }

    // -- Long flag with space-separated value --

    @Test
    public void testParseLongFlagWithValue() {
        final CliParseResult result = CliParser.parse(new String[]{"--node.name", "mynode"});
        assertTrue(result.hasOption("node.name"));
        assertThat(result.getString("node.name").orElse(""), is(equalTo("mynode")));
    }

    @Test
    public void testParseLongFlagPathConfig() {
        final CliParseResult result = CliParser.parse(new String[]{"--path.config", "/etc/logstash/conf.d"});
        assertTrue(result.hasOption("path.config"));
        assertThat(result.getString("path.config").orElse(""), is(equalTo("/etc/logstash/conf.d")));
    }

    // -- Long flag with = value --

    @Test
    public void testParseLongFlagWithEqualsValue() {
        final CliParseResult result = CliParser.parse(new String[]{"--node.name=mynode"});
        assertTrue(result.hasOption("node.name"));
        assertThat(result.getString("node.name").orElse(""), is(equalTo("mynode")));
    }

    @Test
    public void testParseLongFlagWithEqualsAndEmptyValue() {
        final CliParseResult result = CliParser.parse(new String[]{"--config.string="});
        assertTrue(result.hasOption("config.string"));
        assertThat(result.getString("config.string").orElse("missing"), is(equalTo("")));
    }

    // -- Short flag with value --

    @Test
    public void testParseShortFlagWithValue() {
        final CliParseResult result = CliParser.parse(new String[]{"-f", "/etc/logstash/conf.d"});
        assertTrue(result.hasOption("path.config"));
        assertThat(result.getString("path.config").orElse(""), is(equalTo("/etc/logstash/conf.d")));
    }

    @Test
    public void testParseShortFlagE() {
        final CliParseResult result = CliParser.parse(new String[]{"-e", "input { stdin {} }"});
        assertTrue(result.hasOption("config.string"));
        assertThat(result.getString("config.string").orElse(""), is(equalTo("input { stdin {} }")));
    }

    @Test
    public void testParseShortFlagN() {
        final CliParseResult result = CliParser.parse(new String[]{"-n", "testnode"});
        assertTrue(result.hasOption("node.name"));
        assertThat(result.getString("node.name").orElse(""), is(equalTo("testnode")));
    }

    // -- Integer type parsing --

    @Test
    public void testParseIntegerOption() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.workers", "8"});
        assertTrue(result.hasOption("pipeline.workers"));
        assertThat(result.getInt("pipeline.workers").orElse(0), is(equalTo(8)));
    }

    @Test
    public void testParseIntegerOptionShortFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"-w", "4"});
        assertTrue(result.hasOption("pipeline.workers"));
        assertThat(result.getInt("pipeline.workers").orElse(0), is(equalTo(4)));
    }

    @Test
    public void testParseIntegerOptionWithEquals() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.batch.size=250"});
        assertTrue(result.hasOption("pipeline.batch.size"));
        assertThat(result.getInt("pipeline.batch.size").orElse(0), is(equalTo(250)));
    }

    @Test
    public void testParseIntegerOptionBatchDelay() {
        final CliParseResult result = CliParser.parse(new String[]{"-u", "100"});
        assertTrue(result.hasOption("pipeline.batch.delay"));
        assertThat(result.getInt("pipeline.batch.delay").orElse(0), is(equalTo(100)));
    }

    @Test
    public void testParseApiHttpPort() {
        final CliParseResult result = CliParser.parse(new String[]{"--api.http.port", "9601"});
        assertTrue(result.hasOption("api.http.port"));
        assertThat(result.getInt("api.http.port").orElse(0), is(equalTo(9601)));
    }

    // -- Boolean flag parsing --

    @Test
    public void testParseBooleanFlagPresent() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.unsafe_shutdown"});
        assertTrue(result.hasOption("pipeline.unsafe_shutdown"));
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(false), is(true));
    }

    @Test
    public void testParseBooleanFlagWithTrueValue() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.unsafe_shutdown", "true"});
        assertTrue(result.hasOption("pipeline.unsafe_shutdown"));
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(false), is(true));
    }

    @Test
    public void testParseBooleanFlagWithFalseValue() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.unsafe_shutdown", "false"});
        assertTrue(result.hasOption("pipeline.unsafe_shutdown"));
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(true), is(false));
    }

    @Test
    public void testParseBooleanNoPrefix() {
        final CliParseResult result = CliParser.parse(new String[]{"--no-pipeline.unsafe_shutdown"});
        assertTrue(result.hasOption("pipeline.unsafe_shutdown"));
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(true), is(false));
    }

    @Test
    public void testParseConfigDebugBoolean() {
        final CliParseResult result = CliParser.parse(new String[]{"--config.debug"});
        assertTrue(result.hasOption("config.debug"));
        assertThat(result.getBoolean("config.debug").orElse(false), is(true));
    }

    @Test
    public void testParseConfigTestAndExit() {
        final CliParseResult result = CliParser.parse(new String[]{"-t"});
        assertTrue(result.hasOption("config.test_and_exit"));
        assertThat(result.getBoolean("config.test_and_exit").orElse(false), is(true));
    }

    @Test
    public void testParseConfigReloadAutomatic() {
        final CliParseResult result = CliParser.parse(new String[]{"-r"});
        assertTrue(result.hasOption("config.reload.automatic"));
        assertThat(result.getBoolean("config.reload.automatic").orElse(false), is(true));
    }

    @Test
    public void testParseEnableLocalPluginDev() {
        final CliParseResult result = CliParser.parse(new String[]{"--enable-local-plugin-development"});
        assertTrue(result.hasOption("enable-local-plugin-development"));
        assertThat(result.getBoolean("enable-local-plugin-development").orElse(false), is(true));
    }

    // -- Version flag --

    @Test
    public void testParseVersionLongFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"--version"});
        assertTrue(result.isVersionRequested());
    }

    @Test
    public void testParseVersionShortFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"-V"});
        assertTrue(result.isVersionRequested());
    }

    // -- Help flag --

    @Test
    public void testParseHelpLongFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"--help"});
        assertTrue(result.isHelpRequested());
    }

    @Test
    public void testParseHelpShortFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"-h"});
        assertTrue(result.isHelpRequested());
    }

    // -- Passthrough settings --

    @Test
    public void testParsePassthroughSettingWithShortFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"-S", "custom.key=custom.value"});
        final Map<String, String> passthrough = result.getPassthroughSettings();
        assertThat(passthrough.size(), is(1));
        assertThat(passthrough.get("custom.key"), is(equalTo("custom.value")));
    }

    @Test
    public void testParsePassthroughSettingWithLongFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"--setting", "custom.key=custom.value"});
        final Map<String, String> passthrough = result.getPassthroughSettings();
        assertThat(passthrough.size(), is(1));
        assertThat(passthrough.get("custom.key"), is(equalTo("custom.value")));
    }

    @Test
    public void testParseMultiplePassthroughSettings() {
        final CliParseResult result = CliParser.parse(new String[]{
                "-S", "key1=val1", "-S", "key2=val2"
        });
        final Map<String, String> passthrough = result.getPassthroughSettings();
        assertThat(passthrough.size(), is(2));
        assertThat(passthrough.get("key1"), is(equalTo("val1")));
        assertThat(passthrough.get("key2"), is(equalTo("val2")));
    }

    @Test
    public void testParsePassthroughSettingWithValueContainingEquals() {
        final CliParseResult result = CliParser.parse(new String[]{"-S", "key=value=with=equals"});
        final Map<String, String> passthrough = result.getPassthroughSettings();
        assertThat(passthrough.get("key"), is(equalTo("value=with=equals")));
    }

    // -- Unknown flags --

    @Test
    public void testParseUnknownLongFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"--unknown.option", "value"});
        final List<String> unknown = result.getUnknownFlags();
        assertTrue(unknown.contains("--unknown.option"));
    }

    @Test
    public void testParseUnknownShortFlag() {
        final CliParseResult result = CliParser.parse(new String[]{"-Z"});
        final List<String> unknown = result.getUnknownFlags();
        assertTrue(unknown.contains("-Z"));
    }

    @Test
    public void testParseUnknownNoPrefix() {
        final CliParseResult result = CliParser.parse(new String[]{"--no-unknown.flag"});
        final List<String> unknown = result.getUnknownFlags();
        assertTrue(unknown.contains("--no-unknown.flag"));
    }

    // -- Multiple options combined --

    @Test
    public void testParseMultipleOptions() {
        final CliParseResult result = CliParser.parse(new String[]{
                "-f", "/etc/logstash/pipelines",
                "-w", "4",
                "--pipeline.batch.size=250",
                "--log.level", "debug",
                "--pipeline.unsafe_shutdown",
                "-S", "custom.setting=value"
        });

        assertThat(result.getString("path.config").orElse(""), is(equalTo("/etc/logstash/pipelines")));
        assertThat(result.getInt("pipeline.workers").orElse(0), is(equalTo(4)));
        assertThat(result.getInt("pipeline.batch.size").orElse(0), is(equalTo(250)));
        assertThat(result.getString("log.level").orElse(""), is(equalTo("debug")));
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(false), is(true));
        assertThat(result.getPassthroughSettings().get("custom.setting"), is(equalTo("value")));
    }

    @Test
    public void testParseRealWorldCommandLine() {
        final CliParseResult result = CliParser.parse(new String[]{
                "--path.config", "/etc/logstash/conf.d",
                "--path.data", "/var/lib/logstash",
                "--path.logs", "/var/log/logstash",
                "--pipeline.workers", "2",
                "--pipeline.batch.size", "125",
                "--log.level", "info",
                "--api.http.port", "9600"
        });

        assertThat(result.getString("path.config").orElse(""), is(equalTo("/etc/logstash/conf.d")));
        assertThat(result.getString("path.data").orElse(""), is(equalTo("/var/lib/logstash")));
        assertThat(result.getString("path.logs").orElse(""), is(equalTo("/var/log/logstash")));
        assertThat(result.getInt("pipeline.workers").orElse(0), is(equalTo(2)));
        assertThat(result.getInt("pipeline.batch.size").orElse(0), is(equalTo(125)));
        assertThat(result.getString("log.level").orElse(""), is(equalTo("info")));
        assertThat(result.getInt("api.http.port").orElse(0), is(equalTo(9600)));
        assertTrue(result.getUnknownFlags().isEmpty());
    }

    // -- STRING_LIST type --

    @Test
    public void testParseStringListOption() {
        final CliParseResult result = CliParser.parse(new String[]{
                "--path.plugins", "/plugins/a",
                "--path.plugins", "/plugins/b"
        });
        final List<String> plugins = result.getStringList("path.plugins");
        assertThat(plugins.size(), is(2));
        assertThat(plugins.get(0), is(equalTo("/plugins/a")));
        assertThat(plugins.get(1), is(equalTo("/plugins/b")));
    }

    @Test
    public void testParseStringListWithShortFlag() {
        final CliParseResult result = CliParser.parse(new String[]{
                "-p", "/plugins/a",
                "-p", "/plugins/b",
                "-p", "/plugins/c"
        });
        final List<String> plugins = result.getStringList("path.plugins");
        assertThat(plugins.size(), is(3));
    }

    // -- Positional arguments treated as unknown --

    @Test
    public void testParsePositionalArgument() {
        final CliParseResult result = CliParser.parse(new String[]{"positional"});
        assertTrue(result.getUnknownFlags().contains("positional"));
    }

    // -- Boolean with case-insensitive value --

    @Test
    public void testParseBooleanUpperCaseTrue() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.unsafe_shutdown", "TRUE"});
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(false), is(true));
    }

    @Test
    public void testParseBooleanMixedCaseFalse() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.unsafe_shutdown", "False"});
        assertThat(result.getBoolean("pipeline.unsafe_shutdown").orElse(true), is(false));
    }

    // -- Edge cases --

    @Test
    public void testParseVersionWithOtherFlags() {
        final CliParseResult result = CliParser.parse(new String[]{"--version", "--node.name", "test"});
        assertTrue(result.isVersionRequested());
        assertTrue(result.hasOption("node.name"));
    }

    @Test
    public void testParseEqualsValueWithSpecialChars() {
        final CliParseResult result = CliParser.parse(new String[]{"--config.string=input { stdin {} } output { stdout {} }"});
        assertThat(result.getString("config.string").orElse(""),
                is(equalTo("input { stdin {} } output { stdout {} }")));
    }

    @Test
    public void testParseApiEnabledBoolean() {
        final CliParseResult result = CliParser.parse(new String[]{"--api.enabled", "false"});
        assertThat(result.getBoolean("api.enabled").orElse(true), is(false));
    }

    @Test
    public void testParseLogLevelString() {
        final CliParseResult result = CliParser.parse(new String[]{"--log.level", "trace"});
        assertThat(result.getString("log.level").orElse(""), is(equalTo("trace")));
    }

    @Test
    public void testParseLogFormatString() {
        final CliParseResult result = CliParser.parse(new String[]{"--log.format", "json"});
        assertThat(result.getString("log.format").orElse(""), is(equalTo("json")));
    }

    @Test
    public void testParseConfigReloadInterval() {
        final CliParseResult result = CliParser.parse(new String[]{"--config.reload.interval", "3s"});
        assertThat(result.getString("config.reload.interval").orElse(""), is(equalTo("3s")));
    }

    @Test
    public void testParseApiHttpHost() {
        final CliParseResult result = CliParser.parse(new String[]{"--api.http.host", "0.0.0.0"});
        assertThat(result.getString("api.http.host").orElse(""), is(equalTo("0.0.0.0")));
    }

    @Test
    public void testParseInteractiveOption() {
        final CliParseResult result = CliParser.parse(new String[]{"-i", "irb"});
        assertThat(result.getString("interactive").orElse(""), is(equalTo("irb")));
    }

    @Test
    public void testParsePathSettings() {
        final CliParseResult result = CliParser.parse(new String[]{"--path.settings", "/etc/logstash"});
        assertThat(result.getString("path.settings").orElse(""), is(equalTo("/etc/logstash")));
    }

    @Test
    public void testParsePipelineOrdered() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.ordered", "auto"});
        assertThat(result.getString("pipeline.ordered").orElse(""), is(equalTo("auto")));
    }

    @Test
    public void testParsePipelineEcsCompatibility() {
        final CliParseResult result = CliParser.parse(new String[]{"--pipeline.ecs_compatibility", "v8"});
        assertThat(result.getString("pipeline.ecs_compatibility").orElse(""), is(equalTo("v8")));
    }

    @Test
    public void testParseFieldReferenceEscapeStyle() {
        final CliParseResult result = CliParser.parse(new String[]{"--field-reference-escape-style", "percent"});
        assertThat(result.getString("config.field_reference.escape_style").orElse(""), is(equalTo("percent")));
    }
}
