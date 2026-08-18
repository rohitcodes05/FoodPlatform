package com.foodplatform.app.ui.profile

import com.foodplatform.app.data.local.SecureTokenStorage
import com.foodplatform.app.data.remote.AuthApi
import com.foodplatform.app.data.remote.LoginRequest
import com.foodplatform.app.data.remote.RegisterRequest
import com.foodplatform.app.data.remote.UserResponse
import com.foodplatform.app.data.repository.AuthRepository
import com.foodplatform.app.data.repository.AuthResult
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ProfileViewModel
    private lateinit var authRepository: FakeAuthRepository

    class FakeAuthApi : AuthApi {
        override suspend fun register(request: RegisterRequest): Response<UserResponse> = Response.success(null)
        override suspend fun login(request: LoginRequest): Response<com.foodplatform.app.data.remote.LoginResponse> = Response.success(null)
        override suspend fun getMe(): Response<UserResponse> = Response.success(null)
        override suspend fun updateProfile(request: com.foodplatform.app.data.remote.UpdateProfileRequest): Response<UserResponse> = Response.success(null)
    }

    class FakeTokenStorage : SecureTokenStorage {
        var savedToken: String? = "fake_token"
        override fun saveToken(token: String) { savedToken = token }
        override fun getToken(): String? = savedToken
        override fun clearToken() { savedToken = null }
    }

    class FakeAuthRepository(
        api: AuthApi,
        private val tokenStorage: SecureTokenStorage
    ) : AuthRepository(api, tokenStorage) {
        
        var mockCurrentUserResult: AuthResult<UserResponse> = AuthResult.Error("Not initialized")
        var isLogoutCalled = false

        override suspend fun getCurrentUser(): AuthResult<UserResponse> = mockCurrentUserResult

        override fun logout() {
            tokenStorage.clearToken()
            isLogoutCalled = true
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val fakeApi = FakeAuthApi()
        val fakeStorage = FakeTokenStorage()
        authRepository = FakeAuthRepository(fakeApi, fakeStorage)
        viewModel = ProfileViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProfile success updates uiState to Success`() = runTest {
        val userResponse = UserResponse("1", "test@test.com", "Test User", "123456", "date", "date")
        authRepository.mockCurrentUserResult = AuthResult.Success(userResponse)

        viewModel.loadProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Success)
        assertEquals("Test User", (state as ProfileUiState.Success).user.name)
    }

    @Test
    fun `loadProfile failure updates uiState to Error`() = runTest {
        authRepository.mockCurrentUserResult = AuthResult.Error("Network error")

        viewModel.loadProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Error)
        assertEquals("Network error", (state as ProfileUiState.Error).message)
    }

    @Test
    fun `logout sets logoutEvent to true and calls repository logout`() = runTest {
        viewModel.logout()
        
        assertTrue(authRepository.isLogoutCalled)
        assertTrue(viewModel.logoutEvent.value)
    }

    @Test
    fun `clearLogoutEvent sets logoutEvent to false`() = runTest {
        viewModel.logout()
        assertTrue(viewModel.logoutEvent.value)

        viewModel.clearLogoutEvent()
        assertTrue(!viewModel.logoutEvent.value)
    }
}
