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

package fyi.ioclub.workyras.data.csv.bean.converters

import com.opencsv.bean.AbstractBeanField
import org.apache.commons.lang3.StringUtils

@Suppress("UNCHECKED_CAST")
abstract class AbstractNullableConverter<T>(
    private val innerConverter: (String) -> T,
    private val innerConverterToWrite: (T) -> String
) : AbstractBeanField<Any, Any>() {

    override fun convert(value: String?): T? =
        if (value.isNullOrEmpty()) null else innerConverter(value)

    override fun convertToWrite(value: Any?): String =
        (value as T?)?.let(innerConverterToWrite) ?: StringUtils.EMPTY
}

class NullableStringConverter :
    AbstractNullableConverter<String>(String::toString, String::toString)

class NullableLongConverter : AbstractNullableConverter<Long>(String::toLong, Long::toString)
