
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

import forge.Card;
import arcane.ui.util.CardPanelMouseListener;
import arcane.ui.util.UI;

/**
 * Manages mouse events and common funcitonality for CardPanel containing components.
 */
abstract public class CardPanelContainer extends JPanel {
	private static final long serialVersionUID = -6400018234895548306L;

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

	public CardPanelContainer (JScrollPane scrollPane) {
		this.scrollPane = scrollPane;

		setOpaque(true);

		addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged (MouseEvent evt) {
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
				if (Math.abs(x - intialMouseDragX) < DRAG_SMUDGE && Math.abs(y - intialMouseDragY) < DRAG_SMUDGE) return;
				mouseDownPanel = null;
				mouseDragPanel = panel;
				mouseDragOffsetX = panel.getX() - intialMouseDragX;
				mouseDragOffsetY = panel.getY() - intialMouseDragY;
				CardPanelContainer.this.mouseDragStart(mouseDragPanel, evt);
			}

			public void mouseMoved (MouseEvent evt) {
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

			public void mousePressed (MouseEvent evt) {
				int button = evt.getButton();
				if (button < 1 || button > 3) return;
				buttonsDown[button] = true;
				mouseDownPanel = getCardPanel(evt.getX(), evt.getY());
			}

			public void mouseReleased (MouseEvent evt) {
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

			public void mouseExited (MouseEvent evt) {
				mouseOutPanel(evt);
			}

			public void mouseEntered (MouseEvent e) {
			}
		});
	}

	private void mouseOutPanel (MouseEvent evt) {
		if (mouseOverPanel == null) return;
		mouseOverPanel.setSelected(false);
		mouseOut(mouseOverPanel, evt);
		mouseOverPanel = null;
	}

	/*public void resetDrag(){
		mouseDragPanel = null;
		invalidate();
	};*/
	abstract protected CardPanel getCardPanel (int x, int y);

	/**
	 * Must call from the Swing event thread.
	 */
	public CardPanel addCard (Card card) {
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

	public CardPanel getCardPanel (int gameCardID) {
		for (CardPanel panel : cardPanels)
			if (panel.gameCard.getUniqueNumber() == gameCardID) return panel;
		return null;
	}

	public void removeCardPanel (final CardPanel fromPanel) {
		UI.invokeAndWait(new Runnable() {
			public void run () {
				if (mouseDragPanel != null){
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

	public void clear () {
		UI.invokeAndWait(new Runnable() {
			public void run () {
				cardPanels.clear();
				removeAll();
				setPreferredSize(new Dimension(0, 0));
				invalidate();
				getParent().validate();
				repaint();
			}
		});
	}

	public JScrollPane getScrollPane () {
		return scrollPane;
	}

	public int getCardWidthMin () {
		return cardWidthMin;
	}

	public void setCardWidthMin (int cardWidthMin) {
		this.cardWidthMin = cardWidthMin;
	}

	public int getCardWidthMax () {
		return cardWidthMax;
	}

	public void setCardWidthMax (int cardWidthMax) {
		this.cardWidthMax = cardWidthMax;
	}

	public boolean isDragEnabled () {
		return dragEnabled;
	}

	public void setDragEnabled (boolean dragEnabled) {
		this.dragEnabled = dragEnabled;
	}

	public void addCardPanelMouseListener (CardPanelMouseListener listener) {
		listeners.add(listener);
	}

	public void mouseLeftClicked (CardPanel panel, MouseEvent evt) {
		for (CardPanelMouseListener listener : listeners)
			listener.mouseLeftClicked(panel, evt);
	}

	public void mouseRightClicked (CardPanel panel, MouseEvent evt) {
		for (CardPanelMouseListener listener : listeners)
			listener.mouseRightClicked(panel, evt);
	}

	public void mouseMiddleClicked (CardPanel panel, MouseEvent evt) {
		for (CardPanelMouseListener listener : listeners)
			listener.mouseMiddleClicked(panel, evt);
	}

	public void mouseDragEnd (CardPanel dragPanel, MouseEvent evt) {
		for (CardPanelMouseListener listener : listeners)
			listener.mouseDragEnd(dragPanel, evt);
	}

	public void mouseDragged (CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt) {
		for (CardPanelMouseListener listener : listeners)
			listener.mouseDragged(mouseDragPanel, mouseDragOffsetX, mouseDragOffsetY, evt);
	}

	public void mouseDragStart (CardPanel dragPanel, MouseEvent evt) {
		for (CardPanelMouseListener listener : listeners)
			listener.mouseDragStart(mouseDragPanel, evt);
	}

	public void mouseOut (CardPanel panel, MouseEvent evt) {
		for (CardPanelMouseListener listener : listeners)
			listener.mouseOut(mouseOverPanel, evt);
	}

	public void mouseOver (CardPanel panel, MouseEvent evt) {
		for (CardPanelMouseListener listener : listeners)
			listener.mouseOver(panel, evt);
	}
	
	public Card getCardFromMouseOverPanel(){
		if(mouseOverPanel != null)
			return mouseOverPanel.gameCard;
		else
			return null;
	}

	public int getZoneID () {
		return zoneID;
	}

	public void setZoneID (int zoneID) {
		this.zoneID = zoneID;
	}
}
