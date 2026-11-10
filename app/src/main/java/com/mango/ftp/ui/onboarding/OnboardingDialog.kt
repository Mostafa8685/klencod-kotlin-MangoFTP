package com.mango.ftp.ui.onboarding

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mango.ftp.ui.theme.*

@Composable
fun OnboardingDialog(
    initialName: String,
    onSaveProfile: (displayName: String, deviceModel: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }
    val detectedDevice = remember { Build.MODEL ?: "Android Phone" }

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🥭", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "مرحباً بك في Mango FTP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MangoOrange
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "مشاركة الملفات الضخمة عبر فلاشة الراوتر والشات العائلي المباشر بدون إنترنت",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MangoTextMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("اسمك المعروض (Display Name)") },
                    placeholder = { Text("مثال: أحمد، سارة، بابا...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MangoCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📱 نوع الهاتف المكتشف:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MangoTextDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = detectedDevice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MangoOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val validName = if (nameInput.isNotBlank()) nameInput else "مستخدم المانجو"
                        onSaveProfile(validName, detectedDevice)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MangoOrange)
                ) {
                    Text(
                        text = "ابدأ الاستخدام الآن 🚀",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
