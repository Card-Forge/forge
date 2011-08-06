
package forge.gui.input;

import forge.*;
import forge.card.mana.ManaCost;
import forge.card.mana.Mana_PartPhyrexian;
import forge.card.spellability.SpellAbility;

import java.util.ArrayList;

//pays the cost of a card played from the player's hand
//the card is removed from the players hand if the cost is paid
//CANNOT be used for ABILITIES
public class Input_PayManaCost extends Input {
	// anything that uses this should be converted to Ability_Cost
    private static final long  serialVersionUID = 3467312982164195091L;
    
    private final String       originalManaCost;
    
    private final Card         originalCard;
    public ManaCost            manaCost;
    
    private final SpellAbility spell;
    
    private boolean skipStack;

    private int phyLifeToLose = 0;
   
    public Input_PayManaCost(SpellAbility sa,boolean noStack) {
    	skipStack = noStack;
    	originalManaCost = sa.getManaCost(); // Change
        originalCard = sa.getSourceCard();
            
        spell = sa;

        if(Phase.getGameBegins() == 1)  {
        	if(sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                if(spell.getAfterPayMana() != null) stopSetNext(spell.getAfterPayMana());
                else {
                	manaCost = new ManaCost("0"); 
                    AllZone.Stack.add(spell);
                }
        	} else {
        		manaCost = AllZone.GameAction.getSpellCostChange(sa, new ManaCost(originalManaCost)); 
        	}    	
        }
        else
        {
        	manaCost = new ManaCost(sa.getManaCost());
        }
    }
    
    public Input_PayManaCost(SpellAbility sa) {
        originalManaCost = sa.getManaCost(); // Change
        originalCard = sa.getSourceCard();
            
        spell = sa;

        if(Phase.getGameBegins() == 1)  {
        	if(sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                if(spell.getAfterPayMana() != null) stopSetNext(spell.getAfterPayMana());
                else {
                	manaCost = new ManaCost("0"); 
                    AllZone.Stack.add(spell);
                }
        	} else {
        		manaCost = AllZone.GameAction.getSpellCostChange(sa, new ManaCost(originalManaCost)); 
        	}    	
        }
        else
        {
        	manaCost = new ManaCost(sa.getManaCost());
        }
    }
   
    private void resetManaCost() {
        manaCost = new ManaCost(originalManaCost);
        phyLifeToLose = 0;
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        //this is a hack, to prevent lands being able to use mana to pay their own abilities from cards like
        //Kher Keep, Pendelhaven, Blinkmoth Nexus, and Mikokoro, Center of the Sea, .... 

        if(originalCard.equals(card) && spell.isTapAbility()) {
        	// I'm not sure if this actually prevents anything that wouldn't be handled by canPlay below
            return;
        }

        manaCost = Input_PayManaCostUtil.activateManaAbility(spell, card, manaCost);
        
        // only show message if this is the active input
        if (AllZone.InputControl.getInput() == this)
        	showMessage();
        
        if(manaCost.isPaid()) {
        	originalCard.setSunburstValue(manaCost.getSunburst());
        	done();
        }
    }

    @Override
    public void selectPlayer(Player player)
    {
        showMessage();
        if(player.equals(AllZone.HumanPlayer))
        {
            if(manaCost.payPhyrexian())
            {
                phyLifeToLose += 2;
            }

            if(manaCost.isPaid()) {
                System.out.println("Phyrexian Mana: Pay " + phyLifeToLose);
                resetManaCost();

                done();
            }
        }

    }
    
    private void done() {
        if(phyLifeToLose > 0)
            AllZone.HumanPlayer.payLife(phyLifeToLose,originalCard);
		if (spell.getSourceCard().isCopiedSpell()) {
			if (spell.getAfterPayMana() != null) {
				stopSetNext(spell.getAfterPayMana());
			} else
				AllZone.InputControl.resetInput();
		} else {
			AllZone.ManaPool.clearPay(spell, false);
			resetManaCost();

			// if tap ability, tap card
			if (spell.isTapAbility())
				originalCard.tap();
			if (spell.isUntapAbility())
				originalCard.untap();

			// if this is a spell, move it to the Stack ZOne

			if (spell.isSpell())	// already checked for if its a copy
				AllZone.GameAction.moveToStack(originalCard);

			if (spell.getAfterPayMana() != null)
				stopSetNext(spell.getAfterPayMana());
			else {
				if(skipStack)
				{
					spell.resolve();
				}
				else
				{
					AllZone.Stack.add(spell);
				}
				AllZone.InputControl.resetInput();
			}
		}
    }
    
    @Override
    public void selectButtonCancel() {
        resetManaCost();
        AllZone.ManaPool.unpaid(spell, true);
        AllZone.Human_Battlefield.updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap
        
        stop();
    }
    
    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyCancel();

        StringBuilder msg = new StringBuilder("Pay Mana Cost: " + manaCost.toString());
        if(phyLifeToLose > 0)
        {
            msg.append(" (");
            msg.append(phyLifeToLose);
            msg.append(" life paid for phyrexian mana)");
        }

        AllZone.Display.showMessage(msg.toString());
        if(manaCost.isPaid() && !new ManaCost(originalManaCost).isPaid()) {
        	originalCard.setSunburstValue(manaCost.getSunburst());
        	done(); 
        }


    }
}
