/*
 * Forge: Play Magic: the Gathering.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package forge.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forge.Singletons;
import forge.game.card.CardView;
import forge.screens.match.CMatchUI;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.Localizer;
import forge.view.FDialog;
import forge.view.arcane.CardArea;
import forge.view.arcane.CardPanel;
import forge.view.arcane.util.CardPanelMouseAdapter;

import net.miginfocom.swing.MigLayout;

/** Two-pane card-grid replacement for DualListBox when the payload is CardView. */
@SuppressWarnings("serial")
public class DualCardBox extends FDialog {

    private static final int DEFAULT_WIDTH = 750;
    private static final int DEFAULT_HEIGHT = 450;
    private static final Color INSERTION_LINE_COLOR = new Color(70, 130, 230);
    private static final Color EMPTY_TEXT_COLOR = new Color(140, 140, 140);

    /** remainingMin/Max are constraints on what stays in the pool; -1 means unbounded on that side. */
    public static List<CardView> show(final CMatchUI matchUI,
                                      final String title,
                                      final String destLabel,
                                      final int remainingMin,
                                      final int remainingMax,
                                      final List<CardView> source,
                                      final List<CardView> dest,
                                      final CardView referenceCard) {
        final Callable<List<CardView>> callable = () -> {
            DualCardBox box = new DualCardBox(matchUI, title, destLabel,
                    remainingMin, remainingMax,
                    new ArrayList<>(source),
                    dest == null ? new ArrayList<>() : new ArrayList<>(dest),
                    referenceCard);
            box.pack();
            box.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            box.setLocationRelativeTo(Singletons.getView().getFrame());
            box.setVisible(true);
            return box.getDestList();
        };
        FutureTask<List<CardView>> ft = new FutureTask<>(callable);
        FThreads.invokeInEdtAndWait(ft);
        try {
            return ft.get();
        } catch (final Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private final CMatchUI matchUI;
    private final int remainingMin;
    private final int remainingMax;
    private final List<CardView> poolList;
    private final List<CardView> destList;

    private PoolPane poolPane;
    private DestPane destPane;
    private FButton okButton;
    private FButton moveAllButton;
    private FButton moveAllBackButton;
    private FButton autoButton;
    private FLabel countLabel;
    private FLabel hotkeyHintBase;
    private FLabel hotkeyHintMin;
    private FTextField searchField;
    private String filter = "";

    private DualCardBox(final CMatchUI matchUI,
                        final String title,
                        final String destLabel,
                        final int remainingMin,
                        final int remainingMax,
                        final List<CardView> poolList,
                        final List<CardView> destList,
                        final CardView referenceCard) {
        super(true, true, "dialog");
        // Match DualListBox: title-bar X is a no-op; user must finish via OK or Auto.
        // For min==0 prompts, OK is enabled from the start so the empty selection is still accessible.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.matchUI = matchUI;
        this.remainingMin = remainingMin;
        this.remainingMax = remainingMax;
        this.poolList = poolList;
        this.destList = destList;
        setTitle(title);

        final FScrollPane poolScroll = new FScrollPane(false,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        final FScrollPane destScroll = new FScrollPane(false,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.poolPane = new PoolPane(matchUI, poolScroll);
        this.destPane = new DestPane(matchUI, destScroll);
        poolScroll.setViewportView(poolPane);
        destScroll.setViewportView(destPane);

        buildLayout(destLabel);
        installAcceptKey();
        installResizeRelayout();
        renderPanes();
        if (referenceCard != null) {
            matchUI.setCard(referenceCard);
        }
        refreshState();
    }

    public List<CardView> getDestList() {
        return destList;
    }

    @Override
    public void setVisible(final boolean visible) {
        if (visible) {
            registerActive(this);
        } else {
            unregisterActive(this);
        }
        super.setVisible(visible);
    }

    private void buildLayout(final String destLabel) {
        setLayout(new MigLayout("fill, insets 8, gap 8",
                "[sg col,fill][70!,center][sg col,fill]",
                "[shrink][shrink]2[grow,fill][shrink]0[shrink]14[shrink]"));

        add(new FLabel.Builder()
                .text(Localizer.getInstance().getMessage("lblDualCardBoxAvailable"))
                .fontAlign(SwingConstants.CENTER)
                .build(), "align center");
        add(new FLabel.Builder().text("").build(), "align center");
        add(new FLabel.Builder().text(destLabel).fontAlign(SwingConstants.CENTER).build(),
                "align center, wrap");

        searchField = new FTextField.Builder()
                .ghostText(Localizer.getInstance().getMessage("lblFilterByName"))
                .build();
        searchField.setFont(searchField.getFont().deriveFont(11f));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(final DocumentEvent e) { onFilterChanged(); }
            @Override public void removeUpdate(final DocumentEvent e) { onFilterChanged(); }
            @Override public void changedUpdate(final DocumentEvent e) { onFilterChanged(); }
        });

        countLabel = new FLabel.Builder().text("").fontAlign(SwingConstants.CENTER).build();
        add(searchField, "growx, h 22!");
        add(new FLabel.Builder().text("").build(), "");
        add(countLabel, "growx, align center, wrap");

        add(wrapPane(poolPane.getScrollPane()), "grow");
        add(buildButtonStrip(), "growy, w 70!");
        add(wrapPane(destPane.getScrollPane()), "grow, wrap");

        hotkeyHintBase = new FLabel.Builder()
                .text(Localizer.getInstance().getMessage("lblHotkeySelectHint"))
                .fontSize(10)
                .fontAlign(SwingConstants.CENTER)
                .build();
        hotkeyHintMin = new FLabel.Builder()
                .text(Localizer.getInstance().getMessage("lblHotkeySelectHintMin"))
                .fontSize(10)
                .fontAlign(SwingConstants.CENTER)
                .build();
        hotkeyHintBase.setVisible(false);
        hotkeyHintMin.setVisible(false);
        // hidemode 3 — when invisible, the hint takes no space so the row collapses.
        // span 3 centres the hint across both panes plus the button strip.
        add(hotkeyHintBase, "span 3, hidemode 3, growx, wrap");
        add(hotkeyHintMin, "span 3, hidemode 3, growx, wrap");

        okButton = new FButton(Localizer.getInstance().getMessage("lblOK"));
        okButton.addActionListener(e -> accept());
        autoButton = new FButton(Localizer.getInstance().getMessage("lblAuto"));
        autoButton.setToolTipText(Localizer.getInstance().getMessage("lblDualCardBoxAutoTooltip"));
        autoButton.addActionListener(e -> { moveAll(true); accept(); });

        final JPanel buttonRow = new JPanel(new MigLayout("insets 0, gap 24", "push[][]push", "[]"));
        buttonRow.setOpaque(false);
        buttonRow.add(okButton, "sg btn, w 110!, h 28!");
        buttonRow.add(autoButton, "sg btn, w 110!, h 28!");
        add(buttonRow, "span 3, align center");
    }

    private JPanel buildButtonStrip() {
        final JPanel strip = new JPanel(new MigLayout("insets 0, gap 4, wrap 1",
                "[grow,fill]", "push[][]push"));
        strip.setOpaque(false);
        moveAllButton = new FButton(">>");
        moveAllButton.setToolTipText(Localizer.getInstance().getMessage("lblDualCardBoxMoveAllToDest"));
        moveAllButton.addActionListener(e -> moveAll(true));
        moveAllBackButton = new FButton("<<");
        moveAllBackButton.setToolTipText(Localizer.getInstance().getMessage("lblDualCardBoxMoveAllBackToPool"));
        moveAllBackButton.addActionListener(e -> moveAll(false));
        strip.add(moveAllButton, "h 32!");
        strip.add(moveAllBackButton, "h 32!");
        return strip;
    }

    private static FPanel wrapPane(final FScrollPane scroll) {
        final FPanel wrap = new FPanel(new MigLayout("fill, insets 4", "[grow,fill]", "[grow,fill]"));
        wrap.setBackground(Color.BLACK);
        wrap.add(scroll, "grow");
        return wrap;
    }

    private void onFilterChanged() {
        filter = searchField.getText();
        renderPanes();
    }

    private void installAcceptKey() {
        final InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "dualcardbox-accept");
        am.put("dualcardbox-accept", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (okButton.isEnabled()) DualCardBox.this.accept();
            }
        });
    }

    private void accept() {
        setVisible(false);
    }

    /** Force CardArea panes to relayout cards when the dialog is resized. */
    private void installResizeRelayout() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                poolPane.revalidate();
                destPane.revalidate();
                poolPane.repaint();
                destPane.repaint();
            }
        });
    }

    private void moveAll(final boolean toDest) {
        if (toDest) {
            destList.addAll(poolList);
            poolList.clear();
        } else {
            poolList.addAll(destList);
            destList.clear();
        }
        renderPanes();
        refreshState();
    }

    private boolean activateHotkey(final int digit) {
        if (digit < 1) return false;
        final int destSize = destList.size();
        if (digit <= destSize) {
            final CardView card = destList.remove(digit - 1);
            poolList.add(card);
            renderPanes();
            refreshState();
            return true;
        }
        final List<CardView> visible = filteredPool();
        final int poolIndex = digit - destSize - 1;
        if (poolIndex >= visible.size()) return false;
        final CardView card = visible.get(poolIndex);
        if (!poolList.remove(card)) return false;
        destList.add(card);
        renderPanes();
        refreshState();
        return true;
    }

    /** Ctrl+0 handler: move the first N pool cards to dest, where N is the count needed to satisfy remainingMax. */
    private boolean activateMinFill() {
        final int need = minMoveNeeded();
        if (need < 1) return false;
        final List<CardView> visible = filteredPool();
        if (visible.isEmpty()) return false;
        final int take = Math.min(need, visible.size());
        for (int i = 0; i < take; i++) {
            final CardView card = visible.get(i);
            if (poolList.remove(card)) destList.add(card);
        }
        renderPanes();
        refreshState();
        return true;
    }

    private List<CardView> filteredPool() {
        if (filter.isEmpty()) return new ArrayList<>(poolList);
        final String needle = filter.toLowerCase(Locale.ROOT);
        final List<CardView> out = new ArrayList<>();
        for (final CardView c : poolList) {
            if (c.getName().toLowerCase(Locale.ROOT).contains(needle)) out.add(c);
        }
        return out;
    }

    private void renderPanes() {
        renderPool();
        renderDest();
    }

    private void renderPool() {
        final List<CardView> visible = filteredPool();
        final List<CardPanel> panels = new ArrayList<>();
        for (final CardView card : visible) {
            CardPanel panel = poolPane.getCardPanel(card.getId());
            if (panel == null) {
                panel = new CardPanel(matchUI, card);
                panel.setDisplayEnabled(true);
            } else {
                panel.setCard(card);
            }
            panels.add(panel);
        }
        poolPane.setCardPanels(panels);
        poolPane.revalidate();
        poolPane.repaint();
        // Re-apply badges so the numbering tracks the live pool order while Ctrl is held;
        // dispatchHotkey alone only fires on Ctrl press/release, not on intra-hold mutations.
        assignHotkeyAffordance(!ctrlHeld);
    }

    /** Pool digits continue after dest's order digits so 1..9 picks a unique card across both panes. */
    private void assignHotkeyAffordance(final boolean clear) {
        int next = destList.size() + 1;
        for (final CardPanel panel : poolPane.getCardPanels()) {
            if (!clear && next <= 9) {
                panel.setHotkeyDigit(next++);
            } else {
                panel.setHotkeyDigit(0);
            }
        }
        poolPane.repaint();
    }

    private void refreshHotkeyHints() {
        hotkeyHintBase.setVisible(true);
        hotkeyHintMin.setVisible(hasMinFillCapability());
    }

    /** True iff the remainingMax constraint forces at least one card from this dialog to be moved to dest. */
    private boolean hasMinFillCapability() {
        if (remainingMax < 0) return false;
        return (poolList.size() + destList.size()) > remainingMax;
    }

    /** How many cards must move from pool to dest now to satisfy the remainingMax constraint. */
    private int minMoveNeeded() {
        if (remainingMax < 0) return 0;
        return Math.max(0, poolList.size() - remainingMax);
    }

    private void renderDest() {
        final List<CardPanel> panels = new ArrayList<>();
        for (int i = 0; i < destList.size(); i++) {
            final CardView card = destList.get(i);
            CardPanel panel = destPane.getCardPanel(card.getId());
            if (panel == null) {
                panel = new CardPanel(matchUI, card);
                panel.setDisplayEnabled(true);
            } else {
                panel.setCard(card);
            }
            panel.setHotkeyDigit(i + 1);
            panels.add(panel);
        }
        destPane.setCardPanels(panels);
        destPane.revalidate();
        destPane.repaint();
    }

    private void refreshState() {
        final int poolSize = poolList.size();
        final boolean withinMin = remainingMin < 0 || poolSize >= remainingMin;
        final boolean withinMax = remainingMax < 0 || poolSize <= remainingMax;
        okButton.setEnabled(withinMin && withinMax);
        countLabel.setText(formatCount(poolSize, withinMin));

        moveAllButton.setEnabled(!poolList.isEmpty());
        moveAllBackButton.setEnabled(!destList.isEmpty());
        autoButton.setEnabled(canAuto());
        refreshHotkeyHints();
    }

    /** Auto = "move everything from pool to dest, then accept" — only when that result satisfies constraints. */
    private boolean canAuto() {
        return !poolList.isEmpty() && remainingMin <= 0;
    }

    private String formatCount(final int poolSize, final boolean withinMin) {
        final int destSize = destList.size();
        if (!withinMin) {
            return Localizer.getInstance().getMessage("lblDualCardBoxSelectedAtLeast",
                    String.valueOf(destSize), String.valueOf(remainingMin - poolSize));
        }
        final int totalUpperBound = remainingMin < 0 ? (poolSize + destSize) : (poolSize + destSize - remainingMin);
        return Localizer.getInstance().getMessage("lblDualCardBoxSelectedCount",
                String.valueOf(destSize), String.valueOf(totalUpperBound));
    }

    /** Cap cardWidthMax so cardHeight (=cardWidth*ASPECT_RATIO) fits in the pane height —
     *  CardArea's horizontal-mode loop only checks row width, so without this a single card
     *  can render taller than the viewport. */
    private static void clampCardWidthMaxToHeight(final CardArea pane) {
        final Rectangle vis = pane.getScrollPane().getVisibleRect();
        final Insets insets = pane.getScrollPane().getInsets();
        final int avail = vis.height - insets.top - insets.bottom - 2 * CardArea.GUTTER_Y;
        if (avail <= 0) return;
        final int maxByHeight = (int) (avail / CardPanel.ASPECT_RATIO);
        pane.setCardWidthMax(Math.max(pane.getCardWidthMin(), Math.min(300, maxByHeight)));
    }

    private static void paintEmptyText(final Graphics g, final JComponent c, final String text) {
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(EMPTY_TEXT_COLOR);
            final Font base = c.getFont();
            g2.setFont(base.deriveFont(Font.ITALIC, base.getSize2D() + 1f));
            final FontMetrics fm = g2.getFontMetrics();
            final int textWidth = fm.stringWidth(text);
            final int x = (c.getWidth() - textWidth) / 2;
            final int y = c.getHeight() / 2 + fm.getAscent() / 2;
            g2.drawString(text, x, y);
        } finally {
            g2.dispose();
        }
    }

    final class PoolPane extends CardArea {
        PoolPane(final CMatchUI matchUI, final FScrollPane scrollPane) {
            super(matchUI, scrollPane);
            setVertical(false);
            // No overlap: shrink + wrap to rows instead of fanning cards across.
            setMaxCoverage(0f);
            setOpaque(false);
            setDragEnabled(true);
            addCardPanelMouseListener(new CardPanelMouseAdapter() {
                @Override
                public void mouseDragged(final CardPanel dragPanel, final int dx, final int dy, final MouseEvent evt) {
                    destPane.setDragInsertionFromScreen(evt.getXOnScreen(), evt.getYOnScreen());
                }

                @Override
                public void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
                    destPane.clearDragInsertion();
                    handlePoolDragEnd(dragPanel, evt);
                }
            });
        }

        @Override
        public void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
            final CardView card = panel.getCard();
            if (poolList.remove(card)) {
                destList.add(card);
            }
            matchUI.setCard(card);
            renderPanes();
            refreshState();
            super.mouseLeftClicked(panel, evt);
        }

        @Override
        public void doLayout() {
            clampCardWidthMaxToHeight(this);
            super.doLayout();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (getCardPanels().isEmpty()) {
                paintEmptyText(g, this,
                        Localizer.getInstance().getMessage("lblDualCardBoxEmptyPool"));
            }
        }
    }

    final class DestPane extends CardArea {
        private int dragInsertionIndex = -1;

        DestPane(final CMatchUI matchUI, final FScrollPane scrollPane) {
            super(matchUI, scrollPane);
            setVertical(false);
            setMaxCoverage(0f);
            setOpaque(false);
            setDragEnabled(true);
            addCardPanelMouseListener(new CardPanelMouseAdapter() {
                @Override
                public void mouseDragStart(final CardPanel dragPanel, final MouseEvent evt) {
                    setDragInsertion(getCardPanels().indexOf(dragPanel));
                }

                @Override
                public void mouseDragged(final CardPanel dragPanel, final int dx, final int dy, final MouseEvent evt) {
                    setDragInsertion(getCardPanels().indexOf(dragPanel));
                }

                @Override
                public void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
                    clearDragInsertion();
                    handleDestDragEnd(dragPanel, evt);
                }
            });
        }

        void setDragInsertion(final int index) {
            if (index == dragInsertionIndex) return;
            dragInsertionIndex = index;
            repaint();
        }

        void clearDragInsertion() {
            setDragInsertion(-1);
        }

        void setDragInsertionFromScreen(final int screenX, final int screenY) {
            final Point top = getLocationOnScreen();
            final int relX = screenX - top.x;
            final int relY = screenY - top.y;
            if (relX < 0 || relY < 0 || relX >= getWidth() || relY >= getHeight()) {
                clearDragInsertion();
                return;
            }
            setDragInsertion(insertionIndexAt(relX, relY));
        }

        private int insertionIndexAt(final int x, final int y) {
            final List<CardPanel> panels = getCardPanels();
            if (panels.isEmpty()) return 0;
            final CardPanel hit = getCardPanel(x, y);
            if (hit != null) {
                final int idx = panels.indexOf(hit);
                final int midX = hit.getCardX() + hit.getCardWidth() / 2;
                return x < midX ? idx : idx + 1;
            }
            return panels.size();
        }

        @Override
        public void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
            final CardView card = panel.getCard();
            if (destList.remove(card)) {
                poolList.add(card);
            }
            matchUI.setCard(card);
            renderPanes();
            refreshState();
            super.mouseLeftClicked(panel, evt);
        }

        @Override
        public void doLayout() {
            clampCardWidthMaxToHeight(this);
            super.doLayout();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final List<CardPanel> panels = getCardPanels();
            if (panels.isEmpty() && dragInsertionIndex < 0) {
                paintEmptyText(g, this,
                        Localizer.getInstance().getMessage("lblDualCardBoxEmptyDest"));
                return;
            }
            if (dragInsertionIndex < 0) return;
            final int x, y, height;
            if (panels.isEmpty()) {
                x = getWidth() / 2;
                y = 12;
                height = Math.max(0, getHeight() - 24);
            } else if (dragInsertionIndex >= panels.size()) {
                final CardPanel last = panels.get(panels.size() - 1);
                x = last.getCardX() + last.getCardWidth() + 2;
                y = last.getCardY();
                height = last.getCardHeight();
            } else {
                final CardPanel target = panels.get(dragInsertionIndex);
                x = target.getCardX() - 2;
                y = target.getCardY();
                height = target.getCardHeight();
            }
            final Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(INSERTION_LINE_COLOR);
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(x, y, x, y + height);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Returns true if the drag-release point is over the opposite pane (cross-pane move). */
    private boolean releasedOverOtherPane(final MouseEvent evt, final CardArea sourcePane) {
        final CardArea otherPane = (sourcePane == poolPane) ? destPane : poolPane;
        final Point screenPt = new Point(evt.getXOnScreen(), evt.getYOnScreen());
        final Point otherTopLeft = otherPane.getLocationOnScreen();
        final int relX = screenPt.x - otherTopLeft.x;
        final int relY = screenPt.y - otherTopLeft.y;
        return relX >= 0 && relY >= 0 && relX < otherPane.getWidth() && relY < otherPane.getHeight();
    }

    private void handlePoolDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
        if (releasedOverOtherPane(evt, poolPane)) {
            final CardView card = dragPanel.getCard();
            if (poolList.remove(card)) {
                destList.add(card);
            }
            renderPanes();
            refreshState();
            suppressDragGhost();
        }
    }

    private void handleDestDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
        if (releasedOverOtherPane(evt, destPane)) {
            final CardView card = dragPanel.getCard();
            if (destList.remove(card)) {
                poolList.add(card);
            }
            renderPanes();
            refreshState();
            suppressDragGhost();
            return;
        }
        final CardView card = dragPanel.getCard();
        final int newIndex = destPane.getCardPanels().indexOf(dragPanel);
        if (newIndex < 0) return;
        final int oldIndex = destList.indexOf(card);
        if (oldIndex == newIndex) return;
        destList.remove(card);
        destList.add(newIndex, card);
        renderPanes();
        refreshState();
    }

    /**
     * After a cross-pane drop, the source pane's drag panel has been disposed via renderPanes().
     * CardArea.mouseDragEnd (final, can't override) would otherwise tween the ghost back to that
     * stale source-pane location. Hiding the ghost makes the tween run on an invisible panel.
     */
    private static void suppressDragGhost() {
        final CardPanel ghost = CardPanel.getDragAnimationPanel();
        if (ghost != null) ghost.setVisible(false);
    }

    private static final Set<DualCardBox> activeDialogs = new LinkedHashSet<>();
    private static boolean dispatcherInstalled;
    private static boolean ctrlHeld;

    private static void registerActive(final DualCardBox dlg) {
        ensureDispatcherInstalled();
        activeDialogs.add(dlg);
    }

    private static void unregisterActive(final DualCardBox dlg) {
        activeDialogs.remove(dlg);
    }

    private static void ensureDispatcherInstalled() {
        if (dispatcherInstalled) return;
        dispatcherInstalled = true;
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(DualCardBox::dispatchHotkey);
    }

    private static boolean dispatchHotkey(final KeyEvent e) {
        if (activeDialogs.isEmpty()) return false;
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            if (e.getID() == KeyEvent.KEY_PRESSED) ctrlHeld = true;
            else if (e.getID() == KeyEvent.KEY_RELEASED) ctrlHeld = false;
            for (final DualCardBox dlg : activeDialogs) {
                dlg.assignHotkeyAffordance(!ctrlHeld);
            }
        }
        if (e.getID() != KeyEvent.KEY_PRESSED) return false;
        // Esc clears non-empty filter when search field has focus, instead of closing the dialog.
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
            for (final DualCardBox dlg : activeDialogs) {
                if (dlg.searchField.isFocusOwner() && !dlg.searchField.getText().isEmpty()) {
                    dlg.searchField.setText("");
                    return true;
                }
            }
            return false;
        }
        if (!e.isControlDown() || e.isAltDown() || e.isMetaDown()) return false;
        final int digit = e.getKeyCode() - KeyEvent.VK_0;
        if (digit < 0 || digit > 9) return false;
        if (digit == 0) {
            for (final DualCardBox dlg : activeDialogs) {
                if (dlg.activateMinFill()) return true;
            }
            return false;
        }
        for (final DualCardBox dlg : activeDialogs) {
            if (dlg.activateHotkey(digit)) return true;
        }
        return false;
    }
}
