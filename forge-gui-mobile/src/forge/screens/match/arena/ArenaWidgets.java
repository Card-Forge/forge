package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPrompt;
import forge.toolbox.ArenaMenus;
import forge.toolbox.FButton;

/**
 * The Arena look of the toolbox widgets the match shares with the menus (dialog sheets, buttons, drop-down menus and
 * their items, list rows, text fields, check boxes, label buttons). On the match screen it is the safety net under the
 * bespoke Arena overlays, so that whatever classic dialog a match still opens (yield settings, auto-yields, a draw
 * offer, sideboarding between games, an input box, a very long list, a reconnect notice, the dev tools, the game and
 * player menus) is dressed in navy glass, gold and Arena buttons rather than the default skin; off it, the same pieces
 * dress the menus (home, new game lobbies, deck manager and editor, settings), with {@link ArenaMenus} and
 * {@link forge.toolbox.Frost}'s Arena palette for the rest. Each widget asks one method here first and keeps its own look when it says no.
 */
public final class ArenaWidgets {
    private ArenaWidgets() { }

    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color();
    private static final Color SHEET_TOP = ArenaTheme.rgba(16, 26, 46, 246), SHEET_BOTTOM = ArenaTheme.rgba(7, 11, 22, 250);
    private static final Color BAND = ArenaTheme.rgba(8, 13, 25, 250);
    private static final Color MENU_TOP = ArenaTheme.rgba(18, 28, 48, 248), MENU_BOTTOM = ArenaTheme.rgba(8, 12, 22, 250);
    private static final Color HOVER_WASH = ArenaTheme.rgba(216, 178, 106, 40), PRESS_WASH = ArenaTheme.rgba(216, 178, 106, 90);
    private static final Color SEPARATOR = ArenaTheme.rgba(216, 178, 106, 45);
    private static final Color FIELD = ArenaTheme.rgba(4, 7, 14, 235);
    private static final Color ZEBRA = ArenaTheme.rgba(27, 42, 68, 150), ZEBRA_ALT = ArenaTheme.rgba(15, 26, 46, 150);
    private static final Color SELECTED_ROW = ArenaTheme.rgba(140, 109, 59, 210);

    /** Skin colours for widgets that draw with {@link FSkinColor}s (made once, never changed). */
    public static final FSkinColor TEXT = FSkinColor.getStandardColor(ArenaTheme.TEXT);
    public static final FSkinColor TEXT_DIM = FSkinColor.getStandardColor(ArenaTheme.TEXT_DIM);
    public static final FSkinColor FIELD_BACK = FSkinColor.getStandardColor(FIELD);
    public static final FSkinColor ROW = FSkinColor.getStandardColor(ZEBRA);
    public static final FSkinColor ROW_ALT = FSkinColor.getStandardColor(ZEBRA_ALT);
    public static final FSkinColor ROW_SELECTED = FSkinColor.getStandardColor(SELECTED_ROW);
    public static final FSkinColor ROW_PRESSED = FSkinColor.getStandardColor(ArenaTheme.rgba(216, 178, 106, 120));
    public static final FSkinColor GOLD = FSkinColor.getStandardColor(ArenaTheme.GOLD);
    public static final FSkinColor GOLD_LINE = FSkinColor.getStandardColor(ArenaTheme.rgba(216, 178, 106, 110));

    /** Whether the shared widgets wear the Arena look now: on the Arena match board, and in the desktop menus. */
    public static boolean on() {
        return ArenaTheme.isMatchBoard() || ArenaMenus.on();
    }

    // ---- dialog sheets (FDialog) ------------------------------------------------------------------------------------

    /**
     * Under a dialog sheet: the board dimmed as the Arena dialogs dim it, and the sheet itself (from {@code sheetY},
     * {@code sheetH} tall, the button band below it) in navy glass with a gold line along its top. False without the Arena look.
     */
    public static boolean dialogBackground(final Graphics g, final float w, final float h, final float sheetY, final float sheetH,
            final float reveal) {
        if (!on()) {
            return false;
        }
        if (reveal <= 0f) {
            return true;
        }
        ArenaDialogKit.backdrop(g, w, h, Math.min(1f, reveal * 1.4f));
        g.fillGradientRect(SHEET_TOP, SHEET_BOTTOM, true, 0, sheetY, w, sheetH);
        ArenaArt.divider(g, 0, sheetY, w, ArenaTheme.alpha(ArenaTheme.GOLD, 0.85f, S1), Math.max(1f, ArenaTheme.px(2)));
        return true;
    }

    /**
     * Over a dialog sheet: its grip (a slim bar with a gold notch the sheet can be swiped down by), the line over the
     * button band and the title over the middle button if there is one. False without the Arena look.
     */
    public static boolean dialogOverlay(final Graphics g, final float w, final float gripY, final float gripH, final float bandY,
            final String title, final float titleH, final FSkinFont titleFont) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(ArenaTheme.alpha(ArenaTheme.NAVY_LIGHT, 0.97f, S1), ArenaTheme.alpha(ArenaTheme.NAVY, 0.97f, S2), true, 0, gripY, w, gripH);
        final float notchW = Math.max(gripH * 3f, w * 0.06f), notchH = Math.max(2f, gripH * 0.28f);
        g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.GOLD, 0.8f, S1), (w - notchW) / 2f, gripY + (gripH - notchH) / 2f, notchW, notchH, notchH / 2f);
        ArenaArt.divider(g, 0, gripY, w, ArenaTheme.alpha(ArenaTheme.GOLD, 0.6f, S1), 1f);
        ArenaArt.divider(g, 0, gripY + gripH, w, ArenaTheme.alpha(ArenaTheme.GOLD, 0.6f, S1), 1f);
        if (bandY < 0) {
            return true; // the sheet is swiped away: only its grip shows
        }
        float y = bandY;
        if (title != null && !title.isEmpty() && titleH > 0) {
            y -= titleH;
            g.fillRect(BAND, 0, y, w, titleH);
            ArenaArt.textShadowed(g, title, titleFont, ArenaTheme.GOLD_BRIGHT, 0, y, w, titleH, Align.center);
        }
        ArenaArt.divider(g, 0, y, w, ArenaTheme.alpha(ArenaTheme.GOLD, 0.7f, S1), 1f);
        return true;
    }

    /**
     * The Arena card of an older dialog on a desktop's match screen (FDialog: yield settings, a list to choose from, an
     * order list): the board dimmed, then a navy glass card centred over it with a gold rim and a gold line under its
     * heading ({@code headH} tall, 0 for none). {@code reveal} 0..1 fades it in.
     */
    public static void dialogCard(final Graphics g, final float w, final float h, final float x, final float y, final float cw,
            final float ch, final float headH, final float reveal) {
        if (reveal <= 0f) {
            return;
        }
        ArenaDialogKit.backdrop(g, w, h, Math.min(1f, reveal * 1.4f));
        final float r = Math.max(4f, ArenaTheme.px(CARD_RADIUS));
        g.fillSmoothRoundRectGradient(SHEET_TOP, SHEET_BOTTOM, true, x, y, cw, ch, r);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.5f)), ArenaTheme.alpha(ArenaTheme.GOLD, 0.8f, S1), x, y, cw, ch, r);
        if (headH > 0) {
            ArenaArt.divider(g, x, y + headH, cw, ArenaTheme.alpha(ArenaTheme.GOLD, 0.7f, S1), 1f);
        }
    }

    /** The heading of an Arena dialog card: its title in the Arena voice, centred in the band at the card's top. */
    public static void dialogHeading(final Graphics g, final String title, final float x, final float y, final float cw, final float headH,
            final float reveal) {
        if (title == null || title.isEmpty()) {
            return;
        }
        final String line = title.replace('\n', ' ');
        final FSkinFont font = ArenaDialogKit.fitFont(line, headH * 0.5f, headH * 0.3f, cw * 0.9f);
        ArenaArt.textShadowed(g, line, font, ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, reveal, S2), x, y, cw, headH, Align.center);
    }

    /** The corner of an Arena dialog card, in Swing pixels. */
    private static final float CARD_RADIUS = 12f;

    /** The button band of a dialog (its VPrompt): dark glass. False without the Arena look. */
    public static boolean dialogButtonBand(final Graphics g, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(BAND, ArenaTheme.alpha(ArenaTheme.NIGHT, 1f, S1), true, 0, 0, w, h);
        return true;
    }

    // ---- buttons (FButton) ------------------------------------------------------------------------------------------

    /**
     * An FButton as an Arena pill (the molten one if it is the dialog's main answer). The board's own prompt buttons are
     * left to the board's prompt look. False without the Arena look (or for those), then the button draws itself.
     */
    public static boolean button(final Graphics g, final FButton b, final boolean pressed, final boolean primary) {
        if (!on() || isBoardPromptButton(b)) {
            return false;
        }
        final float w = b.getWidth(), h = b.getHeight();
        final String text = b.getText();
        if ((text == null || text.isEmpty()) && !b.isEnabled()) {
            return true; // an answer slot the dialog does not use: nothing to show
        }
        final float inset = Math.min(w, h) * 0.06f;
        if (h > w * 1.15f) {
            // an upright button (a list's move arrows): a rounded key rather than a pill on its end
            final float x = inset, y = inset, bw = w - 2 * inset, bh = h - 2 * inset, r = Math.min(bw, bh) * 0.32f;
            final boolean hot = b.isHovered() && b.isEnabled();
            final Color top = !b.isEnabled() ? ArenaTheme.BTN_OFF_TOP : pressed ? ArenaTheme.BTN_BOTTOM : hot ? ArenaTheme.BTN_HOVER_TOP : ArenaTheme.BTN_TOP;
            final Color bottom = !b.isEnabled() ? ArenaTheme.BTN_OFF_BOTTOM : pressed ? ArenaTheme.BLACK : hot ? ArenaTheme.BTN_HOVER_BOTTOM : ArenaTheme.BTN_BOTTOM;
            g.fillSmoothRoundRectGradient(top, bottom, true, x, y, bw, bh, r);
            g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.5f)), !b.isEnabled() ? ArenaTheme.BTN_OFF_RIM : hot ? ArenaTheme.GOLD_BRIGHT : ArenaTheme.GOLD,
                    x, y, bw, bh, r);
            if (text != null && !text.isEmpty()) {
                final FSkinFont f = ArenaDialogKit.font(Math.min(bw * 0.5f, ArenaTheme.px(30)));
                ArenaArt.textBold(g, text, f, b.isEnabled() ? ArenaTheme.TEXT : ArenaTheme.TEXT_DIM, x, y + (pressed ? 1 : 0), bw, bh, Align.center);
            }
            return true;
        }
        ArenaDialogKit.pill(g, text, inset, inset, w - 2 * inset, h - 2 * inset, primary && b.isEnabled(), b.isHovered(), pressed,
                b.isToggled(), b.isEnabled(), 1f);
        return true;
    }

    /** Whether this is one of the match screen's own prompt buttons (OK / Cancel under the board). */
    private static boolean isBoardPromptButton(final FButton b) {
        final MatchScreen view = MatchController.getView();
        if (view == null) {
            return false;
        }
        for (final VPlayerPanel panel : view.getPlayerPanelsList()) {
            final VPrompt prompt = view.getPrompt(panel.getPlayer());
            if (prompt != null && (prompt.getBtnOk() == b || prompt.getBtnCancel() == b)) {
                return true;
            }
        }
        return false;
    }

    // ---- drop-downs and menus (FDropDown, FMenuItem) ----------------------------------------------------------------

    /** Under a drop-down: navy glass. False without the Arena look. */
    public static boolean dropDownBackground(final Graphics g, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(MENU_TOP, MENU_BOTTOM, true, 0, 0, w, h);
        return true;
    }

    /** Round a drop-down: a thin gold rim and a soft shadow line under it. False without the Arena look. */
    public static boolean dropDownOverlay(final Graphics g, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.drawRect(Math.max(1f, ArenaTheme.px(1.5f)), ArenaTheme.alpha(ArenaTheme.GOLD, 0.75f, S1), 0, 0, w, h);
        return true;
    }

    /** One menu item: a gold wash under the pointer or while pressed, its icon, its words, a faint gold separator. */
    public static boolean menuItem(final Graphics g, final float w, final float h, final boolean hovered, final boolean pressed,
            final forge.assets.FImage icon, final float iconX, final float iconSize, final boolean allowForIcon, final String text,
            final FSkinFont font, final boolean enabled, final float gap) {
        if (!on()) {
            return false;
        }
        if (pressed) {
            g.fillRect(PRESS_WASH, 0, 0, w, h);
        } else if (hovered && enabled) {
            g.fillGradientRect(HOVER_WASH, ArenaTheme.alpha(ArenaTheme.GOLD, 0.02f, S1), false, 0, 0, w, h);
            g.fillRect(ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.9f, S2), 0, 0, Math.max(2f, ArenaTheme.px(3)), h);
        }
        float x = gap;
        if (allowForIcon) {
            if (icon != null) {
                final float old = g.getfloatAlphaComposite();
                if (!enabled) {
                    g.setAlphaComposite(old * 0.5f);
                }
                g.drawImage(icon, iconX, (h - iconSize) / 2f, iconSize, iconSize);
                if (!enabled) {
                    g.setAlphaComposite(old);
                }
            }
            x = iconX + iconSize + gap;
        }
        ArenaArt.text(g, text, font, enabled ? (hovered || pressed ? ArenaTheme.GOLD_BRIGHT : ArenaTheme.TEXT) : ArenaTheme.TEXT_DIM, x, 0, w - x - gap, h,
                Align.left);
        g.fillRect(SEPARATOR, gap, h - 1, w - 2 * gap, 1);
        return true;
    }

    // ---- check boxes (FCheckBox) ------------------------------------------------------------------------------------

    /** A check box as the Arena remember box: a dark well in a gold outline, a gold tick when checked. False without the Arena look. */
    public static boolean checkBox(final Graphics g, final boolean checked, final float x, final float y, final float w, final float h) {
        if (!on()) {
            return false;
        }
        final float box = Math.min(w, h);
        ArenaDialogKit.checkBox(g, null, checked, false, x + (w - box) / 2f, y + (h - box) / 2f, box, 1f);
        return true;
    }

    // ---- text fields (FTextField, FComboBox) ------------------------------------------------------------------------

    /** A text field's well: dark, with a gold rim (brighter while typing). False without the Arena look. */
    public static boolean fieldBackground(final Graphics g, final float w, final float h, final boolean editing) {
        if (!on()) {
            return false;
        }
        final float r = Math.min(h * 0.18f, ArenaTheme.px(6));
        g.fillSmoothRoundRect(FIELD, 0, 0, w, h, r);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.5f)), editing ? ArenaTheme.GOLD_BRIGHT : ArenaTheme.alpha(ArenaTheme.GOLD, 0.7f, S1), 0, 0, w, h, r);
        return true;
    }

    // ---- label buttons (FLabel) -------------------------------------------------------------------------------------

    /**
     * Behind a label that works as a button (opaque, selectable or pressed): an Arena secondary button (gold when
     * selected). Plain labels stay as they are. False without the Arena look.
     */
    public static boolean labelButton(final Graphics g, final float w, final float h, final boolean pressed, final boolean selected,
            final boolean hovered) {
        if (!on()) {
            return false;
        }
        final float r = Math.min(h / 2f, ArenaTheme.px(10));
        final Color top, bottom, rim;
        if (pressed) {
            top = ArenaTheme.BTN_BOTTOM;
            bottom = ArenaTheme.BLACK;
            rim = ArenaTheme.BTN_HOVER_RIM;
        } else if (selected) {
            top = ArenaTheme.BTN_ON_TOP;
            bottom = ArenaTheme.BTN_ON_BOTTOM;
            rim = ArenaTheme.GOLD_BRIGHT;
        } else {
            top = hovered ? ArenaTheme.BTN_HOVER_TOP : ArenaTheme.BTN_TOP;
            bottom = hovered ? ArenaTheme.BTN_HOVER_BOTTOM : ArenaTheme.BTN_BOTTOM;
            //on the board the Arena match's white rim; in the menus a gold line in place of the skin's white borders
            rim = hovered ? ArenaTheme.BTN_HOVER_RIM : ArenaTheme.isMatchBoard() ? ArenaTheme.BTN_RIM : ArenaMenus.LINE;
        }
        final float inset = Math.max(1f, ArenaTheme.px(1));
        g.fillSmoothRoundRectGradient(top, bottom, true, inset, inset, w - 2 * inset, h - 2 * inset, r);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.4f)), rim, inset, inset, w - 2 * inset, h - 2 * inset, r);
        return true;
    }

    // ---- list rows (FList, FChoiceList) -----------------------------------------------------------------------------

    /** Under a list: navy glass (no skin texture). False without the Arena look. */
    public static boolean listBackground(final Graphics g, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(SHEET_TOP, SHEET_BOTTOM, true, 0, 0, w, h);
        return true;
    }

    /** The fill of a list row: gold for the selected, a soft navy zebra for the rest, or null without the Arena look. */
    public static FSkinColor rowFill(final boolean selected, final boolean alternate) {
        if (!on()) {
            return null;
        }
        return selected ? ROW_SELECTED : alternate ? ROW_ALT : ROW;
    }
}
