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

package fyi.ioclub.workyras.data.repository

import android.content.SharedPreferences
import fyi.ioclub.workyras.data.db.MainDatabase
import fyi.ioclub.workyras.data.db.dao.WorkPointDao
import fyi.ioclub.workyras.data.db.entities.WorkPointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow

object WorkPointRepository {

    private val dao by lazy { MainDatabase.INSTANCE.workPointDao() }

    private val sharedPref get() = _sharedPref
    private lateinit var _sharedPref: SharedPreferences

    val flowSelectedWorkTagIdGlobal get() = _flowSelectedWorkTagIdGlobal
    private lateinit var _flowSelectedWorkTagIdGlobal: Flow<ByteArray?>

    private val _flowTaggedRowsChanged = MutableSharedFlow<Long>()
    private val flowTaggedRowsChanged: SharedFlow<Long> = _flowTaggedRowsChanged

    suspend fun getAllWorkPoints() = dao.getAllWorkPoints()

    suspend fun flowCountWorkPointsByProducedAtRange(workTagIdGlobal: ByteArray) =
        dao.flowCountWorkPointsByWorkTagIdAndProducedAtRange(
            WorkTagRepository.getWorkTagByIdGlobal(workTagIdGlobal)!!.id
        )

    suspend fun flowCountWorkPointsByProducedAtRange(
        workTagIdGlobal: ByteArray,
        startTime: Long?,
        endTime: Long? = null
    ): Flow<Int> = dynamicTimestampFlow(
        workTagIdGlobal, startTime, endTime,

        fetchNone = { 0 },
        fetchNoEndTime = WorkPointDao::countWorkPointsByWorkTagIdAndProducedAtRange,
        flowFetchNoEndTime = WorkPointDao::flowCountWorkPointsByWorkTagIdAndProducedAtRange,
        fetch = WorkPointDao::countWorkPointsByWorkTagIdAndProducedAtRange,
        flowFetch = WorkPointDao::flowCountWorkPointsByWorkTagIdAndProducedAtRange,
    )

    suspend fun flowGetWorkPointsByProducedAtRange(
        workTagIdGlobal: ByteArray,
        startTime: Long?,
        endTime: Long? = null,
    ): Flow<List<WorkPointEntity>> = dynamicTimestampFlow(
        workTagIdGlobal, startTime, endTime,

        fetchNone = ::listOf,
        fetchNoEndTime = WorkPointDao::getWorkPointsByWorkTagIdAndProducedAtRange,
        flowFetchNoEndTime = WorkPointDao::flowGetWorkPointsByWorkTagIdAndProducedAtRange,
        fetch = WorkPointDao::getWorkPointsByWorkTagIdAndProducedAtRange,
        flowFetch = WorkPointDao::flowGetWorkPointsByWorkTagIdAndProducedAtRange,
    )

    private suspend inline fun <T> dynamicTimestampFlow(
        workTagIdGlobal: ByteArray,
        startTime: Long?,
        endTime: Long?,

        crossinline fetchNone: () -> T,
        crossinline fetchNoEndTime: suspend WorkPointDao.(Long, Long) -> T,
        flowFetchNoEndTime: WorkPointDao.(Long, Long) -> Flow<T>,
        crossinline fetch: suspend WorkPointDao.(Long, Long, Long) -> T,
        flowFetch: WorkPointDao.(Long, Long, Long) -> Flow<T>,
    ): Flow<T> {

        startTime ?: return flow { emit(fetchNone()) }

        val workTagId = WorkTagRepository.getWorkTagByIdGlobal(workTagIdGlobal)!!.id

        return if (startTime >= 0) when {

            endTime == null -> dao.flowFetchNoEndTime(workTagId, startTime)

            endTime >= 0 -> dao.flowFetch(workTagId, startTime, endTime)

            else /* endTime < 0 */ -> flow {
                FlowCollector<Long> {
                    emit(dao.fetch(workTagId, startTime, System.currentTimeMillis() + endTime))
                }.run {
                    emit(workTagId)
                    flowTaggedRowsChanged.filter(workTagId::equals).collect(this)
                }
            }
        } else /* startTime < 0 */ flow {
            FlowCollector<Long> {
                val curr = System.currentTimeMillis()
                val queryStartTime = curr + startTime
                emit(
                    dao.run {
                        endTime?.run {
                            fetch(
                                workTagId,
                                queryStartTime,
                                if (endTime >= 0) endTime else curr + endTime,
                            )
                        } ?: fetchNoEndTime(workTagId, queryStartTime)
                    }
                )
            }.run {
                emit(workTagId)
                flowTaggedRowsChanged.filter(workTagId::equals).collect(this)
            }
        }
    }

    suspend fun insertWorkPoint(params: WorkPointInsertionParams) = params.run {
        val workTagId = requireNotNull(WorkTagRepository.getWorkTagByIdGlobal(workTagIdGlobal)).id
        dao.insertWorkPoint(
            WorkPointEntity(
                workTagId = workTagId,
                comment = if (comment.isNullOrEmpty()) null else comment,
                producedAt = producedAt,
            )
        )
        _flowTaggedRowsChanged.emit(workTagId)
    }

    sealed interface WorkPointInsertionParams {

        val workTagIdGlobal: ByteArray

        val comment: String?

        /** Timestamp in milliseconds. */
        val producedAt: Long

        class Impl(
            override val workTagIdGlobal: ByteArray,
            override val comment: String? = null,
            override val producedAt: Long = System.currentTimeMillis(),
        ) : WorkPointInsertionParams
    }

    suspend fun deleteWorkPointByWorkTagIdGlobal(workTagIdGlobal: ByteArray) {
        WorkTagRepository.getWorkTagByIdGlobal(workTagIdGlobal)?.let {
            val workTagId = it.id
            dao.deleteWorkPointsByWorkTagId(workTagId)
            _flowTaggedRowsChanged.emit(workTagId)
        }
    }

    suspend fun deleteAllWorkPoints() {
        with(dao) {
            deleteAllWorkPoints()
            deleteFromSqliteSequence()
        }
    }
}
