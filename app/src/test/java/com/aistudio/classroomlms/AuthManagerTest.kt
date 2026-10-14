package com.aistudio.classroomlms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.classroomlms.data.auth.AuthManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for AuthManager focusing on offline-safe behavior:
 * - getAccessToken() returns null when no Google account is signed in
 * - isUserSignedIn() returns false when no account with the Sheets scope is present
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthManagerTest {

    private lateinit var authManager: AuthManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        authManager = AuthManager(context)
    }

    @Test
    fun `getAccessToken returns null when not signed in`() = runBlocking {
        val token = authManager.getAccessToken()
        assertNull("Token must be null when no account is signed in", token)
    }

    @Test
    fun `isUserSignedIn returns false when not signed in`() {
        assertFalse("No account should be signed in at startup", authManager.isUserSignedIn())
    }

    @Test
    fun `getSignedInAccount returns null when not signed in`() {
        assertNull("No signed-in account at startup", authManager.getSignedInAccount())
    }

    @Test
    fun `credentialManager is instantiated lazily`() {
        assertNotNull(authManager.credentialManager)
    }

    @Test
    fun `signInIntent is non-null`() {
        val intent = authManager.getSignInIntent()
        assertNotNull(intent)
    }
}
