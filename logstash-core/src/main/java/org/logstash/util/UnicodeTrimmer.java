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

import java.nio.charset.StandardCharsets;

/**
 * UTF-8 aware string truncation that respects multi-byte character boundaries.
 * <p>
 * Corresponds to Ruby {@code LogStash::Util::UnicodeTrimmer}.
 * Truncates a UTF-8 string to fit within a maximum number of bytes while
 * preserving as much data as possible and never splitting multi-byte characters.
 * </p>
 */
public final class UnicodeTrimmer {

    private static final int MAX_CHAR_BYTES = 4;

    private UnicodeTrimmer() {}

    /**
     * Truncates a string so its UTF-8 byte representation fits within {@code desiredBytes}.
     * Returns the original string unchanged if it already fits.
     *
     * @param origStr      the original string to truncate
     * @param desiredBytes the maximum number of UTF-8 bytes
     * @return the truncated string
     */
    public static String trimBytes(String origStr, int desiredBytes) {
        byte[] origBytes = origStr.getBytes(StandardCharsets.UTF_8);
        if (origBytes.length <= desiredBytes) {
            return origStr;
        }

        String preShortened = preShorten(origStr, origBytes.length, desiredBytes);
        int preShortenedByteSize = preShortened.getBytes(StandardCharsets.UTF_8).length;

        if (preShortenedByteSize == desiredBytes) {
            return preShortened;
        } else if (preShortenedByteSize > desiredBytes) {
            return shrinkBytes(preShortened, desiredBytes);
        } else {
            return growBytes(preShortened, origStr, desiredBytes);
        }
    }

    /**
     * Estimates a truncation point based on average character byte size.
     */
    private static String preShorten(String origStr, int origByteSize, int desiredBytes) {
        int origLen = origStr.length();
        double avgSize = (double) origByteSize / origLen;
        int origExtraBytes = origByteSize - desiredBytes;
        int preShortenBy = (int) (origExtraBytes / avgSize);
        int newLen = Math.max(0, origLen - preShortenBy);
        return origStr.substring(0, newLen);
    }

    /**
     * Grows a pre-shortened string by appending characters from the original,
     * stopping before exceeding the byte limit.
     */
    private static String growBytes(String preShortened, String origStr, int desiredBytes) {
        StringBuilder result = new StringBuilder(preShortened);

        while (true) {
            int currentByteSize = result.toString().getBytes(StandardCharsets.UTF_8).length;
            int deficit = desiredBytes - currentByteSize;
            int lengthenBy = Math.max(1, deficit / MAX_CHAR_BYTES);
            int startIdx = result.length();
            int endIdx = Math.min(startIdx + lengthenBy, origStr.length());

            if (startIdx >= origStr.length()) {
                break;
            }

            String append = origStr.substring(startIdx, endIdx);
            int appendByteSize = append.getBytes(StandardCharsets.UTF_8).length;

            if (currentByteSize + appendByteSize > desiredBytes) {
                break;
            }

            result.append(append);
        }

        return result.toString();
    }

    /**
     * Shrinks a string by removing trailing characters until it fits within the byte limit.
     */
    private static String shrinkBytes(String preShortened, int desiredBytes) {
        String result = preShortened;

        while (true) {
            int currentByteSize = result.getBytes(StandardCharsets.UTF_8).length;
            if (currentByteSize <= desiredBytes) {
                break;
            }

            int extra = currentByteSize - desiredBytes;
            int shortenBy = Math.max(1, extra / MAX_CHAR_BYTES);
            int newLen = Math.max(0, result.length() - shortenBy);
            result = result.substring(0, newLen);
        }

        return result;
    }
}
