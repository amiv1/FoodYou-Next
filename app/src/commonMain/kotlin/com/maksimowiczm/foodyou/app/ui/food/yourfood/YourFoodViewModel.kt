package com.maksimowiczm.foodyou.app.ui.food.yourfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.usecase.DeleteFoodUseCase
import com.maksimowiczm.foodyou.food.search.domain.FoodSearch
import com.maksimowiczm.foodyou.food.search.domain.FoodSearchUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class YourFoodViewModel(
    private val foodSearchUseCase: FoodSearchUseCase,
    private val deleteFoodUseCase: DeleteFoodUseCase,
) : ViewModel() {
    val items: Flow<PagingData<FoodSearch>> =
        foodSearchUseCase
            .search(query = null, source = FoodSource.Type.User, excludedRecipeId = null)
            .cachedIn(viewModelScope)

    private val _selectedIds = MutableStateFlow<Set<FoodId>>(emptySet())
    val selectedIds: StateFlow<Set<FoodId>> = _selectedIds.asStateFlow()

    fun toggleSelection(id: FoodId) {
        _selectedIds.value =
            if (id in _selectedIds.value) {
                _selectedIds.value - id
            } else {
                _selectedIds.value + id
            }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAllLoaded(ids: List<FoodId>) {
        _selectedIds.value = _selectedIds.value + ids
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) {
            return
        }

        viewModelScope.launch {
            deleteFoodUseCase.deleteAll(ids)
            clearSelection()
        }
    }
}
