/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.api;

import alexiil.mc.lib.attributes.mixin.api.UnloadableBlockEntity;

/** @deprecated Since LibBlockAttributes now has {@link UnloadableBlockEntity} */
@Deprecated(since = "0.6.0", forRemoval = false)
public interface IUnloadableBlockEntity extends UnloadableBlockEntity {

    @Override
    void onChunkUnload();
}
