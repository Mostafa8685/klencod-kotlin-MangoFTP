package com.mango.ftp.data.model

data class FtpServerConfig(
    val host: String = "192.168.1.1",
    val port: Int = 21,
    val username: String = "anonymous",
    val password: String = "",
    val isAnonymous: Boolean = false,
    val defaultPath: String = "/"
)
