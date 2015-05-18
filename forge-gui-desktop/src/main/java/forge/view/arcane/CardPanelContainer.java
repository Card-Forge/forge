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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import forge.FThreads;
import forge.game.card.CardView;
import forge.screens.match.CMatchUI;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.special.CardZoomer;
import forge.view.arcane.util.CardPanelMouseListener;

/**
 * Manages mouse events and common functionality for CardPanel containing
 * components.
 *
 * @author Forge
 * @version $Id: CardPanelContainer.java 24793 2014-02-10 08:04:02Z Max mtg $
 */
public abstract class CardPanelContainer extends SkinnedPanel {
    private static final long serialVersionUID = -6400018234895548306L;
    private static final int DRAG_SMUDGE = 10;

    private final List<CardPanel> cardPanels = new ArrayList<CardPanel>();
    private final CMatchUI matchUI;
    private final FScrollPane scrollPane;

    private int cardWidthMin = 50;
    private int cardWidthMax = 300;

    private CardPanel hoveredPanel;
    private CardPanel mouseDownPanel;
    private CardPanel mouseDragPanel;

    private final List<CardPanelMouseListener> listeners = new ArrayList<CardPanelMouseListener>(2);
    private int mouseDragOffsetX, mouseDragOffsetY;
    private int intialMouseDragX = -1, intialMouseDragY;
    private boolean dragEnabled;

    public CardPanelContainer(final CMatchUI matchUI, final FScrollPane scrollPane) {
        this.matchUI = matchUI;
        this.scrollPane = scrollPane;
        this.setOpaque(true);
        setupMouseListeners();
    }

    protected final CMatchUI getMatchUI() {
        return matchUI;
    }

    private void mouseWheelZoom(final CardView card) {
        if (canZoom(card)) {
            CardZoomer.SINGLETON_INSTANCE.setCard(card.getCurrentState(), false);
            CardZoomer.SINGLETON_INSTANCE.doMouseWheelZoom();
        }
    }
    private void mouseButtonZoom(final CardView card) {
        if (canZoom(card)) {
            CardZoomer.SINGLETON_INSTANCE.setCard(card.getCurrentState(), false);
            CardZoomer.SINGLETON_INSTANCE.doMouseButtonZoom();
        }
    }
    private boolean canZoom(final CardView card) {
        return getMatchUI().mayView(card);
    }

    private void setupMouseListeners() {
        final MouseMotionListener mml = setupMotionMouseListener();
        setupMouseListener(mml);
        setupMouseWheelListener();
    }

    private void setupMouseWheelListener() {
        this.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(final MouseWheelEvent e) {
                final CardPanel hitPanel = getCardPanel(e.getX(), e.getY());
                if (hitPanel != null && e.getWheelRotation() < 0) {
                    mouseWheelZoom(hitPanel.getCard());
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
                mouseDownPanel = getCardPanel(evt.getX(), evt.getY());

                if (mouseDownPanel != null && getMouseDragPanel() == null &&
                        (this.buttonsDown[2] || (this.buttonsDown[1] && this.buttonsDown[3]))) {
                    //zoom card when middle mouse button down or both left and right mouse buttons down
                    mouseButtonZoom(mouseDownPanel.getCard());
                }
            }

            @Override
            public void mouseReleased(final MouseEvent evt) {
                final int button = evt.getButton();
                if (button < 1 || button > 3) {
                    return;
                }

                final boolean zoomed = CardZoomer.SINGLETON_INSTANCE.isZoomerOpen();
                if (!zoomed && dragEnabled) {
                    intialMouseDragX = -1;
                    if (getMouseDragPanel() != null) {
                        final CardPanel panel = getMouseDragPanel();
                        setMouseDragPanel(null);
                        mouseDragEnd(panel, evt);
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

                final CardPanel panel = getCardPanel(evt.getX(), evt.getY());
                if (panel != null && mouseDownPanel == panel) {
                    if (SwingUtilities.isLeftMouseButton(evt)) {
                        mouseLeftClicked(panel, evt);
                    } else if (SwingUtilities.isRightMouseButton(evt)) {
                        mouseRightClicked(panel, evt);
                    }
                } else {
                    // reeval cursor hover
                    mml.mouseMoved(evt);
                }
            }

            @Override
            public void mouseExited(final MouseEvent evt) {
                mouseOutPanel(evt);
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
                if (!dragEnabled) {
                    mouseOutPanel(evt);
                    return;
                }
                if (getMouseDragPanel() != null) {
                    CardPanelContainer.this.mouseDragged(getMouseDragPanel(),
                            mouseDragOffsetX, mouseDragOffsetY, evt);
                    return;
                }
                final int x = evt.getX();
                final int y = evt.getY();
                final CardPanel panel = getCardPanel(x, y);
                if (panel == null) {
                    return;
                }
                if (panel != mouseDownPanel) {
                    return;
                }
                if (intialMouseDragX == -1) {
                    intialMouseDragX = x;
                    intialMouseDragY = y;
                    return;
                }
                if ((Math.abs(x - intialMouseDragX) < CardPanelContainer.DRAG_SMUDGE)
                        && (Math.abs(y - intialMouseDragY) < CardPanelContainer.DRAG_SMUDGE)) {
                    return;
                }
                mouseDownPanel = null;
                setMouseDragPanel(panel);
                mouseDragOffsetX = panel.getX() - intialMouseDragX;
                mouseDragOffsetY = panel.getY() - intialMouseDragY;
                mouseDragStart(getMouseDragPanel(), evt);
            }

            @Override
            public void mouseMoved(final MouseEvent evt) {
                final CardPanel hitPanel = getCardPanel(evt.getX(), evt.getY());

                if (hoveredPanel == hitPanel) { // no big change
                    return;
                }

                if (hoveredPanel != null) {
                    mouseOutPanel(evt); // hovered <= null is inside
                }

                if (hitPanel != null) {
                    matchUI.setCard(hitPanel.getCard());

                    hoveredPanel = hitPanel;
                    hoveredPanel.setSelected(true);
                    mouseOver(hitPanel, evt);
                }

                // System.err.format("%d %d over %s%n", evt.getX(), evt.getY(), hitPanel == null ? null : hitPanel.getCard().getName());
            }
        };
        this.addMouseMotionListener(mml);
        return mml;
    }

    private void mouseOutPanel(final MouseEvent evt) {
        if (this.hoveredPanel == null) {
            return;
        }
        this.hoveredPanel.setSelected(false);
        this.mouseOut(this.hoveredPanel, evt);
        this.hoveredPanel = null;
    }

    protected abstract CardPanel getCardPanel(int x, int y);

    public CardPanel addCard(final CardView card) {
        final CardPanel placeholder = new CardPanel(matchUI, card);
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

    public final CardPanel getCardPanel(final int gameCardID) {
        for (final CardPanel panel : this.getCardPanels()) {
            if (panel.getCard().getId() == gameCardID) {
                return panel;
            }
        }
        return null;
    }

    public final void removeCardPanel(final CardPanel fromPanel) {
        FThreads.assertExecutedByEdt(true);
        if (getMouseDragPanel() != null) {
            CardPanel.getDragAnimationPanel().setVisible(false);
            CardPanel.getDragAnimationPanel().repaint();
            getCardPanels().remove(CardPanel.getDragAnimationPanel());
            remove(CardPanel.getDragAnimationPanel());
            setMouseDragPanel(null);
        }
        hoveredPanel = null;
        fromPanel.dispose();
        getCardPanels().remove(fromPanel);
        remove(fromPanel);
        invalidate();
        repaint();
    }

    public final void setCardPanels(final List<CardPanel> cardPanels) {
        if (cardPanels.size() == 0) {
            clear();
            return;
        }

        for (final CardPanel p : this.getCardPanels()) {
            if (!cardPanels.contains(p)) { //dispose of any card panels that have been removed
                p.dispose();
            }
        }
        this.getCardPanels().clear();
        this.removeAll();
        this.getCardPanels().addAll(cardPanels);
        for (final CardPanel cardPanel : cardPanels) {
            this.add(cardPanel);
        }
        this.doLayout();
        this.invalidate();
        this.getParent().validate();
        this.repaint();
    }

    public final void clear() {
        FThreads.assertExecutedByEdt(true);
        for (final CardPanel p : getCardPanels()) {
            p.dispose();
        }
        getCardPanels().clear();
        removeAll();
        setPreferredSize(new Dimension(0, 0));
        invalidate();
        getParent().validate();
        repaint();
    }

    public final FScrollPane getScrollPane() {
        return this.scrollPane;
    }

    public final int getCardWidthMin() {
        return this.cardWidthMin;
    }

    public final void setCardWidthMin(final int cardWidthMin) {
        this.cardWidthMin = cardWidthMin;
    }

    public final int getCardWidthMax() {
        return this.cardWidthMax;
    }

    public final void setCardWidthMax(final int cardWidthMax) {
        this.cardWidthMax = cardWidthMax;
    }

    public final boolean isDragEnabled() {
        return this.dragEnabled;
    }

    public final void setDragEnabled(final boolean dragEnabled) {
        this.dragEnabled = dragEnabled;
    }

    public final void addCardPanelMouseListener(final CardPanelMouseListener listener) {
        this.listeners.add(listener);
    }

    public void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseLeftClicked(panel, evt);
        }
    }

    public void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseRightClicked(panel, evt);
        }
    }

    public void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseDragEnd(dragPanel, evt);
        }
    }

    public void mouseDragged(final CardPanel dragPanel, final int dragOffsetX, final int dragOffsetY, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseDragged(this.getMouseDragPanel(), this.mouseDragOffsetX, this.mouseDragOffsetY, evt);
        }
    }

    public void mouseDragStart(final CardPanel dragPanel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseDragStart(this.getMouseDragPanel(), evt);
        }
    }

    public final void mouseOut(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseOut(this.hoveredPanel, evt);
        }
    }

    public void mouseOver(final CardPanel panel, final MouseEvent evt) {
        for (final CardPanelMouseListener listener : this.listeners) {
            listener.mouseOver(panel, evt);
        }
    }

    public final List<CardPanel> getCardPanels() {
        return this.cardPanels;
    }

    public CardPanel getMouseDragPanel() {
        return this.mouseDragPanel;
    }

    public void setMouseDragPanel(final CardPanel mouseDragPanel0) {
        this.mouseDragPanel = mouseDragPanel0;
    }
}
