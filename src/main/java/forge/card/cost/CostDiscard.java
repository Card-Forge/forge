package forge.card.cost;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.Constant;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostDiscard extends CostPartWithList {
	// Discard<Num/Type{/TypeDescription}>

    public CostDiscard(String amount, String type, String description){
    	this.amount = amount;
    	this.type = type;
    	this.typeDescription = description;
    }

	@Override
	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Discard");
        
        Integer i = convertAmount();
        
        if (getThis()) {
        	sb.append(" ").append(type);
        } 
        else if (type.equals("Hand")) {
        	sb.append(" your hand");
        } 
        else if (type.equals("LastDrawn")) {
        	sb.append(" last drawn card");
        } 
        else {
        	StringBuilder desc = new StringBuilder();
        	
        	if (type.equals("Card") || type.equals("Random"))
        		desc.append("Card");
        	else
        		desc.append(typeDescription == null ? type : typeDescription).append(" card");
        	
        	sb.append(Cost.convertAmountTypeToWords(i, amount, desc.toString()));

            if (type.equals("Random"))
            	sb.append(" at random");
        }
        return sb.toString();

	}

	@Override
	public void refund(Card source) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        CardList handList = AllZoneUtil.getPlayerHand(activator);
        String type = getType();
        Integer amount = convertAmount();

        if (getThis()) {
            if (!AllZone.getZone(source).is(Constant.Zone.Hand))
                return false;
        } else if (type.equals("Hand")) {
            // this will always work
        } else if (type.equals("LastDrawn")) {
            Card c = activator.getLastDrawnCard();
            return handList.contains(c);
        } else {
            if (!type.equals("Random")) {
                handList = handList.getValidCards(type.split(";"), activator, source);
            }
            if (amount != null && amount > handList.size()) {
                // not enough cards in hand to pay
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        Player activator = ability.getActivatingPlayer();
        for (Card c : list)
            activator.discard(c, ability);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        Player activator = ability.getActivatingPlayer();
        CardList handList = AllZoneUtil.getPlayerHand(activator);
        String discType = getType();
        String amount = getAmount();

        if (getThis()) {
            activator.discard(source, ability);
            payment.setPaidManaPart(this, true);
        } else if (discType.equals("Hand")) {
            activator.discardHand(ability);
            payment.setPaidManaPart(this, true);
        } else if (discType.equals("LastDrawn")) {
            if (handList.contains(activator.getLastDrawnCard())) {
                activator.discard(activator.getLastDrawnCard(), ability);
                payment.setPaidManaPart(this, true);
            }
        } else {
            Integer c = convertAmount();

            if (discType.equals("Random")) {
                if (c == null){
                    String sVar = source.getSVar(amount);
                    // Generalize this
                    if (sVar.equals("XChoice")){
                        c = CostUtil.chooseXValue(source, handList.size());
                    }
                    else{
                        c = AbilityFactory.calculateAmount(source, amount, ability);
                    }
                }
                
                activator.discardRandom(c, ability);
                payment.setPaidManaPart(this, true);
            } else {
                String validType[] = discType.split(";");
                handList = handList.getValidCards(validType, activator, ability.getSourceCard());
                
                if (c == null){
                    String sVar = source.getSVar(amount);
                    // Generalize this
                    if (sVar.equals("XChoice")){
                        c = CostUtil.chooseXValue(source, handList.size());
                    }
                    else{
                        c = AbilityFactory.calculateAmount(source, amount, ability);
                    }
                }
                
                CostUtil.setInput(Cost_Input.input_discardCost(discType, handList, ability, payment, this, c));
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        String type = getType();
        Player activator = ability.getActivatingPlayer();
        CardList hand = AllZoneUtil.getPlayerHand(activator);
        resetList();
        if (type.equals("LastDrawn")){
            if (!hand.contains(activator.getLastDrawnCard()))
                return false;
            list.add(activator.getLastDrawnCard());
        }

        else if (getThis()){
            if (!hand.contains(source))
                return false;
            
            list.add(source);
        }
        
        else if (type.equals("Hand")){
            list.addAll(hand);
        }
        
        else{
            Integer c = convertAmount();
            if (c == null){
                String sVar = source.getSVar(amount);
                if (sVar.equals("XChoice")){
                    return false;
                }
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }

            if (type.equals("Random")){
                list = CardListUtil.getRandomSubList(hand, c);
            }
            else{
                list = AllZone.getGameAction().AI_discardNumType(c, type.split(";"), ability);
            }
        }
        return list != null;
    }
}
