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

import javax.swing.JOptionPane;

import com.google.common.base.Predicate;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;

import forge.CardLists;
import forge.Command;
import forge.Counters;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.Target;
import forge.control.input.Input;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;

import forge.view.ButtonUtil;

/**
 * <p>
 * CardFactoryLands class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class CardFactoryLands {

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * @param cf
     *            a {@link forge.card.cardfactory.CardFactoryInterface} object.
     * @return a {@link forge.Card} object.
     */
    public static void buildCard(final Card card, final String cardName) {

        // *************** START *********** START **************************
        // Ravinca Dual Lands
        if (cardName.equals("Blood Crypt") || cardName.equals("Breeding Pool") || cardName.equals("Godless Shrine")
                || cardName.equals("Hallowed Fountain") || cardName.equals("Overgrown Tomb")
                || cardName.equals("Sacred Foundry") || cardName.equals("Steam Vents")
                || cardName.equals("Stomping Ground") || cardName.equals("Temple Garden")
                || cardName.equals("Watery Grave")) {
            // if this isn't done, computer plays more than 1 copy
            // card.clearSpellAbility();
            card.clearSpellKeepManaAbility();

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 7352127748114888255L;

                @Override
                public void execute() {
                    if (card.getController().isHuman()) {
                        this.humanExecute();
                    } else {
                        this.computerExecute();
                    }
                }

                public void computerExecute() {
                    boolean needsTheMana = false;
                    if (AllZone.getComputerPlayer().getLife() > 3) {
                        final int landsize = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer()).size();
                        for (Card c : AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand)) {
                            if (landsize == c.getCMC()) {
                                needsTheMana = true;
                            }
                        }
                    }
                    if (needsTheMana) {
                        AllZone.getComputerPlayer().payLife(2, card);
                    } else {
                        this.tapCard();
                    }
                }

                public void humanExecute() {
                    final int life = card.getController().getLife();
                    if (2 < life) {

                        final String question = String.format("Pay 2 life? If you don't, %s enters the battlefield tapped.", card.getName());

                        if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                            AllZone.getHumanPlayer().payLife(2, card);
                        } else {
                            this.tapCard();
                        }
                    } // if
                    else {
                        this.tapCard();
                    }
                } // execute()

                private void tapCard() {
                    // it enters the battlefield this way, and should not fire
                    // triggers
                    card.setTapped(true);
                }
            });
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Novijen, Heart of Progress")) {
            card.clearSpellKeepManaAbility();

            final Predicate<Card> targets = new Predicate<Card>() {

                @Override
                public boolean apply(final Card c) {
                    return AllZoneUtil.isCardInPlay(c) && c.isCreature()
                            && (c.getTurnInZone() == Singletons.getModel().getGameState().getPhaseHandler().getTurn());
                }
            };

            class AbilityNovijenHeartOfProgress extends AbilityActivated {
                public AbilityNovijenHeartOfProgress(final Card ca, final Cost co, final Target t) {
                    super(ca, co, t);
                }

                @Override
                public AbilityActivated getCopy() {
                    AbilityActivated res = new AbilityNovijenHeartOfProgress(getSourceCard(),
                            getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                    CardFactoryUtil.copySpellAbility(this, res);
                    return res;
                }

                private static final long serialVersionUID = 1416258136308898492L;

                private final List<Card> inPlay = new ArrayList<Card>();

                @Override
                public boolean canPlayAI() {
                    if (Singletons.getModel().getGameState().getPhaseHandler().getPhase() != PhaseType.MAIN1
                     && Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().isComputer()) {
                        return false;
                    }
                    this.inPlay.clear();
                    this.inPlay.addAll(AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield));
                    return (CardLists.filter(this.inPlay, targets).size() > 1) && super.canPlayAI();
                }

                @Override
                public void resolve() {
                    this.inPlay.clear();
                    this.inPlay.addAll(AllZoneUtil.getCardsIn(ZoneType.Battlefield));
                    for (final Card targ : CardLists.filter(this.inPlay, targets)) {
                        targ.addCounter(Counters.P1P1, 1);
                    }
                }
            }

            final Cost abCost = new Cost(card, "G U T", true);
            final AbilityActivated ability = new AbilityNovijenHeartOfProgress(card, abCost, null);

            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append(abCost);
            sbDesc.append("Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            ability.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(cardName);
            sbStack.append(" - Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            ability.setStackDescription(sbStack.toString());

            card.addSpellAbility(ability);
        }
        // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Sheltered Valley")) {

            /*
             * If Sheltered Valley would enter the battlefield, instead
             * sacrifice each other permanent named Sheltered Valley you
             * control, then put Sheltered Valley onto the battlefield.
             */
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 685604326470832887L;

                @Override
                public void execute() {
                    final Player player = card.getController();
                    final List<Card> land = player.getCardsIn(ZoneType.Battlefield, "Sheltered Valley");
                    land.remove(card);

                    if (land.size() > 0) {
                        for (final Card c : land) {
                            Singletons.getModel().getGameAction().sacrifice(c, null);
                        }
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        // Lorwyn Dual Lands, and a couple Morningtide...
        else if (cardName.equals("Ancient Amphitheater") || cardName.equals("Auntie's Hovel")
                || cardName.equals("Gilt-Leaf Palace") || cardName.equals("Secluded Glen")
                || cardName.equals("Wanderwine Hub") || cardName.equals("Rustic Clachan")
                || cardName.equals("Murmuring Bosk") || cardName.equals("Primal Beyond")) {

            String shortTemp = "";
            if (cardName.equals("Ancient Amphitheater")) {
                shortTemp = "Giant";
            }
            else if (cardName.equals("Auntie's Hovel")) {
                shortTemp = "Goblin";
            }
            else if (cardName.equals("Gilt-Leaf Palace")) {
                shortTemp = "Elf";
            }
            else if (cardName.equals("Secluded Glen")) {
                shortTemp = "Faerie";
            }
            else if (cardName.equals("Wanderwine Hub")) {
                shortTemp = "Merfolk";
            }
            else if (cardName.equals("Rustic Clachan")) {
                shortTemp = "Kithkin";
            }
            else if (cardName.equals("Murmuring Bosk")) {
                shortTemp = "Treefolk";
            }
            else if (cardName.equals("Primal Beyond")) {
                shortTemp = "Elemental";
            }

            final String type = shortTemp;

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -5646344170306812481L;

                @Override
                public void execute() {
                    if (card.getController().isHuman()) {
                        this.humanExecute();
                    } else {
                        this.computerExecute();
                    }
                }

                public void computerExecute() {
                    List<Card> hand = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand);
                    hand = CardLists.getType(hand, type);
                    if (hand.size() > 0) {
                        this.revealCard(hand.get(0));
                    } else {
                        card.setTapped(true);
                    }
                }

                public void humanExecute() {
                    AllZone.getInputControl().setInput(new Input() {
                        private static final long serialVersionUID = -2774066137824255680L;

                        @Override
                        public void showMessage() {
                            final StringBuilder sb = new StringBuilder();
                            sb.append(card.getName()).append(" - Reveal a card.");
                            CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                            ButtonUtil.enableOnlyCancel();
                        }

                        @Override
                        public void selectCard(final Card c, final PlayerZone zone) {
                            if (zone.is(ZoneType.Hand) && c.isType(type)) {
                                final StringBuilder sb = new StringBuilder();
                                sb.append("Revealed card: ").append(c.getName());
                                JOptionPane.showMessageDialog(null, sb.toString(), card.getName(),
                                        JOptionPane.PLAIN_MESSAGE);
                                this.stop();
                            }
                        }

                        @Override
                        public void selectButtonCancel() {
                            card.setTapped(true);
                            this.stop();
                        }
                    });
                } // execute()

                private void revealCard(final Card c) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(c.getController()).append(" reveals ").append(c.getName());
                    JOptionPane.showMessageDialog(null, sb.toString(), card.getName(), JOptionPane.PLAIN_MESSAGE);
                }
            });
        } // *************** END ************ END **************************
    }

} // end class CardFactoryLands
