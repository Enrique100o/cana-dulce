package com.canadulce.app.ui.screens.teacher

import androidx.compose.runtime.Composable
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.AppTextField
import com.canadulce.app.ui.components.ChoiceRow
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.StatusMessage

@Composable
internal fun ReadingEditorScreen(vm: AppViewModel) {
    val d = vm.state.readingDraft
    Heading(
        if (d.id == null) "Nueva lectura" else "Editar lectura",
        "Una lectura será una misión para los estudiantes.",
    )
    AppTextField("Título", d.title, { vm.draft(d.copy(title = it)) })
    AppTextField(
        "Texto de la historia",
        d.content,
        { vm.draft(d.copy(content = it)) },
        multiline = true,
    )
    ChoiceRow("Grado", listOf("3.º", "4.º"), d.grade - 3) { vm.draft(d.copy(grade = it + 3)) }
    ChoiceRow(
        "Dificultad",
        listOf("Baja", "Media", "Alta"),
        listOf("BAJA", "MEDIA", "ALTA").indexOf(d.difficulty),
    ) {
        vm.draft(d.copy(difficulty = listOf("BAJA", "MEDIA", "ALTA")[it]))
    }
    AppTextField("Categoría (opcional)", d.category, { vm.draft(d.copy(category = it)) })
    StatusMessage(
        if (d.id == null) "Se guardará inactiva. Agrega preguntas y actívala cuando esté lista."
        else "El estado actual se conserva. Puedes cambiarlo desde el detalle de la lectura.",
        false,
    )
    PrimaryButton("Guardar lectura") { vm.saveReading() }
}
