package com.example.nba_salary_manager.viewmodel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel(context: Context) : ViewModel() {
    private val autenticacion: FirebaseAuth = FirebaseAuth.getInstance()
    private val preferencias = context.applicationContext.getSharedPreferences(
        USER_PROFILE_PREFS,
        Context.MODE_PRIVATE
    )

    val currentUser: MutableState<FirebaseUser?> = mutableStateOf(autenticacion.currentUser)
    val currentPhone: MutableState<String?> = mutableStateOf(cargarTelefono(autenticacion.currentUser?.uid))

    fun signIn(email: String, contrasena: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank() || contrasena.isBlank()) {
            onResult(false)
            return
        }

        autenticacion.signInWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener { tarea ->
                currentUser.value = autenticacion.currentUser
                currentPhone.value = cargarTelefono(autenticacion.currentUser?.uid)
                onResult(tarea.isSuccessful)
            }
    }

    fun signUp(
        email: String,
        contrasena: String,
        nombreUsuario: String,
        telefono: String,
        onResult: (String?) -> Unit
    ) {
        if (nombreUsuario.isBlank()) {
            onResult("El nombre de usuario no puede estar vacío")
            return
        }
        if (telefono.isBlank()) {
            onResult("El teléfono no puede estar vacío")
            return
        }
        if (email.isBlank() || contrasena.isBlank()) {
            onResult("El correo y la contraseña no pueden estar vacíos")
            return
        }
        if (contrasena.length < 6) {
            onResult("La contraseña debe tener al menos 6 caracteres")
            return
        }

        autenticacion.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener { tarea ->
                val usuarioCreado = autenticacion.currentUser
                if (!tarea.isSuccessful || usuarioCreado == null) {
                    currentUser.value = usuarioCreado
                    currentPhone.value = cargarTelefono(usuarioCreado?.uid)
                    onResult(tarea.exception?.message)
                    return@addOnCompleteListener
                }

                val solicitudActualizacion = UserProfileChangeRequest.Builder()
                    .setDisplayName(nombreUsuario.trim())
                    .build()

                usuarioCreado.updateProfile(solicitudActualizacion)
                    .addOnCompleteListener { tareaActualizacion ->
                        guardarTelefono(usuarioCreado.uid, telefono)
                        currentUser.value = autenticacion.currentUser
                        currentPhone.value = cargarTelefono(usuarioCreado.uid)
                        if (tareaActualizacion.isSuccessful) {
                            onResult(null)
                        } else {
                            onResult(tareaActualizacion.exception?.message)
                        }
                    }
            }
    }

    fun updateDisplayName(displayName: String, onResult: (String?) -> Unit) {
        val usuario = autenticacion.currentUser
        if (usuario == null) {
            onResult("No hay ninguna sesión iniciada")
            return
        }
        if (displayName.isBlank()) {
            onResult("El nombre de usuario no puede estar vacío")
            return
        }

        val solicitudActualizacion = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName.trim())
            .build()

        usuario.updateProfile(solicitudActualizacion)
            .addOnCompleteListener { tarea ->
                currentUser.value = autenticacion.currentUser
                if (tarea.isSuccessful) onResult(null) else onResult(tarea.exception?.message)
            }
    }

    fun updatePhone(phone: String, onResult: (String?) -> Unit) {
        val usuario = autenticacion.currentUser
        if (usuario == null) {
            onResult("No hay ninguna sesión iniciada")
            return
        }
        if (phone.isBlank()) {
            onResult("El teléfono no puede estar vacío")
            return
        }

        guardarTelefono(usuario.uid, phone)
        currentPhone.value = cargarTelefono(usuario.uid)
        onResult(null)
    }

    fun sendPasswordReset(onResult: (String?) -> Unit) {
        val email = autenticacion.currentUser?.email
        if (email.isNullOrBlank()) {
            onResult("La cuenta no tiene un correo disponible")
            return
        }

        autenticacion.sendPasswordResetEmail(email)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) onResult(null) else onResult(tarea.exception?.message)
            }
    }

    fun reloadUser(onResult: (String?) -> Unit = {}) {
        val usuario = autenticacion.currentUser
        if (usuario == null) {
            currentUser.value = null
            currentPhone.value = null
            onResult("No hay ninguna sesión iniciada")
            return
        }

        usuario.reload()
            .addOnCompleteListener { tarea ->
                currentUser.value = autenticacion.currentUser
                currentPhone.value = cargarTelefono(autenticacion.currentUser?.uid)
                if (tarea.isSuccessful) onResult(null) else onResult(tarea.exception?.message)
            }
    }

    fun signOut() {
        autenticacion.signOut()
        currentUser.value = null
        currentPhone.value = null
    }

    private fun cargarTelefono(uid: String?): String? {
        if (uid.isNullOrBlank()) return null
        return preferencias.getString(claveTelefono(uid), null)
    }

    private fun guardarTelefono(uid: String, phone: String) {
        preferencias.edit()
            .putString(claveTelefono(uid), phone.trim())
            .apply()
    }

    private fun claveTelefono(uid: String): String = "phone_$uid"

    private companion object {
        const val USER_PROFILE_PREFS = "user_profile_prefs"
    }
}
