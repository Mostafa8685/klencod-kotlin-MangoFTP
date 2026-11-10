package com.mango.ftp.data.model

data class PeerDevice(
    val serviceName: String,
    val displayName: String,
    val deviceModel: String,
    val hostAddress: String,
    val port: Int,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    val id: String
        get() = "$hostAddress:$port"
}
