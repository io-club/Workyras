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
package fyi.ioclub.workyras.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import fyi.ioclub.workyras.MainActivity
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.databinding.FragmentAboutBinding
import fyi.ioclub.workyras.enableGestureOnView


class AboutFragment : Fragment() {

    private val binding get() = _binding
    private lateinit var _binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentAboutBinding.inflate(inflater, container, false).also(::_binding::set).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {

        with(webViewAbout)
        {
            loadUrl(
                getString(
                    R.string.asset_url_format, getString(R.string.html_about)
                )
            )
            with(requireActivity().onBackPressedDispatcher) {
                addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (canGoBack()) goBack()
                        else {
                            isEnabled = false
                            onBackPressed()
                        }
                    }
                })
            }
            let(::enableGestureOnView)
        }

        buttonOpenHelpInUsage.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_open_help_in_usage_url_title)
                .setPositiveButton(getString(R.string.dialog_positive_button_text)) { _, _ ->
                    val uri = getString(R.string.help_url).let(Uri::parse)
                    Intent(Intent.ACTION_VIEW, uri).let(::startActivity)
                }.show()
        }

        textViewAddressToDonateTo.text = ADDRESS_TO_DONATE_TO
        buttonCopyAddressToDonateTo.setOnClickListener {
            getSystemService(requireContext(), ClipboardManager::class.java).let(::requireNotNull)
                .setPrimaryClip(
                    ClipData.newPlainText(
                        "Donate to ${getString(R.string.app_name)}",
                        ADDRESS_TO_DONATE_TO,
                    )
                )
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        (requireActivity() as MainActivity).resumeNavDrawerFragment()
    }

    companion object {

        private const val ADDRESS_TO_DONATE_TO =
            "bitcoin:bc1qhf4wv9l6ma5jc0lmr6mjm" + "pf" + "xq4gw485mwq" + "th" + "zn"
    }
}
