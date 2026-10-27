package com.canadulce.app.ui.screens.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.BadgeArt
import com.canadulce.app.ui.components.ContentCard
import com.canadulce.app.ui.components.EmptyState
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.ProgressSummary
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.components.Stars
import com.canadulce.app.ui.formatNumber
import com.canadulce.app.ui.theme.Forest
import com.canadulce.app.ui.theme.Leaf
import com.canadulce.app.ui.theme.Mint

@Composable
internal fun ProgressScreen(vm: AppViewModel) {
    val s = vm.state
    val p = s.progress ?: return
    Heading(
        if (vm.teacher) "Progreso de " + p.alias else "Tu jardín de logros",
        "Cada historia deja una nueva semilla.",
    )
    ProgressSummary(p)
    ContentCard {
        Text("Así vas creciendo", style = MaterialTheme.typography.titleMedium)
        Text("Promedio: " + formatNumber(p.average) + " / 100")
        Text("Mejor intento: " + formatNumber(p.best) + " / 100")
        Text(
            p.completed.toString() +
                (if (p.completed == 1) " intento completado · " else " intentos completados · ") +
                p.missions +
                if (p.missions == 1) " lectura diferente" else " lecturas diferentes",
            fontSize = 14.sp,
        )
    }
    Text("Tus insignias", style = MaterialTheme.typography.titleLarge)
    p.badges.forEachIndexed { index, badge ->
        ContentCard(color = if (badge.obtained) Mint else Color.White) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BadgeArt(badge.obtained, Modifier.size(58.dp))
                Column(Modifier.weight(1f)) {
                    Text(badge.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (index) {
                            0 -> "Completa tu primera lectura."
                            1 -> "Completa 3 lecturas diferentes."
                            else -> "Logra 100 en un intento."
                        },
                        fontSize = 13.sp,
                        color = Leaf,
                    )
                    Text(
                        if (badge.obtained) "¡Obtenida!" else "Por descubrir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.obtained) Forest else Color.Gray,
                    )
                }
            }
        }
    }
    Text("Tu recorrido", style = MaterialTheme.typography.titleLarge)
    if (s.attempts.isEmpty())
        EmptyState(
            "Tu primera historia te espera",
            "Completa una misión para ver tus resultados aquí.",
        )
    s.attempts.forEach { a ->
        ContentCard {
            Text(a.title, style = MaterialTheme.typography.titleMedium)
            Text(
                a.started.take(10) +
                    " · " +
                    if (a.status == "COMPLETADO") formatNumber(a.score) + " / 100"
                    else "Por continuar",
                fontSize = 14.sp,
            )
            if (a.status == "COMPLETADO") {
                Stars(a.stars ?: 0, 23)
                SecondaryButton("Ver respuestas") { vm.result(a.id) }
            } else if (!vm.teacher) SecondaryButton("Retomar misión") { vm.resume(a) }
        }
    }
}
