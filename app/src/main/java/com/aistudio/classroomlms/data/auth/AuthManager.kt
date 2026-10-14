package com.aistudio.classroomlms.data.auth

import android.accounts.Account
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

@Suppress("DEPRECATION")
@Singleton
class AuthManager @Inject constructor(@ApplicationContext private val context: Context) {

    val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope("https://www.googleapis.com/auth/spreadsheets"))
        .build()

    val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        return@withContext try {
            val androidAccount = account.account ?: account.email?.let { Account(it, "com.google") }
            if (androidAccount == null) return@withContext null
            val scope = "oauth2:https://www.googleapis.com/auth/spreadsheets"
            GoogleAuthUtil.getToken(context, androidAccount, scope)
        } catch (e: Exception) {
            SafeLogger.e("AuthManager", "Failed to obtain Google Sheets access token", e)
            null
        }
    }

    fun isUserSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
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