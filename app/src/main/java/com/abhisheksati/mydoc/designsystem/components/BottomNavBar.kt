package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.HomeTab

/** Root navigation on a Material 3 navigation bar. */
@Composable
fun BottomNavBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppHaptics()
    val scheme = MaterialTheme.colorScheme

    NavigationBar(
        modifier = modifier,
        containerColor = scheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        HomeTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            val icon: ImageVector = if (isSelected) when (tab) {
                HomeTab.FILES -> Icons.Filled.Folder
                HomeTab.TOOLS -> Icons.Filled.Build
            } else when (tab) {
                HomeTab.FILES -> Icons.Outlined.Folder
                HomeTab.TOOLS -> Icons.Outlined.Build
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        haptics.tick()
                        onTabSelected(tab)
                    }
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = scheme.onSecondaryContainer,
                    selectedTextColor = scheme.onSurface,
                    indicatorColor = scheme.secondaryContainer,
                    unselectedIconColor = scheme.onSurfaceVariant,
                    unselectedTextColor = scheme.onSurfaceVariant
                )
            )
        }
    }
}
