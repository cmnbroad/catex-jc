// Copyright 2026 Christopher Norman
package com.github.catexjc.hasse;

import com.github.catexjc.core.FiniteCategory;
import com.github.catexjc.lattice.Lattice;
import com.github.catexjc.poset.PartialOrder;
import com.github.catexjc.poset.PosetConverter;

import java.util.*;

/**
 * Converts {@link PartialOrder}, {@link Lattice} and {@link FiniteCategory}
 * instances into {@link HasseDiagram}s.
 *
 * <h3>Algorithm</h3>
 * Cover relations are obtained from {@link PartialOrder#hasseCovers()}.
 * Node ranks are then assigned by a longest-path computation from minimal
 * elements: rank(⊥) = 0, rank(b) = max(rank(a) + 1) over all covers a ≺ b.
 * This is evaluated in topological order via Kahn's algorithm on the cover DAG.
 */
public final class HasseDiagramConverter {

    private HasseDiagramConverter() {}

    // -------------------------------------------------------------------------
    // Public entry points
    // -------------------------------------------------------------------------

    /** Build a Hasse diagram from a partial order. */
    public static <E> HasseDiagram<E> fromPoset(final PartialOrder<E> poset) {
        return build(poset.getElements(), toHasseCovers(poset.hasseCovers()));
    }

    /** Build a Hasse diagram from a lattice (uses its underlying partial order). */
    public static <E> HasseDiagram<E> fromLattice(final Lattice<E> lattice) {
        return fromPoset(lattice.getOrder());
    }

    /**
     * Build a Hasse diagram from a preorder finite category (one that represents a poset).
     *
     * @throws IllegalArgumentException if the category is not a preorder
     */
    public static <OL, ML> HasseDiagram<OL> fromCategory(final FiniteCategory<OL, ML> category) {
        return fromPoset(PosetConverter.toPoset(category));
    }

    // -------------------------------------------------------------------------
    // Core builder
    // -------------------------------------------------------------------------

    private static <E> List<HasseDiagram.Cover<E>> toHasseCovers(
            final List<PartialOrder.Cover<E>> src) {
        final List<HasseDiagram.Cover<E>> result = new ArrayList<>(src.size());
        for (final PartialOrder.Cover<E> c : src) {
            result.add(new HasseDiagram.Cover<>(c.lower(), c.upper()));
        }
        return result;
    }

    /**
     * Assigns ranks via longest-path from minimal elements (Kahn's algorithm on
     * the cover DAG), then constructs the {@link HasseDiagram}.
     */
    static <E> HasseDiagram<E> build(final Set<E> nodes, final List<HasseDiagram.Cover<E>> covers) {

        // Build adjacency: lower → list of upper neighbours
        final Map<E, List<E>> upNeighbours = new LinkedHashMap<>();
        final Map<E, Integer> inDegree     = new LinkedHashMap<>();
        for (final E n : nodes) {
            upNeighbours.put(n, new ArrayList<>());
            inDegree.put(n, 0);
        }
        for (final HasseDiagram.Cover<E> c : covers) {
            upNeighbours.get(c.lower()).add(c.upper());
            inDegree.merge(c.upper(), 1, Integer::sum);
        }

        // Rank map: initialise to 0
        final Map<E, Integer> rank = new LinkedHashMap<>();
        for (final E n : nodes) {
            rank.put(n, 0);
        }

        // Kahn's BFS — seed with nodes that have in-degree 0 (minimal elements)
        final Deque<E> queue = new ArrayDeque<>();
        for (final E n : nodes) {
            if (inDegree.get(n) == 0) {
                queue.add(n);
            }
        }

        while (!queue.isEmpty()) {
            final E cur = queue.poll();
            for (final E upper : upNeighbours.get(cur)) {
                // Longest-path update
                final int candidate = rank.get(cur) + 1;
                if (candidate > rank.get(upper)) {
                    rank.put(upper, candidate);
                }

                final int newDeg = inDegree.merge(upper, -1, Integer::sum);
                if (newDeg == 0) {
                    queue.add(upper);
                }
            }
        }

        return new HasseDiagram<>(nodes, covers, rank);
    }
}
