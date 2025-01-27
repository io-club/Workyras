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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.ioclub.workyras.data.db.entities.WorkTagEntity
import fyi.ioclub.workyras.data.repository.WorkTagRepository
import fyi.ioclub.workyras.models.DisplayWorkTag
import fyi.ioclub.workyras.models.WorkTag
import fyi.ioclub.workyras.ui.common.ToLateInit
import fyi.ioclub.workyras.utils.delegates.delegateNotNull
import fyi.ioclub.workyras.utils.runIfNot
import fyi.ioclub.workyras.utils.runQuietly
import kotlinx.coroutines.launch

abstract class WorkTagsViewModelBase : ViewModel(), ToLateInit<Lazy<WorkTagsViewModelBase>> {

    protected abstract val isRecycling: Boolean

    private val oppositeViewModel get() = _lazyOppositeViewModel.value
    private lateinit var _lazyOppositeViewModel: Lazy<WorkTagsViewModelBase>

    protected val mutableWorkTagListLiveData = MutableLiveData<MutableList<WorkTag.Mutable>>()
    val workTagListLiveData: LiveData<out List<DisplayWorkTag>> = mutableWorkTagListLiveData

    val workTagList: List<WorkTag> get() = mutableWorkTagMutableList
    protected val mutableWorkTagList: List<WorkTag.Mutable> get() = mutableWorkTagMutableList
    protected val mutableWorkTagMutableList get() = _mutableWorkTagMutableList
    private var _mutableWorkTagMutableList by mutableWorkTagListLiveData.delegateNotNull

    override val toLateInit get() = !::_lazyOppositeViewModel.isInitialized

    override fun lateInit(param: Lazy<WorkTagsViewModelBase>) {
        _lazyOppositeViewModel = param
        viewModelScope.launch {
            // Load the data from the database
            assert(oppositeViewModel.isRecycling != isRecycling)
            WorkTagRepository.flowAllWorkTags.collect { entityList ->
                runIfNot(isQuiteToCollectFromDb) {
                    Log.d("Workyras.Tags", "Collected in raw ${entityList.size}")
                    entityList.forEach {
                        it.run {
                            Log.d(
                                "Workyras.Tags",
                                "$name: $id -> $nextId"
                            )
                        }
                    }
                    val orderedList = mutableListOf<WorkTag.Mutable>()
                    val oppositeOrderedList = mutableListOf<WorkTag.Mutable>()
                    if (entityList.isNotEmpty()) {
                        val entityMap = entityList.associateBy { it.id }
                        val firstEntity = entityMap[
                            (entityMap.keys - entityList.mapNotNull { it.nextId }.toSet())
                                .apply {
                                    if (size != 1) {
                                        // When happens while adding, it's not an error
                                        Log.w(
                                            "Workyras.Tags",
                                            StringBuilder().appendLine("Wrong number of entity with no prev")
                                                .apply {
                                                    entityList.forEach { appendLine("${it.id} next to ${it.nextId}") }
                                                }.toString()
                                        )
                                        return@collect
                                    }
                                }
                                .first()
                        ]!!
                        val first = WorkTag.Mutable.Impl(firstEntity.idGlobal, firstEntity.name)
                        firstEntity.addToViewModel(first, orderedList, oppositeOrderedList)
                        WorkTag.Mutable.Root.run {
                            link = WorkTag.Mutable.Link.Impl(prev = first, next = first)
                            first.link = WorkTag.Mutable.Link.Impl(prev = this, next = this)
                        }
                        var curr = first
                        var currEntityId = firstEntity.nextId
                        while (currEntityId != null) {
                            entityMap[currEntityId]!!.run {
                                currEntityId = nextId
                                curr =
                                    WorkTag.Mutable.Impl(idGlobal, name)
                                        .apply { insertedAfter(curr) }
                                addToViewModel(curr, orderedList, oppositeOrderedList)
                            }
                        }
                    }

                    for ((liveData, list) in arrayOf(
                        mutableWorkTagListLiveData to orderedList,
                        oppositeViewModel.mutableWorkTagListLiveData to oppositeOrderedList
                    )) liveData.postValue(list)

                    Log.d("Workyras.Tags", "Loaded count ${orderedList.size}")
                    orderedList.forEach {
                        Log.d("Workyras.Tags", "Loaded $it")
                        it.link.run {
                            Log.d("Workyras.Tags", "prev = $prev, next=$next")
                        }
                    }
                }
            }
        }
    }

    /** Need manual global insertion. */
    private fun WorkTagEntity.addToViewModel(
        workTag: WorkTag.Mutable,
        dstThis: MutableList<WorkTag.Mutable>,
        dstOpposite: MutableList<WorkTag.Mutable>,
    ) = (if (isRecycled == isRecycling) dstThis else dstOpposite).add(workTag)

    // Modifying methods

    fun editWorkTag(index: Int, workTagName: String) =
        mutableWorkTagList[index].run {
            name = workTagName
            viewModelScope.launch {
                WorkTagRepository.renameWorkTag(
                    id,
                    workTagName,
                )
            }
            Unit
        }

    fun moveWorkTag(fromIndex: Int, toIndex: Int) {
        Log.d("Workyras.Tags", "Move from $fromIndex to $toIndex")
        val workTag = mutableWorkTagMutableList.removeAt(fromIndex)
        val currId = workTag.id
        fun WorkTag.Mutable.getPair() = link.run { prev to next }.toList().map { it.idGlobal }
        val (oldPrevId, oldNextId) = workTag.getPair()
        if (toIndex == mutableWorkTagList.size) workTag.link = WorkTag.Mutable.Link.Unbound
        addWorkTag(toIndex, workTag)
        val (newPrevId, newNextId) = workTag.getPair()
        viewModelScope.launch {
            WorkTagRepository.run {
                runQuietly(::isQuiteToCollectFromDb) {
                    // Update relative position info of [this]
                    linkWorkTagNext(idGlobal = oldPrevId, nextIdGlobal = oldNextId)

                    // Insert [currId] between [newPrevId] and [newNextId]
                    linkWorkTagNext(idGlobal = newPrevId, nextIdGlobal = currId)
                    linkWorkTagNext(idGlobal = currId, nextIdGlobal = newNextId)
                }
            }
        }
    }

    /** @return new index of work tag in opposite view model. */
    fun transferWorkTag(index: Int): Int {
        val workTag = mutableWorkTagMutableList.removeAt(index)
        viewModelScope.launch {
            WorkTagRepository.setWorkTagRecycled(
                workTag.id,
                oppositeViewModel.isRecycling,
            )
        }
        return oppositeViewModel.receiveWorkTag(workTag)
    }

    private fun receiveWorkTag(workTagRcv: WorkTag): Int {
        // Find the suitable index where to add the work tag
        val list = workTagList
        val size = list.size
        var i = 0
        var curr: WorkTag.Mutable = WorkTag.Mutable.Root
        while (i < size && curr !== workTagRcv)
            if (curr.link.next.also { curr = it } === list[i]) i++

        addWorkTag(i, workTagRcv)
        return i
    }

    protected fun addWorkTag(index: Int, workTag: WorkTag) {
        // Link update in global work tag list
        val list = mutableWorkTagMutableList
        (workTag as WorkTag.Mutable).run {
            val isListRelevant = index < list.size
            (
                    if (link !== WorkTag.Mutable.Link.Unbound)
                        if (isListRelevant) {
                            link.removed()
                            list[index]
                        } else null
                    else
                        if (isListRelevant) list[index] else WorkTag.Mutable.Root

                    )?.let { insertedBefore(it) }
        }
        list.add(index, workTag)
    }

    /* For debugging. */
    private fun logEntireList() {
        // Log entire list
        val builder = StringBuilder()
            .appendLine("Global Work Tag List:")
        var curr = WorkTag.Root.link.next
        while (curr !== WorkTag.Root) {
            builder.appendLine("${curr.name},")
            curr = curr.link.next
        }
        Log.d("Workyras.Tags", builder.toString())
    }

    companion object {

        @JvmStatic
        protected val WorkTag.idGlobal get() = if (this !== WorkTag.Root) id else null

        @JvmStatic
        protected var isQuiteToCollectFromDb = false
    }
}
