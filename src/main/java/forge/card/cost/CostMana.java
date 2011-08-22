package forge.card.cost;

import com.google.common.base.Strings;

import forge.Card;
import forge.ComputerUtil;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostMana extends CostPart {
	//"Leftover"
    private String mana = "";
    private int amountX = 0;
    private String adjustedMana = "";
    
    public String getMana() {
    	// Only used for Human to pay for non-X cost first
        return mana;
    }    

    public void setMana(String sCost) {
        mana = sCost;
    }

    public boolean hasNoXManaCost() {
        return amountX == 0;
    }

    public int getXMana() {
        return amountX;
    }

    public void setXMana(int xCost) {
        amountX = xCost;
    }
    
    public String getAdjustedMana() {
        return adjustedMana;
    }

    public void setAdjustedMana(String adjustedMana) {
        this.adjustedMana = adjustedMana;
    }
    
    public CostMana(String mana, int amount){
    	this.mana = mana.trim();
    	this.amountX = amount;
    	this.isUndoable = true;
    	this.isReusable = true;
    }    

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
	
		sb.append(Strings.repeat("X ", amountX));
		if (!mana.equals("0"))
			sb.append(mana);
		
		return sb.toString().trim();
	}

	@Override
	public void refund(Card source) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        // For now, this will always return true. But this should probably be checked at some point
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        ComputerUtil.payManaCost(ability);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        int manaToAdd = 0;
        if (!hasNoXManaCost()) {
            // if X cost is a defined value, other than xPaid
            if (!source.getSVar("X").equals("Count$xPaid")) {
                // this currently only works for things about Targeted object
                manaToAdd = AbilityFactory.calculateAmount(source, "X", ability) * getXMana();
            }
        }
        if (!getMana().equals("0") || manaToAdd > 0)
            CostUtil.setInput(Cost_Input.input_payMana(ability, payment, this, manaToAdd));
        else if (getXMana() > 0)
            CostUtil.setInput(Cost_Input.input_payXMana(ability, payment, this, getXMana()));
        
        // We return false here because the Inputs set above should recall payment.payCosts()
        return false;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        return true;
    }
}
