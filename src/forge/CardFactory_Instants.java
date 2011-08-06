package forge;

import java.util.ArrayList;
import java.util.Random;

import javax.swing.JOptionPane;


public class CardFactory_Instants {

    public static Card getCard(final Card card, final String cardName, Player owner) {
    	
    	
        //*************** START *********** START **************************
        if (cardName.equals("Resuscitate")) {
            /**
             *  This card does not work and this is a place holder.
             *  May require a keyword factory.
             */
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 2024445242584858534L;
                
                @Override
                public void resolve() {
                    
                }//resolve
            };//SpellAbility
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - I do nothing but go to the graveyard.");
            spell.setStackDescription(sb.toString());
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if (cardName.equals("Brave the Elements")) {
        	/**
        	 *  This card now works slightly better than it did before the spAllPump 
        	 *  keyword was created. The AI is too simple and needs some work.
        	 */
        	final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = -7998437920995642451L;
				
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

	             		   // String[] colors = Constant.Color.Colors;
	             		   // colors[colors.length-1] = null;
	             		   
	             		   // You can no longer choose to gain "protection from null".
	             		   String[] colors = Constant.Color.onlyColors;

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
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                           return c.isWhite();
                        }
                    });
                    
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
                        
                        if (AllZone.GameAction.isCardInPlay(target[0]) 
                        		&& !target[0].getKeyword().contains(kboost)) {
                            target[0].addExtrinsicKeyword(kboost);
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }//if
                    }//for
				}//resolve
        	};//SpellAbility
        	card.setSVar("PlayMain1", "TRUE");
        	card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Pongify")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -7657135492744577568L;
                
                @Override
                public boolean canPlayAI() {
                    return (getCreature().size() != 0) && (AllZone.Phase.getTurn() > 3);
                }
                
                @Override
                public void chooseTargetAI() {
                    Card best = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(best);
                }
                
                CardList getCreature() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (3 < c.getNetAttack());
                        }
                    });
                    list = list.getNotKeyword("Indestructible");
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        CardFactoryUtil.makeToken("Ape", "G 3 3 Ape", getTargetCard().getController(), "G",
                                new String[] {"Creature", "Ape"}, 3, 3, new String[] {""});
                        AllZone.GameAction.destroyNoRegeneration(getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Peel from Reality")) {
            final Card[] target = new Card[2];
            final int[] index = new int[1];
            
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5781099237509350795L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    //bounce two creatures in target[]
                    for(int i = 0; i < target.length; i++) {
                        Card c = target[i];
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                        
                        if(AllZone.GameAction.isCardInPlay(c) 
                        		&& CardFactoryUtil.canTarget(card, c)) AllZone.GameAction.moveTo(hand, c);
                    }
                }//resolve()
            };//SpellAbility
            

            final Input input = new Input() {
                private static final long serialVersionUID = -5897481915350104062L;
                
                @Override
                public void showMessage() {
                    if(index[0] == 0) AllZone.Display.showMessage("Select target creature you control to bounce to your hand");
                    else AllZone.Display.showMessage("Select target creature you don't control to return to its owner's hand");
                    
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    //must target creature you control
                    if(index[0] == 0 
                    	&& !c.getController().equals(card.getController())) return;
                    
                    //must target creature you don't control
                    if(index[0] == 1 
                    	&& c.getController().equals(card.getController())) return;
                    

                    if(c.isCreature() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(card, c)) {
                        target[index[0]] = c;
                        index[0]++;
                        showMessage();
                        
                        if(index[0] == target.length) {
                            if(this.isFree()) {
                                this.setFree(false);
                                AllZone.Stack.add(spell);
                                stop();
                            } else stopSetNext(new Input_PayManaCost(spell));
                        }
                    }
                }//selectCard()
            };//Input
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 1194864613104644447L;
                
                @Override
                public void showMessage() {
                    index[0] = 0;
                    stopSetNext(input);
                }
            };//Input
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            spell.setBeforePayMana(runtime);
            
            card.setSVar("PlayMain1", "TRUE");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Wings of Velis Vel")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5744842090293912606L;
                
                @Override
                public boolean canPlayAI() {
                    CardList small = new CardList(AllZone.Computer_Battlefield.getCards());
                    small = small.getType("Creature");
                    
                    //try to make a good attacker
                    if(0 < small.size()) {
                        CardListUtil.sortAttackLowFirst(small);
                        setTargetCard(small.get(0));
                        
                        return true && AllZone.Phase.getPhase().equals(Constant.Phase.Main1);
                    }
                    
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    //in case ability is played twice
                    final int[] oldAttack = new int[1];
                    final int[] oldDefense = new int[1];
                    
                    final Card card[] = new Card[1];
                    card[0] = getTargetCard();
                    
                    oldAttack[0] = card[0].getBaseAttack();
                    oldDefense[0] = card[0].getBaseDefense();
                    
                    card[0].setBaseAttack(4);
                    card[0].setBaseDefense(4);
                    card[0].addExtrinsicKeyword("Flying");
                    
                    //EOT
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 7236360479349324099L;
                        
                        public void execute() {
                            card[0].setBaseAttack(oldAttack[0]);
                            card[0].setBaseDefense(oldDefense[0]);
                            
                            card[0].removeExtrinsicKeyword("Flying");
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("About Face") || cardName.equals("Inside Out") || cardName.equals("Transmutation") || cardName.equals("Twisted Image")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5744842090293911111L;
                
                @Override
                public void resolve() {
                    //in case ability is played twice
                    final int[] oldAttack = new int[1];
                    final int[] oldDefense = new int[1];
                    
                    final Card card[] = new Card[1];
                    card[0] = getTargetCard();
                    
                    oldAttack[0] = card[0].getBaseAttack();
                    oldDefense[0] = card[0].getBaseDefense();
                    
                    card[0].setBaseAttack(oldDefense[0]);
                    card[0].setBaseDefense(oldAttack[0]);
                    
                    //EOT
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 7236360479349324099L;
                        
                        public void execute() {
                            card[0].setBaseAttack(oldAttack[0]);
                            card[0].setBaseDefense(oldDefense[0]);
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if(cardName.equals("Crib Swap")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -4567382566960071562L;
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	Player controller = getTargetCard().getController();
                    	
                        AllZone.GameAction.exile(getTargetCard());
                        
                        CardFactoryUtil.makeToken("Shapeshifter", "C 1 1 Shapeshifter",
                                controller, "", new String[] {"Creature", "Shapeshifter"}, 1,
                                1, new String[] {"Changeling"});
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                	return (getCreature().size() != 0) && (AllZone.Phase.getTurn() > 4);
                }//canPlayAI()
                
                CardList getCreature() {
                	CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getNetAttack() > 2 
                            		&& CardFactoryUtil.canTarget(card, c) 
                            		&& (!c.getName().equals("Shapeshifter") 
                            		|| (c.getName().equals("Shapeshifter") 
                            		&& (c.isEnchanted() 
                            		|| c.getCounters(Counters.P1P1) != 0))));
                        }
                    });
                    return list;
                }//getCreature()

                @Override
                public void chooseTargetAI() {
                	Card best = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(best);
                }//chooseTargetAI()
            };//SpellAbility
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************

        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Entomb")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 4724906962713222211L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void humanResolve() {
                    Object check = AllZone.Display.getChoiceOptional("Select card",
                            AllZone.Human_Library.getCards());
                    if(check != null) {
                        PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                        AllZone.GameAction.moveTo(grave, (Card) check);
                    }
                    AllZone.HumanPlayer.shuffle();
                }
                
                public void computerResolve() {
                    Card[] library = AllZone.Computer_Library.getCards();
                    CardList list = new CardList(library);
                    

                    //pick best creature
                    Card c = CardFactoryUtil.AI_getBestCreature(list);
                    if(c == null) c = library[0];
                    //System.out.println("comptuer picked - " +c);
                    AllZone.Computer_Library.remove(c);
                    AllZone.Computer_Graveyard.add(c);
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    return library.getCards().length != 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList creature = new CardList();
                    creature.addAll(AllZone.Computer_Library.getCards());
                    creature = creature.getType("Creature");
                    return creature.size() != 0;
                }
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        */
        
      //*************** START *********** START **************************
        else if(cardName.equals("Beacon of Destruction")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 6653675303299939465L;
                
                @Override
                public void resolve() {
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            getTargetCard().addDamage(5, card);
                            done();
                        } else AllZone.GameAction.moveToGraveyard(card);
                    } else {
                        getTargetPlayer().addDamage(5, card);
                        done();
                    }
                }//resolve()
                
                void done() {
                    //shuffle card back into the library
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    AllZone.GameAction.moveTo(library,card);
                    card.getController().shuffle();
                }
            };
            spell.setChooseTargetAI(CardFactoryUtil.AI_targetHumanCreatureOrPlayer());
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(spell, true, false));
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Whispers of the Muse")) {
            final SpellAbility spell_one = new Spell(card) {
                
                private static final long serialVersionUID = 8341386638247535343L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                	card.getController().drawCard();
                }//resolve()
            };//SpellAbility
            
            final SpellAbility spell_two = new Spell(card) {
                
                private static final long serialVersionUID = -131686114078716307L;
                
                @Override
                public void resolve() {
                	card.getController().drawCard();
                    done();
                }//resolve()
                
                void done() {
                    //return card to the hand
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    AllZone.GameAction.moveTo(hand, card);
                }
            };//SpellAbility
            spell_two.setManaCost("5 U");
            spell_two.setAdditionalManaCost("5");
            
            spell_one.setDescription("Draw a card.");
            
            StringBuilder sb1 = new StringBuilder();
            sb1.append(cardName).append(" - ").append(card.getController()).append(" draws a card.");
            spell_one.setStackDescription(sb1.toString());
            
            spell_two.setDescription("Buyback 5 (You may pay an additional 5 as you cast this spell. If you do, put this card into your hand as it resolves.)");
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(cardName).append(" - (Buyback) ").append(card.getController()).append(" draws a card.");
            spell_two.setStackDescription(sb2.toString());
            spell_two.setIsBuyBackAbility(true);
            
            card.clearSpellAbility();
            card.addSpellAbility(spell_one);
            card.addSpellAbility(spell_two);
            
        }//*************** END ************ END **************************
        */
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sprout Swarm")) {
            final SpellAbility spell_one = new Spell(card) {
                private static final long serialVersionUID = -609007714604161377L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeTokenSaproling(card.getController());
                }
            };//SpellAbility
            
            final SpellAbility spell_two = new Spell(card) {
                private static final long serialVersionUID = -1387385820860395676L;
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeTokenSaproling(card.getController());
                    //return card to the hand
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    AllZone.GameAction.moveTo(hand, card);
                }
            };//SpellAbility
            
            spell_one.setManaCost("1 G");
            spell_two.setManaCost("4 G");
            spell_two.setAdditionalManaCost("3");
            
            spell_one.setDescription("Put a 1/1 green Saproling token into play.");
            spell_two.setDescription("Buyback 3 (You may pay an additional 3 as you cast this spell. If you do, put this card into your hand as it resolves.)");
            // spell_two.setDescription("Buyback 3 - Pay 4G, put this card into your hand as it resolves.");
            
            spell_one.setStackDescription("Sprout Swarm - Put a 1/1 green Saproling token into play");
            spell_two.setStackDescription("Sprout Swarm - Buyback, Put a 1/1 green Saproling token into play");
            
            spell_two.setIsBuyBackAbility(true);
            
            card.clearSpellAbility();
            card.addSpellAbility(spell_one);
            card.addSpellAbility(spell_two);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Ensnare")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5170378205496330425L;
                
                @Override
                public void resolve() {
                    CardList creats = new CardList();
                    creats.addAll(AllZone.Human_Battlefield.getCards());
                    creats.addAll(AllZone.Computer_Battlefield.getCards());
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature();
                        }
                    });
                    for(int i = 0; i < creats.size(); i++)
                        creats.get(i).tap();
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            spell.setDescription("Tap all creatures.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Tap all creatures");
            spell.setStackDescription(sb.toString());
            
            final SpellAbility bounce = new Spell(card) {
                private static final long serialVersionUID = 6331598238749406160L;
                
                @Override
                public void resolve() {
                    CardList creats = new CardList();
                    creats.addAll(AllZone.Human_Battlefield.getCards());
                    creats.addAll(AllZone.Computer_Battlefield.getCards());
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature();
                        }
                    });
                    for(int i = 0; i < creats.size(); i++)
                        creats.get(i).tap();
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Island");
                    return list.size() >= 2;
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
            };
            bounce.setDescription("You may return two Islands you control to their owner's hand rather than pay Ensnare's mana cost.");
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - Tap all creatures.");
            bounce.setStackDescription(sb2.toString());
            
            bounce.setManaCost("0");
            
            final Input bounceIslands = new Input() {
                private static final long serialVersionUID = -8511915834608321343L;
                int                       stop             = 2;
                int                       count            = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select an Island");
                    ButtonUtil.disableAll();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(c.getType().contains("Island") && zone.is(Constant.Zone.Battlefield)) {
                        AllZone.GameAction.moveToHand(c);
                        
                        count++;
                        if(count == stop) {
                            AllZone.Stack.add(bounce);
                            stop();
                        }
                    }
                }//selectCard()
            };
            
            bounce.setBeforePayMana(bounceIslands);
            
            Command bounceIslandsAI = new Command() {
                private static final long serialVersionUID = 6399831162328201755L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Island");
                    //TODO: sort by tapped
                    
                    for(int i = 0; i < 2; i++) {
                        AllZone.GameAction.moveToHand(list.get(i));
                    }
                }
            };
            
            bounce.setBeforePayManaAI(bounceIslandsAI);
            
            card.clearSpellAbility();
            card.addSpellAbility(bounce);
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Astral Steel")) {
           final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 141478784123241969L;
                
                @Override                
                public boolean canPlay() {
                	Player player = AllZone.Phase.getPlayerTurn();
                	Player opponent = player.getOpponent();
                    PlayerZone PlayerPlayZone = AllZone.getZone(Constant.Zone.Battlefield, player);
            		PlayerZone opponentPlayZone = AllZone.getZone(Constant.Zone.Battlefield, opponent);                  
                    CardList start = new CardList(PlayerPlayZone.getCards());
                    CardList start2 = new CardList(opponentPlayZone.getCards());
                    final CardList list = start.getType("Creature");
                    final CardList list2 = start2.getType("Creature");
                    return (list.size() + list2.size() > 0);
                }
                
                public boolean canPlayAI() {
                    return getAttacker() != null && AllZone.Phase.getPhase().equals(Constant.Phase.Main1);
                }
                
                
                @Override
                public void chooseTargetAI() {
                    setTargetCard(getAttacker());
                }
                
                public Card getAttacker() {
                	Player Computer = AllZone.Phase.getPlayerTurn();
					PlayerZone ComputerPlayZone = AllZone.getZone(Constant.Zone.Battlefield, Computer);
			        CardList ComputerCreatureList = new CardList(ComputerPlayZone.getCards());
			        ComputerCreatureList = ComputerCreatureList.getType("Creature");
			        ComputerCreatureList = ComputerCreatureList.filter(new CardListFilter() {
						public boolean addCard(Card c) {
							return c.getNetAttack() >= 2 && CardFactoryUtil.canTarget(card, getTargetCard());
						}
					});
                    if(ComputerCreatureList.size() != 0){
                        Card[] Target = new Card[ComputerCreatureList.size()];
            			for(int i = 0; i < ComputerCreatureList.size(); i++) {
            				Card crd = ComputerCreatureList.get(i);
            				Target[i] = crd;
            			}
    			        Random randomGenerator = new Random();
  			          int randomInt = randomGenerator.nextInt(ComputerCreatureList.size());
                    	return Target[randomInt];
                    }
                    else return null;
                }//getAttacker()              
                
                @Override
                public void resolve() {
                    final Card c = getTargetCard();
                        c.addTempAttackBoost(1);
                        c.addTempDefenseBoost(2);
                    
                    c.updateObservers();
                    
                    Command untilEOT = new Command() {
                        private static final long serialVersionUID = -28032591440730370L;
                        
                        public void execute() {
                            c.addTempAttackBoost(-1);
                            c.addTempDefenseBoost(-2);
                        }
                    };
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }//resolve()
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Scattershot")) {
           final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 74777841291969L;
                
                @Override                           
                public boolean canPlayAI() {
                    return false;
                }      
                @Override
                public void resolve() {

                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(1,card);
                    }
                    };
           };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        	spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END ************************** 
        
      //*************** START *********** START **************************
        else if(cardName.equals("Reaping the Graves")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -57014445262924814L;
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone grave = AllZone.getZone(c);
                    
                    if(AllZone.GameAction.isCardInZone(c, grave)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getController());
                        AllZone.GameAction.moveTo(hand, c);
                    }
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    return super.canPlay() && getCreatures().length != 0;
                }
                
                public Card[] getCreatures() {
                    CardList creature = new CardList();
                    PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    creature.addAll(zone.getCards());
                    creature = creature.getType("Creature");
                    return creature.toArray();
                }
                
                @Override
                public void chooseTargetAI() {
                    Card c[] = getCreatures();
                    Card biggest = c[0];
                    for(int i = 0; i < c.length; i++)
                        if(biggest.getNetAttack() < c[i].getNetAttack()) biggest = c[i];
                    
                    setTargetCard(biggest);
                }
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Input target = new Input() {
                private static final long serialVersionUID = -3717723884199321767L;
                
                @Override
                public void showMessage() {
                    Object check = AllZone.Display.getChoiceOptional("Select creature", getCreatures());
                    if(check != null) {
                        spell.setTargetCard((Card) check);
                        stopSetNext(new Input_PayManaCost(spell));
                    } else stop();
                }//showMessage()
                
                public Card[] getCreatures() {
                    CardList creature = new CardList();
                    PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    creature.addAll(zone.getCards());
                    creature = creature.getType("Creature");
                    return creature.toArray();
                }
            };//Input
            spell.setBeforePayMana(target);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sprouting Vines")) {
            SpellAbility spell = new Spell(card) {

				private static final long serialVersionUID = -65984152637468746L;

				@Override
                public void resolve() {
					AllZone.GameAction.searchLibraryBasicLand(card.getController(), 
							Constant.Zone.Hand, false);
				}
                
                public boolean canPlayAI()
                {
                	PlayerZone library = AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer);
                	CardList list = new CardList(library.getCards());
                	list = list.getType("Basic");
                	return list.size() > Phase.StormCount;
                }
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END ************************** 
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Reiterate")) {
            final SpellAbility spell_one = new Spell(card) {

				private static final long serialVersionUID = -659841515428746L;

				@Override
                public void resolve() {
					AllZone.CardFactory.copySpellontoStack(card,getTargetCard(), true);					
				}
                
                public boolean canPlay()
                {
                	ArrayList<Card> list = AllZone.Stack.getSourceCards();
                	CardList StackList = new CardList();
                	for(int i = 0; i < list.size(); i++) StackList.add(list.get(i));

                	StackList = StackList.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return c.isSorcery() || c.isInstant();
                    	}
                    });
                	return StackList.size() > 0 && super.canPlay();
                }
            };//SpellAbility
            
            final SpellAbility spell_two = new Spell(card) {
                
                private static final long serialVersionUID = -131686114078716307L;
   				@Override
                    public void resolve() {
   					AllZone.CardFactory.copySpellontoStack(card,getTargetCard(), true);
                        done();
                }//resolve()
                    
                    public boolean canPlay()
                    {
                    	ArrayList<Card> list = AllZone.Stack.getSourceCards();
                    	CardList StackList = new CardList();
                    	for(int i = 0; i < list.size(); i++) StackList.add(list.get(i));

                    	StackList = StackList.filter(new CardListFilter() {
                        	public boolean addCard(Card c) {
                        		return c.isSorcery() || c.isInstant();
                        	}
                        });
                    	return StackList.size() > 0 && super.canPlay();
                    }

                
                void done() {
                    //return card to the hand
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    AllZone.GameAction.moveTo(hand, card);
                }
            };//SpellAbility
            spell_two.setManaCost("4 R R");
            spell_two.setAdditionalManaCost("3");
            
            spell_one.setDescription("Copy target instant or sorcery spell. You may choose new targets for the copy.");
            // spell_one.setStackDescription(cardName + " - " + card.getController() + "Copies " + spell_one.getTargetCard());
            StringBuilder sbOne = new StringBuilder();
            sbOne.append(cardName).append(" - ").append(card.getController()).append(" Copies ").append(spell_one.getTargetCard());
            spell_one.setStackDescription(sbOne.toString());
            
            // spell_two.setDescription("Buyback 3 - Pay 4 R R , put this card into your hand as it resolves.");
            spell_two.setDescription("Buyback 3 (You may pay an additional 3 as you cast this spell. If you do, put this card into your hand as it resolves.)");
            // spell_two.setStackDescription(cardName + " - (Buyback) " + card.getController() + "Copies " + spell_two.getTargetCard());
            StringBuilder sbTwo = new StringBuilder();
            sbTwo.append(cardName).append(" - (Buyback) ").append(card.getController()).append(" Copies ").append(spell_two.getTargetCard());
            
            spell_two.setIsBuyBackAbility(true);
            
            Input runtime1 = new Input() {
                private static final long serialVersionUID = -7823269301012427007L;
                
                @Override
                public void showMessage() {
                	ArrayList<Card> list = AllZone.Stack.getSourceCards();
                	CardList StackList = new CardList();
                	for(int i = 0; i < list.size(); i++) StackList.add(list.get(i));

                	StackList = StackList.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.isSorcery() || c.isInstant();
                    	}
                    });
                    
                    stopSetNext(CardFactoryUtil.input_Spell(spell_one, StackList, false));
                    
                }//showMessage()
            };//Input
           
            Input runtime2 = new Input() {
                private static final long serialVersionUID = -7823269301012427007L;
                
                @Override
                public void showMessage() {
                	ArrayList<Card> list = AllZone.Stack.getSourceCards();
                	CardList StackList = new CardList();
                	for(int i = 0; i < list.size(); i++) StackList.add(list.get(i));

                	StackList = StackList.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.isSorcery() || c.isInstant();
                    	}
                    });
                    
                    stopSetNext(CardFactoryUtil.input_Spell(spell_two, StackList, false));
                    
                }//showMessage()
            };//Input
            
            card.clearSpellAbility();
            card.addSpellAbility(spell_one);
            card.addSpellAbility(spell_two);
            card.setCopiesSpells(true);
            spell_one.setBeforePayMana(runtime1);
            spell_two.setBeforePayMana(runtime2);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Twincast") || cardName.equals("Reverberate") || cardName.equals("Fork")) {
            final SpellAbility spell = new Spell(card) {

				private static final long serialVersionUID = -659841515428746L;

				@Override
                public void resolve() {
					AllZone.CardFactory.copySpellontoStack(card,getTargetCard(), true);				
				}
                
                public boolean canPlay()
                {
                	ArrayList<Card> list = AllZone.Stack.getSourceCards();
                	CardList StackList = new CardList();
                	for(int i = 0; i < list.size(); i++) StackList.add(list.get(i));

                	StackList = StackList.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return c.isSorcery() || c.isInstant();
                    	}
                    });
                	return StackList.size() > 0 && super.canPlay();
                }
            };//SpellAbility
            Input runtime = new Input() {
                private static final long serialVersionUID = -7823269301012427007L;
                
                @Override
                public void showMessage() {
                	ArrayList<Card> list = AllZone.Stack.getSourceCards();
                	CardList StackList = new CardList();
                	for(int i = 0; i < list.size(); i++) StackList.add(list.get(i));

                	StackList = StackList.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return c.isSorcery() || c.isInstant();
                    	}
                    });
                    
                    stopSetNext(CardFactoryUtil.input_Spell(spell, StackList, false));
                    
                }//showMessage()
            };//Input
            card.clearSpellAbility();
            card.setCopiesSpells(true);
            card.addSpellAbility(spell);
            spell.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Wing Shards")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 4780150265170723L;
                
                @Override                
                public boolean canPlay() {
                    return (AllZone.Phase.getPhase().equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility) || (AllZone.Phase.getPhase().equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility)));
                }
                @Override
                public void resolve() {
                	Card attack[] = AllZone.Combat.getAttackers(); 
                	Card target = null;
                	Player player = card.getController();
                    if(!player.isHuman()){
                        Object check = AllZone.Display.getChoiceOptional("Select creature", attack);
                        if(check != null) {
                           target = ((Card) check);
                        } 
                    } else {
                        CardList Targets = new CardList();
                        Player TPlayer = card.getController().getOpponent();
    					PlayerZone TZone = AllZone.getZone(Constant.Zone.Battlefield, TPlayer);
                        for(int i = 0; i < attack.length; i++) {
            				Card crd = attack[i];			                                
                                if(AllZone.GameAction.isCardInZone(attack[i], TZone)) Targets.add(crd);
                        }
                        CardListUtil.sortAttack(Targets);
                        if(Targets.size() != 0) target = (Targets.get(Targets.size() - 1));
                        }

                    if(target != null) AllZone.GameAction.sacrifice(target);                
                }
                
                @Override
                public boolean canPlayAI() {
                	Card attack[] = AllZone.Combat.getAttackers();
                    CardList Targets = new CardList();
                    for(int i = 0; i < attack.length; i++) {
        				Card crd = attack[i];
                        Targets.add(crd);
                    }
                    return (Targets.size() > 0 && AllZone.Phase.getPhase().equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)) ;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Fact or Fiction")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1481112451519L;
                
                @Override
                public void resolve() {
                    
                    Card choice = null;
                    
                    //check for no cards in hand on resolve
                    PlayerZone Library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone Hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    //PlayerZone Grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList cards = new CardList();
                    
                    if(Library.size() == 0) {
                    	JOptionPane.showMessageDialog(null, "No more cards in library.", "", JOptionPane.INFORMATION_MESSAGE);
                    	return;
                    }
                    int Count = 5;
                    if(Library.size() < 5) Count = Library.size();
                    for(int i = 0; i < Count; i++) cards.add(Library.get(i));
                    CardList Pile1 = new CardList();
                    CardList Pile2 = new CardList();
                    boolean stop = false;
                    int  Pile1CMC = 0;
                    int  Pile2CMC = 0;
                   

                        AllZone.Display.getChoice("Revealing top " + Count + " cards of library: ", cards.toArray());
                        //Human chooses
                        if(card.getController().equals(AllZone.ComputerPlayer)) {
                        for(int i = 0; i < Count; i++) {
                        	if(stop == false) {
                        choice = AllZone.Display.getChoiceOptional("Choose cards to put into the first pile: ", cards.toArray());
                        if(choice != null) {
                        	Pile1.add(choice);
                        	cards.remove(choice);
                        	Pile1CMC = Pile1CMC + CardUtil.getConvertedManaCost(choice);
                        }
                        else stop = true;	
                        }
                        }
                        for(int i = 0; i < Count; i++) {
                        	if(!Pile1.contains(Library.get(i))) {
                        		Pile2.add(Library.get(i));
                        		Pile2CMC = Pile2CMC + CardUtil.getConvertedManaCost(Library.get(i));
                        	}
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("You have spilt the cards into the following piles" + "\r\n" + "\r\n");
                        sb.append("Pile 1: " + "\r\n");
                        for(int i = 0; i < Pile1.size(); i++) sb.append(Pile1.get(i).getName() + "\r\n");
                        sb.append("\r\n" + "Pile 2: " + "\r\n");
                        for(int i = 0; i < Pile2.size(); i++) sb.append(Pile2.get(i).getName() + "\r\n");
                        JOptionPane.showMessageDialog(null, sb, "", JOptionPane.INFORMATION_MESSAGE);
                        if(Pile1CMC >= Pile2CMC) {
                        	JOptionPane.showMessageDialog(null, "Computer adds the first pile to its hand and puts the second pile into the graveyard", "", JOptionPane.INFORMATION_MESSAGE);
	                    	  for(int i = 0; i < Pile1.size(); i++) AllZone.GameAction.moveTo(Hand, Pile1.get(i));
	                    	  for(int i = 0; i < Pile2.size(); i++) AllZone.GameAction.moveToGraveyard(Pile2.get(i));
                        } else {
                        	JOptionPane.showMessageDialog(null, "Computer adds the second pile to its hand and puts the first pile into the graveyard", "", JOptionPane.INFORMATION_MESSAGE);
	                    	  for(int i = 0; i < Pile2.size(); i++) AllZone.GameAction.moveTo(Hand, Pile2.get(i));
	                    	  for(int i = 0; i < Pile1.size(); i++) AllZone.GameAction.moveToGraveyard(Pile1.get(i));		
		    		}
                        
                    } else//Computer chooses (It picks the highest converted mana cost card and 1 random card.)
                    {
                        Card biggest = null;
                        biggest = Library.get(0);
                        
                        for(int i = 0; i < Count; i++) {
                            if(CardUtil.getConvertedManaCost(biggest.getManaCost()) >= CardUtil.getConvertedManaCost(biggest.getManaCost())) {
                                biggest = cards.get(i);
                            }
                        }
                        Pile1.add(biggest);
                        cards.remove(biggest);
                        if(cards.size() > 0) { 
                        Card Random = CardUtil.getRandom(cards.toArray());
                        Pile1.add(Random);
                        }
                        for(int i = 0; i < Count; i++) if(!Pile1.contains(Library.get(i))) Pile2.add(Library.get(i));
                        StringBuilder sb = new StringBuilder();
                        sb.append("Choose a pile to add to your hand: " + "\r\n" + "\r\n");
                        sb.append("Pile 1: " + "\r\n");
                        for(int i = 0; i < Pile1.size(); i++) sb.append(Pile1.get(i).getName() + "\r\n");
                        sb.append("\r\n" + "Pile 2: " + "\r\n");
                        for(int i = 0; i < Pile2.size(); i++) sb.append(Pile2.get(i).getName() + "\r\n");
			        	Object[] possibleValues = {"Pile 1", "Pile 2"};
			        	Object q = JOptionPane.showOptionDialog(null, sb, "Fact or Fiction", 
			        			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
			        			null, possibleValues, possibleValues[0]);
	                      if(q.equals(0)) {	                    	 
	                    	  for(int i = 0; i < Pile1.size(); i++) AllZone.GameAction.moveTo(Hand, Pile1.get(i));
	                    	  for(int i = 0; i < Pile2.size(); i++) AllZone.GameAction.moveToGraveyard(Pile2.get(i));
			    		} else {
	                    	  for(int i = 0; i < Pile2.size(); i++) AllZone.GameAction.moveTo(Hand, Pile2.get(i));
	                    	  for(int i = 0; i < Pile1.size(); i++) AllZone.GameAction.moveToGraveyard(Pile1.get(i));	
			    		}
                    }
                   Pile1.clear();
                   Pile2.clear();
                }//resolve()
                        
                @Override
                public boolean canPlayAI() {
                	PlayerZone Library = AllZone.getZone(Constant.Zone.Library, card.getController());
                	CardList cards = new CardList(Library.getCards());
                    return cards.size() >= 10;
                }
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Brain Freeze")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 4247050159744693L;
                
                @Override
                public boolean canPlayAI() {
                    
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
                    CardList libList = new CardList(lib.getCards());
                    
                    return (libList.size() > 0 
                                && ((AllZone.Phase.getPhase().equals(Constant.Phase.Main2)) 
                                || Phase.StormCount*3 >= libList.size()));
                }//canPlayAI()
                
                @Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.HumanPlayer);
                }//chooseTargetAI()
                
                @Override
                public void resolve() {
                    getTargetPlayer().mill(3);
                }//resolve()
            };//SpellAbility
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
       
        
        //*************** START *********** START **************************
        else if (cardName.equals("Renewed Faith")) { 
            /**
             *   The "You gain 6 life." ability is now done via a keyword. This code
             *   object will give the controller 2 life when this card is cycled.
             */
            card.addCycleCommand(new Command() {
                private static final long serialVersionUID = 7699412574052780825L;
                    
                public void execute() {
                	card.getController().gainLife(2, card);
                }
            });
        }//*************** END ************ END **************************
       

        //*************** START *********** START **************************
        else if(cardName.equals("Life Burst")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 5653342880372240806L;
                
                @Override
                public void resolve() {
                    CardList count = new CardList();
                    count.addAll(AllZone.Human_Graveyard.getCards());
                    count.addAll(AllZone.Computer_Graveyard.getCards());
                    count = count.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getName().equals("Life Burst");
                        }
                    });
                    
                    getTargetPlayer().gainLife(4 + (4 * count.size()), card);
                }
            };
            spell.setChooseTargetAI(CardFactoryUtil.AI_targetComputer());
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Accumulated Knowledge")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -7650377883588723237L;
                
                @Override
                public void resolve() {
                	card.getController().drawCards(1);
                    CardList count = new CardList();
                    count.addAll(AllZone.Human_Graveyard.getCards());
                    count.addAll(AllZone.Computer_Graveyard.getCards());
                    count = count.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getName().equals("Accumulated Knowledge");
                        }
                    });
                    
                    card.getController().drawCards(count.size());
                }
            };
            spell.setDescription("Draw a card, then draw cards equal to the number of cards named Accumulated Knowledge in all graveyards.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - Draw a card, then draw cards equal to the number of cards named Accumulated Knowledge in all graveyards.");
            spell.setStackDescription(sb.toString());
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Echoing Decay")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 3154935854257358023L;
                
                @Override
                public boolean canPlayAI() {
                    CardList c = getCreature();
                    if(c.isEmpty()) return false;
                    else {
                        setTargetCard(c.get(0));
                        return true;
                    }
                }//canPlayAI()
                
                CardList getCreature() {
                    CardList out = new CardList();
                    CardList list = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
                    list.shuffle();
                    
                    for(int i = 0; i < list.size(); i++)
                        if((list.get(i).getNetAttack() >= 2) && (list.get(i).getNetDefense() <= 2)) out.add(list.get(i));
                    
                    //in case human player only has a few creatures in play, target anything
                    if(out.isEmpty() && 0 < CardFactoryUtil.AI_getHumanCreature(2, card, true).size()
                            && 3 > CardFactoryUtil.AI_getHumanCreature(card, true).size()) {
                        out.addAll(CardFactoryUtil.AI_getHumanCreature(2, card, true).toArray());
                        CardListUtil.sortFlying(out);
                    }
                    return out;
                }//getCreature()
                

                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        final Card c = getTargetCard();
                        
                        c.addTempAttackBoost(-2);
                        c.addTempDefenseBoost(-2);
                        
                        AllZone.EndOfTurn.addUntil(new Command() {
                            private static final long serialVersionUID = 1327455269456577020L;
                            
                            public void execute() {
                                c.addTempAttackBoost(2);
                                c.addTempDefenseBoost(2);
                            }
                        });
                        
                        //get all creatures
                        CardList list = new CardList();
                        list.addAll(AllZone.Human_Battlefield.getCards());
                        list.addAll(AllZone.Computer_Battlefield.getCards());
                        
                        list = list.getName(getTargetCard().getName());
                        list.remove(getTargetCard());
                        
                        if(!getTargetCard().isFaceDown()) for(int i = 0; i < list.size(); i++) {
                            final Card crd = list.get(i);
                            
                            crd.addTempAttackBoost(-2);
                            crd.addTempDefenseBoost(-2);
                            
                            AllZone.EndOfTurn.addUntil(new Command() {
                                private static final long serialVersionUID = 5151337777143949221L;
                                
                                public void execute() {
                                    crd.addTempAttackBoost(2);
                                    crd.addTempDefenseBoost(2);
                                }
                            });
                            //list.get(i).addDamage(2);
                        }
                        
                    }//in play?
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Hidetsugu's Second Rite")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 176857775451818523L;
                
                @Override
                public void resolve() {
                    if(getTargetPlayer().getLife() == 10) {
                    	getTargetPlayer().addDamage(10, card);
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return AllZone.HumanPlayer.getLife() == 10;
                }
                
            };
            spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Spell Pierce")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 4685055135070191326L;
                
                @Override
                public void resolve() {
                    String manaCost = "2";
                    Ability ability = new Ability(card, manaCost) {
                        @Override
                        public void resolve() {
                            ;
                        }
                    };
                    
                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 8094833091127334678L;
                        
                        public void execute() {
                            SpellAbility sa = AllZone.Stack.pop();
                            AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                        }
                    };
                    
                    if(AllZone.Stack.peek().getActivatingPlayer().isHuman()) {
                    	GameActionUtil.payManaDuringAbilityResolve(card + "\r\n", ability.getManaCost(), 
                    			Command.Blank, unpaidCommand);
                    } else {
                        if(ComputerUtil.canPayCost(ability)) ComputerUtil.playNoStack(ability);
                        else {
                            SpellAbility sa = AllZone.Stack.pop();
                            AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                        }
                    }
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.Stack.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    //Player opponent = card.getController().getOpponent();
                    SpellAbility sa = AllZone.Stack.peek();
                    
                    //is spell?, did opponent play it?, is this a creature spell?
                    return sa.isSpell() //&& opponent.equals(sa.getSourceCard().getController())
                            && !sa.getSourceCard().getType().contains("Creature")
                            && CardFactoryUtil.isCounterable(sa.getSourceCard());
                }//canPlay()
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Mana Leak") || cardName.equals("Convolute") || cardName.equals("Daze")
                || cardName.equals("Force Spike") || cardName.equals("Runeboggle")
                || cardName.equals("Spell Snip") || cardName.equals("Mana Tithe")
                || cardName.equals("Miscalculation")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 6139754377230333678L;
                
                @Override
                public void resolve() {
                    String manaCost = "1";
                    if(cardName.equals("Miscalculation")) manaCost = "2";
                    else if(cardName.equals("Mana Leak")) manaCost = "3";
                    else if(cardName.equals("Convolute")) manaCost = "4";
                    Ability ability = new Ability(card, manaCost) {
                        @Override
                        public void resolve() {
                            ;
                        }
                    };
                    
                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 8094833091127334678L;
                        
                        public void execute() {
                            SpellAbility sa = AllZone.Stack.pop();
                            AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                        }
                    };
                    
                    if(AllZone.Stack.peek().getActivatingPlayer().isHuman()) {
                    	GameActionUtil.payManaDuringAbilityResolve(card + "\r\n", ability.getManaCost(), 
                    			Command.Blank, unpaidCommand);
                    } else {
                        if(ComputerUtil.canPayCost(ability)) ComputerUtil.playNoStack(ability);
                        else {
                        	SpellAbility sa = AllZone.Stack.pop();
                            AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                        }
                    }
                    
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.Stack.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    //Player opponent = card.getController().getOpponent();
                    SpellAbility sa = AllZone.Stack.peek();
                    
                    return sa.isSpell() //&& opponent.equals(sa.getSourceCard().getController())
                            && CardFactoryUtil.isCounterable(sa.getSourceCard());
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            if(cardName.equals("Daze")) {
                spell.setDescription("Counter target spell unless its controller pays 1.");
                StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Counter target spell unless its controller pays 1.");
                spell.setStackDescription(sb.toString());
                
                final SpellAbility bounce = new Spell(card) {
                    private static final long serialVersionUID = -8310299673731730438L;
                    
                    @Override
                    public void resolve() {
                        SpellAbility sa = AllZone.Stack.pop();
                        AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                    }
                    
                    @Override
                    public boolean canPlay() {
                        if(AllZone.Stack.size() == 0) return false;
                        
                        //see if spell is on stack and that opponent played it
                        Player opponent = card.getController().getOpponent();
                        SpellAbility sa = AllZone.Stack.peek();
                        
                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                        CardList list = new CardList(play.getCards());
                        list = list.getType("Island");
                        return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
                                && CardFactoryUtil.isCounterable(sa.getSourceCard()) && list.size() >= 1;
                    }
                    
                    @Override
                    public boolean canPlayAI() {
                        return false;
                    }
                    
                };
                bounce.setDescription("You may return an Island you control to their owner's hand rather than pay Daze's mana cost.");
                StringBuilder sb2 = new StringBuilder();
                sb2.append(card.getName()).append(" - Counter target spell unless its controller pays 1.");
                bounce.setStackDescription(sb2.toString());
                bounce.setManaCost("0");
                
                final Input bounceIslands = new Input() {
                    private static final long serialVersionUID = 7624182730685889456L;
                    int                       stop             = 1;
                    int                       count            = 0;
                    
                    @Override
                    public void showMessage() {
                        AllZone.Display.showMessage("Select an Island");
                        ButtonUtil.disableAll();
                    }
                    
                    @Override
                    public void selectButtonCancel() {
                        stop();
                    }
                    
                    @Override
                    public void selectCard(Card c, PlayerZone zone) {
                        if(c.getType().contains("Island") && zone.is(Constant.Zone.Battlefield)) {
                            AllZone.GameAction.moveToHand(c);
                            
                            count++;
                            if(count == stop) {
                                AllZone.Stack.add(bounce);
                                stop();
                            }
                        }
                    }//selectCard()
                };
                
                bounce.setBeforePayMana(bounceIslands);
                card.addSpellAbility(bounce);
            }//if Daze
            else // This is Chris' Evil hack to get the Cycling cards to give us a choose window with text for the SpellAbility
            {
                spell.setDescription(card.getText());
                card.setText("");
            }
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Impulse")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -6793636573741251978L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    CardList top = new CardList();
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    Card c;
                    int j = 4;
                    if(library.size() < 4) j = library.size();
                    for(int i = 0; i < j; i++) {
                        c = library.get(0);
                        library.remove(0);
                        top.add(c);
                    }
                    
                    if(top.size() >= 1) {
                        //let user get choice
                        Card chosen = AllZone.Display.getChoice("Choose a card to put into your hand",
                                top.toArray());
                        top.remove(chosen);
                        
                        //put card in hand
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        hand.add(chosen);
                        
                        //add cards to bottom of library
                        for(int i = 0; i < top.size(); i++)
                            library.add(top.get(i));
                    }
                }//resolve()
            };//SpellAbility
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Echoing Truth")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 563933533543239220L;
                
                @Override
                public boolean canPlayAI() {
                    CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                    return 4 < AllZone.Phase.getTurn() && 0 < human.size();
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                    setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
                }
                
                @Override
                public void resolve() {
                    //if target card is not in play, just quit
                    if(!AllZone.GameAction.isCardInPlay(getTargetCard())
                            || !CardFactoryUtil.canTarget(card, getTargetCard())) return;
                    
                    //get all permanents
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Battlefield.getCards());
                    all.addAll(AllZone.Computer_Battlefield.getCards());
                    
                    CardList sameName = all.getName(getTargetCard().getName());
                    sameName = sameName.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card c)
                    	{
                    		return !c.isFaceDown();
                    	}
                    });
                    
                    if(!getTargetCard().isFaceDown()) {
                        //bounce all permanents with the same name
                        for(int i = 0; i < sameName.size(); i++) {
                            if(sameName.get(i).isToken()) AllZone.GameAction.exile(sameName.get(i));
                            else {
                                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, sameName.get(i).getOwner());
                                AllZone.GameAction.moveTo(hand, sameName.get(i));
                            }
                        }//for
                    }//if (!isFaceDown())
                    else {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            Input target = new Input() {
                private static final long serialVersionUID = -3978705328511825933L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target non-land permanent for " + spell.getSourceCard());
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(!card.isLand() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(spell, card)) {
                        spell.setTargetCard(card);
                        if (this.isFree())
                        {
                        	this.setFree(false);
                            AllZone.Stack.add(spell);
                            stop();
                        }
                        else
                        	stopSetNext(new Input_PayManaCost(spell));
                    }
                }
            };//Input
            
            card.setSVar("PlayMain1", "TRUE");
            
            spell.setBeforePayMana(target);
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Opt")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 6002051826637535590L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.isHuman()) humanResolve();
                    else computerResolve();
                }
                
                public void computerResolve() {
                    //if top card of library is a land, put it on bottom of library
                    if(AllZone.Computer_Library.getCards().length != 0) {
                        Card top = AllZone.Computer_Library.get(0);
                        if(top.isLand()) {
                            AllZone.Computer_Library.remove(top);
                            AllZone.Computer_Library.add(top);
                        }
                    }
                    // AllZone.GameAction.drawCard(card.getController());
                }//computerResolve()
                
                public void humanResolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    //see if any cards are in library
                    if(library.getCards().length != 0) {
                        Card top = library.get(0);
                        
                        Object o = top;
                        while(o instanceof Card)
                            o = AllZone.Display.getChoice("Do you want draw this card?", new Object[] {
                                    top, "Yes", "No"});
                        
                        if(o.toString().equals("No")) {
                            library.remove(top);
                            library.add(top);
                        }
                    }//if
                    // AllZone.GameAction.drawCard(card.getController());
                }//resolve()
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Worldly Tutor")) {
            SpellAbility spell = new Spell(card) {
                
				private static final long serialVersionUID = -2388471137292697028L;

				@Override
                public boolean canPlayAI() {
                    return 6 < AllZone.Phase.getTurn();
                }
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void computerResolve() {
                    CardList creature = new CardList(AllZone.Computer_Library.getCards());
                    creature = creature.getType("Creature");
                    if(creature.size() != 0) {
                        Card c = creature.get(0);
                        card.getController().shuffle();
                        
                        //move to top of library
                        AllZone.Computer_Library.remove(c);
                        AllZone.Computer_Library.add(c, 0);
                        
                        CardList list = new CardList();
                        list.add(c);
                        AllZone.Display.getChoiceOptional("Computer picked:", list.toArray());
                    }
                }//computerResolve()
                
                public void humanResolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    CardList list = new CardList(library.getCards());
                    list = list.getType("Creature");
                    
                    if(list.size() != 0) {
                        Object o = AllZone.Display.getChoiceOptional("Select a creature", list.toArray());
                        
                        card.getController().shuffle();
                        if(o != null) {
                            //put creature on top of library
                            library.remove(o);
                            library.add((Card) o, 0);
                        }
                    }//if
                }//resolve()
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Wrap in Vigor")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -4235465815975050436L;
                
                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }
                
                public Card getAttacker() {
                    //target creature that is going to attack
                    Combat c = ComputerUtil.getAttackers();
                    Card[] att = c.getAttackers();
                    if(att.length != 0) return att[0];
                    else return null;
                }//getAttacker()
                
                @Override
                public void resolve() {
                    final Card[] c = AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards();
                    
                    for(int i = 0; i < c.length; i++)
                        if(c[i].isCreature()) c[i].addShield();
                    
                    AllZone.EndOfTurn.addUntil(new Command() {
                        private static final long serialVersionUID = -3946800525315027053L;
                        
                        public void execute() {
                            for(int i = 0; i < c.length; i++)
                                c[i].resetShield();
                        }
                    });
                    
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Strangling Soot")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -3598479453933951865L;
                
                @Override
                public boolean canPlayAI() {
                    CardList c = CardFactoryUtil.AI_getHumanCreature(3, card, true);
                    CardListUtil.sortAttack(c);
                    CardListUtil.sortFlying(c);
                    
                    if(c.isEmpty()) return false;
                    else {
                        setTargetCard(c.get(0));
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
                        return AllZone.GameAction.isCardInZone(card, hand);
                    }
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && c.getNetDefense() <= 3
                            && CardFactoryUtil.canTarget(card, getTargetCard())) AllZone.GameAction.destroy(c);
                    
                }//resolve()
            };//SpellAbility
            
            final SpellAbility flashback = new Spell(card) {
                
                private static final long serialVersionUID = -4009531242109129036L;
                
                @Override
                public boolean canPlay() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    
                    return AllZone.GameAction.isCardInZone(card, grave);
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList c = CardFactoryUtil.AI_getHumanCreature(3, card, true);
                    CardListUtil.sortAttack(c);
                    CardListUtil.sortFlying(c);
                    
                    if(c.isEmpty()) return false;
                    else {
                        setTargetCard(c.get(0));
                        return true;
                    }
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    PlayerZone removed = AllZone.getZone(Constant.Zone.Exile, card.getController());
                    
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && c.getNetDefense() <= 3
                            && CardFactoryUtil.canTarget(card, getTargetCard())) AllZone.GameAction.destroy(c);
                    
                    grave.remove(card);
                    removed.add(card);
                }//resolve()
            };//flashback
            
            Input targetFB = new Input() {
                
                private static final long serialVersionUID = -5469698194749752297L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target creature for " + card.getName()
                            + " - creature must have a toughness of 3 or less");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(flashback, card)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    }
                    if(card.isCreature() && zone.is(Constant.Zone.Battlefield) && card.getNetDefense() <= 3) {
                        flashback.setTargetCard(card);
                        stopSetNext(new Input_PayManaCost(flashback));
                    }
                }
            };//Input
            
            flashback.setFlashBackAbility(true);
            flashback.setManaCost("5 R");
            flashback.setBeforePayMana(targetFB);
            flashback.setDescription("Flashback: 5 R");
            
            Input target = new Input() {
                private static final long serialVersionUID = -198153850086215235L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target creature for " + card.getName()
                            + " - creature must have a toughness of 3 or less");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(spell, card)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    }
                    if(card.isCreature() && zone.is(Constant.Zone.Battlefield) && card.getNetDefense() <= 3) {
                    	spell.setTargetCard(card);
                    	if(this.isFree()) 
                        {
                        	this.setFree(false);
                        	AllZone.Stack.add(spell);
                        	stop();
                    	} 
                    	else 
                    		stopSetNext(new Input_PayManaCost(spell));
                    }
                }
            };//Input
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            spell.setBeforePayMana(target);
            
            card.addSpellAbility(flashback);
            
            card.setFlashback(true);
            
            card.setSVar("PlayMain1", "TRUE");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Eladamri's Call")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -6495398165357932918L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.isHuman()) humanResolve();
                    else computerResolve();
                }
                
                public void humanResolve() {
                    CardList creatures = new CardList(AllZone.Human_Library.getCards());
                    creatures = creatures.getType("Creature");
                    
                    Object check = AllZone.Display.getChoiceOptional("Select creature", creatures.toArray());
                    if(check != null) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        AllZone.GameAction.moveTo(hand, (Card) check);
                    }
                    AllZone.HumanPlayer.shuffle();
                }
                
                public void computerResolve() {
                    Card[] library = AllZone.Computer_Library.getCards();
                    CardList list = new CardList(library);
                    list = list.getType("Creature");
                    
                    if(list.size() > 0) {
                        //pick best creature
                        Card c = CardFactoryUtil.AI_getBestCreature(list);
                        if(c == null) c = list.get(0);
                        AllZone.Computer_Library.remove(c);
                        AllZone.Computer_Hand.add(c);
                        CardList cl = new CardList();
                        cl.add(c);
                        AllZone.Display.getChoiceOptional("Computer picked:", cl.toArray());
                    }
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    return library.getCards().length != 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList creature = new CardList();
                    creature.addAll(AllZone.Computer_Library.getCards());
                    creature = creature.getType("Creature");
                    return creature.size() != 0;
                }
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
  
        //*************** START *********** START **************************
        else if(cardName.equals("Overwhelming Intellect")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -8825219868732813877L;
                
                @Override
                public void resolve() {
                    SpellAbility sa = AllZone.Stack.pop();
                    AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                    
                    int convertedManaCost = CardUtil.getConvertedManaCost(sa.getSourceCard().getManaCost());
                    for(int i = 0; i < convertedManaCost; i++) {
                    	card.getController().drawCard();
                    }
                    
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.Stack.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    Player opponent = card.getController().getOpponent();
                    SpellAbility sa = AllZone.Stack.peek();
                    return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
                            && sa.getSourceCard().getType().contains("Creature")
                            && CardFactoryUtil.isCounterable(sa.getSourceCard());
                    

                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Banishing Knack")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 6518824567946786581L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    final Card creature = getTargetCard();
                    final Ability_Tap tBanish = new Ability_Tap(creature) {
                        private static final long serialVersionUID = -1008113001678623984L;
                        
                        @Override
                        public boolean canPlayAI() {
                            return false;
                        }
                        
                        @Override
                        public void resolve() {
                            StringBuilder sb = new StringBuilder();
                            sb.append(creature).append(" - Return").append(getTargetCard()).append("to its owner's hand");
                            setStackDescription(sb.toString());
                            final Card[] target = new Card[1];
                            target[0] = getTargetCard();
                            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, target[0].getOwner());
                            
                            if(AllZone.GameAction.isCardInPlay(target[0])
                                    && CardFactoryUtil.canTarget(creature, target[0])) {
                                AllZone.GameAction.moveTo(hand, target[0]);
                            }
                        }//resolve()
                    };//tBanish;
                    tBanish.setDescription("tap: Return target nonland permanent to its owner's hand.");
                    creature.addSpellAbility(tBanish);
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Battlefield.getCards());
                    all.addAll(AllZone.Computer_Battlefield.getCards());
                    all = all.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (!c.isLand() && CardFactoryUtil.canTarget(creature, c));
                        }
                    });
                    
                    tBanish.setBeforePayMana(CardFactoryUtil.input_targetSpecific(tBanish, all,
                            "Return target nonland permanent to its owner's hand.", true, false));
                    AllZone.EndOfTurn.addUntil(new Command() {
                        private static final long serialVersionUID = -7819140065166374666L;
                        
                        public void execute() {
                            creature.removeSpellAbility(tBanish);
                        }
                    });
                }
            };//SpellAbility
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            spell.setDescription("Until end of turn, target creature gains \"tap: Return target nonland permanent to its owner's hand.\"");
            spell.setStackDescription("Target creature gains \"tap: Return target nonland permanent to its owner's hand.\"");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Vampiric Tutor")) {
            SpellAbility spell = new Spell(card) {
                

				private static final long serialVersionUID = 8792913782443246572L;

				@Override
                public boolean canPlayAI() {
                    int life = AllZone.ComputerPlayer.getLife();
                    if(4 < AllZone.Phase.getTurn() && AllZone.Computer_Library.size() > 0 && life >= 4) return true;
                    else return false;
                }
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void computerResolve() {
                    //TODO: somehow select a good non-creature card for AI
                    CardList creature = new CardList(AllZone.Computer_Library.getCards());
                    creature = creature.getType("Creature");
                    if(creature.size() != 0) {
                        Card c = CardFactoryUtil.AI_getBestCreature(creature);
                        
                        if(c == null) {
                            creature.shuffle();
                            c = creature.get(0);
                        }
                        
                        card.getController().shuffle();
                        
                        //move to top of library
                        AllZone.Computer_Library.remove(c);
                        AllZone.Computer_Library.add(c, 0);
                        
                        //lose 2 life
                        AllZone.ComputerPlayer.loseLife(2,card);
                    }
                }//computerResolve()
                
                public void humanResolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    CardList list = new CardList(library.getCards());
                    
                    if(list.size() != 0) {
                        Object o = AllZone.Display.getChoiceOptional("Select a card", list.toArray());
                        
                        card.getController().shuffle();
                        if(o != null) {
                            //put card on top of library
                            library.remove(o);
                            library.add((Card) o, 0);
                        }
                        //lose 2 life
                        AllZone.HumanPlayer.loseLife(2, card);
                    }//if
                    

                }//resolve()
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Intuition")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 8282597086298330698L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    if(player.equals(AllZone.HumanPlayer)) humanResolve();
                    else computerResolve();
                }
                
                public void humanResolve() {
                    CardList libraryList = new CardList(AllZone.Human_Library.getCards());
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList selectedCards = new CardList();
                    
                    Object o = AllZone.Display.getChoiceOptional("Select first card", libraryList.toArray());
                    if(o != null) {
                        Card c1 = (Card) o;
                        libraryList.remove(c1);
                        selectedCards.add(c1);
                    } else {
                        return;
                    }
                    o = AllZone.Display.getChoiceOptional("Select second card", libraryList.toArray());
                    if(o != null) {
                        Card c2 = (Card) o;
                        libraryList.remove(c2);
                        selectedCards.add(c2);
                    } else {
                        return;
                    }
                    o = AllZone.Display.getChoiceOptional("Select third card", libraryList.toArray());
                    if(o != null) {
                        Card c3 = (Card) o;
                        libraryList.remove(c3);
                        selectedCards.add(c3);
                    } else {
                        return;
                    }
                    
                    Card choice = selectedCards.get(MyRandom.random.nextInt(2)); //comp randomly selects one of the three cards
                    
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    library.remove(choice);
                    hand.add(choice);
                    
                    selectedCards.remove(choice);
                    Card toGrave1 = selectedCards.get(0);
                    Card toGrave2 = selectedCards.get(1);
                    library.remove(toGrave1);
                    library.remove(toGrave2);
                    selectedCards.remove(toGrave2);
                    selectedCards.remove(toGrave2);
                    
                    grave.add(toGrave1);
                    grave.add(toGrave2);
                    
                    AllZone.HumanPlayer.shuffle();
                }
                
                public void computerResolve() {
                    Card[] library = AllZone.Computer_Library.getCards();
                    CardList list = new CardList(library);
                    CardList selectedCards = new CardList();
                    
                    //pick best creature
                    Card c = CardFactoryUtil.AI_getBestCreature(list);
                    if(c == null) {
                        c = library[0];
                    }
                    list.remove(c);
                    selectedCards.add(c);
                    
                    c = CardFactoryUtil.AI_getBestCreature(list);
                    if(c == null) {
                        c = library[0];
                    }
                    list.remove(c);
                    selectedCards.add(c);
                    
                    c = CardFactoryUtil.AI_getBestCreature(list);
                    if(c == null) {
                        c = library[0];
                    }
                    list.remove(c);
                    selectedCards.add(c);
                    
                    // NOTE: Using getChoiceOptional() results in a null error when you click on Cancel.
                    Object o = AllZone.Display.getChoice("Select card to give to computer", selectedCards.toArray());
                    
                    Card choice = (Card) o;
                    
                    selectedCards.remove(choice);
                    AllZone.Computer_Library.remove(choice);
                    AllZone.Computer_Hand.add(choice);
                    
                    AllZone.Computer_Library.remove(selectedCards.get(0));
                    AllZone.Computer_Library.remove(selectedCards.get(1));
                    
                    AllZone.Computer_Graveyard.add(selectedCards.get(0));
                    AllZone.Computer_Graveyard.add(selectedCards.get(1));
                    
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    return library.getCards().length >= 3;
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList creature = new CardList();
                    creature.addAll(AllZone.Computer_Library.getCards());
                    creature = creature.getType("Creature");
                    return creature.size() != 0;
                }
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("High Tide")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -4997834721261916L;
                
                @Override
                public boolean canPlayAI() {                   
                    return false;
                }//canPlay()
                
                @Override
                public void resolve() {
                	Phase.HighTideCount = Phase.HighTideCount + 1;
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Tithe")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1504792204536793942L;
                
                public boolean oppMoreLand() {
                	Player oppPlayer = card.getController().getOpponent();
                    
                    PlayerZone selfZone = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    PlayerZone oppZone = AllZone.getZone(Constant.Zone.Battlefield, oppPlayer);
                    
                    CardList self = new CardList(selfZone.getCards());
                    CardList opp = new CardList(oppZone.getCards());
                    
                    self = self.getType("Land");
                    opp = opp.getType("Land");
                    
                    return (self.size() < opp.size()); // && super.canPlay();
                }//oppoMoreLand()
                
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    
                    CardList plains = new CardList(library.getCards());
                    plains = plains.getType("Plains");
                    
                    if(0 < plains.size()) AllZone.GameAction.moveTo(hand, plains.get(0));
                    
                    if(oppMoreLand() && 1 < plains.size()) AllZone.GameAction.moveTo(hand, plains.get(1));
                    
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Primal Boost")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 2449600319884238808L;
                
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
                    
                    CardList list = new CardList(c.getAttackers());
                    CardListUtil.sortFlying(list);
                    
                    Card[] att = list.toArray();
                    if(att.length != 0) return att[0];
                    else return null;
                }//getAttacker()
                
                @Override
                public void resolve() {
                    final Card[] target = new Card[1];
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 3753684523153747308L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(target[0])) {
                                target[0].addTempAttackBoost(-4);
                                target[0].addTempDefenseBoost(-4);
                            }
                        }
                    };
                    
                    target[0] = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0])) {
                        target[0].addTempAttackBoost(4);
                        target[0].addTempDefenseBoost(4);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
            };
            spell.setDescription("\r\nTarget creature gets +4/+4 until end of turn.");
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
            //card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "2 G"));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Animate Land")) {
            final Card[] target = new Card[1];
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -3359299797188942353L;
                
                public void execute() {
                    if(AllZone.GameAction.isCardInPlay(target[0])) {
                        target[0].removeType("Creature");
                    }
                }
            };
            
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 4890851927124377327L;
                
                @Override
                public void resolve() {
                    target[0] = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0])) {
                        target[0].addType("Creature");
                        target[0].setBaseAttack(3);
                        target[0].setBaseDefense(3);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                    /* all this doesnt work, computer will not attack with the animated land

                    //does the computer have any land in play?
                    CardList land = new CardList(AllZone.Computer_Play.getCards());
                    land = land.getType("Land");
                    land = land.filter(new CardListFilter()
                    {
                      public boolean addCard(Card c)
                      {
                              //checks for summoning sickness, and is not tapped
                        return CombatUtil.canAttack(c);
                      }
                    });
                    return land.size() > 1 && CardFactoryUtil.AI_isMainPhase();
                    */
                }
            };//SpellAbility
//      spell.setChooseTargetAI(CardFactoryUtil.AI_targetType("Land", AllZone.Computer_Play));
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "Land"));
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
               
        //*************** START *********** START **************************
        else if(cardName.equals("Seething Song")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 113811381138L;
                
                @Override
                public void resolve() {
                    Card mp = AllZone.ManaPool;
                    mp.addExtrinsicKeyword("ManaPool:R");
                    mp.addExtrinsicKeyword("ManaPool:R");
                    mp.addExtrinsicKeyword("ManaPool:R");
                    mp.addExtrinsicKeyword("ManaPool:R");
                    mp.addExtrinsicKeyword("ManaPool:R");
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            
            spell.setStackDescription("Adds R R R R R to your mana pool");
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            return card;
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Dark Ritual")) {
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = -8579887529151755266L;
                
                @Override
                public void resolve() {
                    Card mp = AllZone.ManaPool;
                    mp.addExtrinsicKeyword("ManaPool:B");
                    mp.addExtrinsicKeyword("ManaPool:B");
                    mp.addExtrinsicKeyword("ManaPool:B");
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" adds B B B to your mana pool");
            spell.setStackDescription(sb.toString());
            
            // spell.setStackDescription(cardName + " adds B B B to your mana pool");
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            return card;
        }//*************** END ************ END **************************

   
        //*************** START *********** START **************************
        else if (cardName.equals("Pyretic Ritual")) {
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = -5473428583650237774L;

				@Override
                public void resolve() {
                    Card mp = AllZone.ManaPool;
                    mp.addExtrinsicKeyword("ManaPool:R");
                    mp.addExtrinsicKeyword("ManaPool:R");
                    mp.addExtrinsicKeyword("ManaPool:R");
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" adds R R R to your mana pool");
            spell.setStackDescription(sb.toString());
            
            // spell.setStackDescription(cardName + " adds R R R to your mana pool");
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            return card;
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if(cardName.equals("Path to Exile")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 4752934806606319269L;
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	Player player = getTargetCard().getController();
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                        
                        //remove card from play
                        AllZone.GameAction.exile(getTargetCard());
                        
                        //Retrieve basic land
                        CardList lands = new CardList(lib.getCards());
                        lands = lands.getType("Basic");
                        
                        if(player.equals(AllZone.HumanPlayer) && lands.size() > 0) {
                            String[] choices = {"Yes", "No"};
                            Object choice = AllZone.Display.getChoice("Search for Basic Land?", choices);
                            if(choice.equals("Yes")) {
                                Object o = AllZone.Display.getChoiceOptional(
                                        "Pick a basic land card to put into play", lands.toArray());
                                if(o != null) {
                                    Card card = (Card) o;
                                    lib.remove(card);
                                    AllZone.Human_Battlefield.add(card);
                                    card.tap();
                                    lands.remove(card);
                                    player.shuffle();
                                }
                            }// if choice yes
                        } // player equals human
                        else if(player.equals(AllZone.ComputerPlayer) && lands.size() > 0) {
                            Card card = lands.get(0);
                            lib.remove(card);
                            // hand.add(card);
                            AllZone.Computer_Battlefield.add(card);
                            card.tap();
                            lands.remove(card);
                            player.shuffle();
                        }
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    CardList creature = new CardList(AllZone.Human_Battlefield.getCards());
                    creature = creature.getType("Creature");
                    creature = creature.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    return creature.size() != 0 && (AllZone.Phase.getTurn() > 4);
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList play = new CardList(AllZone.Human_Battlefield.getCards());
                    Card target = CardFactoryUtil.AI_getBestCreature(play, card);
                    setTargetCard(target);
                }
            };
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Gush")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 8881817765689776033L;
                
                @Override
                public void resolve() {
                	card.getController().drawCards(2);
                }
            };
            spell.setDescription("Draw two cards.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Draw two cards.");
            spell.setStackDescription(sb.toString());
            
            final SpellAbility bounce = new Spell(card) {
                private static final long serialVersionUID = 1950742710354343569L;
                
                @Override
                public void resolve() {
                	card.getController().drawCards(2);
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Island");
                    return list.size() >= 2;
                }
                
            };
            bounce.setDescription("You may return two Islands you control to their owner's hand rather than pay Gush's mana cost.");
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - Draw two cards.");
            bounce.setStackDescription(sb2.toString());
            bounce.setManaCost("0");
            
            final Input bounceIslands = new Input() {
                private static final long serialVersionUID = 3124427514142382129L;
                int                       stop             = 2;
                int                       count            = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select an Island");
                    ButtonUtil.disableAll();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(c.getType().contains("Island") && zone.is(Constant.Zone.Battlefield)) {
                        AllZone.GameAction.moveToHand(c);
                        
                        count++;
                        if(count == stop) {
                            AllZone.Stack.add(bounce);
                            stop();
                        }
                    }
                }//selectCard()
            };
            
            bounce.setBeforePayMana(bounceIslands);
            
            Command bounceIslandsAI = new Command() {
                private static final long serialVersionUID = 235908265780575226L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Island");
                    //TODO: sort by tapped
                    
                    for(int i = 0; i < 2; i++) {
                        AllZone.GameAction.moveToHand(list.get(i));
                    }
                }
            };
            
            bounce.setBeforePayManaAI(bounceIslandsAI);
            
            card.clearSpellAbility();
            card.addSpellAbility(bounce);
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Thwart")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 6549506712141125977L;
                
                @Override
                public void resolve() {
                    SpellAbility sa = AllZone.Stack.pop();
                    AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.Stack.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    Player opponent = card.getController().getOpponent();
                    SpellAbility sa = AllZone.Stack.peek();
                    
                    return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
                            && CardFactoryUtil.isCounterable(sa.getSourceCard());
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            spell.setDescription("Counter target spell.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Counter target spell.");
            spell.setStackDescription(sb.toString());
            
            final SpellAbility bounce = new Spell(card) {
                private static final long serialVersionUID = -8310299673731730438L;
                
                @Override
                public void resolve() {
                    SpellAbility sa = AllZone.Stack.pop();
                    AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.Stack.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    Player opponent = card.getController().getOpponent();
                    SpellAbility sa = AllZone.Stack.peek();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Island");
                    return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
                            && CardFactoryUtil.isCounterable(sa.getSourceCard()) && list.size() >= 3;
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
            };
            bounce.setDescription("You may return three Islands you control to their owner's hand rather than pay Thwart's mana cost.");
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - Counter target spell.");
            bounce.setStackDescription(sb2.toString());            
            bounce.setManaCost("0");
            
            final Input bounceIslands = new Input() {
                private static final long serialVersionUID = 3124427514142382129L;
                int                       stop             = 3;
                int                       count            = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select an Island");
                    ButtonUtil.disableAll();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(c.getType().contains("Island") && zone.is(Constant.Zone.Battlefield)) {
                        AllZone.GameAction.moveToHand(c);
                        
                        count++;
                        if(count == stop) {
                            AllZone.Stack.add(bounce);
                            stop();
                        }
                    }
                }//selectCard()
            };
            
            bounce.setBeforePayMana(bounceIslands);
            
            Command bounceIslandsAI = new Command() {
                private static final long serialVersionUID = 8250154784542733353L;
                
                public void execute() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Island");
                    //TODO: sort by tapped
                    
                    for(int i = 0; i < 3; i++) {
                        AllZone.GameAction.moveToHand(list.get(i));
                    }
                }
            };
            
            bounce.setBeforePayManaAI(bounceIslandsAI);
            
            card.clearSpellAbility();
            card.addSpellAbility(bounce);
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Force of Will")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 7960371805654673281L;
                
                @Override
                public void resolve() {
                    SpellAbility sa = AllZone.Stack.pop();
                    AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.Stack.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    Player opponent = card.getController().getOpponent();
                    SpellAbility sa = AllZone.Stack.peek();
                    
                    return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
                            && CardFactoryUtil.isCounterable(sa.getSourceCard());
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            spell.setDescription("Counter target spell.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - Counter target spell.");
            spell.setStackDescription(sb.toString());
            
            final SpellAbility alt = new Spell(card) {
                private static final long serialVersionUID = -8643870743780757816L;
                
                @Override
                public void resolve() {
                    SpellAbility sa = AllZone.Stack.pop();
                    AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.Stack.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    Player opponent = card.getController().getOpponent();
                    SpellAbility sa = AllZone.Stack.peek();
                    
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(hand.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isBlue() && !c.equals(card);
                        }
                    });
                    return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
                            && CardFactoryUtil.isCounterable(sa.getSourceCard()) && list.size() >= 1;
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
            };
            alt.setDescription("You may pay 1 life and exile a blue card from your hand rather than pay Force of Will's mana cost.");
            StringBuilder sb2 = new StringBuilder();
            sb2.append(card.getName()).append(" - Counter target spell.");
            alt.setStackDescription(sb2.toString());
            alt.setManaCost("0");
            
            final Input exileBlue = new Input() {
                private static final long serialVersionUID = 8692998689009712987L;
                int                       stop             = 1;
                int                       count            = 0;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select a blue card");
                    ButtonUtil.disableAll();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(c.isBlue() && zone.is(Constant.Zone.Hand)
                            && !c.equals(card)) {
                        AllZone.GameAction.exile(c);
                        card.getController().loseLife(1, card);
                        
                        count++;
                        if(count == stop) {
                            AllZone.Stack.add(alt);
                            stop();
                        }
                    }
                }//selectCard()
            };
            

            alt.setBeforePayMana(exileBlue);
            
            /*
            Command bounceIslandsAI = new Command()
            {
            private static final long serialVersionUID = -8745630329512914365L;

            public void execute()
              {
            	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
            	  CardList list = new CardList(play.getCards());
            	  list = list.getType("Island");
            	  //TODO: sort by tapped
            	  
            	  for (int i=0;i<3;i++)
            	  {
            		  AllZone.GameAction.moveToHand(list.get(i));
            	  }  
              }
            };
            
            alt.setBeforePayManaAI(bounceIslandsAI);
            */

            card.clearSpellAbility();
            card.addSpellAbility(alt);
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Squall Line"))
        {
      	  final SpellAbility spell = new Spell(card)
      	  {
			private static final long serialVersionUID = 8031146002062605694L;
			public void resolve()
      		{
  				int damage = card.getXManaCostPaid();
  				CardList all = new CardList();
                  all.addAll(AllZone.Human_Battlefield.getCards());
                  all.addAll(AllZone.Computer_Battlefield.getCards());
                  all = all.filter(new CardListFilter()
                  {
                  	public boolean addCard(Card c)
                  	{
                  		return c.isCreature() && c.getKeyword().contains("Flying") &&
                  			   CardFactoryUtil.canDamage(card, c);
                  	}
                  });
                  
                  for(int i = 0; i < all.size(); i++)
                      	all.get(i).addDamage(card.getXManaCostPaid(), card);
                  
                  AllZone.HumanPlayer.addDamage(damage, card);
                  AllZone.ComputerPlayer.addDamage(damage, card);
                  
      			card.setXManaCostPaid(0);
      		}
  			public boolean canPlayAI()
  			{
  				final int maxX = ComputerUtil.getAvailableMana().size() - 1;
  				
  				if (AllZone.HumanPlayer.getLife() <= maxX)
  					return true;
  				
  				CardListFilter filter = new CardListFilter(){
  					public boolean addCard(Card c)
  					{
  						return c.isCreature() && c.getKeyword().contains("Flying") &&
  							   CardFactoryUtil.canDamage(card, c) && maxX >= (c.getNetDefense() + c.getDamage());
  					}
  				};
  				
  				CardList humanFliers = new CardList(AllZone.Human_Battlefield.getCards());
  			    humanFliers = humanFliers.filter(filter);
  			    
  			    CardList compFliers = new CardList(AllZone.Computer_Battlefield.getCards());
  			    compFliers = compFliers.filter(filter);
  			    
  			    return humanFliers.size() > (compFliers.size() + 2) && AllZone.ComputerPlayer.getLife() > maxX + 3;
  			}
      	  };
      	  StringBuilder sbDesc = new StringBuilder();
      	  sbDesc.append(cardName).append(" deals X damage to each creature with flying and each player.");
      	  spell.setDescription(sbDesc.toString());
      	  
      	  StringBuilder sbStack = new StringBuilder();
      	  sbStack.append(card).append(" - deals X damage to each creature with flying and each player.");
       	  spell.setStackDescription(sbStack.toString());
      	  
      	  card.clearSpellAbility();
      	  card.addSpellAbility(spell);
        } 
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Fault Line"))
        {
      	  final String[] keyword = new String[1];
      	  
      	  keyword[0] = "Flying";

      	  final SpellAbility spell = new Spell(card)
      	  {
			private static final long serialVersionUID = -1887664058112475665L;
			public void resolve()
      		{
  				int damage = card.getXManaCostPaid();
  				CardList all = new CardList();
                  all.addAll(AllZone.Human_Battlefield.getCards());
                  all.addAll(AllZone.Computer_Battlefield.getCards());
                  all = all.filter(new CardListFilter()
                  {
                  	public boolean addCard(Card c)
                  	{
                  		return c.isCreature() && !c.getKeyword().contains(keyword[0]) &&
                  			   CardFactoryUtil.canDamage(card, c);
                  	}
                  });
                  
                  for(int i = 0; i < all.size(); i++)
                      	all.get(i).addDamage(card.getXManaCostPaid(), card);
                  
                  AllZone.HumanPlayer.addDamage(damage, card);
                  AllZone.ComputerPlayer.addDamage(damage, card);
                  
      			card.setXManaCostPaid(0);
      		}
  			public boolean canPlayAI()
  			{
  				final int maxX = ComputerUtil.getAvailableMana().size() - CardUtil.getConvertedManaCost(card);
  				
  				if (AllZone.HumanPlayer.getLife() <= maxX)
  					return true;
  				
  				CardListFilter filter = new CardListFilter(){
  					public boolean addCard(Card c)
  					{
  						return c.isCreature() && !c.getKeyword().contains(keyword) &&
  							   CardFactoryUtil.canDamage(card, c) && maxX >= (c.getNetDefense() + c.getDamage());
  					}
  				};
  				
  				CardList human = new CardList(AllZone.Human_Battlefield.getCards());
  			    human = human.filter(filter);
  			    
  			    CardList comp = new CardList(AllZone.Computer_Battlefield.getCards());
  			    comp = comp.filter(filter);
  			    
  			    return human.size() > (comp.size() + 2) && AllZone.ComputerPlayer.getLife() > maxX + 3;
  			}
      	  };
      	  StringBuilder sbDesc = new StringBuilder();
      	  sbDesc.append(cardName).append(" deals X damage to each creature without ").append(keyword[0]).append(" and each player.");
      	  spell.setDescription(sbDesc.toString());
      	  
      	  StringBuilder sbStack = new StringBuilder();
      	  sbStack.append(card).append(" - deals X damage to each creature without ").append(keyword[0]).append(" and each player.");
      	  spell.setStackDescription(sbStack.toString());
      	  
      	  card.clearSpellAbility();
      	  card.addSpellAbility(spell);
        } 
        //*************** END ************ END **************************
      
        
        //*************** START *********** START **************************
        else if(cardName.equals("Stroke of Genius"))
        {
      	  final SpellAbility spell = new Spell(card){
  			private static final long serialVersionUID = -7141472916367953810L;

  			public void resolve()
      		  {
  				Player player = getTargetPlayer();
      			  for(int i=0;i<card.getXManaCostPaid();i++)
      			  {
      				  player.drawCard();
      			  }
      			  card.setXManaCostPaid(0);
      		  }
      		  
      		  public boolean canPlayAI()
      		  {
      			  final int maxX = ComputerUtil.getAvailableMana().size() - 1;
      			  return maxX > 3 && AllZone.Computer_Hand.size() <= 3;
      		  }
      	  };
      	  spell.setDescription("Target player draws X cards.");
      	  spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
      	  spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
      	  
      	  card.clearSpellAbility();
      	  card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Windstorm"))
        {
      	  final SpellAbility spell = new Spell(card)
      	  {
  			private static final long serialVersionUID = 6024081054401784073L;
  			public void resolve()
      		{
  				CardList all = new CardList();
                  all.addAll(AllZone.Human_Battlefield.getCards());
                  all.addAll(AllZone.Computer_Battlefield.getCards());
                  all = all.filter(new CardListFilter()
                  {
                  	public boolean addCard(Card c)
                  	{
                  		return c.isCreature() && c.getKeyword().contains("Flying") &&
                  			   CardFactoryUtil.canDamage(card, c);
                  	}
                  });
                  
                  for(int i = 0; i < all.size(); i++)
                      	all.get(i).addDamage(card.getXManaCostPaid(), card);
                  
      			card.setXManaCostPaid(0);
      		}
  			public boolean canPlayAI()
  			{
  				final int maxX = ComputerUtil.getAvailableMana().size() - 1;
  								
  				CardListFilter filter = new CardListFilter(){
  					public boolean addCard(Card c)
  					{
  						return c.isCreature() && c.getKeyword().contains("Flying") &&
  							   CardFactoryUtil.canDamage(card, c) && maxX >= (c.getNetDefense() + c.getDamage());
  					}
  				};
  				
  				CardList humanFliers = new CardList(AllZone.Human_Battlefield.getCards());
  			    humanFliers = humanFliers.filter(filter);
  			    
  			    CardList compFliers = new CardList(AllZone.Computer_Battlefield.getCards());
  			    compFliers = compFliers.filter(filter);
  			    
  			    return humanFliers.size() > (compFliers.size() + 2);
  			}
      	  };
      	  spell.setDescription("Windstorm deals X damage to each creature with flying.");
      	  spell.setStackDescription("Windstorm - deals X damage to each creature with flying.");
      	  
      	  card.clearSpellAbility();
      	  card.addSpellAbility(spell);
        } 
        //*************** END ************ END **************************
          
        
        //*************** START *********** START **************************
        else if(cardName.equals("Echoing Courage"))
        {
          final SpellAbility spell = new Spell(card)
          {
  		  private static final long serialVersionUID = -8649611733196156346L;

      	  public boolean canPlayAI()
            {
              CardList c = getCreature();
              if(c.isEmpty())
                return false;
              else
              {
                setTargetCard(c.get(0));
                return true;
              }
            }//canPlayAI()
            CardList getCreature()
            {
              CardList out = new CardList();
              CardList list = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
              list.shuffle();

              for(int i = 0; i < list.size(); i++)
                if((list.get(i).getNetAttack() >= 2) && (list.get(i).getNetDefense() <= 2))
                  out.add(list.get(i));

              //in case human player only has a few creatures in play, target anything
              if(out.isEmpty() &&
                  0 < CardFactoryUtil.AI_getHumanCreature(2, card, true).size() &&
                 3 > CardFactoryUtil.AI_getHumanCreature(card, true).size())
              {
                out.addAll(CardFactoryUtil.AI_getHumanCreature(2, card, true).toArray());
                CardListUtil.sortFlying(out);
              }
              return out;
            }//getCreature()


            public void resolve()
            {
              if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
              {
                final Card c = getTargetCard();
               
                c.addTempAttackBoost(2);
                 c.addTempDefenseBoost(2);

                 AllZone.EndOfTurn.addUntil(new Command()
                 {
                private static final long serialVersionUID = 1327455269456577020L;

                public void execute()
                    {
                       c.addTempAttackBoost(-2);
                       c.addTempDefenseBoost(-2);
                    }
                 });

                //get all creatures
                CardList list = new CardList();
                list.addAll(AllZone.Human_Battlefield.getCards());
                list.addAll(AllZone.Computer_Battlefield.getCards());

                list = list.getName(getTargetCard().getName());
                list.remove(getTargetCard());
                 
                if (!getTargetCard().isFaceDown())
                   for(int i = 0; i < list.size(); i++)
                   {
                      final Card crd = list.get(i);
                      
                      crd.addTempAttackBoost(2);
                      crd.addTempDefenseBoost(2);
                      
                      AllZone.EndOfTurn.addUntil(new Command()
                        {
                      private static final long serialVersionUID = 5151337777143949221L;

                      public void execute()
                           {
                              crd.addTempAttackBoost(-2);
                              crd.addTempDefenseBoost(-2);
                           }
                        });
                      //list.get(i).addDamage(2);
                   }
                    
              }//in play?
            }//resolve()
          };//SpellAbility
          card.clearSpellAbility();
          card.addSpellAbility(spell);

          spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Fog") || cardName.equals("Angelsong") || cardName.equals("Darkness") ||
      		  cardName.equals("Holy Day") || cardName.equals("Lull") || cardName.equals("Moment's Peace") ||
      		  cardName.equals("Respite"))
        {
      	  SpellAbility spell = new Spell(card)
      	  {
  			private static final long serialVersionUID = -493504450911948985L;

  			public void resolve()
      		{
      			  AllZone.GameInfo.setPreventCombatDamageThisTurn(true);
      			  
      			  if (cardName.equals("Respite"))
      			  {
      				  CardList attackers = new CardList();
      				  attackers.addAll(AllZone.Combat.getAttackers());
      				  attackers.addAll(AllZone.pwCombat.getAttackers());
      				  card.getController().gainLife(attackers.size(), card);
      			  }
      		}
  			public boolean canPlayAI()
  			{
  				return false;
  			}
      	  };
      	  if (card.getName().equals("Lull") || card.getName().equals("Angelsong")) {
      		  spell.setDescription("Prevent all combat damage that would be dealt this turn.");
      		  StringBuilder sb = new StringBuilder();
      		  sb.append(card.getName()).append(" - Prevent all combat damage that would be dealt this turn.");
      		  spell.setStackDescription(sb.toString());
      	  }
      	  card.clearSpellAbility();
      	  card.addSpellAbility(spell);
      		  
      	  if (cardName.equals("Moment's Peace")) {
      		  card.setFlashback(true);
      		  card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "2 G"));
      	  }
        }//*************** END ************ END **************************
          
        
        //*************** START *********** START **************************
        else if (cardName.equals("Starstorm"))
        {
      	  final SpellAbility spell = new Spell(card)
      	  {
 
			private static final long serialVersionUID = -3554283811532201543L;
			public void resolve()
      		{
  				CardList all = new CardList();
                  all.addAll(AllZone.Human_Battlefield.getCards());
                  all.addAll(AllZone.Computer_Battlefield.getCards());
                  all = all.filter(new CardListFilter()
                  {
                  	public boolean addCard(Card c)
                  	{
                  		return c.isCreature() && CardFactoryUtil.canDamage(card, c);
                  	}
                  });
                  
                  for(int i = 0; i < all.size(); i++)
                      	all.get(i).addDamage(card.getXManaCostPaid(), card);
                  
      			card.setXManaCostPaid(0);
      		}
  			public boolean canPlayAI()
  			{
  				final int maxX = ComputerUtil.getAvailableMana().size() - 1;
  								
  				CardListFilter filter = new CardListFilter(){
  					public boolean addCard(Card c)
  					{
  						return c.isCreature() && CardFactoryUtil.canDamage(card, c) && 
  							   maxX >= (c.getNetDefense() + c.getDamage());
  					}
  				};
  				
  				CardList humanAll = new CardList(AllZone.Human_Battlefield.getCards());
  			    humanAll = humanAll.filter(filter);
  			    
  			    CardList compAll = new CardList(AllZone.Computer_Battlefield.getCards());
  			    compAll = compAll.filter(filter);
  			    
  			    return humanAll.size() > (compAll.size() + 2);
  			}
      	  };
      	  StringBuilder sbDesc = new StringBuilder();
      	  sbDesc.append(cardName).append(" deals X damage to each creature.");
      	  spell.setDescription(sbDesc.toString());
      	  
      	  StringBuilder sbStack = new StringBuilder();
      	  sbStack.append(cardName).append(" - deals X damage to each creature.");
      	  spell.setStackDescription(sbStack.toString());
      	  
      	  card.clearSpellAbility();
      	  card.addSpellAbility(spell);
        } 
        //*************** END ************ END **************************
        
            
        //*************** START *********** START **************************
        else if(cardName.equals("Vitalizing Cascade"))
        {
      	  final SpellAbility spell = new Spell(card){
  			  private static final long serialVersionUID = -5930794708688097023L;

  			  public void resolve()
      		  {
  				  card.getController().gainLife(card.getXManaCostPaid()+3, card);
      			  card.setXManaCostPaid(0);
      		  }
      		  
      		  public boolean canPlayAI()
      		  {
      			  int humanLife = AllZone.HumanPlayer.getLife();
      			  int computerLife = AllZone.ComputerPlayer.getLife();
      			  
      			  final int maxX = ComputerUtil.getAvailableMana().size() - 1;
      			  return maxX > 3 && (humanLife >= computerLife);
      		  }
      	  };
      	  spell.setDescription("You gain X plus 3 life.");
      	  spell.setStackDescription("Vitalizing Cascade - You gain X plus 3 life.");
      	  
      	  card.clearSpellAbility();
      	  card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Natural Selection")) {
        	/* Look at the top 3 cards of target player's library and put them
        	 * back in any order. You may have that player shuffle his or
        	 * her library */

        	final SpellAbility spell = new Spell(card) {
        		private static final long serialVersionUID = 8649520296192617609L;

        		@Override
        		public void resolve() {
        			Player player = getTargetPlayer();
        			AllZoneUtil.rearrangeTopOfLibrary(player, 3, false);
        			AllZone.GameAction.promptForShuffle(player);
        		}
        		@Override
        		public boolean canPlayAI() {
        			//basically the same reason as Sensei's Diving Top
        			return false;
        		}
        	};//spell
        	card.clearSpellAbility();
        	card.addSpellAbility(spell);
        	spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Hurkyl's Recall")) {
        	/*
        	 * Return all artifacts target player owns to his or her hand.
        	 */
        	SpellAbility spell = new Spell(card) {
        		private static final long serialVersionUID = -4098702062413878046L;

        		@Override
        		public boolean canPlayAI() {
        			PlayerZone humanPlay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
        			CardList humanArts = new CardList(humanPlay.getCards());
        			humanArts = humanArts.getType("Artifact");
        			if(humanArts.size() > 0) {
        				return true;
        			}
        			else {
        				return false;
        			}
        		}//canPlayAI

        		@Override
        		public void chooseTargetAI() {
        			setTargetPlayer(AllZone.HumanPlayer);
        		}//chooseTargetAI()

        		@Override
        		public void resolve() {
        			Player player = getTargetPlayer();
        			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
        			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
        			final Player opponent = player.getOpponent();
        			PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Battlefield, opponent);
        			CardList artifacts = new CardList(play.getCards());
        			artifacts.addAll(oppPlay.getCards());
        			artifacts = artifacts.getType("Artifact");

        			for(int i = 0; i < artifacts.size(); i++) {
        				Card thisArtifact = artifacts.get(i);
        				//if is token, remove token from play, else return artifact to hand
        				if(thisArtifact.getOwner().equals(player)) {
        					if(thisArtifact.isToken()) {
        						play.remove(thisArtifact);
        					}
        					else {
        						AllZone.GameAction.moveTo(hand, thisArtifact);
        					}
        				}
        			}
        		}//resolve()
        	};
        	card.clearSpellAbility();
        	card.addSpellAbility(spell);
        	spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Burst Lightning")) {
        	/*
        	 * Kicker 4 (You may pay an additional 4 as you cast this spell.)
        	 * Burst Lightning deals 2 damage to target creature or player. If 
        	 * Burst Lightning was kicked, it deals 4 damage to that creature 
        	 * or player instead.
        	 */
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = -5191461039745723331L;
				int                       damage           = 2;
                
                @Override
                public void chooseTargetAI() {
                    
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
                    creatures = creatures.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return c.getNetAttack() <= damage && !c.getKeyword().contains("Indestructible");
                    	}
                    });
                    if(AllZone.HumanPlayer.getLife() <= damage || 0 == creatures.size()) {
                        setTargetPlayer(AllZone.HumanPlayer);
                        return;
                    }
                    Card c = CardFactoryUtil.AI_getBestCreature(creatures);
                    setTargetCard(c);
                }//chooseTargetAI()
                
                @Override
                public void resolve() {
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            Card c = getTargetCard();
                            c.addDamage(damage, card);
                        }
                    } else getTargetPlayer().addDamage(damage, card);
                }
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - deal 2 damage to target creature or player. If Burst Lightning was kicked, it deals 4 damage to that creature or player instead.");
            spell.setDescription(sb.toString());
            
            final SpellAbility kicker = new Spell(card) {
				private static final long serialVersionUID = 7608486082373416819L;
				int                       damage           = 4;
                
                @Override
                public void chooseTargetAI() {
                	
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
                    creatures = creatures.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return c.getNetAttack() <= damage && !c.getKeyword().contains("Indestructible");
                    	}
                    });
                    if(AllZone.HumanPlayer.getLife() <= damage || 0 == creatures.size()) {
                        setTargetPlayer(AllZone.HumanPlayer);
                        return;
                    }
                    Card c = CardFactoryUtil.AI_getBestCreature(creatures);
                    setTargetCard(c);
                }//chooseTargetAI()
                
                @Override
                public void resolve() {
                    
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            Card c = getTargetCard();
                            c.addDamage(damage, card);
                        }
                    } else getTargetPlayer().addDamage(damage, card);
                    
                    card.setKicked(true);
                }
            };//kicker
            kicker.setManaCost("R 4");
            kicker.setAdditionalManaCost("4");
            kicker.setKickerAbility(true);
            kicker.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(kicker, true, false));
            kicker.setDescription("Kicker: 4");
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            card.addSpellAbility(kicker);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(spell, true, false));
        }//*************** END ************ END **************************
        
        
        //*****************************START*******************************
        else if(cardName.equals("Twiddle") || cardName.equals("Twitch")) {
        	/*
        	 * You may tap or untap target artifact, creature, or land.
        	 */
        	final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 8126471702898625866L;
				
				public boolean canPlayAI() {
        			return false;
        		}
        		public void chooseTargetAI() {
        			//setTargetCard(c);
        		}//chooseTargetAI()
        		public void resolve() {
        			if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
        				if(getTargetCard().isTapped()) {
        					getTargetCard().untap();
        				}
        				else {
        					getTargetCard().tap();
        				}
        			}
        		}
        	};//SpellAbility
        	spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "Artifact;Creature;Land"));
        	card.clearSpellAbility();
        	card.addSpellAbility(spell);		
        }//end Twiddle
        //****************END*******END***********************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Storm Seeker") || cardName.equals("Sudden Impact")) {
        	/*
        	 * Storm Seeker deals damage equal to the number of cards in target player's hand to that player.
        	 */
        	// TODO - this should be converted to keyword.  
        	// tweak spDamageTgt keyword and add "TgtPHand" or something to CardFactoryUtil.xCount()
        	SpellAbility spell = new Spell(card) {
        		private static final long serialVersionUID = -5456164079435151319L;

        		@Override
        		public void resolve() {
        			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetPlayer());
        			int damage = hand.size();

        			//sanity check
        			if( damage < 0 )
        				damage = 0;

        			getTargetPlayer().addDamage(damage, card);
        		}
        	};
        	spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());

        	spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));

        	card.clearSpellAbility();
        	card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if (cardName.equals("Suffer the Past"))
        {
        	final SpellAbility spell = new Spell(card){
				private static final long serialVersionUID = 1168802375190293222L;
				
				@Override
				public void resolve() {
					Player tPlayer = getTargetPlayer();
					Player player = card.getController();
					final int max = card.getXManaCostPaid();

					PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, tPlayer);
					CardList graveList = new CardList(grave.getCards());
					int X = Math.min(max, graveList.size());
					
					if( player.equals(AllZone.HumanPlayer)) {
						for(int i = 0; i < X; i++) {
							Object o = AllZone.Display.getChoice("Remove from game", graveList.toArray());
							if(o == null) break;
							Card c_1 = (Card) o;
							graveList.remove(c_1); //remove from the display list
							AllZone.GameAction.exile(c_1);
						}
					}
					else { //Computer
						//Random random = new Random();
						for(int j = 0; j < X; j++) {
							//int index = random.nextInt(X-j);
							AllZone.GameAction.exile(graveList.get(j));
						}
					}

					tPlayer.loseLife(X, card);
					player.gainLife(X, card);
					card.setXManaCostPaid(0);
				}
				
				@Override
        		public void chooseTargetAI() {
        			setTargetPlayer(AllZone.HumanPlayer);
        		}//chooseTargetAI()
				
				@Override
        		public boolean canPlayAI() {
					Player player = getTargetPlayer();
        			PlayerZone grave = AllZone.getZone(Constant.Zone.Library, player);
        			CardList graveList = new CardList(grave.getCards());
        			
        			//int computerLife = AllZone.ComputerPlayer.getLife();

        			final int maxX = ComputerUtil.getAvailableMana().size() - 1;
        			return (maxX >= 3) && (graveList.size() > 0);
        		}
        	};
        	
        	spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
        	card.clearSpellAbility();
        	card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Machinate")) {
        	/* 
        	 * Look at the top X cards of your library, where X is the number
        	 * of artifacts you control. Put one of those cards into your hand
        	 * and the rest on the bottom of your library in any order.
        	 */
        	final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 5559004016728325736L;

				@Override
        		public void resolve() {
					Player player = card.getController();
        			CardList artifacts = AllZoneUtil.getPlayerCardsInPlay(player);
        			artifacts = artifacts.getType("Artifact");
        			AllZoneUtil.rearrangeTopOfLibrary(player, artifacts.size(), false);
        		}

        		@Override
        		public boolean canPlayAI() {
        			//basically the same reason as Sensei's Diving Top
        			return false;
        		}
        	};//spell
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Rearrange the top X cards in your library in any order.");
        	spell.setStackDescription(sb.toString());
        	card.clearSpellAbility();
        	card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Inferno")) {
        	SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 4728714298882795253L;

				@Override
        		public void resolve() {
        			int damage = 6;
        			CardList all = AllZoneUtil.getCreaturesInPlay();
        			for(Card c:all) {
        				c.addDamage(damage, card);
        			}
        			AllZone.ComputerPlayer.addDamage(damage, card);
        			AllZone.HumanPlayer.addDamage(damage, card);
        		}// resolve()

        		@Override
        		public boolean canPlayAI() {
        			CardList human = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
        			human = human.filter(powerSix);
        			human = human.getNotKeyword("Indestructible");
        			CardList computer = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
        			computer = computer.filter(powerSix);
        			computer = computer.getNotKeyword("Indestructible");
        			
        			// the computer will at least destroy 2 more human creatures
        			return  (AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && 
        			(computer.size() < human.size() - 1
        					|| (AllZone.ComputerPlayer.getLife() > 6 && !human.isEmpty())))
        					|| AllZone.HumanPlayer.getLife() < 7;
        		}
        		
        		private CardListFilter powerSix = new CardListFilter() {
        			public boolean addCard(Card c) {
        				return c.getNetDefense() <= 6;
        			}
        		};
        	};// SpellAbility
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Deal 6 damage to all creatures and all players.");
        	spell.setStackDescription(sb.toString());
        	
        	card.clearSpellAbility();
        	card.addSpellAbility(spell);
        }// *************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Demonic Consultation")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1481101852928051519L;

                @Override
                public void resolve() {
                	Player player = AllZone.Phase.getPlayerTurn();
                    PlayerZone PlayerHand = AllZone.getZone(Constant.Zone.Hand, player);
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    CardList libList = new CardList(lib.getCards());
                    final String[] input = new String[1];
                    input[0] = JOptionPane.showInputDialog(null, "Which card?", "Pick card", JOptionPane.QUESTION_MESSAGE);
                    
                    for(int i = 0; i < 7; i++) {
                        Card c = libList.get(i);
                        AllZone.GameAction.exile(c);
                    }

                    int max = libList.size();
                    int stop = 0;
                    for(int i = 0; i < max; i++) {
                        Card c = libList.get(i);
                        if(c.getName().equals(input[0])) {
                            if(stop == 0) {
                                AllZone.GameAction.moveTo(PlayerHand, c);
                                stop = 1;
                            }
                            
                        } else if(stop == 0) {
                            AllZone.GameAction.exile(c);
                        }
                    }
                }

                @Override
                public boolean canPlay() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());

                    return library.getCards().length > 6 && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility
            card.clearSpellAbility();
            spell.setStackDescription("Name a card. Exile the top six cards of your library, then reveal cards from the top of your library until you reveal the named card. Put that card into your hand and exile all other cards revealed this way");
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START ********** START *************************
        else if(cardName.equals("Mana Drain"))//NOTE: The AI can't cast this spell due to inability to use a manapool, but provisions are still made for it for if/when we get to that point.
        {
        	SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 6139754377230333678L;
                
                @Override
                public void resolve() {
                	SpellAbility sa = AllZone.Stack.pop();
                	
                	if(card.getController().equals(AllZone.HumanPlayer))
                	{
                		Phase.ManaDrain_BonusMana_Human.add(CardUtil.getConvertedManaCost(sa.getSourceCard()));
                		Phase.ManaDrain_Source_Human.add(card);
                	}
                	else if(card.getController().equals(AllZone.ComputerPlayer))
                	{
                		Phase.ManaDrain_BonusMana_AI.add(CardUtil.getConvertedManaCost(sa.getSourceCard()));
                		Phase.ManaDrain_Source_AI.add(card);        		
                	}
                }
                
                @Override
                public boolean canPlayAI()
                {
                	return false;
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.Stack.size() != 0)
                    {
                    	return AllZone.Stack.peek().isSpell();
                    }
                    else
                    {
                    	return false;
                    }
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Kaervek's Spite")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -6259614160639535500L;
                
                @Override
                public boolean canPlayAI() {
                    if(AllZone.HumanPlayer.getLife() <= 5) return true;
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer);
                    
                    CardList playList = new CardList(play.getCards());
                    CardList libList = new CardList(lib.getCards());
                    
                    playList = playList.getName("Academy Rector");
                    libList = libList.getName("Barren Glory");
                    
                    return (AllZone.HumanPlayer.getLife() <= 5) || (playList.size() == 1 && libList.size() >= 1);
                }
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.getName().equals("Mana Pool");
                        }
                    });
                    CardList handList = new CardList(hand.getCards());
                    
                    for(Card c:list) {
                        AllZone.GameAction.sacrifice(c);
                    }
                    card.getController().discardRandom(handList.size(), this);
                    
                    getTargetPlayer().loseLife(5, card);
                }
            };
            spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            /*
            final Command sac = new Command(){
            private static final long serialVersionUID = 1643946454479782123L;

            public void execute() {
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                PlayerZone hand = AllZone.getZone(Constant.Zone.Play, card.getController());
                CardList list = new CardList(play.getCards());
                list = list.filter(new CardListFilter()
                {
                    public boolean addCard(Card c) {
                        return !c.getName().equals("Mana Pool");
                    }
                });
                CardList handList = new CardList(hand.getCards());
                
                for (Card c : list)
                {
                    AllZone.GameAction.sacrifice(c);
                }
                AllZone.GameAction.discardRandom(card.getController(), handList.size());
            }
              
            };
            */

            spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if( cardName.equals("Siren's Call") ) {
            /**
             *  Creatures the active player controls attack this turn if able.
             *  
             *  At the beginning of the next end step, destroy all non-Wall creatures
             *  that player controls that didn't attack this turn. Ignore this effect
             *  for each creature the player didn't control continuously since the
             *  beginning of the turn.
             *  
             *  Note: I cheated a bit - they are destroyed at the end of combat since
             *  the getCreatureAttackedThisCombat is cleared at the end of combat, and
             *  as far as I know, this info is not available at EndOfTurn
             *  
             *  TODO - add getCreatureAttackedThisTurn function
             */
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = -5746330758531799264L;

				@Override
                public boolean canPlay() {
					return PhaseUtil.isBeforeAttackersAreDeclared() && AllZone.Phase.isPlayerTurn(card.getController().getOpponent());
                }//canPlay
				
				@Override
				public boolean canPlayAI() {
					return false;
				}//canPlayAI
                
                @Override
                public void resolve() {
                	//this needs to get a list of opponents creatures and set the siren flag
                	Player player = card.getController();
                	Player opponent = player.getOpponent();
                	CardList creatures = AllZoneUtil.getCreaturesInPlay(opponent);
                	for(Card creature:creatures) {
                		//skip walls, skip creatures with summoning sickness
                		//also skip creatures with haste if they came into play this turn
                		if((!creature.isWall() && !creature.hasSickness())
                				|| (creature.getKeyword().contains("Haste") && creature.getTurnInZone() != 1)) {
                			creature.setSirenAttackOrDestroy(true);
                			//System.out.println("Siren's Call - setting flag for "+creature.getName());
                		}
                	}
                	Command atEOT = new Command() {
						private static final long serialVersionUID = 5369528776959445848L;

						public void execute() {
							Player player = card.getController();
							Player opponent = player.getOpponent();
							CardList creatures = AllZoneUtil.getCreaturesInPlay(opponent);
							
							for(Card creature:creatures) {
								//System.out.println("Siren's Call - EOT - "+creature.getName() +" flag: "+creature.getSirenAttackOrDestroy());
								//System.out.println("Siren's Call - EOT - "+creature.getName() +" attacked?: "+creature.getCreatureAttackedThisCombat());
								if(creature.getSirenAttackOrDestroy() && !creature.getCreatureAttackedThisCombat()) {
									if(AllZone.GameAction.isCardInPlay(creature)) {
										//System.out.println("Siren's Call - destroying "+creature.getName());
										//this should probably go on the stack
										AllZone.GameAction.destroy(creature);
									}
								}
								creature.setSirenAttackOrDestroy(false);
							}
                        }//execute
                    };//Command
                    AllZone.EndOfCombat.addAt(atEOT);
                }//resolve
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - All creatures that can attack must do so or be destroyed.");
            spell.setStackDescription(sb.toString());
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if( cardName.equals("Reset") ) {
            /*
             * Cast Reset only during an opponent's turn after his or her upkeep step.
             * Untap all lands you control.
             */
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 1399682288920959188L;

				@Override
                public boolean canPlay() {
					Player opponent = card.getController().getOpponent();
					return Phase.canPlayAfterUpkeep() && AllZone.Phase.isPlayerTurn(opponent);
				}//canPlay
				
				@Override
				public boolean canPlayAI() {
					return false;
				}//canPlayAI
                
                @Override
                public void resolve() {
                	CardList lands = AllZoneUtil.getPlayerLandsInPlay(card.getController());
                	for(Card land:lands) land.untap();
                }
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - untap all lands you control.");
            spell.setStackDescription(sb.toString());
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mana Short")) {
            /*
             * Tap all lands target player controls and empty his or her mana pool.
             */
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = -2175586347805121896L;

				@Override
                public boolean canPlayAI() {
                    CardList lands = AllZoneUtil.getPlayerLandsInPlay(AllZone.HumanPlayer);
                    lands = lands.filter(AllZoneUtil.untapped);
                    return lands.size() > 0 || !AllZone.ManaPool.isEmpty();
                }
				
                @Override
                public void resolve() {
                	CardList lands = AllZoneUtil.getPlayerLandsInPlay(getTargetPlayer());
                	for(Card c:lands) c.tap();
                	if(getTargetPlayer().equals(AllZone.HumanPlayer)) AllZone.ManaPool.clearPool();
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);

            spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sacrifice")) {
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 7081747227572709229L;

				@Override
                public boolean canPlay() {
                    return AllZoneUtil.getCreaturesInPlay(card.getController()).size() > 0;
                }
                
                @Override
                public boolean canPlayAI() {
                	//Compy doesn't have a mana pool, so can't play this spell
                    return false;
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.GameAction.sacrifice(c);
                        int amt = CardUtil.getConvertedManaCost(c);
                        StringBuilder mana = new StringBuilder();
                        for(int i = 0; i < amt; i++) {
                        	mana.append("B ");
                        }
                        Card mp = AllZone.ManaPool;
                        mp.addExtrinsicKeyword("ManaPool:"+mana.toString());
                        
                    }//if isCardInPlay
                }
            };

            Input runtime = new Input() {
				private static final long serialVersionUID = 2544440783628551409L;

				@Override
                public void showMessage() {
                    CardList choice = AllZoneUtil.getCreaturesInPlay(card.getController());
                    
                    boolean free = false;
                    if(this.isFree()) free = true;
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, choice,
                            "Sacrifice - Select creature to sacrifice.", false, free));
                }
            };
            spell.setBeforePayMana(runtime);
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Brightstone Ritual")) {
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 7081747227572709229L;
                @Override
                public boolean canPlayAI() {
                	//Compy doesn't have a mana pool, so can't play this spell
                    return false;
                }
                
                @Override
                public void resolve() {
                	CardList goblins = AllZoneUtil.getTypeInPlay("Goblin");
                	StringBuilder mana = new StringBuilder();
                	for(int i = 0; i < goblins.size(); i++) {
                		mana.append("R ");
                	}
                	Card mp = AllZone.ManaPool;
                	mp.addExtrinsicKeyword("ManaPool:"+mana.toString());
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Berserk")) {
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = -4271469206538681785L;

				@Override
                public boolean canPlayAI() {
                	//computer doesn't use x spells very effectively
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                	final Card[] target = new Card[1];
                	target[0] = getTargetCard();
                	final int x = target[0].getNetAttack();
                    final Command untilEOT = new Command() {
						private static final long serialVersionUID = -3673524041113224182L;

						public void execute() {
                            if(AllZone.GameAction.isCardInPlay(target[0])) {
                                target[0].addTempAttackBoost(-x);
                                target[0].removeExtrinsicKeyword("Trample");
                                target[0].removeExtrinsicKeyword("At the beginning of the next end step, destroy CARDNAME if it attacked this turn.");
                            }
                        }
                    };
                    
                    
                    if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0])) {
                        target[0].addTempAttackBoost(x);
                        target[0].addExtrinsicKeyword("Trample");
                        target[0].addExtrinsicKeyword("At the beginning of the next end step, destroy CARDNAME if it attacked this turn.");
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    } else {

                    }
                }//resolve()
                
                @Override
                public boolean canPlay() {
                	CardList creatures = AllZoneUtil.getCreaturesInPlay();
                	return PhaseUtil.isBeforeCombatDamage() && creatures.size() > 0;
                }
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
    	return card;
    }//getCard
}
