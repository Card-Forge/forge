package forge.screens.match.arena;

import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.game.GameView;
import forge.screens.match.MatchController;

/**
 * Where the parts of a player's area go on the Arena board, as fractions of the window, so 21:9, 16:9 and smaller
 * landscape screens all get the same board (port of the Swing {@code VField.ArenaFieldLayout},
 * {@code PlayArea.layoutArenaBoard} and the {@code CardArea} hand fan). The two fields tile the window: the opponent's
 * the top {@link ArenaTheme#FIELD_SPLIT}, yours the rest. With three or four players the opponents share the top as
 * "seats" side by side. Everything is returned in window coordinates unless a method says otherwise; nothing here
 * allocates.
 */
public final class ArenaBoard {
    private ArenaBoard() { }

    // ---- portrait --------------------------------------------------------------------------------------------------
    /** Portrait diameter, as a share of the window's height. */
    public static final float PORTRAIT = 0.125f, PORTRAIT_SEAT = 0.066f;
    /** Where the middle of the local portrait is, as a share of the window's height. */
    public static final float LOCAL_PORTRAIT_Y = 0.778f;

    // ---- battlefield rows (PlayArea) ---------------------------------------------------------------------------------
    /** A battlefield tile's height (the local front row), as a share of the window's height, and how far a crowded row shrinks. */
    public static final float TILE_H = 0.145f, TILE_H_SEAT = 0.11f, TILE_H_MIN = 0.07f;
    /** Tiles are wider than tall: art, name and stats. */
    public static final float TILE_RATIO = 1.27f;
    /** Half the gap the back row leaves for the portrait. */
    public static final float CLEAR = 0.205f, CLEAR_SEAT = 0.008f;
    /** The board recedes: each row's size against the local front row. */
    public static final float OWN_BACK = 0.85f, FOE_FRONT = 0.92f, FOE_BACK = 0.64f, SEAT_BACK = 0.74f;
    /** In combat the fighters step out towards the other army, and stand a little larger. */
    public static final float COMBAT_STEP = 0.015f, COMBAT_SCALE = 1.05f;
    /** Rows' tops in the window: the local front and back row, an opponent's back and front row. */
    public static final float LOCAL_FRONT_Y = 0.425f, LOCAL_BACK_Y = 0.605f, FOE_BACK_Y = 0.08f, FOE_FRONT_Y = 0.205f,
            SEAT_BACK_Y = 0.1f, SEAT_FRONT_Y = 0.205f;
    /** Space between tiles, as a share of a tile's width; stacked copies show this much of each. */
    public static final float TILE_GAP = 0.085f, STACK_STEP = 0.1f;

    // ---- the hand fan (CardArea.doFanLayout) ------------------------------------------------------------------------
    /** The local hand's strip: from this share of the window's height down, between these shares of its width. */
    public static final float HAND_TOP = 0.835f, HAND_LEFT = 0.14f, HAND_RIGHT = 0.86f;
    public static final float FAN_STEP = 0.78f, FAN_MIN_STEP = 0.3f, FAN_ARC = 0.06f, FAN_LIFT = 0.08f, FAN_SINK = 0.52f,
            FAN_COST_ROW = 0.09f;
    /** Lean per place away from the middle of the hand, and the most a card leans (degrees). */
    public static final float FAN_LEAN = 2.2f, FAN_MAX_LEAN = 9f;
    /** How much a hovered hand card grows besides rising. */
    public static final float FAN_HOVER_GROWTH = 0.06f;

    // ---- window ----------------------------------------------------------------------------------------------------
    public static float windowW() {
        return Math.max(1, Forge.getScreenWidth());
    }

    public static float windowH() {
        return Math.max(1, Forge.getScreenHeight());
    }

    /** Whether opponents sit side by side along the top (more than two players still in the game). */
    public static boolean isSeatGame() {
        final GameView game = MatchController.instance.getGameView();
        return game != null && game.getPlayers() != null && countLiving(game) > 2;
    }

    private static int countLiving(final GameView game) {
        int n = 0;
        for (final forge.game.player.PlayerView p : game.getPlayers()) {
            if (!p.getHasLost()) {
                n++;
            }
        }
        return n == 0 ? game.getPlayers().size() : n;
    }

    // ---- portrait --------------------------------------------------------------------------------------------------
    public static float portraitDiameter(final boolean seat) {
        return Math.round(windowH() * (seat ? PORTRAIT_SEAT : PORTRAIT));
    }

    /** The portrait's centre y in the window: low over the local field, hanging from the top for an opponent. */
    public static float portraitCenterY(final boolean local, final boolean seat) {
        final float d = portraitDiameter(seat);
        if (local) {
            return Math.round(windowH() * LOCAL_PORTRAIT_Y);
        }
        return seat ? Math.round(windowH() * 0.008f + d / 2f) : Math.round(d * 0.62f);
    }

    // ---- rows ------------------------------------------------------------------------------------------------------
    /** The front row's tile height before a crowded row shrinks it. */
    public static float frontTileHeight(final boolean local, final boolean seat) {
        final float share = seat ? TILE_H_SEAT : local ? TILE_H : TILE_H * FOE_FRONT;
        return Math.round(windowH() * share);
    }

    /** The back row's tile height for a front row this tall. */
    public static float backTileHeight(final boolean local, final boolean seat, final float frontHeight) {
        final float share = seat ? SEAT_BACK : local ? OWN_BACK : FOE_BACK / FOE_FRONT;
        return Math.max(minTileHeight(), Math.round(frontHeight * share));
    }

    public static float minTileHeight() {
        return Math.round(windowH() * TILE_H_MIN);
    }

    /** The smallest a back row's lands get before they start to overlap. */
    public static float smallestBackHeight() {
        return Math.round(windowH() * TILE_H * FOE_BACK);
    }

    public static float frontRowTop(final boolean local, final boolean seat) {
        return Math.round(windowH() * (local ? LOCAL_FRONT_Y : seat ? SEAT_FRONT_Y : FOE_FRONT_Y));
    }

    public static float backRowTop(final boolean local, final boolean seat) {
        return Math.round(windowH() * (local ? LOCAL_BACK_Y : seat ? SEAT_BACK_Y : FOE_BACK_Y));
    }

    /** Half the gap the back row leaves round the portrait. */
    public static float portraitClear(final boolean seat) {
        return Math.round(windowH() * (seat ? CLEAR_SEAT : CLEAR));
    }

    /** The side margin of the rows. */
    public static float rowMargin(final boolean seat) {
        return Math.round(windowW() * (seat ? 0.016f : 0.024f));
    }

    /** How far right an opponent's back row may run in a field {@code fieldW} wide (short of their piles), or yours. */
    public static float backRowRight(final boolean local, final boolean seat, final float fieldW) {
        if (seat) {
            return fieldW - rowMargin(true) - Math.round(fieldW * 0.02f);
        }
        return local ? fieldW - rowMargin(false) : fieldW - Math.round(fieldW * 0.172f);
    }

    // ---- piles, names, extras --------------------------------------------------------------------------------------
    /** Where the library, graveyard and other piles go, in the window: bottom left for you, top right for an opponent. */
    public static Rectangle piles(final boolean local, final boolean seat, final float fieldX, final float fieldW, final Rectangle out) {
        final float w = windowW(), h = windowH();
        if (local) {
            return out.set(Math.round(h * 0.012f), Math.round(h * 0.822f), Math.round(w * 0.16f), Math.round(h * 0.135f));
        }
        if (seat) {
            final float d = portraitDiameter(true);
            final float zonesX = fieldX + fieldW / 2 + Math.round(d * 0.7f);
            final float reserve = fieldX + fieldW >= w - 4 ? Math.max(Math.round(h * 0.075f), Math.round(w * 0.06f)) : 14;
            return out.set(zonesX, Math.round(h * 0.008f), Math.max(0, fieldX + fieldW - zonesX - reserve), Math.round(h * 0.076f));
        }
        final float zonesH = Math.round(h * 0.118f), zonesW = Math.round(w * 0.15f);
        final float right = fieldX + fieldW - Math.max(Math.round(h * 0.075f), Math.round(fieldW * 0.06f));
        return out.set(right - zonesW, Math.round(h * 0.006f), zonesW, zonesH);
    }

    /** An opponent's hand: a small fan of card backs beside the portrait. */
    public static Rectangle opponentHand(final boolean seat, final float fieldX, final float fieldW, final Rectangle out) {
        final float w = windowW(), h = windowH(), d = portraitDiameter(seat), cx = fieldX + fieldW / 2;
        if (seat) {
            final float fanW = Math.round(h * 0.1f);
            return out.set(cx - Math.round(d * 0.7f) - fanW, Math.round(h * -0.012f), fanW, Math.round(h * 0.085f));
        }
        final float fanW = Math.round(h * 0.26f);
        return out.set(cx - Math.round(d * 1.6f) - Math.round(w * 0.015f) - fanW, Math.round(h * -0.045f), fanW, Math.round(h * 0.105f));
    }

    /** The player's name plate in the corner. */
    public static Rectangle name(final boolean local, final boolean seat, final float fieldX, final float fieldW, final Rectangle out) {
        final float w = windowW(), h = windowH(), nameH = Math.round(h * 0.03f);
        if (local) {
            return out.set(Math.round(h * 0.014f), Math.round(h * 0.962f), Math.round(w * 0.16f), nameH);
        }
        if (seat) {
            final float d = portraitDiameter(true);
            return out.set(fieldX + Math.round(w * 0.0075f) + 10, 8, Math.max(40, fieldW / 2 - Math.round(d * 0.7f) - Math.round(h * 0.1f) - 16), nameH);
        }
        return out.set(fieldX + Math.round(h * 0.014f), Math.round(h * 0.008f), Math.round(w * 0.2f), nameH);
    }

    /** The local hand's strip (the cards sink below the window's bottom edge). */
    public static Rectangle hand(final Rectangle out) {
        final float w = windowW(), h = windowH();
        final float top = Math.round(h * HAND_TOP);
        return out.set(Math.round(w * HAND_LEFT), top, Math.round(w * (HAND_RIGHT - HAND_LEFT)), h - top);
    }

    /** Where what floats beside the portrait goes (mana in the pool, commander damage): its left end and middle. */
    public static float extrasLeft(final boolean seat, final float portraitCx) {
        return portraitCx + Math.round(portraitDiameter(seat) * 1.7f);
    }
}
