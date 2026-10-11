package com.canadulce.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canadulce.app.*
import com.canadulce.app.data.*
import com.canadulce.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private fun number(value: Double?): String = value?.let {
    NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).apply { maximumFractionDigits = 2 }.format(it)
} ?: "—"
private fun label(value: String) = when (value) {
    "BAJA" -> "Baja"; "MEDIA" -> "Media"; "ALTA" -> "Alta"; "ACTIVA" -> "Activa"; "INACTIVA" -> "Inactiva"
    "LITERAL" -> "Literal"; "INFERENCIAL" -> "Inferencial"; "IDEA_PRINCIPAL" -> "Idea principal"
    "VOCABULARIO" -> "Vocabulario"; "SECUENCIA" -> "Secuencia"; else -> value
}

@Composable
fun CanaDulceApp(vm: AppViewModel) {
    val s = vm.state
    var confirmation by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val homeScreens = setOf(Screen.MISSIONS, Screen.TEACHER_READINGS, Screen.PROGRESS, Screen.STUDENTS)
    fun back() {
        if (s.screen == Screen.QUIZ) confirmation = "Podrás retomar esta misión después. Al retomarla tendrás que seleccionar las respuestas nuevamente." to { vm.back() }
        else vm.back()
    }
    BackHandler(enabled = s.screen != Screen.WELCOME && s.screen !in setOf(Screen.MISSIONS, Screen.TEACHER_READINGS)) { back() }
    Scaffold(
        containerColor = Cream,
        topBar = {
            Surface(color = Cream) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (s.screen != Screen.WELCOME && s.screen !in setOf(Screen.MISSIONS, Screen.TEACHER_READINGS))
                        TextButton(onClick = { back() }, enabled = !s.busy, modifier = Modifier.testTag("back")) { Text("‹", fontSize = 30.sp) }
                    else LeafMark(Modifier.size(30.dp))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("Caña Dulce", style = MaterialTheme.typography.titleMedium)
                        Text(if (vm.teacher) "ESPACIO DOCENTE" else "AVENTURAS DE LECTURA", fontSize = 10.sp, letterSpacing = 1.5.sp, color = Leaf)
                    }
                    if (s.session != null) TextButton(enabled = !s.busy, onClick = {
                        confirmation = "¿Quieres cerrar la sesión? Podrás volver a ingresar con tus datos." to { vm.logout() }
                    }, modifier = Modifier.testTag("logout")) { Text("Salir", fontSize = 14.sp) }
                }
            }
        },
        bottomBar = {
            if (s.session != null && s.screen in homeScreens) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    NavigationBarItem(selected = s.screen in setOf(Screen.MISSIONS, Screen.TEACHER_READINGS), enabled = !s.busy,
                        onClick = vm::home, icon = { Text("▤", fontSize = 23.sp) }, label = { Text(if (vm.teacher) "Lecturas" else "Misiones") },
                        modifier = Modifier.testTag("nav_missions"))
                    NavigationBarItem(selected = s.screen in setOf(Screen.PROGRESS, Screen.STUDENTS), enabled = !s.busy,
                        onClick = { if (vm.teacher) vm.students() else vm.progress() },
                        icon = { Text("★", fontSize = 23.sp) }, label = { Text(if (vm.teacher) "Resultados" else "Mi progreso") },
                        modifier = Modifier.testTag("nav_progress"))
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                AnimatedVisibility(s.busy) { LinearProgressIndicator(Modifier.fillMaxWidth().testTag("loading"), color = Leaf) }
                key(s.screen, s.reading?.id.takeIf { s.screen == Screen.DETAIL }) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 22.dp).padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (s.error != null) Message(s.error, true, "error") {
                            TextButton(onClick = vm::clearError, enabled = !s.busy) { Text("Cerrar") }
                        }
                        if (s.notice != null) Message(s.notice, false)
                        when (s.screen) {
                            Screen.WELCOME -> Welcome(vm)
                            Screen.LOGIN -> Login(vm)
                            Screen.REGISTER -> Register(vm)
                            Screen.CONNECTION -> Connection(vm)
                            Screen.MISSIONS -> Missions(vm)
                            Screen.DETAIL -> Detail(vm) { text, action -> confirmation = text to action }
                            Screen.READ -> Reader(vm)
                            Screen.QUIZ -> Quiz(vm)
                            Screen.RESULT -> Result(vm)
                            Screen.PROGRESS, Screen.STUDENT_PROGRESS -> ProgressPage(vm)
                            Screen.TEACHER_READINGS -> TeacherReadings(vm)
                            Screen.READING_EDITOR -> ReadingEditor(vm)
                            Screen.QUESTIONS -> QuestionsPage(vm) { text, action -> confirmation = text to action }
                            Screen.QUESTION_EDITOR -> QuestionEditor(vm)
                            Screen.STUDENTS -> Students(vm)
                        }
                    }
                }
            }
            if (s.busy) Box(Modifier.fillMaxSize().background(Cream.copy(alpha = .25f)).clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {})
        }
    }
    confirmation?.let { (message, action) ->
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text("Antes de continuar") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { confirmation = null; action() }, modifier = Modifier.testTag("confirm")) { Text("Continuar") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun Welcome(vm: AppViewModel) {
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth().height(235.dp).clip(RoundedCornerShape(30.dp)).background(
        Brush.verticalGradient(listOf(Mint, Color(0xFFF1F5DB)))), contentAlignment = Alignment.Center) {
        StoryArt(Modifier.size(210.dp))
        Text("UN PEQUEÑO PASO, UNA GRAN HISTORIA", fontSize = 9.sp, letterSpacing = 1.sp, color = Forest,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }
    Text("Cada lectura,\nuna aventura.", style = MaterialTheme.typography.headlineLarge)
    Text("Descubre historias, supera misiones y haz crecer tus logros.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Tag("3.º y 4.º de primaria")
    Action("Soy estudiante", "student_role") { vm.loginScreen(false) }
    Secondary("Soy docente", "teacher_role") { vm.loginScreen(true) }
    Text("Institución Educativa Caña Dulce", fontSize = 12.sp, color = Leaf, modifier = Modifier.fillMaxWidth().wrapContentWidth())
    TextButton(onClick = { vm.show(Screen.CONNECTION) }, modifier = Modifier.fillMaxWidth()) { Text("Configurar conexión de aula", fontSize = 12.sp) }
}
@Composable
private fun Login(vm: AppViewModel) {
    val teacher = vm.state.teacherLogin
    var alias by rememberSaveable(teacher) { mutableStateOf("") }
    var code by rememberSaveable(teacher) { mutableStateOf("") }
    Heading(if (teacher) "Tu espacio docente" else "¡Qué bueno verte!", if (teacher) "Prepara lecturas y acompaña los avances." else "Entra y continúa tu aventura lectora.")
    if (!teacher) StoryArt(Modifier.fillMaxWidth().height(125.dp))
    Field(if (teacher) "Usuario" else "Tu alias", alias, { alias = it }, "login_alias")
    Field(if (teacher) "Contraseña" else "Código de acceso", code, { code = it }, "login_code", password = true)
    Action("Ingresar", "login_submit") { vm.login(alias, code) }
    if (!teacher) {
        Secondary("Crear mi alias", "create_alias") { vm.show(Screen.REGISTER) }
        Text("Usa un apodo. No necesitas escribir tu nombre real.", style = MaterialTheme.typography.bodyMedium, color = Leaf)
    }
}
@Composable
private fun Register(vm: AppViewModel) {
    var alias by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableIntStateOf(3) }
    Heading("Tu aventura comienza", "Crea un alias y guarda tu código para volver a ingresar.")
    Field("Elige un alias", alias, { alias = it }, "register_alias")
    ChoiceRow("Tu grado", listOf("3.º", "4.º"), grade - 3) { grade = it + 3 }
    Field("Código de acceso", code, { code = it }, "register_code", password = true)
    Field("Repite tu código", confirmation, { confirmation = it }, "register_confirmation", password = true)
    Message("El código debe tener entre 6 y 32 caracteres. Recuerda tu alias y tu código.", false)
    Action("Crear mi perfil", "register_submit") { vm.register(alias, grade, code, confirmation) }
}
@Composable
private fun Connection(vm: AppViewModel) {
    var url by rememberSaveable { mutableStateOf(vm.state.baseUrl) }
    Heading("Conexión de aula", "Configura la dirección proporcionada para esta aplicación.")
    Field("Dirección de la API", url, { url = it }, "api_url")
    if (BuildConfig.DEBUG) Message("Emulador de este computador: http://10.0.2.2:8080/api. Teléfono por USB con adb reverse: http://127.0.0.1:8080/api.", false)
    Action("Guardar conexión", "api_save") { vm.connection(url) }
}
@Composable
private fun Missions(vm: AppViewModel) {
    val s = vm.state
    var search by rememberSaveable { mutableStateOf("") }
    Heading("¡Hola, " + s.session?.student?.alias + "!", "Hoy puedes descubrir algo nuevo.")
    JourneySummary(s.progress)
    s.attempts.filter { it.status == "EN_PROGRESO" }.forEach { attempt ->
        Panel { Tag("MISIÓN POR CONTINUAR"); Text(attempt.title, style = MaterialTheme.typography.titleMedium)
            Action("Retomar misión", "resume_" + attempt.id) { vm.resume(attempt) } }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Tus misiones", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = vm::home) { Text("Actualizar") }
    }
    Field("Buscar una historia", search, { search = it }, "search")
    Secondary("Buscar", "search_submit") { vm.searchReadings(search) }
    val filtered = s.readings
    if (filtered.isEmpty() && !s.busy) Empty("Aún no hay misiones aquí", "Prueba otra búsqueda o vuelve más tarde.")
    filtered.forEachIndexed { index, r ->
        val best = s.attempts.filter { it.readingId == r.id && it.status == "COMPLETADO" }.maxByOrNull { it.score ?: 0.0 }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(46.dp).background(Mint, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text((index + 1).toString().padStart(2, '0'), fontWeight = FontWeight.Bold, color = Forest) }
                Column(Modifier.weight(1f)) {
                    Text(r.category.ifBlank { "AVENTURA" }.uppercase(), fontSize = 10.sp, letterSpacing = 1.sp, color = Leaf)
                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(r.questionCount.toString() + " retos · Dificultad " + label(r.difficulty).lowercase(), style = MaterialTheme.typography.bodyMedium)
            if (best != null) Stars(best.stars ?: 0, 24)
            Action(if (best == null) "Explorar misión" else "Practicar de nuevo", "mission_" + r.id) { vm.openReading(r.id) }
        }
    }
}
@Composable
private fun Detail(vm: AppViewModel, confirm: (String, () -> Unit) -> Unit) {
    val r = vm.state.reading ?: return
    Heading(r.title, r.category.ifBlank { "Una nueva historia por descubrir" })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Tag(r.grade.toString() + ".º"); Tag(label(r.difficulty)); if (vm.teacher) Tag(label(r.status)) }
    if (vm.teacher) {
        Text(r.content, fontSize = 18.sp, lineHeight = 29.sp)
        if (r.locked) Message("Esta lectura ya tiene intentos. Su contenido se conserva para proteger el historial. Puedes crear otra lectura.", false)
        Action("Administrar preguntas", "manage_questions") { vm.questions() }
        Secondary("Editar contenido", "edit_reading", !r.locked) { vm.editReading() }
        if (r.status == "ACTIVA") Secondary("Desactivar lectura", "deactivate_reading") {
            confirm("Dejará de admitir nuevas misiones. El historial se conserva y los intentos iniciados podrán terminarse.") { vm.setReadingState(false) }
        } else Action("Activar lectura", "activate_reading") { vm.setReadingState(true) }
        Secondary("Crear otra lectura", "new_reading") { vm.newReading() }
    } else {
        StoryArt(Modifier.fillMaxWidth().height(160.dp))
        Panel {
            Text("Tu misión", style = MaterialTheme.typography.titleMedium)
            Text("Lee con calma y responde " + r.questionCount + " retos. Puedes consultar el texto mientras respondes.")
            Stars(3, 27); Text("Hasta 3 estrellas por intento", fontSize = 13.sp, color = Leaf)
        }
        Action("Vamos a leer", "read_mission") { vm.readText() }
    }
}
@Composable
private fun Reader(vm: AppViewModel) {
    val s = vm.state; val r = s.reading ?: return
    Heading(r.title, "Tómate tu tiempo. Disfruta la historia.")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Tamaño del texto", style = MaterialTheme.typography.bodyMedium)
        Row { TextButton(onClick = { vm.textSize(-2) }, enabled = s.readerSize > 18) { Text("A−") }
            TextButton(onClick = { vm.textSize(2) }, enabled = s.readerSize < 28) { Text("A+") } }
    }
    Panel { Text(r.content, fontSize = s.readerSize.sp, lineHeight = (s.readerSize * 1.6).sp, modifier = Modifier.testTag("reading_text")) }
    Action(if (s.returnToQuiz) "Volver a las preguntas" else "¡Listo! Empezar los retos", "start_quiz") {
        if (s.returnToQuiz) vm.show(Screen.QUIZ) else vm.startMission()
    }
}
@Composable
private fun Quiz(vm: AppViewModel) {
    val s = vm.state; val q = s.questions.getOrNull(s.questionIndex) ?: return
    Text("RETO " + (s.questionIndex + 1) + " DE " + s.questions.size, fontSize = 12.sp, color = Leaf, letterSpacing = 1.5.sp)
    LinearProgressIndicator(progress = { (s.questionIndex + 1f) / s.questions.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Leaf, trackColor = Mint)
    Text(q.text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("question_text"))
    Text("Elige una respuesta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    q.options.forEachIndexed { index, option ->
        val selected = s.selections[q.id] == option.id
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (selected) Mint else Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) Forest else Color(0xFFCFD8CC), RoundedCornerShape(18.dp))
            .selectable(selected, role = Role.RadioButton, onClick = { vm.choose(q.id, option.id) }).padding(12.dp).testTag("option_" + index),
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = null); Text(option.text, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (s.questionIndex > 0) OutlinedButton(onClick = { vm.question(-1) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Anterior") }
        Button(onClick = { if (s.questionIndex == s.questions.lastIndex) vm.finish() else vm.question(1) },
            modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("quiz_next"), enabled = q.id in s.selections) {
            Text(if (s.questionIndex == s.questions.lastIndex) "Finalizar misión" else "Siguiente") }
    }
    TextButton(onClick = { vm.readText(true) }, modifier = Modifier.fillMaxWidth().testTag("consult_text")) { Text("Consultar la historia") }
}
@Composable
private fun Result(vm: AppViewModel) {
    val a = vm.state.attempt ?: return
    Heading(if (vm.teacher) "Detalle del intento" else if ((a.score ?: 0.0) >= 80) "¡Tu lectura brilló!" else "¡Misión completada!", a.title)
    Panel(color = Mint) {
        Text(number(a.score) + " / 100", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("result_score"))
        Stars(a.stars ?: 0, 42); Text((a.correct ?: 0).toString() + " de " + a.total + " respuestas correctas")
        Text(if (a.score == 100.0) "¡Comprendiste todos los retos!" else "Cada intento te ayuda a comprender mejor. Sigue practicando.", style = MaterialTheme.typography.bodyMedium)
    }
    if (!vm.teacher) {
        Action("Ver mis logros", "result_progress") { vm.progress() }
        Secondary("Volver a misiones", "result_home") { vm.home() }
    }
    Text("Tus respuestas", style = MaterialTheme.typography.titleLarge)
    a.answers.forEach { answer -> Panel {
        Tag(if (answer.correct) "COMPRENDIDO" else "POR REVISAR")
        Text(answer.text, style = MaterialTheme.typography.titleMedium); Text(answer.chosen)
        if (!answer.correct) Text("Vuelve al texto y piensa en las pistas de la historia.", fontSize = 13.sp, color = Leaf)
    } }
}
@Composable
private fun ProgressPage(vm: AppViewModel) {
    val s = vm.state; val p = s.progress ?: return
    Heading(if (vm.teacher) "Progreso de " + p.alias else "Tu jardín de logros", "Cada historia deja una nueva semilla.")
    JourneySummary(p)
    Panel {
        Text("Así vas creciendo", style = MaterialTheme.typography.titleMedium)
        Text("Promedio: " + number(p.average) + " / 100"); Text("Mejor intento: " + number(p.best) + " / 100")
        Text(p.completed.toString() + (if (p.completed == 1) " intento completado · " else " intentos completados · ") +
            p.missions + if (p.missions == 1) " lectura diferente" else " lecturas diferentes", fontSize = 14.sp)
    }
    Text("Tus insignias", style = MaterialTheme.typography.titleLarge)
    p.badges.forEachIndexed { index, badge -> Panel(color = if (badge.obtained) Mint else Color.White) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeArt(badge.obtained, Modifier.size(58.dp))
            Column(Modifier.weight(1f)) {
                Text(badge.name, style = MaterialTheme.typography.titleMedium)
                Text(when (index) { 0 -> "Completa tu primera lectura."; 1 -> "Completa 3 lecturas diferentes."; else -> "Logra 100 en un intento." }, fontSize = 13.sp, color = Leaf)
                Text(if (badge.obtained) "¡Obtenida!" else "Por descubrir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (badge.obtained) Forest else Color.Gray)
            }
        }
    } }
    Text("Tu recorrido", style = MaterialTheme.typography.titleLarge)
    if (s.attempts.isEmpty()) Empty("Tu primera historia te espera", "Completa una misión para ver tus resultados aquí.")
    s.attempts.forEach { a -> Panel {
        Text(a.title, style = MaterialTheme.typography.titleMedium)
        Text(a.started.take(10) + " · " + if (a.status == "COMPLETADO") number(a.score) + " / 100" else "Por continuar", fontSize = 14.sp)
        if (a.status == "COMPLETADO") { Stars(a.stars ?: 0, 23); Secondary("Ver respuestas", "attempt_" + a.id) { vm.result(a.id) } }
        else if (!vm.teacher) Secondary("Retomar misión", "resume_" + a.id) { vm.resume(a) }
    } }
}
@Composable
private fun TeacherReadings(vm: AppViewModel) {
    var search by rememberSaveable { mutableStateOf("") }; var grade by rememberSaveable { mutableIntStateOf(0) }; var status by rememberSaveable { mutableIntStateOf(0) }
    Heading("Historias para aprender", "Prepara las misiones de tu aula.")
    Action("Nueva lectura", "new_reading") { vm.newReading() }
    Field("Buscar por título", search, { search = it }, "search")
    ChoiceRow("Grado", listOf("Todos", "3.º", "4.º"), grade) { grade = it }
    ChoiceRow("Estado", listOf("Todos", "Activas", "Inactivas"), status) { status = it }
    Secondary("Aplicar filtros", "filter_readings") { vm.searchReadings(search, if (grade == 0) null else grade + 2,
        if (status == 0) null else if (status == 1) "ACTIVA" else "INACTIVA") }
    val readings = vm.state.readings
    if (readings.isEmpty() && !vm.state.busy) Empty("Sin lecturas en esta consulta", "Crea una nueva o cambia los filtros.")
    readings.forEach { r -> Panel {
        Text(r.title, style = MaterialTheme.typography.titleMedium)
        Text(r.grade.toString() + ".º · " + label(r.status) + " · " + r.questionCount + " preguntas", fontSize = 14.sp)
        if (r.locked) Tag("Contenido con historial")
        Secondary("Administrar", "admin_" + r.id) { vm.openReading(r.id) }
    } }
    Secondary("Actualizar lecturas", "refresh_readings") { vm.home() }
}
@Composable
private fun ReadingEditor(vm: AppViewModel) {
    val d = vm.state.readingDraft
    Heading(if (d.id == null) "Nueva lectura" else "Editar lectura", "Una lectura será una misión para los estudiantes.")
    Field("Título", d.title, { vm.draft(d.copy(title = it)) }, "reading_title")
    Field("Texto de la historia", d.content, { vm.draft(d.copy(content = it)) }, "reading_content", multiline = true)
    ChoiceRow("Grado", listOf("3.º", "4.º"), d.grade - 3) { vm.draft(d.copy(grade = it + 3)) }
    ChoiceRow("Dificultad", listOf("Baja", "Media", "Alta"), listOf("BAJA", "MEDIA", "ALTA").indexOf(d.difficulty)) { vm.draft(d.copy(difficulty = listOf("BAJA", "MEDIA", "ALTA")[it])) }
    Field("Categoría (opcional)", d.category, { vm.draft(d.copy(category = it)) }, "reading_category")
    Message(if (d.id == null) "Se guardará inactiva. Agrega preguntas y actívala cuando esté lista." else "El estado actual se conserva. Puedes cambiarlo desde el detalle de la lectura.", false)
    Action("Guardar lectura", "save_reading") { vm.saveReading() }
}
@Composable
private fun QuestionsPage(vm: AppViewModel, confirm: (String, () -> Unit) -> Unit) {
    val s = vm.state; val r = s.reading ?: return
    Heading("Los retos de la misión", r.title); Tag(label(r.status))
    if (r.locked) Message("Esta misión tiene intentos. Las preguntas y opciones se conservan para mantener el historial.", false)
    else Action("Nueva pregunta", "new_question") { vm.newQuestion() }
    if (s.questions.isEmpty()) Empty("Agrega el primer reto", "Cada pregunta debe tener entre 2 y 4 opciones y una respuesta correcta.")
    s.questions.forEach { q -> Panel {
        Text(q.order.toString() + ". " + q.text, style = MaterialTheme.typography.titleMedium)
        Text(label(q.type) + " · " + label(q.status), fontSize = 13.sp, color = Leaf)
        q.options.forEach { Text((if (it.correct) "✓ " else "• ") + it.text, fontSize = 15.sp) }
        if (!r.locked) {
            Secondary("Editar pregunta", "edit_question_" + q.id) { vm.editQuestion(q) }
            if (q.status == "ACTIVA") TextButton(onClick = {
                confirm("Esta pregunta dejará de aparecer en nuevos intentos. La lectura debe conservar al menos una pregunta activa.") { vm.deactivateQuestion(q.id) }
            }) { Text("Desactivar pregunta") }
        }
    } }
    if (r.status == "INACTIVA") Action("Activar lectura", "activate_reading", r.questionCount > 0) { vm.setReadingState(true) }
    Secondary("Volver a la lectura", "question_back") { vm.show(Screen.DETAIL) }
}
@Composable
private fun QuestionEditor(vm: AppViewModel) {
    val d = vm.state.questionDraft; val types = listOf("LITERAL", "INFERENCIAL", "IDEA_PRINCIPAL", "VOCABULARIO", "SECUENCIA")
    Heading(if (d.id == null) "Nuevo reto" else "Editar reto", "Escribe opciones claras y marca una correcta.")
    Field("Enunciado", d.text, { vm.draft(d.copy(text = it)) }, "question_prompt", multiline = true)
    Dropdown("Habilidad", types.map(::label), types.indexOf(d.type)) { vm.draft(d.copy(type = types[it])) }
    Field("Orden", d.order, { vm.draft(d.copy(order = it)) }, "question_order", numeric = true)
    if (d.id != null) ChoiceRow("Estado", listOf("Activa", "Inactiva"), if (d.status == "ACTIVA") 0 else 1) { vm.draft(d.copy(status = if (it == 0) "ACTIVA" else "INACTIVA")) }
    Text("Opciones · Marca la correcta", style = MaterialTheme.typography.titleMedium)
    d.options.forEachIndexed { index, value -> Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(d.correctIndex == index, onClick = { vm.draft(d.copy(correctIndex = index)) }, modifier = Modifier.testTag("correct_" + index))
        Field("Opción " + (index + 1), value, { text -> vm.draft(d.copy(options = d.options.toMutableList().apply { this[index] = text })) },
            "question_option_" + index, modifier = Modifier.weight(1f))
    } }
    Row {
        TextButton(enabled = d.options.size < 4, onClick = { vm.draft(d.copy(options = d.options + "")) }) { Text("Agregar opción") }
        TextButton(enabled = d.options.size > 2, onClick = { vm.draft(d.copy(options = d.options.dropLast(1), correctIndex = d.correctIndex.coerceAtMost(d.options.size - 2))) }) { Text("Quitar última") }
    }
    Action("Guardar pregunta", "save_question") { vm.saveQuestion() }
}
@Composable
private fun Students(vm: AppViewModel) {
    var search by rememberSaveable { mutableStateOf("") }; var grade by rememberSaveable { mutableIntStateOf(0) }
    Heading("Avances del aula", "Consulta los logros y el historial por estudiante.")
    Field("Buscar por alias", search, { search = it }, "student_search")
    ChoiceRow("Grado", listOf("Todos", "3.º", "4.º"), grade) { grade = it }
    Secondary("Buscar estudiantes", "filter_students") { vm.searchStudents(search, if (grade == 0) null else grade + 2) }
    val students = vm.state.students
    if (students.isEmpty()) Empty("Sin estudiantes en esta consulta", "Prueba otro alias o grado.")
    students.forEach { student -> Panel {
        Text(student.alias, style = MaterialTheme.typography.titleMedium); Text(student.grade.toString() + ".º de primaria", color = Leaf)
        Secondary("Consultar progreso", "student_" + student.id) { vm.progress(student) }
    } }
}

@Composable private fun JourneySummary(p: Progress?) {
    Panel(color = Forest) {
        Text("TU AVENTURA SIGUE CRECIENDO", fontSize = 10.sp, letterSpacing = 1.sp, color = Color(0xFFD9E9C8))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(number(p?.points ?: 0.0), "Puntos"); Metric((p?.stars ?: 0).toString(), "Estrellas"); Metric((p?.missions ?: 0).toString(), "Misiones")
        }
    }
}
@Composable private fun Metric(value: String, title: String) {
    Column { Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(title, fontSize = 12.sp, color = Color(0xFFD9E9C8)) }
}
@Composable private fun Heading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun Panel(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
@Composable private fun Tag(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Forest, modifier = Modifier.background(Mint, RoundedCornerShape(9.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
}
@Composable private fun Action(text: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Button({ focus.clearFocus(); keyboard?.hide(); onClick() }, enabled = enabled, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag(tag), contentPadding = PaddingValues(16.dp)) { Text(text) }
}
@Composable private fun Secondary(text: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedButton({ focus.clearFocus(); keyboard?.hide(); onClick() }, enabled = enabled, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag(tag), contentPadding = PaddingValues(14.dp)) { Text(text) }
}
@Composable private fun Field(label: String, value: String, onChange: (String) -> Unit, tag: String, modifier: Modifier = Modifier,
    password: Boolean = false, multiline: Boolean = false, numeric: Boolean = false) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = !multiline, minLines = if (multiline) 4 else 1,
        modifier = modifier.fillMaxWidth().testTag(tag), shape = RoundedCornerShape(14.dp),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else if (numeric) KeyboardType.Number else KeyboardType.Text),
        trailingIcon = if (password) { { TextButton(onClick = { visible = !visible }) { Text(if (visible) "Ocultar" else "Ver", fontSize = 12.sp) } } } else null)
}
@Composable private fun ChoiceRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column { Text(title, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, text -> FilterChip(selected == i, onClick = { onSelect(i) }, label = { Text(text) }) }
        }
    }
}
@Composable private fun Dropdown(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column { Text(title, style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(options.getOrElse(selected) { "Selecciona" } + "  ▾") }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { i, text -> DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(i); expanded = false }) }
            }
        }
    }
}
@Composable private fun Message(message: String, error: Boolean, tag: String = "notice", extra: @Composable RowScope.() -> Unit = {}) {
    Surface(color = if (error) Color(0xFFFFE9E3) else Mint, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.padding(14.dp)) {
            Text(message, color = if (error) Color(0xFF842C23) else Forest, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Row(content = extra)
        }
    }
}
@Composable private fun Empty(title: String, subtitle: String) { Panel { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = Leaf) } }
@Composable private fun Stars(count: Int, size: Int) {
    Text((0..2).joinToString("  ") { if (it < count) "★" else "☆" }, fontSize = size.sp, color = Gold,
        modifier = Modifier.semantics { contentDescription = count.toString() + " de 3 estrellas" })
}
@Composable private fun LeafMark(modifier: Modifier) {
    Canvas(modifier) {
        drawOval(Leaf, Offset(size.width * .1f, size.height * .1f), Size(size.width * .75f, size.height * .6f))
        drawLine(Forest, Offset(size.width * .2f, size.height * .85f), Offset(size.width * .7f, size.height * .25f), strokeWidth = 3f)
    }
}
/** Ilustración nativa de un libro y una planta que crece con las historias. */
@Composable private fun StoryArt(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "semilla")
    val tilt by transition.animateFloat(-2f, 2f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "balanceo")
    Canvas(modifier.rotate(tilt).semantics { contentDescription = "Un libro abierto del que crece una planta sonriente" }) {
        val w = size.width; val h = size.height; val cx = w / 2
        drawCircle(Color(0xFFFFE8AD), h * .33f, Offset(cx, h * .4f))
        drawCircle(Color(0xFFE0EACB), h * .14f, Offset(cx - h * .4f, h * .62f))
        drawCircle(Color(0xFFD4E8C4), h * .1f, Offset(cx + h * .38f, h * .35f))
        val book = Path().apply {
            moveTo(cx, h * .68f); quadraticTo(cx - h * .2f, h * .52f, cx - h * .4f, h * .58f)
            lineTo(cx - h * .4f, h * .84f); quadraticTo(cx - h * .2f, h * .8f, cx, h * .93f)
            quadraticTo(cx + h * .2f, h * .8f, cx + h * .4f, h * .84f); lineTo(cx + h * .4f, h * .58f)
            quadraticTo(cx + h * .2f, h * .52f, cx, h * .68f); close()
        }
        drawPath(book, Forest); drawPath(book, Color.White, style = Stroke(h * .017f))
        drawLine(Color(0xFFB8D5A5), Offset(cx, h * .68f), Offset(cx, h * .91f), h * .013f)
        repeat(3) { i ->
            drawLine(Color(0xFF9DBA96), Offset(cx - h * .32f, h * (.65f + i * .055f)), Offset(cx - h * .08f, h * (.71f + i * .055f)), h * .009f)
            drawLine(Color(0xFF9DBA96), Offset(cx + h * .08f, h * (.71f + i * .055f)), Offset(cx + h * .32f, h * (.65f + i * .055f)), h * .009f)
        }
        drawLine(Leaf, Offset(cx, h * .66f), Offset(cx, h * .28f), h * .038f, cap = StrokeCap.Round)
        drawOval(Leaf, Offset(cx - h * .24f, h * .22f), Size(h * .26f, h * .16f))
        drawOval(Color(0xFF7CA75C), Offset(cx, h * .13f), Size(h * .26f, h * .19f))
        drawCircle(Color(0xFFA9C97E), h * .105f, Offset(cx, h * .48f))
        drawCircle(Ink, h * .012f, Offset(cx - h * .038f, h * .46f)); drawCircle(Ink, h * .012f, Offset(cx + h * .038f, h * .46f))
        drawArc(Ink, 10f, 160f, false, Offset(cx - h * .035f, h * .48f), Size(h * .07f, h * .045f), style = Stroke(h * .01f))
    }
}
@Composable private fun BadgeArt(obtained: Boolean, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = if (obtained) "Insignia obtenida" else "Insignia por descubrir" }) {
        drawCircle(if (obtained) Color(0xFFF6C762) else Color(0xFFE4E7DE), size.minDimension / 2)
        drawCircle(if (obtained) Gold else Color.Gray, size.minDimension * .4f, style = Stroke(2.dp.toPx()))
        val center = Offset(size.width / 2, size.height / 2); val star = Path()
        repeat(10) { i ->
            val radius = size.minDimension * if (i % 2 == 0) .27f else .12f; val angle = (-90 + i * 36) * Math.PI / 180
            val x = center.x + cos(angle).toFloat() * radius; val y = center.y + sin(angle).toFloat() * radius
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close(); drawPath(star, if (obtained) Forest else Color.Gray)
    }
}
