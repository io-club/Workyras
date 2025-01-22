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

package fyi.ioclub.workyras.ui.settings

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import fyi.ioclub.workyras.MainActivity
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.enableGestureOnView

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        requireNotNull(findPreference<EditTextPreference>(getString(R.string.pref_gen_work_tag_id_size_key)))
            .setOnPreferenceChangeListener { _, newValue ->
                (newValue as String).toIntOrNull() in GEN_WORK_TAG_ID_SIZE_RANGE
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enableGestureOnView(listView)
    }

    override fun onResume() {
        super.onResume()

        (requireActivity() as MainActivity).resumeNavDrawerFragment()
    }

    companion object {

        val GEN_WORK_TAG_ID_SIZE_RANGE = 1..64
    }
}
