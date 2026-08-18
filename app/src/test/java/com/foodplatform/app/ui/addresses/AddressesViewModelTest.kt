package com.foodplatform.app.ui.addresses

import com.foodplatform.app.data.remote.AddressApi
import com.foodplatform.app.data.remote.AddressDto
import com.foodplatform.app.data.remote.CreateAddressRequest
import com.foodplatform.app.data.remote.UpdateAddressRequest
import com.foodplatform.app.data.repository.AddressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AddressesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AddressesViewModel
    private lateinit var addressRepository: FakeAddressRepository

    class FakeAddressApi : AddressApi {
        override suspend fun getAddresses(): Response<List<AddressDto>> = Response.success(emptyList())
        override suspend fun createAddress(request: CreateAddressRequest): Response<AddressDto> = Response.success(AddressDto("1", "", "", "", "", ""))
        override suspend fun updateAddress(id: String, request: UpdateAddressRequest): Response<AddressDto> = Response.success(AddressDto("1", "", "", "", "", ""))
        override suspend fun deleteAddress(id: String): Response<AddressDto> = Response.success(AddressDto("1", "", "", "", "", ""))
    }

    class FakeAddressRepository : AddressRepository(FakeAddressApi()) {
        var mockResult: Result<List<AddressDto>> = Result.success(emptyList())
        var mockCreateResult: Result<AddressDto> = Result.success(AddressDto("1", "", "", "", "", ""))
        var mockUpdateResult: Result<AddressDto> = Result.success(AddressDto("1", "", "", "", "", ""))
        var mockDeleteResult: Result<AddressDto> = Result.success(AddressDto("1", "", "", "", "", ""))

        override suspend fun getAddresses(): Result<List<AddressDto>> = mockResult
        override suspend fun createAddress(street: String, city: String, state: String, postalCode: String, country: String): Result<AddressDto> = mockCreateResult
        override suspend fun updateAddress(id: String, street: String, city: String, state: String, postalCode: String, country: String): Result<AddressDto> = mockUpdateResult
        override suspend fun deleteAddress(id: String): Result<AddressDto> = mockDeleteResult
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addressRepository = FakeAddressRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAddresses success updates state with addresses`() = runTest {
        val addresses = listOf(AddressDto("1", "Street", "City", "State", "123", "Country"))
        addressRepository.mockResult = Result.success(addresses)

        viewModel = AddressesViewModel(addressRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AddressesUiState.Success)
        val successState = state as AddressesUiState.Success
        assertEquals(1, successState.addresses.size)
        assertEquals("1", successState.addresses[0].id)
    }

    @Test
    fun `loadAddresses failure updates state to error`() = runTest {
        addressRepository.mockResult = Result.failure(Exception("Network error"))

        viewModel = AddressesViewModel(addressRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AddressesUiState.Error)
        val errorState = state as AddressesUiState.Error
        assertEquals("Network error", errorState.message)
    }

    @Test
    fun `addAddress success sets message and reloads addresses`() = runTest {
        addressRepository.mockResult = Result.success(emptyList())
        viewModel = AddressesViewModel(addressRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val newAddress = AddressDto("2", "New St", "City", "State", "456", "Country")
        addressRepository.mockCreateResult = Result.success(newAddress)
        addressRepository.mockResult = Result.success(listOf(newAddress))

        viewModel.addAddress("New St", "City", "State", "456", "Country")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Address added successfully", viewModel.messageEvent.value)
        assertFalse(viewModel.isActionLoading.value)
        val state = viewModel.uiState.value
        assertTrue(state is AddressesUiState.Success)
        assertEquals(1, (state as AddressesUiState.Success).addresses.size)
    }

    @Test
    fun `updateAddress success sets message and reloads addresses`() = runTest {
        val initial = AddressDto("1", "Old St", "City", "State", "123", "Country")
        addressRepository.mockResult = Result.success(listOf(initial))
        viewModel = AddressesViewModel(addressRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = AddressDto("1", "Updated St", "City", "State", "123", "Country")
        addressRepository.mockUpdateResult = Result.success(updated)
        addressRepository.mockResult = Result.success(listOf(updated))

        viewModel.updateAddress("1", "Updated St", "City", "State", "123", "Country")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Address updated successfully", viewModel.messageEvent.value)
        assertFalse(viewModel.isActionLoading.value)
    }

    @Test
    fun `deleteAddress success sets message and reloads addresses`() = runTest {
        val initial = AddressDto("1", "Old St", "City", "State", "123", "Country")
        addressRepository.mockResult = Result.success(listOf(initial))
        viewModel = AddressesViewModel(addressRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        addressRepository.mockDeleteResult = Result.success(initial)
        addressRepository.mockResult = Result.success(emptyList())

        viewModel.deleteAddress("1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Address deleted successfully", viewModel.messageEvent.value)
        assertFalse(viewModel.isActionLoading.value)
        val state = viewModel.uiState.value
        assertTrue(state is AddressesUiState.Success)
        assertTrue((state as AddressesUiState.Success).addresses.isEmpty())
    }
}
