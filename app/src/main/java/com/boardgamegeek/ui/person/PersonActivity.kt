package com.boardgamegeek.ui.person

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTask
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.artists.ArtistsActivity
import com.boardgamegeek.ui.designers.DesignersActivity
import com.boardgamegeek.ui.forums.ForumsViewModel
import com.boardgamegeek.ui.publishers.PublishersActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class PersonActivity : ComponentActivity() {
    private var id = BggContract.INVALID_ID
    private var name = ""
    private var personType = PersonType.DESIGNER

    private val viewModel by viewModels<PersonViewModel>()
    private val forumsViewModel by viewModels<ForumsViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = intent.getIntExtra(KEY_PERSON_ID, BggContract.INVALID_ID)
        name = intent.getStringExtra(KEY_PERSON_NAME).orEmpty()
        personType = intent.getSerializableExtra(KEY_PERSON_TYPE) as? PersonType ?: PersonType.DESIGNER

        viewModel.setPerson(personType, id)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Person")
                param(FirebaseAnalytics.Param.ITEM_ID, id.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, name)
            }
        }

        setContent {
            val details by viewModel.details.collectAsStateWithLifecycle()
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(details?.data?.name?.ifBlank { name } ?: name) },
                            navigationIcon = {
                                IconButton(onClick = { goUp() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back),
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = ::viewOnBgg) {
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
                        personId = id,
                        personName = name,
                        viewModel = viewModel,
                        forumsViewModel = forumsViewModel,
                        paddingValues = paddingValues,
                    )
                }
            }
        }
    }

    private fun viewOnBgg() {
        @Suppress("SpellCheckingInspection")
        val path = when (personType) {
            PersonType.DESIGNER -> "boardgamedesigner"
            PersonType.ARTIST -> "boardgameartist"
            PersonType.PUBLISHER -> "boardgamepublisher"
        }
        linkToBgg(path, id)
    }

    private fun goUp() {
        when (personType) {
            PersonType.DESIGNER -> startActivity(intentFor<DesignersActivity>().clearTop())
            PersonType.ARTIST -> startActivity(intentFor<ArtistsActivity>().clearTop())
            PersonType.PUBLISHER -> startActivity(intentFor<PublishersActivity>().clearTop())
        }
        finish()
    }

    companion object {
        private const val KEY_PERSON_TYPE = "PERSON_TYPE"
        private const val KEY_PERSON_ID = "PERSON_ID"
        private const val KEY_PERSON_NAME = "PERSON_NAME"

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
            context.startActivity(createIntent(context, id, name, PersonType.ARTIST).clearTask().clearTop())
        }

        fun startUpForDesigner(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, PersonType.DESIGNER).clearTask().clearTop())
        }

        fun startUpForPublisher(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, PersonType.PUBLISHER).clearTask().clearTop())
        }

        private fun createIntent(context: Context, id: Int, name: String, personType: PersonType): Intent {
            return context.intentFor<PersonActivity>(
                KEY_PERSON_ID to id,
                KEY_PERSON_NAME to name,
                KEY_PERSON_TYPE to personType,
            )
        }
    }
}
