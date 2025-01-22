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

package fyi.ioclub.workyras.data.csv.bean

import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvCustomBindByName
import fyi.ioclub.workyras.data.csv.bean.converters.HexConverter
import fyi.ioclub.workyras.data.csv.bean.converters.NullableLongConverter
import fyi.ioclub.workyras.data.db.entities.WorkTagEntity

class WorkTagBean(

    @CsvBindByName(column = "id", required = true)
    private val _id: Long?,

    @CsvCustomBindByName(column = "id_global", converter = HexConverter::class, required = true)
    private val _idGlobal: ByteArray?,

    @CsvBindByName(column = "name", required = true)
    private val _name: String?,

    @CsvBindByName(column = "is_recycled")
    val isRecycled: Boolean = false,

    @CsvCustomBindByName(column = "next_id", converter = NullableLongConverter::class)
    val nextId: Long? = null
) {

    /** Used by `OpenCSV`. */
    constructor() : this(
        _id = null,
        _idGlobal = null,
        _name = null,
    )

    val id get() = _id!!

    val idGlobal get() = _idGlobal!!

    val name get() = _name!!

    /** [isRecycled] has non-null default value. */

    /** [nextId] is nullable. */

    companion object {
        fun abbreviatedFromEntity(
            entity: WorkTagEntity, idAbbreviatingMap: Map<Long, Long>,
        ): WorkTagBean = entity.run {
            WorkTagBean(
                _id = idAbbreviatingMap.getValue(id),
                _idGlobal = idGlobal,
                _name = name,
                isRecycled = isRecycled,
                nextId = nextId?.let(idAbbreviatingMap::getValue),
            )
        }
    }
}
