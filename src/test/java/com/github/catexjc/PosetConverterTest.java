package com.github.catexjc;

import com.github.catexjc.core.FiniteCategory;
import com.github.catexjc.poset.PartialOrder;
import com.github.catexjc.poset.PosetConverter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class PosetConverterTest {

    // -----------------------------------------------------------------------
    // DataProvider: category → poset element counts
    // -----------------------------------------------------------------------

    @DataProvider(name = "posetElementCounts")
    public Object[][] posetElementCounts() {
        return new Object[][] {
            { "two-object",  Fixtures.twoObjectCategory(), 2 },
            { "chain A≤B≤C", Fixtures.chainCategory(),    3 },
            { "diamond",     Fixtures.diamondCategory(),   4 },
        };
    }

    @Test(dataProvider = "posetElementCounts")
    public void testElementCount(final String name, final FiniteCategory<String,String> cat, final int expected) {
        final PartialOrder<String> pos = PosetConverter.toPoset(cat);
        assertEquals(pos.getElements().size(), expected,
                "Element count mismatch for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // DataProvider: ordering relations in chain A≤B≤C
    // -----------------------------------------------------------------------

    @DataProvider(name = "chainOrderRelations")
    public Object[][] chainOrderRelations() {
        final PartialOrder<String> pos = PosetConverter.toPoset(Fixtures.chainCategory());
        return new Object[][] {
            { pos, "A", "A", true  },
            { pos, "A", "B", true  },
            { pos, "A", "C", true  },
            { pos, "B", "C", true  },
            { pos, "B", "A", false },
            { pos, "C", "A", false },
            { pos, "C", "B", false },
        };
    }

    @Test(dataProvider = "chainOrderRelations")
    public void testChainOrderRelation(final PartialOrder<String> pos, final String a, final String b, final boolean expected) {
        assertEquals(pos.leq(a, b), expected,
                a + " ≤ " + b + " expected " + expected);
    }

    // -----------------------------------------------------------------------
    // Poset validation
    // -----------------------------------------------------------------------

    @DataProvider(name = "validPosets")
    public Object[][] validPosets() {
        return new Object[][] {
            { "chain",   PosetConverter.toPoset(Fixtures.chainCategory())   },
            { "diamond", PosetConverter.toPoset(Fixtures.diamondCategory()) },
        };
    }

    @Test(dataProvider = "validPosets")
    public void testPosetValidation(final String name, final PartialOrder<String> pos) {
        final List<String> errors = pos.validate();
        assertTrue(errors.isEmpty(),
                "Poset '" + name + "' should be valid but got: " + errors);
    }

    // -----------------------------------------------------------------------
    // Hasse covers
    // -----------------------------------------------------------------------

    @Test
    public void testHasseCoversChain() {
        final PartialOrder<String> pos = PosetConverter.toPoset(Fixtures.chainCategory());
        final List<PartialOrder.Cover<String>> covers = pos.hasseCovers();
        // Chain: A<B, B<C — only direct covers, A<C is NOT a cover
        assertEquals(covers.size(), 2);
        final boolean hasAB = covers.stream().anyMatch(c -> c.lower().equals("A") && c.upper().equals("B"));
        final boolean hasBC = covers.stream().anyMatch(c -> c.lower().equals("B") && c.upper().equals("C"));
        assertTrue(hasAB, "Cover A<B should be present");
        assertTrue(hasBC, "Cover B<C should be present");
    }

    @Test
    public void testHasseCoversChainExcludesTransitive() {
        final PartialOrder<String> pos = PosetConverter.toPoset(Fixtures.chainCategory());
        final List<PartialOrder.Cover<String>> covers = pos.hasseCovers();
        final boolean hasAC = covers.stream().anyMatch(c -> c.lower().equals("A") && c.upper().equals("C"));
        assertFalse(hasAC, "A<C is not a direct cover in the chain");
    }

    @Test
    public void testHasseCoversDiamond() {
        final PartialOrder<String> pos = PosetConverter.toPoset(Fixtures.diamondCategory());
        final List<PartialOrder.Cover<String>> covers = pos.hasseCovers();
        // bot<a, bot<b, a<top, b<top — bot<top is NOT a cover
        assertEquals(covers.size(), 4);
    }

    // -----------------------------------------------------------------------
    // Upper / lower sets
    // -----------------------------------------------------------------------

    @Test
    public void testUpperSetInChain() {
        final PartialOrder<String> pos = PosetConverter.toPoset(Fixtures.chainCategory());
        assertEquals(pos.upperSet("A").size(), 3); // A itself, B, C
        assertEquals(pos.upperSet("B").size(), 2); // B, C
        assertEquals(pos.upperSet("C").size(), 1); // C only
    }

    @Test
    public void testLowerSetInChain() {
        final PartialOrder<String> pos = PosetConverter.toPoset(Fixtures.chainCategory());
        assertEquals(pos.lowerSet("C").size(), 3);
        assertEquals(pos.lowerSet("B").size(), 2);
        assertEquals(pos.lowerSet("A").size(), 1);
    }

    // -----------------------------------------------------------------------
    // Round-trip: poset → category → poset
    // -----------------------------------------------------------------------

    @Test
    public void testRoundTripPosetToCategory() {
        final PartialOrder<String> pos    = PosetConverter.toPoset(Fixtures.chainCategory());
        final FiniteCategory<String,String> cat2 = PosetConverter.fromPoset(pos);
        final PartialOrder<String> pos2   = PosetConverter.toPoset(cat2);

        assertEquals(pos2.getElements(), pos.getElements());
        for (final String a : pos.getElements())
            for (final String b : pos.getElements())
                assertEquals(pos2.leq(a, b), pos.leq(a, b),
                        "Relation " + a + " ≤ " + b + " differs after round-trip");
    }

    // -----------------------------------------------------------------------
    // Non-preorder category throws
    // -----------------------------------------------------------------------

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNonPreorderCategoryThrows() {
        // Build a category with two morphisms A→B
        final var A    = new com.github.catexjc.core.CategoryObject<>("A");
        final var B    = new com.github.catexjc.core.CategoryObject<>("B");
        final var idA  = new com.github.catexjc.core.Morphism<>("id_A", A, A);
        final var idB  = new com.github.catexjc.core.Morphism<>("id_B", B, B);
        final var f    = new com.github.catexjc.core.Morphism<>("f", A, B);
        final var g    = new com.github.catexjc.core.Morphism<>("g", A, B);

        final var cat = FiniteCategory.<String,String>builder()
                .addObject(A).addObject(B)
                .addMorphism(idA).addMorphism(idB).addMorphism(f).addMorphism(g)
                .build();

        PosetConverter.toPoset(cat); // should throw
    }
}
