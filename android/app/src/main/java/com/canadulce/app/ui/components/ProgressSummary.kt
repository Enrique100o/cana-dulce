package com.canadulce.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.canadulce.app.data.Progress
import com.canadulce.app.ui.formatNumber
import com.canadulce.app.ui.theme.Forest
import com.canadulce.app.ui.theme.Gold

@Composable
internal fun ProgressSummary(p: Progress?) {
    ContentCard(color = Forest) {
        Text(
            "TU AVENTURA SIGUE CRECIENDO",
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = Color(0xFFD9E9C8),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(formatNumber(p?.points ?: 0.0), "Puntos")
            Metric((p?.stars ?: 0).toString(), "Estrellas")
            Metric((p?.missions ?: 0).toString(), "Misiones")
        }
    }
}

@Composable
internal fun Metric(value: String, title: String) {
    Column {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(title, fontSize = 12.sp, color = Color(0xFFD9E9C8))
    }
}

@Composable
internal fun Stars(count: Int, size: Int) {
    Text(
        (0..2).joinToString("  ") { if (it < count) "★" else "☆" },
        fontSize = size.sp,
        color = Gold,
        modifier = Modifier.semantics { contentDescription = count.toString() + " de 3 estrellas" },
    )
}
