
package com.example.project_lttbd.ui.screens.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.remote.MedicineLookupCandidate
import com.example.project_lttbd.data.remote.MedicineLookupRepository
import com.example.project_lttbd.data.remote.parseCatalogStrength
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

private fun normalizeMedicineName(value: String): String {
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

private fun findUniqueExactMatch(
    products: List<MedicineLookupCandidate>,
    ocrName: String,
    ocrStrength: String,
    ocrUnit: String
): MedicineLookupCandidate? {

    // Thiếu tên hoặc hàm lượng thì không tự xác định thuốc.
    if (ocrName.isBlank() || ocrStrength.isBlank()) {
        return null
    }

    // API hiện giới hạn 20 kết quả.
    // Nếu đã chạm giới hạn, chưa thể kết luận duy nhất.
    if (products.size >= 20) {
        return null
    }

    val expectedName = normalizeMedicineName(ocrName)
    val expectedAmount = ocrStrength
        .replace(',', '.')
        .toBigDecimalOrNull() ?: return null

    val matches = products.filter { product ->
        val catalogStrength = parseCatalogStrength(
            product.strength
        )

        val catalogAmount = catalogStrength
            ?.amount
            ?.replace(',', '.')
            ?.toBigDecimalOrNull()

        normalizeMedicineName(product.name) == expectedName &&
                catalogAmount != null &&
                catalogAmount.compareTo(expectedAmount) == 0 &&
                catalogStrength.unit.equals(
                    ocrUnit,
                    ignoreCase = true
                ) &&
                product.dosage_form.isNotBlank()
    }

    // Không tự gộp nhiều mã sản phẩm thành một thuốc.
    return matches.singleOrNull()
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

    var query by rememberSaveable(initialQuery) {
        mutableStateOf(initialQuery.trim())
    }

    var manualMode by rememberSaveable {
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

    var allResults by remember {
        mutableStateOf(
            emptyList<MedicineLookupCandidate>()
        )
    }

    var exactResult by remember {
        mutableStateOf<MedicineLookupCandidate?>(null)
    }

    var showAllResults by remember {
        mutableStateOf(false)
    }

    var searchJob by remember {
        mutableStateOf<Job?>(null)
    }

    fun search(isAutomatic: Boolean) {
        val keyword = query.trim()

        if (keyword.length < 3) {
            message = "Hãy nhập ít nhất 3 ký tự tên thuốc."
            return
        }

        searchJob?.cancel()

        searchJob = scope.launch {
            loading = true
            searched = true
            message = null
            exactResult = null
            allResults = emptyList()
            showAllResults = !isAutomatic

            try {
                val products = repository.find(keyword)

                allResults = products

                if (products.isEmpty()) {
                    message =
                        "Không tìm thấy thuốc phù hợp. " +
                                "Bạn có thể sửa tên hoặc nhập thủ công."
                } else if (isAutomatic) {
                    exactResult = findUniqueExactMatch(
                        products = products,
                        ocrName = initialQuery,
                        ocrStrength = scannedStrength,
                        ocrUnit = scannedUnit
                    )

                    if (exactResult == null) {
                        showAllResults = true
                        message =
                            "Chưa xác định chắc chắn thuốc. " +
                                    "Vui lòng chọn đúng sản phẩm trên hộp."
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                allResults = emptyList()
                exactResult = null

                message =
                    "Không tra cứu được thuốc. " +
                            "Hãy kiểm tra Internet hoặc thử lại."
            } finally {
                loading = false
            }
        }
    }

    // Tự tra cứu ngay sau khi OCR, nếu đã nhận diện được tên.
    LaunchedEffect(initialQuery) {
        if (
            !manualMode &&
            initialQuery.trim().length >= 3
        ) {
            search(isAutomatic = true)
        }
    }

    val displayedResults = when {
        manualMode || showAllResults -> allResults

        exactResult != null -> listOfNotNull(exactResult)

        else -> allResults
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(
            onClick = {
                searchJob?.cancel()
                onBack()
            },
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                "← Quay lại",
                fontSize = 17.sp
            )
        }

        Text(
            text = "Tìm đúng thuốc",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Kiểm tra tên và hàm lượng trên hộp " +
                    "trước khi chọn thuốc.",
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
                exactResult = null
                allResults = emptyList()
                showAllResults = true
                message = null
            },
            label = {
                Text("Tên thuốc")
            },
            placeholder = {
                Text("Ví dụ: Tylenol")
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 18.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                manualMode = true
                search(isAutomatic = false)
            },
            enabled = query.trim().length >= 3 && !loading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                if (loading) "Đang tìm thuốc..." else "Tìm thuốc",
                fontSize = 18.sp
            )
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Đang kiểm tra dữ liệu sản phẩm...",
                fontSize = 16.sp
            )
        }

        message?.let { info ->
            Text(
                text = info,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (exactResult != null && !showAllResults) {
            Text(
                text = "Đã tìm thấy một sản phẩm khớp",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (displayedResults.isNotEmpty()) {
            Text(
                text = "Chọn sản phẩm phù hợp",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!searched && !loading) {
                Text(
                    "Bạn có thể sửa tên thuốc ở trên rồi tìm lại.",
                    fontSize = 16.sp
                )
            }

            displayedResults.forEach { item ->
                OutlinedCard(
                    onClick = {
                        searchJob?.cancel()
                        onChoose(item)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 19.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Hàm lượng: " +
                                    item.strength.ifBlank {
                                        "Chưa có thông tin"
                                    },
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Dạng thuốc: " +
                                    item.dosage_form.ifBlank {
                                        "Chưa có thông tin"
                                    },
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Mã NDC: " +
                                    item.registration_number.ifBlank {
                                        "Không có"
                                    },
                            fontSize = 14.sp
                        )

                        Text(
                            text = "Nguồn: ${item.source}",
                            fontSize = 13.sp
                        )

                        Text(
                            text = "Chọn thuốc này",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            if (
                exactResult != null &&
                !manualMode &&
                !showAllResults
            ) {
                OutlinedButton(
                    onClick = {
                        showAllResults = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                ) {
                    Text(
                        "Xem thêm kết quả khác",
                        fontSize = 16.sp
                    )
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Thông tin tra cứu chỉ để đối chiếu sản phẩm, " +
                    "không thay thế hướng dẫn của bác sĩ hoặc dược sĩ.",
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        OutlinedButton(
            onClick = {
                searchJob?.cancel()
                onSkip()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                "Không thấy thuốc? Nhập thủ công",
                fontSize = 16.sp
            )
        }
    }
}
