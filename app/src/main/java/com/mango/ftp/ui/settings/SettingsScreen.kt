package com.mango.ftp.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mango.ftp.data.model.FtpServerConfig
import com.mango.ftp.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val ftpConfig by viewModel.ftpConfig.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val testStatus by viewModel.testStatus.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    var hostInput by remember(ftpConfig) { mutableStateOf(ftpConfig.host) }
    var portInput by remember(ftpConfig) { mutableStateOf(ftpConfig.port.toString()) }
    var userInput by remember(ftpConfig) { mutableStateOf(ftpConfig.username) }
    var passInput by remember(ftpConfig) { mutableStateOf(ftpConfig.password) }
    var isAnonInput by remember(ftpConfig) { mutableStateOf(ftpConfig.isAnonymous) }
    var pathInput by remember(ftpConfig) { mutableStateOf(ftpConfig.defaultPath) }
    var passwordVisible by remember { mutableStateOf(false) }

    var displayNameInput by remember(userProfile) { mutableStateOf(userProfile.displayName) }
    var deviceModelInput by remember(userProfile) { mutableStateOf(userProfile.deviceModel) }

    var showSavedSnackbar by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Router FTP Settings Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Router, contentDescription = null, tint = MangoOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إعدادات اتصال FTP الراوتر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MangoOrange
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("عنوان IP الراوتر") },
                        placeholder = { Text("192.168.1.1") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )

                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("المنفذ (Port)") },
                        placeholder = { Text("21") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("اسم المستخدم (Username)") },
                    singleLine = true,
                    enabled = !isAnonInput,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passInput,
                    onValueChange = { passInput = it },
                    label = { Text("كلمة المرور (Password)") },
                    singleLine = true,
                    enabled = !isAnonInput,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "تبديل الرؤية"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isAnonInput,
                        onCheckedChange = { isAnonInput = it },
                        colors = CheckboxDefaults.colors(checkedColor = MangoOrange)
                    )
                    Text("تسجيل دخول كزائر (Anonymous)", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Connection & Save Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val tempConfig = FtpServerConfig(
                                host = hostInput,
                                port = portInput.toIntOrNull() ?: 21,
                                username = userInput,
                                password = passInput,
                                isAnonymous = isAnonInput,
                                defaultPath = pathInput
                            )
                            viewModel.testFtpConnection(tempConfig)
                        },
                        enabled = !isTesting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("اختبار الاتصال")
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.saveFtpSettings(
                                host = hostInput,
                                port = portInput.toIntOrNull() ?: 21,
                                username = userInput,
                                password = passInput,
                                isAnon = isAnonInput,
                                defaultPath = pathInput
                            )
                            showSavedSnackbar = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MangoOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ الإعدادات", color = Color.White)
                    }
                }

                if (testStatus != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (testStatus?.startsWith("نجح") == true) MangoLeafGreen.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = testStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // User Profile Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = MangoOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الملف الشخصي وهوية الرفع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MangoOrange
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    label = { Text("الاسم المعروض (الذي يظهر للآخرين)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deviceModelInput,
                    onValueChange = { deviceModelInput = it },
                    label = { Text("نوع الهاتف المكتشف تلقائياً") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.saveProfile(displayNameInput, deviceModelInput)
                        showSavedSnackbar = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MangoOrange),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("تحديث الملف الشخصي", color = Color.White)
                }
            }
        }

        // About Mango FTP Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MangoCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🥭 حول تطبيق Mango FTP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MangoTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "تطبيق محلي 100% يعمل داخل شبكة الواي فاي المنزلية بدون إنترنت. تم تصميمه للاستفادة القصوى من مدخل USB في راوتر المنزل لمشاركة الملفات الضخمة (أفلام، مسلسلات، صور عائلية) بسرعات فائقة، مع دردشة فورية بين هواتف الأسرة بتقنية NSD المباشرة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MangoTextMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "الإصدار: 1.0.0 (Clean Architecture & Jetpack Compose)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MangoOrangeDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
