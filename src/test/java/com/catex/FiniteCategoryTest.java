package com.catex;

import com.catex.core.CategoryObject;
import com.catex.core.FiniteCategory;
import com.catex.core.Morphism;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class FiniteCategoryTest {

    // -----------------------------------------------------------------------
    // DataProvider: well-formed categories
    // -----------------------------------------------------------------------

    @DataProvider(name = "validCategories")
    public Object[][] validCategories() {
        return new Object[][] {
            { "two-object",  Fixtures.twoObjectCategory() },
            { "chain A≤B≤C", Fixtures.chainCategory()     },
            { "diamond",     Fixtures.diamondCategory()    },
        };
    }

    @Test(dataProvider = "validCategories")
    public void testValidCategoriesPassValidation(final String name, final FiniteCategory<String, String> cat) {
        List<String> errors = cat.validate();
        assertTrue(errors.isEmpty(),
                "Expected no validation errors for '" + name + "' but got: " + errors);
    }

    // -----------------------------------------------------------------------
    // DataProvider: expected object counts
    // -----------------------------------------------------------------------

    @DataProvider(name = "objectCounts")
    public Object[][] objectCounts() {
        return new Object[][] {
            { Fixtures.twoObjectCategory(), 2 },
            { Fixtures.chainCategory(),     3 },
            { Fixtures.diamondCategory(),   4 },
        };
    }

    @Test(dataProvider = "objectCounts")
    public void testObjectCount(final FiniteCategory<String, String> cat, final int expected) {
        assertEquals(cat.getObjects().size(), expected);
    }

    // -----------------------------------------------------------------------
    // DataProvider: expected morphism counts
    // -----------------------------------------------------------------------

    @DataProvider(name = "morphismCounts")
    public Object[][] morphismCounts() {
        // identities + non-identity morphisms
        return new Object[][] {
            { Fixtures.twoObjectCategory(), 3 },  // id_A, id_B, f
            { Fixtures.chainCategory(),     6 },  // 3 ids + ab + bc + ac
            { Fixtures.diamondCategory(),   9 },  // 4 ids + 4 edges + bot≤top
        };
    }

    @Test(dataProvider = "morphismCounts")
    public void testMorphismCount(final FiniteCategory<String, String> cat, final int expected) {
        assertEquals(cat.getMorphisms().size(), expected);
    }

    // -----------------------------------------------------------------------
    // Identity morphisms
    // -----------------------------------------------------------------------

    @DataProvider(name = "identityChecks")
    public Object[][] identityChecks() {
        return new Object[][] {
            { "A", Fixtures.twoObjectCategory() },
            { "B", Fixtures.twoObjectCategory() },
            { "A", Fixtures.chainCategory()     },
            { "B", Fixtures.chainCategory()     },
            { "C", Fixtures.chainCategory()     },
        };
    }

    @Test(dataProvider = "identityChecks")
    public void testIdentityPresent(final String label, final FiniteCategory<String, String> cat) {
        var obj = new CategoryObject<>(label);
        assertTrue(cat.identityOf(obj).isPresent(), "Identity missing for object " + label);
        assertTrue(cat.identityOf(obj).get().isIdentity());
    }

    // -----------------------------------------------------------------------
    // Composition
    // -----------------------------------------------------------------------

    @Test
    public void testCompositionInChain() {
        var cat = Fixtures.chainCategory();
        var ab  = cat.getMorphisms().stream().filter(m -> m.getLabel().equals("A≤B")).findFirst().orElseThrow();
        var bc  = cat.getMorphisms().stream().filter(m -> m.getLabel().equals("B≤C")).findFirst().orElseThrow();
        var ac  = cat.getMorphisms().stream().filter(m -> m.getLabel().equals("A≤C")).findFirst().orElseThrow();

        var composed = cat.compose(bc, ab);
        assertTrue(composed.isPresent(), "bc ∘ ab should be defined");
        assertEquals(composed.get(), ac, "bc ∘ ab should equal ac");
    }

    @Test
    public void testCompositionUndefinedWhenTypeMismatch() {
        var cat = Fixtures.chainCategory();
        var ab  = cat.getMorphisms().stream().filter(m -> m.getLabel().equals("A≤B")).findFirst().orElseThrow();
        var bc  = cat.getMorphisms().stream().filter(m -> m.getLabel().equals("B≤C")).findFirst().orElseThrow();

        // ab ∘ bc is undefined (wrong order)
        var composed = cat.compose(ab, bc);
        assertFalse(composed.isPresent(), "ab ∘ bc should not be defined");
    }

    // -----------------------------------------------------------------------
    // Category that violates identity requirement
    // -----------------------------------------------------------------------

    @Test
    public void testMissingIdentityFailsValidation() {
        // Build a category with no identity on B
        var A = new CategoryObject<>("A");
        var B = new CategoryObject<>("B");
        var idA = new Morphism<>("id_A", A, A);
        var f   = new Morphism<>("f", A, B);

        var cat = FiniteCategory.<String, String>builder()
                .addObject(A).addObject(B)
                .addMorphism(idA).addMorphism(f)
                .build();

        List<String> errors = cat.validate();
        assertFalse(errors.isEmpty(), "Should have validation error for missing id_B");
    }

    // -----------------------------------------------------------------------
    // morphismsFrom / morphismsTo
    // -----------------------------------------------------------------------

    @Test
    public void testMorphismsFrom() {
        var cat = Fixtures.chainCategory();
        var A   = new CategoryObject<>("A");
        var from = cat.morphismsFrom(A);
        // id_A, A≤B, A≤C
        assertEquals(from.size(), 3);
    }

    @Test
    public void testMorphismsTo() {
        var cat = Fixtures.chainCategory();
        var C   = new CategoryObject<>("C");
        var to  = cat.morphismsTo(C);
        // id_C, B≤C, A≤C
        assertEquals(to.size(), 3);
    }
}
