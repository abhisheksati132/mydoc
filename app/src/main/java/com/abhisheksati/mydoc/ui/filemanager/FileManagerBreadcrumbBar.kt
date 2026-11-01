package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.DocumentFilterChips
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import java.io.File

/**
 * Header bar displaying the item count for an active category, the document
 * format filters, or the breadcrumb trail for hierarchical directory browsing.
 */
@Composable
fun FileManagerBreadcrumbBar(
    activeCategory: FileCategory?,
    displayedItemsCount: Int,
    onClearCategory: () -> Unit,
    documentFilter: DocumentFilter,
    onDocumentFilterSelected: (DocumentFilter) -> Unit,
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
                    .padding(start = AppSpacing.medium, end = AppSpacing.xSmall)
            ) {
                Text(
                    text = if (displayedItemsCount == 1) "1 ITEM" else "$displayedItemsCount ITEMS",
                    style = typography.overline.withTabularFigures(),
                    color = colors.textSecondary
                )
                IconButton(
                    onClick = onClearCategory,
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Exit category",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (activeCategory == FileCategory.DOCUMENTS) {
                DocumentFilterChips(
                    selected = documentFilter,
                    onSelected = onDocumentFilterSelected,
                    modifier = Modifier.padding(
                        start = AppSpacing.medium,
                        end = AppSpacing.medium,
                        bottom = AppSpacing.xSmall
                    )
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.xSmall)
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
                        style = if (isLast) typography.bodyLargeStrong else typography.body,
                        color = if (isLast) colors.ink else colors.inkMuted,
                        modifier = Modifier
                            .clip(AppRadius.microShape)
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
