package com.github.catexjc.render;

/**
 * Immutable configuration bag used by all SVG renderers.
 *
 * <p>Create a custom set of options via the {@link Builder}:
 * <pre>{@code
 * RenderOptions opts = RenderOptions.builder()
 *         .width(1000).height(700)
 *         .nodeColor("#4A90D9").labelColor("#FFFFFF")
 *         .build();
 * }</pre>
 */
public final class RenderOptions {

    // Canvas
    public final int    width;
    public final int    height;
    public final int    margin;

    // Nodes
    public final double nodeRadius;
    public final String nodeColor;
    public final String nodeStroke;
    public final double nodeStrokeWidth;

    // Edges
    public final String edgeColor;
    public final double edgeStrokeWidth;
    public final double arrowSize;

    // Text
    public final String labelColor;
    public final int    fontSize;

    // Layout
    public final double layerHeight;   // vertical gap between Hasse/poset layers
    public final double layoutRadius;  // radius used by circular layout

    // Highlight
    public final String topColor;      // lattice top element colour
    public final String bottomColor;   // lattice bottom element colour

    private RenderOptions(final Builder b) {
        this.width           = b.width;
        this.height          = b.height;
        this.margin          = b.margin;
        this.nodeRadius      = b.nodeRadius;
        this.nodeColor       = b.nodeColor;
        this.nodeStroke      = b.nodeStroke;
        this.nodeStrokeWidth = b.nodeStrokeWidth;
        this.edgeColor       = b.edgeColor;
        this.edgeStrokeWidth = b.edgeStrokeWidth;
        this.arrowSize       = b.arrowSize;
        this.labelColor      = b.labelColor;
        this.fontSize        = b.fontSize;
        this.layerHeight     = b.layerHeight;
        this.layoutRadius    = b.layoutRadius;
        this.topColor        = b.topColor;
        this.bottomColor     = b.bottomColor;
    }

    /** Default options suitable for most diagrams. */
    public static RenderOptions defaults() {
        return builder().build();
    }

    public static Builder builder() { return new Builder(); }

    // -------------------------------------------------------------------------

    public static final class Builder {
        private int    width           = 800;
        private int    height          = 600;
        private int    margin          = 60;
        private double nodeRadius      = 22;
        private String nodeColor       = "#4A90D9";
        private String nodeStroke      = "#2C5F8A";
        private double nodeStrokeWidth = 2.0;
        private String edgeColor       = "#555555";
        private double edgeStrokeWidth = 1.5;
        private double arrowSize       = 8;
        private String labelColor      = "#FFFFFF";
        private int    fontSize        = 13;
        private double layerHeight     = 110;
        private double layoutRadius    = 200;
        private String topColor        = "#E8704A";
        private String bottomColor     = "#5BAD72";

        public Builder width(final int v)           { width = v;           return this; }
        public Builder height(final int v)          { height = v;          return this; }
        public Builder margin(final int v)          { margin = v;          return this; }
        public Builder nodeRadius(final double v)   { nodeRadius = v;      return this; }
        public Builder nodeColor(final String v)    { nodeColor = v;       return this; }
        public Builder nodeStroke(final String v)   { nodeStroke = v;      return this; }
        public Builder nodeStrokeWidth(final double v){ nodeStrokeWidth=v; return this; }
        public Builder edgeColor(final String v)    { edgeColor = v;       return this; }
        public Builder edgeStrokeWidth(final double v){ edgeStrokeWidth=v; return this; }
        public Builder arrowSize(final double v)    { arrowSize = v;       return this; }
        public Builder labelColor(final String v)   { labelColor = v;      return this; }
        public Builder fontSize(final int v)        { fontSize = v;        return this; }
        public Builder layerHeight(final double v)  { layerHeight = v;     return this; }
        public Builder layoutRadius(final double v) { layoutRadius = v;    return this; }
        public Builder topColor(final String v)     { topColor = v;        return this; }
        public Builder bottomColor(final String v)  { bottomColor = v;     return this; }

        public RenderOptions build() { return new RenderOptions(this); }
    }
}
