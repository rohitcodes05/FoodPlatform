package com.foodplatform.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SecureTokenStorageImplTest {

    private lateinit var context: Context
    private lateinit var storage: SecureTokenStorageImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storage = SecureTokenStorageImpl(context)
    }

    @After
    fun teardown() {
        storage.clearToken()
    }

    @Test
    fun saveAndRetrieveToken_returnsCorrectToken() {
        val testToken = "test_jwt_12345"
        storage.saveToken(testToken)
        val retrieved = storage.getToken()
        assertEquals(testToken, retrieved)
    }

    @Test
    fun getEmptyToken_returnsNull() {
        val retrieved = storage.getToken()
        assertNull(retrieved)
    }

    @Test
    fun clearToken_removesToken() {
        storage.saveToken("to_be_deleted")
        assertNotNull(storage.getToken())
        storage.clearToken()
        assertNull(storage.getToken())
    }

    @Test
    fun checkTokenIsNotInPlaintext() {
        val testToken = "super_secret_token_value_xyz"
        storage.saveToken(testToken)

        // Read the raw shared preferences XML file directly from device storage
        val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/auth_prefs.xml")
        if (prefsFile.exists()) {
            val rawContent = prefsFile.readText()
            
            // The raw content MUST NOT contain the plaintext token because it should be encrypted via EncryptedSharedPreferences
            assertFalse("Token is stored in plaintext!", rawContent.contains(testToken))
        }
    }
}
