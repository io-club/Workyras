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
import fyi.ioclub.workyras.data.csv.bean.converters.NullableStringConverter
import fyi.ioclub.workyras.data.db.entities.WorkPointEntity

class WorkPointBean private constructor(

    @CsvBindByName(column = "work_tag_id", required = true)
    private val _workTagId: Long?,

    @CsvCustomBindByName(column = "comment", converter = NullableStringConverter::class)
    val comment: String? = null,

    @CsvBindByName(column = "produced_at", required = true)
    private val _producedAt: Long?,
) {

    /** Used by `OpenCSV`. */
    constructor() : this(
        _workTagId = null,
        _producedAt = null,
    )

    val workTagId get() = _workTagId!!

    /** [comment] is nullable. */

    val producedAt get() = _producedAt!!

    companion object {

        /** To ignore work pt of tag ID not found in work tag table, return `null`. */
        fun abbreviatedFromEntity(
            entity: WorkPointEntity, idAbbreviatingMap: Map<Long, Long>,
        ): WorkPointBean? = entity.run {
            WorkPointBean(
                _workTagId = idAbbreviatingMap[workTagId] ?: return null,
                comment = comment,
                _producedAt = producedAt,
            )
        }
    }
}
