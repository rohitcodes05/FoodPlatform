package com.foodplatform.app.ui.checkout

import com.foodplatform.app.data.remote.AddressApi
import com.foodplatform.app.data.remote.AddressDto
import com.foodplatform.app.data.remote.CreateAddressRequest
import com.foodplatform.app.data.remote.CreateOrderRequest
import com.foodplatform.app.data.remote.OrderApi
import com.foodplatform.app.data.remote.OrderDto
import com.foodplatform.app.data.remote.UpdateAddressRequest
import com.foodplatform.app.data.repository.AddressRepository
import com.foodplatform.app.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CheckoutViewModel

    class FakeAddressApi : AddressApi {
        override suspend fun getAddresses(): Response<List<AddressDto>> = Response.success(emptyList())
        override suspend fun createAddress(request: CreateAddressRequest): Response<AddressDto> = Response.success(AddressDto("1", "", "", "", "", ""))
        override suspend fun updateAddress(id: String, request: UpdateAddressRequest): Response<AddressDto> = Response.success(AddressDto("1", "", "", "", "", ""))
        override suspend fun deleteAddress(id: String): Response<AddressDto> = Response.success(AddressDto("1", "", "", "", "", ""))
    }

    class FakeOrderApi : OrderApi {
        override suspend fun createOrder(request: CreateOrderRequest): Response<OrderDto> = Response.success(OrderDto("1", "user", "1", "PENDING", "2026-08-18T10:00:00Z"))
        override suspend fun getOrders(): Response<List<OrderDto>> = Response.success(emptyList())
        override suspend fun getOrderById(id: String): Response<OrderDto> = Response.success(OrderDto("1", "user", "1", "PENDING", "2026-08-18T10:00:00Z"))
    }

    class FakeAddressRepository : AddressRepository(FakeAddressApi()) {
        var mockResult: Result<List<AddressDto>> = Result.success(emptyList())
        var mockUpdateResult: Result<AddressDto> = Result.success(AddressDto("1", "", "", "", "", ""))
        var mockDeleteResult: Result<AddressDto> = Result.success(AddressDto("1", "", "", "", "", ""))

        override suspend fun getAddresses(): Result<List<AddressDto>> = mockResult
        override suspend fun updateAddress(id: String, street: String, city: String, state: String, postalCode: String, country: String): Result<AddressDto> = mockUpdateResult
        override suspend fun deleteAddress(id: String): Result<AddressDto> = mockDeleteResult
    }

    class FakeOrderRepository : OrderRepository(FakeOrderApi()) {
        var mockResult: Result<OrderDto> = Result.success(OrderDto("1", "user", "100", "PENDING", "2026-08-18T10:00:00Z"))
        override suspend fun createOrder(addressId: String): Result<OrderDto> = mockResult
    }

    private lateinit var addressRepository: FakeAddressRepository
    private lateinit var orderRepository: FakeOrderRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addressRepository = FakeAddressRepository()
        orderRepository = FakeOrderRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAddresses success updates state with addresses and selects first`() = runTest {
        val addresses = listOf(AddressDto("1", "Street", "City", "State", "123", "Country"))
        addressRepository.mockResult = Result.success(addresses)

        viewModel = CheckoutViewModel(addressRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.Success)
        val successState = state as CheckoutUiState.Success
        assertEquals(1, successState.addresses.size)
        assertEquals("1", successState.selectedAddressId)
    }

    @Test
    fun `placeOrder without selection sets error`() = runTest {
        addressRepository.mockResult = Result.success(emptyList())

        viewModel = CheckoutViewModel(addressRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.placeOrder()
        assertEquals("Please select an address", viewModel.errorEvent.value)
    }

    @Test
    fun `placeOrder success sets success event`() = runTest {
        val addresses = listOf(AddressDto("1", "Street", "City", "State", "123", "Country"))
        addressRepository.mockResult = Result.success(addresses)
        orderRepository.mockResult = Result.success(OrderDto("order1", "user1", "100", "PENDING", "2026-08-18T10:00:00Z"))

        viewModel = CheckoutViewModel(addressRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.placeOrder()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.orderSuccessEvent.value)
        assertFalse(viewModel.isActionLoading.value)
    }

    @Test
    fun `placeOrder failure sets error event`() = runTest {
        val addresses = listOf(AddressDto("1", "Street", "City", "State", "123", "Country"))
        addressRepository.mockResult = Result.success(addresses)
        orderRepository.mockResult = Result.failure(Exception("Out of stock"))

        viewModel = CheckoutViewModel(addressRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.placeOrder()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Out of stock", viewModel.errorEvent.value)
        assertFalse(viewModel.orderSuccessEvent.value)
    }

    @Test
    fun `updateAddress success reloads addresses`() = runTest {
        val addresses = listOf(AddressDto("1", "Street", "City", "State", "123", "Country"))
        addressRepository.mockResult = Result.success(addresses)

        viewModel = CheckoutViewModel(addressRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        addressRepository.mockUpdateResult = Result.success(AddressDto("1", "Street 2", "City", "State", "123", "Country"))
        viewModel.updateAddress("1", "Street 2", "City", "State", "123", "Country")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.isActionLoading.value)
    }

    @Test
    fun `deleteAddress success removes selection and reloads addresses`() = runTest {
        val addresses = listOf(AddressDto("1", "Street", "City", "State", "123", "Country"))
        addressRepository.mockResult = Result.success(addresses)

        viewModel = CheckoutViewModel(addressRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should initially select the first address
        assertTrue((viewModel.uiState.value as CheckoutUiState.Success).selectedAddressId == "1")

        addressRepository.mockDeleteResult = Result.success(AddressDto("1", "Street", "City", "State", "123", "Country"))
        addressRepository.mockResult = Result.success(emptyList()) // on reload

        viewModel.deleteAddress("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.Success)
        val successState = state as CheckoutUiState.Success
        assertTrue(successState.selectedAddressId == null)
        assertTrue(successState.addresses.isEmpty())
    }
}
