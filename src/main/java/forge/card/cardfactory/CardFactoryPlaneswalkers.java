package forge.card.cardfactory;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Counters;
import forge.GameActionUtil;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;

/**
 * <p>
 * CardFactory_Planeswalkers class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryPlaneswalkers {

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName) {
        // All Planeswalkers set their loyality in the beginning
        if (card.getBaseLoyalty() > 0) {
            card.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card, Counters.LOYALTY,
                    card.getBaseLoyalty()));
        }
        // *************** START *********** START **************************
        if (cardName.equals("Jace, the Mind Sculptor")) {
            final int[] turn = new int[1];
            turn[0] = -1;

            final Target t1 = new Target(card, "Select target player", "Player");
            final Cost cost1 = new Cost("AddCounter<2/LOYALTY>", cardName, true);

            final SpellAbility ability1 = new AbilityActivated(card, cost1, t1) {
                private static final long serialVersionUID = -986543400626807336L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    // card.addCounterFromNonEffect(Counters.LOYALTY, 2);
                    final Player targetPlayer = this.getTargetPlayer();

                    final PlayerZone lib = targetPlayer.getZone(Constant.Zone.Library);

                    if (lib.size() == 0) {
                        return;
                    }

                    final Card c = lib.get(0);

                    if (card.getController().isHuman()) {
                        final StringBuilder question = new StringBuilder();
                        question.append("Put the card ").append(c).append(" on the bottom of the ");
                        question.append(c.getController()).append("'s library?");

                        if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                            AllZone.getGameAction().moveToBottomOfLibrary(c);
                        }

                    } else {
                        final CardList land = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());

                        // TODO improve this:
                        if ((land.size() > 4) && c.isLand()) {
                        } else {
                            AllZone.getGameAction().moveToBottomOfLibrary(c);
                        }
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return (card.getCounters(Counters.LOYALTY) < 12)
                            && (AllZone.getHumanPlayer().getZone(Zone.Library).size() > 2);
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && (turn[0] != AllZone.getPhase().getTurn()) && Phase.canCastSorcery(card.getController());
                } // canPlay()
            };
            ability1.setDescription("+2: Look at the top card of target player's library. "
                    + "You may put that card on the bottom of that player's library.");
            final StringBuilder stack1 = new StringBuilder();
            stack1.append(card.getName()).append(
                    " - Look at the top card of target player's library. "
                            + "You may put that card on the bottom of that player's library.");
            ability1.setStackDescription(stack1.toString());

            ability1.setChooseTargetAI(CardFactoryUtil.targetHumanAI());
            card.addSpellAbility(ability1);

            final Ability ability2 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    card.getController().drawCards(3);

                    final Player player = card.getController();
                    if (player.isHuman()) {
                        this.humanResolve();
                        // else
                        // computerResolve();
                    }
                }

                public void humanResolve() {
                    CardList putOnTop = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);

                    if (putOnTop.size() > 0) {
                        final Object o = GuiUtils.getChoice("First card to put on top: ", putOnTop.toArray());
                        if (o != null) {
                            final Card c1 = (Card) o;
                            putOnTop.remove(c1);
                            AllZone.getGameAction().moveToLibrary(c1);
                        }
                    }

                    putOnTop = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);

                    if (putOnTop.size() > 0) {
                        final Object o = GuiUtils.getChoice("Second card to put on top: ", putOnTop.toArray());
                        if (o != null) {
                            final Card c2 = (Card) o;
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
                            && (turn[0] != AllZone.getPhase().getTurn()) && Phase.canCastSorcery(card.getController());
                } // canPlay()
            };
            ability2.setDescription("0: Draw three cards, then put two cards from your "
                    + "hand on top of your library in any order.");
            final StringBuilder stack2 = new StringBuilder();
            stack2.append(card.getName()).append(
                    " - Draw three cards, then put two cards from your hand on top of your library in any order.");
            ability2.setStackDescription(stack2.toString());
            card.addSpellAbility(ability2);

            final Cost cost = new Cost("SubCounter<1/LOYALTY>", cardName, true);
            final Target target = new Target(card, "TgtC");

            final SpellAbility ability3 = new AbilityActivated(card, cost, target) {
                private static final long serialVersionUID = -1113077473448818423L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    // card.subtractCounter(Counters.LOYALTY, 1);

                    if (AllZoneUtil.isCardInPlay(this.getTargetCard())
                            && CardFactoryUtil.canTarget(card, this.getTargetCard())) {
                        AllZone.getGameAction().moveToHand(this.getTargetCard());
                    } // if
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {
                    return (card.getCounters(Counters.LOYALTY) >= 1)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && (turn[0] != AllZone.getPhase().getTurn()) && Phase.canCastSorcery(card.getController());
                }
            };
            ability3.setDescription("-1: Return target creature to its owner's hand.");
            final StringBuilder stack3 = new StringBuilder();
            stack3.append(card.getName()).append(" - Return target creature to its owner's hand.");
            ability3.setStackDescription(stack3.toString());
            card.addSpellAbility(ability3);

            final Target target4 = new Target(card, "Select target player", "Player");
            final Cost cost4 = new Cost("SubCounter<12/LOYALTY>", cardName, true);
            final SpellAbility ability4 = new AbilityActivated(card, cost4, target4) {
                private static final long serialVersionUID = 5512803971603404142L;

                @Override
                public void resolve() {
                    turn[0] = AllZone.getPhase().getTurn();
                    // card.subtractCounter(Counters.LOYALTY, 12);

                    final Player player = this.getTargetPlayer();

                    final CardList libList = player.getCardsIn(Zone.Library);
                    final CardList handList = player.getCardsIn(Zone.Hand);

                    for (final Card c : libList) {
                        AllZone.getGameAction().exile(c);
                    }

                    for (final Card c : handList) {
                        AllZone.getGameAction().moveToLibrary(c);
                    }
                    player.shuffle();
                }

                @Override
                public boolean canPlayAI() {
                    final int libSize = AllZone.getHumanPlayer().getZone(Zone.Library).size();
                    final int handSize = AllZone.getHumanPlayer().getZone(Zone.Hand).size();
                    return (libSize > 0) && (libSize >= handSize);
                }

                @Override
                public boolean canPlay() {
                    return (card.getCounters(Counters.LOYALTY) >= 12)
                            && AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && (turn[0] != AllZone.getPhase().getTurn()) && Phase.canCastSorcery(card.getController());
                }
            };
            ability4.setDescription("-12: Exile all cards from target player's library, then that "
                    + "player shuffles his or her hand into his or her library.");
            final StringBuilder stack4 = new StringBuilder();
            stack4.append(card.getName()).append(
                    " - Exile all cards from target player's library, then that player "
                            + "shuffles his or her hand into his or her library.");
            ability4.setStackDescription(stack4.toString());
            ability4.setChooseTargetAI(CardFactoryUtil.targetHumanAI());
            card.addSpellAbility(ability4);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Sarkhan the Mad")) {

            // Planeswalker book-keeping
            final int[] turn = new int[1];
            turn[0] = -1;

            // ability1
            /*
             * 0: Reveal the top card of your library and put it into your hand.
             * Sarkhan the Mad deals damage to himself equal to that card's
             * converted mana cost.
             */
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounterFromNonEffect(Counters.LOYALTY, 0);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Player player = card.getController();
                    final PlayerZone lib = player.getZone(Constant.Zone.Library);

                    final Card topCard = lib.get(0);
                    final int convertedManaTopCard = CardUtil.getConvertedManaCost(topCard.getManaCost());
                    final CardList showTop = new CardList();
                    showTop.add(topCard);
                    GuiUtils.getChoiceOptional("Revealed top card: ", showTop.toArray());

                    // now, move it to player's hand
                    AllZone.getGameAction().moveToHand(topCard);

                    // now, do X damage to Sarkhan
                    card.addDamage(convertedManaTopCard, card);

                } // resolve()

                @Override
                public boolean canPlayAI() {
                    // the computer isn't really smart enough to play this
                    // effectively, and it doesn't really
                    // help unless there are no cards in his hand
                    return false;
                }

                @Override
                public boolean canPlay() {
                    // looks like standard Planeswalker stuff...
                    // maybe should check if library is empty, or 1 card?
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && (turn[0] != AllZone.getPhase().getTurn()) && Phase.canCastSorcery(card.getController());
                } // canPlay()
            };
            ability1.setDescription("0: Reveal the top card of your library and put it "
                    + "into your hand. Sarkhan the Mad deals damage to himself equal to that card's converted mana cost.");
            final StringBuilder stack1 = new StringBuilder();
            stack1.append(card.getName()).append(" - Reveal top card and do damage.");
            ability1.setStackDescription(stack1.toString());

            // ability2
            /*
             * -2: Target creature's controller sacrifices it, then that player
             * puts a 5/5 red Dragon creature token with flying onto the
             * battlefield.
             */
            final Target target2 = new Target(card, "TgtC");
            final Cost cost2 = new Cost("SubCounter<2/LOYALTY>", cardName, true);
            final SpellAbility ability2 = new AbilityActivated(card, cost2, target2) {
                private static final long serialVersionUID = 4322453486268967722L;

                @Override
                public void resolve() {
                    // card.subtractCounter(Counters.LOYALTY, 2);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Card target = this.getTargetCard();
                    AllZone.getGameAction().sacrifice(target);
                    // in makeToken, use target for source, so it goes into the
                    // correct Zone
                    CardFactoryUtil.makeToken("Dragon", "R 5 5 Dragon", target.getController(), "R", new String[] {
                            "Creature", "Dragon" }, 5, 5, new String[] { "Flying" });

                } // resolve()

                @Override
                public boolean canPlayAI() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    creatures = creatures.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return !(c.isToken() && c.isType("Dragon"));
                        }
                    });
                    return creatures.size() >= 1;
                }

                @Override
                public void chooseTargetAI() {
                    CardList cards = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    // avoid targeting the dragon tokens we just put in play...
                    cards = cards.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return !(c.isToken() && c.isType("Dragon"));
                        }
                    });
                    this.setTargetCard(CardFactoryUtil.getCheapestCreatureAI(cards, card, true));
                    Log.debug(
                            "Sarkhan the Mad",
                            "Sarkhan the Mad caused sacrifice of: "
                                    + CardFactoryUtil.getCheapestCreatureAI(cards, card, true));
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && (card.getCounters(Counters.LOYALTY) >= 2) && (turn[0] != AllZone.getPhase().getTurn())
                            && Phase.canCastSorcery(card.getController());
                } // canPlay()
            };
            ability2.setDescription("-2: Target creature's controller sacrifices it, "
                    + "then that player puts a 5/5 red Dragon creature token with flying onto the battlefield.");

            // ability3
            /*
             * -4: Each Dragon creature you control deals damage equal to its
             * power to target player.
             */
            final Target target3 = new Target(card, "Select target player", "Player");
            final Cost cost3 = new Cost("SubCounter<4/LOYALTY>", cardName, true);
            final SpellAbility ability3 = new AbilityActivated(card, cost3, target3) {
                private static final long serialVersionUID = -5488579738767048060L;

                @Override
                public void resolve() {
                    // card.subtractCounter(Counters.LOYALTY, 4);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Player target = this.getTargetPlayer();
                    final Player player = card.getController();
                    final CardList dragons = player.getCardsIn(Zone.Battlefield).getType("Dragon");
                    for (int i = 0; i < dragons.size(); i++) {
                        final Card dragon = dragons.get(i);
                        final int damage = dragon.getNetAttack();
                        target.addDamage(damage, dragon);
                    }

                } // resolve()

                @Override
                public boolean canPlayAI() {
                    this.setTargetPlayer(AllZone.getHumanPlayer());
                    final CardList dragons = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield).getType("Dragon");
                    return (card.getCounters(Counters.LOYALTY) >= 4) && (dragons.size() >= 1);
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                            && (card.getCounters(Counters.LOYALTY) >= 4) && (turn[0] != AllZone.getPhase().getTurn())
                            && Phase.canCastSorcery(card.getController());
                } // canPlay()
            };
            ability3.setDescription("-4: Each Dragon creature you control "
                    + "deals damage equal to its power to target player.");

            card.addSpellAbility(ability1);
            card.addSpellAbility(ability2);
            card.addSpellAbility(ability3);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        } // *************** END ************ END **************************

        return card;
    }
    
} //end class CardFactoryPlaneswalkers
