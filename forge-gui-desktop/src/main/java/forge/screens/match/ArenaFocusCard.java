package forge.screens.match;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
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
    private static final float HEIGHT_ONE = 0.42f;
    private static final float HEIGHT_TWO = 0.34f;
    private static final float HEIGHT_THREE = 0.27f;
    private static final int PAD = 16;
    private static final int GAP = 28;
    private static final int NOTE_LINE = 22;
    private static ArenaFocusCard instance;

    /** One card in the row, with the notes printed under it. */
    private static final class Entry {
        final CardView card;
        final Color glow;
        final List<String> notes;
        final String abilityText; // printed over the card's text box when an ability is on the stack
        CachedCardImage image;

        Entry(final CardView card, final Color glow, final List<String> notes, final String abilityText) {
            this.card = card;
            this.glow = glow;
            this.notes = notes;
            this.abilityText = abilityText;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
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
        if (next.isEmpty()) {
            if (instance != null) {
                instance.setVisible(false);
            }
            return;
        }
        if (instance == null) {
            instance = new ArenaFocusCard();
            FView.SINGLETON_INSTANCE.getLpnDocument().add(instance, FView.TARGETING_LAYER);
        }
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        final int h = Math.round(lpn.getHeight() * (next.size() > 2 ? HEIGHT_THREE : next.size() > 1 ? HEIGHT_TWO : HEIGHT_ONE));
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
            noteLines = Math.max(noteLines, e.notes.size());
        }
        instance.entries.clear();
        instance.entries.addAll(next);
        instance.cardW = w;
        instance.cardH = h;
        final int width = next.size() * w + (next.size() - 1) * GAP + 2 * PAD;
        final int height = h + 2 * PAD + (noteLines == 0 ? 0 : 8 + noteLines * NOTE_LINE);
        instance.setBounds(lpn.getWidth() - width - 8, Math.round(lpn.getHeight() * 0.16f), width, height);
        instance.setVisible(true);
        instance.repaint();
    }

    private static List<Entry> collectEntries(final CMatchUI matchUI) {
        final List<Entry> entries = new ArrayList<>();
        final CardView promptCard = matchUI.getCPrompt().getView().getCardView();
        final GameView gameView = matchUI.getGameView();
        final StackItemView top = gameView == null ? null : gameView.peekStack();
        if (top != null && top.getSourceCard() != null) {
            final List<String> notes = describeStackItem(top);
            final int more = gameView.getStack().size() - 1;
            if (more > 0) {
                notes.add(Localizer.getInstance().getMessage("lblArenaMoreOnStack", more));
            }
            entries.add(new Entry(top.getSourceCard(), ArenaTheme.GOLD, notes, top.isAbility() ? abilityText(top) : null));
            // a trigger shows the card that set it off beside it (the spell cast, the creature that died...)
            for (final CardView cause : triggerCauses(top, gameView)) {
                if (!cause.equals(promptCard)) {
                    final List<String> causeNotes = new ArrayList<>();
                    causeNotes.add(Localizer.getInstance().getMessage("lblArenaTriggeredBy"));
                    entries.add(new Entry(cause, ArenaTheme.HOVER, causeNotes, null));
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
    private static List<String> describeStackItem(final StackItemView item) {
        final Localizer localizer = Localizer.getInstance();
        final List<String> notes = new ArrayList<>();
        String kind = localizer.getMessage(!item.isAbility() ? "lblArenaSpell" : item.isTrigger() ? "lblArenaTriggeredAbility" : "lblArenaAbility");
        if (item.getActivatingPlayer() != null) {
            kind += " • " + item.getActivatingPlayer().getName();
        }
        notes.add(kind);
        if (item.getTargetCards() != null) {
            for (final CardView target : item.getTargetCards()) {
                notes.add("→ " + CardTranslation.getTranslatedName(target.getCurrentState().getName()));
            }
        }
        if (item.getTargetPlayers() != null) {
            for (final PlayerView target : item.getTargetPlayers()) {
                notes.add("→ " + target.getName());
            }
        }
        return notes;
    }

    /**
     * Greys out the card's text box and prints the ability over it. {@code x, y, w, h} is the card
     * image as drawn; the box matches where the modern frame prints its rules text.
     */
    private static void paintAbilityText(final Graphics2D g, final String text, final int x, final int y, final int w, final int h) {
        final int bx = x + Math.round(w * 0.067f), by = y + Math.round(h * 0.627f);
        final int bw = Math.round(w * 0.854f), bh = Math.round(h * 0.299f);
        g.setColor(new Color(226, 226, 230, 250));
        g.fillRoundRect(bx, by, bw, bh, 8, 8);
        g.setColor(new Color(90, 90, 96));
        g.drawRoundRect(bx, by, bw, bh, 8, 8);
        final int pad = Math.max(5, w / 40);
        // the largest text that fits the box
        List<List<String>> lines = null;
        for (int size = Math.max(11, w / 17); size >= 9; size--) {
            g.setFont(FSkin.getFont(size).getBaseFont());
            lines = ArenaText.wrap(text, g.getFontMetrics(), bw - 2 * pad);
            if (ArenaText.height(lines, g.getFontMetrics()) <= bh - 2 * pad) {
                break;
            }
        }
        g.setColor(new Color(24, 24, 28));
        ArenaText.draw(g, lines, bx + pad, by + pad, by + bh - pad);
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        if (!ArenaTheme.isMatchBoard() || entries.isEmpty()) {
            return;
        }
        final Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(FSkin.getBoldFont(14).getBaseFont());
            final FontMetrics fm = g.getFontMetrics();
            final int w = cardW, h = cardH, arc = Math.round(w * 0.07f);
            for (int i = 0; i < entries.size(); i++) {
                final Entry e = entries.get(i);
                final int x = PAD + i * (w + GAP);
                ArenaTheme.paintGlow(g, x, PAD, w, h, arc, e.glow, PAD - 2, 230);
                final RoundRectangle2D shape = new RoundRectangle2D.Float(x, PAD, w, h, arc, arc);
                final Shape oldClip = g.getClip();
                g.clip(shape);
                g.setColor(Color.BLACK);
                g.fill(shape);
                final BufferedImage img = e.image == null ? null : e.image.getImage();
                int ix = x, iy = PAD, iw = w, ih = h;
                if (img != null) {
                    final float scale = Math.min(w / (float) img.getWidth(), h / (float) img.getHeight());
                    iw = Math.round(img.getWidth() * scale);
                    ih = Math.round(img.getHeight() * scale);
                    ix = x + (w - iw) / 2;
                    iy = PAD + (h - ih) / 2;
                    g.drawImage(img, ix, iy, iw, ih, null);
                }
                g.setClip(oldClip);
                g.setColor(e.glow);
                g.draw(shape);
                if (e.abilityText != null && !e.abilityText.isEmpty()) {
                    paintAbilityText(g, e.abilityText, ix, iy, iw, ih);
                    g.setFont(FSkin.getBoldFont(14).getBaseFont()); // the notes below are measured in this font
                }

                int y = PAD + h + 8;
                for (final String note : e.notes) {
                    String text = note;
                    while (text.length() > 2 && fm.stringWidth(text) + 24 > w + GAP - 4) {
                        text = text.substring(0, text.length() - 2) + "…";
                    }
                    final int pw = fm.stringWidth(text) + 24, ph = NOTE_LINE - 3;
                    final int px = x + (w - pw) / 2;
                    g.setColor(new Color(8, 10, 16, 225));
                    g.fillRoundRect(px, y, pw, ph, ph, ph);
                    g.setColor(ArenaTheme.GOLD_DIM);
                    g.drawRoundRect(px, y, pw, ph, ph, ph);
                    g.setColor(ArenaTheme.TEXT);
                    g.drawString(text, px + 12, y + (ph + fm.getAscent() - fm.getDescent()) / 2);
                    y += NOTE_LINE;
                }
            }
        } finally {
            g.dispose();
        }
    }
}
