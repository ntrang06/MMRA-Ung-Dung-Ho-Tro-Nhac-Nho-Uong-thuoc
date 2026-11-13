package com.example.project_lttbd.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun loadingBrush(): Brush {
    val transition = rememberInfiniteTransition(
        label = "medicineLoading"
    )

    val offset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingHighlight"
    )

    return Brush.linearGradient(
        colors = listOf(
            Color(0xFFE1E7E5),
            Color(0xFFF2F5F4),
            Color(0xFFE1E7E5)
        ),
        start = Offset(offset, 0f),
        end = Offset(offset + 480f, 240f)
    )
}

@Composable
private fun SkeletonBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .background(
                brush = brush,
                shape = RoundedCornerShape(10.dp)
            )
    )
}

@Composable
private fun SkeletonCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            Color(0xFFD7E2DF)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun MedicineCardPlaceholder(
    brush: Brush
) {
    SkeletonCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBlock(
                brush = brush,
                modifier = Modifier.width(54.dp),
                height = 62.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonBlock(
                    brush,
                    Modifier.fillMaxWidth(0.8f),
                    height = 20.dp
                )

                SkeletonBlock(
                    brush,
                    Modifier.fillMaxWidth(0.6f),
                    height = 14.dp
                )

                SkeletonBlock(
                    brush,
                    Modifier.fillMaxWidth(0.9f),
                    height = 14.dp
                )
            }
        }
    }
}

@Composable
fun MedicineListSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = loadingBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription =
                    "Đang tải danh sách thuốc và lịch uống"

                progressBarRangeInfo =
                    ProgressBarRangeInfo.Indeterminate
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBlock(
            brush,
            Modifier.fillMaxWidth(),
            height = 52.dp
        )

        SkeletonBlock(
            brush,
            Modifier.width(90.dp),
            height = 22.dp
        )

        repeat(3) {
            MedicineCardPlaceholder(brush)
        }

        SkeletonBlock(
            brush,
            Modifier.fillMaxWidth(),
            height = 54.dp
        )
    }
}

@Composable
fun MedicineFormSkeleton(
    modifier: Modifier = Modifier
) {
    FormSkeleton(
        isSchedule = false,
        modifier = modifier
    )
}

@Composable
fun ScheduleFormSkeleton(
    modifier: Modifier = Modifier
) {
    FormSkeleton(
        isSchedule = true,
        modifier = modifier
    )
}

@Composable
private fun FormSkeleton(
    isSchedule: Boolean,
    modifier: Modifier
) {
    val brush = loadingBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .clearAndSetSemantics {
                contentDescription = if (isSchedule) {
                    "Đang tải thông tin lịch uống"
                } else {
                    "Đang tải thông tin thuốc"
                }

                progressBarRangeInfo =
                    ProgressBarRangeInfo.Indeterminate
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBlock(
            brush,
            Modifier.fillMaxWidth(0.7f),
            height = 26.dp
        )

        SkeletonBlock(
            brush,
            Modifier.fillMaxWidth(0.45f),
            height = 14.dp
        )

        MedicineCardPlaceholder(brush)

        if (isSchedule) {
            repeat(2) { group ->
                SkeletonCard {
                    SkeletonBlock(
                        brush,
                        Modifier.fillMaxWidth(0.65f),
                        height = 20.dp
                    )

                    repeat(if (group == 0) 4 else 3) {
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            SkeletonBlock(
                                brush,
                                Modifier.width(32.dp),
                                height = 36.dp
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                SkeletonBlock(
                                    brush,
                                    Modifier.fillMaxWidth(0.45f),
                                    height = 12.dp
                                )

                                SkeletonBlock(
                                    brush,
                                    Modifier.fillMaxWidth(0.8f),
                                    height = 16.dp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            SkeletonCard {
                repeat(4) {
                    SkeletonBlock(
                        brush,
                        Modifier.fillMaxWidth(0.35f),
                        height = 14.dp
                    )

                    SkeletonBlock(
                        brush,
                        Modifier.fillMaxWidth(),
                        height = 48.dp
                    )
                }
            }
        }

        SkeletonBlock(
            brush,
            Modifier.fillMaxWidth(),
            height = 54.dp
        )
    }
}