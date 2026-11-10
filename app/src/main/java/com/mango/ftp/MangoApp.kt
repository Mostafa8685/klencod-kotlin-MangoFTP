package com.mango.ftp

import android.app.Application
import com.mango.ftp.data.ftp.FtpManager
import com.mango.ftp.data.local.PreferencesManager
import com.mango.ftp.data.p2p.ChatSocketManager
import com.mango.ftp.data.p2p.NsdHelper

class MangoApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var ftpManager: FtpManager
        private set

    lateinit var nsdHelper: NsdHelper
        private set

    lateinit var chatSocketManager: ChatSocketManager
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        ftpManager = FtpManager()
        nsdHelper = NsdHelper(this)
        chatSocketManager = ChatSocketManager()
    }
}
