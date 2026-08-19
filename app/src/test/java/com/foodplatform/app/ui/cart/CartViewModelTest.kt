package com.foodplatform.app.ui.cart

import com.foodplatform.app.data.remote.CartDto
import com.foodplatform.app.data.remote.CartItemDto
import com.foodplatform.app.data.remote.ProductDto
import com.foodplatform.app.data.remote.ProductType
import com.foodplatform.app.data.repository.CartRepository
import com.foodplatform.app.data.repository.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    class FakeCartRepository : CartRepository(FakeCartApi()) {
        var mockCartResponse: Resource<CartDto> = Resource.Success(CartDto("1", "user1", emptyList()))
        var mockActionResponse: Resource<Unit> = Resource.Success(Unit)

        override suspend fun getCart(): Resource<CartDto> = mockCartResponse

        override suspend fun addItem(productId: String, quantity: Int, cutOptionId: String?, weightOptionId: String?): Resource<Unit> {
            if (mockActionResponse is Resource.Success) notifyCartUpdated()
            return mockActionResponse
        }

        override suspend fun updateItemQuantity(productId: String, quantity: Int): Resource<Unit> {
            if (mockActionResponse is Resource.Success) notifyCartUpdated()
            return mockActionResponse
        }

        override suspend fun removeItem(productId: String): Resource<Unit> {
            if (mockActionResponse is Resource.Success) notifyCartUpdated()
            return mockActionResponse
        }

        override suspend fun clearCart(): Resource<Unit> {
            if (mockActionResponse is Resource.Success) notifyCartUpdated()
            return mockActionResponse
        }
    }

    class FakeCartApi : com.foodplatform.app.data.remote.CartApi {
        override suspend fun getCart() = throw NotImplementedError()
        override suspend fun addItem(request: com.foodplatform.app.data.remote.AddCartItemRequest) = throw NotImplementedError()
        override suspend fun updateItemQuantity(productId: String, request: com.foodplatform.app.data.remote.UpdateCartItemRequest) = throw NotImplementedError()
        override suspend fun removeItem(productId: String) = throw NotImplementedError()
        override suspend fun clearCart() = throw NotImplementedError()
    }

    private lateinit var repository: FakeCartRepository
    private lateinit var viewModel: CartViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCartRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadCart_success_updatesUiStateToSuccess() = runTest {
        val mockProduct = ProductDto("p1", "Burger", "Desc", ProductType.COOKED_FOOD, 10.0, true)
        val mockCartItem = CartItemDto("item1", "cart1", "p1", null, null, 2.0, mockProduct, null, null)
        val mockCart = CartDto("cart1", "user1", listOf(mockCartItem))

        repository.mockCartResponse = Resource.Success(mockCart)

        viewModel = CartViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CartUiState.Success)
        assertEquals(mockCart, (state as CartUiState.Success).cart)

        val total = viewModel.calculateTotal(mockCart)
        assertEquals(20.0, total, 0.01)
    }

    @Test
    fun updateQuantity_success_refreshesCart() = runTest {
        val mockCartEmpty = CartDto("cart1", "user1", emptyList())
        repository.mockCartResponse = Resource.Success(mockCartEmpty)

        viewModel = CartViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.mockActionResponse = Resource.Success(Unit)

        val mockProduct = ProductDto("p1", "Burger", "Desc", ProductType.COOKED_FOOD, 10.0, true)
        val mockCartItem = CartItemDto("item1", "cart1", "p1", null, null, 3.0, mockProduct, null, null)
        val mockCartLoaded = CartDto("cart1", "user1", listOf(mockCartItem))

        repository.mockCartResponse = Resource.Success(mockCartLoaded)

        viewModel.updateQuantity("p1", 3)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CartUiState.Success)
        assertEquals(mockCartLoaded, (state as CartUiState.Success).cart)
    }
}
