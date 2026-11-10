package com.mango.ftp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mango.ftp.data.model.TransferProgress
import com.mango.ftp.ui.theme.*

@Composable
fun TransferProgressDialog(
    progress: TransferProgress,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (progress.isCompleted || progress.isError) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val title = when {
                    progress.isError -> "حدث خطأ أثناء النقل"
                    progress.isCompleted -> if (progress.isUpload) "تم الرفع بنجاح!" else "تم التنزيل بنجاح!"
                    progress.isUpload -> "جاري الرفع إلى فلاشة الراوتر..."
                    else -> "جاري التنزيل من الراوتر..."
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (progress.isError) MaterialTheme.colorScheme.error else MangoOrange
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = progress.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (progress.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MangoLeafGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MangoOrange)
                    ) {
                        Text("تم", color = Color.White)
                    }
                } else if (progress.isError) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progress.errorMessage ?: "خطأ غير معروف",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MangoOrange)
                    ) {
                        Text("إغلاق", color = Color.White)
                    }
                } else {
                    // Ongoing transfer
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MangoOrange,
                        trackColor = MangoCardStroke
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${progress.progressPercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MangoOrange
                        )
                        Text(
                            text = "السرعة: ${progress.formattedSpeed}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MangoTextMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("إلغاء العملية")
                    }
                }
            }
        }
    }
}
