package com.canadulce.app.ui.screens.teacher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.components.AppTextField
import com.canadulce.app.ui.components.ChoiceRow
import com.canadulce.app.ui.components.ContentCard
import com.canadulce.app.ui.components.EmptyState
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.theme.Leaf

@Composable
internal fun StudentsScreen(vm: AppViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableIntStateOf(0) }
    Heading("Avances del aula", "Consulta los logros y el historial por estudiante.")
    AppTextField("Buscar por alias", search, { search = it })
    ChoiceRow("Grado", listOf("Todos", "3.º", "4.º"), grade) { grade = it }
    SecondaryButton("Buscar estudiantes") {
        vm.searchStudents(search, if (grade == 0) null else grade + 2)
    }
    val students = vm.state.students
    if (students.isEmpty())
        EmptyState("Sin estudiantes en esta consulta", "Prueba otro alias o grado.")
    students.forEach { student ->
        ContentCard {
            Text(student.alias, style = MaterialTheme.typography.titleMedium)
            Text(student.grade.toString() + ".º de primaria", color = Leaf)
            SecondaryButton("Consultar progreso") { vm.progress(student) }
        }
    }
}
