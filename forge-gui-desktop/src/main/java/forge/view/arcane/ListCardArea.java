/*
 * Forge: Play Magic: the Gathering.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import forge.Singletons;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gui.framework.SDisplayUtil;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.CMatchUI;
import forge.view.arcane.util.CardPanelMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.MouseTriggerEvent;
import forge.view.FFrame;
import forge.view.FDialog;

import forge.toolbox.FButton;

public class ListCardArea extends CardArea {

    private static final String COORD_DELIM = ","; 
    private static final ForgePreferences prefs = FModel.getPreferences();

    public void show() {
        this.showWindow(); 
    }
    public void hide() {
        this.hideWindow(); 
    }

    private ArrayList<Card> cardList;
    private ArrayList<Card> moveableCards;
    private boolean toTop, toBottom, toAnywhere;
    private String title;
    private FPref locPref;
    private boolean hasBeenShown = false, locLoaded;
    private static ListCardArea storedArea;

    private final FButton doneButton;

    public ListCardArea(final CMatchUI matchUI, final String title0, final List<Card> cardList0, final List<Card> moveableCards0, final boolean toTop0, final boolean toBottom0, final boolean toAnywhere0) {
        super(matchUI, new FScrollPane(false, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
	window.add(getScrollPane(),"grow, push");
	try { Thread.sleep(1000); } catch(InterruptedException ex) { }
        getScrollPane().setViewportView(this);
        setOpaque(false);
	doneButton = new FButton("Done");
	doneButton.addActionListener(new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) { window.setVisible(false); } 
	    });
	window.add(doneButton,BorderLayout.SOUTH);
	cardList = new ArrayList<Card>(cardList0);  // this is modified - pfps - is there a better way?
	moveableCards = new ArrayList<Card>(moveableCards0);
	title = title0;
	toTop = toTop0;
	toBottom = toBottom0;
	toAnywhere = toAnywhere0;
        this.setDragEnabled(true);
	this.setVertical(true);
	storedArea = this;
    }

    public List<Card> getCardList() {
	return cardList;
    }

    @SuppressWarnings("serial")
    //    private SkinnedFrame window = new SkinnedFrame() {
    private final FDialog window = new FDialog(true, true, "0") {
        @Override
        public void setLocationRelativeTo(Component c) {
            super.setLocationRelativeTo(c);
        }

        @Override
        public void setVisible(boolean b0) {
            if (isVisible() == b0) { return; }
            if (b0) {
                refresh();
            }
            super.setVisible(b0);
        }
    };

    private void showWindow() {
	onShow();
        window.setFocusableWindowState(true);
        window.setVisible(true);
    }
    private void hideWindow() {
	onShow();
        window.setFocusableWindowState(false); // should probably do this earlier
        window.setVisible(false);
    }
    private void onShow() {
        if (!hasBeenShown) {
            loadLocation();
	    this.addCardPanelMouseListener(new CardPanelMouseAdapter() {
		    @Override
		    public void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
			dragEnd(dragPanel);
		    }
		});
	    this.addKeyListener(new KeyAdapter() {
		    @Override
		    public void keyPressed(final KeyEvent e) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ENTER:
			    doneButton.doClick();
			    break;
			default:
			    break;
			}
		    }
		});
	}

    }

    // is this a valid place to move the card?
    private boolean validIndex(final Card card, final int index) {
	if (toAnywhere) { return true; }
	int oldIndex = cardList.indexOf(card);
	boolean topMove = true;
	for(int i=0; i<index+(oldIndex<index?1:0); i++) {
	    if (!moveableCards.contains(cardList.get(i))) { topMove=false; break; }
	}
	if (toTop && topMove) { return true; }
	boolean bottomMove = true;
	for(int i=index+1-(oldIndex>index?1:0); i<cardList.size(); i++) {
	    if (!moveableCards.contains(cardList.get(i))) { bottomMove=false; break; }
	}
	if (toBottom && bottomMove) { return true; }
	return false;
    }

    protected Card panelToCard(final CardPanel panel) { //pfps there must be a better way
	final CardView panelView = panel.getCard();
	Card panelCard = null;  
	for ( Card card : cardList ) { if ( panelView == card.getView() ) { panelCard = card; } }
	return panelCard;
    }

    @Override
    protected boolean cardPanelDraggable(final CardPanel panel) {
	return moveableCards.contains(panelToCard(panel));
    }

    private void dragEnd(final CardPanel dragPanel) {
	// if drag is not allowed, don't move anything
	final Card dragCard = panelToCard(dragPanel);
	if (moveableCards.contains(dragCard)) {
	    //update index of dragged card in hand zone to match new index within hand area
	    final int index = getCardPanels().indexOf(dragPanel);
	    if (validIndex(dragCard,index)) { 
		synchronized (cardList) {
		    cardList.remove(dragCard);
		    cardList.add(index, dragCard);
		}
	    }
	}		
	refresh();
    }

    private void loadLocation() {
        if (locPref != null) {
            String value = prefs.getPref(locPref);
            if (value.length() > 0) {
                String[] coords = value.split(COORD_DELIM);
                if (coords.length == 4) {
                    try {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int w = Integer.parseInt(coords[2]);
                        int h = Integer.parseInt(coords[3]);
    
                        //ensure the window is accessible
                        int centerX = x + w / 2;
                        int centerY = y + h / 2;
                        Rectangle screenBounds = SDisplayUtil.getScreenBoundsForPoint(new Point(centerX, centerY)); 
                        if (centerX < screenBounds.x) {
                            x = screenBounds.x;
                        }
                        else if (centerX > screenBounds.x + screenBounds.width) {
                            x = screenBounds.x + screenBounds.width - w;
                            if (x < screenBounds.x) {
                                x = screenBounds.x;
                            }
                        }
                        if (centerY < screenBounds.y) {
                            y = screenBounds.y;
                        }
                        else if (centerY > screenBounds.y + screenBounds.height) {
                            y = screenBounds.y + screenBounds.height - h;
                            if (y < screenBounds.y) {
                                y = screenBounds.y;
                            }
                        }
                        window.setBounds(x, y, w, h);
                        locLoaded = true;
                        return;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                prefs.setPref(locPref, ""); //clear value if invalid
                prefs.save();
            }
        }
        //fallback default size
        FFrame mainFrame = Singletons.getView().getFrame();
        window.setSize(mainFrame.getWidth() / 5, mainFrame.getHeight() / 2);
    }

    public void refresh() {
        List<CardPanel> cardPanels = new ArrayList<CardPanel>();
	//        FCollectionView<Card> cards = new FCollection<Card>(cardList);
        if (cardList != null) {
            for (final Card card : cardList) {
                CardPanel cardPanel = getCardPanel(card.getId());
                if (cardPanel == null) {
                    cardPanel = new CardPanel(getMatchUI(), card.getView());
                    cardPanel.setDisplayEnabled(true);
                }
                else {
                    cardPanel.setCard(card.getView()); //ensure card view updated
                }
                cardPanels.add(cardPanel);
            }
        }

        boolean hadCardPanels = getCardPanels().size() > 0;
        setCardPanels(cardPanels);
        window.setTitle(String.format(title, cardPanels.size()));

        //if window had cards and now doesn't, hide window
        //(e.g. cast final card from Flashback zone)
        if (hadCardPanels && cardPanels.size() == 0) {
            window.setVisible(false);
        }
    }

    @Override
    public void doLayout() {
	//        if (window.isResizing()) {
	//  //delay layout slightly to reduce flicker during window resize
	//     layoutTimer.restart();
	// }
        //else {
            finishDoLayout();
        //}
    }

    private final Timer layoutTimer = new Timer(250, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            layoutTimer.stop();
            finishDoLayout();
        }
    });

    private void finishDoLayout() {
        super.doLayout();
    }

    @Override
    public final void mouseOver(final CardPanel panel, final MouseEvent evt) {
        getMatchUI().setCard(panel.getCard(), evt.isShiftDown());
        super.mouseOver(panel, evt);
    }
    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt));
        super.mouseLeftClicked(panel, evt);
    }
    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt));
        super.mouseRightClicked(panel, evt);
    }

}
