package com.canadulce.app.data

object InputRules {
    fun registration(alias: String, code: String, confirmation: String): String? =
        when {
            alias.trim().codePointCount(0, alias.trim().length) !in 3..30 ->
                "El alias debe tener entre 3 y 30 caracteres."
            code.codePointCount(0, code.length) !in 6..32 ->
                "El código debe tener entre 6 y 32 caracteres."
            code != confirmation -> "Los códigos no coinciden."
            else -> null
        }

    fun question(draft: QuestionDraft): String? =
        when {
            draft.text.trim().length !in 5..400 ->
                "El enunciado debe tener entre 5 y 400 caracteres."
            draft.order.toIntOrNull() !in 1..999 -> "El orden debe ser un número entre 1 y 999."
            draft.options.size !in 2..4 -> "Agrega entre 2 y 4 opciones."
            draft.options.any { it.trim().isEmpty() || it.trim().length > 250 } ->
                "Cada opción debe tener entre 1 y 250 caracteres."
            draft.options.map { it.trim().lowercase() }.distinct().size != draft.options.size ->
                "Las opciones deben ser diferentes."
            draft.correctIndex !in draft.options.indices -> "Selecciona una opción correcta."
            else -> null
        }

    fun complete(questions: List<Question>, selections: Map<Int, Int>): Boolean =
        questions.isNotEmpty() && questions.all { q -> q.options.any { it.id == selections[q.id] } }
}
