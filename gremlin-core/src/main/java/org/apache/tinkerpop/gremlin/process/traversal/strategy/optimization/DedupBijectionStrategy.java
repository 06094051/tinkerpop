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
package org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization;

import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.DedupGlobalStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.OrderGlobalStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.IdentityStep;
import org.apache.tinkerpop.gremlin.process.traversal.lambda.IdentityTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;

import java.util.Arrays;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class DedupBijectionStrategy extends AbstractTraversalStrategy<TraversalStrategy.OptimizationStrategy> implements TraversalStrategy.OptimizationStrategy {

    private static final DedupBijectionStrategy INSTANCE = new DedupBijectionStrategy();

    private DedupBijectionStrategy() {
    }

    private static final List<Class<? extends Step>> BIJECTIVE_PIPES = Arrays.asList(IdentityStep.class, OrderGlobalStep.class);

    @Override
    public void apply(final Traversal.Admin<?, ?> traversal) {
        if (!TraversalHelper.hasStepOfClass(DedupGlobalStep.class, traversal))
            return;

        boolean done = false;
        while (!done) {
            done = true;
            for (int i = 0; i < traversal.getSteps().size(); i++) {
                final Step step1 = traversal.getSteps().get(i);
                if (step1 instanceof DedupGlobalStep && !(((DedupGlobalStep) step1).getLocalChildren().get(0) instanceof IdentityTraversal)) {
                    for (int j = i; j >= 0; j--) {
                        final Step step2 = traversal.getSteps().get(j);
                        if (BIJECTIVE_PIPES.stream().filter(c -> c.isAssignableFrom(step2.getClass())).findAny().isPresent()) {
                            traversal.removeStep(step1);
                            traversal.addStep(j, step1);
                            done = false;
                            break;
                        }
                    }
                }
                if (!done)
                    break;
            }
        }
    }

    public static DedupBijectionStrategy instance() {
        return INSTANCE;
    }
}
