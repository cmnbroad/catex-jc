// Copyright 2026 Christopher Norman
package com.github.catexjc;

import com.github.catexjc.convert.CategoryConverter;
import com.github.catexjc.core.CategorySource;
import com.github.catexjc.core.FiniteCategory;
import com.github.catexjc.lattice.Lattice;
import com.github.catexjc.lattice.LatticeConverter;
import com.github.catexjc.poset.PartialOrder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * Tests for the {@link CategorySource} interface and the {@link CategoryConverter} façade.
 */
public class CategorySourceTest {

    // -----------------------------------------------------------------------
    // DataProvider: domain objects implementing CategorySource
    // -----------------------------------------------------------------------

    @DataProvider(name = "categorySources")
    public Object[][] categorySources() {
        return new Object[][] {
            { "ChainDomain", new Fixtures.ChainDomain() },
        };
    }

    @Test(dataProvider = "categorySources")
    public void testCategorySourceProducesValidCategory(final String name, final CategorySource<String,String> source) {
        final FiniteCategory<String,String> cat = source.toCategory();
        final List<String> errors = cat.validate();
        assertTrue(errors.isEmpty(),
                "Category from '" + name + "' should be valid: " + errors);
    }

    // -----------------------------------------------------------------------
    // CategoryConverter façade — from CategorySource
    // -----------------------------------------------------------------------

    @Test(dataProvider = "categorySources")
    public void testConverterFromSource(final String name, final CategorySource<String,String> source) {
        final var converter = CategoryConverter.from(source);
        assertNotNull(converter.getCategory());
        assertFalse(converter.getCategory().getObjects().isEmpty(),
                "Category from '" + name + "' should have objects");
    }

    // -----------------------------------------------------------------------
    // Full pipeline: CategorySource → graph / poset / lattice
    // -----------------------------------------------------------------------

    @Test
    public void testFullPipelineToGraph() {
        final var g = CategoryConverter.from(new Fixtures.ChainDomain()).toGraph();
        assertEquals(g.getVertices().size(), 3);
        assertEquals(g.getEdges().size(), 3);  // ab, bc, ac
        assertTrue(g.hasEdge("A", "C"), "Transitive edge A→C should be present");
    }

    @Test
    public void testFullPipelineToPoset() {
        final PartialOrder<String> pos = CategoryConverter.from(new Fixtures.ChainDomain()).toPoset();
        final List<String> errors = pos.validate();
        assertTrue(errors.isEmpty(), "Poset from ChainDomain should be valid: " + errors);
        assertTrue(pos.leq("A", "C"));
        assertFalse(pos.leq("C", "A"));
    }

    @Test
    public void testFullPipelineToLattice() {
        final Lattice<String> lat = CategoryConverter.from(new Fixtures.ChainDomain()).toLattice();
        assertEquals(lat.join("A", "B"), "B");
        assertEquals(lat.meet("B", "C"), "B");
    }

    // -----------------------------------------------------------------------
    // Custom domain: divisibility lattice of {1,2,3,6} where a|b means a≤b
    // -----------------------------------------------------------------------

    /** Divisibility poset: 1 divides everything, 6 is divided by all. */
    static class DivisibilityLattice implements CategorySource<Integer, String> {

        private static final int[] ELEMS = {1, 2, 3, 6};

        @Override
        public FiniteCategory<Integer, String> toCategory() {
            final FiniteCategory.Builder<Integer, String> builder = FiniteCategory.builder();

            for (final int e : ELEMS) {
                builder.addObject(new com.github.catexjc.core.CategoryObject<>(e));
            }

            for (final int a : ELEMS) {
                for (final int b : ELEMS) {
                    if (b % a == 0) { // a divides b
                        final String label = a == b ? "id_" + a : a + "|" + b;
                        builder.addMorphism(new com.github.catexjc.core.Morphism<>(
                                label,
                                new com.github.catexjc.core.CategoryObject<>(a),
                                new com.github.catexjc.core.CategoryObject<>(b)));
                    }
                }
            }

            // Add composition entries for all transitive triples
            for (final int a : ELEMS)
                for (final int b : ELEMS)
                    for (final int c : ELEMS)
                        if (b % a == 0 && c % b == 0) {
                            final String lAB = a == b ? "id_" + a : a + "|" + b;
                            final String lBC = b == c ? "id_" + b : b + "|" + c;
                            final String lAC = a == c ? "id_" + a : a + "|" + c;
                            builder.addComposition(
                                    new com.github.catexjc.core.Morphism<>(lBC,
                                            new com.github.catexjc.core.CategoryObject<>(b),
                                            new com.github.catexjc.core.CategoryObject<>(c)),
                                    new com.github.catexjc.core.Morphism<>(lAB,
                                            new com.github.catexjc.core.CategoryObject<>(a),
                                            new com.github.catexjc.core.CategoryObject<>(b)),
                                    new com.github.catexjc.core.Morphism<>(lAC,
                                            new com.github.catexjc.core.CategoryObject<>(a),
                                            new com.github.catexjc.core.CategoryObject<>(c)));
                        }

            return builder.build();
        }
    }

    @DataProvider(name = "divisibilityJoins")
    public Object[][] divisibilityJoins() {
        final Lattice<Integer> lat = LatticeConverter.fromCategory(new DivisibilityLattice().toCategory());
        return new Object[][] {
            { lat, 1, 1, 1 },
            { lat, 1, 2, 2 },
            { lat, 2, 3, 6 },  // lcm(2,3) = 6
            { lat, 1, 6, 6 },
            { lat, 6, 6, 6 },
        };
    }

    @Test(dataProvider = "divisibilityJoins")
    public void testDivisibilityJoin(final Lattice<Integer> lat, final int a, final int b, final int expected) {
        assertEquals((int) lat.join(a, b), expected,
                "lcm(" + a + "," + b + ") should be " + expected);
    }

    @DataProvider(name = "divisibilityMeets")
    public Object[][] divisibilityMeets() {
        Lattice<Integer> lat = LatticeConverter.fromCategory(new DivisibilityLattice().toCategory());
        return new Object[][] {
            { lat, 6, 6, 6 },
            { lat, 6, 2, 2 },
            { lat, 2, 3, 1 },  // gcd(2,3) = 1
            { lat, 6, 1, 1 },
        };
    }

    @Test(dataProvider = "divisibilityMeets")
    public void testDivisibilityMeet(final Lattice<Integer> lat, final int a, final int b, final int expected) {
        assertEquals((int) lat.meet(a, b), expected,
                "gcd(" + a + "," + b + ") should be " + expected);
    }

    @Test
    public void testDivisibilityLatticeValid() {
        final Lattice<Integer> lat = LatticeConverter.fromCategory(new DivisibilityLattice().toCategory());
        final List<String> errors = lat.validate();
        assertTrue(errors.isEmpty(), "Divisibility lattice should be valid: " + errors);
    }
}
