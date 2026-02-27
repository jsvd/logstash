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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.logstash.common.SourceWithMetadata;
import org.logstash.config.ir.DSL;
import org.logstash.config.ir.InvalidIRException;
import org.logstash.config.ir.PluginDefinition;
import org.logstash.config.ir.imperative.Statement;

/**
 * Java-based compiler for the Logstash Configuration Language (LSCL).
 * Replaces the Ruby Treetop-based compiler ({@code LogStash::Compiler}).
 *
 * <p>Entry point: {@link #compile(String, SourceWithMetadata, boolean)} returns a
 * {@code Map<PluginDefinition.Type, Statement>} with keys INPUT, FILTER, OUTPUT,
 * identical in structure to what the Ruby compiler produces.
 */
public final class LSCLCompiler {

    private LSCLCompiler() {
        // Utility class
    }

    /**
     * Compiles LSCL configuration text into a map of imperative IR statements.
     *
     * @param configText       the LSCL configuration source text
     * @param source           source metadata (protocol, id, etc.)
     * @param supportEscapes   whether to process escape sequences in strings
     * @return map from section type (INPUT, FILTER, OUTPUT) to the compiled Statement
     * @throws InvalidIRException if the IR cannot be constructed
     * @throws LSCLParseException if the configuration text is syntactically invalid
     */
    public static Map<PluginDefinition.Type, Statement> compile(
            String configText,
            SourceWithMetadata source,
            boolean supportEscapes) throws InvalidIRException {

        String protocol = source != null ? source.getProtocol() : "config_ast";
        String id = source != null ? source.getId() : "config_ast";

        // Lex
        LSCLLexer lexer = new LSCLLexer(configText);
        List<LSCLToken> tokens = lexer.tokenize();

        // Parse
        LSCLParser parser = new LSCLParser(tokens, protocol, id, supportEscapes);
        List<LSCLParser.ParsedSection> sections = parser.parse();

        // Group statements by section type
        Map<PluginDefinition.Type, List<Statement>> sectionMap = new EnumMap<>(PluginDefinition.Type.class);
        sectionMap.put(PluginDefinition.Type.INPUT, new ArrayList<>());
        sectionMap.put(PluginDefinition.Type.FILTER, new ArrayList<>());
        sectionMap.put(PluginDefinition.Type.OUTPUT, new ArrayList<>());

        for (LSCLParser.ParsedSection section : sections) {
            List<Statement> list = sectionMap.get(section.getType());
            if (list == null) {
                throw new InvalidIRException("Unknown section type: " + section.getType());
            }
            list.addAll(section.getStatements());
        }

        // Compose statements per section
        Map<PluginDefinition.Type, Statement> result = new EnumMap<>(PluginDefinition.Type.class);
        result.put(PluginDefinition.Type.INPUT, composeForSection(PluginDefinition.Type.INPUT, sectionMap.get(PluginDefinition.Type.INPUT)));
        result.put(PluginDefinition.Type.FILTER, composeForSection(PluginDefinition.Type.FILTER, sectionMap.get(PluginDefinition.Type.FILTER)));
        result.put(PluginDefinition.Type.OUTPUT, composeForSection(PluginDefinition.Type.OUTPUT, sectionMap.get(PluginDefinition.Type.OUTPUT)));

        return result;
    }

    /**
     * Composes a list of statements for a section using the appropriate composition strategy:
     * - Filters use sequence composition (order matters)
     * - Inputs and outputs use parallel composition (order doesn't matter)
     */
    private static Statement composeForSection(PluginDefinition.Type type, List<Statement> statements) throws InvalidIRException {
        Statement[] stmtArray = statements.toArray(new Statement[0]);
        if (type == PluginDefinition.Type.FILTER) {
            return DSL.iComposeSequence(null, stmtArray);
        } else {
            return DSL.iComposeParallel(null, stmtArray);
        }
    }
}
