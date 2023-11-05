package com.convallyria.forcepack.api.utils;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Tuple type
 */
public interface Tuple {

    /**
     * Get the tuple size
     *
     * @return Tuple size
     */
    int getSize();

    /**
     * Turn the tuple into a type erased array
     *
     * @return Created array
     */
    @NonNull Object[] toArray();

}
