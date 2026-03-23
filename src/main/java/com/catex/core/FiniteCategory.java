package com.catex.core;

import java.util.*;

/**
 * An immutable finite category: a finite set of objects, a finite set of
 * morphisms, and a partial composition map.
 *
 * <p>Category laws that a well-formed instance must satisfy:
 * <ol>
 *   <li><b>Identity</b>   — every object A has exactly one identity morphism
 *       {@code id_A : A → A} stored in the morphism set.</li>
 *   <li><b>Associativity</b> — if {@code h∘(g∘f)} is defined, it equals
 *       {@code (h∘g)∘f}.</li>
 *   <li><b>Unit laws</b>  — {@code id_B ∘ f = f} and {@code f ∘ id_A = f}.</li>
 * </ol>
 *
 * <p>These laws are checked lazily by {@link #validate()}.
 *
 * @param <OL> object-label type
 * @param <ML> morphism-label type
 */
public final class FiniteCategory<OL, ML> {

    /** Composition key: (first morphism label, second morphism label) → composite. */
    public record CompositionKey<ML>(ML first, ML second) {}

    private final Set<CategoryObject<OL>> objects;
    private final Set<Morphism<ML, OL>>   morphisms;
    /** Partial composition table: compose(g, f) = g∘f where codomain(f)=domain(g). */
    private final Map<CompositionKey<ML>, Morphism<ML, OL>> compositionTable;

    private FiniteCategory(
            Set<CategoryObject<OL>>              objects,
            Set<Morphism<ML, OL>>                morphisms,
            Map<CompositionKey<ML>, Morphism<ML, OL>> compositionTable) {
        this.objects          = Collections.unmodifiableSet(new LinkedHashSet<>(objects));
        this.morphisms        = Collections.unmodifiableSet(new LinkedHashSet<>(morphisms));
        this.compositionTable = Collections.unmodifiableMap(new LinkedHashMap<>(compositionTable));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Set<CategoryObject<OL>> getObjects()   { return objects; }
    public Set<Morphism<ML, OL>>   getMorphisms()  { return morphisms; }

    /**
     * Returns all morphisms whose domain equals the given object.
     */
    public Set<Morphism<ML, OL>> morphismsFrom(CategoryObject<OL> source) {
        final Set<Morphism<ML, OL>> result = new LinkedHashSet<>();
        for (Morphism<ML, OL> m : morphisms) {
            if (m.getDomain().equals(source)) {
                result.add(m);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns all morphisms whose codomain equals the given object.
     */
    public Set<Morphism<ML, OL>> morphismsTo(CategoryObject<OL> target) {
        final Set<Morphism<ML, OL>> result = new LinkedHashSet<>();
        for (Morphism<ML, OL> m : morphisms) {
            if (m.getCodomain().equals(target)) {
                result.add(m);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Computes g∘f if defined (codomain(f) == domain(g) and the pair is in the
     * composition table), otherwise returns {@link Optional#empty()}.
     */
    public Optional<Morphism<ML, OL>> compose(Morphism<ML, OL> g, Morphism<ML, OL> f) {
        return Optional.ofNullable(
                compositionTable.get(new CompositionKey<>(g.getLabel(), f.getLabel())));
    }

    /**
     * Returns the identity morphism for the given object, if present.
     */
    public Optional<Morphism<ML, OL>> identityOf(CategoryObject<OL> object) {
        return morphisms.stream()
                .filter(m -> m.isIdentity() && m.getDomain().equals(object))
                .findFirst();
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validates the category laws.  Returns a list of violation descriptions;
     * an empty list means the category is well-formed.
     */
    public List<String> validate() {
        final List<String> errors = new ArrayList<>();

        // 1. Every object must have an identity morphism
        for (CategoryObject<OL> obj : objects) {
            if (identityOf(obj).isEmpty()) {
                errors.add("No identity morphism for object: " + obj);
            }
        }

        // Build a label→morphism lookup for composition checks
        final Map<ML, Morphism<ML, OL>> byLabel = new LinkedHashMap<>();
        for (Morphism<ML, OL> m : morphisms) {
            byLabel.put(m.getLabel(), m);
        }

        // 2. Unit laws and composition type-checking
        for (Map.Entry<CompositionKey<ML>, Morphism<ML, OL>> entry : compositionTable.entrySet()) {
            final ML gLabel = entry.getKey().first();
            final ML fLabel = entry.getKey().second();
            final Morphism<ML, OL> composite = entry.getValue();
            final Morphism<ML, OL> g = byLabel.get(gLabel);
            final Morphism<ML, OL> f = byLabel.get(fLabel);

            if (g == null || f == null) {
                errors.add("Composition references unknown morphism(s): " + gLabel + ", " + fLabel);
                continue;
            }
            if (!f.getCodomain().equals(g.getDomain())) {
                errors.add("Composition type mismatch: codomain(" + f.getLabel()
                        + ") != domain(" + g.getLabel() + ")");
            }
            if (!composite.getDomain().equals(f.getDomain())) {
                errors.add("Composite domain wrong for: " + gLabel + "∘" + fLabel);
            }
            if (!composite.getCodomain().equals(g.getCodomain())) {
                errors.add("Composite codomain wrong for: " + gLabel + "∘" + fLabel);
            }
        }

        return errors;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static <OL, ML> Builder<OL, ML> builder() {
        return new Builder<>();
    }

    public static final class Builder<OL, ML> {

        private final Set<CategoryObject<OL>>                  objects          = new LinkedHashSet<>();
        private final Set<Morphism<ML, OL>>                    morphisms        = new LinkedHashSet<>();
        private final Map<CompositionKey<ML>, Morphism<ML, OL>> compositionTable = new LinkedHashMap<>();

        public Builder<OL, ML> addObject(CategoryObject<OL> obj) {
            objects.add(obj);
            return this;
        }

        public Builder<OL, ML> addMorphism(Morphism<ML, OL> m) {
            morphisms.add(m);
            return this;
        }

        /**
         * Records that g∘f = composite.
         */
        public Builder<OL, ML> addComposition(Morphism<ML, OL> g, Morphism<ML, OL> f,
                                               Morphism<ML, OL> composite) {
            compositionTable.put(new CompositionKey<>(g.getLabel(), f.getLabel()), composite);
            return this;
        }

        public FiniteCategory<OL, ML> build() {
            return new FiniteCategory<>(objects, morphisms, compositionTable);
        }
    }

    @Override
    public String toString() {
        return "FiniteCategory{objects=" + objects.size() + ", morphisms=" + morphisms.size() + "}";
    }
}
