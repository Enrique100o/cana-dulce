package com.canadulce.app.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.AppTextField
import com.canadulce.app.ui.components.ContentCard
import com.canadulce.app.ui.components.EmptyState
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.LabelChip
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.ProgressSummary
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.components.Stars
import com.canadulce.app.ui.displayLabel
import com.canadulce.app.ui.theme.Forest
import com.canadulce.app.ui.theme.Leaf
import com.canadulce.app.ui.theme.Mint

@Composable
internal fun MissionsScreen(vm: AppViewModel) {
    val s = vm.state
    var search by rememberSaveable { mutableStateOf("") }
    Heading("¡Hola, " + s.session?.student?.alias + "!", "Hoy puedes descubrir algo nuevo.")
    ProgressSummary(s.progress)
    s.attempts
        .filter { it.status == "EN_PROGRESO" }
        .forEach { attempt ->
            ContentCard {
                LabelChip("MISIÓN POR CONTINUAR")
                Text(attempt.title, style = MaterialTheme.typography.titleMedium)
                PrimaryButton("Retomar misión") { vm.resume(attempt) }
            }
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Tus misiones",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = vm::home) { Text("Actualizar") }
    }
    AppTextField("Buscar una historia", search, { search = it })
    SecondaryButton("Buscar") { vm.searchReadings(search) }
    val filtered = s.readings
    if (filtered.isEmpty() && !s.busy)
        EmptyState("Aún no hay misiones aquí", "Prueba otra búsqueda o vuelve más tarde.")
    filtered.forEachIndexed { index, r ->
        val best =
            s.attempts
                .filter { it.readingId == r.id && it.status == "COMPLETADO" }
                .maxByOrNull { it.score ?: 0.0 }
        ContentCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(46.dp).background(Mint, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (index + 1).toString().padStart(2, '0'),
                        fontWeight = FontWeight.Bold,
                        color = Forest,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        r.category.ifBlank { "AVENTURA" }.uppercase(),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = Leaf,
                    )
                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                r.questionCount.toString() +
                    " retos · Dificultad " +
                    displayLabel(r.difficulty).lowercase(),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (best != null) Stars(best.stars ?: 0, 24)
            PrimaryButton(if (best == null) "Explorar misión" else "Practicar de nuevo") {
                vm.openReading(r.id)
            }
        }
    }
}
