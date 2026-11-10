package com.mango.ftp.data.ftp

import com.mango.ftp.data.model.FtpFileItem
import com.mango.ftp.data.model.FtpServerConfig
import com.mango.ftp.data.model.TransferProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class FtpManager {

    private var ftpClient: FTPClient? = null

    suspend fun connect(config: FtpServerConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val client = FTPClient().apply {
                connectTimeout = 8000
                defaultTimeout = 15000
                controlEncoding = "UTF-8"
            }

            client.connect(config.host, config.port)
            val reply = client.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect()
                return@withContext Result.failure(Exception("رفض الراوتر الاتصال: كود $reply"))
            }

            val loggedIn = if (config.isAnonymous) {
                client.login("anonymous", "anonymous@mango.ftp")
            } else {
                client.login(config.username, config.password)
            }

            if (!loggedIn) {
                client.disconnect()
                return@withContext Result.failure(Exception("فشل تسجيل الدخول: تحقق من اسم المستخدم وكلمة المرور للراوتر"))
            }

            // Enter local passive mode (Essential for Routers & NAT firewalls)
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            client.setFileTransferMode(FTP.STREAM_TRANSFER_MODE)
            client.bufferSize = 64 * 1024 // 64KB buffer for high local Wi-Fi speeds

            ftpClient = client
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(config: FtpServerConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            val testClient = FTPClient().apply {
                connectTimeout = 5000
                defaultTimeout = 5000
            }
            testClient.connect(config.host, config.port)
            if (!FTPReply.isPositiveCompletion(testClient.replyCode)) {
                testClient.disconnect()
                return@withContext Result.failure(Exception("لم يستجب منفذ FTP في الراوتر"))
            }

            val loggedIn = if (config.isAnonymous) {
                testClient.login("anonymous", "test@mango.local")
            } else {
                testClient.login(config.username, config.password)
            }

            val systemType = testClient.systemType ?: "Unknown"
            testClient.logout()
            testClient.disconnect()

            if (loggedIn) {
                Result.success("تم الاتصال بالراوتر بنجاح! نوع النظام: $systemType")
            } else {
                Result.failure(Exception("فشل تسجيل الدخول: خطأ في كلمة المرور أو اسم المستخدم"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الوصول لعنوان الراوتر (${config.host}): ${e.localizedMessage}"))
        }
    }

    suspend fun listFiles(path: String): Result<List<FtpFileItem>> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(Exception("الخادم غير متصل"))
            if (!client.isConnected) {
                return@withContext Result.failure(Exception("انقطع الاتصال بالراوتر"))
            }

            // Change directory or list directly
            val targetPath = if (path.isBlank()) "/" else path
            val rawFiles = client.listFiles(targetPath) ?: emptyArray()

            val parsedList = rawFiles
                .filter { it.name != "." && it.name != ".." }
                .map { ftpFile -> FileParser.parseFtpFile(ftpFile, targetPath) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.cleanName.lowercase() }))

            Result.success(parsedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        remotePath: String,
        destinationFile: File,
        fileName: String,
        fileSize: Long,
        onProgress: (TransferProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null
        try {
            val client = ftpClient ?: return@withContext Result.failure(Exception("الخادم غير متصل"))

            destinationFile.parentFile?.mkdirs()
            outputStream = FileOutputStream(destinationFile)
            inputStream = client.retrieveFileStream(remotePath)
                ?: return@withContext Result.failure(Exception("تعذر فتح دفق تنزيل الملف من الراوتر"))

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L
            var currentSpeed = 0L

            onProgress(
                TransferProgress(
                    fileName = fileName,
                    isUpload = false,
                    bytesTransferred = 0,
                    totalBytes = fileSize,
                    speedBytesPerSec = 0
                )
            )

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!coroutineContext.isActive) {
                    throw Exception("تم إلغاء التنزيل من قبل المستخدم")
                }
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                bytesSinceLastSample += bytesRead

                val now = System.currentTimeMillis()
                val delta = now - lastTime
                if (delta >= 500) { // update speed every 500ms
                    currentSpeed = (bytesSinceLastSample * 1000) / delta
                    lastTime = now
                    bytesSinceLastSample = 0L

                    onProgress(
                        TransferProgress(
                            fileName = fileName,
                            isUpload = false,
                            bytesTransferred = totalRead,
                            totalBytes = fileSize,
                            speedBytesPerSec = currentSpeed
                        )
                    )
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            inputStream.close()
            inputStream = null

            if (!client.completePendingCommand()) {
                return@withContext Result.failure(Exception("فشل إتمام أمر التنزيل من الراوتر"))
            }

            onProgress(
                TransferProgress(
                    fileName = fileName,
                    isUpload = false,
                    bytesTransferred = totalRead,
                    totalBytes = fileSize,
                    speedBytesPerSec = 0,
                    isCompleted = true
                )
            )

            Result.success(destinationFile)
        } catch (e: Exception) {
            outputStream?.close()
            inputStream?.close()
            destinationFile.delete()
            onProgress(
                TransferProgress(
                    fileName = fileName,
                    isUpload = false,
                    isError = true,
                    errorMessage = e.localizedMessage
                )
            )
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        sourceInputStream: InputStream,
        remoteDirectory: String,
        smartFileName: String,
        fileSize: Long,
        onProgress: (TransferProgress) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var remoteOutputStream: OutputStream? = null
        try {
            val client = ftpClient ?: return@withContext Result.failure(Exception("الخادم غير متصل"))

            val remotePath = if (remoteDirectory.endsWith("/")) {
                "$remoteDirectory$smartFileName"
            } else {
                "$remoteDirectory/$smartFileName"
            }

            remoteOutputStream = client.storeFileStream(remotePath)
                ?: return@withContext Result.failure(Exception("تعذر إنشاء ملف جديد على فلاشة الراوتر (تحقق من الصلاحيات)"))

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalWritten = 0L
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L
            var currentSpeed = 0L

            onProgress(
                TransferProgress(
                    fileName = smartFileName,
                    isUpload = true,
                    bytesTransferred = 0,
                    totalBytes = fileSize,
                    speedBytesPerSec = 0
                )
            )

            while (sourceInputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!coroutineContext.isActive) {
                    throw Exception("تم إلغاء عملية الرفع")
                }
                remoteOutputStream.write(buffer, 0, bytesRead)
                totalWritten += bytesRead
                bytesSinceLastSample += bytesRead

                val now = System.currentTimeMillis()
                val delta = now - lastTime
                if (delta >= 500) {
                    currentSpeed = (bytesSinceLastSample * 1000) / delta
                    lastTime = now
                    bytesSinceLastSample = 0L

                    onProgress(
                        TransferProgress(
                            fileName = smartFileName,
                            isUpload = true,
                            bytesTransferred = totalWritten,
                            totalBytes = fileSize,
                            speedBytesPerSec = currentSpeed
                        )
                    )
                }
            }

            remoteOutputStream.flush()
            remoteOutputStream.close()
            remoteOutputStream = null

            sourceInputStream.close()

            if (!client.completePendingCommand()) {
                return@withContext Result.failure(Exception("فشل إكمال كتابة الملف على الراوتر"))
            }

            onProgress(
                TransferProgress(
                    fileName = smartFileName,
                    isUpload = true,
                    bytesTransferred = totalWritten,
                    totalBytes = fileSize,
                    speedBytesPerSec = 0,
                    isCompleted = true
                )
            )

            Result.success(true)
        } catch (e: Exception) {
            remoteOutputStream?.close()
            sourceInputStream.close()
            onProgress(
                TransferProgress(
                    fileName = smartFileName,
                    isUpload = true,
                    isError = true,
                    errorMessage = e.localizedMessage
                )
            )
            Result.failure(e)
        }
    }

    suspend fun deleteFile(remotePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(Exception("الخادم غير متصل"))
            val deleted = client.deleteFile(remotePath)
            if (deleted) Result.success(true) else Result.failure(Exception("تعذر حذف الملف من الراوتر"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(parentPath: String, dirName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(Exception("الخادم غير متصل"))
            val target = if (parentPath.endsWith("/")) "$parentPath$dirName" else "$parentPath/$dirName"
            val created = client.makeDirectory(target)
            if (created) Result.success(true) else Result.failure(Exception("تعذر إنشاء المجلد"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            if (ftpClient?.isConnected == true) {
                ftpClient?.logout()
                ftpClient?.disconnect()
            }
        } catch (_: Exception) {}
        ftpClient = null
    }

    val isConnected: Boolean
        get() = ftpClient?.isConnected == true
}
