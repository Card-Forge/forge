/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that itinksidd will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.phase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.Command;
import forge.CounterType;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.SpellManaCost;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.mana.ManaCostParser;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.control.input.InputSelectManyCards;
import forge.game.GameState;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;

import forge.view.ButtonUtil;

/**
 * <p>
 * The Upkeep class handles ending effects with "until your next upkeep" and
 * "until next upkeep".
 * 
 * It also handles hardcoded triggers "At the beginning of upkeep".
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Upkeep extends Phase {
    private static final long serialVersionUID = 6906459482978819354L;

    /**
     * <p>
     * Handles all the hardcoded events that happen at the beginning of each
     * Upkeep Phase.
     * 
     * This will freeze the Stack at the start, and unfreeze the Stack at the
     * end.
     * </p>
     */
    @Override
    public final void executeAt() {
        Singletons.getModel().getGame().getStack().freezeStack();
        Upkeep.upkeepBraidOfFire();

        Upkeep.upkeepSlowtrips(); // for "Draw a card at the beginning of the next turn's upkeep."
        Upkeep.upkeepUpkeepCost(); // sacrifice unless upkeep cost is paid
        Upkeep.upkeepEcho();

        Upkeep.upkeepTheAbyss();
        Upkeep.upkeepDropOfHoney();
        Upkeep.upkeepDemonicHordes();
        Upkeep.upkeepTangleWire();

        Upkeep.upkeepKarma();
        Upkeep.upkeepOathOfDruids();
        Upkeep.upkeepOathOfGhouls();
        Upkeep.upkeepSuspend();
        Upkeep.upkeepVanishing();
        Upkeep.upkeepFading();
        Upkeep.upkeepBlazeCounters();
        Upkeep.upkeepCurseOfMisfortunes();
        Upkeep.upkeepPowerSurge();

        Singletons.getModel().getGame().getStack().unfreezeStack();
    }

    // UPKEEP CARDS:

    /**
     * <p>
     * upkeepBraidOfFire.
     * </p>
     */
    private static void upkeepBraidOfFire() {
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        final List<Card> braids = player.getCardsIn(ZoneType.Battlefield, "Braid of Fire");

        for (int i = 0; i < braids.size(); i++) {
            final Card c = braids.get(i);

            final StringBuilder sb = new StringBuilder();
            sb.append("Cumulative Upkeep for ").append(c).append("\n");
            final Ability upkeepAbility = new Ability(c, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    c.addCounter(CounterType.AGE, 1, true);
                    StringBuilder rs = new StringBuilder("R");
                    for (int ageCounters = c.getCounters(CounterType.AGE); ageCounters > 1; ageCounters--) {
                        rs.append(" R");
                    }
                    Map<String, String> produced = new HashMap<String, String>();
                    produced.put("Produced", rs.toString());
                    final AbilityManaPart abMana = new AbilityManaPart(c, produced);
                    if (player.isComputer()) {
                        abMana.produceMana(this);
                    } else if (GameActionUtil.showYesNoDialog(c, sb.toString())) {
                        abMana.produceMana(this);
                    } else {
                        Singletons.getModel().getGame().getAction().sacrifice(c, null);
                    }

                }
            };
            upkeepAbility.setStackDescription(sb.toString());

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(upkeepAbility);

        }
    } // upkeepBraidOfFire

    /**
     * <p>
     * upkeepEcho.
     * </p>
     */
    private static void upkeepEcho() {
        List<Card> list = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield);
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasStartOfKeyword("(Echo unpaid)");
            }
        });

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            if (c.hasStartOfKeyword("(Echo unpaid)")) {

                final Command paidCommand = Command.BLANK;

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -7354791599039157375L;

                    @Override
                    public void execute() {
                        Singletons.getModel().getGame().getAction().sacrifice(c, null);
                    }
                };

                final Ability blankAbility = Upkeep.BlankAbility(c, c.getEchoCost());

                final StringBuilder sb = new StringBuilder();
                sb.append("Echo for ").append(c).append("\n");

                final Ability sacAbility = new Ability(c, SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        Player controller = c.getController();
                        if (controller.isHuman()) {
                            Cost cost = new Cost(c, c.getEchoCost().trim(), true);
                            GameActionUtil.payCostDuringAbilityResolve(blankAbility, cost, paidCommand, unpaidCommand, null);
                        } else { // computer
                            if (ComputerUtil.canPayCost(blankAbility, controller)) {
                                ComputerUtil.playNoStack(controller, blankAbility);
                            } else {
                                Singletons.getModel().getGame().getAction().sacrifice(c, null);
                            }
                        }
                    }
                };
                sacAbility.setStackDescription(sb.toString());
                sacAbility.setDescription(sb.toString());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(sacAbility);

                c.removeAllExtrinsicKeyword("(Echo unpaid)");
            }
        }
    } // echo

    /**
     * <p>
     * upkeepSlowtrips. Draw a card at the beginning of the next turn's upkeep.
     * </p>
     */
    private static void upkeepSlowtrips() {
        Player turnOwner = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        // does order matter here?
        drawForSlowtrips(turnOwner);
        for (Player p : Singletons.getModel().getGame().getPlayers()) {
            if (p == turnOwner) {
                continue;
            }
            drawForSlowtrips(p);
        }
    }

    public static void drawForSlowtrips(final Player player) {
        List<Card> list = player.getSlowtripList();

        for (Card card : list) {
            // otherwise another slowtrip gets added
            card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep.");

            final Ability slowtrip = new Ability(card, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    player.drawCard();
                }
            };
            slowtrip.setStackDescription(card + " - Draw a card.");
            slowtrip.setDescription(card + " - Draw a card.");

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(slowtrip);

        }
        player.clearSlowtripList();
    }

    /**
     * <p>
     * upkeepUpkeepCost.
     * </p>
     */
    private static void upkeepUpkeepCost() {
        final List<Card> list = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield);

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            final Player controller = c.getController();
            final ArrayList<String> a = c.getKeyword();
            for (int j = 0; j < a.size(); j++) {
                final String ability = a.get(j);

                // destroy
                if (ability.startsWith("At the beginning of your upkeep, destroy CARDNAME")) {
                    final String[] k = ability.split(" pay ");
                    final SpellManaCost upkeepCost = new SpellManaCost(new ManaCostParser(k[1]));

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 8942537892273123542L;

                        @Override
                        public void execute() {
                            if (c.getName().equals("Cosmic Horror")) {
                                controller.addDamage(7, c);
                            }
                            Singletons.getModel().getGame().getAction().destroy(c);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability aiPaid = Upkeep.BlankAbility(c, upkeepCost.toString());

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Upkeep for ").append(c).append("\n");
                    final Ability upkeepAbility = new Ability(c, SpellManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand, unpaidCommand);
                            } else { // computer
                                if (ComputerUtil.canPayCost(aiPaid, controller) && !c.hasKeyword("Indestructible")) {
                                    ComputerUtil.playNoStack(controller, aiPaid);
                                } else {
                                    if (c.getName().equals("Cosmic Horror")) {
                                        controller.addDamage(7, c);
                                    }
                                    Singletons.getModel().getGame().getAction().destroy(c);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(upkeepAbility);
                } // destroy

                // sacrifice
                if (ability.startsWith("At the beginning of your upkeep, sacrifice")
                        || ability.startsWith("Cumulative upkeep")) {
                    String cost = "0";
                    final StringBuilder sb = new StringBuilder();

                    if (ability.startsWith("At the beginning of your upkeep, sacrifice")) {
                        final String[] k = ability.split(" pay ");
                        cost = k[1].toString();
                        sb.append("Sacrifice upkeep for ").append(c).append("\n");
                    }

                    if (ability.startsWith("Cumulative upkeep")) {
                        final String[] k = ability.split(":");
                        c.addCounter(CounterType.AGE, 1, true);
                        cost = CardFactoryUtil.multiplyCost(k[1], c.getCounters(CounterType.AGE));
                        sb.append("Cumulative upkeep for ").append(c).append("\n");
                    }

                    final String upkeepCost = cost;

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 5612348769167529102L;

                        @Override
                        public void execute() {
                            Singletons.getModel().getGame().getAction().sacrifice(c, null);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability blankAbility = Upkeep.BlankAbility(c, upkeepCost);

                    final Ability upkeepAbility = new Ability(c, SpellManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payCostDuringAbilityResolve(blankAbility, blankAbility.getPayCosts(),
                                        paidCommand, unpaidCommand, null);
                            } else { // computer
                                if (ComputerUtil.shouldPayCost(controller, c, upkeepCost) && ComputerUtil.canPayCost(blankAbility, controller)) {
                                    ComputerUtil.playNoStack(controller, blankAbility);
                                } else {
                                    Singletons.getModel().getGame().getAction().sacrifice(c, null);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(upkeepAbility);
                } // sacrifice

                // destroy
                if (ability.startsWith("At the beginning of your upkeep, CARDNAME deals ")) {
                    final String[] k = ability.split("deals ");
                    final String s1 = k[1].substring(0, 2);
                    final int upkeepDamage = Integer.parseInt(s1.trim());
                    final String[] l = k[1].split(" pay ");
                    final SpellManaCost upkeepCost = new SpellManaCost(new ManaCostParser(l[1]));

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 1238166187561501928L;

                        @Override
                        public void execute() {
                            controller.addDamage(upkeepDamage, c);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability aiPaid = Upkeep.BlankAbility(c, upkeepCost.toString());

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Damage upkeep for ").append(c).append("\n");
                    final Ability upkeepAbility = new Ability(c, SpellManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand, unpaidCommand);
                            } else { // computers
                                if (ComputerUtil.canPayCost(aiPaid, controller)
                                        && (controller.predictDamage(upkeepDamage, c, false) > 0)) {
                                    ComputerUtil.playNoStack(controller, aiPaid);
                                } else {
                                    controller.addDamage(upkeepDamage, c);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(upkeepAbility);
                } // destroy
            }

        } // for
    } // upkeepCost

    /**
     * <p>
     * upkeepAIPayment.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link java.lang.String} object.
     * @param cost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.Ability} object.
     */
    private static Ability BlankAbility(final Card c, final String costString) {
        Cost cost = new Cost(c, costString, true);
        return new AbilityStatic(c, cost, null) {
            @Override
            public void resolve() {

            }
        };
    }

    /**
     * <p>
     * upkeepTheAbyss.
     * </p>
     */
    private static void upkeepTheAbyss() {
        /*
         * At the beginning of each player's upkeep, destroy target nonartifact
         * creature that player controls of his or her choice. It can't be
         * regenerated.
         */
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final List<Card> the = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("The Abyss"));
        final List<Card> magus = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Magus of the Abyss"));

        final List<Card> cards = new ArrayList<Card>();
        cards.addAll(the);
        cards.addAll(magus);

        for (final Card c : cards) {
            final Card abyss = c;

            final List<Card> abyssGetTargets = CardLists.filter(player.getCreaturesInPlay(), Presets.NON_ARTIFACTS);

            final Ability sacrificeCreature = new Ability(abyss, SpellManaCost.EMPTY) {
                @Override
                public void resolve() {
                    final List<Card> targets = CardLists.getTargetableCards(abyssGetTargets, this);
                    final Input chooseArt = new InputSelectManyCards(1, 1) {
                        private static final long serialVersionUID = 4820011040853968644L;

                        @Override
                        public String getMessage() {
                            return abyss.getName() + " - Select one nonartifact creature to destroy";
                        }

                        @Override
                        protected boolean isValidChoice(Card choice) {
                            return choice.isCreature() && !choice.isArtifact() && canTarget(choice) && choice.getController() == player;
                        };

                        @Override
                        protected Input onDone() {
                            Singletons.getModel().getGame().getAction().destroyNoRegeneration(selected.get(0));
                            return null;
                        }
                    };
                    if (player.isHuman() && targets.size() > 0) {
                        Singletons.getModel().getMatch().getInput().setInput(chooseArt); // Input
                    } else { // computer

                        final List<Card> indestruct = CardLists.getKeyword(targets, "Indestructible");
                        if (indestruct.size() > 0) {
                            Singletons.getModel().getGame().getAction().destroyNoRegeneration(indestruct.get(0));
                        } else if (targets.size() > 0) {
                            final Card target = CardFactoryUtil.getWorstCreatureAI(targets);
                            if (null == target) {
                                // must be nothing valid to destroy
                            } else {
                                Singletons.getModel().getGame().getAction().destroyNoRegeneration(target);
                            }
                        }
                    }
                } // resolve
            }; // sacrificeCreature

            final StringBuilder sb = new StringBuilder();
            sb.append(abyss.getName()).append(" - destroy a nonartifact creature of your choice.");
            sacrificeCreature.setStackDescription(sb.toString());
            Singletons.getModel().getGame().getStack().addAndUnfreeze(sacrificeCreature);
        } // end for
    } // The Abyss

    /**
     * <p>
     * upkeepDropOfHoney.
     * </p>
     */
    private static void upkeepDropOfHoney() {
        /*
         * At the beginning of your upkeep, destroy the creature with the least
         * power. It can't be regenerated. If two or more creatures are tied for
         * least power, you choose one of them.
         */
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final List<Card> drops = player.getCardsIn(ZoneType.Battlefield, "Drop of Honey");
        drops.addAll(player.getCardsIn(ZoneType.Battlefield, "Porphyry Nodes"));
        final List<Card> cards = drops;

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);

            final Ability ability = new Ability(c, SpellManaCost.EMPTY) {
                @Override
                public void resolve() {
                    final List<Card> creatures = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
                    if (creatures.size() > 0) {
                        CardLists.sortAttackLowFirst(creatures);
                        final int power = creatures.get(0).getNetAttack();
                        if (player.isHuman()) {
                            Singletons.getModel().getMatch().getInput().setInput(
                                    CardFactoryUtil.inputDestroyNoRegeneration(this.getLowestPowerList(creatures),
                                            "Select creature with power: " + power + " to sacrifice."));
                        } else { // computer
                            final Card compyTarget = this.getCompyCardToDestroy(creatures);
                            Singletons.getModel().getGame().getAction().destroyNoRegeneration(compyTarget);
                        }
                    }
                } // resolve

                private List<Card> getLowestPowerList(final List<Card> original) {
                    final List<Card> lowestPower = new ArrayList<Card>();
                    final int power = original.get(0).getNetAttack();
                    int i = 0;
                    while ((i < original.size()) && (original.get(i).getNetAttack() == power)) {
                        lowestPower.add(original.get(i));
                        i++;
                    }
                    return lowestPower;
                }

                private Card getCompyCardToDestroy(final List<Card> original) {
                    final List<Card> options = this.getLowestPowerList(original);
                    final List<Card> humanCreatures = CardLists.filter(options, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return c.getController().isHuman();
                        }
                    });
                    if (humanCreatures.isEmpty()) {
                        CardLists.shuffle(options);
                        return options.get(0);
                    } else {
                        CardLists.shuffle(humanCreatures);
                        return humanCreatures.get(0);
                    }
                }
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - destroy 1 creature with lowest power.");
            ability.setStackDescription(sb.toString());

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

        } // end for
    } // upkeepDropOfHoney()

    /**
     * <p>
     * upkeepDemonicHordes.
     * </p>
     */
    private static void upkeepDemonicHordes() {

        /*
         * At the beginning of your upkeep, unless you pay BBB, tap Demonic
         * Hordes and sacrifice a land of an opponent's choice.
         */

        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final List<Card> cards = player.getCardsIn(ZoneType.Battlefield, "Demonic Hordes");

        for (int i = 0; i < cards.size(); i++) {

            final Card c = cards.get(i);

            final Ability cost = new Ability(c, new SpellManaCost(new ManaCostParser("B B B"))) {
                @Override
                public void resolve() {
                }
            }; // end cost ability

            final Ability noPay = new Ability(c, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    final List<Card> playerLand = player.getLandsInPlay();

                    c.tap();
                    if (playerLand.isEmpty()) {
                        return;
                    }
                    Card target = null;
                    if (c.getController().isComputer()) {
                        target = GuiChoose.one("Select a card to sacrifice", playerLand);
                    } else {
                        target = CardFactoryUtil.getBestLandAI(playerLand);
                    }
                    Singletons.getModel().getGame().getAction().sacrifice(target, null);
                } // end resolve()
            }; // end noPay ability

            final Player cp = c.getController();
            if (cp.isHuman()) {
                final String question = "Pay Demonic Hordes upkeep cost?";
                if (GameActionUtil.showYesNoDialog(c, question)) {
                    final Ability pay = new Ability(c, SpellManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            if (Singletons.getModel().getGame().getZoneOf(c).is(ZoneType.Battlefield)) {
                                final StringBuilder coststring = new StringBuilder();
                                coststring.append("Pay cost for ").append(c).append("\r\n");
                                GameActionUtil.payManaDuringAbilityResolve(coststring.toString(), cost.getManaCost(),
                                        Command.BLANK, Command.BLANK);
                            }
                        } // end resolve()
                    }; // end pay ability
                    pay.setStackDescription("Demonic Hordes - Upkeep Cost");
                    pay.setDescription("Demonic Hordes - Upkeep Cost");

                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(pay);

                } // end choice
                else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(c.getName()).append(" - is tapped and you must sacrifice a land of opponent's choice");
                    noPay.setStackDescription(sb.toString());

                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(noPay);

                }
            } // end human
            else { // computer
                noPay.setActivatingPlayer(cp);
                if (ComputerUtil.canPayCost(cost, cp)) {
                    final Ability computerPay = new Ability(c, SpellManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            ComputerUtil.payManaCost(cp, cost);
                        }
                    };
                    computerPay.setStackDescription("Computer pays Demonic Hordes upkeep cost");

                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(computerPay);
                } else {
                    noPay.setStackDescription("Demonic Hordes - Upkeep Cost");
                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(noPay);

                }
            } // end computer

        } // end for loop

    } // upkeepDemonicHordes

    /**
     * <p>
     * upkeepSuspend.
     * </p>
     */
    private static void upkeepSuspend() {
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        List<Card> list = player.getCardsIn(ZoneType.Exile);

        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasSuspend();
            }
        });

        if (list.size() == 0) {
            return;
        }

        for (final Card c : list) {
            final int counters = c.getCounters(CounterType.TIME);
            if (counters > 0) {
                c.subtractCounter(CounterType.TIME, 1);
            }
        }
    } // suspend

    /**
     * <p>
     * upkeepVanishing.
     * </p>
     */
    private static void upkeepVanishing() {

        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        List<Card> list = player.getCardsIn(ZoneType.Battlefield);
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CardFactoryUtil.hasKeyword(c, "Vanishing") != -1;
            }
        });
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                final Ability ability = new Ability(card, SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        card.subtractCounter(CounterType.TIME, 1);
                    }
                }; // ability

                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Vanishing - remove a time counter from it. ");
                sb.append("When the last is removed, sacrifice it.)");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());
                ability.setActivatingPlayer(card.getController());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * upkeepFading.
     * </p>
     */
    private static void upkeepFading() {

        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        List<Card> list = player.getCardsIn(ZoneType.Battlefield);
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CardFactoryUtil.hasKeyword(c, "Fading") != -1;
            }
        });
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                final Ability ability = new Ability(card, SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final int fadeCounters = card.getCounters(CounterType.FADE);
                        if (fadeCounters <= 0) {
                            Singletons.getModel().getGame().getAction().sacrifice(card, null);
                        } else {
                            card.subtractCounter(CounterType.FADE, 1);
                        }
                    }
                }; // ability

                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Fading - remove a fade counter from it. ");
                sb.append("If you can't, sacrifice it.)");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());
                ability.setActivatingPlayer(card.getController());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * upkeepOathOfDruids.
     * </p>
     */
    private static void upkeepOathOfDruids() {
        final List<Card> oathList = CardLists.filter(Singletons.getModel().getGame()
                .getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Oath of Druids"));
        if (oathList.isEmpty()) {
            return;
        }

        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        if (GameState.compareTypeAmountInPlay(player, "Creature") < 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Card oath = oathList.get(i);
                final Ability ability = new Ability(oath, SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final List<Card> libraryList = new ArrayList<Card>(player.getCardsIn(ZoneType.Library));
                        final PlayerZone battlefield = player.getZone(ZoneType.Battlefield);
                        boolean oathFlag = true;

                        if (GameState.compareTypeAmountInPlay(player, "Creature") < 0) {
                            if (player.isHuman()) {
                                final StringBuilder question = new StringBuilder();
                                question.append("Reveal cards from the top of your library and place ");
                                question.append("the first creature revealed onto the battlefield?");
                                if (!GameActionUtil.showYesNoDialog(oath, question.toString())) {
                                    oathFlag = false;
                                }
                            } else { // if player == Computer
                                final List<Card> creaturesInLibrary =
                                        CardLists.filter(player.getCardsIn(ZoneType.Library), CardPredicates.Presets.CREATURES);
                                final List<Card> creaturesInBattlefield =
                                        CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);

                                // if there are at least 3 creatures in library,
                                // or none in play with one in library, oath
                                if ((creaturesInLibrary.size() > 2)
                                        || ((creaturesInBattlefield.size() == 0) && (creaturesInLibrary.size() > 0))) {
                                    oathFlag = true;
                                } else {
                                    oathFlag = false;
                                }
                            }

                            if (oathFlag) {
                                final List<Card> cardsToReveal = new ArrayList<Card>();
                                for (final Card c : libraryList) {
                                    cardsToReveal.add(c);
                                    if (c.isCreature()) {
                                        Singletons.getModel().getGame().getAction().moveTo(battlefield, c);
                                        break;
                                    } else {
                                        Singletons.getModel().getGame().getAction().moveToGraveyard(c);
                                    }
                                } // for loop
                                if (cardsToReveal.size() > 0) {
                                    GuiChoose.one("Revealed cards", cardsToReveal);
                                }
                            }
                        }
                    }
                }; // Ability

                final StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, that player chooses target player ");
                sb.append("who controls more creatures than he or she does and is his or her opponent. The ");
                sb.append("first player may reveal cards from the top of his or her library until he or she ");
                sb.append("reveals a creature card. If he or she does, that player puts that card onto the ");
                sb.append("battlefield and all other cards revealed this way into his or her graveyard.");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());
                ability.setActivatingPlayer(oath.getController());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);
            }
        }
    } // upkeepOathOfDruids()

    /**
     * <p>
     * upkeepOathOfGhouls.
     * </p>
     */
    private static void upkeepOathOfGhouls() {
        final List<Card> oathList = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Oath of Ghouls"));
        if (oathList.isEmpty()) {
            return;
        }

        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        if (GameState.compareTypeAmountInGraveyard(player, "Creature") > 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Ability ability = new Ability(oathList.get(0), SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final List<Card> graveyardCreatures =
                                CardLists.filter(player.getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);

                        if (GameState.compareTypeAmountInGraveyard(player, "Creature") > 0) {
                            if (player.isHuman()) {
                                final Card o = GuiChoose.oneOrNone("Pick a creature to return to hand", graveyardCreatures);
                                if (o != null) {
                                    final Card card = o;

                                    Singletons.getModel().getGame().getAction().moveToHand(card);
                                }
                            } else if (player.isComputer()) {
                                final Card card = graveyardCreatures.get(0);

                                Singletons.getModel().getGame().getAction().moveToHand(card);
                            }
                        }
                    }
                }; // Ability

                final StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, Oath of Ghouls returns a creature ");
                sb.append("from their graveyard to owner's hand if they have more than an opponent.");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());
                ability.setActivatingPlayer(oathList.get(0).getController());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

            }
        }
    } // Oath of Ghouls

    /**
     * <p>
     * upkeepKarma.
     * </p>
     */
    private static void upkeepKarma() {
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final List<Card> karmas =
                CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Karma"));
        final List<Card> swamps = CardLists.getType(player.getCardsIn(ZoneType.Battlefield), "Swamp");

        // determine how much damage to deal the current player
        final int damage = swamps.size();

        // if there are 1 or more Karmas on the
        // battlefield have each of them deal damage.
        if (0 < karmas.size()) {
            for (final Card karma : karmas) {
                final Ability ability = new Ability(karma, SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        if (damage > 0) {
                            player.addDamage(damage, karma);
                        }
                    }
                }; // Ability
                if (damage > 0) {

                final StringBuilder sb = new StringBuilder();
                sb.append("Karma deals ").append(damage).append(" damage to ").append(player);
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());
                ability.setActivatingPlayer(karma.getController());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);
                }
            }
        } // if
    } // upkeepKarma()

    /**
     * <p>
     * upkeepPowerSurge.
     * </p>
     */
    private static void upkeepPowerSurge() {
        /*
         * At the beginning of each player's upkeep, Power Surge deals X damage
         * to that player, where X is the number of untapped lands he or she
         * controlled at the beginning of this turn.
         */
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final List<Card> list = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Power Surge"));
        final int damage = player.getNumPowerSurgeLands();

        for (final Card surge : list) {
            final Card source = surge;
            final Ability ability = new Ability(source, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    player.addDamage(damage, source);
                }
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(source).append(" - deals ").append(damage).append(" damage to ").append(player);
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());

            if (damage > 0) {
                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);
            }
        } // for
    } // upkeepPowerSurge()

    /**
     * <p>
     * upkeepTangleWire.
     * </p>
     */
    private static void upkeepTangleWire() {
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final List<Card> wires = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Tangle Wire"));

        for (final Card source : wires) {
            final SpellAbility ability = new Ability(source, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    final int num = source.getCounters(CounterType.FADE);
                    final List<Card> list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return (c.isArtifact() || c.isLand() || c.isCreature()) && c.isUntapped();
                        }
                    });

                    for (int i = 0; i < num; i++) {
                        if (player.isComputer()) {
                            Card toTap = CardFactoryUtil.getWorstPermanentAI(list, false, false, false, false);
                            // try to find non creature cards without tap abilities
                            List<Card> betterList = CardLists.filter(list, new Predicate<Card>() {
                                @Override
                                public boolean apply(final Card c) {
                                    if (c.isCreature()) {
                                        return false;
                                    }
                                    for (SpellAbility sa : c.getAllSpellAbilities()) {
                                        if (sa.getPayCosts() != null && sa.getPayCosts().hasTapCost()) {
                                            return false;
                                        }
                                    }
                                    return true;
                                }
                            });
                            System.out.println("Tangle Wire" + list + " - " + betterList);
                            if (!betterList.isEmpty()) {
                                toTap = betterList.get(0);
                            }
                            if (null != toTap) {
                                toTap.tap();
                                list.remove(toTap);
                            }
                        } else {
                            Singletons.getModel().getMatch().getInput().setInput(new Input() {
                                private static final long serialVersionUID = 5313424586016061612L;

                                @Override
                                public void showMessage() {
                                    if (list.isEmpty()) {
                                        this.stop();
                                        return;
                                    }
                                    String message = String.format("%s - Select %d untapped artifact(s), creature(s), or land(s) you control", source.getName(), num);
                                    CMatchUI.SINGLETON_INSTANCE.showMessage(message);
                                    ButtonUtil.disableAll();
                                }

                                @Override
                                public void selectCard(final Card card) {
                                    Zone zone = Singletons.getModel().getGame().getZoneOf(card);
                                    if (zone.is(ZoneType.Battlefield, player) && list.contains(card)) {
                                        card.tap();
                                        list.remove(card);
                                        this.stop();
                                    }
                                }
                            });
                        }
                    }
                }
            };
            ability.setStackDescription(source.getName() + " - " + player
                    + " taps X artifacts, creatures or lands he or she controls.");
            ability.setDescription(source.getName() + " - " + player
                    + " taps X artifacts, creatures or lands he or she controls.");

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

        } // foreach(wire)
    } // upkeepTangleWire()

    /**
     * <p>
     * upkeepBlazeCounters.
     * </p>
     */
    private static void upkeepBlazeCounters() {
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        List<Card> blaze = player.getCardsIn(ZoneType.Battlefield);
        blaze = CardLists.filter(blaze, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isLand() && (c.getCounters(CounterType.BLAZE) > 0);
            }
        });

        for (int i = 0; i < blaze.size(); i++) {
            final Card source = blaze.get(i);
            final Ability ability = new Ability(blaze.get(i),SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    player.addDamage(1, source);
                }
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(blaze.get(i)).append(" - has a blaze counter and deals 1 damage to ");
            sb.append(player).append(".");
            ability.setStackDescription(sb.toString());

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

        }
    }

    /**
     * <p>
     * upkeepCurseOfMisfortunes.
     * </p>
     */
    private static void upkeepCurseOfMisfortunes() {
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        final List<Card> misfortunes = player.getCardsIn(ZoneType.Battlefield, "Curse of Misfortunes");

        for (int i = 0; i < misfortunes.size(); i++) {
            final Card source = misfortunes.get(i);

            final Ability ability = new Ability(source, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    final List<Card> enchantmentsAttached = new ArrayList<Card>(source.getEnchantingPlayer().getEnchantedBy());
                    List<Card> enchantmentsInLibrary =
                            CardLists.filter(source.getController().getCardsIn(ZoneType.Library), new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return c.isEnchantment() && c.hasKeyword("Enchant player")
                                    && !source.getEnchantingPlayer().hasProtectionFrom(c)
                                    && !Iterables.any(enchantmentsAttached, CardPredicates.nameEquals(c.getName()));
                        }
                    });
                    final Player player = source.getController();
                    Card enchantment = null;
                    if (player.isHuman()) {
                        final Card[] target = new Card[enchantmentsInLibrary.size()];
                        for (int j = 0; j < enchantmentsInLibrary.size(); j++) {
                            final Card crd = enchantmentsInLibrary.get(j);
                            target[j] = crd;
                        }
                        final Object check = GuiChoose.oneOrNone("Select Curse to attach", target);
                        if (check != null) {
                            enchantment = ((Card) check);
                        }
                    } else {
                        enchantment = CardFactoryUtil.getBestEnchantmentAI(enchantmentsInLibrary, this, false);
                    }
                    if (enchantment != null) {
                        Singletons.getModel().getGame().getAction().changeZone(
                                Singletons.getModel().getGame().getZoneOf(enchantment),
                                enchantment.getOwner().getZone(ZoneType.Battlefield), enchantment, null);
                        enchantment.enchantEntity(source.getEnchantingPlayer());
                    }
                    source.getController().shuffle();
                } // resolve
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(source).append(
                    " At the beginning of your upkeep, you may search your library for a Curse card that doesn't have"
                            + " the same name as a Curse attached to enchanted player, "
                            + "put it onto the battlefield attached to that player, then shuffle you library.");
            ability.setStackDescription(sb.toString());
            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);
        }
    } // upkeepCurseOfMisfortunes

} // end class Upkeep
