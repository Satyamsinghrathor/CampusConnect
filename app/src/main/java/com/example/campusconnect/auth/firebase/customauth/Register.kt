package com.example.campusconnect.auth.firebase.customauth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class Register {

    private val auth = FirebaseAuth.getInstance()

    suspend fun register(
        email: String,
        password: String
    ): Result<String> {

        return try {

            val result = auth
                .createUserWithEmailAndPassword(
                    email,
                    password
                )
                .await()

            Result.success(
                result.user?.uid ?: ""
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}