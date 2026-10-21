# MT Manager UI style

This fork follows the UI style of MT Manager, learned from screenshots of
MT Manager 2.x (file list, drawer, overflow menu, Preferences, Extract APK).
The rule in one line: **everything is a simple flat list.**

Defaults for this fork: color style **Monochrome**, background animation
**None**, launcher icon **Slate** (`dark_5`, enabled by the icon generator
instead of the first catalog entry).

## Palette (dark, the default look)

| Role | Value |
| --- | --- |
| Background / surface | `#2B2B2B` |
| Panel / surface variant | `#383838` |
| Container ladder | `#232323` / `#2B2B2B` / `#323232` / `#383838` / `#404040` |
| Primary text | `#F2F2F2` |
| Secondary text | `#A3A3A3` |
| Accent (toggles, progress, section headers) | `#2196F3` |
| Outline | `#666666`, variant `#404040` |

Light theme is the same idea in light gray: `#F5F5F5` background, `#212121`
text, `#616161` secondary text, `#1976D2` accent.

## Rules

- No gradients, no glass/translucency, no animated backgrounds, no shimmer
  sweeps, no press-scale animations, no text shadows.
- No per-app colors. Apps and sources never tint cards, chips or icons;
  everything draws on the theme surfaces.
- No Material You dynamic color and no custom accent/theme-color washes.
  One flat palette for everyone.
- Corners are almost square: 4 dp for cards, rows, chips and buttons,
  8 dp maximum for sheets and dialogs.
- Elevation is 0 everywhere. Panels separate by one flat step of the
  container ladder, or a 1 dp outline when the outlines setting is on.
- Type is one plain sans, no letter spacing: screen title 20 sp, list
  title 16 sp, second line 13-14 sp in the secondary color. Nothing bold
  except where the component already was.
- App lists (home, installed-apps picker) copy MT's Extract APK page:
  paneled rows one step lighter than the page (`#383838` on `#2B2B2B`),
  8 dp corners, about 10 dp between rows, 56 dp icon on the left, then
  three lines - app name (20 sp), version and size, package name - the
  last two in the secondary color.
- Settings look like MT's Preferences: no group panels at all. A small
  blue section header (`Startup`, `Appearance` style), then flat rows
  with a title and a smaller gray description under it sitting directly
  on the page background, blue switch on the right.
- Menus are flat dark panels with an icon and a label per row.
- The patching screen is only progress + log: no mini-games, no tab bar,
  no memory / CPU / storage-IO usage displays. The log is a Linux
  terminal - JetBrains Mono (bundled, `res/font`), green text on pure
  black; warnings amber, errors red. The run-info card (version, APK
  size, patches, sources) starts collapsed and expands on tap.

## Where it is enforced

- `ui/theme/Color.kt`, `ui/theme/Theme.kt` (`MtShapes`, `withMtContainers`),
  `ui/theme/Type.kt` - palette, shapes, type scale.
- `ui/theme/ThemeTraits.kt` - trait adapters always return the flat neutral
  values (the old monochrome behavior) for every theme.
- `ui/screen/shared/SettingComponents.kt` (`Defaults`) - card radius 4 dp,
  elevation 0, blue `SectionTitle`.
- `ui/screen/shared/AnimatedBackground.kt` - draws nothing.
- `ui/screen/shared/PressFeedback.kt` - `pressScale` is a no-op.
- `ui/screen/shared/ShimmerEffects.kt` - loading blocks are static.
- `ui/screen/home/HomeAppCards.kt` - home rows: MT app-list panels, 88 dp,
  56 dp icon, name / version + size / package.
- `ui/screen/shared/CompactListCard.kt` + `CardHeadingText.kt` - the same
  panel rows for the pickers (installed apps, files, patches).
- `ui/screen/shared/SettingComponents.kt` (`SettingsGroup`) - settings
  groups render flat on the page, no panel.
- `domain/manager/PreferencesManager.kt` - defaults: Monochrome style,
  no background animation. `app/build.gradle.kts` +
  `GenerateLauncherIconsTask` - Slate is the default launcher icon.

## Round 4 (Shuhaib, 2026-10-21)

- Defaults: "Optimize for device architecture" (strip unused native libs) ON,
  "Greeting Phrases" OFF. Both apply where the preference was never changed.
- Settings no longer offers an accent color or a background animation: the
  AccentColorSelector, BackgroundSettingsItem and BackgroundPickerDialog are
  deleted with their settings entries. The Appearance "Colors" section keeps
  only the app-card-colors entry, where that setting applies.
- The patching log canvas spans 100% of the screen width: portrait headers
  keep the normal inset while the panel runs edge to edge; in landscape the
  panel runs to the right edge. The terminal surface is square-cornered with
  no border, like a real terminal window.
