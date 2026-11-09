package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.HomeTab
import dev.chrisbanes.haze.HazeState

/** A bounded glass dock; equal tab widths leave room for larger text and translations. */
@Composable
fun BottomNavBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    motion: GlassMotionState? = null,
    onScanClick: () -> Unit = {}
) {
    val haptics = rememberAppHaptics()
    val colors = AppTheme.colors
    val selectedTone = when (currentTab) {
        HomeTab.HOME -> colors.accent
        HomeTab.LIBRARY -> colors.accentCyan
        HomeTab.TOOLS -> colors.accentGreen
    }
    val indicatorColor by animateColorAsState(
        targetValue = selectedTone.copy(alpha = if (AppTheme.isDark) 0.24f else 0.17f),
        animationSpec = if (LocalReducedMotion.current) snap() else AppMotion.snappy(),
        label = "navigationTint"
    )
    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = RoundedCornerShape(36.dp),
            hazeState = hazeState,
            motion = motion,
            elevation = 8.dp,
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(6.dp)) {
                val tabWidth = (maxWidth - 8.dp) / 3
                val selectedIndex = listOf(HomeTab.HOME, HomeTab.LIBRARY, HomeTab.TOOLS).indexOf(currentTab).coerceAtLeast(0)
                val offset by animateDpAsState(
                    (tabWidth + 4.dp) * selectedIndex,
                    animationSpec = if (LocalReducedMotion.current) snap() else AppMotion.snappy(),
                    label = "navigationIndicator"
                )
                Row(Modifier.matchParentSize()) {
                    Box(Modifier.offset { IntOffset(offset.roundToPx(), 0) }.width(tabWidth).fillMaxHeight()
                        .background(indicatorColor, RoundedCornerShape(28.dp)))
                }
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(HomeTab.HOME, HomeTab.LIBRARY, HomeTab.TOOLS)
                val labels = listOf(R.string.nav_home, R.string.nav_library, R.string.nav_tools)
                val filled = listOf(Icons.Filled.AutoStories, Icons.Filled.FolderCopy, Icons.Filled.Widgets)
                val outlined = listOf(Icons.Outlined.AutoStories, Icons.Outlined.FolderCopy, Icons.Outlined.Widgets)
                tabs.forEachIndexed { index, tab ->
                    NavPillItem(
                        selected = currentTab == tab,
                        label = stringResource(labels[index]),
                        selectedIcon = filled[index],
                        unselectedIcon = outlined[index],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (currentTab != tab) {
                                haptics.tick()
                                onTabSelected(tab)
                            }
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val foreground by animateColorAsState(
        if (selected) AppTheme.colors.ink else AppTheme.colors.inkMuted,
        animationSpec = if (reducedMotion) snap() else AppMotion.snappy(), label = "dockInk"
    )
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier.heightIn(min = 60.dp).clip(shape)
            .then(if (focused) Modifier.border(2.dp, scheme.primary, shape) else Modifier)
            .selectable(selected = selected, role = Role.Tab,
                interactionSource = interactionSource, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Icon(if (selected) selectedIcon else unselectedIcon,
            contentDescription = null, tint = foreground, modifier = Modifier.size(23.dp))
        Text(label, style = AppTheme.typography.captionStrong,
            color = foreground, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
