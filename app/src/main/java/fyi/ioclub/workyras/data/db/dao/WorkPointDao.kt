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

package fyi.ioclub.workyras.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import fyi.ioclub.workyras.data.db.entities.WorkPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkPointDao {

    @Query("SELECT COUNT(*) FROM work_points WHERE work_tag_id = :workTagId")
    fun flowCountWorkPointsByWorkTagIdAndProducedAtRange(workTagId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM work_points WHERE produced_at >= :startTime AND work_tag_id = :workTagId")
    suspend fun countWorkPointsByWorkTagIdAndProducedAtRange(
        workTagId: Long,
        startTime: Long,
    ): Int

    @Query("SELECT COUNT(*) FROM work_points WHERE produced_at >= :startTime AND work_tag_id = :workTagId")
    fun flowCountWorkPointsByWorkTagIdAndProducedAtRange(
        workTagId: Long,
        startTime: Long,
    ): Flow<Int>

    @Query("SELECT COUNT(*) FROM work_points WHERE (produced_at BETWEEN :startTime AND :endTime) AND work_tag_id = :workTagId")
    suspend fun countWorkPointsByWorkTagIdAndProducedAtRange(
        workTagId: Long,
        startTime: Long,
        endTime: Long,
    ): Int

    @Query("SELECT COUNT(*) FROM work_points WHERE (produced_at BETWEEN :startTime AND :endTime) AND work_tag_id = :workTagId")
    fun flowCountWorkPointsByWorkTagIdAndProducedAtRange(
        workTagId: Long,
        startTime: Long,
        endTime: Long,
    ): Flow<Int>

    @Query("SELECT * FROM work_points")
    suspend fun getAllWorkPoints(): List<WorkPointEntity>

    @Query("SELECT * FROM work_points WHERE work_tag_id = :workTagId")
    fun flowGetWorkPointsByWorkTagIdAndProducedAtRange(workTagId: Long): Flow<List<WorkPointEntity>>

    @Query("SELECT * FROM work_points WHERE produced_at >= :startTime AND work_tag_id = :workTagId")
    suspend fun getWorkPointsByWorkTagIdAndProducedAtRange(
        workTagId: Long,
        startTime: Long,
    ): List<WorkPointEntity>

    @Query("SELECT * FROM work_points WHERE produced_at >= :startTime AND work_tag_id = :workTagId")
    fun flowGetWorkPointsByWorkTagIdAndProducedAtRange(
        workTagId: Long,
        startTime: Long,
    ): Flow<List<WorkPointEntity>>

    @Query("SELECT * FROM work_points WHERE (produced_at BETWEEN :startTime AND :endTime) AND work_tag_id = :workTagId")
    suspend fun getWorkPointsByWorkTagIdAndProducedAtRange(
        workTagId: Long,
        startTime: Long,
        endTime: Long,
    ): List<WorkPointEntity>

    @Query("SELECT * FROM work_points WHERE (produced_at BETWEEN :startTime AND :endTime) AND work_tag_id = :workTagId")
    fun flowGetWorkPointsByWorkTagIdAndProducedAtRange(
        workTagId: Long,
        startTime: Long,
        endTime: Long,
    ): Flow<List<WorkPointEntity>>

    @Insert
    suspend fun insertWorkPoint(workPoint: WorkPointEntity)

    @Query("DELETE FROM work_points WHERE work_tag_id = :workTagId")
    suspend fun deleteWorkPointsByWorkTagId(workTagId: Long)

    @Query("DELETE FROM work_points")
    suspend fun deleteAllWorkPoints()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'work_points'")
    suspend fun deleteFromSqliteSequence()
}
