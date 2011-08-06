
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
        return AllZone.GameAction.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown();
        
//      return false;
    }
}
