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

import androidx.lifecycle.MutableLiveData
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Simple delegate class to [MutableLiveData].
 *
 * So [setValue] here only calls [MutableLiveData.setValue].
 * As [MutableLiveData.postValue], which has to be called manually if you use
 * [MutableLiveDataDelegate], is more common, it is more recommended not to use
 * [MutableLiveDataDelegate], or use
 * [fyi.ioclub.workyras.utils.wrappers.ValueLiveDataWrapper] instead if you still want a wrapper.
 *
 * @see fyi.ioclub.workyras.utils.wrappers.ValueLiveDataWrapper
 *
 */
@JvmInline
value class MutableLiveDataDelegate<T>(private val liveData: MutableLiveData<T>) :
    ReadWriteProperty<Any?, T?> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = liveData.value

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) =
        value.let(liveData::setValue)
}

val <T> MutableLiveData<T>.delegate get() = let(::MutableLiveDataDelegate)

@JvmInline
value class MutableLiveDataDelegateNotNull<T>(private val liveData: MutableLiveData<T>) :
    ReadWriteProperty<Any?, T> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = liveData.value!!

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        value.let(liveData::setValue)
}

val <T> MutableLiveData<T>.delegateNotNull get() = let(::MutableLiveDataDelegateNotNull)
