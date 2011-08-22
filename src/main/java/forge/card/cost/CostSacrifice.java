package forge.card.cost;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostSacrifice extends CostPartWithList {

	public CostSacrifice(String amount, String type, String description){
    	this.amount = amount;
    	this.type = type;
    	this.typeDescription = description;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append("Sacrifice ");

        Integer i = convertAmount();
        
        if (getThis())
            sb.append(type);
        else{
        	String desc = typeDescription == null ? type : typeDescription;
	        if (i != null)
	        	sb.append(Cost.convertIntAndTypeToWords(i, desc));
	        else
	        	sb.append(Cost.convertAmountTypeToWords(amount, desc));
        }
        return sb.toString();
	}

	@Override
	public void refund(Card source) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        // You can always sac all
        if (!getThis()) {
            CardList typeList = AllZoneUtil.getPlayerCardsInPlay(activator);
            typeList = typeList.getValidCards(getType().split(";"), activator, source);

            Integer amount = convertAmount();   
            
            if (amount != null && typeList.size() < amount)
                return false;
            
            // If amount is null, it's either "ALL" or "X" 
            // if X is defined, it needs to be calculated and checked, if X is choice, it can be Paid even if it's 0 
        } else if (!AllZoneUtil.isCardInPlay(source))
            return false;
        
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        for (Card c : list)
            AllZone.getGameAction().sacrifice(c);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        String amount = getAmount();
        String type = getType();
        Player activator = ability.getActivatingPlayer();
        CardList list = AllZoneUtil.getPlayerCardsInPlay(activator);
        list = list.getValidCards(type.split(";"), activator, source);
        
        if (getThis()){
            CostUtil.setInput(Cost_Input.sacrificeThis(ability, payment, this));
        }
        else if (amount.equals("All")){
            Cost_Input.sacrificeAll(ability, payment, this, list);
            return true;
        }
        else{
            Integer c = convertAmount();
            if (c == null){
                String sVar = source.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")){
                    c = CostUtil.chooseXValue(source, list.size());
                }
                else{
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }
            
            CostUtil.setInput(Cost_Input.sacrificeFromList(ability, payment, this, list, c));
        }
            
        return false;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        resetList();
        Player activator = ability.getActivatingPlayer();
        if (getThis()){
            list.add(source);
        }
        else if (amount.equals("All")) {
            CardList typeList = AllZoneUtil.getPlayerCardsInPlay(activator);
            typeList = typeList.getValidCards(type.split(","), activator, source);
            // Does the AI want to use Sacrifice All?
            return false;
        } else{
            Integer c = convertAmount();
            if (c == null){
                if (source.getSVar(amount).equals("XChoice")){
                    return false;
                }
                
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
            list = ComputerUtil.chooseSacrificeType(type, source, ability.getTargetCard(), c);
            if (list == null)
                return false;
        }
        return true;
    }
}
