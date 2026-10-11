package com.canadulce.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.canadulce.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

enum class Screen { WELCOME, LOGIN, REGISTER, CONNECTION, MISSIONS, DETAIL, READ, QUIZ, RESULT, PROGRESS, TEACHER_READINGS, READING_EDITOR, QUESTIONS, QUESTION_EDITOR, STUDENTS, STUDENT_PROGRESS }
data class AppState(
    val screen: Screen = Screen.WELCOME, val session: Session? = null, val teacherLogin: Boolean = false,
    val busy: Boolean = false, val error: String? = null, val notice: String? = null,
    val readings: List<Reading> = emptyList(), val reading: Reading? = null,
    val questions: List<Question> = emptyList(), val attempt: Attempt? = null,
    val selections: Map<Int, Int> = emptyMap(), val questionIndex: Int = 0,
    val progress: Progress? = null, val attempts: List<Attempt> = emptyList(),
    val students: List<Student> = emptyList(), val selectedStudent: Student? = null,
    val readingDraft: ReadingDraft = ReadingDraft(), val questionDraft: QuestionDraft = QuestionDraft(),
    val baseUrl: String = "", val readerSize: Int = 20, val returnToQuiz: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("cana_connection", 0)
    private val api = ApiClient(preferences.getString("baseUrl", BuildConfig.API_BASE_URL).orEmpty())
    var state by mutableStateOf(AppState(baseUrl = api.baseUrl))
        private set
    private var retryAction: (suspend () -> Unit)? = null
    val teacher get() = state.session?.role == "DOCENTE"

    private fun run(action: suspend () -> Unit) {
        if (state.busy) return
        retryAction = action
        viewModelScope.launch {
            state = state.copy(busy = true, error = null, notice = null)
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (error: ApiException) {
                if (state.session != null && error.status == 401) {
                    val wasTeacher = teacher
                    api.token = ""
                    retryAction = null
                    state = AppState(screen = Screen.LOGIN, teacherLogin = wasTeacher, baseUrl = api.baseUrl,
                        error = "Tu sesión terminó. Vuelve a ingresar.")
                } else { state = state.copy(error = error.message) }
            }
            catch (_: IOException) { state = state.copy(error = "No pudimos conectar con el aula. Comprueba la conexión y vuelve a intentar.") }
            catch (_: Exception) { state = state.copy(error = "No pudimos cargar los datos. Vuelve a intentar.") }
            finally { state = state.copy(busy = false) }
        }
    }
    fun retry() { retryAction?.let(::run) }
    fun clearError() { state = state.copy(error = null) }
    fun message(value: String) { state = state.copy(error = value) }
    fun show(screen: Screen) { if (!state.busy) state = state.copy(screen = screen, error = null, notice = null) }
    fun loginScreen(teacher: Boolean) { state = state.copy(screen = Screen.LOGIN, teacherLogin = teacher, error = null, notice = null) }
    fun logout() {
        if (state.busy) return
        api.token = ""
        retryAction = null
        state = AppState(baseUrl = api.baseUrl)
    }
    fun connection(value: String) {
        val normalized = value.trim().trimEnd('/')
        if (!ApiClient.validBaseUrl(normalized, BuildConfig.DEBUG)) { message("Usa una dirección HTTPS que termine en /api. En pruebas locales se admite 10.0.2.2 o 127.0.0.1."); return }
        api.baseUrl = normalized
        preferences.edit().putString("baseUrl", normalized).apply()
        state = state.copy(baseUrl = normalized, screen = Screen.WELCOME, error = null, notice = "Conexión de aula guardada.")
    }
    fun login(alias: String, code: String) {
        if (alias.isBlank() || code.isBlank()) { message("Completa los dos campos para ingresar."); return }
        run {
            val session = if (state.teacherLogin) api.loginTeacher(alias, code) else api.loginStudent(alias, code)
            api.token = session.token
            state = state.copy(session = session, screen = if (session.role == "DOCENTE") Screen.TEACHER_READINGS else Screen.MISSIONS)
            loadHome()
        }
    }
    fun register(alias: String, grade: Int, code: String, confirmation: String) {
        InputRules.registration(alias, code, confirmation)?.let { message(it); return }
        run {
            api.register(alias, grade, code)
            // No se reintenta el registro después de crear el perfil: si falla el ingreso, se vuelve al login.
            state = state.copy(screen = Screen.LOGIN, teacherLogin = false, notice = "Alias creado. Ingresa con tu alias y código.")
            retryAction = null
        }
    }
    private suspend fun loadHome() {
        val readings = api.readings()
        val attempts = if (teacher) emptyList() else api.attempts()
        val progress = if (teacher) null else state.session?.student?.let { api.progress(it.id) }
        state = state.copy(readings = readings, attempts = attempts, progress = progress)
    }
    fun home() = run {
        state = state.copy(screen = if (teacher) Screen.TEACHER_READINGS else Screen.MISSIONS)
        loadHome()
    }
    fun searchReadings(query: String, grade: Int? = null, status: String? = null) = run {
        state = state.copy(readings = api.readings(query, grade, status))
    }
    fun searchStudents(query: String, grade: Int? = null) = run {
        state = state.copy(students = api.students(query, grade))
    }
    fun openReading(id: Int) = run {
        val reading = api.reading(id)
        state = state.copy(reading = reading, screen = Screen.DETAIL, returnToQuiz = false)
    }
    fun readText(fromQuiz: Boolean = false) {
        state = state.copy(screen = Screen.READ, returnToQuiz = fromQuiz, error = null)
    }
    fun textSize(delta: Int) { state = state.copy(readerSize = (state.readerSize + delta).coerceIn(18, 28)) }
    fun startMission() = run {
        val reading = state.reading ?: return@run
        val attempt = try { api.start(reading.id) } catch (e: ApiException) {
            if (e.code == "INTENTO_EN_PROGRESO_EXISTENTE" && e.existingAttempt != null) api.attempt(e.existingAttempt) else throw e
        }
        val questions = api.questions(reading.id)
        check(questions.isNotEmpty())
        state = state.copy(attempt = attempt, questions = questions, selections = emptyMap(), questionIndex = 0,
            screen = Screen.QUIZ, returnToQuiz = false)
    }
    fun resume(attempt: Attempt) = run {
        val current = api.attempt(attempt.id)
        val reading = api.reading(current.readingId)
        val questions = api.questions(current.readingId)
        state = state.copy(reading = reading, attempt = current, questions = questions, selections = emptyMap(),
            questionIndex = 0, screen = Screen.QUIZ, returnToQuiz = false)
    }
    fun choose(questionId: Int, optionId: Int) { if (!state.busy) state = state.copy(selections = state.selections + (questionId to optionId), error = null) }
    fun question(delta: Int) {
        val current = state.questions.getOrNull(state.questionIndex) ?: return
        if (delta > 0 && current.id !in state.selections) { message("Elige una respuesta para continuar."); return }
        state = state.copy(questionIndex = (state.questionIndex + delta).coerceIn(0, state.questions.lastIndex), error = null)
    }
    fun finish() {
        if (!InputRules.complete(state.questions, state.selections)) { message("Responde todas las preguntas antes de finalizar."); return }
        val attemptId = state.attempt?.id ?: return
        val answers = state.selections.toMap()
        run {
            val result = api.finish(attemptId, answers)
            state = state.copy(attempt = result, screen = Screen.RESULT, selections = emptyMap())
            retryAction = null
        }
    }
    fun progress(student: Student? = null) = run {
        val target = student ?: state.session?.student ?: return@run
        val progress = api.progress(target.id)
        val attempts = api.attempts(if (teacher) target.id else null)
        state = state.copy(progress = progress, attempts = attempts, selectedStudent = target,
            screen = if (teacher) Screen.STUDENT_PROGRESS else Screen.PROGRESS)
    }
    fun result(id: Int) = run { state = state.copy(attempt = api.attempt(id), screen = Screen.RESULT) }
    fun students() = run { state = state.copy(students = api.students(), screen = Screen.STUDENTS) }
    fun newReading() { state = state.copy(readingDraft = ReadingDraft(), screen = Screen.READING_EDITOR, error = null) }
    fun editReading() {
        val r = state.reading ?: return
        if (r.locked) { message("Esta lectura ya tiene intentos. Crea otra para cambiar su contenido."); return }
        state = state.copy(readingDraft = ReadingDraft(r.id, r.title, r.content, r.grade, r.difficulty, r.category, r.status),
            screen = Screen.READING_EDITOR, error = null)
    }
    fun draft(value: ReadingDraft) { state = state.copy(readingDraft = value) }
    fun saveReading() {
        val d = state.readingDraft
        if (d.title.trim().length !in 3..120 || d.content.trim().length !in 20..8000 || d.category.trim().length > 50) {
            message("Revisa el título (3-120), el texto (20-8000) y la categoría (hasta 50 caracteres)."); return
        }
        run {
            val saved = api.saveReading(d)
            val questions = api.questions(saved.id, true)
            state = state.copy(reading = saved, questions = questions, screen = Screen.QUESTIONS, notice = "Lectura guardada.")
        }
    }
    fun questions() = run {
        val r = state.reading ?: return@run
        state = state.copy(reading = api.reading(r.id), questions = api.questions(r.id, true), screen = Screen.QUESTIONS)
    }
    fun newQuestion() { state = state.copy(questionDraft = QuestionDraft(order = ((state.questions.maxOfOrNull { it.order } ?: 0) + 1).toString()),
        screen = Screen.QUESTION_EDITOR, error = null) }
    fun editQuestion(q: Question) { state = state.copy(questionDraft = QuestionDraft(q.id, q.text, q.type, q.order.toString(), q.status,
        q.options.map { it.text }, q.options.indexOfFirst { it.correct }.coerceAtLeast(0)), screen = Screen.QUESTION_EDITOR, error = null) }
    fun draft(value: QuestionDraft) { state = state.copy(questionDraft = value) }
    fun saveQuestion() {
        val d = state.questionDraft
        InputRules.question(d)?.let { message(it); return }
        run {
            val r = state.reading ?: return@run
            api.saveQuestion(r.id, d)
            state = state.copy(reading = api.reading(r.id), questions = api.questions(r.id, true),
                screen = Screen.QUESTIONS, notice = "Pregunta guardada.")
        }
    }
    fun deactivateQuestion(id: Int) = run {
        api.deactivateQuestion(id)
        val r = state.reading ?: return@run
        state = state.copy(reading = api.reading(r.id), questions = api.questions(r.id, true), notice = "Pregunta desactivada.")
    }
    fun setReadingState(active: Boolean) = run {
        val r = state.reading ?: return@run
        if (active) state = state.copy(reading = api.setReadingState(r, true), notice = "Lectura activada.")
        else { api.deactivateReading(r.id); state = state.copy(reading = api.reading(r.id), notice = "Lectura desactivada. El historial se conserva.") }
    }
    fun back() {
        if (state.busy) return
        when (state.screen) {
            Screen.LOGIN, Screen.CONNECTION -> show(Screen.WELCOME)
            Screen.REGISTER -> loginScreen(false)
            Screen.DETAIL -> home()
            Screen.READ -> show(if (state.returnToQuiz) Screen.QUIZ else Screen.DETAIL)
            Screen.QUIZ -> home()
            Screen.RESULT -> if (teacher) progress(state.selectedStudent) else home()
            Screen.PROGRESS -> home()
            Screen.READING_EDITOR -> if (state.readingDraft.id == null) home() else show(Screen.DETAIL)
            Screen.QUESTIONS -> show(Screen.DETAIL)
            Screen.QUESTION_EDITOR -> show(Screen.QUESTIONS)
            Screen.STUDENTS -> home()
            Screen.STUDENT_PROGRESS -> students()
            else -> Unit
        }
    }
}
