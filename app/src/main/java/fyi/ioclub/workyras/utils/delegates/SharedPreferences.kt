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

import android.content.SharedPreferences
import fyi.ioclub.workyras.utils.runIf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private inline fun <T> SharedPreferences.baseGet(
    key: String,
    defValue: T,
    getter: SharedPreferences.(String, T) -> T,
) = getter(key, defValue)

private inline fun <T> SharedPreferences.basePut(
    key: String,
    value: T,
    putter: SharedPreferences.Editor.(String, T) -> Unit,
) =
    with(edit()) {
        putter(key, value)
        apply()
    }

sealed class BasePreferenceDelegate<T>(
    protected val sharedPref: SharedPreferences,
    protected val key: String,
    protected val defValue: T,
    private val getter: SharedPreferences.(String, T) -> T,
    private val putter: SharedPreferences.Editor.(String, T) -> Unit,
) : ReadWriteProperty<Any?, T> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        sharedPref.baseGet(key, defValue, getter)

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        runIf(value != getValue(thisRef, property)) { sharedPref.basePut(key, value, putter) }
}

open class StringPreferenceDelegate(
    sharedPref: SharedPreferences, key: String, defValue: String?
) : BasePreferenceDelegate<String?>(
    sharedPref, key, defValue,
    SharedPreferences::getString,
    SharedPreferences.Editor::putString,
)

fun SharedPreferences.getStringPreferenceDelegate(key: String, defValue: String?) =
    StringPreferenceDelegate(this, key, defValue)

open class FloatPreferenceDelegate(
    sharedPref: SharedPreferences, key: String, defValue: Float
) : BasePreferenceDelegate<Float>(
    sharedPref, key, defValue,
    SharedPreferences::getFloat,
    SharedPreferences.Editor::putFloat,
)

fun SharedPreferences.getFloatPreferenceDelegate(key: String, defValue: Float) =
    FloatPreferenceDelegate(this, key, defValue)

open class LongPreferenceDelegate(
    sharedPref: SharedPreferences, key: String, defValue: Long
) : BasePreferenceDelegate<Long>(
    sharedPref, key, defValue,
    SharedPreferences::getLong,
    SharedPreferences.Editor::putLong,
)

fun SharedPreferences.getLongPreferenceDelegate(key: String, defValue: Long) =
    LongPreferenceDelegate(this, key, defValue)

open class IntPreferenceDelegate(
    sharedPref: SharedPreferences, key: String, defValue: Int
) : BasePreferenceDelegate<Int>(
    sharedPref, key, defValue,
    SharedPreferences::getInt,
    SharedPreferences.Editor::putInt,
)

fun SharedPreferences.getIntPreferenceDelegate(key: String, defValue: Int) =
    IntPreferenceDelegate(this, key, defValue)

open class BooleanPreferenceDelegate(
    sharedPref: SharedPreferences, key: String, defValue: Boolean
) : BasePreferenceDelegate<Boolean>(
    sharedPref, key, defValue,
    SharedPreferences::getBoolean,
    SharedPreferences.Editor::putBoolean,
)

fun SharedPreferences.getBooleanPreferenceDelegate(key: String, defValue: Boolean) =
    BooleanPreferenceDelegate(this, key, defValue)

// Preference values

class StringPreference(delegate: StringPreferenceDelegate) :
    ReadWriteValueWrapper.ByDelegate<String?>(delegate)

fun SharedPreferences.getStringPreference(key: String, defValue: String?) =
    StringPreference(getStringPreferenceDelegate(key, defValue))

class LongPreference(delegate: LongPreferenceDelegate) :
    ReadWriteValueWrapper.ByDelegate<Long>(delegate)

class FloatPreference(delegate: FloatPreferenceDelegate) :
    ReadWriteValueWrapper.ByDelegate<Float>(delegate)

fun SharedPreferences.getFloatPreference(key: String, defValue: Float) =
    FloatPreference(getFloatPreferenceDelegate(key, defValue))

fun SharedPreferences.getLongPreference(key: String, defValue: Long) =
    LongPreference(getLongPreferenceDelegate(key, defValue))

class IntPreference(delegate: IntPreferenceDelegate) :
    ReadWriteValueWrapper.ByDelegate<Int>(delegate)

fun SharedPreferences.getIntPreferencePreference(key: String, defValue: Int) =
    IntPreference(getIntPreferenceDelegate(key, defValue))

class BooleanPreference(delegate: BooleanPreferenceDelegate) :
    ReadWriteValueWrapper.ByDelegate<Boolean>(delegate)

fun SharedPreferences.getBooleanPreferencePreference(key: String, defValue: Boolean) =
    BooleanPreference(getBooleanPreferenceDelegate(key, defValue))
