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

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link CliOption}.
 */
public final class CliOptionTest {

    @Test
    public void testBuilderMinimalOption() {
        final CliOption opt = CliOption.builder("--test.flag").build();
        assertThat(opt.getLongFlag(), is(equalTo("--test.flag")));
        assertThat(opt.getShortFlag(), is(nullValue()));
        assertThat(opt.getSettingKey(), is(nullValue()));
        assertThat(opt.getDescription(), is(equalTo("")));
        assertThat(opt.getType(), is(equalTo(CliOption.OptionType.STRING)));
        assertThat(opt.getDefaultValue(), is(nullValue()));
        assertThat(opt.isRequired(), is(false));
        assertThat(opt.isDeprecated(), is(false));
        assertThat(opt.getDeprecatedAlias(), is(nullValue()));
    }

    @Test
    public void testBuilderFullOption() {
        final CliOption opt = CliOption.builder("--pipeline.workers")
                .shortFlag("-w")
                .settingKey("pipeline.workers")
                .description("Number of workers")
                .type(CliOption.OptionType.INTEGER)
                .defaultValue(4)
                .required(true)
                .deprecated(false)
                .build();

        assertThat(opt.getLongFlag(), is(equalTo("--pipeline.workers")));
        assertThat(opt.getShortFlag(), is(equalTo("-w")));
        assertThat(opt.getSettingKey(), is(equalTo("pipeline.workers")));
        assertThat(opt.getDescription(), is(equalTo("Number of workers")));
        assertThat(opt.getType(), is(equalTo(CliOption.OptionType.INTEGER)));
        assertThat(opt.getDefaultValue(), is(equalTo(4)));
        assertThat(opt.isRequired(), is(true));
        assertThat(opt.isDeprecated(), is(false));
    }

    @Test
    public void testDeprecatedOption() {
        final CliOption opt = CliOption.builder("--verbose")
                .description("Deprecated")
                .type(CliOption.OptionType.BOOLEAN)
                .deprecated(true)
                .deprecatedAlias("log.level")
                .build();

        assertThat(opt.isDeprecated(), is(true));
        assertThat(opt.getDeprecatedAlias(), is(equalTo("log.level")));
    }

    @Test
    public void testTakesArgumentForStringType() {
        final CliOption opt = CliOption.builder("--node.name")
                .type(CliOption.OptionType.STRING)
                .build();
        assertThat(opt.takesArgument(), is(true));
    }

    @Test
    public void testTakesArgumentForIntegerType() {
        final CliOption opt = CliOption.builder("--pipeline.workers")
                .type(CliOption.OptionType.INTEGER)
                .build();
        assertThat(opt.takesArgument(), is(true));
    }

    @Test
    public void testTakesArgumentForBooleanType() {
        final CliOption opt = CliOption.builder("--pipeline.unsafe_shutdown")
                .type(CliOption.OptionType.BOOLEAN)
                .build();
        assertThat(opt.takesArgument(), is(false));
    }

    @Test
    public void testTakesArgumentForPathType() {
        final CliOption opt = CliOption.builder("--path.data")
                .type(CliOption.OptionType.PATH)
                .build();
        assertThat(opt.takesArgument(), is(true));
    }

    @Test
    public void testTakesArgumentForPasswordType() {
        final CliOption opt = CliOption.builder("--secret")
                .type(CliOption.OptionType.PASSWORD)
                .build();
        assertThat(opt.takesArgument(), is(true));
    }

    @Test
    public void testTakesArgumentForStringListType() {
        final CliOption opt = CliOption.builder("--path.plugins")
                .type(CliOption.OptionType.STRING_LIST)
                .build();
        assertThat(opt.takesArgument(), is(true));
    }

    @Test
    public void testToMapMinimal() {
        final CliOption opt = CliOption.builder("--test")
                .build();
        final Map<String, Object> map = opt.toMap();

        assertThat(map.get("longFlag"), is(equalTo("--test")));
        assertThat(map.containsKey("shortFlag"), is(false));
        assertThat(map.containsKey("settingKey"), is(false));
        assertThat(map.get("description"), is(equalTo("")));
        assertThat(map.get("type"), is(equalTo("STRING")));
        assertThat(map.containsKey("defaultValue"), is(false));
        assertThat(map.get("required"), is(false));
        assertThat(map.get("deprecated"), is(false));
        assertThat(map.containsKey("deprecatedAlias"), is(false));
    }

    @Test
    public void testToMapFull() {
        final CliOption opt = CliOption.builder("--pipeline.workers")
                .shortFlag("-w")
                .settingKey("pipeline.workers")
                .description("Number of workers")
                .type(CliOption.OptionType.INTEGER)
                .defaultValue(4)
                .deprecated(true)
                .deprecatedAlias("workers")
                .build();
        final Map<String, Object> map = opt.toMap();

        assertThat(map.get("longFlag"), is(equalTo("--pipeline.workers")));
        assertThat(map.get("shortFlag"), is(equalTo("-w")));
        assertThat(map.get("settingKey"), is(equalTo("pipeline.workers")));
        assertThat(map.get("description"), is(equalTo("Number of workers")));
        assertThat(map.get("type"), is(equalTo("INTEGER")));
        assertThat(map.get("defaultValue"), is(equalTo(4)));
        assertThat(map.get("deprecated"), is(true));
        assertThat(map.get("deprecatedAlias"), is(equalTo("workers")));
    }

    @Test
    public void testToStringContainsLongFlag() {
        final CliOption opt = CliOption.builder("--test.flag")
                .shortFlag("-t")
                .settingKey("test.flag")
                .type(CliOption.OptionType.STRING)
                .build();
        final String str = opt.toString();
        assertThat(str.contains("--test.flag"), is(true));
        assertThat(str.contains("-t"), is(true));
        assertThat(str.contains("test.flag"), is(true));
    }

    @Test
    public void testEqualsAndHashCode() {
        final CliOption opt1 = CliOption.builder("--test").settingKey("test").build();
        final CliOption opt2 = CliOption.builder("--test").settingKey("different").build();
        final CliOption opt3 = CliOption.builder("--other").settingKey("test").build();

        assertThat(opt1.equals(opt2), is(true));
        assertThat(opt1.hashCode(), is(equalTo(opt2.hashCode())));
        assertThat(opt1.equals(opt3), is(false));
    }

    @Test
    public void testEqualsNull() {
        final CliOption opt = CliOption.builder("--test").build();
        assertThat(opt.equals(null), is(false));
    }

    @Test
    public void testEqualsSameInstance() {
        final CliOption opt = CliOption.builder("--test").build();
        assertThat(opt.equals(opt), is(true));
    }

    @Test
    public void testEqualsDifferentType() {
        final CliOption opt = CliOption.builder("--test").build();
        assertThat(opt.equals("--test"), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void testBuilderRequiresLongFlag() {
        CliOption.builder(null).build();
    }

    @Test
    public void testAllOptionTypes() {
        final CliOption.OptionType[] types = CliOption.OptionType.values();
        assertThat(types.length, is(6));
        assertThat(CliOption.OptionType.valueOf("STRING"), is(notNullValue()));
        assertThat(CliOption.OptionType.valueOf("INTEGER"), is(notNullValue()));
        assertThat(CliOption.OptionType.valueOf("BOOLEAN"), is(notNullValue()));
        assertThat(CliOption.OptionType.valueOf("PATH"), is(notNullValue()));
        assertThat(CliOption.OptionType.valueOf("PASSWORD"), is(notNullValue()));
        assertThat(CliOption.OptionType.valueOf("STRING_LIST"), is(notNullValue()));
    }
}
