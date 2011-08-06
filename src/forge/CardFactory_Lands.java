package forge;

import java.util.ArrayList;

class CardFactory_Lands {
	
	private static final int hasKeyword(Card c, String k)
    {
    	ArrayList<String> a = c.getKeyword();
    	for (int i = 0; i < a.size(); i++)
    		if (a.get(i).toString().startsWith(k))
    			return i;

    	return -1;
    }
	
	public static Card getCard(final Card card, String cardName, String owner)
	{
		
//	    computer plays 2 land of these type instead of just 1 per turn

	    
		//*************** START *********** START **************************
		if(cardName.equals("Oran-Rief, the Vastwood"))
		{
			card.clearSpellKeepManaAbility();
			
			final CardListFilter targets = new CardListFilter()
			{

				public boolean addCard(Card c) {
					return AllZone.GameAction.isCardInPlay(c) && c.isCreature()
						&& c.getTurnInZone() == AllZone.Phase.getTurn()
						&& CardUtil.getColors(c).contains(Constant.Color.Green);
				}
				
			};
			Ability_Tap ability = new Ability_Tap(card)
			{

				private static final long serialVersionUID = 1416258136308898492L;

				CardList inPlay = new CardList();
				public boolean canPlayAI()
				{
					if(!(AllZone.Phase.getPhase().equals(Constant.Phase.Main1)
					  && AllZone.Phase.getActivePlayer().equals(Constant.Player.Computer)))
						return false;
					inPlay.clear();
					inPlay.addAll(AllZone.Computer_Play.getCards());
					return (inPlay.filter(targets).size() > 1);
				}
				public void resolve() {
					inPlay.clear();
					inPlay.addAll(AllZone.Human_Play.getCards());
					inPlay.addAll(AllZone.Computer_Play.getCards());
					for(Card targ : inPlay.filter(targets))
						targ.addCounter(Counters.P1P1, 1);
				}
			};
			ability.setDescription("tap: Put a +1/+1 counter on each green creature that entered the battlefield this turn.");
			ability.setStackDescription("Put a +1/+1 counter on each green creature that entered the battlefield this turn.");
			card.addSpellAbility(ability);
		}
		//*************** END ************ END **************************
	    
		//*************** START *********** START **************************
		//Ravinca Dual Lands
	    if(cardName.equals("Blood Crypt") || cardName.equals("Breeding Pool") || cardName.equals("Godless Shrine") || cardName.equals("Hallowed Fountain") || cardName.equals("Overgrown Tomb") || cardName.equals("Sacred Foundry") || cardName.equals("Steam Vents") || cardName.equals("Stomping Ground") || cardName.equals("Temple Garden") || cardName.equals("Watery Grave"))
	    {
	      //if this isn't done, computer plays more than 1 copy
	      //card.clearSpellAbility();
	      card.clearSpellKeepManaAbility();

	      card.addComesIntoPlayCommand(new Command()
	      {
			private static final long serialVersionUID = 7352127748114888255L;
			
			public void execute()
	        {
	          if(card.getController().equals(Constant.Player.Human))
	            humanExecute();
	          else
	            computerExecute();
	        }
	        public void computerExecute()
	        {
	          boolean pay = false;

	          if(AllZone.Computer_Life.getLife() > 9)
	            pay = MyRandom.random.nextBoolean();

	          if(pay)
	            AllZone.Computer_Life.subtractLife(2);
	          else
	            card.tap();
	        }
	        public void humanExecute()
	        {
	          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	          if(2 < life.getLife())
	          {
	            String[] choices = {"Yes", "No"};
	            Object o = AllZone.Display.getChoice("Pay 2 life?", choices);
	            if(o.equals("Yes"))
	              life.subtractLife(2);
	            else
	              tapCard();
	          }//if
	          else
	            tapCard();
	        }//execute()
	        private void tapCard()
	        {
	          card.tap();
	        }
	      });
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Kabira Crossroads"))
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
			private static final long serialVersionUID = -4550013855602477643L;

			public void execute()
	    	  {
				  card.tap();
	    		  ability.setStackDescription(card.getName() + " - " +card.getController() +" gains 2 life");
	    		  AllZone.Stack.add(ability);
	    	  }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Graypelt Refuge")|| cardName.equals("Sejiri Refuge")|| cardName.equals("Jwar Isle Refuge") || 
	       cardName.equals("Akoum Refuge")|| cardName.equals("Kazandu Refuge"))
	    {
	      final SpellAbility ability = new Ability(card, "0")
	      {
	        public void resolve()
	        {
	          Card c = card;
	          c.tap();
	          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
	          life.addLife(1);
	        }
	      };
	      Command intoPlay = new Command()
	      {
	      private static final long serialVersionUID = 5055232386220487221L;

	      public void execute()
	        {
	    	  card.tap();
	          ability.setStackDescription(card.getName() + " - " +card.getController() +" gains 1 life");
	          AllZone.Stack.add(ability);
	        }
	      };
	      card.addComesIntoPlayCommand(intoPlay);
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Faerie Conclave"))
	    {
	      card.addComesIntoPlayCommand(new Command()
	      {
			private static final long serialVersionUID = 2792041290726604698L;

			public void execute()
	        {
	          card.tap();
	        }
	      });

	      final Command eot1 = new Command()
	      {
			private static final long serialVersionUID = 5106629534549783845L;

			public void execute()
	        {
	          Card c = card;

	          c.setBaseAttack(0);
	          c.setBaseDefense(0);
	          c.removeIntrinsicKeyword("Flying");
	          c.removeType("Creature");
	          c.removeType("Faerie");
	          c.setManaCost("");
	        }
	      };

	      final SpellAbility a1 = new Ability(card, "1 U")
	      {
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	        public void resolve()
	        {
	          Card c = card;

	          c.setBaseAttack(2);
	          c.setBaseDefense(1);

	          //to prevent like duplication like "Flying Flying Creature Creature"
	          if(! c.getIntrinsicKeyword().contains("Flying"))
	          {
	            c.addIntrinsicKeyword("Flying");
	            c.addType("Creature");
	            c.addType("Faerie");
	            c.setManaCost("U");
	          }
	          AllZone.EndOfTurn.addUntil(eot1);
	        }
	      };//SpellAbility
//	      card.setManaCost("U");

	      card.clearSpellKeepManaAbility();
	      card.addSpellAbility(a1);
	      a1.setDescription("1 U: Faerie Conclave becomes a 2/1 blue Faerie creature with flying until end of turn. It's still a land.");
	      a1.setStackDescription(card +" becomes a 2/1 creature with flying until EOT");

	      Command paid1 = new Command() {
			private static final long serialVersionUID = -601119544294387668L;
			public void execute() {AllZone.Stack.add(a1);}
		  };

	      a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Forbidding Watchtower"))
	    {
	      card.addComesIntoPlayCommand(new Command()
	      {
			private static final long serialVersionUID = 5212793782060828409L;

			public void execute()
	        {
	          card.tap();
	        }
	      });

	      final Command eot1 = new Command()
	      {
			private static final long serialVersionUID = 8806880921707550181L;

			public void execute()
	        {
	          Card c = card;

	          c.setBaseAttack(0);
	          c.setBaseDefense(0);
	          c.removeType("Creature");
	          c.removeType("Soldier");
	          c.setManaCost("");
	        }
	      };

	      final SpellAbility a1 = new Ability(card, "1 W")
	      {
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	        public void resolve()
	        {
	          Card c = card;

	          c.setBaseAttack(1);
	          c.setBaseDefense(5);

	          //to prevent like duplication like "Creature Creature"
	          if(! c.getType().contains("Creature"))
	          {
	            c.addType("Creature");
	            c.addType("Soldier");
	            c.setManaCost("W");
	          }
	          AllZone.EndOfTurn.addUntil(eot1);
	        }
	      };//SpellAbility

	      card.clearSpellKeepManaAbility();
	      card.addSpellAbility(a1);
	      a1.setDescription("1 W: Forbidding Watchtower becomes a 1/5 white Soldier creature until end of turn. It's still a land.");
	      a1.setStackDescription(card +" becomes a 1/5 creature until EOT");

	      Command paid1 = new Command() {
			private static final long serialVersionUID = -7211256926392695778L;
			public void execute() {AllZone.Stack.add(a1);}
	      };

	      a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Treetop Village"))
	    {
	      card.addComesIntoPlayCommand(new Command()
	      {
			private static final long serialVersionUID = -2246560994818997231L;

			public void execute()
	        {
	          card.tap();
	        }
	      });

	      final Command eot1 = new Command()
	      {
			private static final long serialVersionUID = -8535770979347971863L;

			public void execute()
	        {
	          Card c = card;

	          c.setBaseAttack(0);
	          c.setBaseDefense(0);
	          c.removeType("Creature");
	          c.removeType("Ape");
	          c.removeIntrinsicKeyword("Trample");
	          c.setManaCost("");
	        }
	      };

	      final SpellAbility a1 = new Ability(card, "1 G")
	      {
	        public boolean canPlayAI()
	        {
	          return ! card.getType().contains("Creature");
	        }
	        public void resolve()
	        {
	          Card c = card;

	          c.setBaseAttack(3);
	          c.setBaseDefense(3);

	          //to prevent like duplication like "Creature Creature"
	          if(! c.getIntrinsicKeyword().contains("Trample"))
	          {
	            c.addType("Creature");
	            c.addType("Ape");
	            c.addIntrinsicKeyword("Trample");
	            c.setManaCost("G");
	          }
	          AllZone.EndOfTurn.addUntil(eot1);
	        }
	      };//SpellAbility

	      card.clearSpellKeepManaAbility();
	      card.addSpellAbility(a1);
	      a1.setStackDescription(card +" becomes a 3/3 creature with trample until EOT");
	      a1.setDescription("1 G: Treetop Village becomes a 3/3 green Ape creature with trample until end of turn. It's still a land.");

	      Command paid1 = new Command() {
			private static final long serialVersionUID = -6800983290478844750L;

			public void execute() {AllZone.Stack.add(a1);}
		  };

	      a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
	    }//*************** END ************ END **************************
	    
	    
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Blinkmoth Nexus"))
	    {
	      final SpellAbility a1 = new Ability(card, "1")
	      {
	        final Command eot1 = new Command()
	        {
	   	   private static final long serialVersionUID = 3564161001279001235L;

	   	   public void execute()
	          {
	            Card c = card;

	            c.setBaseAttack(0);
	            c.setBaseDefense(0);
	            c.removeIntrinsicKeyword("Flying");
	            c.removeType("Artifact");
	            c.removeType("Creature");
	            c.removeType("Blinkmoth");
	          }
	        };
	        public boolean canPlayAI()
	        {
	          return false;
	        }
	        public void resolve()
	        {
	          Card c = card;

	          c.setBaseAttack(1);
	          c.setBaseDefense(1);
	          //to prevent like duplication like "Flying Flying Creature Creature"
	          if(! c.getIntrinsicKeyword().contains("Flying"))
	          {
	            c.addIntrinsicKeyword("Flying");
	          }
	          c.addType("Artifact");
	          c.addType("Creature");
	          c.addType("Blinkmoth");

	          AllZone.EndOfTurn.addUntil(eot1);
	        }
	      };//SpellAbility
	      card.addSpellAbility(a1);
	      a1.setDescription("1: Blinkmoth Nexus becomes a 1/1 Blinkmoth artifact creature with flying until end of turn. It's still a land.");
	      a1.setStackDescription(card +" becomes a 1/1 creature with flying until EOT");

	      Command paid1 = new Command() {
	   	   private static final long serialVersionUID = -5122292582368202498L;
	   	   public void execute() {AllZone.Stack.add(a1);}
	      };
	      a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));

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

	      a2[0] = new Ability_Tap(card,"1")
	      {
	   	private static final long serialVersionUID = 3561450520225198222L;

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
	      a2[0].setDescription("1, tap: Target Blinkmoth gets +1/+1 until end of turn.");


	      @SuppressWarnings("unused") // target unused
	   final Input target = new Input()
	      {
	   	 private static final long serialVersionUID = 8913477363141356082L;
	   	
	   	 public void showMessage()
	        {
	          ButtonUtil.enableOnlyCancel();
	          AllZone.Display.showMessage("Select Blinkmoth to get +1/+1");
	        }
	        public void selectCard(Card c, PlayerZone zone)
	        {
	         if(!CardFactoryUtil.canTarget(card, c)){
	          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
	         }
	         else if(c.isCreature() && c.getType().contains("Blinkmoth"))
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
	      a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Blinkmoth"));
	    }//*************** END ************ END **************************




	       //*************** START *********** START **************************
	       else if(cardName.equals("Mishra's Factory"))
	       {
	         final Command eot1 = new Command()
	         {
	   		private static final long serialVersionUID = -956566640027406078L;

	   		public void execute()
	           {
	             Card c = card;

	             c.setBaseAttack(0);
	             c.setBaseDefense(0);
	             c.removeType("Artifact");
	             c.removeType("Creature");
	             c.removeType("Assembly-Worker");
	           }
	         };

	         final SpellAbility a1 = new Ability(card, "1")
	         {
	           public boolean canPlayAI()
	           {
	             return false;
	             //it turns into a creature, but doesn't attack
//	             return (! card.getKeyword().contains("Flying") &&
//	                    (CardFactoryUtil.AI_getHumanCreature("Flying").isEmpty()));
	           }
	           public void resolve()
	           {
	             Card c = card;

	             c.setBaseAttack(2);
	             c.setBaseDefense(2);
	             //to prevent like duplication like "Creature Creature"
	             if(! c.getKeyword().contains("Creature"))
	             {
	               c.addType("Artifact");
	               c.addType("Creature");
	               c.addType("Assembly-Worker");
	             }
	             AllZone.EndOfTurn.addUntil(eot1);
	           }
	         };//SpellAbility
	         card.addSpellAbility(a1);
	         a1.setDescription("1: Mishra's Factory becomes a 2/2 Assembly-Worker artifact creature until end of turn. It's still a land.");
	         a1.setStackDescription(card +" - becomes a 2/2 creature until EOT");

	         Command paid1 = new Command() {
	   		private static final long serialVersionUID = -6767109002136516590L;

	   		public void execute() {AllZone.Stack.add(a1);}
	   	  };

	         a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
	         
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
	            private static final long serialVersionUID = 3561450520225198222L;

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
	            a2[0].setDescription("tap: Target Assembly-Worker gets +1/+1 until end of turn.");


	            a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Assembly-Worker"));
	         
	       }//*************** END ************ END **************************
	       
	       //*************** START *********** START **************************
	       else if(cardName.equals("Terramorphic Expanse"))
	       {
	         //tap sacrifice
	         final Ability_Tap ability = new Ability_Tap(card, "0")
	         {
	           private static final long serialVersionUID = 5441740362881917927L;

	   		public boolean canPlayAI()
	           {
	             return false;
	             /*
	             //sacrifice Sakura-Tribe Elder if Human has any creatures
	             CardList list = new CardList(AllZone.Human_Play.getCards());
	             list = list.getType("Creature");
	             return list.size() != 0 && card.isUntapped();
	             */
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
	             if (super.canPlay() && list.size() > 0 && AllZone.GameAction.isCardInPlay(card))
	           	  return true;
	             else
	           	 return false;
	             		
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
	           private static final long serialVersionUID = -4379321114820908030L;
	   		boolean once = true;
	           public void showMessage()
	           {
	             //this is necessary in order not to have a StackOverflowException
	             //because this updates a card, it creates a circular loop of observers
	             if(once)
	             {
	               once = false;
	               AllZone.GameAction.sacrifice(card);

	               ability.setStackDescription(card.getController() +" - Search your library for a basic land card and put it into play tapped. Then shuffle your library.");
	               AllZone.Stack.add(ability);

	               stop();
	             }
	           }//showMessage()
	         };
	         card.addSpellAbility(ability);
	         ability.setDescription("tap, Sacrifice Terramorphic Expanse: Search your library for a basic land card and put it into play tapped. Then shuffle your library.");
	         ability.setBeforePayMana(runtime);
	       }//*************** END ************ END **************************
	    
	    
	       //*************** START *********** START **************************
	       else if(cardName.equals("Arid Mesa") || cardName.equals("Marsh Flats") || cardName.equals("Misty Rainforest") || 
	    		   cardName.equals("Scalding Tarn")  || cardName.equals("Verdant Catacombs") || 
	    		   cardName.equals("Bloodstained Mire") || cardName.equals("Flooded Strand") || cardName.equals("Polluted Delta") ||
	    		   cardName.equals("Windswept Heath") || cardName.equals("Wooded Foothills"))
	       {
	    	  
	    	 final String[] land1 = new String[1];
	    	 final String[] land2 = new String[1];
	    	 
	    	 if (cardName.equals("Arid Mesa")) { land1[0] = "Mountain"; land2[0] = "Plains";}
	    	 else if (cardName.equals("Marsh Flats")) { land1[0] = "Plains"; land2[0] = "Swamp";}
	    	 else if (cardName.equals("Misty Rainforest")) { land1[0] = "Forest"; land2[0] = "Island";}
	    	 else if (cardName.equals("Scalding Tarn")) { land1[0] = "Island"; land2[0] = "Mountain";}
	    	 else if (cardName.equals("Verdant Catacombs")) { land1[0] = "Swamp"; land2[0] = "Forest";}
	    	 else if (cardName.equals("Bloodstained Mire")) { land1[0] = "Swamp"; land2[0] = "Mountain";}
	    	 else if (cardName.equals("Flooded Strand")) { land1[0] = "Plains"; land2[0] = "Island";}
	    	 else if (cardName.equals("Polluted Delta")) { land1[0] = "Island"; land2[0] = "Swamp";}
	    	 else if (cardName.equals("Windswept Heath")) { land1[0] = "Forest"; land2[0] = "Plains";}
	    	 else if (cardName.equals("Wooded Foothills")) { land1[0] = "Mountain"; land2[0] = "Forest";}
	    	 
	         //tap sacrifice
	         final Ability_Tap ability = new Ability_Tap(card, "0")
	         {

			   private static final long serialVersionUID = 6865042319287843154L;

			   public boolean canPlayAI()
	           {
	             return false;
	           }
	           public void chooseTargetAI()
	           {
	             AllZone.GameAction.sacrifice(card);
	           }
	           public boolean canPlay()
	           {
	             PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	             CardList list = new CardList(library.getCards());
	             list = list.filter(new CardListFilter()
	             {
	            	public boolean addCard(Card c)
	            	{
	            		return c.getType().contains(land1[0]) || c.getType().contains(land2[0]);
	            	}
	             });
	             if (super.canPlay() && list.size() > 0 && AllZone.GameAction.isCardInPlay(card))
	           	  return true;
	             else
	           	  return false;
	             		
	           }//canPlay()
	           public void resolve()
	           {
	             if(card.getOwner().equals(Constant.Player.Human))
	               humanResolve();
	             //else
	             //  computerResolve();
	           }

	           public void humanResolve()
	           {
	             PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	             PlayerZone play    = AllZone.getZone(Constant.Zone.Play   , card.getController());

	             CardList full = new CardList(library.getCards());
	             CardList land = new CardList(library.getCards());
	             land = land.filter(new CardListFilter()
	             {
	            	 public boolean addCard(Card c)
	            	 {
	            		 return c.getType().contains(land1[0]) || c.getType().contains(land2[0]);
	            	 }
	             });

	             Object o = AllZone.Display.getChoiceOptional("Choose a " +land1[0] +  " or " + land2[0], full.toArray());
	             if(o != null)
	             {
	               
	               Card c = (Card)o;
	               if (land.contains(c))
	               {	
	            	   library.remove(c);
	            	   play.add(c);
	               }
	             }
	             AllZone.GameAction.shuffle(card.getController());
	           }//resolve()
	         };//SpellAbility

	         Input runtime = new Input()
	         {

			   private static final long serialVersionUID = -7328086812286814833L;
			   boolean once = true;
	           public void showMessage()
	           {
	             //this is necessary in order not to have a StackOverflowException
	             //because this updates a card, it creates a circular loop of observers
	             if(once)
	             {
	               once = false;
	               String player = card.getController();
	               AllZone.GameAction.getPlayerLife(player).subtractLife(1);
	               AllZone.GameAction.sacrifice(card);
	               
	               ability.setStackDescription(card.getController() +" - Search your library for a "+land1[0]+" or "+land2[0]+" card and put it onto the battlefield. Then shuffle your library.");
	               AllZone.Stack.add(ability);

	               stop();
	             }
	           }//showMessage()
	         };
	         card.addSpellAbility(ability);
	         ability.setDescription("Tap, Pay 1 life, Sacrifice " + card.getName() +": Search your library for a "+land1[0]+" or "+land2[0]+" card and put it onto the battlefield. Then shuffle your library.");
	         ability.setBeforePayMana(runtime);
	       }//*************** END ************ END **************************

	       
	     //*************** START *********** START **************************
	       else if(cardName.equals("Tortuga"))
	       {
	         final Input discardThenDraw = new Input()
	         {
	   		private static final long serialVersionUID = -7119292573232058526L;
	   		int nCards = 1;
	           int n = 0;

	           public void showMessage()
	           {
	             AllZone.Display.showMessage("Select a card to discard");
	             ButtonUtil.disableAll();

	             //in case no more cards in hand
	             if(n == nCards || AllZone.Human_Hand.getCards().length == 0)
	             {
	               stop();
	               AllZone.GameAction.drawCard(card.getController());
	               n = 0; //very important, otherwise the 2nd time you play this ability, you
	                      //don't have to discard
	             }
	           }
	           public void selectCard(Card card, PlayerZone zone)
	           {
	             if(zone.is(Constant.Zone.Hand))
	             {
	               AllZone.GameAction.discard(card);
	               n++;
	               showMessage();
	             }
	           }
	         };//SpellAbility

	         final Ability_Tap ability = new Ability_Tap(card)
	         {

	   		private static final long serialVersionUID = -2946606436670861559L;
	   		public boolean canPlayAI() {return false;}
	           public void resolve()
	           {
	             AllZone.InputControl.setInput(discardThenDraw);
	           }
	         };//SpellAbility
	         card.addSpellAbility(ability);
	         ability.setDescription("tap: Discard a card, then draw a card.");
	         ability.setStackDescription("Tortuga - Discard a card, then draw a card.");
	         ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));


	         final Ability_Tap ability2 = new Ability_Tap(card)
	         {
	   		private static final long serialVersionUID = 8961266883009597786L;
	   		public boolean canPlay()
	           {
	             PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	             return hand.getCards().length == 7;
	           }
	           public void resolve()
	           {
	             AllZone.GameAction.drawCard(card.getController());
	           }
	         };//SpellAbility
	         card.addSpellAbility(ability2);
	         ability2.setDescription("tap: Draw a card. Play this ability only if you have exactly 7 cards in your hand.");
	         ability2.setStackDescription("Tortuga - draw a card.");
	         ability2.setBeforePayMana(new Input_NoCost_TapAbility(ability2));

	       }//*************** END ************ END **************************


	       //*************** START *********** START **************************
	       else if(cardName.equals("Library of Alexandria"))
	       {
	         final Ability_Tap ability2 = new Ability_Tap(card)
	         {
	   		private static final long serialVersionUID = -3405763871882165537L;
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
	         ability2.setDescription("tap: Draw a card. Play this ability only if you have exactly 7 cards in your hand.");
	         ability2.setStackDescription("Library of Alexandria - draw a card.");
	         ability2.setBeforePayMana(new Input_NoCost_TapAbility(ability2));

	       }//*************** END ************ END **************************

	       //*************** START *********** START **************************
	       else if (cardName.equals("Dark Depths"))
	       {
	       	Command intoPlay = new Command()
	           {
	   		   private static final long serialVersionUID = 6805304924956145866L;
	   		  
	   		   public boolean firstTime = true;
	              public void execute()
	              {

	                if(firstTime){
	              	  card.setCounter(Counters.ICE, 10); 
	                }
	                firstTime = false;
	              }
	            };
	            
	            card.addComesIntoPlayCommand(intoPlay);
	            
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
	            	  
	            	  if (card.getCounters(Counters.ICE) > 0 && AllZone.GameAction.isCardInPlay(card))
	            		  return true;
	            	  else
	            		  return false;
	              }
	              public boolean canPlayAI() {
	           	   String phase = AllZone.Phase.getPhase();
	           	   return phase.equals(Constant.Phase.Main2);
	              }

	              public void resolve()
	              {
	           	 card.subtractCounter(Counters.ICE, 1);
	           	 
	           	 if (card.getCounters(Counters.ICE) == 0)
	           	 {
	   	             PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	   	
	   	             //make token
	   	             Card c = new Card();
	   	
	   	             c.setOwner(card.getController());
	   	             c.setController(card.getController());
	   	
	   	             c.setName("Marit Lage");
	   	             c.setName("B 20 20 Marit Lage");
	   	             c.setManaCost("B");
	   	             c.setToken(true);
	   	
	   	             c.addType("Legendary");
	   	             c.addType("Creature");
	   	             c.addType("Avatar");
	   	             c.addIntrinsicKeyword("Flying");
	   	             c.addExtrinsicKeyword("Indestructible");
	   	             c.setBaseAttack(20);
	   	             c.setBaseDefense(20);
	   	
	   	             play.add(c);
	   	             AllZone.GameAction.sacrifice(card);
	           	 }// if counters == 0
	              }
	            };
	            ability.setDescription("Dark Depths enters the battlefield with ten ice counters on it.\r\n\r\n3: Remove an ice counter from Dark Depths.\r\n\r\nWhen Dark Depths has no ice counters on it, sacrifice it. If you do, put an indestructible legendary 20/20 black Avatar creature token with flying named Marit Lage onto the battlefield.");
	            ability.setStackDescription(card.getName() +" - remove an ice counter.");
	            
	            card.addSpellAbility(ability);
	       	
	    }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	       else if(cardName.equals("Karakas"))
	       {
	     	  final Ability_Tap ability = new Ability_Tap(card, "0")
	     	  {

	     	  private static final long serialVersionUID = -6589125907956046586L;

	   		  public boolean canPlayAI()
     	      {
     	          CardList list = new CardList(AllZone.Human_Play.getCards());
     	          list = list.filter(new CardListFilter()
     	          {
     	        	  public boolean addCard(Card c) {
     	        		  return c.isCreature() && c.getKeyword().contains("Legendary");
     	        	  } 
     	          });
     	          
     	          if (list.size() > 0)
     	        	  setTargetCard(CardFactoryUtil.AI_getBestCreature(list, card));
     	          
     	          return list.size() > 0;
     	      }
	     		  
	     		  public void resolve()
	     		  {
	     			  Card c = getTargetCard();
	     			  
	     			  if (c!=null)
	     			  {
	     				  if ( CardFactoryUtil.canTarget(card, c) && c.isCreature() && c.getType().contains("Legendary") )
	     					  AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Hand, card.getOwner()), c);
	     			  }  
	     		  }
	     	  };
	     	  
	     	  Input runtime = new Input()
	         {

	   		private static final long serialVersionUID = -7649200192384343204L;

	   		public void showMessage()
	           {
	               CardList choice = new CardList();
	               choice.addAll(AllZone.Human_Play.getCards());
	               choice.addAll(AllZone.Computer_Play.getCards());

	               choice = choice.getType("Creature");
	               choice = choice.filter(new CardListFilter()
	               {
	                 public boolean addCard(Card c)
	                 {
	                   return (c.isCreature() && c.getType().contains("Legendary"));
	                 }
	               });
	               
	               //System.out.println("size of choice: " + choice.size());
	               stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice, "Select target Legendary creature:", true, false));
	             }
	           };
	     	  
	     	  ability.setDescription("tap: Return target legendary creature to its owner's hand.");
	     	  //ability.setStackDescription(card.getName() + " - gives target creature +1/+2 until end of turn.");
	     	  
	     	  card.addSpellAbility(ability);
	     	  ability.setBeforePayMana(runtime);
	     	  
	     	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	     	  //anyway, this does the trick:
	     	  //card.removeIntrinsicKeyword("tap: add G");
	     	  //card.setText(card.getSpellText() +  "\r\ntap: Return target legendary creature to its owner's hand.");
	     	  //card.addIntrinsicKeyword("tap: add G");
	     	  
	       }//*************** END ************ END **************************

	     //*************** START *********** START **************************
	     else if(cardName.equals("Pendelhaven"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "0")
	   	  {
	   		private static final long serialVersionUID = 8154776336533992188L;

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
	   	          CardList att = new CardList();
	   	          att.addAll(c.getAttackers());
	   	          
	   	          for (int i=0; i<att.size(); i++)
	   	          {
	   	        	  Card crd = att.get(i);
	   	        	  if (crd.getNetAttack() == 1 && crd.getNetDefense() == 1)
	   	        		  return crd;
	   	          }
	   	          
	   	          return null;
	   		  }//getAttacker()
	   		  
	   		  public void resolve()
	   		  {
	   	  
	   			  final Card[] target = new Card[1];
	   	          final Command untilEOT = new Command()
	   	          {

	   				private static final long serialVersionUID = 6362813153010836856L;

	   				public void execute()
	   	            {
	   	              if(AllZone.GameAction.isCardInPlay(target[0]))
	   	              {
	   	                target[0].addTempAttackBoost(-1);
	   	                target[0].addTempDefenseBoost(-2);
	   	              }
	   	            }
	   	          
	   	          };
	   	          
	   	          target[0] = getTargetCard();
	   	          if(AllZone.GameAction.isCardInPlay(target[0]) && target[0].getNetDefense() == 1 && target[0].getNetAttack() == 1 && CardFactoryUtil.canTarget(card, target[0]))
	   	          {
	   	            target[0].addTempAttackBoost(1);
	   	            target[0].addTempDefenseBoost(2);
	   	
	   	            AllZone.EndOfTurn.addUntil(untilEOT);
	   	          }
	   		  }
	   	  };
	   	  
	   	  Input runtime = new Input()
	         {
	   		private static final long serialVersionUID = 6126636768830864856L;

	   		public void showMessage()
	           {
	             CardList choice = new CardList();
	             choice.addAll(AllZone.Human_Play.getCards());
	             choice.addAll(AllZone.Computer_Play.getCards());

	             choice = choice.getType("Creature");
	             choice = choice.filter(new CardListFilter()
	             {
	               public boolean addCard(Card c)
	               {
	                 return (c.getNetAttack() == 1 && c.getNetDefense() == 1);
	               }
	             });
	             
	             //System.out.println("size of choice: " + choice.size());
	             stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice, "Select target 1/1 Creature:", true, false));
	           }
	         };
	   	  
	   	  ability.setDescription("tap: Target 1/1 creature gets +1/+2 until end of turn.");
	   	  ability.setStackDescription(card.getName() + " - gives target creature +1/+2 until end of turn.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  ability.setBeforePayMana(runtime);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add G");
	   	  //card.setText(card.getSpellText() +  "\r\ntap: Target 1/1 creature gets +1/+2 until end of turn.");
	   	  //card.addIntrinsicKeyword("tap: add G");
	   	  
	     }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	     else if(cardName.equals("Okina, Temple to the Grandfathers"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "G")
	   	  {
	   		private static final long serialVersionUID = 8154776336533992188L;

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
	   	          CardList att = new CardList();
	   	          att.addAll(c.getAttackers());
	   	          
	   	          for (int i=0; i<att.size(); i++)
	   	          {
	   	        	  Card crd = att.get(i);
	   	        	  if (crd.getType().contains("Legendary"))
	   	        		  return crd;
	   	          }
	   	          
	   	          return null;
	   		  }//getAttacker()
	   		  
	   		  public void resolve()
	   		  {
	   	  
	   			  final Card[] target = new Card[1];
	   	          final Command untilEOT = new Command()
	   	          {

	   				private static final long serialVersionUID = 6362813153010836856L;

	   				public void execute()
	   	            {
	   	              if(AllZone.GameAction.isCardInPlay(target[0]))
	   	              {
	   	                target[0].addTempAttackBoost(-1);
	   	                target[0].addTempDefenseBoost(-1);
	   	              }
	   	            }
	   	          
	   	          };
	   	          
	   	          target[0] = getTargetCard();
	   	          if(AllZone.GameAction.isCardInPlay(target[0]) && target[0].getType().contains("Legendary") && CardFactoryUtil.canTarget(card, target[0]))
	   	          {
	   	            target[0].addTempAttackBoost(1);
	   	            target[0].addTempDefenseBoost(1);
	   	
	   	            AllZone.EndOfTurn.addUntil(untilEOT);
	   	          }
	   		  }
	   	  };
	   	  
	   	  Input runtime = new Input()
	         {
	   		private static final long serialVersionUID = 6126636768830864856L;

	   		public void showMessage()
	           {
	             CardList choice = new CardList();
	             choice.addAll(AllZone.Human_Play.getCards());
	             choice.addAll(AllZone.Computer_Play.getCards());

	             choice = choice.getType("Creature");
	             choice = choice.filter(new CardListFilter()
	             {
	               public boolean addCard(Card c)
	               {
	                 return (c.getType().contains("Legendary"));
	               }
	             });
	             
	             //System.out.println("size of choice: " + choice.size());
	             stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice, "Select target legendary Creature:", true, false));
	           }
	         };
	   	  
	   	  ability.setDescription("G, tap: Target legendary creature gets +1/+1 until end of turn.");
	   	  ability.setStackDescription(card.getName() + " - gives target legendary creature +1/+1 until end of turn.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  ability.setBeforePayMana(runtime);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add G");
	   	  //card.setText(card.getSpellText() +  "\r\nG, tap: Target legendary creature gets +1/+1 until end of turn.");
	   	  //card.addIntrinsicKeyword("tap: add G");
	   	  
	     }//*************** END ************ END **************************
	       
	       
	     //*************** START *********** START **************************
	     else if(cardName.equals("Wirewood Lodge"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "G")
	   	  {
	   		  private static final long serialVersionUID = -4352872789672871590L;

	   		public boolean canPlayAI()
	   	      {
	   	          return false;
	   	      }
	   		  
	   		  public void resolve()
	   		  {
	   	  
	   			  final Card[] target = new Card[1];
	   	          
	   	          
	   	          target[0] = getTargetCard();
	   	          if(AllZone.GameAction.isCardInPlay(target[0]) && target[0].isTapped() && 
	   	        	(target[0].getType().contains("Elf") || target[0].getKeyword().contains("Changeling")) && CardFactoryUtil.canTarget(card, target[0]))
	   	          {
	   	        	  target[0].untap();
	   	          }
	   		  }
	   	  };
	   	  
	   	  Input runtime = new Input()
	         {
	           private static final long serialVersionUID = -6822924521729238991L;

	   		public void showMessage()
	           {
	             CardList choice = new CardList();
	             choice.addAll(AllZone.Human_Play.getCards());
	             choice.addAll(AllZone.Computer_Play.getCards());

	             choice = choice.getType("Elf");
	             choice = choice.filter(new CardListFilter()
	             {
	               public boolean addCard(Card c)
	               {
	                 return (c.isTapped());
	               }
	             });
	             
	             //System.out.println("size of choice: " + choice.size());
	             stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice, "Select target Elf", true, false));
	           }
	         };
	   	  
	   	  ability.setDescription("G, tap: Untap target Elf.");
	   	  ability.setStackDescription(card.getName() + " - untaps target elf.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  ability.setBeforePayMana(runtime);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add 1");
	   	  //card.setText(card.getSpellText() +  "\r\nG, tap: Untap target Elf.");
	   	  //card.addIntrinsicKeyword("tap: add 1");
	   	  
	     }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	     else if(cardName.equals("Academy Ruins"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "1 U")
	   	  {
	   		private static final long serialVersionUID = -1322368528417127121L;

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
	               CardList cards = new CardList(AllZone.Human_Graveyard.getCards());

	               CardList list = new CardList();
	               
	               for (int i=0;i < cards.size(); i++)
	               {
	             	  //System.out.println("type: " +cards.get(i).getType());
	             	  if (cards.get(i).getType().contains("Artifact")){
	             		  //System.out.println(cards.get(i).getName());
	             		  Card c = cards.get(i);
	             		  list.add(c);
	             		  
	             	  }
	               }
	               
	               if (list.size() != 0) {          
	     	          Object check = AllZone.Display.getChoiceOptional("Select Artifact", list.toArray());
	     	          if(check != null)
	     	          {
	     	            //PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	     	            //library.add((Card)check, 0);
	     	            AllZone.GameAction.moveToTopOfLibrary((Card)check);
	     	          }
	               }
	             }
	             public void computerResolve()
	             {
	               Card[] grave = AllZone.Computer_Graveyard.getCards();
	               CardList list = new CardList(grave);
	               CardList arts = new CardList();
	               
	               for (int i=0;i < list.size(); i++)
	               {
	             	  if (list.get(i).getType().contains("Artifact")){
	             		  Card k = list.get(i);
	             		  arts.add(k);
	             	  }
	               
	               }
	               
	               //pick best artifact
	               if (arts.size() != 0){
	     	          Card c = CardFactoryUtil.AI_getBestArtifact(list);
	     	          if(c == null)
	     	            c = grave[0];
	     	          System.out.println("computer picked - " +c);
	     	          AllZone.Computer_Graveyard.remove(c);
	     	          AllZone.Computer_Library.add(c, 0);
	               }
	             }//computerResolve
	             
	             public boolean canPlay(){
	           	  String controller = card.getController();
	           	  
	           	  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, controller);
	           	  CardList list = new CardList(grave.getCards());
	           	  CardList cards = new CardList();
	           	  
	           	  for (int i=0;i<list.size();i++)
	           	  {
	           		  if (list.get(i).getType().contains("Artifact") ){
	           			  cards.add(list.get(i));
	           		  }
	           	  }
	        
	           	  if (cards.size() > 0 && AllZone.GameAction.isCardInPlay(card) && card.isUntapped())
	           		  return true;
	           	  else
	           		  return false;
	             }
	   		
	   	  };
	   	  
	   	  ability.setDescription("1 U, tap: Put target artifact card in your graveyard on top of your library.");
	   	  ability.setStackDescription(card.getName() + " - put artifact card in your graveyard on top of your library.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add 1");
	   	  //card.setText(card.getSpellText() +  "\r\n1 U, tap: Put target artifact card in your graveyard on top of your library.");
	   	  //card.addExtrinsicKeyword("tap: add 1");
	   	  
	     }//*************** END ************ END **************************
	     
	     //*************** START *********** START **************************
	     else if(cardName.equals("Volrath's Stronghold"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "1 B")
	   	  {
	   		private static final long serialVersionUID = 2821525387844776907L;

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
	               CardList cards = new CardList(AllZone.Human_Graveyard.getCards());

	               CardList list = new CardList();
	               
	               for (int i=0;i < cards.size(); i++)
	               {
	             	  //System.out.println("type: " +cards.get(i).getType());
	             	  if (cards.get(i).getType().contains("Creature")){
	             		  //System.out.println(cards.get(i).getName());
	             		  Card c = cards.get(i);
	             		  list.add(c);
	             		  
	             	  }
	               }
	               
	               if (list.size() != 0) {          
	     	          Object check = AllZone.Display.getChoiceOptional("Select Creature", list.toArray());
	     	          if(check != null)
	     	          {
	     	            //PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
	     	            //library.add((Card)check, 0);
	     	            AllZone.GameAction.moveToTopOfLibrary((Card)check);
	     	          }
	               }
	             }
	             public void computerResolve()
	             {
	               Card[] grave = AllZone.Computer_Graveyard.getCards();
	               CardList list = new CardList(grave);
	               CardList creats = new CardList();
	               
	               for (int i=0;i < list.size(); i++)
	               {
	             	  if (list.get(i).getType().contains("Creature")){
	             		  Card k = list.get(i);
	             		  creats.add(k);
	             	  }
	               
	               }
	               
	               //pick best artifact
	               if (creats.size() != 0){
	     	          Card c = CardFactoryUtil.AI_getBestCreature(list);
	     	          if(c == null)
	     	            c = grave[0];
	     	          //System.out.println("computer picked - " +c);
	     	          AllZone.Computer_Graveyard.remove(c);
	     	          //AllZone.Computer_Library.add(c, 0);
	     	          AllZone.GameAction.moveToTopOfLibrary(c);
	               }
	             }//computerResolve
	             
	             public boolean canPlay(){
	           	  String controller = card.getController();
	           	  
	           	  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, controller);
	           	  CardList list = new CardList(grave.getCards());
	           	  CardList cards = new CardList();
	           	  
	           	  for (int i=0;i<list.size();i++)
	           	  {
	           		  if (list.get(i).getType().contains("Creature") ){
	           			  cards.add(list.get(i));
	           		  }
	           	  }
	        
	           	  if (super.canPlay() && cards.size() > 0 && AllZone.GameAction.isCardInPlay(card) && card.isUntapped())
	           		  return true;
	           	  else
	           		  return false;
	             }
	   		
	   	  };
	   	  
	   	  ability.setDescription("1 B, tap: Put target creature card in your graveyard on top of your library.");
	   	  ability.setStackDescription(card.getName() + " - put creature card in your graveyard on top of your library.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add 1");
	   	  //card.setText(card.getSpellText() +  "\r\n1 B, tap: Put target creature card in your graveyard on top of your library.");
	   	  //card.addExtrinsicKeyword("tap: add 1");
	   	  
	     }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	     else if(cardName.equals("Oboro, Palace in the Clouds"))
	     {
	   	  final Ability ability = new Ability(card, "1")
	   	  {

	   		public boolean canPlayAI()
	   		{
	   			return false;
	   		}
	   		  
	   		public void resolve()
	   		{
	   			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	   			AllZone.GameAction.moveTo(hand, card);
	   		}
	   	  };
	   	  
	   	  ability.setDescription("1: Return Oboro, Palace in the Clouds to your hand.");
	   	  ability.setStackDescription("Return " + card.getName() + " to your hand.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add 1");
	   	  //card.setText(card.getSpellText() +  "\r\n1: Return Oboro, Palace in the Clouds to your hand.");
	   	  //card.addExtrinsicKeyword("tap: add 1");
	   	  
	     }//*************** END ************ END **************************
	      
	     //*************** START *********** START **************************
	     else if(cardName.equals("Mikokoro, Center of the Sea"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "2")
	   	  {
	   		private static final long serialVersionUID = -199960897120235012L;

	   		public void resolve()
	   		  {
	   			  AllZone.GameAction.drawCard(Constant.Player.Computer);
	   			  AllZone.GameAction.drawCard(Constant.Player.Human);
	   		  }
	   	  };
	   	  
	   	  ability.setDescription("2, tap: Each player draws a card.");
	   	  ability.setStackDescription(card.getName() + " - Each player draws a card.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add 1");
	   	  //card.setText(card.getSpellText() +  "\r\n2, tap: Each player draws a card.");
	   	  //card.addExtrinsicKeyword("tap: add 1");
	   	  
	     }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************  
	     else if(cardName.equals("Gargoyle Castle"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "5")
	   	  {

	   		private static final long serialVersionUID = 8524185208900629992L;

	   		public boolean canPlay()
	   		  {
	   			  if (AllZone.GameAction.isCardInPlay(card) && card.isUntapped())
	   				  return true;
	   			  else
	   				  return false;
	   		  }
	   		  
	   		  public void resolve()
	   		  {
	   			  String player = card.getController();
	   			  AllZone.GameAction.sacrifice(card);
	   			  
	   			  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

	   	          //make token
	   	          Card c = new Card();

	   	          c.setOwner(card.getController());
	   	          c.setController(card.getController());

	   	          c.setName("Gargoyle");
	   	          c.setImageName("C 3 4 Gargoyle");
	   	          c.setManaCost("1");
	   	          c.setToken(true);

	   	          c.addType("Artifact");
	   	          c.addType("Creature");
	   	          c.addType("Gargoyle");
	   	          c.setBaseAttack(3);
	   	          c.setBaseDefense(4);
	   	          c.addIntrinsicKeyword("Flying");

	   	          play.add(c);
	   		  }
	   	  };
	   	  
	   	  ability.setDescription("5, tap, sacrifice Gargoyle Castle: Put a 3/4 colorless Gargoyle artifact creature token with flying onto the battlefield.");
	   	  ability.setStackDescription(card.getName() + " - Put a 3/4 colorless Gargoyle artifact creature token with flying onto the battlefield.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add 1");
	   	  //card.setText(card.getSpellText() +  "\r\n5, tap, sacrifice Gargoyle Castle: Put a 3/4 colorless Gargoyle artifact creature token with flying onto the battlefield.");
	   	  //card.addIntrinsicKeyword("tap: add 1");
	   	  
	     }//*************** END ************ END **************************
	     
	     //*************** START *********** START **************************  
	     else if(cardName.equals("Kher Keep"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "1 R")
	   	  {
	   		private static final long serialVersionUID = 4037838521451709399L;

	   		public boolean canPlay()
	   		  {
	   			  if (AllZone.GameAction.isCardInPlay(card) && card.isUntapped())
	   				  return true;
	   			  else
	   				  return false;
	   		  }
	   		  
	   		  public void resolve()
	   		  {
	   			  String player = card.getController();
	   			  
	   			  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

	   	          //make token
	   	          Card c = new Card();

	   	          c.setOwner(card.getController());
	   	          c.setController(card.getController());

	   	          c.setName("Kobolds of Kher Keep");
	   	          c.setImageName("R 0 1 Kobolds of Kher Keep");
	   	          c.setManaCost("R");
	   	          c.setToken(true);

	   	          c.addType("Creature");
	   	          c.addType("Kobold");
	   	          c.setBaseAttack(0);
	   	          c.setBaseDefense(1);

	   	          play.add(c);
	   		  }
	   	  };
	   	  
	   	  ability.setDescription("1 R, tap: Put a 0/1 red Kobold creature token named Kobolds of Kher Keep into play.");
	   	  ability.setStackDescription(card.getName() + " - Put a 0/1 red Kobold creature token named Kobolds of Kher Keep into play.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add 1");
	   	  //card.setText(card.getSpellText() +  "\r\n1 R, tap: Put a 0/1 red Kobold creature token named Kobolds of Kher Keep into play.");
	   	  //card.addIntrinsicKeyword("tap: add 1");
	   	  
	     }//*************** END ************ END **************************
	       
	       //*************** START *********** START **************************
	     else if(cardName.equals("Vitu-Ghazi, the City-Tree"))
	     {
	   	  final Ability_Tap ability = new Ability_Tap(card, "2 G W")
	   	  {
	   		private static final long serialVersionUID = 1781653158406511188L;

	   		public boolean canPlay()
	   		  {
	   			  if (AllZone.GameAction.isCardInPlay(card))
	   				  return true;
	   			  else
	   				  return false;
	   		  }
	   		  
	   		  public void resolve()
	   		  {
	   			  String player = card.getController();
	   			  
	   			  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

	   	          //make token
	   	          Card c = new Card();

	   	          c.setOwner(card.getController());
	   	          c.setController(card.getController());

	   	          c.setName("Saproling");
	   	          c.setImageName("G 1 1 Saproling");
	   	          c.setManaCost("G");
	   	          c.setToken(true);

	   	          c.addType("Creature");
	   	          c.addType("Saproling");
	   	          c.setBaseAttack(1);
	   	          c.setBaseDefense(1);

	   	          play.add(c);
	   		  }
	   	  };
	   	  
	   	  ability.setDescription("2 G W, tap: Put a 1/1 green Saproling creature token into play.");
	   	  ability.setStackDescription(card.getName() + " - Put a 1/1 green Saproling creature token named into play.");
	   	  
	   	  card.addSpellAbility(ability);
	   	  
	   	  //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	  //anyway, this does the trick:
	   	  //card.removeIntrinsicKeyword("tap: add 1");
	   	  //card.setText(card.getSpellText() +  "\r\n 2 G W, tap: Put a 1/1 green Saproling creature token into play.");
	   	  //card.addIntrinsicKeyword("tap: add 1");
	   	  
	     }//*************** END ************ END **************************
	       
	     //*************** START *********** START **************************
	     else if(cardName.equals("Gods' Eye, Gate to the Reikai"))
	     {   
	    	 
	    	 final Ability ability = new Ability(card, "0")
	         {
	           public void resolve()
	           {
	        	    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	        	    
	        	   Card c = new Card();
	              
	              c.setOwner(card.getController());
	              c.setController(card.getController());

	              c.setManaCost("1");
	              c.setToken(true);
	             
	              c.setName("Spirit");
	              c.setImageName("C 1 1 Spirit");
	              
	              c.addType("Creature");
	              c.addType("Spirit");
	              c.setBaseAttack(1);
	              c.setBaseDefense(1);
	              
	              play.add(c);
	    	      
	           }//resolve()
	         };//Ability
	    	 
	    	 Command makeToken = new Command()
	         {
	   		private static final long serialVersionUID = 2339209292936869322L;

	            public void execute()
	            {
	            	ability.setStackDescription(card.getName()+ " - put a 1/1 Spirit creature token into play");
	               AllZone.Stack.add(ability);
	            }
	         };

	         card.addDestroyCommand(makeToken);
	      }//*************** END ************ END **************************
	       
	    //*************** START *********** START **************************
	    else if(cardName.equals("Flagstones of Trokair"))
	    {   
	   	 
	   	 final Ability ability = new Ability(card, "0")
	        {
	          public void resolve()
	          {
	       	    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	   	       	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
	   	       	
	   	       	CardList plains = new CardList(lib.getCards());
	   	       	plains = plains.getType("Plains");
	   	       	
	   	       	if (card.getController().equals(Constant.Player.Computer))
	   	       	{
	   	       		if (plains.size() > 0)
	   	       		{
	   	       			Card c = plains.get(0);
	   	       			lib.remove(c);
	   	       			play.add(c);
	   	       			c.tap();
	   	       			
	   	       		}
	   	       	}
	   	       	else // human
	   	       	{
	   	       		if (plains.size() > 0) 
	   	       		{
	   	       			Object o = AllZone.Display.getChoiceOptional("Select plains card to put into play tapped: ", plains.toArray());
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
	   	 
	   	 Command fetchPlains = new Command()
	        {

	           private static final long serialVersionUID = 5991465998493672076L;

	   		public void execute()
	           {
	           	ability.setStackDescription(card.getName()+ " - search library for a plains card and put it into play tapped.");
	               AllZone.Stack.add(ability);
	           }
	        };

	        card.addDestroyCommand(fetchPlains);
	     }//*************** END ************ END **************************
	       
	       //*************** START *********** START **************************
	       else if(cardName.equals("Mutavault"))
	       {
	         final Command eot1 = new Command()
	         {
	         private static final long serialVersionUID = 5106629534549783845L;

	         public void execute()
	           {
	             Card c = card;

	             c.setBaseAttack(0);
	             c.setBaseDefense(0);
	             c.removeIntrinsicKeyword("Changeling");
	             c.removeType("Creature");
	           }
	         };

	         final SpellAbility a1 = new Ability(card, "1")
	         {
	           public boolean canPlayAI()
	           {
	             return false;
	           }
	           public void resolve()
	           {
	             Card c = card;

	             c.setBaseAttack(2);
	             c.setBaseDefense(2);

	             //to prevent like duplication like "Changeling Changeling Creature Creature"
	             if(! c.getIntrinsicKeyword().contains("Changeling"))
	             {
	               c.addIntrinsicKeyword("Changeling");
	               c.addType("Creature");
	             }
	             AllZone.EndOfTurn.addUntil(eot1);
	           }
	         };//SpellAbility

	         card.clearSpellKeepManaAbility();
	         card.addSpellAbility(a1);
	         a1.setDescription("1: Mutavault becomes a 2/2 creature with all creature types until end of turn. It's still a land.");
	         a1.setStackDescription(card +" becomes a 2/2 creature with changeling until EOT");

	         Command paid1 = new Command() {
	         private static final long serialVersionUID = -601119544294387668L;
	         public void execute() {AllZone.Stack.add(a1);}
	        };

	         a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
	         
	         //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
	   	     //anyway, this does the trick:
	   	     //card.removeIntrinsicKeyword("tap: add 1");
	   	     //card.setText(card.getText() +  "\r\n1: Mutavault becomes a 2/2 creature with all creature types until end of turn. It's still a land.");
	   	     //card.addIntrinsicKeyword("tap: add 1");

	       }//*************** END ************ END **************************
	       
	    
	     //*************** START *********** START **************************
	          else if(cardName.equals("Spawning Pool"))
	          {

	             final Command untilEOT = new Command()
	             {
	             private static final long serialVersionUID = -451839437837081897L;

	             public void execute()
	               {
	                 card.setShield(0);
	               }
	             };

	             final SpellAbility a2 = new Ability(card, "B")
	             {
	               public boolean canPlayAI() {return false;}

	               public void resolve()
	               {
	                 card.addShield();
	                 AllZone.EndOfTurn.addUntil(untilEOT);
	               }
	             };//SpellAbility
	                         a2.setDescription("B: Regenerate Spawning Pool.");
	             a2.setStackDescription("Regenerate Spawning Pool");

	             a2.setBeforePayMana(new Input_PayManaCost(a2));

	            final Command eot1 = new Command()
	            {
	            private static final long serialVersionUID = -8535770979347971863L;

	            public void execute()
	              {
	                Card c = card;

	                c.setBaseAttack(0);
	                c.setBaseDefense(0);
	                c.removeType("Creature");
	                c.removeType("Skeleton");
	                c.setManaCost("");
	                c.removeSpellAbility(a2);
	                
	              }
	            };

	            final SpellAbility a1 = new Ability(card, "1 B")
	            {
	              public boolean canPlayAI()
	              {
	                return ! card.getType().contains("Creature");
	              }
	              public void resolve()
	              {
	                Card c = card;

	                c.setBaseAttack(1);
	                c.setBaseDefense(1);
	                c.setManaCost("B");

	                //to prevent like duplication like "Creature Creature"
	                  boolean hasRegen = false;
	                  SpellAbility[] sas = card.getSpellAbility();
	                  for (SpellAbility sa : sas)
	                  {
	                     if(sa.toString().equals("B: Regenerate Spawning Pool.")) //this is essentially ".getDescription()"
	                        hasRegen = true;
	                  }
	                  if (!hasRegen){  card.addSpellAbility(a2);
	                                }
	                  if(!c.getType().contains("Creature"))
	                    c.addType("Creature");
	                  if(!c.getType().contains("Skeleton"))
	                    c.addType("Skeleton");
	                AllZone.EndOfTurn.addUntil(eot1);
	              }
	            };//SpellAbility

	            
	            card.clearSpellKeepManaAbility();
	            card.addSpellAbility(a1);
	            a1.setStackDescription(card +" becomes a 1/1 skeleton creature with B: regenerate this creature until EOT");
	            a1.setDescription("1B: Spawning Pool becomes a 1/1 skeleton creature with B: regenerate this creature until end of the turn.  It's still a land.");
	            Command paid1 = new Command() {
	            	private static final long serialVersionUID = -6800983290478844750L;
	            
	            	public void execute() {AllZone.Stack.add(a1);}
	           };
	            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
	          }//*************** END ************ END **************************
	       
       		//*************** START *********** START **************************
             else if(cardName.equals("Shizo, Death's Storehouse"))
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
                       c.removeIntrinsicKeyword("Fear");
                                            }
                   }
                 };

               a2[0] = new Ability_Tap(card, "B")
               {
               private static final long serialVersionUID = 3561450520225198222L;

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
                       if(!c.getIntrinsicKeyword().contains("Fear"))
                             c.addIntrinsicKeyword("Fear");
                    
                     AllZone.EndOfTurn.addUntil(eot2);
                   }
                 }//resolve()
               };//SpellAbility
               card.addSpellAbility(a2[0]);
               a2[0].setDescription("B, tap: Target legendary creature gains fear until end of turn.");


               @SuppressWarnings("unused") // target unused
            final Input target = new Input()
               {
                private static final long serialVersionUID = 8913477363141356082L;
               
                public void showMessage()
                 {
                   ButtonUtil.enableOnlyCancel();
                   AllZone.Display.showMessage("Select egendary creature to get fear");
                 }
                 public void selectCard(Card c, PlayerZone zone)
                 {
                  if(!CardFactoryUtil.canTarget(card, c)){
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                  }
                  else if(c.isCreature() && c.getType().contains("Legendary"))
                  {
                     card.tap();
                     AllZone.Human_Play.updateObservers();

                     a2[0].setTargetCard(c);//since setTargetCard() changes stack description
                     a2[0].setStackDescription(c +" gets fear until EOT");

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
               a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Legendary"));

             }//*************** END ************ END **************************
	
     //*************** START *********** START **************************
         if(cardName.equals("Novijen, Heart of Progress"))
         {
            card.clearSpellKeepManaAbility();
            
            final CardListFilter targets = new CardListFilter()
            {

               public boolean addCard(Card c) {
                  return AllZone.GameAction.isCardInPlay(c) && c.isCreature()
                     && c.getTurnInZone() == AllZone.Phase.getTurn();     
               }
            };
            Ability_Tap ability = new Ability_Tap(card,"G U")
            {
               private static final long serialVersionUID = 1416258136308898492L;

               CardList inPlay = new CardList();
               public boolean canPlayAI()
               {
                  if(!(AllZone.Phase.getPhase().equals(Constant.Phase.Main1)
                    && AllZone.Phase.getActivePlayer().equals(Constant.Player.Computer)))
                     return false;
                  inPlay.clear();
                  inPlay.addAll(AllZone.Computer_Play.getCards());
                  return (inPlay.filter(targets).size() > 1);
               }
               public void resolve() {
                  inPlay.clear();
                  inPlay.addAll(AllZone.Human_Play.getCards());
                  inPlay.addAll(AllZone.Computer_Play.getCards());
                  for(Card targ : inPlay.filter(targets))
                     targ.addCounter(Counters.P1P1, 1);
               }
            };
            ability.setDescription("G U, tap: Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            ability.setStackDescription("Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            card.addSpellAbility(ability);
         }
         //*************** END ************ END **************************
         
       //*************** START *********** START **************************
         else if(cardName.equals("Urza's Factory"))
         {
            final Ability_Tap ability = new Ability_Tap(card, "7")
            {
             private static final long serialVersionUID = 1781653158406511188L;

             public boolean canPlay()
               {
                  if (AllZone.GameAction.isCardInPlay(card))
                     return true;
                  else
                     return false;
               }
              
               public void resolve()
               {
                  String player = card.getController();
                 
                  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

                    //make token
                    Card c = new Card();

                    c.setOwner(card.getController());
                    c.setController(card.getController());

                    c.setName("Assembly-Worker");
                    c.setImageName("c 2 2 Assembly-Worker");
                    c.setManaCost("");
                    c.setToken(true);
                   
                    c.addType("Artifact");
                    c.addType("Creature");
                    c.addType("Assembly-Worker");
                    c.setBaseAttack(2);
                    c.setBaseDefense(2);

                    play.add(c);
               }
            };
           
            ability.setDescription("7, tap: Put a 2/2 colorless Assembly-Worker artifact creature token onto the battlefield.");
            ability.setStackDescription(card.getName() + " - Put a 2/2 colorless Assembly-Worker artifact creature token onto the battlefield.");
           
            card.addSpellAbility(ability);
           
            //not sure what's going on here, maybe because it's a land it doesn't add the ability to the text?
            //anyway, this does the trick:
            //card.removeIntrinsicKeyword("tap: add 1");
            //card.setText(card.getSpellText() +  "\r\n  Put a 2/2 colorless Assembly-Worker artifact creature token onto the battlefield.");
            //card.addIntrinsicKeyword("tap: add 1");
           
         }//*************** END ************ END **************************
         
       //*************** START *********** START **************************
         else if(cardName.equals("Goblin Burrows"))
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
                   c.addTempAttackBoost(-2);
                                        }
               }
             };

           a2[0] = new Ability_Tap(card, "1 R")
           {
           private static final long serialVersionUID = 3561450520225198222L;

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
                 c.addTempAttackBoost(2);
                
                 AllZone.EndOfTurn.addUntil(eot2);
               }
             }//resolve()
           };//SpellAbility
           card.addSpellAbility(a2[0]);
           a2[0].setDescription("1 R, tap: Target Goblin gets +2/+0 until end of turn.");


           @SuppressWarnings("unused") // target unused
        final Input target = new Input()
           {
            private static final long serialVersionUID = 8913477363141356082L;
           
            public void showMessage()
             {
               ButtonUtil.enableOnlyCancel();
               AllZone.Display.showMessage("Select Goblin to get +2/+0");
             }
             public void selectCard(Card c, PlayerZone zone)
             {
              if(!CardFactoryUtil.canTarget(card, c)){
                    AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
              }
              else if(c.isCreature() && c.getType().contains("Goblin"))
              {
                 card.tap();
                 AllZone.Human_Play.updateObservers();

                 a2[0].setTargetCard(c);//since setTargetCard() changes stack description
                 a2[0].setStackDescription(c +" gets +2/+0 until EOT");

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
           a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Goblin"));

         }//*************** END ************ END **************************
         
        /*
       //*************** START *********** START **************************
         else if(cardName.equals("Skarrg, the Rage Pits"))
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
                   c.removeIntrinsicKeyword("Trample");
                                        }
               }
             };

           a2[0] = new Ability_Tap(card, "G R")
           {
           private static final long serialVersionUID = 3561450520225198222L;

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
                 c.addIntrinsicKeyword("Trample");
                 AllZone.EndOfTurn.addUntil(eot2);
               }
             }//resolve()
           };//SpellAbility
           card.addSpellAbility(a2[0]);
           a2[0].setDescription("G R, tap: Target creature gets +1/+1 and gains trample until end of turn.");


           @SuppressWarnings("unused") // target unused
           final Input target = new Input()
           {
            private static final long serialVersionUID = 8913477363141356082L;
           
            public void showMessage()
             {
               ButtonUtil.enableOnlyCancel();
               AllZone.Display.showMessage("Select Creature to get +1/+1 and trample");
             }
             public void selectCard(Card c, PlayerZone zone)
             {
              if(!CardFactoryUtil.canTarget(card, c)){
                    AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
              }
              else if(c.isCreature())
              {
                 card.tap();
                 AllZone.Human_Play.updateObservers();

                 a2[0].setTargetCard(c);//since setTargetCard() changes stack description
                 a2[0].setStackDescription(c +" gets +1/+1 and trample until EOT");

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
           a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Creature"));

         }//*************** END ************ END **************************
         
         */
         
       //*************** START *********** START **************************
         else if(cardName.equals("Daru Encampment"))
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

           a2[0] = new Ability_Tap(card, "W")
           {
           private static final long serialVersionUID = 3561450520225198222L;

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
           a2[0].setDescription("W, tap: Target Soldier gets +1/+1 until end of turn.");


           @SuppressWarnings("unused") // target unused
        final Input target = new Input()
           {
            private static final long serialVersionUID = 8913477363141356082L;
           
            public void showMessage()
             {
               ButtonUtil.enableOnlyCancel();
               AllZone.Display.showMessage("Select Soldier to get +1/+1");
             }
             public void selectCard(Card c, PlayerZone zone)
             {
              if(!CardFactoryUtil.canTarget(card, c)){
                    AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
              }
              else if(c.isCreature() && c.getType().contains("Soldier"))
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
           a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Soldier"));

         }//*************** END ************ END **************************
         
       //*************** START *********** START **************************
         if(cardName.equals("Duskmantle, House of Shadow"))
         {
            card.clearSpellKeepManaAbility();
            
            Ability_Tap ability = new Ability_Tap(card,"U B")
            {
               private static final long serialVersionUID = 42470566751344693L;

                 public boolean canPlayAI()
                 {
                     String player = getTargetPlayer();
                     PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                     CardList libList = new CardList(lib.getCards());
                     return libList.size() > 0;
                 }

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
            };
            ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
            ability.setDescription("tap U B: Target player puts the top card of his or her library into his or her graveyard.");
            ability.setStackDescription("Target player puts the top card of his or her library into his or her graveyard.");
            card.addSpellAbility(ability);
         }
         //*************** END ************ END **************************
 
	     //*************** START *********** START **************************
	       if (cardName.equals("Crypt of Agadeem"))
	       {
	          final SpellAbility ability = new Ability_Tap(card, "2")
	          {
				private static final long serialVersionUID = -3561865824450791583L;
				public void resolve()
	             {
	                /*CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
	                list = list.getName("Mana Pool");*/
	                Card mp = AllZone.ManaPool;//list.getCard(0);
	                
	                PlayerZone Grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
	                CardList evildead = new CardList();
	                evildead.addAll(Grave.getCards());
	                evildead = evildead.filter(new CardListFilter(){
	                    public boolean addCard(Card c) {
	                       return (c.isCreature() && CardUtil.getColors(c).contains(Constant.Color.Black));
	                    }
	                 });
	                
	                for(int i=0;i<evildead.size();i++)
	                {
	               	 mp.addExtrinsicKeyword("ManaPool:B");
	                }
	             }
	             public boolean canPlayAI()
	             {
	                return false;
	             }
	          };
	          
	          ability.setDescription("2, tap: Add B to your mana pool for each for each black creature card in your graveyard.");
	          ability.setStackDescription(cardName + " adds B to your mana pool for each black creature card in your graveyard.");
	          //card.clearSpellAbility();
	          //card.setText(card.getText() +  ability.toString());
	          card.addSpellAbility(ability);

	          return card;
	       }//*************** END ************ END **************************

			
		      //*************** START *********** START **************************
	        if(cardName.equals("Rix Maadi, Dungeon Palace"))
	        {
	           card.clearSpellKeepManaAbility();
	           
	           Ability_Tap ability = new Ability_Tap(card,"1 B R")
	           {
	              private static final long serialVersionUID = 42470566751344693L;
	              
	              public boolean canPlay()
		            {
		            	 if (((AllZone.Phase.getPhase().equals(Constant.Phase.Main2)&& AllZone.Phase.getActivePlayer() == card.getController()) || (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer() == card.getController())) && AllZone.GameAction.isCardInPlay(card) )
		            	        return true;
		            	     else  
		            	          return false;
		            }

	                public boolean canPlayAI()
	                {
	                	PlayerZone hand_c = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
	                	PlayerZone hand_h = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);                	
	                    CardList hand_comp = new CardList(hand_c.getCards());
	                    CardList hand_hum = new CardList(hand_h.getCards());
	                    return ((hand_comp.size()-hand_hum.size())>1 && hand_hum.size()>0);
	                }

	                public void resolve()
	                {
	                	AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
	                	AllZone.GameAction.discardRandom(Constant.Player.Computer);  // wise discard should be here  
	                }
	           };          
	           ability.setDescription("tap 1 B R: Each player discards a card. Activate this ability only any time you could cast a sorcery.");
	           ability.setStackDescription("Each player discards a card.");
	           card.addSpellAbility(ability);
	        }
	        //*************** END ************ END **************************        
	        
	 
	      //*************** START *********** START **************************
	        if(cardName.equals("Orzhova, the Church of Deals"))
	        {
	           card.clearSpellKeepManaAbility();
	           
	           Ability_Tap ability = new Ability_Tap(card,"3 W B")
	           {
	              private static final long serialVersionUID = 42470566751344693L;
	                            	   		   
	               
	                public void resolve()
	                {                	                         
	                     AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(1);
	                     PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	                     life.addLife(1);
	              }
	           };
	           ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());           
	           ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
	           ability.setDescription("tap 3 W B: Target player loses 1 life and you gain 1 life.");
	           ability.setStackDescription("Target player loses 1 life and you gain 1 life.");
	           card.addSpellAbility(ability);
	        }
	        //*************** END ************ END **************************
	 
	        //*************** START *********** START **************************
	        else if(cardName.equals("Svogthos, the Restless Tomb"))
	        {
	          final Command eot1 = new Command()
	          {
	          private static final long serialVersionUID = -8535770979347971863L;
	         
	          public void execute()
	            {
	              Card c = card;	              
	              c.removeType("Creature");
	              c.removeType("Zombie");
	              c.removeType("Plant");              
	              c.setManaCost("");
	              c.setBaseAttack(0);
	              c.setBaseDefense(0);
	              
	            }
	          };

	          final SpellAbility a1 = new Ability(card, "3 B G")
	          {            
	            	 public boolean canPlayAI()
	                 {
	               	  PlayerZone compGrave = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer);         
	                     CardList list = new CardList();
	                     list.addAll(compGrave.getCards());       
	                     list = list.filter(new CardListFilter(){
	                        public boolean addCard(Card c) {
	                           return c.isCreature();
	                        }
	                     });
	                     return ((list.size()>0)&!card.getType().contains("Creature"));
	                 }
	            	            	            
	            public void resolve()
	            {
	              Card c = card;

	              c.setBaseAttack(1);
	              c.setBaseDefense(1);
	              c.setManaCost("B G");

	              //to prevent like duplication like "Creature Creature"              
	                if(!c.getType().contains("Creature"))
	                  c.addType("Creature");
	                if(!c.getType().contains("Zombie"))
	                  c.addType("Zombie");
	                if(!c.getType().contains("Plant"))
	                    c.addType("Plant");
	              AllZone.EndOfTurn.addUntil(eot1);
	            }
	          };//SpellAbility

	          card.clearSpellKeepManaAbility();
	          card.addSpellAbility(a1);
	          a1.setStackDescription(card +" becomes a black and green Plant Zombie creature with power and toughness each equal to the number of creature cards in your graveyard until EOT");
	          a1.setDescription("3 B G: Until end of turn, Svogthos, the Restless Tomb becomes a black and green Plant Zombie creature with This creature's power and toughness are each equal to the number of creature cards in your graveyard. It's still a land.");
	        }//*************** END ************ END **************************
	        
	        
	 
	        //*************** START *********** START **************************
	        else if(cardName.equals("Ghitu Encampment"))
	        {
	          final Command eot1 = new Command()
	          {
	          private static final long serialVersionUID = -8535770979347971863L;

	          public void execute()
	            {
	              Card c = card;

	              c.setBaseAttack(0);
	              c.setBaseDefense(0);
	              c.removeType("Creature");
	              c.removeType("Warrior");
	              c.removeIntrinsicKeyword("First Strike");
	              c.setManaCost("");
	              
	            }
	          };

	          final SpellAbility a1 = new Ability(card, "1 R")
	          {
	            public boolean canPlayAI()
	            {
	              return ! card.getType().contains("Creature");
	            }
	            public void resolve()
	            {
	              Card c = card;

	              c.setBaseAttack(2);
	              c.setBaseDefense(1);
	              c.setManaCost("R");

	              //to prevent like duplication like "Creature Creature"
	              if(!c.getIntrinsicKeyword().contains("First Strike"))
	                  c.addIntrinsicKeyword("First Strike");
	                if(!c.getType().contains("Creature"))
	                  c.addType("Creature");
	                if(!c.getType().contains("Warrior"))
	                  c.addType("Warrior");
	              AllZone.EndOfTurn.addUntil(eot1);
	            }
	          };//SpellAbility

	          card.clearSpellKeepManaAbility();
	          card.addSpellAbility(a1);
	          a1.setStackDescription(card +" becomes a 2/1 creature with first strike until EOT");
	          a1.setDescription("1 R: Ghitu Encampment becomes a 2/1 red Warrior creature with first strike until end of turn. It's still a land.");
	          Command paid1 = new Command() {
	          private static final long serialVersionUID = -6800983290478844750L;

	          public void execute() {AllZone.Stack.add(a1);}
	         };
	          a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
	        }//*************** END ************ END **************************
	        
	        //*************** START *********** START **************************
	        else if(cardName.equals("Stalking Stones"))
	        {
	        
	          final SpellAbility a1 = new Ability(card, "6")
	          {
	            public boolean canPlayAI()
	            {
	              return ! card.getType().contains("Creature");
	            }
	            public void resolve()
	            {
	              Card c = card;

	              c.setBaseAttack(3);
	              c.setBaseDefense(3);
	              c.setManaCost("");

	              //to prevent like duplication like "Creature Creature"
	              if(!c.getType().contains("Elemental"))
	                  c.addType("Elemental");
	                      if(!c.getType().contains("Artifact"))
	                  c.addType("Artifact");
	                 if(!c.getType().contains("Creature"))
	                  c.addType("Creature");

	              
	            }
	          };//SpellAbility

	          card.clearSpellKeepManaAbility();
	          card.addSpellAbility(a1);
	          a1.setStackDescription(card +" becomes a 3/3 Elemental artifact creature that's still a land.");
	               a1.setDescription("6: Stalking Stones becomes a 3/3 Elemental artifact creature that's still a land. (This effect lasts indefinitely.)");
	          Command paid1 = new Command() {
	          private static final long serialVersionUID = -6800983290478844750L;

	          public void execute() {AllZone.Stack.add(a1);}
	         };
	          a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
	        }//*************** END ************ END **************************
	        
	        //*************** START *********** START **************************
	        else if (cardName.equals("Khalni Garden"))
	        {
	        	final Ability ability = new Ability(card, "0")
	        	{
	        		public void resolve()
	        		{
	        			CardFactoryUtil.makeToken("Plant", "G 0 1 Plant", card, "G", new String[] {"Creature", "Plant"}, 0, 1, new String[] {""} );
	        		}
	        	};
	        	ability.setStackDescription("When Khalni Garden enters the battlefield, put a 0/1 green Plant creature token onto the battlefield.");
	        	
	        	final Command comesIntoPlay = new Command()
	        	{
					private static final long serialVersionUID = 6175835326425915833L;
					public void execute()
	        		{
	        			AllZone.Stack.add(ability);
	        		}
	        	};
	        	card.clearSpellKeepManaAbility();
	        	card.addComesIntoPlayCommand(comesIntoPlay);
	        }//*************** END ************ END **************************
    
	        
	        if (hasKeyword(card, "Cycling") != -1)
		       {
		         int n = hasKeyword(card, "Cycling");
		         if (n != -1)
		         {
		           String parse = card.getKeyword().get(n).toString();
		           card.removeIntrinsicKeyword(parse);

		           String k[] = parse.split(":");
		           final String manacost = k[1];

		           card.addSpellAbility(CardFactoryUtil.ability_cycle(card, manacost));
		         }
		       }//Cycling
		       
		       while (hasKeyword(card, "TypeCycling") != -1)
		       {
		         int n = hasKeyword(card, "TypeCycling");
		         if (n != -1)
		         {
		           String parse = card.getKeyword().get(n).toString();
		           card.removeIntrinsicKeyword(parse);

		           String k[] = parse.split(":");
		           final String type = k[1];
		           final String manacost = k[2];

		           card.addSpellAbility(CardFactoryUtil.ability_typecycle(card, manacost,type));
		         }
		       }//TypeCycling

		       if (hasKeyword(card, "Transmute") != -1)
		       {
		         int n = hasKeyword(card, "Transmute");
		         if (n != -1)
		         {
		           String parse = card.getKeyword().get(n).toString();
		           card.removeIntrinsicKeyword(parse);

		           String k[] = parse.split(":");
		           final String manacost = k[1];

		           card.addSpellAbility(CardFactoryUtil.ability_transmute(card, manacost));
		         }
		       }//transmute


		return card;
	}
	
	
	
}