
package forge.card.spellability;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;


abstract public class Ability extends SpellAbility {
	//Slight hack for Pithing Needle
	private String sourceCardName;
	
    public Ability(Card sourceCard, String manaCost) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(manaCost);
        sourceCardName = sourceCard.getName();
    }
    
    public Ability(Card sourceCard, String manaCost, String stackDescription) {
        this(sourceCard, manaCost);
        setStackDescription(stackDescription);
        Log.debug("an ability is being played from" + sourceCard.getName());
    }
    
    @Override
    public boolean canPlay() {
    	if(AllZone.Stack.isSplitSecondOnStack()) return false;
    	
    	CardList pithing = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		pithing.addAll(AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer));
		pithing = pithing.getName("Pithing Needle");
		pithing = pithing.filter(new CardListFilter() {
			public boolean addCard(Card c){
				return c.getSVar("PithingTarget").equals(sourceCardName);
			}
		});
    		
    	return AllZoneUtil.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown() && getSourceCard().getName().equals("Spreading Seas") == false && pithing.size() == 0;
    }
}
