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

package org.logstash.settings;

import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.jruby.exceptions.ArgumentError;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.logstash.util.TimeValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TimeValueSettingTest {

    private static final String CONFIG = "log4j2-test1.xml";

    @ClassRule
    public static LoggerContextRule CTX = new LoggerContextRule(CONFIG);

    private ListAppender appender;

    @Before
    public void setUp() {
        appender = CTX.getListAppender("EventLogger").clear();
    }

    @Test
    public void testDefaultValueCoercesCorrectly() {
        TimeValueSetting setting = new TimeValueSetting("option", "-1");
        assertEquals(-1L, setting.value().toNanos());
    }

    @Test
    public void testInvalidStringValueThrowsArgumentError() {
        TimeValueSetting setting = new TimeValueSetting("option", "-1");
        assertThrows(org.jruby.exceptions.ArgumentError.class, () -> setting.set("invalid"));
    }

    @Test
    public void testInvalidFloatValueThrowsArgumentError() {
        TimeValueSetting setting = new TimeValueSetting("option", "-1");
        assertThrows(org.jruby.exceptions.ArgumentError.class, () -> setting.set(5.5));
    }

    @Test
    public void testSetWithTimeValueString() {
        TimeValueSetting setting = new TimeValueSetting("option", "-1");
        setting.set("18m");
        assertEquals(18L * 60 * 1_000_000_000L, setting.value().toNanos());
    }

    @Test
    public void testSetWithIntegerValueAsNanosecondsLogsADeprecationMessage() {
        TimeValueSetting setting = new TimeValueSetting("option", "-1");
        setting.set(5);
        assertEquals(5L, setting.value().toNanos());

        boolean printStalling = appender.getMessages().stream().anyMatch((msg) -> msg.contains("Time units will be required in a future release of Logstash."));
        assertTrue(printStalling);
    }

    @Test
    public void givenNumberExceedingMaxInt_whenSet_thenThrowsArgumentError() {
        TimeValueSetting setting = new TimeValueSetting("option", "-1");
        long valueExceedingMaxInt = (long) Integer.MAX_VALUE + 1;
        ArgumentError ex = assertThrows(ArgumentError.class, () -> setting.set(valueExceedingMaxInt));
        assertThat(ex.getMessage()).contains("exceeds the maximum int");
    }

    @Test
    public void defaultValueIsParsedFromString() {
        TimeValueSetting sut = new TimeValueSetting("config.reload.interval", "3s");
        TimeValue value = sut.value();
        assertNotNull(value);
        assertEquals(3_000_000_000L, value.toNanos());
    }

    @Test
    public void setWithTimeValueInstance() {
        TimeValueSetting sut = new TimeValueSetting("test.time", "1s");
        TimeValue tv = TimeValue.fromValue("5m");
        sut.set(tv);
        assertEquals(tv, sut.value());
    }

    @Test
    public void setWithIntegerCoercesToNanoseconds() {
        TimeValueSetting sut = new TimeValueSetting("test.time", "-1", false);
        sut.set(1000);
        assertEquals(1000L, sut.value().toNanos());
    }

    @Test
    public void setWithLongCoercesToNanoseconds() {
        TimeValueSetting sut = new TimeValueSetting("test.time", "-1", false);
        sut.set(5000L);
        assertEquals(5000L, sut.value().toNanos());
    }

    @Test
    public void negativeOneStringDefault() {
        TimeValueSetting sut = new TimeValueSetting("slowlog.threshold.warn", "-1");
        assertEquals(-1L, sut.value().toNanos());
    }

    @Test(expected = org.jruby.exceptions.ArgumentError.class)
    public void invalidStringThrows() {
        TimeValueSetting sut = new TimeValueSetting("test.time", "1s");
        sut.set("invalid");
    }

    @Test
    public void resetReturnsToDefault() {
        TimeValueSetting sut = new TimeValueSetting("test.time", "3s");
        sut.set("5m");
        assertNotEquals(TimeValue.fromValue("3s"), sut.value());
        sut.reset();
        assertEquals(TimeValue.fromValue("3s"), sut.value());
    }

    @Test
    public void isSetReturnsFalseByDefault() {
        TimeValueSetting sut = new TimeValueSetting("test.time", "3s");
        assertFalse(sut.isSet());
    }

    @Test
    public void isSetReturnsTrueAfterSet() {
        TimeValueSetting sut = new TimeValueSetting("test.time", "3s");
        sut.set("5s");
        assertTrue(sut.isSet());
    }
}
