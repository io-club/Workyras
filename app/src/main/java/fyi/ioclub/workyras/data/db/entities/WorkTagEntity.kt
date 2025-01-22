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

package fyi.ioclub.workyras.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_tags",
    indices = [
        Index(value = ["id_global"], unique = true),
    ],
)
data class WorkTagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "id_global") val idGlobal: ByteArray,
    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "is_recycled") val isRecycled: Boolean,
    @ColumnInfo(name = "next_id") val nextId: Long?,
) {

    override fun equals(other: Any?) =
        this === other || javaClass == other?.javaClass && id == (other as WorkTagEntity).id

    override fun hashCode() = id.hashCode()
}
