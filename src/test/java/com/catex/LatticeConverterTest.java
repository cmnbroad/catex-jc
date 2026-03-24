package com.catex;

import com.catex.lattice.Lattice;
import com.catex.lattice.LatticeConverter;
import com.catex.poset.PartialOrder;
import com.catex.poset.PosetConverter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class LatticeConverterTest {

    // -----------------------------------------------------------------------
    // DataProvider: lattice element counts
    // -----------------------------------------------------------------------

    @DataProvider(name = "latticeElementCounts")
    public Object[][] latticeElementCounts() {
        return new Object[][] {
            { "chain A≤B≤C", Fixtures.chainCategory(),  3 },
            { "diamond",     Fixtures.diamondCategory(), 4 },
        };
    }

    @Test(dataProvider = "latticeElementCounts")
    public void testElementCount(final String name, final com.catex.core.FiniteCategory<String,String> cat, final int expected) {
        Lattice<String> lat = LatticeConverter.fromCategory(cat);
        assertEquals(lat.getElements().size(), expected,
                "Element count mismatch for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // DataProvider: lattice validation
    // -----------------------------------------------------------------------

    @DataProvider(name = "validLattices")
    public Object[][] validLattices() {
        return new Object[][] {
            { "chain",   LatticeConverter.fromCategory(Fixtures.chainCategory())   },
            { "diamond", LatticeConverter.fromCategory(Fixtures.diamondCategory()) },
        };
    }

    @Test(dataProvider = "validLattices")
    public void testLatticeValidation(final String name, final Lattice<String> lat) {
        List<String> errors = lat.validate();
        assertTrue(errors.isEmpty(),
                "Lattice '" + name + "' should be valid but got: " + errors);
    }

    // -----------------------------------------------------------------------
    // DataProvider: join operations in diamond
    // -----------------------------------------------------------------------

    @DataProvider(name = "diamondJoins")
    public Object[][] diamondJoins() {
        Lattice<String> lat = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        return new Object[][] {
            { lat, "bot", "bot", "bot"  },
            { lat, "bot", "a",   "a"    },
            { lat, "bot", "b",   "b"    },
            { lat, "a",   "b",   "top"  },
            { lat, "a",   "top", "top"  },
            { lat, "b",   "top", "top"  },
            { lat, "top", "top", "top"  },
        };
    }

    @Test(dataProvider = "diamondJoins")
    public void testJoin(final Lattice<String> lat, final String a, final String b, final String expected) {
        assertEquals(lat.join(a, b), expected,
                "join(" + a + "," + b + ") expected " + expected);
    }

    // -----------------------------------------------------------------------
    // DataProvider: meet operations in diamond
    // -----------------------------------------------------------------------

    @DataProvider(name = "diamondMeets")
    public Object[][] diamondMeets() {
        Lattice<String> lat = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        return new Object[][] {
            { lat, "top", "top", "top"  },
            { lat, "top", "a",   "a"    },
            { lat, "top", "b",   "b"    },
            { lat, "a",   "b",   "bot"  },
            { lat, "a",   "bot", "bot"  },
            { lat, "b",   "bot", "bot"  },
            { lat, "bot", "bot", "bot"  },
        };
    }

    @Test(dataProvider = "diamondMeets")
    public void testMeet(final Lattice<String> lat, final String a, final String b, final String expected) {
        assertEquals(lat.meet(a, b), expected,
                "meet(" + a + "," + b + ") expected " + expected);
    }

    // -----------------------------------------------------------------------
    // DataProvider: join in chain (chain is also a lattice)
    // -----------------------------------------------------------------------

    @DataProvider(name = "chainJoins")
    public Object[][] chainJoins() {
        Lattice<String> lat = LatticeConverter.fromCategory(Fixtures.chainCategory());
        return new Object[][] {
            { lat, "A", "A", "A" },
            { lat, "A", "B", "B" },
            { lat, "A", "C", "C" },
            { lat, "B", "C", "C" },
        };
    }

    @Test(dataProvider = "chainJoins")
    public void testChainJoin(final Lattice<String> lat, final String a, final String b, final String expected) {
        assertEquals(lat.join(a, b), expected);
    }

    // -----------------------------------------------------------------------
    // Top and bottom elements
    // -----------------------------------------------------------------------

    @Test
    public void testDiamondTop() {
        Lattice<String> lat = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        assertTrue(lat.top().isPresent());
        assertEquals(lat.top().get(), "top");
    }

    @Test
    public void testDiamondBottom() {
        Lattice<String> lat = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        assertTrue(lat.bottom().isPresent());
        assertEquals(lat.bottom().get(), "bot");
    }

    @Test
    public void testChainTop() {
        Lattice<String> lat = LatticeConverter.fromCategory(Fixtures.chainCategory());
        assertTrue(lat.top().isPresent());
        assertEquals(lat.top().get(), "C");
    }

    @Test
    public void testChainBottom() {
        Lattice<String> lat = LatticeConverter.fromCategory(Fixtures.chainCategory());
        assertTrue(lat.bottom().isPresent());
        assertEquals(lat.bottom().get(), "A");
    }

    // -----------------------------------------------------------------------
    // Non-lattice poset throws
    // -----------------------------------------------------------------------

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNonLatticeThrows() {
        // "N" shape: bot, a, b, top but WITHOUT b≤top (so a and b have no join)
        // Build a poset that is not a lattice: {0,1,2} with 0≤1 and 0≤2 but 1,2 incomparable
        // and no common upper bound other than themselves — actually we need a partial order
        // where some pair has no join.
        // Use: elements {a, b} with no ordering between them (antichain of size 2),
        // no common upper bound.
        PartialOrder<String> antichain = PosetConverter.toPoset(
                Fixtures.twoObjectCategory()); // A→B is not symmetric, A≤B but B≰A — this IS a lattice
        // Instead build a proper antichain manually via fromPoset
        // Two elements with only reflexivity — no join for (a,b) because no upper bound
        var a = new com.catex.core.CategoryObject<>("a");
        var b = new com.catex.core.CategoryObject<>("b");
        var idA = new com.catex.core.Morphism<>("id_a", a, a);
        var idB = new com.catex.core.Morphism<>("id_b", b, b);
        var cat = com.catex.core.FiniteCategory.<String,String>builder()
                .addObject(a).addObject(b)
                .addMorphism(idA).addMorphism(idB)
                .addComposition(idA, idA, idA)
                .addComposition(idB, idB, idB)
                .build();

        LatticeConverter.fromCategory(cat); // should throw: no join for (a,b)
    }

    // -----------------------------------------------------------------------
    // Round-trip: lattice → category → lattice
    // -----------------------------------------------------------------------

    @Test
    public void testRoundTripLatticeToCategory() {
        Lattice<String> original = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        var cat2 = LatticeConverter.toCategory(original);
        Lattice<String> restored = LatticeConverter.fromCategory(cat2);

        assertEquals(restored.getElements(), original.getElements());
        for (final String a : original.getElements()) {
            for (final String b : original.getElements()) {
                assertEquals(restored.join(a, b), original.join(a, b),
                        "Join mismatch after round-trip for (" + a + "," + b + ")");
                assertEquals(restored.meet(a, b), original.meet(a, b),
                        "Meet mismatch after round-trip for (" + a + "," + b + ")");
            }
        }
    }
}
