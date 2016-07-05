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

package org.apache.tinkerpop.gremlin.java.translator;

import org.apache.tinkerpop.gremlin.process.computer.Computer;
import org.apache.tinkerpop.gremlin.process.traversal.ByteCode;
import org.apache.tinkerpop.gremlin.process.traversal.Operator;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.SackFunctions;
import org.apache.tinkerpop.gremlin.process.traversal.Translator;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.VerificationException;
import org.apache.tinkerpop.gremlin.process.traversal.util.ConnectiveP;
import org.apache.tinkerpop.gremlin.process.traversal.util.EmptyTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.util.OrP;
import org.apache.tinkerpop.gremlin.process.traversal.util.TranslatorHelper;
import org.apache.tinkerpop.gremlin.python.util.SymbolHelper;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.function.Lambda;
import org.apache.tinkerpop.gremlin.util.iterator.ArrayIterator;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class PythonTranslator implements Translator<String> {

    private static final Set<String> STEP_NAMES = Stream.of(GraphTraversal.class.getMethods()).filter(method -> Traversal.class.isAssignableFrom(method.getReturnType())).map(Method::getName).collect(Collectors.toSet());
    private static final Set<String> NO_STATIC = Stream.of(T.values(), Operator.values())
            .flatMap(arg -> IteratorUtils.stream(new ArrayIterator<>(arg)))
            .map(arg -> ((Enum) arg).name())
            .collect(Collectors.toCollection(() -> new HashSet<>(Arrays.asList("not"))));

    protected String alias;
    protected final boolean importStatics;

    protected PythonTranslator(final String alias, final boolean importStatics) {
        this.alias = alias;
        this.importStatics = importStatics;
    }

    public static PythonTranslator of(final String alias) {
        return new PythonTranslator(alias, false);
    }

    public static PythonTranslator of(final String alias, final boolean importStatics) {
        return new PythonTranslator(alias, importStatics);
    }

    @Override
    public String getAlias() {
        return this.alias;
    }

    @Override
    public String translate(final ByteCode byteCode) {
        final StringBuilder traversalScript = new StringBuilder(this.alias);
        for (final ByteCode.Instruction instruction : byteCode.getStepInstructions()) {
            final String methodName = instruction.getOperator();
            final Object[] arguments = instruction.getArguments();
            final List<Object> objects = TranslatorHelper.flattenArguments(arguments);
            final int size = objects.size();
            if (0 == size)
                traversalScript.append(".").append(SymbolHelper.toPython(methodName)).append("()");
            else if (methodName.equals("range") && 2 == size)
                traversalScript.append("[").append(objects.get(0)).append(":").append(objects.get(1)).append("]");
            else if (methodName.equals("limit") && 1 == size)
                traversalScript.append("[0:").append(objects.get(0)).append("]");
            else if (methodName.equals("values") && 1 == size && traversalScript.length() > 3 && !STEP_NAMES.contains(objects.get(0).toString()))
                traversalScript.append(".").append(objects.get(0));
            else {
                traversalScript.append(".");
                String temp = SymbolHelper.toPython(methodName) + "(";
                for (final Object object : objects) {
                    temp = temp + convertToString(object) + ",";
                }
                traversalScript.append(temp.substring(0, temp.length() - 1)).append(")");
            }

            // clip off __.
            if (this.importStatics && traversalScript.substring(0, 3).startsWith("__.")
                    && !NO_STATIC.stream().filter(name -> traversalScript.substring(3).startsWith(SymbolHelper.toPython(name))).findAny().isPresent()) {
                traversalScript.delete(0, 3);
            }
        }
        final String script = traversalScript.toString();
        if (script.contains("$"))
            throw new VerificationException("Lambdas are currently not supported: " + script, EmptyTraversal.instance());
        return script;
    }

    @Override
    public String getSourceLanguage() {
        return "gremlin-java";
    }

    @Override
    public String getTargetLanguage() {
        return "jython";
    }

    @Override
    public String toString() {
        return StringFactory.translatorString(this);
    }

    ///////

    private String convertToString(final Object object) {
        if (object instanceof String)
            return "\"" + object + "\"";
        else if (object instanceof List) {
            final List<String> list = new ArrayList<>(((List) object).size());
            for (final Object item : (List) object) {
                list.add(convertToString(item));
            }
            return list.toString();
        } else if (object instanceof Long)
            return object + "L";
        else if (object instanceof Boolean)
            return object.equals(Boolean.TRUE) ? "True" : "False";
        else if (object instanceof Class)
            return ((Class) object).getCanonicalName();
        else if (object instanceof VertexProperty.Cardinality)
            return "Cardinality." + object.toString();
        else if (object instanceof SackFunctions.Barrier)
            return "Barrier." + object.toString();
        else if (object instanceof Enum)
            return convertStatic(((Enum) object).getDeclaringClass().getSimpleName() + ".") + SymbolHelper.toPython(object.toString());
        else if (object instanceof P)
            return convertPToString((P) object, new StringBuilder()).toString();
        else if (object instanceof Element)
            return convertToString(((Element) object).id()); // hack
        else if (object instanceof ByteCode)
            return new PythonTranslator("__", this.importStatics).translate(((ByteCode) object));
        else if (object instanceof Computer) {
            return "";
        } else if (object instanceof Lambda) {
            final String lambdaString = ((Lambda) object).getLambdaScript();
            return lambdaString.startsWith("lambda") ? lambdaString : "lambda: \"" + lambdaString + "\"";
        } else
            return null == object ? "" : object.toString();
    }

    private String convertStatic(final String name) {
        return this.importStatics ? "" : name;
    }

    private StringBuilder convertPToString(final P p, final StringBuilder current) {
        if (p instanceof ConnectiveP) {
            final List<P<?>> list = ((ConnectiveP) p).getPredicates();
            for (int i = 0; i < list.size(); i++) {
                convertPToString(list.get(i), current);
                if (i < list.size() - 1)
                    current.append(p instanceof OrP ? "._or(" : "._and(");
            }
            current.append(")");
        } else
            current.append(convertStatic("P.")).append(p.getBiPredicate().toString()).append("(").append(convertToString(p.getValue())).append(")");
        return current;
    }

}
