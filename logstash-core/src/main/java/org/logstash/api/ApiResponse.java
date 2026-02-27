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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java response wrapper for the Logstash web API.
 * Encapsulates an HTTP response with status code, body, and content type.
 *
 * @param <T> the type of the response body
 */
public final class ApiResponse<T> {

    private static final String DEFAULT_CONTENT_TYPE = "application/json";

    private final int statusCode;
    private final T body;
    private final String contentType;

    /**
     * Constructs an ApiResponse with the specified status code, body, and content type.
     *
     * @param statusCode  the HTTP status code
     * @param body        the response body
     * @param contentType the MIME content type
     */
    public ApiResponse(final int statusCode, final T body, final String contentType) {
        this.statusCode = statusCode;
        this.body = body;
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
    }

    /**
     * Constructs an ApiResponse with the specified status code and body, defaulting to JSON content type.
     *
     * @param statusCode the HTTP status code
     * @param body       the response body
     */
    public ApiResponse(final int statusCode, final T body) {
        this(statusCode, body, DEFAULT_CONTENT_TYPE);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public T getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * Returns true if the status code indicates success (2xx).
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Returns true if the status code indicates a client error (4xx).
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Returns true if the status code indicates a server error (5xx).
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Converts this response to a Map suitable for serialization.
     *
     * @return a map with statusCode, body, and contentType entries
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("status_code", statusCode);
        map.put("body", body);
        map.put("content_type", contentType);
        return map;
    }

    // --- Factory methods ---

    /**
     * Creates a 200 OK response with the given body.
     */
    public static <T> ApiResponse<T> ok(final T data) {
        return new ApiResponse<>(200, data);
    }

    /**
     * Creates a 201 Created response with the given body.
     */
    public static <T> ApiResponse<T> created(final T data) {
        return new ApiResponse<>(201, data);
    }

    /**
     * Creates a 404 Not Found response with the given error message.
     */
    public static ApiResponse<Map<String, String>> notFound(final String message) {
        return errorResponse(404, message);
    }

    /**
     * Creates a 400 Bad Request response with the given error message.
     */
    public static ApiResponse<Map<String, String>> badRequest(final String message) {
        return errorResponse(400, message);
    }

    /**
     * Creates a 500 Internal Server Error response with the given error message.
     */
    public static ApiResponse<Map<String, String>> error(final String message) {
        return errorResponse(500, message);
    }

    /**
     * Creates a 408 Request Timeout response with the given error message.
     */
    public static ApiResponse<Map<String, String>> timeout(final String message) {
        return errorResponse(408, message);
    }

    private static ApiResponse<Map<String, String>> errorResponse(final int statusCode, final String message) {
        final Map<String, String> errorBody = new LinkedHashMap<>();
        errorBody.put("error", message);
        return new ApiResponse<>(statusCode, errorBody);
    }

    @Override
    public String toString() {
        return "ApiResponse{statusCode=" + statusCode + ", contentType='" + contentType + "', body=" + body + "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ApiResponse<?> that = (ApiResponse<?>) o;
        return statusCode == that.statusCode
                && Objects.equals(body, that.body)
                && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, body, contentType);
    }
}
