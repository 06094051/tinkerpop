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

package org.apache.tinkerpop.gremlin.python

import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.python.util.SymbolHelper
import org.apache.tinkerpop.gremlin.util.CoreImports

import java.lang.reflect.Modifier

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalSourceGenerator {

    public static void create(final String traversalSourceFile) {

        final StringBuilder pythonClass = new StringBuilder()

        pythonClass.append("""'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
'''
""")
        pythonClass.append("from abc import abstractmethod\n")
        pythonClass.append("from aenum import Enum\n")
        pythonClass.append("import statics\n")

        pythonClass.append("""
class Traversal(object):
    def __init__(self, graph, traversal_strategies, bytecode):
        self.graph = graph
        self.traversal_strategies = traversal_strategies
        self.bytecode = bytecode
        self.results = None
        self.last_traverser = None
        self.bindings = {}

    def __repr__(self):
        return self.graph.translator.translate(self.bytecode)

    def __getitem__(self, index):
        if isinstance(index, int):
            return self.range(index, index + 1)
        elif isinstance(index, slice):
            return self.range(index.start, index.stop)
        else:
            raise TypeError("Index must be int or slice")

    def __getattr__(self, key):
        return self.values(key)

    def __iter__(self):
        return self

    def __next__(self):
        if self.results is None:
            self.traversal_strategies.apply_strategies(self)
        if self.last_traverser is None:
            self.last_traverser = next(self.results)
        object = self.last_traverser.object
        self.last_traverser.bulk = self.last_traverser.bulk - 1
        if self.last_traverser.bulk <= 0:
            self.last_traverser = None
        return object

    def toList(self):
        return list(iter(self))

    def toSet(self):
        return set(iter(self))

    def next(self, amount=None):
        if amount is None:
            return self.__next__()
        else:
            count = 0
            tempList = []
            while count < amount:
                count = count + 1
                try: temp = self.__next__()
                except StopIteration: return tempList
                tempList.append(temp)
            return tempList

""")

///////////
// Enums //
///////////
        for (final Class<? extends Enum> enumClass : CoreImports.getClassImports()
                .findAll { Enum.class.isAssignableFrom(it) }
                .sort { a, b -> a.getSimpleName() <=> b.getSimpleName() }
                .collect()) {
            pythonClass.append("${enumClass.getSimpleName()} = Enum('${enumClass.getSimpleName()}', '");
            enumClass.getEnumConstants()
                    .sort { a, b -> a.name() <=> b.name() }
                    .each { value -> pythonClass.append("${SymbolHelper.toPython(value.name())} "); }
            pythonClass.deleteCharAt(pythonClass.length() - 1).append("')\n\n")
            enumClass.getEnumConstants().each { value ->
                pythonClass.append("statics.add_static('${SymbolHelper.toPython(value.name())}', ${value.getDeclaringClass().getSimpleName()}.${SymbolHelper.toPython(value.name())})\n");
            }
            pythonClass.append("\n");
        }
        //////////////

        pythonClass.append("""class P(object):
   def __init__(self, operator, value, other=None):
      self.operator = operator
      self.value = value
      self.other = other
""")
        P.class.getMethods()
                .findAll { Modifier.isStatic(it.getModifiers()) }
                .findAll { P.class.isAssignableFrom(it.returnType) }
                .collect { SymbolHelper.toPython(it.name) }
                .unique()
                .sort { a, b -> a <=> b }
                .each { method ->
            pythonClass.append(
                    """   @staticmethod
   def ${method}(*args):
      return P("${SymbolHelper.toJava(method)}", *args)
""")
        };
        pythonClass.append("""   def _and(self, arg):
      return P("_and", arg, self)
   def _or(self, arg):
      return P("_or", arg, self)
""")
        pythonClass.append("\n")
        P.class.getMethods()
                .findAll { Modifier.isStatic(it.getModifiers()) }
                .findAll { !it.name.equals("clone") }
                .findAll { P.class.isAssignableFrom(it.getReturnType()) }
                .collect { SymbolHelper.toPython(it.name) }
                .unique()
                .sort { a, b -> a <=> b }
                .forEach {
            pythonClass.append("def ${it}(*args):\n").append("      return P.${it}(*args)\n\n")
            pythonClass.append("statics.add_static('${it}',${it})\n\n")
        }
        pythonClass.append("\n")
        //////////////

        pythonClass.append("""class RawExpression(object):
   def __init__(self, *args):
      self.bindings = dict()
      self.parts = [self._process_arg(arg) for arg in args]

   def _process_arg(self, arg):
      if isinstance(arg, tuple) and 2 == len(arg) and isinstance(arg[0], str):
         self.bindings[arg[0]] = arg[1]
         return Raw(arg[0])
      else:
         return Raw(arg)

class Raw(object):
   def __init__(self, value):
      self.value = value

   def __str__(self):
      return str(self.value)

""")

        pythonClass.append("""
'''
TRAVERSAL STRATEGIES
'''

class TraversalStrategies(object):
    global_cache = {}

    def __init__(self, traversal_strategies):
        self.traversal_strategies = traversal_strategies
        return

    def apply_strategies(self, traversal):
        for traversal_strategy in self.traversal_strategies:
            traversal_strategy.apply(traversal)
        return


class TraversalStrategy(object):
    @abstractmethod
    def apply(self, traversal):
        return

'''
BYTECODE AND TRANSLATOR
'''

class Bytecode(object):
    def __init__(self, bytecode=None):
        self.source_instructions = []
        self.step_instructions = []
        if bytecode is not None:
            self.source_instructions = list(bytecode.source_instructions)
            self.step_instructions = list(bytecode.step_instructions)

    def add_source(self, source_name, *args):
        newArgs = ()
        for arg in args:
            newArgs = newArgs + (Bytecode.__convertArgument(arg),)
        self.source_instructions.append((source_name, newArgs))
        return

    def add_step(self, step_name, *args):
        newArgs = ()
        for arg in args:
            newArgs = newArgs + (Bytecode.__convertArgument(arg),)
        self.step_instructions.append((step_name, newArgs))
        return

    @staticmethod
    def __convertArgument(arg):
        if isinstance(arg, Traversal):
            return arg.bytecode
        else:
            return arg


TO_JAVA_MAP = {"_global": "global", "_as": "as", "_in": "in", "_and": "and",
               "_or": "or", "_is": "is", "_not": "not", "_from": "from",
               "Cardinality": "VertexProperty.Cardinality", "Barrier": "SackFunctions.Barrier"}


class Translator(object):
    def __init__(self, traversal_source, anonymous_traversal, target_language):
        self.traversal_source = traversal_source
        self.anonymous_traversal = anonymous_traversal
        self.target_language = target_language

    @abstractmethod
    def translate(self, bytecode):
        return

    @abstractmethod
    def __repr__(self):
        return "translator[" + self.traversal_source + ":" + self.target_language + "]"


class SymbolHelper(object):
    @staticmethod
    def toJava(symbol):
        if (symbol in TO_JAVA_MAP):
            return TO_JAVA_MAP[symbol]
        else:
            return symbol

    @staticmethod
    def mapEnum(enum):
        if (enum in enumMap):
            return enumMap[enum]
        else:
            return enum
""")
        //////////////

        // save to a python file
        final File file = new File(traversalSourceFile);
        file.delete()
        pythonClass.eachLine { file.append(it + "\n") }
    }
}