package forge.card.cost;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;

public class CostTapType extends CostPartWithList {
    public CostTapType(String amount, String type, String description){
        super(amount, type, description);
        isReusable = true;
    }
    
    public String getDescription(){
    	return typeDescription == null ? type : typeDescription;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append("Tap ");

        Integer i = convertAmount();
        String desc = getDescription();
        
        sb.append(Cost.convertAmountTypeToWords(i, amount, "untapped " + desc));
        
        sb.append(" you control");
        
        return sb.toString();
	}

    public void addToTappedList(Card c) {
        list.add(c);
    }
	
	@Override
	public void refund(Card source) {
        for (Card c : list){
            c.untap();
        }
        
        list.clear();
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        CardList typeList = activator.getCardsIn(Zone.Battlefield);

        typeList = typeList.getValidCards(getType().split(";"), activator, source);

        if (cost.getTap())
            typeList.remove(source);
        typeList = typeList.filter(CardListFilter.untapped);

        Integer amount = convertAmount();
        if (typeList.size() == 0 || (amount != null && typeList.size() < amount))
            return false;
        
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        for (Card c : list)
            c.tap();
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        CardList typeList = source.getController().getCardsIn(Zone.Battlefield);
        typeList = typeList.getValidCards(getType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
        String amount = getAmount();
        Integer c = convertAmount();
        if (c == null){
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")){
                c = CostUtil.chooseXValue(source, typeList.size());
            }
            else{
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        
        CostUtil.setInput(CostTapType.input_tapXCost(this, typeList, ability, payment, c));
        return false;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        boolean tap = payment.getCost().getTap();
        Integer c = convertAmount();
        if (c == null){
            // Determine Amount
        }
        
        list = ComputerUtil.chooseTapType(getType(), source, tap, c);
        
        if (list == null) {
            System.out.println("Couldn't find a valid card to tap for: " + source.getName());
            return false;
        }

        return true;
    }

    // Inputs
    
    /**
     * <p>input_tapXCost.</p>
     *
     * @param nCards a int.
     * @param cardType a {@link java.lang.String} object.
     * @param cardList a {@link forge.CardList} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_tapXCost(final CostTapType tapType, final CardList cardList, final SpellAbility sa, final Cost_Payment payment, final int nCards) {      
        Input target = new Input() {
    
            private static final long serialVersionUID = 6438988130447851042L;
            int nTapped = 0;
    
            @Override
            public void showMessage() {
                if (nCards == 0){
                    done();
                }
                
                if (cardList.size() == 0) stop();
    
                int left = nCards - nTapped;
                AllZone.getDisplay().showMessage("Select a " + tapType.getDescription() + " to tap (" + left + " left)");
                ButtonUtil.enableOnlyCancel();
            }
    
            @Override
            public void selectButtonCancel() {
                cancel();
            }
    
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (zone.is(Constant.Zone.Battlefield) && cardList.contains(card) && card.isUntapped()) {
                    // send in CardList for Typing
                    card.tap();
                    tapType.addToList(card);
                    cardList.remove(card);

                    nTapped++;
    
                    if (nTapped == nCards)
                        done();
                    else if (cardList.size() == 0)    // this really shouldn't happen
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
                payment.paidCost(tapType);
                tapType.addListToHash(sa, "Tapped");
            }
        };
    
        return target;
    }//input_tapXCost() 
}
