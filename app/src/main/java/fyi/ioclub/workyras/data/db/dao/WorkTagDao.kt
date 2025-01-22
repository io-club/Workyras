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
import fyi.ioclub.workyras.data.db.entities.WorkTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTagDao {

    @Query("SELECT * FROM work_tags")
    suspend fun getAllWorkTags(): List<WorkTagEntity>

    @Query("SELECT * FROM work_tags")
    fun flowGetAllWorkTags(): Flow<List<WorkTagEntity>>

    @Query("SELECT * FROM work_tags WHERE id_global = :idGlobal LIMIT 1")
    suspend fun getWorkTagByIdGlobal(idGlobal: ByteArray): WorkTagEntity?

    @Insert
    suspend fun insertWorkTag(workTag: WorkTagEntity)

    @Query("UPDATE work_tags SET name = :name WHERE id_global = :idGlobal")
    suspend fun updateWorkTagNameByIdGlobal(idGlobal: ByteArray, name: String)

    @Query("UPDATE work_tags SET next_id = :nextId WHERE id_global = :idGlobal")
    suspend fun updateWorkTagNextIdByIdGlobal(idGlobal: ByteArray, nextId: Long?)

    @Query("UPDATE work_tags SET is_recycled = :isRecycled WHERE id_global = :idGlobal")
    suspend fun updateWorkTagIsRecycledByIdGlobal(idGlobal: ByteArray, isRecycled: Boolean)

    @Query("DELETE FROM work_tags WHERE id_global IN (:idGlobalBatch)")
    suspend fun deleteWorkTagByIdGlobalInBatch(idGlobalBatch: List<ByteArray>)

    @Query("DELETE FROM work_tags")
    suspend fun deleteAllWorkTags()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'work_tags'")
    suspend fun deleteFromSqliteSequence()

    // Methods currently for only debugging

    @Query("UPDATE work_tags SET next_id= :nextId WHERE id = :id")
    suspend fun updateWorkTagNextIdById(id: Long, nextId: Long?)

    @Query("DELETE FROM work_tags WHERE id = :id")
    suspend fun deleteWorkTagById(id: Long)
}
