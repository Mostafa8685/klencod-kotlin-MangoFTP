package com.mango.ftp.data.p2p

import android.util.Log
import com.google.gson.Gson
import com.mango.ftp.data.model.ChatMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class ChatSocketManager {

    companion object {
        const val TAG = "MangoChatSocket"
        const val DEFAULT_PORT = 8989
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages.asSharedFlow()

    var assignedPort: Int = DEFAULT_PORT
        private set

    fun startServer(preferredPort: Int = DEFAULT_PORT): Int {
        stopServer()
        try {
            // Bind to preferred or dynamic port
            val ss = try {
                ServerSocket(preferredPort)
            } catch (e: Exception) {
                Log.w(TAG, "Port $preferredPort busy, binding to dynamic port...")
                ServerSocket(0)
            }
            serverSocket = ss
            assignedPort = ss.localPort
            Log.d(TAG, "Chat ServerSocket listening on port: $assignedPort")

            serverJob = coroutineScope.launch {
                while (isActive && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        launch {
                            handleIncomingConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!ss.isClosed) {
                            Log.e(TAG, "Error accepting socket connection", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ServerSocket", e)
        }
        return assignedPort
    }

    private suspend fun handleIncomingConnection(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 10000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            val line = reader.readLine()
            if (!line.isNullOrBlank()) {
                val message = gson.fromJson(line, ChatMessage::class.java)
                // Mark received message as not mine
                val receivedMsg = message.copy(isMine = false)
                _incomingMessages.emit(receivedMsg)
                Log.d(TAG, "Received message from ${receivedMsg.senderName}: ${receivedMsg.messageText}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from client socket", e)
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    suspend fun sendMessage(
        targetHost: String,
        targetPort: Int,
        message: ChatMessage
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(targetHost, targetPort), 5000)
            socket.soTimeout = 5000

            val writer = PrintWriter(socket.getOutputStream(), true)
            val jsonPayload = gson.toJson(message)
            writer.println(jsonPayload)
            writer.flush()

            Log.d(TAG, "Sent message successfully to $targetHost:$targetPort")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to $targetHost:$targetPort", e)
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}
