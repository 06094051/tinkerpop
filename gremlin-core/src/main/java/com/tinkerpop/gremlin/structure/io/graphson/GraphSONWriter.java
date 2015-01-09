package com.tinkerpop.gremlin.structure.io.graphson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.io.GraphWriter;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A @{link GraphWriter} that writes a graph and its elements to a JSON-based representation. This implementation
 * only supports JSON data types and is therefore lossy with respect to data types (e.g. a float will become a double).
 * Further note that serialized {@code Map} objects do not support complex types for keys.  {@link Edge} and
 * {@link Vertex} objects are serialized to {@code Map} instances. If an
 * {@link com.tinkerpop.gremlin.structure.Element} is used as a key, it is coerced to its identifier.  Other complex
 * objects are converted via {@link Object#toString()} unless a custom serializer is supplied.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphSONWriter implements GraphWriter {
    private final ObjectMapper mapper;

    private GraphSONWriter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void writeGraph(final OutputStream outputStream, final Graph g) throws IOException {
        this.mapper.writeValue(outputStream, new GraphSONGraph(g));
    }

    @Override
    public void writeVertex(final OutputStream outputStream, final Vertex v, final Direction direction) throws IOException {
        this.mapper.writeValue(outputStream, new GraphSONVertex(v, direction));
    }

    @Override
    public void writeVertex(final OutputStream outputStream, final Vertex v) throws IOException {
        this.mapper.writeValue(outputStream, v);
    }

    @Override
    public void writeEdge(final OutputStream outputStream, final Edge e) throws IOException {
        this.mapper.writeValue(outputStream, e);
    }

    @Override
    public void writeVertices(final OutputStream outputStream, final Traversal<?, Vertex> traversal, final Direction direction) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        while (traversal.hasNext()) {
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                writeVertex(baos, traversal.next(), direction);
                writer.write(new String(baos.toByteArray()));
                writer.newLine();
            }
        }

        writer.flush();
    }

    @Override
    public void writeVertices(final OutputStream outputStream, final Traversal<?, Vertex> traversal) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        while (traversal.hasNext()) {
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                writeVertex(baos, traversal.next());
                writer.write(new String(baos.toByteArray()));
                writer.newLine();
            }
        }

        writer.flush();
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {
        private boolean loadCustomModules = false;
        private boolean normalize = false;
        private List<SimpleModule> customModules = new ArrayList<>();
        private boolean embedTypes = false;

        private GraphSONObjectMapper overridingMapper = null;

        private Builder() {
        }

        /**
         * Supply a custom module for serialization/deserialization.
         */
        public Builder addCustomModule(final SimpleModule custom) {
            this.customModules.add(custom);
            return this;
        }

        /**
         * Try to load {@code SimpleModule} instances from the current classpath.  These are loaded in addition to
         * the one supplied to the {@link #addCustomModule(com.fasterxml.jackson.databind.module.SimpleModule)};
         */
        public Builder loadCustomModules(final boolean loadCustomModules) {
            this.loadCustomModules = loadCustomModules;
            return this;
        }

        /**
         * Set to true in order to embed data types into the JSON so as to explicitly preserve data types.
         */
        public Builder embedTypes(final boolean embedTypes) {
            this.embedTypes = embedTypes;
            return this;
        }

        /**
         * Normalized output is deterministic with respect to the order of elements and properties in the resulting
         * XML document, and is compatible with line diff-based tools such as Git. Note: normalized output is
         * sideEffects-intensive and is not appropriate for very large graphs.
         *
         * @param normalize whether to normalize the output.
         */
        public Builder normalize(final boolean normalize) {
            this.normalize = normalize;
            return this;
        }

        /**
         * Override all of the builder options with this mapper.  If this value is set to something other than
         * null then that value will be used to construct the writer.
         */
        public Builder overridingMapper(final GraphSONObjectMapper mapper) {
            this.overridingMapper = mapper;
            return this;
        }

        public GraphSONWriter create() {
            if (overridingMapper != null) return new GraphSONWriter(overridingMapper);

            final GraphSONObjectMapper.Builder builder = GraphSONObjectMapper.build();
            customModules.forEach(builder::addCustomModule);
            final GraphSONObjectMapper mapper = builder.loadCustomModules(loadCustomModules)
                    .normalize(normalize)
                    .embedTypes(embedTypes).create();
            return new GraphSONWriter(mapper);
        }
    }
}
