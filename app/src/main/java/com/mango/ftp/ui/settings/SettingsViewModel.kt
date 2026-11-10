package com.mango.ftp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mango.ftp.data.ftp.FtpManager
import com.mango.ftp.data.local.PreferencesManager
import com.mango.ftp.data.model.FtpServerConfig
import com.mango.ftp.data.model.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val ftpManager: FtpManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val ftpConfig: StateFlow<FtpServerConfig> = preferencesManager.ftpConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, FtpServerConfig())

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: StateFlow<String?> = _testStatus.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    fun saveFtpSettings(
        host: String,
        port: Int,
        username: String,
        password: String,
        isAnon: Boolean,
        defaultPath: String
    ) {
        viewModelScope.launch {
            val newConfig = FtpServerConfig(
                host = host.trim(),
                port = port,
                username = username.trim(),
                password = password,
                isAnonymous = isAnon,
                defaultPath = defaultPath.trim()
            )
            preferencesManager.saveFtpConfig(newConfig)
        }
    }

    fun saveProfile(displayName: String, deviceModel: String) {
        viewModelScope.launch {
            preferencesManager.saveUserProfile(displayName, deviceModel)
        }
    }

    fun testFtpConnection(tempConfig: FtpServerConfig) {
        viewModelScope.launch {
            _isTesting.value = true
            _testStatus.value = "جاري اختبار الاتصال بعنوان ${tempConfig.host}:${tempConfig.port}..."
            val result = ftpManager.testConnection(tempConfig)
            _isTesting.value = false
            if (result.isSuccess) {
                _testStatus.value = "نجح: ${result.getOrNull()}"
            } else {
                _testStatus.value = "فشل: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearTestStatus() {
        _testStatus.value = null
    }
}
