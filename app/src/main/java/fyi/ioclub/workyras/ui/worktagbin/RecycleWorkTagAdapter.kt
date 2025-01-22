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

import fyi.ioclub.workyras.ui.common.worktagsbase.WorkTagAdapterBase
import fyi.ioclub.workyras.ui.worktags.WorkTagAdapter
import fyi.ioclub.workyras.ui.worktags.WorkTagsViewModel

class RecycleWorkTagAdapter(viewModel: WorkTagBinViewModel) :
    WorkTagAdapterBase<WorkTagBinViewModel, WorkTagsViewModel>(viewModel) {

    override val oppositeAdapter get() = WorkTagAdapter.instance

    init {
        _instance = this
    }

    fun clearWorkTags() {
        notifyItemRangeRemoved(0, itemCount)
        viewModel.clearWorkTags()
    }

    companion object {

        val instance get() = _instance
        private var _instance: RecycleWorkTagAdapter? = null
    }
}
