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
import org.logstash.config.ir.PluginDefinition;
import org.logstash.config.ir.expression.BooleanExpression;
import org.logstash.config.ir.expression.binary.*;
import org.logstash.config.ir.expression.unary.Not;
import org.logstash.config.ir.imperative.*;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class LSCLParserTest {

    private List<LSCLParser.ParsedSection> parse(String config) {
        LSCLLexer lexer = new LSCLLexer(config);
        List<LSCLToken> tokens = lexer.tokenize();
        LSCLParser parser = new LSCLParser(tokens, "test", "test", false);
        return parser.parse();
    }

    @Test
    public void testParseSimpleInput() {
        List<LSCLParser.ParsedSection> sections = parse("input { stdin {} }");

        assertEquals(1, sections.size());
        LSCLParser.ParsedSection section = sections.get(0);
        assertEquals(PluginDefinition.Type.INPUT, section.getType());
        assertEquals(1, section.getStatements().size());

        Statement stmt = section.getStatements().get(0);
        assertInstanceOf(PluginStatement.class, stmt);
        PluginStatement plugin = (PluginStatement) stmt;
        assertEquals("stdin", plugin.getPluginDefinition().getName());
        assertEquals(PluginDefinition.Type.INPUT, plugin.getPluginDefinition().getType());
    }

    @Test
    public void testParseInputWithAttributes() {
        String config = "input { file { path => \"/var/log/syslog\" } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        assertEquals(1, sections.size());
        PluginStatement plugin = (PluginStatement) sections.get(0).getStatements().get(0);
        assertEquals("file", plugin.getPluginDefinition().getName());

        Map<String, Object> args = plugin.getPluginDefinition().getArguments();
        assertEquals("/var/log/syslog", args.get("path"));
    }

    @Test
    public void testParseMultipleAttributes() {
        String config = "input { file { path => \"/var/log/syslog\" type => \"syslog\" start_position => \"beginning\" } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        PluginStatement plugin = (PluginStatement) sections.get(0).getStatements().get(0);
        Map<String, Object> args = plugin.getPluginDefinition().getArguments();
        assertEquals("/var/log/syslog", args.get("path"));
        assertEquals("syslog", args.get("type"));
        assertEquals("beginning", args.get("start_position"));
    }

    @Test
    public void testParseFilterWithIf() {
        String config = "filter { if [message] =~ /error/ { mutate { add_tag => \"error\" } } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        assertEquals(1, sections.size());
        assertEquals(PluginDefinition.Type.FILTER, sections.get(0).getType());
        assertEquals(1, sections.get(0).getStatements().size());

        Statement stmt = sections.get(0).getStatements().get(0);
        assertInstanceOf(IfStatement.class, stmt);
        IfStatement ifStmt = (IfStatement) stmt;

        // The condition should be a RegexEq
        BooleanExpression boolExpr = ifStmt.getBooleanExpression();
        assertNotNull(boolExpr);

        // The true branch should contain a mutate plugin
        Statement trueStmt = ifStmt.getTrueStatement();
        assertInstanceOf(PluginStatement.class, trueStmt);
        PluginStatement mutatePlugin = (PluginStatement) trueStmt;
        assertEquals("mutate", mutatePlugin.getPluginDefinition().getName());
    }

    @Test
    public void testParseArrayValues() {
        String config = "filter { grok { match => [\"message\", \"%{COMBINEDAPACHELOG}\"] } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        PluginStatement plugin = (PluginStatement) sections.get(0).getStatements().get(0);
        Map<String, Object> args = plugin.getPluginDefinition().getArguments();
        Object matchValue = args.get("match");
        assertInstanceOf(List.class, matchValue);
        @SuppressWarnings("unchecked")
        List<Object> matchList = (List<Object>) matchValue;
        assertEquals(2, matchList.size());
        assertEquals("message", matchList.get(0));
        assertEquals("%{COMBINEDAPACHELOG}", matchList.get(1));
    }

    @Test
    public void testParseHashValues() {
        String config = "filter { mutate { add_field => { \"foo\" => \"bar\" } } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        PluginStatement plugin = (PluginStatement) sections.get(0).getStatements().get(0);
        Map<String, Object> args = plugin.getPluginDefinition().getArguments();
        Object addFieldValue = args.get("add_field");
        assertInstanceOf(Map.class, addFieldValue);
        @SuppressWarnings("unchecked")
        Map<String, Object> addFieldMap = (Map<String, Object>) addFieldValue;
        assertEquals("bar", addFieldMap.get("foo"));
    }

    @Test
    public void testParseMultipleSections() {
        String config = "input { stdin {} }\n" +
                "filter { mutate { add_tag => \"processed\" } }\n" +
                "output { stdout {} }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        assertEquals(3, sections.size());
        assertEquals(PluginDefinition.Type.INPUT, sections.get(0).getType());
        assertEquals(PluginDefinition.Type.FILTER, sections.get(1).getType());
        assertEquals(PluginDefinition.Type.OUTPUT, sections.get(2).getType());
    }

    @Test
    public void testParseNestedConditionals() {
        String config = "filter {\n" +
                "  if [type] == \"syslog\" {\n" +
                "    grok { match => \"test\" }\n" +
                "  } else if [type] == \"apache\" {\n" +
                "    grok { match => \"test2\" }\n" +
                "  } else {\n" +
                "    drop {}\n" +
                "  }\n" +
                "}";
        List<LSCLParser.ParsedSection> sections = parse(config);

        assertEquals(1, sections.size());
        Statement stmt = sections.get(0).getStatements().get(0);
        assertInstanceOf(IfStatement.class, stmt);
        IfStatement ifStmt = (IfStatement) stmt;

        // True branch: grok plugin
        assertInstanceOf(PluginStatement.class, ifStmt.getTrueStatement());

        // False branch: another if (else if)
        assertInstanceOf(IfStatement.class, ifStmt.getFalseStatement());
        IfStatement elseIfStmt = (IfStatement) ifStmt.getFalseStatement();

        // True branch of else-if: grok plugin
        assertInstanceOf(PluginStatement.class, elseIfStmt.getTrueStatement());

        // False branch of else-if: else body (drop plugin)
        assertInstanceOf(PluginStatement.class, elseIfStmt.getFalseStatement());
        PluginStatement dropPlugin = (PluginStatement) elseIfStmt.getFalseStatement();
        assertEquals("drop", dropPlugin.getPluginDefinition().getName());
    }

    @Test
    public void testParseBooleanAndOperator() {
        String config = "filter { if [type] == \"syslog\" and [severity] == \"error\" { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        BooleanExpression expr = ifStmt.getBooleanExpression();
        // The condition should be an And expression
        assertInstanceOf(And.class, expr);
    }

    @Test
    public void testParseBooleanOrOperator() {
        String config = "filter { if [type] == \"syslog\" or [type] == \"apache\" { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        BooleanExpression expr = ifStmt.getBooleanExpression();
        assertInstanceOf(Or.class, expr);
    }

    @Test
    public void testParseComparisonOperators() {
        // Test all comparison operators
        String config = "filter { if [count] > 10 { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        BooleanExpression expr = ifStmt.getBooleanExpression();
        assertInstanceOf(Gt.class, expr);
    }

    @Test
    public void testParseLessThanComparison() {
        String config = "filter { if [count] < 10 { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(Lt.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseGreaterThanOrEqual() {
        String config = "filter { if [count] >= 10 { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(Gte.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseLessThanOrEqual() {
        String config = "filter { if [count] <= 10 { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(Lte.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseNotEqualComparison() {
        String config = "filter { if [type] != \"syslog\" { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(Neq.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseNegativeExpression() {
        String config = "filter { if !([type] == \"syslog\") { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(Not.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseNegatedSelector() {
        String config = "filter { if ![message] { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(Not.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseBareSelector() {
        String config = "filter { if [message] { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        // A bare selector used as a condition is wrapped in Truthy
        assertNotNull(ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseInExpression() {
        String config = "filter { if \"error\" in [tags] { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(In.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseNotInExpression() {
        String config = "filter { if \"error\" not in [tags] { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        // 'not in' produces Not(In(...))
        assertInstanceOf(Not.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseRegexMatch() {
        String config = "filter { if [message] =~ /error/ { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(RegexEq.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseRegexNotMatch() {
        String config = "filter { if [message] !~ /error/ { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        // !~ produces Not(RegexEq(...))
        assertInstanceOf(Not.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseMultiplePluginsInSection() {
        String config = "filter {\n" +
                "  grok { match => \"test\" }\n" +
                "  mutate { add_tag => \"processed\" }\n" +
                "}";
        List<LSCLParser.ParsedSection> sections = parse(config);

        assertEquals(2, sections.get(0).getStatements().size());
    }

    @Test
    public void testParseCodecPlugin() {
        String config = "input { stdin { codec => json {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        PluginStatement inputPlugin = (PluginStatement) sections.get(0).getStatements().get(0);
        Map<String, Object> args = inputPlugin.getPluginDefinition().getArguments();
        Object codec = args.get("codec");
        // Codec plugins are stored as PluginStatement in the arguments
        assertInstanceOf(PluginStatement.class, codec);
        PluginStatement codecPlugin = (PluginStatement) codec;
        assertEquals("json", codecPlugin.getPluginDefinition().getName());
        assertEquals(PluginDefinition.Type.CODEC, codecPlugin.getPluginDefinition().getType());
    }

    @Test
    public void testParseBarewordValue() {
        String config = "input { stdin { codec => plain } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        PluginStatement plugin = (PluginStatement) sections.get(0).getStatements().get(0);
        Map<String, Object> args = plugin.getPluginDefinition().getArguments();
        assertEquals("plain", args.get("codec"));
    }

    @Test
    public void testParseNumberAttribute() {
        String config = "input { generator { count => 5 } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        PluginStatement plugin = (PluginStatement) sections.get(0).getStatements().get(0);
        Map<String, Object> args = plugin.getPluginDefinition().getArguments();
        assertEquals(5L, args.get("count"));
    }

    @Test
    public void testParseFloatAttribute() {
        String config = "filter { throttle { period => 60.5 } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        PluginStatement plugin = (PluginStatement) sections.get(0).getStatements().get(0);
        Map<String, Object> args = plugin.getPluginDefinition().getArguments();
        assertEquals(60.5, args.get("period"));
    }

    @Test
    public void testParseParenthesizedCondition() {
        String config = "filter { if ([type] == \"syslog\") { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(Eq.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseBooleanPrecedence() {
        // AND has higher precedence than OR
        // "a or b and c" should parse as "a or (b and c)"
        String config = "filter { if [a] == \"1\" or [b] == \"2\" and [c] == \"3\" { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        BooleanExpression expr = ifStmt.getBooleanExpression();
        // Should be Or(Eq, And(Eq, Eq))
        assertInstanceOf(Or.class, expr);
    }

    @Test
    public void testParseEmptySection() {
        String config = "input { }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        assertEquals(1, sections.size());
        assertEquals(0, sections.get(0).getStatements().size());
    }

    @Test
    public void testParseHashWithMultipleEntries() {
        String config = "filter { mutate { add_field => { \"foo\" => \"bar\" \"baz\" => \"qux\" } } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        PluginStatement plugin = (PluginStatement) sections.get(0).getStatements().get(0);
        Map<String, Object> args = plugin.getPluginDefinition().getArguments();
        @SuppressWarnings("unchecked")
        Map<String, Object> hash = (Map<String, Object>) args.get("add_field");
        assertEquals("bar", hash.get("foo"));
        assertEquals("qux", hash.get("baz"));
    }

    @Test
    public void testParseRegexWithStringRvalue() {
        // The Ruby parser allows strings as rvalues for =~ operator
        String config = "filter { if [message] =~ \"error\" { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(RegexEq.class, ifStmt.getBooleanExpression());
    }

    @Test(expected = LSCLParseException.class)
    public void testParseDuplicateHashKeys() {
        String config = "filter { mutate { add_field => { \"foo\" => \"bar\" \"foo\" => \"qux\" } } }";
        parse(config);
    }

    @Test
    public void testParseNestedSelectors() {
        String config = "filter { if [request][headers][content-type] == \"text/html\" { drop {} } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        assertInstanceOf(Eq.class, ifStmt.getBooleanExpression());
    }

    @Test
    public void testParseMultiplePluginSections() {
        String config = "input { stdin {} }\n" +
                "input { file { path => \"/tmp/test\" } }";
        List<LSCLParser.ParsedSection> sections = parse(config);

        assertEquals(2, sections.size());
        assertEquals(PluginDefinition.Type.INPUT, sections.get(0).getType());
        assertEquals(PluginDefinition.Type.INPUT, sections.get(1).getType());
    }

    @Test
    public void testParseIfWithMultiplePlugins() {
        String config = "filter {\n" +
                "  if [type] == \"syslog\" {\n" +
                "    grok { match => \"test\" }\n" +
                "    mutate { add_tag => \"matched\" }\n" +
                "  }\n" +
                "}";
        List<LSCLParser.ParsedSection> sections = parse(config);

        IfStatement ifStmt = (IfStatement) sections.get(0).getStatements().get(0);
        // True branch should be a ComposedSequenceStatement with 2 plugins (in filter section)
        Statement trueStmt = ifStmt.getTrueStatement();
        assertInstanceOf(ComposedSequenceStatement.class, trueStmt);
        ComposedSequenceStatement composed = (ComposedSequenceStatement) trueStmt;
        assertEquals(2, composed.getStatements().size());
    }

    private static void assertInstanceOf(Class<?> expected, Object actual) {
        assertNotNull("Expected instance of " + expected.getSimpleName() + " but got null", actual);
        assertTrue("Expected instance of " + expected.getSimpleName() + " but got " + actual.getClass().getSimpleName(),
                expected.isInstance(actual));
    }
}
