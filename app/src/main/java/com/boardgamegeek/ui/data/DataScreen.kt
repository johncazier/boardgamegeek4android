package com.boardgamegeek.ui.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.export.Constants
import com.boardgamegeek.extensions.toast
import com.boardgamegeek.livedata.ProgressData
import com.boardgamegeek.ui.widget.DataStepRow
import com.boardgamegeek.util.FileUtils
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import timber.log.Timber

@Composable
fun DataScreen(
    viewModel: DataPortViewModel,
    paddingValues: PaddingValues,
) {
    val context = LocalContext.current
    val collectionProgress by viewModel.collectionViewProgress.collectAsStateWithLifecycle()
    val gameProgress by viewModel.gameProgress.collectAsStateWithLifecycle()
    val userProgress by viewModel.userProgress.collectAsStateWithLifecycle()

    val exportCollectionViewsLauncher = rememberExportLauncher { uri ->
        doExport(context, uri) { viewModel.exportCollectionViews(it) }
    }
    val importCollectionViewsLauncher = rememberImportLauncher { uri ->
        doImport(context, uri) { viewModel.importCollectionViews(it) }
    }
    val exportGamesLauncher = rememberExportLauncher { uri ->
        doExport(context, uri) { viewModel.exportGames(it) }
    }
    val importGamesLauncher = rememberImportLauncher { uri ->
        doImport(context, uri) { viewModel.importGames(it) }
    }
    val exportUsersLauncher = rememberExportLauncher { uri ->
        doExport(context, uri) { viewModel.exportUsers(it) }
    }
    val importUsersLauncher = rememberImportLauncher { uri ->
        doImport(context, uri) { viewModel.importUsers(it) }
    }

    LaunchedEffect(viewModel) {
        viewModel.message.collect { message ->
            context.toast(message)
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
    ) {
        androidx.compose.material3.Text(
            text = stringResource(R.string.description_backup),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        DataStepRowView(
            titleRes = R.string.backup_type_collection_view,
            descriptionRes = R.string.backup_description_collection_view,
            progress = collectionProgress,
            onExport = { exportCollectionViewsLauncher.launch(Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION) },
            onImport = { importCollectionViewsLauncher.launch(null) }
        )

        DataStepRowView(
            titleRes = R.string.backup_type_game,
            descriptionRes = R.string.backup_description_game,
            progress = gameProgress,
            onExport = { exportGamesLauncher.launch(Constants.TYPE_GAMES_DESCRIPTION) },
            onImport = { importGamesLauncher.launch(null) }
        )

        DataStepRowView(
            titleRes = R.string.backup_type_user,
            descriptionRes = R.string.backup_description_user,
            progress = userProgress,
            onExport = { exportUsersLauncher.launch(Constants.TYPE_USERS_DESCRIPTION) },
            onImport = { importUsersLauncher.launch(null) }
        )
    }
}

@Composable
private fun DataStepRowView(
    titleRes: Int,
    descriptionRes: Int,
    progress: ProgressData,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            DataStepRow(ctx).apply {
                findViewById<TextView>(R.id.typeView).text = ctx.getString(titleRes)
                findViewById<TextView>(R.id.descriptionView).text = ctx.getString(descriptionRes)
            }
        },
        update = { view ->
            view.onExport { onExport() }
            view.onImport { onImport() }
            view.updateProgressBar(progress)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun rememberExportLauncher(onExport: (Uri?) -> Unit) =
    androidx.activity.compose.rememberLauncherForActivityResult(ExportFileContract()) { uri ->
        onExport(uri)
    }

@Composable
private fun rememberImportLauncher(onImport: (Uri?) -> Unit) =
    androidx.activity.compose.rememberLauncherForActivityResult(ImportFileContract()) { uri ->
        onImport(uri)
    }

private fun doExport(context: Context, uri: Uri?, export: (Uri) -> Unit) {
    uri?.let {
        tryUriPermission(context, it)
        export(it)
        logAction("Export")
    }
}

private fun doImport(context: Context, uri: Uri?, import: (Uri) -> Unit) {
    uri?.let {
        tryUriPermission(context, it)
        import(it)
        logAction("Import")
    }
}

private fun tryUriPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    } catch (e: SecurityException) {
        Timber.e(e, "Could not persist URI permissions for '%s'.", uri.toString())
    }
}

private fun logAction(action: String) {
    Firebase.analytics.logEvent("DataManagement") {
        param("Action", action)
    }
}

class ExportFileContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/*")
            .putExtra(Intent.EXTRA_TITLE, FileUtils.getExportFileName(input))
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

class ImportFileContract : ActivityResultContract<Unit?, Uri?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/*")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}
