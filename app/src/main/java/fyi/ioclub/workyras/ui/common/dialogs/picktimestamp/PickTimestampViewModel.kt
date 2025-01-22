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

package fyi.ioclub.workyras.ui.common.dialogs.picktimestamp

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import fyi.ioclub.workyras.ui.common.ToLateInit
import fyi.ioclub.workyras.utils.delegates.getBooleanPreferenceDelegate
import fyi.ioclub.workyras.utils.wrappers.TrinityDataWrapper
import fyi.ioclub.workyras.utils.wrappers.ValueLiveDataWrapper

class PickTimestampViewModel : ViewModel(), ToLateInit<PickTimestampViewModel.LateInitParams> {

    override val toLateInit get() = !::_sharedPrefs.isInitialized

    val isToShowTimestampRef: ValueLiveDataWrapper<Boolean> get() = _isToShowTimestampRef
    private lateinit var _isToShowTimestampRef: TrinityDataWrapper<Boolean>

    private val sharedPrefs get() = _sharedPrefs
    private lateinit var _sharedPrefs: SharedPreferences

    private val onSharedPrefChangeListener get() = _onSharedPrefChangeListener
    private lateinit var _onSharedPrefChangeListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun lateInit(param: LateInitParams) {
        require(toLateInit)

        with(param.sharedPrefs) {
            _sharedPrefs = this

            with(param.prefKeys) {

                _isToShowTimestampRef = TrinityDataWrapper.NonNull(
                    getBooleanPreferenceDelegate(
                        isToShowTimestampRef,
                        DefaultPrefs.IS_TO_SHOW_TIMESTAMP_REF,
                    )
                ).apply { syncToSrc() }

                _onSharedPrefChangeListener =
                    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        when (key) {
                            isToShowTimestampRef -> _isToShowTimestampRef
                            else -> null
                        }?.syncToSrc()
                    }.also(::registerOnSharedPreferenceChangeListener)
            }
        }
    }

    sealed interface LateInitParams {

        val sharedPrefs: SharedPreferences

        val prefKeys: PrefKeys

        sealed interface PrefKeys {

            val isToShowTimestampRef: String

            class Impl(
                override val isToShowTimestampRef: String,
            ) : PrefKeys
        }

        class Impl(
            override val sharedPrefs: SharedPreferences,
            override val prefKeys: PrefKeys,
        ) : LateInitParams
    }

    private object DefaultPrefs {

        const val IS_TO_SHOW_TIMESTAMP_REF = true
    }

    override fun onCleared() {
        super.onCleared()

        sharedPrefs.unregisterOnSharedPreferenceChangeListener(onSharedPrefChangeListener)
    }
}
