// Copyright 2026 Christopher Norman
package com.github.catexjc.render.internal;

import com.github.catexjc.hasse.HasseDiagram;
import com.github.catexjc.render.RenderOptions;

import java.util.*;

/**
 * Assigns (x, y) coordinates to nodes in a {@link HasseDiagram} using a
 * rank-based (Sugiyama-inspired) layered layout.
 *
 * <p>Layout rules:
 * <ul>
 *   <li>Y increases downward (SVG convention).  Rank-0 (bottom) nodes are
 *       placed near the bottom of the canvas; higher-ranked nodes move up.</li>
 *   <li>Within each layer nodes are evenly distributed across the canvas width,
 *       centred horizontally.</li>
 * </ul>
 */
public final class LayeredLayout {

    private LayeredLayout() {}

    /**
     * Computes a position map for every node in the diagram.
     *
     * @param diagram the Hasse diagram whose nodes are to be laid out
     * @param opts    render options (width, height, margin, layerHeight)
     * @return a map from node to its (x, y) SVG position
     */
    public static <E> Map<E, Point> layout(final HasseDiagram<E> diagram, final RenderOptions opts) {
        final Map<E, Point> positions = new LinkedHashMap<>();

        final int layers     = diagram.maxRank() + 1;
        final double usableH = opts.height - 2.0 * opts.margin;
        final double usableW = opts.width  - 2.0 * opts.margin;

        // Vertical step: evenly distribute layers; clamp to layerHeight preference
        final double layerStep = (layers > 1)
                ? Math.min(opts.layerHeight, usableH / (layers - 1))
                : 0;

        for (int rank = 0; rank <= diagram.maxRank(); rank++) {
            final List<E> layer = diagram.layer(rank);
            final int     n     = layer.size();
            // y: rank 0 at bottom, maxRank at top
            final double  y = opts.margin + (diagram.maxRank() - rank) * layerStep;

            for (int i = 0; i < n; i++) {
                final double x = opts.margin + (n == 1
                        ? usableW / 2.0
                        : usableW * i / (n - 1));
                positions.put(layer.get(i), new Point(x, y));
            }
        }
        return positions;
    }

    // -------------------------------------------------------------------------
    // Utility: build a HasseDiagram-like structure from a generic DAG
    // so that GraphRenderer can reuse this layout.
    // -------------------------------------------------------------------------

    /**
     * Assigns ranks to nodes in an arbitrary directed acyclic graph using a
     * longest-path computation from source nodes (in-degree 0).
     *
     * @param nodes    all nodes
     * @param edges    adjacency list (from → to)
     * @return rank map (sources get rank 0)
     * @throws IllegalArgumentException if the graph has a cycle
     */
    public static <V> Map<V, Integer> computeRanks(final Set<V> nodes, final Map<V, List<V>> edges) {
        final Map<V, Integer> inDegree = new LinkedHashMap<>();
        for (final V v : nodes) {
            inDegree.put(v, 0);
        }
        for (final List<V> targets : edges.values()) {
            for (final V t : targets) {
                inDegree.merge(t, 1, Integer::sum);
            }
        }

        final Map<V, Integer> rank = new LinkedHashMap<>();
        for (final V v : nodes) {
            rank.put(v, 0);
        }

        final Deque<V> queue = new ArrayDeque<>();
        for (final V v : nodes) {
            if (inDegree.get(v) == 0) {
                queue.add(v);
            }
        }

        int processed = 0;
        while (!queue.isEmpty()) {
            final V cur = queue.poll();
            processed++;
            for (final V next : edges.getOrDefault(cur, Collections.emptyList())) {
                final int candidate = rank.get(cur) + 1;
                if (candidate > rank.get(next)) {
                    rank.put(next, candidate);
                }
                if (inDegree.merge(next, -1, Integer::sum) == 0) {
                    queue.add(next);
                }
            }
        }
        if (processed != nodes.size()) {
            throw new IllegalArgumentException("Graph contains a cycle; cannot use layered layout.");
        }
        return rank;
    }

    /** Lays out an arbitrary rank map against a canvas. */
    public static <V> Map<V, Point> layoutRanks(final Map<V, Integer> rankMap,
                                                  final Map<Integer, List<V>> layers,
                                                  final int maxRank,
                                                  final RenderOptions opts) {
        final Map<V, Point> positions = new LinkedHashMap<>();
        final double usableH = opts.height - 2.0 * opts.margin;
        final double usableW = opts.width  - 2.0 * opts.margin;
        final double layerStep = (maxRank > 0)
                ? Math.min(opts.layerHeight, usableH / maxRank)
                : 0;

        for (final Map.Entry<Integer, List<V>> entry : layers.entrySet()) {
            final int rank     = entry.getKey();
            final List<V> layer = entry.getValue();
            final int n        = layer.size();
            final double y     = opts.margin + (maxRank - rank) * layerStep;
            for (int i = 0; i < n; i++) {
                final double x = opts.margin + (n == 1
                        ? usableW / 2.0
                        : usableW * i / (n - 1));
                positions.put(layer.get(i), new Point(x, y));
            }
        }
        return positions;
    }
}
