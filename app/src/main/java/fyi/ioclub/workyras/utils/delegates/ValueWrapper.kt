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

package fyi.ioclub.workyras.utils.delegates

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

interface ReadOnlyValueWrapper<V> {

    val value: V

    open class ByDelegate<V>(delegate: ReadOnlyProperty<Any?, V>) : ReadOnlyValueWrapper<V> {
        override val value by delegate
    }
}

val <V> ReadOnlyProperty<Any?, V>.valueWrapper get() = ReadOnlyValueWrapper.ByDelegate(this)

interface ReadWriteValueWrapper<V> : ReadOnlyValueWrapper<V> {

    override var value: V

    open class ByDelegate<V>(delegate: ReadWriteProperty<Any?, V>) : ReadWriteValueWrapper<V> {
        override var value by delegate
    }
}

val <V> ReadWriteProperty<Any?, V>.valueWrapper get() = ReadWriteValueWrapper.ByDelegate(this)
