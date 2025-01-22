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

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.databinding.DialogAddWorkTagBinding
import fyi.ioclub.workyras.databinding.FragmentWorkTagsBinding
import fyi.ioclub.workyras.enableGestureOnView
import fyi.ioclub.workyras.ui.common.worktagsbase.WorkTagsFragmentBase
import fyi.ioclub.workyras.ui.worktagbin.WorkTagBinViewModel
import fyi.ioclub.workyras.utils.hexDecoded
import fyi.ioclub.workyras.utils.hexEncoded
import kotlin.random.Random

class WorkTagsFragment :
    WorkTagsFragmentBase<FragmentWorkTagsBinding, WorkTagAdapter, WorkTagsViewModel, WorkTagBinViewModel>(
        FragmentWorkTagsBinding::inflate,
        ::WorkTagAdapter,
    ) {

    override val viewModel: WorkTagsViewModel by activityViewModels()
    override val lazyOppositeViewModel = activityViewModels<WorkTagBinViewModel>()

    private val defaultWorkTagName: String
        get() {
            val format = defaultWorkTagNameFormat
            val list = viewModel.workTagList.map { it.name }
            var i = list.size
            var name: String
            do {
                name = format.format(++i)
                Log.i("Workyras.Common", "Default work tag as $name")
            } while (name in list)
            return name
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        lateInit(binding.layoutEmptyWorkTags.root)
        super.onViewCreated(view, savedInstanceState)

        if (!isDefaultWorkTagNameFormatInitialized) _defaultWorkTagNameFormat =
            getString(R.string.edit_text_new_work_tag_name_text_default)

        with(binding) {

            recyclerViewWorkTags.run {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = this@WorkTagsFragment.adapter
                ItemTouchHelper(
                    this@WorkTagsFragment.adapter.WorkTagTouchHelperCallback()
                ).attachToRecyclerView(this)
                let(::enableGestureOnView)
            }

            buttonAddWorkTag.setOnClickListener { showAddWorkTagDialog() }
        }
    }

    private fun showAddWorkTagDialog() {
        val context = requireContext()
        with(DialogAddWorkTagBinding.inflate(LayoutInflater.from(context))) {
            val genIdSize =
                PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                    getString(R.string.pref_gen_work_tag_id_size_key),
                    getString(R.string.pref_gen_work_tag_id_size_default),
                )!!.toInt()
            var id = Random.nextBytes(genIdSize)
            with(editTextNewWorkTagId) {
                setText(id.hexEncoded)
                addTextChangedListener(object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) = Unit

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) = Unit

                    override fun afterTextChanged(s: Editable?) {
                        if (s.isNullOrBlank()) return
                        val str = s.toString()
                        try {
                            id = str.hexDecoded
                        } catch (e: IllegalArgumentException) {
                            Log.e("Workyras.WorkTags", "$s is not a hex string")
                            setBackgroundColor(
                                resources.getColor(R.color.red, null)
                            )
                            return
                        }
                        setBackgroundColor(
                            resources.getColor(R.color.transparent, null)
                        )
                    }
                })
            }

            editTextNewWorkTagName.setText(defaultWorkTagName)
            AlertDialog.Builder(context).setTitle(R.string.dialog_add_work_tag_title)
                .setView(root)
                .setPositiveButton(R.string.dialog_positive_button_text) { _, _ ->
                    val name = editTextNewWorkTagName.text.toString().trim()
                    if (name.isEmpty()) return@setPositiveButton
                    adapter.insertWorkTag(
                        -1, WorkTagFactoryParams.Impl(
                            name,
                            id,
                        )
                    )
                }.show()
        }
    }

    companion object {

        private val defaultWorkTagNameFormat get() = _defaultWorkTagNameFormat
        private lateinit var _defaultWorkTagNameFormat: String
        private val isDefaultWorkTagNameFormatInitialized get() = ::_defaultWorkTagNameFormat.isInitialized
    }
}