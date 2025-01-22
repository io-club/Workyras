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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Interface exposed to outer.
 *
 * Always implement sub-interfaces of [ValueLiveDataWrapper.Internal] instead,
 * since default getter of [value] cannot be here without the useless default setter.
 *
 * We do not provide a similar class with immutable [value] because it will be useless.
 *
 */
interface ValueLiveDataWrapper<T> {

    var value: T

    val liveData: LiveData<T>

    sealed interface Internal<T> : ValueLiveDataWrapper<T> {

        override var value: T
            get() = liveData.value!!
            set(it) = liveData.postValue(it)

        override val liveData: MutableLiveData<T>

        sealed class ImplBase<T>(
            override val liveData: MutableLiveData<T> = MutableLiveData(),
        ) : Internal<T>

        interface NonNull<T : Any> : Internal<T> {

            open class Impl<T : Any>(
                liveData: MutableLiveData<T> = MutableLiveData(),
            ) : ImplBase<T>(liveData), NonNull<T>
        }

        interface Nullable<T> : Internal<T?> {

            override var value: T?
                get() = liveData.value
                set(value) {
                    super.value = value
                }

            open class Impl<T>(
                liveData: MutableLiveData<T?> = MutableLiveData(),
            ) : ImplBase<T?>(liveData), Nullable<T?>
        }
    }
}
