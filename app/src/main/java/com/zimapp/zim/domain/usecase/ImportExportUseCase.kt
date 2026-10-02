package com.zimapp.zim.domain.usecase

import android.net.Uri
import android.util.Log
import com.zimapp.zim.data.repository.ImportExportRepository
import com.zimapp.zim.data.repository.NoteRepositoryImpl
import com.zimapp.zim.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

data class ImportResult(
    val successful: Int,
    val total: Int,
)

class ImportExportUseCase @Inject constructor(
    private val noteRepository: NoteRepositoryImpl,
    private val coroutineScope: CoroutineScope,
    private val fileRepository: ImportExportRepository,
)
{
    /**
     * Import notes from text files.
     */
    fun importNotes(uris: List<Uri>, onResult: (ImportResult) -> Unit) {
        coroutineScope.launch(NonCancellable + Dispatchers.IO) {
            var successful = 0
            uris.forEach{uri ->
                try {
                    noteRepository.addNote(fileRepository.importFile(uri))
                    successful++
                } catch (e: IOException) {
                    Log.e(ImportExportUseCase::class.simpleName, e.message, e)
                }
            }
            withContext(Dispatchers.Main) {
                onResult(ImportResult(successful, uris.size))
            }
        }
    }
}