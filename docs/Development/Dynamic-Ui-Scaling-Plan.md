# Dynamic UI scaling plan (desktop / Wayland)

## Goal

On JBR/Wayland, recompute UI density when the compositor's scale changes or a window moves to a differently scaled output, without restarting Forge. Follow IntelliJ's approach: read the scale from the component being painted and key caches by scale, rather than listening for a global scale-change event.

Non-goals: replacing Swing; fixing Wayland window positioning (`Window.setLocation()` is a no-op under WLToolkit) — that is a separate JBR adaptation task covering `FFrame`, `FDialog`, and `SLayoutIO`.

## Background

- `forge-gui-desktop/src/main/java/forge/GuiDesktop.java` derives the only desktop scale from `GraphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform()`, caches it once in `ScreenScaleHolder`, and exposes it through `IGuiBase.getScreenScale()`. Roughly 11 image-draw sites multiply sizes by that value.
- On a stock JDK in a Wayland session AWT runs through XWayland (`XToolkit`): the transform is 1.0 and fractional `-Dsun.java2d.uiScale=1.25/1.5/1.75` is ignored; only integer `2` is applied. There is no Wayland toolkit in the stock JDK (Project Wakefield remains a prototype branch).
- JBR exposes the compositor scale per output through `GraphicsConfiguration.getDefaultTransform()`. In one KDE session it reported two outputs simultaneously: `1920x1200 @ 1x` and `1600x1001 @ 2x`.
- IntelliJ's implementation reads `component.graphicsConfiguration.defaultTransform.scaleX` at call time (`JBUIScale.sysScale(component/gc/g)`), tracks per-component scale in `ScaleContext`, and regenerates cache entries when the scale changes (`ScaleContextCache.getOrProvide`). See [JBUIScale.kt](https://github.com/JetBrains/intellij-community/blob/master/platform/util/ui/src/com/intellij/ui/scale/JBUIScale.kt) and [ScaleContext.kt](https://github.com/JetBrains/intellij-community/blob/master/platform/util/ui/src/com/intellij/ui/scale/ScaleContext.kt).

Prerequisite: launch with JBR and `-Dawt.toolkit.name=auto` (plus the existing Forge JVM arguments). A launch-time `-Dsun.java2d.uiScale=<factor>` overrides all monitors and is not a mixed-DPI solution.

## Design

Add one desktop-only utility, `forge-gui-desktop/src/main/java/forge/toolbox/FUiScale.java`:

```java
public final class FUiScale {
    public static float of(Component c);   // c.getGraphicsConfiguration().getDefaultTransform().getScaleX()
    public static float of(Graphics g);    // ((Graphics2D) g).getDeviceConfiguration()
    public static float fallback();        // GuiBase.getInterface().getScreenScale()
    public static void addScaleChangeListener(Runnable r);
    public static void removeScaleChangeListener(Runnable r);
}
```

Rules:

- Null, headless, or invalid transform falls back to `fallback()`; no AWT access in static initializers (`Main` sets headless before AWT loads).
- Track the last observed scale per top-level window in a `WeakHashMap<Window, Float>`. On a change, schedule one debounced `invokeLater(() -> { window.revalidate(); window.repaint(); listeners.fire(); })`. This covers scale changes that produce no resize event and the few icons built once during UI construction.
- Do not add `Component`/AWT types to `IGuiBase` in `forge-gui`; mobile and headless frontends stay untouched.

## Step 1 — switch scale resolution at call sites

| File / line | Change |
|---|---|
| `ImageView.java:1158` | `FUiScale.of(this)` |
| `AddBasicLandsDialog.java:426` | `FUiScale.of(this)` |
| `FImagePanel.java:293,336` | `FUiScale.of(this)` in `getAffineTransform()` and `setImageScale()` |
| `ScaledImagePanel.java:112` | `FUiScale.of(g)` (`paint` has `Graphics`) |
| `CardFaceSymbols.java:218,229` | `FUiScale.of(g)` in `drawSymbol`/`drawWatermark` |
| `CardPanel.java:247` | `FUiScale.of(imagePanel)` |
| `ImageCache.java:501` | Remove scale dependency: generate the placeholder at the requested `width`/`height` passed down from `scaleImage()` instead of `488 × 680 × screenScale`. Callers already compute pixel dimensions. |
| `StatTypeFilter.java:45`, `ACEditorBase.java:139-140`, `VStatistics.java:318` | Compute icon size with `FUiScale.of(component)` and register a scale-change listener that rebuilds the icon, or defer icon creation to first paint. These are the only construction-time sites. |

`GuiDesktop.getScreenScale()` stays as the headless/fallback path; optionally drop its static holder so the default-device value is also read on demand. `HeadlessStartupTest.ScreenScaleProbe` expects `screenScale=1.0` and must keep passing.

## Step 2 — make the renderer's font cache scale-aware

`FCardImageRenderer.initialize()` allocates `cachedFonts` once with `new Font[font.getSize() * round(2 * screenScale)]` (`FCardImageRenderer.java:100-105`), and `getFontBySize()` indexes that array directly (`:150-158`). A scale increase after startup can overflow it.

- Replace with `Map<Float, Map<Font, Font[]>> cachedFontsByScale`, populated via `computeIfAbsent`.
- Pass the current scale from `drawCardImage(Graphics2D g, ...)` into `getFontBySize(...)`; resolve it once per call with `FUiScale.of(g)` instead of storing mutable static state.
- Keep at most the current and previous scale entry to bound memory.

`ImageCache._CACHE` is already Guava-bounded (`maximumSize` + `expireAfterAccess`) and `FSkin.SkinImage.scaledImages` keys on pixel dimensions, so both produce new entries at a new scale; no eviction work is needed there.

## Step 3 — layout and persistence

- Keep `FControl`'s `componentResized` listener (`FControl.java:266-284`): it already calls `sizeChildren()`, `window.updateNormalBounds()` and saves the layout. Toolkit-driven resize does most of the relayout.
- `SLayoutIO` stores component bounds, which are already logical coordinates; do not add scale conversion. Drop x/y restore under Wayland since window placement is compositor-controlled.
- The `FUiScale` repaint hook is the only extra trigger needed for scale changes that do not resize a window.

## Step 4 — verification

Deterministic tests, isolated and full-suite-safe:

- `FUiScaleTest`: injectable scale provider returns `1.0`, then `1.5`; assert resolved values and that the change listener fires once after debounce.
- `FCardImageRendererTest`: draw at 1× then 2×; assert no `ArrayIndexOutOfBoundsException` and two scale entries in the font cache.
- Existing `HeadlessStartupTest` must remain green.

Manual acceptance on JBR 25 in a Wayland session:

1. Launch at 1×; open home, deck editor, item manager, a match, and a rogue dialog.
2. Change compositor scale live: 1.5× → 1.75× → 2× → 1×; then move the window between outputs with different scales.
3. Check card art and mana symbols stay sharp, no clipped text, no stale large bitmaps after scaling down, no exceptions in `forge.log`, window size remains usable.
4. Check repeated paints after a scale change reuse cached images; no per-frame regeneration.

## Risks

- JBR reported 2× for one compositor output configured at 1.75×; verify the reported transform on target desktops before relying on auto detection.
- Helpers without a component context must use the fallback; in mixed-DPI setups that can resolve the wrong monitor. Every paint path in the table has a component or `Graphics`, so this should be limited to the fallback only.
- `FCardImageRenderer` is static state; keep scale as a parameter, not a field, to avoid cross-thread contamination.

Estimate: roughly 3–5 days of work plus the manual scale matrix; no native code and no changes outside `forge-gui-desktop`.
