package com.catex.lattice;

import com.catex.core.FiniteCategory;
import com.catex.poset.PartialOrder;
import com.catex.poset.PosetConverter;

import java.util.*;

/**
 * Converts between {@link FiniteCategory}, {@link PartialOrder} and {@link Lattice}.
 *
 * <h3>Poset → Lattice</h3>
 * Checks that every pair of elements has a least upper bound and a greatest
 * lower bound, then builds the join and meet tables.  Throws
 * {@link IllegalArgumentException} if the poset is not a lattice.
 *
 * <h3>Category → Lattice</h3>
 * Delegates to {@link PosetConverter#toPoset} then to
 * {@link #fromPoset(PartialOrder)}.
 *
 * <h3>Lattice → Category</h3>
 * Delegates to {@link PosetConverter#fromPoset}.
 */
public final class LatticeConverter {

    private LatticeConverter() {}

    // -------------------------------------------------------------------------
    // Poset → Lattice
    // -------------------------------------------------------------------------

    /**
     * Promotes a {@link PartialOrder} to a {@link Lattice}.
     *
     * @throws IllegalArgumentException if the poset is not a lattice
     */
    public static <E> Lattice<E> fromPoset(final PartialOrder<E> poset) {
        final List<E> elems = new ArrayList<>(poset.getElements());
        final Map<Lattice.Pair<E>, E> joinTable = new LinkedHashMap<>();
        final Map<Lattice.Pair<E>, E> meetTable = new LinkedHashMap<>();

        for (final E a : elems) {
            for (final E b : elems) {
                final Lattice.Pair<E> key = new Lattice.Pair<>(a, b);
                joinTable.put(key, computeJoin(a, b, poset));
                meetTable.put(key, computeMeet(a, b, poset));
            }
        }
        return new Lattice<>(poset, joinTable, meetTable);
    }

    // -------------------------------------------------------------------------
    // Category → Lattice
    // -------------------------------------------------------------------------

    /**
     * Converts a preorder finite category (poset) to a {@link Lattice}.
     */
    public static <OL, ML> Lattice<OL> fromCategory(final FiniteCategory<OL, ML> category) {
        return fromPoset(PosetConverter.toPoset(category));
    }

    // -------------------------------------------------------------------------
    // Lattice → Category
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link Lattice} to a preorder finite category via its underlying
     * partial order.
     */
    public static <E> FiniteCategory<E, String> toCategory(final Lattice<E> lattice) {
        return PosetConverter.fromPoset(lattice.getOrder());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Least upper bound of a and b, or throws if none exists. */
    private static <E> E computeJoin(final E a, final E b, final PartialOrder<E> poset) {
        // Upper bounds of both a and b
        final Set<E> upperA = poset.upperSet(a);
        final Set<E> upperB = poset.upperSet(b);
        final Set<E> common = new LinkedHashSet<>(upperA);
        common.retainAll(upperB);

        if (common.isEmpty()) {
            throw new IllegalArgumentException("No upper bound for " + a + " and " + b);
        }

        // Least among the common upper bounds
        for (final E candidate : common) {
            boolean least = true;
            for (final E other : common) {
                if (!poset.leq(candidate, other) && !candidate.equals(other)) {
                    least = false;
                    break;
                }
            }
            if (least) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("No least upper bound for " + a + " and " + b);
    }

    /** Greatest lower bound of a and b, or throws if none exists. */
    private static <E> E computeMeet(final E a, final E b, final PartialOrder<E> poset) {
        final Set<E> lowerA = poset.lowerSet(a);
        final Set<E> lowerB = poset.lowerSet(b);
        final Set<E> common = new LinkedHashSet<>(lowerA);
        common.retainAll(lowerB);

        if (common.isEmpty()) {
            throw new IllegalArgumentException("No lower bound for " + a + " and " + b);
        }

        for (final E candidate : common) {
            boolean greatest = true;
            for (final E other : common) {
                if (!poset.leq(other, candidate) && !candidate.equals(other)) {
                    greatest = false;
                    break;
                }
            }
            if (greatest) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("No greatest lower bound for " + a + " and " + b);
    }
}
