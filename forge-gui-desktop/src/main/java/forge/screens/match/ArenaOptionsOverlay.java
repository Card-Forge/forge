package forge.screens.match;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import forge.CachedCardImage;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.toolbox.ArenaText;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FSkin;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.view.FView;
import forge.view.arcane.CardPanel;

/**
 * The Arena overlay for the decisions that are not a row of cards: picking from a few
 * options (modes, abilities, players, colours, numbers), putting options in order, and
 * plain questions with buttons. Like the card browser it dims the board, shows the
 * prompt's card on the right, can step aside with "View Battlefield", and takes every
 * click while it is up.
 */
@SuppressWarnings("serial")
public final class ArenaOptionsOverlay extends JPanel {
    /** Longer lists than this stay in the classic list, which can be searched and scrolled quickly. */
    public static final int MAX_OPTIONS = 40;
    /** Number lists up to this long still fit the grid of number buttons. */
    public static final int MAX_NUMBERS = 101;

    private static final int GAP = 10;
    private static final int FLOW_MAX_LABEL = 200;
    private static final int COLUMN_MAX_W = 760;
    private static final int PILL_H = 46;
    private static final int PAD_X = 22;

    private enum Kind {
        /** pick between min and max options; a single pick closes at once */
        CHOICE,
        /** drag every option into order, first at the top */
        ORDER,
        /** a question answered with one of a few buttons */
        BUTTONS
    }

    private final CMatchUI matchUI;
    private final Kind kind;
    private final String title;
    private final String message;
    private final String orderCaption;
    private final int min;
    private final int max;
    private final List<Option> options = new ArrayList<>();
    private final List<Option> picked = new ArrayList<>();
    private final JPanel list = new JPanel(null);
    private final JScrollPane scroller;
    private final FButton btnSubmit = new FButton();
    private final FButton btnCancel = new FButton();
    private final FButton btnView = new FButton();
    private final FCheckBox chkRemember;
    private final CardView focusCard;
    private CachedCardImage focusImage;
    private boolean flow;
    private boolean viewingBattlefield;
    private boolean cancelled;
    private int answer = -1;
    private Option dragging;
    private int dragGrabY;
    private boolean dragMoved;
    private Runnable onClose;
    private final ComponentAdapter followWindow = new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent e) {
            fitToWindow();
        }
    };

    /**
     * Lets the player pick between {@code min} and {@code max} of {@code choices}; returns the
     * picks in the order they were made, or an empty list when an optional choice is declined.
     */
    public static <T> List<T> choose(final CMatchUI matchUI, final String title, final String message, final int min, final int max,
            final List<T> choices, final List<T> selected, final Function<T, String> display, final CardView card) {
        final ArenaOptionsOverlay overlay = showAndWait(() -> {
            final ArenaOptionsOverlay o = new ArenaOptionsOverlay(matchUI, Kind.CHOICE, title, message, null, min, max, card, false);
            o.addOptions(choices, display);
            if (selected != null) {
                for (final Option option : o.options) {
                    if (selected.contains(option.value) && o.picked.size() < max) {
                        o.picked.add(option);
                    }
                }
            }
            return o;
        });
        final List<T> result = new ArrayList<>();
        if (overlay != null && !overlay.cancelled) {
            for (final Option option : overlay.picked) {
                result.add(choices.get(option.index));
            }
        }
        return result;
    }

    /** Lets the player drag {@code items} into order, the first at the top; returns them in that order and whether to remember it. */
    public static <T> Ordered<T> order(final CMatchUI matchUI, final String title, final String caption, final List<T> items,
            final CardView card, final boolean offerRemember) {
        final ArenaOptionsOverlay overlay = showAndWait(() -> {
            final ArenaOptionsOverlay o = new ArenaOptionsOverlay(matchUI, Kind.ORDER, title, null, caption, items.size(), items.size(), card, offerRemember);
            o.addOptions(items, null);
            return o;
        });
        final List<T> result = new ArrayList<>();
        if (overlay == null) {
            return new Ordered<>(new ArrayList<>(items), false);
        }
        for (final Option option : overlay.options) {
            result.add(items.get(option.index));
        }
        return new Ordered<>(result, overlay.chkRemember != null && overlay.chkRemember.isSelected());
    }

    /** Asks a question answered by one of {@code buttons}; returns the index of the one pressed. */
    public static int ask(final CMatchUI matchUI, final String title, final String message, final List<String> buttons,
            final int defaultIndex, final CardView card) {
        final ArenaOptionsOverlay overlay = showAndWait(() -> {
            final ArenaOptionsOverlay o = new ArenaOptionsOverlay(matchUI, Kind.BUTTONS, title, message, null, 1, 1, card, false);
            o.addOptions(buttons, null);
            if (defaultIndex >= 0 && defaultIndex < o.options.size()) {
                o.options.get(defaultIndex).primary = true;
            }
            return o;
        });
        return overlay == null ? defaultIndex : overlay.answer;
    }

    /** The order the player settled on, and whether they asked for it to be used again. */
    public record Ordered<T>(List<T> items, boolean remember) { }

    /** Opens an overlay and waits for it to close; on the event thread it keeps events flowing meanwhile. */
    private static ArenaOptionsOverlay showAndWait(final Supplier<ArenaOptionsOverlay> create) {
        if (SwingUtilities.isEventDispatchThread()) {
            final SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            final ArenaOptionsOverlay overlay = create.get();
            overlay.onClose = loop::exit;
            overlay.open();
            loop.enter();
            return overlay;
        }
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<ArenaOptionsOverlay> shown = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            final ArenaOptionsOverlay overlay = create.get();
            overlay.onClose = done::countDown;
            shown.set(overlay);
            overlay.open();
        });
        try {
            done.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return shown.get();
    }

    private ArenaOptionsOverlay(final CMatchUI matchUI, final Kind kind, final String title, final String message, final String orderCaption,
            final int min, final int max, final CardView card, final boolean offerRemember) {
        super(null);
        this.matchUI = matchUI;
        this.kind = kind;
        this.title = title;
        this.message = message == null ? null : message.replaceAll("(?s)<head>.*?</head>", "").replaceAll("<br\\s*/?>", "\n").replaceAll("<[^>]*>", "").trim();
        this.orderCaption = orderCaption;
        this.min = min;
        this.max = max;
        // a plain question shows only the card it was asked about
        this.focusCard = card != null || kind == Kind.BUTTONS ? card : matchUI.getCPrompt().getView().getCardView();

        setOpaque(false);
        // the board underneath must not react while the overlay covers it
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });
        addMouseWheelListener(e -> { });

        list.setOpaque(false);
        scroller = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(new EmptyBorder(0, 0, 0, 0));
        final JScrollBar bar = scroller.getVerticalScrollBar();
        bar.setUI(new ArenaCardBrowser.ThinScrollBarUI());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 0));
        bar.setUnitIncrement(24);
        add(scroller);

        final Localizer localizer = Localizer.getInstance();
        btnSubmit.setPrimary(true);
        btnSubmit.setFont(FSkin.getBoldFont(18));
        btnSubmit.addActionListener(e -> submit());
        btnSubmit.setVisible(kind == Kind.ORDER || (kind == Kind.CHOICE && max > 1));
        add(btnSubmit);

        btnCancel.setFont(FSkin.getBoldFont(14));
        btnCancel.setText(localizer.getMessage(kind == Kind.CHOICE && max > 1 ? "lblCancel" : "lblArenaDecline"));
        btnCancel.addActionListener(e -> cancel());
        btnCancel.setVisible(kind == Kind.CHOICE && min == 0);
        add(btnCancel);

        btnView.setFont(FSkin.getBoldFont(14));
        btnView.addActionListener(e -> toggleBattlefield());
        add(btnView);

        if (offerRemember) {
            chkRemember = new FCheckBox(localizer.getMessage("lblAlwaysUseThisOrder"));
            chkRemember.setToolTipText(localizer.getMessage("lblAlwaysUseThisOrderTooltip"));
            chkRemember.setSelected(true);
            chkRemember.setOpaque(false);
            chkRemember.setForeground(ArenaTheme.TEXT_DIM);
            add(chkRemember);
        } else {
            chkRemember = null;
        }

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (kind == Kind.BUTTONS) {
                    for (final Option option : options) {
                        if (option.primary) {
                            answer(option);
                        }
                    }
                } else {
                    submit();
                }
            }
        });
        getActionMap().put("escape", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (btnCancel.isVisible()) {
                    cancel();
                }
            }
        });
    }

    private <T> void addOptions(final List<T> values, final Function<T, String> display) {
        for (int i = 0; i < values.size(); i++) {
            final T value = values.get(i);
            final Option option = new Option(i, value, display != null ? display.apply(value) : describe(value));
            options.add(option);
            list.add(option);
        }
        // short labels such as numbers, colours or player names sit side by side
        final FontMetrics fm = getFontMetrics(optionFont());
        flow = kind != Kind.ORDER && (kind == Kind.BUTTONS || options.size() >= 3);
        for (final Option option : options) {
            if (option.label.contains("\n") || fm.stringWidth(option.label) > (kind == Kind.BUTTONS ? 320 : FLOW_MAX_LABEL)) {
                flow = false;
            }
        }
        updateButtons();
    }

    private static String describe(final Object value) {
        if (value instanceof CardView card) {
            return CardTranslation.getTranslatedName(card.getCurrentState().getName());
        }
        if (value instanceof PlayerView player) {
            return player.getName();
        }
        return String.valueOf(value);
    }

    private static Font optionFont() {
        return FSkin.getFont(17).getBaseFont();
    }

    private void open() {
        ArenaCardPreview.dismiss(); // a card still previewed from the hand would sit over the overlay's dimming
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        lpn.add(this, FView.OVERLAY_LAYER);
        lpn.moveToFront(this);
        lpn.addComponentListener(followWindow);
        fitToWindow();
        requestFocusInWindow();
    }

    private void close() {
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        lpn.removeComponentListener(followWindow);
        lpn.remove(this);
        lpn.repaint();
        if (onClose != null) {
            onClose.run();
        }
    }

    /** Covers the whole window, or only the View Battlefield button while the board is being looked at. */
    private void fitToWindow() {
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        if (viewingBattlefield) {
            setBounds(lpn.getWidth() - 220, 40, 210, 52);
        } else {
            setBounds(0, 0, lpn.getWidth(), lpn.getHeight());
        }
        revalidate();
        repaint();
        lpn.repaint();
    }

    private void toggleBattlefield() {
        viewingBattlefield = !viewingBattlefield;
        scroller.setVisible(!viewingBattlefield);
        btnSubmit.setVisible(!viewingBattlefield && (kind == Kind.ORDER || (kind == Kind.CHOICE && max > 1)));
        btnCancel.setVisible(!viewingBattlefield && kind == Kind.CHOICE && min == 0);
        if (chkRemember != null) {
            chkRemember.setVisible(!viewingBattlefield);
        }
        updateButtons();
        fitToWindow();
    }

    private void submit() {
        if (btnSubmit.isVisible() && btnSubmit.isEnabled()) {
            close();
        }
    }

    private void cancel() {
        cancelled = true;
        picked.clear();
        close();
    }

    private void answer(final Option option) {
        answer = option.index;
        close();
    }

    private void updateButtons() {
        final Localizer localizer = Localizer.getInstance();
        if (kind == Kind.ORDER) {
            btnSubmit.setText(localizer.getMessage("lblArenaSubmitPlain"));
            btnSubmit.setEnabled(true);
        } else {
            btnSubmit.setText(localizer.getMessage("lblArenaSubmit", picked.size()));
            btnSubmit.setEnabled(picked.size() >= min && picked.size() <= max);
        }
        btnView.setText(localizer.getMessage(viewingBattlefield ? "lblArenaReturn" : "lblArenaViewBattlefield"));
    }

    private void onOptionClicked(final Option option) {
        switch (kind) {
            case BUTTONS:
                answer(option);
                return;
            case ORDER:
                return;
            default:
                break;
        }
        if (max == 1) {
            picked.clear();
            picked.add(option);
            close();
            return;
        }
        if (picked.contains(option)) {
            picked.remove(option);
        } else if (picked.size() < max) {
            picked.add(option);
        }
        updateButtons();
        list.repaint();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        paintBackdrop((Graphics2D) g, getWidth(), getHeight());
    }

    @Override
    public void doLayout() {
        final int w = getWidth(), h = getHeight();
        if (viewingBattlefield) {
            btnView.setBounds(10, 8, 190, 36);
            return;
        }
        btnView.setBounds(w - 210, 48, 190, 36); // clear of the window's title bar

        final int reserved = focusCard == null ? 0 : focusWidth() + 80;
        final int areaW = Math.max(320, w - 80 - 2 * reserved);
        final int top = Math.round(h * (kind == Kind.BUTTONS ? 0.4f : 0.22f)) + messageHeight(Math.min(areaW, 900));
        final int buttonsY = Math.round(h * 0.78f);
        final int areaH = Math.max(PILL_H, buttonsY - 24 - top);
        final int listH = flow ? layoutFlow(areaW) : layoutColumn(areaW);
        final int listW = list.getPreferredSize().width;
        final boolean scrolls = listH > areaH;
        final int viewW = listW + (scrolls ? 14 : 0);
        scroller.setBounds((w - viewW) / 2, top, viewW, Math.min(listH, areaH));
        scroller.revalidate();

        final int buttonsAt = kind == Kind.BUTTONS ? top + Math.min(listH, areaH) + 24 : buttonsY;
        if (btnSubmit.isVisible() && btnCancel.isVisible()) {
            btnSubmit.setBounds(w / 2 - 230, buttonsAt, 220, 48);
            btnCancel.setBounds(w / 2 + 10, buttonsAt + 6, 170, 38);
        } else if (btnSubmit.isVisible()) {
            btnSubmit.setBounds((w - 220) / 2, buttonsAt, 220, 48);
        } else {
            btnCancel.setBounds((w - 170) / 2, buttonsAt + 6, 170, 38);
        }
        if (chkRemember != null) {
            final Dimension d = chkRemember.getPreferredSize();
            chkRemember.setBounds((w - d.width) / 2, buttonsAt + 58, d.width, d.height);
        }
    }

    /** Pills side by side, wrapping into centred rows. Returns the height they take. */
    private int layoutFlow(final int areaW) {
        final FontMetrics fm = getFontMetrics(optionFont());
        // equal widths read as a set; in a long list (numbers) a last odd one out ("Other...") keeps its own width
        final int last = options.size() - 1;
        final boolean ownLast = options.size() > 12;
        int widest = 0;
        for (int i = 0; i <= last; i++) {
            if (i < last || !ownLast) {
                widest = Math.max(widest, fm.stringWidth(options.get(i).label));
            }
        }
        final int minW = kind == Kind.BUTTONS ? 150 : 64;
        final int pillW = Math.max(minW, widest + 2 * PAD_X);
        final int lastW = ownLast ? Math.max(pillW, fm.stringWidth(options.get(last).label) + 2 * PAD_X) : pillW;
        final int maxRowW = Math.max(lastW, Math.min(areaW, 940));
        final int pillH = options.size() > 30 ? 40 : PILL_H, gap = options.size() > 30 ? 8 : GAP;

        // fill rows greedily, then centre each one
        final List<List<Option>> rows = new ArrayList<>();
        List<Option> row = new ArrayList<>();
        int rowW = 0, widestRow = 0;
        for (int i = 0; i <= last; i++) {
            final int ow = i == last ? lastW : pillW;
            if (!row.isEmpty() && rowW + gap + ow > maxRowW) {
                rows.add(row);
                row = new ArrayList<>();
                rowW = 0;
            }
            rowW += (row.isEmpty() ? 0 : gap) + ow;
            widestRow = Math.max(widestRow, rowW);
            row.add(options.get(i));
        }
        rows.add(row);
        for (int r = 0; r < rows.size(); r++) {
            final List<Option> cells = rows.get(r);
            int cellsW = (cells.size() - 1) * gap;
            for (final Option option : cells) {
                cellsW += option == options.get(last) ? lastW : pillW;
            }
            int x = (widestRow - cellsW) / 2;
            for (final Option option : cells) {
                final int ow = option == options.get(last) ? lastW : pillW;
                option.setBounds(x, r * (pillH + gap), ow, pillH);
                x += ow + gap;
            }
        }
        final int height = rows.size() * pillH + (rows.size() - 1) * gap;
        list.setPreferredSize(new Dimension(widestRow, height));
        return height;
    }

    /** One pill per row, all the width of the longest, tall enough for wrapped text. */
    private int layoutColumn(final int areaW) {
        final FontMetrics fm = getFontMetrics(optionFont());
        final int badge = kind == Kind.ORDER || (kind == Kind.CHOICE && max > 1) ? 34 : 0;
        int widest = 0;
        for (final Option option : options) {
            for (final String line : option.label.split("\n")) {
                widest = Math.max(widest, fm.stringWidth(line));
            }
        }
        final int grip = kind == Kind.ORDER ? 26 : 0;
        final int pillW = Math.min(Math.min(areaW, COLUMN_MAX_W), Math.max(260, widest + 2 * PAD_X + badge + grip));
        final int textW = pillW - 2 * PAD_X - badge - grip;
        int y = 0;
        for (final Option option : options) {
            option.lines = ArenaText.wrap(option.label, fm, textW);
            option.textX = PAD_X + badge;
            final int ph = Math.max(PILL_H, ArenaText.height(option.lines, fm) + 22);
            option.slotY = y;
            if (option != dragging) {
                option.setBounds(0, y, pillW, ph);
            } else {
                option.setSize(pillW, ph);
            }
            y += ph + GAP;
        }
        final int height = Math.max(0, y - GAP);
        list.setPreferredSize(new Dimension(pillW, height));
        return height;
    }

    private int messageHeight(final int width) {
        if (message == null || message.isEmpty() || kind != Kind.BUTTONS) {
            return 0;
        }
        final FontMetrics fm = getFontMetrics(FSkin.getFont(20).getBaseFont());
        return ArenaText.height(ArenaText.wrap(message, fm, width), fm) + 28;
    }

    private int focusWidth() {
        return focusCard == null ? 0 : Math.round(getHeight() * 0.4f / CardPanel.ASPECT_RATIO);
    }

    private void paintBackdrop(final Graphics2D g, final int w, final int h) {
        if (viewingBattlefield) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(4, 6, 11, kind == Kind.BUTTONS ? 175 : 205));
        g.fillRect(0, 0, w, h);

        // the title with a fading rule beneath it
        g.setFont(FSkin.getBoldFont(kind == Kind.BUTTONS ? 34 : 40).getBaseFont());
        FontMetrics fm = g.getFontMetrics();
        final int titleTop = Math.round(h * (kind == Kind.BUTTONS ? 0.3f : 0.075f));
        final int titleY = titleTop + fm.getAscent();
        String heading = title == null ? "" : title;
        while (heading.length() > 1 && fm.stringWidth(heading) > w - 120) {
            heading = heading.substring(0, heading.length() - 2) + "…";
        }
        g.setColor(ArenaTheme.TEXT);
        g.drawString(heading, (w - fm.stringWidth(heading)) / 2, titleY);
        final int ruleY = titleY + 12, ruleW = Math.min(w / 2, 640);
        g.setPaint(new GradientPaint((w - ruleW) / 2f, 0, ArenaTheme.alpha(ArenaTheme.GOLD, 0), w / 2f, 0, ArenaTheme.GOLD, true));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine((w - ruleW) / 2, ruleY, (w + ruleW) / 2, ruleY);

        final int reserved = focusCard == null ? 0 : focusWidth() + 80;
        final int textW = Math.min(Math.max(320, w - 80 - 2 * reserved), 900);
        if (kind == Kind.BUTTONS) {
            // the question itself, centred under the title
            if (message != null && !message.isEmpty()) {
                g.setFont(FSkin.getFont(20).getBaseFont());
                g.setColor(ArenaTheme.TEXT);
                drawCentred(g, ArenaText.wrap(message, g.getFontMetrics(), textW), w / 2, Math.round(h * 0.4f));
            }
        } else {
            String instruction = message;
            if (kind == Kind.ORDER) {
                instruction = Localizer.getInstance().getMessage("lblArenaDragToOrder", orderCaption == null ? "" : orderCaption);
            } else if (instruction == null || instruction.isEmpty() || sameWords(instruction, title)) {
                instruction = null;
            }
            if (instruction != null) {
                g.setFont(FSkin.getFont(17).getBaseFont());
                fm = g.getFontMetrics();
                g.setColor(ArenaTheme.TEXT_DIM);
                final List<List<String>> lines = ArenaText.wrap(instruction, fm, textW);
                drawCentred(g, lines.size() > 3 ? lines.subList(0, 3) : lines, w / 2, ruleY + 10);
            }
        }

        // the card the decision is about, large on the right
        if (focusCard != null) {
            final int fh = Math.round(h * 0.4f), fw = focusWidth();
            if (focusImage == null) {
                focusImage = new CachedCardImage(focusCard, matchUI.getLocalPlayers(), fw, fh) {
                    @Override
                    public void onImageFetched() {
                        ArenaOptionsOverlay.this.repaint();
                    }
                };
            }
            ArenaCardBrowser.paintCard(g, focusImage.getImage(), w - fw - 48, Math.round(h * 0.18f), fw, fh, ArenaTheme.PLAYABLE, 220);
        }
    }

    private static boolean sameWords(final String a, final String b) {
        return b != null && a.replaceAll("[^\\p{L}\\p{N}]", "").equalsIgnoreCase(b.replaceAll("[^\\p{L}\\p{N}]", ""));
    }

    /** Draws wrapped lines each centred on {@code cx}, from {@code y} down. */
    private static void drawCentred(final Graphics2D g, final List<List<String>> lines, final int cx, final int y) {
        final FontMetrics fm = g.getFontMetrics();
        int ly = y;
        for (final List<String> line : lines) {
            ArenaText.draw(g, List.of(line), cx - ArenaText.width(line, fm) / 2, ly, Integer.MAX_VALUE);
            ly += fm.getHeight();
        }
    }

    private void startDrag(final Option option, final MouseEvent e) {
        if (kind != Kind.ORDER) {
            return;
        }
        dragging = option;
        dragGrabY = e.getY();
        dragMoved = false;
        list.setComponentZOrder(option, 0);
    }

    /** Moves the dragged option with the mouse; the others make room as it passes them. */
    private void drag(final Option option, final MouseEvent e) {
        if (dragging != option) {
            return;
        }
        if (!dragMoved && Math.abs(e.getY() - dragGrabY) < 5) {
            return;
        }
        dragMoved = true;
        final Point p = SwingUtilities.convertPoint(option, e.getPoint(), list);
        final int y = Math.max(-option.getHeight() / 2, Math.min(list.getHeight() - option.getHeight() / 2, p.y - dragGrabY));
        option.setLocation(option.getX(), y);
        final int centre = y + option.getHeight() / 2;
        int index = 0;
        for (final Option other : options) {
            if (other != option && other.slotY + other.getHeight() / 2 < centre) {
                index++;
            }
        }
        if (options.indexOf(option) != index) {
            options.remove(option);
            options.add(index, option);
            layoutColumn(list.getWidth());
        }
        list.repaint();
    }

    private void endDrag(final Option option) {
        if (dragging != option) {
            return;
        }
        dragging = null;
        layoutColumn(list.getWidth());
        list.repaint();
    }

    /** One option: a navy pill, gold once picked, with its pick number (or place in the order) on the left. */
    private final class Option extends JComponent {
        private final int index;
        private final Object value;
        private final String label;
        private List<List<String>> lines;
        private int textX = PAD_X;
        private int slotY;
        private boolean primary;
        private boolean hover;
        private boolean pressed;

        Option(final int index, final Object value, final String label) {
            this.index = index;
            this.value = value;
            this.label = label == null ? "" : label.trim();
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hover = true;
                    final CardView card = value instanceof CardView c ? c : value instanceof SpellAbilityView sa ? sa.getHostCard() : null;
                    if (card != null) {
                        matchUI.setCard(card);
                    }
                    repaint();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    pressed = true;
                    startDrag(Option.this, e);
                    repaint();
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    pressed = false;
                    if (kind == Kind.ORDER) {
                        endDrag(Option.this);
                    } else if (contains(e.getPoint())) {
                        onOptionClicked(Option.this);
                    }
                    repaint();
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(final MouseEvent e) {
                    drag(Option.this, e);
                }
            });
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                final int w = getWidth(), h = getHeight();
                final int pick = kind == Kind.ORDER ? options.indexOf(this) + 1 : picked.indexOf(this) + 1;
                final boolean isPicked = kind == Kind.CHOICE && pick > 0;
                ArenaTheme.paintButton(g, w, h, primary, hover || this == dragging, pressed, false, isPicked, true);
                if (kind == Kind.CHOICE && !isPicked && max > 1 && picked.size() >= max) {
                    g.setColor(new Color(4, 6, 11, 120)); // the limit is reached; the rest wait dimmed
                    g.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, Math.min(h - 2, 26), Math.min(h - 2, 26)));
                }
                g.setFont(optionFont());
                final FontMetrics fm = g.getFontMetrics();
                if ((kind == Kind.ORDER || (kind == Kind.CHOICE && max > 1)) && !flow && pick > 0) {
                    // the place in the order, in a small disc
                    final int d = 24, bx = PAD_X - 6, by = (h - d) / 2;
                    g.setColor(kind == Kind.ORDER ? ArenaTheme.NIGHT : ArenaTheme.GOLD_BRIGHT);
                    g.fillOval(bx, by, d, d);
                    g.setColor(ArenaTheme.GOLD);
                    g.drawOval(bx, by, d, d);
                    g.setFont(FSkin.getBoldFont(13).getBaseFont());
                    final FontMetrics bfm = g.getFontMetrics();
                    final String n = String.valueOf(pick);
                    g.setColor(kind == Kind.ORDER ? ArenaTheme.GOLD_BRIGHT : ArenaTheme.NIGHT);
                    g.drawString(n, bx + (d - bfm.stringWidth(n)) / 2, by + (d + bfm.getAscent() - bfm.getDescent()) / 2);
                    g.setFont(optionFont());
                }
                g.setColor(primary ? Color.WHITE : isPicked ? ArenaTheme.NIGHT : ArenaTheme.TEXT);
                if (flow || lines == null) {
                    String text = label;
                    while (text.length() > 1 && fm.stringWidth(text) > w - 16) {
                        text = text.substring(0, text.length() - 2) + "…";
                    }
                    g.drawString(text, (w - fm.stringWidth(text)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
                } else {
                    final int textH = ArenaText.height(lines, fm);
                    ArenaText.draw(g, lines, textX, (h - textH) / 2, h - 4);
                }
                if (kind == Kind.ORDER) {
                    // a grip on the right says the row can be dragged
                    g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, hover || this == dragging ? 200 : 110));
                    final int gx = w - PAD_X, gy = h / 2;
                    for (int i = -1; i <= 1; i++) {
                        g.fillRect(gx - 9, gy + i * 5 - 1, 14, 2);
                    }
                }
            } finally {
                g.dispose();
            }
        }
    }
}
