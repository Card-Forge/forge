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

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.Command;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseUtil;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
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
    public static void buildCard(final Card card, final String cardName) {

        //*************** START *********** START **************************
        if (cardName.equals("Hurkyl's Recall")) {
            /*
             * Return all artifacts target player owns to his or her hand.
             */
            Target t = new Target(card, "Select target player", "Player");
            Cost cost = new Cost(card, "1 U", false);

            SpellAbility spell = new Spell(card, cost, t) {
                private static final long serialVersionUID = -4098702062413878046L;

                @Override
                public boolean canPlayAI() {
                    List<Card> humanCards = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
                    return Iterables.any(humanCards, CardPredicates.Presets.ARTIFACTS);
                } //canPlayAI

                @Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                } //chooseTargetAI()

                @Override
                public void resolve() {
                    Player player = getTargetPlayer();
                    List<Card> artifacts = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
                    artifacts = CardLists.filter(artifacts, CardPredicates.Presets.ARTIFACTS);

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
                    final List<Card> libraryList = AllZone.getHumanPlayer().getCardsIn(ZoneType.Library);
                    final List<Card> selectedCards = new ArrayList<Card>();

                    Object o = GuiChoose.oneOrNone("Select first card", libraryList);
                    if (o != null) {
                        final Card c1 = (Card) o;
                        libraryList.remove(c1);
                        selectedCards.add(c1);
                    } else {
                        return;
                    }
                    o = GuiChoose.oneOrNone("Select second card", libraryList);
                    if (o != null) {
                        final Card c2 = (Card) o;
                        libraryList.remove(c2);
                        selectedCards.add(c2);
                    } else {
                        return;
                    }
                    o = GuiChoose.oneOrNone("Select third card", libraryList);
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
                    final List<Card> list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Library);
                    final List<Card> selectedCards = new ArrayList<Card>();

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
                    final Card choice = GuiChoose.one("Select card to give to computer", selectedCards);

                    selectedCards.remove(choice);
                    Singletons.getModel().getGameAction().moveToHand(choice);

                    for (final Card trash : selectedCards) {
                        Singletons.getModel().getGameAction().moveToGraveyard(trash);
                    }
                }

                @Override
                public boolean canPlay() {
                    final List<Card> library = card.getController().getCardsIn(ZoneType.Library);
                    return library.size() >= 3 && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    Iterable<Card> creature = Iterables.filter(AllZone.getComputerPlayer().getCardsIn(ZoneType.Library), CardPredicates.Presets.CREATURES); 
                    return Iterables.size(creature) >= 3;
                }
            }; // SpellAbility

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Suffer the Past")) {
            final Cost cost = new Cost(card, "X B", false);
            final Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 1168802375190293222L;

                @Override
                public void resolve() {
                    final Player tPlayer = this.getTargetPlayer();
                    final Player player = card.getController();
                    final int max = card.getXManaCostPaid();

                    final List<Card> graveList = tPlayer.getCardsIn(ZoneType.Graveyard);
                    final int x = Math.min(max, graveList.size());

                    if (player.isHuman()) {
                        for (int i = 0; i < x; i++) {
                            final Object o = GuiChoose.one("Remove from game", graveList);
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
                    final List<Card> graveList = AllZone.getHumanPlayer().getCardsIn(ZoneType.Graveyard);

                    final int maxX = ComputerUtil.getAvailableMana(true).size() - 1;
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
                    final List<Card> creatures = AllZoneUtil.getCreaturesInPlay(opponent);
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
                            final List<Card> creatures = AllZoneUtil.getCreaturesInPlay(opponent);

                            for (final Card creature : creatures) {
                                // System.out.println("Siren's Call - EOT - "+creature.getName()
                                // +" flag: "+creature.getSirenAttackOrDestroy());
                                // System.out.println("Siren's Call - EOT - "+creature.getName()
                                // +" attacked?: "+creature.getCreatureAttackedThisCombat());
                                if (creature.getSirenAttackOrDestroy() && !creature.getDamageHistory().getCreatureAttackedThisTurn()) {
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
                    final PlayerZone lib = card.getController().getZone(ZoneType.Library);
                    final List<Card> choices = new ArrayList<Card>();
                    for (int i = 0; (i < 3) && (lib.size() > 0); i++) {
                        choices.add(lib.get(i));
                    }

                    for (int i = 0; (i < 3) && !choices.isEmpty(); i++) {
                        final Object o = GuiChoose.one(this.prompt[i], choices);
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
                    final List<Card> ens = CardLists.filter(AllZoneUtil.getCardsIn(ZoneType.Battlefield), Presets.ENCHANTMENTS);
                    final List<Card> toReturn = CardLists.filter(ens, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            final Card enchanting = c.getEnchantingCard();

                            if (enchanting != null) {
                                if ((enchanting.isAttacking() && enchanting.getController().equals(you.getOpponent()))
                                        || enchanting.getController().equals(you)) {
                                    return true;
                                }
                            }

                            return (c.getOwner().equals(you) && c.getController().equals(you));
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
            final Cost abCost = new Cost(card, "2 U U", false);
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
                    final Object o = GuiChoose.one("Select permanent type", choices);
                    final String cardType = (String) o;
                    final List<Card> list = CardLists.getType(this.getTargetPlayer().getCardsIn(ZoneType.Battlefield), cardType);

                    final String[] tapOrUntap = new String[] { "Tap", "Untap" };
                    final Object z = GuiChoose.one("Tap or Untap?", tapOrUntap);
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
            class DrawbackWingPuncture extends AbilitySub {
                public DrawbackWingPuncture(final Card ca, final Target t) {
                    super(ca, t);
                }

                @Override
                public AbilitySub getCopy() {
                    AbilitySub res = new DrawbackWingPuncture(getSourceCard(),
                            getTarget() == null ? null : new Target(getTarget()));
                    CardFactoryUtil.copySpellAbility(this, res);
                    return res;
                }

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
            }
            final AbilitySub sub = new DrawbackWingPuncture(card, t2);

            final Cost abCost = new Cost(card, "G", false);
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
    } // getCard

} // end class CardFactory_Instants
