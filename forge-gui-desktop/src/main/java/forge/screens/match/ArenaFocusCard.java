package forge.screens.match;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;

import forge.CachedCardImage;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.toolbox.ArenaText;
import forge.toolbox.ArenaMotion;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FSkin;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.view.FView;
import forge.view.arcane.CardPanel;

/**
 * The large cards on the right of the Arena board. The top of the stack shows with what
 * it is, who cast it and what it targets; when the current prompt is about a different
 * card (a trigger of yours answering that spell, say) that card shows beside it. It has
 * no mouse listeners, so it never blocks the board beneath it.
 */
@SuppressWarnings("serial")
public final class ArenaFocusCard extends JComponent {
    private static final float HEIGHT_ONE = 0.45f;
    private static final float HEIGHT_TWO = 0.4f;
    private static final float HEIGHT_THREE = 0.31f;
    private static final int PAD = 16;
    private static final int GAP = 20;
    private static final int NOTE_LINE = 22;
    private static final float MINOR_SCALE = 0.7f;
    private static ArenaFocusCard instance;

    /** One card in the row, with the notes printed under it. */
    private static final class Entry {
        final CardView card;
        final Color glow;
        final List<String> notes;
        final String abilityText; // printed over the card's text box when an ability is on the stack
        String abilityLabel; // what the type line says instead while that is so
        int moreOnStack;
        boolean minor; // what set a trigger off: smaller than the ability it caused
        CachedCardImage image;

        Entry(final CardView card, final Color glow, final List<String> notes, final String abilityText) {
            this.card = card;
            this.glow = glow;
            this.notes = notes;
            this.abilityText = abilityText;
        }
    }

    /** Where a card was just showing, for it to fly from there as it goes into play. */
    public record Shown(Rectangle at, BufferedImage image, long when) { }

    private static final long SHOWN_LASTS_MILLIS = 1500L;
    private static final Map<Integer, Shown> justShown = new HashMap<>();

    private final List<Entry> entries = new ArrayList<>();
    private final ArenaMotion.Fade appear = new ArenaMotion.Fade(this::repaint, 0f);
    private int cardW;
    private int cardH;

    private ArenaFocusCard() {
        setOpaque(false);
        setVisible(false);
    }

    /** Re-reads the prompt card and the stack; safe to call from any thread. */
    public static void refresh(final CMatchUI matchUI) {
        FThreads.invokeInEdtNowOrLater(() -> update(matchUI));
    }

    private static void update(final CMatchUI matchUI) {
        final List<Entry> next = ArenaTheme.isMatchBoard() ? collectEntries(matchUI) : new ArrayList<>();
        if (instance != null) {
            instance.rememberGoing(next);
        }
        if (next.isEmpty()) {
            if (instance != null && instance.isVisible()) {
                instance.appear.to(0f, 140, () -> {
                    instance.setVisible(false);
                    instance.entries.clear();
                });
            }
            return;
        }
        if (instance == null) {
            instance = new ArenaFocusCard();
            FView.SINGLETON_INSTANCE.getLpnDocument().add(instance, FView.TARGETING_LAYER);
        }
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        // what set a trigger off is drawn small, so it does not count towards how big the main cards can be
        final long major = next.stream().filter(e -> !e.minor).count();
        final int h = Math.round(lpn.getHeight() * (major > 2 ? HEIGHT_THREE : major > 1 ? HEIGHT_TWO : HEIGHT_ONE));
        final int w = Math.round(h / CardPanel.ASPECT_RATIO);
        int noteLines = 0;
        for (final Entry e : next) {
            // keep the images already fetched for cards still showing
            for (final Entry old : instance.entries) {
                if (old.card.equals(e.card) && instance.cardH == h) {
                    e.image = old.image;
                }
            }
            if (e.image == null) {
                e.image = new CachedCardImage(e.card, matchUI.getLocalPlayers(), w, h) {
                    @Override
                    public void onImageFetched() {
                        instance.repaint();
                    }
                };
            }
            if (!e.minor) {
                noteLines = Math.max(noteLines, e.notes.size()); // a minor card's label goes above it
            }
        }
        // a new card rises into view; a card that stays is left alone
        final boolean newTop = instance.entries.isEmpty() || !instance.entries.get(0).card.equals(next.get(0).card);
        instance.entries.clear();
        instance.entries.addAll(next);
        instance.cardW = w;
        instance.cardH = h;
        // sized as if the small ones were full width, then they just leave space; simple and stable
        int rowW = -GAP;
        for (final Entry e : next) {
            rowW += (e.minor ? Math.round(w * MINOR_SCALE) : w) + GAP;
        }
        final int width = rowW + 2 * PAD;
        final int height = h + 2 * PAD + (noteLines == 0 ? 0 : 8 + noteLines * NOTE_LINE);
        instance.setBounds(lpn.getWidth() - width - 2, Math.round(lpn.getHeight() * 0.125f), width, height);
        instance.setVisible(true);
        if (newTop) {
            instance.appear.set(0f);
            instance.appear.to(1f, 240, 70, null);
        } else {
            instance.appear.to(1f, 120);
        }
        instance.repaint();
    }

    /** The cards about to stop showing, and where they were. */
    private void rememberGoing(final List<Entry> next) {
        final long now = System.currentTimeMillis();
        justShown.values().removeIf(s -> now - s.when() > SHOWN_LASTS_MILLIS);
        for (int i = 0; i < entries.size(); i++) {
            final Entry e = entries.get(i);
            final boolean stays = next.stream().anyMatch(n -> n.card.equals(e.card));
            if (!stays) {
                justShown.put(e.card.getId(), new Shown(entryBounds(i), e.image == null ? null : e.image.getImage(), now));
            }
        }
    }

    /** Where this entry's card is, in the layered pane. */
    private Rectangle entryBounds(final int index) {
        int x = PAD;
        for (int i = 0; i < index; i++) {
            x += (entries.get(i).minor ? Math.round(cardW * MINOR_SCALE) : cardW) + GAP;
        }
        final Entry e = entries.get(index);
        return new Rectangle(getX() + x, getY() + (e.minor ? PAD + NOTE_LINE : PAD),
                e.minor ? Math.round(cardW * MINOR_SCALE) : cardW, e.minor ? Math.round(cardH * MINOR_SCALE) : cardH);
    }

    /** Where this card is showing as the large card now, in the layered pane. Null if it is not. */
    public static Rectangle getCardBounds(final CardView card) {
        if (instance == null || !instance.isVisible()) {
            return null;
        }
        for (int i = 0; i < instance.entries.size(); i++) {
            if (instance.entries.get(i).card.equals(card)) {
                return instance.entryBounds(i);
            }
        }
        return null;
    }

    /** Where this card is showing as the large card, or was a moment ago. Null if nowhere. */
    public static Shown lastShown(final CardView card) {
        final Rectangle live = getCardBounds(card);
        if (live != null) {
            for (final Entry e : instance.entries) {
                if (e.card.equals(card)) {
                    return new Shown(live, e.image == null ? null : e.image.getImage(), System.currentTimeMillis());
                }
            }
        }
        final Shown shown = justShown.get(card.getId());
        return shown == null || System.currentTimeMillis() - shown.when() > SHOWN_LASTS_MILLIS ? null : shown;
    }

    /** Where arcs to the stack's targets start: the left edge of the large card, on screen. Null if it is not showing. */
    public static java.awt.Point getArcOrigin() {
        if (instance == null || !instance.isShowing() || instance.entries.isEmpty()) {
            return null;
        }
        final java.awt.Point p = instance.getLocationOnScreen();
        // an arc begins a little way along its path, so that puts its visible start on the card's edge
        p.translate(PAD + 70, PAD + instance.cardH / 2);
        return p;
    }

    private static List<Entry> collectEntries(final CMatchUI matchUI) {
        final List<Entry> entries = new ArrayList<>();
        final CardView promptCard = matchUI.getCPrompt().getView().getCardView();
        final GameView gameView = matchUI.getGameView();
        final StackItemView top = gameView == null ? null : gameView.peekStack();
        if (top != null && top.getSourceCard() != null) {
            final List<String> notes = describeStackItem(top, matchUI);
            final Entry entry = new Entry(top.getSourceCard(), ArenaTheme.GOLD, notes, top.isAbility() ? abilityText(top) : null);
            entry.abilityLabel = Localizer.getInstance().getMessage(top.isTrigger() ? "lblArenaTriggeredAbility" : "lblArenaAbility");
            entry.moreOnStack = gameView.getStack().size() - 1;
            entries.add(entry);
            // a trigger shows the card that set it off beside it (the spell cast, the creature that died...)
            for (final CardView cause : triggerCauses(top, gameView)) {
                if (!cause.equals(promptCard)) {
                    final List<String> causeNotes = new ArrayList<>();
                    causeNotes.add(Localizer.getInstance().getMessage("lblArenaTriggeredBy"));
                    final Entry causeEntry = new Entry(cause, ArenaTheme.HOVER, causeNotes, null);
                    causeEntry.minor = true;
                    entries.add(causeEntry);
                }
            }
        }
        if (promptCard != null && (top == null || !promptCard.equals(top.getSourceCard()))) {
            final List<String> notes = new ArrayList<>();
            if (top != null) {
                notes.add(Localizer.getInstance().getMessage("lblArenaYourChoice"));
            }
            entries.add(new Entry(promptCard, ArenaTheme.PLAYABLE, notes, null));
        }
        return entries;
    }

    /**
     * The stack item's own description, which has the numbers worked out ("scry 2"), without
     * the "Card (id) - " in front, the targets (listed under the card) or the bracketed list of
     * what set a trigger off (shown as a card beside it).
     */
    private static String abilityText(final StackItemView item) {
        String text = item.getText();
        if (text == null) {
            return null;
        }
        final int dash = text.indexOf(" - ");
        if (dash >= 0 && item.getSourceCard() != null && text.startsWith(item.getSourceCard().getCurrentState().getName())) {
            text = text.substring(dash + 3);
        }
        text = TRIGGER_OBJECTS.matcher(text).replaceAll("");
        return text.replaceAll("(?s)\\s*\\(Targeting.*$", "").trim();
    }

    /** A trigger's stack text ends with what set it off: "[Card: Name (id), Activator: ...]". */
    private static final Pattern TRIGGER_OBJECTS = Pattern.compile("\\s*\\[([^\\[\\]]*)\\]\\s*$");
    private static final Pattern CARD_ID = Pattern.compile("\\((\\d+)\\)");

    /** The cards named in a trigger's bracketed list that can still be found in play, on the stack or in a zone. */
    private static List<CardView> triggerCauses(final StackItemView item, final GameView gameView) {
        final List<CardView> causes = new ArrayList<>();
        if (!item.isTrigger() || item.getText() == null) {
            return causes;
        }
        final Matcher objects = TRIGGER_OBJECTS.matcher(item.getText());
        if (!objects.find()) {
            return causes;
        }
        final Matcher ids = CARD_ID.matcher(objects.group(1));
        while (ids.find() && causes.size() < 1) {
            final CardView card = findCard(gameView, Integer.parseInt(ids.group(1)));
            if (card != null && !card.equals(item.getSourceCard()) && !causes.contains(card)) {
                causes.add(card);
            }
        }
        return causes;
    }

    private static CardView findCard(final GameView gameView, final int id) {
        for (final StackItemView si : gameView.getStack()) {
            if (si.getSourceCard() != null && si.getSourceCard().getId() == id) {
                return si.getSourceCard();
            }
        }
        for (final PlayerView p : gameView.getPlayers()) {
            for (final ZoneType zone : CAUSE_ZONES) {
                final Iterable<CardView> cards = p.getCards(zone);
                if (cards != null) {
                    for (final CardView c : cards) {
                        if (c.getId() == id) {
                            return c;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static final ZoneType[] CAUSE_ZONES = {ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Hand, ZoneType.Command};

    /** What the stack item is and who cast it, then one line per target. */
    private static List<String> describeStackItem(final StackItemView item, final CMatchUI matchUI) {
        // the targets are shown by the arcs reaching out from the card; this just says whose it is, if not yours
        final List<String> notes = new ArrayList<>();
        if (item.getActivatingPlayer() != null && !matchUI.isLocalPlayer(item.getActivatingPlayer())) {
            notes.add(item.getActivatingPlayer().getName());
        }
        return notes;
    }

    private static Color mix(final Color a, final Color b, final float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t), Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** A bar across the frame in a pale cast of its colour, with words on it, as a card's title and type lines. */
    private static void paintBar(final Graphics2D g, final String text, final Color pale, final int x, final int y, final int w, final int h) {
        final int radius = h / 2;
        g.setPaint(new java.awt.GradientPaint(0, y, ArenaTheme.lift(pale), 0, y + h, mix(pale, Color.BLACK, 0.14f)));
        g.fillRoundRect(x, y, w, h, radius, radius);
        g.setColor(new Color(16, 16, 20, 175));
        g.drawRoundRect(x, y, w, h, radius, radius);
        g.setFont(ArenaTheme.displayFont(java.awt.Font.BOLD, Math.max(12f, h * 0.66f)));
        final FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(14, 14, 18));
        g.drawString(text, x + h / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    /**
     * An ability on the stack gets a card of its own in one continuous frame, in the colour of the card it
     * comes from: its name and cost on a bar, its art, a bar saying what this is, and the ability itself in a
     * pale box with the numbers worked out.
     */
    private static void paintAbilityCard(final Graphics2D g, final Entry e, final BufferedImage img, final Shape shape,
            final int x, final int y, final int w, final int h, final int arc) {
        final Color frame = ArenaTheme.frameLine(ArenaTheme.tileTint(e.card.getCurrentState()));
        final Color pale = mix(frame, Color.WHITE, 0.74f);
        g.setColor(new Color(8, 8, 10));
        g.fill(shape);
        final int edge = Math.max(3, Math.round(w * 0.026f));
        g.setPaint(new java.awt.GradientPaint(0, y, ArenaTheme.lift(frame), 0, y + h, frame.darker()));
        g.fill(new RoundRectangle2D.Float(x + edge, y + edge, w - 2 * edge, h - 2 * edge, arc - edge, arc - edge));

        final int inset = Math.round(w * 0.052f);
        final int innerW = w - 2 * inset;
        final int barH = Math.round(h * 0.062f);
        final int small = Math.max(2, Math.round(h * 0.006f));
        int cy = y + Math.round(h * 0.03f);

        paintBar(g, CardTranslation.getTranslatedName(e.card.getCurrentState().getName()), pale, x + inset, cy, innerW, barH);
        final forge.card.mana.ManaCost cost = e.card.getCurrentState().getManaCost();
        if (cost != null && !cost.isNoCost() && cost.getGlyphCount() > 0) {
            final int pip = Math.round(barH * 0.7f);
            g.drawImage(ArenaTheme.brightPips(cost, pip), x + inset + innerW - cost.getGlyphCount() * (pip + 1) - barH / 3, cy + (barH - pip) / 2 - 2, null);
        }
        cy += barH + small;

        final int artX = x + inset + small, artW = innerW - 2 * small, artH = Math.round(h * 0.43f);
        g.setColor(new Color(8, 8, 10));
        g.fillRect(artX - 1, cy - 1, artW + 2, artH + 2);
        if (img != null) {
            // the art box of a standard frame, cover-fitted
            final float sx = img.getWidth() * 0.1f, sy = img.getHeight() * 0.11f, sw = img.getWidth() * 0.8f, sh = img.getHeight() * 0.45f;
            final float fit = Math.max(artW / sw, artH / sh);
            final float cw = artW / fit, ch = artH / fit, cx0 = sx + (sw - cw) / 2f, cy0 = sy + (sh - ch) * 0.22f;
            g.drawImage(img, artX, cy, artX + artW, cy + artH, Math.round(cx0), Math.round(cy0), Math.round(cx0 + cw), Math.round(cy0 + ch), null);
        } else {
            g.setColor(ArenaTheme.NAVY);
            g.fillRect(artX, cy, artW, artH);
        }
        cy += artH + small;

        paintBar(g, e.abilityLabel, pale, x + inset, cy, innerW, barH);
        cy += barH + small;

        final int boxX = x + inset + small, boxW = innerW - 2 * small, boxBottom = y + h - Math.round(h * 0.036f), boxH = boxBottom - cy;
        final int boxArc = Math.round(w * 0.03f);
        g.setPaint(new java.awt.GradientPaint(0, cy, mix(new Color(0xF6F2E7), frame, 0.06f), 0, boxBottom, mix(new Color(0xE6E0CF), frame, 0.12f)));
        g.fillRoundRect(boxX, cy, boxW, boxH, boxArc, boxArc);
        g.setColor(new Color(16, 16, 20, 150));
        g.drawRoundRect(boxX, cy, boxW, boxH, boxArc, boxArc);

        final int pad = Math.max(8, w / 24);
        // the largest text that fits, starting from about 1.9% of the window's height
        List<List<String>> lines = null;
        for (int size = Math.max(13, Math.round(h * 0.041f)); size >= 9; size--) {
            g.setFont(ArenaTheme.rulesFont(java.awt.Font.PLAIN, size));
            lines = ArenaText.wrap(e.abilityText, g.getFontMetrics(), boxW - 2 * pad);
            if (ArenaText.height(lines, g.getFontMetrics()) <= boxH - 2 * pad) {
                break;
            }
        }
        g.setColor(new Color(12, 12, 14));
        final int textH = ArenaText.height(lines, g.getFontMetrics());
        ArenaText.draw(g, lines, boxX + pad, cy + Math.max(pad / 2, (boxH - textH) / 2), boxBottom - pad / 2);
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        if (!ArenaTheme.isMatchBoard() || entries.isEmpty()) {
            return;
        }
        final Graphics2D g = (Graphics2D) g0.create();
        try {
            final float visible = appear.get();
            if (visible <= 0f) {
                return;
            }
            if (visible < 1f) {
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(visible));
                g.translate(0, Math.round((1f - visible) * (PAD - 4)));
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            final java.awt.Font noteFont = ArenaTheme.displayFont(java.awt.Font.BOLD, 15f);
            g.setFont(noteFont);
            final FontMetrics fm = g.getFontMetrics();
            int nextX = PAD;
            for (int i = 0; i < entries.size(); i++) {
                final Entry e = entries.get(i);
                final int w = e.minor ? Math.round(cardW * MINOR_SCALE) : cardW;
                final int h = e.minor ? Math.round(cardH * MINOR_SCALE) : cardH;
                final int arc = Math.round(w * 0.07f);
                final int x = nextX;
                // a minor card hangs under its label, the rest under nothing
                final int cy = e.minor ? PAD + NOTE_LINE : PAD;
                nextX += w + GAP;
                ArenaTheme.paintGlow(g, x, cy + 3, w, h, arc, Color.BLACK, PAD - 3, 220);
                if (e.glow != ArenaTheme.GOLD && e.glow != ArenaTheme.HOVER) {
                    ArenaTheme.paintGlow(g, x, cy, w, h, arc, e.glow, PAD - 4, 200);
                }
                if (e.glow == ArenaTheme.GOLD) {
                    // lit in its own colour
                    ArenaTheme.paintGlow(g, x, cy, w, h, arc, ArenaTheme.frameLine(ArenaTheme.tileTint(e.card.getCurrentState())), PAD - 4, 135);
                }
                final RoundRectangle2D shape = new RoundRectangle2D.Float(x, cy, w, h, arc, arc);
                final Shape oldClip = g.getClip();
                g.clip(shape);
                g.setColor(Color.BLACK);
                g.fill(shape);
                final BufferedImage img = e.image == null ? null : e.image.getImage();
                final boolean ability = e.abilityText != null && !e.abilityText.isEmpty();
                int ix = x, iy = cy, iw = w, ih = h;
                if (ability) {
                    paintAbilityCard(g, e, img, shape, x, cy, w, h, arc);
                } else if (img != null) {
                    final float scale = Math.min(w / (float) img.getWidth(), h / (float) img.getHeight());
                    iw = Math.round(img.getWidth() * scale);
                    ih = Math.round(img.getHeight() * scale);
                    ix = x + (w - iw) / 2;
                    iy = cy + (h - ih) / 2;
                    g.drawImage(img, ix, iy, iw, ih, null);
                }
                g.setClip(oldClip);
                g.setColor(new Color(0, 0, 0, 220));
                g.draw(shape);
                if (e.moreOnStack > 0) {
                    // how many more wait beneath it
                    final int d = Math.max(26, w / 8);
                    g.setColor(new Color(6, 6, 8, 245));
                    g.fillRoundRect(x - d / 3, cy + h / 8, d, d, 8, 8);
                    g.setColor(new Color(255, 255, 255, 170));
                    g.drawRoundRect(x - d / 3, cy + h / 8, d, d, 8, 8);
                    g.setFont(ArenaTheme.numeralFont(java.awt.Font.BOLD, d * 0.62f));
                    final FontMetrics more = g.getFontMetrics();
                    final String n = "+" + e.moreOnStack;
                    g.setColor(Color.WHITE);
                    g.drawString(n, x - d / 3 + (d - more.stringWidth(n)) / 2, cy + h / 8 + (d + more.getAscent() - more.getDescent()) / 2);
                }
                g.setFont(noteFont);

                int y = e.minor ? PAD : PAD + h + 8;
                for (final String note : e.notes) {
                    String text = note;
                    while (text.length() > 2 && fm.stringWidth(text) + 24 > w + GAP - 4) {
                        text = text.substring(0, text.length() - 2) + "…";
                    }
                    final int tx = x + (w - fm.stringWidth(text)) / 2;
                    final int ty = y + (NOTE_LINE - 3 + fm.getAscent() - fm.getDescent()) / 2;
                    g.setColor(new Color(6, 7, 11, 205));
                    g.fillRoundRect(tx - 10, y, fm.stringWidth(text) + 20, NOTE_LINE - 3, NOTE_LINE - 3, NOTE_LINE - 3);
                    g.setColor(Color.WHITE);
                    g.drawString(text, tx, ty);
                    y += NOTE_LINE;
                }
            }
        } finally {
            g.dispose();
        }
    }
}
