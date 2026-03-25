// Copyright 2026 Christopher Norman
package com.github.catexjc.hasse;

import java.util.*;

/**
 * An immutable Hasse diagram — the canonical graphical representation of a
 * partially ordered set.
 *
 * <p>A Hasse diagram retains only the <em>cover</em> relations of the poset
 * (immediate neighbours), discarding all edges implied by transitivity.  This
 * makes the structure much sparser and far easier to read than the full order
 * graph.
 *
 * <p>Each node is assigned a non-negative integer <em>rank</em> equal to the
 * length of the longest chain from any minimal element up to that node.
 * Minimal elements have rank 0; the top element of a lattice has the maximum
 * rank.  Nodes of equal rank form a <em>layer</em>, and layers are indexed
 * from 0 (bottom) upward.
 *
 * @param <E> element type
 */
public final class HasseDiagram<E> {

    /** A directed cover relation: {@code lower} is immediately below {@code upper}. */
    public record Cover<E>(E lower, E upper) {
        @Override public String toString() { return lower + " ≺ " + upper; }
    }

    private final Set<E>             nodes;
    private final List<Cover<E>>     covers;
    private final Map<E, Integer>    rankMap;
    private final Map<Integer, List<E>> layers;   // rank → ordered list of nodes
    private final int                maxRank;

    HasseDiagram(final Set<E> nodes, final List<Cover<E>> covers, final Map<E, Integer> rankMap) {
        this.nodes   = Collections.unmodifiableSet(new LinkedHashSet<>(nodes));
        this.covers  = Collections.unmodifiableList(new ArrayList<>(covers));
        this.rankMap = Collections.unmodifiableMap(new LinkedHashMap<>(rankMap));

        int max = 0;
        final Map<Integer, List<E>> layersMut = new LinkedHashMap<>();
        for (final Map.Entry<E, Integer> e : rankMap.entrySet()) {
            final int r = e.getValue();
            max = Math.max(max, r);
            layersMut.computeIfAbsent(r, k -> new ArrayList<>()).add(e.getKey());
        }
        this.maxRank = max;
        final Map<Integer, List<E>> layersImmutable = new LinkedHashMap<>();
        for (final var entry : layersMut.entrySet()) {
            layersImmutable.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        this.layers = Collections.unmodifiableMap(layersImmutable);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Set<E>         getNodes()  { return nodes; }
    public List<Cover<E>> getCovers() { return covers; }
    public int            maxRank()   { return maxRank; }

    /** The rank (layer index, 0 = bottom) of the given node. */
    public int rank(final E node) {
        final Integer r = rankMap.get(node);
        if (r == null) {
            throw new NoSuchElementException("Node not in diagram: " + node);
        }
        return r;
    }

    /**
     * All nodes at the given rank, in insertion order.
     * Returns an empty list for ranks that have no nodes.
     */
    public List<E> layer(final int rank) {
        return layers.getOrDefault(rank, Collections.emptyList());
    }

    /** All nodes ordered bottom-to-top, layer by layer. */
    public List<E> nodesByRank() {
        final List<E> result = new ArrayList<>(nodes.size());
        for (int r = 0; r <= maxRank; r++) {
            result.addAll(layer(r));
        }
        return result;
    }

    /** Cover edges whose {@code lower} node is {@code node} (edges going up from node). */
    public List<Cover<E>> coversAbove(final E node) {
        final List<Cover<E>> result = new ArrayList<>();
        for (final Cover<E> c : covers) {
            if (c.lower().equals(node)) {
                result.add(c);
            }
        }
        return result;
    }

    /** Cover edges whose {@code upper} node is {@code node} (edges coming into node from below). */
    public List<Cover<E>> coversBelow(final E node) {
        final List<Cover<E>> result = new ArrayList<>();
        for (final Cover<E> c : covers) {
            if (c.upper().equals(node)) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "HasseDiagram{nodes=" + nodes.size() + ", covers=" + covers.size()
                + ", layers=" + (maxRank + 1) + "}";
    }
}
