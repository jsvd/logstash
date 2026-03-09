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

import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SafeURITest {

    @Test
    public void testHostnamePort() throws URISyntaxException {
        SafeURI uri = new SafeURI("localhost:9200");
        assertEquals("localhost", uri.getHost());
        assertEquals(Integer.valueOf(9200), uri.getPort());
    }

    @Test
    public void testFullURI() throws URISyntaxException {
        SafeURI uri = new SafeURI("http://user:pass@example.com:8080/path?q=1#frag");
        assertEquals("http", uri.getScheme());
        assertEquals("user", uri.getUser());
        assertEquals("pass", uri.getPassword());
        assertEquals("example.com", uri.getHost());
        assertEquals(Integer.valueOf(8080), uri.getPort());
        assertEquals("/path", uri.getPath());
        assertEquals("q=1", uri.getQuery());
        assertEquals("frag", uri.getFragment());
    }

    @Test
    public void testPasswordMasking() throws URISyntaxException {
        SafeURI uri = new SafeURI("http://user:secret@example.com/path");
        String str = uri.toString();
        assertFalse(str.contains("secret"));
        assertTrue(str.contains("xxxxxx"));
        assertTrue(str.contains("user"));
    }

    @Test
    public void testNoPasswordNoMasking() throws URISyntaxException {
        SafeURI uri = new SafeURI("http://example.com/path");
        String str = uri.toString();
        assertEquals("http://example.com/path", str);
    }

    @Test
    public void testEquality() throws URISyntaxException {
        SafeURI uri1 = new SafeURI("http://example.com:9200");
        SafeURI uri2 = new SafeURI("http://example.com:9200");
        assertEquals(uri1, uri2);
        assertEquals(uri1.hashCode(), uri2.hashCode());
    }

    @Test
    public void testUpdate() throws URISyntaxException {
        SafeURI uri = new SafeURI("http://example.com:9200/path");
        uri.update("host", "other.com");
        assertEquals("other.com", uri.getHost());
        uri.update("port", 9300);
        assertEquals(Integer.valueOf(9300), uri.getPort());
    }

    @Test
    public void testNormalize() throws URISyntaxException {
        SafeURI uri = new SafeURI("HTTP://Example.COM:9200");
        uri.normalize();
        assertEquals("http", uri.getScheme());
        assertEquals("example.com", uri.getHost());
    }

    @Test
    public void testClone() throws URISyntaxException {
        SafeURI uri = new SafeURI("http://example.com:9200");
        SafeURI cloned = uri.clone();
        assertEquals(uri, cloned);
    }

    @Test
    public void testPortNullWhenNegative() throws URISyntaxException {
        SafeURI uri = new SafeURI("http://example.com/path");
        assertNull(uri.getPort());
    }

    @Test
    public void testIPv6Host() throws URISyntaxException {
        SafeURI uri = new SafeURI("[::1]:9200");
        assertNotNull(uri.getHost());
        assertEquals(Integer.valueOf(9200), uri.getPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidURINoHost() throws URISyntaxException {
        new SafeURI("mailto:user@example.com");
    }
}
