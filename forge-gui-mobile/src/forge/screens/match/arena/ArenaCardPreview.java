package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.Multiset;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardFaceSymbols;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.mana.ManaCost;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.gui.card.CardDetailUtil;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;

/**
 * The Arena hover preview (port of the Swing {@code ArenaCardPreview}): a large copy of the card under the pointer.
 * A card in the hand rises in place over its own slot; a card anywhere else shows at the side of the window away from
 * it. Beside it a glass panel gives all of the card as it stands now: name and mana cost, type line, power and
 * toughness (or loyalty, defence), state (counters, tapped, summoning sick, in combat, phased out, face down, damage)
 * and its whole rules text, with whatever it has been given. Where its power or toughness differ from the printed
 * ones, a plate with the current values covers the printed box.
 *
 * <p>Two ways to use it, both from {@code draw()} on the render thread:</p>
 * <ul>
 * <li>{@link #drawHover}: call it every frame with the hovered card (or null); it places itself, works out its panel,
 *     and fades in (120 ms) and out (90 ms) by itself.</li>
 * <li>{@link #place} and {@link #draw} for a caller that places and fades the preview itself.</li>
 * </ul>
 */
public final class ArenaCardPreview {
    private ArenaCardPreview() { }

    /** The preview's height, as a share of the window's (Swing HEIGHT_OF_WINDOW). */
    public static final float HEIGHT_OF_WINDOW = 0.52f;
    // Swing pixel sizes
    // Swing pixel sizes (the panel a little wider and its type a little larger than Swing's, for the GPU's lighter font)
    private static final float GAP = 12f, PANEL_W = 400f, PAD = 14f;
    private static final int FADE_IN_MS = 120, FADE_OUT_MS = 90;

    private static final Color PANEL_FILL = ArenaTheme.rgba(10, 12, 18, 240);
    private static final Color PANEL_RIM = ArenaTheme.rgba(255, 255, 255, 60);
    private static final Color CHIP_FILL = ArenaTheme.rgb(0x282C36);
    private static final Color PLATE_EDGE = ArenaTheme.rgb(0x040406);
    private static final Color PLATE_RIM_TOP = ArenaTheme.rgb(0xFFFFFF), PLATE_RIM_BOTTOM = ArenaTheme.rgb(0xA8A49A);
    private static final Color PLATE_TOP = ArenaTheme.rgb(0xFBF8EF), PLATE_BOTTOM = ArenaTheme.rgb(0xE0DACC);
    private static final Color STATS_WOUNDED = ArenaTheme.rgb(0xB3261E), STATS_UP = ArenaTheme.rgb(0x1450A8),
            STATS_DOWN = ArenaTheme.rgb(0x8A1C14);
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color();
    private static final Rectangle CARD = new Rectangle();
    /** Its own text renderer: it keeps the laid-out text from frame to frame as long as the text is the same. */
    private static final TextRenderer BODY = new TextRenderer();

    // ---- geometry ---------------------------------------------------------------------------------------------------

    /** How tall the preview is in a window this tall. */
    public static float preferredHeight(final float screenH) {
        return Math.round(screenH * HEIGHT_OF_WINDOW);
    }

    /** How wide the preview's card is in a window this tall. */
    public static float preferredWidth(final float screenH) {
        return Math.round(preferredHeight(screenH) / FCardPanel.ASPECT_RATIO);
    }

    /** The space kept round the preview and between it and its panel. */
    public static float gap() {
        return Math.max(4f, ArenaTheme.px(GAP));
    }

    /**
     * Where the preview of a card at {@code anchor} (screen coordinates) goes, as Swing places it: a card in the hand
     * rises over its own slot, its foot just above the bottom of the window; a card anywhere else shows at the side of
     * the window away from it, 14% of the height from the top. {@code panelW} is the width of a status panel beside the
     * card (0 for none), which goes on the side facing the middle of the window. The card's rectangle goes into
     * {@code out}; returns whether the panel goes on its left.
     */
    public static boolean place(final Rectangle anchor, final boolean inHand, final float panelW, final float screenW, final float screenH,
            final Rectangle out) {
        final float h = preferredHeight(screenH), w = preferredWidth(screenH), gap = gap();
        final float panel = panelW > 0f ? panelW + gap : 0f;
        final float blockW = w + panel;
        float x, y;
        boolean panelLeft = false;
        if (inHand) {
            // the card rises over its own slot; the panel goes on its left, or its right when there is no room there
            x = anchor.x + anchor.width / 2f - w / 2f;
            y = screenH - h - gap;
            x = Math.max(gap, Math.min(screenW - w - gap, x));
            panelLeft = panel > 0f && x - panel >= gap;
            out.set(Math.round(x), Math.round(y), w, h);
            return panelLeft;
        } else {
            final boolean onRight = anchor.x + anchor.width / 2f > screenW * 0.6f;
            panelLeft = !onRight;
            x = onRight ? gap : screenW - blockW - gap;
            y = Math.round(screenH * 0.14f);
        }
        x = Math.max(gap, Math.min(screenW - blockW - gap, x));
        y = Math.max(gap, Math.min(screenH - h - gap, y));
        out.set(Math.round(panelLeft ? x + panel : x), Math.round(y), w, h);
        return panelLeft;
    }

    // ---- the card ---------------------------------------------------------------------------------------------------

    /**
     * The preview's card at x, y, w, h: a soft shadow cast below it, the cyan light when it can be played now, the card
     * itself and, when its power or toughness are not the printed ones, a plate with the current values over the
     * printed box. {@code alpha} fades all of it.
     */
    public static void draw(final Graphics g, final CardView card, final float x, final float y, final float w, final float h, final float alpha) {
        if (card == null || alpha <= 0f || w <= 0 || h <= 0) {
            return;
        }
        final float old = g.getfloatAlphaComposite();
        if (alpha < 1f) {
            g.setAlphaComposite(old * alpha);
        }
        try {
            final float r = w * 0.035f, gap = gap();
            g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.94f, S1), x, y + gap * 0.5f, w, h, r, gap);
            if (MatchController.instance.isActionable(card)) {
                g.drawGlow(ArenaTheme.alpha(ArenaTheme.PLAYABLE, 0.9f, S1), x, y, w, h, r, gap * 0.85f);
            }
            // a hand card risen over its slot tilts towards the pointer, in perspective, catching the light
            final boolean underPointer = inHand && card == ArenaCardPreview.card;
            if (underPointer) {
                TILT_X.to((0.5f - ArenaMotion.clamp01((Forge.mouseMovedY - y) / h)) * 2f * ArenaCards.MAX_TILT);
                TILT_Y.to((ArenaMotion.clamp01((Forge.mouseMovedX - x) / w) - 0.5f) * 2f * ArenaCards.MAX_TILT);
            } else {
                TILT_X.to(0f);
                TILT_Y.to(0f);
            }
            final float tx = TILT_X.get(), ty = TILT_Y.get();
            final com.badlogic.gdx.graphics.g2d.TextureRegion face = Math.abs(tx) + Math.abs(ty) > 0.05f ? ArenaCards.faceOf(card) : null;
            if (face != null) {
                g.drawTiltedImage(face, x, y, w, h, tx, ty, 0f, 1f, r, 0.7f, 0f, 1f);
            } else {
                CardRenderer.drawCard(g, card, x, y, w, h, CardStackPosition.Top, false, false, true);
                g.drawSmoothRoundRect(1f, ArenaTheme.alpha(ArenaTheme.BLACK, 0.78f, S1), x, y, w, h, r);
            }
            drawStatsNow(g, card, x, y, w, h);
        } finally {
            if (alpha < 1f) {
                g.setAlphaComposite(old);
            }
        }
    }

    private static final ArenaMotion.Spring TILT_X = new ArenaMotion.Spring(0f, 70, 0.05f), TILT_Y = new ArenaMotion.Spring(0f, 70, 0.05f);

    // what the stats plate says, worked out again only when the card or its numbers change
    private static CardView statsOf;
    private static int statsPower = Integer.MIN_VALUE, statsToughness, statsDamage;
    private static String statsNow;
    private static Color statsColour;

    private static void drawStatsNow(final Graphics g, final CardView card, final float x, final float y, final float w, final float h) {
        final CardStateView state = card.getCurrentState();
        if (state == null || !state.isCreature() || card.isFaceDown()) {
            return;
        }
        if (statsOf != card || statsPower != state.getPower() || statsToughness != state.getToughness() || statsDamage != card.getDamage()) {
            statsOf = card;
            statsPower = state.getPower();
            statsToughness = state.getToughness();
            statsDamage = card.getDamage();
            statsNow = null;
            final int[] printed = printedStats(state);
            final int basePower = printed[0], baseToughness = printed[1];
            if (statsPower != basePower || statsToughness != baseToughness || statsDamage > 0) {
                statsNow = statsPower + "/" + statsToughness;
                statsColour = statsDamage > 0 ? STATS_WOUNDED
                        : statsPower >= basePower && statsToughness >= baseToughness ? STATS_UP : STATS_DOWN;
            }
        }
        if (statsNow == null) {
            return;
        }
        // large enough to hide the printed box entirely
        final float bw = Math.round(w * 0.235f), bh = Math.round(h * 0.066f);
        final float bx = x + w - bw - Math.round(w * 0.032f), by = y + h - bh - Math.round(h * 0.036f);
        final float edge = Math.max(1.5f, ArenaTheme.px(2));
        g.fillSmoothRoundRect(PLATE_EDGE, bx - edge, by - edge, bw + 2 * edge, bh + 2 * edge, (bh + 2 * edge) / 2f);
        g.fillSmoothRoundRectGradient(PLATE_RIM_TOP, PLATE_RIM_BOTTOM, true, bx, by, bw, bh, bh / 2f);
        final float rim = Math.max(2f, Math.round(bh * 0.09f));
        g.fillSmoothRoundRectGradient(PLATE_TOP, PLATE_BOTTOM, true, bx + rim, by + rim, bw - 2 * rim, bh - 2 * rim, (bh - 2 * rim) / 2f);
        final FSkinFont font = ArenaTheme.fontPx(Math.round(bh * 0.68f));
        ArenaArt.textBold(g, statsNow, font, statsColour, bx, by, bw, bh, Align.center);
    }

    // ---- the status panel ---------------------------------------------------------------------------------------------

    /**
     * What the details panel says: the card's name and mana cost, its type line, its current power and toughness (or
     * loyalty, defence), chips of state (counters, tapped, summoning sick, in combat, damage...), then its whole rules
     * text as it stands now, with whatever it has been given. Worked out off draw().
     */
    private static final class Status {
        final String name, type, stats;
        final ManaCost cost;
        final Color statsColor;
        final List<String> chips = new ArrayList<>();
        final float[] chipW;
        final String body;
        float width, height, chipH, nameH, lineH, costSize;
        FSkinFont nameFont, lineFont, chipFont, bodyFont;
        int rows;

        Status(final String name0, final ManaCost cost0, final String type0, final String stats0, final Color statsColor0,
                final List<String> chips0, final String body0, final float maxH) {
            name = name0 == null || name0.isEmpty() ? null : name0;
            cost = cost0 == null || cost0.isNoCost() || cost0.getGlyphCount() == 0 ? null : cost0;
            type = type0 == null || type0.isEmpty() ? null : type0;
            stats = stats0 == null || stats0.isEmpty() ? null : stats0;
            statsColor = statsColor0;
            chips.addAll(chips0);
            chipW = new float[chips.size()];
            body = body0 == null || body0.isEmpty() ? null : body0;
            // the text gets smaller before any of it is cut off
            for (final float size : TEXT_SIZES) {
                layout(maxH, size);
                if (height < maxH) {
                    break;
                }
            }
        }

        private void layout(final float maxH, final float size) {
            final float pad = ArenaTheme.px(PAD);
            width = ArenaTheme.px(PANEL_W);
            nameFont = ArenaTheme.font(size + 4f);
            lineFont = ArenaTheme.font(size);
            chipFont = lineFont;
            bodyFont = lineFont;
            nameH = nameFont.getLineHeight();
            lineH = lineFont.getLineHeight();
            costSize = Math.round(nameH * 0.8f);
            chipH = chipFont.getLineHeight() + ArenaTheme.px(4);
            final float room = width - 2 * pad;
            for (int i = 0; i < chips.size(); i++) {
                chipW[i] = Math.min(room, ArenaArt.textWidth(chips.get(i), chipFont) + ArenaTheme.px(14));
            }
            rows = 0;
            float cx = 0f;
            for (int i = 0; i < chips.size(); i++) {
                if (rows == 0 || cx + chipW[i] > room) {
                    rows++;
                    cx = 0f;
                }
                cx += chipW[i] + ArenaTheme.px(4);
            }
            float h = pad;
            if (name != null || cost != null) {
                h += nameH;
            }
            if (type != null) {
                h += lineH;
            }
            if (stats != null) {
                h += lineH;
            }
            if (rows > 0) {
                h += ArenaTheme.px(8) + rows * (chipH + ArenaTheme.px(4)) - ArenaTheme.px(4);
            }
            if (body != null) {
                h += ArenaTheme.px(10) + BODY.getWrappedBounds(body, bodyFont, room).height;
            }
            height = Math.min(maxH, h + pad + ArenaTheme.px(6)); // a little slack: wrapped text measures a hair short
        }
    }

    /** Text sizes (Swing points) the details panel tries, largest first: a few, since each new size builds a font. */
    private static final float[] TEXT_SIZES = {21f, 18f, 15f};
    private static final Color STATS_UP_LIGHT = ArenaTheme.rgb(0x8EC5FF), STATS_DOWN_LIGHT = ArenaTheme.rgb(0xFF8A7E);

    /** Everything the details panel says about a card (null when there is nothing to say). Allocates: not for every frame. */
    private static Status describe(final CardView cv, final float maxH) {
        final CardStateView state = cv.getCurrentState();
        if (state == null) {
            return null;
        }
        final List<String> chips = new ArrayList<>();
        final Multiset<CounterType> counters = cv.getCounters();
        if (counters != null) {
            for (final Multiset.Entry<CounterType> e : counters.entrySet()) {
                chips.add(e.getCount() + "x " + e.getElement().getCounterOnCardDisplayName());
            }
        }
        if (cv.isFaceDown()) { chips.add(Forge.getLocalizer().getMessage("lblArenaFaceDown")); }
        if (cv.isTapped()) { chips.add(Forge.getLocalizer().getMessage("lblArenaTapped")); }
        if (cv.isSick() && !state.hasKeyword(forge.game.keyword.Keyword.HASTE)) { chips.add(Forge.getLocalizer().getMessage("lblArenaSummoningSick")); }
        if (cv.isAttacking()) { chips.add(Forge.getLocalizer().getMessage("lblArenaAttacking")); }
        if (cv.isBlocking()) { chips.add(Forge.getLocalizer().getMessage("lblArenaBlocking")); }
        if (cv.isPhasedOut()) { chips.add(Forge.getLocalizer().getMessage("lblArenaPhasedOut")); }
        if (cv.getDamage() > 0) { chips.add(Forge.getLocalizer().getMessage("lblArenaDamage", cv.getDamage())); }

        String name = null, type = null, stats = null, body = null;
        ManaCost cost = null;
        Color statsColor = ArenaTheme.TEXT;
        if (MatchController.instance.mayView(cv)) {
            name = CardDetailUtil.formatCardName(cv, true, false);
            cost = cv.isFaceDown() ? null : state.getManaCost();
            type = CardDetailUtil.formatCardType(state, true);
            stats = CardDetailUtil.formatPrimaryCharacteristic(state, true);
            if (state.isCreature() && !cv.isFaceDown()) {
                final int[] printed = printedStats(state);
                if (cv.getDamage() > 0) {
                    statsColor = STATS_DOWN_LIGHT;
                } else if (state.getPower() != printed[0] || state.getToughness() != printed[1]) {
                    statsColor = state.getPower() >= printed[0] && state.getToughness() >= printed[1] ? STATS_UP_LIGHT : STATS_DOWN_LIGHT;
                }
            }
            try {
                final String composed = CardDetailUtil.composeCardText(state, MatchController.instance.getGameView(), true);
                if (composed != null) {
                    final StringBuilder text = new StringBuilder();
                    for (final String line : composed.split("\\r?\\n")) {
                        final String trimmed = line.trim();
                        // counters are already in the chips
                        if (!trimmed.isEmpty() && !trimmed.contains(" counters:")) {
                            text.append(text.length() == 0 ? "" : "\n").append(trimmed);
                        }
                    }
                    body = text.toString();
                }
            } catch (final RuntimeException e) {
                // a view the details cannot be worked out for: the rest still shows
            }
        }
        if (name == null && chips.isEmpty()) {
            return null;
        }
        return new Status(name, cost, type, stats, statsColor, chips, body, maxH);
    }

    /** The printed power and toughness of a card (its current ones for a card the database does not know, like a token). */
    private static int[] printedStats(final CardStateView state) {
        int power = state.getPower(), toughness = state.getToughness();
        try {
            final PaperCard printed = FModel.getMagicDb().getCommonCards().getCard(state.getName());
            if (printed != null && printed.getRules() != null) {
                power = printed.getRules().getIntPower();
                toughness = printed.getRules().getIntToughness();
            }
        } catch (final RuntimeException e) {
            // not in the database
        }
        return new int[] {power, toughness};
    }

    /** The details panel on glass: name and cost, type, stats, chips of state, then the rules text. {@code x}, {@code y}: its top left. */
    private static void drawStatus(final Graphics g, final Status s, final float x, final float y, final float alpha) {
        final float pad = ArenaTheme.px(PAD), r = ArenaTheme.px(6);
        final float old = g.getfloatAlphaComposite();
        if (alpha < 1f) {
            g.setAlphaComposite(old * alpha);
        }
        try {
            g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.6f, S1), x, y + ArenaTheme.px(2), s.width, s.height, r, ArenaTheme.px(8));
            g.fillSmoothRoundRect(PANEL_FILL, x, y, s.width, s.height, r);
            g.drawSmoothRoundRect(1f, PANEL_RIM, x, y, s.width, s.height, r);
            final float room = s.width - 2 * pad;
            final float bottom = y + s.height - pad;
            float cy = y + pad;
            if (s.name != null || s.cost != null) {
                float costW = 0f;
                if (s.cost != null) {
                    costW = Math.min(room * 0.5f, CardFaceSymbols.getWidth(s.cost, s.costSize));
                    final float size = Math.min(s.costSize, costW / s.cost.getGlyphCount());
                    CardFaceSymbols.drawManaCost(g, s.cost, x + pad + room - costW, cy + (s.nameH - size) / 2f, size);
                }
                if (s.name != null) {
                    ArenaArt.textBold(g, s.name, s.nameFont, ArenaTheme.WHITE, x + pad, cy, room - costW - ArenaTheme.px(6), s.nameH, Align.left);
                }
                cy += s.nameH;
            }
            if (s.type != null) {
                ArenaArt.text(g, s.type, s.lineFont, ArenaTheme.TEXT_DIM, x + pad, cy, room, s.lineH, Align.left);
                cy += s.lineH;
            }
            if (s.stats != null) {
                ArenaArt.textBold(g, s.stats, s.lineFont, s.statsColor, x + pad, cy, room, s.lineH, Align.left);
                cy += s.lineH;
            }
            if (s.rows > 0) {
                cy += ArenaTheme.px(8);
                float cx = x + pad;
                boolean first = true;
                for (int i = 0; i < s.chips.size(); i++) {
                    if (!first && cx + s.chipW[i] > x + s.width - pad) {
                        cx = x + pad;
                        cy += s.chipH + ArenaTheme.px(4);
                    }
                    first = false;
                    g.fillSmoothRoundRect(CHIP_FILL, cx, cy, s.chipW[i], s.chipH, ArenaTheme.px(3));
                    ArenaArt.text(g, s.chips.get(i), s.chipFont, ArenaTheme.WHITE, cx, cy, s.chipW[i], s.chipH, Align.center);
                    cx += s.chipW[i] + ArenaTheme.px(4);
                }
                cy += s.chipH;
            }
            if (s.body != null) {
                cy += ArenaTheme.px(10);
                if (bottom > cy) {
                    g.startClip(x, cy, s.width, bottom - cy + ArenaTheme.px(2));
                    BODY.drawText(g, s.body, s.bodyFont, ArenaTheme.TEXT, x + pad, cy, room, bottom - cy, cy, bottom - cy, true, Align.left, false);
                    g.endClip();
                }
            }
        } finally {
            if (alpha < 1f) {
                g.setAlphaComposite(old);
            }
        }
    }

    // ---- the hover preview, run here ----------------------------------------------------------------------------------

    private static final ArenaMotion.Fade shown = new ArenaMotion.Fade(0f);
    private static CardView card;
    private static final Rectangle anchor = new Rectangle();
    private static boolean inHand;
    private static Status status;
    private static boolean statusKnown;
    private static long statusFrame;
    private static float statusMaxH;
    /** How many frames a status is kept before it is worked out again, so a card changing under the pointer shows it. */
    private static final int STATUS_FRAMES = 20;

    /**
     * The whole hover preview, for {@code drawOverlay} of the match screen: call it every frame with the card under the
     * pointer and its rectangle on screen ({@code inHand} for cards in the hand), or with null when there is none. It
     * fades in when a card comes, goes straight over to another card, and fades out once there is none.
     */
    public static void drawHover(final Graphics g, final CardView hovered, final Rectangle hoveredAt, final boolean hoveredInHand,
            final float screenW, final float screenH) {
        if (hovered != null && hoveredAt != null) {
            if (hovered != card) {
                card = hovered;
                statusKnown = false;
            }
            anchor.set(hoveredAt);
            inHand = hoveredInHand;
            if (shown.goal() != 1f) {
                shown.to(1f, FADE_IN_MS);
            }
        } else if (card != null && shown.goal() != 0f) {
            shown.to(0f, FADE_OUT_MS, ArenaCardPreview::forget);
        }
        if (card == null) {
            return;
        }
        final float a = shown.get();
        if (a <= 0f) {
            return;
        }
        final float h = preferredHeight(screenH);
        if (!statusKnown || statusMaxH != h || ArenaMotion.frame() - statusFrame > STATUS_FRAMES) {
            status = describe(card, h);
            statusKnown = true;
            statusMaxH = h;
            statusFrame = ArenaMotion.frame();
        }
        final boolean panelLeft = place(anchor, inHand, status == null ? 0f : status.width, screenW, screenH, CARD);
        // it rises a few pixels into place as it fades in
        final float rise = Math.round((1f - a) * (gap() - 2f));
        draw(g, card, CARD.x, CARD.y + rise, CARD.width, CARD.height, a);
        if (status != null) {
            final float px = panelLeft ? CARD.x - ArenaTheme.px(4) - status.width : CARD.x + CARD.width + gap();
            drawStatus(g, status, px, CARD.y + rise, a);
        }
    }

    /** Whether a preview is showing (or fading). */
    public static boolean isShowing() {
        return card != null && shown.get() > 0f;
    }

    /** Takes the preview down at once. */
    public static void clear() {
        shown.set(0f);
        forget();
    }

    private static void forget() {
        card = null;
        status = null;
        statusKnown = false;
    }
}
