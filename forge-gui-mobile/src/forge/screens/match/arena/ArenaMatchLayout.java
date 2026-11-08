package forge.screens.match.arena;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FButton;

/**
 * Where things sit on the Arena match screen (port of the Swing {@code res/defaults/match_arena.xml} board and the
 * placement code of Swing {@code VPrompt} / {@code ArenaCornerMenu}). The two battlefields tile the whole window, the
 * opponent's field on top ({@link ArenaTheme#FIELD_SPLIT} of the height), yours below it; with three or four players
 * the opponents sit side by side along the top. Everything else floats over the fields:
 * <ul>
 * <li>the prompt: priority-only buttons (OK with End Turn or Undo) in the bottom right corner, a decision centred
 *     above the local portrait (its bottom at 71% of the height);</li>
 * <li>the corner menu (gear and log buttons) in the top right corner;</li>
 * <li>the stack, docked on the right ({@link #stackDock}), the log, docked on the left ({@link #logDock}), both
 *     toggled from the corner menu or with S / L; with seats they sit below the seats.</li>

 * </ul>
 * Sizes are fractions of the screen, so 21:9 and 16:9 both work; fixed Swing pixel sizes go through
 * {@link ArenaTheme#px}. Each player panel lays out its own inside ({@code VPlayerPanel.doArenaLayout}, with the
 * {@link ArenaBoard} fractions): field rows, portrait, phase ladder, piles and name plate; the local panel's hand fans
 * out along the bottom edge, between {@link #HAND_LEFT} and {@link #HAND_RIGHT} of the width.
 *
 * <p>Nothing here allocates per call: results go into the caller's rectangle.</p>
 */
public final class ArenaMatchLayout {
    private ArenaMatchLayout() { }

    /** Where the hand runs, as fractions of the screen (Swing HAND_0 cell x 0.14 .. 0.86, from 0.835 down). */
    public static final float HAND_LEFT = 0.14f, HAND_RIGHT = 0.86f, HAND_TOP = 0.835f;
    /** The stack and log docks (Swing REPORT cell 0.8, 0.17, 0.19 x 0.42). */
    public static final float DOCK_TOP = 0.17f, DOCK_W = 0.19f, DOCK_H = 0.42f, DOCK_MARGIN = 0.01f;
    /** Where a decision prompt's bottom is (the local portrait's frame top in Swing). */
    public static final float DECISION_BOTTOM = 0.71f;

    private static volatile boolean boardPossible = true;

    /**
     * Whether the Arena layout is used: Arena look on, landscape (the Arena board has no portrait layout), and a match
     * the board can show (not two local players each with their own prompt at opposite ends of the screen).
     */
    public static boolean applies() {
        return ArenaTheme.isActive() && Forge.isLandscapeMode() && boardPossible;
    }

    /** Set by each new match screen: false for a layout the Arena board cannot show, which then keeps the default look. */
    public static void setBoardPossible(final boolean possible) {
        boardPossible = possible;
    }

    /** Whether the desktop-only extras apply (mouse and keyboard: right click, shortcuts, wheel, hover preview). */
    public static boolean desktop() {
        return ArenaTheme.isActive() && forge.gui.GuiBase.getInterface() != null && forge.gui.GuiBase.getInterface().isRunningOnDesktop();
    }

    /**
     * Lays the player panels out. {@code topToBottom} is MatchScreen's list (the local player last).
     * Two players: the opponent over the top {@link ArenaTheme#FIELD_SPLIT}, you below. More: every opponent gets an
     * equal share of the top strip, side by side.
     */
    public static void layoutPanels(final List<VPlayerPanel> topToBottom, final float w, final float h) {
        final int n = topToBottom.size();
        if (n == 0) {
            return;
        }
        final float split = Math.round(h * ArenaTheme.FIELD_SPLIT);
        if (n == 1) {
            topToBottom.get(0).setBounds(0, 0, w, h);
            return;
        }
        final int opponents = n - 1;
        final float each = w / opponents;
        for (int i = 0; i < opponents; i++) {
            final float x = Math.round(i * each);
            final float x2 = i == opponents - 1 ? w : Math.round((i + 1) * each);
            topToBottom.get(i).setBounds(x, 0, x2 - x, split);
        }
        topToBottom.get(n - 1).setBounds(0, split, w, h - split);
    }

    /** The two kinds of prompt the Arena board shows (Swing ArenaPromptMode without HIDDEN: the view decides that). */
    public enum PromptMode { PRIORITY, DECISION }

    /**
     * Priority-only (OK plus End Turn or Undo) or a decision, from the prompt's buttons, as Swing VPrompt decides.
     * Also when nothing is enabled (waiting on someone else): that stays in the corner.
     */
    public static PromptMode promptMode(final VPrompt prompt) {
        final FButton ok = prompt.getBtnOk(), cancel = prompt.getBtnCancel();
        if (!ok.isEnabled() && !cancel.isEnabled()) {
            return PromptMode.PRIORITY;
        }
        final String okText = ok.getText(), cancelText = cancel.getText();
        final String lblOk = Forge.getLocalizer().getMessage("lblOK");
        final boolean priority = lblOk.equals(okText) && cancelText != null
                && (cancelText.equals(Forge.getLocalizer().getMessage("lblEndTurn"))
                || cancelText.startsWith(Forge.getLocalizer().getMessage("lblUndo")));
        return priority ? PromptMode.PRIORITY : PromptMode.DECISION;
    }

    /**
     * Where the prompt goes for this mode. Priority: 12.5% of the width, the Swing stack of strip, button, caption and
     * small cancel button tall (16.6% of the height), 1.2% in from the right, its bottom at 98.5%. Decision: max(340
     * Swing px, 40% of the height) wide, centred, its bottom at {@link #DECISION_BOTTOM}, 22% of the height tall (the
     * message pill above the buttons).
     */
    public static Rectangle promptBounds(final PromptMode mode, final float w, final float h, final Rectangle out) {
        if (mode == PromptMode.PRIORITY) {
            final float pw = Math.round(w * 0.125f);
            final float ph = Math.round(h * 0.032f) + Math.round(h * 0.07f) + Math.round(h * 0.026f) + Math.round(h * 0.038f) + 2;
            return out.set(w - pw - Math.round(w * 0.012f), Math.round(h * 0.985f) - ph, pw, ph);
        }
        final float pw = Math.max(ArenaTheme.px(340), Math.round(h * 0.4f));
        final float ph = Math.round(h * 0.22f);
        return out.set(Math.round((w - pw) / 2), Math.max(ArenaTheme.px(8), Math.round(h * DECISION_BOTTOM) - ph), pw, ph);
    }

    /** Diameter of a corner button (Swing: max(24, 3% of the window height)). */
    public static float cornerButtonSize(final float h) {
        return Math.max(ArenaTheme.px(24), Math.round(h * 0.03f));
    }

    /** Gap round the corner buttons (Swing 6 px). */
    public static float cornerGap() {
        return Math.max(2f, ArenaTheme.pxRound(6));
    }

    /** The corner menu: two round buttons side by side in the top right corner. */
    public static Rectangle cornerMenu(final float w, final float h, final Rectangle out) {
        final float s = cornerButtonSize(h), gap = cornerGap();
        final float cw = 2 * s + gap;
        return out.set(w - cw - gap, gap, cw, s);
    }

    /**
     * With three or four players the docks go below the seats (their glass is {@link #SEAT_DOCK_TOP} of the height
     * tall), a little shorter so they end above the priority buttons: at the Swing height they would cover the right
     * (or left) seat's whole battlefield, where a two-player board only has the opponent's front row there.
     */
    public static final float SEAT_DOCK_TOP = 0.36f, SEAT_DOCK_H = 0.38f;

    private static float dockTop(final float h) {
        return Math.round(h * (ArenaBoard.isSeatGame() ? SEAT_DOCK_TOP : DOCK_TOP));
    }

    private static float dockHeight(final float h) {
        return Math.round(h * (ArenaBoard.isSeatGame() ? SEAT_DOCK_H : DOCK_H));
    }

    /** The stack's dock: the right side, under the corner (Swing REPORT cell); below the seats in a bigger game. */
    public static Rectangle stackDock(final float w, final float h, final Rectangle out) {
        final float dw = Math.max(ArenaTheme.px(300), Math.round(w * DOCK_W));
        return out.set(w - dw - Math.round(w * DOCK_MARGIN), dockTop(h), dw, dockHeight(h));
    }

    /** The log's dock: the left side, the same size as the stack's. */
    public static Rectangle logDock(final float w, final float h, final Rectangle out) {
        final float dw = Math.max(ArenaTheme.px(300), Math.round(w * DOCK_W));
        return out.set(Math.round(w * DOCK_MARGIN), dockTop(h), dw, dockHeight(h));
    }


    /** What dropdowns may cover: the whole board (the prompt floats, it is not a strip any more). */
    public static Rectangle dropDownBoundary(final float w, final float h, final Rectangle out) {
        return out.set(0, 0, w, h);
    }
}
