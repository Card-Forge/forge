package forge.card.cost;

import forge.AllZone;
import forge.Card;
import forge.GameActionUtil;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostGainLife extends CostPart {
    private int lastPaidAmount = 0;

    public int getLastPaidAmount(){
        return lastPaidAmount;
    }
    
    public void setLastPaidAmount(int paidAmount){
    	lastPaidAmount = paidAmount;
    }
    
    public CostGainLife(String amount){
    	this.amount = amount;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Have each other player gain ").append(amount).append(" Life");
		return sb.toString();
	}

	@Override
	public void refund(Card source) {

	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        Integer amount = convertAmount();
        if (amount != null && !activator.getOpponent().canGainLife()) {
            return false;
        }

        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        AllZone.getHumanPlayer().gainLife(getLastPaidAmount(), null);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        String amount = getAmount();
        Player activator = ability.getActivatingPlayer();
        int life = activator.getLife();
        
        Integer c = convertAmount();
        if (c == null){
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")){
                c = CostUtil.chooseXValue(source, life);
            }
            else{
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Have each other player gain ").append(c).append(" Life?");
        
        if (GameActionUtil.showYesNoDialog(source, sb.toString()) && activator.getOpponent().canGainLife()) {
            activator.getOpponent().gainLife(c, null);
            setLastPaidAmount(c);
            payment.setPaidManaPart(this, true);
        } else {
            payment.setCancel(true);
            payment.getRequirements().finishPaying();
            return false;
        }
        return true;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        Player activator = ability.getActivatingPlayer();

        Integer c = convertAmount();
        if (c == null){
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")){
                return false;
            }
            else{
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        if (!activator.getOpponent().canGainLife()){
            return false;
        }
        setLastPaidAmount(c);
        return true;
    }
}
