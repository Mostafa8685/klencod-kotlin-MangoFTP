package com.mango.ftp.data.model

data class FtpFileItem(
    val rawName: String,
    val cleanName: String,
    val uploaderName: String? = null,
    val deviceModel: String? = null,
    val size: Long = 0L,
    val modifiedTimeMillis: Long = 0L,
    val isDirectory: Boolean = false,
    val extension: String = "",
    val category: FileCategory = FileCategory.OTHER,
    val fullPath: String = ""
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "مجلد"
            if (size <= 0) return "0 B"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }

    val isSmartNamed: Boolean
        get() = !uploaderName.isNullOrBlank() || !deviceModel.isNullOrBlank()
}
