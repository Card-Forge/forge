
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
			{Constant.Player.Human, Constant.Phase.Untap},
//	{Constant.Player.Human, Constant.Phase.Upkeep},
            {Constant.Player.Human, Constant.Phase.Draw},
            {Constant.Player.Human, Constant.Phase.Main1},
            {Constant.Player.Human, Constant.Phase.Combat_Declare_Attackers},
            {Constant.Player.Human, Constant.Phase.Combat_Declare_Attackers_InstantAbility},
            {Constant.Player.Computer, Constant.Phase.Combat_Declare_Blockers},
            {Constant.Player.Human, Constant.Phase.Combat_Declare_Blockers_InstantAbility},
            {Constant.Player.Computer, Constant.Phase.Combat_Declare_Blockers_InstantAbility},
            {Constant.Player.Computer, Constant.Phase.Combat_After_Declare_Blockers},
            {Constant.Player.Human, Constant.Phase.Combat_FirstStrikeDamage}, //TODO: need to allow computer to have priority (play instants and abilities).
            {Constant.Player.Human, Constant.Phase.Combat_Damage},
            {Constant.Player.Human, Constant.Phase.End_Of_Combat},
            {Constant.Player.Human, Constant.Phase.Main2},
            {Constant.Player.Human, Constant.Phase.At_End_Of_Turn},
//	{Constant.Player.Computer , Constant.Phase.End_Of_Turn},
            {Constant.Player.Human, Constant.Phase.Until_End_Of_Turn},
            {Constant.Player.Human, Constant.Phase.Cleanup},
            
            //computer's turn
            {Constant.Player.Computer, Constant.Phase.Untap},
//	{Constant.Player.Computer, Constant.Phase.Upkeep},
            {Constant.Player.Computer, Constant.Phase.Draw},
            {Constant.Player.Computer, Constant.Phase.Main1},
            {Constant.Player.Human, Constant.Phase.Combat_Before_Declare_Attackers_InstantAbility},
            {Constant.Player.Computer, Constant.Phase.Combat_Declare_Attackers},
            {Constant.Player.Human, Constant.Phase.Combat_Declare_Attackers_InstantAbility},
            {Constant.Player.Human, Constant.Phase.Combat_Declare_Blockers},
            {Constant.Player.Computer, Constant.Phase.Combat_Declare_Blockers_InstantAbility},
            {Constant.Player.Human, Constant.Phase.Combat_Declare_Blockers_InstantAbility},
            {Constant.Player.Computer, Constant.Phase.Combat_After_Declare_Blockers},
            /*{Constant.Player.Human, Constant.Phase.Combat_After_Declare_Blockers},*/
            {Constant.Player.Human, Constant.Phase.Combat_FirstStrikeDamage}, //TODO: need to allow computer to have priority (play instants and abilities).
            {Constant.Player.Human, Constant.Phase.Combat_Damage},
            {Constant.Player.Human, Constant.Phase.End_Of_Combat},
            {Constant.Player.Computer, Constant.Phase.End_Of_Combat},
            {Constant.Player.Computer, Constant.Phase.Main2},
            {Constant.Player.Computer, Constant.Phase.At_End_Of_Turn},
            {Constant.Player.Human, Constant.Phase.End_Of_Turn},
            {Constant.Player.Computer, Constant.Phase.Until_End_Of_Turn},
            {Constant.Player.Computer, Constant.Phase.Cleanup},};
    
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
    
    public void setPhase(String phase, String player) {
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
        //CardList cl = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
        //cl = cl.getName("Mana Pool");
        //if (cl.size() > 0)
        //{
        //	Card c = cl.get(0);
        //	c.setExtrinsicKeyword(new ArrayList<String>());
        //}
        

        AllZone.ManaPool.clearPool();
        if (getPhase().equals(Constant.Phase.Combat_Declare_Attackers)) {
        	nCombatsThisTurn++;
        } else if (getPhase().equals(Constant.Phase.Main1)) {
        	nCombatsThisTurn = 0;
        }

        
        //time vault:
        if((is(Constant.Phase.Cleanup, Constant.Player.Human) && humanExtraTurns >= 0)
                || (is(Constant.Phase.Cleanup, Constant.Player.Computer) && computerExtraTurns >= 0)) {
            String player = getActivePlayer();
            String opponent = AllZone.GameAction.getOpponent(player);
            CardList list = CardFactoryUtil.getCards("Time Vault", opponent);
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
                		if (controller.equals(Constant.Player.Human))
                			humanExtraTurns--;
                		else
                			computerExtraTurns--;
                		
                		crd.untap();
                	}
                };
                ability.setStackDescription(crd + " - skip this turn instead, untap Time Vault.");
                */

                if(player.equals(Constant.Player.Computer)) {
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
        if((is(Constant.Phase.Cleanup, Constant.Player.Human) && humanExtraTurns > 0)
                || (is(Constant.Phase.Cleanup, Constant.Player.Computer) && computerExtraTurns > 0)) {
            //System.out.println("CLEANUP!");
            String player = getActivePlayer();
            
            if(player.equals(Constant.Player.Computer)) computerExtraTurns--;
            else humanExtraTurns--;
            
            setPhase(Constant.Phase.Untap, player);
        } else if((is(Constant.Phase.Cleanup, Constant.Player.Computer) && humanExtraTurns < 0)
                || (is(Constant.Phase.Cleanup, Constant.Player.Human) && computerExtraTurns < 0)) {
            String player = getActivePlayer();

            if(player.equals(Constant.Player.Computer)) humanExtraTurns++;
            else computerExtraTurns++;
            
            setPhase(Constant.Phase.Untap, player);
        } else if ((is(Constant.Phase.End_Of_Combat, Constant.Player.Human) && humanExtraCombats > 0) ||
        		(is(Constant.Phase.End_Of_Combat, Constant.Player.Computer) && computerExtraCombats > 0) )
        {
        	String player = getActivePlayer();
        	String opp = AllZone.GameAction.getOpponent(player);

        	if (player.equals(Constant.Player.Computer)) {
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
        if(is(Constant.Phase.Untap, Constant.Player.Human)) {
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
        } else if(is(Constant.Phase.Untap, Constant.Player.Computer)) {       	
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
        if(turn == 1 && is(Constant.Phase.Main1, Constant.Player.Human)) {
        	StormCount = 0;
            PlayerSpellCount = 0;
            PlayerCreatureSpellCount = 0;   
            ComputerSpellCount = 0;
            ComputerCreatureSpellCount = 0;
        }
        
        //Mana Drain's delayed bonus mana.The AI can't cast Mana Drain yet, but there are 
        //provisions for that here for future use
        if((is(Constant.Phase.Main1,Constant.Player.Human) || is(Constant.Phase.Main2,Constant.Player.Human) )&& Phase.ManaDrain_BonusMana_Human.size() != 0)
        {        	
        	for(int i=0;i<Phase.ManaDrain_BonusMana_Human.size();i++)
        	{
        		AllZone.ManaPool.addManaToFloating(Integer.toString(Phase.ManaDrain_BonusMana_Human.get(i)), Phase.ManaDrain_Source_Human.get(i) );
        	}
        	
        	Phase.ManaDrain_BonusMana_Human.clear();
        	Phase.ManaDrain_Source_Human.clear();
        }
        if((is(Constant.Phase.Main1,Constant.Player.Computer) || is(Constant.Phase.Main2,Constant.Player.Computer) )&& Phase.ManaDrain_BonusMana_AI.size() != 0)
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
    
    public synchronized boolean is(String phase, String player) {
        return (getPhase().equals(phase) && getActivePlayer().equals(player));
    }
    
    private int findIndex(String phase, String player) {
        for(int i = 0; i < phases.length; i++) {
            if(player.equals(phases[i][0]) && phase.equals(phases[i][1])) return i;
        }
        throw new RuntimeException("Phase : findIndex() invalid argument, phase = " + phase + " player = "
                + player);
    }
    
    public String getActivePlayer() {
        //hack
        return phases[phaseIndex][0];
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
    
    public void addExtraTurn(String player) {
        if(player.equals(Constant.Player.Human)) humanExtraTurns++;
        else computerExtraTurns++;
    }
    
    public void subtractExtraTurn(String player) {
        if(player.equals(Constant.Player.Human)) humanExtraTurns--;
        else computerExtraTurns--;
    }
    
    public int getExtraTurns(String player) {
        if(player.equals(Constant.Player.Human)) return humanExtraTurns;
        else return computerExtraTurns;
    }
    
    public void setExtraTurns(int i, String player) {
        if(player.equals(Constant.Player.Human)) humanExtraTurns = i;
        else computerExtraTurns = i;
    }
    
    // Could have a full set of accessors here but for Finest Hour there's no need
    public void addExtraCombat(String player) {
    	if (player.equals(Constant.Player.Human))
    		humanExtraCombats++;
    	else
    		computerExtraCombats++;
    }

    public boolean isFirstCombat() {
    	return (nCombatsThisTurn == 1);
    }
    
    public void resetAttackedThisCombat(String player) {
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

	public static boolean canCastSorcery(String player)
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
