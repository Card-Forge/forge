package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.game.GameView;
import forge.game.phase.PhaseType;
import forge.screens.match.MatchController;
import forge.util.Localizer;

/**
 * The Arena look of the match prompt (port of the Swing VPrompt's Arena layout). Passing priority is a luminous orange
 * action button in the bottom right corner: "Next" on your turn, "Pass" on someone else's, "Resolve" while something
 * waits on the stack, with where it leads written under it ("To Combat") and a strip above it showing where in the turn
 * play is; the end-turn button sits small beside the caption. A decision sits centred above your portrait: its message
 * on a soft dark pill with two equal buttons beneath. While only waiting on someone else, the prompt is not shown. The
 * game logic keeps reading the buttons' own texts (OK, End Turn); only what is shown changes.
 */
public final class ArenaPrompt {
    private ArenaPrompt() { }

    private static final Color CAPTION = ArenaTheme.rgb(0xDADEE6), SHADE = ArenaTheme.rgba(0, 0, 0, 92);
    private static final Color S1 = new Color();
    private static final int[] STRIP = {ArenaGlyphs.STEP, ArenaGlyphs.MAIN, ArenaGlyphs.ATTACK, ArenaGlyphs.MAIN, ArenaGlyphs.STEP};

    /** What a prompt remembers between frames: how far it has come into view, and the turn strip's light. */
    public static final class State {
        public final ArenaMotion.Fade shown = new ArenaMotion.Fade(1f);
        final ArenaMotion.Fade stripMix = new ArenaMotion.Fade(1f);
        int stripLit = -1, stripWas = -1;
        public ArenaMatchLayout.PromptMode mode;
        public boolean hidden;
    }

    private static String msg(final String key) {
        return Forge.getLocalizer() == null ? key : Forge.getLocalizer().getMessage(key);
    }

    /** Whether the OK button only passes priority (OK with End Turn or Undo beside it), as Swing decides. */
    public static boolean isPriorityOnly(final String ok, final String cancel) {
        return msg("lblOK").equals(ok) && cancel != null && (cancel.equals(msg("lblEndTurn")) || cancel.startsWith(msg("lblUndo")));
    }

    /** What the action button says: Next on your turn, Pass on another's, Resolve while the stack waits. */
    public static String actionLabel() {
        final GameView game = MatchController.instance.getGameView();
        if (game != null && game.getStack() != null && !game.getStack().isEmpty()) {
            return msg("lblArenaResolve");
        }
        return isMyTurn(game) ? msg("lblArenaNext") : msg("lblArenaPass");
    }

    private static boolean isMyTurn(final GameView game) {
        return game != null && ArenaBoard.isMe(game.getPlayerTurn());
    }

    /** Where passing leads, under the action button (only on your turn with an empty stack). */
    public static String caption() {
        final GameView game = MatchController.instance.getGameView();
        if (game == null || !isMyTurn(game) || game.getPhase() == null || (game.getStack() != null && !game.getStack().isEmpty())) {
            return "";
        }
        switch (game.getPhase()) {
            case UNTAP:
            case UPKEEP:
            case DRAW:
                return msg("lblArenaToMain");
            case MAIN1:
                return msg("lblArenaToCombat");
            case COMBAT_BEGIN:
            case COMBAT_DECLARE_ATTACKERS:
            case COMBAT_DECLARE_BLOCKERS:
            case COMBAT_FIRST_STRIKE_DAMAGE:
            case COMBAT_DAMAGE:
            case COMBAT_END:
                return msg("lblArenaToMain2");
            default:
                return msg("lblArenaToEnd");
        }
    }

    /** The prompt's sizes for a window this tall. */
    public static float stripHeight(final float h) {
        return Math.round(h * 0.032f);
    }

    public static float actionHeight(final float h) {
        return Math.round(h * 0.07f);
    }

    public static float captionHeight(final float h) {
        return Math.round(h * 0.026f);
    }

    public static float smallHeight(final float h) {
        return Math.round(h * 0.038f);
    }

    public static float smallWidth(final float h) {
        return Math.round(h * 0.062f);
    }

    /** A decision's buttons (Swing arenaButtonHeight). */
    public static float decisionButtonHeight(final float h) {
        return Math.max(ArenaTheme.px(30), Math.round(h * 0.058f));
    }

    public static FSkinFont messageFont(final float h) {
        return ArenaCards.evenFont(Math.max(ArenaTheme.px(13), decisionButtonHeight(h) * 0.34f));
    }

    /** The turn at a glance above the action button: a step, main, combat, main, a step, with the current one alight. */
    public static void drawStrip(final Graphics g, final State st, final float w, final float stripH, final float alpha) {
        final GameView game = MatchController.instance.getGameView();
        int lit = -1;
        if (game != null && game.getPhase() != null) {
            final PhaseType p = game.getPhase();
            lit = p == PhaseType.UNTAP || p == PhaseType.UPKEEP || p == PhaseType.DRAW ? 0 : p == PhaseType.MAIN1 ? 1 : p == PhaseType.MAIN2 ? 3
                    : p == PhaseType.END_OF_TURN || p == PhaseType.CLEANUP ? 4 : 2;
        }
        if (lit != st.stripLit) {
            // the light moves along from the part of the turn just left
            st.stripWas = st.stripLit;
            st.stripLit = lit;
            st.stripMix.set(0f);
            st.stripMix.to(1f, 300);
        }
        final float mix = st.stripMix.get();
        final float size = stripH, gap = size / 4f;
        final float total = STRIP.length * size + (STRIP.length - 1) * gap;
        float x = (w - total) / 2f;
        for (int i = 0; i < STRIP.length; i++) {
            final float light = i == st.stripLit ? mix : i == st.stripWas ? 1f - mix : 0f;
            if (light < 1f) {
                ArenaGlyphs.draw(g, STRIP[i], x, 0, size, size, false, true, false, alpha * (1f - light));
            }
            if (light > 0f) {
                ArenaGlyphs.draw(g, STRIP[i], x, 0, size, size, true, true, false, alpha * light);
            }
            x += size + gap;
        }
    }

    /** White words with a dark edge, readable over whatever the corner of the board is doing. */
    public static void drawCaption(final Graphics g, final String caption, final float x, final float y, final float w, final float h, final float alpha) {
        if (caption == null || caption.isEmpty()) {
            return;
        }
        final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(13), ArenaBoard.windowH() * 0.0165f));
        ArenaArt.textOutlined(g, caption, f, ArenaTheme.alpha(CAPTION, alpha, S1), x, y, w, h, Align.center);
    }

    /** The soft dark shadow a decision's words sit on, fading out on every side. */
    public static void drawMessageShade(final Graphics g, final float x, final float y, final float w, final float h, final float alpha) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final float soft = Math.min(ArenaTheme.px(20), h / 2f - 1);
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.37f * alpha, S1), x + soft, y + soft, w - 2 * soft, h - 2 * soft, soft, soft);
        g.fillSmoothRoundRect(ArenaTheme.fadeBy(SHADE, alpha, S1), x + soft / 2f, y + soft / 2f, w - soft, h - soft, soft);
    }

    private static final Color PANEL = ArenaTheme.rgba(8, 10, 16, 236);
    private static String endTurnHint, undoHint, autoPassText;
    private static long autoPassAt = Long.MIN_VALUE / 2;

    public static String endTurnHint() {
        if (endTurnHint == null) {
            endTurnHint = msg("lblArenaHintEndTurn");
        }
        return endTurnHint;
    }

    public static String undoHint() {
        if (undoHint == null) {
            undoHint = msg("lblArenaHintUndo");
        }
        return undoHint;
    }

    /** A hint plate above a button (in its coordinates), kept inside the window. */
    public static void drawHintAbove(final Graphics g, final String text, final forge.toolbox.FDisplayObject button) {
        final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(16), ArenaBoard.windowH() * 0.015f));
        ArenaArt.hint(g, text, f, button.getWidth() / 2f, -ArenaArt.hintHeight(f) - ArenaTheme.px(6), -button.screenPos.x,
                ArenaBoard.windowW() - button.screenPos.x);
    }

    /**
     * While auto-passing is on (End turn, F2-F4, the stack's "yield"), a plate above the priority buttons says until when
     * ("Yielding until end of turn"); the game's own words, read again at most every 100 ms.
     */
    public static void drawAutoPass(final Graphics g, final forge.toolbox.FDisplayObject prompt) {
        if (com.badlogic.gdx.utils.TimeUtils.timeSinceMillis(autoPassAt) >= ArenaPlayerInfo.REFRESH_MS) {
            autoPassAt = com.badlogic.gdx.utils.TimeUtils.millis();
            autoPassText = MatchController.instance.getAutoPassText();
        }
        if (autoPassText == null || autoPassText.isEmpty()) {
            return;
        }
        final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(16), ArenaBoard.windowH() * 0.015f));
        ArenaArt.hint(g, autoPassText, f, prompt.getWidth() / 2f, -ArenaArt.hintHeight(f) - ArenaTheme.px(6), -prompt.screenPos.x,
                ArenaBoard.windowW() - prompt.screenPos.x, ArenaTheme.SELECTABLE, ArenaTheme.WHITE);
    }

    /**
     * A decision's own ground: a solid dark panel with a gold rim behind its words and buttons, so the board under it never
     * shows through (it is drawn above the arcs, and placed where it covers nothing you can pick).
     */
    public static void drawDecisionPanel(final Graphics g, final float x, final float y, final float w, final float h, final float alpha) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final float r = Math.min(ArenaTheme.px(16), h / 4f);
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.6f * alpha, S1), x, y + ArenaTheme.px(4), w, h, r, ArenaTheme.px(14));
        g.fillSmoothRoundRect(ArenaTheme.fadeBy(PANEL, alpha, S1), x, y, w, h, r);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.5f)), ArenaTheme.alpha(ArenaTheme.GOLD, 0.8f * alpha, S1), x, y, w, h, r);
    }

    /** One of the prompt's buttons: the Arena pill (molten when primary), its label fitted inside. */
    public static void drawButton(final Graphics g, final String label, final float w, final float h, final boolean primary, final boolean hovered,
            final boolean pressed, final boolean enabled, final float alpha) {
        if (alpha <= 0f) {
            return;
        }
        final float old = g.getfloatAlphaComposite();
        if (alpha < 1f) {
            g.setAlphaComposite(old * alpha);
        }
        ArenaArt.button(g, 0, 0, w, h, primary, hovered, pressed, false, false, enabled);
        if (label != null && !label.isEmpty()) {
            final float pad = ArenaArt.buttonPad(h);
            final float bh = h - 2 * pad;
            FSkinFont f = ArenaCards.evenFont(bh * (label.length() == 1 ? 1.05f : 0.52f)); //a lone chevron is a small glyph
            final float room = w - 2 * pad - bh * 0.6f;
            if (ArenaArt.textWidth(label, f) > room) {
                f = ArenaCards.evenFont(bh * 0.52f * room / Math.max(1f, ArenaArt.textWidth(label, f)));
            }
            final float dy = pressed ? Math.max(1f, ArenaTheme.px(1)) : 0f;
            if (primary && enabled) {
                g.drawText(label, f, ArenaTheme.rgbaInto(60, 22, 0, 190, S1), pad, pad + dy + 1, w - 2 * pad, bh, false, Align.center, true);
                ArenaArt.textBold(g, label, f, ArenaTheme.WHITE, pad, pad + dy, w - 2 * pad, bh, Align.center);
            } else {
                ArenaArt.textShadowed(g, label, f, enabled ? ArenaTheme.TEXT : ArenaTheme.rgbaInto(220, 232, 250, 200, S1), pad, pad + dy, w - 2 * pad, bh, Align.center);
            }
        }
        if (alpha < 1f) {
            g.setAlphaComposite(old);
        }
    }

    /** Localizer access for code that may run before it is ready. */
    static Localizer localizer() {
        return Forge.getLocalizer();
    }
}
