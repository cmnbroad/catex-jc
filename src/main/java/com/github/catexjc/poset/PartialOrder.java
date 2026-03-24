package com.github.catexjc.poset;

import java.util.*;

/**
 * An immutable partially ordered set (poset).
 *
 * <p>A poset is a set {@code P} with a binary relation ≤ that is:
 * <ul>
 *   <li><b>Reflexive</b>  — a ≤ a for all a ∈ P</li>
 *   <li><b>Antisymmetric</b> — a ≤ b and b ≤ a imply a = b</li>
 *   <li><b>Transitive</b> — a ≤ b and b ≤ c imply a ≤ c</li>
 * </ul>
 *
 * <p>Internally the relation is stored as its <em>Hasse diagram</em>
 * (immediate cover relations only) plus the full computed closure, which is
 * derived eagerly on construction.
 *
 * @param <E> element type
 */
public final class PartialOrder<E> {

    /** An immediate covering relation: {@code lower} is covered by {@code upper}. */
    public record Cover<E>(E lower, E upper) {}

    private final Set<E>           elements;
    /** Full reflexive-transitive closure of ≤. */
    private final Map<E, Set<E>>   leq;      // leq.get(a) = { b | a ≤ b }

    PartialOrder(final Set<E> elements, final Map<E, Set<E>> leq) {
        this.elements = Collections.unmodifiableSet(new LinkedHashSet<>(elements));
        final Map<E, Set<E>> copy = new LinkedHashMap<>();
        for (final Map.Entry<E, Set<E>> e : leq.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(e.getValue())));
        }
        this.leq = Collections.unmodifiableMap(copy);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Set<E> getElements() { return elements; }

    /** Returns {@code true} if a ≤ b. */
    public boolean leq(final E a, final E b) {
        final Set<E> above = leq.get(a);
        return above != null && above.contains(b);
    }

    /** Returns all elements x such that a ≤ x. */
    public Set<E> upperSet(final E a) {
        return leq.getOrDefault(a, Collections.emptySet());
    }

    /** Returns all elements x such that x ≤ a. */
    public Set<E> lowerSet(final E a) {
        final Set<E> result = new LinkedHashSet<>();
        for (final E x : elements) {
            if (leq(x, a)) {
                result.add(x);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Returns the cover relations that make up the Hasse diagram. */
    public List<Cover<E>> hasseCovers() {
        final List<Cover<E>> covers = new ArrayList<>();
        for (final E a : elements) {
            for (final E b : elements) {
                if (a.equals(b) || !leq(a, b)) {
                    continue;
                }
                // b covers a iff there is no c with a < c < b
                boolean direct = true;
                for (final E c : elements) {
                    if (c.equals(a) || c.equals(b)) {
                        continue;
                    }
                    if (leq(a, c) && leq(c, b)) {
                        direct = false;
                        break;
                    }
                }
                if (direct) {
                    covers.add(new Cover<>(a, b));
                }
            }
        }
        return covers;
    }

    /**
     * Validates the three poset axioms.
     * Returns an empty list when the poset is well-formed.
     */
    public List<String> validate() {
        final List<String> errors = new ArrayList<>();
        final List<E> elems = new ArrayList<>(elements);

        for (final E a : elems) {
            // Reflexivity
            if (!leq(a, a)) {
                errors.add("Not reflexive: " + a + " ≤ " + a + " missing");
            }

            for (final E b : elems) {
                if (a.equals(b)) {
                    continue;
                }
                if (leq(a, b) && leq(b, a)) {
                    errors.add("Not antisymmetric: " + a + " ≤ " + b + " and " + b + " ≤ " + a);
                }

                for (final E c : elems) {
                    if (leq(a, b) && leq(b, c) && !leq(a, c)) {
                        errors.add("Not transitive: " + a + " ≤ " + b + ", " + b + " ≤ " + c
                                + " but not " + a + " ≤ " + c);
                    }
                }
            }
        }
        return errors;
    }

    @Override
    public String toString() {
        return "PartialOrder{elements=" + elements.size() + "}";
    }
}
