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

import kotlin.reflect.KMutableProperty0

inline fun <R> runQuietly(isQuietSetter: (Boolean) -> Unit, block: () -> R): R {
    isQuietSetter(true)
    val rsl = block()
    isQuietSetter(false)
    return rsl
}

inline fun <R> runQuietly(isQuietProperty: KMutableProperty0<Boolean>, block: () -> R) =
    runQuietly(isQuietProperty::set, block)
