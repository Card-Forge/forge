/**
 * The Arena look of the libGDX match screen: a port of the Swing desktop client's Arena theme
 * ({@code forge-gui-desktop .../toolbox/Arena*.java}, {@code .../screens/match/Arena*.java}) onto the GPU.
 *
 * <h2>What is here</h2>
 * <ul>
 * <li>{@link forge.screens.match.arena.ArenaTheme}: on/off ({@code isActive()}, {@code isMatchBoard()}), palette,
 *     {@code px()} size scaling from Swing pixels, fonts, colour helpers (tile tints, frame colours...).</li>
 * <li>{@link forge.screens.match.arena.ArenaMotion}: the frame clock, eases, {@code Tween}, {@code Fade},
 *     {@code Spring}, {@code pulse()}, {@code requestFrame()}, timing constants.</li>
 * <li>{@link forge.screens.match.arena.ArenaArt}: shared drawing primitives (panels, frames, buttons, pills,
 *     badges, dividers, glow, shadow, text, board backdrop, portraits), on top of the new {@code forge.Graphics}
 *     shape/glow/tilt/fade helpers.</li>
 * <li>{@link forge.screens.match.arena.ArenaSkin}: the hook a view draws through.</li>
 * <li>{@link forge.screens.match.arena.ArenaCurtain}: fades over screen changes and over things that go away.</li>
 * <li>{@link forge.screens.match.arena.ArenaShortcuts}: the match keyboard shortcuts, as data.</li>
 * </ul>
 *
 * <h2>The one pattern every view uses</h2>
 * Each restyled view keeps a static skin and asks it first; the default look below is left exactly as it was:
 * <pre>{@code
 * // forge/screens/match/views/VAvatar.java (the view: a two-line change)
 * private static final ArenaSkin<VAvatar> ARENA = new ArenaAvatarSkin();
 *
 * public void draw(Graphics g) {
 *     if (ArenaSkin.draw(ARENA, g, this)) { return; }
 *     ... default look, untouched ...
 * }
 *
 * // forge/screens/match/arena/ArenaAvatarSkin.java (the skin: all the Arena drawing)
 * final class ArenaAvatarSkin implements ArenaSkin<VAvatar> {
 *     public boolean draw(Graphics g, VAvatar v) {
 *         float d = Math.min(v.getWidth(), v.getHeight());
 *         ArenaArt.circleImage(g, v.getAvatarRegion(), v.getWidth() / 2, v.getHeight() / 2, d * 0.9f);
 *         ArenaArt.portraitRing(g, v.getWidth() / 2, v.getHeight() / 2, d * 0.9f, ArenaTheme.GOLD, 0.6f);
 *         return true;
 *     }
 * }
 * }</pre>
 * FContainers hook {@code drawBackground}/{@code drawOverlay}/{@code doLayout} the same way
 * ({@code ArenaSkin.drawBackground(ARENA, g, this)} ...). Widgets shared with the menus (toolbox dialogs, option
 * panes, lists) use the {@code ...OnMatch} variants so the menus keep the default look. Skins live in this package,
 * one file per view, named {@code Arena<View>Skin}; add package-private or public getters to the view for the state
 * the skin needs rather than duplicating it. No central registry: nothing to merge.
 *
 * <h2>Coordinates and sizes</h2>
 * {@code forge.Graphics} is y-down: (0, 0) is the top left of the component being drawn ({@code g.getBounds()} holds
 * its place on the screen), units are logical pixels (the window's size from {@code Forge.getScreenWidth/Height()}),
 * and the framebuffer may be larger (HiDPI): the shape shaders antialias in framebuffer pixels by themselves.
 * Port Swing pixel constants through {@code ArenaTheme.px(v)} (1:1 at 1440 logical px high) and fonts through
 * {@code ArenaTheme.font(swingPointSize)}; keep Swing's fractions-of-the-view layout as it is.
 *
 * <h2>Animation</h2>
 * Use {@code ArenaMotion.Fade} / {@code Tween} / {@code Spring} (ticked once per frame, before drawing); read their
 * values in {@code draw()}. While they move the clock keeps frames coming. Anything computed from the time in
 * {@code draw()} must call {@code ArenaMotion.requestFrame()} each frame it is not at rest (or use {@code pulse()}),
 * because the client renders only on demand. Never use threads, Swing timers or {@code Thread.sleep} for animation.
 *
 * <h2>Performance</h2>
 * Target: 120 fps at 3440x1440. Shapes are batched (see the notes above {@code Graphics.drawArenaShape}): a run of
 * shapes and plain rects is one draw call until text, an image or another shader interrupts it, so draw layers in
 * passes (all glows, then all cards, then all text) where you can. {@code drawTiltedImage} costs a draw call per card:
 * only for the hovered/tilted few. Do not render things into textures per card; the GPU draws them each frame.
 */
package forge.screens.match.arena;
