/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.ui.screen.home

import android.content.pm.PackageInfo
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.morphe.manager.R
import app.morphe.manager.data.room.apps.installed.InstalledApp
import app.morphe.manager.ui.model.HomeAppItem
import app.morphe.manager.ui.screen.shared.*
import app.morphe.manager.ui.screen.shared.Animations
import app.morphe.manager.ui.theme.LocalAppCardColorResolver
import app.morphe.manager.ui.theme.LocalThemeTraits
import app.morphe.manager.ui.theme.ThemeTraitsDefaults
import app.morphe.manager.util.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// A verdict answered from cache lands within a frame, so the badge waits rather than flashing
private const val INSTALL_VERIFICATION_BADGE_DELAY_MS = 400L

// Apps whose version strings carry a build stamp run far past this, and printing one in a badge
// would leave no room for the version the card already shows, so those are badged by word instead
private const val MAX_BADGE_VERSION_LENGTH = 10

private data class HomeAppCardStyle(
    val monochrome: Boolean,
    val outlines: Boolean,
    val colorResolver: AppCardColorResolver?,
    val iconSize: Dp,
    val titleColor: Color,
    val subtitleColor: Color,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val chipContainerColor: Color,
    val chipContentColor: Color,
    val cardColor: Color,
    val cardRadius: Dp = 4.dp,
    val cardHeight: Dp = 72.dp,
    val contentPadding: Dp = 12.dp,
    val contentSpacing: Dp = 12.dp
) {
    /**
     * Applies the card colors chosen in the appearance settings to [bundleColors], the palette
     * declared by this card's own bundle. Stops bound to the bundle are derived from it, so the
     * result stays per-app even when the rest of the gradient is fixed.
     */
    fun cardColors(bundleColors: List<Color>): List<Color> =
        colorResolver?.resolve(bundleColors) ?: bundleColors

    /**
     * This style adjusted to the gradient it will be drawn on. White reads on the colors a bundle
     * declares for itself, but the appearance settings let a card be any color, including one
     * light enough to swallow it.
     */
    fun onCard(bundleColors: List<Color>): HomeAppCardStyle = this
}

@Composable
private fun homeAppCardStyle(subtitleAlpha: Float = 0.75f): HomeAppCardStyle {
    // MT style row: a plain list item on the flat background. White/gray theme
    // text, no shadows, no per-app colors, icon in a simple square slot.
    return HomeAppCardStyle(
        monochrome = LocalThemeTraits.current.monochrome,
        outlines = LocalThemeTraits.current.outlines,
        colorResolver = null,
        iconSize = 48.dp,
        titleColor = MaterialTheme.colorScheme.onSurface,
        subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = subtitleAlpha),
        titleStyle = MaterialTheme.typography.titleMedium,
        subtitleStyle = MaterialTheme.typography.bodySmall,
        chipContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        chipContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cardColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * Shared icon + text content for [AppCardLayout] rows.
 *
 * @param packageName    Package name used for icon lookup when [packageInfo] is null;
 *   null renders the glass placeholder without resolving an icon.
 * @param packageInfo    Resolved [PackageInfo]; when non-null [packageName] is ignored for the icon.
 * @param displayName    Primary label shown in bold.
 * @param subtitle       Secondary line shown below [displayName]; null → not rendered.
 * @param gradientColors Gradient palette forwarded to [AppIcon] placeholder, unless the user
 *   picked fixed card colors in the appearance settings.
 */
@Composable
internal fun RowScope.AppCardContent(
    packageName: String?,
    packageInfo: PackageInfo?,
    displayName: String,
    subtitle: String?,
    gradientColors: List<Color>
) {
    val cardStyle = homeAppCardStyle().onCard(gradientColors)

    AppIcon(
        packageInfo = packageInfo,
        packageName = if (packageInfo == null) packageName else null,
        contentDescription = null,
        modifier = Modifier.size(cardStyle.iconSize),
        preferredSource = AppDataSource.PATCHED_APK,
        placeholderGradientColors = cardStyle.cardColors(gradientColors)
    )

    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = displayName,
            style = cardStyle.titleStyle,
            color = cardStyle.titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (subtitle != null) {
            Text(
                // Matches the badge-height subtitle row of the installed cards, so names line up
                // across every card in the list
                modifier = Modifier
                    .height(statusBadgeHeight)
                    .wrapContentHeight(Alignment.CenterVertically),
                text = subtitle,
                style = cardStyle.subtitleStyle,
                color = cardStyle.subtitleColor,
                // The row is one badge tall, so a subtitle that wraps would be cut in half
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * One home screen card for [item]: the build Morphe keeps a record of, or the not-patched-yet
 * button when there is no record to show.
 *
 * @param showStatusBadges Whether the badges pinned to the end of the card may show.
 */
@Composable
internal fun HomeAppCard(
    item: HomeAppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showStatusBadges: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    val installedApp = item.installedApp
    if (installedApp != null) {
        InstalledAppCard(
            item = item,
            installedApp = installedApp,
            showStatusBadges = showStatusBadges,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    } else {
        NotPatchedAppCard(
            item = item,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    }
}

/**
 * Installed app card with gradient background.
 *
 * [installedApp] is [HomeAppItem.installedApp] handed over separately, so the card is written
 * against a record that is there rather than around one that might not be.
 */
@Composable
private fun InstalledAppCard(
    item: HomeAppItem,
    installedApp: InstalledApp,
    showStatusBadges: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cardStyle = homeAppCardStyle(subtitleAlpha = 0.85f).onCard(item.gradientColors)

    val versionLabel = stringResource(R.string.version)
    val cloneLabel = stringResource(R.string.clone)
    val installedLabel = stringResource(R.string.installed)
    val updateLabel = stringResource(R.string.update)
    val updateAvailableLabel = stringResource(R.string.update_available)
    val supportedVersionLabel = stringResource(R.string.home_app_info_newest_supported_version)
    val deletedLabel = stringResource(R.string.uninstalled)
    val replacementLabel = stringResource(R.string.home_unpatched_version_installed)
    val replacementBadgeLabel = stringResource(R.string.home_unpatched_badge)
    val unverifiedLabel = stringResource(R.string.home_unverified)
    val pendingLabel = stringResource(R.string.home_install_verification_pending)

    val showsPendingBadge = remember { mutableStateOf(false) }
    LaunchedEffect(item.isInstallStatePending) {
        if (!item.isInstallStatePending) {
            showsPendingBadge.value = false
            return@LaunchedEffect
        }
        delay(INSTALL_VERIFICATION_BADGE_DELAY_MS.milliseconds)
        showsPendingBadge.value = true
    }

    val version = remember(item) { item.version.withVersionPrefix() }
    // What tells the app from a namesake leads the row, ahead of the version
    val subtitle = remember(item) {
        listOfNotNull(item.nameSuffix, version.ifEmpty { null }).joinToString(" • ")
    }

    // The version worth badging, out of the one the sources support: only when it is short enough
    // to leave the row its width, with the long build-stamped kind left to the app's dialog, which
    // can print both versions in full
    val supportedVersionBadge = remember(item) {
        item.versionStatus
            ?.takeIf { item.showsVersionBadge && it.supportedVersion.length <= MAX_BADGE_VERSION_LENGTH }
            ?.supportedVersion
            ?.withVersionPrefix()
    }

    // The states the badges stand for are read out whether the badges themselves are showing or
    // not, so reordering does not quietly drop them from the card's description
    val contentDesc = remember(
        item,
        version,
        versionLabel,
        cloneLabel,
        installedLabel,
        updateAvailableLabel,
        supportedVersionLabel,
        deletedLabel,
        replacementLabel,
        unverifiedLabel,
        showsPendingBadge.value,
        pendingLabel
    ) {
        buildString {
            append(item.displayName)
            item.nameSuffix?.let { append(", $it") }
            if (item.isClone) append(", $cloneLabel")
            if (version.isNotEmpty()) {
                append(", $versionLabel $version")
            }
            append(", ")
            append(
                when {
                    showsPendingBadge.value -> pendingLabel
                    item.isInstallStateNotPatched -> replacementLabel
                    item.isDeleted -> deletedLabel
                    item.isInstallStateUnknown -> unverifiedLabel
                    else -> installedLabel
                }
            )
            if (item.showsUpdateBadge) append(", $updateAvailableLabel")
            // Read out in full even when the badge had no room to print it
            item.versionStatus?.takeIf { item.showsVersionBadge }?.let {
                append(", $supportedVersionLabel ${it.supportedVersion}")
            }
        }
    }

    AppCardLayout(
        gradientColors = item.gradientColors,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.semantics {
            role = Role.Button
            this.contentDescription = contentDesc
        }
    ) {
        // App icon
        AppIcon(
            packageInfo = item.packageInfo,
            // Named after the package this record was installed under: a clone carries an icon of
            // its own, which is regularly the whole point of keeping several copies of an app apart
            packageName = installedApp.currentPackageName,
            contentDescription = null,
            modifier = Modifier.size(cardStyle.iconSize),
            preferredSource = AppDataSource.INSTALLED,
            // A record can outlive every artifact carrying its icon, and the glass placeholder is
            // what the rest of the list shows in that case
            placeholderGradientColors = cardStyle.cardColors(item.gradientColors)
        )

        // App info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // App name
            Text(
                text = item.displayName,
                style = cardStyle.titleStyle,
                color = cardStyle.titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Version + deleted status + update chip, both pinned to the card edge
            Row(
                // Badge height is reserved whether a badge is showing, otherwise the row
                // grows around it and nudges the app name above out of place
                modifier = Modifier.height(statusBadgeHeight),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Says what the card is rather than how its install is doing, so it leads the
                // row and stays put while the state badges at the end come and go. Wordless
                // because the badges it shares the row with need the width for their own labels
                if (item.isClone) {
                    StatusBadge(
                        text = null,
                        icon = Icons.Outlined.ContentCopy,
                        containerColor = cardStyle.chipContainerColor,
                        contentColor = cardStyle.chipContentColor
                    )
                }

                // Fills the row so the chip is pinned to the card edge rather than trailing
                // a version string of whatever length
                Text(
                    modifier = Modifier.weight(1f),
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = cardStyle.subtitleStyle,
                    color = cardStyle.subtitleColor
                )

                // The drag handle rides over this edge of the card while reordering, so the
                // badges pinned here stand down rather than crowd it
                if (showStatusBadges) {
                    // Flat chip colors from the theme, like every other row.
                    if (item.isDeleted && !item.isInstallStateNotPatched) {
                        StatusBadge(
                            text = deletedLabel,
                            icon = Icons.Outlined.DeleteOutline,
                            containerColor = cardStyle.chipContainerColor,
                            contentColor = cardStyle.chipContentColor
                        )
                    }

                    if (item.isInstallStateNotPatched) {
                        StatusBadge(
                            text = replacementBadgeLabel,
                            icon = Icons.Outlined.AutoFixHigh,
                            containerColor = cardStyle.chipContainerColor,
                            contentColor = cardStyle.chipContentColor
                        )
                    }

                    if (item.isInstallStateUnknown) {
                        StatusBadge(
                            text = unverifiedLabel,
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            containerColor = cardStyle.chipContainerColor,
                            contentColor = cardStyle.chipContentColor
                        )
                    }

                    if (showsPendingBadge.value) {
                        StatusBadge(
                            text = pendingLabel,
                            icon = Icons.Outlined.HourglassEmpty,
                            containerColor = cardStyle.chipContainerColor,
                            contentColor = cardStyle.chipContentColor
                        )
                    }

                    // Newer patches and a newer supported app version are both answered by
                    // rebuilding the app, so one badge stands for either rather than two of
                    // them stacking into a row that already carries the installed version
                    AnimatedVisibility(
                        visible = item.showsRebuildBadge,
                        enter = Animations.expandHorizFadeIn,
                        exit = Animations.shrinkHorizFadeOut
                    ) {
                        StatusBadge(
                            text = supportedVersionBadge ?: updateLabel,
                            icon = Icons.Outlined.ArrowUpward,
                            containerColor = cardStyle.chipContainerColor,
                            contentColor = cardStyle.chipContentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card for an app Morphe has no build of yet.
 */
@Composable
private fun NotPatchedAppCard(
    item: HomeAppItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val notPatchedText = stringResource(R.string.home_not_patched_yet)

    // Only for an app the device actually has: other cards are described by an APK Morphe kept
    // rather than by an install, and that version answers a different question
    val subtitle = remember(item, notPatchedText) {
        val version = item.version.takeIf { item.isInstalledOnDevice && it.isNotEmpty() }
        listOfNotNull(item.nameSuffix, version?.withVersionPrefix(), notPatchedText).joinToString(" • ")
    }

    val contentDesc = remember(item.displayName, subtitle) {
        "${item.displayName}, $subtitle"
    }

    AppCardLayout(
        gradientColors = item.gradientColors,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.semantics {
            role = Role.Button
            this.contentDescription = contentDesc
        }
    ) {
        AppCardContent(
            packageName = item.packageName,
            packageInfo = item.packageInfo,
            displayName = item.displayName,
            subtitle = subtitle,
            gradientColors = item.gradientColors,
        )
    }
}

/**
 * Shared content layout for app rows and buttons.
 *
 * MT style: a flat row on the theme surface, 4.dp corners, no gradient,
 * no border sweep, no press animation.
 */
@Composable
internal fun AppCardLayout(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val cardStyle = homeAppCardStyle().onCard(gradientColors)
    val shape = RoundedCornerShape(cardStyle.cardRadius)
    val view = LocalView.current

    // Long-pressing a card always means the same thing here, so the gesture is announced
    // rather than left as an unlabeled action the screen reader cannot describe
    val longClickLabel = stringResource(R.string.accessibility_select_app)
        .takeIf { onLongClick != null }

    // Press scale animation removed: MT rows do not move, they just click.
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardStyle.cardHeight)
            .clip(shape)
            .background(cardStyle.cardColor, shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                },
                onLongClickLabel = longClickLabel,
                onLongClick = if (onLongClick != null) {
                    {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongClick()
                    }
                } else null
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = cardStyle.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(cardStyle.contentSpacing),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Shimmer loading animation for app cards.
 */
@Composable
fun AppLoadingCard(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    // MT style: a static flat skeleton row, no pulse and no shimmer sweep.
    val cardStyle = homeAppCardStyle().onCard(gradientColors)
    val shape = RoundedCornerShape(cardStyle.cardRadius)
    val titleRowHeight = with(LocalDensity.current) { cardStyle.titleStyle.lineHeight.toDp() }
    val skeletonColor = cardStyle.titleColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardStyle.cardHeight)
            .clip(shape)
            .background(cardStyle.cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = cardStyle.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(cardStyle.contentSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(
                modifier = Modifier
                    .size(cardStyle.iconSize)
                    .wrapContentSize()
                    .fillMaxSize(AppIconContentFraction),
                shape = AppIconShape,
                baseColor = skeletonColor.copy(alpha = 0.2f)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShimmerLine(
                    height = titleRowHeight,
                    widthFraction = 0.6f,
                    baseColor = skeletonColor.copy(alpha = 0.25f)
                )
                ShimmerLine(
                    height = statusBadgeHeight,
                    widthFraction = 0.4f,
                    baseColor = skeletonColor.copy(alpha = 0.15f)
                )
            }
        }
    }
}
