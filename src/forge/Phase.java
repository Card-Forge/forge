package forge;

import java.util.Observer;
import java.util.ArrayList;
import java.util.Stack;

import com.esotericsoftware.minlog.Log;

public class Phase extends MyObservable
{
	private int phaseIndex;
	private int turn;

	static int	 	GameBegins = 0; // Omnath
    static int	   StormCount;
    static int	   HighTideCount = 0;
    static int	   PlayerSpellCount;
    static int	   PlayerCreatureSpellCount;   
    static int	   ComputerSpellCount;
    static int	   ComputerCreatureSpellCount;
    static boolean	   Sac_Dauntless_Escort;
    static boolean	   Sac_Dauntless_Escort_Comp;
    
    //Not sure these should be here but I can't think of a better place
    static ArrayList<Integer> ManaDrain_BonusMana_Human = new ArrayList<Integer>();
    static ArrayList<Integer> ManaDrain_BonusMana_AI = new ArrayList<Integer>();
    static CardList ManaDrain_Source_Human = new CardList();
    static CardList ManaDrain_Source_AI = new CardList();
    
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
    
    private Player bPlayerPriority = AllZone.HumanPlayer;
    
    public Player getPriorityPlayer() {
    	return bPlayerPriority;
    }
    
    public void setPriorityPlayer(Player p) {
    	bPlayerPriority = p;
    }
    
	private boolean bPhaseEffects = true;
    public boolean doPhaseEffects() {
    	return bPhaseEffects;
    }
    
    public void setPhaseEffects(boolean b) {
    	bPhaseEffects = b;
    } 
    
    private boolean bCombat = false;
    public boolean inCombat() {
    	return bCombat;
    }
    
    public void setCombat(boolean b) {
    	bCombat = b;
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
        bPlayerPriority = playerTurn;
        bPhaseEffects = true;
        needToNextPhase = false;
        GameBegins = 0;
        phaseIndex = 0;
        extraTurns.clear();
        nCombatsThisTurn = 0;
        extraCombats = 0;
        bCombat = false;
        this.updateObservers();
    }
    
    public void turnReset(){
    	StormCount = 0;
    	HighTideCount = 0;
        PlayerSpellCount = 0;
        PlayerCreatureSpellCount = 0;   
        ComputerSpellCount = 0;
        ComputerCreatureSpellCount = 0;
        if (playerTurn.isHuman())
        	AllZone.GameInfo.setHumanPlayedLands(0);
        else
            AllZone.GameInfo.setComputerPlayedLands(0);
    }

	public void handleBeginPhase(){
		if (Phase.GameBegins == 0)
			return;
		
		AllZone.Phase.setPhaseEffects(false);
		// Handle effects that happen at the beginning of phases
        final String phase = AllZone.Phase.getPhase();
        final Player turn = AllZone.Phase.getPlayerTurn();
        
        if(phase.equals(Constant.Phase.Untap)) {
            PhaseUtil.handleUntap();
	    }
	    else if(phase.equals(Constant.Phase.Upkeep)){
	    	PhaseUtil.handleUpkeep();
	    }
	    
	    else if(phase.equals(Constant.Phase.Draw)){
	    	PhaseUtil.handleDraw();
	    }
	    	
	    else if (phase.equals(Constant.Phase.Main1) || phase.equals(Constant.Phase.Main2)){
	    	// TODO: Move the function to Player class, and use gainMamanaDrainMana() instead
	    	// turn.gainManaDrainMana();
	    	
	    	if (turn.isHuman() && Phase.ManaDrain_BonusMana_Human.size() != 0){
	        	for(int i=0;i<Phase.ManaDrain_BonusMana_Human.size();i++)
	        		AllZone.ManaPool.addManaToFloating(Integer.toString(Phase.ManaDrain_BonusMana_Human.get(i)), Phase.ManaDrain_Source_Human.get(i) );
	        	
	        	Phase.ManaDrain_BonusMana_Human.clear();
	        	Phase.ManaDrain_Source_Human.clear();
	    	}
	    	
	        if(turn.isComputer() && Phase.ManaDrain_BonusMana_AI.size() != 0){
	        	//for(int i=0;i<Phase.ManaDrain_BonusMana_AI.size();i++)
	        	//	AllZone.ManaPool.addManaToFloating(Integer.toString(Phase.ManaDrain_BonusMana_AI.get(i)), Phase.ManaDrain_Source_AI.get(i) );

	        	// Mana is currently lost for AI. The above commented code was adding to the Human's mana pool
	        	
	        	Phase.ManaDrain_BonusMana_AI.clear();
	        	Phase.ManaDrain_Source_AI.clear();
	        }
	    }
        
	    else if(phase.equals(Constant.Phase.Combat_Begin)){
	    	if (turn.isHuman() || ComputerUtil.getPossibleAttackers().size() > 0) {
	            AllZone.Combat.verifyCreaturesInPlay();
	            CombatUtil.showCombat();
		    }
            else {
                this.setNeedToNextPhase(true);
            }
	    }
        
	    else if (phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)){
            if(inCombat()) {
	            AllZone.Combat.verifyCreaturesInPlay();
	            CombatUtil.showCombat();
            }
            else
            	AllZone.Phase.setNeedToNextPhase(true);
	    }
        
        // we can skip AfterBlockers and AfterAttackers if necessary
	    else if(phase.equals(Constant.Phase.Combat_Declare_Blockers) ||
	    		phase.equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
            if(inCombat()) {
	            AllZone.Combat.verifyCreaturesInPlay();
	            CombatUtil.showCombat();
            }
            else
            	AllZone.Phase.setNeedToNextPhase(true);
        }
        
	    else if (phase.equals(Constant.Phase.Combat_FirstStrikeDamage)){
	    	if(!inCombat())
	    		AllZone.Phase.setNeedToNextPhase(true);
	    	else{
	    		AllZone.Combat.verifyCreaturesInPlay();
	            AllZone.pwCombat.verifyCreaturesInPlay();

				AllZone.Combat.setAssignedFirstStrikeDamage();
				AllZone.pwCombat.setAssignedFirstStrikeDamage();
				
		    	if (!AllZone.GameInfo.isPreventCombatDamageThisTurn())
		    		 Combat.dealAssignedDamage();
		        
		        AllZone.GameAction.checkStateEffects();
		        CombatUtil.showCombat();
	    	}
	    }
	    	
	    else if (phase.equals(Constant.Phase.Combat_Damage)){
	    	if(!inCombat())
	    		AllZone.Phase.setNeedToNextPhase(true);
	    	else{
	    		AllZone.Combat.verifyCreaturesInPlay();
	            AllZone.pwCombat.verifyCreaturesInPlay();
	    		
		        AllZone.Combat.setAssignedDamage();
		        AllZone.pwCombat.setAssignedDamage();
	            
	    		if (!AllZone.GameInfo.isPreventCombatDamageThisTurn())
	    			Combat.dealAssignedDamage();
	    			
	    		AllZone.GameAction.checkStateEffects();
		        CombatUtil.showCombat();
	    	}
	    }
        
	    else if (phase.equals(Constant.Phase.Combat_End))
        {
			if (!inCombat()){
				AllZone.Phase.setNeedToNextPhase(true);
			}
			else{
				AllZone.EndOfCombat.executeUntil();
				AllZone.EndOfCombat.executeAt();
			}
        }

	    else if(phase.equals(Constant.Phase.End_Of_Turn)) {
	    	AllZone.EndOfTurn.executeAt();
	    	// todo: when we have a bar for selecting which phases to stop at, we will check that instead of display.stop
	    	if(!AllZone.Display.stopEOT()) 
	    		AllZone.Phase.setNeedToNextPhase(true);
        }
        
	    else if(phase.equals(Constant.Phase.Cleanup)){
	    	AllZone.EndOfTurn.executeUntil();
	    }
        
		bPlayerPriority = playerTurn;
	}
	
    public void nextPhase() {
        //experimental, add executeCardStateEffects() here:
        for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
            Command com = GameActionUtil.commands.get(effect);
            com.execute();
        }
        
        GameActionUtil.executeCardStateEffects();
        
        Input_Cleanup.calcMaxHandSize();
        Computer_Cleanup.calcMaxHandSize();
        
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
        	nCombatsThisTurn++;
        	CardList list = new CardList();
	        list.addAll(AllZone.Combat.getAttackers());
	        
	        // Remove illegal Propaganda attacks first
	        for(Card c:list)
	            CombatUtil.checkPropagandaEffects(c);
	        
	        // Then run other Attacker bonuses
	        //check for exalted:
	        if ((AllZone.Combat.getDeclaredAttackers() + AllZone.pwCombat.getDeclaredAttackers() == 1)) {
	        	
	        	AllZone.GameAction.CheckWheneverKeyword(list.get(0), "Attack - Alone", null);
	            Player attackingPlayer = AllZone.Combat.getAttackingPlayer();
	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, attackingPlayer);
	            CardList exalted = new CardList(play.getCards());
	            exalted = exalted.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.getKeyword().contains("Exalted");
	                }
	            });
	            if(exalted.size() > 0) CombatUtil.executeExaltedAbility(list.get(0), exalted.size());
	            // Make sure exalted effects get applied only once per combat
	        }
	        
	        for(Card c:list)
	            CombatUtil.checkDeclareAttackers(c);
        } 
        else if (getPhase().equals(Constant.Phase.Untap)) {
        	nCombatsThisTurn = 0;
        }
        
        if (getPhase().equals(Constant.Phase.Combat_End)) {
            AllZone.Combat.reset();
            AllZone.pwCombat.reset();
            AllZone.Display.showCombat("");
        	resetAttackedThisCombat(getPlayerTurn());
        	this.bCombat = false;
        }
        
        if (phaseOrder[phaseIndex].equals(Constant.Phase.Cleanup))
        	AllZone.Phase.setPlayerTurn(handleNextTurn());
        
        if (is(Constant.Phase.Combat_Declare_Blockers)){
        	// Before damage is assigned, confirm how things are blocked/blocking
        	CardList list = new CardList();
            list.addAll(AllZone.Combat.getAllBlockers().toArray());
            list.addAll(AllZone.pwCombat.getAllBlockers().toArray());
            list = list.filter(new CardListFilter(){
            	public boolean addCard(Card c)
            	{
            		return !c.getCreatureBlockedThisCombat();
            	}
            });
            
            CardList attList = new CardList();
            attList.addAll(AllZone.Combat.getAttackers());
            
            CardList pwAttList = new CardList();
            pwAttList.addAll(AllZone.pwCombat.getAttackers());

            CombatUtil.checkDeclareBlockers(list);
            
            for (Card a:attList){
            	CardList blockList = AllZone.Combat.getBlockers(a);
            	for (Card b:blockList)
            		CombatUtil.checkBlockedAttackers(a, b);
            }
            
            for (Card a:pwAttList){
            	CardList blockList = AllZone.pwCombat.getBlockers(a);
            	for (Card b:blockList)
            		CombatUtil.checkBlockedAttackers(a, b);
            }
        }
        
        if (is(Constant.Phase.Combat_End) && extraCombats > 0){
        	// todo: ExtraCombat needs to be changed for other spell/abilities that give extra combat
        	// can do it like ExtraTurn stack ExtraPhases

        	Player player = getPlayerTurn();
        	Player opp = player.getOpponent();

        	bCombat = true;
        	extraCombats--;
        	AllZone.Combat.reset();
        	AllZone.Combat.setAttackingPlayer(player);
        	AllZone.Combat.setDefendingPlayer(opp);
        	AllZone.pwCombat.reset();
        	AllZone.Combat.setAttackingPlayer(player);
        	AllZone.Combat.setDefendingPlayer(opp);
        	phaseIndex = findIndex(Constant.Phase.Combat_Declare_Attackers);
        }  
        else {
            phaseIndex++;
            phaseIndex %= phaseOrder.length;
        }
        
        // **** Anything BELOW Here is actually in the next phase. Maybe move this to handleBeginPhase
        if(getPhase().equals(Constant.Phase.Untap)){
            turn++;
        }
        if(turn == 1 && is(Constant.Phase.Upkeep, AllZone.HumanPlayer)) {
        	turnReset();	// Not sure if Untap is skipped on first turn?
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

            if(turn.equals(AllZone.HumanPlayer)) {
                String[] choices = {"Yes", "No"};
                Object q = null;
                q = AllZone.Display.getChoiceOptional("Untap " + crd + "?", choices);
                if("Yes".equals(q)) {
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
        PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
        
        CardList list = new CardList();
        list.addAll(play.getCards());
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
    	Player lastToAct = (AllZone.Stack.size() == 0) ? getPlayerTurn() : AllZone.Stack.peek().getActivatingPlayer();
    	
    	if (lastToAct == null){	// Just in case it slips through, I think I have it nailed down now though?
    		Card c = AllZone.Stack.peek().getSourceCard();
    		System.out.println(c.getName() + " :Activating Player not set.");
    		lastToAct = c.getController();
    	}
    	
    	// actingPlayer is the player who may act
    	// the lastToActed player is the person that is the most recent act, either that players turn, or last player to use a SA
    	
    	if (lastToAct.equals(actingPlayer)){
    		// pass the priority to other player
    		bPlayerPriority = actingPlayer.getOpponent();
    		//if (!actingPlayer.isComputer())
    		AllZone.InputControl.resetInput();
    	}
    	else{
    		if (AllZone.Stack.size() == 0){
    			// end phase
    			needToNextPhase = true;
    			bPlayerPriority = getPlayerTurn();	// think this is needed here, but not elsewhere
    		}
    		else{
    			AllZone.Stack.resolveStack();
    		}
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
		return ((AllZone.Phase.getPhase().equals(Constant.Phase.Main2) || (AllZone.Phase.getPhase().equals(Constant.Phase.Main1)) 
				&& AllZone.Phase.isPlayerTurn(player)) && AllZone.Stack.size() == 0);
	}
	
	public static boolean canPlayDuringCombat() {
		String phase = AllZone.Phase.getPhase();
		ArrayList<String> validPhases = new ArrayList<String>();
		validPhases.add(Constant.Phase.Combat_Begin);
		validPhases.add(Constant.Phase.Combat_Declare_Attackers);
		validPhases.add(Constant.Phase.Combat_Declare_Attackers_InstantAbility);
		validPhases.add(Constant.Phase.Combat_Declare_Blockers);
		validPhases.add(Constant.Phase.Combat_Declare_Blockers_InstantAbility);
		validPhases.add(Constant.Phase.Combat_FirstStrikeDamage);
		validPhases.add(Constant.Phase.Combat_Damage);
		validPhases.add(Constant.Phase.Combat_End);
		
		return validPhases.contains(phase);
	}
	
	public static boolean canPlayAfterUpkeep() {
		String phase = AllZone.Phase.getPhase();
		
		return !phase.equals(Constant.Phase.Upkeep);
	}
	
    public static void main(String args[]) {
        Phase phase = new Phase();
        for(int i = 0; i < phase.phaseOrder.length; i++) {
            System.out.println(phase.getPlayerTurn() + " " + phase.getPhase());
            phase.nextPhase();
        }
    }
}
