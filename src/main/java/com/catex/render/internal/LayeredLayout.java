package com.catex.render.internal;

import com.catex.hasse.HasseDiagram;
import com.catex.render.RenderOptions;

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
    public static <E> Map<E, Point> layout(HasseDiagram<E> diagram, RenderOptions opts) {
        Map<E, Point> positions = new LinkedHashMap<>();

        int layers    = diagram.maxRank() + 1;
        double usableH = opts.height - 2.0 * opts.margin;
        double usableW = opts.width  - 2.0 * opts.margin;

        // Vertical step: evenly distribute layers; clamp to layerHeight preference
        double layerStep = (layers > 1)
                ? Math.min(opts.layerHeight, usableH / (layers - 1))
                : 0;

        for (int rank = 0; rank <= diagram.maxRank(); rank++) {
            List<E> layer   = diagram.layer(rank);
            int     n       = layer.size();
            // y: rank 0 at bottom, maxRank at top
            double  y = opts.margin + (diagram.maxRank() - rank) * layerStep;

            for (int i = 0; i < n; i++) {
                double x = opts.margin + (n == 1
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
    public static <V> Map<V, Integer> computeRanks(Set<V> nodes, Map<V, List<V>> edges) {
        Map<V, Integer> inDegree = new LinkedHashMap<>();
        for (V v : nodes) inDegree.put(v, 0);
        for (List<V> targets : edges.values())
            for (V t : targets)
                inDegree.merge(t, 1, Integer::sum);

        Map<V, Integer> rank = new LinkedHashMap<>();
        for (V v : nodes) rank.put(v, 0);

        Deque<V> queue = new ArrayDeque<>();
        for (V v : nodes) if (inDegree.get(v) == 0) queue.add(v);

        int processed = 0;
        while (!queue.isEmpty()) {
            V cur = queue.poll();
            processed++;
            for (V next : edges.getOrDefault(cur, Collections.emptyList())) {
                int candidate = rank.get(cur) + 1;
                if (candidate > rank.get(next)) rank.put(next, candidate);
                if (inDegree.merge(next, -1, Integer::sum) == 0) queue.add(next);
            }
        }
        if (processed != nodes.size())
            throw new IllegalArgumentException("Graph contains a cycle; cannot use layered layout.");
        return rank;
    }

    /** Lays out an arbitrary rank map against a canvas. */
    public static <V> Map<V, Point> layoutRanks(Map<V, Integer> rankMap,
                                                  Map<Integer, List<V>> layers,
                                                  int maxRank,
                                                  RenderOptions opts) {
        Map<V, Point> positions = new LinkedHashMap<>();
        double usableH = opts.height - 2.0 * opts.margin;
        double usableW = opts.width  - 2.0 * opts.margin;
        double layerStep = (maxRank > 0)
                ? Math.min(opts.layerHeight, usableH / maxRank)
                : 0;

        for (Map.Entry<Integer, List<V>> entry : layers.entrySet()) {
            int rank = entry.getKey();
            List<V> layer = entry.getValue();
            int n = layer.size();
            double y = opts.margin + (maxRank - rank) * layerStep;
            for (int i = 0; i < n; i++) {
                double x = opts.margin + (n == 1
                        ? usableW / 2.0
                        : usableW * i / (n - 1));
                positions.put(layer.get(i), new Point(x, y));
            }
        }
        return positions;
    }
}
