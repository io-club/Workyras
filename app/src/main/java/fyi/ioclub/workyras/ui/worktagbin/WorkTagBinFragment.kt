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

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import fyi.ioclub.workyras.MainActivity
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.databinding.DialogClearWorkTagsBinding
import fyi.ioclub.workyras.databinding.FragmentWorkTagBinBinding
import fyi.ioclub.workyras.enableGestureOnView
import fyi.ioclub.workyras.ui.common.worktagsbase.WorkTagsFragmentBase
import fyi.ioclub.workyras.ui.worktags.WorkTagsViewModel

class WorkTagBinFragment :
    WorkTagsFragmentBase<FragmentWorkTagBinBinding, RecycleWorkTagAdapter, WorkTagBinViewModel, WorkTagsViewModel>(
        FragmentWorkTagBinBinding::inflate,
        ::RecycleWorkTagAdapter,
    ) {

    override val viewModel: WorkTagBinViewModel by activityViewModels()
    override val lazyOppositeViewModel = activityViewModels<WorkTagsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        lateInit(binding.layoutEmptyWorkTagBin.root)
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            val fragmentAdapter = adapter
            recyclerViewWorkTagBin.run {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = fragmentAdapter
                ItemTouchHelper(
                    fragmentAdapter.WorkTagTouchHelperCallback()
                ).attachToRecyclerView(this)
                let(::enableGestureOnView)
            }

            buttonClearWorkTags.setOnClickListener { showClearWorkTagsDialog(adapter) }
        }
    }

    override fun onResume() {
        super.onResume()

        (requireActivity() as MainActivity).resumeNavDrawerFragment()
    }

    private fun showClearWorkTagsDialog(adapter: RecycleWorkTagAdapter) {
        val context = requireContext()
        with(DialogClearWorkTagsBinding.inflate(LayoutInflater.from(context))) {
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.dialog_clear_work_tags_title))
                .setView(root)
                .setPositiveButton(getString(R.string.dialog_clear_wok_tags_positive_button)) { _, _ ->
                    adapter.clearWorkTags()
                }
                .show()
        }
    }
}
