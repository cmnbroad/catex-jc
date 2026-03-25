// Copyright 2026 Christopher Norman
package com.github.catexjc.render;

import com.github.catexjc.core.FiniteCategory;
import com.github.catexjc.core.Morphism;
import com.github.catexjc.render.internal.CircularLayout;
import com.github.catexjc.render.internal.LayeredLayout;
import com.github.catexjc.render.internal.Point;
import com.github.catexjc.render.internal.SvgCanvas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public String renderSvg(final FiniteCategory<OL, ML> category, final RenderOptions opts) {
        return renderSvg(category, opts, true);
    }

    /**
     * @param showIdentities if {@code false}, identity self-loops are omitted
     */
    public String renderSvg(final FiniteCategory<OL, ML> category,
                             final RenderOptions opts,
                             final boolean showIdentities) {

        final String markerId = "arrow";
        final SvgCanvas canvas = new SvgCanvas(opts.width, opts.height, "#FAFAFA");
        canvas.addArrowMarker(markerId, opts.edgeColor, opts.arrowSize);

        final Map<OL, Point> pos = computeLayout(category, opts);

        // Morphisms (drawn before objects)
        for (final Morphism<ML, OL> m : category.getMorphisms()) {
            final OL srcLabel = m.getDomain().getLabel();
            final OL tgtLabel = m.getCodomain().getLabel();

            if (m.isIdentity()) {
                if (!showIdentities) {
                    continue;
                }
                final Point p = pos.get(srcLabel);
                canvas.selfLoop(p.x(), p.y(), opts.nodeRadius,
                                opts.edgeColor, opts.edgeStrokeWidth, markerId);
                canvas.text(p.x(), p.y() - opts.nodeRadius * 2.8,
                            m.getLabel().toString(), opts.fontSize - 3, "middle", "#777");
                continue;
            }

            final Point srcPt = pos.get(srcLabel);
            final Point tgtPt = pos.get(tgtLabel);

            // Detect parallel morphisms in the opposite direction → curve them
            final boolean hasReverse = category.getMorphisms().stream()
                    .anyMatch(n -> !n.isIdentity()
                            && n.getDomain().getLabel().equals(tgtLabel)
                            && n.getCodomain().getLabel().equals(srcLabel));
            final double curvature = hasReverse ? 28 : 0;

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

            final Point perp = srcPt.perpendicular(tgtPt, 16 + curvature / 2);
            canvas.edgeLabel(from.x(), from.y(), to.x(), to.y(),
                             perp.x(), perp.y(),
                             m.getLabel().toString(), opts.fontSize - 2, "#333");
        }

        // Objects
        for (final var obj : category.getObjects()) {
            final OL label = obj.getLabel();
            final Point p  = pos.get(label);
            canvas.circle(p.x(), p.y(), opts.nodeRadius,
                          opts.nodeColor, opts.nodeStroke, opts.nodeStrokeWidth);
            canvas.text(p.x(), p.y(), label.toString(),
                        opts.fontSize, "middle", opts.labelColor);
        }

        return canvas.build();
    }

    /**
     * Renders {@code category} to a file, including or excluding identity morphisms.
     *
     * @param category       the category to render
     * @param opts           render options
     * @param path           destination file path
     * @param showIdentities if {@code false}, identity self-loops are omitted
     * @throws IOException if the file cannot be written
     */
    public void renderSvgToFile(final FiniteCategory<OL, ML> category,
                                 final RenderOptions opts,
                                 final Path path,
                                 final boolean showIdentities) throws IOException {
        Files.writeString(path, renderSvg(category, opts, showIdentities));
    }

    // -------------------------------------------------------------------------

    private Map<OL, Point> computeLayout(final FiniteCategory<OL, ML> category, final RenderOptions opts) {
        final Set<OL> labels = new LinkedHashSet<>();
        for (final var obj : category.getObjects()) {
            labels.add(obj.getLabel());
        }

        // Build adjacency from non-identity morphisms
        final Map<OL, List<OL>> adj = new LinkedHashMap<>();
        for (final OL l : labels) {
            adj.put(l, new ArrayList<>());
        }
        for (final Morphism<ML, OL> m : category.getMorphisms()) {
            if (!m.isIdentity()) {
                adj.get(m.getDomain().getLabel()).add(m.getCodomain().getLabel());
            }
        }

        try {
            final Map<OL, Integer> rankMap = LayeredLayout.computeRanks(labels, adj);
            final int maxRank = rankMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            final Map<Integer, List<OL>> layers = new LinkedHashMap<>();
            for (final Map.Entry<OL, Integer> e : rankMap.entrySet()) {
                layers.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }
            return LayeredLayout.layoutRanks(rankMap, layers, maxRank, opts);
        } catch (IllegalArgumentException cyclic) {
            return CircularLayout.layout(labels, opts);
        }
    }
}
