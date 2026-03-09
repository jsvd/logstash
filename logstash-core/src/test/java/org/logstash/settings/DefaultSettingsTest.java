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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class DefaultSettingsTest {

    private SettingsContainer settings;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        settings = new SettingsContainer();
        // Create the data directory so WritableDirectorySetting validation passes
        File dataDir = tempFolder.newFolder("data");
        DefaultSettings.registerAll(settings, tempFolder.getRoot().getAbsolutePath());
    }

    @Test
    public void registersAllExpectedSettings() {
        // Verify a representative sample of settings are registered
        assertTrue(settings.registered("allow_superuser"));
        assertTrue(settings.registered("node.name"));
        assertTrue(settings.registered("path.config"));
        assertTrue(settings.registered("path.data"));
        assertTrue(settings.registered("pipeline.id"));
        assertTrue(settings.registered("pipeline.workers"));
        assertTrue(settings.registered("pipeline.batch.size"));
        assertTrue(settings.registered("api.http.port"));
        assertTrue(settings.registered("queue.type"));
        assertTrue(settings.registered("dead_letter_queue.enable"));
        assertTrue(settings.registered("keystore.classname"));
        assertTrue(settings.registered("monitoring.cluster_uuid"));
        assertTrue(settings.registered("pipeline.buffer.type"));

        // Derived path settings
        assertTrue(settings.registered("path.queue"));
        assertTrue(settings.registered("path.dead_letter_queue"));
    }

    @Test
    public void booleanDefaults() {
        assertEquals(false, settings.get("allow_superuser"));
        assertEquals(false, settings.get("config.test_and_exit"));
        assertEquals(false, settings.get("config.reload.automatic"));
        assertEquals(true, settings.get("metric.collect"));
        assertEquals(false, settings.get("pipeline.system"));
        assertEquals(true, settings.get("pipeline.reloadable"));
        assertEquals(true, settings.get("api.enabled"));
        assertEquals(false, settings.get("api.ssl.enabled"));
        assertEquals(false, settings.get("dead_letter_queue.enable"));
        assertEquals(true, settings.get("queue.checkpoint.retry"));
    }

    @Test
    public void stringDefaults() {
        assertEquals("main", settings.get("pipeline.id"));
        assertEquals("info", settings.get("log.level"));
        assertEquals("plain", settings.get("log.format"));
        assertEquals("127.0.0.1", settings.get("api.http.host"));
        assertEquals("production", settings.get("api.environment"));
        assertEquals("none", settings.get("api.auth.type"));
        assertEquals("memory", settings.get("queue.type"));
        assertEquals("none", settings.get("queue.compression"));
        assertEquals("drop_newer", settings.get("dead_letter_queue.storage_policy"));
        assertEquals("org.logstash.secret.store.backend.JavaKeyStore", settings.get("keystore.classname"));
        assertEquals("heap", settings.get("pipeline.buffer.type"));
    }

    @Test
    public void numericDefaults() {
        assertEquals(50, settings.get("pipeline.batch.delay"));
        assertEquals(0, settings.get("queue.max_events"));
        assertEquals(1024, settings.get("queue.checkpoint.acks"));
        assertEquals(1024, settings.get("queue.checkpoint.writes"));
        assertEquals(1000, settings.get("queue.checkpoint.interval"));
        assertEquals(5000, settings.get("dead_letter_queue.flush_interval"));
        assertEquals(8, settings.get("api.auth.basic.password_policy.length.minimum"));
    }

    @Test
    public void positiveIntegerDefaults() {
        assertEquals(125, settings.get("pipeline.batch.size"));
        int workers = (Integer) settings.get("pipeline.workers");
        assertTrue("pipeline.workers should be > 0", workers > 0);
        assertEquals(Runtime.getRuntime().availableProcessors(), workers);
    }

    @Test
    public void pathDataDefault() {
        String expected = Paths.get(tempFolder.getRoot().getAbsolutePath(), "data").toString();
        assertEquals(expected, settings.get("path.data"));
    }

    @Test
    public void derivedPathDefaults() {
        String dataPath = Paths.get(tempFolder.getRoot().getAbsolutePath(), "data").toString();
        assertEquals(Paths.get(dataPath, "queue").toString(), settings.get("path.queue"));
        assertEquals(Paths.get(dataPath, "dead_letter_queue").toString(), settings.get("path.dead_letter_queue"));
    }

    @Test
    public void keystoreFileDefault() {
        String expected = Paths.get(tempFolder.getRoot().getAbsolutePath(), "config", "logstash.keystore").toString();
        assertEquals(expected, settings.get("keystore.file"));
    }

    @Test
    public void portRangeDefault() {
        Object portRange = settings.get("api.http.port");
        assertNotNull(portRange);
        assertTrue(portRange instanceof Range);
        @SuppressWarnings("unchecked")
        Range<Integer> range = (Range<Integer>) portRange;
        assertEquals(Integer.valueOf(9600), range.getFirst());
        assertEquals(Integer.valueOf(9700), range.getLast());
    }

    @Test
    public void coercibleStringDefaults() {
        assertEquals("auto", settings.get("pipeline.ordered"));
        assertEquals("v8", settings.get("pipeline.ecs_compatibility"));
    }

    @Test
    public void nodeNameDefaultIsNotNull() {
        assertNotNull(settings.get("node.name"));
        assertFalse(((String) settings.get("node.name")).isEmpty());
    }

    @Test
    public void nullableSettingsDefaultToNull() {
        assertNull(settings.get("path.config"));
        assertNull(settings.get("config.string"));
        assertNull(settings.get("interactive"));
        assertNull(settings.get("monitoring.cluster_uuid"));
        assertNull(settings.get("dead_letter_queue.retain.age"));
    }
}
