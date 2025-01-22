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

package fyi.ioclub.workyras.ui.common.worktagsbase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import fyi.ioclub.workyras.utils.delegates.ReadWriteValueWrapper
import fyi.ioclub.workyras.utils.delegates.togglingDelegate
import fyi.ioclub.workyras.utils.delegates.valueWrapper
import kotlin.properties.Delegates

abstract class WorkTagsFragmentBase<VB : ViewBinding, A : WorkTagAdapterBase<VM, OVM>, VM : WorkTagsViewModelBase, OVM : WorkTagsViewModelBase>(
    private val fragmentBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
    private val workTagAdapterFactory: (VM) -> A,
) : Fragment() {

    protected val binding get() = requireNotNull(_binding)
    private var _binding: VB? = null

    protected abstract val viewModel: VM
    protected abstract val lazyOppositeViewModel: Lazy<OVM>

    protected val adapter get() = _adapter
    private lateinit var _adapter: A

    private var toggleViewEmpty
        get() = _toggleViewEmptyDelegateValueWrapper.value
        set(value) = value.let(_toggleViewEmptyDelegateValueWrapper::value::set)
    private lateinit var _toggleViewEmptyDelegateValueWrapper: ReadWriteValueWrapper<Boolean>

    fun lateInit(viewEmpty: View) {
        _toggleViewEmptyDelegateValueWrapper = viewEmpty.togglingDelegate.valueWrapper
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = fragmentBindingInflater(inflater, container, false).also(::_binding::set).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.run {
            viewTreeObserver.addOnPreDrawListener {
                _width = width
                true
            }
        }

        _adapter = workTagAdapterFactory(viewModel)
        viewModel.workTagListLiveData.observe(viewLifecycleOwner) {
            with(it) {
                let(adapter::submitList)
                toggleViewEmpty = isEmpty()
            }
        }
    }

    final override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    companion object {

        val width get() = _width
        private var _width by Delegates.notNull<Int>()
    }
}