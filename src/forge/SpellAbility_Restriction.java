package forge;

public class SpellAbility_Restriction {
	// A class for handling SpellAbility Restrictions. These restrictions include: 
	// Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player, 
	// Threshold, Metalcraft, LevelRange, etc
	// Each value will have a default, that can be overridden (mostly by AbilityFactory)
	// The CanPlay function will use these values to determine if the current game state is ok with these restrictions
	
	// default values for Sorcery speed abilities
	private String activateZone = Constant.Zone.Play;

	public void setActivateZone(String zone){
		activateZone = zone;
	}
	
	public String getActivateZone(){
		return activateZone;
	}
	
	private boolean bSorcerySpeed = false;
	
	public void setSorcerySpeed(boolean bSpeed){
		bSorcerySpeed = bSpeed;
	}
	
	public boolean getSorcerySpeed(){
		return bSorcerySpeed;
	}
	
	private boolean bAnyPlayer = false;
	
	public void setAnyPlayer(boolean anyPlayer){
		bAnyPlayer = anyPlayer;
	}
	
	public boolean getAnyPlayer(){
		return bAnyPlayer;
	}
	
	private boolean bPlayerTurn = false;
	
	public void setPlayerTurn(boolean bTurn){
		bPlayerTurn = bTurn;
	}
	
	public boolean getPlayerTurn(){
		return bPlayerTurn;
	}
	
	int activationLimit = -1;
	int numberTurnActivations = 0;
	
	public void setActivationLimit(int limit){
		activationLimit = limit;
	}
	
	public void abilityActivated(){
		numberTurnActivations++;
	}
	
	public void resetTurnActivations(){
		numberTurnActivations = 0;
	}
	
	/*
	 * Restrictions of the future

	ArrayList<String> activatePhases = new ArrayList<String>();

	boolean bHasThreshold = false;
	boolean bHasMetalcraft = false;
	int levelMin = 0;
	int levelMax = 0;
	youControl
	oppControl
	*/
	
	SpellAbility_Restriction(){	}

	public boolean canPlay(Card c, SpellAbility sa){
		if (!AllZone.getZone(c).getZone().equals(activateZone))
			return false;
		
		Player activater = sa.getActivatingPlayer();
		if (activater == null){
			c.getController();
			System.out.println(c.getName() + " Did not have activater set in SpellAbility_Restriction.canPlay()");
		}
		
		if (bSorcerySpeed && !Phase.canCastSorcery(activater))
			return false;
		
		if (bPlayerTurn && !AllZone.GameAction.isPlayerTurn(activater))
			return false;
		
		if (!bAnyPlayer && !activater.equals(c.getController()))
			return false;
		
		if (activationLimit != -1 && numberTurnActivations >= activationLimit)
			return false;
			
		return true;
	}
}
