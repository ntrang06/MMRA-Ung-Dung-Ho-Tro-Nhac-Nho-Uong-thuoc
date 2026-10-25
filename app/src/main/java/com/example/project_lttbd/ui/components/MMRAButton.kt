package com.example.project_lttbd.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MMRAButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = if (enabled) {
            BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            null
        },
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 13.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )

            trailingIcon?.invoke()
        }
    }
}