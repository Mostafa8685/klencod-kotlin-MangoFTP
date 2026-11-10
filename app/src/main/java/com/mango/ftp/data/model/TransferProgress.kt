package com.mango.ftp.data.model

data class TransferProgress(
    val fileName: String = "",
    val isUpload: Boolean = false,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val isIndeterminate: Boolean = false,
    val isCompleted: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val formattedSpeed: String
        get() {
            val kb = speedBytesPerSec / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.2f MB/s", mb)
                kb >= 1.0 -> String.format("%.1f KB/s", kb)
                else -> "$speedBytesPerSec B/s"
            }
        }
}
