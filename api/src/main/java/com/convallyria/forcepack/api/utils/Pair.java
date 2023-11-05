package com.convallyria.forcepack.api.utils;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

/**
 * Immutable generic 2-tuple
 *
 * @param <U> First type
 * @param <V> Second type
 */
public class Pair<U, V> implements Tuple {

    private final U first;
    private final V second;

    protected Pair(
            final U first,
            final V second
    ) {
        this.first = first;
        this.second = second;
    }

    /**
     * Create a new 2-tuple
     *
     * @param first  First value
     * @param second Second value
     * @param <U>    First type
     * @param <V>    Second type
     * @return Created pair
     */
    @NonNull
    public static <U, V> Pair<U, V> of(
            final U first,
            final V second
    ) {
        return new Pair<>(first, second);
    }

    /**
     * Get the first value
     *
     * @return First value
     */
    public final U getFirst() {
        return this.first;
    }

    /**
     * Get the second value
     *
     * @return Second value
     */
    public final V getSecond() {
        return this.second;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(getFirst(), pair.getFirst()) && Objects.equals(getSecond(), pair.getSecond());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getFirst(), getSecond());
    }

    @Override
    public final String toString() {
        return String.format("(%s, %s)", this.first, this.second);
    }

    @Override
    public final int getSize() {
        return 2;
    }

    @Override
    @NonNull
    public final Object[] toArray() {
        final Object[] array = new Object[2];
        array[0] = this.first;
        array[1] = this.second;
        return array;
    }

}
