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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.logstash.common.IncompleteSourceWithMetadataException;
import org.logstash.common.SourceWithMetadata;
import org.logstash.config.ir.DSL;
import org.logstash.config.ir.InvalidIRException;
import org.logstash.config.ir.PluginDefinition;
import org.logstash.config.ir.expression.BooleanExpression;
import org.logstash.config.ir.expression.EventValueExpression;
import org.logstash.config.ir.expression.Expression;
import org.logstash.config.ir.expression.ValueExpression;
import org.logstash.config.ir.imperative.IfStatement;
import org.logstash.config.ir.imperative.PluginStatement;
import org.logstash.config.ir.imperative.Statement;

/**
 * Recursive descent parser for the Logstash Configuration Language (LSCL).
 * Produces the same IR objects (Statement, Expression) as the Ruby Treetop parser.
 *
 * <p>Grammar rules implemented:
 * <pre>
 *   config          -> pluginSection*
 *   pluginSection   -> pluginType '{' branchOrPlugin* '}'
 *   pluginType      -> 'input' | 'filter' | 'output'
 *   branchOrPlugin  -> branch | plugin
 *   plugin          -> name '{' attribute* '}'
 *   attribute       -> name '=>' value
 *   value           -> plugin | bareword | string | number | array | hash
 *   array           -> '[' (value (',' value)*)? ']'
 *   hash            -> '{' (hashEntry (',' hashEntry)* ','?)? '}'
 *   hashEntry       -> (string | bareword | number) '=>' value
 *   branch          -> ifBlock elseIfBlock* elseBlock?
 *   ifBlock         -> 'if' condition '{' branchOrPlugin* '}'
 *   elseIfBlock     -> 'else' 'if' condition '{' branchOrPlugin* '}'
 *   elseBlock       -> 'else' '{' branchOrPlugin* '}'
 *   condition       -> expression (boolOp expression)*
 *   expression      -> '(' condition ')' | negativeExpr | inExpr | notInExpr | compareExpr | regexpExpr | rvalue
 *   rvalue          -> string | number | selector | array | regexp
 *   selector        -> selectorElement+
 *   selectorElement -> '[' fieldName ']'
 * </pre>
 */
public final class LSCLParser {

    private final List<LSCLToken> tokens;
    private int pos;
    private final String baseProtocol;
    private final String baseId;
    private final boolean supportEscapes;

    /**
     * The section type context tracks what plugin section we are currently in
     * (input, filter, output), used to determine the PluginDefinition.Type for plugins.
     */
    private PluginDefinition.Type currentSectionType;

    /**
     * Tracks nesting depth to distinguish top-level plugins from codec (nested) plugins.
     */
    private int pluginNestingDepth;

    public LSCLParser(List<LSCLToken> tokens, String baseProtocol, String baseId, boolean supportEscapes) {
        this.tokens = tokens;
        this.pos = 0;
        this.baseProtocol = baseProtocol;
        this.baseId = baseId;
        this.supportEscapes = supportEscapes;
    }

    /**
     * Parses the token stream and returns a list of parsed plugin sections,
     * each represented as a pair of (section type, list of statements).
     */
    public List<ParsedSection> parse() {
        List<ParsedSection> sections = new ArrayList<>();
        while (!isAtEnd()) {
            sections.add(parsePluginSection());
        }
        return sections;
    }

    // ---- Section Parsing ----

    private ParsedSection parsePluginSection() {
        PluginDefinition.Type sectionType = parsePluginType();
        this.currentSectionType = sectionType;
        expect(LSCLToken.Type.LBRACE, "Expected '{' after plugin section type");

        List<Statement> statements = new ArrayList<>();
        while (!check(LSCLToken.Type.RBRACE)) {
            statements.add(parseBranchOrPlugin());
        }
        expect(LSCLToken.Type.RBRACE, "Expected '}' to close plugin section");

        return new ParsedSection(sectionType, statements);
    }

    private PluginDefinition.Type parsePluginType() {
        LSCLToken token = current();
        switch (token.getType()) {
            case INPUT:
                advance();
                return PluginDefinition.Type.INPUT;
            case FILTER:
                advance();
                return PluginDefinition.Type.FILTER;
            case OUTPUT:
                advance();
                return PluginDefinition.Type.OUTPUT;
            default:
                throw parseError("Expected 'input', 'filter', or 'output', got " + token);
        }
    }

    // ---- Branch or Plugin ----

    private Statement parseBranchOrPlugin() {
        if (check(LSCLToken.Type.IF)) {
            return parseBranch();
        }
        return parsePlugin();
    }

    // ---- Plugin ----

    private PluginStatement parsePlugin() {
        LSCLToken nameToken = current();
        String pluginName = parseName();
        SourceWithMetadata meta = sourceMeta(nameToken);

        expect(LSCLToken.Type.LBRACE, "Expected '{' after plugin name '" + pluginName + "'");

        // Determine the plugin type: if we are nested inside another plugin, it's a codec
        pluginNestingDepth++;
        PluginDefinition.Type pluginType;
        if (pluginNestingDepth > 1) {
            pluginType = PluginDefinition.Type.CODEC;
        } else {
            pluginType = currentSectionType;
        }

        Map<String, Object> attributes = parseAttributes(pluginType, pluginName);

        expect(LSCLToken.Type.RBRACE, "Expected '}' to close plugin '" + pluginName + "'");
        pluginNestingDepth--;

        return DSL.iPlugin(meta, pluginType, pluginName, attributes);
    }

    private Map<String, Object> parseAttributes(PluginDefinition.Type pluginType, String pluginName) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        while (!check(LSCLToken.Type.RBRACE) && !isAtEnd()) {
            LSCLToken nameToken = current();
            String attrName;
            if (nameToken.getType() == LSCLToken.Type.STRING) {
                // Quoted attribute name - strip quotes
                advance();
                attrName = nameToken.getValue();
            } else {
                attrName = parseName();
            }
            expect(LSCLToken.Type.ARROW, "Expected '=>' after attribute name '" + attrName + "'");
            Object attrValue = parseValue();

            // Handle codec validation: if input/output section has multiple codec attributes, error
            Object existing = attributes.get(attrName);
            if (existing != null) {
                if (existing instanceof Map) {
                    // Merge hash attributes (legacy behavior for e.g., grok 'match')
                    if (attrValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> existingMap = (Map<String, Object>) existing;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> newMap = (Map<String, Object>) attrValue;
                        existingMap.putAll(newMap);
                        attrValue = existingMap;
                    }
                } else if (existing instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> existingList = (List<Object>) existing;
                    if (attrValue instanceof List) {
                        existingList.addAll((List<?>) attrValue);
                    } else {
                        existingList.add(attrValue);
                    }
                    attrValue = existingList;
                } else if (!existing.equals(attrValue)) {
                    List<Object> merged = new ArrayList<>();
                    merged.add(existing);
                    merged.add(attrValue);
                    attrValue = merged;
                }
            }
            attributes.put(attrName, attrValue);
        }
        return attributes;
    }

    private String parseName() {
        LSCLToken token = current();
        if (token.getType() == LSCLToken.Type.BAREWORD || isKeywordUsableAsName(token.getType())) {
            advance();
            return token.getValue();
        }
        if (token.getType() == LSCLToken.Type.STRING) {
            advance();
            return token.getValue();
        }
        throw parseError("Expected a name (bareword or string), got " + token);
    }

    /**
     * Some keywords can also be used as plugin names or attribute names.
     * For example: "input", "output", "filter" can be bare names in certain contexts.
     */
    private boolean isKeywordUsableAsName(LSCLToken.Type type) {
        switch (type) {
            case INPUT:
            case FILTER:
            case OUTPUT:
            case IF:
            case ELSE:
            case IN:
            case NOT:
            case AND:
            case OR:
            case XOR:
            case NAND:
                return true;
            default:
                return false;
        }
    }

    // ---- Values ----

    /**
     * Parses a value for a plugin attribute.
     * value -> plugin | bareword | string | number | array | hash
     */
    private Object parseValue() {
        LSCLToken token = current();

        switch (token.getType()) {
            case STRING: {
                advance();
                String val = token.getValue();
                if (supportEscapes) {
                    val = processEscapes(val);
                }
                return val;
            }
            case NUMBER: {
                advance();
                return parseNumberValue(token.getValue());
            }
            case BAREWORD: {
                // Check if this is a plugin (bareword followed by '{')
                if (pos + 1 < tokens.size() && tokens.get(pos + 1).getType() == LSCLToken.Type.LBRACE) {
                    return parsePlugin();
                }
                advance();
                return token.getValue();
            }
            case LBRACKET: {
                return parseArray();
            }
            case LBRACE: {
                return parseHash();
            }
            default:
                // Check for keywords that can be used as barewords in value context
                if (isKeywordUsableAsName(token.getType())) {
                    // Check if this is a plugin (keyword followed by '{')
                    if (pos + 1 < tokens.size() && tokens.get(pos + 1).getType() == LSCLToken.Type.LBRACE) {
                        return parsePlugin();
                    }
                    advance();
                    return token.getValue();
                }
                throw parseError("Expected a value (string, number, bareword, array, hash, or plugin), got " + token);
        }
    }

    private List<Object> parseArray() {
        expect(LSCLToken.Type.LBRACKET, "Expected '['");
        List<Object> elements = new ArrayList<>();
        if (!check(LSCLToken.Type.RBRACKET)) {
            elements.add(parseValue());
            while (check(LSCLToken.Type.COMMA)) {
                advance(); // consume comma
                if (check(LSCLToken.Type.RBRACKET)) {
                    break; // trailing comma
                }
                elements.add(parseValue());
            }
        }
        expect(LSCLToken.Type.RBRACKET, "Expected ']' to close array");
        return elements;
    }

    private Map<String, Object> parseHash() {
        expect(LSCLToken.Type.LBRACE, "Expected '{'");
        Map<String, Object> hash = new LinkedHashMap<>();
        if (!check(LSCLToken.Type.RBRACE)) {
            parseHashEntry(hash);
            while (!check(LSCLToken.Type.RBRACE) && !isAtEnd()) {
                // Hash entries can be separated by whitespace (no comma required in LSCL)
                // or by commas
                if (check(LSCLToken.Type.COMMA)) {
                    advance(); // consume optional comma
                }
                if (check(LSCLToken.Type.RBRACE)) {
                    break; // trailing comma or end
                }
                parseHashEntry(hash);
            }
        }
        expect(LSCLToken.Type.RBRACE, "Expected '}' to close hash");
        return hash;
    }

    private void parseHashEntry(Map<String, Object> hash) {
        LSCLToken keyToken = current();
        String key;
        switch (keyToken.getType()) {
            case STRING:
                advance();
                key = keyToken.getValue();
                break;
            case BAREWORD:
                advance();
                key = keyToken.getValue();
                break;
            case NUMBER:
                advance();
                key = keyToken.getValue();
                break;
            default:
                if (isKeywordUsableAsName(keyToken.getType())) {
                    advance();
                    key = keyToken.getValue();
                    break;
                }
                throw parseError("Expected hash key (string, bareword, or number), got " + keyToken);
        }
        expect(LSCLToken.Type.ARROW, "Expected '=>' in hash entry after key '" + key + "'");
        Object value = parseValue();

        if (hash.containsKey(key)) {
            throw parseError("Duplicate key '" + key + "' found in hash at line " + keyToken.getLine());
        }
        hash.put(key, value);
    }

    private Object parseNumberValue(String text) {
        if (text.contains(".")) {
            return Double.parseDouble(text);
        }
        return Long.parseLong(text);
    }

    // ---- Branches (if/else if/else) ----

    /**
     * Parses a branch: if/else if/else chain.
     * Produces nested IfStatements following the Ruby compiler's pattern.
     */
    private Statement parseBranch() {
        // Parse the 'if' block
        LSCLToken ifToken = current();
        expect(LSCLToken.Type.IF, "Expected 'if'");
        Expression ifCondition = parseCondition();
        SourceWithMetadata condMeta = ifCondition.getSourceWithMetadata();

        expect(LSCLToken.Type.LBRACE, "Expected '{' after if condition");
        List<Statement> ifBody = parseBranchBody();
        expect(LSCLToken.Type.RBRACE, "Expected '}' to close if block");

        // Collect else-if and else blocks
        List<ElseIfClause> elseIfClauses = new ArrayList<>();
        List<Statement> elseBody = null;

        while (check(LSCLToken.Type.ELSE)) {
            advance(); // consume 'else'
            if (check(LSCLToken.Type.IF)) {
                // else if
                advance(); // consume 'if'
                Expression elseIfCondition = parseCondition();
                expect(LSCLToken.Type.LBRACE, "Expected '{' after else if condition");
                List<Statement> elseIfBody = parseBranchBody();
                expect(LSCLToken.Type.RBRACE, "Expected '}' to close else if block");
                elseIfClauses.add(new ElseIfClause(elseIfCondition, elseIfBody));
            } else {
                // else
                expect(LSCLToken.Type.LBRACE, "Expected '{' after else");
                elseBody = parseBranchBody();
                expect(LSCLToken.Type.RBRACE, "Expected '}' to close else block");
                break;
            }
        }

        // Build nested if/else structure from the bottom up (like the Ruby compiler)
        try {
            return buildIfStatement(condMeta, ifCondition, ifBody, elseIfClauses, elseBody);
        } catch (InvalidIRException e) {
            throw new LSCLParseException("Failed to build if statement: " + e.getMessage(),
                    ifToken.getLine(), ifToken.getColumn());
        }
    }

    private Statement buildIfStatement(
            SourceWithMetadata condMeta,
            Expression condition,
            List<Statement> trueBody,
            List<ElseIfClause> elseIfClauses,
            List<Statement> elseBody) throws InvalidIRException {

        // Build the false branch by nesting else-if clauses
        Statement falseBranch;
        if (!elseIfClauses.isEmpty()) {
            // Build from the last else-if backward
            ElseIfClause last = elseIfClauses.get(elseIfClauses.size() - 1);
            Statement lastElseBody = elseBody != null ? composeBody(elseBody) : DSL.noop();
            falseBranch = DSL.iIf(
                    last.condition.getSourceWithMetadata(),
                    last.condition,
                    composeBody(last.body),
                    lastElseBody
            );

            for (int i = elseIfClauses.size() - 2; i >= 0; i--) {
                ElseIfClause clause = elseIfClauses.get(i);
                falseBranch = DSL.iIf(
                        clause.condition.getSourceWithMetadata(),
                        clause.condition,
                        composeBody(clause.body),
                        falseBranch
                );
            }
        } else if (elseBody != null) {
            falseBranch = composeBody(elseBody);
        } else {
            falseBranch = DSL.noop();
        }

        Statement trueBranch = composeBody(trueBody);
        return DSL.iIf(condMeta, condition, trueBranch, falseBranch);
    }

    private Statement composeBody(List<Statement> statements) throws InvalidIRException {
        if (statements.isEmpty()) {
            return DSL.noop();
        }
        if (statements.size() == 1) {
            return statements.get(0);
        }
        // Use the section type to determine sequence vs parallel
        if (currentSectionType == PluginDefinition.Type.FILTER) {
            return DSL.iComposeSequence(null, statements.toArray(new Statement[0]));
        } else {
            return DSL.iComposeParallel(null, statements.toArray(new Statement[0]));
        }
    }

    private List<Statement> parseBranchBody() {
        List<Statement> statements = new ArrayList<>();
        while (!check(LSCLToken.Type.RBRACE) && !isAtEnd()) {
            statements.add(parseBranchOrPlugin());
        }
        return statements;
    }

    // ---- Conditions ----

    /**
     * Parses a condition, handling boolean operators (and, or, xor, nand) with
     * proper precedence using the shunting-yard algorithm (matching the Ruby implementation).
     *
     * condition -> expression (boolOp expression)*
     */
    private Expression parseCondition() {
        List<Object> elements = new ArrayList<>(); // Expression and BoolOp interleaved
        elements.add(parseExpression());

        while (isBooleanOperator(current().getType())) {
            String op = current().getValue();
            advance();
            Expression right = parseExpression();
            elements.add(op);
            elements.add(right);
        }

        if (elements.size() == 1) {
            Expression expr = (Expression) elements.get(0);
            // A bare selector used as a condition needs to be wrapped in eTruthy
            return wrapTruthyIfNeeded(expr);
        }

        // Apply shunting-yard algorithm for operator precedence
        return applyShuntingYard(elements);
    }

    private Expression wrapTruthyIfNeeded(Expression expr) {
        try {
            if (expr instanceof BooleanExpression) {
                return expr;
            }
            return DSL.eTruthy(expr.getSourceWithMetadata(), expr);
        } catch (InvalidIRException e) {
            throw new LSCLParseException("Failed to wrap expression in truthy: " + e.getMessage());
        }
    }

    /**
     * Uses the shunting-yard algorithm to handle operator precedence.
     * AND has higher precedence than OR (matching the Ruby implementation).
     */
    private Expression applyShuntingYard(List<Object> elements) {
        List<Object> output = new ArrayList<>();
        List<String> operators = new ArrayList<>();

        for (Object elem : elements) {
            if (elem instanceof String) {
                String op = (String) elem;
                while (!operators.isEmpty() &&
                        boolOpPrecedence(operators.get(operators.size() - 1)) > boolOpPrecedence(op)) {
                    output.add(operators.remove(operators.size() - 1));
                }
                operators.add(op);
            } else {
                output.add(elem);
            }
        }

        // Pop remaining operators
        for (int i = operators.size() - 1; i >= 0; i--) {
            output.add(operators.get(i));
        }

        // Evaluate the postfix expression
        List<Expression> stack = new ArrayList<>();
        try {
            for (Object elem : output) {
                if (elem instanceof String) {
                    String op = (String) elem;
                    Expression rval = stack.remove(stack.size() - 1);
                    Expression lval = stack.remove(stack.size() - 1);
                    SourceWithMetadata meta = lval.getSourceWithMetadata();
                    switch (op) {
                        case "and":
                            stack.add(DSL.eAnd(meta, lval, rval));
                            break;
                        case "or":
                            stack.add(DSL.eOr(meta, lval, rval));
                            break;
                        case "nand":
                            stack.add(DSL.eNand(meta, lval, rval));
                            break;
                        case "xor":
                            stack.add(DSL.eXor(meta, lval, rval));
                            break;
                        default:
                            throw new LSCLParseException("Unknown boolean operator: " + op);
                    }
                } else {
                    stack.add((Expression) elem);
                }
            }
        } catch (InvalidIRException e) {
            throw new LSCLParseException("Failed to build boolean expression: " + e.getMessage());
        }

        if (stack.size() != 1) {
            throw new LSCLParseException("Invalid condition: stack size is " + stack.size());
        }
        return stack.get(0);
    }

    private int boolOpPrecedence(String op) {
        switch (op) {
            case "and":
            case "nand":
                return 2;
            case "or":
            case "xor":
                return 1;
            default:
                return 0;
        }
    }

    private boolean isBooleanOperator(LSCLToken.Type type) {
        return type == LSCLToken.Type.AND ||
                type == LSCLToken.Type.OR ||
                type == LSCLToken.Type.XOR ||
                type == LSCLToken.Type.NAND;
    }

    // ---- Expressions ----

    /**
     * Parses a single expression within a condition.
     * expression -> '(' condition ')' | negativeExpr | inExpr | notInExpr | compareExpr | regexpExpr | rvalue
     */
    private Expression parseExpression() {
        LSCLToken token = current();

        // Parenthesized condition
        if (token.getType() == LSCLToken.Type.LPAREN) {
            advance(); // consume '('
            Expression inner = parseCondition();
            expect(LSCLToken.Type.RPAREN, "Expected ')' to close parenthesized condition");
            return inner;
        }

        // Negative expression: ! ( condition ) or ! selector
        if (token.getType() == LSCLToken.Type.BANG) {
            return parseNegativeExpression();
        }

        // Try to parse an rvalue first, then check for operators
        Expression lval = parseRvalue();

        // Check for comparison operators
        if (isComparisonOperator(current().getType())) {
            return parseComparisonRest(lval);
        }

        // Check for regex operators
        if (current().getType() == LSCLToken.Type.MATCH ||
                current().getType() == LSCLToken.Type.NOT_MATCH) {
            return parseRegexpExpressionRest(lval);
        }

        // Check for 'in' operator
        if (current().getType() == LSCLToken.Type.IN) {
            advance(); // consume 'in'
            Expression rval = parseRvalue();
            try {
                return DSL.eIn(lval.getSourceWithMetadata(), lval, rval);
            } catch (Exception e) {
                throw new LSCLParseException("Failed to build 'in' expression: " + e.getMessage());
            }
        }

        // Check for 'not in' operator
        if (current().getType() == LSCLToken.Type.NOT && peekIs(LSCLToken.Type.IN)) {
            advance(); // consume 'not'
            advance(); // consume 'in'
            Expression rval = parseRvalue();
            try {
                return DSL.eNot(lval.getSourceWithMetadata(),
                        DSL.eIn(lval.getSourceWithMetadata(), lval, rval));
            } catch (InvalidIRException e) {
                throw new LSCLParseException("Failed to build 'not in' expression: " + e.getMessage());
            }
        }

        return lval;
    }

    private Expression parseNegativeExpression() {
        LSCLToken bangToken = current();
        expect(LSCLToken.Type.BANG, "Expected '!'");
        SourceWithMetadata meta = sourceMeta(bangToken);

        try {
            if (check(LSCLToken.Type.LPAREN)) {
                // !( condition )
                advance(); // consume '('
                Expression inner = parseCondition();
                expect(LSCLToken.Type.RPAREN, "Expected ')' after negated condition");
                return DSL.eNot(meta, inner);
            } else {
                // !selector
                Expression expr = parseRvalue();
                return DSL.eNot(meta, expr);
            }
        } catch (InvalidIRException e) {
            throw new LSCLParseException("Failed to build negative expression: " + e.getMessage(),
                    bangToken.getLine(), bangToken.getColumn());
        }
    }

    private Expression parseComparisonRest(Expression lval) {
        LSCLToken opToken = current();
        advance(); // consume operator
        Expression rval = parseRvalue();
        SourceWithMetadata meta = lval.getSourceWithMetadata();

        switch (opToken.getType()) {
            case EQ:
                return DSL.eEq(meta, lval, rval);
            case NEQ:
                return DSL.eNeq(meta, lval, rval);
            case GT:
                return DSL.eGt(meta, lval, rval);
            case LT:
                return DSL.eLt(meta, lval, rval);
            case GTE:
                return DSL.eGte(meta, lval, rval);
            case LTE:
                return DSL.eLte(meta, lval, rval);
            default:
                throw parseError("Unknown comparison operator: " + opToken);
        }
    }

    private Expression parseRegexpExpressionRest(Expression lval) {
        LSCLToken opToken = current();
        advance(); // consume =~ or !~
        Expression rval = parseRegexOrStringRvalue();
        SourceWithMetadata meta = lval.getSourceWithMetadata();

        try {
            // If the rval is a string value, convert it to a regex
            ValueExpression regexExpr;
            if (rval instanceof ValueExpression) {
                ValueExpression valExpr = (ValueExpression) rval;
                Object val = valExpr.get();
                if (val instanceof String) {
                    regexExpr = DSL.eRegex(meta, (String) val);
                } else {
                    regexExpr = valExpr;
                }
            } else {
                throw parseError("Expected regex or string after regex operator");
            }

            if (opToken.getType() == LSCLToken.Type.MATCH) {
                return DSL.eRegexEq(meta, lval, regexExpr);
            } else {
                return DSL.eRegexNeq(meta, lval, regexExpr);
            }
        } catch (InvalidIRException e) {
            throw new LSCLParseException("Failed to build regex expression: " + e.getMessage(),
                    opToken.getLine(), opToken.getColumn());
        }
    }

    private boolean isComparisonOperator(LSCLToken.Type type) {
        return type == LSCLToken.Type.EQ ||
                type == LSCLToken.Type.NEQ ||
                type == LSCLToken.Type.GT ||
                type == LSCLToken.Type.LT ||
                type == LSCLToken.Type.GTE ||
                type == LSCLToken.Type.LTE;
    }

    // ---- RValues ----

    /**
     * Parses an rvalue (right-hand value in conditions).
     * rvalue -> string | number | selector | array | regexp
     */
    private Expression parseRvalue() {
        LSCLToken token = current();

        try {
            switch (token.getType()) {
                case STRING: {
                    advance();
                    String val = token.getValue();
                    if (supportEscapes) {
                        val = processEscapes(val);
                    }
                    return DSL.eValue(sourceMeta(token), val);
                }
                case NUMBER: {
                    advance();
                    return parseNumberExpression(token);
                }
                case LBRACKET: {
                    // Distinguish between selector [field] and list literal ["a", "b"]
                    // Field names in selectors are never quoted, so a STRING after '[' means list literal
                    if (peekIs(LSCLToken.Type.STRING)) {
                        return parseListExpression();
                    }
                    // Selector: [field][subfield]...
                    return parseSelector();
                }
                case REGEX: {
                    advance();
                    return DSL.eRegex(sourceMeta(token), token.getValue());
                }
                default:
                    throw parseError("Expected rvalue (string, number, selector, array, or regex), got " + token);
            }
        } catch (InvalidIRException e) {
            throw new LSCLParseException("Failed to build rvalue: " + e.getMessage(),
                    token.getLine(), token.getColumn());
        }
    }

    /**
     * Parses a regex or string rvalue (used after =~ or !~ operators).
     */
    private Expression parseRegexOrStringRvalue() {
        LSCLToken token = current();
        try {
            if (token.getType() == LSCLToken.Type.REGEX) {
                advance();
                return DSL.eRegex(sourceMeta(token), token.getValue());
            }
            if (token.getType() == LSCLToken.Type.STRING) {
                advance();
                String val = token.getValue();
                if (supportEscapes) {
                    val = processEscapes(val);
                }
                return DSL.eValue(sourceMeta(token), val);
            }
            throw parseError("Expected regex or string after regex operator, got " + token);
        } catch (InvalidIRException e) {
            throw new LSCLParseException("Failed to build regex/string rvalue: " + e.getMessage(),
                    token.getLine(), token.getColumn());
        }
    }

    /**
     * Parses a selector: [field1][field2]...
     * The full selector text is passed to DSL.eEventValue.
     */
    private Expression parseSelector() {
        LSCLToken startToken = current();
        StringBuilder selectorText = new StringBuilder();
        while (check(LSCLToken.Type.LBRACKET)) {
            advance(); // consume '['
            StringBuilder fieldName = new StringBuilder();
            int lastTokenEndCol = -1;
            // Read field name tokens until ']' — reject nested brackets
            // Track column positions to preserve whitespace (e.g. [field with space])
            while (!check(LSCLToken.Type.RBRACKET) && !isAtEnd()) {
                LSCLToken fieldToken = current();
                if (fieldToken.getType() == LSCLToken.Type.LBRACKET) {
                    throw new LSCLParseException(
                            "Invalid field reference: unexpected '[' inside selector",
                            fieldToken.getLine(), fieldToken.getColumn());
                }
                // Insert spaces between tokens based on column gap
                if (lastTokenEndCol >= 0) {
                    int gap = fieldToken.getColumn() - lastTokenEndCol;
                    for (int i = 0; i < gap; i++) {
                        fieldName.append(' ');
                    }
                }
                fieldName.append(fieldToken.getValue());
                lastTokenEndCol = fieldToken.getColumn() + fieldToken.getValue().length();
                advance();
            }
            if (fieldName.length() == 0) {
                throw new LSCLParseException(
                        "Invalid field reference: empty selector '[]'",
                        startToken.getLine(), startToken.getColumn());
            }
            expect(LSCLToken.Type.RBRACKET, "Expected ']' to close selector element");
            selectorText.append("[").append(fieldName.toString()).append("]");
        }
        return DSL.eEventValue(sourceMeta(startToken), selectorText.toString());
    }

    /**
     * Parses a list literal expression used in conditions, e.g. ["hello", "world", "foo"].
     * Returns a ValueExpression wrapping a List of values.
     */
    private Expression parseListExpression() {
        LSCLToken startToken = current();
        expect(LSCLToken.Type.LBRACKET, "Expected '['");
        List<Object> elements = new ArrayList<>();
        if (!check(LSCLToken.Type.RBRACKET)) {
            elements.add(parseListElementValue());
            while (check(LSCLToken.Type.COMMA)) {
                advance(); // consume comma
                if (check(LSCLToken.Type.RBRACKET)) {
                    break; // trailing comma
                }
                elements.add(parseListElementValue());
            }
        }
        expect(LSCLToken.Type.RBRACKET, "Expected ']' to close list literal");
        try {
            return DSL.eValue(sourceMeta(startToken), elements);
        } catch (InvalidIRException e) {
            throw new LSCLParseException("Failed to build list expression: " + e.getMessage(),
                    startToken.getLine(), startToken.getColumn());
        }
    }

    /**
     * Parses a single element value within a list literal.
     * Supports strings, numbers, and barewords.
     */
    private Object parseListElementValue() {
        LSCLToken token = current();
        switch (token.getType()) {
            case STRING:
                advance();
                String val = token.getValue();
                if (supportEscapes) {
                    val = processEscapes(val);
                }
                return val;
            case NUMBER:
                advance();
                return parseNumberValue(token.getValue());
            case BAREWORD:
                advance();
                return token.getValue();
            default:
                if (isKeywordUsableAsName(token.getType())) {
                    advance();
                    return token.getValue();
                }
                throw parseError("Expected list element (string, number, or bareword), got " + token);
        }
    }

    private Expression parseNumberExpression(LSCLToken token) throws InvalidIRException {
        String text = token.getValue();
        if (text.contains(".")) {
            return DSL.eValue(sourceMeta(token), Double.parseDouble(text));
        }
        return DSL.eValue(sourceMeta(token), Long.parseLong(text));
    }

    // ---- Utility Methods ----

    private LSCLToken current() {
        if (pos >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // Return EOF
        }
        return tokens.get(pos);
    }

    private boolean check(LSCLToken.Type type) {
        return current().getType() == type;
    }

    private boolean peekIs(LSCLToken.Type type) {
        if (pos + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(pos + 1).getType() == type;
    }

    private void advance() {
        if (pos < tokens.size()) {
            pos++;
        }
    }

    private boolean isAtEnd() {
        return current().getType() == LSCLToken.Type.EOF;
    }

    private LSCLToken expect(LSCLToken.Type type, String message) {
        LSCLToken token = current();
        if (token.getType() != type) {
            throw parseError(message + ", got " + token);
        }
        advance();
        return token;
    }

    private LSCLParseException parseError(String message) {
        LSCLToken token = current();
        return new LSCLParseException(message, token.getLine(), token.getColumn());
    }

    private SourceWithMetadata sourceMeta(LSCLToken token) {
        try {
            return new SourceWithMetadata(baseProtocol, baseId, token.getLine(), token.getColumn(),
                    token.getValue());
        } catch (IncompleteSourceWithMetadataException e) {
            throw new LSCLParseException("Failed to create source metadata: " + e.getMessage(),
                    token.getLine(), token.getColumn());
        }
    }

    /**
     * Processes escape sequences in strings when config.support_escapes is enabled.
     * Mirrors the Ruby LogStash::Config::StringEscape.process_escapes behavior.
     */
    static String processEscapes(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case '"':
                    case '\'':
                    case '\\':
                        sb.append(next);
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case '0':
                        sb.append('\0');
                        i++;
                        break;
                    default:
                        // Unknown escape, keep as-is
                        sb.append(c);
                        sb.append(next);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- Inner Classes ----

    /**
     * Represents a parsed plugin section with its type and statements.
     */
    public static final class ParsedSection {
        private final PluginDefinition.Type type;
        private final List<Statement> statements;

        public ParsedSection(PluginDefinition.Type type, List<Statement> statements) {
            this.type = type;
            this.statements = statements;
        }

        public PluginDefinition.Type getType() {
            return type;
        }

        public List<Statement> getStatements() {
            return statements;
        }
    }

    private static final class ElseIfClause {
        final Expression condition;
        final List<Statement> body;

        ElseIfClause(Expression condition, List<Statement> body) {
            this.condition = condition;
            this.body = body;
        }
    }
}
