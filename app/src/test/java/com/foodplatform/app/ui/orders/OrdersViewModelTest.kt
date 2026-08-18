package com.foodplatform.app.ui.orders

import com.foodplatform.app.data.remote.OrderApi
import com.foodplatform.app.data.remote.CreateOrderRequest
import com.foodplatform.app.data.remote.OrderDto
import com.foodplatform.app.data.remote.ReviewApi
import com.foodplatform.app.data.remote.ReviewDto
import com.foodplatform.app.data.remote.CreateReviewRequest
import com.foodplatform.app.data.remote.UpdateReviewRequest
import com.foodplatform.app.data.repository.OrderRepository
import com.foodplatform.app.data.repository.ReviewRepository
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
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: OrdersViewModel
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var reviewRepository: ReviewRepository

    class FakeOrderApi : OrderApi {
        override suspend fun createOrder(request: CreateOrderRequest): Response<OrderDto> = Response.success(OrderDto("1", "user", "1", "PENDING", "2026-08-18T10:00:00Z"))
        override suspend fun getOrders(): Response<List<OrderDto>> = Response.success(emptyList())
        override suspend fun getOrderById(id: String): Response<OrderDto> = Response.success(OrderDto("1", "user", "1", "PENDING", "2026-08-18T10:00:00Z"))
    }

    class FakeOrderRepository : OrderRepository(FakeOrderApi()) {
        var mockOrdersResult: Result<List<OrderDto>> = Result.success(emptyList())
        var mockOrderByIdResult: Result<OrderDto> = Result.success(OrderDto("1", "user", "1", "PENDING", "2026-08-18T10:00:00Z"))

        override suspend fun getOrders(): Result<List<OrderDto>> = mockOrdersResult
        override suspend fun getOrderById(id: String): Result<OrderDto> = mockOrderByIdResult
    }

    class FakeReviewApi : ReviewApi {
        override suspend fun getReviewsByProduct(productId: String): Response<List<ReviewDto>> = Response.success(emptyList())
        override suspend fun createReview(request: CreateReviewRequest): Response<Unit> = Response.success(Unit)
        override suspend fun updateReview(id: String, request: UpdateReviewRequest): Response<Unit> = Response.success(Unit)
        override suspend fun deleteReview(id: String): Response<Unit> = Response.success(Unit)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        orderRepository = FakeOrderRepository()
        reviewRepository = ReviewRepository(FakeReviewApi())
        viewModel = OrdersViewModel(orderRepository, reviewRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadOrders success updates uiState to Success`() = runTest {
        val orders = listOf(OrderDto("1", "user", "100", "PENDING", "2026-08-18T10:00:00Z"))
        orderRepository.mockOrdersResult = Result.success(orders)

        viewModel.loadOrders()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is OrdersUiState.Success)
        assertEquals(1, (state as OrdersUiState.Success).orders.size)
    }

    @Test
    fun `loadOrders failure updates uiState to Error`() = runTest {
        orderRepository.mockOrdersResult = Result.failure(Exception("Network error"))

        viewModel.loadOrders()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is OrdersUiState.Error)
        assertEquals("Network error", (state as OrdersUiState.Error).message)
    }

    @Test
    fun `loadOrderDetails success updates detailsUiState to Success`() = runTest {
        val order = OrderDto("1", "user", "100", "PENDING", "2026-08-18T10:00:00Z")
        orderRepository.mockOrderByIdResult = Result.success(order)

        viewModel.loadOrderDetails("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.detailsUiState.value
        assertTrue(state is OrderDetailsUiState.Success)
        assertEquals("1", (state as OrderDetailsUiState.Success).order.id)
    }

    @Test
    fun `loadOrderDetails failure updates detailsUiState to Error`() = runTest {
        orderRepository.mockOrderByIdResult = Result.failure(Exception("Not found"))

        viewModel.loadOrderDetails("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.detailsUiState.value
        assertTrue(state is OrderDetailsUiState.Error)
        assertEquals("Not found", (state as OrderDetailsUiState.Error).message)
    }
}
