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

import android.view.View
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@JvmInline
value class ViewTogglingDelegate(private val view: View) : ReadWriteProperty<Any?, Boolean> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) = when (view.visibility) {
        View.GONE -> false
        View.VISIBLE -> true
        else -> throw IllegalStateException("$view is not either GONE nor VISIBLE")
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        view.visibility = if (value) View.VISIBLE else View.GONE
    }
}

val View.togglingDelegate get() = let(::ViewTogglingDelegate)
