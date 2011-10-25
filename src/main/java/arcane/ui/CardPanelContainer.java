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
    public List<CardPanel> cardPanels = new ArrayList<CardPanel>();
    /**
     * 
     */
    protected JScrollPane scrollPane;
    /**
     * 
     */
    protected int cardWidthMin = 50, cardWidthMax = Constant.Runtime.width[0];
    /**
     * 
     */
    protected CardPanel mouseOverPanel, mouseDownPanel, mouseDragPanel;

    private List<CardPanelMouseListener> listeners = new ArrayList<CardPanelMouseListener>(2);
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

        setOpaque(true);

        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(final MouseEvent evt) {
                if (!dragEnabled) {
                    mouseOutPanel(evt);
                    return;
                }
                if (mouseDragPanel != null) {
                    CardPanelContainer.this.mouseDragged(mouseDragPanel, mouseDragOffsetX, mouseDragOffsetY, evt);
                    return;
                }
                int x = evt.getX();
                int y = evt.getY();
                CardPanel panel = getCardPanel(x, y);
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
                if (Math.abs(x - intialMouseDragX) < DRAG_SMUDGE && Math.abs(y - intialMouseDragY) < DRAG_SMUDGE) {
                    return;
                }
                mouseDownPanel = null;
                mouseDragPanel = panel;
                mouseDragOffsetX = panel.getX() - intialMouseDragX;
                mouseDragOffsetY = panel.getY() - intialMouseDragY;
                CardPanelContainer.this.mouseDragStart(mouseDragPanel, evt);
            }

            public void mouseMoved(final MouseEvent evt) {
                CardPanel panel = getCardPanel(evt.getX(), evt.getY());
                if (mouseOverPanel != null && mouseOverPanel != panel) {
                    CardPanelContainer.this.mouseOutPanel(evt);
                }
                if (panel == null) {
                    return;
                }
                mouseOverPanel = panel;
                mouseOverPanel.setSelected(true);
                CardPanelContainer.this.mouseOver(panel, evt);
            }
        });

        addMouseListener(new MouseAdapter() {
            private boolean[] buttonsDown = new boolean[4];

            public void mousePressed(final MouseEvent evt) {
                int button = evt.getButton();
                if (button < 1 || button > 3) {
                    return;
                }
                buttonsDown[button] = true;
                mouseDownPanel = getCardPanel(evt.getX(), evt.getY());
            }

            public void mouseReleased(final MouseEvent evt) {
                int button = evt.getButton();
                if (button < 1 || button > 3) {
                    return;
                }

                if (dragEnabled) {
                    intialMouseDragX = -1;
                    if (mouseDragPanel != null) {
                        CardPanel panel = mouseDragPanel;
                        mouseDragPanel = null;
                        CardPanelContainer.this.mouseDragEnd(panel, evt);
                    }
                }

                if (!buttonsDown[button]) {
                    return;
                }
                buttonsDown[button] = false;

                CardPanel panel = getCardPanel(evt.getX(), evt.getY());
                if (panel != null && mouseDownPanel == panel) {
                    int downCount = 0;
                    for (int i = 1; i < buttonsDown.length; i++) {
                        if (buttonsDown[i]) {
                            buttonsDown[i] = false;
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

            public void mouseExited(final MouseEvent evt) {
                mouseOutPanel(evt);
            }

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
        if (mouseOverPanel == null) {
            return;
        }
        mouseOverPanel.setSelected(false);
        mouseOut(mouseOverPanel, evt);
        mouseOverPanel = null;
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
        cardPanels.add(placeholder);
        add(placeholder);
        doLayout();
        // int y = Math.min(placeholder.getHeight(),
        // scrollPane.getVisibleRect().height);
        scrollRectToVisible(new Rectangle(placeholder.getCardX(), placeholder.getCardY(), placeholder.getCardWidth(),
                placeholder.getCardHeight()));
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
        for (CardPanel panel : cardPanels) {
            if (panel.gameCard.getUniqueNumber() == gameCardID) {
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
            public void run() {
                if (mouseDragPanel != null) {
                    CardPanel.dragAnimationPanel.setVisible(false);
                    CardPanel.dragAnimationPanel.repaint();
                    cardPanels.remove(CardPanel.dragAnimationPanel);
                    remove(CardPanel.dragAnimationPanel);
                    mouseDragPanel = null;
                }
                mouseOverPanel = null;
                cardPanels.remove(fromPanel);
                remove(fromPanel);
                invalidate();
                repaint();
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
            public void run() {
                cardPanels.clear();
                removeAll();
                setPreferredSize(new Dimension(0, 0));
                invalidate();
                getParent().validate();
                repaint();
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
        return scrollPane;
    }

    /**
     * <p>
     * Getter for the field <code>cardWidthMin</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getCardWidthMin() {
        return cardWidthMin;
    }

    /**
     * <p>
     * Setter for the field <code>cardWidthMin</code>.
     * </p>
     * 
     * @param cardWidthMin
     *            a int.
     */
    public final void setCardWidthMin(int cardWidthMin) {
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
        return cardWidthMax;
    }

    /**
     * <p>
     * Setter for the field <code>cardWidthMax</code>.
     * </p>
     * 
     * @param cardWidthMax
     *            a int.
     */
    public final void setCardWidthMax(int cardWidthMax) {
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
        return dragEnabled;
    }

    /**
     * <p>
     * Setter for the field <code>dragEnabled</code>.
     * </p>
     * 
     * @param dragEnabled
     *            a boolean.
     */
    public final void setDragEnabled(boolean dragEnabled) {
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
        listeners.add(listener);
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
        for (CardPanelMouseListener listener : listeners) {
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
        for (CardPanelMouseListener listener : listeners) {
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
        for (CardPanelMouseListener listener : listeners) {
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
        for (CardPanelMouseListener listener : listeners) {
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
        for (CardPanelMouseListener listener : listeners) {
            listener.mouseDragged(mouseDragPanel, mouseDragOffsetX, mouseDragOffsetY, evt);
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
        for (CardPanelMouseListener listener : listeners) {
            listener.mouseDragStart(mouseDragPanel, evt);
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
        for (CardPanelMouseListener listener : listeners) {
            listener.mouseOut(mouseOverPanel, evt);
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
        for (CardPanelMouseListener listener : listeners) {
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
        if (mouseOverPanel != null) {
            return mouseOverPanel.gameCard;
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
        return zoneID;
    }

    /**
     * <p>
     * Setter for the field <code>zoneID</code>.
     * </p>
     * 
     * @param zoneID
     *            a int.
     */
    public void setZoneID(final int zoneID) {
        this.zoneID = zoneID;
    }
}
