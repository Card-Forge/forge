
package forge;


abstract public class Ability extends SpellAbility {
    public Ability(Card sourceCard, String manaCost) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(manaCost);
    }
    
    public Ability(Card sourceCard, String manaCost, String stackDescription) {
        this(sourceCard, manaCost);
        setStackDescription(stackDescription);
        System.out.println("an ability is being played from" + sourceCard.getName());
    }
    
    @Override
    public boolean canPlay() {
//      if(getSourceCard().isCreature() && (!getSourceCard().hasSickness()))
    	if(getSourceCard().isCreature() == true) {
		CardList Silence = AllZoneUtil.getPlayerCardsInPlay(AllZone.GameAction.getOpponent(getSourceCard().getController()));
		Silence = Silence.getName("Linvala, Keeper of Silence");		
        return AllZone.GameAction.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown() && getSourceCard().getName().equals("Spreading Seas") == false && Silence.size() == 0; // For Spreading Seas
    	}
    	return AllZone.GameAction.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown() && getSourceCard().getName().equals("Spreading Seas") == false;
//      return false;
    }
}
