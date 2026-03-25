// Copyright 2026 Christopher Norman
package com.github.catexjc.render.internal;

import com.github.catexjc.render.RenderOptions;

import java.util.*;

/**
 * Places nodes evenly around a circle — used when the graph has cycles and a
 * layered layout is not applicable.
 */
public final class CircularLayout {

    private CircularLayout() {}

    /**
     * Returns a position for every node, evenly distributed around a circle
     * centred on the canvas.
     *
     * @param nodes ordered collection of nodes (order determines angular position)
     * @param opts  render options (width, height, layoutRadius)
     */
    public static <V> Map<V, Point> layout(final Collection<V> nodes, final RenderOptions opts) {
        final Map<V, Point> positions = new LinkedHashMap<>();
        final List<V> list = new ArrayList<>(nodes);
        final int n = list.size();

        final double cx = opts.width  / 2.0;
        final double cy = opts.height / 2.0;
        double r = Math.min(opts.layoutRadius,
                Math.min(cx, cy) - opts.margin - opts.nodeRadius);
        r = Math.max(r, opts.nodeRadius * 2);

        for (int i = 0; i < n; i++) {
            // Start from the top (-π/2) and go clockwise
            final double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            positions.put(list.get(i), new Point(cx + r * Math.cos(angle),
                                                  cy + r * Math.sin(angle)));
        }
        return positions;
    }
}
