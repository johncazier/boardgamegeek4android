package com.boardgamegeek.ui.thread

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.model.Forum
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.set
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class ThreadFragment : Fragment() {
    private var threadId = BggContract.INVALID_ID
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION

    private var articleCount = 0
    private var latestArticleId: Int = INVALID_ARTICLE_ID

    private val viewModel by activityViewModels<ThreadViewModel>()
    private val listState = LazyListState()
    private val scrollCommands = MutableSharedFlow<ThreadScrollCommand>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            threadId = it.getInt(KEY_THREAD_ID, BggContract.INVALID_ID)
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
                    ThreadScreen(
                        viewModel = viewModel,
                        threadId = threadId,
                        forumId = forumId,
                        forumTitle = forumTitle,
                        objectId = objectId,
                        objectName = objectName,
                        objectType = objectType,
                        listState = listState,
                        scrollCommands = scrollCommands,
                        onLatestArticleSeen = ::updateLatestArticle,
                        onArticleCountChanged = ::updateArticleCount
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.thread, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.menu_scroll_last)?.isVisible = latestArticleId != INVALID_ARTICLE_ID && articleCount > 0
                menu.findItem(R.id.menu_scroll_bottom)?.isVisible = articleCount > 0
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_scroll_last -> scrollToLatestArticle()
                    R.id.menu_scroll_bottom -> scrollToBottom()
                    else -> return false
                }
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        latestArticleId = requireContext().preferences()[getThreadKey(threadId), INVALID_ARTICLE_ID] ?: INVALID_ARTICLE_ID
    }

    override fun onPause() {
        super.onPause()
        if (latestArticleId != INVALID_ARTICLE_ID) {
            requireContext().preferences()[getThreadKey(threadId)] = latestArticleId
        }
    }

    private fun updateLatestArticle(articleId: Int) {
        if (articleId > latestArticleId) {
            latestArticleId = articleId
            activity?.invalidateOptionsMenu()
        }
    }

    private fun updateArticleCount(count: Int) {
        if (articleCount != count) {
            articleCount = count
            activity?.invalidateOptionsMenu()
        }
    }

    private fun getThreadKey(threadId: Int): String {
        return "THREAD-$threadId"
    }

    private fun scrollToLatestArticle() {
        if (latestArticleId != INVALID_ARTICLE_ID) {
            scrollCommands.tryEmit(ThreadScrollCommand.ScrollToLatest(latestArticleId))
        }
    }

    private fun scrollToBottom() {
        scrollCommands.tryEmit(ThreadScrollCommand.ScrollToBottom)
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_THREAD_ID = "THREAD_ID"
        private const val INVALID_ARTICLE_ID = -1

        fun newInstance(
            threadId: Int,
            forumId: Int,
            forumTitle: String?,
            objectId: Int,
            objectName: String?,
            objectType: Forum.Type?,
        ): ThreadFragment {
            return ThreadFragment().apply {
                arguments = bundleOf(
                    KEY_THREAD_ID to threadId,
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
