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
package arcane.ui;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import arcane.ui.util.CardPanelMouseListener;
import arcane.ui.util.UI;
import forge.Card;
import forge.Constant;

/**
 * Manages mouse events and common functionality for CardPanel containing
 * components.
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class CardPanelContainer extends JPanel {
    /** Constant <code>serialVersionUID=-6400018234895548306L</code>. */
    private static final long serialVersionUID = -6400018234895548306L;

    /** Constant <code>DRAG_SMUDGE=10</code>. */
    private static final int DRAG_SMUDGE = 10;

    /**
     * 
     */
    private List<CardPanel> cardPanels = new ArrayList<CardPanel>();
    /**
     * 
     */
    private JScrollPane scrollPane;
    /**
     * 
     */
    private int cardWidthMin = 50;

    private int cardWidthMax = Constant.Runtime.WIDTH[0];
    /**
     * 
     */
    private CardPanel mouseOverPanel;
    private CardPanel mouseDownPanel;
    private CardPanel mouseDragPanel;

    private final List<CardPanelMouseListener> listeners = new ArrayList<CardPanelMouseListener>(2);
    private int mouseDragOffsetX, mouseDragOffsetY;
    private int intialMouseDragX = -1, intialMouseDragY;
    private boolean dragEnabled;
    private int zoneID;

    /**
     * <p>
     * Constructor for CardPanelContainer.
     * </p>
     * 
     * @param scrollPane
     *            a {@link javax.swing.JScrollPane} object.
     */
    public CardPanelContainer(final JScrollPane scrollPane) {
        this.scrollPane = scrollPane;

        this.setOpaque(true);

        this.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(final MouseEvent evt) {
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
                final CardPanel panel = CardPanelContainer.this.getCardPanel(evt.getX(), evt.getY());
                if ((CardPanelContainer.this.mouseOverPanel != null)
                        && (CardPanelContainer.this.mouseOverPanel != panel)) {
                    CardPanelContainer.this.mouseOutPanel(evt);
                }
                if (panel == null) {
                    return;
                }
                CardPanelContainer.this.mouseOverPanel = panel;
                CardPanelContainer.this.mouseOverPanel.setSelected(true);
                CardPanelContainer.this.mouseOver(panel, evt);
            }
        });

        this.addMouseListener(new MouseAdapter() {
            private final boolean[] buttonsDown = new boolean[4];

            @Override
            public void mousePressed(final MouseEvent evt) {
                final int button = evt.getButton();
                if ((button < 1) || (button > 3)) {
                    return;
                }
                this.buttonsDown[button] = true;
                CardPanelContainer.this.mouseDownPanel = CardPanelContainer.this.getCardPanel(evt.getX(), evt.getY());
            }

            @Override
            public void mouseReleased(final MouseEvent evt) {
                final int button = evt.getButton();
                if ((button < 1) || (button > 3)) {
                    return;
                }

                if (CardPanelContainer.this.dragEnabled) {
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

                final CardPanel panel = CardPanelContainer.this.getCardPanel(evt.getX(), evt.getY());
                if ((panel != null) && (CardPanelContainer.this.mouseDownPanel == panel)) {
                    int downCount = 0;
                    for (int i = 1; i < this.buttonsDown.length; i++) {
                        if (this.buttonsDown[i]) {
                            this.buttonsDown[i] = false;
                            downCount++;
                        }
                    }
                    if (downCount > 0) {
                        CardPanelContainer.this.mouseMiddleClicked(panel, evt);
                    } else if (SwingUtilities.isLeftMouseButton(evt)) {
                        CardPanelContainer.this.mouseLeftClicked(panel, evt);
                    } else if (SwingUtilities.isRightMouseButton(evt)) {
                        CardPanelContainer.this.mouseRightClicked(panel, evt);
                    } else if (SwingUtilities.isMiddleMouseButton(evt)) {
                        CardPanelContainer.this.mouseMiddleClicked(panel, evt);
                    }
                }
            }

            @Override
            public void mouseExited(final MouseEvent evt) {
                CardPanelContainer.this.mouseOutPanel(evt);
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
            }
        });
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
        if (this.mouseOverPanel == null) {
            return;
        }
        this.mouseOverPanel.setSelected(false);
        this.mouseOut(this.mouseOverPanel, evt);
        this.mouseOverPanel = null;
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
     * @return a {@link arcane.ui.CardPanel} object.
     */
    protected abstract CardPanel getCardPanel(int x, int y);

    /**
     * Must call from the Swing event thread.
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link arcane.ui.CardPanel} object.
     */
    public final CardPanel addCard(final Card card) {
        final CardPanel placeholder = new CardPanel(card);
        placeholder.setDisplayEnabled(false);
        this.getCardPanels().add(placeholder);
        this.add(placeholder);
        this.doLayout();
        // int y = Math.min(placeholder.getHeight(),
        // scrollPane.getVisibleRect().height);
        this.scrollRectToVisible(new Rectangle(placeholder.getCardX(), placeholder.getCardY(), placeholder
                .getCardWidth(), placeholder.getCardHeight()));
        return placeholder;
    }

    /**
     * <p>
     * getCardPanel.
     * </p>
     * 
     * @param gameCardID
     *            a int.
     * @return a {@link arcane.ui.CardPanel} object.
     */
    public final CardPanel getCardPanel(final int gameCardID) {
        for (final CardPanel panel : this.getCardPanels()) {
            if (panel.getGameCard().getUniqueNumber() == gameCardID) {
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
     *            a {@link arcane.ui.CardPanel} object.
     */
    public final void removeCardPanel(final CardPanel fromPanel) {
        UI.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (CardPanelContainer.this.getMouseDragPanel() != null) {
                    CardPanel.getDragAnimationPanel().setVisible(false);
                    CardPanel.getDragAnimationPanel().repaint();
                    CardPanelContainer.this.getCardPanels().remove(CardPanel.getDragAnimationPanel());
                    CardPanelContainer.this.remove(CardPanel.getDragAnimationPanel());
                    CardPanelContainer.this.setMouseDragPanel(null);
                }
                CardPanelContainer.this.mouseOverPanel = null;
                CardPanelContainer.this.getCardPanels().remove(fromPanel);
                CardPanelContainer.this.remove(fromPanel);
                CardPanelContainer.this.invalidate();
                CardPanelContainer.this.repaint();
            }
        });
    }

    /**
     * <p>
     * clear.
     * </p>
     */
    public final void clear() {
        UI.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                CardPanelContainer.this.getCardPanels().clear();
                CardPanelContainer.this.removeAll();
                CardPanelContainer.this.setPreferredSize(new Dimension(0, 0));
                CardPanelContainer.this.invalidate();
                CardPanelContainer.this.getParent().validate();
                CardPanelContainer.this.repaint();
            }
        });
    }

    /**
     * <p>
     * Getter for the field <code>scrollPane</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JScrollPane} object.
     */
    public final JScrollPane getScrollPane() {
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
     *            a {@link arcane.ui.util.CardPanelMouseListener} object.
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
     *            a {@link arcane.ui.CardPanel} object.
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
     *            a {@link arcane.ui.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseRightClicked(panel, evt);
        }
    }

    /**
     * <p>
     * mouseMiddleClicked.
     * </p>
     * 
     * @param panel
     *            a {@link arcane.ui.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public final void mouseMiddleClicked(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseMiddleClicked(panel, evt);
        }
    }

    /**
     * <p>
     * mouseDragEnd.
     * </p>
     * 
     * @param dragPanel
     *            a {@link arcane.ui.CardPanel} object.
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
     *            a {@link arcane.ui.CardPanel} object.
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
     *            a {@link arcane.ui.CardPanel} object.
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
     *            a {@link arcane.ui.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public final void mouseOut(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseOut(this.mouseOverPanel, evt);
        }
    }

    /**
     * <p>
     * mouseOver.
     * </p>
     * 
     * @param panel
     *            a {@link arcane.ui.CardPanel} object.
     * @param evt
     *            a {@link java.awt.event.MouseEvent} object.
     */
    public final void mouseOver(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseOver(panel, evt);
        }
    }

    /**
     * <p>
     * getCardFromMouseOverPanel.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getCardFromMouseOverPanel() {
        if (this.mouseOverPanel != null) {
            return this.mouseOverPanel.getGameCard();
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>zoneID</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getZoneID() {
        return this.zoneID;
    }

    /**
     * <p>
     * Setter for the field <code>zoneID</code>.
     * </p>
     * 
     * @param zoneID
     *            a int.
     */
    public final void setZoneID(final int zoneID) {
        this.zoneID = zoneID;
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
     * Sets the card panels.
     * 
     * @param cardPanels
     *            the cardPanels to set
     */
    public final void setCardPanels(final List<CardPanel> cardPanels) {
        this.cardPanels = cardPanels; // TODO: Add 0 to parameter's name.
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
     * @param mouseDragPanel
     *            the mouseDragPanel to set
     */
    public void setMouseDragPanel(final CardPanel mouseDragPanel) {
        this.mouseDragPanel = mouseDragPanel; // TODO: Add 0 to parameter's
                                              // name.
    }
}
