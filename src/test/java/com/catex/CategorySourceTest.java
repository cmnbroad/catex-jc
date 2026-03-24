package com.catex;

import com.catex.convert.CategoryConverter;
import com.catex.core.CategorySource;
import com.catex.core.FiniteCategory;
import com.catex.lattice.Lattice;
import com.catex.lattice.LatticeConverter;
import com.catex.poset.PartialOrder;
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
        FiniteCategory<String,String> cat = source.toCategory();
        List<String> errors = cat.validate();
        assertTrue(errors.isEmpty(),
                "Category from '" + name + "' should be valid: " + errors);
    }

    // -----------------------------------------------------------------------
    // CategoryConverter façade — from CategorySource
    // -----------------------------------------------------------------------

    @Test(dataProvider = "categorySources")
    public void testConverterFromSource(final String name, final CategorySource<String,String> source) {
        var converter = CategoryConverter.from(source);
        assertNotNull(converter.getCategory());
        assertFalse(converter.getCategory().getObjects().isEmpty(),
                "Category from '" + name + "' should have objects");
    }

    // -----------------------------------------------------------------------
    // Full pipeline: CategorySource → graph / poset / lattice
    // -----------------------------------------------------------------------

    @Test
    public void testFullPipelineToGraph() {
        var g = CategoryConverter.from(new Fixtures.ChainDomain()).toGraph();
        assertEquals(g.getVertices().size(), 3);
        assertEquals(g.getEdges().size(), 3);  // ab, bc, ac
        assertTrue(g.hasEdge("A", "C"), "Transitive edge A→C should be present");
    }

    @Test
    public void testFullPipelineToPoset() {
        PartialOrder<String> pos = CategoryConverter.from(new Fixtures.ChainDomain()).toPoset();
        List<String> errors = pos.validate();
        assertTrue(errors.isEmpty(), "Poset from ChainDomain should be valid: " + errors);
        assertTrue(pos.leq("A", "C"));
        assertFalse(pos.leq("C", "A"));
    }

    @Test
    public void testFullPipelineToLattice() {
        Lattice<String> lat = CategoryConverter.from(new Fixtures.ChainDomain()).toLattice();
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
            FiniteCategory.Builder<Integer, String> builder = FiniteCategory.builder();

            for (final int e : ELEMS)
                builder.addObject(new com.catex.core.CategoryObject<>(e));

            for (final int a : ELEMS) {
                for (final int b : ELEMS) {
                    if (b % a == 0) { // a divides b
                        String label = a == b ? "id_" + a : a + "|" + b;
                        builder.addMorphism(new com.catex.core.Morphism<>(
                                label,
                                new com.catex.core.CategoryObject<>(a),
                                new com.catex.core.CategoryObject<>(b)));
                    }
                }
            }

            // Add composition entries for all transitive triples
            for (final int a : ELEMS)
                for (final int b : ELEMS)
                    for (final int c : ELEMS)
                        if (b % a == 0 && c % b == 0) {
                            String lAB = a == b ? "id_" + a : a + "|" + b;
                            String lBC = b == c ? "id_" + b : b + "|" + c;
                            String lAC = a == c ? "id_" + a : a + "|" + c;
                            builder.addComposition(
                                    new com.catex.core.Morphism<>(lBC,
                                            new com.catex.core.CategoryObject<>(b),
                                            new com.catex.core.CategoryObject<>(c)),
                                    new com.catex.core.Morphism<>(lAB,
                                            new com.catex.core.CategoryObject<>(a),
                                            new com.catex.core.CategoryObject<>(b)),
                                    new com.catex.core.Morphism<>(lAC,
                                            new com.catex.core.CategoryObject<>(a),
                                            new com.catex.core.CategoryObject<>(c)));
                        }

            return builder.build();
        }
    }

    @DataProvider(name = "divisibilityJoins")
    public Object[][] divisibilityJoins() {
        Lattice<Integer> lat = LatticeConverter.fromCategory(new DivisibilityLattice().toCategory());
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
        Lattice<Integer> lat = LatticeConverter.fromCategory(new DivisibilityLattice().toCategory());
        List<String> errors = lat.validate();
        assertTrue(errors.isEmpty(), "Divisibility lattice should be valid: " + errors);
    }
}
