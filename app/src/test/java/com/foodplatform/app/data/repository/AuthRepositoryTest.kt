package com.foodplatform.app.data.repository

import com.foodplatform.app.data.local.SecureTokenStorage
import com.foodplatform.app.data.remote.*
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    class FakeSecureTokenStorage : SecureTokenStorage {
        private var token: String? = null
        override fun saveToken(token: String) { this.token = token }
        override fun getToken(): String? = token
        override fun clearToken() { token = null }
    }

    class FakeAuthApi(
        var loginResponse: Response<LoginResponse>? = null,
        var registerResponse: Response<UserResponse>? = null,
        var getMeResponse: Response<UserResponse>? = null
    ) : AuthApi {
        override suspend fun register(request: RegisterRequest): Response<UserResponse> = registerResponse ?: Response.success(null)
        override suspend fun login(request: LoginRequest): Response<LoginResponse> = loginResponse ?: Response.success(null)
        override suspend fun getMe(): Response<UserResponse> = getMeResponse ?: Response.success(null)
    }

    @Test
    fun `login success saves token and returns success`() = runBlocking {
        val storage = FakeSecureTokenStorage()
        val api = FakeAuthApi(
            loginResponse = Response.success(LoginResponse("valid_token"))
        )
        val repository = AuthRepository(api, storage)

        val result = repository.login(LoginRequest("test@test.com", "pass"))
        
        assertTrue(result is AuthResult.Success)
        assertEquals("valid_token", storage.getToken())
        assertTrue(repository.hasToken())
    }

    @Test
    fun `login error returns Error result and does not save token`() = runBlocking {
        val storage = FakeSecureTokenStorage()
        val errorBody = "{\"message\":\"Invalid credentials\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val api = FakeAuthApi(
            loginResponse = Response.error(401, errorBody)
        )
        val repository = AuthRepository(api, storage)

        val result = repository.login(LoginRequest("test@test.com", "wrong"))

        assertTrue(result is AuthResult.Error)
        assertEquals("Invalid credentials", (result as AuthResult.Error).message)
        assertNull(storage.getToken())
    }

    @Test
    fun `registerAndLogin success performs both and saves token`() = runBlocking {
        val storage = FakeSecureTokenStorage()
        val api = FakeAuthApi(
            registerResponse = Response.success(UserResponse("1", "test@test.com", "Test", "123", "", "")),
            loginResponse = Response.success(LoginResponse("auto_login_token"))
        )
        val repository = AuthRepository(api, storage)

        val result = repository.registerAndLogin(RegisterRequest("test@test.com", "pass", "Test", "123"))

        assertTrue(result is AuthResult.Success)
        assertEquals("auto_login_token", storage.getToken())
    }

    @Test
    fun `register success but login fails returns Error`() = runBlocking {
        val storage = FakeSecureTokenStorage()
        val errorBody = "{\"message\":\"Unknown login error\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val api = FakeAuthApi(
            registerResponse = Response.success(UserResponse("1", "test@test.com", "Test", "123", "", "")),
            loginResponse = Response.error(500, errorBody)
        )
        val repository = AuthRepository(api, storage)

        val result = repository.registerAndLogin(RegisterRequest("test@test.com", "pass", "Test", "123"))

        assertTrue(result is AuthResult.Error)
        assertNull(storage.getToken())
    }
}
