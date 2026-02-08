package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.boardgamegeek.databinding.DialogCollectionFilterBinding
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class CollectionFilterDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogCollectionFilterBinding? = null
    private val binding get() = _binding!!
    private val filters = mutableListOf<CollectionFilterer>()
    private val factory by lazy { CollectionFiltererFactory(requireContext()) }
    private val viewModelBridge by lazy { requireActivity().collectionFilterViewModelBridge() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogCollectionFilterBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModelBridge.observeEffectiveFilters(viewLifecycleOwner) { effectiveFilters ->
            filters.clear()
            filters.addAll(effectiveFilters)
            bindUi()
        }
    }

    private fun bindUi() {
        binding.container.children.filterIsInstance<ChipGroup>().forEach {
            it.children.filterIsInstance<Chip>().forEach { chip ->
                val type = getTypeFromViewTag(chip)
                if (filters.map { filter -> filter.type }.contains(type)) {
                    chip.isChecked = true
                    chip.isCloseIconVisible = true
                }

                factory.create(type)?.let { filter ->
                    chip.checkedIcon = ContextCompat.getDrawable(requireContext(), filter.iconResourceId)
                }

                chip.setOnClickListener {
                    if (type != CollectionFiltererFactory.TYPE_UNKNOWN) {
                        Timber.d("Filter by %s", type)
                        CollectionFilterDialogFactory().create(requireContext(), type)?.let { dialog ->
                            dialog.createDialog(requireActivity(), filters.find { filter -> filter.type == type })
                        } ?: Timber.w("Couldn't find a filter dialog of type %s", type)
                    } else {
                        Timber.w("Invalid filter type selected: %s", type)
                    }
                    dismiss()
                }

                chip.setOnCloseIconClickListener {
                    viewModelBridge.removeFilter(type)
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getTypeFromViewTag(it: View) = it.tag?.toString()?.toIntOrNull() ?: CollectionFiltererFactory.TYPE_UNKNOWN
}
