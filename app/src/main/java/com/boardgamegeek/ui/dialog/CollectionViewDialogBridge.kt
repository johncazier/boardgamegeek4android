package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.ui.collection.CollectionActivity
import com.boardgamegeek.ui.collection.CollectionViewModel
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

interface CollectionViewDialogBridge {
    fun findViewId(name: String): Int
    fun insert(name: String, isDefault: Boolean)
    fun update(name: String, isDefault: Boolean)
    fun deleteView(viewId: Int, name: String)
    fun getViews(): List<CollectionView>
    fun getDefaultViewId(): Int
}

fun FragmentActivity.collectionViewDialogBridge(): CollectionViewDialogBridge {
    return if (this is CollectionActivity) {
        val viewModel = ViewModelProvider(this)[CollectionViewModel::class.java]
        ComposeCollectionViewDialogBridge(viewModel)
    } else {
        val viewModel = ViewModelProvider(this)[CollectionViewViewModel::class.java]
        LegacyCollectionViewDialogBridge(viewModel)
    }
}

private class ComposeCollectionViewDialogBridge(
    private val viewModel: CollectionViewModel
) : CollectionViewDialogBridge {
    override fun findViewId(name: String) = viewModel.findViewId(name)

    override fun insert(name: String, isDefault: Boolean) {
        viewModel.insert(name, isDefault)
    }

    override fun update(name: String, isDefault: Boolean) {
        viewModel.update(name, isDefault)
    }

    override fun deleteView(viewId: Int, name: String) {
        viewModel.deleteView(viewId, name)
    }

    override fun getViews(): List<CollectionView> = viewModel.views.value

    override fun getDefaultViewId(): Int = viewModel.defaultViewIdFlow.value
}

private class LegacyCollectionViewDialogBridge(
    private val viewModel: CollectionViewViewModel
) : CollectionViewDialogBridge {
    override fun findViewId(name: String) = viewModel.findViewId(name)

    override fun insert(name: String, isDefault: Boolean) {
        viewModel.insert(name, isDefault)
    }

    override fun update(name: String, isDefault: Boolean) {
        viewModel.update(name, isDefault)
    }

    override fun deleteView(viewId: Int, name: String) {
        viewModel.deleteView(viewId, name)
    }

    override fun getViews(): List<CollectionView> = viewModel.views.value.orEmpty()

    override fun getDefaultViewId(): Int = viewModel.defaultViewId.value ?: -1
}
