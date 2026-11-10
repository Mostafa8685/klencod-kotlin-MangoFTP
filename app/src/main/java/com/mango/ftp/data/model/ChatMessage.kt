package com.mango.ftp.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val deviceModel: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMine: Boolean = false
)
