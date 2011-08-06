
package forge;


import java.util.Stack;


//@SuppressWarnings("unused") // java.util.*
public class InputControl extends MyObservable implements java.io.Serializable {
    private static final long serialVersionUID = 3955194449319994301L;
    
    private Input             input;
    static int                n                = 0;
    private Stack<Input>      inputStack       = new Stack<Input>();
    private boolean appliedExaltedEffects = false;
    
    public void setInput(final Input in) {
        if(!(input == null || input instanceof Input_StackNotEmpty)) inputStack.add(in);
        else input = in;
        updateObservers();
    }
    
    public void resetInput() {
        input = null;
        updateObservers();
    }
    
    public Input getInput() {
        final String phase = AllZone.Phase.getPhase();
        final Player player = AllZone.Phase.getActivePlayer();
        //System.out.println(n++ +" " +phase +" " +player);
        
        if(input != null) return input;
        
        else if(inputStack.size() > 0) {
            setInput(inputStack.pop());
            return input;
        }

        else if(AllZone.Stack.size() > 0) {
            input = new Input_StackNotEmpty();
            return input;
        } else if(player.isHuman()) {
            if(phase.equals(Constant.Phase.Untap)) {
                AllZone.Combat.reset();
                AllZone.Combat.setAttackingPlayer(AllZone.HumanPlayer);
                AllZone.Combat.setDefendingPlayer(AllZone.ComputerPlayer);
                
                AllZone.pwCombat.reset();
                AllZone.pwCombat.setAttackingPlayer(AllZone.HumanPlayer);
                AllZone.pwCombat.setDefendingPlayer(AllZone.ComputerPlayer);
                
                return new Input_Untap();
            }
            //else if(phase.equals(Constant.Phase.Upkeep))
            //return new Input_Instant(Input_Instant.YES_NEXT_PHASE, "Upkeep Phase: Play Instants and Abilities");
            
            else if(phase.equals(Constant.Phase.Draw)) return new Input_Draw();
            
            else if(phase.equals(Constant.Phase.Main1) || phase.equals(Constant.Phase.Main2)) return new Input_Main();
            
            else if(phase.equals(Constant.Phase.Combat_Before_Declare_Attackers_InstantAbility)) {
                if(ComputerUtil.getPossibleAttackers().size() > 0) return new Input_Before_Attack_Instant();
                else {
                    AllZone.Phase.setNeedToNextPhase(true);
                    return null;
                }
            }

            else if(phase.equals(Constant.Phase.Combat_Declare_Attackers)) {
                appliedExaltedEffects = false;
                return new Input_Attack();
            }

            else if(phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)) {
                if(!skipPhase()) {
                    CardList list = new CardList();
                    list.addAll(AllZone.Combat.getAttackers());
                    list.addAll(AllZone.pwCombat.getAttackers());
                    
                    //check for exalted:
                    if ((AllZone.Combat.getDeclaredAttackers() + AllZone.pwCombat.getDeclaredAttackers() == 1) &&
                    		!appliedExaltedEffects) {
                    	
                    	AllZone.GameAction.CheckWheneverKeyword(list.get(0), "Attack - Alone", null);
                    	
//                    if (list.size()==1) {
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
                        appliedExaltedEffects = true;
                    }
                    
                    for(Card c:list)
                        CombatUtil.checkPropagandaEffects(c);
                    
                    for(Card c:list)
                        CombatUtil.checkDeclareAttackers(c);
                    
                    return new Input_Attack_Instant();
                } else {
                    AllZone.Phase.setNeedToNextPhase(true);
                    return null;
                }
            }

            //this is called twice per turn, only when its human's turn
            //and then the 2nd time when its the computer's turn
            else if(phase.equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility)) {
                if(!skipPhase()) return new Input_Block_Instant();
                else {
                    
                	/*
                	CardList list = new CardList();
                    list.add(AllZone.Combat.getAllBlockers());
                    list.add(AllZone.pwCombat.getAllBlockers());
                	*/
                    
                	
                    //AllZone.Phase.nextPhase();
                    //for debugging: System.out.println("need to nextPhase(InputControl,phase.equals(Combat_Declare_Blockers_InstantAbility) = true");
                    AllZone.Phase.setNeedToNextPhase(true);
                    //do not return getInput() here. There is now a check for null by this method's caller.
                    return null;
                }
            }
            
            else if (phase.equals(Constant.Phase.Combat_After_Declare_Blockers))
            {
                    CardList list = new CardList();
                    list.addAll(AllZone.Combat.getAllBlockers().toArray());
                    list.addAll(AllZone.pwCombat.getAllBlockers().toArray());
                    
                    CardList attList = new CardList();
                    attList.addAll(AllZone.Combat.getAttackers());
                    attList.addAll(AllZone.pwCombat.getAttackers());

                    CombatUtil.checkDeclareBlockers(list);
                    
                    /*
                    for(Card c:list)
                        CombatUtil.checkDeclareBlockers(c);
                        */
                    
                    for (Card a:attList){
                    	CardList blockList = AllZone.Combat.getBlockers(a);
                    	for (Card b:blockList)
                    		CombatUtil.checkBlockedAttackers(a, b);
                    }
                
            		AllZone.Phase.setNeedToNextPhase(true);
            }
            
            
            else if(phase.equals(Constant.Phase.Combat_FirstStrikeDamage)) {
                if(!skipPhase()) return new Input_FirstStrikeDamage();
                else {
                    
                    //AllZone.Phase.nextPhase();
                    //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(Combat_Damage)) = true");
                    AllZone.Phase.setNeedToNextPhase(true);
                    //do not return getInput here. There is now a check for null by this method's caller.
                    //return getInput();
                    return null;
                }
            } else if(phase.equals(Constant.Phase.Combat_Damage)) {
                if(!skipPhase()) return new Input_CombatDamage();
                else {
                    
                    //AllZone.Phase.nextPhase();
                    //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(Combat_Damage)) = true");
                    //System.out.println("stack size:" +AllZone.Stack.size()); 
                    
                    AllZone.Phase.setNeedToNextPhase(true);
                    
                    //do not return getInput here. There is now a check for null by this method's caller.
                    //return getInput();
                    return null;
                }
            }
            /*
            else if (phase.equals(Constant.Phase.End_Of_Combat))
            {
              if (! skipPhase())
            	  return new Input_EndOfCombat();
              else
              {
            	  AllZone.Phase.setNeedToNextPhase(true);
              }
              return null;
            }
            */
            else if(phase.equals(Constant.Phase.End_Of_Combat)) {
            	AllZone.EndOfCombat.executeUntil();
                AllZone.EndOfCombat.executeAt();
                AllZone.Phase.setNeedToNextPhase(true);
                appliedExaltedEffects = false;
                return null;
            } else if(phase.equals(Constant.Phase.At_End_Of_Turn)) {
                AllZone.EndOfTurn.executeAt();
                
                //AllZone.Phase.nextPhase();
                //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(At_End_Of_Turn)) = true");
                AllZone.Phase.setNeedToNextPhase(true);
                //do not return getInput() here. There is now a check for null by this method's caller
                //return getInput();
                return null;
            } else if(phase.equals(Constant.Phase.End_Of_Turn)) {
                //System.out.println("Cache size: " + ImageCache.cache.size());
                
                /*
                CardList visibleCards = new CardList();
                PlayerZone hPlay = AllZone.Human_Play;
                PlayerZone hand = AllZone.Human_Hand;
                PlayerZone cPlay = AllZone.Computer_Play;
                
                visibleCards.addAll(hPlay.getCards());
                visibleCards.addAll(hand.getCards());
                visibleCards.addAll(cPlay.getCards());
                
                ArrayList<String> list = new ArrayList<String>();
                
                Iterator<String> iter = ImageCache.cache.keySet().iterator();
                while(iter.hasNext()) {
                	String cardName = iter.next();	
                	if ( !(cardName.startsWith("Swamp") || cardName.startsWith("Mountain") || cardName.startsWith("Island")
                		|| cardName.startsWith("Plains") || cardName.startsWith("Forest")) &&
                		visibleCards.getImageName(cardName).size() == 0 ) {
                		list.add(cardName);
                	}
                }
                
                for (String s : list)
                {
                ImageCache.cache.remove(s);
                	System.out.println("Removing " + s + " from cache.");
                }
                */

                if(AllZone.Display.stopEOT()) return new Input_EOT();
                else {
                    //AllZone.Phase.nextPhase();
                    //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(End_Of_Turn)) = true");
                    AllZone.Phase.setNeedToNextPhase(true);
                    //do not return getInput() here. There is now a check for null in this method's caller.
                    //return getInput();
                    return null;
                }
            } else if(phase.equals(Constant.Phase.Until_End_Of_Turn)) {
                AllZone.EndOfTurn.executeUntil();
                
                //AllZone.Phase.nextPhase();
                //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(Until_End_Of_Turn)) = true");
                AllZone.Phase.setNeedToNextPhase(true);
                //do not return getInput() here. There is now a check for null by this method's caller.
                //return getInput();
                return null;
            } else if(phase.equals(Constant.Phase.Cleanup)) return new Input_Cleanup();
            
            //takes place during the computer's turn, like blocking etc...
            else if(phase.equals(Constant.Phase.Combat_Declare_Blockers)) {
                if(!skipPhase()) {
                    /*
                     CardList list = new CardList();
                       list.addAll(AllZone.Combat.getAttackers());
                       list.addAll(AllZone.pwCombat.getAttackers());
                       for (Card c : list)
                       {
                    	   CombatUtil.checkDeclareAttackers(c);
                       }
                       */
                    //return new Input_Block();
                    if(AllZone.pwCombat.getAttackers().length != 0) return new Input_Block_Planeswalker();
                    else return new Input_Block();
                } else {
                    //AllZone.Phase.nextPhase();
                    //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(Combat_Declare_Blockers)) = true");
                    AllZone.Phase.setNeedToNextPhase(true);
                    //do not return getInput here. There is now a check for null by this method's caller.
                    //return getInput();
                    return null;
                }
            }
            //       else if(phase.equals(Constant.Phase.End_Of_Turn))
            //      return new Input_Instant(Input_Instant.YES_NEXT_PHASE, "End of Computer's Turn: Play Instants and Abilities");
            
        }//Human
        else { //computer
            if(phase.equals(Constant.Phase.Untap)) {
                AllZone.Combat.reset();
                AllZone.Combat.setAttackingPlayer(AllZone.ComputerPlayer);
                AllZone.Combat.setDefendingPlayer(AllZone.HumanPlayer);
                
                AllZone.pwCombat.reset();
                AllZone.pwCombat.setAttackingPlayer(AllZone.ComputerPlayer);
                AllZone.pwCombat.setDefendingPlayer(AllZone.HumanPlayer);
                
                return new Input_Untap();
            } 
            
            else if(phase.equals(Constant.Phase.Draw))
				return new Input_Draw();
            
            else if(phase.equals(Constant.Phase.Cleanup)) {
                return new Computer_Cleanup();
            }
            /*
            else if (phase.equals(Constant.Phase.Combat_Declare_Blockers))
            {
               CardList list = new CardList();
            	   list.addAll(AllZone.Combat.getAttackers());
            	   list.addAll(AllZone.pwCombat.getAttackers());
            	   
            	   //check for exalted:
            	   if (list.size() == 1)
            	   {
               		String attackingPlayer = AllZone.Combat.getAttackingPlayer();
            		PlayerZone play = AllZone.getZone(Constant.Zone.Play, attackingPlayer);
            		CardList exalted = new CardList(play.getCards());
            		exalted = exalted.filter(new CardListFilter(){
            			public boolean addCard(Card c)
            			{
            				return c.getKeyword().contains("Exalted");
            			}
            		});
            		if (exalted.size() > 0)
            			CombatUtil.executeExaltedAbility(list.get(0), exalted.size());
            	   }
            	   
               for (Card c : list)
             	   {
             		   CombatUtil.checkPropagandaEffects(c);
             	   }
            	   for (Card c : list)
            	   {
            		   CombatUtil.checkDeclareAttackers(c);
            	   }
               return AllZone.Computer;
            }
            else
            */
            return AllZone.Computer;
        }//Computer
        
        return new Input() {
            private static final long serialVersionUID = 7378891097354547936L;
            
            @Override
            public void showMessage() {
                AllZone.Display.showMessage("InputControl : Error nothing found");
                throw new RuntimeException("InputControl : getInput() error, should not be here, phase " + phase
                        + ", player " + player);
            }
        };
    }//getInput()
    
    private boolean skipPhase() {
        CardList check = new CardList();
        check.addAll(AllZone.Combat.getAttackers());
        check.addAll(AllZone.pwCombat.getAttackers());
        
        CardList all = new CardList();
        all.addAll(AllZone.Computer_Play.getCards());
        all.addAll(AllZone.Human_Play.getCards());
        all = all.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isCreature() && c.getTotalAssignedDamage() > 0;
            }
            
        });
        
        return check.size() == 0 && all.size() == 0;
    }
}//InputControl
