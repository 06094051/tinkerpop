/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.tinkerpop.gremlin.process.traversal.strategy.creation;

import org.apache.tinkerpop.gremlin.process.remote.RemoteGraph;
import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Translator;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.EmptyStep;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.util.ScriptEngineCache;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TranslationStrategy extends AbstractTraversalStrategy<TraversalStrategy.CreationStrategy> implements TraversalStrategy.CreationStrategy {

    private final TraversalSource traversalSource;
    private final Translator translator;

    public TranslationStrategy(final TraversalSource traversalSource, final Translator translator) {
        this.traversalSource = traversalSource;
        this.translator = translator;
    }

    @Override
    public void apply(final Traversal.Admin<?, ?> traversal) {
        if (!(traversal.getParent() instanceof EmptyStep))
            return;

        // if the graph is RemoteGraph, RemoteStrategy will send the traversal
        if (traversal.getGraph().isPresent() && traversal.getGraph().get() instanceof RemoteGraph)
            return;

        final Traversal.Admin<?, ?> translatedTraversal;
        ////////////////
        if (this.translator instanceof Translator.StepTranslator) {
            // reflection based translation
            translatedTraversal = (Traversal.Admin<?, ?>) this.translator.translate(traversal.getBytecode());
        } else if (this.translator instanceof Translator.ScriptTranslator) {
            try {
                // script based translation
                final ScriptEngine scriptEngine = ScriptEngineCache.get(this.translator.getTargetLanguage());
                final Bindings bindings = scriptEngine.createBindings();
                scriptEngine.getContext().getBindings(ScriptContext.ENGINE_SCOPE).forEach(bindings::put);
                bindings.put(this.translator.getTraversalSource().toString(), this.traversalSource);
                translatedTraversal = (Traversal.Admin<?, ?>) scriptEngine.eval(this.translator.translate(traversal.getBytecode()).toString(), bindings);
            } catch (final Exception e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("TranslationStrategy does not know how to process the provided translator type: " + this.translator.getClass().getSimpleName());
        }
        ////////////////
        assert !translatedTraversal.isLocked();
        assert !traversal.isLocked();
        translatedTraversal.getSideEffects().mergeInto(traversal.getSideEffects());
        TraversalHelper.removeAllSteps(traversal);
        TraversalHelper.removeToTraversal((Step) translatedTraversal.getStartStep(), EmptyStep.instance(), traversal);
    }

    public TraversalSource getTraversalSource() {
        return this.traversalSource;
    }

    public Translator getTranslator() {
        return this.translator;
    }

}
