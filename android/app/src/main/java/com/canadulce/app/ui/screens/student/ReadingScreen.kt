package com.canadulce.app.ui.screens.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.Screen
import com.canadulce.app.ui.components.ContentCard
import com.canadulce.app.ui.components.Heading
import com.canadulce.app.ui.components.PrimaryButton

@Composable
internal fun ReadingScreen(vm: AppViewModel) {
    val s = vm.state
    val r = s.reading ?: return
    Heading(r.title, "Tómate tu tiempo. Disfruta la historia.")
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Tamaño del texto", style = MaterialTheme.typography.bodyMedium)
        Row {
            TextButton(onClick = { vm.textSize(-2) }, enabled = s.readerSize > 18) { Text("A−") }
            TextButton(onClick = { vm.textSize(2) }, enabled = s.readerSize < 28) { Text("A+") }
        }
    }
    ContentCard {
        Text(
            r.content,
            fontSize = s.readerSize.sp,
            lineHeight = (s.readerSize * 1.6).sp,
            modifier = Modifier,
        )
    }
    PrimaryButton(if (s.returnToQuiz) "Volver a las preguntas" else "¡Listo! Empezar los retos") {
        if (s.returnToQuiz) vm.show(Screen.QUIZ) else vm.startMission()
    }
}
