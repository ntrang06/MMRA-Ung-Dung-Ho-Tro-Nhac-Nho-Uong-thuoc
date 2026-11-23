package com.example.project_lttbd.ui.screens.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.remote.LookupErrorKind
import com.example.project_lttbd.data.remote.MedicineLookupCandidate
import com.example.project_lttbd.data.remote.MedicineLookupException
import com.example.project_lttbd.data.remote.MedicineLookupRepository
import com.example.project_lttbd.data.remote.hasMultipleActiveIngredients
import com.example.project_lttbd.data.remote.isSafeSingleStrengthForAutoFill
import com.example.project_lttbd.data.remote.isSpecialReleaseMedicine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

private fun normalizedMedicineName(
    value: String
): String {
    return Normalizer.normalize(
        value.lowercase(Locale.ROOT),
        Normalizer.Form.NFD
    )
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd')
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

private fun findUniqueMedicine(
    products: List<MedicineLookupCandidate>,
    ocrName: String,
    ocrStrength: String,
    ocrUnit: String
): MedicineLookupCandidate? {
    if (ocrName.isBlank() || ocrStrength.isBlank()) {
        return null
    }

    if (products.size >= 55) {
        return null
    }

    val expectedName = normalizedMedicineName(ocrName)
    val expectedAmount = ocrStrength
        .replace(',', '.')
        .toBigDecimalOrNull() ?: return null

    val matching = products.filter { product ->
        val safeCatalog = isSafeSingleStrengthForAutoFill(product)
            ?: return@filter false

        val amount = safeCatalog.amount
            .replace(',', '.')
            .toBigDecimalOrNull()

        normalizedMedicineName(product.name) == expectedName &&
            amount != null &&
            amount.compareTo(expectedAmount) == 0 &&
            safeCatalog.unit.equals(ocrUnit, ignoreCase = true) &&
            product.dosage_form.isNotBlank()
    }

    return matching.singleOrNull()
}

@Composable
private fun SourceBadge(source: String) {
    val (bgColor, textColor, label) = when {
        "Việt Nam" in source -> Triple(
            Color(0xFFE0F2EE),
            Color(0xFF0F5B51),
            source.ifBlank { "Việt Nam - Cục Quản lý Dược" }
        )

        "Mỹ" in source || "openFDA" in source -> Triple(
            Color(0xFFE3F2FD),
            Color(0xFF1565C0),
            source
        )

        "Canada" in source -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFB71C1C),
            source
        )

        else -> Triple(
            Color(0xFFF1F5F4),
            Color(0xFF3E4F4C),
            source.ifBlank { "Danh mục thuốc" }
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun WarningChip(text: String, bgColor: Color, borderColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MedicineLookupChooser(
    initialQuery: String,
    scannedStrength: String = "",
    scannedUnit: String = "mg",
    onChoose: (MedicineLookupCandidate) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    val repository = remember {
        MedicineLookupRepository()
    }

    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var query by rememberSaveable(initialQuery) {
        mutableStateOf(initialQuery.trim())
    }

    var manualMode by rememberSaveable(initialQuery) {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var searched by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var errorKind by remember {
        mutableStateOf<LookupErrorKind?>(null)
    }

    var products by remember {
        mutableStateOf(emptyList<MedicineLookupCandidate>())
    }

    var exactResult by remember {
        mutableStateOf<MedicineLookupCandidate?>(null)
    }

    var showAll by remember {
        mutableStateOf(false)
    }

    var searchJob by remember {
        mutableStateOf<Job?>(null)
    }

    var selectedMedicine by remember {
        mutableStateOf<MedicineLookupCandidate?>(null)
    }

    var confirmed by remember {
        mutableStateOf(false)
    }

    fun search(automatic: Boolean, bypassCache: Boolean = false) {
        val keyword = query.trim()

        if (keyword.length < 3) {
            message = "Vui lòng nhập ít nhất 3 ký tự."
            errorKind = null
            return
        }

        keyboard?.hide()
        focusManager.clearFocus()

        searchJob?.cancel()

        searchJob = scope.launch {
            loading = true
            searched = true
            products = emptyList()
            exactResult = null
            message = null
            errorKind = null
            showAll = !automatic

            try {
                val found = repository.find(keyword, bypassCache = bypassCache)
                products = found

                if (found.isEmpty()) {
                    message =
                        "Chưa tìm thấy thuốc phù hợp từ dữ liệu Việt Nam, Mỹ và Canada. " +
                            "Hãy thử tìm theo tên hoạt chất hoặc chọn Nhập thủ công."
                } else if (automatic) {
                    exactResult = findUniqueMedicine(
                        products = found,
                        ocrName = initialQuery,
                        ocrStrength = scannedStrength,
                        ocrUnit = scannedUnit
                    )

                    if (exactResult == null) {
                        showAll = true
                        message =
                            "Có thể có nhiều sản phẩm tương tự. " +
                                "Hãy đối chiếu nguồn, hoạt chất và chọn đúng thuốc trên bao bì."
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: MedicineLookupException) {
                products = emptyList()
                exactResult = null
                errorKind = error.kind
                message = error.message
            } catch (_: Exception) {
                products = emptyList()
                exactResult = null
                errorKind = LookupErrorKind.NETWORK
                message =
                    "Không kết nối được dữ liệu thuốc. " +
                        "Vui lòng kiểm tra Internet và thử lại."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(initialQuery) {
        if (!manualMode && initialQuery.trim().length >= 3) {
            search(automatic = true)
        }
    }

    val displayedProducts = when {
        manualMode || showAll -> products
        exactResult != null -> listOfNotNull(exactResult)
        else -> products
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(
            onClick = {
                searchJob?.cancel()
                keyboard?.hide()
                onBack()
            },
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("← Quay lại", fontSize = 17.sp)
        }

        Text(
            text = "Tìm đúng thuốc",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tra cứu theo tên thuốc, hoạt chất hoặc số đăng ký (Việt Nam, Mỹ, Canada).",
            fontSize = 16.sp,
            lineHeight = 23.sp
        )

        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                searchJob?.cancel()
                loading = false

                query = newValue
                manualMode = true
                searched = false
                message = null
                errorKind = null
                exactResult = null
                products = emptyList()
                showAll = true
            },
            label = { Text("Tên thuốc / Hoạt chất / Số ĐK") },
            placeholder = { Text("Ví dụ: Panfor PR-750 hoặc Metformin") },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    manualMode = true
                    search(automatic = false)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                manualMode = true
                search(automatic = false)
            },
            enabled = query.trim().length >= 3 && !loading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                if (loading) "Đang tìm..." else "Tìm thuốc",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Đang tra cứu dữ liệu thuốc chính thống...",
                fontSize = 16.sp
            )
        }

        message?.let { info ->
            Text(
                text = info,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = if (errorKind != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        if (errorKind != null && !loading) {
            OutlinedButton(
                onClick = {
                    manualMode = true
                    search(automatic = false, bypassCache = true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
            ) {
                Text("Thử lại", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (exactResult != null && !showAll && !manualMode) {
            Text(
                text = "Tìm thấy một sản phẩm khớp",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (displayedProducts.isNotEmpty()) {
            Text(
                text = "Chọn đúng sản phẩm (${displayedProducts.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        displayedProducts.forEach { item ->
            val safeStrength = isSafeSingleStrengthForAutoFill(item)
            val isComplex = hasMultipleActiveIngredients(item.active_ingredient) ||
                (item.strength.isNotBlank() && safeStrength == null)
            val isSpecialRelease = isSpecialReleaseMedicine(item.name, item.dosage_form)

            OutlinedCard(
                onClick = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    selectedMedicine = item
                    confirmed = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SourceBadge(item.source)

                    Text(
                        text = item.name,
                        fontSize = 19.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isSpecialRelease) {
                        WarningChip(
                            text = "Giải phóng kéo dài / đặc biệt",
                            bgColor = Color(0xFFFFF4E5),
                            borderColor = Color(0xFFE6A23C),
                            textColor = Color(0xFF8A4B00)
                        )
                    }

                    if (isComplex) {
                        WarningChip(
                            text = "Nhiều hoạt chất / hàm lượng phức hợp",
                            bgColor = Color(0xFFF3EFFE),
                            borderColor = Color(0xFFD0C4F7),
                            textColor = Color(0xFF4B2B9A)
                        )
                    }

                    Text(
                        text = "Hoạt chất: " + item.active_ingredient.ifBlank {
                            "Chưa có thông tin"
                        },
                        fontSize = 15.sp
                    )

                    Text(
                        text = "Hàm lượng: " + item.strength.ifBlank {
                            "Chưa có thông tin"
                        },
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Dạng bào chế: " + item.dosage_form.ifBlank {
                            "Chưa có thông tin"
                        },
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Số đăng ký / Mã SP: " + item.registration_number.ifBlank {
                            "Chưa có"
                        },
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Nhà sản xuất: " + item.manufacturer.ifBlank {
                            "Chưa có thông tin"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Chọn thuốc này",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (exactResult != null && !manualMode && !showAll) {
            OutlinedButton(
                onClick = { showAll = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
            ) {
                Text("Xem thêm sản phẩm khác", fontSize = 16.sp)
            }
        }

        HorizontalDivider()

        Text(
            text = "Thông tin được lấy từ danh mục thuốc (Cục Quản lý Dược VN, openFDA, Health Canada). " +
                "Hãy luôn đối chiếu với hộp thuốc hoặc đơn thuốc trước khi lưu.",
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        OutlinedButton(
            onClick = {
                searchJob?.cancel()
                keyboard?.hide()
                onSkip()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text("Không thấy thuốc? Nhập thủ công", fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))
    }

    selectedMedicine?.let { item ->
        val safeStrength = isSafeSingleStrengthForAutoFill(item)
        val isComplex = hasMultipleActiveIngredients(item.active_ingredient) ||
            (item.strength.isNotBlank() && safeStrength == null)
        val isSpecialRelease = isSpecialReleaseMedicine(item.name, item.dosage_form)
        val hasMissingFields = item.strength.isBlank() || item.dosage_form.isBlank()

        AlertDialog(
            onDismissRequest = {
                selectedMedicine = null
                confirmed = false
            },
            title = {
                Text(
                    "Kiểm tra trước khi tiếp tục",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SourceBadge(item.source)

                    Text(
                        text = item.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Hoạt chất: " + item.active_ingredient.ifBlank { "Chưa có thông tin" },
                        fontSize = 15.sp
                    )

                    Text(
                        "Hàm lượng: " + item.strength.ifBlank { "Chưa có thông tin" },
                        fontSize = 15.sp
                    )

                    Text(
                        "Dạng thuốc: " + item.dosage_form.ifBlank { "Chưa có thông tin" },
                        fontSize = 15.sp
                    )

                    if (item.registration_number.isNotBlank()) {
                        Text(
                            "Số ĐK / Mã SP: ${item.registration_number}",
                            fontSize = 14.sp
                        )
                    }

                    if (item.manufacturer.isNotBlank()) {
                        Text(
                            "Nhà sản xuất: ${item.manufacturer}",
                            fontSize = 14.sp
                        )
                    }

                    if (isSpecialRelease) {
                        WarningChip(
                            text = "Lưu ý: Thuốc giải phóng kéo dài/đặc biệt. Kiểm tra kỹ bao bì và không tự ý bẻ viên.",
                            bgColor = Color(0xFFFFF4E5),
                            borderColor = Color(0xFFE6A23C),
                            textColor = Color(0xFF8A4B00)
                        )
                    }

                    if (isComplex) {
                        WarningChip(
                            text = "Thuốc có nhiều hoạt chất hoặc hàm lượng phức hợp: hệ thống sẽ không tự điền ô hàm lượng đơn lẻ và sẽ lưu chi tiết vào Ghi chú.",
                            bgColor = Color(0xFFF3EFFE),
                            borderColor = Color(0xFFD0C4F7),
                            textColor = Color(0xFF4B2B9A)
                        )
                    }

                    if (hasMissingFields) {
                        WarningChip(
                            text = "Nguồn thiếu thông tin hàm lượng hoặc dạng thuốc. Hệ thống để trống trường thiếu để bạn tự đối chiếu bao bì.",
                            bgColor = Color(0xFFFFF4E5),
                            borderColor = Color(0xFFE6A23C),
                            textColor = Color(0xFF8A4B00)
                        )
                    }

                    Text(
                        text = "Thông tin tra cứu chỉ mang tính đối chiếu. " +
                            "Vui lòng kiểm tra tên, hàm lượng và dạng thuốc trên hộp.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = confirmed,
                            onCheckedChange = { confirmed = it }
                        )

                        Text(
                            text = "Tôi đã kiểm tra thông tin thuốc",
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = confirmed,
                    onClick = {
                        selectedMedicine = null
                        confirmed = false
                        onChoose(item)
                    }
                ) {
                    Text("Tiếp tục", fontSize = 17.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedMedicine = null
                        confirmed = false
                    }
                ) {
                    Text("Chọn lại", fontSize = 17.sp)
                }
            }
        )
    }
}
