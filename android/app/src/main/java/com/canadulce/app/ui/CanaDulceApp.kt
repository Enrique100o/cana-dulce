package com.canadulce.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canadulce.app.AppViewModel
import com.canadulce.app.Screen
import com.canadulce.app.ui.components.LeafMark
import com.canadulce.app.ui.components.StatusMessage
import com.canadulce.app.ui.screens.ReadingDetailScreen
import com.canadulce.app.ui.screens.auth.ConnectionScreen
import com.canadulce.app.ui.screens.auth.LoginScreen
import com.canadulce.app.ui.screens.auth.RegistrationScreen
import com.canadulce.app.ui.screens.auth.WelcomeScreen
import com.canadulce.app.ui.screens.student.MissionsScreen
import com.canadulce.app.ui.screens.student.ProgressScreen
import com.canadulce.app.ui.screens.student.QuizScreen
import com.canadulce.app.ui.screens.student.ReadingScreen
import com.canadulce.app.ui.screens.student.ResultScreen
import com.canadulce.app.ui.screens.teacher.QuestionEditorScreen
import com.canadulce.app.ui.screens.teacher.QuestionsScreen
import com.canadulce.app.ui.screens.teacher.ReadingEditorScreen
import com.canadulce.app.ui.screens.teacher.StudentsScreen
import com.canadulce.app.ui.screens.teacher.TeacherReadingsScreen
import com.canadulce.app.ui.theme.Cream
import com.canadulce.app.ui.theme.Leaf

@Composable
fun CanaDulceApp(vm: AppViewModel) {
    val s = vm.state
    var confirmation by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val homeScreens =
        setOf(Screen.MISSIONS, Screen.TEACHER_READINGS, Screen.PROGRESS, Screen.STUDENTS)
    fun back() {
        if (s.screen == Screen.QUIZ)
            confirmation =
                "Podrás retomar esta misión después. Al retomarla tendrás que seleccionar " +
                    "las respuestas nuevamente." to { vm.back() }
        else vm.back()
    }
    BackHandler(
        enabled =
            s.screen != Screen.WELCOME &&
                s.screen !in setOf(Screen.MISSIONS, Screen.TEACHER_READINGS),
    ) {
        back()
    }
    Scaffold(
        containerColor = Cream,
        topBar = {
            Surface(color = Cream) {
                Row(
                    Modifier.fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (
                        s.screen != Screen.WELCOME &&
                            s.screen !in setOf(Screen.MISSIONS, Screen.TEACHER_READINGS)
                    )
                        TextButton(onClick = { back() }, enabled = !s.busy, modifier = Modifier) {
                            Text("‹", fontSize = 30.sp)
                        }
                    else LeafMark(Modifier.size(30.dp))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("Caña Dulce", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (vm.teacher) "ESPACIO DOCENTE" else "AVENTURAS DE LECTURA",
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            color = Leaf,
                        )
                    }
                    if (s.session != null)
                        TextButton(
                            enabled = !s.busy,
                            onClick = {
                                confirmation =
                                    "¿Quieres cerrar la sesión? Podrás volver a ingresar con tus datos." to
                                        {
                                            vm.logout()
                                        }
                            },
                            modifier = Modifier,
                        ) {
                            Text("Salir", fontSize = 14.sp)
                        }
                }
            }
        },
        bottomBar = {
            if (s.session != null && s.screen in homeScreens) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = s.screen in setOf(Screen.MISSIONS, Screen.TEACHER_READINGS),
                        enabled = !s.busy,
                        onClick = vm::home,
                        icon = { Text("▤", fontSize = 23.sp) },
                        label = { Text(if (vm.teacher) "Lecturas" else "Misiones") },
                        modifier = Modifier,
                    )
                    NavigationBarItem(
                        selected = s.screen in setOf(Screen.PROGRESS, Screen.STUDENTS),
                        enabled = !s.busy,
                        onClick = { if (vm.teacher) vm.students() else vm.progress() },
                        icon = { Text("★", fontSize = 23.sp) },
                        label = { Text(if (vm.teacher) "Resultados" else "Mi progreso") },
                        modifier = Modifier,
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                AnimatedVisibility(s.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Leaf)
                }
                key(s.screen, s.reading?.id.takeIf { s.screen == Screen.DETAIL }) {
                    Column(
                        Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(horizontal = 22.dp)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (s.error != null)
                            StatusMessage(s.error, true) {
                                TextButton(onClick = vm::clearError, enabled = !s.busy) {
                                    Text("Cerrar")
                                }
                            }
                        if (s.notice != null) StatusMessage(s.notice, false)
                        when (s.screen) {
                            Screen.WELCOME -> WelcomeScreen(vm)
                            Screen.LOGIN -> LoginScreen(vm)
                            Screen.REGISTER -> RegistrationScreen(vm)
                            Screen.CONNECTION -> ConnectionScreen(vm)
                            Screen.MISSIONS -> MissionsScreen(vm)
                            Screen.DETAIL ->
                                ReadingDetailScreen(vm) { text, action ->
                                    confirmation = text to action
                                }
                            Screen.READ -> ReadingScreen(vm)
                            Screen.QUIZ -> QuizScreen(vm)
                            Screen.RESULT -> ResultScreen(vm)
                            Screen.PROGRESS,
                            Screen.STUDENT_PROGRESS -> ProgressScreen(vm)
                            Screen.TEACHER_READINGS -> TeacherReadingsScreen(vm)
                            Screen.READING_EDITOR -> ReadingEditorScreen(vm)
                            Screen.QUESTIONS ->
                                QuestionsScreen(vm) { text, action ->
                                    confirmation = text to action
                                }
                            Screen.QUESTION_EDITOR -> QuestionEditorScreen(vm)
                            Screen.STUDENTS -> StudentsScreen(vm)
                        }
                    }
                }
            }
            if (s.busy)
                Box(
                    Modifier.fillMaxSize().background(Cream.copy(alpha = .25f)).clickable(
                        interactionSource =
                            remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            },
                        indication = null,
                    ) {},
                )
        }
    }
    confirmation?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Antes de continuar") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                        action()
                    },
                    modifier = Modifier,
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancelar") } },
        )
    }
}
