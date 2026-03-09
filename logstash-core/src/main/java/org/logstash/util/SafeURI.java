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

package org.logstash.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URI wrapper that masks passwords in {@code toString()} and {@code inspect()} output.
 * <p>
 * Corresponds to Ruby {@code LogStash::Util::SafeURI}.
 * Wraps a {@link java.net.URI} and provides accessors with password masking
 * for safe logging and display.
 * </p>
 */
public class SafeURI {

    public static final String PASS_PLACEHOLDER = "xxxxxx";
    private static final Pattern HOSTNAME_PORT_REGEX =
            Pattern.compile("\\A(?<hostname>([A-Za-z0-9.\\-]+)|\\[[0-9A-Fa-f:]+])(:(\\d+))?\\Z");

    private URI uri;

    public SafeURI(String arg) throws URISyntaxException {
        Matcher m = HOSTNAME_PORT_REGEX.matcher(arg);
        if (m.matches()) {
            arg = "//" + arg;
        }
        this.uri = new URI(arg);
        if (this.uri.getHost() == null) {
            throw new IllegalArgumentException("URI is not valid - host is not specified");
        }
    }

    public SafeURI(URI uri) {
        this.uri = uri;
        if (this.uri.getHost() == null) {
            throw new IllegalArgumentException("URI is not valid - host is not specified");
        }
    }

    /**
     * Factory method that returns the argument if it is already a SafeURI,
     * otherwise constructs a new SafeURI from the object's string representation.
     */
    public static SafeURI from(Object obj) {
        if (obj instanceof SafeURI) {
            return (SafeURI) obj;
        }
        if (obj instanceof URI) {
            return new SafeURI((URI) obj);
        }
        try {
            return new SafeURI(obj.toString());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + obj, e);
        }
    }

    public URI getURI() {
        return uri;
    }

    // JRuby setter methods: maps Ruby `user=` to `setUser()`, etc.
    public void setUser(String newUser) { update("user", newUser); }
    public void setPassword(String newPassword) { update("password", newPassword); }
    public void setHost(String newHost) { update("host", newHost); }
    public void setPort(Integer newPort) { update("port", newPort); }
    public void setPath(String newPath) { update("path", newPath); }
    public void setQuery(String newQuery) { update("query", newQuery); }
    public void setFragment(String newFragment) { update("fragment", newFragment); }

    @Override
    public String toString() {
        return sanitized().toString();
    }

    public String inspect() {
        return sanitized().toString();
    }

    public URI sanitized() {
        if (getPassword() == null) {
            return uri;
        }
        String userInfo = getUser() != null ? getUser() + ":" + PASS_PLACEHOLDER : null;
        return makeURI(getScheme(), userInfo, getHost(), getPort(), getPath(), getQuery(), getFragment());
    }

    @Override
    public int hashCode() {
        return uri.hashCode() * 11;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SafeURI) {
            return this.uri.equals(((SafeURI) other).uri);
        }
        return false;
    }

    @Override
    public SafeURI clone() {
        return new SafeURI(uri);
    }

    public void update(String field, Object value) {
        String newScheme = getScheme();
        String newUser = getUser();
        String newPassword = getPassword();
        String newHost = getHost();
        Integer newPort = getPort();
        String newPath = getPath();
        String newQuery = getQuery();
        String newFragment = getFragment();

        switch (field) {
            case "scheme": newScheme = (String) value; break;
            case "user": newUser = (String) value; break;
            case "password": newPassword = (String) value; break;
            case "host": newHost = (String) value; break;
            case "port": newPort = value == null ? null : ((Number) value).intValue(); break;
            case "path": newPath = (String) value; break;
            case "query": newQuery = (String) value; break;
            case "fragment": newFragment = (String) value; break;
        }

        String userInfo = newUser;
        if (newUser != null && newPassword != null) {
            userInfo = newUser + ":" + newPassword;
        }
        this.uri = makeURI(newScheme, userInfo, newHost, newPort, newPath, newQuery, newFragment);
    }

    public String getUser() {
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            return userInfo.split(":")[0];
        }
        return null;
    }

    public String getPassword() {
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":", 2);
            return parts.length > 1 ? parts[1] : null;
        }
        return null;
    }

    public String getScheme() {
        return uri.getScheme();
    }

    public String getHost() {
        return uri.getHost();
    }

    public String getHostname() {
        return getHost();
    }

    public Integer getPort() {
        int port = uri.getPort();
        return port < 1 ? null : port;
    }

    public String getPath() {
        return uri.getRawPath();
    }

    public String getQuery() {
        return uri.getRawQuery();
    }

    public String getFragment() {
        return uri.getRawFragment();
    }

    public String getUserinfo() {
        return uri.getRawUserInfo();
    }

    public boolean isAbsolute() {
        return uri.isAbsolute();
    }

    public void normalize() {
        if (getPath() != null && getPath().isEmpty()) {
            update("path", "/");
        }
        if (getScheme() != null && !getScheme().equals(getScheme().toLowerCase())) {
            update("scheme", getScheme().toLowerCase());
        }
        if (getHost() != null && !getHost().equals(getHost().toLowerCase())) {
            update("host", getHost().toLowerCase());
        }
    }

    private static URI makeURI(String scheme, String userInfo, String host,
                               Integer port, String path, String query, String fragment) {
        // Match Ruby behavior: any non-null path not starting with "/" gets prefixed
        String prefixedPath = path;
        if (path != null && !path.startsWith("/")) {
            prefixedPath = "/" + path;
        }
        try {
            return new URI(scheme, userInfo, host, port != null ? port : -1,
                    prefixedPath, query, fragment);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI components", e);
        }
    }
}
