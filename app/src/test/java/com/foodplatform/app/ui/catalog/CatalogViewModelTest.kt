package com.foodplatform.app.ui.catalog

import com.foodplatform.app.data.remote.PaginatedResponse
import com.foodplatform.app.data.remote.PaginationMeta
import com.foodplatform.app.data.remote.ProductApi
import com.foodplatform.app.data.remote.ProductDto
import com.foodplatform.app.data.remote.ProductType
import com.foodplatform.app.data.remote.CategoryApi
import com.foodplatform.app.data.remote.CategoryDto
import com.foodplatform.app.data.repository.CategoryRepository
import com.foodplatform.app.data.repository.ProductRepository
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private lateinit var fakeApi: FakeProductApi
    private lateinit var repository: ProductRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: CatalogViewModel
    private val testDispatcher = StandardTestDispatcher()

    class FakeProductApi : ProductApi {
        var page1Response: PaginatedResponse<ProductDto>? = null
        var page2Response: PaginatedResponse<ProductDto>? = null
        var lastSearchQuery: String? = null

        override suspend fun getProducts(page: Int?, limit: Int?, categoryId: String?, type: String?, search: String?): PaginatedResponse<ProductDto> {
            lastSearchQuery = search
            return if (page == 1) page1Response!! else page2Response!!
        }

        override suspend fun getProductById(id: String): ProductDto {
            throw NotImplementedError()
        }
    }

    class FakeCategoryApi : CategoryApi {
        override suspend fun getCategories(): Response<List<CategoryDto>> = Response.success(emptyList())
    }

    class FakeCategoryRepository : CategoryRepository(FakeCategoryApi()) {
        var mockCategoriesResult: Result<List<CategoryDto>> = Result.success(emptyList())
        override suspend fun getCategories(): Result<List<CategoryDto>> = mockCategoriesResult
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeProductApi()
        repository = ProductRepository(fakeApi)
        categoryRepository = FakeCategoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads first page`() = runTest(testDispatcher) {
        val dummyProducts = listOf(
            ProductDto("1", "Burger", "Tasty", ProductType.COOKED_FOOD, 5.99, true)
        )
        fakeApi.page1Response = PaginatedResponse(dummyProducts, PaginationMeta(1, 1, 20, 1))

        viewModel = CatalogViewModel(repository, categoryRepository)
        
        // Wait for coroutines to complete
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CatalogUiState.Success)
        val successState = state as CatalogUiState.Success
        assertEquals(1, successState.products.size)
        assertTrue(successState.isEndReached)
    }

    @Test
    fun `loadNextPage appends products`() = runTest(testDispatcher) {
        val page1 = listOf(ProductDto("1", "Burger", "Tasty", ProductType.COOKED_FOOD, 5.99, true))
        val page2 = listOf(ProductDto("2", "Pizza", "Cheesy", ProductType.COOKED_FOOD, 12.99, true))
        
        fakeApi.page1Response = PaginatedResponse(page1, PaginationMeta(2, 1, 1, 2))
        fakeApi.page2Response = PaginatedResponse(page2, PaginationMeta(2, 2, 1, 2))

        viewModel = CatalogViewModel(repository, categoryRepository)
        advanceUntilIdle()
        
        // Trigger next page
        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CatalogUiState.Success)
        val successState = state as CatalogUiState.Success
        assertEquals(2, successState.products.size)
        assertTrue(successState.isEndReached)
    }

    @Test
    fun `updateSearchQuery resets page and passes search query to api`() = runTest(testDispatcher) {
        val initialProducts = listOf(ProductDto("1", "Burger", "Tasty", ProductType.COOKED_FOOD, 5.99, true))
        val searchResult = listOf(ProductDto("2", "Pizza Margherita", "Delicious", ProductType.COOKED_FOOD, 10.99, true))

        fakeApi.page1Response = PaginatedResponse(initialProducts, PaginationMeta(1, 1, 20, 1))

        viewModel = CatalogViewModel(repository, categoryRepository)
        advanceUntilIdle()

        assertEquals(null, fakeApi.lastSearchQuery)

        fakeApi.page1Response = PaginatedResponse(searchResult, PaginationMeta(1, 1, 20, 1))
        viewModel.updateSearchQuery("Pizza")
        advanceUntilIdle()

        assertEquals("Pizza", fakeApi.lastSearchQuery)
        val state = viewModel.uiState.value
        assertTrue(state is CatalogUiState.Success)
        val successState = state as CatalogUiState.Success
        assertEquals(1, successState.products.size)
        assertEquals("Pizza Margherita", successState.products[0].name)
        assertEquals("Pizza", successState.searchQuery)
    }
}
