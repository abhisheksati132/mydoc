package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

/**
 * Static tonal placeholder for first paint. Deliberately shimmer-free: a
 * pulsing skeleton is itself motion, which reduced-motion users asked us
 * not to show.
 */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    Box(modifier.background(AppTheme.colors.surfaceMuted, AppRadius.cardShape))
}

@Composable
fun HomeFeedSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(180.dp))
        repeat(3) {
            SkeletonBlock(Modifier.fillMaxWidth().height(72.dp))
        }
    }
}

@Composable
fun BrowseSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(200.dp))
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(3) {
                    SkeletonBlock(Modifier.size(96.dp))
                }
            }
        }
        repeat(2) {
            SkeletonBlock(Modifier.fillMaxWidth().height(64.dp))
        }
    }
}
