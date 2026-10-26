package com.example.project_lttbd.ui.navigation

import androidx.annotation.DrawableRes
import com.example.mmra_medicationmanager.R

data class MMRABottomNavItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int
)

val mmraBottomNavItems = listOf(
    MMRABottomNavItem(
        route = MMRADestinations.HOME,
        label = "Trang chủ",
        iconRes = R.drawable.home
    ),
    MMRABottomNavItem(
        route = MMRADestinations.MEDICINE,
        label = "Thuốc",
        iconRes = R.drawable.thuoc
    ),
    MMRABottomNavItem(
        route = MMRADestinations.SCHEDULE,
        label = "Lịch uống",
        iconRes = R.drawable.lich
    ),
    MMRABottomNavItem(
        route = MMRADestinations.SCAN,
        label = "Quét thuốc",
        iconRes = R.drawable.scan
    ),
    MMRABottomNavItem(
        route = MMRADestinations.HISTORY,
        label = "Lịch sử",
        iconRes = R.drawable.history
    ),
    MMRABottomNavItem(
        route = MMRADestinations.FAMILY,
        label = "Gia đình",
        iconRes = R.drawable.family
    )
)