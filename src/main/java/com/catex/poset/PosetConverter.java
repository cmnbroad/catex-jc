package com.catex.poset;

import com.catex.core.CategoryObject;
import com.catex.core.FiniteCategory;
import com.catex.core.Morphism;

import java.util.*;

/**
 * Converts between {@link FiniteCategory} and {@link PartialOrder}.
 *
 * <h3>Category → Poset</h3>
 * A category is a thin category (poset) when there is at most one morphism
 * between any ordered pair of objects.  Under this correspondence:
 * <ul>
 *   <li>Objects become poset elements.</li>
 *   <li>The existence of a morphism A → B is interpreted as A ≤ B.</li>
 *   <li>Identity morphisms encode reflexivity.</li>
 *   <li>Composition encodes transitivity.</li>
 * </ul>
 * If the category has multiple morphisms between the same pair of objects a
 * {@link IllegalArgumentException} is thrown because the structure is not a
 * thin category.
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
     * Converts a thin finite category to a {@link PartialOrder}.
     *
     * @throws IllegalArgumentException if the category is not thin
     */
    public static <OL, ML> PartialOrder<OL> toPoset(FiniteCategory<OL, ML> category) {
        // Verify thinness: at most one morphism per (domain, codomain) pair
        Map<OL, Map<OL, Integer>> counts = new LinkedHashMap<>();
        for (Morphism<ML, OL> m : category.getMorphisms()) {
            OL src = m.getDomain().getLabel();
            OL tgt = m.getCodomain().getLabel();
            counts.computeIfAbsent(src, k -> new LinkedHashMap<>())
                  .merge(tgt, 1, Integer::sum);
        }
        for (var outer : counts.entrySet())
            for (var inner : outer.getValue().entrySet())
                if (inner.getValue() > 1)
                    throw new IllegalArgumentException(
                            "Category is not thin: " + inner.getValue()
                            + " morphisms from " + outer.getKey() + " to " + inner.getKey());

        // Build leq map: a ≤ b iff there exists a morphism a → b
        Set<OL> elements = new LinkedHashSet<>();
        Map<OL, Set<OL>> leq = new LinkedHashMap<>();

        for (CategoryObject<OL> obj : category.getObjects()) {
            elements.add(obj.getLabel());
            leq.put(obj.getLabel(), new LinkedHashSet<>());
        }
        for (Morphism<ML, OL> m : category.getMorphisms()) {
            leq.get(m.getDomain().getLabel()).add(m.getCodomain().getLabel());
        }

        return new PartialOrder<>(elements, leq);
    }

    // -------------------------------------------------------------------------
    // Poset → Category
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link PartialOrder} to a thin {@link FiniteCategory}.
     */
    public static <E> FiniteCategory<E, String> fromPoset(PartialOrder<E> poset) {
        FiniteCategory.Builder<E, String> builder = FiniteCategory.builder();

        // Objects
        for (E e : poset.getElements())
            builder.addObject(new CategoryObject<>(e));

        // Morphisms: one per relation a ≤ b
        for (E a : poset.getElements()) {
            for (E b : poset.upperSet(a)) {
                String label = morphismLabel(a, b);
                var mAB = new Morphism<>(label, new CategoryObject<>(a), new CategoryObject<>(b));
                builder.addMorphism(mAB);
            }
        }

        // Composition: if a ≤ b and b ≤ c then (b≤c) ∘ (a≤b) = (a≤c)
        for (E a : poset.getElements()) {
            for (E b : poset.upperSet(a)) {
                for (E c : poset.upperSet(b)) {
                    var mAB = new Morphism<>(morphismLabel(a, b),
                            new CategoryObject<>(a), new CategoryObject<>(b));
                    var mBC = new Morphism<>(morphismLabel(b, c),
                            new CategoryObject<>(b), new CategoryObject<>(c));
                    var mAC = new Morphism<>(morphismLabel(a, c),
                            new CategoryObject<>(a), new CategoryObject<>(c));
                    builder.addComposition(mBC, mAB, mAC);
                }
            }
        }

        return builder.build();
    }

    private static <E> String morphismLabel(E a, E b) {
        return a.equals(b) ? "id_" + a : a + "≤" + b;
    }
}
