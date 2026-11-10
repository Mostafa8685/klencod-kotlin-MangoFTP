package com.mango.ftp.data.model

data class UserProfile(
    val displayName: String = "مستخدم المانجو",
    val deviceModel: String = android.os.Build.MODEL,
    val isOnboarded: Boolean = false
)
