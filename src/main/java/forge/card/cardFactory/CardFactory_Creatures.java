package forge.card.cardFactory;


import com.esotericsoftware.minlog.Log;
import forge.*;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cost.Cost;
import forge.card.spellability.*;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCost;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.Constant.Zone;

import javax.swing.*;

import net.slightlymagic.maxmtg.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * <p>CardFactory_Creatures class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class CardFactory_Creatures {

    /**
     * <p>hasKeyword.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param k a {@link java.lang.String} object.
     * @return a int.
     */
    private static final int hasKeyword(Card c, String k) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith(k)) return i;

        return -1;
    }

    /**
     * <p>shouldCycle.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public static int shouldCycle(Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith("Cycling")) return i;

        return -1;
    }

    /**
     * <p>shouldTypeCycle.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public static int shouldTypeCycle(Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith("TypeCycling")) return i;

        return -1;
    }

    /**
     * <p>shouldTransmute.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public static int shouldTransmute(Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith("Transmute")) return i;

        return -1;
    }

    /**
     * <p>shouldSoulshift.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public static int shouldSoulshift(Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith("Soulshift")) return i;

        return -1;
    }


    /**
     * <p>getCard.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param cardName a {@link java.lang.String} object.
     * @param owner a {@link forge.Player} object.
     * @param cf a {@link forge.card.cardFactory.CardFactoryInterface} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName, CardFactoryInterface cf) {

        //*************** START *********** START **************************
        if (cardName.equals("Force of Savagery")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 1603238129819160467L;

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);

                    return list.containsName("Glorious Anthem") || list.containsName("Gaea's Anthem");
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Gilder Bairn")) {
            Cost abCost = new Cost("2 GU Untap", cardName, true);
            Target tgt = new Target(card, "Select target permanent.", new String[]{"Permanent"});
            final Ability_Activated a1 = new Ability_Activated(card, abCost, tgt) {
                private static final long serialVersionUID = -1847685865277129366L;

                @Override
                public void resolve() {
                    Card c = getTargetCard();

                    if (c.sumAllCounters() == 0) return;
                    else if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        //zerker clean up:
                        for (Counters c_1 : Counters.values())
                            if (c.getCounters(c_1) > 0) c.addCounter(c_1, c.getCounters(c_1));
                    }
                }

                @Override
                public void chooseTargetAI() {
                    CardList perms = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
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
                    CardList perms = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    perms = perms.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.sumAllCounters() > 0 && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    return perms.size() > 0;
                }
            };//SpellAbility

            card.addSpellAbility(a1);
            a1.setDescription(abCost + "For each counter on target permanent, put another of those counters on that permanent.");
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Primal Plasma") || cardName.equals("Primal Clay")) {
            card.setBaseAttack(3);
            card.setBaseDefense(3);
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    String choice = "";
                    String choices[] = {"3/3", "2/2 with flying", "1/6 with defender"};

                    if (card.getController().isHuman()) {
                        choice = GuiUtils.getChoice("Choose one", choices);
                    } else choice = choices[MyRandom.random.nextInt(3)];

                    if (choice.equals("2/2 with flying")) {
                        card.setBaseAttack(2);
                        card.setBaseDefense(2);
                        card.addIntrinsicKeyword("Flying");
                    }
                    if (choice.equals("1/6 with defender")) {
                        card.setBaseAttack(1);
                        card.setBaseDefense(6);
                        card.addIntrinsicKeyword("Defender");
                        card.addType("Wall");
                    }

                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 8957338395786245312L;

                public void execute() {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - choose: 3/3, 2/2 flying, 1/6 defender");
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Oracle of Mul Daya")) {
            final SpellAbility ability = new Ability(card, "0") {
                private static final long serialVersionUID = 2902408812353813L;

                @Override
                public void resolve() {
                    // TODO: change to static ability?
                    CardList library = card.getController().getCardsIn(Zone.Library);
                    if (library.size() == 0)
                        return;

                    Card top = library.get(0);
                    if (top.isLand())
                        card.getController().playLand(top);
                }//resolve()

                @Override
                public boolean canPlay() {
                    CardList library = card.getController().getCardsIn(Zone.Library);
                    if (library.size() == 0) return false;
                    PlayerZone play = card.getController().getZone(Constant.Zone.Battlefield);
                    boolean canPlayLand = card.getController().canPlayLand();

                    return (AllZoneUtil.isCardInZone(play, card) && library.get(0).isLand() && canPlayLand);
                }
            };//SpellAbility

            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" - plays land from top of library.");
            ability.setStackDescription(sb.toString());
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Drekavac")) {
            final Input discard = new Input() {
                private static final long serialVersionUID = -6392468000100283596L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Select a noncreature card to discard");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (zone.is(Constant.Zone.Hand) && !c.isCreature()) {
                        c.getController().discard(c, null);
                        stop();
                    }
                }

                @Override
                public void selectButtonCancel() {
                    AllZone.getGameAction().sacrifice(card);
                    stop();
                }
            };//Input

            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if (card.getController().isHuman()) {
                        if (AllZone.getHumanPlayer().getCardsIn(Zone.Hand).size() == 0)
                            AllZone.getGameAction().sacrifice(card);
                        else AllZone.getInputControl().setInput(discard);
                    } else {
                        CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
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

                    AllZone.getStack().addSimultaneousStackEntry(ability);

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
                    CardList list = card.getController().getCardsIn(Zone.Hand);
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
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Minotaur Explorer") || cardName.equals("Balduvian Horde") ||
                cardName.equals("Pillaging Horde")) {

            final SpellAbility creature = new Spell_Permanent(card) {
                private static final long serialVersionUID = -7326018877172328480L;

                @Override
                public boolean canPlayAI() {
                    int reqHand = 1;
                    if (AllZone.getZoneOf(card).is(Constant.Zone.Hand))
                        reqHand++;

                    // Don't play if it would sacrifice as soon as it comes into play
                    return AllZone.getComputerPlayer().getCardsIn(Constant.Zone.Hand).size() > reqHand;
                }
            };
            card.clearFirstSpell();
            card.addFirstSpellAbility(creature);

            final SpellAbility ability = new Ability(card, "0") {

                @Override
                public void resolve() {
                    CardList hand = card.getController().getCardsIn(Zone.Hand);
                    if (hand.size() == 0)
                        AllZone.getGameAction().sacrifice(card);
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

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Sleeper Agent")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    // TODO: this need to be targeted
                    card.addController(card.getController().getOpponent());
                    //AllZone.getGameAction().changeController(new CardList(card), card.getController(), card.getController().getOpponent());
                }
            };

            ability.setStackDescription("When Sleeper Agent enters the battlefield, target opponent gains control of it.");
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -3934471871041458847L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }//execute()
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Phylactery Lich")) {
            final CommandReturn getArt = new CommandReturn() {
                //get target card, may be null
                public Object execute() {
                    CardList art = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
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
                    if (!list.isEmpty())
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

                    if (AllZoneUtil.isCardInPlay(c) && c.isArtifact()) {
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
                            AllZone.getDisplay().showMessage("Select target artifact you control");
                            ButtonUtil.disableAll();
                        }

                        @Override
                        public void selectCard(Card card, PlayerZone zone) {
                            if (card.isArtifact() && zone.is(Constant.Zone.Battlefield) && card.getController().isHuman()) {
                                ability.setTargetCard(card);
                                AllZone.getStack().add(ability);
                                stop();
                            }
                        }
                    };//Input target


                    if (card.getController().isHuman()) {
                        CardList artifacts = AllZoneUtil.getPlayerTypeIn(AllZone.getHumanPlayer(), Zone.Battlefield, "Artifact");

                        if (artifacts.size() != 0) AllZone.getInputControl().setInput(target);

                    } else { //computer
                        Object o = getArt.execute();
                        if (o != null)//should never happen, but just in case
                        {
                            ability.setTargetCard((Card) o);
                            AllZone.getStack().addSimultaneousStackEntry(ability);

                        }
                    }//else
                }//execute()
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(new Spell_Permanent(card) {

                private static final long serialVersionUID = -1506199222879057809L;

                @Override
                public boolean canPlayAI() {
                    Object o = getArt.execute();
                    return (o != null) && AllZone.getZoneOf(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if (cardName.equals("Sky Swallower")) {
            final SpellAbility ability = new Ability(card, "0") {

                @Override
                public void resolve() {
                	//TODO - this needs to be targeted
                    Player opp = card.getController().getOpponent();

                    CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
                    list = list.getValidCards("Card.Other+YouCtrl".split(","), card.getController(), card);
                    card.addController(opp);
                    //AllZone.getGameAction().changeController(list, card.getController(), opp);
                }//resolve()
            };//SpellAbility

            Command intoPlay = new Command() {
                private static final long serialVersionUID = -453410206437839334L;

                public void execute() {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getController().getOpponent());
                    sb.append(" gains control of all other permanents you control");
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Jhoira of the Ghitu")) {
            final Stack<Card> chosen = new Stack<Card>();
            final SpellAbility ability = new Ability(card, "2") {
                private static final long serialVersionUID = 4414609319033894302L;

                @Override
                public boolean canPlay() {
                    CardList possible = card.getController().getCardsIn(Zone.Hand);
                    possible = possible.filter(AllZoneUtil.nonlands);
                    return !possible.isEmpty() && super.canPlay();
                }

                public boolean canPlayAI() {
                    return false;
                }

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
                public void showMessage() {
                    ButtonUtil.disableAll();
                    AllZone.getDisplay().showMessage("Exile a nonland card from your hand.");
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (zone.is(Constant.Zone.Hand) && !c.isLand()) {
                        AllZone.getGameAction().exile(c);
                        chosen.push(c);
                        ability.setStackDescription(card.toString() + " - Suspending " + c.toString());
                        AllZone.getStack().add(ability);
                        stop();
                    }
                }
            });

            card.addSpellAbility(ability);
        }//*************** END ************ END **************************

/*
        //*************** START *********** START **************************
        else if (cardName.equals("Hermit Druid")) {
            Cost abCost = new Cost("G T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = 5884624727757154056L;

                @Override
                public boolean canPlayAI() {
                    //Use the ability if there is still a forest in the library
                    CardList library = card.getController().getCardsIn(Zone.Library);
                    return !library.getName("Forest").isEmpty();
                }

                @Override
                public void resolve() {
                    CardList library = card.getController().getCardsIn(Zone.Library);
                    if (library.size() == 0) return;    // maybe provide some notification that library is empty?

                    CardList revealed = new CardList();

                    Card basicGrab = null;

                    int count = 0;
                    // reveal top card until library runs out or hit a basic land
                    while (basicGrab == null) {
                        Card top = library.get(count);
                        count++;
                        revealed.add(top);

                        if (top.isBasicLand())
                            basicGrab = top;

                        if (count == library.size())
                            break;
                    }//while
                    GuiUtils.getChoiceOptional("Revealed cards:", revealed.toArray());

                    if (basicGrab != null) {
                        // put basic in hand
                        AllZone.getGameAction().moveToHand(basicGrab);
                        revealed.remove(basicGrab);
                    }
                    // place revealed cards in graveyard (TODO: player should choose order)
                    for (Card c : revealed) {
                        AllZone.getGameAction().moveToGraveyard(c);
                    }
                }
            };
            ability.setStackDescription(abCost + "Reveal cards from the top of your library until you reveal a basic land card." +
                    " Put that card into your hand and all other cards revealed this way into your graveyard.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
*/

        //*************** START *********** START **************************
        else if (cardName.equals("Vedalken Plotter")) {
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

                    if (crd0 != null && crd1 != null) {
                        Player p0 = crd0.getController();
                        Player p1 = crd1.getController();
                        crd0.addController(p1);
                        crd1.addController(p0);
                        //AllZone.getGameAction().changeController(new CardList(crd0), p0, p1);
                        //AllZone.getGameAction().changeController(new CardList(crd1), p1, p0);
                    }

                }//resolve()
            };//SpellAbility


            final Input input = new Input() {

                private static final long serialVersionUID = -7143706716256752987L;

                @Override
                public void showMessage() {
                    if (index[0] == 0) AllZone.getDisplay().showMessage("Select target land you control.");
                    else AllZone.getDisplay().showMessage("Select target land opponent controls.");

                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    //must target creature you control
                    if (index[0] == 0 && !c.getController().equals(card.getController())) return;

                    //must target creature you don't control
                    if (index[0] == 1 && c.getController().equals(card.getController())) return;


                    if (c.isLand() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(card, c)) {
                        //System.out.println("c is: " +c);
                        target[index[0]] = c;
                        index[0]++;
                        showMessage();

                        if (index[0] == target.length) {
                            AllZone.getStack().add(ability);
                            stop();
                        }
                    }
                }//selectCard()
            };//Input

            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 6513203926272187582L;

                public void execute() {
                    index[0] = 0;
                    if (card.getController().isHuman()) AllZone.getInputControl().setInput(input);
                }
            };

            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - Exchange control of target land you control and target land an opponent controls.");
            ability.setStackDescription(sb.toString());

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if (cardName.equals("Wojek Embermage")) {
            Cost abCost = new Cost("T", cardName, true);
            Target tgt = new Target(card, "TgtC");
            final Ability_Activated ability = new Ability_Activated(card, abCost, tgt) {
                private static final long serialVersionUID = -1208482961653326721L;

                @Override
                public boolean canPlayAI() {
                    return (CardFactoryUtil.AI_getHumanCreature(1, card, true).size() != 0)
                            && (AllZone.getPhase().getPhase().equals(Constant.Phase.Main2));
                }

                @Override
                public void chooseTargetAI() {
                    CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
                    list.shuffle();
                    setTargetCard(list.get(0));
                }

                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        CardList list = getRadiance(getTargetCard());
                        for (int i = 0; i < list.size(); i++) {
                            list.get(i).addDamage(1, card);
                        }
                    }
                }//resolve()

                //parameter Card c, is included in CardList
                //no multi-colored cards
                CardList getRadiance(Card c) {
                    if (CardUtil.getColors(c).contains(Constant.Color.Colorless)) {
                        CardList list = new CardList();
                        list.add(c);
                        return list;
                    }

                    CardList sameColor = new CardList();
                    CardList list = AllZoneUtil.getCreaturesInPlay();

                    for (int i = 0; i < list.size(); i++)
                        if (list.get(i).sharesColorWith(c)) sameColor.add(list.get(i));

                    return sameColor;
                }

            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("Radiance - " + abCost + cardName + " deals 1 damage to target creature and each other creature that shares a color with it.");

        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Adarkar Valkyrie")) {
            //tap ability - no cost - target creature - EOT

            final Card[] target = new Card[1];

            final Command destroy = new Command() {
            	private static final long serialVersionUID = -2433442359225521472L;

            	public void execute() {

            		AllZone.getStack().addSimultaneousStackEntry(new Ability(card, "0", "Adarkar Valkyrie - Return " + target[0] + " from graveyard to the battlefield") {
            			@Override
            			public void resolve() {
            				PlayerZone grave = AllZone.getZoneOf(target[0]);
            				//checks to see if card is still in the graveyard

            				if (grave != null && AllZoneUtil.isCardInZone(grave, target[0])) {
            					PlayerZone play = card.getController().getZone(Constant.Zone.Battlefield);
            					target[0].addController(card.getController());
            					AllZone.getGameAction().moveTo(play, target[0]);
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

            Cost abCost = new Cost("T", cardName, true);
            Target tgt = new Target(card, "Target creature other than " + cardName, "Creature.Other".split(","));
            final Ability_Activated ability = new Ability_Activated(card, abCost, tgt) {
                private static final long serialVersionUID = -8454685126878522607L;

                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(getTargetCard())) {
                        target[0] = getTargetCard();

                        if (!target[0].isToken()) {    // not necessary, but will help speed up stack resolution
                            AllZone.getEndOfTurn().addUntil(untilEOT);
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
        else if (cardName.equals("Painter's Servant")) {
            final long[] timeStamp = new long[1];
            final String[] color = new String[1];

            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 333134223161L;

                public void execute() {
                    if (card.getController().isHuman()) {
                        String[] colors = Constant.Color.onlyColors;

                        Object o = GuiUtils.getChoice("Choose color", colors);
                        color[0] = (String) o;
                    } else {
                        // AI chooses the color that appears in the keywords of the most cards in its deck, hand and on battlefield
                        CardList list = new CardList();
                        list.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Library));
                        list.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Hand));
                        list.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));

                        color[0] = Constant.Color.White;
                        int max = list.getKeywordsContain(color[0]).size();

                        String[] colors = {Constant.Color.Blue, Constant.Color.Black, Constant.Color.Red, Constant.Color.Green};
                        for (String c : colors) {
                            int cmp = list.getKeywordsContain(c).size();
                            if (cmp > max) {
                                max = cmp;
                                color[0] = c;
                            }
                        }
                    }
                    card.setChosenColor(color[0]);
                    String s = CardUtil.getShortColor(color[0]);

                    timeStamp[0] = AllZone.getColorChanger().addColorChanges(s, card, true, true);
                }
            };//Command

            Command leavesBattlefield = new Command() {
                private static final long serialVersionUID = 2559212590399132459L;

                public void execute() {
                    String s = CardUtil.getShortColor(color[0]);
                    AllZone.getColorChanger().removeColorChanges(s, card, true, timeStamp[0]);
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
            card.addLeavesPlayCommand(leavesBattlefield);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Stangg")) {

            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList cl = CardFactoryUtil.makeToken("Stangg Twin", "RG 3 4 Stangg Twin", card.getController(), "R G",
                            new String[]{"Legendary", "Creature", "Human", "Warrior"}, 3, 4, new String[]{""});

                    cl.get(0).addLeavesPlayCommand(new Command() {
                        private static final long serialVersionUID = 3367390368512271319L;

                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(card)) AllZone.getGameAction().sacrifice(card);
                        }
                    });
                }
            };
            ability.setStackDescription("When Stangg enters the battlefield, if Stangg is on the battlefield, put a legendary 3/4 red and green Human Warrior creature token named Stangg Twin onto the battlefield.");

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 6667896040611028600L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            });

            card.addLeavesPlayCommand(new Command() {
                private static final long serialVersionUID = 1786900359843939456L;

                public void execute() {
                    CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield, "Stangg Twin");

                    if (list.size() == 1) AllZone.getGameAction().exile(list.get(0));
                }
            });
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Horde of Notions")) {
            final Ability ability = new Ability(card, "W U B R G") {
                @Override
                public void resolve() {
                    Card c = null;
                    if (card.getController().isHuman()) {
                        Object o = GuiUtils.getChoiceOptional("Select Elemental", getCreatures());
                        c = (Card) o;

                    } else {
                        c = getAIElemental();
                    }

                    if (AllZoneUtil.isCardInPlayerGraveyard(card.getController(), c)) {
                        PlayerZone play = c.getController().getZone(Constant.Zone.Battlefield);
                        AllZone.getGameAction().moveTo(play, c);
                    }
                }//resolve()

                @Override
                public boolean canPlay() {
                    return getCreatures().size() != 0 && AllZoneUtil.isCardInPlay(card) && super.canPlay();
                }

                public CardList getCreatures() {
                    CardList creatures = AllZoneUtil.getPlayerTypeIn(card.getController(), Zone.Graveyard, "Elemental");
                    return creatures;
                }

                public Card getAIElemental() {
                    CardList c = getCreatures();
                    Card biggest = c.get(0);
                    for (int i = 0; i < c.size(); i++)
                        if (biggest.getNetAttack() < c.get(i).getNetAttack()) biggest = c.get(i);

                    return biggest;
                }
            };//SpellAbility
            card.addSpellAbility(ability);

            ability.setDescription("W U B R G: You may play target Elemental card from your graveyard without paying its mana cost.");
            ability.setStackDescription("Horde of Notions - play Elemental card from graveyard without paying its mana cost.");
            ability.setBeforePayMana(new Input_PayManaCost(ability));
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Rhys the Redeemed")) {

            Cost abCost = new Cost("4 GW GW T", card.getName(), true);
            final Ability_Activated copyTokens1 = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = 6297992502069547478L;

                @Override
                public void resolve() {
                    CardList allTokens = AllZoneUtil.getCreaturesInPlay(card.getController());
                    allTokens = allTokens.filter(AllZoneUtil.token);

                    int multiplier = AllZoneUtil.getDoublingSeasonMagnitude(card.getController());

                    for (int i = 0; i < allTokens.size(); i++) {
                        Card c = allTokens.get(i);
                        for (int j = 0; j < multiplier; j++)
                            copyToken(c);
                    }
                }

                public void copyToken(Card token) {
                    Card copy = new Card();
                    copy.setName(token.getName());
                    copy.setImageName(token.getImageName());

                    copy.setOwner(token.getController());
                    copy.addController(token.getController());
                    copy.setManaCost(token.getManaCost());
                    copy.setColor(token.getColor());
                    copy.setToken(true);
                    copy.setType(token.getType());
                    copy.setBaseAttack(token.getBaseAttack());
                    copy.setBaseDefense(token.getBaseDefense());

                    AllZone.getGameAction().moveToPlay(copy);
                }

                @Override
                public boolean canPlayAI() {
                    CardList allTokens = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    allTokens = allTokens.filter(AllZoneUtil.token);

                    return allTokens.size() >= 2;
                }
            };

            card.addSpellAbility(copyTokens1);
            copyTokens1.setDescription(abCost + "For each creature token you control, put a token that's a copy of that creature onto the battlefield.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - For each creature token you control, put a token that's a copy of that creature onto the battlefield.");
            copyTokens1.setStackDescription(sb.toString());
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Treva, the Renewer")) {
            final Player player = card.getController();

            final Ability ability2 = new Ability(card, "2 W") {
                @Override
                public void resolve() {
                    int lifeGain = 0;
                    if (card.getController().isHuman()) {
                        String choices[] = {"white", "blue", "black", "red", "green"};
                        Object o = GuiUtils.getChoiceOptional("Select Color: ", choices);
                        Log.debug("Treva, the Renewer", "Color:" + o);
                        lifeGain = CardFactoryUtil.getNumberOfPermanentsByColor((String) o);

                    } else {
                        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
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
        else if (cardName.equals("Rith, the Awakener")) {
            final Player player = card.getController();

            final Ability ability2 = new Ability(card, "2 G") {
                @Override
                public void resolve() {
                    int numberTokens = 0;
                    if (card.getController().isHuman()) {
                        String choices[] = {"white", "blue", "black", "red", "green"};
                        Object o = GuiUtils.getChoiceOptional("Select Color: ", choices);
                        //System.out.println("Color:" + o);
                        numberTokens = CardFactoryUtil.getNumberOfPermanentsByColor((String) o);
                    } else {
                        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
                        String color = CardFactoryUtil.getMostProminentColor(list);
                        numberTokens = CardFactoryUtil.getNumberOfPermanentsByColor(color);
                    }

                    for (int i = 0; i < numberTokens; i++) {
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
        else if (cardName.equals("Sphinx of Jwar Isle")) {
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Player player = card.getController();
                    PlayerZone lib = player.getZone(Constant.Zone.Library);

                    if (lib.size() < 1) return;

                    CardList cl = new CardList();
                    cl.add(lib.get(0));

                    GuiUtils.getChoiceOptional("Top card", cl.toArray());
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
        else if (cardName.equals("Master of the Wild Hunt")) {

            final Cost abCost = new Cost("T", cardName, true);
            final Target abTgt = new Target(card, "Target a creature to Hunt", "Creature".split(","));
            final Ability_Activated ability = new Ability_Activated(card, abCost, abTgt) {
                private static final long serialVersionUID = 35050145102566898L;

                @Override
                public boolean canPlayAI() {
                    CardList wolves = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    wolves = wolves.getType("Wolf");

                    wolves = wolves.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && c.isCreature();
                        }
                    });
                    int power = 0;
                    for (int i = 0; i < wolves.size(); i++)
                        power += wolves.get(i).getNetAttack();

                    if (power == 0)
                        return false;

                    final int totalPower = power;

                    CardList targetables = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);

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
                    CardList wolves = card.getController().getCardsIn(Zone.Battlefield);
                    wolves = wolves.getType("Wolf");

                    wolves = wolves.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && c.isCreature();
                        }
                    });

                    final Card target = getTargetCard();

                    if (wolves.size() == 0)
                        return;

                    if (!(CardFactoryUtil.canTarget(card, target) && AllZoneUtil.isCardInPlay(target)))
                        return;

                    for (Card c : wolves) {
                        c.tap();
                        target.addDamage(c.getNetAttack(), c);
                    }

                    if (target.getController().isHuman()) {    // Human choose spread damage
                        for (int x = 0; x < target.getNetAttack(); x++) {
                            AllZone.getInputControl().setInput(CardFactoryUtil.MasteroftheWildHunt_input_targetCreature(this, wolves, new Command() {
                                private static final long serialVersionUID = -328305150127775L;

                                public void execute() {
                                    getTargetCard().addDamage(1, target);
                                    AllZone.getGameAction().checkStateEffects();
                                }
                            }));
                        }
                    } else {        // AI Choose spread Damage
                        CardList damageableWolves = wolves.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return (c.predictDamage(target.getNetAttack(), target, false) > 0);
                            }
                        });

                        if (damageableWolves.size() == 0)    // don't bother if I can't damage anything
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
                                if (best.getKillDamage() <= 0 || target.hasKeyword("Deathtouch")) {
                                    wolvesLeft.remove(best);
                                }
                            } else {
                                // Add -1/-1s to Random Indestructibles
                                if (target.hasKeyword("Infect") || target.hasKeyword("Wither")) {
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

/*
        //*************** START *********** START **************************
        else if (cardName.equals("Figure of Destiny")) {
            Ability ability1 = new Ability(card, "RW") {
                @Override
                public void resolve() {
                    boolean artifact = false;
                    card.setBaseAttack(2);
                    card.setBaseDefense(2);

                    card.removeIntrinsicKeyword("Flying");
                    card.removeIntrinsicKeyword("First Strike");

                    if (card.isArtifact()) artifact = true;

                    card.setType(new ArrayList<String>());
                    if (artifact) card.addType("Artifact");
                    card.addType("Creature");
                    card.addType("Kithkin");
                    card.addType("Spirit");
                }

                @Override
                public boolean canPlayAI() {
                    return !card.isType("Spirit")
                            && super.canPlayAI();
                }

            };// ability1

            ability1.setDescription("RW: Figure of Destiny becomes a 2/2 Kithkin Spirit.");
            ability1.setStackDescription("Figure of Destiny becomes a 2/2 Kithkin Spirit.");
            card.addSpellAbility(ability1);


            Ability ability2 = new Ability(card, "RW RW RW") {
                @Override
                public void resolve() {
                    if (card.isType("Spirit")) {
                        boolean artifact = false;
                        card.setBaseAttack(4);
                        card.setBaseDefense(4);

                        card.removeIntrinsicKeyword("Flying");
                        card.removeIntrinsicKeyword("First Strike");

                        if (card.isArtifact()) artifact = true;

                        card.setType(new ArrayList<String>());
                        if (artifact) card.addType("Artifact");
                        card.addType("Creature");
                        card.addType("Kithkin");
                        card.addType("Spirit");
                        card.addType("Warrior");
                    }
                }

                @Override
                public boolean canPlay() {
                    return card.isType("Spirit")
                            && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    return !card.isType("Warrior")
                            && super.canPlayAI();
                }

            };// ability2

            ability2.setDescription("RW RW RW: If Figure of Destiny is a Spirit, it becomes a 4/4 Kithkin Spirit Warrior.");
            ability2.setStackDescription("Figure of Destiny becomes a 4/4 Kithkin Spirit Warrior.");
            card.addSpellAbility(ability2);


            Ability ability3 = new Ability(card, "RW RW RW RW RW RW") {
                @Override
                public void resolve() {
                    if (card.isType("Warrior")) {
                        boolean artifact = false;
                        card.setBaseAttack(8);
                        card.setBaseDefense(8);

                        card.addIntrinsicKeyword("Flying");
                        card.addIntrinsicKeyword("First Strike");

                        if (card.isArtifact()) artifact = true;

                        card.setType(new ArrayList<String>());
                        if (artifact) card.addType("Artifact");
                        card.addType("Creature");
                        card.addType("Kithkin");
                        card.addType("Spirit");
                        card.addType("Warrior");
                        card.addType("Avatar");
                    }
                }

                @Override
                public boolean canPlay() {
                    return card.isType("Warrior")
                            && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    return !card.isType("Avatar")
                            && super.canPlayAI();
                }
            };// ability3

            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("RW RW RW RW RW RW: If Figure of Destiny is a Warrior, it becomes ");
            sbDesc.append("an 8/8 Kithkin Spirit Warrior Avatar with flying and first strike.");
            ability3.setDescription(sbDesc.toString());

            ability3.setStackDescription("Figure of Destiny becomes an 8/8 Kithkin Spirit Warrior Avatar with flying and first strike.");
            card.addSpellAbility(ability3);
        }//*************** END ************ END **************************

*/
        //*************** START *********** START **************************
        else if (cardName.equals("Cantivore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7254358703158629514L;

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCardsIn(Zone.Graveyard);
                    list = list.getType("Enchantment");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Terravore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7316190829288665283L;

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCardsIn(Zone.Graveyard);
                    list = list.getType("Land");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Mortivore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -7118801410173525870L;

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCardsIn(Zone.Graveyard);
                    list = list.getType("Creature");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Cognivore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -2216181341715046786L;

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCardsIn(Zone.Graveyard);
                    list = list.getType("Instant");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Magnivore")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -2252263708643462897L;

                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCardsIn(Zone.Graveyard);
                    list = list.getType("Sorcery");
                    return super.canPlayAI() && list.size() > 0;
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Shifting Wall") || cardName.equals("Maga, Traitor to Mortals") || cardName.equals("Feral Hydra")
                || cardName.equals("Krakilin") || cardName.equals("Ivy Elemental") || cardName.equals("Lightning Serpent")) {

            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7708945715867177172L;

                @Override
                public boolean canPlayAI() {
                    return super.canPlay() && 4 <= ComputerUtil.getAvailableMana().size() - CardUtil.getConvertedManaCost(card.getManaCost());
                }
            };
            card.clearFirstSpell();
            card.addFirstSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Apocalypse Hydra")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -11489323313L;

                @Override
                public boolean canPlayAI() {
                    return super.canPlay() && 5 <= ComputerUtil.getAvailableMana().size() - 2;
                }

                @Override
                public void resolve() {
                    int XCounters = card.getXManaCostPaid();
                    Card c = AllZone.getGameAction().moveToPlay(getSourceCard());

                    if (XCounters >= 5) XCounters = 2 * XCounters;
                    c.addCounter(Counters.P1P1, XCounters);
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Molten Hydra")) {
            Target target = new Target(card, "TgtCP");
            Cost abCost = new Cost("T", cardName, true);
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
                    if (AllZone.getHumanPlayer().getLife() < card.getCounters(Counters.P1P1))
                        setTargetPlayer(AllZone.getHumanPlayer());
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
                    if (getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(total,
                                card);
                    } else getTargetPlayer().addDamage(total, card);
                    card.subtractCounter(Counters.P1P1, total);
                }//resolve()
            };//SpellAbility

            card.addSpellAbility(ability2);

            StringBuilder sb = new StringBuilder();
            sb.append(abCost + "Remove all +1/+1 counters from " + cardName + ":  " + cardName);
            sb.append(" deals damage to target creature or player equal to the number of +1/+1 counters removed this way.");
            ability2.setDescription(sb.toString());

            ability2.setStackDescription("Molten Hydra deals damage to number of +1/+1 counters on it to target creature or player.");
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Academy Rector") || cardName.equals("Lost Auramancers")) {
            final SpellAbility ability = new Ability(card, "0") {

                @Override
                public void resolve() {

                    if (card.getController().isHuman()) {
                        StringBuilder question = new StringBuilder();
                        if (card.getName().equals("Academy Rector")) {
                            question.append("Exile ").append(card.getName()).append(" and place ");
                        } else {
                            question.append("Place ");
                        }
                        question.append("an enchantment from your library onto the battlefield?");

                        if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                            if (card.getName().equals("Academy Rector")) {
                                AllZone.getGameAction().exile(card);
                            }
                            CardList list = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                            list = list.getType("Enchantment");

                            if (list.size() > 0) {
                                Object objectSelected = GuiUtils.getChoiceOptional("Choose an enchantment", list.toArray());

                                if (objectSelected != null) {

                                    final Card c = (Card) objectSelected;
                                    AllZone.getGameAction().moveToPlay(c);

                                    if (c.isAura()) {

                                        String enchantThisType[] = {""};
                                        String message[] = {""};

                                        // The type following "Enchant" maybe upercase or lowercase, cardsfolder has both
                                        // Note that I am being overly cautious.

                                        if (c.hasKeyword("Enchant creature without flying")
                                                || c.hasKeyword("Enchant Creature without flying")) {
                                            enchantThisType[0] = "Creature.withoutFlying";
                                            message[0] = "Select a creature without flying";
                                        } else if (c.hasKeyword("Enchant creature with converted mana cost 2 or less")
                                                || c.hasKeyword("Enchant Creature with converted mana cost 2 or less")) {
                                            enchantThisType[0] = "Creature.cmcLE2";
                                            message[0] = "Select a creature with converted mana cost 2 or less";
                                        } else if (c.hasKeyword("Enchant red or green creature")) {
                                            enchantThisType[0] = "Creature.Red,Creature.Green";
                                            message[0] = "Select a red or green creature";
                                        } else if (c.hasKeyword("Enchant tapped creature")) {
                                            enchantThisType[0] = "Creature.tapped";
                                            message[0] = "Select a tapped creature";
                                        } else if (c.hasKeyword("Enchant creature")
                                                || c.hasKeyword("Enchant Creature")) {
                                            enchantThisType[0] = "Creature";
                                            message[0] = "Select a creature";
                                        } else if (c.hasKeyword("Enchant wall")
                                                || c.hasKeyword("Enchant Wall")) {
                                            enchantThisType[0] = "Wall";
                                            message[0] = "Select a Wall";
                                        } else if (c.hasKeyword("Enchant land you control")
                                                || c.hasKeyword("Enchant Land you control")) {
                                            enchantThisType[0] = "Land.YouCtrl";
                                            message[0] = "Select a land you control";
                                        } else if (c.hasKeyword("Enchant land")
                                                || c.hasKeyword("Enchant Land")) {
                                            enchantThisType[0] = "Land";
                                            message[0] = "Select a land";
                                        } else if (c.hasKeyword("Enchant artifact")
                                                || c.hasKeyword("Enchant Artifact")) {
                                            enchantThisType[0] = "Artifact";
                                            message[0] = "Select an artifact";
                                        } else if (c.hasKeyword("Enchant enchantment")
                                                || c.hasKeyword("Enchant Enchantment")) {
                                            enchantThisType[0] = "Enchantment";
                                            message[0] = "Select an enchantment";
                                        }

                                        CardList allCards = AllZoneUtil.getCardsIn(Zone.Battlefield);

                                        // Make sure that we were able to match the selected aura with our list of criteria

                                        if (enchantThisType[0] != "" && message[0] != "") {

                                            final CardList choices = allCards.getValidCards(enchantThisType[0], card.getController(), card);
                                            final String msg = message[0];

                                            AllZone.getInputControl().setInput(new Input() {
                                                private static final long serialVersionUID = -6271957194091955059L;

                                                @Override
                                                public void showMessage() {
                                                    AllZone.getDisplay().showMessage(msg);
                                                    ButtonUtil.enableOnlyOK();
                                                }

                                                @Override
                                                public void selectButtonOK() {
                                                    stop();
                                                }

                                                @Override
                                                public void selectCard(Card card, PlayerZone zone) {
                                                    if (choices.contains(card)) {

                                                        if (AllZoneUtil.isCardInPlay(card)) {
                                                            c.enchantEntity(card);
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
                        CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Library);
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isEnchantment() && !c.isAura();
                            }
                        });

                        if (list.size() > 0) {
                            Card c = CardFactoryUtil.AI_getBestEnchantment(list, card, false);

                            AllZone.getGameAction().moveToPlay(c);
                            if (card.getName().equals("Academy Rector")) {
                                AllZone.getGameAction().exile(card);
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
                        AllZone.getStack().addSimultaneousStackEntry(ability);

                    } else if (card.getName().equals("Academy Rector")) {
                        AllZone.getStack().addSimultaneousStackEntry(ability);

                    }

                }// execute()
            };// Command destroy

            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Deadly Grub")) {
            final Command destroy = new Command() {
                private static final long serialVersionUID = -4352349741511065318L;

                public void execute() {
                    if (card.getCounters(Counters.TIME) <= 0) CardFactoryUtil.makeToken("Insect", "G 6 1 Insect",
                            card.getController(), "G", new String[]{"Creature", "Insect"}, 6, 1, new String[]{"Shroud"});
                }
            };

            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Meddling Mage")) {
            final String[] input = new String[1];
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if (card.getController().isHuman()) {
                        input[0] = JOptionPane.showInputDialog(null, "Which card?", "Pick card",
                                JOptionPane.QUESTION_MESSAGE);
                        card.setNamedCard(input[0]);
                    } else {
                        String s = "Ancestral Recall";

                        CardList list = new CardList();
                        list.addAll(AllZone.getHumanPlayer().getCardsIn(Zone.Hand));
                        list.addAll(AllZone.getHumanPlayer().getCardsIn(Zone.Library));
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(final Card c) {
                                return !c.isLand() && !c.isUnCastable();
                            }
                        });

                        if (list.size() > 0) {
                            Predicate<CardPrinted> isRare = CardPrinted.Predicates.Presets.isRareOrMythic;
                            List<CardPrinted> rares = isRare.select(list, CardDb.fnGetCardPrintedByForgeCard, CardDb.fnGetCardPrintedByForgeCard);

                            if (!rares.isEmpty()) {
                                s = Predicate.getTrue(CardPrinted.class).random(rares).getName();
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
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };//Command
            ability.setStackDescription("As Meddling Mage enters the battlefield, name a nonland card.");
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Iona, Shield of Emeria")) {
            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 3331342605626623161L;

                public void execute() {
                    if (card.getController().isHuman()) {

                        String color = "";
                        String[] colors = Constant.Color.Colors;
                        colors[colors.length - 1] = null;

                        Object o = GuiUtils.getChoice("Choose color", colors);
                        color = (String) o;
                        card.setChosenColor(color);
                    } else {
                        CardList list = new CardList();
                        list.addAll(AllZone.getHumanPlayer().getCardsIn(Zone.Library));
                        list.addAll(AllZone.getHumanPlayer().getCardsIn(Zone.Hand));

                        if (list.size() > 0) {
                            String color = CardFactoryUtil.getMostProminentColor(list);
                            if (!color.equals("")) card.setChosenColor(color);
                            else card.setChosenColor("black");
                        } else {
                            card.setChosenColor("black");
                        }
                    }
                }
            };//Command
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Singe-Mind Ogre")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Player opponent = card.getController().getOpponent();
                    CardList handChoices = opponent.getCardsIn(Zone.Hand);
                    if (handChoices.size() > 0) {
                        Card random = CardUtil.getRandom(handChoices.toArray());
                        CardList reveal = new CardList(random);
                        GuiUtils.getChoice("Random card", reveal);
                        opponent.loseLife(CardUtil.getConvertedManaCost(random.getManaCost()), card);
                    }
                }//resolve()
            };
            Command intoPlay = new Command() {

                private static final long serialVersionUID = -4833144157620224716L;

                public void execute() {
                    ability.setStackDescription("When CARDNAME enters the battlefield, target player reveals a card at random from his or her hand, then loses life equal to that card's converted mana cost.");
                    AllZone.getStack().addSimultaneousStackEntry(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Kinsbaile Borderguard")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, countKithkin());
                    //System.out.println("all counters: " +card.sumAllCounters());
                }//resolve()

                public int countKithkin() {
                    CardList kithkin = card.getController().getCardsIn(Zone.Battlefield);
                    kithkin = kithkin.filter(new CardListFilter() {

                        public boolean addCard(Card c) {
                            return (c.isType("Kithkin"))
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
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };

            final SpellAbility ability2 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    for (int i = 0; i < card.sumAllCounters(); i++) {
                        makeToken();
                    }
                }//resolve()

                public void makeToken() {
                    CardFactoryUtil.makeToken("Kithkin Soldier", "W 1 1 Kithkin Soldier", card.getController(), "W", new String[]{
                            "Creature", "Kithkin", "Soldier"}, 1, 1, new String[]{""});
                }
            };

            Command destroy = new Command() {
                private static final long serialVersionUID = 304026662487997331L;

                public void execute() {
                    ability2.setStackDescription("When Kinsbaile Borderguard is put into a graveyard from play, put a 1/1 white " +
                            "Kithkin Soldier creature token onto the battlefield for each counter on it.");
                    AllZone.getStack().addSimultaneousStackEntry(ability2);

                }
            };

            card.addComesIntoPlayCommand(intoPlay);
            card.addDestroyCommand(destroy);

        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Arctic Nishoba")) {
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

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };//command

            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************


        //*************** START *********** START ************************** 
        else if (cardName.equals("Kavu Titan")) {
            final SpellAbility kicker = new Spell(card) {
                private static final long serialVersionUID = -1598664196463358630L;

                @Override
                public void resolve() {
                    card.setKicked(true);
                    AllZone.getGameAction().moveToPlay(card);
                }

                @Override
                public boolean canPlay() {
                    return super.canPlay() && AllZone.getPhase().getPlayerTurn().equals(card.getController())
                            && !AllZone.getPhase().getPhase().equals("End of Turn")
                            && !AllZoneUtil.isCardInPlay(card);
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
                    if (card.isKicked()) {
                        ability.setStackDescription("Kavu Titan gets 3 +1/+1 counters and gains trample.");
                        AllZone.getStack().addSimultaneousStackEntry(ability);

                    }
                }//execute()
            };//CommandComes

            card.addComesIntoPlayCommand(commandComes);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Gnarlid Pack") || cardName.equals("Apex Hawks") || cardName.equals("Enclave Elite") ||
                cardName.equals("Quag Vampires") || cardName.equals("Skitter of Lizards") ||
                cardName.equals("Joraga Warcaller")) {
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
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if (cardName.equals("Storm Entity")) {
            final SpellAbility intoPlay = new Ability(card, "0") {

                @Override
                public boolean canPlayAI() {
                    CardList human = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    CardListUtil.sortAttack(human);
                    return (human.get(0).getNetAttack() < Phase.getStormCount() && Phase.getStormCount() > 1);
                }

                @Override
                public void resolve() {
                    for (int i = 0; i < Phase.getStormCount() - 1; i++) {
                        card.addCounter(Counters.P1P1, 1);
                    }
                }
            };//SpellAbility

            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = -3734151854295L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(intoPlay);

                }
            };

            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - enters the battlefield with a +1/+1 counter on it for each other spell played this turn.");
            intoPlay.setStackDescription(sb.toString());

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Vampire Hexmage")) {
            /*
                * Sacrifice Vampire Hexmage: Remove all counters from target permanent.
                */

            Cost cost = new Cost("Sac<1/CARDNAME>", cardName, true);
            final Target tgt = new Target(card, "Select a permanent", "Permanent".split(","));
            final SpellAbility ability = new Ability_Activated(card, cost, tgt) {
                private static final long serialVersionUID = -5084369399105353155L;

                @Override
                public boolean canPlayAI() {

                    //Dark Depths:
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield, "Dark Depths");
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card crd) {
                            return crd.getCounters(Counters.ICE) >= 3;
                        }
                    });

                    if (list.size() > 0) {
                        tgt.addTarget(list.get(0));
                        return true;
                    }

                    //Get rid of Planeswalkers:
                    list = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card crd) {
                            return crd.isPlaneswalker() && crd.getCounters(Counters.LOYALTY) >= 5;
                        }
                    });

                    if (list.size() > 0) {
                        tgt.addTarget(list.get(0));
                        return true;
                    }

                    return false;
                }

                @Override
                public void resolve() {
                    final Card c = getTargetCard();
                    for (Counters counter : Counters.values()) {
                        if (c.getCounters(counter) > 0) {
                            c.setCounter(counter, 0, false);
                        }
                    }
                }
            };
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Sutured Ghoul")) {
            final int[] numCreatures = new int[1];
            final int[] sumPower = new int[1];
            final int[] sumToughness = new int[1];

            Command intoPlay = new Command() {
                private static final long serialVersionUID = -75234586897814L;

                public void execute() {
                    int intermSumPower, intermSumToughness;
                    intermSumPower = intermSumToughness = 0;
                    CardList creats = card.getController().getCardsIn(Zone.Graveyard);
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && !c.equals(card);
                        }
                    });

                    if (card.getController().isHuman()) {
                        if (creats.size() > 0) {
                            List<Card> selection = GuiUtils.getChoicesOptional("Select creatures to sacrifice", creats.toArray());

                            numCreatures[0] = selection.size();
                            for (int m = 0; m < selection.size(); m++) {
                                intermSumPower += selection.get(m).getBaseAttack();
                                intermSumToughness += selection.get(m).getBaseDefense();
                                AllZone.getGameAction().exile(selection.get(m));
                            }
                        }

                    }//human
                    else {
                        int count = 0;
                        for (int i = 0; i < creats.size(); i++) {
                            Card c = creats.get(i);
                            if (c.getNetAttack() <= 2 && c.getNetDefense() <= 3) {
                                intermSumPower += c.getBaseAttack();
                                intermSumToughness += c.getBaseDefense();
                                AllZone.getGameAction().exile(c);
                                count++;
                            }
                            //is this needed?
                            AllZone.getComputerPlayer().getZone(Zone.Battlefield).updateObservers();
                        }
                        numCreatures[0] = count;
                    }
                    sumPower[0] = intermSumPower;
                    sumToughness[0] = intermSumToughness;
                    card.setBaseAttack(sumPower[0]);
                    card.setBaseDefense(sumToughness[0]);
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addComesIntoPlayCommand(intoPlay);
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 304885517082977723L;

                @Override
                public boolean canPlayAI() {
                    //get all creatures
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Graveyard);
                    list = list.filter(AllZoneUtil.creatures);
                    return 0 < list.size();
                }
            });
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Nameless Race")) {
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
                    CardList play = opp.getCardsIn(Zone.Battlefield);
                    play = play.filter(AllZoneUtil.nonToken);
                    play = play.filter(AllZoneUtil.white);
                    max += play.size();

                    CardList grave = opp.getCardsIn(Zone.Graveyard);
                    grave = grave.filter(AllZoneUtil.white);
                    max += grave.size();

                    String[] life = new String[max + 1];
                    for (int i = 0; i <= max; i++) {
                        life[i] = String.valueOf(i);
                    }

                    Object o = GuiUtils.getChoice("Nameless Race - pay X life", life);
                    String answer = (String) o;
                    int loseLife = 0;
                    try {
                        loseLife = Integer.parseInt(answer.trim());
                    } catch (NumberFormatException nfe) {
                        System.out.println(card.getName() + " - NumberFormatException: " + nfe.getMessage());
                    }

                    card.setBaseAttack(loseLife);
                    card.setBaseDefense(loseLife);

                    player.loseLife(loseLife, card);
                }//resolve()
            };//SpellAbility

            Command intoPlay = new Command() {
                private static final long serialVersionUID = 931101364538995898L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };

            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - pay any amount of life.");
            ability.setStackDescription(sb.toString());

            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Banshee")) {
            /*
                * X, Tap: Banshee deals half X damage, rounded down, to target creature or
                * player, and half X damage, rounded up, to you.
                */

            Cost abCost = new Cost("X T", cardName, true);
            Target tgt = new Target(card, "TgtCP");

            final Ability_Activated ability = new Ability_Activated(card, abCost, tgt) {
                private static final long serialVersionUID = 2755743211116192949L;

                @Override
                public void resolve() {
                    int x = card.getXManaCostPaid();
                    if (getTargetPlayer() == null) {
                        getTargetCard().addDamage((int) Math.floor(x / 2.0), card);
                    } else {
                        getTargetPlayer().addDamage((int) Math.floor(x / 2.0), card);
                    }
                    card.getController().addDamage((int) Math.ceil(x / 2.0), card);
                    card.setXManaCostPaid(0);
                }//resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }

            };//SpellAbility

            ability.setDescription("X, tap: " + "Banshee deals half X damage, rounded down, to target creature or player, and half X damage, rounded up, to you.");
            ability.setStackDescription(card.getName() + " - Banshee deals half X damage, rounded down, to target creature or player, and half X damage, rounded up, to you.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Shapeshifter")) {
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 5447692676152380940L;

                public void execute() {
                    if (!card.isToken()) {  //ugly hack to get around tokens created by Crib Swap
                        int num = 0;
                        if (card.getController().isHuman()) {
                            String[] choices = new String[7];
                            for (int j = 0; j < 7; j++) {
                                choices[j] = "" + j;
                            }
                            String answer = (String) (GuiUtils.getChoiceOptional(
                                    card.getName() + " - Choose a number", choices));
                            num = Integer.parseInt(answer);
                        } else {
                            num = 3;
                        }
                        card.setBaseAttack(num);
                        card.setBaseDefense(7 - num);
                    }
                }
            };

            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Metalworker")) {
            final Cost abCost = new Cost("T", card.getName(), true);

            final SpellAbility ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = 6661308920885136284L;

                @Override
                public boolean canPlayAI() {
                    //compy doesn't have a manapool
                    return false;
                }//canPlayAI()

                @Override
                public void resolve() {
                    AllZone.getInputControl().setInput(new Input() {
                        private static final long serialVersionUID = 6150236529653275947L;
                        CardList revealed = new CardList();

                        @Override
                        public void showMessage() {
                            //in case hand is empty, don't do anything
                            if (card.getController().getCardsIn(Zone.Hand).size() == 0) stop();

                            AllZone.getDisplay().showMessage(card.getName() + " - Reveal an artifact.  Revealed " + revealed.size() + " so far.  Click OK when done.");
                            ButtonUtil.enableOnlyOK();
                        }

                        @Override
                        public void selectCard(Card c, PlayerZone zone) {
                            if (zone.is(Constant.Zone.Hand) && c.isArtifact() && !revealed.contains(c)) {
                                revealed.add(c);

                                //in case no more cards in hand to reveal
                                if (revealed.size() == card.getController().getCardsIn(Zone.Hand).size()) done();
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
                            for (Card reveal : revealed) sb.append(reveal.getName() + "\n");
                            JOptionPane.showMessageDialog(null, "Revealed Cards:\n" + sb.toString(), card.getName(), JOptionPane.PLAIN_MESSAGE);
                            //adding mana

                            Ability_Mana abMana = new Ability_Mana(card, "0", "1", 2 * revealed.size()) {
                                private static final long serialVersionUID = -2182129023960978132L;
                            };
                            abMana.setUndoable(false);
                            abMana.produceMana();

                            stop();
                        }
                    });
                }//resolve()
            };//SpellAbility

            ability.setDescription(abCost + "Reveal any number of artifact cards in your hand. Add 2 to your mana pool for each card revealed this way.");
            ability.setStackDescription(cardName + " - Reveal any number of artifact cards in your hand.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Necratog")) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 6743592637334556854L;

                public void execute() {
                    if (AllZoneUtil.isCardInPlay(card)) {
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
                    CardList grave = card.getController().getCardsIn(Zone.Graveyard);
                    grave = grave.filter(AllZoneUtil.creatures);
                    return super.canPlay() && grave.size() > 0;
                }

                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(card)) {
                        card.addTempAttackBoost(2);
                        card.addTempDefenseBoost(2);
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }
            };

            Input runtime = new Input() {
                private static final long serialVersionUID = 63327418012595048L;
                Card topCreature = null;

                public void showMessage() {

                    PlayerZone grave = card.getController().getZone(Constant.Zone.Graveyard);
                    for (int i = grave.size() - 1; i >= 0; i--) {
                        Card c = grave.get(i);
                        if (c.isCreature()) {
                            topCreature = c;
                            break;
                        }
                    }
                    AllZone.getDisplay().showMessage(card.getName() + " - Select OK to exile " + topCreature + ".");
                    ButtonUtil.enableAll();
                }

                public void selectButtonOK() {
                    AllZone.getGameAction().exile(topCreature);
                    AllZone.getStack().add(ability);
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
        else if (cardName.equals("Phyrexian Scuta")) {
            Cost abCost = new Cost("3 B PayLife<3>", cardName, false);
            final SpellAbility kicker = new Spell(card, abCost, null) {
                private static final long serialVersionUID = -6420757044982294960L;

                @Override
                public void resolve() {
                    card.setKicked(true);
                    AllZone.getGameAction().moveToPlay(card);
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


        //*************** START *********** START **************************
        else if (cardName.equals("Yosei, the Morning Star")) {
            final CardList targetPerms = new CardList();
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Player p = getTargetPlayer();
                    if (p.canTarget(this)) {
                        p.setSkipNextUntap(true);
                        for (Card c : targetPerms) {
                            if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
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
                    if (targetPerms.size() == 5) done();
                    AllZone.getDisplay().showMessage("Select up to 5 target permanents.  Selected (" + targetPerms.size() + ") so far.  Click OK when done.");
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
                    AllZone.getStack().add(ability);
                    stop();
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (zone.is(Constant.Zone.Battlefield, ability.getTargetPlayer()) && !targetPerms.contains(c)) {
                        if (CardFactoryUtil.canTarget(card, c)) {
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
                    AllZone.getDisplay().showMessage(card.getName() + " - Select target player");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectPlayer(Player p) {
                    if (p.canTarget(ability)) {
                        ability.setTargetPlayer(p);
                        stopSetNext(targetInput);
                    }
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }
            };

            Command destroy = new Command() {
                private static final long serialVersionUID = -3868616119471172026L;

                public void execute() {
                    Player player = card.getController();
                    CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);

                    if (player.isHuman()) AllZone.getInputControl().setInput(playerInput);
                    else if (list.size() != 0) {
                        Card target = CardFactoryUtil.AI_getBestCreature(list);
                        ability.setTargetCard(target);
                        AllZone.getStack().addSimultaneousStackEntry(ability);

                    }
                }//execute()
            };
            card.addDestroyCommand(destroy);
        }
        //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Phyrexian Dreadnought")) {
            final Player player = card.getController();
            final CardList toSac = new CardList();

            final Ability sacOrSac = new Ability(card, "") {
                @Override
                public void resolve() {
                    if (player.isHuman()) {
                        Input target = new Input() {
                            private static final long serialVersionUID = 2698036349873486664L;

                            @Override
                            public void showMessage() {
                                String toDisplay = cardName + " - Select any number of creatures to sacrifice.  ";
                                toDisplay += "Currently, (" + toSac.size() + ") selected with a total power of: " + getTotalPower();
                                toDisplay += "  Click OK when Done.";
                                AllZone.getDisplay().showMessage(toDisplay);
                                ButtonUtil.enableAll();
                            }

                            @Override
                            public void selectButtonOK() {
                                done();
                            }

                            @Override
                            public void selectButtonCancel() {
                                toSac.clear();
                                AllZone.getGameAction().sacrifice(card);
                                stop();
                            }

                            @Override
                            public void selectCard(Card c, PlayerZone zone) {
                                if (c.isCreature() && zone.is(Constant.Zone.Battlefield, AllZone.getHumanPlayer())
                                        && !toSac.contains(c)) {
                                    toSac.add(c);
                                }
                                showMessage();
                            }//selectCard()

                            private void done() {
                                if (getTotalPower() >= 12) {
                                    for (Card sac : toSac) AllZone.getGameAction().sacrifice(sac);
                                } else {
                                    AllZone.getGameAction().sacrifice(card);
                                }
                                toSac.clear();
                                stop();
                            }
                        };//Input
                        AllZone.getInputControl().setInput(target);
                    }
                }//end resolve

                private int getTotalPower() {
                    int sum = 0;
                    for (Card c : toSac) {
                        sum += c.getNetAttack();
                    }
                    return sum;
                }
            };// end sacOrSac

            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 7680692311339496770L;

                public void execute() {
                    sacOrSac.setStackDescription("When " + cardName + " enters the battlefield, sacrifice it unless you sacrifice any number of creatures with total power 12 or greater.");
                    AllZone.getStack().addSimultaneousStackEntry(sacOrSac);

                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Clone") || cardName.equals("Vesuvan Doppelganger")
                || cardName.equals("Quicksilver Gargantuan")
                || cardName.equals("Jwari Shapeshifter")
                || cardName.equals("Phyrexian Metamorph")
                || cardName.equals("Phantasmal Image")) {
            final CardFactoryInterface cfact = cf;
            final Card[] copyTarget = new Card[1];
            final Card[] cloned = new Card[1];

            final Command leaves = new Command() {
                private static final long serialVersionUID = 8590474793502538215L;

                public void execute() {
                    //Slight hack if the cloner copies a card with triggers
                    AllZone.getTriggerHandler().removeAllFromCard(cloned[0]);

                    Card orig = cfact.getCard(card.getName(), card.getController());
                    PlayerZone dest = AllZone.getZoneOf(card.getCurrentlyCloningCard());
                    AllZone.getGameAction().moveTo(dest, orig);
                    dest.remove(card.getCurrentlyCloningCard());

                }
            };

            final SpellAbility copy = new Spell(card) {
                private static final long serialVersionUID = 4496978456522751302L;

                @Override
                public void resolve() {
                    if (card.getController().isComputer()) {
                        CardList creatures = AllZoneUtil.getCreaturesInPlay();
                        if (!creatures.isEmpty()) {
                            copyTarget[0] = CardFactoryUtil.AI_getBestCreature(creatures);
                        }
                    }

                    if (copyTarget[0] != null) {
                    	/*
                    	 * This cannot just be copyStats with an addSpellAbility loop from copyTarget[0].
                    	 * Unless we get a copySpellAbility.  Adding the SpellAbility from the
                    	 * source card causes many weird and Bad Things to happen.
                    	 */
                    	try {
                    		cloned[0] = cfact.getCard(copyTarget[0].getName(), card.getController());
                    	}
                    	catch(RuntimeException re) {
                    		//the copyTarget was not found in CardFactory
                    		cloned[0] = CardFactoryUtil.copyStats(copyTarget[0]);
                    	}
                        cloned[0].setOwner(card.getController());
                        cloned[0].addController(card.getController());
                        if (cardName.equals("Phyrexian Metamorph")) cloned[0].addType("Artifact");
                        if (cardName.equals("Phantasmal Image")) cloned[0].addType("Illusion");
                        cloned[0].setCloneOrigin(card);
                        cloned[0].addLeavesPlayCommand(leaves);
                        cloned[0].setCloneLeavesPlayCommand(leaves);
                        cloned[0].setCurSetCode(copyTarget[0].getCurSetCode());
                        cloned[0].setImageFilename(copyTarget[0].getImageFilename());
                        if (cardName.equals("Vesuvan Doppelganger")) {
                            cloned[0].addExtrinsicKeyword("At the beginning of your upkeep, you may have this creature become a copy of target creature except it doesn't copy that creature's color. If you do, this creature gains this ability.");
                            cloned[0].addColor("U", cloned[0], false, true);
                        } else if (cardName.equals("Quicksilver Gargantuan")) {
                            cloned[0].setBaseDefense(7);
                            cloned[0].setBaseAttack(7);
                        } else if (cardName.equals("Phantasmal Image")) {
                            StringBuilder trigScript = new StringBuilder("Mode$ BecomesTarget | ValidTarget$ Card.Self | TriggerZones$ Battlefield | Execute$ ");
                            StringBuilder svarName = new StringBuilder("TrigSac");
                            //Couple of hoops to jump through to make sure no svar is overwritten.
                            int iter = 0;
                            while(cloned[0].getSVar(svarName.toString()) != "")
                            {
                                iter++;
                                if(svarName.length() == 7)
                                {
                                    svarName.append(iter);
                                }
                                else
                                {
                                    svarName.delete(8, svarName.length());
                                    svarName.append(iter);
                                }
                            }
                            trigScript.append(svarName.toString());
                            trigScript.append(" | TriggerDescription$ When this creature becomes the target of a spell or ability, sacrifice it.");
                            cloned[0].addTrigger(forge.card.trigger.TriggerHandler.parseTrigger(trigScript.toString(),card,true));
                            cloned[0].setSVar(svarName.toString(), "AB$Sacrifice | Cost$ 0 | Defined$ Self");
                        }
                        

                        //Slight hack in case the cloner copies a card with triggers
                        for (Trigger t : cloned[0].getTriggers()) {
                            AllZone.getTriggerHandler().registerTrigger(t);
                        }

                        AllZone.getGameAction().moveToPlayFromHand(cloned[0]);
                        card.setCurrentlyCloningCard(cloned[0]);
                    }
                }
            };//SpellAbility

            Input runtime = new Input() {
                private static final long serialVersionUID = 7615038074569687330L;

                @Override
                public void showMessage() {
                    String message = "Select a creature ";
                    if (cardName.equals("Phyrexian Metamorph")) message += "or artifact ";
                    message += "on the battlefield";
                    AllZone.getDisplay().showMessage(cardName + " - " + message);
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(Card c, PlayerZone z) {
                    if (z.is(Constant.Zone.Battlefield) &&
                            (c.isCreature() || (cardName.equals("Phyrexian Metamorph") && c.isArtifact()))) {
                        if (cardName.equals("Jwari Shapeshifter") && !c.isType("Ally")) {
                            return;
                        }
                        copyTarget[0] = c;
                        stopSetNext(new Input_PayManaCost(copy));
                    }
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(copy);
            copy.setStackDescription(cardName + " - enters the battlefield as a copy of selected card.");
            copy.setBeforePayMana(runtime);
        }//*************** END ************ END **************************


        //*************** START ************ START **************************
        else if (cardName.equals("Nebuchadnezzar")) {
            /*
                * X, T: Name a card. Target opponent reveals X cards at random from his or her hand.
                * Then that player discards all cards with that name revealed this way.
                * Activate this ability only during your turn.
                */
            Cost abCost = new Cost("X T", cardName, true);
            Target target = new Target(card, "Select target opponent", "Opponent".split(","));
            Ability_Activated discard = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = 4839778470534392198L;

                @Override
                public void resolve() {
                    //name a card
                    String choice = JOptionPane.showInputDialog(null, "Name a card", cardName, JOptionPane.QUESTION_MESSAGE);
                    CardList hand = getTargetPlayer().getCardsIn(Zone.Hand);
                    int numCards = card.getXManaCostPaid();
                    numCards = Math.min(hand.size(), numCards);

                    CardList revealed = new CardList();
                    for (int i = 0; i < numCards; i++) {
                        Card random = CardUtil.getRandom(hand.toArray());
                        revealed.add(random);
                        hand.remove(random);
                    }
                    if (!revealed.isEmpty()) {
                        GuiUtils.getChoice("Revealed at random", revealed.toArray());
                    } else {
                        GuiUtils.getChoice("Revealed at random", new String[]{"Nothing to reveal"});
                    }

                    for (Card c : revealed) {
                        if (c.getName().equals(choice)) c.getController().discard(c, this);
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
        else if (cardName.equals("Brass Squire")) {

            Target t2 = new Target(card, "Select target creature you control", "Creature.YouCtrl".split(","));
            final Ability_Sub sub = new Ability_Sub(card, t2) {
                private static final long serialVersionUID = -8926850792424930054L;

                @Override
                public boolean chkAI_Drawback() {
                    return false;
                }

                @Override
                public void resolve() {
                    Card equipment = this.getParent().getTargetCard();
                    Card creature = getTargetCard();
                    if (AllZoneUtil.isCardInPlay(equipment) && AllZoneUtil.isCardInPlay(creature)) {
                        if (CardFactoryUtil.canTarget(card, equipment) && CardFactoryUtil.canTarget(card, creature)) {
                            if (equipment.isEquipping()) {
                                Card equipped = equipment.getEquipping().get(0);
                                if (!equipped.equals(creature)) {
                                    equipment.unEquipCard(equipped);
                                    equipment.equipCard(creature);
                                }
                            } else {
                                equipment.equipCard(getTargetCard());
                            }
                        }
                    }
                }

                @Override
                public boolean doTrigger(boolean b) {
                    return false;
                }
            };

            Cost abCost = new Cost("T", cardName, true);
            Target t1 = new Target(card, "Select target equipment you control", "Equipment.YouCtrl".split(","));
            final Ability_Activated ability = new Ability_Activated(card, abCost, t1) {
                private static final long serialVersionUID = 3818559481920103914L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    sub.resolve();
                }
            };
            ability.setSubAbility(sub);
            ability.setStackDescription(cardName + " - Attach target Equipment you control to target creature you control.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Gore Vassal")) {
            Cost abCost = new Cost("Sac<1/CARDNAME>", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, new Target(card, "TgtC")) {
                private static final long serialVersionUID = 3689290210743241201L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Card target = getTargetCard();

                    if (AllZoneUtil.isCardInPlay(target) && CardFactoryUtil.canTarget(card, target)) {
                        target.addCounter(Counters.M1M1, 1);
                        if (target.getNetDefense() >= 1) {
                            target.addShield();
                            AllZone.getEndOfTurn().addUntil(new Command() {
                                private static final long serialVersionUID = -3332692040606224591L;

                                public void execute() {
                                    target.resetShield();
                                }
                            });
                        }
                    }
                }//resolve()
            };//SpellAbility

            card.addSpellAbility(ability);
            ability.setDescription(abCost + "Put a -1/-1 counter on target creature. Then if that creature's toughness is 1 or greater, regenerate it.");

            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" put a -1/-1 counter on target creature.");
            ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if (cardName.equals("Awakener Druid")) {
            final long[] timeStamp = {0};

            Trigger myTrig = TriggerHandler.parseTrigger("Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | TriggerDescription$ When CARDNAME enters the battlefield, target Forest becomes a 4/5 green Treefolk creature for as long as CARDNAME is on the battlefield. It's still a land.", card, true);
            Target myTarget = new Target(card, "Choose target forest.", "Land.Forest".split(","), "1", "1");
            final SpellAbility awaken = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if (!AllZone.getZoneOf(card).is(Zone.Battlefield) || getTarget().getTargetCards().size() == 0)
                        return;
                    final Card c = getTarget().getTargetCards().get(0);
                    String[] types = {"Creature", "Treefolk"};
                    String[] keywords = {};
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 4, 5, types, keywords, "G");

                    final Command onleave = new Command() {
                        private static final long serialVersionUID = -6004932214386L;
                        long stamp = timeStamp[0];
                        Card tgt = c;

                        public void execute() {
                            String[] types = {"Creature", "Treefolk"};
                            String[] keywords = {""};
                            CardFactoryUtil.revertManland(tgt, types, keywords, "G", stamp);
                        }
                    };
                    card.addLeavesPlayCommand(onleave);
                }
            };//SpellAbility
            awaken.setTarget(myTarget);

            myTrig.setOverridingAbility(awaken);
            card.addTrigger(myTrig);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if (cardName.equals("Duct Crawler") || cardName.equals("Shrewd Hatchling") || cardName.equals("Spin Engine")) {
            String theCost = "0";
            if (cardName.equals("Duct Crawler"))
                theCost = "1 R";
            else if (cardName.equals("Shrewd Hatchling"))
                theCost = "UR";
            else if (cardName.equals("Spin Engine"))
                theCost = "R";

            StringBuilder keywordBuilder = new StringBuilder("HIDDEN CARDNAME can't block ");
            keywordBuilder.append(card.getName()).append(" (").append(card.getUniqueNumber()).append(")");

            AbilityFactory createAb = new AbilityFactory();
            StringBuilder abilityBuilder = new StringBuilder("AB$Pump | Cost$ ");
            abilityBuilder.append(theCost);
            abilityBuilder.append(" | Tgt$ TgtC | IsCurse$ True | KW$ ");
            abilityBuilder.append(keywordBuilder.toString());
            abilityBuilder.append(" | SpellDescription$ Target creature can't block CARDNAME this turn.");
            SpellAbility myAb = createAb.getAbility(abilityBuilder.toString(), card);

            card.addSpellAbility(myAb);


        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Krovikan Sorcerer")) {
            Cost abCost = new Cost("T Discard<1/Card.Black>", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = 3689290210743241201L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                	final Player player = card.getController();
                	if (player.isHuman()) {
                		final CardList n = player.drawCards(2);
                		
                		AllZone.getInputControl().setInput(new Input() {
							private static final long serialVersionUID = -1411038851955251074L;

							@Override
                			public void showMessage() {
                				if(n.isEmpty()) stop();
                				AllZone.getDisplay().showMessage(card+" - discard one of the cards drawn.");
                				ButtonUtil.disableAll();
                			}

                			@Override
                			public void selectCard(Card c, PlayerZone zone) {
                				if (zone.is(Constant.Zone.Hand) && n.contains(c)) {
                					player.discard(c, null);
                					stop();
                				}
                			}
                		});//end Input
                	}
                }//resolve()
            };//SpellAbility

            card.addSpellAbility(ability);
            ability.setDescription("Tap, Discard a black card: " + "Draw two cards, then discard one of them.");

            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - Draw two cards, then discard one of them.");
            ability.setStackDescription(sb.toString());
        }//*************** END ************ END **************************
        
        
        //***************************************************
        // end of card specific code
        //***************************************************

        if (hasKeyword(card, "Level up") != -1 && hasKeyword(card, "maxLevel") != -1) {
            int n = hasKeyword(card, "Level up");
            int m = hasKeyword(card, "maxLevel");
            if (n != -1) {
                String parse = card.getKeyword().get(n).toString();
                String parseMax = card.getKeyword().get(m).toString();

                card.removeIntrinsicKeyword(parse);
                card.removeIntrinsicKeyword(parseMax);


                String k[] = parse.split(":");
                final String manacost = k[1];

                String l[] = parseMax.split(":");
                final int maxLevel = Integer.parseInt(l[1]);

                final SpellAbility levelUp = new Ability_Activated(card, manacost) {
                    private static final long serialVersionUID = 3998280279949548652L;

                    public void resolve() {
                        card.addCounter(Counters.LEVEL, 1);
                    }

                    public boolean canPlayAI() {
                        // Todo: Improve Level up code
                        return card.getCounters(Counters.LEVEL) < maxLevel;
                    }

                };
                levelUp.getRestrictions().setSorcerySpeed(true);
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