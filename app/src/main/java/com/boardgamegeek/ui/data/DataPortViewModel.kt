package com.boardgamegeek.ui.data

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.export.Constants
import com.boardgamegeek.export.model.CollectionViewForExport
import com.boardgamegeek.export.model.ColorForExport
import com.boardgamegeek.export.model.ExportModel
import com.boardgamegeek.export.model.GameForExport
import com.boardgamegeek.export.model.PlayerColorForExport
import com.boardgamegeek.export.model.UserForExport
import com.boardgamegeek.livedata.ProgressData
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.mappers.mapForExport
import com.boardgamegeek.repository.CollectionViewRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class DataPortViewModel @Inject constructor(
    application: Application,
    private val collectionViewRepository: CollectionViewRepository,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    private companion object {
        const val NAME_TYPE = "type"
        const val NAME_VERSION = "version"
        const val NAME_ITEMS = "items"
    }

    private val gson: Gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message = _message.asSharedFlow()

    private val _collectionViewProgress = MutableStateFlow(ProgressData())
    val collectionViewProgress: StateFlow<ProgressData> = _collectionViewProgress

    private val _gameProgress = MutableStateFlow(ProgressData())
    val gameProgress: StateFlow<ProgressData> = _gameProgress

    private val _userProgress = MutableStateFlow(ProgressData())
    val userProgress: StateFlow<ProgressData> = _userProgress

    fun exportCollectionViews(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val views = collectionViewRepository.loadViews().map { it.mapForExport() }
            export(
                uri,
                Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION,
                1,
                _collectionViewProgress,
                views,
            ) { record: CollectionViewForExport, writer: JsonWriter ->
                gson.toJson(record, CollectionViewForExport::class.java, writer)
            }
        }
    }

    fun exportGames(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val colors = gameRepository.getPlayColors()
                .map { GameForExport(it.key, it.value.map { entity -> ColorForExport(entity.color) }) }
                .filter { it.colors.isNotEmpty() }
            export(
                uri,
                Constants.TYPE_GAMES_DESCRIPTION,
                1,
                _gameProgress,
                colors,
            ) { record: GameForExport, writer: JsonWriter ->
                gson.toJson(record, GameForExport::class.java, writer)
            }
        }
    }

    fun exportUsers(uri: Uri) {
        // TODO export non-users
        viewModelScope.launch(Dispatchers.IO) {
            val buddies = userRepository.loadAllUsers().map {
                val colors = playRepository.loadUserColors(it.username).filter { color -> color.description.isNotBlank() }
                UserForExport(it.username, colors.map { color -> PlayerColorForExport(color.sortOrder, color.description) })
            }.filter { it.colors.isNotEmpty() }
            export(
                uri,
                Constants.TYPE_USERS_DESCRIPTION,
                1,
                _userProgress,
                buddies,
            ) { record: UserForExport, writer: JsonWriter ->
                gson.toJson(record, UserForExport::class.java, writer)
            }
        }
    }

    @SuppressLint("Recycle")
    private suspend fun openFile(uri: Uri): ParcelFileDescriptor? = withContext(Dispatchers.IO) {
        val pfd = try {
            getApplication<BggApplication>().contentResolver.openFileDescriptor(uri, "w")
        } catch (e: SecurityException) {
            Timber.w(e)
            postMessage(R.string.msg_export_failed_permissions, uri)
            null
        } catch (e: FileNotFoundException) {
            Timber.w(e)
            postMessage(R.string.msg_export_failed_file_not_found, uri)
            null
        }
        if (pfd == null) {
            postMessage(R.string.msg_export_failed_null_pfd, uri)
        }
        pfd
    }

    @Suppress("SameParameterValue")
    private suspend fun <T : ExportModel> export(
        uri: Uri,
        typeDescription: String,
        version: Int,
        progress: MutableStateFlow<ProgressData>,
        list: List<T>,
        writeJsonRecord: (record: T, writer: JsonWriter) -> Unit,
    ) = withContext(Dispatchers.IO) {
        openFile(uri)?.use {
            progress.start()
            try {
                JsonWriter(OutputStreamWriter(FileOutputStream(it.fileDescriptor), "UTF-8")).use { writer ->
                    writer.setIndent("  ")
                    writer.beginObject()
                    writer.name(NAME_TYPE).value(typeDescription)
                    writer.name(NAME_VERSION).value(version)
                    writer.name(NAME_ITEMS)
                    writer.beginArray()

                    progress.start(list.size)
                    list.forEachIndexed { index, record ->
                        progress.update(index)
                        try {
                            writeJsonRecord(record, writer)
                        } catch (e: RuntimeException) {
                            Timber.e(e)
                        }
                    }

                    writer.endArray()
                    writer.endObject()
                }

                postMessage(R.string.msg_export_success)
            } catch (e: Exception) {
                Timber.e(e)
                postMessage(R.string.msg_export_failed_write_json)
            } finally {
                progress.complete()
            }
        }
    }

    private fun postMessage(@StringRes resId: Int, vararg formatArgs: Any?) {
        _message.tryEmit(getApplication<BggApplication>().getString(resId, *formatArgs))
    }

    fun importCollectionViews(uri: Uri) {
        viewModelScope.launch {
            import(
                uri,
                Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION,
                _collectionViewProgress,
                { reader -> gson.fromJson(reader, CollectionViewForExport::class.java) },
                { item: CollectionViewForExport, _ -> collectionViewRepository.insertView(item.mapToModel()) },
                { collectionViewRepository.deleteAll() },
            )
        }
    }

    fun importGames(uri: Uri) {
        viewModelScope.launch {
            import(
                uri,
                Constants.TYPE_GAMES_DESCRIPTION,
                _gameProgress,
                { reader -> gson.fromJson(reader, GameForExport::class.java) },
                { item: GameForExport, _ -> gameRepository.replaceColors(item.id, item.colors.map { it.color }) },
            )
        }
    }

    fun importUsers(uri: Uri) {
        viewModelScope.launch {
            import(
                uri,
                Constants.TYPE_USERS_DESCRIPTION,
                _userProgress,
                { reader: JsonReader -> gson.fromJson(reader, UserForExport::class.java) },
                { item: UserForExport, _ -> playRepository.savePlayerColors(item.name, PlayRepository.PlayerType.USER, item.colors.sortedBy { it.sort }.map { it.color }) },
            )
        }
    }

    private fun openFileToRead(uri: Uri): ParcelFileDescriptor? {
        val pfd = try {
            getApplication<BggApplication>().contentResolver.openFileDescriptor(uri, "r")
        } catch (e: SecurityException) {
            Timber.w(e)
            postMessage(R.string.msg_import_failed_file_not_read, uri)
            return null
        } catch (e: FileNotFoundException) {
            Timber.w(e)
            postMessage(R.string.msg_import_failed_file_not_exist, uri)
            return null
        }
        if (pfd == null) {
            postMessage(R.string.msg_export_failed_null_pfd, uri)
        }
        return pfd
    }

    private suspend fun <T> import(
        uri: Uri,
        typeDescription: String,
        progress: MutableStateFlow<ProgressData>,
        parseItem: (reader: JsonReader) -> T,
        importRecord: suspend (item: T, version: Int) -> Unit,
        initializeImport: suspend () -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val items: MutableList<T> = mutableListOf()
        var version = 0
        openFileToRead(uri)?.use { pfd ->
            val reader = JsonReader(InputStreamReader(FileInputStream(pfd.fileDescriptor), "UTF-8"))
            try {
                progress.start()
                var shouldContinue = true
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        items += parseItem(reader)
                    }
                    reader.endArray()
                } else {
                    reader.beginObject()
                    while (shouldContinue) {
                        when (reader.nextName()) {
                            NAME_TYPE -> shouldContinue = reader.nextString() == typeDescription
                            NAME_VERSION -> version = reader.nextInt()
                            NAME_ITEMS -> {
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    items += parseItem(reader)
                                }
                                reader.endArray()
                            }
                            else -> reader.skipValue()
                        }
                        if (reader.peek() == JsonToken.END_OBJECT) break
                    }
                    reader.endObject()
                }
                if (!shouldContinue) {
                    postMessage(R.string.msg_import_failed_wrong_type, typeDescription, "unknown")
                } else {
                    initializeImport()
                    progress.start(items.size)
                    items.forEachIndexed { index, item ->
                        progress.update(index)
                        importRecord(item, version)
                    }
                    postMessage(R.string.msg_import_success)
                }
            } catch (e: Exception) {
                Timber.e(e)
                postMessage(R.string.msg_import_failed_parse_json)
            } finally {
                progress.complete()
            }
        }
    }

    private fun MutableStateFlow<ProgressData>.start() {
        value = ProgressData(mode = ProgressData.Mode.INDETERMINATE)
    }

    private fun MutableStateFlow<ProgressData>.start(max: Int) {
        value = ProgressData(0, max, ProgressData.Mode.DETERMINATE)
    }

    private fun MutableStateFlow<ProgressData>.update(current: Int) {
        value = value.copy(current = current)
    }

    private fun MutableStateFlow<ProgressData>.complete() {
        value = value.copy(current = value.max, mode = ProgressData.Mode.OFF)
    }
}
