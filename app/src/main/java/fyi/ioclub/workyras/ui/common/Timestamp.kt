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

package fyi.ioclub.workyras.ui.common

import android.content.Context
import fyi.ioclub.workyras.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.exp

object Timestamp : ToLateInit<Context> {

    override val toLateInit get() = !::_dateTimeFormatter.isInitialized

    val dateTimeFormatter get() = _dateTimeFormatter
    private lateinit var _dateTimeFormatter: DateTimeFormatter

    fun getLocalDateTimeOfTimestamp(fixedTimestamp: Long): LocalDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(fixedTimestamp),
        ZoneId.systemDefault(),
    )

    fun getLocalDateTimeOfTimestamp(
        timestamp: Long?,
        ref: Long,
    ) = getLocalDateTimeOfTimestamp(toFixedTimestamp(timestamp, ref))

    val LocalDateTime.fixedTimestamp
        get() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun toRelativeTimestamp(timestamp: Long?, ref: Long) =
        if (timestamp == null || timestamp < 0) timestamp else (timestamp - ref).let { if (it == 0L) null else it }

    fun toFixedTimestamp(timestamp: Long?, ref: Long) =
        timestamp?.let { if (it < 0) ref + it else it } ?: ref

    override fun lateInit(param: Context) {
        with(param) {
            _dateTimeFormatter = DateTimeFormatter.ofPattern(getString(R.string.datetime_format))
        }
    }
}

internal val Long.dividedBy1k get() = this / 1_000F
internal val Float.multiplyBy1k get() = (this * 1_000).toLong()

/** Function to calculate the weighted frequencies. */
internal fun calculateTimestampFrequencies(timestamps: List<Float>, sigma: Number): List<Float> {
    return timestamps.map {
        // For each timestamp, calculate its frequency
        timestamps.sumOf { other ->
            val diff = abs(it - other).toDouble() // Difference in minutes
            exp(-diff / sigma.toFloat())  // Gaussian weight
        }.toFloat()
    }
}
