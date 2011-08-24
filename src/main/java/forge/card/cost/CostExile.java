package forge.card.cost;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostExile extends CostPartWithList {
	//Exile<Num/Type{/TypeDescription}>
	//ExileFromHand<Num/Type{/TypeDescription}>
	//ExileFromGraveyard<Num/Type{/TypeDescription}>
	//ExileFromLibrary<Num/Type{/TypeDescription}>

    private String from = Constant.Zone.Battlefield;

    public String getFrom() {
        return from;
    }
    
    public CostExile(String amount, String type, String description, String from){
    	this.amount = amount;
    	this.type = type;
    	this.typeDescription = description;
    	if (from != null)
    		this.from = from;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append("Exile ");

        if (getThis()) {
        	sb.append(type);
        }
        else{
	        Integer i = convertAmount();
	        String desc = typeDescription == null ? type : typeDescription;
	        
	        sb.append(Cost.convertAmountTypeToWords(i, amount, desc));
        }
        
        if (from.equals("Battlefield")){
        	if (!getThis())
        		sb.append(" you control");
        }
        else{
        	sb.append(" from your ").append(from);
        }
        
        return sb.toString();
	}

	@Override
	public void refund(Card source) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        PlayerZone zone = AllZone.getZone(getFrom(), activator);
        if (!getThis()) {
            CardList typeList = AllZoneUtil.getCardsInZone(zone);

            typeList = typeList.getValidCards(getType().split(";"), activator, source);
            
            Integer amount = convertAmount(); 
            if (amount != null && typeList.size() < amount)
                return false;
        } else if (!AllZoneUtil.isCardInZone(zone, source))
            return false;
        
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        for (Card c : list)
            AllZone.getGameAction().exile(c);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        String amount = getAmount();
        Integer c = convertAmount();
        Player activator = ability.getActivatingPlayer();
        CardList list = AllZoneUtil.getCardsInZone(getFrom(), activator);
        list = list.getValidCards(type.split(";"), activator, source);
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
        if (getThis()){
            CostUtil.setInput(Cost_Input.exileThis(ability, payment, this));
        }
        else if (from.equals(Constant.Zone.Battlefield) || from.equals(Constant.Zone.Hand)){
            CostUtil.setInput(Cost_Input.exileType(ability, this, getType(), payment, c));
        }
        else if (from.equals(Constant.Zone.Library)){
            Cost_Input.exileFromTop(ability, this, payment, c);
        }
        else{
            CostUtil.setInput(Cost_Input.exileFrom(ability, this, getType(), payment, c));
        }
        return false;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        resetList();
        if (getThis())
            list.add(source);
        else{
            Integer c = convertAmount();
            if (c == null){
                String sVar = source.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")){
                    return false;
                }
                
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
            
            if (from.equals(Constant.Zone.Library)){
                list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.getComputerPlayer(), c);
            }
            else{
                list = ComputerUtil.chooseExileFrom(getFrom(), getType(), source, ability.getTargetCard(), c);
            }
            if (list == null || list.size() < c)
                return false;
        }
        return true;
    }
}
