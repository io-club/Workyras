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

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginEnd
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fyi.ioclub.workyras.databinding.ItemWorkTagBinding
import fyi.ioclub.workyras.databinding.PopupWorkTagInfoBinding
import fyi.ioclub.workyras.models.DisplayWorkTag
import fyi.ioclub.workyras.utils.hexEncoded

abstract class WorkTagAdapterBase<VM : WorkTagsViewModelBase, OVM : WorkTagsViewModelBase>(
    protected val viewModel: VM,
) : ListAdapter<DisplayWorkTag, WorkTagAdapterBase<VM, OVM>.TagViewHolder>(TagDiffCallback()) {

    protected abstract val oppositeAdapter: WorkTagAdapterBase<OVM, VM>?

    class TagDiffCallback : DiffUtil.ItemCallback<DisplayWorkTag>() {

        override fun areItemsTheSame(oldItem: DisplayWorkTag, newItem: DisplayWorkTag) =
            oldItem.id.contentEquals(newItem.id)

        override fun areContentsTheSame(oldItem: DisplayWorkTag, newItem: DisplayWorkTag) =
            oldItem.name == newItem.name
    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TagViewHolder(
        ItemWorkTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
    )

    final override fun onBindViewHolder(holder: TagViewHolder, position: Int) = holder.update()

    inner class TagViewHolder(private val binding: ItemWorkTagBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val workTag get() = viewModel.workTagList[adapterPosition]

        init {
            with(binding) {
                itemMarginEnd ?: run { _itemMarginEnd = root.marginEnd }
                root.setOnClickListener {
                    with(
                        PopupWorkTagInfoBinding.inflate(
                            LayoutInflater.from(root.context), root, false
                        )
                    ) {
                        with(
                            PopupWindow(
                                root,
                                WorkTagsFragmentBase.width - itemMarginEnd!!,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            )
                        ) {
                            isFocusable = true
                            isClippingEnabled = false
                            inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED

                            val workTag = workTag
                            textViewWorkTagInfoId.text = workTag.id.hexEncoded
                            val editText = editTextWorkTagInfoName
                            val prevName = workTag.name
                            editText.setText(prevName)

                            // TODO: Check whether the objects have to be changed during this time

                            setOnDismissListener {
                                val currName = editText.text.toString()
                                if (currName == prevName) return@setOnDismissListener
                                val pos = adapterPosition
                                viewModel.editWorkTag(pos, currName)
                                notifyItemChanged(pos)
                            }

                            showAsDropDown(itemView)
                        }
                    }
                }
            }
        }

        fun update() {
            with(binding) {
                textViewWorkTagName.text = workTag.name
            }
        }
    }

    companion object {
        private val itemMarginEnd get() = _itemMarginEnd
        private var _itemMarginEnd: Int? = null
    }

    inner class WorkTagTouchHelperCallback : ItemTouchHelper.Callback() {

        override fun isLongPressDragEnabled(): Boolean = true
        override fun isItemViewSwipeEnabled(): Boolean = true
        override fun getMovementFlags(
            recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
        ) = makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END,
        )

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val fromPos = viewHolder.adapterPosition
            val toPos = target.adapterPosition
            viewModel.moveWorkTag(fromPos, toPos)
            notifyItemMoved(fromPos, toPos)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val fromIndex = viewHolder.adapterPosition
            val toIndex = viewModel.transferWorkTag(fromIndex)
            notifyItemRemoved(fromIndex)
            oppositeAdapter?.notifyItemChanged(toIndex)
        }
    }
}
