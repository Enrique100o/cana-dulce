package com.canadulce.app.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.ui.theme.Forest
import com.canadulce.app.ui.theme.Leaf
import com.canadulce.app.ui.theme.Mint

@Composable
internal fun QuizScreen(vm: AppViewModel) {
    val s = vm.state
    val q = s.questions.getOrNull(s.questionIndex) ?: return
    Text(
        "RETO " + (s.questionIndex + 1) + " DE " + s.questions.size,
        fontSize = 12.sp,
        color = Leaf,
        letterSpacing = 1.5.sp,
    )
    LinearProgressIndicator(
        progress = { (s.questionIndex + 1f) / s.questions.size },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
        color = Leaf,
        trackColor = Mint,
    )
    Text(q.text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier)
    Text("Elige una respuesta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    q.options.forEachIndexed { index, option ->
        val selected = s.selections[q.id] == option.id
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) Mint else Color.White)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) Forest else Color(0xFFCFD8CC),
                    RoundedCornerShape(18.dp),
                )
                .selectable(
                    selected,
                    role = Role.RadioButton,
                    onClick = { vm.choose(q.id, option.id) },
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected, onClick = null)
            Text(
                option.text,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (s.questionIndex > 0)
            OutlinedButton(
                onClick = { vm.question(-1) },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Text("Anterior")
            }
        Button(
            onClick = {
                if (s.questionIndex == s.questions.lastIndex) vm.finish() else vm.question(1)
            },
            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            enabled = q.id in s.selections,
        ) {
            Text(if (s.questionIndex == s.questions.lastIndex) "Finalizar misión" else "Siguiente")
        }
    }
    TextButton(onClick = { vm.readText(true) }, modifier = Modifier.fillMaxWidth()) {
        Text("Consultar la historia")
    }
}
