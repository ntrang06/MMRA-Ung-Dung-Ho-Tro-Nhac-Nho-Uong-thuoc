package com.example.project_lttbd.data.mock

import androidx.compose.runtime.mutableStateMapOf

enum class MockDoseAction {
    TAKEN,
    SNOOZED,
    SKIPPED
}

data class MockDoseActionRecord(
    val action: MockDoseAction,
    val recordedAtMillis: Long
)

object MockDoseActions {
    private val records = mutableStateMapOf<String, MockDoseActionRecord>()

    fun get(doseId: String): MockDoseActionRecord? {
        return records[doseId]
    }

    fun isFinished(doseId: String): Boolean {
        val action = records[doseId]?.action

        return action == MockDoseAction.TAKEN ||
                action == MockDoseAction.SKIPPED
    }

    fun record(
        doseId: String,
        action: MockDoseAction
    ) {
        if (isFinished(doseId)) return

        records[doseId] = MockDoseActionRecord(
            action = action,
            recordedAtMillis = System.currentTimeMillis()
        )
    }
}