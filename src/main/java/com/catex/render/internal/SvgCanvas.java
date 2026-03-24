package com.catex.render.internal;

/**
 * Low-level SVG document builder.  All drawing coordinates are in SVG user
 * units (pixels at the default 1:1 scale).
 *
 * <p>Usage pattern:
 * <pre>{@code
 * SvgCanvas canvas = new SvgCanvas(800, 600, "#FAFAFA");
 * canvas.addArrowMarker("arrow", "#555", 8);
 * canvas.circle(100, 100, 20, "#4A90D9", "#2C5F8A", 2);
 * canvas.text(100, 100, "A", 13, "middle", "#FFF");
 * System.out.println(canvas.build());
 * }</pre>
 */
public final class SvgCanvas {

    private final int    width;
    private final int    height;
    private final StringBuilder defs = new StringBuilder();
    private final StringBuilder body = new StringBuilder();

    public SvgCanvas(final int width, final int height, final String background) {
        this.width  = width;
        this.height = height;
        if (background != null) {
            body.append(fmt("<rect width=\"%d\" height=\"%d\" fill=\"%s\"/>%n",
                    width, height, background));
        }
    }

    // -------------------------------------------------------------------------
    // Defs
    // -------------------------------------------------------------------------

    /**
     * Adds a triangular arrowhead marker that can be referenced by {@code id}.
     *
     * @param id    SVG marker id (referenced as {@code url(#id)})
     * @param color fill colour of the arrowhead polygon
     * @param size  half-length of the arrowhead in px (markerWidth = size * 1.5)
     */
    public void addArrowMarker(final String id, final String color, final double size) {
        final double w = size * 1.5;
        final double h = size;
        // refX = w so the tip aligns with the line endpoint
        defs.append(fmt(
            "<marker id=\"%s\" markerUnits=\"userSpaceOnUse\" markerWidth=\"%.1f\" "
            + "markerHeight=\"%.1f\" refX=\"%.1f\" refY=\"%.1f\" orient=\"auto\">%n"
            + "  <polygon points=\"0 0, %.1f %.1f, 0 %.1f\" fill=\"%s\"/>%n"
            + "</marker>%n",
            id, w, h, w, h / 2, w, h / 2, h, color));
    }

    // -------------------------------------------------------------------------
    // Shapes
    // -------------------------------------------------------------------------

    public void circle(final double cx, final double cy, final double r,
                       final String fill, final String stroke, final double strokeWidth) {
        body.append(fmt(
            "<circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" fill=\"%s\" "
            + "stroke=\"%s\" stroke-width=\"%.2f\"/>%n",
            cx, cy, r, fill, stroke, strokeWidth));
    }

    /** Straight line, optionally with a marker at the end. */
    public void line(final double x1, final double y1, final double x2, final double y2,
                     final String stroke, final double strokeWidth, final String markerId) {
        final String marker = markerId == null ? "" : fmt(" marker-end=\"url(#%s)\"", markerId);
        body.append(fmt(
            "<line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" "
            + "stroke=\"%s\" stroke-width=\"%.2f\"%s/>%n",
            x1, y1, x2, y2, stroke, strokeWidth, marker));
    }

    /**
     * Curved edge via a quadratic Bézier, optionally with a marker.
     * The control point is offset from the midpoint by ({@code cpOffX}, {@code cpOffY}).
     */
    public void curvedLine(final double x1, final double y1, final double x2, final double y2,
                           final double cpOffX, final double cpOffY,
                           final String stroke, final double strokeWidth, final String markerId) {
        final double mx  = (x1 + x2) / 2 + cpOffX;
        final double my  = (y1 + y2) / 2 + cpOffY;
        final String marker = markerId == null ? "" : fmt(" marker-end=\"url(#%s)\"", markerId);
        body.append(fmt(
            "<path d=\"M %.2f %.2f Q %.2f %.2f %.2f %.2f\" fill=\"none\" "
            + "stroke=\"%s\" stroke-width=\"%.2f\"%s/>%n",
            x1, y1, mx, my, x2, y2, stroke, strokeWidth, marker));
    }

    /**
     * Self-loop drawn as a circle arc above the given node centre.
     */
    public void selfLoop(final double cx, final double cy, final double nodeR,
                         final String stroke, final double strokeWidth, final String markerId) {
        // Small circle centred directly above the node
        final double loopR = nodeR * 0.8;
        final double lcx   = cx;
        final double lcy   = cy - nodeR - loopR + 2; // slightly overlap node edge

        // Arc: almost-full circle (350° to leave a tiny gap for the arrowhead)
        // We draw it as two lines entering/exiting the loop circle
        final double gap = Math.toRadians(10);
        final double startAngle = Math.PI / 2 + gap;   // just past bottom of loop
        final double endAngle   = Math.PI / 2 - gap;   // just before bottom of loop

        final double sx = lcx + loopR * Math.cos(startAngle);
        final double sy = lcy + loopR * Math.sin(startAngle);
        final double ex = lcx + loopR * Math.cos(endAngle);
        final double ey = lcy + loopR * Math.sin(endAngle);

        final String marker = markerId == null ? "" : fmt(" marker-end=\"url(#%s)\"", markerId);
        // large-arc-flag=1 to draw the long way round
        body.append(fmt(
            "<path d=\"M %.2f %.2f A %.2f %.2f 0 1 1 %.2f %.2f\" fill=\"none\" "
            + "stroke=\"%s\" stroke-width=\"%.2f\"%s/>%n",
            sx, sy, loopR, loopR, ex, ey, stroke, strokeWidth, marker));
    }

    // -------------------------------------------------------------------------
    // Text
    // -------------------------------------------------------------------------

    public void text(final double x, final double y, final String content,
                     final int fontSize, final String anchor, final String fill) {
        body.append(fmt(
            "<text x=\"%.2f\" y=\"%.2f\" font-size=\"%d\" font-family=\"sans-serif\" "
            + "text-anchor=\"%s\" dominant-baseline=\"middle\" fill=\"%s\">%s</text>%n",
            x, y, fontSize, anchor, fill, escapeXml(content)));
    }

    /**
     * Label drawn beside an edge, offset perpendicularly from the midpoint.
     *
     * @param perpOffX perpendicular offset in X
     * @param perpOffY perpendicular offset in Y
     */
    public void edgeLabel(final double x1, final double y1, final double x2, final double y2,
                          final double perpOffX, final double perpOffY,
                          final String content, final int fontSize, final String fill) {
        final double mx = (x1 + x2) / 2 + perpOffX;
        final double my = (y1 + y2) / 2 + perpOffY;
        // Translucent background pill for readability
        final double fw = content.length() * fontSize * 0.6 + 6;
        final double fh = fontSize + 4;
        body.append(fmt(
            "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" "
            + "rx=\"3\" ry=\"3\" fill=\"white\" fill-opacity=\"0.8\"/>%n",
            mx - fw / 2, my - fh / 2, fw, fh));
        text(mx, my, content, fontSize, "middle", fill);
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    public String build() {
        final StringBuilder sb = new StringBuilder();
        sb.append(fmt(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\" "
            + "width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">%n",
            width, height, width, height));
        if (defs.length() > 0) {
            sb.append("<defs>\n").append(defs).append("</defs>\n");
        }
        sb.append(body);
        sb.append("</svg>");
        return sb.toString();
    }

    // -------------------------------------------------------------------------

    private static String fmt(final String format, final Object... args) {
        return String.format(format, args);
    }

    private static String escapeXml(final String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
