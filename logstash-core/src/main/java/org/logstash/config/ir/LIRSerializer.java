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

package org.logstash.config.ir;

import org.logstash.common.SourceWithMetadata;
import org.logstash.config.ir.graph.BooleanEdge;
import org.logstash.config.ir.graph.Edge;
import org.logstash.config.ir.graph.Graph;
import org.logstash.config.ir.graph.IfVertex;
import org.logstash.config.ir.graph.PlainEdge;
import org.logstash.config.ir.graph.PluginVertex;
import org.logstash.config.ir.graph.QueueVertex;
import org.logstash.config.ir.graph.SeparatorVertex;
import org.logstash.config.ir.graph.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes a PipelineIR graph into a Map/List structure suitable for the monitoring API.
 * <p>
 * Corresponds to Ruby {@code LogStash::Config::LIRSerializer}.
 * Converts the Java IR (PipelineIR, Graph, Vertex) into nested Maps and Lists.
 * </p>
 */
public final class LIRSerializer {

    private final PipelineIR lirPipeline;

    private LIRSerializer(PipelineIR lirPipeline) {
        this.lirPipeline = lirPipeline;
    }

    public static Map<String, Object> serialize(PipelineIR lirPipeline) {
        return new LIRSerializer(lirPipeline).doSerialize();
    }

    private Map<String, Object> doSerialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("hash", lirPipeline.uniqueHash());
        result.put("type", "lir");
        result.put("version", "0.0.0");

        Map<String, Object> graphMap = new HashMap<>();
        graphMap.put("vertices", serializeVertices());
        graphMap.put("edges", serializeEdges());
        result.put("graph", graphMap);

        return result;
    }

    private List<Map<String, Object>> serializeVertices() {
        List<Map<String, Object>> vertices = new ArrayList<>();
        for (Vertex v : lirPipeline.getGraph().getVertices()) {
            Map<String, Object> serialized = serializeVertex(v);
            if (serialized != null) {
                vertices.add(serialized);
            }
        }
        return vertices;
    }

    private List<Map<String, Object>> serializeEdges() {
        return removeSeparatorsFromEdges(lirPipeline.getGraph().getEdges());
    }

    private Map<String, Object> serializeVertex(Vertex v) {
        Map<String, Object> vertexMap;

        if (v instanceof PluginVertex) {
            vertexMap = serializePluginVertex((PluginVertex) v);
        } else if (v instanceof IfVertex) {
            vertexMap = serializeIfVertex((IfVertex) v);
        } else if (v instanceof QueueVertex) {
            vertexMap = new HashMap<>();
        } else if (v instanceof SeparatorVertex) {
            return null;
        } else {
            throw new IllegalArgumentException("Unexpected vertex type: " + v.getClass().getName());
        }

        return decorateVertex(v, vertexMap);
    }

    private Map<String, Object> decorateVertex(Vertex v, Map<String, Object> vertexMap) {
        vertexMap.put("meta", formatSourceWithMetadata(v.getSourceWithMetadata()));
        vertexMap.put("id", v.getId());
        vertexMap.put("explicit_id", v.getExplicitId() != null);
        vertexMap.put("type", getVertexTypeName(v));
        return vertexMap;
    }

    private Map<String, Object> serializePluginVertex(PluginVertex v) {
        PluginDefinition pd = v.getPluginDefinition();
        Map<String, Object> map = new HashMap<>();
        map.put("config_name", pd.getName());
        map.put("plugin_type", pd.getType().toString().toLowerCase());
        return map;
    }

    private Map<String, Object> serializeIfVertex(IfVertex v) {
        Map<String, Object> map = new HashMap<>();
        map.put("condition", v.humanReadableExpression());
        return map;
    }

    private String getVertexTypeName(Vertex v) {
        if (v instanceof PluginVertex) return "plugin";
        if (v instanceof IfVertex) return "if";
        if (v instanceof QueueVertex) return "queue";
        if (v instanceof SeparatorVertex) return "separator";
        return "unknown";
    }

    /**
     * For separator vertices, create new edges bridging across the separator,
     * then remove the separator vertices from the output.
     */
    private List<Map<String, Object>> removeSeparatorsFromEdges(Iterable<Edge> edges) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Edge e : edges) {
            if (e.getTo() instanceof SeparatorVertex) {
                // Bridge across the separator: connect e.from to all outgoing targets
                for (Edge outgoing : e.getTo().getOutgoingEdges()) {
                    try {
                        Edge bridged;
                        if (e instanceof BooleanEdge) {
                            bridged = new BooleanEdge(((BooleanEdge) e).getEdgeType(), e.getFrom(), outgoing.getTo());
                        } else {
                            bridged = PlainEdge.factory.make(e.getFrom(), outgoing.getTo());
                        }
                        result.add(serializeEdge(bridged));
                    } catch (InvalidIRException ex) {
                        throw new RuntimeException("Error bridging separator edge", ex);
                    }
                }
            } else if (e.getFrom() instanceof SeparatorVertex) {
                // Skip edges coming from a separator (they were already handled above)
            } else {
                result.add(serializeEdge(e));
            }
        }
        return result;
    }

    private Map<String, Object> serializeEdge(Edge e) {
        Map<String, Object> edgeMap = new HashMap<>();
        edgeMap.put("from", e.getFrom().getId());
        edgeMap.put("to", e.getTo().getId());
        edgeMap.put("id", e.getId());

        if (e instanceof BooleanEdge) {
            edgeMap.put("when", ((BooleanEdge) e).getEdgeType());
            edgeMap.put("type", "boolean");
        } else {
            edgeMap.put("type", "plain");
        }

        return edgeMap;
    }

    private Map<String, Object> formatSourceWithMetadata(SourceWithMetadata swm) {
        if (swm == null) {
            return null;
        }
        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("protocol", swm.getProtocol());
        sourceMap.put("id", swm.getId());
        sourceMap.put("line", swm.getLine());
        sourceMap.put("column", swm.getColumn());
        // Omit text for security reasons (may contain passwords)

        Map<String, Object> metaMap = new HashMap<>();
        metaMap.put("source", sourceMap);
        return metaMap;
    }
}
