package com.canadulce.app

import com.canadulce.app.data.*
import org.junit.Assert.*
import org.junit.Test

class InputRulesTest {
    @Test fun aliasAndCodeAreRequired() {
        assertNotNull(InputRules.registration("ab", "123456", "123456"))
        assertNotNull(InputRules.registration("Lector", "12345", "12345"))
        assertNotNull(InputRules.registration("Lector", "123456", "654321"))
        assertNull(InputRules.registration("  Lector  ", "123456", "123456"))
    }
    @Test fun unicodeAliasUsesCharactersRatherThanUtf16Units() {
        assertNotNull(InputRules.registration("😀😀", "123456", "123456"))
        assertNull(InputRules.registration("😀😀😀", "123456", "123456"))
    }
    @Test fun invalidQuestionCannotBeSubmitted() {
        assertNotNull(InputRules.question(QuestionDraft(text = "¿Qué pasó?", order = "0", options = listOf("A", "B"))))
        assertNotNull(InputRules.question(QuestionDraft(text = "¿Qué pasó?", options = listOf(" A ", "a"))))
        assertNotNull(InputRules.question(QuestionDraft(text = "¿Qué pasó?", options = listOf("A", ""))))
        assertNotNull(InputRules.question(QuestionDraft(text = "¿Qué pasó?", options = listOf("A", "B"), correctIndex = 2)))
        assertNull(InputRules.question(QuestionDraft(text = "¿Qué pasó?", options = listOf("Una semilla", "Un libro"))))
    }
    @Test fun quizRequiresAnExistingOptionForEveryQuestion() {
        val q = Question(1, 1, "¿Qué pasó?", "LITERAL", 1, "ACTIVA", listOf(Option(5, "A", 1), Option(6, "B", 2)))
        assertFalse(InputRules.complete(emptyList(), emptyMap()))
        assertFalse(InputRules.complete(listOf(q), emptyMap()))
        assertFalse(InputRules.complete(listOf(q), mapOf(1 to 999)))
        assertTrue(InputRules.complete(listOf(q), mapOf(1 to 5)))
    }
    @Test fun apiUrlRejectsCredentialsQueriesAndUntrustedCleartext() {
        assertTrue(ApiClient.validBaseUrl("http://10.0.2.2:8080/api", true))
        assertTrue(ApiClient.validBaseUrl("https://lectura.example/api", false))
        assertFalse(ApiClient.validBaseUrl("http://10.0.2.2:8080/api", false))
        assertFalse(ApiClient.validBaseUrl("http://example.org/api", true))
        assertFalse(ApiClient.validBaseUrl("https://user:pass@example.org/api", true))
        assertFalse(ApiClient.validBaseUrl("https://example.org/api?token=abc", true))
        assertFalse(ApiClient.validBaseUrl("https://example.org/api#test", true))
        assertFalse(ApiClient.validBaseUrl("file:///api", true))
        assertFalse(ApiClient.validBaseUrl("https://example.org", true))
    }
}
