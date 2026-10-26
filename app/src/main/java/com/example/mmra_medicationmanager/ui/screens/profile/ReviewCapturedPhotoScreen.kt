package com.example.mmra_medicationmanager.ui.screens.profile

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R
import com.example.mmra_medicationmanager.model.UserProfileManager

@Composable
fun ReviewCapturedPhotoScreen(
    modifier: Modifier = Modifier,
    capturedBitmap: Bitmap? = UserProfileManager.tempCapturedBitmap,
    onBackClick: () -> Unit = {},
    onContinueCropClick: () -> Unit = {},
    onRetakeClick: () -> Unit = {},
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = lightBgColor,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onBackClick() },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = stringResource(id = R.string.cd_back_button),
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.avatar_review_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = stringResource(id = R.string.avatar_review_step),
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Image Preview
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFD3E4E0),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFB5D4CC),
                            modifier = Modifier.size(180.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_user),
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(90.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.avatar_illustrative_note),
                fontSize = 12.sp,
                color = subTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.avatar_review_heading),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = darkTextColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.avatar_review_desc),
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Button: Tiếp tục cắt ảnh
            Button(
                onClick = onContinueCropClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_crop),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.avatar_btn_continue_crop),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Outlined Button: Chụp lại
            OutlinedButton(
                onClick = onRetakeClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.avatar_btn_retake),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewCapturedPhotoScreenPreview() {
    ReviewCapturedPhotoScreen()
}