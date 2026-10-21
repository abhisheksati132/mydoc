package com.abhisheksati.mydoc.ui.shelves

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics

/**
 * First Run Empty State ("Put your files on a shelf").
 *
 * Rules:
 * - Three overlapping rotated rounded rectangles in blue, orange, mint (pure shape art).
 * - Headline "Put your files on a shelf."
 * - Body: "Pick the folder where your notes and PDFs live. MyDoc sorts them into shelves, one per course. Everything stays on your phone."
 * - Primary button "Choose a folder" and text button "Open a single file".
 */
@Composable
fun FirstRunShelvesHero(
    onChooseFolder: () -> Unit,
    onOpenSingleFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pure shape art: Three overlapping rotated rounded rectangles in blue, orange, mint
        Box(
            modifier = Modifier
                .size(160.dp, 120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Blue rect
            Box(
                modifier = Modifier
                    .size(90.dp, 70.dp)
                    .rotate(-14f)
                    .clip(RoundedCornerShape(AppSpacing.cardRadius))
                    .background(colors.getShelfColor(0).color.copy(alpha = 0.85f))
            )
            // Orange rect
            Box(
                modifier = Modifier
                    .size(85.dp, 65.dp)
                    .rotate(8f)
                    .clip(RoundedCornerShape(AppSpacing.cardRadius))
                    .background(colors.getShelfColor(1).color.copy(alpha = 0.90f))
            )
            // Mint rect
            Box(
                modifier = Modifier
                    .size(80.dp, 60.dp)
                    .rotate(-2f)
                    .clip(RoundedCornerShape(AppSpacing.cardRadius))
                    .background(colors.getShelfColor(2).color)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Put your files on a shelf.",
            style = typography.title,
            color = colors.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Pick the folder where your notes and PDFs live. MyDoc sorts them into shelves, one per course. Everything stays on your phone.",
            style = typography.bodyLarge,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Primary Button "Choose a folder" (full width, 52dp tall, 24dp radius)
        Button(
            onClick = {
                haptics.select()
                onChooseFolder()
            },
            shape = RoundedCornerShape(AppSpacing.cardRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.ink,
                contentColor = colors.background
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Choose a folder",
                style = typography.body.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Text Button "Open a single file"
        TextButton(
            onClick = {
                haptics.select()
                onOpenSingleFile()
            }
        ) {
            Text(
                text = "Open a single file",
                style = typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.inkMuted
            )
        }
    }
}
