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

package fyi.ioclub.workyras.ui.work

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.ioclub.workyras.data.repository.WorkPointRepository
import fyi.ioclub.workyras.data.repository.WorkTagRepository
import fyi.ioclub.workyras.ui.common.ToLateInit
import fyi.ioclub.workyras.utils.coroutine.delegates.DynamicJob
import fyi.ioclub.workyras.utils.delegates.getLongPreferenceDelegate
import fyi.ioclub.workyras.utils.delegates.getStringPreferenceDelegate
import fyi.ioclub.workyras.utils.hexDecoded
import fyi.ioclub.workyras.utils.hexEncoded
import fyi.ioclub.workyras.utils.wrappers.TrinityDataWrapper
import fyi.ioclub.workyras.utils.wrappers.ValueLiveDataWrapper
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class WorkViewModel : ViewModel(), ToLateInit<WorkViewModel.LateInitParams> {

    override val toLateInit get() = !::_sharedPrefs.isInitialized

    private val _workPtLiveData = MutableLiveData<Int>()
    val workPtLiveData: LiveData<Int> = _workPtLiveData

    val workTagId: ValueLiveDataWrapper<ByteArray?> get() = _workTagId
    private lateinit var _workTagId: TrinityDataWrapper.Nullable<ByteArray?>

    val timestampStart get() = _timestampStart as ValueLiveDataWrapper<Long?>
    private lateinit var _timestampStart: TrinityDataWrapper.Nullable<Long?>

    private val sharedPrefs get() = _sharedPrefs
    private lateinit var _sharedPrefs: SharedPreferences

    private val onSharedPrefChangeListener get() = _onSharedPrefChangeListener
    private lateinit var _onSharedPrefChangeListener: SharedPreferences.OnSharedPreferenceChangeListener

    private var workPtCollectingJob by DynamicJob()

    /** Invoke by fragment's trial. */
    override fun lateInit(param: LateInitParams) {
        require(toLateInit)

        with(param.sharedPrefs) {
            _sharedPrefs = this
            with(param.prefKeys) {

                _workTagId =
                    TrinityDataWrapper.Nullable(object : ReadWriteProperty<Any?, ByteArray?> {

                        private var pref by getStringPreferenceDelegate(
                            selectedWorkTagIdGlobal,
                            null,
                        )

                        override fun getValue(thisRef: Any?, property: KProperty<*>) =
                            pref?.hexDecoded

                        override fun setValue(
                            thisRef: Any?,
                            property: KProperty<*>,
                            value: ByteArray?
                        ) = value?.hexEncoded.let(::pref::set)
                    })

                _timestampStart = TrinityDataWrapper.Nullable(object :
                    ReadWriteProperty<Any?, Long?> {

                    private var pref by _sharedPrefs.getLongPreferenceDelegate(
                        pickedTimestampStart,
                        DefaultPrefs.PICKED_TIMESTAMP_START,
                    )

                    override fun getValue(thisRef: Any?, property: KProperty<*>) =
                        if (pref < Long.MAX_VALUE) pref else null

                    override fun setValue(
                        thisRef: Any?, property: KProperty<*>, value: Long?
                    ) = (value ?: Long.MAX_VALUE).let(::pref::set)
                })
            }
            WorkTagRepository.setOnDeleteWorkTagsListener(object :
                WorkTagRepository.OnDeleteWorkTagsListener {

                override fun onDeleteWorkTags(idGlobalList: List<ByteArray>) =
                    with(workTagId) {
                        value?.run { if (idGlobalList.any(::contentEquals)) value = null }
                        Unit
                    }

                override fun onDeleteAllWorkTags() = null.let(workTagId::value::set)
            })
            _onSharedPrefChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    with(param.prefKeys) {
                        when (key) {
                            selectedWorkTagIdGlobal -> _workTagId
                            pickedTimestampStart -> _timestampStart
                            else -> null
                        }?.syncToSrc()
                    }
                }.also(::registerOnSharedPreferenceChangeListener)
        }
        arrayOf(
            _workTagId,
            _timestampStart,
        ).forEach {
            with(it) {
                liveData.observeForever { updateWorkPtCollectingJob() }
                syncToSrc()
            }
        }
    }

    sealed interface LateInitParams {

        val sharedPrefs: SharedPreferences

        val prefKeys: PrefKeys

        sealed interface PrefKeys {

            val selectedWorkTagIdGlobal: String
            val pickedTimestampStart: String

            class Impl(
                override val selectedWorkTagIdGlobal: String,
                override val pickedTimestampStart: String,
            ) : PrefKeys
        }

        class Impl(
            override val sharedPrefs: SharedPreferences,
            override val prefKeys: PrefKeys,
        ) : LateInitParams
    }

    private object DefaultPrefs {

        /** Relative mode, `-1` ms to s to min to h to d to week. */
        const val PICKED_TIMESTAMP_START = -1 * (1000L * 60 * 60 * 24 * 7)
    }

    private fun updateWorkPtCollectingJob() = viewModelScope.launch {
        val post = _workPtLiveData::postValue
        val id = workTagId.value ?: return@launch NO_WORK_PT.let(post)
        Log.i("Workyras.Work", "Collect Pt from ${id.hexEncoded}")
        WorkPointRepository.flowCountWorkPointsByProducedAtRange(id, timestampStart.value)
            .collect(_workPtLiveData::postValue)
    }.let(::workPtCollectingJob::set)

    /** @throws IllegalStateException when [workTagId] is `null`.*/
    fun addWorkPt(comment: String? = null) {
        val workTagId = workTagId.value ?: throw IllegalStateException("No work tag selected.")
        _workPtLiveData.postValue(workPtLiveData.value!! + 1)
        viewModelScope.launch {
            WorkPointRepository.insertWorkPoint(
                WorkPointRepository.WorkPointInsertionParams.Impl(
                    workTagIdGlobal = workTagId,
                    comment = comment,
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()

        sharedPrefs.unregisterOnSharedPreferenceChangeListener(onSharedPrefChangeListener)
    }

    companion object {

        const val NO_WORK_PT = 0
    }
}