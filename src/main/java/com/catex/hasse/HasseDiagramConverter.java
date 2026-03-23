package com.catex.hasse;

import com.catex.core.FiniteCategory;
import com.catex.lattice.Lattice;
import com.catex.poset.PartialOrder;
import com.catex.poset.PosetConverter;

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
    public static <E> HasseDiagram<E> fromPoset(PartialOrder<E> poset) {
        return build(poset.getElements(), toHasseCovers(poset.hasseCovers()));
    }

    /** Build a Hasse diagram from a lattice (uses its underlying partial order). */
    public static <E> HasseDiagram<E> fromLattice(Lattice<E> lattice) {
        return fromPoset(lattice.getOrder());
    }

    /**
     * Build a Hasse diagram from a thin finite category (one that represents a poset).
     *
     * @throws IllegalArgumentException if the category is not thin
     */
    public static <OL, ML> HasseDiagram<OL> fromCategory(FiniteCategory<OL, ML> category) {
        return fromPoset(PosetConverter.toPoset(category));
    }

    // -------------------------------------------------------------------------
    // Core builder
    // -------------------------------------------------------------------------

    private static <E> List<HasseDiagram.Cover<E>> toHasseCovers(
            List<PartialOrder.Cover<E>> src) {
        List<HasseDiagram.Cover<E>> result = new ArrayList<>(src.size());
        for (PartialOrder.Cover<E> c : src)
            result.add(new HasseDiagram.Cover<>(c.lower(), c.upper()));
        return result;
    }

    /**
     * Assigns ranks via longest-path from minimal elements (Kahn's algorithm on
     * the cover DAG), then constructs the {@link HasseDiagram}.
     */
    static <E> HasseDiagram<E> build(Set<E> nodes, List<HasseDiagram.Cover<E>> covers) {

        // Build adjacency: lower → list of upper neighbours
        Map<E, List<E>> upNeighbours = new LinkedHashMap<>();
        Map<E, Integer> inDegree     = new LinkedHashMap<>();
        for (E n : nodes) { upNeighbours.put(n, new ArrayList<>()); inDegree.put(n, 0); }
        for (HasseDiagram.Cover<E> c : covers) {
            upNeighbours.get(c.lower()).add(c.upper());
            inDegree.merge(c.upper(), 1, Integer::sum);
        }

        // Rank map: initialise to 0
        Map<E, Integer> rank = new LinkedHashMap<>();
        for (E n : nodes) rank.put(n, 0);

        // Kahn's BFS — seed with nodes that have in-degree 0 (minimal elements)
        Deque<E> queue = new ArrayDeque<>();
        for (E n : nodes) if (inDegree.get(n) == 0) queue.add(n);

        while (!queue.isEmpty()) {
            E cur = queue.poll();
            for (E upper : upNeighbours.get(cur)) {
                // Longest-path update
                int candidate = rank.get(cur) + 1;
                if (candidate > rank.get(upper)) rank.put(upper, candidate);

                int newDeg = inDegree.merge(upper, -1, Integer::sum);
                if (newDeg == 0) queue.add(upper);
            }
        }

        return new HasseDiagram<>(nodes, covers, rank);
    }
}
