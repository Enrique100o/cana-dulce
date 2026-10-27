package com.canadulce.app.ui.screens.teacher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.Screen
import com.canadulce.app.ui.components.ContentCard
import com.canadulce.app.ui.components.EmptyState
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.LabelChip
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.components.StatusMessage
import com.canadulce.app.ui.displayLabel
import com.canadulce.app.ui.theme.Leaf

@Composable
internal fun QuestionsScreen(vm: AppViewModel, confirm: (String, () -> Unit) -> Unit) {
    val s = vm.state
    val r = s.reading ?: return
    Heading("Los retos de la misión", r.title)
    LabelChip(displayLabel(r.status))
    if (r.locked)
        StatusMessage(
            "Esta misión tiene intentos. Las preguntas y opciones se conservan para " +
                "mantener el historial.",
            false,
        )
    else PrimaryButton("Nueva pregunta") { vm.newQuestion() }
    if (s.questions.isEmpty())
        EmptyState(
            "Agrega el primer reto",
            "Cada pregunta debe tener entre 2 y 4 opciones y una respuesta correcta.",
        )
    s.questions.forEach { q ->
        ContentCard {
            Text(q.order.toString() + ". " + q.text, style = MaterialTheme.typography.titleMedium)
            Text(
                displayLabel(q.type) + " · " + displayLabel(q.status),
                fontSize = 13.sp,
                color = Leaf,
            )
            q.options.forEach { Text((if (it.correct) "✓ " else "• ") + it.text, fontSize = 15.sp) }
            if (!r.locked) {
                SecondaryButton("Editar pregunta") { vm.editQuestion(q) }
                if (q.status == "ACTIVA")
                    TextButton(
                        onClick = {
                            confirm(
                                "Esta pregunta dejará de aparecer en nuevos intentos. La lectura debe " +
                                    "conservar al menos una pregunta activa.",
                            ) {
                                vm.deactivateQuestion(q.id)
                            }
                        },
                    ) {
                        Text("Desactivar pregunta")
                    }
            }
        }
    }
    if (r.status == "INACTIVA")
        PrimaryButton("Activar lectura", r.questionCount > 0) { vm.setReadingState(true) }
    SecondaryButton("Volver a la lectura") { vm.show(Screen.DETAIL) }
}
