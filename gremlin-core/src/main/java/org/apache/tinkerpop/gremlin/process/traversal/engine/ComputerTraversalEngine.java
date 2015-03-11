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
package org.apache.tinkerpop.gremlin.process.traversal.engine;

import org.apache.tinkerpop.gremlin.process.Traversal;
import org.apache.tinkerpop.gremlin.process.TraversalEngine;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.traversal.step.map.ComputerResultStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.EmptyStep;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.io.Serializable;
import java.util.Optional;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class ComputerTraversalEngine implements TraversalEngine {

    private final transient GraphComputer graphComputer;

    private ComputerTraversalEngine(final GraphComputer graphComputer) {
        this.graphComputer = graphComputer;
    }

    @Override
    public void processTraversal(final Traversal.Admin<?, ?> traversal) {
        if (traversal.getParent() instanceof EmptyStep)
            traversal.addStep(new ComputerResultStep<>(traversal, this.graphComputer, true));
    }

    @Override
    public Type getType() {
        return Type.COMPUTER;
    }

    @Override
    public String toString() {
        return StringFactory.traversalEngineString(this);
    }

    @Override
    public Optional<GraphComputer> getGraphComputer() {
        return Optional.ofNullable(this.graphComputer);
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder implements TraversalEngine.Builder, Serializable {

        private Class<? extends GraphComputer> graphComputerClass;
        private GraphComputer.Isolation isolation = GraphComputer.Isolation.BSP;

        public Builder computer(final Class<? extends GraphComputer> graphComputerClass) {
            this.graphComputerClass = graphComputerClass;
            return this;
        }

        public Builder isolation(final GraphComputer.Isolation isolation) {
            this.isolation = isolation;
            return this;
        }

        public ComputerTraversalEngine create(final Graph graph) {
            return new ComputerTraversalEngine(graph.compute(this.graphComputerClass).isolation(this.isolation));
        }
    }
}
