package com.catex.core;

import java.util.Objects;

/**
 * Represents an object in a finite category. Objects are identified by a label.
 * In category theory, objects are the "nodes" — morphisms relate them.
 *
 * @param <L> the type used to label / identify this object
 */
public final class CategoryObject<L> {

    private final L label;

    public CategoryObject(L label) {
        this.label = Objects.requireNonNull(label, "label must not be null");
    }

    public L getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryObject<?> that)) return false;
        return Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public String toString() {
        return "Obj(" + label + ")";
    }
}
