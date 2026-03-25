// Copyright 2026 Christopher Norman
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
        final var A   = new CategoryObject<>("A");
        final var B   = new CategoryObject<>("B");
        final var idA = new Morphism<>("id_A", A, A);
        final  var idB = new Morphism<>("id_B", B, B);
        final var f   = new Morphism<>("f", A, B);

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
        final var A   = new CategoryObject<>("A");
        final var B   = new CategoryObject<>("B");
        final var C   = new CategoryObject<>("C");
        final var idA = new Morphism<>("id_A", A, A);
        final var idB = new Morphism<>("id_B", B, B);
        final var idC = new Morphism<>("id_C", C, C);
        final var ab  = new Morphism<>("A≤B", A, B);
        final var bc  = new Morphism<>("B≤C", B, C);
        final var ac  = new Morphism<>("A≤C", A, C);

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
        final var bot  = new CategoryObject<>("bot");
        final var a    = new CategoryObject<>("a");
        final var b    = new CategoryObject<>("b");
        final var top  = new CategoryObject<>("top");

        final var idBot = new Morphism<>("id_bot", bot, bot);
        final var idA   = new Morphism<>("id_a",   a,   a);
        final var idB   = new Morphism<>("id_b",   b,   b);
        final var idTop = new Morphism<>("id_top", top, top);

        final var botA  = new Morphism<>("bot≤a",  bot, a);
        final var botB  = new Morphism<>("bot≤b",  bot, b);
        final var aTop  = new Morphism<>("a≤top",  a,   top);
        final var bTop  = new Morphism<>("b≤top",  b,   top);
        final var botTop= new Morphism<>("bot≤top",bot, top);

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
