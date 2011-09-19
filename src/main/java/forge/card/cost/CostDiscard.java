package forge.card.cost;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;

public class CostDiscard extends CostPartWithList {
	// Discard<Num/Type{/TypeDescription}>

    public CostDiscard(String amount, String type, String description){
        super(amount, type, description);
    }

	@Override
	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Discard ");
        
        Integer i = convertAmount();
        
        if (getThis()) {
        	sb.append(type);
        } 
        else if (type.equals("Hand")) {
        	sb.append("your hand");
        } 
        else if (type.equals("LastDrawn")) {
        	sb.append("last drawn card");
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
        CardList handList = activator.getCardsIn(Zone.Hand);
        String type = getType();
        Integer amount = convertAmount();

        if (getThis()) {
            if (!AllZone.getZoneOf(source).is(Constant.Zone.Hand))
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
        CardList handList = activator.getCardsIn(Zone.Hand);
        String discType = getType();
        String amount = getAmount();
        resetList();

        if (getThis()) {
            if (!handList.contains(source)){
                return false;
            }
            activator.discard(source, ability);
            payment.setPaidManaPart(this, true);
            addToList(source);
        } else if (discType.equals("Hand")) {
            list = handList;
            activator.discardHand(ability);
            payment.setPaidManaPart(this, true);
        } else if (discType.equals("LastDrawn")) {
            Card lastDrawn = activator.getLastDrawnCard();
            addToList(lastDrawn);
            if (!handList.contains(lastDrawn)) {
                return false;
            }
            activator.discard(lastDrawn, ability);
            payment.setPaidManaPart(this, true);
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
                
                list = activator.discardRandom(c, ability);
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
                
                CostUtil.setInput(CostDiscard.input_discardCost(discType, handList, ability, payment, this, c));
                return false;
            }
        }
        addListToHash(ability, "Discarded");
        return true;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        String type = getType();
        Player activator = ability.getActivatingPlayer();
        CardList hand = activator.getCardsIn(Zone.Hand);
        resetList();
        if (type.equals("LastDrawn")){
            if (!hand.contains(activator.getLastDrawnCard()))
                return false;
            addToList(activator.getLastDrawnCard());
        }

        else if (getThis()){
            if (!hand.contains(source))
                return false;
            
            addToList(source);
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
                list = ComputerUtil.AI_discardNumType(c, type.split(";"), ability);
            }
        }
        return list != null;
    }

    // Inputs
    
    /**
     * <p>input_discardCost.</p>
     * @param discType a {@link java.lang.String} object.
     * @param handList a {@link forge.CardList} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param part TODO
     * @param nNeeded a int.
     *
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_discardCost(final String discType, final CardList handList, SpellAbility sa, final Cost_Payment payment, final CostDiscard part, final int nNeeded) {
        final SpellAbility sp = sa;
        Input target = new Input() {
            private static final long serialVersionUID = -329993322080934435L;
    
            int nDiscard = 0;
    
            @Override
            public void showMessage() {
                if (nNeeded == 0){
                    done();
                }
                
                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) stop();
                StringBuilder type = new StringBuilder("");
                if (!discType.equals("Card")) {
                    type.append(" ").append(discType);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Select a ");
                sb.append(part.getDescriptiveType());
                sb.append(" to discard.");
                if (nNeeded > 1) {
                    sb.append(" You have ");
                    sb.append(nNeeded - nDiscard);
                    sb.append(" remaining.");
                }
                AllZone.getDisplay().showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }
    
            @Override
            public void selectButtonCancel() {
                cancel();
            }
    
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand) && handList.contains(card)) {
                    // send in CardList for Typing
                    card.getController().discard(card, sp);
                    part.addToList(card);
                    handList.remove(card);
                    nDiscard++;
    
                    //in case no more cards in hand
                    if (nDiscard == nNeeded)
                        done();
                    else if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0)    // this really shouldn't happen
                        cancel();
                    else
                        showMessage();
                }
            }
    
            public void cancel() {
                stop();
                payment.cancelCost();
            }
    
            public void done() {
                stop();
                part.addListToHash(sp, "Discarded");
                payment.paidCost(part);
            }
        };
    
        return target;
    }//input_discard() 
}
