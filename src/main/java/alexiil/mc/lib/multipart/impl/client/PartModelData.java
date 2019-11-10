/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client;

import com.google.common.collect.ImmutableList;

import alexiil.mc.lib.multipart.api.render.PartModelKey;

public final class PartModelData {

    public final ImmutableList<PartModelKey> keys;

    public PartModelData(ImmutableList<PartModelKey> keys) {
        this.keys = keys;
    }
}
