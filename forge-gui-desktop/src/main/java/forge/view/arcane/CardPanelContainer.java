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
package forge.view.arcane;

import forge.FThreads;
import forge.game.card.Card;
import forge.screens.match.CMatchUI;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.special.CardZoomer;
import forge.view.arcane.util.CardPanelMouseListener;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages mouse events and common functionality for CardPanel containing
 * components.
 * 
 * @author Forge
 * @version $Id: CardPanelContainer.java 24793 2014-02-10 08:04:02Z Max mtg $
 */
public abstract class CardPanelContainer extends SkinnedPanel {
    /** Constant <code>serialVersionUID=-6400018234895548306L</code>. */
    private static final long serialVersionUID = -6400018234895548306L;

    /** Constant <code>DRAG_SMUDGE=10</code>. */
    private static final int DRAG_SMUDGE = 10;

    /**
     * 
     */
    private final List<CardPanel> cardPanels = new ArrayList<CardPanel>();
    /**
     * 
     */
    private FScrollPane scrollPane;
    /**
     * 
     */
    private int cardWidthMin = 50;

    private int cardWidthMax = 300;
    /**
     * 
     */
    private CardPanel hoveredPanel;
    private CardPanel mouseDownPanel;
    private CardPanel mouseDragPanel;

    private final List<CardPanelMouseListener> listeners = new ArrayList<CardPanelMouseListener>(2);
    private int mouseDragOffsetX, mouseDragOffsetY;
    private int intialMouseDragX = -1, intialMouseDragY;
    private boolean dragEnabled;

    /**
     * <p>
     * Constructor for CardPanelContainer.
     * </p>
     * 
     * @param scrollPane
     */
    public CardPanelContainer(final FScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        this.setOpaque(true);
        setupMouseListeners();
    }

    private void setupMouseListeners() {
        final MouseMotionListener mml = setupMotionMouseListener();
        setupMouseListener(mml);
        setupMouseWheelListener();
    }

    private void setupMouseWheelListener() {
        this.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                final CardPanel hitPanel = CardPanelContainer.this.getCardPanel(e.getX(), e.getY());
                if (hitPanel != null) {
                    if (e.getWheelRotation() < 0) {
                        CardZoomer.SINGLETON_INSTANCE.doMouseWheelZoom(hitPanel.getCard());
                    }
                }
            }
        });
    }

    private void setupMouseListener(final MouseMotionListener mml) {
        this.addMouseListener(new MouseAdapter() {
            private final boolean[] buttonsDown = new boolean[4];

            @Override
            public void mousePressed(final MouseEvent evt) {
                final int button = evt.getButton();
                if (button < 1 || button > 3) {
                    return;
                }
                this.buttonsDown[button] = true;
                CardPanelContainer.this.mouseDownPanel = CardPanelContainer.this.getCardPanel(evt.getX(), evt.getY());

                if (CardPanelContainer.this.mouseDownPanel != null && CardPanelContainer.this.getMouseDragPanel() == null &&
                        (this.buttonsDown[2] || (this.buttonsDown[1] && this.buttonsDown[3]))) {
                    //zoom card when middle mouse button down or both left and right mouse buttons down
                    CardZoomer.SINGLETON_INSTANCE.doMouseButtonZoom(CardPanelContainer.this.mouseDownPanel.getCard());
                }
            }

            @Override
            public void mouseReleased(final MouseEvent evt) {
                final int button = evt.getButton();
                if (button < 1 || button > 3) {
                    return;
                }

                boolean zoomed = CardZoomer.SINGLETON_INSTANCE.isZoomerOpen();
                if (!zoomed && CardPanelContainer.this.dragEnabled) {
                    CardPanelContainer.this.intialMouseDragX = -1;
                    if (CardPanelContainer.this.getMouseDragPanel() != null) {
                        final CardPanel panel = CardPanelContainer.this.getMouseDragPanel();
                        CardPanelContainer.this.setMouseDragPanel(null);
                        CardPanelContainer.this.mouseDragEnd(panel, evt);
                    }
                }

                if (!this.buttonsDown[button]) {
                    return;
                }
                this.buttonsDown[button] = false;

                if (zoomed) {
                    if (!this.buttonsDown[1] && !this.buttonsDown[2] && !this.buttonsDown[3]) {
                        //don't stop zooming until all mouse buttons released
                        CardZoomer.SINGLETON_INSTANCE.closeZoomer();
                    }
                    return; //don't raise click events if zoom was open
                }

                final CardPanel panel = CardPanelContainer.this.getCardPanel(evt.getX(), evt.getY());
                if (panel != null && CardPanelContainer.this.mouseDownPanel == panel) {
                    if (SwingUtilities.isLeftMouseButton(evt)) {
                        CardPanelContainer.this.mouseLeftClicked(panel, evt);
                    } else if (SwingUtilities.isRightMouseButton(evt)) {
                        CardPanelContainer.this.mouseRightClicked(panel, evt);
                    }
                } else {
                    // reeval cursor hover
                    mml.mouseMoved(evt);
                }
            }

            @Override
            public void mouseExited(final MouseEvent evt) {
                CardPanelContainer.this.mouseOutPanel(evt);
            }
        });
    }

    private MouseMotionListener setupMotionMouseListener() {
        final MouseMotionListener mml = new MouseMotionListener() {
            @Override
            public void mouseDragged(final MouseEvent evt) {
                if (CardZoomer.SINGLETON_INSTANCE.isZoomerOpen() || !SwingUtilities.isLeftMouseButton(evt)) {
                    return; //don't support dragging while zoomed or with mouse button besides left
                }
                if (!CardPanelContainer.this.dragEnabled) {
                    CardPanelContainer.this.mouseOutPanel(evt);
                    return;
                }
                if (CardPanelContainer.this.getMouseDragPanel() != null) {
                    CardPanelContainer.this.mouseDragged(CardPanelContainer.this.getMouseDragPanel(),
                            CardPanelContainer.this.mouseDragOffsetX, CardPanelContainer.this.mouseDragOffsetY, evt);
                    return;
                }
                final int x = evt.getX();
                final int y = evt.getY();
                final CardPanel panel = CardPanelContainer.this.getCardPanel(x, y);
                if (panel == null) {
                    return;
                }
                if (panel != CardPanelContainer.this.mouseDownPanel) {
                    return;
                }
                if (CardPanelContainer.this.intialMouseDragX == -1) {
                    CardPanelContainer.this.intialMouseDragX = x;
                    CardPanelContainer.this.intialMouseDragY = y;
                    return;
                }
                if ((Math.abs(x - CardPanelContainer.this.intialMouseDragX) < CardPanelContainer.DRAG_SMUDGE)
                        && (Math.abs(y - CardPanelContainer.this.intialMouseDragY) < CardPanelContainer.DRAG_SMUDGE)) {
                    return;
                }
                CardPanelContainer.this.mouseDownPanel = null;
                CardPanelContainer.this.setMouseDragPanel(panel);
                CardPanelContainer.this.mouseDragOffsetX = panel.getX() - CardPanelContainer.this.intialMouseDragX;
                CardPanelContainer.this.mouseDragOffsetY = panel.getY() - CardPanelContainer.this.intialMouseDragY;
                CardPanelContainer.this.mouseDragStart(CardPanelContainer.this.getMouseDragPanel(), evt);
            }

            @Override
            public void mouseMoved(final MouseEvent evt) {
                final CardPanel hitPanel = CardPanelContainer.this.getCardPanel(evt.getX(), evt.getY());

                if (CardPanelContainer.this.hoveredPanel == hitPanel) { // no big change
                    return;
                }

                if (CardPanelContainer.this.hoveredPanel != null) {
                    CardPanelContainer.this.mouseOutPanel(evt); // hovered <= null is inside
                }

                if (hitPanel != null) {
                    CMatchUI.SINGLETON_INSTANCE.setCard(hitPanel.getCard());

                    CardPanelContainer.this.hoveredPanel = hitPanel;
                    CardPanelContainer.this.hoveredPanel.setSelected(true);
                    CardPanelContainer.this.mouseOver(hitPanel, evt);
                }

                // System.err.format("%d %d over %s%n", evt.getX(), evt.getY(), hitPanel == null ? null : hitPanel.getCard().getName());
            }
        };
        this.addMouseMotionListener(mml);
        return mml;

    }

    /**
     * <p>
     * mouseOutPanel.
     * </p>
     * 
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    private void mouseOutPanel(final MouseEvent evt) {
        if (this.hoveredPanel == null) {
            return;
        }
        this.hoveredPanel.setSelected(false);
        this.mouseOut(this.hoveredPanel, evt);
        this.hoveredPanel = null;
    }

    /*
     * public void resetDrag(){ mouseDragPanel = null; invalidate(); };
     */
    /**
     * <p>
     * getCardPanel.
     * </p>
     * 
     * @param x
     *            a int.
     * @param y
     *            a int.
     * @return a {@link forge.view.arcane.CardPanel} object.
     */
    protected abstract CardPanel getCardPanel(int x, int y);

    /**
     * <p>
     * getCardPanel.
     * </p>
     * 
     * @param gameCardID
     *            a int.
     * @return a {@link forge.view.arcane.CardPanel} object.
     */
    public final CardPanel getCardPanel(final int gameCardID) {
        for (final CardPanel panel : this.getCardPanels()) {
            if (panel.getCard().getUniqueNumber() == gameCardID) {
                return panel;
            }
        }
        return null;
    }

    /**
     * <p>
     * removeCardPanel.
     * </p>
     * 
     * @param fromPanel
     *            a {@link forge.view.arcane.CardPanel} object.
     */
    public final void removeCardPanel(final CardPanel fromPanel) {
        FThreads.assertExecutedByEdt(true);
        if (CardPanelContainer.this.getMouseDragPanel() != null) {
            CardPanel.getDragAnimationPanel().setVisible(false);
            CardPanel.getDragAnimationPanel().repaint();
            CardPanelContainer.this.getCardPanels().remove(CardPanel.getDragAnimationPanel());
            CardPanelContainer.this.remove(CardPanel.getDragAnimationPanel());
            CardPanelContainer.this.setMouseDragPanel(null);
        }
        CardPanelContainer.this.hoveredPanel = null;
        fromPanel.dispose();
        CardPanelContainer.this.getCardPanels().remove(fromPanel);
        CardPanelContainer.this.remove(fromPanel);
        CardPanelContainer.this.invalidate();
        CardPanelContainer.this.repaint();
    }

    /**
     * <p>
     * setCardPanels.
     * </p>
     * 
     * @param cardPanels
     */
    public final void setCardPanels(List<CardPanel> cardPanels) {
        if (cardPanels.size() == 0) {
            clear();
            return;
        }

        for (CardPanel p : this.getCardPanels()) {
            if (!cardPanels.contains(p)) { //dispose of any card panels that have been removed
                p.dispose();
            }
        }
        this.getCardPanels().clear();
        this.removeAll();
        this.getCardPanels().addAll(cardPanels);
        for (CardPanel cardPanel : cardPanels) {
            this.add(cardPanel);
        }
        this.doLayout();
        this.invalidate();
        this.getParent().validate();
        this.repaint();
    }

    /**
     * <p>
     * clear.
     * </p>
     */
    public final void clear() {
        FThreads.assertExecutedByEdt(true);
        for (CardPanel p : CardPanelContainer.this.getCardPanels()) {
            p.dispose();
        }
        CardPanelContainer.this.getCardPanels().clear();
        CardPanelContainer.this.removeAll();
        CardPanelContainer.this.setPreferredSize(new Dimension(0, 0));
        CardPanelContainer.this.invalidate();
        CardPanelContainer.this.getParent().validate();
        CardPanelContainer.this.repaint();
    }

    /**
     * <p>
     * Getter for the field <code>scrollPane</code>.
     * </p>
     * 
     * @return a {@link forge.toolbox.FScrollPane} object.
     */
    public final FScrollPane getScrollPane() {
        return this.scrollPane;
    }

    /**
     * <p>
     * Getter for the field <code>cardWidthMin</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getCardWidthMin() {
        return this.cardWidthMin;
    }

    /**
     * <p>
     * Setter for the field <code>cardWidthMin</code>.
     * </p>
     * 
     * @param cardWidthMin
     *            a int.
     */
    public final void setCardWidthMin(final int cardWidthMin) {
        this.cardWidthMin = cardWidthMin;
    }

    /**
     * <p>
     * Getter for the field <code>cardWidthMax</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getCardWidthMax() {
        return this.cardWidthMax;
    }

    /**
     * <p>
     * Setter for the field <code>cardWidthMax</code>.
     * </p>
     * 
     * @param cardWidthMax
     *            a int.
     */
    public final void setCardWidthMax(final int cardWidthMax) {
        this.cardWidthMax = cardWidthMax;
    }

    /**
     * <p>
     * isDragEnabled.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isDragEnabled() {
        return this.dragEnabled;
    }

    /**
     * <p>
     * Setter for the field <code>dragEnabled</code>.
     * </p>
     * 
     * @param dragEnabled
     *            a boolean.
     */
    public final void setDragEnabled(final boolean dragEnabled) {
        this.dragEnabled = dragEnabled;
    }

    /**
     * <p>
     * addCardPanelMouseListener.
     * </p>
     * 
     * @param listener
     *            a {@link forge.view.arcane.util.CardPanelMouseListener} object.
     */
    public final void addCardPanelMouseListener(final CardPanelMouseListener listener) {
        this.listeners.add(listener);
    }

    /**
     * <p>
     * mouseLeftClicked.
     * </p>
     * 
     * @param panel
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseLeftClicked(panel, evt);
        }
    }

    /**
     * <p>
     * mouseRightClicked.
     * </p>
     * 
     * @param panel
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseRightClicked(panel, evt);
        }
    }

    /**
     * <p>
     * mouseDragEnd.
     * </p>
     * 
     * @param dragPanel
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseDragEnd(dragPanel, evt);
        }
    }

    /**
     * <p>
     * mouseDragged.
     * </p>
     * 
     * @param dragPanel
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param dragOffsetX
     *            a int.
     * @param dragOffsetY
     *            a int.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseDragged(final CardPanel dragPanel, final int dragOffsetX, final int dragOffsetY,
            final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseDragged(this.getMouseDragPanel(), this.mouseDragOffsetX, this.mouseDragOffsetY, evt);
        }
    }

    /**
     * <p>
     * mouseDragStart.
     * </p>
     * 
     * @param dragPanel
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseDragStart(final CardPanel dragPanel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseDragStart(this.getMouseDragPanel(), evt);
        }
    }

    /**
     * <p>
     * mouseOut.
     * </p>
     * 
     * @param panel
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public final void mouseOut(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseOut(this.hoveredPanel, evt);
        }
    }

    /**
     * <p>
     * mouseOver.
     * </p>
     * 
     * @param panel
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseOver(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseOver(panel, evt);
        }
    }

    /**
     * <p>
     * getCardFromMouseOverPanel.
     * </p>
     * 
     * @return a {@link forge.game.card.Card} object.
     */
    public final Card getHoveredCard(MouseEvent e) {
        // re-evaluate cursor position so if we hovered over a card, alt-tabbed out of the application, then
        // clicked back on the application somewhere else, the last hovered card won't register the click
        // this cannot protect against alt tabbing off then re-focusing on the application by clicking on
        // the already-hovered card, though, since we cannot tell the difference between that and clicking
        // on the hovered card when the app already has focus.
        CardPanel p = getCardPanel(e.getX(), e.getY());

        // if cursor has jumped, for example via the above alt-tabbing example, fix the card hover highlight
        if (null != hoveredPanel && p != hoveredPanel) {
            mouseOut(hoveredPanel, e);
        }

        return (null == p || p != hoveredPanel) ? null : p.getCard();
    }


    /**
     * Gets the card panels.
     * 
     * @return the cardPanels
     */
    public final List<CardPanel> getCardPanels() {
        return this.cardPanels;
    }

    /**
     * Gets the mouse drag panel.
     * 
     * @return the mouseDragPanel
     */
    public CardPanel getMouseDragPanel() {
        return this.mouseDragPanel;
    }

    /**
     * Sets the mouse drag panel.
     * 
     * @param mouseDragPanel0
     *            the mouseDragPanel to set
     */
    public void setMouseDragPanel(final CardPanel mouseDragPanel0) {
        this.mouseDragPanel = mouseDragPanel0;
    }
}
