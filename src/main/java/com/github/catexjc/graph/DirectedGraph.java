package com.github.catexjc.graph;

import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple directed (multi-)graph whose vertices and edges carry labels.
 *
 * <p>Multiple edges between the same pair of vertices are supported (one per
 * non-identity morphism in the source category).
 *
 * @param <V> vertex label type
 * @param <E> edge label type
 */
public final class DirectedGraph<V, E> {

    public record Edge<V, E>(E label, V source, V target) {
        @Override public String toString() {
            return label + ": " + source + " -> " + target;
        }
    }

    /**
     * Identity-equality wrapper so JGraphT treats every inserted edge as a
     * distinct object.  {@code Edge<V,E>} is a record whose {@code equals} and
     * {@code hashCode} are derived from its components; without this wrapper,
     * two edges that share the same {@code (label, source, target)} triple
     * would collide in JGraphT's internal edge map.
     */
    private static final class EdgeHolder<V, E> {
        final Edge<V, E> edge;
        EdgeHolder(final Edge<V, E> edge) { this.edge = edge; }
    }

    private final DirectedPseudograph<V, EdgeHolder<V, E>> graph;

    DirectedGraph(final Set<V> vertices, final List<Edge<V, E>> edges) {
        // null suppliers are valid when edges are added manually via addEdge(u,v,edgeObj)
        this.graph = new DirectedPseudograph<>(null, null, false);
        for (final V v : vertices)       graph.addVertex(v);
        for (final Edge<V, E> e : edges) graph.addEdge(e.source(), e.target(), new EdgeHolder<>(e));
    }

    public Set<V> getVertices() {
        return Collections.unmodifiableSet(graph.vertexSet());
    }

    public List<Edge<V, E>> getEdges() {
        return graph.edgeSet().stream()
                .map(h -> h.edge)
                .collect(Collectors.toUnmodifiableList());
    }

    /** All edges leaving {@code source}. */
    public List<Edge<V, E>> edgesFrom(final V source) {
        return graph.outgoingEdgesOf(source).stream()
                .map(h -> h.edge)
                .collect(Collectors.toUnmodifiableList());
    }

    /** All edges arriving at {@code target}. */
    public List<Edge<V, E>> edgesTo(final V target) {
        return graph.incomingEdgesOf(target).stream()
                .map(h -> h.edge)
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns {@code true} if there is at least one edge from {@code u} to {@code v}. */
    public boolean hasEdge(final V u, final V v) {
        return graph.containsEdge(u, v);
    }

    @Override
    public String toString() {
        return "DirectedGraph{vertices=" + graph.vertexSet().size()
                + ", edges=" + graph.edgeSet().size() + "}";
    }
}
