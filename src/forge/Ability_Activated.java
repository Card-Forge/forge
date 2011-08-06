
package forge;


abstract public class Ability_Activated extends SpellAbility implements java.io.Serializable {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    
    public Ability_Activated(Card sourceCard) {
        this(sourceCard, "");
    }
    
    // todo: remove this constructor when everything uses the abCost system
    public Ability_Activated(Card sourceCard, String manaCost) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(manaCost);
    }
    
    public Ability_Activated(Card sourceCard, Ability_Cost abCost, Target tgt) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(abCost.getMana());
        setPayCosts(abCost);
        if (tgt != null && tgt.doesTarget())
        	setTarget(tgt);
    }
    
    @Override
    public boolean canPlay() {
        Card c = getSourceCard();
        if(c.isCreature() == true) {
		CardList Silence = AllZoneUtil.getPlayerCardsInPlay(AllZone.GameAction.getOpponent(getSourceCard().getController()));
		Silence = Silence.getName("Linvala, Keeper of Silence");
        return AllZone.GameAction.isCardInPlay(c) && !c.isFaceDown() && Silence.size() == 0;
        }
        return AllZone.GameAction.isCardInPlay(c) && !c.isFaceDown();
        //TODO: make sure you can't play the Computer's activated abilities
    }
}
