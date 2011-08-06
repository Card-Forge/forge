
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
    

    public static Card getCard(final Card card, String cardName, Player owner, CardFactory cf) {
        /*
        //*************** START *********** START **************************
        if(cardName.equals("Filthy Cur")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(final int n, Card source) {
                    super.addDamage(n, source);
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            getController().loseLife(n,card);
                        }
                    };
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Filthy Cur - ").append(getController()).append(" loses ").append(n).append(" life.");
                    ability.setStackDescription(sb.toString());
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
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        if(cardName.equals("Stone Giant")) {
        	final ArrayList<Card> Tgt = new ArrayList<Card>();
        	
        	final Command untilEOT = new Command() {

				private static final long serialVersionUID = -6829468812598100958L;

				//@Override
        		public void execute() {
        			for(Card c : Tgt)
        			{
        				AllZone.GameAction.destroy(c);
        			}
        			Tgt.clear();
        		}
        	};
        	
        	final SpellAbility ability = new Ability_Tap(card) {

				private static final long serialVersionUID = -807485081546883513L;

				public void resolve() {
					getTargetCard().addOtherExtrinsicKeyword("Flying");
					Tgt.add(getTargetCard());
					AllZone.EndOfTurn.addAt(untilEOT);
        		}
        		
        		public boolean canPlay() {
        			CardList creats = new CardList(AllZone.getZone(Constant.Zone.Play,card.getController()).getCards());
        			
        			creats = creats.filter(new CardListFilter() {
        				public boolean addCard(Card c) {
        					return c.getType().contains("Creature") && c.getNetDefense() < card.getNetAttack();
        				}
        			});
        			
        			return creats.size() > 0 && !card.isTapped() && !card.isSick();        			
        		}
        	};
        	
        	Input runtime = new Input() {

				private static final long serialVersionUID = 2101131021036402902L;

				@Override
        		public void showMessage() {
					CardList valids = new CardList(AllZone.getZone(Constant.Zone.Play,card.getController()).getCards());
        			valids = valids.filter(new CardListFilter() {
        				public boolean addCard(Card c) {
        					return c.getNetDefense() < card.getNetAttack(); 
        				}
        			});
        			
        			stopSetNext(CardFactoryUtil.input_targetSpecific(ability,valids,"Select target creature you control with toughness less than Stone Giant's power.",true,false));
        		}
        	};
        	ability.setBeforePayMana(runtime);
        	ability.setDescription("Tap: Target creature you control with toughness less than Stone Giant's power gains flying until end of turn. Destroy that creature at the beginning of the next end step.");
        	
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Belligerent Hatchling") || cardName.equals("Noxious Hatchling")
                || cardName.equals("Shrewd Hatchling") || cardName.equals("Sturdy Hatchling")
                || cardName.equals("Voracious Hatchling")) {
            final SpellAbility ability = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.M1M1, 4);
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4757054648163014149L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" enters the battlefield with four -1/-1 counters on it.");
            ability.setStackDescription(sb.toString());
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lurking Informant")) {
            final SpellAbility a1 = new Ability_Tap(card, "2") {
                private static final long serialVersionUID = 1446529067071763245L;
                
                @Override
                public void resolve() {
                    Player player = getTargetPlayer();
                    
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
                    CardList libList = new CardList(lib.getCards());
                    Card c = libList.get(0);
                    String[] choices = {"Yes", "No"};
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoice("Mill " + c.getName() + " ?", choices);
                        if(o.equals("Yes")) {
                            lib.remove(c);
                            grave.add(c);
                        }
                    } else {
                        CardList landlist = new CardList();
                        landlist.addAll(AllZone.Human_Play.getCards());
                        // i have no better idea how AI could use it then letting draw unneeded lands
                        // this part will be good place to use card values lists or card values deck info 
                        if(countLands(card) > 5 && !c.getType().contains("Land")) {
                            lib.remove(c);
                            grave.add(c);
                        }
                        if(countLands(card) <= 5) {
                            lib.remove(c);
                            grave.add(c);
                        }
                    }
                    

                }
                
                private int countLands(Card c) {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                    CardList lands = new CardList(play.getCards());
                    lands = lands.getType("Land");
                    return lands.size();
                }
                
                @Override
                public boolean canPlayAI() {
                    Player player = getTargetPlayer();
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Shinka Gatekeeper")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(final int n, Card source) {
                    super.addDamage(n, source);
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            getController().addDamage(n, card);
                        }
                    };
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Shinka Gatekeeper - causes ").append(n).append(" damage to ").append(getController());
                    ability.setStackDescription(sb.toString());
                    
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
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Jackal Pup")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(final int n, final Card source) {
                    super.addDamage(n, source);
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            getController().addDamage(n, card);
                        }
                    };
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Jackal Pup - causes ").append(n).append(" damage to ").append(getController());
                    ability.setStackDescription(sb.toString());
                    
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
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Stuffy Doll")) {
        	/
            final Card newCard = new Card() {
                Card c = this;
                
                @Override
                public void addDamage(final int n, final Card source) {
                    super.addDamage(n, source);
                    final Player opponent = card.getOwner().getOpponent();
                    
                    SpellAbility ability = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                        	//AllZone.GameAction.addDamage(opponent, c, n);
                        	opponent.addDamage(n, c);
                        }
                    };
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Stuffy Doll - causes ").append(n).append(" damage to ").append(opponent);
                    ability.setStackDescription(sb.toString());
                    
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
            *
            
			Ability_Cost abilCost = new Ability_Cost("T", cardName, true);

			final Ability_Activated ability = new Ability_Activated(card, abilCost, null){
                private static final long serialVersionUID = 577739727089395613L;
                
                @Override
                public void resolve() {
                    card.addDamage(1, card);
                }
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(abilCost.toString()).append("Stuffy Doll deals 1 damage to itself.");
            ability.setDescription(sb.toString());
            ability.setStackDescription("Stuffy Doll - deals 1 damage to itself.");
            
            card.addSpellAbility(ability);
            /*
            ability.setStackDescription("Stuffy Doll - deals 1 damage to itself.");
            
            newCard.addSpellAbility(new Spell_Permanent(newCard));
            newCard.addSpellAbility(ability);
            
            newCard.addSpellAbility(ability);
            
            newCard.setSVars(card.getSVars());
            
            return newCard;*
        }//*************** END ************ END **************************
        */

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
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    
                    return list.containsName("Glorious Anthem") || list.containsName("Gaea's Anthem");
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Greater Forgeling")) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -4569751606008597903L;
                
                public void execute() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(-3);
                        card.addTempDefenseBoost(3);
                    }
                }
            };
            
            SpellAbility ability = new Ability(card, "1 R") {
                @Override
                public boolean canPlayAI() {
                    return MyRandom.random.nextBoolean() && CardFactoryUtil.AI_doesCreatureAttack(card)
                            && 3 < card.getNetDefense();
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addTempAttackBoost(3);
                        card.addTempDefenseBoost(-3);
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }
            };
            
            ability.setDescription("1 R: Greater Forgeling gets +3/-3 until end of turn.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets +3/-3 until end of turn.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
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
            ability.setStackDescription("Caller of the Claw - Put a 2/2 green Bear creature token into play for each nontoken creature put into your graveyard from play this turn.");
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
            
            final SpellAbility ability = new Ability_Tap(card) {
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
                    CardList list = null;
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        list = new CardList(AllZone.Human_Play.getCards());
                    } else {
                        list = new CardList(AllZone.Computer_Play.getCards());
                    }
                    
                    list = list.getType("Creature");
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
	                                         	
	                        Card copy;
	                        if(!getTargetCard().isToken()) {
	                            //CardFactory cf = new CardFactory("cards.txt");
	                            
	
	                            //copy creature and put it into play
	                            //copy = getCard(getTargetCard(), getTargetCard().getName(), card.getController());
	                            copy = cfact.getCard(getTargetCard().getName(), getTargetCard().getOwner());
	                            
	                            //when copying something stolen:
	                            copy.setController(getTargetCard().getController());
	                            
	                            copy.setToken(true);
	                            copy.setCopiedToken(true);
	                            
	                            if(getTargetCard().isFaceDown()) {
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
	                        
	                        
	                        
	                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 7171284831370490875L;
                
                @Override
                public void showMessage() {
                    //get all non-legendary creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (!c.getType().contains("Legendary"));
                        }
                    });
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list,
                            "Select target creature to copy that is not legendary.", true, false));
                }
            };//Input
            ability.setStackDescription("Kiki-Jiki - copy card.");
            
            StringBuilder sb = new StringBuilder();
            sb.append("tap: Put a token into play that's a copy of target nonlegendary creature you control. ");
            sb.append("That creature token has haste. Sacrifice it at end of turn.");
            ability.setDescription(sb.toString());
            
            ability.setBeforePayMana(runtime);
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Sliver Overlord")) {
            //final String player = card.getController();
            final SpellAbility ability = new Ability(card, "3") {
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    if(AllZone.GameAction.isCardInPlay(card) && super.canPlay()) return true;
                    else return false;
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList list = new CardList(lib.getCards());
                    list = list.getType("Sliver");
                    
                    if(list.size() == 0) return false;
                    
                    if(AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && list.size() > 0) return true;
                    else return false;
                    
                }
                
                
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    Player player = card.getController();
                    
                    CardList list = new CardList(lib.getCards());
                    list = list.getType("Sliver");
                    
                    if(list.size() == 0) return;
                    
                    if(player.equals(AllZone.ComputerPlayer)) {
                        Card sliver = CardFactoryUtil.AI_getBestCreature(list);
                        lib.remove(sliver);
                        hand.add(sliver);
                    } else //human
                    {
                        Object o = AllZone.Display.getChoiceOptional("Select target Sliver", list.toArray());
                        Card sliver = (Card) o;
                        lib.remove(sliver);
                        hand.add(sliver);
                    }
                    player.shuffle();
                }
            };
            
            final SpellAbility ability2 = new Ability(card, "3") {
                
                @Override
                public void resolve() {
                    

                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)
                            && (c.getType().contains("Sliver") || c.getKeyword().contains("Changeling"))
                            && CardFactoryUtil.canTarget(card, c)) {
                        //set summoning sickness
                        if(c.getKeyword().contains("Haste")) {
                            c.setSickness(false);
                        } else {
                            c.setSickness(true);
                        }
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        PlayerZone from = AllZone.getZone(c);
                        from.remove(c);
                        
                        c.setController(card.getController());
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
                        to.add(c);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                    }//if
                    

                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    
                    CardList c = CardFactoryUtil.AI_getHumanCreature(card, true);
                    CardListUtil.sortAttack(c);
                    CardListUtil.sortFlying(c);
                    c = c.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            return c.getType().contains("Sliver") || c.getKeyword().contains("Changeling");
                        }
                        
                    });
                    
                    if(c.isEmpty()) return false;
                    
                    if(2 <= c.get(0).getNetAttack() && c.get(0).getKeyword().contains("Flying")
                            && c.get(0).getKeyword().contains("Sliver")) {
                        setTargetCard(c.get(0));
                        return true;
                    }
                    
                    CardListUtil.sortAttack(c);
                    if(4 <= c.get(0).getNetAttack() && c.get(0).getKeyword().contains("Sliver")) {
                        setTargetCard(c.get(0));
                        return true;
                    }
                    
                    return false;
                }//canPlayAI()
                
                @Override
                public boolean canPlay() {
                    return AllZone.GameAction.isCardInPlay(card) && super.canPlay();
                    
                }//canPlay()
            };//SpellAbility ability2
            
            ability2.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 1489433384490805477L;
                
                @Override
                public void showMessage() {
                    Player opponent = card.getController().getOpponent();
                    CardList slivers = new CardList(AllZone.getZone(Constant.Zone.Play, opponent).getCards());
                    slivers = slivers.getType("Sliver");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability2, slivers, "Select a Sliver", true,
                            false));
                }
            });
            
            ability.setDescription("3: Search your library for a Sliver card, reveal that card, and put it into your hand. Then shuffle your library.");
            StringBuilder sb1 = new StringBuilder();
            sb1.append(card.getName()).append(" - search for a Sliver card and put it into your hand.");
            ability.setStackDescription(sb1.toString());
            
            ability2.setDescription("3: Gain control of target Sliver.");
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - Gain control of target Sliver.");
            ability2.setStackDescription(sb2.toString());
            
            card.addSpellAbility(ability);
            card.addSpellAbility(ability2);
        }//*************** END ************ END **************************
        */
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Sliver Queen")) {
            final SpellAbility a1 = new Ability(card, "2") {
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    //this is a hack, check the stack to see if this card has an ability on the stack
                    //if so, we can't use the ability
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    return AllZone.GameAction.isCardInPlay(card) && super.canPlay();
                }
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Sliver", "C 1 1 Sliver", card, "", new String[] {
                            "Creature", "Sliver"}, 1, 1, new String[] {""});
                }
                
                @Override
                public boolean canPlayAI() {
                    return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
                }
            };//SpellAbility
            card.addSpellAbility(a1);
            a1.setDescription("2: Put a 1/1 colorless Sliver creature token into play.");
            a1.setStackDescription("Put a 1/1 colorless Sliver creature token into play.");
            
            a1.setBeforePayMana(new Input_PayManaCost(a1));
        }//*************** END ************ END **************************
        */

        //*************** START *********** START **************************
        else if(cardName.equals("Korlash, Heir to Blackblade")) {
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void chooseTargetAI() {
                    PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(p.getCards());
                    list = list.getName(card.getName());
                    
                    list.get(0).getController().discard(list.get(0), this);
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(p.getCards());
                    list = list.getName(card.getName());
                    return 0 < list.size() && AllZone.getZone(card).getZoneName().equals(Constant.Zone.Play) && super.canPlay();
                }
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList list = new CardList(library.getCards());
                    CardList swamp = list.getType("Swamp");
                    
                    /*
                    for(int i = 0; i < 2 && (!swamp.isEmpty()); i++) {
                        Card c = swamp.get(0);
                        swamp.remove(c);
                        
                        library.remove(c);
                        play.add(c);
                        c.tap();
                    }
                    */
                    
                    if (swamp.size() > 0)
                    {
                    	if (card.getController().equals(AllZone.HumanPlayer))
                    	{
		                    List<Card> selection = AllZone.Display.getChoices("Select up to two swamps", swamp.toArray());
		                    
		                    for(int i = 0; i < selection.size(); i++) {
		                        if (i == 2)
		                        	break;
		                    	Card c = selection.get(i);
		                        
		                        library.remove(c);
		                        play.add(c);
		                        c.tap();
		                    }
	                    }
                    	else
                    	{
                    		Card c = swamp.get(0);
                    		play.add(c);
                    		c.tap();
                    		swamp.remove(c);
                    		if (swamp.size() > 0) {
                    			c = swamp.get(0);
                    			play.add(c);
                    			c.tap();
                    			
                    		}
                    	}
                    }
                    
                    for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
                        Command com = GameActionUtil.commands.get(effect);
                        com.execute();
                    }
                    GameActionUtil.executeCardStateEffects();
                }
            };
            Input removeCard = new Input() {
                private static final long serialVersionUID = -8560221326412798885L;
                
                int                       n                = 0;
                
                @Override
                public void showMessage() {
                    //this is called twice, this is an ugly hack
                    if(n % 2 == 0) stop();
                    

                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(hand.getCards());
                    
                    list = list.getName(card.getName());
                    list.get(0).getController().discard(list.get(0), ability);
                    
                    AllZone.Stack.add(ability);
                    stop();
                }
            };
            ability.setBeforePayMana(removeCard);
            
            ability.setDescription("Grandeur - Discard Korlash and put two Swamps from your library into play tapped.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Search for two swamps and put them into play tapped.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Tarox Bladewing")) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 2642394522583318055L;
                
                public void execute() {
                    int n = card.getNetAttack();
                    
                    card.addTempDefenseBoost(-n / 2);
                    card.addTempAttackBoost(-n / 2);
                }
            };
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void chooseTargetAI() {
                    PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(p.getCards());
                    list = list.getName(card.getName());
                    
                    list.get(0).getController().discard(list.get(0), this);
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(p.getCards());
                    list = list.getName(card.getName());
                    return 0 < list.size() && AllZone.getZone(card).getZoneName().equals(Constant.Zone.Play) && super.canPlay();
                }
                
                @Override
                public void resolve() {
                    card.addTempDefenseBoost(card.getNetAttack());
                    card.addTempAttackBoost(card.getNetAttack());
                    
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }
            };
            Input removeCard = new Input() {
                private static final long serialVersionUID = -1312910959802746127L;
                
                int                       n                = 0;
                
                @Override
                public void showMessage() {
                    //this is called twice, this is an ugly hack
                    if(n % 2 == 0) stop();
                    

                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(hand.getCards());
                    
                    list = list.getName(card.getName());
                    list.get(0).getController().discard(list.get(0), ability);
                    
                    AllZone.Stack.add(ability);
                    stop();
                }
            };
            ability.setBeforePayMana(removeCard);
            
            ability.setDescription("Grandeur - Discard another card named Tarox Bladewing: Tarox Bladewing gets +X/+X until end of turn, where X is his power.");
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - gets +X/+X until end of turn.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Baru, Fist of Krosa")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void chooseTargetAI() {
                    PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(p.getCards());
                    list = list.getName(card.getName());
                    
                    list.get(0).getController().discard(list.get(0), this);
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone p = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(p.getCards());
                    list = list.getName(card.getName());
                    return 0 < list.size() && AllZone.getZone(card).getZoneName().equals(Constant.Zone.Play) && super.canPlay();
                }
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Land");
                    CardFactoryUtil.makeToken("Wurm", "G X X Wurm", card.getController(), "G", new String[] {"Creature", "Wurm"},
                            list.size(), list.size(), new String[] {""});
                }
            };
            Input removeCard = new Input() {
                private static final long serialVersionUID = 7738090787920616790L;
                
                int                       n                = 0;
                
                @Override
                public void showMessage() {
                    //this is called twice, this is an ugly hack
                    if(n % 2 == 0) stop();
                    

                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(hand.getCards());
                    
                    list = list.getName(card.getName());
                    list.get(0).getController().discard(list.get(0), ability);
                    
                    AllZone.Stack.add(ability);
                    stop();
                }
            };
            ability.setBeforePayMana(removeCard);
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Grandeur - Discard another card named Baru, Fist of Krosa: Put an X/X ");
            sbDesc.append("green Wurm creature token into play, where X is the number of lands that you control.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(cardName).append(" - put X/X token into play.");
            ability.setStackDescription(sbStack.toString());
            
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                || cardName.equals("Jedit's Dragoons") || cardName.equals("Loxodon Hierarch")
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
            final Ability ability = new Ability(card, "G W") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    final Card[] c = AllZone.getZone(Constant.Zone.Play, card.getController()).getCards();
                    
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
            ability.setDescription("GW, Sacrifice Loxodon Hierarch: Regenerate each creature you control.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" regenerate each of ").append(card.getController()).append("'s creatures.");
            ability.setStackDescription(sb.toString());
            
            ability.setBeforePayMana(new Input_PayManaCost_Ability(ability.getManaCost(), new Command() {
                private static final long serialVersionUID = -8594393519904006680L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                    AllZone.Stack.add(ability);
                }
            }));
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Springjack Shepherd")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int number = CardFactoryUtil.getNumberOfManaSymbolsControlledByColor("W", card.getController());
                    for(int i = 0; i < number; i++)
                        CardFactoryUtil.makeToken("Goat", "W 0 1 Goat", card, "W", new String[] {
                                "Creature", "Goat"}, 0, 1, new String[] {""});
                    
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -5515684113290670830L;
                
                public void execute() {
                    ability.setStackDescription("Springjack Shepherd - put a 0/1 white Goat creature token into play for each white mana symbol of permanents you control");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Gilder Bairn")) {
            final SpellAbility a1 = new Ability(card, "2 GU") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(c.sumAllCounters() == 0) return;
                    else if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        //zerker clean up:
                        for(Counters c_1:Counters.values())
                            if(c.getCounters(c_1) != 0) c.addCounter(c_1, c.getCounters(c_1));
                    }
                }
                
                @Override
                public boolean canPlay() {
                    
                    if(card.isTapped() && !card.hasSickness() && super.canPlay()) return true;
                    else return false;
                }
                
                @Override
                public void chooseTargetAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList perms = new CardList(play.getCards());
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList perms = new CardList(play.getCards());
                    perms = perms.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.sumAllCounters() > 0;
                        }
                    });
                    return perms.size() > 0;
                }
            };//SpellAbility
            a1.makeUntapAbility();
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 1571239319226728848L;
                
                @Override
                public void showMessage() {
                    PlayerZone human = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList perms = new CardList();
                    perms.addAll(human.getCards());
                    perms.addAll(comp.getCards());
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a1, perms, "Select target permanent.", true,
                            false));
                }
            };
            
            card.addSpellAbility(a1);
            a1.setDescription("2 GU, Untap: For each counter on target permanent, put another of those counters on that permanent.");
            
            a1.setBeforePayMana(runtime);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Order of Whiteclay")) {
            final SpellAbility a1 = new Ability(card, "1 W W") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
            a1.setDescription("1 W W, Untap:  Return target creature card with converted mana cost 3 or less from your graveyard to play.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - return target creature card with converted mana cost 3 or less from your graveyard to play.");
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
                    PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList creatures = new CardList();
                    
                    creatures.addAll(hPlay.getCards());
                    creatures.addAll(cPlay.getCards());
                    creatures = creatures.getType("Creature");
                    
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
                    PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList creatures = new CardList();
                    
                    creatures.addAll(hPlay.getCards());
                    creatures.addAll(cPlay.getCards());
                    creatures = creatures.getType("Creature");
                    
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
                    //AllZone.GameAction.getPlayerLife(card.getController()).loseLife(4,card);
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
                	
      				CardList all = new CardList();
                      all.addAll(AllZone.Human_Play.getCards());
                      all.addAll(AllZone.Computer_Play.getCards());
                      all = all.filter(new CardListFilter()
                      {
                      	public boolean addCard(Card c)
                      	{
                      		return c.isCreature() && !c.getKeyword().contains("Flying") &&
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

      				CardList all = new CardList();
                      all.addAll(AllZone.Human_Play.getCards());
                      all.addAll(AllZone.Computer_Play.getCards());
                      all = all.filter(new CardListFilter()
                      {
                      	public boolean addCard(Card c)
                      	{
                      		return c.isCreature() && c.getKeyword().contains("Flying") &&
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
                    PlayerZone zone = AllZone.getZone(Constant.Zone.Play, card.getController());
                    CardList list = new CardList(zone.getCards());
                    list = list.getType("Creature");
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
            /*
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    for(int i = 0; i < 3; i++)
                        CardFactoryUtil.makeToken("Serf", "B 0 1 Serf", card, "B", new String[] {
                                "Creature", "Serf"}, 0, 1, new String[] {""});
                }
            };//SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -2966662310531173458L;
                
                public void execute() {
                    ability.setStackDescription(card.getName() + " - " + card.getController()
                            + " puts three 0/1 tokens into play");
                    AllZone.Stack.add(ability);
                }
            };*/
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = 7242867764317580066L;
                
                public void execute() {
                    CardList all = AllZoneUtil.getTypeInPlay("Serf");
                    for(Card serf:all)
                        AllZone.GameAction.exile(serf);
                }//execute
            };//Command
           // card.addComesIntoPlayCommand(intoPlay);
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
                    

                    //remove this card from the graveyard and from the game
                    
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
                    sb.append(card.getName()).append(" - ").append(opponent).append(" puts a 3/3 flying token into play");
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
                    //PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
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
					PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                	sb.append("Symbiotic Elf - ").append(card.getController()).append(" puts two 1/1 tokens into play");
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
                    Player opponent = card.getController().getOpponent();
                    
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, opponent);
                    
                    if(hand.size() == 0) return;
                    
                    //randomly move card from hand to top of library
                    int index = MyRandom.random.nextInt(hand.size());
                    Card card = hand.get(index);
                    
                    hand.remove(card);
                    library.add(card, 0);
                }//resolve()
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 160195797163952303L;
                
                public void execute() {
                    ability.setStackDescription("Chittering Rats - Opponent randomly puts a card from his hand on top of his library.");
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
                    //if(AllZone.HumanPlayer.equals(opponent)) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
                    //else //computer
                    //AllZone.GameAction.discardRandom(opponent, this);
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
	                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());                                      
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
                    AllZone.Display.showMessage("Discard from your hand a non-creature card");
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
                	sb.append(card.getController()).append(" sacrifices Drekavac unless he discards a non-creature card");
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
        else if(cardName.equals("Minotaur Explorer")) {
            final SpellAbility ability = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    if(hand.getCards().length == 0) AllZone.GameAction.sacrifice(card);
                    else card.getController().discardRandom(this);
                }
            };//SpellAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4986114285467649619L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" - discards at random or sacrifices Minotaur Explorer");
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
                    //AllZone.GameAction.addDamage(opponent, card, 4);
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    
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
            ability.setStackDescription("Child of Alara - Destroy all non-land permanents, they can't be regenerated");
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Ryusei, the Falling Star")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
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
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                    
                    PlayerZone from = AllZone.getZone(card);
                    from.remove(card);
                    
                    card.setController(card.getOwner().getOpponent());
                    
                    PlayerZone to = AllZone.getZone(Constant.Zone.Play,
                            card.getOwner().getOpponent());
                    to.add(card);
                    Log.debug("Sleeper Agent", "cards controller = " + card.getController());
                    
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                }
            };
            
            ability.setStackDescription("When Sleeper Agent comes into play, target opponent gains control of it.");
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
                            } else if(card.isCreature() && zone.is(Constant.Zone.Play)) {
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
                            if(card.isArtifact() && zone.is(Constant.Zone.Play) && card.getController().equals(AllZone.HumanPlayer)) {
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
                        list.addAll(AllZone.Computer_Play.getCards());
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
                        list.addAll(AllZone.Computer_Play.getCards());
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
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Eternal Witness")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 1658050744890095441L;
                
                public void execute() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    
                    if(grave.getCards().length == 0) return;
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", grave.getCards());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else//computer
                    {
                        CardList list = new CardList(grave.getCards());
                        Card best = CardFactoryUtil.AI_getBestCreature(list);
                        
                        if(best == null) {
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
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sun Titan")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Karmic Guide")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                        grave.remove(getTargetCard());
                        play.add(getTargetCard());
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 2128307240208621147L;
                
                public void execute() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    list = list.getType("Creature");
                    
                    if(list.isEmpty()) return;
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", list.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    }//if
                    else//computer
                    {
                        list = list.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");
                        Card best = CardFactoryUtil.AI_getBestCreature(list);
                        ability.setTargetCard(best);
                        AllZone.Stack.add(ability);
                    }
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
            
            card.clearSpellAbility();
            
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 4446838001015234917L;
                
                @Override
                public boolean canPlayAI() {
                    CardList creats = new CardList();
                    creats.addAll(AllZone.Computer_Graveyard.getCards());
                    creats = creats.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.getNetAttack() > 2;
                        }
                    });
                    
                    if(creats.size() > 0) return true;
                    else return false;
                }
            });
        }//*************** END ************ END **************************
        */
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Gravedigger") || cardName.equals("Cadaver Imp") || cardName.equals("Mnemonic Wall") || cardName.equals("Corpse Cur")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7433708170033536384L;
                
                public void execute() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card crd) {
                            return ((card.getName().equals("Gravedigger") || card.getName().equals("Cadaver Imp")) && crd.isCreature())
                                        ||
                                    (card.getName().equals("Mnemonic Wall") && (crd.isInstant() || crd.isSorcery())
                                    	||
                                    (card.getName().equals("Corpse Cur") && crd.isCreature() && crd.KeywordsContain("Infect")));
                        }
                    });
                    // list = list.getType("Creature");
                    
                    if(list.isEmpty()) return;
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", list.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    }//if
                    else//computer
                    {
                        Card best = card;
                        if (card.getName().equals("Gravedigger") || card.getName().equals("Cadaver Imp") || card.getName().equals("Corpse Cur")) {
                            best = CardFactoryUtil.AI_getBestCreature(list);
                    	} else{
                            // compy will select a random Instant or Sorcery
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
        */
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Strongarm Thug")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 4519970074391756730L;
                
                public void execute() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    list = list.getType("Mercenary");
                    
                    if(list.isEmpty()) return;
                    
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", list.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
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
        */
        
        //*************** START *********** START ************************
        else if(cardName.equals("Adun Oakenshield")) {
            final Ability_Tap ability = new Ability_Tap(card, "B R G") {
                private static final long serialVersionUID = -7913968639880781838L;
                
                @Override
                public boolean canPlayAI() {
                    return getGraveCreatures().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList grave = getGraveCreatures();
                    Card target = CardFactoryUtil.AI_getBestCreature(grave);
                    setTargetCard(target);
                }
                
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Card c = AllZone.Display.getChoice("Select card", getGraveCreatures().toArray());
                        setTargetCard(c);
                    }
                    
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) AllZone.GameAction.moveTo(hand,
                            getTargetCard());
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    return super.canPlay() && getGraveCreatures().size() != 0 && super.canPlay();
                }
                
                CardList getGraveCreatures() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    list = list.getType("Creature");
                    return list;
                }
            };//SpellAbility
            ability.setDescription("B R G, Tap: Return target creature card from your graveyard to your hand.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - return target creature from your graveyard to your hand.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Anarchist")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7459412502903144952L;
                
                public void execute() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList sorcery = new CardList(grave.getCards());
                    sorcery = sorcery.getType("Sorcery");
                    
                    Player controller = card.getController();
                    
                    if(sorcery.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target card", sorcery.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
                    {
                        sorcery.shuffle();
                        ability.setTargetCard(sorcery.get(0));
                        AllZone.Stack.add(ability);
                    }
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        */

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
                	sb.append(card.getController()).append(" puts a 3/3 creature into play from Penumbra Kavu");
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
                	sb.append(card.getController()).append(" puts a 2/1 creature into play from Penumbra Bobcat");
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
                	sb.append(" puts a 2/4 Black Spider creature into play from Penumbra Spider");
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
                	sb.append(" puts a 6/6 Black Wurm creature with trample into play from Penumbra Wurm");
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
                    if(card.isLand() && zone.is(Constant.Zone.Play)) {
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
                        CardList list = new CardList(AllZone.Computer_Play.getCards());
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
                    if(card.isLand() && zone.is(Constant.Zone.Play)) {
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
                        CardList list = new CardList(AllZone.Computer_Play.getCards());
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
                    if(card.isLand() && zone.is(Constant.Zone.Play)) {
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
                        CardList list = new CardList(AllZone.Computer_Play.getCards());
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
                    if(card.isLand() && zone.is(Constant.Zone.Play)) {
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
                        CardList list = new CardList(AllZone.Computer_Play.getCards());
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
                    
                    //if(card.getController().equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
                    //else AllZone.GameAction.discardRandom(AllZone.ComputerPlayer, this);
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
                    PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play, opp);
                    PlayerZone myPlay = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    CardList list = new CardList(myPlay.getCards());
                    //list.remove(card);//doesn't move Sky Swallower
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.equals(card) && !c.getName().equals("Mana Pool");
                        }
                    });
                    
                    while(!list.isEmpty()) {
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        //so "comes into play" abilities don't trigger
                        ///list.get(0).addComesIntoPlayCommand(Command.Blank);
                        
                        oppPlay.add(list.get(0));
                        myPlay.remove(list.get(0));
                        
                        list.get(0).setController(opp);
                        list.remove(0);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
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
        else if(cardName.equals("Nova Chaser") || cardName.equals("Supreme Exemplar")) {
            final CommandReturn getCreature = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                    CardList list = new CardList();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    list.addAll(play.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getType().contains("Elemental") || c.getKeyword().contains("Changeling");
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
                        /*
                        PlayerZone play = AllZone.getZone(getTargetCard());
                        PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
                        play.remove(getTargetCard());
                        removed.add(getTargetCard());
                        */
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
                            "Select Elemental to remove from the game", false, false));
                    ButtonUtil.disableAll();
                }
                
            };
            Command commandComes = new Command() {
                private static final long serialVersionUID = -3498068247359658023L;
                
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
                        CardList computer = new CardList(AllZone.Computer_Play.getCards());
                        computer = computer.getType("Elemental");
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
                private static final long serialVersionUID = 4236503599017025393L;
                
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
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
                                removed.remove(c);
                                play.add(c);
                                
                            }
                        }//resolve()
                    };//SpellAbility
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - returning creature to play");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }//execute()
            };//Command
            
            card.addComesIntoPlayCommand(commandComes);
            card.addLeavesPlayCommand(commandLeavesPlay);
            
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
        else if(cardName.equals("Lightning Crafter")) {
            final CommandReturn getCreature = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                    CardList list = new CardList();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                        /*
                        PlayerZone play = AllZone.getZone(getTargetCard());
                        PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
                        play.remove(getTargetCard());
                        removed.add(getTargetCard());
                        */
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
                            "Select Goblin or Shaman to remove from the game", false, false));
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
                        CardList computer = new CardList(AllZone.Computer_Play.getCards());
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
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
                                removed.remove(c);
                                play.add(c);
                                
                            }
                        }//resolve()
                    };//SpellAbility
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - returning creature to play");
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
        else if(cardName.equals("Wren's Run Packmaster")) {
            final CommandReturn getCreature = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                    CardList list = new CardList();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    list.addAll(play.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getType().contains("Elf") || c.getKeyword().contains("Changeling");
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
                        /*
                        PlayerZone play = AllZone.getZone(getTargetCard());
                        PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
                        play.remove(getTargetCard());
                        removed.add(getTargetCard());
                        */
                        AllZone.GameAction.exile(getTargetCard());
                    }
                }//resolve()
            };
            
            final Input inputComes = new Input() {
                private static final long serialVersionUID = 5210700665533271691L;
                
                @Override
                public void showMessage() {
                    CardList choice = (CardList) getCreature.execute();
                    
                    stopSetNext(CardFactoryUtil.input_targetChampionSac(card, abilityComes, choice,
                            "Select Elf to remove from the game", false, false));
                    ButtonUtil.disableAll(); //target this card means: sacrifice this card
                }
            };
            Command commandComes = new Command() {
                
                private static final long serialVersionUID = -3580408066322945328L;
                
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
                        CardList computer = new CardList(AllZone.Computer_Play.getCards());
                        computer = computer.getType("Elf");
                        computer.remove(card);
                        
                        computer.shuffle();
                        if(computer.size() != 0) {
                            target = computer.get(0);
                            abilityComes.setTargetCard(target);
                            AllZone.Stack.add(abilityComes);
                        }
                        else
                        	AllZone.GameAction.sacrifice(card);
                    }//computer
                }//execute()
            };//CommandComes
            Command commandLeavesPlay = new Command() {
                
                private static final long serialVersionUID = -5903638227914705191L;
                
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
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
                                removed.remove(c);
                                play.add(c);
                                
                            }
                        }//resolve()
                    };//SpellAbility
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - returning creature to play");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }//execute()
            };//Command
            

            final SpellAbility a1 = new Ability(card, "2 G") {
                @Override
                public boolean canPlayAI() {
                    return MyRandom.random.nextBoolean();
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    //this is a hack, check the stack to see if this card has an ability on the stack
                    //if so, we can't use the ability
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    return AllZone.GameAction.isCardInPlay(card) && super.canPlay();
                    
                }
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", card.getController(), "G", new String[] {"Creature", "Wolf"},
                            2, 2, new String[] {""});
                }
            };//SpellAbility
            
            a1.setDescription("2 G: Put a 2/2 green Wolf creature token into play.");
            a1.setStackDescription("Put a 2/2 Wolf into play.");
            
            card.clearSpellAbility();
            
            card.addComesIntoPlayCommand(commandComes);
            card.addLeavesPlayCommand(commandLeavesPlay);
            
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 2583297503017070549L;
                
                @Override
                public boolean canPlayAI() {
                    Object o = getCreature.execute();
                    if(o == null) return false;
                    
                    CardList cl = (CardList) getCreature.execute();
                    return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
            
            card.addSpellAbility(a1);
            a1.setBeforePayMana(new Input_PayManaCost(a1));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Changeling Berserker") || cardName.equals("Changeling Hero")
                || cardName.equals("Changeling Titan")) {
            final CommandReturn getCreature = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                    CardList list = new CardList();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    list.addAll(play.getCards());
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getType().contains("Creature");
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
                        /*
                        PlayerZone play = AllZone.getZone(getTargetCard());
                        PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
                        play.remove(getTargetCard());
                        removed.add(getTargetCard());
                        */
                        AllZone.GameAction.exile(getTargetCard());
                    }
                }//resolve()
            };
            
            final Input inputComes = new Input() {
                
                private static final long serialVersionUID = 5210700665533271691L;
                
                @Override
                public void showMessage() {
                    CardList choice = (CardList) getCreature.execute();
                    
                    stopSetNext(CardFactoryUtil.input_targetChampionSac(card, abilityComes, choice,
                            "Select creature to remove from the game", false, false));
                    ButtonUtil.disableAll();
                }
            };
            Command commandComes = new Command() {
                
                private static final long serialVersionUID = -3580408066322945328L;
                
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
                        CardList computer = new CardList(AllZone.Computer_Play.getCards());
                        computer = computer.getType("Creature");
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
                
                private static final long serialVersionUID = -5903638227914705191L;
                
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
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
                                removed.remove(c);
                                play.add(c);
                                
                            }
                        }//resolve()
                    };//SpellAbility
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - returning creature to play");
                    ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }//execute()
            };//Command
            
            card.addComesIntoPlayCommand(commandComes);
            card.addLeavesPlayCommand(commandLeavesPlay);
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                
                private static final long serialVersionUID = 2583297503017070549L;
                
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
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && CardFactoryUtil.canTarget(card, c);
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
                        /*
                        PlayerZone play = AllZone.getZone(getTargetCard());
                        PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, getTargetCard().getController());
                        play.remove(getTargetCard());
                        removed.add(getTargetCard());
                        */
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
                            "Select target creature to remove from the game", true, false));
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
                            CardList computer = new CardList(AllZone.Computer_Play.getCards());
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
                    if((card.isCreature() || card.isPlaneswalker()) && zone.is(Constant.Zone.Play)) {
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
                    //AllZone.GameAction.addDamage(player, card, damage);
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
                    getTargetPlayer().discard(discard, this);
                    
                    //if(AllZone.HumanPlayer.equals(getTargetPlayer())) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(discard, this));
                    //else {
                     //   for(int i = 0; i < discard; i++)
                      //      AllZone.GameAction.discardRandom(AllZone.ComputerPlayer, this);
                    //}
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
                    sb.append(card).append(" - ").append(card.getController()).append(" puts tokens into play");
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
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                        PlayerZone oldPlay = AllZone.getZone(getTargetCard());
                        
                        //so "comes into play" abilities don't trigger
                        //getTargetCard().addComesIntoPlayCommand(Command.Blank);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        play.add(getTargetCard());
                        oldPlay.remove(getTargetCard());
                        
                        getTargetCard().setController(card.getController());
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
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
                    } else if(c.isCreature() && zone.is(Constant.Zone.Play)) {
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
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Elvish Piper")) {
            final SpellAbility ability = new Ability_Tap(card, "G") {
                private static final long serialVersionUID = 4414609319033894302L;
                
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
                    return list;
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(c, hand)) {
                        hand.remove(c);
                        play.add(c);
                    }
                }
            };
            
            ability.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -1647181037510967127L;
                
                @Override
                public void showMessage() {
                	Player controller = card.getController();
                    CardList creats = new CardList(AllZone.getZone(Constant.Zone.Hand, controller).getCards());
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            PlayerZone zone = AllZone.getZone(c);
                            return c.isCreature() && zone.is(Constant.Zone.Hand);
                        }
                        
                    });
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, creats, "Select a creature", false,
                            false));
                }
            });
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
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
                
                /*@Override
                public void chooseTargetAI() {
                    CardList targets = new CardList(AllZone.Computer_Hand.getCards());
                	CardListUtil.sortCMC(targets);
                    Card c = targets.get(0);
                    AllZone.GameAction.removeFromGame(c);
                	chosen.push(c);
                }*/
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
                		AllZone.Stack.add(ability);
                        stopSetNext(new ComputerAI_StackNotEmpty());
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
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
        else if(cardName.equals("Weathered Wayfarer")) {
            final SpellAbility ability = new Ability_Tap(card, "W") {
                private static final long serialVersionUID = 2902408879239353813L;
                
                @Override
                public void resolve() {
                    //getTargetCard() will NEVER be null
                    
                    //checks to see if card is still in the library
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), library)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
                }//resolve()
                
                @Override
                public boolean canPlay() {
                	Player oppPlayer = card.getController().getOpponent();
                    
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
                
                @Override
                public void chooseTargetAI() {
                    PlayerZone selfLibrary = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList library = new CardList(selfLibrary.getCards());
                    library = library.getType("Land");
                    
                    setTargetCard(library.get(0));
                }
            };//SpellAbility
            
            Input target = new Input() {
                private static final long serialVersionUID = 3492362297282622857L;
                
                @Override
                public void showMessage() {
                    CardList land = new CardList(AllZone.Human_Library.getCards());
                    land = land.getType("Land");
                    Object o = AllZone.Display.getChoiceOptional("Select a Land", land.toArray());
                    
                    //techincally not correct, but correct enough
                    //this allows players to look at their decks without paying anything
                    if(o == null) stop();
                    else {
                        AllZone.HumanPlayer.shuffle();
                        ability.setTargetCard((Card) o);
                        stopSetNext(new Input_PayManaCost(ability));
                    }
                }//showMessage()
            };//Input - target
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append("W, tap: Search your library for a land card, reveal it, and put it into your hand. ");
            sb.append("Then shuffle your library. Play this ability only if an opponent controls more lands than you.");
            ability.setDescription(sb.toString());
            
            ability.setBeforePayMana(target);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if (cardName.equals("Disciple of Kangee")) {
            final SpellAbility ability = new Ability_Tap(card, "U") {
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
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
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
            ability.setDescription("U, tap: Target creature gains flying and becomes blue until end of turn.");
            
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Puppeteer")) {
            //tap - target creature
            final SpellAbility ability = new Ability_Tap(card, "U") {
                private static final long serialVersionUID = 7698358771800336470L;
                
                @Override
                public boolean canPlayAI() {
                    return getTapped().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    Card target = CardFactoryUtil.AI_getBestCreature(getTapped());
                    setTargetCard(target);
                }
                
                CardList getTapped() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.isTapped();
                        }
                    });
                    return list;
                }//getTapped()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        Card c = getTargetCard();
                        if(c.isTapped()) c.untap();
                        else c.tap();
                    }
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("U, tap: Tap or untap target creature.");
            
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mirror Entity"))
        {
      	  final Ability ability = new Ability(card, "0")
      	  {
      		  public void resolve()
      		  {
        			final CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards()).getType("Creature");
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
      			  /**
      			CardList Clist = new CardList(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards()).getType("Creature"); 
      			CardList Hlist = new CardList(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards()).getType("Creature");
      			return((Clist.size() - Hlist.size() * ComputerUtil.getAvailableMana().size() > AllZone.GameAction.getPlayerLife(AllZone.HumanPlayer).getLife())
      					&& AllZone.Phase.getPhase().equals(Constant.Phase.Main1));
      					**/
      					
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
                    CardList untapped = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards()).getType("Creature");
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
                    CardList targets = new CardList(AllZone.getZone(Constant.Zone.Play, player).getCards()); 
                    targets = targets.getType("Creature");
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, targets,
                            "Select a creature you control", true, false));
                }
            });
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Hermit Druid")) {
            final SpellAbility ability = new Ability_Tap(card, "G") {
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
            ability.setStackDescription("G, tap: Reveal cards until you reveal a basic land. Put that in your hand, and put the rest in your graveyard");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sorceress Queen") || cardName.equals("Serendib Sorcerer")) {
            final Ability_Tap ability = new Ability_Tap(card) {
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
            sb.append("tap: Target creature other than ").append(cardName).append(" becomes 0/2 until end of turn.");
            ability.setDescription(sb.toString());
            
            //this ability can target "this card" when it shouldn't be able to
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature_NoCost_TapAbility_NoTargetSelf(ability));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Immaculate Magistrate")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 8976980151320100343L;
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        PlayerZone zone = AllZone.getZone(Constant.Zone.Play, card.getController());
                        CardList list = new CardList(zone.getCards());
                        int nElf = list.getType("Elf").size();
                        
                        Card c = getTargetCard();
                        c.addCounter(Counters.P1P1, nElf);
                        
                    }//is card in play?
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && !c.equals(card) && c.isCreature();
                        }
                    });
                    
                    if(list.isEmpty()) return false;
                    
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
        else if(cardName.equals("Steel Overseer")) {
            final Ability_Tap ability = new Ability_Tap(card) {

				private static final long serialVersionUID = 1822894871718751099L;

				@Override
                public void resolve() {
                	Player player = card.getController();
                    CardList arts;
                    if(player.equals(AllZone.HumanPlayer)) {
                        arts = new CardList(AllZone.Human_Play.getCards());
                    } else {
                        arts = new CardList(AllZone.Computer_Play.getCards());
                    }
                    
                    arts = arts.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.isCreature() && c.isArtifact();
                    	}
                    });
                    
                    for(int i = 0; i < arts.size(); i++) {
                        Card card = arts.get(i);
                        card.addCounter(Counters.P1P1, 1);
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.isArtifact();
                        }
                    });
                    
                    return list.size() > 1;
                }//canPlayAI()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("tap: Put a +1/+1 counter on each artifact creature you control.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - Put a +1/+1 counter on each artifact creature you control.");
            ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************
        
        /*

					String player = card2.getController();
                    CardList creatures;
                    if(player.equals(AllZone.HumanPlayer)) {
                        creatures = new CardList(AllZone.Human_Play.getCards());
                    } else {
                        creatures = new CardList(AllZone.Computer_Play.getCards());
                    }
                    
                    creatures = creatures.getType("Creature");
                    
                    for(int i = 0; i < creatures.size(); i++) {
                        Card card = creatures.get(i);
                        card.addCounter(Counters.P1P1, 1);
                        card.addExtrinsicKeyword("Vigilance");
                    }
                    
         */
                
        //*************** START *********** START **************************
        else if(cardName.equals("Giltspire Avenger")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = -1117719063688165635L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    Log.debug("Giltspire Avenger","phase =" + AllZone.Phase.getPhase());
                    if((AllZone.Phase.getPhase().equals(Constant.Phase.Main2) || AllZone.Phase.getPhase().equals(
                            Constant.Phase.End_Of_Turn))
                            && !card.hasSickness() && card.isUntapped() && super.canPlay()) return true;
                    else return false;
                    
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c)
                            && (c.getDealtCombatDmgToOppThisTurn() || c.getDealtDmgToOppThisTurn())
                            && CardFactoryUtil.canTarget(card, c)) {
                        AllZone.GameAction.destroy(c);
                    }
                }//resolve()
            };//SpellAbility
            
            Input target = new Input() {
                private static final long serialVersionUID = -4946540988877576202L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target creature to destroy");
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
                    } else if(c.isCreature() && zone.is(Constant.Zone.Play)
                            && (c.getDealtCombatDmgToOppThisTurn() || c.getDealtDmgToOppThisTurn())) {
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
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        PlayerZone from0 = AllZone.getZone(crd0);
                        from0.remove(crd0);
                        PlayerZone from1 = AllZone.getZone(crd1);
                        from1.remove(crd1);
                        
                        crd0.setController(card.getController().getOpponent());
                        crd1.setController(card.getController());
                        
                        PlayerZone to0 = AllZone.getZone(Constant.Zone.Play,
                                card.getController().getOpponent());
                        to0.add(crd0);
                        PlayerZone to1 = AllZone.getZone(Constant.Zone.Play, card.getController());
                        to1.add(crd1);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
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
                    

                    if(c.isLand() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, c)) {
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
			        PlayerZone PlayerPlayZone = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
			        CardList PlayerCreatureList = new CardList(PlayerPlayZone.getCards());
			        PlayerCreatureList = PlayerCreatureList.getType("Creature");
					PlayerZone opponentPlayZone = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
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
    			        PlayerZone PlayerPlayZone = AllZone.getZone(Constant.Zone.Play, card.getController());
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
            final Ability_Tap ability = new Ability_Tap(card) {
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
                	//String color = CardUtil.getColor(c);
                    //if(color.equals(Constant.Color.Colorless)) {
                	if(CardUtil.getColors(c).contains(Constant.Color.Colorless)) {
                        CardList list = new CardList();
                        list.add(c);
                        return list;
                    }
                    
                    CardList sameColor = new CardList();
                    
                    //get all creatures
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    
                    for(int i = 0; i < list.size(); i++)
                        if(list.get(i).sharesColorWith(c)) sameColor.add(list.get(i));
                    
                    return sameColor;
                }
                
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("Radiance - tap: Wojek Embermage deals 1 damage to target creature and each other creature that shares a color with it.");
            
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
        }//*************** END ************ END **************************
        

        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Mad Auntie")) {
            final Card[] creature = new Card[1];
            final Command EOT = new Command() {
                private static final long serialVersionUID = -5143708900761432510L;
                
                public void execute() {
                    if(AllZone.GameAction.isCardInPlay(creature[0])) {
                        creature[0].setShield(0);
                    }
                }
            };
            
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = -1280855188535819509L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        getTargetCard().addShield();
                        AllZone.EndOfTurn.addUntil(EOT);
                    }
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("tap: Regenerate another target Goblin.");
            
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature_NoCost_TapAbility_NoTargetSelf(ability));
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Adarkar Valkyrie")) {
            //tap ability - no cost - target creature - EOT
            
            final Card[] target = new Card[1];
            
            final Command destroy = new Command() {
                private static final long serialVersionUID = -2433442359225521472L;
                
                public void execute() {
                    AllZone.Stack.add(new Ability(card, "0", "Return " + target[0] + " from graveyard to play") {
                        @Override
                        public void resolve() {
                            PlayerZone grave = AllZone.getZone(target[0]);
                            //checks to see if card is still in the graveyard
                            if(AllZone.GameAction.isCardInZone(target[0], grave)) {
                                PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                    //target[0].addDestroy(Command.Blank);
                    target[0].removeDestroyCommand(destroy);
                    
                }
            };
            
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = -8454685126878522607L;
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
                        target[0] = getTargetCard();
                        AllZone.EndOfTurn.addUntil(untilEOT);
                        
                        //when destroyed, return to play
                        //add triggered ability to target card
                        target[0].addDestroyCommand(destroy);
                    }//if
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility
            
            Input targetInput = new Input() {
                private static final long serialVersionUID = 913860087744941946L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target non-token creature other than this card");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    //must target non-token creature, and cannot target itself
                    if(c.isCreature() && (!c.isToken()) && (!c.equals(card))) {
                        ability.setTargetCard(c);
                        stopSetNext(new Input_NoCost_TapAbility(ability));
                    }
                }
            };
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append("tap: When target creature other than Adarkar Valkyrie is put into a ");
            sb.append("graveyard this turn, return that card to play under your control.");
            ability.setDescription(sb.toString());
            
            ability.setBeforePayMana(targetInput);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mayael the Anima")) {
            final Ability_Tap ability = new Ability_Tap(card, "3 R G W") {
                
                private static final long serialVersionUID = -9076784333448226913L;
                
				@Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                            	Compplay.addAll(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
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
            sbDesc.append("3 R G W, Tap: Look at the top five cards of your library. ");
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
        else if(cardName.equals("Vedalken Mastermind")) {
            //mana tap ability
            final Ability_Tap ability = new Ability_Tap(card, "U") {
                private static final long serialVersionUID = -6131368241135911606L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
                    AllZone.GameAction.moveTo(hand, getTargetCard());
                    
                    if(getTargetCard().isToken()) hand.remove(getTargetCard());
                }//resolve()
            };//SpellAbility
            

            Input runtime = new Input() {
                private static final long serialVersionUID = -5218098811060156481L;
                
                @Override
                public void showMessage() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    CardList choice = new CardList(play.getCards());
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choice,
                            "Select a permanent you control.", true, false));
                }
            };
            card.addSpellAbility(ability);
            ability.setDescription("U, tap: Return target permanent you control to its owner's hand.");
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Helldozer")) {
            //mana tap ability
            final Ability_Tap ability = new Ability_Tap(card, "B B B") {
                private static final long serialVersionUID = 6426884086364885861L;
                
                @Override
                public boolean canPlayAI() {
                    if(CardFactoryUtil.AI_doesCreatureAttack(card)) return false;
                    
                    CardList land = new CardList(AllZone.Human_Play.getCards());
                    land = land.getType("Land");
                    return land.size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    
                    //target basic land that Human only has 1 or 2 in play
                    CardList land = new CardList(AllZone.Human_Play.getCards());
                    land = land.getType("Land");
                    
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
                        if(!getTargetCard().getType().contains("Basic")) card.untap();
                    }
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription("BBB, tap: Destroy target land. If that land is nonbasic, untap Helldozer.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetType(ability, "Land"));
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Cao Cao, Lord of Wei")) {
            //mana tap ability
            final Ability_Tap ability = new Ability_Tap(card, "0") {
                private static final long serialVersionUID = 6760838700101179614L;
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                }
                
                @Override
                public boolean canPlayAI() {
                    int hand = AllZone.Human_Hand.getCards().length;
                    if((!CardFactoryUtil.AI_doesCreatureAttack(card)) && hand != 0) return true;
                    
                    return 2 <= hand;
                }
                
                @Override
                public void resolve() {
                    Player player = card.getController().getOpponent();
                    player.discard(2, this);
                    
                    //if(player.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(2, this));
                    //else {
                     //   AllZone.GameAction.discardRandom(player, this);
                      //  AllZone.GameAction.discardRandom(player, this);
                    //}
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    Player opp = card.getController().getOpponent();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(opp).append(" discards 2 cards");
                    setStackDescription(sb.toString());
                    
                    String phase = AllZone.Phase.getPhase();
                    Player activePlayer = AllZone.Phase.getPlayerTurn();
                    
                    return super.canPlay() && phase.equals(Constant.Phase.Main1)
                            && card.getController().equals(activePlayer);
                }
            };//SpellAbility
            
            ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            ability.setDescription("tap: Target opponent discards two cards. Play this ability only during your turn, before the combat phase.");
            ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        */
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Gwendlyn Di Corci")) {
            //mana tap ability
            final Ability_Tap ability = new Ability_Tap(card, "0") {
                
                private static final long serialVersionUID = -4211234606012596777L;
                
                @Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.HumanPlayer);
                }
                
                @Override
                public boolean canPlayAI() {
                    int hand = AllZone.Human_Hand.getCards().length;
                    return hand > 0;
                }
                
                @Override
                public void resolve() {
                    Player player = getTargetPlayer();
                    
                    player.discardRandom(this);
                    
                }//resolve()
                
                @Override
                public boolean canPlay() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - ").append(getTargetPlayer()).append(" discards a card at random.");
                	setStackDescription(sb.toString());
                    
                    Player activePlayer = AllZone.Phase.getPlayerTurn();
                    
                    return super.canPlay() && card.getController().equals(activePlayer);
                }
            };//SpellAbility
            
            Input input = new Input() {
                private static final long serialVersionUID = 3312693459353844120L;
                
                @Override
                public void showMessage() {
                    //prevents this from running multiple times, which it is for some reason
                    if(ability.getSourceCard().isUntapped()) {
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
        */
               

        //*************** START *********** START **************************
        else if(cardName.equals("Wayward Soul")) {
            //mana ability
            final Ability ability = new Ability(card, "U") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
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
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 1469011418219527227L;
                
                @Override
                public void showMessage() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append("Put ").append(card).append(" on top of its owner's library");
                	ability.setStackDescription(sb.toString());
                    
                    stopSetNext(new Input_PayManaCost(ability));
                }
            };
            ability.setDescription("U: Put Wayward Soul on top of its owner's library.");
            ability.setStackDescription("Put Wayward Soul on top of its owner's library.");
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
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
            ability.setDescription("Sacrifice a land: Spitting Spider deals 1 damage to each creature with flying.");
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Killer Whale")) {
            final Ability ability = new Ability(card, "U") {
                
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -8494294720368074013L;
                        
                        public void execute() {
                            card.removeIntrinsicKeyword("Flying");
                        }
                    };
                    
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addIntrinsicKeyword("Flying");
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gains flying until end of turn.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("U: Killer Whale gains flying until end of turn.");
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
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
        else if(cardName.equals("Glint-Eye Nephilim")) {
        	//TODO: update to Ability_Cost
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 5790680475821014099L;
                
                public void execute() {
                    card.addTempAttackBoost(-1);
                    card.addTempDefenseBoost(-1);
                }
            };
            
            final Ability ability = new Ability(card, "1") {
                @Override
                public boolean canPlayAI() {
                    Card[] hand = AllZone.Computer_Hand.getCards();
                    return CardFactoryUtil.AI_doesCreatureAttack(card) && (hand.length != 0);
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
            };//SpellAbility

            Input runtime = new Input() {
                private static final long serialVersionUID = -4302110760957471033L;
                
                @Override
                public void showMessage() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card).append(" gets +1/+1 until EOT.");
                	ability.setStackDescription(sb.toString());
                    
                    //stopSetNext(CardFactoryUtil.input_sacrifice(ability, choice, "Select a card to discard."));
                    stopSetNext(CardFactoryUtil.input_discard(ability, 1));
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets +1/+1 until end of turn.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("1, Discard a card: Glint-Eye Nephilim gets +1/+1 until end of turn.");
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Amugaba")) {
            
            final Ability ability = new Ability(card, "2 U") {
                @Override
                public boolean canPlayAI() {
                   return false;
                }

                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    card.untap();
                    AllZone.getZone(card).remove(card);
                    if(!card.isToken()) hand.add(card);
                }//resolve()
            };//SpellAbility

            Input runtime = new Input() {
                private static final long serialVersionUID = -4302210760957471033L;
                
                @Override
                public void showMessage() {
                    stopSetNext(CardFactoryUtil.input_discard(ability, 1));
                    
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets returned to owner's hand");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("2U, Discard a card: Return Amugaba to owner's hand.");
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Subterranean Spirit")) {
            
            final SpellAbility ability = new Ability_Tap(card, "0") {
            	
                private static final long serialVersionUID = 7698358771810336470L;

                @Override
                public void resolve() {
                	
      				CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = all.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.isCreature() && !c.getKeyword().contains("Flying") &&
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
            
            ability.setDescription("T: Subterranean Spirit deals 1 damage to each creature without flying.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Ashen Firebeast")) {
            
            final Ability ability = new Ability(card, "1 R") {
            	
                @Override
                public void resolve() {
                	
      				CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = all.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.isCreature() && !c.getKeyword().contains("Flying") &&
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
            
            ability.setDescription("1 R: Ashen Firebeast deals 1 damage to each creature without flying.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Scourge of Kher Ridges")) {
            
            final Ability ability = new Ability(card, "1 R") {
            	
                @Override
                public void resolve() {
                	
      				CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = all.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.isCreature() && !c.getKeyword().contains("Flying") &&
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
                	
      				CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = all.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.isCreature() && c.getKeyword().contains("Flying") &&
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
        else if(cardName.equals("Ephemeron")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                   return false;
                }

                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    card.untap();
                    AllZone.getZone(card).remove(card);
                    if(!card.isToken()) hand.add(card);
                }//resolve()
            };//SpellAbility
            

            Input runtime = new Input() {
                private static final long serialVersionUID = -4312210760957471033L;
                
                @Override
                public void showMessage() {
                    stopSetNext(CardFactoryUtil.input_discard(ability, 1));
                    
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets returned to owner's hand");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("Discard a card: Return Ephemeron to owner's hand.");
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
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
            
            //mana tap ability
            final Ability ability = new Ability(card, "0") {
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
                            list.addAll(AllZone.Computer_Play.getCards());
                            
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
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -4209163355325441624L;
                
                @Override
                public void showMessage() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card).append(" gets +1/+1 until EOT.");
                	ability.setStackDescription(sb.toString());
                    
                    stopSetNext(CardFactoryUtil.input_discard(ability, 1));
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets +1/+1 and becomes the color of your choiceuntil end of turn.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("Discard a card: Wild Mongrel gets +1/+1 and becomes the color of your choice until end of turn.");
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Spiritmonger")) {
        	
        	final String[] color = new String[1];
        	final long[] timeStamp = new long[1];
            
            //color change ability
            final Ability ability = new Ability(card, "G") {
            	
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
            
            ability.setDescription("G: Spiritmonger becomes the color of your choice until end of turn.");
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
        else if(cardName.equals("Hell-Bent Raider")) {
            final Command untilEOT = new Command() {
                
                private static final long serialVersionUID = -2693050198371979012L;
                
                public void execute() {
                    card.removeIntrinsicKeyword("Protection from white");
                }
            };
            
            //mana tap ability
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    card.getController().discardRandom(this);
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addIntrinsicKeyword("Protection from white");
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" gets Protection from white until end of turn.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("Discard a card at random: Hell-Bent Raider gets protection from white until end of turn.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Whiptongue Frog")) {
            //mana ability - targets itself - until EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -2693050198371979012L;
                
                public void execute() {
                    card.removeIntrinsicKeyword("Flying");
                }
            };
            
            //mana tap ability
            final Ability ability = new Ability(card, "U") {
                @Override
                public boolean canPlayAI() {
                    return (!card.hasSickness()) && (!card.getKeyword().contains("Flying"))
                            && (AllZone.Phase.getPhase().equals(Constant.Phase.Main1));
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addIntrinsicKeyword("Flying");
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 1268037036474796569L;
                
                @Override
                public void showMessage() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card).append(" gains flying until EOT.");
                	ability.setStackDescription(sb.toString());
                    
                    stopSetNext(new Input_PayManaCost(ability));
                }
            };
            ability.setStackDescription("Whiptongue Frog gains flying until EOT.");
            ability.setDescription("U: Whiptongue Frog gains flying until end of turn.");
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sarcomite Myr")) {
            //mana ability - targets itself - until EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -1726670429352834671L;
                
                public void execute() {
                    card.removeIntrinsicKeyword("Flying");
                }
            };
            
            //mana tap ability
            final Ability ability = new Ability(card, "2") {
                @Override
                public boolean canPlayAI() {
                    return (!card.hasSickness()) && (!card.getKeyword().contains("Flying"))
                            && (AllZone.Phase.getPhase().equals(Constant.Phase.Main1));
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
                        card.addIntrinsicKeyword("Flying");
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -685958984421033465L;
                
                @Override
                public void showMessage() {
                	StringBuilder sb = new StringBuilder();
                	sb.append(card).append(" gains flying until EOT.");
                	ability.setStackDescription(sb.toString());
                    
                    stopSetNext(new Input_PayManaCost(ability));
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - gains flying until EOT.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("2: Sarcomite Myr gains flying until end of turn.");
            card.addSpellAbility(ability);
            ability.setBeforePayMana(runtime);
            
            //ability 2
            final Ability ability2 = new Ability(card, "2") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                	card.getController().drawCard();
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability2);
            ability2.setDescription("2, Sacrifice Sarcomite Myr: Draw a card.");
            ability2.setStackDescription("Sarcomite Myr - draw a card");
            ability2.setBeforePayMana(new Input_PayManaCost_Ability(ability2.getManaCost(), new Command() {
                private static final long serialVersionUID = -4357239016463815380L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                    AllZone.Stack.add(ability2);
                }
            }));
            
        }//*************** END ************ END **************************
                        

        //*************** START *********** START **************************
        else if(cardName.equals("Turtleshell Changeling")) {
            //mana ability - targets itself - until EOT
            //mana ability
            final Ability ability = new Ability(card, "1 U") {
                @Override
                public boolean canPlayAI() {
                    return CardFactoryUtil.AI_doesCreatureAttack(card) && card.getNetAttack() == 1;
                }
                
                @Override
                public void resolve() {
                    //in case ability is played twice
                    final int[] oldAttack = new int[1];
                    final int[] oldDefense = new int[1];
                    
                    oldAttack[0] = card.getBaseAttack();
                    oldDefense[0] = card.getBaseDefense();
                    
                    card.setBaseAttack(oldDefense[0]);
                    card.setBaseDefense(oldAttack[0]);
                    
                    //EOT
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -5494886974452901728L;
                        
                        public void execute() {
                            card.setBaseAttack(oldAttack[0]);
                            card.setBaseDefense(oldDefense[0]);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - switch power and toughness until EOT.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("1 U: Switch Turtleshell Changeling's power and toughness until end of turn.");
            card.addSpellAbility(ability);
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
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
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
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        c.setSickness(true);
                        c.setController(card.getController());
                        
                        PlayerZone from = AllZone.getZone(c);
                        from.remove(c);
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
                        to.add(c);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
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
                            CardList computer = new CardList(AllZone.Computer_Play.getCards());
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
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        c.setSickness(true);
                        c.setController(c.getController().getOpponent());
                        
                        PlayerZone from = AllZone.getZone(c);
                        from.remove(c);
                        
                        //make sure the creature is removed from combat:
                        CardList list = new CardList(AllZone.Combat.getAttackers());
                        if(list.contains(c)) AllZone.Combat.removeFromCombat(c);
                        
                        CardList pwlist = new CardList(AllZone.pwCombat.getAttackers());
                        if(pwlist.contains(c)) AllZone.pwCombat.removeFromCombat(c);
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                        to.add(c);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
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
        else if(cardName.equals("Frostling")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList list = getCreature();
                    list.shuffle();
                    setTargetCard(list.get(0));
                    
                    AllZone.GameAction.sacrifice(card);
                }
                
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
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(1, card);
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription("Sacrifice Frostling: Frostling deals 1 damage to target creature.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability, new Command() {
                private static final long serialVersionUID = 3482118508536148313L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                }
            }));
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
                        list.addAll(AllZone.Computer_Play.getCards());
                        
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

        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Mogg Fanatic")) {
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
                    } else AllZone.GameAction.getPlayerLife(getTargetPlayer()).loseLife(1,card);
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription("Sacrifice Mogg Fanatic: Mogg Fanatic deals 1 damage to target creature or player.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, new Command() {
                private static final long serialVersionUID = 8283052965865884779L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                }
            }, true, false));
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Bloodfire Colossus")) {
        	
        	Ability_Cost abCost = new Ability_Cost("R Sac<1/CARDNAME>", cardName, true);

        	final Ability_Activated ability = new Ability_Activated(card, abCost, null){
        		
                private static final long serialVersionUID = 8283052965865884889L;

                @Override
                public void resolve() {
        			int damage = 6;
        			CardList all = AllZoneUtil.getCreaturesInPlay();
        			for(Card c:all) {
        				c.addDamage(damage, card);
        			}
        			AllZone.ComputerPlayer.addDamage(damage, card);
        			AllZone.HumanPlayer.addDamage(damage, card);
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append(abCost.toString()).append("Bloodfire Colossus deals 6 damage to each creature and each player.");
            ability.setDescription(sb.toString());
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
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.getKeyword().contains("Flying")
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
        else if(cardName.equals("Sakura-Tribe Elder")) {
        	Ability_Cost abCost = new Ability_Cost("Sac<1/CARDNAME>", cardName, true);
        	
            //tap sacrifice
            final SpellAbility ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = 1135117614484689768L;
                
                @Override
                public boolean canPlayAI() {
                    //sacrifice Sakura-Tribe Elder if Human has any creatures
                    CardList creature = new CardList(AllZone.Human_Play.getCards());
                    creature = creature.getType("Creature");
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList basic = new CardList(library.getCards());
                    basic = basic.getType("Basic");
                    return creature.size() > 0 && basic.size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    //AllZone.GameAction.sacrifice(card);
                }
                
                @Override
                public boolean canPlay() {
                	return super.canPlay();
                }//canPlay()
                
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) humanResolve();
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
                    
                    AllZone.ComputerPlayer.shuffle();
                }//computerResolve()
                
                public void humanResolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    CardList basicLand = new CardList(library.getCards());
                    basicLand = basicLand.getType("Basic");
                    if(basicLand.isEmpty()) return;
                    
                    Object o = AllZone.Display.getChoiceOptional("Choose a basic land", basicLand.toArray());
                    if(o != null) {
                        Card land = (Card) o;
                        land.tap();
                        
                        library.remove(land);
                        play.add(land);
                    }
                    
                    card.getController().shuffle();
                }//resolve()
            };//SpellAbility

            card.addSpellAbility(ability);
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Sacrifice Sakura-Tribe Elder: Search your library for a basic ");
            sbDesc.append("land card, put that card into play tapped, then shuffle your library.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append("Search your library for a basic land card, put that ");
            sbStack.append("card into play tapped, then shuffle your library.");
            ability.setStackDescription(sbStack.toString());
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
            ability.setDescription("W, Sacrifice Transluminant: Put a 1/1 white Spirit creature token with flying into play at end of turn.");
            ability.setStackDescription("Put a 1/1 white Spirit creature token with flying into play at end of turn.");
            ability.setBeforePayMana(new Input_PayManaCost_Ability(ability.getManaCost(), new Command() {
                private static final long serialVersionUID = -6553009833190713980L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                    AllZone.Stack.add(ability);
                }
            }));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Llanowar Behemoth")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return (getUntapped().size() != 0) && CardFactoryUtil.AI_doesCreatureAttack(card);
                }
                
                @Override
                public void chooseTargetAI() {
                    Card c = getUntapped().get(0);
                    c.tap();
                    setTargetCard(c);
                }
                
                CardList getUntapped() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.isUntapped() && (!CardFactoryUtil.AI_doesCreatureAttack(c));
                        }
                    });
                    return list;
                }//getUntapped()
                
                @Override
                public void resolve() {
                    card.addTempAttackBoost(1);
                    card.addTempDefenseBoost(1);
                    
                    Command untilEOT = new Command() {
                        private static final long serialVersionUID = 6445782721494547172L;
                        
                        public void execute() {
                            card.addTempAttackBoost(-1);
                            card.addTempDefenseBoost(-1);
                        }//execute()
                    };//Command
                    
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }//resolve()
            };//SpellAbility
            
            Input target = new Input() {
                private static final long serialVersionUID = 7721637420366357272L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select an untapped creature you control");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(c.isCreature() && zone.is(Constant.Zone.Play, card.getController()) && c.isUntapped()) {
                    	
                    	StringBuilder sb = new StringBuilder();
                    	sb.append(card).append(" gets +1/+1 until end of turn.");
                    	ability.setStackDescription(sb.toString());
                        
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
        else if(cardName.equals("Memnarch")) {
            //has 2 non-tap abilities that effects itself
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
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (!c.isArtifact()) && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    return list;
                }//getCreature()
            };//SpellAbility
            /*
            //**** start of ability2
            final SpellAbility ability2 = new Ability(card, "3 U") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        //gain control of target artifact
                        PlayerZone from = AllZone.getZone(Constant.Zone.Play, getTargetCard().getController());
                        from.remove(getTargetCard());
                        

                        getTargetCard().setController(card.getController());
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
                        to.add(getTargetCard());
                        to.setUpdate(true);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                        

//	              AllZone.GameAction.moveTo(play, getTargetCard());
                        
                        //TODO: controller probably is not set correctly
                        //TODO: when you take control, the creature looses type "Artifact" since
                        //      GameAction.moveTo() makes a new card object
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = getArtifactCreatures();
                    return list.size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList list = getArtifactCreatures();
                    Card target = CardFactoryUtil.AI_getBestCreature(list);
                    if(target == null) target = AllZone.Human_Play.get(0);
                    
                    setTargetCard(target);
                }
                
                CardList getArtifactCreatures() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.isArtifact() && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    return list;
                }
            };//SpellAbility
            */
            card.addSpellAbility(ability1);
            //card.addSpellAbility(ability2);
            
            ability1.setDescription("1 U U: Target permanent becomes an artifact in addition to its other types. (This effect doesn't end at end of turn.)");
            //ability2.setDescription("3 U: Gain control of target artifact. (This effect doesn't end at end of turn.)");
            
            ability1.setBeforePayMana(CardFactoryUtil.input_targetType(ability1, "All"));
            //ability2.setBeforePayMana(CardFactoryUtil.input_targetType(ability2, "Artifact"));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Hammerfist Giant")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 1089840397064226840L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(4, card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.getKeyword().contains("Flying");
                        }
                    });
                    
                    return list.size() > 3 && 6 < AllZone.ComputerPlayer.getLife();
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    //get all creatures
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.getKeyword().contains("Flying") && CardFactoryUtil.canDamage(card, c);
                        }
                    });
                    for(int i = 0; i < list.size(); i++)
                        list.get(i).addDamage(4, card);
                    
                    AllZone.HumanPlayer.addDamage(4, card);
                    AllZone.ComputerPlayer.addDamage(4, card);
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("tap: Hammerfist Giant deals 4 damage to each creature without flying and each player.");
            ability.setStackDescription("Hammerfist Giant - deals 4 damage to each creature without flying and each player.");
            ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Urborg Syphon-Mage")) {
            final Ability_Tap ability = new Ability_Tap(card, "2 B") {
                private static final long serialVersionUID = -1965170715774280112L;
                
                @Override
                public void resolve() {
                	Player opponent = card.getController().getOpponent();
                    opponent.loseLife(2, card);
                    card.getController().gainLife(2, card);
                    
                    //computer discards here, TODO: should discard when ability put on stack
                    if(card.getController().equals(AllZone.ComputerPlayer)) AllZone.ComputerPlayer.discardRandom(this);
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    return super.canPlay() && hand.getCards().length != 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    int life = AllZone.HumanPlayer.getLife();
                    Card[] hand = AllZone.Computer_Hand.getCards();
                    return ((life < 11) || (5 < AllZone.Phase.getTurn())) && hand.length > 0;
                }
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription("2B, tap, Discard a card: Each other player loses 2 life. You gain life equal to the life lost this way.");
            ability.setStackDescription("Urborg Syphon-Mage - Opponent loses 2 life, and you gain 2 life");
            ability.setBeforePayMana(new Input_PayManaCost_Ability("2 B", new Command() {
                private static final long serialVersionUID = 1186455545951390853L;
                
                public void execute() {
                    card.tap();
                    AllZone.InputControl.setInput(CardFactoryUtil.input_discard(ability));
                    AllZone.Stack.add(ability);
                }
            }));
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
                    CardList list = new CardList();
                    list.addAll(AllZone.Computer_Play.getCards());
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getName("Stangg Twin");
                    
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
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
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
            sb.append("W, tap, Discard a card: Put a 1/1 white Kithkin Soldier creature token ");
            sb.append("named Goldmeadow Harrier into play with \"W, T: tap target creature.\"");
            ability.setDescription(sb.toString());
            
            ability.setStackDescription("Goldmeadow Lookout - Put a 1/1 token into play");
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        if(cardName.equals("Goldmeadow Harrier") || cardName.equals("Loxodon Mystic")
                || cardName.equals("Master Decoy") || cardName.equals("Benalish Trapper")
                || cardName.equals("Whipcorder") || cardName.equals("Blinding Mage")
                || cardName.equals("Ostiary Thrull") || cardName.equals("Squall Drifter")
                || cardName.equals("Stormscape Apprentice") || cardName.equals("Thornscape Apprentice")
                || cardName.equals("Naya Battlemage")) {
            final SpellAbility ability = new Ability_Tap(card, "W") {
                
                private static final long serialVersionUID = 4424848120984319655L;
                
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
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
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
                	
                    return (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer().equals(card.getController()) && 
                    		human.size() > 0 && (assassins.size() > 0 || !list.contains(card)));
                    
                }//canPlayAI
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("W, tap: Tap target creature.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
            
        }//*************** END ************ END **************************
        */
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Rathi Trapper")) {
            final SpellAbility ability = new Ability_Tap(card, "B") {
                
                private static final long serialVersionUID = 4424848120984319655L;
                
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
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
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
                	
                    return (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer().equals(card.getController()) && 
                    		human.size() > 0 && (assassins.size() > 0 || !list.contains(card)));
                    
                }//canPlayAI
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("B, tap: Tap target creature.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
            
        }//*************** END ************ END **************************
        */
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Minister of Impediments") || cardName.equals("Ballynock Trapper")) {
            final SpellAbility ability = new Ability_Tap(card, "0") {
                
                private static final long serialVersionUID = 4424848120984319655L;
                
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
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
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
                	
                    return (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer().equals(card.getController()) && 
                    		human.size() > 0 && (assassins.size() > 0 || !list.contains(card)));
                    
                }//canPlayAI
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("tap: Tap target creature.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
            
        }//*************** END ************ END **************************
        */
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Crowd Favorites")) {
            final SpellAbility ability = new Ability_Tap(card, "3 W") {
				private static final long serialVersionUID = -5819767122230717160L;

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
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
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
                	
                    return (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) && AllZone.Phase.getActivePlayer().equals(card.getController()) && 
                    		human.size() > 0 && (assassins.size() > 0 || !list.contains(card)));
                    
                }//canPlayAI
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("3 W, tap: Tap target creature.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
            
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if (cardName.equals("Stalking Assassin")) {
            
            final Ability_Tap destroy = new Ability_Tap(card, "3 B") {
                private static final long serialVersionUID = -6612039354743803366L;

                @Override
                public boolean canPlayAI() {
                    CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                    human = human.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isTapped() 
                                && CardFactoryUtil.canTarget(card, c) 
                                && !c.getKeyword().contains("Indestructible");
                        }
                    });
                    
                    if (human.size() > 0) {
                        CardListUtil.sortAttack(human);
                        CardListUtil.sortFlying(human);
                        setTargetCard(human.get(0));
                    }
                                        
                    return 0 < human.size();
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if (AllZone.GameAction.isCardInPlay(c) 
                            && c.isTapped() 
                            && CardFactoryUtil.canTarget(card, c)) {
                        AllZone.GameAction.destroy(c);
                    }
                }//resolve()
            };//SpellAbility
            
            Input target = new Input() {
                private static final long serialVersionUID = -8953453455402148585L;

                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target tapped creature to destroy");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (!CardFactoryUtil.canTarget(card, c)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if (c.isCreature() 
                            && zone.is(Constant.Zone.Play) 
                            && c.isTapped()) {
                        //tap ability
                        card.tap();
                        
                        destroy.setTargetCard(c);
                        AllZone.Stack.add(destroy);
                        stop();
                    }
                }//selectCard()
            };//Input
            /*
            final SpellAbility tap = new Ability_Tap(card, "3 U") {
                private static final long serialVersionUID = -8634280576775825017L;

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
                            return c.isUntapped() 
                                && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    if (human.size() > 0) {
                        CardListUtil.sortAttack(human);
                        CardListUtil.sortFlying(human);
                        setTargetCard(human.get(0));
                    }
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList assassins = new CardList();
                    assassins.addAll(play.getCards());
                    
                    assassins = assassins.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (!c.hasSickness() || c.getKeyword().contains("Haste")) && 
                                   c.isUntapped() && !c.equals(card) && 
                                  (c.getName().equals("Rathi Assassin") || c.getName().equals("Royal Assassin") || 
                                   c.getName().equals("Tetsuo Umezawa") || c.getName().equals("Stalking Assassin"));
                        }
                    });
                    
                    Combat attackers = ComputerUtil.getAttackers();
                    CardList list = new CardList(attackers.getAttackers());
                    
                    return (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) 
                            && AllZone.Phase.getActivePlayer().equals(card.getController()) 
                            && human.size() > 0 
                            && (assassins.size() > 0 || !list.contains(card)));
                    
                }//canPlayAI
            };//SpellAbility
            */
            
            //card.addSpellAbility(tap);
            //tap.setDescription("3 U, tap: Tap target creature.");
            //tap.setBeforePayMana(CardFactoryUtil.input_targetCreature(tap));
            card.addSpellAbility(destroy);
            destroy.setDescription("3 B, tap: Destroy target tapped creature.");
            destroy.setBeforePayMana(target);
            destroy.setBeforePayMana(CardFactoryUtil.input_targetCreature(destroy));
        }//*************** END ************ END **************************

        /* Keyworded
        //*************** START *********** START **************************
        else if(cardName.equals("Ramosian Revivalist")) {
            int a = Integer.parseInt("6");
            a--;
            final int converted = a;
            final Player player = card.getController();
            
            final SpellAbility ability = new Ability_Tap(card, "6") {
                private static final long serialVersionUID = 2675327938055139432L;
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    if(AllZone.GameAction.isCardInPlay(card) && !card.hasSickness() && !card.isTapped() && super.canPlay()) return true;
                    else return false;
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
                    
                    CardList list = new CardList(grave.getCards());
                    list = list.getType("Rebel");
                    
                    if(AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && list.size() > 0) return true;
                    else return false;
                    
                }
                
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList rebels = new CardList();
                    CardList list = new CardList(grave.getCards());
                    list = list.getType("Rebel");
                    
                    if(list.size() > 0) {
                        for(int i = 0; i < list.size(); i++) {
                            if(CardUtil.getConvertedManaCost(list.get(i).getManaCost()) <= converted) {
                                rebels.add(list.get(i));
                            }
                            
                        }
                        
                        if(rebels.size() > 0) {
                            if(player.equals(AllZone.ComputerPlayer)) {
                                Card rebel = CardFactoryUtil.AI_getBestCreature(rebels);
                                grave.remove(rebel);
                                play.add(rebel);
                            } else //human
                            {
                                Object o = AllZone.Display.getChoiceOptional("Select target Rebel",
                                        rebels.toArray());
                                Card rebel = (Card) o;
                                grave.remove(rebel);
                                play.add(rebel);
                            }
                        }//rebels.size() >0
                    }//list.size() > 0
                }//resolve
            };
            ability.setDescription("6: Return target Rebel permanent card with converted mana cost 5 or less from your graveyard to play.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - return Rebel from graveyard to play.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Marrow-Gnawer")) {
            final Player player = card.getController();
            
            final SpellAbility ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 447190529377334168L;
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    CardList cards = new CardList(play.getCards());
                    
                    Card c = getTargetCard();
                    if(c != null) {
                        AllZone.GameAction.sacrifice(c);
                        
                        CardList rats = new CardList();
                        
                        for(int i = 0; i < cards.size(); i++) {
                            if(cards.get(i).getType().contains("Rat")
                                    || cards.get(i).getKeyword().contains("Changeling")) {
                                Card k = cards.get(i);
                                rats.add(k);
                            }
                        }
                        
                        if(!c.getName().equals("Marrow-Gnawer")) {
                            for(int j = 0; j < rats.size() - 1; j++) {
                                makeToken();
                            }
                        } else //some weird thing when targeting marrow himself, number of rats is different... so here's a hack:
                        {
                            for(int k = 0; k < rats.size(); k++) {
                                makeToken();
                            }
                        }
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Rat");
                    
                    for(int i = 0; i < list.size(); i++) {
                        String name = list.get(i).getName();
                        if(!name.equals("Marrow-Gnawer") && !name.equals("Relentless Rats")) setTargetCard(list.get(i));
                        if(name.equals("B 1 1 Rat")) {
                            setTargetCard(list.get(i));
                            break;
                        }
                        
                    }
                    
                    if(getTargetCard() == null) Log.debug("Marrow-Gnawer", "getTargetCard null");
                    if(getTargetCard() != null && list.size() > 3) return true;
                    else return false;
                }
                
                
                public void makeToken() {
                    CardFactoryUtil.makeToken("Rat", "B 1 1 Rat", card.getController(), "B", new String[] {"Creature", "Rat"}, 1,
                            1, new String[] {""});
                }
                
            };//ability
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 8552290582665041908L;
                
                @Override
                public void showMessage() {
                    CardList rats = new CardList(
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                    rats = rats.getType("Rat");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, rats, "Select a Rat to sacrifice.",
                            false, false));
                }
            };
            
            card.addSpellAbility(ability);
            ability.setDescription("Tap, Sacrifice a rat: Put X 1/1 black Rat creature tokens into play, where X is the number of Rats you control.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Put X 1/1 black Rat creature tokens into play, where X is the number of Rats you control.");
            ability.setStackDescription(sb.toString());
            
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Arcanis the Omnipotent")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 4743686230518855738L;
                
                @Override
                public boolean canPlayAI() {
                    return true;
                }
                
                @Override
                public void resolve() {
                	card.getController().drawCards(3);
                }
            };//SpellAbility
            
            final SpellAbility ability2 = new Ability(card, "2 U U") {
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    card.untap();
                    AllZone.getZone(card).remove(card);
                    if(!card.isToken()) hand.add(card);
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }; //ability2
            
            card.addSpellAbility(ability);
            ability.setDescription("tap: Draw three cards.");
            StringBuilder sb = new StringBuilder();
            sb.append("Arcanis - ").append(card.getController()).append(" draws three cards.");
            ability.setStackDescription(sb.toString());
            ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
            
            card.addSpellAbility(ability2);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getController()).append(" returns Arcanis back to owner's hand.");
            ability2.setStackDescription(sb2.toString());
            ability2.setDescription("2 U U: Return Arcanis the Omnipotent to its owner's hand.");
        }//*************** END ************ END ************************** 
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Blinking Spirit")) {
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    card.untap();
                    AllZone.getZone(card).remove(card);
                    if(!card.isToken()) hand.add(card);
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }; 
            
            card.addSpellAbility(ability);
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" returns Blinking Spirit back to owner's hand.");
            ability.setStackDescription(sb.toString());
            ability.setDescription("0: return Blinking Spirit to its owner's hand.");
        }//*************** END ************ END ************************** 
        
        //*************** START *********** START **************************
        else if(cardName.equals("Selenia, Dark Angel")) {
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    card.getController().loseLife(2, card);
                    
                    card.untap();
                    AllZone.getZone(card).remove(card);
                    if(!card.isToken()) hand.add(card);
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility
            
            card.addSpellAbility(ability);
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" returns Selenia, Dark Angel back to owner's hand.");
            ability.setStackDescription(sb.toString());
            ability.setDescription("Pay 2 life: return Selenia, Dark Angel to its owner's hand.");
        }//*************** END ************ END ************************** 
        */
        
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
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
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
            //ability.setBeforePayMana(new Input_);
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
                    //if(AllZone.ComputerPlayer.equals(getTargetPlayer())) AllZone.GameAction.discardRandom(getTargetPlayer(), this);
                    //else AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Boris Devilboon")) {
            final Ability_Tap tokenAbility1 = new Ability_Tap(card, "2 B R") {
                private static final long serialVersionUID = -6343382804503119405L;
                
                @Override
                public boolean canPlayAI() {
                    String phase = AllZone.Phase.getPhase();
                    return phase.equals(Constant.Phase.Main2);
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                }
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Minor Demon", "BR 1 1 Minor Demon", card, "B R", new String[] {
                            "Creature", "Demon"}, 1, 1, new String[] {""});
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(tokenAbility1);
            
            tokenAbility1.setDescription("2 B R, tap: Put a 1/1 black and red Demon creature token named Minor Demon onto the battlefield.");
            tokenAbility1.setStackDescription(card.getName()
                    + " - Put a 1/1 black and red Demon creature token named Minor Demon onto the battlefield.");
            tokenAbility1.setBeforePayMana(new Input_PayManaCost(tokenAbility1));
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Rhys the Redeemed")) {
            /*
        	final Ability_Tap tokenAbility1 = new Ability_Tap(card, "2 GW") {
                private static final long serialVersionUID = 411298860775527337L;
                
                @Override
                public boolean canPlayAI() {
                    String phase = AllZone.Phase.getPhase();
                    return phase.equals(Constant.Phase.Main2);
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                }
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Elf Warrior", "GW 1 1 Elf Warrior", card, "GW", new String[] {
                            "Creature", "Elf", "Warrior"}, 1, 1, new String[] {""});
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(tokenAbility1);
            
            tokenAbility1.setDescription("2 GW, tap: Put a 1/1 green and white Elf Warrior creature token into play.");
            tokenAbility1.setStackDescription(card.getName()
                    + " - Put a 1/1 green and white Elf Warrior creature token into play.");
            tokenAbility1.setBeforePayMana(new Input_PayManaCost(tokenAbility1));
            */
            /////////////////////////////////////////////////////////////////////
            
            final Ability_Tap copyTokens1 = new Ability_Tap(card, "4 GW GW") {
                private static final long serialVersionUID = 6297992502069547478L;
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(copy);
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
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
            copyTokens1.setDescription("4 GW GW, tap: For each creature token you control, put a token into play that's a copy of that creature.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - For each creature token you control, put a token into play that's a copy of that creature.");
            copyTokens1.setStackDescription(sb.toString());
            copyTokens1.setBeforePayMana(new Input_PayManaCost(copyTokens1));
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
            final Player player = card.getController();
            Command turnsFaceUp = new Command() {
                private static final long serialVersionUID = -1407485989096862288L;
                
                public void execute() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    
                    CardList list = new CardList(lib.getCards());
                    list = list.getType("Dragon");
                    
                    if(list.size() == 0) return;
                    

                    if(player.equals(AllZone.ComputerPlayer)) {
                        Card dragon = CardFactoryUtil.AI_getBestCreature(list);
                        lib.remove(dragon);
                        hand.add(dragon);
                    } else //human
                    {
                        Object o = AllZone.Display.getChoiceOptional("Select Dragon", list.toArray());
                        Card dragon = (Card) o;
                        lib.remove(dragon);
                        hand.add(dragon);
                    }
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
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    
                    CardList hum = new CardList();
                    hum.addAll(AllZone.Human_Play.getCards());
                    
                    if(all.size() != 0) {
                        
                        if(card.getController().equals(AllZone.HumanPlayer)) {
                            AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, all,
                                    "Select target permanent to tap/untap.", true, false));
                            ButtonUtil.enableAll();
                        } else if(card.getController().equals(AllZone.ComputerPlayer)) {
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
        else if(cardName.equals("Captain Sisay")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 7978812786945030021L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }//resolve()
                
                public void humanResolve() {
                    CardList cards = new CardList(AllZone.Human_Library.getCards());
                    //legends = legends.getType().contains("Legendary");
                    CardList legends = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        //System.out.println("type: " +cards.get(i).getType());
                        if(cards.get(i).getType().contains("Legendary")) {
                            //System.out.println(cards.get(i).getName());
                            Card c = cards.get(i);
                            legends.add(c);
                            
                        }
                    }
                    
                    if(legends.size() != 0) {
                        Object check = AllZone.Display.getChoiceOptional("Select a legendary card", legends.toArray());
                        if(check != null) {
                            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                            AllZone.GameAction.moveTo(hand, (Card) check);
                        }
                        AllZone.HumanPlayer.shuffle();
                    }
                }
                
                public void computerResolve() {
                    Card[] library = AllZone.Computer_Library.getCards();
                    CardList list = new CardList(library);
                    CardList legends = new CardList();
                    //list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(list.get(i).getType().contains("Legendary")) {
                            Card k = list.get(i);
                            legends.add(k);
                        }
                        
                    }
                    
                    //pick best creature
                    if(legends.size() != 0) {
                        Card c = CardFactoryUtil.AI_getBestCreature(legends);
                        if(c == null) c = library[0];
                        Log.debug("Captain Sisay","computer picked - " + c);
                        AllZone.Computer_Library.remove(c);
                        AllZone.Computer_Hand.add(c);
                    }
                }
                

            };//SpellAbility
            //card.addSpellAbility(ability);
            ability.setDescription("tap: Search your library for a legendary card, reveal that card, and put it into your hand. Then shuffle your library.");
            ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
            ability.setStackDescription("Captain Sisay searches for a legendary card...");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Siege-Gang Commander")) {
            
            final SpellAbility comesIntoPlayAbility = new Ability(card, "0") {
                @Override
                public void resolve() {
                    makeToken();
                    makeToken();
                    makeToken();
                }//resolve()
                
                public void makeToken() {
                    CardFactoryUtil.makeToken("Goblin", "R 1 1 Goblin", card, "R", new String[] {
                            "Creature", "Goblin"}, 1, 1, new String[] {""});
                }
                
            }; //comesIntoPlayAbility
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 8778828278589063477L;
                
                public void execute() {
                    comesIntoPlayAbility.setStackDescription(card.getName()
                            + " - put three 1/1 red Goblin creature tokens into play.");
                    AllZone.Stack.add(comesIntoPlayAbility);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            

            final SpellAbility ability = new Ability(card, "1 R") {
                
                private static final long serialVersionUID = -6653781740344703908L;
                
                @Override
                public void resolve() {
                    String player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }//resolve()
                
                public void humanResolve() {
                    String player = card.getController();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    CardList cards = new CardList(play.getCards());
                    
                    CardList creatures = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).isType("Goblin")) {
                            Card k = cards.get(i);
                            creatures.add(k);
                        }
                    }
                    
                    if(creatures.size() != 0) {
                        Object check = AllZone.Display.getChoiceOptional("Select Goblin to Sacrifice",
                                creatures.toArray());
                        if(check != null) {
                            Card c = (Card) check;
                            if(AllZone.GameAction.isCardInPlay(c)) {
                                AllZone.GameAction.sacrifice(c);
                                
                                if(getTargetCard() != null) {
                                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                                        Card crd = getTargetCard();
                                        //c.addDamage(damage);
                                        AllZone.GameAction.addDamage(crd, card, 2);
                                    }
                                } else AllZone.GameAction.getPlayerLife(getTargetPlayer()).loseLife(2,card);
                                
                            }
                        }
                    }
                }//humanResolve
                
                public void computerResolve() {
                    String player = card.getController();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    CardList cards = new CardList(play.getCards());
                    
                    CardList creatures = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).isType("Goblin")) {
                            Card k = cards.get(i);
                            creatures.add(k);
                        }
                    }
                    //.... TODO
                    
                }//compResolve
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
            };//ability
            
            card.addSpellAbility(ability);
            ability.setDescription("1 R, Sacrifice a goblin: Siege-Gang Commander deals 2 damage to target creature or player .");
            ability.setStackDescription("Siege-Gang Commander deals 2 damage to target creature or player");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true, false));
        }//*************** END ************ END **************************
        */

        //*************** START *********** START **************************
        else if(cardName.equals("Brion Stoutarm")) {
            
            final SpellAbility ability = new Ability_Tap(card, "R") {
                private static final long serialVersionUID = -7755879134314608010L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }//resolve()
                
                public void humanResolve() {
                	Player player = card.getController();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    CardList cards = new CardList(play.getCards());
                    
                    CardList creatures = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Creature")
                                && !cards.get(i).getName().equals("Brion Stoutarm")) {
                            Card k = cards.get(i);
                            creatures.add(k);
                        }
                    }
                    
                    if(creatures.size() != 0) {
                        Object check = AllZone.Display.getChoiceOptional("Select Creature to Sacrifice",
                                creatures.toArray());
                        if(check != null) {
                            Card c = (Card) check;
                            if(AllZone.GameAction.isCardInPlay(c)) {
                                int power = c.getNetAttack();
                                AllZone.GameAction.sacrifice(c);
                                Player opponent = player.getOpponent();
                                
                                opponent.addDamage(power, card);
                                
                                //unTODO: this may not be needed - definitely not needed (slapshot5)
                                //GameActionUtil.executeLifeLinkEffects(card, power);
                                /*
                                CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
                                for(Card crd:cl) {
                                    GameActionUtil.executeGuiltyConscienceEffects(card, crd, power);
                                }
                                */
                                
                                //TODO - this may not bee needed (or may need to be added in player.addDamage
                                card.setDealtDmgToOppThisTurn(true);
                            }
                        }
                    }
                }//humanResolve
                
                public void computerResolve() {
                	Player player = card.getController();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    CardList cards = new CardList(play.getCards());
                    
                    CardList creatures = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        if(cards.get(i).getType().contains("Creature")
                                && !cards.get(i).getName().equals("Brion Stoutarm")) {
                            Card k = cards.get(i);
                            creatures.add(k);
                        }
                    }
                    //.... TODO
                    
                }//compResolve
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
            };//ability
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append("R, tap, Sacrifice a creature other than Brion Stoutarm: Brion Stoutarm ");
            sb.append("deals damage equal to the sacrificed creature's power to target player.");
            ability.setDescription(sb.toString());
            
            ability.setStackDescription("Brion Stoutarm deals damage equal to sacrificed creature's power");
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
                        
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                

                /**
			 * 
			 */
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
            ability.setStackDescription("Archon of Justice - Remove target permament from the game.");
            
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
                        
                        CardList human_list = new CardList(AllZone.Human_Play.getCards());
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
                                CardList computer_list = new CardList(AllZone.Computer_Play.getCards());
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
        else if(cardName.equals("Knight of the Reliquary")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 7554368501399705784L;
                
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.GameAction.sacrifice(c);
                        
                        CardList landInLib = new CardList(lib.getCards());
                        landInLib = landInLib.getType("Land");
                        
                        if(landInLib.size() > 0) {
                            if(card.getController().equals(AllZone.ComputerPlayer)) {
                                lib.remove(landInLib.get(0));
                                play.add(landInLib.get(0));
                            } else {
                                Object o = AllZone.Display.getChoiceOptional(
                                        "Select land card to put into play: ", landInLib.toArray());
                                if(o != null) {
                                    Card crd = (Card) o;
                                    lib.remove(crd);
                                    play.add(crd);
                                }
                            }
                            card.getController().shuffle();
                        }
                    }//if(isCardInPlay)
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList landInLib = new CardList(AllZone.getZone(Constant.Zone.Library,
                            AllZone.ComputerPlayer).getCards());
                    CardList landInPlay = new CardList(AllZone.getZone(Constant.Zone.Play,
                            AllZone.ComputerPlayer).getCards());
                    
                    landInLib = landInLib.getType("Land");
                    landInPlay = landInPlay.getType("Land");
                    
                    if(landInLib.size() > 0 && landInPlay.size() > 0
                            && (AllZone.Phase.getPhase().equals("Main2") || card.getNetAttack() < 5)) return true;
                    else return false;
                    
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList land = new CardList(
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                    land = land.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            if(c.getType().contains("Plains") || c.getType().contains("Forest")) return true;
                            else return false;
                        }
                    });
                    if(land.size() > 0) setTargetCard(land.get(0));
                }
            };
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -4320917612145305541L;
                
                @Override
                public void showMessage() {
                    CardList land = new CardList(
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                    land = land.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            if(c.getType().contains("Plains") || c.getType().contains("Forest")) return true;
                            else return false;
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, land,
                            "Select a Plains or Forest to sacrifice.", false, false));
                }
            };
            ability.setBeforePayMana(runtime);
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("\r\n").append("tap, Sacrifice a Forest or Plains: Search your library ");
            sbDesc.append("for a land card, put it into play, then shuffle your library.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card.getName()).append(" - Search your library for a card and put it into play, then shuffle your library");
            ability.setStackDescription(sbStack.toString());
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Knight of the White Orchid")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                            Object o = AllZone.Display.getChoiceOptional("Select Plains card to put into play: ",
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
                    PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play,
                            player.getOpponent());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList self = new CardList(play.getCards());
                    CardList opp = new CardList(oppPlay.getCards());
                    
                    self = self.getType("Land");
                    opp = opp.getType("Land");
                    
                    if(self.size() < opp.size()) {
                    	StringBuilder sb = new StringBuilder();
                    	sb.append(card.getName()).append(" - search library for a plains and put it into play");
                    	ability.setStackDescription(sb.toString());
                        
                        AllZone.Stack.add(ability);
                    }
                }
            };
            
            card.addComesIntoPlayCommand(fetchBasicLand);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if(cardName.equals("Doomed Necromancer")) {   
            final SpellAbility ability = new Ability_Tap(card, "B") {
                private static final long serialVersionUID = -6432831150810562390L;
            
                @Override
                public boolean canPlayAI() {
                    return getCreatures().length != 0;
                }//canPlayAI()
            
                public Card[] getCreatures() {
                    CardList creature = new CardList();
                    PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    creature.addAll(zone.getCards());
                    creature = creature.getType("Creature");
                    if (AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && card.getController().equals(AllZone.ComputerPlayer)) {
                        creature = creature.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");
                    }
                    creature = creature.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getNetAttack() > 4;
                        }
                    });                   
                    return creature.toArray();
                }//getCreatures()
                
                @Override
                public void chooseTargetAI() {
                    Card c[] = getCreatures();
                    Card biggest = c[0];
                    for(int i = 0; i < c.length; i++)
                        if(biggest.getNetAttack() < c[i].getNetAttack()) biggest = c[i];
                   
                    setTargetCard(biggest);
                }//chooseTargetAI()
                
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        CardList creature = new CardList();
                        PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                        creature.addAll(zone.getCards());
                        creature = creature.getType("Creature");
                        if(creature.size() != 0) {
                            Object o = AllZone.Display.getChoice("Choose a creature from the graveyard to return to the battlefield", creature.toArray());
                    
                            if(o != null) {
                                Card c = (Card) o;
                                if(AllZone.GameAction.isCardInZone(c, zone)) {
                                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                                    AllZone.GameAction.moveTo(play, c);
                                    AllZone.GameAction.sacrifice(card);
                                }
                            }
                        } else {   
                            AllZone.GameAction.sacrifice(card);                          
                        }
                    } else {//Computer
                        CardList creature = new CardList();
                        PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                        creature.addAll(zone.getCards());
                        creature = creature.getType("Creature");
                        Card c[] = getCreatures();
                        Card biggest = c[0];
                        for(int i = 0; i < c.length; i++)
                            if(biggest.getNetAttack() < c[i].getNetAttack()) biggest = c[i];
                        
                        if(creature.size() != 0) {
                            if(biggest != null) {
                                if(AllZone.GameAction.isCardInZone(biggest, zone)) {
                                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, biggest.getController());
                                    AllZone.GameAction.moveTo(play, biggest);
                                    AllZone.GameAction.sacrifice(card);
                                }
                            }
                        } else {
                            AllZone.GameAction.sacrifice(card);                          
                        }   
                    }
                }//resolve()   
            };//SpellAbility
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" gets sacrificed to return target creature card from your graveyard to the battlefield");
            ability.setStackDescription(sb.toString());
            
            // ability.setDescription("B, Tap: Sacrifice Doomed Necromancer: Return target creature card from your graveyard to the battlefield");
            // card.addSpellAbility(new Spell_Permanent(card) {
            //     private static final long serialVersionUID = -462134621235305833L;
            // }); 
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
                            list.addAll(AllZone.Computer_Play.getCards());
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
        else if(cardName.equals("Elvish Farmer") || cardName.equals("Mycologist")) {
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 882942955555047018L;
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
                @Override
                public void chooseTargetAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList saps = new CardList(play.getCards());
                    saps = saps.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            if((c.getType().contains("Saproling") || c.getKeyword().contains("Changeling"))
                                    && AllZone.GameAction.isCardInPlay(c)) return true;
                            return false;
                        }
                        
                    });
                    
                    if(saps.size() != 0) setTargetCard(saps.getCard(0));
                }
                
                @Override
                public void resolve() {
                    //get all saprolings:
                    Card c = getTargetCard();
                    if(c == null) return;
                    
                    if(!AllZone.GameAction.isCardInPlay(c)) return;
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.GameAction.sacrifice(c);
                        c.getController().gainLife(2, card);
                    }
                }//resolve
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList saps = new CardList(play.getCards());
                    saps = saps.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            if(c.getType().contains("Saproling") || c.getKeyword().contains("Changeling")
                                    && AllZone.GameAction.isCardInPlay(c)) return true;
                            return false;
                        }
                        
                    });
                    if(AllZone.ComputerPlayer.getLife() < 6 && saps.size() > 0) return true;
                    else return false;
                }
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -4803541385354247499L;
                
                @Override
                public void showMessage() {
                    CardList saps = new CardList(
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                    saps = saps.getType("Saproling");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2, saps, "Select a Saproling to sacrifice.",
                            false, false));
                }
            };
            
            card.addSpellAbility(a2);
            a2.setDescription("Sacrifice a Saproling: You gain 2 life.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" gains 2 life.");
            a2.setStackDescription(sb.toString());
            
            a2.setBeforePayMana(runtime);
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
                                           PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                                           
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
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList creats = new CardList(play.getCards());
                    creats = creats.getType("Creature");
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        //AllZone.getZone(c).remove(c);
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
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
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
        else if(cardName.equals("Psychotrope Thallid")) {
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 8020106056714209199L;
                public boolean            firstTime        = true;
                
                public void execute() {
                    
                    if(firstTime) {
                        card.setCounter(Counters.SPORE, 0, false);
                    }
                    firstTime = false;
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            
            final SpellAbility a2 = new Ability(card, "1") {
                @Override
                public void resolve() {
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        //AllZone.getZone(c).remove(c);
                        AllZone.GameAction.sacrifice(c);
                        
                        card.getController().drawCard();
                    }
                }//resolve
                
                @Override
                public boolean canPlayAI() {
                    //TODO: make AI able to use this
                    return false;
                }
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -6388866343458002392L;
                
                @Override
                public void showMessage() {
                    CardList saps = new CardList(
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                    saps = saps.getType("Saproling");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2, saps, "Select a Saproling to sacrifice.",
                            false, false));
                }
            };
            
            card.addSpellAbility(a2);
            a2.setDescription("1, Sacrifice a Saproling: You draw a card.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" draws a card.");
            a2.setStackDescription(sb.toString());
            
            a2.setBeforePayMana(runtime);
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if(cardName.equals("Rootwater Thief")) {
            //final String player = card.getController();
            //final String opponent = AllZone.GameAction.getOpponent(player);
            
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
            //card.clearSpellAbility();
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
                    CardList cards = new CardList();
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    cards.addAll(hum.getCards());
                    cards.addAll(comp.getCards());
                    cards = cards.getType("Creature");
                    
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
                        PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                        PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                        CardList list = new CardList();
                        list.addAll(cPlay.getCards());
                        list.addAll(hPlay.getCards());
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
                        PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                        PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                        CardList list = new CardList();
                        list.addAll(cPlay.getCards());
                        list.addAll(hPlay.getCards());
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
                	sb.append(card.getName()).append(" - put three 1/1 Saproling creature tokens into play.");
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
                    sb.append(card.getName()).append(" - put creature tokens into play.");
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                                    "Select Basic Land card to put into play tapped: ", basic.toArray());
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
                	sb.append(card.getName()).append(" - search library for a basic land card and put it into play tapped.");
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
        else if(cardName.equals("Master Transmuter")) {
            final Ability_Tap ability = new Ability_Tap(card, "U") {
                
                private static final long serialVersionUID = -9076784333448226913L;
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    CardList artifacts = new CardList(hand.getCards());
                    artifacts = artifacts.getType("Artifact");
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional(
                                "Select artifact to put onto the battlefield: ", artifacts.toArray());
                        if(o != null) {
                            Card c = (Card) o;
                            hand.remove(c);
                            play.add(c);
                        }
                    } else {
                        //CardList arts = new CardList(play.getCards());
                        //arts = arts.getType("Artifact");
                        
                        Card c = getTargetCard();
                        AllZone.GameAction.moveTo(hand, c);
                        
                        Card crd = CardFactoryUtil.AI_getMostExpensivePermanent(artifacts, card, false);
                        hand.remove(crd);
                        play.add(crd);
                        
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    
                    CardList handArts = new CardList(hand.getCards());
                    handArts = handArts.getType("Artifact");
                    
                    CardList playArts = new CardList(play.getCards());
                    playArts = playArts.getType("Artifact");
                    
                    if(handArts.size() > 0 && playArts.size() > 0) {
                        
                        if(CardUtil.getConvertedManaCost(CardFactoryUtil.AI_getCheapestPermanent(playArts, card,
                                false).getManaCost()) < CardUtil.getConvertedManaCost(CardFactoryUtil.AI_getMostExpensivePermanent(
                                handArts, card, false).getManaCost())) {
                            setTargetCard(CardFactoryUtil.AI_getCheapestPermanent(playArts, card, false));
                            return true;
                        }
                        
                    }
                    return false;
                }
            };
            
            Input target = new Input() {
                
                private static final long serialVersionUID = 4246650335595231655L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select artifact to return to hand");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    if(c.isArtifact() && AllZone.GameAction.isCardInZone(c, play)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        
                        AllZone.GameAction.moveTo(hand, c);
                        AllZone.Stack.add(ability);
                        stopSetNext(new ComputerAI_StackNotEmpty());
                    }
                }//selectCard()
            };//Input
            
            card.addSpellAbility(ability);
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("U, tap, Return an artifact you control to its owner's hand: ");
            sbDesc.append("You may put an artifact card from your hand onto the battlefield.");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append("You may put an artifact card from your hand onto the battlefield");
            ability.setStackDescription(sbStack.toString());
            
            ability.setAfterPayMana(target);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Hanna, Ship's Navigator")) {
            final Ability_Tap ability = new Ability_Tap(card, "1 U W") {
                private static final long serialVersionUID = 7959233413572648987L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }//resolve()
                
                public void humanResolve() {
                    CardList cards = new CardList(AllZone.Human_Graveyard.getCards());
                    //legends = legends.getType().contains("Legendary");
                    CardList list = new CardList();
                    
                    for(int i = 0; i < cards.size(); i++) {
                        //System.out.println("type: " +cards.get(i).getType());
                        if(cards.get(i).getType().contains("Artifact")
                                || cards.get(i).getType().contains("Enchantment")) {
                            //System.out.println(cards.get(i).getName());
                            Card c = cards.get(i);
                            list.add(c);
                            
                        }
                    }
                    
                    if(list.size() != 0) {
                        Object check = AllZone.Display.getChoiceOptional("Select Artifact or Enchantment",
                                list.toArray());
                        if(check != null) {
                            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                            AllZone.GameAction.moveTo(hand, (Card) check);
                        }
                    }
                }
                
                public void computerResolve() {
                    Card[] grave = AllZone.Computer_Graveyard.getCards();
                    CardList list = new CardList(grave);
                    CardList artenchants = new CardList();
                    //list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(list.get(i).getType().contains("Artifact")
                                || list.get(i).getType().contains("Enchantment")) {
                            Card k = list.get(i);
                            artenchants.add(k);
                        }
                        
                    }
                    
                    //pick best artifact / enchantment
                    if(artenchants.size() != 0) {
                        Card c = CardFactoryUtil.AI_getBestArtifact(list);
                        if(c == null) c = CardFactoryUtil.AI_getBestEnchantment(list, card, true);
                        if(c == null) c = grave[0];
                        Log.debug("Hanna, Ship's Navigator", "computer picked - " + c);
                        AllZone.Computer_Graveyard.remove(c);
                        AllZone.Computer_Hand.add(c);
                    }
                }//computerResolve
                
                @Override
                public boolean canPlay() {
                	Player controller = card.getController();
                    
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, controller);
                    CardList list = new CardList(grave.getCards());
                    CardList cards = new CardList();
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(list.get(i).getType().contains("Artifact")
                                || list.get(i).getType().contains("Enchantment")) {
                            cards.add(list.get(i));
                        }
                    }
                    
                    //System.out.println("cards.size(): " + cards.size());
                    //hasSickness is a little hack, I'm not sure about the setBeforeMana input
                    if(cards.size() > 0 && AllZone.GameAction.isCardInPlay(card) && !card.hasSickness()
                            && card.isUntapped() && super.canPlay()) return true;
                    else return false;
                }
                

            };//SpellAbility
            //card.addSpellAbility(ability);
            ability.setDescription("1 U W, tap: Return target artifact or enchantment card from your graveyard to your hand.");
            ability.setBeforePayMana(new Input_PayManaCost(ability));
            ability.setStackDescription("Hanna, Ship's Navigator - Returns an artifact or enchantment card from graveyard to hand.");
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cromat")) {
            
            //LibBounce Ability
            final Ability a1 = new Ability(card, "G U") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(card)) {
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
            
            Input runtime1 = new Input() {
                private static final long serialVersionUID = 1469011418219527227L;
                
                @Override
                public void showMessage() {
                	StringBuilder sb = new StringBuilder();
                	sb.append("Put ").append(card).append(" on top of its owner's library");
                	a1.setStackDescription(sb.toString());
                    
                    stopSetNext(new Input_PayManaCost(a1));
                }
            };
            a1.setDescription("G U: Put Cromat on top of its owner's library.");
            a1.setStackDescription("Put Cromat on top of its owner's library.");
            card.addSpellAbility(a1);
            a1.setBeforePayMana(runtime1);
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
        else if(cardName.equals("Energizer")) {
            Ability_Tap ability = new Ability_Tap(card, "2") {
                private static final long serialVersionUID = 6444406158364728638L;
                
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, 1);
                }
                
                @Override
                public boolean canPlayAI() {
                    return (true);
                }
            };
            ability.setDescription("2, tap: Put a +1/+1 counter on Energizer.");
            ability.setStackDescription("Put a +1/+1 counter on target Energizer.");
            card.addSpellAbility(ability);
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
                    //PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
                    //PlayerLife oppLife = AllZone.GameAction.getPlayerLife(AllZone.GameAction.getOpponent(c.getController()));
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
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 2045940121508110423L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7530032969328799083L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
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
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = -8473976122518500976L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
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
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 6575359591031318957L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
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
                    
                    //Object o = AllZone.Display.getChoiceOptional("Select a creature card to bounce", blackBlue.toArray());
                    

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
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -4018162972761688814L;
                
                public void execute() {
//                  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard,
//                          AllZone.GameAction.getOpponent(card.getController()));
//                  CardList gravecards = new CardList(grave.getCards());
                	
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
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    CardList creatures = new CardList(play.getCards());
                    creatures = creatures.getType("Creature");
                    
                    //Object o = AllZone.Display.getChoiceOptional("Select a creature card to bounce", blackBlue.toArray());
                    

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
        else if (cardName.equals("Crovax, Ascendant Hero"))
        {
        	final SpellAbility a1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().loseLife(2,card);
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    if(card.isToken()) AllZone.getZone(card).remove(card);
                    else AllZone.GameAction.moveTo(hand, card);
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            card.addSpellAbility(a1);
            a1.setDescription("Pay 2 life: Return Crovax, Ascendant Hero to its owner's hand.");
            
            StringBuilder sb1 = new StringBuilder();
            sb1.append(card.getController()).append(" pays 2 life and returns Crovax, Ascendant Hero back to owner's hand.");
            a1.setStackDescription(sb1.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cavern Harpy")) {
            final SpellAbility a1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    //PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
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
                            hand.add(newCard);
                        }
                    }
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7855081477395863590L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
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
            /*
        	final SpellAbility a1 = new Ability(card, "2 G") {
                @Override
                public boolean canPlayAI() {
                    return MyRandom.random.nextBoolean();
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    //this is a hack, check the stack to see if this card has an ability on the stack
                    //if so, we can't use the ability
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    return AllZone.GameAction.isCardInPlay(card) && super.canPlay();
                    
                }
                
                @Override
                public void resolve() {
                    makeToken();
                }
                
                void makeToken() {
                    CardFactoryUtil.makeToken("Saproling", "G 1 1 Saproling", card, "G", new String[] {
                            "Creature", "Saproling"}, 1, 1, new String[] {""});
                }//makeToken()
            };//SpellAbility
            */
            
            final SpellAbility a2 = new Ability(card, "0") {
                final Command eot1 = new Command() {
                                       private static final long serialVersionUID = -389286901477839863L;
                                       
                                       public void execute() {
                                           CardList saps = new CardList();
                                           saps.addAll(AllZone.Human_Play.getCards());
                                           saps.addAll(AllZone.Computer_Play.getCards());
                                           
                                           saps = saps.getType("Saproling");
                                           
                                           for(int i = 0; i < saps.size(); i++) {
                                               Card sap = saps.get(i);
                                               
                                               sap.addTempAttackBoost(-1);
                                               sap.addTempDefenseBoost(-1);
                                           }
                                           
                                       }
                                   };
                
                @Override
                public void resolve() {
                    //get all saprolings:
                    
                    CardList saps = new CardList();
                    saps.addAll(AllZone.Human_Play.getCards());
                    saps.addAll(AllZone.Computer_Play.getCards());
                    
                    saps = saps.getType("Saproling");
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        //AllZone.getZone(c).remove(c);
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
                    CardList saps = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
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
            		CardList creats = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
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
                            saps.addAll(AllZone.Human_Play.getCards());
                            saps.addAll(AllZone.Computer_Play.getCards());
                            
                            saps = saps.getType("Saproling");
                            
                            for(int i = 0; i < saps.size(); i++) {
                                Card sap = saps.get(i);
                                
                                sap.addTempAttackBoost(-numCreatures[0]);
                                sap.addTempDefenseBoost(-numCreatures[0]);
                            }
                            
                        }
                    };
                    
                    CardList saps = new CardList();
                    saps.addAll(AllZone.Human_Play.getCards());
                    saps.addAll(AllZone.Computer_Play.getCards());
                    
                    saps = saps.getType("Saproling");
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
            		CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards());
            		list = list.getType("Saproling");
            		return list.size() > 1 && super.canPlay();
            	}
            };
            //a1.setDescription("2G: Put a 1/1 green Saproling creature token into play.");
            //a1.setStackDescription("Put a 1/1 Saproling into play.");
            //card.addSpellAbility(a1);
            //a1.setBeforePayMana(new Input_PayManaCost(a1));
            
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
            //ability.setStackDescription("Ranger of Eos - Grab 2 creatures");
            //AllZone.Stack.add(ability);
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Master of the Wild Hunt")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 35050145102566898L;
                
                @Override
                public boolean canPlay() {
                	Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    
                    CardList Wolfs = new CardList(play.getCards());
                    Wolfs = Wolfs.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && (c.getType().contains("Wolf") || c.hasKeyword("Changeling")) && c.isCreature();
                        }
                    });
                    if(Wolfs.size() > 0 && AllZone.GameAction.isCardInPlay(card)
                            && CardFactoryUtil.canTarget(card, getTargetCard()) && super.canPlay()) return true;
                    else return false;                  
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList Wolfs = new CardList(play.getCards());
                    Wolfs = Wolfs.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && (c.getType().contains("Wolf") || c.hasKeyword("Changeling")) && c.isCreature();
                        }
                    });
                    final int TotalWolfPower = 2 * Wolfs.size();
                   // for(int i = 0; i < Wolfs.size(); i++) TotalWolfPower = TotalWolfPower + Wolfs.get(i).getNetAttack();
                    PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList human = new CardList(hplay.getCards());
                    human = human.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c) && !c.hasKeyword("Protection from Green") && c.isCreature() && c.getNetDefense() <= TotalWolfPower;
                        }
                    });
                	return human.size() > 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList Wolfs = new CardList(play.getCards());
                    Wolfs = Wolfs.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && (c.getType().contains("Wolf") || c.hasKeyword("Changeling")) && c.isCreature();
                        }
                    });
                    final int TotalWolfPower = 2 * Wolfs.size();
                   // for(int i = 0; i < Wolfs.size(); i++) TotalWolfPower = TotalWolfPower + Wolfs.get(i).getNetAttack();
                    PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList human = new CardList(hplay.getCards());
                    human = human.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c) && !c.hasKeyword("Protection from Green") && c.isCreature() && c.getNetDefense() <= TotalWolfPower;
                        }
                    });
                    if(human.size() > 0) {
                    Card biggest = human.get(0);
                    for(int i = 0; i < human.size(); i++)
                        if(biggest.getNetAttack() < human.get(i).getNetAttack()) biggest = human.get(i);                         
                    		setTargetCard(biggest); 
                    }
                }
                @Override
                public void resolve() {
                	Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    
                    CardList Wolfs = new CardList(play.getCards());
                    Wolfs = Wolfs.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && (c.getType().contains("Wolf") || c.hasKeyword("Changeling")) && c.isCreature();
                        }
                    });
                                      
                            final Card target = getTargetCard();                           
                            if(AllZone.GameAction.isCardInPlay(target) && CardFactoryUtil.canTarget(card, target) && Wolfs.size() > 0) {
                             for(int i = 0; i < Wolfs.size() ; i++) {
                            	 Wolfs.get(i).tap();
                            	 target.addDamage(Wolfs.get(i).getNetAttack(),Wolfs.get(i));
                             }
                            }
                             if(card.getController().equals(AllZone.ComputerPlayer)) {
                             for(int x = 0; x < target.getNetAttack() ; x++) {
                            	 
                            	 AllZone.InputControl.setInput(CardFactoryUtil.MasteroftheWildHunt_input_targetCreature(this, Wolfs ,new Command() {
                                     
                                     private static final long serialVersionUID = -328305150127775L;
                                     
                                     public void execute() {
                                    	 getTargetCard().addDamage(1,target);
                                    	 AllZone.GameAction.checkStateEffects();
                                     }
                                 }));
                            }
                                                       
                    }//player.equals("human")
                    else {
                        for(int i = 0; i < target.getNetAttack(); i++) {
                        	CardList NewWolfs = Wolfs;
                        NewWolfs = NewWolfs.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return CardFactoryUtil.canTarget(card, c) && AllZone.GameAction.isCardInPlay(c) && !c.hasKeyword("Protection from Green") && !c.hasKeyword("Indestructible") && c.isCreature() && c.getNetDefense() <= target.getNetAttack();
                            }
                        });
                        if(NewWolfs.size() > 0) {
                        Card biggest = NewWolfs.get(0); 
                        for(int d = 0; d < NewWolfs.size(); d ++) if(biggest.getNetAttack() < NewWolfs.get(d).getNetAttack()) biggest = NewWolfs.get(d);                         
                        		setTargetCard(biggest);   
                        		getTargetCard().addDamage(1,target);
                    } else {
                        Wolfs = Wolfs.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return AllZone.GameAction.isCardInPlay(c);
                            }
                        });
                    	if(Wolfs.size() > 0) {
                    	Card biggest = Wolfs.get(0);
                    	setTargetCard(biggest);
                    	getTargetCard().addDamage(1,target);
                    	}
                    }
                        }
                    }
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Tap: Tap all untapped Wolf creatures you control. Each Wolf tapped ");
            sb.append("this way deals damage equal to its power to target creature. That creature deals ");
            sb.append("damage equal to its power divided as its controller chooses among any number of those Wolves.");
            ability.setDescription(sb.toString());
            
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Scarblade Elite")) {
            final Ability_Tap ability = new Ability_Tap(card) {
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
                        Object o = AllZone.Display.getChoiceOptional("Pick an Assassin to remove", sins.toArray());
                        
                        if(o != null) {
                            Card crd = (Card) o;
                            graveyard.remove(crd);
                            

                            Card c = getTargetCard();
                            
                            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                                AllZone.GameAction.destroy(c);
                            }
                        } //if o!= null
                    }//player.equals("human")
                    else {
                        Card crd = sins.get(0);
                        graveyard.remove(crd);
                        
                        Card c = getTargetCard();
                        if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                            AllZone.GameAction.destroy(c);
                        }
                    }
                }//resolve()
            };//SpellAbility
            
            Input target = new Input() {
                private static final long serialVersionUID = -4853162388286494888L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target creature to destroy");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(ability, card)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    }
                    if(c.isCreature() && zone.is(Constant.Zone.Play)) {
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Broodmate Dragon")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Dragon", "R 4 4 Dragon", card, "R", new String[] {
                            "Creature", "Dragon"}, 4, 4, new String[] {"Flying"});
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 2848700532090223394L;
                
                public void execute() {
                    ability.setStackDescription("Broodmate Dragon - put a 4/4 red Dragon creature token into play.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        */
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Deranged Hermit")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    for(int i = 0; i < 4; i++)
                        makeToken();
                }//resolve()
                
                void makeToken() {
                    CardFactoryUtil.makeToken("Squirrel", "G 1 1 Squirrel", card, "G", new String[] {
                            "Creature", "Squirrel"}, 1, 1, new String[] {""});
                }
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7299232424224916928L;
                
                public void execute() {
                    ability.setStackDescription("Deranged Hermit - put four green 1/1 Squirrel creature tokens onto the battlefield.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        */
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Grave Titan")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    for(int i = 0; i < 2; i++)
                        makeToken();
                }//resolve()
                
                void makeToken() {
                    CardFactoryUtil.makeToken("Zombie", "B 2 2 Zombie", card, "B", new String[] {
                            "Creature", "Zombie"}, 2, 2, new String[] {""});
                }
            };
            Command intoPlay = new Command() {

				private static final long serialVersionUID = 4152436387481421717L;

				public void execute() {
                    ability.setStackDescription("Grave Titan - put two black 2/2 Zombie creature tokens onto the battlefield.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Primeval Titan")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	AllZone.GameAction.searchLibraryTwoLand("Land", card.getController(), 
							Constant.Zone.Play, true, 
							Constant.Zone.Play, true);
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

        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Wydwen, the Biting Gale")) {
            final SpellAbility a1 = new Ability(card, "U B PayLife<1>") {
            	
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    AllZone.getZone(card).remove(card);
                    hand.add(card);
                    
                    if(card.isToken()) AllZone.getZone(card).remove(card);
                    else AllZone.GameAction.moveTo(hand, card);
                }
            };//a1
            
            card.addSpellAbility(a1);
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" pays 1 life and returns Wydwen back to owner's hand.");
            a1.setStackDescription(sb.toString());
            
            a1.setDescription("U B, Pay 1 life: Return Wydwen, the Biting Gale to its owner's hand.");
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Tradewind Rider")) {
        	String select = "Select target permanent to return to owner's hand.";
            final String Tgts[] = {"Permanent"};
            Target target = new Target(select, Tgts);

            final Ability_Cost cost = new Ability_Cost("T tapXType<2/Creature>", card.getName(), true);
        	
            final SpellAbility bounce = new Ability_Activated(card, cost, target) {
                private static final long serialVersionUID = 3438865371487994984L;
                
                @Override
                public void chooseTargetAI() {
                    if(getCreature().size() != 0) {
                        Card bestCreature = CardFactoryUtil.AI_getBestCreature(getCreature());
                        if(getEnchantment().size() != 0) {
                            Card bestEnchantment = CardFactoryUtil.AI_getBestEnchantment(getEnchantment(), card,
                                    true);
                            if(CardUtil.getConvertedManaCost(bestCreature.getManaCost()) > CardUtil.getConvertedManaCost(bestEnchantment.getManaCost())) {
                                setTargetCard(bestCreature);
                            } else {
                                setTargetCard(bestEnchantment);
                            }
                        } else {
                            setTargetCard(bestCreature);
                        }
                    } else if(getArtifact().size() != 0) {
                        Card bestArtifact = CardFactoryUtil.AI_getBestArtifact(getArtifact());
                        setTargetCard(bestArtifact);
                    }
                    
                }//ChooseTargetAI()
                
                CardList getCreature() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
                    return list;
                }//getEnchantment()
                
                CardList getArtifact() {
                    CardList list = CardFactoryUtil.AI_getHumanArtifact(card, true);
                    return list;
                }//getArtifact()
                
                CardList getEnchantment() {
                    CardList list = CardFactoryUtil.AI_getHumanEnchantment(card, true);
                    return list;
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList cards = new CardList();
                    
                    cards.addAll(play.getCards());
                    cards = cards.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            return (c.isArtifact() || c.isEnchantment() || c.isCreature())
                                    && CardFactoryUtil.canTarget(card, c);
                        }
                        
                    });
                    
                    return cards.size() > 0;
                    
                }
                
                @Override
                public boolean canPlay() {
                	Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    
                    CardList creats = new CardList();
                    
                    creats.addAll(play.getCards());
                    creats = creats.getType("Creature");
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && !c.equals(card);
                        }
                    });
                    
                    if(creats.size() > 1 && AllZone.GameAction.isCardInPlay(card) && card.isUntapped()
                            && !card.hasSickness() && super.canPlay()) return true;
                    else return false;
                }
                
                @Override
                public void resolve() {
                    
                    if(getTargetCard() == null) return;
                    Card c = getTargetCard();
                    if(c.isToken()) AllZone.getZone(c).remove(c);
                    else {
                    	PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                    	AllZone.GameAction.moveTo(hand, c);
                    }
                }
                
            };//a1
            
            bounce.setDescription("tap, Tap two untapped creatures you control: Return target permanent to its owner's hand.");
            card.addSpellAbility(bounce);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Nullmage Shepherd")) {
            final SpellAbility a1 = new Ability(card, "0") {
                @Override
                public void chooseTargetAI() {
                    if(getEnchantment().size() != 0) {
                        Card bestEnchantment = CardFactoryUtil.AI_getBestEnchantment(getEnchantment(), card, true);
                        if(getArtifact().size() != 0) {
                            Card bestArtifact = CardFactoryUtil.AI_getBestArtifact(getArtifact());
                            if(CardUtil.getConvertedManaCost(bestArtifact.getManaCost()) > CardUtil.getConvertedManaCost(bestEnchantment.getManaCost())) {
                                setTargetCard(bestArtifact);
                            } else {
                                setTargetCard(bestEnchantment);
                            }
                        } else {
                            setTargetCard(bestEnchantment);
                        }
                    } else if(getArtifact().size() != 0) {
                        Card bestArtifact = CardFactoryUtil.AI_getBestArtifact(getArtifact());
                        setTargetCard(bestArtifact);
                    }
                    
                }
                
                CardList getEnchantment() {
                    CardList list = CardFactoryUtil.AI_getHumanEnchantment(card, true);
                    return list;
                }//getEnchantment()
                
                CardList getArtifact() {
                    CardList list = CardFactoryUtil.AI_getHumanArtifact(card, true);
                    return list;
                }//getArtifact()
                

                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList cards = new CardList();
                    
                    cards.addAll(play.getCards());
                    cards = cards.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            return (c.isArtifact() || c.isEnchantment()) && CardFactoryUtil.canTarget(card, c);
                        }
                        
                    });
                    
                    return cards.size() > 0;
                    
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    
                    CardList creats = new CardList();
                    
                    creats.addAll(play.getCards());
                    creats = creats.getType("Creature");
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped();
                        }
                    });
                    
                    if(creats.size() > 3 && AllZone.GameAction.isCardInPlay(card) && super.canPlay()) return true;
                    else return false;
                }
                
                @Override
                public void resolve() {
                    
                    if(getTargetCard() == null) return;
                    
                    Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void humanResolve() {
                    Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    CardList creats = new CardList();
                    
                    creats.addAll(play.getCards());
                    creats = creats.getType("Creature");
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped();
                        }
                    });
                    
                    CardList tappedCreats = new CardList();
                    
                    Object o = AllZone.Display.getChoice("Pick first creature to tap", creats.toArray());
                    
                    if(o != null) {
                        Card c1 = (Card) o;
                        creats.remove(c1);
                        tappedCreats.add(c1);
                    } else return;
                    
                    o = AllZone.Display.getChoice("Pick second creature to tap", creats.toArray());
                    if(o != null) {
                        Card c2 = (Card) o;
                        creats.remove(c2);
                        tappedCreats.add(c2);
                    } else return;
                    
                    o = AllZone.Display.getChoice("Pick third creature to tap", creats.toArray());
                    if(o != null) {
                        Card c3 = (Card) o;
                        creats.remove(c3);
                        tappedCreats.add(c3);
                    } else return;
                    
                    o = AllZone.Display.getChoice("Pick fourth creature to tap", creats.toArray());
                    if(o != null) {
                        Card c4 = (Card) o;
                        creats.remove(c4);
                        tappedCreats.add(c4);
                    } else return;
                    

                    for(int i = 0; i < tappedCreats.size(); i++) {
                        Card tapCreat = tappedCreats.get(i);
                        tapCreat.tap();
                    }
                    
                    AllZone.GameAction.destroy(getTargetCard());
                }//humanResolve
                
                public void computerResolve() {
                    Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    CardList creats = new CardList();
                    
                    creats.addAll(play.getCards());
                    creats = creats.getType("Creature");
                    

                    for(int i = 0; i < 4; i++) {
                        Card c = creats.get(i);
                        c.tap();
                    }
                    
                    AllZone.GameAction.destroy(getTargetCard());
                    
                }//computerResolve
                
            };//a1
            
            //card.clearSpellAbility();
            card.addSpellAbility(a1);
            a1.setDescription("Tap four untapped creatures you control: Destroy target artifact or enchantment.");
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = -7702308833923538927L;
                
                @Override
                public void showMessage() {
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = all.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.isEnchantment() || c.isArtifact()) && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a1, all,
                            "Destroy target artifact or enchantment.", true, false));
                }
            };
            a1.setBeforePayMana(runtime);
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Gilt-Leaf Archdruid")) {
            final SpellAbility a1 = new Ability(card, "0") {
                @Override
                public boolean canPlay() {
                    Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    
                    CardList druids = new CardList();
                    
                    druids.addAll(play.getCards());
                    druids = druids.getType("Druid");
                    
                    //System.out.println("Druids size: " + druids.size());
                    
                    int druidsUntapped = 0;
                    for(int i = 0; i < druids.size(); i++) {
                        Card c = druids.get(0);
                        if(!c.isTapped()) druidsUntapped++;
                    }
                    
                    if(druids.size() > 6 && druidsUntapped > 6 && AllZone.GameAction.isCardInPlay(card) && super.canPlay()) return true;
                    else return false;
                }
                
                @Override
                public void resolve() {
                    
                    Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void humanResolve() {
                    Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    CardList druids = new CardList();
                    
                    druids.addAll(play.getCards());
                    druids = druids.getType("Druid");
                    
                    CardList tappedDruids = new CardList();
                    
                    Object o = AllZone.Display.getChoice("Pick first druid to tap", druids.toArray());
                    
                    if(o != null) {
                        Card c1 = (Card) o;
                        druids.remove(c1);
                        tappedDruids.add(c1);
                    } else return;
                    
                    o = AllZone.Display.getChoice("Pick second druid to tap", druids.toArray());
                    if(o != null) {
                        Card c2 = (Card) o;
                        druids.remove(c2);
                        tappedDruids.add(c2);
                    } else return;
                    
                    o = AllZone.Display.getChoice("Pick third druid to tap", druids.toArray());
                    if(o != null) {
                        Card c3 = (Card) o;
                        druids.remove(c3);
                        tappedDruids.add(c3);
                    } else return;
                    
                    o = AllZone.Display.getChoice("Pick fourth druid to tap", druids.toArray());
                    if(o != null) {
                        Card c4 = (Card) o;
                        druids.remove(c4);
                        tappedDruids.add(c4);
                    } else return;
                    o = AllZone.Display.getChoice("Pick fifth druid to tap", druids.toArray());
                    if(o != null) {
                        Card c5 = (Card) o;
                        druids.remove(c5);
                        tappedDruids.add(c5);
                    } else return;
                    
                    o = AllZone.Display.getChoice("Pick sixth druid to tap", druids.toArray());
                    if(o != null) {
                        Card c6 = (Card) o;
                        druids.remove(c6);
                        tappedDruids.add(c6);
                    } else return;
                    
                    o = AllZone.Display.getChoice("Pick seventh druid to tap", druids.toArray());
                    if(o != null) {
                        Card c7 = (Card) o;
                        druids.remove(c7);
                        tappedDruids.add(c7);
                    } else return;
                    
                    for(int i = 0; i < tappedDruids.size(); i++) {
                        Card tapDruid = tappedDruids.get(i);
                        tapDruid.tap();
                    }
                    
                    Player opponent = controller.getOpponent();
                    PlayerZone opponentPlay = AllZone.getZone(Constant.Zone.Play, opponent);
                    
                    CardList lands = new CardList();
                    lands.addAll(opponentPlay.getCards());
                    lands = lands.getType("Land");
                    
                    //System.out.println("Land size: " +lands.size());
                    
                    for(int i = 0; i < lands.size(); i++) {
                        Card land = lands.get(i);
                        

                        if(AllZone.GameAction.isCardInPlay(land)) {
                            land.setController(controller);
                            
                            //set summoning sickness
                            if(land.getKeyword().contains("Haste")) {
                                land.setSickness(false);
                            } else {
                                land.setSickness(true);
                            }
                            
                            ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                            ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                            
                            PlayerZone from = AllZone.getZone(land);
                            from.remove(land);
                            
                            PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
                            to.add(land);
                            
                            ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                            ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                        }//if
                        

                    }
                    
                }//humanResolve
                
                public void computerResolve() {
                    Player controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    CardList druids = new CardList();
                    
                    druids.addAll(play.getCards());
                    druids = druids.getType("Druid");
                    

                    for(int i = 0; i < 7; i++) {
                        Card c = druids.get(i);
                        c.tap();
                        
                    }
                    
                    Player opponent = controller.getOpponent();
                    PlayerZone opponentPlay = AllZone.getZone(Constant.Zone.Play, opponent);
                    
                    CardList lands = new CardList();
                    lands.addAll(opponentPlay.getCards());
                    lands = lands.getType("Land");
                    
                    for(int i = 0; i < lands.size(); i++) {
                        Card land = lands.get(i);
                        if(AllZone.GameAction.isCardInPlay(land)) {
                            land.setController(controller);
                            
                            //set summoning sickness
                            if(land.getKeyword().contains("Haste")) {
                                land.setSickness(false);
                            } else {
                                land.setSickness(true);
                            }
                            
                            ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                            ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                            
                            PlayerZone from = AllZone.getZone(land);
                            from.remove(land);
                            
                            PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
                            to.add(land);
                            
                            ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                            ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                        }//if
                    }
                    
                }//computerResolve
                
            };//a1
            
            card.clearSpellAbility();
            card.addSpellAbility(a1);
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" taps seven untapped Druids and gains control of all opponent's land.");
            a1.setStackDescription(sb.toString());
            
            a1.setDescription("Tap seven untapped Druids you control: Gain control of all lands target player controls.");
            
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -4621346281051305833L;
                
                @Override
                public boolean canPlayAI() {
                    return true;
                }
            });
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
                    return !card.getType().contains("Spirit");
                }
                
            };// ability1
            
            ability1.setDescription("RW: Figure of Destiny becomes a 2/2 Kithkin Spirit.");
            ability1.setStackDescription("Figure of Destiny becomes a 2/2 Kithkin Spirit.");
            card.addSpellAbility(ability1);
            

            Ability ability2 = new Ability(card, "RW RW RW") {
                @Override
                public void resolve() {
                    if(card.getType().contains("Spirit") || card.getKeyword().contains("Changeling")) {
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
                    return card.getType().contains("Spirit") || card.getKeyword().contains("Changeling") && super.canPlay();
                }
                
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Warrior");
                }
                
            };// ability2
            
            ability2.setDescription("RW RW RW: If Figure of Destiny is a Spirit, it becomes a 4/4 Kithkin Spirit Warrior.");
            ability2.setStackDescription("Figure of Destiny becomes a 4/4 Kithkin Spirit Warrior.");
            card.addSpellAbility(ability2);
            

            Ability ability3 = new Ability(card, "RW RW RW RW RW RW") {
                @Override
                public void resolve() {
                    if(card.getType().contains("Warrior") || card.getKeyword().contains("Changeling")) {
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
                    return card.getType().contains("Warrior") || card.getKeyword().contains("Changeling") && super.canPlay();
                }
                
                @Override
                public boolean canPlayAI() {
                    return !card.getType().contains("Avatar");
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
        else if(cardName.equals("Jenara, Asura of War")) {
            
            Ability ability2 = new Ability(card, "1 W") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, 1);
                }
            };// ability2
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - gets a +1/+1 counter.");
            ability2.setStackDescription(sb2.toString());
             
            ability2.setDescription("1 W: Put a +1/+1 counter on Jenara, Asura of War.");
            card.addSpellAbility(ability2);
        }//*************** END ************ END **************************
        
        
        //*************** START ************ START **************************
        else if(cardName.equals("Rats of Rath")) {
            final Ability ability = new Ability(card, "B") {
                @Override
                public boolean canPlayAI() {
                    return false;
                };
                
                @Override
                public void resolve() {
                    AllZone.GameAction.destroy(getTargetCard());
                };
            };
            ability.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 7691864588170864421L;
                
                @Override
                public void showMessage() {
                    CardList choices = new CardList(
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                    choices.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() || c.isCreature() || c.isLand();
                        }
                    });
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choices,
                            "Select an artifact, creature, or land you control", true, false));
                }
            });
            ability.setDescription("B: Destroy target artifact, creature, or land you control.");
            card.addSpellAbility(ability);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lich Lord of Unx")) {
            /*
        	final SpellAbility ability = new Ability_Tap(card, "U B") {
                private static final long serialVersionUID = 8909297504020264315L;
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Zombie Wizard", "UB 1 1 Zombie Wizard", card, "UB", new String[] {
                            "Creature", "Zombie", "Wizard"}, 1, 1, new String[] {""});
                }//resolve()
            };
            
            ability.setDescription("U B, Tap: Put a 1/1 blue and black Zombie Wizard creature token onto the battlefield.");
            ability.setStackDescription(card.getName() + " - " + card.getController()
                    + "puts a 1/1 blue and black Zombie Wizard creature token onto the battlefield.");
            */
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
            
            //card.addSpellAbility(ability);
            card.addSpellAbility(ability2);         
        }//*************** END ************ END **************************
        
                
        //*************** START *********** START **************************
        else if(cardName.equals("Covetous Dragon")) {
            SpellAbility spell = new Spell_Permanent(card) {
                
                private static final long serialVersionUID = -1446713295855849195L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(
                            AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
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
                            AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
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
                            
                            CardList LandList = new CardList(AllZone.getZone(Constant.Zone.Play,
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
        else if(cardName.equals("Magus of the Library")) {
            final Ability_Tap ability2 = new Ability_Tap(card) {
                private static final long serialVersionUID = 6567685794684744457L;
                
                @Override
                public boolean canPlay() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    return hand.getCards().length == 7 && super.canPlay();
                }
                
                @Override
                public void resolve() {
                	getActivatingPlayer().drawCard();
                }
            };//SpellAbility
            card.addSpellAbility(ability2);
            ability2.setDescription("tap: Draw a card. Play this ability only if you have exactly 7 cards in hand.");
            ability2.setStackDescription("Magus of the Library - draw a card.");
            ability2.setBeforePayMana(new Input_NoCost_TapAbility(ability2));
            
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Magus of the Disk")) {
            SpellAbility summoningSpell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 2510163318362956239L;
                
                @Override
                public boolean canPlayAI() {
                    boolean nevinyrralInPlay = false;
                    
                    CardList inPlay = new CardList();
                    inPlay.addAll(AllZone.Computer_Play.getCards());
                    for(int i = 0; i < inPlay.size(); ++i) {
                        if(inPlay.getCard(i).getName().equals("Nevinyrral's Disk")) {
                            nevinyrralInPlay = true;
                        }
                    }
                    return !nevinyrralInPlay && (0 < CardFactoryUtil.AI_getHumanCreature(card, false).size());
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(summoningSpell);
            
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 1227443034730254929L;
                
                public void execute() {
                    card.tap();
                }
            });
            final SpellAbility ability = new Ability_Tap(card, "1") {
                private static final long serialVersionUID = -4871606824998622131L;
                
                @Override
                public void resolve() {
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = filter(all);
                    
                    for(int i = 0; i < all.size(); i++)
                        AllZone.GameAction.destroy(all.get(i));
                }
                
                private CardList filter(CardList list) {
                    return list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() || c.isCreature() || c.isEnchantment();
                        }
                    });
                }//filter()
                
                @Override
                public boolean canPlayAI() {
                    CardList human = new CardList(AllZone.Human_Play.getCards());
                    CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    human = human.getType("Creature");
                    computer = computer.getType("Creature");
                    
                    //the computer will at least destroy 2 more human creatures
                    return computer.size() < human.size() - 1 || AllZone.ComputerPlayer.getLife() < 7;
                }
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("1, tap: Destroy all artifacts, creatures, and enchantments.");
            ability.setStackDescription("Destroy all artifacts, creatures, and enchantments.");
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Stern Judge")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 3059547795996737707L;
                
                @Override
                public void resolve() {
                    AllZone.HumanPlayer.loseLife(countSwamps(AllZone.HumanPlayer),card);
                    AllZone.ComputerPlayer.loseLife(countSwamps(AllZone.ComputerPlayer),card);
                }
                
                int countSwamps(Player player) {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    CardList swamps = new CardList(play.getCards());
                    swamps = swamps.getType("Swamp");
                    return swamps.size();
                }
                
                @Override
                public boolean canPlayAI() {
                    int computer = countSwamps(AllZone.ComputerPlayer);
                    int human = countSwamps(AllZone.HumanPlayer);
                    
                    if((computer >= AllZone.ComputerPlayer.getLife()) || (human == 0)) return false;
                    
                    return computer <= human;
                }
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("tap: Each player loses 1 life for each Swamp he or she controls.");
            ability.setStackDescription("Stern Judge - Each player loses 1 life for each Swamp he or she controls");
            ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
        }//*************** END ************ END **************************
  
        
        //*************** START *********** START **************************
        else if(cardName.equals("Elvish Hunter")) {
            final SpellAbility ability = new Ability_Tap(card, "1 G") {
                private static final long serialVersionUID = -560200335562416099L;
                
                @Override
                public boolean canPlayAI() {
                    if(CardFactoryUtil.AI_doesCreatureAttack(card)) return false;
                    
                    return (getCreature().size() != 0);
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature()
                                    && (!c.getKeyword().contains(
                                            "This card doesn't untap during your next untap step."))
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
        else if(cardName.equals("Scarland Thrinax")) {
            
            final SpellAbility a2 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        //AllZone.getZone(c).remove(c);
                        AllZone.GameAction.sacrifice(c);
                        
                        if(AllZone.GameAction.isCardInPlay(card)) card.addCounter(Counters.P1P1, 1);
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    //this is a hack, check the stack to see if this card has an ability on the stack
                    //if so, we can't use the ability: this is to prevent using a limited ability too many times
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    if(super.canPlay()) return true;
                    return false;
                }
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 8445133749305465286L;
                
                @Override
                public void showMessage() {
                    CardList creats = new CardList(
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                    creats = creats.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2, creats,
                            "Select a creature to sacrifice.", false, false));
                }
            };
            
            card.addSpellAbility(a2);
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Sacrifice a creature: Put a +1/+1 counter on ").append(card.getName()).append(".");
            a2.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card.getName()).append(" gets a +1/+1 counter.");
            a2.setStackDescription(sbStack.toString());
            
            a2.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Cartographer")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -3887243972980889087L;
                
                public void execute() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList lands = new CardList(grave.getCards());
                    lands = lands.getType("Land");
                    
                    Player controller = card.getController();
                    
                    if(lands.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target land", lands.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } else //computer
                    {
                        lands.shuffle();
                        ability.setTargetCard(lands.get(0));
                        AllZone.Stack.add(ability);
                    }
                    
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Feral Hydra")) { 

            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -11489323319L;
                
                @Override
                public boolean canPlayAI() {
                    return super.canPlay() && 4 <= ComputerUtil.getAvailableMana().size() - CardUtil.getConvertedManaCost(card.getManaCost());
                }
            };
             card.clearSpellAbility();
             card.addSpellAbility(spell);
            
            final SpellAbility ability = new Ability(card, "3") {
                @Override
                public void resolve() {
                	card.addCounter(Counters.P1P1,1);
                }//resolve()
            };
            
            ability.setDescription("3: put a +1/+1 counter on Feral Hydra. Any player may activate this ability.");
            ability.setStackDescription("Feral Hydra gets a +1/+1 counter.");
            ability.getRestrictions().setAnyPlayer(true);
            card.addSpellAbility(ability);
  
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = 2559021590L;
                
                public void execute() {
                	int XCounters = card.getXManaCostPaid();
                	card.addCounter(Counters.P1P1, XCounters);

                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Shifting Wall") || cardName.equals("Maga, Traitor to Mortals")
        		|| cardName.equals("Krakilin") || cardName.equals("Ivy Elemental") || cardName.equals("Lightning Serpent")) { 
        	
        	if(!card.getName().equals("Krakilin")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -11489323313L;
                
                @Override
                public boolean canPlayAI() {
                    return super.canPlay() && 4 <= ComputerUtil.getAvailableMana().size() - CardUtil.getConvertedManaCost(card.getManaCost());
                }
            };
             card.clearSpellAbility();
             card.addSpellAbility(spell);
        	}
            
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    //PlayerLife target = AllZone.GameAction.getPlayerLife(getTargetPlayer());
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
            
            final Ability ability = new Ability(card, "1 R") {
                
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
                    card.subtractCounter(Counters.P1P1, 1); 
                }//chooseTargetAI()
                
                CardList getCreature() {
                    //toughness of 1
                    CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (1 == c.getKillDamage());
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
            Input target = new Input() {
                
                private static final long serialVersionUID = 4246601245231655L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select creature or player to target: ");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectPlayer(Player player) {
                	ability.setTargetPlayer(player);
                	card.subtractCounter(Counters.P1P1, 1);
                    AllZone.Stack.add(ability);
                    stopSetNext(new ComputerAI_StackNotEmpty());	
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    PlayerZone Cplay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    if(AllZone.GameAction.isCardInZone(c, Hplay) || AllZone.GameAction.isCardInZone(c, Cplay)) {
                    	card.subtractCounter(Counters.P1P1, 1);
                    	ability.setTargetCard(c);
                        AllZone.Stack.add(ability);
                        stopSetNext(new ComputerAI_StackNotEmpty());
                    }
                }//selectCard()
            };//Input
            card.addSpellAbility(ability);
            ability.setAfterPayMana(target);
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
        
        /*keyworded
        //*************** START *********** START **************************
        else if(cardName.equals("Mindless Automaton")) {      
            
            final Ability ability1 = new Ability(card, "1") {
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
                private static final long serialVersionUID = -4312210760057471033L;
                
                @Override
                public void showMessage() {
                    stopSetNext(CardFactoryUtil.input_discard(ability1, 1));
                    
                }
            };
            
            ability1.setStackDescription(cardName + " gets a +1/+1 counter. ");
            ability1.setDescription("1, Discard a card: put a +1/+1 counter on Mindless Automaton.");
            card.addSpellAbility(ability1);
            ability1.setBeforePayMana(runtime1);
            
        	final SpellAbility ability2 = new Ability(card, "0") {
        		@Override
        		public void resolve() {
        			final String player = card.getController();
        			AllZone.GameAction.drawCards(player, 1);
        			card.subtractCounter(Counters.P1P1, 2);
        		}   
        	};
        	
            ability2.setStackDescription("Remove two +1/+1 counters: Draw a card.");
            ability2.setDescription("Remove two +1/+1 counters from Mindless Automaton: Draw a card.");
            card.addSpellAbility(ability2);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Molten Hydra")) {
            
            final Ability ability1 = new Ability(card, "1 R R") {
                
                @Override
                public void resolve() {
                	card.addCounter(Counters.P1P1,1);
                }//resolve()
                
            };//SpellAbility
            
            ability1.setDescription("1 R R: put a +1/+1 counter on Molten Hydra. ");
            ability1.setStackDescription("Molten Hydra gets a +1/+1 counter. ");
            card.addSpellAbility(ability1);
        	
            final Ability_Tap ability2 = new Ability_Tap(card,"0") {
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
             //   card.subtractCounter(Counters.P1P1, total); 
                }//chooseTargetAI()
                
                CardList getCreature() {

                    //toughness of 1
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card.getCounters(Counters.P1P1), card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                        	int total = card.sumAllCounters();
                            return (total >= c.getKillDamage());
                        }
                    });
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                	int total = card.sumAllCounters();
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(total,
                                card);
                    } else getTargetPlayer().addDamage(total, card);
                   card.subtractCounter(Counters.P1P1,total);
                }//resolve()
            };//SpellAbility
            
        Input target = new Input() {
                
                private static final long serialVersionUID = 4246601245231656L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select creature or player to target: ");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectPlayer(Player player) {
                	ability2.setTargetPlayer(player);
                    AllZone.Stack.add(ability2);
                    stopSetNext(new ComputerAI_StackNotEmpty());
                	//card.subtractCounter(Counters.P1P1, total);
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    PlayerZone Cplay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    if(AllZone.GameAction.isCardInZone(c, Hplay) || AllZone.GameAction.isCardInZone(c, Cplay)) {
                    	ability2.setTargetCard(c);
                        AllZone.Stack.add(ability2);
                        stopSetNext(new ComputerAI_StackNotEmpty());
                    	//card.subtractCounter(Counters.P1P1, total);
                    }
                }//selectCard()
            };//Input

            card.addSpellAbility(ability2);
            ability2.setBeforePayMana(target);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Tap: Remove all +1/+1 counters and deal damage equal to number of counters ");
            sb.append("removed to target creature or player.");
            ability2.setDescription(sb.toString());
            
            ability2.setStackDescription("Molten Hydra deals damage to number of counters on it to target creature or player.");
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Auramancer") || cardName.equals("Monk Idealist")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = 25590819729244894L;
                
                public void execute() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList enchantments = new CardList(grave.getCards());
                    enchantments = enchantments.getType("Enchantment");
                    
                    Player controller = card.getController();
                    
                    if(enchantments.size() == 0) return;
                    
                    if(controller.equals(AllZone.HumanPlayer)) {
                        Object o = AllZone.Display.getChoiceOptional("Select target enchantment",
                                enchantments.toArray());
                        if(o != null) {
                            ability.setTargetCard((Card) o);
                            AllZone.Stack.add(ability);
                        }
                    } 
                    else{ //computer
                        enchantments.shuffle();
                        ability.setTargetCard(enchantments.get(0));
                        AllZone.Stack.add(ability);
                    }
                }//execute()
            };//Command
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Merfolk Sovereign")) {
            final Ability_Tap ability = new Ability_Tap(card, "0") {
                private static final long serialVersionUID = -4663016921034366082L;
                
                @Override
                public boolean canPlayAI() {
                    //return getMerfolk().size() != 0; 
                    if(getMerfolk().size() > 0) {
                        CardList merfolk = getMerfolk();
                        merfolk.shuffle();
                        setTargetCard(merfolk.get(0));
                        return true;
                    }
                    return false;
                }
                
                CardList getMerfolk() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList merfolk = new CardList(play.getCards());
                    merfolk = merfolk.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (c.getType().contains("Merfolk") || c.getKeyword().contains("Changeling"))
                                    && CardFactoryUtil.AI_doesCreatureAttack(c) && !c.equals(card)
                                    && !c.getKeyword().contains("Unblockable");
                        }
                    });
                    return merfolk;
                }//getFlying()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        getTargetCard().addExtrinsicKeyword("Unblockable");
                    }
                    
                    AllZone.EndOfTurn.addUntil(new Command() {
                        private static final long serialVersionUID = -1884018112259809603L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(getTargetCard())) getTargetCard().removeExtrinsicKeyword(
                                    "Unblockable");
                        }
                    });
                }//resolve()
            };//SpellAbility
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = 4512556936796509819L;
                
                @Override
                public void showMessage() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (c.getType().contains("Merfolk") || c.getKeyword().contains("Changeling"))
                                    && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list,
                            "Select target Merfolk creature", Command.Blank, true, false));
                }//showMessage()
            };//Input
            
            card.addSpellAbility(ability);
            ability.setDescription("Tap: Target Merfolk creature is unblockable this turn.");
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Dwarven Pony")) {
            final Ability_Tap ability = new Ability_Tap(card, "1 R") {
                private static final long serialVersionUID = 2626619319289064288L;
                
                @Override
                public boolean canPlayAI() {
                    return getDwarves().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    AllZone.GameAction.sacrifice(card);
                    
                    CardList dwarves = getDwarves();
                    dwarves.shuffle();
                    setTargetCard(dwarves.get(0));
                }
                
                CardList getDwarves() {
                    PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    CardList mountains = new CardList(hplay.getCards());
                    mountains = mountains.getType("Mountain");
                    if(mountains.size() == 0) return mountains;
                    

                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    CardList dwarves = new CardList(play.getCards());
                    dwarves = dwarves.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.isType("Dwarf")
                                    && CardFactoryUtil.AI_doesCreatureAttack(c) && !c.equals(card)
                                    && !c.getKeyword().contains("Mountainwalk");
                        }
                    });
                    return dwarves;
                }//getFlying()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        getTargetCard().addExtrinsicKeyword("Mountainwalk");
                    }
                    
                    AllZone.EndOfTurn.addUntil(new Command() {
                        
                        private static final long serialVersionUID = -6845643843049229106L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(getTargetCard())) getTargetCard().removeExtrinsicKeyword(
                                    "Mountainwalk");
                        }
                    });
                }//resolve()
            };//SpellAbility
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = -2962059144349469134L;
                
                @Override
                public void showMessage() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.isType("Dwarf")
                                    && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list,
                            "Select target Dwarf creature", Command.Blank, true, false));
                }//showMessage()
            };//Input
            
            card.addSpellAbility(ability);
            ability.setDescription("1 R, Tap: Target Dwarf creature gains mountainwalk until end of turn.");
            ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
     
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Sliptide Serpent")) {
            final SpellAbility a1 = new Ability(card, "3 U") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    //AllZone.getZone(card).remove(card);
                    //hand.add(card);
                    
                    if(card.isToken()) AllZone.getZone(card).remove(card);
                    else AllZone.GameAction.moveTo(hand, card);
                    
                }
            };//a1
            
            //card.clearSpellAbility();
            card.addSpellAbility(a1);
            
            StringBuilder sb1 = new StringBuilder();
            sb1.append(card.getController()).append(" returns Sliptide Serpent back to its owner's hand.");
            a1.setStackDescription(sb1.toString());
            
            a1.setDescription("3 U: Return Sliptide Serpent to its owner's hand.");
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Fleeting Image")) {
            final SpellAbility a1 = new Ability(card, "1 U") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getOwner());
                    
                    //AllZone.getZone(card).remove(card);
                    //hand.add(card);
                    
                    if(card.isToken()) AllZone.getZone(card).remove(card);
                    else AllZone.GameAction.moveTo(hand, card);
                    
                }
            };//a1
            
            //card.clearSpellAbility();
            card.addSpellAbility(a1);
            
            StringBuilder sb1 = new StringBuilder();
            sb1.append(card.getController()).append(" returns Fleeting Image back to its owner's hand.");
            a1.setStackDescription(sb1.toString());
            
            a1.setDescription("1 U: Return Fleeting Image to its owner's hand.");
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Windreaver")) {
            final SpellAbility a1 = new Ability(card, "U") {
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
            
            //card.clearSpellAbility();
            card.addSpellAbility(a1);
            
            StringBuilder sb1 = new StringBuilder();
            sb1.append(card.getController()).append(" returns Windreaver back to its owner's hand.");
            a1.setStackDescription(sb1.toString());
            
            a1.setDescription("U: return Windreaver to its owner's hand.");
            
            final SpellAbility a2 = new Ability(card, "U") {
                
                @Override
                public void resolve() {

                	
                    final Command EOT = new Command() {
                        private static final long serialVersionUID = 6437163765161964445L;
                        
                        public void execute() {
                      	  
                            int power = card.getNetAttack();
                            int tough = card.getNetDefense();
                            card.addTempAttackBoost(tough - power);
                            card.addTempDefenseBoost(power - tough);
                            
                      	  }
                        
                        };

                        if(AllZone.GameAction.isCardInPlay(card)) {
                            int power = card.getNetAttack();
                            int tough = card.getNetDefense();
                            card.addTempAttackBoost(tough - power);
                            card.addTempDefenseBoost(power - tough);
                            AllZone.EndOfTurn.addUntil(EOT);
                        }
                	
                }
            };//a2

            card.addSpellAbility(a2);
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getController()).append(" switches Windreaver's power and toughness until EOT.");
            a2.setStackDescription(sb2.toString());
            
            a2.setDescription("U: Switch Windreaver's power and toughness until end of turn. ");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Academy Rector")) {
            final Command destroy = new Command() {
                private static final long serialVersionUID = -4352349741511065318L;
                
                public void execute() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        String[] choices = {"Yes", "No"};
                        Object q = null;
                        
                        q = AllZone.Display.getChoiceOptional("Exile " + card.getName() + "?", choices);
                        
                        if(q == null || q.equals("No")) ;
                        else {
                            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
                            CardList list = new CardList(lib.getCards());
                            list = list.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.isEnchantment();
                                }
                            });
                            
                            if(list.size() > 0) {
                                Object o = AllZone.Display.getChoiceOptional(
                                        "Choose enchantment card to put onto the battlefield", list.toArray());
                                if(o != null) {
                                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                                    PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play,
                                            AllZone.ComputerPlayer);
                                    Card c = (Card) o;
                                    lib.remove(c);
                                    play.add(c);
                                    
                                    if(c.isAura()) {
                                        Object obj = null;
                                        if(c.getKeyword().contains("Enchant creature")) {
                                            CardList creats = new CardList(play.getCards());
                                            creats.addAll(oppPlay.getCards());
                                            creats = creats.getType("Creature");
                                            obj = AllZone.Display.getChoiceOptional("Pick a creature to attach "
                                                    + c.getName() + " to", creats.toArray());
                                        } else if(c.getKeyword().contains("Enchant land")
                                                || c.getKeyword().contains("Enchant land you control")) {
                                            CardList lands = new CardList(play.getCards());
                                            //lands.addAll(oppPlay.getCards());
                                            lands = lands.getType("Land");
                                            if(lands.size() > 0) obj = AllZone.Display.getChoiceOptional(
                                                    "Pick a land to attach " + c.getName() + " to",
                                                    lands.toArray());
                                        }
                                        if(obj != null) {
                                            Card target = (Card) obj;
                                            if(AllZone.GameAction.isCardInPlay(target)) {
                                                c.enchantCard(target);
                                            }
                                        }
                                    }
                                }
                            }
                            AllZone.GameAction.exile(card);
                        }
                    }//if human
                    else {
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer);
                        CardList list = new CardList(lib.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isEnchantment() && !c.isAura();
                            }
                        });
                        
                        if(list.size() > 0) {
                            PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                            Card c = CardFactoryUtil.AI_getBestEnchantment(list, card, false);
                            lib.remove(c);
                            play.add(c);
                            AllZone.GameAction.exile(card);
                            
                        }
                    }
                }
            };
            
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
                                Object o = AllZone.Display.getChoiceOptional("Select target non-land card",
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    CardList landInLib = new CardList(lib.getCards());
                    landInLib = landInLib.getType("Plains");
                    
                    if(landInLib.size() > 0) {
                        if(card.getController().equals(AllZone.ComputerPlayer)) {
                            lib.remove(landInLib.get(0));
                            landInLib.get(0).tap();
                            play.add(landInLib.get(0));
                        } else {
                            Object o = AllZone.Display.getChoiceOptional("Select plains card to put into play: ",
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
                    CardList landInPlay = new CardList(AllZone.getZone(Constant.Zone.Play,
                            AllZone.ComputerPlayer).getCards());
                    
                    landInLib = landInLib.getType("Land");
                    landInPlay = landInPlay.getType("Land");
                    
                    if(landInLib.size() > 0 && landInPlay.size() > 0) return true;
                    else return false;
                    
                }
            };//SpellAbility
            
            ab1.setStackDescription("search your library for a plains card, put it onto the battlefield tapped, then shuffle your library.");
            
            Command cip = new Command() {
                /**
			 * 
			 */
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
                        
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
        else if(cardName.equals("Assembly-Worker")) {
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
                private static final long serialVersionUID = 3561450525225198222L;
                
                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                /*
                 *  getAttacker() will now filter out non-Assembly-Worker and non-Changelings
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
            card.addSpellAbility(a2[0]);
            a2[0].setDescription("tap: Target Assembly-Worker creature gets +1/+1 until end of turn.");
            
            /*
            @SuppressWarnings("unused") // target unused
            inal Input target = new Input()
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
            */
            
            /*
             *  This input method will allow the human to select both Assembly-Workers and Changelings
             */
            Input runtime = new Input() {
				private static final long serialVersionUID = -2520339470741575052L;

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
                }
            };//Input target
            a2[0].setBeforePayMana(runtime);
            
//          a2[0].setBeforePayMana(CardFactoryUtil.input_targetType(a2[0], "Assembly-Worker"));     
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Wood Elves")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;
                    Player player = c.getController();
                    //PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
                    //life.addLife(2);
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    

                    CardList lands = new CardList(lib.getCards());
                    lands = lands.getType("Forest");
                    
                    if(player.equals(AllZone.HumanPlayer) && lands.size() > 0) {
                        Object o = AllZone.Display.getChoiceOptional("Pick a forest card to put into play",
                                lands.toArray());
                        if(o != null) {
                            Card card = (Card) o;
                            lib.remove(card);
                            AllZone.Human_Play.add(card);
                            //  card.tap();
                            lands.remove(card);
                            player.shuffle();
                        }
                    } // player equals human
                    else if(player.equals(AllZone.ComputerPlayer) && lands.size() > 0) {
                        Card card = lands.get(0);
                        lib.remove(card);
                        // hand.add(card);
                        AllZone.Computer_Play.add(card);
                        //   card.tap();
                        lands.remove(card);
                        player.shuffle();
                    }
                }
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 1832932499373431651L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" searches his library for a Forest card to put that card into play.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Ambassador Laquatus")) {
            final SpellAbility a1 = new Ability(card, "3") {
                
                @Override
                public void resolve() {
                    getTargetPlayer().mill(3);                    
                }
                
                @Override
                public boolean canPlayAI() {
                    Player player = getTargetPlayer();
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
        else if(cardName.equals("Cathartic Adept")) {
            final Ability_Tap a1 = new Ability_Tap(card) {
                private static final long serialVersionUID = 2359247592519063187L;
                
                @Override
                public void resolve() {
                    getTargetPlayer().mill(1);
                }
                
                @Override
                public boolean canPlayAI() {
                    Player player = getTargetPlayer();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    CardList libList = new CardList(lib.getCards());
                    return libList.size() > 0;
                }
            };//SpellAbility
            
            //not sure why, but this card doesn't seem to want to tap:
            final Command tap = new Command() {
                private static final long serialVersionUID = -6290276896549170403L;
                
                public void execute() {
                    card.tap();
                }
            };
            
            a1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            a1.setDescription("tap: Target player puts the top card of his or her library into his or her graveyard.");
            a1.setStackDescription("Player puts the top card of his or her library into his or her graveyard");
            //a1.setBeforePayMana(new Input_PayManaCost(a1));
            a1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(a1, tap));
            card.addSpellAbility(a1);
        }//*************** END ************ END **************************
        */

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
        else if(cardName.equals("Phantom Nishoba")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(final int n, Card source) {
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
            
            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = -2570661526160966399L;
                
                public void execute() {
                    newCard.addCounter(Counters.P1P1, 7);
                }
            };//Command
            
            newCard.addComesIntoPlayCommand(comesIntoPlay);
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Phantom Centaur")) {
            final Card newCard = new Card() {
                @Override
                public void addDamage(final int n, Card source) {
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
            
            Command comesIntoPlay = new Command() {
                
                private static final long serialVersionUID = 4217898403350036317L;
                
                public void execute() {
                    newCard.addCounter(Counters.P1P1, 3);
                }
            };//Command
            
            newCard.addComesIntoPlayCommand(comesIntoPlay);
            
            newCard.setSVars(card.getSVars());
            
            return newCard;
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Chainer, Dementia Master")) {
            final Ability ability = new Ability(card, "B B B") {
                @Override
                public void resolve() {
                    card.getController().loseLife(3,card);
                    
                    PlayerZone hGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
                    PlayerZone cGrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                    PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    
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
                        			return;//*
                        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, getSourceCard().getController());
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
                            }// */
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
            final SpellAbility ability = new Ability_Tap(card, "U B") {
                private static final long serialVersionUID = -3880035465617987801L;
                
                @Override
                public void resolve() {
                	Player opponent = card.getController().getOpponent();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
                    opponent.loseLife(1,card);
                    Card[] handLook = hand.getCards();
                    if(opponent.equals(AllZone.ComputerPlayer)) {
                        AllZone.Display.getChoice("Look", handLook);
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
            card.addSpellAbility(ability);
            ability.setDescription("U B, tap: Target player loses 1 life and reveals his or her hand.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Opponent loses 1 life.");
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
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lichenthrope")) {
            /*
        	final Card newCard = new Card() {
            	final Card crd = this;
            	@Override
                public void addDamage(final int n, final Card source) {
                    final Ability ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            crd.addCounter(Counters.M1M1, n);
                        }
                    };
                    ability.setStackDescription(card.getName() + " - gets " + n + " -1/-1 counters.");
                    AllZone.Stack.add(ability);
                }
            };
            */
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
                    ability.setStackDescription("Kinsbaile Borderguard comes into play with a +1/+1 counter on it for each other Kithkin you control.");
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
                    		"Kithkin Soldier creature token into play for each counter on it.");
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
                    PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
                    PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                    
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
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
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Avenger of Zendikar")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    CardList land = new CardList(play.getCards());
                    land = land.getType("Land");
                    for(int i = 0; i < land.size(); i++)
                        CardFactoryUtil.makeToken("Plant", "G 0 1 Plant", card, "G", new String[] {
                                "Creature", "Plant"}, 0, 1, new String[] {""});
                }
            };
            ability.setStackDescription("When Avenger of Zendikar enters the battlefield, put a 0/1 green Plant creature token onto the battlefield for each land you control.");
            
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 4245563898487609274L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        */
        
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
            
            final SpellAbility ab = new Ability_Tap(card, "1 W") {
                private static final long serialVersionUID = -4952768517408793535L;
                
                @Override
                public boolean canPlayAI() {
                    return getEquipment().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    Card target = CardFactoryUtil.AI_getBestArtifact(getEquipment());
                    setTargetCard(target);
                }
                
                CardList getEquipment() {
                    CardList list = new CardList(AllZone.Computer_Hand.getCards());
                    list = list.getType("Equipment");
                    return list;
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(c, hand)) {
                        hand.remove(c);
                        play.add(c);
                    }
                }
            };
            
            ab.setBeforePayMana(new Input() {
                
                private static final long serialVersionUID = -5107440034982095276L;
                
                @Override
                public void showMessage() {
                	Player controller = card.getController();
                    CardList eq = new CardList(AllZone.getZone(Constant.Zone.Hand, controller).getCards());
                    eq = eq.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            PlayerZone zone = AllZone.getZone(c);
                            return c.isEquipment() && zone.is(Constant.Zone.Hand);
                        }
                    });
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ab, eq, "Select an equipment card", false,
                            false));
                }
            });
            card.addSpellAbility(ab);
            ab.setDescription("1 W, tap: You may put an Equipment card from your hand onto the battlefield.");
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Wolfbriar Elemental"))
        {
        	final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    for (int i=0;i<card.getMultiKickerMagnitude();i++) {
                    	 CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", card, "G", new String[] {"Creature", "Wolf"},
                                 2, 2, new String[] {""});
                    }
                    card.setMultiKickerMagnitude(0);
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append("When ");
            sb.append(cardName);
            sb.append(" enters the battlefield, put a 2/2 green Wolf creature token onto the battlefield for each time it was kicked.");
            ability.setStackDescription(sb.toString());
            
            final Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = -4362419545718133008L;

				public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        */
        
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
        
        /*
        //****************************START*****************
        else if(cardName.equals("Ley Druid") || cardName.equals("Juniper Order Druid") || cardName.equals("Stone-Seeder Hierophant")) {
           final Ability_Tap ability = new Ability_Tap(card) {
			private static final long serialVersionUID = -7229312351481710270L;
			public boolean canPlayAI() {
                 return false;
              }
              public void resolve() {
                 if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
                    getTargetCard().untap();  //untapping land
                 }
              }
           };//Ability_Tap
           
           Input target = new Input() {
			private static final long serialVersionUID = -9065927577683004322L;
			public void showMessage() {
                 AllZone.Display.showMessage("Select target tapped land to untap");
                 ButtonUtil.enableOnlyCancel();
              }
              public void selectButtonCancel() {stop();}
              public void selectCard(Card c, PlayerZone zone) {
                 if(c.isLand() && zone.is(Constant.Zone.Play) && c.isTapped()) {
                    card.tap(); //tapping Ley Druid
                    ability.setTargetCard(c);
                    AllZone.Stack.add(ability);
                    stop();
                 }
              }//selectCard()
           };//Input
           
           card.addSpellAbility(ability);
           ability.setDescription("tap: Untap target land.");
           ability.setBeforePayMana(target);
        }//end Ley Druid
        //**************END****************END***********************
        */
        
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
        	final Ability_Tap ability = new Ability_Tap(card, "0") {
                /**
				 * 
				 */
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
            ability.setDescription("tap: Put a 3/3 green Elephant creature token onto the battlefield.(LEVEL 2-5)");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - Put a 3/3 green Elephant creature token onto the battlefield.");
            ability.setStackDescription(sb.toString());
            
            final Ability_Tap ability2 = new Ability_Tap(card, "0") {

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
            ability2.setDescription("tap: Put two 3/3 green Elephant creature tokens onto the battlefield.(LEVEL 6+)");
            
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
        			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
        			if( player.equals(AllZone.HumanPlayer)) {
        				if(graveList.size() > 0) {
        					for(int i = 0; i < 2; i++) {
        						Card c = AllZone.Display.getChoiceOptional("Select creature", graveList.toArray());
        						if(c == null) break;
        						AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Play, player), c);
        						graveList.remove(c);
        					}
        				}
        			}
        			else{ //computer
        				if(graveList.size() > 0) {
        					int end = graveList.size();
        					graveList.shuffle();
        					for(int i=0;i<end;i++) {
        						AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Play, player), graveList.get(i));
        					}
        				}
        			}
        		}//resolve()
        	};//SpellAbility
        	Command leavesPlay = new Command() {
				private static final long serialVersionUID = -2495216861720523362L;

				public void execute() {
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - return up to 2 creatures with power < 2 from graveyard to play.");
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
                 
                 PlayerZone play = AllZone.getZone(Constant.Zone.Play,player);
                 CardList choice = new CardList(play.getCards());
                 choice  = choice.getType("Land");
                 
                 if (choice.size() > 0)
                 {
	                 if (player.equals(AllZone.HumanPlayer))
		                 AllZone.InputControl.setInput(CardFactoryUtil.input_sacrificePermanent(ability, choice, "Select a land to sacrifice"));
	                 else //compy
	                 {
	                	 //AllZone.GameAction.sacrificePermanent(AllZone.ComputerPlayer, ability, choice);
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

        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Death Cultist")) {
        	
        	// Sacrifice Death Cultist: Target player loses 1 life and you gain 1 life.
        	 
        	final SpellAbility ability = new Ability(card, "0") {
        		
        		@Override
        		public boolean canPlayAI() {
        			//PlayerLife human = AllZone.GameAction.getPlayerLife(AllZone.HumanPlayer);
        			return AllZone.HumanPlayer.getLife() == 1;
        		}

        		@Override
        		public void resolve() {
        			final Player target = getTargetPlayer();
        			
        			AllZone.GameAction.sacrifice(card);
        			
        			target.loseLife(1,card);
        			card.getController().gainLife(1, card);   			
        		}
        	};

        	//ability.setStackDescription(cardName + " - Target player loses 1 life and controller gains 1 life.");
        	card.addSpellAbility(ability);
        	ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
        }//*************** END ************ END **************************
        */
        
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
        	sb.append(cardName).append(" - comes into play with a +1/+1 counter on it for each other spell played this turn.");
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
        	final Ability_Tap ability = new Ability_Tap(card, "0") {
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
        	//ability.setStackDescription(cardName + " - Rearrange the top X cards in your library in any order.");
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
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Kor Line-Slinger")) {
        	final Ability_Tap ability = new Ability_Tap(card) {
				private static final long serialVersionUID = -5883773208646266056L;
				
				@Override
				public boolean canPlayAI() {
        			CardList list = new CardList(AllZone.Human_Play.getCards());
        			list = list.filter(new CardListFilter() {
        				public boolean addCard(Card c) {
        					return c.isUntapped() &&c.isCreature() && 
        						c.getNetAttack() <= 3 && CardFactoryUtil.canTarget(card, c);
        				}
        			});
        			if (list.isEmpty()) return false;

        			CardListUtil.sortAttack(list);
        			CardListUtil.sortFlying(list);
        			setTargetCard(list.get(0));
        			return true;
        		}//canPlayAI()
				
				@Override
        		public void resolve() {
					Card c = getTargetCard();
        			if(AllZone.GameAction.isCardInPlay(c) && c.isUntapped()) {
        				c.tap();
        			}
        		}//resolve
        	};//SpellAbility

        	Input target = new Input() {
				private static final long serialVersionUID = 5727787884951469579L;
				@Override
				public void showMessage() {
        			AllZone.Display.showMessage("Select target Creature to tap");
        			ButtonUtil.enableOnlyCancel();
        		}
				@Override
        		public void selectButtonCancel() {
        			stop();
        		}
        		@Override
        		public void selectCard(Card c, PlayerZone zone) {
        			if(zone.is(Constant.Zone.Play) && c.isUntapped() && 
        					c.isCreature() && (c.getNetAttack() <= 3)) {
        				ability.setTargetCard(c);
        				card.tap();
        				 AllZone.Stack.add(ability);
        				 stop();
        			}
        		}
        	};//input
        	card.addSpellAbility(ability);
        	ability.setBeforePayMana(target);
        }//*************** END ************ END **************************
        */
        
        
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
        	final Ability_Tap ability = new Ability_Tap(card, "2 B")
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
					return humanList.size()>0;
				}
        	};
        	card.addSpellAbility(ability);
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append("2 B, tap: Exile target creature card from a graveyard. ");
        	sbDesc.append("Put a 2/2 black Zombie creature token onto the battlefield.");
        	ability.setDescription(sbDesc.toString());
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(card).append("Exile target creature card from a graveyard. ");
        	sbStack.append("Put a 2/2 black Zombie creature token onto the battlefield.");
        	ability.setStackDescription(sbStack.toString());
    	}//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Rubinia Soulsinger")) {
        	/*
        	 * Tap: Gain control of target creature for as long as you
        	 * control Rubinia and Rubinia remains tapped.
        	 *
        	final Card movedCreature[] = new Card[1];
            final Ability_Tap ability = new Ability_Tap(card, "0") {
				private static final long serialVersionUID = 7018915669688488647L;
				@Override
				public boolean canPlay() {
					//need to check if there are other creatures in play
					return AllZone.GameAction.isCardInPlay(card) && !card.hasSickness() && super.canPlay();
				}
				@Override
				public boolean canPlayAI() {
					CardList human = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
					human = human.filter(new CardListFilter() {
						public boolean addCard(Card c) {
							return CardFactoryUtil.canTarget(card, getTargetCard());
						}
					});
					return human.size() > 0;
				}
				@Override
                public void resolve() {
                    Card c = getTargetCard();
                    movedCreature[0] = c;
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        //set summoning sickness
                        if(c.getKeyword().contains("Haste")) {
                            c.setSickness(false);
                        } else {
                            c.setSickness(true);
                        }
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        c.setSickness(true);
                        c.setController(card.getController());
                        
                        PlayerZone from = AllZone.getZone(c);
                        from.remove(c);
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
                        to.add(c);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                    }
                }//resolve()
            };//SpellAbility
            
            final Command untapLeavesPlay = new Command() {
				private static final long serialVersionUID = 2783051953965817611L;

				public void execute() {
                    Card c = movedCreature[0];
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        c.setSickness(true);
                        c.setController(c.getController().getOpponent());
                        
                        PlayerZone from = AllZone.getZone(c);
                        from.remove(c);
                        
                        //make sure the creature is removed from combat:
                        CardList list = new CardList(AllZone.Combat.getAttackers());
                        if(list.contains(c)) AllZone.Combat.removeFromCombat(c);
                        
                        CardList pwlist = new CardList(AllZone.pwCombat.getAttackers());
                        if(pwlist.contains(c)) AllZone.pwCombat.removeFromCombat(c);
                        
                        PlayerZone to = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                        to.add(c);
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                    }//if
                }//execute()
            };//Command
            card.addUntapCommand(untapLeavesPlay);
            card.addLeavesPlayCommand(untapLeavesPlay);
            card.addChangeControllerCommand(untapLeavesPlay);
            
            card.addSpellAbility(ability);            
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
        }//*************** END ************ END **************************
        */
        
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
        
        
        //*************** START *********** START ************************
        else if(cardName.equals("Lord of the Undead")) {
            final Ability_Tap ability = new Ability_Tap(card, "1 B") {
                
				private static final long serialVersionUID = -4287216165943846367L;

				@Override
                public boolean canPlayAI() {
                    return getGraveCreatures().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList grave = getGraveCreatures();
                    Card target = CardFactoryUtil.AI_getBestCreature(grave);
                    setTargetCard(target);
                }
                
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Card c = AllZone.Display.getChoice("Select card", getGraveCreatures().toArray());
                        setTargetCard(c);
                    }
                    
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) AllZone.GameAction.moveTo(hand,
                            getTargetCard());
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    return super.canPlay() && getGraveCreatures().size() != 0;
                }
                
                CardList getGraveCreatures() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList list = new CardList(grave.getCards());
                    list = list.filter(new CardListFilter(){
                    	public boolean addCard(Card c)
                    	{
                    		return c.getType().contains("Zombie") || c.getKeyword().contains("Changeling");
                    	}
                    });
                    return list;
                }
            };//SpellAbility
            ability.setDescription("1 B, Tap: Return target Zombie card from your graveyard to your hand.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - Return target Zombie card from your graveyard to your hand.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
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
                	sb.append("Symbiotic Wurm - ").append(card.getController()).append(" puts seven 1/1 tokens into play");
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
        else if(cardName.equals("Pestilence Demon")) {
            final SpellAbility ability = new Ability(card, "B") {
            	
                @Override
                public boolean canPlayAI() {
                    CardList human = new CardList(AllZone.Human_Play.getCards());
                    CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    human = human.getType("Creature");
                    computer = computer.getType("Creature");
                    
                    return AllZone.ComputerPlayer.getLife() > 2 
                    			&& !(human.size() == 0 
                    			&& 0 < computer.size()) 
                    			&& card.getKillDamage() > 1;
                }
                
                @Override
                public void resolve() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(1, card);
                    }
                    
                    AllZone.HumanPlayer.addDamage(1, card);
                    AllZone.ComputerPlayer.addDamage(1, card);
                }//resolve()
            };//SpellAbility
            ability.setDescription("B: Pestilence Demon deals 1 damage to each creature and each player.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" deals 1 damage to each creature and each player.");
            ability.setStackDescription(sb.toString());
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
				private static final long serialVersionUID = -9008807568695047980L;

				@Override
                public boolean canPlayAI() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    return 0 < list.size();
                }
            });
            
            card.addSpellAbility(ability);
            
            card.setSVar("PlayMain1", "TRUE");
        }//*************** END ************ END **************************
        
        else if(cardName.equals("Thrashing Wumpus")) {
            final SpellAbility ability = new Ability(card, "B") {
            	
                @Override
                public boolean canPlayAI() {
                    CardList human = new CardList(AllZone.Human_Play.getCards());
                    CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    human = human.getType("Creature");
                    computer = computer.getType("Creature");
                    
                    return AllZone.ComputerPlayer.getLife() > 2 
                    			&& !(human.size() == 0 
                    			&& 0 < computer.size()) 
                    			&& card.getKillDamage() > 1;
                }
                
                @Override
                public void resolve() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(1, card);
                    }
                    
                    AllZone.HumanPlayer.addDamage(1, card);
                    AllZone.ComputerPlayer.addDamage(1, card);
                }//resolve()
            };//SpellAbility
            ability.setDescription("B: Thrashing Wumpus deals 1 damage to each creature and each player.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" deals 1 damage to each creature and each player.");
            ability.setStackDescription(sb.toString());
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
				private static final long serialVersionUID = -9008807568695047980L;

				@Override
                public boolean canPlayAI() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    return 0 < list.size();
                }
            });
            
            card.addSpellAbility(ability);
            
            card.setSVar("PlayMain1", "TRUE");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Ifh-Biff Efreet")) {
            final SpellAbility ability = new Ability(card, "G") {
            	
                @Override
                public boolean canPlayAI() {
					//todo(sol) setting up some AI for next go through
					//CardList human = new CardList(AllZone.Human_Play.getCards());
					//CardList computer = new CardList(AllZone.Computer_Play.getCards());
					  
					//human = human.getType("Creature").getKeyword("Flying");
					//computer = computer.getType("Creature").getKeyword("Flying");
                	
					//int compLife = AllZone.ComputerPlayer.getLife();
					//int humanLife = AllZone.HumanPlayer.getLife();
					
					// if complife > humanLife && humanlife <= available green mana, try to kill human
					
                	if (card.getController().equals(AllZone.ComputerPlayer)){
                		// needs to be careful activating ability if human has green mana available
                	}
                	else{
                		// should try to kill human's flyers but spare own
                		return true;
                	}

                	return false;
                }
                
                @Override
                public void resolve() {
                    //get all creatures
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    list = list.getKeyword("Flying");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(1, card);
                    }
                    
                    AllZone.HumanPlayer.addDamage(1, card);
                    AllZone.ComputerPlayer.addDamage(1, card);
                }//resolve()
            };//SpellAbility
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("G: Ifh-Biff Efreet deals 1 damage to each creature with ");
            sbDesc.append("flying and each player. Any player may activate this ability");
            ability.setDescription(sbDesc.toString());
            
            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append(" deals 1 damage to each flying creature and each player.");
            ability.setStackDescription(sbStack.toString());
            
            ability.getRestrictions().setAnyPlayer(true);
            card.addSpellAbility(ability);
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
        else if (cardName.equals("Yavimaya Elder"))
        {
    	        final SpellAbility ability = new Ability(card, "2") {
    	            @Override
    	            public boolean canPlay() {
    	                return AllZone.GameAction.isCardInPlay(card)
    	                        && !AllZone.Stack.getSourceCards().contains(card);//in play and not already activated(Sac cost problems)
    	            }
    	            
    	            @Override
    	            public boolean canPlayAI() {
    	                return (AllZone.Computer_Hand.size() < 3) && (AllZone.Computer_Library.size() > 0)
    	                        && MyRandom.random.nextBoolean();
    	            }
    	            
    	            @Override
    	            public void resolve() {
    	                //if (card.getController().equals(AllZone.ComputerPlayer))
    	            	//for now, sac happens during resolution:
    	                AllZone.GameAction.sacrifice(getSourceCard());
    	                card.getController().drawCard();
    	            }
    	        };
    	        
    	        StringBuilder sbDesc = new StringBuilder();
    	        sbDesc.append("2, Sacrifice ").append(card.getName()).append(": Draw a card.");
    	        ability.setDescription(sbDesc.toString());
    	        
    	        StringBuilder sbStack = new StringBuilder();
    	        sbStack.append(card.getName()).append(" - Draw a card.");
    	        ability.setStackDescription(sbStack.toString());
    	        
    	        final Command destroy = new Command()
    	        {
					private static final long serialVersionUID = -5552202665064265632L;

					public void execute()
    	        	{
    	        		AllZone.GameAction.searchLibraryTwoBasicLand(card.getController(), Constant.Zone.Hand, false, Constant.Zone.Hand, false);
    	        	}
    	        };
    	        
    	        /*
    	        Input runtime = new Input() {
                    
					private static final long serialVersionUID = -4361362367624073190L;
					boolean                   once             = true;
                    
                    @Override
                    public void showMessage() {
                        //this is necessary in order not to have a StackOverflowException
                        //because this updates a card, it creates a circular loop of observers
                        if(once) {
                            once = false;
                            stopSetNext(new Input_PayManaCost(ability));
                            AllZone.GameAction.sacrifice(card);
                            
                            
                            //AllZone.Stack.add(ability);
                            //stop();
                        }
                    }//showMessage()
                };
    	        
                ability.setBeforePayMana(runtime);
                */
                
    	        card.addSpellAbility(ability);
    	        card.addDestroyCommand(destroy);
        	    
        }
        //*************** END ************ END **************************
       
        //*************** START *********** START **************************
        else if(cardName.equals("Gwafa Hazid, Profiteer")) {
            final Ability_Tap ability = new Ability_Tap(card, "W U") {
                private static final long serialVersionUID = 8926798567122080343L;
                
                @Override
                public void resolve() {
                    Card tgtC = getTargetCard();
                	if(AllZone.GameAction.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(card, tgtC)) {
                        tgtC.addCounter(Counters.BRIBERY, 1);
                        tgtC.getController().drawCard();
                    }//is card in play?
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());                    
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getCounters(Counters.BRIBERY) < 1 &&
                                   CardFactoryUtil.canTarget(card, c) &&
                                   c.isCreature();
                        }
                    });
                    
                    if(list.isEmpty()) return false;
                    
                    setTargetCard(list.get(CardUtil.getRandomIndex(list)));
                    return true;
                }//canPlayAI()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("W U, tap: Put a bribery counter on target creature you don't control. Its controller draws a card.");
            
            ability.setBeforePayMana(new Input()
            {
            	private static final long serialVersionUID = 141164423096887945L;
                        
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target creature you don't control");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(ability, card)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if(card.isCreature() && zone.is(Constant.Zone.Play) && zone.getPlayer().equals(AllZone.ComputerPlayer)) {
                        ability.setTargetCard(card);
                        done();
                    }
                }
                
                void done() {
                    ability.getSourceCard().tap();
                	stopSetNext(new Input_PayManaCost(ability));
                }
            });

        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
          else if(cardName.equals("Overgrown Battlement")) {
              final Ability_Mana ability = new Ability_Mana(card,"tap: add G to your mana pool for each creature with defender you control.") {
     
  				private static final long serialVersionUID = 422282090183907L;

  				@Override
                  public String mana() {
                      String res = "";
                      
                      CardList cl = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                      
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
                
                //final String player = card.getController();
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
                        
                        //System.out.println("Creats size: " + creats.size());
                        
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
                                AllZone.Computer_Play.updateObservers();
                            }
                            numCreatures[0] = count;
                        }
                        sumPower[0] = intermSumPower;
                        sumToughness[0] = intermSumToughness;
                        card.setBaseAttack(sumPower[0]);
                        card.setBaseDefense(sumToughness[0]);
                        //AllZone.Stack.add(suture);
                    }
                };
                
                //suture.setStackDescription(card.getName() + " - has power equal to the sum of the power of all exiled creatures and toughness equal to the sum of their toughness.");
                //suture.setDescription("When Sutured Ghoul enters the battlefield, exile any number of creature cards from your graveyard. Sutured Ghoul's power is equal to the total power of the exiled cards and its toughness is equal to their total toughness.");
                //card.addSpellAbility(suture);
                card.addComesIntoPlayCommand(intoPlay);
                
                //card.addSpellAbility(CardFactoryUtil.ability_Devour(card, magnitude));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Myr Galvanizer"))
        {
        	final SpellAbility ability = new Ability_Tap(card,"1")
        	{
				private static final long serialVersionUID = -2151219929263378286L;

				public void resolve()
        		{
        			CardList list = AllZoneUtil.getPlayerCardsInPlay(card.getController());
        			list = list.filter(new CardListFilter()
        			{
        				public boolean addCard(Card c)
        				{
        					return !c.equals(card) && (c.getType().contains("Myr") || c.getKeyword().contains("Changeling"));
        				}
        			});
        			
        			for (Card crd:list)
        				if (crd.isTapped())
        					crd.untap();
        		}
				public boolean canPlayAI()
				{
					return false;
				}
        	};
        	ability.setDescription("1, tap: Untap each other Myr you control.");
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(card).append(" - Untap each other Myr you control.");
        	ability.setStackDescription(sb.toString());
        	
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Voice of the Woods"))
        {
        	final Ability ability = new Ability(card, "0")
        	{
        		public void resolve()
        		{
        			CardFactoryUtil.makeToken("Elemental", "G 7 7 Elemental", getActivatingPlayer(), "G", new String[] {
                        "Creature", "Elemental"}, 7, 7, new String[] {"Trample"});
        		}
        	};
        	
        	final Ability_Cost cost = new Ability_Cost("tapXType<5/Elf>", card.getName(), true);
            ability.setPayCosts(cost);
        	
        	ability.setDescription("Tap five untapped Elves you control: Put a 7/7 green Elemental creature token with trample onto the battlefield.");
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(card).append(" - Put a 7/7 green Elemental creature token with trample onto the battlefield.");
        	ability.setStackDescription(sb.toString());
        	
        	card.addSpellAbility(ability);
	    }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Altar Golem"))
        {
        	Ability untap = new Ability(card,"0")
        	{
	        	public void resolve()
	        	{
	        		card.untap();
	        	}
        	};
        	final Ability_Cost cost = new Ability_Cost("tapXType<5/Creature>", card.getName(), true);
            untap.setPayCosts(cost);
        	
            untap.setDescription("Tap five untapped creatures you control: Untap Altar Golem.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" untaps.");
            untap.setStackDescription(sb.toString());
        	
        	card.addSpellAbility(untap);
    	}//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cerulean Sphinx")) {
        	final Ability intoLibrary = new Ability(card, "U") {
        		public void resolve() {
        			if(AllZoneUtil.isCardInPlay(card)) {
        				AllZone.GameAction.moveToLibrary(card);
        				card.getOwner().shuffle();
        			}
        		}
        	};

        	Input runtime = new Input() {
        		private static final long serialVersionUID = 8914195530360741167L;

        		@Override
        		public void showMessage() {
        			StringBuilder sb = new StringBuilder();
        			sb.append("Shuffle ").append(card).append(" into its owner's library");
        			intoLibrary.setStackDescription(sb.toString());
        			
        			stopSetNext(new Input_PayManaCost(intoLibrary));
        		}
        	};
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(card).append(" - shuffle ").append(card).append(" into owner's library");
        	intoLibrary.setStackDescription(sb.toString());
        	
        	card.addSpellAbility(intoLibrary);
        	intoLibrary.setBeforePayMana(runtime);
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
        else if(cardName.equals("Witch Hunter")) {
        	final String Tgts[] = {"Creature+YouDontCtrl"};
        	Target target = new Target("Select target creature you don't control.", Tgts);
        	final Ability_Cost abCost = new Ability_Cost("1 W W T", card.getName(), true);

        	final SpellAbility ability = new Ability_Activated(card, abCost, target) {
				private static final long serialVersionUID = -7134239527522243583L;

				@Override
        		public void resolve() {
        			Card target = getTargetCard();
        			if( AllZone.GameAction.isCardInPlay(target) && CardFactoryUtil.canTarget(card, target)) {
        				AllZone.GameAction.moveToHand(target);
        			}
        		}
        		
        		@Override
        		public boolean canPlay() {
        			Player opp = card.getController().getOpponent();
        			CardList targets = AllZoneUtil.getCreaturesInPlay(opp);
        			return AllZoneUtil.isCardInPlay(card) && targets.size() > 0 && super.canPlay();
        		}

        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}
        	};//SpellAbility
        	
        	card.addSpellAbility(ability);
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append(abCost).append("Return target creature an opponent controls to its owner's hand.");
        	ability.setDescription(sbDesc.toString());
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(card.getName()).append(" - return target creature to owner's hand.");
        	ability.setStackDescription(sbStack.toString());
        	
        	//ability.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START ************************
        if(cardName.equals("Sanctum Gargoyle")) {
            final SpellAbility ability = new Ability(card, "0") {
                
                @Override
                public void chooseTargetAI() {
                    CardList grave = getGraveArts();
                    Card target = CardFactoryUtil.AI_getBestArtifact(grave);
                    setTargetCard(target);
                }
                
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Card c = AllZone.Display.getChoice("Select card", getGraveArts().toArray());
                        setTargetCard(c);
                    }
                    
                    Card target = getTargetCard();
                    
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    if(AllZone.GameAction.isCardInZone(target, grave)
                    		&& CardFactoryUtil.canTarget(card, target)) 
                    	AllZone.GameAction.moveToHand(target);
                }//resolve()
                
                CardList getGraveArts() {
                    CardList list = AllZoneUtil.getPlayerGraveyard(card.getController());
                    return list.filter(AllZoneUtil.artifacts);
                }
            };//SpellAbility
            
            final Command cip = new Command() {
				private static final long serialVersionUID = -8905934905046258175L;

				CardList getGraveArts() {
                    CardList list = AllZoneUtil.getPlayerGraveyard(card.getController());
                    return list.filter(AllZoneUtil.artifacts);
                }
				
				public void execute() {
					if(getGraveArts().size() > 0)
						AllZone.Stack.add(ability);
            	}
            };
            
            ability.setStackDescription(cardName + " - return target artifact from your graveyard to your hand.");
            card.addComesIntoPlayCommand(cip);
        }//*************** END ************ END **************************
        */
        /*
        //*************** START *********** START ************************
        if(cardName.equals("Sharuum the Hegemon")) {
            final SpellAbility ability = new Ability(card, "0") {
                
                @Override
                public void chooseTargetAI() {
                    CardList grave = getGraveArts();
                    Card target = CardFactoryUtil.AI_getBestArtifact(grave);
                    setTargetCard(target);
                }
                
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        Card c = AllZone.Display.getChoice("Select card", getGraveArts().toArray());
                        setTargetCard(c);
                    }
                    
                    Card target = getTargetCard();
                    
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    if(AllZone.GameAction.isCardInZone(target, grave)
                    		&& CardFactoryUtil.canTarget(card, target)) 
                    	AllZone.GameAction.moveTo(play, target);
                }//resolve()
                
                CardList getGraveArts() {
                    CardList list = AllZoneUtil.getPlayerGraveyard(card.getController());
                    return list.filter(AllZoneUtil.artifacts);
                }
            };//SpellAbility
            
            final Command cip = new Command() {
				private static final long serialVersionUID = 7977273396908140261L;

				CardList getGraveArts() {
                    CardList list = AllZoneUtil.getPlayerGraveyard(card.getController());
                    return list.filter(AllZoneUtil.artifacts);
                }
				
				public void execute() {
					if(getGraveArts().size() > 0)
						AllZone.Stack.add(ability);
            	}
            };
            
            ability.setStackDescription(cardName + " - return target artifact from your graveyard to the battlefield.");
            card.addComesIntoPlayCommand(cip);
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Temporal Adept")) {
        	final Ability_Tap ability = new Ability_Tap(card, "U U U") {
				private static final long serialVersionUID = 6775250144167556206L;

				@Override
        		public void resolve() {
        			Card target = getTargetCard();
        			if( CardFactoryUtil.canTarget(card, target)) {
        				AllZone.GameAction.moveToHand(target);
        			}
        		}
        		
        		@Override
        		public boolean canPlay() {
        			CardList targets = AllZoneUtil.getCardsInPlay();
        			return AllZoneUtil.isCardInPlay(card) && targets.size() > 0 && super.canPlay();
        		}

        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}
        	};//SpellAbility
        	
        	card.addSpellAbility(ability);
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(card.getName()).append(" - return target permanent to owner's hand.");
        	ability.setStackDescription(sb.toString());
        	
        	ability.setBeforePayMana(CardFactoryUtil.input_targetPermanent(ability));
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
        	
        	//final Ability_Tap ability = new Ability_Tap(card, "X") {
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
                		return AllZone.getZone(card).is(Constant.Zone.Play) && Phase.canCastSorcery(card.getController());
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
