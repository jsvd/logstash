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
import org.logstash.common.SourceWithMetadata;
import org.logstash.config.ir.InvalidIRException;
import org.logstash.config.ir.PluginDefinition;
import org.logstash.config.ir.imperative.*;

import java.util.Map;

import static org.junit.Assert.*;

public class LSCLCompilerTest {

    private Map<PluginDefinition.Type, Statement> compile(String config) throws Exception {
        SourceWithMetadata source = new SourceWithMetadata("test", "test", 0, 0, config);
        return LSCLCompiler.compile(config, source, false);
    }

    @Test
    public void testCompileSimpleConfig() throws Exception {
        String config = "input { stdin {} }\n" +
                "filter { mutate { add_tag => \"test\" } }\n" +
                "output { stdout {} }";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        assertNotNull(result.get(PluginDefinition.Type.INPUT));
        assertNotNull(result.get(PluginDefinition.Type.FILTER));
        assertNotNull(result.get(PluginDefinition.Type.OUTPUT));
    }

    @Test
    public void testCompileProducesPluginStatements() throws Exception {
        String config = "input { stdin {} }\n" +
                "output { stdout {} }";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        // Single plugin in a section - should be the plugin itself (not wrapped in composed)
        Statement inputStmt = result.get(PluginDefinition.Type.INPUT);
        assertInstanceOf(PluginStatement.class, inputStmt);
        PluginStatement inputPlugin = (PluginStatement) inputStmt;
        assertEquals("stdin", inputPlugin.getPluginDefinition().getName());
        assertEquals(PluginDefinition.Type.INPUT, inputPlugin.getPluginDefinition().getType());
    }

    @Test
    public void testCompileProducesIfStatements() throws Exception {
        String config = "filter {\n" +
                "  if [type] == \"syslog\" {\n" +
                "    grok { match => \"test\" }\n" +
                "  }\n" +
                "}";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        Statement filterStmt = result.get(PluginDefinition.Type.FILTER);
        assertInstanceOf(IfStatement.class, filterStmt);
    }

    @Test
    public void testInputsComposedAsParallel() throws Exception {
        String config = "input {\n" +
                "  stdin {}\n" +
                "  file { path => \"/tmp/test\" }\n" +
                "}";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        Statement inputStmt = result.get(PluginDefinition.Type.INPUT);
        assertInstanceOf(ComposedParallelStatement.class, inputStmt);
        ComposedParallelStatement composed = (ComposedParallelStatement) inputStmt;
        assertEquals(2, composed.getStatements().size());
    }

    @Test
    public void testFiltersComposedAsSequence() throws Exception {
        String config = "filter {\n" +
                "  grok { match => \"test\" }\n" +
                "  mutate { add_tag => \"processed\" }\n" +
                "}";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        Statement filterStmt = result.get(PluginDefinition.Type.FILTER);
        assertInstanceOf(ComposedSequenceStatement.class, filterStmt);
        ComposedSequenceStatement composed = (ComposedSequenceStatement) filterStmt;
        assertEquals(2, composed.getStatements().size());
    }

    @Test
    public void testOutputsComposedAsParallel() throws Exception {
        String config = "output {\n" +
                "  stdout {}\n" +
                "  elasticsearch { hosts => \"localhost\" }\n" +
                "}";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        Statement outputStmt = result.get(PluginDefinition.Type.OUTPUT);
        assertInstanceOf(ComposedParallelStatement.class, outputStmt);
        ComposedParallelStatement composed = (ComposedParallelStatement) outputStmt;
        assertEquals(2, composed.getStatements().size());
    }

    @Test
    public void testCompileEmptySections() throws Exception {
        String config = "input { stdin {} }";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        // Filter and output sections should be NoopStatement when empty
        Statement filterStmt = result.get(PluginDefinition.Type.FILTER);
        assertInstanceOf(NoopStatement.class, filterStmt);

        Statement outputStmt = result.get(PluginDefinition.Type.OUTPUT);
        assertInstanceOf(NoopStatement.class, outputStmt);
    }

    @Test
    public void testCompileFullConfig() throws Exception {
        String config = "input {\n" +
                "  stdin { codec => json {} }\n" +
                "}\n" +
                "filter {\n" +
                "  if [type] == \"syslog\" {\n" +
                "    grok { match => \"test\" }\n" +
                "  } else {\n" +
                "    drop {}\n" +
                "  }\n" +
                "  mutate { add_tag => \"processed\" }\n" +
                "}\n" +
                "output {\n" +
                "  stdout {}\n" +
                "}";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        assertNotNull(result.get(PluginDefinition.Type.INPUT));
        assertNotNull(result.get(PluginDefinition.Type.FILTER));
        assertNotNull(result.get(PluginDefinition.Type.OUTPUT));

        // Filter should be a ComposedSequenceStatement (if + mutate)
        Statement filterStmt = result.get(PluginDefinition.Type.FILTER);
        assertInstanceOf(ComposedSequenceStatement.class, filterStmt);
        ComposedSequenceStatement filterComposed = (ComposedSequenceStatement) filterStmt;
        assertEquals(2, filterComposed.getStatements().size());
        assertInstanceOf(IfStatement.class, filterComposed.getStatements().get(0));
        assertInstanceOf(PluginStatement.class, filterComposed.getStatements().get(1));
    }

    @Test
    public void testCompileWithEscapeSupport() throws Exception {
        String config = "input { generator { message => \"hello\\nworld\" } }";
        SourceWithMetadata source = new SourceWithMetadata("test", "test", 0, 0, config);
        Map<PluginDefinition.Type, Statement> result = LSCLCompiler.compile(config, source, true);

        PluginStatement plugin = (PluginStatement) result.get(PluginDefinition.Type.INPUT);
        assertEquals("hello\nworld", plugin.getPluginDefinition().getArguments().get("message"));
    }

    @Test
    public void testCompileMultipleSectionsOfSameType() throws Exception {
        String config = "input { stdin {} }\n" +
                "input { file { path => \"/tmp/test\" } }";
        Map<PluginDefinition.Type, Statement> result = compile(config);

        Statement inputStmt = result.get(PluginDefinition.Type.INPUT);
        assertInstanceOf(ComposedParallelStatement.class, inputStmt);
        ComposedParallelStatement composed = (ComposedParallelStatement) inputStmt;
        assertEquals(2, composed.getStatements().size());
    }

    @Test(expected = LSCLParseException.class)
    public void testCompileInvalidConfig() throws Exception {
        compile("invalid config text");
    }

    @Test(expected = LSCLParseException.class)
    public void testRejectsInvalidFieldReferenceWithNestedBrackets() throws Exception {
        compile("input { } output { if [[f[[[oo] == [bar] { } }");
    }

    @Test(expected = LSCLParseException.class)
    public void testRejectsEmptyFieldReference() throws Exception {
        compile("filter { if [] == \"test\" { drop {} } }");
    }

    @Test
    public void testFieldReferenceWithSpaces() throws Exception {
        Map<PluginDefinition.Type, Statement> result =
                compile("filter { if [field with space] == \"hurray\" { drop {} } }");
        assertNotNull(result.get(PluginDefinition.Type.FILTER));
    }

    @Test
    public void testNestedFieldReferenceWithSpaces() throws Exception {
        Map<PluginDefinition.Type, Statement> result =
                compile("filter { if [nested field][reference with][some spaces] == \"hurray\" { drop {} } }");
        assertNotNull(result.get(PluginDefinition.Type.FILTER));
    }

    @Test
    public void testInOperatorWithListLiteral() throws Exception {
        Map<PluginDefinition.Type, Statement> result =
                compile("filter { if [foo] in [\"hello\", \"world\", \"foo\"] { drop {} } }");
        assertNotNull(result.get(PluginDefinition.Type.FILTER));
    }

    @Test
    public void testNotInOperatorWithListLiteral() throws Exception {
        Map<PluginDefinition.Type, Statement> result =
                compile("filter { if \"foo\" not in [\"hello\", \"world\"] { drop {} } }");
        assertNotNull(result.get(PluginDefinition.Type.FILTER));
    }

    private static void assertInstanceOf(Class<?> expected, Object actual) {
        assertNotNull("Expected instance of " + expected.getSimpleName() + " but got null", actual);
        assertTrue("Expected instance of " + expected.getSimpleName() + " but got " + actual.getClass().getSimpleName(),
                expected.isInstance(actual));
    }
}
