/*
 * Copyright (C) 2025 IO Club
 *
 * This file is part of Workyras.
 *
 * Workyras is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Workyras is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with Workyras.  If not, see <https://www.gnu.org/licenses/>.
 */

package fyi.ioclub.workyras.utils

import android.graphics.Color

const val HUE_MAX_VALUE = 360F
const val SATURATION_MAX_VALUE = 1F
const val LIGHTNESS_MAX_VALUE = 1F

val DEFAULT_COLOR_OFFSET = Color.HSVToColor(
    floatArrayOf(
        0F,
        SATURATION_MAX_VALUE,
        LIGHTNESS_MAX_VALUE
    )
)

fun generateEvenlyDividedColors(count: Int, offset: Int = DEFAULT_COLOR_OFFSET) =
    (HUE_MAX_VALUE / count).let { unit ->
        val hsv = FloatArray(3)
        Color.RGBToHSV(
            Color.red(offset),
            Color.green(offset),
            Color.blue(offset),
            hsv,
        )
        (0..<count).map { it ->
            Color.HSVToColor(
                floatArrayOf(
                    (hsv[0] + unit * it) % HUE_MAX_VALUE,
                    hsv[1],
                    hsv[2],
                )
            )
        }
    }
