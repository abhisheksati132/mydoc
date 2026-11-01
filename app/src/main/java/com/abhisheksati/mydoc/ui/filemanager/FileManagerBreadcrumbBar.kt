package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import java.io.File

/**
 * Header bar displaying active category label, sub-category chips (for documents),
 * or the horizontal breadcrumb navigation trail for hierarchical directory browsing.
 */
@Composable
fun FileManagerBreadcrumbBar(
    activeCategory: FileCategory?,
    displayedItemsCount: Int,
    onClearCategory: () -> Unit,
    docSubCategoryFilter: String,
    onDocSubCategoryFilterSelected: (String) -> Unit,
    breadcrumbTrail: List<File>,
    storageVolumes: List<StorageVolume>,
    onNavigateToCrumb: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (activeCategory != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Category: ${activeCategory.displayName}",
                        style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "($displayedItemsCount items)",
                        style = typography.caption.copy(fontSize = 12.sp),
                        color = colors.textSecondary
                    )
                }
                IconButton(
                    onClick = onClearCategory,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Exit Category",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Sub-category filters for documents (PDF, Word, Excel, PPT, Text)
            if (activeCategory == FileCategory.DOCUMENTS) {
                val docFilters = listOf("All", "PDF", "Word", "Excel", "PPT", "Text")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    docFilters.forEach { label ->
                        val isSelected = docSubCategoryFilter == label
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surfaceContainer,
                            animationSpec = tween(durationMillis = 200),
                            label = "fileFilterBg"
                        )
                        val chipText by animateColorAsState(
                            targetValue = if (isSelected) colors.accent else colors.textSecondary,
                            animationSpec = tween(durationMillis = 200),
                            label = "fileFilterText"
                        )

                        Surface(
                            shape = AppRadius.pillShape,
                            color = chipBg,
                            border = BorderStroke(0.5.dp, if (isSelected) colors.accent.copy(alpha = 0.35f) else colors.line),
                            modifier = Modifier
                                .clip(AppRadius.pillShape)
                                .pressScaleEffect(onClick = { onDocSubCategoryFilterSelected(label) })
                        ) {
                            Text(
                                text = label,
                                style = typography.caption.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = chipText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                breadcrumbTrail.forEachIndexed { index, crumb ->
                    val isLast = index == breadcrumbTrail.size - 1
                    val volumeForCrumb = storageVolumes.firstOrNull { it.path == crumb.absolutePath }
                    val crumbName = when {
                        volumeForCrumb != null -> volumeForCrumb.title
                        crumb.absolutePath == "/storage/emulated/0" -> "Internal Storage"
                        index == 0 -> "Storage"
                        else -> crumb.name
                    }

                    Text(
                        text = crumbName,
                        style = typography.body.copy(
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        ),
                        color = if (isLast) colors.accent else colors.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigateToCrumb(crumb) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    if (!isLast) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = colors.divider,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
