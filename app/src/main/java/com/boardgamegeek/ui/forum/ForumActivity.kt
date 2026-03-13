package com.boardgamegeek.ui.forum

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.model.Forum
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.forums.ForumsActivity.Companion.startUp
import com.boardgamegeek.ui.game.GameActivity.Companion.startUp
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForArtist
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForDesigner
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForPublisher
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForumActivity : ComponentActivity() {
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION
    private val viewModel by viewModels<ForumViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Forum")
                param(FirebaseAnalytics.Param.ITEM_ID, forumId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, forumTitle)
            }
        }

        setContent {
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                ForumTitle(objectName = objectName, forumTitle = forumTitle)
                            },
                            navigationIcon = {
                                IconButton(onClick = ::navigateUp) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { linkToBgg("forum/$forumId") }) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = stringResource(R.string.menu_view)
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        ForumScreen(
                            viewModel = viewModel,
                            forumId = forumId,
                            forumTitle = forumTitle,
                            objectId = objectId,
                            objectName = objectName,
                            objectType = objectType,
                        )
                    }
                }
            }
        }
    }

    private fun readIntent() {
        forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID)
        forumTitle = intent.getStringExtra(KEY_FORUM_TITLE).orEmpty()
        objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
        objectType = intent.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
        objectName = intent.getStringExtra(KEY_OBJECT_NAME).orEmpty()
    }

    private fun navigateUp() {
        when (objectType) {
            Forum.Type.REGION -> startUp(this)
            Forum.Type.GAME -> startUp(this, objectId, objectName)
            Forum.Type.ARTIST -> startUpForArtist(this, objectId, objectName)
            Forum.Type.DESIGNER -> startUpForDesigner(this, objectId, objectName)
            Forum.Type.PUBLISHER -> startUpForPublisher(this, objectId, objectName)
        }
        finish()
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"

        fun start(context: Context, forumId: Int, forumTitle: String, objectId: Int, objectName: String, objectType: Forum.Type) {
            context.startActivity(createIntent(context, forumId, forumTitle, objectId, objectName, objectType))
        }

        fun startUp(context: Context, forumId: Int, forumTitle: String, objectId: Int, objectName: String, objectType: Forum.Type) {
            context.startActivity(createIntent(context, forumId, forumTitle, objectId, objectName, objectType).clearTop())
        }

        private fun createIntent(
            context: Context,
            forumId: Int,
            forumTitle: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type
        ): Intent {
            return context.intentFor<ForumActivity>(
                KEY_FORUM_ID to forumId,
                KEY_FORUM_TITLE to forumTitle,
                KEY_OBJECT_ID to objectId,
                KEY_OBJECT_NAME to objectName,
                KEY_OBJECT_TYPE to objectType,
            )
        }
    }
}

@Composable
private fun ForumTitle(objectName: String, forumTitle: String) {
    if (objectName.isNotBlank()) {
        Column {
            Text(text = objectName)
            Text(text = forumTitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    } else {
        Text(text = forumTitle)
    }
}
