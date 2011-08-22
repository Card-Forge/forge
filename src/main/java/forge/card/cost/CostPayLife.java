package forge.card.cost;

import forge.AllZone;
import forge.Card;
import forge.GameActionUtil;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostPayLife extends CostPart {
    private int lastPaidAmount = 0;

    public int getLastPaidAmount(){
        return lastPaidAmount;
    }
    
    public void setLastPaidAmount(int paidAmount){
    	lastPaidAmount = paidAmount;
    }
    
    public CostPayLife(String amount){
    	this.amount = amount;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Pay ").append(amount).append(" Life");
		return sb.toString();
	}

	@Override
	public void refund(Card source) {
		// Really should be activating player
		source.getController().payLife(lastPaidAmount * -1, null);
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        Integer amount = convertAmount();
        if (amount != null && !activator.canPayLife(amount)) {
            return false;
        }

        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        AllZone.getComputerPlayer().payLife(getLastPaidAmount(), null);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        String amount = getAmount();
        int life = ability.getActivatingPlayer().getLife();
        
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
        sb.append(source.getName()).append(" - Pay ").append(c).append(" Life?");
        
        if (GameActionUtil.showYesNoDialog(source, sb.toString())) {
            AllZone.getHumanPlayer().payLife(c, null);
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
                // This decision should be locked in the AF and be calculable at this state
                //c = chooseXValue(life);
                // Figure out what to do here
            }
            else{
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        if (!activator.canPayLife(c)){
            return false;
        }
        
        setLastPaidAmount(c);
        return true;
    }
}
