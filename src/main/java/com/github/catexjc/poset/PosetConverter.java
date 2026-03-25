// Copyright 2026 Christopher Norman
package com.github.catexjc.poset;

import com.github.catexjc.core.CategoryObject;
import com.github.catexjc.core.FiniteCategory;
import com.github.catexjc.core.Morphism;

import java.util.*;

/**
 * Converts between {@link FiniteCategory} and {@link PartialOrder}.
 *
 * <h3>Category → Poset</h3>
 * A category is a preorder when there is at most one morphism
 * between any ordered pair of objects.  Under this correspondence:
 * <ul>
 *   <li>Objects become poset elements.</li>
 *   <li>The existence of a morphism A → B is interpreted as A ≤ B.</li>
 *   <li>Identity morphisms encode reflexivity.</li>
 *   <li>Composition encodes transitivity.</li>
 * </ul>
 * If the category has multiple morphisms between the same pair of objects a
 * {@link IllegalArgumentException} is thrown because the structure is not a
 * preorder.
 *
 * <h3>Poset → Category</h3>
 * Each relation a ≤ b becomes a unique morphism {@code "a≤b"} from
 * {@code Obj(a)} to {@code Obj(b)}.  Composition is fully populated from
 * transitivity.
 */
public final class PosetConverter {

    private PosetConverter() {}

    // -------------------------------------------------------------------------
    // Category → Poset
    // -------------------------------------------------------------------------

    /**
     * Converts a preorder finite category to a {@link PartialOrder}.
     *
     * @throws IllegalArgumentException if the category is not a preorder
     */
    public static <OL, ML> PartialOrder<OL> toPoset(final FiniteCategory<OL, ML> category) {
        // Verify preorder: at most one morphism per (domain, codomain) pair
        final Map<OL, Map<OL, Integer>> counts = new LinkedHashMap<>();
        for (final Morphism<ML, OL> m : category.getMorphisms()) {
            final OL src = m.getDomain().getLabel();
            final OL tgt = m.getCodomain().getLabel();
            counts.computeIfAbsent(src, k -> new LinkedHashMap<>())
                  .merge(tgt, 1, Integer::sum);
        }
        for (final var outer : counts.entrySet()) {
            for (final var inner : outer.getValue().entrySet()) {
                if (inner.getValue() > 1) {
                    throw new IllegalArgumentException(
                            "Category is not a preorder: " + inner.getValue()
                            + " morphisms from " + outer.getKey() + " to " + inner.getKey());
                }
            }
        }

        // Build leq map: a ≤ b iff there exists a morphism a → b
        final Set<OL> elements = new LinkedHashSet<>();
        final Map<OL, Set<OL>> leq = new LinkedHashMap<>();

        for (final CategoryObject<OL> obj : category.getObjects()) {
            elements.add(obj.getLabel());
            leq.put(obj.getLabel(), new LinkedHashSet<>());
        }
        for (final Morphism<ML, OL> m : category.getMorphisms()) {
            leq.get(m.getDomain().getLabel()).add(m.getCodomain().getLabel());
        }

        return new PartialOrder<>(elements, leq);
    }

    // -------------------------------------------------------------------------
    // Poset → Category
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link PartialOrder} to a preorder {@link FiniteCategory}.
     */
    public static <E> FiniteCategory<E, String> fromPoset(final PartialOrder<E> poset) {
        final FiniteCategory.Builder<E, String> builder = FiniteCategory.builder();

        // Objects
        for (final E e : poset.getElements()) {
            builder.addObject(new CategoryObject<>(e));
        }

        // Morphisms: one per relation a ≤ b
        for (final E a : poset.getElements()) {
            for (final E b : poset.upperSet(a)) {
                final String label = morphismLabel(a, b);
                final var mAB = new Morphism<>(label, new CategoryObject<>(a), new CategoryObject<>(b));
                builder.addMorphism(mAB);
            }
        }

        // Composition: if a ≤ b and b ≤ c then (b≤c) ∘ (a≤b) = (a≤c)
        for (final E a : poset.getElements()) {
            for (final E b : poset.upperSet(a)) {
                for (final E c : poset.upperSet(b)) {
                    final var mAB = new Morphism<>(morphismLabel(a, b),
                            new CategoryObject<>(a), new CategoryObject<>(b));
                    final var mBC = new Morphism<>(morphismLabel(b, c),
                            new CategoryObject<>(b), new CategoryObject<>(c));
                    final var mAC = new Morphism<>(morphismLabel(a, c),
                            new CategoryObject<>(a), new CategoryObject<>(c));
                    builder.addComposition(mBC, mAB, mAC);
                }
            }
        }

        return builder.build();
    }

    private static <E> String morphismLabel(final E a, final E b) {
        return a.equals(b) ? "id_" + a : a + "≤" + b;
    }
}
