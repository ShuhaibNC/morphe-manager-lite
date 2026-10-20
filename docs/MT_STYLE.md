# MT Manager UI style

This fork follows the UI style of MT Manager, learned from screenshots of
MT Manager 2.x (file list, drawer, overflow menu, Preferences). The rule in
one line: **everything is a simple flat list.**

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
- Lists are tight rows starting at the top of the screen: icon slot on the
  left (48 dp in a flat dark square), title, small gray second line, and
  2-4 dp between rows. No floating cards, no centered content blocks.
- Settings look like MT's Preferences: a small blue section header
  (`Startup`, `Appearance` style), then flat rows with a title and a
  smaller gray description under it, blue switch on the right.
- Menus are flat dark panels with an icon and a label per row.

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
- `ui/screen/home/HomeAppCards.kt` - home rows: flat, 72 dp, 48 dp icon.
