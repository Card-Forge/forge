package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.Multiset;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FImageComplex;
import forge.assets.FSkinFont;
import forge.card.CardFaceSymbols;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.util.CardTranslation;

/**
 * The Arena look of the match's own cards, drawn on the GPU every frame (no per-card pictures): battlefield tiles (art,
 * a thin frame in the card's colour, the name on a pale lozenge, power and toughness on a plate, on a dark slab that
 * casts a shadow) and hand cards (the whole card, leaning in the fan). Both carry the Arena light: a soft bloom and a
 * rim in the colour of what the card can do now (play it, pick it, it is attacking, blocking, aimed at...), which comes
 * up and goes down rather than being switched, and breathes while it asks to be noticed. Ported from the Swing
 * {@code CardPanel} (paintArenaFrame, paintArenaTile) and {@code CardArea}'s fan.
 *
 * <p>Movement: a card that is laid out somewhere else glides there ({@link State#follow}), a hovered card lifts, a card
 * that taps swings over a little past an 8 degree tilt and drains of colour. All of it runs off {@link ArenaMotion}.</p>
 */
public final class ArenaCards {
    private ArenaCards() { }

    // the light of what a card can do now
    static final int GLOW_NONE = -1, GLOW_CHOSEN = 0, GLOW_SELECTABLE = 1, GLOW_TARGET = 2, GLOW_ATTACK = 3, GLOW_BLOCK = 4,
            GLOW_SOURCE = 5, GLOW_PLAYABLE = 6, GLOW_HOVER = 7, GLOW_AUTOTAP = 8;
    private static final Color[] GLOW_COLOR = {
            ArenaTheme.CHOSEN, ArenaTheme.SELECTABLE, ArenaTheme.rgb(0xBFEFFF), ArenaTheme.rgb(0xFF6A1F), ArenaTheme.rgb(0x4C8DFF),
            ArenaTheme.rgb(0xFFB020), ArenaTheme.PLAYABLE, ArenaTheme.HOVER, ArenaTheme.rgb(0xFFEC78)};
    private static final float[] GLOW_STRENGTH = {0.9f, 0.9f, 1f, 1f, 1f, 0.9f, 0.92f, 0.59f, 0.67f};

    /** A tapped tile turns this far (degrees, clockwise) and loses its colour. */
    public static final float TAPPED_ANGLE = 8f;
    /** A hovered tile grows this much. */
    private static final float HOVER_GROWTH = 0.035f;

    private static final Color SLAB_TOP = ArenaTheme.rgb(0x202026), SLAB_BOTTOM = ArenaTheme.rgb(0x0B0B0E), SLAB_BASE = ArenaTheme.rgb(0x030305);
    private static final Color TAPPED_LINE = ArenaTheme.rgb(0x4A4A4E), TAPPED_PALE = ArenaTheme.rgb(0x87878B), UNKNOWN_TINT = ArenaTheme.rgb(0x8A8F98);
    private static final Color NAME_TEXT = ArenaTheme.rgb(0x100E0C), NAME_EDGE = ArenaTheme.rgba(10, 10, 12, 120);
    private static final Color PLATE_EDGE = ArenaTheme.rgb(0x040406), PLATE_LINE = ArenaTheme.rgba(50, 44, 36, 170);
    private static final Color RIM_TOP = ArenaTheme.WHITE, RIM_BOTTOM = ArenaTheme.rgb(0xA8A49A), RIM_TOP_TAPPED = ArenaTheme.rgb(0xB4B4B4),
            RIM_BOTTOM_TAPPED = ArenaTheme.rgb(0x707070);
    private static final Color PLATE_TOP = ArenaTheme.rgb(0xFBF8EF), PLATE_BOTTOM = ArenaTheme.rgb(0xE0DACC), PLATE_TAPPED_TOP = ArenaTheme.rgb(0xA6A6A6),
            PLATE_TAPPED_BOTTOM = ArenaTheme.rgb(0x868686);
    private static final Color STATS_DAMAGED = ArenaTheme.rgb(0xB3261E), STATS_BUFFED = ArenaTheme.rgb(0x1450A8), STATS_PLAIN = ArenaTheme.rgb(0x141210),
            STATS_TAPPED = ArenaTheme.rgb(0x101012);
    private static final Color HIT = ArenaTheme.rgb(0xFF4632), CHIP = ArenaTheme.rgba(6, 7, 11, 215), CHIP_EDGE = ArenaTheme.rgba(255, 255, 255, 90);
    private static final Color BACK_FIELD = ArenaTheme.rgb(0x1C1846), BACK_EDGE = ArenaTheme.rgb(0x8C6D3B);

    // render-thread scratch
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color(), S4 = new Color(), S5 = new Color(), S6 = new Color();
    private static final Color[] STOPS3 = new Color[3];
    private static final float[] TOP_LIGHT = {0f, 0.5f, 1f};

    /** What a card's panel remembers between frames for its Arena look: where it was, its light, its motion. */
    public static final class State {
        // gliding to a new place: the offset from where the layout puts it
        final ArenaMotion.Spring dx = new ArenaMotion.Spring(0f, 75, 0.3f), dy = new ArenaMotion.Spring(0f, 75, 0.3f);
        private float seenX, seenY;
        private long seenFrame = Long.MIN_VALUE;
        private Object seenArea;
        // hover lift, tap swing, lean in the fan
        final ArenaMotion.Fade lift = new ArenaMotion.Fade(0f);
        final ArenaMotion.Fade tap = new ArenaMotion.Fade(0f);
        private boolean tapKnown, tappedShown;
        final ArenaMotion.Spring lean = new ArenaMotion.Spring(0f, 70, 0.02f);
        // light
        private int glow = GLOW_NONE, glowWas = GLOW_NONE;
        private boolean glowKnown;
        final ArenaMotion.Fade glowMix = new ArenaMotion.Fade(1f);
        // what changed on it: a pop of the stats, a red flash when it is hurt
        final ArenaMotion.Fade statsPop = new ArenaMotion.Fade(0f), hitFlash = new ArenaMotion.Fade(0f);
        private String statsSeen;
        private int damageSeen = -1;
        // the name, fitted to its lozenge once per name and width
        private String nameFor;
        private float nameWidth;
        private String nameShown;
        private FSkinFont nameFont;
        // the counters' chips, rebuilt when the counters change
        private int countersHash = Integer.MIN_VALUE;
        private final String[] chipText = new String[3];
        private final Color[] chipColor = {new Color(), new Color(), new Color()};
        private int chips;

        /** Set by the hand fan: how far the card leans (degrees, clockwise) and whether it is the raised one. */
        public float fanLean;
        public boolean raised;
        /** Set by the hand fan: the card's own height (the panel below it is the strip it rose from). */
        public float cardHeight;
        /** Set by a battlefield row: drawn this much larger (a fighter) and this many cards stand in its stack. */
        public float scale = 1f;
        public int groupCount;
        /** Behind another card of its stack: its name and stats are hidden under the one in front. */
        public boolean stackBack;

        /**
         * Glides from where the card was last seen to where the layout has put it (screen position {@code x, y}); a
         * card seen for the first time, or one that has been away (another zone, or not drawn for a while), just
         * appears. Call once per frame from draw.
         */
        public void follow(final float x, final float y, final Object area) {
            final long frame = ArenaMotion.frame();
            if (seenFrame >= frame - 3 && area == seenArea) {
                if (x != seenX) {
                    dx.shift(seenX - x);
                }
                if (y != seenY) {
                    dy.shift(seenY - y);
                }
            } else {
                dx.set(0f);
                dy.set(0f);
            }
            seenX = x;
            seenY = y;
            seenFrame = frame;
            seenArea = area;
        }

        /** Whether this card is still gliding into place. */
        public boolean isMoving() {
            return dx.isMoving() || dy.isMoving();
        }

        void noticeTapped(final boolean tapped) {
            if (!tapKnown) {
                tapKnown = true;
                tappedShown = tapped;
                tap.set(tapped ? 1f : 0f);
                return;
            }
            if (tapped != tappedShown) {
                tappedShown = tapped;
                // it swings over a little past the tilt, and just settles back when untapped
                tap.to(tapped ? 1f : 0f, tapped ? 240 : 190, 0, tapped ? ArenaMotion.BACK : ArenaMotion.OUT, null);
            }
        }

        void noticeGlow(final int now) {
            if (!glowKnown) {
                glowKnown = true;
                glow = now;
                return;
            }
            if (now != glow) {
                glowWas = glow;
                glow = now;
                glowMix.set(0f);
                glowMix.to(1f, 170);
            }
        }

        void noticeStats(final String stats, final int damage) {
            if (statsSeen != null && stats != null && !stats.equals(statsSeen)) {
                statsPop.set(1f);
                statsPop.to(0f, 420);
            }
            statsSeen = stats;
            if (damageSeen >= 0 && damage > damageSeen) {
                hitFlash.set(1f);
                hitFlash.to(0f, 520);
            }
            damageSeen = damage;
        }

        /** Forgets what it showed: the next frame shows the card as it is, without animating to it. */
        public void reset() {
            tapKnown = false;
            glowKnown = false;
            statsSeen = null;
            damageSeen = -1;
            seenFrame = Long.MIN_VALUE;
            dx.set(0f);
            dy.set(0f);
            lift.set(0f);
            lean.set(0f);
            raised = false;
        }
    }

    // ---- what a card can do now ------------------------------------------------------------------------------------

    /** The light a card on the board or in the hand should have now. */
    static int glowFor(final CardView card, final boolean hovered, final boolean inHand) {
        final MatchController mc = MatchController.instance;
        if (mc.isHighlighted(card)) {
            return GLOW_CHOSEN;
        }
        if (mc.isSelectable(card)) {
            return GLOW_SELECTABLE;
        }
        if (!inHand) {
            if (isStackTarget(card)) {
                return GLOW_TARGET;
            }
            if (card.isAttacking()) {
                return GLOW_ATTACK;
            }
            if (card.isBlocking()) {
                return GLOW_BLOCK;
            }
            if (isStackSource(card)) {
                return GLOW_SOURCE;
            }
        }
        if (mc.isActionable(card)) {
            return GLOW_PLAYABLE;
        }
        if (hovered) {
            return GLOW_HOVER;
        }
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_SHOW_AUTOTAP_PREVIEW) && mc.getActionableStrength(card) >= 2) {
            return GLOW_AUTOTAP;
        }
        return GLOW_NONE;
    }

    /** Whether what is on top of the stack (or one of its parts) aims at this card. */
    static boolean isStackTarget(final CardView card) {
        final GameView game = MatchController.instance.getGameView();
        StackItemView item = game == null ? null : game.peekStack();
        while (item != null) {
            final Iterable<CardView> targets = item.getTargetCards();
            if (targets != null) {
                for (final CardView t : targets) {
                    if (t != null && t.getId() == card.getId()) {
                        return true;
                    }
                }
            }
            item = item.getSubInstance();
        }
        return false;
    }

    /** Whether this card is what put the top of the stack there (an ability of a permanent). */
    static boolean isStackSource(final CardView card) {
        final GameView game = MatchController.instance.getGameView();
        final StackItemView item = game == null ? null : game.peekStack();
        final CardView source = item == null ? null : item.getSourceCard();
        return source != null && source.getId() == card.getId();
    }

    /** Whether the other army is in combat and this card is not part of it: it steps back into the shade. */
    static boolean isSidelined(final CardView card) {
        final GameView game = MatchController.instance.getGameView();
        final forge.game.combat.CombatView combat = game == null ? null : game.getCombat();
        return combat != null && combat.getNumAttackers() > 0 && !card.isAttacking() && !card.isBlocking()
                && !MatchController.instance.isSelectable(card) && !MatchController.instance.isHighlighted(card);
    }

    // ---- battlefield tiles -----------------------------------------------------------------------------------------

    /** Power/toughness, or loyalty; null when the card has neither (or cannot be seen). */
    static String tileStats(final CardView card, final boolean known) {
        if (!known) {
            return null;
        }
        final CardStateView state = card.getCurrentState();
        if (state.isCreature() || state.isVehicle()) {
            return state.getPower() + "/" + state.getToughness();
        }
        return state.isPlaneswalker() ? state.getLoyalty() : null;
    }

    /**
     * One battlefield tile in the box x, y, w, h (the panel's own coordinates), with its light, its tap and hover, and
     * everything written on it.
     */
    public static void drawTile(final Graphics g, final CardView card, final State st, final boolean hovered, final float bx, final float by,
            final float bw, final float bh) {
        final boolean known = MatchController.instance.mayView(card);
        final CardStateView state = card.getCurrentState();
        st.noticeTapped(card.isTapped());
        final int glow = glowFor(card, hovered, false);
        st.noticeGlow(glow);
        final String stats = tileStats(card, known);
        st.noticeStats(stats, card.getDamage());
        if (st.lift.goal() != (hovered ? 1f : 0f)) {
            st.lift.to(hovered ? 1f : 0f, hovered ? 110 : 170);
        }

        final float tapAmount = ArenaMotion.clamp01(st.tap.get());
        final float tapAngle = st.tap.get() * TAPPED_ANGLE; // past 1 while it overshoots
        final float grow = st.scale * (1f + HOVER_GROWTH * st.lift.get());
        final float w = bw * grow, h = bh * grow;
        final float x = bx + (bw - w) / 2f + st.dx.get(), y = by + (bh - h) / 2f + st.dy.get();
        final float cx = x + w / 2f, cy = y + h / 2f;
        final float arc = Math.max(ArenaTheme.px(6), Math.round(w * 0.08f));
        final float radius = arc / 2f;
        final float lip = Math.max(2f, Math.round(h * 0.025f));
        final float tall = h + lip;
        final float spread = Math.max(ArenaTheme.px(6), Math.round(w * 0.085f));

        final boolean turned = Math.abs(tapAngle) > 0.05f;
        if (turned) {
            g.startRotateTransform(cx, cy, -tapAngle);
        }
        // the shadow it casts down and away, so that it sits on the board
        final float drop = Math.max(ArenaTheme.px(4), h / 24f);
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.75f, S1), x, y + drop, w, tall, radius + drop, spread);

        // the light: what it was, fading, and what it is now, coming up (and breathing while it asks to be noticed)
        final float mix = st.glowMix.get();
        float breath = 1f;
        if (glow == GLOW_SELECTABLE) {
            breath = 0.72f + 0.28f * ArenaMotion.pulse(1.5f);
        }
        if (st.glowWas != GLOW_NONE && mix < 1f) {
            bloom(g, st.glowWas, x, y, w, tall, radius, spread, hovered, 1f - mix);
        }
        if (glow != GLOW_NONE) {
            bloom(g, glow, x, y, w, tall, radius, spread, hovered, mix * breath);
        }

        // the slab: near-black, thicker at the foot, its top edge catching the light
        g.fillSmoothRoundRect(SLAB_BASE, x, y + lip, w, h, radius);
        g.fillSmoothRoundRectGradient(SLAB_TOP, SLAB_BOTTOM, true, x, y, w, h, radius);
        STOPS3[0] = ArenaTheme.alpha(ArenaTheme.WHITE, 0.24f, S1);
        STOPS3[1] = ArenaTheme.alpha(ArenaTheme.WHITE, 0f, S2);
        STOPS3[2] = S2;
        g.drawArenaShape(Graphics.SHAPE_RING, x, y, w, h, radius, 1f, STOPS3, TOP_LIGHT, true);

        // the frame in the card's colour, the art set into it, the name above
        final Color tint = known ? ArenaTheme.frameLine(ArenaTheme.tileTint(state)) : UNKNOWN_TINT;
        final float edge = Math.max(ArenaTheme.px(3), Math.round(w * 0.026f));
        final float footer = stats != null ? Math.max(ArenaTheme.px(9), Math.round(h * 0.125f)) : edge;
        final float line = Math.max(2f, Math.round(w * 0.021f));
        final float gap = Math.max(2f, Math.round(w * 0.011f));
        final float fx = x + edge, fy = y + edge, fw = w - 2 * edge, fh = h - edge - footer;
        final boolean token = known && card.isToken();
        final float nameH = token ? 0f : Math.max(ArenaTheme.px(13), Math.round(h * 0.1f));
        final float frameRadius = Math.max(ArenaTheme.px(5), arc * 0.7f) / 2f;
        final Color lineColor;
        if (tapAmount > 0f) {
            ArenaTheme.mix(ArenaTheme.fade(tint), TAPPED_LINE, 0.55f, S3);
            lineColor = ArenaTheme.mix(tint, S3, tapAmount, S3);
        } else {
            lineColor = tint;
        }
        g.fillSmoothRoundRectGradient(lighter(lineColor, S4), darker(lineColor, S5), true, fx, fy, fw, fh, frameRadius);

        final float artTop = token ? fy + line : fy + gap + nameH + gap;
        final float ax = fx + line, aw = fw - 2 * line, ah = fy + fh - line - artTop;
        final float artRadius = Math.max(ArenaTheme.px(3), frameRadius * 2 - line) / 2f;
        if (aw > 0 && ah > 0) {
            drawArt(g, card, known, ax, artTop, aw, ah, artRadius, tapAmount);
            // the art is set into the frame, not laid on it
            g.drawSmoothRoundRect(Math.max(1.5f, ArenaTheme.px(2f)), ArenaTheme.alpha(ArenaTheme.BLACK, 0.47f, S1), ax, artTop, aw, ah, artRadius);
        }
        if (nameH > 0) {
            final float nx = fx + gap, ny = fy + gap, nw = fw - 2 * gap;
            final Color pale = ArenaTheme.mix(ArenaTheme.mix(tint, ArenaTheme.WHITE, 0.64f, S1), TAPPED_PALE, tapAmount, S1);
            g.fillSmoothRoundRectGradient(pale, ArenaTheme.mix(pale, ArenaTheme.BLACK, 0.1f, S2), true, nx, ny, nw, nameH, nameH * 0.31f);
            g.drawSmoothRoundRect(1f, NAME_EDGE, nx, ny, nw, nameH, nameH * 0.31f);
            if (known && !st.stackBack) {
                final float pad = Math.max(ArenaTheme.px(4), nameH / 3f);
                fitName(st, state, nw - 2 * pad, nameH);
                g.drawText(st.nameShown, st.nameFont, NAME_TEXT, nx + pad, ny, nw - 2 * pad, nameH, false, Align.left, true);
            }
        }

        if (tapAmount > 0f && aw > 0) {
            final float s = Math.min(aw, ah) * 0.62f;
            final float old = g.getfloatAlphaComposite();
            g.setAlphaComposite(old * 0.6f * tapAmount);
            CardFaceSymbols.drawSymbol("T", g, ax + (aw - s) / 2f, artTop + (ah - s) / 2f, s, s);
            g.setAlphaComposite(old);
        }

        // counters, as small chips along the bottom of the art
        if (known) {
            drawCounterChips(g, card, st, ax, artTop + ah, aw, h);
        }

        // power/toughness: a tinted plate in a pale rim, across the art's lower edge and the footer
        if (stats != null && !st.stackBack) {
            drawStatsPlate(g, card, state, st, stats, tint, tapAmount, x, y, w, h);
        }

        if (st.groupCount > 1) {
            final float d = Math.max(ArenaTheme.px(16), Math.round(h * 0.2f));
            ArenaArt.countBadge(g, "×" + st.groupCount, x + w - d * 0.35f, y + d * 0.35f, d);
        }

        if (isSidelined(card)) {
            g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.BLACK, 0.25f, S1), x, y, w, h, radius);
        }
        final float flash = st.hitFlash.get();
        if (flash > 0f) {
            g.fillSmoothRoundRect(ArenaTheme.alpha(HIT, 0.59f * flash, S1), x, y, w, h, radius);
        }

        // the rim in the light's colour, with a fine bright line along its inside
        if (st.glowWas != GLOW_NONE && mix < 1f) {
            rim(g, st.glowWas, x, y, w, tall, radius, 1f - mix);
        }
        if (glow != GLOW_NONE) {
            rim(g, glow, x, y, w, tall, radius, mix * (0.5f + 0.5f * breath));
        }
        if (turned) {
            g.endTransform();
        }
    }

    private static void bloom(final Graphics g, final int kind, final float x, final float y, final float w, final float h, final float radius,
            final float spread, final boolean hovered, final float alpha) {
        if (alpha <= 0f) {
            return;
        }
        final boolean fighting = kind == GLOW_ATTACK || kind == GLOW_BLOCK;
        final float reach = spread + (hovered ? ArenaTheme.px(2) : 0f) + (fighting ? ArenaTheme.px(4) : 0f);
        g.drawGlow(ArenaTheme.alpha(GLOW_COLOR[kind], GLOW_STRENGTH[kind] * alpha, S1), x, y, w, h, radius, reach);
    }

    private static void rim(final Graphics g, final int kind, final float x, final float y, final float w, final float h, final float radius,
            final float alpha) {
        if (alpha <= 0f) {
            return;
        }
        final Color c = GLOW_COLOR[kind];
        final boolean fighting = kind == GLOW_ATTACK || kind == GLOW_BLOCK;
        final float out = ArenaTheme.px(fighting ? 1.6f : 1.2f);
        final float t = Math.max(2f, ArenaTheme.px(fighting ? 3.2f : 3f));
        g.drawSmoothRoundRect(t, ArenaTheme.alpha(c, 0.98f * alpha, S1), x - out - t / 2f, y - out - t / 2f, w + 2 * out + t, h + 2 * out + t,
                radius + out + t / 2f);
        S2.set((c.r + 1f) / 2f, (c.g + 1f) / 2f, (c.b + 1f) / 2f, 0.92f * alpha);
        g.drawSmoothRoundRect(1f, S2, x - 0.5f, y - 0.5f, w + 1f, h + 1f, radius + 0.5f);
    }

    /** The art crop, drained of colour and light as the tile taps; the sleeve for a card that cannot be seen. */
    private static void drawArt(final Graphics g, final CardView card, final boolean known, final float x, final float y, final float w,
            final float h, final float radius, final float tapAmount) {
        TextureRegion art = null;
        if (known) {
            final FImageComplex complex = CardRenderer.getCardArt(card);
            if (complex != null) {
                art = complex.getTextureRegion();
            }
        }
        if (art == null) {
            if (!known) {
                drawCardBack(g, card.getOwner(), x, y, w, h, radius, false);
                return;
            }
            g.fillSmoothRoundRect(ArenaTheme.NAVY, x, y, w, h, radius);
            return;
        }
        // fill the art box, cropping what does not fit its shape
        final TextureRegion region = cropTo(art, w / h);
        final float a = g.getfloatAlphaComposite();
        if (tapAmount > 0f) {
            final float dim = 1f - 0.42f * tapAmount;
            g.setColorRGBA(dim, dim, dim, a);
        }
        g.drawTiltedImage(region, x, y, w, h, 0f, 0f, 0f, 1f, radius, 0f, 0.82f * tapAmount, 1f);
        if (tapAmount > 0f) {
            g.resetColorRGBA(a);
            g.setAlphaComposite(a);
        }
    }

    private static final TextureRegion CROP = new TextureRegion();

    /** The middle of {@code art} cut to the aspect {@code aspect} (width / height). Shared scratch region. */
    private static TextureRegion cropTo(final TextureRegion art, final float aspect) {
        final float rw = art.getRegionWidth(), rh = art.getRegionHeight();
        if (rw <= 0 || rh <= 0) {
            return art;
        }
        float cw = rw, ch = rh;
        if (rw / rh > aspect) {
            cw = rh * aspect;
        } else {
            ch = rw / aspect;
        }
        CROP.setTexture(art.getTexture());
        CROP.setRegion(art.getRegionX() + Math.round((rw - cw) / 2f), art.getRegionY() + Math.round((rh - ch) * 0.35f), Math.round(cw), Math.round(ch));
        return CROP;
    }

    private static Color lighter(final Color c, final Color out) {
        return out.set(Math.min(1f, c.r + 26 / 255f), Math.min(1f, c.g + 26 / 255f), Math.min(1f, c.b + 26 / 255f), c.a);
    }

    private static Color darker(final Color c, final Color out) {
        return out.set(c.r * 0.7f, c.g * 0.7f, c.b * 0.7f, c.a);
    }

    /** One size on every tile; a long name gives way a little, then ends in an ellipsis. Cached per name and width. */
    private static void fitName(final State st, final CardStateView state, final float maxW, final float nameH) {
        final String name = state.getName();
        if (name != null && name.equals(st.nameFor) && st.nameWidth == maxW && st.nameFont != null) {
            return;
        }
        st.nameFor = name;
        st.nameWidth = maxW;
        String shown = name == null ? "" : CardTranslation.getTranslatedName(name);
        FSkinFont font = evenFont(nameH * 0.74f);
        final FSkinFont smaller = evenFont(nameH * 0.64f);
        if (ArenaArt.textWidth(shown, font) > maxW) {
            font = smaller;
        }
        if (ArenaArt.textWidth(shown, font) > maxW) {
            while (shown.length() > 1 && ArenaArt.textWidth(shown + "…", font) > maxW) {
                shown = shown.substring(0, shown.length() - 1);
            }
            shown = shown.trim() + "…";
        }
        st.nameShown = shown;
        st.nameFont = font;
    }

    /** A font this many logical pixels high, rounded to an even size so a handful of sizes serve the whole board. */
    static FSkinFont evenFont(final float px) {
        return ArenaTheme.fontPx(Math.max(8, Math.round(px / 2f) * 2));
    }

    private static void drawStatsPlate(final Graphics g, final CardView card, final CardStateView state, final State st, final String stats,
            final Color tint, final float tapAmount, final float x, final float y, final float w, final float h) {
        final float row = Math.max(ArenaTheme.px(15), Math.round(h * 0.19f));
        final FSkinFont font = evenFont(row * 0.66f);
        final float textW = ArenaArt.textWidth(stats, font);
        float bw = Math.max(Math.round(w * 0.34f), textW + Math.round(row * 0.9f));
        float bh = row;
        float sx = x + w - bw - Math.round(w * 0.022f), sy = y + h - row - Math.round(h * 0.022f);
        final float popped = st.statsPop.get();
        if (popped > 0f) {
            // the plate jumps when what it says changes
            final float grow = 1f + 0.3f * popped;
            final float ncx = sx + bw / 2f, ncy = sy + bh / 2f;
            bw *= grow;
            bh *= grow;
            sx = ncx - bw / 2f;
            sy = ncy - bh / 2f;
        }
        final float r = bh / 4f;
        final float o = Math.max(1f, ArenaTheme.px(1.5f));
        g.fillSmoothRoundRect(PLATE_EDGE, sx - o, sy - o, bw + 2 * o, bh + 2 * o, r + o);
        g.fillSmoothRoundRectGradient(ArenaTheme.mix(RIM_TOP, RIM_TOP_TAPPED, tapAmount, S1), ArenaTheme.mix(RIM_BOTTOM, RIM_BOTTOM_TAPPED, tapAmount, S2),
                true, sx, sy, bw, bh, r);
        final float rim = Math.max(2f, Math.round(bh * 0.09f));
        ArenaTheme.mix(ArenaTheme.mix(PLATE_TOP, tint, 0.17f, S3), PLATE_TAPPED_TOP, tapAmount, S3);
        ArenaTheme.mix(ArenaTheme.mix(PLATE_BOTTOM, tint, 0.27f, S4), PLATE_TAPPED_BOTTOM, tapAmount, S4);
        S3.a = 1f;
        S4.a = 1f;
        g.fillSmoothRoundRectGradient(S3, S4, true, sx + rim, sy + rim, bw - 2 * rim, bh - 2 * rim, Math.max(1f, r - rim));
        g.drawSmoothRoundRect(1f, PLATE_LINE, sx + rim, sy + rim, bw - 2 * rim, bh - 2 * rim, Math.max(1f, r - rim));
        final Color text = tapAmount >= 1f ? STATS_TAPPED : statsColor(card, state);
        final FSkinFont shown = popped > 0f ? evenFont(bh * 0.66f) : font;
        ArenaArt.textBold(g, stats, shown, text, sx, sy, bw, bh, Align.center);
    }

    /** Dark type; blue when above the printed values, red when hurt. */
    private static Color statsColor(final CardView card, final CardStateView state) {
        if (card.getDamage() > 0) {
            return STATS_DAMAGED;
        }
        if (state.isCreature() && !card.isToken()) {
            final PaperCard printed = FModel.getMagicDb().getCommonCards().getCard(state.getName());
            if (printed != null && printed.getRules() != null) {
                if (state.getPower() > printed.getRules().getIntPower() || state.getToughness() > printed.getRules().getIntToughness()) {
                    return STATS_BUFFED;
                }
            }
        }
        return STATS_PLAIN;
    }

    private static void drawCounterChips(final Graphics g, final CardView card, final State st, final float ax, final float artBottom,
            final float aw, final float h) {
        final Multiset<CounterType> counters = card.getCounters();
        final int hash = counters == null ? 0 : counters.hashCode();
        if (hash != st.countersHash) {
            st.countersHash = hash;
            st.chips = 0;
            if (counters != null) {
                for (final Multiset.Entry<CounterType> e : counters.entrySet()) {
                    if (st.chips >= st.chipText.length) {
                        break;
                    }
                    final CounterType type = e.getElement();
                    st.chipText[st.chips] = e.getCount() + "×" + type.getCounterOnCardDisplayName();
                    st.chipColor[st.chips].set(type.getRed() / 255f, type.getGreen() / 255f, type.getBlue() / 255f, 1f);
                    st.chips++;
                }
            }
        }
        if (st.chips == 0) {
            return;
        }
        final float ch = Math.max(ArenaTheme.px(12), Math.round(h * 0.12f));
        final FSkinFont font = evenFont(ch * 0.72f);
        float cx = ax + ArenaTheme.px(3);
        final float cy = artBottom - ch - ArenaTheme.px(3);
        for (int i = 0; i < st.chips; i++) {
            final String t = st.chipText[i];
            final float cw = Math.min(aw * 0.6f, ArenaArt.textWidth(t, font) + ch * 0.8f);
            g.fillSmoothRoundRect(CHIP, cx, cy - i * (ch + 2), cw, ch, ch / 2f);
            g.drawSmoothRoundRect(1f, CHIP_EDGE, cx, cy - i * (ch + 2), cw, ch, ch / 2f);
            g.drawText(t, font, st.chipColor[i], cx, cy - i * (ch + 2), cw, ch, false, Align.center, true);
        }
    }

    // ---- hand cards ------------------------------------------------------------------------------------------------

    /**
     * A card in the local hand: the whole card, leaning in the fan, with a shadow and the Arena light (what can be
     * played breathes slowly). x, y, w, h is where the fan put the card (the panel's coordinates).
     */
    public static void drawHandCard(final Graphics g, final CardView card, final State st, final boolean hovered, final float bx, final float by,
            final float bw, final float bh) {
        final int glow = glowFor(card, false, true);
        st.noticeGlow(glow);
        if (st.lift.goal() != (st.raised ? 1f : 0f)) {
            st.lift.to(st.raised ? 1f : 0f, st.raised ? 110 : 170);
        }
        st.lean.to(st.fanLean);
        final float grow = 1f + ArenaBoard.FAN_HOVER_GROWTH * st.lift.get();
        final float w = bw * grow, h = bh * grow;
        final float x = bx + (bw - w) / 2f + st.dx.get(), y = by + (bh - h) + st.dy.get();
        final float radius = w * 0.045f;
        final float lean = st.lean.get();
        final boolean turned = Math.abs(lean) > 0.05f;
        if (turned) {
            g.startRotateTransform(x + w / 2f, y + h, -lean);
        }
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.7f, S1), x - h * 0.012f, y + h * 0.015f, w, h, radius, Math.max(ArenaTheme.px(6), h * 0.035f));
        final float mix = st.glowMix.get();
        float breath = 1f;
        if (glow == GLOW_SELECTABLE) {
            breath = 0.72f + 0.28f * ArenaMotion.pulse(1.5f);
        } else if (glow == GLOW_PLAYABLE && !st.raised) {
            breath = 0.8f + 0.2f * ArenaMotion.pulse(2.4f);
        }
        final float spread = Math.max(ArenaTheme.px(6), Math.round(w * 0.07f));
        if (st.glowWas != GLOW_NONE && mix < 1f) {
            bloom(g, st.glowWas, x, y, w, h, radius, spread, false, 1f - mix);
        }
        if (glow != GLOW_NONE) {
            bloom(g, glow, x, y, w, h, radius, spread, st.raised, mix * breath);
        }
        CardRenderer.drawCard(g, card, x, y, w, h, CardStackPosition.Top);
        if (st.glowWas != GLOW_NONE && mix < 1f) {
            rim(g, st.glowWas, x, y, w, h, radius, 1f - mix);
        }
        if (glow != GLOW_NONE) {
            rim(g, glow, x, y, w, h, radius, mix * (0.5f + 0.5f * breath));
        } else {
            g.drawSmoothRoundRect(1f, ArenaTheme.alpha(ArenaTheme.WHITE, 0.25f, S1), x, y, w, h, radius);
        }
        if (turned) {
            g.endTransform();
        }
    }

    // ---- card backs ------------------------------------------------------------------------------------------------

    /** The back of a card: the owner's sleeve, or (for the default sleeve, a small logo that does not enlarge well) an indigo field in a bronze border with a compass star. */
    public static void drawCardBack(final Graphics g, final PlayerView owner, final float x, final float y, final float w, final float h,
            final float radius, final boolean dim) {
        final FImage sleeve = owner == null || owner.getSleeveIndex() == 0 ? null : MatchController.getPlayerSleeve(owner);
        if (sleeve != null) {
            g.drawImage(sleeve, x, y, w, h);
        } else {
            g.fillSmoothRoundRectGradient(ArenaTheme.rgbaInto(40, 34, 92, 255, S1), BACK_FIELD, true, x, y, w, h, radius);
            final float inset = Math.max(2f, w * 0.07f);
            g.drawSmoothRoundRect(Math.max(1f, w * 0.03f), BACK_EDGE, x + inset, y + inset, w - 2 * inset, h - 2 * inset, Math.max(1f, radius - inset / 2f));
            // the compass of the floor medallion: a ring crossed by the four points (no rotation: this may be drawn
            // inside a turned tile, and Graphics transforms do not nest)
            final float cx = x + w / 2f, cy = y + h / 2f, s = Math.min(w, h) * 0.3f;
            final Color gold = ArenaTheme.alpha(ArenaTheme.GOLD, 0.85f, S2);
            g.drawSmoothCircle(Math.max(1f, s * 0.08f), gold, cx, cy, s * 0.62f);
            g.fillSmoothRoundRect(gold, cx - s * 0.07f, cy - s, s * 0.14f, 2 * s, s * 0.07f);
            g.fillSmoothRoundRect(gold, cx - s, cy - s * 0.07f, 2 * s, s * 0.14f, s * 0.07f);
            g.fillSmoothCircle(ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.9f, S3), cx, cy, s * 0.2f);
        }
        if (dim) {
            g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.BLACK, 0.43f, S4), x, y, w, h, radius);
        }
        g.drawSmoothRoundRect(1f, ArenaTheme.alpha(ArenaTheme.BLACK, 0.6f, S4), x, y, w, h, radius);
    }
}
