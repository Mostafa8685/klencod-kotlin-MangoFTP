package com.mango.ftp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mango.ftp.data.model.ChatMessage
import com.mango.ftp.data.model.PeerDevice
import com.mango.ftp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedPeer by viewModel.selectedPeer.collectAsState()
    val inputMessage by viewModel.inputMessage.collectAsState()
    val isSending by viewModel.isSending.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startP2PService()
        onDispose {
            // Keep running or stop when destroyed
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // Top Peers Bar (Family members discovered on Wi-Fi)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "الأجهزة المكتشفة في شبكة الواي فاي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MangoTextDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Count badge
                        Surface(
                            shape = CircleShape,
                            color = MangoOrange
                        ) {
                            Text(
                                text = "${discoveredPeers.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (selectedPeer != null) {
                        TextButton(onClick = { viewModel.selectPeer(null) }) {
                            Text("إرسال للجميع (Broadcast)", fontSize = 12.sp, color = MangoOrange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (discoveredPeers.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MangoOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جاري البحث التلقائي عن هواتف الأسرة في نفس شبكة الواي فاي...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MangoTextMedium
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(discoveredPeers) { peer ->
                            PeerChip(
                                peer = peer,
                                isSelected = selectedPeer?.id == peer.id,
                                onClick = {
                                    if (selectedPeer?.id == peer.id) {
                                        viewModel.selectPeer(null)
                                    } else {
                                        viewModel.selectPeer(peer)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Active chat target indicator
        Surface(
            color = MangoCardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (selectedPeer != null) Icons.Default.Person else Icons.Default.Group,
                    contentDescription = null,
                    tint = MangoOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedPeer != null) {
                        "محادثة مباشرة مع: ${selectedPeer?.displayName} (${selectedPeer?.deviceModel})"
                    } else {
                        "بث عام لجميع أجهزة الأسرة المتصلة بالواي فاي"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MangoTextDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Messages Conversation List
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("💬", fontSize = 52.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد رسائل بعد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MangoTextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "الدردشة تعمل بتقنية P2P السريعة داخل الشبكة المحلية بدون الحاجة للإنترنت مطلقاً!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MangoTextMedium
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }
                }
            }
        }

        // Bottom Input Row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { viewModel.updateInput(it) },
                    placeholder = { Text("اكتب رسالة سريعة للأسرة...") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = inputMessage.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputMessage.isNotBlank()) MangoOrange else Color.Gray)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PeerChip(
    peer: PeerDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MangoOrange else MangoCardBg,
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Online green indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = peer.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MangoTextDark
                )
                Text(
                    text = peer.deviceModel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MangoTextMedium
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isMine = message.isMine
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MangoOrange
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${message.deviceModel})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MangoTextLight
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 2.dp,
                bottomEnd = if (isMine) 2.dp else 16.dp
            ),
            color = if (isMine) MangoOrange else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isMine) Color.White.copy(alpha = 0.75f) else MangoTextLight,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
