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

import org.logstash.common.SettingKeyDefinitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of all Logstash CLI options. This mirrors the option declarations
 * from runner.rb (Clamp-based), expressed purely in Java.
 *
 * <p>Each option maps to a setting key that can be applied to the Logstash settings system.
 */
public final class CliOptionRegistry {

    private static final List<CliOption> OPTIONS;
    private static final Map<String, CliOption> BY_FLAG;
    private static final Map<String, CliOption> BY_SETTING_KEY;

    static {
        final List<CliOption> opts = new ArrayList<>();

        // -- Node Settings --
        opts.add(CliOption.builder("--node.name")
                .shortFlag("-n")
                .settingKey("node.name")
                .description("Specify the name of this logstash instance")
                .type(CliOption.OptionType.STRING)
                .build());

        opts.add(CliOption.builder("--enable-local-plugin-development")
                .settingKey("enable-local-plugin-development")
                .description("Allow Logstash to load ungemified plugins for development")
                .type(CliOption.OptionType.BOOLEAN)
                .defaultValue(false)
                .build());

        // -- Config Settings --
        opts.add(CliOption.builder("--path.config")
                .shortFlag("-f")
                .settingKey("path.config")
                .description("Load the Logstash config from a specific file or directory")
                .type(CliOption.OptionType.STRING)
                .build());

        opts.add(CliOption.builder("--config.string")
                .shortFlag("-e")
                .settingKey("config.string")
                .description("Use the given string as the configuration data")
                .type(CliOption.OptionType.STRING)
                .build());

        opts.add(CliOption.builder("--field-reference-escape-style")
                .settingKey("config.field_reference.escape_style")
                .description("Set the field reference escape style")
                .type(CliOption.OptionType.STRING)
                .build());

        // -- Pipeline Settings --
        opts.add(CliOption.builder("--pipeline.id")
                .settingKey(SettingKeyDefinitions.PIPELINE_ID)
                .description("Set the ID of the pipeline")
                .type(CliOption.OptionType.STRING)
                .defaultValue("main")
                .build());

        opts.add(CliOption.builder("--pipeline.workers")
                .shortFlag("-w")
                .settingKey(SettingKeyDefinitions.PIPELINE_WORKERS)
                .description("Sets the number of pipeline workers to run")
                .type(CliOption.OptionType.INTEGER)
                .build());

        opts.add(CliOption.builder("--pipeline.ordered")
                .settingKey("pipeline.ordered")
                .description("Set the pipeline event ordering")
                .type(CliOption.OptionType.STRING)
                .build());

        opts.add(CliOption.builder("--pipeline.batch.size")
                .shortFlag("-b")
                .settingKey(SettingKeyDefinitions.PIPELINE_BATCH_SIZE)
                .description("Size of batches the pipeline worker receives")
                .type(CliOption.OptionType.INTEGER)
                .defaultValue(125)
                .build());

        opts.add(CliOption.builder("--pipeline.batch.delay")
                .shortFlag("-u")
                .settingKey("pipeline.batch.delay")
                .description("Maximum delay in milliseconds between pipeline batches")
                .type(CliOption.OptionType.INTEGER)
                .defaultValue(50)
                .build());

        opts.add(CliOption.builder("--pipeline.unsafe_shutdown")
                .settingKey("pipeline.unsafe_shutdown")
                .description("Force logstash to exit during shutdown even if there are still inflight events")
                .type(CliOption.OptionType.BOOLEAN)
                .defaultValue(false)
                .build());

        opts.add(CliOption.builder("--pipeline.ecs_compatibility")
                .settingKey("pipeline.ecs_compatibility")
                .description("Set the default ECS compatibility mode for pipelines")
                .type(CliOption.OptionType.STRING)
                .build());

        opts.add(CliOption.builder("--plugin-classloaders")
                .settingKey("pipeline.plugin_classloaders")
                .description("Load Java plugins in independent classloaders")
                .type(CliOption.OptionType.BOOLEAN)
                .defaultValue(false)
                .build());

        // -- Path Settings --
        opts.add(CliOption.builder("--path.data")
                .settingKey("path.data")
                .description("Path to the data directory")
                .type(CliOption.OptionType.PATH)
                .build());

        opts.add(CliOption.builder("--path.plugins")
                .shortFlag("-p")
                .settingKey("path.plugins")
                .description("Path(s) to find custom plugins")
                .type(CliOption.OptionType.STRING_LIST)
                .build());

        opts.add(CliOption.builder("--path.logs")
                .shortFlag("-l")
                .settingKey("path.logs")
                .description("Path for Logstash log files")
                .type(CliOption.OptionType.PATH)
                .build());

        opts.add(CliOption.builder("--path.settings")
                .settingKey("path.settings")
                .description("Path to the Logstash settings directory")
                .type(CliOption.OptionType.PATH)
                .build());

        // -- Logging Settings --
        opts.add(CliOption.builder("--log.level")
                .settingKey("log.level")
                .description("Set the log level (fatal, error, warn, info, debug, trace)")
                .type(CliOption.OptionType.STRING)
                .build());

        opts.add(CliOption.builder("--log.format")
                .settingKey("log.format")
                .description("Set the log format (json, plain)")
                .type(CliOption.OptionType.STRING)
                .build());

        opts.add(CliOption.builder("--log.format.json.fix_duplicate_message_fields")
                .settingKey("log.format.json.fix_duplicate_message_fields")
                .description("Fix duplicate message fields in JSON log format")
                .type(CliOption.OptionType.STRING)
                .build());

        opts.add(CliOption.builder("--config.debug")
                .settingKey("config.debug")
                .description("Print the compiled config ruby code as a debug log")
                .type(CliOption.OptionType.BOOLEAN)
                .defaultValue(false)
                .build());

        // -- Other Settings --
        opts.add(CliOption.builder("--config.test_and_exit")
                .shortFlag("-t")
                .settingKey("config.test_and_exit")
                .description("Check configuration and exit")
                .type(CliOption.OptionType.BOOLEAN)
                .defaultValue(false)
                .build());

        opts.add(CliOption.builder("--config.reload.automatic")
                .shortFlag("-r")
                .settingKey("config.reload.automatic")
                .description("Enable automatic config reloading")
                .type(CliOption.OptionType.BOOLEAN)
                .defaultValue(false)
                .build());

        opts.add(CliOption.builder("--config.reload.interval")
                .settingKey("config.reload.interval")
                .description("Interval between config reload checks")
                .type(CliOption.OptionType.STRING)
                .build());

        // -- API Settings --
        opts.add(CliOption.builder("--api.enabled")
                .settingKey("api.enabled")
                .description("Enable or disable the API endpoint")
                .type(CliOption.OptionType.BOOLEAN)
                .defaultValue(true)
                .build());

        opts.add(CliOption.builder("--api.http.host")
                .settingKey("api.http.host")
                .description("The bind address for the API endpoint")
                .type(CliOption.OptionType.STRING)
                .defaultValue("127.0.0.1")
                .build());

        opts.add(CliOption.builder("--api.http.port")
                .settingKey("api.http.port")
                .description("The bind port for the API endpoint")
                .type(CliOption.OptionType.INTEGER)
                .defaultValue(9600)
                .build());

        // -- Interactive --
        opts.add(CliOption.builder("--interactive")
                .shortFlag("-i")
                .settingKey("interactive")
                .description("Start an interactive Ruby shell")
                .type(CliOption.OptionType.STRING)
                .build());

        // -- Special Flags --
        opts.add(CliOption.builder("--version")
                .shortFlag("-V")
                .description("Display the version of Logstash")
                .type(CliOption.OptionType.BOOLEAN)
                .build());

        opts.add(CliOption.builder("--help")
                .shortFlag("-h")
                .description("Print help information")
                .type(CliOption.OptionType.BOOLEAN)
                .build());

        // -- Passthrough Setting --
        opts.add(CliOption.builder("--setting")
                .shortFlag("-S")
                .description("Set an individual setting (key=value)")
                .type(CliOption.OptionType.STRING)
                .build());

        // -- Deprecated Flags --
        opts.add(CliOption.builder("--verbose")
                .description("Deprecated: use --log.level=info")
                .type(CliOption.OptionType.BOOLEAN)
                .deprecated(true)
                .deprecatedAlias("log.level")
                .build());

        opts.add(CliOption.builder("--debug")
                .description("Deprecated: use --log.level=debug")
                .type(CliOption.OptionType.BOOLEAN)
                .deprecated(true)
                .deprecatedAlias("log.level")
                .build());

        opts.add(CliOption.builder("--quiet")
                .description("Deprecated: use --log.level=error")
                .type(CliOption.OptionType.BOOLEAN)
                .deprecated(true)
                .deprecatedAlias("log.level")
                .build());

        OPTIONS = Collections.unmodifiableList(opts);

        // Build lookup maps
        final Map<String, CliOption> flagMap = new LinkedHashMap<>();
        final Map<String, CliOption> keyMap = new LinkedHashMap<>();
        for (CliOption opt : opts) {
            flagMap.put(opt.getLongFlag(), opt);
            if (opt.getShortFlag() != null) {
                flagMap.put(opt.getShortFlag(), opt);
            }
            if (opt.getSettingKey() != null) {
                keyMap.put(opt.getSettingKey(), opt);
            }
        }
        BY_FLAG = Collections.unmodifiableMap(flagMap);
        BY_SETTING_KEY = Collections.unmodifiableMap(keyMap);
    }

    private CliOptionRegistry() {
        // utility class
    }

    /**
     * Returns all registered CLI options.
     *
     * @return an unmodifiable list of all CLI options
     */
    public static List<CliOption> getOptions() {
        return OPTIONS;
    }

    /**
     * Finds a CLI option by its long or short flag.
     *
     * @param flag the flag string (e.g., "--pipeline.workers" or "-w")
     * @return an Optional containing the matching option, or empty if not found
     */
    public static Optional<CliOption> findByFlag(String flag) {
        return Optional.ofNullable(BY_FLAG.get(flag));
    }

    /**
     * Finds a CLI option by its setting key.
     *
     * @param key the setting key (e.g., "pipeline.workers")
     * @return an Optional containing the matching option, or empty if not found
     */
    public static Optional<CliOption> findBySettingKey(String key) {
        return Optional.ofNullable(BY_SETTING_KEY.get(key));
    }
}
