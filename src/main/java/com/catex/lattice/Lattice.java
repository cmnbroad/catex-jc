package com.catex.lattice;

import com.catex.poset.PartialOrder;

import java.util.*;

/**
 * An immutable finite lattice — a poset where every pair of elements has
 * both a <em>join</em> (least upper bound, ⊔) and a <em>meet</em>
 * (greatest lower bound, ⊓).
 *
 * <p>A finite lattice always has a minimum element (⊥, bottom) and a maximum
 * element (⊤, top).
 *
 * @param <E> element type
 */
public final class Lattice<E> {

    private final PartialOrder<E>  order;
    private final Map<Pair<E>, E>  joinTable;
    private final Map<Pair<E>, E>  meetTable;

    Lattice(final PartialOrder<E> order, final Map<Pair<E>, E> joinTable, final Map<Pair<E>, E> meetTable) {
        this.order     = order;
        this.joinTable = Collections.unmodifiableMap(new LinkedHashMap<>(joinTable));
        this.meetTable = Collections.unmodifiableMap(new LinkedHashMap<>(meetTable));
    }

    // -------------------------------------------------------------------------
    // Core operations
    // -------------------------------------------------------------------------

    public Set<E> getElements() { return order.getElements(); }
    public PartialOrder<E> getOrder() { return order; }

    /**
     * Returns the join (least upper bound) of {@code a} and {@code b}.
     *
     * @throws NoSuchElementException if the join is not defined (lattice is incomplete)
     */
    public E join(final E a, final E b) {
        final E result = joinTable.get(new Pair<>(a, b));
        if (result == null) {
            throw new NoSuchElementException("Join not defined for " + a + ", " + b);
        }
        return result;
    }

    /**
     * Returns the meet (greatest lower bound) of {@code a} and {@code b}.
     *
     * @throws NoSuchElementException if the meet is not defined (lattice is incomplete)
     */
    public E meet(final E a, final E b) {
        final E result = meetTable.get(new Pair<>(a, b));
        if (result == null) {
            throw new NoSuchElementException("Meet not defined for " + a + ", " + b);
        }
        return result;
    }

    /**
     * Returns the bottom element (minimum) of the lattice.
     */
    public Optional<E> bottom() {
        for (final E e : order.getElements()) {
            if (order.lowerSet(e).size() == 1) { // only itself
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the top element (maximum) of the lattice.
     */
    public Optional<E> top() {
        for (final E e : order.getElements()) {
            if (order.upperSet(e).size() == 1) { // only itself
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Validates that every pair of elements has a join and a meet.
     * Returns an empty list when the lattice is well-formed.
     */
    public List<String> validate() {
        final List<String> errors = new ArrayList<>(order.validate());
        final List<E> elems = new ArrayList<>(order.getElements());
        for (final E a : elems) {
            for (final E b : elems) {
                if (!joinTable.containsKey(new Pair<>(a, b))) {
                    errors.add("Join missing for (" + a + ", " + b + ")");
                }
                if (!meetTable.containsKey(new Pair<>(a, b))) {
                    errors.add("Meet missing for (" + a + ", " + b + ")");
                }
            }
        }
        return errors;
    }

    @Override
    public String toString() {
        return "Lattice{elements=" + order.getElements().size() + "}";
    }

    // -------------------------------------------------------------------------
    // Internal pair key (unordered for join/meet since they are commutative)
    // -------------------------------------------------------------------------

    record Pair<E>(E first, E second) {
        /** Normalise so that {a,b} == {b,a} for symmetric operations. */
        static <E extends Comparable<E>> Pair<E> of(final E a, final E b) {
            return (a.compareTo(b) <= 0) ? new Pair<>(a, b) : new Pair<>(b, a);
        }
    }
}
