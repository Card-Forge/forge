package forge.card.cost;

import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Counters;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

public class CostUtil {
    static private Random r = new Random();
    static private double P1P1Percent = .1;
    static private double OtherPercent = .25;
        
    static public boolean checkSacrificeCost(Cost cost, Card source){
        for(CostPart part : cost.getCostParts()){
            if (part instanceof CostSacrifice){
                CostSacrifice sac = (CostSacrifice)part;
                
                String type = sac.getType();
                
                if (type.equals("CARDNAME"))
                    continue;
                
                CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
                typeList = typeList.getValidCards(type.split(","), source.getController(), source);
                if (ComputerUtil.getCardPreference(source, "SacCost", typeList) == null)
                    return false;
            }
        }
        return true;
    }
    
    static public boolean checkCreatureSacrificeCost(Cost cost, Card source){
        for(CostPart part : cost.getCostParts()){
            if (part instanceof CostSacrifice){
                CostSacrifice sac = (CostSacrifice)part;
                if (sac.getThis() && source.isCreature())
                    return false;
            }
        }
        return true;
    }
    
    static public boolean checkLifeCost(Cost cost, Card source, int remainingLife){
        for(CostPart part : cost.getCostParts()){
            if (part instanceof CostPayLife){
                CostPayLife payLife = (CostPayLife)part;
                if (AllZone.getComputerPlayer().getLife() - payLife.convertAmount() < remainingLife)
                    return false;
            }
        }
        return true;
    }
    
    static public boolean checkDiscardCost(Cost cost, Card source){
        for(CostPart part : cost.getCostParts()){
            if (part instanceof CostDiscard)
                return false;
        }
        return true;
    }
    
    static public boolean checkRemoveCounterCost(Cost cost, Card source){
        for(CostPart part : cost.getCostParts()){
            if (part instanceof CostRemoveCounter){
                CostRemoveCounter remCounter = (CostRemoveCounter)part;
                
                // A card has a 25% chance per counter to be able to pass through here
                // 4+ counters will always pass. 0 counters will never
                Counters type = remCounter.getCounter();
                double percent = type.name().equals("P1P1") ? P1P1Percent : OtherPercent;            
                int currentNum = source.getCounters(type);
                
                double chance = percent * (currentNum / part.convertAmount());
                if (chance <= r.nextFloat())
                    return false;
            }
        }

        return true;
    }
    
    static public boolean checkAddM1M1CounterCost(Cost cost, Card source){
        for(CostPart part : cost.getCostParts()){
            if (part instanceof CostPutCounter){
                CostPutCounter addCounter = (CostPutCounter)part;
                Counters type = addCounter.getCounter();
                
                if (type.equals(Counters.M1M1))
                    return false;
            }
        }

        return true;
    }
    
    static public boolean hasDiscardHandCost(Cost cost){
        for(CostPart part : cost.getCostParts()){
            if (part instanceof CostDiscard){
                CostDiscard disc = (CostDiscard)part;
                if (disc.getType().equals("Hand"))
                    return true;
            }
        }
        return false;
    }
    
    static public Integer determineAmount(CostPart part, Card source, SpellAbility ability, int maxChoice){
        String amount = part.getAmount();
        Integer c = part.convertAmount();
        if (c == null){
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")){
                c = CostUtil.chooseXValue(source, maxChoice);
            }
            else{
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        return c;
    }
    
    static public int chooseXValue(Card card, int maxValue){
        String chosen = card.getSVar("ChosenX");
        if (chosen.length() > 0){
            return AbilityFactory.calculateAmount(card, "ChosenX", null);
        }
        
        Integer[] choiceArray = new Integer[maxValue+1];
        for(int i = 0; i < choiceArray.length; i++){
            choiceArray[i] = i;
        }
        Object o = GuiUtils.getChoice(card.toString()  + " - Choose a Value for X", choiceArray);
        int chosenX = (Integer)o;
        card.setSVar("ChosenX", "Number$"+Integer.toString(chosenX));
        
        return chosenX;
    }
    
    /**
     * <p>setInput.</p>
     *
     * @param in a {@link forge.gui.input.Input} object.
     */
    static public void setInput(Input in) {
        // Just a shortcut..
        AllZone.getInputControl().setInput(in, true);
    }
}
