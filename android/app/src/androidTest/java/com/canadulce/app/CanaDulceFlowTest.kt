package com.canadulce.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.runners.MethodSorters
import java.io.File

/** Requiere scripts/servidor-android-pruebas.mjs en el puerto 8081 del computador. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CanaDulceFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    companion object {
        @BeforeClass @JvmStatic fun configure() {
            InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences("cana_connection", 0).edit().putString("baseUrl", "http://10.0.2.2:8081/api").commit()
        }
        @AfterClass @JvmStatic fun restore() {
            InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences("cana_connection", 0).edit().remove("baseUrl").commit()
        }
    }
    private fun ready(tag: String) {
        compose.waitUntil(25000) {
            compose.onAllNodesWithTag("loading").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun click(tag: String) {
        ready(tag)
        val node = compose.onNodeWithTag(tag)
        if (tag !in setOf("back", "confirm", "logout", "nav_progress", "nav_missions")) node.performScrollTo()
        node.performClick()
    }
    private fun enter(tag: String, value: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
    }
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")!!).apply { mkdirs() }
        File(directory, name + ".png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun login(teacher: Boolean = false) {
        click(if (teacher) "teacher_role" else "student_role")
        enter("login_alias", if (teacher) "docente" else "Lector03")
        enter("login_code", if (teacher) "Lectura2026!" else "728461")
        click("login_submit")
        ready(if (teacher) "new_reading" else "mission_1")
    }
    @Test fun a_studentReadsResumesAndEarnsRewards() {
        capture("01-bienvenida")
        login()
        enter("search", "árbol")
        click("search_submit")
        ready("mission_1")
        compose.onNodeWithTag("mission_2").assertDoesNotExist()
        capture("02-misiones")
        click("mission_1")
        click("read_mission")
        capture("03-lectura")
        click("start_quiz")
        compose.onNodeWithTag("quiz_next").assertIsNotEnabled()
        click("option_0")
        click("consult_text")
        click("start_quiz")
        compose.onNodeWithTag("option_0").assertIsSelected()
        click("back")
        click("confirm")
        ready("resume_1")
        click("resume_1")
        compose.onNodeWithTag("quiz_next").assertIsNotEnabled()
        repeat(4) { index ->
            click(if (index == 3) "option_1" else "option_0")
            if (index == 0) capture("04-pregunta")
            click("quiz_next")
        }
        ready("result_score")
        compose.onNodeWithTag("result_score").assertTextEquals("75 / 100")
        capture("05-resultado")
        click("result_progress")
        ready("nav_progress")
        compose.onNodeWithText("1 intento completado · 1 lectura diferente").assertExists()
        compose.onNodeWithText("Primera aventura").assertExists()
        capture("06-progreso")
    }
    @Test fun b_teacherCreatesEditsPublishesAndPreservesHistory() {
        login(true)
        capture("07-docente")
        click("admin_1")
        compose.onNodeWithTag("edit_reading").assertIsNotEnabled()
        click("back")
        click("new_reading")
        enter("reading_title", "La huerta del aula")
        enter("reading_content", "Ana sembró una semilla en la huerta de la escuela. La regó cada mañana y observó cómo crecía.")
        enter("reading_category", "Naturaleza")
        click("save_reading")
        ready("new_question")
        click("new_question")
        enter("question_prompt", "¿Qué sembró Ana en la huerta?")
        enter("question_option_0", "Una semilla")
        enter("question_option_1", "Un libro")
        enter("question_option_2", "Una pelota")
        click("save_question")
        ready("edit_question_17")
        click("edit_question_17")
        enter("question_prompt", "¿Qué sembró Ana en la huerta de su escuela?")
        click("save_question")
        ready("activate_reading")
        capture("08-preguntas-docente")
        click("activate_reading")
        ready("question_back")
        compose.onNodeWithTag("activate_reading").assertDoesNotExist()
        click("question_back")
        click("edit_reading")
        enter("reading_title", "La huerta de nuestra aula")
        click("save_reading")
        ready("question_back")
        click("question_back")
        click("deactivate_reading")
        click("confirm")
        ready("activate_reading")
        click("activate_reading")
        ready("deactivate_reading")
        click("back")
        click("nav_progress")
        ready("student_1")
        click("student_1")
        ready("attempt_1")
        click("attempt_1")
        ready("result_score")
        compose.onNodeWithTag("result_score").assertTextEquals("75 / 100")
        capture("09-resultado-docente")
    }
    @Test fun c_registrationValidationAndLoginErrors() {
        click("student_role")
        enter("login_alias", "Lector03")
        enter("login_code", "000000")
        click("login_submit")
        ready("error")
        compose.onNodeWithText("El alias o el código de acceso no son válidos.").assertExists()
        click("create_alias")
        enter("register_alias", "NuevaAventura")
        enter("register_code", "123456")
        enter("register_confirmation", "654321")
        click("register_submit")
        ready("error")
        compose.onNodeWithText("Los códigos no coinciden.").assertExists()
        enter("register_confirmation", "123456")
        click("register_submit")
        ready("login_submit")
        enter("login_alias", "NuevaAventura")
        enter("login_code", "123456")
        click("login_submit")
        ready("mission_1")
        compose.onNodeWithText("¡Hola, NuevaAventura!").assertExists()
    }
}
