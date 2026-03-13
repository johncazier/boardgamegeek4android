package com.boardgamegeek.ui.forum

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.model.Forum
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForumFragment : Fragment() {
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION

    private val viewModel by activityViewModels<ForumViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            forumId = it.getInt(KEY_FORUM_ID, BggContract.INVALID_ID)
            forumTitle = it.getString(KEY_FORUM_TITLE).orEmpty()
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME).orEmpty()
            objectType = it.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
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

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"

        fun newInstance(forumId: Int, forumTitle: String?, objectId: Int, objectName: String?, objectType: Forum.Type?): ForumFragment {
            return ForumFragment().apply {
                arguments = bundleOf(
                    KEY_FORUM_ID to forumId,
                    KEY_FORUM_TITLE to forumTitle,
                    KEY_OBJECT_ID to objectId,
                    KEY_OBJECT_NAME to objectName,
                    KEY_OBJECT_TYPE to objectType,
                )
            }
        }
    }
}
