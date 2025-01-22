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

package fyi.ioclub.workyras.ui.save

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.opencsv.bean.CsvToBeanBuilder
import com.opencsv.bean.StatefulBeanToCsvBuilder
import fyi.ioclub.workyras.MainActivity
import fyi.ioclub.workyras.R
import fyi.ioclub.workyras.data.csv.bean.WorkPointBean
import fyi.ioclub.workyras.data.csv.bean.WorkTagBean
import fyi.ioclub.workyras.data.repository.WorkPointRepository
import fyi.ioclub.workyras.data.repository.WorkTagRepository
import fyi.ioclub.workyras.databinding.FragmentSaveBinding
import fyi.ioclub.workyras.enableGestureOnView
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.reflect.KClass


class SaveFragment : Fragment() {

    private val binding get() = requireNotNull(_binding)
    private var _binding: FragmentSaveBinding? = null

    private val toLoadLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            it?.also {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }?.let(::loadFromUri)
        }

    private val toSaveLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            it?.also {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }?.let(::saveToUri)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentSaveBinding.inflate(inflater, container, false).also(::_binding::set).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {

        with(webViewSaveInstruction) {
            loadUrl(
                getString(
                    R.string.asset_url_format, getString(R.string.html_save)
                )
            )
            // No gesture for nav drawer enabled here to make horizontal page scroll easy
        }

        for ((btn, launcher) in arrayOf(
            buttonLoadFromFile to toLoadLauncher,
            buttonSaveToFile to toSaveLauncher,
        )) with(btn) {
            setOnClickListener { launcher.launch(null) }
            // Instead we enable user to use gesture in all other areas of screen
            let(::enableGestureOnView)
        }
    }

    override fun onResume() {
        super.onResume()

        (requireActivity() as MainActivity).resumeNavDrawerFragment()
    }

    private fun loadFromUri(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        fun <T : Any> loadTable(beanType: KClass<T>, tableName: String): List<T> {
            val fileUri = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
                        + "/" + TABLE_FILE_NAME_FORMAT.format(tableName),
            )
            val inputStream = contentResolver.openInputStream(fileUri)
                ?: throw IOException("Failed to open input stream")
            val reader = InputStreamReader(inputStream)
            val csvToBean = CsvToBeanBuilder<T>(reader).withType(beanType.java).build()
            val beans = csvToBean.parse()
            return beans
        }
        lifecycleScope.launch {
            try {
                val workTagBeans = loadTable(WorkTagBean::class, TABLE_NAME_WORK_TAGS)
                val workPointBeans = loadTable(WorkPointBean::class, TABLE_NAME_WORK_POINTS)
                WorkTagRepository.deleteAllWorkTags()   // Also delete all work points
                val reversed = run {
                    var last: WorkTagBean? = null
                    val nextIdToBeanMap =
                        workTagBeans.filterNot { bean ->
                            (bean.nextId == null).also { if (it) last = bean }
                        }.associateBy { it.nextId!! }
                    var next = requireNotNull(last)
                    val size = workTagBeans.size
                    val end = size - 1
                    Array(size) { i ->
                        next.let {
                            if (i < end) next = nextIdToBeanMap.getValue(it.id)
                            it
                        }
                    }
                }
                val idToGlobalMap = workTagBeans.associate { it.run { id to idGlobal } }
                for (bean in reversed) WorkTagRepository.insertWorkTag(
                    WorkTagRepository.WorkTagInsertionParams.Impl(
                        idGlobal = bean.idGlobal,
                        name = bean.name,
                        nextIdGlobal = bean.nextId?.let(idToGlobalMap::getValue),
                    )
                )
                for (bean in workPointBeans) WorkPointRepository.insertWorkPoint(
                    WorkPointRepository.WorkPointInsertionParams.Impl(
                        workTagIdGlobal = idToGlobalMap.getValue(bean.workTagId),
                        comment = bean.comment,
                        producedAt = bean.producedAt,
                    )
                )
                makeToast("Loading completed").show()
            } catch (e: Exception) {
                makeToastOfException(e)
                throw e
            }
        }
    }

    private fun saveToUri(uri: Uri) {
        fun <T> saveTable(tableName: String, beans: List<T>) {
            val contentResolver = requireContext().contentResolver
            val fileUri = DocumentsContract.createDocument(
                contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri)
                ),
                "text/csv",
                TABLE_FILE_NAME_FORMAT.format(tableName),
            )!!

            val outputStream = contentResolver.openOutputStream(fileUri)
                ?: throw IOException("Failed to open output stream")

            val writer = OutputStreamWriter(outputStream)
            StatefulBeanToCsvBuilder<T>(writer)
                .withApplyQuotesToAll(false)
                .build()
                .write(beans)
            writer.close()
        }
        lifecycleScope.launch {
            try {
                if (DocumentFile.fromTreeUri(requireContext(), uri)!!.listFiles().isNotEmpty())
                    throw IllegalStateException("Selected directory is not empty")

                val workTags = WorkTagRepository.getAllWorkTags()
                val idAbbreviatingMap = run {
                    var idAbbreviated = 1L   // Counts from 1
                    workTags.associate { it.id to idAbbreviated++ }
                }

                saveTable(
                    TABLE_NAME_WORK_TAGS,
                    workTags.map { WorkTagBean.abbreviatedFromEntity(it, idAbbreviatingMap) },
                )
                saveTable(
                    TABLE_NAME_WORK_POINTS,
                    WorkPointRepository.getAllWorkPoints().mapNotNull {
                        WorkPointBean.abbreviatedFromEntity(it, idAbbreviatingMap)
                    },
                )

                makeToast("Saving completed").show()
            } catch (e: Exception) {
                makeToastOfException(e)
            }
        }
    }

    private fun makeToastOfException(e: Exception) =
        makeToast("${e::class.simpleName}:\n${e.message}").show()

    private fun makeToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(requireContext(), text, duration)

    //    private fun test(uri: Uri) {
//        val contentResolver = requireContext().contentResolver
//        try {
//            lifecycleScope.launch {
//                val fileUri = DocumentsContract.buildDocumentUriUsingTree(
//                    uri,
//                    "${DocumentsContract.getTreeDocumentId(uri)}/test.csv",
//                )!!
//                val tUri = DocumentsContract.buildDocumentUriUsingTree(
//                    uri,
//                    DocumentsContract.getTreeDocumentId(uri)
//                )
//                DocumentFile.fromTreeUri(requireContext(), tUri)!!.listFiles()
//                val inputStream = contentResolver.openInputStream(fileUri) ?: run {
//                    makeToast("Failed to open input stream", Toast.LENGTH_SHORT).show()
//                    return@launch
//                }
//                val reader = InputStreamReader(inputStream)
//                val beans =
//                    CsvToBeanBuilder<WorkTagBean>(reader).withType(WorkTagBean::class.java).build()
//                        .parse()
//                val idG = beans[0].idGlobal
//                beans
//
////                val fileUri = DocumentsContract.createDocument(
////                    contentResolver,
////                    DocumentsContract.buildDocumentUriUsingTree(
////                        uri,
////                        DocumentsContract.getTreeDocumentId(uri)
////                    ),
////                    "text/csv",
////                    "test.csv",
////                )!!
////                val outputStream = contentResolver.openOutputStream(fileUri) ?: run {
////                    makeToast("Failed to open output stream", Toast.LENGTH_SHORT).show()
////                    return@launch
////                }
////                val writer = OutputStreamWriter(outputStream)
////                with(StatefulBeanToCsvBuilder<WorkTagBean>(writer).withApplyQuotesToAll(false).build()) {
////                    write(WorkTagBean(1, "ff88ff".hexDecoded, "nam", true, 0))
////                }
////                writer.close()
//
//                makeToast("Saving completed", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            makeToast(e.toString(), Toast.LENGTH_SHORT).show()
//        }
//    }

    companion object {

        const val TABLE_FILE_NAME_FORMAT = "%s.csv"

        const val TABLE_NAME_WORK_TAGS = "work_tags"
        const val TABLE_NAME_WORK_POINTS = "work_points"
    }
}
