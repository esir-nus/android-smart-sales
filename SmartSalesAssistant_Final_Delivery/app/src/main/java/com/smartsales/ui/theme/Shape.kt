package com.smartsales.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Smart Sales Assistant - Shapes
 *
 * Material 3 shape scale
 */

val Shapes =
    Shapes(
        // Extra Small - Chips, small buttons
        extraSmall = RoundedCornerShape(4.dp),
        // Small - Cards, dialogs
        small = RoundedCornerShape(8.dp),
        // Medium - Cards, buttons
        medium = RoundedCornerShape(12.dp),
        // Large - Sheets, large cards
        large = RoundedCornerShape(16.dp),
        // Extra Large - Full screen dialogs
        extraLarge = RoundedCornerShape(28.dp),
    )

// Custom shapes for specific components
val MessageBubbleShape =
    RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 4.dp,
        bottomEnd = 16.dp,
    )

val MessageBubbleShapeReverse =
    RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 4.dp,
    )

val CardShape = RoundedCornerShape(12.dp)
val DialogShape = RoundedCornerShape(24.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
