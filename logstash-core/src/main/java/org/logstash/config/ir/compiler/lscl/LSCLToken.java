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

/**
 * Represents a token produced by the LSCL lexer.
 * This class has zero org.jruby.* imports.
 */
public final class LSCLToken {

    public enum Type {
        // Literals
        BAREWORD,
        STRING,
        NUMBER,
        REGEX,

        // Brackets and braces
        LBRACKET,    // [
        RBRACKET,    // ]
        LBRACE,      // {
        RBRACE,      // }
        LPAREN,      // (
        RPAREN,      // )

        // Operators
        ARROW,       // =>
        COMMA,       // ,
        EQ,          // ==
        NEQ,         // !=
        LT,          // <
        GT,          // >
        LTE,         // <=
        GTE,         // >=
        MATCH,       // =~
        NOT_MATCH,   // !~
        BANG,        // !

        // Keywords
        IF,
        ELSE,
        IN,
        NOT,
        AND,
        OR,
        XOR,
        NAND,
        INPUT,
        FILTER,
        OUTPUT,

        // Special
        COMMENT,
        EOF
    }

    private final Type type;
    private final String value;
    private final int line;
    private final int column;

    public LSCLToken(Type type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "LSCLToken{type=" + type + ", value='" + value + "', line=" + line + ", column=" + column + "}";
    }
}
