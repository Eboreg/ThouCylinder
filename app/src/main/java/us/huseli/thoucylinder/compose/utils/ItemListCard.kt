package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemListCardWithThumbnail(
    thumbnailModel: Any?,
    modifier: Modifier = Modifier,
    isSelected: () -> Boolean = { false },
    thumbnailPlaceholder: ImageVector? = null,
    thumbnailPlaceholderTint: Color? = null,
    height: Dp = 70.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Card(
        border = border,
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
                ?: if (isSelected()) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified,
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.extraSmall)
            .then(
                if (onClick != null || onLongClick != null) Modifier.combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = { onLongClick?.invoke() },
                ) else Modifier
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (thumbnailModel is Collection<*>) {
                Thumbnail4x4(
                    models = thumbnailModel.toList(),
                    placeholderIcon = thumbnailPlaceholder,
                    borderWidth = if (isSelected()) null else 1.dp,
                    placeholderIconTint = thumbnailPlaceholderTint,
                )
            } else {
                Thumbnail(
                    model = thumbnailModel,
                    placeholderIcon = thumbnailPlaceholder,
                    borderWidth = if (isSelected()) null else 1.dp,
                    placeholderIconTint = thumbnailPlaceholderTint,
                )
            }

            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemListCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    height: Dp = 70.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        border = border,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.fillMaxWidth().height(height).then(
            if (onClick != null || onLongClick != null) Modifier.combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() },
            ) else Modifier
        ),
        content = content,
    )
}
