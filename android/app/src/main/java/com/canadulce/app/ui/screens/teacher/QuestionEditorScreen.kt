package com.canadulce.app.ui.screens.teacher

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.AppTextField
import com.canadulce.app.ui.components.ChoiceRow
import com.canadulce.app.ui.components.Dropdown
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.displayLabel

@Composable
internal fun QuestionEditorScreen(vm: AppViewModel) {
    val d = vm.state.questionDraft
    val types = listOf("LITERAL", "INFERENCIAL", "IDEA_PRINCIPAL", "VOCABULARIO", "SECUENCIA")
    Heading(
        if (d.id == null) "Nuevo reto" else "Editar reto",
        "Escribe opciones claras y marca una correcta.",
    )
    AppTextField("Enunciado", d.text, { vm.draft(d.copy(text = it)) }, multiline = true)
    Dropdown("Habilidad", types.map(::displayLabel), types.indexOf(d.type)) {
        vm.draft(d.copy(type = types[it]))
    }
    AppTextField("Orden", d.order, { vm.draft(d.copy(order = it)) }, numeric = true)
    if (d.id != null)
        ChoiceRow("Estado", listOf("Activa", "Inactiva"), if (d.status == "ACTIVA") 0 else 1) {
            vm.draft(d.copy(status = if (it == 0) "ACTIVA" else "INACTIVA"))
        }
    Text("Opciones · Marca la correcta", style = MaterialTheme.typography.titleMedium)
    d.options.forEachIndexed { index, value ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                d.correctIndex == index,
                onClick = { vm.draft(d.copy(correctIndex = index)) },
                modifier = Modifier,
            )
            AppTextField(
                "Opción " + (index + 1),
                value,
                { text ->
                    vm.draft(
                        d.copy(options = d.options.toMutableList().apply { this[index] = text }),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Row {
        TextButton(
            enabled = d.options.size < 4,
            onClick = { vm.draft(d.copy(options = d.options + "")) },
        ) {
            Text("Agregar opción")
        }
        TextButton(
            enabled = d.options.size > 2,
            onClick = {
                vm.draft(
                    d.copy(
                        options = d.options.dropLast(1),
                        correctIndex = d.correctIndex.coerceAtMost(d.options.size - 2),
                    ),
                )
            },
        ) {
            Text("Quitar última")
        }
    }
    PrimaryButton("Guardar pregunta") { vm.saveQuestion() }
}
