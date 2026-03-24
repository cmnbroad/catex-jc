package com.github.catexjc.core;

/**
 * Interface for domain objects that know how to expose themselves as a
 * {@link FiniteCategory}.
 *
 * <p>Implementors describe the categorical structure of their domain data.
 * The resulting {@link FiniteCategory} can then be fed into any of the
 * library's converters to obtain graphs, posets, lattices, etc.
 *
 * <p>Example skeleton:
 * <pre>{@code
 * public class MyDomain implements CategorySource<String, String> {
 *
 *     @Override
 *     public FiniteCategory<String, String> toCategory() {
 *         var a = new CategoryObject<>("A");
 *         var b = new CategoryObject<>("B");
 *         var idA = new Morphism<>("id_A", a, a);
 *         var idB = new Morphism<>("id_B", b, b);
 *         var f   = new Morphism<>("f",    a, b);
 *
 *         return FiniteCategory.<String, String>builder()
 *                 .addObject(a).addObject(b)
 *                 .addMorphism(idA).addMorphism(idB).addMorphism(f)
 *                 .addComposition(idB, f, f)   // id_B ∘ f = f
 *                 .addComposition(f, idA, f)   // f ∘ id_A = f
 *                 .addComposition(idA, idA, idA)
 *                 .addComposition(idB, idB, idB)
 *                 .build();
 *     }
 * }
 * }</pre>
 *
 * @param <OL> type of object labels
 * @param <ML> type of morphism labels
 */
public interface CategorySource<OL, ML> {

    /**
     * Converts this domain object into a {@link FiniteCategory}.
     *
     * @return a finite category representation of this object's structure
     */
    FiniteCategory<OL, ML> toCategory();
}
