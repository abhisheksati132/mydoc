package com.abhisheksati.mydoc.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import kotlinx.coroutines.launch

private data class IntroPage(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val body: Int
)

private val introPages = listOf(
    IntroPage(Icons.Outlined.Shield, R.string.intro_page1_title, R.string.intro_page1_body),
    IntroPage(Icons.Outlined.FolderOpen, R.string.intro_page2_title, R.string.intro_page2_body),
    IntroPage(Icons.Outlined.AllInclusive, R.string.intro_page3_title, R.string.intro_page3_body)
)

/**
 * Three cards, skippable at any point, shown once on a fresh install. It explains the two
 * promises the app is built on — on-device processing and your own folders — and where the
 * three entry points sit, without a permission wall.
 */
@Composable
fun FirstRunFlow(onFinish: () -> Unit) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val reducedMotion = LocalReducedMotion.current
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == introPages.lastIndex

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        // This window sits above the activity's, so it must claim the same edge-to-edge bars.
        val view = LocalView.current
        val dark = AppTheme.isDark
        LaunchedEffect(Unit) {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onFinish) {
                        Text(stringResource(R.string.intro_skip), color = colors.inkMuted)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { index ->
                    IntroPageContent(introPages[index])
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    introPages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (index == pagerState.currentPage) colors.accent else colors.line)
                        )
                    }
                }

                Button(
                    onClick = {
                        haptics.snap()
                        if (isLast) {
                            onFinish()
                        } else {
                            val next = pagerState.currentPage + 1
                            scope.launch {
                                if (reducedMotion) pagerState.scrollToPage(next)
                                else pagerState.animateScrollToPage(next)
                            }
                        }
                    },
                    shape = AppRadius.pillShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(stringResource(if (isLast) R.string.intro_start else R.string.intro_continue))
                }
            }
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(AppRadius.xLargeShape)
                .background(colors.accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(page.title),
            style = AppTheme.typography.screenTitle,
            color = colors.ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.body),
            style = AppTheme.typography.body,
            color = colors.inkMuted,
            textAlign = TextAlign.Center
        )
    }
}
