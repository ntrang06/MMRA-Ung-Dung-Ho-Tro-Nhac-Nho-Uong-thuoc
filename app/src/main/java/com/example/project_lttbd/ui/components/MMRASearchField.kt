package com.example.project_lttbd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.ui.theme.InterFontFamily

private val SearchBackground = Color(0xFFFFFFFF)
private val SearchBorder = Color(0xFFD7E2DF)
private val SearchText = Color(0xFF526565)

@Composable
fun MMRASearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Tìm tên thuốc"
) {
    val shape = RoundedCornerShape(12.dp)
    val searchTextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        color = SearchText
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .background(SearchBackground, shape)
            .border(1.dp, SearchBorder, shape)
            .padding(horizontal = 14.dp),
        singleLine = true,
        textStyle = searchTextStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .heightIn(min = 50.dp)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokeWidth = 1.5.dp.toPx()

                    drawCircle(
                        color = SearchText,
                        radius = size.minDimension * 0.28f,
                        center = Offset(
                            size.width * 0.42f,
                            size.height * 0.42f
                        ),
                        style = Stroke(width = strokeWidth)
                    )

                    drawLine(
                        color = SearchText,
                        start = Offset(
                            size.width * 0.63f,
                            size.height * 0.63f
                        ),
                        end = Offset(
                            size.width * 0.86f,
                            size.height * 0.86f
                        ),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = searchTextStyle
                        )
                    }

                    innerTextField()
                }
            }
        }
    )
}