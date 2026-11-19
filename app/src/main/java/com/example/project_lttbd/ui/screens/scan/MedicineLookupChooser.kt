
package com.example.project_lttbd.ui.screens.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.remote.MedicineLookupCandidate
import com.example.project_lttbd.data.remote.MedicineLookupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun MedicineLookupChooser(
    initialQuery: String,
    onChoose: (MedicineLookupCandidate) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    val repository = remember {
        MedicineLookupRepository()
    }

    val scope = rememberCoroutineScope()

    var query by remember {
        mutableStateOf(initialQuery.trim())
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

    var results by remember {
        mutableStateOf(
            emptyList<MedicineLookupCandidate>()
        )
    }

    fun search() {
        if (query.trim().length < 3 || loading) {
            return
        }

        scope.launch {
            loading = true
            message = null
            searched = true

            try {
                results = repository.find(query)

                if (results.isEmpty()) {
                    message =
                        "Không tìm thấy sản phẩm phù hợp. " +
                                "Hãy sửa từ khóa hoặc nhập thủ công."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                results = emptyList()
                message =
                    "Chưa tra cứu được dữ liệu thuốc. " +
                            "Kiểm tra kết nối FastAPI và khóa API."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text("← Quay lại")
        }

        Text(
            text = "Đối chiếu sản phẩm thuốc",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Chọn đúng sản phẩm trên hộp. " +
                    "Dữ liệu được lấy từ danh mục thuốc, " +
                    "không suy đoán từ ảnh."
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
            },
            label = {
                Text("Tên thuốc / số đăng ký")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                search()
            },
            enabled = query.trim().length >= 3 && !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (loading) {
                    "Đang tra cứu..."
                } else {
                    "Tìm sản phẩm"
                }
            )
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!searched) {
                Text(
                    "Nhập tên thuốc rồi nhấn Tìm sản phẩm."
                )
            }

            results.forEach { item ->
                OutlinedCard(
                    onClick = {
                        onChoose(item)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Hàm lượng: " +
                                    item.strength.ifBlank {
                                        "Chưa có"
                                    }
                        )

                        Text(
                            "Dạng bào chế: " +
                                    item.dosage_form.ifBlank {
                                        "Chưa có"
                                    }
                        )

                        Text(
                            "Số đăng ký: " +
                                    item.registration_number.ifBlank {
                                        "Không có"
                                    }
                        )

                        Text(
                            text = "Nguồn: ${item.source}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Không thấy thuốc? Nhập thủ công")
        }
    }
}
