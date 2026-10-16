package com.example.project_lttbd.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun MMRANavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MMRADestinations.HOME,
        modifier = modifier
    ) {
        composable(MMRADestinations.LOGIN) {
            PlaceholderScreen("Đăng nhập")
        }

        composable(MMRADestinations.REGISTER) {
            PlaceholderScreen("Đăng ký")
        }

        composable(MMRADestinations.HOME) {
            PlaceholderScreen("Trang chủ")
        }

        composable(MMRADestinations.MEDICINE) {
            PlaceholderScreen("Thuốc của tôi")
        }

        composable(MMRADestinations.SCHEDULE) {
            PlaceholderScreen("Lịch uống thuốc")
        }

        composable(MMRADestinations.SCAN) {
            PlaceholderScreen("Quét thuốc")
        }

        composable(MMRADestinations.HISTORY) {
            PlaceholderScreen("Lịch sử")
        }

        composable(MMRADestinations.FAMILY) {
            PlaceholderScreen("Gia đình")
        }

        composable(MMRADestinations.ACCOUNT) {
            PlaceholderScreen("Tài khoản")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}