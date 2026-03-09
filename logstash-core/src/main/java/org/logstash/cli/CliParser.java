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

import java.util.Optional;

/**
 * Pure Java CLI argument parser for Logstash.
 * Parses command-line arguments using the definitions from {@link CliOptionRegistry}.
 *
 * <p>Supports:
 * <ul>
 *   <li>Long flags: {@code --key value}, {@code --key=value}</li>
 *   <li>Short flags: {@code -f value}</li>
 *   <li>Boolean flags: {@code --flag}, {@code --no-flag}, {@code --flag true}, {@code --flag false}</li>
 *   <li>Passthrough settings: {@code -S key=value}, {@code --setting key=value}</li>
 * </ul>
 */
public final class CliParser {

    private static final String NO_PREFIX = "--no-";
    private static final String SETTING_LONG = "--setting";
    private static final String SETTING_SHORT = "-S";

    private CliParser() {
        // utility class
    }

    /**
     * Parses the given command-line arguments.
     *
     * @param args the command-line arguments
     * @return a {@link CliParseResult} containing all parsed values
     */
    public static CliParseResult parse(String[] args) {
        final CliParseResult.Builder builder = CliParseResult.builder();

        if (args == null || args.length == 0) {
            return builder.build();
        }

        int i = 0;
        while (i < args.length) {
            final String arg = args[i];

            // Handle passthrough settings: -S key=value or --setting key=value
            if (SETTING_SHORT.equals(arg) || SETTING_LONG.equals(arg)) {
                i++;
                if (i < args.length) {
                    parsePassthroughSetting(builder, args[i]);
                }
                i++;
                continue;
            }

            // Handle --key=value form
            if (arg.startsWith("--") && arg.contains("=")) {
                final int eqIdx = arg.indexOf('=');
                final String flag = arg.substring(0, eqIdx);
                final String value = arg.substring(eqIdx + 1);
                i = handleFlagWithValue(builder, flag, value, args, i, false);
                i++;
                continue;
            }

            // Handle --no-flag form (boolean negation)
            if (arg.startsWith(NO_PREFIX)) {
                final String positiveFlag = "--" + arg.substring(NO_PREFIX.length());
                final Optional<CliOption> optOpt = CliOptionRegistry.findByFlag(positiveFlag);
                if (optOpt.isPresent() && optOpt.get().getType() == CliOption.OptionType.BOOLEAN) {
                    final CliOption opt = optOpt.get();
                    if (opt.getSettingKey() != null) {
                        builder.setValue(opt.getSettingKey(), false);
                    }
                } else {
                    builder.addUnknownFlag(arg);
                }
                i++;
                continue;
            }

            // Handle normal flags (long or short)
            if (arg.startsWith("-")) {
                final Optional<CliOption> optOpt = CliOptionRegistry.findByFlag(arg);
                if (optOpt.isPresent()) {
                    final CliOption opt = optOpt.get();
                    i = handleKnownOption(builder, opt, args, i);
                } else {
                    builder.addUnknownFlag(arg);
                }
                i++;
                continue;
            }

            // Non-flag argument (positional) - treat as unknown
            builder.addUnknownFlag(arg);
            i++;
        }

        return builder.build();
    }

    private static void parsePassthroughSetting(CliParseResult.Builder builder, String kvPair) {
        final int eqIdx = kvPair.indexOf('=');
        if (eqIdx > 0) {
            final String key = kvPair.substring(0, eqIdx);
            final String value = kvPair.substring(eqIdx + 1);
            builder.addPassthroughSetting(key, value);
        }
    }

    private static int handleKnownOption(CliParseResult.Builder builder, CliOption opt, String[] args, int currentIndex) {
        // Check for special flags
        if ("--version".equals(opt.getLongFlag()) || "-V".equals(opt.getLongFlag())) {
            builder.versionRequested(true);
            return currentIndex;
        }
        if ("--help".equals(opt.getLongFlag()) || "-h".equals(opt.getLongFlag())) {
            builder.helpRequested(true);
            return currentIndex;
        }

        if (opt.getType() == CliOption.OptionType.BOOLEAN) {
            return handleBooleanOption(builder, opt, args, currentIndex);
        }

        // Value-taking option
        if (currentIndex + 1 < args.length) {
            final String nextArg = args[currentIndex + 1];
            if (!nextArg.startsWith("-") || isNumeric(nextArg)) {
                return handleFlagWithValue(builder, opt.getLongFlag(), nextArg, args, currentIndex, true);
            }
        }

        // No value provided for a value-taking option; set empty
        if (opt.getSettingKey() != null) {
            builder.setValue(opt.getSettingKey(), "");
        }
        return currentIndex;
    }

    private static int handleBooleanOption(CliParseResult.Builder builder, CliOption opt, String[] args, int currentIndex) {
        // Check if next argument is an explicit boolean value
        if (currentIndex + 1 < args.length) {
            final String nextArg = args[currentIndex + 1];
            if ("true".equalsIgnoreCase(nextArg) || "false".equalsIgnoreCase(nextArg)) {
                if (opt.getSettingKey() != null) {
                    builder.setValue(opt.getSettingKey(), Boolean.parseBoolean(nextArg));
                }
                return currentIndex + 1;
            }
        }

        // No explicit value means true (flag-style)
        if (opt.getSettingKey() != null) {
            builder.setValue(opt.getSettingKey(), true);
        }
        return currentIndex;
    }

    /**
     * Handles storing a flag value. If consumeNext is true, increments the index past the value argument.
     */
    private static int handleFlagWithValue(CliParseResult.Builder builder, String flag, String value,
                                           String[] args, int currentIndex, boolean consumeNext) {
        final Optional<CliOption> optOpt = CliOptionRegistry.findByFlag(flag);
        if (!optOpt.isPresent()) {
            builder.addUnknownFlag(flag);
            return consumeNext ? currentIndex + 1 : currentIndex;
        }

        final CliOption opt = optOpt.get();
        if (opt.getSettingKey() == null) {
            return consumeNext ? currentIndex + 1 : currentIndex;
        }

        switch (opt.getType()) {
            case INTEGER:
                try {
                    builder.setValue(opt.getSettingKey(), Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    builder.setValue(opt.getSettingKey(), value);
                }
                break;
            case BOOLEAN:
                builder.setValue(opt.getSettingKey(), Boolean.parseBoolean(value));
                break;
            case STRING_LIST:
                builder.addToList(opt.getSettingKey(), value);
                break;
            default:
                builder.setValue(opt.getSettingKey(), value);
                break;
        }

        return consumeNext ? currentIndex + 1 : currentIndex;
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        int start = 0;
        if (str.charAt(0) == '-') {
            if (str.length() == 1) {
                return false;
            }
            start = 1;
        }
        for (int i = start; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
