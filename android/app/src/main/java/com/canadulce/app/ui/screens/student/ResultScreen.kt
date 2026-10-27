package com.canadulce.app.ui.screens.student

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.ContentCard
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.LabelChip
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.components.Stars
import com.canadulce.app.ui.formatNumber
import com.canadulce.app.ui.theme.Leaf
import com.canadulce.app.ui.theme.Mint

@Composable
internal fun ResultScreen(vm: AppViewModel) {
    val a = vm.state.attempt ?: return
    Heading(
        if (vm.teacher) "Detalle del intento"
        else if ((a.score ?: 0.0) >= 80) "¡Tu lectura brilló!" else "¡Misión completada!",
        a.title,
    )
    ContentCard(color = Mint) {
        Text(
            formatNumber(a.score) + " / 100",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier,
        )
        Stars(a.stars ?: 0, 42)
        Text((a.correct ?: 0).toString() + " de " + a.total + " respuestas correctas")
        Text(
            if (a.score == 100.0) "¡Comprendiste todos los retos!"
            else "Cada intento te ayuda a comprender mejor. Sigue practicando.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (!vm.teacher) {
        PrimaryButton("Ver mis logros") { vm.progress() }
        SecondaryButton("Volver a misiones") { vm.home() }
    }
    Text("Tus respuestas", style = MaterialTheme.typography.titleLarge)
    a.answers.forEach { answer ->
        ContentCard {
            LabelChip(if (answer.correct) "COMPRENDIDO" else "POR REVISAR")
            Text(answer.text, style = MaterialTheme.typography.titleMedium)
            Text(answer.chosen)
            if (!answer.correct)
                Text(
                    "Vuelve al texto y piensa en las pistas de la historia.",
                    fontSize = 13.sp,
                    color = Leaf,
                )
        }
    }
}
