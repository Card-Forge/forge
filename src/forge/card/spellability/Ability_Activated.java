
package forge.card.spellability;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;


abstract public class Ability_Activated extends SpellAbility implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    public Ability_Activated(Card card, String manacost) {
		this(card, new Cost(manacost, card.getName(), true), null);
	}
    
    public Ability_Activated(Card sourceCard, Cost abCost, Target tgt) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(abCost.getTotalMana());
        setPayCosts(abCost);
        if (tgt != null && tgt.doesTarget())
        	setTarget(tgt);
    }

	@Override
    public boolean canPlay() {
    	if(AllZone.Stack.isSplitSecondOnStack()) return false;
    	
        final Card c = getSourceCard();
        if (c.isFaceDown() && isIntrinsic())	// Intrinsic abilities can't be activated by face down cards
        	return false;
        if(c.hasKeyword("CARDNAME's activated abilities can't be activated.")) return false;
        
        CardList Pithing = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		Pithing.add(AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer));
		Pithing = Pithing.getName("Pithing Needle");
		Pithing = Pithing.filter(new CardListFilter() {
			public boolean addCard(Card crd){
				return crd.getSVar("PithingTarget").equals(c.getName());
			}
		});
		
		if(Pithing.size() != 0) return false;
        
        if (!(getRestrictions().canPlay(c, this)))     
        	return false;
        
        return Cost_Payment.canPayAdditionalCosts(payCosts, this);
    }
}
