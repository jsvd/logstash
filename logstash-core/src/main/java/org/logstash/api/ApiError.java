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

/**
 * Base class for API errors in the Logstash web API.
 * Each error carries an HTTP status code and a descriptive message.
 */
public class ApiError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    /**
     * Constructs an ApiError with the given status code and message.
     *
     * @param statusCode the HTTP status code
     * @param message    the error message
     */
    public ApiError(final int statusCode, final String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs an ApiError with the given status code, message, and cause.
     *
     * @param statusCode the HTTP status code
     * @param message    the error message
     * @param cause      the underlying cause
     */
    public ApiError(final int statusCode, final String message, final Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code associated with this error.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * 404 Not Found error.
     */
    public static class NotFoundError extends ApiError {
        private static final long serialVersionUID = 1L;

        public NotFoundError(final String message) {
            super(404, message);
        }

        public NotFoundError(final String message, final Throwable cause) {
            super(404, message, cause);
        }
    }

    /**
     * 400 Bad Request error.
     */
    public static class BadRequestError extends ApiError {
        private static final long serialVersionUID = 1L;

        public BadRequestError(final String message) {
            super(400, message);
        }

        public BadRequestError(final String message, final Throwable cause) {
            super(400, message, cause);
        }
    }

    /**
     * 408 Request Timeout error.
     */
    public static class RequestTimeoutError extends ApiError {
        private static final long serialVersionUID = 1L;

        public RequestTimeoutError(final String message) {
            super(408, message);
        }

        public RequestTimeoutError(final String message, final Throwable cause) {
            super(408, message, cause);
        }
    }
}
