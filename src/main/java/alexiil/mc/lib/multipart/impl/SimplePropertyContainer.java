/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import alexiil.mc.lib.multipart.api.MultipartContainer;
import alexiil.mc.lib.multipart.api.property.MultipartProperty;
import alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer;
import alexiil.mc.lib.multipart.api.property.PartPropertyChangedEvent;

public class SimplePropertyContainer implements MultipartPropertyContainer {

    public final PartContainer container;
    private final Map<MultipartProperty<?>, InternalContainer<?>> properties = new IdentityHashMap<>();

    public SimplePropertyContainer(PartContainer container) {
        this.container = container;
    }

    @Override
    public MultipartContainer getContainer() {
        return container;
    }

    @Override
    public <T> T getValue(MultipartProperty<T> property) {
        InternalContainer<?> ic = properties.get(property);
        if (ic == null) {
            return property.defaultValue;
        } else {
            return property.clazz.cast(ic.combinedValue);
        }
    }

    @Override
    public <T> void setValue(Object key, MultipartProperty<T> property, T value) {
        if (value == null) {
            value = property.defaultValue;
        }
        InternalContainer<?> ic = properties.get(property);
        if (ic == null) {
            if (Objects.equals(value, property.defaultValue)) {
                return;
            }
            ic = new InternalContainer<>(property);
            properties.put(property, ic);
        }
        ic.setUnknownValue(key, value);
        if (value == null && ic.values.isEmpty()) {
            InternalContainer<?> removed = properties.remove(key);
            assert removed == ic;
        }
    }

    @Override
    public <T> void clearValue(Object key, MultipartProperty<T> property) {
        setValue(key, property, property.defaultValue);
    }

    @Override
    public void clearValues(Object key) {
        Iterator<InternalContainer<?>> iter = properties.values().iterator();
        while (iter.hasNext()) {
            InternalContainer<?> ic = iter.next();
            ic.setValue(key, null);
            if (ic.values.isEmpty()) {
                iter.remove();
            }
        }
    }

    private class InternalContainer<T> {
        final MultipartProperty<T> property;
        final Map<Object, T> values = new IdentityHashMap<>();
        T combinedValue;

        InternalContainer(MultipartProperty<T> property) {
            this.property = property;
            combinedValue = property.defaultValue;
        }

        void setUnknownValue(Object key, Object value) {
            setValue(key, property.clazz.cast(value));
        }

        void setValue(Object key, T value) {
            if (value == null) {
                value = property.defaultValue;
            }
            T old;
            if (Objects.equals(value, property.defaultValue)) {
                old = values.remove(key);
            } else {
                old = values.put(key, value);
            }
            if (old == null) {
                old = property.defaultValue;
            }
            if (Objects.equals(old, value)) {
                // No reason to recompute
                return;
            }

            T oldCombined = combinedValue;
            List<T> list = new ArrayList<>();
            list.addAll(values.values());
            if (list.isEmpty()) {
                combinedValue = property.defaultValue;
            } else if (list.size() == 1) {
                combinedValue = list.get(0);
            } else {
                combinedValue = property.combine(list);
            }

            if (Objects.equals(oldCombined, combinedValue)) {
                // No reason to fire the event
                return;
            }

            container.onPropertyChanged(property, oldCombined, combinedValue);
            container.fireEvent(new PartPropertyChangedEvent<>(property, oldCombined, combinedValue));
        }
    }
}
