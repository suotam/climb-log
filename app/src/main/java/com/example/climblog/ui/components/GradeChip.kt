package com.example.climblog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.climblog.domain.model.AscentStyle

@Composable
fun GradeChip(
    grade: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = grade,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun AscentStyleChip(
    style: AscentStyle,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = ascentStyleColors(style)
    Text(
        text = style.label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun ascentStyleColors(style: AscentStyle): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (style) {
        AscentStyle.ONSIGHT  -> cs.tertiary to cs.onTertiary
        AscentStyle.FLASH    -> cs.tertiaryContainer to cs.onTertiaryContainer
        AscentStyle.REDPOINT -> cs.primary to cs.onPrimary
        AscentStyle.TOPROPE  -> cs.secondary to cs.onSecondary
        AscentStyle.ATTEMPT  -> cs.surfaceVariant to cs.onSurfaceVariant
        AscentStyle.PROJECT  -> cs.surfaceVariant to cs.onSurfaceVariant
    }
}
