package com.example.project_lttbd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MMRATextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    labelOutside: Boolean = false,
    minLines: Int = 1
) {
    val hasError = !errorMessage.isNullOrBlank()

    val field: @Composable () -> Unit = {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = if (labelOutside) {
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
            } else {
                modifier
            },
            label = if (labelOutside) {
                null
            } else {
                { Text(label) }
            },
            placeholder = { Text(placeholder) },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            isError = hasError,
            supportingText = if (hasError) {
                { Text(errorMessage.orEmpty()) }
            } else {
                null
            },
            textStyle = if (labelOutside) {
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp
                )
            } else {
                MaterialTheme.typography.bodyLarge
            },
            colors = if (labelOutside) {
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    errorContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFD7E2DF)
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            },
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (labelOutside) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF102A2A)
            )

            field()
        }
    } else {
        field()
    }
}