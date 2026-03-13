package com.boardgamegeek.ui.forums

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForumsFragment : Fragment() {
    private var forumType = Forum.Type.REGION
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""

    private val viewModel by activityViewModels<ForumsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            forumType = it.getSerializable(KEY_TYPE) as? Forum.Type ?: Forum.Type.REGION
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME).orEmpty()
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
                    ForumsScreen(
                        viewModel = viewModel,
                        forumType = forumType,
                        objectId = objectId,
                        objectName = objectName
                    )
                }
            }
        }
    }

    companion object {
        private const val KEY_TYPE = "TYPE"
        private const val KEY_OBJECT_ID = "ID"
        private const val KEY_OBJECT_NAME = "NAME"

        fun newInstance(): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.REGION,
                    KEY_OBJECT_ID to BggContract.INVALID_ID,
                    KEY_OBJECT_NAME to "",
                )
            }
        }

        fun newInstanceForGame(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.GAME,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name,
                )
            }
        }

        fun newInstanceForArtist(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.ARTIST,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name,
                )
            }
        }

        fun newInstanceForDesigner(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.DESIGNER,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name,
                )
            }
        }

        fun newInstanceForPublisher(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.PUBLISHER,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name,
                )
            }
        }
    }
}
