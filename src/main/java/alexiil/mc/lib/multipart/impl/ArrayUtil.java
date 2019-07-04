/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.Collections;
import java.util.List;

public final class ArrayUtil {
    private ArrayUtil() {}

    /** Analogous to {@link Collections#reverse(List)} */
    public static void reverse(int[] array) {
        int middle = array.length / 2;
        for (int lower = 0, upper = array.length - 1; lower < middle; lower++, upper--) {
            int val = array[lower];
            array[lower] = array[upper];
            array[upper] = val;
        }
    }
}
