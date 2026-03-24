package com.catex.render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders a structure {@code T} to an SVG string or persistent file.
 *
 * @param <T> the type of structure to render
 */
public interface Renderer<T> {

    /**
     * Renders {@code structure} with the given options and returns a
     * self-contained SVG document as a string.
     */
    String renderSvg(final T structure, final RenderOptions options);

    /** Renders with {@link RenderOptions#defaults()}. */
    default String renderSvg(final T structure) {
        return renderSvg(structure, RenderOptions.defaults());
    }

    /**
     * Renders {@code structure} to a file at the given {@code path},
     * creating or overwriting the file as needed.
     *
     * @param structure the structure to render
     * @param options   render options
     * @param path      destination file path
     * @throws IOException if the file cannot be written
     */
    default void renderSvgToFile(final T structure, final RenderOptions options, final Path path) throws IOException {
        Files.writeString(path, renderSvg(structure, options));
    }

    /**
     * Renders {@code structure} to a file using {@link RenderOptions#defaults()}.
     *
     * @param structure the structure to render
     * @param path      destination file path
     * @throws IOException if the file cannot be written
     */
    default void renderSvgToFile(final T structure, final Path path) throws IOException {
        renderSvgToFile(structure, RenderOptions.defaults(), path);
    }
}
