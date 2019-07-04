/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.render;

import alexiil.mc.lib.multipart.api.AbstractPart;

public interface PartRenderer<P extends AbstractPart> {
    void render(P part, double x, double y, double z, float partialTicks, int breakProgress);
}
