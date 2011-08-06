
package forge;


import java.util.HashMap;
import java.util.List;

import com.esotericsoftware.minlog.Log;


class CardFactory_Planeswalkers {
    public static Card getCard(final Card card, String cardName, String owner) {
        //*************** START *********** START **************************
        if(cardName.equals("Elspeth, Knight-Errant")) {
            //computer only plays ability 1 and 3, put 1/1 Soldier in play and make everything indestructible
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 4));
            
            //ability2: target creature gets +3/+3 and flying until EOT
            final SpellAbility ability2 = new Ability(card2, "0") {
                
                @Override
                public void resolve() {
                    
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    
                    final Command eot = new Command() {
                        private static final long serialVersionUID = 94488363210770877L;
                        
                        public void execute() {
                            Card c = getTargetCard();
                            if(AllZone.GameAction.isCardInPlay(c)) {
                                c.addTempAttackBoost(-3);
                                c.addTempDefenseBoost(-3);
                                c.removeExtrinsicKeyword("Flying");
                            }
                        }//execute()
                    };//Command
                    
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card2, c)) {
                        c.addTempAttackBoost(3);
                        c.addTempDefenseBoost(3);
                        c.addExtrinsicKeyword("Flying");
                        
                        AllZone.EndOfTurn.addUntil(eot);
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                    

                }//canPlay()
            };//SpellAbility ability2
            
            ability2.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 9062830120519820799L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        
                        AllZone.Stack.push(ability2);
                    }
                    stop();
                }//showMessage()
            });
            

            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 8);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    Card emblem = new Card();
                    //should we even name this permanent?
                    //emblem.setName("Elspeth Emblem");
                    emblem.addIntrinsicKeyword("Indestructible");
                    emblem.addIntrinsicKeyword("Shroud");
                    emblem.addIntrinsicKeyword("Artifacts, creatures, enchantments, and lands you control are indestructible.");
                    emblem.setImmutable(true);
                    emblem.addType("Emblem");
                    emblem.setController(card2.getController());
                    emblem.setOwner(card2.getOwner());
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    play.add(emblem);
                    
                    //AllZone.GameAction.checkStateEffects();
                    AllZone.StaticEffects.rePopulateStateBasedList();
                    for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
                        Command com = GameActionUtil.commands.get(effect);
                        com.execute();
                    }  
                    
                    /*
                    //make all permanents in play/hand/library and graveyard	
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card2.getController());
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card2.getController());
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card2.getController());
                    
                    CardList list = new CardList();
                    list.addAll(play.getCards());
                    list.addAll(hand.getCards());
                    list.addAll(library.getCards());
                    list.addAll(grave.getCards());
                    

                    for(int i = 0; i < list.size(); i++) {
                        Card c = list.get(i);
                        if(c.isPermanent() && !c.isPlaneswalker()) {
                            c.addExtrinsicKeyword("Indestructible");
                        }
                        
                    }
                    */
                    
                }
                
                @Override
                public boolean canPlay() {
                    return 8 <= card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
                    list = list.filter(new CardListFilter(){
                    	public boolean addCard(Card c)
                    	{
                    		return c.isEmblem() && c.getKeyword().contains("Artifacts, creatures, enchantments, and lands you control are indestructible.");
                    	}
                    });
                	return list.size() == 0 && card2.getCounters(Counters.LOYALTY) > 8;
                }
            };
            ability3.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -2054686425541429389L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability3);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability 1: create white 1/1 token
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    CardFactoryUtil.makeToken("Soldier", "W 1 1 Soldier", card2, "W", new String[] {
                            "Creature", "Soldier"}, 1, 1, new String[] {""});
                }
                
                @Override
                public boolean canPlayAI() {
                    if(ability3.canPlay() && ability3.canPlayAI()) {
                        return false;
                    } else {
                        return true;
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };//SpellAbility ability1
            
            ability1.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -7892114885686285881L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
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
            
            ability3.setDescription("-8: You get an emblem with \"Artifacts, creatures, enchantments, and lands you control are indestructible.\"");
            ability3.setStackDescription("Elspeth, Knight-Errant - You get an emblem with \"Artifacts, creatures, enchantments, and lands you control are indestructible.\"");
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }
        //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Nissa Revane")) {
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 2));
            
            //ability2: gain 2 life for each elf controlled
            final SpellAbility ability2 = new Ability(card2, "0") {
                
                @Override
                public void resolve() {
                    
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    CardList elves = new CardList(play.getCards());
                    elves = elves.getType("Elf");
                    
                    AllZone.GameAction.gainLife(card.getController(), 2 * elves.size());
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
                    
                    CardList chosens = new CardList(lib.getCards());
                    chosens = chosens.getName("Nissa's Chosen");
                    
                    if(chosens.size() > 0) return false;
                    
                    return true;
                }
                
                @Override
                public boolean canPlay() {
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                    
                }//canPlay()
            };//SpellAbility ability2
            
            ability2.setBeforePayMana(new Input() {
                

                private static final long serialVersionUID = 2828718386226165026L;
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        

                        AllZone.Stack.push(ability2);
                    }
                    stop();
                }//showMessage()
            });
            

            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 7);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    //make all permanents in play/hand/library and graveyard	
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    CardList list = new CardList();;
                    list.addAll(library.getCards());
                    list = list.getType("Elf");
                    
                    if (card.getController().equals(Constant.Player.Human))
                    {
	                    List<Card> selection = AllZone.Display.getChoicesOptional("Select Elves to put into play", list.toArray());
	                    
	                    int numElves = selection.size();
	                    for(int m = 0; m < numElves; m++) {
	                    	Card c = selection.get(m);
	                    	library.remove(c);
	                    	play.add(c);
	                    }
                    }
                    else //computer
                    {
	                    for(int i = 0; i < list.size(); i++) {
	                        Card c = list.get(i);
	                        if(c.isCreature()) {
	                            library.remove(c);
	                            play.add(c);
	                        }
	                    }
                    }
                    
                }
                
                @Override
                public boolean canPlay() {
                    return 7 <= card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
                    
                    CardList elves = new CardList(lib.getCards());
                    elves = elves.getType("Elf");
                    
                    return elves.size() > 3;
                }
            };
            ability3.setBeforePayMana(new Input() {
                
                private static final long serialVersionUID = -7189927522150479572L;
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability3);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability 1: search for Nessa's Chosen
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    if(card2.getController().equals(Constant.Player.Human)) {
                        Object check = AllZone.Display.getChoiceOptional("Search for Nissa's Chosen",
                                AllZone.Human_Library.getCards());
                        if(check != null) {
                            Card c = (Card) check;
                            
                            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card2.getController());
                            
                            if(c.getName().equals("Nissa's Chosen")) {
                                lib.remove(c);
                                play.add(c);
                            }
                        }
                        AllZone.GameAction.shuffle(Constant.Player.Human);
                    }//human
                    else {
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card2.getController());
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                        CardList nissas = new CardList(lib.getCards());
                        nissas = nissas.getName("Nissa's Chosen");
                        
                        if(nissas.size() > 0) {
                            Card nissa = nissas.get(0);
                            lib.remove(nissa);
                            play.add(nissa);
                        }
                        AllZone.GameAction.shuffle(Constant.Player.Computer);
                    }
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    if(ability3.canPlay() && ability3.canPlayAI()) {
                        return false;
                    } else {
                        return true;
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };//SpellAbility ability1
            
            ability1.setBeforePayMana(new Input() {
                
                private static final long serialVersionUID = 7668642820407492396L;
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
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
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }
        //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Nicol Bolas, Planeswalker")) {
            
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 5));
            
            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 9);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    String player = card2.getController();
                    String opponent = AllZone.GameAction.getOpponent(player);
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);
                    CardList oppPerms = new CardList(play.getCards());
                    
                    //PlayerLife life = AllZone.GameAction.getPlayerLife(opponent);
                    //life.subtractLife(7,card2);
                    AllZone.GameAction.addDamage(opponent, card2, 7);
                    
                    for(int j = 0; j < 7; j++) {
                        //will not actually let human choose which cards to discard
                        AllZone.GameAction.discardRandom(opponent, this);
                    }
                    
                    CardList permsToSac = new CardList();
                    CardList oppPermTempList = new CardList(play.getCards());
                    oppPermTempList = oppPermTempList.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.getName().equals("Mana Pool");
                        }
                    });
                    
                    if(player.equals("Human")) {
                        for(int k = 0; k < oppPerms.size(); k++) {
                            Card c = oppPerms.get(k);
                            
                            permsToSac.add(c);
                            
                            if(k == 6) break;
                        }
                    }

                    else //computer
                    {
                        Object o = null;
                        for(int k = 0; k < oppPerms.size(); k++) {
                            o = AllZone.Display.getChoiceOptional("Select Card to sacrifice",
                                    oppPermTempList.toArray());
                            Card c = (Card) o;
                            //AllZone.GameAction.sacrifice(c);
                            permsToSac.add(c);
                            oppPermTempList.remove(c);
                            

                            if(k == 6) break;
                        }
                    }
                    for(int m = 0; m < permsToSac.size(); m++) {
                        AllZone.GameAction.sacrifice(permsToSac.get(m));
                    }
                    
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    return 9 <= card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    return true;
                }
            };
            ability3.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 2946754243072466628L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability3);
                    }
                    stop();
                }//showMessage()
            });
            
            final SpellAbility ability2 = new Ability(card2, "0") {
                
                @Override
                public void resolve() {
                    
                    card2.subtractCounter(Counters.LOYALTY, 2);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card2, c)) {
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
                    if(ability3.canPlay() && ability3.canPlayAI()) {
                        return false;
                    }
                    CardList c = CardFactoryUtil.AI_getHumanCreature(card, true);
                    CardListUtil.sortAttack(c);
                    CardListUtil.sortFlying(c);
                    
                    if(c.isEmpty()) return false;
                    
                    if(2 <= c.get(0).getNetAttack() && c.get(0).getKeyword().contains("Flying")) {
                        setTargetCard(c.get(0));
                        return true;
                    }
                    
                    CardListUtil.sortAttack(c);
                    if(4 <= c.get(0).getNetAttack()) {
                        setTargetCard(c.get(0));
                        return true;
                    }
                    
                    return false;
                }//canPlayAI()
                
                @Override
                public boolean canPlay() {
                    
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    return 2 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                    
                }//canPlay()
            };//SpellAbility ability2
            
            ability2.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -1877437173665495402L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        

                        AllZone.Stack.push(ability2);
                    }
                    stop();
                }//showMessage()
            });
            

            //ability 1: destroy target noncreature permanent
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 3);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    Card c = getTargetCard();
                    AllZone.GameAction.destroy(c);
                }
                
                @Override
                public boolean canPlayAI() {
                    if(ability3.canPlay() && ability3.canPlayAI() || getNonCreaturePermanent() == null) {
                        return false;
                    } else {
                        return true;
                    }
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public void chooseTargetAI() {
                    Card c = getNonCreaturePermanent();
                    
                    if(getNonCreaturePermanent() != null) setTargetCard(c);
                }//chooseTargetAI()
                
                Card getNonCreaturePermanent() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    int highestCost = 0;
                    Card bestCard = null;
                    CardList nonCreaturePermanents = new CardList(play.getCards());
                    nonCreaturePermanents = nonCreaturePermanents.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card2, c) && !c.isCreature();
                        }
                        
                    });
                    
                    for(int i = 0; i < nonCreaturePermanents.size(); i++) {
                        if(CardUtil.getConvertedManaCost(nonCreaturePermanents.get(i).getManaCost()) > highestCost) {
                            highestCost = CardUtil.getConvertedManaCost(nonCreaturePermanents.get(i).getManaCost());
                            bestCard = nonCreaturePermanents.get(i);
                        }
                    }
                    if(bestCard == null && nonCreaturePermanents.size() > 0) {
                        bestCard = nonCreaturePermanents.get(0);
                        return bestCard;
                    }
                    
                    return null;
                }
            };//SpellAbility ability1
            
            ability1.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 9167121234861249451L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
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
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }
        //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Ajani Goldmane")) {
            //computer only plays ability 1 and 3, gain life and put X\X token into play
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 4));
            
            //ability2: all controller's creatures get +1\+1 and vigilance until EOT
            final SpellAbility ability2 = new Ability(card2, "0") {
                final Command untilEOT = new Command() {
                                           private static final long serialVersionUID = -5436621445704076988L;
                                           
                                           public void execute() {
                                               String player = card2.getController();
                                               CardList creatures;
                                               if(player.equals(Constant.Player.Human)) {
                                                   creatures = new CardList(AllZone.Human_Play.getCards());
                                               } else {
                                                   creatures = new CardList(AllZone.Computer_Play.getCards());
                                               }
                                               
                                               creatures = creatures.getType("Creature");
                                               
                                               for(int i = 0; i < creatures.size(); i++) {
                                                   Card card = creatures.get(i);
                                                   //card.setAttack(card.getAttack() - 1);
                                                   //card.setDefense(card.getDefense() - 1);
                                                   card.removeExtrinsicKeyword("Vigilance");
                                               }
                                           }
                                       };
                
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    String player = card2.getController();
                    CardList creatures;
                    if(player.equals(Constant.Player.Human)) {
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
                    
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                    
                }//canPlay()
            };//SpellAbility ability2
            
            ability2.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 6373573398967821630L;
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability2);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 6);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    //Create token
                    int n = AllZone.GameAction.getPlayerLife(card.getController()).getLife();
                    CardFactoryUtil.makeToken("Avatar", "W N N Avatar", card2, "W", new String[] {
                            "Creature", "Avatar"}, n, n,
                            new String[] {"This creature's power and toughness are each equal to your life total"});
                }
                
                @Override
                public boolean canPlay() {
                    return 6 <= card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    // may be it's better to put only if you have less than 5 life
                    return true;
                }
            };
            ability3.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 7530960428366291386L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability3);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability 1: gain 2 life
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    

                    AllZone.GameAction.gainLife(card2.getController(), 2);
                    Log.debug("Ajani Goldmane", "current phase: " + AllZone.Phase.getPhase());
                }
                
                @Override
                public boolean canPlayAI() {
                    if(ability3.canPlay() && ability3.canPlayAI()) {
                        return false;
                    } else {
                        return true;
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };//SpellAbility ability1
            
            ability1.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -7969603493514210825L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability1);
                    }
                    stop();
                }//showMessage()
            });
            
            ability1.setDescription("+1: You gain 2 life.");
            ability1.setStackDescription("Ajani Goldmane - " + card2.getController() + " gains 2 life");
            card2.addSpellAbility(ability1);
            
            ability2.setDescription("-1: Put a +1/+1 counter on each creature you control. Those creatures gain vigilance until end of turn.");
            ability2.setStackDescription("Ajani Goldmane - Put a +1/+1 counter on each creature you control. They get vigilance.");
            card2.addSpellAbility(ability2);
            
            ability3.setDescription("-6: Put a white Avatar creature token into play with \"This creature's power and toughness are each equal to your life total.\"");
            ability3.setStackDescription("Ajani Goldmane - Put X\\X white Avatar creature token into play.");
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }
        //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Liliana Vess")) {
            //computer only plays ability 1 and 3, discard and return creature from graveyard to play
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 5));
            
            //ability2
            final SpellAbility ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 2);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    String player = card2.getController();
                    if(player.equals(Constant.Player.Human)) humanResolve();
                    else computerResolve();
                }
                
                public void computerResolve() {
                    CardList creature = new CardList(AllZone.Computer_Library.getCards());
                    creature = creature.getType("Creature");
                    if(creature.size() != 0) {
                        Card c = creature.get(0);
                        AllZone.GameAction.shuffle(card2.getController());
                        
                        //move to top of library
                        AllZone.Computer_Library.remove(c);
                        AllZone.Computer_Library.add(c, 0);
                    }
                }//computerResolve()
                
                public void humanResolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card2.getController());
                    
                    CardList list = new CardList(library.getCards());
                    
                    if(list.size() != 0) {
                        Object o = AllZone.Display.getChoiceOptional("Select any card", list.toArray());
                        
                        AllZone.GameAction.shuffle(card2.getController());
                        if(o != null) {
                            //put creature on top of library
                            library.remove(o);
                            library.add((Card) o, 0);
                        }
                    }//if
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card2.getController());
                    
                    return 2 <= card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && 1 < library.size()
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };//SpellAbility ability2
            
            ability2.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 5726590384281714755L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability2);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
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
                    for(int i = 0; i < list.size(); i++) {
                        //this is a rough hack, but no one will ever see this code anyways, lol ;+)
                        c = list.get(i);
                        c.setController(card.getController());
                        
                        grave = AllZone.getZone(c);
                        if(grave != null) grave.remove(c);
                        
                        play.add(c);
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return 8 <= card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Graveyard.getCards());
                    list.addAll(AllZone.Computer_Graveyard.getCards());
                    list = list.getType("Creature");
                    
                    return 3 < list.size();
                }
            };
            ability3.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -3297439284172874241L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability3);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability 1
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    String s = getTargetPlayer();
                    setStackDescription("Liliana Vess - " + s + " discards a card");
                    
                    if(s.equals(Constant.Player.Human)) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
                    else AllZone.GameAction.discardRandom(Constant.Player.Computer, this);
                }
                
                @Override
                public boolean canPlayAI() {
                    if(ability3.canPlay() && ability3.canPlayAI()) return false;
                    else {
                        setTargetPlayer(Constant.Player.Human);
                        return true;
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };//SpellAbility ability1
            
            Input target = new Input() {
                private static final long serialVersionUID = 4997055112713151705L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target player");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectPlayer(String player) {
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
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }
        //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Chandra Nalaar")) {
            //computer only plays ability 1 and 3, discard and return creature from graveyard to play
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 6));
            
            //ability 1
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card2, getTargetCard())) {
                            Card c = getTargetCard();
                            if(CardFactoryUtil.canDamage(card2, c)) c.addDamage(1, card2);
                        }
                    }

                    else {
                        //PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
                        //life.subtractLife(1,card2);
                    	AllZone.GameAction.addDamage(getTargetPlayer(), card2, 1);
                    }
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(Constant.Player.Human);
                    setStackDescription("Chandra Nalaar - deals 1 damage to " + Constant.Player.Human);
                    return card2.getCounters(Counters.LOYALTY) < 8;
                }
            };//SpellAbility ability1
            
            Input target1 = new Input() {
                private static final long serialVersionUID = 5263705146686766284L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target Player or Planeswalker");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(card.isPlaneswalker() && zone.is(Constant.Zone.Play) &&
                       CardFactoryUtil.canTarget(card2, card)) {
                        ability1.setTargetCard(card);
                        //stopSetNext(new Input_PayManaCost(ability1));
                        AllZone.Stack.add(ability1);
                        stop();
                    }
                }//selectCard()
                
                @Override
                public void selectPlayer(String player) {
                    ability1.setTargetPlayer(player);
                    //stopSetNext(new Input_PayManaCost(ability1));
                    AllZone.Stack.add(ability1);
                    stop();
                }
            };
            ability1.setBeforePayMana(target1);
            ability1.setDescription("+1: Chandra Nalaar deals 1 damage to target player.");
            card2.addSpellAbility(ability1);
            //end ability1
            
            //ability 2
            final int damage2[] = new int[1];
            
            final SpellAbility ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.Phase.getTurn();
                    
                    card2.subtractCounter(Counters.LOYALTY, damage2[0]);
                    if(CardFactoryUtil.canDamage(card2, getTargetCard())) getTargetCard().addDamage(damage2[0],
                            card2);
                    
                    damage2[0] = 0;
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility ability2
            
            Input target2 = new Input() {
                private static final long serialVersionUID = -2160464080456452897L;
                
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
                    } else if(c.isCreature()) {
                        turn[0] = AllZone.Phase.getTurn();
                        

                        damage2[0] = getDamage();
                        
                        ability2.setTargetCard(c);
                        ability2.setStackDescription("Chandra Nalaar - deals damage to " + c);
                        
                        AllZone.Stack.add(ability2);
                        stop();
                    }
                }//selectCard()
                
                int getDamage() {
                    int size = card2.getCounters(Counters.LOYALTY);
                    Object choice[] = new Object[size];
                    
                    for(int i = 0; i < choice.length; i++)
                        choice[i] = Integer.valueOf(i + 1);
                    
                    Integer damage = (Integer) AllZone.Display.getChoice("Select X", choice);
                    return damage.intValue();
                }
            };//Input target
            ability2.setBeforePayMana(target2);
            ability2.setDescription("-X: Chandra Nalaar deals X damage to target creature.");
            card2.addSpellAbility(ability2);
            //end ability2
            

            //ability 3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 8);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    //PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
                    //life.subtractLife(10,card2);
                    
                    AllZone.GameAction.addDamage(getTargetPlayer(), card2, 10);
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, getTargetPlayer());
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canDamage(card2, list.get(i))) list.get(i).addDamage(10, card2);
                    }
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && 7 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(Constant.Player.Human);
                    setStackDescription("Chandra Nalaar - deals 10 damage to " + Constant.Player.Human
                            + " and each creature he or she controls.");
                    return true;
                }
            };//SpellAbility ability3
            
            Input target3 = new Input() {
                private static final long serialVersionUID = -3014450919506364666L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target player");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectPlayer(String player) {
                    turn[0] = AllZone.Phase.getTurn();
                    
                    ability3.setTargetPlayer(player);
                    ability3.setStackDescription("Chandra Nalaar - deals 10 damage to " + player
                            + " and each creature he or she controls.");
                    
                    AllZone.Stack.add(ability3);
                    stop();
                }
            };//Input target
            ability3.setBeforePayMana(target3);
            ability3.setDescription("-8: Chandra Nalaar deals 10 damage to target player and each creature he or she controls.");
            card2.addSpellAbility(ability3);
            //end ability3
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }
        //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Garruk Wildspeaker")) {
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 3));
            
            //ability1
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    
                    //only computer uses the stack
                    CardList tapped = new CardList(AllZone.Computer_Play.getCards());
                    tapped = tapped.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand() && c.isTapped();
                        }
                    });
                    
                    for(int i = 0; i < 2 && i < tapped.size(); i++)
                        tapped.get(i).untap();
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return card2.getCounters(Counters.LOYALTY) < 4
                            && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            final Input targetLand = new Input() {
                private static final long serialVersionUID = -6609158314106861676L;
                
                private int               count;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select a land to untap");
                    ButtonUtil.disableAll();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(c.isLand() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card2, c)) {
                        count++;
                        c.untap();
                    }
                    
                    //doesn't use the stack, its just easier this way
                    if(count == 2) {
                        count = 0;
                        turn[0] = AllZone.Phase.getTurn();
                        card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                        stop();
                    }
                }//selectCard()
            };//Input
            
            Input runtime1 = new Input() {
                private static final long serialVersionUID = 8709088526618867662L;
                
                @Override
                public void showMessage() {
                    stopSetNext(targetLand);
                }
            };//Input
            ability1.setDescription("+1: Untap two target lands.");
            ability1.setStackDescription("Garruk Wildspeaker - Untap two target lands.");
            
            ability1.setBeforePayMana(runtime1);
            card2.addSpellAbility(ability1);
            //end ability 1
            

            //start ability 2
            final SpellAbility ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    CardFactoryUtil.makeToken("Beast", "G 3 3 Beast", card2, "G", new String[] {
                            "Creature", "Beast"}, 3, 3, new String[] {""});
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    CardList c = new CardList(AllZone.Computer_Play.getCards());
                    c = c.getType("Creature");
                    return c.size() < 4;
                }
            };//SpellAbility ability 2
            Input runtime2 = new Input() {
                private static final long serialVersionUID = -1718455991391244845L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        
                        AllZone.Stack.push(ability2);
                        stop();
                    }
                }
            };//Input
            ability2.setStackDescription(card2.getName() + " -  Put a 3/3 green Beast creature token into play.");
            ability2.setDescription("-1: Put a 3/3 green Beast creature token into play.");
            ability2.setBeforePayMana(runtime2);
            card2.addSpellAbility(ability2);
            //end ability 2
            

            //start ability 3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 4);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    final int boost = 3;
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    CardList list = new CardList(play.getCards());
                    @SuppressWarnings("unused")
                    // c
                    Card c;
                    
                    for(int i = 0; i < list.size(); i++) {
                        final Card[] target = new Card[1];
                        target[0] = list.get(i);
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = 478068133055335098L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(target[0])) {
                                    target[0].addTempAttackBoost(-boost);
                                    target[0].addTempDefenseBoost(-boost);
                                    
                                    target[0].removeExtrinsicKeyword("Trample");
                                }
                            }
                        };//Command
                        
                        if(AllZone.GameAction.isCardInPlay(target[0])) {
                            target[0].addTempAttackBoost(boost);
                            target[0].addTempDefenseBoost(boost);
                            
                            target[0].addExtrinsicKeyword("Trample");
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }//if
                    }//for
                    
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && 3 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    CardList c = new CardList(AllZone.Computer_Play.getCards());
                    c = c.getType("Creature");
                    return c.size() >= 4 && AllZone.Phase.getPhase().equals(Constant.Phase.Main1)
                            && AllZone.Phase.getActivePlayer().equals(card2.getController());
                }
            };//SpellAbility ability3
            Input runtime3 = new Input() {
                private static final long serialVersionUID = 7697504647440222302L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        
                        AllZone.Stack.push(ability3);
                        stop();
                    }
                }
            };//Input
            ability3.setStackDescription(card2.getName()
                    + " -  Creatures you control get +3/+3 and trample until end of turn.");
            ability3.setDescription("-4: Creatures you control get +3/+3 and trample until end of turn.");
            ability3.setBeforePayMana(runtime3);
            card2.addSpellAbility(ability3);
            //end ability 3
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Jace Beleren")) {
            
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 3));
            
            //ability1
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 2);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    
                    AllZone.GameAction.drawCard(Constant.Player.Computer);
                    AllZone.GameAction.drawCard(Constant.Player.Human);
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return card2.getCounters(Counters.LOYALTY) < 11
                            && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            
            ability1.setDescription("+2: Each player draws a card.");
            ability1.setStackDescription(cardName + " - Each player draws a card.");
            
            //ability2
            final SpellAbility ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 1);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    String player = getTargetPlayer();
                    
                    AllZone.GameAction.drawCard(player);
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && card2.getCounters(Counters.LOYALTY) >= 1
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            ability2.setDescription("-1: Target player draws a card.");
            ability2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability2));
            
            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 10);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    AllZone.GameAction.mill(getTargetPlayer(),20);
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(Constant.Player.Human);
                    return card2.getCounters(Counters.LOYALTY) >= 11;
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && card2.getCounters(Counters.LOYALTY) >= 10
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            ability3.setDescription("-10: Target player puts the top twenty cards of his or her library into his or her graveyard.");
            ability3.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability3));
            
            card2.addSpellAbility(ability1);
            card2.addSpellAbility(ability2);
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Ajani Vengeant")) {
            
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 3));
            

            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                	Card c = getTargetCard();
                	if (c != null) 
                	{		
	                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
	                    turn[0] = AllZone.Phase.getTurn();
	                    c.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
                	}
                }
                
                @Override
                public boolean canPlayAI() {
                	CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
                	list = list.filter(new CardListFilter()
                	{
                		public boolean addCard(Card c)
                		{
                			return CardFactoryUtil.canTarget(card2, c);
                		}
                	});
                	if (list.size() > 0) {
                		CardListUtil.sortCMC(list);
                		setTargetCard(list.get(0));
                	}
                	
                    return card2.getCounters(Counters.LOYALTY) < 8 && list.size() > 0;
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                
                
                /*
                Card getPermanent() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    int highestCost = 0;
                    Card bestCard = null;
                    CardList perms = new CardList(play.getCards());
                    perms = perms.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card2, c);
                        }
                    });
                    
                    for(int i = 0; i < perms.size(); i++) {
                        if(CardUtil.getConvertedManaCost(perms.get(i).getManaCost()) > highestCost
                                && perms.get(i).isTapped()) {
                            highestCost = CardUtil.getConvertedManaCost(perms.get(i).getManaCost());
                            bestCard = perms.get(i);
                        }
                    }
                    if(bestCard == null && perms.size() > 0) {
                        bestCard = perms.get(0);
                        return bestCard;
                    }
                    
                    return null;
                }
                */
    			
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
            ability1.setDescription("+1: Target permanent doesn't untap during its controller's next untap step.");
            ability1.setBeforePayMana(CardFactoryUtil.input_targetPermanent(ability1));
            

            final Ability ability2 = new Ability(card, "0") {
                int damage = 3;
                
                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(Constant.Player.Human);
                    return AllZone.Human_Life.getLife() <= damage;
                    
                }
                
                @Override
                public void resolve() {
                	turn[0] = AllZone.Phase.getTurn();
                    card2.subtractCounter(Counters.LOYALTY, 2);
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card2, getTargetCard())) {
                            Card c = getTargetCard();
                            c.addDamage(damage, card2);
                        }
                    } 
                    else 
                    	AllZone.GameAction.addDamage(getTargetPlayer(), card2, damage);
                    
                    AllZone.GameAction.gainLife(card2.getController(), 3);
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && card2.getCounters(Counters.LOYALTY) >= 2
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
            };//ability2
            
            Input target = new Input() {
                private static final long serialVersionUID = -6688689065812475609L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target Creature, Player or Planeswalker");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(ability2, card)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if((card.isCreature() || card.isPlaneswalker()) && zone.is(Constant.Zone.Play)) {
                        ability2.setTargetCard(card);
                        //stopSetNext(new Input_PayManaCost(ability2));
                        AllZone.Stack.add(ability2);
                        stop();
                    }
                }//selectCard()
                
                @Override
                public void selectPlayer(String player) {
                    ability2.setTargetPlayer(player);
                    //stopSetNext(new Input_PayManaCost(ability2));
                    AllZone.Stack.add(ability2);
                    stop();
                }
            };
            ability2.setBeforePayMana(target);
            ability2.setDescription("-2: Ajani Vengeant deals 3 damage to target creature or player and you gain 3 life.");
            

            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 7);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    String player = getTargetPlayer();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    CardList land = new CardList(play.getCards());
                    land = land.getType("Land");
                    
                    for(Card c:land) {
                        AllZone.GameAction.destroy(c);
                    }
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone pz = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList land = new CardList(pz.getCards());
                    land = land.getType("Land");
                    
                    setTargetPlayer(Constant.Player.Human);
                    return card2.getCounters(Counters.LOYALTY) >= 8 && land.size() >= 4;
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && card2.getCounters(Counters.LOYALTY) >= 7
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            ability3.setDescription("-7: Destroy all lands target player controls.");
            ability3.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability3));
            
            card2.addSpellAbility(ability1);
            card2.addSpellAbility(ability2);
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Tezzeret the Seeker")) {
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 4));
            
            //ability1
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    
                    //only computer uses the stack
                    CardList tapped = new CardList(AllZone.Computer_Play.getCards());
                    tapped = tapped.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() && c.isTapped() && CardFactoryUtil.canTarget(card2, c);
                        }
                    });
                    
                    for(int i = 0; i < 2 && i < tapped.size(); i++)
                        tapped.get(i).untap();
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return card2.getCounters(Counters.LOYALTY) <= 6
                            && AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            final Input targetArtifact = new Input() {
                
                private static final long serialVersionUID = -7915255038817192835L;
                private int               count;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select an artifact to untap");
                    ButtonUtil.disableAll();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(c.isArtifact() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card2, c)) {
                        count++;
                        c.untap();
                    }
                    
                    //doesn't use the stack, its just easier this way
                    if(count == 2) {
                        count = 0;
                        turn[0] = AllZone.Phase.getTurn();
                        card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                        stop();
                    }
                }//selectCard()
            };//Input
            
            Input runtime1 = new Input() {
                private static final long serialVersionUID = 871304623687370615L;
                
                @Override
                public void showMessage() {
                    stopSetNext(targetArtifact);
                }
            };//Input
            ability1.setDescription("+1: Untap up to two target artifacts.");
            ability1.setStackDescription("Tezzeret the Seeker - Untap two target artifacts.");
            
            ability1.setBeforePayMana(runtime1);
            card2.addSpellAbility(ability1);
            //end ability 1
            

            //ability 2
            final SpellAbility ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.Phase.getTurn();
                    
                    int size = card2.getCounters(Counters.LOYALTY) + 1;
                    Object choice[] = new Object[size];
                    
                    for(int i = 0; i < choice.length; i++)
                        choice[i] = Integer.valueOf(i);
                    
                    Integer damage = (Integer) AllZone.Display.getChoice("Select X", choice);
                    final int dam = damage.intValue();
                    
                    card2.subtractCounter(Counters.LOYALTY, dam);
                    
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card2.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    CardList list = new CardList(lib.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() && CardUtil.getConvertedManaCost(c.getManaCost()) <= dam;
                        }
                    });
                    
                    if(list.size() > 0) {
                        Object o = AllZone.Display.getChoiceOptional("Select artifact",
                                AllZone.Human_Library.getCards());
                        if(o != null) {
                            Card c = (Card) o;
                            if(list.contains(c)) {
                                lib.remove(c);
                                play.add(c);
                            }
                        }
                    }
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility ability2
            ability2.setDescription("-X: Search your library for an artifact card with converted mana cost X or less and put it onto the battlefield. Then shuffle your library.");
            ability2.setStackDescription(card2.getName()
                    + " - Search your library for an artifact card with converted mana cost X or less and put it onto the battlefield. Then shuffle your library.");
            card2.addSpellAbility(ability2);
            

            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    
                    card2.subtractCounter(Counters.LOYALTY, 5);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Artifact");
                    CardList creatures = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature();
                        }
                    });
                    
                    //final Card[] tempCards = new Card[creatures.size()];
                    final HashMap<Integer, Card> tempCardMap = new HashMap<Integer, Card>();
                    
                    for(Card creatureCard:creatures) {
                        Card crd = copyStats(creatureCard);
                        tempCardMap.put(creatureCard.getUniqueNumber(), crd);
                        //System.out.println("Just added:" + crd);
                    }
                    
                    for(Card c:list) {
                        final Card[] art = new Card[1];
                        art[0] = c;
                        if(AllZone.GameAction.isCardInPlay(art[0])) {
                            if(c.isCreature()) {
                                //Card crd = copyStats(art[0]);
                                //tempCards[c.getUniqueNumber()] = crd;
                                
                                final Command creatureUntilEOT = new Command() {
                                    private static final long serialVersionUID = 5063161656920609389L;
                                    
                                    public void execute() {
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
                            } else {
                                final Command nonCreatureUntilEOT = new Command() {
                                    private static final long serialVersionUID = 248122386218960073L;
                                    
                                    public void execute() {
                                        art[0].removeType("Creature");
                                        art[0].setBaseAttack(0);
                                        art[0].setBaseDefense(0);
                                    }
                                };//Command
                                
                                if(art[0].isEquipment() && art[0].isEquipping()) {
                                    Card equippedCreature = art[0].getEquipping().get(0);
                                    art[0].unEquipCard(equippedCreature);
                                }
                                art[0].addType("Creature");
                                art[0].setBaseAttack(5);
                                art[0].setBaseDefense(5);
                                
                                AllZone.EndOfTurn.addUntil(nonCreatureUntilEOT);
                            }//noncreature artifact
                            
                        }
                    }//for
                }//resolve
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && card2.getCounters(Counters.LOYALTY) >= 5
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    CardList list = new CardList(play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact()
                                    && (!c.isCreature() || (c.isCreature() && c.getBaseAttack() < 4))
                                    && !c.hasSickness();
                        }
                    });
                    return list.size() > 4 && AllZone.Phase.getPhase().equals("Main1")
                            && card2.getCounters(Counters.LOYALTY) > 5;
                }
            };
            ability3.setDescription("-5: Artifacts you control become 5/5 artifact creatures until end of turn.");
            ability3.setStackDescription(card2.getName()
                    + " - Artifacts you control become 5/5 artifact creatures until end of turn.");
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sarkhan Vol")) {
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 4));
            
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    final int boost = 1;
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        final Card[] target = new Card[1];
                        target[0] = list.get(i);
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = 2893066467461166183L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(target[0])) {
                                    target[0].addTempAttackBoost(-boost);
                                    target[0].addTempDefenseBoost(-boost);
                                    
                                    target[0].removeExtrinsicKeyword("Haste");
                                }
                            }
                        };//Command
                        
                        if(AllZone.GameAction.isCardInPlay(target[0])) {
                            target[0].addTempAttackBoost(boost);
                            target[0].addTempDefenseBoost(boost);
                            
                            target[0].addExtrinsicKeyword("Haste");
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }//if
                    }//for
                    
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    return AllZone.Phase.getPhase().equals(Constant.Phase.Main1)
                            && card2.getCounters(Counters.LOYALTY) < 7;
                }
            };//ability1
            Input runtime1 = new Input() {
                private static final long serialVersionUID = 3843631106383444950L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        
                        AllZone.Stack.push(ability1);
                        stop();
                    }
                }
            };//Input
            ability1.setStackDescription(card2.getName()
                    + " - Creatures you control get +1/+1 and gain haste until end of turn.");
            ability1.setDescription("+1: Creatures you control get +1/+1 and gain haste until end of turn.");
            ability1.setBeforePayMana(runtime1);
            card2.addSpellAbility(ability1);
            
            final PlayerZone[] orig = new PlayerZone[1];
            final PlayerZone[] temp = new PlayerZone[1];
            final String[] controllerEOT = new String[1];
            final Card[] target = new Card[1];
            
            final Command untilEOT = new Command() {
                
                private static final long serialVersionUID = -815595604846219653L;
                
                public void execute() {
                    //if card isn't in play, do nothing
                    if(!AllZone.GameAction.isCardInPlay(target[0])) return;
                    
                    target[0].setController(controllerEOT[0]);
                    
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                    
                    //moveTo() makes a new card, so you don't have to remove "Haste"
                    //AllZone.GameAction.moveTo(playEOT[0], target[0]);
                    temp[0].remove(target[0]);
                    orig[0].add(target[0]);
                    target[0].untap();
                    target[0].removeExtrinsicKeyword("Haste");
                    
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                }//execute()
            };//Command
            
            final Ability ability2 = new Ability(card, "0") {
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card2, getTargetCard())) {
                        card2.subtractCounter(Counters.LOYALTY, 2);
                        turn[0] = AllZone.Phase.getTurn();
                        
                        orig[0] = AllZone.getZone(getTargetCard());
                        controllerEOT[0] = getTargetCard().getController();
                        target[0] = getTargetCard();
                        
                        //set the controller
                        getTargetCard().setController(card.getController());
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                        
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                        play.add(getTargetCard());
                        temp[0] = play;
                        orig[0].remove(getTargetCard());
                        
                        ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                        ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                        

                        getTargetCard().untap();
                        getTargetCard().addExtrinsicKeyword("Haste");
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }//is card in play?
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }//canPlayAI()
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && card2.getCounters(Counters.LOYALTY) >= 2
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }
            };//SpellAbility
            ability2.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability2));
            ability2.setDescription("-2: Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn.");
            card2.addSpellAbility(ability2);
            
            //ability3
            final Ability ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 6);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    for(int i = 0; i < 5; i++)
                        CardFactoryUtil.makeToken("Dragon", "R 4 4 Dragon", card2, "R", new String[] {
                                "Creature", "Dragon"}, 4, 4, new String[] {"Flying"});
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && card2.getCounters(Counters.LOYALTY) >= 6
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    return card2.getCounters(Counters.LOYALTY) > 6;
                }
            };//ability3
            ability3.setStackDescription(card2.getName()
                    + " - Put five 4/4 red Dragon creature tokens with flying onto the battlefield.");
            ability3.setDescription("-6: Put five 4/4 red Dragon creature tokens with flying onto the battlefield.");
            card2.addSpellAbility(ability3);
            //end ability 3
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }//*************** END ************ END **************************    
        

        //*************** START *********** START **************************
        if(cardName.equals("Jace, the Mind Sculptor")) {
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 3));
            /*
            card2.addComesIntoPlayCommand(new Command() {
            	public void execute()
            	{
            		turn[0] = -1;
            	}
            });
            */
            
            final Ability ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.Phase.getTurn();
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 2);
                    String targetPlayer = getTargetPlayer();
                    
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, targetPlayer);
                    
                    if(lib.size() == 0) return;
                    
                    Card c = lib.get(0);
                    
                    if(card2.getController().equals(Constant.Player.Human)) {
                        
                        String[] choices = {"Yes", "No"};
                        Object choice = AllZone.Display.getChoice("Put " + c + " on bottom of owner's library?",
                                choices);
                        if(choice != null) {
                            if(choice.equals("Yes")) {
                                lib.remove(c);
                                lib.add(c);
                            }
                        }
                    } else //compy
                    {
                        PlayerZone humanPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                        CardList land = new CardList(humanPlay.getCards());
                        land = land.getType("Land");
                        
                        //TODO: improve this:
                        if(land.size() > 4 && c.isLand()) ;
                        else {
                            lib.remove(c);
                            lib.add(c);
                        }
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return card2.getCounters(Counters.LOYALTY) < 13 && AllZone.Human_Library.size() > 2;
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            ability1.setDescription("+2: Look at the top card of target player's library. You may put that card on the bottom of that player's library.");
            ability1.setStackDescription(card2
                    + " - Look at the top card of target player's library. You may put that card on the bottom of that player's library.");
            ability1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability1));
            ability1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card2.addSpellAbility(ability1);
            
            final Ability ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.Phase.getTurn();
                    AllZone.GameAction.drawCard(card.getController());
                    AllZone.GameAction.drawCard(card.getController());
                    AllZone.GameAction.drawCard(card.getController());
                    
                    String player = card.getController();
                    if(player.equals(Constant.Player.Human)) humanResolve();
                    //else
                    //  computerResolve();
                }
                
                public void humanResolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human);
                    
                    CardList putOnTop = new CardList(hand.getCards());
                    
                    Object o = AllZone.Display.getChoiceOptional("First card to put on top: ", putOnTop.toArray());
                    if(o != null) {
                        Card c1 = (Card) o;
                        putOnTop.remove(c1);
                        hand.remove(c1);
                        lib.add(c1, 0);
                    }
                    o = AllZone.Display.getChoiceOptional("Second card to put on top: ", putOnTop.toArray());
                    if(o != null) {
                        Card c2 = (Card) o;
                        putOnTop.remove(c2);
                        hand.remove(c2);
                        lib.add(c2, 0);
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            ability2.setDescription("0: Draw three cards, then put two cards from your hand on top of your library in any order.");
            ability2.setStackDescription(card2
                    + " - Draw three cards, then put two cards from your hand on top of your library in any order.");
            card2.addSpellAbility(ability2);
            
            final Ability ability3 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.Phase.getTurn();
                    card2.subtractCounter(Counters.LOYALTY, 1);
                    
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        if(getTargetCard().isToken()) AllZone.getZone(getTargetCard()).remove(getTargetCard());
                        else {
                            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
                            AllZone.GameAction.moveTo(hand, getTargetCard());
                        }
                    }//if
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    return card2.getCounters(Counters.LOYALTY) >= 1
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }
            };
            ability3.setDescription("-1: Return target creature to its owner's hand.");
            ability3.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability3));
            card2.addSpellAbility(ability3);
            
            final Ability ability4 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.Phase.getTurn();
                    card2.subtractCounter(Counters.LOYALTY, 12);
                    
                    String player = getTargetPlayer();
                    
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
                    
                    CardList libList = new CardList(lib.getCards());
                    CardList handList = new CardList(hand.getCards());
                    
                    for(Card c:libList)
                        AllZone.GameAction.removeFromGame(c);
                    
                    handList.shuffle();
                    for(Card c:handList) {
                        hand.remove(c);
                        lib.add(c);
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    int libSize = AllZone.Human_Library.size();
                    int handSize = AllZone.Human_Hand.size();
                    return libSize > 10 && (libSize > handSize);
                }
                
                @Override
                public boolean canPlay() {
                    return card2.getCounters(Counters.LOYALTY) >= 12
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }
            };
            ability4.setDescription("-12: Exile all cards from target player's library, then that player shuffles his or her hand into his or her library.");
            ability4.setStackDescription(card2
                    + " - Exile all cards from target player's library, then that player shuffles his or her hand into his or her library.");
            ability4.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability4));
            ability4.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card2.addSpellAbility(ability4);
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if(cardName.equals("Sarkhan the Mad")) {

        	//Planeswalker book-keeping
        	final int turn[] = new int[1];
        	turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            //Sarkhan starts with 7 loyalty counters
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 7));
            
            //ability1
            /*
             * 0: Reveal the top card of your library and put it into your hand. Sarkhan
             * the Mad deals damage to himself equal to that card's converted mana cost.
             */
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 0);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    final String player = card.getController();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
                    Card topCard = lib.get(0);
                    int convertedManaTopCard = CardUtil.getConvertedManaCost(topCard.getManaCost());
                    CardList showTop = new CardList();
                    showTop.add(topCard);
                    AllZone.Display.getChoiceOptional("Revealed top card: ", showTop.toArray());
                    
                    //now, move it to player's hand
                    lib.remove(topCard);
                    hand.add(topCard);                    
                    
                    //now, do X damage to Sarkhan
                    card2.addDamage(convertedManaTopCard, card);
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                	//the computer isn't really smart enough to play this effectively, and it doesn't really
                	//help unless there are no cards in his hand
                	return false;
                }
                
                @Override
                public boolean canPlay() {
                	//looks like standard Planeswalker stuff...
                	//maybe should check if library is empty, or 1 card?
                	final String player = card.getController();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0 && lib.size() != 0;
                }//canPlay()
            };
            ability1.setDescription("0: Reveal the top card of your library and put it into your hand. Sarkhan the Mad deals damage to himself equal to that card's converted mana cost.");
            ability1.setStackDescription(cardName + " - Reveal top card and do damage.");
            
            //ability2
            /*
             * -2: Target creature's controller sacrifices it, then that player puts a 5/5 red Dragon
             * creature token with flying onto the battlefield.
             */
            final SpellAbility ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 2);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    Card target = getTargetCard();
                    AllZone.GameAction.sacrifice(target);
                    //in makeToken, use target for source, so it goes into the correct Zone
                    CardFactoryUtil.makeToken("Dragon", "R 5 5 Dragon", target, "", new String[] {"Creature", "Dragon"}, 5, 5, new String[] {"Flying"});
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    CardList creatures = new CardList(play.getCards());
                    creatures = creatures.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return c.isCreature();
                    	}
                    });
                	return creatures.size() >= 1;
                }
                
                @Override
                public void chooseTargetAI() {
                	PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    CardList cards = new CardList(play.getCards());
                    //avoid targeting the dragon tokens we just put in play...
                    cards = cards.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return !(c.isToken() && c.getType().contains("Dragon"));
                    	}
                    });
                	setTargetCard(CardFactoryUtil.AI_getCheapestCreature(cards, card2, true));
                	Log.debug("Sarkhan the Mad", "Sarkhan the Mad caused sacrifice of: "+
                			CardFactoryUtil.AI_getCheapestCreature(cards, card2, true).getName());
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && card2.getCounters(Counters.LOYALTY) >= 2
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            ability2.setDescription("-2: Target creature's controller sacrifices it, then that player puts a 5/5 red Dragon creature token with flying onto the battlefield.");
            ability2.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability2));
            
            //ability3
            /*
             * -4: Each Dragon creature you control deals damage equal to its
             * power to target player.
             */
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 4);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    final String target = getTargetPlayer();
                    final String player = card2.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    CardList dragons = new CardList(play.getCards());
                    dragons = dragons.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return c.isType("Dragon");
                    	}
                    });
                    for(int i = 0; i < dragons.size(); i++) {
                    	Card dragon = dragons.get(i);
                    	int damage = dragon.getNetAttack();
                    	AllZone.GameAction.addDamage(target, dragon, damage);
                    }
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(Constant.Player.Human);
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    CardList dragons = new CardList(play.getCards());
                    dragons = dragons.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return c.isType("Dragon");
                    	}
                    });
                    return card2.getCounters(Counters.LOYALTY) >= 4 && dragons.size() >= 1;
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && card2.getCounters(Counters.LOYALTY) >= 4
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            ability3.setDescription("-4: Each Dragon creature you control deals damage equal to its power to target player.");
            ability3.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability3));
            
            card2.addSpellAbility(ability1);
            card2.addSpellAbility(ability2);
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Elspeth Tirel")) {

        	//Planeswalker book-keeping
        	final int turn[] = new int[1];
        	turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 4));
        
            final SpellAbility ability3 = new Ability(card2, "0")
            {
            	public void resolve()
            	{
                    card2.subtractCounter(Counters.LOYALTY, 5);
                    turn[0] = AllZone.Phase.getTurn();
                    
            		CardList list = AllZoneUtil.getCardsInPlay();
            		list = list.filter(new CardListFilter(){
            			public boolean addCard(Card c)
            			{
            				return !c.isToken() && !c.isLand() && !c.equals(card2);
            			}
            		});
            		
            		CardListUtil.sortByIndestructible(list);
        			CardListUtil.sortByDestroyEffect(list);
        			
        			for (int i=0;i<list.size();i++)
        			{
        				AllZone.GameAction.destroy(list.get(i));
        			}
            	}
            	
            	public boolean canPlayAI()
            	{
            		CardList humanList = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Human);
            		CardList compList = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
            		
            		CardListFilter filter = new CardListFilter()
            		{
						public boolean addCard(Card c) {
							return !c.getName().equals("Mana Pool") && !c.isLand() && !c.isToken() && !c.equals(card2) &&
							       !c.getKeyword().contains("Indestructible");
						}
            		};
            		
            		humanList = humanList.filter(filter);
            		compList = compList.filter(filter);

            		return card2.getCounters(Counters.LOYALTY) > 5 && (humanList.size() > (compList.size() +1));
            	}
            	
            	public boolean canPlay() {
                    return  card2.getCounters(Counters.LOYALTY) >= 5
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };
            
            ability3.setBeforePayMana(new Input() {

				private static final long serialVersionUID = -3310512279978705284L;
				int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability3);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability 1: gain 1 life for each creature
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 2);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    int life = AllZoneUtil.getCreaturesInPlay(card2.getController()).size();
                    AllZone.GameAction.gainLife(card2.getController(), life);
                    Log.debug("Elspeth Tirel", "current phase: " + AllZone.Phase.getPhase());
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    return (list.size() > 2 || card2.getCounters(Counters.LOYALTY) < 4) &&
                    		!(ability3.canPlay() && ability3.canPlayAI()) && card2.getCounters(Counters.LOYALTY) < 12;
                }
                
                @Override
                public boolean canPlay() {
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };//SpellAbility ability1
            
            ability1.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -7969603493514210825L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability1);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability 1: create 3 white 1/1 tokens
            final SpellAbility ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 2);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    for (int i=0;i<3;i++)
                    	CardFactoryUtil.makeToken("Soldier", "W 1 1 Soldier", card2, "W", new String[] {
                            "Creature", "Soldier"}, 1, 1, new String[] {""});
                }
                
                @Override
                public boolean canPlayAI() {
                    return card2.getCounters(Counters.LOYALTY) >= 3 && 
                           !(ability3.canPlay() && ability3.canPlayAI());
                }
                
                @Override
                public boolean canPlay() {
                    return  card2.getCounters(Counters.LOYALTY) >= 2
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };//SpellAbility ability2
            
            ability2.setBeforePayMana(new Input() {

				private static final long serialVersionUID = 5089161242872591541L;
				int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability2);
                    }
                    stop();
                }//showMessage()
            });
            
            
            ability1.setDescription("+2: You gain 1 life for each creature you control.");
            ability1.setStackDescription("Elspeth Tirel - " + card2.getController() + " gains 1 life for each creature he/she controls.");
            card2.addSpellAbility(ability1);

            ability2.setDescription("-2: Put three white 1/1 Soldier creature tokens onto the battlefield.");
            ability2.setStackDescription("Elspeth Tirel - put three 1/1 Soldier creature tokens onto the battlefield.");
            card2.addSpellAbility(ability2);
            
            ability3.setDescription("-5: Destroy all other permanents except for lands and tokens.");
            ability3.setStackDescription(card2 + " - Destroy all other permanents except for lands and tokens.");
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
            return card2;
    	}//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Koth of the Hammer")) {
            //computer only plays ability 1 and 3, put 1/1 Soldier in play and make everything indestructible
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 3));
            
            //ability2: add R for each mountain
            final SpellAbility ability2 = new Ability(card2, "0") {
                
                @Override
                public void resolve() {
                    
                    card2.subtractCounter(Counters.LOYALTY, 2);
                    
                    turn[0] = AllZone.Phase.getTurn();
                    
                    Card mp = AllZone.ManaPool;//list.getCard(0);
                    CardList list = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Human);
                    list = list.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card crd)
                    	{
                    		return crd.getType().contains("Mountain");
                    	}
                    });
                    
                    for (int i=0;i<list.size();i++)
                    	mp.addExtrinsicKeyword("ManaPool:R");
                    
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                    
                }//canPlay()
            };//SpellAbility ability2
            
            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 5);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    Card emblem = new Card();

                    emblem.addIntrinsicKeyword("Indestructible");
                    emblem.addIntrinsicKeyword("Shroud");
                    emblem.addIntrinsicKeyword("Mountains you control have 'tap: This land deals 1 damage to target creature or player.'");
                    emblem.setImmutable(true);
                    emblem.addType("Emblem");
                    emblem.setController(card2.getController());
                    emblem.setOwner(card2.getOwner());
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    play.add(emblem);
                    
                    //AllZone.GameAction.checkStateEffects();
                    AllZone.StaticEffects.rePopulateStateBasedList();
                    for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
                        Command com = GameActionUtil.commands.get(effect);
                        com.execute();
                    }  
                }
                
                @Override
                public boolean canPlay() {
                    return 5 <= card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
                    list = list.filter(new CardListFilter(){
                    	public boolean addCard(Card c)
                    	{
                    		return c.isEmblem() && c.getKeyword().contains("Mountains you control have 'tap: This land deals 1 damage to target creature or player.'");
                    	}
                    });
                	return list.size() == 0 && card2.getCounters(Counters.LOYALTY) > 5;
                }
            };
            ability3.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -2054686425541429389L;
                
                int                       check            = -1;
                
                @Override
                public void showMessage() {
                    if(check != AllZone.Phase.getTurn()) {
                        check = AllZone.Phase.getTurn();
                        turn[0] = AllZone.Phase.getTurn();
                        AllZone.Stack.push(ability3);
                    }
                    stop();
                }//showMessage()
            });
            
            //ability 1: make 4/4 out of moutain
            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    final Card card[] = new Card[1];
                    card[0] = getTargetCard();
                    
                    final int[] oldAttack = new int[1];
                    final int[] oldDefense = new int[1];
                    
                    oldAttack[0] = card[0].getBaseAttack();
                    oldDefense[0] = card[0].getBaseDefense();
                   
                    if (card[0].getType().contains("Mountain"))
                    {
                    	card[0].untap();
                    	
	                    card[0].setBaseAttack(4);
	                    card[0].setBaseDefense(4);
	                    card[0].addType("Creature");
	                    card[0].addType("Elemental");
	                    
	                    //EOT
	                    final Command untilEOT = new Command() {
	
							private static final long serialVersionUID = 6426615528873039915L;
	
							public void execute() {
	                            card[0].setBaseAttack(oldAttack[0]);
	                            card[0].setBaseDefense(oldDefense[0]);
	                            
	                            card[0].removeType("Creature");
	                            card[0].removeType("Elemental");
	                        }
	                    };
	                    AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                	CardList list = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
                	list = list.filter(new CardListFilter()
                	{
                		public boolean addCard(Card crd)
                		{
                			return crd.isEmblem() && crd.getKeyword().contains("Mountains you control have 'tap: This land deals 1 damage to target creature or player.'");
                		}
                	});
                	
                	CardList mountains = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
                	mountains = mountains.filter(new CardListFilter()
                	{
                		public boolean addCard(Card crd)
                		{
                			return crd.getType().contains("Mountain") && CardFactoryUtil.canTarget(card2, crd);
                		}
                	});
                	CardListUtil.sortByTapped(mountains);
                	
                	if (mountains.size() == 0)
                		return false;
                	
                    if(ability3.canPlay() && ability3.canPlayAI() && list.size() == 0) {
                        return false;
                    } else {
                    	setTargetCard(mountains.get(0));
                        return true;
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
            };//SpellAbility ability1
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -7823269301012427007L;
                
                @Override
                public void showMessage() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    
                    CardList lands = new CardList();
                    lands.addAll(play.getCards());
                    lands = lands.filter(new CardListFilter() {
                    	public boolean addCard(Card crd)
                    	{
                    		return crd.getType().contains("Mountain");
                    	}
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability1, lands, "Select target Mountain",
                            true, false));
                }//showMessage()
            };//Input
            
            ability1.setBeforePayMana(runtime);
            
            ability1.setDescription("+1: Untap target Mountain. It becomes a 4/4 red Elemental creature until end of turn. It's still a land.");
            //ability1.setStackDescription("");
            card2.addSpellAbility(ability1);
            
            ability2.setDescription("-2: Add R to your mana pool for each Mountain you control.");
            ability2.setStackDescription("Koth of the Hammer - Add R to your mana pool for each Mountain you control.");
            card2.addSpellAbility(ability2);
            
            ability3.setDescription("-5: You get an emblem with \"Mountains you control have 'tap: This land deals 1 damage to target creature or player.'\"");
            ability3.setStackDescription("Koth of the Hammer - You get an emblem with \"Mountains you control have ‘tap: This land deals 1 damage to target creature or player.'\"");
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
            return card2;
        }
        //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Venser, the Sojourner")) {
            
            final int turn[] = new int[1];
            turn[0] = -1;
            
            final Card card2 = new Card() {
                @Override
                public void addDamage(int n, Card source) {
                    subtractCounter(Counters.LOYALTY, n);
                    AllZone.GameAction.checkStateEffects();
                }
            };
            card2.setOwner(owner);
            card2.setController(owner);
            
            card2.setName(card.getName());
            card2.setType(card.getType());
            card2.setManaCost(card.getManaCost());
            card2.addSpellAbility(new Spell_Permanent(card2));
            card2.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card2, Counters.LOYALTY, 3));
            

            final SpellAbility ability1 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                	final Card c = getTargetCard();
                	                	
                	if (c != null && AllZone.GameAction.isCardInPlay(c)) 
                	{		
                		 final Command eot = new Command() {

							private static final long serialVersionUID = -947355314271308770L;

							public void execute() {
                                 if(AllZone.GameAction.isCardRemovedFromGame(c)) {
                                     PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                                	 AllZone.GameAction.moveTo(play, c);
                                 }
                             }//execute()
                         };//Command
                		
	                    card2.addCounterFromNonEffect(Counters.LOYALTY, 2);
	                    turn[0] = AllZone.Phase.getTurn();
	                    
	                    AllZone.GameAction.exile(c);
	                    AllZone.EndOfTurn.addAt(eot);
                	}
                	
                }
                
                @Override
                public boolean canPlayAI() {
                	CardList list = AllZoneUtil.getCardsInPlay();
                	list = list.filter(new CardListFilter()
                	{
                		public boolean addCard(Card c)
                		{
                			return CardFactoryUtil.canTarget(card2, c) && c.getOwner().equals(Constant.Player.Computer) &&
                				   !c.equals(card2);
                		}
                	});
                	if (list.size() > 0) {
                		
                		CardList bestCards = list.filter(new CardListFilter()
                		{
                			public boolean addCard(Card c)
                			{
                				return c.getKeyword().contains("When CARDNAME enters the battlefield, draw a card.") ||
                					   c.getName().equals("Venerated Teacher") || c.getName().equals("Stoneforge Mystic") || c.getName().equals("Sun Titan") ||
                					   c.getType().contains("Ally");
                			}
                		});
                		
                		if (bestCards.size()>0) {
                			bestCards.shuffle();
                			setTargetCard(bestCards.get(0));
                		}
                		setTargetCard(list.get(0));
                	}
                	
                    return card2.getCounters(Counters.LOYALTY) < 8 && list.size() > 0 &&
                    	   AllZone.Phase.getPhase().equals("Main2");
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card2)) return false;
                    }
                    return 0 < card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
            };//SpellAbility ability1
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 8609211991425118222L;
                
                @Override
                public void showMessage() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isPermanent() && c.getOwner().equals(Constant.Player.Human) 
                            	   && CardFactoryUtil.canTarget(card, c) && !c.equals(card2);
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability1, list,
                            "Select target permanent you own", true, false));
                }//showMessage()
            };//Input
            
            
            final SpellAbility ability2 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 1);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    CardList list = AllZoneUtil.getCardsInPlay();
                    list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        final Card[] target = new Card[1];
                        target[0] = list.get(i);
                        
                        final Command untilEOT = new Command() {
							private static final long serialVersionUID = -7291011871465745495L;

							public void execute() {
                                if(AllZone.GameAction.isCardInPlay(target[0])) {
                                    target[0].removeExtrinsicKeyword("Unblockable");
                                }
                            }
                        };//Command
                        
                        if(AllZone.GameAction.isCardInPlay(target[0])) {
                            target[0].addExtrinsicKeyword("Unblockable");
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }//if
                    }//for
                    
                }//resolve()
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && card2.getCounters(Counters.LOYALTY) >= 1
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    
                	CardList list = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
                    list = list.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card crd)
                    	{
                    		return crd.isEmblem() && crd.getKeyword().contains("Whenever you cast a spell, exile target permanent.");
                    	}
                    });
                	
                    CardList creatList = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
                    creatList = creatList.filter(new CardListFilter()
                    {
                    	public boolean addCard(Card crd)
                    	{
                    		return crd.isCreature() && CombatUtil.canAttack(crd);
                    	}
                    });
                    
                    return list.size() >= 1 && card2.getCounters(Counters.LOYALTY) > 2 && creatList.size() >= 3 && AllZone.Phase.getPhase().equals("Main1");
                    
                }
            };//SpellAbility ability2
            
            
            //ability3
            final SpellAbility ability3 = new Ability(card2, "0") {
                @Override
                public void resolve() {
                    card2.subtractCounter(Counters.LOYALTY, 8);
                    turn[0] = AllZone.Phase.getTurn();
                    
                    Card emblem = new Card();
                    //should we even name this permanent?
                    //emblem.setName("Elspeth Emblem");
                    emblem.addIntrinsicKeyword("Indestructible");
                    emblem.addIntrinsicKeyword("Shroud");
                    emblem.addIntrinsicKeyword("Whenever you cast a spell, exile target permanent.");
                    emblem.setImmutable(true);
                    emblem.addType("Emblem");
                    emblem.setController(card2.getController());
                    emblem.setOwner(card2.getOwner());
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card2.getController());
                    play.add(emblem);
                    
                    /*
                    //AllZone.GameAction.checkStateEffects();
                    AllZone.StaticEffects.rePopulateStateBasedList();
                    for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
                        Command com = GameActionUtil.commands.get(effect);
                        com.execute();
                    }           
                    */           
                }
                
                @Override
                public boolean canPlay() {
                    return 8 <= card2.getCounters(Counters.LOYALTY)
                            && AllZone.getZone(card2).is(Constant.Zone.Play)
                            && turn[0] != AllZone.Phase.getTurn()
                            && AllZone.Phase.getActivePlayer().equals(card2.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2")) && AllZone.Stack.size() == 0;
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                	//multiple venser emblems are NOT redundant
                	/*
                    CardList list = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
                    list = list.filter(new CardListFilter(){
                    	public boolean addCard(Card c)
                    	{
                    		return c.isEmblem() && c.getKeyword().contains("Whenever you cast a spell, exile target permanent.");
                    	}
                    });
                    */
                	return card2.getCounters(Counters.LOYALTY) > 8;
                }
            };
            
            ability1.setBeforePayMana(runtime);
            ability1.setDescription("+2: Exile target permanent you own. Return it to the battlefield under your control at the beginning of the next end step.");
            card2.addSpellAbility(ability1);
            
            ability2.setDescription("-1: Creatures are unblockable this turn.");
            ability2.setStackDescription("Creatures are unblockable this turn.");
            card2.addSpellAbility(ability2);
            
            ability3.setDescription("-8: You get an emblem with \"Whenever you cast a spell, exile target permanent.\"");
            ability3.setStackDescription(card + "You get an emblem with \"Whenever you cast a spell, exile target permanent.\"");
            card2.addSpellAbility(ability3);
            
            card2.setSVars(card.getSVars());
            
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
