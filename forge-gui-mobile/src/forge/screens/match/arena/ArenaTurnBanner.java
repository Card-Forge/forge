package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.game.player.PlayerView;
import forge.gui.FThreads;

/**
 * Whose turn it is, said across the middle of the board as the turn begins (port of the Swing {@code ArenaFx} turn
 * banner): a dark band, clear at both ends, with an accent line along its top and bottom, slides a little and fades in,
 * holds, and goes the same way, about 1.15 s in all. It never takes input.
 *
 * <p>The match screen calls {@link #draw} from its overlay every frame (it draws nothing while idle) and
 * {@link #showTurn}/{@link #show} when a turn begins.</p>
 */
public final class ArenaTurnBanner {
    private ArenaTurnBanner() { }

    /** How long it shows in all, and the shares of that it takes to come and to go (Swing Banner). */
    private static final int DURATION_MS = 1150;
    private static final float IN_UNTIL = 0.2f, OUT_FROM = 0.8f;
    /** The band's middle and height, as shares of the board's height. */
    private static final float BAND_Y = 0.44f, BAND_H = 0.1f;

    private static final Color ACCENT_LOCAL = ArenaTheme.GOLD_BRIGHT;
    private static final Color ACCENT_OTHER = ArenaTheme.rgb(0xFF8A5C);
    private static final Color S1 = new Color(), S2 = new Color();

    private static String text;
    private static boolean local;
    private static float progress = -1f;
    private static ArenaMotion.Tween tween;

    /**
     * Says whose turn it is: "Your turn" for a player at this screen, "{name}'s turn" otherwise. Any thread. Does
     * nothing when the Arena look is off.
     */
    public static void showTurn(final PlayerView player, final boolean localPlayersTurn) {
        if (player == null) {
            return;
        }
        final String words = localPlayersTurn ? Forge.getLocalizer().getMessage("lblArenaYourTurn")
                : Forge.getLocalizer().getMessage("lblArenaPlayersTurn", player.getName());
        show(words, localPlayersTurn);
    }

    /** Shows these words on the band (gold accent for the local player's turn, ember for anyone else's). Any thread. */
    public static void show(final String words, final boolean localPlayersTurn) {
        if (words == null || words.isEmpty()) {
            return;
        }
        FThreads.invokeInEdtNowOrLater(() -> start(words, localPlayersTurn));
    }

    private static void start(final String words, final boolean localPlayersTurn) {
        if (!ArenaTheme.isMatchBoard()) {
            return;
        }
        if (tween != null) {
            tween.cancel();
        }
        text = words;
        local = localPlayersTurn;
        progress = 0f;
        tween = ArenaMotion.play(DURATION_MS, ArenaMotion.LINEAR, t -> progress = t, ArenaTurnBanner::clear);
    }

    /** Takes it down at once (a new game, the screen going). Render thread. */
    public static void clear() {
        if (tween != null) {
            tween.cancel();
            tween = null;
        }
        progress = -1f;
        text = null;
    }

    /** Whether it is showing now. */
    public static boolean isShowing() {
        return text != null && progress >= 0f;
    }

    /** Draws the band over a board {@code w} by {@code h} (the match screen's size), if it is showing. Render thread. */
    public static void draw(final Graphics g, final float w, final float h) {
        if (text == null || progress < 0f || w <= 0 || h <= 0 || !ArenaTheme.isActive()) {
            return;
        }
        final float in = ArenaMotion.OUT.at(ArenaMotion.clamp01(progress / IN_UNTIL));
        final float out = ArenaMotion.clamp01((progress - OUT_FROM) / (1f - OUT_FROM));
        final float alpha = in * (1f - out);
        if (alpha <= 0f) {
            return;
        }
        final float bandH = Math.max(ArenaTheme.px(44), Math.round(h * BAND_H));
        final float y = Math.round(h * BAND_Y - bandH / 2f);
        final Color accent = local ? ACCENT_LOCAL : ACCENT_OTHER;
        ArenaArt.banner(g, 0, y, w, bandH, accent, alpha);

        // the words come in from a little to the left and go on a little to the right
        final float dx = (1f - in) * -w * 0.03f + out * w * 0.03f;
        final FSkinFont font = ArenaTheme.fontPx(Math.min(72, Math.round(bandH * 0.5f)));
        final float o = Math.max(1f, ArenaTheme.px(1.5f));
        g.drawText(text, font, ArenaTheme.alpha(ArenaTheme.BLACK, 0.75f * alpha, S1), dx + o, y + 2 * o, w, bandH, false, Align.center, true);
        ArenaArt.textBold(g, text, font, ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S2), dx, y, w, bandH, Align.center);
    }
}
