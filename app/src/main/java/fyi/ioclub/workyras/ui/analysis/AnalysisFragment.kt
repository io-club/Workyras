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

package fyi.ioclub.workyras.ui.analysis

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.databinding.FragmentAnalysisBinding
import fyi.ioclub.workyras.ui.common.StringResource
import fyi.ioclub.workyras.ui.common.Timestamp.dateTimeFormatter
import fyi.ioclub.workyras.ui.common.Timestamp.getLocalDateTimeOfTimestamp
import fyi.ioclub.workyras.ui.common.calculateTimestampFrequencies
import fyi.ioclub.workyras.ui.common.dialogs.picktimestamp.PickTimestampDialog
import fyi.ioclub.workyras.ui.common.dividedBy1k
import fyi.ioclub.workyras.ui.common.multiplyBy1k
import fyi.ioclub.workyras.ui.worktags.WorkTagsViewModel
import fyi.ioclub.workyras.utils.generateEvenlyDividedColors
import fyi.ioclub.workyras.utils.resolveAttribute
import fyi.ioclub.workyras.utils.runIfNot
import fyi.ioclub.workyras.utils.runQuietly

class AnalysisFragment : Fragment() {

    private val workTagsViewModel: WorkTagsViewModel by activityViewModels()
    private val analysisViewModel: AnalysisViewModel by activityViewModels()

    private val binding get() = requireNotNull(_binding)
    private var _binding: FragmentAnalysisBinding? = null

    private var isQuietToSeekBarSigmaChange = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentAnalysisBinding.inflate(inflater, container, false).also(::_binding::set).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Rotate screen for better viewing experience
        val activity = requireActivity().apply {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val context = requireContext()

        analysisViewModel.tryLateInit {
            AnalysisViewModel.LateInitParams.Impl(
                workTagsViewModel,
                activity.getSharedPreferences(this::class.qualifiedName, Context.MODE_PRIVATE),
                AnalysisViewModel.LateInitParams.PrefKeys.Impl(
                    sigmaMin = getString(R.string.pref_analysis_key_sigma_min),
                    sigmaMax = getString(R.string.pref_analysis_key_sigma_max),
                    sigma = getString(R.string.pref_analysis_key_sigma),
                    pickedTimestampStart = getString(R.string.pref_analysis_key_picked_timestamp_start),
                    pickedTimestampEnd = getString(R.string.pref_analysis_key_picked_timestamp_end),
                ),
            )
        }

        val colorPrimaryVariant =
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryVariant)

        val colorSecondaryVariant =
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorSecondaryVariant)
        val textColorPrimary = context.theme.resolveAttribute(android.R.attr.textColorPrimary)
        with(binding) {

            with(workPtChart) {
                xAxis.run {
                    position = XAxis.XAxisPosition.BOTTOM
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) =
                            getLocalDateTimeOfTimestamp(value.multiplyBy1k).format(dateTimeFormatter)
                    }
                }
                listOf(xAxis, axisLeft, axisRight).forEach {
                    with(it) {
                        setDrawGridLines(false)
                        textColor = textColorPrimary
                        axisLineColor = colorPrimaryVariant
                    }
                }
                description.run {
                    text = getString(R.string.work_pt_chart_description)
                    textColor = colorPrimaryVariant
                }
                legend.textColor = textColorPrimary
                setNoDataTextColor(colorSecondaryVariant)
            }

            fun updateChart() {
                val taggedLists = analysisViewModel.workPointLists ?: return
                val colorOff = context.theme.resolveAttribute(android.R.attr.colorPrimary)
                val colorList = generateEvenlyDividedColors(taggedLists.size, colorOff)
                val entryLists = taggedLists.map { (_, list) ->
                    val listInSec = list.map(Long::dividedBy1k)
                    val freqList =
                        calculateTimestampFrequencies(listInSec, analysisViewModel.sigma.value)
                    (listInSec zip freqList).map { (time, freq) -> Entry(time, freq) }
                }
                val dataSetList = taggedLists.mapIndexed { i, (tag, _) ->
                    LineDataSet(entryLists[i], tag.name).apply {
                        setDrawValues(false)
                        colorList[i]
                            .also(::setColor)
                            .also(::setCircleColor)
                            .also(::setHighLightColor)
                    }
                }
                with(workPtChart) {
                    data = LineData(dataSetList)
                    invalidate()
                }
            }

            with(analysisViewModel) {

                workPointListsLiveData.observe(viewLifecycleOwner) { updateChart() }

                for ((wp, setter) in seekBarSigma.run {
                    arrayOf(
                        sigmaMin to ::setMin,
                        sigmaMax to ::setMax,
                    )
                }) wp.liveData.observe(viewLifecycleOwner) {
                    runQuietly(::isQuietToSeekBarSigmaChange) { setter(it) }
                }
                sigma.liveData.observe(viewLifecycleOwner) { sigma ->
                    textViewSigma.text = StringResource.intFormat.format(sigma)
                    with(seekBarSigma) {
                        sigma.coerceIn(min, max).let {
                            if (progress != it)
                                progress = it
                        }
                    }
                    updateChart()
                }

                arrayOf(
                    Triple(
                        timestampStart,
                        buttonAnalysisPickTimestampStart,
                        R.string.dialog_pick_timestamp_title_start,
                    ),
                    Triple(
                        timestampEnd,
                        buttonAnalysisPickTimestampEnd,
                        R.string.dialog_pick_timestamp_title_end,
                    ),
                ).forEach { (wp, btn, titleId) ->
                    wp.liveData.observe(viewLifecycleOwner) { timestamp ->
                        btn.text = getLocalDateTimeOfTimestamp(
                            timestamp,
                            System.currentTimeMillis(),
                        ).format(dateTimeFormatter)
                    }
                    btn.setOnClickListener {
                        PickTimestampDialog(
                            wp::value,
                            titleId,
                        ).show(childFragmentManager, null)
                    }
                }

                seekBarSigma.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) = runIfNot(isQuietToSeekBarSigmaChange) { sigma.value = progress }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })

                buttonSetSigma.setOnClickListener {
                    SetSigmaDialog(
                        sigmaMin::value,
                        sigmaMax::value,
                        sigma::value,
                    ).show(childFragmentManager, null)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null

        // Rotate screen back
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
    }
}
