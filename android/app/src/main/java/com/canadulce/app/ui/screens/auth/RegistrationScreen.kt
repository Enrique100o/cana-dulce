package com.canadulce.app.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.AppTextField
import com.canadulce.app.ui.components.ChoiceRow
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.StatusMessage

@Composable
internal fun RegistrationScreen(vm: AppViewModel) {
    var alias by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableIntStateOf(3) }
    Heading("Tu aventura comienza", "Crea un alias y guarda tu código para volver a ingresar.")
    AppTextField("Elige un alias", alias, { alias = it })
    ChoiceRow("Tu grado", listOf("3.º", "4.º"), grade - 3) { grade = it + 3 }
    AppTextField("Código de acceso", code, { code = it }, password = true)
    AppTextField("Repite tu código", confirmation, { confirmation = it }, password = true)
    StatusMessage(
        "El código debe tener entre 6 y 32 caracteres. Recuerda tu alias y tu código.",
        false,
    )
    PrimaryButton("Crear mi perfil") { vm.register(alias, grade, code, confirmation) }
}
