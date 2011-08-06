package forge;

import java.util.HashMap;

class CardFactory_Planeswalkers {
	public static Card getCard(final Card card, String cardName, String owner)
	{
		 //*************** START *********** START **************************
	    if(cardName.equals("Elspeth, Knight-Errant"))
	    {
	      //computer only plays ability 1 and 3, gain life and put X\X token into play
	      final int turn[] = new int[1];
	      turn[0] = -1;
	
	      final Card card2 = new Card()
	      {
	        public void addDamage(int n)
	        {
	          subtractCounter(Counters.LOYALTY, n);
	          AllZone.GameAction.checkStateEffects();
	        }
	      };
	      card2.addCounter(Counters.LOYALTY, 4);
	
	      card2.setOwner(owner);
	      card2.setController(owner);
	
	      card2.setName(card.getName());
	      card2.setType(card.getType());
	      card2.setManaCost(card.getManaCost());
	      card2.addSpellAbility(new Spell_Permanent(card2));
	
	      //ability2: target creature gets +3/+3 and flying until EOT
	      final SpellAbility ability2 = new Ability(card2, "0")
	      {
	   	   
	   	   public void resolve()
	          {
	   		   
	   		 card2.addCounter(Counters.LOYALTY, 1);
	   		 
	   		 turn[0] = AllZone.Phase.getTurn();
	   		 
	            final Command eot = new Command()
	            {
				   private static final long serialVersionUID = 94488363210770877L;
	
				   public void execute()
	              {
	                Card c = getTargetCard();
	                if(AllZone.GameAction.isCardInPlay(c))
	                {
	                  c.addTempAttackBoost(-3);
	                  c.addTempDefenseBoost(-3);
	                  c.removeExtrinsicKeyword("Flying");
	                }
	              }//execute()
	            };//Command
	
	            Card c = getTargetCard();
	            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card2, c))
	            {
	              c.addTempAttackBoost(3);
	              c.addTempDefenseBoost(3);
	              c.addExtrinsicKeyword("Flying");
	
	              AllZone.EndOfTurn.addUntil(eot);
	            }
	          }//resolve()
	
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	
	        public boolean canPlay()
	        {
	          @SuppressWarnings("unused") // library
			   PlayerZone library = AllZone.getZone(Constant.Zone.Library, card2.getController());
	
	          return 	  0 < card2.getCounters(Counters.LOYALTY) &&
	                      AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                      turn[0] != AllZone.Phase.getTurn() &&
	                      AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                      !AllZone.Phase.getPhase().equals("End of Turn") && 
	                      (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2")) 
	                      && AllZone.Stack.size() == 0 ;
	          			
	          
	        }//canPlay()
	      };//SpellAbility ability2
	
	      ability2.setBeforePayMana(new Input()
	      {
			private static final long serialVersionUID = 9062830120519820799L;
			
			int check = -1;
	         public void showMessage()
	         {
	           if(check != AllZone.Phase.getTurn())
	           {
	             check = AllZone.Phase.getTurn();
	             turn[0] = AllZone.Phase.getTurn();
	             
	             
	             AllZone.Stack.push(ability2);
	           }
	           stop();
	         }//showMessage()
	      });
	      
	      
	
	      //ability3
	      final SpellAbility ability3 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY, 8);
	          turn[0] = AllZone.Phase.getTurn();
	
	          //make all permanents in play/hand/library and graveyard	
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          
	          CardList list = new CardList();
	          list.addAll(play.getCards());
	          list.addAll(hand.getCards());
	          list.addAll(library.getCards());
	          list.addAll(grave.getCards());
	          
	          
	          for (int i=0;i < list.size(); i++)
	          {
	       	   Card c = list.get(i);
	       	   if (c.isPermanent() && !c.isPlaneswalker())
	       	   {
	       		   c.addExtrinsicKeyword("Indestructible");
	       	   }
	       	   
	          }
	          
	        }
	        public boolean canPlay()
	        {
	          return 8 <= card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	              turn[0] != AllZone.Phase.getTurn() &&
	              AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	              !AllZone.Phase.getPhase().equals("End of Turn") && 
	              (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	              && AllZone.Stack.size() == 0;
	        }//canPlay()
	        public boolean canPlayAI()
	        {
	                return true;
	        }
	      };
	      ability3.setBeforePayMana(new Input()
	      {
			  private static final long serialVersionUID = -2054686425541429389L;
			
			  int check = -1;
	         public void showMessage()
	         {
	           if(check != AllZone.Phase.getTurn())
	           {
	             check = AllZone.Phase.getTurn();
	             turn[0] = AllZone.Phase.getTurn();
	             AllZone.Stack.push(ability3);
	           }
	           stop();
	         }//showMessage()
	      });
	
	      //ability 1: create white 1/1 token
	      final SpellAbility ability1 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.addCounter(Counters.LOYALTY, 1);
	          turn[0] = AllZone.Phase.getTurn();
	          
	          Card c = new Card();
	
	          c.setOwner(card2.getController());
	          c.setController(card2.getController());
	
	          c.setManaCost("W");
	          c.setToken(true);
	          
	          c.setImageName("W 1 1 Soldier");
	          c.setName("Soldier");
	          c.addType("Creature");
	          c.addType("Soldier");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);
	          //AllZone.GameAction.getPlayerLife(card.getController()).addLife(2);
	          
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	          play.add(c);
	        }
	        public boolean canPlayAI()
	        {
	          if(ability3.canPlay() && ability3.canPlayAI()) {
	            return false;
	          } else
	          {
	            return true;
	          }
	        }
	        public boolean canPlay()
	        {
	          return 0 < card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	              turn[0] != AllZone.Phase.getTurn() &&
	              AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	              !AllZone.Phase.getPhase().equals("End of Turn") && 
	              (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	              && AllZone.Stack.size() == 0;
	        }//canPlay()
	      };//SpellAbility ability1
	
	      ability1.setBeforePayMana(new Input()
	      {
			  private static final long serialVersionUID = -7892114885686285881L;
			  
			  int check = -1;
	         public void showMessage()
	         {
	           if(check != AllZone.Phase.getTurn())
	           {
	             check = AllZone.Phase.getTurn();
	             turn[0] = AllZone.Phase.getTurn();
	             AllZone.Stack.push(ability1);
	           }
	           stop();
	         }//showMessage()
	      });
	
	      ability1.setDescription("+1: Put a white 1/1 Soldier creature token into play.");
	      ability1.setStackDescription("Elspeth, Knight-Errant - put 1/1 token into play.");
	      card2.addSpellAbility(ability1);
	
	      ability2.setDescription("+1: Target creature gets +3/+3 and gains flying until end of turn.");
	      ability2.setStackDescription("Elspeth, Knight-Errant - creature gets +3/+3 and Flying until EOT.");
	      ability2.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability2));
	      
	      card2.addSpellAbility(ability2);
	     
	      ability3.setDescription("-8: For the rest of the game, artifacts, creatures, enchantments, and lands you control are indestructible.");
	      ability3.setStackDescription("Elspeth, Knight-Errant - Make everything indestructible.");
	      card2.addSpellAbility(ability3);
	
	      return card2;
	    }
	    //*************** END ************ END **************************
	    
	     
	   //*************** START *********** START **************************
	     else if(cardName.equals("Nissa Revane"))
	     {
	       final int turn[] = new int[1];
	       turn[0] = -1;
	
	       final Card card2 = new Card()
	       {
	         public void addDamage(int n)
	         {
	           subtractCounter(Counters.LOYALTY, n);
	           AllZone.GameAction.checkStateEffects();
	         }
	       };
	       card2.addCounter(Counters.LOYALTY, 2);
	
	       card2.setOwner(owner);
	       card2.setController(owner);
	
	       card2.setName(card.getName());
	       card2.setType(card.getType());
	       card2.setManaCost(card.getManaCost());
	       card2.addSpellAbility(new Spell_Permanent(card2));
	
	       //ability2: gain 2 life for each elf controlled
	       final SpellAbility ability2 = new Ability(card2, "0")
	       {
	    	   
	    	   public void resolve()
	           {
	    		   
	    		 card2.addCounter(Counters.LOYALTY, 1);
	    		 
	    		 turn[0] = AllZone.Phase.getTurn();
	    		 
	    		 PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	    		 CardList elves = new CardList(play.getCards());
	    		 elves = elves.getType("Elf");
	
	    		 AllZone.GameAction.getPlayerLife(card.getController()).addLife(2*elves.size());
	             
	           }//resolve()
	
	         public boolean canPlayAI()
	         {
	        	 PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
	
	        	 CardList chosens = new CardList(lib.getCards());
	    		 chosens = chosens.getName("Nissa's Chosen");
	    		 
	    		 if (chosens.size() > 0)
	    			 return false;
	    		 
	    		 return true;
	         }
	
	         public boolean canPlay()
	         {
	           @SuppressWarnings("unused") // library
			   PlayerZone library = AllZone.getZone(Constant.Zone.Library, card2.getController());
	
	           return 0 < card2.getCounters(Counters.LOYALTY) &&
	                       AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                       turn[0] != AllZone.Phase.getTurn() &&
	                       AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                       !AllZone.Phase.getPhase().equals("End of Turn") &&
	                       (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                       && AllZone.Stack.size() == 0;
	           
	         }//canPlay()
	       };//SpellAbility ability2
	
	       ability2.setBeforePayMana(new Input()
	       {
			
			
			private static final long serialVersionUID = 2828718386226165026L;
			int check = -1;
	          public void showMessage()
	          {
	            if(check != AllZone.Phase.getTurn())
	            {
	              check = AllZone.Phase.getTurn();
	              turn[0] = AllZone.Phase.getTurn();
	              
	              
	              AllZone.Stack.push(ability2);
	            }
	            stop();
	          }//showMessage()
	       });
	       
	
	       //ability3
	       final SpellAbility ability3 = new Ability(card2, "0")
	       {
	         public void resolve()
	         {
	           card2.subtractCounter(Counters.LOYALTY, 7);
	           turn[0] = AllZone.Phase.getTurn();
	
	           //make all permanents in play/hand/library and graveyard	
	           PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	           PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	           
	           CardList list = new CardList();;
	           list.addAll(library.getCards());
	           list = list.getType("Elf");
	           
	           //currently, just adds all elves into play.
	           for (int i=0;i < list.size(); i++)
	           {
	        	   Card c = list.get(i);
	        	   if (c.isCreature() )
	        	   {
	        		   library.remove(c);
	        		   play.add(c);
	        	   }
	           }
	         }
	         public boolean canPlay()
	         {
	           return 7 <= card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	               turn[0] != AllZone.Phase.getTurn() &&
	               AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	               !AllZone.Phase.getPhase().equals("End of Turn") &&
	               (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	               && AllZone.Stack.size() == 0;
	         }//canPlay()
	         public boolean canPlayAI()
	         {
	        	 PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
	    		 
	        	 CardList elves = new CardList(lib.getCards());
	    		 elves = elves.getType("Elf");
	    		 
	    		 return elves.size() > 3;
	         }
	       };
	       ability3.setBeforePayMana(new Input()
	       {
	
			private static final long serialVersionUID = -7189927522150479572L;
			int check = -1;
	          public void showMessage()
	          {
	            if(check != AllZone.Phase.getTurn())
	            {
	              check = AllZone.Phase.getTurn();
	              turn[0] = AllZone.Phase.getTurn();
	              AllZone.Stack.push(ability3);
	            }
	            stop();
	          }//showMessage()
	       });
	
	       //ability 1: search for Nessa's Chosen
	       final SpellAbility ability1 = new Ability(card2, "0")
	       {
	         public void resolve()
	         {
	           card2.addCounter(Counters.LOYALTY, 1);
	           turn[0] = AllZone.Phase.getTurn();
	           
	           if (card2.getController().equals(Constant.Player.Human)){
		           Object check = AllZone.Display.getChoiceOptional("Search for Nissa's Chosen", AllZone.Human_Library.getCards());
		           if(check != null)
		           {
		        	 Card c = (Card)check;
		        	   
		             PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
		             PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card2.getController());
		             
		             if (c.getName().equals("Nissa's Chosen"))
		             {
			             lib.remove(c);
			             play.add(c);
		             }
		           }
		           AllZone.GameAction.shuffle(Constant.Player.Human);
	           }//human
	           else
	           {
	        	   PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card2.getController());
	        	   PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	        	   CardList nissas = new CardList(lib.getCards());
	        	   nissas = nissas.getName("Nissa's Chosen");
	        	   
	        	   if (nissas.size() > 0)
	        	   {
	        		   Card nissa = nissas.get(0);
	        		   lib.remove(nissa);
	        		   play.add(nissa);
	        	   }
	        	   AllZone.GameAction.shuffle(Constant.Player.Computer);
	           }
	
	         }
	         public boolean canPlayAI()
	         {
	           if(ability3.canPlay() && ability3.canPlayAI()) {
	             return false;
	           } else
	           {
	             return true;
	           }
	         }
	         public boolean canPlay()
	         {
	           return 0 < card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	               turn[0] != AllZone.Phase.getTurn() &&
	               AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	               !AllZone.Phase.getPhase().equals("End of Turn") && 
	               (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	               && AllZone.Stack.size() == 0;
	         }//canPlay()
	       };//SpellAbility ability1
	
	       ability1.setBeforePayMana(new Input()
	       {
			  
			private static final long serialVersionUID = 7668642820407492396L;
			int check = -1;
	          public void showMessage()
	          {
	            if(check != AllZone.Phase.getTurn())
	            {
	              check = AllZone.Phase.getTurn();
	              turn[0] = AllZone.Phase.getTurn();
	              AllZone.Stack.push(ability1);
	            }
	            stop();
	          }//showMessage()
	       });
	
	       ability1.setDescription("+1: Search your library for a card named Nissa's Chosen and put it onto the battlefield. Then shuffle your library.");
	       ability1.setStackDescription("Nissa Revane - Search for a card named Nissa's Chosen and put it onto the battlefield.");
	       card2.addSpellAbility(ability1);
	
	       ability2.setDescription("+1: You gain 2 life for each Elf you control.");
	       ability2.setStackDescription("Nissa Revane - You gain 2 life for each Elf you control.");
	       
	       card2.addSpellAbility(ability2);
	      
	       ability3.setDescription("-7: Search your library for any number of Elf creature cards and put them onto the battlefield. Then shuffle your library.");
	       ability3.setStackDescription("Nissa Revane - Search your library for any number of Elf creature cards and put them onto the battlefield. Then shuffle your library.");
	       card2.addSpellAbility(ability3);
	
	       return card2;
	     }
	     //*************** END ************ END **************************
	     
	   //*************** START *********** START **************************
	     else if(cardName.equals("Nicol Bolas, Planeswalker"))
	     {
	       
	       final int turn[] = new int[1];
	       turn[0] = -1;
	
	       final Card card2 = new Card()
	       {
	         public void addDamage(int n)
	         {
	           subtractCounter(Counters.LOYALTY, n);
	           AllZone.GameAction.checkStateEffects();
	         }
	       };
	       card2.addCounter(Counters.LOYALTY, 5);
	
	       card2.setOwner(owner);
	       card2.setController(owner);
	
	       card2.setName(card.getName());
	       card2.setType(card.getType());
	       card2.setManaCost(card.getManaCost());
	       card2.addSpellAbility(new Spell_Permanent(card2));
	       
	       //ability3
	       final SpellAbility ability3 = new Ability(card2, "0")
	       {
	         public void resolve()
	         {
	           card2.subtractCounter(Counters.LOYALTY, 9);
	           turn[0] = AllZone.Phase.getTurn();
	
	           String player = card2.getController();
	           String opponent = AllZone.GameAction.getOpponent(player);
	           
	           PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);
	           CardList oppPerms = new CardList(play.getCards());
	           
	           PlayerLife life = AllZone.GameAction.getPlayerLife(opponent);
	    	   life.subtractLife(7);
	    	   
	    	   for (int j=0; j<7; j++)
	    	   {
	    		   //will not actually let human choose which cards to discard
	    		   AllZone.GameAction.discardRandom(opponent);
	    	   }
	    	   
	    	   CardList permsToSac = new CardList();
	    	   CardList oppPermTempList = new CardList(play.getCards());
	           
	           if (player.equals("Human"))
	           {
	        	   for(int k=0; k < oppPerms.size(); k++)
	        	   {
	        		   Card c = oppPerms.get(k);
	        		   
	        		   permsToSac.add(c);
	        		   
	        		   if (k == 6)
	        			   break;
	        	   }
	           }
	           
	           else //computer
	           {
	        	   Object o = null;
	        	   for(int k=0; k < oppPerms.size(); k++)
	        	   {
	        		   o = AllZone.Display.getChoiceOptional("Select Card to sacrifice", oppPermTempList.toArray());
	        		   Card c = (Card)o;
	        		   //AllZone.GameAction.sacrifice(c);
	        		   permsToSac.add(c);
	        		   oppPermTempList.remove(c);
	        		   
	        		           		   
	        		   if (k == 6)
	        			   break;
	        	   }
	           }
	           for(int m=0;m<permsToSac.size();m++)
	           {
	        	   AllZone.GameAction.sacrifice(permsToSac.get(m));
	           }
	
	         }
	         public boolean canPlay()
	         {
	           SpellAbility sa;
	    	   for (int i=0; i<AllZone.Stack.size(); i++)
	    	   {
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card2))
	    	    	          return false;
	    	   }
	           return 9 <= card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	               turn[0] != AllZone.Phase.getTurn() &&
	               AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	               !AllZone.Phase.getPhase().equals("End of Turn") &&
	               (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	               && AllZone.Stack.size() == 0;
	         }//canPlay()
	         public boolean canPlayAI()
	         {
	        	 return true;
	         }
	       };
	       ability3.setBeforePayMana(new Input()
	       {
			  private static final long serialVersionUID = 2946754243072466628L;
			  
			  int check = -1;
	          public void showMessage()
	          {
	            if(check != AllZone.Phase.getTurn())
	            {
	              check = AllZone.Phase.getTurn();
	              turn[0] = AllZone.Phase.getTurn();
	              AllZone.Stack.push(ability3);
	            }
	            stop();
	          }//showMessage()
	       });
	       
	       final SpellAbility ability2 = new Ability(card2, "0")
	       {
	    	   
	    	   public void resolve()
	           {
	    		   
	    		 card2.subtractCounter(Counters.LOYALTY, 2);
	    		 
	    		 turn[0] = AllZone.Phase.getTurn();
	    		 
	    		 Card c = getTargetCard();
	             if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card2,c))
	             {
	              //set summoning sickness
	               if(c.getKeyword().contains("Haste")){
	                 c.setSickness(false);
	               }
	               else{
	                 c.setSickness(true);
	               }
	               
	               ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
	               ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
	
	               PlayerZone from = AllZone.getZone(c);
	               from.remove(c);
	              
	               c.setController(card.getController());
	
	               PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
	               to.add(c);
	
	               ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	               ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	             }//if
	    		 
	
	           }//resolve()
	
	    	   public boolean canPlayAI()
	           {
	    		 if (ability3.canPlay() && ability3.canPlayAI())
	    		 {
	    			 return false;
	    		 }
	             CardList c = CardFactoryUtil.AI_getHumanCreature(card, true);
	             CardListUtil.sortAttack(c);
	             CardListUtil.sortFlying(c);
	
	             if(c.isEmpty())
	               return false;
	
	             if(2 <= c.get(0).getNetAttack() && c.get(0).getKeyword().contains("Flying"))
	             {
	               setTargetCard(c.get(0));
	               return true;
	             }
	
	             CardListUtil.sortAttack(c);
	             if(4 <= c.get(0).getNetAttack())
	             {
	               setTargetCard(c.get(0));
	               return true;
	             }
	
	             return false;
	           }//canPlayAI()
	
	         public boolean canPlay()
	         {
	        	 
	           SpellAbility sa;
	    	   for (int i=0; i<AllZone.Stack.size(); i++)
	    	   {
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card2))
	    	    	          return false;
	    	   }
	           return 2 < card2.getCounters(Counters.LOYALTY) &&
	                       AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                       turn[0] != AllZone.Phase.getTurn() &&
	                       AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                       !AllZone.Phase.getPhase().equals("End of Turn") &&
	                       (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                       && AllZone.Stack.size() == 0;
	           
	         }//canPlay()
	       };//SpellAbility ability2
	
	       ability2.setBeforePayMana(new Input()
	       {
			  private static final long serialVersionUID = -1877437173665495402L;
			  
			  int check = -1;
	          public void showMessage()
	          {
	            if(check != AllZone.Phase.getTurn())
	            {
	              check = AllZone.Phase.getTurn();
	              turn[0] = AllZone.Phase.getTurn();
	              
	              
	              AllZone.Stack.push(ability2);
	            }
	            stop();
	          }//showMessage()
	       });
	       
	
	       //ability 1: destroy target noncreature permanent
	       final SpellAbility ability1 = new Ability(card2, "0")
	       {
	         public void resolve()
	         {
	           card2.addCounter(Counters.LOYALTY, 3);
	           turn[0] = AllZone.Phase.getTurn();
	           
	           Card c = getTargetCard();
	           AllZone.GameAction.destroy(c);
	         }
	         public boolean canPlayAI()
	         {
	           if(ability3.canPlay() && ability3.canPlayAI() || getNonCreaturePermanent() == null) {
	             return false;
	           } else
	           {
	             return true;
	           }
	         }
	         public boolean canPlay()
	         {
	           SpellAbility sa;
	    	   for (int i=0; i<AllZone.Stack.size(); i++)
	    	   {
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card2))
	    	    	          return false;
	    	   }
	           return 0 < card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	               turn[0] != AllZone.Phase.getTurn() &&
	               AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	               !AllZone.Phase.getPhase().equals("End of Turn") &&
	               (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	               && AllZone.Stack.size() == 0;
	         }//canPlay()
	         
	         public void chooseTargetAI()
	         {
	           Card c = getNonCreaturePermanent();
	           
	           if (getNonCreaturePermanent() != null)
	        	   setTargetCard(c);
	         }//chooseTargetAI()
	
	         Card getNonCreaturePermanent()
	         {
	           PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	           int highestCost = 0;
	           Card bestCard = null;
	           CardList nonCreaturePermanents = new CardList(play.getCards());
	           nonCreaturePermanents = nonCreaturePermanents.filter(new CardListFilter()
	           {
	
				public boolean addCard(Card c) {
					return CardFactoryUtil.canTarget(card2, c) && !c.isCreature();
				}
	        	   
	           });
	
	           for(int i = 0; i < nonCreaturePermanents.size(); i++)
	           {
	             if(CardUtil.getConvertedManaCost(nonCreaturePermanents.get(i).getManaCost()) > highestCost)
	             {
	            	 highestCost = CardUtil.getConvertedManaCost(nonCreaturePermanents.get(i).getManaCost());
	            	 bestCard = nonCreaturePermanents.get(i);
	             }
	           }
	           if (bestCard == null && nonCreaturePermanents.size() > 0)
	           {
	        	   bestCard = nonCreaturePermanents.get(0);
	        	   return bestCard;
	           }
	           
	           return null;
	         }
	       };//SpellAbility ability1
	
	       ability1.setBeforePayMana(new Input()
	       {
			  private static final long serialVersionUID = 9167121234861249451L;
			  
			  int check = -1;
	          public void showMessage()
	          {
	            if(check != AllZone.Phase.getTurn())
	            {
	              check = AllZone.Phase.getTurn();
	              turn[0] = AllZone.Phase.getTurn();
	              AllZone.Stack.push(ability1);
	            }
	            stop();
	          }//showMessage()
	       });
	
	       ability1.setDescription("+3: Destroy target noncreature permanent.");
	       ability1.setStackDescription("Nicol Bolas - Destroy target noncreature permanent.");
	       ability1.setBeforePayMana(CardFactoryUtil.input_targetNonCreaturePermanent(ability1, Command.Blank));
	       
	       card2.addSpellAbility(ability1);
	       
	       ability2.setDescription("-2: Gain control of target creature.");
	       ability2.setStackDescription("Nicol Bolas - Gain control of target creature.");
	       ability2.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability2));
	       
	       card2.addSpellAbility(ability2);
	      
	       ability3.setDescription("-9: Nicol Bolas deals 7 damage to target player. That player discards 7 cards, then sacrifices 7 permanents.");
	       ability3.setStackDescription("Nicol Bolas - deals 7 damage to target player. That player discards 7 cards, then sacrifices 7 permanents.");
	       card2.addSpellAbility(ability3);
	
	       return card2;
	     }
	     //*************** END ************ END **************************
	
	//*************** START *********** START **************************
	    else if(cardName.equals("Ajani Goldmane"))
	    {
	      //computer only plays ability 1 and 3, gain life and put X\X token into play
	      final int turn[] = new int[1];
	      turn[0] = -1;
	
	      final Card card2 = new Card()
	      {
	        public void addDamage(int n)
	        {
	          subtractCounter(Counters.LOYALTY,n);
	          AllZone.GameAction.checkStateEffects();
	        }
	      };
	      card2.addCounter(Counters.LOYALTY, 4);
	
	      card2.setOwner(owner);
	      card2.setController(owner);
	
	      card2.setName(card.getName());
	      card2.setType(card.getType());
	      card2.setManaCost(card.getManaCost());
	      card2.addSpellAbility(new Spell_Permanent(card2));
	
	      //ability2: all controller's creatures get +1\+1 and vigilance until EOT
	      final SpellAbility ability2 = new Ability(card2, "0")
	      {
	              final Command untilEOT = new Command()
	          {
				private static final long serialVersionUID = -5436621445704076988L;
	
				public void execute()
	            {
	                String player = card2.getController();
	                CardList creatures;
	                if(player.equals(Constant.Player.Human)) {
	                        creatures = new CardList(AllZone.Human_Play.getCards());
	                } else {
	                        creatures = new CardList(AllZone.Computer_Play.getCards());
	                }
	                
	                creatures = creatures.getType("Creature");
	                
	                for (int i = 0; i < creatures.size(); i++) {
	                        Card card = creatures.get(i);
	                        //card.setAttack(card.getAttack() - 1);
	                        //card.setDefense(card.getDefense() - 1);
	                        card.removeExtrinsicKeyword("Vigilance");
	                }
	            }
	          };
	
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY,1);
	          turn[0] = AllZone.Phase.getTurn();
	
	          String player = card2.getController();
	          CardList creatures;
	          if(player.equals(Constant.Player.Human)) {
	                creatures = new CardList(AllZone.Human_Play.getCards());
	          } else {
	                creatures = new CardList(AllZone.Computer_Play.getCards());
	          }
	          
	          creatures = creatures.getType("Creature");
	          
	          for (int i = 0; i < creatures.size(); i++) {
	                  Card card = creatures.get(i);
	                  card.addCounter(Counters.P1P1,1);
	                  card.addExtrinsicKeyword("Vigilance");
	          }
	
	          AllZone.EndOfTurn.addUntil(untilEOT);
	        }
	
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	
	        public boolean canPlay()
	        {
	          @SuppressWarnings("unused") // library
			  PlayerZone library = AllZone.getZone(Constant.Zone.Library, card2.getController());
	
	          return 0 < card2.getCounters(Counters.LOYALTY) &&
	                                  AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                      turn[0] != AllZone.Phase.getTurn() &&
	                      AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                      !AllZone.Phase.getPhase().equals("End of Turn") &&
	                      (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                      && AllZone.Stack.size() == 0;
	          
	        }//canPlay()
	      };//SpellAbility ability2
	
	      ability2.setBeforePayMana(new Input()
	      {
			 private static final long serialVersionUID = 6373573398967821630L;
	        int check = -1;
	         public void showMessage()
	         {
	           if(check != AllZone.Phase.getTurn())
	           {
	             check = AllZone.Phase.getTurn();
	             turn[0] = AllZone.Phase.getTurn();
	             AllZone.Stack.push(ability2);
	           }
	           stop();
	         }//showMessage()
	      });
	
	      //ability3
	      final SpellAbility ability3 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY, 6);
	          turn[0] = AllZone.Phase.getTurn();
	
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	
	          //Create token
	          Card c = new Card();
	
	          c.setOwner(card.getController());
	          c.setController(card.getController());
	
	          c.setImageName("W N N Avatar");
	          c.setName("Avatar");
	          c.setManaCost("W");
	          c.setToken(true);
	
	          c.addType("Creature");
	          c.addType("Avatar");
	          c.setBaseAttack(AllZone.GameAction.getPlayerLife(card.getController()).getLife());
	          c.setBaseDefense(AllZone.GameAction.getPlayerLife(card.getController()).getLife());
	
	          c.addIntrinsicKeyword("This creature's power and toughness are each equal to your life total.");
	
	          play.add(c);
	        }
	        public boolean canPlay()
	        {
	          return 6 <= card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	              turn[0] != AllZone.Phase.getTurn() &&
	              AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	              !AllZone.Phase.getPhase().equals("End of Turn") &&
	              (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	              && AllZone.Stack.size() == 0;
	        }//canPlay()
	        public boolean canPlayAI()
	        {
	                // may be it's better to put only if you have less than 5 life
	                return true;
	        }
	      };
	      ability3.setBeforePayMana(new Input()
	      {
			 private static final long serialVersionUID = 7530960428366291386L;
			 
			 int check = -1;
	         public void showMessage()
	         {
	           if(check != AllZone.Phase.getTurn())
	           {
	             check = AllZone.Phase.getTurn();
	             turn[0] = AllZone.Phase.getTurn();
	             AllZone.Stack.push(ability3);
	           }
	           stop();
	         }//showMessage()
	      });
	
	      //ability 1: gain 2 life
	      final SpellAbility ability1 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.addCounter(Counters.LOYALTY, 1);
	          turn[0] = AllZone.Phase.getTurn();
	          
	          
	          AllZone.GameAction.getPlayerLife(card.getController()).addLife(2);
	          System.out.println("current phase: " +AllZone.Phase.getPhase());
	        }
	        public boolean canPlayAI()
	        {
	          if(ability3.canPlay() && ability3.canPlayAI()) {
	            return false;
	          } else
	          {
	            return true;
	          }
	        }
	        public boolean canPlay()
	        {
	          return 0 < card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	              turn[0] != AllZone.Phase.getTurn() &&
	              AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	              !AllZone.Phase.getPhase().equals("End of Turn") &&
	              (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	              && AllZone.Stack.size() == 0;
	        }//canPlay()
	      };//SpellAbility ability1
	
	      ability1.setBeforePayMana(new Input()
	      {
			 private static final long serialVersionUID = -7969603493514210825L;
			 
			 int check = -1;
	         public void showMessage()
	         {
	           if(check != AllZone.Phase.getTurn())
	           {
	             check = AllZone.Phase.getTurn();
	             turn[0] = AllZone.Phase.getTurn();
	             AllZone.Stack.push(ability1);
	           }
	           stop();
	         }//showMessage()
	      });
	
	      ability1.setDescription("+1: You gain 2 life.");
	      ability1.setStackDescription("Ajani Goldmane - " + card2.getController() +  " gains 2 life");
	      card2.addSpellAbility(ability1);
	
	      ability2.setDescription("-1: Put a +1/+1 counter on each creature you control. Those creatures gain vigilance until end of turn.");
	      ability2.setStackDescription("Ajani Goldmane - Put a +1/+1 counter on each creature you control. They get vigilance.");
	      card2.addSpellAbility(ability2);
	
	      ability3.setDescription("-6: Put a white Avatar creature token into play with \"This creature's power and toughness are each equal to your life total.\"");
	      ability3.setStackDescription("Ajani Goldmane - Put X\\X white Avatar creature token into play.");
	      card2.addSpellAbility(ability3);
	
	      return card2;
	    }
	    //*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Liliana Vess"))
	    {
	      //computer only plays ability 1 and 3, discard and return creature from graveyard to play
	      final int turn[] = new int[1];
	      turn[0] = -1;

	      final Card card2 = new Card()
	      {
	        public void addDamage(int n)
	        {
	          subtractCounter(Counters.LOYALTY,n);
	          AllZone.GameAction.checkStateEffects();
	        }
	      };
	      card2.addCounter(Counters.LOYALTY, 5);

	      card2.setOwner(owner);
	      card2.setController(owner);

	      card2.setName(card.getName());
	      card2.setType(card.getType());
	      card2.setManaCost(card.getManaCost());
	      card2.addSpellAbility(new Spell_Permanent(card2));

	      //ability2
	      final SpellAbility ability2 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY, 2);
	          turn[0] = AllZone.Phase.getTurn();

	          String player = card2.getController();
	          if(player.equals(Constant.Player.Human))
	            humanResolve();
	          else
	            computerResolve();
	        }
	        public void computerResolve()
	        {
	          CardList creature = new CardList(AllZone.Computer_Library.getCards());
	          creature = creature.getType("Creature");
	          if(creature.size() != 0)
	          {
	            Card c = creature.get(0);
	            AllZone.GameAction.shuffle(card2.getController());

	            //move to top of library
	            AllZone.Computer_Library.remove(c);
	            AllZone.Computer_Library.add(c, 0);
	          }
	        }//computerResolve()
	        public void humanResolve()
	        {
	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card2.getController());

	          CardList list = new CardList(library.getCards());

	          if(list.size() != 0)
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select any card", list.toArray());

	            AllZone.GameAction.shuffle(card2.getController());
	            if(o != null)
	            {
	              //put creature on top of library
	              library.remove(o);
	              library.add((Card)o, 0);
	            }
	          }//if
	        }//resolve()
	        public boolean canPlayAI()
	        {
	          return false;
	        }

	        public boolean canPlay()
	        {
	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card2.getController());

	          return 2 <= card2.getCounters(Counters.LOYALTY)                  &&
	                      AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                      1 < library.size()                            &&
	                      turn[0] != AllZone.Phase.getTurn() &&
	                      AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                      !AllZone.Phase.getPhase().equals("End of Turn") &&
	                      (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                      && AllZone.Stack.size() == 0;
	        }//canPlay()
	      };//SpellAbility ability2

	      ability2.setBeforePayMana(new Input()
	      {
			 private static final long serialVersionUID = 5726590384281714755L;
			
			 int check = -1;
	         public void showMessage()
	         {
	           if(check != AllZone.Phase.getTurn())
	           {
	             check = AllZone.Phase.getTurn();
	             turn[0] = AllZone.Phase.getTurn();
	             AllZone.Stack.push(ability2);
	           }
	           stop();
	         }//showMessage()
	      });

	      //ability3
	      final SpellAbility ability3 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY, 8);
	          turn[0] = AllZone.Phase.getTurn();

	          //get all graveyard creatures
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Graveyard.getCards());
	          list.addAll(AllZone.Computer_Graveyard.getCards());
	          list = list.getType("Creature");

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	          PlayerZone grave = null;
	          Card c = null;
	          for(int i = 0; i < list.size(); i++)
	          {
	            //this is a rough hack, but no one will ever see this code anyways, lol ;+)
	            c = list.get(i);
	            c.setController(card.getController());

	            grave = AllZone.getZone(c);
	            if(grave != null)
	              grave.remove(c);

	            play.add(c);
	          }
	        }
	        public boolean canPlay()
	        {
	          return 8 <= card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	              turn[0] != AllZone.Phase.getTurn() &&
	              AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	              !AllZone.Phase.getPhase().equals("End of Turn") &&
	              (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	              && AllZone.Stack.size() == 0;
	        }//canPlay()
	        public boolean canPlayAI()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Graveyard.getCards());
	          list.addAll(AllZone.Computer_Graveyard.getCards());
	          list = list.getType("Creature");

	          return 3 < list.size();
	        }
	      };
	      ability3.setBeforePayMana(new Input()
	      {
			 private static final long serialVersionUID = -3297439284172874241L;
			
			 int check = -1;
	         public void showMessage()
	         {
	           if(check != AllZone.Phase.getTurn())
	           {
	             check = AllZone.Phase.getTurn();
	             turn[0] = AllZone.Phase.getTurn();
	             AllZone.Stack.push(ability3);
	           }
	           stop();
	         }//showMessage()
	      });

	      //ability 1
	      final SpellAbility ability1 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.addCounter(Counters.LOYALTY,1);
	          turn[0] = AllZone.Phase.getTurn();

	          String s = getTargetPlayer();
	          setStackDescription("Liliana Vess - " +s +" discards a card");

	          if(s.equals(Constant.Player.Human))
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	          else
	            AllZone.GameAction.discardRandom(Constant.Player.Computer);
	        }
	        public boolean canPlayAI()
	        {
	          if(ability3.canPlay() && ability3.canPlayAI())
	            return false;
	          else
	          {
	            setTargetPlayer(Constant.Player.Human);
	            return true;
	          }
	        }
	        public boolean canPlay()
	        {
	          return 0 < card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
	              turn[0] != AllZone.Phase.getTurn() &&
	              AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	              !AllZone.Phase.getPhase().equals("End of Turn") &&
	              (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	              && AllZone.Stack.size() == 0;
	        }//canPlay()
	      };//SpellAbility ability1

	      Input target = new Input()
	      {
			private static final long serialVersionUID = 4997055112713151705L;
			
			public void showMessage()
	        {
	          AllZone.Display.showMessage("Select target player");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectPlayer(String player)
	        {
	          turn[0] = AllZone.Phase.getTurn();
	          ability1.setTargetPlayer(player);
	          AllZone.Stack.add(ability1);
	          stop();
	        }
	      };//Input target
	      ability1.setBeforePayMana(target);
	      ability1.setDescription("+1: Target player discards a card.");
	      card2.addSpellAbility(ability1);

	      ability2.setDescription("-2: Search your library for a card, then shuffle your library and put that card on top of it.");
	      ability2.setStackDescription("Liliana Vess - Search your library for a card, then shuffle your library and put that card on top of it.");
	      card2.addSpellAbility(ability2);

	      ability3.setDescription("-8: Put all creature cards in all graveyards into play under your control.");
	      ability3.setStackDescription("Liliana Vess - Put all creature cards in all graveyards into play under your control.");
	      card2.addSpellAbility(ability3);

	      return card2;
	    }
	    //*************** END ************ END **************************





	    //*************** START *********** START **************************
	    else if(cardName.equals("Chandra Nalaar"))
	    {
	      //computer only plays ability 1 and 3, discard and return creature from graveyard to play
	      final int turn[] = new int[1];
	      turn[0] = -1;

	      final Card card2 = new Card()
	      {
	        public void addDamage(int n)
	        {
	          subtractCounter(Counters.LOYALTY, n);
	          AllZone.GameAction.checkStateEffects();
	        }
	      };
	      card2.addCounter(Counters.LOYALTY, 6);

	      card2.setOwner(owner);
	      card2.setController(owner);

	      card2.setName(card.getName());
	      card2.setType(card.getType());
	      card2.setManaCost(card.getManaCost());
	      card2.addSpellAbility(new Spell_Permanent(card2));

	      //ability 1
	      final SpellAbility ability1 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.addCounter(Counters.LOYALTY, 1);
	          turn[0] = AllZone.Phase.getTurn();
	          
	          if(getTargetCard() != null)
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card2,getTargetCard()))
	            {
	              Card c = getTargetCard();
	              if (CardFactoryUtil.canDamage(card2, c))
	            	  c.addDamage(1);
	            }
	          }
	          
	          else
	          {
		          PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
		          life.subtractLife(1);
	          }
	        }
	        public boolean canPlay()
	        {
	          SpellAbility sa;
		      for (int i=0; i<AllZone.Stack.size(); i++)
		      {
		    	     sa = AllZone.Stack.peek(i);
		    	     if (sa.getSourceCard().equals(card2))
		    	          return false;
		      }
				
	          return AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                 turn[0] != AllZone.Phase.getTurn() &&
	                 AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                 !AllZone.Phase.getPhase().equals("End of Turn") && 
	                 (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                 && AllZone.Stack.size() == 0;
	        }
	        public boolean canPlayAI()
	        {
	          setTargetPlayer(Constant.Player.Human);
	          setStackDescription("Chandra Nalaar - deals 1 damage to " +Constant.Player.Human);
	          return card2.getCounters(Counters.LOYALTY) < 8;
	        }
	      };//SpellAbility ability1

	      Input target1 = new Input()
	      {
			private static final long serialVersionUID = 5263705146686766284L;
			
			public void showMessage()
	        {
	          AllZone.Display.showMessage("Select target Player or Planeswalker");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectCard(Card card, PlayerZone zone)
	        {
	          if(card.isPlaneswalker() && zone.is(Constant.Zone.Play))
	          {
	            ability1.setTargetCard(card);
	            stopSetNext(new Input_PayManaCost(ability1));
	          }
	        }//selectCard()
	        public void selectPlayer(String player)
	        {
	          ability1.setTargetPlayer(player);
	          stopSetNext(new Input_PayManaCost(ability1));
	        }
	      };
	      ability1.setBeforePayMana(target1);
	      ability1.setDescription("+1: Chandra Nalaar deals 1 damage to target player.");
	      card2.addSpellAbility(ability1);
	      //end ability1

	      //ability 2
	      final int damage2[] = new int[1];

	      final SpellAbility ability2 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          turn[0] = AllZone.Phase.getTurn();

	          card2.subtractCounter(Counters.LOYALTY, damage2[0]);
	          if (CardFactoryUtil.canDamage(card2, getTargetCard()))
	        	  getTargetCard().addDamage(damage2[0]);

	          damage2[0] = 0;
	        }//resolve()
	        public boolean canPlay()
	        {
	          SpellAbility sa;
	  	      for (int i=0; i<AllZone.Stack.size(); i++)
	  	      {
	  	    	     sa = AllZone.Stack.peek(i);
	  	    	     if (sa.getSourceCard().equals(card2))
	  	    	          return false;
	  	      }

	          return AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                 turn[0] != AllZone.Phase.getTurn() &&
	                 AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                 !AllZone.Phase.getPhase().equals("End of Turn") && 
	                 (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                 && AllZone.Stack.size() == 0;
	        }
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	      };//SpellAbility ability2

	      Input target2 = new Input()
	      {
			private static final long serialVersionUID = -2160464080456452897L;
			
			public void showMessage()
	        {
	          AllZone.Display.showMessage("Select target creature");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectCard(Card c, PlayerZone zone)
	        {
	          if(!CardFactoryUtil.canTarget(card, c)){
	          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
	          }
	          else if(c.isCreature())
	          {
	            turn[0] = AllZone.Phase.getTurn();


	            damage2[0] = getDamage();

	            ability2.setTargetCard(c);
	            ability2.setStackDescription("Chandra Nalaar - deals damage to " +c);

	            AllZone.Stack.add(ability2);
	            stop();
	          }
	        }//selectCard()
	        int getDamage()
	        {
	          int size = card2.getCounters(Counters.LOYALTY);
	          Object choice[] = new Object[size];

	          for(int i = 0; i < choice.length; i++)
	            choice[i] = new Integer(i + 1);

	           Integer damage = (Integer) AllZone.Display.getChoice("Select X", choice);
	           return damage.intValue();
	        }
	      };//Input target
	      ability2.setBeforePayMana(target2);
	      ability2.setDescription("-X: Chandra Nalaar deals X damage to target creature.");
	      card2.addSpellAbility(ability2);
	      //end ability2



	      //ability 3
	      final SpellAbility ability3 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY, 8);
	          turn[0] = AllZone.Phase.getTurn();

	          PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
	          life.subtractLife(10);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, getTargetPlayer());
	          CardList list = new CardList(play.getCards());
	          list = list.getType("Creature");

	          for(int i = 0; i < list.size(); i++)
	          { 
	        	  if (CardFactoryUtil.canDamage(card, list.get(i)))
	        		  list.get(i).addDamage(10);
	          }
	        }//resolve()
	        public boolean canPlay()
	        {
	        	
	          SpellAbility sa;
	  	      for (int i=0; i<AllZone.Stack.size(); i++)
	  	      {
	  	    	     sa = AllZone.Stack.peek(i);
	  	    	     if (sa.getSourceCard().equals(card2))
	  	    	          return false;
	  	      }
	  			
	          return AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                 turn[0] != AllZone.Phase.getTurn()            &&
	                 7 < card2.getCounters(Counters.LOYALTY) &&
	          		 AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	          		!AllZone.Phase.getPhase().equals("End of Turn") &&
	          		(AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	          		&& AllZone.Stack.size() == 0;
	        }
	        public boolean canPlayAI()
	        {
	          setTargetPlayer(Constant.Player.Human);
	          setStackDescription("Chandra Nalaar - deals 10 damage to " +Constant.Player.Human +" and each creature he or she controls.");
	          return true;
	        }
	      };//SpellAbility ability3

	      Input target3 = new Input()
	      {
			private static final long serialVersionUID = -3014450919506364666L;
			
			public void showMessage()
	        {
	          AllZone.Display.showMessage("Select target player");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectPlayer(String player)
	        {
	          turn[0] = AllZone.Phase.getTurn();

	          ability3.setTargetPlayer(player);
	          ability3.setStackDescription("Chandra Nalaar - deals 10 damage to " +player +" and each creature he or she controls.");

	          AllZone.Stack.add(ability3);
	          stop();
	        }
	      };//Input target
	      ability3.setBeforePayMana(target3);
	      ability3.setDescription("-8: Chandra Nalaar deals 10 damage to target player and each creature he or she controls.");
	      card2.addSpellAbility(ability3);
	      //end ability3

	      return card2;
	    }
	    //*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Garruk Wildspeaker"))
	    {
	      final int turn[] = new int[1];
	      turn[0] = -1;

	      final Card card2 = new Card()
	      {
	        public void addDamage(int n)
	        {
	          subtractCounter(Counters.LOYALTY,n);
	          AllZone.GameAction.checkStateEffects();
	        }
	      };
	      card2.addCounter(Counters.LOYALTY,3);

	      card2.setOwner(owner);
	      card2.setController(owner);

	      card2.setName(card.getName());
	      card2.setType(card.getType());
	      card2.setManaCost(card.getManaCost());
	      card2.addSpellAbility(new Spell_Permanent(card2));

	      //ability1
	      final SpellAbility ability1 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.addCounter(Counters.LOYALTY, 1);

	          turn[0] = AllZone.Phase.getTurn();

	          //only computer uses the stack
	          CardList tapped = new CardList(AllZone.Computer_Play.getCards());
	          tapped = tapped.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isLand() && c.isTapped();
	            }
	          });

	          for(int i = 0; i < 2 && i < tapped.size(); i++)
	            tapped.get(i).untap();
	        }//resolve()
	        public boolean canPlayAI()
	        {
	          return card2.getCounters(Counters.LOYALTY) < 4 && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
	        }
	        public boolean canPlay()
	        {
	          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                  turn[0] != AllZone.Phase.getTurn() &&
	                  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                  !AllZone.Phase.getPhase().equals("End of Turn") &&
	                  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                  && AllZone.Stack.size() == 0;
	        }//canPlay()
	      };
	      final Input targetLand = new Input()
	      {
			private static final long serialVersionUID = -6609158314106861676L;
			
			private int count;
	        public void showMessage()
	        {
	          AllZone.Display.showMessage("Select a land to untap");
	          ButtonUtil.disableAll();
	        }
	        public void selectCard(Card c, PlayerZone zone)
	        {
	          if(c.isLand() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card2, c))
	          {
	            count++;
	            c.untap();
	          }

	          //doesn't use the stack, its just easier this way
	          if(count == 2)
	          {
	            count = 0;
	            turn[0] = AllZone.Phase.getTurn();
	            card2.addCounter(Counters.LOYALTY,1);
	            stop();
	          }
	        }//selectCard()
	      };//Input

	      Input runtime1 = new Input()
	      {
			private static final long serialVersionUID = 8709088526618867662L;

			public void showMessage()
	        {
	          stopSetNext(targetLand);
	        }
	      };//Input
	      ability1.setDescription("+1: Untap two target lands.");
	      ability1.setStackDescription("Garruk Wildspeaker - Untap two target lands.");

	      ability1.setBeforePayMana(runtime1);
	      card2.addSpellAbility(ability1);
	      //end ability 1


	      //start ability 2
	      final SpellAbility ability2 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY,1);
	          turn[0] = AllZone.Phase.getTurn();

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	          play.add(getToken());
	        }
	        Card getToken()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());
	          
	          c.setImageName("G 3 3 Beast");
	          c.setName("Beast");
	          c.setManaCost("G");
	          c.setToken(true);
	          //c.addKeyword("Token");

	          c.addType("Creature");
	          c.addType("Beast");
	          c.setBaseAttack(3);
	          c.setBaseDefense(3);

	          return c;
	        }//makeToken()

	        public boolean canPlay()
	        {
	          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                  turn[0] != AllZone.Phase.getTurn() &&
	                  0 < card2.getCounters(Counters.LOYALTY) &&
	                  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                  !AllZone.Phase.getPhase().equals("End of Turn") && 
	                  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                  && AllZone.Stack.size() == 0;
	        }//canPlay()
	        public boolean canPlayAI()
	        {
	          CardList c = new CardList(AllZone.Computer_Play.getCards());
	          c = c.getType("Creature");
	          return c.size() < 4;
	        }
	      };//SpellAbility ability 2
	      Input runtime2 = new Input()
	      {
			private static final long serialVersionUID = -1718455991391244845L;
			
			int check = -1;
	        public void showMessage()
	        {
	          if(check != AllZone.Phase.getTurn())
	          {
	            check = AllZone.Phase.getTurn();
	            turn[0] = AllZone.Phase.getTurn();

	            AllZone.Stack.push(ability2);
	            stop();
	          }
	        }
	      };//Input
	      ability2.setStackDescription(card2.getName() +" -  Put a 3/3 green Beast creature token into play.");
	      ability2.setDescription("-1: Put a 3/3 green Beast creature token into play.");
	      ability2.setBeforePayMana(runtime2);
	      card2.addSpellAbility(ability2);
	      //end ability 2


	      //start ability 3
	      final SpellAbility ability3 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY, 4);
	          turn[0] = AllZone.Phase.getTurn();

	          final int boost = 3;
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	          CardList list = new CardList(play.getCards());
	          @SuppressWarnings("unused") // c
	          Card c;

	          for(int i = 0; i < list.size(); i++)
	          {
	            final Card[] target = new Card[1];
	            target[0] = list.get(i);

	            final Command untilEOT = new Command()
	            {
				  private static final long serialVersionUID = 478068133055335098L;

				  public void execute()
	              {
	                if(AllZone.GameAction.isCardInPlay(target[0]))
	                {
	                  target[0].addTempAttackBoost(-boost);
	                  target[0].addTempDefenseBoost(-boost);

	                  target[0].removeExtrinsicKeyword("Trample");
	                }
	              }
	            };//Command

	            if(AllZone.GameAction.isCardInPlay(target[0]))
	            {
	              target[0].addTempAttackBoost(boost);
	              target[0].addTempDefenseBoost(boost);

	              target[0].addExtrinsicKeyword("Trample");

	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }//if
	          }//for

	        }//resolve()
	        public boolean canPlay()
	        {
	          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                  turn[0] != AllZone.Phase.getTurn() &&
	                  3 < card2.getCounters(Counters.LOYALTY) &&
	          		  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	          		  !AllZone.Phase.getPhase().equals("End of Turn") &&
	          		  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	          		  && AllZone.Stack.size() == 0;
	        }//canPlay()
	        public boolean canPlayAI()
	        {
	          CardList c = new CardList(AllZone.Computer_Play.getCards());
	          c = c.getType("Creature");
	          return c.size() >= 4 && AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && 
	          AllZone.Phase.getActivePlayer().equals(card2.getController());
	        }
	      };//SpellAbility ability3
	      Input runtime3 = new Input()
	      {
			private static final long serialVersionUID = 7697504647440222302L;
			
			int check = -1;
	        public void showMessage()
	        {
	          if(check != AllZone.Phase.getTurn())
	          {
	            check = AllZone.Phase.getTurn();
	            turn[0] = AllZone.Phase.getTurn();

	            AllZone.Stack.push(ability3);
	            stop();
	          }
	        }
	      };//Input
	      ability3.setStackDescription(card2.getName() +" -  Creatures you control get +3/+3 and trample until end of turn.");
	      ability3.setDescription("-4: Creatures you control get +3/+3 and trample until end of turn.");
	      ability3.setBeforePayMana(runtime3);
	      card2.addSpellAbility(ability3);
	      //end ability 3

	      return card2;
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Jace Beleren"))
	    {
	    
	    	final int turn[] = new int[1];
		      turn[0] = -1;

		      final Card card2 = new Card()
		      {
		        public void addDamage(int n)
		        {
		          subtractCounter(Counters.LOYALTY,n);
		          AllZone.GameAction.checkStateEffects();
		        }
		      };
		      card2.addCounter(Counters.LOYALTY,3);

		      card2.setOwner(owner);
		      card2.setController(owner);

		      card2.setName(card.getName());
		      card2.setType(card.getType());
		      card2.setManaCost(card.getManaCost());
		      card2.addSpellAbility(new Spell_Permanent(card2));
		      
		      
		      //ability1
		      final SpellAbility ability1 = new Ability(card2, "0")
		      {
		        public void resolve()
		        {
		          card2.addCounter(Counters.LOYALTY, 2);

		          turn[0] = AllZone.Phase.getTurn();
		          
		          AllZone.GameAction.drawCard(Constant.Player.Computer);
		          AllZone.GameAction.drawCard(Constant.Player.Human);
		          
		        }//resolve()
		        public boolean canPlayAI()
		        {
		          return card2.getCounters(Counters.LOYALTY) < 11 && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
		        }
		        public boolean canPlay()
		        {
		          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
		                  turn[0] != AllZone.Phase.getTurn() &&
		                  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
		                  !AllZone.Phase.getPhase().equals("End of Turn") &&
		                  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
		                  && AllZone.Stack.size() == 0;
		        }//canPlay()
		      };
		      
		      ability1.setDescription("+2: Each player draws a card.");
		      ability1.setStackDescription(cardName +  " - Each player draws a card.");
		      
		      //ability2
		      final SpellAbility ability2 = new Ability(card2, "0")
		      {
		        public void resolve()
		        {
		          card2.subtractCounter(Counters.LOYALTY, 1);

		          turn[0] = AllZone.Phase.getTurn();
		          String player = getTargetPlayer();
		          
		          AllZone.GameAction.drawCard(player);
		          
		        }//resolve()
		        public boolean canPlayAI()
		        {
		          return false;
		        }
		        public boolean canPlay()
		        {
		          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
		          		  card2.getCounters(Counters.LOYALTY) >= 1 &&
		                  turn[0] != AllZone.Phase.getTurn() &&
		                  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
		                  !AllZone.Phase.getPhase().equals("End of Turn") &&
		                  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
		                  && AllZone.Stack.size() == 0;
		        }//canPlay()
		      };
		      ability2.setDescription("-1: Target player draws a card.");
		      ability2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability2));
		      
		      //ability3
		      final SpellAbility ability3 = new Ability(card2, "0")
		      {
		        public void resolve()
		        {
		          card2.subtractCounter(Counters.LOYALTY, 10);

		          turn[0] = AllZone.Phase.getTurn();
		          String player = getTargetPlayer();
		          
		          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
		          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
		          CardList libList = new CardList(lib.getCards());

		          int max = 20;
		          if (libList.size() < 20)
		        	  max = libList.size();
		          
		          for (int i=0;i<max;i++)
		          {
		        	  Card c = libList.get(i);
		        	  lib.remove(c);
		        	  grave.add(c);
		          }
		          
		        }//resolve()
		        public boolean canPlayAI()
		        {
		          setTargetPlayer(Constant.Player.Human);
		          return card2.getCounters(Counters.LOYALTY) >= 11;
		        }
		        public boolean canPlay()
		        {
		          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
		          		  card2.getCounters(Counters.LOYALTY) >= 10 &&
		                  turn[0] != AllZone.Phase.getTurn() &&
		                  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
		                  !AllZone.Phase.getPhase().equals("End of Turn") &&
		                  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
		                  && AllZone.Stack.size() == 0;
		        }//canPlay()
		      };
		      ability3.setDescription("-10: Target player puts the top twenty cards of his or her library into his or her graveyard.");
		      ability3.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability3));
		      
		      card2.addSpellAbility(ability1);
		      card2.addSpellAbility(ability2);
		      card2.addSpellAbility(ability3);
		      
		      return card2;
	  	}//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if (cardName.equals("Ajani Vengeant"))
	    {
	    
	    	final int turn[] = new int[1];
		      turn[0] = -1;

		      final Card card2 = new Card()
		      {
		        public void addDamage(int n)
		        {
		          subtractCounter(Counters.LOYALTY,n);
		          AllZone.GameAction.checkStateEffects();
		        }
		      };
		      card2.addCounter(Counters.LOYALTY, 3);

		      card2.setOwner(owner);
		      card2.setController(owner);

		      card2.setName(card.getName());
		      card2.setType(card.getType());
		      card2.setManaCost(card.getManaCost());
		      card2.addSpellAbility(new Spell_Permanent(card2));
		      
		    //ability 1: destroy target noncreature permanent
		       final SpellAbility ability1 = new Ability(card2, "0")
		       {
		         public void resolve()
		         {
		           card2.addCounter(Counters.LOYALTY, 1);
		           turn[0] = AllZone.Phase.getTurn();
		           
		           Card c = getTargetCard();
		           c.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
		         }
		         public boolean canPlayAI()
		         {
		           return card2.getCounters(Counters.LOYALTY) < 8;
		         }
		         public boolean canPlay()
		         {
		           SpellAbility sa;
		    	   for (int i=0; i<AllZone.Stack.size(); i++)
		    	   {
		    	    	     sa = AllZone.Stack.peek(i);
		    	    	     if (sa.getSourceCard().equals(card2))
		    	    	          return false;
		    	   }
		           return 0 < card2.getCounters(Counters.LOYALTY) && AllZone.getZone(card2).is(Constant.Zone.Play) &&
		               turn[0] != AllZone.Phase.getTurn() &&
		               AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
		               !AllZone.Phase.getPhase().equals("End of Turn") &&
		               (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
		               && AllZone.Stack.size() == 0;
		         }//canPlay()
		         
		         public void chooseTargetAI()
		         {
		           Card c = getPermanent();
		           
		           if (getPermanent() != null)
		        	   setTargetCard(c);
		         }//chooseTargetAI()
		
		         Card getPermanent()
		         {
		           PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		           int highestCost = 0;
		           Card bestCard = null;
		           CardList perms = new CardList(play.getCards());
		           perms = perms.filter(new CardListFilter()
		           {
						public boolean addCard(Card c) {
							return CardFactoryUtil.canTarget(card2, c) /* && c.isTapped()*/;
						}   
		           });
		
		           for(int i = 0; i < perms.size(); i++)
		           {
		             if(CardUtil.getConvertedManaCost(perms.get(i).getManaCost()) > highestCost && perms.get(i).isTapped())
		             {
		            	 highestCost = CardUtil.getConvertedManaCost(perms.get(i).getManaCost());
		            	 bestCard = perms.get(i);
		             }
		           }
		           if (bestCard == null && perms.size() > 0)
		           {
		        	   bestCard = perms.get(0);
		        	   return bestCard;
		           }
		           
		           return null;
		         }
		       };//SpellAbility ability1
		
		       /*
		       ability1.setBeforePayMana(new Input()
		       {
				  private static final long serialVersionUID = 9167121234861249451L;
				  
				  int check = -1;
		          public void showMessage()
		          {
		            if(check != AllZone.Phase.getTurn())
		            {
		              check = AllZone.Phase.getTurn();
		              turn[0] = AllZone.Phase.getTurn();
		              AllZone.Stack.push(ability1);
		            }
		            stop();
		          }//showMessage()
		       });
		        */
		       ability1.setDescription("+1: Target permanent doesn't untap during its controller's next untap step..");
		       ability1.setBeforePayMana(CardFactoryUtil.input_targetPermanent(ability1));
		       
		       
		       final Ability ability2 = new Ability(card, "0")
		       {
		 		 int damage = 3;
		 		public boolean canPlayAI()
		        {
		 		  setTargetPlayer(Constant.Player.Human);
		          return AllZone.Human_Life.getLife() <= damage;
		          
		        }
		         public void resolve()
		         {
		           card2.subtractCounter(Counters.LOYALTY, 2);
		           if(getTargetCard() != null)
		           {
		             if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card2, getTargetCard()))
		             {
		               Card c = getTargetCard();
		               c.addDamage(damage);
		             }
		           }
		           else
		             AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage);
		           	 AllZone.GameAction.getPlayerLife(card2.getController()).addLife(3);
		           
		         }
		       };//ability2

		       Input target = new Input()
		       {
				private static final long serialVersionUID = -6688689065812475609L;
				public void showMessage()
		         {
		           AllZone.Display.showMessage("Select target Creature, Player or Planeswalker");
		           ButtonUtil.enableOnlyCancel();
		         }
		         public void selectButtonCancel() {stop();}
		         public void selectCard(Card card, PlayerZone zone)
		         {
		           if(!CardFactoryUtil.canTarget(ability2, card)){
		           	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
		           }
		           else if((card.isCreature() || card.isPlaneswalker()) && zone.is(Constant.Zone.Play))
		           {
		             ability2.setTargetCard(card);
		             stopSetNext(new Input_PayManaCost(ability2));
		           }
		         }//selectCard()
		         public void selectPlayer(String player)
		         {
		           ability2.setTargetPlayer(player);
		           stopSetNext(new Input_PayManaCost(ability2));
		         }
		       };
		       ability2.setBeforePayMana(target);
		       ability2.setDescription("-2: Ajani Vengeant deals 3 damage to target creature or player and you gain 3 life.");
		       
		       
	       	   //ability3
		       final SpellAbility ability3 = new Ability(card2, "0")
		       {
		        public void resolve()
		        {
		          card2.subtractCounter(Counters.LOYALTY, 7);

		          turn[0] = AllZone.Phase.getTurn();
		          String player = getTargetPlayer();
		          
		          PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
		          CardList land = new CardList(play.getCards());
		          land = land.getType("Land");

		          for (Card c : land)
		          {
		        	  AllZone.GameAction.destroy(c);
		          }
		          
		        }//resolve()
		        public boolean canPlayAI()
		        {
		          PlayerZone pz = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		          CardList land = new CardList(pz.getCards());
		          land = land.getType("Land");

		          setTargetPlayer(Constant.Player.Human);
		          return card2.getCounters(Counters.LOYALTY) >= 8 && land.size() >= 4;
		        }
		        public boolean canPlay()
		        {
		          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
		          		  card2.getCounters(Counters.LOYALTY) >= 7 &&
		                  turn[0] != AllZone.Phase.getTurn() &&
		                  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
		                  !AllZone.Phase.getPhase().equals("End of Turn") &&
		                  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
		                  && AllZone.Stack.size() == 0;
		         }//canPlay()
		       };
		       ability3.setDescription("-7: Destroy all lands target player controls.");
		       ability3.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability3));
		       
		       card2.addSpellAbility(ability1);
		       card2.addSpellAbility(ability2);
		       card2.addSpellAbility(ability3);
		      
		       return card2;
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Tezzeret the Seeker"))
	    {
	      final int turn[] = new int[1];
	      turn[0] = -1;

	      final Card card2 = new Card()
	      {
	        public void addDamage(int n)
	        {
	          subtractCounter(Counters.LOYALTY,n);
	          AllZone.GameAction.checkStateEffects();
	        }
	      };
	      card2.addCounter(Counters.LOYALTY,4);

	      card2.setOwner(owner);
	      card2.setController(owner);

	      card2.setName(card.getName());
	      card2.setType(card.getType());
	      card2.setManaCost(card.getManaCost());
	      card2.addSpellAbility(new Spell_Permanent(card2));

	      //ability1
	      final SpellAbility ability1 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.addCounter(Counters.LOYALTY, 1);

	          turn[0] = AllZone.Phase.getTurn();

	          //only computer uses the stack
	          CardList tapped = new CardList(AllZone.Computer_Play.getCards());
	          tapped = tapped.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isArtifact() && c.isTapped() && CardFactoryUtil.canTarget(card2, c);
	            }
	          });

	          for(int i = 0; i < 2 && i < tapped.size(); i++)
	            tapped.get(i).untap();
	        }//resolve()
	        public boolean canPlayAI()
	        {
	          return card2.getCounters(Counters.LOYALTY) < 4 && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
	        }
	        public boolean canPlay()
	        {
	          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                  turn[0] != AllZone.Phase.getTurn() &&
	                  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                  !AllZone.Phase.getPhase().equals("End of Turn") &&
	                  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                  && AllZone.Stack.size() == 0;
	        }//canPlay()
	      };
	      final Input targetArtifact = new Input()
	      {

			private static final long serialVersionUID = -7915255038817192835L;
			private int count;
	        public void showMessage()
	        {
	          AllZone.Display.showMessage("Select a land to untap");
	          ButtonUtil.disableAll();
	        }
	        public void selectCard(Card c, PlayerZone zone)
	        {
	          if(c.isArtifact() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card2, c))
	          {
	            count++;
	            c.untap();
	          }

	          //doesn't use the stack, its just easier this way
	          if(count == 2)
	          {
	            count = 0;
	            turn[0] = AllZone.Phase.getTurn();
	            card2.addCounter(Counters.LOYALTY,1);
	            stop();
	          }
	        }//selectCard()
	      };//Input

	      Input runtime1 = new Input()
	      {
			private static final long serialVersionUID = 871304623687370615L;

			public void showMessage()
	        {
	          stopSetNext(targetArtifact);
	        }
	      };//Input
	      ability1.setDescription("+1: Untap up to two target artifacts.");
	      ability1.setStackDescription("Tezzeret the Seeker - Untap two target artifacts.");

	      ability1.setBeforePayMana(runtime1);
	      card2.addSpellAbility(ability1);
	      //end ability 1
	      
	      
	      
	      //ability 2
	      final SpellAbility ability2 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          turn[0] = AllZone.Phase.getTurn();
	          
	          int size = card2.getCounters(Counters.LOYALTY) + 1;
	          Object choice[] = new Object[size];

	          for(int i = 0; i < choice.length; i++)
	            choice[i] = new Integer(i);

	          Integer damage = (Integer) AllZone.Display.getChoice("Select X", choice);
	          final int dam = damage.intValue();

	          card2.subtractCounter(Counters.LOYALTY, dam);
	          
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card2.getController());
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	          CardList list = new CardList(lib.getCards());
	          list = list.filter(new CardListFilter()
	          {
	        	  public boolean addCard(Card c)
	        	  {
	        		  return c.isArtifact() && CardUtil.getConvertedManaCost(c.getManaCost()) <= dam;
	        	  }
	          });
	          
	          if (list.size() > 0)
	          {
	        	  Object o = AllZone.Display.getChoiceOptional("Select artifact", AllZone.Human_Library.getCards());
	        	  if (o != null)
	        	  {
	        		  Card c = (Card)o;
	        		  if (list.contains(c))
	        		  {
	        			  lib.remove(c);
	        			  play.add(c);
	        		  }
	        	  }
	          }
	        }//resolve()
	        public boolean canPlay()
	        {
	          SpellAbility sa;
	  	      for (int i=0; i<AllZone.Stack.size(); i++)
	  	      {
	  	    	     sa = AllZone.Stack.peek(i);
	  	    	     if (sa.getSourceCard().equals(card2))
	  	    	          return false;
	  	      }

	          return AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                 turn[0] != AllZone.Phase.getTurn() &&
	                 AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                 !AllZone.Phase.getPhase().equals("End of Turn") && 
	                 (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                 && AllZone.Stack.size() == 0;
	        }
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	      };//SpellAbility ability2
	      ability2.setDescription("-X: Search your library for an artifact card with converted mana cost X or less and put it onto the battlefield. Then shuffle your library.");
	      ability2.setStackDescription(card2.getName() + " - Search your library for an artifact card with converted mana cost X or less and put it onto the battlefield. Then shuffle your library.");
	      card2.addSpellAbility(ability2);
	      
	      
	      final SpellAbility ability3 = new Ability(card2, "0")
	      {
			public void resolve() {
				
				card2.subtractCounter(Counters.LOYALTY, 5);

		        turn[0] = AllZone.Phase.getTurn();
		        
		        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
		        CardList list = new CardList(play.getCards());
		        list = list.getType("Artifact");
		        CardList creatures = list.filter(new CardListFilter(){
		        	public boolean addCard(Card c)
		        	{
		        		return c.isCreature();
		        	}
		        });
				
		        //final Card[] tempCards = new Card[creatures.size()];
		        final HashMap<Integer, Card> tempCardMap = new HashMap<Integer, Card>();
		        
		        for (Card creatureCard : creatures)
		        {
		        	Card crd = copyStats(creatureCard);
		        	tempCardMap.put(creatureCard.getUniqueNumber(), crd);
		        	//System.out.println("Just added:" + crd);
		        }
		        
		        for (Card c : list)
		        {
		        	final Card[] art = new Card[1];
		        	art[0] = c;
		        	if (AllZone.GameAction.isCardInPlay(art[0]))
			        {
			        	if (c.isCreature())
			        	{
			        		//Card crd = copyStats(art[0]);
			        		//tempCards[c.getUniqueNumber()] = crd;
			        		
			        		final Command creatureUntilEOT = new Command()
			                {
			        			private static final long serialVersionUID = 5063161656920609389L;

			        			public void execute()
			        			{
			        				final int id = art[0].getUniqueNumber();
			        				
			        				Card tempCard = tempCardMap.get(id);
			        				
			        				//CardList cl = new CardList();
			        				/*
			        				cl = cl.filter(new CardListFilter(){
			        					public boolean addCard(Card c)
			        					{
			        						
			        						return c.getUniqueNumber() == id;
			        					}
			        				});
			        				*/
			        			
			        				//Card temp = cl.get(0);
				        				
				        			art[0].setBaseAttack(tempCard.getBaseAttack());
				        			art[0].setBaseDefense(tempCard.getBaseDefense());
			        				
			        			}
			                };//Command
			                
			                art[0].setBaseAttack(5);
			                art[0].setBaseDefense(5);
			                
			        		AllZone.EndOfTurn.addUntil(creatureUntilEOT);
			        	}
			        	else 
			        	{	
			        		final Command nonCreatureUntilEOT = new Command()
			        		{
								private static final long serialVersionUID = 248122386218960073L;

								public void execute()
			        			{
			        				art[0].removeType("Creature");
			        				art[0].setBaseAttack(0);
			        				art[0].setBaseDefense(0);
			        			}
			                };//Command
			                
			                art[0].addType("Creature");
			                art[0].setBaseAttack(5);
			                art[0].setBaseDefense(5);
			                
			        		AllZone.EndOfTurn.addUntil(nonCreatureUntilEOT);
			        	}//noncreature artifact
			        		
		        	}
		        }//for
			}//resolve
			
			public boolean canPlay()
	        {
	          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                  turn[0] != AllZone.Phase.getTurn() &&
	                  card2.getCounters(Counters.LOYALTY) >= 5 &&
	          		  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	          		  !AllZone.Phase.getPhase().equals("End of Turn") &&
	          		  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	          		  && AllZone.Stack.size() == 0;
	        }//canPlay()
			
			public boolean canPlayAI()
			{
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
				CardList list = new CardList(play.getCards());
				list = list.filter(new CardListFilter()
				{
					public boolean addCard(Card c)
					{
						return c.isArtifact() && (!c.isCreature() || (c.isCreature() && c.getBaseAttack() < 4)) && !c.hasSickness();
					}
				});
				return list.size() > 4 && AllZone.Phase.getPhase().equals("Main1") && card2.getCounters(Counters.LOYALTY) > 5;
			}
	      };
	      ability3.setDescription("-5: Artifacts you control become 5/5 artifact creatures until end of turn.");
	      ability3.setStackDescription(card2.getName() + " - Artifacts you control become 5/5 artifact creatures until end of turn.");
	      card2.addSpellAbility(ability3);
	      
	      return card2;
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Sarkhan Vol"))
	    {
	      final int turn[] = new int[1];
	      turn[0] = -1;

	      final Card card2 = new Card()
	      {
	        public void addDamage(int n)
	        {
	          subtractCounter(Counters.LOYALTY,n);
	          AllZone.GameAction.checkStateEffects();
	        }
	      };
	      card2.addCounter(Counters.LOYALTY,4);

	      card2.setOwner(owner);
	      card2.setController(owner);

	      card2.setName(card.getName());
	      card2.setType(card.getType());
	      card2.setManaCost(card.getManaCost());
	      card2.addSpellAbility(new Spell_Permanent(card2));
	      
	      final SpellAbility ability1 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.addCounter(Counters.LOYALTY, 1);
	          turn[0] = AllZone.Phase.getTurn();

	          final int boost = 1;
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	          CardList list = new CardList(play.getCards());

	          for(int i = 0; i < list.size(); i++)
	          {
	            final Card[] target = new Card[1];
	            target[0] = list.get(i);

	            final Command untilEOT = new Command()
	            {
				  private static final long serialVersionUID = 2893066467461166183L;

				  public void execute()
	              {
	                if(AllZone.GameAction.isCardInPlay(target[0]))
	                {
	                  target[0].addTempAttackBoost(-boost);
	                  target[0].addTempDefenseBoost(-boost);

	                  target[0].removeExtrinsicKeyword("Haste");
	                }
	              }
	            };//Command

	            if(AllZone.GameAction.isCardInPlay(target[0]))
	            {
	              target[0].addTempAttackBoost(boost);
	              target[0].addTempDefenseBoost(boost);

	              target[0].addExtrinsicKeyword("Haste");

	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }//if
	          }//for

	        }//resolve()
	        public boolean canPlay()
	        {
	          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                  turn[0] != AllZone.Phase.getTurn() &&
	          		  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	          		  !AllZone.Phase.getPhase().equals("End of Turn") &&
	          		  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	          		  && AllZone.Stack.size() == 0;
	        }//canPlay()
	        public boolean canPlayAI()
	        {
	          return AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && card2.getCounters(Counters.LOYALTY) < 7;
	        }
	      };//ability1
	      Input runtime1 = new Input()
	      {
			private static final long serialVersionUID = 3843631106383444950L;

			int check = -1;
	        public void showMessage()
	        {
	          if(check != AllZone.Phase.getTurn())
	          {
	            check = AllZone.Phase.getTurn();
	            turn[0] = AllZone.Phase.getTurn();

	            AllZone.Stack.push(ability1);
	            stop();
	          }
	        }
	      };//Input
	      ability1.setStackDescription(card2.getName() +" - Creatures you control get +1/+1 and gain haste until end of turn.");
	      ability1.setDescription("+1: Creatures you control get +1/+1 and gain haste until end of turn.");
	      ability1.setBeforePayMana(runtime1);
	      card2.addSpellAbility(ability1);
	      
	      final PlayerZone[] orig = new PlayerZone[1];
	      final PlayerZone[] temp = new PlayerZone[1];
	      final String[] controllerEOT = new String[1];
	      final Card[] target          = new Card[1];

	      final Command untilEOT = new Command()
	      {

			private static final long serialVersionUID = -815595604846219653L;

			public void execute()
	        {
	          //if card isn't in play, do nothing
	          if(! AllZone.GameAction.isCardInPlay(target[0]))
	            return;

	          target[0].setController(controllerEOT[0]);

	          ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
	          ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

	          //moveTo() makes a new card, so you don't have to remove "Haste"
	          //AllZone.GameAction.moveTo(playEOT[0], target[0]);
	          temp[0].remove(target[0]);
	          orig[0].add(target[0]);
	          target[0].untap();
	          target[0].removeExtrinsicKeyword("Haste");
	          
	          ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	          ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	        }//execute()
	      };//Command

	      final Ability ability2 = new Ability(card, "0")
	      {
			
			public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card2, getTargetCard()) )
	          {
	        	card2.subtractCounter(Counters.LOYALTY, 2);
	        	  
	            orig[0]       = AllZone.getZone(getTargetCard());
	            controllerEOT[0] = getTargetCard().getController();
	            target[0]        = getTargetCard();

	            //set the controller
	            getTargetCard().setController(card.getController());

	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            play.add(getTargetCard());
	            temp[0] = play;
	            orig[0].remove(getTargetCard());

	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);


	            getTargetCard().untap();
	            getTargetCard().addExtrinsicKeyword("Haste");

	            AllZone.EndOfTurn.addUntil(untilEOT);
	          }//is card in play?
	        }//resolve()
	        public boolean canPlayAI()
	        {
	          return false;
	        }//canPlayAI()
	        
	        public boolean canPlay()
	        {
	        	return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
                turn[0] != AllZone.Phase.getTurn() &&
                  card2.getCounters(Counters.LOYALTY) >= 2 &&
        		  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
        		  !AllZone.Phase.getPhase().equals("End of Turn") &&
        		  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
        		  && AllZone.Stack.size() == 0;
	        }
	      };//SpellAbility
	      ability2.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability2));
	      ability2.setDescription("-2: Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn.");
	      card2.addSpellAbility(ability2);
	      
	      //ability3
	      final Ability ability3 = new Ability(card2, "0")
	      {
	        public void resolve()
	        {
	          card2.subtractCounter(Counters.LOYALTY, 6);
	          turn[0] = AllZone.Phase.getTurn();

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
	          for (int i=0;i<5;i++)
	          {
	        	  play.add(getToken());
	          }
	          
	        }
	        Card getToken()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());
	          
	          c.setImageName("R 4 4 Dragon");
	          c.setName("Dragon");
	          c.setManaCost("R");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Dragon");
	          c.setBaseAttack(4);
	          c.setBaseDefense(4);
	          c.addIntrinsicKeyword("Flying");
	
	          return c;
	        }//makeToken()

	        public boolean canPlay()
	        {
	          return  AllZone.getZone(card2).is(Constant.Zone.Play) &&
	                  turn[0] != AllZone.Phase.getTurn() &&
	                  card2.getCounters(Counters.LOYALTY) >=6 &&
	                  AllZone.Phase.getActivePlayer().equals(card2.getController()) &&
	                  !AllZone.Phase.getPhase().equals("End of Turn") && 
	                  (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"))
	                  && AllZone.Stack.size() == 0;
	        }//canPlay()
	        public boolean canPlayAI()
	        {
	          return card2.getCounters(Counters.LOYALTY) > 6;
	        }
	      };//ability3
	      ability3.setStackDescription(card2.getName() +" - Put five 4/4 red Dragon creature tokens with flying onto the battlefield.");
	      ability3.setDescription("-6: Put five 4/4 red Dragon creature tokens with flying onto the battlefield.");
	      card2.addSpellAbility(ability3);
	      //end ability 2
	      
	      return card2;
	    }//*************** END ************ END **************************
	    
	    
	    
	    
	    return card;
	}
	
	
	
	// copies stats like attack, defense, etc..
	private static Card copyStats(Object o) {
		Card sim = (Card) o;
		Card c = new Card();

		c.setBaseAttack(sim.getBaseAttack());
		c.setBaseDefense(sim.getBaseDefense());
		c.setIntrinsicKeyword(sim.getKeyword());
		c.setName(sim.getName());
		c.setType(sim.getType());
		c.setText(sim.getSpellText());
		c.setManaCost(sim.getManaCost());

		return c;
	}// copyStats()
}