
package forge;


import java.util.ArrayList;
import java.util.Stack;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import com.esotericsoftware.minlog.Log;

public class CardFactory_Creatures {
    
    private static final int hasKeyword(Card c, String k) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith(k)) return i;
        
        return -1;
    }
    
    public static int shouldCycle(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith("Cycling")) return i;
        
        return -1;
    }
    
    public static int shouldTypeCycle(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith("TypeCycling")) return i;
        
        return -1;
    }
    
    public static int shouldTransmute(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith("Transmute")) return i;
        
        return -1;
    }
    
    public static int shouldSoulshift(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith("Soulshift")) return i;
        
        return -1;
    }
    

    public static Card getCard(final Card card, final String cardName, Player owner, CardFactory cf) {
        
        //*************** START *********** START **************************
        if(cardName.equals("Lurking Informant")) {
            Target target = new Target("Select target player", new String[] {"Player"});
        	Ability_Cost abCost = new Ability_Cost("2 T", cardName, true);
            final SpellAbility a1 = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = 1446529067071763245L;
                
                @Override
                public void resolve() {
                    Player player = getTargetPlayer();
                    if(player == null) player = AllZone.HumanPlayer;
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    Card c = lib.get(0);
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                    	if(GameActionUtil.showYesNoDialog(card, "Mill "+c.getName()+"?")) {
                            AllZone.GameAction.moveToGraveyard(c);
                    	}
                    } else {
                        CardList landlist = AllZoneUtil.getPlayerLandsInPlay(AllZone.HumanPlayer);
                        //AI will just land starve or land feed human.  Perhaps this could use overall card ranking
                        if(landlist.size() >= 5 && !c.getType().contains("Land")) {
                        	AllZone.GameAction.moveToGraveyard(c);
                        }
                        else if(landlist.size() < 5 && c.getType().contains("Land")) {
                        	AllZone.GameAction.moveToGraveyard(c);
                        }
                        else if(lib.size() <= 5) {
                        	AllZone.GameAction.moveToGraveyard(c);
                        }
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList libList = AllZoneUtil.getPlayerCardsInLibrary(AllZone.HumanPlayer);
                    return libList.size() > 0;
                }
            };//SpellAbility
            card.addSpellAbility(a1);
            a1.setDescription(abCost+"Look at the top card of target player's library. You may put that card into that player's graveyard.");
            a1.setStackDescription(cardName+" - Look at the top card of target player's library. You may put that card into that player's graveyard.");
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Serra Avenger")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -1148518222979323313L;
                
                @Override
                public boolean canPlay() {
                	Player turn = AllZone.Phase.getPlayerTurn();
                	if (turn.equals(card.getController()) && turn.getTurn() <= 3)
                		return false;
                	
                    return super.canPlay();
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Force of Savagery")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 1603238129819160467L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
                    
                    return list.containsName("Glorious Anthem") || list.containsName("Gaea's Anthem");
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Caller of the Claw")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int stop = countGraveyard();
                    for(int i = 0; i < stop; i++)
                        makeToken();
                }//resolve()
                
                int countGraveyard() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (c.getTurnInZone() == AllZone.Phase.getTurn());
                        }
                    });
                    return list.size();
                }//countGraveyard()
                
                void makeToken() {
                    CardFactoryUtil.makeToken("Bear", "G 2 2 Bear", card.getController(), "G", new String[] {"Creature", "Bear"},
                            2, 2, new String[] {""});
                }//makeToken()
            };//SpellAbility
            
            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 8485080996453793968L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };//Command
            ability.setStackDescription("Caller of the Claw - Put a 2/2 green Bear creature token onto the battlefield for each nontoken creature put into your graveyard from play this turn.");
            card.addComesIntoPlayCommand(comesIntoPlay);
            
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 6946020026681536710L;
                
                @Override
                public boolean canPlayAI() {
                    return super.canPlay();
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Kiki-Jiki, Mirror Breaker")) {
            final CardFactory cfact = cf;
            Ability_Cost abCost = new Ability_Cost("T", cardName, true);
            Target target = new Target("Select target nonlegendary creature you control.", new String[] {"Creature.nonLegendary+YouCtrl"});
            final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = -943706942500499644L;
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getCreature().get(0));
                }
                
                CardList getCreature() {
                    CardList list = AllZoneUtil.getCreaturesInPlay(card.getController());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (!c.getType().contains("Legendary"));
                        }
                    });
                    CardListUtil.sortAttack(list);
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && getTargetCard().getController().equals(card.getController())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        
                    	 int multiplier = AllZoneUtil.getDoublingSeasonMagnitude(card.getController());
                         Card[] crds = new Card[multiplier];
                         
                         for (int i=0;i<multiplier;i++)
                         {
	                        //TODO: Use central copy methods
	                        Card copy;
	                        if(!getTargetCard().isToken()) {
	                            //CardFactory cf = new CardFactory("cards.txt");
	                            
	
	                            //copy creature and put it onto the battlefield
	                            //copy = getCard(getTargetCard(), getTargetCard().getName(), card.getController());
	                            copy = cfact.getCard(getTargetCard().getName(), getTargetCard().getOwner());
	                            
	                            //when copying something stolen:
	                            copy.setController(getTargetCard().getController());
	                            
	                            copy.setToken(true);
	                            copy.setCopiedToken(true);
	                            
	                            copy.addIntrinsicKeyword("Haste");
	                        } else //isToken()
	                        {
	                            Card c = getTargetCard();
	                            
	                            copy = new Card();
	                            
	                            copy.setName(c.getName());
	                            copy.setImageName(c.getImageName());
	                            
	                            copy.setOwner(c.getController());
	                            copy.setController(c.getController());
	                            
	                            copy.setManaCost(c.getManaCost());
	                            copy.setToken(true);
	                            
	                            copy.setType(c.getType());
	                            
	                            copy.setBaseAttack(c.getBaseAttack());
	                            copy.setBaseDefense(c.getBaseDefense());
	                            copy.addIntrinsicKeyword("Haste");
	                        }
	                        
	                        copy.setCurSetCode(getTargetCard().getCurSetCode());
	                        copy.setImageFilename(getTargetCard().getImageFilename());
	                        
                            if(getTargetCard().isFaceDown()) {
                                copy.setIsFaceDown(true);
                                copy.setManaCost("");
                                copy.setBaseAttack(2);
                                copy.setBaseDefense(2);
                                copy.setIntrinsicKeyword(new ArrayList<String>()); //remove all keywords
                                copy.setType(new ArrayList<String>()); //remove all types
                                copy.addType("Creature");
                                copy.clearSpellAbility(); //disallow "morph_up"
                                copy.setCurSetCode("");
                                copy.setImageFilename("morph.jpg");
                            }

	                        
	                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
	                        play.add(copy);
	                        crds[i] = copy;
                    	}
                        

                        //have to do this since getTargetCard() might change
                        //if Kiki-Jiki somehow gets untapped again
                        final Card[] target = new Card[multiplier];
                        for (int i=0;i<multiplier;i++) {
                        	final int index = i;
	                        target[i] = crds[i];
	                        Command atEOT = new Command() {
	                            private static final long serialVersionUID = 7803915905490565557L;
	                            
	                            public void execute() {
	                                //technically your opponent could steal the token
	                                //and the token shouldn't be sacrificed
	                                if(AllZone.GameAction.isCardInPlay(target[index])) AllZone.GameAction.sacrifice(target[index]); //maybe do a setSacrificeAtEOT, but probably not.
	                            }
	                        };//Command
	                        AllZone.EndOfTurn.addAt(atEOT);
                        }
                    }//is card in play?
                }//resolve()
            };//SpellAbility
            
            ability.setStackDescription("Kiki-Jiki - copy card.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Put a token that's a copy of target nonlegendary creature you control onto the battlefield. ");
            sb.append("That token has haste. Sacrifice it at the beginning of the next end step.");
            ability.setDescription(sb.toString());
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if(cardName.equals("Filigree Angel")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int n = countArtifacts();
                    
                    card.getController().gainLife(3 * n, card);
                }
                
                int countArtifacts() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Artifact");
                    return list.size();
                }
                
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -319011246650583681L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getController()).append(" gains 3 life for each artifact he controls");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Venerable Monk") || cardName.equals("Kitchen Finks")
                || cardName.equals("Shu Grain Caravan")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;

                    c.getController().gainLife(2, card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 1832932499373431651L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" gains 2 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Radiant's Dragoons")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
					card.getController().gainLife(5, card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7748429739046909730L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" gains 5 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Shu Soldier-Farmers") || cardName.equals("Staunch Defenders")
                || cardName.equals("Spiritual Guardian") || cardName.equals("Teroh's Faithful")
                || cardName.equals("Jedit's Dragoons") 
                || cardName.equals("Lone Missionary") || cardName.equals("Obstinate Baloth")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().gainLife(4, card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -1537994957313929513L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" gains 4 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Loxodon Hierarch")) {
        	Ability_Cost abCost = new Ability_Cost("G W Sac<1/CARDNAME>", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = 6606519504236074186L;

				@Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    final Card[] c = AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards();
                    
                    for(int i = 0; i < c.length; i++)
                        if(c[i].isCreature()) c[i].addShield();
                    
                    AllZone.EndOfTurn.addUntil(new Command() {
                        private static final long serialVersionUID = 5853778391858472471L;
                        
                        public void execute() {
                            for(int i = 0; i < c.length; i++)
                                c[i].resetShield();
                        }
                    });
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription(abCost+"Regenerate each creature you control.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" regenerate each of ").append(card.getController()).append("'s creatures.");
            ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Gilder Bairn")) {
        	Ability_Cost abCost = new Ability_Cost("2 GU Untap", cardName, true);
        	Target tgt = new Target("Select target permanent.", new String[]{"Permanent"});
            final Ability_Activated a1 = new Ability_Activated(card, abCost, tgt) {
				private static final long serialVersionUID = -1847685865277129366L;

				@Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(c.sumAllCounters() == 0) return;
                    else if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        //zerker clean up:
                        for(Counters c_1:Counters.values())
                            if(c.getCounters(c_1) > 0) c.addCounter(c_1, c.getCounters(c_1));
                    }
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList perms = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
                    perms = perms.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.sumAllCounters() > 0 && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    perms.shuffle();
                    setTargetCard(perms.get(0)); //TODO: improve this.
                }
                
                @Override
                public boolean canPlayAI() {
                	CardList perms = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
                    perms = perms.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.sumAllCounters() > 0 && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    return perms.size() > 0;
                }
            };//SpellAbility
            
            card.addSpellAbility(a1);
            a1.setDescription(abCost+"For each counter on target permanent, put another of those counters on that permanent.");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Order of Whiteclay")) {
            final SpellAbility a1 = new Ability(card, "1 W W") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList creats = new CardList(grave.getCards());
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardUtil.getConvertedManaCost(c.getManaCost()) <= 3;
                        }
                        
                    });
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Choose a creature", creats.toArray());
                        if(o != null) {
                            Card c = (Card) o;
                            grave.remove(c);
                            play.add(c);
                            card.untap();
                        }
                    } else //Computer
                    {
                        Card c = creats.get(0);
                        grave.remove(c);
                        play.add(c);
                        card.untap();
                    }
                    
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList creats = new CardList(grave.getCards());
                    creats = creats.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            return CardUtil.getConvertedManaCost(c.getManaCost()) <= 3;
                        }
                        
                    });
                    if(card.isTapped() && !card.hasSickness() && creats.size() > 0 && super.canPlay()) return true;
                    else return false;
                }
                
                @Override
                public boolean canPlayAI() {
                    return true;
                }
            };//SpellAbility
            a1.makeUntapAbility();
            card.addSpellAbility(a1);
            a1.setDescription("1 W W, Untap:  Return target creature card with converted mana cost 3 or less from your graveyard to the battlefield.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - return target creature card with converted mana cost 3 or less from your graveyard to the battlefield.");
            a1.setStackDescription(sb.toString());
            
            a1.setBeforePayMana(new Input_PayManaCost(a1));
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mudbutton Torchrunner")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(getTargetCard() != null && CardFactoryUtil.canDamage(card, getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(3, card);
                    else getTargetPlayer().addDamage(3, card);
                }
            };
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = 2740098107360213191L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreaturePlayer(
                            ability, true, false));
                    else {
                        CardList list = CardFactoryUtil.AI_getHumanCreature(3, card, true);
                        CardListUtil.sortAttack(list);
                        
                        if(MyRandom.percentTrue(50)) CardListUtil.sortFlying(list);
                        
                        for(int i = 0; i < list.size(); i++)
                            if(2 <= list.get(i).getNetAttack()) ability.setTargetCard(list.get(i));
                        
                        if(ability.getTargetCard() == null) ability.setTargetPlayer(AllZone.HumanPlayer);
                        
                        AllZone.Stack.add(ability);
                    }
                }//execute()
            };//Command
            card.addDestroyCommand(leavesPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Crater Hellion")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay();
                    
                    for(int i = 0; i < creatures.size(); i++) {
                        Card crd = creatures.get(i);
                        if(CardFactoryUtil.canDamage(card, crd) && !crd.equals(card)) crd.addDamage(4, card);
                    }

                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9072052875006010512L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" deals 4 damage to each other creature.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Magma Giant")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay();
                    
                    for(int i = 0; i < creatures.size(); i++) {
                        Card crd = creatures.get(i);
                        if(CardFactoryUtil.canDamage(card, crd)) crd.addDamage(2, card);
                    }
        			AllZone.ComputerPlayer.addDamage(2, card);
        			AllZone.HumanPlayer.addDamage(2, card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9072052875006010499L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" deals 2 damage to each creature and each player.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lava Hounds")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	card.getController().loseLife(4, card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9072152875006010499L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" deals 4 damage to controller.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);  
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Thunder Dragon")) {
        	final SpellAbility ability = new Ability(card, "0") {
        		@Override
        		public void resolve() {

        			CardList all = AllZoneUtil.getCreaturesInPlay();
        			all = all.filter(new CardListFilter()
        			{
        				public boolean addCard(Card c)
        				{
        					return !c.getKeyword().contains("Flying") &&
        					CardFactoryUtil.canDamage(card, c);
        				}
        			});

        			for(int i = 0; i < all.size(); i++)
        				all.get(i).addDamage(3, card);

        		}
        	};
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9072052875006010434L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" deals 3 damage to each creature without flying.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Cloudthresher")) {
        	final SpellAbility ability = new Ability(card, "0") {
        		@Override
        		public void resolve() {

        			CardList all = AllZoneUtil.getCreaturesInPlay();
        			all = all.filter(new CardListFilter()
        			{
        				public boolean addCard(Card c)
        				{
        					return c.getKeyword().contains("Flying") &&
        					CardFactoryUtil.canDamage(card, c);
        				}
        			});

        			for(int i = 0; i < all.size(); i++)
        				all.get(i).addDamage(2, card);

        			AllZone.HumanPlayer.addDamage(2, card);
        			AllZone.ComputerPlayer.addDamage(2, card);

        		}
        	};
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9072052875006010410L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" deals 2 damage to each creature with flying and each player.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
            card.addSpellAbility(new Spell_Evoke(card, "2 G G") {
                private static final long serialVersionUID = 5061298336319833911L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mulldrifter")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().drawCards(2);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9072052875006010497L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getController()).append(" draws 2 cards.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
            card.addSpellAbility(new Spell_Evoke(card, "2 U") {
                private static final long serialVersionUID = 5061298336319833956L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Meadowboon")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList list = AllZoneUtil.getCreaturesInPlay(card.getController());
                    Card c;
                    
                    for(int i = 0; i < list.size(); i++) {
                        c = list.get(i);
                        c.addCounter(Counters.P1P1, 1);
                        
                    }
                }//resolve()
            };
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = -8083212279082607731L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getController()).append(" puts a +1/+1 on each creature he controls.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addLeavesPlayCommand(leavesPlay);
            
            card.addSpellAbility(new Spell_Evoke(card, "3 W") {
                private static final long serialVersionUID = 5001777391157132871L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sengir Autocrat")) {
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = 7242867764317580066L;
                
                public void execute() {
                    CardList all = AllZoneUtil.getTypeInPlay("Serf");
                    for(Card serf:all)
                        AllZone.GameAction.exile(serf);
                }//execute
            };//Command
            card.addLeavesPlayCommand(leavesPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Haunted Angel")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Angel", "B 3 3 Angel",
                            card.getController().getOpponent(), "B", new String[] {
                                    "Creature", "Angel"}, 3, 3, new String[] {"Flying"});
                    
                    //fixed - error if this card is copied like with Kiki, Jiki mirror breaker
                    //null pointer exception
                    
                    if(card.isToken()) return;
                    
                    AllZone.GameAction.exile(card);
                }
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 8044338194100037815L;
                
                public void execute() {
                    Player opponent = card.getController().getOpponent();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(opponent).append(" puts a 3/3 flying token onto the battlefield");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Angel of Mercy") || cardName.equals("Rhox Bodyguard") || cardName.equals("Tireless Missionaries")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().gainLife(3, card);
                }
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 6457889481637587581L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getController()).append(" gains 3 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Rukh Egg")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Bird", "R 4 4 Bird", card.getController(), "R", new String[] {"Creature", "Bird"},
                            4, 4, new String[] {"Flying"});
                }
            }; //ability
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - Put a 4/4 red Bird creature token with flying onto the battlefield.");
            ability.setStackDescription(sb.toString());
            
            final Command createBird = new Command() {
                private static final long serialVersionUID = 2856638426932227407L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            
            final Command destroy = new Command() {
                private static final long serialVersionUID = 2320128493809478823L;
                
                public void execute() {
                    AllZone.EndOfTurn.addAt(createBird);
                }
            };
            
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Primal Plasma") || cardName.equals("Primal Clay")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    String choice = "";
                    String choices[] = {"3/3", "2/2 with flying", "1/6 with defender"};
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        choice = AllZone.Display.getChoice("Choose one", choices);
                    } else choice = choices[MyRandom.random.nextInt(3)];
                    
                    if(choice.equals("2/2 with flying")) {
                        card.setBaseAttack(2);
                        card.setBaseDefense(2);
                        card.addIntrinsicKeyword("Flying");
                    }
                    if(choice.equals("1/6 with defender")) {
                        card.setBaseAttack(1);
                        card.setBaseDefense(6);
                        card.addIntrinsicKeyword("Defender");
                    }
                    
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 8957338395786245312L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - choose: 3/3, 2/2 flying, 1/6 defender");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Phyrexian Gargantua")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().loseLife(2,card);
                    
                    card.getController().drawCards(2);
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -3016651104325305186L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Phyrexian Gargantua - ").append(card.getController());
                	sb.append(" draws 2 cards and loses 2 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Phyrexian Rager")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().loseLife(1,card);
                    
                    card.getController().drawCard();
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -4808204528319094292L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Phyrexian Rager - ").append(card.getController());
                	sb.append(" draws 1 card and loses 1 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END ************************** 
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cao Ren, Wei Commander")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().loseLife(3,card);
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = -6954568998599730697L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Cao Ren, Wei Commander - ").append(card.getController());
                	sb.append(" loses 3 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END ************************** 
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Brawn")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {}
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -3009968608543593584L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getOwner()).append(" creatures have Trample.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Filth")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {}
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -3009968608543593584L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getOwner());
                	sb.append(" creatures have Swampwalk.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Glory")) {
            final Ability ability = new Ability(card, "2 W") {
				private static final long serialVersionUID = -79984345642451L;
				
				@Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
				
				public Card getAttacker() {
                    // target creatures that is going to attack
                    Combat c = ComputerUtil.getAttackers();
                    Card[] att = c.getAttackers();

                    // Effect best used on at least a couple creatures
                    if (att.length > 1) {
                        return att[0];
                    } else return null;
                }//getAttacker()
				
				String getKeywordBoost() {
					String theColor = getChosenColor();
					return "Protection from " + theColor;
                }//getKeywordBoost()
				
				String getChosenColor() {
					// Choose color for protection in Brave the Elements
					String color = "";
					if (card.getController().equals(AllZone.HumanPlayer)) {

						String[] colors = Constant.Color.Colors;
						colors[colors.length-1] = null;

						Object o = AllZone.Display.getChoice("Choose color", colors);
						color = (String)o;
					}
					else {
						PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer);
						CardList list = new CardList();
						list.addAll(lib.getCards());
						list.addAll(hand.getCards());

						if (list.size() > 0) {  
							String mpcolor = CardFactoryUtil.getMostProminentColor(list);
							if (!mpcolor.equals(""))
								color = mpcolor;
							else
								color = "black";
						}
						else  {
							color = "black";
						}
					}
					return color;
				} // getChosenColor
				
				@Override
				public void resolve() {
					final String kboost = getKeywordBoost();
					
					CardList list = new CardList();
					PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    list.addAll(play.getCards());
                    
                    for (int i = 0; i < list.size(); i++) {
                        final Card[] target = new Card[1];
                        target[0] = list.get(i);
                        
                        final Command untilEOT = new Command() {
							private static final long serialVersionUID = 6308754740309909072L;

							public void execute() {
                                if (AllZone.GameAction.isCardInPlay(target[0])) {
                                	target[0].removeExtrinsicKeyword(kboost);
                                }
                            }
                        };//Command
                        
                        if (AllZone.GameAction.isCardInPlay(target[0]) && 
                        		!target[0].getKeyword().contains(kboost)) {
                            target[0].addExtrinsicKeyword(kboost);
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }//if
                    }//for
				}//resolve
                
                @Override
                public boolean canPlay() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    
                    return AllZone.GameAction.isCardInZone(card, grave);                   
                }
            };//Ability

            card.addSpellAbility(ability);
            ability.setFlashBackAbility(true);
            card.setUnearth(true);
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("2 W: Creatures you control gain protection from the color of your choice ");
            sbDesc.append("until end of turn. Activate this ability only if Glory is in your graveyard.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card.getName()).append(" - Creatures ").append(card.getController());
            sbStack.append(" controls gain protection from the color of his/her choice until end of turn");
            ability.setStackDescription(sbStack.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Anger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {}
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 1707519783018941582L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getOwner());
                	sb.append(" creatures have Haste.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Valor")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {}
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -3009968608543593584L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getOwner());
                	sb.append(" creatures have First Strike.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Wonder")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {}
            };
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 340877499423908818L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getOwner());
                	sb.append(" creatures have Flying.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Kokusho, the Evening Star")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Player opponent = card.getController().getOpponent();
                    
                    opponent.loseLife(5, card);
                    card.getController().gainLife(5, card);
                }
            };
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -2648843419728951661L;
                
                public void execute() {
                	Player opponent = card.getController().getOpponent();
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append("Kokusho, the Evening Star - ").append(opponent).append(" loses 5 life and ");
                	sb.append(card.getController()).append(" gains 5 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Symbiotic Elf")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    makeToken();
                    makeToken();
                }
                
                void makeToken() {
                    CardFactoryUtil.makeToken("Insect", "G 1 1 Insect", card.getController(), "G", new String[] {
                            "Creature", "Insect"}, 1, 1, new String[] {""});
                }//makeToken()
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -7121390569051656027L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Symbiotic Elf - ").append(card.getController()).append(" puts two 1/1 tokens onto the battlefield");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mongrel Pack")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    makeToken();
                    makeToken();
                    makeToken();
                    makeToken();
                }
                
                void makeToken() {
                    CardFactoryUtil.makeToken("Hound", "G 1 1 Hound", card.getController(), "G", new String[] {
                            "Creature", "Hound"}, 1, 1, new String[] {""});
                }//makeToken()
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -7121390569051656127L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Mongrel Pack - ").append(card.getController()).append(" puts four 1/1 tokens onto the battlefield.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Chittering Rats")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final Player opponent = card.getController().getOpponent();
                    final PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
                    
                    if(hand.size() == 0) return;
                    
                    if(opponent.isComputer()) {
                    	//randomly move card from hand to top of library
                    	int index = MyRandom.random.nextInt(hand.size());
                    	Card c = hand.get(index);
                    	AllZone.GameAction.moveToTopOfLibrary(c);
                    }
                    else {
                    	if(hand.size() > 0) {
                    		AllZone.InputControl.setInput(new Input() {
								private static final long serialVersionUID = 2358951895134165788L;

								@Override
								public void showMessage() {
                    				AllZone.Display.showMessage("Select a card from your hand to put on top of your library.");
                    				ButtonUtil.disableAll();
                    			}
                    			
								@Override
                    			public void selectCard(Card c, PlayerZone p) {
                    				if(p.is(Constant.Zone.Hand, opponent)) {
                    					AllZone.GameAction.moveToTopOfLibrary(c);
                    					stop();
                    				}
                    			}
                    		});
                    	}
                    }
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 160195797163952303L;
                
                public void execute() {
                    ability.setStackDescription(cardName+" - opponent puts a card from his or her hand on top of his or her library.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Kemuri-Onna")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    Player opponent = card.getController().getOpponent();
                    opponent.discard(this);
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -6451753440468941341L;
                
                public void execute() {
                    Player opponent = card.getController().getOpponent();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(opponent).append(" discards a card");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
  
        
        //*************** START *********** START **************************
        else if(cardName.equals("Oracle of Mul Daya")) {
            final SpellAbility ability = new Ability(card, "0") {
                private static final long serialVersionUID = 2902408812353813L;
                
                @Override
                public void resolve() {
                    CardList library = new CardList(AllZone.getZone(Constant.Zone.Library, card.getController()).getCards());
                    Card top = library.get(0);
                    
                    if(library.size() > 0 && top.getType().contains("Land") ) {
                    	boolean canPlayLand = false;
                    	boolean isHuman = false;
                    	if(card.getController() == AllZone.HumanPlayer){
                    		canPlayLand = CardFactoryUtil.canHumanPlayLand();
                    		isHuman = true;
                    	}
                        else{
                        	canPlayLand = CardFactoryUtil.canComputerPlayLand();
                        }
                    	if (canPlayLand){
                    		 //todo(sol): would prefer to use GameAction.playLand(top, play) but it doesn't work
	                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
	                        Card land = AllZone.GameAction.moveTo(play, top);
	                        CardFactoryUtil.playLandEffects(land);
	                        if (isHuman)
	                        	AllZone.GameInfo.incrementHumanPlayedLands();
	                        else
	                        	AllZone.GameInfo.incrementComputerPlayedLands();
                    	}
                    }
                }//resolve()
                
                @Override
                public boolean canPlay() { 
                    CardList library = new CardList(AllZone.getZone(Constant.Zone.Library, card.getController()).getCards());
                    if(library.size() == 0) return false;
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());                                      
                    boolean canPlayLand = false;
                        if(card.getController() == AllZone.HumanPlayer) canPlayLand = CardFactoryUtil.canHumanPlayLand();
                        else canPlayLand = CardFactoryUtil.canComputerPlayLand();
                        
                    return (AllZone.GameAction.isCardInZone(card, play) && library.get(0).getType().contains("Land") && canPlayLand);
                }
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" - plays land from top of library.");
            ability.setStackDescription(sb.toString());
            card.addSpellAbility(ability);           
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Highway Robber") || cardName.equals("Dakmor Ghoul")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    Player opponent = card.getController().getOpponent();
                    opponent.loseLife(2, card);
                    
                    card.getController().gainLife(2, card);
                }
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 321989007584083996L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getController());
                	sb.append(" gains 2 life and opponent loses 2 life.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Serpent Warrior")) {
            SpellAbility summoningSpell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 1937549779526559727L;
                
                @Override
                public boolean canPlayAI() {
                    return AllZone.ComputerPlayer.getLife() > 3;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(summoningSpell);
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    
                    card.getController().loseLife(3, card);
                }
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 2334517567512130479L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Serpent Warrior - ").append(card.getController()).append(" loses 3 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Eviscerator")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().loseLife(5,card);
                }
                
                @Override
                public boolean canPlayAI() {
                    return 8 < AllZone.ComputerPlayer.getLife();
                }
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -221296021551561668L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Eviscerator - ").append(card.getController()).append(" loses 5 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            card.clearSpellAbility();
            
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 7053381164164384390L;
                
                @Override
                public boolean canPlayAI() {
                    return 8 <= AllZone.ComputerPlayer.getLife();
                }
            });
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Foul Imp")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    card.getController().loseLife(2, card);
                }
                
                @Override
                public boolean canPlayAI() {
                    return 4 < AllZone.ComputerPlayer.getLife();
                }
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -5371716833341661084L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Foul Imp - ").append(card.getController()).append(" loses 2 life");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Drekavac")) {
            final Input discard = new Input() {
                private static final long serialVersionUID = -6392468000100283596L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select a noncreature card to discard");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(zone.is(Constant.Zone.Hand) && !c.isCreature()) {
                        c.getController().discard(c, null);
                        stop();
                    }
                }
                
                @Override
                public void selectButtonCancel() {
                    AllZone.GameAction.sacrifice(card);
                    stop();
                }
            };//Input
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        if(AllZone.Human_Hand.getCards().length == 0) AllZone.GameAction.sacrifice(card);
                        else AllZone.InputControl.setInput(discard);
                    } else {
                        CardList list = new CardList(AllZone.Computer_Hand.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return (!c.isCreature());
                            }
                        });
                        list.get(0).getController().discard(list.get(0), this);
                    }//else
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9202753910259054021L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" sacrifices Drekavac unless he discards a noncreature card");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -2940969025405788931L;
                
                //could never get the AI to work correctly
                //it always played the same card 2 or 3 times
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(hand.getCards());
                    list.remove(card);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
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
        else if(cardName.equals("Minotaur Explorer") || cardName.equals("Balduvian Horde") ||
        		cardName.equals("Pillaging Horde")) {

        	final SpellAbility creature = new Spell_Permanent(card){
				private static final long serialVersionUID = -7326018877172328480L;

				@Override
				public boolean canPlayAI(){
        			int reqHand = 1;
        			if (AllZone.getZone(card).is(Constant.Zone.Hand))
        				reqHand++;
        			
        			// Don't play if it would sacrifice as soon as it comes into play
        			return AllZoneUtil.getCardsInZone(Constant.Zone.Hand, AllZone.ComputerPlayer).size() > reqHand;
        		}
        	};
        	card.clearFirstSpellAbility();
        	card.addFirstSpellAbility(creature);
        	
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    if(hand.getCards().length == 0) 
                    	AllZone.GameAction.sacrifice(card);
                    else 
                    	card.getController().discardRandom(this);
                }
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4986114285467649619L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" - discards at random or sacrifices ").append(cardName);
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Goretusk Firebeast")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	Player opponent = card.getController().getOpponent();
                	opponent.addDamage(4, card);
                }
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 2977308349468915040L;
                
                public void execute() {
                	Player opponent = card.getController().getOpponent();
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append("Goretusk Firebeast - deals 4 damage to ").append(opponent);
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Skirk Prospector")) {
            final Ability_Mana ability = new Ability_Mana(card, "Sacrifice a Goblin: Add R") {
				private static final long serialVersionUID = -6764282980691397966L;

				@Override
                public boolean canPlayAI() {
					return false;
                }
               
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                   
                    if(c != null && c.isCreature() ) {
                    	AllZone.GameAction.sacrifice(c);
                    	super.resolve();
                    }
                }
                
                @Override
				public String mana() {
					return "R";
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

                    choice = choice.getType("Goblin");
                   
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice,
                            "Sacrifice a Goblin:", true, false));
                }
            };

            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Sylvan Messenger") 
        		|| cardName.equals("Enlistment Officer") 
        		|| cardName.equals("Tidal Courier")
        		|| cardName.equals("Goblin Ringleader")
        		|| cardName.equals("Grave Defiler")) {
        	
        	final String[] typeToGet = {""};
            if(card.getName().equals("Sylvan Messenger"))
            {
            	typeToGet[0] = "Elf";
            }
            else if(card.getName().equals("Enlistment Officer"))
            {
            	typeToGet[0] = "Soldier";
            }
            else if(card.getName().equals("Tidal Courier"))
            {
            	typeToGet[0] = "Merfolk";
            }
            else if(card.getName().equals("Grave Defiler"))
            {
            	typeToGet[0] = "Zombie";
            }
            else if(card.getName().equals("Goblin Ringleader"))
            {
            	typeToGet[0] = "Goblin";
            }
        	
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone libraryZone = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    
                    //get top 4 cards of the library
                    CardList top = new CardList();
                    Card[] library = libraryZone.getCards();
                    for(int i = 0; i < 4 && i < library.length; i++)
                        top.add(library[i]);
                    
                    //put top 4 cards on bottom of library
                    for(int i = 0; i < top.size(); i++) {
                        libraryZone.remove(top.get(i));
                        libraryZone.add(top.get(i));
                    }
                    
                    CardList typeLimitedTop = top.getType(typeToGet[0]);
                    
                    for(int i = 0; i < typeLimitedTop.size(); i++)
                        AllZone.GameAction.moveTo(hand, typeLimitedTop.get(i));
                    
                    if (card.getController().equals(AllZone.ComputerPlayer))
                    {
                    	StringBuilder sb = new StringBuilder();
                    	sb.append("<html><b>");
                    	for (Card c:top) {
                    		sb.append(c.getName());
                    		sb.append("<br>");
                    	}
                    	sb.append("</b></html>");
                    	JOptionPane.showMessageDialog(null, sb.toString(), "Computer reveals:", JOptionPane.INFORMATION_MESSAGE); 
                    }
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4757054648163014149L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - reveal the top four cards of your library. Put all ").append(typeToGet[0]);
            sb.append(" cards revealed this way into your hand and the rest on the bottom of your library.");
            ability.setStackDescription(sb.toString());
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Child of Alara")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    CardList list = AllZoneUtil.getCardsInPlay();
                    
                    for(int i = 0; i < list.size(); i++)
                        if(!list.get(i).getType().contains("Land")) {
                            Card c = list.get(i);
                            AllZone.GameAction.destroyNoRegeneration(c);
                        }
                }
            };
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -2937565366066183385L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            ability.setStackDescription("Child of Alara - Destroy all nonland permanents, they can't be regenerated");
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Ryusei, the Falling Star")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    for(int i = 0; i < list.size(); i++)
                        if(!list.get(i).getKeyword().contains("Flying")
                                && CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(5, card);
                }
            };
            Command destroy = new Command() {
                private static final long serialVersionUID = -6585074939675844265L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            ability.setStackDescription("Ryusei, the Falling Star - deals 5 damage to each creature without flying");
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sleeper Agent")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                    
                    PlayerZone from = AllZone.getZone(card);
                    from.remove(card);
                    
                    card.setController(card.getOwner().getOpponent());
                    
                    PlayerZone to = AllZone.getZone(Constant.Zone.Battlefield,
                            card.getOwner().getOpponent());
                    to.add(card);
                    Log.debug("Sleeper Agent", "cards controller = " + card.getController());
                    
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                }
            };
            
            ability.setStackDescription("When Sleeper Agent enters the battlefield, target opponent gains control of it.");
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -3934471871041458847L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                    
                }//execute()
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Flametongue Kavu")) {
            final CommandReturn getCreature = new CommandReturn() {
                //get target card, may be null
                public Object execute() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(4, card, true);
                    CardListUtil.sortAttack(list);
                    CardListUtil.sortFlying(list);
                    
                    if(list.size() != 0) {
                        Card c = list.get(0);
                        if(3 <= c.getNetAttack() || (2 <= c.getNetAttack() && c.getKeyword().contains("Flying"))) return c;
                    }
                    if((AllZone.ComputerPlayer.getLife() < 10)
                            && (CardFactoryUtil.AI_getHumanCreature(card, true).size() != 0)) {
                        list = CardFactoryUtil.AI_getHumanCreature(card, true);
                        CardListUtil.sortAttack(list);
                        CardListUtil.sortFlying(list);
                        
                        return list.get(0);
                    }
                    return null;
                }//execute()
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canDamage(card, getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	getTargetCard().addDamage(4, card);
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -1920425335456952853L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
                        ButtonUtil.disableAll();
                    } else//computer
                    {
                        Object o = getCreature.execute();
                        if(o != null)//should never happen, but just in case
                        {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        } else {
                            ability.setTargetCard(card);
                            AllZone.Stack.add(ability);
                        }
                    }//else
                }//execute()
            };
            card.addComesIntoPlayCommand(intoPlay);
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 5741146386242415357L;
                
                @Override
                public boolean canPlayAI() {
                    Object o = getCreature.execute();
                    
                    return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Skinrender")) {

            final CommandReturn getCreature = new CommandReturn() {
                //get target card, may be null
                public Object execute() {
                    CardList l = CardFactoryUtil.AI_getHumanCreature(card, true);
                                        
                    CardList list = new CardList(l.toArray());                  
                    if (list.isEmpty())	// todo: if human doesn't have a valid creature must kill own valid target
                    	return null;
                    
                    // Sorts: Highest Attacking Flyer at the top. 
                    CardListUtil.sortAttack(list);
                    CardListUtil.sortFlying(list);
                    
                    Card target = list.get(0);
                    // if "Best creature has 2+ Attack and flying target that. 
                    if(2 <= target.getNetAttack() && target.getKeyword().contains("Flying")) 
                    	return target;
                    
                    if(MyRandom.percentTrue(50))
                    	CardListUtil.sortAttack(list);

                    return target;
                }//execute()
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                    	c.addCounter(Counters.M1M1,3);
                    }
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {

				private static final long serialVersionUID = 8876482925803330585L;

				public void execute() {
                    Input target = new Input() {
                    	
						private static final long serialVersionUID = -2760098744343748530L;

						@Override
                        public void showMessage() {
                            AllZone.Display.showMessage("Select target creature");
                            ButtonUtil.disableAll();
                        }
                        
                        @Override
                        public void selectCard(Card card, PlayerZone zone) {
                            if(!CardFactoryUtil.canTarget(ability, card)) {
                                AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                            } else if(card.isCreature() && zone.is(Constant.Zone.Battlefield)) {
                                ability.setTargetCard(card);
                                AllZone.Stack.add(ability);
                                stop();
                            }
                        }
                    };//Input target
                    

                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        //get all creatures
                    	CardList creatures = AllZoneUtil.getTypeInPlay("Creature");
                        creatures = creatures.filter(new CardListFilter(){
                        	public boolean addCard(Card c)
                        	{
                        		return CardFactoryUtil.canTarget(card, c);
                        	}
                        });
                    	
                        if(creatures.size() != 0) AllZone.InputControl.setInput(target);

                    } 
                    else{ //computer
                        Object o = getCreature.execute();
                        if(o != null)//should never happen, but just in case
                        {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    }//else
                }//execute()
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
               
        
        //*************** START *********** START **************************
        else if(cardName.equals("Phylactery Lich") ) {
            final CommandReturn getArt = new CommandReturn() {
                //get target card, may be null
                public Object execute() {
                    CardList art = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
                    art = art.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact();
                        }
                    });
                    
                    CardList list = new CardList(art.toArray());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getIntrinsicKeyword().contains("Indestructible");
                        }
                    });

                    Card target = null;
                    if(!list.isEmpty())
                    	target = list.get(0);
                    else if (!art.isEmpty())
                    	target = art.get(0);

                    return target;
                }//execute()
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && c.isArtifact()) {
                    	c.addCounter(Counters.PHYLACTERY, 1);
                    	card.setFinishedEnteringBF(true);
                    }
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -1601957445498569156L;

				public void execute() {
                    Input target = new Input() {

						private static final long serialVersionUID = -806140334868210520L;

						@Override
                        public void showMessage() {
                            AllZone.Display.showMessage("Select target artifact you control");
                            ButtonUtil.disableAll();
                        }
                        
                        @Override
                        public void selectCard(Card card, PlayerZone zone) {
                            if(card.isArtifact() && zone.is(Constant.Zone.Battlefield) && card.getController().equals(AllZone.HumanPlayer)) {
                                ability.setTargetCard(card);
                                AllZone.Stack.add(ability);
                                stop();
                            }
                        }
                    };//Input target
                    

                    if(card.getController().equals(AllZone.HumanPlayer)) {
                    	CardList artifacts = AllZoneUtil.getPlayerTypeInPlay(AllZone.HumanPlayer, "Artifact");
                        
                        if(artifacts.size() != 0) AllZone.InputControl.setInput(target);

                    } 
                    else{ //computer
                        Object o = getArt.execute();
                        if(o != null)//should never happen, but just in case
                        {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    }//else
                }//execute()
            };
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                
				private static final long serialVersionUID = -1506199222879057809L;

				@Override
                public boolean canPlayAI() {
                    Object o = getArt.execute();
                    return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Briarhorn")) {
            final CommandReturn getCreature = new CommandReturn() {
                //get target card, may be null
                public Object execute() {
                    Combat combat = ComputerUtil.getAttackers();
                    Card[] c = combat.getAttackers();
                    
                    if(c.length == 0) {
                        CardList list = new CardList();
                        list.addAll(AllZone.Computer_Battlefield.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return (c.isCreature() && !c.hasSickness());
                            }
                        });
                        
                        if(list.size() == 0) return card;
                        else {
                            CardListUtil.sortAttack(list);
                            CardListUtil.sortFlying(list);
                            
                            for(int i = 0; i < list.size(); i++)
                                if(list.get(i).isUntapped()) return list.get(i);
                            
                            return list.get(0);
                        }
                        
                    }
                    
                    return c[0];
                }//execute()
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addTempAttackBoost(3);
                        c.addTempDefenseBoost(3);
                        
                        AllZone.EndOfTurn.addUntil(new Command() {
                            private static final long serialVersionUID = -5417966443737481535L;
                            
                            public void execute() {
                                c.addTempAttackBoost(-3);
                                c.addTempDefenseBoost(-3);
                            }
                        });
                    }//if
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -5497111036332352337L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
                    } else//computer
                    {
                        Object o = getCreature.execute();
                        if(o != null)//should never happen, but just in case
                        {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    }//else
                }//execute()
            };
            card.addComesIntoPlayCommand(intoPlay);
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -681505091538444209L;
                
                @Override
                public boolean canPlayAI() {
                    Object o = getCreature.execute();
                    
                    return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
            
            card.addSpellAbility(new Spell_Evoke(card, "1 G") {
                private static final long serialVersionUID = 8565746177492779899L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                //because this card has Flash
                @Override
                public boolean canPlay() {
                    return AllZone.GameAction.isCardInZone(card, AllZone.Human_Hand)
                            || AllZone.GameAction.isCardInZone(card, AllZone.Computer_Hand);
                }
            });
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Inner-Flame Acolyte") || cardName.equals("Vulshok Heartstoker")) {
            final CommandReturn getCreature = new CommandReturn() {
                //get target card, may be null
                public Object execute() {
                    Combat combat = ComputerUtil.getAttackers();
                    Card[] c = combat.getAttackers();
                    CardList list = new CardList();
                    
                    if(c.length == 0) {
                        list.addAll(AllZone.Computer_Battlefield.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isCreature();
                            }
                        });
                        
                        if(list.size() == 0) return card;
                        else {
                            CardListUtil.sortAttack(list);
                            CardListUtil.sortFlying(list);
                            
                            for(int i = 0; i < list.size(); i++)
                                if(list.get(i).isUntapped()) return list.get(i);
                            
                            return list.get(0);
                        }
                    }
                    
                    return c[0];
                }//execute()
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addTempAttackBoost(2);
                        if (card.getName().equals("Inner-Flame Acolyte")) {
                            c.addExtrinsicKeyword("Haste");
                        }
                        
                        AllZone.EndOfTurn.addUntil(new Command() {
                            private static final long serialVersionUID = -6478147896119509688L;
                            
                            public void execute() {
                                c.addTempAttackBoost(-2);
                                if (card.getName().equals("Inner-Flame Acolyte")) {
                                    c.removeExtrinsicKeyword("Haste");
                                }
                            }
                        });
                    }//if
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -4514610171270596654L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
                    } else//computer
                    {
                        Object o = getCreature.execute();
                        if(o != null)//should never happen, but just in case
                        {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    }//else
                }//execute()
            };
            card.addComesIntoPlayCommand(intoPlay);
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 7153795935713327863L;
                
                @Override
                public boolean canPlayAI() {
                    Object o = getCreature.execute();
                    
                    return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
            if (card.getName().equals("Inner-Flame Acolyte")) {
                card.addSpellAbility(new Spell_Evoke(card, "R") {
                    private static final long serialVersionUID = 8173305091293824506L;
                    
                    @Override
                    public boolean canPlayAI() {
                        return false;
                    }
                });
            }
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Slaughterhouse Bouncer")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canDamage(card, getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(3, card);
                }
            };
            Command destroy = new Command() {
                private static final long serialVersionUID = 1619442728548153928L;
                
                public void execute() {
                    //check to see if any other creatures in play
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    
                    //check to see if any cards in hand
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    if(hand.getCards().length == 0 && list.size() != 0) {
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
                            ButtonUtil.disableAll();
                        } else//computer
                        {
                            //1.try to get human creature with defense of 3
                            list = CardFactoryUtil.AI_getHumanCreature(card, true);
                            list = list.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.getNetDefense() == 3;
                                }
                            });
                            //2.try to get human creature with defense of 2 or less
                            if(list.isEmpty()) list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
                            //3.get any computer creature
                            if(list.isEmpty()) {
                                list = new CardList(AllZone.Computer_Battlefield.getCards());
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
        else if(cardName.equals("Undying Beast")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.setDamage(0);
                    card.clearAssignedDamage();
                    card.untap();
                    
                    //moves card to top of library
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getOwner());
                    library.add(card, 0);
                }
            };
            Command destroy = new Command() {
                private static final long serialVersionUID = -318081458847722674L;
                
                public void execute() {
                    if(card.isToken()) return;
                    
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
        else if(cardName.equals("Fire Imp") || cardName.equals("Corrupt Eunuchs")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canDamage(card, getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(2, card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7639628386947162984L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
                        ButtonUtil.disableAll();
                    } else//computer
                    {
                        CardList list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
                        CardListUtil.sortAttack(list);
                        CardListUtil.sortFlying(list);
                        
                        if(list.isEmpty()) {
                            list = CardFactoryUtil.AI_getHumanCreature(card, true);
                            list.shuffle();
                        }
                        
                        if(list.size() > 0) ability.setTargetCard(list.get(0));
                        else ability.setTargetCard(card);
                        
                        AllZone.Stack.add(ability);
                    }//else
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 1731831041621831246L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
                    
                    return (list.size() > 0) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Keening Banshee")) {
            
            final SpellAbility ability = new Ability(card, "0") {
                
                @Override
                public void resolve() {
                    final Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addTempAttackBoost(-2);
                        c.addTempDefenseBoost(-2);
                        
                        AllZone.EndOfTurn.addUntil(new Command() {
                            private static final long serialVersionUID = 8479364459667467780L;
                            
                            public void execute() {
                                c.addTempAttackBoost(2);
                                c.addTempDefenseBoost(2);
                            }
                        });
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 6283666887577455663L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
                        ButtonUtil.disableAll();
                    } else//computer
                    {
                        CardList list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
                        CardListUtil.sortAttack(list);
                        CardListUtil.sortFlying(list);
                        
                        if(list.isEmpty()) {
                            list = CardFactoryUtil.AI_getHumanCreature(card, true);
                            list.shuffle();
                        }
                        
                        ability.setTargetCard(list.get(0));
                        AllZone.Stack.add(ability);
                    }//else
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -1893090545602255371L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
                    
                    return (list.size() > 0) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
            
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Sun Titan")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                        grave.remove(getTargetCard());
                        play.add(getTargetCard());
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                                
				private static final long serialVersionUID = 6483805330273377116L;

				public void execute() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList graveList = new CardList(grave.getCards());
                    graveList = graveList.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card crd)
                    	{
                    		return crd.isPermanent() && CardUtil.getConvertedManaCost(crd.getManaCost()) <=3;
                    	}
                    });
                    
                    if(graveList.size() == 0) return;
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", graveList.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else//computer
                    {
                        Card best = CardFactoryUtil.AI_getBestCreature(graveList);
                        
                        if(best == null) {
                        	graveList.shuffle();
                            best = graveList.get(0);
                        }
                        ability.setTargetCard(best);
                        AllZone.Stack.add(ability);
                    }
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Penumbra Kavu")) {
            final Ability ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Kavu", "B 3 3 Kavu", card.getController(), "B", new String[] {"Creature", "Kavu"},
                            3, 3, new String[] {""});
                }//resolve()
            };//Ability
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 1281791927604583468L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" puts a 3/3 creature onto the battlefield from Penumbra Kavu");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Penumbra Bobcat")) {
            final Ability ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Cat", "B 2 1 Cat", card.getController(), "B", new String[] {"Creature", "Cat"}, 2,
                            1, new String[] {""});
                }//resolve()
            };//Ability
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -8057009255325020247L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" puts a 2/1 creature onto the battlefield from Penumbra Bobcat");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Penumbra Spider")) {
            final Ability ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Spider", "B 2 4 Spider", card.getController(), "B", new String[] {
                            "Creature", "Spider"}, 2, 4, new String[] {"Reach"});
                }//resolve()
            };//Ability
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 9186718803540678064L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController());
                	sb.append(" puts a 2/4 Black Spider creature onto the battlefield from Penumbra Spider");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Penumbra Wurm")) {
            final Ability ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Wurm", "B 6 6 Wurm", card.getController(), "B", new String[] {"Creature", "Wurm"},
                            6, 6, new String[] {"Trample"});
                }//resolve()
            };//Ability
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -8819664543962631239L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController());
                	sb.append(" puts a 6/6 Black Wurm creature with trample onto the battlefield from Penumbra Wurm");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Aven Fisher") || cardName.equals("Riptide Crab")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                	card.getController().drawCard();
                }
            };
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -2786138225183288814L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(card.getController()).append(" draws a card");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Peregrine Drake")) {
            final Input untap = new Input() {
                private static final long serialVersionUID = 2287264826189281795L;
                
                int                       stop             = 5;
                int                       count            = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select a land to untap");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(card.isLand() && zone.is(Constant.Zone.Battlefield)) {
                        card.untap();
                        count++;
                        if(count == stop) stop();
                    }
                }//selectCard()
            };
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(untap);
                    else {
                        CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isLand() && c.isTapped();
                            }
                        });
                        for(int i = 0; i < 5 && i < list.size(); i++)
                            list.get(i).untap();
                    }//else
                }//resolve()
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 3208277692165539396L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" untaps up to 5 lands.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Great Whale")) {
            final Input untap = new Input() {
                private static final long serialVersionUID = -2167059018040912025L;
                
                int                       stop             = 7;
                int                       count            = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select a land to untap");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(card.isLand() && zone.is(Constant.Zone.Battlefield)) {
                        card.untap();
                        count++;
                        if(count == stop) stop();
                    }
                }//selectCard()
            };
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(untap);
                    else {
                        CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isLand() && c.isTapped();
                            }
                        });
                        for(int i = 0; i < 7 && i < list.size(); i++) {
                            list.get(i).untap();
                        }
                    }//else
                }//resolve()
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7222997838266323277L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" untaps up to 7 lands.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END ***************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Palinchron")) {
            final Input untap = new Input() {
                private static final long serialVersionUID = -2167159918040912025L;
                
                int                       stop             = 7;
                int                       count            = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select a land to untap");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(card.isLand() && zone.is(Constant.Zone.Battlefield)) {
                        card.untap();
                        count++;
                        if(count == stop) stop();
                    }
                }//selectCard()
            };
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(untap);
                    else {
                        CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isLand() && c.isTapped();
                            }
                        });
                        for(int i = 0; i < 7 && i < list.size(); i++) {
                            list.get(i).untap();
                        }
                    }//else
                }//resolve()
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7222997848166323277L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" untaps up to 7 lands.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
            final SpellAbility a1 = new Ability(card, "2 U U") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    /*
                    AllZone.getZone(card).remove(card);
                    hand.add(card);
                    */
                    if(card.isToken()) AllZone.getZone(card).remove(card);
                    else AllZone.GameAction.moveTo(hand, card);
                    
                }
            };//a1
            
            card.addSpellAbility(a1);
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" returns Palinchron back to its owner's hand.");
            a1.setStackDescription(sb.toString());
            
            a1.setDescription("2 U U: Return Palinchron to its owner's hand.");
        }//*************** END ************ END ***************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cloud of Faeries")) {
            final Input untap = new Input() {
                private static final long serialVersionUID = -2167059918040912025L;
                
                int                       stop             = 2;
                int                       count            = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select a land to untap");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(card.isLand() && zone.is(Constant.Zone.Battlefield)) {
                        card.untap();
                        count++;
                        if(count == stop) stop();
                    }
                }//selectCard()
            };
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(untap);
                    else {
                        CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isLand() && c.isTapped();
                            }
                        });
                        for(int i = 0; i < 2 && i < list.size(); i++) {
                            list.get(i).untap();
                        }
                    }//else
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7222997838166323277L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" untaps up to 2 lands.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END ***************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Vodalian Merchant")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                	card.getController().drawCard();
                	card.getController().discard(this);
                }
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -8924243774757009091L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" draws a card, then discards a card");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Whirlpool Rider")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    //shuffle hand into library, then shuffle library
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    Card c[] = hand.getCards();
                    for(int i = 0; i < c.length; i++)
                        AllZone.GameAction.moveTo(library, c[i]);
                    card.getController().shuffle();
                    
                    //draw same number of cards as before
                    for(int i = 0; i < c.length; i++)
                    	card.getController().drawCard();
                }
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 6290392806910817877L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController());
                	sb.append(" shuffles the cards from his hand into his library, then draws that many cards.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sky Swallower")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                	Player opp = card.getController().getOpponent();
                    PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Battlefield, opp);
                    PlayerZone myPlay = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    CardList list = new CardList(myPlay.getCards());
                    //list.remove(card);//doesn't move Sky Swallower
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.equals(card) && !c.getName().equals("Mana Pool");
                        }
                    });
                    
                    while(!list.isEmpty()) {
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                        //so "enters the battlefield" abilities don't trigger
                        ///list.get(0).addComesIntoPlayCommand(Command.Blank);
                        
                        oppPlay.add(list.get(0));
                        myPlay.remove(list.get(0));
                        
                        list.get(0).setController(opp);
                        list.remove(0);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                    }
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -453410206437839334L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController().getOpponent());
                	sb.append(" gains control of all other permanents you control");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************


       
        //*************** START *********** START **************************
        else if(cardName.equals("Lightning Crafter")) {
            final CommandReturn getCreature = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                    CardList list = new CardList();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    list.addAll(play.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getType().contains("Goblin") || 
                            c.getKeyword().contains("Shaman") || c.getKeyword().contains("Changeling");
                        }
                    });
                    
                    return list;
                }
            };//CommandReturn
            
            final SpellAbility abilityComes = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(getTargetCard() == null || getTargetCard() == card) AllZone.GameAction.sacrifice(card);
                    
                    else if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
                        AllZone.GameAction.exile(getTargetCard());
                    }
                }//resolve()
            };
            
            final Input inputComes = new Input() {
                private static final long serialVersionUID = -6066115143834426784L;
                
                @Override
                public void showMessage() {
                    CardList choice = (CardList) getCreature.execute();
                    
                    stopSetNext(CardFactoryUtil.input_targetChampionSac(card, abilityComes, choice,
                            "Select Goblin or Shaman to exile", false, false));
                    ButtonUtil.disableAll();
                }
                
            };
            Command commandComes = new Command() {
                private static final long serialVersionUID = -3498068347359658023L;
                
                public void execute() {
                    CardList creature = (CardList) getCreature.execute();
                    Player s = card.getController();
                    if(creature.size() == 0) {
                        AllZone.GameAction.sacrifice(card);
                        return;
                    } else if(s.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(inputComes);
                    else //computer
                    {
                        Card target;
                        //must target computer creature
                        CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
                        computer = computer.getType("Goblin");
                        computer.remove(card);
                        
                        computer.shuffle();
                        if(computer.size() != 0) {
                            target = computer.get(0);
                            abilityComes.setTargetCard(target);
                            AllZone.Stack.add(abilityComes);
                        }
                        else
                        	AllZone.GameAction.sacrifice(card);
                    }//else
                }//execute()
            };//CommandComes
            Command commandLeavesPlay = new Command() {
                private static final long serialVersionUID = 4236503599117025393L;
                
                public void execute() {
                    //System.out.println(abilityComes.getTargetCard().getName());
                    Object o = abilityComes.getTargetCard();
                    
                    if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardExiled((Card) o)) return;
                    
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            //copy card to reset card attributes like attack and defense
                            Card c = abilityComes.getTargetCard();
                            if(!c.isToken()) {
                                c = AllZone.CardFactory.copyCard(c);
                                c.setController(c.getOwner());
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getOwner());
                                PlayerZone removed = AllZone.getZone(Constant.Zone.Exile, c.getOwner());
                                removed.remove(c);
                                play.add(c);
                                
                            }
                        }//resolve()
                    };//SpellAbility
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - returning creature to the battlefield");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }//execute()
            };//Command
            
            card.addComesIntoPlayCommand(commandComes);
            card.addLeavesPlayCommand(commandLeavesPlay);
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -62128538115338896L;
                
                @Override
                public boolean canPlayAI() {
                    Object o = getCreature.execute();
                    if(o == null) return false;
                    
                    CardList cl = (CardList) getCreature.execute();
                    return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Faceless Butcher")) {
            final CommandReturn getCreature = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    //remove "this card"
                    list.remove(card);
                    
                    return list;
                }
            };//CommandReturn
            
            final SpellAbility abilityComes = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        AllZone.GameAction.exile(getTargetCard());
                    }
                }//resolve()
            };
            
            final Input inputComes = new Input() {
                private static final long serialVersionUID = -1932054059769056049L;
                
                @Override
                public void showMessage() {
                    CardList choice = (CardList) getCreature.execute();
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice,
                            "Select target creature to exile", true, false));
                    ButtonUtil.disableAll();//to disable the Cancel button
                }
            };
            Command commandComes = new Command() {
                private static final long serialVersionUID = -5675532512302863456L;
                
                public void execute() {
                    CardList creature = (CardList) getCreature.execute();
                    Player s = card.getController();
                    if(creature.size() == 0) return;
                    else if(s.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(inputComes);
                    else //computer
                    {
                        Card target;
                        
                        //try to target human creature
                        CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                        target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty
                        
                        if(target == null) {
                            //must target computer creature
                            CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
                            computer = computer.getType("Creature");
                            computer.remove(card);
                            
                            computer.shuffle();
                            if(computer.size() != 0) target = computer.get(0);
                        }
                        abilityComes.setTargetCard(target);
                        AllZone.Stack.add(abilityComes);
                    }//else
                }//execute()
            };//CommandComes
            Command commandLeavesPlay = new Command() {
                private static final long serialVersionUID = 5518706316791622193L;
                
                public void execute() {
                    //System.out.println(abilityComes.getTargetCard().getName());
                    Object o = abilityComes.getTargetCard();
                    
                    if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardExiled((Card) o)) return;
                    
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            //copy card to reset card attributes like attack and defense
                            Card c = abilityComes.getTargetCard();
                            if(!c.isToken()) {
                                c = AllZone.CardFactory.copyCard(c);
                                c.setController(c.getOwner());
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getOwner());
                                PlayerZone removed = AllZone.getZone(Constant.Zone.Exile, c.getOwner());
                                removed.remove(c);
                                play.add(c);
                                
                            }
                        }//resolve()
                    };//SpellAbility
                    ability.setStackDescription("Faceless Butcher - returning creature to the battlefield");
                    AllZone.Stack.add(ability);
                }//execute()
            };//Command
            
            card.addComesIntoPlayCommand(commandComes);
            card.addLeavesPlayCommand(commandLeavesPlay);
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                
                private static final long serialVersionUID = -62128538015338896L;
                
                @Override
                public boolean canPlayAI() {
                    Object o = getCreature.execute();
                    if(o == null) return false;
                    
                    CardList cl = (CardList) getCreature.execute();
                    return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Ember-Fist Zubera")) {
            //counts Zubera in all graveyards for this turn
            final CommandReturn countZubera = new CommandReturn() {
                public Object execute() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Graveyard.getCards());
                    list.addAll(AllZone.Computer_Graveyard.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getTurnInZone() == AllZone.Phase.getTurn())
                                    && (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling"));
                        }
                    });//CardListFilter()
                    
                    return Integer.valueOf(list.size());
                }
            };
            
            final Input[] input = new Input[1];
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    //human chooses target on resolve,
                    //computer chooses target in Command destroy
                    if(AllZone.HumanPlayer.equals(card.getController())) AllZone.InputControl.setInput(input[0]);
                    else {
                        int damage = ((Integer) countZubera.execute()).intValue();
                        
                        if(getTargetCard() != null) {
                            if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                    && CardFactoryUtil.canDamage(card, getTargetCard())
                                    && CardFactoryUtil.canTarget(card, getTargetCard())) {
                                Card c = getTargetCard();
                                c.addDamage(damage, card);
                            }
                        } else getTargetPlayer().addDamage(damage, card);
                    }
                }//resolve()
            };//SpellAbility
            
            input[0] = new Input() {
                private static final long serialVersionUID = 1899925898843297992L;
                
                @Override
                public void showMessage() {
                    int damage = ((Integer) countZubera.execute()).intValue();
                    AllZone.Display.showMessage("Select target Creature, Planeswalker or Player - " + damage
                            + " damage ");
                    ButtonUtil.disableAll();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if((card.isCreature() || card.isPlaneswalker()) && zone.is(Constant.Zone.Battlefield)) {
                        int damage = ((Integer) countZubera.execute()).intValue();
                        card.addDamage(damage, card);
                        
                        //have to do this since state effects aren't checked
                        //after this "Input" class is done
                        //basically this makes everything work right
                        //Ember-Fist Zubera can destroy a 2/2 creature
                        AllZone.GameAction.checkStateEffects();
                        stop();
                    }
                }//selectCard()
                
                @Override
                public void selectPlayer(Player player) {
                    int damage = ((Integer) countZubera.execute()).intValue();
                    player.addDamage(damage, card);
                    stop();
                }//selectPlayer()
            };//Input
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -1889425992069348304L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card).append(" causes damage to creature or player");
                	ability.setStackDescription(sb.toString());
                    
                    //@SuppressWarnings("unused") // damage
                    //int damage = ((Integer)countZubera.execute()).intValue();
                    
                    Player con = card.getController();
                    
                    //human chooses target on resolve,
                    //computer chooses target in Command destroy
                    if(con.equals(AllZone.ComputerPlayer)) ability.setTargetPlayer(AllZone.HumanPlayer);
                    
                    AllZone.Stack.add(ability);
                }//execute()
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Ashen-Skin Zubera")) {
            //counts Zubera in all graveyards for this turn
            final CommandReturn countZubera = new CommandReturn() {
                public Object execute() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Graveyard.getCards());
                    list.addAll(AllZone.Computer_Graveyard.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getTurnInZone() == AllZone.Phase.getTurn())
                                    && (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling"));
                        }
                    });//CardListFilter()
                    return Integer.valueOf(list.size());
                }
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int discard = ((Integer) countZubera.execute()).intValue();
                    getTargetPlayer().discard(discard, this, false);
                }//resolve()
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -7494691537986218546L;
                
                public void execute() {
                    Player opponent = card.getController().getOpponent();
                    ability.setTargetPlayer(opponent);
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card).append(" - ").append(opponent).append(" discards cards");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }//execute()
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Floating-Dream Zubera")) {
            //counts Zubera in all graveyards for this turn
            final CommandReturn countZubera = new CommandReturn() {
                public Object execute() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Graveyard.getCards());
                    list.addAll(AllZone.Computer_Graveyard.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getTurnInZone() == AllZone.Phase.getTurn())
                                    && (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling"));
                        }
                    });//CardListFilter()
                    return Integer.valueOf(list.size());
                }
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int number = ((Integer) countZubera.execute()).intValue();
                    
                    for(int i = 0; i < number; i++)
                        getTargetPlayer().drawCard();
                }//resolve()
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -5814070329854975419L;
                
                public void execute() {
                    ability.setTargetPlayer(card.getController());
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card).append(" - ").append(card.getController()).append(" draws cards");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                    
                }//execute()
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Silent-Chant Zubera")) {
            //counts Zubera in all graveyards for this turn
            final CommandReturn countZubera = new CommandReturn() {
                public Object execute() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Graveyard.getCards());
                    list.addAll(AllZone.Computer_Graveyard.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getTurnInZone() == AllZone.Phase.getTurn())
                                    && (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling"));
                        }
                    });//CardListFilter()
                    return Integer.valueOf(list.size());
                }
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int number = ((Integer) countZubera.execute()).intValue();
                    
                    getTargetPlayer().gainLife(number*2, card);
                }//resolve()
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -2327085948421343657L;
                
                public void execute() {
                    ability.setTargetPlayer(card.getController());
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card).append(" - ").append(card.getController()).append(" gains life");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                    
                }//execute()
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Dripping-Tongue Zubera")) {
            //counts Zubera in all graveyards for this turn
            final CommandReturn countZubera = new CommandReturn() {
                public Object execute() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Graveyard.getCards());
                    list.addAll(AllZone.Computer_Graveyard.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getTurnInZone() == AllZone.Phase.getTurn())
                                    && (c.getType().contains("Zubera") || c.getKeyword().contains("Changeling"));
                        }
                    });//CardListFilter()
                    return Integer.valueOf(list.size());
                }
            };//CommandReturn
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int count = ((Integer) countZubera.execute()).intValue();
                    for(int i = 0; i < count; i++)
                        CardFactoryUtil.makeToken("Spirit", "C 1 1 Spirit", card.getController(), "", new String[] {
                                "Creature", "Spirit"}, 1, 1, new String[] {""});
                }//resolve()
            };//SpellAbility
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 8362692868619919330L;
                
                public void execute() {
                    ability.setTargetPlayer(card.getController());
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card).append(" - ").append(card.getController()).append(" puts tokens onto the battlefield");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }//execute()
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Keiga, the Tide Star")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                        PlayerZone oldPlay = AllZone.getZone(getTargetCard());
                        
                        //so "enters the battlefield" abilities don't trigger
                        //getTargetCard().addComesIntoPlayCommand(Command.Blank);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                        
                        play.add(getTargetCard());
                        oldPlay.remove(getTargetCard());
                        
                        getTargetCard().setController(card.getController());
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                    }
                }//resolve()
            };
            
            final Input targetInput = new Input() {
                private static final long serialVersionUID = -8727869672234802473L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target creature");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(card, c)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if(c.isCreature() && zone.is(Constant.Zone.Battlefield)) {
                        ability.setTargetCard(c);
                        
                        StringBuilder sb = new StringBuilder();
                        sb.append("Gain control of ").append(ability.getTargetCard());
                        ability.setStackDescription(sb.toString());
                        
                        AllZone.Stack.add(ability);
                        stop();
                    }
                }
            };//Input
            Command destroy = new Command() {
                private static final long serialVersionUID = -3868616119471172026L;
                
                public void execute() {
                	Player con = card.getController();
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
                    
                    if(con.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(targetInput);
                    else if(list.size() != 0) {
                        Card target = CardFactoryUtil.AI_getBestCreature(list);
                        ability.setTargetCard(target);
                        AllZone.Stack.add(ability);
                    }
                }//execute()
            };
            card.addDestroyCommand(destroy);
        }
        //*************** END ************ END **************************
        
      //*************** START *********** START **************************
        else if(cardName.equals("Jhoira of the Ghitu")) {
            final Stack<Card> chosen= new Stack<Card>();
            final SpellAbility ability = new Ability(card, "2") {
                private static final long serialVersionUID = 4414609319033894302L;
                @Override
                public boolean canPlay() {
                    CardList possible = new CardList(AllZone.getZone(Constant.Zone.Hand, card.getController()).getCards());
                    possible.filter(new CardListFilter(){
                    	public boolean addCard(Card c){
                    		return !c.isLand();
                    	}                    	
                    });
                    return !possible.isEmpty() && super.canPlay();
                }
                
                public boolean canPlayAI(){return false;}
                
                @Override
                public void resolve() {
                    Card c = chosen.pop();
                    c.addCounter(Counters.TIME, 4);
                    c.setSuspend(true);
                }
            };
            
            ability.setAfterPayMana(new Input() {
                private static final long serialVersionUID = -1647181037510967127L;
                
                @Override
                public void showMessage()
                {
                	ButtonUtil.disableAll();
                    AllZone.Display.showMessage("Exile a nonland card from your hand.");
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone)
                {
                	if(zone.is(Constant.Zone.Hand) && !c.isLand())
                	{
                		AllZone.GameAction.exile(c);
                		chosen.push(c);
                		ability.setStackDescription(card.toString() + " - Suspending " + c.toString());
                		AllZone.Stack.add(ability);
                		stop();
                	}
                }
            });
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mindwrack Liege")) {
            final SpellAbility ability = new Ability(card, "UR UR UR UR") {
                private static final long serialVersionUID = 3978560192382921056L;
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Computer_Hand.getCards());
                    list = list.getType("Creature");
                    CardList list2 = list.getColor("Blue");
                    list2.add(list.getColor("Red"));
                    return list2;
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(c, hand)) {
                        hand.remove(c);
                        play.add(c);
                    }
                }
            };
            
            ability.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -1038409328463518290L;
                
                @Override
                public void showMessage() {
                	Player controller = card.getController();
                    CardList creats = new CardList(AllZone.getZone(Constant.Zone.Hand, controller).getCards());
                    creats = creats.getType("Creature");
                    CardList creats2 = creats.getColor("U");
                    creats2.add(creats.getColor("R"));      
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, creats2, "Select a creature", false,
                            false));
                }
            });
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Disciple of Kangee")) {
        	Ability_Cost abCost = new Ability_Cost("U T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, new Target("TgtC")) {
                private static final long serialVersionUID = -5169389637917649036L;
                
                @Override
                public boolean canPlayAI() {
                    if (CardFactoryUtil.AI_doesCreatureAttack(card)) return false;
                    
                    return CardFactoryUtil.AI_getHumanCreature("Flying", card, false).isEmpty()
                            && (getCreature().size() != 0);
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && 
                            	   (!CardFactoryUtil.AI_doesCreatureAttack(c)) && 
                            	   (!c.getKeyword().contains("Flying")) && 
                            	   CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    list.remove(card);
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard()) && 
                    		CardFactoryUtil.canTarget(card, getTargetCard())) {
                        final Card[] creature = new Card[1];
                        final long timestamp;
                        
                        creature[0] = getTargetCard();
                        creature[0].addExtrinsicKeyword("Flying");
                        timestamp = creature[0].addColor("U", card, false, true);
            
                        final Command EOT = new Command() {
                            private static final long serialVersionUID = -1899153704584793548L;
                            long stamp = timestamp;
                            public void execute() {
                                if (AllZone.GameAction.isCardInPlay(creature[0])) {
                                    creature[0].removeExtrinsicKeyword("Flying");
                                    creature[0].removeColor("U", card, false, stamp);
                                }
                            }
                        };
                        AllZone.EndOfTurn.addUntil(EOT);

                    }//if (card is in play)
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription(abCost+"Target creature gains flying and becomes blue until end of turn.");
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Mirror Entity"))
        {
      	  final Ability ability = new Ability(card, "0")
      	  {
      		  public void resolve()
      		  {
        			final CardList list = new CardList(AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards()).getType("Creature");
                    final int[] originalAttack = new int[list.size()];
                    final int[] originalDefense = new int[list.size()];
          			for(int i = 0; i < list.size(); i++) {
                     originalAttack[i] = list.get(i).getBaseAttack();
                     originalDefense[i] = list.get(i).getBaseDefense();
                      
                      list.get(i).setBaseAttack(Integer.parseInt(getManaCost()));
                      list.get(i).setBaseDefense(Integer.parseInt(getManaCost()));
                      list.get(i).addExtrinsicKeyword("Changeling");
                      if(i + 1 == list.size()) {
                      final Command EOT = new Command() {
                          private static final long serialVersionUID = 6437463765161964445L;
                          
                          public void execute() {
                        	  
                        	  for(int x = 0; x < list.size(); x++) {
                              if(AllZone.GameAction.isCardInPlay(list.get(x))) {
                            	  list.get(x).setBaseAttack(originalAttack[x]);
                            	  list.get(x).setBaseDefense(originalDefense[x]);
                            	  list.get(x).removeExtrinsicKeyword("Changeling");
                              }
                        	  }
                        	  }
                          };
                          AllZone.EndOfTurn.addUntil(EOT);
                      };
      		  }
      		  }
      		  public boolean canPlayAI()
      		  {
      			 return false;
      			  /*
      			CardList Clist = new CardList(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards()).getType("Creature"); 
      			CardList Hlist = new CardList(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards()).getType("Creature");
      			return((Clist.size() - Hlist.size() * ComputerUtil.getAvailableMana().size() > AllZone.HumanPlayer.getLife())
      					&& AllZone.Phase.getPhase().equals(Constant.Phase.Main1));
      					*/
      					
      		  }
      	  };
      	  ability.setBeforePayMana(new Input()
      	  {
      		private static final long serialVersionUID = 4378124586732L;

  			public void showMessage()
      		 {
      			 String s = JOptionPane.showInputDialog("What would you like X to be?");
      	  		 try {
      	  			     Integer.parseInt(s);
      	  				 ability.setManaCost(s);
      	  				 stopSetNext(new Input_PayManaCost(ability));
      	  			 }
      	  			 catch(NumberFormatException e){
      	  				 AllZone.Display.showMessage("\"" + s + "\" is not a number.");
      	  				 showMessage();
      	  			 }
      		 }
      	  });
      	  ability.setDescription("X: Creatures you control become X/X and gain changeling until end of turn.");
      	  
      	  StringBuilder sb = new StringBuilder();
      	  sb.append(card.getName()).append("X: Creatures you control become X/X and gain changeling until end of turn.");
      	  ability.setStackDescription(sb.toString());
      	  
      	  card.addSpellAbility(ability);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Gigantomancer")) {
        	final Ability ability = new Ability(card, "1") {
                private static final long serialVersionUID = -68531201448677L;
                
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
                public Card getCreature() {
                    CardList untapped = new CardList(AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards()).getType("Creature");
                    untapped = untapped.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && 6 > c.getNetAttack();
                        }
                    });                   
                    if(untapped.isEmpty()) return null;
                    Card worst = untapped.get(0);
                    for(int i = 0; i < untapped.size(); i++)
                        if(worst.getNetAttack() > untapped.get(i).getNetAttack()) worst = untapped.get(i);
                    return worst;
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        final Card[] creature = new Card[1];
                        
                        creature[0] = getTargetCard();
                        final int[] originalAttack = {creature[0].getBaseAttack()};
                        final int[] originalDefense = {creature[0].getBaseDefense()};
                        
                        creature[0].setBaseAttack(7);
                        creature[0].setBaseDefense(7);
                        
                        final Command EOT = new Command() {
                            private static final long serialVersionUID = 6437463765161964445L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(creature[0])) {
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
            ability.setDescription("1: Target creature you control becomes 7/7 until end of turn.");
            //this ability can target "this card" when it shouldn't be able to
            ability.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -7903295056497483023L;
                
                @Override
                public void showMessage() {
                	Player player = card.getController();
                    CardList targets = new CardList(AllZone.getZone(Constant.Zone.Battlefield, player).getCards()); 
                    targets = targets.getType("Creature");
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, targets,
                            "Select a creature you control", true, false));
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Hermit Druid")) {
        	Ability_Cost abCost = new Ability_Cost("G T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = 5884624727757154056L;

				@Override
                public boolean canPlayAI() {
                    // todo: figure out when the AI would want to use the Druid
					return false;
                }
                
                
                @Override
                public void resolve() {
                	CardList library = AllZoneUtil.getPlayerCardsInLibrary(card.getController());
                	if(library.size() == 0) return;	// maybe provide some notification that library is empty?
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());

					CardList revealed = new CardList();

					Card basicGrab = null;
					Card top;
					int count = 0;
					// reveal top card until library runs out or hit a basic land
					while(basicGrab == null) {
						top = library.get(count);
						count++;
						revealed.add(top);
						lib.remove(top);
						if (top.isBasicLand())
							basicGrab = top;

						if(count == library.size()) 
							break;
					}//while
					AllZone.Display.getChoiceOptional("Revealed cards:", revealed.toArray());
					
					if (basicGrab != null){
						// put basic in hand
						hand.add(basicGrab);
						revealed.remove(basicGrab);
					}
					// place revealed cards in graveyard (todo: player should choose order)
					for(Card c : revealed){
						grave.add(c);
					}
                }
            };
            ability.setStackDescription(abCost+"Reveal cards until you reveal a basic land. Put that in your hand, and put the rest in your graveyard");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sorceress Queen") || cardName.equals("Serendib Sorcerer")) {
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
        	Target target = new Target("Select target creature other than "+cardName, new String[] {"Creature.Other"});
            final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = -6853184726011448677L;
                
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
                public Card getCreature() {
                    CardList untapped = CardFactoryUtil.AI_getHumanCreature(card, true);
                    untapped = untapped.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && 2 < c.getNetDefense() && c != card;
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
                        final int[] originalDefense = {creature[0].getBaseDefense()};
                        
                        creature[0].setBaseAttack(0);
                        creature[0].setBaseDefense(2);
                        
                        final Command EOT = new Command() {
                            private static final long serialVersionUID = 6437463765161964445L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(creature[0])) {
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
            
            StringBuilder sb = new StringBuilder();
            sb.append(abCost).append("Target creature other than ").append(cardName).append(" becomes 0/2 until end of turn.");
            ability.setDescription(sb.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Steel Overseer")) {
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {

				private static final long serialVersionUID = 1822894871718751099L;

				@Override
                public void resolve() {
                    CardList arts = AllZoneUtil.getCreaturesInPlay(card.getController());
                    arts = arts.filter(AllZoneUtil.artifacts);
                    
                    for(int i = 0; i < arts.size(); i++) {
                        Card card = arts.get(i);
                        card.addCounter(Counters.P1P1, 1);
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                	CardList arts = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
                    arts = arts.filter(AllZoneUtil.artifacts);
                    
                    return arts.size() > 1;
                }//canPlayAI()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription(abCost+"Put a +1/+1 counter on each artifact creature you control.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - Put a +1/+1 counter on each artifact creature you control.");
            ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Vedalken Plotter")) {
            final Card[] target = new Card[2];
            final int[] index = new int[1];
            
            final Ability ability = new Ability(card, "") {
                
                private static final long serialVersionUID = -3075569295823682336L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    
                    Card crd0 = target[0];
                    Card crd1 = target[1];
                    
                    if(crd0 != null && crd1 != null) {
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                        
                        PlayerZone from0 = AllZone.getZone(crd0);
                        from0.remove(crd0);
                        PlayerZone from1 = AllZone.getZone(crd1);
                        from1.remove(crd1);
                        
                        crd0.setController(card.getController().getOpponent());
                        crd1.setController(card.getController());
                        
                        PlayerZone to0 = AllZone.getZone(Constant.Zone.Battlefield,
                                card.getController().getOpponent());
                        to0.add(crd0);
                        PlayerZone to1 = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                        to1.add(crd1);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                    }
                    
                }//resolve()
            };//SpellAbility
            

            final Input input = new Input() {
                
                private static final long serialVersionUID = -7143706716256752987L;
                
                @Override
                public void showMessage() {
                    if(index[0] == 0) AllZone.Display.showMessage("Select target land you control.");
                    else AllZone.Display.showMessage("Select target land opponent controls.");
                    
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    //must target creature you control
                    if(index[0] == 0 && !c.getController().equals(card.getController())) return;
                    
                    //must target creature you don't control
                    if(index[0] == 1 && c.getController().equals(card.getController())) return;
                    

                    if(c.isLand() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(card, c)) {
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
            
            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 6513203926272187582L;
                
                public void execute() {
                    index[0] = 0;
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(input);
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - Exchange control of target land you control and target land an opponent controls.");
            ability.setStackDescription(sb.toString());
            
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Dauntless Escort")) {
        	final SpellAbility ability = new Ability(card, "0") {
        		@Override
        		public boolean canPlayAI() {
			        PlayerZone PlayerPlayZone = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
			        CardList PlayerCreatureList = new CardList(PlayerPlayZone.getCards());
			        PlayerCreatureList = PlayerCreatureList.getType("Creature");
					PlayerZone opponentPlayZone = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
			        CardList opponentCreatureList = new CardList(opponentPlayZone.getCards());
			        opponentCreatureList = opponentCreatureList.getType("Creature");
                    return ((PlayerCreatureList.size() + 1 > 2* opponentCreatureList.size() + 1) && (Phase.Sac_Dauntless_Escort_Comp == false) && (AllZone.Phase.getPhase().equals(Constant.Phase.Main1))) ;
        		}
               
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = 2701248867610L;
                    
                    public void execute() {
                        if(card.getController() == AllZone.HumanPlayer) {
                        	Phase.Sac_Dauntless_Escort = false;
                        	} else {
                        	Phase.Sac_Dauntless_Escort_Comp = false;  
                        	}
    			        PlayerZone PlayerPlayZone = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
    			        CardList PlayerCreatureList = new CardList(PlayerPlayZone.getCards());
    			        PlayerCreatureList = PlayerCreatureList.getType("Creature");
        				if(PlayerCreatureList.size() != 0) {
        	                for(int i = 0; i < PlayerCreatureList.size(); i++) {
        	                	Card c = PlayerCreatureList.get(i);
        	                    c.removeExtrinsicKeyword("Indestructible");				
        				}
        					}
                    }
                };
        		@Override
        		public void resolve() {
                    AllZone.GameAction.sacrifice(card);
                    if(card.getController() == AllZone.HumanPlayer) {
                    	Phase.Sac_Dauntless_Escort = true;
                    }
                    else Phase.Sac_Dauntless_Escort_Comp = true;	

                    AllZone.EndOfTurn.addUntil(untilEOT);
        		}
        	};
        	card.addSpellAbility(ability);
            ability.setStackDescription("Sacrifice Dauntless Escort: Creatures you control are indestructible this turn.");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Wojek Embermage")) {
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
            Target tgt = new Target("TgtC");
            final Ability_Activated ability = new Ability_Activated(card, abCost, tgt) {
                private static final long serialVersionUID = -1208482961653326721L;
                
                @Override
                public boolean canPlayAI() {
                    return (CardFactoryUtil.AI_getHumanCreature(1, card, true).size() != 0)
                            && (AllZone.Phase.getPhase().equals(Constant.Phase.Main2));
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
                    list.shuffle();
                    setTargetCard(list.get(0));
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        CardList list = getRadiance(getTargetCard());
                        for(int i = 0; i < list.size(); i++) {
                            if(CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(1, card);
                        }
                    }
                }//resolve()
                
                //parameter Card c, is included in CardList
                //no multi-colored cards
                CardList getRadiance(Card c) {
                	if(CardUtil.getColors(c).contains(Constant.Color.Colorless)) {
                        CardList list = new CardList();
                        list.add(c);
                        return list;
                    }
                    
                    CardList sameColor = new CardList();
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    
                    for(int i = 0; i < list.size(); i++)
                        if(list.get(i).sharesColorWith(c)) sameColor.add(list.get(i));
                    
                    return sameColor;
                }
                
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("Radiance - "+abCost+cardName+" deals 1 damage to target creature and each other creature that shares a color with it.");
            
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Adarkar Valkyrie")) {
            //tap ability - no cost - target creature - EOT
            
            final Card[] target = new Card[1];
            
            final Command destroy = new Command() {
                private static final long serialVersionUID = -2433442359225521472L;
                
                public void execute() {
                    AllZone.Stack.add(new Ability(card, "0", "Adarkar Valkyrie - Return " + target[0] + " from graveyard to the battlefield") {
                        @Override
                        public void resolve() {
                            PlayerZone grave = AllZone.getZone(target[0]);
                            //checks to see if card is still in the graveyard
        
                            if(grave != null && AllZone.GameAction.isCardInZone(target[0], grave)) {
                                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                                target[0].setController(card.getController());
                                AllZone.GameAction.moveTo(play, target[0]);
                            }
                        }
                    });
                }//execute()
            };
            
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 2777978927867867610L;
                
                public void execute() {
                    //resets the Card destroy Command
                    target[0].removeDestroyCommand(destroy);
                }
            };
            
            Ability_Cost abCost = new Ability_Cost("T", cardName, true);
            Target tgt = new Target("Target creature other than "+cardName, "Creature.Other".split(","));
            final Ability_Activated ability = new Ability_Activated(card, abCost, tgt){
                private static final long serialVersionUID = -8454685126878522607L;
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
                        target[0] = getTargetCard();
                        
                        if (!target[0].isToken()){	// not necessary, but will help speed up stack resolution
	                        AllZone.EndOfTurn.addUntil(untilEOT);
	                        target[0].addDestroyCommand(destroy);
                        }
                    }//if
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility

            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append("tap: When target creature other than Adarkar Valkyrie is put into a ");
            sb.append("graveyard this turn, return that card to the battlefield under your control.");
            ability.setDescription(sb.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mayael the Anima")) {
        	Ability_Cost abCost = new Ability_Cost("3 R G W T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
                
                private static final long serialVersionUID = -9076784333448226913L;
                
				@Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList Library = new CardList(lib.getCards());
                    int Count = 5;
                    if(Library.size() < 5) Count = Library.size();
                    CardList TopCards = new CardList();
                    
                    for(int i = 0; i < Count; i++) TopCards.add(Library.get(i));
                    CardList TopCreatures = TopCards;
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        if(TopCards.size()  > 0) {
						AllZone.Display.getChoice(
                                "Look at the top five cards: ", TopCards.toArray());
                        TopCreatures = TopCreatures.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                if(c.isCreature() && c.getNetAttack() >= 5) return true;
                                else return false;
                            }
                        });
                        if(TopCreatures.size() > 0) {
                        Object o2 = AllZone.Display.getChoiceOptional(
                                "Put a creature with a power 5 or greater onto the battlefield: ", TopCreatures.toArray());
                        if(o2 != null) {
                            Card c = (Card) o2;
                            lib.remove(c);
                            play.add(c);
                            TopCards.remove(c);
                        }
                        } else JOptionPane.showMessageDialog(null, "No creatures in top 5 cards with a power greater than 5.", "", JOptionPane.INFORMATION_MESSAGE); 
                        
                        Count = TopCards.size();
                    	for(int i = 0; i < Count; i++) {   
                            AllZone.Display.showMessage("Select a card to put " + (Count - i) + " from the bottom of your library: "  + (Count - i) + " Choices to go.");
                            ButtonUtil.enableOnlyCancel();
                            Object check = AllZone.Display.getChoice("Select a card: ", TopCards.toArray());   
                            AllZone.GameAction.moveTo(lib, (Card) check);
                            TopCards.remove((Card) check);
                        	}
                        }  else JOptionPane.showMessageDialog(null, "No more cards in library.", "", JOptionPane.INFORMATION_MESSAGE);
                    }
                        else {
                        TopCreatures = TopCreatures.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                            	CardList Compplay = new CardList();
                            	Compplay.addAll(AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
                            	Compplay = Compplay.getName(c.getName());
                                if(c.isCreature() && c.getNetAttack() >= 5 && (Compplay.size() == 0 && c.getType().contains("Legendary"))) return true;
                                else return false;
                            }
                        });
                        if(TopCreatures.size() > 0) {
                        	Card c = CardFactoryUtil.AI_getBestCreature(TopCreatures);
                            lib.remove(c);
                            play.add(c);
                            TopCards.remove(c);
                        Count = TopCards.size();
                    	for(int i = 0; i < Count; i++) {
                    		Card Remove_Card = TopCards.get(i);
                            AllZone.GameAction.moveTo(lib, Remove_Card);
                        	}
                    } 
                        
                    }
                }              
                @Override
                public boolean canPlayAI() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList Library = new CardList(lib.getCards());
                    return Library.size() > 0 && super.canPlay();
                }
            };
                     
            card.addSpellAbility(ability);
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append(abCost+"Look at the top five cards of your library. ");
            sbDesc.append("You may put a creature card with power 5 or greater from among them onto the battlefield. ");
            sbDesc.append("Put the rest on the bottom of your library in any order.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append(" - Looks at the top five cards of his/her library. ");
            sbStack.append("That player may put a creature card with power 5 or greater from among them onto the battlefield. ");
            sbStack.append("The player then puts the rest on the bottom of his/her library in any order.");
            ability.setStackDescription(sbStack.toString());
        }//*************** END ************ END ************************** 

        
        //*************** START *********** START **************************
        else if(cardName.equals("Helldozer")) {
            Ability_Cost abCost = new Ability_Cost("B B B T", cardName, true);
            Target target = new Target("Select target land.", new String[]{"Land"});
            final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = 6426884086364885861L;
                
                @Override
                public boolean canPlayAI() {
                    if(CardFactoryUtil.AI_doesCreatureAttack(card)) return false;
                    
                    CardList land = new CardList(AllZone.Human_Battlefield.getCards());
                    land = land.getType("Land");
                    return land.size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    //target basic land that Human only has 1 or 2 in play
                    CardList land = AllZoneUtil.getPlayerLandsInPlay(AllZone.HumanPlayer);
                    
                    Card target = null;
                    
                    String[] name = {"Forest", "Swamp", "Plains", "Mountain", "Island"};
                    for(int i = 0; i < name.length; i++)
                        if(land.getName(name[i]).size() == 1) {
                            target = land.getName(name[i]).get(0);
                            break;
                        }
                    
                    //see if there are only 2 lands of the same type
                    if(target == null) {
                        for(int i = 0; i < name.length; i++)
                            if(land.getName(name[i]).size() == 2) {
                                target = land.getName(name[i]).get(0);
                                break;
                            }
                    }//if
                    if(target == null) {
                        land.shuffle();
                        target = land.get(0);
                    }
                    setTargetCard(target);
                }//chooseTargetAI()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        AllZone.GameAction.destroy(getTargetCard());
                        
                        //if non-basic, untap Helldozer
                        if(!getTargetCard().isBasicLand()) card.untap();
                    }
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription(abCost+"Destroy target land. If that land was nonbasic, untap Helldozer.");
        }//*************** END ************ END **************************
               
        
        //*************** START *********** START **************************
        else if(cardName.equals("Spitting Spider")) {
        	// temporary fix until DamageAll is created
        	Ability_Cost abCost = new Ability_Cost("Sac<1/Land>", cardName, true);
            final SpellAbility ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = 2560268493829888869L;

				@Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    CardList list = AllZoneUtil.getCreaturesInPlayWithKeyword("Flying");
                    
                    for(int i = 0; i < list.size(); i++)
                        if(CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(1, card);
                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" deals 1 damage to each creature with flying.");
            ability.setStackDescription(sb.toString());
            ability.setDescription(abCost+"Spitting Spider deals 1 damage to each creature with flying.");
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Obsidian Fireheart")) {
            final Ability ability = new Ability(card, "1 R R") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && c.isLand() && (c.getCounters(Counters.BLAZE) == 0)) c.addCounter(
                            Counters.BLAZE, 1);
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
                    CardList land = new CardList(play.getCards());
                    land = land.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand() && c.getCounters(Counters.BLAZE) < 1;
                        }
                    });
                    
                    if(land.size() > 0) setTargetCard(land.get(0));
                    
                    return land.size() > 0;
                }
                
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append("1 R R: Put a blaze counter on target land without a blaze counter on it. ");
            sb.append("For as long as that land has a blaze counter on it, it has \"At the beginning of your upkeep, ");
            sb.append("this land deals 1 damage to you.\" (The land continues to burn after Obsidian Fireheart has left the battlefield.)");
            ability.setDescription(sb.toString());
            
            ability.setBeforePayMana(CardFactoryUtil.input_targetType(ability, "Land"));
            card.addSpellAbility(ability);
        }// *************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Subterranean Spirit")) {
            Ability_Cost abCost = new Ability_Cost("T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
            	
                private static final long serialVersionUID = 7698358771810336470L;

                @Override
                public void resolve() {
                	
      				CardList all = AllZoneUtil.getCreaturesInPlay();
                    all = all.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return !c.getKeyword().contains("Flying") &&
                    			   CardFactoryUtil.canDamage(card, c);
                    	}
                    });
                    
                    for(int i = 0; i < all.size(); i++)
                        	all.get(i).addDamage(1, card);
 
                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" deals 1 damage to each creature without flying.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription(abCost+cardName+" deals 1 damage to each creature without flying.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Scourge of Kher Ridges")) {
            
            final Ability ability = new Ability(card, "1 R") {
            	
                @Override
                public void resolve() {
                	
      				CardList all = AllZoneUtil.getCreaturesInPlay();
                    all = all.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return !c.getKeyword().contains("Flying") &&
                    			   CardFactoryUtil.canDamage(card, c);
                    	}
                    });
                    
                    for(int i = 0; i < all.size(); i++)
                        	all.get(i).addDamage(2, card);

                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" deals 2 damage to each creature without flying.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("1 R: Scourge of Kher Ridges deals 2 damage to each creature without flying.");
            card.addSpellAbility(ability);
            
            final Ability a2 = new Ability(card, "5 R") {
            	
                @Override
                public void resolve() {
                	
      				CardList all = AllZoneUtil.getCreaturesInPlay();
                    all = all.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.getKeyword().contains("Flying") &&
                    			   !c.equals(card) && CardFactoryUtil.canDamage(card, c);
                    	}
                    });
                    
                    for(int i = 0; i < all.size(); i++)
                        	all.get(i).addDamage(6, card);

                }//resolve()
            };//SpellAbility
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card).append(" deals 6 damage to each other creature with flying.");
            a2.setStackDescription(sb2.toString());
            
            a2.setDescription("5 R: Scourge of Kher Ridges deals 6 damage to each creature with flying.");
            card.addSpellAbility(a2);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Flowstone Sculpture")) {
            
            final Ability ability1 = new Ability(card, "2") {
                @Override
                public boolean canPlayAI() {
                   return false;
                }

                @Override
                public void resolve() {
                	card.addCounter(Counters.P1P1,1);
                }//resolve()
            };//SpellAbility
            

            Input runtime1 = new Input() {
                private static final long serialVersionUID = -4312210760957471033L;
                
                @Override
                public void showMessage() {
                    stopSetNext(CardFactoryUtil.input_discard(ability1, 1));
                    
                }
            };
            
            StringBuilder sb1 = new StringBuilder();
            sb1.append(card).append(" gets a +1/+1 counter.");
            ability1.setStackDescription(sb1.toString());
            
            ability1.setDescription("2, Discard a card: put a +1/+1 counter on Flowstone Sculpture.");
            card.addSpellAbility(ability1);
            ability1.setBeforePayMana(runtime1);
            
            final Ability ability2 = new Ability(card, "2") {
                @Override
                public boolean canPlayAI() {
                   return false;
                }

                @Override
                public void resolve() {
                    card.addIntrinsicKeyword("Flying");
                }//resolve()
            };//SpellAbility
            

            Input runtime2 = new Input() {
                private static final long serialVersionUID = -4312210745957471033L;
                
                @Override
                public void showMessage() {
                    stopSetNext(CardFactoryUtil.input_discard(ability2, 1));
                    
                }
            };
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card).append(" gains flying.");
            ability2.setStackDescription(sb2.toString());
            
            ability2.setDescription("2, Discard a card: Flowstone Sculpture gains flying.");
            card.addSpellAbility(ability2);
            ability2.setBeforePayMana(runtime2);
            
            final Ability ability3 = new Ability(card, "2") {
                @Override
                public boolean canPlayAI() {
                   return false;
                }

                @Override
                public void resolve() {
                    card.addIntrinsicKeyword("First Strike");
                }//resolve()
            };//SpellAbility
            

            Input runtime3 = new Input() {
                private static final long serialVersionUID = -4312213760957471033L;
                
                @Override
                public void showMessage() {
                    stopSetNext(CardFactoryUtil.input_discard(ability3, 1));
                    
                }
            };
            
            StringBuilder sb3 = new StringBuilder();
            sb3.append(card).append(" gains First Strike.");
            ability3.setStackDescription(sb3.toString());
            
            ability3.setDescription("2, Discard a card: FlowStone Sculpture gains first strike.");
            card.addSpellAbility(ability3);
            ability3.setBeforePayMana(runtime3);
            
            final Ability ability4 = new Ability(card, "2") {
                @Override
                public boolean canPlayAI() {
                   return false;
                }

                @Override
                public void resolve() {
                    card.addIntrinsicKeyword("Trample");
                }//resolve()
            };//SpellAbility
            
            Input runtime4 = new Input() {
                private static final long serialVersionUID = -4312210700957472033L;
                
                @Override
                public void showMessage() {
                    stopSetNext(CardFactoryUtil.input_discard(ability4, 1));
                    
                }
            };
            
            StringBuilder sb4 = new StringBuilder();
            sb4.append(card).append(" gains trample.");
            ability4.setStackDescription(sb4.toString());
            
            ability4.setDescription("2, Discard a card: Flowstone Sculpture gains trample.");
            card.addSpellAbility(ability4);
            ability4.setBeforePayMana(runtime4);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Wild Mongrel")) {
        	
        	final String[] color = new String[1];
        	final long[] timeStamp = new long[1];
            
        	final Ability_Cost abCost = new Ability_Cost("Discard<1/Card>", cardName, true);
        	
            //mana tap ability
            final SpellAbility ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = 5443609178720006665L;

				@Override
                public boolean canPlayAI() {
                    Card[] hand = AllZone.Computer_Hand.getCards();
                    return CardFactoryUtil.AI_doesCreatureAttack(card) && (hand.length > 3);
                }
                
                @Override
                public void chooseTargetAI() {
                    AllZone.ComputerPlayer.discardRandom(this);
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(1);
                        card.addTempDefenseBoost(1);
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            String[] colors = Constant.Color.onlyColors;
                            
                            Object o = AllZone.Display.getChoice("Choose color", colors);
                            color[0] = (String) o;
                            card.setChosenColor(color[0]);
                        } else { 
                        	// wild mongrel will choose a color that appears the most, but that might not be right way to choose
                        	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer);
                            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
                            CardList list = new CardList();
                            list.addAll(lib.getCards());
                            list.addAll(hand.getCards());
                            list.addAll(AllZone.Computer_Battlefield.getCards());
                            
                            color[0] = Constant.Color.White;
                            int max = list.getKeywordsContain(color[0]).size();
                            
                            String[] colors = { Constant.Color.Blue, Constant.Color.Black, Constant.Color.Red, Constant.Color.Green };
                            for(String c : colors){
    	                        int cmp = list.getKeywordsContain(c).size();
    	                        if (cmp > max){
    	                        	max = cmp;
    	                        	color[0] = c;
    	                        }
                            }
                            card.setChosenColor(color[0]);
                        }
                        String s = CardUtil.getShortColor(color[0]);
                        timeStamp[0] = card.addColor(s, card, false, true);
                        
                        //sacrifice ability - targets itself - until EOT
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -5563743272875711445L;
                            long stamp = timeStamp[0];
                            String s = CardUtil.getShortColor(color[0]);
                            
                            public void execute() {
                                card.addTempAttackBoost(-1);
                                card.addTempDefenseBoost(-1);
                                card.removeColor(s, card, false, stamp);
                                card.setChosenColor("");
                            }
                        };
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
            };//SpellAbility

            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets +1/+1 and becomes the color of your choice until end of turn.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("Discard a card: Wild Mongrel gets +1/+1 and becomes the color of your choice until end of turn.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Spiritmonger")) {
        	
        	final String[] color = new String[1];
        	final long[] timeStamp = new long[1];
            
            //color change ability
        	Ability_Cost abCost = new Ability_Cost("G", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -5362934962417382279L;

				@Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            String[] colors = Constant.Color.onlyColors;
                            
                            Object o = AllZone.Display.getChoice("Choose color", colors);
                            color[0] = (String) o;
                            card.setChosenColor(color[0]);
                            String s = CardUtil.getShortColor(color[0]);
                            timeStamp[0] = card.addColor(s, card, false, true);
                            
                            //until EOT
                            final Command untilEOT = new Command() {
                                private static final long serialVersionUID = -7093762180313802891L;
                                long stamp = timeStamp[0];
                                String s = CardUtil.getShortColor(color[0]);
                                public void execute() {
                                    card.removeColor(s, card, false, stamp);
                                    card.setChosenColor("");
                                }
                            };
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }
                    }
                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes the color of your choice until end of turn.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription(abCost+cardName+" becomes the color of your choice until end of turn.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
      //*************** START *********** START **************************
        else if(cardName.equals("Psychatog")) {
            final Command untilEOT = new Command() {

				private static final long serialVersionUID = -280983229935814313L;

				public void execute() {
                    card.addTempAttackBoost(-1);
                    card.addTempDefenseBoost(-1);
                }
            };

            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    Card[] hand = AllZone.Computer_Hand.getCards();
                    return CardFactoryUtil.AI_doesCreatureAttack(card) && (hand.length > 2);
                }
                
                @Override
                public void chooseTargetAI() {
                    AllZone.ComputerPlayer.discardRandom(this);
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(1);
                        card.addTempDefenseBoost(1);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
                
                public boolean canPlay()
                {
                	return super.canPlay() && AllZoneUtil.getPlayerHand(card.getController()).size() >= 1;
                }
            };//SpellAbility
            
            Input runtime = new Input() {
				private static final long serialVersionUID = -1987380648014917445L;

				@Override
                public void showMessage() {
					StringBuilder sb = new StringBuilder();
					sb.append(card).append(" gets +1/+1 until EOT.");
					ability.setStackDescription(sb.toString());
                    
                    stopSetNext(CardFactoryUtil.input_discard(ability, 1));
                }
            };
            
            final Ability ability2 = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    Card[] grave = AllZone.Computer_Graveyard.getCards();
                    return CardFactoryUtil.AI_doesCreatureAttack(card) && (grave.length >= 2);
                }
                
                @Override
                public void chooseTargetAI() {
                	if (AllZone.Computer_Graveyard.getCards().length >=2) {
                		AllZone.GameAction.exile(AllZone.Computer_Graveyard.getCards()[0]);
                		AllZone.GameAction.exile(AllZone.Computer_Graveyard.getCards()[0]);
                	}
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(1);
                        card.addTempDefenseBoost(1);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
                
                public boolean canPlay()
                {
                	return super.canPlay() && AllZoneUtil.getPlayerGraveyard(card.getController()).size() >= 2;
                }
            };//SpellAbility
            
            Input runtime2 = new Input() {

            	boolean once = false;
				private static final long serialVersionUID = 8243511353958609599L;

				@Override
                public void showMessage() {
					CardList list = new CardList(AllZone.Human_Graveyard.getCards());
					if (list.size() < 2 || once) {
						once = false;
						stop();
					}
					else {
						Object o = AllZone.Display.getChoice("Choose first card to exile", list.toArray());
						if (o!=null)
						{
							Card c1 = (Card)o;
							AllZone.GameAction.exile(c1);
							list.remove(c1);
							
							o = AllZone.Display.getChoice("Choose second card to exile", list.toArray());
							if (o!=null)
							{
								
								Card c2 = (Card)o;
								AllZone.GameAction.exile(c2);
	
								once = true;
								AllZone.Stack.add(ability2);
							}
						}
					}
					stop();
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets +1/+1 until end of turn.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("Discard a card: Psychatog gets +1/+1 until end of turn.");
            ability.setBeforePayMana(runtime);
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card).append(" gets +1/+1 until end of turn.");
            ability2.setStackDescription(sb2.toString());
            
            ability2.setDescription("Exile two cards from your graveyard: Psychatog gets +1/+1 until end of turn.");
            ability2.setBeforePayMana(runtime2);
            
            card.addSpellAbility(ability);
            card.addSpellAbility(ability2);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Jugan, the Rising Star")) {
            final SpellAbility ability = new Ability(card, "0") {
                //should the computer play this card?
                @Override
                public boolean canPlayAI() {
                    return (getComputerCreatures().size() != 0);
                }
                
                //set the target for the computer AI
                @Override
                public void chooseTargetAI() {
                    CardList list = getComputerCreatures();
                    
                    if(0 < list.size()) setTargetCard(list.get(0));
                    else
                    //the computer doesn't have any other creatures
                    setTargetCard(null);
                }
                
                CardList getComputerCreatures() {
                    CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
                    CardList out = list.getType("Creature");
                    return out;
                }//getCreatures
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    if(c != null && AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addCounter(Counters.P1P1, 5);
                    }
                }//resolve()
            };
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
            
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = -2823505283781217181L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
                    else if(ability.canPlayAI()) {
                        ability.chooseTargetAI();
                        //need to add this to the stack
                        AllZone.Stack.add(ability);
                    }
                    
                }//execute()
            };//Command
            
            card.addDestroyCommand(leavesPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sower of Temptation")) {
            final Card movedCreature[] = new Card[1];
            
            final CommandReturn getCreature = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                	CardList list = AllZoneUtil.getCreaturesInPlay();
                	list = list.filter(AllZoneUtil.getCanTargetFilter(card));
                    
                    //remove "this card"
                    list.remove(card);
                    
                    return list;
                }
            };//CommandReturn
            

            final SpellAbility comesAbility = new Ability(card, "0") {
                @Override
                public void resolve() {
                    //super.resolve();
                    
                    Card c = getTargetCard();
                    movedCreature[0] = c;
                    
                    if(AllZone.GameAction.isCardInPlay(card) && AllZone.GameAction.isCardInPlay(c) && 
                    		CardFactoryUtil.canTarget(card, c)) {
                        //set summoning sickness
                        if(c.getKeyword().contains("Haste")) {
                            c.setSickness(false);
                        } else {
                            c.setSickness(true);
                        }
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                        
                        c.setSickness(true);
                        c.setController(card.getController());
                        
                        PlayerZone from = AllZone.getZone(c);
                        from.remove(c);
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                        to.add(c);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                    }
                }//resolve()
            };//SpellAbility
            
            final Input inputComes = new Input() {
                private static final long serialVersionUID = -8449238833091942579L;
                
                @Override
                public void showMessage() {
                    CardList choice = (CardList) getCreature.execute();
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(comesAbility, choice,
                            "Select target creature to gain control of: ", true, false));
                    ButtonUtil.disableAll();//to disable the Cancel button
                }
            };
            
            final Command commandCIP = new Command() {
                private static final long serialVersionUID = -5675532512302863456L;
                
                public void execute() {
                    CardList creature = (CardList) getCreature.execute();
                    Player s = card.getController();
                    if(creature.size() == 0) return;
                    else if(s.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(inputComes);
                    else //computer
                    {
                        Card target;
                        //try to target human creature
                        CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                        target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty
                        
                        if(target == null) {
                            //must target computer creature
                            CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
                            computer = computer.getType("Creature");
                            computer.remove(card);
                            
                            computer.shuffle();
                            if(computer.size() != 0) target = computer.get(0);
                        }
                        comesAbility.setTargetCard(target);
                        AllZone.Stack.add(comesAbility);
                    }//else
                }//execute()
            };//CommandComes
            card.addComesIntoPlayCommand(commandCIP);
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.addLeavesPlayCommand(new Command() {
                private static final long serialVersionUID = 6737424952039552060L;
                
                public void execute() {
                    Card c = movedCreature[0];
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                        
                        c.setSickness(true);
                        c.setController(c.getController().getOpponent());
                        
                        PlayerZone from = AllZone.getZone(c);
                        from.remove(c);
                        
                        //make sure the creature is removed from combat:
                        CardList list = new CardList(AllZone.Combat.getAttackers());
                        if(list.contains(c)) AllZone.Combat.removeFromCombat(c);
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Battlefield, c.getOwner());
                        to.add(c);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                    }//if
                }//execute()
            });//Command
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -6810781646652311270L;
                
                @Override
                public boolean canPlay() {
                    CardList choice = (CardList) getCreature.execute();
                    return choice.size() > 0 && super.canPlay();
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList c = CardFactoryUtil.AI_getHumanCreature(card, true);
                    CardListUtil.sortAttack(c);
                    CardListUtil.sortFlying(c);
                    
                    if(c.isEmpty()) return false;
                    
                    if( c.get(0).getNetAttack() >= 2 && c.get(0).getKeyword().contains("Flying"))
                        return true;
                    
                    CardListUtil.sortAttack(c);
                    if(4 <= c.get(0).getNetAttack())
                        return true;
                    
                    return false;
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Painter's Servant")) {
        	final long[] timeStamp = new long[1];
        	final String[] color = new String[1];
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        String[] colors = Constant.Color.onlyColors;
                        
                        Object o = AllZone.Display.getChoice("Choose color", colors);
                        color[0] = (String) o;
                        card.setChosenColor(color[0]);
                    } else { 
                    	// AI chooses the color that appears in the keywords of the most cards in its deck, hand and on battlefield
                    	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer);
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
                        CardList list = new CardList();
                        list.addAll(lib.getCards());
                        list.addAll(hand.getCards());
                        list.addAll(AllZone.Computer_Battlefield.getCards());
                        
                        color[0] = Constant.Color.White;
                        int max =  list.getKeywordsContain(color[0]).size();
                        
                        String[] colors = { Constant.Color.Blue, Constant.Color.Black, Constant.Color.Red, Constant.Color.Green };
                        for(String c : colors){
	                        int cmp = list.getKeywordsContain(c).size();
	                        if (cmp > max){
	                        	max = cmp;
	                        	color[0] = c;
	                        }
                        }
                        card.setChosenColor(color[0]);
                    }
                    
                    String s = CardUtil.getShortColor(color[0]);
                    timeStamp[0] = AllZone.GameInfo.addColorChanges(s, card, true, true);
                }
            };
            
            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 333134223161L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };//Command

            final Ability unpaint = new Ability(card, "0") {
            	public void resolve(){
            		String s = CardUtil.getShortColor(color[0]);
            		AllZone.GameInfo.removeColorChanges(s, card, true, timeStamp[0]);
            	}
            };
            
            Command leavesBattlefield = new Command() {
				private static final long serialVersionUID = 2559212590399132459L;

				public void execute(){
            		AllZone.Stack.add(unpaint);
            	}
            };
 
            ability.setStackDescription("As Painter's Servant enters the battlefield, choose a color.");
            unpaint.setStackDescription("Painter's Servant left the battlefield, resetting colors.");
            card.addComesIntoPlayCommand(comesIntoPlay);
            card.addLeavesPlayCommand(leavesBattlefield);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Goblin Skycutter")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return getFlying().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    AllZone.GameAction.sacrifice(card);
                    
                    CardList flying = getFlying();
                    flying.shuffle();
                    setTargetCard(flying.get(0));
                }
                
                CardList getFlying() {
                    CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
                    flying = flying.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getNetDefense() == 2;
                        }
                    });
                    return flying;
                }//getFlying()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        getTargetCard().addDamage(2, card);
                        getTargetCard().removeIntrinsicKeyword("Flying");
                        getTargetCard().removeExtrinsicKeyword("Flying");
                    }
                    
                    AllZone.EndOfTurn.addUntil(new Command() {
                        private static final long serialVersionUID = -8889549737746466810L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(getTargetCard())) getTargetCard().addIntrinsicKeyword(
                                    "Flying");
                        }
                    });
                }//resolve()
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 8609211991425118222L;
                
                @Override
                public void showMessage() {
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getKeyword().contains("Flying")
                                    && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list,
                            "Select a creature with flying to deal 2 damage to", new Command() {
                                private static final long serialVersionUID = -3287971244881855563L;
                                
                                public void execute() {
                                    AllZone.GameAction.sacrifice(card);
                                }
                            }, true, false));
                }//showMessage()
            };//Input
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Sacrifice Goblin Skycutter: Goblin Skycutter deals 2 damage to target ");
            sb.append("creature with flying. That creature loses flying until end of turn.");
            ability.setDescription(sb.toString());
            
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if(cardName.equals("Transluminant")) {
            final Command atEOT = new Command() {
                private static final long serialVersionUID = -5126793112740563180L;
                
                public void execute() {
                    CardFactoryUtil.makeToken("Spirit", "W 1 1 Spirit", card.getController(), "W", new String[] {
                            "Creature", "Spirit"}, 1, 1, new String[] {"Flying"});
                }//execute()
            };//Command
            
            final Ability ability = new Ability(card, "W") {
                @Override
                public boolean canPlayAI() { /*
                                             CardList list = new CardList(AllZone.Human_Play.getCards());
                                             list = list.getType("Creature");

                                             String phase = AllZone.Phase.getPhase();
                                             return phase.equals(Constant.Phase.Main2) && list.size() != 0;
                                             */
                    return false;
                }
                
                @Override
                public void chooseTargetAI() {
                    AllZone.GameAction.sacrifice(card);
                }
                
                @Override
                public void resolve() {
                    AllZone.EndOfTurn.addAt(atEOT);
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription("W, Sacrifice Transluminant: Put a 1/1 white Spirit creature token with flying onto the battlefield at the beginning of the next end step.");
            ability.setStackDescription("Put a 1/1 white Spirit creature token with flying onto the battlefield at end of turn.");
            ability.setBeforePayMana(new Input_PayManaCost_Ability(ability.getManaCost(), new Command() {
                private static final long serialVersionUID = -6553009833190713980L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                    AllZone.Stack.add(ability);
                }
            }));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Memnarch")) {
            //has 2 non-tap abilities that affect itself
            final SpellAbility ability1 = new Ability(card, "1 U U") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        Card crd = getTargetCard();
                        ArrayList<String> types = crd.getType();
                        crd.setType(new ArrayList<String>()); //clear
                        getTargetCard().addType("Artifact"); //make sure artifact is at the beginning
                        for(String type:types)
                            crd.addType(type);
                        
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = getCreature();
                    return list.size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }//chooseTargetAI()
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Human_Battlefield.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (!c.isArtifact()) && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    return list;
                }//getCreature()
            };//SpellAbility
            
            card.addSpellAbility(ability1);
            ability1.setDescription("1 U U: Target permanent becomes an artifact in addition to its other types. (This effect doesn't end at end of turn.)");
            ability1.setBeforePayMana(CardFactoryUtil.input_targetType(ability1, "All"));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Stangg")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList cl = CardFactoryUtil.makeToken("Stangg Twin", "RG 3 4 Stangg Twin", card.getController(), "R G",
                            new String[] {"Legendary", "Creature", "Human", "Warrior"}, 3, 4, new String[] {""});
                    
                    cl.get(0).addLeavesPlayCommand(new Command() {
                        private static final long serialVersionUID = 3367390368512271319L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(card)) AllZone.GameAction.sacrifice(card);
                        }
                    });
                }
            };
            ability.setStackDescription("When Stangg enters the battlefield, if Stangg is on the battlefield, put a legendary 3/4 red and green Human Warrior creature token named Stangg Twin onto the battlefield.");
            
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 6667896040611028600L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            });
            
            card.addLeavesPlayCommand(new Command() {
                private static final long serialVersionUID = 1786900359843939456L;
                
                public void execute() {
                    CardList list = AllZoneUtil.getCardsInPlay("Stangg Twin");
                    
                    if(list.size() == 1) AllZone.GameAction.exile(list.get(0));
                }
            });
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Goldmeadow Lookout")) {
        	final Ability_Cost lookCost = new Ability_Cost("W T Discard<1/Any>", card.getName(), true);
        	final SpellAbility ability = new Ability_Activated(card, lookCost, null){
                private static final long serialVersionUID = -8413409735529340094L;
                
                @Override
                public void resolve() {
                    makeToken();
                }
                
                void makeToken() {
                    CardList cl = CardFactoryUtil.makeToken("Goldmeadow Harrier", "W 1 1 Goldmeadow Harrier",
                    		card.getController(), "W", new String[] {"Creature", "Kithkin", "Soldier"}, 1, 1, new String[] {""});
                    
                    for(final Card c:cl) {
                    	final Ability_Cost abCost = new Ability_Cost("W T", c.getName(), true);
                    	final Target tgt = new Target("TgtC");
                    	final SpellAbility tokenAbility = new Ability_Activated(card, abCost, tgt){
                            private static final long serialVersionUID = -7327585136675896817L;
                            
                            @Override
                            public void resolve() {
                                Card c = getTargetCard();
                                c.tap();
                            }
                            
                            @Override
                            public boolean canPlayAI() {
                            	CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                            	human = human.filter(new CardListFilter() {
                            		public boolean addCard(Card c) {
                            			return c.isUntapped() && CardFactoryUtil.canTarget(card, c);
                            		}
                            	});
                            	
                                if (human.size() > 0) {
                                	CardListUtil.sortAttack(human);
                                    CardListUtil.sortFlying(human);
                                	setTargetCard(human.get(0));
                                }
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                                CardList assassins = new CardList();
                                assassins.addAll(play.getCards());
                                
                                assassins = assassins.filter(new CardListFilter() {
                                    public boolean addCard(Card c) {
                                        return c.isCreature() && (!c.hasSickness() || c.getKeyword().contains("Haste")) && c.isUntapped() && 
                                              (c.getName().equals("Rathi Assassin") || c.getName().equals("Royal Assassin") || 
                                               c.getName().equals("Tetsuo Umezawa") || c.getName().equals("Stalking Assassin"));
                                    }
                                });
                                
                                Combat attackers = ComputerUtil.getAttackers();
                                CardList list = new CardList(attackers.getAttackers());
                            	
                                return (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getPlayerTurn().equals(card.getController()) && 
                                		human.size() > 0 && (assassins.size() > 0 || !list.contains(card)));
                                
                            }//canPlayAI
                        };//SpellAbility
                        c.addSpellAbility(new Spell_Permanent(c));
                        c.addSpellAbility(tokenAbility);
                        tokenAbility.setDescription("W, tap: Tap target creature.");
                    }
                    
                }//makeToken()
                
                @Override
                public boolean canPlayAI() {
                    return super.canPlayAI() && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
                }
            };//SpellAbility
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append("W, tap, Discard a card: Put a 1/1 white Kithkin Soldier creature token named ");
            sb.append("Goldmeadow Harrier onto the battlefield. It has \"W, tap : Tap target creature.\"");
            ability.setDescription(sb.toString());
            
            ability.setStackDescription(cardName+" - Put a 1/1 token onto the battlefield");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Horde of Notions")) {
            final Ability ability = new Ability(card, "W U B R G") {
                @Override
                public void resolve() {
                    Card c = null;
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select Elemental", getCreatures());
                        c = (Card) o;
                        
                    } else {
                        c = getAIElemental();
                    }
                    
                    PlayerZone grave = AllZone.getZone(c);
                    
                    if(AllZone.GameAction.isCardInZone(c, grave)) {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getController());
                        AllZone.GameAction.moveTo(play, c);
                    }
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    return getCreatures().length != 0 && AllZone.GameAction.isCardInPlay(card) && super.canPlay();
                }
                
                public Card[] getCreatures() {
                    CardList creature = new CardList();
                    PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    creature.addAll(zone.getCards());
                    creature = creature.getType("Elemental");
                    return creature.toArray();
                }
                
                public Card getAIElemental() {
                    Card c[] = getCreatures();
                    Card biggest = c[0];
                    for(int i = 0; i < c.length; i++)
                        if(biggest.getNetAttack() < c[i].getNetAttack()) biggest = c[i];
                    
                    return biggest;
                }
            };//SpellAbility
            card.addSpellAbility(ability);
            
            ability.setDescription("W U B R G: You may play target Elemental card from your graveyard without paying its mana cost.");
            ability.setStackDescription("Horde of Notions - play Elemental card from graveyard without paying its mana cost.");
            ability.setBeforePayMana(new Input_PayManaCost(ability));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Ravenous Rats") || cardName.equals("Corrupt Court Official")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return true;
                }
                
                @Override
                public void resolve() {
                	getTargetPlayer().discard(this);
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -2028008593708491452L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
                        ButtonUtil.disableAll();
                    } else//computer
                    {
                        ability.setTargetPlayer(AllZone.HumanPlayer);
                        AllZone.Stack.add(ability);
                    }//else
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Rhys the Redeemed")) {
           
        	Ability_Cost abCost = new Ability_Cost("4 GW GW T", card.getName(), true);
            final Ability_Activated copyTokens1 = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = 6297992502069547478L;
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList allTokens = new CardList();
                    allTokens.addAll(play.getCards());
                    allTokens = allTokens.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.isToken();
                        }
                    });
                    
                    int multiplier = AllZoneUtil.getDoublingSeasonMagnitude(card.getController());
                    
                    for(int i = 0; i < allTokens.size(); i++) {
                        Card c = allTokens.get(i);
                        for(int j = 0; j < multiplier; j++)
                            copyToken(c);
                    }
                }
                
                public void copyToken(Card token) {
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
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    play.add(copy);
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                    CardList tokens = new CardList(play.getCards());
                    tokens = tokens.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            return c.isToken();
                        }
                        
                    });
                    return tokens.size() >= 2;
                }
            };
            
            card.addSpellAbility(copyTokens1);
            copyTokens1.setDescription(abCost+"For each creature token you control, put a token that's a copy of that creature onto the battlefield.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - For each creature token you control, put a token that's a copy of that creature onto the battlefield.");
            copyTokens1.setStackDescription(sb.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Thelonite Hermit")) {
            
            Command turnsFaceUp = new Command() {
                private static final long serialVersionUID = -3882798504865405413L;
                
                public void execute() {
                	for(int i = 0; i < 4; i++)
                		CardFactoryUtil.makeTokenSaproling(card.getController());
                }//execute()
            };//Command
            card.addTurnFaceUpCommand(turnsFaceUp);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Imperial Hellkite")) {
            Command turnsFaceUp = new Command() {
                private static final long serialVersionUID = -1407485989096862288L;
                
                public void execute() {
                	final Player player = card.getController();
                    CardList list = AllZoneUtil.getPlayerCardsInLibrary(player);
                    list = list.getType("Dragon");
                    
                    if(list.size() == 0) return;
                    
                    Card dragon = null;
                    if(player.isComputer()) {
                        dragon = CardFactoryUtil.AI_getBestCreature(list);
                    } else //human
                    {
                        Object o = AllZone.Display.getChoiceOptional("Select Dragon", list.toArray());
                        dragon = (Card) o;
                    }
                    AllZone.GameAction.moveToHand(dragon);
                    card.getController().shuffle();
                }//execute()
            };//Command
            
            card.addTurnFaceUpCommand(turnsFaceUp);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Serra Avatar")) {
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = -2274397219668820020L;
                
                public void execute() {
                    //moveto library
                    PlayerZone libraryZone = AllZone.getZone(Constant.Zone.Library, card.getOwner());
                    AllZone.GameAction.moveTo(libraryZone, card);
                    //shuffle library
                    card.getOwner().shuffle();
                }//execute()
            };//Command
            card.addDestroyCommand(leavesPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Pestermite")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        if(c.isTapped()) c.untap();
                        else c.tap();
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 5202575895575352408L;
                
                public void execute() {
                    CardList all = AllZoneUtil.getCardsInPlay();
                    
                    CardList hum = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
                    
                    if(all.size() != 0) {
                        if(card.getController().isHuman()) {
                            AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, all,
                                    "Select target permanent to tap/untap.", true, false));
                            ButtonUtil.enableAll();
                        } else {
                            Card human = CardFactoryUtil.AI_getBestCreature(hum);
                            ability.setTargetCard(human);
                            AllZone.Stack.add(ability);
                        }
                    }
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -3055232264358172133L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped();
                        }
                    });
                    
                    return (list.size() > 0) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mystic Snake")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(AllZone.Stack.size() > 0) {
                        SpellAbility sa = AllZone.Stack.peek();
                        if(sa.isSpell() && CardFactoryUtil.isCounterable(sa.getSourceCard())) {
                            sa = AllZone.Stack.pop();
                            AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                        }
                    }
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -6564365394043612388L;
                
                public void execute() {
                    if(AllZone.Stack.size() > 0) {
                    	StringBuilder sb = new StringBuilder();
                    	sb.append("Mystic Snake counters ").append(AllZone.Stack.peek().getSourceCard().getName());
                    	ability.setStackDescription(sb.toString());
                        
                        AllZone.Stack.add(ability);
                    }
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 6440845807532409545L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Trinket Mage")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        hand.add(c);
                        
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = 4022442363194287539L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList arts = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Artifact")
                                && CardUtil.getConvertedManaCost(cards.get(i).getManaCost()) <= 1) {
                            arts.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(arts.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", arts.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Goblin Matron")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        hand.add(c);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = 4022442363194287539L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList goblins = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).isType("Goblin")) {
                            goblins.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(goblins.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", goblins.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Kithkin Harbinger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        lib.add(c, 0);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4022442363194287539L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList kithkin = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).isType("Kithkin")) {
                            kithkin.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(kithkin.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", kithkin.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Treefolk Harbinger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        lib.add(c, 0);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9170723718484515120L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList treefolkForests = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if((cards.get(i).getType().contains("Treefolk") || cards.get(i).getKeyword().contains(
                                "Changeling"))
                                || cards.get(i).getType().contains("Forest")) {
                            treefolkForests.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(treefolkForests.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card",
                                treefolkForests.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Archon of Justice")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            AllZone.GameAction.exile(getTargetCard());
                        }
                    }
                }
            };
            ability.setStackDescription("Archon of Justice - Exile target permanent.");
            
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = 7552566264976488465L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_targetPermanent(ability));
                    else {
                        /*
                         *  if computer controlled Archon of Justice have it select the best creature, or enchantment, 
                         *  or artifact, whatever the human controllers, and as a last option a card it controls.
                         */
                        Card temp;
                        
                        CardList human_list = new CardList(AllZone.Human_Battlefield.getCards());
                        human_list.remove("Mana Pool");
                        temp = CardFactoryUtil.AI_getBestCreature(human_list);
                        if(temp != null) ability.setTargetCard(CardFactoryUtil.AI_getBestCreature(human_list));
                        if(ability.getTargetCard() == null) {
                            temp = CardFactoryUtil.AI_getBestEnchantment(human_list, card, false);
                            if(temp != null) ability.setTargetCard(CardFactoryUtil.AI_getBestEnchantment(
                                    human_list, card, true));
                        }
                        if(ability.getTargetCard() == null) {
                            temp = CardFactoryUtil.AI_getBestArtifact(human_list);
                            if(temp != null) ability.setTargetCard(CardFactoryUtil.AI_getBestArtifact(human_list));
                        }
                        if(ability.getTargetCard() == null) {
                            if(human_list.size() == 0) {
                                CardList computer_list = new CardList(AllZone.Computer_Battlefield.getCards());
                                if(computer_list.size() == 0) {
                                    return; //we have nothing in play to destroy.
                                } else {
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
        else if(cardName.equals("Knight of the White Orchid")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    

                    CardList basic = new CardList(lib.getCards());
                    basic = basic.getType("Plains");
                    

                    if(card.getController().equals(AllZone.ComputerPlayer)) {
                        if(basic.size() > 0) {
                            Card c = basic.get(0);
                            lib.remove(c);
                            play.add(c);
                            
                        }
                    } else // human
                    {
                        if(basic.size() > 0) {
                            Object o = AllZone.Display.getChoiceOptional("Select Plains card to put onto the battlefield: ",
                                    basic.toArray());
                            if(o != null) {
                                Card c = (Card) o;
                                lib.remove(c);
                                play.add(c);
                            }
                        }
                    }
                    card.getController().shuffle();
                }//resolve()
                
            };//Ability
            
            Command fetchBasicLand = new Command() {
                
                private static final long serialVersionUID = -1086551054597721988L;
                
                public void execute() {
                	Player player = card.getController();
                    PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Battlefield,
                            player.getOpponent());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
                    
                    CardList self = new CardList(play.getCards());
                    CardList opp = new CardList(oppPlay.getCards());
                    
                    self = self.getType("Land");
                    opp = opp.getType("Land");
                    
                    if(self.size() < opp.size()) {
                    	StringBuilder sb = new StringBuilder();
                    	sb.append(card.getName()).append(" - search library for a plains and put it onto the battlefield");
                    	ability.setStackDescription(sb.toString());
                        
                        AllZone.Stack.add(ability);
                    }
                }
            };
            
            card.addComesIntoPlayCommand(fetchBasicLand);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Affa Guard Hound")) {
        	final CommandReturn getCreature = new CommandReturn() {
        		//get target card, may be null
        		public Object execute() {
        			Combat combat = ComputerUtil.getAttackers();
        			Card[] c = combat.getAttackers();
        			CardList list = new CardList();

        			if(c.length == 0) {
        				list.addAll(AllZone.Computer_Battlefield.getCards());
        				list = list.filter(new CardListFilter() {
        					public boolean addCard(Card c) {
        						return c.isCreature();
        					}
        				});

        				if(list.size() == 0) return card;
        				else {
        					CardListUtil.sortAttack(list);
        					CardListUtil.sortFlying(list);

        					for(int i = 0; i < list.size(); i++)
        						if(list.get(i).isUntapped()) return list.get(i);

        					return list.get(0);
        				}
        			}

        			return c[0];
        		}//execute()
        	};//CommandReturn

        	final SpellAbility ability = new Ability(card, "0") {
        		@Override
        		public void resolve() {
        			final Card c = getTargetCard();

        			if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
        				c.addTempDefenseBoost(3);

        				AllZone.EndOfTurn.addUntil(new Command() {
        					private static final long serialVersionUID = -6478141025919509688L;

        					public void execute() {
        						c.addTempDefenseBoost(-3);
        					}
        				});
        			}//if
        		}//resolve()
        	};//SpellAbility
        	Command intoPlay = new Command() {
        		private static final long serialVersionUID = -4514602963470596654L;

        		public void execute() {
        			if(card.getController().equals(AllZone.HumanPlayer)) {
        				AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(ability));
        			} else//computer
        			{
        				Object o = getCreature.execute();
        				if(o != null)//should never happen, but just in case
        				{
        					ability.setTargetCard((Card) o);
        					AllZone.Stack.add(ability);
        				}
        			}//else
        		}//execute()
        	};
        	card.addComesIntoPlayCommand(intoPlay);

        	card.setSVar("PlayMain1", "TRUE");

        	card.clearSpellAbility();
        	card.addSpellAbility(new Spell_Permanent(card) {
        		private static final long serialVersionUID = 7153795935713327863L;

        		@Override
        		public boolean canPlayAI() {
        			Object o = getCreature.execute();

        			return (o != null) && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
        		}
        	});
        }//*************** END ************ END **************************         

        
        //*************** START *********** START **************************
        else if(cardName.equals("Pallid Mycoderm")) {
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 3400057700040211691L;
                public boolean            firstTime        = true;
                
                public void execute() {
                    
                    if(firstTime) {
                        card.setCounter(Counters.SPORE, 0, false);
                    }
                    firstTime = false;
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            
            final SpellAbility a2 = new Ability(card, "0") {
            	final Command eot1 = new Command() {
            		private static final long serialVersionUID = -4485431571276851181L;

            		public void execute() {
            			Player player = card.getController();
            			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);

            			CardList creats = new CardList(play.getCards());
            			creats = creats.getType("Creature");

            			for(int i = 0; i < creats.size(); i++) {
            				Card creat = creats.get(i);

            				if(creat.getType().contains("Fungus")
            						|| creat.getType().contains("Saproling")
            						|| creat.getKeyword().contains("Changeling")) {
            					creat.addTempAttackBoost(-1);
            					creat.addTempDefenseBoost(-1);
            				}
            			}

            		}
            	};
                
                @Override
                public void resolve() {
                    //get all player controls saprolings:
                	Player player = card.getController();
                    CardList creats = AllZoneUtil.getCreaturesInPlay(player);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.GameAction.sacrifice(c);
                        

                        for(int i = 0; i < creats.size(); i++) {
                            Card creat = creats.get(i);
                            
                            if(creat.isType("Fungus") || creat.isType("Saproling")) {
                                creat.addTempAttackBoost(1);
                                creat.addTempDefenseBoost(1);
                            }
                        }
                        
                    }
                    AllZone.EndOfTurn.addUntil(eot1);
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 6754180514935882692L;
                
                @Override
                public void showMessage() {
                    CardList saps = new CardList(
                            AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
                    saps = saps.getType("Saproling");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2, saps, "Select a Saproling to sacrifice.",
                            false, false));
                }
            };
            
            card.addSpellAbility(a2);
            a2.setDescription("Sacrifice a Saproling: Each Fungus and each Saproling you control gets +1/+1 until end of turn");
            a2.setStackDescription("Saprolings and Fungi you control get +1/+1 until end of turn.");
            
            a2.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Rootwater Thief")) {
            final Ability ability2 = new Ability(card, "2") {
                @Override
                public void resolve() {
                	Player opponent = card.getController().getOpponent();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, opponent);
                    CardList cards = new CardList(lib.getCards());
                    
                    if(cards.size() > 0) {
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            Object o = AllZone.Display.getChoiceOptional("Select card to remove: ",
                                    cards.toArray());
                            Card c = (Card) o;
                            AllZone.GameAction.exile(c);
                            opponent.shuffle();
                        } else {
                            Card c = lib.get(0);
                            AllZone.GameAction.exile(c);
                            opponent.shuffle();
                        }
                    }
                    
                }
                
                @Override
                public boolean canPlay() {
                    //this is set to false, since it should only TRIGGER
                    return false;
                }
            };// ability2
            card.addSpellAbility(ability2);
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - search opponent's library and remove a card from game.");
            ability2.setStackDescription(sb2.toString());
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Oros, the Avenger")) {
            final Ability ability2 = new Ability(card, "2 W") {
                @Override
                public void resolve() {
                    CardList cards = AllZoneUtil.getCreaturesInPlay();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(!(cards.get(i)).isWhite()
                                && CardFactoryUtil.canDamage(card, cards.get(i))) {
                            cards.get(i).addDamage(3, card);
                        }
                    }
                }
                
                @Override
                public boolean canPlay() {
                    //this is set to false, since it should only TRIGGER
                    return false;
                }
            };// ability2
            //card.clearSpellAbility();
            card.addSpellAbility(ability2);
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - deals 3 damage to each nonwhite creature.");
            ability2.setStackDescription(sb2.toString());
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Treva, the Renewer")) {
            final Player player = card.getController();
            
            final Ability ability2 = new Ability(card, "2 W") {
                @Override
                public void resolve() {
                    int lifeGain = 0;
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        String choices[] = {"white", "blue", "black", "red", "green"};
                        Object o = AllZone.Display.getChoiceOptional("Select Color: ", choices);
                        Log.debug("Treva, the Renewer", "Color:" + o);
                        lifeGain = CardFactoryUtil.getNumberOfPermanentsByColor((String) o);
                        
                    } else {
                        CardList list = AllZoneUtil.getCardsInPlay();
                        String color = CardFactoryUtil.getMostProminentColor(list);
                        lifeGain = CardFactoryUtil.getNumberOfPermanentsByColor(color);
                    }
                    
                    card.getController().gainLife(lifeGain, card);
                }
                
                @Override
                public boolean canPlay() {
                    //this is set to false, since it should only TRIGGER
                    return false;
                }
            };// ability2
            //card.clearSpellAbility();
            card.addSpellAbility(ability2);
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - ").append(player);
            sb2.append(" gains life equal to permanents of the chosen color.");
            ability2.setStackDescription(sb2.toString());
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Rith, the Awakener")) {
            final Player player = card.getController();
            
            final Ability ability2 = new Ability(card, "2 G") {
                @Override
                public void resolve() {
                    int numberTokens = 0;
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        String choices[] = {"white", "blue", "black", "red", "green"};
                        Object o = AllZone.Display.getChoiceOptional("Select Color: ", choices);
                        //System.out.println("Color:" + o);
                        numberTokens = CardFactoryUtil.getNumberOfPermanentsByColor((String) o);
                    } else {
                        CardList list = AllZoneUtil.getCardsInPlay();
                        String color = CardFactoryUtil.getMostProminentColor(list);
                        numberTokens = CardFactoryUtil.getNumberOfPermanentsByColor(color);
                    }
                    
                    for(int i = 0; i < numberTokens; i++) {
                        CardFactoryUtil.makeTokenSaproling(card.getController());
                    }
                }
                
                @Override
                public boolean canPlay() {
                    //this is set to false, since it should only TRIGGER
                    return false;
                }
            };// ability2
            //card.clearSpellAbility();
            card.addSpellAbility(ability2);
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - ").append(player);
            sb2.append(" puts a 1/1 green Saproling creature token onto the battlefield for each permanent of the chosen color");
            ability2.setStackDescription(sb2.toString());
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Vorosh, the Hunter")) {
            
            final Ability ability2 = new Ability(card, "2 G") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, 6);
                }
                
                @Override
                public boolean canPlay() {
                    //this is set to false, since it should only TRIGGER
                    return false;
                }
            };// ability2
            //card.clearSpellAbility();
            card.addSpellAbility(ability2);
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - gets six +1/+1 counters.");
            ability2.setStackDescription(sb2.toString());
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************  
        else if(cardName.equals("Anodet Lurker")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().gainLife(3, card);
                }
            };
            
            Command gain3Life = new Command() {
                private static final long serialVersionUID = 9156307402354672176L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - Gain 3 life.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(gain3Life);
        }//*************** END ************ END **************************  
        

        //*************** START *********** START **************************  
        else if(cardName.equals("Tarpan")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().gainLife(1, card);
                }
            };
            
            Command gain1Life = new Command() {
                private static final long serialVersionUID = 206350020224577500L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - Gain 1 life.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(gain1Life);
        }//*************** END ************ END **************************  
        
        
        //*************** START *********** START **************************  
        else if(cardName.equals("Onulet")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	card.getController().gainLife(2, card);
                }
            };//Ability
            
            Command gain2Life = new Command() {
                private static final long serialVersionUID = 7840609060047275126L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - Gain 2 life.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(gain2Life);
        }//*************** END ************ END **************************  
        
        
        //*************** START *********** START **************************  
        else if(cardName.equals("Sprouting Thrinax")) {
            
            final Ability ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                	for(int i = 0; i < 3; i++)
                		CardFactoryUtil.makeTokenSaproling(card.getController());
                }//resolve()
            };//Ability
            
            Command make3Tokens = new Command() {
                private static final long serialVersionUID = 5246587197020320581L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - put three 1/1 Saproling creature tokens onto the battlefield.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(make3Tokens);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Wurmcoil Engine")) {
            final Ability ability = new Ability(card, "0") {
                
                @Override
                public void resolve() {
                    makeTokens();
                }//resolve()
                
                void makeTokens() {
                    CardFactoryUtil.makeToken("Wurm", "C 3 3 Wurm Deathtouch", card.getController(), "", new String[] {
                            "Artifact", "Creature", "Wurm"}, 3, 3, new String[] {"Deathtouch"});
                    CardFactoryUtil.makeToken("Wurm", "C 3 3 Wurm Lifelink", card.getController(), "", new String[] {
                            "Artifact", "Creature", "Wurm"}, 3, 3, new String[] {"Lifelink"});
                }//makeToken()
            };//Ability
            
            Command makeTokens = new Command() {
                
                private static final long serialVersionUID = 8458814538376248271L;

                public void execute() {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - put creature tokens onto the battlefield.");
                    ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };//Command
            
            card.addDestroyCommand(makeTokens);
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Solemn Simulacrum") || cardName.equals("Yavimaya Granger")
        		|| cardName.equals("Ondu Giant") || cardName.equals("Quirion Trailblazer")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    CardList basic = new CardList(lib.getCards());
                    basic = basic.getType("Basic");
                    
                    if(card.getController().equals(AllZone.ComputerPlayer)) {
                        if(basic.size() > 0) {
                            Card c = basic.get(0);
                            lib.remove(c);
                            play.add(c);
                            c.tap();
                            
                        }
                    } else // human
                    {
                        if(basic.size() > 0) {
                            Object o = AllZone.Display.getChoiceOptional(
                                    "Select Basic Land card to put onto the battlefield tapped: ", basic.toArray());
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
            
            Command fetchBasicLand = new Command() {
                private static final long serialVersionUID = -7912757481694029348L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - search library for a basic land card and put it onto the battlefield tapped.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            final Ability ability2 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    
                	card.getController().drawCard();
                }//resolve()
            };//Ability
            
            Command draw = new Command() {
                private static final long serialVersionUID = -549395102229088642L;
                
                public void execute() {
                	StringBuilder sb2 = new StringBuilder();
                	sb2.append(card.getName()).append(" - Draw a card.");
                	ability2.setStackDescription(sb2.toString());
                    
                    AllZone.Stack.add(ability2);
                }
            };
            
            if(cardName.equals("Solemn Simulacrum")) card.addDestroyCommand(draw);
            card.addComesIntoPlayCommand(fetchBasicLand);
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Cromat")) {
            //Kill ability
            
            final Ability a2 = new Ability(card, "W B") {
                @Override
                public boolean canPlay() {
                    return (AllZone.GameAction.isCardInPlay(card) && (AllZone.Combat.isBlocked(card) || AllZone.Combat.getAllBlockers().contains(
                            card)) && super.canPlay());
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                    //TODO
                }
                
                @Override
                public void resolve() {
                    AllZone.GameAction.destroy(getTargetCard());
                }//resolve()
            };
            Input runtime2 = new Input() {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void showMessage() {
                    CardList targets = new CardList();
                    if(AllZone.Combat.isBlocked(card) || AllZone.Combat.getAllBlockers().contains(card)) {
                        if(AllZone.Combat.isBlocked(card)) {
                            targets = AllZone.Combat.getBlockers(card);
                        } else {
                            targets = new CardList();
                            for(Card c:AllZone.Combat.getAttackers()) {
                                if(AllZone.Combat.isBlocked(c)) if(AllZone.Combat.getBlockers(c).contains(card)) targets.add(c);
                            }
                        }
                    }
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2, targets,
                            "Select target blocking or blocked by Cromat.", true, false));
                }
            };
            card.addSpellAbility(a2);
            a2.setBeforePayMana(runtime2);
            a2.setDescription("W B: Destroy target creature blocking or blocked by Cromat.");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sphinx of Jwar Isle")) {
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                	Player player = card.getController();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    
                    if(lib.size() < 1) return;
                    
                    CardList cl = new CardList();
                    cl.add(lib.get(0));
                    
                    AllZone.Display.getChoiceOptional("Top card", cl.toArray());
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility
            
            StringBuilder sb1 = new StringBuilder();
            sb1.append(card.getName()).append(" - look at top card of library.");
            ability1.setStackDescription(sb1.toString());
            
            ability1.setDescription("You may look at the top card of your library.");
            card.addSpellAbility(ability1);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Imperial Recruiter")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        hand.add(c, 0);
                        
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -8887306085997352723L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList powerTwoCreatures = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Creature") && (cards.get(i).getNetAttack() <= 2)) {
                            powerTwoCreatures.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(powerTwoCreatures.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card",
                                powerTwoCreatures.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
                    {
                    	if (powerTwoCreatures.getNotName("Imperial Recruiter").size() != 0) 
                    	{
                    		powerTwoCreatures = powerTwoCreatures.getNotName("Imperial Recruiter");
                    	}
                        powerTwoCreatures.shuffle();
                        ability.setTargetCard(powerTwoCreatures.get(0));
                        AllZone.Stack.add(ability);
                    }
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Maggot Carrier")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	card.getController().loseLife(1,card);
                	card.getController().getOpponent().loseLife(1,card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 685222802927427442L;
                
                public void execute() {
                    ability.setStackDescription("Maggot Carrier - everyone loses 1 life.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Rathi Fiend")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;
                    c.getController().loseLife(3,card);
                    c.getController().getOpponent().loseLife(3,card);
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 3362571791271852381L;
                
                public void execute() {
                    ability.setStackDescription("Rathi Fiend - everyone loses 3 life.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Dream Stalker") || cardName.equals("Kor Skyfisher")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.getZone(c).remove(c);
                        
                        if(!c.isToken()) {
                            Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
                            
                            newCard.setCurSetCode(c.getCurSetCode());
                            newCard.setImageFilename(c.getImageFilename());
                            
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 2045940121508110423L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList choice = new CardList(play.getCards());
                    choice = choice.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return !c.getName().equals("Mana Pool");
                    	}
                    });
                    AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, choice,
                            "Select a permanent you control.", false, false));
                    ButtonUtil.disableAll();
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 4802059067438200061L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Horned Kavu") || cardName.equals("Shivan Wurm")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.getZone(c).remove(c);
                        
                        if(!c.isToken()) {
                            Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
                            
                            newCard.setCurSetCode(c.getCurSetCode());
                            newCard.setImageFilename(c.getImageFilename());
                            
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7530032969328799083L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    CardList creatures = new CardList(play.getCards());
                    creatures = creatures.getType("Creature");
                    
                    CardList redGreen = new CardList();
                    

                    for(int i = 0; i < creatures.size(); i++) {
                        if((creatures.get(i)).isRed()) {
                            redGreen.add(creatures.get(i));
                        } else if((creatures.get(i)).isGreen()) {
                            redGreen.add(creatures.get(i));
                        }
                    }
                    
                    //Object o = AllZone.Display.getChoiceOptional("Select a creature card to bounce", blackBlue.toArray());
                    

                    AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, redGreen,
                            "Select a red or green creature you control.", false, false));
                    ButtonUtil.disableAll();
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -517667816379595978L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Silver Drake")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.getZone(c).remove(c);
                        
                        if(!c.isToken()) {
                            Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
                            
                            newCard.setCurSetCode(c.getCurSetCode());
                            newCard.setImageFilename(c.getImageFilename());
                            
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = -8473976122518500976L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    CardList creatures = new CardList(play.getCards());
                    creatures = creatures.getType("Creature");
                    
                    CardList whiteBlue = new CardList();
                    

                    for(int i = 0; i < creatures.size(); i++) {
                        if((creatures.get(i)).isWhite()) {
                            whiteBlue.add(creatures.get(i));
                        } else if((creatures.get(i)).isBlue()) {
                            whiteBlue.add(creatures.get(i));
                        }
                    }
                    
                    AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, whiteBlue,
                            "Select a white or blue creature you control.", false, false));
                    ButtonUtil.disableAll();
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            
            card.addSpellAbility(new Spell_Permanent(card) {
                
                private static final long serialVersionUID = -5048658151377675270L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Fleetfoot Panther") || cardName.equals("Steel Leaf Paladin")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.getZone(c).remove(c);
                        
                        if(!c.isToken()) {
                            Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
                            
                            newCard.setCurSetCode(c.getCurSetCode());
                            newCard.setImageFilename(c.getImageFilename());

                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 6575359591031318957L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    CardList creatures = new CardList(play.getCards());
                    creatures = creatures.getType("Creature");
                    
                    CardList greenWhite = new CardList();
                    
                    for(int i = 0; i < creatures.size(); i++) {
                        if((creatures.get(i)).isGreen()) {
                            greenWhite.add(creatures.get(i));
                        } else if((creatures.get(i)).isWhite()) {
                            greenWhite.add(creatures.get(i));
                        }
                    }
                    
                    AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, greenWhite,
                            "Select a green or white creature you control.", false, false));
                    ButtonUtil.disableAll();
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -1408300578781963711L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Stonecloaker")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.getZone(c).remove(c);
                        
                        if(!c.isToken()) {
                            Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
                            
                            newCard.setCurSetCode(c.getCurSetCode());
                            newCard.setImageFilename(c.getImageFilename());
                            
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -4018162972761688814L;
                
                public void execute() {
                	
                	PlayerZone hYard = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
                	PlayerZone cYard = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
                	CardList gravecards = new CardList();
                	String title;
                	
                	gravecards.addAll(cYard.getCards());
                	if (gravecards.isEmpty()) {
                		gravecards.addAll(hYard.getCards());
                		title = "Choose your card";
                	} else {
                		title = "Choose compy's card";
                	}
                    
                    //System.out.println("size of grave: " + gravecards.size());
                    
                    if(gravecards.size() > 0) {
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            Object o = AllZone.Display.getChoiceOptional(title, gravecards.toArray());
                            if(o != null) {
                                Card removedCard = (Card) o;
                                AllZone.GameAction.exile(removedCard);
                            }
                        } else {
                            AllZone.GameAction.exile(gravecards.get(0));
                        }
                    }
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    CardList creatures = new CardList(play.getCards());
                    creatures = creatures.getType("Creature");
                    
                    AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, creatures,
                            "Select a creature you control.", false, false));
                    ButtonUtil.disableAll();
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 3089921616375272120L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cavern Harpy")) {
            final SpellAbility a1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().loseLife(1,card);
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    if(card.isToken()) AllZone.getZone(card).remove(card);
                    else AllZone.GameAction.moveTo(hand, card);
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.getZone(c).remove(c);
                        
                        if(!c.isToken()) {
                            Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());

                            //TODO: Stop making new cards! Need a common card copier to fix multiple isues including images:
                            newCard.setCurSetCode(c.getCurSetCode());
                            newCard.setImageFilename(c.getImageFilename());
                            
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7855081477395863590L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    CardList creatures = new CardList(play.getCards());
                    creatures = creatures.getType("Creature");
                    
                    CardList blackBlue = new CardList();
                    

                    for(int i = 0; i < creatures.size(); i++) {
                        if(creatures.get(i).isBlack()) {
                            blackBlue.add(creatures.get(i));
                        } else if(creatures.get(i).isBlue()) {
                            blackBlue.add(creatures.get(i));
                        }
                    }
                    
                    //Object o = AllZone.Display.getChoiceOptional("Select a creature card to bounce", blackBlue.toArray());
                    

                    AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, blackBlue,
                            "Select blue or black creature you control.", false, false));
                    ButtonUtil.disableAll();
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -6750896183003809261L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            });
            
            card.addSpellAbility(a1);
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" pays 1 life and returns Cavern Harpy back to owner's hand.");
            a1.setStackDescription(sb.toString());
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Nemata, Grove Guardian")) {
            
        	final SpellAbility a2 = new Ability(card, "0") {
        		final Command eot1 = new Command() {
        			private static final long serialVersionUID = -389286901477839863L;

        			public void execute() {
        				CardList saps = AllZoneUtil.getTypeInPlay("Saproling");

        				for(int i = 0; i < saps.size(); i++) {
        					Card sap = saps.get(i);

        					sap.addTempAttackBoost(-1);
        					sap.addTempDefenseBoost(-1);
        				}

        			}
        		};
                
                @Override
                public void resolve() {
                    CardList saps = AllZoneUtil.getTypeInPlay("Saproling");
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.GameAction.sacrifice(c);
                        
                        for(int i = 0; i < saps.size(); i++) {
                            Card sap = saps.get(i);
                            
                            sap.addTempAttackBoost(1);
                            sap.addTempDefenseBoost(1);
                        }
                        
                    } else return;
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -8827919636559042903L;
                
                @Override
                public void showMessage() {
                    CardList saps = new CardList(AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
                    saps = saps.getType("Saproling");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2, saps, "Select a Saproling to sacrifice.",
                            false, false));
                }
            };
            
            final int[] numCreatures = new int[1];
            final Ability a3 = new Ability(card,"0")
            {
            	public void resolve()
            	{
            		CardList creats = new CardList(AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
            		creats = creats.getType("Saproling");
                    
            		List<Card> selection = AllZone.Display.getChoices("Select Saprolings to sacrifice", creats.toArray());
                    
                    numCreatures[0] = selection.size();
                    for(int m = 0; m < selection.size(); m++) {
                        AllZone.GameAction.sacrifice(selection.get(m));
                    }
                    
                    final Command eot1 = new Command() {
                        
						private static final long serialVersionUID = 5732420491509961333L;

						public void execute() {
                            CardList saps = new CardList();
                            saps.addAll(AllZone.Human_Battlefield.getCards());
                            saps.addAll(AllZone.Computer_Battlefield.getCards());
                            
                            saps = saps.getType("Saproling");
                            
                            for(int i = 0; i < saps.size(); i++) {
                                Card sap = saps.get(i);
                                
                                sap.addTempAttackBoost(-numCreatures[0]);
                                sap.addTempDefenseBoost(-numCreatures[0]);
                            }
                            
                        }
                    };
                    
                    CardList saps = AllZoneUtil.getTypeInPlay("Saproling");
                    for(int i = 0; i < saps.size(); i++) {
                        Card sap = saps.get(i);
                        
                        sap.addTempAttackBoost(numCreatures[0]);
                        sap.addTempDefenseBoost(numCreatures[0]);
                    }
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                    
            	}
            	
            	public boolean canPlayAI()
            	{
            		return false;
            	}
            	
            	public boolean canPlay()
            	{
            		CardList list = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
            		list = list.getType("Saproling");
            		return list.size() > 1 && super.canPlay();
            	}
            };
            
            card.addSpellAbility(a2);
            a2.setDescription("Sacrifice a Saproling: Saproling creatures get +1/+1 until end of turn");
            a2.setStackDescription("Saprolings get +1/+1 until end of turn.");
            a2.setBeforePayMana(runtime);
            
            card.addSpellAbility(a3);
            a3.setDescription("(Alternate way of sacrificing multiple creatures).");
            a3.setStackDescription("Saprolings get +X/+X until end of turn.");
                
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Ranger of Eos")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    

                    CardList cards = new CardList(lib.getCards());
                    CardList oneCostCreatures = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Creature")
                                && (CardUtil.getConvertedManaCost(cards.get(i).getManaCost()) <= 1)) {
                            oneCostCreatures.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(oneCostCreatures.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select First Creature",
                                oneCostCreatures.toArray());
                        if(o != null) {
                            //ability.setTargetCard((Card)o);
                            //AllZone.Stack.add(ability);
                            Card c1 = (Card) o;
                            lib.remove(c1);
                            hand.add(c1);
                            oneCostCreatures.remove(c1);
                            
                            if(oneCostCreatures.size() == 0) return;
                            
                            o = AllZone.Display.getChoiceOptional("Select Second Creature",
                                    oneCostCreatures.toArray());
                            
                            if(o != null) {
                                Card c2 = (Card) o;
                                lib.remove(c2);
                                hand.add(c2);
                                
                                oneCostCreatures.remove(c2);
                            }
                        }
                        controller.shuffle();
                    } else //computer
                    {
                        oneCostCreatures.shuffle();
                        if(oneCostCreatures.size() >= 1) {
                            Card c1 = oneCostCreatures.getCard(0);
                            lib.remove(c1);
                            hand.add(c1);
                            oneCostCreatures.remove(c1);
                            
                            if(oneCostCreatures.size() >= 1) {
                                Card c2 = oneCostCreatures.getCard(0);
                                lib.remove(c2);
                                hand.add(c2);
                                oneCostCreatures.remove(c2);
                                
                            }
                            
                        }
                        //ability.setTargetCard(powerTwoCreatures.get(0));
                        //AllZone.Stack.add(ability);
                       controller.shuffle();
                    }
                    

                    //...
                    
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -5697680711324878027L;
                
                public void execute() {
                    ability.setStackDescription("Ranger of Eos - Grab 2 creatures");
                    AllZone.Stack.add(ability);
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Master of the Wild Hunt")) {
        	
        	final Ability_Cost abCost = new Ability_Cost("T", cardName, true);
        	final Target abTgt = new Target("Target a creature to Hunt", "Creature".split(","));
        	final Ability_Activated ability = new Ability_Activated(card, abCost, abTgt) {
                private static final long serialVersionUID = 35050145102566898L;
                
                @Override
                public boolean canPlayAI() {
                    CardList wolves = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
                    wolves = wolves.getType("Wolf");
                    
                    wolves = wolves.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && c.isCreature();
                        }
                    });
                    int power = 0;
                    for(int i = 0; i < wolves.size(); i++) 
                    	power += wolves.get(i).getNetAttack();
                    
                    if (power == 0) 
                    	return false;
                    
                    final int totalPower = power;
                    
                    CardList targetables = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
                    
                    targetables = targetables.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c) && c.isCreature() && c.getNetDefense() <= totalPower;
                        }
                    });
                    
                    if (targetables.size() == 0)
                    	return false;
                    
                    getTarget().resetTargets();
                    setTargetCard(CardFactoryUtil.AI_getBestCreature(targetables));
                    
                	return true;
                }

                @Override
                public void resolve() {
                    CardList wolves = AllZoneUtil.getPlayerCardsInPlay(card.getController());
                    wolves = wolves.getType("Wolf");
                    
                    wolves = wolves.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && c.isCreature();
                        }
                    });

                    final Card target = getTargetCard();   
                    
                    if (wolves.size() == 0)
                    	return;
                    
                    if (!(CardFactoryUtil.canTarget(card, target) && AllZone.GameAction.isCardInPlay(target)))
                    	return;
                    
                    for(Card c : wolves){
                    	c.tap();
                    	target.addDamage(c.getNetAttack(),c);
                    }

                    if (target.getController().isHuman()){	// Human choose spread damage
                         for(int x = 0; x < target.getNetAttack() ; x++) {
                        	 AllZone.InputControl.setInput(CardFactoryUtil.MasteroftheWildHunt_input_targetCreature(this, wolves, new Command() {
                                 private static final long serialVersionUID = -328305150127775L;
                                 
                                 public void execute() {
                                	 getTargetCard().addDamage(1,target);
                                	 AllZone.GameAction.checkStateEffects();
                                 }
                             }));
                        }               
                    }
                    else {		// AI Choose spread Damage
                    	CardList damageableWolves = wolves.filter(new CardListFilter() {
                    		public boolean addCard(Card c) {
                    			return CardFactoryUtil.canDamage(target, c);
                    		}
                    	});
                    	
                    	if (damageableWolves.size() == 0)	// don't bother if I can't damage anything
                    		return;
                    	
                    	CardList wolvesLeft = damageableWolves.filter(new CardListFilter() {
                    		public boolean addCard(Card c) {
                    			return !c.hasKeyword("Indestructible");
                    		}
                    	});
                    	
						for (int i = 0; i < target.getNetAttack(); i++) {
							wolvesLeft = wolvesLeft.filter(new CardListFilter() {
	                    		public boolean addCard(Card c) {
	                    			return c.getKillDamage() > 0 && (c.getKillDamage() <= target.getNetAttack() 
	                    					|| target.hasKeyword("Deathtouch"));
	                    		}
	                    	});
							
							// Kill Wolves that can be killed first
							if (wolvesLeft.size() > 0) {
								Card best = CardFactoryUtil.AI_getBestCreature(wolvesLeft);								
								best.addDamage(1, target);
								if (best.getKillDamage() <= 0 || target.hasKeyword("Deathtouch")){
									wolvesLeft.remove(best);
								}
							} 

							else{
								// Add -1/-1s to Random Indestructibles
								if (target.hasKeyword("Infect") || target.hasKeyword("Wither")){
									CardList indestructibles = damageableWolves.filter(new CardListFilter() {
										public boolean addCard(Card c) {
											return c.hasKeyword("Indestructible");
										}
									});
									indestructibles.shuffle();
									indestructibles.get(0).addDamage(1, target);
								}
 
								// Then just add Damage randomnly
	
								else {
									damageableWolves.shuffle();
									wolves.get(0).addDamage(1, target);
								}
							}
						}
					}
                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append("Tap: Tap all untapped Wolf creatures you control. Each Wolf tapped ");
            sb.append("this way deals damage equal to its power to target creature. That creature deals ");
            sb.append("damage equal to its power divided as its controller chooses among any number of those Wolves.");
            ability.setDescription(sb.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Scarblade Elite")) {
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, new Target("TgtC")) {
                private static final long serialVersionUID = 3505019464802566898L;
                
                @Override
                public boolean canPlay() {
                	Player controller = card.getController();
                    PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, controller);
                    
                    CardList sins = new CardList(graveyard.getCards());
                    sins = sins.getType("Assassin");
                    
                    if(sins.size() > 0 && AllZone.GameAction.isCardInPlay(card)
                            && CardFactoryUtil.canTarget(card, getTargetCard()) && super.canPlay()) return true;
                    else return false;
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                    human = human.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return AllZone.GameAction.isCardInPlay(c);
                        }
                    });
                    
                    CardListUtil.sortAttack(human);
                    CardListUtil.sortFlying(human);
                    
                    //if(0 < human.size())
                    //  setTargetCard(human.get(0));
                    
                    PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
                    
                    CardList grave = new CardList(graveyard.getCards());
                    grave = grave.getType("Assassin");
                    
                    if(human.size() > 0 && grave.size() > 0) setTargetCard(human.get(0));
                    
                    return 0 < human.size() && 0 < grave.size();
                }
                
                @Override
                public void resolve() {
                	Player controller = card.getController();
                    PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, controller);
                    
                    CardList sins = new CardList(graveyard.getCards());
                    sins = sins.getType("Assassin");
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select an Assassin to exile", sins.toArray());
                        
                        if(o != null) {
                            Card crd = (Card) o;
                            AllZone.GameAction.exile(crd);
                            
                            Card c = getTargetCard();
                            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                                AllZone.GameAction.destroy(c);
                            }
                        } //if o!= null
                    }//player.equals("human")
                    else {
                        Card crd = sins.get(0);
                        AllZone.GameAction.exile(crd);
                        
                        Card c = getTargetCard();
                        if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                            AllZone.GameAction.destroy(c);
                        }
                    }
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription(abCost+"Exile an Assassin card from your graveyard: Destroy target creature.");
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Primeval Titan")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	AllZone.GameAction.searchLibraryTwoLand("Land", card.getController(), 
							Constant.Zone.Battlefield, true, 
							Constant.Zone.Battlefield, true);
                }//resolve()
            };
            Command intoPlay = new Command() {

				private static final long serialVersionUID = 4991367699382641872L;

				public void execute() {
                    ability.setStackDescription("Primeval Titan - search your library for up to two land cards, put them onto the battlefield tapped, then shuffle your library.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mogg War Marshal") || cardName.equals("Goblin Marshal")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    makeToken();
                    if(card.getName().equals("Goblin Marshal")) makeToken();
                }//resolve()
                
                void makeToken() {
                    CardFactoryUtil.makeToken("Goblin", "R 1 1 Goblin", card.getController(), "R", new String[] {
                            "Creature", "Goblin"}, 1, 1, new String[] {""});
                }
            };
            Command intoPlayDestroy = new Command() {
                private static final long serialVersionUID = 5554242458006247407L;
                
                public void execute() {
                    if(card.getName().equals("Mogg War Marshal")) ability.setStackDescription("Mogg War Marshal - put a red 1/1 Goblin creature token onto the battlefield.");
                    else if(card.getName().equals("Goblin Marshal")) ability.setStackDescription("Goblin Marshal - put two red 1/1 Goblin creature tokens onto the battlefield.");
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlayDestroy);
            card.addDestroyCommand(intoPlayDestroy);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Aven Riftwatcher")) {
            Command gain2Life = new Command() {
                private static final long serialVersionUID = 5588978023269625349L;
                
                public void execute() {
                	card.getController().gainLife(2, card);
                }
            };
            
            card.addLeavesPlayCommand(gain2Life);
            card.addComesIntoPlayCommand(gain2Life);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Gilt-Leaf Archdruid")) {
        	Ability_Cost abCost = new Ability_Cost("tapXType<7/Druid>", cardName, true);
        	Target tgt = new Target("Select a player to gain lands from", "Player".split(","));
        	final SpellAbility stealLands = new Ability_Activated(card, abCost, tgt){
				private static final long serialVersionUID = 636594487143500891L;

				@Override
        		public boolean canPlayAI(){
        			Player p = AllZone.HumanPlayer;
        			
        			if (!p.canTarget(card))
        				return false;
        			
        			setTargetPlayer(p);
        			
        			CardList lands = AllZoneUtil.getPlayerCardsInPlay(p);
        			lands = lands.getType("Land");
        			
        			// Don't steal lands if Human has less than 2
        			return lands.size() >= 2;
        		}
                
                @Override
                public void resolve() {
                    Player activator = this.getActivatingPlayer();
                    
                    CardList lands = AllZoneUtil.getPlayerCardsInPlay(getTargetPlayer());
                    lands = lands.getType("Land");
                    
                    for(int i = 0; i < lands.size(); i++) {
                        Card land = lands.get(i);
                        if(AllZone.GameAction.isCardInPlay(land)) {	// this really shouldn't fail in the middle of resolution
                            land.setController(activator);
                            
                            // i don't know how the code handles Sum Sickness so I'm leaving this
                            // but a card changing controllers should always gain this no matter if it has haste or not
                            if(land.getKeyword().contains("Haste")) {
                                land.setSickness(false);
                            } else {
                                land.setSickness(true);
                            }
                            
                            ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                            ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                            
                            PlayerZone from = AllZone.getZone(land);
                            from.remove(land);
                            
                            PlayerZone to = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                            to.add(land);
                            
                            ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                            ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                        }//if
                    }
                }
            };
            
            card.addSpellAbility(stealLands);
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.toString()).append(" - Gain control of all lands target player controls.");
            stealLands.setStackDescription(sb.toString());
            
            stealLands.setDescription("Tap seven untapped Druids you control: Gain control of all lands target player controls.");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Figure of Destiny")) {
            Ability ability1 = new Ability(card, "RW") {
                @Override
                public void resolve() {
                    boolean artifact = false;
                    card.setBaseAttack(2);
                    card.setBaseDefense(2);
                    
                    card.removeIntrinsicKeyword("Flying");
                    card.removeIntrinsicKeyword("First Strike");
                    
                    if(card.isArtifact()) artifact = true;
                    
                    card.setType(new ArrayList<String>());
                    if(artifact) card.addType("Artifact");
                    card.addType("Creature");
                    card.addType("Kithkin");
                    card.addType("Spirit");
                }
                
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Spirit") && super.canPlayAI();
                }
                
            };// ability1
            
            ability1.setDescription("RW: Figure of Destiny becomes a 2/2 Kithkin Spirit.");
            ability1.setStackDescription("Figure of Destiny becomes a 2/2 Kithkin Spirit.");
            card.addSpellAbility(ability1);
            

            Ability ability2 = new Ability(card, "RW RW RW") {
                @Override
                public void resolve() {
                    if(card.isType("Spirit")) {
                        boolean artifact = false;
                        card.setBaseAttack(4);
                        card.setBaseDefense(4);
                        
                        card.removeIntrinsicKeyword("Flying");
                        card.removeIntrinsicKeyword("First Strike");
                        
                        if(card.isArtifact()) artifact = true;
                        
                        card.setType(new ArrayList<String>());
                        if(artifact) card.addType("Artifact");
                        card.addType("Creature");
                        card.addType("Kithkin");
                        card.addType("Spirit");
                        card.addType("Warrior");
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return card.isType("Spirit") && super.canPlay();
                }
                
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Warrior") && super.canPlayAI();
                }
                
            };// ability2
            
            ability2.setDescription("RW RW RW: If Figure of Destiny is a Spirit, it becomes a 4/4 Kithkin Spirit Warrior.");
            ability2.setStackDescription("Figure of Destiny becomes a 4/4 Kithkin Spirit Warrior.");
            card.addSpellAbility(ability2);
            

            Ability ability3 = new Ability(card, "RW RW RW RW RW RW") {
                @Override
                public void resolve() {
                    if(card.isType("Warrior")) {
                        boolean artifact = false;
                        card.setBaseAttack(8);
                        card.setBaseDefense(8);
                        
                        card.addIntrinsicKeyword("Flying");
                        card.addIntrinsicKeyword("First Strike");
                        
                        if(card.isArtifact()) artifact = true;
                        
                        card.setType(new ArrayList<String>());
                        if(artifact) card.addType("Artifact");
                        card.addType("Creature");
                        card.addType("Kithkin");
                        card.addType("Spirit");
                        card.addType("Warrior");
                        card.addType("Avatar");
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return card.isType("Warrior") && super.canPlay();
                }
                
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Avatar") && super.canPlayAI();
                }
            };// ability3
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("RW RW RW RW RW RW: If Figure of Destiny is a Warrior, it becomes ");
            sbDesc.append("an 8/8 Kithkin Spirit Warrior Avatar with flying and first strike.");
            ability3.setDescription(sbDesc.toString());
            
            ability3.setStackDescription("Figure of Destiny becomes an 8/8 Kithkin Spirit Warrior Avatar with flying and first strike.");
            card.addSpellAbility(ability3);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lich Lord of Unx")) {
            final Ability ability2 = new Ability(card, "U U B B") {
                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(AllZone.HumanPlayer);
                    return countZombies() >= 3;
                }
                
                @Override
                public void resolve() {
                    if(getTargetPlayer() != null) {
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, getTargetPlayer());
                        for(int i = 0; i < countZombies(); i++) {
                            //probably should be updated to AllZone.GameAction.mill(getTargetPlayer(),1);
                        	if(lib.size() > 0) {
                            	AllZone.GameAction.moveToGraveyard(lib.get(0));
                            }
                            getTargetPlayer().loseLife(1, card);
                        }
                    }
                }//end resolve
                
                public int countZombies() {
                    return AllZoneUtil.getPlayerTypeInPlay(card.getController(), "Zombie").size();
                }
                
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append("U U B B: Target player loses X life and puts the top X cards of his or her library ");
            sb.append("into his or her graveyard, where X is the number of Zombies you control.");
            ability2.setDescription(sb.toString());
            
            ability2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability2));
            card.addSpellAbility(ability2);         
        }//*************** END ************ END **************************
        
                
        //*************** START *********** START **************************
        else if(cardName.equals("Covetous Dragon")) {
            SpellAbility spell = new Spell_Permanent(card) {
                
                private static final long serialVersionUID = -1446713295855849195L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(
                            AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
                    list = list.getType("Artifact");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Tethered Griffin")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -7872917651421012893L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(
                            AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
                    list = list.getType("Enchantment");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cantivore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7254358703158629514L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(
                            AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer).getCards());
                    list.addAll(AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer).getCards());
                    list = list.getType("Enchantment");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Terravore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7316190829288665283L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(
                            AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer).getCards());
                    list.addAll(AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer).getCards());
                    list = list.getType("Land");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mortivore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -7118801410173525870L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(
                            AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer).getCards());
                    list.addAll(AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer).getCards());
                    list = list.getType("Creature");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            final Command untilEOT = new Command() {
				private static final long serialVersionUID = 8163158247122311120L;

				public void execute() {
                    card.setShield(0);   
                }
            };
            
            final SpellAbility a1 = new Ability(card, "B") {
                @Override
                public boolean canPlayAI() {
                    if(CardFactoryUtil.AI_isMainPhase()) {
                        if(CardFactoryUtil.AI_doesCreatureAttack(card)) {
                         
                            int weight[] = new int[3];
                            
                            if(card.getKeyword().size() > 0) weight[0] = 75;
                            else weight[0] = 0;
                            
                            CardList HandList = new CardList(AllZone.getZone(Constant.Zone.Hand,
                                    AllZone.ComputerPlayer).getCards());
                            
                            if(HandList.size() >= 4) weight[1] = 25;
                            else weight[1] = 75;
                            
                            int hCMC = 0;
                            for(int i = 0; i < HandList.size(); i++)
                                if(CardUtil.getConvertedManaCost(HandList.getCard(i).getManaCost()) > hCMC) hCMC = CardUtil.getConvertedManaCost(HandList.getCard(
                                        i).getManaCost());
                            
                            CardList LandList = new CardList(AllZone.getZone(Constant.Zone.Battlefield,
                                    AllZone.ComputerPlayer).getCards());
                            LandList = LandList.getType("Land");
                            
                            if(hCMC + 2 >= LandList.size()) weight[2] = 50;
                            else weight[2] = 0;
                            
                            int aw = (weight[0] + weight[1] + weight[2]) / 3;
                            Random r = new Random();
                            if(r.nextInt(100) <= aw) return true;
                        }
                    }
                    return false;
                }
                
                @Override
                public void resolve() {
                    card.addShield();
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }
            }; //SpellAbility
            a1.setDescription("Regenerate: B");
            card.addSpellAbility(a1);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cognivore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -2216181341715046786L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(
                            AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer).getCards());
                    list.addAll(AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer).getCards());
                    list = list.getType("Instant");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Magnivore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -2252263708643462897L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(
                            AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer).getCards());
                    list.addAll(AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer).getCards());
                    list = list.getType("Sorcery");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Elvish Hunter")) {
        	Ability_Cost abCost = new Ability_Cost("1 G T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, new Target("TgtC")) {
                private static final long serialVersionUID = -560200335562416099L;
                
                @Override
                public boolean canPlayAI() {
                    if(CardFactoryUtil.AI_doesCreatureAttack(card)) return false;
                    
                    return (getCreature().size() != 0);
                }
                
                @Override
                public void chooseTargetAI() {
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {
                    CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (!c.getKeyword().contains("This card doesn't untap during your next untap step."))
                                    && CardFactoryUtil.canTarget(card, c) && c.isTapped();
                        }
                    });
                    list.remove(card);
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        getTargetCard().addExtrinsicKeyword("This card doesn't untap during your next untap step.");
                    }//if (card is in play)
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription(abCost+"Target creature doesn't untap during its controller's next untap step.");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Shifting Wall") || cardName.equals("Maga, Traitor to Mortals") || cardName.equals("Feral Hydra")
        		|| cardName.equals("Krakilin") || cardName.equals("Ivy Elemental") || cardName.equals("Lightning Serpent")) { 
        	
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -11489323313L;
                
                @Override
                public boolean canPlayAI() {
                    return super.canPlay() && 4 <= ComputerUtil.getAvailableMana().size() - CardUtil.getConvertedManaCost(card.getManaCost());
                }
            };
            card.clearFirstSpellAbility();
            card.addFirstSpellAbility(spell);
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
        	        getTargetPlayer().loseLife(card.getCounters(Counters.P1P1),card);
                }//resolve()
            };
  
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = 2559021594L;
                
                public void execute() {
                	int XCounters = card.getXManaCostPaid();
                	if(card.getName().equals("Lightning Serpent")) card.addCounter(Counters.P1P0, XCounters);
                	else card.addCounter(Counters.P1P1, XCounters);
                	if(card.getName().equals("Maga, Traitor to Mortals")) {
                		StringBuilder sb = new StringBuilder();
                		sb.append(ability.getTargetPlayer()).append(" - loses life equal to the number of +1/+1 counters on ").append(card.getName());
                		ability.setStackDescription(sb.toString());
                        
                        if(card.getController() == AllZone.HumanPlayer) AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
                        else {
                        	ability.setTargetPlayer(AllZone.HumanPlayer);
                        	AllZone.Stack.add(ability);
                        }
                	} 

                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Apocalypse Hydra")) {      
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -11489323313L;
                
                @Override
                public boolean canPlayAI() {
                    return super.canPlay() && 5 <= ComputerUtil.getAvailableMana().size() - 2;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);

            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = 255901529244894L;
                
                public void execute() {
                	int XCounters = card.getXManaCostPaid();
                	if(XCounters >= 5) XCounters = 2 * XCounters;
                    card.addCounter(Counters.P1P1, XCounters);                   
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Workhorse")) {      
            
            final Ability_Mana ability = new Ability_Mana(card, "0: Add 1") {
				private static final long serialVersionUID = -6764282980691397966L;

				@Override
                public boolean canPlayAI() {
					return false;
                }
               
                @Override
                public void resolve() {
                	
                    card.subtractCounter(Counters.P1P1,1);
                	super.resolve();
                    
                }
                
                @Override
				public String mana() {
					return "1";
            	}
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" add 1 mana to mana pool.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("Remove a +1/+1 counter from Workhorse: Add 1 to your mana pool.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Molten Hydra")) {
        	Target target = new Target("TgtCP");
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
            final Ability_Activated ability2 = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = 2626619319289064289L;
                
                @Override
                public boolean canPlay() {
                    return card.getCounters(Counters.P1P1) > 0 && super.canPlay();
                }
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    if(AllZone.HumanPlayer.getLife() < card.getCounters(Counters.P1P1)) setTargetPlayer(AllZone.HumanPlayer);
                    else {
                        CardList list = getCreature();
                        list.shuffle();
                        setTargetCard(list.get(0));
                    }
                }//chooseTargetAI()
                
                CardList getCreature() {

                    //toughness of 1
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card.getCounters(Counters.P1P1), card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                        	int total = card.getCounters(Counters.P1P1);
                            return (total >= c.getKillDamage());
                        }
                    });
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                	int total = card.getCounters(Counters.P1P1);
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(total,
                                card);
                    } else getTargetPlayer().addDamage(total, card);
                   card.subtractCounter(Counters.P1P1,total);
                }//resolve()
            };//SpellAbility

            card.addSpellAbility(ability2);
            
            StringBuilder sb = new StringBuilder();
            sb.append(abCost+"Remove all +1/+1 counters from "+cardName+".  "+cardName);
            sb.append(" deals damage to target creature or player equal to the number of counters removed this way.");
            ability2.setDescription(sb.toString());
            
            ability2.setStackDescription("Molten Hydra deals damage to number of counters on it to target creature or player.");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Academy Rector") || cardName.equals("Lost Auramancers")) {
            final SpellAbility ability = new Ability(card, "0") {
                
                @Override
                public void resolve() {
                    
                    if (card.getController().equals(AllZone.HumanPlayer)) {
                        StringBuilder question = new StringBuilder();
                        if (card.getName().equals("Academy Rector")) {
                            question.append("Exile ").append(card.getName()).append(" and place ");
                        } else {
                            question.append("Place ");
                        }
                        question.append("an enchantment from your library onto the battlefield?");
                        
                        if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                            if (card.getName().equals("Academy Rector")) {
                                AllZone.GameAction.exile(card);
                            }
                            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
                            CardList list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.HumanPlayer);
                            list = list.getType("Enchantment");
                            
                            if (list.size() > 0) {
                                Object objectSelected = AllZone.Display.getChoiceOptional("Choose an enchantment", list.toArray());
                                
                                if (objectSelected != null) {
                                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
                                    final Card c = (Card) objectSelected;
                                    lib.remove(c);
                                    play.add(c);
                                    
                                    if (c.isAura()) {
                                        
                                        String enchantThisType[] = {""};
                                        String message[] = {""};
                                        
                                        // The type following "Enchant" maybe upercase or lowercase, cardsfolder has both
                                        // Note that I am being overly cautious.
                                        
                                        if (c.getKeyword().contains("Enchant creature without flying") 
                                                || c.getKeyword().contains("Enchant Creature without flying")) {
                                            enchantThisType[0] = "Creature.withoutFlying";
                                            message[0] = "Select a creature without flying";
                                        } else if (c.getKeyword().contains("Enchant creature with converted mana cost 2 or less") 
                                                || c.getKeyword().contains("Enchant Creature with converted mana cost 2 or less")) {
                                            enchantThisType[0] = "Creature.cmcLE2";
                                            message[0] = "Select a creature with converted mana cost 2 or less";
                                        } else if (c.getKeyword().contains("Enchant red or green creature")) {
                                            enchantThisType[0] = "Creature.Red,Creature.Green";
                                            message[0] = "Select a red or green creature";
                                        } else if (c.getKeyword().contains("Enchant tapped creature")) {
                                            enchantThisType[0] = "Creature.tapped";
                                            message[0] = "Select a tapped creature";
                                        } else if (c.getKeyword().contains("Enchant creature") 
                                                || c.getKeyword().contains("Enchant Creature")) {
                                            enchantThisType[0] = "Creature";
                                            message[0] = "Select a creature";
                                        } else if (c.getKeyword().contains("Enchant wall") 
                                                || c.getKeyword().contains("Enchant Wall")) {
                                            enchantThisType[0] = "Wall";
                                            message[0] = "Select a Wall";
                                        } else if (c.getKeyword().contains("Enchant land you control") 
                                                || c.getKeyword().contains("Enchant Land you control")) {
                                            enchantThisType[0] = "Land.YouCtrl";
                                            message[0] = "Select a land you control";
                                        } else if (c.getKeyword().contains("Enchant land") 
                                                || c.getKeyword().contains("Enchant Land")) {
                                            enchantThisType[0] = "Land";
                                            message[0] = "Select a land";
                                        } else if (c.getKeyword().contains("Enchant artifact") 
                                                || c.getKeyword().contains("Enchant Artifact")) {
                                            enchantThisType[0] = "Artifact";
                                            message[0] = "Select an artifact";
                                        } else if (c.getKeyword().contains("Enchant enchantment") 
                                                || c.getKeyword().contains("Enchant Enchantment")) {
                                            enchantThisType[0] = "Enchantment";
                                            message[0] = "Select an enchantment";
                                        }
                                        
                                        CardList allCards = new CardList();
                                        allCards.addAll(AllZone.Human_Battlefield.getCards());
                                        allCards.addAll(AllZone.Computer_Battlefield.getCards());
                                        
                                        // Make sure that we were able to match the selected aura with our list of criteria
                                        
                                        if (enchantThisType[0] != "" && message[0] != "") {
                                        
                                            final CardList choices = allCards.getValidCards(enchantThisType[0], card.getController(), card);
                                            final String msg = message[0];
                                        
                                            AllZone.InputControl.setInput(new Input() {
                                                private static final long serialVersionUID = -6271957194091955059L;

                                                @Override
                                                public void showMessage() {
                                                    AllZone.Display.showMessage(msg);
                                                    ButtonUtil.enableOnlyOK();
                                                }
                                            
                                                @Override
                                                public void selectButtonOK() {
                                                    stop();
                                                }
                                            
                                                @Override
                                                public void selectCard(Card card, PlayerZone zone) {
                                                    if (choices.contains(card)) {
                                                    
                                                        if (AllZone.GameAction.isCardInPlay(card)) {
                                                            c.enchantCard(card);
                                                            stop();
                                                        }
                                                    }
                                                }//selectCard()
                                            });// Input()
                                            
                                        }// if we were able to match the selected aura with our list of criteria
                                    }// If enchantment selected is an aura
                                }// If an enchantment is selected
                            }// If there are enchantments in library
                            
                            card.getController().shuffle();
                        }// If answered yes to may exile
                    }// If player is human
                    
                    // player is the computer
                    else {
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer);
                        CardList list = new CardList(lib.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isEnchantment() && !c.isAura();
                            }
                        });
                        
                        if (list.size() > 0) {
                            PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                            Card c = CardFactoryUtil.AI_getBestEnchantment(list, card, false);
                            lib.remove(c);
                            play.add(c);
                            if (card.getName().equals("Academy Rector")) {
                                AllZone.GameAction.exile(card);
                            }
                            card.getController().shuffle();
                        }
                    }// player is the computer
                }// resolve()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            if (card.getName().equals("Academy Rector")) {
                sb.append("Academy Rector - ").append(card.getController());
                sb.append(" may exile this card and place an enchantment from his library onto the battlefield.");
            } else {
                sb.append("Lost Auramancers - ").append(card.getController());
                sb.append(" may place an enchantment from his library onto the battlefield.");
            }
            ability.setStackDescription(sb.toString());
            
            final Command destroy = new Command() {
                private static final long serialVersionUID = -4352349741511065318L;
                public void execute() {
                    
                    if (card.getName().equals("Lost Auramancers") 
                            && card.getCounters(Counters.TIME) <= 0) {
                        AllZone.Stack.add(ability);
                    } else if (card.getName().equals("Academy Rector")) {
                        AllZone.Stack.add(ability);
                    }
                    
                }// execute()
            };// Command destroy
            
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Deadly Grub")) {
            final Command destroy = new Command() {
                private static final long serialVersionUID = -4352349741511065318L;
                
                public void execute() {
                    if(card.getCounters(Counters.TIME) <= 0) CardFactoryUtil.makeToken("Insect", "G 6 1 Insect",
                    		card.getController(), "G", new String[] {"Creature", "Insect"}, 6, 1, new String[] {"Shroud"});
                }
            };
            
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Boggart Harbinger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        lib.add(c, 0);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4022442363194287539L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList goblins = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Goblin")
                                || cards.get(i).getKeyword().contains("Changeling")) {
                            goblins.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(goblins.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", goblins.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Merrow Harbinger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        lib.add(c, 0);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4022442363194287539L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList merfolk = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Merfolk")
                                || cards.get(i).getKeyword().contains("Changeling")) {
                            merfolk.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(merfolk.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", merfolk.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Elvish Harbinger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        lib.add(c, 0);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4022442363194287539L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList elves = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Elf")
                                || cards.get(i).getKeyword().contains("Changeling")) {
                            elves.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(elves.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", elves.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Vendilion Clique")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Player player = getTargetPlayer();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    CardList list = new CardList(hand.getCards());
                    CardList nonLandList = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.isLand();
                        }
                    });
                    
                    if(list.size() > 0) {
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            AllZone.Display.getChoiceOptional("Revealing hand", list.toArray());
                            if(nonLandList.size() > 0) {
                                Object o = AllZone.Display.getChoiceOptional("Select nonland card",
                                        nonLandList.toArray());
                                if(o != null) {
                                    Card c = (Card) o;
                                    hand.remove(c);
                                    lib.add(c); //put on bottom
                                    
                                    player.drawCard();
                                }
                            }
                        } else //comp
                        {
                            if(AllZone.Phase.getTurn() >= 12 && nonLandList.size() > 0) {
                                Card c = CardFactoryUtil.AI_getMostExpensivePermanent(nonLandList, card, false);
                                hand.remove(c);
                                lib.add(c);
                                AllZone.HumanPlayer.drawCard();
                            }
                        }
                    }//handsize > 0
                    
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -5052568979553782714L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
                        ButtonUtil.disableAll();
                    } else if(card.getController().equals(AllZone.ComputerPlayer)) {
                        ability.setTargetPlayer(AllZone.HumanPlayer);
                        AllZone.Stack.add(ability);
                    }
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Faerie Harbinger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        lib.add(c, 0);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = -708639335039567945L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList faeries = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Faerie")
                                || cards.get(i).getKeyword().contains("Changeling")) {
                            faeries.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(faeries.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", faeries.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Flamekin Harbinger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        lib.add(c, 0);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7114265436722599216L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList ele = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Elemental")
                                || cards.get(i).getKeyword().contains("Changeling")) {
                            ele.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(ele.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", ele.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Giant Harbinger")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        lib.add(c, 0);
                        if (card.getController().isPlayer(AllZone.ComputerPlayer)) 
                        	AllZone.Display.getChoiceOptional(card + " - Computer picked:", c);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -2671592749882297551L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList giants = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Giant")
                                || cards.get(i).getKeyword().contains("Changeling")) {
                            giants.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(giants.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", giants.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Kor Cartographer")) {
            final Ability ab1 = new Ability(card, "no cost") {
                private static final long serialVersionUID = -3361422153566629825L;
                
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    CardList landInLib = new CardList(lib.getCards());
                    landInLib = landInLib.getType("Plains");
                    
                    if(landInLib.size() > 0) {
                        if(card.getController().equals(AllZone.ComputerPlayer)) {
                            lib.remove(landInLib.get(0));
                            landInLib.get(0).tap();
                            play.add(landInLib.get(0));
                        } else {
                            Object o = AllZone.Display.getChoiceOptional("Select plains card to put onto the battlefield: ",
                                    landInLib.toArray());
                            if(o != null) {
                                Card crd = (Card) o;
                                lib.remove(crd);
                                crd.tap();
                                play.add(crd);
                            }
                        }
                        card.getController().shuffle();
                    }//if(isCardInPlay)
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList landInLib = new CardList(AllZone.getZone(Constant.Zone.Library,
                            AllZone.ComputerPlayer).getCards());
                    CardList landInPlay = new CardList(AllZone.getZone(Constant.Zone.Battlefield,
                            AllZone.ComputerPlayer).getCards());
                    
                    landInLib = landInLib.getType("Land");
                    landInPlay = landInPlay.getType("Land");
                    
                    if(landInLib.size() > 0 && landInPlay.size() > 0) return true;
                    else return false;
                    
                }
            };//SpellAbility
            
            ab1.setStackDescription("search your library for a plains card, put it onto the battlefield tapped, then shuffle your library.");
            
            Command cip = new Command() {
                private static final long serialVersionUID = -2084426519099911543L;
                
                public void execute() {
                    AllZone.Stack.add(ab1);
                }
            };
            card.addComesIntoPlayCommand(cip);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Loyal Retainers")) {
            final Ability ability = new Ability(card, "0") {
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        AllZone.GameAction.sacrifice(card);
                        
                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                        PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                        CardList list = new CardList(grave.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isCreature() && c.getType().contains("Legendary");
                            }
                        });
                        
                        if(list.size() > 0) {
                            if(card.getController().equals(AllZone.HumanPlayer)) {
                                Object o = AllZone.Display.getChoiceOptional("Select Legendary creature",
                                        list.toArray());
                                if(o != null) {
                                    Card c = (Card) o;
                                    grave.remove(c);
                                    play.add(c);
                                }
                                
                            } else //computer
                            {
                                Card c = CardFactoryUtil.AI_getBestCreature(list);
                                grave.remove(c);
                                play.add(c);
                            }
                        }
                    }
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.getType().contains("Legendary");
                        }
                    });
                    
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    return super.canPlay() && list.size() > 0
                            && AllZone.Phase.getPhase().equals(Constant.Phase.Main1)
                            && AllZone.Phase.getPlayerTurn().equals(card.getController());
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.getType().contains("Legendary")
                                    && CardUtil.getConvertedManaCost(c.getManaCost()) > 4;
                        }
                    });
                    return list.size() > 0;
                }
            };//Ability
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Sacrifice Loyal Retainers: Return target legendary creature card from your graveyard to the battlefield. ");
            sbDesc.append("Activate this ability only during your turn, before attackers are declared.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(cardName).append(" - Return target legendary creature card from your graveyard to the battlefield.");
            ability.setStackDescription(sbStack.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Xiahou Dun, the One-Eyed")) {
            final Ability ability = new Ability(card, "0") {
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        AllZone.GameAction.sacrifice(card);
                        
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                        CardList list = new CardList(grave.getCards());
                        list = list.filter(AllZoneUtil.black);
                        
                        if(list.size() > 0) {
                            if(card.getController().equals(AllZone.HumanPlayer)) {
                                Object o = AllZone.Display.getChoiceOptional("Select black card", list.toArray());
                                if(o != null) {
                                    Card c = (Card) o;
                                    grave.remove(c);
                                    hand.add(c);
                                }
                                
                            } else //computer
                            {
                                //TODO
                            }
                        }
                    }
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    list = list.filter(AllZoneUtil.black);
                    
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    return super.canPlay() && list.size() > 0
                            && AllZone.Phase.getPhase().equals(Constant.Phase.Main1)
                            && AllZone.Phase.getPlayerTurn().equals(card.getController());
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Sacrifice Xiahou Dun, the One-Eyed: Return target black card from your graveyard to your hand. ");
            sbDesc.append("Activate this ability only during your turn, before attackers are declared.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(cardName).append(" - Return target black card from your graveyard to your hand.");
            ability.setStackDescription(sbStack.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Fire Bowman")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    if(AllZone.HumanPlayer.getLife() < 3) setTargetPlayer(AllZone.HumanPlayer);
                    else {
                        CardList list = getCreature();
                        list.shuffle();
                        setTargetCard(list.get(0));
                    }
                    AllZone.GameAction.sacrifice(card);
                }//chooseTargetAI()
                
                CardList getCreature() {
                    //toughness of 1
                    CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            //only get 1/1 flyers or 2/1 creatures
                            return (2 <= c.getNetAttack()) || c.getKeyword().contains("Flying");
                        }
                    });
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(1,
                                card);
                    } else getTargetPlayer().addDamage(1, card);
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Sacrifice Fire Bowman: Fire Bowman deals 1 damage to target creature or player. ");
            sb.append("Activate this ability only during your turn, before attackers are declared.");
            ability.setDescription(sb.toString());
            
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, new Command() {
                
                private static final long serialVersionUID = -3283051501556347775L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                }
            }, true, false));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sygg, River Guide")) {
            final HashMap<Card, String[]> creatureMap = new HashMap<Card, String[]>();
            
            final Ability ability = new Ability(card, "1 W") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    String color = "";
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        
                        Object o = AllZone.Display.getChoice("Choose mana color", Constant.Color.ColorsOnly);
                        color = (String) o;
                        c.addExtrinsicKeyword("Protection from " + color);
                        if(creatureMap.containsKey(c)) {
                            int size = creatureMap.get(c).length;
                            String[] newString = new String[size + 1];
                            
                            for(int i = 0; i < size; i++) {
                                newString[i] = creatureMap.get(c)[i];
                            }
                            newString[size] = color;
                            creatureMap.put(c, newString);
                        } else creatureMap.put(c, new String[] {color});
                        
                        final Card crd = c;
                        final Command atEOT = new Command() {
                            private static final long serialVersionUID = 8630868536866681014L;
                            
                            public void execute() {
                                //if(AllZone.GameAction.isCardInPlay(c))
                                //  c.removeExtrinsicKeyword("Protection from "+color);
                                if(AllZone.GameAction.isCardInPlay(crd)) {
                                    String[] colors = creatureMap.get(crd);
                                    for(String col:colors) {
                                        crd.removeExtrinsicKeyword("Protection from " + col);
                                    }
                                }
                            }
                        };//Command
                        AllZone.EndOfTurn.addUntil(atEOT);
                    }
                }
            };
            Input runtime = new Input() {
                private static final long serialVersionUID = -2171146532836387392L;
                
                @Override
                public void showMessage() {
                    CardList creats = new CardList(
                            AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
                    creats = creats.getType("Merfolk");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, creats, "Select a target Merfolk",
                            true, false));
                }
            };
            ability.setDescription("1 W: Target Merfolk you control gains protection from the color of your choice until end of turn.");
            ability.setBeforePayMana(runtime);
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Laquatus's Champion")) {
            final SpellAbility abilityComes = new Ability(card, "0") {
                @Override
                public void resolve() {
                    getTargetPlayer().loseLife(6,card);
                }//resolve()
            };
            
            final Input inputComes = new Input() {
                private static final long serialVersionUID = -2666229064706311L;
                
                @Override
                public void showMessage() {
                    stopSetNext(CardFactoryUtil.input_targetPlayer(abilityComes));
                    ButtonUtil.disableAll();//to disable the Cancel button
                }
            };
            Command commandComes = new Command() {
                private static final long serialVersionUID = -4246229185669164581L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(inputComes);
                    else //computer
                    {
                        abilityComes.setTargetPlayer(AllZone.HumanPlayer);
                        AllZone.Stack.add(abilityComes);
                    }//else
                }//execute()
            };//CommandComes
            Command commandLeavesPlay = new Command() {
                
                private static final long serialVersionUID = 9172348861441804625L;
                
                public void execute() {
                    //System.out.println(abilityComes.getTargetCard().getName());
                    
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            abilityComes.getTargetPlayer().gainLife(6, card);
                            
                        }//resolve()
                    };//SpellAbility
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Laquatus's Champion - ").append(abilityComes.getTargetPlayer()).append(" regains 6 life.");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }//execute()
            };//Command
            
            card.addComesIntoPlayCommand(commandComes);
            card.addLeavesPlayCommand(commandLeavesPlay);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Malakir Bloodwitch")) {
            final SpellAbility abilityComes = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getType().contains("Vampire") || c.getKeyword().contains("Changeling");
                        }
                    });
                    int drain = list.size();
                    card.getController().getOpponent().loseLife(drain, card);
                    card.getController().gainLife(drain, card);               
                }//resolve()
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - Opponent loses life equal to the number of Vampires you control. ");
            sb.append("You gain life equal to the life lost this way.");
            abilityComes.setStackDescription(sb.toString());
            
            Command commandComes = new Command() {
                private static final long serialVersionUID = 6375360999823102355L;
                
                public void execute() {
                    AllZone.Stack.add(abilityComes);
                }//execute()
            };//CommandComes
            
            card.addComesIntoPlayCommand(commandComes);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Wood Elves")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;
                    Player player = c.getController();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    

                    CardList lands = new CardList(lib.getCards());
                    lands = lands.getType("Forest");
                    
                    if(player.equals(AllZone.HumanPlayer) && lands.size() > 0) {
                        Object o = AllZone.Display.getChoiceOptional("Pick a forest card to put onto the battlefield",
                                lands.toArray());
                        if(o != null) {
                            Card card = (Card) o;
                            lib.remove(card);
                            AllZone.Human_Battlefield.add(card);
                            lands.remove(card);
                            player.shuffle();
                        }
                    } // player equals human
                    else if(player.equals(AllZone.ComputerPlayer) && lands.size() > 0) {
                        Card card = lands.get(0);
                        lib.remove(card);
                        AllZone.Computer_Battlefield.add(card);
                        lands.remove(card);
                        player.shuffle();
                    }
                }
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 1832932499373431651L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" searches his library for a Forest card to put that card onto the battlefield.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Meddling Mage")) {
            final String[] input = new String[1];
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        input[0] = JOptionPane.showInputDialog(null, "Which card?", "Pick card",
                                JOptionPane.QUESTION_MESSAGE);
                        card.setNamedCard(input[0]);
                    } else {
                        String s = "Ancestral Recall";
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer);
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
                        
                        CardList list = new CardList();
                        list.addAll(hand.getCards());
                        list.addAll(lib.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return !c.isLand() && !c.isUnCastable();
                            }
                        });
                        
                        if(list.size() > 0) {
                            CardList rare;
                            rare = list;
                            rare = rare.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.getRarity().equals("Rare");
                                }
                            });
                            
                            if(rare.size() > 0) {
                                s = rare.get(CardUtil.getRandomIndex(rare)).getName();
                            } else {
                                Card c = list.get(CardUtil.getRandomIndex(list));
                                //System.out.println(c + " - " + c.getRarity());
                                s = c.getName();
                            }
                        }
                        
                        card.setNamedCard(s);
                        
                    }
                    
                }
            };
            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 8485080996453793968L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };//Command
            ability.setStackDescription("As Meddling Mage enters the battlefield, name a nonland card.");
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Iona, Shield of Emeria")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        
                        String color = "";
                        String[] colors = Constant.Color.Colors;
                        colors[colors.length - 1] = null;
                        
                        Object o = AllZone.Display.getChoice("Choose color", colors);
                        color = (String) o;
                        card.setChosenColor(color);
                    } else {
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer);
                        CardList list = new CardList();
                        list.addAll(lib.getCards());
                        list.addAll(hand.getCards());
                        
                        if(list.size() > 0) {
                            String color = CardFactoryUtil.getMostProminentColor(list);
                            if(!color.equals("")) card.setChosenColor(color);
                            else card.setChosenColor("black");
                        } else {
                            card.setChosenColor("black");
                        }
                    }
                }
            };
            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 3331342605626623161L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };//Command
            ability.setStackDescription("As Iona, Shield of Emeria enters the battlefield, choose a color.");
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Chainer, Dementia Master")) {
            final Ability ability = new Ability(card, "B B B") {
                @Override
                public void resolve() {
                    card.getController().loseLife(3,card);
                    
                    PlayerZone hGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
                    PlayerZone cGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList creatures = new CardList();
                    creatures.addAll(hGrave.getCards());
                    creatures.addAll(cGrave.getCards());
                    

                    creatures = creatures.getType("Creature");
                    if(creatures.size() > 0) {
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            Object o = AllZone.Display.getChoice("Pick creature: ", creatures.toArray());
                            if(o != null) {
                                Card c = (Card) o;
                                PlayerZone zone = AllZone.getZone(c);
                                zone.remove(c);
                                play.add(c);
                                c.untap();
                                c.addExtrinsicKeyword(c.getName() + " is black.");
                                c.addType("Nightmare");
                                c.setController(card.getController());
                            }
                        } else {
                            Card c = CardFactoryUtil.AI_getBestCreature(creatures);
                            PlayerZone zone = AllZone.getZone(c);
                            zone.remove(c);
                            play.add(c);
                            c.untap();
                            c.addExtrinsicKeyword(c.getName() + " is black.");
                            c.addType("Nightmare");
                            c.setController(card.getController());
                        }
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    if(AllZone.ComputerPlayer.getLife() < 7) return false;
                    
                    PlayerZone hGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
                    PlayerZone cGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
                    CardList creatures = new CardList();
                    creatures.addAll(hGrave.getCards());
                    creatures.addAll(cGrave.getCards());
                    creatures = creatures.getType("Creature");
                    return creatures.size() > 0;
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone hGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
                    PlayerZone cGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
                    CardList creatures = new CardList();
                    creatures.addAll(hGrave.getCards());
                    creatures.addAll(cGrave.getCards());
                    creatures = creatures.getType("Creature");
                    return creatures.size() > 0 && super.canPlay();
                }
            };
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("B B B, Pay 3 life: Put target creature card from a graveyard onto the battlefield under your control. ");
            sbDesc.append("That creature is black and is a Nightmare in addition to its other creature types.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append("Put target creature card from a graveyard onto the battlefield under your control. ");
            sbStack.append("That creature is black and is a Nightmare in addition to its other creature types.");
            ability.setStackDescription(sbStack.toString());
            
            card.addSpellAbility(ability);
            
            final Command leavesPlay = new Command() {
                private static final long serialVersionUID = 3367460511478891560L;
                
                public void execute() {
                    PlayerZone hPlay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
                    PlayerZone cPlay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                    
                    CardList list = new CardList();
                    list.addAll(hPlay.getCards());
                    list.addAll(cPlay.getCards());
                    list = list.getType("Nightmare");
                    
                    for(Card c:list) {
                        AllZone.GameAction.exile(c);
                    }
                }
            };
            
            card.addLeavesPlayCommand(leavesPlay);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Swans of Bryn Argoll")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(final int n, final Card source) {
                    final Ability_Static ability = new Ability_Static(card, "0") {
                        @Override
                        public void resolve() {
                            Player player = source.getController();
                            for(int i = 0; i < n; i++)
                                player.drawCard();
                        }
                    };
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Swans of Bryn Argoll - ").append(source.getController());
                    sb.append(" draws ").append(n).append(" cards.");
                    ability.setStackDescription(sb.toString());
                    
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
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Dromad Purebred")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(HashMap<Card, Integer> map) {
                	final HashMap<Card, Integer> m = map;
                    final Ability ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            card.getController().gainLife(1, card);
                        }
                    };
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(card.getController()).append(" gains 1 life.");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                    
                    for(Entry<Card, Integer> entry : m.entrySet()) {
                        this.addDamage(entry.getValue(), entry.getKey());
                    }
                    
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
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        
        
      //*************** START *********** START **************************
        else if(cardName.equals("Sprouting Phytohydra")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(HashMap<Card, Integer> map) {
                	final HashMap<Card, Integer> m = map;
                    final Ability ability = new Ability(card, "0") {
                    	@Override
                    	public void resolve() {
                    		if(getController().isHuman() &&
                    				AllZone.Display.getChoice("Copy " + getSourceCard(),
                    						new String[] {"Yes", "No"}).equals("No"))
                    			return;
                    		PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, getSourceCard().getController());
                    		CardList DoublingSeasons = new CardList(play.getCards());
                    		DoublingSeasons = DoublingSeasons.getName("Doubling Season");
                    		PlayerZone_ComesIntoPlay.SimultaneousEntry = true;      
                    		double Count = DoublingSeasons.size();
                    		Count = Math.pow(2,Count);
                    		for(int i = 0; i < Count; i++) {
                    			if(i + 1== Count) PlayerZone_ComesIntoPlay.SimultaneousEntry = false;                 
                    			Card Copy = AllZone.CardFactory.copyCardintoNew(getSourceCard());
                    			Copy.setToken(true);
                    			Copy.setController(getSourceCard().getController());
                    			play.add(Copy); 
                    		}
                    	}
                    };
                    ability.setStackDescription(toString() + " - you may put a token that's a copy of " + getName() + " onto the battlefield.");
                    AllZone.Stack.add(ability);
                    
                    for(Entry<Card, Integer> entry : m.entrySet()) {
                        this.addDamage(entry.getValue(), entry.getKey());
                    }
                    
                }
            };
            
            newCard.setOwner(card.getOwner());
            newCard.setController(card.getController());
            
            newCard.setManaCost(card.getManaCost());
            newCard.setName(card.getName());
            newCard.setType(card.getType());
            newCard.setText(card.getSpellText());
            newCard.setBaseAttack(card.getBaseAttack());
            newCard.setBaseDefense(card.getBaseDefense());
            
            newCard.addSpellAbility(new Spell_Permanent(newCard));
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Thoughtcutter Agent")) {
        	Target target = new Target("Select target player", new String[] {"Player"});
        	Ability_Cost abCost = new Ability_Cost("U B T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = -3880035465617987801L;
                
                @Override
                public void resolve() {
                	Player player = getTargetPlayer();
                    CardList hand = AllZoneUtil.getPlayerHand(player);
                    player.loseLife(1, card);
                    if(player.equals(AllZone.ComputerPlayer)) {
                        AllZone.Display.getChoice("Look", hand.toArray());
                    }
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    //computer should play ability if this creature doesn't attack
                    Combat c = ComputerUtil.getAttackers();
                    CardList list = new CardList(c.getAttackers());
                    
                    //could this creature attack?, if attacks, do not use ability
                    return (!list.contains(card));
                }
            };//SpellAbility
            ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card.addSpellAbility(ability);
            ability.setDescription(abCost+"Target player loses 1 life and reveals his or her hand.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - target player loses 1 life.");
            ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Singe-Mind Ogre")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card choice = null;
                    Player opponent = card.getController().getOpponent();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
                    Card[] handChoices = hand.getCards();
                    if (handChoices.length > 0)
                    {
	                    choice = CardUtil.getRandom(handChoices);
	                    handChoices[0] = choice;
	                    for(int i = 1; i < handChoices.length; i++) {
	                        handChoices[i] = null;
	                    }
	                    AllZone.Display.getChoice("Random card", handChoices);
	                    opponent.loseLife(CardUtil.getConvertedManaCost(choice.getManaCost()),card);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = -4833144157620224716L;
                
                public void execute() {
                    ability.setStackDescription("Singe-Mind Ogre - target player reveals a card at random from " +
                    		"his or her hand, then loses life equal to that card's converted mana cost.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Lichenthrope")) {
        	final Card newCard = new Card()
        	{
        		@Override
                public void addDamage(final int n, final Card source) {
                    this.addCounter(Counters.M1M1, n);
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
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Phytohydra")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(final int n, final Card source) {
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
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        */

        //*************** START *********** START **************************
        else if(cardName.equals("Callous Giant")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    if(n <= 3) n = 0;
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
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Chronatog")) {
            
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 6926430725410883578L;
                
                public void execute() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(-3);
                        card.addTempDefenseBoost(-3);
                    }
                }
            };
            
            Ability_Cost abCost = new Ability_Cost("0", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -8345060615720565828L;

				@Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(3);
                        card.addTempDefenseBoost(3);
                        AllZone.EndOfTurn.addUntil(untilEOT);
                        
                        AllZone.Phase.skipTurn(card.getController());
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            ability.getRestrictions().setActivationLimit(1);
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" gets +3/+3 until end of turn, ");
            sb.append(card.getController()).append(" skips his/her next turn.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("0: Chronatog gets +3/+3 until end of turn. You skip your next turn. Activate this ability only once each turn.");
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Kinsbaile Borderguard")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, countKithkin());
                    //System.out.println("all counters: " +card.sumAllCounters());
                }//resolve()
                
                public int countKithkin() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList kithkin = new CardList(play.getCards());
                    kithkin = kithkin.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            return (c.getType().contains("Kithkin") || c.getKeyword().contains("Changeling"))
                                    && !c.equals(card);
                        }
                        
                    });
                    return kithkin.size();
                    
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7067218066522935060L;
                
                public void execute() {
                    ability.setStackDescription("Kinsbaile Borderguard enters the battlefield with a +1/+1 counter on it for each other Kithkin you control.");
                    AllZone.Stack.add(ability);
                }
            };
            
            final SpellAbility ability2 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    for(int i = 0; i < card.sumAllCounters(); i++) {
                        makeToken();
                    }
                }//resolve()
                
                public void makeToken() {
                    CardFactoryUtil.makeToken("Kithkin Soldier", "W 1 1 Kithkin Soldier", card.getController(), "W", new String[] {
                            "Creature", "Kithkin", "Soldier"}, 1, 1, new String[] {""});
                }
            };
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 304026662487997331L;
                
                public void execute() {
                    ability2.setStackDescription("When Kinsbaile Borderguard is put into a graveyard from play, put a 1/1 white " +
                    		"Kithkin Soldier creature token onto the battlefield for each counter on it.");
                    AllZone.Stack.add(ability2);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            card.addDestroyCommand(destroy);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lockjaw Snapper")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone hPlay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
                    PlayerZone cPlay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                    
                    CardList creatures = new CardList();
                    creatures.addAll(hPlay.getCards());
                    creatures.addAll(cPlay.getCards());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getCounters(Counters.M1M1) > 0;
                        }
                    });
                    
                    for(int i = 0; i < creatures.size(); i++) {
                        Card c = creatures.get(i);
                        c.addCounter(Counters.M1M1, 1);
                    }
                }
            };
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 6389028698247230474L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - put -1/-1 counter on each creature that has a -1/-1 counter on it.");
                	ability.setStackDescription(sb.toString());
                	
                    AllZone.Stack.add(ability);
                }
            };//command
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Arctic Nishoba")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int lifeGain = card.getCounters(Counters.AGE) * 2;
                    card.getController().gainLife(lifeGain, card);
                }
            };
            
            Command destroy = new Command() {
                private static final long serialVersionUID = 1863551466234257411L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - gain 2 life for each age counter on it.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };//command
            
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START ************************** 
        else if(cardName.equals("Kavu Titan")) {
            final SpellAbility kicker = new Spell(card) {
                private static final long serialVersionUID = -1598664196463358630L;
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    card.setKicked(true);
                    hand.remove(card);
                    play.add(card);
                }
                
                @Override
                public boolean canPlay() {
                    return super.canPlay() && AllZone.Phase.getPlayerTurn().equals(card.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && !AllZone.GameAction.isCardInPlay(card);
                }
                
            };
            kicker.setKickerAbility(true);
            kicker.setManaCost("3 G G");
            kicker.setAdditionalManaCost("2 G");
            kicker.setDescription("Kicker 2 G");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Creature 5/5 (Kicked)");
            kicker.setStackDescription(sb.toString());
            
            card.addSpellAbility(kicker);
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, 3);
                    card.addIntrinsicKeyword("Trample");

                	card.setKicked(false);
                }
            };
            
            Command commandComes = new Command() {
                private static final long serialVersionUID = -2622859088591798773L;
                
                public void execute() {
                    if(card.isKicked()) {
                            ability.setStackDescription("Kavu Titan gets 3 +1/+1 counters and gains trample.");
                            AllZone.Stack.add(ability);
                    }
                }//execute()
            };//CommandComes
            
            card.addComesIntoPlayCommand(commandComes);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START ************************** 
        else if(cardName.equals("Gatekeeper of Malakir")) {
            final SpellAbility kicker = new Spell(card) {
                private static final long serialVersionUID = -1598664186463358630L;
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    card.setKicked(true);
                    hand.remove(card);
                    play.add(card);
                    //card.comesIntoPlay(); //do i need this?
                }
                
                @Override
                public boolean canPlay() {
                    return super.canPlay() && AllZone.Phase.getPlayerTurn().equals(card.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && !AllZone.GameAction.isCardInPlay(card);
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
                    CardList cl = new CardList(play.getCards());
                    cl = cl.getType("Creature");
                    
                    return cl.size() > 0;
                }
                
            };
            kicker.setKickerAbility(true);
            kicker.setManaCost("B B B");
            kicker.setAdditionalManaCost("B");
            kicker.setDescription("Kicker B");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Creature 2/2 (Kicked)");
            kicker.setStackDescription(sb.toString());
            
            card.addSpellAbility(kicker);
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	if (card.getController().equals(AllZone.ComputerPlayer))
                		setTargetPlayer(AllZone.HumanPlayer);
                	getTargetPlayer().sacrificeCreature();

                	card.setKicked(false);
                }
            };
            
            Command commandComes = new Command() {
                private static final long serialVersionUID = -2622859088591798773L;
                
                public void execute() {
                    if(card.isKicked()) {
                        if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
                        else //computer
                        {
                            ability.setStackDescription("Gatekeeper of Malakir - targeting Human");
                            AllZone.Stack.add(ability);
                        }//else
                    }
                }//execute()
            };//CommandComes
            
            card.addComesIntoPlayCommand(commandComes);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Stoneforge Mystic")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
                        Card c = getTargetCard();
                        card.getController().shuffle();
                        lib.remove(c);
                        hand.add(c);
                        
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = 4022442363194287539L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList cards = new CardList(lib.getCards());
                    CardList arts = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Equipment")) {
                            arts.add(cards.get(i));
                        }
                    }
                    
                    Player controller = card.getController();
                    
                    if(arts.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", arts.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
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
        else if(cardName.equals("Gnarlid Pack") || cardName.equals("Apex Hawks") || cardName.equals("Enclave Elite") || 
                cardName.equals("Quag Vampires") || cardName.equals("Skitter of Lizards") ||
                cardName.equals("Joraga Warcaller"))
        {
        	final Ability_Static ability = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, card.getMultiKickerMagnitude());
                    card.setMultiKickerMagnitude(0);
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append(cardName);
            sb.append(" enters the battlefield with a +1/+1 counter on it for each time it was kicked.");
            ability.setStackDescription(sb.toString());
            
            final Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = 4245563898487609274L;

				public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Viridian Joiner"))
        {
        	Ability_Mana ma = new Ability_Mana(card, "tap: add an amount of G to your mana pool equal to CARDNAME's power.")
        	{
				private static final long serialVersionUID = 3818278127211421729L;

				public String mana()
        		{
        			StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < card.getNetAttack(); i++){
                    	if (i != 0)
                    		sb.append(" ");
                    	sb.append("G");
                    }
                    return sb.toString();
        		}
        	};
        	card.addSpellAbility(ma);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if (cardName.equals("Lightkeeper of Emeria"))
        {
        	final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	card.getController().gainLife(card.getMultiKickerMagnitude() * 2, card);
                    card.setMultiKickerMagnitude(0);
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append("Lightkeeper of Emeria enters the battlefield and ");
            sb.append(card.getController());
            sb.append(" gains 2 life for each time it was kicked.");
            ability.setStackDescription(sb.toString());
            
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 4418758359403878255L;

                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Borderland Ranger") || cardName.equals("Sylvan Ranger")
        		|| cardName.equals("Civic Wayfinder") || cardName.equals("Pilgrim's Eye")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    CardList basic = new CardList(lib.getCards());
                    basic = basic.getType("Basic");
                    
                    if(card.getController().equals(AllZone.ComputerPlayer)) {
                        if(basic.size() > 0) {
                            Card c = basic.get(0);
                            lib.remove(c);
                            hand.add(c);
                            
                        }
                    } else // human
                    {
                        if(basic.size() > 0) {
                            Object o = AllZone.Display.getChoiceOptional(
                                    "Select Basic Land card to put into your hand: ", basic.toArray());
                            if(o != null) {
                                Card c = (Card) o;
                                lib.remove(c);
                                hand.add(c);
                            }
                        }
                    }
                    card.getController().shuffle();
                }//resolve()
            };//Ability
            
            Command fetchBasicLand = new Command() {
                
				private static final long serialVersionUID = 7042012311958529153L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - search library for a basic land card and put it into your hand.");
					ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(fetchBasicLand);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Kazandu Tuskcaller"))
        {
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = 5172811502850812588L;
				@Override
                public void resolve() {
					Player controller = card.getController();
                    CardFactoryUtil.makeToken("Elephant", "G 3 3 Elephant", controller, "G", new String[] {
                                "Creature", "Elephant"}, 3, 3, new String[] {""});
                }
                public boolean canPlay()
                {
                	int lcs = card.getCounters(Counters.LEVEL);
                	return super.canPlay() && lcs >= 2 && lcs <= 5;
                }
            };//Ability
            ability.setDescription(abCost+"Put a 3/3 green Elephant creature token onto the battlefield.(LEVEL 2-5)");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - Put a 3/3 green Elephant creature token onto the battlefield.");
            ability.setStackDescription(sb.toString());
            
            Ability_Cost abCost2 = new Ability_Cost("T", cardName, true);
            final Ability_Activated ability2 = new Ability_Activated(card, abCost2, null) {

				private static final long serialVersionUID = 4795715660485178553L;
				@Override
                public void resolve() {
					Player controller = card.getController();
                    for (int i=0;i<2;i++)
                    	CardFactoryUtil.makeToken("Elephant", "G 3 3 Elephant", controller, "G", new String[] {
                                "Creature", "Elephant"}, 3, 3, new String[] {""});
                }
                public boolean canPlay()
                {
                	int lcs = card.getCounters(Counters.LEVEL);
                	return super.canPlay() && lcs >= 6;
                }
            };//Ability
            ability2.setDescription(abCost2+" Put two 3/3 green Elephant creature tokens onto the battlefield.(LEVEL 6+)");
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card).append(" - Put two 3/3 green Elephant creature tokens onto the battlefield.");
            ability2.setStackDescription(sb2.toString());
            
            card.addSpellAbility(ability);
            card.addSpellAbility(ability2);
    	}//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Venerated Teacher")) {
        	/*
        	 * When Venerated Teacher enters the battlefield, put two level counters
        	 * on each creature you control with level up.
        	 */
        	final Ability ability = new Ability(card, "0") {
        		
        		@Override
        		public void resolve() {
        			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
        			CardList level = new CardList(play.getCards());
        			level = level.filter(new CardListFilter() {
        				public boolean addCard(Card c) {
        					return c.hasLevelUp();
        				}
        			});
        			for( int i = 0; i < level.size(); i++ ) {
        				Card c = level.get(i);
        				c.addCounter(Counters.LEVEL, 2);
        			}
        		}//resolve()
        	};//Ability

        	Command addLevelCounters = new Command() {
				private static final long serialVersionUID = 1919112942772054206L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - Add 2 Level counters to each creature you control with Level up.");
					ability.setStackDescription(sb.toString());
        			
        			AllZone.Stack.add(ability);
        		}
        	};
        	card.addComesIntoPlayCommand(addLevelCounters);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Ichor Rats")) {

        	final Ability ability = new Ability(card, "0") {
        		
        		@Override
        		public void resolve() {
        			AllZone.HumanPlayer.addPoisonCounters(1);
        			AllZone.ComputerPlayer.addPoisonCounters(1);
        		}//resolve()
        	};//Ability

        	Command addPsnCounters = new Command() {
				private static final long serialVersionUID = 454918862752568246L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card).append(" - Each player gets a poison counter.");
					ability.setStackDescription(sb.toString());
        			
        			AllZone.Stack.add(ability);
        		}
        	};
        	card.addComesIntoPlayCommand(addPsnCounters);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Tuktuk the Explorer")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Tuktuk the Returned", "C 5 5 Tuktuk the Returned", card.getController(), "", 
                    		new String[] {"Legendary", "Artifact", "Creature", "Goblin", "Golem"}, 5, 5, new String[] {""});
                }//resolve()
            };//Ability
            
            Command destroy = new Command() {
				private static final long serialVersionUID = -2301867871037110012L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card.getController()).append(" puts a 5/5 Legendary Artifact Goblin Golem creature onto the battlefield.");
					ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Reveillark")) {

        	final SpellAbility ability = new Ability(card, "0") {
        		@Override
        		public void resolve() {
        			final Player player = card.getController();
        			PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
    				CardList graveList = new CardList(grave.getCards());
    				graveList = graveList.filter(new CardListFilter() {
    					public boolean addCard(Card c) {
    						return c.isCreature() && c.getNetAttack() <=2;
    					}
    				});
    				
    				if (graveList.size() == 0)
    					return;
    				
					PlayerZone battlefield = AllZone.getZone(Constant.Zone.Battlefield, player);
    				
        			if( player.equals(AllZone.HumanPlayer)) {
    					for(int i = 0; i < 2; i++) {
    						if(graveList.size() == 0)  break;
    						
    						Card c = AllZone.Display.getChoiceOptional("Select creature", graveList.toArray());
    						if(c == null) break;
    						AllZone.GameAction.moveTo(battlefield, c);
    						graveList.remove(c);
    					}
        			}
        			else{ //computer
    					for(int i=0; i < 2; i++) {
    						if(graveList.size() == 0)  break;
    						
    						Card c = CardFactoryUtil.AI_getBestCreature(graveList);
    						AllZone.GameAction.moveTo(battlefield, c);
    						graveList.remove(c);
    					}
        			}
        		}//resolve()
        	};//SpellAbility
        	Command leavesPlay = new Command() {
				private static final long serialVersionUID = -2495216861720523362L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - return up to 2 creatures with power < 2 from graveyard to the battlefield.");
					ability.setStackDescription(sb.toString());
					
        			AllZone.Stack.add(ability);
        		}//execute()
        	};
            card.addLeavesPlayCommand(leavesPlay);
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
				private static final long serialVersionUID = -3588276621923227230L;

				@Override
                public boolean canPlayAI() {
                	return true;
                }
            });
            
            card.addSpellAbility(new Spell_Evoke(card, "5 W") {
				private static final long serialVersionUID = -6197651256234977129L;

				@Override
                public boolean canPlayAI() {
                    return false;
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Phyrexian War Beast")) {
           /* When Phyrexian War Beast leaves the battlefield, sacrifice a land
            * and Phyrexian War Beast deals 1 damage to you.
            */
           final Ability ability = new Ability(card, "0") {
              private static final long serialVersionUID = -3829801813561677938L;

              public void resolve() {
                 Card c = getTargetCard();
                 if (c != null)
                	 AllZone.GameAction.sacrifice(c);
                 card.getController().addDamage(1, card);
              }
           };

           final Command sacrificeLandAndOneDamage = new Command() {
              private static final long serialVersionUID = -1793348608291550952L;

              public void execute() {
            	 Player player = card.getController();
            	 
            	 StringBuilder sb = new StringBuilder();
            	 sb.append(card.getName()).append(" - does 1 damage to ").append(player).append(" and sacrifice one land.");
            	 ability.setStackDescription(sb.toString());
                 
                 //AllZone.Stack.add(ability);
                 //probably want to check that there are lands in play
                 
                 PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield,player);
                 CardList choice = new CardList(play.getCards());
                 choice  = choice.getType("Land");
                 
                 if (choice.size() > 0)
                 {
	                 if (player.equals(AllZone.HumanPlayer))
		                 AllZone.InputControl.setInput(CardFactoryUtil.input_sacrificePermanent(ability, choice, "Select a land to sacrifice"));
	                 else //compy
	                 {
	               		 ability.setTargetCard(choice.get(0));
	                	 AllZone.Stack.add(ability);
	                 }
                 }
                 else
                	 AllZone.Stack.add(ability);
                 
              }
           };
           
           card.addLeavesPlayCommand(sacrificeLandAndOneDamage);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Tribal Forcemage")) {
        	/*
        	 * Morph: 1G
        	 * When Tribal Forcemage is turned face up, creatures of the creature
        	 * type of your choice get +2/+2 and gain trample until end of turn.
        	 */
        	final Command turnsFaceUp = new Command() {
				private static final long serialVersionUID = 2826741404979610245L;

				public void execute() {
        			final int pump = 2;
        			final Command eot = new Command() {
        				private static final long serialVersionUID = -3638246921594162776L;

        				public void execute() {
        					CardList type = AllZoneUtil.getCardsInPlay();
        					type = type.getType(card.getChosenType());

        					for(int i = 0; i < type.size(); i++) {
        						Card c = type.get(i);
        						c.addTempAttackBoost(-pump);
        						c.addTempDefenseBoost(-pump);
        						c.removeExtrinsicKeyword("Trample");
        					}
        					card.setChosenType(null);
        				}
        			};
        			final SpellAbility ability = new Ability(card, "0") {
        				@Override
        				public void resolve() {
        					String chosenType = "";
        					if(card.getController().equals(AllZone.HumanPlayer)) {
        						chosenType = JOptionPane.showInputDialog(null, "Select a card type:", card.getName(),
        								JOptionPane.QUESTION_MESSAGE);
        					}
        					else {
        						//TODO - this could probably be updated to get the most prominent type in play
        						chosenType = "Elf";
        					}
        					card.setChosenType(chosenType);
        					CardList type = AllZoneUtil.getCardsInPlay();
        					type = type.getType(chosenType);
        					for(int i = 0; i < type.size(); i++) {
        						Card c = type.get(i);
        						c.addTempAttackBoost(pump);
        						c.addTempDefenseBoost(pump);
        						c.addExtrinsicKeyword("Trample");
        					}
        					AllZone.EndOfTurn.addUntil(eot);
        				}
        			};//SpellAbility
        			
        			StringBuilder sb = new StringBuilder();
        			sb.append(card.getName()).append(" - chosen type gets +2/+2 and Trample until EOT");
        			ability.setStackDescription(sb.toString());
        			
        			AllZone.Stack.add(ability);
        		}//execute
        	};//command

        	card.addTurnFaceUpCommand(turnsFaceUp);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Kozilek's Predator") || cardName.equals("Dread Drone")) {
            final SpellAbility comesIntoPlayAbility = new Ability(card, "0") {
                @Override
                public void resolve() {
                    makeToken();
                    makeToken();
                }//resolve()
                
                public void makeToken() {
                	CardList cl = CardFactoryUtil.makeToken("Eldrazi Spawn", "C 0 1 Eldrazi Spawn", card.getController(), "", new String[] {
							"Creature", "Eldrazi", "Spawn"}, 0, 1, new String[] {"Sacrifice CARDNAME: Add 1 to your mana pool."});
        			for (Card crd:cl)
        				crd.addSpellAbility(CardFactoryUtil.getEldraziSpawnAbility(crd));
                }
                
            }; //comesIntoPlayAbility
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 4193134733200317562L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - put two 0/1 colorless Eldrazi Spawn creature tokens onto the battlefield.");
					comesIntoPlayAbility.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(comesIntoPlayAbility);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Emrakul's Hatcher")) {
            final SpellAbility comesIntoPlayAbility = new Ability(card, "0") {
                @Override
                public void resolve() {
                    makeToken();
                    makeToken();
                    makeToken();
                }//resolve()
                
                public void makeToken() {
                	CardList cl = CardFactoryUtil.makeToken("Eldrazi Spawn", "C 0 1 Eldrazi Spawn", card.getController(), "", new String[] {
							"Creature", "Eldrazi", "Spawn"}, 0, 1, new String[] {"Sacrifice CARDNAME: Add 1 to your mana pool."});
        			for (Card crd:cl)
        				crd.addSpellAbility(CardFactoryUtil.getEldraziSpawnAbility(crd));
                }
                
            }; //comesIntoPlayAbility
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -8661023016178518439L;

				public void execute() {
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - put three 0/1 colorless Eldrazi Spawn creature tokens onto the battlefield.");
					comesIntoPlayAbility.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(comesIntoPlayAbility);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Nest Invader")) {
            final SpellAbility comesIntoPlayAbility = new Ability(card, "0") {
                @Override
                public void resolve() {
                    makeToken();
                }//resolve()
                
                public void makeToken() {
                	CardList cl = CardFactoryUtil.makeToken("Eldrazi Spawn", "C 0 1 Eldrazi Spawn", card.getController(), "", new String[] {
							"Creature", "Eldrazi", "Spawn"}, 0, 1, new String[] {"Sacrifice CARDNAME: Add 1 to your mana pool."});
        			for (Card crd:cl)
        				crd.addSpellAbility(CardFactoryUtil.getEldraziSpawnAbility(crd));
                }
                
            }; //comesIntoPlayAbility
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 2179492272870559564L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - put a 0/1 colorless Eldrazi Spawn creature token onto the battlefield.");
					comesIntoPlayAbility.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(comesIntoPlayAbility);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Sage Owl") || cardName.equals("Inkfathom Divers") ||
        		cardName.equals("Sage Aven") || cardName.equals("Sage of Epityr") ||
        		cardName.equals("Spire Owl")) {
        	final SpellAbility ability = new Ability(card, "0") {
        		
        		@Override
        		public void resolve() {
        			if(card.getController().equals(AllZone.HumanPlayer)) {
        				AllZoneUtil.rearrangeTopOfLibrary(card.getController(), 4, false);
        			}
        		}//resolve()
        	};//SpellAbility
        	
        	Command intoPlay = new Command() {
        		private static final long serialVersionUID = 4757054648163014149L;

        		public void execute() {
        			AllZone.Stack.add(ability);
        		}
        	};
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Rearrange the top 4 cards in your library in any order.");
        	ability.setStackDescription(sb.toString());
        	
        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Storm Entity")) {
        	final SpellAbility intoPlay = new Ability(card, "0") {
        		
        		@Override
        		public boolean canPlayAI() {
    			CardList human = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
                CardListUtil.sortAttack(human);
        		 return (human.get(0).getNetAttack() < Phase.StormCount && Phase.StormCount > 1);
        		}
        		@Override
        		public void resolve() {
                    for(int i = 0; i < Phase.StormCount - 1; i++) {
                        card.addCounter(Counters.P1P1, 1);
        		    }  
        		}
        	};//SpellAbility
        	
        	Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = -3734151854295L;

				public void execute() {
        			AllZone.Stack.add(intoPlay);
        		}
        	};
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - enters the battlefield with a +1/+1 counter on it for each other spell played this turn.");
        	intoPlay.setStackDescription(sb.toString());
        	
        	card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END ************************** 
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Aven Fateshaper")) {
        	/*
        	 * When Aven Fateshaper enters the battlefield, look at the top four
        	 * cards of your library, then put them back in any order.
        	 * 4U: Look at the top four cards of your library, then put them back
        	 * in any order.
        	 */
        	final SpellAbility ability = new Ability(card, "4 U") {
        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}
        		@Override
        		public void resolve() {
        			if(card.getController().equals(AllZone.HumanPlayer)) {
        				AllZoneUtil.rearrangeTopOfLibrary(card.getController(), 4, false);
        			}
        		}   
        	};
        	final SpellAbility intoPlay = new Ability(card, "0") {
        		@Override
        		public void resolve() {
        			if(card.getController().equals(AllZone.HumanPlayer)) {
        				AllZoneUtil.rearrangeTopOfLibrary(card.getController(), 4, false);
        			}
        		}   
        	};
        	Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = -3735668300887854295L;

				public void execute() {
        			AllZone.Stack.add(intoPlay);
        		}
        	};
        	card.addSpellAbility(ability);
        	
        	StringBuilder sbIntoPlay = new StringBuilder();
        	sbIntoPlay.append(cardName).append(" - Rearrange the top 4 cards in your library in any order.");
        	intoPlay.setStackDescription(sbIntoPlay.toString());
        	
        	ability.setDescription("4U: Look at the top four cards of your library, then put them back in any order.");
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(cardName).append(" - Rearrange the top 4 cards in your library in any order.");
        	ability.setStackDescription(sbStack.toString());
        	
        	card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Descendant of Soramaro")) {
        	/*
        	 * 1U: Look at the top X cards of your library, where X is the number
        	 * of cards in your hand, then put them back in any order.
        	 */
        	final SpellAbility ability = new Ability(card, "1 U") {
        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}
        		@Override
        		public void resolve() {
        			if(card.getController().equals(AllZone.HumanPlayer)) {
        				int x = AllZoneUtil.getPlayerHand(card.getController()).size();
        				AllZoneUtil.rearrangeTopOfLibrary(card.getController(), x, false);
        			}
        		}   
        	};
        	card.addSpellAbility(ability);
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Rearrange the top X cards in your library in any order.");
        	ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Information Dealer")) {
        	/*
        	 * Tap: Look at the top X cards of your library, where X is the
        	 * number of Wizards on the battlefield, then put them back in any order.
        	 */
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = 3451190255076340818L;
				
				@Override
        		public boolean canPlayAI() {
        			return false;
        		}
        		@Override
        		public void resolve() {
        			if(card.getController().equals(AllZone.HumanPlayer)) {
        				int x = AllZoneUtil.getPlayerTypeInPlay(card.getController(), "Wizard").size();
        				AllZoneUtil.rearrangeTopOfLibrary(card.getController(), x, false);
        			}
        		}   
        	};
        	ability.setDescription(abCost+"Look at the top X cards of your library, where X is the number of Wizards on the battlefield, then put them back in any order.");
        	card.addSpellAbility(ability);
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Rearrange the top X cards in your library in any order.");
        	ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Dawnglare Invoker")) {
        	/*
        	 * 8: Tap all creatures target player controls.
        	 */
        	final SpellAbility ability = new Ability(card, "8") {
        		@Override
        		public boolean canPlayAI() {
        			CardList human = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
        			human = human.filter(AllZoneUtil.tapped);
        			return human.size() > 0 && AllZone.Phase.getPhase().equals("Main1");
        		}
        		@Override
        		public void resolve() {
        			final Player player = getTargetPlayer();
        			CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
        			for(Card c:creatures) {
        				if( c.isUntapped() ) {
        					c.tap();
        				}
        			}
        		}   
        	};
        	card.addSpellAbility(ability);
        	ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
        	ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sphinx of Magosi")) {
        	/*
        	 * 2 U: Draw a card, then put a +1/+1 counter on Sphinx of Magosi.
        	 */
        	final SpellAbility ability = new Ability(card, "2 U") {
        		@Override
        		public void resolve() {
        			final Player player = card.getController();
        			player.drawCards(1);
        			card.addCounter(Counters.P1P1, 1);
        		}   
        	};
        	card.addSpellAbility(ability);
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - add a +1+1 counter and draw a card.");
        	ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Arc-Slogger")) {
        	/*
        	 * R, Exile the top ten cards of your library: Arc-Slogger deals
        	 * 2 damage to target creature or player.
        	 */
        	final SpellAbility ability = new Ability(card, "R") {
        		@Override
        		public boolean canPlayAI() {
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer);
        			int life = AllZone.HumanPlayer.getLife();
        			if(lib.size() > 10 && life <=2) {
        				return true;
        			}
        			else{
        				return false;
        			}
        		}
        		@Override
        		public void resolve() {
        			int damage = 2;
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
        			int max = Math.min(lib.size(), 10);
        			for(int i = 0; i < max; i++) {
        				//remove the top card 10 times
        				AllZone.GameAction.exile(lib.get(0));
        			}
        			if(getTargetCard() != null) {
        				if(AllZone.GameAction.isCardInPlay(getTargetCard())
        						&& CardFactoryUtil.canTarget(card, getTargetCard())) {
        					getTargetCard().addDamage(damage, card);
        				}
        			} else getTargetPlayer().addDamage(damage, card);

        		}   
        	};
        	card.addSpellAbility(ability);
        	ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true, false));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cemetery Reaper")) {
        	Ability_Cost abCost = new Ability_Cost("2 B T", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, null)
        	{
				private static final long serialVersionUID = 1067370853723993280L;

				public void makeToken(Card c)
				{
					AllZone.GameAction.exile(c);
            		CardFactoryUtil.makeToken("Zombie", "B 2 2 Zombie", card.getController(), "B", new String[] {
                            "Creature", "Zombie"}, 2, 2, new String[] {""});
				}
				
				public void resolve()
        		{
        			CardList list = AllZoneUtil.getCardsInGraveyard();
        			list = list.getType("Creature");
        			
        			if(list.size() > 0) {
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            Object o = AllZone.Display.getChoice("Pick creature to exile: ", list.toArray());
                            if(o != null) {
                            	Card c = (Card)o;
                            	if (AllZone.GameAction.isCardInGrave(c))
                            		makeToken(c);
                            }
                        } else {
                            Card c = list.get(0);
                            if (AllZone.GameAction.isCardInGrave(c))
                        		makeToken(c);
                        }
                    }
        		}
				
				public boolean canPlayAI()
				{
					//AI will only use this when there's creatures in human's graveyard:
					CardList humanList = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
					humanList = humanList.getType("Creature");
					return humanList.size() > 0;
				}
        	};
        	card.addSpellAbility(ability);
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append(abCost);
        	sbDesc.append("Exile target creature card from a graveyard. ");
        	sbDesc.append("Put a 2/2 black Zombie creature token onto the battlefield.");
        	ability.setDescription(sbDesc.toString());
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(card).append("Exile target creature card from a graveyard. ");
        	sbStack.append("Put a 2/2 black Zombie creature token onto the battlefield.");
        	ability.setStackDescription(sbStack.toString());
    	}//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Vampire Hexmage")) {
        	/*
        	 * Sacrifice Vampire Hexmage: Remove all counters from target permanent.
        	 */
        	final SpellAbility ability = new Ability(card, "0") {

        		@Override
        		public boolean canPlayAI() {
        			
        			//Dark Depths:
        			CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer, "Dark Depths");
        			list = list.filter(new CardListFilter(){
        				public boolean addCard(Card crd)
        				{
        					return crd.getCounters(Counters.ICE) >= 3;
        				}
        			});
        			
        			if (list.size()>0)
        			{
        				setTargetCard(list.get(0));
        				return true;
        			}
        			
        			//Get rid of Planeswalkers:
        			list = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
        			list = list.filter(new CardListFilter(){
        				public boolean addCard(Card crd)
        				{
        					return crd.isPlaneswalker() && crd.getCounters(Counters.LOYALTY) >= 5;
        				}
        			});
        			
        			if (list.size()>0)
        			{
        				setTargetCard(list.get(0));
        				return true;
        			}
        			
        			return false;
        		}
        		
        		@Override
        		public boolean canPlay() {
        			return AllZoneUtil.isCardInPlay(card) && super.canPlay();
        		}

        		@Override
        		public void resolve() {
        			final Card c = getTargetCard();
        			for(Counters counter:Counters.values()) {
        				if(c.getCounters(counter) > 0) {
        					c.setCounter(counter, 0, false);
        				}
        			}
        			AllZone.GameAction.sacrifice(card);
        		}
        	};
        	card.addSpellAbility(ability);
        	ability.setBeforePayMana(CardFactoryUtil.input_targetPermanent(ability));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Kargan Dragonlord"))
        {
        	Ability_Cost abCost = new Ability_Cost("R", cardName, true);
        	
	        final SpellAbility ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -2252408767635375616L;

				@Override
	            public boolean canPlayAI() {
	                
	                if(AllZone.Phase.getPhase().equals(Constant.Phase.Main2)) return false;
	                
	                setTargetCard(card);
	                    
	                if((card.hasSickness() && (!card.getKeyword().contains("Haste"))) ) return false;
	                else {
	                	Random r = new Random();
	                    if(r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed())) return CardFactoryUtil.AI_doesCreatureAttack(card);
	                }
	                return false;
	            }
	            
	            @Override
	            public boolean canPlay() {
	                return card.getCounters(Counters.LEVEL) >= 8 && super.canPlay();
	            }
	            
	            @Override
	            public void resolve() {
	                if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
	                    final Card[] creature = new Card[1];
	                    creature[0] = card;
	                    
	                    final Command EOT = new Command() {
	                        
							private static final long serialVersionUID = 3161373279207630319L;

							public void execute() {
	                            if(AllZone.GameAction.isCardInPlay(creature[0])) {
	                                creature[0].addTempAttackBoost(-1);
	                            }
	                        }
	                    };
	                    
	                    creature[0].addTempAttackBoost(1);

	                    card.setAbilityUsed(card.getAbilityUsed() + 1);
	                    AllZone.EndOfTurn.addUntil(EOT);
	                }//if (card is in play)
	            }//resolve()
	        };//SpellAbility
	        
	        ability.setDescription("R: Kargan Dragonlord gets +1/+0 until end of turn. (LEVEL 8+)");
	        //ability.setStackDescription(stDesc[0]);
	        
	        ability.setTargetCard(card);
	        card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Symbiotic Wurm")) {
            final SpellAbility ability = new Ability(card, "0") {

                @Override
                public void resolve() {
                    for(int i = 0; i < 7; i++) {
                        makeToken();
                    }
                }
        
                void makeToken() {
                    CardFactoryUtil.makeToken("Insect", "G 1 1 Insect", card.getController(), "G", new String[] {
                            "Creature", "Insect"}, 1, 1, new String[] {""});
                }//makeToken()
            };//SpellAbility
        
            Command destroy = new Command() {
                private static final long serialVersionUID = -7121390569051656027L;

                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Symbiotic Wurm - ").append(card.getController()).append(" puts seven 1/1 tokens onto the battlefield");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Totem-Guide Hartebeest")) {
        	final SpellAbility ability = new Ability(card, "0") {

        		@Override
        		public void resolve() {
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
        			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
        			if (AllZone.GameAction.isCardInZone(getTargetCard(), lib)) {
        				Card c = getTargetCard();
        				card.getController().shuffle();
        				lib.remove(c);
        				hand.add(c);
        			}
        		}//resolve()
        	};//spell ability

        	Command intoPlay = new Command() {
				private static final long serialVersionUID = -4230274815515610713L;

				public void execute() {
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
        			CardList cards = new CardList(lib.getCards());
        			CardList auras = new CardList();

        			for (int i = 0; i < cards.size(); i++) {
        				if (cards.get(i).getType().contains("Enchantment") && cards.get(i).getType().contains("Aura")) {
        					auras.add(cards.get(i));
        				}
        			}

        			Player controller = card.getController();

        			if(auras.size() == 0) return;

        			if (controller.equals(AllZone.HumanPlayer)) {
        				Object o = AllZone.Display.getChoiceOptional("Select target card", auras.toArray());
        				if (o != null) {
        					ability.setTargetCard((Card) o);
        					AllZone.Stack.add(ability);
        				}
        			} else {    //computer	
        				auras.shuffle();
        				ability.setTargetCard(auras.get(0));
        				AllZone.Stack.add(ability);
        			}
        		}//execute()
        	};//Command
        	
        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Roc Egg")) {
            final SpellAbility ability = new Ability(card, "0") {
                
                @Override
                public void resolve() {
                    
                    CardFactoryUtil.makeToken("Bird", "W 3 3 Bird", card.getController(), "W", new String[] {"Creature", "Bird"},
                            3, 3, new String[] {"Flying"});
                    
                }// resolve()
            };// ability

            Command destroy = new Command() {
                private static final long serialVersionUID = 159321399857094976L;

                public void execute() {
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getController()).append(" - puts a 3/3 white Bird creature token with flying onto the battlefield.");
                    ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                    
                }// execute()
            };// Command destroy
            
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Yavimaya Elder")) {
    	        
    	        final Command destroy = new Command()
    	        {
					private static final long serialVersionUID = -5552202665064265632L;

					public void execute()
    	        	{
    	        		AllZone.GameAction.searchLibraryTwoBasicLand(card.getController(), Constant.Zone.Hand, false, Constant.Zone.Hand, false);
    	        	}
    	        };
    	        card.addDestroyCommand(destroy);   	    
        }//*************** END ************ END **************************
       
        
        //*************** START *********** START **************************
          else if(cardName.equals("Overgrown Battlement")) {
              final Ability_Mana ability = new Ability_Mana(card,"tap: add G to your mana pool for each creature with defender you control.") {
     
  				private static final long serialVersionUID = 422282090183907L;

  				@Override
                  public String mana() {
                      String res = "";
                      
                      CardList cl = new CardList(AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
                      
                      cl = cl.filter(new CardListFilter() {

                      	public boolean addCard(Card c)
                      	{
                      		return c.hasKeyword("Defender");
                      	}
                      	
                      });
                      
                      for(int i=0;i<cl.size();i++)
                      {
                      	res = res + "G ";
                      }
                      
                      if(!res.equals(""))
                      {
                      	res = res.substring(0,res.length()-1);
                      }
                      
                      return res;
                  }//mana()                
                  
              };//Ability_Mana

              card.addSpellAbility(ability);
          }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
          else if(cardName.equals("Sutured Ghoul")) {
        	  final int[] numCreatures = new int[1];
        	  final int[] sumPower = new int[1];
        	  final int[] sumToughness = new int[1];

        	  Command intoPlay = new Command() {
        		  private static final long serialVersionUID = -75234586897814L;

        		  public void execute() {
        			  int intermSumPower,intermSumToughness;
        			  intermSumPower = intermSumToughness = 0;
        			  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
        			  CardList creats = new CardList(grave.getCards());
        			  creats = creats.filter(new CardListFilter() {
        				  public boolean addCard(Card c) {
        					  return c.isCreature() && !c.equals(card);
        				  }
        			  });

        			  if(card.getController().equals(AllZone.HumanPlayer)) {
        				  if (creats.size() > 0)
        				  {
        					  List<Card> selection = AllZone.Display.getChoicesOptional("Select creatures to sacrifice", creats.toArray());

        					  numCreatures[0] = selection.size();
        					  for(int m = 0; m < selection.size(); m++) {
        						  intermSumPower += selection.get(m).getBaseAttack();
        						  intermSumToughness += selection.get(m).getBaseDefense();
        						  AllZone.GameAction.exile(selection.get(m));
        					  }
        				  }

        			  }//human
        			  else {
        				  int count = 0;
        				  for(int i = 0; i < creats.size(); i++) {
        					  Card c = creats.get(i);
        					  if(c.getNetAttack() <= 2 && c.getNetDefense() <= 3) {
        						  intermSumPower += c.getBaseAttack();
        						  intermSumToughness += c.getBaseDefense();
        						  AllZone.GameAction.exile(c);
        						  count++;
        					  }
        					  //is this needed?
        					  AllZone.Computer_Battlefield.updateObservers();
        				  }
        				  numCreatures[0] = count;
        			  }
        			  sumPower[0] = intermSumPower;
        			  sumToughness[0] = intermSumToughness;
        			  card.setBaseAttack(sumPower[0]);
        			  card.setBaseDefense(sumToughness[0]);
        		  }
        	  };

        	  card.clearSpellAbility();
        	  card.addComesIntoPlayCommand(intoPlay);
        	  card.addSpellAbility(new Spell_Permanent(card) {
        		  private static final long serialVersionUID = 304885517082977723L;

        		  @Override
        		  public boolean canPlayAI() {
        			  //get all creatures
        			  CardList list = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
        			  list = list.filter(AllZoneUtil.creatures);
        			  return 0 < list.size();
        		  }
        	  });
          }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if(cardName.equals("Singing Tree")) {
            final String Tgts[] = {"Creature.attacking"};
            Target target = new Target("Select target attacking creature.", Tgts);
          
            final Ability_Cost cost = new Ability_Cost("T", card.getName(), true);

            final SpellAbility ability = new Ability_Activated(card, cost, target) {
				private static final long serialVersionUID = 3750045284339229395L;

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
                public Card getCreature() {
                    CardList attacking = AllZoneUtil.getCreaturesInPlay();
                    attacking = attacking.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isAttacking() && c != card && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    if(attacking.isEmpty()) return null;
                    
                    Card big = CardFactoryUtil.AI_getBestCreature(attacking);
                    return big;
                }
                
                @Override
                public boolean canPlay() {
                	return Phase.canPlayDuringCombat();
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
							private static final long serialVersionUID = -7188543458319933986L;

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
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sunblast Angel") ) {
                        
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList tappedCreatures = AllZoneUtil.getCreaturesInPlay();
                    tappedCreatures = tappedCreatures.filter(AllZoneUtil.tapped);
                    for(Card c:tappedCreatures) {
                    	AllZone.GameAction.destroy(c);
                    }
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -8702934390670388771L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card).append(" - destroy all tapped creatures.");
					ability.setStackDescription(sb.toString());
					
                    AllZone.Stack.add(ability);
                }//execute()
            };
                       
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Thundermare") || cardName.equals("Timbermare")) {
        	/*
        	 * When Thundermare enters the battlefield, tap all other creatures.
        	 */
        	final SpellAbility ability = new Ability(card, "0") {
        		
        		@Override
        		public void resolve() {
        			CardList cards = AllZoneUtil.getCreaturesInPlay();
        			cards.remove(card);
        			for(Card c:cards) c.tap();
        		}//resolve()
        	};//SpellAbility
        	
        	Command intoPlay = new Command() {
				private static final long serialVersionUID = -692103773738198353L;

				public void execute() {
        			AllZone.Stack.add(ability);
        		}
        	};
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - tap all other creatures.");
        	ability.setStackDescription(sb.toString());
        	
        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END *************************
       
        
        //*************** START *********** START **************************
        else if(cardName.equals("Nameless Race")) {
        	/*
        	 * As Nameless Race enters the battlefield, pay any amount of life.
        	 * The amount you pay can't be more than the total number of white
        	 * nontoken permanents your opponents control plus the total number
        	 * of white cards in their graveyards.
        	 * Nameless Race's power and toughness are each equal to the life
        	 * paid as it entered the battlefield.
        	 */
        	final SpellAbility ability = new Ability(card, "0") {
        		@Override
        		public void resolve() {
        			Player player = card.getController();
        			Player opp = player.getOpponent();
        			int max = 0;
        			CardList play = AllZoneUtil.getPlayerCardsInPlay(opp);
        			play = play.filter(AllZoneUtil.nonToken);
        			play = play.filter(AllZoneUtil.white);
        			max += play.size();
        			
        			CardList grave = AllZoneUtil.getPlayerGraveyard(opp);
        			grave = grave.filter(AllZoneUtil.white);
        			max += grave.size();
        			
        			String[] life = new String[max+1];
        			for(int i = 0; i <= max; i++) {
        				life[i] = String.valueOf(i);
        			}
        			
        			Object o = AllZone.Display.getChoice("Nameless Race - pay X life", life);
        			String answer = (String) o;
        			int loseLife = 0;
        			try {
        				loseLife = Integer.parseInt(answer.trim());
        			}
        			catch (NumberFormatException nfe) {
        				System.out.println(card.getName()+" - NumberFormatException: " + nfe.getMessage());
        			}
        			
        			card.setBaseAttack(loseLife);
        			card.setBaseDefense(loseLife);
        			
        			player.loseLife(loseLife, card);
        		}//resolve()
        	};//SpellAbility
        	
        	Command intoPlay = new Command() {
				private static final long serialVersionUID = 931101364538995898L;

				public void execute() {
        			AllZone.Stack.add(ability);
        		}
        	};
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - pay any amount of life.");
        	ability.setStackDescription(sb.toString());
        	
        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Architects of Will")) {
        	/*
        	 * When Architects of Will enters the battlefield, look at the
        	 * top three cards of target player's library, then put them
        	 * back in any order.
        	 */
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    AllZoneUtil.rearrangeTopOfLibrary(getTargetPlayer(), 3, false);
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 3539746365351917811L;

				public void execute() {
                	if(card.getController().equals(AllZone.HumanPlayer)) {
                        AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
                        ButtonUtil.disableAll();
                    }
                	else { //Computer
                        //not implemented for computer
                    }//else
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - rearrange top 3 cards of target player's library.");
            ability.setStackDescription(sb.toString());
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Leveler")) {
        	/*
        	 * When Leveler enters the battlefield, exile all cards from your library.
        	 */
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList lib = AllZoneUtil.getPlayerCardsInLibrary(card.getController());
                    for(Card c:lib) AllZone.GameAction.exile(c);
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -5462488189911159119L;

				public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - exile all cards from your library.");
            ability.setStackDescription(sb.toString());
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Banshee")) {
        	/*
        	 * X, Tap: Banshee deals half X damage, rounded down, to target creature or
        	 * player, and half X damage, rounded up, to you.
        	 */
        	
        	Ability_Cost abCost = new Ability_Cost("X T", cardName, true);
        	Target tgt = new Target("TgtCP");
        	
        	final Ability_Activated ability = new Ability_Activated(card, abCost, tgt) {
				private static final long serialVersionUID = 2755743211116192949L;

				@Override
        		public void resolve() {
        			int x = card.getXManaCostPaid();
        			if(getTargetPlayer() == null) {
        				getTargetCard().addDamage((int)Math.floor(x/2.0), card);
        			}
        			else {
        				getTargetPlayer().addDamage((int)Math.floor(x/2.0), card);
        			}
        			card.getController().addDamage((int)Math.ceil(x/2.0), card);
        			card.setXManaCostPaid(0);
        		}//resolve()
				
				@Override
				public boolean canPlayAI() {
					return false;
				}
        		
        	};//SpellAbility
        	
        	ability.setDescription("X, tap: "+"Banshee deals half X damage, rounded down, to target creature or player, and half X damage, rounded up, to you.");
        	ability.setStackDescription(card.getName()+" - Banshee deals half X damage, rounded down, to target creature or player, and half X damage, rounded up, to you.");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Shapeshifter")) {
        	Command intoPlay = new Command() {
				private static final long serialVersionUID = 5447692676152380940L;

				public void execute() {
					if(!card.isToken()) {  //ugly hack to get around tokens created by Crib Swap
						int num = 0;
						if(card.getController().isHuman()) {
							String[] choices = new String[7];
							for(int j = 0; j < 7; j++) {
								choices[j] = ""+j;
							}
							String answer = (String)(AllZone.Display.getChoiceOptional(
									card.getName()+" - Choose a number", choices));
							num = Integer.parseInt(answer);
						}
						else {
							num = 3;
						}
						card.setBaseAttack(num);
						card.setBaseDefense(7-num);
					}
				}
        	};

        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Metalworker")) {
        	final Ability_Cost abCost = new Ability_Cost("T", card.getName(), true);
        	
        	final SpellAbility ability = new Ability_Activated(card, abCost, null) {
        		private static final long serialVersionUID = 6661308920885136284L;
        		
        		@Override
        		public boolean canPlayAI() {
        			//compy doesn't have a manapool
        			return false;
        		}//canPlayAI()
        		
        		@Override
        		public void resolve() {
        			AllZone.InputControl.setInput(new Input() {
						private static final long serialVersionUID = 6150236529653275947L;
        				CardList revealed = new CardList();

        				@Override
        				public void showMessage() {
        					//in case hand is empty, don't do anything
        					if (AllZoneUtil.getPlayerHand(card.getController()).size() == 0) stop();

        					AllZone.Display.showMessage(card.getName()+" - Reveal an artifact.  Revealed "+revealed.size()+" so far.  Click OK when done.");
        					ButtonUtil.enableOnlyOK();
        				}

        				@Override
        				public void selectCard(Card c, PlayerZone zone) {
        					if(zone.is(Constant.Zone.Hand) && c.isArtifact() && !revealed.contains(c)) {
        						revealed.add(c);

        						//in case no more cards in hand to reveal
        						if(revealed.size() == AllZoneUtil.getPlayerHand(card.getController()).size()) done();
        						else
        							showMessage();
        					}
        				}
        				
        				@Override
        				public void selectButtonOK() {
        					done();
        				}

        				void done() {
        					StringBuilder sb = new StringBuilder();
        					for(Card reveal:revealed) sb.append(reveal.getName()+"\n");
        					JOptionPane.showMessageDialog(null, "Revealed Cards:\n"+sb.toString(), card.getName(), JOptionPane.PLAIN_MESSAGE);
        					//adding mana
        					for(int i = 0; i < revealed.size(); i++) {
        						AllZone.ManaPool.addManaToFloating("2", card);
        					}
        					stop();
        				}
        			});
        		}//resolve()
        	};//SpellAbility

        	ability.setDescription(abCost+"Reveal any number of artifact cards in your hand. Add 2 to your mana pool for each card revealed this way.");
        	ability.setStackDescription(cardName+" - Reveal any number of artifact cards in your hand.");
        	card.addSpellAbility(ability); 
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sea Gate Oracle")) {
        	final Ability ability = new Ability(card, "") {
        		@Override
        		public void resolve() {
        			if(card.getController().isHuman()) {
        				PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
        				int maxCards = lib.size();
        				maxCards = Math.min(maxCards, 2);
        				if(maxCards == 0) return;
        				CardList topCards =   new CardList();
        				//show top n cards:
        				for(int j = 0; j < maxCards; j++ ) {
        					topCards.add(lib.get(j));
        				}
        				Object o = AllZone.Display.getChoice("Put one card in your hand", topCards.toArray());
    					if(o != null) {
    						Card c_1 = (Card) o;
    						topCards.remove(c_1);
    						AllZone.GameAction.moveToHand(c_1);
    					}
    					for(Card c:topCards) {
    						AllZone.GameAction.moveToBottomOfLibrary(c);
    					}
        			}
        		}
        	};
        	ability.setStackDescription(cardName+" - Look at the top two cards of your library. Put one of them into your hand and the other on the bottom of your library.");
        	Command intoPlay = new Command() {
				private static final long serialVersionUID = -4300804642226899861L;

				public void execute() {
        			AllZone.Stack.add(ability);
        		}
        	};

        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START ************************** 
        else if(cardName.equals("Skizzik")) {
            final SpellAbility kicker = new Spell(card) {
                private static final long serialVersionUID = -1598664196463358630L;
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    card.setKicked(true);
                    hand.remove(card);
                    play.add(card);
                }
                
                @Override
                public boolean canPlay() {
                    return super.canPlay() && AllZone.Phase.getPlayerTurn().equals(card.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && !AllZone.GameAction.isCardInPlay(card);
                }
                
            };
            kicker.setKickerAbility(true);
            kicker.setManaCost("3 R R");
            kicker.setAdditionalManaCost("R");
            kicker.setDescription("Kicker R");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Creature 5/3 (Kicked)");
            kicker.setStackDescription(sb.toString());
            
            card.addSpellAbility(kicker);
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.removeIntrinsicKeyword("At the beginning of the end step, sacrifice CARDNAME.");
                	card.setKicked(false);
                }
            };
            
            Command commandComes = new Command() {
				private static final long serialVersionUID = -5895458115371755529L;

				public void execute() {
                    if(card.isKicked()) {
                            ability.setStackDescription(card.getName()+" is not sacrificed at end of turn.");
                            AllZone.Stack.add(ability);
                    }
                }//execute()
            };//CommandComes
            
            card.addComesIntoPlayCommand(commandComes);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Karn, Silver Golem")) {
        	final long[] timeStamp = new long[1];
        	Ability_Cost abCost = new Ability_Cost("1", cardName, true);
        	Target target = new Target("Select target noncreature artifact", "Artifact.nonCreature".split(","));
            Ability_Activated ability = new Ability_Activated(card, abCost, target) {
				private static final long serialVersionUID = -8888163768273148474L;

				@Override
                public boolean canPlayAI() {
					return false;
                }
                
                @Override
                public void resolve() {
                	final Card c = getTargetCard();
                    final String[] types = { "Creature" };
                    final String[] keywords = { };
                    int pt = CardUtil.getConvertedManaCost(c);
                    timeStamp[0] = CardFactoryUtil.activateManland(c, pt, pt, types, keywords, "0");

                    final Command eot1 = new Command() {
						private static final long serialVersionUID = 1430561816965534163L;

						public void execute() {
                        	long stamp = timeStamp[0];
                            CardFactoryUtil.revertManland(c, types, keywords, "0", stamp);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(eot1);
                }//resolve()
            };//SpellAbility
            ability.setDescription(abCost+"Target noncreature artifact becomes an artifact creature with power and toughness each equal to its converted mana cost until end of turn.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" creating artifact creature.");
            ability.setStackDescription(sb.toString());
            card.addSpellAbility(ability);
            card.setSVar("PlayMain1", "TRUE");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************  
        else if(cardName.equals("Plague Spitter")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	for(Player p:AllZoneUtil.getPlayersInGame()) p.addDamage(1, card);
					for(Card c:AllZoneUtil.getCreaturesInPlay()) c.addDamage(1, card);
                }
            };
            
            Command damage = new Command() {
                private static final long serialVersionUID = 206350020224577500L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
        			sb.append(card.getName()).append(" - deals 1 damage to each creature and each player.");
        			ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(damage);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Necratog")) {
            final Command untilEOT = new Command() {
				private static final long serialVersionUID = 6743592637334556854L;

				public void execute() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(-2);
                        card.addTempDefenseBoost(-2);
                    }
                }
            };
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override 
                public boolean canPlay() {
                	CardList grave = AllZoneUtil.getPlayerGraveyard(card.getController());
                	grave = grave.filter(AllZoneUtil.creatures);
                	return super.canPlay() && grave.size() > 0;
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(2);
                        card.addTempDefenseBoost(2);
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }
            };
            
            Input runtime = new Input() {
				private static final long serialVersionUID = 63327418012595048L;
				Card topCreature = null;
            	public void showMessage() {
            		
            		PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
            		for(int i = grave.size()-1; i >=0; i--) {
            			Card c = grave.get(i);
            			if(c.isCreature()) {
            				topCreature = c;
            				break;
            			}
            		}
            		AllZone.Display.showMessage(card.getName()+" - Select OK to exile "+topCreature+".");
            		ButtonUtil.enableAll();
            	}
            	
            	public void selectButtonOK() {
            		AllZone.GameAction.exile(topCreature);
            		AllZone.Stack.add(ability);
            		stop();
            	}
            	
            	public void selectButtonCancel() {
            		stop();
            	}
            };
            
            
            ability.setDescription("Exile the top creature card of your graveyard: CARDNAME gets +2/+2 until end of turn.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets +2/+2 until end of turn.");
            ability.setStackDescription(sb.toString());
            ability.setBeforePayMana(runtime);
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START ************************** 
        else if(cardName.equals("Phyrexian Scuta")) {
        	Ability_Cost abCost = new Ability_Cost("3 B PayLife<3>", cardName, false);
            final SpellAbility kicker = new Spell(card, abCost, null) {
				private static final long serialVersionUID = -6420757044982294960L;

				@Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    card.setKicked(true);
                    hand.remove(card);
                    play.add(card);
                    card.addCounterFromNonEffect(Counters.P1P1, 2);
                }
                
                @Override
                public boolean canPlay() {
                    return super.canPlay() && card.getController().getLife() >= 3;
                }
                
            };
            kicker.setKickerAbility(true);
            kicker.setManaCost("3 B");
            kicker.setDescription("Kicker - Pay 3 life.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Creature 3/3 (Kicked)");
            kicker.setStackDescription(sb.toString());
            
            card.addSpellAbility(kicker);
        }//*************** END ************ END **************************
        
        
        //*************** START ************ START **************************
        else if(cardName.equals("Wall of Roots")) {
        	/*
        	 * Put a -0/-1 counter on Wall of Roots: Add  to your mana pool. Activate this ability only once each turn.
        	 */
        	Ability_Cost abCost = new Ability_Cost("AddCounter<1/M0M1>", cardName, true);
        	Ability_Activated mana = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = 5369063561394954892L;

				@Override
        		public void resolve() {
        			AllZone.ManaPool.addManaToFloating("G", card);
        		}
				
				@Override
				public boolean canPlayAI() {
					return false;
				}
        	};
        	
        	mana.getRestrictions().setActivationLimit(1);
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append(abCost).append("Add G to your mana pool. Activate this ability only once each turn.");
        	mana.setDescription(sbDesc.toString());
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(cardName).append(" - add G to your mana pool.");
        	mana.setStackDescription(sbStack.toString());
        	
        	card.addSpellAbility(mana);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Eater of Days")) {
        	final Ability ability = new Ability(card, "") {
        		@Override
        		public void resolve() {
        			AllZone.Phase.addExtraTurn(card.getController().getOpponent());
        			AllZone.Phase.addExtraTurn(card.getController().getOpponent());
        		}
        	};
        	ability.setStackDescription(cardName+" - "+card.getController()+" skips his or her next two turns.");
        	Command intoPlay = new Command() {
				private static final long serialVersionUID = 2021250034977097040L;

				public void execute() {
        			AllZone.Stack.add(ability);
        		}
        	};
        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Yosei, the Morning Star")) {
        	final CardList targetPerms = new CardList();
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	Player p = getTargetPlayer();
                	if(p.canTarget(card)) {
                		p.setSkipNextUntap(true);
                		for(Card c:targetPerms) {
                    		if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                    			c.tap();
                    		}
                    	}
                	}
                	targetPerms.clear();
                }//resolve()
            };
            
            final Input targetInput = new Input() {
                private static final long serialVersionUID = -8727869672234802473L;
                
                @Override
                public void showMessage() {
                    if(targetPerms.size() == 5) done();
                	AllZone.Display.showMessage("Select up to 5 target permanents.  Selected ("+targetPerms.size()+") so far.  Click OK when done.");
                    ButtonUtil.enableOnlyOK();
                }
                
                @Override
                public void selectButtonOK() {
                	done();
                }
                
                private void done() {
                	//here, we add the ability to the stack since it's triggered.
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - tap up to 5 permanents target player controls. Target player skips his or her next untap step.");
                	ability.setStackDescription(sb.toString());
                	AllZone.Stack.add(ability);
                	stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                	if(zone.is(Constant.Zone.Battlefield, ability.getTargetPlayer()) && !targetPerms.contains(c)) {
                		if(CardFactoryUtil.canTarget(card, c)) {
                			targetPerms.add(c);
                		}
                	}
                    showMessage();
                }
            };//Input
            
            final Input playerInput = new Input() {
				private static final long serialVersionUID = 4765535692144126496L;

				@Override
            	public void showMessage() {
            		AllZone.Display.showMessage(card.getName()+" - Select target player");
            		ButtonUtil.enableOnlyCancel();
            	}
            	
            	@Override
            	public void selectPlayer(Player p) {
            		if(p.canTarget(card)) {
            			ability.setTargetPlayer(p);
            			stopSetNext(targetInput);
            		}
            	}
            	
            	@Override
            	public void selectButtonCancel() { stop(); }
            };
            
            Command destroy = new Command() {
                private static final long serialVersionUID = -3868616119471172026L;
                
                public void execute() {
                	Player player = card.getController();
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
                    
                    if(player.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(playerInput);
                    else if(list.size() != 0) {
                        Card target = CardFactoryUtil.AI_getBestCreature(list);
                        ability.setTargetCard(target);
                        AllZone.Stack.add(ability);
                    }
                }//execute()
            };
            card.addDestroyCommand(destroy);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Phyrexian Dreadnought")) {
        	final Command comesIntoPlay = new Command() {
        		private static final long serialVersionUID = 7680692311339496770L;
        		final Player player = card.getController();
        		final CardList toSac = new CardList();

        		public void execute() {
        			if(player.isHuman()) {
        				Input target = new Input() {
        					private static final long serialVersionUID = 2698036349873486664L;
        					
        					@Override
        					public void showMessage() {
        						String toDisplay = cardName+" - Select any number of creatures to sacrifice.  ";
        						toDisplay += "Currently, ("+toSac.size()+") selected with a total power of: "+getTotalPower();
        						toDisplay += "  Click OK when Done.";
        						AllZone.Display.showMessage(toDisplay);
        						ButtonUtil.enableAll();
        					}
        					
        					@Override
        					public void selectButtonOK() {
        						done();
        					}
        					
        					@Override
        					public void selectButtonCancel() {
        						toSac.clear();
        						AllZone.GameAction.sacrifice(card);
        						stop();
        					}
        					
        					@Override
        					public void selectCard(Card c, PlayerZone zone) {
        						if(c.isCreature() && zone.is(Constant.Zone.Battlefield, AllZone.HumanPlayer)
        								&& !toSac.contains(c)) {
        							toSac.add(c);
        						}
        						showMessage();
        					}//selectCard()
        					
        					private void done() {
        						if(getTotalPower() >= 12) {
        							for(Card sac:toSac) AllZone.GameAction.sacrifice(sac);
        						}
        						else {
        							AllZone.GameAction.sacrifice(card);
        						}
        						toSac.clear();
        						stop();
        					}
        				};//Input
        				AllZone.InputControl.setInput(target);
        			}
        		}

        		private int getTotalPower() {
        			int sum = 0;
        			for(Card c:toSac) {
        				sum += c.getNetAttack();
        			}
        			return sum;
        		}
        	};

        	card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Clone") || cardName.equals("Vesuvan Doppelganger") 
        		|| cardName.equals("Quicksilver Gargantuan")) {
        	final CardFactory cfact = cf;
        	final Card[] copyTarget = new Card[1];
        	final Card[] cloned = new Card[1];
        	
        	final SpellAbility copyBack = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card orig = cfact.getCard(card.getName(), card.getController());
                    PlayerZone dest = AllZone.getZone(card.getCurrentlyCloningCard());
                    AllZone.GameAction.moveTo(dest, orig);
                    dest.remove(card.getCurrentlyCloningCard());
                }
            };//SpellAbility
        	
        	final Command leaves = new Command() {
				private static final long serialVersionUID = 8590474793502538215L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - reverting self to "+card.getName()+".");
                    copyBack.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(copyBack);
                }
            };
        	
        	final SpellAbility copy = new Spell(card) {
				private static final long serialVersionUID = 4496978456522751302L;

				@Override
                public void resolve() {
					if (card.getController().isComputer()) {
						CardList creatures = AllZoneUtil.getCreaturesInPlay();
						if(!creatures.isEmpty()) {
							copyTarget[0] = CardFactoryUtil.AI_getBestCreature(creatures);
						}
					}
					
					if (copyTarget[0] != null) {
						cloned[0] = cfact.getCard(copyTarget[0].getName(), card.getController());
						cloned[0].setCloneOrigin(card);
						cloned[0].addLeavesPlayCommand(leaves);
						cloned[0].setCloneLeavesPlayCommand(leaves);
						cloned[0].setCurSetCode(copyTarget[0].getCurSetCode());
						cloned[0].setImageFilename(copyTarget[0].getImageFilename());
						if(cardName.equals("Vesuvan Doppelganger")) {
							cloned[0].addExtrinsicKeyword("At the beginning of your upkeep, you may have this creature become a copy of target creature except it doesn't copy that creature's color. If you do, this creature gains this ability.");
							cloned[0].addColor("U", cloned[0], false, true);
						}
						
						else if (cardName.equals("Quicksilver Gargantuan")) {
							cloned[0].setBaseDefense(7);
							cloned[0].setBaseAttack(7);
						}
						PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
						play.add(cloned[0]);
						card.setCurrentlyCloningCard(cloned[0]);
					}
                }
            };//SpellAbility
            
            Input runtime = new Input() {
				private static final long serialVersionUID = 7615038074569687330L;

				@Override
            	public void showMessage() {
            		AllZone.Display.showMessage(cardName+" - Select a creature on the battlefield");
            		ButtonUtil.enableOnlyCancel();
            	}
				
				@Override
				public void selectButtonCancel() { stop(); }
            	
            	@Override
            	public void selectCard(Card c, PlayerZone z) {
            		if( z.is(Constant.Zone.Battlefield) && c.isCreature()) {
            			copyTarget[0] = c;
            			stopSetNext(new Input_PayManaCost(copy));
            		}
            	}
            };
            card.clearSpellAbility();
            card.addSpellAbility(copy);
            copy.setStackDescription(cardName+" - enters the battlefield as a copy of selected card.");
            copy.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        
        //*************** START ************ START **************************
        else if(cardName.equals("Nebuchadnezzar")) {
        	/*
        	 * X, T: Name a card. Target opponent reveals X cards at random from his or her hand. 
        	 * Then that player discards all cards with that name revealed this way. 
        	 * Activate this ability only during your turn.
        	 */ 
        	Ability_Cost abCost = new Ability_Cost("X T", cardName, true);
        	Target target = new Target("Select target opponent", "Opponent".split(","));
        	Ability_Activated discard = new Ability_Activated(card, abCost, target) {
				private static final long serialVersionUID = 4839778470534392198L;

				@Override
        		public void resolve() {
        			//name a card
					String choice = JOptionPane.showInputDialog(null, "Name a card", cardName, JOptionPane.QUESTION_MESSAGE);
					CardList hand = AllZoneUtil.getPlayerHand(getTargetPlayer());
					int numCards = card.getXManaCostPaid();
					numCards = Math.min(hand.size(), numCards);
					
					CardList revealed = new CardList();
					for(int i = 0; i < numCards; i++) {
						Card random = CardUtil.getRandom(hand.toArray());
						revealed.add(random);
						hand.remove(random);
					}
					if(!revealed.isEmpty()) {
						AllZone.Display.getChoice("Revealed at random", revealed.toArray());
					}
					else {
						AllZone.Display.getChoice("Revealed at random", new String[] {"Nothing to reveal"});
					}
					
					for(Card c:revealed) {
						if(c.getName().equals(choice)) c.getController().discard(c, this);
					}
        		}
				
				@Override
				public boolean canPlayAI() {
					return false;
				}
        	};
        	
        	discard.getRestrictions().setPlayerTurn(true);
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append(abCost).append("Name a card. Target opponent reveals X cards at random from his or her hand. ");
        	sbDesc.append("Then that player discards all cards with that name revealed this way. ");
        	sbDesc.append("Activate this ability only during your turn.");
        	discard.setDescription(sbDesc.toString());
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(cardName).append(" - name a card.");
        	discard.setStackDescription(sbStack.toString());
        	
        	card.addSpellAbility(discard);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Accursed Centaur") || cardName.equals("Commander Greven il-Vec") ||
        		cardName.equals("Kjeldoran Dead") || cardName.equals("Spined Fluke") ||
        		cardName.equals("Vindictive Mob")) {
        	
        	final Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = -6986957647765851979L;
				
        		public void execute() {
        			Player player = card.getController();
        			CardList type = AllZoneUtil.getCreaturesInPlay(player);
        			type.remove(card);

        			if(player.isComputer()) {
        				if( type.size() > 0 ) {
        					Card sac = CardFactoryUtil.AI_getWorstCreature(type);
        					AllZone.GameAction.sacrifice(sac);
        				}
        				else {
        					AllZone.GameAction.sacrifice(card);
        				}
        			}
        			else { //this is the human resolution
        				Input target = new Input() {
							private static final long serialVersionUID = 2795318747338985959L;
							public void showMessage() {
        						AllZone.Display.showMessage(cardName+" - Select a creature to sacrifice");
        						ButtonUtil.enableOnlyCancel();
        					}
        					public void selectButtonCancel() {
        						AllZone.GameAction.sacrifice(card);
        						stop();
        					}
        					public void selectCard(Card c, PlayerZone zone) {
        						if(zone.is(Constant.Zone.Battlefield, card.getController()) && c.isCreature()) {
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
        else if(cardName.equals("Denizen of the Deep")) {
            final SpellAbility returnAll = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(card.getController());
                    creatures.remove(card);
                    for(Card c:creatures) {
                    	AllZone.GameAction.moveToHand(c);
                    }
                }
            };
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 7181675096954076868L;

				public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(cardName).append(" - ");
                	sb.append("return each other creature you control to its owner's hand.");
                	returnAll.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(returnAll);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
               
        
        if(hasKeyword(card, "Level up") != -1 && hasKeyword(card, "maxLevel") != -1)
        {
        	int n = hasKeyword(card, "Level up");
        	int m = hasKeyword(card, "maxLevel");
        	if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                String parseMax = card.getKeyword().get(m).toString();
                
                card.removeIntrinsicKeyword(parse);
                card.removeIntrinsicKeyword(parseMax);
                
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                String l[] = parseMax.split(":");
                final int maxLevel = Integer.parseInt(l[1]);
                
                final Ability levelUp = new Ability(card, manacost){
                	public void resolve()
                	{
                		card.addCounter(Counters.LEVEL, 1);
                	}
                	
                	public boolean canPlay()
                	{
                		//only as sorcery
                		return AllZone.getZone(card).is(Constant.Zone.Battlefield) && Phase.canCastSorcery(card.getController());
                	}
                	
                	public boolean canPlayAI()
                	{
                		return card.getCounters(Counters.LEVEL) < maxLevel;
                	}
                	
                };
                card.addSpellAbility(levelUp);
                
                StringBuilder sbDesc = new StringBuilder();
                sbDesc.append("Level up ").append(manacost).append(" (").append(manacost);
                sbDesc.append(": Put a level counter on this. Level up only as a sorcery.)");
                levelUp.setDescription(sbDesc.toString());
                
                StringBuilder sbStack = new StringBuilder();
                sbStack.append(card).append(" - put a level counter on this.");
                levelUp.setStackDescription(sbStack.toString());
                
                card.setLevelUp(true);
                
            }
        }//level up
        
        return card;
    }
}
