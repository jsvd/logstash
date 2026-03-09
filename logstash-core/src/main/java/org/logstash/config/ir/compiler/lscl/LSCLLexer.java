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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lexer for the Logstash Configuration Language (LSCL).
 * Tokenizes LSCL config text into a list of {@link LSCLToken}s.
 * This class has zero org.jruby.* imports.
 */
public final class LSCLLexer {

    private static final Map<String, LSCLToken.Type> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("if", LSCLToken.Type.IF);
        KEYWORDS.put("else", LSCLToken.Type.ELSE);
        KEYWORDS.put("in", LSCLToken.Type.IN);
        KEYWORDS.put("not", LSCLToken.Type.NOT);
        KEYWORDS.put("and", LSCLToken.Type.AND);
        KEYWORDS.put("or", LSCLToken.Type.OR);
        KEYWORDS.put("xor", LSCLToken.Type.XOR);
        KEYWORDS.put("nand", LSCLToken.Type.NAND);
        KEYWORDS.put("input", LSCLToken.Type.INPUT);
        KEYWORDS.put("filter", LSCLToken.Type.FILTER);
        KEYWORDS.put("output", LSCLToken.Type.OUTPUT);
    }

    private final String input;
    private int pos;
    private int line;
    private int column;

    public LSCLLexer(String input) {
        this.input = input;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
    }

    /**
     * Tokenizes the entire input into a list of tokens.
     * Comments and whitespace are consumed but not emitted as tokens.
     *
     * @return list of tokens ending with EOF
     */
    public List<LSCLToken> tokenize() {
        List<LSCLToken> tokens = new ArrayList<>();
        while (pos < input.length()) {
            skipWhitespaceAndComments();
            if (pos >= input.length()) {
                break;
            }
            LSCLToken token = nextToken();
            if (token != null) {
                tokens.add(token);
            }
        }
        tokens.add(new LSCLToken(LSCLToken.Type.EOF, "", line, column));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                advance();
            } else if (c == '#') {
                // Skip comment until end of line
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    advance();
                }
            } else {
                break;
            }
        }
    }

    private LSCLToken nextToken() {
        if (pos >= input.length()) {
            return new LSCLToken(LSCLToken.Type.EOF, "", line, column);
        }

        char c = input.charAt(pos);
        int startLine = line;
        int startColumn = column;

        // Two-character operators
        if (pos + 1 < input.length()) {
            String two = input.substring(pos, pos + 2);
            switch (two) {
                case "=>":
                    advance();
                    advance();
                    return new LSCLToken(LSCLToken.Type.ARROW, "=>", startLine, startColumn);
                case "==":
                    advance();
                    advance();
                    return new LSCLToken(LSCLToken.Type.EQ, "==", startLine, startColumn);
                case "!=":
                    advance();
                    advance();
                    return new LSCLToken(LSCLToken.Type.NEQ, "!=", startLine, startColumn);
                case "<=":
                    advance();
                    advance();
                    return new LSCLToken(LSCLToken.Type.LTE, "<=", startLine, startColumn);
                case ">=":
                    advance();
                    advance();
                    return new LSCLToken(LSCLToken.Type.GTE, ">=", startLine, startColumn);
                case "=~":
                    advance();
                    advance();
                    return new LSCLToken(LSCLToken.Type.MATCH, "=~", startLine, startColumn);
                case "!~":
                    advance();
                    advance();
                    return new LSCLToken(LSCLToken.Type.NOT_MATCH, "!~", startLine, startColumn);
            }
        }

        // Single-character tokens
        switch (c) {
            case '{':
                advance();
                return new LSCLToken(LSCLToken.Type.LBRACE, "{", startLine, startColumn);
            case '}':
                advance();
                return new LSCLToken(LSCLToken.Type.RBRACE, "}", startLine, startColumn);
            case '[':
                advance();
                return new LSCLToken(LSCLToken.Type.LBRACKET, "[", startLine, startColumn);
            case ']':
                advance();
                return new LSCLToken(LSCLToken.Type.RBRACKET, "]", startLine, startColumn);
            case '(':
                advance();
                return new LSCLToken(LSCLToken.Type.LPAREN, "(", startLine, startColumn);
            case ')':
                advance();
                return new LSCLToken(LSCLToken.Type.RPAREN, ")", startLine, startColumn);
            case ',':
                advance();
                return new LSCLToken(LSCLToken.Type.COMMA, ",", startLine, startColumn);
            case '<':
                advance();
                return new LSCLToken(LSCLToken.Type.LT, "<", startLine, startColumn);
            case '>':
                advance();
                return new LSCLToken(LSCLToken.Type.GT, ">", startLine, startColumn);
            case '!':
                advance();
                return new LSCLToken(LSCLToken.Type.BANG, "!", startLine, startColumn);
        }

        // Strings
        if (c == '"') {
            return readDoubleQuotedString(startLine, startColumn);
        }
        if (c == '\'') {
            return readSingleQuotedString(startLine, startColumn);
        }

        // Regex
        if (c == '/') {
            return readRegex(startLine, startColumn);
        }

        // Numbers (including negative)
        if (c == '-' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1))) {
            return readNumber(startLine, startColumn);
        }
        if (Character.isDigit(c)) {
            return readNumber(startLine, startColumn);
        }

        // Barewords and keywords
        if (isIdentifierStart(c)) {
            return readBareword(startLine, startColumn);
        }

        throw new LSCLParseException("Unexpected character '" + c + "'", startLine, startColumn);
    }

    private LSCLToken readDoubleQuotedString(int startLine, int startColumn) {
        advance(); // consume opening "
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\' && pos + 1 < input.length()) {
                // Include the escape sequence as-is (will be processed later if escape support is on)
                sb.append(c);
                advance();
                sb.append(input.charAt(pos));
                advance();
            } else if (c == '"') {
                advance(); // consume closing "
                return new LSCLToken(LSCLToken.Type.STRING, sb.toString(), startLine, startColumn);
            } else {
                sb.append(c);
                advance();
            }
        }
        throw new LSCLParseException("Unterminated double-quoted string", startLine, startColumn);
    }

    private LSCLToken readSingleQuotedString(int startLine, int startColumn) {
        advance(); // consume opening '
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\' && pos + 1 < input.length() && input.charAt(pos + 1) == '\'') {
                // Escaped single quote
                sb.append(c);
                advance();
                sb.append(input.charAt(pos));
                advance();
            } else if (c == '\'') {
                advance(); // consume closing '
                return new LSCLToken(LSCLToken.Type.STRING, sb.toString(), startLine, startColumn);
            } else {
                sb.append(c);
                advance();
            }
        }
        throw new LSCLParseException("Unterminated single-quoted string", startLine, startColumn);
    }

    private LSCLToken readRegex(int startLine, int startColumn) {
        advance(); // consume opening /
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\' && pos + 1 < input.length() && input.charAt(pos + 1) == '/') {
                // Escaped forward slash
                sb.append(c);
                advance();
                sb.append(input.charAt(pos));
                advance();
            } else if (c == '/') {
                advance(); // consume closing /
                return new LSCLToken(LSCLToken.Type.REGEX, sb.toString(), startLine, startColumn);
            } else {
                sb.append(c);
                advance();
            }
        }
        throw new LSCLParseException("Unterminated regex", startLine, startColumn);
    }

    private LSCLToken readNumber(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        if (input.charAt(pos) == '-') {
            sb.append('-');
            advance();
        }
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            sb.append(input.charAt(pos));
            advance();
        }
        // Check for decimal point
        if (pos < input.length() && input.charAt(pos) == '.') {
            sb.append('.');
            advance();
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                sb.append(input.charAt(pos));
                advance();
            }
        }
        return new LSCLToken(LSCLToken.Type.NUMBER, sb.toString(), startLine, startColumn);
    }

    private LSCLToken readBareword(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && isIdentifierChar(input.charAt(pos))) {
            sb.append(input.charAt(pos));
            advance();
        }
        String word = sb.toString();

        // Check if this is a keyword
        LSCLToken.Type keywordType = KEYWORDS.get(word);
        if (keywordType != null) {
            return new LSCLToken(keywordType, word, startLine, startColumn);
        }

        return new LSCLToken(LSCLToken.Type.BAREWORD, word, startLine, startColumn);
    }

    private void advance() {
        if (pos < input.length()) {
            if (input.charAt(pos) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            pos++;
        }
    }

    private static boolean isIdentifierStart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_' || c == '@';
    }

    private static boolean isIdentifierChar(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9') || c == '-';
    }
}
