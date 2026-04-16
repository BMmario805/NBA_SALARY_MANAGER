package com.example.nba_salary_manager.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nba_salary_manager.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun UserAccessButton(
    user: FirebaseUser?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (user == null) {
        Box(
            modifier = modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Iniciar sesión",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else {
        Box(
            modifier = modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D428A))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = accountInitials(user),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun AuthDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var esRegistro by rememberSaveable { mutableStateOf(false) }
    var nombreUsuario by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    val contexto = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (esRegistro) "Crear cuenta" else "Iniciar sesión",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (esRegistro) {
                        "Completa el registro con tu nombre de usuario, tu teléfono y tu correo."
                    } else {
                        "Accede a tu cuenta sin salir de la pantalla principal."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (esRegistro) {
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = nombreUsuario,
                        onValueChange = { nombreUsuario = it },
                        label = { Text("Nombre de usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (esRegistro) {
                            viewModel.signUp(email, contrasena, nombreUsuario, telefono) { error ->
                                if (error == null) {
                                    Toast.makeText(contexto, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show()
                                    onAuthenticated()
                                } else {
                                    Toast.makeText(contexto, error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            viewModel.signIn(email, contrasena) { inicioCorrecto ->
                                if (inicioCorrecto) {
                                    Toast.makeText(contexto, "Sesión iniciada correctamente", Toast.LENGTH_SHORT).show()
                                    onAuthenticated()
                                } else {
                                    Toast.makeText(
                                        contexto,
                                        "No se pudo iniciar sesión",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (esRegistro) "Crear cuenta" else "Entrar")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { esRegistro = !esRegistro }) {
                        Text(
                            if (esRegistro) {
                                "Ya tengo una cuenta"
                            } else {
                                "Crear una cuenta nueva"
                            }
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsSheet(
    user: FirebaseUser,
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val contexto = LocalContext.current
    val telefonoGuardado by viewModel.currentPhone
    var nombreUsuario by rememberSaveable(user.uid, user.displayName) {
        mutableStateOf(user.displayName.orEmpty())
    }
    var telefono by rememberSaveable(user.uid, telefonoGuardado) {
        mutableStateOf(telefonoGuardado.orEmpty())
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D428A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = accountInitials(user),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName?.takeIf { it.isNotBlank() }
                            ?: user.email?.substringBefore("@")
                            ?: "Usuario",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email ?: "Correo no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Perfil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nombreUsuario,
                        onValueChange = { nombreUsuario = it },
                        label = { Text("Nombre de usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateDisplayName(nombreUsuario) { errorNombre ->
                                    if (errorNombre != null) {
                                        Toast.makeText(contexto, errorNombre, Toast.LENGTH_SHORT).show()
                                        return@updateDisplayName
                                    }
                                    viewModel.updatePhone(telefono) { errorTelefono ->
                                        if (errorTelefono == null) {
                                            Toast.makeText(
                                                contexto,
                                                "Perfil actualizado correctamente",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(contexto, errorTelefono, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.reloadUser { error ->
                                    if (error == null) {
                                        Toast.makeText(
                                            contexto,
                                            "Datos actualizados correctamente",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(contexto, error, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Recargar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cuenta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AccountInfoRow("Nombre de usuario", user.displayName ?: "No disponible")
                    AccountInfoRow("Teléfono", telefonoGuardado ?: "No disponible")
                    AccountInfoRow("Correo", user.email ?: "No disponible")

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.sendPasswordReset { error ->
                                if (error == null) {
                                    Toast.makeText(
                                        contexto,
                                        "Se ha enviado el correo para cambiar la contraseña",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(contexto, error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cambiar contraseña")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.signOut()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cerrar sesión")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun accountInitials(user: FirebaseUser): String {
    val source = when {
        !user.displayName.isNullOrBlank() -> user.displayName!!
        !user.email.isNullOrBlank() -> user.email!!.substringBefore("@")
        else -> "U"
    }

    return source
        .trim()
        .split(Regex("[\\s._-]+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { segment -> segment.first().uppercase() }
        .ifBlank { "U" }
}

private fun providerLabel(providerId: String): String {
    return when (providerId) {
        "password" -> "Correo y contraseña"
        "google.com" -> "Google"
        "facebook.com" -> "Facebook"
        "github.com" -> "GitHub"
        "phone" -> "Teléfono"
        else -> providerId
    }
}
