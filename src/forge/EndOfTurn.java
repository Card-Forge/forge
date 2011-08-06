package forge;

//import java.util.*;

//handles "until end of turn" and "at end of turn" commands from cards
public class EndOfTurn implements java.io.Serializable
{
  private static final long serialVersionUID = -3656715295379727275L;

  private CommandList at = new CommandList();
  private CommandList until = new CommandList();
  private CommandList last = new CommandList();

  public void addAt(Command c)    {at.add(c);}
  public void addUntil(Command c) {until.add(c);}
  public void addLast(Command c) {last.add(c);}

  public void executeAt()
  {
	  AllZone.GameAction.checkWheneverKeyword(AllZone.CardFactory.HumanNullCard,"BeginningOfEndStep",null);
	  
    //Pyrohemia and Pestilence
    CardList all = new CardList();
    all.addAll(AllZone.Human_Battlefield.getCards());
    all.addAll(AllZone.Computer_Battlefield.getCards());

    CardList creature = all.getType("Creature");

    if(creature.isEmpty())
    {
      CardList sacrifice = new CardList();
      sacrifice.addAll(all.getName("Pyrohemia").toArray());
      sacrifice.addAll(all.getName("Pestilence").toArray());

      for(int i = 0; i < sacrifice.size(); i++)
        AllZone.GameAction.sacrifice(sacrifice.get(i));
    }
    
    GameActionUtil.endOfTurn_Predatory_Advantage();
    GameActionUtil.endOfTurn_Wall_Of_Reverence();
    GameActionUtil.endOfTurn_Lighthouse_Chronologist();
    GameActionUtil.endOfTurn_Thran_Quarry();
    GameActionUtil.endOfTurn_Glimmervoid();
    GameActionUtil.endOfTurn_Krovikan_Horror();
    
    //GameActionUtil.removeExaltedEffects();
    GameActionUtil.removeAttackedBlockedThisTurn();
    AllZone.GameInfo.setPreventCombatDamageThisTurn(false);
    
    AllZone.StaticEffects.rePopulateStateBasedList();
    
    /*
    PlayerZone cz = AllZone.getZone(Constant.Zone.Removed_From_Play, AllZone.ComputerPlayer);
    PlayerZone hz = AllZone.getZone(Constant.Zone.Removed_From_Play, AllZone.HumanPlayer);
    
    CardList c = new CardList(cz.getCards());
    CardList h = new CardList(hz.getCards());
    
    System.out.println("number of cards in compy removed zone: " + c.size());
    System.out.println("number of cards in human removed zone: " + h.size());
    */
    for(Card c : all) {
    	if(!c.isFaceDown()
    			&& c.getKeyword().contains("At the beginning of the end step, sacrifice CARDNAME."))
    	{
    		final Card card = c;
    		final SpellAbility sac = new Ability(card, "0") {
    			@Override
    			public void resolve() {
    				if(AllZone.GameAction.isCardInPlay(card)) AllZone.GameAction.sacrifice(card);
    			}
    		};
    		StringBuilder sb = new StringBuilder();
    		sb.append("Sacrifice ").append(card);
    		sac.setStackDescription(sb.toString());
    		
    		AllZone.Stack.add(sac);
    	}
    	if(!c.isFaceDown() && c.getKeyword().contains("At the beginning of the end step, exile CARDNAME.")) {
    		final Card card = c;
    		final SpellAbility exile = new Ability(card, "0") {
    			@Override
    			public void resolve() {
    				if(AllZone.GameAction.isCardInPlay(card)) AllZone.GameAction.exile(card);
    			}
    		};
    		StringBuilder sb = new StringBuilder();
    		sb.append("Exile ").append(card);
    		exile.setStackDescription(sb.toString());
    		
    		AllZone.Stack.add(exile);
    	}
    	if(!c.isFaceDown() && c.getKeyword().contains("At the beginning of the end step, destroy CARDNAME.")) {
    		final Card card = c;
    		final SpellAbility destroy = new Ability(card, "0") {
    			@Override
    			public void resolve() {
    				if(AllZone.GameAction.isCardInPlay(card)) AllZone.GameAction.destroy(card);
    			}
    		};
    		StringBuilder sb = new StringBuilder();
    		sb.append("Destroy ").append(card);
    		destroy.setStackDescription(sb.toString());
    		
    		AllZone.Stack.add(destroy);
    	}
    	//Berserk is using this, so don't check isFaceDown()
    	if(c.getKeyword().contains("At the beginning of the next end step, destroy CARDNAME if it attacked this turn.")) {
    		if(c.getCreatureAttackedThisTurn()) {
    		final Card card = c;
    		final SpellAbility sac = new Ability(card, "0") {
    			@Override
    			public void resolve() {
    				if(AllZone.GameAction.isCardInPlay(card)) AllZone.GameAction.sacrifice(card);
    			}
    		};
    		StringBuilder sb = new StringBuilder();
    		sb.append("Sacrifice ").append(card);
    		sac.setStackDescription(sb.toString());
    		
    		AllZone.Stack.add(sac);
    		}
    		else {
    			c.removeExtrinsicKeyword("At the beginning of the next end step, destroy CARDNAME if it attacked this turn.");
    		}
    	}
    	if( c.getKeyword().contains("An opponent gains control of CARDNAME at the beginning of the next end step.")) {
    		final Card vale = c;
    		final SpellAbility change = new Ability(vale, "0") {
    			@Override
    			public void resolve() {
    				if(AllZone.GameAction.isCardInPlay(vale)) {
    					((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                        
                        vale.setController(vale.getController().getOpponent());
                        
                        PlayerZone from = AllZone.getZone(vale);
                        from.remove(vale);
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Battlefield, vale.getController());
                        to.add(vale);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                        vale.removeExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
    				}
    			}
    		};
    		StringBuilder sb = new StringBuilder();
    		sb.append(vale.getName()).append(" changes controllers.");
    		change.setStackDescription(sb.toString());
    		
    		AllZone.Stack.add(change);
    	}
    	if(c.getCreatureAttackedThisTurn()) c.setCreatureAttackedThisTurn(false);
    }
    execute(at);
    
  }//executeAt()


  public void executeUntil() {
	  execute(until);
	  execute(last);
  }

    public int sizeAt() {return at.size();}
    public int sizeUntil() {return until.size();}
    public int sizeLast() { return last.size();}

  private void execute(CommandList c)
  {
    int length = c.size();

    for(int i = 0; i < length; i++)
      c.remove(0).execute();
  }
}
