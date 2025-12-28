package com.bookstats.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.bookstats.BuildConfig
import com.bookstats.data.remote.SupabaseConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication state.
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object NotAuthenticated : AuthState
    data class Authenticated(val userId: String, val email: String?) : AuthState
    data class Error(val message: String) : AuthState
}

/**
 * Repository handling authentication with Supabase.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val auth = SupabaseConfig.client.auth
    
    /**
     * Flow of current authentication state.
     */
    val authState: Flow<AuthState> = auth.sessionStatus.map { status ->
        when {
            status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                val user = status.session.user
                AuthState.Authenticated(
                    userId = user?.id ?: "",
                    email = user?.email
                )
            }
            status is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                AuthState.NotAuthenticated
            }
            else -> AuthState.Loading
        }
    }
    
    /**
     * Get current user ID, or null if not authenticated.
     */
    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
    
    /**
     * Get current user info.
     */
    fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }
    
    /**
     * Check if user is currently authenticated.
     */
    fun isAuthenticated(): Boolean {
        return auth.currentUserOrNull() != null
    }
    
    /**
     * Sign up with email and password.
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in with email and password.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in with Google using Credential Manager.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            
            // Configure Google Sign-In
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            // Get credential from user
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            
            // Extract Google ID token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            
            // Sign in to Supabase with Google token using IDToken provider
            auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
            }
            
            Result.success(Unit)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send password reset email.
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out.
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
