package com.catex.render;

import com.catex.hasse.HasseDiagram;
import com.catex.render.internal.LayeredLayout;
import com.catex.render.internal.Point;
import com.catex.render.internal.SvgCanvas;

import java.util.Map;

/**
 * Renders a {@link HasseDiagram} to SVG.
 *
 * <p>Cover edges are drawn as straight lines pointing upward (no arrowheads —
 * the upward direction encodes the order by convention).  Nodes are laid out
 * in horizontal layers grouped by rank, with rank 0 at the bottom.
 *
 * @param <E> element type
 */
public final class HasseDiagramRenderer<E> implements Renderer<HasseDiagram<E>> {

    @Override
    public String renderSvg(HasseDiagram<E> diagram, RenderOptions opts) {
        final SvgCanvas canvas = new SvgCanvas(opts.width, opts.height, "#FAFAFA");

        final Map<E, Point> pos = LayeredLayout.layout(diagram, opts);

        // Edges (drawn before nodes so they sit underneath)
        for (HasseDiagram.Cover<E> cover : diagram.getCovers()) {
            final Point src = pos.get(cover.lower());
            final Point tgt = pos.get(cover.upper());

            // Shorten line to node boundaries
            final Point from = src.edgeToward(tgt, opts.nodeRadius + 2);
            final Point to   = tgt.edgeToward(src, opts.nodeRadius + 2);

            canvas.line(from.x(), from.y(), to.x(), to.y(),
                        opts.edgeColor, opts.edgeStrokeWidth, null);
        }

        // Nodes
        for (E node : diagram.getNodes()) {
            final Point p = pos.get(node);
            canvas.circle(p.x(), p.y(), opts.nodeRadius,
                          opts.nodeColor, opts.nodeStroke, opts.nodeStrokeWidth);
            canvas.text(p.x(), p.y(), node.toString(),
                        opts.fontSize, "middle", opts.labelColor);
        }

        // Rank labels on left margin
        for (int rank = 0; rank <= diagram.maxRank(); rank++) {
            if (diagram.layer(rank).isEmpty()) {
                continue;
            }
            final Point sample = pos.get(diagram.layer(rank).get(0));
            canvas.text(opts.margin / 2.0, sample.y(),
                        "r" + rank, opts.fontSize - 2, "middle", "#999999");
        }

        return canvas.build();
    }
}
