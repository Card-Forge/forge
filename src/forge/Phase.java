
package forge;


import java.util.Observer;
import java.util.ArrayList;


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
    
	private int humanExtraTurns;
	private int computerExtraTurns;

	private int humanExtraCombats;
	private int computerExtraCombats;

	private int nCombatsThisTurn;
	
	private String phases[][] =
	{
    //human's turn
			{AllZone.HumanPlayer.getName(), Constant.Phase.Untap},
//	{AllZone.HumanPlayer, Constant.Phase.Upkeep},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Draw},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Main1},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Declare_Attackers},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Declare_Attackers_InstantAbility},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Combat_Declare_Blockers},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Declare_Blockers_InstantAbility},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Combat_Declare_Blockers_InstantAbility},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Combat_After_Declare_Blockers},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_FirstStrikeDamage}, //TODO: need to allow computer to have priority (play instants and abilities).
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Damage},
            {AllZone.HumanPlayer.getName(), Constant.Phase.End_Of_Combat},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Main2},
            {AllZone.HumanPlayer.getName(), Constant.Phase.At_End_Of_Turn},
//	{AllZone.ComputerPlayer , Constant.Phase.End_Of_Turn},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Until_End_Of_Turn},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Cleanup},
            
            //computer's turn
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Untap},
//	{AllZone.ComputerPlayer, Constant.Phase.Upkeep},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Draw},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Main1},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Before_Declare_Attackers_InstantAbility},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Combat_Declare_Attackers},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Declare_Attackers_InstantAbility},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Declare_Blockers},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Combat_Declare_Blockers_InstantAbility},
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Declare_Blockers_InstantAbility},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Combat_After_Declare_Blockers},
            /*{AllZone.HumanPlayer, Constant.Phase.Combat_After_Declare_Blockers},*/
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_FirstStrikeDamage}, //TODO: need to allow computer to have priority (play instants and abilities).
            {AllZone.HumanPlayer.getName(), Constant.Phase.Combat_Damage},
            {AllZone.HumanPlayer.getName(), Constant.Phase.End_Of_Combat},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.End_Of_Combat},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Main2},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.At_End_Of_Turn},
            {AllZone.HumanPlayer.getName(), Constant.Phase.End_Of_Turn},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Until_End_Of_Turn},
            {AllZone.ComputerPlayer.getName(), Constant.Phase.Cleanup},};
    
    public Phase() {
        reset();
    }
    
    public void reset() {
        turn = 1;
        GameBegins = 0;
        phaseIndex = 0;
        humanExtraTurns = 0;
        computerExtraTurns = 0;
        nCombatsThisTurn = 0;
        humanExtraCombats = 0;
        computerExtraCombats = 0;
        this.updateObservers();
    }
    
    public void setPhase(String phase, Player player) {
        phaseIndex = findIndex(phase, player);
        this.updateObservers();
    }
    
    public void nextPhase() {
        
    	//System.out.println("Current Phase:" + AllZone.Phase.getPhase());
        //System.out.println("current active Player: " + getActivePlayer());  
        //experimental, add executeCardStateEffects() here:
        for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
            Command com = GameActionUtil.commands.get(effect);
            com.execute();
        }
        
        GameActionUtil.executeCardStateEffects();
        
        Input_Cleanup.calcMaxHandSize();
        Computer_Cleanup.calcMaxHandSize();
        
        //for debugging: System.out.print("this phase - " +getActivePlayer() +" " +getPhase()+", next phase - ");
        needToNextPhase = false;
        

        if(getPhase().equals(Constant.Phase.Combat_Damage) || getPhase().equals(Constant.Phase.Combat_FirstStrikeDamage)) {
            if(AllZone.Stack.size() != 0) {
                return;
            }
        }
        
        //empty manapool:
        //CardList cl = new CardList(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards());
        //cl = cl.getName("Mana Pool");
        //if (cl.size() > 0)
        //{
        //	Card c = cl.get(0);
        //	c.setExtrinsicKeyword(new ArrayList<String>());
        //}
        
        
        if(!AllZoneUtil.isCardInPlay("Upwelling")) AllZone.ManaPool.clearPool();
        
        if (getPhase().equals(Constant.Phase.Combat_Declare_Attackers)) {
        	nCombatsThisTurn++;
        } else if (getPhase().equals(Constant.Phase.Main1)) {
        	nCombatsThisTurn = 0;
        }

        
        //time vault:
        if((is(Constant.Phase.Cleanup, AllZone.HumanPlayer) && humanExtraTurns >= 0)
                || (is(Constant.Phase.Cleanup, AllZone.ComputerPlayer) && computerExtraTurns >= 0)) {
            Player player = getActivePlayer();
            Player opponent = player.getOpponent();
            CardList list = AllZoneUtil.getPlayerCardsInPlay(opponent, "Time Vault");
            list = list.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                    return c.isTapped();
                }
            });
            
            for(int i = 0; i < list.size(); i++) {
                final Card crd = list.get(i);
                
                /*
                Ability ability = new Ability(list.get(i), "0")
                {
                	public void resolve()
                	{
                		String controller = crd.getController();
                		if (controller.equals(AllZone.HumanPlayer))
                			humanExtraTurns--;
                		else
                			computerExtraTurns--;
                		
                		crd.untap();
                	}
                };
                ability.setStackDescription(crd + " - skip this turn instead, untap Time Vault.");
                */

                if(player.equals(AllZone.ComputerPlayer)) {
                    String[] choices = {"Yes", "No"};
                    Object q = null;
                    q = AllZone.Display.getChoiceOptional("Untap " + crd + "?", choices);
                    if("Yes".equals(q)) {
                        //AllZone.Stack.add(ability);
                        humanExtraTurns--;
                        crd.untap();
                    }
                    
                }
            }
        }
        
        if (getPhase().equals(Constant.Phase.End_Of_Combat)) {
        	resetAttackedThisCombat(getActivePlayer());
        }
        
        //if (getPhase().equals(Constant.Phase.Cleanup) && extraTurns > 0)
        if((is(Constant.Phase.Cleanup, AllZone.HumanPlayer) && humanExtraTurns > 0)
                || (is(Constant.Phase.Cleanup, AllZone.ComputerPlayer) && computerExtraTurns > 0)) {
            //System.out.println("CLEANUP!");
            Player player = getActivePlayer();
            
            if(player.equals(AllZone.ComputerPlayer)) computerExtraTurns--;
            else humanExtraTurns--;
            
            setPhase(Constant.Phase.Untap, player);
        } else if((is(Constant.Phase.Cleanup, AllZone.ComputerPlayer) && humanExtraTurns < 0)
                || (is(Constant.Phase.Cleanup, AllZone.HumanPlayer) && computerExtraTurns < 0)) {
            Player player = getActivePlayer();

            if(player.equals(AllZone.ComputerPlayer)) humanExtraTurns++;
            else computerExtraTurns++;
            
            setPhase(Constant.Phase.Untap, player);
        } else if ((is(Constant.Phase.End_Of_Combat, AllZone.HumanPlayer) && humanExtraCombats > 0) ||
        		(is(Constant.Phase.End_Of_Combat, AllZone.ComputerPlayer) && computerExtraCombats > 0) )
        {
        	Player player = getActivePlayer();
        	Player opp = player.getOpponent();

        	if (player.equals(AllZone.ComputerPlayer)) {
        		computerExtraCombats--;
        	} else {
        		humanExtraCombats--;
        	}
        	AllZone.Combat.reset();
        	AllZone.Combat.setAttackingPlayer(player);
        	AllZone.Combat.setDefendingPlayer(opp);
        	AllZone.pwCombat.reset();
        	AllZone.Combat.setAttackingPlayer(player);
        	AllZone.Combat.setDefendingPlayer(opp);
        	phaseIndex = findIndex(Constant.Phase.Combat_Declare_Attackers, player);
        	//setPhase(Constant.Phase.Combat_Declare_Attackers,player);
        }  else {
            phaseIndex++;
            if(phases.length <= phaseIndex) phaseIndex = 0;
        }
        

        //if(getPhase().equals(Constant.Phase.Untap)) {
        if(is(Constant.Phase.Untap, AllZone.HumanPlayer)) {
        	StormCount = 0;
        	
        	HighTideCount = 0;
            PlayerSpellCount = 0;
            PlayerCreatureSpellCount = 0;   
            ComputerSpellCount = 0;
            ComputerCreatureSpellCount = 0;
            AllZone.GameInfo.setHumanPlayedLands(0);
            turn++;
            /*
            if (humanExtraTurns > 0)
              humanExtraTurns--;
            else if(humanExtraTurns < 0)
              humanExtraTurns++;
              */
        } else if(is(Constant.Phase.Untap, AllZone.ComputerPlayer)) {       	
        	StormCount = 0;
        	GameBegins = 1;
        	HighTideCount = 0;
            PlayerSpellCount = 0;
            PlayerCreatureSpellCount = 0;   
            ComputerSpellCount = 0;
            ComputerCreatureSpellCount = 0;
            AllZone.GameInfo.setComputerPlayedLands(0);
            turn++;
            
            /*
            if (computerExtraTurns > 0)
              computerExtraTurns--;
            else if(computerExtraTurns < 0)
              computerExtraTurns++;
              */
        }
        if(turn == 1 && is(Constant.Phase.Main1, AllZone.HumanPlayer)) {
        	StormCount = 0;
            PlayerSpellCount = 0;
            PlayerCreatureSpellCount = 0;   
            ComputerSpellCount = 0;
            ComputerCreatureSpellCount = 0;
        }
        
        //Mana Drain's delayed bonus mana.The AI can't cast Mana Drain yet, but there are 
        //provisions for that here for future use
        if((is(Constant.Phase.Main1,AllZone.HumanPlayer) || is(Constant.Phase.Main2,AllZone.HumanPlayer) )&& Phase.ManaDrain_BonusMana_Human.size() != 0)
        {        	
        	for(int i=0;i<Phase.ManaDrain_BonusMana_Human.size();i++)
        	{
        		AllZone.ManaPool.addManaToFloating(Integer.toString(Phase.ManaDrain_BonusMana_Human.get(i)), Phase.ManaDrain_Source_Human.get(i) );
        	}
        	
        	Phase.ManaDrain_BonusMana_Human.clear();
        	Phase.ManaDrain_Source_Human.clear();
        }
        if((is(Constant.Phase.Main1,AllZone.ComputerPlayer) || is(Constant.Phase.Main2,AllZone.ComputerPlayer) )&& Phase.ManaDrain_BonusMana_AI.size() != 0)
        {
        	for(int i=0;i<Phase.ManaDrain_BonusMana_AI.size();i++)
        	{
        		AllZone.ManaPool.addManaToFloating(Integer.toString(Phase.ManaDrain_BonusMana_AI.get(i)), Phase.ManaDrain_Source_AI.get(i) );
        	}
        	
        	Phase.ManaDrain_BonusMana_AI.clear();
        	Phase.ManaDrain_Source_AI.clear();
        }
        
        
        //for debugging: System.out.println(getPhase());
        //System.out.println(getPhase() + " " + getActivePlayer());
        //System.out.print("");
        this.updateObservers();
        if(AllZone.Phase != null) {
            if(AllZone.Phase.isNeedToNextPhase() == true) {
                AllZone.Phase.setNeedToNextPhase(false);
                AllZone.Phase.nextPhase();
            }
        }
    }
    
    public synchronized boolean is(String phase, Player player) {
        return (getPhase().equals(phase) && getActivePlayer().isPlayer(player));
    }
    
    private int findIndex(String phase, Player player) {
        for(int i = 0; i < phases.length; i++) {
            if(player.getName().equals(phases[i][0]) && phase.equals(phases[i][1])) return i;
        }
        throw new RuntimeException("Phase : findIndex() invalid argument, phase = " + phase + " player = "
                + player);
    }
    
    public Player getActivePlayer() {
    	//TODO - Player class conversion - see if there's a better way than this hack.
        String name = phases[phaseIndex][0];
        if(name.equals("Human")) return AllZone.HumanPlayer;
        else return AllZone.ComputerPlayer;
        
        //return phases[phaseIndex][0];
    }
    
    public String getPhase() {
        return phases[phaseIndex][1];
    }
    
    public int getTurn() {
        return turn;
    }
    
    public void setTurn(int in_turn) {
        turn = in_turn;
    }
    
    public void addExtraTurn(Player player) {
        if(player.equals(AllZone.HumanPlayer)) humanExtraTurns++;
        else computerExtraTurns++;
    }
    
    public void subtractExtraTurn(Player player) {
        if(player.equals(AllZone.HumanPlayer)) humanExtraTurns--;
        else computerExtraTurns--;
    }
    
    public int getExtraTurns(Player player) {
        if(player.equals(AllZone.HumanPlayer)) return humanExtraTurns;
        else return computerExtraTurns;
    }
    
    public void setExtraTurns(int i, Player player) {
        if(player.equals(AllZone.HumanPlayer)) humanExtraTurns = i;
        else computerExtraTurns = i;
    }
    
    // Could have a full set of accessors here but for Finest Hour there's no need
    public void addExtraCombat(Player player) {
    	if (player.equals(AllZone.HumanPlayer))
    		humanExtraCombats++;
    	else
    		computerExtraCombats++;
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

    
    public static void main(String args[]) {
        Phase phase = new Phase();
        for(int i = 0; i < phase.phases.length + 3; i++) {
            System.out.println(phase.getActivePlayer() + " " + phase.getPhase());
            phase.nextPhase();
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
				&& AllZone.GameAction.isPlayerTurn(player)) && AllZone.Stack.size() == 0);
	}
	
	public static boolean canPlayDuringCombat() {
		String phase = AllZone.Phase.getPhase();
		ArrayList<String> validPhases = new ArrayList<String>();
		validPhases.add(Constant.Phase.Combat_Before_Declare_Attackers_InstantAbility);
		validPhases.add(Constant.Phase.Combat_Declare_Attackers);
		validPhases.add(Constant.Phase.Combat_Declare_Attackers_InstantAbility);
		validPhases.add(Constant.Phase.Combat_Declare_Blockers);
		validPhases.add(Constant.Phase.Combat_Declare_Blockers_InstantAbility);
		validPhases.add(Constant.Phase.Combat_After_Declare_Blockers);
		validPhases.add(Constant.Phase.Combat_FirstStrikeDamage);
		validPhases.add(Constant.Phase.Combat_Damage);
		
		return validPhases.contains(phase);
	}
	
	public static boolean canPlayAfterUpkeep() {
		String phase = AllZone.Phase.getPhase();
		ArrayList<String> validPhases = new ArrayList<String>();
		
		validPhases.add(Constant.Phase.Draw);
		validPhases.add(Constant.Phase.Main1);
		validPhases.add(Constant.Phase.Combat_Before_Declare_Attackers_InstantAbility);
		validPhases.add(Constant.Phase.Combat_Declare_Attackers);
		validPhases.add(Constant.Phase.Combat_Declare_Attackers_InstantAbility);
		validPhases.add(Constant.Phase.Combat_Declare_Blockers);
		validPhases.add(Constant.Phase.Combat_Declare_Blockers_InstantAbility);
		validPhases.add(Constant.Phase.Combat_After_Declare_Blockers);
		validPhases.add(Constant.Phase.Combat_FirstStrikeDamage);
		validPhases.add(Constant.Phase.Combat_Damage);
		validPhases.add(Constant.Phase.Main2);
		validPhases.add(Constant.Phase.At_End_Of_Turn);
		validPhases.add(Constant.Phase.End_Of_Turn);
		validPhases.add(Constant.Phase.Until_End_Of_Turn);
		validPhases.add(Constant.Phase.Cleanup);
		
		return validPhases.contains(phase);
	}
}
