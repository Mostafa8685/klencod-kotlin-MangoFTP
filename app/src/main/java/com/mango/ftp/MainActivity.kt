package com.mango.ftp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mango.ftp.ui.chat.ChatScreen
import com.mango.ftp.ui.chat.ChatViewModel
import com.mango.ftp.ui.components.MangoBottomNav
import com.mango.ftp.ui.components.MangoTopBar
import com.mango.ftp.ui.components.TransferProgressDialog
import com.mango.ftp.ui.files.FileListScreen
import com.mango.ftp.ui.files.FileListViewModel
import com.mango.ftp.ui.navigation.Screen
import com.mango.ftp.ui.onboarding.OnboardingDialog
import com.mango.ftp.ui.settings.SettingsScreen
import com.mango.ftp.ui.settings.SettingsViewModel
import com.mango.ftp.ui.theme.MangoFTPTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var app: MangoApp
    private lateinit var fileListViewModel: FileListViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    // File Picker Launcher for FTP upload
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleUploadUri(it) }
    }

    // Permission Request Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions response
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as MangoApp

        fileListViewModel = FileListViewModel(app.ftpManager, app.preferencesManager)
        chatViewModel = ChatViewModel(app.nsdHelper, app.chatSocketManager, app.preferencesManager)
        settingsViewModel = SettingsViewModel(app.ftpManager, app.preferencesManager)

        checkAndRequestPermissions()

        setContent {
            MangoFTPTheme {
                MainScreenContent()
            }
        }
    }

    @Composable
    private fun MainScreenContent() {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Files) }
        val userProfile by fileListViewModel.userProfile.collectAsState()
        val transferProgress by fileListViewModel.transferProgress.collectAsState()

        // Show onboarding if not yet completed
        if (!userProfile.isOnboarded) {
            OnboardingDialog(
                initialName = userProfile.displayName,
                onSaveProfile = { name, device ->
                    settingsViewModel.saveProfile(name, device)
                }
            )
        }

        // Show transfer progress dialog if upload/download is active
        transferProgress?.let { progress ->
            TransferProgressDialog(
                progress = progress,
                onCancel = { fileListViewModel.cancelTransfer() },
                onDismiss = { fileListViewModel.dismissProgress() }
            )
        }

        Scaffold(
            topBar = {
                MangoTopBar(
                    title = currentScreen.titleArabic,
                    isConnected = app.ftpManager.isConnected,
                    onRefreshClick = if (currentScreen == Screen.Files) {
                        { fileListViewModel.connectAndRefresh() }
                    } else null
                )
            },
            bottomBar = {
                MangoBottomNav(
                    currentScreen = currentScreen,
                    onScreenSelected = { currentScreen = it }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.Files -> FileListScreen(
                        viewModel = fileListViewModel,
                        onUploadClick = { filePickerLauncher.launch("*/*") },
                        onFileDownloaded = { file -> openDownloadedFile(file) }
                    )
                    Screen.Chat -> ChatScreen(
                        viewModel = chatViewModel
                    )
                    Screen.Settings -> SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }

    private fun handleUploadUri(uri: Uri) {
        var fileName = "file_${System.currentTimeMillis()}"
        var fileSize = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        fileListViewModel.uploadFile(uri, fileName, fileSize, this)
    }

    private fun openDownloadedFile(file: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val mimeType = contentResolver.getType(contentUri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(Intent.createChooser(intent, "فتح الملف عبر:"))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الملف تلقائياً: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}