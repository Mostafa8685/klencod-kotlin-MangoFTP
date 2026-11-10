package com.mango.ftp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mango.ftp.data.local.PreferencesManager
import com.mango.ftp.data.model.ChatMessage
import com.mango.ftp.data.model.PeerDevice
import com.mango.ftp.data.model.UserProfile
import com.mango.ftp.data.p2p.ChatSocketManager
import com.mango.ftp.data.p2p.NsdHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val nsdHelper: NsdHelper,
    private val socketManager: ChatSocketManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val discoveredPeers: StateFlow<List<PeerDevice>> = nsdHelper.discoveredPeers

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _selectedPeer = MutableStateFlow<PeerDevice?>(null)
    val selectedPeer: StateFlow<PeerDevice?> = _selectedPeer.asStateFlow()

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    init {
        // Start listening to incoming chat messages
        viewModelScope.launch {
            socketManager.incomingMessages.collect { msg ->
                _messages.update { current -> current + msg }
            }
        }
    }

    fun startP2PService() {
        val port = socketManager.startServer()
        viewModelScope.launch {
            userProfile.first { it.isOnboarded }.let { profile ->
                nsdHelper.startAdvertising(
                    displayName = profile.displayName,
                    deviceModel = profile.deviceModel,
                    port = port
                )
                nsdHelper.startDiscovery()
            }
        }
    }

    fun stopP2PService() {
        nsdHelper.stopAdvertising()
        nsdHelper.stopDiscovery()
        socketManager.stopServer()
    }

    fun selectPeer(peer: PeerDevice?) {
        _selectedPeer.value = peer
    }

    fun updateInput(text: String) {
        _inputMessage.value = text
    }

    fun sendMessage() {
        val text = _inputMessage.value.trim()
        if (text.isBlank()) return

        val profile = userProfile.value
        val chatMessage = ChatMessage(
            senderName = profile.displayName,
            deviceModel = profile.deviceModel,
            messageText = text,
            isMine = true
        )

        val target = _selectedPeer.value
        val peersToSend = if (target != null) listOf(target) else discoveredPeers.value

        if (peersToSend.isEmpty()) {
            // Keep locally as draft or offline notice
            _messages.update { current ->
                current + chatMessage.copy(
                    messageText = "$text (لم يتم العثور على أجهزة متصلة في الواي فاي حالياً)"
                )
            }
            _inputMessage.value = ""
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            var sentAny = false

            // Broadcast to selected peer or all active peers on network
            for (peer in peersToSend) {
                val result = socketManager.sendMessage(peer.hostAddress, peer.port, chatMessage)
                if (result.isSuccess) {
                    sentAny = true
                }
            }

            _isSending.value = false
            _inputMessage.value = ""

            // Add to my timeline
            _messages.update { current -> current + chatMessage }
        }
    }
}
