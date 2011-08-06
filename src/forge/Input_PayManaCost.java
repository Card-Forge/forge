
package forge;


//import java.util.*;

//pays the cost of a card played from the player's hand
//the card is removed from the players hand if the cost is paid
//CANNOT be used for ABILITIES
public class Input_PayManaCost extends Input {
    private static final long  serialVersionUID = 3467312982164195091L;
    
    private final String       originalManaCost;
    
    private final Card         originalCard;
    public ManaCost            manaCost;
    
    //private final ArrayList<Card> tappedLand = new ArrayList<Card>();
    private final SpellAbility spell;
   
    public Input_PayManaCost(SpellAbility sa) {
        originalManaCost = sa.getManaCost(); // Change
        originalCard = sa.getSourceCard();
            
        spell = sa;

        if(Phase.GameBegins == 1)  {
        	if(sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                if(spell.getAfterPayMana() != null) stopSetNext(spell.getAfterPayMana());
                else {
                	manaCost = new ManaCost("0"); 
                    AllZone.Stack.add(spell);
                }
        	} else {
        		manaCost = AllZone.GameAction.GetSpellCostChange(sa, new ManaCost(originalManaCost)); 
        	}    	
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
        //this is a hack, to prevent lands being able to use mana to pay their own abilities from cards like
        //Kher Keep, Pendelhaven, Blinkmoth Nexus, and Mikokoro, Center of the Sea, .... 
        /*if (originalCard.equals(card) && !card.getName().equals("Oboro, Palace in the Clouds"))*/
        if(originalCard.equals(card) && spell.isTapAbility()) {
            return;
            //originalCard.tap();
        }
        boolean canUse = false;
        for(Ability_Mana am:card.getManaAbility())
            canUse |= am.canPlay();
        manaCost = Input_PayManaCostUtil.tapCard(card, manaCost);
        showMessage();
        
        if(manaCost.isPaid()) done();
    }
    
    private void done() {
    	if(spell.getSourceCard().isCopiedSpell()) {
            if(spell.getAfterPayMana() != null) {
            	stopSetNext(spell.getAfterPayMana());            	
            } else stopSetNext(new ComputerAI_StackNotEmpty());
    	} else {
        AllZone.ManaPool.clearPay(false);
        resetManaCost();
        
        //if tap ability, tap card
        if(spell.isTapAbility()) originalCard.tap();
        if(spell.isUntapAbility()) originalCard.untap();
        
        //this seems to remove a card if it is in the player's hand
        //and trys to remove abilities, but no error messsage is generated
        AllZone.Human_Hand.remove(originalCard);
        
        if(spell.getAfterPayMana() != null) stopSetNext(spell.getAfterPayMana());
        else {
        	AllZone.Stack.add(spell);
            stopSetNext(new ComputerAI_StackNotEmpty());
        }
    }
    }
    
    @Override
    public void selectButtonCancel() {
        resetManaCost();
        AllZone.ManaPool.unpaid();
        AllZone.Human_Play.updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap
        
        stop();
    }
    
    @Override
    public void showMessage() {
        //if(manaCost.toString().equals(""))
        
        ButtonUtil.enableOnlyCancel();
        AllZone.Display.showMessage("Pay Mana Cost: " + manaCost.toString());
        if(manaCost.isPaid() && !new ManaCost(originalManaCost).isPaid()) done(); 
    }
}
