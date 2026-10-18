package com.example.project_lttbd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.project_lttbd.data.mock.MockFamilyData
import com.example.project_lttbd.ui.screens.account.AccountScreen
import com.example.project_lttbd.ui.screens.auth.LoginScreen
import com.example.project_lttbd.ui.screens.auth.RegisterScreen
import com.example.project_lttbd.ui.screens.family.CaregiverMemberDetailScreen
import com.example.project_lttbd.ui.screens.family.FamilyAlertsScreen
import com.example.project_lttbd.ui.screens.family.FamilyInviteDetailScreen
import com.example.project_lttbd.ui.screens.family.FamilyPermissionsScreen
import com.example.project_lttbd.ui.screens.family.FamilyScreen
import com.example.project_lttbd.ui.screens.family.InviteFamilyScreen
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
            HomeScreen()
        }

        composable(MMRADestinations.MEDICINE) {
            MedicineScreen()
        }

        composable(MMRADestinations.SCHEDULE) {
            ScheduleScreen()
        }

        composable(MMRADestinations.SCAN) {
            ScanScreen(
                onNavigateToAddMedicine = { _, _ ->
                    navController.navigate(MMRADestinations.MEDICINE)
                },
                onNavigateToManualInput = {
                    navController.navigate(MMRADestinations.MEDICINE)
                },
                onClose = {
                    if (!navController.popBackStack()) {
                        navController.navigate(MMRADestinations.HOME)
                    }
                }
            )
        }

        composable(MMRADestinations.HISTORY) {
            HistoryScreen()
        }

        composable(MMRADestinations.FAMILY) {
            FamilyScreen(
                onMemberClick = { memberId ->
                    navController.navigate(MMRADestinations.familyMemberDetail(memberId))
                },
                onInviteClick = {
                    navController.navigate(MMRADestinations.FAMILY_INVITE)
                },
                onInviteDetailClick = {
                    navController.navigate(MMRADestinations.FAMILY_INVITE_DETAIL)
                },
                onAlertsClick = {
                    navController.navigate(MMRADestinations.FAMILY_ALERTS)
                }
            )
        }

        composable(MMRADestinations.FAMILY_INVITE) {
            InviteFamilyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.FAMILY_INVITE_DETAIL) {
            FamilyInviteDetailScreen(
                onAcceptClick = {
                    navController.navigate(MMRADestinations.FAMILY_PERMISSIONS)
                },
                onDeclineClick = {
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.FAMILY_PERMISSIONS) {
            FamilyPermissionsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = MMRADestinations.FAMILY_PERMISSIONS_MEMBER,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: ""
            val member = MockFamilyData.mockMembers.firstOrNull { it.id == memberId }
            FamilyPermissionsScreen(
                onBackClick = { navController.popBackStack() },
                memberName = member?.fullName ?: "Nguyễn Minh Anh",
                memberRole = if (member != null) "${member.relationship} • Hồ sơ theo dõi" else "Con gái • Người chăm sóc"
            )
        }

        composable(
            route = MMRADestinations.FAMILY_MEMBER_DETAIL,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: ""
            CaregiverMemberDetailScreen(
                memberId = memberId,
                onBackClick = { navController.popBackStack() },
                onViewHistoryClick = {
                    navController.navigate(MMRADestinations.HISTORY)
                },
                onPermissionsClick = {
                    navController.navigate(MMRADestinations.familyPermissions(memberId))
                }
            )
        }

        composable(MMRADestinations.FAMILY_ALERTS) {
            FamilyAlertsScreen(
                onBackClick = { navController.popBackStack() },
                onAlertClick = { alert ->
                    navController.navigate(MMRADestinations.familyMemberDetail(alert.memberId))
                }
            )
        }
    }
}
