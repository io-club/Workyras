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

package fyi.ioclub.workyras.ui.work

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.databinding.FragmentWorkBinding
import fyi.ioclub.workyras.ui.common.StringResource
import fyi.ioclub.workyras.ui.common.Timestamp.dateTimeFormatter
import fyi.ioclub.workyras.ui.common.Timestamp.getLocalDateTimeOfTimestamp
import fyi.ioclub.workyras.ui.common.dialogs.picktimestamp.PickTimestampDialog
import fyi.ioclub.workyras.ui.worktags.WorkTagsViewModel

class WorkFragment : Fragment() {

    private val workTagsViewModel: WorkTagsViewModel by activityViewModels()
    private val workViewModel: WorkViewModel by activityViewModels()

    private val binding get() = requireNotNull(_binding)
    private var _binding: FragmentWorkBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentWorkBinding.inflate(inflater, container, false).apply { _binding = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        workViewModel.tryLateInit {
            WorkViewModel.LateInitParams.Impl(
                requireActivity().getSharedPreferences(
                    this::class.qualifiedName,
                    Context.MODE_PRIVATE
                ),
                WorkViewModel.LateInitParams.PrefKeys.Impl(
                    selectedWorkTagIdGlobal = getString(R.string.pref_work_key_selected_work_tag_id_global),
                    pickedTimestampStart = getString(R.string.pref_work_key_picked_timestamp_start),
                ),
            )
        }

        with(binding) {

            workViewModel.workPtLiveData.observe(viewLifecycleOwner) { workPt ->
                textViewWorkPt.text = StringResource.intFormat.format(workPt)
            }

            val workPtDefault = getString(R.string.text_view_work_pt_default)
            workTagsViewModel.workTagListLiveData.observe(viewLifecycleOwner) { workTagList ->
                with(spinnerWorkTag) {
                    ArrayAdapter(
                        requireContext(),
                        R.layout.spinner_work_tag_item,
                        workTagList.map { it.name },
                    ).apply {
                        adapter = this
                        setDropDownViewResource(R.layout.spinner_work_tag_dropdown_item)
                    }

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long,
                        ) {
                            val currId = workTagList[selectedItemPosition].id
                            if (currId.contentEquals(workViewModel.workTagId.value)) return

                            textViewWorkPt.text = workPtDefault
                            workViewModel.workTagId.value = currId
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            workViewModel.workTagId.value = null
                        }
                    }.also {
                        if (selectedItemPosition == AdapterView.INVALID_POSITION)
                            let(it::onNothingSelected)  // Call `onNothingSelected` manually
                    }

                    workViewModel.workTagId.liveData.observe(viewLifecycleOwner) { workTagId ->
                        setSelection(workTagList.indexOfFirst { it.id.contentEquals(workTagId) })
                    }
                }
            }

            with(workViewModel.timestampStart) {
                liveData.observe(viewLifecycleOwner) {
                    buttonWorkPickTimestampStart.text = getLocalDateTimeOfTimestamp(
                        it,
                        System.currentTimeMillis(),
                    ).format(dateTimeFormatter)
                }
                buttonWorkPickTimestampStart.setOnClickListener {
                    PickTimestampDialog(
                        ::value,
                        R.string.dialog_pick_timestamp_title_start,
                    ).show(childFragmentManager, null)
                }
            }

            buttonAddPt.setOnClickListener {
                try {
                    workViewModel.addWorkPt()
                } catch (e: IllegalStateException) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}