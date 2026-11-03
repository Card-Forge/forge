package forge.screens.match;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SecondaryLoop;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

import org.apache.commons.text.WordUtils;

import forge.CachedCardImage;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FButton;
import forge.toolbox.FSkin;
import forge.toolbox.MouseTriggerEvent;
import forge.util.Localizer;
import forge.view.FView;
import forge.view.arcane.CardPanel;

/**
 * Arena-style card browser: the board dims behind a title, one row of large cards that
 * scrolls sideways, the prompt's card on the right, and a Submit button just above the
 * local portrait. "View Battlefield" shrinks it to that one button until pressed again.
 * It is drawn inside the match window and takes every click while it covers the board.
 */
@SuppressWarnings("serial")
public final class ArenaCardBrowser extends JPanel {
    private static final float CARD_HEIGHT = 0.36f; // of the window
    private static final int GLOW = 14;
    private static final int LIFT = 18;

    /** What the browser is for. */
    private enum Mode {
        /** pick cards locally, then hand the picks back to the caller */
        CHOOSE,
        /** nothing to pick; the cards are only being shown */
        REVEAL,
        /** look through a zone; a click plays the card, as a click in the zone window would */
        ZONE,
        /** the game's current input is picking cards from hidden zones; clicks and Submit go to it */
        SELECTION,
        /** put cards on the top and/or bottom of a library by dragging them either side of a gap */
        ARRANGE
    }

    private static ArenaCardBrowser selectionBrowser;

    private final CMatchUI matchUI;
    private final Mode mode;
    private final String title;
    private final String subtitle;
    private final int min;
    private final int max;
    private final List<CardView> cards;
    private final List<CardView> chosen = new ArrayList<>();
    private final List<CardItem> items = new ArrayList<>();
    private final JPanel strip = new JPanel(null);
    private final JScrollPane scroller;
    private final FButton btnSubmit = new FButton();
    private final FButton btnSecondary = new FButton();
    private final FButton btnView = new FButton();
    private final CardView focusCard;
    private CachedCardImage focusImage;
    private boolean viewingBattlefield;
    private CardView played;
    private MouseEvent playedEvent;
    private Runnable onClose;
    // ARRANGE: the two sides, left to right; leftmost is the top of the library, rightmost the very bottom
    private final List<CardView> topSide = new ArrayList<>();
    private final List<CardView> bottomSide = new ArrayList<>();
    private boolean allowTop;
    private boolean allowBottom;
    private String topLabel; // ARRANGE: what the left side is called when it is not the top of a library
    private Rectangle topArea;
    private Rectangle bottomArea;
    private int gapCenterX;
    private int itemStep;
    private CardItem dragItem;
    private Point dragGrab;
    private boolean dragMoved;
    private int dropMarkerX = -1;
    private final ComponentAdapter followWindow = new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent e) {
            fitToWindow();
        }
    };

    /**
     * Lets the player pick between {@code min} and {@code max} of {@code cards} and returns
     * them in the order picked. Blocks until Submit.
     */
    public static List<CardView> choose(final CMatchUI matchUI, final String title, final String subtitle,
            final int min, final int max, final List<CardView> cards, final Collection<CardView> selected) {
        final ArenaCardBrowser browser = showAndWait(() -> new ArenaCardBrowser(matchUI, Mode.CHOOSE, title, subtitle, cards, min, max, selected));
        return browser == null ? new ArrayList<>() : new ArrayList<>(browser.chosen);
    }

    /** Shows cards that are only being revealed: nothing to pick, just Done. Blocks until Done. */
    public static void reveal(final CMatchUI matchUI, final String message, final List<CardView> cards) {
        final String title = Localizer.getInstance().getMessage("lblRevealed");
        showAndWait(() -> new ArenaCardBrowser(matchUI, Mode.REVEAL, title, message, cards, 0, 0, null));
    }

    /** Shows a player's zone, newest card first; clicking a card closes the browser and plays it as a click on the zone would. */
    public static void browseZone(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final List<CardView> cards = new ArrayList<>();
        if (player.getCards(zone) != null) {
            for (final CardView c : player.getCards(zone)) {
                cards.add(0, c);
            }
        }
        final Localizer localizer = Localizer.getInstance();
        final String title = WordUtils.capitalize(zone.getTranslatedName());
        final String subtitle = localizer.getMessage("lblArenaZoneSubtitle", player.getName(), cards.size());
        final ArenaCardBrowser browser = new ArenaCardBrowser(matchUI, Mode.ZONE, title, subtitle, cards, 0, 0, null);
        browser.onClose = () -> {
            if (browser.played != null) {
                matchUI.getGameController().selectCard(browser.played, null, new MouseTriggerEvent(browser.playedEvent));
            }
        };
        browser.open();
    }

    /**
     * Shows the cards the game's current input lets the player pick from hidden zones
     * (the top of a library, a graveyard...). Replaces the cards if it is already open.
     */
    public static void showSelection(final CMatchUI matchUI, final String title, final List<CardView> cards) {
        if (selectionBrowser != null) {
            if (selectionBrowser.cards.equals(cards)) {
                refreshSelection();
                return;
            }
            closeSelection();
        }
        selectionBrowser = new ArenaCardBrowser(matchUI, Mode.SELECTION, title, null, cards, 0, 0, null);
        selectionBrowser.open();
    }

    /** Re-reads which cards are chosen and what the prompt buttons say. */
    public static void refreshSelection() {
        if (selectionBrowser != null) {
            selectionBrowser.updateButtons();
            selectionBrowser.repaint();
        }
    }

    public static void closeSelection() {
        if (selectionBrowser != null) {
            final ArenaCardBrowser browser = selectionBrowser;
            selectionBrowser = null;
            browser.close();
        }
    }

    /**
     * Lets the player put {@code cards} on the top and/or bottom of their library by dragging
     * them either side of a gap: left of it is the top (leftmost on top), right of it the
     * bottom (rightmost at the very bottom). A click moves a card across. Returns {top, bottom}.
     */
    public static List<List<CardView>> arrange(final CMatchUI matchUI, final String subtitle, final List<CardView> cards,
            final boolean toTop, final boolean toBottom) {
        final String title = Localizer.getInstance().getMessage(toTop && toBottom ? "lblArenaTopOrBottom"
                : toTop ? "lblArenaOrderTop" : "lblArenaOrderBottom");
        final ArenaCardBrowser browser = showAndWait(() -> {
            final ArenaCardBrowser b = new ArenaCardBrowser(matchUI, Mode.ARRANGE, title, subtitle, cards, 0, 0, null);
            b.allowTop = toTop;
            b.allowBottom = toBottom;
            (toTop ? b.topSide : b.bottomSide).addAll(cards);
            return b;
        });
        if (browser == null) {
            return List.of(toTop ? new ArrayList<>(cards) : new ArrayList<>(), toTop ? new ArrayList<>() : new ArrayList<>(cards));
        }
        return List.of(new ArrayList<>(browser.topSide), new ArrayList<>(browser.bottomSide));
    }

    /**
     * Lets the player drag {@code cards} into order along one row, the leftmost being
     * {@code caption} ("Damaged first", "Closest to top"...). Returns them left to right.
     */
    public static List<CardView> order(final CMatchUI matchUI, final String title, final String caption, final List<CardView> cards) {
        final String subtitle = Localizer.getInstance().getMessage("lblArenaDragToOrder", caption);
        final ArenaCardBrowser browser = showAndWait(() -> {
            final ArenaCardBrowser b = new ArenaCardBrowser(matchUI, Mode.ARRANGE, title, subtitle, cards, 0, 0, null);
            b.allowTop = true;
            b.topLabel = Localizer.getInstance().getMessage("lblArenaFirstLeft", caption);
            b.topSide.addAll(cards);
            return b;
        });
        return browser == null ? new ArrayList<>(cards) : new ArrayList<>(browser.topSide);
    }

    /** Opens a browser and waits for it to close; on the event thread it keeps events flowing meanwhile. */
    private static ArenaCardBrowser showAndWait(final Supplier<ArenaCardBrowser> create) {
        if (SwingUtilities.isEventDispatchThread()) {
            final SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            final ArenaCardBrowser browser = create.get();
            browser.onClose = loop::exit;
            browser.open();
            loop.enter();
            return browser;
        }
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<ArenaCardBrowser> shown = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            final ArenaCardBrowser browser = create.get();
            browser.onClose = done::countDown;
            shown.set(browser);
            browser.open();
        });
        try {
            done.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return shown.get();
    }

    private ArenaCardBrowser(final CMatchUI matchUI, final Mode mode, final String title, final String subtitle,
            final List<CardView> cards, final int min, final int max, final Collection<CardView> selected) {
        super(null);
        this.matchUI = matchUI;
        this.mode = mode;
        this.title = title;
        this.subtitle = subtitle;
        this.cards = new ArrayList<>(cards);
        this.min = min;
        this.max = max;
        this.focusCard = matchUI.getCPrompt().getView().getCardView();
        if (selected != null) {
            chosen.addAll(selected);
        }

        setOpaque(false);
        // the board underneath must not react while the browser covers it
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });
        addMouseWheelListener(e -> { });

        strip.setOpaque(false);
        for (final CardView card : cards) {
            final CardItem item = new CardItem(card);
            items.add(item);
            if (mode == Mode.ARRANGE) {
                add(item);
            } else {
                strip.add(item);
            }
        }
        scroller = new JScrollPane(strip, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(new EmptyBorder(0, 0, 0, 0));
        final JScrollBar rowBar = scroller.getHorizontalScrollBar();
        rowBar.setUI(new ThinScrollBarUI());
        rowBar.setOpaque(false);
        rowBar.setPreferredSize(new Dimension(0, 10));
        rowBar.setUnitIncrement(40);
        // the wheel scrolls the row sideways
        scroller.setWheelScrollingEnabled(false);
        scroller.addMouseWheelListener(e -> {
            final JScrollBar bar = scroller.getHorizontalScrollBar();
            bar.setValue(bar.getValue() + e.getUnitsToScroll() * 25);
        });
        scroller.setVisible(mode != Mode.ARRANGE);
        add(scroller);

        btnSubmit.setPrimary(true);
        btnSubmit.setFont(FSkin.getBoldFont(18));
        btnSubmit.addActionListener(e -> submit());
        add(btnSubmit);

        btnSecondary.setFont(FSkin.getBoldFont(14));
        btnSecondary.addActionListener(e -> matchUI.getGameController().selectButtonCancel());
        btnSecondary.setVisible(mode == Mode.SELECTION);
        add(btnSecondary);

        btnView.setFont(FSkin.getBoldFont(14));
        btnView.addActionListener(e -> toggleBattlefield());
        add(btnView);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                submit();
            }
        });
        getActionMap().put("escape", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (mode == Mode.REVEAL || mode == Mode.ZONE || (mode == Mode.CHOOSE && min == 0)) {
                    chosen.clear();
                    close();
                }
            }
        });

        updateButtons();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        paintBackdrop((Graphics2D) g, getWidth(), getHeight());
    }

    @Override
    public void doLayout() {
        layoutContents(getWidth(), getHeight());
    }

    private void open() {
        ArenaCardPreview.dismiss(); // a card still previewed from the hand would sit over the browser's dimming
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

    private void submit() {
        if (!btnSubmit.isEnabled()) {
            return;
        }
        if (mode == Mode.SELECTION) {
            matchUI.getGameController().selectButtonOk(); // the input closes the browser when it is done
            return;
        }
        close();
    }

    private void toggleBattlefield() {
        viewingBattlefield = !viewingBattlefield;
        scroller.setVisible(!viewingBattlefield);
        btnSubmit.setVisible(!viewingBattlefield);
        btnSecondary.setVisible(!viewingBattlefield && mode == Mode.SELECTION);
        updateButtons();
        fitToWindow();
    }

    private int chosenCount() {
        if (mode != Mode.SELECTION) {
            return chosen.size();
        }
        int n = 0;
        for (final CardView card : cards) {
            if (matchUI.isHighlighted(card)) {
                n++;
            }
        }
        return n;
    }

    private void updateButtons() {
        final Localizer localizer = Localizer.getInstance();
        switch (mode) {
            case CHOOSE:
                btnSubmit.setText(localizer.getMessage("lblArenaSubmit", chosen.size()));
                btnSubmit.setEnabled(chosen.size() >= min && chosen.size() <= max);
                break;
            case ARRANGE:
                btnSubmit.setText(localizer.getMessage("lblArenaSubmitPlain"));
                btnSubmit.setEnabled(true);
                break;
            case SELECTION:
                // mirror the prompt's own buttons, which the input keeps up to date
                final FButton ok = matchUI.getCPrompt().getView().getBtnOK();
                final FButton cancel = matchUI.getCPrompt().getView().getBtnCancel();
                btnSubmit.setText(localizer.getMessage("lblArenaSubmit", chosenCount()));
                btnSubmit.setEnabled(ok.isEnabled());
                btnSecondary.setText(cancel.getText());
                btnSecondary.setEnabled(cancel.isEnabled());
                break;
            default:
                btnSubmit.setText(localizer.getMessage("lblArenaDone"));
                btnSubmit.setEnabled(true);
                break;
        }
        btnView.setText(localizer.getMessage(viewingBattlefield ? "lblArenaReturn" : "lblArenaViewBattlefield"));
    }

    private void onCardClicked(final CardItem item, final MouseEvent e) {
        switch (mode) {
            case ZONE:
                played = item.card;
                playedEvent = e;
                close();
                return;
            case SELECTION:
                matchUI.getGameController().selectCard(item.card, null, new MouseTriggerEvent(e));
                return;
            case REVEAL:
            case ARRANGE:
                return;
            default:
                break;
        }
        if (chosen.contains(item.card)) {
            chosen.remove(item.card);
        } else if (max == 1) {
            chosen.clear();
            chosen.add(item.card);
        } else if (chosen.size() < max) {
            chosen.add(item.card);
        }
        updateButtons();
        strip.repaint();
    }

    private boolean isChosen(final CardView card) {
        return mode == Mode.SELECTION ? matchUI.isHighlighted(card) : chosen.contains(card);
    }

    private int cardHeight() {
        return Math.round(getHeight() * CARD_HEIGHT);
    }

    private int focusWidth() {
        return focusCard == null ? 0 : Math.round(getHeight() * 0.4f / CardPanel.ASPECT_RATIO);
    }

    private void layoutContents(final int w, final int h) {
        if (viewingBattlefield) {
            btnView.setBounds(10, 8, 190, 36);
            return;
        }
        if (mode == Mode.ARRANGE) {
            layoutArrange(w, h);
            btnSubmit.setBounds((w - 220) / 2, Math.round(h * 0.68f), 220, 48);
            btnView.setBounds(w - 210, 48, 190, 36);
            return;
        }
        final int ch = cardHeight(), cw = Math.round(ch / CardPanel.ASPECT_RATIO);
        final int itemW = cw + 2 * GLOW, itemH = ch + 2 * GLOW + LIFT;
        final int step = itemW - 4;
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setBounds(i * step, 0, itemW, itemH);
        }
        final int stripW = items.isEmpty() ? 0 : (items.size() - 1) * step + itemW;
        strip.setPreferredSize(new Dimension(stripW, itemH));
        final int reserved = focusCard == null ? 0 : focusWidth() + 80;
        final int available = Math.max(itemW, w - 80 - 2 * reserved);
        final int viewW = Math.min(stripW, available);
        final boolean scrolls = stripW > available;
        scroller.setBounds((w - viewW) / 2, Math.round(h * 0.2f), viewW, itemH + (scrolls ? 12 : 0));
        scroller.revalidate();

        final int buttonsY = Math.round(h * 0.68f);
        if (btnSecondary.isVisible()) {
            btnSubmit.setBounds(w / 2 - 230, buttonsY, 220, 48);
            btnSecondary.setBounds(w / 2 + 10, buttonsY + 6, 170, 38);
        } else {
            btnSubmit.setBounds((w - 220) / 2, buttonsY, 220, 48);
        }
        btnView.setBounds(w - 210, 48, 190, 36); // clear of the window's title bar
    }

    private CardItem itemFor(final CardView card) {
        for (final CardItem item : items) {
            if (item.card == card) {
                return item;
            }
        }
        return null;
    }

    /** One row: the top side, a gap with a divider, then the bottom side; an empty side keeps one slot to drop into. */
    private void layoutArrange(final int w, final int h) {
        final boolean both = allowTop && allowBottom;
        int ch = cardHeight();
        int itemW, itemH, gapW, rowW;
        while (true) {
            final int cw = Math.round(ch / CardPanel.ASPECT_RATIO);
            itemW = cw + 2 * GLOW;
            itemH = ch + 2 * GLOW + LIFT;
            itemStep = itemW - 4;
            gapW = both ? Math.max(90, cw / 2) : 0;
            final int slots = (allowTop ? Math.max(1, topSide.size()) : 0) + (allowBottom ? Math.max(1, bottomSide.size()) : 0);
            rowW = slots * itemStep + gapW;
            if (rowW <= w - 120 || ch <= 90) {
                break;
            }
            ch -= 8;
        }
        int x = (w - rowW) / 2;
        final int y = Math.round(h * 0.24f);
        if (allowTop) {
            topArea = new Rectangle(x, y, Math.max(1, topSide.size()) * itemStep, itemH);
            for (final CardView card : topSide) {
                placeArranged(card, x, y, itemW, itemH);
                x += itemStep;
            }
            x = topArea.x + topArea.width;
        }
        gapCenterX = x + gapW / 2;
        x += gapW;
        if (allowBottom) {
            bottomArea = new Rectangle(x, y, Math.max(1, bottomSide.size()) * itemStep, itemH);
            for (final CardView card : bottomSide) {
                placeArranged(card, x, y, itemW, itemH);
                x += itemStep;
            }
        }
    }

    private void placeArranged(final CardView card, final int x, final int y, final int itemW, final int itemH) {
        final CardItem item = itemFor(card);
        if (item != null && item != dragItem) {
            item.setBounds(x, y, itemW, itemH);
        }
    }

    private boolean dropsOnTop(final int x) {
        return allowTop && (!allowBottom || x < gapCenterX);
    }

    /** Where a card dropped at {@code x} goes in its side, counting only the other cards. */
    private int dropIndex(final List<CardView> side, final int x) {
        int index = 0;
        for (final CardView card : side) {
            final CardItem item = itemFor(card);
            if (item != null && item != dragItem && item.getX() + item.getWidth() / 2 < x) {
                index++;
            }
        }
        return index;
    }

    private void startDrag(final CardItem item, final MouseEvent e) {
        dragItem = item;
        dragGrab = e.getPoint();
        dragMoved = false;
        setComponentZOrder(item, 0);
    }

    private void drag(final CardItem item, final MouseEvent e) {
        if (dragItem != item) {
            return;
        }
        final Point p = SwingUtilities.convertPoint(item, e.getPoint(), this);
        if (!dragMoved && Math.abs(e.getX() - dragGrab.x) + Math.abs(e.getY() - dragGrab.y) < 6) {
            return;
        }
        dragMoved = true;
        item.setLocation(p.x - dragGrab.x, p.y - dragGrab.y);
        // mark where it would land
        final int cx = p.x;
        final List<CardView> side = dropsOnTop(cx) ? topSide : bottomSide;
        final Rectangle area = dropsOnTop(cx) ? topArea : bottomArea;
        final int index = dropIndex(side, cx);
        int marker = area.x;
        int seen = 0;
        for (final CardView card : side) {
            final CardItem other = itemFor(card);
            if (other == null || other == item) {
                continue;
            }
            if (seen == index) {
                break;
            }
            marker = other.getX() + other.getWidth();
            seen++;
        }
        dropMarkerX = marker;
        repaint();
    }

    private void endDrag(final CardItem item, final MouseEvent e) {
        if (dragItem != item) {
            return;
        }
        final Point p = SwingUtilities.convertPoint(item, e.getPoint(), this);
        final boolean wasOnTop = topSide.contains(item.card);
        if (dragMoved) {
            final boolean toTop = dropsOnTop(p.x);
            final List<CardView> target = toTop ? topSide : bottomSide;
            final int index = dropIndex(target, p.x);
            topSide.remove(item.card);
            bottomSide.remove(item.card);
            target.add(Math.min(index, target.size()), item.card);
        } else if (allowTop && allowBottom) {
            // a click sends the card across the gap, next to it
            if (wasOnTop) {
                topSide.remove(item.card);
                bottomSide.add(0, item.card);
            } else {
                bottomSide.remove(item.card);
                topSide.add(item.card);
            }
        }
        dragItem = null;
        dropMarkerX = -1;
        revalidate();
        repaint();
    }

    private void paintArrangeGuides(final Graphics2D g) {
        final Localizer localizer = Localizer.getInstance();
        g.setFont(FSkin.getBoldFont(16).getBaseFont());
        final FontMetrics fm = g.getFontMetrics();
        final Rectangle[] areas = {allowTop ? topArea : null, allowBottom ? bottomArea : null};
        final String[] labels = {topLabel != null ? topLabel : localizer.getMessage("lblArenaTop"), localizer.getMessage("lblArenaBottom")};
        final List<?>[] sides = {topSide, bottomSide};
        for (int i = 0; i < 2; i++) {
            final Rectangle area = areas[i];
            if (area == null) {
                continue;
            }
            g.setColor(ArenaTheme.GOLD);
            g.drawString(labels[i], area.x + (area.width - fm.stringWidth(labels[i])) / 2, area.y - 10);
            if (sides[i].isEmpty() || (sides[i].size() == 1 && dragItem != null && sides[i].contains(dragItem.card))) {
                g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 90));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {6f, 6f}, 0f));
                g.drawRoundRect(area.x + GLOW, area.y + GLOW + LIFT, area.width - 2 * GLOW - 4, area.height - 2 * GLOW - LIFT, 14, 14);
            }
        }
        if (allowTop && allowBottom) {
            g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 120));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(gapCenterX, topArea.y + GLOW, gapCenterX, topArea.y + topArea.height - GLOW);
        }
        if (dropMarkerX >= 0) {
            g.setColor(ArenaTheme.GOLD_BRIGHT);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(dropMarkerX + 2, topArea != null ? topArea.y + GLOW : bottomArea.y + GLOW,
                    dropMarkerX + 2, (topArea != null ? topArea : bottomArea).y + (topArea != null ? topArea : bottomArea).height - GLOW);
        }
    }

    private void paintBackdrop(final Graphics2D g, final int w, final int h) {
        if (viewingBattlefield) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(4, 6, 11, 205));
        g.fillRect(0, 0, w, h);

        // title, a fading rule beneath it, and the instruction
        g.setFont(FSkin.getBoldFont(40).getBaseFont());
        FontMetrics fm = g.getFontMetrics();
        final int titleY = Math.round(h * 0.075f) + fm.getAscent();
        g.setColor(ArenaTheme.TEXT);
        g.drawString(title, (w - fm.stringWidth(title)) / 2, titleY);
        final int ruleY = titleY + 12, ruleW = Math.min(w / 2, 640);
        g.setPaint(new GradientPaint((w - ruleW) / 2f, 0, ArenaTheme.alpha(ArenaTheme.GOLD, 0), w / 2f, 0, ArenaTheme.GOLD, true));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine((w - ruleW) / 2, ruleY, (w + ruleW) / 2, ruleY);
        // a live selection takes its instruction from the prompt, which the input keeps current
        final String instruction = mode == Mode.SELECTION ? matchUI.getCPrompt().getView().getTarMessage().getText() : subtitle;
        if (instruction != null && !instruction.isEmpty()) {
            g.setFont(FSkin.getFont(17).getBaseFont());
            fm = g.getFontMetrics();
            String line = instruction.replaceAll("(?s)<head>.*?</head>", "").replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            while (line.length() > 1 && fm.stringWidth(line) > w - 120) {
                line = line.substring(0, line.length() - 2) + "…";
            }
            g.setColor(ArenaTheme.TEXT_DIM);
            g.drawString(line, (w - fm.stringWidth(line)) / 2, ruleY + 10 + fm.getAscent());
        }

        if (mode == Mode.ARRANGE) {
            paintArrangeGuides(g);
        }

        // the card the prompt is about, large on the right
        if (focusCard != null) {
            final int fh = Math.round(h * 0.4f), fw = focusWidth();
            if (focusImage == null) {
                focusImage = new CachedCardImage(focusCard, matchUI.getLocalPlayers(), fw, fh) {
                    @Override
                    public void onImageFetched() {
                        ArenaCardBrowser.this.repaint();
                    }
                };
            }
            paintCard(g, focusImage.getImage(), w - fw - 48, Math.round(h * 0.18f), fw, fh, ArenaTheme.PLAYABLE, 220);
        }
    }

    static void paintCard(final Graphics2D g, final BufferedImage img, final int x, final int y, final int w, final int h,
            final Color glow, final int glowStrength) {
        final int arc = Math.round(w * 0.07f);
        if (glow != null) {
            ArenaTheme.paintGlow(g, x, y, w, h, arc, glow, GLOW - 2, glowStrength);
        }
        final RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
        final Shape oldClip = g.getClip();
        g.clip(shape);
        g.setColor(ArenaTheme.NAVY);
        g.fill(shape);
        if (img != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            final float scale = Math.min(w / (float) img.getWidth(), h / (float) img.getHeight());
            final int iw = Math.round(img.getWidth() * scale), ih = Math.round(img.getHeight() * scale);
            g.drawImage(img, x + (w - iw) / 2, y + (h - ih) / 2, iw, ih, null);
        }
        g.setClip(oldClip);
        if (glow != null) {
            g.setColor(glow);
            g.setStroke(new BasicStroke(2f));
            g.draw(shape);
        }
    }

    /** A gold disc with a number, centred on {@code cx, cy}. */
    private static void paintPlace(final Graphics2D g, final int place, final int cx, final int cy) {
        final int d = 26;
        g.setColor(ArenaTheme.NIGHT);
        g.fillOval(cx - d / 2, cy - d / 2, d, d);
        g.setColor(ArenaTheme.GOLD_BRIGHT);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(cx - d / 2, cy - d / 2, d, d);
        g.setFont(FSkin.getBoldFont(15).getBaseFont());
        final FontMetrics fm = g.getFontMetrics();
        final String n = String.valueOf(place);
        g.drawString(n, cx - fm.stringWidth(n) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
    }

    /** A slim gold thumb on a faint track, with no arrow buttons. */
    static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(final int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(final int orientation) {
            return createZeroButton();
        }

        private static JButton createZeroButton() {
            final JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(final Graphics g, final JComponent c, final Rectangle r) {
            g.setColor(new Color(255, 255, 255, 28));
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                g.fillRoundRect(r.x + r.width / 2 - 2, r.y, 4, r.height, 4, 4);
            } else {
                g.fillRoundRect(r.x, r.y + r.height / 2 - 2, r.width, 4, 4, 4);
            }
        }

        @Override
        protected void paintThumb(final Graphics g, final JComponent c, final Rectangle r) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ArenaTheme.GOLD);
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                g2.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, r.width, r.width);
            } else {
                g2.fillRoundRect(r.x, r.y + 1, r.width, r.height - 2, r.height, r.height);
            }
        }
    }

    /** One card in the row: raised with a gold glow once chosen, blue while it can be picked. */
    private final class CardItem extends JComponent {
        private final CardView card;
        private CachedCardImage image;
        private boolean hover;

        CardItem(final CardView card) {
            this.card = card;
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hover = true;
                    matchUI.setCard(card);
                    repaint();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    if (mode == Mode.ARRANGE) {
                        startDrag(CardItem.this, e);
                    }
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    if (mode == Mode.ARRANGE) {
                        endDrag(CardItem.this, e);
                    } else if (contains(e.getPoint())) {
                        onCardClicked(CardItem.this, e);
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(final MouseEvent e) {
                    if (mode == Mode.ARRANGE) {
                        drag(CardItem.this, e);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final int cw = getWidth() - 2 * GLOW, ch = getHeight() - 2 * GLOW - LIFT;
                if (image == null) {
                    image = new CachedCardImage(card, matchUI.getLocalPlayers(), cw, ch) {
                        @Override
                        public void onImageFetched() {
                            repaint();
                        }
                    };
                }
                final boolean isChosen = isChosen(card);
                final Color glow;
                if (isChosen) {
                    glow = ArenaTheme.GOLD_BRIGHT;
                } else if (this == dragItem) {
                    glow = ArenaTheme.GOLD;
                } else if (mode == Mode.CHOOSE || mode == Mode.ARRANGE || (mode == Mode.SELECTION && matchUI.isSelectable(card))) {
                    glow = ArenaTheme.PLAYABLE;
                } else {
                    glow = hover ? ArenaTheme.HOVER : null;
                }
                final int y = isChosen || hover ? GLOW : GLOW + LIFT;
                if (mode == Mode.CHOOSE && !isChosen && chosen.size() >= max && max > 1) {
                    // the limit is reached; the rest wait dimmed
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                }
                paintCard(g, image.getImage(), GLOW, y, cw, ch, glow, hover || isChosen ? 255 : 170);
                // the order matters: number the picks, or the places along a one-sided row
                int place = 0;
                if (mode == Mode.CHOOSE && max > 1 && isChosen) {
                    place = chosen.indexOf(card) + 1;
                } else if (mode == Mode.ARRANGE && allowTop != allowBottom) {
                    place = (allowTop ? topSide : bottomSide).indexOf(card) + 1;
                }
                if (place > 0) {
                    paintPlace(g, place, GLOW + cw / 2, y + ch);
                }
            } finally {
                g.dispose();
            }
        }
    }
}
