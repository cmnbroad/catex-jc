package com.github.catexjc;

import com.github.catexjc.core.CategoryObject;
import com.github.catexjc.core.CategorySource;
import com.github.catexjc.core.FiniteCategory;
import com.github.catexjc.core.Morphism;

/**
 * Shared test fixtures.
 *
 * <p>Three canonical categories are provided:
 * <ol>
 *   <li>{@link #twoObjectCategory()}  — two objects A, B with a single non-identity morphism f : A→B</li>
 *   <li>{@link #chainCategory()}      — chain A ≤ B ≤ C (preorder / poset)</li>
 *   <li>{@link #diamondCategory()}    — diamond lattice ⊥ ≤ {a,b} ≤ ⊤</li>
 * </ol>
 */
public final class Fixtures {

    private Fixtures() {}

    // -----------------------------------------------------------------------
    // Two-object category:  A --f--> B
    // -----------------------------------------------------------------------

    public static FiniteCategory<String, String> twoObjectCategory() {
        var A   = new CategoryObject<>("A");
        var B   = new CategoryObject<>("B");
        var idA = new Morphism<>("id_A", A, A);
        var idB = new Morphism<>("id_B", B, B);
        var f   = new Morphism<>("f", A, B);

        return FiniteCategory.<String, String>builder()
                .addObject(A).addObject(B)
                .addMorphism(idA).addMorphism(idB).addMorphism(f)
                .addComposition(idB, f,   f)
                .addComposition(f,   idA, f)
                .addComposition(idA, idA, idA)
                .addComposition(idB, idB, idB)
                .build();
    }

    // -----------------------------------------------------------------------
    // Chain category:  A ≤ B ≤ C
    // -----------------------------------------------------------------------

    public static FiniteCategory<String, String> chainCategory() {
        var A   = new CategoryObject<>("A");
        var B   = new CategoryObject<>("B");
        var C   = new CategoryObject<>("C");
        var idA = new Morphism<>("id_A", A, A);
        var idB = new Morphism<>("id_B", B, B);
        var idC = new Morphism<>("id_C", C, C);
        var ab  = new Morphism<>("A≤B", A, B);
        var bc  = new Morphism<>("B≤C", B, C);
        var ac  = new Morphism<>("A≤C", A, C);

        return FiniteCategory.<String, String>builder()
                .addObject(A).addObject(B).addObject(C)
                .addMorphism(idA).addMorphism(idB).addMorphism(idC)
                .addMorphism(ab).addMorphism(bc).addMorphism(ac)
                // identity compositions
                .addComposition(idA, idA, idA)
                .addComposition(idB, idB, idB)
                .addComposition(idC, idC, idC)
                // unit laws
                .addComposition(idB, ab, ab).addComposition(ab, idA, ab)
                .addComposition(idC, bc, bc).addComposition(bc, idB, bc)
                .addComposition(idC, ac, ac).addComposition(ac, idA, ac)
                // transitivity
                .addComposition(bc, ab, ac)
                .build();
    }

    // -----------------------------------------------------------------------
    // Diamond lattice:  ⊥ ≤ {a, b} ≤ ⊤
    // -----------------------------------------------------------------------

    public static FiniteCategory<String, String> diamondCategory() {
        var bot  = new CategoryObject<>("bot");
        var a    = new CategoryObject<>("a");
        var b    = new CategoryObject<>("b");
        var top  = new CategoryObject<>("top");

        var idBot = new Morphism<>("id_bot", bot, bot);
        var idA   = new Morphism<>("id_a",   a,   a);
        var idB   = new Morphism<>("id_b",   b,   b);
        var idTop = new Morphism<>("id_top", top, top);

        var botA  = new Morphism<>("bot≤a",  bot, a);
        var botB  = new Morphism<>("bot≤b",  bot, b);
        var aTop  = new Morphism<>("a≤top",  a,   top);
        var bTop  = new Morphism<>("b≤top",  b,   top);
        var botTop= new Morphism<>("bot≤top",bot, top);

        return FiniteCategory.<String, String>builder()
                .addObject(bot).addObject(a).addObject(b).addObject(top)
                .addMorphism(idBot).addMorphism(idA).addMorphism(idB).addMorphism(idTop)
                .addMorphism(botA).addMorphism(botB).addMorphism(aTop).addMorphism(bTop)
                .addMorphism(botTop)
                // identity compositions
                .addComposition(idBot, idBot, idBot)
                .addComposition(idA,   idA,   idA)
                .addComposition(idB,   idB,   idB)
                .addComposition(idTop, idTop, idTop)
                // unit laws
                .addComposition(idA,   botA, botA).addComposition(botA, idBot, botA)
                .addComposition(idB,   botB, botB).addComposition(botB, idBot, botB)
                .addComposition(idTop, aTop, aTop).addComposition(aTop, idA,   aTop)
                .addComposition(idTop, bTop, bTop).addComposition(bTop, idB,   bTop)
                .addComposition(idTop, botTop, botTop).addComposition(botTop, idBot, botTop)
                // transitivity
                .addComposition(aTop, botA, botTop)
                .addComposition(bTop, botB, botTop)
                .build();
    }

    // -----------------------------------------------------------------------
    // Example CategorySource implementation for testing
    // -----------------------------------------------------------------------

    /** A minimal domain object that wraps the chain A≤B≤C category. */
    public static class ChainDomain implements CategorySource<String, String> {
        @Override
        public FiniteCategory<String, String> toCategory() {
            return chainCategory();
        }
    }
}
