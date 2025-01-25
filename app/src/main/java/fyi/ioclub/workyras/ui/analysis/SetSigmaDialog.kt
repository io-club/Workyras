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

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.databinding.DialogSetSigmaBinding
import fyi.ioclub.workyras.ui.common.StringResource
import fyi.ioclub.workyras.utils.runIfNot
import fyi.ioclub.workyras.utils.runQuietly
import kotlin.reflect.KMutableProperty0

class SetSigmaDialog(
    sigmaMinProperty: KMutableProperty0<Int>,
    sigmaMaxProperty: KMutableProperty0<Int>,
    sigmaProperty: KMutableProperty0<Int>,
) : DialogFragment() {

    private var sigmaMin by sigmaMinProperty
    private var sigmaMax by sigmaMaxProperty
    private var sigma by sigmaProperty

    private var sigmaMinCache = sigmaMin
    private var sigmaMaxCache = sigmaMax
    private var sigmaCache = sigma

    private var isQuietToEditTextSigmaTextChanging = false

    private var isQuietToSeekBarSigmaChanging = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        DialogSetSigmaBinding.inflate(layoutInflater).run {
            initUI()
            AlertDialog.Builder(context)
                .setTitle(R.string.dialog_set_sigma_title)
                .setView(root)
                .setPositiveButton(R.string.dialog_positive_button_text) { _, _ ->
                    sigmaMin = sigmaMinCache
                    sigmaMax = sigmaMaxCache
                    sigma = sigmaCache
                }.create()
        }

    private fun DialogSetSigmaBinding.initUI() {

        editTextSigmaCache.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable?) =
                runIfNot(isQuietToEditTextSigmaTextChanging || s.isNullOrBlank()) {
                    sigmaCache = try {
                        s.toString().toInt()
                    } catch (e: NumberFormatException) {
                        Log.e("Workyras.Analysis", "Cannot set sigma to $s ")
                        return
                    }
                    updateProgressBarProgress()
                }
        })

        seekBarSigmaCache.run {
            arrayOf(
                (buttonSetSeekBarSigmaCacheMin to textViewSeekBarSigmaMin) to (::setMin to ::sigmaMinCache),
                (buttonSetSeekBarSigmaCacheMax to textViewSeekBarSigmaMax) to (::setMax to ::sigmaMaxCache),
            )
        }.forEach { (views, models) ->
            val (btn, tv) = views
            val (seekBarSetter, cacheProperty) = models
            updateBoundText(tv, cacheProperty.get())
            btn.setOnClickListener {
                with(sigmaCache) {
                    let(cacheProperty::set)
                    updateBoundText(tv, cacheProperty.get())
                    runQuietly(::isQuietToSeekBarSigmaChanging) { let(seekBarSetter) }
                }
                updateProgressBarProgress()
            }
        }

        with(seekBarSigmaCache) {

            min = sigmaMinCache
            max = sigmaMaxCache

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) = runIfNot(isQuietToSeekBarSigmaChanging) {
                    sigmaCache = progress
                    updateText()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        updateAll()
    }

    private fun DialogSetSigmaBinding.updateText() =
        runQuietly(::isQuietToEditTextSigmaTextChanging) {
            editTextSigmaCache.setText(StringResource.intFormat.format(sigmaCache))
        }

    private fun DialogSetSigmaBinding.updateProgressBarProgress() =
        runQuietly(::isQuietToSeekBarSigmaChanging) {
            with(seekBarSigmaCache) { progress = sigmaCache.coerceIn(min, max) }
        }

    private fun DialogSetSigmaBinding.updateAll() {
        updateText()
        updateProgressBarProgress()
    }

    companion object {

        private fun updateBoundText(textView: TextView, boundCache: Int) {
            textView.text = StringResource.intFormat.format(boundCache)
        }
    }
}