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

package fyi.ioclub.workyras.ui.common.dialogs.picktimestamp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.databinding.DialogPickTimestampBinding
import fyi.ioclub.workyras.databinding.PopupDatePickerBinding
import fyi.ioclub.workyras.databinding.PopupTimePickerBinding
import fyi.ioclub.workyras.ui.common.StringResource
import fyi.ioclub.workyras.ui.common.Timestamp
import fyi.ioclub.workyras.ui.common.Timestamp.dateTimeFormatter
import fyi.ioclub.workyras.ui.common.Timestamp.fixedTimestamp
import fyi.ioclub.workyras.ui.common.Timestamp.toFixedTimestamp
import fyi.ioclub.workyras.ui.common.Timestamp.toRelativeTimestamp
import fyi.ioclub.workyras.utils.delegates.togglingDelegate
import fyi.ioclub.workyras.utils.runIfNot
import fyi.ioclub.workyras.utils.runQuietly
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlin.reflect.KMutableProperty0

class PickTimestampDialog(
    timestampProperty: KMutableProperty0<Long?>,
    @StringRes private val titleId: Int,
) : DialogFragment() {

    private val viewModel get() = _viewModel
    private lateinit var _viewModel: PickTimestampViewModel

    private val ref = System.currentTimeMillis()

    private var timestamp by timestampProperty

    private var _timestampCache = timestamp
    private var timestampCache
        get() = _timestampCache
        set(value) = (if (value == null || value < ref) value else null)
            .let(::_timestampCache::set)
    private val localDateTimeCache get() = getLocalDateTimeOfTimestamp()

    private var isQuietToRadioCheckedChanging = false

    private var isQuietToEditTextTimestampTextChanging = false

    private var isQuietToEditTextDatetimeTextChanging = false

    private var isCheckedChanging = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        DialogPickTimestampBinding.inflate(layoutInflater).run {
            initUI()
            AlertDialog.Builder(requireParentFragment().requireContext())
                .setTitle(titleId)
                .setView(root)
                .setPositiveButton(R.string.dialog_positive_button_text) { _, _ ->
                    timestamp = timestampCache
                }.create()
        }

    private fun DialogPickTimestampBinding.initUI() {

        _viewModel = activityViewModels<PickTimestampViewModel>().value.apply {
            tryLateInit {
                PickTimestampViewModel.LateInitParams.Impl(
                    requireActivity().getSharedPreferences(
                        this::class.qualifiedName,
                        Context.MODE_PRIVATE
                    ),
                    PickTimestampViewModel.LateInitParams.PrefKeys.Impl(
                        isToShowTimestampRef = getString(R.string.pref_analysis_key_is_to_show_timestamp_ref),
                    ),
                )
            }
        }

        updateAll()

        editTextTimestamp.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isQuietToEditTextTimestampTextChanging) return
                s ?: return
                timestampCache =
                    if (s.isEmpty()) null
                    else
                        try {
                            s.toString().toLong()
                        } catch (e: NumberFormatException) {
                            Log.e("Workyras.Analysis", "$s is not a timestamp")
                            return
                        }
                updateRadioCheck()
                updateDatetimeText()
            }
        })

        editTextDatetime.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable?) =
                runIfNot(isQuietToEditTextDatetimeTextChanging) {
                    s ?: return
                    autoSetTimestamp(if (s.isBlank()) null else {
                        val dateTime: LocalDateTime
                        try {
                            dateTime = LocalDateTime.parse(
                                s.toString(),
                                dateTimeFormatter
                            )
                        } catch (e: DateTimeParseException) {
                            editTextDatetime.setBackgroundColor(
                                resources.getColor(R.color.red, null)
                            )
                            return
                        }
                        editTextDatetime.setBackgroundColor(
                            resources.getColor(R.color.transparent, null)
                        )
                        dateTime.fixedTimestamp.also { if (it > ref) updateDatetimeText() }
                    })
                    updateTimestampText()
                }
        })

        buttonResetTimestamp.setOnClickListener {
            timestampCache = null
            updateAll()
        }

        fun <T : ViewBinding> popUpPickerTemplate(
            bindingFactory: (LayoutInflater, ViewGroup, Boolean) -> T,
            bindingLateInitializer: T.() -> Unit,
            localDateTimeModifierFactory: T.() -> (LocalDateTime) -> LocalDateTime,
        ) {
            with(bindingFactory(layoutInflater, root, false)) {
                bindingLateInitializer()
                with(
                    PopupWindow(
                        root,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    )
                ) {
                    isFocusable = true
                    this.setOnDismissListener {
                        autoSetTimestamp(localDateTimeModifierFactory()(localDateTimeCache).fixedTimestamp)
                        updateTimeText()
                    }
                    showAsDropDown(viewDummyPopupHelper)
                }
            }
        }

        buttonPickDate.setOnClickListener {
            popUpPickerTemplate(PopupDatePickerBinding::inflate, {
                with(datePickerTimestamp) {
                    with(localDateTimeCache) {
                        updateDate(
                            year,
                            monthValue - DATE_PICKER_MONTH_OFFSET,
                            dayOfMonth,
                        )
                    }
                    minDate = 0
                    maxDate = ref
                }
            }) {
                datePickerTimestamp.run {
                    {
                        it
                            .withYear(year)
                            .withMonth(DATE_PICKER_MONTH_OFFSET + month)
                            .withDayOfMonth(dayOfMonth)
                    }
                }
            }
        }

        buttonPickTime.setOnClickListener {
            popUpPickerTemplate(
                PopupTimePickerBinding::inflate,
                {
                    with(timePickerTimestamp) {
                        setIs24HourView(DateFormat.is24HourFormat(context))
                        localDateTimeCache.let {
                            hour = it.hour
                            minute = it.minute
                        }
                    }
                },
            ) {
                timePickerTimestamp.run {
                    {
                        it
                            .withHour(hour)
                            .withMinute(minute)
                    }
                }
            }
        }

        with(viewModel.isToShowTimestampRef) {
            liveData.observe(this@PickTimestampDialog) { updateRefVisibility() }
            radioButtonRelative.setOnClickListener {
                if (isCheckedChanging) isCheckedChanging = false else value = !value
            }
        }

        textViewTimestampRef.text = getString(
            R.string.ref_datetime_format,
            getLocalDateTimeOfTimestamp(ref).format(dateTimeFormatter),
        )

        radioGroupTimestampPickerMode.setOnCheckedChangeListener { _, _ ->
            Log.i("Workyras.Analysis", "on checked change")
            isCheckedChanging = true
            autoSetTimestamp()
            updateTimestampText()
            updateRefVisibility()
        }
    }

    private fun getLocalDateTimeOfTimestamp(ts: Long? = timestampCache) =
        Timestamp.getLocalDateTimeOfTimestamp(ts, ref)

    private fun DialogPickTimestampBinding.autoSetTimestamp(ts: Long? = timestampCache) =
        when (radioGroupTimestampPickerMode.checkedRadioButtonId) {
            radioButtonRelative.id -> toRelativeTimestamp(ts, ref)
            radioButtonFixed.id -> toFixedTimestamp(ts, ref)
            else -> throw AssertionError("Impossible checked radio button ID")
        }.let(::timestampCache::set)

    private fun DialogPickTimestampBinding.updateAll() {
        updateRadioCheck()
        updateTimeText()
    }

    private fun DialogPickTimestampBinding.updateRadioCheck() =
        runQuietly(::isQuietToRadioCheckedChanging) {
            radioGroupTimestampPickerMode.setOnCheckedChangeListener(null)
            radioGroupTimestampPickerMode.check(timestampCache.let { (if (it == null || it < 0) radioButtonRelative else radioButtonFixed) }.id)
        }

    private fun DialogPickTimestampBinding.updateTimeText() {
        updateTimestampText()
        updateDatetimeText()
    }

    private fun DialogPickTimestampBinding.updateTimestampText() =
        runQuietly(::isQuietToEditTextTimestampTextChanging) {
            editTextTimestamp.setText(timestampCache?.let { StringResource.intFormat.format(it) })
        }

    private fun DialogPickTimestampBinding.updateDatetimeText() =
        runQuietly(::isQuietToEditTextDatetimeTextChanging) {
            editTextDatetime.setText(localDateTimeCache.format(dateTimeFormatter))
        }

    private fun DialogPickTimestampBinding.updateRefVisibility() {
        var toggleTextViewTimestampRef by textViewTimestampRef.togglingDelegate
        toggleTextViewTimestampRef =
            radioGroupTimestampPickerMode.checkedRadioButtonId == R.id.radio_button_relative
                    && viewModel.isToShowTimestampRef.value
    }

    companion object {

        /** DatePicker's month has an offset of 1. */
        private const val DATE_PICKER_MONTH_OFFSET = 1
    }
}