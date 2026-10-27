package com.canadulce.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.ContentCard
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.LabelChip
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.components.Stars
import com.canadulce.app.ui.components.StatusMessage
import com.canadulce.app.ui.components.StoryArt
import com.canadulce.app.ui.displayLabel
import com.canadulce.app.ui.theme.Leaf

@Composable
internal fun ReadingDetailScreen(vm: AppViewModel, confirm: (String, () -> Unit) -> Unit) {
    val r = vm.state.reading ?: return
    Heading(r.title, r.category.ifBlank { "Una nueva historia por descubrir" })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelChip(r.grade.toString() + ".º")
        LabelChip(displayLabel(r.difficulty))
        if (vm.teacher) LabelChip(displayLabel(r.status))
    }
    if (vm.teacher) {
        Text(r.content, fontSize = 18.sp, lineHeight = 29.sp)
        if (r.locked)
            StatusMessage(
                "Esta lectura ya tiene intentos. Su contenido se conserva para proteger el " +
                    "historial. Puedes crear otra lectura.",
                false,
            )
        PrimaryButton("Administrar preguntas") { vm.questions() }
        SecondaryButton("Editar contenido", !r.locked) { vm.editReading() }
        if (r.status == "ACTIVA")
            SecondaryButton("Desactivar lectura") {
                confirm(
                    "Dejará de admitir nuevas misiones. El historial se conserva y los intentos " +
                        "iniciados podrán terminarse.",
                ) {
                    vm.setReadingState(false)
                }
            }
        else PrimaryButton("Activar lectura") { vm.setReadingState(true) }
        SecondaryButton("Crear otra lectura") { vm.newReading() }
    } else {
        StoryArt(Modifier.fillMaxWidth().height(160.dp))
        ContentCard {
            Text("Tu misión", style = MaterialTheme.typography.titleMedium)
            Text(
                "Lee con calma y responde " +
                    r.questionCount +
                    " retos. Puedes consultar el texto mientras respondes.",
            )
            Stars(3, 27)
            Text("Hasta 3 estrellas por intento", fontSize = 13.sp, color = Leaf)
        }
        PrimaryButton("Vamos a leer") { vm.readText() }
    }
}
