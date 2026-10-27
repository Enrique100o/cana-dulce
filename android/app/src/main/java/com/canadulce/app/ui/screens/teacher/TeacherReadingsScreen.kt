package com.canadulce.app.ui.screens.teacher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.AppTextField
import com.canadulce.app.ui.components.ChoiceRow
import com.canadulce.app.ui.components.ContentCard
import com.canadulce.app.ui.components.EmptyState
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.LabelChip
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.displayLabel

@Composable
internal fun TeacherReadingsScreen(vm: AppViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableIntStateOf(0) }
    var status by rememberSaveable { mutableIntStateOf(0) }
    Heading("Historias para aprender", "Prepara las misiones de tu aula.")
    PrimaryButton("Nueva lectura") { vm.newReading() }
    AppTextField("Buscar por título", search, { search = it })
    ChoiceRow("Grado", listOf("Todos", "3.º", "4.º"), grade) { grade = it }
    ChoiceRow("Estado", listOf("Todos", "Activas", "Inactivas"), status) { status = it }
    SecondaryButton("Aplicar filtros") {
        vm.searchReadings(
            search,
            if (grade == 0) null else grade + 2,
            if (status == 0) null else if (status == 1) "ACTIVA" else "INACTIVA",
        )
    }
    val readings = vm.state.readings
    if (readings.isEmpty() && !vm.state.busy)
        EmptyState("Sin lecturas en esta consulta", "Crea una nueva o cambia los filtros.")
    readings.forEach { r ->
        ContentCard {
            Text(r.title, style = MaterialTheme.typography.titleMedium)
            Text(
                r.grade.toString() +
                    ".º · " +
                    displayLabel(r.status) +
                    " · " +
                    r.questionCount +
                    " preguntas",
                fontSize = 14.sp,
            )
            if (r.locked) LabelChip("Contenido con historial")
            SecondaryButton("Administrar") { vm.openReading(r.id) }
        }
    }
    SecondaryButton("Actualizar lecturas") { vm.home() }
}
