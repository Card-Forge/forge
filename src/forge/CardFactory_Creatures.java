package forge;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

public class CardFactory_Creatures {
	
	private static final int hasKeyword(Card c, String k)
    {
    	ArrayList<String> a = c.getKeyword();
    	for (int i = 0; i < a.size(); i++)
    		if (a.get(i).toString().startsWith(k))
    			return i;

    	return -1;
    }
	
	public static int shouldCycle(Card c) {
		ArrayList<String> a = c.getKeyword();
		for (int i = 0; i < a.size(); i++)
			if (a.get(i).toString().startsWith("Cycling"))
				return i;

		return -1;
	}
	
	public static int shouldTypeCycle(Card c) {
		ArrayList<String> a = c.getKeyword();
		for (int i = 0; i < a.size(); i++)
			if (a.get(i).toString().startsWith("TypeCycling"))
				return i;

		return -1;
	}
	
	public static int shouldTransmute(Card c) {
		ArrayList<String> a = c.getKeyword();
		for (int i = 0; i < a.size(); i++)
			if (a.get(i).toString().startsWith("Transmute"))
				return i;

		return -1;
	}

	public static int shouldSoulshift(Card c) {
		ArrayList<String> a = c.getKeyword();
		for (int i = 0; i < a.size(); i++)
			if (a.get(i).toString().startsWith("Soulshift"))
				return i;

		return -1;
	}

	public static int shouldVanish(Card c) {
		ArrayList<String> a = c.getKeyword();
		for (int i = 0; i < a.size(); i++)
			if (a.get(i).toString().startsWith("Vanishing"))
				return i;

		return -1;
	}
	
	

	
	public static Card getCard(final Card card, String cardName, String owner, CardFactory cf)
	{
			    
		//*************** START *********** START **************************
	    if(cardName.equals("Filthy Cur"))
	    {
	      final Card newCard = new Card()
	      {
	        public void addDamage(final int n, Card source)
	        {
	          super.addDamage(n, source);
	          SpellAbility ability = new Ability(card, "0")
	          {
	            public void resolve()
	            {
	              AllZone.GameAction.getPlayerLife(getController()).subtractLife(n);
	            }
	          };
	          ability.setStackDescription("Filthy Cur - causes " +n +" damage to " +getController());
	          AllZone.Stack.add(ability);
	        }//addDamage()
	      };//Card

	      newCard.setOwner(card.getOwner());
	      newCard.setController(card.getController());

	      newCard.setManaCost(card.getManaCost());
	      newCard.setName(card.getName());
	      newCard.addType("Creature");
	      newCard.addType("Hound");
	      newCard.setText(card.getSpellText());
	      newCard.setBaseAttack(card.getBaseAttack());
	      newCard.setBaseDefense(card.getBaseDefense());

	      newCard.addSpellAbility(new Spell_Permanent(newCard));

	      return newCard;
	    }//*************** END ************ END **************************

	    //*************** START *********** START **************************
	    else if(cardName.equals("Belligerent Hatchling") || cardName.equals("Noxious Hatchling")
	    		||cardName.equals("Shrewd Hatchling") ||cardName.equals("Sturdy Hatchling")
	    		 || cardName.equals("Voracious Hatchling"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        	 card.addCounter(Counters.M1M1, 4); 
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 4757054648163014149L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription(cardName + "enters the battlefield with four -1/-1 counters on it.");
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************

		  //*************** START *********** START **************************
	    else if(cardName.equals("Lurking Informant"))
	       {
	         final SpellAbility a1 = new Ability_Tap(card, "2")
	         {
	            private static final long serialVersionUID = 1446529067071763245L;
	             public void resolve()
	           {
	                    String player = getTargetPlayer();
	                    
	                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	                    CardList libList = new CardList(lib.getCards());
	                    Card c = libList.get(0);
	                    String[] choices = {"Yes", "No"};
	                    if(card.getController().equals(Constant.Player.Human)) 
	                    {
	                         Object o = AllZone.Display.getChoice("Mill " + c.getName() + " ?" , choices);
	                         if(o.equals("Yes"))
	                           {lib.remove(c);
	                            grave.add(c);
	                           }
	                    }
	                    else {
	                    CardList landlist = new CardList();
	                   landlist.addAll(AllZone.Human_Play.getCards());
	                   // i have no better idea how AI could use it then letting draw unneeded lands
	                   // this part will be good place to use card values lists or card values deck info 
	                   if (countLands(card) > 5 && !c.getType().contains("Land")) 
	                                  {
	                                      lib.remove(c);
	                                         grave.add(c);
	                                  }
	                  if (countLands(card) <= 5)
	                                      {
	                                   lib.remove(c);
	                                   grave.add(c);
	                                      }
	                    }
	                   
	            
	           }
	             private int countLands(Card c)
	           {
	              PlayerZone play = AllZone.getZone(Constant.Zone.Play, c
	                    .getController());
	              CardList lands = new CardList(play.getCards());
	              lands = lands.getType("Land");
	              return lands.size();
	           } 
	           
	           public boolean canPlayAI()
	           {
	               String player = getTargetPlayer();
	                PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                CardList libList = new CardList(lib.getCards());
	                return libList.size() > 0;
	           }
	         };//SpellAbility
	         card.addSpellAbility(a1);
	         a1.setDescription("2, tap: Look at the top card of target player's library. You may put that card into that player's graveyard.");
	         a1.setStackDescription("Lurking Informant ability");
	         a1.setBeforePayMana(new Input_PayManaCost(a1));
	         a1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(a1));
	       }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	       else if(cardName.equals("Captain of the Watch"))
	       {
	          
	          final SpellAbility comesIntoPlayAbility = new Ability(card, "0")
	           {
	             public void resolve()
	             {
	               makeToken();
	               makeToken();
	               makeToken();
	             }//resolve()
	             
	             public void makeToken()
	            {
	                Card c = new Card();

	                c.setName("Soldier");
	                  c.setImageName("W 1 1 Soldier");

	                  c.setOwner(card.getController());
	                  c.setController(card.getController());

	                  c.setManaCost("W");
	                  c.setToken(true);
	                 
	                  c.addType("Creature");
	                  c.addType("Soldier");
	                  c.setBaseAttack(1);
	                  c.setBaseDefense(1);

	                  PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	                  play.add(c);
	              }

	           }; //comesIntoPlayAbility
	           
	           Command intoPlay = new Command()
	           {
	            private static final long serialVersionUID = 8778828278589063477L;

	            public void execute()
	                {
	                  comesIntoPlayAbility.setStackDescription(card.getName() + " - put three 1/1 white Soldier creature tokens into play.");
	                  AllZone.Stack.add(comesIntoPlayAbility);
	                }
	           };
	           
	           card.addComesIntoPlayCommand(intoPlay);
	}//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Dimir Guildmage"))
	       {
	         final SpellAbility a1 = new Ability(card, "3 B")
	         {
	            private static final long serialVersionUID = 1446529067071763245L;

	            public void chooseTargetAI()
	            {
	            setTargetPlayer(Constant.Player.Human);	
	            }
	            
	            public boolean canPlay()
	            {
	            	 if (((AllZone.Phase.getPhase().equals(Constant.Phase.Main2)&& AllZone.Phase.getActivePlayer() == card.getController()) || (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer() == card.getController())) && AllZone.GameAction.isCardInPlay(card) )
	            	        return true;
	            	     else  
	            	          return false;
	            }
	             public void resolve()
	           {
	                    String player = getTargetPlayer();
	                    if(player.equals(Constant.Player.Human))
				            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
				          else
				            AllZone.GameAction.discardRandom(Constant.Player.Computer);  // wise discard should be here  
   	             
	           }	            
	           
	           public boolean canPlayAI()
	           {
	               String player = getTargetPlayer();
	                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
	                CardList HandList = new CardList(hand.getCards());
	                return HandList.size() > 0;
	           }
	         };//SpellAbility a1
	         
	         final SpellAbility a2 = new Ability(card, "3 U")
	         {
	            private static final long serialVersionUID = 1446529067071763245L;
	            
	            public void chooseTargetAI()
	            {
	            	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human);
	            	CardList LibList = new CardList(lib.getCards());
	            	if (LibList.size()<3) setTargetPlayer(Constant.Player.Human); else setTargetPlayer(Constant.Player.Computer);
	            }	           
	            public boolean canPlay()
	            {
	            	 if (((AllZone.Phase.getPhase().equals(Constant.Phase.Main2)&& AllZone.Phase.getActivePlayer() == card.getController()) || (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer() == card.getController())) && AllZone.GameAction.isCardInPlay(card) )
	            	        return true;
	            	     else  
	            	          return false;
	            }
	             public void resolve()
	           {
	                    String player = getTargetPlayer();
	                    AllZone.GameAction.drawCard(player);  
   	             
	           }	            
	           
	           public boolean canPlayAI()
	           {	               
	                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
	                PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human);
	                CardList HandList = new CardList(hand.getCards());
	                CardList LibList = new CardList(lib.getCards());
	                return ((HandList.size() < 3) && (LibList.size()>3));
	           }
	         };//SpellAbility a2
	         card.addSpellAbility(a1);
	         a1.setDescription("3 B: Target player discards a card.  Play this ability only any time you could play a sorcery.");
	         a1.setStackDescription(card + "Target player discards a card.");
	         a1.setBeforePayMana(new Input_PayManaCost(a1));
	         a1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(a1));
	         card.addSpellAbility(a2);
	         a2.setDescription("3 U: Target player draws a card.  Play this ability only any time you could play a sorcery.");
	         a2.setStackDescription(card + "Target player draws a card.");
	         a2.setBeforePayMana(new Input_PayManaCost(a2));
	         a2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(a2));
	       }//*************** END ************ END **************************

	    
		  //*************** START *********** START **************************
	    else if(cardName.equals("Street Wraith"))
	       {
	         final SpellAbility a1 = new Ability_Hand(card, "0")
	         {

	        		  private static final long serialVersionUID = -4960704261761785512L;

	        		  public boolean canPlayAI() {return false;}

	        	      public void resolve()
	        	      {
	        	    	//AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(2);
	        	        AllZone.GameAction.discard(card);
	        	        AllZone.GameAction.drawCard(card.getController());
	        	        card.cycle();	        	 
	        	    };
	        	 	          
	          
	         };//SpellAbility
	         
	         Input runtime = new Input()
		        {
		          private static final long serialVersionUID = 1959709104655340395L;
		  		boolean once = true;
		          public void showMessage()
		          {
		            //this is necessary in order not to have a StackOverflowException
		            //because this updates a card, it creates a circular loop of observers
		            if(once)
		            {
		              once = false;
		              AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(2);
		              AllZone.Stack.add(a1);

		              stop();
		            }
		          }//showMessage()
		        };
		        
		     a1.setBeforePayMana(runtime);
	         card.addSpellAbility(a1);
	         a1.setDescription("Cycling - (Pay 2 life, Discard this card: Draw a card.)");
     	     a1.setStackDescription(card +" Cycling: Draw a card");
	    
	       }//*************** END ************ END **************************

	    //*************** START *********** START **************************
	    else if(cardName.equals("Street Wraith"))
	       {
	         final SpellAbility a1 = new Ability_Hand(card, "0")
	         {

	        		  private static final long serialVersionUID = -4960704261761785512L;

	        		  public boolean canPlayAI() {return false;}

	        	      public void resolve()
	        	      {
	        	    	AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(2);
	        	        AllZone.GameAction.discard(card);
	        	        AllZone.GameAction.drawCard(card.getController());
	        	        card.cycle();	        	 
	        	    };
	        	 	          
	          
	         };//SpellAbility
	         card.addSpellAbility(a1);
	         a1.setDescription("Cycling - (Pay 2 life, Discard this card: Draw a card.)");
     	     a1.setStackDescription(card +" Cycling: Draw a card");
	    
	       }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Shinka Gatekeeper"))
	    {
	      final Card newCard = new Card()
	      {
	        public void addDamage(final int n, Card source)
	        {
	          super.addDamage(n, source);
	          SpellAbility ability = new Ability(card, "0")
	          {
	            public void resolve()
	            {
	              AllZone.GameAction.getPlayerLife(getController()).subtractLife(n);
	            }
	          };
	          ability.setStackDescription("Shinka Gatekeeper - causes " +n +" damage to " +getController());
	          AllZone.Stack.add(ability);
	        }//addDamage()
	      };//Card

	      newCard.setOwner(card.getOwner());
	      newCard.setController(card.getController());

	      newCard.setManaCost(card.getManaCost());
	      newCard.setName(card.getName());
	      newCard.addType("Creature");
	      newCard.addType("Ogre");
	      newCard.addType("Warrior");
	      newCard.setText(card.getSpellText());
	      newCard.setBaseAttack(card.getBaseAttack());
	      newCard.setBaseDefense(card.getBaseDefense());

	      newCard.addSpellAbility(new Spell_Permanent(newCard));

	      return newCard;
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Jackal Pup"))
	    {
	      final Card newCard = new Card()
	      {
	        public void addDamage(final int n, final Card source)
	        {
	          super.addDamage(n, source);
	          SpellAbility ability = new Ability(card, "0")
	          {
	            public void resolve()
	            {
	              AllZone.GameAction.getPlayerLife(getController()).subtractLife(n);
	            }
	          };
	          ability.setStackDescription("Jackal Pup - causes " +n +" damage to " +getController());
	          AllZone.Stack.add(ability);
	        }//addDamage()
	      };//Card

	      newCard.setOwner(card.getOwner());
	      newCard.setController(card.getController());

	      newCard.setManaCost(card.getManaCost());
	      newCard.setName(card.getName());
	      newCard.addType("Creature");
	      newCard.addType("Hound");
	      newCard.setText(card.getSpellText());
	      newCard.setBaseAttack(card.getBaseAttack());
	      newCard.setBaseDefense(card.getBaseDefense());

	      newCard.addSpellAbility(new Spell_Permanent(newCard));

	      return newCard;
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Stuffy Doll"))
	    {
	      final Card newCard = new Card()
	      {
	    	Card c = this;
	        public void addDamage(final int n, final Card source)
	        {
	          super.addDamage(n, source);
	          final String opponent = AllZone.GameAction.getOpponent(card.getOwner());

	          SpellAbility ability = new Ability(card, "0")
	          {
	            public void resolve()
	            {
	              AllZone.GameAction.getPlayerLife(opponent).subtractLife(n);
	              
	              if (c.getKeyword().contains("Lifelink"))
	            	  GameActionUtil.executeLifeLinkEffects(c,n);

                  CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
        		  for (Card crd : cl)
        		  {
        			   GameActionUtil.executeGuiltyConscienceEffects(c, crd, n);
        		  }
	              
	            }
	          };
	          ability.setStackDescription("Stuffy Doll - causes " +n +" damage to " +opponent);
	          AllZone.Stack.add(ability);
	        }//addDamage()
	      };//Card

	      newCard.setOwner(card.getOwner());
	      newCard.setController(card.getController());

	      newCard.setManaCost(card.getManaCost());
	      newCard.setName(card.getName());
	      newCard.addType("Artifact");
	      newCard.addType("Creature");
	      newCard.addType("Construct");
	      newCard.setText("Whenever damage is dealt to Stuffy Doll, it deals that much damage to your opponent.");
	      newCard.setBaseAttack(0);
	      newCard.setBaseDefense(1);

	      newCard.addIntrinsicKeyword("Indestructible");

	      final Ability_Tap ability = new Ability_Tap(newCard)
	      {
			private static final long serialVersionUID = 577739727089395613L;

			public void resolve()
	        {
	          newCard.addDamage(1, newCard);
	          
	          if (newCard.getKeyword().contains("Lifelink"))
	        	  GameActionUtil.executeLifeLinkEffects(newCard, 1);
	          
	          CardList cl = CardFactoryUtil.getAurasEnchanting(newCard, "Guilty Conscience");
    		  for (Card crd : cl)
    		  {
    			   GameActionUtil.executeGuiltyConscienceEffects(newCard, crd, 1);
    		  }
	        }
	      };//SpellAbility
	      ability.setDescription("tap: Stuffy Doll deals 1 damage to itself.");
	      ability.setStackDescription("Stuffy Doll - deals 1 damage to itself.");
	      ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));

//	      card.addSpellAbility(ability);
//	      return card;
	///*
	      newCard.addSpellAbility(new Spell_Permanent(newCard));
	      newCard.addSpellAbility(ability);

	      return newCard;
	//*/
	    }//*************** END ************ END **************************


	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Serra Avenger"))
	    {
	      SpellAbility spell = new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -1148518222979323313L;

			public boolean canPlay()
	        {
	          return super.canPlay() && 6 < AllZone.Phase.getTurn();
	        }
	      };
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }
	    //*************** END ************ END **************************

	  //*************** START *********** START **************************
	    else if(cardName.equals("Force of Savagery"))
	    {
	      SpellAbility spell = new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 1603238129819160467L;

			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());

	          return list.containsName("Glorious Anthem") || list.containsName("Gaea's Anthem");
	        }
	      };
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }
	    //*************** END ************ END **************************
	    

	    //*************** START *********** START **************************
	    else if(cardName.equals("Greater Forgeling"))
	    {
	      final Command untilEOT = new Command()
	      {
			private static final long serialVersionUID = -4569751606008597903L;

			public void execute()
	        {
	          if(AllZone.GameAction.isCardInPlay(card))
	          {
	            card.addTempAttackBoost(-3);
	            card.addTempDefenseBoost(3);
	          }
	        }
	      };

	      SpellAbility ability = new Ability(card, "1 R")
	      {
	        public boolean canPlayAI()
	        {
	          return MyRandom.random.nextBoolean() && CardFactoryUtil.AI_doesCreatureAttack(card) &&
	              3 < card.getNetDefense();
	        }
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(card))
	          {
	            card.addTempAttackBoost(3);
	            card.addTempDefenseBoost(-3);
	            AllZone.EndOfTurn.addUntil(untilEOT);
	          }
	        }
	      };

	      ability.setDescription("1 R: Greater Forgeling gets +3/-3 until end of turn.");
	      ability.setStackDescription(card +" gets +3/-3 until end of turn.");
	      card.addSpellAbility(ability);
	    }
	    //*************** END ************ END **************************
	    


	    //*************** START *********** START **************************
	    else if(cardName.equals("Caller of the Claw"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          int stop = countGraveyard();
	          for(int i = 0; i < stop; i++)
	            makeToken();
	        }//resolve()
	        int countGraveyard()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          CardList list = new CardList(grave.getCards());
	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isCreature() && (c.getTurnInZone() == AllZone.Phase.getTurn());
	            }
	          });
	          return list.size();
	        }//countGraveyard()
	        void makeToken()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());
	          
	          c.setImageName("G 2 2 Bear");
	          c.setName("Bear");
	          c.setManaCost("G");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Bear");
	          c.setBaseAttack(2);
	          c.setBaseDefense(2);

	          play.add(c);
	        }//makeToken()
	      };//SpellAbility

	      Command comesIntoPlay = new Command()
	      {
			private static final long serialVersionUID = 8485080996453793968L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };//Command
	      ability.setStackDescription("Caller of the Claw - Put a 2/2 green Bear creature token into play for each nontoken creature put into your graveyard from play this turn.");
	      card.addComesIntoPlayCommand(comesIntoPlay);

	      SpellAbility spell = new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 6946020026681536710L;

			public boolean canPlayAI() {return super.canPlay();}
	      };
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Kiki-Jiki, Mirror Breaker"))
	    {
	      final CardFactory cfact = cf;
	    	
	      final SpellAbility ability = new Ability_Tap(card)
	      {
			private static final long serialVersionUID = -943706942500499644L;
			
			public boolean canPlayAI() {return getCreature().size() != 0;}
	        public void chooseTargetAI()
	        {
	          setTargetCard(getCreature().get(0));
	        }
	        CardList getCreature()
	        {
	          CardList list = null;
	          if(card.getController().equals(Constant.Player.Human)){
	             list = new CardList(AllZone.Human_Play.getCards());
	          }
	          else{
	             list = new CardList(AllZone.Computer_Play.getCards());
	          }

	          list = list.getType("Creature");
	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return (!c.getType().contains("Legendary") );
	            }
	          });
	          CardListUtil.sortAttack(list);
	          return list;
	        }//getCreature()
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && getTargetCard().getController().equals(card.getController()) 
	       		  && CardFactoryUtil.canTarget(card,getTargetCard()) )
	          {
	        	Card copy;
	        	if (!getTargetCard().isToken())
	        	{
	        		//CardFactory cf = new CardFactory("cards.txt");
	        		
	        		
		            //copy creature and put it into play
		            //copy = getCard(getTargetCard(), getTargetCard().getName(), card.getController());
		            copy = cfact.getCard(getTargetCard().getName(), getTargetCard().getOwner());
	        		copy.setToken(true);
		            
		            if (getTargetCard().isFaceDown()) {
		            	copy.setIsFaceDown(true);
		            	copy.setManaCost("");
		            	copy.setBaseAttack(2);
		            	copy.setBaseDefense(2);
		            	copy.setIntrinsicKeyword(new ArrayList<String>()); //remove all keywords
		    			copy.setType(new ArrayList<String>()); //remove all types
		    			copy.addType("Creature");
		    			copy.clearSpellAbility(); //disallow "morph_up"
		            }
		            copy.addIntrinsicKeyword("Haste");
	        	}
	        	else //isToken()
	        	{
	        		Card c = getTargetCard();
	        		copy = new Card();
	        		
	        		copy.setName(c.getName());

	                copy.setOwner(c.getController());
	                copy.setController(c.getController());

	                copy.setManaCost(c.getManaCost());
	                copy.setToken(true);

	                copy.setType(c.getType());

	                copy.setBaseAttack(c.getBaseAttack());
	                copy.setBaseDefense(c.getBaseDefense());
	                copy.addIntrinsicKeyword("Haste");
	        	}

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            play.add(copy);

	            

	            //have to do this since getTargetCard() might change
	            //if Kiki-Jiki somehow gets untapped again
	            final Card[] target = new Card[1];
	            target[0] = copy;
	            Command atEOT = new Command()
	            {
				  private static final long serialVersionUID = 7803915905490565557L;
	              public void execute()
	              {
	                //technically your opponent could steal the token
	                //and the token shouldn't be sacrificed
	                if(AllZone.GameAction.isCardInPlay(target[0]))
	                  AllZone.GameAction.sacrifice(target[0]); //maybe do a setSacrificeAtEOT, but probably not.
	              }
	            };//Command
	            AllZone.EndOfTurn.addAt(atEOT);
	          }//is card in play?
	        }//resolve()
	      };//SpellAbility

	      Input runtime = new Input()
	      {
			private static final long serialVersionUID = 7171284831370490875L;

			public void showMessage()
	        {
	          //get all non-legendary creatures you control
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Play.getCards());
	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isCreature() &&
	                  (!c.getType().contains("Legendary"));
	            }
	          });
	          stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list, "Select target creature to copy that is not legendary.", true));
	        }
	      };//Input
	      ability.setStackDescription("Kiki-Jiki - copy card.");
	      ability.setDescription("tap: Put a token into play that's a copy of target nonlegendary creature you control. That creature token has haste. Sacrifice it at end of turn.");
	      ability.setBeforePayMana(runtime);
	      card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	    
	  /*
	  //*************** START *********** START **************************
	    else if(cardName.equals("Azusa, Lost but Seeking"))
	    {
	    	
	    	final Ability ability = new Ability(card, "0")
	    	{
	    		public boolean canPlay()
	    		{
	    			SpellAbility sa;
	    	    	for (int i=0; i<AllZone.Stack.size(); i++)
	    	    	{
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card))
	    	    	          return false;
	    	    	}
	    			
	    			String player = card.getController();
	    			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
	    			CardList lands = new CardList(hand.getCards());
	    			lands = lands.getType("Land");
	    			
	    			return lands.size() > 0 &&
	    				(AllZone.Phase.getPhase().equals(Constant.Phase.Main2) || AllZone.Phase.getPhase().equals(Constant.Phase.Main1))
	    				&& AllZone.GameAction.isCardInPlay(card) && CardFactoryUtil.canUseAbility(card);
	    		}
	    		public void resolve()
	    		{

	    			String player = card.getController();
	    			
	    			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
	    			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
	    			
	    			
	    			CardList lands = new CardList(hand.getCards());
	    			lands = lands.getType("Land");
	    			
	    			if (lands.size() > 0)
	    			{
	    				if (player.equals(Constant.Player.Human))
	    				{
			    			Object o = AllZone.Display.getChoiceOptional("Select land to play", lands.toArray());
			    			if (o!=null)
			    			{
			    				Card c = (Card)o;
			    				hand.remove(c);
			    				play.add(c);
			    			}
	    				}
	    				else
	    				{
	    					Card c = lands.get(0);
	    					hand.remove(c);
	    					play.add(c);
	    				}
	    				card.setAbilityUsed(card.getAbilityUsed()+1);
	    			}
	    		}
	    	};
	    	
	    	card.addSpellAbility(ability);
	    	ability.setDescription("You may play two additional lands on each of your turns.");
	        ability.setStackDescription(card.getName() + " - " + card.getController() + " plays an additional land.");
	    }//*************** END ************ END **************************  
	    
		*/

	    //*************** START *********** START **************************
	    else if(cardName.equals("Wellwisher"))
	    {
	      final SpellAbility ability = new Ability_Tap(card, "0")
	      {
			private static final long serialVersionUID = 1446529067071763247L;
			
			public boolean canPlay()
	        {
	          setStackDescription(card.getName() +" - " +card.getController() +" gains " +countElf() +" life.");

	          return super.canPlay();
	        }
	        public void resolve()
	        {
	          AllZone.GameAction.getPlayerLife(card.getController()).addLife(countElf());
	        }
	        int countElf()
	        {
	          //get all creatures
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Play.getCards());
	          list.addAll(AllZone.Computer_Play.getCards());
	          list = list.getType("Elf");
	          return list.size();
	        }
	      };//SpellAbility
	      ability.setDescription("tap: You gain 1 life for each Elf in play.");
	      ability.setBeforePayMana(new Input_NoCost_TapAbility((Ability_Tap) ability));
	      card.addSpellAbility(ability);
	    }//*************** END ************ END **************************

	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Sliver Overlord"))
	    {
	    	//final String player = card.getController();
		    final SpellAbility ability = new Ability(card, "3")
			  {
			      public boolean canPlay()
			      {
			    	  SpellAbility sa;
			    	  for (int i=0; i<AllZone.Stack.size(); i++)
			    	  {
			    	       sa = AllZone.Stack.peek(i);
			    	       if (sa.getSourceCard().equals(card))
			    	             return false;
			    	  }
			    	  
			    	  if (AllZone.GameAction.isCardInPlay(card))
			    		  return true;
			    	  else
			    		  return false;
			      }
			      public boolean canPlayAI() 
			      {
			    	  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
			    	  CardList list = new CardList(lib.getCards());
			    	  list = list.getType("Sliver");
			    	  
			    	  if (list.size()==0)
			    		  return false;
			    	  
			    	  if (AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && list.size() > 0)
			    		  return true;
			    	  else
			    		  return false;
			    	  
			      }
			      
			      
			      public void resolve()
			      {
			    	  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
			    	  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
			    	  String player = card.getController();
			    	  
			    	  CardList list = new CardList(lib.getCards());
			    	  list = list.getType("Sliver");
			    	  
			    	  if (list.size()==0)
			    		  return;
			    	  
			    	  if (player.equals(Constant.Player.Computer))
			    	  {
			    		  Card sliver = CardFactoryUtil.AI_getBestCreature(list);
			    		  lib.remove(sliver);
			    		  hand.add(sliver);
			    	  }
			    	  else //human
			    	  {
			    		  Object o = AllZone.Display.getChoiceOptional("Select target Sliver", list.toArray());
			    		  Card sliver = (Card)o;
			    		  lib.remove(sliver);
			    		  hand.add(sliver);
			    	  }
			    	  AllZone.GameAction.shuffle(player);
			      }
			  };
			  
			  final SpellAbility ability2 = new Ability(card, "3")
		       {
		    	   
		    	   public void resolve()
		           {
		    		   
		    		 
		    		 Card c = getTargetCard();
		             if(AllZone.GameAction.isCardInPlay(c) && (c.getType().contains("Sliver") || c.getKeyword().contains("Changeling") )
		            	&& CardFactoryUtil.canTarget(card,c) )
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
		    		
		             CardList c = CardFactoryUtil.AI_getHumanCreature(card, true);
		             CardListUtil.sortAttack(c);
		             CardListUtil.sortFlying(c);
		             c = c.filter(new CardListFilter(){

						public boolean addCard(Card c) {
							return c.getType().contains("Sliver") || c.getKeyword().contains("Changeling");
						}
		            	 
		             });

		             if(c.isEmpty())
		               return false;

		             if(2 <= c.get(0).getNetAttack() && c.get(0).getKeyword().contains("Flying") &&
		            	c.get(0).getKeyword().contains("Sliver"))
		             {
		               setTargetCard(c.get(0));
		               return true;
		             }

		             CardListUtil.sortAttack(c);
		             if(4 <= c.get(0).getNetAttack() && c.get(0).getKeyword().contains("Sliver"))
		             {
		               setTargetCard(c.get(0));
		               return true;
		             }

		             return false;
		           }//canPlayAI()

		         public boolean canPlay()
		         {
		           return AllZone.GameAction.isCardInPlay(card);
		           
		         }//canPlay()
		       };//SpellAbility ability2

		       ability2.setBeforePayMana(new Input()
		       {
				private static final long serialVersionUID = 1489433384490805477L;

				public void showMessage()
		           {
		    		 String opponent = AllZone.GameAction.getOpponent(card.getController());
		             CardList slivers = new CardList(AllZone.getZone(Constant.Zone.Play, opponent).getCards());
		             slivers = slivers.getType("Sliver");
		             
		             stopSetNext(CardFactoryUtil.input_targetSpecific(ability2, slivers, "Select a Sliver", true));
		           }
		       });
		       
			  ability.setDescription("3: Search your library for a Sliver card, reveal that card, and put it into your hand. Then shuffle your library.");
			  ability.setStackDescription(card.getName() +" - search for a Sliver card and put it into your hand.");
			  
			  ability2.setDescription("3: Gain control of target Sliver.");
			  ability.setStackDescription(card.getName() +" - Gain control of target Sliver.");
			  
			  card.addSpellAbility(ability);
			  card.addSpellAbility(ability2);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Sliver Queen"))
	    {
	      final SpellAbility a1 = new Ability(card, "2")
	      {
	    	  
	    	public boolean canPlay()
	    	{
	    		SpellAbility sa;
	            //this is a hack, check the stack to see if this card has an ability on the stack
	            //if so, we can't use the ability
	            for (int i=0; i<AllZone.Stack.size(); i++)
	            {
	            	sa = AllZone.Stack.peek(i);
	            	if (sa.getSourceCard().equals(card))
	            			return false;
	            }
	            return AllZone.GameAction.isCardInPlay(card);
	    	}
	        public void resolve()
	        {
	          makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();

	          c.setImageName("C 1 1 Sliver");
	          c.setName("Sliver");

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setManaCost("");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Sliver");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);
	          
	          

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	        
	        public boolean canPlayAI()
	        {
	        	return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
	        }
	      };//SpellAbility
	      card.addSpellAbility(a1);
	      a1.setDescription("2: Put a 1/1 colorless Sliver creature token into play.");
	      a1.setStackDescription("Put a 1/1 colorless Sliver creature token into play.");

	      a1.setBeforePayMana(new Input_PayManaCost(a1));
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Korlash, Heir to Blackblade"))
	    {
	      ///////////////////////////////////////////

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void chooseTargetAI()
	        {
	          PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(p.getCards());
	          list = list.getName(card.getName());

	          AllZone.GameAction.discard(list.get(0));
	        }

	        public boolean canPlay()
	        {
	          PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(p.getCards());
	          list = list.getName(card.getName());
	          return 0 < list.size() && AllZone.getZone(card).getZone().equals(Constant.Zone.Play);
	        }

	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList list = new CardList(library.getCards());
	          CardList swamp = list.getType("Swamp");

	          for(int i = 0; i < 2 && (! swamp.isEmpty()); i++)
	          {
	            Card c = swamp.get(0);
	            swamp.remove(c);

	            library.remove(c);
	            play.add(c);
	            c.tap();
	          }
	          for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
	  			Command com = GameActionUtil.commands.get(effect);
	  			com.execute();
	  		  }
	          GameActionUtil.executeCardStateEffects();
	        }
	      };
	      Input removeCard = new Input()
	      {
			private static final long serialVersionUID = -8560221326412798885L;
			
			int n = 0;
	        public void showMessage()
	        {
	          //this is called twice, this is an ugly hack
	          if(n % 2 == 0)
	            stop();


	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(hand.getCards());

	          list = list.getName(card.getName());
	          AllZone.GameAction.discard(list.get(0));

	          AllZone.Stack.push(ability);
	          stop();
	        }
	      };
	      ability.setBeforePayMana(removeCard);

	      ability.setDescription("Grandeur - Discard Korlash and put two Swamps from your library into play tapped.");
	      ability.setStackDescription(card.getName() +" - Search for two swamps and put them into play tapped.");

	      card.addSpellAbility(ability);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Tarox Bladewing"))
	    {
	      final Command untilEOT = new Command()
	      {
			private static final long serialVersionUID = 2642394522583318055L;

			public void execute()
	        {
	          int n = card.getNetAttack();

	          card.addTempDefenseBoost(- n/2);
	          card.addTempAttackBoost(- n/2);
	        }
	      };

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void chooseTargetAI()
	        {
	          PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(p.getCards());
	          list = list.getName(card.getName());

	          AllZone.GameAction.discard(list.get(0));
	        }

	        public boolean canPlay()
	        {
	          PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(p.getCards());
	          list = list.getName(card.getName());
	          return 0 < list.size() && AllZone.getZone(card).getZone().equals(Constant.Zone.Play);
	        }

	        public void resolve()
	        {
	          card.addTempDefenseBoost(card.getNetAttack());
	          card.addTempAttackBoost(card.getNetAttack());

	          AllZone.EndOfTurn.addUntil(untilEOT);
	        }
	      };
	      Input removeCard = new Input()
	      {
			private static final long serialVersionUID = -1312910959802746127L;
			
			int n = 0;
	        public void showMessage()
	        {
	          //this is called twice, this is an ugly hack
	          if(n % 2 == 0)
	            stop();


	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(hand.getCards());

	          list = list.getName(card.getName());
	          AllZone.GameAction.discard(list.get(0));

	          AllZone.Stack.push(ability);
	          stop();
	        }
	      };
	      ability.setBeforePayMana(removeCard);

	      ability.setDescription("Grandeur - Discard another card named Tarox Bladewing: Tarox Bladewing gets +X/+X until end of turn, where X is his power.");
	      ability.setStackDescription(cardName +" - gets +X/+X until end of turn.");

	      card.addSpellAbility(ability);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Baru, Fist of Krosa"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void chooseTargetAI()
	        {
	          PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(p.getCards());
	          list = list.getName(card.getName());

	          AllZone.GameAction.discard(list.get(0));
	        }

	        public boolean canPlay()
	        {
	          PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(p.getCards());
	          list = list.getName(card.getName());
	          return 0 < list.size() && AllZone.getZone(card).getZone().equals(Constant.Zone.Play);
	        }

	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          CardList list = new CardList(play.getCards());
	          list = list.getType("Land");
	          makeToken(list.size());
	        }
	        void makeToken(int stats)
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setImageName("G X X Wurm");
	          c.setName("Wurm");
	          c.setManaCost("G");
	          c.setToken(true);
	          
	          c.addType("Creature");
	          c.addType("Wurm");
	          c.setBaseAttack(stats);
	          c.setBaseDefense(stats);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
	          play.add(c);
	        }//makeToken()
	      };
	      Input removeCard = new Input()
	      {
			private static final long serialVersionUID = 7738090787920616790L;
			
			int n = 0;
	        public void showMessage()
	        {
	          //this is called twice, this is an ugly hack
	          if(n % 2 == 0)
	            stop();


	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(hand.getCards());

	          list = list.getName(card.getName());
	          AllZone.GameAction.discard(list.get(0));

	          AllZone.Stack.push(ability);
	          stop();
	        }
	      };
	      ability.setBeforePayMana(removeCard);

	      ability.setDescription("Grandeur - Discard another card named Baru, Fist of Krosa: Put an X/X green Wurm creature token into play, where X is the number of lands that you control.");
	      ability.setStackDescription(cardName +" - put X/X token into play.");

	      card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Hunted Phantasm"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          for(int i = 0; i < 5; i++)
	            makeToken();
	        }
	        void makeToken()
	        {
	          //warning, different owner and controller
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(opponent);

	          c.setName("Goblin");
	          c.setImageName("R 1 1 Goblin");
	          c.setManaCost("R");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Goblin");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);


	          play.add(c);
	        }//makeToken()
	      };//SpellAbility

	      Command intoPlay = new Command()
	      {

			private static final long serialVersionUID = -5515684113290670830L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Hunted Phantasm - Opponent puts five 1/1 Goblin Creature tokens onto the battlefield.");
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Hunted Horror"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          for(int i = 0; i < 2; i++)
	            makeToken();
	        }
	        void makeToken()
	        {
	          //warning, different owner and controller
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(opponent);

	          c.setName("Centaur");
	          c.setImageName("G 3 3 Centaur Pro Black");
	          c.setManaCost("G");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Centaur");
	          c.setBaseAttack(3);
	          c.setBaseDefense(3);
	          c.addIntrinsicKeyword("Protection from black");

	          play.add(c);
	        }//makeToken()
	      };//SpellAbility

	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -5515684113290670830L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Hunted Horror - Opponent puts two 3/3 Centaur tokens with Protection from Black onto the battlefield.");
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Hunted Lammasu"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {	
	        	makeToken();
	        }
	        void makeToken()
	        {
	          //warning, different owner and controller
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(opponent);

	          c.setName("Horror");
	          c.setImageName("B 4 4 Horror");
	          c.setManaCost("B");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Horror");
	          c.setBaseAttack(4);
	          c.setBaseDefense(4);

	          play.add(c);
	        }//makeToken()
	      };//SpellAbility

	      Command intoPlay = new Command()
	      {
			
			private static final long serialVersionUID = -5515684113290670830L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Hunted Lammasu - Opponent puts a 4/4 black Horror creature token onto the battlefield.");
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Hunted Dragon"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          for(int i = 0; i < 3; i++)
	            makeToken();
	        }
	        void makeToken()
	        {
	          //warning, different owner and controller
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(opponent);

	          c.setName("Knight");
	          c.setImageName("W 2 2 Knight");
	          c.setManaCost("W");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Knight");
	          c.setBaseAttack(2);
	          c.setBaseDefense(2);
	          c.addIntrinsicKeyword("First Strike");

	          play.add(c);
	        }//makeToken()
	      };//SpellAbility

	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -5091710462434865200L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Hunted Dragon - Opponent puts 3 Knight tokens with First Strike into play");
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Hunted Troll"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {

	          for(int i = 0; i < 4; i++)
	            makeToken();
	        }
	        void makeToken()
	        {
	          //warning, different owner and controller
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(opponent);
	          
	          c.setName("Faerie");
	          c.setImageName("U 1 1 Faerie");
	          c.setManaCost("U");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Faerie ");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);
	          c.addIntrinsicKeyword("Flying");

	          play.add(c);
	        }//makeToken()
	      };//SpellAbility

	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 7599515041000061901L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Hunted Troll - Opponent puts 4 Faerie tokens with flying into play");
	      card.addComesIntoPlayCommand(intoPlay);


	      final Command untilEOT = new Command()
	      {
			private static final long serialVersionUID = -451839437837081897L;

			public void execute()
	        {
	          card.setShield(0);
	        }
	      };

	      final SpellAbility a1 = new Ability(card, "G")
	      {
	        public boolean canPlayAI() {return false;}

	        public void resolve()
	        {
	          card.addShield();
	          AllZone.EndOfTurn.addUntil(untilEOT);
	        }
	      };//SpellAbility
	      card.addSpellAbility(a1);
	      a1.setDescription("G: Regenerate Hunted Troll.");
	      a1.setStackDescription("Regenerate Hunted Troll");

	      a1.setBeforePayMana(new Input_PayManaCost(a1));
	    }//*************** END ************ END **************************




	  //*************** START *********** START **************************
	    else if(cardName.equals("Filigree Angel"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          int n = countArtifacts();

	          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	            life.addLife(3*n);

	        }
	        int countArtifacts()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          CardList list = new CardList(play.getCards());
	          list = list.getType("Artifact");
	          return list.size();
	        }

	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -319011246650583681L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() + " - " +card.getController() +" gains 3 life for each artifact he controls");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Venerable Monk") || cardName.equals("Kitchen Finks") || cardName.equals("Shu Grain Caravan"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = card;
	          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	          life.addLife(2);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 1832932499373431651L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" gains 2 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Radiant's Dragoons"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = card;
	          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	          life.addLife(5);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -7748429739046909730L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" gains 5 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Shu Soldier-Farmers") || cardName.equals("Staunch Defenders") ||
	       cardName.equals("Spiritual Guardian") || cardName.equals("Teroh's Faithful") || 
	       cardName.equals("Jedit's Dragoons") || cardName.equals("Loxodon Hierarch"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = card;
	          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	          life.addLife(4);
	        }
	      };
	      Command intoPlay = new Command()
	      {
	 		private static final long serialVersionUID = -1537994957313929513L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" gains 4 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    if(cardName.equals("Loxodon Hierarch"))
	    {
	      final Ability ability = new Ability(card, "G W")
	      {
	        public boolean canPlayAI() {return false;}

	        public void resolve()
	        {
	        	final Card[] c = AllZone.getZone(Constant.Zone.Play, card.getController()).getCards();

	            for(int i = 0; i < c.length; i++)
	              if(c[i].isCreature())
	                c[i].addShield();

	            AllZone.EndOfTurn.addUntil(new Command()
	            {
				  private static final long serialVersionUID = 5853778391858472471L;

				  public void execute()
	              {
	                for(int i = 0; i < c.length; i++)
	                  c[i].resetShield();
	              }
	            });
	        }//resolve()
	      };//SpellAbility

	      card.addSpellAbility(ability);
	      ability.setDescription("GW, Sacrifice Loxodon Hierarch: Regenerate each creature you control.");
	      ability.setStackDescription(cardName + " regenerate each of " + card.getController() + "'s creatures.");
	      ability.setBeforePayMana(new Input_PayManaCost_Ability(ability.getManaCost(), new Command()
	      {
			private static final long serialVersionUID = -8594393519904006680L;

			public void execute()
	        {
	          AllZone.GameAction.sacrifice(card);
	          AllZone.Stack.add(ability);
	        }
	      }));
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Springjack Shepherd"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        	int number = CardFactoryUtil.getNumberOfManaSymbolsControlledByColor("W", card.getController());
	        	for (int i=0;i<number;i++)
	        	{
	        		makeToken();
	        	}
	            
	        }//resolve()
	        public void makeToken()
	        {
		        Card c = new Card();
		
		        c.setOwner(card.getController());
		        c.setController(card.getController());
		
		        c.setImageName("W 0 1 Goat");
		        c.setName("Goat");
		        c.setManaCost("W");
		        c.setToken(true);
		
		        c.addType("Creature");
		        c.addType("Goat");
		
		        c.setBaseAttack(0);
		        c.setBaseDefense(1);
		
		        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		        play.add(c);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -5515684113290670830L;

			public void execute()
	        {
	          ability.setStackDescription("Springjack Shepherd - put a 0/1 white Goat creature token into play for each white mana symbol of permanents you control");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Gilder Bairn"))
	    {
	    	final SpellAbility a1 = new Ability(card,"2 GU")
	        {
	          public void resolve()
	          {
	        	Card c = getTargetCard();
	        	card.untap();
	        	
	        	if (c.sumAllCounters() == 0)
	        		return;
	        	else if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c))
	        	{
	        		//zerker clean up:
	        		for(Counters c_1 : Counters.values())
	                    if (c.getCounters(c_1) != 0)
	                        c.addCounter(c_1, c.getCounters(c_1));
	        	}
	          }
	          
	          public boolean canPlay()
	          {
	        	 
	        	  if (card.isTapped() && !card.hasSickness())
	        		  return true;
	        	  else 
	        		  return false;
	          }
	          
	          public void chooseTargetAI()
	          {
	        	  PlayerZone play  = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	          	  CardList perms = new CardList(play.getCards());
	          	  perms = perms.filter(new CardListFilter()
	          	  {
					public boolean addCard(Card c) {
						return c.sumAllCounters() > 0 && CardFactoryUtil.canTarget(card, c);
					}
	          	  });
	          	  perms.shuffle();
	          	  setTargetCard(perms.get(0)); //TODO: improve this.
	          }
	          
	          public boolean canPlayAI()
	          {
	        	  PlayerZone play  = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	          	  CardList perms = new CardList(play.getCards());
	          	  perms = perms.filter(new CardListFilter()
	          	  {
					public boolean addCard(Card c) {
						return c.sumAllCounters() > 0;
					}
	          	  });
	          	  return perms.size() > 0;
	          }
	        };//SpellAbility
	        
	        Input runtime = new Input()
		    {
				private static final long serialVersionUID = 1571239319226728848L;

				public void showMessage()
		        {
				  PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
			      PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
				  CardList perms = new CardList();
				  perms.addAll(human.getCards());
				  perms.addAll(comp.getCards());
				  
		          stopSetNext(CardFactoryUtil.input_targetSpecific(a1, perms, "Select target permanent.", true));
		        }
		    };  
	        
	        card.addSpellAbility(a1);
	        a1.setDescription("2 GU, Untap: For each counter on target permanent, put another of those counters on that permanent.");
	        
	        a1.setBeforePayMana(runtime);
	    
	  }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Order of Whiteclay"))
	    {
	    	final SpellAbility a1 = new Ability(card,"1 W W")
	        {
	          public void resolve()
	          {
	        	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	        	PlayerZone play  = AllZone.getZone(Constant.Zone.Play, card.getController());
	        	CardList creats = new CardList(grave.getCards());
	        	creats = creats.filter(new CardListFilter()
	      	  	{
					public boolean addCard(Card c)
					{
						return CardUtil.getConvertedManaCost(c.getManaCost()) <= 3;				
					}
	      		  
	      	  	});
	        	
	        	if (card.getController().equals(Constant.Player.Human))
	        	{
	        		Object o = AllZone.Display.getChoiceOptional("Choose a creature", creats.toArray());
	        		if (o!=null)
	        		{
	        			Card c = (Card)o;
	        			grave.remove(c);
	        			play.add(c);
	        			card.untap();
	        		}
	        	}
	        	else //Computer
	        	{
	        		Card c = creats.get(0);
	        		grave.remove(c);
	        		play.add(c);
	        		card.untap();
	        	}
	            
	          }
	          
	          public boolean canPlay()
	          {
	        	  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	        	  CardList creats = new CardList(grave.getCards());
	        	  creats = creats.filter(new CardListFilter()
	        	  {

					public boolean addCard(Card c)
					{
						return CardUtil.getConvertedManaCost(c.getManaCost()) <= 3;				
					}
	        		  
	        	  });
	        	  if (card.isTapped() && !card.hasSickness() && creats.size() > 0)
	        		  return true;
	        	  else 
	        		  return false;
	          }
	          
	          public boolean canPlayAI()
	          {
	        	  return true;
	          }
	        };//SpellAbility
	        card.addSpellAbility(a1);
	        a1.setDescription("1 W W, Untap:  Return target creature card with converted mana cost 3 or less from your graveyard to play.");
	        a1.setStackDescription(card.getName() + " - return target creature card with converted mana cost 3 or less from your graveyard to play.");

	        a1.setBeforePayMana(new Input_PayManaCost(a1));
	    
	  }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Patrol Signaler"))
	    {
	    	final SpellAbility a1 = new Ability(card,"1 W")
	        {
	          public void resolve()
	          {
	        	card.untap();
	            makeToken();
	          }
	          void makeToken()
	          {
	            Card c = new Card();

	            c.setName("Kithkin Soldier");
	            c.setImageName("W 1 1 Kithkin Soldier");

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setManaCost("W");
	            c.setToken(true);             
	            
	            c.addType("Creature");
	            c.addType("Kithkin");
	            c.addType("Soldier");
	            c.setBaseAttack(1);
	            c.setBaseDefense(1);

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            play.add(c);
	          }//makeToken()
	          public boolean canPlay()
	          {
	        	  SpellAbility sa;
	        	  for (int i=0; i<AllZone.Stack.size(); i++)
	              {
	              	sa = AllZone.Stack.peek(i);
	              	if (sa.getSourceCard().equals(card))
	              			return false;
	              }
	        	  
	        	  if (card.isTapped() && !card.hasSickness())
	        		  return true;
	        	  else 
	        		  return false;
	          }
	          
	          public boolean canPlayAI()
	          {
	        	  return true;
	          }
	        };//SpellAbility
	        card.addSpellAbility(a1);
	        a1.setDescription("1 W, Untap:  Put a 1/1 white Kithkin Soldier creature token into play.");
	        a1.setStackDescription(card.getName() + " - put a 1/1 white Kithkin Soldier creature token into play.");

	        a1.setBeforePayMana(new Input_PayManaCost(a1));
	    
	  }//*************** END ************ END **************************
		
	  //*************** START *********** START **************************
	    else if(cardName.equals("Guardian of Cloverdell"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          makeToken();
	          makeToken();
	          makeToken();
	        }//resolve()
	        
	        public void makeToken()
	        {
		        Card c = new Card();
		
		        c.setOwner(card.getController());
		        c.setController(card.getController());
		
		        c.setName("Kithkin Soldier");
		        c.setImageName("W 1 1 Kithkin Soldier");
		        c.setManaCost("W");
		        c.setToken(true);
		
		        c.addType("Creature");
		        c.addType("Kithkin");
		        c.addType("Soldier");
		
		        c.setBaseAttack(1);
		        c.setBaseDefense(1);
		
		        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		        play.add(c);
	        }

	      }; //ability
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -2030165469109890833L;
	        public void execute()
	        {
	          ability.setStackDescription(card.getName() + " - put three 1/1 white Kithkin Soldier creature tokens into play.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      
	      card.addComesIntoPlayCommand(intoPlay);
	      
	      final SpellAbility a2 = new Ability(card, "G")
		    {
	    	  public void chooseTargetAI()
		    	{
		    		PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		        	CardList kiths = new CardList(play.getCards());
		        	kiths = kiths.filter(new CardListFilter()
		        	{

						public boolean addCard(Card c) {
							if(c.getType().contains("Kithkin") || c.getKeyword().contains("Changeling"))
								return true;
							return false;
						}
		        	
		        	});
		        	
		        	if (kiths.size() != 0)
		        		setTargetCard(kiths.getCard(0));
		    	}
	    	  
		        public void resolve()
		        {
		          //get all Kithkin:
		          Card c = getTargetCard();
		
		          if(AllZone.GameAction.isCardInPlay(c))
		          {
		            //AllZone.getZone(c).remove(c);
		        	AllZone.GameAction.sacrifice(c);
		        	  
		        	PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
		        	life.addLife(1);
		          }
		        }//resolve
		        public boolean canPlayAI()
		        {
		        	if(AllZone.Computer_Life.getLife() < 4)
		        		return true;
		        	else
		        		return false;
		        }
		      };//SpellAbility
		      
		      Input runtime = new Input()
		      {
				private static final long serialVersionUID = 1775972794359668520L;

				public void showMessage()
		        {
		          CardList kith = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
		          kith = kith.getType("Kithkin");
		          
		          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, kith, "Select a Kithkin to sacrifice.", true));
		        }
		      };  
		      
		      card.addSpellAbility(a2);
		      a2.setDescription("Sacrifice a Kithkin: You gain 1 life.");
		      a2.setStackDescription(card.getController() + " gains 1 life.");
		      a2.setBeforePayMana(runtime);
	  	
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Ambassador Oak"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Elf Warrior");
	          c.setImageName("G 1 1 Elf Warrior");
	          c.setManaCost("G");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Elf");
	          c.addType("Warrior");

	          c.setBaseAttack(1);
	          c.setBaseDefense(1);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);

	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -8593037498281765796L;

			public void execute()
	        {
	          ability.setStackDescription("Ambassador Oak - put a 1/1 green Elf Warrior creature token into play.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Mudbutton Torchrunner"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(getTargetCard() != null && CardFactoryUtil.canDamage(card, getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()) )
	            getTargetCard().addDamage(3, card);
	          else
	            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(3);
	        }
	      };
	      Command leavesPlay = new Command()
	      {
			private static final long serialVersionUID = 2740098107360213191L;

			public void execute()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	            AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreaturePlayer(ability, true));
	          else
	          {
	            CardList list = CardFactoryUtil.AI_getHumanCreature(3, card, true);
	            CardListUtil.sortAttack(list);

	            if(MyRandom.percentTrue(50))
	              CardListUtil.sortFlying(list);

	            for(int i = 0; i < list.size(); i++)
	              if(2 <= list.get(i).getNetAttack())
	                ability.setTargetCard(list.get(i));

	            if(ability.getTargetCard() == null)
	              ability.setTargetPlayer(Constant.Player.Human);

	            AllZone.Stack.add(ability);
	          }
	        }//execute()
	      };//Command
	      card.addDestroyCommand(leavesPlay);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Mulldrifter"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          for(int i = 0; i < 2; i++)
	            AllZone.GameAction.drawCard(card.getController());
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 9072052875006010497L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() +" - " +card.getController() +" draws 2 cards.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);

	      card.addSpellAbility(new Spell_Evoke(card, "2 U")
	      {
			private static final long serialVersionUID = 5061298336319833956L;

			public boolean canPlayAI() {return false;}
	      });
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Meadowboon"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone zone = AllZone.getZone(Constant.Zone.Play, card.getController());
	          CardList list = new CardList(zone.getCards());
	          list = list.getType("Creature");
	          Card c;

	          for(int i = 0; i < list.size(); i++)
	          {
	            c = list.get(i);
	            c.addCounter(Counters.P1P1, 1);

	          }
	        }//resolve()
	      };
	      Command leavesPlay = new Command()
	      {
			private static final long serialVersionUID = -8083212279082607731L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() +" - " +card.getController() +" puts a +1/+1 on each creature he controls.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addLeavesPlayCommand(leavesPlay);

	      card.addSpellAbility(new Spell_Evoke(card, "3 W")
	      {
			private static final long serialVersionUID = 5001777391157132871L;

			public boolean canPlayAI() {return false;}
	      });
	    }//*************** END ************ END **************************





	    //*************** START *********** START **************************
	    else if(cardName.equals("Sengir Autocrat"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          for(int i = 0; i < 3; i++)
	            makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Serf");
	          c.setImageName("B 0 1 Serf");
	          c.setManaCost("B");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Serf");
	          c.setBaseAttack(0);
	          c.setBaseDefense(1);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -2966662310531173458L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() +" - " +card.getController() +" puts three 0/1 tokens into play");
	          AllZone.Stack.add(ability);
	        }
	      };
	      Command leavesPlay = new Command()
	      {
			private static final long serialVersionUID = 7242867764317580066L;

			public void execute()
	        {
	          CardList all = new CardList();
	          all.addAll(AllZone.Human_Play.getCards());
	          all.addAll(AllZone.Computer_Play.getCards());

	          all = all.getType("Serf");
	          for(int i = 0; i < all.size(); i++)
	            AllZone.GameAction.destroy(all.get(i));
	        }//execute
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	      card.addLeavesPlayCommand(leavesPlay);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Haunted Angel"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          makeToken();

	          //remove this card from the graveyard and from the game

	          //fixed - error if this card is copied like with Kiki, Jiki mirror breaker
	          //null pointer exception

	          if(card.isToken())
	            return;

	          AllZone.GameAction.removeFromGame(card);
	        }
	        void makeToken()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Angel");
	          c.setImageName("B 3 3 Angel");
	          c.setManaCost("B");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Angel");
	          c.setBaseAttack(3);
	          c.setBaseDefense(3);
	          c.addIntrinsicKeyword("Flying");

	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = 8044338194100037815L;

			public void execute()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          ability.setStackDescription(card.getName() +" - " +opponent +" puts a 3/3 flying token into play");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Angel of Mercy") || cardName.equals("Rhox Bodyguard"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = card;
	          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	          life.addLife(3);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 6457889481637587581L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() + " - " +card.getController() +" gains 3 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	  //*************** START *********** START **************************
	    else if(cardName.equals("Groundbreaker") || cardName.equals("Ball Lightning") ||
	       cardName.equals("Blistering Firecat") || cardName.equals("Spark Elemental"))
	    {
	      final SpellAbility spell = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(card))
	            AllZone.GameAction.sacrifice(card);
	        }
	      };
	      spell.setStackDescription("Sacrifice " +card);

	      final Command destroy = new Command()
	      {
			private static final long serialVersionUID = -3114095443175559598L;

			public void execute()
	        {
	          if(AllZone.GameAction.isCardInPlay(card))
	            AllZone.Stack.add(spell);
	        }
	      };

	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 1923179467785421942L;

			public void execute()
	        {
	          if(!card.isFaceDown())
	        	  AllZone.EndOfTurn.addAt(destroy);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	      card.addTurnFaceUpCommand(intoPlay);
	      card.setSacrificeAtEOT(true);
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    if (cardName.equals("Rukh Egg"))
	    {
	    	final SpellAbility ability = new Ability(card, "0")
	        {
	          public void resolve()
	          {
	            makeToken();
	          }
	          public void makeToken()
	          {
	        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	              Card c = new Card();

	              c.setOwner(card.getController());
	              c.setController(card.getController());

	              c.setName("Bird");
	              c.setImageName("R 4 4 Bird");
	              c.setManaCost("R");
	              c.setToken(true);

	              c.addType("Creature");
	              c.addType("Bird");
	              c.setBaseAttack(4);
	              c.setBaseDefense(4);
	              c.addIntrinsicKeyword("Flying");
	              
	              play.add(c);
	          }
	        }; //ability
	        ability.setStackDescription(cardName + " - Put a 4/4 red Bird creature token with flying onto the battlefield.");

	        
	        final Command createBird = new Command()
	        {
				private static final long serialVersionUID = 2856638426932227407L;

				public void execute(){
	        		AllZone.Stack.add(ability);
	        	}
	        };
	        
	        final Command destroy = new Command()
	        {
			  private static final long serialVersionUID = 2320128493809478823L;

			  public void execute()
	          {
	        	  AllZone.EndOfTurn.addAt(createBird);
	          }
	        };
	        
	        card.addDestroyCommand(destroy);
	    	
	    }//*************** END ************ END **************************

		

	    //*************** START *********** START **************************
	    else if(cardName.equals("Primal Plasma") || cardName.equals("Primal Clay"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          String choice = "";
	          String choices[] = {"3/3", "2/2 with flying", "1/6 with defender"};

	          if(card.getController().equals(Constant.Player.Human))
	          {
	            choice = (String) AllZone.Display.getChoice("Choose one", choices);
	          }
	          else
	            choice = choices[MyRandom.random.nextInt(3)];

	          if(choice.equals("2/2 with flying"))
	          {
	            card.setBaseAttack(2);
	            card.setBaseDefense(2);
	            card.addIntrinsicKeyword("Flying");
	          }
	          if(choice.equals("1/6 with defender"))
	          {
	            card.setBaseAttack(1);
	            card.setBaseDefense(6);
	            card.addIntrinsicKeyword("Defender");
	          }

	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 8957338395786245312L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() +" - choose: 3/3, 2/2 flying, 1/6 defender");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Phyrexian Gargantua"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	          life.subtractLife(2);

	          AllZone.GameAction.drawCard(card.getController());
	          AllZone.GameAction.drawCard(card.getController());
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -3016651104325305186L;

			public void execute()
	        {
	          ability.setStackDescription("Phyrexian Gargantua - " +card.getController() +" draws 2 cards and loses 2 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Phyrexian Rager"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	          life.subtractLife(1);

	          AllZone.GameAction.drawCard(card.getController());
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -4808204528319094292L;

			public void execute()
	        {
	          ability.setStackDescription("Phyrexian Rager - " +card.getController() +" draws 1 card and loses 1 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END ************************** 
	  
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Cao Ren, Wei Commander"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	          life.subtractLife(3);

	          AllZone.GameAction.drawCard(card.getController());
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {

			private static final long serialVersionUID = -6954568998599730697L;

			public void execute()
	        {
	          ability.setStackDescription("Cao Ren, Wei Commander - " +card.getController() +" loses 3 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END ************************** 


	    //*************** START *********** START **************************
	    else if(cardName.equals("Anger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = 1707519783018941582L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() +" - " +card.getOwner() +" creatures have Haste.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Valor"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -3009968608543593584L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() +" - " +card.getOwner() +" creatures have First Strike.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Wonder"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = 340877499423908818L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() +" - " +card.getOwner() +" creatures have Flying.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Kokusho, the Evening Star"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());

	          PlayerLife opp = AllZone.GameAction.getPlayerLife(opponent);
	          PlayerLife my = AllZone.GameAction.getPlayerLife(card.getController());

	          opp.subtractLife(5);
	          my.addLife(5);
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -2648843419728951661L;

			public void execute()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          ability.setStackDescription("Kokusho, the Evening Star - " +opponent +" loses 5 life and " +card.getController() +" gains 5 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Symbiotic Elf"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          makeToken();
	          makeToken();
	        }
	        void makeToken()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Insect");
	          c.setImageName("G 1 1 Insect");
	          c.setManaCost("G");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Insect");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);

	          play.add(c);
	        }//makeToken()
	      };//SpellAbility

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -7121390569051656027L;

			public void execute()
	        {
	          ability.setStackDescription("Symbiotic Elf - " +card.getController() +" puts two 1/1 tokens into play ");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Chittering Rats"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());

	          PlayerZone hand    = AllZone.getZone(Constant.Zone.Hand   , opponent);
	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, opponent);

	          if(hand.size() == 0)
	            return;

	          //randomly move card from hand to top of library
	          int index = MyRandom.random.nextInt(hand.size());
	          Card card = hand.get(index);

	          hand.remove(card);
	          library.add(card, 0);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 160195797163952303L;

			public void execute()
	        {
	          ability.setStackDescription("Chittering Rats - Opponent randomly puts a card from his hand on top of his library.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Kemuri-Onna"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          if(Constant.Player.Human.equals(opponent))
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	          else//computer
	            AllZone.GameAction.discardRandom(opponent);
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -6451753440468941341L;

			public void execute()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          ability.setStackDescription(card.getName() +" - " +opponent +" discards a card");

	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Highway Robber") || cardName.equals("Dakmor Ghoul"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          AllZone.GameAction.getPlayerLife(opponent).subtractLife(2);

	          AllZone.GameAction.getPlayerLife(card.getController()).addLife(2);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 321989007584083996L;

			public void execute()
	        {
	          ability.setStackDescription("Highway Robber - " +card.getController() +" gains 2 life and opponent loses 2 life.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Serpent Warrior"))
	    {
	      SpellAbility summoningSpell = new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 1937549779526559727L;

			public boolean canPlayAI()
	        {
	          return AllZone.Computer_Life.getLife()>3;
	        }
	      };
	      card.clearSpellAbility();
	      card.addSpellAbility(summoningSpell);

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {

	          AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(3);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 2334517567512130479L;

			public void execute()
	        {
	          ability.setStackDescription("Serpent Warrior - "
	                                      +card.getController() +" loses 3 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************

	  //*************** START *********** START **************************
	    else if(cardName.equals("Eviscerator"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(5);
	        }
	        public boolean canPlayAI()
	        {
	          return 8 < AllZone.Computer_Life.getLife();
	        }
	      };
	      Command intoPlay = new Command()
	      {

			private static final long serialVersionUID = -221296021551561668L;

			public void execute()
	        {
	          ability.setStackDescription("Eviscerator - " +card.getController() +" loses 5 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Foul Imp"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(2);
	        }
	        public boolean canPlayAI()
	        {
	          return 4 < AllZone.Computer_Life.getLife();
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -5371716833341661084L;

			public void execute()
	        {
	          ability.setStackDescription("Foul Imp - " +card.getController() +" loses 2 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Drekavac"))
	    {
	      final Input discard = new Input()
	      {
			private static final long serialVersionUID = -6392468000100283596L;
			
			public void showMessage()
	        {
	          AllZone.Display.showMessage("Discard from your hand a non-creature card");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectCard(Card c, PlayerZone zone)
	        {
	          if(zone.is(Constant.Zone.Hand) && c.isCreature())
	          {
	            AllZone.GameAction.discard(c);
	            stop();
	          }
	        }
	        public void selectButtonCancel()
	        {
	        	AllZone.GameAction.sacrifice(card);
	        	stop();
	        }
	      };//Input

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	          {
	            if(AllZone.Human_Hand.getCards().length == 0)
	              AllZone.GameAction.sacrifice(card);
	            else
	              AllZone.InputControl.setInput(discard);
	          }
	          else
	          {
	            CardList list = new CardList(AllZone.Computer_Hand.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return (!c.isCreature());
	              }
	            });
	            AllZone.GameAction.discard(list.get(0));
	          }//else
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 9202753910259054021L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" sacrifices Drekavac unless he discards a non-creature card");
	          AllZone.Stack.add(ability);
	        }
	      };
	      SpellAbility spell = new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -2940969025405788931L;

			//could never get the AI to work correctly
	        //it always played the same card 2 or 3 times
	        public boolean canPlayAI() {return false;}

	        public boolean canPlay()
	        {
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          CardList list = new CardList(hand.getCards());
	          list.remove(card);
	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return (!c.isCreature());
	            }
	          });
	          return list.size() != 0;
	        }//canPlay()
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Minotaur Explorer"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          if(hand.getCards().length == 0)
	            AllZone.GameAction.sacrifice(card);
	          else
	            AllZone.GameAction.discardRandom(card.getController());
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 4986114285467649619L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" - discards at random or sacrifices Minotaur Explorer");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Goretusk Firebeast"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          AllZone.GameAction.getPlayerLife(opponent).subtractLife(4);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 2977308349468915040L;

			public void execute()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          ability.setStackDescription("Goretusk Firebeast - deals 4 damage to " +opponent);
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Goblin Ringleader"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone libraryZone = AllZone.getZone(Constant.Zone.Library, card.getController());
	          PlayerZone hand        = AllZone.getZone(Constant.Zone.Hand   , card.getController());

	          //get top 4 cards of the library
	          CardList top = new CardList();
	          Card[] library = libraryZone.getCards();
	          for(int i = 0; i < 4 && i < library.length; i++)
	            top.add(library[i]);

	          //put top 4 cards on bottom of library
	          for(int i = 0; i < top.size(); i++)
	          {
	            libraryZone.remove(top.get(i));
	            libraryZone.add(top.get(i));
	          }

	          CardList goblin = top.getType("Goblin");

	          for(int i = 0; i < goblin.size(); i++)
	            AllZone.GameAction.moveTo(hand, goblin.get(i));
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -7538870520237796620L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Goblin Ringleader - reveal the top four cards of your library. Put all Goblin cards revealed this way into your hand and the rest on the bottom of your library.");
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Sylvan Messenger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone libraryZone = AllZone.getZone(Constant.Zone.Library, card.getController());
	          PlayerZone hand        = AllZone.getZone(Constant.Zone.Hand   , card.getController());

	          //get top 4 cards of the library
	          CardList top = new CardList();
	          Card[] library = libraryZone.getCards();
	          for(int i = 0; i < 4 && i < library.length; i++)
	            top.add(library[i]);

	          //put top 4 cards on bottom of library
	          for(int i = 0; i < top.size(); i++)
	          {
	            libraryZone.remove(top.get(i));
	            libraryZone.add(top.get(i));
	          }

	          CardList goblin = top.getType("Elf");

	          for(int i = 0; i < goblin.size(); i++)
	            AllZone.GameAction.moveTo(hand, goblin.get(i));
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 4757054648163014149L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Sylvan Messenger - reveal the top four cards of your library. Put all Elf cards revealed this way into your hand and the rest on the bottom of your library.");
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    

	    //*************** START *********** START **************************
	    else if(cardName.equals("Child of Alara"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Play.getCards());
	          list.addAll(AllZone.Computer_Play.getCards());
	          
	          
	          for(int i = 0; i < list.size(); i++)
	            if(!list.get(i).getType().contains("Land"))
	            {
	            	Card c = list.get(i);
	            	AllZone.GameAction.destroyNoRegeneration(c);
	            }
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -2937565366066183385L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Child of Alara - Destroy all non-land permanents, they can't be regenerated");
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Ryusei, the Falling Star"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Play.getCards());
	          list.addAll(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");
	          for(int i = 0; i < list.size(); i++)
	            if(! list.get(i).getKeyword().contains("Flying") && CardFactoryUtil.canDamage(card, list.get(i)))
	            	list.get(i).addDamage(5, card);
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -6585074939675844265L;

			public void execute()
	        {
	          AllZone.Stack.add(ability);
	        }
	      };
	      ability.setStackDescription("Ryusei, the Falling Star - deals 5 damage to each creature without flying");
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Sleeper Agent"))
	    {
	    	 final SpellAbility ability = new Ability(card, "0")
	         {
	    		 public void resolve(){
				    @SuppressWarnings("unused") // opponent
					String opponent = card.getController();
	    			((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
				    ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
				
				    PlayerZone from = AllZone.getZone(card);
				    from.remove(card);
				   
				    card.setController(AllZone.GameAction.getOpponent(card.getOwner()));
				
				    PlayerZone to = AllZone.getZone(Constant.Zone.Play, AllZone.GameAction.getOpponent(card.getOwner()));
				    to.add(card);
				    System.out.println("cards controller = " + card.getController());
				
				    ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
				    ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	    		 }
	         };
	         
	         ability.setStackDescription("When Sleeper Agent comes into play, target opponent gains control of it.");
	         Command intoPlay = new Command()
	         {
			   private static final long serialVersionUID = -3934471871041458847L;

			   public void execute()
	           {
	             AllZone.Stack.add(ability);

	           }//execute()
	         };
	         card.addComesIntoPlayCommand(intoPlay);

	    }//*************** END ************ END **************************

	    //*************** START *********** START **************************
	    else if(cardName.equals("Flametongue Kavu"))
	    {
	      final CommandReturn getCreature = new CommandReturn()
	      {
	        //get target card, may be null
	        public Object execute()
	        {
	          CardList list = CardFactoryUtil.AI_getHumanCreature(4, card, true);
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          if(list.size() != 0)
	          {
	            Card c = list.get(0);
	            if(3 <= c.getNetAttack() ||
	              (2 <= c.getNetAttack() && c.getKeyword().contains("Flying")))
	              return c;
	          }
	          if((AllZone.Computer_Life.getLife() < 10) &&
	             (CardFactoryUtil.AI_getHumanCreature(card, true).size() != 0))
	          {
	            list = CardFactoryUtil.AI_getHumanCreature(card, true);
	            CardListUtil.sortAttack(list);
	            CardListUtil.sortFlying(list);

	            return list.get(0);
	          }
	          return null;
	        }//execute()
	      };//CommandReturn

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canDamage(card, getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()) )
	            AllZone.GameAction.addDamage(getTargetCard(), card, 4);
	          }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -1920425335456952853L;

			public void execute()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	          {
	            AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	            ButtonUtil.disableAll();
	          }
	          else//computer
	          {
	            Object o = getCreature.execute();
	            if(o != null)//should never happen, but just in case
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	            else {
	              ability.setTargetCard(card);
	              AllZone.Stack.add(ability);
	            }
	          }//else
	        }//execute()
	      };
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 5741146386242415357L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();

	          return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Shriekmaw"))
	    {
	      final CommandReturn getCreature = new CommandReturn()
	      {
	        //get target card, may be null
	        public Object execute()
	        {
	          CardList nonblack = CardFactoryUtil.AI_getHumanCreature(card, true);
	          nonblack = nonblack.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return(!c.isArtifact() && !CardUtil.getColors(c).contains(Constant.Color.Black));
	            }
	          });

	          CardList list = new CardList(nonblack.toArray());
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          if(list.size() != 0)
	          {
	            Card c = list.get(0);
	            if(2 <= c.getNetAttack() && c.getKeyword().contains("Flying"))
	              return c;
	          }

	          if((AllZone.Computer_Life.getLife() < 10) && list.size() != 0)
	          {
	            CardListUtil.sortAttack(list);

	            if(MyRandom.percentTrue(50))
	              CardListUtil.sortFlying(list);

	            return list.get(0);
	          }
	          return null;
	        }//execute()
	      };//CommandReturn

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c) &&  CardFactoryUtil.canTarget(card,c) &&
	             !CardUtil.getColors(c).contains(Constant.Color.Black) &&
	             !c.isArtifact())
	          {
	            AllZone.GameAction.destroy(c);
	          }
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -70141932446179740L;

			public void execute()
	        {
	          Input target = new Input()
	          {
				private static final long serialVersionUID = 2634600975568025031L;
				
				public void showMessage()
	            {
	              AllZone.Display.showMessage("Select target nonartifact, nonblack creature to destroy");
	              ButtonUtil.disableAll();
	            }
	            public void selectCard(Card card, PlayerZone zone)
	            {
	              if(!CardFactoryUtil.canTarget(ability, card)){
	              	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
	              }
	              else if(card.isCreature() && zone.is(Constant.Zone.Play) &&
	                 !card.isArtifact() && !CardUtil.getColors(card).contains(Constant.Color.Black))
	              {
	                ability.setTargetCard(card);
	                AllZone.Stack.add(ability);
	                stop();
	              }
	            }
	          };//Input target


	          if(card.getController().equals(Constant.Player.Human))
	          {
	            //get all creatures
	            CardList list = new CardList();
	            list.addAll(AllZone.Human_Play.getCards());
	            list.addAll(AllZone.Computer_Play.getCards());
	            list = list.getType("Creature");

	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return(!c.isArtifact() && !CardUtil.getColors(c).contains(Constant.Color.Black));
	              }
	            });

	            if(list.size() != 0)
	              AllZone.InputControl.setInput(target);
//	              AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	          }
	          else//computer
	          {
	            Object o = getCreature.execute();
	            if(o != null)//should never happen, but just in case
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }//else
	        }//execute()
	      };
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -7508374608705757486L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();

	          return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });

	      card.addSpellAbility(new Spell_Evoke(card, "1 B")
	      {
			private static final long serialVersionUID = 6838065487515852527L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();

	          return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Briarhorn"))
	    {
	      final CommandReturn getCreature = new CommandReturn()
	      {
	        //get target card, may be null
	        public Object execute()
	        {
	          Combat combat = ComputerUtil.getAttackers();
	          Card[] c = combat.getAttackers();

	          if(c.length == 0)
	          {
	            CardList list = new CardList();
	            list.addAll(AllZone.Computer_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return(c.isCreature() && !c.hasSickness());
	              }
	            });

	            if(list.size() == 0)
	              return card;
	            else
	            {
	              CardListUtil.sortAttack(list);
	              CardListUtil.sortFlying(list);

	              for(int i = 0; i < list.size(); i++)
	                if(list.get(i).isUntapped())
	                  return list.get(i);

	              return list.get(0);
	            }

	          }

	          return c[0];
	        }//execute()
	      };//CommandReturn

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          final Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card,c))
	          {
	            c.addTempAttackBoost(3);
	            c.addTempDefenseBoost(3);

	            AllZone.EndOfTurn.addUntil(new Command()
	            {
				  private static final long serialVersionUID = -5417966443737481535L;

				  public void execute()
	              {
	                c.addTempAttackBoost(-3);
	                c.addTempDefenseBoost(-3);
	              }
	            });
	          }//if
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -5497111036332352337L;

			public void execute()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	          {
	              AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	          }
	          else//computer
	          {
	            Object o = getCreature.execute();
	            if(o != null)//should never happen, but just in case
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }//else
	        }//execute()
	      };
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -681505091538444209L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();

	          return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });

	      card.addSpellAbility(new Spell_Evoke(card, "1 G")
	      {
			private static final long serialVersionUID = 8565746177492779899L;
			
			public boolean canPlayAI()
	        {
	          return false;
	        }
	        //because this card has Flash
	        public boolean canPlay()
	        {
	        	return AllZone.GameAction.isCardInZone(card, AllZone.Human_Hand) ||
	   					AllZone.GameAction.isCardInZone(card, AllZone.Computer_Hand);
	        }
	      });
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Inner-Flame Acolyte"))
	    {
	      final CommandReturn getCreature = new CommandReturn()
	      {
	        //get target card, may be null
	        public Object execute()
	        {
	          Combat combat = ComputerUtil.getAttackers();
	          Card[] c = combat.getAttackers();
	          CardList list = new CardList();

	          if(c.length == 0)
	          {
	            list.addAll(AllZone.Computer_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isCreature();
	              }
	            });

	            if(list.size() == 0)
	              return card;
	            else
	            {
	              CardListUtil.sortAttack(list);
	              CardListUtil.sortFlying(list);

	              for(int i = 0; i < list.size(); i++)
	                if(list.get(i).isUntapped())
	                  return list.get(i);

	              return list.get(0);
	            }
	          }

	          return c[0];
	        }//execute()
	      };//CommandReturn

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          final Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card,c) )
	          {
	            c.addTempAttackBoost(2);
	            c.addExtrinsicKeyword("Haste");

	            AllZone.EndOfTurn.addUntil(new Command()
	            {
				  private static final long serialVersionUID = -6478147896119509688L;

				  public void execute()
	              {
	                c.addTempAttackBoost(-2);
	                c.removeExtrinsicKeyword("Haste");
	              }
	            });
	          }//if
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -4514610171270596654L;

			public void execute()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	          {
	              AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	          }
	          else//computer
	          {
	            Object o = getCreature.execute();
	            if(o != null)//should never happen, but just in case
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }//else
	        }//execute()
	      };
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 7153795935713327863L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();

	          return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });

	      card.addSpellAbility(new Spell_Evoke(card, "R")
	      {
			private static final long serialVersionUID = 8173305091293824506L;

			public boolean canPlayAI()
	        {
	          return false;
	        }
	      });
	    }//*************** END ************ END **************************





	    //*************** START *********** START **************************
	    else if(cardName.equals("Slaughterhouse Bouncer"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canDamage(card, getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()) )
	            getTargetCard().addDamage(3, card);
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = 1619442728548153928L;

			public void execute()
	        {
	          //check to see if any other creatures in play
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Play.getCards());
	          list.addAll(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          //check to see if any cards in hand
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          if(hand.getCards().length == 0 && list.size() != 0)
	          {
	            if(card.getController().equals(Constant.Player.Human))
	            {
	              AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	              ButtonUtil.disableAll();
	            }
	            else//computer
	            {
	              //1.try to get human creature with defense of 3
	              list = CardFactoryUtil.AI_getHumanCreature(card, true);
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c) {return c.getNetDefense() == 3;}
	              });
	              //2.try to get human creature with defense of 2 or less
	              if(list.isEmpty())
	                list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
	              //3.get any computer creature
	              if(list.isEmpty())
	              {
	                list = new CardList(AllZone.Computer_Play.getCards());
	                list = list.getType("Creature");
	              }
	              list.shuffle();
	              ability.setTargetCard(list.get(0));
	              AllZone.Stack.add(ability);
	            }
	          }//if ok to play
	        }//execute()
	      };//Command
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Undying Beast"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          card.setDamage(0);
	          card.clearAssignedDamage();
	          card.untap();

	          //moves card to top of library
	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getOwner());
	          library.add(card, 0);
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -318081458847722674L;

			public void execute()
	        {
	          if(card.isToken())
	            return;

	          //remove from graveyard
	          PlayerZone grave = AllZone.getZone(card);
	          grave.remove(card);

	          ability.setStackDescription("Put Undying Beast on top of its owner's library.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Fire Imp") || cardName.equals("Corrupt Eunuchs"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canDamage(card, getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()) )
	            getTargetCard().addDamage(2, card);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -7639628386947162984L;

			public void execute()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	          {
	            AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	            ButtonUtil.disableAll();
	          }
	          else//computer
	          {
	            CardList list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
	            CardListUtil.sortAttack(list);
	            CardListUtil.sortFlying(list);

	            if(list.isEmpty())
	            {
	              list = CardFactoryUtil.AI_getHumanCreature(card, true);
	              list.shuffle();
	            }
	            
	            if (list.size() > 0)
	            	ability.setTargetCard(list.get(0));
	            else 
	            	ability.setTargetCard(card);
	            
	            AllZone.Stack.add(ability);
	          }//else
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 1731831041621831246L;

			public boolean canPlayAI()
	        {
	          CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);

	          return (list.size() > 0) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });

	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Man-o'-War") || cardName.equals("Sun Ce, Young Conquerer") )
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = getTargetCard();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
	          
	          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card,getTargetCard()) )
	          {
	            AllZone.getZone(c).remove(c);

	            if(! c.isToken())
	            {
	              Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
	              hand.add(newCard);
	            }
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -7628289586347295144L;

			public void execute()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	          {
	            AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	            ButtonUtil.disableAll();
	          }
	          else//computer
	          {
	            Card human = CardFactoryUtil.AI_getBestCreature(CardFactoryUtil.AI_getHumanCreature(card, true));
	            ability.setTargetCard(human);
	            
	            
	            if (human == null)
	            	ability.setTargetCard(card);
	                        
	            AllZone.Stack.add(ability);
	          }//else
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 2458286917800051358L;

			public boolean canPlayAI()
	        {
	          CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);

	          return (list.size() > 0) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Keening Banshee"))
	    {
	      
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        
	        public void resolve()
	        {
	          final Card c = getTargetCard();
	          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card,c) )
	          {
	        	c.addTempAttackBoost(-2);
	          	c.addTempDefenseBoost(-2);

	          	AllZone.EndOfTurn.addUntil(new Command()
	          	{
					private static final long serialVersionUID = 8479364459667467780L;

					public void execute()
	          		{
	          			c.addTempAttackBoost(2);
	          			c.addTempDefenseBoost(2);
	          		}
	          	});
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 6283666887577455663L;

			public void execute()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	          {
	            AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	            ButtonUtil.disableAll();
	          }
	          else//computer
	          {
	            CardList list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
	            CardListUtil.sortAttack(list);
	            CardListUtil.sortFlying(list);

	            if(list.isEmpty())
	            {
	              list = CardFactoryUtil.AI_getHumanCreature(card, true);
	              list.shuffle();
	            }

	            ability.setTargetCard(list.get(0));
	            AllZone.Stack.add(ability);
	          }//else
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -1893090545602255371L;

			public boolean canPlayAI()
	        {
	          CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);

	          return (list.size() > 0) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });

	    }//*************** END ************ END **************************

	    //*************** START *********** START **************************
	    else if(cardName.equals("Ant Queen"))
	    {
	    	final SpellAbility ability = new Ability(card, "1 G")
	        {
	          public void resolve()
	          {
	            Card c = new Card();
	            c.setName("Insect");
	            c.setImageName("G 1 1 Insect");

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setManaCost("G");
	            c.setToken(true);

	            c.addType("Creature");
	            c.addType("Insect");

	            c.setBaseAttack(1);
	            c.setBaseDefense(1);

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            play.add(c);
	          }//resolve()
	        };
	        ability.setDescription("1 G: Put a 1/1 green Insect creature token onto the battlefield.");
	        ability.setStackDescription(card.getName() +  " - Put a 1/1 green Insect token onto the battlefield.");
	        card.addSpellAbility(ability);
	    	
	    	
	    }//*************** END ************ END **************************
	    


	    //*************** START *********** START **************************
	    else if(cardName.equals("Eternal Witness"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), grave))
	          {
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	            AllZone.GameAction.moveTo(hand, getTargetCard());
	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 1658050744890095441L;

			public void execute()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());

	          if(grave.getCards().length == 0)
	            return;

	          if(card.getController().equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", grave.getCards());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else//computer
	          {
	            CardList list = new CardList(grave.getCards());
	            Card best = CardFactoryUtil.AI_getBestCreature(list);

	            if(best == null)
	            {
	              list.shuffle();
	              best = list.get(0);
	            }
	            ability.setTargetCard(best);
	            AllZone.Stack.add(ability);
	          }
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************

	  //*************** START *********** START **************************
	    else if(cardName.equals("Karmic Guide"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), grave))
	          {
	            PlayerZone play = AllZone.getZone(Constant.Zone.Hand, card.getController());
	            grave.remove(getTargetCard());
	            play.add(getTargetCard());
	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 2128307240208621147L;

			public void execute()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          CardList list = new CardList(grave.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	            return;

	          if(card.getController().equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", list.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }//if
	          else//computer
	          {
	            Card best = CardFactoryUtil.AI_getBestCreature(list);
	            ability.setTargetCard(best);
	            AllZone.Stack.add(ability);
	          }
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	      
	      card.clearSpellAbility();

	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 4446838001015234917L;

			public boolean canPlayAI()
	        {
	        	CardList creats = new CardList();
	        	creats.addAll(AllZone.Computer_Graveyard.getCards());
	        	creats = creats.filter(new CardListFilter()
	        	{
	        		public boolean addCard(Card c)
	        		{
	        			return c.isCreature() && c.getNetAttack() > 2;
	        		}
	        	});
	        	
	        	if (creats.size() > 0)
	        		return true;
	        	else 
	        		return false;    	
	        }
	      });
	    }//*************** END ************ END **************************
	    
	    

	    //*************** START *********** START **************************
	    else if(cardName.equals("Gravedigger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), grave))
	          {
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	            AllZone.GameAction.moveTo(hand, getTargetCard());
	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -7433708170033536384L;

			public void execute()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          CardList list = new CardList(grave.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	            return;

	          if(card.getController().equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", list.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }//if
	          else//computer
	          {
	            Card best = CardFactoryUtil.AI_getBestCreature(list);
	            ability.setTargetCard(best);
	            AllZone.Stack.add(ability);
	          }
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Strongarm Thug"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), grave))
	          {
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	            AllZone.GameAction.moveTo(hand, getTargetCard());
	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 4519970074391756730L;

			public void execute()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          CardList list = new CardList(grave.getCards());
	          list = list.getType("Mercenary");

	          if(list.isEmpty())
	            return;

	          if(card.getController().equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", list.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }//if
	          else//computer
	          {
	            Card best = CardFactoryUtil.AI_getBestCreature(list);
	            ability.setTargetCard(best);
	            AllZone.Stack.add(ability);
	          }
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************

	    //*************** START *********** START ************************
	    if (cardName.equals("Adun Oakenshield"))
	    {
	    	final Ability_Tap ability = new Ability_Tap(card, "B R G")
	        {
				private static final long serialVersionUID = -7913968639880781838L;
				public boolean canPlayAI() {return getGraveCreatures().size() != 0;}
		
		          public void chooseTargetAI()
		          {
		            CardList grave = getGraveCreatures();
		            Card target = CardFactoryUtil.AI_getBestCreature(grave);
		            setTargetCard(target);
		          }
		
		          public void resolve()
		          {
		            if(card.getController().equals(Constant.Player.Human))
		            {
		              Card c = (Card) AllZone.Display.getChoice("Select card", getGraveCreatures().toArray());
		              setTargetCard(c);
		            }
		
		            PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
		            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
		
		            if(AllZone.GameAction.isCardInZone(getTargetCard(), grave))
		               AllZone.GameAction.moveTo(hand, getTargetCard());
		          }//resolve()
		          public boolean canPlay()
		          {
		            return super.canPlay() && getGraveCreatures().size() != 0;
		          }
		          CardList getGraveCreatures()
		          {
		            PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
		            CardList list = new CardList(grave.getCards());
		            list = list.getType("Creature");
		            return list;
		          }
	        };//SpellAbility
	        ability.setDescription("B R G, Tap: Return target creature card from your graveyard to your hand.");
	        ability.setStackDescription(cardName + " - return target creature from your graveyard to your hand.");
	        
	        card.addSpellAbility(ability);
	    
	    
	  	}//*************** END ************ END **************************
	    
	    

	    //*************** START *********** START **************************
	    else if(cardName.equals("Anarchist"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), grave))
	          {
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	            AllZone.GameAction.moveTo(hand, getTargetCard());
	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -7459412502903144952L;

			public void execute()
	        {
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	          CardList sorcery = new CardList(grave.getCards());
	          sorcery = sorcery.getType("Sorcery");

	          String controller = card.getController();

	          if(sorcery.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", sorcery.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            sorcery.shuffle();
	            ability.setTargetCard(sorcery.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Penumbra Kavu"))
	    {
	      final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());
	          
	          c.setName("Kavu");
	          c.setImageName("B 3 3 Kavu");
	          c.setManaCost("B");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Kavu");
	          c.setBaseAttack(3);
	          c.setBaseDefense(3);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//resolve()
	      };//Ability

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = 1281791927604583468L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" puts a 3/3 creature into play from Penumbra Kavu");
	          AllZone.Stack.add(ability);
	        }
	      };

	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Penumbra Bobcat"))
	    {
	      final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Cat");
	          c.setImageName("B 2 1 Cat");
	          c.setManaCost("B");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Cat");
	          c.setBaseAttack(2);
	          c.setBaseDefense(1);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//resolve()
	      };//Ability

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -8057009255325020247L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" puts a 2/1 creature into play from Penumbra Bobcat");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Penumbra Spider"))
	    {
	      final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Spider");
	          c.setImageName("B 2 4 Spider");
	          c.setManaCost("B");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Spider");
	          c.setBaseAttack(2);
	          c.setBaseDefense(4);
	          c.addIntrinsicKeyword("Reach");

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//resolve()
	      };//Ability

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = 9186718803540678064L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" puts a 2/4 Black Spider creature into play from Penumbra Spider");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Penumbra Wurm"))
	    {
	      final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Wurm");
	          c.setImageName("B 6 6 Wurm");
	          c.setManaCost("B");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Wurm");
	          c.setBaseAttack(6);
	          c.setBaseDefense(6);
	          c.addIntrinsicKeyword("Trample");

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//resolve()
	      };//Ability

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -8819664543962631239L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" puts a 6/6 Black Wurm creature with trample into play from Penumbra Wurm");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Aven Fisher") || cardName.equals("Riptide Crab"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          AllZone.GameAction.drawCard(card.getController());
	        }
	      };
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -2786138225183288814L;

			public void execute()
	        {
	          ability.setStackDescription(card.getName() + " - " + card.getController() +" draws a card");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Peregrine Drake"))
	    {
	      final Input untap = new Input()
	      {
			private static final long serialVersionUID = 2287264826189281795L;
			
			int stop = 5;
	        int count = 0;

	        public void showMessage()
	        {
	          AllZone.Display.showMessage("Select a land to untap");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectCard(Card card, PlayerZone zone)
	        {
	          if(card.isLand() && zone.is(Constant.Zone.Play))
	          {
	            card.untap();
	            count++;
	            if(count == stop)
	              stop();
	          }
	        }//selectCard()
	      };

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(card.getController().equals("Human"))
	            AllZone.InputControl.setInput(untap);
	          else
	          {
	            CardList list = new CardList(AllZone.Computer_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isLand() && c.isTapped();
	              }
	            });
	            for(int i = 0; i < 5 && i < list.size(); i++)
	              list.get(i).untap();
	          }//else
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 3208277692165539396L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" untaps up to 5 lands.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  
	    //*************** START *********** START **************************
	    else if(cardName.equals("Cloud of Faeries"))
	    {
	      final Input untap = new Input()
	      {
			private static final long serialVersionUID = -2167059918040912025L;
			
			int stop = 2;
	        int count = 0;

	        public void showMessage()
	        {
	          AllZone.Display.showMessage("Select a land to untap");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectCard(Card card, PlayerZone zone)
	        {
	          if(card.isLand() && zone.is(Constant.Zone.Play))
	          {
	            card.untap();
	            count++;
	            if(count == stop)
	              stop();
	          }
	        }//selectCard()
	      };

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(card.getController().equals("Human"))
	            AllZone.InputControl.setInput(untap);
	          else
	          {
	            CardList list = new CardList(AllZone.Computer_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isLand() && c.isTapped();
	              }
	            });
	            for(int i = 0; i < 2 && i < list.size(); i++)
	            {
	              list.get(i).untap();
	            }
	          }//else
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 7222997838166323277L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" untaps up to 2 lands.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);

	      //add cycling
	      card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "2"));
	    }//*************** END ************ END ***************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Vodalian Merchant"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          AllZone.GameAction.drawCard(card.getController());

	          if(card.getController().equals("Human"))
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	          else
	            AllZone.GameAction.discardRandom("Computer");
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -8924243774757009091L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" draws a card, then discards a card");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Whirlpool Rider"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          //shuffle hand into library, then shuffle library
	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	          PlayerZone hand    = AllZone.getZone(Constant.Zone.Hand   , card.getController());
	          Card c[] = hand.getCards();
	          for(int i = 0; i < c.length; i++)
	              AllZone.GameAction.moveTo(library, c[i]);
	          AllZone.GameAction.shuffle(card.getController());

	          //draw same number of cards as before
	          for(int i = 0; i < c.length; i++)
	            AllZone.GameAction.drawCard(card.getController());
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 6290392806910817877L;

			public void execute()
	        {
	          ability.setStackDescription(card.getController() +" shuffles the cards from his hand into his library, then draws that many cards.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Sky Swallower"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          String opp = AllZone.GameAction.getOpponent(card.getController());
	          PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play, opp);
	          PlayerZone myPlay = AllZone.getZone(Constant.Zone.Play, card.getController());

	          CardList list = new CardList(myPlay.getCards());
	          //list.remove(card);//doesn't move Sky Swallower
	          
	          list = list.filter(new CardListFilter()
	          {
				public boolean addCard(Card c) {
					return !c.equals(card) && !c.getName().equals("Mana Pool");
				}
	          });
	          
	          while(! list.isEmpty())
	          {
	        	((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
		        ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
	            //so "comes into play" abilities don't trigger
	            ///list.get(0).addComesIntoPlayCommand(Command.Blank);

	            oppPlay.add(list.get(0));
	            myPlay.remove(list.get(0));

	            list.get(0).setController(opp);
	            list.remove(0);
	            
	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
		        ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	          }
	        }//resolve()
	      };

	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -453410206437839334L;

			public void execute()
	        {
	          ability.setStackDescription(AllZone.GameAction.getOpponent(card.getController()) +" gains control of all other permanents you control");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Nova Chaser") || cardName.equals("Supreme Exemplar"))
	    {
	      final CommandReturn getCreature = new CommandReturn()
	      {
	        public Object execute()
	        {
	          //get all creatures
	          CardList list = new CardList();
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          list.addAll(play.getCards());
	          
	          list = list.filter(new CardListFilter(){
				public boolean addCard(Card c) {
					return c.getType().contains("Elemental") || c.getKeyword().contains("Changeling");
				}
	          });

	          return list;
	        }
	      };//CommandReturn

	      final SpellAbility abilityComes = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if (getTargetCard() == null || getTargetCard() == card)
	        	  AllZone.GameAction.sacrifice(card);
	          
	          else if(AllZone.GameAction.isCardInPlay(getTargetCard()))
	          {
	        	/*
	            PlayerZone play = AllZone.getZone(getTargetCard());
	            PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
	            play.remove(getTargetCard());
	            removed.add(getTargetCard());
	            */
	        	AllZone.GameAction.removeFromGame(getTargetCard());  
	          }
	        }//resolve()
	      };

	      final Input inputComes = new Input()
	      {
			private static final long serialVersionUID = -6066115143834426784L;

			public void showMessage()
	        {
	          CardList choice = (CardList)getCreature.execute();

	          stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice, "Select Elemental to remove from the game", false));
	          ButtonUtil.disableAll();
	        }

	      };
	      Command commandComes = new Command()
	      {
			private static final long serialVersionUID = -3498068247359658023L;

			public void execute()
	        {
	          CardList creature = (CardList)getCreature.execute();
	          String s = card.getController();
	          if(creature.size() == 0) {
	        	  AllZone.GameAction.sacrifice(card);
	        	  return;
	          }
	          else if(s.equals(Constant.Player.Human))
	            AllZone.InputControl.setInput(inputComes);
	          else //computer
	          {
	            Card target;
	            //must target computer creature
	            CardList computer = new CardList(AllZone.Computer_Play.getCards());
	            computer = computer.getType("Elemental");
	            computer.remove(card);

	            computer.shuffle();
	            if (computer.size()!= 0) {
	            	  target = computer.get(0);
	            	  abilityComes.setTargetCard(target);
	            	  AllZone.Stack.add(abilityComes);
	            }
	          }//else
	        }//execute()
	      };//CommandComes
	      Command commandLeavesPlay = new Command()
	      {
			private static final long serialVersionUID = 4236503599017025393L;

			public void execute()
	        {
	          //System.out.println(abilityComes.getTargetCard().getName());
	          Object o = abilityComes.getTargetCard();
	          
	          if(o == null || ((Card)o).isToken()|| !AllZone.GameAction.isCardRemovedFromGame((Card)o) )
	            return;

	          SpellAbility ability = new Ability(card, "0")
	          {
	            public void resolve()
	            {
	              //copy card to reset card attributes like attack and defense
	              Card c = abilityComes.getTargetCard();
	              if(! c.isToken())
	              {
	                c = AllZone.CardFactory.copyCard(c);
	                c.setController(c.getOwner());

	                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
	                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
	                removed.remove(c);
	                play.add(c);
	                
	              }
	            }//resolve()
	          };//SpellAbility
	          ability.setStackDescription(card.getName() + " - returning creature to play");
	          AllZone.Stack.add(ability);
	        }//execute()
	      };//Command

	      card.addComesIntoPlayCommand(commandComes);
	      card.addLeavesPlayCommand(commandLeavesPlay);
	      
	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -62128538015338896L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();
	          if (o == null)
	        	  return false;
	          
	          CardList cl = (CardList)getCreature.execute();
	          return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Wren's Run Packmaster"))
	    {
	      final CommandReturn getCreature = new CommandReturn()
	      {
	        public Object execute()
	        {
	          //get all creatures
	          CardList list = new CardList();
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          list.addAll(play.getCards());
	          
	          list = list.filter(new CardListFilter(){
				public boolean addCard(Card c) {
					return c.getType().contains("Elf") || c.getKeyword().contains("Changeling");
				}
	          });

	          return list;
	        }
	      };//CommandReturn

	      final SpellAbility abilityComes = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if (getTargetCard() == null || getTargetCard() == card)
	        	  AllZone.GameAction.sacrifice(card);
	          
	          else if(AllZone.GameAction.isCardInPlay(getTargetCard()))
	          {
	        	/*
	            PlayerZone play = AllZone.getZone(getTargetCard());
	            PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
	            play.remove(getTargetCard());
	            removed.add(getTargetCard());
	            */
	        	AllZone.GameAction.removeFromGame(getTargetCard());  
	          }
	        }//resolve()
	      };

	      final Input inputComes = new Input()
	      {
			private static final long serialVersionUID = 5210700665533271691L;

			public void showMessage()
	        {
	          CardList choice = (CardList)getCreature.execute();

	          stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice, "Select Elf to remove from the game", false));
	          ButtonUtil.disableAll(); //target this card means: sacrifice this card
	        }
	      };
	      Command commandComes = new Command()
	      {

			private static final long serialVersionUID = -3580408066322945328L;

			public void execute()
	        {
	          CardList creature = (CardList)getCreature.execute();
	          String s = card.getController();
	          if(creature.size() == 0) {
	        	  AllZone.GameAction.sacrifice(card);
	        	  return;
	          }
	          else if(s.equals(Constant.Player.Human))
	            AllZone.InputControl.setInput(inputComes);
	          else //computer
	          {
	            Card target;
	            //must target computer creature
	            CardList computer = new CardList(AllZone.Computer_Play.getCards());
	            computer = computer.getType("Elf");
	            computer.remove(card);

	            computer.shuffle();
	            if (computer.size()!= 0) {
	            	  target = computer.get(0);
	            	  abilityComes.setTargetCard(target);
	            	  AllZone.Stack.add(abilityComes);
	            }
	          }//else
	        }//execute()
	      };//CommandComes
	      Command commandLeavesPlay = new Command()
	      {

			private static final long serialVersionUID = -5903638227914705191L;

			public void execute()
	        {
	          //System.out.println(abilityComes.getTargetCard().getName());
	          Object o = abilityComes.getTargetCard();
	          
	          if(o == null || ((Card)o).isToken()|| !AllZone.GameAction.isCardRemovedFromGame((Card)o) )
	            return;

	          SpellAbility ability = new Ability(card, "0")
	          {
	            public void resolve()
	            {
	              //copy card to reset card attributes like attack and defense
	              Card c = abilityComes.getTargetCard();
	              if(! c.isToken())
	              {
	                c = AllZone.CardFactory.copyCard(c);
	                c.setController(c.getOwner());

	                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
	                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
	                removed.remove(c);
	                play.add(c);
	                
	              }
	            }//resolve()
	          };//SpellAbility
	          ability.setStackDescription(card.getName() + " - returning creature to play");
	          AllZone.Stack.add(ability);
	        }//execute()
	      };//Command
	      
	      
	      final SpellAbility a1 = new Ability(card, "2 G")
	      {
	    	public boolean canPlayAI()
	    	{
	    		return MyRandom.random.nextBoolean();
	    	}
	    	public boolean canPlay()
	    	{
	    		SpellAbility sa;
	            //this is a hack, check the stack to see if this card has an ability on the stack
	            //if so, we can't use the ability
	            for (int i=0; i<AllZone.Stack.size(); i++)
	            {
	            	sa = AllZone.Stack.peek(i);
	            	if (sa.getSourceCard().equals(card))
	            			return false;
	            }
	            return AllZone.GameAction.isCardInPlay(card);
	    		
	    	}
	        public void resolve()
	        {
	          makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();

	          c.setName("Wolf");
	          c.setImageName("G 2 2 Wolf");

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setManaCost("G");
	          c.setToken(true);
	         
	          c.addType("Creature");
	          c.addType("Wolf");
	          c.setBaseAttack(2);
	          c.setBaseDefense(2);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      
	      a1.setDescription("2 G: Put a 2/2 green Wolf creature token into play.");
	      a1.setStackDescription("Put a 2/2 Wolf into play.");
	      
	      card.clearSpellAbility();
	      
	      card.addComesIntoPlayCommand(commandComes);
	      card.addLeavesPlayCommand(commandLeavesPlay);
	      
	      

	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 2583297503017070549L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();
	          if (o == null)
	        	  return false;
	          
	          CardList cl = (CardList)getCreature.execute();
	          return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	      
	      card.addSpellAbility(a1);
	      a1.setBeforePayMana(new Input_PayManaCost(a1));
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Changeling Berserker") || cardName.equals("Changeling Hero") || cardName.equals("Changeling Titan"))
	    {
	      final CommandReturn getCreature = new CommandReturn()
	      {
	        public Object execute()
	        {
	          //get all creatures
	          CardList list = new CardList();
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          list.addAll(play.getCards());
	          
	          list = list.filter(new CardListFilter(){
				public boolean addCard(Card c) {
					return c.getType().contains("Creature");
				}
	          });

	          return list;
	        }
	      };//CommandReturn

	      final SpellAbility abilityComes = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if (getTargetCard() == null || getTargetCard() == card)
	        	  AllZone.GameAction.sacrifice(card);
	          
	          else if(AllZone.GameAction.isCardInPlay(getTargetCard()) )
	          {
	        	/*
	            PlayerZone play = AllZone.getZone(getTargetCard());
	            PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
	            play.remove(getTargetCard());
	            removed.add(getTargetCard());
	            */
	        	AllZone.GameAction.removeFromGame(getTargetCard());  
	          }
	        }//resolve()
	      };

	      final Input inputComes = new Input()
	      {

			private static final long serialVersionUID = 5210700665533271691L;

			public void showMessage()
	        {
	          CardList choice = (CardList)getCreature.execute();

	          stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice, "Select creature to remove from the game", false));
	          ButtonUtil.disableAll(); 
	        }
	      };
	      Command commandComes = new Command()
	      {

			private static final long serialVersionUID = -3580408066322945328L;

			public void execute()
	        {
	          CardList creature = (CardList)getCreature.execute();
	          String s = card.getController();
	          if(creature.size() == 0) {
	        	  AllZone.GameAction.sacrifice(card);
	        	  return;
	          }
	          else if(s.equals(Constant.Player.Human))
	            AllZone.InputControl.setInput(inputComes);
	          else //computer
	          {
	            Card target;
	            //must target computer creature
	            CardList computer = new CardList(AllZone.Computer_Play.getCards());
	            computer = computer.getType("Creature");
	            computer.remove(card);

	            computer.shuffle();
	            if (computer.size()!= 0) {
	            	  target = computer.get(0);
	            	  abilityComes.setTargetCard(target);
	            	  AllZone.Stack.add(abilityComes);
	            }
	          }//else
	        }//execute()
	      };//CommandComes
	      Command commandLeavesPlay = new Command()
	      {

			private static final long serialVersionUID = -5903638227914705191L;

			public void execute()
	        {
	          //System.out.println(abilityComes.getTargetCard().getName());
	          Object o = abilityComes.getTargetCard();
	          
	          if(o == null || ((Card)o).isToken()|| !AllZone.GameAction.isCardRemovedFromGame((Card)o) )
	            return;

	          SpellAbility ability = new Ability(card, "0")
	          {
	            public void resolve()
	            {
	              //copy card to reset card attributes like attack and defense
	              Card c = abilityComes.getTargetCard();
	              if(! c.isToken())
	              {
	                c = AllZone.CardFactory.copyCard(c);
	                c.setController(c.getOwner());

	                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
	                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
	                removed.remove(c);
	                play.add(c);
	                
	              }
	            }//resolve()
	          };//SpellAbility
	          ability.setStackDescription(card.getName() + " - returning creature to play");
	          AllZone.Stack.add(ability);
	        }//execute()
	      };//Command

	      card.addComesIntoPlayCommand(commandComes);
	      card.addLeavesPlayCommand(commandLeavesPlay);
	      
	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {

			private static final long serialVersionUID = 2583297503017070549L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();
	          if (o == null)
	        	  return false;
	          
	          CardList cl = (CardList)getCreature.execute();
	          return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Devout Lightcaster"))
	    {
	      final CommandReturn getBlackPerm = new CommandReturn()
	      {
	        public Object execute()
	        {
	          //get all creatures
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Play.getCards());
	          list.addAll(AllZone.Computer_Play.getCards());
	          list = list.filter(new CardListFilter(){
				public boolean addCard(Card c) {
					return c.isPermanent() && CardFactoryUtil.canTarget(card,c) && CardUtil.getColor(c).equals(Constant.Color.Black);
				}
	          });

	          return list;
	        }
	      };//CommandReturn

	      final SpellAbility abilityComes = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()) )
	          {

	        	AllZone.GameAction.removeFromGame(getTargetCard());  
	          }
	        }//resolve()
	      };

	      final Input inputComes = new Input()
	      {
			private static final long serialVersionUID = 3762167908738465100L;

			public void showMessage()
	        {
	          CardList choice = (CardList)getBlackPerm.execute();

	          stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice, "Select target black permanent to remove from the game", true));
	          ButtonUtil.disableAll();//to disable the Cancel button
	        }
	      };
	      Command commandComes = new Command()
	      {
		
			private static final long serialVersionUID = 3878683618127503416L;

			public void execute()
	        {
	          CardList creature = (CardList)getBlackPerm.execute();
	          String s = card.getController();
	          if(creature.size() == 0)
	            return;
	          else if(s.equals(Constant.Player.Human))
	            AllZone.InputControl.setInput(inputComes);
	          else //computer
	          {
	            Card target;

	            //try to target human permanent
	            PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	            CardList human = new CardList(hum.getCards());
	            human = human.filter(new CardListFilter()
	            {
					public boolean addCard(Card c) {
						return c.isPermanent() && CardUtil.getColor(c).equals(Constant.Color.Black);
					}
	            	
	            });
	            //target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty
	            human.shuffle();
	            target = null;
	            if (human.size()!=0)
	            	target = human.get(0);

	            if(target == null)
	            {
	              //must target computer creature
	              CardList computer = new CardList(AllZone.Computer_Play.getCards());
	              computer = computer.filter(new CardListFilter()
	              {
					public boolean addCard(Card c) {
						return c.isPermanent() && CardUtil.getColor(c).equals(Constant.Color.Black);
					}
	            	  
	              });

	              computer.shuffle();
	              if (computer.size()!= 0)
	            	  target = computer.get(0);
	            }
	            abilityComes.setTargetCard(target);
	            AllZone.Stack.add(abilityComes);
	          }//else
	        }//execute()
	      };//CommandComes
	      
	      card.addComesIntoPlayCommand(commandComes);
	      
	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {

			private static final long serialVersionUID = -9059177006257139430L;

			public boolean canPlayAI()
	        {
	          Object o = getBlackPerm.execute();
	          if (o == null)
	        	  return false;
	          
	          CardList cl = (CardList)getBlackPerm.execute();
	          return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************
	    

	    //*************** START *********** START **************************
	    else if(cardName.equals("Faceless Butcher"))
	    {
	      final CommandReturn getCreature = new CommandReturn()
	      {
	        public Object execute()
	        {
	          //get all creatures
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Play.getCards());
	          list.addAll(AllZone.Computer_Play.getCards());
	          list = list.filter(new CardListFilter(){
				public boolean addCard(Card c) {
					return c.isCreature() && CardFactoryUtil.canTarget(card,c);
				}
	          });
	          
	          //remove "this card"
	          list.remove(card);

	          return list;
	        }
	      };//CommandReturn

	      final SpellAbility abilityComes = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()) )
	          {
	        	/*
	            PlayerZone play = AllZone.getZone(getTargetCard());
	            PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
	            play.remove(getTargetCard());
	            removed.add(getTargetCard());
	            */
	        	AllZone.GameAction.removeFromGame(getTargetCard());  
	          }
	        }//resolve()
	      };

	      final Input inputComes = new Input()
	      {
			private static final long serialVersionUID = -1932054059769056049L;

			public void showMessage()
	        {
	          CardList choice = (CardList)getCreature.execute();

	          stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice, "Select target creature to remove from the game", true));
	          ButtonUtil.disableAll();//to disable the Cancel button
	        }
	      };
	      Command commandComes = new Command()
	      {
			private static final long serialVersionUID = -5675532512302863456L;

			public void execute()
	        {
	          CardList creature = (CardList)getCreature.execute();
	          String s = card.getController();
	          if(creature.size() == 0)
	            return;
	          else if(s.equals(Constant.Player.Human))
	            AllZone.InputControl.setInput(inputComes);
	          else //computer
	          {
	            Card target;

	            //try to target human creature
	            CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
	            target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty

	            if(target == null)
	            {
	              //must target computer creature
	              CardList computer = new CardList(AllZone.Computer_Play.getCards());
	              computer = computer.getType("Creature");
	              computer.remove(card);

	              computer.shuffle();
	              if (computer.size()!= 0)
	            	  target = computer.get(0);
	            }
	            abilityComes.setTargetCard(target);
	            AllZone.Stack.add(abilityComes);
	          }//else
	        }//execute()
	      };//CommandComes
	      Command commandLeavesPlay = new Command()
	      {
			private static final long serialVersionUID = 5518706316791622193L;

			public void execute()
	        {
	          //System.out.println(abilityComes.getTargetCard().getName());
	          Object o = abilityComes.getTargetCard();
	          
	          if(o == null || ((Card)o).isToken()|| !AllZone.GameAction.isCardRemovedFromGame((Card)o) )
	            return;

	          SpellAbility ability = new Ability(card, "0")
	          {
	            public void resolve()
	            {
	              //copy card to reset card attributes like attack and defense
	              Card c = abilityComes.getTargetCard();
	              if(! c.isToken())
	              {
	                c = AllZone.CardFactory.copyCard(c);
	                c.setController(c.getOwner());

	                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
	                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
	                removed.remove(c);
	                play.add(c);
	                
	              }
	            }//resolve()
	          };//SpellAbility
	          ability.setStackDescription("Faceless Butcher - returning creature to play");
	          AllZone.Stack.add(ability);
	        }//execute()
	      };//Command

	      card.addComesIntoPlayCommand(commandComes);
	      card.addLeavesPlayCommand(commandLeavesPlay);
	      
	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {

			private static final long serialVersionUID = -62128538015338896L;

			public boolean canPlayAI()
	        {
	          Object o = getCreature.execute();
	          if (o == null)
	        	  return false;
	          
	          CardList cl = (CardList)getCreature.execute();
	          return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************

	    

	    //*************** START *********** START **************************
	    else if(cardName.equals("Ember-Fist Zubera"))
	    {
	      //counts Zubera in all graveyards for this turn
	      final CommandReturn countZubera = new CommandReturn()
	      {
	        public Object execute()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Graveyard.getCards());
	          list.addAll(AllZone.Computer_Graveyard.getCards());

	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return (c.getTurnInZone() == AllZone.Phase.getTurn()) &&
	                      (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling") );
	            }
	          });//CardListFilter()

	          return new Integer(list.size());
	        }
	      };

	      final Input[] input = new Input[1];

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          //human chooses target on resolve,
	          //computer chooses target in Command destroy
	          if(Constant.Player.Human.equals(card.getController()))
	             AllZone.InputControl.setInput(input[0]);
	          else
	          {
	            int damage = ((Integer)countZubera.execute()).intValue();

	            if(getTargetCard() != null)
	            {
	              if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canDamage(card, getTargetCard()) 
	                 && CardFactoryUtil.canTarget(card,getTargetCard()) )
	              {
	                Card c = getTargetCard();
	                c.addDamage(damage, card);
	              }
	            }
	            else
	              AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage);
	          }
	        }//resolve()
	      };//SpellAbility

	      input[0] = new Input()
	      {
			private static final long serialVersionUID = 1899925898843297992L;
			
			public void showMessage()
	        {
	          int damage = ((Integer)countZubera.execute()).intValue();
	          AllZone.Display.showMessage("Select target Creature, Planeswalker or Player - " + damage +" damage ");
	          ButtonUtil.disableAll();
	        }
	        public void selectCard(Card card, PlayerZone zone)
	        {
	          if((card.isCreature() || card.isPlaneswalker()) && zone.is(Constant.Zone.Play))
	          {
	            int damage = ((Integer)countZubera.execute()).intValue();
	            card.addDamage(damage, card);

	            //have to do this since state effects aren't checked
	            //after this "Input" class is done
	            //basically this makes everything work right
	            //Ember-Fist Zubera can destroy a 2/2 creature
	            AllZone.GameAction.checkStateEffects();
	            stop();
	          }
	        }//selectCard()
	        public void selectPlayer(String player)
	        {
	          int damage = ((Integer)countZubera.execute()).intValue();
	          AllZone.GameAction.getPlayerLife(player).subtractLife(damage);
	          stop();
	        }//selectPlayer()
	      };//Input

	      Command destroy = new Command()
	      {
	 		private static final long serialVersionUID = -1889425992069348304L;

			public void execute()
	        {
	          ability.setStackDescription(card +" causes damage to creature or player");

	          @SuppressWarnings("unused") // damage
			  int damage = ((Integer)countZubera.execute()).intValue();

	          String con = card.getController();

	          //human chooses target on resolve,
	          //computer chooses target in Command destroy
	          if(con.equals(Constant.Player.Computer))
	            ability.setTargetPlayer(Constant.Player.Human);

	          AllZone.Stack.add(ability);
	        }//execute()
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Ashen-Skin Zubera"))
	    {
	      //counts Zubera in all graveyards for this turn
	      final CommandReturn countZubera = new CommandReturn()
	      {
	        public Object execute()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Graveyard.getCards());
	          list.addAll(AllZone.Computer_Graveyard.getCards());

	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return (c.getTurnInZone() == AllZone.Phase.getTurn()) &&
	                      (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling") );
	            }
	          });//CardListFilter()
	          return new Integer(list.size());
	        }
	      };//CommandReturn

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          int discard = ((Integer)countZubera.execute()).intValue();

	          if(Constant.Player.Human.equals(getTargetPlayer()))
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard(discard));
	          else
	          {
	            for(int i = 0; i < discard; i++)
	              AllZone.GameAction.discardRandom(Constant.Player.Computer);
	          }
	        }//resolve()
	      };//SpellAbility

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -7494691537986218546L;

			public void execute()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          ability.setTargetPlayer(opponent);
	          ability.setStackDescription(card +" - " + opponent +" discards cards");

	          AllZone.Stack.add(ability);
	        }//execute()
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Floating-Dream Zubera"))
	    {
	      //counts Zubera in all graveyards for this turn
	      final CommandReturn countZubera = new CommandReturn()
	      {
	        public Object execute()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Graveyard.getCards());
	          list.addAll(AllZone.Computer_Graveyard.getCards());

	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return (c.getTurnInZone() == AllZone.Phase.getTurn()) &&
	                      (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling") );
	            }
	          });//CardListFilter()
	          return new Integer(list.size());
	        }
	      };//CommandReturn

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          int number = ((Integer)countZubera.execute()).intValue();

	          for(int i = 0; i < number; i++)
	            AllZone.GameAction.drawCard(getTargetPlayer());
	        }//resolve()
	      };//SpellAbility

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -5814070329854975419L;

			public void execute()
	        {
	          ability.setTargetPlayer(card.getController());
	          ability.setStackDescription(card +" - " +card.getController() +" draws cards");
	          AllZone.Stack.add(ability);

	        }//execute()
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Silent-Chant Zubera"))
	    {
	      //counts Zubera in all graveyards for this turn
	      final CommandReturn countZubera = new CommandReturn()
	      {
	        public Object execute()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Graveyard.getCards());
	          list.addAll(AllZone.Computer_Graveyard.getCards());

	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return (c.getTurnInZone() == AllZone.Phase.getTurn()) &&
	                      (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling") );
	            }
	          });//CardListFilter()
	          return new Integer(list.size());
	        }
	      };//CommandReturn

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          int number = ((Integer)countZubera.execute()).intValue();

	          PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
	          life.addLife(number * 2);
	        }//resolve()
	      };//SpellAbility

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -2327085948421343657L;

			public void execute()
	        {
	          ability.setTargetPlayer(card.getController());
	          ability.setStackDescription(card +" - " +card.getController() +" gains life");
	          AllZone.Stack.add(ability);

	        }//execute()
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************




	    //*************** START *********** START **************************
	    else if(cardName.equals("Dripping-Tongue Zubera"))
	    {
	      //counts Zubera in all graveyards for this turn
	      final CommandReturn countZubera = new CommandReturn()
	      {
	        public Object execute()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Graveyard.getCards());
	          list.addAll(AllZone.Computer_Graveyard.getCards());

	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return (c.getTurnInZone() == AllZone.Phase.getTurn()) &&
	                      (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling") );
	            }
	          });//CardListFilter()
	          return new Integer(list.size());
	        }
	      };//CommandReturn

	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          int count = ((Integer)countZubera.execute()).intValue();
	          for(int i = 0; i < count; i++)
	            makeToken();
	        }//resolve()
	        void makeToken()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Spirit");
	          c.setImageName("C 1 1 Spirit");
	          c.setManaCost("");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Spirit");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);

	          play.add(c);
	        }//makeToken()

	      };//SpellAbility

	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = 8362692868619919330L;
	        public void execute()
	        {
	          ability.setTargetPlayer(card.getController());
	          ability.setStackDescription(card +" - " +card.getController() +" puts tokens into play");
	          AllZone.Stack.add(ability);
	        }//execute()
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Keiga, the Tide Star"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            PlayerZone oldPlay = AllZone.getZone(getTargetCard());

	            //so "comes into play" abilities don't trigger
	            //getTargetCard().addComesIntoPlayCommand(Command.Blank);

	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
		        ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
	            
	            play.add(getTargetCard());
	            oldPlay.remove(getTargetCard());

	            getTargetCard().setController(card.getController());
	            
	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
		        ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	          }
	        }//resolve()
	      };

	      final Input targetInput = new Input()
	      {
			private static final long serialVersionUID = -8727869672234802473L;
			
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
	          else if(c.isCreature() && zone.is(Constant.Zone.Play))
	          {
	            ability.setTargetCard(c);
	            ability.setStackDescription("Gain control of " +ability.getTargetCard());
	            AllZone.Stack.add(ability);
	            stop();
	          }
	        }
	      };//Input
	      Command destroy = new Command()
	      {
			private static final long serialVersionUID = -3868616119471172026L;

			public void execute()
	        {
	          String con = card.getController();
	          CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);

	          if(con.equals(Constant.Player.Human))
	            AllZone.InputControl.setInput(targetInput);
	          else if(list.size() != 0)
	          {
	            Card target = CardFactoryUtil.AI_getBestCreature(list);
	            ability.setTargetCard(target);
	            AllZone.Stack.add(ability);
	          }
	        }//execute()
	      };
	      card.addDestroyCommand(destroy);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if (cardName.equals("Elvish Piper"))
	    {
	    	final SpellAbility ability = new Ability_Tap(card, "G")
		    {
				private static final long serialVersionUID = 4414609319033894302L;
				public boolean canPlayAI() {return getCreature().size() != 0;}
		        public void chooseTargetAI()
		        {
		          card.tap();
		          Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
		          setTargetCard(target);
		        }
		        CardList getCreature()
		        {
		          CardList list = new CardList(AllZone.Computer_Hand.getCards());
		          list = list.getType("Creature");
		          return list;
		        }
	    		public void resolve()
	    		{
	    			Card c = getTargetCard();
	  	          	PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	  	          	PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

	  	          	if(AllZone.GameAction.isCardInZone(c, hand))
	  	          	{
	  	          		hand.remove(c);
	  	          		play.add(c);
	  	          	}
	    		}
		    };
		    
		    ability.setBeforePayMana(new Input()
		    { 
               private static final long serialVersionUID = -1647181037510967127L;

			   public void showMessage()
		       {
		   		   	String controller = card.getController();
		            CardList creats = new CardList(AllZone.getZone(Constant.Zone.Hand, controller).getCards());
		            creats = creats.filter(new CardListFilter()
		            {
						public boolean addCard(Card c)
						{
							PlayerZone zone = AllZone.getZone(c);
							return c.isCreature() && zone.is(Constant.Zone.Hand);
						}
		            	
		            });
		            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, creats, "Select a creature", false));
		          }
		      });
		      card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	    
	    /*
	    //*************** START *********** START **************************
	    else if(cardName.equals("Elvish Piper"))
	    {
	      final SpellAbility ability = new Ability_Tap(card, "G")
	      {
	        private static final long serialVersionUID = 8788555124182810249L;

			public boolean canPlayAI() {return getCreature().size() != 0;}
	        public void chooseTargetAI()
	        {
	          card.tap();
	          Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	          setTargetCard(target);
	        }
	        CardList getCreature()
	        {
	          CardList list = new CardList(AllZone.Computer_Hand.getCards());
	          list = list.getType("Creature");
	          return list;
	        }

	        public void resolve()
	        {
	          Card c = getTargetCard();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

	          if(AllZone.GameAction.isCardInZone(c, hand))
	          {
	            hand.remove(c);
	            play.add(c);
	          }
	        }
	      };//SpellAbility
	      card.addSpellAbility(ability);
	      ability.setDescription("G, tap: Put a creature card from your hand into play.");

	      final Command paid  = new Command()
	      {
			private static final long serialVersionUID = 6859737530872573139L;
	        public void execute()
	        {
	          AllZone.InputControl.resetInput();
	          AllZone.Stack.add(ability);
	        }
	      };
	      final Command unpaid = new Command()
	      {
			private static final long serialVersionUID = -257927480355704167L;

			public void execute()
	        {
	            card.untap();
	        }
	      };
	      final Input target = new Input()
	      {
			private static final long serialVersionUID = -1041198540673942649L;
			
			public void showMessage()
	        {
	          ButtonUtil.enableOnlyCancel();
	          AllZone.Display.showMessage("Select creature from your hand to put into play");
	        }
	        public void selectCard(Card c, PlayerZone zone)
	        {
	          if(c.isCreature() && zone.is(Constant.Zone.Hand, Constant.Player.Human))
	          {
	            card.tap();

	            ability.setTargetCard(c);//since setTargetCard() changes stack description
	            ability.setStackDescription("Put into play " +c);

	            AllZone.InputControl.setInput(new Input_PayManaCost_Ability(ability.getManaCost(), paid, unpaid));
	          }
	        }
	        public void selectButtonCancel()
	        {
	          card.untap();
	          stop();
	        }
	      };//Input target
	      ability.setBeforePayMana(target);
	    }//*************** END ************ END **************************
	    
	     */

	    //*************** START *********** START **************************
	    else if(cardName.equals("Weathered Wayfarer"))
	    {
	      final SpellAbility ability = new Ability_Tap(card, "W")
	      {
			private static final long serialVersionUID = 2902408879239353813L;
			
			public void resolve()
	        {
	          //getTargetCard() will NEVER be null

	          //checks to see if card is still in the library
	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), library))
	          {
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	            AllZone.GameAction.moveTo(hand, getTargetCard());
	          }
	        }//resolve()
	        public boolean canPlay()
	        {
	          String oppPlayer = AllZone.GameAction.getOpponent(card.getController());

	          PlayerZone selfZone = AllZone.getZone(Constant.Zone.Play, card.getController());
	          PlayerZone oppZone = AllZone.getZone(Constant.Zone.Play, oppPlayer);

	          CardList self = new CardList(selfZone.getCards());
	          CardList opp = new CardList(oppZone.getCards());

	          self = self.getType("Land");
	          opp = opp.getType("Land");

	          //checks to see if any land in library
	          PlayerZone selfLibrary = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList library = new CardList(selfLibrary.getCards());
	          library = library.getType("Land");

	          return (self.size() < opp.size()) && (library.size() != 0) && super.canPlay();
	        }
	       public void chooseTargetAI()
	       {
	         PlayerZone selfLibrary = AllZone.getZone(Constant.Zone.Library, card.getController());
	         CardList library = new CardList(selfLibrary.getCards());
	         library = library.getType("Land");

	         setTargetCard(library.get(0));
	       }
	      };//SpellAbility

	      Input target = new Input()
	      {
	        private static final long serialVersionUID = 3492362297282622857L;

			public void showMessage()
	        {
	          CardList land = new CardList(AllZone.Human_Library.getCards());
	          land = land.getType("Land");
	          Object o = AllZone.Display.getChoiceOptional("Select a Land", land.toArray());

	          //techincally not correct, but correct enough
	          //this allows players to look at their decks without paying anything
	          if(o == null)
	            stop();
	          else
	          {
	            AllZone.GameAction.shuffle("Human");
	            ability.setTargetCard((Card)o);
	            stopSetNext(new Input_PayManaCost(ability));
	          }
	        }//showMessage()
	      };//Input - target

	      card.addSpellAbility(ability);
	      ability.setDescription("W, tap: Search your library for a land card, reveal it, and put it into your hand. Then shuffle your library. Play this ability only if an opponent controls more lands than you.");
	      ability.setBeforePayMana(target);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Disciple of Kangee"))
	    {
	      final SpellAbility ability = new Ability_Tap(card, "U")
	      {
	        private static final long serialVersionUID = -5169389637917649036L;
			public boolean canPlayAI()
	        {
	          if(CardFactoryUtil.AI_doesCreatureAttack(card))
	            return false;

	          return CardFactoryUtil.AI_getHumanCreature("Flying", card, false).isEmpty() &&
	                 (getCreature().size() != 0);
	        }
	        public void chooseTargetAI()
	        {
	          card.tap();
	          Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	          setTargetCard(target);
	        }
	        CardList getCreature()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) &&
	                     (! c.getKeyword().contains("Flying")) && CardFactoryUtil.canTarget(card, c);
	            }
	          });
	          list.remove(card);
	          return list;
	        }//getCreature()
	        public void resolve()
	        {
	          if (AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	            final Card[] creature = new Card[1];
	            
	            creature[0] = getTargetCard();
	            final String origManaCost = creature[0].getManaCost();
	            final String tgtName = creature[0].getName();
	            final String[] creatureIsColor = {tgtName + " is black.", tgtName + " is blue.", tgtName + " is green.",
	            	                    	tgtName + " is red.", tgtName + " is white.", tgtName + " is colorless."};
	            final boolean[] colorFlag = {false, false, false, false, false, false};
  	            
	            for (int i=0; i<6; i++)
	            {
	              if (creature[0].getIntrinsicKeyword().contains(creatureIsColor[i]))
	              {
	                colorFlag[i] = true;
	              }
	            }
	            
	            final Command EOT = new Command()
	            {
				  private static final long serialVersionUID = -1899153704584793548L;

				  public void execute()
	              {
	                if (AllZone.GameAction.isCardInPlay(creature[0]))
	                {
	                  creature[0].removeExtrinsicKeyword("Flying");
	                  creature[0].removeExtrinsicKeyword(tgtName + " is blue.");
	                  creature[0].setManaCost(origManaCost);
	                  for (int i=0; i<6; i++)
	                  {
	                    if (colorFlag[i])
	                    {
	                      creature[0].addIntrinsicKeyword(creatureIsColor[i]);
	                    }
	                  }
	                }
	              }
	            };
	            creature[0].setManaCost(Integer.toString(CardUtil.getConvertedManaCost(origManaCost)));
	            for (int i=0; i<6; i++)
	            {
	              if (colorFlag[i])
	              {
	                creature[0].removeIntrinsicKeyword(creatureIsColor[i]);
	              }
	            }
	            creature[0].addExtrinsicKeyword("Flying");
	            creature[0].addExtrinsicKeyword(tgtName + " is blue.");
	            AllZone.EndOfTurn.addUntil(EOT);
	          }//if (card is in play)
	        }//resolve()
	      };//SpellAbility
	      card.addSpellAbility(ability);
	      ability.setDescription("U, tap: Target creature gains flying and becomes blue until end of turn.");

	      ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Puppeteer"))
	    {
	      //tap - target creature
	      final SpellAbility ability = new Ability_Tap(card, "U")
	      {
	        private static final long serialVersionUID = 7698358771800336470L;
			public boolean canPlayAI() {return getTapped().size() != 0;}
	        public void chooseTargetAI()
	        {
	          card.tap();
	          Card target = CardFactoryUtil.AI_getBestCreature(getTapped());
	          setTargetCard(target);
	        }
	        CardList getTapped()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isCreature() && c.isTapped();
	            }
	          });
	          return list;
	        }//getTapped()
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	            Card c = getTargetCard();
	            if(c.isTapped())
	              c.untap();
	            else
	              c.tap();
	          }
	        }//resolve()
	      };//SpellAbility
	      card.addSpellAbility(ability);
	      ability.setDescription("U, tap: Tap or untap target creature.");

	      ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
	    }//*************** END ************ END **************************



	  //*************** START *********** START **************************
	    else if(cardName.equals("Sorceress Queen") || cardName.equals("Serendib Sorcerer"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card)
	      {
	        private static final long serialVersionUID = -6853184726011448677L;
			public boolean canPlayAI()
	        {
	          Card c = getCreature();
	          if(c == null)
	            return false;
	          else
	          {
	            setTargetCard(c);
	            return true;
	          }
	        }//canPlayAI()
	        //may return null
	        public Card getCreature()
	        {
	          CardList untapped = CardFactoryUtil.AI_getHumanCreature(card, true);
	          untapped = untapped.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isUntapped() && 2 < c.getNetDefense() && c != card;
	            }
	          });
	          if(untapped.isEmpty())
	            return null;

	          Card big = CardFactoryUtil.AI_getBestCreature(untapped);
	          return big;
	        }
	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	            final Card[] creature = new Card[1];

	            creature[0] = getTargetCard();
	            final int[] originalAttack  = {creature[0].getBaseAttack()};
	            final int[] originalDefense = {creature[0].getBaseDefense()};

	            creature[0].setBaseAttack(0);
	            creature[0].setBaseDefense(2);

	            final Command EOT = new Command()
	            {
					private static final long serialVersionUID = 6437463765161964445L;

				public void execute()
	              {
	                if(AllZone.GameAction.isCardInPlay(creature[0]))
	                {
	                  creature[0].setBaseAttack(originalAttack[0]);
	                  creature[0].setBaseDefense(originalDefense[0]);
	                }
	              }
	            };
	            AllZone.EndOfTurn.addUntil(EOT);
	          }//is card in play?
	        }//resolve()
	      };//SpellAbility
	      card.addSpellAbility(ability);
	      ability.setDescription("tap: Target creature other than "+cardName +" becomes 0/2 until end of turn.");
	      //this ability can target "this card" when it shouldn't be able to
	      ability.setBeforePayMana(CardFactoryUtil.input_targetCreature_NoCost_TapAbility_NoTargetSelf(ability));
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Immaculate Magistrate"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card)
	      {
	        private static final long serialVersionUID = 8976980151320100343L;
			public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	            PlayerZone zone = AllZone.getZone(Constant.Zone.Play, card.getController());
	            CardList list = new CardList(zone.getCards());
	            int nElf = list.getType("Elf").size();

	            Card c = getTargetCard();
	            c.addCounter(Counters.P1P1, nElf);
	            
	          }//is card in play?
	        }//resolve()
	        public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isUntapped() && !c.equals(card) && c.isCreature();
	            }
	          });

	          if(list.isEmpty())
	            return false;

	          list.shuffle();

	          setTargetCard(list.get(0));
	          return true;
	        }//canPlayAI()
	      };//SpellAbility
	      card.addSpellAbility(ability);
	      ability.setDescription("tap: Put a +1/+1 counter on target creature for each Elf you control.");

	      ability.setBeforePayMana(CardFactoryUtil.input_targetCreature_NoCost_TapAbility(ability));
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Mawcor") || cardName.equals("Pirate Ship"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card)
	      {
	        private static final long serialVersionUID = 6238678956434079L;
			public boolean canPlayAI()
	        {
	          return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
	        }

	        public void chooseTargetAI()
	        {
	          setTargetPlayer(Constant.Player.Human);
	        }
	        public void resolve()
	        {
	          if(getTargetCard() != null)
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	              getTargetCard().addDamage(1, card);
	          }
	          else
	            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(1);
	        }//resolve()
	      };//SpellAbility
	      card.addSpellAbility(ability);
	      ability.setDescription("tap: "+ cardName +" deals 1 damage to target creature or player.");

	      ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true));
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Orcish Artillery") || cardName.equals("Goblin Artillery") 
	    || cardName.equals("Orcish Cannoneers"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card)
	      {
	        private static final long serialVersionUID = -305363091630642003L;
			public boolean canPlayAI()
	        {
	          return AllZone.Computer_Life.getLife() > 6;
	        }

	        public void chooseTargetAI()
	        {
	          CardList list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
	          list.shuffle();

	          if(list.isEmpty() || AllZone.Human_Life.getLife() < 5)
	            setTargetPlayer(Constant.Player.Human);
	          else
	            setTargetCard(list.get(0));
	        }
	        public void resolve()
	        {
	          if(getTargetCard() != null)
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              getTargetCard().addDamage(2, card);
	              //3 damage to self
	              AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(3);
	              
	              if (card.getKeyword().contains("Lifelink"))
	              	GameActionUtil.executeLifeLinkEffects(card, 5);
	              
	              CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
        		  for (Card crd : cl)
        		  {
        			   GameActionUtil.executeGuiltyConscienceEffects(card, crd, 5);
        		  }
	              
	            }
	          }
	          else
	          {
	            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(2);
	            //3 damage to self
	            AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(3);
	            
	            if (card.getKeyword().contains("Lifelink"))
	            	GameActionUtil.executeLifeLinkEffects(card, 5);
	            
	            CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
      		  	for (Card crd : cl)
      		    {
      			   GameActionUtil.executeGuiltyConscienceEffects(card, crd, 5);
      		    }
	          }
	          
	        }//resolve()
	      };//SpellAbility
	      card.addSpellAbility(ability);
	      ability.setDescription("tap: " +cardName + " deals 2 damage to target creature or player and 3 damage to you.");

	      ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability,true));
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Sparksmith"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card)
	      {
	        private static final long serialVersionUID = 681102636956052363L;
			public boolean canPlayAI()
	        {
	          int n = countGoblins();
	          return (AllZone.Computer_Life.getLife() > n) &&
	                 (CardFactoryUtil.AI_getHumanCreature(n, card, true).size() != 0);
	        }
	        public void chooseTargetAI()
	        {
	          CardList list = CardFactoryUtil.AI_getHumanCreature(countGoblins(), card, true);
	          list.shuffle();
	          setTargetCard(list.get(0));
	        }

	        public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	            PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	            life.subtractLife(countGoblins());

	            getTargetCard().addDamage(countGoblins(), card);
	            
	            if (card.getKeyword().contains("Lifelink"))
	            	GameActionUtil.executeLifeLinkEffects(card, 2*countGoblins());
	            
	            CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
      		    for (Card crd : cl)
      		    {
      			   GameActionUtil.executeGuiltyConscienceEffects(card, crd, 2*countGoblins());
      		    }
	          }
	        }//resolve()
	        int countGoblins()
	        {
	          CardList list = new CardList();
	          list.addAll(AllZone.Human_Play.getCards());
	          list.addAll(AllZone.Computer_Play.getCards());
	          list = list.getType("Goblin");
	          return list.size();
	        }
	      };//SpellAbility
	      card.addSpellAbility(ability);
	      ability.setDescription("tap: Sparksmith deals X damage to target creature and X damage to you, where X is the number of Goblins in play.");

	      ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
	    }//*************** END ************ END **************************

	  //*************** START *********** START **************************
	    else if(cardName.equals("Rathi Assassin"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card, "1 B B")
	      {
	    	private static final long serialVersionUID = 1117792059002738977L;
			public boolean canPlay()
	    	{
	    		return AllZone.GameAction.isCardInPlay(card) && !card.isTapped() && !card.hasSickness();
	    	}
	        public boolean canPlayAI()
	        {
	          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
	          human = human.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c) {return c.isTapped() && !CardUtil.getColor(c).equals(Constant.Color.Black);}
	          });

	          CardListUtil.sortAttack(human);
	          CardListUtil.sortFlying(human);

	          if(0 < human.size())
	            setTargetCard(human.get(0));

	          return 0 < human.size();
	        }
	        public void resolve()
	        {
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c) && c.isTapped()  && CardFactoryUtil.canTarget(card, c) && !CardUtil.getColor(c).equals(Constant.Color.Black) )
	          {
	            AllZone.GameAction.destroy(c);
	          }
	        }//resolve()
	      };//SpellAbility
	            
	      ability.setBeforePayMana(new Input()
	      {
	   	   private static final long serialVersionUID = -7903295056497483023L;

	   	   public void showMessage()
	          {
	   		   	String opponent = AllZone.GameAction.getOpponent(card.getController());
	            CardList slivers = new CardList(AllZone.getZone(Constant.Zone.Play, opponent).getCards());
	            slivers = slivers.filter(new CardListFilter()
	            {

					public boolean addCard(Card c)
					{
						PlayerZone zone = AllZone.getZone(c);
						return c.isCreature() && zone.is(Constant.Zone.Play) && c.isTapped() && ! CardUtil.getColor(c).equals(Constant.Color.Black);
					}
	            	
	            });
	            
	            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, slivers, "Select a tapped non-black creature", true));
	          }
	      });

	      card.addSpellAbility(ability);
	      ability.setDescription("1 B B, tap: Destroy target tapped nonblack creature.");
	      //ability.setBeforePayMana(target);
	      //ability.setAfterPayMana(target);
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Royal Assassin"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card)
	      {
	        private static final long serialVersionUID = 1974437552336643722L;
			public boolean canPlayAI()
	        {
	          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
	          human = human.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c) {return c.isTapped();}
	          });

	          CardListUtil.sortAttack(human);
	          CardListUtil.sortFlying(human);

	          if(0 < human.size())
	            setTargetCard(human.get(0));

	          return 0 < human.size();
	        }
	        public void resolve()
	        {
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c) && c.isTapped()  && CardFactoryUtil.canTarget(card, c) )
	          {
	            AllZone.GameAction.destroy(c);
	          }
	        }//resolve()
	      };//SpellAbility

	      Input target = new Input()
	      {
	        private static final long serialVersionUID = -5894703805950364923L;
			public void showMessage()
	        {
	          AllZone.Display.showMessage("Select target tapped creature to destroy");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectCard(Card c, PlayerZone zone)
	        {
	          if(!CardFactoryUtil.canTarget(card, c)){
	          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
	          }
	          else if(c.isCreature() && zone.is(Constant.Zone.Play) && c.isTapped())
	          {
	            //tap ability
	            card.tap();

	            ability.setTargetCard(c);
	            AllZone.Stack.add(ability);
	            stop();
	          }
	        }//selectCard()
	      };//Input

	      card.addSpellAbility(ability);
	      ability.setDescription("tap: Destroy target tapped creature.");
	      ability.setBeforePayMana(target);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Giltspire Avenger"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card)
	      {
	        private static final long serialVersionUID = -1117719063688165635L;

			public boolean canPlayAI()
	        {
	          return false;
	        }
	        
	        public boolean canPlay()
	        {
	        	System.out.println("phase =" + AllZone.Phase.getPhase());
	        	if ((AllZone.Phase.getPhase().equals(Constant.Phase.Main2) || 
	        		AllZone.Phase.getPhase().equals(Constant.Phase.End_Of_Turn)) &&
	        		!card.hasSickness() && card.isUntapped())
	        		return true;
	        	else
	        		return false;
	        	
	        }
	        
	        public void resolve()
	        {
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c) && (c.getDealtCombatDmgToOppThisTurn() || c.getDealtDmgToOppThisTurn())
	             && CardFactoryUtil.canTarget(card, c) )
	          {
	            AllZone.GameAction.destroy(c);
	          }
	        }//resolve()
	      };//SpellAbility

	      Input target = new Input()
	      {
	        private static final long serialVersionUID = -4946540988877576202L;
			public void showMessage()
	        {
	          AllZone.Display.showMessage("Select target creature to destroy");
	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectCard(Card c, PlayerZone zone)
	        {
	          if(!CardFactoryUtil.canTarget(card, c)){
	          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
	          }
	          else if(c.isCreature() && zone.is(Constant.Zone.Play) && (c.getDealtCombatDmgToOppThisTurn() || 
	        		  c.getDealtDmgToOppThisTurn()) )
	          {
	            //tap ability
	            card.tap();

	            ability.setTargetCard(c);
	            AllZone.Stack.add(ability);
	            stop();
	          }
	        }//selectCard()
	      };//Input

	      card.addSpellAbility(ability);
	      ability.setDescription("tap: Destroy target creature that dealt damage to you this turn.");
	      ability.setBeforePayMana(target);
	    }//*************** END ************ END **************************

	    //*************** START *********** START **************************
	    else if(cardName.equals("Vedalken Plotter"))
	    {
	      final Card[] target = new Card[2];
	      final int[] index = new int[1];

	      final Ability ability = new Ability(card, "")
	      {

			private static final long serialVersionUID = -3075569295823682336L;
			public boolean canPlayAI()
	        {
	          return false;
	        }
	        public void resolve()
	        {
	            
	            Card crd0 = target[0];
	            Card crd1 = target[1];
	            
	            if (crd0 != null && crd1 != null)
	            {
	            
		            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
		            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
		
		            PlayerZone from0 = AllZone.getZone(crd0);
		            from0.remove(crd0);
		            PlayerZone from1 = AllZone.getZone(crd1);
		            from1.remove(crd1);
		           
		            crd0.setController(AllZone.GameAction.getOpponent(card.getController()));
		            crd1.setController(card.getController());
		            
		            PlayerZone to0 = AllZone.getZone(Constant.Zone.Play, AllZone.GameAction.getOpponent(card.getController()));
		            to0.add(crd0);
		            PlayerZone to1 = AllZone.getZone(Constant.Zone.Play, card.getController());
		            to1.add(crd1);
		
		            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
		            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	            }
	            
	        }//resolve()
	      };//SpellAbility


	      final Input input = new Input()
	      {
			
			private static final long serialVersionUID = -7143706716256752987L;
			public void showMessage()
	        {
	          if(index[0] == 0)
	            AllZone.Display.showMessage("Select target land you control.");
	          else
	            AllZone.Display.showMessage("Select target land opponent controls.");

	          ButtonUtil.enableOnlyCancel();
	        }
	        public void selectButtonCancel() {stop();}
	        public void selectCard(Card c, PlayerZone zone)
	        {
	          //must target creature you control
	          if(index[0] == 0 && !c.getController().equals(card.getController()))
	             return;

	          //must target creature you don't control
	          if(index[0] == 1 && c.getController().equals(card.getController()))
	             return;


	          if(c.isLand() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, c))
	          {
	        	  //System.out.println("c is: " +c);
	            target[index[0]] = c;
	            index[0]++;
	            showMessage();

	            if(index[0] == target.length) {
	                AllZone.Stack.add(ability);
	                stop();
	            }
	          }
	        }//selectCard()
	      };//Input

	      Command comesIntoPlay = new Command()
	      {
				private static final long serialVersionUID = 6513203926272187582L;
		
				public void execute() {
					index[0] = 0;
					if(card.getController().equals(Constant.Player.Human))
			            AllZone.InputControl.setInput(input);
				}  
	      };

	      //card.clearSpellAbility();
	      ability.setStackDescription(cardName + " - Exchange control of target land you control and target land an opponent controls.");
	      card.addComesIntoPlayCommand(comesIntoPlay);
	    }//*************** END ************ END **************************
	    


	    //*************** START *********** START **************************
	      else if(cardName.equals("Ramses Overdark"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {

	  		private static final long serialVersionUID = 3560953910041049722L;

	  		public boolean canPlayAI()
	          {
	            if(CardFactoryUtil.AI_doesCreatureAttack(card))
	              return false;

	            return CardFactoryUtil.AI_getHumanCreature(card, true).size() != 0;
	          }
	          public void chooseTargetAI()
	          {
	            CardList creature = CardFactoryUtil.AI_getHumanCreature(card, true);
	            Card target = CardFactoryUtil.AI_getBestCreature(creature);
	            setTargetCard(target);
	          }

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) && getTargetCard().isEnchanted() )
	            {
	              AllZone.GameAction.destroy(getTargetCard());
	            }
	          }//resolve()
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Destroy target enchanted creature.");

	        Input runtime = new Input()
	        {

	  		private static final long serialVersionUID = 7538894357147291895L;

	  		public void showMessage()
	          {
	            CardList all = new CardList();
	            all.addAll(AllZone.Human_Play.getCards());
	            all.addAll(AllZone.Computer_Play.getCards());
	            all = all.filter(new CardListFilter()
	            {
	  			public boolean addCard(Card c) {
	  				return AllZone.GameAction.isCardInPlay(c) && c.isCreature() && CardFactoryUtil.canTarget(card, c) && c.isEnchanted();
	  			}
	            });
	            
	            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, all, "Destroy target enchanted creature.", true));
	          }
	        };
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************




	      //*************** START *********** START **************************
	      else if(cardName.equals("Visara the Dreadful"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	          private static final long serialVersionUID = 6371765024236754171L;

	  		public boolean canPlayAI()
	          {
	            if(CardFactoryUtil.AI_doesCreatureAttack(card))
	              return false;

	            return CardFactoryUtil.AI_getHumanCreature(card, true).size() != 0;
	          }
	          public void chooseTargetAI()
	          {
	            CardList creature = CardFactoryUtil.AI_getHumanCreature(card, true);
	            Card target = CardFactoryUtil.AI_getBestCreature(creature);
	            setTargetCard(target);
	          }

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              AllZone.GameAction.destroyNoRegeneration(getTargetCard());
	            }
	          }//resolve()
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Destroy target creature. It can't be regenerated");

	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
	      }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Ethersworn Adjudicator"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card, "1 W B")
	        {
	  		private static final long serialVersionUID = 4160943954590944389L;

	  		public boolean canPlay()
	      	{
	      		SpellAbility sa;
	  	    	  for (int i=0; i<AllZone.Stack.size(); i++)
	  	    	  {
	  	    	       sa = AllZone.Stack.peek(i);
	  	    	       if (sa.getSourceCard().equals(card))
	  	    	             return false;
	  	    	  }
	  	    	  
	  	    	  if (AllZone.GameAction.isCardInPlay(card) && !card.hasSickness() && !card.isTapped())
	  	    		  return true;
	  	    	  else
	  	    		  return false;
	      		
	      	}
	          public boolean canPlayAI()
	          {
	            if(CardFactoryUtil.AI_doesCreatureAttack(card))
	              return false;

	            return CardFactoryUtil.AI_getHumanCreature(card, true).size() != 0 && CardFactoryUtil.AI_getHumanEnchantment(card, true).size() != 0;
	          }
	          public void chooseTargetAI()
	          {
	            CardList creature = CardFactoryUtil.AI_getHumanCreature(card, true);
	            if (creature.size() > 0){
	          	  Card target = CardFactoryUtil.AI_getBestCreature(creature);
	          	  setTargetCard(target);
	            }
	            else
	            {
	          	  CardList enchantment = CardFactoryUtil.AI_getHumanEnchantment(card, true);
	          	  if (enchantment.size() > 0)
	          	  {
	          		  Card target = CardFactoryUtil.AI_getBestEnchantment(enchantment, card, true);
	          		  setTargetCard(target);
	          	  }
	            }
	          }

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              AllZone.GameAction.destroy(getTargetCard());
	            }
	          }//resolve()
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap, 1 W B: Destroy target creature or enchantment.");

	        //ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
	        
	        Input runtime = new Input()
	        {
	  		private static final long serialVersionUID = -8099713981623158814L;

	  		public void showMessage()
	          {
	            CardList all = new CardList();
	            all.addAll(AllZone.Human_Play.getCards());
	            all.addAll(AllZone.Computer_Play.getCards());
	            all = all.filter(new CardListFilter()
	            {
	  			public boolean addCard(Card c) {
	  				return (c.isEnchantment() || c.isCreature() ) && CardFactoryUtil.canTarget(card, c);
	  			}
	            });
	            
	            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, all, "Destroy target creature or enchantment.", true));
	          }
	        };
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Wojek Embermage"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	          private static final long serialVersionUID = -1208482961653326721L;
	  		public boolean canPlayAI()
	          {
	            return (CardFactoryUtil.AI_getHumanCreature(1, card, true).size() != 0) &&
	                    (AllZone.Phase.getPhase().equals(Constant.Phase.Main2));
	          }

	          public void chooseTargetAI()
	          {
	            CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
	            list.shuffle();
	            setTargetCard(list.get(0));
	          }

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              CardList list = getRadiance(getTargetCard());
	              for(int i = 0; i < list.size(); i++) {
	                if (CardFactoryUtil.canDamage(card, list.get(i)))
	            	  list.get(i).addDamage(1, card);
	              }
	            }
	          }//resolve()
	          //parameter Card c, is included in CardList
	          //no multi-colored cards
	          
	          
	          CardList getRadiance(Card c)
	          {
	            String color = CardUtil.getColor(c);
	            if(color.equals(Constant.Color.Colorless))
	            {
	              CardList list = new CardList();
	              list.add(c);
	              return list;
	            }

	            CardList sameColor = new CardList();

	            //get all creatures
	            CardList list = new CardList();
	            list.addAll(AllZone.Human_Play.getCards());
	            list.addAll(AllZone.Computer_Play.getCards());
	            list = list.getType("Creature");

	            for(int i = 0; i < list.size(); i++)
	              if(CardFactoryUtil.sharesColorWith(list.get(i), c))
	                sameColor.add(list.get(i));

	            return sameColor;
	          }
	          
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("Radiance - tap: Wojek Embermage deals 1 damage to target creature and each other creature that shares a color with it.");

	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
	      }//*************** END ************ END **************************


	    /*
	    
	      //*************** START *********** START **************************
	      else if(cardName.equals("Timberwatch Elf"))
	      {
	        final Card[] creature = new Card[1];
	        final int[] pump = new int[1];
	        final Command EOT = new Command()
	        {
	  		private static final long serialVersionUID = -2764970883655205904L;

	  		public void execute()
	          {
	            if(AllZone.GameAction.isCardInPlay(creature[0]))
	            {
	              creature[0].addTempAttackBoost(-pump[0]);
	              creature[0].addTempDefenseBoost(-pump[0]);
	            }
	          }
	        };

	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	          private static final long serialVersionUID = -7536296719726210357L;
	  		public boolean canPlayAI()
	          {
	            CardList c = getAttackers();
	            if(c.isEmpty())
	              return false;
	            else
	            {
	              setTargetCard(c.get(0));
	              return true;
	            }
	          }//canPlayAI()
	          CardList getAttackers()
	          {
	            Card[] c = ComputerUtil.getAttackers().getAttackers();
	            CardList list = new CardList(c);
	            list = list.filter(new CardListFilter(){

	  			public boolean addCard(Card c) {
	  				return CardFactoryUtil.canTarget(card, c);
	  			}
	          	  
	            });
	            
	            //remove "this card" from attackers, if it is there
	            list.remove(card);
	            list.shuffle();
	            return list;
	          }//getAttackers()

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              creature[0] = getTargetCard();
	              pump[0]     = countElves();

	              creature[0].addTempAttackBoost(pump[0]);
	              creature[0].addTempDefenseBoost(pump[0]);

	              AllZone.EndOfTurn.addUntil(EOT);
	            }
	          }//resolve()
	          int countElves()
	          {
	            CardList elf = new CardList();
	            elf.addAll(AllZone.Human_Play.getCards());
	            elf.addAll(AllZone.Computer_Play.getCards());

	            elf = elf.getType("Elf");
	            return elf.size();
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Target creature gets +X/+X until end of turn, where X is the number of Elves in play.");

	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreature_NoCost_TapAbility(ability));
	      }//*************** END ************ END **************************
			*/

	      //*************** START *********** START **************************
	      else if(cardName.equals("Mad Auntie"))
	      {
	        final Card[] creature = new Card[1];
	        final Command EOT = new Command()
	        {
	          private static final long serialVersionUID = -5143708900761432510L;

	  		public void execute()
	          {
	            if(AllZone.GameAction.isCardInPlay(creature[0]))
	            {
	              creature[0].setShield(0);
	            }
	          }
	        };

	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	          private static final long serialVersionUID = -1280855188535819509L;
	  		public boolean canPlayAI()
	          {
	              return false;
	          }//canPlayAI()
	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              getTargetCard().addShield();
	              AllZone.EndOfTurn.addUntil(EOT);
	            }
	          }//resolve()
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Regenerate another target Goblin.");

	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreature_NoCost_TapAbility(ability));
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Intrepid Hero"))
	      {
	        //tap ability - no cost - target creature

	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	          private static final long serialVersionUID = -7825416464364928117L;
	  		public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) &&
	          	 getTargetCard().getNetAttack() >= 4)
	            {
	              AllZone.GameAction.destroy(getTargetCard());
	            }
	          }//resolve()
	          public boolean canPlayAI()
	          {
	            return getHumanCreatures().size() != 0;
	          }
	          public void chooseTargetAI()
	          {
	            CardList human = getHumanCreatures();
	            human.shuffle();
	            setTargetCard(human.get(0));
	          }
	          CardList getHumanCreatures()
	          {
	            CardList list = new CardList(AllZone.Human_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isCreature() && 3 < c.getNetAttack();
	              }
	            });
	            return list;
	          }
	        };//SpellAbility

	        Input target = new Input()
	        {
	          private static final long serialVersionUID = -7024383930124942288L;
	  		public void showMessage()
	          {
	            AllZone.Display.showMessage("Select target creature with power 4 or greater");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card card, PlayerZone zone)
	          {
	            if(card.isCreature() && zone.is(Constant.Zone.Play) && 3 < card.getNetAttack())
	            {
	              ability.setTargetCard(card);
	              stopSetNext(new Input_NoCost_TapAbility(ability));
	            }
	          }
	        };

	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Destroy target creature with power 4 or greater.");

	        ability.setBeforePayMana(target);
	      }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Tetsuo Umezawa"))
	      {
	        //tap ability - no cost - target creature

	        final Ability_Tap ability = new Ability_Tap(card)
	        {

	  		private static final long serialVersionUID = -8034678094689484203L;
	  		public void resolve()
	          {
	  		  CardList blockers = AllZone.Combat.getAllBlockers();
	  			
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) &&
	          	(blockers.contains(getTargetCard()) || getTargetCard().isTapped()) )  
	            {
	          	  AllZone.GameAction.destroy(getTargetCard());
	            }
	          }//resolve()
	          public boolean canPlayAI()
	          {
	            return false;
	          }
	        };//SpellAbility

	        Input target = new Input()
	        {

	  		private static final long serialVersionUID = -1939019440028116051L;
	  		public void showMessage()
	          {
	            AllZone.Display.showMessage("Select target tapped or blocking creature.");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card card, PlayerZone zone)
	          {
	            CardList blockers = AllZone.Combat.getAllBlockers();
	            if(card.isCreature() && zone.is(Constant.Zone.Play) &&
	          	  (blockers.contains(card) || card.isTapped()) )
	            {
	              ability.setTargetCard(card);
	              stopSetNext(new Input_NoCost_TapAbility(ability));
	            }
	          }
	        };

	        card.addSpellAbility(ability);
	        ability.setDescription("U B B R, Tap: Destroy target tapped or blocking creature.");

	        ability.setBeforePayMana(target);
	      }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Tor Wauki"))
	      {
	        //tap ability - no cost - target creature

	        final Ability_Tap ability = new Ability_Tap(card)
	        {

	  		private static final long serialVersionUID = -8034678094689484203L;
	  		public void resolve()
	          {
	  		  CardList attackers = new CardList(AllZone.Combat.getAttackers());
	  		  CardList blockers = AllZone.Combat.getAllBlockers();
	  			
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) &&
	          	(attackers.contains(getTargetCard()) || blockers.contains(getTargetCard()) )  )
	            {
	          	  getTargetCard().addDamage(2, card);
	            }
	          }//resolve()
	          public boolean canPlayAI()
	          {
	            return false;
	          }
	        };//SpellAbility

	        Input target = new Input()
	        {

	  		private static final long serialVersionUID = -1939019440028116051L;
	  		public void showMessage()
	          {
	            AllZone.Display.showMessage("Select target attacking or blocking creature.");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card card, PlayerZone zone)
	          {
	            CardList attackers = new CardList(AllZone.Combat.getAttackers());
	            CardList blockers = AllZone.Combat.getAllBlockers();
	            if(card.isCreature() && zone.is(Constant.Zone.Play) &&
	          	 (attackers.contains(card) || blockers.contains(card) ) )
	            {
	              ability.setTargetCard(card);
	              stopSetNext(new Input_NoCost_TapAbility(ability));
	            }
	          }
	        };

	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Tor Wauki deals 2 damage to target attacking or blocking creature.");

	        ability.setBeforePayMana(target);
	      }//*************** END ************ END **************************
	      
	      

	      //*************** START *********** START **************************
	      else if(cardName.equals("Lady Caleria"))
	      {
	        //tap ability - no cost - target creature

	        final Ability_Tap ability = new Ability_Tap(card)
	        {

	  		private static final long serialVersionUID = -8034678094689484203L;
	  		public void resolve()
	          {
	  		  CardList attackers = new CardList(AllZone.Combat.getAttackers());
	  		  CardList blockers = AllZone.Combat.getAllBlockers();
	  			
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) &&
	          	(attackers.contains(getTargetCard()) || blockers.contains(getTargetCard()) )  )
	            {
	          	  getTargetCard().addDamage(3, card);
	            }
	          }//resolve()
	          public boolean canPlayAI()
	          {
	            return false;
	          }
	        };//SpellAbility

	        Input target = new Input()
	        {

	  		private static final long serialVersionUID = -1939019440028116051L;
	  		public void showMessage()
	          {
	            AllZone.Display.showMessage("Select target attacking or blocking creature.");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card card, PlayerZone zone)
	          {
	            CardList attackers = new CardList(AllZone.Combat.getAttackers());
	            CardList blockers = AllZone.Combat.getAllBlockers();
	            if(card.isCreature() && zone.is(Constant.Zone.Play) &&
	          	 (attackers.contains(card) || blockers.contains(card) ) )
	            {
	              ability.setTargetCard(card);
	              stopSetNext(new Input_NoCost_TapAbility(ability));
	            }
	          }
	        };

	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Lady Caleria deals 3 damage to target attacking or blocking creature.");

	        ability.setBeforePayMana(target);
	      }//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	      else if(cardName.equals("Femeref Archers"))
	      {
	        //tap ability - no cost - target creature

	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	          
	  		private static final long serialVersionUID = -4369831076920644547L;
	  		public void resolve()
	          {
	  		  CardList attackers = new CardList(AllZone.Combat.getAttackers());
	  			
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) &&
	          	attackers.contains(getTargetCard()) && getTargetCard().getKeyword().contains("Flying"))
	            {
	          	  getTargetCard().addDamage(4, card);
	            }
	          }//resolve()
	          public boolean canPlayAI()
	          {
	            return false;
	          }
	        };//SpellAbility

	        Input target = new Input()
	        {
	          
	  		private static final long serialVersionUID = 3455890565752527632L;
	  		public void showMessage()
	          {
	            AllZone.Display.showMessage("Select target attacking creature with flying.");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card card, PlayerZone zone)
	          {
	            CardList attackers = new CardList(AllZone.Combat.getAttackers());
	            if(card.isCreature() && zone.is(Constant.Zone.Play) && card.getKeyword().contains("Flying") &&
	          	 attackers.contains(card))
	            {
	              ability.setTargetCard(card);
	              stopSetNext(new Input_NoCost_TapAbility(ability));
	            }
	          }
	        };

	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Femeref Archers deals 4 damage to target attacking creature with flying.");

	        ability.setBeforePayMana(target);
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Adarkar Valkyrie"))
	      {
	        //tap ability - no cost - target creature - EOT

	        final Card[] target = new Card[1];
	        
	        final Command destroy = new Command()
	        {
	  		private static final long serialVersionUID = -2433442359225521472L;

	  		public void execute()
	          {
	            AllZone.Stack.add(new Ability(card, "0", "Return " +target[0] +" from graveyard to play")
	            {
	              public void resolve()
	              {
	                PlayerZone grave = AllZone.getZone(target[0]);
	                //checks to see if card is still in the graveyard
	                if(AllZone.GameAction.isCardInZone(target[0], grave))
	                {
	                  PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	                  AllZone.GameAction.moveTo(play, target[0]);
	                  target[0].setController(card.getController());
	                }
	              }
	            });
	          }//execute()
	        };
	        
	        final Command untilEOT = new Command()
	        {
	          private static final long serialVersionUID = 2777978927867867610L;

	  		public void execute()
	          {
	            //resets the Card destroy Command
	            //target[0].addDestroy(Command.Blank);
	  		  target[0].removeDestroyCommand(destroy);	
	  		
	          }
	        };

	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	  		private static final long serialVersionUID = -8454685126878522607L;
	  		public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()))
	            {
	              target[0] = getTargetCard();
	              AllZone.EndOfTurn.addUntil(untilEOT);

	              //when destroyed, return to play
	              //add triggered ability to target card
	              target[0].addDestroyCommand(destroy);
	            }//if
	          }//resolve()
	          public boolean canPlayAI()
	          {
	            return false;
	          }
	        };//SpellAbility

	        Input targetInput = new Input()
	        {
	          private static final long serialVersionUID = 913860087744941946L;
	  		public void showMessage()
	          {
	            AllZone.Display.showMessage("Select target non-token creature other than this card");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card c, PlayerZone zone)
	          {
	            //must target non-token creature, and cannot target itself
	            if(c.isCreature() && (!c.isToken()) && (!c.equals(card)))
	            {
	              ability.setTargetCard(c);
	              stopSetNext(new Input_NoCost_TapAbility(ability));
	            }
	          }
	        };

	        card.addSpellAbility(ability);
	        ability.setDescription("tap: When target creature other than Adarkar Valkyrie is put into a graveyard this turn, return that card to play under your control.");

	        ability.setBeforePayMana(targetInput);
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Imperious Perfect"))
	      {
	        //mana tap ability
	        final Ability_Tap ability = new Ability_Tap(card, "G")
	        {
	          private static final long serialVersionUID = -3266607637871879336L;

	  		public boolean canPlayAI()
	          {
	            String phase = AllZone.Phase.getPhase();
	            return phase.equals(Constant.Phase.Main2);
	          }
	          public void chooseTargetAI() {card.tap();}

	          public void resolve()
	          {
	            Card c = new Card();

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setName("Elf Warrior");
	            c.setImageName("G 1 1 Elf Warrior");
	            c.setManaCost("G");
	            c.setToken(true);

	            c.addType("Creature");
	            c.addType("Elf");
	            c.addType("Warrior");

	            c.setBaseAttack(1);
	            c.setBaseDefense(1);

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            play.add(c);
	          }//resolve()
	        };//SpellAbility

	        card.addSpellAbility(ability);

	        ability.setDescription("G, tap: Put a 1/1 green Elf Warrior creature token into play.");
	        ability.setStackDescription("Imperious Perfect - Put a 1/1 green Elf Warrior creature token into play.");
	        ability.setBeforePayMana(new Input_PayManaCost(ability));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Wall of Kelp"))
	      {
	        //mana tap ability
	        final Ability_Tap ability = new Ability_Tap(card, "U U")
	        {
	          private static final long serialVersionUID = 2893813929304858905L;

	  		public boolean canPlayAI()
	          {
	            String phase = AllZone.Phase.getPhase();
	            return phase.equals(Constant.Phase.Main2);
	          }
	          public void chooseTargetAI() {card.tap();}

	          public void resolve()
	          {
	            Card c = new Card();

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setName("Kelp");
	            c.setImageName("U 0 1 Kelp");
	            c.setManaCost("U");
	            c.setToken(true);

	            c.addType("Creature");
	            c.addType("Plant");
	            c.addType("Wall");
	            c.addIntrinsicKeyword("Defender");

	            c.setBaseAttack(0);
	            c.setBaseDefense(1);

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            play.add(c);
	          }//resolve()
	        };//SpellAbility

	        card.addSpellAbility(ability);

	        ability.setDescription("UU, tap: Put a 0/1 blue Kelp Wall creature token with defender into play.");
	        ability.setStackDescription("Wall of Kelp - Put a 0/1 blue Kelp Wall creature token with defender into play");
	        ability.setBeforePayMana(new Input_PayManaCost(ability));
	      }//*************** END ************ END **************************




	      //*************** START *********** START **************************
	      else if(cardName.equals("Vedalken Mastermind"))
	      {
	        //mana tap ability
	        final Ability_Tap ability = new Ability_Tap(card, "U")
	        {
	          private static final long serialVersionUID = -6131368241135911606L;

	  		public boolean canPlayAI() {return false;}

	          public void resolve()
	          {
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
	            AllZone.GameAction.moveTo(hand, getTargetCard());
	            
	            if (getTargetCard().isToken())
	          	  hand.remove(getTargetCard());
	          }//resolve()
	        };//SpellAbility


	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = -5218098811060156481L;

	  		public void showMessage()
	          {
	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            CardList choice = new CardList(play.getCards());
	            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice, "Select a permanent you control.", true));
	          }
	        };
	        card.addSpellAbility(ability);
	        ability.setDescription("U, tap: Return target permanent you control to its owner's hand.");
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************




	      //*************** START *********** START **************************
	      else if(cardName.equals("Helldozer"))
	      {
	        //mana tap ability
	        final Ability_Tap ability = new Ability_Tap(card, "B B B")
	        {
	          private static final long serialVersionUID = 6426884086364885861L;

	  		public boolean canPlayAI()
	          {
	            if(CardFactoryUtil.AI_doesCreatureAttack(card))
	              return false;

	            CardList land = new CardList(AllZone.Human_Play.getCards());
	            land = land.getType("Land");
	            return land.size() != 0;
	          }

	          public void chooseTargetAI()
	          {
	            card.tap();

	            //target basic land that Human only has 1 or 2 in play
	            CardList land = new CardList(AllZone.Human_Play.getCards());
	            land = land.getType("Land");

	            Card target = null;

	            String[] name = {"Forest", "Swamp", "Plains", "Mountain", "Island"};
	            for(int i = 0; i < name.length; i++)
	              if(land.getName(name[i]).size() == 1)
	              {
	                target = land.getName(name[i]).get(0);
	                break;
	              }

	            //see if there are only 2 lands of the same type
	            if(target == null)
	            {
	              for(int i = 0; i < name.length; i++)
	                if(land.getName(name[i]).size() == 2)
	                {
	                target = land.getName(name[i]).get(0);
	                break;
	              }
	            }//if
	            if(target == null)
	            {
	              land.shuffle();
	              target = land.get(0);
	            }
	            setTargetCard(target);
	          }//chooseTargetAI()

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              AllZone.GameAction.destroy(getTargetCard());

	              //if non-basic, untap Helldozer
	              if(! getTargetCard().getType().contains("Basic"))
	                card.untap();
	            }
	          }//resolve()
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("BBB, tap: Destroy target land. If that land is nonbasic, untap Helldozer.");
	        ability.setBeforePayMana(CardFactoryUtil.input_targetType(ability, "Land"));
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Cao Cao, Lord of Wei"))
	      {
	        //mana tap ability
	        final Ability_Tap ability = new Ability_Tap(card, "0")
	        {
	          private static final long serialVersionUID = 6760838700101179614L;
	  		public void chooseTargetAI() {card.tap();}
	          public boolean canPlayAI()
	          {
	            int hand = AllZone.Human_Hand.getCards().length;
	            if((! CardFactoryUtil.AI_doesCreatureAttack(card)) && hand != 0)
	              return true;

	            return 2 <= hand;
	          }

	          public void resolve()
	          {
	            String player = AllZone.GameAction.getOpponent(card.getController());

	            if(player.equals(Constant.Player.Human))
	               AllZone.InputControl.setInput(CardFactoryUtil.input_discard(2));
	            else
	            {
	              AllZone.GameAction.discardRandom(player);
	              AllZone.GameAction.discardRandom(player);
	            }
	          }//resolve()
	          public boolean canPlay()
	          {
	            String opp = AllZone.GameAction.getOpponent(card.getController());
	            setStackDescription(card.getName() +" - " +opp +" discards 2 cards");

	            String phase         = AllZone.Phase.getPhase();
	            String activePlayer = AllZone.Phase.getActivePlayer();

	            return super.canPlay() &&
	                   phase.equals(Constant.Phase.Main1) &&
	                   card.getController().equals(activePlayer);
	          }
	        };//SpellAbility

	        ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
	        ability.setDescription("tap: Target opponent discards two cards. Play this ability only during your turn, before the combat phase.");
	        ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));

	        card.addSpellAbility(ability);
	      }//*************** END ************ END **************************
	      
	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Gwendlyn Di Corci"))
	      {
	        //mana tap ability
	        final Ability_Tap ability = new Ability_Tap(card, "0")
	        {

	  		private static final long serialVersionUID = -4211234606012596777L;
	  		public void chooseTargetAI() {setTargetPlayer(Constant.Player.Human); }
	          public boolean canPlayAI()
	          {
	            int hand = AllZone.Human_Hand.getCards().length;
	            return hand > 0;
	          }

	          public void resolve()
	          {
	            String player = getTargetPlayer();

	            AllZone.GameAction.discardRandom(player);
	            
	          }//resolve()
	          public boolean canPlay()
	          {
	            setStackDescription(card.getName() +" - " +getTargetPlayer() +" discards a card at random.");

	            String activePlayer = AllZone.Phase.getActivePlayer();

	            return super.canPlay() &&
	                   card.getController().equals(activePlayer);
	          }
	        };//SpellAbility
	        
	        Input input = new Input()
	        {
	  		private static final long serialVersionUID = 3312693459353844120L;

	  		public void showMessage()
	      	  {
	      	    //prevents this from running multiple times, which it is for some reason
	      	    if(ability.getSourceCard().isUntapped())
	      	    {
	      	      ability.getSourceCard().tap();
	      	      stopSetNext(CardFactoryUtil.input_targetPlayer(ability));
	      	    }
	      	  }
	        };

	        ability.setDescription("Tap: Target player discards a card at random. Activate this ability only during your turn.");
	        ability.setBeforePayMana(input);
	        //ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));

	        card.addSpellAbility(ability);
	      }//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	      else if(cardName.equals("Xira Arien"))
	      {
	        //mana tap ability
	        final Ability_Tap ability = new Ability_Tap(card, "B R G")
	        {

	  		private static final long serialVersionUID = 5373361883064666451L;
	  		public void chooseTargetAI() {setTargetPlayer(Constant.Player.Computer);}
	          public boolean canPlayAI()
	          {
	            int hand = AllZone.Computer_Hand.getCards().length;
	            int lib = AllZone.Computer_Library.getCards().length;
	            return hand < 6 && lib > 0;
	          }

	          public void resolve()
	          {
	            String player = getTargetPlayer();

	            AllZone.GameAction.drawCard(player);
	            
	          }//resolve()
	          public boolean canPlay()
	          {
	            
	            setStackDescription(card.getName() +" - " +getTargetPlayer() +" discards a card at random.");

	            return super.canPlay();
	          }
	        };//SpellAbility

	        ability.setDescription("B R G, Tap: Target player draws a card.");
	        //ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
	        ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));

	        card.addSpellAbility(ability);
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Wayward Soul"))
	      {
	        //mana ability
	        final Ability ability = new Ability(card, "U")
	        {
	          public boolean canPlayAI() {return false;}

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card) )
	            {
	              card.setBaseAttack(3);
	              card.setBaseDefense(2);
	              card.setIntrinsicKeyword(new ArrayList<String>());
	              card.addIntrinsicKeyword("Flying");

	              card.clearAssignedDamage();
	              card.setDamage(0);
	              card.untap();
	              AllZone.getZone(card).remove(card);

	              //put card on top of library
	              PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getOwner());
	              library.add(card, 0);
	            }
	          }//resolve()
	        };//SpellAbility

	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = 1469011418219527227L;

	  		public void showMessage()
	          {
	            ability.setStackDescription("Put " +card +" on top of its owner's library");

	            stopSetNext(new Input_PayManaCost(ability));
	          }
	        };
	        ability.setDescription("U: Put Wayward Soul on top of its owner's library.");
	        ability.setStackDescription("Put Wayward Soul on top of its owner's library.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************




	      //*************** START *********** START **************************
	      else if(cardName.equals("Coastal Hornclaw"))
	      {
	        //sacrifice ability - targets itself - until EOT
	        final Command untilEOT = new Command()
	        {
	          private static final long serialVersionUID = 7538741250040204529L;

	  		public void execute()
	          {
	            card.removeIntrinsicKeyword("Flying");
	          }
	        };

	        //mana tap ability
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI()
	          {
	            CardList land = new CardList(AllZone.Computer_Play.getCards());
	            land = land.getType("Land");

	            CardList canBlock = CardFactoryUtil.AI_getHumanCreature(card, true);
	            canBlock = canBlock.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isUntapped();
	              }
	            });

	            return (land.size() != 0)                       &&
	                   (! card.getKeyword().contains("Flying")) &&
	                   CardFactoryUtil.AI_getHumanCreature("Flying", card, false).isEmpty() &&
	                   (! card.hasSickness()) &&
	                   (AllZone.Phase.getPhase().equals(Constant.Phase.Main1));
	          }

	          public void chooseTargetAI()
	          {
	            CardList land = new CardList(AllZone.Computer_Play.getCards());
	            land = land.getType("Land");
	            land.shuffle();
	            AllZone.GameAction.sacrifice(land.get(0));
	          }

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              card.addIntrinsicKeyword("Flying");
	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility


	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = 4874019210748846864L;

	  		public void showMessage()
	          {
	            ability.setStackDescription(card +" gains flying until EOT.");

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            CardList choice = new CardList(play.getCards());
	            choice = choice.getType("Land");
	            stopSetNext(CardFactoryUtil.input_sacrifice(ability, choice, "Select a land to sacrifice."));
	          }
	        };
	        ability.setStackDescription(card +" gains flying until end of turn.");
	        ability.setDescription("Sacrifice a land: Coastal Hornclaw gains flying until end of turn.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************





	      //*************** START *********** START **************************
	      else if(cardName.equals("Spitting Spider"))
	      {
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI()
	          {
	            return false;
	          }
	          public void resolve()
	          {
	            CardList list = new CardList();
	            list.addAll(AllZone.Human_Play.getCards());
	            list.addAll(AllZone.Computer_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isCreature() && c.getKeyword().contains("Flying");
	              }
	            });

	            for(int i = 0; i < list.size(); i++)
	              if (CardFactoryUtil.canDamage(card, list.get(i)))
	            	list.get(i).addDamage(1, card);
	          }//resolve()
	        };//SpellAbility

	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = 2004031367305867525L;

	  		public void showMessage()
	          {

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            CardList choice = new CardList(play.getCards());
	            choice = choice.getType("Land");
	            stopSetNext(CardFactoryUtil.input_sacrifice(ability, choice, "Select a land to sacrifice."));
	          }
	        };
	        ability.setStackDescription(card +" deals 1 damage to each creature with flying.");
	        ability.setDescription("Sacrifice a land: Spitting Spider deals 1 damage to each creature with flying.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Killer Whale"))
	      {
	        final Ability ability = new Ability(card, "U")
	        {
	          public boolean canPlayAI()
	          {
	            return false;
	          }
	          public void resolve()
	          {
	            final Command untilEOT = new Command()
	            {
	              private static final long serialVersionUID = -8494294720368074013L;

	  			public void execute()
	              {
	                card.removeIntrinsicKeyword("Flying");
	              }
	            };

	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              card.addIntrinsicKeyword("Flying");
	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility

	        ability.setStackDescription(card +" gains flying until end of turn.");
	        ability.setDescription("U: Killer Whale gains flying until end of turn.");
	        card.addSpellAbility(ability);
	      }//*************** END ************ END **************************
	      
	  	//*************** START *********** START **************************
	      else if(cardName.equals("Obsidian Fireheart"))
	      {
	      	final Ability ability = new Ability(card, "1 R R")
	      	{
	  			public void resolve() {
	  				Card c = getTargetCard();
	  				
	  				if (AllZone.GameAction.isCardInPlay(c) && c.isLand() && (c.getCounters(Counters.BLAZE)==0) )
	  					c.addCounter(Counters.BLAZE, 1);
	  				
	  			}
	  			public boolean canPlayAI()
	  			{
	  				PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	  				CardList land = new CardList(play.getCards());
	  				land = land.filter(new CardListFilter()
	  				{
	  					public boolean addCard(Card c) {
	  						return c.isLand() && c.getCounters(Counters.BLAZE) < 1;
	  					}
	  				});
	  				
	  				if (land.size() > 0)
	  					setTargetCard(land.get(0));
	  				
	  				return land.size() > 0;
	  			}
	      		
	      	};
	      	
	      	ability.setDescription("1 R R: Put a blaze counter on target land without a blaze counter on it. For as long as that land has a blaze counter on it, it has \"At the beginning of your upkeep, this land deals 1 damage to you.\" (The land continues to burn after Obsidian Fireheart has left the battlefield.)");
	      	ability.setBeforePayMana(CardFactoryUtil.input_targetType(ability, "Land"));
	  		card.addSpellAbility(ability);
	      	
	      }// *************** END ************ END **************************

	  	//*************** START *********** START **************************
	      else if(cardName.equals("Glint-Eye Nephilim"))
	      {
	        final Command untilEOT = new Command()
	        {
	          private static final long serialVersionUID = 5790680475821014099L;

	  		public void execute()
	          {
	            card.addTempAttackBoost(-1);
	            card.addTempDefenseBoost(-1);
	          }
	        };

	        final Ability ability = new Ability(card, "1")
	        {
	          public boolean canPlayAI()
	          {
	            Card[] hand = AllZone.Computer_Hand.getCards();
	            return CardFactoryUtil.AI_doesCreatureAttack(card) && (hand.length != 0);
	          }
	          public void  chooseTargetAI() {AllZone.GameAction.discardRandom(Constant.Player.Computer);}

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card) )
	            {
	              card.addTempAttackBoost(1);
	              card.addTempDefenseBoost(1);

	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility


	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = -4302110760957471033L;

	  		public void showMessage()
	          {
	            ability.setStackDescription(card +" gets +1/+1 until EOT.");
	            //stopSetNext(CardFactoryUtil.input_sacrifice(ability, choice, "Select a card to discard."));
	            stopSetNext(CardFactoryUtil.input_discard(ability, 1));
	            
	          }
	        };
	        ability.setStackDescription(card +" gets +1/+1 until end of turn.");
	        ability.setDescription("1, Discard a card: Glint-Eye Nephilim gets +1/+1 until end of turn.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Wild Mongrel"))
	      {
	        //sacrifice ability - targets itself - until EOT
	        final Command untilEOT = new Command()
	        {
	          private static final long serialVersionUID = -5563743272875711445L;

	  		public void execute()
	          {
	            card.addTempAttackBoost(-1);
	            card.addTempDefenseBoost(-1);
	          }
	        };

	        //mana tap ability
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI()
	          {
	            Card[] hand = AllZone.Computer_Hand.getCards();
	            return CardFactoryUtil.AI_doesCreatureAttack(card) && (hand.length > 3);
	          }
	          public void  chooseTargetAI() {AllZone.GameAction.discardRandom(Constant.Player.Computer);}

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              card.addTempAttackBoost(1);
	              card.addTempDefenseBoost(1);

	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility


	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = -4209163355325441624L;

	  		public void showMessage()
	          {
	            ability.setStackDescription(card +" gets +1/+1 until EOT.");
	            //stopSetNext(CardFactoryUtil.input_sacrifice(ability, choice, "Select a card to discard."));
	            stopSetNext(CardFactoryUtil.input_discard(ability, 1));
	            //AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	          }
	        };
	        ability.setStackDescription(card +" gets +1/+1 until end of turn.");
	        ability.setDescription("Discard a card: Wild Mongrel gets +1/+1 until end of turn.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Hell-Bent Raider"))
	      {
	        final Command untilEOT = new Command()
	        {

	  		private static final long serialVersionUID = -2693050198371979012L;

	  		public void execute()
	          {
	            card.removeIntrinsicKeyword("Protection from white");
	          }
	        };

	        //mana tap ability
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI()
	          {
	            return false;
	          }

	          public void resolve()
	          {
	            AllZone.GameAction.discardRandom(card.getController());
	            if(AllZone.GameAction.isCardInPlay(card))
	            { 
	          	card.addIntrinsicKeyword("Protection from white");
	              
	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility


	        ability.setStackDescription(card +" gets Protection from white until end of turn.");
	        ability.setDescription("Discard a card at random: Hell-Bent Raider gets protection from white until end of turn.");
	        card.addSpellAbility(ability);
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Whiptongue Frog"))
	      {
	        //mana ability - targets itself - until EOT
	        final Command untilEOT = new Command()
	        {
	          private static final long serialVersionUID = -2693050198371979012L;

	  		public void execute()
	          {
	            card.removeIntrinsicKeyword("Flying");
	          }
	        };

	        //mana tap ability
	        final Ability ability = new Ability(card, "U")
	        {
	          public boolean canPlayAI()
	          {       
	            return (! card.hasSickness()) &&
	                   (! card.getKeyword().contains("Flying")) &&
	                   (AllZone.Phase.getPhase().equals(Constant.Phase.Main1));
	          }

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              card.addIntrinsicKeyword("Flying");
	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility


	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = 1268037036474796569L;

	  		public void showMessage()
	          {
	            ability.setStackDescription(card +" gains flying until EOT.");
	            stopSetNext(new Input_PayManaCost(ability));
	          }
	        };
	        ability.setStackDescription("Whiptongue Frog gains flying until EOT.");
	        ability.setDescription("U: Whiptongue Frog gains flying until end of turn.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Sarcomite Myr"))
	      {
	        //mana ability - targets itself - until EOT
	        final Command untilEOT = new Command()
	        {
	          private static final long serialVersionUID = -1726670429352834671L;

	  		public void execute()
	          {
	            card.removeIntrinsicKeyword("Flying");
	          }
	        };

	        //mana tap ability
	        final Ability ability = new Ability(card, "2")
	        {
	          public boolean canPlayAI()
	          {
	            return (! card.hasSickness()) &&
	                   (! card.getKeyword().contains("Flying")) &&
	                   (AllZone.Phase.getPhase().equals(Constant.Phase.Main1));
	          }

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              card.addIntrinsicKeyword("Flying");
	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility


	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = -685958984421033465L;

	  		public void showMessage()
	          {
	            ability.setStackDescription(card +" gains flying until EOT.");
	            stopSetNext(new Input_PayManaCost(ability));
	          }
	        };
	        ability.setStackDescription(card +" - gains flying until EOT.");
	        ability.setDescription("2: Sarcomite Myr gains flying until end of turn.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);

	        //ability 2
	        final Ability ability2 = new Ability(card, "2")
	        {
	          public boolean canPlayAI()
	          {
	            return false;
	          }

	          public void resolve()
	          {
	            AllZone.GameAction.drawCard(card.getController());
	          }//resolve()
	        };//SpellAbility

	        card.addSpellAbility(ability2);
	        ability2.setDescription("2, Sacrifice Sarcomite Myr: Draw a card.");
	        ability2.setStackDescription("Sarcomite Myr - draw a card");
	        ability2.setBeforePayMana(new Input_PayManaCost_Ability(ability2.getManaCost(), new Command()
	        {
	          private static final long serialVersionUID = -4357239016463815380L;

	  		public void execute()
	          {
	            AllZone.GameAction.sacrifice(card);
	            AllZone.Stack.add(ability2);
	          }
	        }));

	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Goblin Balloon Brigade"))
	      {
	        //mana ability - targets itself - until EOT
	        final Command untilEOT = new Command()
	        {
	          private static final long serialVersionUID = 3574792977266468264L;

	  		public void execute()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	              card.removeIntrinsicKeyword("Flying");
	          }
	        };

	        //mana tap ability
	        final Ability ability = new Ability(card, "R")
	        {
	          public boolean canPlayAI()
	          {
	            CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, false);
	            return flying.isEmpty()    &&
	                (! card.hasSickness()) &&
	                (! card.getKeyword().contains("Flying")) &&
	                (! CardFactoryUtil.AI_getHumanCreature(card, true).isEmpty());
	          }
	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              card.addIntrinsicKeyword("Flying");
	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility


	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = 7757347395564372034L;

	  		public void showMessage()
	          {
	            ability.setStackDescription(card +" gains flying until EOT.");
	            stopSetNext(new Input_PayManaCost(ability));
	          }
	        };
	        ability.setStackDescription("Goblin Balloon Brigade gains flying until EOT.");
	        ability.setDescription("R: Goblin Balloon Brigade gains flying until end of turn.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Manta Riders"))
	      {
	        //mana ability - targets itself - until EOT
	        final Command untilEOT = new Command()
	        {
	          private static final long serialVersionUID = -8732007889935536106L;

	  		public void execute()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	              card.removeIntrinsicKeyword("Flying");
	          }
	        };

	        //mana tap ability
	        final Ability ability = new Ability(card, "U")
	        {
	          public boolean canPlayAI()
	          {
	            CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, false);
	            return flying.isEmpty()    &&
	                (! card.hasSickness()) &&
	                (! card.getKeyword().contains("Flying")) &&
	                (! CardFactoryUtil.AI_getHumanCreature(card, true).isEmpty());
	          }
	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              card.addIntrinsicKeyword("Flying");
	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }//resolve()
	        };//SpellAbility


	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = -3889041123558196949L;

	  		public void showMessage()
	          {
	            ability.setStackDescription(card +" gains flying until EOT.");
	            stopSetNext(new Input_PayManaCost(ability));
	          }
	        };
	        ability.setStackDescription(card.getName() +" gains flying until EOT.");
	        ability.setDescription("U: Manta Riders gains flying until end of turn.");
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Turtleshell Changeling"))
	      {
	        //mana ability - targets itself - until EOT
	        //mana ability
	        final Ability ability = new Ability(card, "1 U")
	        {
	          public boolean canPlayAI()
	          {
	            return CardFactoryUtil.AI_doesCreatureAttack(card) && card.getNetAttack() == 1;
	          }
	          public void resolve()
	          {
	            //in case ability is played twice
	            final int[] oldAttack = new int[1];
	            final int[] oldDefense = new int[1];

	            oldAttack[0]  = card.getBaseAttack();
	            oldDefense[0] = card.getBaseDefense();

	            card.setBaseAttack(oldDefense[0]);
	            card.setBaseDefense(oldAttack[0]);

	            //EOT
	            final Command untilEOT = new Command()
	            {
	              private static final long serialVersionUID = -5494886974452901728L;

	  			public void execute()
	              {
	                card.setBaseAttack(oldAttack[0]);
	                card.setBaseDefense(oldDefense[0]);
	              }
	            };

	            AllZone.EndOfTurn.addUntil(untilEOT);
	          }//resolve()
	        };//SpellAbility


	        ability.setStackDescription(card +" - switch power and toughness until EOT.");
	        ability.setDescription("1 U: Switch Turtleshell Changeling's power and toughness until end of turn.");
	        card.addSpellAbility(ability);
	      }//*************** END ************ END **************************

	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Jugan, the Rising Star"))
	      {
	        final SpellAbility ability = new Ability(card, "0")
	        {
	          //should the computer play this card?
	          public boolean canPlayAI()
	          {
	            return (getComputerCreatures().size() != 0);
	          }
	          //set the target for the computer AI
	          public void chooseTargetAI()
	          {
	            CardList list = getComputerCreatures();

	            if(0 < list.size())
	              setTargetCard(list.get(0));
	            else
	              //the computer doesn't have any other creatures
	              setTargetCard(null);
	          }
	          CardList getComputerCreatures()
	          {
	            CardList list = new CardList(AllZone.Computer_Play.getCards());
	            CardList out = list.getType("Creature");
	            return out;
	          }//getCreatures
	          public void resolve()
	          {
	            Card c = getTargetCard();
	            if(c != null && AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) )
	            {
	              c.addCounter(Counters.P1P1, 5);
	            }
	          }//resolve()
	        };
	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));

	        Command leavesPlay = new Command()
	        {
	      	private static final long serialVersionUID = -2823505283781217181L;

	      	public void execute()
	          {
	            if(card.getController().equals(Constant.Player.Human))
	              AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
	            else if(ability.canPlayAI())
	            {
	              ability.chooseTargetAI();
	              //need to add this to the stack
	              AllZone.Stack.push(ability);
	            }


	          }//execute()
	        };//Command

	        card.addDestroyCommand(leavesPlay);
	      }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	      //destroy doesn't work
	      else if(cardName.equals("Sower of Temptation"))
	      {
	        final Card moveCreature[] = new Card[1];

	        final SpellAbility spell = new Spell_Permanent(card)
	        {
	          private static final long serialVersionUID = -6432280175218503718L;
	  		public boolean canPlayAI()
	          {
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
	          public void resolve()
	          {
	            super.resolve();

	            Card c = getTargetCard();
	            moveCreature[0] = c;

	            if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
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

	              c.setSickness(true);
	              c.setController(card.getController());

	              PlayerZone from = AllZone.getZone(c);
	              from.remove(c);

	              PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
	              to.add(c);

	              ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	              ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	            }
	          }//resolve()
	        };//SpellAbility
	        card.clearSpellAbility();
	        card.addSpellAbility(spell);

	        spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));

	        card.addLeavesPlayCommand(new Command()
	        {
	          private static final long serialVersionUID = -8961588142846220965L;

	  		public void execute()
	          {
	            Card c = moveCreature[0];

	            if(AllZone.GameAction.isCardInPlay(c))
	            {
	              ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
	              ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

	              c.setSickness(true);
	              c.setController(AllZone.GameAction.getOpponent(c.getController()));
	              
	              PlayerZone from = AllZone.getZone(c);
	              from.remove(c);

	              PlayerZone to = AllZone.getZone(Constant.Zone.Play, c.getOwner());
	              to.add(c);

	              ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	              ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	            }//if
	          }//execute()
	        });//Command
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Frostling"))
	      {
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI() {return getCreature().size() != 0;}

	          public void  chooseTargetAI()
	          {
	            CardList list = getCreature();
	            list.shuffle();
	            setTargetCard(list.get(0));

	            AllZone.GameAction.sacrifice(card);
	          }
	          CardList getCreature()
	          {
	            //toughness of 1
	            CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                //only get 1/1 flyers or 2/1 creatures
	                return (2 <= c.getNetAttack()) || c.getKeyword().contains("Flying");
	              }
	            });
	            return list;
	          }//getCreature()

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	              getTargetCard().addDamage(1, card);
	          }//resolve()
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("Sacrifice Frostling: Frostling deals 1 damage to target creature.");
	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability, new Command()
	        {
	          private static final long serialVersionUID = 3482118508536148313L;

	  		public void execute()
	          {
	            AllZone.GameAction.sacrifice(card);
	          }
	        }));
	      }//*************** END ************ END **************************
	      
	      
	      //*************** START *********** START **************************
	      else if(cardName.equals("Bottle Gnomes"))
	      {
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI() 
	          { 
	          	return AllZone.Computer_Life.getLife() < 3;
	          	
	          }
	          
	          public boolean canPlay()
	          {
	          	  SpellAbility sa;
	  	    	  for (int i=0; i<AllZone.Stack.size(); i++)
	  	    	  {
	  	    	       sa = AllZone.Stack.peek(i);
	  	    	       if (sa.getSourceCard().equals(card))
	  	    	             return false;
	  	    	  }
	  	    	  if (AllZone.GameAction.isCardInPlay(card) && super.canPlay())
	  	    		  return true;
	  	    	  else
	  	    		  return false;
	          }
	  		
	          public void resolve()
	          {
	          	PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	              life.addLife(3);
	              
	              AllZone.GameAction.sacrifice(card);
	          }//resolve()
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("Sacrifice Bottle Gnomes: Gain 3 life.");
	        ability.setStackDescription(card.getName() + " - " +card.getController() + " gains 3 life.");

	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Mogg Fanatic"))
	      {
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI() {return getCreature().size() != 0;}

	          public void chooseTargetAI()
	          {
	            if(AllZone.Human_Life.getLife() < 3)
	              setTargetPlayer(Constant.Player.Human);
	            else
	            {
	              CardList list = getCreature();
	              list.shuffle();
	              setTargetCard(list.get(0));
	            }
	            AllZone.GameAction.sacrifice(card);
	          }//chooseTargetAI()
	          CardList getCreature()
	          {
	            //toughness of 1
	            CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                //only get 1/1 flyers or 2/1 creatures
	                return (2 <= c.getNetAttack()) || c.getKeyword().contains("Flying");
	              }
	            });
	            return list;
	          }//getCreature()

	          public void resolve()
	          {
	            if(getTargetCard() != null)
	            {
	              if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	                getTargetCard().addDamage(1, card);
	            }
	            else
	              AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(1);
	          }//resolve()
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("Sacrifice Mogg Fanatic: Mogg Fanatic deals 1 damage to target creature or player.");
	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, new Command()
	        {
	          private static final long serialVersionUID = 8283052965865884779L;

	  		public void execute()
	          {
	            AllZone.GameAction.sacrifice(card);
	          }
	        }, true));
	      }//*************** END ************ END **************************

	      //*************** START *********** START **************************
	      else if(cardName.equals("Goblin Skycutter"))
	      {
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI() {return getFlying().size() != 0;}

	          public void chooseTargetAI()
	          {
	            AllZone.GameAction.sacrifice(card);

	            CardList flying = getFlying();
	            flying.shuffle();
	            setTargetCard(flying.get(0));
	          }

	          CardList getFlying()
	          {
	            CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
	            flying = flying.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.getNetDefense() == 2;
	              }
	            });
	            return flying;
	          }//getFlying()

	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              getTargetCard().addDamage(2, card);
	              getTargetCard().removeIntrinsicKeyword("Flying");
	              getTargetCard().removeExtrinsicKeyword("Flying");
	            }

	            @SuppressWarnings("unused") // target
	  		  final Card[] target = {getTargetCard()};
	            AllZone.EndOfTurn.addUntil(new Command()
	            {
	              private static final long serialVersionUID = -8889549737746466810L;

	  			public void execute()
	              {
	                if(AllZone.GameAction.isCardInPlay(getTargetCard()))
	                  getTargetCard().addIntrinsicKeyword("Flying");
	              }
	            });
	          }//resolve()
	        };//SpellAbility

	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = 8609211991425118222L;

	  		public void showMessage()
	          {
	            CardList list = new CardList();
	            list.addAll(AllZone.Human_Play.getCards());
	            list.addAll(AllZone.Computer_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isCreature() && c.getKeyword().contains("Flying") && CardFactoryUtil.canTarget(card, c);
	              }
	            });

	            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list, "Select a creature with flying to deal 2 damage to", new Command()
	            {
	              private static final long serialVersionUID = -3287971244881855563L;

	  			public void execute()
	              {
	                AllZone.GameAction.sacrifice(card);
	              }
	            }, true));
	          }//showMessage()
	        };//Input

	        card.addSpellAbility(ability);
	        ability.setDescription("Sacrifice Goblin Skycutter: Goblin Skycutter deals 2 damage to target creature with flying. That creature loses flying until end of turn.");
	        ability.setBeforePayMana(runtime);
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Sakura-Tribe Elder"))
	      {
	        //tap sacrifice
	        final SpellAbility ability = new Ability_Activated(card, "0")
	        {
	   		private static final long serialVersionUID = 1135117614484689768L;
	  		public boolean canPlayAI()
	          {
	            //sacrifice Sakura-Tribe Elder if Human has any creatures
	            CardList list = new CardList(AllZone.Human_Play.getCards());
	            list = list.getType("Creature");
	            return list.size() != 0;
	          }
	          public void chooseTargetAI()
	          {
	            AllZone.GameAction.sacrifice(card);
	          }
	          public boolean canPlay()
	          {
	            PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	            CardList list = new CardList(library.getCards());
	            list = list.getType("Basic");
	            return list.size() > 0 && super.canPlay();
	          }//canPlay()
	          public void resolve()
	          {
	            if(card.getOwner().equals(Constant.Player.Human))
	              humanResolve();
	            else
	              computerResolve();
	          }
	          public void computerResolve()
	          {
	            CardList play = new CardList(AllZone.Computer_Play.getCards());
	            play = play.getType("Basic");

	            CardList library = new CardList(AllZone.Computer_Library.getCards());
	            library = library.getType("Basic");

	            //this shouldn't happen, but it is defensive programming, haha
	            if(library.isEmpty())
	              return;

	            Card land = null;

	            //try to find a basic land that isn't in play
	            for(int i = 0; i < library.size(); i++)
	              if(! play.containsName(library.get(i)))
	              {
	                land = library.get(i);
	                break;
	              }

	            //if not found
	            //library will have at least 1 basic land because canPlay() checks that
	            if(land == null)
	              land = library.get(0);

	            land.tap();
	            AllZone.Computer_Library.remove(land);
	            AllZone.Computer_Play.add(land);

	            AllZone.GameAction.shuffle(Constant.Player.Computer);
	          }//computerResolve()
	          public void humanResolve()
	          {
	            PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	            PlayerZone play    = AllZone.getZone(Constant.Zone.Play   , card.getController());

	            CardList basicLand = new CardList(library.getCards());
	            basicLand = basicLand.getType("Basic");
	            if(basicLand.isEmpty())
	              return;

	            Object o = AllZone.Display.getChoiceOptional("Choose a basic land", basicLand.toArray());
	            if(o != null)
	            {
	              Card land = (Card)o;
	              land.tap();

	              library.remove(land);
	              play.add(land);
	            }

	            AllZone.GameAction.shuffle(card.getController());
	          }//resolve()
	        };//SpellAbility

	        Input runtime = new Input()
	        {
	          private static final long serialVersionUID = 1959709104655340395L;
	  		boolean once = true;
	          public void showMessage()
	          {
	            //this is necessary in order not to have a StackOverflowException
	            //because this updates a card, it creates a circular loop of observers
	            if(once)
	            {
	              once = false;
	              AllZone.GameAction.sacrifice(card);
	              AllZone.Stack.add(ability);

	              stop();
	            }
	          }//showMessage()
	        };
	        card.addSpellAbility(ability);
	        ability.setBeforePayMana(runtime);
	        ability.setDescription("Sacrifice Sakura-Tribe Elder: Search your library for a basic land card, put that card into play tapped, then shuffle your library.");
	        ability.setStackDescription("Search your library for a basic land card, put that card into play tapped, then shuffle your library.");
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Transluminant"))
	      {
	        final Command atEOT = new Command()
	        {
	          private static final long serialVersionUID = -5126793112740563180L;

	  		public void execute()
	          {
	            Card c = new Card();

	            c.setOwner(card.getController());
	            c.setController(card.getController());
	            
	            c.setName("Spirit");
	            c.setImageName("W 1 1 Spirit");
	            c.setManaCost("W");
	            c.setToken(true);

	            c.addType("Creature");
	            c.addType("Spirit");
	            c.setBaseAttack(1);
	            c.setBaseDefense(1);
	            c.addIntrinsicKeyword("Flying");

	            PlayerZone play    = AllZone.getZone(Constant.Zone.Play   , card.getController());
	            play.add(c);
	          }//execute()
	        };//Command

	        final Ability ability = new Ability(card, "W")
	        {
	          public boolean canPlayAI()
	          { /*
	            CardList list = new CardList(AllZone.Human_Play.getCards());
	            list = list.getType("Creature");

	            String phase = AllZone.Phase.getPhase();
	            return phase.equals(Constant.Phase.Main2) && list.size() != 0;
	            */
	            return false;
	          }
	          public void chooseTargetAI()
	          {
	            AllZone.GameAction.sacrifice(card);
	          }

	          public void resolve()
	          {
	            AllZone.EndOfTurn.addAt(atEOT);
	          }//resolve()
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("W, Sacrifice Transluminant: Put a 1/1 white Spirit creature token with flying into play at end of turn.");
	        ability.setStackDescription("Put a 1/1 white Spirit creature token with flying into play at end of turn.");
	        ability.setBeforePayMana(new Input_PayManaCost_Ability(ability.getManaCost(), new Command()
	        {
	          private static final long serialVersionUID = -6553009833190713980L;

	  		public void execute()
	          {
	            AllZone.GameAction.sacrifice(card);
	            AllZone.Stack.add(ability);
	          }
	        }));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Llanowar Behemoth"))
	      {
	        final Ability ability = new Ability(card, "0")
	        {
	          public boolean canPlayAI()
	          {
	            return (getUntapped().size() != 0) && CardFactoryUtil.AI_doesCreatureAttack(card);
	          }
	          public void chooseTargetAI()
	          {
	            Card c = getUntapped().get(0);
	            c.tap();
	            setTargetCard(c);
	          }
	          CardList getUntapped()
	          {
	            CardList list = new CardList(AllZone.Computer_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isCreature() && c.isUntapped() && (!CardFactoryUtil.AI_doesCreatureAttack(c));
	              }
	            });
	            return list;
	          }//getUntapped()

	          public void resolve()
	          {
	            card.addTempAttackBoost(1);
	            card.addTempDefenseBoost(1);

	            Command untilEOT = new Command()
	            {
	              private static final long serialVersionUID = 6445782721494547172L;

	  			public void execute()
	              {
	                card.addTempAttackBoost(-1);
	                card.addTempDefenseBoost(-1);
	              }//execute()
	            };//Command

	            AllZone.EndOfTurn.addUntil(untilEOT);
	          }//resolve()
	        };//SpellAbility

	        Input target = new Input()
	        {
	          private static final long serialVersionUID = 7721637420366357272L;
	  		public void showMessage()
	          {
	            AllZone.Display.showMessage("Select an untapped creature you control");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card c, PlayerZone zone)
	          {
	            if(c.isCreature() && zone.is(Constant.Zone.Play, card.getController()) && c.isUntapped())
	            {
	              ability.setStackDescription(card +" gets +1/+1 until end of turn.");
	              c.tap();
	              AllZone.Stack.add(ability);
	              stop();
	            }
	          }
	        };//Input
	        ability.setBeforePayMana(target);
	        ability.setDescription("Tap an untapped creature you control: Llanowar Behemoth gets +1/+1 until end of turn.");

	        card.addSpellAbility(ability);
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Memnarch"))
	      {
	        //has 2 non-tap abilities that effects itself
	        final SpellAbility ability1 = new Ability(card, "1 U U")
	        {
	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              getTargetCard().addType("Artifact");
	            }
	          }//resolve()
	          public boolean canPlayAI()
	          {
	            CardList list = getCreature();
	            return list.size() != 0;
	          }
	          public void chooseTargetAI()
	          {
	            Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	            setTargetCard(target);
	          }//chooseTargetAI()
	          CardList getCreature()
	          {
	            CardList list = new CardList(AllZone.Human_Play.getCards());
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isCreature() && (!c.isArtifact()) && CardFactoryUtil.canTarget(card, c);
	              }
	            });
	            return list;
	          }//getCreature()
	        };//SpellAbility

	        //**** start of ability2
	        final SpellAbility ability2 = new Ability(card, "3 U")
	        {
	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	            {
	              
	              ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
		          ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
	              
		          //gain control of target artifact
	              PlayerZone from = AllZone.getZone(Constant.Zone.Play, getTargetCard().getController());
	              from.remove(getTargetCard());


	              getTargetCard().setController(card.getController());

	              PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
	              to.add(getTargetCard());
	              to.setUpdate(true);

	              ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	              ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);


//	              AllZone.GameAction.moveTo(play, getTargetCard());

	              //TODO: controller probably is not set correctly
	              //TODO: when you take control, the creature looses type "Artifact" since
	              //      GameAction.moveTo() makes a new card object
	            }
	          }//resolve()
	          public boolean canPlayAI()
	          {
	            CardList list = getArtifactCreatures();
	            return list.size() != 0;
	          }
	          public void chooseTargetAI()
	          {
	            CardList list = getArtifactCreatures();
	            Card target = CardFactoryUtil.AI_getBestCreature(list);
	            if(target == null)
	              target = AllZone.Human_Play.get(0);

	            setTargetCard(target);
	          }
	          CardList getArtifactCreatures()
	          {
	            CardList list = new CardList(AllZone.Human_Play.getCards());
	            list = list.filter(new CardListFilter(){
	            	public boolean addCard(Card c)
	            	{
	            		return c.isCreature() && c.isArtifact() && CardFactoryUtil.canTarget(card, c);
	            	}
	            });
	            return list;
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability1);
	        card.addSpellAbility(ability2);

	        ability1.setDescription("1 U U: Target permanent becomes an artifact in addition to its other types. (This effect doesn't end at end of turn.)");
	        ability2.setDescription("3 U: Gain control of target artifact. (This effect doesn't end at end of turn.)");

	        ability1.setBeforePayMana(CardFactoryUtil.input_targetType(ability1, "All"));
	        ability2.setBeforePayMana(CardFactoryUtil.input_targetType(ability2, "Artifact"));
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Meloku the Clouded Mirror"))
	      {
	        final SpellAbility ability = new Ability(card, "1")
	        {
	          public void resolve()
	          {
	            Card c = new Card();
	            c.setToken(true);
	            c.setImageName("U 1 1 Illusion");
	            c.setName("Illusion");

	            c.setManaCost("U");
	            c.setBaseAttack(1);
	            c.setBaseDefense(1);
	            c.addIntrinsicKeyword("Flying");
	            c.addType("Creature");
	            c.addType("Illusion");
	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            play.add(c);

	            //TODO: the "bounced" land should be chosen by the user
	            //instead of "automatically" done for him
	            CardList island = new CardList(play.getCards());
	            island = island.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.getType().contains("Land") && c.isTapped();
	              }
	            });
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());

	            if(! island.isEmpty())
	              AllZone.GameAction.moveTo(hand, island.get(0));
	          }//resolve()

	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("1, Return a land you control to its owner's hand: Put a 1/1 blue Illusion creature token with flying into play.");
	        ability.setStackDescription("Put 1/1 token with flying into play");
	        ability.setBeforePayMana(new Input_PayManaCost(ability));
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Squall Drifter"))
	      {
	        final SpellAbility ability = new Ability_Tap(card, "W")
	        {
	  		private static final long serialVersionUID = 3862705592994953819L;
	  		public void resolve()
	          {
	            Card c = getTargetCard();
	            c.tap();
	          }
	          public boolean canPlayAI() {return false;}
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("W, tap: Tap target creature.");

	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
	      }//*************** END ************ END **************************

	      

	      //*************** START *********** START **************************
	      else if(cardName.equals("Bonded Fetch") || cardName.equals("Merfolk Looter"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	  		private static final long serialVersionUID = 3959966663907905001L;
	  		public boolean canPlayAI() {return false;}
	          public void resolve()
	          {
	            AllZone.GameAction.drawCard(card.getController());
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Draw a card, then discard a card.");
	        ability.setStackDescription("Bonded Fetch - draw a card, then discard a card.");
	        ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Hammerfist Giant"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	  		private static final long serialVersionUID = 1089840397064226840L;
	  		
	  		public boolean canPlayAI()
	          {
	            CardList list = CardFactoryUtil.AI_getHumanCreature(4, card, true);
	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c) {return ! c.getKeyword().contains("Flying");}
	            });

	            return list.size() > 3 && 6 < AllZone.Computer_Life.getLife();
	          }//canPlayAI()
	          public void resolve()
	          {
	            //get all creatures
	            CardList list = new CardList();
	            list.addAll(AllZone.Human_Play.getCards());
	            list.addAll(AllZone.Computer_Play.getCards());
	            list = list.getType("Creature");

	            list = list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return ! c.getKeyword().contains("Flying") && CardFactoryUtil.canDamage(card, c);
	              }
	            });
	            for(int i = 0; i < list.size(); i++)
	              list.get(i).addDamage(4, card);

	            AllZone.Human_Life.subtractLife(4);
	            AllZone.Computer_Life.subtractLife(4);
	          }//resolve()
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Hammerfist Giant deals 4 damage to each creature without flying and each player.");
	        ability.setStackDescription("Hammerfist Giant - deals 4 damage to each creature without flying and each player.");
	        ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Urborg Syphon-Mage"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card, "2 B")
	        {
	  		private static final long serialVersionUID = -1965170715774280112L;
	  		
	  		public void resolve()
	          {
	            String opponent = AllZone.GameAction.getOpponent(card.getController());
	            AllZone.GameAction.getPlayerLife(opponent).subtractLife(2);

	            AllZone.GameAction.getPlayerLife(card.getController()).addLife(2);

	            //computer discards here, todo: should discard when ability put on stack
	            if(card.getController().equals(Constant.Player.Computer))
	              AllZone.GameAction.discardRandom(Constant.Player.Computer);
	          }
	          public boolean canPlay()
	          {
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	            return super.canPlay() && hand.getCards().length != 0;
	          }
	          public boolean canPlayAI()
	          {
	            int life = AllZone.Human_Life.getLife();
	            Card[] hand = AllZone.Computer_Hand.getCards();
	            return ((life < 11) || (5 < AllZone.Phase.getTurn())) &&
	                   hand.length > 0;
	          }
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("2B, tap, Discard a card: Each other player loses 2 life. You gain life equal to the life lost this way.");
	        ability.setStackDescription("Urborg Syphon-Mage - Opponent loses 2 life, and you gain 2 life");
	        ability.setBeforePayMana(new Input_PayManaCost_Ability("2 B", new Command()
	        {
	  		private static final long serialVersionUID = 1186455545951390853L;

	  		public void execute()
	          {
	            card.tap();
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	            AllZone.Stack.add(ability);
	          }
	        }));
	      }//*************** END ************ END **************************

	      
	    //*************** START *********** START **************************
	      if (cardName.equals("Stangg")){
	      	
	      	final Ability ability = new Ability(card, "0")
	  		{
	  			public void resolve()
	  			{
	  				Card c = new Card();

	  				c.setName("Stangg Twin");
	  				c.setImageName("RG 3 4 Stangg Twin");
	  				
	  				c.setOwner(card.getController());
	  				c.setController(card.getController());

	  				c.setManaCost("R G");
	  				c.setToken(true);

	  				c.addType("Legendary");
	  				c.addType("Creature");
	  				c.addType("Human");
	  				c.addType("Warrior");
	  				c.setBaseAttack(3);
	  				c.setBaseDefense(4);
	  				
	  				c.addLeavesPlayCommand(new Command()
	  				{
	  					private static final long serialVersionUID = 3367390368512271319L;

	  					public void execute() {
	  						if (AllZone.GameAction.isCardInPlay(card))
	  							AllZone.GameAction.sacrifice(card);
	  					}
	  					
	  				});

	  				PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	  				play.add(c);
	  				
	  			}
	  		};
	  		ability.setStackDescription("When Stangg enters the battlefield, if Stangg is on the battlefield, put a legendary 3/4 red and green Human Warrior creature token named Stangg Twin onto the battlefield.");
	  		
	  		card.addComesIntoPlayCommand(new Command()
	  		{
	  			private static final long serialVersionUID = 6667896040611028600L;

	  			public void execute() {
	  				AllZone.Stack.add(ability);
	  			}			
	  		});
	  		
	  		card.addLeavesPlayCommand(new Command()
	  		{
	  			private static final long serialVersionUID = 1786900359843939456L;

	  			public void execute() {
	  				CardList list = new CardList();
	  				list.addAll(AllZone.Computer_Play.getCards());
	  				list.addAll(AllZone.Human_Play.getCards());
	  				list = list.getName("Stangg Twin");
	  				
	  				if (list.size() == 1)
	  					AllZone.GameAction.removeFromGame(list.get(0));
	  			}	
	  		});
	      	
	      }//*************** END ************ END **************************

	      //*************** START *********** START **************************
	      else if(cardName.equals("Llanowar Mentor"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card, "G")
	        {
	  		private static final long serialVersionUID = 752280918226277729L;

	  		public void resolve()
	          {
	            makeToken();

	            //computer discards here, todo: should discard when ability put on stack
	            if(card.getController().equals(Constant.Player.Computer))
	              AllZone.GameAction.discardRandom(Constant.Player.Computer);
	          }
	          void makeToken()
	          {
	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            Card c = new Card();

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setManaCost("G");
	            c.setToken(true);

	            c.addType("Creature");
	            c.addType("Elf");
	            c.addType("Druid");
	            c.setBaseAttack(1);
	            c.setBaseDefense(1);


	            //custom settings
	            c.setName("Llanowar Elves");
	            c.setImageName("G 1 1 Llanowar Elves");
	            //c.addIntrinsicKeyword("tap: add G");
	            c.addSpellAbility(new Ability_Mana(card, "tap: add G")
	  		  {
	  			private static final long serialVersionUID = 7871036527184588884L;  
	  		  });


	            play.add(c);
	          }//makeToken()

	          public boolean canPlay()
	          {
	            Card c[] = AllZone.getZone(Constant.Zone.Hand, card.getController()).getCards();

	            return super.canPlay() && (0 < c.length);
	          }

	          public boolean canPlayAI()
	          {
	            boolean canDiscard =  0 < AllZone.Computer_Hand.getCards().length;
	            return canPlay() && canDiscard && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
	          }
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("G, tap, Discard a card: Put a 1/1 green Elf Druid creature token named Llanowar Elves into play with \"tap: add G\" ");
	        ability.setStackDescription("Llanowar Mentor - Put a 1/1 token into play");
	        ability.setBeforePayMana(new Input_PayManaCost_Ability("G", new Command()
	        {
	  		private static final long serialVersionUID = -8140640118045101485L;

	  		public void execute()
	          {
	            card.tap();
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	            AllZone.Stack.add(ability);
	          }
	        }));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Sparkspitter"))
	      {
	    	
	    	  
	        final Ability_Tap ability = new Ability_Tap(card, "R")
	        {
	  		private static final long serialVersionUID = -6381252527344512333L;

	  		public void resolve()
	          {
	            makeToken();

	            //computer discards here, todo: should discard when ability put on stack
	            if(card.getController().equals(Constant.Player.Computer))
	              AllZone.GameAction.discardRandom(Constant.Player.Computer);
	          }
	          void makeToken()
	          {
	            
	        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		          final Card c = new Card();

		          c.setOwner(card.getController());
		          c.setController(card.getController());

		          c.setManaCost("R");
		          c.setToken(true);

		          c.addType("Creature");
		          c.addType("Elemental");
		          c.setBaseAttack(3);
		          c.setBaseDefense(1);

		          //custom settings
		          c.setName("Spark Elemental");
		          c.setImageName("R 3 1 Spark Elemental");
		          
		          
		          final SpellAbility spell = new Ability(card, "0")
				    {
				        public void resolve()
				        {
				          if(AllZone.GameAction.isCardInPlay(c))
				            AllZone.GameAction.sacrifice(c);
				        }
				    };
				    spell.setStackDescription("Sacrifice " +c);
		          
		          final Command destroy = new Command()
			      {

					private static final long serialVersionUID = -8633713067929006933L;

					public void execute()
			        {
			          if(AllZone.GameAction.isCardInPlay(c))
			            AllZone.Stack.add(spell);
			        }
			      };

			      Command intoPlay = new Command()
			      {

					private static final long serialVersionUID = -7174114937547707480L;

					public void execute()
			        {
			          if(!c.isFaceDown())
			        	  AllZone.EndOfTurn.addAt(destroy);
			        }
			      };
			      c.addComesIntoPlayCommand(intoPlay);
			      c.setSacrificeAtEOT(true);
		          
		          play.add(c);
		         
	          }//makeToken()

	          public boolean canPlay()
	          {
	            Card c[] = AllZone.getZone(Constant.Zone.Hand, card.getController()).getCards();

	            return super.canPlay() && (0 < c.length);
	          }

	          public boolean canPlayAI()
	          {
	            boolean canDiscard =  0 < AllZone.Computer_Hand.getCards().length;
	            return canPlay() && canDiscard;
	          }
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("R, tap, Discard a card: Put a 3/1 red Elemental creature token named Spark Elemental into play with trample, haste, and \"At end of turn, sacrifice Spark Elemental.\" ");
	        ability.setStackDescription("Sparkspitter - Put a 3/1 token into play");
	        ability.setBeforePayMana(new Input_PayManaCost_Ability("R", new Command()
	        {
	  		private static final long serialVersionUID = 4717424466422508064L;

	  		public void execute()
	          {
	            card.tap();
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	            AllZone.Stack.add(ability);
	          }
	        }));
	      }//*************** END ************ END **************************



	      //*************** START *********** START **************************
	      else if(cardName.equals("Sliversmith"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card, "1")
	        {
	  		private static final long serialVersionUID = -901356795848120643L;

	  		public void resolve()
	          {
	            makeToken();

	            //computer discards here, todo: should discard when ability put on stack
	            if(card.getController().equals(Constant.Player.Computer))
	              AllZone.GameAction.discardRandom(Constant.Player.Computer);
	          }
	          void makeToken()
	          {
	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            Card c = new Card();

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setManaCost("1");
	            c.setToken(true);
	            
	            c.addType("Artifact");
	            c.addType("Creature");
	            c.addType("Sliver");
	            c.setBaseAttack(1);
	            c.setBaseDefense(1);


	            //custom settings
	            c.setName("Metallic Sliver");
	            c.setImageName("C 1 1 Metallic Sliver");

	            play.add(c);
	          }//makeToken()

	          public boolean canPlay()
	          {
	            Card c[] = AllZone.getZone(Constant.Zone.Hand, card.getController()).getCards();

	            return super.canPlay() && (0 < c.length);
	          }

	          public boolean canPlayAI()
	          {
	            boolean canDiscard =  0 < AllZone.Computer_Hand.getCards().length;
	            return canPlay() && canDiscard && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
	          }
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("1, tap, Discard a card: Put a 1/1 Sliver artifact creature token named Metallic Sliver into play.");
	        ability.setStackDescription(card +" - Put a 1/1 token into play");
	        ability.setBeforePayMana(new Input_PayManaCost_Ability("1", new Command()
	        {
	  		private static final long serialVersionUID = 7980998398222481323L;

	  		public void execute()
	          {
	            card.tap();
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	            AllZone.Stack.add(ability);
	          }
	        }));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Cloudseeder"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card, "U")
	        {
	  		private static final long serialVersionUID = -4685908556244137496L;

	  		public void resolve()
	          {
	            makeToken();

	            //computer discards here, todo: should discard when ability put on stack
	            if(card.getController().equals(Constant.Player.Computer))
	              AllZone.GameAction.discardRandom(Constant.Player.Computer);
	          }
	          void makeToken()
	          {
	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            Card c = new Card();

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setManaCost("U");
	            c.setToken(true);

	            c.addType("Creature");
	            c.addType("Faerie");
	            c.setBaseAttack(1);
	            c.setBaseDefense(1);


	            //custom settings
	            c.setName("Cloud Sprite");
	            c.setImageName("U 1 1 Cloud Sprite");
	            c.addIntrinsicKeyword("Flying");
	            c.addIntrinsicKeyword("This creature can block only creatures with flying");

	            play.add(c);
	          }//makeToken()

	          public boolean canPlay()
	          {
	            Card c[] = AllZone.getZone(Constant.Zone.Hand, card.getController()).getCards();

	            return super.canPlay() && (0 < c.length);
	          }

	          public boolean canPlayAI()
	          {
	            boolean canDiscard =  0 < AllZone.Computer_Hand.getCards().length;
	            return canPlay() && canDiscard && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
	          }
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("U, tap, Discard a card: Put a 1/1 blue Sprite creature token named Cloud Sprite into play with flying and \"this creature can block only creatures with flying\".");
	        ability.setStackDescription("Cloudseeker - Put a 1/1 token into play");
	        ability.setBeforePayMana(new Input_PayManaCost_Ability("U", new Command()
	        {
	  		private static final long serialVersionUID = 7707504858274558816L;

	  		public void execute()
	          {
	            card.tap();
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	            AllZone.Stack.add(ability);
	          }
	        }));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Goldmeadow Lookout"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card, "G")
	        {
	  		private static final long serialVersionUID = -8413409735529340094L;

	  		public void resolve()
	          {
	            makeToken();

	            //computer discards here, todo: should discard when ability put on stack
	            if(card.getController().equals(Constant.Player.Computer))
	              AllZone.GameAction.discardRandom(Constant.Player.Computer);
	          }
	          void makeToken()
	          {
	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            Card c = new Card();

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setManaCost("W");
	            c.setToken(true);

	            c.addType("Creature");
	            c.addType("Kithkin");
	            c.addType("Soldier");
	            c.setBaseAttack(1);
	            c.setBaseDefense(1);


	            //custom settings
	            c.setName("Goldmeadow Harrier");
	            c.setImageName("W 1 1 Goldmeadow Harrier");
	            final SpellAbility ability = new Ability_Tap(c, "W")
	            {
	  			private static final long serialVersionUID = -7327585136675896817L;
	  			
	  			public void resolve()
	              {
	                Card c = getTargetCard();
	                c.tap();
	              }
	              public boolean canPlayAI() {return false;}
	            };//SpellAbility
	            c.addSpellAbility(new Spell_Permanent(c));
	            c.addSpellAbility(ability);
	            ability.setDescription("W, tap: Tap target creature.");
	            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));


	            play.add(c);
	          }//makeToken()

	          public boolean canPlay()
	          {
	            Card c[] = AllZone.getZone(Constant.Zone.Hand, card.getController()).getCards();

	            return super.canPlay() && (0 < c.length);
	          }

	          public boolean canPlayAI()
	          {
	            boolean canDiscard =  0 < AllZone.Computer_Hand.getCards().length;
	            return canPlay() && canDiscard && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
	          }
	        };//SpellAbility

	        card.addSpellAbility(ability);
	        ability.setDescription("W, tap, Discard a card: Put a 1/1 white Kithkin Soldier creature token named Goldmeadow Harrier into play with \"W, tap target creature.\"");
	        ability.setStackDescription("Goldmeadow Lookout - Put a 1/1 token into play");
	        ability.setBeforePayMana(new Input_PayManaCost_Ability("W", new Command()
	        {
	  		private static final long serialVersionUID = 8621733943286161557L;

	  		public void execute()
	          {
	            card.tap();
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	            AllZone.Stack.add(ability);
	          }
	        }));
	      }//*************** END ************ END **************************

	      //*************** START *********** START **************************
	      if (cardName.equals("Goldmeadow Harrier") || cardName.equals("Loxodon Mystic") 
	      	|| cardName.equals("Master Decoy") || cardName.equals("Benalish Trapper") || cardName.equals("Whipcorder")
	      	|| cardName.equals("Blinding Mage") || cardName.equals("Ostiary Thrull"))
	      {
	      	final SpellAbility ability = new Ability_Tap(card, "W")
	          {

	  		   private static final long serialVersionUID = 4424848120984319655L;
	  		   public void resolve()
	             {
	               Card c = getTargetCard();
	               c.tap();
	             }
	             public boolean canPlayAI() {return false;}
	          };//SpellAbility
	          card.addSpellAbility(ability);
	          ability.setDescription("W, tap: Tap target creature.");
	          ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));

	    	}//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	      if (cardName.equals("Rathi Trapper"))
	      {
	      	final SpellAbility ability = new Ability_Tap(card, "B")
	          {

	  		   private static final long serialVersionUID = 4424848120984319655L;
	  		   public void resolve()
	             {
	               Card c = getTargetCard();
	               c.tap();
	             }
	             public boolean canPlayAI() {return false;}
	          };//SpellAbility
	          card.addSpellAbility(ability);
	          ability.setDescription("W, tap: Tap target creature.");
	          ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));

	    	}//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	      if (cardName.equals("Minister of Impediments") ||  cardName.equals("Ballynock Trapper"))
	      {
	      	final SpellAbility ability = new Ability_Tap(card, "0")
	          {

	  		   private static final long serialVersionUID = 4424848120984319655L;
	  		   public void resolve()
	             {
	               Card c = getTargetCard();
	               c.tap();
	             }
	             public boolean canPlayAI() {return false;}
	          };//SpellAbility
	          card.addSpellAbility(ability);
	          ability.setDescription("tap: Tap target creature.");
	          ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));

	    	}//*************** END ************ END **************************

	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Silent Attendant"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	   
	  		private static final long serialVersionUID = -2602340080268657851L;
	  		public void resolve()
	          {
	           
	            AllZone.GameAction.getPlayerLife(card.getController()).addLife(1);
	          }
	          public boolean canPlayAI()
	          {
	            //computer should play ability if this creature doesn't attack
	            Combat c = ComputerUtil.getAttackers();
	            CardList list = new CardList(c.getAttackers());

	            //could this creature attack?, if attacks, do not use ability
	            return (! list.contains(card));
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: You gain 1 life.");
	        ability.setStackDescription(card.getName() + " -  you gain 1 life.");
	        ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
	      }//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	      else if(cardName.equals("Ghost-Lit Redeemer"))
	      {
	        final SpellAbility ability = new Ability_Tap(card, "W")
	        {
	  		private static final long serialVersionUID = -7119153679100849498L;

	  		public boolean canPlayAI() {return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);}

	          public void resolve()
	          {
	            Card c = card;
	            PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	            life.addLife(2);
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("W, tap: You gain 2 life");
	        ability.setStackDescription("Computer gains 2 life");

	        final Command paid  = new Command()
	        {
	  		private static final long serialVersionUID = 3649597692883194760L;

	  		public void execute()
	          {
	            Card c = card;
	            c.tap();
	            AllZone.Human_Play.updateObservers();//so the card will tap at the correct time

	            ability.setStackDescription(c.getController()+" gains 2 life");
	            AllZone.Stack.add(ability);
	            AllZone.InputControl.resetInput();
	          }
	        };
	        ability.setBeforePayMana(new Input_PayManaCost_Ability(ability.getManaCost(), paid, Command.Blank));
	      }//*************** END ************ END **************************

	    //*************** START *********** START **************************
	      else if(cardName.equals("Ramosian Revivalist"))
	      {
	      	@SuppressWarnings("unused") // costMinusOne
	  		String costMinusOne = "";
	  	    int a = Integer.parseInt("6");
	  	    a--;
	  	    costMinusOne = Integer.toString(a);
	  	    final int converted = a;
	  	    final String player = card.getController();
	  	    
	  	    final SpellAbility ability = new Ability_Tap(card, "6")
	  		  {
	  			private static final long serialVersionUID = 2675327938055139432L;

	  			public boolean canPlay()
	  		      {
	  		    	  SpellAbility sa;
	  		    	  for (int i=0; i<AllZone.Stack.size(); i++)
	  		    	  {
	  		    	       sa = AllZone.Stack.peek(i);
	  		    	       if (sa.getSourceCard().equals(card))
	  		    	             return false;
	  		    	  }
	  		    	  
	  		    	  if (AllZone.GameAction.isCardInPlay(card) && !card.hasSickness() && !card.isTapped())
	  		    		  return true;
	  		    	  else
	  		    		  return false;
	  		      }
	  		      public boolean canPlayAI() 
	  		      {
	  		    	  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);

	  		    	  CardList list = new CardList(grave.getCards());
	  		    	  list = list.getType("Rebel");
	  		    	  
	  		    	  if (AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && list.size() >0)
	  		    		  return true;
	  		    	  else
	  		    		  return false;
	  		    	  
	  		      }
	  		      
	  		      public void resolve()
	  		      {
	  		    	  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	  		    	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
	  		    	  
	  		    	  CardList rebels = new CardList();
	  		    	  CardList list = new CardList(grave.getCards());
	  		    	  list = list.getType("Rebel");
	  		    	  
	  		    	  if (list.size()>0)
	  		    	  {
	  			    	  for (int i=0;i < list.size(); i++)
	  			    	  {
	  			    		  if (CardUtil.getConvertedManaCost(list.get(i).getManaCost()) <= converted)
	  			    		  {
	  			    			  rebels.add(list.get(i));
	  			    		  }
	  			    			  
	  			    	  }
	  			    	  
	  			    	  if (rebels.size() > 0)
	  			    	  {
	  				    	  if (player.equals(Constant.Player.Computer))
	  				    	  {
	  				    		  Card rebel = CardFactoryUtil.AI_getBestCreature(rebels);
	  				    		  grave.remove(rebel);
	  				    		  play.add(rebel);
	  				    	  }
	  				    	  else //human
	  				    	  {
	  				    		  Object o = AllZone.Display.getChoiceOptional("Select target Rebel", rebels.toArray());
	  				    		  Card rebel = (Card)o;
	  				    		  grave.remove(rebel);
	  				    		  play.add(rebel);
	  				    	  }
	  			    	  }//rebels.size() >0
	  		    	  }//list.size() > 0
	  		      }//resolve
	  		  };
	  		  ability.setDescription("6: Return target Rebel permanent card with converted mana cost 5 or less from your graveyard to play.");
	  		  ability.setStackDescription(card.getName() +" - return Rebel from graveyard to play.");
	  		  card.addSpellAbility(ability);
	      
	      }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Marrow-Gnawer"))
	      {
	      	final String player = card.getController();
	      	
	      	final SpellAbility ability = new Ability_Tap(card)
	          {
	  		  private static final long serialVersionUID = 447190529377334168L;

	  		  public void resolve()
	            {
	          	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
	          	  CardList cards = new CardList(play.getCards());
	          	  
	          	  Card c = getTargetCard();
	          	  if (c != null)
	          	  {
	  	        	  AllZone.GameAction.sacrifice(c);
	  	        	  
	  	        	  CardList rats = new CardList();
	  	        	  
	  	        	  for (int i=0;i < cards.size(); i++)
	  	              {
	  	            	  if (cards.get(i).getType().contains("Rat") || cards.get(i).getKeyword().contains("Changeling")){
	  	            		  Card k = cards.get(i);
	  	            		  rats.add(k);
	  	            	  }    
	  	              } 
	  	        	  
	  	        	  if (!c.getName().equals("Marrow-Gnawer"))
	  	        	  {
	  		        	  for (int j=0; j < rats.size()-1; j++)
	  		        	  {
	  		        		 makeToken(); 
	  		        	  }
	  	        	  }
	  	        	  else //some weird thing when targeting marrow himself, number of rats is different... so here's a hack:
	  	        	  {
	  	        		  for (int k=0; k < rats.size(); k++)
	  	        		  {
	  	        			  makeToken();
	  	        		  }
	  	        	  }
	          	  }
	            }//resolve()
	            
	            public boolean canPlayAI()
	            {
	          	  CardList list = new CardList(AllZone.Computer_Play.getCards());
	                list = list.getType("Rat");
	                
	                for (int i=0;i < list.size(); i++)
	                {
	                	String name = list.get(i).getName();
	                	if (!name.equals("Marrow-Gnawer") && !name.equals("Relentless Rats"))
	                		setTargetCard(list.get(i));
	                	if (name.equals("B 1 1 Rat"))
	                	{
	                		setTargetCard(list.get(i));
	                		break;
	                	}
	                	
	                }

	                if (getTargetCard() == null)
	              	  System.out.println("getTargetCard null");
	          	  if(getTargetCard() != null && list.size() > 3)
	          		  return true;
	          	  else
	          		  return false;
	            }

	            
	            public void makeToken()
	            {
	          	  Card c = new Card();

	                c.setOwner(card.getController());
	                c.setController(card.getController());

	                c.setName("Rat");
	                c.setImageName("B 1 1 Rat");
	                c.setManaCost("B");
	                c.setToken(true);

	                c.addType("Creature");
	                c.addType("Rat");
	                c.setBaseAttack(1);
	                c.setBaseDefense(1);

	                PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	                play.add(c);
	            }
	            
	            
	          };//ability
	          
	          Input runtime = new Input()
	          {
	  		  private static final long serialVersionUID = 8552290582665041908L;

	  		  public void showMessage()
	            {
	              CardList rats = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	              rats = rats.getType("Rat");
	              
	              stopSetNext(CardFactoryUtil.input_targetSpecific(ability, rats, "Select a Rat to sacrifice.", false));
	            }
	          };
	          
	          card.addSpellAbility(ability);
	          ability.setDescription("Tap, Sacrifice a rat: Put X 1/1 black Rat creature tokens into play, where X is the number of Rats you control.");
	          ability.setStackDescription(card.getName() + " - Put X 1/1 black Rat creature tokens into play, where X is the number of Rats you control.");
	          ability.setBeforePayMana(runtime);
	      	
	      	
	      }//*************** END ************ END **************************
	    //*************** START *********** START **************************
	      else if(cardName.equals("Arcanis the Omnipotent"))
	      {
	  	    final Ability_Tap ability = new Ability_Tap(card)
	  	    {
	  		  private static final long serialVersionUID = 4743686230518855738L;

	  		  public boolean canPlayAI()
	  	      {
	  	    	  return true;
	  	      }

	  	      public void resolve()
	  	      {
	  	        AllZone.GameAction.drawCard(card.getController());
	  	        AllZone.GameAction.drawCard(card.getController());
	  	        AllZone.GameAction.drawCard(card.getController());
	  	      }
	  	    };//SpellAbility
	  	    
	  	    final SpellAbility ability2 = new Ability(card,"2 U U")
	  	      {
	  	    	  public void resolve()
	  	          {
	  	    		  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
	  	    		  
	  	    		  card.untap();
	  	    		  AllZone.getZone(card).remove(card);
	  	    		  if (!card.isToken())
	  	    			  hand.add(card);
	  	    		  
	  	          }
	  	    	  public boolean canPlayAI()
	  	    	  {
	  	    		  return false;
	  	    	  }
	  	      }; //ability2
	  	    
	  	    card.addSpellAbility(ability);
	  	    ability.setDescription("tap: Draw three cards.");
	  	    ability.setStackDescription("Arcanis - " + card.getController() + " draws three cards.");
	  	    ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
	  	    
	  	    card.addSpellAbility(ability2);
	  	    ability2.setStackDescription(card.getController() + " returns Arcanis back to owner's hand.");
	  	    ability2.setDescription("2 U U: Return Arcanis the Omnipotent to its owner's hand.");
	  	    
	    }//*************** END ************ END ************************** 
	      
	    //*************** START *********** START **************************
	      else if(cardName.equals("Horde of Notions"))
	      {
	        final Ability ability = new Ability(card, "W U B R G")
	        {
	          public void resolve()
	          {
	            Card c = null;
	            if (card.getController().equals("Human"))
	            {
	          	  Object o = AllZone.Display.getChoiceOptional("Select Elemental", getCreatures());
	          	  c = (Card)o;    	          	 
	          	  
	            }
	            else 
	            {
	          	  c = getAIElemental();
	            }
	            
	            PlayerZone grave = AllZone.getZone(c);

	            if(AllZone.GameAction.isCardInZone(c, grave))
	            {
	              PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
	              AllZone.GameAction.moveTo(play, c);
	            }
	          }//resolve()
	          public boolean canPlay()
	          {
	            return getCreatures().length != 0 && AllZone.GameAction.isCardInPlay(card);
	          }
	          public Card[] getCreatures()
	          {
	            CardList creature = new CardList();
	            PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	            creature.addAll(zone.getCards());
	            creature = creature.getType("Elemental");
	            return creature.toArray();
	          }
	          public Card getAIElemental()
	          {
	            Card c[] = getCreatures();
	            Card biggest = c[0];
	            for(int i = 0; i < c.length; i++)
	              if(biggest.getNetAttack() < c[i].getNetAttack())
	                biggest = c[i];

	            return biggest;
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        
	        ability.setDescription("W U B R G: You may play target Elemental card from your graveyard without paying its mana cost.");
	        //ability.setBeforePayMana(new Input_);
	        ability.setStackDescription("Horde of Notions - play Elemental card from graveyard without paying its mana cost.");
	        ability.setBeforePayMana(new Input_PayManaCost(ability));
	       
	      }//*************** END ************ END **************************
	      

	    //*************** START *********** START **************************
	  	else if(cardName.equals("Ravenous Rats") || cardName.equals("Corrupt Court Official"))
	  	{
	  		final Ability ability = new Ability(card,"0")
	  	    {
	  	      public boolean canPlayAI()
	  	      {
	  	    	  return true;	       
	  	      }
	  	      public void resolve()
	  	      {
	  	        if(Constant.Player.Computer.equals(getTargetPlayer()))
	  	          AllZone.GameAction.discardRandom(getTargetPlayer());
	  	        else
	  	          AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	  	      }//resolve()
	  	    };//SpellAbility
	  	    
	  	  Command intoPlay = new Command()
	        {
	  		private static final long serialVersionUID = -2028008593708491452L;

	  		public void execute()
	          {
	            if(card.getController().equals(Constant.Player.Human))
	            {
	              AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
	              ButtonUtil.disableAll();
	            }
	            else//computer
	            {
	          	ability.setTargetPlayer(Constant.Player.Human);
	              AllZone.Stack.add(ability);
	            }//else
	          }//execute()
	        };//Command
	        card.addComesIntoPlayCommand(intoPlay);
	      
	    }//*************** END ************ END **************************
	  	
	   //*************** START *********** START **************************
	    else if(cardName.equals("Boris Devilboon"))
	    {		
	  		final Ability_Tap tokenAbility1 = new Ability_Tap(card, "2 B R")
	  	    {
	  			private static final long serialVersionUID = -6343382804503119405L;

	  			public boolean canPlayAI()
	  	        {
	  	          String phase = AllZone.Phase.getPhase();
	  	          return phase.equals(Constant.Phase.Main2);
	  	        }
	  	        public void chooseTargetAI() {card.tap();}

	  	        public void resolve()
	  	        {
	  	          Card c = new Card();

	  	          c.setOwner(card.getController());
	  	          c.setController(card.getController());

	  	          c.setName("Minor Demon");
	  	          c.setImageName("BR 1 1 Minor Demon");
	  	          c.setManaCost("B R");
	  	          c.setToken(true);

	  	          c.addType("Creature");
	  	          c.addType("Demon");

	  	          c.setBaseAttack(1);
	  	          c.setBaseDefense(1);

	  	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	  	          play.add(c);
	  	        }//resolve()
	  	      };//SpellAbility

	  	      card.addSpellAbility(tokenAbility1);

	  	      tokenAbility1.setDescription("2 B R, tap: Put a 1/1 black and red Demon creature token named Minor Demon onto the battlefield.");
	  	      tokenAbility1.setStackDescription(card.getName() + " - Put a 1/1 black and red Demon creature token named Minor Demon onto the battlefield.");
	  	      tokenAbility1.setBeforePayMana(new Input_PayManaCost(tokenAbility1));
	    }
	  	  
	    //*************** START *********** START **************************
	    else if(cardName.equals("Rhys the Redeemed"))
	    {		
	  		final Ability_Tap tokenAbility1 = new Ability_Tap(card, "2 GW")
	  	      {
	  			private static final long serialVersionUID = 411298860775527337L;

	  			public boolean canPlayAI()
	  	        {
	  	          String phase = AllZone.Phase.getPhase();
	  	          return phase.equals(Constant.Phase.Main2);
	  	        }
	  	        public void chooseTargetAI() {card.tap();}

	  	        public void resolve()
	  	        {
	  	          Card c = new Card();

	  	          c.setOwner(card.getController());
	  	          c.setController(card.getController());

	  	          c.setName("Elf Warrior");
	  	          c.setImageName("GW 1 1 Elf Warrior");
	  	          c.setManaCost("GW");
	  	          c.setToken(true);

	  	          c.addType("Creature");
	  	          c.addType("Elf");
	  	          c.addType("Warrior");

	  	          c.setBaseAttack(1);
	  	          c.setBaseDefense(1);

	  	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	  	          play.add(c);
	  	        }//resolve()
	  	      };//SpellAbility

	  	      card.addSpellAbility(tokenAbility1);

	  	      tokenAbility1.setDescription("2 GW, tap: Put a 1/1 green and white Elf Warrior creature token into play.");
	  	      tokenAbility1.setStackDescription(card.getName() + " - Put a 1/1 green and white Elf Warrior creature token into play.");
	  	      tokenAbility1.setBeforePayMana(new Input_PayManaCost(tokenAbility1));
	  	      
	  	      /////////////////////////////////////////////////////////////////////
	  	       
	  	      final Ability_Tap copyTokens1 = new Ability_Tap(card, "4 GW GW")
	  	      {
	  			private static final long serialVersionUID = 6297992502069547478L;

	  			public void resolve()
	  	    	  {
	  	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	  	    		  CardList allTokens = new CardList();
	  	    		  allTokens.addAll(play.getCards());
	  	    		  allTokens = allTokens.filter(new CardListFilter()
	  	    		  {
	  	    			  public boolean addCard(Card c)
	  	    			  {
	  	    				  return c.isCreature() && c.isToken();
	  	    			  }
	  	    		  });
	  	    		  
	  	    		  for (int i=0;i<allTokens.size();i++)
	  	    		  {
	  	    			  Card c = allTokens.get(i);
	  	    			  copyToken(c);
	  	    		  }  
	  	    	  }
	  	    	  
	  	    	  public void copyToken(Card token)
	  		      {
	  		    	  Card copy = new Card();
	  		    	  copy.setName(token.getName());
	  		    	  copy.setImageName(token.getImageName());
	  		    	  
	  	              copy.setOwner(token.getController());
	  	              copy.setController(token.getController());
	  	              copy.setManaCost(token.getManaCost());
	  	              copy.setToken(true);
	  	              copy.setType(token.getType());
	  	              copy.setBaseAttack(token.getBaseAttack());
	  	              copy.setBaseDefense(token.getBaseDefense());
	  	              
	  	              PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	  	              play.add(copy);
	  		      }
	  	    	  
	  	    	  public boolean canPlayAI()
	  	    	  {
	  	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	  	    		  CardList tokens = new CardList(play.getCards());
	  	    		  tokens = tokens.filter(new CardListFilter(){

	  					public boolean addCard(Card c) {
	  						return c.isToken();
	  					}
	  	    			  
	  	    		  });
	  	    		  return tokens.size() >= 2;
	  	    	  }
	  	      };
	  	      
	  	      card.addSpellAbility(copyTokens1);

	  	      copyTokens1.setDescription("4 GW GW, tap: For each creature token you control, put a token into play that's a copy of that creature.");
	  	      copyTokens1.setStackDescription(card.getName() + " - For each creature token you control, put a token into play that's a copy of that creature.");
	  	      copyTokens1.setBeforePayMana(new Input_PayManaCost(copyTokens1));
	  	      
	  	      
	  	      
	    }//*************** END ************ END **************************
	  	
	  	//*************** START *********** START **************************
	  	else if(cardName.equals("Bringer of the Green Dawn") || cardName.equals("Bringer of the Blue Dawn")  || 
	  	   cardName.equals("Bringer of the White Dawn"))
	  	{
	  		final SpellAbility diffCost = new Spell(card)
	  		{
	  			private static final long serialVersionUID = -1598664186463358630L;
	  			public void resolve()
	  			{
	  				PlayerZone hand = AllZone.getZone(Constant.Zone.Hand ,card.getController());
	  				PlayerZone play = AllZone.getZone(Constant.Zone.Play ,card.getController());
	  				
	  				
	  				hand.remove(card);
	  				play.add(card);
	  				card.comesIntoPlay(); //do i need this?
	  			}
	  			public boolean canPlay()
	  			{
	  				return AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                  !AllZone.Phase.getPhase().equals("End of Turn") && !AllZone.GameAction.isCardInPlay(card);
	  			}
	  			
	  		};
	  		diffCost.setManaCost("W U B R G");
	  		diffCost.setDescription("You may pay W U B R G rather than pay "+card.getName() +"'s mana cost. ");
	  		diffCost.setStackDescription(card.getName() + " - Creature 5/5");
	  		card.addSpellAbility(diffCost);
	  		
	  	
	  		
	  	}//*************** END ************ END **************************
	  	
	  	//*************** START *********** START **************************
	  	else if(cardName.equals("Thelonite Hermit"))
	  	{
	  		
	  		Command turnsFaceUp = new Command()
	  	    {    
	  			private static final long serialVersionUID = -3882798504865405413L;

	  			public void execute()
	  	        {
	  	          makeToken();
	  	          makeToken();
	  	          makeToken();
	  	          makeToken();
	  	          
	  	        }//execute()
	  	        
	  	        public void makeToken()
	  	        {
	  	        	Card c = new Card();


	  	        	  c.setName("Saproling");
	  	        	  c.setImageName("G 1 1 Saproling");

	  	  	          c.setOwner(card.getController());
	  	  	          c.setController(card.getController());

	  	  	          c.setManaCost("G");
	  	  	          c.setToken(true);
	  	  	         
	  	  	          c.addType("Creature");
	  	  	          c.addType("Saproling");
	  	  	          c.setBaseAttack(1);
	  	  	          c.setBaseDefense(1);

	  	  	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	  	  	          play.add(c);
	  	        }
	  	      };//Command

	  		
	  		card.addTurnFaceUpCommand(turnsFaceUp);
	  	}//*************** END ************ END **************************

	  	//*************** START *********** START **************************
	  	else if(cardName.equals("Imperial Hellkite"))
	  	{
	  		final String player = card.getController();
	  		Command turnsFaceUp = new Command()
	  	    {
	  			private static final long serialVersionUID = -1407485989096862288L;

	  			public void execute()
	  	        {
	  	        	  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	  		    	  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	  		    	  
	  		    	  CardList list = new CardList(lib.getCards());
	  		    	  list = list.getType("Dragon");
	  		    	  
	  		    	  if (list.size()==0)
	  		    		  return;

	  		    	  
	  		    	  if (player.equals(Constant.Player.Computer))
	  		    	  {
	  		    		  Card dragon = CardFactoryUtil.AI_getBestCreature(list);
	  		    		  lib.remove(dragon);
	  		    		  hand.add(dragon);
	  		    	  }
	  		    	  else //human
	  		    	  {
	  		    		  Object o = AllZone.Display.getChoiceOptional("Select Dragon", list.toArray());
	  		    		  Card dragon = (Card)o;
	  		    		  lib.remove(dragon);
	  		    		  hand.add(dragon);
	  		    	  }
	  		    	  AllZone.GameAction.shuffle(card.getController());
	  	        }//execute()
	  	    };//Command
	  		
	  		card.addTurnFaceUpCommand(turnsFaceUp);
	  		
	  	}//*************** END ************ END **************************
	  	
	  	  //*************** START *********** START **************************
	    else if(cardName.equals("Serra Avatar"))
	    {
	      Command leavesPlay = new Command()
	      {
	        private static final long serialVersionUID = -2274397219668820020L;

			public void execute()
	        {
	          //moveto library
	          PlayerZone libraryZone = AllZone.getZone(Constant.Zone.Library, card.getOwner());
	          AllZone.GameAction.moveTo(libraryZone, card);
	          //shuffle library
	          AllZone.GameAction.shuffle(card.getOwner());
	        }//execute()
	      };//Command
	      card.addDestroyCommand(leavesPlay);
	    }//***************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Pestermite"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	    	  public void resolve()
	          {
	            Card c = getTargetCard();

	            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c))
	            {
		          	if(c.isTapped())
		          		c.untap();
		          	else 
		               c.tap();
	            }
	          }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 5202575895575352408L;

			public void execute()
	        {
	        	CardList all = new CardList();
	        	all.addAll(AllZone.Human_Play.getCards());
	        	all.addAll(AllZone.Computer_Play.getCards());
	        	
	        	CardList hum = new CardList();
	        	hum.addAll(AllZone.Human_Play.getCards());
	        	
	        	if (all.size() != 0) {
	        		
	        		if(card.getController().equals(Constant.Player.Human)) {
	        			AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, all, "Select target permanent to tap/untap.", true));
	        			ButtonUtil.enableAll();
	        		}
	        		else if (card.getController().equals(Constant.Player.Computer)) {
	        			Card human = CardFactoryUtil.AI_getBestCreature(hum);
	        			ability.setTargetCard(human);
	        			AllZone.Stack.add(ability);
	        		}
	        	}
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -3055232264358172133L;

			public boolean canPlayAI()
	        {
	          CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
	          list = list.filter(new CardListFilter(){
				public boolean addCard(Card c) {
					return c.isUntapped();
				}
	          });
	          
	          return (list.size() > 0) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Angel of Despair"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	    	  public void resolve()
	          {
	            Card c = getTargetCard();

	            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c))
	            {
		          	if(c.isToken())
		          		AllZone.getZone(c).remove(c);
		
		          	else 
		                AllZone.GameAction.destroy(c);
	            }
	          }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -3583483691705438214L;

			public void execute()
	        {
	        	CardList all = new CardList();
	        	all.addAll(AllZone.Human_Play.getCards());
	        	all.addAll(AllZone.Computer_Play.getCards());
	        	
	        	CardList hum = new CardList();
	        	hum.addAll(AllZone.Human_Play.getCards());
	        	
	        	if (all.size() != 0) {
	        		
	        		if(card.getController().equals(Constant.Player.Human)) {
	        			AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, all, "Select target permanent to destroy.", true));
	        			ButtonUtil.disableAll();
	        		}
	        		else if (card.getController().equals(Constant.Player.Computer)) {
	        			if (hum.size() > 0 )
	        			{
		        			Card human = CardFactoryUtil.AI_getBestCreature(hum);
		        			ability.setTargetCard(human);
		        			AllZone.Stack.add(ability);
	        			}
	        		}
	        	}
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -173202865726476053L;

			public boolean canPlayAI()
	        {
	          CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);

	          return (list.size() > 0) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
	        }
	      });
	    }//*************** END ************ END **************************   
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Mystic Snake"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          if(AllZone.Stack.size() > 0) {
	        	  SpellAbility sa = AllZone.Stack.peek();
	        	  if (sa.isSpell() && CardFactoryUtil.isCounterable(sa.getSourceCard()) ) {
	        		  sa = AllZone.Stack.pop();
	        		  AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
	        	  }
	          }
	        }//resolve()
	      };//SpellAbility
	      Command intoPlay = new Command()
	      {
	        private static final long serialVersionUID = -6564365394043612388L;

			public void execute()
	        {
	          if(AllZone.Stack.size() > 0) {
	        	ability.setStackDescription("Mystic Snake counters " +AllZone.Stack.peek().getSourceCard().getName());
	        	AllZone.Stack.add(ability);
	          }
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	      
	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = 6440845807532409545L;

			public boolean canPlayAI()
	        {
	          return false;
	        }
	      });

	    }//*************** END ************ END **************************

	      
	    //*************** START *********** START **************************
	    else if(cardName.equals("Captain Sisay"))
	    {
	      final Ability_Tap ability = new Ability_Tap(card)
	      {
			private static final long serialVersionUID = 7978812786945030021L;
			public void resolve()
	        {
	        	String player = card.getController();
	            if(player.equals(Constant.Player.Human))
	              humanResolve();
	            else
	              computerResolve();
	        }//resolve()
	        public void humanResolve()
	        {
	          CardList cards = new CardList(AllZone.Human_Library.getCards());
	          //legends = legends.getType().contains("Legendary");
	          CardList legends = new CardList();
	          
	          for (int i=0;i < cards.size(); i++)
	          {
	        	  //System.out.println("type: " +cards.get(i).getType());
	        	  if (cards.get(i).getType().contains("Legendary")){
	        		  //System.out.println(cards.get(i).getName());
	        		  Card c = cards.get(i);
	        		  legends.add(c);
	        		  
	        	  }
	          }
	          
	          if (legends.size() != 0) {          
		          Object check = AllZone.Display.getChoiceOptional("Select Legend", legends.toArray());
		          if(check != null)
		          {
		            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
		            AllZone.GameAction.moveTo(hand, (Card)check);
		          }
		          AllZone.GameAction.shuffle(Constant.Player.Human);
	          }
	        }
	        public void computerResolve()
	        {
	          Card[] library = AllZone.Computer_Library.getCards();
	          CardList list = new CardList(library);
	          CardList legends = new CardList();
	          //list = list.getType("Creature");

	          for (int i=0;i < list.size(); i++)
	          {
	        	  if (list.get(i).getType().contains("Legendary")){
	        		  Card k = list.get(i);
	        		  legends.add(k);
	        	  }
	          
	          }
	          
	          //pick best creature
	          if (legends.size() != 0){
		          Card c = CardFactoryUtil.AI_getBestCreature(legends);
		          if(c == null)
		            c = library[0];
		          System.out.println("computer picked - " +c);
		          AllZone.Computer_Library.remove(c);
		          AllZone.Computer_Hand.add(c);
	          }
	        }
	        
	        
	      };//SpellAbility
	      //card.addSpellAbility(ability);
	      ability.setDescription("tap: Search your library for a Legend or legendary card, reveal that card, and put it into your hand. Then shuffle your library.");
	      ability.setBeforePayMana(new Input_NoCost_TapAbility((Ability_Tap) ability));
	      ability.setStackDescription("Captain Sisay searches for a Legend or Legendary card...");
	      card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Siege-Gang Commander"))
	    {
	    	
	    	final SpellAbility comesIntoPlayAbility = new Ability(card, "0")
	        {
	          public void resolve()
	          {
	            makeToken();
	            makeToken();
	            makeToken();
	          }//resolve()
	          
	          public void makeToken()
		      {
	        	  Card c = new Card();

	        	  c.setName("Goblin");
	  	          c.setImageName("R 1 1 Goblin");

	  	          c.setOwner(card.getController());
	  	          c.setController(card.getController());

	  	          c.setManaCost("R");
	  	          c.setToken(true);
	  	         
	  	          c.addType("Creature");
	  	          c.addType("Goblin");
	  	          c.setBaseAttack(1);
	  	          c.setBaseDefense(1);

	  	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	  	          play.add(c);
		        }

	        }; //comesIntoPlayAbility
	        
	        Command intoPlay = new Command()
	        {
				private static final long serialVersionUID = 8778828278589063477L;

				public void execute()
		          {
		            comesIntoPlayAbility.setStackDescription(card.getName() + " - put three 1/1 red Goblin creature tokens into play.");
		            AllZone.Stack.add(comesIntoPlayAbility);
		          }
	        };
	        
	        card.addComesIntoPlayCommand(intoPlay);
	    	
	    
	    	final SpellAbility ability = new Ability(card, "1 R")
	        {

				private static final long serialVersionUID = -6653781740344703908L;

				public void resolve()
		        {
		        	  String player = card.getController();
		              if(player.equals(Constant.Player.Human))
		                humanResolve();
		              else
		                computerResolve();
		        }//resolve()
		          
		        public void humanResolve() {
		        	  String player = card.getController();
		        	  
		        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
		        	  CardList cards = new CardList(play.getCards());
		        	  
		        	  CardList creatures = new CardList();
		        	  
		        	  for (int i=0;i < cards.size(); i++)
		              {
		            	  if (cards.get(i).getType().contains("Goblin") || cards.get(i).getKeyword().contains("Changeling")){
		            		  Card k = cards.get(i);
		            		  creatures.add(k);
		            	  }    
		              } 
		        	  
		        	  if (creatures.size() != 0) {          
		    	          Object check = AllZone.Display.getChoiceOptional("Select Goblin to Sacrifice", creatures.toArray());
		    	          if(check != null)
		    	          {
		    	            Card c = (Card)check;
		    	            if(AllZone.GameAction.isCardInPlay(c))
		    	            {
		    	            	AllZone.GameAction.sacrifice(c);
		    	            	
		    	            	if(getTargetCard() != null)
		    	                {
		    	                  if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
		    	                  {
		    	                      Card crd = getTargetCard();
		    	                      //c.addDamage(damage);
		    	                      AllZone.GameAction.addDamage(crd, card, 2);
		    	                  }
		    	                }
		    	                else
		    	                  AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(2);
		    	            	
		    	            }
		    	          }
		              }
		        }//humanResolve
		          
		        public void computerResolve() {
		        	  String player = card.getController();
		        	  
		        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
		        	  CardList cards = new CardList(play.getCards());
		        	  
		        	  CardList creatures = new CardList();
		        	  
		        	  for (int i=0;i < cards.size(); i++)
		              {
		            	  if (cards.get(i).getType().contains("Goblin") || cards.get(i).getType().contains("Changeling")){
		            		  Card k = cards.get(i);
		            		  creatures.add(k);
		            	  }    
		              } 
		        	  //.... TODO
		        	  
		          }//compResolve
		          
		          public boolean canPlayAI()
		          {
		        	  return false;
		          }
	          
	        };//ability
	        
	        card.addSpellAbility(ability);
	        ability.setDescription("1 R, Sacrifice a goblin: Siege-Gang Commander deals 2 damage to target creature or player .");
	        ability.setStackDescription("Siege-Gang Commander deals 2 damage to target creature or player");
	        ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true));
	  	}//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Brion Stoutarm"))
	    {
	    
	    	final SpellAbility ability = new Ability_Tap(card, "R")
	        {
	       		private static final long serialVersionUID = -7755879134314608010L;

				public void resolve()
		        {
		        	  String player = card.getController();
		              if(player.equals(Constant.Player.Human))
		                humanResolve();
		              else
		                computerResolve();
		        }//resolve()
		          
		        public void humanResolve() {
		        	  String player = card.getController();
		        	  
		        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
		        	  CardList cards = new CardList(play.getCards());
		        	  
		        	  CardList creatures = new CardList();
		        	  
		        	  for (int i=0;i < cards.size(); i++)
		              {
		            	  if (cards.get(i).getType().contains("Creature") && !cards.get(i).getName().equals("Brion Stoutarm")){
		            		  Card k = cards.get(i);
		            		  creatures.add(k);
		            	  }    
		              } 
		        	  
		        	  if (creatures.size() != 0) {          
		    	          Object check = AllZone.Display.getChoiceOptional("Select Creature to Sacrifice", creatures.toArray());
		    	          if(check != null)
		    	          {
		    	            Card c = (Card)check;
		    	            if(AllZone.GameAction.isCardInPlay(c))
		    	            {
		    	            	int power = c.getNetAttack();
		    	            	AllZone.GameAction.sacrifice(c);
		    	            	String opponent = AllZone.GameAction.getOpponent(player);
		    	            	
		    	            	PlayerLife life = AllZone.GameAction.getPlayerLife(opponent);
		    	            	life.subtractLife(power);
		    	            	
		    	            	GameActionUtil.executeLifeLinkEffects(card, power);
		    	            	
    	    	            	CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
		    	        		for (Card crd : cl)
		    	        		{
		    	        		  GameActionUtil.executeGuiltyConscienceEffects(card, crd, power);
		    	        		}
		    	            	
		    	            	card.setDealtDmgToOppThisTurn(true);
		    	            }
		    	          }
		              }
		          }//humanResolve
		          
		          public void computerResolve() {
		        	  String player = card.getController();
		        	  
		        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
		        	  CardList cards = new CardList(play.getCards());
		        	  
		        	  CardList creatures = new CardList();
		        	  
		        	  for (int i=0;i < cards.size(); i++)
		              {
		            	  if (cards.get(i).getType().contains("Creature") && !cards.get(i).getName().equals("Brion Stoutarm")){
		            		  Card k = cards.get(i);
		            		  creatures.add(k);
		            	  }    
		              } 
		        	  //.... TODO
		        	  
		          }//compResolve
		          
		          public boolean canPlayAI()
		          {
		        	  return false;
		          }
		          
	        };//ability
	        
	        card.addSpellAbility(ability);
	        ability.setDescription("R, tap, Sacrifice a creature other than Brion Stoutarm: Brion Stoutarm deals damage equal to the sacrificed creature's power to target player.");
	        ability.setStackDescription("Brion Stoutarm deals damage equal to sacrificed creature's power");
	  	}//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Trinket Mage"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        hand.add(c);	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {

			private static final long serialVersionUID = 4022442363194287539L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList arts = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Artifact") && CardUtil.getConvertedManaCost(cards.get(i).getManaCost()) <= 1 )
	        	  {
	        		  arts.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(arts.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", arts.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            arts.shuffle();
	            ability.setTargetCard(arts.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Goblin Matron"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        hand.add(c);	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {


			/**
			 * 
			 */
			private static final long serialVersionUID = 4022442363194287539L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList goblins = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Goblin") || cards.get(i).getKeyword().contains("Changeling"))
	        	  {
	        		  goblins.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(goblins.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", goblins.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            goblins.shuffle();
	            ability.setTargetCard(goblins.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Kithkin Harbinger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        lib.add(c, 0);  	
	        	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
	        private static final long serialVersionUID = 4022442363194287539L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList kithkin = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Kithkin") || cards.get(i).getKeyword().contains("Changeling"))
	        	  {
	        		  kithkin.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(kithkin.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", kithkin.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            kithkin.shuffle();
	            ability.setTargetCard(kithkin.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Treefolk Harbinger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
	        	lib.add(c, 0);  	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
	        private static final long serialVersionUID = 9170723718484515120L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList treefolkForests = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if((cards.get(i).getType().contains("Treefolk") || cards.get(i).getKeyword().contains("Changeling")) 
	        		|| cards.get(i).getType().contains("Forest"))
	        	  {
	        		  treefolkForests.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(treefolkForests.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", treefolkForests.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            treefolkForests.shuffle();
	            ability.setTargetCard(treefolkForests.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
    else if(cardName.equals("Archon of Justice"))
    {
        final SpellAbility ability = new Ability(card, "0")
        {
          public void resolve()
          {
            if(getTargetCard() != null){
                if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
                {
                	AllZone.GameAction.removeFromGame(getTargetCard());
                }
            }
          }
        };
        ability.setStackDescription("Archon of Justice - Remove target permament from the game.");
       
        Command leavesPlay = new Command()
        {
          private static final long serialVersionUID = 7552566264976488465L;

		public void execute()
          {
            if(card.getController().equals(Constant.Player.Human))
              AllZone.InputControl.setInput(CardFactoryUtil.input_targetPermanent(ability));
            else
            {
                //if computer controlled Archon of Justice have it select the best creature, or enchantment, or artifact, whatever the human controllers, and as a last option a card it controls
               
               Card temp;
            	
               CardList human_list = new CardList(AllZone.Human_Play.getCards());
               temp = CardFactoryUtil.AI_getBestCreature(human_list);
               if (temp != null)
            	   ability.setTargetCard(CardFactoryUtil.AI_getBestCreature(human_list));
               if(ability.getTargetCard() == null){
            	  temp = CardFactoryUtil.AI_getBestEnchantment(human_list, card ,false);
            	  if (temp != null)
            		  ability.setTargetCard(CardFactoryUtil.AI_getBestEnchantment(human_list, card, true));
               }
               if(ability.getTargetCard() == null){
            	  temp = CardFactoryUtil.AI_getBestArtifact(human_list);
            	  if (temp != null)
            		  ability.setTargetCard(CardFactoryUtil.AI_getBestArtifact(human_list));
               }
               if(ability.getTargetCard() == null){
                  if(human_list.size() == 0){
                     CardList computer_list = new CardList(AllZone.Computer_Play.getCards());
                     if(computer_list.size() == 0){
                        return; //we have nothing in play to destroy.
                     }
                     else{
                        ability.setTargetCard(computer_list.get(0)); //should determine the worst card to destroy, but this case wont be hit much.
                     }
                  }
                  ability.setTargetCard(human_list.get(0));
               }
              AllZone.Stack.add(ability);
            }
          }//execute()
        };//Command
        card.addDestroyCommand(leavesPlay);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Knight of the Reliquary"))
    {
    	final Ability_Tap ability = new Ability_Tap(card)
    	{
    		private static final long serialVersionUID = 7554368501399705784L;
			public void resolve()
    		{
    			PlayerZone lib  = AllZone.getZone(Constant.Zone.Library, card.getController());
    			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController()); 
    			
    			Card c = getTargetCard();
    			if (AllZone.GameAction.isCardInPlay(c))
    			{
	    			AllZone.GameAction.sacrifice(c);
	    			
	    			CardList landInLib  = new CardList(lib.getCards());
	    			landInLib = landInLib.getType("Land");
	    			
	    			if (landInLib.size() > 0)
	    			{
	    				if (card.getController().equals(Constant.Player.Computer))
	    				{
	    					lib.remove(landInLib.get(0));
	    					play.add(landInLib.get(0));
	    				}
	    				else
	    				{
	    					Object o = AllZone.Display.getChoiceOptional("Select land card to put into play: ", landInLib.toArray());
	       	       			if (o != null)
	       	       			{	
	       	       				Card crd = (Card)o;
	       	       				lib.remove(crd);
	       	       				play.add(crd);
	       	       			}
	    				}
	    				AllZone.GameAction.shuffle(card.getController());
	    			}
    			}//if(isCardInPlay)
    		}

    		public boolean canPlayAI()
    		{
    			CardList landInLib  = new CardList(AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer).getCards());
    			CardList landInPlay = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
    			
    			landInLib  = landInLib.getType("Land");
    			landInPlay = landInPlay.getType("Land");
    			
    			if (landInLib.size() > 0 && landInPlay.size() > 0)
    				return true;
    			else
    				return false;
    				
    		}
    		public void chooseTargetAI()
    		{
    			CardList land = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                land = land.filter(new CardListFilter()
                {
                    public boolean addCard(Card c)
                    {
                    	if(c.getType().contains("Plains") || c.getType().contains("Forest"))
                    		return true;
                    	else
                    		return false;
                    }
                });
                if(land.size()>0)
                	setTargetCard(land.get(0));
    		}
    	};
    	
    	Input runtime = new Input()
        {
          private static final long serialVersionUID = -4320917612145305541L;

		public void showMessage()
          {
            CardList land = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
            land = land.filter(new CardListFilter()
            {
                public boolean addCard(Card c)
                {
                	if(c.getType().contains("Plains") || c.getType().contains("Forest"))
                		return true;
                	else
                		return false;
                }
            });
            
            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, land, "Select a Plains or Forest to sacrifice.", false));
          }
        };
        ability.setBeforePayMana(runtime);
    	ability.setDescription("\r\n" + "tap, Sacrifice a Forest or Plains: Search your library for a land card, put it into play, then shuffle your library.");
    	ability.setStackDescription(card.getName() + " - Search your library for a card and put it into play, then shuffle your library");
    	card.addSpellAbility(ability);
    	
    }
    
  //*************** START *********** START **************************
    else if(cardName.equals("Knight of the White Orchid"))
    {
    	final Ability ability = new Ability(card, "0")
        {
          public void resolve()
          {
       	   
            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
   	        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
   	       
   	       
   	        CardList basic = new CardList(lib.getCards());
   	        basic = basic.getType("Plains");
   	        
   	                	
   	        
   	       	if (card.getController().equals(Constant.Player.Computer))
   	       	{
   	       		if (basic.size() > 0)
   	       		{
   	       			Card c = basic.get(0);
   	       			lib.remove(c);
   	       			play.add(c);
   	       			
   	       		}
   	       	}
   	       	else // human
   	       	{
   	       		if (basic.size() > 0) 
   	       		{
   	       			Object o = AllZone.Display.getChoiceOptional("Select Plains card to put into play: ", basic.toArray());
   	       			if (o != null)
   	       			{	
   	       				Card c = (Card)o;
   	       				lib.remove(c);
   	       				play.add(c);
   	       			}
   	       		}
   	       	}
   	       	AllZone.GameAction.shuffle(card.getController());
          }//resolve()
                    
        };//Ability
   	 
   	 Command fetchBasicLand = new Command()
        {

           private static final long serialVersionUID = -1086551054597721988L;

		public void execute()
           {
        	    String player = card.getController();
        	    PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play, AllZone.GameAction.getOpponent(player));
        	    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
        	    
      	        CardList self = new CardList(play.getCards());
      	        CardList opp = new CardList(oppPlay.getCards());

      	        self = self.getType("Land");
      	        opp = opp.getType("Land");
      	       	
      	        if (self.size() < opp.size())
      	        {
      	        	ability.setStackDescription(card.getName()+ " - search library for a plains and put it into play");
      	        	AllZone.Stack.add(ability);
      	        }
           }
        };
        
        card.addComesIntoPlayCommand(fetchBasicLand);
     
       
      
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Niv-Mizzet, the Firemind"))
    {
      final Ability_Tap ability = new Ability_Tap(card)
      {
        private static final long serialVersionUID = 8670005059055071206L;
		public boolean canPlayAI() {return false;}
        public void resolve()
        {
          AllZone.GameAction.drawCard(card.getController());
        } 
      };//SpellAbility
      card.addSpellAbility(ability);
      ability.setDescription("tap: Draw a card.");
      ability.setStackDescription(card.getName() + " - draw a card.");
      ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
    }//*************** END ************ END **************************
    
    
    
    //*************** START *********** START **************************
    else if(cardName.equals("Elvish Farmer") || cardName.equals("Mycologist")) 
    {
    	Command intoPlay = new Command()
        {
		   private static final long serialVersionUID = 882942955555047018L;
		   public boolean firstTime = true;
           public void execute()
           {

             if(firstTime){
           	  card.setCounter(Counters.SPORE, 0); 
             }
             firstTime = false;
           }
         };
         
         card.addComesIntoPlayCommand(intoPlay);

  	    final SpellAbility a2 = new Ability(card, "0")
	    {
  	    	public void chooseTargetAI()
  	    	{
  	    		PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	        	CardList saps = new CardList(play.getCards());
	        	saps = saps.filter(new CardListFilter()
	        	{

					public boolean addCard(Card c) {
						if((c.getType().contains("Saproling") || c.getKeyword().contains("Changeling")) &&
							AllZone.GameAction.isCardInPlay(c)	)
							return true;
						return false;
					}
	        	
	        	});
	        	
	        	if (saps.size() != 0)
	        		setTargetCard(saps.getCard(0));
  	    	}
	        public void resolve()
	        {
	          //get all saprolings:
	          Card c = getTargetCard();
	          if(c == null )
	        	  return;
	          
	          if (!AllZone.GameAction.isCardInPlay(c))
	        	  return;
	          
	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            //AllZone.getZone(c).remove(c);
	        	AllZone.GameAction.sacrifice(c);
	        	  
	        	PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	        	life.addLife(2);
	          }
	        }//resolve
	        public boolean canPlayAI()
	        {
	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	        	CardList saps = new CardList(play.getCards());
	        	saps = saps.filter(new CardListFilter()
	        	{

					public boolean addCard(Card c) {
						if(c.getType().contains("Saproling") || c.getKeyword().contains("Changeling") &&
							AllZone.GameAction.isCardInPlay(c))
							return true;
						return false;
					}
	        	
	        	});
	        	if(AllZone.Computer_Life.getLife() < 6 && saps.size() > 0)
	        		return true;
	        	else
	        		return false;
	        }
	      };//SpellAbility
	      
	      Input runtime = new Input()
	      {
	        private static final long serialVersionUID = -4803541385354247499L;

			public void showMessage()
	        {
	          CardList saps = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	          saps = saps.getType("Saproling");
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, saps, "Select a Saproling to sacrifice.", false));
	        }
	      };  
	      
	      card.addSpellAbility(a2);
	      a2.setDescription("Sacrifice a Saproling: You gain 2 life.");
	      a2.setStackDescription(card.getController() + " gains 2 life.");
	      a2.setBeforePayMana(runtime);
    	
	  
    }//*************** END ************ END **************************
    
    else if (cardName.equals("Pallid Mycoderm"))
    {
    	
    	Command intoPlay = new Command()
        {
    	  private static final long serialVersionUID = 3400057700040211691L;
		public boolean firstTime = true;
          public void execute()
          {

            if(firstTime){
          	  card.setCounter(Counters.SPORE, 0); 
            }
            firstTime = false;
          }
        };
         
        card.addComesIntoPlayCommand(intoPlay);
         
	    final SpellAbility a2 = new Ability(card, "0")
	    {
	      final Command eot1 = new Command()
	      {
	        private static final long serialVersionUID = -4485431571276851181L;

			public void execute()
	        {
	        	String player = card.getController();
	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
	        	
	        	CardList creats = new CardList(play.getCards());
	        	creats = creats.getType("Creature");
	        		
	        	for (int i=0; i < creats.size(); i++)
		        {
		      		Card creat = creats.get(i);
		      		
		      		if (creat.getType().contains("Fungus") || creat.getType().contains("Saproling") || creat.getKeyword().contains("Changeling"))
		      		{
		      			creat.addTempAttackBoost(-1);
		      			creat.addTempDefenseBoost(-1);
		      		}         	
		        }
		      	
	         }
	      };
	      public void resolve()
	      {
	        //get all player controls saprolings:
	        String player = card.getController();
	    	  
	        PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
	        
	        CardList creats = new CardList(play.getCards());
	        creats = creats.getType("Creature");
	        
	        @SuppressWarnings("unused") // saps
			CardList saps = new CardList();
	        
	        Card c = getTargetCard();
	        @SuppressWarnings("unused") // hand
			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
	
	        if(AllZone.GameAction.isCardInPlay(c))
	        {
	          //AllZone.getZone(c).remove(c);
	      	AllZone.GameAction.sacrifice(c);
	      	  
	          
	      	for (int i=0; i < creats.size(); i++)
	        {
	        	Card creat = creats.get(i);
	        	
	        	if (creat.getType().contains("Fungus") || creat.getType().contains("Saproling"))
	        	{
	      			creat.addTempAttackBoost(1);
	      			creat.addTempDefenseBoost(1);
	        	}
	        }
	          
	        }
	        AllZone.EndOfTurn.addUntil(eot1);
	      }
	      public boolean canPlayAI()
	      {
	        return false;
	      }
	    };//SpellAbility
	    
	    Input runtime = new Input()
	    {
	      private static final long serialVersionUID = 6754180514935882692L;

		public void showMessage()
	      {
	        CardList saps = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	        saps = saps.getType("Saproling");
	        
	        stopSetNext(CardFactoryUtil.input_targetSpecific(a2, saps, "Select a Saproling to sacrifice.", false));
	      }
	    };
	    
	    card.addSpellAbility(a2);
	    a2.setDescription("Sacrifice a Saproling: Each Fungus and each Saproling you control gets +1/+1 until end of turn");
	    a2.setStackDescription("Saprolings and Fungi you control get +1/+1 until end of turn.");
	
	    a2.setBeforePayMana(runtime);
	  }//*************** END ************ END **************************
	    //*************** START *********** START **************************
	  else if(cardName.equals("Psychotrope Thallid"))
	  {
		  Command intoPlay = new Command()
	      {
	  	   private static final long serialVersionUID = 8020106056714209199L;
		public boolean firstTime = true;
	         public void execute()
	         {
	
	           if(firstTime){
	         	  card.setCounter(Counters.SPORE, 0); 
	           }
	           firstTime = false;
	         }
	       };
	       
	       card.addComesIntoPlayCommand(intoPlay);
	
		    final SpellAbility a2 = new Ability(card, "1")
		    {
		        public void resolve()
		        {
		         	
		          Card c = getTargetCard();
		
		          if(AllZone.GameAction.isCardInPlay(c))
		          {
		            //AllZone.getZone(c).remove(c);
		        	AllZone.GameAction.sacrifice(c);
		        	  
		        	AllZone.GameAction.drawCard(c.getController());
		          }
		        }//resolve
		        public boolean canPlayAI()
		        {
		          //TODO: make AI able to use this
		          return false;
		        }
		      };//SpellAbility
		      
		      Input runtime = new Input()
		      {
		        private static final long serialVersionUID = -6388866343458002392L;
	
				public void showMessage()
		        {
		          CardList saps = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
		          saps = saps.getType("Saproling");
		          
		          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, saps, "Select a Saproling to sacrifice.", false));
		        }
		      };  
		      
		      card.addSpellAbility(a2);
		      a2.setDescription("1, Sacrifice a Saproling: You draw a card.");
		      a2.setStackDescription(card.getController() + " draws a card.");
		      a2.setBeforePayMana(runtime);
	    
	    	
	  }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	  else if(cardName.equals("Wall of Mulch"))
	  {
	
		    final SpellAbility a2 = new Ability(card, "G")
		    {
		        public void resolve()
		        {
	
		          Card c = getTargetCard();
		
		          if(AllZone.GameAction.isCardInPlay(c))
		          {
		            //AllZone.getZone(c).remove(c);
		        	AllZone.GameAction.sacrifice(c);
		        	AllZone.GameAction.drawCard(c.getController());
		          }
		        }//resolve
		        public boolean canPlayAI()
		        {
		          //TODO: make AI able to use this
		          return false;
		        }
		      };//SpellAbility
		      
		      Input runtime = new Input()
		      {
		        private static final long serialVersionUID = -4390488827563977718L;
	
				public void showMessage()
		        {
		          CardList walls = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
		          walls = walls.getType("Wall");
		          
		          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, walls, "Select a Wall to sacrifice.", false));
		        }
		      };  
		      
		      card.addSpellAbility(a2);
		      a2.setDescription("G, Sacrifice a Wall: You draw a card.");
		      a2.setStackDescription(card.getController() + " draws a card.");
		      a2.setBeforePayMana(runtime);
	    
	    	
	  }//*************** END ************ END **************************
	    
	
	 //*************** START *********** START **************************
	    else if (cardName.equals("Rootwater Thief"))
	    {
	        //final String player = card.getController();
	        //final String opponent = AllZone.GameAction.getOpponent(player);
	  	
		  	final Ability ability2 = new Ability(card, "2")
		  	{
		  		public void resolve()
		  		{
		  			String opponent = AllZone.GameAction.getOpponent(card.getController());
		  			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, opponent);
		  			CardList cards = new CardList(lib.getCards());
		  			
		  			if (cards.size() > 0 )
		  			{
			  			if (card.getController().equals(Constant.Player.Human))
			  			{
			  				Object o = AllZone.Display.getChoiceOptional("Select card to remove: ", cards.toArray());
			  				Card c = (Card)o;
			  				AllZone.GameAction.removeFromGame(c);
			  				AllZone.GameAction.shuffle(opponent);
			  			}
			  			else
			  			{
			  				Card c = lib.get(0);
			  				AllZone.GameAction.removeFromGame(c);
			  				AllZone.GameAction.shuffle(opponent);
			  			}
		  			}
		  			
		  		}
		  		public boolean canPlay()
		  		{
		  			//this is set to false, since it should only TRIGGER
		  			return false;
		  		}
		  	};// ability2
		  	//card.clearSpellAbility();
		  	card.addSpellAbility(ability2);
		  	ability2.setStackDescription(card.getName() + " - search opponent's library and remove a card from game.");
		  	
	   }//*************** END ************ END **************************
	    
	      
	    //*************** START *********** START **************************
	    else if (cardName.equals("Oros, the Avenger"))
	    {
		  	final Ability ability2 = new Ability(card, "2 W")
		  	{
		  		public void resolve()
		  		{
		  			//if (player.equals("Human"))
		  			//{
	  				CardList cards = new CardList();
	  				PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	  				PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	  				cards.addAll(hum.getCards());
	  				cards.addAll(comp.getCards());
	  				cards = cards.getType("Creature");
	  				
	  				for (int i=0;i<cards.size(); i++)
	  				{
	  					if (!CardUtil.getColors(cards.get(i)).contains(Constant.Color.White) 
	  						&& CardFactoryUtil.canDamage(card, cards.get(i)))
	  					{
	  						cards.get(i).addDamage(3, card);
	  					}
	  				}
		  			//}
		  		}
		  		public boolean canPlay()
		  		{
		  			//this is set to false, since it should only TRIGGER
		  			return false;
		  		}
		  	};// ability2
		  	//card.clearSpellAbility();
		  	card.addSpellAbility(ability2);
		  	ability2.setStackDescription(card.getName() + " - deals 3 damage to each nonwhite creature.");
		
		  	
	   }//*************** END ************ END **************************
	    

	  //*************** START *********** START **************************
	  else if (cardName.equals("Treva, the Renewer"))
	  {
	    final String player = card.getController();

		final Ability ability2 = new Ability(card, "2 W")
		{
			public void resolve()
			{
				int lifeGain = 0;
				if (card.getController().equals("Human"))
				{
					String choices[] = {"white", "blue" , "black" , "red" , "green"};
					Object o = AllZone.Display.getChoiceOptional("Select Color: ", choices);
					System.out.println("Color:" + o);
					lifeGain = CardFactoryUtil.getNumberOfPermanentsByColor((String)o);
					
				}
				else
				{
					PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
					PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
					CardList list = new CardList();
					list.addAll(cPlay.getCards());
					list.addAll(hPlay.getCards());
					String color = CardFactoryUtil.getMostProminentColor(list);
					lifeGain = CardFactoryUtil.getNumberOfPermanentsByColor(color);
				}
							
				PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
				life.addLife(lifeGain);
			}
			public boolean canPlay()
			{
				//this is set to false, since it should only TRIGGER
				return false;
			}
		};// ability2
		//card.clearSpellAbility();
		card.addSpellAbility(ability2);
		ability2.setStackDescription(card.getName() + " - " + player + " gains life equal to permanents of the chosen color.");

		
	 }//*************** END ************ END **************************
	      
	      
	  //*************** START *********** START **************************
	  else if (cardName.equals("Rith, the Awakener"))
	  {
	    final String player = card.getController();

		final Ability ability2 = new Ability(card, "2 G")
		{
			public void resolve()
			{
				int numberTokens = 0;
				if (card.getController().equals("Human"))
				{
					String choices[] = {"white", "blue" , "black" , "red" , "green"};
					Object o = AllZone.Display.getChoiceOptional("Select Color: ", choices);
					//System.out.println("Color:" + o);
					numberTokens = CardFactoryUtil.getNumberOfPermanentsByColor((String)o);
				}
				else
				{
					PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
					PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
					CardList list = new CardList();
					list.addAll(cPlay.getCards());
					list.addAll(hPlay.getCards());
					String color = CardFactoryUtil.getMostProminentColor(list);
					numberTokens = CardFactoryUtil.getNumberOfPermanentsByColor(color);
				}
				
				for (int i=0;i<numberTokens;i++)
				{
					makeToken();
				}
			}
			
			void makeToken()
	        {
	          Card c = new Card();

	          c.setName("Saproling");
	          c.setImageName("G 1 1 Saproling");

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setManaCost("G");
	          c.setToken(true);
	         
	          c.addType("Creature");
	          c.addType("Saproling");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
			
			public boolean canPlay()
			{
				//this is set to false, since it should only TRIGGER
				return false;
			}
		};// ability2
		//card.clearSpellAbility();
		card.addSpellAbility(ability2);
		ability2.setStackDescription(card.getName() + " - " + player + " puts a 1/1 green Saproling creature token onto the battlefield for each permanent of the chosen color");

		
	 }//*************** END ************ END **************************
	      
	      
	      //*************** START *********** START **************************
	  else if (cardName.equals("Vorosh, the Hunter"))
	  {

		final Ability ability2 = new Ability(card, "2 G")
		{
			public void resolve()
			{
				card.addCounter(Counters.P1P1, 6);
			}

			public boolean canPlay()
			{
				//this is set to false, since it should only TRIGGER
				return false;
			}
		};// ability2
		//card.clearSpellAbility();
		card.addSpellAbility(ability2);
		ability2.setStackDescription(card.getName() + " - gets six +1/+1 counters.");

		
	 }//*************** END ************ END **************************
    
    
  //*************** START *********** START **************************  
	  else if(cardName.equals("Anodet Lurker"))
	  {
		  
		  final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        	PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	            life.addLife(3);
	        }
	      };
	      
		  Command gain3Life = new Command()
	      {
			private static final long serialVersionUID = 9156307402354672176L;

			public void execute()
	         {
	        	 ability.setStackDescription(card.getName()+ " - Gain 3 life.");
	             AllZone.Stack.add(ability);
	         }
	       };
	       
	       card.addDestroyCommand(gain3Life);
	  }//*************** END ************ END **************************  

	    
	  //*************** START *********** START **************************  
	  else if(cardName.equals("Tarpan"))
	  {
		  
		  final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        	PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	            life.addLife(1);
	        }
	      };
	      
		  Command gain1Life = new Command()
	      {
			private static final long serialVersionUID = 206350020224577500L;

			public void execute()
	         {
	        	 ability.setStackDescription(card.getName()+ " - Gain 1 life.");
	             AllZone.Stack.add(ability);
	         }
	       };
	       
	       card.addDestroyCommand(gain1Life);
	  }//*************** END ************ END **************************  
	    
	  //*************** START *********** START **************************  
	  else if(cardName.equals("Onulet"))
	  {
		  
		  final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        	PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	            life.addLife(2);
	        }
	      };
	      
		  Command gain2Life = new Command()
	      {
			private static final long serialVersionUID = 7840609060047275126L;

			public void execute()
	         {
	        	 ability.setStackDescription(card.getName()+ " - Gain 2 life.");
	             AllZone.Stack.add(ability);
	         }
	       };
	       
	       card.addDestroyCommand(gain2Life);

	    
	  }//*************** END ************ END **************************  
	    
	  //*************** START *********** START **************************  
	  else if(cardName.equals("Sprouting Thrinax"))
	  {   
	 	 
	 	 final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	     	   makeToken();
	     	   makeToken();
	     	   makeToken();
	        }//resolve()
	        
	        void makeToken()
	        {
	          Card c = new Card();

	          c.setName("Saproling");
	          c.setImageName("G 1 1 Saproling");

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setManaCost("G");
	          c.setToken(true);
	         
	          c.addType("Creature");
	          c.addType("Saproling");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//Ability
	 	 
	 	 Command make3Tokens = new Command()
	      {
			private static final long serialVersionUID = 5246587197020320581L;

	         public void execute()
	         {
	         	ability.setStackDescription(card.getName()+ " - put three 1/1 Saproling creature tokens into play.");
	            AllZone.Stack.add(ability);
	         }
	      };
	      
	      card.addDestroyCommand(make3Tokens);
	      
	   }//*************** END ************ END **************************
	    
	    
	    
	  //*************** START *********** START **************************
	  else if(cardName.equals("Solemn Simulacrum"))
	  {   
	 	 
	 	 final Ability ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	     	   PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	 	       	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	 	       	
	 	       	CardList basic = new CardList(lib.getCards());
	 	       	basic = basic.getType("Basic");
	 	       	
	 	       	if (card.getController().equals(Constant.Player.Computer))
	 	       	{
	 	       		if (basic.size() > 0)
	 	       		{
	 	       			Card c = basic.get(0);
	 	       			lib.remove(c);
	 	       			play.add(c);
	 	       			c.tap();
	 	       			
	 	       		}
	 	       	}
	 	       	else // human
	 	       	{
	 	       		if (basic.size() > 0) 
	 	       		{
	 	       			Object o = AllZone.Display.getChoiceOptional("Select Basic Land card to put into play tapped: ", basic.toArray());
	 	       			if (o != null)
	 	       			{	
	 	       				Card c = (Card)o;
	 	       				lib.remove(c);
	 	       				play.add(c);
	 	       				c.tap();
	 	       			}
	 	       		}
	 	       	}
	 	       	AllZone.GameAction.shuffle(card.getController());
	        }//resolve()
	      };//Ability
	 	 
	 	 Command fetchBasicLand = new Command()
	      {
			private static final long serialVersionUID = -7912757481694029348L;

	         public void execute()
	         {
	         	ability.setStackDescription(card.getName()+ " - search library for a basic land card and put it into play tapped.");
	            AllZone.Stack.add(ability);
	         }
	      };
	      
	      final Ability ability2 = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	     	   
	 	       	AllZone.GameAction.drawCard(card.getController());
	        }//resolve()
	      };//Ability
	      
	      Command draw = new Command()
	      {
			private static final long serialVersionUID = -549395102229088642L;

	         public void execute()
	         {
	         	ability2.setStackDescription(card.getName()+ " - Draw a card.");
	            AllZone.Stack.add(ability2);
	         }
	      };

	      card.addDestroyCommand(draw);
	      card.addComesIntoPlayCommand(fetchBasicLand);
	      
	   }//*************** END ************ END **************************
	      
	  
	 
	    //*************** START *********** START **************************
	    if (cardName.equals("Master Transmuter"))
	    {
	    	final Ability_Tap ability = new Ability_Tap(card, "U")
	    	{

				private static final long serialVersionUID = -9076784333448226913L;

				public void resolve() {
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
					PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
					CardList artifacts = new CardList(hand.getCards());
					artifacts = artifacts.getType("Artifact");
					if (card.getController().equals(Constant.Player.Human))
					{
						Object o = AllZone.Display.getChoiceOptional("Select artifact to put onto the battlefield: ", artifacts.toArray());
						if(o!=null)
						{
							Card c = (Card)o;
							hand.remove(c);
							play.add(c);
						}
					}
					else
					{
						//CardList arts = new CardList(play.getCards());
						//arts = arts.getType("Artifact");
						
						Card c = getTargetCard();
						AllZone.GameAction.moveTo(hand, c);
						
						Card crd = CardFactoryUtil.AI_getMostExpensivePermanent(artifacts, card, false);
						hand.remove(crd);
						play.add(crd);
						
					}
				}
				
				public boolean canPlayAI()
				{
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
					PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
					
					CardList handArts = new CardList(hand.getCards());
					handArts = handArts.getType("Artifact");
					
					CardList playArts = new CardList(play.getCards());
					playArts = playArts.getType("Artifact");
					
					if (handArts.size() > 0 && playArts.size() > 0){
						
						if  (CardUtil.getConvertedManaCost(CardFactoryUtil.AI_getCheapestPermanent(playArts, card, false).getManaCost()) < 
							   CardUtil.getConvertedManaCost(CardFactoryUtil.AI_getMostExpensivePermanent(handArts, card, false).getManaCost()))
						{
							setTargetCard(CardFactoryUtil.AI_getCheapestPermanent(playArts, card, false));
							return true;
						}
					
					}
					return false;
				}
	    	};
	    	
	    	Input target = new Input()
	        {
					
			private static final long serialVersionUID = 4246650335595231655L;
			public void showMessage()
	          {
	            AllZone.Display.showMessage("Select artifact to return to hand");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card c, PlayerZone zone)
	          {
	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play,card.getController());
	            if(c.isArtifact() && AllZone.GameAction.isCardInZone(c, play))
	            {
	              PlayerZone hand = AllZone.getZone(Constant.Zone.Hand,card.getController());
	              
	              AllZone.GameAction.moveTo(hand, c);
	              AllZone.Stack.add(ability);
	              stopSetNext(new ComputerAI_StackNotEmpty());
	            }
	          }//selectCard()
	        };//Input
	    	
	        card.addSpellAbility(ability);
	        ability.setDescription("U, tap, Return an artifact you control to its owner's hand: You may put an artifact card from your hand onto the battlefield.");
	        ability.setStackDescription(card + "You may put an artifact card from your hand onto the battlefield");
	        ability.setAfterPayMana(target);
	    	
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Hanna, Ship's Navigator"))
	    {
	    	final Ability_Tap ability = new Ability_Tap(card, "1 U W")
	        {
	          private static final long serialVersionUID = 7959233413572648987L;

			public void resolve()
	          {
	          	String player = card.getController();
	              if(player.equals(Constant.Player.Human))
	                humanResolve();
	              else
	                computerResolve();
	          }//resolve()
	          public void humanResolve()
	          {
	            CardList cards = new CardList(AllZone.Human_Graveyard.getCards());
	            //legends = legends.getType().contains("Legendary");
	            CardList list = new CardList();
	            
	            for (int i=0;i < cards.size(); i++)
	            {
	          	  //System.out.println("type: " +cards.get(i).getType());
	          	  if (cards.get(i).getType().contains("Artifact") || cards.get(i).getType().contains("Enchantment")){
	          		  //System.out.println(cards.get(i).getName());
	          		  Card c = cards.get(i);
	          		  list.add(c);
	          		  
	          	  }
	            }
	            
	            if (list.size() != 0) {          
	  	          Object check = AllZone.Display.getChoiceOptional("Select Artifact or Enchantment", list.toArray());
	  	          if(check != null)
	  	          {
	  	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	  	            AllZone.GameAction.moveTo(hand, (Card)check);
	  	          }
	            }
	          }
	          public void computerResolve()
	          {
	            Card[] grave = AllZone.Computer_Graveyard.getCards();
	            CardList list = new CardList(grave);
	            CardList artenchants = new CardList();
	            //list = list.getType("Creature");

	            for (int i=0;i < list.size(); i++)
	            {
	          	  if (list.get(i).getType().contains("Artifact") || list.get(i).getType().contains("Enchantment")){
	          		  Card k = list.get(i);
	          		  artenchants.add(k);
	          	  }
	            
	            }
	            
	            //pick best artifact / enchantment
	            if (artenchants.size() != 0){
	  	          Card c = CardFactoryUtil.AI_getBestArtifact(list);
	  	          if(c == null)
	  	        	c = CardFactoryUtil.AI_getBestEnchantment(list,card, true);
	  	          if(c == null)
	  	            c = grave[0];
	  	          System.out.println("computer picked - " +c);
	  	          AllZone.Computer_Graveyard.remove(c);
	  	          AllZone.Computer_Hand.add(c);
	            }
	          }//computerResolve
	          
	          public boolean canPlay(){
	        	  String controller = card.getController();
	        	  
	        	  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, controller);
	        	  CardList list = new CardList(grave.getCards());
	        	  CardList cards = new CardList();
	        	  
	        	  for (int i=0;i<list.size();i++)
	        	  {
	        		  if (list.get(i).getType().contains("Artifact") || list.get(i).getType().contains("Enchantment")){
	        			  cards.add(list.get(i));
	        		  }
	        	  }
	        	  
	        	  //System.out.println("cards.size(): " + cards.size());
	        	  //hasSickness is a little hack, I'm not sure about the setBeforeMana input
	        	  if (cards.size() > 0 && AllZone.GameAction.isCardInPlay(card) && !card.hasSickness() && card.isUntapped())
	        		  return true;
	        	  else 
	        		  return false;
	          }
	          
	          
	        };//SpellAbility
	        //card.addSpellAbility(ability);
	        ability.setDescription("1 U W, tap: Return target artifact or enchantment card from your graveyard to your hand.");
	        ability.setBeforePayMana(new Input_PayManaCost(ability));
	        ability.setStackDescription("Hanna, Ship's Navigator - Returns an artifact or enchantment card from graveyard to hand.");
	        card.addSpellAbility(ability);
	  	  
	        
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    if (cardName.equals("Cromat")){
		   
	    //LibBounce Ability
	    final Ability a1 = new Ability(card, "G U")
	    {
	      public boolean canPlayAI() {return false;}

	      public void resolve()
	      {
	        if(AllZone.GameAction.isCardInPlay(card) )
	        {
	          card.setBaseAttack(5);
	          card.setBaseDefense(5);

	          card.clearAssignedDamage();
	          card.setDamage(0);
	          card.untap();
	          AllZone.getZone(card).remove(card);

	          //put card on top of library
	          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getOwner());
	          library.add(card, 0);
	        }
	      }//resolve()
	    };//SpellAbility

	    Input runtime1 = new Input()
	    {
	      private static final long serialVersionUID = 1469011418219527227L;

	      public void showMessage()
	      {
	        a1.setStackDescription("Put " +card +" on top of its owner's library");

	        stopSetNext(new Input_PayManaCost(a1));
	      }
	    };
	    a1.setDescription("G U: Put Cromat on top of its owner's library.");
	    a1.setStackDescription("Put Cromat on top of its owner's library.");
	    card.addSpellAbility(a1);
	    a1.setBeforePayMana(runtime1);
	    //Kill ability
	   
	    final Ability a2 = new Ability(card, "W B"){
	       public boolean canPlay()
	       {
	          return (AllZone.GameAction.isCardInPlay(card) && (AllZone.Combat.isBlocked(card) || AllZone.Combat.getAllBlockers().contains(card)));
	       }
	        public boolean canPlayAI()
	        {
	          return false;
	          //TODO
	        }
	       public void resolve()
	       {
	          AllZone.GameAction.destroy(getTargetCard());
	       }//resolve()
	    };
	    Input runtime2 = new Input()
	    {
	      private static final long serialVersionUID = 1L;

	      public void showMessage()
	       {
	          CardList targets = new CardList();
	          if(AllZone.Combat.isBlocked(card) || AllZone.Combat.getAllBlockers().contains(card)){
	             if (AllZone.Combat.isBlocked(card)){ targets = AllZone.Combat.getBlockers(card); }
	             else {
	                targets = new CardList();
	                for (Card c : AllZone.Combat.getAttackers()){
	                   if (AllZone.Combat.isBlocked(c))
	                      if (AllZone.Combat.getBlockers(c).contains(card)) targets.add(c);
	                }
	             }
	          }
	          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, targets, "Select target blocking or blocked by Cromat.", true));
	       }
	    };
	    card.addSpellAbility(a2);
	    a2.setBeforePayMana(runtime2);
	    a2.setDescription("W B: Destroy target creature blocking or blocked by Cromat.");
	  }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Energizer"))
	    {
	       Ability_Tap ability = new Ability_Tap(card, "2")
	       {
			private static final long serialVersionUID = 6444406158364728638L;
			public void resolve()
	          {
	             card.addCounter(Counters.P1P1,1);
	          }
	          public boolean canPlayAI()
	          {
	             return(true);
	          }
	       };
	       ability.setDescription("2, T: Put a +1/+1 counter on Energizer.");
	       ability.setStackDescription("Put a +1/+1 counter on target Energizer.");
	       card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Sphinx of Jwar Isle"))
	    {
	    	final SpellAbility ability1 = new Ability(card, "0")
	        {
	          public void resolve()
	          {
			    	String player = card.getController();
			    	PlayerZone lib =  AllZone.getZone(Constant.Zone.Library, player);
			    	
			    	if (lib.size() < 1)
			    		return;
			    	
			    	CardList cl = new CardList();
			    	cl.add(lib.get(0));
			    	
			    	AllZone.Display.getChoiceOptional("Top card", cl.toArray());
	          }
	          public boolean canPlayAI()
	          {
	        	  return false;
	          }
	        };
	        
	        ability1.setStackDescription(card.getName() + " - look at top card of library.");
	        ability1.setDescription("You may look at the top card of your library.");
	        card.addSpellAbility(ability1);
	    }
	  //*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Imperial Recruiter"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
	        	hand.add(c, 0);  	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -8887306085997352723L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList powerTwoCreatures = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Creature") && (cards.get(i).getNetAttack() <= 2))
	        	  {
	        		  powerTwoCreatures.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(powerTwoCreatures.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", powerTwoCreatures.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	        	powerTwoCreatures.shuffle();
	            ability.setTargetCard(powerTwoCreatures.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Maggot Carrier"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = card;
	          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	          PlayerLife oppLife = AllZone.GameAction.getPlayerLife(AllZone.GameAction.getOpponent(c.getController()));
	          life.subtractLife(1);
	          oppLife.subtractLife(1);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 685222802927427442L;

			public void execute()
	        {
	          ability.setStackDescription("Maggot Carrier - everyone loses 1 life.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Rathi Fiend"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = card;
	          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	          PlayerLife oppLife = AllZone.GameAction.getPlayerLife(AllZone.GameAction.getOpponent(c.getController()));
	          life.subtractLife(3);
	          oppLife.subtractLife(3);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 3362571791271852381L;

			public void execute()
	        {
	          ability.setStackDescription("Rathi Fiend - everyone loses 3 life.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Dream Stalker"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = getTargetCard();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            AllZone.getZone(c).remove(c);

	            if(! c.isToken())
	            {
	              Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
	              hand.add(newCard);
	            }
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 2045940121508110423L;

			public void execute()
	        {
	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	        	CardList choice = new CardList(play.getCards());
	        	AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, choice, "Select a permanent you control.", false));
	            ButtonUtil.disableAll();
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
	        private static final long serialVersionUID = 4802059067438200061L;

			public boolean canPlayAI()
	        {
	          return false;
	        }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Horned Kavu") || cardName.equals("Shivan Wurm"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = getTargetCard();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            AllZone.getZone(c).remove(c);

	            if(! c.isToken())
	            {
	              Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
	              hand.add(newCard);
	            }
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 7530032969328799083L;

			public void execute()
	        {
	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	        	
	        	CardList creatures = new CardList(play.getCards());
	        	creatures = creatures.getType("Creature");
	        	
	        	CardList redGreen = new CardList();
	        	
	        	
	        	for(int i=0;i <creatures.size(); i++)
	        	{
	        		//if(!CardUtil.getColors(nonBlackCards.get(i)).contains(Constant.Color.Black))
	        		if (CardUtil.getColors(creatures.get(i)).contains(Constant.Color.Red))
	        		{
	        			redGreen.add(creatures.get(i));
	        		}
	        		else if (CardUtil.getColors(creatures.get(i)).contains(Constant.Color.Green))
	        		{
	        			redGreen.add(creatures.get(i));
	        		}
	        	}
	        	
	        	//Object o = AllZone.Display.getChoiceOptional("Select a creature card to bounce", blackBlue.toArray());
	        	
	        	
	        	AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, redGreen, "Select a red or green creature you control.", false));
	            ButtonUtil.disableAll();
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();

	      card.addSpellAbility(new Spell_Permanent(card)
	      {
	        private static final long serialVersionUID = -517667816379595978L;

			public boolean canPlayAI()
	        {
	          return false;
	        }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    if(cardName.equals("Silver Drake"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = getTargetCard();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            AllZone.getZone(c).remove(c);

	            if(! c.isToken())
	            {
	              Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
	              hand.add(newCard);
	            }
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {

			private static final long serialVersionUID = -8473976122518500976L;

			public void execute()
	        {
	           PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	           
	           CardList creatures = new CardList(play.getCards());
	           creatures = creatures.getType("Creature");
	           
	           CardList whiteBlue = new CardList();
	           
	           
	           for(int i=0;i <creatures.size(); i++)
	           {
	              //if(!CardUtil.getColors(nonBlackCards.get(i)).contains(Constant.Color.Black))
	              if (CardUtil.getColors(creatures.get(i)).contains(Constant.Color.White))
	              {
	                 whiteBlue.add(creatures.get(i));
	              }
	              else if (CardUtil.getColors(creatures.get(i)).contains(Constant.Color.Blue))
	              {
	                 whiteBlue.add(creatures.get(i));
	              }
	           }
	           
	           AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, whiteBlue, "Select a white or blue creature you control.", false));
	            ButtonUtil.disableAll();
	         
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();

	      card.addSpellAbility(new Spell_Permanent(card)
	      {

			private static final long serialVersionUID = -5048658151377675270L;

			public boolean canPlayAI()
			{
		          return false;
		    }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Fleetfoot Panther") || cardName.equals("Steel Leaf Paladin"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = getTargetCard();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            AllZone.getZone(c).remove(c);

	            if(! c.isToken())
	            {
	              Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
	              hand.add(newCard);
	            }
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 6575359591031318957L;

			public void execute()
	        {
	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	        	
	        	CardList creatures = new CardList(play.getCards());
	        	creatures = creatures.getType("Creature");
	        	
	        	CardList greenWhite = new CardList();
	        	
	        	
	        	for(int i=0;i <creatures.size(); i++)
	        	{
	        		//if(!CardUtil.getColors(nonBlackCards.get(i)).contains(Constant.Color.Black))
	        		if (CardUtil.getColors(creatures.get(i)).contains(Constant.Color.Green))
	        		{
	        			greenWhite.add(creatures.get(i));
	        		}
	        		else if (CardUtil.getColors(creatures.get(i)).contains(Constant.Color.White))
	        		{
	        			greenWhite.add(creatures.get(i));
	        		}
	        	}
	        	
	        	//Object o = AllZone.Display.getChoiceOptional("Select a creature card to bounce", blackBlue.toArray());
	        	
	        	
	        	AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, greenWhite, "Select a green or white creature you control.",false));
	            ButtonUtil.disableAll();
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();

	      card.addSpellAbility(new Spell_Permanent(card)
	      {
	        private static final long serialVersionUID = -1408300578781963711L;

			public boolean canPlayAI()
	        {
	          return false;
	        }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Stonecloaker"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          
	          Card c = getTargetCard();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            AllZone.getZone(c).remove(c);

	            if(! c.isToken())
	            {
	              Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
	              hand.add(newCard);
	            }
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
	        private static final long serialVersionUID = -4018162972761688814L;

			public void execute()
	        {
	        	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.GameAction.getOpponent(card.getController()));
	            CardList gravecards = new CardList(grave.getCards());
	                  
	            //System.out.println("size of grave: " + gravecards.size());
	            
	            if (gravecards.size() > 0)
	            {
	              if (card.getController().equals("Human"))
	              {
		          	  Object o = AllZone.Display.getChoiceOptional("Select a card in opponent's graveyard to remove", gravecards.toArray());
		          	  if (o!=null)
		          	  {
		          		  Card removedCard = (Card)o;        		  
		          		  AllZone.GameAction.removeFromGame(removedCard);
		          	  }
	              }
	              else
	              {
	            	  AllZone.GameAction.removeFromGame(gravecards.get(0));
	              }
	            }
	            
	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	        	
	        	CardList creatures = new CardList(play.getCards());
	        	creatures = creatures.getType("Creature");
	        	
	        	//Object o = AllZone.Display.getChoiceOptional("Select a creature card to bounce", blackBlue.toArray());
	        	
	        	
	        	AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, creatures, "Select a creature you control.", false));
	            ButtonUtil.disableAll();
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();

	      card.addSpellAbility(new Spell_Permanent(card)
	      {
	        private static final long serialVersionUID = 3089921616375272120L;

			public boolean canPlayAI()
	        {
	          return false;
	        }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Cavern Harpy"))
	    {
	      final SpellAbility a1 = new Ability(card,"0")
	      {
	    	  public void resolve()
	          {
	    		  PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	    		  life.subtractLife(1);
	    		  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
	    		  
	    		  if (card.isToken())
	    			  AllZone.getZone(card).remove(card);
	    		  else
	    			  AllZone.GameAction.moveTo(hand, card);
	    		  
	    		  
	          }
	    	  public boolean canPlayAI()
	    	  {
	    		  return false;
	    	  }
	      };
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = getTargetCard();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            AllZone.getZone(c).remove(c);

	            if(! c.isToken())
	            {
	              Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
	              hand.add(newCard);
	            }
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
	        private static final long serialVersionUID = -7855081477395863590L;

			public void execute()
	        {
	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	        	
	        	CardList creatures = new CardList(play.getCards());
	        	creatures = creatures.getType("Creature");
	        	
	        	CardList blackBlue = new CardList();
	        	
	        	
	        	for(int i=0;i <creatures.size(); i++)
	        	{
	        		//if(!CardUtil.getColors(nonBlackCards.get(i)).contains(Constant.Color.Black))
	        		if (CardUtil.getColors(creatures.get(i)).contains(Constant.Color.Black))
	        		{
	        			blackBlue.add(creatures.get(i));
	        		}
	        		else if (CardUtil.getColors(creatures.get(i)).contains(Constant.Color.Blue))
	        		{
	        			blackBlue.add(creatures.get(i));
	        		}
	        	}
	        	
	        	//Object o = AllZone.Display.getChoiceOptional("Select a creature card to bounce", blackBlue.toArray());
	        	
	        	
	        	AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, blackBlue, "Select blue or black creature you control.", false));
	            ButtonUtil.disableAll();
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();
	     
	      card.addSpellAbility(new Spell_Permanent(card)
	      {
	        private static final long serialVersionUID = -6750896183003809261L;

			public boolean canPlayAI()
	        {
	          return false;
	        }
	      });
	      
	      card.addSpellAbility(a1);
	      a1.setStackDescription(card.getController() + " pays 1 life and returns Cavern Harpy back to owner's hand.");
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Nemata, Grove Guardian"))
	    {
	      final SpellAbility a1 = new Ability(card, "2 G")
	      {
	    	public boolean canPlayAI()
	    	{
	    		return MyRandom.random.nextBoolean();
	    	}
	    	public boolean canPlay()
	    	{
	    		SpellAbility sa;
	            //this is a hack, check the stack to see if this card has an ability on the stack
	            //if so, we can't use the ability
	            for (int i=0; i<AllZone.Stack.size(); i++)
	            {
	            	sa = AllZone.Stack.peek(i);
	            	if (sa.getSourceCard().equals(card))
	            			return false;
	            }
	            return AllZone.GameAction.isCardInPlay(card);
	    		
	    	}
	        public void resolve()
	        {
	          makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();

	          
	          c.setName("Saproling");
	          c.setImageName("G 1 1 Saproling");

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setManaCost("G");
	          c.setToken(true);
	         
	          c.addType("Creature");
	          c.addType("Saproling");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      
	      final SpellAbility a2 = new Ability(card, "0")
	      {
	        final Command eot1 = new Command()
	        {
			  private static final long serialVersionUID = -389286901477839863L;

			  public void execute()
	          {
	        	CardList saps = new CardList();
	        	saps.addAll(AllZone.Human_Play.getCards());
	        	saps.addAll(AllZone.Computer_Play.getCards());

	            saps = saps.getType("Saproling");
	        	
	        	for (int i=0; i < saps.size(); i++)
	            {
	        		Card sap = saps.get(i);
	        		     		
	                sap.addTempAttackBoost(-1);
	                sap.addTempDefenseBoost(-1);
	            }
	        	
	          }
	        };
	        public void resolve()
	        {
	          //get all saprolings:
	          
	          CardList saps = new CardList();
	          saps.addAll(AllZone.Human_Play.getCards());
	          saps.addAll(AllZone.Computer_Play.getCards());

	          saps = saps.getType("Saproling");
	        	
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            //AllZone.getZone(c).remove(c);
	        	AllZone.GameAction.sacrifice(c);
	        	  
	            for (int i=0; i < saps.size(); i++)
	            {
	            	Card sap = saps.get(i);
	            	
	            	sap.addTempAttackBoost(1);
	                sap.addTempDefenseBoost(1);          	
	            }
	            
	          }
	          else
	        	  return;
	          
	          AllZone.EndOfTurn.addUntil(eot1);
	        }
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	      };//SpellAbility
	      
	      Input runtime = new Input()
	      {
			private static final long serialVersionUID = -8827919636559042903L;

			public void showMessage()
	        {
	          CardList saps = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	          saps = saps.getType("Saproling");
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, saps, "Select a Saproling to sacrifice.",false));
	        }
	      };
	      a1.setDescription("2G: Put a 1/1 green Saproling creature token into play.");
	      a1.setStackDescription("Put a 1/1 Saproling into play.");
	      card.addSpellAbility(a1);
	      a1.setBeforePayMana(new Input_PayManaCost(a1));
	      
	      card.addSpellAbility(a2);
	      a2.setDescription("Sacrifice a Saproling: Saproling creatures get +1/+1 until end of turn");
	      a2.setStackDescription("Saprolings get +1/+1 until end of turn.");

	      a2.setBeforePayMana(runtime);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Fallen Angel"))
	    {
	      
	      final SpellAbility a2 = new Ability(card, "0")
	      {
	        final Command eot1 = new Command()
	        {
	          private static final long serialVersionUID = 8955432114774252848L;

			public void execute()
	          {
	                card.addTempAttackBoost(-2);
	                card.addTempDefenseBoost(-1);	
	          }
	        };
	        public void resolve()
	        {
	       	
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            //AllZone.getZone(c).remove(c);
	        	AllZone.GameAction.sacrifice(c);
	            
	        	if(AllZone.GameAction.isCardInPlay(card)) 
	        	{
	        		card.addTempAttackBoost(2);
	        		card.addTempDefenseBoost(1);
	        	}
	          }
	          AllZone.EndOfTurn.addUntil(eot1);
	        }
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	        public boolean canPlay()
	        {
	        	SpellAbility sa;
	            //this is a hack, check the stack to see if this card has an ability on the stack
	            //if so, we can't use the ability: this is to prevent using a limited ability too many times
	            for (int i=0; i<AllZone.Stack.size(); i++)
	            {
	            	sa = AllZone.Stack.peek(i);
	            	if (sa.getSourceCard().equals(card))
	            			return false;
	            }
	            if (super.canPlay())
	            	return true;
	            return false;
	        }
	      };//SpellAbility
	      
	      Input runtime = new Input()
	      {
	        private static final long serialVersionUID = -5917074526767992913L;

			public void showMessage()
	        {
	          CardList creats = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	          creats = creats.getType("Creature");
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, creats, "Select a creature to sacrifice.",false));
	        }
	      };

	      card.addSpellAbility(a2);
	      a2.setDescription("Sacrifice a creature: Fallen Angel gets +2/+1 until end of turn.");
	      a2.setStackDescription("Fallen Angel gets +2/+1 until end of turn.");

	      a2.setBeforePayMana(runtime);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Nantuko Husk") || cardName.equals("Phyrexian Ghoul"))
	    {
	      
	      final SpellAbility a2 = new Ability(card, "0")
	      {
	        final Command eot1 = new Command()
	        {
	          private static final long serialVersionUID = 4450272080079173250L;

			public void execute()
	          {
	                card.addTempAttackBoost(-2);
	                card.addTempDefenseBoost(-2);	
	          }
	        };
	        public void resolve()
	        {
	       	
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c))
	          {
	            //AllZone.getZone(c).remove(c);
	        	AllZone.GameAction.sacrifice(c);
	            
	        	if(AllZone.GameAction.isCardInPlay(card)) 
	        	{
	        		card.addTempAttackBoost(2);
	        		card.addTempDefenseBoost(2);
	        	}
	          }
	          AllZone.EndOfTurn.addUntil(eot1);
	        }
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	        public boolean canPlay()
	        {
	        	SpellAbility sa;
	            //this is a hack, check the stack to see if this card has an ability on the stack
	            //if so, we can't use the ability: this is to prevent using a limited ability too many times
	            for (int i=0; i<AllZone.Stack.size(); i++)
	            {
	            	sa = AllZone.Stack.peek(i);
	            	if (sa.getSourceCard().equals(card))
	            			return false;
	            }
	            if (super.canPlay())
	            	return true;
	            return false;
	        }
	      };//SpellAbility
	      
	      Input runtime = new Input()
	      {
	        private static final long serialVersionUID = 8445133749305465286L;

			public void showMessage()
	        {
	          CardList creats = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	          creats = creats.getType("Creature");
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, creats, "Select a creature to sacrifice.",false));
	        }
	      };

	      card.addSpellAbility(a2);
	      a2.setDescription("Sacrifice a creature: " +card.getName() +" gets +2/+2 until end of turn.");
	      a2.setStackDescription(card.getName() + " gets +2/+2 until end of turn.");

	      a2.setBeforePayMana(runtime);
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Cateran Overlord"))
	    {
	    
	    	final SpellAbility a2 = new Ability(card, "0")
	        {
	          final Command eot1 = new Command()
	          {
				private static final long serialVersionUID = -494422681034894502L;

				public void execute()
	            {
	            	card.setShield(0);
	            }
	          };
	          public void resolve()
	          {
	        	
	            Card c = getTargetCard();
	            @SuppressWarnings("unused") // hand
				PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());

	            if(AllZone.GameAction.isCardInPlay(c))
	            {
	              //AllZone.getZone(c).remove(c);
	            	AllZone.GameAction.sacrifice(c);
	          	  
	            	card.addShield();
	             
	              
	            }
	            AllZone.EndOfTurn.addUntil(eot1);
	          }
	          public boolean canPlayAI()
	          {
	        	  return false;
	          }
	               
	        };//SpellAbility
	        
	        Input runtime = new Input()
	        {
			  private static final long serialVersionUID = -8675314608938902218L;

			  public void showMessage()
	          {
	            CardList creats = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	            creats = creats.getType("Creature");
	            
	            stopSetNext(CardFactoryUtil.input_targetSpecific(a2, creats, "Select a creature to sacrifice.",false));
	          }
	        };
	    	
	    	card.addSpellAbility(a2);
	        a2.setDescription("Sacrifice a creature: Regenerate Cateran Overlord");
	        a2.setStackDescription("Regenerate Cateran Overlord");

	        a2.setBeforePayMana(runtime);
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Viridian Shaman") || cardName.equals("Uktabi Orangutan") || cardName.equals("Vithian Renegades") ||
	    		cardName.equals("Keldon Vandals"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	         public void resolve()
	        {
	          Card c = getTargetCard();
	          @SuppressWarnings("unused") // graveyard
			  PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, c.getOwner());

	          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	        	if(c.isToken())
	        		AllZone.getZone(c).remove(c);

	        	else 
	              AllZone.GameAction.destroy(c);
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 7530032969328799083L;

			public void execute()
	        {
	        	CardList all = new CardList();
	        	all.addAll(AllZone.Human_Play.getCards());
	        	all.addAll(AllZone.Computer_Play.getCards());
	        	all = all.filter(new CardListFilter(){
	        		public boolean addCard(Card c)
	        		{
	        			return c.isArtifact() && CardFactoryUtil.canTarget(card, c);
	        		}
	        	});
	        	
	        	CardList humanList = new CardList(AllZone.Human_Play.getCards());
	        	humanList = humanList.getType("Artifact");
	        	
	        	if (all.size() != 0) {
	        		
	        		if(card.getController().equals(Constant.Player.Human)) {
	        			AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, all, "Select target artifact.", true));
	        			ButtonUtil.disableAll();
	        		}
	        		else if (card.getController().equals(Constant.Player.Computer)) {
	        			if (humanList.size() > 0 ){
		        			Card human = CardFactoryUtil.AI_getBestArtifact(humanList);
		        			ability.setTargetCard(human);
		        			AllZone.Stack.add(ability);
	        			}
	        			else
	        			{
	        				Card comp = CardFactoryUtil.AI_getCheapestPermanent(all, card, true);
	        				ability.setTargetCard(comp);
	        				AllZone.Stack.add(ability);
	        			}
	        		}
	        	}
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();

	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -517667816379595978L;

			public boolean canPlayAI()
	        {
	        	CardList artifacts = new CardList();
	        	artifacts.addAll(AllZone.Human_Play.getCards());
	        	artifacts = artifacts.getType("Artifact");
	        	
	        	if (artifacts.size() > 0)
	        		return true;
	        	else 
	        		return false;    	
	        }
	      });
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Avalanche Riders"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	         public void resolve()
	        {
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	              AllZone.GameAction.destroy(c);
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -8270520707155953207L;

			public void execute()
	        {
	        	CardList all = new CardList();
	        	all.addAll(AllZone.Human_Play.getCards());
	        	all.addAll(AllZone.Computer_Play.getCards());
	        	all = all.filter(new CardListFilter(){
	        		public boolean addCard(Card c)
	        		{
	        			return c.isLand() && CardFactoryUtil.canTarget(card, c);
	        		}
	        	});
	        	
	        	CardList humanList = new CardList(AllZone.Human_Play.getCards());
	        	humanList = humanList.getType("Land");
	        	
	        	if (all.size() != 0) {
	        		
	        		if(card.getController().equals(Constant.Player.Human)) {
	        			AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, all, "Select target artifact.", true));
	        			ButtonUtil.disableAll();
	        		}
	        		else if (card.getController().equals(Constant.Player.Computer)) {
	        			if (humanList.size() > 0 ){
		        			ability.setTargetCard(humanList.get(0));
		        			AllZone.Stack.add(ability);
	        			}
	        			else
	        			{
	        				Card comp = CardFactoryUtil.AI_getCheapestPermanent(all, card, true);
	        				ability.setTargetCard(comp);
	        				AllZone.Stack.add(ability);
	        			}
	        		}
	        	}
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();

	      card.addSpellAbility(new Spell_Permanent(card)
	      {
			private static final long serialVersionUID = -517667816379595978L;

			public boolean canPlayAI()
	        {
	        	CardList lands = new CardList();
	        	lands.addAll(AllZone.Human_Play.getCards());
	        	lands = lands.getType("Land");
	        	
	        	if (lands.size() > 0)
	        		return true;
	        	else 
	        		return false;    	
	        }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Aven Cloudchaser") || cardName.equals("Cloudchaser Eagle") || cardName.equals("Monk Realist"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	         public void resolve()
	        {
	          Card c = getTargetCard();

	          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	        	if(c.isToken())
	        		AllZone.getZone(c).remove(c);

	        	else 
	              AllZone.GameAction.destroy(c);
	          }
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 8586704292133752803L;

			public void execute()
	        {
	        	CardList all = new CardList();
	        	all.addAll(AllZone.Human_Play.getCards());
	        	all.addAll(AllZone.Computer_Play.getCards());
	        	all = all.getType("Enchantment");
	        	
	        	
	        	if (all.size() != 0) {
	        		
	        		if(card.getController().equals(Constant.Player.Human)) {
	        			AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, all, "Select target enchantment.", true));
	        			ButtonUtil.disableAll();
	        		}
	        		else if (card.getController().equals(Constant.Player.Computer)) {
	        			Card human = CardFactoryUtil.AI_getBestEnchantment(all, card, true);
	        			ability.setTargetCard(human);
	        			AllZone.Stack.add(ability);
	        		}
	        	}
	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);

	      card.clearSpellAbility();

	      card.addSpellAbility(new Spell_Permanent(card)
	      {
	        private static final long serialVersionUID = -8467111038318551304L;

			public boolean canPlayAI()
	        {
	        	CardList ench = new CardList();
	        	ench.addAll(AllZone.Human_Play.getCards());
	        	ench = ench.getType("Enchantment");
	        	
	        	if (ench.size() > 0)
	        		return true;
	        	else 
	        		return false;    	
	        }
	      });
	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Rakka Mar"))
	    {
	      final SpellAbility a1 = new Ability_Tap(card, "R")
	      {
			private static final long serialVersionUID = -3868544882464692305L;
			
			public boolean canPlayAI()
	    	{
	    		return true;
	    				
	    	}
	        public void resolve()
	        {
	          makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Elemental");
	          c.setImageName("R 3 1 Elemental");
	          c.setManaCost("R");
	          c.setToken(true);
	          
	          c.addType("Creature");
	          c.addType("Elemental");
	          c.setBaseAttack(3);
	          c.setBaseDefense(1);
	          c.addIntrinsicKeyword("Haste");

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      card.addSpellAbility(a1);
	      a1.setDescription("R, T: Put a 3/1 red Elemental creature token with haste into play.");
	      a1.setStackDescription("Put a 3/1 red Elemental creature token with haste into play.");

	      a1.setBeforePayMana(new Input_PayManaCost(a1));
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Tolsimir Wolfblood"))
	    {
	      final SpellAbility a1 = new Ability_Tap(card)
	      {
			private static final long serialVersionUID = -1900858280382389010L;
			
			public boolean canPlayAI()
	    	{
	    		String controller = card.getController();
	    		
	    		PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
	    		CardList voja = new CardList(play.getCards());
	    		voja = voja.getName("Voja");
	    		
	    		if (voja.size() == 0)
	    			return true;
	    		else
	    			return false;
	    				
	    	}
	        public void resolve()
	        {
	          makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();

	          c.setName("Voja");
	          c.setImageName("GW 2 2 Voja");

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setManaCost("W G");
	          c.setToken(true);
	          
	          c.addType("Legendary");
	          c.addType("Creature");
	          c.addType("Wolf");
	          c.setBaseAttack(2);
	          c.setBaseDefense(2);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      card.addSpellAbility(a1);
	      a1.setDescription("T: Put a legendary 2/2 green and white Wolf creature token named Voja into play.");
	      a1.setStackDescription("Put a 2/2 white green Legendary Wolf creature token named Voja into play.");
	    }//*************** END ************ END **************************

	  //*************** START *********** START **************************
	    if (cardName.equals("Ranger of Eos"))
	    {
	  	  final SpellAbility ability = new Ability(card, "0")
	        {
	          public void resolve()
	          {
	        	  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	              PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	                     
	        	  
	              CardList cards = new CardList(lib.getCards());
	              CardList oneCostCreatures = new CardList();
	              
	              for (int i=0;i<cards.size();i++)
	              {
	            	  if(cards.get(i).getType().contains("Creature") && (CardUtil.getConvertedManaCost(cards.get(i).getManaCost()) <= 1))
	            	  {
	            		  oneCostCreatures.add(cards.get(i));
	            	  }
	              }

	              String controller = card.getController();

	              if(oneCostCreatures.size() == 0)
	                return;

	              if(controller.equals(Constant.Player.Human))
	              {
	                Object o = AllZone.Display.getChoiceOptional("Select First Creature", oneCostCreatures.toArray());
	                if(o != null)
	                {
	                  //ability.setTargetCard((Card)o);
	                  //AllZone.Stack.add(ability);
	                  Card c1 = (Card)o;
	                  lib.remove(c1);
	                  hand.add(c1);
	                  oneCostCreatures.remove(c1);
	                  
	                  if(oneCostCreatures.size() == 0)
	                      return;
	                  
	                  o = AllZone.Display.getChoiceOptional("Select Second Creature", oneCostCreatures.toArray());
	                  
	                  if(o != null)
	                  {
	                	  Card c2 = (Card)o;
	                      lib.remove(c2);
	                      hand.add(c2);
	                      
	                      oneCostCreatures.remove(c2);
	                  }
	                }
	                AllZone.GameAction.shuffle(controller);
	              }
	              else //computer
	              {
	  	          	oneCostCreatures.shuffle();
	  	          	if (oneCostCreatures.size() >= 1)
	  	          	{
	  	          		Card c1 = oneCostCreatures.getCard(0);
	  	          		lib.remove(c1);
	  	          		hand.add(c1);
	  	          		oneCostCreatures.remove(c1);
	  	          		
	  	          		if (oneCostCreatures.size() >= 1)
	  	          		{
	  	          			Card c2 = oneCostCreatures.getCard(0);
	  	              		lib.remove(c2);
	  	              		hand.add(c2);
	  	              		oneCostCreatures.remove(c2);
	  	          		
	  	          		}  	          		
	  	          		
	  	          	}
	                //ability.setTargetCard(powerTwoCreatures.get(0));
	                //AllZone.Stack.add(ability);
	  	          	AllZone.GameAction.shuffle(controller);
	              }
	            
	                   
	            //...
	          	
	          }//resolve()
	        };
	        Command intoPlay = new Command()
	        {
			  private static final long serialVersionUID = -5697680711324878027L;

			  public void execute()
	          {
	        	  ability.setStackDescription("Ranger of Eos - Grab 2 creatures");
	        	  AllZone.Stack.add(ability);
	          }//execute()
	        };//Command
	        //ability.setStackDescription("Ranger of Eos - Grab 2 creatures");
	        //AllZone.Stack.add(ability);
	        card.addComesIntoPlayCommand(intoPlay);
	  	  
	  	  
	             
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Scarblade Elite"))
	    {
	    	final Ability_Tap ability = new Ability_Tap(card)
	        {
			  private static final long serialVersionUID = 3505019464802566898L;
			
			  public boolean canPlay()
	          {
	        	  String controller = card.getController();
	        	  PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard ,controller);
	        	  
	        	  CardList sins = new CardList(graveyard.getCards());
	        	  sins = sins.getType("Assassin");
	        	  
	        	  if (sins.size() > 0 && AllZone.GameAction.isCardInPlay(card) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	        		  return true;
	        	  else 
	        		  return false;
	        	  
	          }
	          public boolean canPlayAI()
	          {
	            CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
	            human = human.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c) {return AllZone.GameAction.isCardInPlay(c);}
	            });

	            CardListUtil.sortAttack(human);
	            CardListUtil.sortFlying(human);

	            //if(0 < human.size())
	            //  setTargetCard(human.get(0));
	            
	            PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer);
	            
	            CardList grave = new CardList(graveyard.getCards());
	            grave = grave.getType("Assassin");
	            
	            if (human.size() > 0 && grave.size() > 0)
	            	setTargetCard(human.get(0));
	            
	            return 0 < human.size() && 0 < grave.size();
	          }

	          public void resolve()
	          {
	        	String controller = card.getController();
	        	PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard ,controller);
	        	  
	        	CardList sins = new CardList(graveyard.getCards());
	        	sins = sins.getType("Assassin");
	        	
	        	if (card.getController().equals(Constant.Player.Human))
	        	{
		        	Object o = AllZone.Display.getChoiceOptional("Pick an Assassin to remove", sins.toArray());
		        	
			        if(o != null)
			        {
			          Card crd = (Card)o;	
			          graveyard.remove(crd);
				      
			        	  
			          Card c = getTargetCard();
			
			          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) )
			          {
			            AllZone.GameAction.destroy(c);
			          }
			        } //if o!= null
		       	}//player.equals("human")
	        	else
	        	{
	        		Card crd = sins.get(0);
	        		graveyard.remove(crd);
	        		
	        		Card c = getTargetCard();
	        		if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) )
			        {
			            AllZone.GameAction.destroy(c);
			        }
	        	}
	          }//resolve()
	        };//SpellAbility

	        Input target = new Input()
	        {
			  private static final long serialVersionUID = -4853162388286494888L;
			
			  public void showMessage()
	          {
	            AllZone.Display.showMessage("Select target creature to destroy");
	            ButtonUtil.enableOnlyCancel();
	          }
	          public void selectButtonCancel() {stop();}
	          public void selectCard(Card c, PlayerZone zone)
	          {
	        	if(!CardFactoryUtil.canTarget(ability, card)){
	            	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
	            }
	            if(c.isCreature() && zone.is(Constant.Zone.Play))
	            {
	              //tap ability
	              card.tap();

	              ability.setTargetCard(c);
	              AllZone.Stack.add(ability);
	              stop();
	            }
	          }//selectCard()
	        };//Input

	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Remove an Assassin card in your graveyard from the game: Destroy target creature.");
	        ability.setBeforePayMana(target);
	    	
	    
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	  else if(cardName.equals("Broodmate Dragon"))
	  {
		  final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

	          c.setName("Dragon");
	          c.setImageName("R 4 4 Dragon");
	          c.setManaCost("R");
	          c.setToken(true);

	          c.addType("Creature");
	          c.addType("Dragon");
	          
	          c.addIntrinsicKeyword("Flying");

	          c.setBaseAttack(4);
	          c.setBaseDefense(4);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);

	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 2848700532090223394L;

			public void execute()
	        {
	          ability.setStackDescription("Broodmate Dragon - put a 4/4 red Dragon creature token into play.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    
	  }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	  else if(cardName.equals("Deranged Hermit"))
	  { 
		  final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        	for (int i=0;i<4;i++)
	        		makeToken();
	        }//resolve()

	        void makeToken()
			{
				  Card c = new Card();

		          c.setOwner(card.getController());
		          c.setController(card.getController());

		          c.setName("Squirrel");
		          c.setImageName("G 1 1 Squirrel");
		          c.setManaCost("G");
		          c.setToken(true);

		          c.addType("Creature");
		          c.addType("Squirrel");

		          c.setBaseAttack(1);
		          c.setBaseDefense(1);

		          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		          play.add(c);
			 }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 7299232424224916928L;

			public void execute()
	        {
	          ability.setStackDescription("Deranged Hermit - put four green 1/1 Squirrel creature tokens onto the battlefield.");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    
	  }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	  else if(cardName.equals("Mogg War Marshal") || cardName.equals("Goblin Marshal"))
	  { 
		  final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	        	makeToken();
	        	if (card.getName().equals("Goblin Marshal"))
	        		makeToken();
	        }//resolve()

	        void makeToken()
			{
				  Card c = new Card();

		          c.setOwner(card.getController());
		          c.setController(card.getController());

		          c.setName("Goblin");
		          c.setImageName("R 1 1 Goblin");
		          c.setManaCost("R");
		          c.setToken(true);

		          c.addType("Creature");
		          c.addType("Goblin");

		          c.setBaseAttack(1);
		          c.setBaseDefense(1);

		          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		          play.add(c);
			 }
	      };
	      Command intoPlayDestroy = new Command()
	      {
			private static final long serialVersionUID = 5554242458006247407L;

			public void execute()
	        {
			  if (card.getName().equals("Mogg War Marshal"))
				  ability.setStackDescription("Mogg War Marshal - put a red 1/1 Goblin creature token onto the battlefield.");
			  else if (card.getName().equals("Goblin Marshal"))
				  ability.setStackDescription("Goblin Marshal - put two red 1/1 Goblin creature tokens onto the battlefield.");
	          
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlayDestroy);
	      card.addDestroyCommand(intoPlayDestroy);
	    
	  }//*************** END ************ END **************************
		
	    //*************** START *********** START **************************
	    else if(cardName.equals("Aven Riftwatcher"))
	    {
	       Command gain2Life = new Command()
	         {
				private static final long serialVersionUID = 5588978023269625349L;
				
				public boolean firstTime = true;
	            public void execute()
	            {
	              PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	              life.addLife(2);

	              //testAndSet - only needed when comes into play.
	              if(firstTime){
	            	  card.setCounter(Counters.AGE, 3); //vanishing
	              }
	              firstTime = false;
	            }
	          };

	      card.addLeavesPlayCommand(gain2Life);
	      card.addComesIntoPlayCommand(gain2Life);

	    }//*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Calciderm"))
	    {
	      Command ageCounters = new Command()
	      {
	    	    private static final long serialVersionUID = 431920157968451817L;
				public boolean firstTime = true;
	            public void execute()
	            {

	              //testAndSet - only needed when comes into play.
	              if(firstTime){
	            	  card.setCounter(Counters.AGE, 4); 
	              }
	              firstTime = false;
	            }
	      };
	      card.addComesIntoPlayCommand(ageCounters);

	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Blastoderm"))
	    {
	      Command fadeCounters = new Command()
	      {
	    	    private static final long serialVersionUID = -580691660706977218L;
				public boolean firstTime = true;
	            public void execute()
	            {

	              //testAndSet - only needed when comes into play.
	              if(firstTime){
	            	  card.setCounter(Counters.FADE, 3); 
	              }
	              firstTime = false;
	            }
	      };

	      card.addComesIntoPlayCommand(fadeCounters);

	    }//*************** END ************ END **************************

	    
	  
	    //*************** START *********** START **************************
	    else if(cardName.equals("Godsire"))
	    {
	    	 final SpellAbility a1 = new Ability_Tap(card)
	         {
			   private static final long serialVersionUID = -1160527561099142816L;
			   
			   public void resolve()
	           {
	             makeToken();
	           }
	           void makeToken()
	           {
	             Card c = new Card();

	             c.setName("Beast");
	             c.setImageName("RGW 8 8 Beast");

	             c.setOwner(card.getController());
	             c.setController(card.getController());

	             c.setManaCost("R G W");
	             c.setToken(true);             
	             
	             c.addType("Creature");
	             c.addType("Beast");
	             c.setBaseAttack(8);
	             c.setBaseDefense(8);

	             PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	             play.add(c);
	           }//makeToken()
	           
	           public boolean canPlayAI()
	           {
	        	   return AllZone.Phase.getPhase().equals("Main2");
	           }
	           
	         };//SpellAbility
	         card.addSpellAbility(a1);
	         a1.setDescription("T: Put an 8/8 Beast creature token into play that's red, green, and white.");
	         a1.setStackDescription("Put an 8/8 Beast creature token into play that's red, green, and white.");

	         //a1.setBeforePayMana(new Input_PayManaCost(a1));
	    }//*************** END ************ END **************************
	    
	
	    //*************** START *********** START **************************
	    else if(cardName.equals("Wydwen, the Biting Gale"))
	    {
	    	final SpellAbility a1 = new Ability(card,"U B")
	        {
	    	  public boolean canPlayAI()
	    	  {
	    		  return false;
	    	  }
	      	  public void resolve()
	            {
	      		  PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	      		  life.subtractLife(1);
	      		  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
	      		  /*
	      		  AllZone.getZone(card).remove(card);
	      		  hand.add(card);
	      		  */
	      		  if (card.isToken())
	    			  AllZone.getZone(card).remove(card);
	    		  else
	    			  AllZone.GameAction.moveTo(hand, card);
	      		  

	            }
	        };//a1
	        
	        //card.clearSpellAbility();
	        card.addSpellAbility(a1);
	        a1.setStackDescription(card.getController() + " pays 1 life and returns Wydwen back to owner's hand.");
	        a1.setDescription("U B, Pay 1 life: Return Wydwen, the Biting Gale to its owner's hand.");

	    
	   }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Tradewind Rider"))
	    {
	    	final SpellAbility a1 = new Ability_Tap(card,"0")
	        {
				private static final long serialVersionUID = 3438865371487994984L;

				public void chooseTargetAI()
	            {
	              if(getCreature().size() != 0)
	              {
	                Card bestCreature = CardFactoryUtil.AI_getBestCreature(getCreature());
	                if(getEnchantment().size() != 0){
	                  Card bestEnchantment = CardFactoryUtil.AI_getBestEnchantment(getEnchantment(), card, true);
	                  if(CardUtil.getConvertedManaCost(bestCreature.getManaCost()) > CardUtil.getConvertedManaCost(bestEnchantment.getManaCost())){
	                    setTargetCard(bestCreature);
	                  }
	                  else{
	                    setTargetCard(bestEnchantment);
	                  }
	                }
	                else{
	                  setTargetCard(bestCreature);
	                }
	              }
	              else if(getArtifact().size() != 0){
	                Card bestArtifact = CardFactoryUtil.AI_getBestArtifact(getArtifact());
	                setTargetCard(bestArtifact);
	              }

	            }//ChooseTargetAI()
	            CardList getCreature()
	            {
	              CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
	              return list;
	            }//getEnchantment()
	            CardList getArtifact()
	            {
	              CardList list = CardFactoryUtil.AI_getHumanArtifact(card, true);
	              return list;
	            }//getArtifact()
	            CardList getEnchantment()
	            {
	            	CardList list = CardFactoryUtil.AI_getHumanEnchantment(card, true);
	            	return list;
	            }
	           
	    		
	    		
	          public boolean canPlayAI()
	          {
	        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	        	  CardList cards = new CardList();
	        	  
	        	  cards.addAll(play.getCards());
	        	  cards = cards.filter(new CardListFilter(){

					public boolean addCard(Card c) {
						return (c.isArtifact() || c.isEnchantment() || c.isCreature() ) && CardFactoryUtil.canTarget(card, c);
					}
	        		  
	        	  });
	        	  
	        	  return cards.size() > 0;
	        	  
	          }
	    	  public boolean canPlay()
	    	  {
	    		  String controller = card.getController();
	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
		  
	    		  CardList creats = new CardList();
	    		  
	    		  creats.addAll(play.getCards());
	    		  creats = creats.getType("Creature");
	    		  creats = creats.filter(new CardListFilter()
	    		  {
					public boolean addCard(Card c) {
						return c.isUntapped() && !c.equals(card);
					} 
	    		  });
	    		  
	    		  if (creats.size() > 1 && AllZone.GameAction.isCardInPlay(card) && card.isUntapped() && !card.hasSickness())
	    			  return true;
	    		  else
	    			  return false;
	    	  }
	    	  public void resolve()
	    	  {

	    		if (getTargetCard()== null)
	    			return;
	    		  
	            String player = card.getController();
	            if(player.equals(Constant.Player.Human))
	              humanResolve();
	            else
	              computerResolve();
	          }
	      	  public void humanResolve()
	          {
	      		  String controller = card.getController();
	      		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
	      		  CardList creats = new CardList();
	  		  
	      		  creats.addAll(play.getCards());
	      		  creats = creats.getType("Creature");
	      		  creats = creats.filter(new CardListFilter()
	      		  {
					public boolean addCard(Card c) {
						return c.isUntapped() && !c.equals(card);
					}
	      		  });    		  
	      		  
	      		  CardList tappedCreats = new CardList();
	      		  
	      		  Object o = AllZone.Display.getChoice("Pick first creature to tap", creats.toArray());
	      		  
	      		  if (o!= null)
	      		  {
	      			  Card c1 = (Card)o;
	      			  creats.remove(c1);     
	      			  tappedCreats.add(c1);
	      		  }
	      		  else
	      			  return;
	      		  
	      		  o = AllZone.Display.getChoice("Pick second creature to tap", creats.toArray());
	      		  if (o!= null)
	        	  {
	      			  Card c2 = (Card)o;
	      			  creats.remove(c2); 
	      			  tappedCreats.add(c2);
	        	  }	  
	      		  else 
	      			  return;
	    		     		  
	    		  for(int i = 0; i<tappedCreats.size(); i++)
	    		  {
	    			  Card tapCreat = tappedCreats.get(i);
	    			  tapCreat.tap();
	    		  }
	    		  
	    		  //AllZone.GameAction.destroy(getTargetCard());   
	    		  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
	    		  AllZone.GameAction.moveTo(hand, getTargetCard());
	          }//humanResolve
	      	  
	      	  public void computerResolve()
	      	  {
	    		  String controller = card.getController();
	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
	    		  CardList creats = new CardList();
			  
	    		  creats.addAll(play.getCards());
	    		  creats = creats.getType("Creature");
	    		  
	    		      		  
	    		  for (int i=0; i < 3; i++)
	    		  {
	    			  Card c = creats.get(i);
	    			  c.tap();
	    		  }
	    		  
	    		  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
	    		  AllZone.GameAction.moveTo(hand, getTargetCard()); 

	      	  }//computerResolve

	        };//a1
	        
	        //card.clearSpellAbility();
	        card.addSpellAbility(a1);
	        a1.setDescription("tap, Tap two untapped creatures you control: Return target permanent to its owner's hand.");
	        
	        Input runtime = new Input()
	        {


	        private static final long serialVersionUID = 5673846456179861542L;

			public void showMessage()
	          {
	            CardList all = new CardList();
	            all.addAll(AllZone.Human_Play.getCards());
	            all.addAll(AllZone.Computer_Play.getCards());
	            all = all.filter(new CardListFilter()
	            {
		  			public boolean addCard(Card c) {
		  				return (c.isPermanent()) && CardFactoryUtil.canTarget(card, c);
		  			}
	            });
	            
	            stopSetNext(CardFactoryUtil.input_targetSpecific(a1, all, "Return target permanent", true));
	          }
	        };
	        a1.setBeforePayMana(runtime);
	    	
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Nullmage Shepherd"))
	    {
	    	final SpellAbility a1 = new Ability(card,"0")
	        {
	    		public void chooseTargetAI()
	            {
	              if(getEnchantment().size() != 0)
	              {
	                Card bestEnchantment = CardFactoryUtil.AI_getBestEnchantment(getEnchantment(), card, true);
	                if(getArtifact().size() != 0){
	                  Card bestArtifact = CardFactoryUtil.AI_getBestArtifact(getArtifact());
	                  if(CardUtil.getConvertedManaCost(bestArtifact.getManaCost()) > CardUtil.getConvertedManaCost(bestEnchantment.getManaCost())){
	                    setTargetCard(bestArtifact);
	                  }
	                  else{
	                    setTargetCard(bestEnchantment);
	                  }
	                }
	                else{
	                  setTargetCard(bestEnchantment);
	                }
	              }
	              else if(getArtifact().size() != 0){
	                Card bestArtifact = CardFactoryUtil.AI_getBestArtifact(getArtifact());
	                setTargetCard(bestArtifact);
	              }

	            }
	            CardList getEnchantment()
	            {
	              CardList list = CardFactoryUtil.AI_getHumanEnchantment(card, true);
	              return list;
	            }//getEnchantment()
	            CardList getArtifact()
	            {
	              CardList list = CardFactoryUtil.AI_getHumanArtifact(card, true);
	              return list;
	            }//getArtifact()
	    		
	    		
	          public boolean canPlayAI()
	          {
	        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	        	  CardList cards = new CardList();
	        	  
	        	  cards.addAll(play.getCards());
	        	  cards = cards.filter(new CardListFilter(){

					public boolean addCard(Card c) {
						return (c.isArtifact() || c.isEnchantment() ) && CardFactoryUtil.canTarget(card, c);
					}
	        		  
	        	  });
	        	  
	        	  return cards.size() > 0;
	        	  
	          }
	    	  public boolean canPlay()
	    	  {
	    		   SpellAbility sa;
		      	   for (int i=0; i<AllZone.Stack.size(); i++)
		      	   {
		      	    	     sa = AllZone.Stack.peek(i);
		      	    	     if (sa.getSourceCard().equals(card))
		      	    	          return false;
		      	   }
	    		  
	    		  String controller = card.getController();
	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
		  
	    		  CardList creats = new CardList();
	    		  
	    		  creats.addAll(play.getCards());
	    		  creats = creats.getType("Creature");
	    		  creats = creats.filter(new CardListFilter()
	    		  {
					public boolean addCard(Card c) {
						return c.isUntapped();
					} 
	    		  });
	    		  
	    		  if (creats.size() > 3 && AllZone.GameAction.isCardInPlay(card))
	    			  return true;
	    		  else
	    			  return false;
	    	  }
	    	  public void resolve()
	    	  {

	    		if (getTargetCard()== null)
	    			return;
	    		  
	            String player = card.getController();
	            if(player.equals(Constant.Player.Human))
	              humanResolve();
	            else
	              computerResolve();
	          }
	      	  public void humanResolve()
	          {
	      		  String controller = card.getController();
	      		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
	      		  CardList creats = new CardList();
	  		  
	      		  creats.addAll(play.getCards());
	      		  creats = creats.getType("Creature");
	      		  creats = creats.filter(new CardListFilter()
	      		  {
					public boolean addCard(Card c) {
						return c.isUntapped();
					}
	      		  });    		  
	      		  
	      		  CardList tappedCreats = new CardList();
	      		  
	      		  Object o = AllZone.Display.getChoice("Pick first creature to tap", creats.toArray());
	      		  
	      		  if (o!= null)
	      		  {
	      			  Card c1 = (Card)o;
	      			  creats.remove(c1);     
	      			  tappedCreats.add(c1);
	      		  }
	      		  else
	      			  return;
	      		  
	      		  o = AllZone.Display.getChoice("Pick second creature to tap", creats.toArray());
	      		  if (o!= null)
	        	  {
	      			  Card c2 = (Card)o;
	      			  creats.remove(c2); 
	      			  tappedCreats.add(c2);
	        	  }	  
	      		  else 
	      			  return;
	      		  
	      		  o = AllZone.Display.getChoice("Pick third creature to tap", creats.toArray());
	      		  if (o!= null)
	        	  {
	      			  Card c3 = (Card) o;
	        		  creats.remove(c3); 
	        		  tappedCreats.add(c3);
	        	  }
	      		  else 
	      			  return;
	      		  
	      		  o = AllZone.Display.getChoice("Pick fourth creature to tap", creats.toArray());
	    		  if (o!= null)
	    		  {
	    			  Card c4 = (Card) o;
	    			  creats.remove(c4); 
	    			  tappedCreats.add(c4);
	    		  }
	    		  else 
	    			  return;
	    		 
	    		  
	    		  for(int i = 0; i<tappedCreats.size(); i++)
	    		  {
	    			  Card tapCreat = tappedCreats.get(i);
	    			  tapCreat.tap();
	    		  }
	    		  
	    		  AllZone.GameAction.destroy(getTargetCard());    		  
	          }//humanResolve
	      	  
	      	  public void computerResolve()
	      	  {
	    		  String controller = card.getController();
	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
	    		  CardList creats = new CardList();
			  
	    		  creats.addAll(play.getCards());
	    		  creats = creats.getType("Creature");
	    		  
	    		      		  
	    		  for (int i=0; i < 4; i++)
	    		  {
	    			  Card c = creats.get(i);
	    			  c.tap();
	    		  }
	    		  
	    		  AllZone.GameAction.destroy(getTargetCard());    		  

	      	  }//computerResolve

	        };//a1
	        
	        //card.clearSpellAbility();
	        card.addSpellAbility(a1);
	        a1.setDescription("Tap four untapped creatures you control: Destroy target artifact or enchantment.");
	        
	        Input runtime = new Input()
	        {

			private static final long serialVersionUID = -7702308833923538927L;

			public void showMessage()
	          {
	            CardList all = new CardList();
	            all.addAll(AllZone.Human_Play.getCards());
	            all.addAll(AllZone.Computer_Play.getCards());
	            all = all.filter(new CardListFilter()
	            {
		  			public boolean addCard(Card c) {
		  				return (c.isEnchantment() || c.isArtifact()) && CardFactoryUtil.canTarget(card, c);
		  			}
	            });
	            
	            stopSetNext(CardFactoryUtil.input_targetSpecific(a1, all, "Destroy target artifact or enchantment.", true));
	          }
	        };
	        a1.setBeforePayMana(runtime);
	    	
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Gilt-Leaf Archdruid"))
	    {
	    	final SpellAbility a1 = new Ability(card,"0")
	        {
	    	  public boolean canPlay()
	    	  {
	    		  String controller = card.getController();
	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
		  
	    		  CardList druids = new CardList();
	    		  
	    		  druids.addAll(play.getCards());
	    		  druids = druids.getType("Druid");
	    		  
	    		  //System.out.println("Druids size: " + druids.size());
	    		  
	    		  int druidsUntapped = 0;
	    		  for (int i=0; i<druids.size();i++)
	    		  {
	    			  Card c = druids.get(0);
	    			  if (!c.isTapped())
	    				  druidsUntapped++;
	    		  }
	    		  
	    		  if (druids.size() > 6 && druidsUntapped > 6 && AllZone.GameAction.isCardInPlay(card))
	    			  return true;
	    		  else
	    			  return false;
	    	  }
	    	  public void resolve()
	    	  {

	            String player = card.getController();
	            if(player.equals(Constant.Player.Human))
	              humanResolve();
	            else
	              computerResolve();
	          }
	      	  public void humanResolve()
	          {
	      		  String controller = card.getController();
	      		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
	      		  CardList druids = new CardList();
	  		  
	      		  druids.addAll(play.getCards());
	      		  druids = druids.getType("Druid");
	      		  
	      		  CardList tappedDruids = new CardList();
	      		  
	      		  Object o = AllZone.Display.getChoice("Pick first druid to tap", druids.toArray());
	      		  
	      		  if (o!= null)
	      		  {
	      			  Card c1 = (Card)o;
	      			  druids.remove(c1);     
	      			  tappedDruids.add(c1);
	      		  }
	      		  else
	      			  return;
	      		  
	      		  o = AllZone.Display.getChoice("Pick second druid to tap", druids.toArray());
	      		  if (o!= null)
	        	  {
	      			  Card c2 = (Card)o;
	      			  druids.remove(c2); 
	      			  tappedDruids.add(c2);
	        	  }	  
	      		  else 
	      			  return;
	      		  
	      		  o = AllZone.Display.getChoice("Pick third druid to tap", druids.toArray());
	      		  if (o!= null)
	        	  {
	      			  Card c3 = (Card) o;
	        		  druids.remove(c3); 
	        		  tappedDruids.add(c3);
	        	  }
	      		  else 
	      			  return;
	      		  
	      		  o = AllZone.Display.getChoice("Pick fourth druid to tap", druids.toArray());
	    		  if (o!= null)
	    		  {
	    			  Card c4 = (Card) o;
	    			  druids.remove(c4); 
	    			  tappedDruids.add(c4);
	    		  }
	    		  else 
	    			  return;
	    		  o = AllZone.Display.getChoice("Pick fifth druid to tap", druids.toArray());
	    		  if (o!= null)
	    		  {
	    			  Card c5 = (Card) o;
	    			  druids.remove(c5);
	    			  tappedDruids.add(c5);
	    		  }
	    		  else 
	    			  return;
	    		  
	    		  o = AllZone.Display.getChoice("Pick sixth druid to tap", druids.toArray());
	    		  if (o!= null)
	    		  {
	    			  Card c6 = (Card) o;
	    			  druids.remove(c6);
	    			  tappedDruids.add(c6);
	    		  }
	    		  else 
	    			  return;
	    		  
	    		  o = AllZone.Display.getChoice("Pick seventh druid to tap", druids.toArray());
	    		  if (o!=null)
	    		  {
	    			  Card c7 = (Card) o;
	    			  druids.remove(c7); 
	    			  tappedDruids.add(c7);
	    		  }
	    		  else 
	    			  return;
	    		  
	    		  for(int i = 0; i<tappedDruids.size(); i++)
	    		  {
	    			  Card tapDruid = tappedDruids.get(i);
	    			  tapDruid.tap();
	    		  }
	    		  
	    		  String opponent = AllZone.GameAction.getOpponent(controller);
	    		  PlayerZone opponentPlay = AllZone.getZone(Constant.Zone.Play, opponent);
	    		  
	    		  CardList lands = new CardList();
	    		  lands.addAll(opponentPlay.getCards());
	    		  lands = lands.getType("Land");
	    		  
	    		  //System.out.println("Land size: " +lands.size());
	    		  
	    		  for (int i = 0; i < lands.size(); i++)
	    		  {
	    			  Card land = lands.get(i);
	    			  
	    			  
	    	          if(AllZone.GameAction.isCardInPlay(land))
	    	          {
	    	        	land.setController(controller);
	    	        	
	    	        	//set summoning sickness
	    	      		if(land.getKeyword().contains("Haste")){
	    	      		  land.setSickness(false);
	    	      		}
	    	      		else{
	    	      		  land.setSickness(true);
	    	      		}
	    	        	  
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

	    	            PlayerZone from = AllZone.getZone(land);
	    	            from.remove(land);

	    	            PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
	    	            to.add(land);

	    	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	    	          }//if
	    			  
	    			  
	    		  }
	      		  
	          }//humanResolve
	      	  
	      	  public void computerResolve()
	      	  {
	    		  String controller = card.getController();
	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
	    		  CardList druids = new CardList();
			  
	    		  druids.addAll(play.getCards());
	    		  druids = druids.getType("Druid");
	    		  
	    		      		  
	    		  for (int i=0; i < 7; i++)
	    		  {
	    			  Card c = druids.get(i);
	    			  c.tap();
	    			  
	    		  }
	    		  
	    		  String opponent = AllZone.GameAction.getOpponent(controller);
	    		  PlayerZone opponentPlay = AllZone.getZone(Constant.Zone.Play, opponent);
	    		  
	    		  CardList lands = new CardList();
	    		  lands.addAll(opponentPlay.getCards());
	    		  lands = lands.getType("Land");
	    		  
	    		  for (int i = 0; i < lands.size(); i++)
	    		  {
	    			  Card land = lands.get(i);
	    			  if(AllZone.GameAction.isCardInPlay(land))
	    	          {
	    	        	land.setController(controller);
	    	        	
	    	        	//set summoning sickness
	    	      		if(land.getKeyword().contains("Haste")){
	    	      		  land.setSickness(false);
	    	      		}
	    	      		else{
	    	      		  land.setSickness(true);
	    	      		}
	    	        	  
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

	    	            PlayerZone from = AllZone.getZone(land);
	    	            from.remove(land);

	    	            PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
	    	            to.add(land);

	    	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	    	          }//if
	    		  }

	      	  }//computerResolve

	        };//a1
	        
	        card.clearSpellAbility();
	        card.addSpellAbility(a1);
	        a1.setStackDescription(card.getController() + " taps seven untapped Druids and gains control of all opponent's land.");
	        a1.setDescription("Tap seven untapped Druids you control: Gain control of all lands target player controls.");
	        card.addSpellAbility(new Spell_Permanent(card)
	        {
			  private static final long serialVersionUID = -4621346281051305833L;

			  public boolean canPlayAI()
	          {
	            return true;
	          }
	        });
	    	
	    }//*************** END ************ END **************************
	    
	    
/*
	    //*************** START *********** START **************************
	    if (cardName.equals("Chameleon Colossus"))
	    {
	    	final int attack[] = new int[1];
	        final int defense[] = new int[1];
	        
	        
	        final Command untilEOT = new Command()
	        {
			
			private static final long serialVersionUID = 861635058350850025L;

			public void execute()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              card.setTempAttackBoost(0);
	              card.setTempDefenseBoost(0);
	            }
	          }
	        };

	        SpellAbility ability = new Ability_Activated(card, "2 G G")
	        {	

				private static final long serialVersionUID = -4910313985049099384L;
				public boolean canPlayAI()
				{
	              return CardFactoryUtil.AI_doesCreatureAttack(card);
				}
	          public boolean canPlay()
	          {
	          	if (CardFactoryUtil.canUseAbility(card) && AllZone.GameAction.isCardInPlay(card) && 
	          		!card.isFaceDown())
	          		return true;
	          	else 
	          		return false;
	          }
	          public void resolve()
	          {
	            if(AllZone.GameAction.isCardInPlay(card))
	            {
	              attack[0] = card.getNetAttack();
	              defense[0] = attack[0];
	              
	              card.addTempAttackBoost(attack[0]);
	              card.addTempDefenseBoost(defense[0]);

	              AllZone.EndOfTurn.addUntil(untilEOT);
	            }
	          }
	        };
	        

	        ability.setDescription("2 G G : Chameleon Colossus gets +X/+X until end of turn, where X is its power." );
	        ability.setStackDescription("Chameleon Colossus gets +X/+X until end of turn, where X is its power.");

	        card.addSpellAbility(ability);
	    
		}//*************** END ************ END **************************
*/
	    
	    
	    //*************** START *********** START **************************
	    if (cardName.equals("Figure of Destiny"))
	    {
	    	 Ability ability1 = new Ability(card, "RW")
	 		{
	 			public void resolve()
	 			{
	 				boolean artifact = false;
	 				card.setBaseAttack(2);
	 				card.setBaseDefense(2);
	 				
	 				card.removeIntrinsicKeyword("Flying");
	 				card.removeIntrinsicKeyword("First Strike");
	 				
	 				if (card.isArtifact())
	 					artifact = true;
	 				
	 				card.setType(new ArrayList<String>());
	 				if (artifact)
	 					card.addType("Artifact");
	 				card.addType("Creature"); 
	 				card.addType("Kithkin");
	 				card.addType("Spirit");
	 			}
	 			
	 			public boolean canPlayAI()
	 			{
	 				return !card.getType().contains("Spirit");
	 			}
	 			
	 		};// ability1
	 		
	 		ability1.setDescription("RW: Figure of Destiny becomes a 2/2 Kithkin Spirit.");
	 		ability1.setStackDescription("Figure of Destiny becomes a 2/2 Kithkin Spirit.");
	 		card.addSpellAbility(ability1);
	 		
	 		
	 		 Ability ability2 = new Ability(card, "RW RW RW")
	  		{
	  			public void resolve()
	  			{
	  				if (card.getType().contains("Spirit") || card.getKeyword().contains("Changeling")) {
		  				boolean artifact = false;
		  				card.setBaseAttack(4);
		  				card.setBaseDefense(4);
		  				
		  				card.removeIntrinsicKeyword("Flying");
		  				card.removeIntrinsicKeyword("First Strike");
		  				
		  				if (card.isArtifact())
		  					artifact = true;
		  				
		  				card.setType(new ArrayList<String>());
		  				if (artifact)
		  					card.addType("Artifact");
		  				card.addType("Creature"); 
		  				card.addType("Kithkin");
		  				card.addType("Spirit");
		  				card.addType("Warrior");
	  				}
	  			}
	  			
	  			public boolean canPlay()
	  			{
	  				return card.getType().contains("Spirit") || card.getKeyword().contains("Changeling");
	  			}
	  			
	  			public boolean canPlayAI()
	  			{
	  				return !card.getType().contains("Warrior");
	  			}
	  			
	  		};// ability2
	 		
	  		ability2.setDescription("RW RW RW: If Figure of Destiny is a Spirit, it becomes a 4/4 Kithkin Spirit Warrior.");
	 		ability2.setStackDescription("Figure of Destiny becomes a 4/4 Kithkin Spirit Warrior.");
	 		card.addSpellAbility(ability2);
	 		
	 		
	 		 Ability ability3 = new Ability(card, "RW RW RW RW RW RW")
	   		{
	   			public void resolve()
	   			{
	   				if (card.getType().contains("Warrior") || card.getKeyword().contains("Changeling")) {
		   				boolean artifact = false;
		   				card.setBaseAttack(8);
		   				card.setBaseDefense(8);
		   				
		   				card.addIntrinsicKeyword("Flying");
		   				card.addIntrinsicKeyword("First Strike");
		   				
		   				if (card.isArtifact())
		   					artifact = true;
		   				
		   				card.setType(new ArrayList<String>());
		   				if (artifact)
		   					card.addType("Artifact");
		   				card.addType("Creature"); 
		   				card.addType("Kithkin");
		   				card.addType("Spirit");
		   				card.addType("Warrior");
		   				card.addType("Avatar");
	   				}
	   			}
	   			public boolean canPlay()
	   			{
	   				return card.getType().contains("Warrior") || card.getKeyword().contains("Changeling");
	   			}
	   			public boolean canPlayAI()
	   			{
	   				return !card.getType().contains("Avatar");
	   			}
	   		};// ability3
	   		
	   		ability3.setDescription("RW RW RW RW RW RW: If Figure of Destiny is a Warrior, it becomes an 8/8 Kithkin Spirit Warrior Avatar with flying and first strike. ");
	   		ability3.setStackDescription("Figure of Destiny becomes an 8/8 Kithkin Spirit Warrior Avatar with flying and first strike.");
	 		card.addSpellAbility(ability3);
	    	
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    if (cardName.equals("Jenara, Asura of War"))
	    {
	    
		    Ability ability2 = new Ability(card, "1 W")
			{
				public void resolve()
				{
					card.addCounter(Counters.P1P1, 1);
				}
			};// ability2
			
			ability2.setStackDescription(card.getName() + " - gets a +1/+1 counter.");
			ability2.setDescription("1 W: Put a +1/+1 counter on Jenara, Asura of War.");
			card.addSpellAbility(ability2);
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    if (cardName.equals("Composite Golem"))
	    {
	       final Ability ability = new Ability(card, "0")
	       {

	    	 public boolean canPlay()
	    	 {
	    	   SpellAbility sa;
	      	   for (int i=0; i<AllZone.Stack.size(); i++)
	      	   {
	      	    	     sa = AllZone.Stack.peek(i);
	      	    	     if (sa.getSourceCard().equals(card))
	      	    	          return false;
	      	   }
	      	   return AllZone.GameAction.isCardInPlay(card);
	    	 }
	         public boolean canPlayAI(){return false;}
	          
	          public void resolve()
	          {
	             if (card.getController().equals(Constant.Player.Human))
	             {
	                    /*CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
	                    list = list.getName("Mana Pool");*/
	                    Card mp = AllZone.ManaPool;//list.getCard(0);

	                   	mp.addExtrinsicKeyword("ManaPool:W");
	                	mp.addExtrinsicKeyword("ManaPool:U");
	                	mp.addExtrinsicKeyword("ManaPool:B");
	                	mp.addExtrinsicKeyword("ManaPool:R");
	                	mp.addExtrinsicKeyword("ManaPool:G");
	                   
	                    AllZone.GameAction.sacrifice(card);
	             }
	          }
	       };
	       ability.setDescription("Sacrifice Composite Golem: Add W U B R G to your mana pool.");
	       ability.setStackDescription("Add WUBRG your mana pool");
	       card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    if (cardName.equals("Elvish Archdruid"))
	    {
	       final SpellAbility ability = new Ability_Tap(card)
	       {
	    	
			private static final long serialVersionUID = -7073759752440531772L;
			public void resolve()
	          {
	             /*CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
	             list = list.getName("Mana Pool");*/
	             Card mp = AllZone.ManaPool;//list.getCard(0);
	             
	             PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	             
	             CardList creats = new CardList();
	             creats.addAll(play.getCards());
	             
	             creats = creats.filter(new CardListFilter()
	             {
	            	public boolean addCard(Card c) 
	            	{
	            		return c.getType().contains("Elf") || c.getKeyword().contains("Changeling");
	            	}
	             });
	             
	             for(int i=0;i<creats.size();i++)
	             {
	            	 mp.addExtrinsicKeyword("ManaPool:G");
	             }
	          }
	          public boolean canPlayAI()
	          {
	             return false;
	          }
	       };
	       
	       ability.setDescription("tap: add G to your mana pool for each Elf you control.");
	       ability.setStackDescription(cardName + " adds G to your mana pool for each Elf you control.");
	       //card.clearSpellAbility();
	       card.addSpellAbility(ability);

	       return card;
	    }//*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    if (cardName.equals("Priest of Titania"))
	    {
	       final SpellAbility ability = new Ability_Tap(card)
	       {
	    	
			private static final long serialVersionUID = -9155272432379335551L;
			public void resolve()
	          {
	             /*CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
	             list = list.getName("Mana Pool");*/
	             Card mp = AllZone.ManaPool;//list.getCard(0);
	             
	             PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	             PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	             
	             CardList creats = new CardList();
	             creats.addAll(hum.getCards());
	             creats.addAll(comp.getCards());
	             
	             creats = creats.filter(new CardListFilter()
	             {
	            	public boolean addCard(Card c) 
	            	{
	            		return c.getType().contains("Elf") || c.getKeyword().contains("Changeling");
	            	}
	             });
	             
	             for(int i=0;i<creats.size();i++)
	             {
	            	 mp.addExtrinsicKeyword("ManaPool:G");
	             }
	          }
	          public boolean canPlayAI()
	          {
	             return false;
	          }
	       };
	       
	       ability.setDescription("tap: add G to your mana pool for each Elf in play.");
	       ability.setStackDescription(cardName + " adds G to your mana pool for each Elf in play.");
	       //card.clearSpellAbility();
	       card.addSpellAbility(ability);

	       return card;
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START ************ START **************************
	    if (cardName.equals("Rats of Rath"))
	    {
	    	final Ability ability = new Ability(card,"B")
	    	{
	    		public boolean canPlayAI(){return false;};
	    		public void resolve(){AllZone.GameAction.destroy(getTargetCard());};
	    	};
	    	ability.setBeforePayMana(new Input(){
				private static final long serialVersionUID = 7691864588170864421L;

				public void showMessage()
	    		{
	    			CardList choices = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	    			choices.filter(new CardListFilter(){public boolean addCard(Card c){
	    				return c.isArtifact() || c.isCreature() || c.isLand();}});
	    			stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choices, "Select an artifact, creature, or land you control", true));}
	    		});
	    	ability.setDescription("B: Destroy target artifact, creature, or land you control.");
	    	card.addSpellAbility(ability);
	    }
	    //*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    if (cardName.equals("Benalish Heralds"))
	    {
	       final SpellAbility ability = new Ability_Tap(card, "3 U")
	        {

			   private static final long serialVersionUID = 7131266665290870798L;

			   public boolean canPlayAI() {return false;}
	       
	           public void resolve()
	           {
	        	   AllZone.GameAction.drawCard(card.getController());
	           }
	           
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("3 U, tap: Draw a card.");

	     }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    if (cardName.equals("Jund Battlemage"))
	    {
	    	final SpellAbility ability = new Ability_Tap(card, "G")
	        {
				private static final long serialVersionUID = -2775698532993198968L;

				public void resolve()
		        {
	                Card c = new Card();
	                c.setName("Saproling");
	                c.setImageName("G 1 1 Saproling");
	
	                c.setOwner(card.getController());
	                c.setController(card.getController());
	
	                c.setManaCost("G");
	                c.setToken(true);
	
	                c.addType("Creature");
	                c.addType("Saproling");
	
	                c.setBaseAttack(1);
	                c.setBaseDefense(1);
	
	                PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	                play.add(c);
	              }//resolve()
            };


	       final SpellAbility ability2 = new Ability_Tap(card, "B")
	       {
	    	    private static final long serialVersionUID = -8102068245477091900L;
				public void resolve()
	             {
	              String opponent = AllZone.GameAction.getOpponent(card.getController());
	              AllZone.GameAction.getPlayerLife(opponent).subtractLife(1);
	            }
	            public boolean canPlayAI()
	            {
	              //computer should play ability if this creature doesn't attack
	              Combat c = ComputerUtil.getAttackers();
	              CardList list = new CardList(c.getAttackers());

	              //could this creature attack?, if attacks, do not use ability
	              return (! list.contains(card));
	            }
	            };//SpellAbility


	            card.addSpellAbility(ability);
	            ability.setDescription("G , tap: put a 1/1 green Saproling creature token onto the battlefield.");
	            ability.setStackDescription(card.getName() +  " - Put a 1/1 green Saproling token onto the battlefield.");

	            card.addSpellAbility(ability2);
		        ability2.setDescription("B, tap: Target player loses 1 life.");
		        ability2.setStackDescription(card.getName() + " - Opponent loses 1 life.");

	         }//*************** END ************ END **************************

	    	 //*************** START *********** START **************************
	    	 if (cardName.equals("Lich Lord of Unx"))
	    	 {
	    		 final SpellAbility ability = new Ability_Tap(card, "U B")
	 	         {
					private static final long serialVersionUID = 8909297504020264315L;

					public void resolve()
	 		        {
	 	                Card c = new Card();
	 	                c.setName("Zombie Wizard");
	 	                c.setImageName("UB 1 1 Zombie Wizard");
	 	
	 	                c.setOwner(card.getController());
	 	                c.setController(card.getController());
	 	
	 	                c.setManaCost("UB");
	 	                c.setToken(true);
	 	
	 	                c.addType("Creature");
	 	                c.addType("Zombie");
	 	                c.addType("Wizard");
	 	
	 	                c.setBaseAttack(1);
	 	                c.setBaseDefense(1);
	 	
	 	                PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	 	                play.add(c);
	 	              }//resolve()
	              };
	              
	              ability.setDescription("U B, Tap: Put a 1/1 blue and black Zombie Wizard creature token onto the battlefield.");
	              ability.setStackDescription(card.getName() + " - " + card.getController() + "puts a 1/1 blue and black Zombie Wizard creature token onto the battlefield.");

	              final Ability ability2 = new Ability(card, "U U B B")
	              {
	            	  public boolean canPlayAI()
	            	  {
	            		  setTargetPlayer(Constant.Player.Human);
	            		  return countZombies() >= 3;
	            	  }
	            	  
	            	  public void resolve()
	            	  {
	            		  if (getTargetPlayer() != null) {
		            		  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, getTargetPlayer());
		            		  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, getTargetPlayer());
		            		  
		            		  for (int i=0;i<countZombies();i++)
		            		  {
		            			  Card c = lib.get(0);
		            			  lib.remove(0);
		            			  grave.add(c);
		            			  
		            			  AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(1);
		            		  }
	            		  }
	            	  }
	            	  
	            	  public int countZombies()
	            	  {
	            		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            		  CardList list = new CardList(play.getCards());
	            		  list = list.getType("Zombie");
	            		  
	            		  return list.size();
	            	  }
	            	  
	              };
	              
	              ability2.setDescription("U U B B: Target player loses X life and puts the top X cards of his or her library into his or her graveyard, where X is the number of Zombies you control.");
	              ability2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability2));
	              
	              card.addSpellAbility(ability);
	              card.addSpellAbility(ability2);
	              
			 }//*************** END ************ END **************************
	    	 
	    	//*************** START *********** START **************************
    	    if(cardName.equals("Frontline Sage"))
    	    {
    	         final SpellAbility ability = new Ability_Tap(card, "U")
    	         {
					private static final long serialVersionUID = 4999015283563842050L;
					public boolean canPlayAI() {return false;}
	    	        public void resolve()
	    	        {
	    	          AllZone.GameAction.drawCard(card.getController());
	    	          AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	    	        }
    	      };//SpellAbility
    	      card.addSpellAbility(ability);
    	      ability.setDescription("U, tap: Draw a card, then discard a card.");
    	      ability.setStackDescription("Frontline Sage - draw a card, then discard a card.");
    	    }//*************** END ************ END **************************
    	    
    	    
    	  //*************** START *********** START **************************
	      if (cardName.equals("Thornscape Apprentice"))
          {
	           final SpellAbility ability = new Ability_Tap(card, "W")
	            {
					private static final long serialVersionUID = 7296467409302755815L;
					public void resolve()
		            {
		                 Card c = getTargetCard();
		                 c.tap();
		            }
		            public boolean canPlayAI() {return false;}
	            };//SpellAbility

	            card.addSpellAbility(ability);
	            ability.setDescription("W, tap: Tap target creature.");
	            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));

	       }//*************** END ************ END **************************
	      
	      
	      //*************** START *********** START **************************
		    else if(cardName.equals("Covetous Dragon"))
		    {
		      SpellAbility spell = new Spell_Permanent(card)
		      {
				
				private static final long serialVersionUID = -1446713295855849195L;

				public boolean canPlayAI()
		        {
				  CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
				  list = list.getType("Artifact");
		          return super.canPlayAI() && list.size() > 0;
		        }
		      };
		      card.clearSpellAbility();
		      card.addSpellAbility(spell);
		    }
		    //*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Tethered Griffin"))
		    {
		      SpellAbility spell = new Spell_Permanent(card)
		      {
				private static final long serialVersionUID = -7872917651421012893L;

				public boolean canPlayAI()
		        {
				  CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
				  list = list.getType("Enchantment");
		          return super.canPlayAI() && list.size() > 0;
		        }
		      };
		      card.clearSpellAbility();
		      card.addSpellAbility(spell);
		    }
		    //*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Cantivore"))
		    {
		      SpellAbility spell = new Spell_Permanent(card)
		      {
				private static final long serialVersionUID = 7254358703158629514L;

				public boolean canPlayAI()
		        {
				  CardList list = new CardList(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer).getCards());
				  list.addAll(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human).getCards());
				  list = list.getType("Enchantment");
		          return super.canPlayAI() && list.size() > 0;
		        }
		      };
		      card.clearSpellAbility();
		      card.addSpellAbility(spell);
		    }
		    //*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Terravore"))
		    {
		      SpellAbility spell = new Spell_Permanent(card)
		      {
				private static final long serialVersionUID = 7316190829288665283L;

				public boolean canPlayAI()
		        {
				  CardList list = new CardList(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer).getCards());
				  list.addAll(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human).getCards());
				  list = list.getType("Land");
		          return super.canPlayAI() && list.size() > 0;
		        }
		      };
		      card.clearSpellAbility();
		      card.addSpellAbility(spell);
		    }
		    //*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Mortivore"))
		    {
		      SpellAbility spell = new Spell_Permanent(card)
		      {
				private static final long serialVersionUID = -7118801410173525870L;

				public boolean canPlayAI()
		        {
				  CardList list = new CardList(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer).getCards());
				  list.addAll(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human).getCards());
				  list = list.getType("Creature");
		          return super.canPlayAI() && list.size() > 0;
		        }
		      };
		      card.clearSpellAbility();
		      card.addSpellAbility(spell);
		    }
		    //*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Cognivore"))
		    {
		      SpellAbility spell = new Spell_Permanent(card)
		      {
				private static final long serialVersionUID = -2216181341715046786L;

				public boolean canPlayAI()
		        {
				  CardList list = new CardList(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer).getCards());
				  list.addAll(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human).getCards());
				  list = list.getType("Instant");
		          return super.canPlayAI() && list.size() > 0;
		        }
		      };
		      card.clearSpellAbility();
		      card.addSpellAbility(spell);
		    }
		    //*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Magnivore"))
		    {
		      SpellAbility spell = new Spell_Permanent(card)
		      {
				private static final long serialVersionUID = -2252263708643462897L;

				public boolean canPlayAI()
		        {
				  CardList list = new CardList(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer).getCards());
				  list.addAll(AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human).getCards());
				  list = list.getType("Sorcery");
		          return super.canPlayAI() && list.size() > 0;
		        }
		      };
		      card.clearSpellAbility();
		      card.addSpellAbility(spell);
		    }
		    //*************** END ************ END **************************
	      
	      
	      //*************** START *********** START **************************
	      if(cardName.equals("Magus of the Library"))
	      {
	        final Ability_Tap ability2 = new Ability_Tap(card)
	        {
	  		private static final long serialVersionUID = 6567685794684744457L;
	  		public boolean canPlay()
	          {
	            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	            return hand.getCards().length == 7 && super.canPlay();
	          }
	          public void resolve()
	          {
	            AllZone.GameAction.drawCard(card.getController());
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability2);
	        ability2.setDescription("tap: Draw a card. Play this ability only if you have exactly 7 cards in hand.");
	        ability2.setStackDescription("Magus of the Library - draw a card.");
	        ability2.setBeforePayMana(new Input_NoCost_TapAbility(ability2));

	      }//*************** END ************ END **************************
	      

	      //*************** START *********** START **************************
	      else if(cardName.equals("Magus of the Disk"))
	      {
	        SpellAbility summoningSpell = new Spell_Permanent(card)
	        {
	  		private static final long serialVersionUID = 2510163318362956239L;

	  		public boolean canPlayAI()
	          {
	            boolean nevinyrralInPlay = false;

	            CardList inPlay = new CardList();
	            inPlay.addAll(AllZone.Computer_Play.getCards());
	            for(int i=0; i<inPlay.size(); ++i)
	            {
	              if( inPlay.getCard(i).getName().equals("Nevinyrral's Disk"))
	              {
	                nevinyrralInPlay = true;
	              }
	            }
	            return ! nevinyrralInPlay && (0 < CardFactoryUtil.AI_getHumanCreature(card, false).size());
	          }
	        };
	        card.clearSpellAbility();
	        card.addSpellAbility(summoningSpell);

	        card.addComesIntoPlayCommand(new Command()
	        {
	  		private static final long serialVersionUID = 1227443034730254929L;

	  		public void execute()
	          {
	            card.tap();
	          }
	        });
	        final SpellAbility ability = new Ability_Tap(card, "1")
	        {
	  		private static final long serialVersionUID = -4871606824998622131L;
	  		
	  		public void resolve()
	          {
	            CardList all = new CardList();
	            all.addAll(AllZone.Human_Play.getCards());
	            all.addAll(AllZone.Computer_Play.getCards());
	            all = filter(all);

	            for(int i = 0; i < all.size(); i++)
	              AllZone.GameAction.destroy(all.get(i));
	          }
	          private CardList filter(CardList list)
	          {
	            return list.filter(new CardListFilter()
	            {
	              public boolean addCard(Card c)
	              {
	                return c.isArtifact() || c.isCreature() || c.isEnchantment();
	              }
	            });
	          }//filter()
	          public boolean canPlayAI()
	          {
	            CardList human    = new CardList(AllZone.Human_Play.getCards());
	            CardList computer = new CardList(AllZone.Computer_Play.getCards());

	            human    = human.getType("Creature");
	            computer = computer.getType("Creature");

	            //the computer will at least destroy 2 more human creatures
	            return computer.size() < human.size()-1  || AllZone.Computer_Life.getLife() < 7;
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("1, tap: Destroy all artifacts, creatures, and enchantments.");
	        ability.setStackDescription("Destroy all artifacts, creatures, and enchantments.");
	      }//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	      else if(cardName.equals("Stern Judge"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	  		private static final long serialVersionUID = 3059547795996737707L;
	  		public void resolve()
	          {
	            AllZone.Human_Life.subtractLife(countSwamps("Human"));
	            AllZone.Computer_Life.subtractLife(countSwamps("Computer"));
	          }
	          int countSwamps(String player)
	          {
	            PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
	            CardList swamps = new CardList(play.getCards());
	            swamps = swamps.getType("Swamp");
	            return swamps.size();
	          }
	          public boolean canPlayAI()
	          {
	            int computer = countSwamps(Constant.Player.Computer);
	            int human    = countSwamps(Constant.Player.Human);

	            if((computer >= AllZone.Computer_Life.getLife()) || (human == 0))
	              return false;

	            return computer <= human;
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Each player loses 1 life for each Swamp he or she controls.");
	        ability.setStackDescription("Stern Judge - Each player loses 1 life for each Swamp he or she controls");
	        ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Cackling Imp") || cardName.equals("Blightspeaker"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	  		private static final long serialVersionUID = -8034678094689484203L;
	  		public void resolve()
	          {
	            String opponent = AllZone.GameAction.getOpponent(card.getController());
	            AllZone.GameAction.getPlayerLife(opponent).subtractLife(1);
	          }
	          public boolean canPlayAI()
	          {
	            //computer should play ability if this creature doesn't attack
	            Combat c = ComputerUtil.getAttackers();
	            CardList list = new CardList(c.getAttackers());

	            //could this creature attack?, if attacks, do not use ability
	            return (! list.contains(card));
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Target player loses 1 life.");
	        ability.setStackDescription(card.getName() + " - Opponent loses 1 life.");
	        ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
	      }//*************** END ************ END **************************


	      //*************** START *********** START **************************
	      else if(cardName.equals("Thought Courier"))
	      {
	        final Ability_Tap ability = new Ability_Tap(card)
	        {
	  		private static final long serialVersionUID = -7702857487992969125L;
	  		public boolean canPlayAI() {return false;}
	          public void resolve()
	          {
	            AllZone.GameAction.drawCard(card.getController());
	            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	          }
	        };//SpellAbility
	        card.addSpellAbility(ability);
	        ability.setDescription("tap: Draw a card, then discard a card.");
	        ability.setStackDescription("Thought Courier - draw a card, then discard a card.");
	        ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
	      }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Elvish Hunter"))
		    {
		      final SpellAbility ability = new Ability_Tap(card, "1 G")
		      {
				private static final long serialVersionUID = -560200335562416099L;
				public boolean canPlayAI()
		        {
		          if(CardFactoryUtil.AI_doesCreatureAttack(card))
		            return false;

		          return (getCreature().size() != 0);
		        }
		        public void chooseTargetAI()
		        {
		          card.tap();
		          Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
		          setTargetCard(target);
		        }
		        CardList getCreature()
		        {
		          CardList list = new CardList(AllZone.Human_Play.getCards());
		          list = list.filter(new CardListFilter()
		          {
		            public boolean addCard(Card c)
		            {
		              return c.isCreature() && (! c.getKeyword().contains("This card doesn't untap during your next untap step.")) && 
		              CardFactoryUtil.canTarget(card, c) && c.isTapped();
		            }
		          });
		          list.remove(card);
		          return list;
		        }//getCreature()
		        public void resolve()
		        {
		          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
		          {
		            final Card[] creature = new Card[1];
		            creature[0] = getTargetCard();
		            creature[0].addExtrinsicKeyword("This card doesn't untap during your next untap step.");
		          }//if (card is in play)
		        }//resolve()
		      };//SpellAbility
		      card.addSpellAbility(ability);
		      ability.setDescription("1 G, tap: Target creature doesn't untap during its controller's next untap step.");

		      ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
		    }//*************** END ************ END **************************
	      
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Scarland Thrinax"))
		    {
		      
		      final SpellAbility a2 = new Ability(card, "0")
		      {
		        public void resolve()
		        {
		          Card c = getTargetCard();
		          if(AllZone.GameAction.isCardInPlay(c))
		          {
		            //AllZone.getZone(c).remove(c);
		        	AllZone.GameAction.sacrifice(c);
		            
		        	if(AllZone.GameAction.isCardInPlay(card)) 
		        		card.addCounter(Counters.P1P1, 1);
		          }
		        }
		        public boolean canPlayAI()
		        {
		          return false;
		        }
		        public boolean canPlay()
		        {
		        	SpellAbility sa;
		            //this is a hack, check the stack to see if this card has an ability on the stack
		            //if so, we can't use the ability: this is to prevent using a limited ability too many times
		            for (int i=0; i<AllZone.Stack.size(); i++)
		            {
		            	sa = AllZone.Stack.peek(i);
		            	if (sa.getSourceCard().equals(card))
		            			return false;
		            }
		            if (super.canPlay())
		            	return true;
		            return false;
		        }
		      };//SpellAbility
		      
		      Input runtime = new Input()
		      {
		        private static final long serialVersionUID = 8445133749305465286L;

				public void showMessage()
		        {
		          CardList creats = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
		          creats = creats.getType("Creature");
		          
		          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, creats, "Select a creature to sacrifice.",false));
		        }
		      };

		      card.addSpellAbility(a2);
		      a2.setDescription("Sacrifice a creature: Put a +1/+1 counter on " +card.getName() +".");
		      a2.setStackDescription(card.getName() + " gets a +1/+1 counter.");

		      a2.setBeforePayMana(runtime);
		    }//*************** END ************ END **************************
	      
	      
	      //*************** START *********** START **************************
		    else if(cardName.equals("Cartographer"))
		    {
		      final SpellAbility ability = new Ability(card, "0")
		      {
		        public void resolve()
		        {
		          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
		          if(AllZone.GameAction.isCardInZone(getTargetCard(), grave))
		          {
		            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
		            AllZone.GameAction.moveTo(hand, getTargetCard());
		          }
		        }//resolve()
		      };
		      Command intoPlay = new Command()
		      {
				private static final long serialVersionUID = -3887243972980889087L;

				public void execute()
		        {
		          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
		          CardList lands = new CardList(grave.getCards());
		          lands = lands.getType("Land");

		          String controller = card.getController();

		          if(lands.size() == 0)
		            return;

		          if(controller.equals(Constant.Player.Human))
		          {
		            Object o = AllZone.Display.getChoiceOptional("Select target land", lands.toArray());
		            if(o != null)
		            {
		              ability.setTargetCard((Card)o);
		              AllZone.Stack.add(ability);
		            }
		          }
		          else //computer
		          {
		            lands.shuffle();
		            ability.setTargetCard(lands.get(0));
		            AllZone.Stack.add(ability);
		          }

		        }//execute()
		      };//Command
		      card.addComesIntoPlayCommand(intoPlay);
		    }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
		    else if(cardName.equals("Auramancer"))
		    {
		      final SpellAbility ability = new Ability(card, "0")
		      {
		        public void resolve()
		        {
		          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
		          if(AllZone.GameAction.isCardInZone(getTargetCard(), grave))
		          {
		            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
		            AllZone.GameAction.moveTo(hand, getTargetCard());
		          }
		        }//resolve()
		      };
		      Command intoPlay = new Command()
		      {
		    	  
				private static final long serialVersionUID = 25590819729244894L;

				public void execute()
		        {
		          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
		          CardList enchantments = new CardList(grave.getCards());
		          enchantments = enchantments.getType("Enchantment");

		          String controller = card.getController();

		          if(enchantments.size() == 0)
		            return;

		          if(controller.equals(Constant.Player.Human))
		          {
		            Object o = AllZone.Display.getChoiceOptional("Select target enchantment", enchantments.toArray());
		            if(o != null)
		            {
		              ability.setTargetCard((Card)o);
		              AllZone.Stack.add(ability);
		            }
		          }
		          else //computer
		          {
		            enchantments.shuffle();
		            ability.setTargetCard(enchantments.get(0));
		            AllZone.Stack.add(ability);
		          }

		        }//execute()
		      };//Command
		      card.addComesIntoPlayCommand(intoPlay);
		    }//*************** END ************ END **************************
	      
	        //*************** START *********** START **************************
            else if(cardName.equals("Acolyte of Xathrid"))
            {
               final SpellAbility ability = new Ability_Tap(card, "1 B")
               {
	               private static final long serialVersionUID = -560200331236516099L;
	               public void resolve()
	               {
	                  String opponent = AllZone.GameAction.getOpponent(card.getController());
	                  AllZone.GameAction.getPlayerLife(opponent).subtractLife(1);
	               }
	               public boolean canPlayAI()
	               {
	                  //computer should play ability if this creature doesn't attack
	                  Combat c = ComputerUtil.getAttackers();
	                  CardList list = new CardList(c.getAttackers());
	
	                  //could this creature attack?, if attacks, do not use ability
	                  if (!list.contains(card)) {
	                	  setTargetPlayer(Constant.Player.Human);
	                	  return true;
	                  }
	                  return false;
	               }
              };//SpellAbility
              card.addSpellAbility(ability);
              ability.setDescription("1 B, tap: Target player loses 1 life.");
              ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
            }//*************** END ************ END **************************
	      
	      
	    //*************** START *********** START **************************
  	      else if(cardName.equals("Merfolk Sovereign"))
  	      {
  	        final Ability_Tap ability = new Ability_Tap(card, "0")
  	        {
			  private static final long serialVersionUID = -4663016921034366082L;

			  public boolean canPlayAI() {return getMerfolk().size() != 0;}

  	          public void chooseTargetAI()
  	          {
  	            AllZone.GameAction.sacrifice(card);

  	            CardList merfolk = getMerfolk();
  	            merfolk.shuffle();
  	            setTargetCard(merfolk.get(0));
  	          }

  	          CardList getMerfolk()
  	          {
  	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
  	            CardList merfolk = new CardList(play.getCards());
  	            merfolk = merfolk.filter(new CardListFilter()
  	            {
  	              public boolean addCard(Card c)
  	              {
  	                return c.isCreature() && c.getType().contains("Merfolk") && CardFactoryUtil.AI_doesCreatureAttack(c) && 
  	                !c.equals(card) && !c.getKeyword().contains("Unblockable");
  	              }
  	            });
  	            return merfolk;
  	          }//getFlying()

  	          public void resolve()
  	          {
  	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
  	            {
  	              getTargetCard().addExtrinsicKeyword("Unblockable");
  	            }

  	            @SuppressWarnings("unused") // target
  	  		    final Card[] target = {getTargetCard()};
  	            AllZone.EndOfTurn.addUntil(new Command()
  	            {
				  private static final long serialVersionUID = -1884018112259809603L;

				  public void execute()
  	              {
  	                if(AllZone.GameAction.isCardInPlay(getTargetCard()))
  	                  getTargetCard().removeExtrinsicKeyword("Unblockable");
  	              }
  	            });
  	          }//resolve()
  	        };//SpellAbility

  	        Input runtime = new Input()
  	        {

  	          private static final long serialVersionUID = 4512556936796509819L;

			  public void showMessage()
  	          {
  	            CardList list = new CardList();
  	            list.addAll(AllZone.Human_Play.getCards());
  	            list.addAll(AllZone.Computer_Play.getCards());
  	            list = list.filter(new CardListFilter()
  	            {
  	              public boolean addCard(Card c)
  	              {
  	                return c.isCreature() && c.getType().contains("Merfolk") && CardFactoryUtil.canTarget(card, c);
  	              }
  	            });

  	            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list, "Select target Merfolk creature", Command.Blank,true));
  	          }//showMessage()
  	        };//Input

  	        card.addSpellAbility(ability);
  	        ability.setDescription("Tap: Target Merfolk creature is unblockable this turn.");
  	        ability.setBeforePayMana(runtime);
  	      }//*************** END ************ END **************************
	      
	      
	    //*************** START *********** START **************************
  	      else if(cardName.equals("Dwarven Pony"))
  	      {
  	        final Ability_Tap ability = new Ability_Tap(card, "1 R")
  	        {
			  private static final long serialVersionUID = 2626619319289064288L;

			  public boolean canPlayAI() {return getDwarves().size() != 0;}

  	          public void chooseTargetAI()
  	          {
  	            AllZone.GameAction.sacrifice(card);

  	            CardList dwarves = getDwarves();
  	            dwarves.shuffle();
  	            setTargetCard(dwarves.get(0));
  	          }

  	          CardList getDwarves()
  	          {
  	        	PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
  	        	CardList mountains = new CardList(hplay.getCards());
  	        	mountains = mountains.getType("Mountain");
  	        	if (mountains.size() == 0)
  	        		return mountains;
  	        	
  	        	
  	        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
  	            CardList dwarves = new CardList(play.getCards());
  	            dwarves = dwarves.filter(new CardListFilter()
  	            {
  	              public boolean addCard(Card c)
  	              {
  	                return c.isCreature() && c.getType().contains("Dwarf") && CardFactoryUtil.AI_doesCreatureAttack(c) && 
  	                !c.equals(card) && !c.getKeyword().contains("Mountainwalk");
  	              }
  	            });
  	            return dwarves;
  	          }//getFlying()

  	          public void resolve()
  	          {
  	            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
  	            {
  	              getTargetCard().addExtrinsicKeyword("Mountainwalk");
  	            }

  	            @SuppressWarnings("unused") // target
  	  		    final Card[] target = {getTargetCard()};
  	            AllZone.EndOfTurn.addUntil(new Command()
  	            {

  	            	private static final long serialVersionUID = -6845643843049229106L;

					public void execute()
	  	              {
	  	                if(AllZone.GameAction.isCardInPlay(getTargetCard()))
	  	                  getTargetCard().removeExtrinsicKeyword("Mountainwalk");
	  	              }
  	            });
  	          }//resolve()
  	        };//SpellAbility

  	        Input runtime = new Input()
  	        {

				private static final long serialVersionUID = -2962059144349469134L;

				public void showMessage()
	  	          {
	  	            CardList list = new CardList();
	  	            list.addAll(AllZone.Human_Play.getCards());
	  	            list.addAll(AllZone.Computer_Play.getCards());
	  	            list = list.filter(new CardListFilter()
	  	            {
	  	              public boolean addCard(Card c)
	  	              {
	  	                return c.isCreature() && c.getType().contains("Dwarf") && CardFactoryUtil.canTarget(card, c);
	  	              }
	  	            });
	
	  	            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list, "Select target Dwarf creature", Command.Blank,true));
	  	          }//showMessage()
	  	        };//Input

  	        card.addSpellAbility(ability);
  	        ability.setDescription("1 R, Tap: Target Dwarf creature gains mountainwalk until end of turn.");
  	        ability.setBeforePayMana(runtime);
  	      }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
  	    else if(cardName.equals("Fleeting Image"))
  	    {
  	    	final SpellAbility a1 = new Ability(card,"1 U")
  	        {
  	    	  public boolean canPlayAI()
  	    	  {
  	    		  return false;
  	    	  }
  	      	  public void resolve()
  	            {
  	      		  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
  	      		  /*
  	      		  AllZone.getZone(card).remove(card);
  	      		  hand.add(card);
  	      		  */
  	      		  if (card.isToken())
  	    			  AllZone.getZone(card).remove(card);
  	    		  else
  	    			  AllZone.GameAction.moveTo(hand, card);
  	      		  
  	            }
  	        };//a1
  	        
  	        //card.clearSpellAbility();
  	        card.addSpellAbility(a1);
  	        a1.setStackDescription(card.getController() + " returns Fleeting Image back to its owner's hand.");
  	        a1.setDescription("1 U: Return Fleeting Image to its owner's hand.");
  	    
  	   }//*************** END ************ END **************************
	      
  	    else if (cardName.equals("Academy Rector"))
  	    {
  	    	final Command destroy = new Command()
  	    	{
				private static final long serialVersionUID = -4352349741511065318L;

				public void execute() {
					if (card.getController().equals("Human"))
					{
						String[] choices = { "Yes", "No" };
						Object q = null;

						q = AllZone.Display.getChoiceOptional("Exile " + card.getName() + "?", choices);

						if (q == null || q.equals("No"))	
							;
						else
						{
							PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human);
							CardList list = new CardList(lib.getCards());
							list = list.filter(new CardListFilter()
							{
								public boolean addCard(Card c) {
									return c.isEnchantment();
								}	
							});
							
							if (list.size() > 0)
							{
								Object o = AllZone.Display.getChoiceOptional("Choose enchantment card to put onto the battlefield", list.toArray());
								if (o != null)
								{
									PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
									Card c = (Card)o;
									lib.remove(c);
									play.add(c);
								}
							}
							AllZone.GameAction.removeFromGame(card);
						}
					}//if human
					else
					{
						PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
						CardList list = new CardList(lib.getCards());
						list = list.filter(new CardListFilter()
						{
							public boolean addCard(Card c) {
								return c.isEnchantment() && !c.isAura();
							}
						});
						
						if (list.size() > 0)
						{
							PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
							Card c = CardFactoryUtil.AI_getBestEnchantment(list, card, false);
							lib.remove(c);
							play.add(c);
							AllZone.GameAction.removeFromGame(card);
							
						}
					}
				}	
  	    	};
  	    	
  	    	card.addDestroyCommand(destroy);
  	    }//*************** END ************ END **************************

	      //*************** START *********** START **************************
  	    else if (cardName.equals("Deadly Grub"))
  	    {
  	    	final Command destroy = new Command()
  	    	{
				private static final long serialVersionUID = -4352349741511065318L;

				public void execute() {
					if (card.getCounters(Counters.AGE) <= 0){
					
					  Card c = new Card();

	                  c.setName("Insect");
	                  c.setImageName("G 6 1 Insect");

	                  c.setOwner(card.getController());
	                  c.setController(card.getController());

	                  c.setManaCost("G");
	                  c.setToken(true);
	                 
	                  c.addType("Creature");
	                  c.addType("Insect");
	                  c.setBaseAttack(6);
	                  c.setBaseDefense(1);
	                  c.addIntrinsicKeyword("Shroud");

	                  PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	                  play.add(c);}
					
				}	
  	    	};
  	    	
  	    	card.addDestroyCommand(destroy);
  	    }//*************** END ************ END **************************

	      
	    //*************** START *********** START **************************
	    else if(cardName.equals("Boggart Harbinger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        lib.add(c, 0);  	
	        	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
	        private static final long serialVersionUID = 4022442363194287539L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList goblins = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Goblin") || cards.get(i).getKeyword().contains("Changeling"))
	        	  {
	        		  goblins.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(goblins.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", goblins.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            goblins.shuffle();
	            ability.setTargetCard(goblins.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	    else if(cardName.equals("Merfolk Harbinger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        lib.add(c, 0);  	
	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
	        private static final long serialVersionUID = 4022442363194287539L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList merfolk = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Merfolk") || cards.get(i).getKeyword().contains("Changeling"))
	        	  {
	        		  merfolk.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(merfolk.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", merfolk.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            merfolk.shuffle();
	            ability.setTargetCard(merfolk.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	    else if(cardName.equals("Elvish Harbinger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        lib.add(c, 0);  	
	        	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
	        private static final long serialVersionUID = 4022442363194287539L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList elves = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Elf") || cards.get(i).getKeyword().contains("Changeling"))
	        	  {
	        		  elves.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(elves.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", elves.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            elves.shuffle();
	            ability.setTargetCard(elves.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	      
	      
	      //*************** START *********** START **************************
	    else if(cardName.equals("Vendilion Clique"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	         public void resolve()
	        {
	          String player = getTargetPlayer();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	          CardList list = new CardList(hand.getCards());
	          CardList nonLandList = list.filter(new CardListFilter()
	          {
				public boolean addCard(Card c) {
					return !c.isLand();
				}
	          });
	          
	          if (list.size() > 0)
	          {
	        	  if (card.getController().equals(Constant.Player.Human))
	        	  {
	        		  AllZone.Display.getChoiceOptional("Revealing hand", list.toArray() );
	        		  if (nonLandList.size() > 0) {
		        		  Object o = AllZone.Display.getChoiceOptional("Select target non-land card", nonLandList.toArray());
		        		  if (o!=null)
		        		  {
		        			  Card c = (Card)o;
		        			  hand.remove(c);
		        			  lib.add(c); //put on bottom
		        			  
		        			  AllZone.GameAction.drawCard(player);
		        		  }
	        		  }
	        	  }
	        	  else //comp
	        	  {
	        		  if (AllZone.Phase.getTurn() >= 12 && nonLandList.size() > 0)
	        		  {
	        			  Card c = CardFactoryUtil.AI_getMostExpensivePermanent(nonLandList, card, false);
	        			  hand.remove(c);
	        			  lib.add(c);
	        			  AllZone.GameAction.drawCard(Constant.Player.Human);
	        		  }
	        	  }
	          }//handsize > 0
	          
	        }
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -5052568979553782714L;

			public void execute()
	        {
	        	if(card.getController().equals(Constant.Player.Human)) {
	        		AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
	        		ButtonUtil.disableAll();
	        	}
	        	else if (card.getController().equals(Constant.Player.Computer)) {
	        		ability.setTargetPlayer(Constant.Player.Human);
	        		AllZone.Stack.add(ability);
	        	}	          
	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	    else if(cardName.equals("Faerie Harbinger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        lib.add(c, 0);  	
	        	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {

			private static final long serialVersionUID = -708639335039567945L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList faeries = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Faerie") || cards.get(i).getKeyword().contains("Changeling"))
	        	  {
	        		  faeries.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(faeries.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", faeries.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	            faeries.shuffle();
	            ability.setTargetCard(faeries.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	    else if(cardName.equals("Flamekin Harbinger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        lib.add(c, 0);  	
	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = 7114265436722599216L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList ele = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Elemental") || cards.get(i).getKeyword().contains("Changeling"))
	        	  {
	        		  ele.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(ele.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", ele.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	        	ele.shuffle();
	            ability.setTargetCard(ele.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	      
	    //*************** START *********** START **************************
	    else if(cardName.equals("Giant Harbinger"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          if(AllZone.GameAction.isCardInZone(getTargetCard(), lib))
	          {
	        	Card c = getTargetCard();
	        	AllZone.GameAction.shuffle(card.getController());
	        	lib.remove(c);
		        lib.add(c, 0);  	
	        	

	          }
	        }//resolve()
	      };
	      Command intoPlay = new Command()
	      {
			private static final long serialVersionUID = -2671592749882297551L;

			public void execute()
	        {
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	          CardList cards = new CardList(lib.getCards());
	          CardList giants = new CardList();
	          
	          for (int i=0;i<cards.size();i++)
	          {
	        	  if(cards.get(i).getType().contains("Giant") || cards.get(i).getKeyword().contains("Changeling"))
	        	  {
	        		  giants.add(cards.get(i));
	        	  }
	          }

	          String controller = card.getController();

	          if(giants.size() == 0)
	            return;

	          if(controller.equals(Constant.Player.Human))
	          {
	            Object o = AllZone.Display.getChoiceOptional("Select target card", giants.toArray());
	            if(o != null)
	            {
	              ability.setTargetCard((Card)o);
	              AllZone.Stack.add(ability);
	            }
	          }
	          else //computer
	          {
	        	  giants.shuffle();
	            ability.setTargetCard(giants.get(0));
	            AllZone.Stack.add(ability);
	          }

	        }//execute()
	      };//Command
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	    else if (cardName.equals("Qasali Pridemage"))
	    {
	    	final Ability ability = new Ability(card, "1")
	    	{
				public void resolve() {
					if (AllZone.GameAction.isCardInPlay(card)) {
						AllZone.GameAction.sacrifice(card);
					
						Card c = getTargetCard();
						AllZone.GameAction.destroy(c);		
					}
				}	    
				
				public boolean canPlay()
				{
					SpellAbility sa;
	    	    	for (int i=0; i<AllZone.Stack.size(); i++)
	    	    	{
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card))
	    	    	          return false;
	    	    	}
	    	    	
					return super.canPlay();
				}
				
				public boolean canPlayAI()
				{
					 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    		 CardList list = new CardList();
		    		 list.addAll(hPlay.getCards());
		    		 
		    		 list = list.filter(new CardListFilter()
		    		 {
						public boolean addCard(Card c) {
							return c.isArtifact() || c.isEnchantment() && CardUtil.getConvertedManaCost(c.getManaCost()) >= 4;
						}
		    		 });
		    		 
		    		 if (list.size() > 0)
		    		 {
		    			 list.shuffle();
		    			 setTargetCard(list.get(0));
		    		 }
		    		 
		    		 return list.size() > 0;
				}
				
				
	    	};
	    	ability.setDescription("1, Sacrifice Qasali Pridemage: Destroy target artifact or enchantment.");
	    	ability.setBeforePayMana(new Input()
 	        {
				private static final long serialVersionUID = -3565024591921095303L;

				public void showMessage()
		        {
		    		 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    		 PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		    		 
		    		 CardList list = new CardList();
		    		 list.addAll(hPlay.getCards());
		    		 list.addAll(cPlay.getCards());
		    		 
		    		 list = list.filter(new CardListFilter()
		    		 {
						public boolean addCard(Card c) {
							return c.isArtifact() || c.isEnchantment();
						}
		    		 });
		    		 
		             stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list, "Select an Artifact or Enchantment", true));
		           }
		       });
	    	card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	    else if (cardName.equals("Viridian Zealot"))
	    {
	    	final Ability ability = new Ability(card, "1 G")
	    	{
				public void resolve() {
					if (AllZone.GameAction.isCardInPlay(card)) {
						AllZone.GameAction.sacrifice(card);
					
						Card c = getTargetCard();
						AllZone.GameAction.destroy(c);		
					}
				}	    
				
				public boolean canPlay()
				{
					SpellAbility sa;
	    	    	for (int i=0; i<AllZone.Stack.size(); i++)
	    	    	{
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card))
	    	    	          return false;
	    	    	}
	    	    	
					return super.canPlay();
				}
				
				public boolean canPlayAI()
				{
					 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    		 CardList list = new CardList();
		    		 list.addAll(hPlay.getCards());
		    		 
		    		 list = list.filter(new CardListFilter()
		    		 {
						public boolean addCard(Card c) {
							return c.isArtifact() || c.isEnchantment() && CardUtil.getConvertedManaCost(c.getManaCost()) >= 4;
						}
		    		 });
		    		 
		    		 if (list.size() > 0)
		    		 {
		    			 list.shuffle();
		    			 setTargetCard(list.get(0));
		    		 }
		    		 
		    		 return list.size() > 0;
				}
				
				
	    	};
	    	ability.setDescription("1 G, Sacrifice Viridian Zealot: Destroy target artifact or enchantment.");
	    	ability.setBeforePayMana(new Input()
 	        {
				private static final long serialVersionUID = -3565024591921095303L;

				public void showMessage()
		        {
		    		 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    		 PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		    		 
		    		 CardList list = new CardList();
		    		 list.addAll(hPlay.getCards());
		    		 list.addAll(cPlay.getCards());
		    		 
		    		 list = list.filter(new CardListFilter()
		    		 {
						public boolean addCard(Card c) {
							return c.isArtifact() || c.isEnchantment();
						}
		    		 });
		    		 
		             stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list, "Select an Artifact or Enchantment", true));
		           }
		       });
	    	card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	    else if (cardName.equals("Ronom Unicorn") || cardName.equals("Kami of Ancient Law"))
	    {
	    	final Ability ability = new Ability(card, "0")
	    	{
				public void resolve() {
					if (AllZone.GameAction.isCardInPlay(card)) {
						AllZone.GameAction.sacrifice(card);
					
						Card c = getTargetCard();
						AllZone.GameAction.destroy(c);		
					}
				}	    
				
				public boolean canPlay()
				{
					SpellAbility sa;
	    	    	for (int i=0; i<AllZone.Stack.size(); i++)
	    	    	{
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card))
	    	    	          return false;
	    	    	}
	    	    	
					return super.canPlay();
				}
				
				public boolean canPlayAI()
				{
					 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    		 CardList list = new CardList();
		    		 list.addAll(hPlay.getCards());
		    		 
		    		 list = list.filter(new CardListFilter()
		    		 {
						public boolean addCard(Card c) {
							return c.isEnchantment() && CardUtil.getConvertedManaCost(c.getManaCost()) >= 4;
						}
		    		 });
		    		 
		    		 if (list.size() > 0)
		    		 {
		    			 list.shuffle();
		    			 setTargetCard(list.get(0));
		    		 }
		    		 
		    		 return list.size() > 0;
				}
				
				
	    	};
	    	ability.setDescription("Sacrifice " +cardName +": Destroy target enchantment.");
	    	ability.setBeforePayMana(new Input()
 	        {
				private static final long serialVersionUID = -2023409303136493987L;

				public void showMessage()
		        {
		    		 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    		 PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		    		 
		    		 CardList list = new CardList();
		    		 list.addAll(hPlay.getCards());
		    		 list.addAll(cPlay.getCards());
		    		 
		    		 list = list.filter(new CardListFilter()
		    		 {
						public boolean addCard(Card c) {
							return c.isEnchantment();
						}
		    		 });
		    		 
		             stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list, "Select target Enchantment", true));
		           }
		       });
	    	card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	    else if (cardName.equals("Capashen Unicorn"))
	    {
	    	final Ability_Tap ability = new Ability_Tap(card, "1 W")
	    	{

				private static final long serialVersionUID = 3723881006291076691L;

				public void resolve() {
					if (AllZone.GameAction.isCardInPlay(card)) {
						AllZone.GameAction.sacrifice(card);
					
						Card c = getTargetCard();
						AllZone.GameAction.destroy(c);		
					}
				}	    
				
				public boolean canPlay()
				{
					SpellAbility sa;
	    	    	for (int i=0; i<AllZone.Stack.size(); i++)
	    	    	{
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card))
	    	    	          return false;
	    	    	}
	    	    	
					return super.canPlay();
				}
				
				public boolean canPlayAI()
				{
					 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    		 CardList list = new CardList();
		    		 list.addAll(hPlay.getCards());
		    		 
		    		 list = list.filter(new CardListFilter()
		    		 {
						public boolean addCard(Card c) {
							return c.isArtifact() || c.isEnchantment() && CardUtil.getConvertedManaCost(c.getManaCost()) >= 4;
						}
		    		 });
		    		 
		    		 if (list.size() > 0)
		    		 {
		    			 list.shuffle();
		    			 setTargetCard(list.get(0));
		    		 }
		    		 
		    		 return list.size() > 0;
				}
				
				
	    	};
	    	ability.setDescription("1 W, tap, Sacrifice Capashen Unicorn: Destroy target artifact or enchantment.");
	    	ability.setBeforePayMana(new Input()
 	        {
				private static final long serialVersionUID = -4032330058005016682L;

				public void showMessage()
		        {
		    		 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    		 PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		    		 
		    		 CardList list = new CardList();
		    		 list.addAll(hPlay.getCards());
		    		 list.addAll(cPlay.getCards());
		    		 
		    		 list = list.filter(new CardListFilter()
		    		 {
						public boolean addCard(Card c) {
							return c.isArtifact() || c.isEnchantment();
						}
		    		 });
		    		 
		             stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list, "Select an Artifact or Enchantment", true));
		           }
		       });
	    	card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	    else if(cardName.equals("Kor Cartographer"))
	    {
	      final Ability ab1 = new Ability(card, "no cost")
	    	{
	    		private static final long serialVersionUID = -3361422153566629825L;

	    		public void resolve()
	    		{
	    			PlayerZone lib  = AllZone.getZone(Constant.Zone.Library, card.getController());
	    			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController()); 

	    			CardList landInLib  = new CardList(lib.getCards());
	    			landInLib = landInLib.getType("Plains");

	    			if (landInLib.size() > 0)
	    			{
	    				if (card.getController().equals(Constant.Player.Computer))
	    				{
	    					lib.remove(landInLib.get(0));
	    					landInLib.get(0).tap();
	    					play.add(landInLib.get(0));
	    				}
	    				else
	    				{
	    					Object o = AllZone.Display.getChoiceOptional("Select plains card to put into play: ", landInLib.toArray());
	    					if (o != null)
	    					{	
	    						Card crd = (Card)o;
	    						lib.remove(crd);
	    						crd.tap();
	    						play.add(crd);
	    					}
	    				}
	    				AllZone.GameAction.shuffle(card.getController());
	    			}//if(isCardInPlay)
	    		}

	    		public boolean canPlayAI()
	    		{
	    			CardList landInLib  = new CardList(AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer).getCards());
	    			CardList landInPlay = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());

	    			landInLib  = landInLib.getType("Land");
	    			landInPlay = landInPlay.getType("Land");

	    			if (landInLib.size() > 0 && landInPlay.size() > 0)
	    				return true;
	    			else
	    				return false;

	    		}
	    	};//SpellAbility
	      Command cip = new Command(){/**
			 * 
			 */
			private static final long serialVersionUID = -2084426519099911543L;

			public void execute()
			{
				AllZone.Stack.add(ab1);
			}};
	      card.addComesIntoPlayCommand(cip);
	    }//*************** END ************ END **************************
	    
	      
	    //*************** START *********** START **************************
	    else if (cardName.equals("Loyal Retainers"))
	    {
	    	final Ability ability = new Ability(card, "0")
	    	{

				public void resolve() {
					if (AllZone.GameAction.isCardInPlay(card)) {
						AllZone.GameAction.sacrifice(card);
					
						PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
						PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
						CardList list = new CardList(grave.getCards());
						list = list.filter(new CardListFilter()
						{
							public boolean addCard(Card c)
							{
								return c.isCreature() && c.getType().contains("Legendary");
							}
						});
						
						if (list.size() >0) {
							if (card.getController().equals(Constant.Player.Human))
							{
								Object o = AllZone.Display.getChoiceOptional("Select Legendary creature", list.toArray());
				    			if (o!=null)
				    			{
				    				Card c = (Card)o;
				    				grave.remove(c);
				    				play.add(c);
				    			}
				    			
							}
							else //computer
							{
								Card c = CardFactoryUtil.AI_getBestCreature(list);
								grave.remove(c);
								play.add(c);
							}
						}
					}
				}	    
				
				public boolean canPlay()
				{
					PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
					CardList list = new CardList(grave.getCards());
					list = list.filter(new CardListFilter()
					{
						public boolean addCard(Card c)
						{
							return c.isCreature() && c.getType().contains("Legendary");
						}
					});
					
					SpellAbility sa;
	    	    	for (int i=0; i<AllZone.Stack.size(); i++)
	    	    	{
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card))
	    	    	          return false;
	    	    	}
	    	    	
					return super.canPlay() && list.size() > 0 && 
						   AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && 
						   AllZone.Phase.getActivePlayer().equals(card.getController());
				}
				
				public boolean canPlayAI()
				{
					PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
					CardList list = new CardList(grave.getCards());
					list = list.filter(new CardListFilter()
					{
						public boolean addCard(Card c)
						{
							return c.isCreature() && c.getType().contains("Legendary") && CardUtil.getConvertedManaCost(c.getManaCost()) > 4;
						}
					});
					return list.size()>0;
				}
				
				
	    	};
	    	ability.setDescription("Sacrifice Loyal Retainers: Return target legendary creature card from your graveyard to the battlefield. Activate this ability only during your turn, before attackers are declared.");
	    	ability.setStackDescription(cardName + " - Return target legendary creature card from your graveyard to the battlefield.");
	    	
	    	card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	      
	      
	      //*************** START *********** START **************************
	    else if (cardName.equals("Xiahou Dun, the One-Eyed"))
	    {
	    	final Ability ability = new Ability(card, "0")
	    	{

				public void resolve() {
					if (AllZone.GameAction.isCardInPlay(card)) {
						AllZone.GameAction.sacrifice(card);
					
						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
						PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
						CardList list = new CardList(grave.getCards());
						list = list.filter(new CardListFilter()
						{
							public boolean addCard(Card c)
							{
								return CardUtil.getColors(c).contains(Constant.Color.Black);
							}
						});
						
						if (list.size() >0) {
							if (card.getController().equals(Constant.Player.Human))
							{
								Object o = AllZone.Display.getChoiceOptional("Select black card", list.toArray());
				    			if (o!=null)
				    			{
				    				Card c = (Card)o;
				    				grave.remove(c);
				    				hand.add(c);
				    			}
				    			
							}
							else //computer
							{
								//TODO
							}
						}
					}
				}	    
				public boolean canPlay()
				{
					PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
					CardList list = new CardList(grave.getCards());
					list = list.filter(new CardListFilter()
					{
						public boolean addCard(Card c)
						{
							return CardUtil.getColors(c).contains(Constant.Color.Black);
						}
					});
					
					SpellAbility sa;
	    	    	for (int i=0; i<AllZone.Stack.size(); i++)
	    	    	{
	    	    	     sa = AllZone.Stack.peek(i);
	    	    	     if (sa.getSourceCard().equals(card))
	    	    	          return false;
	    	    	}
	    	    	
					return super.canPlay() && list.size() > 0 && 
						   AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && 
						   AllZone.Phase.getActivePlayer().equals(card.getController());
				}
				
				public boolean canPlayAI()
				{
					return false;
				}
	    	};
	    	ability.setDescription("Sacrifice Xiahou Dun, the One-Eyed: Return target black card from your graveyard to your hand. Activate this ability only during your turn, before attackers are declared.");
	    	ability.setStackDescription(cardName + " - Return target black card from your graveyard to your hand.");
	    	
	    	card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
	      
	    //*************** START *********** START **************************
	       if (cardName.equals("Magus of the Coffers"))
	       {
	          final SpellAbility ability = new Ability_Tap(card, "2")
	          {
	        	  
				private static final long serialVersionUID = 138661285416402582L;
				public void resolve()
	             {
	                Card mp = AllZone.ManaPool;//list.getCard(0);
	                
	                CardList swamps = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	                swamps = swamps.getType("Swamp");
	                
	                for(int i=0;i<swamps.size();i++)
	                {
	               	 mp.addExtrinsicKeyword("ManaPool:B");
	                }
	             }
	             public boolean canPlayAI()
	             {
	                return false;
	             }
	          };
	          
	          ability.setDescription("2, tap: Add B to your mana pool for each Swamp you control.");
	          ability.setStackDescription(cardName + " adds B to your mana pool for each Swamp you control.");
	          //card.clearSpellAbility();
	          //card.setText(card.getText() +  ability.toString());
	          card.addSpellAbility(ability);

	          return card;
	       }//*************** END ************ END **************************
	       
	       //*************** START *********** START **************************
		      else if(cardName.equals("Fire Bowman"))
		      {
		        final Ability ability = new Ability(card, "0")
		        {
		          public boolean canPlayAI() {return getCreature().size() != 0;}

		          public void chooseTargetAI()
		          {
		            if(AllZone.Human_Life.getLife() < 3)
		              setTargetPlayer(Constant.Player.Human);
		            else
		            {
		              CardList list = getCreature();
		              list.shuffle();
		              setTargetCard(list.get(0));
		            }
		            AllZone.GameAction.sacrifice(card);
		          }//chooseTargetAI()
		          CardList getCreature()
		          {
		            //toughness of 1
		            CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
		            list = list.filter(new CardListFilter()
		            {
		              public boolean addCard(Card c)
		              {
		                //only get 1/1 flyers or 2/1 creatures
		                return (2 <= c.getNetAttack()) || c.getKeyword().contains("Flying");
		              }
		            });
		            return list;
		          }//getCreature()

		          public void resolve()
		          {
		            if(getTargetCard() != null)
		            {
		              if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
		                getTargetCard().addDamage(1, card);
		            }
		            else
		              AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(1);
		          }//resolve()
		        };//SpellAbility

		        card.addSpellAbility(ability);
		        ability.setDescription("Sacrifice Fire Bowman: Fire Bowman deals 1 damage to target creature or player. Activate this ability only during your turn, before attackers are declared.");
		        ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, new Command()
		        {

				  private static final long serialVersionUID = -3283051501556347775L;

				  public void execute()
		          {
		            AllZone.GameAction.sacrifice(card);
		          }
		        }, true));
		      }//*************** END ************ END **************************
	       
	          //*************** START *********** START **************************
		      else if(cardName.equals("Archivist"))
		      {
		       final SpellAbility ability = new Ability_Tap(card, "0")
		       {
					private static final long serialVersionUID = -3630015833049495309L;
	
					public boolean canPlayAI() {return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);}
	
			        public void resolve()
			        {
			          	 AllZone.GameAction.drawCard(card.getController());
			        } 
		        };//SpellAbility
		        card.addSpellAbility(ability);
		        ability.setDescription("tap: Draw a card.");
		        ability.setStackDescription(card.getName() + " - draw a card.");
		      }//*************** END ************ END **************************
	       
	       	  //*************** START *********** START **************************
		      else if(cardName.equals("Sygg, River Guide"))
		      {
		    	  final HashMap<Card, String[]> creatureMap = new HashMap<Card, String[]>();
		    	  
		    	  final Ability ability = new Ability(card, "1 W")
		    	  {
		    		  public void resolve()
		    		  {
		    			  Card c = getTargetCard();
		    			  String color = new String();
		    			  if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card,c)){
	                          
			                  Object o = AllZone.Display.getChoice("Choose mana color",Constant.Color.Colors);
			                  color = (String)o;
			                  c.addExtrinsicKeyword("Protection from " +color); 
			                  if (creatureMap.containsKey(c)) {
			                	  int size = creatureMap.get(c).length;
			                	  String[] newString = new String[size+1];
			                	  
			                	  for (int i=0;i<size;i++)
			                	  {
			                		  newString[i] = creatureMap.get(c)[i];
			                	  }
			                	  newString[size] = color;
			                	  creatureMap.put(c, newString);
			                  }
			                  else
			                	  creatureMap.put(c, new String[] {color});
			                  
			                  final Card crd = c;
			                  final Command atEOT = new Command()
			  	           	  {
								private static final long serialVersionUID = 8630868536866681014L;

								public void execute()
			                	{	
			                		  //if(AllZone.GameAction.isCardInPlay(c))
			                		  //  c.removeExtrinsicKeyword("Protection from "+color);
									  if (AllZone.GameAction.isCardInPlay(crd))
									  {
										  String[] colors = creatureMap.get(crd);
										  for (String col : colors)
										  {
											  crd.removeExtrinsicKeyword("Protection from " + col);
										  }
									  }
			                	}
			  	           	  };//Command
			  	           	  AllZone.EndOfTurn.addUntil(atEOT);
		    			  }
		    		  }
		    	  };
		    	  Input runtime = new Input()
			      {
					private static final long serialVersionUID = -2171146532836387392L;

					public void showMessage()
			        {
			          CardList creats = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
			          creats = creats.getType("Merfolk");
			          
			          stopSetNext(CardFactoryUtil.input_targetSpecific(ability, creats, "Select a target Merfolk",true));
			        }
			      };
			      ability.setDescription("1 W: Target Merfolk you control gains protection from the color of your choice until end of turn.");
			      ability.setBeforePayMana(runtime);
		    	  card.addSpellAbility(ability);
		      }//*************** END ************ END **************************
	       
	       	  //*************** START *********** START **************************
		      else if (cardName.equals("Laquatus's Champion"))
		      {
		    	  final SpellAbility abilityComes = new Ability(card, "0")
			      {
			        public void resolve()
			        {
			        	  AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(6);
			        }//resolve()
			      };

			      final Input inputComes = new Input()
			      {
					private static final long serialVersionUID = -2666229064706311L;

					public void showMessage()
			        {
			          stopSetNext(CardFactoryUtil.input_targetPlayer(abilityComes));
			          ButtonUtil.disableAll();//to disable the Cancel button
			        }
			      };
			      Command commandComes = new Command()
			      {
			    	private static final long serialVersionUID = -4246229185669164581L;

					public void execute()
			        {
			          if(card.getController().equals(Constant.Player.Human))
			            AllZone.InputControl.setInput(inputComes);
			          else //computer
			          {
			            abilityComes.setTargetPlayer(Constant.Player.Human);
			            AllZone.Stack.add(abilityComes);
			          }//else
			        }//execute()
			      };//CommandComes
			      Command commandLeavesPlay = new Command()
			      {

					private static final long serialVersionUID = 9172348861441804625L;

					public void execute()
			        {
			          //System.out.println(abilityComes.getTargetCard().getName());

			          SpellAbility ability = new Ability(card, "0")
			          {
			            public void resolve()
			            {
			              String player = abilityComes.getTargetPlayer();
			              AllZone.GameAction.getPlayerLife(player).addLife(6);
			              
			            }//resolve()
			          };//SpellAbility
			          ability.setStackDescription("Laquatus's Champion - " +abilityComes.getTargetPlayer()  +" regains 6 life.");
			          AllZone.Stack.add(ability);
			        }//execute()
			      };//Command

			      card.addComesIntoPlayCommand(commandComes);
			      card.addLeavesPlayCommand(commandLeavesPlay);
		    	  
		      }//*************** END ************ END **************************
	       
	       	  //*************** START *********** START **************************
	          else if(cardName.equals("Assembly-Worker"))
	          {
	            final SpellAbility[] a2 = new SpellAbility[1];
	            final Command eot2 = new Command()
	              {
	               private static final long serialVersionUID = 6180724472470740160L;

	               public void execute()
	                {
	                  Card c = a2[0].getTargetCard();
	                  if(AllZone.GameAction.isCardInPlay(c))
	                  {
	                    c.addTempAttackBoost(-1);
	                    c.addTempDefenseBoost(-1);
	                                         }
	                }
	              };

	            a2[0] = new Ability_Tap(card)
	            {
	            private static final long serialVersionUID = 3561450525225198222L;

	            public boolean canPlayAI()
	              {
	                return getAttacker() != null;
	              }
	              public void chooseTargetAI()
	              {
	                setTargetCard(getAttacker());
	              }
	              public Card getAttacker()
	              {
	                //target creature that is going to attack
	                Combat c = ComputerUtil.getAttackers();
	                CardList att = new CardList(c.getAttackers());
	                att.remove(card);
	                att.shuffle();

	                if(att.size() != 0)
	                  return att.get(0);
	                else
	                  return null;
	              }//getAttacker()

	              public void resolve()
	              {
	                Card c = a2[0].getTargetCard();
	                if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card,c) )
	                {
	                  c.addTempAttackBoost(1);
	                  c.addTempDefenseBoost(1);
	                  AllZone.EndOfTurn.addUntil(eot2);
	                }
	              }//resolve()
	            };//SpellAbility
	            card.addSpellAbility(a2[0]);
	            a2[0].setDescription("tap: Target Assembly-Worker creature gets +1/+1 until end of turn.");


	            @SuppressWarnings("unused") // target unused
	         final Input target = new Input()
	            {
	             private static final long serialVersionUID = 8913477363141356082L;
	            
	             public void showMessage()
	              {
	                ButtonUtil.enableOnlyCancel();
	                AllZone.Display.showMessage("Select Assembly-Worker to get +1/+1");
	              }
	              public void selectCard(Card c, PlayerZone zone)
	              {
	               if(!CardFactoryUtil.canTarget(card, c)){
	                     AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
	               }
	               else if(c.isCreature() && c.getType().contains("Assembly-Worker"))
	               {
	                  card.tap();
	                  AllZone.Human_Play.updateObservers();

	                  a2[0].setTargetCard(c);//since setTargetCard() changes stack description
	                  a2[0].setStackDescription(c +" gets +1/+1 until EOT");

	                  AllZone.InputControl.resetInput();
	                  AllZone.Stack.add(a2[0]);
	                }
	              }//selectCard()
	              public void selectButtonCancel()
	              {
	                card.untap();
	                stop();
	              }
	            };//Input target
	            a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Assembly-Worker"));

	          }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	          else if(cardName.equals("Wood Elves"))
	          {
	            final SpellAbility ability = new Ability(card, "0")
	            {
	              public void resolve()
	              {
	                Card c = card;
	                String player = c.getController();
	                //PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	                //life.addLife(2);
	                PlayerZone lib = AllZone.getZone(Constant.Zone.Library,
	                        player);
	                 

	                  CardList lands = new CardList(lib.getCards());
	                  lands = lands.getType("Forest");

	                  if (player.equals("Human") && lands.size() > 0)
	                  {
	                       Object o = AllZone.Display
	                              .getChoiceOptional(
	                                    "Pick a forest card to put into play",
	                                    lands.toArray());
	                        if (o != null)
	                        {
	                           Card card = (Card) o;
	                           lib.remove(card);
	                           AllZone.Human_Play.add(card);
	                         //  card.tap();
	                           lands.remove(card);
	                           AllZone.GameAction.shuffle(player);
	                        }
	                     } // player equals human
	               else if (player.equals("Computer") && lands.size() > 0)
	               {
	                  Card card = lands.get(0);
	                  lib.remove(card);
	                  // hand.add(card);
	                  AllZone.Computer_Play.add(card);
	               //   card.tap();
	                  lands.remove(card);
	                  AllZone.GameAction.shuffle(player);
	               }
	              }
	            };
	            Command intoPlay = new Command()
	            {
	            private static final long serialVersionUID = 1832932499373431651L;

	            public void execute()
	              {
	                ability.setStackDescription(card.getController() +" searches his library for a Forest card to put that card into play.");
	                AllZone.Stack.add(ability);
	              }
	            };
	            card.addComesIntoPlayCommand(intoPlay);
	          }//*************** END ************ END **************************
	       
	       
	     //*************** START *********** START **************************
	          else if(cardName.equals("Ambassador Laquatus"))
	          {
	            final SpellAbility a1 = new Ability(card, "3")
	            {
	              
	                public void resolve()
	                {
	                       String player = getTargetPlayer();
	                       
	                       PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                       PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	                       CardList libList = new CardList(lib.getCards());

	                       int max = 3;
	                       if (libList.size() < 3)
	                          max = libList.size();
	                       
	                       for (int i=0;i<max;i++)
	                       {
	                          Card c = libList.get(i);
	                          lib.remove(c);
	                          grave.add(c);
	                       }
	               
	                }
	             
	                public boolean canPlayAI()
	                {
	                  String player = getTargetPlayer();
	                   PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                   CardList libList = new CardList(lib.getCards());
	                   return libList.size() > 0;
	                }
	            };//SpellAbility
	            card.addSpellAbility(a1);
	            a1.setDescription("3: Target player puts the top three cards of his or her library into his or her graveyard.");
	            a1.setStackDescription("Player puts the top three cards of his or her library into his or her graveyard");
	            a1.setBeforePayMana(new Input_PayManaCost(a1));
	            a1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(a1));
	          }//*************** END ************ END **************************
	       
	       
	     //*************** START *********** START **************************
	          else if(cardName.equals("Vedalken Entrancer"))
	          {
	            final SpellAbility a1 = new Ability_Tap(card, "U")
	            {
					private static final long serialVersionUID = 2359247592519063187L;

					public void resolve()
	                {
	                       String player = getTargetPlayer();
	                       
	                       PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                       PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	                       CardList libList = new CardList(lib.getCards());

	                       int max = 2;
	                       if (libList.size() < 2)
	                          max = libList.size();
	                       
	                       for (int i=0;i<max;i++)
	                       {
	                          Card c = libList.get(i);
	                          lib.remove(c);
	                          grave.add(c);
	                       }
	               
	               }
	             
	             
	               public boolean canPlayAI()
	               {
	                  String player = getTargetPlayer();
	                   PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                   CardList libList = new CardList(lib.getCards());
	                   return libList.size() > 0;
	               }
	            };//SpellAbility
	            card.addSpellAbility(a1);
	            a1.setDescription("U, tap: Target player puts the top two cards of his or her library into his or her graveyard.");
	            a1.setStackDescription("Player puts the top two cards of his or her library into his or her graveyard");
	            a1.setBeforePayMana(new Input_PayManaCost(a1));
	            a1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(a1));
	          }//*************** END ************ END **************************

		     //*************** START *********** START **************************
	          else if(cardName.equals("Cathartic Adept"))
	          {
	            final Ability_Tap a1 = new Ability_Tap(card)
	            {
					private static final long serialVersionUID = 2359247592519063187L;

					public void resolve()
	                {
	                       String player = getTargetPlayer();
	                       
	                       PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                       PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	                       CardList libList = new CardList(lib.getCards());

	                       int max = 1;
	                       if (libList.size() < 1)
	                          max = libList.size();
	                       
	                       for (int i=0;i<max;i++)
	                       {
	                          Card c = libList.get(i);
	                          lib.remove(c);
	                          grave.add(c);
	                       }
	               
	               }
	               public boolean canPlayAI()
	               {
	                  String player = getTargetPlayer();
	                   PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                   CardList libList = new CardList(lib.getCards());
	                   return libList.size() > 0;
	               }
	            };//SpellAbility
	           
	            //not sure why, but this card doesn't seem to want to tap:
	            final Command tap =  new Command()
	            {
					private static final long serialVersionUID = -6290276896549170403L;

					public void execute()
	            	{
	            		card.tap();
	            	}
	            };
	            
	            a1.setDescription("tap: Target player puts the top card of his or her library into his or her graveyard.");
	            a1.setStackDescription("Player puts the top card of his or her library into his or her graveyard");
	            //a1.setBeforePayMana(new Input_PayManaCost(a1));
	            a1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(a1, tap));
	            card.addSpellAbility(a1);
	          }//*************** END ************ END **************************

	       
	       	  //*************** START *********** START **************************
	          else if (cardName.equals("Meddling Mage"))
	          {
	        	  final String[] input = new String[1];
	        	  final Ability ability = new Ability(card, "0")
	        	  {
	        		  public void resolve()
	        		  {
	        			  if (card.getController().equals(Constant.Player.Human)) {
	        				  input[0] = (String)JOptionPane.showInputDialog(null, "Which card?", "Pick card", JOptionPane.QUESTION_MESSAGE);
	        				  card.setNamedCard(input[0]);
	        			  }
	        			  else
	        			  {
	        				  String s = "Ancestral Recall";
	        				  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);
	        				  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human);
	        				  
	        				  CardList list = new CardList();
	        				  list.addAll(hand.getCards());
	        				  list.addAll(lib.getCards());
	        				  list = list.filter(new CardListFilter()
	        				  {
	        					 public boolean addCard(Card c)
	        					 {
	        						 return !c.isLand() && !c.isUnCastable();
	        					 }
	        				  });
	        				  
	        				  if (list.size() > 0 ) {
		        				  CardList rare = new CardList();
		        				  rare = list;
		        				  rare = rare.filter(new CardListFilter()
		        				  {
		        					 public boolean addCard(Card c)
		        					 {
		        						 return c.getRarity().equals("Rare");
		        					 }
		        				  });
		        				  
		        				  if (rare.size() > 0) {
		        					  s = rare.get(CardUtil.getRandomIndex(rare)).getName();
		        				  }
		        				  else
		        				  {
		        					  Card c = list.get(CardUtil.getRandomIndex(list));
		        					  //System.out.println(c + " - " + c.getRarity());
		        					  s = c.getName();
		        				  }
	        				  }
	        				  
	        				  card.setNamedCard(s);  
	        				  
	        			  }
	        				  
	        		  }
	        	  };
	        	  Command comesIntoPlay = new Command()
	    	      {
	    			private static final long serialVersionUID = 8485080996453793968L;

	    			public void execute()
	    	        {
	    	          AllZone.Stack.add(ability);
	    	        }
	    	      };//Command
	    	      ability.setStackDescription("As Meddling Mage enters the battlefield, name a nonland card.");
	    	      card.addComesIntoPlayCommand(comesIntoPlay);
	          }//*************** END ************ END **************************
	       
	       	  //*************** START *********** START **************************
	          else if (cardName.equals("Iona, Shield of Emeria"))
	          {
	        	  final Ability ability = new Ability(card, "0")
	        	  {
	        		  public void resolve()
	        		  {
	        			  if (card.getController().equals(Constant.Player.Human)) {

	                          String color = new String();
	                          String[] colors = Constant.Color.Colors;
	                          colors[colors.length-1] = null;
	                          
	                          Object o = AllZone.Display.getChoice("Choose color", colors);
	                          color = (String)o;
	                          card.setChosenColor(color);
	        			  }
	        			  else
	        			  {
	        				  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human);
	        				  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);
	        				  CardList list = new CardList();
	        				  list.addAll(lib.getCards());
	        				  list.addAll(hand.getCards());
	        				  
	        				  if (list.size() > 0)
	        				  {  
	        					  String color = CardFactoryUtil.getMostProminentColor(list);
	        					  if (!color.equals(""))
	        						  card.setChosenColor(color);
	        					  else
	        						  card.setChosenColor("black");
	        				  }
	        				  else 
	        				  {
	        					  card.setChosenColor("black");
	        				  }
	        			  }
	        		  }
	        	  };
	        	  Command comesIntoPlay = new Command()
	    	      {
					private static final long serialVersionUID = 3331342605626623161L;

					public void execute()
	    	        {
	    	          AllZone.Stack.add(ability);
	    	        }
	    	      };//Command
	    	      ability.setStackDescription("As Iona, Shield of Emeria enters the battlefield, choose a color.");
	    	      card.addComesIntoPlayCommand(comesIntoPlay);
	       }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	      else if (cardName.equals("Phantom Nishoba"))
	      {
	    	  final Card newCard = new Card()
	    	  {
		    	  public void addDamage(final int n, Card source)
			      {
		    		  this.subtractCounter(Counters.P1P1, 1);
			      }
	    	  };
	    	  
	    	  newCard.setOwner(card.getOwner());
		      newCard.setController(card.getController());

		      newCard.setManaCost(card.getManaCost());
		      newCard.setName(card.getName());
		      newCard.addType("Creature");
		      newCard.addType("Cat");
		      newCard.addType("Beast");
		      newCard.addType("Spirit");
		      newCard.setText(card.getSpellText());
		      newCard.setBaseAttack(card.getBaseAttack());
		      newCard.setBaseDefense(card.getBaseDefense());
		      
		      newCard.addIntrinsicKeyword("Trample");
		      newCard.addIntrinsicKeyword("Lifelink");

		      newCard.addSpellAbility(new Spell_Permanent(newCard));
		      
		      Command comesIntoPlay = new Command()
    	      {
				private static final long serialVersionUID = -2570661526160966399L;

				public void execute()
    	        {
    	           newCard.addCounter(Counters.P1P1, 7);
    	        }
    	      };//Command
		      
    	      newCard.addComesIntoPlayCommand(comesIntoPlay);
    	      
		      return newCard;
	      }
	       
	      //*************** START *********** START **************************
	      else if (cardName.equals("Phantom Centaur"))
	      {
	    	  final Card newCard = new Card()
	    	  {
		    	  public void addDamage(final int n, Card source)
			      {
		    		  this.subtractCounter(Counters.P1P1, 1);
			      }
	    	  };
	    	  newCard.setOwner(card.getOwner());
		      newCard.setController(card.getController());

		      newCard.setManaCost(card.getManaCost());
		      newCard.setName(card.getName());
		      newCard.addType("Creature");
		      newCard.addType("Centaur");
		      newCard.addType("Spirit");
		      newCard.setText(card.getSpellText());
		      newCard.setBaseAttack(card.getBaseAttack());
		      newCard.setBaseDefense(card.getBaseDefense());
		      
		      newCard.addIntrinsicKeyword("Protection from black");

		      newCard.addSpellAbility(new Spell_Permanent(newCard));
		      
		      Command comesIntoPlay = new Command()
    	      {

				private static final long serialVersionUID = 4217898403350036317L;

				public void execute()
    	        {
    	           newCard.addCounter(Counters.P1P1, 3);
    	        }
    	      };//Command
		      
    	      newCard.addComesIntoPlayCommand(comesIntoPlay);
    	      
		      return newCard;
	      }//*************** END ************ END **************************
	      
	      //*************** START *********** START **************************
	      else if (cardName.equals("Chainer, Dementia Master"))
	      {
	    	  final Ability ability = new Ability(card, "B B B")
	    	  {
	    		  public void resolve()
	    		  {
	    			  AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(3);
	    			  
	    			  PlayerZone hGrave = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human);
				      PlayerZone cGrave = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer);
				      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	    			  CardList creatures = new CardList();
	    			  creatures.addAll(hGrave.getCards());
	    			  creatures.addAll(cGrave.getCards());
	    			  
	    			  
	    			  creatures = creatures.getType("Creature");
	    			  if (creatures.size() > 0 ) 
	    			  {
		    			  if (card.getController().equals(Constant.Player.Human))
		    			  {
		    				  Object o = AllZone.Display.getChoice("Pick creature: " , creatures.toArray());
		    				  if (o!=null)
		  	        		  {
			  	        			Card c = (Card)o;
			  	        			PlayerZone zone = AllZone.getZone(c);
			  	        			zone.remove(c);
			  	        			play.add(c);
			  	        			card.untap();
			  	        			c.addExtrinsicKeyword(c.getName() + " is black.");
			  	        			c.addType("Nightmare");
			  	        			c.setController(card.getController());
		  	        		  }
		    			  }
		    			  else
		    			  {
		    				  Card c = CardFactoryUtil.AI_getBestCreature(creatures);
		    				  PlayerZone zone = AllZone.getZone(c);
		    				  zone.remove(c);
		    				  play.add(c);
		    				  card.untap();
		    			  }
	    			  }
	    		  }
	    		  
	    		  public boolean canPlayAI()
	    		  {
	    			  if(AllZone.Computer_Life.getLife() < 7)
    					  return false;
	    			  
	    			  PlayerZone hGrave= AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human);
				      PlayerZone cGrave = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer);
	    			  CardList creatures = new CardList();
	    			  creatures.addAll(hGrave.getCards());
	    			  creatures.addAll(cGrave.getCards());
	    			  creatures = creatures.getType("Creature");
	    			  return creatures.size() > 0;
	    		  }
	    		  
	    		  public boolean canPlay()
	    		  {
	    			  PlayerZone hGrave= AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human);
				      PlayerZone cGrave = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer);
	    			  CardList creatures = new CardList();
	    			  creatures.addAll(hGrave.getCards());
	    			  creatures.addAll(cGrave.getCards());
	    			  creatures = creatures.getType("Creature");
	    			  return creatures.size() > 0;
	    		  }
	    		  
	    	  };
	    	  
	    	  card.addSpellAbility(ability);
	    	  
	    	  final Command leavesPlay = new Command()
	    	  {
	    		  private static final long serialVersionUID = 3367460511478891560L;

				  public void execute()
	    		  {
	    			 PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
			    	 PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
			    	
			    	 CardList list = new CardList();
			    	 list.addAll(hPlay.getCards());
			    	 list.addAll(cPlay.getCards());
			    	 list = list.getType("Nightmare");
			    	 
			    	 for (Card c : list)
			    	 {
			    		 AllZone.GameAction.removeFromGame(c);
			    	 }
	    		  }
	    		
	    	  };
	    	  
	    	  card.addLeavesPlayCommand(leavesPlay);
	      }//*************** END ************ END **************************
	       
	       
	     //*************** START *********** START **************************
	      else if (cardName.equals("Swans of Bryn Argoll"))
	      {
	    	  final Card newCard = new Card()
	    	  {
		    	  public void addDamage(final int n, final Card source)
			      {
		    		  final Ability ability = new Ability(card, "0")
		    		  {
		    			  public void resolve(){
				    		  String player = source.getController();
				    		  for (int i=0;i<n;i++)
				    		  	AllZone.GameAction.drawCard(player);
		    			  }
		    		  };
		    		  ability.setStackDescription("Swans of Bryn Argoll - " +source.getController() + " draws " + n +" cards.");
			          AllZone.Stack.add(ability);
			      }
	    	  };
	    	  
	    	  newCard.setOwner(card.getOwner());
		      newCard.setController(card.getController());

		      newCard.setManaCost(card.getManaCost());
		      newCard.setName(card.getName());
		      newCard.addType("Creature");
		      newCard.addType("Bird");
		      newCard.addType("Spirit");
		      newCard.setText(card.getSpellText());
		      newCard.setBaseAttack(card.getBaseAttack());
		      newCard.setBaseDefense(card.getBaseDefense());
		      
		      newCard.addIntrinsicKeyword("Flying");

		      newCard.addSpellAbility(new Spell_Permanent(newCard));
		      
		      return newCard;
		  }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	      else if (cardName.equals("Dromad Purebred"))
	      {
	    	  final Card newCard = new Card()
	    	  {
		    	  public void addDamage(HashMap<Card, Integer> map)
			      {
		    		  final Ability ability = new Ability(card, "0")
		    		  {
		    			  public void resolve(){
		    				  String player = card.getController();
				    		  AllZone.GameAction.getPlayerLife(player).addLife(1);
		    			  }
		    		  };
		    		  ability.setStackDescription(card.getName() + " - " +card.getController() + " gains 1 life.");
			          AllZone.Stack.add(ability);
		    		
			      }
	    	  };
	    	  
	    	  newCard.setOwner(card.getOwner());
		      newCard.setController(card.getController());

		      newCard.setManaCost(card.getManaCost());
		      newCard.setName(card.getName());
		      newCard.addType("Creature");
		      newCard.addType("Camel");
		      newCard.addType("Beast");
		      newCard.setText(card.getSpellText());
		      newCard.setBaseAttack(card.getBaseAttack());
		      newCard.setBaseDefense(card.getBaseDefense());

		      newCard.addSpellAbility(new Spell_Permanent(newCard));
		      
		      return newCard;
		  }//*************** END ************ END **************************
		  
		  //*************** START *********** START **************************
          else if(cardName.equals("Thoughtcutter Agent"))
          {
        final SpellAbility ability = new Ability_Tap(card, "U B")
            {
            private static final long serialVersionUID = -3880035465617987801L;
          public void resolve()
              {
                String opponent = AllZone.GameAction.getOpponent(card.getController());
                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
                AllZone.GameAction.getPlayerLife(opponent).subtractLife(1);
                Card[] handLook= hand.getCards();
                if(opponent.equals(Constant.Player.Computer))
                {
                   AllZone.Display.getChoice("Look", handLook);
                }
                                      
              }
              public boolean canPlayAI()
              {
                //computer should play ability if this creature doesn't attack
                Combat c = ComputerUtil.getAttackers();
                CardList list = new CardList(c.getAttackers());

                //could this creature attack?, if attacks, do not use ability
                return (! list.contains(card));
              }
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("U B, tap: Target player loses 1 life and reveals his or her hand.");
            ability.setStackDescription(card.getName() + " - Opponent loses 1 life.");
          }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
          else if(cardName.equals("Singe-Mind Ogre"))
           {
             final SpellAbility ability = new Ability(card, "0")
             {
               public void resolve()
               {
                  Card choice = null;
                  String opponent = AllZone.GameAction.getOpponent(card.getController());
                   PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
                   Card[] handChoices = hand.getCards();
                   choice = CardUtil.getRandom(handChoices);
                   handChoices[0]=choice;
                   for (int i=1; i<handChoices.length; i++)
                   {
                      handChoices[i]=null;
                   }
                   AllZone.Display.getChoice("Random card", handChoices);                    
                   AllZone.GameAction.getPlayerLife(opponent).subtractLife(CardUtil.getConvertedManaCost(choice.getManaCost()));
               }//resolve()
             };
             Command intoPlay = new Command()
             {
          
             private static final long serialVersionUID = -4833144157620224716L;

             public void execute()
               {
                 ability.setStackDescription("Singe-Mind Ogre - target player reveals a card at random from his or her hand, then loses life equal to that card's converted mana cost.");
                 AllZone.Stack.add(ability);
               }
             };
             card.addComesIntoPlayCommand(intoPlay);
          }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	      else if (cardName.equals("Lichenthrope"))
	      {
	    	  final Card newCard = new Card()
	    	  {
		    	  public void addDamage(final int n, final Card source)
			      {
		    		  final Ability ability = new Ability(card, "0")
		    		  {
		    			  public void resolve(){
				    		  for (int i=0;i<n;i++)
				    		  	card.addCounter(Counters.M1M1, n);
		    			  }
		    		  };
		    		  ability.setStackDescription(card.getName() + " - gets " + n +" -1/-1 counters.");
			          AllZone.Stack.add(ability);
			      }
	    	  };
	    	  
	    	  newCard.setOwner(card.getOwner());
		      newCard.setController(card.getController());

		      newCard.setManaCost(card.getManaCost());
		      newCard.setName(card.getName());
		      newCard.addType("Creature");
		      newCard.addType("Plant");
		      newCard.addType("Fungus");
		      newCard.setText(card.getSpellText());
		      newCard.setBaseAttack(card.getBaseAttack());
		      newCard.setBaseDefense(card.getBaseDefense());
		      
		      newCard.addSpellAbility(new Spell_Permanent(newCard));
		      
		      return newCard;
		  }//*************** END ************ END **************************
	       
	       
	     //*************** START *********** START **************************
	      else if (cardName.equals("Phytohydra"))
	      {
	    	  final Card newCard = new Card()
	    	  {
		    	  public void addDamage(final int n, final Card source)
			      {
		    		  this.addCounter(Counters.P1P1, n);
			      }
	    	  };
	    	  
	    	  newCard.setOwner(card.getOwner());
		      newCard.setController(card.getController());

		      newCard.setManaCost(card.getManaCost());
		      newCard.setName(card.getName());
		      newCard.addType("Creature");
		      newCard.addType("Plant");
		      newCard.addType("Hydra");
		      newCard.setText(card.getSpellText());
		      newCard.setBaseAttack(card.getBaseAttack());
		      newCard.setBaseDefense(card.getBaseDefense());
		      
		      newCard.addSpellAbility(new Spell_Permanent(newCard));
		      
		      return newCard;
		  }//*************** END ************ END **************************
	       
	       
	     //*************** START *********** START **************************
	      else if (cardName.equals("Callous Giant"))
	      {
	    	  final Card newCard = new Card()
	    	  {
		    	  public void addDamage(int n, Card source)
			      {		    			  
		    		  if (n <= 3)
		    			  n = 0;
		    		  super.addDamage(n, source);
			      }
	    	  };
	    	  
	    	  newCard.setOwner(card.getOwner());
		      newCard.setController(card.getController());

		      newCard.setManaCost(card.getManaCost());
		      newCard.setName(card.getName());
		      newCard.addType("Creature");
		      newCard.addType("Giant");
		      newCard.setText(card.getSpellText());
		      newCard.setBaseAttack(card.getBaseAttack());
		      newCard.setBaseDefense(card.getBaseDefense());
		      
		      newCard.addSpellAbility(new Spell_Permanent(newCard));
		      
		      return newCard;
		  }//*************** END ************ END **************************
	       
	       /*
	      else if (cardName.equals("Acridian"))
	      {
	    	  card.setEchoCost(card.getManaCost());
	    	  
	    	  final Command intoPlay = new Command()
	    	  {
				private static final long serialVersionUID = 8930023870127082001L;

				public void execute() {
	    			  card.addIntrinsicKeyword("(Echo unpaid)");
	    		  }
	    	  };
	    	  card.addComesIntoPlayCommand(intoPlay);
	      }
	      */
	       
	       
	      // Cards with Cycling abilities
	      // -1 means keyword "Cycling" not found
	      if (shouldCycle(card) != -1)
	      {
	        int n = shouldCycle(card);
	        if (n != -1)
	        {
	          String parse = card.getKeyword().get(n).toString();
	          card.removeIntrinsicKeyword(parse);
	
	          String k[] = parse.split(":");
	          final String manacost = k[1];
	
	          card.addSpellAbility(CardFactoryUtil.ability_cycle(card, manacost));
	        }
	      }//Cycling

	      while (shouldTypeCycle(card) != -1)
	      {
	        int n = shouldTypeCycle(card);
	        if (n != -1)
	        {
	          String parse = card.getKeyword().get(n).toString();
	          card.removeIntrinsicKeyword(parse);
	
	          String k[] = parse.split(":");
	          final String type = k[1];
	          final String manacost = k[2];
	
	          card.addSpellAbility(CardFactoryUtil.ability_typecycle(card, manacost, type));
	        }
	      }//TypeCycling
	      
	      if (shouldTransmute(card) != -1)
	      {
	        int n = shouldTransmute(card);
	        if (n != -1)
	        {
	          String parse = card.getKeyword().get(n).toString();
	          card.removeIntrinsicKeyword(parse);
	
	          String k[] = parse.split(":");
	          final String manacost = k[1];
	
	          card.addSpellAbility(CardFactoryUtil.ability_transmute(card, manacost));
	        }
	      }//Transmute

	      while (shouldSoulshift(card) != -1)
	      {
	        int n = shouldSoulshift(card);
	        if (n != -1)
	        {
	          String parse = card.getKeyword().get(n).toString();
	          card.removeIntrinsicKeyword(parse);
	
	          String k[] = parse.split(":");
	          final String manacost = k[1];
	          card.addSpellAbility(CardFactoryUtil.soul_desc(card, manacost));
	          card.addDestroyCommand(CardFactoryUtil.ability_Soulshift(card, manacost));
	        }
	      }//Soulshift

	      if (shouldVanish(card) != -1)
	      {
	        int n = shouldVanish(card);
	        if (n != -1)
	        {
	          String parse = card.getKeyword().get(n).toString();
	          card.removeIntrinsicKeyword(parse);
	
	          String k[] = parse.split(":");	          
	          final int power = Integer.parseInt(k[1]);
	
	          card.addComesIntoPlayCommand(CardFactoryUtil.vanishing(card, power));
	          card.addSpellAbility(CardFactoryUtil.vanish_desc(card, power));
	        }
	      }//Vanishing
	      
	      if (hasKeyword(card, "Echo") != -1)
	      {
	        int n = hasKeyword(card, "Echo");
	        if (n != -1)
	        {
	          String parse = card.getKeyword().get(n).toString();
	          //card.removeIntrinsicKeyword(parse);

	          String k[] = parse.split(":");
	          final String manacost = k[1];

	          card.setEchoCost(manacost);
	          
	          final Command intoPlay = new Command()
	    	  {
				private static final long serialVersionUID = 8930023870127082001L;

				public void execute() {
	    			  card.addIntrinsicKeyword("(Echo unpaid)");
	    		}
	    	  };
	    	  card.addComesIntoPlayCommand(intoPlay);
	          
	        }
	      }//echo
	      
	      /*
	      if (hasKeyword(card, "Upkeep") != -1)
	      {
	        int n = hasKeyword(card, "Upkeep");
	        if (n != -1)
	        {
	          String parse = card.getKeyword().get(n).toString();
	          card.removeIntrinsicKeyword(parse);

	          String k[] = parse.split(":");
	          final String manacost = k[1];
	          card.addIntrinsicKeyword("At the beginning of your upkeep, sacrifice " + card.getName() + " unless you pay " + manacost+".");
	          
	          final Command intoPlay = new Command()
	    	  {
				private static final long serialVersionUID = 925179072354331141L;

				public void execute() {
	    			  card.setUpkeepCost(manacost);
	    		}
	    	  };
	    	  card.addComesIntoPlayCommand(intoPlay);
	          
	        }
	      }//upkeep
	      */
	      
		 return card;
	}
}