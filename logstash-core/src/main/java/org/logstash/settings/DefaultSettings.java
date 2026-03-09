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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

/**
 * Registers all default Logstash settings into a {@link SettingsContainer}.
 * This is the Java equivalent of the settings registration block in environment.rb.
 */
public class DefaultSettings {

    /**
     * Registers all default settings into the given container.
     *
     * @param settings     the settings container to register into
     * @param logstashHome the LOGSTASH_HOME path
     */
    public static void registerAll(SettingsContainer settings, String logstashHome) {
        String defaultDataPath = Paths.get(logstashHome, "data").toString();
        String defaultKeystorePath = Paths.get(logstashHome, "config", "logstash.keystore").toString();

        // Core settings
        settings.register(new BooleanSetting("allow_superuser", false));
        settings.register(new StringSetting("node.name", getHostname()));
        settings.register(new NullableStringSetting("path.config", null, false));
        settings.register(new WritableDirectorySetting("path.data", defaultDataPath));
        settings.register(new NullableStringSetting("config.string", null, false));
        settings.register(new BooleanSetting("config.test_and_exit", false));
        settings.register(new BooleanSetting("config.reload.automatic", false));
        settings.register(new TimeValueSetting("config.reload.interval", "3s"));
        settings.register(new BooleanSetting("config.support_escapes", false));
        settings.register(new StringSetting("config.field_reference.escape_style", "none", true,
                Arrays.asList("none", "percent", "ampersand")));
        settings.register(new BooleanSetting("metric.collect", true));

        // Pipeline settings
        settings.register(new StringSetting("pipeline.id", "main"));
        settings.register(new BooleanSetting("pipeline.system", false));
        settings.register(new PositiveIntegerSetting("pipeline.workers",
                Runtime.getRuntime().availableProcessors()));
        settings.register(new PositiveIntegerSetting("pipeline.batch.size", 125));
        settings.register(new NumericSetting("pipeline.batch.delay", 50));
        settings.register(new BooleanSetting("pipeline.unsafe_shutdown", false));
        settings.register(new BooleanSetting("pipeline.reloadable", true));
        settings.register(new BooleanSetting("pipeline.plugin_classloaders", false));
        settings.register(new BooleanSetting("pipeline.separate_logs", false));
        settings.register(new CoercibleStringSetting("pipeline.ordered", "auto", true,
                Arrays.asList("auto", "true", "false")));
        settings.register(new CoercibleStringSetting("pipeline.ecs_compatibility", "v8", true,
                Arrays.asList("disabled", "v1", "v8")));

        // Paths
        settings.register(new ArrayCoercibleSetting("path.plugins", null, Collections.emptyList()));

        // Interactive/debug
        settings.register(new NullableStringSetting("interactive", null, false));
        settings.register(new BooleanSetting("config.debug", false));

        // Logging
        settings.register(new StringSetting("log.level", "info", true,
                Arrays.asList("fatal", "error", "warn", "debug", "info", "trace")));
        settings.register(new BooleanSetting("version", false));
        settings.register(new BooleanSetting("help", false));
        settings.register(new BooleanSetting("enable-local-plugin-development", false));
        settings.register(new StringSetting("log.format", "plain", true,
                Arrays.asList("json", "plain")));
        settings.register(new BooleanSetting("log.format.json.fix_duplicate_message_fields", true));

        // API settings
        settings.register(new BooleanSetting("api.enabled", true));
        settings.register(new StringSetting("api.http.host", "127.0.0.1"));
        settings.register(new PortRangeSetting("api.http.port", new Range<>(9600, 9700)));
        settings.register(new StringSetting("api.environment", "production"));
        settings.register(new StringSetting("api.auth.type", "none", true,
                Arrays.asList("none", "basic")));
        settings.register(new StringSetting("api.auth.basic.username", null, false).nullable());
        settings.register(new PasswordSetting("api.auth.basic.password", null, false).nullable());
        settings.register(new StringSetting("api.auth.basic.password_policy.mode", "WARN", true,
                Arrays.asList("WARN", "ERROR")));
        settings.register(new NumericSetting("api.auth.basic.password_policy.length.minimum", 8));
        settings.register(new StringSetting("api.auth.basic.password_policy.include.upper", "REQUIRED", true,
                Arrays.asList("REQUIRED", "OPTIONAL")));
        settings.register(new StringSetting("api.auth.basic.password_policy.include.lower", "REQUIRED", true,
                Arrays.asList("REQUIRED", "OPTIONAL")));
        settings.register(new StringSetting("api.auth.basic.password_policy.include.digit", "REQUIRED", true,
                Arrays.asList("REQUIRED", "OPTIONAL")));
        settings.register(new StringSetting("api.auth.basic.password_policy.include.symbol", "OPTIONAL", true,
                Arrays.asList("REQUIRED", "OPTIONAL")));

        // API SSL
        settings.register(new BooleanSetting("api.ssl.enabled", false));
        settings.register(new ExistingFilePathSetting("api.ssl.keystore.path", null, false).nullable());
        settings.register(new PasswordSetting("api.ssl.keystore.password", null, false).nullable());
        settings.register(new StringArraySetting("api.ssl.supported_protocols", null, true,
                Arrays.asList("TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")));

        // Pipeline batch metrics
        settings.register(new StringSetting("pipeline.batch.metrics.sampling_mode", "minimal", true,
                Arrays.asList("disabled", "minimal", "full")));

        // Queue settings
        settings.register(new StringSetting("queue.type", "memory", true,
                Arrays.asList("persisted", "memory")));
        settings.register(new BooleanSetting("queue.drain", false));
        settings.register(new BytesSetting("queue.page_capacity", "64mb"));
        settings.register(new BytesSetting("queue.max_bytes", "1024mb"));
        settings.register(new NumericSetting("queue.max_events", 0));
        settings.register(new NumericSetting("queue.checkpoint.acks", 1024));
        settings.register(new NumericSetting("queue.checkpoint.writes", 1024));
        settings.register(new NumericSetting("queue.checkpoint.interval", 1000));
        settings.register(new BooleanSetting("queue.checkpoint.retry", true));
        settings.register(new StringSetting("queue.compression", "none", true,
                Arrays.asList("none", "speed", "balanced", "size", "disabled")));

        // Dead letter queue
        settings.register(new BooleanSetting("dead_letter_queue.enable", false));
        settings.register(new BytesSetting("dead_letter_queue.max_bytes", "1024mb"));
        settings.register(new NumericSetting("dead_letter_queue.flush_interval", 5000));
        settings.register(new StringSetting("dead_letter_queue.storage_policy", "drop_newer", true,
                Arrays.asList("drop_newer", "drop_older")));
        settings.register(new NullableStringSetting("dead_letter_queue.retain.age"));

        // Slowlog
        settings.register(new TimeValueSetting("slowlog.threshold.warn", "-1"));
        settings.register(new TimeValueSetting("slowlog.threshold.info", "-1"));
        settings.register(new TimeValueSetting("slowlog.threshold.debug", "-1"));
        settings.register(new TimeValueSetting("slowlog.threshold.trace", "-1"));

        // Keystore
        settings.register(new StringSetting("keystore.classname",
                "org.logstash.secret.store.backend.JavaKeyStore"));
        settings.register(new StringSetting("keystore.file", defaultKeystorePath, false));

        // Monitoring
        settings.register(new NullableStringSetting("monitoring.cluster_uuid"));

        // Pipeline buffer
        settings.register(new StringSetting("pipeline.buffer.type", "heap", true,
                Arrays.asList("direct", "heap")));

        // Derived path settings (computed from path.data default)
        String defaultQueuePath = Paths.get(defaultDataPath, "queue").toString();
        settings.register(new WritableDirectorySetting("path.queue", defaultQueuePath));

        String defaultDlqPath = Paths.get(defaultDataPath, "dead_letter_queue").toString();
        settings.register(new WritableDirectorySetting("path.dead_letter_queue", defaultDlqPath));
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
