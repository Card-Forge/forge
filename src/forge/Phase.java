package forge;
import java.util.Observer;

public class Phase extends MyObservable
{
  private int phaseIndex;
  private int turn;
  
  private int humanExtraTurns;
  private int computerExtraTurns;
  
  private String phases[][] =
  {
    //human's turn
    {Constant.Player.Human    , Constant.Phase.Untap}                                  ,
//   {Constant.Player.Human    , Constant.Phase.Upkeep}                                ,
    {Constant.Player.Human    , Constant.Phase.Draw}                                   ,
    {Constant.Player.Human    , Constant.Phase.Main1}                                  ,
    {Constant.Player.Human    , Constant.Phase.Combat_Declare_Attackers}               ,
    {Constant.Player.Computer , Constant.Phase.Combat_Declare_Blockers}                ,
    {Constant.Player.Human    , Constant.Phase.Combat_Declare_Blockers_InstantAbility} ,
    {Constant.Player.Computer , Constant.Phase.Combat_Declare_Blockers_InstantAbility} ,
    {Constant.Player.Human    , Constant.Phase.Combat_FirstStrikeDamage}               , //TODO: need to allow computer to have priority (play instants and abilities).
    {Constant.Player.Human    , Constant.Phase.Combat_Damage}                          ,
    {Constant.Player.Human    , Constant.Phase.Main2}                                  ,
    {Constant.Player.Human    , Constant.Phase.At_End_Of_Turn}                         ,
//   {Constant.Player.Computer , Constant.Phase.End_Of_Turn}                           ,
    {Constant.Player.Human    , Constant.Phase.Until_End_Of_Turn}                      ,
    {Constant.Player.Human    , Constant.Phase.Cleanup}                                ,

    //computer's turn
    {Constant.Player.Computer    , Constant.Phase.Untap}                               ,
    {Constant.Player.Computer    , Constant.Phase.Draw}                                ,
    {Constant.Player.Computer , Constant.Phase.Main1}                                  ,
    {Constant.Player.Human , Constant.Phase.Combat_Before_Declare_Attackers_InstantAbility},
    {Constant.Player.Computer , Constant.Phase.Combat_Declare_Attackers}               ,
    {Constant.Player.Human	  , Constant.Phase.Combat_Declare_Attackers_InstantAbility},
    {Constant.Player.Human    , Constant.Phase.Combat_Declare_Blockers}                ,
    {Constant.Player.Computer , Constant.Phase.Combat_Declare_Blockers_InstantAbility} ,
    {Constant.Player.Human    , Constant.Phase.Combat_Declare_Blockers_InstantAbility} ,
    {Constant.Player.Human    , Constant.Phase.Combat_FirstStrikeDamage}               ,  //TODO: need to allow computer to have priority (play instants and abilities).
    {Constant.Player.Human    , Constant.Phase.Combat_Damage}                          ,
    {Constant.Player.Computer , Constant.Phase.Main2}                                  ,
    {Constant.Player.Computer , Constant.Phase.At_End_Of_Turn}                         ,
    {Constant.Player.Human    , Constant.Phase.End_Of_Turn}                            ,
    {Constant.Player.Computer , Constant.Phase.Until_End_Of_Turn}                      ,
    {Constant.Player.Computer    , Constant.Phase.Cleanup}                             ,
  };

  public Phase()
  {
    reset();
  }
  public void reset()
  {
    turn = 1;
    phaseIndex = 0;
    humanExtraTurns = 0;
    computerExtraTurns = 0;
    this.updateObservers();
  }
  public void setPhase(String phase, String player)
  {
    phaseIndex = findIndex(phase, player);
    this.updateObservers();
  }
  public void nextPhase()
  {
	
	//System.out.println("current active Player: " + getActivePlayer());  
	//experimental, add executeCardStateEffects() here:
	for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
		Command com = GameActionUtil.commands.get(effect);
		com.execute();
	}
	  
    GameActionUtil.executeCardStateEffects();
	
	//for debugging: System.out.print("this phase - " +getActivePlayer() +" " +getPhase()+", next phase - ");
    needToNextPhase = false;
    

    if(getPhase().equals(Constant.Phase.Combat_Damage)){
    	if(AllZone.Stack.size() != 0){
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
    
    
    AllZone.ManaPool.clear();
    
    //if (getPhase().equals(Constant.Phase.Cleanup) && extraTurns > 0)
    if ((is(Constant.Phase.Cleanup, Constant.Player.Human) && humanExtraTurns > 0 ) || 
    	(is(Constant.Phase.Cleanup, Constant.Player.Computer) && computerExtraTurns > 0 ) )
    {
    	//System.out.println("CLEANUP!");
    	String player = getActivePlayer();
    	String opponent = AllZone.GameAction.getOpponent(player);
    	
    	AllZone.GameAction.setLastPlayerToDraw(opponent);
    	setPhase(Constant.Phase.Untap, player);
    	
    }
    else
    {
	    phaseIndex++;
	    if(phases.length <= phaseIndex)
	        phaseIndex = 0;
    }
     

    //if(getPhase().equals(Constant.Phase.Untap)) {
    if (is(Constant.Phase.Untap, Constant.Player.Human))
    {
      turn++;
      if (humanExtraTurns > 0)
    	  humanExtraTurns--;
      else if(humanExtraTurns < 0)
    	  humanExtraTurns++;
    }
    else if (is(Constant.Phase.Untap, Constant.Player.Computer))
    {
      turn++;
      if (computerExtraTurns > 0)
    	  computerExtraTurns--;
      else if(computerExtraTurns < 0)
    	  computerExtraTurns++;
    }
    
    //for debugging: System.out.println(getPhase());
    
    //System.out.print("");
    this.updateObservers();
    if(AllZone.Phase != null){
	    if(AllZone.Phase.isNeedToNextPhase()==true){
	    	AllZone.Phase.setNeedToNextPhase(false);
	    	AllZone.Phase.nextPhase();
	    }
    }
  }
  public synchronized boolean is(String phase, String player)
  {
    return (getPhase().equals(phase) && getActivePlayer().equals(player));
  }
  private int findIndex(String phase, String player)
  {
    for(int i = 0; i < phases.length; i++)
    {
      if(player.equals(phases[i][0]) && phase.equals(phases[i][1]))
        return i;
    }
    throw new RuntimeException("Phase : findIndex() invalid argument, phase = " +phase +" player = " +player);
  }
  public String getActivePlayer()
  {
	//hack
    return phases[phaseIndex][0];
  }
  public String getPhase()
  {
    return phases[phaseIndex][1];
  }
  public int getTurn()
  {
    return turn;
  }
  public void setTurn(int in_turn)
  {
    turn = in_turn;
  }
  
  public void addExtraTurn(String player)
  {
	  if (player.equals(Constant.Player.Human))
		  humanExtraTurns++;
	  else
		  computerExtraTurns++;
  }
  
  public int getExtraTurns(String player)
  {
	  if (player.equals(Constant.Player.Human))
		  return humanExtraTurns;
	  else
		  return computerExtraTurns;
  }
  
  public void setExtraTurns(int i, String player)
  {
	  if (player.equals(Constant.Player.Human))
		  humanExtraTurns = i;
	  else
		  computerExtraTurns = i;
  }
  
  public static void main(String args[])
  {
    Phase phase = new Phase();
    for(int i = 0; i < phase.phases.length + 3; i++)
    {
      System.out.println(phase.getActivePlayer() +" " +phase.getPhase());
      phase.nextPhase();
    }
  }
  
  public void addObserver(Observer o){
	  super.deleteObservers();
	  super.addObserver(o);
  }
  
  boolean needToNextPhase = false;
  public void setNeedToNextPhase(boolean needToNextPhase) {
	  this.needToNextPhase = needToNextPhase;
  }
  
  public boolean isNeedToNextPhase(){
	  return this.needToNextPhase;
  }
  
  //This should only be true four times! that is for the initial nextPhases in MyObservable
  int needToNextPhaseInit = 0;
  public boolean isNeedToNextPhaseInit() {
	  needToNextPhaseInit++;
	if(needToNextPhaseInit <=4){
		return true;
	}
	return false;
}

  
}