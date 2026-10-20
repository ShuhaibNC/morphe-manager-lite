/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 *
 * Original hard forked code:
 * https://github.com/Jman-Github/Universal-ReVanced-Manager/blob/597b3173a004f5a9aae54326046dd7fd4c5b7777/app/src/main/java/app/revanced/manager/ui/theme/Type.kt
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.morphe.manager.R

/**
 * JetBrains Mono, bundled in res/font. The patch log is set in it, terminal style.
 */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

/**
 * MT Manager style type scale: one plain sans, no letter spacing, no bold
 * display sizes. A screen title is 20.sp, a list title 16.sp, and the small
 * gray second line under it 13.sp — the same three steps MT repeats everywhere.
 */
private fun mtStyle(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = mtStyle(28, 34),
    displayMedium = mtStyle(24, 30),
    displaySmall = mtStyle(22, 28),
    headlineLarge = mtStyle(20, 26),
    headlineMedium = mtStyle(20, 26),
    headlineSmall = mtStyle(18, 24),
    titleLarge = mtStyle(20, 26),
    titleMedium = mtStyle(16, 22),
    titleSmall = mtStyle(14, 20, FontWeight.Medium),
    bodyLarge = mtStyle(16, 22),
    bodyMedium = mtStyle(14, 20),
    bodySmall = mtStyle(13, 18),
    labelLarge = mtStyle(14, 20, FontWeight.Medium),
    labelMedium = mtStyle(12, 16, FontWeight.Medium),
    labelSmall = mtStyle(11, 14, FontWeight.Medium)
)
