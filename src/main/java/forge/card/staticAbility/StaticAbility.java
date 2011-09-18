package forge.card.staticAbility;

import forge.*;
import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactory;

import java.util.HashMap;
import java.util.Map;

public class StaticAbility {

    private Card hostCard = null;

    private HashMap<String, String> mapParams = new HashMap<String, String>();
    
    protected boolean temporarilySuppressed = false;
    protected boolean suppressed = false;

    /**
     * <p>getHostCard.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getHostCard() {
        return hostCard;
    }

    /**
     * <p>Getter for the field <code>mapParams</code>.</p>
     *
     * @return a {@link java.util.HashMap} object.
     */
    public HashMap<String, String> getMapParams() {
        return mapParams;
    }

    //*******************************************************

    /**
     * <p>Getter for the field <code>mapParams</code>.</p>
     *
     * @param abString a {@link java.lang.String} object.
     * @param hostCard a {@link forge.Card} object.
     * @return a {@link java.util.HashMap} object.
     */
    public HashMap<String, String> getMapParams(String abString, Card hostCard) {
        HashMap<String, String> mapParameters = new HashMap<String, String>();

        if (!(abString.length() > 0))
            throw new RuntimeException("StaticEffectFactory : getAbility -- abString too short in " + hostCard.getName() + ": [" + abString + "]");

        String a[] = abString.split("\\|");

        for (int aCnt = 0; aCnt < a.length; aCnt++)
            a[aCnt] = a[aCnt].trim();

        if (!(a.length > 0))
            throw new RuntimeException("StaticEffectFactory : getAbility -- a[] too short in " + hostCard.getName());

        for (int i = 0; i < a.length; i++) {
            String aa[] = a[i].split("\\$");

            for (int aaCnt = 0; aaCnt < aa.length; aaCnt++)
                aa[aaCnt] = aa[aaCnt].trim();

            if (aa.length != 2) {
                StringBuilder sb = new StringBuilder();
                sb.append("StaticEffectFactory Parsing Error: Split length of ");
                sb.append(a[i]).append(" in ").append(hostCard.getName()).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParameters.put(aa[0], aa[1]);
        }

        return mapParameters;
    }
    
    // In which layer should the ability be applied (for continuous effects only)
    public int getLayer() {
        
        if(!mapParams.get("Mode").equals("Continuous")) {
            return 0;
        }
        
        if(mapParams.containsKey("AddType") || mapParams.containsKey("RemoveType") || mapParams.containsKey("RemoveCardType")
                || mapParams.containsKey("RemoveSubType") || mapParams.containsKey("RemoveSuperType"))
            return 4;
        
        if(mapParams.containsKey("AddColor") || mapParams.containsKey("RemoveColor") || mapParams.containsKey("SetColor"))
            return 5;
        
        if(mapParams.containsKey("RemoveAllAbilities"))
            return 6; //Layer 6
        
        if(mapParams.containsKey("AddKeyword") || mapParams.containsKey("AddAbility")
                || mapParams.containsKey("AddTrigger") || mapParams.containsKey("RemoveTriggers")
                || mapParams.containsKey("RemoveKeyword"))
            return 7; //Layer 6 (dependent)
        
    	if(mapParams.containsKey("CharacteristicDefining"))
    		return 8; //Layer 7a
    	
    	if(mapParams.containsKey("AddPower") || mapParams.containsKey("AddToughness")  
    			|| mapParams.containsKey("SetPower") || mapParams.containsKey("SetToughness"))
    		return 9; //This is the collection of 7b and 7c
    	
        if(mapParams.containsKey("AddHiddenKeyword"))
            return 10; // rules change

    	// Layer 1, 2 & 3 are not supported
    	
    	return 0;
    }
    
    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        if (mapParams.containsKey("Description") && !isSuppressed()) {
            return mapParams.get("Description").replace("CARDNAME", hostCard.getName());
        } else return "";
    }
    
    //main constructor
    public StaticAbility(String params, Card host) {
        mapParams = getMapParams(params, host);
        hostCard = host;
    }
    
    public StaticAbility(HashMap<String, String> params, Card host) {
        mapParams = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            mapParams.put(entry.getKey(), entry.getValue());
        }
        hostCard = host;
    }
    
    //apply the ability if it has the right mode
    public void applyAbility(String mode) {
    	
    	//don't apply the ability if it hasn't got the right mode
    	if (!mapParams.get("Mode").equals(mode))
    		return;
    	
    	if (isSuppressed() || !checkConditions())
    		return;
    	
    	if (mode.equals("Continuous"))
    		StaticAbility_Continuous.applyContinuousAbility(this);
    }
    
    public boolean checkConditions() {
    	Player controller = hostCard.getController();
    	
    	Zone effectZone = Zone.Battlefield; //default
    	
    	if (mapParams.containsKey("EffectZone"))
    		effectZone = Zone.smartValueOf(mapParams.get("EffectZone"));
    	
    	if( effectZone != null && !AllZone.getZone(hostCard).getZoneType().equals(effectZone))
    		return false;
    	
    	if(mapParams.containsKey("Threshold") && !controller.hasThreshold())
    		return false;
    	
    	if(mapParams.containsKey("Hellbent") && !controller.hasHellbent())
    		return false;
    	
    	if(mapParams.containsKey("Metalcraft") && !controller.hasMetalcraft())
    		return false;
    	
    	if (mapParams.containsKey("PlayerTurn") && !AllZone.getPhase().isPlayerTurn(controller))
    		return false;
    	
    	if (mapParams.containsKey("OpponentTurn") && !AllZone.getPhase().isPlayerTurn(controller.getOpponent()))
    		return false;
    	
    	if (mapParams.containsKey("TopCardOfLibraryIs")) {
    		Card topCard = controller.getCardsIn(Zone.Library).get(0);
    		if (!topCard.isValidCard(mapParams.get("TopCardOfLibraryIs").split(","), controller, hostCard))
    			return false;
    	}
    		
    	
    	/*if(mapParams.containsKey("isPresent")) {
    		String isPresent = mapParams.get("isPresent");
            CardList list = AllZoneUtil.getCardsInPlay();

            list = list.getValidCards(isPresent.split(","), controller, hostCard);
            
    	}*/
    	
        if (mapParams.containsKey("CheckSVar")) {
            int sVar = AbilityFactory.calculateAmount(hostCard, mapParams.get("CheckSVar"), null);
            String comparator = "GE1";
            if (mapParams.containsKey("SVarCompare")) 
            	comparator = mapParams.get("SVarCompare");
            String svarOperator = comparator.substring(0, 2);
            String svarOperand = comparator.substring(2);
            int operandValue = AbilityFactory.calculateAmount(hostCard, svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue))
            	return false;
        }
    		
    	
    	return true;
    }
    
    public void setTemporarilySuppressed(boolean supp) {
        temporarilySuppressed = supp;
    }
    
    public boolean isSuppressed() {
        return (suppressed || temporarilySuppressed);
    }

}//end class StaticEffectFactory
