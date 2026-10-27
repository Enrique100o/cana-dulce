package com.canadulce.app.data

data class Student(val id: Int, val alias: String, val grade: Int, val status: String)

data class Session(val token: String, val role: String, val student: Student? = null)

data class Reading(
    val id: Int,
    val title: String,
    val content: String,
    val grade: Int,
    val difficulty: String,
    val category: String,
    val status: String,
    val questionCount: Int,
    val locked: Boolean,
)

data class Option(val id: Int, val text: String, val order: Int, val correct: Boolean = false)

data class Question(
    val id: Int,
    val readingId: Int,
    val text: String,
    val type: String,
    val order: Int,
    val status: String,
    val options: List<Option>,
)

data class Answer(val questionId: Int, val text: String, val chosen: String, val correct: Boolean)

data class Attempt(
    val id: Int,
    val studentId: Int,
    val readingId: Int,
    val title: String,
    val started: String,
    val finished: String?,
    val correct: Int?,
    val total: Int,
    val score: Double?,
    val stars: Int?,
    val status: String,
    val answers: List<Answer> = emptyList(),
)

data class Badge(val code: String, val name: String, val obtained: Boolean)

data class Progress(
    val alias: String,
    val grade: Int,
    val completed: Int,
    val missions: Int,
    val average: Double?,
    val best: Double?,
    val points: Double,
    val stars: Int,
    val badges: List<Badge>,
)
