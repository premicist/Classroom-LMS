package com.aistudio.classroomlms.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import com.aistudio.classroomlms.util.SafeLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OAuth scope URL requested for Google Sheets access.
 */
private const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"

@Singleton
class AuthManager @Inject constructor(@ApplicationContext private val context: Context) {

    val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    // GSO requests the Sheets scope so silentSignIn() can validate it without a dialog.
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(SHEETS_SCOPE))
        .build()

    val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    /**
     * Returns a valid OAuth access token for the Sheets scope, or null if the user is not signed
     * in / silent re-auth failed. Runs off the main thread.
     *
     * Note: GoogleSignInAccount.accessToken was removed in play-services-auth 21.x.
     * GoogleAuthUtil.getToken() remains the only client-side way to obtain a scoped OAuth token
     * with this library version.
     */
    @Suppress("DEPRECATION")
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val task = signInClient.silentSignIn()
            val account = Tasks.await(task)
            account?.email?.let { email ->
                val token = GoogleAuthUtil.getToken(
                    context,
                    email,
                    "oauth2:$SHEETS_SCOPE"
                )
                token.takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            SafeLogger.e("AuthManager", "Failed to obtain Google Sheets access token", e)
            null
        }
    }

    /** Returns true only when the signed-in account holds the Sheets scope. */
    fun isUserSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && account.grantedScopes.contains(Scope(SHEETS_SCOPE))
    }

    fun getSignedInAccount() = GoogleSignIn.getLastSignedInAccount(context)

    suspend fun signOutUser() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        withContext(Dispatchers.IO) {
            Tasks.await(signInClient.signOut())
        }
    }

    fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }
}