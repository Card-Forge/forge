package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.localinstance.properties.ForgePreferences;
import forge.menu.FDropDown;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.toolbox.FDisplayObject;

/**
 * Two small round buttons in the top right corner of the Arena board (port of the Swing {@code ArenaCornerMenu}).
 * The board shows no menu bar of its own: the gear opens the match tools (the game menu, players, dev mode, the log,
 * the stack, the keyboard shortcuts) and the scroll opens the log. Dark discs with a thin white rim, brighter while
 * the pointer is over them.
 */
public final class ArenaCornerMenu extends FDisplayObject {
    private final MatchScreen screen;
    private final GearMenu gearMenu = new GearMenu();
    private final Rectangle anchor = new Rectangle();
    private final ArenaMotion.Fade[] hot = {new ArenaMotion.Fade(0f), new ArenaMotion.Fade(0f)};
    private static final Color S1 = new Color(), S2 = new Color();

    public ArenaCornerMenu(final MatchScreen screen0) {
        screen = screen0;
    }

    /** Which button is at this local x: 0 the gear, 1 the log. */
    private int buttonAt(final float x) {
        return x < getHeight() + ArenaMatchLayout.cornerGap() / 2 ? 0 : 1;
    }

    @Override
    public boolean tap(final float x, final float y, final int count) {
        if (buttonAt(x) == 0) {
            toggleGear();
        } else {
            toggleLog();
        }
        return true;
    }

    @Override
    public boolean press(final float x, final float y) {
        return true;
    }

    /** Opens (or closes) the match tools under the gear. M does the same. */
    public void toggleGear() {
        if (gearMenu.isVisible()) {
            gearMenu.hide();
            return;
        }
        closeMenus();
        gearMenu.show(this, getWidth(), getHeight() + ArenaMatchLayout.cornerGap());
    }

    public boolean isGearOpen() {
        return gearMenu.isVisible();
    }

    public void closeGear() {
        gearMenu.hide();
    }

    /** Shows or hides the log in its dock. L does the same. */
    public void toggleLog() {
        final FDropDown log = screen.getLog();
        if (log.isVisible()) {
            ArenaCurtain.fadeOut(log, ArenaMotion.CLOSE_MS);
            log.hide();
        } else {
            log.show();
        }
    }

    /** Shows or hides the stack in its dock. S does the same. */
    public void toggleStack() {
        final FDropDown stack = screen.getStack();
        if (stack.isVisible()) {
            ArenaCurtain.fadeOut(stack, ArenaMotion.CLOSE_MS);
            stack.hide();
        } else {
            stack.show();
        }
    }

    /** Opens one of the match's dropdowns (game menu, players, dev menu) hanging from the gear. */
    private void openDropDown(final FDropDown dropDown) {
        if (dropDown == null) {
            return;
        }
        closeMenus();
        if (dropDown.getMenuTab() != null) {
            // the menu bar is hidden on the Arena board, so the tab is only an anchor: put it under the gear
            final Rectangle r = dropDown.getMenuTab().screenPos;
            r.set(screenPos.x + screenPos.width - r.width, screenPos.y + screenPos.height - r.height + ArenaMatchLayout.cornerGap(), r.width, r.height);
            if (r.width <= 0) {
                r.set(screenPos.x, screenPos.y, screenPos.width, screenPos.height + ArenaMatchLayout.cornerGap());
            }
        }
        dropDown.show();
    }

    /** Closes the gear menu and any of the dropdowns it opens. Returns whether anything was open. */
    public boolean closeMenus() {
        boolean any = false;
        if (gearMenu.isVisible()) {
            gearMenu.hide();
            any = true;
        }
        for (final FDropDown d : new FDropDown[] {screen.getGameMenu(), screen.getPlayersMenu(), screen.getDevMenu(), screen.getChat(), screen.getRevealed()}) {
            if (d != null && d.isVisible()) {
                d.hide();
                any = true;
            }
        }
        return any;
    }

    /** The gear's match tools. */
    private final class GearMenu extends FPopupMenu {
        @Override
        protected void buildMenu() {
            addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblGame"), FSkinImage.SETTINGS, e -> openDropDown(screen.getGameMenu())));
            addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblPlayers") + " (" + MatchScreen.getPlayerPanels().size() + ")",
                    FSkinImage.HDCHOICE, e -> openDropDown(screen.getPlayersMenu())));
            addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblLog") + "  [" + ArenaShortcuts.keyLabels(ArenaShortcuts.Action.OPEN_LOG) + "]",
                    FSkinImage.HDLIBRARY, e -> toggleLog()));
            addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblStack") + "  [" + ArenaShortcuts.keyLabels(ArenaShortcuts.Action.OPEN_STACK) + "]",
                    FSkinImage.HDEXILE, e -> toggleStack()));
            if (!MatchController.instance.getRevealedCards().isEmpty()) { //what has been revealed this game (the menu bar's reveal tab)
                addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRevealed") + " (" + MatchController.instance.getRevealedCards().size() + ")",
                        FSkinImage.HDLIBRARY, e -> openDropDown(screen.getRevealed())));
            }
            if (screen.getChat() != null) {
                addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblChat"), FSkinImage.HDLIBRARY, e -> openDropDown(screen.getChat())));
            }
            if (ForgePreferences.DEV_MODE && !MatchController.instance.isReplay()) {
                addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblDev"), FSkinImage.SETTINGS, e -> openDropDown(screen.getDevMenu())));
            }
            addItem(new FMenuItem(Forge.getLocalizer().getMessageorUseDefault("lblArenaGdxShortcuts", "Keyboard shortcuts")
                    + "  [" + ArenaShortcuts.keyLabels(ArenaShortcuts.Action.SHOW_SHORTCUTS) + "]", FSkinImage.HDCHOICE, e -> ArenaShortcutSheet.open()));
        }
    }

    @Override
    public void draw(final Graphics g) {
        final float s = getHeight(), gap = ArenaMatchLayout.cornerGap();
        final float mx = Forge.mouseMovedX - screenPos.x, my = Forge.mouseMovedY - screenPos.y;
        final boolean over = Forge.magnify && mx >= 0 && my >= 0 && mx < getWidth() && my < getHeight();
        for (int i = 0; i < 2; i++) {
            final boolean lit = (over && buttonAt(mx) == i) || (i == 0 ? gearMenu.isVisible() : screen.getLog().isVisible());
            final ArenaMotion.Fade f = hot[i];
            if (f.goal() != (lit ? 1f : 0f)) {
                f.to(lit ? 1f : 0f, ArenaMotion.HOVER_MS);
            }
            final float t = f.get();
            final float x = i * (s + gap);
            final float r = s / 2f;
            g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.45f, S1), x, ArenaTheme.px(1), s, s, r, ArenaTheme.px(4));
            g.fillSmoothCircle(ArenaTheme.rgbaInto(6, 8, 14, ArenaMotion.lerp(170, 235, t), S1), x + r, r, r);
            g.drawSmoothCircle(Math.max(1f, ArenaTheme.px(1.2f)), ArenaTheme.alpha(ArenaTheme.WHITE, ArenaMotion.lerp(90, 200, t) / 255f, S1), x + r, r, r - 0.5f);
            final Color icon = ArenaTheme.alpha(ArenaTheme.WHITE, ArenaMotion.lerp(150, 255, t) / 255f, S2);
            final float size = s * 0.62f;
            if (i == 0) {
                gear(g, x + r, r, size, icon);
            } else {
                logIcon(g, x + r, r, size, icon);
            }
        }
    }

    /** A gear: a toothed wheel with a hole (Swing paintGearIcon). */
    static void gear(final Graphics g, final float cx, final float cy, final float size, final Color c) {
        final float body = size * 0.33f, tooth = size * 0.11f;
        for (int k = 0; k < 8; k++) {
            final double a = Math.PI / 4 * k;
            final float tx = cx + (float) Math.cos(a) * (body + tooth * 0.55f), ty = cy + (float) Math.sin(a) * (body + tooth * 0.55f);
            g.fillSmoothRoundRect(c, tx - tooth, ty - tooth, 2 * tooth, 2 * tooth, tooth * 0.45f);
        }
        g.fillSmoothCircle(c, cx, cy, body);
        g.fillSmoothCircle(ArenaTheme.rgbaInto(6, 8, 14, 235, S1), cx, cy, body * 0.42f);
    }

    /** A scroll: a sheet with lines of writing and a rolled top (Swing paintLogIcon). */
    static void logIcon(final Graphics g, final float cx, final float cy, final float size, final Color c) {
        final float w = size * 0.66f, h = size * 0.82f, x = cx - w / 2, y = cy - h / 2;
        final float line = Math.max(1f, size * 0.085f);
        g.drawSmoothRoundRect(line, c, x, y, w, h, size * 0.1f);
        for (int k = 0; k < 3; k++) {
            final float ly = y + h * (0.3f + 0.2f * k);
            g.fillSmoothRoundRect(c, x + w * 0.22f, ly, w * (k == 2 ? 0.38f : 0.56f), line, line / 2);
        }
    }

    /** Where the corner menu's dropdowns hang from (the gear, on screen). */
    public Rectangle getAnchor() {
        return anchor.set(screenPos);
    }
}
