// Copyright 2026 Christopher Norman
package com.github.catexjc.convert;

import com.github.catexjc.core.CategorySource;
import com.github.catexjc.core.FiniteCategory;
import com.github.catexjc.graph.DirectedGraph;
import com.github.catexjc.graph.GraphConverter;
import com.github.catexjc.hasse.HasseDiagram;
import com.github.catexjc.hasse.HasseDiagramConverter;
import com.github.catexjc.lattice.Lattice;
import com.github.catexjc.lattice.LatticeConverter;
import com.github.catexjc.poset.PartialOrder;
import com.github.catexjc.poset.PosetConverter;

/**
 * Fluent façade for converting between all representations supported by the
 * library.
 *
 * <p>Usage example:
 * <pre>{@code
 * FiniteCategory<String, String> cat = myDomain.toCategory();
 *
 * DirectedGraph<String, String> g    = CategoryConverter.from(cat).toGraph();
 * PartialOrder<String>          pos  = CategoryConverter.from(cat).toPoset();
 * Lattice<String>               lat  = CategoryConverter.from(cat).toLattice();
 * HasseDiagram<String>          hd   = CategoryConverter.from(cat).toHasseDiagram();
 * }</pre>
 *
 * @param <OL> object-label type
 * @param <ML> morphism-label type
 */
public final class CategoryConverter<OL, ML> {

    private final FiniteCategory<OL, ML> category;

    private CategoryConverter(final FiniteCategory<OL, ML> category) {
        this.category = category;
    }

    // -------------------------------------------------------------------------
    // Entry points
    // -------------------------------------------------------------------------

    /** Start a conversion pipeline from an already-built category. */
    public static <OL, ML> CategoryConverter<OL, ML> from(final FiniteCategory<OL, ML> category) {
        return new CategoryConverter<>(category);
    }

    /** Start a conversion pipeline from any {@link CategorySource}. */
    public static <OL, ML> CategoryConverter<OL, ML> from(final CategorySource<OL, ML> source) {
        return new CategoryConverter<>(source.toCategory());
    }

    // -------------------------------------------------------------------------
    // Conversions out
    // -------------------------------------------------------------------------

    /** Convert to a directed graph (identity morphisms excluded). */
    public DirectedGraph<OL, ML> toGraph() {
        return GraphConverter.toGraph(category);
    }

    /** Convert to a directed graph with optional identity self-loops. */
    public DirectedGraph<OL, ML> toGraph(final boolean includeIdentities) {
        return GraphConverter.toGraph(category, includeIdentities);
    }

    /**
     * Convert to a partially ordered set.
     *
     * @throws IllegalArgumentException if the category is not a preorder
     */
    public PartialOrder<OL> toPoset() {
        return PosetConverter.toPoset(category);
    }

    /**
     * Convert to a lattice.
     *
     * @throws IllegalArgumentException if the category is not a preorder or not a lattice
     */
    public Lattice<OL> toLattice() {
        return LatticeConverter.fromCategory(category);
    }

    /**
     * Convert to a Hasse diagram.
     *
     * @throws IllegalArgumentException if the category is not a preorder
     */
    public HasseDiagram<OL> toHasseDiagram() {
        return HasseDiagramConverter.fromCategory(category);
    }

    /** Returns the underlying category for further use. */
    public FiniteCategory<OL, ML> getCategory() {
        return category;
    }
}
