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

package fyi.ioclub.workyras.ui.worktags

import android.util.Log
import androidx.lifecycle.viewModelScope
import fyi.ioclub.workyras.data.repository.WorkTagRepository
import fyi.ioclub.workyras.models.WorkTag
import fyi.ioclub.workyras.ui.common.worktagsbase.WorkTagsViewModelBase
import kotlinx.coroutines.launch

class WorkTagsViewModel : WorkTagsViewModelBase() {

    override val isRecycling = false

    // Modifying methods

    /** Insert the tag at index of [refIndex] if [refIndex] >= 0 otherwise size + [refIndex] + 1.
     *
     * @return the index where tag inserted.
     */
    fun insertWorkTag(refIndex: Int, workTagFactoryParams: WorkTagFactoryParams): Int {
        val list = mutableWorkTagMutableList
        val size = list.size
        val i = if (refIndex >= 0) refIndex else size + refIndex + 1
        val workTag = WorkTag.Mutable.Impl(
            workTagFactoryParams.id,
            workTagFactoryParams.name,
        )
        addWorkTag(i, workTag)
        viewModelScope.launch {
            workTag.run {
                val currId = id
                val prevId = link.prev.idGlobal
                val nextId = link.next.idGlobal
                Log.i("Workyras.Common", "Insert $name to db")
                link.run {
                    Log.i("Workyras.Common", "prev = $prev, next = $next")
                }
                WorkTagRepository.run {
                    insertWorkTag(
                        WorkTagRepository.WorkTagInsertionParams.Impl(
                            idGlobal = currId,
                            name = name,
                            nextIdGlobal = nextId,
                        )
                    )
                    linkWorkTagNext(idGlobal = prevId, nextIdGlobal = currId)
                    linkWorkTagNext(idGlobal = currId, nextIdGlobal = nextId)
                }
            }
        }
        return i
    }
}

interface WorkTagFactoryParams {

    val name: String

    val id: ByteArray

    class Impl(
        override val name: String,
        override val id: ByteArray,
    ) : WorkTagFactoryParams
}
