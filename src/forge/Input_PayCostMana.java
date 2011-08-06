package forge;

// todo: should we move the cost for this to the bottom of CostPayment? The similar names might be confusing.

public class Input_PayCostMana extends Input {
    private static final long  serialVersionUID = 3467312982164195091L;
    
    private final String       originalManaCost;
    private Cost_Payment		costPayment;
    private ManaCost            manaCost;
    private SpellAbility 		sa;
    
    public Input_PayCostMana(Cost_Payment payment) {
    	costPayment = payment;
        originalManaCost = costPayment.getCost().getMana();
        sa = payment.getAbility();

        if(Phase.GameBegins == 1)  {
        	if(sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                	manaCost = new ManaCost("0"); 
        	} else {
    		    String mana = payment.getCost().getMana();
        		manaCost = new ManaCost(mana);//AllZone.GameAction.getSpellCostChange(sa, new ManaCost(mana)); 
        	}    							//^ already factored into costPayment.
        }
        else
        {
        	manaCost = new ManaCost(sa.getManaCost());
        }
    }
   
    private void resetManaCost() {
        manaCost = new ManaCost(originalManaCost);
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
    	// prevent cards from tapping themselves if ability is a tapability, although it should already be tapped
        if(sa.getSourceCard().equals(card) && sa.isTapAbility()) {
            return;
        }
        boolean canUse = false;
        for(Ability_Mana am:card.getManaAbility())
            canUse |= am.canPlay();
        manaCost = Input_PayManaCostUtil.tapCard(card, manaCost);
        showMessage();
        
        if(manaCost.isPaid()) 
        	done();
    }
    
    private void done() {
    	resetManaCost();
    	costPayment.setPayMana(true);
    	stop();
    	costPayment.payCost();
    }
    
    @Override
    public void selectButtonCancel() {
        resetManaCost();
        costPayment.setCancel(true);
        costPayment.payCost();
        AllZone.Human_Play.updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap
        stop();
    }
    
    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyCancel();
        AllZone.Display.showMessage("Pay Mana Cost: " + manaCost.toString());
        if(manaCost.isPaid() /*&& !new ManaCost(originalManaCost).isPaid()*/) 
        	done(); 
    }
}
