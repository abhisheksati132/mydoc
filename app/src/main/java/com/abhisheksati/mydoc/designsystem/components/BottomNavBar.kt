package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.HomeTab
import dev.chrisbanes.haze.HazeState

/**
 * Floating glass pill navigation for the three root tabs: Home, Library, Tools.
 *
 * Part of the "Ink & Seal" chrome:
 * - Backdrop blur via [GlassSurface] and [HazeState]
 * - 1dp specular rim highlight + soft inner shadow
 * - Seal teal marks the active tab; the pill itself carries the selection
 * - High-performance solid fallback when glass effects are disabled
 *
 * Each item is a real tab: selectable semantics for TalkBack and a 48dp touch
 * target, which the visual pill alone would not provide.
 */
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

    // Glass thickens on touch: a damped spring kicks the blur up on tab change
    // and relaxes back, so the bar responds to interaction, not just scroll.
    val reducedMotion = LocalReducedMotion.current
    LaunchedEffect(currentTab, motion, reducedMotion) {
        val target = motion ?: return@LaunchedEffect
        if (reducedMotion) {
            target.interaction = 0f
            return@LaunchedEffect
        }
        target.interaction = 1f
        val decay = Animatable(1f)
        decay.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 300f)) {
            target.interaction = value
        }
        target.interaction = 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = AppRadius.glassNavShape,
            hazeState = hazeState,
            motion = motion,
            elevation = 10.dp,
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Home Tab
                NavPillItem(
                    selected = currentTab == HomeTab.HOME,
                    label = "Home",
                    selectedIcon = Icons.Filled.AutoStories,
                    unselectedIcon = Icons.Outlined.AutoStories,
                    onClick = {
                        if (currentTab != HomeTab.HOME) {
                            haptics.tick()
                            onTabSelected(HomeTab.HOME)
                        }
                    }
                )

                // Library Tab
                NavPillItem(
                    selected = currentTab == HomeTab.LIBRARY,
                    label = "Library",
                    selectedIcon = Icons.Filled.FolderCopy,
                    unselectedIcon = Icons.Outlined.FolderCopy,
                    onClick = {
                        if (currentTab != HomeTab.LIBRARY) {
                            haptics.tick()
                            onTabSelected(HomeTab.LIBRARY)
                        }
                    }
                )

                // Tools Tab
                NavPillItem(
                    selected = currentTab == HomeTab.TOOLS,
                    label = "Tools",
                    selectedIcon = Icons.Filled.Widgets,
                    unselectedIcon = Icons.Outlined.Widgets,
                    onClick = {
                        if (currentTab != HomeTab.TOOLS) {
                            haptics.tick()
                            onTabSelected(HomeTab.TOOLS)
                        }
                    }
                )
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
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.accentContainer else Color.Transparent,
        animationSpec = spring(stiffness = 500f),
        label = "pillContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.inkSecondary,
        animationSpec = spring(stiffness = 500f),
        label = "pillContent"
    )

    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            style = if (selected) AppTheme.typography.captionStrong else AppTheme.typography.caption,
            color = contentColor
        )
    }
}
