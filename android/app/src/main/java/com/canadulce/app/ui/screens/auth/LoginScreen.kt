package com.canadulce.app.ui.screens.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canadulce.app.AppViewModel
import com.canadulce.app.Screen
import com.canadulce.app.ui.components.AppTextField
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.components.StoryArt
import com.canadulce.app.ui.theme.Leaf

@Composable
internal fun LoginScreen(vm: AppViewModel) {
    val teacher = vm.state.teacherLogin
    var alias by rememberSaveable(teacher) { mutableStateOf("") }
    var code by rememberSaveable(teacher) { mutableStateOf("") }
    Heading(
        if (teacher) "Tu espacio docente" else "¡Qué bueno verte!",
        if (teacher) "Prepara lecturas y acompaña los avances."
        else "Entra y continúa tu aventura lectora.",
    )
    if (!teacher) StoryArt(Modifier.fillMaxWidth().height(125.dp))
    AppTextField(if (teacher) "Usuario" else "Tu alias", alias, { alias = it })
    AppTextField(
        if (teacher) "Contraseña" else "Código de acceso",
        code,
        { code = it },
        password = true,
    )
    PrimaryButton("Ingresar") { vm.login(alias, code) }
    if (!teacher) {
        SecondaryButton("Crear mi alias") { vm.show(Screen.REGISTER) }
        Text(
            "Usa un apodo. No necesitas escribir tu nombre real.",
            style = MaterialTheme.typography.bodyMedium,
            color = Leaf,
        )
    }
}
