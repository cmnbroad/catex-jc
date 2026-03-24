package com.github.catexjc.render;

import com.github.catexjc.graph.DirectedGraph;
import com.github.catexjc.render.internal.CircularLayout;
import com.github.catexjc.render.internal.LayeredLayout;
import com.github.catexjc.render.internal.Point;
import com.github.catexjc.render.internal.SvgCanvas;

import java.util.*;

/**
 * Renders a {@link DirectedGraph} to SVG.
 *
 * <p>If the graph is a DAG, a top-down layered layout is used (sources at the
 * top).  If the graph contains cycles, a circular layout is used instead.
 * Self-loop edges are rendered as small arcs above their node.
 * Edge labels are placed beside the midpoint of each edge.
 *
 * @param <V> vertex label type
 * @param <E> edge label type
 */
public final class GraphRenderer<V, E> implements Renderer<DirectedGraph<V, E>> {

    @Override
    public String renderSvg(final DirectedGraph<V, E> graph, final RenderOptions opts) {
        final String markerId = "arrow";
        final SvgCanvas canvas = new SvgCanvas(opts.width, opts.height, "#FAFAFA");
        canvas.addArrowMarker(markerId, opts.edgeColor, opts.arrowSize);

        final Map<V, Point> pos = computeLayout(graph, opts);

        // Edges (before nodes)
        for (final DirectedGraph.Edge<V, E> edge : graph.getEdges()) {
            final V src = edge.source();
            final V tgt = edge.target();

            if (src.equals(tgt)) {
                // Self-loop
                final Point p = pos.get(src);
                canvas.selfLoop(p.x(), p.y(), opts.nodeRadius,
                                opts.edgeColor, opts.edgeStrokeWidth, markerId);
                // Label above the loop
                canvas.text(p.x(), p.y() - opts.nodeRadius * 2.6,
                            edge.label().toString(), opts.fontSize - 2, "middle", "#555");
                continue;
            }

            final Point srcPt = pos.get(src);
            final Point tgtPt = pos.get(tgt);

            // Check for a parallel reverse edge — if so, curve both
            final boolean hasReverse = graph.hasEdge(tgt, src);
            final double curvature = hasReverse ? 30 : 0;

            final Point from = srcPt.edgeToward(tgtPt, opts.nodeRadius + 2);
            final Point to   = tgtPt.edgeToward(srcPt, opts.nodeRadius + opts.arrowSize + 2);

            if (curvature == 0) {
                canvas.line(from.x(), from.y(), to.x(), to.y(),
                            opts.edgeColor, opts.edgeStrokeWidth, markerId);
            } else {
                canvas.curvedLine(from.x(), from.y(), to.x(), to.y(),
                                  curvature, -curvature,
                                  opts.edgeColor, opts.edgeStrokeWidth, markerId);
            }

            // Edge label: offset perpendicular to the edge direction
            final Point perp = srcPt.perpendicular(tgtPt, 16 + curvature / 2);
            canvas.edgeLabel(from.x(), from.y(), to.x(), to.y(),
                             perp.x(), perp.y(),
                             edge.label().toString(), opts.fontSize - 2, "#333");
        }

        // Nodes
        for (final V vertex : graph.getVertices()) {
            final Point p = pos.get(vertex);
            canvas.circle(p.x(), p.y(), opts.nodeRadius,
                          opts.nodeColor, opts.nodeStroke, opts.nodeStrokeWidth);
            canvas.text(p.x(), p.y(), vertex.toString(),
                        opts.fontSize, "middle", opts.labelColor);
        }

        return canvas.build();
    }

    // -------------------------------------------------------------------------

    private Map<V, Point> computeLayout(final DirectedGraph<V, E> graph, final RenderOptions opts) {
        // Build adjacency map
        final Map<V, List<V>> adj = new LinkedHashMap<>();
        for (final V v : graph.getVertices()) {
            adj.put(v, new ArrayList<>());
        }
        for (final DirectedGraph.Edge<V, E> e : graph.getEdges()) {
            if (!e.source().equals(e.target())) { // skip self-loops for layout
                adj.get(e.source()).add(e.target());
            }
        }

        // Try layered layout (requires DAG)
        try {
            final Map<V, Integer> rankMap = LayeredLayout.computeRanks(graph.getVertices(), adj);
            final int maxRank = rankMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            final Map<Integer, List<V>> layers = new LinkedHashMap<>();
            for (final Map.Entry<V, Integer> e : rankMap.entrySet()) {
                layers.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }
            return LayeredLayout.layoutRanks(rankMap, layers, maxRank, opts);
        } catch (IllegalArgumentException cycleDetected) {
            // Fall back to circular layout
            return CircularLayout.layout(graph.getVertices(), opts);
        }
    }
}
