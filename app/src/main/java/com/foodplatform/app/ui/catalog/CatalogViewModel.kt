package com.foodplatform.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.remote.ProductDto
import com.foodplatform.app.data.repository.ProductRepository
import com.foodplatform.app.data.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    object Loading : CatalogUiState()
    data class Success(
        val products: List<ProductDto>,
        val isNextPageLoading: Boolean = false,
        val isEndReached: Boolean = false
    ) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var isFetching = false

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        if (isFetching) return

        val currentState = _uiState.value
        if (currentState is CatalogUiState.Success && currentState.isEndReached) return

        isFetching = true
        val currentProducts = if (currentState is CatalogUiState.Success) currentState.products else emptyList()

        if (currentProducts.isNotEmpty()) {
            _uiState.value = CatalogUiState.Success(currentProducts, isNextPageLoading = true, isEndReached = false)
        } else {
            _uiState.value = CatalogUiState.Loading
        }

        viewModelScope.launch {
            when (val result = repository.getProducts(page = currentPage)) {
                is Resource.Success -> {
                    val newProducts = result.data.items
                    val isEndReached = currentPage >= result.data.meta.totalPages
                    
                    _uiState.value = CatalogUiState.Success(
                        products = currentProducts + newProducts,
                        isNextPageLoading = false,
                        isEndReached = isEndReached
                    )
                    
                    if (!isEndReached) {
                        currentPage++
                    }
                }
                is Resource.Error -> {
                    if (currentProducts.isEmpty()) {
                        _uiState.value = CatalogUiState.Error(result.message)
                    } else {
                        // Keep showing list but clear loading state if we fail to fetch next page
                        _uiState.value = CatalogUiState.Success(
                            products = currentProducts,
                            isNextPageLoading = false,
                            isEndReached = false
                        )
                    }
                }
            }
            isFetching = false
        }
    }

    fun retry() {
        if (_uiState.value is CatalogUiState.Error) {
            currentPage = 1
            loadNextPage()
        }
    }
}

class CatalogViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CatalogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
