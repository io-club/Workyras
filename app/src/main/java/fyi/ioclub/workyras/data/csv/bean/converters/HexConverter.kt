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
import com.opencsv.exceptions.CsvDataTypeMismatchException
import fyi.ioclub.workyras.utils.hexDecoded
import fyi.ioclub.workyras.utils.hexEncoded

class HexConverter : AbstractBeanField<Any, Any>() {

    override fun convert(value: String?): ByteArray =
        try {
            value?.hexDecoded ?: throw IllegalArgumentException()
        } catch (e: IllegalArgumentException) {
            throw CsvDataTypeMismatchException("$value is not a hex string")
        }

    override fun convertToWrite(value: Any?): String = (value as ByteArray).hexEncoded
}
