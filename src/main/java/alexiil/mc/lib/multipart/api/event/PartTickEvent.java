/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

/** Fired once per block entity tick. */
public final class PartTickEvent extends MultipartEvent {
    public static final PartTickEvent INSTANCE = new PartTickEvent();

    private PartTickEvent() {}
}
