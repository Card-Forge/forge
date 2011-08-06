package forge;

public class SpellAbility_Restriction {
	// A class for handling SpellAbility Restrictions. These restrictions include: 
	// Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player, 
	// Threshold, Metalcraft, LevelRange, etc
	// Each value will have a default, that can be overridden (mostly by AbilityFactory)
	// The CanPlay function will use these values to determine if the current game state is ok with these restrictions
	
	// default values for Sorcery speed abilities
	String activateZone = Constant.Zone.Play;

	public void setActivateZone(String zone){
		activateZone = zone;
	}
	
	public String getActivateZone(){
		return activateZone;
	}
	
	/*
	 * Some restrictions to come...
	boolean bInstantActivation = false;
	ArrayList<String> activatePhases = new ArrayList<String>();
	int amountPerTurn = -1;
	int activatedPerTurn = 0;
	boolean bAnyPlayer = false;
	boolean bHasThreshold = false;
	boolean bHasMetalcraft = false;
	int levelMin = 0;
	int levelMax = 0;
	boolean bThreshold = false;
	boolean bMetalcraft = false;
	*/
	
	SpellAbility_Restriction(){	}

	//SpellAbility_Restriction(String zone, Phase phase, boolean bIsInstantSpeed, int perTurn, String activatingPlayer){}
	
	public boolean canPlay(Card c){
		if (!AllZone.getZone(c).getZone().equals(activateZone))
			return false;
		
		
		return true;
	}
}
