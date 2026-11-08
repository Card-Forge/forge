package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.gui.FThreads;
import forge.toolbox.FCardPanel;

/**
 * What every Arena dialog shares (port of the Swing {@code ArenaDialogKit}), so all of them have one backdrop, one
 * heading, one button slot and one way of drawing a card: the dimmed board behind, the title anchored near the top,
 * the row language of cards with a soft glow, cast shadow and lit rim, and the small controls (badges, rails, slots,
 * round buttons, the remember box). All sizes are fractions of the window, as in Swing, so the dialogs look the same
 * at any resolution; everything is drawn on the GPU each frame (no cached pictures).
 *
 * <p>Drawing helpers run on the render thread inside {@code draw()} and allocate nothing; layout helpers (wrapping,
 * the heading's lines) allocate and belong in layout code.</p>
 */
public final class ArenaDialogKit {
    private ArenaDialogKit() { }

    /** The saturated rim of anything that can be picked, and the bloom it throws. */
    public static final Color CYAN = ArenaTheme.rgb(0x19C8FF);
    /** The thin bright core down the middle of that rim. */
    public static final Color CYAN_CORE = ArenaTheme.rgb(0xD2F6FF);
    /** The rim of what has been picked, and its bloom. */
    public static final Color PICKED = ArenaTheme.rgb(0xFFB52E);
    /** The thin bright core down the middle of that rim. */
    public static final Color PICKED_CORE = ArenaTheme.rgb(0xFFF2CC);
    private static final Color PLAIN_RIM = ArenaTheme.rgb(0xDDE6F2);
    private static final Color SOURCE_BLOOM = ArenaTheme.rgb(0x9CC4F0);
    private static final Color CARD_EDGE = ArenaTheme.rgba(236, 242, 250, 165);
    private static final Color TAB_TOP = ArenaTheme.rgb(0x2B2D33), TAB_BOTTOM = ArenaTheme.rgb(0x0E0F13);
    private static final Color BADGE_TOP = ArenaTheme.rgb(0x2A2C33), BADGE_BOTTOM = ArenaTheme.rgb(0x0C0D11);
    private static final Color ROUND_TOP = ArenaTheme.rgb(0x1F1C18), ROUND_BOTTOM = ArenaTheme.rgb(0x0A0908);
    private static final Color ROUND_LIT_TOP = ArenaTheme.rgb(0x3A332A), ROUND_LIT_BOTTOM = ArenaTheme.rgb(0x1E1A15);
    private static final Color CHECK_TOP = ArenaTheme.rgb(0x0C0B09), CHECK_BOTTOM = ArenaTheme.rgb(0x1E1B16);
    private static final Color BACKDROP = ArenaTheme.rgb(0x040507);
    private static final Color PILL_SHADOW = ArenaTheme.rgba(60, 22, 0, 190), PILL_SHADOW_SOFT = ArenaTheme.rgba(60, 22, 0, 150);
    private static final Color PILL_OFF_TEXT = ArenaTheme.rgba(255, 255, 255, 178);

    /** The corner radius of a card-like tile is half of this share of its width: about 5%. */
    public static final float TILE_ARC = 0.1f;
    /** Where the middle of a row of cards or tiles sits. */
    public static final float ROW_CENTRE_Y = 0.47f;
    /** Top and bottom of the area that holds rows of cards. */
    public static final float ROW_AREA_TOP = 0.175f;
    public static final float ROW_AREA_BOTTOM = 0.775f;

    private static final float BUTTON_W = 0.14f;
    private static final float BUTTON_H = 0.058f;
    private static final float BUTTON_CY = 0.81f;
    private static final float TITLE_CY = 0.09f;
    private static final float TITLE_SIZE = 0.055f;
    private static final int TITLE_MOST_CHARS = 30;
    private static final float RULE_Y = 0.13f;
    private static final float RULE_W = 0.42f;
    private static final float SUBTITLE_SIZE = 0.026f;
    /** How dark the board behind a dialog gets. */
    private static final float DIM = 0.6f;
    private static final float SOURCE_H = 0.44f;
    private static final float SOURCE_TAB_W = 0.82f;
    private static final float BADGE = 0.036f;
    private static final float SOURCE_CLEAR = 0.012f;
    private static final float QUESTION_MOST = 0.042f;
    private static final float QUESTION_LEAST = 0.035f;
    private static final float QUESTION_W = 0.4f;
    private static final float QUESTION_CY = 0.078f;
    private static final float RAIL_DROP = 0.025f;

    // render-thread scratch
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color(), S4 = new Color();
    private static final Color[] STOPS4 = new Color[4];
    private static final float[] RULE_STOPS = {0f, 0.1f, 0.9f, 1f};
    private static final TextRenderer SYMBOL_TEXT = new TextRenderer();

    // ---- geometry (all from the window's size w x h) ----------------------------------------------------------------

    public static float buttonW(final float w) {
        return Math.round(w * BUTTON_W);
    }

    public static float buttonH(final float h) {
        return Math.round(h * BUTTON_H);
    }

    /** The y of the top of the button slot. */
    public static float buttonY(final float h) {
        return Math.round(h * BUTTON_CY) - buttonH(h) / 2f;
    }

    /** Puts the visible ones of these side by side in the shared slot, all the same size. */
    public static void placeButtons(final float w, final float h, final Pill... buttons) {
        int shown = 0;
        for (final Pill b : buttons) {
            if (b != null && b.visible) {
                shown++;
            }
        }
        final float bw = buttonW(w);
        int i = 0;
        for (final Pill b : buttons) {
            if (b != null && b.visible) {
                placeInRow(w / 2f, w, h, bw, i++, shown, b.bounds);
            }
        }
    }

    /**
     * Where the {@code i}th of {@code count} things {@code bw} wide goes in the shared slot round {@code cx}: side by
     * side, extra rows stacking upwards (Swing placeRowAt).
     */
    public static void placeInRow(final float cx, final float w, final float h, final float bw, final int i, final int count, final Rectangle out) {
        final float bh = buttonH(h), gap = Math.round(w * 0.012f);
        final int perRow = Math.max(1, (int) ((w - 2 * Math.round(w * 0.04f) + gap) / Math.max(1f, bw + gap)));
        final int rows = (count + perRow - 1) / perRow;
        final int row = i / perRow;
        final int inRow = Math.min(perRow, count - row * perRow);
        final float rowW = inRow * bw + (inRow - 1) * gap;
        final float x = cx - rowW / 2f + (i % perRow) * (bw + gap);
        out.set(Math.round(x), buttonY(h) - (rows - 1 - row) * (bh + gap), bw, bh);
    }

    /** View Battlefield: top right, at the height of the title. */
    public static void viewButtonBounds(final float w, final float h, final Rectangle out) {
        final float bw = Math.round(w * 0.125f), bh = buttonH(h);
        out.set(w - bw - Math.round(w * 0.024f), Math.round(h * TITLE_CY) - bh / 2f, bw, bh);
    }

    /** How wide a row of cards may be, between the window's edges or in the space left of the prompt's card. */
    public static float rowRoom(final float w, final float h, final boolean source) {
        return source ? 2 * (rowRight(w, h, true) - w / 2f) : w - 2 * Math.round(w * 0.03f);
    }

    /** How far right a row may reach. */
    public static float rowRight(final float w, final float h, final boolean source) {
        return source ? sourceTabLeft(w, h) - sourceClear(w, h) : w - Math.round(w * 0.03f);
    }

    /** Where a row this wide has its middle: the window's middle while it stays clear of the prompt's card. */
    public static float rowCentre(final float w, final float h, final float rowW, final boolean source) {
        return !source || w / 2f + (rowW + 1) / 2f <= rowRight(w, h, true) ? w / 2f : sourceTabLeft(w, h) / 2f;
    }

    /** The left end of the tab of the prompt's card. */
    public static float sourceTabLeft(final float w, final float h) {
        final float sh = sourceHeight(h);
        return sourceX(w, h) - Math.round(sourceTabSize(sh) * SOURCE_TAB_W);
    }

    private static float sourceClear(final float w, final float h) {
        return Math.round(w * SOURCE_CLEAR) + glowRim(CHOSEN, h) + glowFall(CHOSEN, h);
    }

    private static float sourceTabSize(final float cardH) {
        return Math.round(cardH * 0.075f);
    }

    public static float sourceHeight(final float h) {
        return Math.round(h * SOURCE_H);
    }

    public static float sourceWidth(final float h) {
        return Math.round(sourceHeight(h) / FCardPanel.ASPECT_RATIO);
    }

    private static float sourceX(final float w, final float h) {
        return w - sourceWidth(h) - Math.round(w * 0.014f);
    }

    /** Where the prompt's card sits: on the right, level with the row. */
    public static void sourceBounds(final float w, final float h, final Rectangle out) {
        final float sh = sourceHeight(h), sw = sourceWidth(h);
        out.set(sourceX(w, h), Math.round(h * ROW_CENTRE_Y) - sh / 2f, sw, sh);
    }

    /** How wide the number under a card is. */
    public static float badgeSize(final float h) {
        return Math.round(h * BADGE);
    }

    /** How far below a card's bottom edge the rail runs. */
    public static float railDrop(final float h) {
        return Math.max(3, Math.round(h * RAIL_DROP));
    }

    /** How far below a card's bottom edge the words under a row in order lie, clear of the numbers on its rail. */
    public static float orderRowDrop(final float h) {
        return railDrop(h) + badgeSize(h) / 2f + Math.round(h * 0.018f);
    }

    /** The y of the rule under every title. */
    public static float ruleY(final float h) {
        return Math.round(h * RULE_Y);
    }

    /** Whether a point lies in a rectangle of half-size hw, hh centred on cx, cy and turned {@code angle} degrees clockwise. */
    public static boolean inRotated(final float px, final float py, final float cx, final float cy, final float hw, final float hh, final float angle) {
        final double a = Math.toRadians(-angle);
        final double dx = px - cx, dy = py - cy;
        final double rx = dx * Math.cos(a) - dy * Math.sin(a), ry = dx * Math.sin(a) + dy * Math.cos(a);
        return Math.abs(rx) <= hw && Math.abs(ry) <= hh;
    }

    /** The corner radius of a card face {@code w} wide (Swing cardArc / 2). */
    public static float cardRadius(final float w) {
        return w * 0.0425f;
    }

    /** The corners of a card-like tile, or of one lying on its side. */
    public static float tileRadius(final float w, final float h) {
        return w > h ? h * 0.1f : w * TILE_ARC / 2f;
    }

    // ---- fonts ------------------------------------------------------------------------------------------------------

    /** A font {@code px} logical pixels big, rounded to an even size so a few sizes serve every dialog. */
    public static FSkinFont font(final float px) {
        return ArenaTheme.fontPx(Math.max(8, Math.round(px / 2f) * 2));
    }

    /** The largest of those fonts, from {@code mostPx} down to {@code leastPx}, at which {@code text} is at most {@code room} wide. */
    public static FSkinFont fitFont(final String text, final float mostPx, final float leastPx, final float room) {
        FSkinFont f = font(mostPx);
        if (text == null || text.isEmpty()) {
            return f;
        }
        final float tw = measure(text, f);
        if (tw <= room) {
            return f;
        }
        // text grows with its size, so one measurement says where to start looking
        float px = Math.max(leastPx, mostPx * room / Math.max(1f, tw));
        f = font(px);
        while (px > leastPx && measure(text, f) > room) {
            px -= 2f;
            f = font(px);
        }
        return f;
    }

    /** The width of one line, mana symbols ({W}, {2}...) included. */
    public static float measure(final String text, final FSkinFont font) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        if (text.indexOf('{') >= 0) {
            return SYMBOL_TEXT.getBounds(text, font).width;
        }
        return ArenaArt.textWidth(text, font);
    }

    /** Greedy word wrap into lines no wider than {@code width} (symbols counted); keeps the text's own line breaks. Allocates. */
    public static List<String> wrap(final String text, final FSkinFont font, final float width) {
        final List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        for (final String paragraph : text.split("\\r?\\n")) {
            final String[] words = paragraph.trim().split("\\s+");
            final StringBuilder line = new StringBuilder();
            for (final String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                if (line.length() == 0) {
                    line.append(word);
                    continue;
                }
                final String tried = line + " " + word;
                if (measure(tried, font) > width) {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.append(' ').append(word);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    // ---- text -------------------------------------------------------------------------------------------------------

    /** Message text as it comes from the game: HTML tags off, line breaks kept. */
    public static String plainText(final String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("(?s)<head>.*?</head>", "").replaceAll("<br\\s*/?>", "\n").replaceAll("<[^>]*>", "").trim();
    }

    /** Whether two texts say the same thing, so one of them need not be shown. */
    public static boolean sameWords(final String a, final String b) {
        return a != null && b != null && a.replaceAll("[^\\p{L}\\p{N}]", "").equalsIgnoreCase(b.replaceAll("[^\\p{L}\\p{N}]", ""));
    }

    /** What the first end of a row means, spoken for the last end: "Resolve first" gives "Resolve last". */
    public static String lastCaption(final String first) {
        if (first != null && first.length() >= 5 && first.toLowerCase().endsWith("first")) {
            final boolean upper = Character.isUpperCase(first.charAt(first.length() - 5));
            return first.substring(0, first.length() - 5) + (upper ? "Last" : "last");
        }
        return Forge.getLocalizer().getMessage("lblArenaLast");
    }

    /**
     * Words standing on the board by themselves: white with a thin dark edge round every letter (Swing paintEdged),
     * mana symbols drawn as symbols. {@code y}/{@code h} is the line's box, the text centred in it.
     */
    public static void edged(final Graphics g, final String text, final FSkinFont font, final Color color, final float x, final float y,
            final float w, final float h, final int align) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (text.indexOf('{') >= 0) {
            SYMBOL_TEXT.drawTextShadowed(g, text, Integer.MAX_VALUE, font, color, x, y, w, h, y, h, false, align, true);
        } else {
            ArenaArt.textOutlined(g, text, font, color, x, y, w, h, align);
        }
    }

    /** A title line: a 2px drop shadow and a heavier stroke (Swing paintTitleLine with the bold display face). */
    public static void titleLine(final Graphics g, final String text, final FSkinFont font, final float x, final float y, final float w,
            final float h, final float alpha) {
        if (text != null && text.indexOf('{') >= 0) {
            // mana symbols in a question ("add {G}?") are drawn as symbols
            SYMBOL_TEXT.drawTextShadowed(g, text, Integer.MAX_VALUE, font, ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S2), x, y, w, h, y, h, false,
                    Align.center, true);
            return;
        }
        final float o = Math.max(1f, ArenaTheme.px(2));
        final Color shadow = ArenaTheme.alpha(ArenaTheme.BLACK, 0.75f * alpha, S1);
        g.drawText(text, font, shadow, x, y + o, w, h, false, Align.center, true);
        ArenaArt.textBold(g, text, font, ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S2), x, y, w, h, Align.center);
    }

    /** A thin white line that fades out at both ends, centred on cx. */
    public static void rule(final Graphics g, final float cx, final float y, final float width, final float h, final Color c, final float alpha) {
        final Color on = ArenaTheme.alpha(c, (215 / 255f) * alpha, S1), off = ArenaTheme.alpha(c, 0f, S2);
        STOPS4[0] = off;
        STOPS4[1] = on;
        STOPS4[2] = on;
        STOPS4[3] = off;
        g.fillGradientStops(STOPS4, RULE_STOPS, false, cx - width / 2f, y, width, Math.max(1f, Math.round(h * 0.0012f)));
    }

    /**
     * The one heading of every dialog (Swing paintHeading): one line of title at one fixed size and height, a rule of
     * fixed width under it, and the instruction centred beneath in at most two lines. A title too long for its line
     * becomes {@code fallback}, its words moving down into the instruction. Lays itself out when the window or the
     * words change; draws without allocating.
     */
    public static final class Heading {
        private String title, subtitle, fallback;
        private String rawTitle, rawSubtitle, rawFallback; // as given, to see whether they changed
        private float lw, lh;
        private String shownTitle;
        private final List<String> lines = new ArrayList<>();
        private FSkinFont titleFont, subFont;
        // a question as the whole heading
        private boolean question;
        private final List<String> qLines = new ArrayList<>();
        private float qPitch;

        /** Sets what the heading says; cheap when nothing changed. */
        public void set(final String title0, final String subtitle0, final String fallback0) {
            if (!question && eq(title0, rawTitle) && eq(subtitle0, rawSubtitle) && eq(fallback0, rawFallback)) {
                return;
            }
            question = false;
            rawTitle = title0;
            rawSubtitle = subtitle0;
            rawFallback = fallback0;
            //the game's words as a player reads them: no engine ids ("Hill Giant (50)"), "X: choose a mode"
            title = ArenaPromptText.clean(title0);
            subtitle = ArenaPromptText.clean(subtitle0);
            fallback = ArenaPromptText.clean(fallback0);
            lw = 0;
        }

        /** Makes a question the whole heading, in one or two lines (Swing paintQuestionTitle). */
        public void setQuestion(final String question0) {
            if (question && eq(question0, rawTitle)) {
                return;
            }
            question = true;
            rawTitle = question0;
            title = ArenaPromptText.clean(question0);
            subtitle = null;
            fallback = null;
            lw = 0;
        }

        private static boolean eq(final String a, final String b) {
            return a == null ? b == null : a.equals(b);
        }

        private void layout(final float w, final float h) {
            if (lw == w && lh == h) {
                return;
            }
            lw = w;
            lh = h;
            lines.clear();
            titleFont = font(h * TITLE_SIZE);
            subFont = font(h * SUBTITLE_SIZE);
            if (question) {
                layoutQuestion(w, h);
                return;
            }
            // a card's name arrives with the game's internal id after it, which means nothing to a player
            final String heading = title == null ? "" : title.replaceAll("\\s*\\n\\s*", " ").replaceAll("\\s*\\(\\d+\\)\\s*$", "").trim();
            String sub = subtitle;
            if (heading.isEmpty()) {
                shownTitle = fallback;
            } else {
                final float room = Math.round(w * RULE_W * 0.9f);
                if (heading.length() <= TITLE_MOST_CHARS && measure(heading, titleFont) <= room) {
                    shownTitle = heading;
                } else {
                    shownTitle = fallback;
                    sub = sub == null || sub.isEmpty() ? heading : heading + " · " + sub;
                }
            }
            if (sub != null && !sub.isEmpty()) {
                final List<String> wrapped = wrap(sub, subFont, Math.round(w * 0.62f));
                for (int i = 0; i < wrapped.size() && i < 2; i++) {
                    lines.add(i == 1 && wrapped.size() > 2 ? wrapped.get(i) + " …" : wrapped.get(i));
                }
            }
        }

        private void layoutQuestion(final float w, final float h) {
            qLines.clear();
            float room = Math.round(w * QUESTION_W);
            FSkinFont f = titleFont;
            List<String> wrapped = wrap(title, f, room);
            if (wrapped.size() > 1) {
                f = null;
                for (float size = h * QUESTION_MOST; size >= h * QUESTION_LEAST; size -= 2f) {
                    final FSkinFont tried = font(size);
                    final List<String> l = wrap(title, tried, room);
                    if (l.size() <= 2) {
                        f = tried;
                        wrapped = l;
                        break;
                    }
                }
                if (f == null) {
                    f = font(h * QUESTION_LEAST);
                    wrapped = wrap(title, f, room);
                }
                // the two lines about equal
                while (wrapped.size() == 2 && room > w / 4f) {
                    final List<String> tighter = wrap(title, f, room - Math.round(w * 0.01f));
                    if (tighter.size() != 2) {
                        break;
                    }
                    room -= Math.round(w * 0.01f);
                    wrapped = tighter;
                }
            }
            titleFont = f;
            qLines.addAll(wrapped);
            qPitch = Math.round(f.getLineHeight() * 1.05f);
        }

        /** Whether this question goes in a title of at most two lines (Swing questionSize > 0). */
        public static boolean fitsAsQuestion(final String text, final float w, final float h) {
            if (text == null || text.isEmpty() || text.indexOf('\n') >= 0) {
                return false;
            }
            return wrap(text, font(h * QUESTION_LEAST), Math.round(w * QUESTION_W)).size() <= 2;
        }

        /** The y just below the heading: under the subtitle if there is one, else under the rule. */
        public float bottom(final float w, final float h) {
            layout(w, h);
            return lines.isEmpty() ? ruleY(h) : subtitleTop(h) + lines.size() * subFont.getLineHeight();
        }

        public void draw(final Graphics g, final float w, final float h, final float alpha) {
            layout(w, h);
            if (alpha <= 0f) {
                return;
            }
            if (question) {
                final float lineH = titleFont.getLineHeight();
                float cy = qLines.size() <= 1 ? h * TITLE_CY : h * QUESTION_CY - qPitch / 2f;
                for (final String line : qLines) {
                    titleLine(g, line, titleFont, 0, cy - lineH / 2f, w, lineH, alpha);
                    cy += qPitch;
                }
                rule(g, w / 2f, ruleY(h), Math.round(w * RULE_W), h, ArenaTheme.WHITE, alpha);
                return;
            }
            if (shownTitle != null) {
                final float lineH = titleFont.getLineHeight();
                titleLine(g, shownTitle, titleFont, 0, h * TITLE_CY - lineH / 2f, w, lineH, alpha);
                rule(g, w / 2f, ruleY(h), Math.round(w * RULE_W), h, ArenaTheme.WHITE, alpha);
            }
            float y = subtitleTop(h);
            final float lineH = subFont.getLineHeight();
            final Color white = ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S3);
            for (final String line : lines) {
                edged(g, line, subFont, white, 0, y, w, lineH, Align.center);
                y += lineH;
            }
        }

        private static float subtitleTop(final float h) {
            return ruleY(h) + Math.round(h * 0.008f);
        }
    }

    // ---- the board behind ----------------------------------------------------------------------------------------

    /**
     * The board behind a dialog, darkened (Swing captureBackdrop: dimmed to 60% near-black, the edges darker). Drawn
     * live over the board each frame, so the board keeps moving underneath; {@code shown} 0..1 fades it in.
     */
    public static void backdrop(final Graphics g, final float w, final float h, final float shown) {
        if (shown <= 0f) {
            return;
        }
        g.fillRect(ArenaTheme.alpha(BACKDROP, DIM * shown, S1), 0, 0, w, h);
        ArenaArt.vignette(g, 0, 0, w, h, Math.min(w, h) * 0.34f, 0.45f * shown);
    }

    // ---- coming into view -------------------------------------------------------------------------------------------

    /** A dialog's way in (Swing Entrance): its backdrop fades over the board, then what it holds rises into place. */
    public static final class Entrance {
        private final ArenaMotion.Fade fade = new ArenaMotion.Fade(0f);

        public void begin(final int millis) {
            fade.set(0f);
            fade.to(1f, millis, 0, ArenaMotion.LINEAR, null);
        }

        public boolean isOver() {
            return fade.get() >= 1f;
        }

        /** How far the backdrop and heading are in. */
        public float scene() {
            return ArenaMotion.clamp01(fade.get() / 0.55f);
        }

        /** How far what the dialog holds is in. */
        public float content() {
            return ArenaMotion.clamp01((fade.get() - 0.2f) / 0.8f);
        }

        /** How far down the contents still are: they rise 2.5% of the window into place. */
        public float contentDrop(final float h) {
            return Math.round((1f - content()) * h * 0.025f);
        }

        /** The same for one of a row of things, each a little behind the one before. */
        public float inTurn(final int index, final int count) {
            final float lag = count <= 1 ? 0f : Math.min(0.5f / (count - 1), 0.08f);
            return ArenaMotion.OUT.at(ArenaMotion.clamp01((content() - index * lag) / Math.max(0.1f, 1f - lag * Math.max(0, count - 1))));
        }
    }

    // ---- card light -------------------------------------------------------------------------------------------------

    /** The light of what can be picked (cyan), what has been picked (gold), a card under the pointer that cannot be (white). */
    public static final int SELECTABLE = 0, CHOSEN = 1, PLAIN = 2;

    private static float glowRim(final int kind, final float h) {
        switch (kind) {
            case CHOSEN: return Math.max(3, Math.round(h * 0.0072f));
            case PLAIN:  return Math.max(1, Math.round(h * 0.002f));
            default:     return Math.max(3, Math.round(h * 0.0064f));
        }
    }

    private static float glowCore(final int kind, final float h) {
        switch (kind) {
            case CHOSEN: return Math.max(1, Math.round(h * 0.002f));
            case PLAIN:  return 1;
            default:     return Math.max(1, Math.round(h * 0.0016f));
        }
    }

    private static float glowFall(final int kind, final float h) {
        switch (kind) {
            case CHOSEN: return Math.max(10, Math.round(h * 0.032f));
            case PLAIN:  return Math.max(8, Math.round(h * 0.014f));
            default:     return Math.max(8, Math.round(h * 0.0267f));
        }
    }

    /** How far beyond a card its light reaches: rim and bloom. */
    public static float lightReach(final int kind, final float h) {
        return glowRim(kind, h) + glowFall(kind, h);
    }

    /**
     * The soft shadow a card casts down and to the left (Swing Sprite: 3.9% of its height, moved by 40% of that).
     * Draw before the card's light and face.
     */
    public static void cardShadow(final Graphics g, final float x, final float y, final float w, final float h, final float radius, final float alpha) {
        final float spread = Math.max(8, Math.round(h * 0.039f));
        final float cast = Math.max(2, Math.round(spread * 0.4f));
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.85f * alpha, S1), x - cast, y + cast, w, h, radius, spread);
    }

    /**
     * The light round a card (Swing rimLight): a saturated rim outside its edge with a thin bright core down the
     * middle of it, and a bloom falling away beyond. {@code strength} is 1, or 1.3 under the pointer; {@code small}
     * is the tighter light of a small flat tile. Draw before the face; {@link #cardRimAfter} goes after it.
     */
    public static void cardLight(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final int kind, final float strength, final boolean small, final float H, final float alpha) {
        final Color rim = kind == CHOSEN ? PICKED : kind == PLAIN ? PLAIN_RIM : CYAN;
        final Color core = kind == CHOSEN ? PICKED_CORE : kind == PLAIN ? ArenaTheme.WHITE : CYAN_CORE;
        float rimW = glowRim(kind, H), coreW = glowCore(kind, H), fall = glowFall(kind, H);
        float bloom = kind == CHOSEN ? 0.9f : kind == PLAIN ? 0.3f : Math.min(0.95f, 0.62f * strength);
        if (small) {
            rimW = Math.max(2, Math.round(H * 0.003f));
            coreW = 1;
            fall = Math.max(6, Math.round(H * 0.009f));
            bloom *= 0.85f;
        }
        // the bloom: strongest at the rim's outer edge, gone a long way out
        g.drawGlow(ArenaTheme.alpha(rim, bloom * alpha, S1), x - rimW, y - rimW, w + 2 * rimW, h + 2 * rimW, radius + rimW, fall);
        // the rim, then its core
        g.drawSmoothRoundRect(rimW, ArenaTheme.alpha(rim, 0.97f * alpha, S2), x - rimW, y - rimW, w + 2 * rimW, h + 2 * rimW, radius + rimW);
        final float c = rimW / 2f + coreW / 2f;
        g.drawSmoothRoundRect(coreW, ArenaTheme.alpha(core, alpha, S3), x - c, y - c, w + 2 * c, h + 2 * c, radius + c);
    }

    /**
     * Over a card's face: the light spilling in over its border and one smooth line over the seam (glowing cards), a
     * thin lit edge (plain cards, so black borders do not run into each other), or the shade of a card that waits.
     */
    public static void cardRimAfter(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final int kind, final boolean lit, final boolean dimmed, final float H, final float alpha) {
        if (dimmed) {
            g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.BLACK, (150 / 255f) * alpha, S1), x, y, w, h, radius);
            return;
        }
        if (!lit) {
            g.drawSmoothRoundRect(1f, ArenaTheme.fadeBy(CARD_EDGE, alpha, S1), x, y, w, h, radius);
            return;
        }
        final Color rim = kind == CHOSEN ? PICKED : kind == PLAIN ? PLAIN_RIM : CYAN;
        final float reach = Math.max(2f, glowRim(kind, H) * 0.8f);
        g.drawSmoothRoundRect(reach, ArenaTheme.alpha(rim, 0.3f * alpha, S1), x, y, w, h, radius);
        g.drawSmoothRoundRect(Math.max(1f, reach * 0.4f), ArenaTheme.alpha(rim, 0.35f * alpha, S2), x, y, w, h, radius);
        g.drawSmoothRoundRect(1.5f, ArenaTheme.alpha(rim, alpha, S3), x, y, w, h, radius);
    }

    /**
     * A card face (art, frame and all, as the board draws it), with rounded corners as the client's card mask has
     * them. Cards the player may not see show their sleeve.
     */
    public static void cardFace(final Graphics g, final CardView card, final float x, final float y, final float w, final float h) {
        if (card == null) {
            return;
        }
        CardRenderer.drawCard(g, card, x, y, w, h, CardStackPosition.Top);
    }

    /**
     * One card of a row, whole: shadow, light, face, rim, centred on cx, cy, turned {@code angle} degrees clockwise and
     * scaled. {@code kind} is {@link #SELECTABLE}/{@link #CHOSEN}/{@link #PLAIN} or -1 for no light. Turning costs a
     * flush; a card lying straight costs none of its own.
     */
    public static void card(final Graphics g, final CardView card, final float cx, final float cy, final float cw, final float ch,
            final float angle, final float scale, final int kind, final float strength, final boolean dimmed, final float H, final float alpha) {
        if (alpha <= 0f) {
            return;
        }
        final float w = cw * scale, h = ch * scale, x = cx - w / 2f, y = cy - h / 2f, r = cardRadius(w);
        final boolean turned = Math.abs(angle) > 0.05f;
        if (turned) {
            g.startRotateTransform(cx, cy, -angle);
        }
        final boolean fade = alpha < 1f;
        final float old = g.getfloatAlphaComposite();
        if (fade) {
            g.setAlphaComposite(old * alpha);
        }
        cardShadow(g, x, y, w, h, r, 1f);
        final boolean lit = kind >= 0 && !dimmed;
        if (lit) {
            cardLight(g, x, y, w, h, r, kind, strength, false, H, 1f);
        }
        cardFace(g, card, x, y, w, h);
        cardRimAfter(g, x, y, w, h, r, kind, lit, dimmed, H, 1f);
        if (fade) {
            g.setAlphaComposite(old);
        }
        if (turned) {
            g.endTransform();
        }
    }

    /**
     * The prompt's card (Swing paintSource): lit by a cool soft bloom but with no selection edge, so it does not read
     * as an option. Beside a row a small tab says it is the source of the decision.
     */
    public static void source(final Graphics g, final CardView card, final float x, final float y, final float w, final float h,
            final boolean tab, final float alpha) {
        if (card == null || alpha <= 0f) {
            return;
        }
        final float spread = Math.round(h * 0.036f), r = cardRadius(w);
        g.drawSoftShadow(ArenaTheme.alpha(SOURCE_BLOOM, 0.35f * alpha, S1), x, y, w, h, r, spread);
        g.drawGlow(ArenaTheme.alpha(SOURCE_BLOOM, 0.5f * alpha, S2), x, y, w, h, r, spread);
        if (tab) {
            sourceTab(g, x, y + Math.round(h * 0.13f), sourceTabSize(h), alpha);
        }
        final float old = g.getfloatAlphaComposite();
        if (alpha < 1f) {
            g.setAlphaComposite(old * alpha);
        }
        cardFace(g, card, x, y, w, h);
        if (alpha < 1f) {
            g.setAlphaComposite(old);
        }
    }

    /** A small dark tab with two gold chevrons, tucked behind the left edge of a card at {@code x}. */
    private static void sourceTab(final Graphics g, final float x, final float y, final float s, final float alpha) {
        final float tw = s * SOURCE_TAB_W, r = s * 0.16f;
        g.fillSmoothRoundRectGradient(ArenaTheme.fadeBy(TAB_TOP, alpha, S1), ArenaTheme.fadeBy(TAB_BOTTOM, alpha, S2), true, x - tw, y, tw + 2 * r, s, r);
        g.drawSmoothRoundRect(1f, ArenaTheme.alpha(PICKED, (200 / 255f) * alpha, S3), x - tw, y, tw + 2 * r, s, r);
        final float cy = y + s / 2f, leg = s * 0.2f, t = Math.max(1.6f, s * 0.085f);
        final Color c = ArenaTheme.alpha(PICKED, alpha, S4);
        for (int i = 0; i < 2; i++) {
            final float cx = x - tw * 0.62f + i * s * 0.24f;
            chevron(g, cx, cy, leg, t, c, 0);
        }
    }

    /** A chevron pointing right (dir 0), left (1), down (2) or up (3), drawn as two round-capped strokes. */
    private static void chevron(final Graphics g, final float cx, final float cy, final float leg, final float t, final Color c, final int dir) {
        final float ax = -leg * 0.5f, ay = -leg, bx = leg * 0.5f, by = 0f, cxx = -leg * 0.5f, cyy = leg;
        stroke(g, cx, cy, ax, ay, bx, by, t, c, dir);
        stroke(g, cx, cy, bx, by, cxx, cyy, t, c, dir);
    }

    // one stroke from (x1, y1) to (x2, y2), relative to cx, cy and turned for dir: a thin rounded bar, batched
    private static void stroke(final Graphics g, final float cx, final float cy, float x1, float y1, float x2, float y2, final float t,
            final Color c, final int dir) {
        float tx;
        switch (dir) {
            case 1: x1 = -x1; x2 = -x2; break;
            case 2: tx = x1; x1 = -y1; y1 = tx; tx = x2; x2 = -y2; y2 = tx; break;
            case 3: tx = x1; x1 = y1; y1 = -tx; tx = x2; x2 = y2; y2 = -tx; break;
            default: break;
        }
        final float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
        final float mx = cx + (x1 + x2) / 2f, my = cy + (y1 + y2) / 2f;
        final float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        g.startRotateTransform(mx, my, -angle);
        g.fillSmoothRoundRect(c, mx - len / 2f - t / 2f, my - t / 2f, len + t, t, t / 2f);
        g.endTransform();
    }

    /** A number in a dark disc with a bright ring (the order of a pick, a place in a row). */
    public static void badge(final Graphics g, final int place, final float cx, final float cy, final float d, final float alpha) {
        final float r = d / 2f;
        g.fillSmoothCircle(ArenaTheme.alpha(ArenaTheme.BLACK, (110 / 255f) * alpha, S1), cx, cy + 1 + 2, r + 2);
        g.fillSmoothRoundRectGradient(ArenaTheme.fadeBy(BADGE_TOP, alpha, S2), ArenaTheme.fadeBy(BADGE_BOTTOM, alpha, S3), true, cx - r, cy - r, d, d, r);
        g.drawSmoothCircle(Math.max(1.5f, d * 0.055f), ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, alpha, S4), cx, cy, r);
        final FSkinFont f = font(d * 0.56f);
        ArenaArt.textBold(g, Integer.toString(place), f, ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S1), cx - r, cy - r, d, d, Align.center);
    }

    /** A word above a row of cards, centred on cx, its middle at cy. */
    public static void rowLabel(final Graphics g, final String label, final float cx, final float cy, final float h, final float alpha) {
        final FSkinFont f = font(h * 0.024f);
        final float lh = f.getLineHeight();
        edged(g, label, f, ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S1), cx - h, cy - lh / 2f, 2 * h, lh, Align.center);
    }

    /** The thin gold rail that the numbers of a row in order sit on, with a small stop at each end. */
    public static void rail(final Graphics g, final float x0, final float x1, final float y, final float h, final float alpha) {
        final float t = Math.max(1.5f, h * 0.0016f), stop = t * 2.2f;
        g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.GOLD, (70 / 255f) * alpha, S1), x0, y - t * 1.5f, x1 - x0, t * 3f, t * 1.5f);
        final Color c = ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, alpha, S2);
        g.fillSmoothRoundRect(c, x0, y - t / 2f, x1 - x0, t, t / 2f);
        g.fillSmoothCircle(c, x0, y, stop);
        g.fillSmoothCircle(c, x1, y, stop);
    }

    /** What the two ends of a row in order mean, each under the number at its end; cy is the middle of the words. */
    public static void railEnds(final Graphics g, final String first, final String last, final float firstX, final float lastX, final float cy,
            final float h, final float alpha) {
        final FSkinFont f = font(h * 0.021f);
        final float lh = f.getLineHeight();
        final Color c = ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S1);
        edged(g, first, f, c, firstX - h, cy - lh / 2f, 2 * h, lh, Align.center);
        if (lastX != firstX && last != null) {
            edged(g, last, f, c, lastX - h, cy - lh / 2f, 2 * h, lh, Align.center);
        }
    }

    /** The one round button, for steppers and rows that scroll: a dark disc in a gold rim, with a bold chevron. dx/dy say which way. */
    public static void roundButton(final Graphics g, final float cx, final float cy, final float d, final int dx, final int dy, final boolean lit,
            final boolean off, final float alpha) {
        final float a = off ? 0.4f * alpha : alpha;
        if (a <= 0f) {
            return;
        }
        final float rim = Math.max(2f, d * 0.05f), inner = d - rim, r = inner / 2f;
        g.fillSmoothRoundRectGradient(ArenaTheme.fadeBy(lit ? ROUND_LIT_TOP : ROUND_TOP, a, S1), ArenaTheme.fadeBy(lit ? ROUND_LIT_BOTTOM : ROUND_BOTTOM, a, S2),
                true, cx - r, cy - r, inner, inner, r);
        g.drawArenaShape(Graphics.SHAPE_RING, cx - r - rim / 2f, cy - r - rim / 2f, inner + rim, inner + rim, r + rim / 2f, rim,
                ArenaTheme.alpha(lit ? ArenaTheme.WHITE : ArenaTheme.GOLD_BRIGHT, a, S3), ArenaTheme.alpha(ArenaTheme.GOLD, a, S4), true);
        final int dir = dy > 0 ? 2 : dy < 0 ? 3 : dx < 0 ? 1 : 0;
        final float leg = d * 0.2f;
        final float shift = dir == 0 ? leg * 0.05f : dir == 1 ? -leg * 0.05f : 0f;
        chevron(g, cx + shift, cy, leg, Math.max(3f, d * 0.105f), ArenaTheme.alpha(ArenaTheme.WHITE, a, S1), dir);
    }

    /** A card-shaped hollow that says something can be dropped here: a dark well in a dashed pale line. */
    public static void slot(final Graphics g, final float cx, final float cy, final float cw, final float ch, final float alpha) {
        final float x = cx - cw / 2f, y = cy - ch / 2f, r = cardRadius(cw);
        g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.BLACK, (70 / 255f) * alpha, S1), x, y, cw, ch, r);
        final Color c = ArenaTheme.alpha(ArenaTheme.WHITE, (70 / 255f) * alpha, S2);
        final float dash = 7f, t = 1.5f;
        for (float px = x + r; px < x + cw - r; px += 2 * dash) {
            final float len = Math.min(dash, x + cw - r - px);
            g.fillRect(c, px, y, len, t);
            g.fillRect(c, px, y + ch - t, len, t);
        }
        for (float py = y + r; py < y + ch - r; py += 2 * dash) {
            final float len = Math.min(dash, y + ch - r - py);
            g.fillRect(c, x, py, t, len);
            g.fillRect(c, x + cw - t, py, t, len);
        }
    }

    /** Where the shards lie under a row: across its width, below its feet (share of the height), length (share of the height), turn. */
    private static final float[][] SHARDS = {
        {0.08f, 0.02f, 0.042f, -68f}, {0.2f, 0.054f, 0.03f, 42f}, {0.35f, 0.026f, 0.05f, 76f}, {0.47f, 0.058f, 0.028f, -32f},
        {0.62f, 0.022f, 0.04f, 108f}, {0.74f, 0.056f, 0.046f, -82f}, {0.91f, 0.028f, 0.034f, 56f}
    };

    /** Says what colour the light under a row is at a given x. */
    public interface LightAt {
        Color at(float x);
    }

    /**
     * Splinters of the light lying at the feet of a row that can be picked from, always in the same places and only
     * below the cards (Swing paintRowShards / ArenaArt.paintShard): each a soft elongated glow with a bright sliver.
     */
    public static void rowShards(final Graphics g, final float x0, final float x1, final float foot, final float h, final LightAt colour,
            final float alpha) {
        if (alpha <= 0f) {
            return;
        }
        for (final float[] s : SHARDS) {
            final float x = Math.round(x0 + (x1 - x0) * s[0]);
            final float y = foot + h * s[1], len = h * s[2];
            final Color c = colour.at(x);
            g.startRotateTransform(x, y, -s[3]);
            g.drawSoftShadow(ArenaTheme.alpha(c, 0.42f * alpha, S1), x - len * 0.12f, y - len * 0.42f, len * 0.24f, len * 0.84f, len * 0.12f, len * 0.3f);
            g.fillSmoothRoundRect(ArenaTheme.alpha(c, 0.85f * alpha, S2), x - len * 0.035f, y - len * 0.5f, len * 0.07f, len, len * 0.035f);
            g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.WHITE, 0.7f * alpha, S3), x - len * 0.012f, y - len * 0.36f, len * 0.024f, len * 0.72f, len * 0.012f);
            g.endTransform();
        }
    }

    /** The remember box: a gold outline round a dark well with a gold tick, its words beside it (box is the box's side). */
    public static void checkBox(final Graphics g, final String text, final boolean checked, final boolean hover, final float x, final float y,
            final float box, final float alpha) {
        final float line = Math.max(1.6f, box * 0.09f), r = box * 0.12f;
        g.fillSmoothRoundRectGradient(ArenaTheme.fadeBy(CHECK_TOP, alpha, S1), ArenaTheme.fadeBy(CHECK_BOTTOM, alpha, S2), true, x, y, box, box, r);
        g.drawSmoothRoundRect(line, ArenaTheme.alpha(hover ? ArenaTheme.GOLD_BRIGHT : ArenaTheme.GOLD, alpha, S3), x, y, box, box, r);
        if (checked) {
            final float t = Math.max(1.8f, box * 0.13f);
            final Color c = ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, alpha, S4);
            tick(g, x, y, box, box * 0.24f, ArenaTheme.alpha(ArenaTheme.GOLD, (70 / 255f) * alpha, S1));
            tick(g, x, y, box, t, c);
        }
        if (text != null) {
            final FSkinFont f = font(box * 0.92f);
            edged(g, text, f, ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S2), x + box * 1.5f, y, Forge.getScreenWidth(), box, Align.left);
        }
    }

    private static void tick(final Graphics g, final float x, final float y, final float box, final float t, final Color c) {
        final float ax = x + box * 0.27f, ay = y + box * 0.52f, bx = x + box * 0.44f, by = y + box * 0.69f, cx = x + box * 0.74f, cy = y + box * 0.31f;
        stroke(g, 0, 0, ax, ay, bx, by, t, c, 0);
        stroke(g, 0, 0, bx, by, cx, cy, t, c, 0);
    }

    /** The width the remember box wants for its words when its box is {@code box} high. */
    public static float checkBoxWidth(final String text, final float box) {
        return box * 1.5f + measure(text, font(box * 0.92f)) + 4;
    }

    // ---- buttons ----------------------------------------------------------------------------------------------------

    /**
     * One of a dialog's buttons (Swing ArenaDialogKit.Pill): the Arena button with its label in the true middle of the
     * pill, white on a brown shadow. Drawn and hit-tested by the dialog that owns it; touch and mouse both press it.
     */
    public static final class Pill {
        public final Rectangle bounds = new Rectangle();
        public String text = "";
        public boolean primary, enabled = true, toggled, visible = true;
        public boolean hover, pressed;
        public Runnable action;
        /** Shown when this pill came into view (Swing FButton arenaAppear): grows in from 90%. */
        private final ArenaMotion.Fade hoverFade = new ArenaMotion.Fade(0f);

        public Pill() {
        }

        public Pill(final String text0, final boolean primary0, final Runnable action0) {
            text = text0;
            primary = primary0;
            action = action0;
        }

        public boolean contains(final float x, final float y) {
            return visible && bounds.contains(x, y);
        }

        public void setHover(final boolean b) {
            if (b != hover) {
                hover = b;
                hoverFade.to(b ? 1f : 0f, b ? 110 : 180);
            }
        }

        /** Runs its action if it can be pressed. */
        public boolean fire() {
            if (visible && enabled && action != null) {
                action.run();
                return true;
            }
            return false;
        }

        public void draw(final Graphics g, final float alpha) {
            if (!visible || alpha <= 0f) {
                return;
            }
            pill(g, text, bounds.x, bounds.y, bounds.width, bounds.height, primary, hoverFade.get() > 0.5f && enabled, pressed && enabled, toggled,
                    enabled, alpha);
        }
    }

    /** Swing paintPill: the Arena button with its label centred by the eye, shrunk to fit. */
    public static void pill(final Graphics g, final String text, final float x, final float y, final float w, final float h, final boolean primary,
            final boolean hover, final boolean pressed, final boolean toggled, final boolean enabled, final float alpha) {
        pill(g, text, null, x, y, w, h, primary, hover, pressed, toggled, enabled, alpha);
    }

    /** The font a pill this tall would pick for its words if they fit (one size for a whole wall of pills). */
    public static FSkinFont pillFont(final float h) {
        return font(Math.max(ArenaTheme.px(12), Math.min(h * 0.5f, ArenaTheme.px(30))));
    }

    /** {@link #pill} in a given font (null: the largest that fits, as a lone button has it). */
    public static void pill(final Graphics g, final String text, final FSkinFont font0, final float x, final float y, final float w, final float h,
            final boolean primary, final boolean hover, final boolean pressed, final boolean toggled, final boolean enabled, final float alpha) {
        final float old = g.getfloatAlphaComposite();
        if (alpha < 1f) {
            g.setAlphaComposite(old * alpha);
        }
        ArenaArt.button(g, x, y, w, h, primary, hover, pressed, false, toggled, enabled);
        if (text != null && !text.isEmpty()) {
            final float most = Math.max(ArenaTheme.px(12), Math.min(h * 0.5f, ArenaTheme.px(30)));
            final FSkinFont f = font0 != null ? font0 : fitFont(text, most, ArenaTheme.px(10), w - h * 0.7f);
            final float dy = pressed ? Math.max(1f, ArenaTheme.px(1)) : 0f;
            if (enabled) {
                g.drawText(text, f, primary ? PILL_SHADOW : PILL_SHADOW_SOFT, x, y + dy + 1, w, h, false, Align.center, true);
                // a heavier stroke: the words twice, a whole pixel apart, so every pill has the same weight
                g.drawText(text, f, ArenaTheme.WHITE, x, y + dy, w, h, false, Align.center, true);
                g.drawText(text, f, ArenaTheme.WHITE, x + 1, y + dy, w, h, false, Align.center, true);
            } else {
                g.drawText(text, f, PILL_OFF_TEXT, x, y, w, h, false, Align.center, true);
            }
        }
        if (alpha < 1f) {
            g.setAlphaComposite(old);
        }
    }

    // ---- what the decision is about ---------------------------------------------------------------------------------

    private static java.lang.reflect.Method promptCardGetter;
    private static java.lang.reflect.Field promptCardField;
    private static boolean promptCardLooked;

    /**
     * The card the match's prompt is about (what is being cast or resolved while the game asks), or null. Read from the
     * active VPrompt: its {@code getCardView()} if it has one, else the card it keeps for its message.
     */
    public static CardView promptCard() {
        try {
            final forge.screens.match.MatchScreen view = forge.screens.match.MatchController.getView();
            if (view == null) {
                return null;
            }
            final Object prompt = view.getActivePrompt();
            if (prompt == null) {
                return null;
            }
            if (!promptCardLooked) {
                promptCardLooked = true;
                try {
                    promptCardGetter = prompt.getClass().getMethod("getCardView");
                } catch (final NoSuchMethodException e) {
                    try {
                        promptCardField = forge.screens.match.views.VPrompt.class.getDeclaredField("card");
                        promptCardField.setAccessible(true);
                    } catch (final ReflectiveOperationException | RuntimeException e2) {
                        promptCardField = null;
                    }
                }
            }
            final Object card = promptCardGetter != null ? promptCardGetter.invoke(prompt)
                    : promptCardField != null ? promptCardField.get(prompt) : null;
            return card instanceof CardView ? (CardView) card : null;
        } catch (final ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** The dialog's own title unless it is empty or only the program's name, in which case the words for {@code fallbackKey}. */
    public static String dialogTitle(final String title, final String fallbackKey) {
        return title == null || title.trim().isEmpty() || "Forge".equals(title.trim()) ? Forge.getLocalizer().getMessage(fallbackKey) : title;
    }

    // ---- waiting for an answer --------------------------------------------------------------------------------------

    /** Something that opens a dialog on the render thread and hands its answer to the callback when it closes. */
    public interface Opener<T> {
        void open(Consumer<T> done);
    }

    /**
     * Opens a dialog on the render thread and blocks the calling (game) thread until it answers. Must not be called
     * on the render thread (it would wait for itself); there it returns {@code fallback} at once.
     */
    public static <T> T await(final Opener<T> opener, final T fallback) {
        if (FThreads.isGuiThread()) {
            return fallback;
        }
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<T> result = new AtomicReference<>(fallback);
        FThreads.invokeInEdtLater(() -> {
            try {
                opener.open(value -> {
                    result.set(value);
                    done.countDown();
                });
            } catch (final RuntimeException e) {
                e.printStackTrace();
                done.countDown();
            }
        });
        try {
            done.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result.get();
    }
}
