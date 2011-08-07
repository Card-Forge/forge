package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observer;
import java.util.Stack;

import com.esotericsoftware.minlog.Log;

import forge.card.spellability.SpellAbility;
import forge.card.spellability.Spell_Permanent;

public class Phase extends MyObservable
{
	private int phaseIndex;
	private int turn;

	private static int	 	GameBegins = 0;
    private static int	   StormCount;
    static int	   PlayerSpellCount;
    static int	   PlayerCreatureSpellCount;   
    static int	   ComputerSpellCount;
    static int	   ComputerCreatureSpellCount;

    private Stack<Player> extraTurns = new Stack<Player>();
    
	private int extraCombats;
	
	private int nCombatsThisTurn;
	
    private Player playerTurn = AllZone.HumanPlayer;
    public boolean isPlayerTurn(Player player) {
        return playerTurn.isPlayer(player);
    }
    
    public void setPlayerTurn(Player s) {
    	playerTurn = s;
    }
    
    public Player getPlayerTurn() {
    	return playerTurn;
    }
    
    // priority player
    
    private Player pPlayerPriority = AllZone.HumanPlayer;
    public Player getPriorityPlayer() {
    	return pPlayerPriority;
    }
    
    public void setPriorityPlayer(Player p) {
    	pPlayerPriority = p;
    }
    
    private Player pFirstPriority = AllZone.HumanPlayer;
    public Player getFirstPriority() {
    	return pFirstPriority;
    }
    
    public void setFirstPriority(Player p) {
    	pFirstPriority = p;
    }
    
    public void setPriority(Player p) {
        if(AllZone.Stack != null)
            AllZone.Stack.chooseOrderOfSimultaneousStackEntryAll();

    	pFirstPriority = p;
    	pPlayerPriority = p;
    }
    
    public void resetPriority() {
    	setPriority(playerTurn);
    }
    
	private boolean bPhaseEffects = true;
    public boolean doPhaseEffects() {
    	return bPhaseEffects;
    }
    
    public void setPhaseEffects(boolean b) {
    	bPhaseEffects = b;
    } 
    
    private boolean bSkipPhase = true;
    public boolean doSkipPhase() {
    	return bSkipPhase;
    }
    
    public void setSkipPhase(boolean b) {
    	bSkipPhase = b;
    } 
    
    private boolean bCombat = false;
    public boolean inCombat() {
    	return bCombat;
    }
    
    public void setCombat(boolean b) {
    	bCombat = b;
    } 
    
    private boolean bRepeat = false;
    public void repeatPhase() {
    	bRepeat = true;
    } 
    
	private String phaseOrder[] = {
			Constant.Phase.Untap,
			Constant.Phase.Upkeep,
			Constant.Phase.Draw, 
			Constant.Phase.Main1,
			Constant.Phase.Combat_Begin,
			Constant.Phase.Combat_Declare_Attackers,
			Constant.Phase.Combat_Declare_Attackers_InstantAbility,
			Constant.Phase.Combat_Declare_Blockers,
			Constant.Phase.Combat_Declare_Blockers_InstantAbility,
			Constant.Phase.Combat_FirstStrikeDamage,
			Constant.Phase.Combat_Damage,
			Constant.Phase.Combat_End,
			Constant.Phase.Main2,
			Constant.Phase.End_Of_Turn,
			Constant.Phase.Cleanup
	};
    
    public Phase() {
        reset();
    }
    
    public void reset() {
        turn = 1;
        playerTurn = AllZone.HumanPlayer;
        resetPriority();
        bPhaseEffects = true;
        needToNextPhase = false;
        setGameBegins(0);
        phaseIndex = 0;
        extraTurns.clear();
        nCombatsThisTurn = 0;
        extraCombats = 0;
        bCombat = false;
        bRepeat = false;
        this.updateObservers();
    }
    
    public void turnReset(){
    	setStormCount(0);
        PlayerSpellCount = 0;
        PlayerCreatureSpellCount = 0;   
        ComputerSpellCount = 0;
        ComputerCreatureSpellCount = 0;
        playerTurn.setNumLandsPlayed(0);
    }

	public void handleBeginPhase(){
		AllZone.Phase.setPhaseEffects(false);
		// Handle effects that happen at the beginning of phases
        final String phase = AllZone.Phase.getPhase();
        final Player turn = AllZone.Phase.getPlayerTurn();
        AllZone.Phase.setSkipPhase(true);

        if(phase.equals(Constant.Phase.Untap)) {
            PhaseUtil.handleUntap();
	    }
	    else if(phase.equals(Constant.Phase.Upkeep)){
	    	PhaseUtil.handleUpkeep();
	    }
	    
	    else if(phase.equals(Constant.Phase.Draw)){
	    	PhaseUtil.handleDraw();
	    }
        
	    else if(phase.equals(Constant.Phase.Combat_Begin)){
	    	PhaseUtil.verifyCombat();
	    }
        
	    else if (phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)){
            if(inCombat()) {
            	PhaseUtil.handleDeclareAttackers();
            }
            else
            	AllZone.Phase.setNeedToNextPhase(true);
	    }
        
        // we can skip AfterBlockers and AfterAttackers if necessary
	    else if(phase.equals(Constant.Phase.Combat_Declare_Blockers)){
            if(inCombat()) {
	            PhaseUtil.verifyCombat();
            }
            else
            	AllZone.Phase.setNeedToNextPhase(true);
        }
        
	    else if (phase.equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
	    	// After declare blockers are finished being declared mark them blocked and trigger blocking things
            if(!inCombat()) 
            	AllZone.Phase.setNeedToNextPhase(true);
            else{
            	PhaseUtil.handleDeclareBlockers();
            }
	    }
        
	    else if (phase.equals(Constant.Phase.Combat_FirstStrikeDamage)){
	    	if(!inCombat())
	    		AllZone.Phase.setNeedToNextPhase(true);
	    	else{
	    		AllZone.Combat.verifyCreaturesInPlay();

				// no first strikers, skip this step
				if (!AllZone.Combat.setAssignedFirstStrikeDamage())		
					AllZone.Phase.setNeedToNextPhase(true);
				
				else{
			    	if (!AllZone.GameInfo.isPreventCombatDamageThisTurn())
			    		 Combat.dealAssignedDamage();
			        
			        AllZone.GameAction.checkStateEffects();
			        CombatUtil.showCombat();
				}
	    	}
	    }
	    	
	    else if (phase.equals(Constant.Phase.Combat_Damage)){
	    	if(!inCombat())
	    		AllZone.Phase.setNeedToNextPhase(true);
	    	else{
	    		AllZone.Combat.verifyCreaturesInPlay();
	    		
		        AllZone.Combat.setAssignedDamage();
	            
	    		if (!AllZone.GameInfo.isPreventCombatDamageThisTurn())
	    			Combat.dealAssignedDamage();
	    			
	    		AllZone.GameAction.checkStateEffects();
		        CombatUtil.showCombat();
	    	}
	    }
        
	    else if (phase.equals(Constant.Phase.Combat_End))
        {
	    	// End Combat always happens
			AllZone.EndOfCombat.executeUntil();
			AllZone.EndOfCombat.executeAt();
        }

	    else if(phase.equals(Constant.Phase.End_Of_Turn)) {
	    	AllZone.EndOfTurn.executeAt();
        }
        
	    else if(phase.equals(Constant.Phase.Cleanup)){
	    	AllZone.Phase.getPlayerTurn().setAssignedDamage(0);
	    	
	    	//Reset Damage received map
	    	CardList list = AllZoneUtil.getCardsInPlay();
			for(Card c:list) {
				c.resetPreventNextDamage();
				c.resetReceivedDamageFromThisTurn();
				c.resetDealtDamageToThisTurn();
			}
			AllZone.HumanPlayer.resetPreventNextDamage();
			AllZone.ComputerPlayer.resetPreventNextDamage();

	    	AllZone.EndOfTurn.executeUntil();
	    	CardList cHand = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
	    	CardList hHand = AllZoneUtil.getPlayerHand(AllZone.HumanPlayer);
	    	for(Card c:cHand) c.setDrawnThisTurn(false);
	    	for(Card c:hHand) c.setDrawnThisTurn(false);
	    	AllZone.HumanPlayer.resetNumDrawnThisTurn();
	    	AllZone.ComputerPlayer.resetNumDrawnThisTurn();
	    }
        
        if (!AllZone.Phase.isNeedToNextPhase()){
            // Run triggers if phase isn't being skipped
            HashMap<String,Object> runParams = new HashMap<String,Object>();
    		runParams.put("Phase", phase);
    		runParams.put("Player", turn);
    		AllZone.TriggerHandler.runTrigger("Phase", runParams);
            
        }

        //This line fixes Combat Damage triggers not going off when they should
        AllZone.Stack.unfreezeStack();

        if(!phase.equals(Constant.Phase.Untap)) //Nobody recieves priority during untap
		    resetPriority();
	}
	
    public void nextPhase() {
        //experimental, add executeCardStateEffects() here:
        for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
            Command com = GameActionUtil.commands.get(effect);
            com.execute();
        }
        
        GameActionUtil.executeCardStateEffects();
        
        needToNextPhase = false;

        // If the Stack isn't empty why is nextPhase being called?
        if(AllZone.Stack.size() != 0) {
        	Log.debug("Phase.nextPhase() is called, but Stack isn't empty.");
            return;
        }
        this.bPhaseEffects = true;
		if(!AllZoneUtil.isCardInPlay("Upwelling"))
			AllZone.ManaPool.clearPool();
        
        if (getPhase().equals(Constant.Phase.Combat_Declare_Attackers)) {
        	AllZone.Stack.unfreezeStack();
        	nCombatsThisTurn++;
        } 
        else if (getPhase().equals(Constant.Phase.Untap)) {
        	nCombatsThisTurn = 0;
        }
        
        if (getPhase().equals(Constant.Phase.Combat_End)) {
            AllZone.Combat.reset();
            AllZone.Display.showCombat("");
        	resetAttackedThisCombat(getPlayerTurn());
        	this.bCombat = false;
        }
        
        if (phaseOrder[phaseIndex].equals(Constant.Phase.Cleanup))
        	if (!bRepeat)
        		AllZone.Phase.setPlayerTurn(handleNextTurn());
        
        if (is(Constant.Phase.Combat_Declare_Blockers)){        	
         	AllZone.Stack.unfreezeStack();
        }
        
        if (is(Constant.Phase.Combat_End) && extraCombats > 0){
        	// TODO: ExtraCombat needs to be changed for other spell/abilities that give extra combat
        	// can do it like ExtraTurn stack ExtraPhases

        	Player player = getPlayerTurn();
        	Player opp = player.getOpponent();

        	bCombat = true;
        	extraCombats--;
        	AllZone.Combat.reset();
        	AllZone.Combat.setAttackingPlayer(player);
        	AllZone.Combat.setDefendingPlayer(opp);
        	phaseIndex = findIndex(Constant.Phase.Combat_Declare_Attackers);
        }  
        else {
        	if (!bRepeat){	// for when Cleanup needs to repeat itself
	            phaseIndex++;
	            phaseIndex %= phaseOrder.length;
        	}
        	else
        		bRepeat = false;
        }
        
        // **** Anything BELOW Here is actually in the next phase. Maybe move this to handleBeginPhase
        if(getPhase().equals(Constant.Phase.Untap)){
            turn++;
        }

        // When consecutively skipping phases (like in combat) this section pushes through that block
        this.updateObservers();
        if(AllZone.Phase != null && AllZone.Phase.isNeedToNextPhase()) {
                AllZone.Phase.setNeedToNextPhase(false);
                AllZone.Phase.nextPhase();
        }
    }
    
    private Player handleNextTurn() {
    	Player nextTurn = extraTurns.isEmpty() ?  getPlayerTurn().getOpponent() : extraTurns.pop();

        AllZone.resetZoneMoveTracking();
    	
    	return skipTurnTimeVault(nextTurn);
	}

	private Player skipTurnTimeVault(Player turn) {
        //time vault:
		CardList vaults = AllZoneUtil.getPlayerCardsInPlay(turn, "Time Vault");
		vaults = vaults.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isTapped();
            }
        });
		
		if (vaults.size() > 0){
            final Card crd = vaults.get(0);

            if(turn.isHuman()) {
            	if(GameActionUtil.showYesNoDialog(crd, "Untap " + crd + "?")) {
            		crd.untap();
            		turn = extraTurns.isEmpty() ?  turn.getOpponent() : extraTurns.pop();
            	}
            }
            else{
            	// TODO: Should AI skip his turn for time vault?
            }
		}
    	return turn;
	}

	public synchronized boolean is(String phase, Player player) {
        return getPhase().equals(phase) && getPlayerTurn().isPlayer(player);
    }
	
	public synchronized boolean is(String phase) {
        return (getPhase().equals(phase));
    }
	
	public boolean isAfter(String phase) {
		return phaseIndex > findIndex(phase);
	}
	
	public boolean isBefore(String phase) {
		return phaseIndex < findIndex(phase);
	}
    
    private int findIndex(String phase) {
        for(int i = 0; i < phaseOrder.length; i++) {
            if(phase.equals(phaseOrder[i])) 
            	return i;
        }
        throw new RuntimeException("Phase : findIndex() invalid argument, phase = " + phase);
    }
    
    public String getPhase() {
    	return phaseOrder[phaseIndex];
    }
    
    public int getTurn() {
        return turn;
    }

    public Player getNextTurn(){
    	if (extraTurns.isEmpty())
    		return getPlayerTurn().getOpponent();
    	
    	return extraTurns.peek();
    }
    
    public boolean isNextTurn(Player pl){
    	Player next = getNextTurn();
    	return (pl.equals(next));
    }
    
    public void addExtraTurn(Player player) {
    	// use a stack to handle extra turns, make sure the bottom of the stack restores original turn order
    	if (extraTurns.isEmpty())
    		extraTurns.push(getPlayerTurn().getOpponent());
    	
    	extraTurns.push(player);
    }
    
    public void skipTurn(Player player) {
    	// skipping turn without having extras is equivalent to giving your opponent an extra turn
    	if (extraTurns.isEmpty())
    		addExtraTurn(player.getOpponent());
    	else{
    		int pos = extraTurns.lastIndexOf(player);
    		if (pos == -1)
    			addExtraTurn(player.getOpponent());
    		else
    			extraTurns.remove(pos);
    	}
    }
    
    public void addExtraCombat() {
    	// Extra combats can only happen 
    	extraCombats++;
    }

    public boolean isFirstCombat() {
    	return (nCombatsThisTurn == 1);
    }
    
    public void resetAttackedThisCombat(Player player) {
        // resets the status of attacked/blocked this phase
        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);

        list = list.getType("Creature");
        
        for(int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if(c.getCreatureAttackedThisCombat()) c.setCreatureAttackedThisCombat(false);
            if(c.getCreatureBlockedThisCombat()) c.setCreatureBlockedThisCombat(false);
            
            if(c.getCreatureGotBlockedThisCombat()) c.setCreatureGotBlockedThisCombat(false);
            
            AllZone.GameInfo.setAssignedFirstStrikeDamageThisCombat(false);
            AllZone.GameInfo.setResolvedFirstStrikeDamageThisCombat(false);
        }
    }

    public void passPriority(){
    	Player actingPlayer = getPriorityPlayer();
    	Player lastToAct = getFirstPriority();
    	
    	// actingPlayer is the player who may act
    	// the lastToAct is the player who gained Priority First in this segment of Priority
    	
    	if (lastToAct.equals(actingPlayer)){
    		// pass the priority to other player
    		setPriorityPlayer(actingPlayer.getOpponent());
    		AllZone.InputControl.resetInput();
            AllZone.Stack.chooseOrderOfSimultaneousStackEntryAll();
    	}
    	else{
    		if (AllZone.Stack.size() == 0){
    			// end phase
    			needToNextPhase = true;
    			pPlayerPriority = getPlayerTurn();	// this needs to be set early as we exit the phase
    		}
    		else{
                if(!AllZone.Stack.hasSimultaneousStackEntries())
    			    AllZone.Stack.resolveStack();
    		}
            AllZone.Stack.chooseOrderOfSimultaneousStackEntryAll();
    	}
    }
    
    @Override
    public void addObserver(Observer o) {
        super.deleteObservers();
        super.addObserver(o);
    }
    
    boolean needToNextPhase = false;
    
    public void setNeedToNextPhase(boolean needToNextPhase) {
        this.needToNextPhase = needToNextPhase;
    }
    
    public boolean isNeedToNextPhase() {
        return this.needToNextPhase;
    }
    
    //This should only be true four times! that is for the initial nextPhases in MyObservable
    int needToNextPhaseInit = 0;
    
    public boolean isNeedToNextPhaseInit() {
        needToNextPhaseInit++;
        if(needToNextPhaseInit <= 4) {
            return true;
        }
        return false;
    }

	public static boolean canCastSorcery(Player player)
	{
		return AllZone.Phase.isPlayerTurn(player) && (AllZone.Phase.getPhase().equals(Constant.Phase.Main2) || 
			AllZone.Phase.getPhase().equals(Constant.Phase.Main1)) && AllZone.Stack.size() == 0;
	}
	
	public String buildActivateString(String startPhase, String endPhase){
		StringBuilder sb = new StringBuilder();
		
		boolean add = false;
		for(int i = 0; i < phaseOrder.length; i++){
			if (phaseOrder[i].equals(startPhase))
				add = true;
			
			if (add){
				if (sb.length() != 0)
					sb.append(",");
				sb.append(phaseOrder[i]);
			}
			
			if (phaseOrder[i].equals(endPhase))
				add = false;
		}
		
		return sb.toString();
	}

    public static void main(String args[]) {
        Phase phase = new Phase();
        for(int i = 0; i < phase.phaseOrder.length; i++) {
            System.out.println(phase.getPlayerTurn() + " " + phase.getPhase());
            phase.nextPhase();
        }
    }

	public static void increaseSpellCount(SpellAbility sp){
		StormCount++;

		if (sp.getActivatingPlayer().isHuman()) {
			PlayerSpellCount++;
			if (sp instanceof Spell_Permanent && sp.getSourceCard().isCreature()) {
				PlayerCreatureSpellCount++;
			}
		} 
		
		else {
			ComputerSpellCount++;
			if (sp instanceof Spell_Permanent && sp.getSourceCard().isCreature()) {
				Phase.ComputerCreatureSpellCount++;
			}
		}
	}

	public static void setStormCount(int stormCount) {
		StormCount = stormCount;
	}

	public static int getStormCount() {
		return StormCount;
	}

	public static void setGameBegins(int gameBegins) {
		GameBegins = gameBegins;
	}

	public static int getGameBegins() {
		return GameBegins;
	}
}
