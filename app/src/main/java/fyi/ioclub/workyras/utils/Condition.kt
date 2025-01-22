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

import kotlin.reflect.KProperty0

inline fun runIf(flag: Boolean, block: () -> Unit) = if (flag) block() else Unit

inline fun runIf(lazyFlag: () -> Boolean, block: () -> Unit) = runIf(lazyFlag(), block)

inline fun runIf(flagProperty: KProperty0<Boolean>, block: () -> Unit) =
    runIf(flagProperty.get(), block)

inline fun runIfNot(flag: Boolean, block: () -> Unit) = runIf(!flag, block)

inline fun runIfNot(lazyFlag: () -> Boolean, block: () -> Unit) = runIfNot(lazyFlag(), block)

inline fun runIfNot(flagProperty: KProperty0<Boolean>, block: () -> Unit) =
    runIfNot(flagProperty.get(), block)
