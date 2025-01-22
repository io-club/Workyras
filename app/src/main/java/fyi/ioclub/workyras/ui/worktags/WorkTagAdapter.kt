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

import fyi.ioclub.workyras.ui.common.worktagsbase.WorkTagAdapterBase
import fyi.ioclub.workyras.ui.worktagbin.RecycleWorkTagAdapter
import fyi.ioclub.workyras.ui.worktagbin.WorkTagBinViewModel

class WorkTagAdapter(viewModel: WorkTagsViewModel) :
    WorkTagAdapterBase<WorkTagsViewModel, WorkTagBinViewModel>(viewModel) {

    override val oppositeAdapter get() = RecycleWorkTagAdapter.instance

    init {
        _instance = this
    }

    fun insertWorkTag(refPosition: Int, workTagFactoryParams: WorkTagFactoryParams) {
        val pos = viewModel.insertWorkTag(refPosition, workTagFactoryParams)
        notifyItemInserted(pos)
    }

    companion object {

        val instance get() = _instance
        private var _instance: WorkTagAdapter? = null
    }
}
