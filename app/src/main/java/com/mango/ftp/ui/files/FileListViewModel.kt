package com.mango.ftp.ui.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mango.ftp.data.ftp.FileParser
import com.mango.ftp.data.ftp.FtpManager
import com.mango.ftp.data.local.PreferencesManager
import com.mango.ftp.data.model.FileCategory
import com.mango.ftp.data.model.FtpFileItem
import com.mango.ftp.data.model.FtpServerConfig
import com.mango.ftp.data.model.TransferProgress
import com.mango.ftp.data.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

class FileListViewModel(
    private val ftpManager: FtpManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _rawFiles = MutableStateFlow<List<FtpFileItem>>(emptyList())
    val rawFiles: StateFlow<List<FtpFileItem>> = _rawFiles.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FileCategory.ALL)
    val selectedCategory: StateFlow<FileCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _transferProgress = MutableStateFlow<TransferProgress?>(null)
    val transferProgress: StateFlow<TransferProgress?> = _transferProgress.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val ftpConfig: StateFlow<FtpServerConfig> = preferencesManager.ftpConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, FtpServerConfig())

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    private var transferJob: Job? = null

    val filteredFiles: StateFlow<List<FtpFileItem>> = combine(
        _rawFiles,
        _selectedCategory,
        _searchQuery
    ) { files, category, query ->
        files.filter { file ->
            val matchCategory = when (category) {
                FileCategory.ALL -> true
                FileCategory.FOLDER -> file.isDirectory
                else -> file.category == category
            }
            val matchQuery = if (query.isBlank()) true else {
                file.cleanName.contains(query, ignoreCase = true) ||
                file.uploaderName?.contains(query, ignoreCase = true) == true ||
                file.deviceModel?.contains(query, ignoreCase = true) == true
            }
            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            ftpConfig.collect { config ->
                // Initial auto-connect
                connectAndRefresh()
            }
        }
    }

    fun toggleLayout() {
        _isGridView.value = !_isGridView.value
    }

    fun setCategory(category: FileCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun connectAndRefresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val config = ftpConfig.value
            val connectResult = ftpManager.connect(config)
            if (connectResult.isSuccess) {
                fetchFiles(_currentPath.value)
            } else {
                _isLoading.value = false
                _errorMessage.value = connectResult.exceptionOrNull()?.localizedMessage ?: "فشل الاتصال براوتر FTP"
            }
        }
    }

    fun fetchFiles(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentPath.value = path

            val listResult = ftpManager.listFiles(path)
            _isLoading.value = false
            if (listResult.isSuccess) {
                _rawFiles.value = listResult.getOrDefault(emptyList())
            } else {
                _errorMessage.value = listResult.exceptionOrNull()?.localizedMessage ?: "تعذر جلب قائمة الملفات"
            }
        }
    }

    fun navigateInto(item: FtpFileItem) {
        if (item.isDirectory) {
            val nextPath = if (_currentPath.value.endsWith("/")) {
                "${_currentPath.value}${item.rawName}"
            } else {
                "${_currentPath.value}/${item.rawName}"
            }
            fetchFiles(nextPath)
        }
    }

    fun navigateUp(): Boolean {
        val curr = _currentPath.value.trimEnd('/')
        if (curr.isEmpty() || curr == "/") return false
        val lastSlash = curr.lastIndexOf('/')
        val parent = if (lastSlash <= 0) "/" else curr.substring(0, lastSlash)
        fetchFiles(parent)
        return true
    }

    fun downloadFile(item: FtpFileItem, context: Context, onCompleted: (File) -> Unit) {
        transferJob?.cancel()
        transferJob = viewModelScope.launch {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val mangoDir = File(downloadDir, "MangoFTP").apply { mkdirs() }
            val localTargetFile = File(mangoDir, item.cleanName)

            val remotePath = item.fullPath
            val result = ftpManager.downloadFile(
                remotePath = remotePath,
                destinationFile = localTargetFile,
                fileName = item.cleanName,
                fileSize = item.size,
                onProgress = { progress ->
                    _transferProgress.value = progress
                }
            )

            if (result.isSuccess) {
                onCompleted(result.getOrThrow())
            } else {
                _errorMessage.value = "فشل التنزيل: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun uploadFile(
        uri: Uri,
        originalName: String,
        fileSize: Long,
        context: Context
    ) {
        transferJob?.cancel()
        transferJob = viewModelScope.launch {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("تعذر قراءة الملف المختار")

                val profile = userProfile.value
                val smartName = FileParser.formatUploadName(
                    originalName = originalName,
                    userName = profile.displayName,
                    deviceModel = profile.deviceModel
                )

                val result = ftpManager.uploadFile(
                    sourceInputStream = inputStream,
                    remoteDirectory = _currentPath.value,
                    smartFileName = smartName,
                    fileSize = fileSize,
                    onProgress = { progress ->
                        _transferProgress.value = progress
                    }
                )

                if (result.isSuccess) {
                    // Refresh file list to see newly uploaded smart file
                    fetchFiles(_currentPath.value)
                } else {
                    _errorMessage.value = "فشل الرفع: ${result.exceptionOrNull()?.localizedMessage}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "خطأ في تجهيز الرفع: ${e.localizedMessage}"
            }
        }
    }

    fun cancelTransfer() {
        transferJob?.cancel()
        transferJob = null
        _transferProgress.value = null
    }

    fun dismissProgress() {
        _transferProgress.value = null
    }

    fun deleteFile(item: FtpFileItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = ftpManager.deleteFile(item.fullPath)
            _isLoading.value = false
            if (res.isSuccess) {
                fetchFiles(_currentPath.value)
            } else {
                _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "فشل حذف الملف"
            }
        }
    }

    fun createDirectory(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = ftpManager.createDirectory(_currentPath.value, name.trim())
            _isLoading.value = false
            if (res.isSuccess) {
                fetchFiles(_currentPath.value)
            } else {
                _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء المجلد"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
