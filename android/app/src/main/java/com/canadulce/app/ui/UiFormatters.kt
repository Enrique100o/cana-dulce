package com.canadulce.app.ui

import java.text.NumberFormat
import java.util.Locale

internal fun formatNumber(value: Double?): String =
    value?.let {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"))
            .apply { maximumFractionDigits = 2 }
            .format(it)
    } ?: "—"

internal fun displayLabel(value: String) =
    when (value) {
        "BAJA" -> "Baja"
        "MEDIA" -> "Media"
        "ALTA" -> "Alta"
        "ACTIVA" -> "Activa"
        "INACTIVA" -> "Inactiva"
        "LITERAL" -> "Literal"
        "INFERENCIAL" -> "Inferencial"
        "IDEA_PRINCIPAL" -> "Idea principal"
        "VOCABULARIO" -> "Vocabulario"
        "SECUENCIA" -> "Secuencia"
        else -> value
    }
