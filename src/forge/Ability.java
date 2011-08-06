
package forge;

import com.esotericsoftware.minlog.Log;


abstract public class Ability extends SpellAbility {
    public Ability(Card sourceCard, String manaCost) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(manaCost);
    }
    
    public Ability(Card sourceCard, String manaCost, String stackDescription) {
        this(sourceCard, manaCost);
        setStackDescription(stackDescription);
        Log.debug("an ability is being played from" + sourceCard.getName());
    }
    
    @Override
    public boolean canPlay() {
//      if(getSourceCard().isCreature() && (!getSourceCard().hasSickness()))
    	if(getSourceCard().isCreature() == true) {
		CardList Silence = AllZoneUtil.getPlayerCardsInPlay(getSourceCard().getController().getOpponent());
		Silence = Silence.getName("Linvala, Keeper of Silence");		
        return AllZone.GameAction.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown() && getSourceCard().getName().equals("Spreading Seas") == false && Silence.size() == 0; // For Spreading Seas
    	}
    	return AllZone.GameAction.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown() && getSourceCard().getName().equals("Spreading Seas") == false;
//      return false;
    }
}
