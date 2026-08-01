/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import forge.CachedCardImage;
import forge.game.GameView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.match.YieldUpdate;
import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.interfaces.IGameController;
import forge.localinstance.skin.FSkinProp;
import forge.player.AutoYieldStore.TriggerDecision;
import forge.screens.match.controllers.CDock.ArcState;
import forge.screens.match.controllers.CStack;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedTextArea;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;
import forge.view.arcane.CardPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of stack report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VStack implements IVDoc<CStack> {

    /** Height of the strip naming the player who put the item on the stack. */
    private static final int HEADER_HEIGHT = 17;
    /** Card width used when the cascade is free to pick its own size. */
    private static final int BASE_CARD_WIDTH = 210;
    private static final int MIN_CARD_HEIGHT = 90;
    /** Fraction of a card left uncovered by the item cascaded over it. */
    private static final float PEEK = 0.12f;
    /** Share of the screen height the cascade grows to before it starts shrinking cards. */
    private static final float MAX_HEIGHT_FRACTION = 0.72f;
    /** Width of the text list, and how much wider the window gets when it opens. */
    private static final int TEXT_PANEL_WIDTH = 250;
    /** Size of the title-bar control that opens the text view. */
    private static final int TOGGLE_SIZE = 21;
    /** Settling time before card images are re-fetched at a new size. */
    private static final int IMAGE_REFRESH_DELAY_MS = 250;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(Localizer.getInstance().getMessage("lblStack"));

    // Top-level containers
    private final CascadePanel cascade = new CascadePanel();
    private final TextPanel textPanel = new TextPanel();
    private final FLabel btnToggleText;

    // Other fields
    private final AbilityMenu abilityMenu = new AbilityMenu();
    private final Timer imageRefreshTimer;

    private StackItemPanel hoveredItem;
    /** True once the window is a size the user chose, so the cascade has to fit it. */
    private boolean fitToContainer;

    public StackItemPanel getHoveredItem() {
        return hoveredItem;
    }

    private final CStack controller;
    public VStack(final CStack controller) {
        this.controller = controller;

        btnToggleText = new FLabel.ButtonBuilder()
                .icon(FSkin.getIcon(FSkinProp.ICO_DECKLIST))
                .iconScaleAuto(true)
                .tooltip(Localizer.getInstance().getMessage("lblStack"))
                .selectable()
                .cmdClick((UiCommand) this::toggleTextList)
                .build();
        btnToggleText.setPreferredSize(new Dimension(TOGGLE_SIZE, TOGGLE_SIZE));
        textPanel.setVisible(false);

        imageRefreshTimer = new Timer(IMAGE_REFRESH_DELAY_MS, e -> {
            for (final StackItemPanel panel : cascade.items) {
                panel.refreshImage();
            }
            cascade.repaint();
        });
        imageRefreshTimer.setRepeats(false);
    }

    @Override
    public void populate() {
        populateInto(parentCell.getBody());
    }

    /**
     * Lays the cascade out into the given container. Used both for the docked
     * cell and for {@link FloatingStack}, so the two share one set of components.
     */
    public void populateInto(final JPanel container) {
        container.removeAll();
        //hidemode 3 so the text panel takes no space at all while it's closed
        container.setLayout(new MigLayout("insets 0, gap 0, hidemode 3"));
        container.add(cascade, "cell 0 0, grow, push");
        container.add(textPanel, "cell 1 0, growy, push, w " + TEXT_PANEL_WIDTH + "!");
    }

    /** The title-bar control that opens and closes the text view. */
    public FLabel getTextToggle() {
        return btnToggleText;
    }

    /** Opens or closes the plain-text view of the stack alongside the cards. */
    private void toggleTextList() {
        final boolean show = !textPanel.isVisible();
        textPanel.setVisible(show);
        controller.stackLayoutChanged(show ? TEXT_PANEL_WIDTH : -TEXT_PANEL_WIDTH);
    }

    /**
     * Whether the cascade has to fit the container it's in, rather than sizing
     * itself. Set once the window is a size the user picked.
     */
    public void setFitToContainer(final boolean fitToContainer0) {
        fitToContainer = fitToContainer0;
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_STACK;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CStack getLayoutControl() {
        return controller;
    }

    public void updateStack() {
        final GameView model = controller.getMatchUI().getGameView();

        if (model == null) {
            return;
        }

        final FCollectionView<StackItemView> items = model.getStack();
        tab.setText(Localizer.getInstance().getMessage("lblStack") + " : " + items.size());

        hoveredItem = null;
        cascade.clear();
        textPanel.clear();

        final Iterable<StackItemView> safeItems = controller.getMatchUI().isNetGame()
                ? items.threadSafeIterable() : items;

        boolean isFirst = true;
        for (final StackItemView item : safeItems) {
            cascade.addItem(new StackItemPanel(item));
            textPanel.addRow(new StackTextRow(item));

            //update the Card Picture/Detail when the spell is added to the stack
            if (isFirst) {
                isFirst = false;
                controller.getMatchUI().setCard(item.getSourceCard());
            }
        }

        // Default the targeting arc to the item resolving next, as the list did.
        setHovered(cascade.items.isEmpty() ? null : cascade.items.get(0));

        cascade.arrange();
        cascade.revalidate();
        cascade.repaint();
        textPanel.revalidate();
        textPanel.repaint();
    }

    /** Points the targeting arc, the card detail and the text list at one item. */
    private void setHovered(final StackItemPanel panel) {
        hoveredItem = panel;
        final int index = panel == null ? -1 : cascade.items.indexOf(panel);
        for (int i = 0; i < textPanel.rows.size(); i++) {
            textPanel.rows.get(i).setHighlighted(i == index);
        }
    }

    /**
     * Stacks the items so each one covers all but the top sliver of the one
     * before it, with the item resolving next fully visible at the bottom.
     */
    @SuppressWarnings("serial")
    private class CascadePanel extends JPanel {
        /** The items in stack order — first is the one resolving next. */
        private final List<StackItemPanel> items = new ArrayList<>();

        private int cardWidth = BASE_CARD_WIDTH;
        private int cardHeight = Math.round(BASE_CARD_WIDTH * CardPanel.ASPECT_RATIO);

        CascadePanel() {
            setLayout(null); //items are positioned by hand, and deliberately overlap
            setOpaque(false);
        }

        void clear() {
            items.clear();
            removeAll();
        }

        void addItem(final StackItemPanel panel) {
            items.add(panel);
            add(panel);
        }

        @Override
        public void doLayout() {
            arrange();
        }

        void arrange() {
            final int count = items.size();
            if (count == 0) {
                setPreferredSize(new Dimension(0, 0));
                return;
            }

            final int availWidth = fitToContainer ? getWidth() : 0;
            final int availHeight = fitToContainer ? getHeight()
                    : Math.round(Toolkit.getDefaultToolkit().getScreenSize().height * MAX_HEIGHT_FRACTION);

            int height = Math.round((availWidth > 0 ? availWidth : BASE_CARD_WIDTH) * CardPanel.ASPECT_RATIO);
            if (availHeight > 0) {
                // Solving (count - 1) * (HEADER_HEIGHT + h * PEEK) + HEADER_HEIGHT + h <= availHeight for h.
                height = Math.min(height,
                        Math.round((availHeight - count * HEADER_HEIGHT) / ((count - 1) * PEEK + 1f)));
            }
            cardHeight = Math.max(MIN_CARD_HEIGHT, height);
            cardWidth = Math.round(cardHeight / CardPanel.ASPECT_RATIO);

            final int itemHeight = HEADER_HEIGHT + cardHeight;
            int step = HEADER_HEIGHT + Math.round(cardHeight * PEEK);
            if (count > 1 && (count - 1) * step + itemHeight > availHeight && availHeight > 0) {
                //cards are already as small as they go, so tighten the overlap instead
                step = Math.max(HEADER_HEIGHT, (availHeight - itemHeight) / (count - 1));
            }

            // The top of the stack sits lowest, where nothing covers it.
            final int x = Math.max(0, (getWidth() - cardWidth) / 2);
            for (int i = 0; i < count; i++) {
                items.get(i).setBounds(x, (count - 1 - i) * step, cardWidth, itemHeight);
            }
            arrangeZOrder();

            final int totalHeight = (count - 1) * step + itemHeight;
            final Dimension preferred = new Dimension(cardWidth, totalHeight);
            if (!preferred.equals(getPreferredSize())) {
                setPreferredSize(preferred); //re-runs layout once, then settles
            }
            if (items.get(0).needsImageRefresh(cardWidth, cardHeight)) {
                imageRefreshTimer.restart(); //wait for a drag-resize to settle before re-fetching
            }
            textPanel.arrange(step, totalHeight);
        }

        /** Swing paints children back to front, so index 0 ends up on top. */
        void arrangeZOrder() {
            for (int i = 0; i < items.size(); i++) {
                setComponentZOrder(items.get(i), i);
            }
        }
    }

    /**
     * The plain-text view of the stack. Each line sits level with the card it
     * describes, so the two columns read across.
     */
    @SuppressWarnings("serial")
    private class TextPanel extends JPanel {
        private final List<StackTextRow> rows = new ArrayList<>();

        private int step;
        private int totalHeight;

        TextPanel() {
            setLayout(null); //rows are placed to match the cascade, not packed
            setOpaque(false);
        }

        void clear() {
            rows.clear();
            removeAll();
        }

        void addRow(final StackTextRow row) {
            rows.add(row);
            add(row);
        }

        @Override
        public void doLayout() {
            arrange(step, totalHeight);
        }

        void arrange(final int step0, final int totalHeight0) {
            step = step0;
            totalHeight = totalHeight0;

            final int count = rows.size();
            final int width = getWidth() > 0 ? getWidth() : TEXT_PANEL_WIDTH;
            for (int i = 0; i < count; i++) {
                final int y = (count - 1 - i) * step;
                // Every row but the bottom one has only its card's sliver of space;
                // the bottom one is level with the fully visible card, so it gets the rest.
                final int height = i == 0 ? totalHeight - y : step - 2;
                rows.get(i).setBounds(0, y, width, Math.max(HEADER_HEIGHT, height));
            }

            final Dimension preferred = new Dimension(TEXT_PANEL_WIDTH, totalHeight);
            if (!preferred.equals(getPreferredSize())) {
                setPreferredSize(preferred);
            }
        }
    }

    /** One item on the stack: who put it there, and the card it came from. */
    @SuppressWarnings("serial")
    public class StackItemPanel extends JPanel {
        private final StackItemView item;
        private final Color headerColor;
        private final String headerText;

        private CachedCardImage cachedImage;
        private int imageWidth;
        private int imageHeight;

        public StackItemView getItem() {
            return item;
        }

        StackItemPanel(final StackItemView item0) {
            item = item0;
            setOpaque(false);

            final boolean optional = item.isOptionalTrigger()
                    && controller.getMatchUI().isLocalPlayer(item.getActivatingPlayer());
            final PlayerView activator = item.getActivatingPlayer();
            headerText = (optional ? "(OPTIONAL) " : "") + (activator == null ? "" : activator.getName());
            setToolTipText((optional ? "(OPTIONAL) " : "") + item.getText());

            // TODO: A hacky workaround is currently used to make the game not leak the color information for Morph cards.
            final CardStateView curState = item.getSourceCard().getCurrentState();
            final boolean isFaceDown = item.getSourceCard().isFaceDown();
            final DetailColors color = isFaceDown ? CardDetailUtil.DetailColors.FACE_DOWN : CardDetailUtil.getBorderColor(curState, true); // otherwise doesn't work correctly for face down Morphs
            headerColor = new Color(color.r, color.g, color.b);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (controller.getMatchUI().getCDock().getArcState() == ArcState.MOUSEOVER)	{
                        setHovered(StackItemPanel.this);
                    }
                    controller.getMatchUI().setCard(item.getSourceCard());
                    raise(true);
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (controller.getMatchUI().getCDock().getArcState() == ArcState.MOUSEOVER)	{
                        if (hoveredItem == StackItemPanel.this) {
                            setHovered(null);
                        }
                    }
                    raise(false);
                }

                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (controller.getMatchUI().getCDock().getArcState() == ArcState.ON) {
                        if (hoveredItem == StackItemPanel.this) {
                            setHovered(null);
                        }
                        else
                        {
                            setHovered(StackItemPanel.this);
                            controller.getMatchUI().setCard(item.getSourceCard());
                        }
                    }
                }
            });

            addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftClick(final MouseEvent e) {
                    onClick(e);
                }
                @Override
                public void onRightClick(final MouseEvent e) {
                    onClick(e);
                }
                private void onClick(final MouseEvent e) {
                    abilityMenu.setStackInstance(item);
                    boolean hasVisibleItem = false;
                    for (Component c : abilityMenu.getComponents()) {
                        if (c.isVisible()) {
                            hasVisibleItem = true;
                            break;
                        }
                    }
                    if (!hasVisibleItem) {
                        return;
                    }
                    abilityMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            });
        }

        /** True once the panel has been resized far enough to be worth re-fetching the image. */
        boolean needsImageRefresh(final int cardWidth, final int cardHeight) {
            return cachedImage == null || imageWidth != cardWidth || imageHeight != cardHeight;
        }

        /** Re-requests the card image at the size the panel now draws it at. */
        void refreshImage() {
            imageWidth = cascade.cardWidth;
            imageHeight = cascade.cardHeight;
            final float screenScale = GuiBase.getInterface().getScreenScale();
            cachedImage = new CachedCardImage(item.getSourceCard(), controller.getMatchUI().getLocalPlayers(),
                    Math.round(imageWidth * screenScale), Math.round(imageHeight * screenScale)) {
                @Override
                public void onImageFetched() {
                    repaint();
                }
            };
            repaint();
        }

        /** Brings a hovered item out from under the ones cascaded over it. */
        private void raise(final boolean hovered) {
            final Component parent = getParent();
            if (!(parent instanceof CascadePanel)) { return; }
            if (hovered) {
                ((CascadePanel) parent).setComponentZOrder(this, 0);
            } else {
                ((CascadePanel) parent).arrangeZOrder();
            }
            parent.repaint();
        }

        /** Where a targeting arc from this item starts, in screen coordinates. */
        public Point getArcOrigin() {
            try {
                final Point p = getLocationOnScreen();
                p.x += Math.round(getWidth() * CardPanel.TARGET_ORIGIN_FACTOR_X);
                p.y += HEADER_HEIGHT + Math.round((getHeight() - HEADER_HEIGHT) * CardPanel.TARGET_ORIGIN_FACTOR_Y);
                return p;
            } catch (final Exception e) {
                //suppress exception that can occur if stack hidden while over an item
                if (hoveredItem == this) {
                    hoveredItem = null; //reset this if this happens
                }
                return null;
            }
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (cachedImage == null) {
                refreshImage();
            }

            final int cardWidth = getWidth();
            final int cardHeight = getHeight() - HEADER_HEIGHT;
            final Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setFont(FSkin.getFont().getBaseFont());

            final int cornerSize = Math.max(4, Math.round(cardWidth * CardPanel.ROUNDED_CORNER_SIZE));
            final Shape fullClip = g2d.getClip();

            //header naming whoever put the item on the stack; rounded on top, flat where it meets the card
            g2d.setColor(headerColor);
            g2d.clipRect(0, 0, cardWidth, HEADER_HEIGHT);
            g2d.fillRoundRect(0, 0, cardWidth, HEADER_HEIGHT + cornerSize, cornerSize, cornerSize);
            g2d.setClip(fullClip);
            g2d.setColor(FSkin.getHighContrastColor(headerColor));
            final FontMetrics headerMetrics = g2d.getFontMetrics();
            g2d.drawString(clip(headerText, headerMetrics, cardWidth - 8), 4,
                    (HEADER_HEIGHT + headerMetrics.getAscent()) / 2 - 1);

            //the card itself
            final BufferedImage img = cachedImage.getImage();
            g2d.setColor(Color.black);
            g2d.fillRoundRect(0, HEADER_HEIGHT, cardWidth, cardHeight, cornerSize, cornerSize);
            if (img != null) {
                g2d.clipRect(0, HEADER_HEIGHT, cardWidth, cardHeight);
                g2d.drawImage(img, 0, HEADER_HEIGHT, cardWidth, cardHeight, null);
                g2d.setClip(fullClip);
            }

            if (hoveredItem == this) {
                g2d.setColor(FSkin.getColor(FSkin.Colors.CLR_ACTIVE).getColor());
                g2d.drawRoundRect(0, 0, cardWidth - 1, getHeight() - 1, cornerSize, cornerSize);
            }

            g2d.dispose();
        }
    }

    /** One line of the plain-text view of the stack. */
    @SuppressWarnings("serial")
    private class StackTextRow extends SkinnedTextArea {
        private static final int PADDING = 3;

        StackTextRow(final StackItemView item) {
            final String txt = (item.isOptionalTrigger() && controller.getMatchUI().isLocalPlayer(item.getActivatingPlayer())
                    ? "(OPTIONAL) " : "") + item.getText();

            setText(txt);
            setOpaque(true);
            setFocusable(false);
            setEditable(false);
            setLineWrap(true);
            setFont(FSkin.getFont());
            setWrapStyleWord(true);
            setHighlighted(false);

            final CardStateView curState = item.getSourceCard().getCurrentState();
            final boolean isFaceDown = item.getSourceCard().isFaceDown();
            final DetailColors color = isFaceDown ? CardDetailUtil.DetailColors.FACE_DOWN : CardDetailUtil.getBorderColor(curState, true);
            setBackground(new Color(color.r, color.g, color.b));
            setForeground(FSkin.getHighContrastColor(getBackground()));
        }

        /** Marks the row matching the card the mouse is over in the cascade. */
        void setHighlighted(final boolean highlighted) {
            if (highlighted) {
                setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_ACTIVE), PADDING));
            } else {
                setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
            }
        }
    }

    /** Truncates text with an ellipsis so it fits the given width. */
    private static String clip(final String text, final FontMetrics fm, final int width) {
        if (fm.stringWidth(text) <= width) { return text; }
        int len = text.length();
        while (len > 0 && fm.stringWidth(text.substring(0, len) + "…") > width) {
            len--;
        }
        return text.substring(0, len) + "…";
    }

    //========= Custom class handling

    private final class AbilityMenu extends JPopupMenu {
        private static final long serialVersionUID = 1548494191627807962L;
        private final JCheckBoxMenuItem jmiAutoYield;
        private final JCheckBoxMenuItem jmiAlwaysYes;
        private final JCheckBoxMenuItem jmiAlwaysNo;
        private final JMenuItem jmiYieldToStack;
        private final JMenuItem jmiYieldToEntireStack;
        private StackItemView item;

        private String yieldKey = "";
        private boolean abilityScope;

        public AbilityMenu(){
            jmiAutoYield = new JCheckBoxMenuItem(Localizer.getInstance().getMessage("cbpAutoYieldMode"));
            jmiAutoYield.addActionListener(arg0 -> {
                final boolean autoYield = controller.getMatchUI().getGameController().shouldAutoYield(yieldKey);
                controller.getMatchUI().getGameController().setShouldAutoYield(yieldKey, !autoYield, abilityScope);
                if (!autoYield && controller.getMatchUI().getGameView().peekStack() == item) {
                    //auto-pass priority if ability is on top of stack
                    controller.getMatchUI().getGameController().passPriority();
                }
            });
            add(jmiAutoYield);

            jmiAlwaysYes = new JCheckBoxMenuItem(Localizer.getInstance().getMessage("lblAlwaysYes"));
            jmiAlwaysYes.addActionListener(arg0 -> {
                if (yieldKey.isEmpty()) return;
                IGameController gc = controller.getMatchUI().getGameController();
                TriggerDecision next = gc.getTriggerDecision(yieldKey) == TriggerDecision.ACCEPT ? TriggerDecision.ASK : TriggerDecision.ACCEPT;
                gc.setTriggerDecision(yieldKey, next, abilityScope);
            });
            add(jmiAlwaysYes);

            jmiAlwaysNo = new JCheckBoxMenuItem(Localizer.getInstance().getMessage("lblAlwaysNo"));
            jmiAlwaysNo.addActionListener(arg0 -> {
                if (yieldKey.isEmpty()) return;
                IGameController gc = controller.getMatchUI().getGameController();
                TriggerDecision next = gc.getTriggerDecision(yieldKey) == TriggerDecision.DECLINE ? TriggerDecision.ASK : TriggerDecision.DECLINE;
                gc.setTriggerDecision(yieldKey, next, abilityScope);
            });
            add(jmiAlwaysNo);

            jmiYieldToStack = new JMenuItem(Localizer.getInstance().getMessage("lblYieldToStack"));
            jmiYieldToStack.addActionListener(arg0 -> {
                final PlayerView local = controller.getMatchUI().getCurrentPlayer();
                if (local == null) return;
                controller.getMatchUI().getGameController().sendYieldUpdate(new YieldUpdate.StackYield(local, true, true));
                controller.getMatchUI().getGameController().passPriority();
            });
            add(jmiYieldToStack);

            jmiYieldToEntireStack = new JMenuItem(Localizer.getInstance().getMessage("lblYieldToEntireStack"));
            jmiYieldToEntireStack.addActionListener(arg0 -> {
                final PlayerView local = controller.getMatchUI().getCurrentPlayer();
                if (local == null) return;
                controller.getMatchUI().getGameController().sendYieldUpdate(new YieldUpdate.StackYield(local, true, false));
                controller.getMatchUI().getGameController().passPriority();
            });
            add(jmiYieldToEntireStack);
        }

        public void setStackInstance(final StackItemView item0) {
            item = item0;
            yieldKey = item.getKey();
            abilityScope = controller.getMatchUI().getGameController().getYieldController().isAbilityScope();

            jmiAutoYield.setVisible(item.isAbility());
            jmiAutoYield.setSelected(item.isAbility()
                    && controller.getMatchUI().getGameController().shouldAutoYield(yieldKey));

            if (item.isOptionalTrigger() && controller.getMatchUI().isLocalPlayer(item.getActivatingPlayer()) && !yieldKey.isEmpty()) {
                TriggerDecision decision = controller.getMatchUI().getGameController().getTriggerDecision(yieldKey);
                jmiAlwaysYes.setSelected(decision == TriggerDecision.ACCEPT);
                jmiAlwaysNo.setSelected(decision == TriggerDecision.DECLINE);
                jmiAlwaysYes.setVisible(true);
                jmiAlwaysNo.setVisible(true);
            } else {
                jmiAlwaysYes.setVisible(false);
                jmiAlwaysNo.setVisible(false);
            }
        }
    }
}
