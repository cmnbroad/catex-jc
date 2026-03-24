package com.catex.convert;

import com.catex.core.CategorySource;
import com.catex.core.FiniteCategory;
import com.catex.graph.DirectedGraph;
import com.catex.graph.GraphConverter;
import com.catex.hasse.HasseDiagram;
import com.catex.hasse.HasseDiagramConverter;
import com.catex.lattice.Lattice;
import com.catex.lattice.LatticeConverter;
import com.catex.poset.PartialOrder;
import com.catex.poset.PosetConverter;

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
