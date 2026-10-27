package com.canadulce.app.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.canadulce.app.AppViewModel
import com.canadulce.app.BuildConfig
import com.canadulce.app.ui.components.AppTextField
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.StatusMessage

@Composable
internal fun ConnectionScreen(vm: AppViewModel) {
    var url by rememberSaveable { mutableStateOf(vm.state.baseUrl) }
    Heading("Conexión de aula", "Configura la dirección proporcionada para esta aplicación.")
    AppTextField("Dirección de la API", url, { url = it })
    if (BuildConfig.DEBUG)
        StatusMessage(
            "Emulador de este computador: http://10.0.2.2:8080/api. Teléfono por USB " +
                "con adb reverse: http://127.0.0.1:8080/api.",
            false,
        )
    PrimaryButton("Guardar conexión") { vm.connection(url) }
}
