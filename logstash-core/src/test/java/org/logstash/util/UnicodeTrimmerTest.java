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

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UnicodeTrimmerTest {

    @Test
    public void testReturnOriginalWhenFits() {
        String str = "hello";
        assertSame(str, UnicodeTrimmer.trimBytes(str, 10));
        assertSame(str, UnicodeTrimmer.trimBytes(str, 5));
    }

    @Test
    public void testTrimAsciiString() {
        String str = "hello world";
        String result = UnicodeTrimmer.trimBytes(str, 5);
        assertTrue(result.getBytes(StandardCharsets.UTF_8).length <= 5);
        assertEquals("hello", result);
    }

    @Test
    public void testTrimMultiByteCharacters() {
        // Each CJK character is 3 bytes in UTF-8
        String str = "\u4e16\u754c\u4f60\u597d"; // "世界你好" - 12 bytes
        String result = UnicodeTrimmer.trimBytes(str, 6);
        assertTrue(result.getBytes(StandardCharsets.UTF_8).length <= 6);
        assertEquals("\u4e16\u754c", result); // "世界" - 6 bytes
    }

    @Test
    public void testTrimDoesNotSplitMultiByteChar() {
        // Each CJK character is 3 bytes in UTF-8
        String str = "\u4e16\u754c\u4f60"; // "世界你" - 9 bytes
        String result = UnicodeTrimmer.trimBytes(str, 7);
        // Should fit 2 chars (6 bytes) but not split 3rd char
        assertTrue(result.getBytes(StandardCharsets.UTF_8).length <= 7);
        assertEquals("\u4e16\u754c", result); // "世界" - 6 bytes
    }

    @Test
    public void testTrimFourByteCharacters() {
        // Emoji characters are 4 bytes each in UTF-8
        String str = "\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02"; // 3 emoji, 12 bytes
        String result = UnicodeTrimmer.trimBytes(str, 8);
        assertTrue(result.getBytes(StandardCharsets.UTF_8).length <= 8);
        assertEquals("\uD83D\uDE00\uD83D\uDE01", result); // 2 emoji, 8 bytes
    }

    @Test
    public void testTrimMixedContent() {
        String str = "ab\u4e16cd"; // 2 + 3 + 2 = 7 bytes
        String result = UnicodeTrimmer.trimBytes(str, 5);
        assertTrue(result.getBytes(StandardCharsets.UTF_8).length <= 5);
        assertEquals("ab\u4e16", result); // "ab世" = 5 bytes
    }

    @Test
    public void testTrimToZero() {
        String result = UnicodeTrimmer.trimBytes("hello", 0);
        assertEquals("", result);
    }

    @Test
    public void testTrimToOne() {
        String result = UnicodeTrimmer.trimBytes("hello", 1);
        assertTrue(result.getBytes(StandardCharsets.UTF_8).length <= 1);
    }
}
