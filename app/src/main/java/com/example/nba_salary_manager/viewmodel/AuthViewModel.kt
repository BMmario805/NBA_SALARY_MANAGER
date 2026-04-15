package com.example.nba_salary_manager.viewmodel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel(context: Context) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefs = context.applicationContext.getSharedPreferences(
        USER_PROFILE_PREFS,
        Context.MODE_PRIVATE
    )

    val currentUser: MutableState<FirebaseUser?> = mutableStateOf(auth.currentUser)
    val currentPhone: MutableState<String?> = mutableStateOf(loadPhone(auth.currentUser?.uid))

    fun signIn(email: String, pass: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onResult(false)
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                currentUser.value = auth.currentUser
                currentPhone.value = loadPhone(auth.currentUser?.uid)
                onResult(task.isSuccessful)
            }
    }

    fun signUp(
        email: String,
        pass: String,
        username: String,
        phone: String,
        onResult: (String?) -> Unit
    ) {
        if (username.isBlank()) {
            onResult("El nombre de usuario no puede estar vacio")
            return
        }
        if (phone.isBlank()) {
            onResult("El telefono no puede estar vacio")
            return
        }
        if (email.isBlank() || pass.isBlank()) {
            onResult("El email y la contrasena no pueden estar vacios")
            return
        }
        if (pass.length < 6) {
            onResult("La contrasena debe tener al menos 6 caracteres")
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                val createdUser = auth.currentUser
                if (!task.isSuccessful || createdUser == null) {
                    currentUser.value = createdUser
                    currentPhone.value = loadPhone(createdUser?.uid)
                    onResult(task.exception?.message)
                    return@addOnCompleteListener
                }

                val updateRequest = UserProfileChangeRequest.Builder()
                    .setDisplayName(username.trim())
                    .build()

                createdUser.updateProfile(updateRequest)
                    .addOnCompleteListener { updateTask ->
                        savePhone(createdUser.uid, phone)
                        currentUser.value = auth.currentUser
                        currentPhone.value = loadPhone(createdUser.uid)
                        if (updateTask.isSuccessful) {
                            onResult(null)
                        } else {
                            onResult(updateTask.exception?.message)
                        }
                    }
            }
    }

    fun updateDisplayName(displayName: String, onResult: (String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult("No hay ninguna sesion iniciada")
            return
        }
        if (displayName.isBlank()) {
            onResult("El nombre de usuario no puede estar vacio")
            return
        }

        val updateRequest = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName.trim())
            .build()

        user.updateProfile(updateRequest)
            .addOnCompleteListener { task ->
                currentUser.value = auth.currentUser
                if (task.isSuccessful) onResult(null) else onResult(task.exception?.message)
            }
    }

    fun updatePhone(phone: String, onResult: (String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult("No hay ninguna sesion iniciada")
            return
        }
        if (phone.isBlank()) {
            onResult("El telefono no puede estar vacio")
            return
        }

        savePhone(user.uid, phone)
        currentPhone.value = loadPhone(user.uid)
        onResult(null)
    }

    fun sendPasswordReset(onResult: (String?) -> Unit) {
        val email = auth.currentUser?.email
        if (email.isNullOrBlank()) {
            onResult("La cuenta no tiene un correo disponible")
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(null) else onResult(task.exception?.message)
            }
    }

    fun reloadUser(onResult: (String?) -> Unit = {}) {
        val user = auth.currentUser
        if (user == null) {
            currentUser.value = null
            currentPhone.value = null
            onResult("No hay ninguna sesion iniciada")
            return
        }

        user.reload()
            .addOnCompleteListener { task ->
                currentUser.value = auth.currentUser
                currentPhone.value = loadPhone(auth.currentUser?.uid)
                if (task.isSuccessful) onResult(null) else onResult(task.exception?.message)
            }
    }

    fun signOut() {
        auth.signOut()
        currentUser.value = null
        currentPhone.value = null
    }

    private fun loadPhone(uid: String?): String? {
        if (uid.isNullOrBlank()) return null
        return prefs.getString(phoneKey(uid), null)
    }

    private fun savePhone(uid: String, phone: String) {
        prefs.edit()
            .putString(phoneKey(uid), phone.trim())
            .apply()
    }

    private fun phoneKey(uid: String): String = "phone_$uid"

    private companion object {
        const val USER_PROFILE_PREFS = "user_profile_prefs"
    }
}
