package arcane.ui;

import arcane.ui.util.CardPanelMouseListener;
import arcane.ui.util.UI;
import forge.Card;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages mouse events and common funcitonality for CardPanel containing components.
 *
 * @author Forge
 * @version $Id$
 */
abstract public class CardPanelContainer extends JPanel {
    /** Constant <code>serialVersionUID=-6400018234895548306L</code> */
    private static final long serialVersionUID = -6400018234895548306L;

    /** Constant <code>DRAG_SMUDGE=10</code> */
    private final static int DRAG_SMUDGE = 10;

    public List<CardPanel> cardPanels = new ArrayList<CardPanel>();
    protected JScrollPane scrollPane;
    protected int cardWidthMin = 50, cardWidthMax = 300;
    protected CardPanel mouseOverPanel, mouseDownPanel, mouseDragPanel;

    private List<CardPanelMouseListener> listeners = new ArrayList<CardPanelMouseListener>(2);
    private int mouseDragOffsetX, mouseDragOffsetY;
    private int intialMouseDragX = -1, intialMouseDragY;
    private boolean dragEnabled;
    private int zoneID;

    /**
     * <p>Constructor for CardPanelContainer.</p>
     *
     * @param scrollPane a {@link javax.swing.JScrollPane} object.
     */
    public CardPanelContainer(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;

        setOpaque(true);

        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent evt) {
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
                if (panel == null) return;
                if (panel != mouseDownPanel) return;
                if (intialMouseDragX == -1) {
                    intialMouseDragX = x;
                    intialMouseDragY = y;
                    return;
                }
                if (Math.abs(x - intialMouseDragX) < DRAG_SMUDGE && Math.abs(y - intialMouseDragY) < DRAG_SMUDGE)
                    return;
                mouseDownPanel = null;
                mouseDragPanel = panel;
                mouseDragOffsetX = panel.getX() - intialMouseDragX;
                mouseDragOffsetY = panel.getY() - intialMouseDragY;
                CardPanelContainer.this.mouseDragStart(mouseDragPanel, evt);
            }

            public void mouseMoved(MouseEvent evt) {
                CardPanel panel = getCardPanel(evt.getX(), evt.getY());
                if (mouseOverPanel != null && mouseOverPanel != panel) CardPanelContainer.this.mouseOutPanel(evt);
                if (panel == null) return;
                mouseOverPanel = panel;
                mouseOverPanel.setSelected(true);
                CardPanelContainer.this.mouseOver(panel, evt);
            }
        });

        addMouseListener(new MouseAdapter() {
            private boolean[] buttonsDown = new boolean[4];

            public void mousePressed(MouseEvent evt) {
                int button = evt.getButton();
                if (button < 1 || button > 3) return;
                buttonsDown[button] = true;
                mouseDownPanel = getCardPanel(evt.getX(), evt.getY());
            }

            public void mouseReleased(MouseEvent evt) {
                int button = evt.getButton();
                if (button < 1 || button > 3) return;

                if (dragEnabled) {
                    intialMouseDragX = -1;
                    if (mouseDragPanel != null) {
                        CardPanel panel = mouseDragPanel;
                        mouseDragPanel = null;
                        CardPanelContainer.this.mouseDragEnd(panel, evt);
                    }
                }

                if (!buttonsDown[button]) return;
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

            public void mouseExited(MouseEvent evt) {
                mouseOutPanel(evt);
            }

            public void mouseEntered(MouseEvent e) {
            }
        });
    }

    /**
     * <p>mouseOutPanel.</p>
     *
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    private void mouseOutPanel(MouseEvent evt) {
        if (mouseOverPanel == null) return;
        mouseOverPanel.setSelected(false);
        mouseOut(mouseOverPanel, evt);
        mouseOverPanel = null;
    }

    /*public void resetDrag(){
         mouseDragPanel = null;
         invalidate();
     };*/
    /**
     * <p>getCardPanel.</p>
     *
     * @param x a int.
     * @param y a int.
     * @return a {@link arcane.ui.CardPanel} object.
     */
    abstract protected CardPanel getCardPanel(int x, int y);

    /**
     * Must call from the Swing event thread.
     *
     * @param card a {@link forge.Card} object.
     * @return a {@link arcane.ui.CardPanel} object.
     */
    public CardPanel addCard(Card card) {
        final CardPanel placeholder = new CardPanel(card);
        placeholder.setDisplayEnabled(false);
        cardPanels.add(placeholder);
        add(placeholder);
        doLayout();
        // int y = Math.min(placeholder.getHeight(), scrollPane.getVisibleRect().height);
        scrollRectToVisible(new Rectangle(placeholder.getCardX(), placeholder.getCardY(), placeholder.getCardWidth(), placeholder
                .getCardHeight()));
        return placeholder;
    }

    /**
     * <p>getCardPanel.</p>
     *
     * @param gameCardID a int.
     * @return a {@link arcane.ui.CardPanel} object.
     */
    public CardPanel getCardPanel(int gameCardID) {
        for (CardPanel panel : cardPanels)
            if (panel.gameCard.getUniqueNumber() == gameCardID) return panel;
        return null;
    }

    /**
     * <p>removeCardPanel.</p>
     *
     * @param fromPanel a {@link arcane.ui.CardPanel} object.
     */
    public void removeCardPanel(final CardPanel fromPanel) {
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
     * <p>clear.</p>
     */
    public void clear() {
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
     * <p>Getter for the field <code>scrollPane</code>.</p>
     *
     * @return a {@link javax.swing.JScrollPane} object.
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * <p>Getter for the field <code>cardWidthMin</code>.</p>
     *
     * @return a int.
     */
    public int getCardWidthMin() {
        return cardWidthMin;
    }

    /**
     * <p>Setter for the field <code>cardWidthMin</code>.</p>
     *
     * @param cardWidthMin a int.
     */
    public void setCardWidthMin(int cardWidthMin) {
        this.cardWidthMin = cardWidthMin;
    }

    /**
     * <p>Getter for the field <code>cardWidthMax</code>.</p>
     *
     * @return a int.
     */
    public int getCardWidthMax() {
        return cardWidthMax;
    }

    /**
     * <p>Setter for the field <code>cardWidthMax</code>.</p>
     *
     * @param cardWidthMax a int.
     */
    public void setCardWidthMax(int cardWidthMax) {
        this.cardWidthMax = cardWidthMax;
    }

    /**
     * <p>isDragEnabled.</p>
     *
     * @return a boolean.
     */
    public boolean isDragEnabled() {
        return dragEnabled;
    }

    /**
     * <p>Setter for the field <code>dragEnabled</code>.</p>
     *
     * @param dragEnabled a boolean.
     */
    public void setDragEnabled(boolean dragEnabled) {
        this.dragEnabled = dragEnabled;
    }

    /**
     * <p>addCardPanelMouseListener.</p>
     *
     * @param listener a {@link arcane.ui.util.CardPanelMouseListener} object.
     */
    public void addCardPanelMouseListener(CardPanelMouseListener listener) {
        listeners.add(listener);
    }

    /**
     * <p>mouseLeftClicked.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseLeftClicked(CardPanel panel, MouseEvent evt) {
        for (CardPanelMouseListener listener : listeners)
            listener.mouseLeftClicked(panel, evt);
    }

    /**
     * <p>mouseRightClicked.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseRightClicked(CardPanel panel, MouseEvent evt) {
        for (CardPanelMouseListener listener : listeners)
            listener.mouseRightClicked(panel, evt);
    }

    /**
     * <p>mouseMiddleClicked.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseMiddleClicked(CardPanel panel, MouseEvent evt) {
        for (CardPanelMouseListener listener : listeners)
            listener.mouseMiddleClicked(panel, evt);
    }

    /**
     * <p>mouseDragEnd.</p>
     *
     * @param dragPanel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseDragEnd(CardPanel dragPanel, MouseEvent evt) {
        for (CardPanelMouseListener listener : listeners)
            listener.mouseDragEnd(dragPanel, evt);
    }

    /**
     * <p>mouseDragged.</p>
     *
     * @param dragPanel a {@link arcane.ui.CardPanel} object.
     * @param dragOffsetX a int.
     * @param dragOffsetY a int.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseDragged(CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt) {
        for (CardPanelMouseListener listener : listeners)
            listener.mouseDragged(mouseDragPanel, mouseDragOffsetX, mouseDragOffsetY, evt);
    }

    /**
     * <p>mouseDragStart.</p>
     *
     * @param dragPanel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseDragStart(CardPanel dragPanel, MouseEvent evt) {
        for (CardPanelMouseListener listener : listeners)
            listener.mouseDragStart(mouseDragPanel, evt);
    }

    /**
     * <p>mouseOut.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseOut(CardPanel panel, MouseEvent evt) {
        for (CardPanelMouseListener listener : listeners)
            listener.mouseOut(mouseOverPanel, evt);
    }

    /**
     * <p>mouseOver.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     * @param evt a {@link java.awt.event.MouseEvent} object.
     */
    public void mouseOver(CardPanel panel, MouseEvent evt) {
        for (CardPanelMouseListener listener : listeners)
            listener.mouseOver(panel, evt);
    }

    /**
     * <p>getCardFromMouseOverPanel.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getCardFromMouseOverPanel() {
        if (mouseOverPanel != null)
            return mouseOverPanel.gameCard;
        else
            return null;
    }

    /**
     * <p>Getter for the field <code>zoneID</code>.</p>
     *
     * @return a int.
     */
    public int getZoneID() {
        return zoneID;
    }

    /**
     * <p>Setter for the field <code>zoneID</code>.</p>
     *
     * @param zoneID a int.
     */
    public void setZoneID(int zoneID) {
        this.zoneID = zoneID;
    }
}
