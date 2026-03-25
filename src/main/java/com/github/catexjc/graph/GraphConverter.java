// Copyright 2026 Christopher Norman
package com.github.catexjc.graph;

import com.github.catexjc.core.CategoryObject;
import com.github.catexjc.core.FiniteCategory;
import com.github.catexjc.core.Morphism;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts a {@link FiniteCategory} into a {@link DirectedGraph}.
 *
 * <p>Mapping rules:
 * <ul>
 *   <li>Each category object becomes a vertex (using its label).</li>
 *   <li>Each <em>non-identity</em> morphism becomes a directed edge.  Identity
 *       morphisms are typically omitted because they add self-loops that clutter
 *       graph visualisation.  Pass {@code includeIdentities = true} to include
 *       them.</li>
 * </ul>
 */
public final class GraphConverter {

    private GraphConverter() {}

    /**
     * Converts to a directed graph, omitting identity morphisms.
     */
    public static <OL, ML> DirectedGraph<OL, ML> toGraph(final FiniteCategory<OL, ML> category) {
        return toGraph(category, false);
    }

    /**
     * Converts to a directed graph.
     *
     * @param includeIdentities if {@code true}, identity morphisms become self-loop edges
     */
    public static <OL, ML> DirectedGraph<OL, ML> toGraph(
            final FiniteCategory<OL, ML> category, final boolean includeIdentities) {

        final Set<OL> vertices = new LinkedHashSet<>();
        for (final CategoryObject<OL> obj : category.getObjects()) {
            vertices.add(obj.getLabel());
        }

        final List<DirectedGraph.Edge<OL, ML>> edges = new ArrayList<>();
        for (final Morphism<ML, OL> m : category.getMorphisms()) {
            if (!includeIdentities && m.isIdentity()) {
                continue;
            }
            edges.add(new DirectedGraph.Edge<>(
                    m.getLabel(), m.getDomain().getLabel(), m.getCodomain().getLabel()));
        }

        return new DirectedGraph<>(vertices, edges);
    }

    /**
     * Reconstructs a {@link FiniteCategory} from a {@link DirectedGraph}.
     *
     * <p>The resulting category contains:
     * <ul>
     *   <li>One identity morphism per vertex, labelled {@code "id_<vertex>"}.</li>
     *   <li>One morphism per edge, using the edge label.</li>
     *   <li>Composition table populated only with unit-law entries (identity ∘ f and f ∘ identity).</li>
     * </ul>
     *
     * <p>This is a free category over the graph — non-trivial composites are not
     * computed because a bare graph carries no composition information.
     */
    public static <V, E> FiniteCategory<V, String> fromGraph(final DirectedGraph<V, E> graph) {
        final FiniteCategory.Builder<V, String> builder = FiniteCategory.builder();

        // Objects and identity morphisms
        for (final V v : graph.getVertices()) {
            final var obj        = new CategoryObject<>(v);
            final var idMorphism = new com.github.catexjc.core.Morphism<String, V>("id_" + v, obj, obj);
            builder.addObject(obj).addMorphism(idMorphism);
        }

        // Edge morphisms and unit-law compositions
        for (final DirectedGraph.Edge<V, E> edge : graph.getEdges()) {
            final var src   = new CategoryObject<>(edge.source());
            final var tgt   = new CategoryObject<>(edge.target());
            final var idSrc = new com.github.catexjc.core.Morphism<String, V>("id_" + edge.source(), src, src);
            final var idTgt = new com.github.catexjc.core.Morphism<String, V>("id_" + edge.target(), tgt, tgt);
            final var m     = new com.github.catexjc.core.Morphism<String, V>(edge.label().toString(), src, tgt);

            builder.addMorphism(m);
            builder.addComposition(idTgt, m, m);   // id_tgt ∘ m = m
            builder.addComposition(m, idSrc, m);   // m ∘ id_src = m
        }

        return builder.build();
    }
}
