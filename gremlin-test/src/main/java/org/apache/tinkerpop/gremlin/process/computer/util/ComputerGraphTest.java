/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.computer.util;

import org.apache.tinkerpop.gremlin.AbstractGremlinTest;
import org.apache.tinkerpop.gremlin.FeatureRequirement;
import org.apache.tinkerpop.gremlin.process.TraversalEngine;
import org.apache.tinkerpop.gremlin.process.UseEngine;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@UseEngine(TraversalEngine.Type.COMPUTER)
public class ComputerGraphTest extends AbstractGremlinTest {

    @Test
    @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_ADD_VERTICES)
    public void shouldFilterHiddenProperties() {
        final ComputerGraph sg = new ComputerGraph(graph, new HashSet<>(Arrays.asList("***hidden-guy")));

        final Vertex v = sg.addVertex("***hidden-guy", "X", "not-hidden-guy", "Y");
        final Iterator<VertexProperty<String>> props = v.properties();
        final VertexProperty v1 = props.next();
        assertEquals("Y", v1.value());
        assertEquals("not-hidden-guy", v1.key());
        assertFalse(props.hasNext());

        final Iterator<String> values = v.values();
        assertEquals("Y", values.next());
        assertFalse(values.hasNext());
    }

    @Test
    @Ignore
    @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_ADD_VERTICES)
    public void shouldAccessHiddenProperties() {
        final ComputerGraph sg = new ComputerGraph(graph, new HashSet<>(new HashSet<>(Arrays.asList("***hidden-guy"))));

        final Vertex v = sg.addVertex("***hidden-guy", "X", "not-hidden-guy", "Y");
        final Iterator<VertexProperty<String>> props = v.properties("***hidden-guy");
        final VertexProperty<String> v1 = props.next();
        assertEquals("X", v1.value());
        assertEquals("***hidden-guy", v1.key());
        assertFalse(props.hasNext());

        final Iterator<String> values = v.values("***hidden-guy");
        assertEquals("X", values.next());
        assertFalse(values.hasNext());
    }

    @Test
    @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_ADD_VERTICES)
    public void shouldHideHiddenKeys() {
        final ComputerGraph sg = new ComputerGraph(graph, new HashSet<>(Arrays.asList("***hidden-guy")));

        final Vertex v = sg.addVertex("***hidden-guy", "X", "not-hidden-guy", "Y");
        final Set<String> keys = v.keys();
        assertTrue(keys.contains("not-hidden-guy"));
        assertFalse(keys.contains("***hidden-guy"));
    }
}
