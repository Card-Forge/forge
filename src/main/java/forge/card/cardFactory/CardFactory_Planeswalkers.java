package forge.card.cardFactory;


import com.esotericsoftware.minlog.Log;
import forge.*;
import forge.Constant.Zone;
import forge.card.cost.Cost;
import forge.card.spellability.*;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

import java.util.HashMap;


/**
 * <p>CardFactory_Planeswalkers class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class CardFactory_Planeswalkers {
    /**
     * <p>getCard.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param cardName a {@link java.lang.String} object.
     * @param owner a {@link forge.Player} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, String cardName) {
        // All Planeswalkers set their loyality in the beginning
        if (card.getBaseLoyalty() > 0)
            card.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card, Counters.LOYALTY, card.getBaseLoyalty()));

        //*************** START *********** START **************************
        if (cardName.equals("Elspeth, Knight-Errant")) {
            //computer only plays ability 1 and 3, put 1/1 Soldier in play and make everything indestructible
            final int turn[] = new int[1];
            turn[0] = -1;

            //ability2: target creature gets +3/+3 and flying until EOT
            Target target2 = new Target(card, "TgtC");
            Cost cost2 = new Cost("AddCounter<1/LOYALTY>", cardName, true);
            final SpellAbility ability2 = new Ability_Activated(card, cost2, target2) {
                private static final long serialVersionUID = 6624768423224398603L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    final Card c = getTargetCard();

                    final Command eot = new Command() {
                        private static final long serialVersionUID = 94488363210770877L;

                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(c)) {
                                c.addTempAttackBoost(-3);
                                c.addTempDefenseBoost(-3);
                                c.removeExtrinsicKeyword("Flying");
                            }
                        }//execute()
                    };//Command
                    if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        c.addTempAttackBoost(3);
                        c.addTempDefenseBoost(3);
                        c.addExtrinsicKeyword("Flying");

                        AllZone.getEndOfTurn().addUntil(eot);
                    }
                }//resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {

                    return 0 < card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());


                }//canPlay()
            };//SpellAbility ability2

            //ability3
            Cost cost3 = new Cost("SubCounter<8/LOYALTY>", cardName, true);
            final SpellAbility ability3 = new Ability_Activated(card, cost3, null) {
                private static final long serialVersionUID = -830373718591602944L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();

                    Card emblem = new Card();
                    //should we even name this permanent?
                    //emblem.setName("Elspeth Emblem");
                    emblem.addIntrinsicKeyword("Indestructible");
                    emblem.addIntrinsicKeyword("Shroud");
                    emblem.addIntrinsicKeyword("Artifacts, creatures, enchantments, and lands you control are indestructible.");
                    emblem.setImmutable(true);
                    emblem.addType("Emblem");
                    emblem.addController(card.getController());
                    emblem.setOwner(card.getController());

                    AllZone.getGameAction().moveToPlay(emblem);

                    //AllZone.getGameAction().checkStateEffects();
                    AllZone.getStaticEffects().rePopulateStateBasedList();
                    for (String effect : AllZone.getStaticEffects().getStateBasedMap().keySet()) {
                        Command com = GameActionUtil.commands.get(effect);
                        com.execute();
                    }
                }

                @Override
                public boolean canPlay() {
                    return 8 <= card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isEmblem()
                                    && c.hasKeyword("Artifacts, creatures, enchantments, and lands you control are indestructible.");
                        }
                    });
                    return list.size() == 0 && card.getCounters(Counters.LOYALTY) > 8;
                }
            };

            //ability 1: create white 1/1 token
            Cost cost1 = new Cost("AddCounter<1/LOYALTY>", cardName, true);
            final SpellAbility ability1 = new Ability_Activated(card, cost1, null) {
                private static final long serialVersionUID = -6766888113766637596L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();

                    CardFactoryUtil.makeToken("Soldier", "W 1 1 Soldier", card.getController(), "W", new String[]{
                            "Creature", "Soldier"}, 1, 1, new String[]{""});
                }

                @Override
                public boolean canPlayAI() {
                    if (ability3.canPlay() && ability3.canPlayAI()) {
                        return false;
                    } else {
                        return true;
                    }
                }

                @Override
                public boolean canPlay() {
                    return 0 < card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };//SpellAbility ability1

            ability1.setDescription("+1: Put a 1/1 white Soldier creature token onto the battlefield.");
            ability1.setStackDescription(card + " - put a 1/1 white Soldier creature token onto the battlefield.");
            card.addSpellAbility(ability1);

            ability2.setDescription("+1: Target creature gets +3/+3 and gains flying until end of turn.");
            ability2.setStackDescription(card + " - creature gets +3/+3 and gains flying until EOT.");

            card.addSpellAbility(ability2);

            ability3.setDescription("-8: You get an emblem with \"Artifacts, creatures, enchantments, and lands you control are indestructible.\"");
            ability3.setStackDescription(card + " - You get an emblem with \"Artifacts, creatures, enchantments, and lands you control are indestructible.\"");
            card.addSpellAbility(ability3);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        }
        //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Ajani Goldmane")) {
            //computer only plays ability 1 and 3, gain life and put X/X token onto battlefield
            final int turn[] = new int[1];
            turn[0] = -1;

            //ability2: Put a +1/+1 counter on each creature you control. Those creatures gain vigilance until end of turn.
            final SpellAbility ability2 = new Ability(card, "0") {
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = -5436621445704076988L;

                    public void execute() {
                        Player player = card.getController();
                        CardList creatures = AllZoneUtil.getCreaturesInPlay(player);

                        for (int i = 0; i < creatures.size(); i++) {
                            Card card = creatures.get(i);
                            card.removeExtrinsicKeyword("Vigilance");
                        }
                    }
                };

                @Override
                public void resolve() {
                    card.subtractCounter(Counters.LOYALTY, 1);
                    turn[0] = AllZone.getPhase().getTurn();

                    Player player = card.getController();
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(player);

                    for (int i = 0; i < creatures.size(); i++) {
                        Card card = creatures.get(i);
                        card.addCounter(Counters.P1P1, 1);
                        card.addExtrinsicKeyword("Vigilance");
                    }

                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {

                    return 0 < card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());

                }//canPlay()
            };//SpellAbility ability2

            ability2.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 6373573398967821630L;
                int check = -1;

                @Override
                public void showMessage() {
                    if (check != AllZone.getPhase().getTurn()) {
                        check = AllZone.getPhase().getTurn();
                        turn[0] = AllZone.getPhase().getTurn();
                        AllZone.getStack().add(ability2);
                    }
                    stop();
                }//showMessage()
            });

            //ability3
            final SpellAbility ability3 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.LOYALTY, 6);
                    turn[0] = AllZone.getPhase().getTurn();

                    //Create token
                    int n = card.getController().getLife();
                    CardFactoryUtil.makeToken("Avatar", "W N N Avatar", card.getController(), "W", new String[]{
                            "Creature", "Avatar"}, n, n,
                            new String[]{"This creature's power and toughness are each equal to your life total"});
                }

                @Override
                public boolean canPlay() {
                    return 6 <= card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()

                @Override
                public boolean canPlayAI() {
                    // may be it's better to put only if you have less than 5 life
                    return true;
                }
            };
            ability3.setBeforePayMana(new Input() {
                private static final long serialVersionUID = 7530960428366291386L;

                int check = -1;

                @Override
                public void showMessage() {
                    if (check != AllZone.getPhase().getTurn()) {
                        check = AllZone.getPhase().getTurn();
                        turn[0] = AllZone.getPhase().getTurn();
                        AllZone.getStack().add(ability3);
                    }
                    stop();
                }//showMessage()
            });

            //ability 1: gain 2 life
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.getPhase().getTurn();


                    card.getController().gainLife(2, card);
                    Log.debug("Ajani Goldmane", "current phase: " + AllZone.getPhase().getPhase());
                }

                @Override
                public boolean canPlayAI() {
                    if (ability3.canPlay() && ability3.canPlayAI()) {
                        return false;
                    } else {
                        return true;
                    }
                }

                @Override
                public boolean canPlay() {
                    return 0 < card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };//SpellAbility ability1

            ability1.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -7969603493514210825L;

                int check = -1;

                @Override
                public void showMessage() {
                    if (check != AllZone.getPhase().getTurn()) {
                        check = AllZone.getPhase().getTurn();
                        turn[0] = AllZone.getPhase().getTurn();
                        AllZone.getStack().add(ability1);
                    }
                    stop();
                }//showMessage()
            });

            ability1.setDescription("+1: You gain 2 life.");
            StringBuilder stack1 = new StringBuilder();
            stack1.append("Ajani Goldmane - ").append(card.getController()).append(" gains 2 life");
            ability1.setStackDescription(stack1.toString());
            // ability1.setStackDescription("Ajani Goldmane - " + card.getController() + " gains 2 life");
            card.addSpellAbility(ability1);

            ability2.setDescription("-1: Put a +1/+1 counter on each creature you control. Those creatures gain vigilance until end of turn.");
            ability2.setStackDescription("Ajani Goldmane - Put a +1/+1 counter on each creature you control. They get vigilance.");
            card.addSpellAbility(ability2);

            ability3.setDescription("-6: Put a white Avatar creature token onto the battlefield. It has \"This creature's power and toughness are each equal to your life total.\"");
            ability3.setStackDescription("Ajani Goldmane - Put a X/X white Avatar creature token onto the battlefield.");
            card.addSpellAbility(ability3);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        }
        //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Chandra Nalaar")) {
            //computer only plays ability 1 and 3, discard and return creature from graveyard to play
            final int turn[] = new int[1];
            turn[0] = -1;

            //ability 1
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.getPhase().getTurn();

                    if (getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            Card c = getTargetCard();
                            c.addDamage(1, card);
                        }
                    } else {
                        getTargetPlayer().addDamage(1, card);
                    }
                }

                @Override
                public boolean canPlay() {
                    for (int i = 0; i < AllZone.getStack().size(); i++) {
                        if (AllZone.getStack().peekInstance(i).getSourceCard().equals(card)) return false;
                    }

                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }

                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                    setStackDescription("Chandra Nalaar - deals 1 damage to " + AllZone.getHumanPlayer());
                    return card.getCounters(Counters.LOYALTY) < 8;
                }
            };//SpellAbility ability1

            Input target1 = new Input() {
                private static final long serialVersionUID = 5263705146686766284L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Select target Player or Planeswalker");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if (card.isPlaneswalker() && zone.is(Constant.Zone.Battlefield) &&
                            CardFactoryUtil.canTarget(card, card)) {
                        ability1.setTargetCard(card);
                        //stopSetNext(new Input_PayManaCost(ability1));
                        AllZone.getStack().add(ability1);
                        stop();
                    }
                }//selectCard()

                @Override
                public void selectPlayer(Player player) {
                    ability1.setTargetPlayer(player);
                    //stopSetNext(new Input_PayManaCost(ability1));
                    AllZone.getStack().add(ability1);
                    stop();
                }
            };
            ability1.setBeforePayMana(target1);
            ability1.setDescription("+1: Chandra Nalaar deals 1 damage to target player.");
            card.addSpellAbility(ability1);
            //end ability1

            //ability 2
            final int damage2[] = new int[1];

            final SpellAbility ability2 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();

                    card.subtractCounter(Counters.LOYALTY, damage2[0]);
                    getTargetCard().addDamage(damage2[0], card);

                    damage2[0] = 0;
                }//resolve()

                @Override
                public boolean canPlay() {
                    for (int i = 0; i < AllZone.getStack().size(); i++) {
                        if (AllZone.getStack().peekInstance(i).getSourceCard().equals(card)) return false;
                    }

                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
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
                    AllZone.getDisplay().showMessage("Select target creature");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (!CardFactoryUtil.canTarget(card, c)) {
                        AllZone.getDisplay().showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if (c.isCreature()) {
                        turn[0] = AllZone.getPhase().getTurn();


                        damage2[0] = getDamage();

                        ability2.setTargetCard(c);
                        ability2.setStackDescription("Chandra Nalaar - deals damage to " + c);

                        AllZone.getStack().add(ability2);
                        stop();
                    }
                }//selectCard()

                int getDamage() {
                    int size = card.getCounters(Counters.LOYALTY);
                    Object choice[] = new Object[size];

                    for (int i = 0; i < choice.length; i++)
                        choice[i] = Integer.valueOf(i + 1);

                    Integer damage = (Integer) GuiUtils.getChoice("Select X", choice);
                    return damage.intValue();
                }
            };//Input target
            ability2.setBeforePayMana(target2);
            ability2.setDescription("-X: Chandra Nalaar deals X damage to target creature.");
            card.addSpellAbility(ability2);
            //end ability2


            //ability 3
            final SpellAbility ability3 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.LOYALTY, 8);
                    turn[0] = AllZone.getPhase().getTurn();

                    getTargetPlayer().addDamage(10, card);

                    CardList list = AllZoneUtil.getCreaturesInPlay(getTargetPlayer());

                    for (int i = 0; i < list.size(); i++)
                        list.get(i).addDamage(10, card);
                }//resolve()

                @Override
                public boolean canPlay() {
                    for (int i = 0; i < AllZone.getStack().size(); i++) {
                        if (AllZone.getStack().peekInstance(i).getSourceCard().equals(card)) return false;
                    }

                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && 7 < card.getCounters(Counters.LOYALTY)
                            && Phase.canCastSorcery(card.getController());
                }

                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                    StringBuilder sb = new StringBuilder();
                    sb.append("Chandra Nalaar - deals 10 damage to ").append(AllZone.getHumanPlayer());
                    sb.append(" and each creature he or she controls.");
                    setStackDescription(sb.toString());
                    //setStackDescription("Chandra Nalaar - deals 10 damage to " + AllZone.getHumanPlayer()
                    //        + " and each creature he or she controls.");
                    return true;
                }
            };//SpellAbility ability3

            Input target3 = new Input() {
                private static final long serialVersionUID = -3014450919506364666L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Select target player");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectPlayer(Player player) {
                    turn[0] = AllZone.getPhase().getTurn();

                    ability3.setTargetPlayer(player);

                    StringBuilder stack3 = new StringBuilder();
                    stack3.append("Chandra Nalaar - deals 10 damage to ").append(player);
                    stack3.append(" and each creature he or she controls.");
                    ability3.setStackDescription(stack3.toString());
                    //ability3.setStackDescription("Chandra Nalaar - deals 10 damage to " + player
                    //        + " and each creature he or she controls.");

                    AllZone.getStack().add(ability3);
                    stop();
                }
            };//Input target
            ability3.setBeforePayMana(target3);
            ability3.setDescription("-8: Chandra Nalaar deals 10 damage to target player and each creature he or she controls.");
            card.addSpellAbility(ability3);
            //end ability3

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        }
        //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Tezzeret the Seeker")) {
            final int turn[] = new int[1];
            turn[0] = -1;

            //ability1
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounterFromNonEffect(Counters.LOYALTY, 1);

                    turn[0] = AllZone.getPhase().getTurn();

                    //only computer uses the stack
                    CardList tapped = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    tapped = tapped.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() && c.isTapped() && CardFactoryUtil.canTarget(card, c);
                        }
                    });

                    for (int i = 0; i < 2 && i < tapped.size(); i++)
                        tapped.get(i).untap();
                }//resolve()

                @Override
                public boolean canPlayAI() {
                    return card.getCounters(Counters.LOYALTY) <= 6
                            && AllZone.getPhase().getPhase().equals(Constant.Phase.Main2);
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };
            final Input targetArtifact = new Input() {

                private static final long serialVersionUID = -7915255038817192835L;
                private int count;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Select an artifact to untap");
                    ButtonUtil.disableAll();
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (c.isArtifact() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(card, c)) {
                        count++;
                        c.untap();
                    }

                    //doesn't use the stack, its just easier this way
                    if (count == 2) {
                        count = 0;
                        turn[0] = AllZone.getPhase().getTurn();
                        card.addCounterFromNonEffect(Counters.LOYALTY, 1);
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
            card.addSpellAbility(ability1);
            //end ability 1


            //ability 2
            final SpellAbility ability2 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();

                    int size = card.getCounters(Counters.LOYALTY) + 1;
                    Object choice[] = new Object[size];

                    for (int i = 0; i < choice.length; i++)
                        choice[i] = Integer.valueOf(i);

                    Integer damage = (Integer) GuiUtils.getChoice("Select X", choice);
                    final int dam = damage.intValue();

                    card.subtractCounter(Counters.LOYALTY, dam);

                    CardList list = card.getController().getCardsIn(Zone.Library);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() && CardUtil.getConvertedManaCost(c.getManaCost()) <= dam;
                        }
                    });

                    if (list.size() > 0) {
                        Object o = GuiUtils.getChoiceOptional("Select artifact", list);
                        if (o != null) {
                            Card c = (Card) o;
                            if (list.contains(c)) {
                                AllZone.getGameAction().moveToPlay(c);
                            }
                        }
                    }
                }//resolve()

                @Override
                public boolean canPlay() {
                    for (int i = 0; i < AllZone.getStack().size(); i++) {
                        if (AllZone.getStack().peekInstance(i).getSourceCard().equals(card)) return false;
                    }

                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility ability2
            ability2.setDescription("-X: Search your library for an artifact card with converted mana cost X or less and put it onto the battlefield. Then shuffle your library.");
            StringBuilder stack2 = new StringBuilder();
            stack2.append(card.getName());
            stack2.append(" - Search your library for an artifact card with converted mana cost X or less and put it onto the battlefield. Then shuffle your library.");
            ability2.setStackDescription(stack2.toString());
            card.addSpellAbility(ability2);


            final SpellAbility ability3 = new Ability(card, "0") {
                @Override
                public void resolve() {

                    card.subtractCounter(Counters.LOYALTY, 5);

                    turn[0] = AllZone.getPhase().getTurn();

                    CardList list = card.getController().getCardsIn(Zone.Battlefield);
                    list = list.getType("Artifact");
                    CardList creatures = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature();
                        }
                    });

                    final HashMap<Integer, Card> tempCardMap = new HashMap<Integer, Card>();

                    for (Card creatureCard : creatures) {
                        Card crd = copyStats(creatureCard);
                        tempCardMap.put(creatureCard.getUniqueNumber(), crd);
                        //System.out.println("Just added:" + crd);
                    }

                    for (Card c : list) {
                        final Card[] art = new Card[1];
                        art[0] = c;
                        if (AllZoneUtil.isCardInPlay(art[0])) {
                            if (c.isCreature()) {
                                //Card crd = copyStats(art[0]);
                                //tempCards[c.getUniqueNumber()] = crd;

                                final Command creatureUntilEOT = new Command() {
                                    private static final long serialVersionUID = 5063161656920609389L;

                                    public void execute() {
                                        final int id = art[0].getUniqueNumber();

                                        Card tempCard = tempCardMap.get(id);
                                        art[0].setBaseAttack(tempCard.getBaseAttack());
                                        art[0].setBaseDefense(tempCard.getBaseDefense());

                                    }
                                };//Command

                                art[0].setBaseAttack(5);
                                art[0].setBaseDefense(5);

                                AllZone.getEndOfTurn().addUntil(creatureUntilEOT);
                            } else {
                                final Command nonCreatureUntilEOT = new Command() {
                                    private static final long serialVersionUID = 248122386218960073L;

                                    public void execute() {
                                        art[0].removeType("Creature");
                                        art[0].setBaseAttack(0);
                                        art[0].setBaseDefense(0);
                                    }
                                };//Command

                                art[0].addType("Creature");
                                art[0].setBaseAttack(5);
                                art[0].setBaseDefense(5);

                                AllZone.getEndOfTurn().addUntil(nonCreatureUntilEOT);
                            }//noncreature artifact

                        }
                    }//for
                }//resolve

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && card.getCounters(Counters.LOYALTY) >= 5
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact()
                                    && (!c.isCreature() || (c.isCreature() && c.getBaseAttack() < 4))
                                    && !c.hasSickness();
                        }
                    });
                    return list.size() > 4 && AllZone.getPhase().getPhase().equals("Main1")
                            && card.getCounters(Counters.LOYALTY) > 5;
                }
            };
            ability3.setDescription("-5: Artifacts you control become 5/5 artifact creatures until end of turn.");
            StringBuilder stack3 = new StringBuilder();
            stack3.append(card.getName()).append(" - Artifacts you control become 5/5 artifact creatures until end of turn.");
            ability3.setStackDescription(stack3.toString());
            card.addSpellAbility(ability3);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Jace, the Mind Sculptor")) {
            final int turn[] = new int[1];
            turn[0] = -1;

            Target t1 = new Target(card, "Select target player", "Player");
            Cost cost1 = new Cost("AddCounter<2/LOYALTY>", cardName, true);

            final SpellAbility ability1 = new Ability_Activated(card, cost1, t1) {
                private static final long serialVersionUID = -986543400626807336L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    //card.addCounterFromNonEffect(Counters.LOYALTY, 2);
                    Player targetPlayer = getTargetPlayer();

                    PlayerZone lib = targetPlayer.getZone(Constant.Zone.Library);

                    if (lib.size() == 0) return;

                    Card c = lib.get(0);

                    if (card.getController().isHuman()) {
                        StringBuilder question = new StringBuilder();
                        question.append("Put the card ").append(c).append(" on the bottom of the ");
                        question.append(c.getController()).append("'s library?");

                        if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                            AllZone.getGameAction().moveToBottomOfLibrary(c);
                        }

                    } else //compy
                    {
                        CardList land = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());

                        //TODO: improve this:
                        if (land.size() > 4 && c.isLand()) ;
                        else {
                            AllZone.getGameAction().moveToBottomOfLibrary(c);
                        }
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return card.getCounters(Counters.LOYALTY) < 12 && AllZone.getHumanPlayer().getZone(Zone.Library).size() > 2;
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };
            ability1.setDescription("+2: Look at the top card of target player's library. You may put that card on the bottom of that player's library.");
            StringBuilder stack1 = new StringBuilder();
            stack1.append(card.getName()).append(" - Look at the top card of target player's library. You may put that card on the bottom of that player's library.");
            ability1.setStackDescription(stack1.toString());

            ability1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card.addSpellAbility(ability1);

            final Ability ability2 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    card.getController().drawCards(3);

                    Player player = card.getController();
                    if (player.isHuman()) humanResolve();
                    //else
                    //  computerResolve();
                }

                public void humanResolve() {
                    CardList putOnTop = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);

                    if (putOnTop.size() > 0) {
                        Object o = GuiUtils.getChoice("First card to put on top: ", putOnTop.toArray());
                        if (o != null) {
                            Card c1 = (Card) o;
                            putOnTop.remove(c1);
                            AllZone.getGameAction().moveToLibrary(c1);
                        }
                    }

                    putOnTop = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);

                    if (putOnTop.size() > 0) {
                        Object o = GuiUtils.getChoice("Second card to put on top: ", putOnTop.toArray());
                        if (o != null) {
                            Card c2 = (Card) o;
                            putOnTop.remove(c2);
                            AllZone.getGameAction().moveToLibrary(c2);
                        }
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };
            ability2.setDescription("0: Draw three cards, then put two cards from your hand on top of your library in any order.");
            StringBuilder stack2 = new StringBuilder();
            stack2.append(card.getName()).append(" - Draw three cards, then put two cards from your hand on top of your library in any order.");
            ability2.setStackDescription(stack2.toString());
            card.addSpellAbility(ability2);

            Cost cost = new Cost("SubCounter<1/LOYALTY>", cardName, true);
            Target target = new Target(card, "TgtC");

            final SpellAbility ability3 = new Ability_Activated(card, cost, target) {
                private static final long serialVersionUID = -1113077473448818423L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    //card.subtractCounter(Counters.LOYALTY, 1);

                    if (AllZoneUtil.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        AllZone.getGameAction().moveToHand(getTargetCard());
                    }//if
                }//resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {
                    return card.getCounters(Counters.LOYALTY) >= 1
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }
            };
            ability3.setDescription("-1: Return target creature to its owner's hand.");
            StringBuilder stack3 = new StringBuilder();
            stack3.append(card.getName()).append(" - Return target creature to its owner's hand.");
            ability3.setStackDescription(stack3.toString());
            card.addSpellAbility(ability3);

            Target target4 = new Target(card, "Select target player", "Player");
            Cost cost4 = new Cost("SubCounter<12/LOYALTY>", cardName, true);
            final SpellAbility ability4 = new Ability_Activated(card, cost4, target4) {
                private static final long serialVersionUID = 5512803971603404142L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    //card.subtractCounter(Counters.LOYALTY, 12);

                    Player player = getTargetPlayer();

                    CardList libList = player.getCardsIn(Zone.Library);
                    CardList handList = player.getCardsIn(Zone.Hand);

                    for (Card c : libList)
                        AllZone.getGameAction().exile(c);

                    for (Card c : handList) {
                        AllZone.getGameAction().moveToLibrary(c);
                    }
                    player.shuffle();
                }

                @Override
                public boolean canPlayAI() {
                    int libSize = AllZone.getHumanPlayer().getZone(Zone.Library).size();
                    int handSize = AllZone.getHumanPlayer().getZone(Zone.Hand).size();
                    return libSize > 0 && (libSize >= handSize);
                }

                @Override
                public boolean canPlay() {
                    return card.getCounters(Counters.LOYALTY) >= 12
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }
            };
            ability4.setDescription("-12: Exile all cards from target player's library, then that player shuffles his or her hand into his or her library.");
            StringBuilder stack4 = new StringBuilder();
            stack4.append(card.getName()).append(" - Exile all cards from target player's library, then that player shuffles his or her hand into his or her library.");
            ability4.setStackDescription(stack4.toString());
            ability4.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card.addSpellAbility(ability4);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Sarkhan the Mad")) {

            //Planeswalker book-keeping
            final int turn[] = new int[1];
            turn[0] = -1;

            //ability1
            /*
             * 0: Reveal the top card of your library and put it into your hand. Sarkhan
             * the Mad deals damage to himself equal to that card's converted mana cost.
             */
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounterFromNonEffect(Counters.LOYALTY, 0);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Player player = card.getController();
                    PlayerZone lib = player.getZone(Constant.Zone.Library);

                    Card topCard = lib.get(0);
                    int convertedManaTopCard = CardUtil.getConvertedManaCost(topCard.getManaCost());
                    CardList showTop = new CardList();
                    showTop.add(topCard);
                    GuiUtils.getChoiceOptional("Revealed top card: ", showTop.toArray());

                    //now, move it to player's hand                
                    AllZone.getGameAction().moveToHand(topCard);

                    //now, do X damage to Sarkhan
                    card.addDamage(convertedManaTopCard, card);

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
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };
            ability1.setDescription("0: Reveal the top card of your library and put it into your hand. Sarkhan the Mad deals damage to himself equal to that card's converted mana cost.");
            StringBuilder stack1 = new StringBuilder();
            stack1.append(card.getName()).append(" - Reveal top card and do damage.");
            ability1.setStackDescription(stack1.toString());

            //ability2
            /*
             * -2: Target creature's controller sacrifices it, then that player puts a 5/5 red Dragon
             * creature token with flying onto the battlefield.
             */
            Target target2 = new Target(card, "TgtC");
            Cost cost2 = new Cost("SubCounter<2/LOYALTY>", cardName, true);
            final SpellAbility ability2 = new Ability_Activated(card, cost2, target2) {
                private static final long serialVersionUID = 4322453486268967722L;

                @Override
                public void resolve() {
                    //card.subtractCounter(Counters.LOYALTY, 2);
                    turn[0] = AllZone.getPhase().getTurn();

                    Card target = getTargetCard();
                    AllZone.getGameAction().sacrifice(target);
                    //in makeToken, use target for source, so it goes into the correct Zone
                    CardFactoryUtil.makeToken("Dragon", "R 5 5 Dragon", target.getController(), "R", new String[]{"Creature", "Dragon"}, 5, 5, new String[]{"Flying"});

                }//resolve()

                @Override
                public boolean canPlayAI() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !(c.isToken() && c.isType("Dragon"));
                        }
                    });
                    return creatures.size() >= 1;
                }

                @Override
                public void chooseTargetAI() {
                    CardList cards = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    //avoid targeting the dragon tokens we just put in play...
                    cards = cards.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !(c.isToken() && c.isType("Dragon"));
                        }
                    });
                    setTargetCard(CardFactoryUtil.AI_getCheapestCreature(cards, card, true));
                    Log.debug("Sarkhan the Mad", "Sarkhan the Mad caused sacrifice of: " +
                            CardFactoryUtil.AI_getCheapestCreature(cards, card, true));
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && card.getCounters(Counters.LOYALTY) >= 2
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };
            ability2.setDescription("-2: Target creature's controller sacrifices it, then that player puts a 5/5 red Dragon creature token with flying onto the battlefield.");

            //ability3
            /*
             * -4: Each Dragon creature you control deals damage equal to its
             * power to target player.
             */
            Target target3 = new Target(card, "Select target player", "Player");
            Cost cost3 = new Cost("SubCounter<4/LOYALTY>", cardName, true);
            final SpellAbility ability3 = new Ability_Activated(card, cost3, target3) {
                private static final long serialVersionUID = -5488579738767048060L;

                @Override
                public void resolve() {
                    //card.subtractCounter(Counters.LOYALTY, 4);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Player target = getTargetPlayer();
                    final Player player = card.getController();
                    CardList dragons = player.getCardsIn(Zone.Battlefield).getType("Dragon");
                    for (int i = 0; i < dragons.size(); i++) {
                        Card dragon = dragons.get(i);
                        int damage = dragon.getNetAttack();
                        target.addDamage(damage, dragon);
                    }

                }//resolve()

                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                    CardList dragons = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield).getType("Dragon");
                    return card.getCounters(Counters.LOYALTY) >= 4 && dragons.size() >= 1;
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && card.getCounters(Counters.LOYALTY) >= 4
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };
            ability3.setDescription("-4: Each Dragon creature you control deals damage equal to its power to target player.");

            card.addSpellAbility(ability1);
            card.addSpellAbility(ability2);
            card.addSpellAbility(ability3);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Koth of the Hammer")) {
            //computer only plays ability 1 and 3, put 1/1 Soldier in play and make everything indestructible
            final int turn[] = new int[1];
            turn[0] = -1;

            //ability2: add R for each mountain
            final SpellAbility ability2 = new Ability(card, "0") {

                @Override
                public void resolve() {

                    card.subtractCounter(Counters.LOYALTY, 2);

                    turn[0] = AllZone.getPhase().getTurn();

                    CardList list = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card crd) {
                            return crd.isType("Mountain");
                        }
                    });

                    Ability_Mana abMana = new Ability_Mana(card, "0", "R", list.size()) {
                        private static final long serialVersionUID = -2182129023960978132L;
                    };
                    abMana.setUndoable(false);
                    abMana.produceMana();

                }//resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {

                    return 0 < card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());

                }//canPlay()
            };//SpellAbility ability2

            //ability3
            final SpellAbility ability3 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.LOYALTY, 5);
                    turn[0] = AllZone.getPhase().getTurn();

                    Card emblem = new Card();

                    emblem.addIntrinsicKeyword("Indestructible");
                    emblem.addIntrinsicKeyword("Shroud");
                    emblem.addIntrinsicKeyword("Mountains you control have 'tap: This land deals 1 damage to target creature or player.'");
                    emblem.setImmutable(true);
                    emblem.addType("Emblem");
                    emblem.addController(card.getController());
                    emblem.setOwner(card.getOwner());

                    // TODO: Emblems live in the command zone
                    AllZone.getGameAction().moveToPlay(emblem);

                    //AllZone.getGameAction().checkStateEffects();
                    AllZone.getStaticEffects().rePopulateStateBasedList();
                    for (String effect : AllZone.getStaticEffects().getStateBasedMap().keySet()) {
                        Command com = GameActionUtil.commands.get(effect);
                        com.execute();
                    }
                }

                @Override
                public boolean canPlay() {
                    return 5 <= card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isEmblem()
                                    && c.hasKeyword("Mountains you control have 'tap: This land deals 1 damage to target creature or player.'");
                        }
                    });
                    return list.size() == 0 && card.getCounters(Counters.LOYALTY) > 5;
                }
            };
            ability3.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -2054686425541429389L;

                int check = -1;

                @Override
                public void showMessage() {
                    if (check != AllZone.getPhase().getTurn()) {
                        check = AllZone.getPhase().getTurn();
                        turn[0] = AllZone.getPhase().getTurn();
                        AllZone.getStack().add(ability3);
                    }
                    stop();
                }//showMessage()
            });

            //ability 1: make 4/4 out of moutain
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounterFromNonEffect(Counters.LOYALTY, 1);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Card card[] = new Card[1];
                    card[0] = getTargetCard();

                    final int[] oldAttack = new int[1];
                    final int[] oldDefense = new int[1];

                    oldAttack[0] = card[0].getBaseAttack();
                    oldDefense[0] = card[0].getBaseDefense();

                    if (card[0].isType("Mountain")) {
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
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card crd) {
                            return crd.isEmblem()
                                    && crd.hasKeyword("Mountains you control have 'tap: This land deals 1 damage to target creature or player.'");
                        }
                    });

                    CardList mountains = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    mountains = mountains.filter(new CardListFilter() {
                        public boolean addCard(Card crd) {
                            return crd.isType("Mountain")
                                    && CardFactoryUtil.canTarget(card, crd);
                        }
                    });
                    CardListUtil.sortByTapped(mountains);

                    if (mountains.size() == 0)
                        return false;

                    if (ability3.canPlay() && ability3.canPlayAI() && list.size() == 0) {
                        return false;
                    } else {
                        setTargetCard(mountains.get(0));
                        return true;
                    }
                }

                @Override
                public boolean canPlay() {
                    return 0 < card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()
            };//SpellAbility ability1

            Input runtime = new Input() {
                private static final long serialVersionUID = -7823269301012427007L;

                @Override
                public void showMessage() {
                    CardList lands = card.getController().getCardsIn(Zone.Battlefield).getType("Mountain");

                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability1, lands, "Select target Mountain",
                            true, false));
                }//showMessage()
            };//Input

            ability1.setBeforePayMana(runtime);

            ability1.setDescription("+1: Untap target Mountain. It becomes a 4/4 red Elemental creature until end of turn. It's still a land.");
            //ability1.setStackDescription("");
            card.addSpellAbility(ability1);

            ability2.setDescription("-2: Add R to your mana pool for each Mountain you control.");
            ability2.setStackDescription("Koth of the Hammer - Add R to your mana pool for each Mountain you control.");
            card.addSpellAbility(ability2);

            ability3.setDescription("-5: You get an emblem with \"Mountains you control have 'tap: This land deals 1 damage to target creature or player.'\"");
            ability3.setStackDescription("Koth of the Hammer - You get an emblem with \"Mountains you control have 'tap: This land deals 1 damage to target creature or player.'\"");
            card.addSpellAbility(ability3);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        }
        //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Venser, the Sojourner")) {

            final int turn[] = new int[1];
            turn[0] = -1;

            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final Card c = getTargetCard();

                    if (c != null && AllZoneUtil.isCardInPlay(c)) {
                        final Command eot = new Command() {

                            private static final long serialVersionUID = -947355314271308770L;

                            public void execute() {
                                if (AllZoneUtil.isCardExiled(c)) {
                                    PlayerZone play = c.getOwner().getZone(Constant.Zone.Battlefield);
                                    AllZone.getGameAction().moveTo(play, AllZoneUtil.getCardState(c));
                                }
                            }//execute()
                        };//Command

                        card.addCounterFromNonEffect(Counters.LOYALTY, 2);
                        turn[0] = AllZone.getPhase().getTurn();

                        AllZone.getGameAction().exile(c);
                        AllZone.getEndOfTurn().addAt(eot);
                    }

                }

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c) && c.getOwner().isComputer() &&
                                    !c.equals(card);
                        }
                    });
                    if (list.size() > 0) {

                        CardList bestCards = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.getName().equals("Venerated Teacher")
                                        || c.getName().equals("Stoneforge Mystic")
                                        || c.getName().equals("Sun Titan")
                                        || c.isType("Ally");
                            }
                        });

                        if (bestCards.size() > 0) {
                            bestCards.shuffle();
                            setTargetCard(bestCards.get(0));
                        }
                        setTargetCard(list.get(0));
                    }

                    return card.getCounters(Counters.LOYALTY) < 8 && list.size() > 0 &&
                            AllZone.getPhase().getPhase().equals("Main2");
                }

                @Override
                public boolean canPlay() {
                    for (int i = 0; i < AllZone.getStack().size(); i++) {
                        if (AllZone.getStack().peekInstance(i).getSourceCard().equals(card)) return false;
                    }
                    return 0 < card.getCounters(Counters.LOYALTY)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && turn[0] != AllZone.getPhase().getTurn()
                            && Phase.canCastSorcery(card.getController());
                }//canPlay()

            };//SpellAbility ability1

            Input runtime = new Input() {
                private static final long serialVersionUID = 8609211991425118222L;

                @Override
                public void showMessage() {
                    CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isPermanent() && c.getOwner().isHuman()
                                    && CardFactoryUtil.canTarget(card, c);
                        }
                    });

                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability1, list,
                            "Select target permanent you own", true, false));
                }//showMessage()
            };//Input

            ability1.setBeforePayMana(runtime);
            ability1.setDescription("+2: Exile target permanent you own. Return it to the battlefield under your control at the beginning of the next end step.");
            card.addSpellAbility(ability1);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        }//*************** END ************ END **************************

        return card;
    }


    // copies stats like attack, defense, etc..
    /**
     * <p>copyStats.</p>
     *
     * @param o a {@link java.lang.Object} object.
     * @return a {@link forge.Card} object.
     */
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
