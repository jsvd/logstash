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

import static org.junit.Assert.*;

public class ApiErrorTest {

    @Test
    public void testBaseApiError() {
        final ApiError error = new ApiError(503, "service unavailable");
        assertEquals(503, error.getStatusCode());
        assertEquals("service unavailable", error.getMessage());
        assertNull(error.getCause());
    }

    @Test
    public void testBaseApiErrorWithCause() {
        final Exception cause = new RuntimeException("root cause");
        final ApiError error = new ApiError(500, "internal error", cause);
        assertEquals(500, error.getStatusCode());
        assertEquals("internal error", error.getMessage());
        assertSame(cause, error.getCause());
    }

    @Test
    public void testApiErrorIsRuntimeException() {
        final ApiError error = new ApiError(500, "test");
        assertTrue(error instanceof RuntimeException);
    }

    // --- NotFoundError ---

    @Test
    public void testNotFoundError() {
        final ApiError.NotFoundError error = new ApiError.NotFoundError("resource missing");
        assertEquals(404, error.getStatusCode());
        assertEquals("resource missing", error.getMessage());
        assertTrue(error instanceof ApiError);
    }

    @Test
    public void testNotFoundErrorWithCause() {
        final Exception cause = new IllegalStateException("gone");
        final ApiError.NotFoundError error = new ApiError.NotFoundError("not found", cause);
        assertEquals(404, error.getStatusCode());
        assertEquals("not found", error.getMessage());
        assertSame(cause, error.getCause());
    }

    @Test
    public void testNotFoundCanBeCaughtAsApiError() {
        boolean caught = false;
        try {
            throw new ApiError.NotFoundError("missing");
        } catch (final ApiError e) {
            assertEquals(404, e.getStatusCode());
            caught = true;
        }
        assertTrue("Should have caught NotFoundError as ApiError", caught);
    }

    // --- BadRequestError ---

    @Test
    public void testBadRequestError() {
        final ApiError.BadRequestError error = new ApiError.BadRequestError("invalid parameter");
        assertEquals(400, error.getStatusCode());
        assertEquals("invalid parameter", error.getMessage());
        assertTrue(error instanceof ApiError);
    }

    @Test
    public void testBadRequestErrorWithCause() {
        final Exception cause = new IllegalArgumentException("bad arg");
        final ApiError.BadRequestError error = new ApiError.BadRequestError("bad request", cause);
        assertEquals(400, error.getStatusCode());
        assertSame(cause, error.getCause());
    }

    @Test
    public void testBadRequestCanBeCaughtAsApiError() {
        boolean caught = false;
        try {
            throw new ApiError.BadRequestError("bad");
        } catch (final ApiError e) {
            assertEquals(400, e.getStatusCode());
            caught = true;
        }
        assertTrue("Should have caught BadRequestError as ApiError", caught);
    }

    // --- RequestTimeoutError ---

    @Test
    public void testRequestTimeoutError() {
        final ApiError.RequestTimeoutError error = new ApiError.RequestTimeoutError("timed out");
        assertEquals(408, error.getStatusCode());
        assertEquals("timed out", error.getMessage());
        assertTrue(error instanceof ApiError);
    }

    @Test
    public void testRequestTimeoutErrorWithCause() {
        final Exception cause = new InterruptedException("interrupted");
        final ApiError.RequestTimeoutError error = new ApiError.RequestTimeoutError("timeout", cause);
        assertEquals(408, error.getStatusCode());
        assertSame(cause, error.getCause());
    }

    @Test
    public void testRequestTimeoutCanBeCaughtAsApiError() {
        boolean caught = false;
        try {
            throw new ApiError.RequestTimeoutError("timeout");
        } catch (final ApiError e) {
            assertEquals(408, e.getStatusCode());
            caught = true;
        }
        assertTrue("Should have caught RequestTimeoutError as ApiError", caught);
    }

    // --- Error hierarchy ---

    @Test
    public void testErrorHierarchy() {
        final ApiError.NotFoundError notFound = new ApiError.NotFoundError("nf");
        final ApiError.BadRequestError badReq = new ApiError.BadRequestError("br");
        final ApiError.RequestTimeoutError timeout = new ApiError.RequestTimeoutError("to");

        // All are ApiError
        assertTrue(notFound instanceof ApiError);
        assertTrue(badReq instanceof ApiError);
        assertTrue(timeout instanceof ApiError);

        // All are RuntimeException
        assertTrue(notFound instanceof RuntimeException);
        assertTrue(badReq instanceof RuntimeException);
        assertTrue(timeout instanceof RuntimeException);

        // Each has distinct status codes
        assertEquals(404, notFound.getStatusCode());
        assertEquals(400, badReq.getStatusCode());
        assertEquals(408, timeout.getStatusCode());
    }

    @Test
    public void testCatchAllApiErrors() {
        int caught = 0;
        try {
            throw new ApiError.NotFoundError("test");
        } catch (final ApiError e) {
            caught++;
        }
        try {
            throw new ApiError.BadRequestError("test");
        } catch (final ApiError e) {
            caught++;
        }
        try {
            throw new ApiError.RequestTimeoutError("test");
        } catch (final ApiError e) {
            caught++;
        }
        assertEquals(3, caught);
    }
}
