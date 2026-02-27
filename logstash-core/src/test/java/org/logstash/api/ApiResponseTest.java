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
package org.logstash.api;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ApiResponseTest {

    @Test
    public void testOkFactory() {
        final ApiResponse<String> response = ApiResponse.ok("hello");
        assertEquals(200, response.getStatusCode());
        assertEquals("hello", response.getBody());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.isSuccess());
        assertFalse(response.isClientError());
        assertFalse(response.isServerError());
    }

    @Test
    public void testOkWithMapBody() {
        final Map<String, Integer> data = Map.of("count", 42);
        final ApiResponse<Map<String, Integer>> response = ApiResponse.ok(data);
        assertEquals(200, response.getStatusCode());
        assertEquals(42, response.getBody().get("count").intValue());
    }

    @Test
    public void testCreatedFactory() {
        final ApiResponse<String> response = ApiResponse.created("new-resource");
        assertEquals(201, response.getStatusCode());
        assertEquals("new-resource", response.getBody());
        assertTrue(response.isSuccess());
    }

    @Test
    public void testNotFoundFactory() {
        final ApiResponse<Map<String, String>> response = ApiResponse.notFound("resource not found");
        assertEquals(404, response.getStatusCode());
        assertEquals("resource not found", response.getBody().get("error"));
        assertTrue(response.isClientError());
        assertFalse(response.isSuccess());
        assertFalse(response.isServerError());
    }

    @Test
    public void testBadRequestFactory() {
        final ApiResponse<Map<String, String>> response = ApiResponse.badRequest("invalid input");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid input", response.getBody().get("error"));
        assertTrue(response.isClientError());
    }

    @Test
    public void testErrorFactory() {
        final ApiResponse<Map<String, String>> response = ApiResponse.error("internal error");
        assertEquals(500, response.getStatusCode());
        assertEquals("internal error", response.getBody().get("error"));
        assertTrue(response.isServerError());
        assertFalse(response.isSuccess());
        assertFalse(response.isClientError());
    }

    @Test
    public void testTimeoutFactory() {
        final ApiResponse<Map<String, String>> response = ApiResponse.timeout("request timed out");
        assertEquals(408, response.getStatusCode());
        assertEquals("request timed out", response.getBody().get("error"));
        assertTrue(response.isClientError());
    }

    @Test
    public void testCustomContentType() {
        final ApiResponse<String> response = new ApiResponse<>(200, "<html></html>", "text/html");
        assertEquals("text/html", response.getContentType());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testToMap() {
        final ApiResponse<String> response = ApiResponse.ok("data");
        final Map<String, Object> map = response.toMap();
        assertEquals(200, map.get("status_code"));
        assertEquals("data", map.get("body"));
        assertEquals("application/json", map.get("content_type"));
    }

    @Test
    public void testToMapWithErrorBody() {
        final ApiResponse<Map<String, String>> response = ApiResponse.notFound("not found");
        final Map<String, Object> map = response.toMap();
        assertEquals(404, map.get("status_code"));
        @SuppressWarnings("unchecked")
        final Map<String, String> body = (Map<String, String>) map.get("body");
        assertEquals("not found", body.get("error"));
    }

    @Test
    public void testEquality() {
        final ApiResponse<String> r1 = ApiResponse.ok("test");
        final ApiResponse<String> r2 = ApiResponse.ok("test");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    public void testInequality() {
        final ApiResponse<String> r1 = ApiResponse.ok("test");
        final ApiResponse<String> r2 = ApiResponse.ok("different");
        assertNotEquals(r1, r2);
    }

    @Test
    public void testInequalityStatusCode() {
        final ApiResponse<String> r1 = new ApiResponse<>(200, "test");
        final ApiResponse<String> r2 = new ApiResponse<>(201, "test");
        assertNotEquals(r1, r2);
    }

    @Test
    public void testInequalityContentType() {
        final ApiResponse<String> r1 = new ApiResponse<>(200, "test", "application/json");
        final ApiResponse<String> r2 = new ApiResponse<>(200, "test", "text/plain");
        assertNotEquals(r1, r2);
    }

    @Test
    public void testToString() {
        final ApiResponse<String> response = ApiResponse.ok("hello");
        final String str = response.toString();
        assertTrue(str.contains("200"));
        assertTrue(str.contains("application/json"));
        assertTrue(str.contains("hello"));
    }

    @Test
    public void testNullBody() {
        final ApiResponse<String> response = ApiResponse.ok(null);
        assertEquals(200, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test(expected = NullPointerException.class)
    public void testNullContentTypeThrows() {
        new ApiResponse<>(200, "test", null);
    }

    @Test
    public void testIsSuccessRange() {
        assertTrue(new ApiResponse<>(200, null).isSuccess());
        assertTrue(new ApiResponse<>(201, null).isSuccess());
        assertTrue(new ApiResponse<>(299, null).isSuccess());
        assertFalse(new ApiResponse<>(199, null).isSuccess());
        assertFalse(new ApiResponse<>(300, null).isSuccess());
    }

    @Test
    public void testIsClientErrorRange() {
        assertTrue(new ApiResponse<>(400, null).isClientError());
        assertTrue(new ApiResponse<>(404, null).isClientError());
        assertTrue(new ApiResponse<>(499, null).isClientError());
        assertFalse(new ApiResponse<>(399, null).isClientError());
        assertFalse(new ApiResponse<>(500, null).isClientError());
    }

    @Test
    public void testIsServerErrorRange() {
        assertTrue(new ApiResponse<>(500, null).isServerError());
        assertTrue(new ApiResponse<>(503, null).isServerError());
        assertTrue(new ApiResponse<>(599, null).isServerError());
        assertFalse(new ApiResponse<>(499, null).isServerError());
        assertFalse(new ApiResponse<>(600, null).isServerError());
    }

    @Test
    public void testEqualsSameObject() {
        final ApiResponse<String> r = ApiResponse.ok("test");
        assertEquals(r, r);
    }

    @Test
    public void testEqualsNull() {
        final ApiResponse<String> r = ApiResponse.ok("test");
        assertNotEquals(null, r);
    }

    @Test
    public void testEqualsDifferentType() {
        final ApiResponse<String> r = ApiResponse.ok("test");
        assertNotEquals("not a response", r);
    }
}
