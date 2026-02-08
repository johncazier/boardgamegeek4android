package com.boardgamegeek.ui.dialog

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.boardgamegeek.R
import com.boardgamegeek.filterer.InventoryLocationFilter
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter

class InventoryLocationFilterDialog : CollectionTextFilterDialog() {
    override val titleResId: Int
        get() = R.string.inventory_location

    override fun createFilter(context: Context) = InventoryLocationFilter(context)

    override fun createAdapter(viewModel: CollectionFilterViewModelBridge, activity: FragmentActivity) =
        AutoCompleteAdapter(activity).also { adapter ->
            viewModel.observeInventoryLocation(activity) { adapter.addData(it) }
        }
}
