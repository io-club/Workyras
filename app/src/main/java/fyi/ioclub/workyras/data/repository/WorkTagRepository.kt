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

import android.util.Log
import fyi.ioclub.workyras.data.db.MainDatabase
import fyi.ioclub.workyras.data.db.entities.WorkTagEntity
import fyi.ioclub.workyras.utils.hexEncoded

object WorkTagRepository {

    private var batchMaxSize = 64

    private val dao by lazy { MainDatabase.INSTANCE.workTagDao() }

    val flowAllWorkTags by lazy { dao.flowGetAllWorkTags() }

    private var onDeleteWorkTagsListener: OnDeleteWorkTagsListener? = null

    suspend fun getAllWorkTags() = dao.getAllWorkTags()

    fun setOnDeleteWorkTagsListener(listener: OnDeleteWorkTagsListener?) {
        onDeleteWorkTagsListener = listener
    }

    interface OnDeleteWorkTagsListener {

        fun onDeleteWorkTags(idGlobalList: List<ByteArray>)

        fun onDeleteAllWorkTags()
    }

    internal suspend fun getWorkTagByIdGlobal(idGlobal: ByteArray) =
        dao.getWorkTagByIdGlobal(idGlobal)

    suspend fun insertWorkTag(params: WorkTagInsertionParams) = dao.insertWorkTag(
        params.run {
            WorkTagEntity(
                idGlobal = idGlobal,
                name = name,
                nextId = nextIdGlobal?.let { requireNotNull(getWorkTagByIdGlobal(it)).id },
                isRecycled = false,
            )
        }
    )

    sealed interface WorkTagInsertionParams {

        val idGlobal: ByteArray
        val name: String
        val nextIdGlobal: ByteArray?

        class Impl(
            override val idGlobal: ByteArray,
            override val name: String,
            override val nextIdGlobal: ByteArray?,
        ) : WorkTagInsertionParams
    }

    suspend fun renameWorkTag(idGlobal: ByteArray, name: String) =
        dao.updateWorkTagNameByIdGlobal(idGlobal, name)

    suspend fun linkWorkTagNext(idGlobal: ByteArray?, nextIdGlobal: ByteArray?) =
        idGlobal?.let { bytes ->
            Log.d(
                "Workyras.Tags",
                "Make ${idGlobal.hexEncoded} next to ${nextIdGlobal?.hexEncoded}"
            )
            dao.updateWorkTagNextIdByIdGlobal(
                idGlobal = bytes,
                nextIdGlobal?.let { getWorkTagByIdGlobal(it)!!.id },
            )
        }

    suspend fun setWorkTagRecycled(idGlobal: ByteArray, isRecycled: Boolean) =
        dao.updateWorkTagIsRecycledByIdGlobal(idGlobal, isRecycled)

    suspend fun deleteWorkTagsByIdGlobals(idGlobalList: List<ByteArray>) {
        idGlobalList.run {
            // Inform external listener
            onDeleteWorkTagsListener?.onDeleteWorkTags(this)
            // Delete all work points with the specific work tags
            forEach { WorkPointRepository.deleteWorkPointByWorkTagIdGlobal(it) }
            // Delete the work tags
            chunked(batchMaxSize).forEach { dao.deleteWorkTagByIdGlobalInBatch(it) }
        }
    }

    suspend fun deleteAllWorkTags() {
        // Delete all work points first
        WorkPointRepository.deleteAllWorkPoints()
        with(dao) {
            deleteAllWorkTags()
            deleteFromSqliteSequence()
        }
        onDeleteWorkTagsListener?.onDeleteAllWorkTags()
    }
}
