package com.canadulce.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.Screen
import com.canadulce.app.ui.components.LabelChip
import com.canadulce.app.ui.components.PrimaryButton
import com.canadulce.app.ui.components.SecondaryButton
import com.canadulce.app.ui.components.StoryArt
import com.canadulce.app.ui.theme.Forest
import com.canadulce.app.ui.theme.Leaf
import com.canadulce.app.ui.theme.Mint

@Composable
internal fun WelcomeScreen(vm: AppViewModel) {
    Spacer(Modifier.height(10.dp))
    Box(
        Modifier.fillMaxWidth()
            .height(235.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(listOf(Mint, Color(0xFFF1F5DB))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        StoryArt(Modifier.size(210.dp))
        Text(
            "UN PEQUEÑO PASO, UNA GRAN HISTORIA",
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            color = Forest,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
        )
    }
    Text("Cada lectura,\nuna aventura.", style = MaterialTheme.typography.headlineLarge)
    Text(
        "Descubre historias, supera misiones y haz crecer tus logros.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LabelChip("3.º y 4.º de primaria")
    PrimaryButton("Soy estudiante") { vm.loginScreen(false) }
    SecondaryButton("Soy docente") { vm.loginScreen(true) }
    Text(
        "Institución Educativa Caña Dulce",
        fontSize = 12.sp,
        color = Leaf,
        modifier = Modifier.fillMaxWidth().wrapContentWidth(),
    )
    TextButton(onClick = { vm.show(Screen.CONNECTION) }, modifier = Modifier.fillMaxWidth()) {
        Text("Configurar conexión de aula", fontSize = 12.sp)
    }
}
