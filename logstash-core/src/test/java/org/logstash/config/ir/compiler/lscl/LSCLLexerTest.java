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

package org.logstash.config.ir.compiler.lscl;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class LSCLLexerTest {

    @Test
    public void testSimpleConfig() {
        List<LSCLToken> tokens = new LSCLLexer("input { stdin {} }").tokenize();

        assertEquals(LSCLToken.Type.INPUT, tokens.get(0).getType());
        assertEquals(LSCLToken.Type.LBRACE, tokens.get(1).getType());
        assertEquals(LSCLToken.Type.BAREWORD, tokens.get(2).getType());
        assertEquals("stdin", tokens.get(2).getValue());
        assertEquals(LSCLToken.Type.LBRACE, tokens.get(3).getType());
        assertEquals(LSCLToken.Type.RBRACE, tokens.get(4).getType());
        assertEquals(LSCLToken.Type.RBRACE, tokens.get(5).getType());
        assertEquals(LSCLToken.Type.EOF, tokens.get(6).getType());
    }

    @Test
    public void testDoubleQuotedString() {
        List<LSCLToken> tokens = new LSCLLexer("\"hello world\"").tokenize();

        assertEquals(LSCLToken.Type.STRING, tokens.get(0).getType());
        assertEquals("hello world", tokens.get(0).getValue());
    }

    @Test
    public void testSingleQuotedString() {
        List<LSCLToken> tokens = new LSCLLexer("'hello world'").tokenize();

        assertEquals(LSCLToken.Type.STRING, tokens.get(0).getType());
        assertEquals("hello world", tokens.get(0).getValue());
    }

    @Test
    public void testDoubleQuotedStringWithEscape() {
        List<LSCLToken> tokens = new LSCLLexer("\"hello \\\"world\\\"\"").tokenize();

        assertEquals(LSCLToken.Type.STRING, tokens.get(0).getType());
        assertEquals("hello \\\"world\\\"", tokens.get(0).getValue());
    }

    @Test
    public void testSingleQuotedStringWithEscape() {
        List<LSCLToken> tokens = new LSCLLexer("'hello \\'world\\''").tokenize();

        assertEquals(LSCLToken.Type.STRING, tokens.get(0).getType());
        assertEquals("hello \\'world\\'", tokens.get(0).getValue());
    }

    @Test
    public void testIntegerNumber() {
        List<LSCLToken> tokens = new LSCLLexer("42").tokenize();

        assertEquals(LSCLToken.Type.NUMBER, tokens.get(0).getType());
        assertEquals("42", tokens.get(0).getValue());
    }

    @Test
    public void testNegativeNumber() {
        List<LSCLToken> tokens = new LSCLLexer("-7").tokenize();

        assertEquals(LSCLToken.Type.NUMBER, tokens.get(0).getType());
        assertEquals("-7", tokens.get(0).getValue());
    }

    @Test
    public void testFloatNumber() {
        List<LSCLToken> tokens = new LSCLLexer("3.14").tokenize();

        assertEquals(LSCLToken.Type.NUMBER, tokens.get(0).getType());
        assertEquals("3.14", tokens.get(0).getValue());
    }

    @Test
    public void testOperators() {
        List<LSCLToken> tokens = new LSCLLexer("== != =~ !~ < > <= >=").tokenize();

        assertEquals(LSCLToken.Type.EQ, tokens.get(0).getType());
        assertEquals(LSCLToken.Type.NEQ, tokens.get(1).getType());
        assertEquals(LSCLToken.Type.MATCH, tokens.get(2).getType());
        assertEquals(LSCLToken.Type.NOT_MATCH, tokens.get(3).getType());
        assertEquals(LSCLToken.Type.LT, tokens.get(4).getType());
        assertEquals(LSCLToken.Type.GT, tokens.get(5).getType());
        assertEquals(LSCLToken.Type.LTE, tokens.get(6).getType());
        assertEquals(LSCLToken.Type.GTE, tokens.get(7).getType());
        assertEquals(LSCLToken.Type.EOF, tokens.get(8).getType());
    }

    @Test
    public void testFieldSelectors() {
        List<LSCLToken> tokens = new LSCLLexer("[message]").tokenize();

        assertEquals(LSCLToken.Type.LBRACKET, tokens.get(0).getType());
        assertEquals(LSCLToken.Type.BAREWORD, tokens.get(1).getType());
        assertEquals("message", tokens.get(1).getValue());
        assertEquals(LSCLToken.Type.RBRACKET, tokens.get(2).getType());
    }

    @Test
    public void testNestedFieldSelectors() {
        List<LSCLToken> tokens = new LSCLLexer("[request][headers]").tokenize();

        assertEquals(LSCLToken.Type.LBRACKET, tokens.get(0).getType());
        assertEquals("request", tokens.get(1).getValue());
        assertEquals(LSCLToken.Type.RBRACKET, tokens.get(2).getType());
        assertEquals(LSCLToken.Type.LBRACKET, tokens.get(3).getType());
        assertEquals("headers", tokens.get(4).getValue());
        assertEquals(LSCLToken.Type.RBRACKET, tokens.get(5).getType());
    }

    @Test
    public void testCommentsAreSkipped() {
        List<LSCLToken> tokens = new LSCLLexer("# this is a comment\ninput").tokenize();

        assertEquals(LSCLToken.Type.INPUT, tokens.get(0).getType());
        assertEquals(LSCLToken.Type.EOF, tokens.get(1).getType());
    }

    @Test
    public void testInlineCommentSkipped() {
        List<LSCLToken> tokens = new LSCLLexer("input # inline comment\n{ }").tokenize();

        assertEquals(LSCLToken.Type.INPUT, tokens.get(0).getType());
        assertEquals(LSCLToken.Type.LBRACE, tokens.get(1).getType());
        assertEquals(LSCLToken.Type.RBRACE, tokens.get(2).getType());
    }

    @Test
    public void testArrowOperator() {
        List<LSCLToken> tokens = new LSCLLexer("path => \"/var/log\"").tokenize();

        assertEquals(LSCLToken.Type.BAREWORD, tokens.get(0).getType());
        assertEquals("path", tokens.get(0).getValue());
        assertEquals(LSCLToken.Type.ARROW, tokens.get(1).getType());
        assertEquals(LSCLToken.Type.STRING, tokens.get(2).getType());
        assertEquals("/var/log", tokens.get(2).getValue());
    }

    @Test
    public void testKeywords() {
        List<LSCLToken> tokens = new LSCLLexer("if else in not and or xor nand input filter output").tokenize();

        assertEquals(LSCLToken.Type.IF, tokens.get(0).getType());
        assertEquals(LSCLToken.Type.ELSE, tokens.get(1).getType());
        assertEquals(LSCLToken.Type.IN, tokens.get(2).getType());
        assertEquals(LSCLToken.Type.NOT, tokens.get(3).getType());
        assertEquals(LSCLToken.Type.AND, tokens.get(4).getType());
        assertEquals(LSCLToken.Type.OR, tokens.get(5).getType());
        assertEquals(LSCLToken.Type.XOR, tokens.get(6).getType());
        assertEquals(LSCLToken.Type.NAND, tokens.get(7).getType());
        assertEquals(LSCLToken.Type.INPUT, tokens.get(8).getType());
        assertEquals(LSCLToken.Type.FILTER, tokens.get(9).getType());
        assertEquals(LSCLToken.Type.OUTPUT, tokens.get(10).getType());
    }

    @Test
    public void testBangOperator() {
        List<LSCLToken> tokens = new LSCLLexer("!").tokenize();

        assertEquals(LSCLToken.Type.BANG, tokens.get(0).getType());
    }

    @Test
    public void testRegex() {
        List<LSCLToken> tokens = new LSCLLexer("/error/").tokenize();

        assertEquals(LSCLToken.Type.REGEX, tokens.get(0).getType());
        assertEquals("error", tokens.get(0).getValue());
    }

    @Test
    public void testRegexWithEscapedSlash() {
        List<LSCLToken> tokens = new LSCLLexer("/path\\/to\\/file/").tokenize();

        assertEquals(LSCLToken.Type.REGEX, tokens.get(0).getType());
        assertEquals("path\\/to\\/file", tokens.get(0).getValue());
    }

    @Test
    public void testComma() {
        List<LSCLToken> tokens = new LSCLLexer(",").tokenize();

        assertEquals(LSCLToken.Type.COMMA, tokens.get(0).getType());
    }

    @Test
    public void testLineAndColumnTracking() {
        List<LSCLToken> tokens = new LSCLLexer("input\n  {\n  }").tokenize();

        // 'input' is at line 1, column 1
        assertEquals(1, tokens.get(0).getLine());
        assertEquals(1, tokens.get(0).getColumn());
        // '{' is at line 2, column 3
        assertEquals(2, tokens.get(1).getLine());
        assertEquals(3, tokens.get(1).getColumn());
        // '}' is at line 3, column 3
        assertEquals(3, tokens.get(2).getLine());
        assertEquals(3, tokens.get(2).getColumn());
    }

    @Test
    public void testBarewordWithHyphen() {
        List<LSCLToken> tokens = new LSCLLexer("my-plugin").tokenize();

        assertEquals(LSCLToken.Type.BAREWORD, tokens.get(0).getType());
        assertEquals("my-plugin", tokens.get(0).getValue());
    }

    @Test
    public void testBarewordWithUnderscore() {
        List<LSCLToken> tokens = new LSCLLexer("my_plugin").tokenize();

        assertEquals(LSCLToken.Type.BAREWORD, tokens.get(0).getType());
        assertEquals("my_plugin", tokens.get(0).getValue());
    }

    @Test
    public void testParentheses() {
        List<LSCLToken> tokens = new LSCLLexer("( )").tokenize();

        assertEquals(LSCLToken.Type.LPAREN, tokens.get(0).getType());
        assertEquals(LSCLToken.Type.RPAREN, tokens.get(1).getType());
    }

    @Test
    public void testEmptyInput() {
        List<LSCLToken> tokens = new LSCLLexer("").tokenize();

        assertEquals(1, tokens.size());
        assertEquals(LSCLToken.Type.EOF, tokens.get(0).getType());
    }

    @Test
    public void testWhitespaceOnlyInput() {
        List<LSCLToken> tokens = new LSCLLexer("   \t\n  ").tokenize();

        assertEquals(1, tokens.size());
        assertEquals(LSCLToken.Type.EOF, tokens.get(0).getType());
    }

    @Test(expected = LSCLParseException.class)
    public void testUnterminatedString() {
        new LSCLLexer("\"unterminated").tokenize();
    }

    @Test(expected = LSCLParseException.class)
    public void testUnterminatedRegex() {
        new LSCLLexer("/unterminated").tokenize();
    }

    @Test
    public void testFullConfigTokenization() {
        String config = "input {\n" +
                "  file {\n" +
                "    path => \"/var/log/syslog\"\n" +
                "    type => \"syslog\"\n" +
                "  }\n" +
                "}\n";
        List<LSCLToken> tokens = new LSCLLexer(config).tokenize();

        // input { file { path => "/var/log/syslog" type => "syslog" } }
        assertEquals(LSCLToken.Type.INPUT, tokens.get(0).getType());
        assertEquals(LSCLToken.Type.LBRACE, tokens.get(1).getType());
        assertEquals(LSCLToken.Type.BAREWORD, tokens.get(2).getType());
        assertEquals("file", tokens.get(2).getValue());
        assertEquals(LSCLToken.Type.LBRACE, tokens.get(3).getType());
        assertEquals(LSCLToken.Type.BAREWORD, tokens.get(4).getType());
        assertEquals("path", tokens.get(4).getValue());
        assertEquals(LSCLToken.Type.ARROW, tokens.get(5).getType());
        assertEquals(LSCLToken.Type.STRING, tokens.get(6).getType());
        assertEquals("/var/log/syslog", tokens.get(6).getValue());
        assertEquals(LSCLToken.Type.BAREWORD, tokens.get(7).getType());
        assertEquals("type", tokens.get(7).getValue());
        assertEquals(LSCLToken.Type.ARROW, tokens.get(8).getType());
        assertEquals(LSCLToken.Type.STRING, tokens.get(9).getType());
        assertEquals("syslog", tokens.get(9).getValue());
    }
}
