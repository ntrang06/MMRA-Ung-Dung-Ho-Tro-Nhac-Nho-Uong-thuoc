package com.example.mmra_medicationmanager.model

enum class TargetUser {
    MEDICATION_USER,
    CAREGIVER,
}

enum class DisplayPreference {
    LARGE_TEXT,
    VOICE_GUIDANCE,
    BOTH,
}

data class InitialSetupPreferences(
    val targetUser: TargetUser? = null,
    val isLargeTextEnabled: Boolean = true,
    val isVoiceGuidanceEnabled: Boolean = true,
) {
    val displayPreference: DisplayPreference
        get() = when {
            isLargeTextEnabled && isVoiceGuidanceEnabled -> DisplayPreference.BOTH
            isLargeTextEnabled -> DisplayPreference.LARGE_TEXT
            isVoiceGuidanceEnabled -> DisplayPreference.VOICE_GUIDANCE
            else -> DisplayPreference.LARGE_TEXT
        }
}