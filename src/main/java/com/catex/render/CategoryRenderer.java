package com.catex.render;

import com.catex.core.FiniteCategory;
import com.catex.core.Morphism;
import com.catex.render.internal.CircularLayout;
import com.catex.render.internal.LayeredLayout;
import com.catex.render.internal.Point;
import com.catex.render.internal.SvgCanvas;

import java.util.*;

/**
 * Renders a {@link FiniteCategory} to SVG.
 *
 * <p>Objects are laid out using a layered layout when the non-identity
 * morphisms form a DAG, and a circular layout otherwise.  By default identity
 * morphisms are rendered as labelled self-loops; this can be suppressed via
 * {@link #renderSvg(FiniteCategory, RenderOptions, boolean)}.
 *
 * @param <OL> object-label type
 * @param <ML> morphism-label type
 */
public final class CategoryRenderer<OL, ML> implements Renderer<FiniteCategory<OL, ML>> {

    @Override
    public String renderSvg(FiniteCategory<OL, ML> category, RenderOptions opts) {
        return renderSvg(category, opts, true);
    }

    /**
     * @param showIdentities if {@code false}, identity self-loops are omitted
     */
    public String renderSvg(FiniteCategory<OL, ML> category,
                             RenderOptions opts,
                             boolean showIdentities) {

        String markerId = "arrow";
        SvgCanvas canvas = new SvgCanvas(opts.width, opts.height, "#FAFAFA");
        canvas.addArrowMarker(markerId, opts.edgeColor, opts.arrowSize);

        Map<OL, Point> pos = computeLayout(category, opts);

        // Morphisms (drawn before objects)
        for (Morphism<ML, OL> m : category.getMorphisms()) {
            OL srcLabel = m.getDomain().getLabel();
            OL tgtLabel = m.getCodomain().getLabel();

            if (m.isIdentity()) {
                if (!showIdentities) continue;
                Point p = pos.get(srcLabel);
                canvas.selfLoop(p.x(), p.y(), opts.nodeRadius,
                                opts.edgeColor, opts.edgeStrokeWidth, markerId);
                canvas.text(p.x(), p.y() - opts.nodeRadius * 2.8,
                            m.getLabel().toString(), opts.fontSize - 3, "middle", "#777");
                continue;
            }

            Point srcPt = pos.get(srcLabel);
            Point tgtPt = pos.get(tgtLabel);

            // Detect parallel morphisms in the opposite direction → curve them
            boolean hasReverse = category.getMorphisms().stream()
                    .anyMatch(n -> !n.isIdentity()
                            && n.getDomain().getLabel().equals(tgtLabel)
                            && n.getCodomain().getLabel().equals(srcLabel));
            double curvature = hasReverse ? 28 : 0;

            Point from = srcPt.edgeToward(tgtPt, opts.nodeRadius + 2);
            Point to   = tgtPt.edgeToward(srcPt, opts.nodeRadius + opts.arrowSize + 2);

            if (curvature == 0) {
                canvas.line(from.x(), from.y(), to.x(), to.y(),
                            opts.edgeColor, opts.edgeStrokeWidth, markerId);
            } else {
                canvas.curvedLine(from.x(), from.y(), to.x(), to.y(),
                                  curvature, -curvature,
                                  opts.edgeColor, opts.edgeStrokeWidth, markerId);
            }

            Point perp = srcPt.perpendicular(tgtPt, 16 + curvature / 2);
            canvas.edgeLabel(from.x(), from.y(), to.x(), to.y(),
                             perp.x(), perp.y(),
                             m.getLabel().toString(), opts.fontSize - 2, "#333");
        }

        // Objects
        for (var obj : category.getObjects()) {
            OL label = obj.getLabel();
            Point p  = pos.get(label);
            canvas.circle(p.x(), p.y(), opts.nodeRadius,
                          opts.nodeColor, opts.nodeStroke, opts.nodeStrokeWidth);
            canvas.text(p.x(), p.y(), label.toString(),
                        opts.fontSize, "middle", opts.labelColor);
        }

        return canvas.build();
    }

    // -------------------------------------------------------------------------

    private Map<OL, Point> computeLayout(FiniteCategory<OL, ML> category, RenderOptions opts) {
        Set<OL> labels = new LinkedHashSet<>();
        for (var obj : category.getObjects()) labels.add(obj.getLabel());

        // Build adjacency from non-identity morphisms
        Map<OL, List<OL>> adj = new LinkedHashMap<>();
        for (OL l : labels) adj.put(l, new ArrayList<>());
        for (Morphism<ML, OL> m : category.getMorphisms()) {
            if (!m.isIdentity())
                adj.get(m.getDomain().getLabel()).add(m.getCodomain().getLabel());
        }

        try {
            Map<OL, Integer> rankMap = LayeredLayout.computeRanks(labels, adj);
            int maxRank = rankMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            Map<Integer, List<OL>> layers = new LinkedHashMap<>();
            for (Map.Entry<OL, Integer> e : rankMap.entrySet())
                layers.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            return LayeredLayout.layoutRanks(rankMap, layers, maxRank, opts);
        } catch (IllegalArgumentException cyclic) {
            return CircularLayout.layout(labels, opts);
        }
    }
}
