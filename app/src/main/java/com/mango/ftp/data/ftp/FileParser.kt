package com.mango.ftp.data.ftp

import com.mango.ftp.data.model.FileCategory
import com.mango.ftp.data.model.FtpFileItem
import org.apache.commons.net.ftp.FTPFile

object FileParser {

    // Pattern: [Original_Name]__by_[UserName]__device_[DeviceModel].[ext]
    private val SMART_UPLOAD_REGEX = Regex("^(.*)__by_(.*)__device_(.*?)(?:\\.([^.]+))?$")

    fun formatUploadName(originalName: String, userName: String, deviceModel: String): String {
        val sanitizedUser = userName.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-\\s\\u0600-\\u06FF]"), "")
            .replace("\\s+".toRegex(), "_")
        val sanitizedDevice = deviceModel.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .replace("\\s+".toRegex(), "_")

        val dotIdx = originalName.lastIndexOf('.')
        val baseName = if (dotIdx != -1) originalName.substring(0, dotIdx) else originalName
        val extension = if (dotIdx != -1) originalName.substring(dotIdx) else ""

        val cleanBase = baseName.replace(Regex("[^a-zA-Z0-9_\\-\\s\\u0600-\\u06FF.]"), "").replace("\\s+".toRegex(), "_")
        return "${cleanBase}__by_${sanitizedUser}__device_${sanitizedDevice}${extension}"
    }

    fun parseFtpFile(ftpFile: FTPFile, currentPath: String): FtpFileItem {
        val rawName = ftpFile.name ?: ""
        val isDirectory = ftpFile.isDirectory
        val fullPath = if (currentPath.endsWith("/")) "$currentPath$rawName" else "$currentPath/$rawName"
        val modifiedTime = ftpFile.timestamp?.timeInMillis ?: 0L
        val size = ftpFile.size

        if (isDirectory) {
            return FtpFileItem(
                rawName = rawName,
                cleanName = rawName,
                uploaderName = null,
                deviceModel = null,
                size = size,
                modifiedTimeMillis = modifiedTime,
                isDirectory = true,
                extension = "",
                category = FileCategory.FOLDER,
                fullPath = fullPath
            )
        }

        val match = SMART_UPLOAD_REGEX.find(rawName)
        val (cleanName, uploader, device, ext) = if (match != null) {
            val base = match.groupValues[1].replace("_", " ")
            val user = match.groupValues[2].replace("_", " ")
            val dev = match.groupValues[3].replace("_", " ")
            val extension = match.groupValues.getOrNull(4) ?: ""
            val fullClean = if (extension.isNotEmpty()) "$base.$extension" else base
            Quadruple(fullClean, user, dev, extension)
        } else {
            val extension = rawName.substringAfterLast('.', "")
            Quadruple(rawName, null, null, extension)
        }

        val category = categorizeByExtension(ext)

        return FtpFileItem(
            rawName = rawName,
            cleanName = cleanName,
            uploaderName = uploader,
            deviceModel = device,
            size = size,
            modifiedTimeMillis = modifiedTime,
            isDirectory = false,
            extension = ext,
            category = category,
            fullPath = fullPath
        )
    }

    fun categorizeByExtension(extension: String): FileCategory {
        return when (extension.lowercase()) {
            "mp4", "mkv", "avi", "mov", "flv", "webm", "wmv", "3gp", "ts" -> FileCategory.VIDEO
            "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "heic" -> FileCategory.IMAGE
            "mp3", "wav", "m4a", "flac", "aac", "ogg", "wma", "opus" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv" -> FileCategory.DOCUMENT
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso" -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
