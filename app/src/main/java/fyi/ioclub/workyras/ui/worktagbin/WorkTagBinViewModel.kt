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

package fyi.ioclub.workyras.ui.worktagbin

import android.util.Log
import androidx.lifecycle.viewModelScope
import fyi.ioclub.workyras.data.repository.WorkTagRepository
import fyi.ioclub.workyras.models.WorkTag
import fyi.ioclub.workyras.ui.common.worktagsbase.WorkTagsViewModelBase
import kotlinx.coroutines.launch

/** Every call of modifying methods should followed by a manual UI-notifier. */
class WorkTagBinViewModel : WorkTagsViewModelBase() {

    override val isRecycling = true

    // Modifying methods

    fun clearWorkTags() {
        Log.i("Workyras.Common", "Delete ${workTagList.size} work tags")
        val list = workTagList.map { (it as WorkTag.Cloneable).copy }
        viewModelScope.launch {
            for (it in mutableWorkTagList)
                it.link.run {
                    WorkTagRepository.linkWorkTagNext(
                        idGlobal = prev.idGlobal,
                        nextIdGlobal = next.idGlobal,
                    )
                    removed()
                }
            WorkTagRepository.deleteWorkTagsByIdGlobals(list.map { it.id })
            mutableWorkTagListLiveData.postValue(mutableListOf())
        }
    }
}
