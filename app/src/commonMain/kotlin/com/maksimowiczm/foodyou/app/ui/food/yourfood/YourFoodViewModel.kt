package com.maksimowiczm.foodyou.app.ui.food.yourfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.search.searchQuery
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.usecase.DeleteFoodUseCase
import com.maksimowiczm.foodyou.food.search.domain.FoodSearch
import com.maksimowiczm.foodyou.food.search.domain.FoodSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

internal class YourFoodViewModel(
    private val foodSearchRepository: FoodSearchRepository,
    private val deleteFoodUseCase: DeleteFoodUseCase,
) : ViewModel() {
    // Uses FoodSearchRepository directly (rather than FoodSearchUseCase) so that live,
    // as-you-type filtering here doesn't publish FoodSearchEvents into the shared "recent
    // searches" history consumed by the Add Food screen.
    private val query = MutableStateFlow<String?>(null)

    val items: Flow<PagingData<FoodSearch>> =
        query
            .flatMapLatest { q ->
                foodSearchRepository.search(
                    query = searchQuery(q),
                    source = FoodSource.Type.User,
                    config = PagingConfig(pageSize = PAGE_SIZE),
                    remoteMediatorFactory = null,
                    excludedRecipeId = null,
                )
            }
            .cachedIn(viewModelScope)

    private val _selectedIds = MutableStateFlow<Set<FoodId>>(emptySet())
    val selectedIds: StateFlow<Set<FoodId>> = _selectedIds.asStateFlow()

    fun search(query: String?) {
        this.query.value = query
    }

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

    private companion object {
        const val PAGE_SIZE = 30
    }
}
