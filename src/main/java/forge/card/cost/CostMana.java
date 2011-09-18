package forge.card.cost;

import com.google.common.base.Strings;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.ComputerUtil;
import forge.Constant.Zone;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCostUtil;

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
    
    public String getManaToPay() {
        // Only used for Human to pay for non-X cost first
        if (!adjustedMana.equals(""))
            return adjustedMana;
        
        return mana;
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
        if (!getManaToPay().equals("0") || manaToAdd > 0){
            CostUtil.setInput(CostMana.input_payMana(ability, payment, this, manaToAdd));
        }
        else if (getXMana() > 0){
            CostUtil.setInput(CostMana.input_payXMana(ability, payment, this, getXMana()));
        }
        else{
            payment.paidCost(this);
        }
        
        // We return false here because the Inputs set above should recall payment.payCosts()
        return false;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        return true;
    }

    // Inputs
    
    /**
     * <p>input_payXMana.</p>
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param costMana TODO
     * @param numX a int.
     *
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_payXMana(final SpellAbility sa, final Cost_Payment payment, final CostMana costMana, final int numX) {
        Input payX = new Input() {
            private static final long serialVersionUID = -6900234444347364050L;
            int xPaid = 0;
            ManaCost manaCost = new ManaCost(Integer.toString(numX));
    
            @Override
            public void showMessage() {
                if (manaCost.toString().equals(Integer.toString(numX))) // Can only cancel if partially paid an X value
                    ButtonUtil.enableAll();
                else
                    ButtonUtil.enableOnlyCancel();
    
                AllZone.getDisplay().showMessage("Pay X Mana Cost for " + sa.getSourceCard().getName() + "\n" + xPaid + " Paid so far.");
            }
    
            // selectCard
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (sa.getSourceCard().equals(card) && sa.isTapAbility()) {
                    // this really shouldn't happen but just in case
                    return;
                }
    
                manaCost = Input_PayManaCostUtil.activateManaAbility(sa, card, manaCost);
                if (manaCost.isPaid()) {
                    manaCost = new ManaCost(Integer.toString(numX));
                    xPaid++;
                }
    
                if (AllZone.getInputControl().getInput() == this)
                    showMessage();
            }
    
            @Override
            public void selectButtonCancel() {
                stop();
                payment.cancelCost();
                AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
            }
    
            @Override
            public void selectButtonOK() {
                stop();
                payment.getCard().setXManaCostPaid(xPaid);
                payment.paidCost(costMana);
            }
    
        };
    
        return payX;
    }

    /**
     * <p>input_payMana.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param manaToAdd a int.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_payMana(final SpellAbility sa, final Cost_Payment payment, final CostMana costMana, final int manaToAdd) {
        final ManaCost manaCost;
    
        if (Phase.getGameBegins() == 1) {
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCost("0");
            } else {
                String mana = costMana.getManaToPay();
                manaCost = new ManaCost(mana);
                manaCost.increaseColorlessMana(manaToAdd);
            }
        } else {
            System.out.println("Is input_payMana ever called when the Game isn't in progress?");
            manaCost = new ManaCost(sa.getManaCost());
        }
    
        Input payMana = new Input() {
            private ManaCost mana = manaCost;
            private static final long serialVersionUID = 3467312982164195091L;
    
            private final String originalManaCost = costMana.getMana();
    
            private int phyLifeToLose = 0;
    
            private void resetManaCost() {
                mana = new ManaCost(originalManaCost);
                phyLifeToLose = 0;
            }
    
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                // prevent cards from tapping themselves if ability is a tapability, although it should already be tapped
                if (sa.getSourceCard().equals(card) && sa.isTapAbility()) {
                    return;
                }
    
                mana = Input_PayManaCostUtil.activateManaAbility(sa, card, mana);
    
                if (mana.isPaid())
                    done();
                else if (AllZone.getInputControl().getInput() == this)
                    showMessage();
            }
    
            @Override
            public void selectPlayer(Player player) {
                if (player.isHuman()) {
                    if (manaCost.payPhyrexian()) {
                        phyLifeToLose += 2;
                    }
    
                    showMessage();
                }
            }
    
            private void done() {
                Card source = sa.getSourceCard();
                if (phyLifeToLose > 0)
                    AllZone.getHumanPlayer().payLife(phyLifeToLose, source);
                source.setColorsPaid(mana.getColorsPaid());
                source.setSunburstValue(mana.getSunburst());
                resetManaCost();
                stop();
                
                if (costMana.hasNoXManaCost() || manaToAdd > 0){
                    payment.paidCost(costMana);
                }
                else{
                    source.setXManaCostPaid(0);
                    CostUtil.setInput(CostMana.input_payXMana(sa, payment, costMana, costMana.getXMana()));
                }
                    
            }
    
            @Override
            public void selectButtonCancel() {
                stop();
                resetManaCost();
                payment.cancelCost();
                AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
            }
    
            @Override
            public void showMessage() {
                ButtonUtil.enableOnlyCancel();
                String displayMana = mana.toString().replace("X", "").trim();
                AllZone.getDisplay().showMessage("Pay Mana Cost: " + displayMana);
    
                StringBuilder msg = new StringBuilder("Pay Mana Cost: " + displayMana);
                if (phyLifeToLose > 0) {
                    msg.append(" (");
                    msg.append(phyLifeToLose);
                    msg.append(" life paid for phyrexian mana)");
                }
    
                if (mana.containsPhyrexianMana()) {
                    msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
                }
    
                AllZone.getDisplay().showMessage(msg.toString());
                if (mana.isPaid())
                    done();
            }
        };
        return payMana;
    }
}
