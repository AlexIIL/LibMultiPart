/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.impl;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// @Mixin(BlockEntity.class)
// Yes this is disabled in the mixin json ATM
// This will be enabled in the future, after testing?
public class BlockEntityMixin {

    // Thanks to lambdas these are all static rather than non-static

    @Redirect(
        at = @At(value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;"
                + "error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
            remap = false, args = "log=true"),
        method = "*")
    private static void hardThrow(Logger logger, String msg, Object first, Object second) {
        logger.error(msg, first, second);
        if (second instanceof Error) {
            throw (Error) second;
        }
        if (second instanceof RuntimeException) {
            throw (RuntimeException) second;
        }
        if (second instanceof Throwable) {
            throw new Error((Throwable) second);
        }
    }
}
