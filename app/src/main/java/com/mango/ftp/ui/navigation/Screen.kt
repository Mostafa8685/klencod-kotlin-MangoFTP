package com.mango.ftp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val titleArabic: String, val icon: ImageVector) {
    object Files : Screen("files", "ملفات الراوتر", Icons.Default.Folder)
    object Chat : Screen("chat", "شات العائلة", Icons.Default.Chat)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
}
