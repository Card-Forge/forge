package forge.screens.match;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import forge.CachedCardImage;
import forge.ImageCache;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FButton;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.view.FView;
import forge.view.arcane.CardPanel;

/**
 * The Arena overlay for the decisions that are not a row of cards: picking from a few options
 * (modes, abilities, players, colours, numbers), putting options in order, and plain questions
 * with buttons. Long options become card-like tiles in the same row language as the card browser,
 * short ones pills, consecutive numbers a stepper. Like the browser it dims the scene, shows the
 * prompt's card, can step aside with "View Battlefield", and takes every click while it is up.
 */
@SuppressWarnings("serial")
public final class ArenaOptionsOverlay extends JPanel {
    /** Longer lists than this stay in the classic list, which can be searched and scrolled quickly. */
    public static final int MAX_OPTIONS = 40;
    /** Number lists up to this long still fit the number stepper. */
    public static final int MAX_NUMBERS = 101;

    private static final float TILE_H = 0.39f; // as tall as the prompt's card beside the row, and of a card's shape
    /** The art takes this share of a tile's height. */
    private static final float TILE_ART = 0.3f;
    /** The words on a tile, as a share of the window's height; every tile in a row has one size. */
    private static final float TILE_TEXT = 0.022f;
    /** The one line pitch of all tiles, however large their words had to be. */
    private static final float TILE_PITCH = 0.0262f;
    /** The most a line of a tile may be spaced, as a share of its size, before the pitch closes up too. */
    private static final float TILE_LEADING_MOST = 1.36f;
    /** The same on flat tiles in a grid, where the tile is cut to the words. */
    private static final float GRID_TEXT = 0.019f;
    private static final float GRID_LINE = 1.24f;
    private static final float HOVER_LIFT = 0.04f;
    private static final float NUMERAL_SIZE = 0.115f;
    /** The one height of the prompt's card when it stands alone above a question or a number, and where its middle lies. */
    private static final float ALONE_CARD = 0.38f;
    private static final float ALONE_CARD_CY = 0.42f;
    private static final float STEP_BUTTON = 0.06f; // 65px at 1080p
    /** A question's card is this far above the buttons, and its top this far down the window. */
    private static final float QUESTION_CARD_GAP = 0.04f;
    private static final float QUESTION_CARD_TOP = 0.15f;
    private static final Color PARCHMENT = new Color(0xF4ECD8);
    private static final Color NO_CARD_FRAME = new Color(0x9C8656);
    private static final Pattern BRACED = Pattern.compile("\\{([^}]*)\\}");
    private static final int LONGEST_PILL = 24;
    private static final int MOST_TILES = 6;

    private enum Kind {
        /** pick between min and max options; a single pick closes at once */
        CHOICE,
        /** drag every option into order, first on the left */
        ORDER,
        /** a question answered with one of a few buttons */
        BUTTONS
    }

    /** How the options are laid out. */
    private enum Style {
        /** the answers themselves are the buttons, in the shared slot */
        ANSWERS,
        /** short words as pills, in a tidy centred row */
        PILLS,
        /** one row of card-like tiles */
        TILES,
        /** too many long options for one row: two columns of flat tiles that scroll if they must */
        GRID,
        /** consecutive numbers: one big numeral between two arrows */
        STEPPER
    }

    private final CMatchUI matchUI;
    private final Kind kind;
    private Style style = Style.ANSWERS;
    private final String title;
    private final String message;
    private final String orderCaption;
    private final int min;
    private final int max;
    private final List<Option> options = new ArrayList<>();
    private final List<Option> picked = new ArrayList<>();
    private final ArenaDialogKit.FadeStrip list = new ArenaDialogKit.FadeStrip(false, this::paintRaised);
    private final JScrollPane scroller;
    private final ArenaDialogKit.Chevron chevUp = new ArenaDialogKit.Chevron(0, -1, () -> scrollList(-1));
    private final ArenaDialogKit.Chevron chevDown = new ArenaDialogKit.Chevron(0, 1, () -> scrollList(1));
    private final FButton btnSubmit = new ArenaDialogKit.Pill();
    private final FButton btnCancel = new ArenaDialogKit.Pill();
    private final FButton btnView = new ArenaDialogKit.Pill();
    private final ArenaDialogKit.CheckBox chkRemember;
    private final ArenaDialogKit.TabBarDim tabBarDim = new ArenaDialogKit.TabBarDim();
    private final Timer animation = new Timer(15, e -> stepAnimation());
    private final CardView focusCard;
    private CachedCardImage focusImage;
    private BufferedImage focusSource;
    private BufferedImage focusFace;
    private BufferedImage backdrop;
    private boolean viewingBattlefield;
    private boolean cancelled;
    private int answer = -1;
    private Option dragging;
    private int dragGrabX;
    private boolean dragMoved;
    private int listScrollStep;
    private boolean listScrolls;
    private int orderCaptionX = -1;
    private int orderCaptionRight;
    private int orderCaptionY;
    private int orderRailY;
    private int orderFirstBadge;
    private int orderLastBadge;
    private int rowFoot;
    private int sizedTextW;
    private int sizedTextH;
    private float sizedText;
    private Runnable onClose;
    // STEPPER: the number options lowest first, the one showing, and what the numbers came with
    private final List<Option> numbers = new ArrayList<>();
    private int numberIndex;
    private Option otherOption;
    private StepButton btnLower;
    private StepButton btnHigher;
    private FButton btnOther;
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

    /** Lets the player drag {@code items} into order, the first on the left; returns them in that order and whether to remember it. */
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
        this.message = ArenaDialogKit.plainText(message);
        this.orderCaption = orderCaption;
        this.min = min;
        this.max = max;
        // a plain question shows only the card it was asked about
        this.focusCard = card != null || kind == Kind.BUTTONS ? card : matchUI.getCPrompt().getView().getCardView();

        setOpaque(true);
        // the board underneath must not react while the overlay covers it
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });
        addMouseWheelListener(e -> {
            if (style == Style.STEPPER) {
                stepNumber(e.getWheelRotation() < 0 ? 1 : -1);
            }
        });

        scroller = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // a faded view cannot be blitted
        scroller.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroller.setWheelScrollingEnabled(false);
        scroller.addMouseWheelListener(e -> {
            if (style == Style.STEPPER) {
                stepNumber(e.getWheelRotation() < 0 ? 1 : -1);
            } else {
                scrollListBy(e.getUnitsToScroll() * Math.max(8, listScrollStep / 3));
            }
        });
        chevUp.setVisible(false);
        chevDown.setVisible(false);
        add(chevUp);
        add(chevDown);
        add(scroller);

        final Localizer localizer = Localizer.getInstance();
        btnSubmit.setPrimary(true);
        btnSubmit.addActionListener(e -> submit());
        add(btnSubmit);

        btnCancel.setText(localizer.getMessage(kind == Kind.CHOICE && max > 1 ? "lblCancel" : "lblArenaDecline"));
        btnCancel.addActionListener(e -> cancel());
        btnCancel.setVisible(kind == Kind.CHOICE && min == 0);
        add(btnCancel);

        btnView.addActionListener(e -> toggleBattlefield());
        add(btnView);

        if (offerRemember) {
            chkRemember = new ArenaDialogKit.CheckBox(localizer.getMessage("lblAlwaysUseThisOrder"));
            chkRemember.setToolTipText(localizer.getMessage("lblAlwaysUseThisOrderTooltip"));
            chkRemember.setSelected(true);
            add(chkRemember, 0); // above the row's area, which reaches down behind it
        } else {
            chkRemember = null;
        }

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (viewingBattlefield) {
                    return;
                }
                if (kind == Kind.BUTTONS) {
                    for (final Option option : options) {
                        if (option.primary) {
                            answer(option);
                            return;
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
            options.add(new Option(i, value, display != null ? display.apply(value) : describe(value)));
        }
        style = chooseStyle(values);
        if (style == Style.STEPPER) {
            buildStepper();
        } else {
            for (final Option option : options) {
                if (style == Style.ANSWERS) {
                    add(option);
                } else {
                    list.add(option, 0); // later ones lie on top, as in the card row
                }
            }
        }
        scroller.setVisible(style != Style.ANSWERS && style != Style.STEPPER);
        list.setFade(0.09f);
        updateButtons();
    }

    private Style chooseStyle(final List<?> values) {
        if (kind == Kind.BUTTONS) {
            return Style.ANSWERS;
        }
        if (kind == Kind.ORDER) {
            return Style.TILES;
        }
        if (isConsecutiveNumbers(values) && max == 1) {
            return Style.STEPPER;
        }
        boolean allShort = true;
        for (final Option option : options) {
            if (option.card() != null || option.label.contains("\n") || option.label.length() > LONGEST_PILL) {
                allShort = false;
            }
        }
        if (allShort) {
            return Style.PILLS;
        }
        return options.size() <= MOST_TILES ? Style.TILES : Style.GRID;
    }

    /** At least two whole numbers one apart, either way round, maybe with "Other..." after them. */
    private static boolean isConsecutiveNumbers(final List<?> values) {
        final int size = values.size();
        final int count = size > 2 && values.get(size - 1) instanceof String ? size - 1 : size;
        if (count < 2) {
            return false;
        }
        Integer direction = null;
        for (int i = 0; i < count; i++) {
            if (!(values.get(i) instanceof Integer)) {
                return false;
            }
            if (i > 0) {
                final int diff = (Integer) values.get(i) - (Integer) values.get(i - 1);
                if (Math.abs(diff) != 1 || (direction != null && direction != diff)) {
                    return false;
                }
                direction = diff;
            }
        }
        return true;
    }

    private void buildStepper() {
        for (final Option option : options) {
            if (option.value instanceof Integer) {
                numbers.add(option);
            } else {
                otherOption = option;
            }
        }
        // the first one offered is where it starts, even in a list that counts down
        final Option first = numbers.get(0);
        numbers.sort((a, b) -> Integer.compare((Integer) a.value, (Integer) b.value));
        numberIndex = numbers.indexOf(first);
        btnLower = new StepButton(-1);
        btnHigher = new StepButton(1);
        add(btnLower);
        add(btnHigher);
        if (otherOption != null) {
            btnOther = new ArenaDialogKit.Pill(otherOption.label);
            btnOther.addActionListener(e -> {
                picked.clear();
                picked.add(otherOption);
                close();
            });
            add(btnOther);
        }
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "higher");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "higher");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "lower");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "lower");
        getActionMap().put("higher", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                stepNumber(1);
            }
        });
        getActionMap().put("lower", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                stepNumber(-1);
            }
        });
    }

    private void stepNumber(final int by) {
        if (viewingBattlefield) {
            return;
        }
        numberIndex = Math.max(0, Math.min(numbers.size() - 1, numberIndex + by));
        repaint();
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

    private void open() {
        ArenaCardPreview.dismiss(); // a card still previewed from the hand would sit over the overlay's dimming
        if (style == Style.STEPPER && !picked.isEmpty() && numbers.contains(picked.get(0))) {
            numberIndex = numbers.indexOf(picked.get(0)); // the selection it was given
        }
        updateButtons();
        backdrop = ArenaDialogKit.captureBackdrop(null);
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        setBounds(0, 0, lpn.getWidth(), lpn.getHeight());
        lpn.add(this, FView.OVERLAY_LAYER);
        lpn.moveToFront(this);
        lpn.addComponentListener(followWindow);
        tabBarDim.attach();
        fitToWindow();
        requestFocusInWindow();
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

    private boolean submitShown() {
        return kind == Kind.ORDER || (kind == Kind.CHOICE && (style != Style.PILLS || max > 1));
    }

    private void toggleBattlefield() {
        viewingBattlefield = !viewingBattlefield;
        setOpaque(!viewingBattlefield);
        if (viewingBattlefield) {
            tabBarDim.detach();
        } else {
            backdrop = ArenaDialogKit.captureBackdrop(this); // the game may have moved on
        }
        final boolean show = !viewingBattlefield;
        scroller.setVisible(show && style != Style.ANSWERS && style != Style.STEPPER);
        btnSubmit.setVisible(show && submitShown());
        btnCancel.setVisible(show && kind == Kind.CHOICE && min == 0);
        if (chkRemember != null) {
            chkRemember.setVisible(show);
        }
        if (style == Style.ANSWERS) {
            for (final Option option : options) {
                option.setVisible(show);
            }
        }
        if (style == Style.STEPPER) {
            btnLower.setVisible(show);
            btnHigher.setVisible(show);
            if (btnOther != null) {
                btnOther.setVisible(show);
            }
        }
        if (!show) {
            chevUp.setVisible(false);
            chevDown.setVisible(false);
        }
        updateButtons();
        fitToWindow();
    }

    private void submit() {
        if (btnSubmit.isVisible() && btnSubmit.isEnabled()) {
            if (style == Style.STEPPER) {
                picked.clear();
                picked.add(numbers.get(numberIndex));
            }
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
        btnSubmit.setVisible(!viewingBattlefield && submitShown());
        if (kind == Kind.ORDER || style == Style.STEPPER) {
            btnSubmit.setText(localizer.getMessage("lblArenaSubmitPlain"));
            btnSubmit.setEnabled(true);
        } else {
            btnSubmit.setText(localizer.getMessage("lblArenaSubmit", picked.size()));
            // declining a single pick has its own button; a list of picks may be left empty
            final int least = max == 1 ? 1 : min;
            btnSubmit.setEnabled(picked.size() >= least && picked.size() <= max);
        }
        btnView.setText(localizer.getMessage(viewingBattlefield ? "lblArenaReturn" : "lblArenaViewBattlefield"));
    }

    private void onOptionClicked(final Option option, final int clicks) {
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
            if (style == Style.PILLS || clicks > 1) {
                close(); // a pill is answer enough; a tile takes a double click or Submit
                return;
            }
        } else if (picked.contains(option)) {
            picked.remove(option);
        } else if (picked.size() < max) {
            picked.add(option);
        }
        updateButtons();
        startAnimation();
        list.repaint();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        if (!viewingBattlefield) {
            paintScene((Graphics2D) g, getWidth(), getHeight());
        }
    }

    @Override
    public void doLayout() {
        final int w = getWidth(), h = getHeight();
        if (viewingBattlefield) {
            btnView.setBounds(6, 6, w - 12, h - 12);
            return;
        }
        btnView.setBounds(ArenaDialogKit.viewButtonBounds(w, h));
        if (btnOther != null) {
            ArenaDialogKit.placeButtons(w, h, btnSubmit, btnOther, btnCancel);
        } else {
            ArenaDialogKit.placeButtons(w, h, btnSubmit, btnCancel);
        }
        switch (style) {
            case ANSWERS:
                ArenaDialogKit.placeRow(w, h, answerWidth(w, h), options);
                break;
            case PILLS:
                layoutPills(w, h);
                break;
            case TILES:
                layoutTiles(w, h);
                break;
            case GRID:
                layoutGrid(w, h);
                break;
            default:
                layoutStepper(w, h);
                break;
        }
        if (chkRemember != null) {
            // under the row, between the words for its two ends
            final Dimension d = chkRemember.sizeFor(Math.round(h * 0.022f));
            chkRemember.setBounds((orderCaptionX + orderCaptionRight - d.width) / 2, orderCaptionY - d.height / 2, d.width, d.height);
        }
    }

    private int rightLimit(final int w, final int h) {
        return focusCard == null || kind == Kind.BUTTONS ? w : ArenaDialogKit.sourceBounds(w, h).x - Math.round(w * 0.012f);
    }

    /** Sets the list's area and its size, and scrolls it if it is taller than that. */
    private void fitList(final int w, final int h, final int listH) {
        final int right = rightLimit(w, h);
        final int areaY = Math.round(h * ArenaDialogKit.ROW_AREA_TOP), areaH = Math.round(h * ArenaDialogKit.ROW_AREA_BOTTOM) - areaY;
        listScrolls = listH > areaH;
        scroller.setBounds(0, areaY, right, areaH);
        final Dimension size = new Dimension(right, Math.max(areaH, listH));
        if (!size.equals(list.getPreferredSize())) {
            list.setPreferredSize(size);
            list.setSize(size);
            scroller.revalidate();
        }
        final int d = Math.round(h * 0.044f);
        chevUp.setBounds((right - d) / 2, areaY + 4, d, d);
        chevDown.setBounds((right - d) / 2, areaY + areaH - d - 4, d, d);
        clampList();
    }

    private void scrollList(final int direction) {
        scrollListBy(direction * Math.round(scroller.getHeight() * 0.6f));
    }

    private void scrollListBy(final int dy) {
        if (listScrolls) {
            final JViewport viewport = scroller.getViewport();
            viewport.setViewPosition(new Point(0, viewport.getViewPosition().y + dy));
            clampList();
            list.repaint();
        }
    }

    private void clampList() {
        final JViewport viewport = scroller.getViewport();
        final int most = Math.max(0, list.getPreferredSize().height - scroller.getHeight());
        final int y = listScrolls ? Math.max(0, Math.min(most, viewport.getViewPosition().y)) : 0;
        if (viewport.getViewPosition().y != y) {
            viewport.setViewPosition(new Point(0, y));
        }
        chevUp.setVisible(listScrolls && y > 0);
        chevDown.setVisible(listScrolls && y < most);
    }

    private int answerWidth(final int w, final int h) {
        final FontMetrics fm = getFontMetrics(ArenaTheme.displayFont(Font.BOLD, Math.min(ArenaDialogKit.buttonH(h) * 0.46f, 26f)));
        int widest = 0;
        for (final Option option : options) {
            widest = Math.max(widest, fm.stringWidth(option.label));
        }
        return Math.max(ArenaDialogKit.buttonW(w), Math.min(Math.round(w * 0.26f), widest + ArenaDialogKit.buttonH(h)));
    }

    /** Equal pills in centred rows. */
    private void layoutPills(final int w, final int h) {
        final int right = rightLimit(w, h), margin = Math.round(w * 0.04f);
        final int pillH = ArenaDialogKit.buttonH(h), gap = Math.round(h * 0.014f);
        final FontMetrics fm = getFontMetrics(ArenaTheme.displayFont(Font.BOLD, Math.min(pillH * 0.46f, 26f)));
        int widest = 0;
        for (final Option option : options) {
            widest = Math.max(widest, fm.stringWidth(option.label));
        }
        final int pillW = Math.max(Math.round(w * 0.09f), Math.min(Math.round(w * 0.24f), widest + Math.round(pillH * 1.2f)));
        final int room = right - 2 * margin;
        final int n = options.size();
        final int fit = Math.max(1, (room + gap) / (pillW + gap));
        // an even wall of pills looks better than a long row and an orphan
        final int perRow = n <= fit ? n : Math.max(Math.min(fit, (int) Math.ceil(Math.sqrt(n) * 1.6)), (n + 4) / 5);
        final int rows = (n + perRow - 1) / perRow;
        final int listH = rows * pillH + (rows - 1) * gap;
        fitList(w, h, listH);
        final int top = Math.max(gap, Math.round(h * ArenaDialogKit.ROW_CENTRE_Y) - scroller.getY() - listH / 2);
        for (int i = 0; i < n; i++) {
            final int row = i / perRow;
            final int inRow = Math.min(perRow, n - row * perRow);
            final int rowW = inRow * pillW + (inRow - 1) * gap;
            final int x = Math.max(margin, Math.min((w - rowW) / 2, right - margin - rowW));
            options.get(i).setBounds(x + (i % perRow) * (pillW + gap), top + row * (pillH + gap), pillW, pillH);
        }
    }

    /** The row of card-like tiles, each of a card's shape; they narrow rather than overlap unless there are too many to read. */
    private void layoutTiles(final int w, final int h) {
        final int n = options.size();
        final boolean source = focusCard != null;
        final int room = ArenaDialogKit.rowRoom(w, h, source);
        final int gap = Math.round(w * 0.02f), narrowest = Math.round(w * 0.11f); // wider than two glows, so that they never meet
        final int th = Math.round(h * TILE_H);
        int tw = Math.round(th / CardPanel.ASPECT_RATIO);
        int step = tw + gap;
        if (n * tw + (n - 1) * gap > room) {
            final int fit = (room - (n - 1) * gap) / n;
            if (fit >= narrowest) {
                tw = fit;
                step = tw + gap;
            } else {
                tw = narrowest;
                step = Math.min(tw + gap, (room - tw) / Math.max(1, n - 1));
            }
        }
        final int rowW = tw + (n - 1) * step;
        final int left = ArenaDialogKit.rowCentre(w, h, rowW, source) - rowW / 2;
        fitList(w, h, 0);
        final int cy = Math.round(h * ArenaDialogKit.ROW_CENTRE_Y) - scroller.getY();
        final boolean overlapped = step < tw;
        for (int i = 0; i < n; i++) {
            final Option option = options.get(i);
            option.overlapped = overlapped;
            if (option != dragging) {
                option.placeTile(left + tw / 2 + i * step, cy, tw, th, 0f, false);
            }
        }
        orderCaptionX = left;
        orderCaptionRight = left + rowW;
        final int inset = overlapped ? Math.round(ArenaDialogKit.badgeSize(h) * 0.8f) : tw / 2;
        orderFirstBadge = left + inset;
        orderLastBadge = left + (n - 1) * step + inset;
        final int rowBottom = Math.round(h * ArenaDialogKit.ROW_CENTRE_Y) + th / 2;
        rowFoot = rowBottom;
        orderRailY = rowBottom + ArenaDialogKit.railDrop(h);
        orderCaptionY = rowBottom + ArenaDialogKit.orderRowDrop(h);
    }

    /** Two columns of flat tiles, all the height of the longest. */
    private void layoutGrid(final int w, final int h) {
        final int right = rightLimit(w, h), margin = Math.round(w * 0.04f);
        final int gap = Math.round(h * 0.014f);
        final int cellW = Math.min(Math.round(w * 0.42f), (right - 2 * margin - gap) / 2);
        final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D sg = scratch.createGraphics();
        ArenaDialogKit.quality(sg);
        sg.setFont(ArenaTheme.rulesFont(Font.PLAIN, h * GRID_TEXT));
        final int lineH = sg.getFontMetrics().getHeight();
        final int textW = cellW - gridTextInset(h);
        int rowH = Math.round(h * 0.075f);
        for (final Option option : options) {
            final int lines = ArenaDialogKit.wrap(option.text(), sg, textW).size();
            rowH = Math.max(rowH, Math.min(Math.round(h * 0.15f), lines * lineH + Math.round(h * 0.03f)));
        }
        sg.dispose();
        final int rows = (options.size() + 1) / 2;
        final int listH = rows * rowH + (rows - 1) * gap;
        final int pad = Math.round(h * 0.03f);
        fitList(w, h, listH + 2 * pad);
        final int top = listScrolls ? pad : Math.max(pad, (scroller.getHeight() - listH) / 2);
        final int gridW = 2 * cellW + gap;
        final int left = Math.max(margin, Math.min((w - gridW) / 2, right - margin - gridW));
        listScrollStep = rowH + gap;
        for (int i = 0; i < options.size(); i++) {
            final int col = i % 2, row = i / 2;
            options.get(i).placeTile(left + cellW / 2 + col * (cellW + gap), top + rowH / 2 + row * (rowH + gap), cellW, rowH, 0f, true);
        }
    }

    private int gridTextInset(final int h) {
        return Math.round(h * 0.085f) + Math.round(h * 0.036f);
    }

    /** The heading as {title, subtitle, what stands in for the title if it is too long}. */
    private String[] heading() {
        final Localizer localizer = Localizer.getInstance();
        if (kind == Kind.ORDER) {
            return new String[] {title, localizer.getMessage("lblArenaDragToReorder"), localizer.getMessage("lblArenaChooseOrder")};
        }
        if (kind == Kind.CHOICE) {
            final String line = message != null && !message.isEmpty() && !ArenaDialogKit.sameWords(message, title) ? message : null;
            return new String[] {title, line, localizer.getMessage("lblArenaMakeChoice")};
        }
        return new String[] {fillerTitle() ? null : title, null, null};
    }

    /** Whether the title only says that this is a question or a notice, which needs no heading. */
    private boolean fillerTitle() {
        final Localizer localizer = Localizer.getInstance();
        return title == null || title.isBlank() || title.equals(localizer.getMessage("lblArenaQuestion")) || title.equals(localizer.getMessage("lblArenaNotice"));
    }

    private int headingBottom(final int w, final int h) {
        final String[] parts = heading();
        return ArenaDialogKit.headingBottom(parts[0], parts[1], parts[2], w, h);
    }

    /** Where the prompt's card stands when it is alone above a question or a number: the same on both. */
    private static Rectangle aloneCard(final int w, final int h) {
        final int fh = Math.round(h * ALONE_CARD), fw = Math.round(fh / CardPanel.ASPECT_RATIO);
        return new Rectangle((w - fw) / 2, Math.round(h * ALONE_CARD_CY) - fh / 2, fw, fh);
    }

    /** The line through the middle of the numeral and its buttons: below the card if there is one, else level with a row. */
    private int stepperLine(final int w, final int h) {
        if (focusCard == null) {
            return Math.round(h * (ArenaDialogKit.ROW_CENTRE_Y - 0.013f));
        }
        final Rectangle card = aloneCard(w, h);
        return card.y + card.height + Math.round(h * 0.065f);
    }

    private void layoutStepper(final int w, final int h) {
        final int cy = stepperLine(w, h), d = Math.round(h * STEP_BUTTON) + 2;
        // the buttons stand clear of the widest number there is
        final FontMetrics fm = getFontMetrics(ArenaTheme.numeralFont(Font.BOLD, h * NUMERAL_SIZE));
        int widest = 0;
        for (final Option option : numbers) {
            widest = Math.max(widest, fm.stringWidth(String.valueOf(option.value)));
        }
        final int off = Math.max(Math.round(h * 0.135f), widest / 2 + Math.round(h * 0.022f) + d / 2);
        btnLower.setBounds(w / 2 - off - d / 2, cy - d / 2, d, d);
        btnHigher.setBounds(w / 2 + off - d / 2, cy - d / 2, d, d);
    }

    private void paintScene(final Graphics2D g, final int w, final int h) {
        ArenaDialogKit.paintBackdrop(g, backdrop, w, h);
        if (questionIsTitle()) {
            ArenaDialogKit.paintQuestionTitle(g, message, ArenaDialogKit.questionSize(message, w, h), w, h);
        } else {
            final String[] parts = heading();
            ArenaDialogKit.paintHeading(g, parts[0], parts[1], parts[2], w, h);
        }

        if (kind == Kind.BUTTONS) {
            paintQuestion(g, w, h);
        } else if (style == Style.STEPPER) {
            paintStepper(g, w, h);
        } else {
            if (focusCard != null) {
                final Rectangle r = ArenaDialogKit.sourceBounds(w, h);
                ArenaDialogKit.paintSource(g, focusFace(r.width, r.height), r, true);
            }
            if (kind == Kind.ORDER && orderCaptionX >= 0 && orderCaption != null && !orderCaption.isEmpty()) {
                ArenaDialogKit.paintRailEnds(g, orderCaption, ArenaDialogKit.lastCaption(orderCaption), orderFirstBadge, orderLastBadge, orderCaptionY, h);
            } else if (kind == Kind.CHOICE && style == Style.TILES && orderCaptionX >= 0) {
                ArenaDialogKit.paintRowShards(g, orderCaptionX, orderCaptionRight, rowFoot, h, this::lightAt);
            }
        }
    }

    /** The colour of the rim of the tile that stands over this x of the window. */
    private Color lightAt(final int x) {
        Option nearest = null;
        int best = Integer.MAX_VALUE;
        for (final Option option : options) {
            final int away = Math.abs(option.getX() + option.getWidth() / 2 - x);
            if (away < best) {
                best = away;
                nearest = option;
            }
        }
        return nearest != null && picked.contains(nearest) ? ArenaDialogKit.PICKED : ArenaDialogKit.CYAN;
    }

    /** A question about a card is the title itself, if it will go in two lines; a longer one, or one with no card to go with it, is body text. */
    private boolean questionIsTitle() {
        return kind == Kind.BUTTONS && focusCard != null && message != null && !message.isEmpty() && !message.contains("\n")
                && ArenaDialogKit.questionSize(message, getWidth(), getHeight()) > 0f;
    }

    private BufferedImage focusFace(final int fw, final int fh) {
        if (focusImage == null) {
            focusImage = new CachedCardImage(focusCard, matchUI.getLocalPlayers(), fw, fh) {
                @Override
                public void onImageFetched() {
                    ArenaOptionsOverlay.this.repaint();
                }
            };
        }
        final BufferedImage src = focusImage.getImage();
        if (focusFace == null || src != focusSource || focusFace.getWidth() != fw + 2 * ArenaDialogKit.FACE_PAD) {
            focusFace = ArenaDialogKit.cardFace(src, fw, fh);
            focusSource = src;
        }
        return focusFace;
    }

    /** The card as large as the space will have it, close above the buttons; any text that is not in the heading goes under it, or stands alone. */
    private void paintQuestion(final Graphics2D g, final int w, final int h) {
        final boolean hasCard = focusCard != null;
        final String text = questionIsTitle() || message == null ? "" : message;
        final int top = headingBottom(w, h), bottom = ArenaDialogKit.buttonY(h);
        List<List<String>> lines = List.of();
        float size = h * (hasCard ? 0.024f : 0.035f);
        int lineH = 0;
        if (!text.isEmpty()) {
            final int most = hasCard ? 3 : 8;
            while (true) {
                g.setFont(ArenaTheme.rulesFont(Font.PLAIN, size));
                lines = ArenaDialogKit.wrap(text, g, Math.round(w * (hasCard ? 0.6f : 0.56f)));
                if (lines.size() <= most || size <= h * 0.018f) {
                    break;
                }
                size -= 1f;
            }
            if (lines.size() > most) {
                lines = new ArrayList<>(lines.subList(0, most));
                lines.get(most - 1).add("…");
            }
            lineH = Math.round(g.getFontMetrics().getHeight() * 0.96f);
        }
        final int textH = lines.size() * lineH;
        int textY;
        if (hasCard) {
            final int cardBottom = bottom - Math.round(h * QUESTION_CARD_GAP) - (textH == 0 ? 0 : textH);
            final int cardTop = Math.max(top, Math.round(h * QUESTION_CARD_TOP)) + Math.round(h * 0.02f);
            final int fh = cardBottom - cardTop, fw = Math.round(fh / CardPanel.ASPECT_RATIO);
            final Rectangle r = new Rectangle((w - fw) / 2, cardTop, fw, fh);
            ArenaDialogKit.paintSource(g, focusFace(r.width, r.height), r, false);
            textY = cardBottom + Math.round(h * 0.012f);
        } else {
            textY = top + (bottom - top - textH) / 2;
        }
        if (!lines.isEmpty()) {
            g.setFont(ArenaTheme.rulesFont(Font.PLAIN, size));
            int baseline = textY + g.getFontMetrics().getAscent();
            for (final List<String> line : lines) {
                ArenaDialogKit.paintEdgedLine(g, line, (w - ArenaDialogKit.lineWidth(line, g)) / 2, baseline, Color.WHITE);
                baseline += lineH;
            }
        }
    }

    /** The source card when it is known, then the numeral between its buttons, with the range below it. */
    private void paintStepper(final Graphics2D g, final int w, final int h) {
        final int cx = w / 2, cy = stepperLine(w, h);
        if (focusCard != null) {
            final Rectangle r = aloneCard(w, h);
            ArenaDialogKit.paintSource(g, focusFace(r.width, r.height), r, false);
        }
        ArenaDialogKit.quality(g);
        final Font font = ArenaTheme.numeralFont(Font.BOLD, h * NUMERAL_SIZE);
        g.setFont(font);
        // by all the figures together, since those of this face stand at different heights: none of them may reach the range
        final java.awt.geom.Rectangle2D figures = font.createGlyphVector(g.getFontRenderContext(), "0123456789").getVisualBounds();
        final String n = String.valueOf(numbers.get(numberIndex).value);
        final int baseline = (int) Math.round(cy - figures.getY() - figures.getHeight() / 2);
        final FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString(n, cx - fm.stringWidth(n) / 2, baseline + 2);
        ArenaDialogKit.paintEdged(g, n, cx - fm.stringWidth(n) / 2f, baseline, Color.WHITE);

        final String range = Localizer.getInstance().getMessage("lblArenaRange",
                String.valueOf(numbers.get(0).value), String.valueOf(numbers.get(numbers.size() - 1).value));
        g.setFont(ArenaTheme.numeralFont(Font.PLAIN, h * 0.024f));
        final FontMetrics rfm = g.getFontMetrics();
        final int rangeY = (int) Math.round(baseline + figures.getMaxY()) + Math.round(h * 0.03f);
        ArenaDialogKit.paintEdged(g, range, cx - rfm.stringWidth(range) / 2f, rangeY, Color.WHITE);
    }

    private void startAnimation() {
        if (!animation.isRunning()) {
            animation.start();
        }
    }

    private void stepAnimation() {
        boolean moving = false;
        for (final Option option : options) {
            moving |= option.step();
        }
        repaint();
        if (!moving) {
            animation.stop();
        }
    }

    /**
     * Tiles raised by the mouse lie above the row, the one under the mouse above them all. A row in
     * order also has its rail here, over the tiles' rims, and over all of that the numbers on it.
     */
    private void paintRaised(final Graphics2D g) {
        if (style != Style.TILES && style != Style.GRID) {
            return;
        }
        final boolean railed = kind == Kind.ORDER && orderCaptionX >= 0;
        if (railed) {
            ArenaDialogKit.paintRail(g, orderCaptionX, orderCaptionRight, orderRailY - scroller.getY(), getHeight());
        }
        Option top = null;
        for (final Option option : options) {
            if (option.isRaised()) {
                if (option.hover || option == dragging) {
                    top = option;
                } else {
                    paintOnTop(g, option);
                }
            }
        }
        if (top != null) {
            paintOnTop(g, top);
        }
        if (railed) {
            for (final Option option : options) {
                final Graphics2D c = (Graphics2D) g.create(option.getX(), option.getY(), option.getWidth(), option.getHeight());
                try {
                    option.drawBadge(c);
                } finally {
                    c.dispose();
                }
            }
        }
    }

    private static void paintOnTop(final Graphics2D g, final Option option) {
        final Graphics2D c = (Graphics2D) g.create(option.getX(), option.getY(), option.getWidth(), option.getHeight());
        try {
            option.drawTile(c);
        } finally {
            c.dispose();
        }
    }

    private void startDrag(final Option option, final MouseEvent e) {
        if (kind != Kind.ORDER) {
            return;
        }
        dragging = option;
        dragGrabX = e.getX();
        dragMoved = false;
        startAnimation();
    }

    /** Moves the dragged tile with the mouse; the others make room as it passes them. */
    private void drag(final Option option, final MouseEvent e) {
        if (dragging != option) {
            return;
        }
        if (!dragMoved && Math.abs(e.getX() - dragGrabX) < 5) {
            return;
        }
        dragMoved = true;
        final Point p = SwingUtilities.convertPoint(option, e.getPoint(), list);
        final int x = Math.max(-option.getWidth() / 2, Math.min(list.getWidth() - option.getWidth() / 2, p.x - dragGrabX));
        option.setLocation(x, option.getY());
        final int centre = x + option.getWidth() / 2;
        int index = 0;
        for (final Option other : options) {
            if (other != option && other.getX() + other.getWidth() / 2 < centre) {
                index++;
            }
        }
        if (options.indexOf(option) != index) {
            options.remove(option);
            options.add(index, option);
            layoutTiles(getWidth(), getHeight());
        }
        repaint();
    }

    private void endDrag(final Option option) {
        if (dragging != option) {
            return;
        }
        dragging = null;
        // the rightmost lies on top again
        for (final Option other : options) {
            list.setComponentZOrder(other, 0);
        }
        layoutTiles(getWidth(), getHeight());
        startAnimation();
        repaint();
    }

    /** A round button beside the numeral; holding it keeps counting. */
    private final class StepButton extends JComponent {
        private final int direction;
        private boolean hover;
        private final Timer repeat;

        StepButton(final int direction) {
            this.direction = direction;
            repeat = new Timer(90, e -> stepNumber(direction));
            repeat.setInitialDelay(380);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hover = false;
                    repeat.stop();
                    repaint();
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    stepNumber(direction);
                    repeat.start();
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    repeat.stop();
                }
            });
        }

        private boolean atEnd() {
            return direction < 0 ? numberIndex == 0 : numberIndex == numbers.size() - 1;
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                ArenaDialogKit.paintRoundButton(g, getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) - 2, direction, 0, hover && !atEnd(), atEnd());
            } finally {
                g.dispose();
            }
        }
    }

    /** One option: a pill for a short answer, or a tile in a card's frame with a piece of art and its text. */
    private final class Option extends JComponent {
        private final int index;
        private final Object value;
        private final String label;
        private final ArenaDialogKit.Sprite sprite = new ArenaDialogKit.Sprite(ArenaDialogKit.TILE_ARC);
        private boolean primary;
        private boolean hover;
        private boolean pressed;
        private float hoverT;
        private float riseT;
        private int tw;
        private int th;
        private int restX;
        private int restY;
        private float angle;
        private boolean flat;
        private boolean overlapped;
        private CachedCardImage artImage;
        private BufferedImage faceArt;
        private BufferedImage face;
        private BufferedImage artFrom;
        private BufferedImage artCropped;

        Option(final int index, final Object value, final String label) {
            this.index = index;
            this.value = value;
            this.label = label == null ? "" : label.trim();
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hover = true;
                    final CardView card = card();
                    if (card != null) {
                        matchUI.setCard(card);
                    }
                    startAnimation();
                    repaint();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hover = false;
                    startAnimation();
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
                        onOptionClicked(Option.this, e.getClickCount());
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

        /** The card this option comes from, if any. */
        CardView card() {
            return value instanceof CardView c ? c : value instanceof SpellAbilityView sa ? sa.getHostCard() : null;
        }

        private CardView artCard() {
            final CardView card = card();
            return card != null ? card : focusCard;
        }

        private String cardName() {
            final CardView card = card();
            return card == null || card.getCurrentState() == null ? null : CardTranslation.getTranslatedName(card.getCurrentState().getName());
        }

        /** What an option in order says before its colon, when it comes as a string and names what it belongs to. */
        private String namedPrefix() {
            if (kind != Kind.ORDER || card() != null) {
                return null;
            }
            final int colon = label.indexOf(':');
            final String head = colon > 0 && colon <= 30 ? label.substring(0, colon) : "";
            return !head.isEmpty() && !head.contains("{") && Character.isUpperCase(head.charAt(0)) ? head : null;
        }

        /** The words on the tile's title bar: the card's name, or failing that which option this is. */
        private String tileTitle() {
            final String name = cardName(), prefix = namedPrefix();
            final Localizer localizer = Localizer.getInstance();
            if (name != null) {
                return name;
            }
            if (prefix != null) {
                return prefix;
            }
            if (kind == Kind.ORDER) {
                return localizer.getMessage("lblArenaAbility");
            }
            if (focusCard != null && focusCard.getCurrentState() != null) {
                return CardTranslation.getTranslatedName(focusCard.getCurrentState().getName());
            }
            return localizer.getMessage("lblArenaModeN", index + 1);
        }

        /** The type line under the art: of the card itself, or of the card a mode or ability belongs to. */
        private String tileKind() {
            final CardView card = artCard();
            final CardStateView state = card == null ? null : card.getCurrentState();
            if (state == null || state.getType() == null) {
                return null;
            }
            final String type = state.getType().toString();
            return type.isEmpty() ? null : type.replace(" - ", " \u2014 ");
        }

        /** The cost on the title bar: of that same card, when it has one. */
        private ManaCost tileCost() {
            final CardView card = artCard();
            final CardStateView state = card == null ? null : card.getCurrentState();
            final ManaCost cost = state == null ? null : state.getManaCost();
            return cost == null || cost.isNoCost() ? null : cost;
        }

        /** The frame takes the colour of the card behind the option; one with no colour of its own (devoid and so on) takes its identity's. */
        private Color tileTint() {
            final CardView card = artCard();
            final CardStateView state = card == null ? null : card.getCurrentState();
            if (state == null) {
                return NO_CARD_FRAME;
            }
            final ColorSet own = state.getColors();
            if (state.isLand() || (own != null && !own.isColorless())) {
                return ArenaTheme.tileTint(state);
            }
            final Color identity = identityTint(state);
            return identity == null ? ArenaTheme.tileTint(state) : identity;
        }

        /** The words for the tile, without the card's name where the tile already shows it. */
        String text() {
            if (value instanceof CardView c && c.getCurrentState() != null && c.getCurrentState().getOracleText() != null
                    && !c.getCurrentState().getOracleText().isEmpty()) {
                return c.getCurrentState().getOracleText();
            }
            final String prefix = namedPrefix();
            if (prefix != null) {
                final String rest = label.substring(prefix.length() + 1).trim();
                if (!rest.isEmpty()) {
                    return rest;
                }
            }
            final String name = cardName();
            if (name != null && label.startsWith(name)) {
                final String rest = label.substring(name.length()).replaceFirst("^\\s*[-:\\u2014]\\s*", "").trim();
                if (!rest.isEmpty()) {
                    return rest;
                }
            }
            return label;
        }

        private boolean isTile() {
            return style == Style.TILES || style == Style.GRID;
        }

        void placeTile(final int cx, final int cy, final int tw0, final int th0, final float angle0, final boolean flat0) {
            if (tw0 != tw || th0 != th || flat0 != flat) {
                tw = tw0;
                th = th0;
                flat = flat0;
                artImage = null;
                face = null;
            }
            angle = angle0;
            final int h = ArenaOptionsOverlay.this.getHeight();
            final int reach = Math.round(spread() * 1.5f) + 6;
            final int lift = flat ? 0 : Math.round(h * HOVER_LIFT);
            final int mx = Math.round(tw * 0.04f) + reach;
            final int my = Math.round(th * 0.04f) + reach;
            restX = mx + tw / 2;
            restY = my + lift + th / 2;
            setBounds(cx - restX, cy - restY, tw + 2 * mx, th + 2 * my + lift);
        }

        private int spread() {
            final int h = ArenaOptionsOverlay.this.getHeight();
            return flat ? Math.max(9, Math.round(h * 0.012f)) : Math.max(14, Math.round(h * 0.029f));
        }

        boolean isRaised() {
            return isTile() && (hoverT > 0f || hover || this == dragging);
        }

        boolean step() {
            if (!isTile()) {
                return false;
            }
            final float hoverGoal = hover || this == dragging ? 1f : 0f, riseGoal = picked.contains(this) ? 1f : 0f;
            final float h0 = hoverT, r0 = riseT;
            hoverT = hoverT < hoverGoal ? Math.min(hoverGoal, hoverT + 0.2f) : Math.max(hoverGoal, hoverT - 0.2f);
            riseT = riseT < riseGoal ? Math.min(riseGoal, riseT + 0.2f) : Math.max(riseGoal, riseT - 0.2f);
            return h0 != hoverT || r0 != riseT;
        }

        private float lift() {
            if (flat) {
                return 0f;
            }
            return ArenaOptionsOverlay.this.getHeight() * HOVER_LIFT * hoverT * (1f - riseT); // a picked tile stays on the row's baseline
        }

        @Override
        public boolean contains(final int x, final int y) {
            if (!isTile()) {
                return super.contains(x, y);
            }
            if (tw == 0) {
                return false;
            }
            if (ArenaDialogKit.inRotated(x, y, restX, restY, tw / 2f, th / 2f, angle)) {
                return true;
            }
            final float lift = lift();
            return lift > 0f && ArenaDialogKit.inRotated(x, y, restX, restY - lift, tw / 2f, th / 2f, angle * (1f - hoverT));
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                if (isTile()) {
                    if (!isRaised()) {
                        drawTile(g);
                    }
                } else {
                    final boolean isPicked = picked.contains(this);
                    if (kind == Kind.CHOICE && !isPicked && max > 1 && picked.size() >= max) {
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f)); // the limit is reached
                    }
                    ArenaDialogKit.paintPill(g, getWidth(), getHeight(), label, primary, hover, pressed, isPicked, true);
                }
            } finally {
                g.dispose();
            }
        }

        void drawTile(final Graphics2D g) {
            ArenaDialogKit.quality(g);
            final BufferedImage art = artCrop();
            if (face == null || art != faceArt) {
                face = renderFace(art);
                faceArt = art;
            }
            final boolean isPicked = picked.contains(this);
            final int h = ArenaOptionsOverlay.this.getHeight();
            final ArenaDialogKit.Glow light = isPicked || this == dragging ? ArenaDialogKit.chosenGlow(h) : ArenaDialogKit.selectableGlow(h, hover ? 1.3f : 1f);
            final ArenaDialogKit.Glow glow = flat ? ArenaDialogKit.smallGlow(light, h) : light;
            final boolean dimmed = kind == Kind.CHOICE && !isPicked && max > 1 && picked.size() >= max; // the limit is reached
            sprite.draw(g, face, 1, restX, Math.round(restY - lift()), 0f, tileScale(), dimmed ? null : glow, dimmed);
            if (kind != Kind.ORDER) {
                drawBadge(g); // a row in order has its numbers on the rail, painted over it
            }
        }

        private float tileScale() {
            return flat ? 1f : 1f + 0.04f * hoverT * (1f - riseT);
        }

        /** Numbers only where order matters, all in one style: straddling the bottom edge, and on the rail in a row in order. */
        void drawBadge(final Graphics2D g) {
            int place = 0;
            if (kind == Kind.ORDER) {
                place = options.indexOf(this) + 1;
            } else if (max > 1 && picked.contains(this)) {
                place = picked.indexOf(this) + 1;
            }
            if (place <= 0) {
                return;
            }
            final int h = ArenaOptionsOverlay.this.getHeight();
            final int d = ArenaDialogKit.badgeSize(h);
            final float scale = tileScale();
            final int px = overlapped ? restX - Math.round(scale * tw / 2f) + Math.round(d * 0.8f) : restX;
            final int drop = flat ? -d / 2 : kind == Kind.ORDER ? ArenaDialogKit.railDrop(h) : 0;
            ArenaDialogKit.paintBadge(g, place, px, Math.round(restY - lift()) + Math.round(scale * th / 2f) + drop, d);
        }

        /** The art of the card this option belongs to, when one is known. */
        private BufferedImage artCrop() {
            final CardView card = artCard();
            if (card == null) {
                return null;
            }
            if (artImage == null) {
                final int iw = Math.round(Math.max(tw, th) * (flat ? 1f : 1.3f)) + 20;
                artImage = new CachedCardImage(card, matchUI.getLocalPlayers(), iw, Math.round(iw * CardPanel.ASPECT_RATIO)) {
                    @Override
                    public void onImageFetched() {
                        ArenaOptionsOverlay.this.repaint();
                    }
                };
            }
            final BufferedImage src = artImage.getImage();
            if (src == null) {
                return null;
            }
            if (src != artFrom) {
                artFrom = src;
                try {
                    artCropped = ImageCache.getCroppedArt(src, 0, 0, src.getWidth(), src.getHeight());
                } catch (final RasterFormatException e) {
                    artCropped = src;
                }
            }
            return artCropped;
        }

        /** A card's frame: a thin black border, a frame in the card's colour, title bar, art, and an inset text box in a cast of that colour. */
        private BufferedImage renderFace(final BufferedImage art) {
            final int h = ArenaOptionsOverlay.this.getHeight();
            final BufferedImage out = new BufferedImage(tw + 2 * ArenaDialogKit.FACE_PAD, th + 2 * ArenaDialogKit.FACE_PAD, BufferedImage.TYPE_INT_ARGB_PRE);
            final Graphics2D g = out.createGraphics();
            try {
                ArenaDialogKit.quality(g);
                g.translate(ArenaDialogKit.FACE_PAD, ArenaDialogKit.FACE_PAD);
                final Color tint = tileTint();
                final Color line = ArenaTheme.frameLine(tint);
                final float arc = ArenaDialogKit.tileArc(tw, th);
                g.setColor(new Color(0x0B0C10));
                g.fill(new RoundRectangle2D.Float(0, 0, tw, th, arc, arc));
                final int b = flat ? Math.max(2, Math.round(th * 0.03f)) : Math.max(2, Math.round(tw * 0.014f));
                final RoundRectangle2D frame = new RoundRectangle2D.Float(b, b, tw - 2f * b, th - 2f * b, Math.max(2f, arc - b), Math.max(2f, arc - b));
                // a pale frame only darkens a little towards its foot, or it turns grey
                final boolean pale = (line.getRed() * 30 + line.getGreen() * 59 + line.getBlue() * 11) / 100 > 170;
                ArenaDialogKit.paintFrameMetal(g, new Rectangle(b, b, tw - 2 * b, th - 2 * b), mix(line, Color.WHITE, 0.22f), mix(line, Color.BLACK, pale ? 0.1f : 0.34f), Math.max(2f, arc - b), 0xF7A3EL + index);
                g.setStroke(new BasicStroke(1f));
                g.setColor(new Color(255, 255, 255, 60));
                g.draw(frame);
                final int m = b + (flat ? Math.max(3, Math.round(th * 0.05f)) : Math.max(4, Math.round(tw * 0.028f)));
                final Rectangle panel;
                if (flat) {
                    final int artW = art == null ? 0 : Math.round((th - 2 * m) * 1.32f);
                    if (art != null) {
                        drawArt(g, art, new Rectangle(m, m, artW, th - 2 * m));
                    }
                    final int px = art == null ? m : m + artW + m;
                    panel = new Rectangle(px, m, tw - px - m, th - 2 * m);
                } else {
                    final int gap = Math.max(2, Math.round(tw * 0.01f));
                    final Rectangle bar = new Rectangle(m, m, tw - 2 * m, Math.round(h * 0.036f));
                    final String kindWords = tileKind();
                    final Rectangle artRect = new Rectangle(m, bar.y + bar.height + gap, tw - 2 * m, Math.round(th * TILE_ART));
                    paintBar(g, bar, tileTitle(), h * 0.0185f, tint, tileCost());
                    if (art != null) {
                        drawArt(g, art, artRect);
                    } else {
                        paintPlainArt(g, artRect, 3f, index + 1, tint);
                    }
                    int py = artRect.y + artRect.height + gap;
                    if (kindWords != null) {
                        final Rectangle kindBar = new Rectangle(m, py, tw - 2 * m, Math.round(h * 0.028f));
                        paintBar(g, kindBar, kindWords, h * 0.0155f, tint, null);
                        py = kindBar.y + kindBar.height + gap;
                    }
                    // narrower than the art, so that the frame shows down both of its sides
                    final int inset = m + Math.max(2, Math.round(tw * 0.016f));
                    panel = new Rectangle(inset, py + Math.max(1, gap / 2), tw - 2 * inset, th - inset - py - Math.max(1, gap / 2));
                }
                final float inner = flat ? 5f : 6f;
                final RoundRectangle2D box = new RoundRectangle2D.Float(panel.x, panel.y, panel.width, panel.height, inner, inner);
                ArenaDialogKit.paintParchment(g, panel, mix(new Color(0xF8F2E3), line, 0.1f), mix(new Color(0xE8DDC6), line, 0.2f), inner, 0x5EEDL + index);
                g.setColor(new Color(0, 0, 0, 170));
                g.setStroke(new BasicStroke(1f));
                g.draw(box);
                paintText(g, panel, h);
            } finally {
                g.dispose();
            }
            return out;
        }

        /** A bar across the frame in a pale cast of the card's colour, as a card's title and type lines; a title has the cost at its right end. */
        private void paintBar(final Graphics2D g, final Rectangle bar, final String words, final float textSize, final Color tint, final ManaCost cost) {
            final float arc = bar.height * 0.42f;
            final RoundRectangle2D shape = new RoundRectangle2D.Float(bar.x, bar.y, bar.width, bar.height, arc, arc);
            g.setPaint(new GradientPaint(0, bar.y, mix(tint, PARCHMENT, 0.72f), 0, bar.y + bar.height, mix(tint, PARCHMENT, 0.5f)));
            g.fill(shape);
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(255, 255, 255, 110));
            g.drawLine(bar.x + Math.round(arc / 2f), bar.y + 1, bar.x + bar.width - Math.round(arc / 2f), bar.y + 1);
            g.setColor(new Color(0, 0, 0, 190));
            g.draw(shape);
            final int padX = Math.round(bar.height * 0.3f);
            int room = bar.width - 2 * padX;
            if (cost != null) {
                final BufferedImage pips = ArenaTheme.brightPips(cost, Math.round(bar.height * 0.6f));
                final int px = bar.x + bar.width - pips.getWidth() - Math.max(2, Math.round(bar.height * 0.14f));
                g.drawImage(pips, px, bar.y + (bar.height - pips.getHeight()) / 2, null);
                room = px - bar.x - padX - Math.round(bar.height * 0.15f);
            }
            float size = textSize;
            g.setFont(ArenaTheme.displayFont(Font.BOLD, size));
            while (size > 9f && g.getFontMetrics().stringWidth(words) > room) {
                size -= 0.5f;
                g.setFont(ArenaTheme.displayFont(Font.BOLD, size));
            }
            g.setColor(new Color(0x15110B));
            g.drawString(words, bar.x + padX, Math.round(bar.y + (bar.height + ArenaDialogKit.capHeight(g)) / 2f));
        }

        /** Rules text at the size and pitch all its siblings share, in the middle of its box. */
        private void paintText(final Graphics2D g, final Rectangle panel, final int h) {
            final String text = text();
            final int padX = Math.round(panel.width * (flat ? 0.025f : 0.055f)), padY = Math.round(h * 0.006f);
            final int textW = panel.width - 2 * padX, textH = panel.height - 2 * padY;
            final float size = tileTextSize(g, textW, textH, h, flat);
            g.setFont(ArenaTheme.rulesFont(Font.PLAIN, size));
            List<List<String>> lines = ArenaDialogKit.wrap(text, g, textW);
            final int lineH = tilePitch(size, h, flat);
            final int most = Math.max(1, textH / lineH);
            if (lines.size() > most) {
                lines = new ArrayList<>(lines.subList(0, most));
                lines.get(most - 1).add("\u2026");
            }
            // from the top of the box, every tile's first line on one height
            final float inset = Math.min(h * 0.015f, Math.max(0f, (panel.height - lines.size() * lineH) / 2f));
            int baseline = Math.round(panel.y + inset + (lineH + ArenaDialogKit.capHeight(g)) / 2f);
            g.setColor(new Color(0x1C1610));
            for (final List<String> line : lines) {
                ArenaDialogKit.drawLine(g, line, panel.x + padX, baseline, true);
                baseline += lineH;
            }
        }
    }

    /** From one line to the next: one pitch for every row of tiles, closing up only for words too small for it. */
    private static int tilePitch(final float size, final int h, final boolean flat) {
        return flat ? Math.round(size * GRID_LINE) : Math.min(Math.round(h * TILE_PITCH), Math.round(size * TILE_LEADING_MOST));
    }

    /** The one text size of a set of tiles: the largest at which the longest of them still fits. */
    private float tileTextSize(final Graphics2D g, final int textW, final int textH, final int h, final boolean flat) {
        if (textW != sizedTextW || textH != sizedTextH) {
            sizedTextW = textW;
            sizedTextH = textH;
            float size = h * (flat ? GRID_TEXT : TILE_TEXT);
            for (final Option option : options) {
                while (size > h * 0.0135f) {
                    g.setFont(ArenaTheme.rulesFont(Font.PLAIN, size));
                    if (ArenaDialogKit.wrap(option.text(), g, textW).size() * tilePitch(size, h, flat) <= textH) {
                        break;
                    }
                    size -= 0.5f;
                }
            }
            sizedText = size;
        }
        return sizedText;
    }

    /** The frame's colour from a card's colour identity, the symbols in its cost and rules; none when it has none. */
    private static Color identityTint(final CardStateView state) {
        int mask = state.getManaCost() == null ? 0 : state.getManaCost().getColorProfile();
        if (state.getOracleText() != null) {
            final Matcher symbols = BRACED.matcher(state.getOracleText());
            while (symbols.find()) {
                final String symbol = symbols.group(1);
                mask |= (symbol.indexOf('W') >= 0 ? forge.card.MagicColor.WHITE : 0) | (symbol.indexOf('U') >= 0 ? forge.card.MagicColor.BLUE : 0)
                        | (symbol.indexOf('B') >= 0 ? forge.card.MagicColor.BLACK : 0) | (symbol.indexOf('R') >= 0 ? forge.card.MagicColor.RED : 0)
                        | (symbol.indexOf('G') >= 0 ? forge.card.MagicColor.GREEN : 0);
            }
        }
        final ColorSet identity = ColorSet.fromMask(mask);
        if (identity.isColorless()) {
            return null;
        }
        if (identity.isMulticolor()) {
            return new Color(0xC9A640);
        }
        return identity.hasWhite() ? new Color(0xE2DAC0) : identity.hasBlue() ? new Color(0x2E6FB4) : identity.hasBlack() ? new Color(0x37333D)
                : identity.hasRed() ? new Color(0xBF4128) : new Color(0x2C7A46);
    }

    private static Color mix(final Color a, final Color b, final float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t), Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** The art crop with a dark keyline. */
    private static void drawArt(final Graphics2D g, final BufferedImage art, final Rectangle r) {
        drawCover(g, art, r, 3f);
        g.setColor(new Color(0, 0, 0, 190));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 3f, 3f));
    }

    /** Fills a rectangle with the image, cropping what does not fit, with smooth rounded corners. */
    private static void drawCover(final Graphics2D g, final BufferedImage img, final Rectangle r, final float arc) {
        final BufferedImage out = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D o = out.createGraphics();
        ArenaDialogKit.quality(o);
        o.setColor(Color.BLACK);
        o.fill(new RoundRectangle2D.Float(0, 0, r.width, r.height, arc, arc));
        o.setComposite(AlphaComposite.SrcAtop);
        final float scale = Math.max(r.width / (float) img.getWidth(), r.height / (float) img.getHeight());
        final int iw = Math.round(img.getWidth() * scale), ih = Math.round(img.getHeight() * scale);
        o.drawImage(img, (r.width - iw) / 2, (r.height - ih) / 2, iw, ih, null);
        o.dispose();
        g.drawImage(out, r.x, r.y, null);
    }

    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI"};

    /** When no card is known: a dark engraved plate in the frame's colour with the option's numeral, in place of art. */
    private static void paintPlainArt(final Graphics2D g, final Rectangle r, final float arc, final int place, final Color tint) {
        final RoundRectangle2D shape = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, arc, arc);
        final float cx = r.x + r.width / 2f, cy = r.y + r.height / 2f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy - r.height * 0.1f), r.width * 0.72f,
                new float[] {0f, 1f}, new Color[] {mix(tint, Color.BLACK, 0.4f), mix(tint, Color.BLACK, 0.82f)}));
        g.fill(shape);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(0, 0, 0, 190));
        g.draw(shape);
        final float ring = r.height * 0.36f;
        g.setStroke(new BasicStroke(Math.max(1.5f, r.height * 0.012f)));
        g.setColor(new Color(0, 0, 0, 90));
        g.draw(new Ellipse2D.Float(cx - ring, cy - ring + 1, 2 * ring, 2 * ring));
        g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 150));
        g.draw(new Ellipse2D.Float(cx - ring, cy - ring, 2 * ring, 2 * ring));
        g.setStroke(new BasicStroke(1f));
        g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 60));
        final float outer = ring * 1.16f;
        g.draw(new Ellipse2D.Float(cx - outer, cy - outer, 2 * outer, 2 * outer));
        final String numeral = place <= NUMERALS.length ? NUMERALS[place - 1] : String.valueOf(place);
        g.setFont(ArenaTheme.numeralFont(Font.BOLD, ring * 0.95f));
        final FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 140));
        g.drawString(numeral, cx - fm.stringWidth(numeral) / 2f + 1, cy + (fm.getAscent() - fm.getDescent()) / 2f + 2);
        g.setColor(ArenaTheme.GOLD_BRIGHT);
        g.drawString(numeral, cx - fm.stringWidth(numeral) / 2f, cy + (fm.getAscent() - fm.getDescent()) / 2f);
    }
}
