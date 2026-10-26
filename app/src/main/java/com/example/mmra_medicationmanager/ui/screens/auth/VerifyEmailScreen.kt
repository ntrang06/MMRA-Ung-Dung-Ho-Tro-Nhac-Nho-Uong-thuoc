package com.example.mmra_medicationmanager.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R

@Composable
fun VerifyEmailScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onVerifyClick: () -> Unit = {},
    onChangeEmailClick: () -> Unit = {},
    onResendCodeClick: () -> Unit = {},
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val infoBgColor = Color(0xFFEFF6F4)

    val otpDigits = listOf("2", "8", "4", "9", "1", "6")

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
                        text = stringResource(id = R.string.verify_email_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = stringResource(id = R.string.verify_email_subtitle),
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = lightBgColor,
                modifier = Modifier.size(56.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_email),
                        contentDescription = stringResource(id = R.string.cd_email_icon),
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.verify_code_headline),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = darkTextColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.verify_email_desc),
                fontSize = 14.sp,
                color = subTextColor,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.sample_email_masked),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.label_verify_code),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        otpDigits.forEachIndexed { index, digit ->
                            val isActive = index == otpDigits.lastIndex
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(
                                    if (isActive) 2.dp else 1.dp,
                                    if (isActive) primaryColor else borderColor,
                                ),
                                modifier = Modifier.size(46.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Text(
                                        text = digit,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = darkTextColor,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.verify_code_validity),
                        fontSize = 12.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onVerifyClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.btn_verify_continue),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.resend_code_timer),
                fontSize = 14.sp,
                color = subTextColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onResendCodeClick() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onChangeEmailClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.btn_change_email),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = infoBgColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info_circle),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.verify_email_spam_note),
                        fontSize = 13.sp,
                        color = subTextColor,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerifyEmailScreenPreview() {
    VerifyEmailScreen()
}