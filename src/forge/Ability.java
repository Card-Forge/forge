
package forge;

import com.esotericsoftware.minlog.Log;


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
//      if(getSourceCard().isCreature() && (!getSourceCard().hasSickness()))
    	CardList Pithing = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		Pithing.add(AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer));
		Pithing = Pithing.getName("Pithing Needle");
		Pithing = Pithing.filter(new CardListFilter() {
			public boolean addCard(Card c){
				return c.getSVar("PithingTarget").equals(sourceCardName);
			}
		});
		
    	if(getSourceCard().isCreature() == true) {
    		CardList Silence = AllZoneUtil.getPlayerCardsInPlay(getSourceCard().getController().getOpponent());
			Silence = Silence.getName("Linvala, Keeper of Silence");

        	return AllZone.GameAction.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown() && getSourceCard().getName().equals("Spreading Seas") == false && Silence.size() == 0 && Pithing.size() == 0; // For Spreading Seas
    	}
    	
		
    	return AllZone.GameAction.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown() && getSourceCard().getName().equals("Spreading Seas") == false && Pithing.size() == 0;
//      return false;
    }
}
