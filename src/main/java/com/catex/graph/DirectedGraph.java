package com.catex.graph;

import java.util.*;

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

    private final Set<V>         vertices;
    private final List<Edge<V,E>> edges;

    DirectedGraph(Set<V> vertices, List<Edge<V, E>> edges) {
        this.vertices = Collections.unmodifiableSet(new LinkedHashSet<>(vertices));
        this.edges    = Collections.unmodifiableList(new ArrayList<>(edges));
    }

    public Set<V>          getVertices() { return vertices; }
    public List<Edge<V,E>> getEdges()    { return edges; }

    /** All edges leaving {@code source}. */
    public List<Edge<V,E>> edgesFrom(V source) {
        final List<Edge<V,E>> result = new ArrayList<>();
        for (Edge<V,E> e : edges) {
            if (e.source().equals(source)) {
                result.add(e);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** All edges arriving at {@code target}. */
    public List<Edge<V,E>> edgesTo(V target) {
        final List<Edge<V,E>> result = new ArrayList<>();
        for (Edge<V,E> e : edges) {
            if (e.target().equals(target)) {
                result.add(e);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns {@code true} if there is at least one edge from {@code u} to {@code v}. */
    public boolean hasEdge(V u, V v) {
        for (Edge<V,E> e : edges) {
            if (e.source().equals(u) && e.target().equals(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "DirectedGraph{vertices=" + vertices.size() + ", edges=" + edges.size() + "}";
    }
}
