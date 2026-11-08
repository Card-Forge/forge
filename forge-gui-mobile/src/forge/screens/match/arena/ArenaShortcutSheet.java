package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.toolbox.FOptionPane;

/**
 * The list of the match keyboard shortcuts ({@link ArenaShortcuts}) in the Arena dialog style, opened by H, from the
 * corner menu and from the game menu's options: the keys of each action in gold-rimmed key caps, what it does beside
 * them, in two columns over the dimmed board. Esc, H, Enter, Done or a click away from the list closes it. Outside the
 * Arena look (or off the match screen) it is a plain message, as before.
 */
public final class ArenaShortcutSheet extends ArenaDialogOverlay {
    private static final Color CAP_TOP = ArenaTheme.rgb(0x2A2C33), CAP_BOTTOM = ArenaTheme.rgb(0x0C0D11);
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color();

    private static ArenaShortcutSheet shown;

    /** One action: the key caps of each of its keys (a cap per key of a chord), and what it does. */
    private static final class Row {
        final List<String[]> keys = new ArrayList<>();
        final String what;

        Row(final String what) {
            this.what = what;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final ArenaDialogKit.Pill btnDone = new ArenaDialogKit.Pill();
    private float top, rowH, colX0, colX1, colW, capH;
    private int perColumn;
    private FSkinFont capFont, textFont;

    /** The sheet's text: "Space / Enter - Pass priority / OK", one action per line, in {@link ArenaShortcuts#all()} order. */
    public static String text() {
        final StringBuilder sb = new StringBuilder();
        ArenaShortcuts.Action last = null;
        for (final ArenaShortcuts.Shortcut s : ArenaShortcuts.all()) {
            if (s.action == last) {
                continue; // keyLabels lists every key of the action on its first line
            }
            last = s.action;
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(ArenaShortcuts.keyLabels(s.action)).append("  -  ").append(s.description());
        }
        if (hasFullscreenKeys()) {
            sb.append('\n').append("F11 / Alt+Enter  -  ").append(fullscreenCaption());
        }
        return sb.toString();
    }

    /** Desktop: F11 and Alt+Enter switch fullscreen anywhere in Forge (see Forge's key handling). */
    private static boolean hasFullscreenKeys() {
        return Forge.getDeviceAdapter() != null && Forge.getDeviceAdapter().hasWindowControls();
    }

    private static String fullscreenCaption() {
        return Forge.getLocalizer().getMessageorUseDefault("lblArenaShortcutFullscreen", "Fullscreen on / off");
    }

    /** Shows the sheet over the match, or takes it away if it is up (H both opens and closes it). Render thread. */
    public static void open() {
        if (shown != null && !shown.isClosed()) {
            shown.close();
            return;
        }
        final String title = Forge.getLocalizer().getMessageorUseDefault("lblArenaGdxShortcuts", "Keyboard shortcuts");
        if (!ArenaTheme.isMatchBoard()) {
            FOptionPane.showMessageDialog(text(), title);
            return;
        }
        shown = new ArenaShortcutSheet(title);
        shown.openOverlay();
    }

    private ArenaShortcutSheet(final String title) {
        heading.set(title, Forge.getLocalizer().getMessageorUseDefault("lblArenaShortcutsHint", "Keys for the match. H shows this list again."), title);
        setOpenMillis(260);
        ArenaShortcuts.Action last = null;
        for (final ArenaShortcuts.Shortcut s : ArenaShortcuts.all()) {
            if (s.action == last) {
                continue;
            }
            last = s.action;
            final Row row = new Row(s.description());
            for (final ArenaShortcuts.Shortcut k : ArenaShortcuts.keysFor(s.action)) {
                final List<String> caps = new ArrayList<>(3);
                if (k.ctrl) {
                    caps.add("Ctrl");
                }
                if (k.shift) {
                    caps.add("Shift");
                }
                final String label = k.keyLabel();
                caps.add(label.substring(label.lastIndexOf('+') + 1));
                row.keys.add(caps.toArray(new String[0]));
            }
            rows.add(row);
        }
        if (hasFullscreenKeys()) { //desktop: the window's own keys work in the match too
            final Row row = new Row(fullscreenCaption());
            row.keys.add(new String[] { "F11" });
            row.keys.add(new String[] { "Alt", "Enter" });
            rows.add(row);
        }
        btnDone.primary = true;
        btnDone.text = Forge.getLocalizer().getMessage("lblArenaDone");
        btnDone.action = this::close;
        addPill(btnDone);
    }

    @Override
    protected void onClosed() {
        if (shown == this) {
            shown = null;
        }
    }

    @Override
    protected void layoutDialog(final float w, final float h) {
        ArenaDialogKit.placeButtons(w, h, btnDone);
        top = heading.bottom(w, h) + Math.round(h * 0.04f);
        final float bottom = ArenaDialogKit.buttonY(h) - Math.round(h * 0.04f);
        perColumn = (rows.size() + 1) / 2;
        rowH = Math.min(Math.round(h * 0.06f), (bottom - top) / Math.max(1, perColumn));
        capH = Math.round(rowH * 0.66f);
        capFont = ArenaDialogKit.font(capH * 0.52f);
        textFont = ArenaDialogKit.font(Math.min(rowH * 0.46f, h * 0.026f));
        colW = Math.min(Math.round(w * 0.36f), Math.round(h * 0.9f));
        final float gap = Math.round(w * 0.04f);
        colX0 = Math.round(w / 2f - gap / 2f - colW);
        colX1 = Math.round(w / 2f + gap / 2f);
    }

    @Override
    protected void drawScene(final Graphics g, final float w, final float h, final float alpha) {
        heading.draw(g, w, h, alpha);
    }

    @Override
    protected void drawContent(final Graphics g, final float w, final float h, final float alpha) {
        final float keysW = colW * 0.42f;
        for (int i = 0; i < rows.size(); i++) {
            final Row row = rows.get(i);
            final int col = i < perColumn ? 0 : 1;
            final float x = col == 0 ? colX0 : colX1;
            final float y = top + (i % Math.max(1, perColumn)) * rowH;
            final float a = alpha * (entrance.isOver() ? 1f : entrance.inTurn(i, rows.size()));
            if (a <= 0f) {
                continue;
            }
            // a faint band under every other row keeps the two sides of a line together
            if (i % 2 == 0) {
                g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.WHITE, 0.035f * a, S1), x - rowH * 0.2f, y, colW + rowH * 0.4f, rowH, rowH * 0.3f);
            }
            // the keys, right-aligned against the words
            float kx = x + keysW;
            final float cy = y + rowH / 2f;
            for (int k = row.keys.size() - 1; k >= 0; k--) {
                final String[] caps = row.keys.get(k);
                for (int c = caps.length - 1; c >= 0; c--) {
                    final float cw = capWidth(caps[c]);
                    kx -= cw;
                    keyCap(g, caps[c], kx, cy - capH / 2f, cw, a);
                    if (c > 0) {
                        final float pw = ArenaArt.textWidth("+", capFont) + capH * 0.3f;
                        kx -= pw;
                        ArenaArt.text(g, "+", capFont, ArenaTheme.alpha(ArenaTheme.TEXT_DIM, a, S2), kx, cy - capH / 2f, pw, capH, Align.center);
                    }
                }
                if (k > 0) {
                    final float sw = ArenaArt.textWidth("/", capFont) + capH * 0.5f;
                    kx -= sw;
                    ArenaArt.text(g, "/", capFont, ArenaTheme.alpha(ArenaTheme.TEXT_DIM, a, S2), kx, cy - capH / 2f, sw, capH, Align.center);
                }
            }
            final float tx = x + keysW + rowH * 0.45f;
            ArenaDialogKit.edged(g, row.what, textFont, ArenaTheme.alpha(ArenaTheme.WHITE, a, S3), tx, y, x + colW - tx, rowH, Align.left);
        }
    }

    private float capWidth(final String label) {
        return Math.max(capH * 1.15f, ArenaArt.textWidth(label, capFont) + capH * 0.7f);
    }

    /** A key cap: a dark rounded key with a gold rim and its label. */
    private void keyCap(final Graphics g, final String label, final float x, final float y, final float w, final float a) {
        final float r = capH * 0.22f;
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.5f * a, S1), x, y + capH * 0.08f, w, capH, r, capH * 0.12f);
        g.fillSmoothRoundRectGradient(ArenaTheme.alpha(CAP_TOP, a, S1), ArenaTheme.alpha(CAP_BOTTOM, a, S2), true, x, y, w, capH, r);
        g.drawArenaShape(Graphics.SHAPE_RING, x, y, w, capH, r, Math.max(1.2f, ArenaTheme.px(1.6f)), ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, a, S1),
                ArenaTheme.alpha(ArenaTheme.GOLD_DIM, a, S2), true);
        ArenaArt.textBold(g, label, capFont, ArenaTheme.alpha(ArenaTheme.TEXT, a, S3), x, y, w, capH, Align.center);
    }

    @Override
    public boolean keyDown(final int keyCode) {
        if (keyCode == Keys.H && !viewingBattlefield) {
            close();
            return true;
        }
        return super.keyDown(keyCode);
    }

    @Override
    protected boolean passesThrough(final int keyCode) {
        return false; // nothing on the board answers while the list of what answers is up
    }

    @Override
    protected void onTap(final float x, final float y, final int count) {
        close(); // a click anywhere but Done (which closes it too) takes the list away
    }

    @Override
    protected void onSubmitKey() {
        close();
    }

    @Override
    protected void onEscapeKey() {
        close();
    }
}
