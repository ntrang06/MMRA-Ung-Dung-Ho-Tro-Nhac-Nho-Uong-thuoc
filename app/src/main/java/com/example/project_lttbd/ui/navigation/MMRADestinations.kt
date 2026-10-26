package com.example.project_lttbd.ui.navigation

object MMRADestinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MEDICINE = "medicine"
    const val MANUAL_MEDICINE = "medicine/manual"
    const val EDIT_MEDICINE = "medicine/edit/{medicineId}"
    const val SCHEDULE = "schedule"
    const val SCHEDULE_SETUP = "schedule/setup"
    const val SCAN = "scan"
    const val HISTORY = "history"
    const val FAMILY = "family"
    const val ACCOUNT = "account"
    const val REMINDER = "reminder/{doseId}?readOnly={readOnly}"

    // Màn hình phụ thuộc luồng Quét thuốc & Gia đình (Huy)
    const val FAMILY_INVITE = "family_invite"
    const val FAMILY_INVITE_DETAIL = "family_invite_detail"
    const val FAMILY_PERMISSIONS = "family_permissions"
    const val FAMILY_PERMISSIONS_MEMBER = "family_permissions/{memberId}"
    const val FAMILY_MEMBER_DETAIL = "family_member_detail/{memberId}"
    const val FAMILY_ALERTS = "family_alerts"

    fun familyMemberDetail(memberId: String) = "family_member_detail/$memberId"
    fun familyPermissions(memberId: String) = "family_permissions/$memberId"
}
