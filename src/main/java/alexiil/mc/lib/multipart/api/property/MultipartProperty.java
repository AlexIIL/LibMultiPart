/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.property;

import java.util.List;

public abstract class MultipartProperty<T> {
    public final String name;
    public final Class<T> clazz;
    public final T defaultValue;

    private final String asString;

    public MultipartProperty(String name, Class<T> clazz, T defaultValue) {
        this.name = name;
        this.clazz = clazz;
        this.defaultValue = defaultValue;
        this.asString = name + "<" + clazz.getSimpleName() + ">";
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public final String toString() {
        return asString;
    }

    /** @param values A non-empty list of property values.
     * @return The combined value. */
    public abstract T combine(List<T> values);

    public static class IntegerBoundProperty extends MultipartProperty<Integer> {

        public final Integer min, max;

        public IntegerBoundProperty(String name, int min, int max, int defaultValue) {
            super(name, Integer.class, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        public Integer combine(List<Integer> values) {
            Integer highest = defaultValue;
            for (Integer val : values) {
                if (val < min) continue;
                if (val >= max) return max;
                if (val > highest) {
                    highest = val;
                }
            }
            return highest;
        }
    }

    /** A property where the value will combine up to whatever is opposite to the
     * {@link MultipartProperty#defaultValue} */
    public static class PreferedBooleanProperty extends MultipartProperty<Boolean> {

        public PreferedBooleanProperty(String name, Boolean defaultValue) {
            super(name, Boolean.class, defaultValue);
        }

        @Override
        public Boolean combine(List<Boolean> values) {
            for (Boolean val : values) {
                if (val != null && val.booleanValue() != defaultValue.booleanValue()) {
                    return val;
                }
            }
            return defaultValue;
        }
    }
}
