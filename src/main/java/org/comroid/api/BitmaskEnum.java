package org.comroid.api;

import org.comroid.util.Bitmask;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface BitmaskEnum<S extends BitmaskEnum<S>> extends IntEnum, SelfDeclared<S>, Named {
    @Override
    @NotNull
    default Integer getValue() {
        return 1 << IntEnum.super.getValue();
    }

    static <T extends java.lang.Enum<? extends T> & BitmaskEnum<T>> Set<T> valueOf(int mask, Class<T> viaEnum) {
        if (!viaEnum.isEnum())
            throw new IllegalArgumentException("Only enums allowed as parameter 'viaEnum'");

        return valueOf(mask, viaEnum.getEnumConstants());
    }

    static <T extends BitmaskEnum<T>> Set<T> valueOf(
            int mask,
            final T[] values
    ) {
        HashSet<T> yields = new HashSet<>();

        for (T constant : values) {
            if (constant.isFlagSet(mask))
                yields.add(constant);
        }

        return Collections.unmodifiableSet(yields);
    }

    static int toMask(BitmaskEnum<?>[] values) {
        int x = 0;
        for (BitmaskEnum<?> each : values)
            each.apply(x, true);
        return x;
    }

    default boolean hasFlag(BitmaskEnum<S> other) {
        return Bitmask.isFlagSet(getValue(), other.getValue());
    }

    default boolean isFlagSet(int inMask) {
        return Bitmask.isFlagSet(inMask, getValue());
    }

    default int apply(int toMask, boolean newState) {
        return Bitmask.modifyFlag(toMask, getValue(), newState);
    }

    @Override
    default boolean equals(int value) {
        return getValue() == value;
    }

    default boolean equals(BitmaskEnum<?> other) {
        return getValue() == other.getValue();
    }
}
