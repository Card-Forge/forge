package forge.card.cost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.Card;
import forge.Player;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbility_Requirements;


/**
 * <p>Cost_Payment class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Cost_Payment {
    private Cost cost = null;
    private SpellAbility ability = null;
    private Card card = null;
    private SpellAbility_Requirements req = null;
    private boolean bCancel = false;
    private Map<CostPart, Boolean> paidCostParts = new HashMap<CostPart, Boolean>();
    
    /**
     * <p>Getter for the field <code>cost</code>.</p>
     *
     * @return a {@link forge.card.cost.Cost} object.
     */
    public Cost getCost() {
        return cost;
    }

    /**
     * <p>Getter for the field <code>ability</code>.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getAbility() {
        return ability;
    }

    /**
     * <p>Getter for the field <code>card</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getCard() {
        return card;
    }

    /**
     * <p>setRequirements.</p>
     *
     * @param reqs a {@link forge.card.spellability.SpellAbility_Requirements} object.
     */
    public void setRequirements(SpellAbility_Requirements reqs) {
        req = reqs;
    }

    public SpellAbility_Requirements getRequirements(){
        return req;
    }
    
    /**
     * <p>setCancel.</p>
     *
     * @param cancel a boolean.
     */
    public void setCancel(boolean cancel) {
        bCancel = cancel;
    }

    /**
     * <p>isCanceled.</p>
     *
     * @return a boolean.
     */
    public boolean isCanceled() {
        return bCancel;
    }

    /**
     * <p>Constructor for Cost_Payment.</p>
     *
     * @param cost a {@link forge.card.cost.Cost} object.
     * @param abil a {@link forge.card.spellability.SpellAbility} object.
     */
    public Cost_Payment(Cost cost, SpellAbility abil) {
        this.cost = cost;
        this.ability = abil;
        card = abil.getSourceCard();
        
        for(CostPart part : cost.getCostParts()){
        	paidCostParts.put(part, false);
        }
    }

    /**
     * <p>canPayAdditionalCosts.</p>
     *
     * @param cost a {@link forge.card.cost.Cost} object.
     * @param ability a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(Cost cost, SpellAbility ability) {
        if (cost == null)
            return true;

        Player activator = ability.getActivatingPlayer();
        final Card card = ability.getSourceCard();
        
        if(activator == null) {
            activator = card.getController();
        }
            
        
        for(CostPart part : cost.getCostParts()){
            if (!part.canPay(ability, card, activator, cost))
                return false;
        }
        
        return true;
    }

    public void setPaidManaPart(CostPart part, boolean bPaid){
    	paidCostParts.put(part, bPaid);
    }
    
    public void paidCost(CostPart part){
        setPaidManaPart(part, true);
        payCost();
    }
    
    public void cancelCost(){
        setCancel(true);
        req.finishPaying();
    }
    
    /**
     * <p>payCost.</p>
     *
     * @return a boolean.
     */
    public boolean payCost() {
        // Nothing actually ever checks this return value, is it needed?
        if (bCancel) {
            req.finishPaying();
            return false;
        }

        for(CostPart part : cost.getCostParts()){
        	// This portion of the cost is already paid for, keep moving
        	if (paidCostParts.get(part))
        		continue;
        	
        	if (!part.payHuman(ability, card, this))
        	    return false;
        }
        
        resetUndoList();
        req.finishPaying();
        return true;
    }

    /**
     * <p>isAllPaid.</p>
     *
     * @return a boolean.
     */
    public boolean isAllPaid() {
        for(CostPart part : paidCostParts.keySet()){
        	if (!paidCostParts.get(part))
        		return false;
        }
    	
        return true;
    }

    /**
     * <p>resetUndoList.</p>
     */
    public void resetUndoList() {
        for(CostPart part : paidCostParts.keySet()){
            if (part instanceof CostPartWithList){
                ((CostPartWithList)part).resetList();
            }
        }
    }

    /**
     * <p>cancelPayment.</p>
     */
    public void cancelPayment() {
        for(CostPart part : paidCostParts.keySet()){
        	if (paidCostParts.get(part) && part.isUndoable())
        		part.refund(card);
        }
        
        // Move this to CostMana
        AllZone.getHumanPlayer().getManaPool().unpaid(ability, false);
    }
    
    /**
     * <p>payComputerCosts.</p>
     *
     * @return a boolean.
     */
    public boolean payComputerCosts() {
        // canPayAdditionalCosts now Player Agnostic

        // Just in case it wasn't set, but honestly it shouldn't have gotten here without being set
        Player activator = AllZone.getComputerPlayer();
        ability.setActivatingPlayer(activator);
        
        Card source = ability.getSourceCard();
        ArrayList<CostPart> parts = cost.getCostParts();
        
        // Set all of the decisions before attempting to pay anything
        for(CostPart part : parts){
            if (!part.decideAIPayment(ability, source, this))
                return false;
        }
        
        for(CostPart part : parts){
            part.payAI(ability, ability.getSourceCard(), this);
        }
        return true;
    }

    /**
     * <p>changeCost.</p>
     */
    public void changeCost() {
        cost.changeCost(ability);
    }
}
