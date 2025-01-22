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

package fyi.ioclub.workyras.utils.wrappers

import androidx.lifecycle.MutableLiveData
import fyi.ioclub.workyras.utils.runIfNot
import fyi.ioclub.workyras.utils.runQuietly
import kotlin.properties.ReadWriteProperty

typealias SrcDelegate<T> = ReadWriteProperty<TrinityDataWrapper<T>, T>

/** You may manually check the equality of [value] in `srcDelegate`. */
sealed class TrinityDataWrapper<T>(
    srcDelegate: SrcDelegate<T>,
    liveData: MutableLiveData<T> = MutableLiveData(),
) : ValueLiveDataWrapper.Internal.ImplBase<T>(liveData) {

    override var value
        get() = super.value
        set(it) = runQuietly(::isQuietToSrc) {
            super.value = it
            src = it
        }

    protected open var isQuietToSrc = false
    protected open var src by srcDelegate

    open fun syncToSrc() = runIfNot(::isQuietToSrc) { value = src }

    open fun syncToSrcNow() = runIfNot(::isQuietToSrc) { liveData.value = src }

    open class NonNull<T : Any>(
        srcDelegate: SrcDelegate<T>,
        liveData: MutableLiveData<T> = MutableLiveData(),
    ) : TrinityDataWrapper<T>(srcDelegate, liveData),
        ValueLiveDataWrapper.Internal.NonNull<T>

    open class Nullable<T>(
        srcDelegate: SrcDelegate<T?>,
        liveData: MutableLiveData<T?> = MutableLiveData(),
    ) : TrinityDataWrapper<T?>(srcDelegate, liveData),
        ValueLiveDataWrapper.Internal.Nullable<T> {

        override var value
            get() = super<ValueLiveDataWrapper.Internal.Nullable>.value
            set(it) {
                super<TrinityDataWrapper>.value = it
            }
    }
}
