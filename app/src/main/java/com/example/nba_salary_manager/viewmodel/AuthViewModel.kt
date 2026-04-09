package com.example.nba_salary_manager.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser: MutableState<FirebaseUser?> = mutableStateOf(auth.currentUser)

    fun signIn(email: String, pass: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onResult(false)
            return
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                currentUser.value = auth.currentUser
                onResult(task.isSuccessful)
            }
    }

    fun signUp(email: String, pass: String, onResult: (String?) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onResult("El email y la contraseña no pueden estar vacíos")
            return    }
        if (pass.length < 6) {
            onResult("La contraseña debe tener al menos 6 caracteres")
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                currentUser.value = auth.currentUser
                if (task.isSuccessful) onResult(null) else onResult(task.exception?.message)
            }
    }

    fun signOut() {
        auth.signOut()
        currentUser.value = null
    }
}
