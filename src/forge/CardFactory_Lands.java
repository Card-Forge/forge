
package forge;


import java.util.HashMap;

import javax.swing.JOptionPane;

class CardFactory_Lands {

    public static Card getCard(final Card card, final String cardName, Player owner) {
        
//	    computer plays 2 land of these type instead of just 1 per turn
        

        //*************** START *********** START **************************
        if(cardName.equals("Oran-Rief, the Vastwood")) {
            card.clearSpellKeepManaAbility();
            
            final CardListFilter targets = new CardListFilter() {
                
                public boolean addCard(Card c) {
                    return AllZone.GameAction.isCardInPlay(c) && c.isCreature()
                            && c.getTurnInZone() == AllZone.Phase.getTurn()
                            && c.isGreen();
                }
                
            };
            Ability_Cost abCost = new Ability_Cost("T", card.getName(), true);
            final SpellAbility ability = new Ability_Activated(card, abCost, null){                
                private static final long serialVersionUID = 1416258136308898492L;
                
                CardList                  inPlay           = new CardList();
                
                @Override
                public boolean canPlayAI() {
                    if(!(AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getPlayerTurn().equals(
                            AllZone.ComputerPlayer))) return false;
                    inPlay.clear();
                    inPlay.addAll(AllZone.Computer_Battlefield.getCards());
                    return (inPlay.filter(targets).size() > 1) && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    inPlay.clear();
                    inPlay.addAll(AllZone.Human_Battlefield.getCards());
                    inPlay.addAll(AllZone.Computer_Battlefield.getCards());
                    for(Card targ:inPlay.filter(targets))
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
        else if (  cardName.equals("Blood Crypt")    || cardName.equals("Breeding Pool") 
                || cardName.equals("Godless Shrine") || cardName.equals("Hallowed Fountain") 
                || cardName.equals("Overgrown Tomb") || cardName.equals("Sacred Foundry") 
                || cardName.equals("Steam Vents")    || cardName.equals("Stomping Ground") 
                || cardName.equals("Temple Garden")  || cardName.equals("Watery Grave")) {
            //if this isn't done, computer plays more than 1 copy
            //card.clearSpellAbility();
            card.clearSpellKeepManaAbility();
            
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 7352127748114888255L;
                
                public void execute() {
                    if (card.getController().equals(AllZone.HumanPlayer)) humanExecute();
                    else computerExecute();
                }
                
                public void computerExecute() {
                    boolean pay = false;
                    
                    if(AllZone.ComputerPlayer.getLife() > 9) pay = MyRandom.random.nextBoolean();
                    
                    if(pay) AllZone.ComputerPlayer.loseLife(2, card);
                    else card.tap();
                }
                
                public void humanExecute() {
                    int life = card.getController().getLife();
                    if (2 < life) {
                        
                        StringBuilder question = new StringBuilder();
                        question.append("Pay 2 life? If you don't, ").append(card.getName());
                        question.append(" enters the battlefield tapped.");
                        
                        if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                            AllZone.HumanPlayer.loseLife(2, card);
                        } else tapCard();
                        
                    }//if
                    else tapCard();
                }//execute()
                
                private void tapCard() {
                    card.tap();
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Kabira Crossroads")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().gainLife(2, card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -4550013855602477643L;
                
                public void execute() {
                    card.tap();
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(card.getController()).append(" gains 2 life");
                    ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Bojuka Bog")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
					if (card.getController().equals(AllZone.ComputerPlayer))
						setTargetPlayer(AllZone.HumanPlayer);
					
        			final Player player = getTargetPlayer();
        			CardList grave = AllZoneUtil.getPlayerGraveyard(player);
        			for(Card c:grave) {
        				AllZone.GameAction.exile(c);
        			}
                }
            };
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -4309535765473933378L;

				public void execute() {
                    card.tap();
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
                        ButtonUtil.disableAll();
                    } else if(card.getController().equals(AllZone.ComputerPlayer)) {
                        ability.setTargetPlayer(AllZone.HumanPlayer);
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - Exile target player's graveyard.");
                    ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sejiri Steppe")) {
            final HashMap<Card, String[]> creatureMap = new HashMap<Card, String[]>();
            final SpellAbility[] a = new SpellAbility[1];
            final Command eot1 = new Command() {
                private static final long serialVersionUID = 5106629534549783845L;
                
                public void execute() {
                	Card c = a[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        String[] colors = creatureMap.get(c);
                        for(String col:colors) {
                            c.removeExtrinsicKeyword("Protection from " + col);
                        }
                }
            };
            };
            a[0] = new Ability(card, "0") {
                @Override
                public void resolve() {
		    		String Color = "";

		        	if(card.getController() == AllZone.HumanPlayer){
	                    if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard())) {                     
	                        Object o = AllZone.Display.getChoice("Choose mana color", Constant.Color.ColorsOnly);
	                        Color = (String) o;
	                    }

                } else {
                    CardList creature = new CardList();
                    PlayerZone zone = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);             
                    if(zone != null) {
                    creature.addAll(zone.getCards());
                    creature = creature.getType("Creature"); 
                    creature = creature.filter(new CardListFilter()
                	{
                		public boolean addCard(Card c)
                		{
                			return (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(a[0], c) && !c.hasKeyword("Defender"));
                		}
                	});
                    Card biggest = null;
                           if(creature.size() > 0) {
                        	   biggest = creature.get(0);
                           
                            for(int i = 0; i < creature.size(); i++) {
                                if(biggest.getNetAttack() < creature.get(i).getNetAttack()) biggest = creature.get(i);   
                            }
                         		setTargetCard(biggest);
                    	
                    }
                    }
                    PlayerZone Hzone = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);  
                    if(zone != null) {
                        CardList creature2 = new CardList();
                        creature2.addAll(Hzone.getCards());
                        creature2 = creature2.getType("Creature"); 
                        creature2 = creature2.filter(new CardListFilter()
                    	{
                    		public boolean addCard(Card c)
                    		{
                    			return (!c.isTapped() && !CardUtil.getColors(c).contains(Constant.Color.Colorless));
                    		}
                    	});
                        Card biggest2 = null;
                        if(creature2.size() > 0) {
                                 biggest2 = creature2.get(0);
                                for(int i = 0; i < creature2.size(); i++) {
                                    if(biggest2.getNetAttack() < creature2.get(i).getNetAttack()) biggest2 = creature2.get(i);   
                                }
                             		if(biggest2 != null) {  
                             			if(biggest2.isGreen()) Color = "green";
                             			if(biggest2.isBlue()) Color = "blue";
                             			if(biggest2.isWhite()) Color = "white";
                             			if(biggest2.isRed()) Color = "red";
                             			if(biggest2.isBlack()) Color = "black";
                             		} else {
                             			Color = "black";          			
                             		}
                                		
                        } else {
                        	Color = "black"; 
                        }
                    }
                }
		        	Card Target = getTargetCard();
					if(Color != "" && Target != null) Target.addExtrinsicKeyword("Protection from " + Color);;
                    if(creatureMap.containsKey(Target)) {
                        int size = creatureMap.get(Target).length;
                        String[] newString = new String[size + 1];
                        
                        for(int i = 0; i < size; i++) {
                            newString[i] = creatureMap.get(Target)[i];
                        }
                        newString[size] = Color;
                        creatureMap.put(Target, newString);
                    } else creatureMap.put(Target, new String[] {Color});
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 5055232386220487221L;
                
                public void execute() {
                    CardList creats = new CardList(
                            AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
                    creats = creats.getType("Creature");
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - target creature you control gains protection from the color of your choice until end of turn");
                    a[0].setStackDescription(sb.toString());
		        	if(card.getController() == AllZone.HumanPlayer) {
		        		AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(a[0], creats, "Select target creature you control", false, false));
		        	} else {
	                    AllZone.Stack.add(a[0]);  		
		        	}
                    }
            };         
            card.addComesIntoPlayCommand(intoPlay);
        
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Gemstone Mine")) {
            final Ability_Mana mine = new Ability_Mana(card, "tap, Remove a mining counter from CARDNAME: Add one mana of any color to your mana pool. If there are no mining counters on CARDNAME, sacrifice it.") {
				private static final long serialVersionUID = -785117149012567841L;

                @Override
                public void undo() {
                	card.untap();
                    card.addCounter(Counters.MINING, 1);
                }
                
                //@Override
                public String mana() {
                    return this.choices_made[0].toString();
                }
                
                @Override
                public boolean canPlay() {
                    if(choices_made[0] == null) choices_made[0] = "1";
                    return super.canPlay() && card.getCounters(Counters.MINING) > 0;
                }
                
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.MINING, 1);
                    card.tap();
                    if (card.getCounters(Counters.MINING) == 0) AllZone.GameAction.sacrifice(card);
                    super.resolve();
                }
            };
            
            mine.choices_made = new String[1];
            mine.setBeforePayMana(new Input() {
                
                private static final long serialVersionUID = 376497609786542558L;
                
                @Override
                public void showMessage() {
                	if (card.isUntapped())
                	{
	                	mine.choices_made[0] = Input_PayManaCostUtil.getShortColorString(AllZone.Display.getChoiceOptional(
	                            "Select a Color", Constant.Color.onlyColors));
	                	if (mine.choices_made[0] != null){
	                		AllZone.Stack.add(mine);
	                	}
                	}
                    stop();
                }
            });

            card.setReflectableMana("WUBRG");
            card.addSpellAbility(mine);
            mine.setDescription("Gemstone Mine - tap, remove a mining counter: Add one mana of any color to your mana pool. If there are no mining counters on CARDNAME, sacrifice it.");
            mine.setStackDescription("Gemstone Mine - tap, remove a mining counter: Add one mana of any color to your mana pool. If there are no mining counters on CARDNAME, sacrifice it.");
        	
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -2231880032957304542L;

				public void execute() {
					card.addCounter(Counters.MINING, 3);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Vivid Crag") || cardName.equals("Vivid Creek") || cardName.equals("Vivid Grove")
        		|| cardName.equals("Vivid Marsh") || cardName.equals("Vivid Meadow")){
            final Ability_Mana vivid = new Ability_Mana(card, "tap, Remove a charge counter from CARDNAME: Add one mana of any color to your mana pool.") {
				private static final long serialVersionUID = -785117149012567841L;

                @Override
                public void undo() {
                	card.untap();
                    card.addCounter(Counters.CHARGE, 1);
                }
                
                //@Override
                public String mana() {
                    return this.choices_made[0].toString();
                }
                
                @Override
                public boolean canPlay() {
                    if(choices_made[0] == null) choices_made[0] = "1";
                    return super.canPlay() && card.getCounters(Counters.CHARGE) > 0;
                }
                
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.CHARGE, 1);
                    card.tap();
                    super.resolve();
                }
            };
            
            vivid.choices_made = new String[1];
            vivid.setBeforePayMana(new Input() {
                
                private static final long serialVersionUID = 376497609786542558L;
                
                @Override
                public void showMessage() {
                	vivid.choices_made[0] = Input_PayManaCostUtil.getShortColorString(AllZone.Display.getChoiceOptional(
                            "Select a Color", Constant.Color.onlyColors));
                	if (vivid.choices_made[0] != null){
                		AllZone.Stack.add(vivid);
                	}
                    stop();
                }
            });
            
            card.setReflectableMana("WUBRG");
            card.addSpellAbility(vivid);
            vivid.setDescription("CARDNAME - tap, remove a charge counter: Add one mana of any color to your mana pool");
            vivid.setStackDescription("CARDNAME - tap, remove a charge counter: Add one mana of any color to your mana pool");
        	
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -2231880032957304542L;

				public void execute() {
					card.addCounter(Counters.CHARGE, 2);
					card.tap();
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Tendo Ice Bridge")){
            final Ability_Mana tendo = new Ability_Mana(card, "tap, Remove a charge counter from CARDNAME: Add one mana of any color to your mana pool.") {
				private static final long serialVersionUID = -785117149012567841L;

                @Override
                public void undo() {
                	card.untap();
                    card.addCounter(Counters.CHARGE, 1);
                }
                
                //@Override
                public String mana() {
                    return this.choices_made[0].toString();
                }
                
                @Override
                public boolean canPlay() {
                    if(choices_made[0] == null) choices_made[0] = "1";
                    return super.canPlay() && card.getCounters(Counters.CHARGE) > 0;
                }
                
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.CHARGE, 1);
                    card.tap();
                    super.resolve();
                }
            };
            
            tendo.choices_made = new String[1];
            tendo.setBeforePayMana(new Input() {
                
                private static final long serialVersionUID = 376497609786542558L;
                
                @Override
                public void showMessage() {
                	tendo.choices_made[0] = Input_PayManaCostUtil.getShortColorString(AllZone.Display.getChoiceOptional(
                            "Select a Color", Constant.Color.onlyColors));
                	if (tendo.choices_made[0] != null){
                		AllZone.Stack.add(tendo);
                	}
                    stop();
                }
            });
            
            card.setReflectableMana("WUBRG");
            card.addSpellAbility(tendo);
            tendo.setDescription("tap, remove a charge counter: Add one mana of any color to your mana pool");
            tendo.setStackDescription("CARDNAME - tap, remove a charge counter: Add one mana of any color to your mana pool");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Graypelt Refuge") || cardName.equals("Sejiri Refuge")
                || cardName.equals("Jwar Isle Refuge") || cardName.equals("Akoum Refuge")
                || cardName.equals("Kazandu Refuge")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().gainLife(1, card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 5055232386220487221L;
                
                public void execute() {
                    card.tap();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(card.getController()).append(" gains 1 life");
                    ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Faerie Conclave")) {
        	final long[] timeStamp = new long[1];
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 2792041290726604698L;
                
                public void execute() {
                    card.tap();
                }
            });
            
            final SpellAbility a1 = new Ability(card, "1 U") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Faerie" };
                    String[] keywords = { "Flying" };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 2, 1, types, keywords, "U");

                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = 5106629534549783845L;
                        
                        public void execute() {
                        	long stamp = timeStamp[0];
                            Card c = card;
                            String[] types = { "Creature", "Faerie" };
                            String[] keywords = { "Flying" };
                            CardFactoryUtil.revertManland(c, types, keywords, "U", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            a1.setDescription("1 U: Faerie Conclave becomes a 2/1 blue Faerie creature with flying until end of turn. It's still a land.");
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 2/1 creature with flying until EOT");
            a1.setStackDescription(sb.toString());
            
            Command paid1 = new Command() {
                private static final long serialVersionUID = -601119544294387668L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Forbidding Watchtower")) {
        	final long[] timeStamp = new long[1];
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 5212793782060828409L;
                
                public void execute() {
                    card.tap();
                }
            });
            
            final SpellAbility a1 = new Ability(card, "1 W") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Soldier" };
                    String[] keywords = {  };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 1, 5, types, keywords, "W");

                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = 8806880921707550181L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            String[] types = { "Creature", "Soldier" };
                            String[] keywords = {  };

                            CardFactoryUtil.revertManland(c, types, keywords, "W", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            a1.setDescription("1 W: Forbidding Watchtower becomes a 1/5 white Soldier creature until end of turn. It's still a land.");
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 1/5 creature until EOT");
            a1.setStackDescription(sb.toString());
            
            Command paid1 = new Command() {
                private static final long serialVersionUID = -7211256926392695778L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Treetop Village")) {
        	final long[] timeStamp = new long[1];
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -2246560994818997231L;
                
                public void execute() {
                    card.tap();
                }
            });
            
            final SpellAbility a1 = new Ability(card, "1 G") {
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Creature")  && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Ape" };
                    String[] keywords = { "Trample" };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 3, 3, types, keywords, "G");

                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -8535770979347971863L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            
                            String[] removeTypes = { "Creature", "Ape" };
                            String[] removeKeywords = { "Trample" };
                            CardFactoryUtil.revertManland(c, removeTypes, removeKeywords, "G", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 3/3 creature with trample until EOT");
            a1.setStackDescription(sb.toString());
            a1.setDescription("1 G: Treetop Village becomes a 3/3 green Ape creature with trample until end of turn. It's still a land.");
            
            Command paid1 = new Command() {
                private static final long serialVersionUID = -6800983290478844750L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Blinkmoth Nexus")) {
        	final long[] timeStamp = new long[1];
            final SpellAbility a1 = new Ability(card, "1") {
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Artifact", "Creature", "Blinkmoth" };
                    String[] keywords = { "Flying" };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 1, 1, types, keywords, "0");
                    
                    final Command eot1 = new Command() {
                    	private static final long serialVersionUID = 3564161001279001235L;
                    	long stamp = timeStamp[0];
                    	public void execute() {
                    		Card c = card;
                    		String[] types = { "Artifact", "Creature", "Blinkmoth" };
                    		String[] keywords = { "Flying" };
                    		CardFactoryUtil.revertManland(c, types, keywords, "", stamp);
                    	}
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            card.addSpellAbility(a1);
            a1.setDescription("1: Blinkmoth Nexus becomes a 1/1 Blinkmoth artifact creature with flying until end of turn. It's still a land.");
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 1/1 creature with flying until EOT");
            a1.setStackDescription(sb.toString());
            
            Command paid1 = new Command() {
                private static final long serialVersionUID = -5122292582368202498L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mishra's Factory")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "1") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Artifact", "Creature", "Assembly-Worker" };
                    String[] keywords = { };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 2, 2, types, keywords, "0");

                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -956566640027406078L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            
                            String[] types = { "Artifact", "Creature", "Assembly-Worker" };
                            String[] keywords = { };
                            CardFactoryUtil.revertManland(c, types, keywords, "", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            card.addSpellAbility(a1);
            a1.setDescription("1: Mishra's Factory becomes a 2/2 Assembly-Worker artifact creature until end of turn. It's still a land.");
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - becomes a 2/2 creature until EOT");
            a1.setStackDescription(sb.toString());
            
            // is this even needed?
            Command paid1 = new Command() {
                private static final long serialVersionUID = -6767109002136516590L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Dark Depths")) {
                        
            card.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card, Counters.ICE , 10));
            
            final SpellAbility ability = new Ability(card, "3") {
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    if(card.getCounters(Counters.ICE) > 0 && AllZone.GameAction.isCardInPlay(card) && super.canPlay()) return true;
                    else return false;
                }
                
                @Override
                public boolean canPlayAI() {
                    String phase = AllZone.Phase.getPhase();
                    return phase.equals(Constant.Phase.Main2) && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.ICE, 1);
                    
                    if(card.getCounters(Counters.ICE) == 0) 
                    {CardFactoryUtil.makeToken("Marit Lage",
                            "B 20 20 Marit Lage", card.getController(), "B", new String[] {"Legendary", "Creature", "Avatar"}, 20,
                            20, new String[] {"Flying", "Indestructible"});
                    	AllZone.GameAction.sacrifice(card);
                    }
                }
            };
            final SpellAbility sacrifice = new Ability(card, "0") {
            	//TODO - this should probably be a state effect
                @Override
                public boolean canPlay() {
                    return card.getCounters(Counters.ICE) == 0 && AllZone.GameAction.isCardInPlay(card) && super.canPlay();
                }
                
                @Override
                public boolean canPlayAI() {
                    return canPlay() && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    if(card.getCounters(Counters.ICE) == 0) {
                    CardFactoryUtil.makeToken("Marit Lage",
                            "B 20 20 Marit Lage", card.getController(), "B", new String[] {"Legendary", "Creature", "Avatar"}, 20,
                            20, new String[] {"Flying", "Indestructible"});
                    }
                    AllZone.GameAction.sacrifice(card);  
                }
            };
            //ability.setDescription("Dark Depths enters the battlefield with ten ice counters on it.\r\n\r\n3: Remove an ice counter from Dark Depths.\r\n\r\nWhen Dark Depths has no ice counters on it, sacrifice it. If you do, put an indestructible legendary 20/20 black Avatar creature token with flying named Marit Lage onto the battlefield.");
            ability.setDescription("3: remove an Ice Counter.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - remove an ice counter.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
            sacrifice.setStackDescription("Sacrifice "+card.getName());
            card.addSpellAbility(sacrifice);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Gods' Eye, Gate to the Reikai")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Spirit", "C 1 1 Spirit", card.getController(), "", new String[] {
                            "Creature", "Spirit"}, 1, 1, new String[] {""});
                }//resolve()
            };//Ability
            
            Command makeToken = new Command() {
                private static final long serialVersionUID = 2339209292936869322L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - put a 1/1 Spirit creature token into play");
                	ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(makeToken);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Flagstones of Trokair")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    CardList plains = new CardList(lib.getCards());
                    plains = plains.getType("Plains");
                    
                    if(card.getController().equals(AllZone.ComputerPlayer)) {
                        if(plains.size() > 0) {
                            Card c = plains.get(0);
                            lib.remove(c);
                            play.add(c);
                            c.tap();
                            
                        }
                    } else // human
                    {
                        if(plains.size() > 0) {
                            Object o = AllZone.Display.getChoiceOptional(
                                    "Select plains card to put into play tapped: ", plains.toArray());
                            if(o != null) {
                                Card c = (Card) o;
                                lib.remove(c);
                                play.add(c);
                                c.tap();
                            }
                        }
                    }
                    card.getController().shuffle();
                }//resolve()
            };//Ability
            
            Command fetchPlains = new Command() {
                
                private static final long serialVersionUID = 5991465998493672076L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - search library for a plains card and put it into play tapped.");
                	ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(fetchPlains);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mutavault")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "1") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature" };
                    String[] keywords = { "Changeling" };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 2, 2, types, keywords, "0");

                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = 5106629534549783845L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;

                            String[] types = { "Creature" };
                            String[] keywords = { "Changeling" };
                            CardFactoryUtil.revertManland(c, types, keywords, "", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            a1.setDescription("1: Mutavault becomes a 2/2 creature with all creature types until end of turn. It's still a land.");
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 2/2 creature with changeling until EOT");
            a1.setStackDescription(sb.toString());
            
            Command paid1 = new Command() {
                private static final long serialVersionUID = -601119544294387668L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Spawning Pool")) {
        	final long[] timeStamp = new long[1];
            
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -451839437837081897L;
                
                public void execute() {
                    card.setShield(0);
                }
            };
            
            final SpellAbility a2 = new Ability(card, "B") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    card.addShield();
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }
            };//SpellAbility
            a2.setType("Extrinsic"); // Required for Spreading Seas
            a2.setDescription("B: Regenerate Spawning Pool.");
            a2.setStackDescription("Regenerate Spawning Pool");
            
            a2.setBeforePayMana(new Input_PayManaCost(a2));
            
            final SpellAbility a1 = new Ability(card, "1 B") {
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Creature") && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Skeleton" };
                    String[] keywords = {  };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 1, 1, types, keywords, "B");
                    
                    // Don't stack Regen ability
                    boolean hasRegen = false;
                    SpellAbility[] sas = card.getSpellAbility();
                    for(SpellAbility sa:sas) {
                        if(sa.toString().equals("B: Regenerate Spawning Pool.")) //this is essentially ".getDescription()"
                        hasRegen = true;
                    }
                    if(!hasRegen) {
                        card.addSpellAbility(a2);
                    }
                    
                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -8535770979347971863L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            String[] types = { "Creature", "Skeleton" };
                            String[] keywords = {  };
                            CardFactoryUtil.revertManland(c, types, keywords, "B", stamp);
                            c.removeSpellAbility(a2);
                        }
                    };

                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 1/1 skeleton creature with B: regenerate this creature until EOT");
            a1.setStackDescription(sb.toString());
            a1.setDescription("1B: Spawning Pool becomes a 1/1 skeleton creature with B: regenerate this creature until end of the turn. It's still a land.");
            Command paid1 = new Command() {
                private static final long serialVersionUID = -6800983290478844750L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Novijen, Heart of Progress")) {
            card.clearSpellKeepManaAbility();
            
            final CardListFilter targets = new CardListFilter() {
                
                public boolean addCard(Card c) {
                    return AllZone.GameAction.isCardInPlay(c) && c.isCreature()
                            && c.getTurnInZone() == AllZone.Phase.getTurn();
                }
            };
            
            Ability_Cost abCost = new Ability_Cost("G U T", cardName, true);
            Ability_Activated ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = 1416258136308898492L;
                
                CardList                  inPlay           = new CardList();
                
                @Override
                public boolean canPlayAI() {
                    if(!(AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getPlayerTurn().equals(
                            AllZone.ComputerPlayer))) return false;
                    inPlay.clear();
                    inPlay.addAll(AllZone.Computer_Battlefield.getCards());
                    return (inPlay.filter(targets).size() > 1) && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    inPlay.clear();
                    inPlay.addAll(AllZone.Human_Battlefield.getCards());
                    inPlay.addAll(AllZone.Computer_Battlefield.getCards());
                    for(Card targ:inPlay.filter(targets))
                        targ.addCounter(Counters.P1P1, 1);
                }
            };
            ability.setDescription(abCost+"Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            ability.setStackDescription(cardName+" - Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            card.addSpellAbility(ability);
        }
        //*************** END ************ END **************************
        
        
       //*************** START *********** START **************************
        else if(cardName.equals("Crypt of Agadeem")) {
        	Ability_Cost abCost = new Ability_Cost("2 T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = -3561865824450791583L;
                
                @Override
                public void resolve() {
                    Card mp = AllZone.ManaPool;
                    CardList evildead = AllZoneUtil.getPlayerTypeInGraveyard(card.getController(), "Creature");
                    evildead = evildead.filter(AllZoneUtil.black);
                    
                    for(int i = 0; i < evildead.size(); i++) {
                        mp.addExtrinsicKeyword("ManaPool:B");
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            
            ability.setDescription(abCost+"Add B to your mana pool for each for each black creature card in your graveyard.");
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" adds B to your mana pool for each black creature card in your graveyard.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
       
        //*************** START *********** START **************************
        else if(cardName.equals("Svogthos, the Restless Tomb")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "3 B G") {
                @Override
                public boolean canPlayAI() {
                    PlayerZone compGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
                    CardList list = new CardList();
                    list.addAll(compGrave.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature();
                        }
                    });
                    return ((list.size() > 0) & !card.getType().contains("Creature")) && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Zombie", "Plant" };
                    String[] keywords = {  };

                    timeStamp[0] = CardFactoryUtil.activateManland(c, 1, 1, types, keywords, "B G");

                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -8535770979347971863L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            String[] types = { "Creature", "Zombie", "Plant" };
                            String[] keywords = {  };

                            CardFactoryUtil.revertManland(c, types, keywords, "B G", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a black and green Plant Zombie creature with power and toughness each equal to the number of creature cards in your graveyard until EOT");
            a1.setStackDescription(sb.toString());            
            a1.setDescription("3 B G: Until end of turn, Svogthos, the Restless Tomb becomes a black and green Plant Zombie creature with This creature's power and toughness are each equal to the number of creature cards in your graveyard. It's still a land.");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Ghitu Encampment")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "1 R") {
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Creature") && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Warrior" };
                    String[] keywords = { "First Strike" };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 2, 1, types, keywords, "R");

                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -8535770979347971863L;
                        
                        public void execute() {
                        	long stamp = timeStamp[0];
                            Card c = card;
                            String[] types = { "Creature", "Warrior" };
                            String[] keywords = { "First Strike" };

                            CardFactoryUtil.revertManland(c, types, keywords, "R", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 2/1 creature with first strike until EOT");
            a1.setStackDescription(sb.toString());
            a1.setDescription("1 R: Ghitu Encampment becomes a 2/1 red Warrior creature with first strike until end of turn. It's still a land.");
            
            Command paid1 = new Command() {
                private static final long serialVersionUID = -6800983290478844750L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Stalking Stones")) {
            
            final SpellAbility a1 = new Ability(card, "6") {
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Creature") && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Artifact", "Creature", "Elemental" };
                    String[] keywords = {  };
                    
                    CardFactoryUtil.activateManland(c, 3, 3, types, keywords, "0");
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 3/3 Elemental artifact creature that's still a land.");
            a1.setStackDescription(sb.toString());            
            a1.setDescription("6: Stalking Stones becomes a 3/3 Elemental artifact creature that's still a land. (This effect lasts indefinitely.)");
            
            Command paid1 = new Command() {
                private static final long serialVersionUID = -6800983290478844750L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Celestial Colonnade")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "3 W U") {
                @Override
                public boolean canPlayAI() {
                    return !card.hasSickness() && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    
                    String[] types = { "Creature", "Elemental" };
                    String[] keywords = { "Vigilance", "Flying" };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 4, 4, types, keywords, "W U");
                    
                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = 7377356496869217420L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            String[] types = { "Creature", "Elemental" };
                            String[] keywords = { "Vigilance", "Flying" };
                            CardFactoryUtil.revertManland(c, types, keywords, "W U", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - until end of turn, Celestial Colonnade becomes a 4/4 white and blue Elemental creature with flying and vigilance.");
            a1.setStackDescription(sb.toString());
            a1.setDescription("3 W U: Until end of turn, Celestial Colonnade becomes a 4/4 white and blue Elemental creature with flying and vigilance. It's still a land.");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lavaclaw Reaches")) {
        	final long[] timeStamp = new long[1];
            final SpellAbility X_ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
					PlayerZone opponentPlayZone = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
			        CardList opponentCreatureList = new CardList(opponentPlayZone.getCards());
			        opponentCreatureList = opponentCreatureList.getType("Creature");
      			  int n = ComputerUtil.getAvailableMana().size() - 1;
      			  if(n > 0) setManaCost(n + "");
                    return (n > 0 && opponentCreatureList.size() == 0) && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    final Card c = card;
                    for(int i = 0; i < Integer.parseInt(getManaCost()); i++) {
                        c.addTempAttackBoost(1);   	
                    }                  
                    c.updateObservers();
                    
                    Command untilEOT = new Command() {
                        private static final long serialVersionUID = -28032591440730370L;
                        
                        public void execute() {
                            for(int i = 0; i < Integer.parseInt(getManaCost()); i++) {
                            c.addTempAttackBoost(-1);
                            }
                        }
                    };
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }//resolve()
            };//SpellAbility 
            
            X_ability.setType("Extrinsic"); // Required for Spreading Seas
            
          	  X_ability.setBeforePayMana(new Input()
        	  {
        		private static final long serialVersionUID = 437814522686732L;

    			public void showMessage()
        		 {
        			 String s = JOptionPane.showInputDialog("What would you like X to be?");
        	  		 try {
        	  			     Integer.parseInt(s);
        	  				 X_ability.setManaCost(s);
        	  				 stopSetNext(new Input_PayManaCost(X_ability));
        	  			 }
        	  			 catch(NumberFormatException e){
        	  				 AllZone.Display.showMessage("\"" + s + "\" is not a number.");
        	  				 showMessage();
        	  			 }
        		 }
        	  });
              
            final SpellAbility a1 = new Ability(card, "1 B R") {
                @Override
                public boolean canPlayAI() {
                    return (!card.hasSickness() && !card.getType().contains("Creature")) && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
	                Card c = card;
	                String[] types = { "Creature", "Elemental" };
	                String[] keywords = {  };
	                timeStamp[0] = CardFactoryUtil.activateManland(c, 2, 2, types, keywords, "B R");
                    
					card.removeSpellAbility(X_ability);
					X_ability.setDescription("X: This creature gets +X/+0 until end of turn.");
					X_ability.setStackDescription("X: This creature gets +X/+0 until end of turn.");
					card.addSpellAbility(X_ability);
                    
		              final Command eot1 = new Command() {
		                  private static final long serialVersionUID = -132950142223575L;
		                  long stamp = timeStamp[0];
		                  public void execute() {
		                      Card c = card;
		                      String[] types = { "Creature", "Elemental" };
		                      String[] keywords = {  };
		                      CardFactoryUtil.revertManland(c, types, keywords, "B R", stamp);
		                      c.removeSpellAbility(X_ability);
		                  }
		              };
					
                    AllZone.EndOfTurn.addUntil(eot1);
                }

            };//SpellAbility
            
            final Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = 4245563898487609274L;

				public void execute() {
					// Comes into tapped Keyword gets removed, so this this command does the tapping. Keyword is still required for things like Amulet of Vigor (Not tested)
					card.tap();
                }
            };

            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            card.addComesIntoPlayCommand(comesIntoPlay);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - until end of turn, Lavaclaw Reaches becomes a 2/2 black and red Elemental creature with {X}: This creature gets +X/+0 until end of turn.");
            a1.setStackDescription(sb.toString());
            a1.setDescription("1 B R: Until end of turn, Lavaclaw Reaches becomes a 2/2 black and red Elemental creature with {X}: This creature gets +X/+0 until end of turn. It's still a land.");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Stirring Wildwood")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "1 G W") {
                @Override
                public boolean canPlayAI() {
                    return !card.hasSickness() && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Elemental" };
                    String[] keywords = { "Reach" };
                    
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 3, 4, types, keywords, "G W");
                    
                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -1329533520874994575L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            String[] types = { "Creature", "Elemental" };
                            String[] keywords = { "Reach" };
                            CardFactoryUtil.revertManland(c, types, keywords, "G W", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - until end of turn, Stirring Wildwood becomes a 3/4 green and white Elemental creature with reach.");
            a1.setStackDescription(sb.toString());
            a1.setDescription("1 G W: Until end of turn, Stirring Wildwood becomes a 3/4 green and white Elemental creature with reach. It's still a land.");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Creeping Tar Pit")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "1 U B") {
                @Override
                public boolean canPlayAI() {
                    return !card.hasSickness() && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Elemental" };
                    String[] keywords = { "Unblockable" };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 3, 2, types, keywords, "U B");
                    
                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -6004932967127014386L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            String[] types = { "Creature", "Elemental" };
                            String[] keywords = { "Unblockable" };
                            CardFactoryUtil.revertManland(c, types, keywords, "U B", stamp);	
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - Until end of turn, Creeping Tar Pit becomes a 3/2 blue and black Elemental creature and is unblockable.");
            a1.setStackDescription(sb.toString());
            a1.setDescription("1 U B: Until end of turn, Creeping Tar Pit becomes a 3/2 blue and black Elemental creature and is unblockable. It's still a land.");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Raging Ravine")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "2 R G") {
                @Override
                public boolean canPlayAI() {
                    return !card.hasSickness() && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Elemental"};
                    String[] keywords = { };
                    
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 3, 3, types, keywords, "R G");
                    
                    // this keyword stacks, so we can't put it through the activate
                    c.addIntrinsicKeyword("Whenever this creature attacks, put a +1/+1 counter on it.");
                    
                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -2632172918887247003L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            
                            String[] types = { "Creature", "Elemental"};
                            String[] keywords = { "Whenever this creature attacks, put a +1/+1 counter on it." };
                            CardFactoryUtil.revertManland(c, types, keywords, "R G", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - until end of turn, Raging Ravine becomes a 3/3 red and green Elemental creature with \"Whenever this creature attacks, put a +1/+1 counter on it.\"");
            a1.setStackDescription(sb.toString());
            a1.setDescription("2 R G: Until end of turn, Raging Ravine becomes a 3/3 red and green Elemental creature with \"Whenever this creature attacks, put a +1/+1 counter on it.\" It's still a land.");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Dread Statuary")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "4") {
                @Override
                public boolean canPlayAI() {
                    return !card.hasSickness() && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Artifact", "Creature", "Golem"};
                    String[] keywords = { };
                    
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 4, 2, types, keywords, "0");
                    
                    final Command eot1 = new Command() {
                        private static final long serialVersionUID = -2632172918887247003L;
                        long stamp = timeStamp[0];
                        public void execute() {
                            Card c = card;
                            
                            String[] types = { "Artifact", "Creature", "Golem"};
                            String[] keywords = {  };
                            CardFactoryUtil.revertManland(c, types, keywords, "", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
            };//SpellAbility
            
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - until end of turn, becomes a 4/2 Golem artifact creature until end of turn.");
            a1.setStackDescription(sb.toString());
            a1.setDescription("4: Until end of turn, becomes a 4/2 Golem artifact creature until end of turn. It's still a land.");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Phyrexian Tower")) {
            final Ability_Mana ability = new Ability_Mana(card, "tap, Sacrifice a creature: Add B B") {
				private static final long serialVersionUID = 5290938125518969674L;

				@Override
                public boolean canPlayAI() {
					return false;
                }
               
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                   
                    if(c != null && c.isCreature() ) {
                    	card.tap();
                    	AllZone.GameAction.sacrifice(c);
                    	super.resolve();
                    }
                }
                
                @Override
				public String mana() {
					return "B B";
            	}
            };
           
            Input runtime = new Input() {
				private static final long serialVersionUID = -7876248316975077074L;

				@Override
                public void showMessage() {
                    CardList choice = new CardList();
                    final Player player = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
                    choice.addAll(play.getCards());

                    choice = choice.getType("Creature");
                   
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice,
                            "Sacrifice a creature:", true, false));
                }
            };

            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Grove of the Burnwillows")) {
        	
            final Ability_Mana ability1 = new Ability_Mana(card, "tap, your opponent gains 1 life: add G") {
				private static final long serialVersionUID = 5290938125518969678L;
               
                @Override
                public void resolve() {
                    card.getController().getOpponent().gainLife(1, card);
                    super.resolve();
                }
                
                @Override
				public String mana() {
					return "G";
            	}
            };
            
            final Ability_Mana ability2 = new Ability_Mana(card, "tap, your opponent gains 1 life: add R") {
				private static final long serialVersionUID = 5290938125518969689L;
               
                @Override
                public void resolve() {
                    card.getController().getOpponent().gainLife(1, card);
                    super.resolve();
                }
                
                @Override
				public String mana() {
					return "R";
            	}
            };

            card.addSpellAbility(ability1);
            card.addSpellAbility(ability2);      
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lotus Vale")) {
        	/*
        	 * If Lotus Vale would enter the battlefield, sacrifice two untapped
        	 * lands instead. If you do, put Lotus Vale onto the battlefield.
        	 * If you don't, put it into its owner's graveyard.
        	 */
        	final Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = -194247993330560188L;
				
				final Player player = card.getController();
        		public void execute() {
        			if(player.isHuman()) {
        				final int[] paid = {0};

        				Input target = new Input() {
							private static final long serialVersionUID = -7835834281866473546L;
							public void showMessage() {
        						AllZone.Display.showMessage(cardName+" - Select an untapped land to sacrifice");
        						ButtonUtil.enableOnlyCancel();
        					}
        					public void selectButtonCancel() {
        						AllZone.GameAction.sacrifice(card);
        						stop();
        					}
        					public void selectCard(Card c, PlayerZone zone) {
        						if(c.isLand() && zone.is(Constant.Zone.Battlefield) && c.isUntapped()) {
        							AllZone.GameAction.sacrifice(c);
        							if(paid[0] < 1) {
        								paid[0]++;
        								AllZone.Display.showMessage(cardName+" - Select an untapped land to sacrifice");
        							}
        							else stop();
        						}
        					}//selectCard()
        				};//Input
        				if ((AllZoneUtil.getPlayerLandsInPlay(AllZone.HumanPlayer).filter(AllZoneUtil.untapped).size() < 2)) {
        					AllZone.GameAction.sacrifice(card);
        					return;
        				}
        				else AllZone.InputControl.setInput(target);
        			}
        			else {
        				//compy can't play this card because it has no mana pool
        			}
        		}
        	};

        	card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if(cardName.equals("Kjeldoran Outpost")) {
           final Command comesIntoPlay = new Command() {
              private static final long serialVersionUID = 6175830918425915833L;
              final Player player = card.getController();
              public void execute() {
                 CardList plains = AllZoneUtil.getPlayerTypeInPlay(player, "Plains");

                 if( player.equals(AllZone.ComputerPlayer)) {
                    if( plains.size() > 0 ) {
                       CardList tappedPlains = new CardList(plains.toArray());
                       tappedPlains = tappedPlains.filter(AllZoneUtil.tapped);
                       //if any are tapped, sacrifice it
                       //else sacrifice random
                       if( tappedPlains.size() > 0 ) {
                          AllZone.GameAction.sacrifice(tappedPlains.get(0));
                       }
                       else {
                          AllZone.GameAction.sacrifice(plains.get(0));
                       }
                    }
                    else {
                       AllZone.GameAction.sacrifice(card);
                    }
                 }
                 else { //this is the human resolution
                    Input target = new Input() {
                       private static final long serialVersionUID = 6653677835621129465L;
                       public void showMessage() {
                          AllZone.Display.showMessage(cardName+" - Select one plains to sacrifice");
                          ButtonUtil.enableOnlyCancel();
                       }
                       public void selectButtonCancel() {
                    	   AllZone.GameAction.sacrifice(card);
                    	   stop();
                       }
                       public void selectCard(Card c, PlayerZone zone) {
                          if(c.isLand() && zone.is(Constant.Zone.Battlefield) && c.getType().contains("Plains")) {
                             AllZone.GameAction.sacrifice(c);
                             stop();
                          }
                       }//selectCard()
                    };//Input
                    AllZone.InputControl.setInput(target);
                 }
              }
           };

           card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Scorched Ruins")) {
            final Command comesIntoPlay = new Command() {
               private static final long serialVersionUID = 6175830918425915833L;
               final Player player = card.getController();
               public void execute() {
                  PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                  CardList plains = new CardList(play.getCards());
                  plains = plains.getType("Land");
                  plains = plains.getTapState("Untapped");

                  if( player.equals(AllZone.ComputerPlayer)) {
                     if( plains.size() > 1 ) {
                        CardList tappedPlains = new CardList(plains.toArray());
                        tappedPlains = tappedPlains.getType("Basic");
                        for(Card c : tappedPlains)
                        	   AllZone.GameAction.sacrifice(c);
                        for(int i = 0; i < tappedPlains.size(); i++){
                           AllZone.GameAction.sacrifice(plains.get(i));
                        }
                        //if any are tapped, sacrifice it
                        //else sacrifice random
                     }
                     else {
                        AllZone.GameAction.sacrifice(card);
                     }
                  }
                  else { //this is the human resolution
                     final int[] paid = {0};
                     if ((new CardList(AllZone.Human_Battlefield.getCards())
                     .getType("Land").getTapState("Untapped").size() < 2))
                    	{
                    	 AllZone.GameAction.sacrifice(card);
                    	 return;
                    	}
                     Input target = new Input() {
                        private static final long serialVersionUID = 6653677835621129465L;
                        public void showMessage() {
                           AllZone.Display.showMessage("Scorched Ruins - Select an untapped land to sacrifice");
                           ButtonUtil.enableOnlyCancel();
                        }
                        public void selectButtonCancel() {
                     	   AllZone.GameAction.sacrifice(card);
                     	   stop();
                        }
                        public void selectCard(Card c, PlayerZone zone) {
                           if(c.isLand() && zone.is(Constant.Zone.Battlefield) && c.isUntapped()) {
                              AllZone.GameAction.sacrifice(c);
                              if(paid[0] < 1){
                            	  paid[0]++;
                            	  AllZone.Display.showMessage("Scorched Ruins - Select an untapped land to sacrifice");
                              }
                              else stop();
                           }
                        }//selectCard()
                     };//Input
                     AllZone.InputControl.setInput(target);
                  }
               }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
         }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Halimar Depths")) {
        	/*
        	 * When Halimar Depths enters the battlefield, look at the top three
        	 * cards of your library, then put them back in any order.
        	 */
        	final SpellAbility ability = new Ability(card, "0") {
        		@Override
        		public void resolve() {
        			if(card.getController().equals(AllZone.HumanPlayer)) {
        				AllZoneUtil.rearrangeTopOfLibrary(card.getController(), 3, false);
        			}
        		}//resolve()
        	};//SpellAbility
        	Command intoPlay = new Command() {
				private static final long serialVersionUID = 4523264604845132603L;

				public void execute() {
        			AllZone.Stack.add(ability);
        		}
        	};
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Rearrange the top 3 cards in your library in any order.");
        	ability.setStackDescription(sb.toString());
        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Island of Wak-Wak")) {
        	/*
        	 * Tap: The power of target creature with flying becomes 0 until end of turn.
        	 */
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
        	final String Tgts[] = {"Creature.withFlying"};
        	Target target = new Target("Select target creature with flying.", Tgts);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
        		private static final long serialVersionUID = -2090435946748184314L;

        		@Override
        		public boolean canPlayAI() {
        			Card c = getCreature();
                    if(c == null) return false;
                    else {
                        setTargetCard(c);
                        return super.canPlayAI();
                    }
                }//canPlayAI()
                
                //may return null
                private Card getCreature() {
                    CardList untapped = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
                    untapped = untapped.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && 0 < c.getNetDefense();
                        }
                    });
                    if(untapped.isEmpty()) return null;
                    
                    Card big = CardFactoryUtil.AI_getBestCreature(untapped);
                    return big;
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        final Card[] creature = new Card[1];
                        
                        creature[0] = getTargetCard();
                        final int[] originalAttack = {creature[0].getBaseAttack()};
                        creature[0].setBaseAttack(0);
                        
                        final Command EOT = new Command() {
							private static final long serialVersionUID = 3502589085738502851L;

							public void execute() {
                                if(AllZone.GameAction.isCardInPlay(creature[0])) {
                                    creature[0].setBaseAttack(originalAttack[0]);
                                }
                            }
                        };
                        AllZone.EndOfTurn.addUntil(EOT);
                    }//is card in play?
                }//resolve()
            };//SpellAbility
            ability.setDescription(abCost+"The power of target creature with flying becomes 0 until end of turn.");
            ability.setStackDescription(cardName+" - target creature's power becomes 0.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Magosi, the Waterveil")) {
        	/*
        	 * Magosi, the Waterveil enters the battlefield tapped.
        	 * Tap: Add Blue to your mana pool.
        	 * Blue, Tap: Put an eon counter on Magosi, the Waterveil. Skip your next turn.
        	 * Tap, Remove an eon counter from Magosi, the Waterveil and return it to its 
        	 * owner's hand: Take an extra turn after this one.
        	 */

        	Ability_Cost skipCost = new Ability_Cost("U T", card.getName(), true);
        	final SpellAbility skipTurn = new Ability_Activated(card, skipCost, null){
				private static final long serialVersionUID = -2404286785963486611L;

				@Override
        		public void resolve() {
					Player player = card.getController();
        			card.addCounter(Counters.EON, 1);
        			AllZone.Phase.addExtraTurn(player.getOpponent());                 
        		}
        	};//skipTurn
        	
        	Ability_Cost extraCost = new Ability_Cost("T SubCounter<1/EON> Return<1/CARDNAME>", card.getName(), true);
        	final SpellAbility extraTurn = new Ability_Activated(card, extraCost, null){
				private static final long serialVersionUID = -2599252144246080154L;

				@Override
        		public void resolve() {
        			AllZone.Phase.addExtraTurn(getActivatingPlayer());
        		}
        	};//extraTurn
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append("U, tap: Put an eon counter on ").append(card.getName()).append(". Skip your next turn.");
        	skipTurn.setDescription(sbDesc.toString());
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(card.getName()).append(" - add an Eon counter and skip you next turn.");
        	skipTurn.setStackDescription(sbStack.toString());
        	card.addSpellAbility(skipTurn);
        	
        	StringBuilder sbDesc2 = new StringBuilder();
        	sbDesc2.append(extraCost.toString());
        	sbDesc2.append("Take an extra turn after this one.");
        	extraTurn.setDescription(sbDesc2.toString());
        	StringBuilder sb = new StringBuilder();
        	sb.append(card.getName()).append(" - Take an extra turn after this one.");
        	extraTurn.setStackDescription(sb.toString());
        	card.addSpellAbility(extraTurn);
        }//*************** END ************ END **************************
        
        
        //*************** START ************ START **************************
        else if(cardName.equals("Bottomless Vault") || cardName.equals("Dwarven Hold")
        		|| cardName.equals("Hollow Trees") || cardName.equals("Icatian Store")
        		|| cardName.equals("Sand Silos")) {
        	final int[] num = new int[1];
        	String shortTemp = "";
        	if(cardName.equals("Bottomless Vault")) shortTemp = "B";
        	if(cardName.equals("Dwarven Hold")) shortTemp = "R";
        	if(cardName.equals("Hollow Trees")) shortTemp = "G";
        	if(cardName.equals("Icatian Store")) shortTemp = "W";
        	if(cardName.equals("Sand Silos")) shortTemp = "U";
        	
        	final String shortString = shortTemp;
        	StringBuilder desc = new StringBuilder();
        	desc.append("tap, Remove any number of storage counters from ");
        	desc.append(cardName);
        	desc.append(": Add ");
        	desc.append(shortString);
        	desc.append(" to your mana pool for each charge counter removed this way.");
            
            final Ability_Mana addMana = new Ability_Mana(card, desc.toString()) {
				private static final long serialVersionUID = -7805885635696245285L;

				@Override
                public void undo() {
                    card.addCounter(Counters.STORAGE, num[0]);
                    card.untap();
                }
                
              //@Override
                public String mana() {
                	StringBuilder mana = new StringBuilder();
                	if(num[0] == 0) mana.append("0");
                	else {
                		for(int i = 0; i < num[0]; i++) {
                			mana.append(shortString).append(" ");
                		}
                	}
                    return mana.toString().trim();
                }
                
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.STORAGE, num[0]);
                    card.tap();
                    super.resolve();
                }
            };
            
            Input runtime = new Input() {
				private static final long serialVersionUID = -4990369861806627183L;

				@Override
                public void showMessage() {
					num[0] = card.getCounters(Counters.STORAGE);
                	String[] choices = new String[num[0]+1];
                	for(int j=0;j<=num[0];j++) {
                		choices[j] = ""+j;
                	}
                    String answer = (String)(AllZone.Display.getChoiceOptional(
                            "Storage counters to remove", choices));
                    num[0] = Integer.parseInt(answer);
                    AllZone.Stack.add(addMana);
                    stop();
                }
            };
            
            addMana.setBeforePayMana(runtime);
            card.addSpellAbility(addMana);
        }//*************** END ************ END **************************
        
        //*************** START ************ START **************************
        else if(cardName.equals("Urza's Power Plant") || cardName.equals("Urza's Mine") ||
        		cardName.equals("Urza's Tower")) {
        	final String[] bonus = new String[1];
        	
        	StringBuilder desc = new StringBuilder();
        	desc.append("Tap: Add 1 to your mana pool. ");
        	if(cardName.equals("Urza's Power Plant")) {
        		bonus[0] = "2";
        		desc.append("If you control an Urza's Mine and an Urza's Tower, add 2 to your mana pool instead.");
        	}
        	else if(cardName.equals("Urza's Mine")) {
        		bonus[0] = "2";
        		desc.append("If you control an Urza's Power Plant and an Urza's Tower, add 2 to your mana pool instead.");
        	}
        	else if(cardName.equals("Urza's Tower")) {
        		bonus[0] = "3";
        		desc.append("If you control an Urza's Mine and an Urza's Power Plant, add 3 to your mana pool instead.");
        	}
        	
            final Ability_Mana addMana = new Ability_Mana(card, desc.toString()) {
				private static final long serialVersionUID = -3598374122722723225L;

				@Override
                public void undo() {
                    card.untap();
                }
                
              //@Override
                public String mana() {
                	if(AllZoneUtil.hasAllUrzas(card.getController()))
                		return bonus[0];
                	else return "1";
                }
                
                @Override
                public void resolve() {
                    card.tap();
                    super.resolve();
                }
            };
            
            addMana.setDescription(desc.toString());
            card.addSpellAbility(addMana);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Saprazzan Skerry") || cardName.equals("Remote Farm") ||
        		cardName.equals("Sandstone Needle") || cardName.equals("Peat Bog") ||
        		cardName.equals("Hickory Woodlot")) {
        	
        	String shortTemp = "";
        	if(cardName.equals("Saprazzan Skerry")) shortTemp = "U";
        	if(cardName.equals("Remote Farm")) shortTemp = "W";
        	if(cardName.equals("Sandstone Needle")) shortTemp = "R";
        	if(cardName.equals("Peat Bog")) shortTemp = "B";
        	if(cardName.equals("Hickory Woodlot")) shortTemp = "G";
        	
        	final String shortString = shortTemp;
        	StringBuilder desc = new StringBuilder();
        	desc.append("tap, Remove a depletion counter from ");
        	desc.append(cardName);
        	desc.append(": Add ");
        	desc.append(shortString).append(" ").append(shortString);
        	desc.append(" to your mana pool. If there are no depletion counters on ");
        	desc.append(cardName).append(", sacrifice it.");
        	
            final Ability_Mana ability = new Ability_Mana(card, desc.toString()) {
				private static final long serialVersionUID = -1147974499024433438L;

				@Override
                public boolean canPlayAI() {
					return false;
                }
				
				@Override
                public void undo() {
					card.addCounterFromNonEffect(Counters.DEPLETION, 1);
                    card.untap();
                    //if it got sacrificed, you're kind of screwed
                }
               
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.DEPLETION, 1);
                    if(card.getCounters(Counters.DEPLETION) == 0) {
                    	AllZone.GameAction.sacrifice(card);
                    }
                    super.resolve();
                }
                
                @Override
                public String mana() {
                	StringBuilder mana = new StringBuilder();
                	for(int i = 0; i < 2; i++) {
                		mana.append(shortString).append(" ");
                	}
                	return mana.toString().trim();
                }
            };
           
            Input runtime = new Input() {
				private static final long serialVersionUID = -7876248316975077074L;

				@Override
                public void showMessage() {
                    if(GameActionUtil.showYesNoDialog(card, "Remove a Depletion counter?")) {
                    	card.tap();
                    	AllZone.Stack.add(ability);
                    	stop();
                    }
                    else stop();
                }
            };

            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        //Lorwyn Dual Lands, and a couple Morningtide...
        else if(cardName.equals("Ancient Amphitheater") || cardName.equals("Auntie's Hovel")
                || cardName.equals("Gilt-Leaf Palace") || cardName.equals("Secluded Glen")
                || cardName.equals("Wanderwine Hub")
                || cardName.equals("Rustic Clachan") || cardName.equals("Murmuring Bosk")) {
        	
        	String shortTemp = "";
        	if(cardName.equals("Ancient Amphitheater")) shortTemp = "Giant";
        	if(cardName.equals("Auntie's Hovel")) shortTemp = "Goblin";
        	if(cardName.equals("Gilt-Leaf Palace")) shortTemp = "Elf";
        	if(cardName.equals("Secluded Glen")) shortTemp = "Faerie";
        	if(cardName.equals("Wanderwine Hub")) shortTemp = "Merfolk";
        	if(cardName.equals("Rustic Clachan")) shortTemp = "Kithkin";
        	if(cardName.equals("Murmuring Bosk")) shortTemp = "Treefolk";
        	
        	final String type = shortTemp;
        	
        	
            card.addComesIntoPlayCommand(new Command() {
				private static final long serialVersionUID = -5646344170306812481L;

				public void execute() {
                    if(card.getController().isHuman()) humanExecute();
                    else computerExecute();
                }
                
                public void computerExecute() {
                    CardList hand = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
                    hand = hand.filter(AllZoneUtil.getTypeFilter(type));
                    if(hand.size() > 0) revealCard(hand.get(0));
                    else card.tap();
                }
                
                public void humanExecute() {
                	AllZone.InputControl.setInput(new Input() {
						private static final long serialVersionUID = -2774066137824255680L;

						@Override
        				public void showMessage() {
        					AllZone.Display.showMessage(card.getName()+" - Reveal a card.");
        					ButtonUtil.enableOnlyCancel();
        				}

        				@Override
        				public void selectCard(Card c, PlayerZone zone) {
        					if(zone.is(Constant.Zone.Hand) && c.isType(type)) {
            					JOptionPane.showMessageDialog(null, "Revealed card: "+c.getName(), card.getName(), JOptionPane.PLAIN_MESSAGE);
            					stop();
        					}
        				}
        				
        				@Override
        				public void selectButtonCancel() {
        					card.tap();
        					stop();
        				}
        			});
                }//execute()
                
                private void revealCard(Card c) {
                	JOptionPane.showMessageDialog(null, c.getController()+" reveals "+c.getName(), card.getName(), JOptionPane.PLAIN_MESSAGE);
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START ************ START **************************
        else if(cardName.equals("Horizon Canopy")) {
        	/*
        	 * Tap, Pay 1 life: Add one W or G to your mana pool.
        	 */
        	Ability_Cost abCost = new Ability_Cost("T PayLife<1>", cardName, true);
        	Ability_Activated mana = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -5393697921811242255L;

				@Override
        		public void resolve() {
        			String color = "";
        			String[] colors = new String[] {Constant.Color.White, Constant.Color.Green};

        			Object o = AllZone.Display.getChoice("Choose mana color", colors);
        			color = (String) o;

        			if(color.equals("white")) color = "W";
        			else if(color.equals("green")) color = "G";

        			Card mp = AllZone.ManaPool;
        			mp.addExtrinsicKeyword("ManaPool:" + color);
        		}
        	};
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append(abCost).append("Add G or W to your mana pool.");
        	mana.setDescription(sbDesc.toString());
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(cardName).append(" - add G or W to your mana pool.");
        	mana.setStackDescription(sbStack.toString());
        	
        	card.addSpellAbility(mana);
        }//*************** END ************ END **************************
        
        
        //*************** START ************ START **************************
        else if(cardName.equals("Calciform Pools") || cardName.equals("Dreadship Reef") ||
        		cardName.equals("Fungal Reaches")  || cardName.equals("Molten Slagheap") ||
        		cardName.equals("Saltcrusted Steppe")) {
        	/*
        	 * tap, Remove X storage counters from Calciform Pools: Add X mana in any combination of W and/or U to your mana pool.
        	 */
        	final int[] num = new int[1];
        	final int[] split = new int[1];
        	
        	String pTemp = "";
        	String sTemp = "";
        	if(cardName.equals("Calciform Pools")) { pTemp = "W"; sTemp = "U"; }
        	if(cardName.equals("Dreadship Reef")) { pTemp = "U"; sTemp = "B"; }
        	if(cardName.equals("Fungal Reaches")) { pTemp = "R"; sTemp = "G"; }
        	if(cardName.equals("Molten Slagheap")) { pTemp = "B"; sTemp = "R"; }
        	if(cardName.equals("Saltcrusted Steppe")) { pTemp = "G"; sTemp = "W"; }
        	
        	final String primary = pTemp;
        	final String secondary = sTemp;
        	
        	final Ability_Mana addMana = new Ability_Mana(card, "tap, Remove X storage counters from "+cardName+": Add X mana in any combination of "+primary+" and/or "+secondary+" to your mana pool.") {
				private static final long serialVersionUID = 7177960799748450242L;

				//@Override
                public String mana() {
                	StringBuilder mana = new StringBuilder();
                	for(int i = 0; i < split[0]; i++) {
                		mana.append(primary).append(" ");
                	}
                	for(int j = 0; j < num[0] - split[0]; j++) {
                		mana.append(secondary).append(" ");
                	}
                    return mana.toString().trim();
                }
                
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.STORAGE, num[0]);
                    card.tap();
                    super.resolve();
                }
            };
            
            Input runtime = new Input() {
				private static final long serialVersionUID = -8808673510875540608L;

				@Override
                public void showMessage() {
					num[0] = card.getCounters(Counters.STORAGE);
                	String[] choices = new String[num[0]+1];
                	for(int j=0;j<=num[0];j++) {
                		choices[j] = ""+j;
                	}
                    String answer = (String)(AllZone.Display.getChoiceOptional(
                            "Storage counters to remove", choices));
                    num[0] = Integer.parseInt(answer);
                    
                    String splitNum = (String)(AllZone.Display.getChoiceOptional(
                            "Number of "+primary+" to add", choices));
                    split[0] = Integer.parseInt(splitNum);
                    if(num[0] > 0 || split[0] > 0) {
                    	AllZone.Stack.add(addMana);
                    }
                    stop();
                }
            };
            
            addMana.setBeforePayMana(runtime);
            card.addSpellAbility(addMana);
        }//*************** END ************ END **************************
        
        return card;
    }
    
}
