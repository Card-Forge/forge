package forge.screens.match;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.apache.commons.text.WordUtils;

import forge.CachedCardImage;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FButton;
import forge.toolbox.MouseTriggerEvent;
import forge.util.Localizer;
import forge.view.FView;
import forge.view.arcane.CardPanel;

/**
 * Arena-style card browser: the scene dims behind a title, the cards fan out in one overlapping
 * row, the prompt's card sits on the right, and Submit waits in the shared slot. A row too long
 * to fit closes up until 60% of each card shows and then scrolls with the wheel, its edges fading away. "View Battlefield"
 * shrinks the browser to that one button until pressed again. It is drawn inside the match
 * window and takes every click while it covers the board.
 */
@SuppressWarnings("serial")
public final class ArenaCardBrowser extends JPanel {
    private static final float CARD_HEIGHT = 0.38f; // of the window
    /** The furthest a row of cards reaches across the window before it scrolls instead. */
    private static final float FAN_CAP = 0.91f;
    private static final float MIN_CARD_HEIGHT = 0.34f;
    /** The least of a card that stays in view in a row, as a share of its width. */
    private static final float MIN_SHOWN = 0.6f;
    /** The same beside the prompt's card, where there is less room. */
    private static final float MIN_SHOWN_SOURCE = 0.4f;
    /** Cards side by side on either side of the divider are all this high. */
    private static final float ARRANGE_HEIGHT = 0.38f;
    /** From the heading to what lies under it, when a row is split by the divider. */
    private static final float ARRANGE_HEADING_GAP = 0.025f;
    private static final float FAN_ANGLE = 4f;
    private static final float FAN_ARC = 0.02f;
    private static final float HOVER_LIFT = 0.045f;
    private static final float CHOSEN_SCALE = 1.05f;
    private static final float HOVER_SCALE = 1.05f;

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
        /** put cards on the top and/or bottom of a library by dragging them either side of a divider */
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
    private final ArenaDialogKit.FadeStrip strip = new ArenaDialogKit.FadeStrip(true, this::paintRaised);
    private final JScrollPane scroller;
    private final ArenaDialogKit.Chevron chevLeft = new ArenaDialogKit.Chevron(-1, 0, () -> scrollRow(-1));
    private final ArenaDialogKit.Chevron chevRight = new ArenaDialogKit.Chevron(1, 0, () -> scrollRow(1));
    private final FButton btnSubmit = new ArenaDialogKit.Pill();
    private final FButton btnSecondary = new ArenaDialogKit.Pill();
    private final FButton btnView = new ArenaDialogKit.Pill();
    private final ArenaDialogKit.TabBarDim tabBarDim = new ArenaDialogKit.TabBarDim();
    private final Timer animation = new Timer(15, e -> stepAnimation());
    private final ArenaDialogKit.Entrance entrance = new ArenaDialogKit.Entrance(this, () -> !this.viewingBattlefield);
    private final CardView focusCard;
    private CachedCardImage focusImage;
    private BufferedImage focusSource;
    private BufferedImage focusFace;
    private BufferedImage backdrop;
    private boolean viewingBattlefield;
    private int rowStep;
    private boolean rowScrolls;
    private int fanLeft;
    private int fanRight;
    private int fanFoot;
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
    private int slotW;
    private int slotH;
    private int slotCentreY;
    private int labelY;
    private int cardGap;
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
            SwingUtilities.invokeLater(selectionBrowser::restack);
            selectionBrowser.startAnimation();
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
     * them across a divider: on its left the leftmost goes on top, on its right the rightmost
     * goes at the very bottom. A click moves a card across. Returns {top, bottom}.
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
        final String subtitle = Localizer.getInstance().getMessage("lblArenaDragToReorder");
        final ArenaCardBrowser browser = showAndWait(() -> {
            final ArenaCardBrowser b = new ArenaCardBrowser(matchUI, Mode.ARRANGE, title, subtitle, cards, 0, 0, null);
            b.allowTop = true;
            b.topLabel = caption;
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

        setOpaque(true);
        // the board underneath must not react while the browser covers it
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });
        addMouseWheelListener(e -> { });

        // the top card of the fan is the rightmost, and comes first among the children
        for (final CardView card : cards) {
            final CardItem item = new CardItem(card);
            items.add(item);
            if (mode == Mode.ARRANGE) {
                add(item);
            } else {
                strip.add(item, 0);
            }
        }
        scroller = new JScrollPane(strip, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // a faded view cannot be blitted
        scroller.setBorder(new EmptyBorder(0, 0, 0, 0));
        // the wheel scrolls the row sideways
        scroller.setWheelScrollingEnabled(false);
        scroller.addMouseWheelListener(e -> scrollRowBy(e.getUnitsToScroll() * Math.max(8, rowStep / 3)));
        scroller.setVisible(mode != Mode.ARRANGE);
        chevLeft.setVisible(false);
        chevRight.setVisible(false);
        add(chevLeft);
        add(chevRight);
        add(scroller);

        btnSubmit.setPrimary(true);
        btnSubmit.addActionListener(e -> submit());
        add(btnSubmit);

        btnSecondary.addActionListener(e -> matchUI.getGameController().selectButtonCancel());
        btnSecondary.setVisible(mode == Mode.SELECTION);
        add(btnSecondary);

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
        if (!viewingBattlefield) {
            final Graphics2D c = entrance.sceneGraphics(g);
            paintScene(c, getWidth(), getHeight());
            c.dispose();
        }
    }

    @Override
    protected void paintChildren(final Graphics g0) {
        final Graphics2D g = entrance.contentGraphics(g0);
        try {
            paintContents(g);
        } finally {
            g.dispose();
        }
    }

    private void paintContents(final Graphics2D g) {
        super.paintChildren(g);
        if (mode == Mode.ARRANGE && !viewingBattlefield) {
            final Graphics2D g2 = g;
            if (railed()) {
                final int left = railLeft();
                ArenaDialogKit.paintRail(g2, left, left + railWidth(), slotCentreY + slotH / 2 + ArenaDialogKit.railDrop(getHeight()), getHeight());
            }
            paintRaised(g2);
            if (railed()) {
                for (final CardItem item : items) {
                    final Graphics2D c = (Graphics2D) g2.create(item.getX(), item.getY(), item.getWidth(), item.getHeight());
                    try {
                        item.drawBadge(c);
                    } finally {
                        c.dispose();
                    }
                }
            }
            paintDropMarker(g2);
        }
    }

    @Override
    public void doLayout() {
        layoutContents(getWidth(), getHeight());
    }

    private void open() {
        ArenaCardPreview.dismiss(); // a card still previewed from the hand would sit over the browser's dimming
        backdrop = ArenaDialogKit.captureBackdrop(null);
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        setBounds(0, 0, lpn.getWidth(), lpn.getHeight());
        lpn.add(this, FView.OVERLAY_LAYER);
        lpn.moveToFront(this);
        lpn.addComponentListener(followWindow);
        tabBarDim.attach();
        if (mode == Mode.ARRANGE) {
            stackArranged();
        } else {
            restack();
            startAnimation(); // cards chosen beforehand make their room too
        }
        fitToWindow();
        requestFocusInWindow();
        entrance.begin(340);
    }

    private void close() {
        animation.stop();
        tabBarDim.detach();
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
            final Rectangle r = ArenaDialogKit.viewButtonBounds(lpn.getWidth(), lpn.getHeight());
            r.grow(6, 6);
            setBounds(r);
        } else {
            setBounds(0, 0, lpn.getWidth(), lpn.getHeight());
            tabBarDim.attach();
        }
        revalidate();
        repaint();
        lpn.repaint();
    }

    private void submit() {
        if (!btnSubmit.isEnabled() || viewingBattlefield) {
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
        setOpaque(!viewingBattlefield);
        if (viewingBattlefield) {
            tabBarDim.detach();
        } else {
            backdrop = ArenaDialogKit.captureBackdrop(this); // the game may have moved on
        }
        scroller.setVisible(!viewingBattlefield && mode != Mode.ARRANGE);
        btnSubmit.setVisible(!viewingBattlefield);
        btnSecondary.setVisible(!viewingBattlefield && mode == Mode.SELECTION);
        if (viewingBattlefield) {
            chevLeft.setVisible(false);
            chevRight.setVisible(false);
        }
        if (mode == Mode.ARRANGE) {
            for (final CardItem item : items) {
                item.setVisible(!viewingBattlefield);
            }
        }
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
        restack();
        startAnimation();
        strip.repaint();
    }

    /** The rightmost card lies on top, with what has been chosen above the rest. */
    private void restack() {
        if (mode == Mode.ARRANGE) {
            return;
        }
        int z = 0;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (isChosen(items.get(i).card)) {
                strip.setComponentZOrder(items.get(i), z++);
            }
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!isChosen(items.get(i).card)) {
                strip.setComponentZOrder(items.get(i), z++);
            }
        }
    }

    private boolean isChosen(final CardView card) {
        return mode == Mode.SELECTION ? matchUI.isHighlighted(card) : chosen.contains(card);
    }

    private void layoutContents(final int w, final int h) {
        if (viewingBattlefield) {
            btnView.setBounds(6, 6, w - 12, h - 12);
            return;
        }
        btnView.setBounds(ArenaDialogKit.viewButtonBounds(w, h));
        ArenaDialogKit.placeButtons(w, h, btnSubmit, btnSecondary);
        if (mode == Mode.ARRANGE) {
            layoutArrange(w, h);
        } else {
            layoutRow(w, h);
        }
    }

    /** How much of each card the next one covers when there is room to spare. */
    private static float overlapFor(final int n) {
        // a long row spreads out while there is room, so that titles stay readable
        return n <= 3 ? 0.35f : n == 4 ? 0.42f : n <= 6 ? 0.5f : 0.38f;
    }

    /**
     * The fan: rightmost on top, a few degrees of tilt and a slight arc, centred on the window while
     * that keeps it clear of the prompt's card and else in the space left of that card. It closes up
     * until 60% of each card shows (40% beside the prompt's card) and shrinks the cards a little; when
     * even that is not enough it lies flat at that overlap and scrolls with the wheel or the arrows.
     * A chosen card lies whole on top, in its own place in the arc and at its own angle, and pushes
     * its neighbours aside to make room.
     */
    private void layoutRow(final int w, final int h) {
        final int n = items.size();
        final boolean source = focusCard != null;
        final int margin = Math.round(w * 0.03f);
        final float shown = source ? MIN_SHOWN_SOURCE : MIN_SHOWN;
        final int room = Math.min(ArenaDialogKit.rowRoom(w, h, source), source ? Integer.MAX_VALUE : Math.round(w * FAN_CAP));
        final float tilt = n <= 1 ? 0f : Math.min(FAN_ANGLE, 1.2f * (n - 1));

        final float size = fitRow(n, h, tilt, room, shown);
        final boolean scrolls = size < 0f;
        final int ch = Math.round(h * (scrolls ? MIN_CARD_HEIGHT : size)), cw = Math.round(ch / CardPanel.ASPECT_RATIO);
        final int step = scrolls ? Math.round(cw * shown) : rowStepFor(n, cw, ch, tilt, room);
        final float maxAngle = scrolls ? 0f : tilt;
        final float arcH = scrolls ? 0f : h * FAN_ARC * Math.min(1f, (n - 1) / 4f);
        final int rowW = cw + Math.max(0, n - 1) * step;
        final int areaY = Math.round(h * ArenaDialogKit.ROW_AREA_TOP), areaH = Math.round(h * ArenaDialogKit.ROW_AREA_BOTTOM) - areaY;
        // the outer corner of a tilted card reaches past its middle by this much, its light with it
        final int rightEdge = ArenaDialogKit.rowRight(w, h, source) - Math.round(ch * (float) Math.sin(Math.toRadians(maxAngle)) / 2f);
        final int rowCx = ArenaDialogKit.rowCentre(w, h, scrolls ? room : rowW, source);

        // what the chosen cards ask of the row, in as far as there is room for it
        final float[] push = pushes(n, cw, step, shown, scrolls ? Integer.MAX_VALUE : rightEdge - margin - rowW);
        final float before = push[n], total = push[n + 1];

        // a row that fits has the whole window, so nothing cuts its glow; one that scrolls fades at its ends
        final int viewX = scrolls ? rowCx - room / 2 : 0, viewW = scrolls ? room : w;
        final int left;
        if (scrolls) {
            left = Math.round(viewW * 0.06f) + Math.round(h * 0.03f);
        } else {
            // a chosen card stays where it was unless the row would run into what lies beside it
            int at = rowCx - rowW / 2 - Math.round(before);
            at -= Math.max(0, at + rowW + Math.round(total) - rightEdge);
            left = Math.max(margin, at);
        }
        final Dimension stripSize = new Dimension(scrolls ? rowW + Math.round(total) + 2 * left : w, areaH);
        rowStep = step;
        rowScrolls = scrolls;
        final int rowCy = Math.round(h * ArenaDialogKit.ROW_CENTRE_Y) - areaY;
        for (int i = 0; i < n; i++) {
            final float at = n <= 1 ? 0f : 2f * i / (n - 1) - 1f;
            items.get(i).place(left + cw / 2 + i * step + Math.round(push[i]), rowCy + Math.round(arcH * at * at), cw, ch, maxAngle * at);
        }
        // the shards lie along the feet of what shows: below the lowest corner of the fan
        fanLeft = scrolls ? viewX : left;
        fanRight = scrolls ? viewX + viewW : left + rowW + Math.round(total);
        fanFoot = areaY + rowCy + Math.round(arcH) + ch / 2 + Math.round(cw / 2f * (float) Math.sin(Math.toRadians(maxAngle)));
        scroller.setBounds(viewX, areaY, viewW, areaH);
        strip.setFade(0.06f);
        if (!stripSize.equals(strip.getPreferredSize())) {
            strip.setPreferredSize(stripSize);
            strip.setSize(stripSize);
            scroller.revalidate();
        }
        final int d = Math.round(h * 0.06f) + 2;
        chevLeft.setBounds(viewX + Math.round(h * 0.004f), areaY + rowCy - d / 2, d, d);
        chevRight.setBounds(viewX + viewW - Math.round(h * 0.004f) - d, areaY + rowCy - d / 2, d, d);
        clampRow();
    }

    /**
     * How far each card moves along the row to let the chosen ones lie whole, followed by what the
     * row grows on the left of them and what it grows in all. A chosen card lies over the one after
     * it and that one is covered from the other side too, so the cards after it go along until that
     * one shows as much as any other; the card before it only has to clear its slightly larger size.
     */
    private float[] pushes(final int n, final int cw, final int step, final float shown, final int slack) {
        final float aside = cw * (CHOSEN_SCALE - 1f) / 2f;
        final float through = Math.max(0f, cw + cw * shown - 2f * step);
        float wanted = 0f;
        for (int i = 0; i < n; i++) {
            wanted += items.get(i).riseT * (2f * aside + (i <= n - 3 ? through : 0f));
        }
        final float fit = wanted <= slack ? 1f : Math.max(0f, slack) / wanted;
        final float[] out = new float[n + 2];
        float run = 0f, before = 0f;
        for (int i = 0; i < n; i++) {
            final float t = items.get(i).riseT * fit;
            before += aside * t;
            out[i] = run + aside * t;
            run += t * (2f * aside + (i <= n - 3 ? through : 0f));
        }
        out[n] = before;
        out[n + 1] = run;
        return out;
    }

    /** The largest card height at which this share of each card shows in this room, or -1 if there is none. */
    private static float fitRow(final int n, final int h, final float tilt, final int room, final float shown) {
        for (float size = CARD_HEIGHT; size > MIN_CARD_HEIGHT - 0.001f; size -= 0.02f) {
            final int ch = Math.round(h * size), cw = Math.round(ch / CardPanel.ASPECT_RATIO);
            if (n <= 1 || rowStepFor(n, cw, ch, tilt, room) >= Math.round(cw * shown)) {
                return size;
            }
        }
        return -1f;
    }

    private static int rowStepFor(final int n, final int cw, final int ch, final float tilt, final int room) {
        if (n <= 1) {
            return 0;
        }
        final int pad = Math.round(ch * (float) Math.sin(Math.toRadians(tilt)) / 2f);
        return Math.min(Math.round(cw * (1f - overlapFor(n))), (room - 2 * pad - cw) / (n - 1));
    }

    private void scrollRow(final int direction) {
        scrollRowBy(direction * Math.round(scroller.getWidth() * 0.6f));
    }

    private void scrollRowBy(final int dx) {
        if (rowScrolls) {
            final JViewport viewport = scroller.getViewport();
            viewport.setViewPosition(new Point(viewport.getViewPosition().x + dx, 0));
            clampRow();
            strip.repaint();
        }
    }

    /** Keeps the view on the row and shows the chevrons on the sides that have more. */
    private void clampRow() {
        final JViewport viewport = scroller.getViewport();
        final int most = Math.max(0, strip.getPreferredSize().width - scroller.getWidth());
        final int x = rowScrolls ? Math.max(0, Math.min(most, viewport.getViewPosition().x)) : 0;
        if (viewport.getViewPosition().x != x) {
            viewport.setViewPosition(new Point(x, 0));
        }
        chevLeft.setVisible(rowScrolls && x > 0);
        chevRight.setVisible(rowScrolls && x < most);
    }

    private CardItem itemFor(final CardView card) {
        for (final CardItem item : items) {
            if (item.card == card) {
                return item;
            }
        }
        return null;
    }

    /** How wide a side of {@code n} cards is when they lie apart. */
    private int naturalWidth(final int n, final int cw) {
        return n <= 1 ? cw : n * cw + (n - 1) * cardGap;
    }

    /**
     * One open row of cards all of one size on one baseline. Two sides are one group, centred as a
     * whole, and the divider lies between them wherever that puts it, with the same gutter on either
     * side of it: each side's cards start from the divider, and close up only if the whole would not fit.
     */
    private void layoutArrange(final int w, final int h) {
        final boolean both = allowTop && allowBottom, source = focusCard != null;
        final int room = ArenaDialogKit.rowRoom(w, h, source);
        final int ch = Math.round(h * ARRANGE_HEIGHT), cw = Math.round(ch / CardPanel.ASPECT_RATIO);
        cardGap = Math.round(w * 0.02f); // wider than two glows, so that they never meet
        slotW = cw;
        slotH = ch;
        if (both) {
            final int gutter = Math.round(w * 0.03f), inner = room - 2 * gutter;
            final int wantTop = naturalWidth(topSide.size(), cw), wantBottom = naturalWidth(bottomSide.size(), cw);
            int topW = wantTop, bottomW = wantBottom;
            if (wantTop + wantBottom > inner) {
                topW = Math.max(cw, Math.min(inner - cw, Math.round(inner * (float) wantTop / (wantTop + wantBottom))));
                bottomW = inner - topW;
            }
            final int whole = topW + 2 * gutter + bottomW;
            final int groupLeft = ArenaDialogKit.rowCentre(w, h, whole, source) - whole / 2;
            labelY = ArenaDialogKit.headingBottom(title, headingLine(), fallbackTitle(), w, h) + Math.round(h * (ARRANGE_HEADING_GAP + 0.017f));
            final int top = labelY + Math.round(h * 0.014f);
            final int cy = top + ch / 2;
            slotCentreY = cy;
            gapCenterX = groupLeft + topW + gutter;
            topArea = new Rectangle(groupLeft, top, topW, ch);
            bottomArea = new Rectangle(gapCenterX + gutter, top, bottomW, ch);
            placeInArea(topSide, topArea, cw, ch, cy, 1);
            placeInArea(bottomSide, bottomArea, cw, ch, cy, -1);
        } else {
            final int cy = Math.round(h * 0.485f);
            final int top = cy - ch / 2;
            slotCentreY = cy;
            labelY = top - Math.round(h * 0.026f);
            final List<CardView> side = allowTop ? topSide : bottomSide;
            final int areaW = Math.min(room, naturalWidth(side.size(), cw));
            gapCenterX = ArenaDialogKit.rowCentre(w, h, areaW, source);
            final Rectangle area = new Rectangle(gapCenterX - areaW / 2, top, areaW, ch);
            topArea = allowTop ? area : null;
            bottomArea = allowBottom ? area : null;
            placeInArea(side, area, cw, ch, cy, 0);
        }
    }

    /** Lays a side's cards out in its area: against its left edge if {@code align} is below zero, its right edge if above, else in its middle. */
    private void placeInArea(final List<CardView> side, final Rectangle area, final int cw, final int ch, final int cy, final int align) {
        final int n = side.size();
        final int step = sideStep(n, area);
        final int rowW = cw + Math.max(0, n - 1) * step;
        int cx = (align < 0 ? area.x : align > 0 ? area.x + area.width - rowW : area.x + (area.width - rowW) / 2) + cw / 2;
        for (final CardView card : side) {
            final CardItem item = itemFor(card);
            if (item != null) {
                item.overlapped = n > 1 && step < cw;
                if (item != dragItem) {
                    item.place(cx, cy, cw, ch, 0f);
                }
            }
            cx += step;
        }
    }

    /** From one card to the next in a side of {@code n} cards: side by side while they fit, then overlapping. */
    private int sideStep(final int n, final Rectangle area) {
        return n <= 1 ? 0 : Math.min(slotW + cardGap, (area.width - slotW) / (n - 1));
    }

    /** The middle of a side's cards, beside the divider when there is one. */
    private int sideCx(final List<CardView> side, final Rectangle area) {
        final int n = side.size(), rowW = slotW + Math.max(0, n - 1) * sideStep(n, area);
        if (!allowTop || !allowBottom) {
            return area.x + area.width / 2;
        }
        return area == topArea ? area.x + area.width - rowW / 2 : area.x + rowW / 2;
    }

    /** Where the first card of an empty side would lie: beside the divider when there is one. */
    private int slotCx(final Rectangle area) {
        if (!allowTop || !allowBottom) {
            return area.x + area.width / 2;
        }
        return area == topArea ? area.x + area.width - slotW / 2 : area.x + slotW / 2;
    }

    /** The rightmost card of each side lies on top. */
    private void stackArranged() {
        for (final List<CardView> side : List.of(topSide, bottomSide)) {
            for (final CardView card : side) {
                final CardItem item = itemFor(card);
                if (item != null) {
                    setComponentZOrder(item, 0);
                }
            }
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
        startAnimation();
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
        CardItem before = null, after = null;
        int seen = 0;
        for (final CardView card : side) {
            final CardItem other = itemFor(card);
            if (other == null || other == item) {
                continue;
            }
            if (seen == index - 1) {
                before = other;
            } else if (seen == index) {
                after = other;
            }
            seen++;
        }
        if (before != null && after != null) {
            dropMarkerX = (before.getX() + before.getWidth() / 2 + slotW / 2 + after.getX() + after.getWidth() / 2 - slotW / 2) / 2;
        } else if (before != null) {
            dropMarkerX = before.getX() + before.getWidth() / 2 + slotW / 2 + Math.round(getWidth() * 0.005f);
        } else if (after != null) {
            dropMarkerX = after.getX() + after.getWidth() / 2 - slotW / 2 - Math.round(getWidth() * 0.005f);
        } else {
            dropMarkerX = slotCx(area);
        }
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
            // a click sends the card across to the other well, next to it
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
        stackArranged();
        startAnimation();
        revalidate();
        repaint();
    }

    private void paintDropMarker(final Graphics2D g) {
        if (dropMarkerX < 0) {
            return;
        }
        ArenaDialogKit.quality(g);
        final int half = slotH / 2 + 6;
        final int bar = Math.max(3, Math.round(getHeight() * 0.004f));
        g.setColor(ArenaTheme.alpha(ArenaDialogKit.PICKED, 70));
        g.fillRoundRect(dropMarkerX - bar * 2, slotCentreY - half, bar * 4, half * 2, bar * 4, bar * 4);
        g.setColor(ArenaDialogKit.PICKED);
        g.fillRoundRect(dropMarkerX - bar / 2, slotCentreY - half, bar, half * 2, bar, bar);
    }

    /** Whether this is one row in order, its numbers on a rail with its two ends named under it. */
    private boolean railed() {
        return topLabel != null && topArea != null;
    }

    private int railWidth() {
        final int n = topSide.size();
        return slotW + Math.max(0, n - 1) * sideStep(n, topArea);
    }

    private int railLeft() {
        return topArea.x + (topArea.width - railWidth()) / 2;
    }

    /** What each side means and the divider between the two, and a hollow where a side has no cards. */
    private void paintArrangeGuides(final Graphics2D g, final int w, final int h) {
        final Localizer localizer = Localizer.getInstance();
        ArenaDialogKit.quality(g);
        final boolean both = allowTop && allowBottom;
        final int rowTop = slotCentreY - slotH / 2, rowBottom = slotCentreY + slotH / 2;
        if (railed()) {
            // a row in order says what each of its ends means, under the number at that end
            final int left = railLeft(), right = left + railWidth(), n = topSide.size();
            final boolean overlapped = n > 1 && sideStep(n, topArea) < slotW;
            final int inset = overlapped ? Math.round(ArenaDialogKit.badgeSize(h) * 0.8f) : slotW / 2;
            ArenaDialogKit.paintRailEnds(g, topLabel, ArenaDialogKit.lastCaption(topLabel), left + inset, right - slotW + inset, rowBottom + ArenaDialogKit.orderRowDrop(h), h);
            return;
        }
        if (allowTop && topArea != null) {
            ArenaDialogKit.paintRowLabel(g, localizer.getMessage("lblArenaTop"), sideCx(topSide, topArea), labelY, h);
            if (isEmptyAsShown(topSide)) {
                ArenaDialogKit.paintSlot(g, slotCx(topArea), slotCentreY, slotW, slotH);
            }
        }
        if (allowBottom && bottomArea != null) {
            ArenaDialogKit.paintRowLabel(g, localizer.getMessage("lblArenaBottom"), sideCx(bottomSide, bottomArea), labelY, h);
            if (isEmptyAsShown(bottomSide)) {
                ArenaDialogKit.paintSlot(g, slotCx(bottomArea), slotCentreY, slotW, slotH);
            }
        }
        if (both) {
            // a line of 2px between the two groups, from their labels to below their numbers, fading out only at its very ends
            final int y0 = labelY - Math.round(h * 0.022f), y1 = rowBottom + ArenaDialogKit.badgeSize(h) + Math.round(h * 0.016f);
            g.setPaint(new LinearGradientPaint(0, y0, 0, y1, new float[] {0f, 0.1f, 0.9f, 1f},
                    new Color[] {new Color(255, 255, 255, 0), new Color(255, 255, 255, 215), new Color(255, 255, 255, 215), new Color(255, 255, 255, 0)}));
            g.fillRect(gapCenterX - 1, y0, 2, y1 - y0);
        }
    }

    private boolean isEmptyAsShown(final List<CardView> side) {
        return side.isEmpty() || (side.size() == 1 && dragMoved && dragItem != null && side.contains(dragItem.card));
    }

    /** The line under the title; a live selection takes it from the prompt, which the input keeps current. */
    private String headingLine() {
        final String instruction = mode == Mode.SELECTION ? matchUI.getCPrompt().getView().getTarMessage().getText() : subtitle;
        final String line = instruction == null ? null : ArenaDialogKit.plainText(instruction);
        return line == null || line.isEmpty() || ArenaDialogKit.sameWords(line, title) ? null : line;
    }

    /** What stands in for a title too long to be one. */
    private String fallbackTitle() {
        return Localizer.getInstance().getMessage(mode == Mode.ARRANGE ? "lblArenaChooseOrder" : "lblArenaChooseCards");
    }

    private void paintScene(final Graphics2D g, final int w, final int h) {
        ArenaDialogKit.paintBackdrop(g, backdrop, w, h);
        ArenaDialogKit.paintHeading(g, title, headingLine(), fallbackTitle(), w, h);
        if (mode == Mode.ARRANGE) {
            paintArrangeGuides(g, w, h);
        } else if (anySelectable() && fanRight > fanLeft) {
            ArenaDialogKit.paintRowShards(g, fanLeft, fanRight, fanFoot, h, this::lightAt);
        }
        // the card the prompt is about, large on the right
        if (focusCard != null) {
            final Rectangle r = ArenaDialogKit.sourceBounds(w, h);
            if (focusImage == null || focusImage.getImage() == null) {
                focusImage = new CachedCardImage(focusCard, matchUI.getLocalPlayers(), r.width, r.height) {
                    @Override
                    public void onImageFetched() {
                        ArenaCardBrowser.this.repaint();
                    }
                };
            }
            final BufferedImage src = focusImage.getImage();
            if (focusFace == null || src != focusSource || focusFace.getWidth() != r.width + 2 * ArenaDialogKit.FACE_PAD) {
                focusFace = ArenaDialogKit.cardFace(src, r.width, r.height);
                focusSource = src;
            }
            ArenaDialogKit.paintSource(g, focusFace, r, true);
        }
    }

    private boolean anySelectable() {
        if (mode == Mode.CHOOSE) {
            return true;
        }
        if (mode == Mode.SELECTION) {
            for (final CardView card : cards) {
                if (matchUI.isSelectable(card)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The colour of the rim of the card that stands over this x of the window. */
    private Color lightAt(final int x) {
        CardItem nearest = null;
        int best = Integer.MAX_VALUE;
        final int view = scroller.getX() - scroller.getViewport().getViewPosition().x;
        for (final CardItem item : items) {
            final int away = Math.abs(view + item.getX() + item.getWidth() / 2 - x);
            if (away < best) {
                best = away;
                nearest = item;
            }
        }
        return nearest != null && isChosen(nearest.card) ? ArenaDialogKit.PICKED : ArenaDialogKit.CYAN;
    }

    private static int itemSpread(final int h) {
        return Math.max(14, Math.round(h * 0.029f));
    }

    private void startAnimation() {
        if (!animation.isRunning()) {
            animation.start();
        }
    }

    private void stepAnimation() {
        boolean moving = false;
        for (final CardItem item : items) {
            moving |= item.step();
        }
        if (mode == Mode.ARRANGE) {
            repaint();
        } else {
            if (moving && !viewingBattlefield && getWidth() > 0) {
                layoutRow(getWidth(), getHeight()); // the neighbours of a chosen card move aside
            }
            strip.repaint();
        }
        if (!moving) {
            animation.stop();
        }
    }

    /** Cards raised by the mouse lie above the whole row, the one under the mouse above them all. */
    private void paintRaised(final Graphics2D g) {
        CardItem top = null;
        for (final CardItem item : items) {
            if (item.isRaised()) {
                if (item.hover || item == dragItem) {
                    top = item;
                } else {
                    paintItemOnTop(g, item);
                }
            }
        }
        if (top != null) {
            paintItemOnTop(g, top);
        }
    }

    private static void paintItemOnTop(final Graphics2D g, final CardItem item) {
        final Graphics2D c = (Graphics2D) g.create(item.getX(), item.getY(), item.getWidth(), item.getHeight());
        try {
            item.draw(c);
        } finally {
            c.dispose();
        }
    }

    /** One card in the row: a cyan rim while it can be picked, a gold one once chosen. */
    private final class CardItem extends JComponent {
        private final CardView card;
        private final ArenaDialogKit.Sprite sprite = new ArenaDialogKit.Sprite();
        private CachedCardImage image;
        private BufferedImage face;
        private BufferedImage faceSource;
        private int faceQ;
        private boolean hover;
        private float hoverT;
        private float riseT;
        private int cw;
        private int ch;
        private int restX;
        private int restY;
        private float angle;
        private boolean overlapped;

        CardItem(final CardView card) {
            this.card = card;
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hover = true;
                    matchUI.setCard(card);
                    startAnimation();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hover = false;
                    startAnimation();
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

        /** Rests the card with its centre on {@code cx, cy}; the bounds leave room to lift, tilt and glow. */
        void place(final int cx, final int cy, final int cw0, final int ch0, final float angle0) {
            if (cw0 != cw || ch0 != ch) {
                image = null;
                face = null;
                cw = cw0;
                ch = ch0;
            }
            angle = angle0;
            // enough for the largest it can get: tilted, scaled, lifted and glowing
            final int reach = Math.round(itemSpread(ArenaCardBrowser.this.getHeight()) * 1.5f) + 6;
            final int lift = Math.round(ArenaCardBrowser.this.getHeight() * HOVER_LIFT);
            final int mx = Math.round(ch * 0.04f) + Math.round(cw * 0.05f) + reach;
            final int my = Math.round(cw * 0.04f) + Math.round(ch * 0.05f) + reach;
            restX = mx + cw / 2;
            restY = my + lift + ch / 2;
            setBounds(cx - restX, cy - restY, cw + 2 * mx, ch + 2 * my + lift);
        }

        boolean isRaised() {
            return hoverT > 0f || hover || this == dragItem;
        }

        /** Moves towards where the card should be; true while it is still on its way. */
        boolean step() {
            final float hoverGoal = hover || this == dragItem ? 1f : 0f, riseGoal = isChosen(card) ? 1f : 0f;
            final float h0 = hoverT, r0 = riseT;
            hoverT = approach(hoverT, hoverGoal, 0.2f);
            riseT = approach(riseT, riseGoal, 0.2f);
            return h0 != hoverT || r0 != riseT;
        }

        private float approach(final float from, final float to, final float by) {
            return from < to ? Math.min(to, from + by) : Math.max(to, from - by);
        }

        /** How far the mouse has raised this card; a chosen card stays in its place in the arc even under the mouse. */
        private float raised() {
            return hoverT * (1f - riseT);
        }

        private float lift() {
            if (mode == Mode.ARRANGE) {
                return 0f; // a row split by a divider stays on its one baseline
            }
            return ArenaCardBrowser.this.getHeight() * HOVER_LIFT * raised();
        }

        private float scale() {
            return mode == Mode.ARRANGE ? 1f : Math.max(1f + (HOVER_SCALE - 1f) * raised(), 1f + (CHOSEN_SCALE - 1f) * riseT);
        }

        private float tilt() {
            return angle * (1f - raised());
        }

        @Override
        public boolean contains(final int x, final int y) {
            if (cw == 0) {
                return false;
            }
            if (ArenaDialogKit.inRotated(x, y, restX, restY, cw / 2f, ch / 2f, angle)) {
                return true;
            }
            // what is raised can be grabbed where it now is, too
            final float lift = lift(), scale = scale();
            return lift > 0f && ArenaDialogKit.inRotated(x, y, restX, restY - lift, scale * cw / 2f, scale * ch / 2f, tilt());
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            if (!isRaised()) {
                final Graphics2D g = (Graphics2D) g0.create();
                try {
                    draw(g);
                } finally {
                    g.dispose();
                }
            }
        }

        void draw(final Graphics2D g) {
            ArenaDialogKit.quality(g);
            // a row that lies flat and scrolls can be long; every other card may be rotated, so it comes from twice the pixels
            final int q = rowScrolls ? 1 : 2;
            if (q != faceQ) {
                faceQ = q;
                image = null;
                face = null;
            }
            if (image == null) {
                image = new CachedCardImage(card, matchUI.getLocalPlayers(), cw * q, ch * q) {
                    @Override
                    public void onImageFetched() {
                        ArenaCardBrowser.this.repaint();
                    }
                };
            }
            final BufferedImage src = image.getImage();
            if (face == null || src != faceSource) {
                face = ArenaDialogKit.cardFace(src, cw, ch, q);
                faceSource = src;
            }
            final boolean picked = isChosen(card);
            final int bh = ArenaCardBrowser.this.getHeight();
            final ArenaDialogKit.Glow glow;
            if (picked || this == dragItem) {
                glow = ArenaDialogKit.chosenGlow(bh);
            } else if (mode == Mode.CHOOSE || mode == Mode.ARRANGE || (mode == Mode.SELECTION && matchUI.isSelectable(card))) {
                glow = ArenaDialogKit.selectableGlow(bh, hover ? 1.3f : 1f);
            } else {
                glow = hover ? ArenaDialogKit.plainGlow(bh) : null;
            }
            // the limit is reached; the rest wait dimmed
            final boolean dimmed = mode == Mode.CHOOSE && !picked && chosen.size() >= max && max > 1;
            // the cards are dealt in, one after the other
            final float dealt = entrance.isOver() ? 1f : entrance.inTurn(items.indexOf(this), items.size());
            if (dealt <= 0f) {
                return;
            }
            if (dealt < 1f) {
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(dealt));
            }
            final int dealRise = Math.round((1f - dealt) * bh * 0.05f);
            sprite.draw(g, face, q, restX, Math.round(restY - lift()) + dealRise, tilt(), scale(), dimmed ? null : glow, dimmed);
            if (!railed()) {
                drawBadge(g);
            }
        }

        /** The order matters: the number of a pick straddling the card's bottom edge, or of a place in the row below the card or on its rail. */
        void drawBadge(final Graphics2D g) {
            int place = 0;
            if (mode == Mode.CHOOSE && max > 1 && isChosen(card)) {
                place = chosen.indexOf(card) + 1;
            } else if (mode == Mode.ARRANGE) {
                place = (topSide.contains(card) ? topSide : bottomSide).indexOf(card) + 1;
            }
            if (place <= 0) {
                return;
            }
            final int bh = ArenaCardBrowser.this.getHeight();
            final int d = ArenaDialogKit.badgeSize(bh);
            final float scale = scale();
            final double rad = Math.toRadians(tilt());
            final boolean inRow = mode == Mode.CHOOSE || overlapped;
            final double bx = inRow ? -scale * cw / 2d + d * 0.8 : 0d;
            final int below = railed() ? ArenaDialogKit.railDrop(bh) : mode == Mode.ARRANGE ? d / 2 + Math.round(bh * 0.006f) : 0;
            final double by = scale * ch / 2d + below;
            final int px = restX + (int) Math.round(bx * Math.cos(rad) - by * Math.sin(rad));
            final int py = Math.round(restY - lift()) + (int) Math.round(bx * Math.sin(rad) + by * Math.cos(rad));
            ArenaDialogKit.paintBadge(g, place, px, py, d);
        }
    }
}
