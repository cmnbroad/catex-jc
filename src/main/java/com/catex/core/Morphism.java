package com.catex.core;

import java.util.Objects;

/**
 * Represents a morphism (arrow) in a finite category.
 *
 * <p>Every morphism has:
 * <ul>
 *   <li>a unique label within the category,</li>
 *   <li>a domain (source object),</li>
 *   <li>a codomain (target object).</li>
 * </ul>
 *
 * <p>Identity morphisms (id_A : A → A) are ordinary morphisms whose domain and
 * codomain coincide; they are not structurally special here but the category is
 * required to contain one for every object.
 *
 * @param <L>  label type for the morphism itself
 * @param <OL> label type of the objects it connects
 */
public final class Morphism<L, OL> {

    private final L label;
    private final CategoryObject<OL> domain;
    private final CategoryObject<OL> codomain;

    public Morphism(final L label, final CategoryObject<OL> domain, final CategoryObject<OL> codomain) {
        this.label    = Objects.requireNonNull(label,    "label must not be null");
        this.domain   = Objects.requireNonNull(domain,   "domain must not be null");
        this.codomain = Objects.requireNonNull(codomain, "codomain must not be null");
    }

    public L getLabel() { return label; }
    public CategoryObject<OL> getDomain()   { return domain; }
    public CategoryObject<OL> getCodomain() { return codomain; }

    /** True when this morphism is the identity morphism on its (shared) object. */
    public boolean isIdentity() {
        return domain.equals(codomain);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Morphism<?, ?> that)) return false;
        return Objects.equals(label, that.label)
            && Objects.equals(domain, that.domain)
            && Objects.equals(codomain, that.codomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, domain, codomain);
    }

    @Override
    public String toString() {
        return "Morphism(" + label + " : " + domain + " -> " + codomain + ")";
    }
}
