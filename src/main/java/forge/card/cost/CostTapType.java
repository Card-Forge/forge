package forge.card.cost;

import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostTapType extends CostPartWithList {
    public CostTapType(String amount, String type, String description){
        list = new CardList();
        isReusable = true;
        this.amount = amount;
        this.type = type;
        this.typeDescription = description;
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
        CardList typeList = AllZoneUtil.getPlayerCardsInPlay(activator);

        typeList = typeList.getValidCards(getType().split(";"), activator, source);

        if (cost.getTap())
            typeList.remove(source);
        typeList = typeList.filter(AllZoneUtil.untapped);

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
        CardList typeList = AllZoneUtil.getPlayerCardsInPlay(source.getController());
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
        
        CostUtil.setInput(Cost_Input.input_tapXCost(this, typeList, ability, payment, c));
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
}
