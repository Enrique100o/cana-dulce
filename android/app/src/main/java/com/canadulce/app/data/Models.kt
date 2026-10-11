package com.canadulce.app.data

data class Student(val id: Int, val alias: String, val grade: Int, val status: String)
data class Session(val token: String, val role: String, val student: Student? = null)
data class Reading(
    val id: Int, val title: String, val content: String, val grade: Int,
    val difficulty: String, val category: String, val status: String,
    val questionCount: Int, val locked: Boolean
)
data class Option(val id: Int, val text: String, val order: Int, val correct: Boolean = false)
data class Question(
    val id: Int, val readingId: Int, val text: String, val type: String, val order: Int,
    val status: String, val options: List<Option>
)
data class Answer(val questionId: Int, val text: String, val chosen: String, val correct: Boolean)
data class Attempt(
    val id: Int, val studentId: Int, val readingId: Int, val title: String,
    val started: String, val finished: String?, val correct: Int?, val total: Int,
    val score: Double?, val stars: Int?, val status: String, val answers: List<Answer> = emptyList()
)
data class Badge(val code: String, val name: String, val obtained: Boolean)
data class Progress(
    val alias: String, val grade: Int, val completed: Int, val missions: Int,
    val average: Double?, val best: Double?, val points: Double, val stars: Int,
    val badges: List<Badge>
)
data class ReadingDraft(
    val id: Int? = null, val title: String = "", val content: String = "",
    val grade: Int = 3, val difficulty: String = "BAJA", val category: String = "",
    val status: String = "INACTIVA"
)
data class QuestionDraft(
    val id: Int? = null, val text: String = "", val type: String = "LITERAL",
    val order: String = "1", val status: String = "ACTIVA",
    val options: List<String> = listOf("", "", ""), val correctIndex: Int = 0
)

object InputRules {
    fun registration(alias: String, code: String, confirmation: String): String? = when {
        alias.trim().codePointCount(0, alias.trim().length) !in 3..30 -> "El alias debe tener entre 3 y 30 caracteres."
        code.codePointCount(0, code.length) !in 6..32 -> "El código debe tener entre 6 y 32 caracteres."
        code != confirmation -> "Los códigos no coinciden."
        else -> null
    }
    fun question(draft: QuestionDraft): String? = when {
        draft.text.trim().length !in 5..400 -> "El enunciado debe tener entre 5 y 400 caracteres."
        draft.order.toIntOrNull() !in 1..999 -> "El orden debe ser un número entre 1 y 999."
        draft.options.size !in 2..4 -> "Agrega entre 2 y 4 opciones."
        draft.options.any { it.trim().isEmpty() || it.trim().length > 250 } -> "Cada opción debe tener entre 1 y 250 caracteres."
        draft.options.map { it.trim().lowercase() }.distinct().size != draft.options.size -> "Las opciones deben ser diferentes."
        draft.correctIndex !in draft.options.indices -> "Selecciona una opción correcta."
        else -> null
    }
    fun complete(questions: List<Question>, selections: Map<Int, Int>): Boolean =
        questions.isNotEmpty() && questions.all { q -> q.options.any { it.id == selections[q.id] } }
}
