package forge;

import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;

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
    CardList all = AllZoneUtil.getCardsInPlay();

    GameActionUtil.endOfTurn_Predatory_Advantage();
    GameActionUtil.endOfTurn_Wall_Of_Reverence();
    GameActionUtil.endOfTurn_Lighthouse_Chronologist();
    GameActionUtil.endOfTurn_Krovikan_Horror();
    
    GameActionUtil.removeAttackedBlockedThisTurn();
    AllZone.GameInfo.setPreventCombatDamageThisTurn(false);
    
    AllZone.StaticEffects.rePopulateStateBasedList();

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

            AllZone.Stack.addSimultaneousStackEntry(sac);

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

            AllZone.Stack.addSimultaneousStackEntry(exile);

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

            AllZone.Stack.addSimultaneousStackEntry(destroy);

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

            AllZone.Stack.addSimultaneousStackEntry(sac);

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
    					AllZone.GameAction.changeController(new CardList(vale), vale.getController(), vale.getController().getOpponent());

                        vale.removeExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
    				}
    			}
    		};
    		StringBuilder sb = new StringBuilder();
    		sb.append(vale.getName()).append(" changes controllers.");
    		change.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(change);

    	}
    	if(c.getName().equals("Erg Raiders") && !c.getCreatureAttackedThisTurn() &&
    			!(c.getTurnInZone() == AllZone.Phase.getTurn()) && AllZone.Phase.isPlayerTurn(c.getController())) {
    		final Card raider = c;
    		final SpellAbility change = new Ability(raider, "0") {
    			@Override
    			public void resolve() {
    				if(AllZone.GameAction.isCardInPlay(raider)) {
    					raider.getController().addDamage(2, raider);
    				}
    			}
    		};
    		StringBuilder sb = new StringBuilder();
    		sb.append(raider.getName()).append(" deals 2 damage to controller.");
    		change.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(change);

    	}
    	if(c.hasKeyword("At the beginning of your end step, sacrifice this creature unless it attacked this turn.")
    			&& !c.getCreatureAttackedThisTurn()
    			/* && !(c.getTurnInZone() == AllZone.Phase.getTurn())*/
    			&& AllZone.Phase.isPlayerTurn(c.getController())) {
    		final Card source = c;
    		final SpellAbility change = new Ability(source, "0") {
    			@Override
    			public void resolve() {
    				if(AllZone.GameAction.isCardInPlay(source)) {
    					AllZone.GameAction.sacrifice(source);
    				}
    			}
    		};
    		StringBuilder sb = new StringBuilder();
    		sb.append(source.getName()).append(" - sacrifice ").append(source.getName()).append(".");
    		change.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(change);

    	}
    	
    }


    execute(at);



    CardList all2 = AllZoneUtil.getCardsInPlay();
    for(Card c:all2) {
    	if(c.getCreatureAttackedThisTurn()) c.setCreatureAttackedThisTurn(false);
    }
        
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
