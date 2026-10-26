package com.example.mmra_medicationmanager.model

enum class TargetRole {
    PATIENT,
    CAREGIVER,
}

enum class DisplayPreference {
    LARGE_TEXT,
    VOICE_GUIDANCE,
    BOTH,
}

data class InitialSetupPreferences(
    val selectedRoles: Set<TargetRole> = setOf(TargetRole.PATIENT),
    val isLargeTextEnabled: Boolean = true,
    val isVoiceGuidanceEnabled: Boolean = true,
) {
    val isPatientMode: Boolean get() = selectedRoles.contains(TargetRole.PATIENT)
    val isCaregiverMode: Boolean get() = selectedRoles.contains(TargetRole.CAREGIVER)
    val isCombinedMode: Boolean get() = isPatientMode && isCaregiverMode
    val hasAnyRoleSelected: Boolean get() = selectedRoles.isNotEmpty()

    val displayPreference: DisplayPreference
        get() = when {
            isLargeTextEnabled && isVoiceGuidanceEnabled -> DisplayPreference.BOTH
            isLargeTextEnabled -> DisplayPreference.LARGE_TEXT
            isVoiceGuidanceEnabled -> DisplayPreference.VOICE_GUIDANCE
            else -> DisplayPreference.LARGE_TEXT
        }
}