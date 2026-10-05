package com.example.data.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class AuthManager(private val context: Context) {

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
            e.printStackTrace()
            null
        }
    }

    fun isUserSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    fun getSignedInAccount() = GoogleSignIn.getLastSignedInAccount(context)

    fun signOut() {
        signInClient.signOut()
    }

    fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }
}