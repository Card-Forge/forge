
package forge;


abstract public class Ability_Activated extends SpellAbility implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
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
        if (c.isFaceDown() && isIntrinsic())	// Intrinsic abilities can't be activated by face down cards
        	return false;
        
        if(c.isCreature()) {
			CardList Silence = AllZoneUtil.getPlayerCardsInPlay(AllZone.GameAction.getOpponent(getSourceCard().getController()));
			Silence = Silence.getName("Linvala, Keeper of Silence");
			if (Silence.size() != 0)
				return false;
        }
        return Cost_Payment.canPayAdditionalCosts(payCosts, this) && AllZone.GameAction.isCardInPlay(c);
        //TODO: make sure you can't play the Computer's activated abilities
    }
}
