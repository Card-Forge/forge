
package forge;


import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;
import com.esotericsoftware.minlog.Log;

class CardFactory_Lands {
    
    private static final int hasKeyword(Card c, String k) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith(k)) return i;
        
        return -1;
    }
    
    public static Card getCard(final Card card, String cardName, Player owner) {
        
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
            Ability_Tap ability = new Ability_Tap(card) {
                
                private static final long serialVersionUID = 1416258136308898492L;
                
                CardList                  inPlay           = new CardList();
                
                @Override
                public boolean canPlayAI() {
                    if(!(AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer().equals(
                            AllZone.ComputerPlayer))) return false;
                    inPlay.clear();
                    inPlay.addAll(AllZone.Computer_Play.getCards());
                    return (inPlay.filter(targets).size() > 1);
                }
                
                @Override
                public void resolve() {
                    inPlay.clear();
                    inPlay.addAll(AllZone.Human_Play.getCards());
                    inPlay.addAll(AllZone.Computer_Play.getCards());
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
        if(cardName.equals("Blood Crypt") || cardName.equals("Breeding Pool") || cardName.equals("Godless Shrine")
                || cardName.equals("Hallowed Fountain") || cardName.equals("Overgrown Tomb")
                || cardName.equals("Sacred Foundry") || cardName.equals("Steam Vents")
                || cardName.equals("Stomping Ground") || cardName.equals("Temple Garden")
                || cardName.equals("Watery Grave")) {
            //if this isn't done, computer plays more than 1 copy
            //card.clearSpellAbility();
            card.clearSpellKeepManaAbility();
            
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 7352127748114888255L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) humanExecute();
                    else computerExecute();
                }
                
                public void computerExecute() {
                    boolean pay = false;
                    
                    if(AllZone.ComputerPlayer.getLife() > 9) pay = MyRandom.random.nextBoolean();
                    
                    if(pay) AllZone.ComputerPlayer.subtractLife(2,card);
                    else card.tap();
                }
                
                public void humanExecute() {
                    int life = card.getController().getLife();
                    if(2 < life) {
                        String[] choices = {"Yes", "No"};
                        Object o = AllZone.Display.getChoice("Pay 2 life?", choices);
                        if(o.equals("Yes")) AllZone.HumanPlayer.subtractLife(2,card);
                        else tapCard();
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
                    Card c = card;
                    //AllZone.GameAction.gainLife(c.getController(), 2);
                    c.getController().gainLife(2);
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
                    PlayerZone zone = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);             
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
                    PlayerZone Hzone = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);  
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
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
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
            tendo.setDescription("CARDNAME - tap, remove a charge counter: Add one mana of any color to your mana pool");
            tendo.setStackDescription("CARDNAME - tap, remove a charge counter: Add one mana of any color to your mana pool");
        	
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -2231880032957304542L;

				public void execute() {
					card.addCounter(Counters.CHARGE, 1);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Graypelt Refuge") || cardName.equals("Sejiri Refuge")
                || cardName.equals("Jwar Isle Refuge") || cardName.equals("Akoum Refuge")
                || cardName.equals("Kazandu Refuge")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;
                    //c.tap();
                    //AllZone.GameAction.gainLife(c.getController(), 1);
                    c.getController().gainLife(1);
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
//	      card.setManaCost("U");
            
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
                    return !card.getType().contains("Creature");
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
            
            final SpellAbility[] a2 = new SpellAbility[1];
            final Command eot2 = new Command() {
                private static final long serialVersionUID = 6180724472470740160L;
                
                public void execute() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        c.addTempAttackBoost(-1);
                        c.addTempDefenseBoost(-1);
                    }
                }
            };
            
            a2[0] = new Ability_Tap(card, "1") {
                private static final long serialVersionUID = 3561450520225198222L;
                
                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                /*
                 *  getAttacker() will now filter out non-Blinkmoths and non-Changelings
                 */
                
                public Card getAttacker() {
                    //target creature that is going to attack
                    Combat attackers = ComputerUtil.getAttackers();
                    CardList list = new CardList(attackers.getAttackers());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c) && 
                            (c.getType().contains("Blinkmoth") || c.getKeyword().contains("Changeling"));
                        }
                    });
                    list.remove(card);
                    list.shuffle();
                    
                    if(list.size() != 0) return list.get(0);
                    else return null;
                }//getAttacker()
                
                @Override
                public void resolve() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addTempAttackBoost(1);
                        c.addTempDefenseBoost(1);
                        
                        AllZone.EndOfTurn.addUntil(eot2);
                    }
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(a2[0]);
            a2[0].setDescription("1, tap: Target Blinkmoth gets +1/+1 until end of turn.");
            
/*
            @SuppressWarnings("unused")
            // target unused
            final Input target = new Input() {
                private static final long serialVersionUID = 8913477363141356082L;
                
                @Override
                public void showMessage() {
                    ButtonUtil.enableOnlyCancel();
                    AllZone.Display.showMessage("Select Blinkmoth to get +1/+1");
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(card, c)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if(c.isCreature() && c.getType().contains("Blinkmoth")) {
                        card.tap();
                        AllZone.Human_Play.updateObservers();
                        
                        a2[0].setTargetCard(c);//since setTargetCard() changes stack description
                        a2[0].setStackDescription(c + " gets +1/+1 until EOT");
                        
                        AllZone.InputControl.resetInput();
                        AllZone.Stack.add(a2[0]);
                    }
                }//selectCard()
                
                @Override
                public void selectButtonCancel() {
                    card.untap();
                    stop();
                }
            };//Input target
*/
            /*
             *  This input method will allow the human to select both Blinkmoths and Changelings
             */
            
            Input runtime = new Input() {
				private static final long serialVersionUID = 2530992128400417560L;

				@Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList creatures = new CardList();
                    creatures.addAll(comp.getCards());
                    creatures.addAll(hum.getCards());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && CardFactoryUtil.canTarget(card, c) && 
                                  (c.getType().contains("Blinkmoth") || c.getKeyword().contains("Changeling"));
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2[0], creatures, "Select target Blinkmoth", true, false));
                }
            };//Input target
            a2[0].setBeforePayMana(runtime);
            
//          a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Blinkmoth"));
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mishra's Factory")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "1") {
                @Override
                public boolean canPlayAI() {
                    return false;
                    //it turns into a creature, but doesn't attack
//	             return (! card.getKeyword().contains("Flying") &&
//	                    (CardFactoryUtil.AI_getHumanCreature("Flying").isEmpty()));
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
            
            Command paid1 = new Command() {
                private static final long serialVersionUID = -6767109002136516590L;
                
                public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
            
            final SpellAbility[] a2 = new SpellAbility[1];
            final Command eot2 = new Command() {
                private static final long serialVersionUID = 6180724472470740160L;
                
                public void execute() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        c.addTempAttackBoost(-1);
                        c.addTempDefenseBoost(-1);
                    }
                }
            };
            
            a2[0] = new Ability_Tap(card) {
                private static final long serialVersionUID = 3561450520225198222L;
                
                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                /*
                 *  getAttacker() will now filter out non-Assembly-Workers and non-Changelings
                 */
                
                public Card getAttacker() {
                    //target creature that is going to attack
                    Combat attackers = ComputerUtil.getAttackers();
                    CardList list = new CardList(attackers.getAttackers());
                    list = list.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return CardFactoryUtil.canTarget(card, c) && 
                    		      (c.getType().contains("Assembly-Worker") || c.getKeyword().contains("Changeling"));
                    	}
                    });
//                  list = list.getType("Assembly-Worker");    // Should return only Assembly-Workers
                    list.remove(card);
                    list.shuffle();
                    
                    if (list.size() != 0 && 
                       (AllZone.Phase.getPhase().equals(Constant.Phase.Main1)) && 
                        CardFactoryUtil.canTarget(card, list.get(0))) return list.get(0);
                    else return null;
                }//getAttacker()
                
                @Override
                public void resolve() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addTempAttackBoost(1);
                        c.addTempDefenseBoost(1);
                        
                        AllZone.EndOfTurn.addUntil(eot2);
                    }
                }//resolve()
            };//SpellAbility
            
            /*
             *  We add this input method and the human can now target both
             *  Assembly-Workers and Changelings. And the Mishra's Factory
             *  will now tap when this ability is used.
             */
            
            Input runtime = new Input() {
				private static final long serialVersionUID = 7047534805576787311L;

				@Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList creatures = new CardList();
                    creatures.addAll(comp.getCards());
                    creatures.addAll(hum.getCards());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && CardFactoryUtil.canTarget(card, c) && 
                                  (c.getType().contains("Assembly-Worker") || c.getKeyword().contains("Changeling"));
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2[0], creatures, "Select target Assembly-Worker", true, false));
                }// showMessage
            };// Input runtime
            
            card.addSpellAbility(a2[0]);
            a2[0].setDescription("tap: Target Assembly-Worker gets +1/+1 until end of turn.");
            
            a2[0].setBeforePayMana(runtime);
            
//          a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Assembly-Worker"));
            
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Eye of Ugin")) {
            final Ability_Tap ability = new Ability_Tap(card, "7") {
                private static final long serialVersionUID = 54417412481917927L;

                @Override
                public boolean canPlayAI() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList list = new CardList(library.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardUtil.getColors(c).contains(Constant.Color.Colorless)  && c.isCreature();
                        }
                    });
                    if(super.canPlay() && list.size() > 0 && AllZone.GameAction.isCardInPlay(card)) return true;
                    else return false;                  
                }//canPlay()
                
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void computerResolve() {
                    CardList library = new CardList(AllZone.Computer_Library.getCards());
                    library = library.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardUtil.getColors(c).contains(Constant.Color.Colorless) && c.isCreature();
                        }
                    });
                    
                    if(library.isEmpty()) return;
                    CardListUtil.sortAttack(library);
                    Card target = null;
                    if(target == null) target = library.get(0);                  
                    AllZone.Computer_Library.remove(target);
                    AllZone.Computer_Hand.add(target);                 
                    AllZone.ComputerPlayer.shuffle();
                    JOptionPane.showMessageDialog(null, "The Computer searched for: " + target.getName(), "", JOptionPane.INFORMATION_MESSAGE);
                }//computerResolve()
                
                public void humanResolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    
                    CardList librarylist = new CardList(AllZone.Human_Library.getCards());
                    librarylist = librarylist.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardUtil.getColors(c).contains(Constant.Color.Colorless) && c.isCreature();
                        }
                    });
                    if(librarylist.size() > 0) {
                    Object o = AllZone.Display.getChoiceOptional("Choose a colorless creature card: ", librarylist.toArray());
                    if(o != null) {
                        Card target = (Card) o;
                        
                        library.remove(target);
                        hand.add(target);
                    }
                    } else {
                    	JOptionPane.showMessageDialog(null, "No more suitable cards in Library", "", JOptionPane.INFORMATION_MESSAGE);
                    }
                    card.getController().shuffle();
                }//resolve()
             };//SpellAbility
             
             card.addSpellAbility(ability);
             
             StringBuilder sb = new StringBuilder();
             sb.append(card.getController()).append(" - searches their library for a colorless creature card, reveals it, and put it into your hand. Then shuffle your library.");
             ability.setStackDescription(sb.toString());
             ability.setBeforePayMana(new Input_PayManaCost(ability));
             ability.setDescription("7, Tap: Search your library for a colorless creature card, reveal it, and put it into your hand. Then shuffle your library.");
        }//*************** END ************ END **************************
        
        /*keyworded
        //*************** START *********** START **************************
        else if(cardName.equals("Terramorphic Expanse") || cardName.equals("Evolving Wilds")) {
            //tap sacrifice
            final Ability_Tap ability = new Ability_Tap(card, "0") {
                private static final long serialVersionUID = 5441740362881917927L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                    
                    //sacrifice Sakura-Tribe Elder if Human has any creatures
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    return list.size() != 0 && card.isUntapped();
                    
                }
                
                @Override
                public void chooseTargetAI() {
                    AllZone.GameAction.sacrifice(card);
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList list = new CardList(library.getCards());
                    list = list.getType("Basic");
                    if(super.canPlay() && list.size() > 0 && AllZone.GameAction.isCardInPlay(card)) return true;
                    else return false;
                    
                }//canPlay()
                
                @Override
                public void resolve() {
                    if(card.getOwner().equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void computerResolve() {
                    CardList play = new CardList(AllZone.Computer_Play.getCards());
                    play = play.getType("Basic");
                    
                    CardList library = new CardList(AllZone.Computer_Library.getCards());
                    library = library.getType("Basic");
                    
                    //this shouldn't happen, but it is defensive programming, haha
                    if(library.isEmpty()) return;
                    
                    Card land = null;
                    
                    //try to find a basic land that isn't in play
                    for(int i = 0; i < library.size(); i++)
                        if(!play.containsName(library.get(i))) {
                            land = library.get(i);
                            break;
                        }
                    
                    //if not found
                    //library will have at least 1 basic land because canPlay() checks that
                    if(land == null) land = library.get(0);
                    
                    land.tap();
                    AllZone.Computer_Library.remove(land);
                    AllZone.Computer_Play.add(land);
                    
                    AllZone.GameAction.shuffle(AllZone.ComputerPlayer);
                }//computerResolve()
                
                public void humanResolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    CardList basicLand = new CardList(library.getCards());
                    basicLand = basicLand.getType("Basic");
                    
                    Object o = AllZone.Display.getChoiceOptional("Choose a basic land", basicLand.toArray());
                    if(o != null) {
                        Card land = (Card) o;
                        land.tap();
                        
                        library.remove(land);
                        play.add(land);
                    }
                    AllZone.GameAction.shuffle(card.getController());
                }//resolve()
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -4379321114820908030L;
                boolean                   once             = true;
                
                @Override
                public void showMessage() {
                    //this is necessary in order not to have a StackOverflowException
                    //because this updates a card, it creates a circular loop of observers
                    if(once) {
                        once = false;
                        AllZone.GameAction.sacrifice(card);
                        
                        ability.setStackDescription(card.getController()
                                + " - Search your library for a basic land card and put it into play tapped. Then shuffle your library.");
                        AllZone.Stack.add(ability);
                        
                        stop();
                    }
                }//showMessage()
            };
            card.addSpellAbility(ability);
            ability.setDescription("tap, Sacrifice " + card.getName() + ": Search your library for a basic land card and put it into play tapped. Then shuffle your library.");
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Wasteland") || cardName.equals("Strip Mine") || cardName.equals("Tectonic Edge")) {
        	
        	final CardListFilter landFilter = new CardListFilter() {
                public boolean addCard(Card c) {
                    if(card.getName().equals("Wasteland") || card.getName().equals("Tectonic Edge")) return !c.getType().contains("Basic");
                    else return true;
                }
            };
            //tap sacrifice
            String cost = "0";
            if(cardName.equals("Tectonic Edge")) cost = "1";
            final Ability_Tap ability = new Ability_Tap(card, cost) {
                private static final long serialVersionUID = 6865042319287843154L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    CardList list = AllZoneUtil.getTypeInPlay("Land");
                    list = list.filter(landFilter);
                    CardList Tectonic_EdgeList = AllZoneUtil.getPlayerLandsInPlay(card.getController().getOpponent());
                    if(card.getName().equals("Tectonic Edge") && Tectonic_EdgeList.size() < 4) return false;
                    if(super.canPlay() && list.size() > 0 && AllZone.GameAction.isCardInPlay(card)) return true;                   
                    else return false;
                }//canPlay()
                
                @Override
                public void resolve() {
                	Card target = getTargetCard();
                	AllZone.GameAction.sacrifice(card);
                    if(target != null) AllZone.GameAction.destroy(target);
                }//resolve()
            };//SpellAbility
            
            Input runtime = new Input() {
				private static final long serialVersionUID = -2682861227834676116L;
				@Override
            	public void showMessage() {
            		AllZone.Display.showMessage("Select target land to destroy");
            		ButtonUtil.enableOnlyCancel();
            	}//showMessage()

            	public void selectButtonCancel() {stop();}
            	public void selectCard(Card c, PlayerZone zone) {
            		if(zone.is(Constant.Zone.Play)) {
            			if((c.isLand() && card.getName().equals("Strip Mine")) ||
            					(!c.isBasicLand() && (card.getName().equals("Wasteland") || card.getName().equals("Tectonic Edge")))) {
            			card.tap(); //tapping Strip Mine
            			ability.setTargetCard(c);
            			AllZone.Stack.add(ability);
            			stop();
            			}
            		}
            	}
            };
            
            card.addSpellAbility(ability);
            ability.setDescription((card.getName().equals("Tectonic Edge")? "1, ":"")
            		+ "Tap, Sacrifice " + card.getName() + ": Destroy target "
                    + ((card.getName().equals("Wasteland") || (card.getName().equals("Tectonic Edge")))? "nonbasic":"") + " land."
                    + (card.getName().equals("Tectonic Edge")? " Activate this ability only if an opponent controls four or more lands.":""));
            if(cardName.equals("Tectonic Edge")) ability.setAfterPayMana(runtime);
            else ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Tortuga")) {
            final Input discardThenDraw = new Input() {
                private static final long serialVersionUID = -7119292573232058526L;
                int                       nCards           = 1;
                int                       n                = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select a card to discard");
                    ButtonUtil.disableAll();
                    
                    //in case no more cards in hand
                    if(n == nCards || AllZone.Human_Hand.getCards().length == 0) {
                        stop();
                        card.getController().drawCard();
                        n = 0; //very important, otherwise the 2nd time you play this ability, you
                        //don't have to discard
                    }
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(zone.is(Constant.Zone.Hand)) {
                        AllZone.GameAction.discard(card, null);
                        n++;
                        showMessage();
                    }
                }
            };//SpellAbility
            
            final Ability_Tap ability = new Ability_Tap(card) {
                
                private static final long serialVersionUID = -2946606436670861559L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    AllZone.InputControl.setInput(discardThenDraw);
                }
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("tap: Discard a card, then draw a card.");
            ability.setStackDescription("Tortuga - Discard a card, then draw a card.");
            ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
            

            final Ability_Tap ability2 = new Ability_Tap(card) {
                private static final long serialVersionUID = 8961266883009597786L;
                
                @Override
                public boolean canPlay() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    return hand.getCards().length == 7;
                }
                
                @Override
                public void resolve() {
                    card.getController().drawCard();
                }
            };//SpellAbility
            card.addSpellAbility(ability2);
            ability2.setDescription("tap: Draw a card. Play this ability only if you have exactly 7 cards in your hand.");
            ability2.setStackDescription("Tortuga - draw a card.");
            ability2.setBeforePayMana(new Input_NoCost_TapAbility(ability2));
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Library of Alexandria")) {
            final Ability_Tap ability2 = new Ability_Tap(card) {
                private static final long serialVersionUID = -3405763871882165537L;
                
                @Override
                public boolean canPlay() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    return hand.getCards().length == 7 && super.canPlay();
                }
                
                @Override
                public void resolve() {
                    card.getController().drawCard();
                }
            };//SpellAbility
            card.addSpellAbility(ability2);
            ability2.setDescription("tap: Draw a card. Play this ability only if you have exactly 7 cards in your hand.");
            ability2.setStackDescription("Library of Alexandria - draw a card.");
            ability2.setBeforePayMana(new Input_NoCost_TapAbility(ability2));
            
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
                    return phase.equals(Constant.Phase.Main2);
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
                    return canPlay();
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
        
        /*converted to keyword
        //*************** START *********** START **************************
        else if(cardName.equals("Karakas")) {
            final Ability_Tap ability = new Ability_Tap(card, "0") {
                
                private static final long serialVersionUID = -6589125907956046586L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.getKeyword().contains("Legendary");
                        }
                    });
                    
                    if(list.size() > 0) setTargetCard(CardFactoryUtil.AI_getBestCreature(list, card));
                    
                    return list.size() > 0;
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(c != null) {
                        if(CardFactoryUtil.canTarget(card, c) && c.isCreature()
                                && c.getType().contains("Legendary")) AllZone.GameAction.moveTo(AllZone.getZone(
                                Constant.Zone.Hand, c.getOwner()), c);
                    }
                }
            };
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = -7649200192384343204L;
                
                @Override
                public void showMessage() {
                    CardList choice = new CardList();
                    choice.addAll(AllZone.Human_Play.getCards());
                    choice.addAll(AllZone.Computer_Play.getCards());
                    
                    choice = choice.getType("Creature");
                    choice = choice.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.isCreature() && c.getType().contains("Legendary"));
                        }
                    });
                    
                    //System.out.println("size of choice: " + choice.size());
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice,
                            "Select target Legendary creature:", true, false));
                }
            };
            
            ability.setDescription("tap: Return target legendary creature to its owner's hand.");
            //ability.setStackDescription(card.getName() + " - gives target creature +1/+2 until end of turn.");
            
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
            
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Pendelhaven")) {
            final Ability_Tap ability = new Ability_Tap(card, "0") {
                private static final long serialVersionUID = 8154776336533992188L;
                
                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                public Card getAttacker() {
                    //target creature that is going to attack
                    Combat c = ComputerUtil.getAttackers();
                    CardList att = new CardList();
                    att.addAll(c.getAttackers());
                    
                    for(int i = 0; i < att.size(); i++) {
                        Card crd = att.get(i);
                        if(crd.getNetAttack() == 1 && crd.getNetDefense() == 1) return crd;
                    }
                    
                    return null;
                }//getAttacker()
                
                @Override
                public void resolve() {
                    
                    final Card[] target = new Card[1];
                    final Command untilEOT = new Command() {
                        
                        private static final long serialVersionUID = 6362813153010836856L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(target[0])) {
                                target[0].addTempAttackBoost(-1);
                                target[0].addTempDefenseBoost(-2);
                            }
                        }
                    };
                    
                    target[0] = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(target[0]) && target[0].getNetDefense() == 1
                            && target[0].getNetAttack() == 1 && CardFactoryUtil.canTarget(card, target[0])) {
                        target[0].addTempAttackBoost(1);
                        target[0].addTempDefenseBoost(2);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }
            };
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 6126636768830864856L;
                
                @Override
                public void showMessage() {
                    CardList choice = new CardList();
                    choice.addAll(AllZone.Human_Play.getCards());
                    choice.addAll(AllZone.Computer_Play.getCards());
                    
                    choice = choice.getType("Creature");
                    choice = choice.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getNetAttack() == 1 && c.getNetDefense() == 1);
                        }
                    });
                    
                    //System.out.println("size of choice: " + choice.size());
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice,
                            "Select target 1/1 Creature:", true, false));
                }
            };
            
            ability.setDescription("tap: Target 1/1 creature gets +1/+2 until end of turn.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - gives target creature +1/+2 until end of turn.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);

        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Okina, Temple to the Grandfathers")) {
            final Ability_Tap ability = new Ability_Tap(card, "G") {
                private static final long serialVersionUID = 8154776336533992188L;
                
                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                public Card getAttacker() {
                    //target creature that is going to attack
                    Combat c = ComputerUtil.getAttackers();
                    CardList att = new CardList();
                    att.addAll(c.getAttackers());
                    
                    for(int i = 0; i < att.size(); i++) {
                        Card crd = att.get(i);
                        if(crd.getType().contains("Legendary")) return crd;
                    }
                    
                    return null;
                }//getAttacker()
                
                @Override
                public void resolve() {
                    
                    final Card[] target = new Card[1];
                    final Command untilEOT = new Command() {
                        
                        private static final long serialVersionUID = 6362813153010836856L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(target[0])) {
                                target[0].addTempAttackBoost(-1);
                                target[0].addTempDefenseBoost(-1);
                            }
                        }
                        
                    };
                    
                    target[0] = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(target[0]) && target[0].getType().contains("Legendary")
                            && CardFactoryUtil.canTarget(card, target[0])) {
                        target[0].addTempAttackBoost(1);
                        target[0].addTempDefenseBoost(1);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }
            };
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 6126636768830864856L;
                
                @Override
                public void showMessage() {
                    CardList choice = new CardList();
                    choice.addAll(AllZone.Human_Play.getCards());
                    choice.addAll(AllZone.Computer_Play.getCards());
                    
                    choice = choice.getType("Creature");
                    choice = choice.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getType().contains("Legendary"));
                        }
                    });
                    
                    //System.out.println("size of choice: " + choice.size());
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice,
                            "Select target legendary Creature:", true, false));
                }
            };
            
            ability.setDescription("G, tap: Target legendary creature gets +1/+1 until end of turn.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - gives target legendary creature +1/+1 until end of turn.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
            
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Wirewood Lodge")) {
            final Ability_Tap ability = new Ability_Tap(card, "G") {
                private static final long serialVersionUID = -4352872789672871590L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    
                    final Card[] target = new Card[1];
                    

                    target[0] = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(target[0])
                            && target[0].isTapped()
                            && (target[0].getType().contains("Elf") || target[0].getKeyword().contains(
                                    "Changeling")) && CardFactoryUtil.canTarget(card, target[0])) {
                        target[0].untap();
                    }
                }
            };
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -6822924521729238991L;
                
                @Override
                public void showMessage() {
                    CardList choice = new CardList();
                    choice.addAll(AllZone.Human_Play.getCards());
                    choice.addAll(AllZone.Computer_Play.getCards());
                    
                    choice = choice.getType("Elf");
                    choice = choice.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.isTapped());
                        }
                    });
                    
                    //System.out.println("size of choice: " + choice.size());
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice, "Select target Elf", true,
                            false));
                }
            };
            
            ability.setDescription("G, tap: Untap target Elf.");
            ability.setStackDescription(card.getName() + " - untaps target elf.");
            
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
            
        }//*************** END ************ END **************************
        */
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Deserted Temple")) {
            final Ability_Tap ability = new Ability_Tap(card, "1") {

				private static final long serialVersionUID = -3463896908132386453L;

				@Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    
                    final Card[] target = new Card[1];
                    

                    target[0] = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(target[0])
                            && target[0].isTapped()
                            && (target[0].getType().contains("Land")) && CardFactoryUtil.canTarget(card, target[0])) {
                        target[0].untap();
                    }
                }
            };
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -6822924521729238991L;
                
                @Override
                public void showMessage() {
                    CardList choice = new CardList();
                    choice.addAll(AllZone.Human_Play.getCards());
                    choice.addAll(AllZone.Computer_Play.getCards());
                    
                    choice = choice.getType("Land");
                    choice = choice.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.isTapped());
                        }
                    });
                    
                    //System.out.println("size of choice: " + choice.size());
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice, "Select target Land", true,
                            false));
                }
            };
            
            ability.setDescription("1, tap: Untap target land.");
            
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
            
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Academy Ruins")) {
            final Ability_Tap ability = new Ability_Tap(card, "1 U") {
                private static final long serialVersionUID = -1322368528417127121L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void humanResolve() {
                    CardList cards = new CardList(AllZone.Human_Graveyard.getCards());
                    
                    CardList list = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        //System.out.println("type: " +cards.get(i).getType());
                        if(cards.get(i).getType().contains("Artifact")) {
                            //System.out.println(cards.get(i).getName());
                            Card c = cards.get(i);
                            list.add(c);
                            
                        }
                    }
                    
                    if(list.size() != 0) {
                        Object check = AllZone.Display.getChoiceOptional("Select Artifact", list.toArray());
                        if(check != null) {
                            //PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                            //library.add((Card)check, 0);
                            AllZone.GameAction.moveToTopOfLibrary((Card) check);
                        }
                    }
                }
                
                public void computerResolve() {
                    Card[] grave = AllZone.Computer_Graveyard.getCards();
                    CardList list = new CardList(grave);
                    CardList arts = new CardList();
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(list.get(i).getType().contains("Artifact")) {
                            Card k = list.get(i);
                            arts.add(k);
                        }
                    }
                    
                    //pick best artifact
                    if(arts.size() != 0) {
                        Card c = CardFactoryUtil.AI_getBestArtifact(list);
                        if(c == null) c = grave[0];
                        Log.debug("Academy Ruins", "computer picked - " + c);
                        AllZone.Computer_Graveyard.remove(c);
                        AllZone.Computer_Library.add(c, 0);
                    }
                }//computerResolve
                
                @Override
                public boolean canPlay() {
                	Player controller = card.getController();
                    
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, controller);
                    CardList list = new CardList(grave.getCards());
                    CardList cards = new CardList();
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(list.get(i).getType().contains("Artifact")) {
                            cards.add(list.get(i));
                        }
                    }
                    
                    if(cards.size() > 0 && AllZone.GameAction.isCardInPlay(card) && card.isUntapped()) return true;
                    else return false;
                }
                
            };
            
            ability.setDescription("1 U, tap: Put target artifact card in your graveyard on top of your library.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - put artifact card in your graveyard on top of your library.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Volrath's Stronghold")) {
            final Ability_Tap ability = new Ability_Tap(card, "1 B") {
                private static final long serialVersionUID = 2821525387844776907L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void humanResolve() {
                    CardList cards = new CardList(AllZone.Human_Graveyard.getCards());
                    
                    CardList list = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        //System.out.println("type: " +cards.get(i).getType());
                        if(cards.get(i).getType().contains("Creature")) {
                            //System.out.println(cards.get(i).getName());
                            Card c = cards.get(i);
                            list.add(c);
                            
                        }
                    }
                    
                    if(list.size() != 0) {
                        Object check = AllZone.Display.getChoiceOptional("Select Creature", list.toArray());
                        if(check != null) {
                            //PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                            //library.add((Card)check, 0);
                            AllZone.GameAction.moveToTopOfLibrary((Card) check);
                        }
                    }
                }
                
                public void computerResolve() {
                    Card[] grave = AllZone.Computer_Graveyard.getCards();
                    CardList list = new CardList(grave);
                    CardList creats = new CardList();
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(list.get(i).getType().contains("Creature")) {
                            Card k = list.get(i);
                            creats.add(k);
                        }
                    }
                    
                    //pick best artifact
                    if(creats.size() != 0) {
                        Card c = CardFactoryUtil.AI_getBestCreature(list);
                        if(c == null) c = grave[0];
                        //System.out.println("computer picked - " +c);
                        AllZone.Computer_Graveyard.remove(c);
                        //AllZone.Computer_Library.add(c, 0);
                        AllZone.GameAction.moveToTopOfLibrary(c);
                    }
                }//computerResolve
                
                @Override
                public boolean canPlay() {
                	Player controller = card.getController();
                    
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, controller);
                    CardList list = new CardList(grave.getCards());
                    CardList cards = new CardList();
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(list.get(i).getType().contains("Creature")) {
                            cards.add(list.get(i));
                        }
                    }
                    
                    if(super.canPlay() && cards.size() > 0 && AllZone.GameAction.isCardInPlay(card)
                            && card.isUntapped()) return true;
                    else return false;
                }
                
            };
            
            ability.setDescription("1 B, tap: Put target creature card in your graveyard on top of your library.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - put creature card in your graveyard on top of your library.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
            
            
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Oboro, Palace in the Clouds")) {
            final Ability ability = new Ability(card, "1") {
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    AllZone.GameAction.moveTo(hand, card);
                }
            };
            
            ability.setDescription("1: Return Oboro, Palace in the Clouds to your hand.");
            StringBuilder sb = new StringBuilder();
            sb.append("Return ").append(card.getName()).append(" to your hand.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        
        
        /*
        //*************** START *********** START **************************  
        else if(cardName.equals("Gargoyle Castle")) {
            final Ability_Tap ability = new Ability_Tap(card, "5") {
                
                private static final long serialVersionUID = 8524185208900629992L;
                
                @Override
                public boolean canPlay() {
                    if(AllZone.GameAction.isCardInPlay(card) && card.isUntapped()) return true;
                    else return false;
                }
                
                @Override
                public void resolve() {
                    AllZone.GameAction.sacrifice(card);
                    CardFactoryUtil.makeToken("Gargoyle", "C 3 4 Gargoyle", card, "", new String[] {
                            "Artifact", "Creature", "Gargoyle"}, 3, 4, new String[] {"Flying"});
                }
            };
            
            ability.setDescription("5, tap, sacrifice Gargoyle Castle: Put a 3/4 colorless Gargoyle artifact creature token with flying onto the battlefield.");
            ability.setStackDescription(card.getName()
                    + " - Put a 3/4 colorless Gargoyle artifact creature token with flying onto the battlefield.");
            
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        */
        /*
        //*************** START *********** START **************************  
        else if(cardName.equals("Kher Keep")) {
            final Ability_Tap ability = new Ability_Tap(card, "1 R") {
                private static final long serialVersionUID = 4037838521451709399L;
                
                @Override
                public boolean canPlay() {
                    if(AllZone.GameAction.isCardInPlay(card) && card.isUntapped()) return true;
                    else return false;
                }
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Kobolds of Kher Keep", "R 0 1 Kobolds of Kher Keep", card, "R",
                            new String[] {"Creature", "Kobold"}, 0, 1, new String[] {""});
                }
            };
            
            ability.setDescription("1 R, tap: Put a 0/1 red Kobold creature token named Kobolds of Kher Keep into play.");
            ability.setStackDescription(card.getName()
                    + " - Put a 0/1 red Kobold creature token named Kobolds of Kher Keep into play.");
            
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        */
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Vitu-Ghazi, the City-Tree")) {
            final Ability_Tap ability = new Ability_Tap(card, "2 G W") {
                private static final long serialVersionUID = 1781653158406511188L;
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Saproling", "G 1 1 Saproling", card, "G", new String[] {
                            "Creature", "Saproling"}, 1, 1, new String[] {""});
                }
            };
            
            ability.setDescription("2 G W, tap: Put a 1/1 green Saproling creature token into play.");
            ability.setStackDescription(card.getName()
                    + " - Put a 1/1 green Saproling creature token named into play.");
            
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        */
        
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                    return !card.getType().contains("Creature");
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
        else if(cardName.equals("Shizo, Death's Storehouse")) {
            final SpellAbility[] a2 = new SpellAbility[1];
            final Command eot2 = new Command() {
                private static final long serialVersionUID = 6180724472470740160L;
                
                public void execute() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        c.removeIntrinsicKeyword("Fear");
                    }
                }
            };
            
            a2[0] = new Ability_Tap(card, "B") {
                private static final long serialVersionUID = 3561450520225198222L;
                
                @Override
                public boolean canPlayAI() {
                    return getLegendaryAttackers().size() > 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                public CardList getLegendaryAttackers() {
                    Combat c = ComputerUtil.getAttackers();
                    CardList att = new CardList(c.getAttackers());
                    // Shizo can only target Legendary, don't target creatures that already have Fear
                    att = att.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return CardFactoryUtil.canTarget(card, c) && c.getType().contains("Legendary") 
                    			&& !c.getIntrinsicKeyword().contains("Fear");
                    	}
                    });
                    att.remove(card);
                    return att;
                }
                	
                public Card getAttacker() {
                    //target creature that is going to attack
                	CardList att = getLegendaryAttackers();
                    if (att.size() == 0) return null;
                    
                    att.shuffle();
                    return att.get(0);
                }//getAttacker()
                
                @Override
                public void resolve() {
                    Card c = a2[0].getTargetCard();
                    if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        if(!c.getIntrinsicKeyword().contains("Fear")) c.addIntrinsicKeyword("Fear");
                        
                        AllZone.EndOfTurn.addUntil(eot2);
                    }
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(a2[0]);
            a2[0].setDescription("B, tap: Target legendary creature gains fear until end of turn.");
            
            Input target = new Input() {
				private static final long serialVersionUID = 1032601702189887609L;
            	
				@Override
			    public void showMessage() {
					PlayerZone comp = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
			        PlayerZone hum = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
			        CardList legendaryCreats = new CardList();
			        legendaryCreats.addAll(comp.getCards());
			        legendaryCreats.addAll(hum.getCards());
			        legendaryCreats = legendaryCreats.filter(new CardListFilter() {
			        	public boolean addCard(Card c) {
			        		return c.isCreature() 
			        				&& c.getType().contains("Legendary") 
			        				&& CardFactoryUtil.canTarget(card, c);
			        	}
			        });
					stopSetNext(CardFactoryUtil.input_targetSpecific(a2[0], legendaryCreats, "Select target legendary creature", true, false));
				}//showMessage()
            };//Input target
            a2[0].setBeforePayMana(target);
            
/* This input allows the human to target any legendary card, creature or not!

            @SuppressWarnings("unused")
            // target unused
            final Input target = new Input() {
                private static final long serialVersionUID = 8913477363141356082L;
                
                @Override
                public void showMessage() {
                    ButtonUtil.enableOnlyCancel();
                    AllZone.Display.showMessage("Select legendary creature to get fear");
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(card, c)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if(c.isCreature() && c.getType().contains("Legendary")) {
                        card.tap();
                        AllZone.Human_Play.updateObservers();
                        
                        a2[0].setTargetCard(c);//since setTargetCard() changes stack description
                        a2[0].setStackDescription(c + " gets fear until EOT");
                        
                        AllZone.InputControl.resetInput();
                        AllZone.Stack.add(a2[0]);
                    }
                }//selectCard()
                
                @Override
                public void selectButtonCancel() {
                    card.untap();
                    stop();
                }
            };//Input target
            a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Legendary"));
*/
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        if(cardName.equals("Novijen, Heart of Progress")) {
            card.clearSpellKeepManaAbility();
            
            final CardListFilter targets = new CardListFilter() {
                
                public boolean addCard(Card c) {
                    return AllZone.GameAction.isCardInPlay(c) && c.isCreature()
                            && c.getTurnInZone() == AllZone.Phase.getTurn();
                }
            };
            Ability_Tap ability = new Ability_Tap(card, "G U") {
                private static final long serialVersionUID = 1416258136308898492L;
                
                CardList                  inPlay           = new CardList();
                
                @Override
                public boolean canPlayAI() {
                    if(!(AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer().equals(
                            AllZone.ComputerPlayer))) return false;
                    inPlay.clear();
                    inPlay.addAll(AllZone.Computer_Play.getCards());
                    return (inPlay.filter(targets).size() > 1);
                }
                
                @Override
                public void resolve() {
                    inPlay.clear();
                    inPlay.addAll(AllZone.Human_Play.getCards());
                    inPlay.addAll(AllZone.Computer_Play.getCards());
                    for(Card targ:inPlay.filter(targets))
                        targ.addCounter(Counters.P1P1, 1);
                }
            };
            ability.setDescription("G U, tap: Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            ability.setStackDescription("Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            card.addSpellAbility(ability);
        }
        //*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Urza's Factory")) {
            final Ability_Tap ability = new Ability_Tap(card, "7") {
                private static final long serialVersionUID = 1781653158406511188L;
                
                @Override
                public boolean canPlay() {
                    if(AllZone.GameAction.isCardInPlay(card)) return true;
                    else return false;
                }
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Assembly-Worker", "C 2 2 Assembly-Worker", card, "", new String[] {
                            "Artifact", "Creature", "Assembly-Worker"}, 2, 2, new String[] {""});
                }
            };
            
            ability.setDescription("7, tap: Put a 2/2 colorless Assembly-Worker artifact creature token onto the battlefield.");
            ability.setStackDescription(card.getName()
                    + " - Put a 2/2 colorless Assembly-Worker artifact creature token onto the battlefield.");
            
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Goblin Burrows")) {
            final SpellAbility[] a2 = new SpellAbility[1];
            final Command eot2 = new Command() {
                private static final long serialVersionUID = 6180724472470740160L;
                
                public void execute() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        c.addTempAttackBoost(-2);
                    }
                }
            };
            
            a2[0] = new Ability_Tap(card, "1 R") {
                private static final long serialVersionUID = 3561450520225198222L;
                
                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                /*
                 *  getAttacker() will now filter out non-Goblins and non-Changelings
                 */
                
                public Card getAttacker() {
                    //target creature that is going to attack
                    Combat attackers = ComputerUtil.getAttackers();
                    CardList list = new CardList(attackers.getAttackers());
                    list = list.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return CardFactoryUtil.canTarget(card, c) && 
                            (c.getType().contains("Goblin") || c.getKeyword().contains("Changeling"));
                    	}
                    });
                    list.remove(card);
                    list.shuffle();
                    
                    if(list.size() != 0) return list.get(0);
                    else return null;
                }//getAttacker()
                
                @Override
                public void resolve() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addTempAttackBoost(2);
                        
                        AllZone.EndOfTurn.addUntil(eot2);
                    }
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(a2[0]);
            a2[0].setDescription("1 R, tap: Target Goblin gets +2/+0 until end of turn.");
            
/*
            @SuppressWarnings("unused")
            // target unused
            final Input target = new Input() {
                private static final long serialVersionUID = 8913477363141356082L;
                
                @Override
                public void showMessage() {
                    ButtonUtil.enableOnlyCancel();
                    AllZone.Display.showMessage("Select Goblin to get +2/+0");
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(card, c)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if(c.isCreature() && (c.getType().contains("Goblin") || c.getKeyword().contains("Changeling") )) {
                        card.tap();
                        AllZone.Human_Play.updateObservers();
                        
                        a2[0].setTargetCard(c);//since setTargetCard() changes stack description
                        a2[0].setStackDescription(c + " gets +2/+0 until EOT");
                        
                        AllZone.InputControl.resetInput();
                        AllZone.Stack.add(a2[0]);
                    }
                }//selectCard()
                
                @Override
                public void selectButtonCancel() {
                    card.untap();
                    stop();
                }
            };//Input target
*/
            /*
             *  This input method will allow the human to select both Goblins and Changelings
             */
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 8320178628066517937L;

                @Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList creatures = new CardList();
                    creatures.addAll(comp.getCards());
                    creatures.addAll(hum.getCards());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && CardFactoryUtil.canTarget(card, c) && 
                                  (c.getType().contains("Goblin") || c.getKeyword().contains("Changeling"));
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2[0], creatures, "Select target Goblin", true, false));
                }
            };//Input target
            a2[0].setBeforePayMana(runtime);
            
//          a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Goblin"));
            
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Daru Encampment")) {
            final SpellAbility[] a2 = new SpellAbility[1];
            final Command eot2 = new Command() {
                private static final long serialVersionUID = 6180724472470740160L;
                
                public void execute() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        c.addTempAttackBoost(-1);
                        c.addTempDefenseBoost(-1);
                    }
                }
            };
            
            a2[0] = new Ability_Tap(card, "W") {
                private static final long serialVersionUID = 3561450520225198222L;
                
                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                /*
                 *  getAttacker() will now filter out non-Soldiers and non-Changelings
                 */
                
                public Card getAttacker() {
                    //target creature that is going to attack
                    Combat attackers = ComputerUtil.getAttackers();
                    CardList list = new CardList(attackers.getAttackers());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c) && 
                                  (c.getType().contains("Soldier") || c.getKeyword().contains("Changeling"));
                        }
                    });
                    list.remove(card);
                    list.shuffle();
                    
                    if(list.size() != 0) return list.get(0);
                    else return null;
                }//getAttacker()
                
                @Override
                public void resolve() {
                    Card c = a2[0].getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addTempAttackBoost(1);
                        c.addTempDefenseBoost(1);
                        
                        AllZone.EndOfTurn.addUntil(eot2);
                    }
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(a2[0]);
            a2[0].setDescription("W, tap: Target Soldier gets +1/+1 until end of turn.");
            
/*
            @SuppressWarnings("unused")
            // target unused
            final Input target = new Input() {
                private static final long serialVersionUID = 8913477363141356082L;
                
                @Override
                public void showMessage() {
                    ButtonUtil.enableOnlyCancel();
                    AllZone.Display.showMessage("Select Soldier to get +1/+1");
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(card, c)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if(c.isCreature() && (c.getType().contains("Soldier") || c.getKeyword().contains("Changeling") ) )  {
                        card.tap();
                        AllZone.Human_Play.updateObservers();
                        
                        a2[0].setTargetCard(c);//since setTargetCard() changes stack description
                        a2[0].setStackDescription(c + " gets +1/+1 until EOT");
                        
                        AllZone.InputControl.resetInput();
                        AllZone.Stack.add(a2[0]);
                    }
                }//selectCard()
                
                @Override
                public void selectButtonCancel() {
                    card.untap();
                    stop();
                }
            };//Input target
*/
            /*
             *  This input method will allow the human to select both Soldiers and Changelings
             */
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 8320178628066517937L;

                @Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList creatures = new CardList();
                    creatures.addAll(comp.getCards());
                    creatures.addAll(hum.getCards());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && CardFactoryUtil.canTarget(card, c) && 
                                  (c.getType().contains("Soldier") || c.getKeyword().contains("Changeling"));
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2[0], creatures, "Select target Soldier", true, false));
                }
            };//Input target
            a2[0].setBeforePayMana(runtime);
            
//          a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Soldier"));
            
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        if(cardName.equals("Duskmantle, House of Shadow")) {
            card.clearSpellKeepManaAbility();
            
            Ability_Tap ability = new Ability_Tap(card, "U B") {
                private static final long serialVersionUID = 42470566751344693L;
                
                @Override
                public boolean canPlayAI() {
                	Player player = getTargetPlayer();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    CardList libList = new CardList(lib.getCards());
                    return libList.size() > 0;
                }
                
                @Override
                public void resolve() {
                    AllZone.GameAction.mill(getTargetPlayer(),1);
                }
            };
            ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
            ability.setDescription("tap U B: Target player puts the top card of his or her library into his or her graveyard.");
            ability.setStackDescription("Target player puts the top card of his or her library into his or her graveyard.");
            card.addSpellAbility(ability);
        }
        //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        if(cardName.equals("Crypt of Agadeem")) {
            final SpellAbility ability = new Ability_Tap(card, "2") {
                private static final long serialVersionUID = -3561865824450791583L;
                
                @Override
                public void resolve() {
                    /*CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards());
                    list = list.getName("Mana Pool");*/
                    Card mp = AllZone.ManaPool;//list.getCard(0);
                    
                    PlayerZone Grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList evildead = new CardList();
                    evildead.addAll(Grave.getCards());
                    evildead = evildead.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.isCreature() && c.isBlack());
                        }
                    });
                    
                    for(int i = 0; i < evildead.size(); i++) {
                        mp.addExtrinsicKeyword("ManaPool:B");
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            
            ability.setDescription("2, tap: Add B to your mana pool for each for each black creature card in your graveyard.");
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" adds B to your mana pool for each black creature card in your graveyard.");
            ability.setStackDescription(sb.toString());
            
            //card.clearSpellAbility();
            //card.setText(card.getText() +  ability.toString());
            card.addSpellAbility(ability);
            
            return card;
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        if(cardName.equals("Rix Maadi, Dungeon Palace")) {
            card.clearSpellKeepManaAbility();
            
            Ability_Tap ability = new Ability_Tap(card, "1 B R") {
                private static final long serialVersionUID = 42470566751344693L;
                
                @Override
                public boolean canPlay() {
                    if(((AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && AllZone.Phase.getActivePlayer() == card.getController()) || (AllZone.Phase.getPhase().equals(
                            Constant.Phase.Main1) && AllZone.Phase.getActivePlayer() == card.getController()))
                            && AllZone.GameAction.isCardInPlay(card)) return true;
                    else return false;
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone hand_c = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
                    PlayerZone hand_h = AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer);
                    CardList hand_comp = new CardList(hand_c.getCards());
                    CardList hand_hum = new CardList(hand_h.getCards());
                    return ((hand_comp.size() - hand_hum.size()) > 1 && hand_hum.size() > 0);
                }
                
                @Override
                public void resolve() {
                    AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
                    AllZone.GameAction.discardRandom(AllZone.ComputerPlayer, this); // wise discard should be here  
                }
            };
            ability.setDescription("tap 1 B R: Each player discards a card. Activate this ability only any time you could cast a sorcery.");
            ability.setStackDescription("Each player discards a card.");
            card.addSpellAbility(ability);
        }
        //*************** END ************ END **************************        
        
        
        
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
                    return ((list.size() > 0) & !card.getType().contains("Creature"));
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
                    return !card.getType().contains("Creature");
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
                    return !card.getType().contains("Creature");
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Khalni Garden")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Plant", "G 0 1 Plant", card, "G",
                            new String[] {"Creature", "Plant"}, 0, 1, new String[] {""});
                }
            };
            ability.setStackDescription("When Khalni Garden enters the battlefield, put a 0/1 green Plant creature token onto the battlefield.");
            
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 6175835326425915833L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            card.clearSpellKeepManaAbility();
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Celestial Colonnade")) {
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "3 W U") {
                @Override
                public boolean canPlayAI() {
                    return !card.hasSickness();
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
					PlayerZone opponentPlayZone = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
			        CardList opponentCreatureList = new CardList(opponentPlayZone.getCards());
			        opponentCreatureList = opponentCreatureList.getType("Creature");
      			  int n = ComputerUtil.getAvailableMana().size() - 1;
      			  if(n > 0) setManaCost(n + "");
                    return (n > 0 && opponentCreatureList.size() == 0);
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
                    return (!card.hasSickness() && !card.getType().contains("Creature"));
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
                    return !card.hasSickness();
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
                    return !card.hasSickness();
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
                    return !card.hasSickness();
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
                    return !card.hasSickness();
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
                    //AllZone.GameAction.gainLife(AllZone.ComputerPlayer, 1);
                	AllZone.ComputerPlayer.gainLife(1);
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
                    //AllZone.GameAction.gainLife(AllZone.ComputerPlayer, 1);
                	AllZone.ComputerPlayer.gainLife(1);
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
        if(cardName.equals("Kjeldoran Outpost")) {
           final Command comesIntoPlay = new Command() {
              private static final long serialVersionUID = 6175830918425915833L;
              final Player player = card.getController();
              public void execute() {
                 PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                 CardList plains = new CardList(play.getCards());
                 plains = plains.getType("Plains");

                 if( player.equals(AllZone.ComputerPlayer)) {
                    if( plains.size() > 0 ) {
                       CardList tappedPlains = new CardList(plains.toArray());
                       tappedPlains = tappedPlains.filter(new CardListFilter() {
                          public boolean addCard(Card c) {
                             return c.isTapped();
                          }
                       });
                       if( tappedPlains.size() > 0 ) {
                          AllZone.GameAction.sacrifice(tappedPlains.get(0));
                       }
                       else {
                          AllZone.GameAction.sacrifice(plains.get(0));
                       }
                       //if any are tapped, sacrifice it
                       //else sacrifice random
                    }
                    else {
                       AllZone.GameAction.sacrifice(card);
                    }
                 }
                 else { //this is the human resolution
                    //this works with correct input
                    //really, what I want is Cancel to sacrifice Kjeldoran Outpost
                    Input target = new Input() {
                       private static final long serialVersionUID = 6653677835621129465L;
                       public void showMessage() {
                          AllZone.Display.showMessage("Kjeldoran Outpost - Select one plains to sacrifice");
                          ButtonUtil.enableOnlyCancel();
                       }
                       public void selectButtonCancel() {
                    	   AllZone.GameAction.sacrifice(card);
                    	   stop();
                       }
                       public void selectCard(Card c, PlayerZone zone) {
                          if(c.isLand() && zone.is(Constant.Zone.Play) && c.getType().contains("Plains")) {
                             AllZone.GameAction.sacrifice(c);
                             stop();
                          }
                       }//selectCard()
                    };//Input
                    AllZone.InputControl.setInput(target);
                 }
              }
           };
           /*
           final Ability_Tap ability2 = new Ability_Tap(card, "1 W") {
              private static final long serialVersionUID = 6987135326425915833L;
              public void resolve() {
                 CardFactoryUtil.makeToken("Soldier", "W 1 1 Soldier", card, "W", new String[] {"Creature", "Soldier"}, 1, 1, new String[] {""});
              }
           };//SpellAbility */

           card.addComesIntoPlayCommand(comesIntoPlay);
           /*
           card.addSpellAbility(ability2);
           ability2.setDescription("1 W, tap: Put a 1/1 white soldier token in play.");
           ability2.setStackDescription("Kjeldoran Outpost - put a 1/1 white soldier token in play");
           ability2.setBeforePayMana(new Input_PayManaCost(ability2));
			*/
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
        	final Ability_Tap ability = new Ability_Tap(card) {
        		private static final long serialVersionUID = -2090435946748184314L;

        		@Override
        		public boolean canPlayAI() {
        			Card c = getCreature();
                    if(c == null) return false;
                    else {
                        setTargetCard(c);
                        return true;
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
            card.addSpellAbility(ability);
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreatureKeyword_NoCost_TapAbility("Flying", ability));
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        if(cardName.equals("Magosi, the Waterveil")) {
        	/*
        	 * Magosi, the Waterveil enters the battlefield tapped.
        	 * Tap: Add Blue to your mana pool.
        	 * Blue, Tap: Put an eon counter on Magosi, the Waterveil. Skip your next turn.
        	 * Tap, Remove an eon counter from Magosi, the Waterveil and return it to its 
        	 * owner's hand: Take an extra turn after this one.
        	 */

        	final Ability_Tap skipTurn = new Ability_Tap(card, "U") {
				private static final long serialVersionUID = -2404286785963486611L;

				@Override
        		public void resolve() {
					Player player = card.getController();
        			card.addCounter(Counters.EON, 1);
        			AllZone.Phase.addExtraTurn(player.getOpponent());                 
        		}
        	};//skipTurn
        	
        	final Ability_Tap extraTurn = new Ability_Tap(card) {
				private static final long serialVersionUID = -2599252144246080154L;

				@Override
        		public void resolve() {
					Player player = card.getController();
        			card.subtractCounter(Counters.EON, 1);
        			AllZone.Phase.addExtraTurn(player);
        			AllZone.GameAction.moveToHand(card);
        		}
				
				@Override
				public boolean canPlay() {
					return card.getCounters(Counters.EON) > 0;
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
        	sbDesc2.append("tap, Remove an eon counter from ").append(card.getName()).append(" and return it to its owner's hand: Take an extra turn after this one.");
        	extraTurn.setDescription(sbDesc2.toString());
        	StringBuilder sb = new StringBuilder();
        	sb.append(card.getName()).append(" - return this card to its owner's hand. Take an extra turn after this one.");
        	extraTurn.setStackDescription(sb.toString());
        	card.addSpellAbility(extraTurn);
        	
        	skipTurn.setBeforePayMana(new Input_PayManaCost(skipTurn));
        }//*************** END ************ END **************************
        
        //*************** START ********** START *************************
        else if(cardName.equals("Elephant Graveyard")) {
        	final String Tgts[] = {"Creature.Elephant"};
        	Target target = new Target("TgtV", "Select target Elephant to Regenerate.", Tgts);

        	final Ability_Cost cost = new Ability_Cost("T", card.getName(), true);
        	
        	final Card[] tgt = new Card[1];
        	final Command untilEOT = new Command() {
				private static final long serialVersionUID = -5392534004045270599L;

				public void execute() {
        			tgt[0].setShield(0);                    
        		}
        	};

        	final SpellAbility ability = new Ability_Activated(card, cost, target) {
				private static final long serialVersionUID = -3783236452506062253L;

				@Override
        		public void resolve() {
        			tgt[0] = this.getTargetCard();

        			tgt[0].addShield();
        			AllZone.EndOfTurn.addUntil(untilEOT);
        		}

        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}

        		@Override
        		public boolean canPlay() {
        			CardList elephants = AllZoneUtil.getTypeInPlay("Elephant");
        			return elephants.size() != 0;
        		}
        	};

        	ability.setDescription("tap: Regenerate target Elephant.");
        	card.addSpellAbility(ability);
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
                	for(int i = 0; i < num[0]; i++) {
                		mana.append(shortString).append(" ");
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
        
        return card;
    }
    

}
