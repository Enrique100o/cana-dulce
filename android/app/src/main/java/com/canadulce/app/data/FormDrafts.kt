package com.canadulce.app.data

data class ReadingDraft(
    val id: Int? = null,
    val title: String = "",
    val content: String = "",
    val grade: Int = 3,
    val difficulty: String = "BAJA",
    val category: String = "",
    val status: String = "INACTIVA",
)

data class QuestionDraft(
    val id: Int? = null,
    val text: String = "",
    val type: String = "LITERAL",
    val order: String = "1",
    val status: String = "ACTIVA",
    val options: List<String> = listOf("", "", ""),
    val correctIndex: Int = 0,
)
