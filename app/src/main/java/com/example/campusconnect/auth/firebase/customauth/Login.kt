package com.example.campusconnect.auth.firebase.customauth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class Login {

    private val auth = FirebaseAuth.getInstance()

    suspend fun login(
        email: String,
        password: String
    ): Result<String> {

        return try {
            val result = auth
                .signInWithEmailAndPassword(email, password)
                .await()

            Result.success(result.user?.uid ?: "")

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(
        idToken: String
    ): Result<Unit> {

        return try {

            val credential = GoogleAuthProvider
                .getCredential(idToken, null)

            auth.signInWithCredential(credential)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun logout() {
        auth.signOut()
    }
}