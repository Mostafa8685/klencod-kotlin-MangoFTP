package com.mango.ftp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mango.ftp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangoTopBar(
    title: String,
    isConnected: Boolean,
    onRefreshClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mango Icon representation
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MangoOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🥭", fontSize = 20.sp)
                }

                Column {
                    Text(
                        text = "Mango FTP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        },
        actions = {
            // Status Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isConnected) MangoLeafGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF69F0AE) else Color(0xFFFF8A80))
                    )
                    Text(
                        text = if (isConnected) "راوتر متصل" else "غير متصل",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            if (onRefreshClick != null) {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MangoOrange,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}
