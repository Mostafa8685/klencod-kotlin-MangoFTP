package com.mango.ftp.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mango.ftp.ui.navigation.Screen
import com.mango.ftp.ui.theme.*

@Composable
fun MangoBottomNav(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val items = listOf(Screen.Files, Screen.Chat, Screen.Settings)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { screen ->
            val isSelected = currentScreen.route == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.titleArabic,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.titleArabic,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MangoOrange,
                    selectedTextColor = MangoOrange,
                    indicatorColor = MangoCardBg,
                    unselectedIconColor = MangoTextMedium,
                    unselectedTextColor = MangoTextMedium
                )
            )
        }
    }
}
