package com.catex.render;

/**
 * Renders a structure {@code T} to an SVG string.
 *
 * @param <T> the type of structure to render
 */
public interface Renderer<T> {

    /**
     * Renders {@code structure} with the given options and returns a
     * self-contained SVG document as a string.
     */
    String renderSvg(T structure, RenderOptions options);

    /** Renders with {@link RenderOptions#defaults()}. */
    default String renderSvg(T structure) {
        return renderSvg(structure, RenderOptions.defaults());
    }
}
