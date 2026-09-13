package com.example.cpen321application.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.cpen321application.BuildConfig.GOOGLE_CLIENT_ID
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class GoogleUserInfo(
    val firstName: String,
    val lastName: String,
    val email: String
)

class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): GoogleUserInfo? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(GOOGLE_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

            val fullName = credential.displayName ?: ""
            val parts = fullName.trim().split(" ")
            GoogleUserInfo(
                firstName = parts.firstOrNull() ?: "",
                lastName = parts.drop(1).joinToString(" "),
                email = credential.id
            )
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            android.util.Log.e("GoogleAuth", "GetCredentialException: ${e.type} - ${e.message}", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("GoogleAuth", "Sign-in failed: ${e.javaClass.simpleName} - ${e.message}", e)
            null
        }
    }
}