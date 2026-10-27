package com.canadulce.app

import com.canadulce.app.data.Attempt
import com.canadulce.app.data.Progress
import com.canadulce.app.data.Question
import com.canadulce.app.data.QuestionDraft
import com.canadulce.app.data.Reading
import com.canadulce.app.data.ReadingDraft
import com.canadulce.app.data.Session
import com.canadulce.app.data.Student

enum class Screen {
    WELCOME,
    LOGIN,
    REGISTER,
    CONNECTION,
    MISSIONS,
    DETAIL,
    READ,
    QUIZ,
    RESULT,
    PROGRESS,
    TEACHER_READINGS,
    READING_EDITOR,
    QUESTIONS,
    QUESTION_EDITOR,
    STUDENTS,
    STUDENT_PROGRESS,
}

data class AppState(
    val screen: Screen = Screen.WELCOME,
    val session: Session? = null,
    val teacherLogin: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val readings: List<Reading> = emptyList(),
    val reading: Reading? = null,
    val questions: List<Question> = emptyList(),
    val attempt: Attempt? = null,
    val selections: Map<Int, Int> = emptyMap(),
    val questionIndex: Int = 0,
    val progress: Progress? = null,
    val attempts: List<Attempt> = emptyList(),
    val students: List<Student> = emptyList(),
    val selectedStudent: Student? = null,
    val readingDraft: ReadingDraft = ReadingDraft(),
    val questionDraft: QuestionDraft = QuestionDraft(),
    val baseUrl: String = "",
    val readerSize: Int = 20,
    val returnToQuiz: Boolean = false,
)
