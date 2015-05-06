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
package org.apache.tinkerpop.gremlin.process.traversal.lambda;

import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalEngine;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSideEffects;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.TraversalParent;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class HasNextTraversal<S> implements Traversal.Admin<S, Boolean>, TraversalParent {

    private Traversal.Admin<S, ?> hasNextTraversal;

    public HasNextTraversal(final Admin<S, ?> hasNextTraversal) {
        this.hasNextTraversal = hasNextTraversal;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Boolean next() {
        return this.hasNextTraversal.hasNext();
    }

    @Override
    public void addStart(final Traverser<S> start) {
        this.hasNextTraversal.addStart(start);
    }

    @Override
    public String toString() {
        return this.hasNextTraversal.toString();
    }

    @Override
    public void setStrategies(final TraversalStrategies strategies) {
        this.hasNextTraversal.setStrategies(strategies);
    }

    @Override
    public TraversalStrategies getStrategies() {
        return this.hasNextTraversal.getStrategies();
    }

    @Override
    public void setSideEffects(final TraversalSideEffects sideEffects) {
        this.hasNextTraversal.setSideEffects(sideEffects);
    }

    @Override
    public TraversalSideEffects getSideEffects() {
        return this.hasNextTraversal.getSideEffects();
    }

    @Override
    public void setParent(final TraversalParent holder) {
        this.hasNextTraversal.setParent(holder);
    }

    @Override
    public TraversalParent getParent() {
        return this.hasNextTraversal.getParent();
    }

    @Override
    public Set<TraverserRequirement> getTraverserRequirements() {
        return this.hasNextTraversal.getTraverserRequirements();
    }

    @Override
    public List<Step> getSteps() {
        return this.hasNextTraversal.getSteps();
    }

    @Override
    public <S2, E2> Traversal.Admin<S2, E2> addStep(final int index, final Step<?, ?> step) throws IllegalStateException {
        return this.hasNextTraversal.addStep(index, step);
    }

    @Override
    public <S2, E2> Traversal.Admin<S2, E2> removeStep(int index) throws IllegalStateException {
        return this.hasNextTraversal.removeStep(index);
    }

    @Override
    public HasNextTraversal<S> clone() {
        try {
            final HasNextTraversal<S> clone = (HasNextTraversal<S>) super.clone();
            clone.hasNextTraversal = this.hasNextTraversal.clone();
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isLocked() {
        return true;
    }

    @Override
    public void applyStrategies() throws IllegalStateException {
        this.hasNextTraversal.applyStrategies();
    }

    @Override
    public TraversalEngine getEngine() {
        return this.hasNextTraversal.getEngine();
    }

    @Override
    public void setEngine(final TraversalEngine engine) {
        this.hasNextTraversal.setEngine(engine);
    }

    @Override
    public void reset() {
        this.hasNextTraversal.reset();
    }

    @Override
    public Optional<Graph> getGraph() {
        return Optional.empty();
    }

    @Override
    public <S, E> List<Traversal.Admin<S, E>> getLocalChildren() {
        return (List) Collections.singletonList(this.hasNextTraversal);
    }



}