package com.example.project_lttbd.ui.screens.home

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.remote.DoseException
import com.example.project_lttbd.data.remote.DoseRepository
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.example.project_lttbd.notifications.storage.LocalNotificationStore
import com.example.project_lttbd.notifications.sync.MedicationReminderSync
import com.example.project_lttbd.ui.screens.schedule.rememberScheduleOwnerUid
import com.example.project_lttbd.ui.screens.schedule.toScheduleDoseUiModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private data class HomeLoad(
    val loading: Boolean = true,
    val hasLoaded: Boolean = false,
    val doses: List<DoseResponseDto> = emptyList(),
    val error: String? = null
)

private data class HomeSummary(
    val nextDose: NextDoseUiModel?,
    val doses: List<HomeDoseUiModel>,
    val taken: Int,
    val skipped: Int,
    val missed: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onViewAllClick: () -> Unit = {},
    onViewDoseClick: (String) -> Unit = {},
    onAccountClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ownerUid = if (preview) "preview" else rememberScheduleOwnerUid()

    val repository = remember(preview) {
        if (preview) null else DoseRepository()
    }

    var now by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    var revision by remember(ownerUid) {
        mutableStateOf(0)
    }

    val zone = remember {
        TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    }

    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = zone
    }.format(Date(now))

    var loaded by remember(ownerUid, dateKey) {
        mutableStateOf(
            HomeLoad(loading = preview || ownerUid != null)
        )
    }

    var hasUnread by remember(ownerUid) {
        mutableStateOf(false)
    }

    LaunchedEffect(preview) {
        if (preview) return@LaunchedEffect

        while (true) {
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    DisposableEffect(lifecycleOwner, ownerUid, preview) {
        var paused = false

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> paused = true

                Lifecycle.Event.ON_RESUME -> {
                    now = System.currentTimeMillis()

                    if (paused) {
                        paused = false
                        revision++
                    }
                }

                else -> Unit
            }
        }

        if (!preview) {
            lifecycleOwner.lifecycle.addObserver(observer)
        }

        onDispose {
            if (!preview) {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    LaunchedEffect(ownerUid, dateKey, revision) {
        if (preview || ownerUid == null || repository == null) {
            return@LaunchedEffect
        }

        // Giữ dữ liệu đang hiển thị trong lúc tải lại.
        loaded = loaded.copy(
            loading = true,
            error = null
        )

        try {
            ensureHomeOwner(ownerUid)

            val result = repository.listDoses(dateKey)

            currentCoroutineContext().ensureActive()
            ensureHomeOwner(ownerUid)

            val dayDoses = result.filter {
                it.localDate == dateKey
            }

            loaded = HomeLoad(
                loading = false,
                hasLoaded = true,
                doses = dayDoses.filter {
                    it.status != DoseStatusDto.CANCELLED
                }
            )

            runCatching {
                MedicationReminderSync.syncDoses(
                    context = context,
                    uid = ownerUid,
                    doses = dayDoses
                )
            }.onFailure {
                Log.w(
                    "MMRA_API_ERROR",
                    "Đã tải Trang chủ nhưng chưa đồng bộ được báo thức.",
                    it
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            ensureHomeOwner(ownerUid)

            loaded = loaded.copy(
                loading = false,
                error = (error as? DoseException)?.message
                    ?: "Chưa tải được liều thuốc hôm nay. Vui lòng thử lại."
            )
        }
    }

    LaunchedEffect(ownerUid, revision, now) {
        if (preview || ownerUid == null) {
            return@LaunchedEffect
        }

        val unread = withContext(Dispatchers.IO) {
            LocalNotificationStore
                .getNotifications(context, ownerUid)
                .any { !it.isRead }
        }

        currentCoroutineContext().ensureActive()
        ensureHomeOwner(ownerUid)

        hasUnread = unread
    }

    val summary = remember(loaded.doses, now) {
        buildHomeSummary(loaded.doses, now)
    }

    val calendar = Calendar.getInstance(zone).apply {
        timeInMillis = now
    }

    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Chào buổi sáng"
        in 12..17 -> "Chào buổi chiều"
        else -> "Chào buổi tối"
    }

    val showProfile = !preview && ownerUid != null

    val displayName = if (showProfile) {
        UserProfileManager.name.trim().ifBlank { "bạn" }
    } else {
        "bạn"
    }

    val todayText = SimpleDateFormat(
        "EEEE, dd 'tháng' MM",
        Locale.forLanguageTag("vi-VN")
    ).apply {
        timeZone = zone
    }.format(Date(now)).replaceFirstChar {
        it.titlecase()
    }

    val refresh: () -> Unit = {
        if (!preview && ownerUid != null && !loaded.loading) {
            loaded = loaded.copy(
                loading = true,
                error = null
            )
            revision++
        }
    }

    PullToRefreshBox(
        isRefreshing = loaded.loading && loaded.hasLoaded,
        onRefresh = refresh,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader(
                greeting = "$greeting, $displayName",
                dateText = todayText,
                initials = if (showProfile) {
                    UserProfileManager.getInitials()
                } else {
                    "B"
                },
                avatarBitmap = if (showProfile) {
                    UserProfileManager.currentAvatarBitmap
                } else {
                    null
                },
                hasUnreadNotifications = hasUnread,
                onNotificationsClick = onNotificationsClick,
                onAccountClick = onAccountClick
            )

            Column(
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 6.dp,
                    end = 20.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    ownerUid == null -> {
                        Text(
                            text = "Vui lòng đăng nhập để xem lịch uống của bạn.",
                            color = Color(0xFF526565)
                        )

                        TextButton(onClick = onAccountClick) {
                            Text(
                                text = "Mở tài khoản",
                                color = Color(0xFF126B62)
                            )
                        }
                    }

                    loaded.loading && !loaded.hasLoaded -> {
                        HomeLoadingSkeleton()
                    }

                    loaded.error != null && !loaded.hasLoaded -> {
                        Text(
                            text = loaded.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )

                        TextButton(onClick = refresh) {
                            Text(
                                text = "Thử lại",
                                color = Color(0xFF126B62)
                            )
                        }
                    }

                    else -> {
                        if (loaded.error != null) {
                            Text(
                                text = loaded.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        val next = summary.nextDose

                        if (next != null) {
                            NextDoseCard(
                                dose = next,
                                onViewDoseClick = onViewDoseClick
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    Color(0xFFD7E2DF)
                                )
                            ) {
                                Text(
                                    text = if (summary.doses.isEmpty()) {
                                        "Bạn chưa có lịch uống thuốc hôm nay."
                                    } else {
                                        "Không còn liều chờ xử lý hôm nay."
                                    },
                                    modifier = Modifier.padding(18.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF102A2A)
                                )
                            }
                        }

                        TodayProgressCard(
                            takenDoses = summary.taken,
                            totalDoses = summary.doses.size,
                            skippedDoses = summary.skipped,
                            missedDoses = summary.missed
                        )

                        TodayScheduleSection(
                            doses = summary.doses,
                            onViewAllClick = onViewAllClick,
                            onDoseClick = onViewDoseClick
                        )
                    }
                }
            }
        }
    }
}

private fun ensureHomeOwner(ownerUid: String) {
    if (FirebaseProvider.auth.currentUser?.uid != ownerUid) {
        throw CancellationException("Tài khoản đã thay đổi.")
    }
}

private fun buildHomeSummary(
    source: List<DoseResponseDto>,
    now: Long
): HomeSummary {
    val doses = source
        .filter { it.status != DoseStatusDto.CANCELLED }
        .sortedWith(
            compareBy<DoseResponseDto> { it.minuteOfDay }
                .thenBy { it.doseId }
        )

    val next = doses
        .filter { it.status == DoseStatusDto.PENDING }
        .minWithOrNull(
            compareBy<DoseResponseDto> {
                homeReminderMillis(it)
            }.thenBy {
                it.minuteOfDay
            }.thenBy {
                it.doseId
            }
        )

    val rows = doses.mapNotNull { dose ->
        val ui = dose.toScheduleDoseUiModel()

        val status = when (dose.status) {
            DoseStatusDto.TAKEN -> HomeDoseStatus.TAKEN
            DoseStatusDto.SKIPPED -> HomeDoseStatus.SKIPPED
            DoseStatusDto.MISSED -> HomeDoseStatus.MISSED
            DoseStatusDto.CANCELLED -> return@mapNotNull null

            DoseStatusDto.PENDING -> when {
                !dose.nextReminderAt.isNullOrBlank() -> {
                    HomeDoseStatus.SNOOZED
                }

                homeReminderMillis(dose) <= now -> {
                    HomeDoseStatus.UNCONFIRMED
                }

                dose.doseId == next?.doseId -> {
                    HomeDoseStatus.NEXT
                }

                else -> HomeDoseStatus.UPCOMING
            }
        }

        HomeDoseUiModel(
            doseId = dose.doseId,
            time = ui.time,
            medicineName = ui.medicineName,
            instruction = listOf(
                ui.quantityText,
                ui.instruction
            ).filter {
                it.isNotBlank()
            }.joinToString(" • "),
            status = status
        )
    }

    val nextCard = next?.let { dose ->
        val ui = dose.toScheduleDoseUiModel()

        NextDoseUiModel(
            doseId = dose.doseId,
            medicineId = dose.medicineId,
            time = ui.time,
            medicineName = ui.medicineName,
            instruction = listOf(
                ui.quantityText,
                ui.instruction
            ).filter {
                it.isNotBlank()
            }.joinToString(" • "),
            medicineCode = "",
            countdownText = homeCountdownText(dose, now)
        )
    }

    return HomeSummary(
        nextDose = nextCard,
        doses = rows,
        taken = doses.count {
            it.status == DoseStatusDto.TAKEN
        },
        skipped = doses.count {
            it.status == DoseStatusDto.SKIPPED
        },
        missed = doses.count {
            it.status == DoseStatusDto.MISSED
        }
    )
}

private fun homeReminderMillis(dose: DoseResponseDto): Long {
    val target = dose.nextReminderAt
        ?.takeIf { it.isNotBlank() }
        ?: dose.scheduledAt

    return MedicationReminderSync.parseIsoToMillis(target)
}

private fun homeCountdownText(
    dose: DoseResponseDto,
    now: Long
): String {
    val remaining = homeReminderMillis(dose) - now

    if (remaining <= 0L) {
        return "Chờ xác nhận"
    }

    val prefix = if (dose.nextReminderAt.isNullOrBlank()) {
        "Còn"
    } else {
        "Nhắc lại sau"
    }

    if (remaining < 60_000L) {
        return "$prefix dưới 1 phút"
    }

    val minutes = remaining / 60_000L
    val hours = minutes / 60L
    val rest = minutes % 60L

    val duration = when {
        hours == 0L -> "$minutes phút"
        rest == 0L -> "$hours giờ"
        else -> "$hours giờ $rest phút"
    }

    return "$prefix $duration"
}

@Composable
private fun HomeLoadingSkeleton() {
    val transition = rememberInfiniteTransition(
        label = "homeLoading"
    )

    val offset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "homeHighlight"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE1E7E5),
            Color(0xFFF2F5F4),
            Color(0xFFE1E7E5)
        ),
        start = Offset(offset, 0f),
        end = Offset(offset + 480f, 240f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Đang tải liều thuốc hôm nay"
                progressBarRangeInfo =
                    ProgressBarRangeInfo.Indeterminate
            },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(3) { index ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    Color(0xFFD7E2DF)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(20.dp)
                            .background(
                                brush,
                                RoundedCornerShape(8.dp)
                            )
                    )

                    repeat(if (index == 1) 2 else 3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    if (index == 2) 38.dp else 24.dp
                                )
                                .background(
                                    brush,
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}