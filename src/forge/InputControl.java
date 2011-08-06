package forge;

import java.util.*;

    //@SuppressWarnings("unused") // java.util.*
	public class InputControl extends MyObservable implements java.io.Serializable
    {
		private static final long serialVersionUID = 3955194449319994301L;
		
		private Input input;
        static int n = 0;
        private Stack<Input> inputStack = new Stack<Input>();

        public void setInput(final Input in)
        {
       if(!(input == null || input instanceof Input_StackNotEmpty))
    	   inputStack.add(in);
       else input = in;
       updateObservers();
        }
        public void resetInput()
        {
       input = null;
       updateObservers();
        }
        public Input getInput()
        {
       final String phase = AllZone.Phase.getPhase();
       final String player = AllZone.Phase.getActivePlayer();
    //System.out.println(n++ +" " +phase +" " +player);

       if(input != null)
           return input;
       
       else if(inputStack.size() > 0)
       {
    	   setInput(inputStack.pop());
    	   return input;
       }

       else if(AllZone.Stack.size() > 0)
       {
           input = new Input_StackNotEmpty();
           return input;
       }
       else if(player.equals(Constant.Player.Human))
       {
           if(phase.equals(Constant.Phase.Untap))
           {
               AllZone.Combat.reset();
               AllZone.Combat.setAttackingPlayer(Constant.Player.Human);
               AllZone.Combat.setDefendingPlayer(Constant.Player.Computer);
       
               AllZone.pwCombat.reset();
               AllZone.pwCombat.setAttackingPlayer(Constant.Player.Human);
               AllZone.pwCombat.setDefendingPlayer(Constant.Player.Computer);
       
             return new Input_Untap();
           }
           //else if(phase.equals(Constant.Phase.Upkeep))
          //return new Input_Instant(Input_Instant.YES_NEXT_PHASE, "Upkeep Phase: Play Instants and Abilities");

           else if(phase.equals(Constant.Phase.Draw))
              return new Input_Draw();

           else if(phase.equals(Constant.Phase.Main1) || phase.equals(Constant.Phase.Main2))
              return new Input_Main();
           
           else if(phase.equals(Constant.Phase.Combat_Before_Declare_Attackers_InstantAbility))
           {
        	   if (ComputerUtil.getPossibleAttackers().size() > 0)
        		   return new Input_Before_Attack_Instant();
        	   else
        	   {
        		   AllZone.Phase.setNeedToNextPhase(true);
        		   return null;
        	   }
           }
           
           else if(phase.equals(Constant.Phase.Combat_Declare_Attackers))
           {
              return new Input_Attack();
           }
           
           else if (phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility))
           {
        	   if (! skipPhase()) 
        	   {
        		   CardList list = new CardList();
        	   	   list.addAll(AllZone.Combat.getAttackers());
        	   	   //list.addAll(AllZone.pwCombat.getAttackers());
        	   	   
	        	   for (Card c : list)
	     	   		   CombatUtil.checkPropagandaEffects(c);
        	   	   
        	   	   for (Card c : list)
        	   		   CombatUtil.checkDeclareAttackers(c);
        	   	   
        		   return new Input_Attack_Instant();
        	   }
        	   else
        	   {
        		   AllZone.Phase.setNeedToNextPhase(true);
        		   return null;
        	   }
           }
           
           //this is called twice per turn, only when its human's turn
           //and then the 2nd time when its the computer's turn
           else if(phase.equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility))
           {
             if(! skipPhase())
                 return new Input_Block_Instant();
             else
             {
                
                //AllZone.Phase.nextPhase();
                //for debugging: System.out.println("need to nextPhase(InputControl,phase.equals(Combat_Declare_Blockers_InstantAbility) = true");
                  AllZone.Phase.setNeedToNextPhase(true);
                 //do not return getInput() here. There is now a check for null by this method's caller.
                  return null;
             }
           }
           else if(phase.equals(Constant.Phase.Combat_FirstStrikeDamage))
           {
             if(! skipPhase())
                 return new Input_FirstStrikeDamage();
             else
             {
                
                //AllZone.Phase.nextPhase();
                //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(Combat_Damage)) = true");
                 AllZone.Phase.setNeedToNextPhase(true);
                 //do not return getInput here. There is now a check for null by this method's caller.
                 //return getInput();
                 return null;
             }
           }
           else if(phase.equals(Constant.Phase.Combat_Damage))
           {
             if(! skipPhase())
                 return new Input_CombatDamage();
             else
             {
                
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
           else if (phase.equals(Constant.Phase.End_Of_Combat))
           {
        	   AllZone.EndOfCombat.executeAt();
        	   AllZone.Phase.setNeedToNextPhase(true);
        	   return null;
           }
           else if(phase.equals(Constant.Phase.At_End_Of_Turn))
           {
            AllZone.EndOfTurn.executeAt();
           
            //AllZone.Phase.nextPhase();
            //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(At_End_Of_Turn)) = true");
            AllZone.Phase.setNeedToNextPhase(true);
            //do not return getInput() here. There is now a check for null by this method's caller
            //return getInput();
            return null;
           }
           else if(phase.equals(Constant.Phase.End_Of_Turn))
           {
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
        	  
              if(AllZone.Display.stopEOT())
                return new Input_EOT();
              else
              {   
               //AllZone.Phase.nextPhase();
               //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(End_Of_Turn)) = true");
               AllZone.Phase.setNeedToNextPhase(true);
                //do not return getInput() here. There is now a check for null in this method's caller.
               //return getInput();
               return null;
              }
           }
           else if(phase.equals(Constant.Phase.Until_End_Of_Turn))
           {
            AllZone.EndOfTurn.executeUntil();
           
            //AllZone.Phase.nextPhase();
            //for debugging: System.out.println("need to nextPhase(InputControl.getInput(),phase.equals(Until_End_Of_Turn)) = true");
            AllZone.Phase.setNeedToNextPhase(true);
            //do not return getInput() here. There is now a check for null by this method's caller.
            //return getInput();
            return null;
           }
           else if(phase.equals(Constant.Phase.Cleanup))
          return new Input_Cleanup();

           //takes place during the computer's turn, like blocking etc...
           else if(phase.equals(Constant.Phase.Combat_Declare_Blockers))
           {
          if(! skipPhase()) 
          {
        	  /*
			   CardList list = new CardList();
		   	   list.addAll(AllZone.Combat.getAttackers());
		   	   list.addAll(AllZone.pwCombat.getAttackers());
		   	   for (Card c : list)
		   	   {
		   		   CombatUtil.checkDeclareAttackers(c);
		   	   }
		   	   */
               return new Input_Block();
          }
          else
          {
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
       else
       {  //computer
           if(phase.equals(Constant.Phase.Untap))
           {
	          AllZone.Combat.reset();
	          AllZone.Combat.setAttackingPlayer(Constant.Player.Computer);
	          AllZone.Combat.setDefendingPlayer(Constant.Player.Human);
	
              AllZone.pwCombat.reset();
              AllZone.pwCombat.setAttackingPlayer(Constant.Player.Computer);
              AllZone.pwCombat.setDefendingPlayer(Constant.Player.Human);
	
	          return new Input_Untap();
	       }
           else if(phase.equals(Constant.Phase.Draw))
        	   return new Computer_Draw();

           else if(phase.equals(Constant.Phase.Cleanup))
           {
                  return new Computer_Cleanup();
           }
           else if (phase.equals(Constant.Phase.Combat_Declare_Blockers))
           {
    		   CardList list = new CardList();
    	   	   list.addAll(AllZone.Combat.getAttackers());
    	   	   //list.addAll(AllZone.pwCombat.getAttackers());
    	   	   
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
        	   return AllZone.Computer;
       }//Computer

       return new Input()
       {
		private static final long serialVersionUID = 7378891097354547936L;

		public void showMessage()
                {
                  AllZone.Display.showMessage("InputControl : Error nothing found");
                  throw new RuntimeException("InputControl : getInput() error, should not be here, phase " +phase +", player " +player);
                }
       };
        }//getInput()
        private boolean skipPhase()
        {
          CardList check = new CardList();
          check.addAll(AllZone.Combat.getAttackers());
          check.addAll(AllZone.pwCombat.getAttackers());

          CardList all = new CardList();
          all.addAll(AllZone.Computer_Play.getCards());
          all.addAll(AllZone.Human_Play.getCards());
          all = all.filter(new CardListFilter(){
			public boolean addCard(Card c) {
				return c.isCreature() && c.getTotalAssignedDamage() > 0;
			}
        	  
          });
          
          return check.size() == 0 && all.size() == 0;
        }
    }//InputControl
