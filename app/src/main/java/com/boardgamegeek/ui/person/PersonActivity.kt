package com.boardgamegeek.ui.person

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTask
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.forums.ForumsViewModel
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.PersonRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

object PersonActivity {
    fun startForArtist(context: Context, id: Int, name: String) {
        context.startActivity(createIntent(context, id, name, PersonType.ARTIST))
    }

    fun startForDesigner(context: Context, id: Int, name: String) {
        context.startActivity(createIntent(context, id, name, PersonType.DESIGNER))
    }

    fun startForPublisher(context: Context, id: Int, name: String) {
        context.startActivity(createIntent(context, id, name, PersonType.PUBLISHER))
    }

    fun startUpForArtist(context: Context, id: Int, name: String) {
        context.startActivity(createIntent(context, id, name, PersonType.ARTIST, replaceBackStack = true).clearTask().clearTop())
    }

    fun startUpForDesigner(context: Context, id: Int, name: String) {
        context.startActivity(createIntent(context, id, name, PersonType.DESIGNER, replaceBackStack = true).clearTask().clearTop())
    }

    fun startUpForPublisher(context: Context, id: Int, name: String) {
        context.startActivity(createIntent(context, id, name, PersonType.PUBLISHER, replaceBackStack = true).clearTask().clearTop())
    }

    private fun createIntent(
        context: Context,
        id: Int,
        name: String,
        personType: PersonType,
        replaceBackStack: Boolean = false,
    ) = MainActivity.createIntent(
        context = context,
        route = PersonRoute(
            personId = id,
            personName = name,
            personType = personType.name,
        ),
        replaceBackStack = replaceBackStack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonRouteScreen(
    route: PersonRoute,
    viewModel: PersonViewModel = hiltViewModel(),
    forumsViewModel: ForumsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val personType = remember(route.personType) { route.personType.asPersonType() }
    val details by viewModel.details.collectAsStateWithLifecycle()

    LaunchedEffect(personType, route.personId) {
        viewModel.setPerson(personType, route.personId)
        if (route.personId != BggContract.INVALID_ID) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Person")
                param(FirebaseAnalytics.Param.ITEM_ID, route.personId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, route.personName)
            }
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(details?.data?.name?.ifBlank { route.personName } ?: route.personName) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { context.viewPersonOnBgg(personType, route.personId) }) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(R.string.menu_view),
                            )
                        }
                        IconButton(onClick = viewModel::refresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.menu_refresh),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            PersonScreen(
                personType = personType,
                personId = route.personId,
                personName = route.personName,
                viewModel = viewModel,
                forumsViewModel = forumsViewModel,
                paddingValues = paddingValues,
            )
        }
    }
}

private fun String.asPersonType(): PersonType = PersonType.entries.firstOrNull { it.name == this } ?: PersonType.DESIGNER

private fun Context.viewPersonOnBgg(personType: PersonType, personId: Int) {
    @Suppress("SpellCheckingInspection")
    val path = when (personType) {
        PersonType.DESIGNER -> "boardgamedesigner"
        PersonType.ARTIST -> "boardgameartist"
        PersonType.PUBLISHER -> "boardgamepublisher"
    }
    linkToBgg(path, personId)
}
