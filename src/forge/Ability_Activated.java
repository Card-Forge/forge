
package forge;


abstract public class Ability_Activated extends SpellAbility implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    public Ability_Activated(Card sourceCard, Ability_Cost abCost, Target tgt) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(abCost.getTotalMana());
        setPayCosts(abCost);
        if (tgt != null && tgt.doesTarget())
        	setTarget(tgt);
    }
    
    @Override
    public boolean canPlay() {
        Card c = getSourceCard();
        if (c.isFaceDown() && isIntrinsic())	// Intrinsic abilities can't be activated by face down cards
        	return false;
        if(c.hasKeyword("CARDNAME's activated abilities can't be activated.")) return false;
        
        CardList Pithing = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		Pithing.add(AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer));
		Pithing = Pithing.getName("Pithing Needle");
		Pithing = Pithing.filter(new CardListFilter() {
			public boolean addCard(Card c){
				return c.getSVar("PithingTarget").equals(c.getName());
			}
		});
		
		if(Pithing.size() != 0) return false;
        
        if(c.isCreature() && AllZone.getZone(c).getZoneName().equals(Constant.Zone.Battlefield)) {
			CardList Silence = AllZoneUtil.getPlayerCardsInPlay(getSourceCard().getController().getOpponent());
			Silence = Silence.getName("Linvala, Keeper of Silence");
			if (Silence.size() != 0)
				return false;
        }
        
        if (!(getRestrictions().canPlay(c, this)))     
        	return false;
        
        return Cost_Payment.canPayAdditionalCosts(payCosts, this);
        //TODO: make sure you can't play the Computer's activated abilities
    }
}
