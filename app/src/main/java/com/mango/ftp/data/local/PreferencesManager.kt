package com.mango.ftp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mango.ftp.data.model.FtpServerConfig
import com.mango.ftp.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mango_ftp_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_display_name")
        val KEY_DEVICE_MODEL = stringPreferencesKey("device_model")
        val KEY_ONBOARDED = booleanPreferencesKey("is_onboarded")

        val KEY_FTP_HOST = stringPreferencesKey("ftp_host")
        val KEY_FTP_PORT = intPreferencesKey("ftp_port")
        val KEY_FTP_USER = stringPreferencesKey("ftp_user")
        val KEY_FTP_PASS = stringPreferencesKey("ftp_pass")
        val KEY_FTP_ANON = booleanPreferencesKey("ftp_is_anon")
        val KEY_FTP_PATH = stringPreferencesKey("ftp_default_path")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            UserProfile(
                displayName = prefs[KEY_USER_NAME] ?: "مستخدم المانجو",
                deviceModel = prefs[KEY_DEVICE_MODEL] ?: android.os.Build.MODEL,
                isOnboarded = prefs[KEY_ONBOARDED] ?: false
            )
        }

    val ftpConfigFlow: Flow<FtpServerConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            FtpServerConfig(
                host = prefs[KEY_FTP_HOST] ?: "192.168.1.1",
                port = prefs[KEY_FTP_PORT] ?: 21,
                username = prefs[KEY_FTP_USER] ?: "admin",
                password = prefs[KEY_FTP_PASS] ?: "",
                isAnonymous = prefs[KEY_FTP_ANON] ?: false,
                defaultPath = prefs[KEY_FTP_PATH] ?: "/"
            )
        }

    suspend fun saveUserProfile(displayName: String, deviceModel: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = displayName.trim()
            prefs[KEY_DEVICE_MODEL] = deviceModel.trim()
            prefs[KEY_ONBOARDED] = true
        }
    }

    suspend fun saveFtpConfig(config: FtpServerConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FTP_HOST] = config.host.trim()
            prefs[KEY_FTP_PORT] = config.port
            prefs[KEY_FTP_USER] = config.username.trim()
            prefs[KEY_FTP_PASS] = config.password
            prefs[KEY_FTP_ANON] = config.isAnonymous
            prefs[KEY_FTP_PATH] = config.defaultPath.trim()
        }
    }
}
