package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.ui.collection.CollectionLauncher
import com.boardgamegeek.ui.collection.CollectionViewModel
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface CollectionFilterViewModelBridge {
    fun addFilter(filter: CollectionFilterer)
    fun removeFilter(type: Int)
    fun setSort(sortType: Int)
    fun observeEffectiveFilters(owner: LifecycleOwner, onFilters: (List<CollectionFilterer>) -> Unit)
    fun observeAcquiredFrom(owner: LifecycleOwner, onValues: (List<String>) -> Unit)
    fun observeInventoryLocation(owner: LifecycleOwner, onValues: (List<String>) -> Unit)
}

fun FragmentActivity.collectionFilterViewModelBridge(): CollectionFilterViewModelBridge {
    return if (this is CollectionLauncher) {
        val viewModel = ViewModelProvider(this)[CollectionViewModel::class.java]
        ComposeCollectionFilterViewModelBridge(viewModel)
    } else {
        val viewModel = ViewModelProvider(this)[CollectionViewViewModel::class.java]
        LegacyCollectionFilterViewModelBridge(viewModel)
    }
}

private class ComposeCollectionFilterViewModelBridge(
    private val viewModel: CollectionViewModel
) : CollectionFilterViewModelBridge {
    override fun addFilter(filter: CollectionFilterer) {
        viewModel.addFilter(filter)
    }

    override fun removeFilter(type: Int) {
        viewModel.removeFilter(type)
    }

    override fun setSort(sortType: Int) {
        viewModel.setSort(sortType)
    }

    override fun observeEffectiveFilters(owner: LifecycleOwner, onFilters: (List<CollectionFilterer>) -> Unit) {
        owner.lifecycleScope.launch {
            viewModel.effectiveFilters.collectLatest { onFilters(it) }
        }
    }

    override fun observeAcquiredFrom(owner: LifecycleOwner, onValues: (List<String>) -> Unit) {
        owner.lifecycleScope.launch {
            viewModel.acquiredFrom.collectLatest { onValues(it) }
        }
    }

    override fun observeInventoryLocation(owner: LifecycleOwner, onValues: (List<String>) -> Unit) {
        owner.lifecycleScope.launch {
            viewModel.inventoryLocation.collectLatest { onValues(it) }
        }
    }
}

private class LegacyCollectionFilterViewModelBridge(
    private val viewModel: CollectionViewViewModel
) : CollectionFilterViewModelBridge {
    override fun addFilter(filter: CollectionFilterer) {
        viewModel.addFilter(filter)
    }

    override fun removeFilter(type: Int) {
        viewModel.removeFilter(type)
    }

    override fun setSort(sortType: Int) {
        viewModel.setSort(sortType)
    }

    override fun observeEffectiveFilters(owner: LifecycleOwner, onFilters: (List<CollectionFilterer>) -> Unit) {
        viewModel.effectiveFilters.observe(owner) { onFilters(it.orEmpty()) }
    }

    override fun observeAcquiredFrom(owner: LifecycleOwner, onValues: (List<String>) -> Unit) {
        viewModel.acquiredFrom.observe(owner) { onValues(it.orEmpty()) }
    }

    override fun observeInventoryLocation(owner: LifecycleOwner, onValues: (List<String>) -> Unit) {
        viewModel.inventoryLocation.observe(owner) { onValues(it.orEmpty()) }
    }
}
