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

package fyi.ioclub.workyras.ui.analysis

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.ioclub.workyras.data.repository.WorkPointRepository
import fyi.ioclub.workyras.models.DisplayWorkTag
import fyi.ioclub.workyras.ui.common.ToLateInit
import fyi.ioclub.workyras.ui.worktags.WorkTagsViewModel
import fyi.ioclub.workyras.utils.coroutine.delegates.DynamicJobCollection
import fyi.ioclub.workyras.utils.delegates.delegate
import fyi.ioclub.workyras.utils.delegates.getIntPreferenceDelegate
import fyi.ioclub.workyras.utils.delegates.getLongPreferenceDelegate
import fyi.ioclub.workyras.utils.wrappers.TrinityDataWrapper
import fyi.ioclub.workyras.utils.wrappers.ValueLiveDataWrapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private typealias WorkPointLists = List<Pair<DisplayWorkTag, List<Long>>>

class AnalysisViewModel : ViewModel(), ToLateInit<AnalysisViewModel.LateInitParams> {

    override val toLateInit get() = !::_sharedPrefs.isInitialized

    private val _workPointListsLiveData = MediatorLiveData<WorkPointLists>()
    val workPointListsLiveData: LiveData<WorkPointLists> = _workPointListsLiveData
    val workPointLists by workPointListsLiveData.delegate

    val sigmaMin: ValueLiveDataWrapper<Int> get() = _sigmaMin
    private lateinit var _sigmaMin: TrinityDataWrapper<Int>
    val sigmaMax: ValueLiveDataWrapper<Int> get() = _sigmaMax
    private lateinit var _sigmaMax: TrinityDataWrapper<Int>

    val sigma: ValueLiveDataWrapper<Int> get() = _sigma
    private lateinit var _sigma: TrinityDataWrapper<Int>

    val timestampStart get() = _timestampStart as TimestampWrapper.Start
    private lateinit var _timestampStart: TrinityDataWrapper.Nullable<Long?>

    val timestampEnd get() = _timestampEnd as TimestampWrapper.End
    private lateinit var _timestampEnd: TrinityDataWrapper.Nullable<Long?>

    sealed interface TimestampWrapper : ValueLiveDataWrapper<Long?> {

        interface Start : TimestampWrapper
        interface End : TimestampWrapper
    }

    private val workTagsViewModel get() = _workTagsViewModel
    private lateinit var _workTagsViewModel: WorkTagsViewModel

    private val sharedPrefs get() = _sharedPrefs
    private lateinit var _sharedPrefs: SharedPreferences

    private val onSharedPrefChangeListener get() = _onSharedPrefChangeListener
    private lateinit var _onSharedPrefChangeListener: SharedPreferences.OnSharedPreferenceChangeListener

    private var workTagCollectingJobs: Collection<Job> by DynamicJobCollection()

    override fun lateInit(param: LateInitParams) {
        require(toLateInit)

        _workTagsViewModel = param.workTagsViewModel

        with(param.sharedPrefs) {
            _sharedPrefs = this

            with(param.prefKeys) {

                _sigmaMin = TrinityDataWrapper.NonNull(
                    getIntPreferenceDelegate(sigmaMin, DefaultPrefs.SIGMA_MIN)
                ).apply { syncToSrcNow() }
                _sigmaMax = TrinityDataWrapper.NonNull(
                    getIntPreferenceDelegate(sigmaMax, DefaultPrefs.SIGMA_MAX)
                ).apply { syncToSrcNow() }
                _sigma = TrinityDataWrapper.NonNull(getIntPreferenceDelegate(
                    sigma,
                    arrayOf(_sigmaMin, _sigmaMax).map { it.value }.run { sum() / size },
                )
                ).apply { syncToSrcNow() }

                abstract class AbstractTimestampWrapper(key: String, defValue: Long) :
                    TrinityDataWrapper.Nullable<Long?>(object :
                        ReadWriteProperty<Any?, Long?> {

                        private var pref by _sharedPrefs.getLongPreferenceDelegate(key, defValue)

                        override fun getValue(thisRef: Any?, property: KProperty<*>) =
                            if (pref < Long.MAX_VALUE) pref else null

                        override fun setValue(
                            thisRef: Any?, property: KProperty<*>, value: Long?
                        ) = (value ?: Long.MAX_VALUE).let(::pref::set)
                    })

                _timestampStart =
                    object : AbstractTimestampWrapper(
                        pickedTimestampStart,
                        DefaultPrefs.PICKED_TIMESTAMP_START
                    ), TimestampWrapper.Start {}
                _timestampEnd =
                    object : AbstractTimestampWrapper(
                        pickedTimestampEnd,
                        DefaultPrefs.PICKED_TIMESTAMP_END
                    ), TimestampWrapper.End {}

                _onSharedPrefChangeListener =
                    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        when (key) {
                            sigmaMin -> _sigmaMin
                            sigmaMax -> _sigmaMax
                            sigma -> _sigma
                            pickedTimestampStart -> _timestampStart
                            pickedTimestampEnd -> _timestampEnd
                            else -> null
                        }?.syncToSrc()
                    }.also(::registerOnSharedPreferenceChangeListener)
            }
        }

        val observer = Observer<Any?> {
            val workTagList = workTagsViewModel.workTagList
            val taggedWorkPointList = workTagList.map { it to emptyList<Long>() }.toMutableList()
            workTagCollectingJobs = workTagList.mapIndexed { i, workTag ->
                viewModelScope.launch {
//                    Log.i("Workyras.Analysis", "Flow collect data of ${workTag.name}")
                    WorkPointRepository.flowGetWorkPointsByProducedAtRange(
                        workTag.id,
                        startTime = _timestampStart.value,
                        endTime = _timestampEnd.value,
                    ).collect { entityList ->
//                        Log.i("Workyras.Analysis", "Collect data of ${workTag.name}")
//                        Log.i(
//                            "Workyras.Analysis",
//                            "Range ${timestampStart.value} to ${timestampEnd.value}",
//                        )
//                        Log.i("Workyras.Analysis", "Collected ${entityList.size} entities")
                        val (tag, _) = taggedWorkPointList[i]
                        taggedWorkPointList[i] = tag to entityList.map { it.producedAt }
                        _workPointListsLiveData.postValue(taggedWorkPointList)
                    }
                }
            }
        }

        for (it in arrayOf(
            _timestampStart,
            _timestampEnd,
        )) it.syncToSrc()

        for (src in arrayOf(
            workTagsViewModel.workTagListLiveData,
            _timestampStart.liveData,
            _timestampEnd.liveData,
        )) _workPointListsLiveData.addSource(src, observer)
    }

    sealed interface LateInitParams {

        val workTagsViewModel: WorkTagsViewModel

        val sharedPrefs: SharedPreferences

        val prefKeys: PrefKeys

        sealed interface PrefKeys {

            val sigmaMin: String
            val sigmaMax: String
            val sigma: String
            val pickedTimestampStart: String
            val pickedTimestampEnd: String

            class Impl(
                override val sigmaMin: String,
                override val sigmaMax: String,
                override val sigma: String,
                override val pickedTimestampStart: String,
                override val pickedTimestampEnd: String,
            ) : PrefKeys
        }

        class Impl(
            override val workTagsViewModel: WorkTagsViewModel,
            override val sharedPrefs: SharedPreferences,
            override val prefKeys: PrefKeys,
        ) : LateInitParams
    }

    private object DefaultPrefs {

        const val SIGMA_MIN = 1

        /** From sec, to min to hour to day, timed by `2` as max value. */
        const val SIGMA_MAX = (60 * 60 * 24) * 2

        /** Relative mode, `-1` ms to s to min to h to d to week. */
        const val PICKED_TIMESTAMP_START = -1 * (1000L * 60 * 60 * 24 * 7)

        const val PICKED_TIMESTAMP_END = Long.MAX_VALUE
    }

    override fun onCleared() {
        super.onCleared()

        sharedPrefs.unregisterOnSharedPreferenceChangeListener(onSharedPrefChangeListener)
        with(_workPointListsLiveData) {
            removeSource(workTagsViewModel.workTagListLiveData)
            removeSource(_timestampStart.liveData)
            removeSource(_timestampEnd.liveData)
        }
    }
}
