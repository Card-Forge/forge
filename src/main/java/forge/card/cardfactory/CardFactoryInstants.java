/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.cardfactory;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.PhaseUtil;
import forge.Player;
import forge.PlayerZone;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.util.MyRandom;

/**
 * <p>
 * CardFactoryInstants class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryInstants {

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

        // *************** START *********** START **************************
        /*if (cardName.equals("Fact or Fiction")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1481112451519L;

                @Override
                public void resolve() {

                    Card choice = null;

                    // check for no cards in hand on resolve
                    final PlayerZone library = card.getController().getZone(Constant.Zone.Library);
                    final PlayerZone hand = card.getController().getZone(Constant.Zone.Hand);
                    // PlayerZone Grave =
                    // card.getController().getZone(Constant.Zone.Graveyard);
                    final CardList cards = new CardList();

                    if (library.size() == 0) {
                        JOptionPane.showMessageDialog(null, "No more cards in library.", "",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    int count = 5;
                    if (library.size() < 5) {
                        count = library.size();
                    }
                    for (int i = 0; i < count; i++) {
                        cards.add(library.get(i));
                    }
                    final CardList pile1 = new CardList();
                    final CardList pile2 = new CardList();
                    boolean stop = false;
                    int pile1CMC = 0;
                    int pile2CMC = 0;

                    final StringBuilder sbMsg = new StringBuilder();
                    sbMsg.append("Revealing top ").append(count).append(" cards of library: ");
                    GuiUtils.getChoice(sbMsg.toString(), cards.toArray());
                    // Human chooses
                    if (card.getController().isComputer()) {
                        for (int i = 0; i < count; i++) {
                            if (!stop) {
                                choice = GuiUtils.getChoiceOptional("Choose cards to put into the first pile: ",
                                        cards.toArray());
                                if (choice != null) {
                                    pile1.add(choice);
                                    cards.remove(choice);
                                    pile1CMC = pile1CMC + CardUtil.getConvertedManaCost(choice);
                                } else {
                                    stop = true;
                                }
                            }
                        }
                        for (int i = 0; i < count; i++) {
                            if (!pile1.contains(library.get(i))) {
                                pile2.add(library.get(i));
                                pile2CMC = pile2CMC + CardUtil.getConvertedManaCost(library.get(i));
                            }
                        }
                        final StringBuilder sb = new StringBuilder();
                        sb.append("You have spilt the cards into the following piles");
                        sb.append("\r\n").append("\r\n");
                        sb.append("Pile 1: ").append("\r\n");
                        for (int i = 0; i < pile1.size(); i++) {
                            sb.append(pile1.get(i).getName()).append("\r\n");
                        }
                        sb.append("\r\n").append("Pile 2: ").append("\r\n");
                        for (int i = 0; i < pile2.size(); i++) {
                            sb.append(pile2.get(i).getName()).append("\r\n");
                        }
                        JOptionPane.showMessageDialog(null, sb, "", JOptionPane.INFORMATION_MESSAGE);
                        if (pile1CMC >= pile2CMC) {
                            final StringBuilder sbMsgP1 = new StringBuilder();
                            sbMsgP1.append("Computer adds the first pile to its hand ");
                            sbMsgP1.append("and puts the second pile into the graveyard");
                            JOptionPane
                                    .showMessageDialog(null, sbMsgP1.toString(), "", JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < pile1.size(); i++) {
                                Singletons.getModel().getGameAction().moveTo(hand, pile1.get(i));
                            }
                            for (int i = 0; i < pile2.size(); i++) {
                                Singletons.getModel().getGameAction().moveToGraveyard(pile2.get(i));
                            }
                        } else {
                            final StringBuilder sbMsgP2 = new StringBuilder();
                            sbMsgP2.append("Computer adds the second pile to its hand and ");
                            sbMsgP2.append("puts the first pile into the graveyard");
                            JOptionPane
                                    .showMessageDialog(null, sbMsgP2.toString(), "", JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < pile2.size(); i++) {
                                Singletons.getModel().getGameAction().moveTo(hand, pile2.get(i));
                            }
                            for (int i = 0; i < pile1.size(); i++) {
                                Singletons.getModel().getGameAction().moveToGraveyard(pile1.get(i));
                            }
                        }

                    } else {
                        // Computer chooses (It picks the highest converted
                        // mana cost card and 1 random card.)
                        Card biggest = null;
                        biggest = library.get(0);

                        for (int i = 0; i < count; i++) {
                            if (CardUtil.getConvertedManaCost(biggest.getManaCost()) >= CardUtil
                                    .getConvertedManaCost(biggest.getManaCost())) {
                                biggest = cards.get(i);
                            }
                        }
                        pile1.add(biggest);
                        cards.remove(biggest);
                        if (cards.size() > 0) {
                            final Card random = CardUtil.getRandom(cards.toArray());
                            pile1.add(random);
                        }
                        for (int i = 0; i < count; i++) {
                            if (!pile1.contains(library.get(i))) {
                                pile2.add(library.get(i));
                            }
                        }
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Choose a pile to add to your hand: ");
                        sb.append("\r\n").append("\r\n");
                        sb.append("Pile 1: ").append("\r\n");
                        for (int i = 0; i < pile1.size(); i++) {
                            sb.append(pile1.get(i).getName()).append("\r\n");
                        }
                        sb.append("\r\n").append("Pile 2: ").append("\r\n");
                        for (int i = 0; i < pile2.size(); i++) {
                            sb.append(pile2.get(i).getName()).append("\r\n");
                        }
                        final Object[] possibleValues = { "Pile 1", "Pile 2" };
                        final Object q = JOptionPane.showOptionDialog(null, sb, "Fact or Fiction",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                                possibleValues[0]);
                        if (q.equals(0)) {
                            for (int i = 0; i < pile1.size(); i++) {
                                Singletons.getModel().getGameAction().moveTo(hand, pile1.get(i));
                            }
                            for (int i = 0; i < pile2.size(); i++) {
                                Singletons.getModel().getGameAction().moveToGraveyard(pile2.get(i));
                            }
                        } else {
                            for (int i = 0; i < pile2.size(); i++) {
                                Singletons.getModel().getGameAction().moveTo(hand, pile2.get(i));
                            }
                            for (int i = 0; i < pile1.size(); i++) {
                                Singletons.getModel().getGameAction().moveToGraveyard(pile1.get(i));
                            }
                        }
                    }
                    pile1.clear();
                    pile2.clear();
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    final CardList cards = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    return cards.size() >= 10;
                }
            }; // SpellAbility

            card.addSpellAbility(spell);
        }*/ // *************** END ************ END **************************

        //*************** START *********** START **************************
        if (cardName.equals("Hurkyl's Recall")) {
            /*
             * Return all artifacts target player owns to his or her hand.
             */
            Target t = new Target(card, "Select target player", "Player");
            Cost cost = new Cost("1 U", cardName, false);

            SpellAbility spell = new Spell(card, cost, t) {
                private static final long serialVersionUID = -4098702062413878046L;

                @Override
                public boolean canPlayAI() {
                    CardList humanArts = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    humanArts = humanArts.getType("Artifact");
                    return humanArts.size() > 0;
                } //canPlayAI

                @Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                } //chooseTargetAI()

                @Override
                public void resolve() {
                    Player player = getTargetPlayer();
                    CardList artifacts = AllZoneUtil.getCardsIn(Zone.Battlefield);
                    artifacts = artifacts.getType("Artifact");

                    for (int i = 0; i < artifacts.size(); i++) {
                        Card thisArtifact = artifacts.get(i);
                        if (thisArtifact.getOwner().equals(player)) {
                            //moveToHand handles tokens
                            Singletons.getModel().getGameAction().moveToHand(thisArtifact);
                        }
                    }
                } //resolve()
            };

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        // *************** START *********** START **************************
        else if (cardName.equals("Intuition")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 8282597086298330698L;

                @Override
                public void resolve() {
                    final Player player = card.getController();
                    if (player.isHuman()) {
                        this.humanResolve();
                    } else {
                        this.computerResolve();
                    }
                    player.shuffle();
                }

                public void humanResolve() {
                    final CardList libraryList = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                    final CardList selectedCards = new CardList();

                    Object o = GuiUtils.chooseOneOrNone("Select first card", libraryList.toArray());
                    if (o != null) {
                        final Card c1 = (Card) o;
                        libraryList.remove(c1);
                        selectedCards.add(c1);
                    } else {
                        return;
                    }
                    o = GuiUtils.chooseOneOrNone("Select second card", libraryList.toArray());
                    if (o != null) {
                        final Card c2 = (Card) o;
                        libraryList.remove(c2);
                        selectedCards.add(c2);
                    } else {
                        return;
                    }
                    o = GuiUtils.chooseOneOrNone("Select third card", libraryList.toArray());
                    if (o != null) {
                        final Card c3 = (Card) o;
                        libraryList.remove(c3);
                        selectedCards.add(c3);
                    } else {
                        return;
                    }

                    // comp randomly selects one of the three cards
                    final Card choice = selectedCards.get(MyRandom.getRandom().nextInt(2));

                    selectedCards.remove(choice);
                    Singletons.getModel().getGameAction().moveToHand(choice);

                    for (final Card trash : selectedCards) {
                        Singletons.getModel().getGameAction().moveToGraveyard(trash);
                    }
                }

                public void computerResolve() {
                    final CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Library);
                    final CardList selectedCards = new CardList();

                    // pick best creature
                    Card c = CardFactoryUtil.getBestCreatureAI(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    c = CardFactoryUtil.getBestCreatureAI(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    c = CardFactoryUtil.getBestCreatureAI(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    // NOTE: Using getChoiceOptional() results in a null error
                    // when you click on Cancel.
                    final Object o = GuiUtils.chooseOne("Select card to give to computer", selectedCards.toArray());

                    final Card choice = (Card) o;

                    selectedCards.remove(choice);
                    Singletons.getModel().getGameAction().moveToHand(choice);

                    for (final Card trash : selectedCards) {
                        Singletons.getModel().getGameAction().moveToGraveyard(trash);
                    }
                }

                @Override
                public boolean canPlay() {
                    final CardList library = card.getController().getCardsIn(Zone.Library);
                    return library.size() >= 3 && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    CardList creature = AllZone.getComputerPlayer().getCardsIn(Zone.Library);
                    creature = creature.getType("Creature");
                    return creature.size() >= 3;
                }
            }; // SpellAbility

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Suffer the Past")) {
            final Cost cost = new Cost("X B", cardName, false);
            final Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 1168802375190293222L;

                @Override
                public void resolve() {
                    final Player tPlayer = this.getTargetPlayer();
                    final Player player = card.getController();
                    final int max = card.getXManaCostPaid();

                    final CardList graveList = tPlayer.getCardsIn(Zone.Graveyard);
                    final int x = Math.min(max, graveList.size());

                    if (player.isHuman()) {
                        for (int i = 0; i < x; i++) {
                            final Object o = GuiUtils.chooseOne("Remove from game", graveList.toArray());
                            if (o == null) {
                                break;
                            }
                            final Card c1 = (Card) o;
                            graveList.remove(c1); // remove from the display
                                                  // list
                            Singletons.getModel().getGameAction().exile(c1);
                        }
                    } else { // Computer
                        // Random random = MyRandom.random;
                        for (int j = 0; j < x; j++) {
                            // int index = random.nextInt(X-j);
                            Singletons.getModel().getGameAction().exile(graveList.get(j));
                        }
                    }

                    tPlayer.loseLife(x, card);
                    player.gainLife(x, card);
                    card.setXManaCostPaid(0);
                }

                @Override
                public void chooseTargetAI() {
                    this.setTargetPlayer(AllZone.getHumanPlayer());
                } // chooseTargetAI()

                @Override
                public boolean canPlayAI() {
                    final CardList graveList = AllZone.getHumanPlayer().getCardsIn(Zone.Graveyard);

                    final int maxX = ComputerUtil.getAvailableMana().size() - 1;
                    return (maxX >= 3) && (graveList.size() > 0);
                }
            };

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************


        // *************** START *********** START **************************
        else if (cardName.equals("Siren's Call")) {
            /*
             * Creatures the active player controls attack this turn if able.
             * 
             * At the beginning of the next end step, destroy all non-Wall
             * creatures that player controls that didn't attack this turn.
             * Ignore this effect for each creature the player didn't control
             * continuously since the beginning of the turn.
             */
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5746330758531799264L;

                @Override
                public boolean canPlay() {
                    return PhaseUtil.isBeforeAttackersAreDeclared()
                            && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(card.getController().getOpponent());
                } // canPlay

                @Override
                public boolean canPlayAI() {
                    return false;
                } // canPlayAI

                @Override
                public void resolve() {
                    // this needs to get a list of opponents creatures and set
                    // the siren flag
                    final Player player = card.getController();
                    final Player opponent = player.getOpponent();
                    final CardList creatures = AllZoneUtil.getCreaturesInPlay(opponent);
                    for (final Card creature : creatures) {
                        // skip walls, skip creatures with summoning sickness
                        // also skip creatures with haste if they came onto the
                        // battlefield this turn
                        if ((!creature.isWall() && !creature.hasSickness())
                                || (creature.hasKeyword("Haste") && (creature.getTurnInZone() != 1))) {
                            creature.setSirenAttackOrDestroy(true);
                            // System.out.println("Siren's Call - setting flag for "+creature.getName());
                        }
                    }
                    final SpellAbility destroy = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            final Player player = card.getController();
                            final Player opponent = player.getOpponent();
                            final CardList creatures = AllZoneUtil.getCreaturesInPlay(opponent);

                            for (final Card creature : creatures) {
                                // System.out.println("Siren's Call - EOT - "+creature.getName()
                                // +" flag: "+creature.getSirenAttackOrDestroy());
                                // System.out.println("Siren's Call - EOT - "+creature.getName()
                                // +" attacked?: "+creature.getCreatureAttackedThisCombat());
                                if (creature.getSirenAttackOrDestroy() && !creature.getCreatureAttackedThisTurn()) {
                                    if (AllZoneUtil.isCardInPlay(creature)) {
                                        // System.out.println("Siren's Call - destroying "+creature.getName());
                                        // this should probably go on the stack
                                        Singletons.getModel().getGameAction().destroy(creature);
                                    }
                                }
                                creature.setSirenAttackOrDestroy(false);
                            }
                        }
                    };
                    final Command atEOT = new Command() {
                        private static final long serialVersionUID = 5369528776959445848L;

                        @Override
                        public void execute() {
                            final StringBuilder sb = new StringBuilder();
                            sb.append(card).append(" - At the beginning of the next end step, ");
                            sb.append("destroy all non-Wall creatures that player controls that didn't ");
                            sb.append("attack this turn. Ignore this effect for each creature the player ");
                            sb.append("didn't control continuously since the beginning of the turn.");
                            destroy.setDescription(sb.toString());
                            destroy.setStackDescription(sb.toString());

                            AllZone.getStack().addSimultaneousStackEntry(destroy);
                        } // execute
                    }; // Command
                    AllZone.getEndOfTurn().addAt(atEOT);
                } // resolve
            }; // SpellAbility

            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - All creatures that can attack must do so or be destroyed.");
            spell.setStackDescription(sb.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Telling Time")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 2626878556107707854L;
                private final String[] prompt = new String[] { "Put a card into your hand",
                        "Put a card on top of library", "Put a card on bottom of library" };

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final PlayerZone lib = card.getController().getZone(Constant.Zone.Library);
                    final CardList choices = new CardList();
                    for (int i = 0; (i < 3) && (lib.size() > 0); i++) {
                        choices.add(lib.get(i));
                    }

                    for (int i = 0; (i < 3) && !choices.isEmpty(); i++) {
                        final Object o = GuiUtils.chooseOne(this.prompt[i], choices.toArray());
                        final Card c1 = (Card) o;
                        if (i == 0) {
                            Singletons.getModel().getGameAction().moveToHand(c1);
                        } else if (i == 1) {
                            Singletons.getModel().getGameAction().moveToLibrary(c1);
                        } else if (i == 2) {
                            Singletons.getModel().getGameAction().moveToBottomOfLibrary(c1);
                        }

                        choices.remove(c1);
                    }
                }
            };

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Remove Enchantments")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -7324132132222075031L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Player you = card.getController();
                    final CardList ens = AllZoneUtil.getCardsIn(Zone.Battlefield).filter(CardListFilter.ENCHANTMENTS);
                    final CardList toReturn = ens.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            final Card enchanting = c.getEnchantingCard();

                            if (enchanting != null) {
                                if ((enchanting.isAttacking() && enchanting.getController().isPlayer(you.getOpponent()))
                                        || enchanting.getController().isPlayer(you)) {
                                    return true;
                                }
                            }

                            return (c.getOwner().isPlayer(you) && c.getController().isPlayer(you));
                        }
                    });
                    for (final Card c : toReturn) {
                        Singletons.getModel().getGameAction().moveToHand(c);
                    }

                    for (final Card c : ens) {
                        if (!toReturn.contains(c)) {
                            Singletons.getModel().getGameAction().destroy(c);
                        }
                    }
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - destroy/return enchantments.");
            spell.setStackDescription(sb.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************


        // *************** START *********** START **************************
        else if (cardName.equals("Turnabout")) {
            /*
             * Choose artifact, creature, or land. Tap all untapped permanents
             * of the chosen type target player controls, or untap all tapped
             * permanents of that type that player controls.
             */
            final Cost abCost = new Cost("2 U U", cardName, false);
            final Target target = new Target(card, "Select target player", "Player".split(","));
            final SpellAbility spell = new Spell(card, abCost, target) {
                private static final long serialVersionUID = -2175586347805121896L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final String[] choices = new String[] { "Artifact", "Creature", "Land" };
                    final Object o = GuiUtils.chooseOne("Select permanent type", choices);
                    final String cardType = (String) o;
                    final CardList list = this.getTargetPlayer().getCardsIn(Zone.Battlefield).getType(cardType);

                    final String[] tapOrUntap = new String[] { "Tap", "Untap" };
                    final Object z = GuiUtils.chooseOne("Tap or Untap?", tapOrUntap);
                    final boolean tap = (z.equals("Tap")) ? true : false;

                    for (final Card c : list) {
                        if (tap) {
                            c.tap();
                        } else {
                            c.untap();
                        }
                    }
                } // resolve()
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Wing Puncture")) {

            final Target t2 = new Target(card, "Select target creature with flying", "Creature.withFlying".split(","));
            final AbilitySub sub = new AbilitySub(card, t2) {
                private static final long serialVersionUID = 4618047889975691050L;

                @Override
                public boolean chkAIDrawback() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Card myc = this.getParent().getTargetCard();
                    final Card tgt = this.getTargetCard();
                    if (AllZoneUtil.isCardInPlay(myc) && AllZoneUtil.isCardInPlay(tgt)) {
                        if (myc.canBeTargetedBy(this) && tgt.canBeTargetedBy(this)) {
                            tgt.addDamage(myc.getNetAttack(), myc);
                        }
                    }
                }

                @Override
                public boolean doTrigger(final boolean b) {
                    return false;
                }
            };

            final Cost abCost = new Cost("G", cardName, false);
            final Target t1 = new Target(card, "Select target creature you control", "Creature.YouCtrl".split(","));
            final SpellAbility spell = new Spell(card, abCost, t1) {
                private static final long serialVersionUID = 8964235807056739219L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    sub.resolve();
                }
            };
            spell.setSubAbility(sub);

            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Target creature you control deals damage ");
            sbDesc.append("equal to its power to target creature with flying.");
            spell.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append(" - Creature you control deals damage ");
            sbStack.append("equal to its power to creature with flying.");
            spell.setStackDescription(sbStack.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        return card;
    } // getCard

} // end class CardFactory_Instants
