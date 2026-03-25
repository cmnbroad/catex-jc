// Copyright 2026 Christopher Norman
package com.github.catexjc.render;

import com.github.catexjc.hasse.HasseDiagram;
import com.github.catexjc.hasse.HasseDiagramConverter;
import com.github.catexjc.lattice.Lattice;
import com.github.catexjc.render.internal.LayeredLayout;
import com.github.catexjc.render.internal.Point;
import com.github.catexjc.render.internal.SvgCanvas;

import java.util.Map;
import java.util.Optional;

/**
 * Renders a {@link Lattice} to SVG.
 *
 * <p>Uses the same layered layout as {@link HasseDiagramRenderer} but
 * additionally highlights the top element (⊤) and bottom element (⊥) with
 * distinct colours from {@link RenderOptions#topColor} and
 * {@link RenderOptions#bottomColor}.
 *
 * @param <E> element type
 */
public final class LatticeRenderer<E> implements Renderer<Lattice<E>> {

    @Override
    public String renderSvg(final Lattice<E> lattice, final RenderOptions opts) {
        final HasseDiagram<E> diagram = HasseDiagramConverter.fromLattice(lattice);
        final SvgCanvas canvas = new SvgCanvas(opts.width, opts.height, "#FAFAFA");

        final Map<E, Point> pos = LayeredLayout.layout(diagram, opts);

        final Optional<E> top    = lattice.top();
        final Optional<E> bottom = lattice.bottom();

        // Cover edges
        for (final HasseDiagram.Cover<E> cover : diagram.getCovers()) {
            final Point src  = pos.get(cover.lower());
            final Point tgt  = pos.get(cover.upper());
            final Point from = src.edgeToward(tgt, opts.nodeRadius + 2);
            final Point to   = tgt.edgeToward(src, opts.nodeRadius + 2);
            canvas.line(from.x(), from.y(), to.x(), to.y(),
                        opts.edgeColor, opts.edgeStrokeWidth, null);
        }

        // Nodes
        for (final E node : diagram.getNodes()) {
            final Point p = pos.get(node);
            String fill = opts.nodeColor;
            if (top.isPresent() && top.get().equals(node)) {
                fill = opts.topColor;
            }
            if (bottom.isPresent() && bottom.get().equals(node)) {
                fill = opts.bottomColor;
            }

            canvas.circle(p.x(), p.y(), opts.nodeRadius,
                          fill, opts.nodeStroke, opts.nodeStrokeWidth);
            canvas.text(p.x(), p.y(), node.toString(),
                        opts.fontSize, "middle", opts.labelColor);
        }

        // Legend for top / bottom
        final double lx = opts.width - opts.margin + 5;
        double ly = opts.margin;
        if (top.isPresent()) {
            canvas.circle(lx, ly, 8, opts.topColor, opts.nodeStroke, 1.5);
            canvas.text(lx + 14, ly, "⊤ " + top.get(), opts.fontSize - 1, "start", "#333");
            ly += 22;
        }
        if (bottom.isPresent()) {
            canvas.circle(lx, ly, 8, opts.bottomColor, opts.nodeStroke, 1.5);
            canvas.text(lx + 14, ly, "⊥ " + bottom.get(), opts.fontSize - 1, "start", "#333");
        }

        return canvas.build();
    }
}
