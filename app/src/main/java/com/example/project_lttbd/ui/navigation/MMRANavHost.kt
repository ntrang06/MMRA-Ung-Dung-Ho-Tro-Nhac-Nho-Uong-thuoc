package com.example.project_lttbd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.project_lttbd.ui.screens.account.AccountScreen
import com.example.project_lttbd.ui.screens.auth.LoginScreen
import com.example.project_lttbd.ui.screens.auth.RegisterScreen
import com.example.project_lttbd.ui.screens.family.FamilyScreen
import com.example.project_lttbd.ui.screens.history.HistoryScreen
import com.example.project_lttbd.ui.screens.home.HomeScreen
import com.example.project_lttbd.ui.screens.medicine.MedicineScreen
import com.example.project_lttbd.ui.screens.scan.ScanScreen
import com.example.project_lttbd.ui.screens.schedule.ScheduleScreen

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
            LoginScreen()
        }

        composable(MMRADestinations.REGISTER) {
            RegisterScreen()
        }

        composable(MMRADestinations.ACCOUNT) {
            AccountScreen()
        }

        composable(MMRADestinations.HOME) {
            HomeScreen(
                onViewAllClick = {
                    navController.navigate(MMRADestinations.SCHEDULE) {
                        popUpTo(MMRADestinations.HOME) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(MMRADestinations.MEDICINE) {
            MedicineScreen()
        }

        composable(MMRADestinations.SCHEDULE) {
            ScheduleScreen()
        }

        composable(MMRADestinations.SCAN) {
            ScanScreen()
        }

        composable(MMRADestinations.HISTORY) {
            HistoryScreen()
        }

        composable(MMRADestinations.FAMILY) {
            FamilyScreen()
        }

    }
}
