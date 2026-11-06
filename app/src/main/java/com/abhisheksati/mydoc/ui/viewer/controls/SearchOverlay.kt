package com.abhisheksati.mydoc.ui.viewer.controls

import com.abhisheksati.mydoc.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@Composable
fun SearchOverlay(
    visible: Boolean,
    query: String,
    matchCount: Int,
    currentMatchIndex: Int,
    onQueryChanged: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
        modifier = modifier
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChanged,
                        placeholder = {
                            Text(
                                stringResource(R.string.find_in_document),
                                style = AppTheme.typography.body,
                                color = colors.inkMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = colors.inkSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { if (matchCount > 0) onNextMatch() }),
                        shape = AppRadius.roundShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.inkPrimary,
                            unfocusedTextColor = colors.inkPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.inkMuted.copy(alpha = 0.3f),
                            cursorColor = colors.accent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (matchCount > 0) {
                        Text(
                            text = "${currentMatchIndex + 1} of $matchCount",
                            style = AppTheme.typography.metadataStrong,
                            color = colors.accent
                        )
                    } else if (query.isNotBlank()) {
                        Text(
                            text = "0 matches",
                            style = AppTheme.typography.metadata,
                            color = colors.inkMuted
                        )
                    }

                    IconButton(onClick = onPreviousMatch, enabled = matchCount > 0) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.cd_prev_match),
                            tint = if (matchCount > 0) colors.inkPrimary else colors.inkMuted.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(onClick = onNextMatch, enabled = matchCount > 0) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_next_match),
                            tint = if (matchCount > 0) colors.inkPrimary else colors.inkMuted.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(onClick = onCloseSearch) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.cd_close_search),
                            tint = colors.inkPrimary
                        )
                    }
                }
            }
        }
    }
}
