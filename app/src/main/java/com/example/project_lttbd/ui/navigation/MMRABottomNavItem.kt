package com.example.project_lttbd.ui.navigation

data class MMRABottomNavItem(
    val route: String,
    val label: String
)

val mmraBottomNavItems = listOf(
    MMRABottomNavItem(
        route = MMRADestinations.HOME,
        label = "Trang chủ"
    ),
    MMRABottomNavItem(
        route = MMRADestinations.MEDICINE,
        label = "Thuốc"
    ),
    MMRABottomNavItem(
        route = MMRADestinations.SCHEDULE,
        label = "Lịch uống"
    ),
    MMRABottomNavItem(
        route = MMRADestinations.SCAN,
        label = "Quét thuốc"
    ),
    MMRABottomNavItem(
        route = MMRADestinations.HISTORY,
        label = "Lịch sử"
    ),
    MMRABottomNavItem(
        route = MMRADestinations.FAMILY,
        label = "Gia đình"
    )
)