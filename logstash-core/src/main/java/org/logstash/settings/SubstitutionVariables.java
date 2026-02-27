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

import co.elastic.logstash.api.Password;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.logstash.exceptions.ConfigurationException;
import org.logstash.secret.SecretIdentifier;
import org.logstash.secret.store.SecretStore;
import org.logstash.secret.store.SecretStoreExt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Static utility for substitution variable replacement (${VAR} and ${VAR:default}).
 * Replaces the Ruby LogStash::Util::SubstitutionVariables module.
 *
 * Uses a lazy-loaded global SecretStore for resolving secret store variables.
 */
public final class SubstitutionVariables {

    private static final Logger LOGGER = LogManager.getLogger(SubstitutionVariables.class);

    static final Pattern SUBSTITUTION_PLACEHOLDER_REGEX =
            Pattern.compile("\\$\\{(?<name>[a-zA-Z_.][a-zA-Z0-9_.]*)(:(?<default>[^}]*))?}");

    private static volatile SecretStore secretStore;
    private static volatile boolean secretStoreLoaded = false;
    private static volatile Function<String, String> envAccessor = System::getenv;

    private SubstitutionVariables() {
        // utility class
    }

    /**
     * Recursively replace substitution variable references in values.
     *
     * @param value the value to process (Map, List, String, Password, or other)
     * @param refine if true, strip enclosing quotes and parse array strings from resolved values
     * @return the value with all substitution variables replaced
     */
    @SuppressWarnings("unchecked")
    public static Object deepReplace(Object value, boolean refine) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                map.put(entry.getKey(), deepReplace(entry.getValue(), refine));
            }
            return map;
        } else if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, deepReplace(list.get(i), refine));
            }
            return list;
        } else {
            return replacePlaceholders(value, refine);
        }
    }

    /**
     * Convenience overload: deepReplace without refinement.
     */
    public static Object deepReplace(Object value) {
        return deepReplace(value, false);
    }

    /**
     * Replace substitution variable placeholders in a single value.
     * Handles Password objects by unwrapping, replacing, and re-wrapping.
     *
     * @param value the value to process
     * @param refine if true, strip enclosing quotes and parse array strings
     * @return the replaced value
     */
    public static Object replacePlaceholders(Object value, boolean refine) {
        if (value instanceof Password) {
            Password pw = (Password) value;
            Object interpolated = replacePlaceholders(pw.getPassword(), refine);
            return new Password(interpolated.toString());
        }

        if (!(value instanceof String)) {
            return value;
        }

        String strValue = (String) value;
        Matcher matcher = SUBSTITUTION_PLACEHOLDER_REGEX.matcher(strValue);

        boolean placeholderFound = false;
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String name = matcher.group("name");
            String defaultVal = matcher.group("default");

            LOGGER.debug("Replacing `{}` with actual value", matcher.group());
            placeholderFound = true;

            // Check the secret store
            String replacement = null;
            SecretStore store = getSecretStore();
            if (store != null) {
                byte[] secretBytes = store.retrieveSecret(SecretStoreExt.getStoreId(name));
                if (secretBytes != null) {
                    replacement = new String(secretBytes, StandardCharsets.UTF_8);
                }
            }

            // Check environment variables
            if (replacement == null) {
                replacement = envAccessor.apply(name);
                if (replacement == null && defaultVal != null) {
                    replacement = defaultVal;
                }
            }

            if (replacement == null) {
                throw new ConfigurationException(
                        "Cannot evaluate `${" + name + "}`. Replacement variable `" + name +
                        "` is not defined in a Logstash secret store or as an Environment entry and there is no default value given.");
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        String placeholderValue = sb.toString();

        if (!placeholderFound || !refine) {
            return placeholderValue;
        }

        // Refine: strip enclosing quotes and parse arrays
        String refined = stripEnclosingChar(stripEnclosingChar(placeholderValue, "'"), "\"");
        if (refined.startsWith("[") && refined.endsWith("]")) {
            String inner = refined.substring(1, refined.length() - 1);
            List<String> parts = Arrays.stream(inner.split(","))
                    .map(String::trim)
                    .map(s -> stripEnclosingChar(stripEnclosingChar(s, "'"), "\""))
                    .collect(Collectors.toList());
            return parts;
        }
        return refined;
    }

    public static String stripEnclosingChar(String value, String removeChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.startsWith(removeChar) && value.endsWith(removeChar) && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Initialize the secret store from keystore settings.
     * Called during settings initialization when keystore settings become available.
     */
    public static void initSecretStore(String keystorePath, String keystoreClass) {
        synchronized (SubstitutionVariables.class) {
            try {
                secretStore = SecretStoreExt.getIfExists(keystorePath, keystoreClass);
            } catch (Exception e) {
                LOGGER.debug("Could not load secret store", e);
                secretStore = null;
            }
            secretStoreLoaded = true;
        }
    }

    /**
     * Reset the secret store so it will be re-loaded on next access.
     * Useful for testing.
     */
    public static void resetSecretStore() {
        synchronized (SubstitutionVariables.class) {
            secretStore = null;
            secretStoreLoaded = false;
        }
    }

    /**
     * Set the environment variable accessor. By default uses System.getenv().
     * In JRuby, should be set to use Ruby's ENV to ensure runtime ENV changes are visible.
     */
    public static void setEnvironmentAccessor(Function<String, String> accessor) {
        synchronized (SubstitutionVariables.class) {
            envAccessor = accessor;
        }
    }

    /**
     * Set the secret store directly. Useful for testing.
     */
    static void setSecretStore(SecretStore store) {
        synchronized (SubstitutionVariables.class) {
            secretStore = store;
            secretStoreLoaded = true;
        }
    }

    private static SecretStore getSecretStore() {
        if (!secretStoreLoaded) {
            // The store hasn't been initialized yet; return null.
            // The SettingsContainer will call initSecretStore once keystore settings are available.
            return null;
        }
        return secretStore;
    }
}
