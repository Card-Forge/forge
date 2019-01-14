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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

//import javax.swing.ScrollPaneConstants;

import forge.game.card.Card;
import forge.game.card.CardView;
//import forge.game.card.CardCollection;
import forge.screens.match.CMatchUI;
import forge.view.arcane.util.CardPanelMouseAdapter;
//import forge.toolbox.FScrollPane;
//import forge.util.collect.FCollectionView;
//import forge.toolbox.MouseTriggerEvent;
//import forge.view.FFrame;
import forge.view.FDialog;

import forge.toolbox.FButton;

// Show a list of cards in a new window
// Allow moves of the visible cards to top, to bottom, or anywhere
// Return a list of cards with the results of the moves
// Really should have a difference between visible cards and moveable cards,
// but that would require consirable changes to card panels and elsewhere
public class ListCardArea extends FloatingCardArea {

    private static ArrayList<Card> cardList;
    private static ArrayList<Card> moveableCards;

    private static ListCardArea storedArea;
    private static FButton doneButton;
    private boolean toTop, toBottom, toAnywhere;

    private ListCardArea(final CMatchUI matchUI) {
	super(matchUI);
	window.add(getScrollPane(),"grow, push");
	getScrollPane().setViewportView(this);
	setOpaque(false);
    }

    public static ListCardArea show(final CMatchUI matchUI, final String title0, final List<Card> cardList0, final List<Card> moveableCards0, final boolean toTop0, final boolean toBottom0, final boolean toAnywhere0) {
	if (storedArea==null) {
	    storedArea = new ListCardArea(matchUI);
	    doneButton = new FButton("Done");
	    doneButton.addActionListener(new ActionListener() {
		    @Override public void actionPerformed(ActionEvent e) { window.setVisible(false); } 
		});
	    window.add(doneButton,BorderLayout.SOUTH);
	}
	cardList = new ArrayList<Card>(cardList0);  // this is modified - pfps - is there a better way?
	moveableCards = new ArrayList<Card>(moveableCards0);
	storedArea.title = title0;
	storedArea.toTop = toTop0;
	storedArea.toBottom = toBottom0;
	storedArea.toAnywhere = toAnywhere0;
        storedArea.setDragEnabled(true);
	storedArea.setVertical(true);
        storedArea.showWindow(); 
	return storedArea;
    }

    public ListCardArea(final CMatchUI matchUI, final String title0, final List<Card> cardList0, final List<Card> moveableCards0, final boolean toTop0, final boolean toBottom0, final boolean toAnywhere0) {
        super(matchUI);
	window.add(getScrollPane(),"grow, push");
	//	try { Thread.sleep(1000); } catch(InterruptedException ex) { }
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

    protected Iterable<CardView> getCards() {
	ArrayList<CardView> result = new ArrayList<CardView>();
	for ( Card c : cardList ) {
	    result.add(c.getView());
	}
	return result;
    }

    public List<Card> getCardList() {
	return cardList;
    }

    @SuppressWarnings("serial")
    protected static final FDialog window = new FDialog(true, true, "0") {
        @Override
        public void setLocationRelativeTo(Component c) {
            super.setLocationRelativeTo(c);
        }
        @Override
        public void setVisible(boolean b0) {
            if (isVisible() == b0) { return; }
            if (b0) {
		storedArea.refresh();
            }
            super.setVisible(b0);
        }
    };

    @Override
    protected FDialog getWindow() {
	return window;
    }

    @Override
    protected void showWindow() {
        onShow();
        getWindow().setFocusableWindowState(true);
        getWindow().setVisible(true);
    }

    @Override
    protected void onShow() {
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

    @Override
    protected void refresh() {
	doRefresh();
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

    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
	final Card clickCard = panelToCard(panel);
	if (moveableCards.contains(clickCard) && toTop) {
	    synchronized (cardList) {
		cardList.remove(clickCard);
		cardList.add(0,clickCard);
	    }
	    refresh();
	}
        super.mouseLeftClicked(panel, evt);
    }
    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
	final Card clickCard = panelToCard(panel);
	if (moveableCards.contains(clickCard) && toBottom ) {
	    synchronized (cardList) {
		cardList.remove(clickCard);
		cardList.add(clickCard);
	    }
	    refresh();
	}
        super.mouseRightClicked(panel, evt);
    }

}
