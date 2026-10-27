package com.canadulce.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AppTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    multiline: Boolean = false,
    numeric: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value,
        onChange,
        label = { Text(label) },
        singleLine = !multiline,
        minLines = if (multiline) 4 else 1,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        visualTransformation =
            if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    if (password) KeyboardType.Password
                    else if (numeric) KeyboardType.Number else KeyboardType.Text,
            ),
        trailingIcon =
            if (password) {
                {
                    TextButton(onClick = { visible = !visible }) {
                        Text(if (visible) "Ocultar" else "Ver", fontSize = 12.sp)
                    }
                }
            } else null,
    )
}
