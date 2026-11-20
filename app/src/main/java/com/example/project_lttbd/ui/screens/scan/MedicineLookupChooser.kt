
package com.example.project_lttbd.ui.screens.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    // API có giới hạn số kết quả. Không tự kết luận
    // duy nhất nếu danh sách chạm ngưỡng giới hạn.
    if (products.size >= 20) {
        return null
    }

    val expectedName = normalizedMedicineName(ocrName)

    val expectedAmount = ocrStrength
        .replace(',', '.')
        .toBigDecimalOrNull() ?: return null

    val matching = products.filter { product ->
        val catalog = parseCatalogStrength(
            product.strength
        )

        val amount = catalog
            ?.amount
            ?.replace(',', '.')
            ?.toBigDecimalOrNull()

        normalizedMedicineName(product.name) == expectedName &&
                amount != null &&
                amount.compareTo(expectedAmount) == 0 &&
                catalog.unit.equals(
                    ocrUnit,
                    ignoreCase = true
                ) &&
                product.dosage_form.isNotBlank()
    }

    // Có nhiều mã sản phẩm cùng thông tin thì
    // người dùng cần tự chọn.
    return matching.singleOrNull()
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

    var products by remember {
        mutableStateOf(
            emptyList<MedicineLookupCandidate>()
        )
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

    fun search(automatic: Boolean) {
        val keyword = query.trim()

        if (keyword.length < 3) {
            message = "Vui lòng nhập ít nhất 3 ký tự."
            return
        }

        // Ẩn bàn phím và bỏ focus khi tìm.
        keyboard?.hide()
        focusManager.clearFocus()

        searchJob?.cancel()

        searchJob = scope.launch {
            loading = true
            searched = true
            products = emptyList()
            exactResult = null
            message = null
            showAll = !automatic

            try {
                val found = repository.find(keyword)

                products = found

                if (found.isEmpty()) {
                    message =
                        "Chưa tìm thấy thuốc. " +
                                "Bạn hãy kiểm tra tên hoặc tìm bằng " +
                                "một từ khóa khác."
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
                                    "Hãy chọn đúng thuốc trên bao bì."
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                products = emptyList()
                exactResult = null

                message =
                    "Không kết nối được dữ liệu thuốc. " +
                            "Vui lòng kiểm tra Internet và thử lại."
            } finally {
                loading = false
            }
        }
    }

    // Chỉ tự tìm khi OCR đã đọc được tên.
    LaunchedEffect(initialQuery) {
        if (
            !manualMode &&
            initialQuery.trim().length >= 3
        ) {
            search(automatic = true)
        }
    }

    val displayedProducts = when {
        manualMode || showAll -> products

        exactResult != null ->
            listOfNotNull(exactResult)

        else -> products
    }

    // Toàn bộ màn hình cuộn được khi mở bàn phím.
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
            text = "Kiểm tra tên thuốc trên hộp. " +
                    "Nếu đọc sai, bạn có thể sửa bên dưới.",
            fontSize = 17.sp,
            lineHeight = 24.sp
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
                exactResult = null
                products = emptyList()
                showAll = true
            },
            label = {
                Text("Tên thuốc")
            },
            placeholder = {
                Text("Nhập tên trên hộp thuốc")
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 18.sp
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    manualMode = true
                    search(automatic = false)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Bạn có thể vuốt màn hình lên để " +
                    "nhấn nút Tìm thuốc.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Đang kiểm tra sản phẩm...",
                fontSize = 16.sp
            )
        }

        message?.let { info ->
            Text(
                text = info,
                fontSize = 16.sp,
                lineHeight = 23.sp
            )
        }

        if (
            exactResult != null &&
            !showAll &&
            !manualMode
        ) {
            Text(
                text = "Tìm thấy một sản phẩm khớp",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (displayedProducts.isNotEmpty()) {
            Text(
                text = "Chọn đúng sản phẩm",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        displayedProducts.forEach { item ->
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
                        text = "Hoạt chất: " +
                                item.active_ingredient.ifBlank {
                                    "Chưa có thông tin"
                                },
                        fontSize = 15.sp
                    )

                    Text(
                        text = "Mã sản phẩm: " +
                                item.registration_number.ifBlank {
                                    "Chưa có"
                                },
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Nguồn: ${item.source}",
                        fontSize = 14.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
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

        if (
            exactResult != null &&
            !manualMode &&
            !showAll
        ) {
            OutlinedButton(
                onClick = {
                    showAll = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
            ) {
                Text(
                    "Xem thêm sản phẩm khác",
                    fontSize = 16.sp
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "Thông tin được lấy từ danh mục thuốc. " +
                    "Hãy luôn đối chiếu với hộp thuốc hoặc " +
                    "đơn thuốc trước khi lưu.",
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
            Text(
                "Không thấy thuốc? Nhập thủ công",
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(12.dp))
    }

    selectedMedicine?.let { item ->
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
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Hàm lượng: " +
                                item.strength.ifBlank {
                                    "Chưa có thông tin"
                                },
                        fontSize = 16.sp
                    )

                    Text(
                        "Dạng thuốc: " +
                                item.dosage_form.ifBlank {
                                    "Chưa có thông tin"
                                },
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Thông tin tra cứu có thể không " +
                                "trùng hoàn toàn với hộp thuốc. " +
                                "Vui lòng kiểm tra tên, hàm lượng " +
                                "và dạng thuốc.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )

                    Row(
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = confirmed,
                            onCheckedChange = {
                                confirmed = it
                            }
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
                    Text(
                        "Tiếp tục",
                        fontSize = 17.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedMedicine = null
                        confirmed = false
                    }
                ) {
                    Text(
                        "Chọn lại",
                        fontSize = 17.sp
                    )
                }
            }
        )
    }
}
