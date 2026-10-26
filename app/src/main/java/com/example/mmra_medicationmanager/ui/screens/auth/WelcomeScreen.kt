package com.example.mmra_medicationmanager.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
) {
    val primaryColor = Color(0xFF0F5B51)
    val infoCardBgColor = Color(0xFFEFF6F4)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo_white),
                            contentDescription = stringResource(id = R.string.cd_app_logo),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.welcome_app_name),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = stringResource(id = R.string.welcome_app_subtitle),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = subTextColor,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.welcome_headline),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = darkTextColor,
                lineHeight = 34.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.welcome_description),
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(342f / 150f),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.care_illustration),
                    contentDescription = stringResource(id = R.string.cd_welcome_illustration),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = infoCardBgColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_heart_pulse),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.welcome_info_card),
                        fontSize = 13.sp,
                        color = subTextColor,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLoginClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.welcome_btn_login),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRegisterClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.welcome_btn_register),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.welcome_footer_privacy),
                fontSize = 12.sp,
                color = subTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen()
}