package com.catex.render.internal;

/** A 2-D point in SVG coordinate space. */
public record Point(double x, double y) {

    /** Linear interpolation between this point and {@code other} at parameter {@code t}. */
    public Point lerp(final Point other, final double t) {
        return new Point(x + t * (other.x - x), y + t * (other.y - y));
    }

    /** Euclidean distance to {@code other}. */
    public double distanceTo(final Point other) {
        double dx = other.x - x;
        double dy = other.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Returns the point on the boundary of a circle centred at {@code this}
     * with radius {@code r} that lies in the direction of {@code target}.
     */
    public Point edgeToward(final Point target, final double r) {
        double d = distanceTo(target);
        if (d < 1e-9) return this;
        return new Point(x + r * (target.x - x) / d,
                         y + r * (target.y - y) / d);
    }

    /**
     * Unit-vector perpendicular to the direction (this → target), scaled by
     * {@code scale}.  Used for positioning edge labels.
     */
    public Point perpendicular(final Point target, final double scale) {
        double d  = distanceTo(target);
        if (d < 1e-9) return new Point(scale, 0);
        double dx = (target.x - x) / d;
        double dy = (target.y - y) / d;
        return new Point(-dy * scale, dx * scale);
    }
}
