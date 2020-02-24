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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import forge.game.card.CardView;
import forge.screens.match.CMatchUI;
import forge.view.arcane.util.CardPanelMouseAdapter;

import forge.toolbox.FButton;
import forge.util.Localizer;

// Show a list of cards in a new window, containing the moveable cards
// Allow moves of the moveable cards to top, to bottom, or anywhere
// Return a list of cards with the results of the moves
// Really should have a difference between visible cards and moveable cards,
// but that would require consirable changes to card panels and elsewhere
public class ListCardArea extends FloatingCardArea {

    private static ArrayList<CardView> cardList;
    private static ArrayList<CardView> moveableCards;

    private static ListCardArea storedArea;
    private static FButton doneButton;
    private boolean toTop, toBottom, toAnywhere;

    private ListCardArea(final CMatchUI matchUI) {
		super(matchUI);
		window.add(getScrollPane(),"grow, push");
		window.setModal(true);
		getScrollPane().setViewportView(this);
		doneButton = new FButton(Localizer.getInstance().getMessage("lblDone"));
		doneButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { window.setVisible(false); } 
		});
		window.add(doneButton,BorderLayout.SOUTH);
		setOpaque(false);
    }

    public static ListCardArea show(final CMatchUI matchUI, final String title0, final Iterable<CardView> cardList0, final Iterable<CardView> moveableCards0, final boolean toTop0, final boolean toBottom0, final boolean toAnywhere0) {
		if (storedArea==null) {
			storedArea = new ListCardArea(matchUI);
		}
		cardList = new ArrayList<>();
		for ( CardView cv : cardList0 ) { cardList.add(cv) ; } 
		moveableCards = new ArrayList<>();  // make sure moveable cards are in cardlist
		for ( CardView card : moveableCards0 ) {
			if ( cardList.contains(card) ) {
				moveableCards.add(card);
			}
		}
		storedArea.title = title0;
		storedArea.toTop = toTop0;
		storedArea.toBottom = toBottom0;
		storedArea.toAnywhere = toAnywhere0;
		storedArea.setDragEnabled(true);
		storedArea.setVertical(true);
		storedArea.showWindow(); 
		return storedArea;
    }

    public ListCardArea(final CMatchUI matchUI, final String title0, final List<CardView> cardList0, final List<CardView> moveableCards0, final boolean toTop0, final boolean toBottom0, final boolean toAnywhere0) {
        super(matchUI);
		window.add(getScrollPane(),"grow, push");
		getScrollPane().setViewportView(this);
		setOpaque(false);
		doneButton = new FButton(Localizer.getInstance().getMessage("lblDone"));
		doneButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { window.setVisible(false); } 
		});
		window.add(doneButton,BorderLayout.SOUTH);
		cardList = new ArrayList<>(cardList0);  // this is modified - pfps - is there a better way?
		moveableCards = new ArrayList<>(moveableCards0);
		title = title0;
		toTop = toTop0;
		toBottom = toBottom0;
		toAnywhere = toAnywhere0;
		this.setDragEnabled(true);
		this.setVertical(true);
		storedArea = this;
    }

    public List<CardView> getCards() {
		return cardList;
    }

    @Override
    protected void showWindow() {
        onShow();
        getWindow().setFocusableWindowState(true);
        getWindow().setVisible(true);
    }

    @Override
    protected void onShow() {
		super.onShow();
        if (!hasBeenShown) {
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
    private boolean validIndex(final CardView card, final int index) {
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
        return toBottom && bottomMove;
    }

    @Override
    protected boolean cardPanelDraggable(final CardPanel panel) {
		return moveableCards.contains(panel.getCard());
    }

    private void dragEnd(final CardPanel dragPanel) {
		// if drag is not allowed, don't move anything
		final CardView dragCard = dragPanel.getCard();
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

    // move to beginning of list if allowable else to beginning of bottom if allowable
    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
		final CardView clickCard = panel.getCard();
		if ( moveableCards.contains(clickCard) ) {
			if ( toTop || toBottom ) {
				synchronized (cardList) {
					cardList.remove(clickCard);
					int position;
					if ( toTop ) {
						position = 0 ;
			    	} else { // to beginning of bottom: warning, untested
						for ( position = cardList.size() ; 
							  position>0 && moveableCards.contains(cardList.get(position-1)) ; 
							  position-- );
					}
					cardList.add(position,clickCard);
				}
				refresh();
			}
		}
        super.mouseLeftClicked(panel, evt);
    }
    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
		final CardView clickCard = panel.getCard();
		if (moveableCards.contains(clickCard)) {
			if ( toTop || toBottom ) {
				synchronized (cardList) {
					cardList.remove(clickCard);
					int position;
					if ( toBottom ) {
						position = cardList.size() ;
		    		} else { // to end of top
						for ( position = 0 ;
							  position<cardList.size() && moveableCards.contains(cardList.get(position)) ;
							  position++ );
					}
					cardList.add(position,clickCard);
				}
				refresh();
			}
		}
        super.mouseRightClicked(panel, evt);
    }

}
