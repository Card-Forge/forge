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
package forge.game.phase;

import java.util.ArrayList;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardCharactersticName;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Command;
import forge.Counters;
import forge.GameAction;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.player.PlayerUtil;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
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
public class Upkeep extends Phase implements java.io.Serializable {
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
        AllZone.getStack().freezeStack();
        Upkeep.upkeepBraidOfFire();

        Upkeep.upkeepSlowtrips(); // for "Draw a card at the beginning of the next turn's upkeep."
        Upkeep.upkeepUpkeepCost(); // sacrifice unless upkeep cost is paid
        Upkeep.upkeepEcho();

        Upkeep.upkeepTheAbyss();
        Upkeep.upkeepYawgmothDemon();
        Upkeep.upkeepDropOfHoney();
        Upkeep.upkeepDemonicHordes();
        Upkeep.upkeepTangleWire();

        Upkeep.upkeepVesuvanDoppelgangerKeyword();

        // Kinship cards
        Upkeep.upkeepInkDissolver();
        Upkeep.upkeepKithkinZephyrnaut();
        Upkeep.upkeepLeafCrownedElder();
        Upkeep.upkeepMudbuttonClanger();
        Upkeep.upkeepNightshadeSchemers();
        Upkeep.upkeepPyroclastConsul();
        Upkeep.upkeepSensationGorger();
        Upkeep.upkeepSqueakingPieGrubfellows();
        Upkeep.upkeepWanderingGraybeard();
        Upkeep.upkeepWaterspoutWeavers();
        Upkeep.upkeepWinnowerPatrol();
        Upkeep.upkeepWolfSkullShaman();


        Upkeep.upkeepKarma();
        Upkeep.upkeepOathOfDruids();
        Upkeep.upkeepOathOfGhouls();
        Upkeep.upkeepSuspend();
        Upkeep.upkeepVanishing();
        Upkeep.upkeepFading();
        Upkeep.upkeepBlazeCounters();
        Upkeep.upkeepCurseOfMisfortunes();
        Upkeep.upkeepPowerSurge();

        AllZone.getStack().unfreezeStack();
    }

    // UPKEEP CARDS:

    /**
     * <p>
     * upkeepBraidOfFire.
     * </p>
     */
    private static void upkeepBraidOfFire() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        final CardList braids = player.getCardsIn(ZoneType.Battlefield, "Braid of Fire");

        for (int i = 0; i < braids.size(); i++) {
            final Card c = braids.get(i);

            final StringBuilder sb = new StringBuilder();
            sb.append("Cumulative Upkeep for ").append(c).append("\n");
            final Ability upkeepAbility = new Ability(c, "0") {
                @Override
                public void resolve() {
                    c.addCounter(Counters.AGE, 1);
                    final int ageCounters = c.getCounters(Counters.AGE);
                    final AbilityMana abMana = new AbilityMana(c, "0", "R", ageCounters) {
                        private static final long serialVersionUID = -2182129023960978132L;
                    };
                    if (player.isComputer()) {
                        abMana.produceMana();
                    } else if (GameActionUtil.showYesNoDialog(c, sb.toString())) {
                        abMana.produceMana();
                    } else {
                        Singletons.getModel().getGameAction().sacrifice(c, null);
                    }

                }
            };
            upkeepAbility.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);

        }
    } // upkeepBraidOfFire

    /**
     * <p>
     * upkeepEcho.
     * </p>
     */
    private static void upkeepEcho() {
        CardList list = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
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
                        Singletons.getModel().getGameAction().sacrifice(c, null);
                    }
                };

                final Ability aiPaid = Upkeep.upkeepAIPayment(c, c.getEchoCost());

                final StringBuilder sb = new StringBuilder();
                sb.append("Echo for ").append(c).append("\n");

                final Ability sacAbility = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        if (c.getController().isHuman()) {
                            GameActionUtil.payManaDuringAbilityResolve(sb.toString(), c.getEchoCost(), paidCommand,
                                    unpaidCommand);
                        } else { // computer
                            if (ComputerUtil.canPayCost(aiPaid)) {
                                ComputerUtil.playNoStack(aiPaid);
                            } else {
                                Singletons.getModel().getGameAction().sacrifice(c, null);
                            }
                        }
                    }
                };
                sacAbility.setStackDescription(sb.toString());
                sacAbility.setDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sacAbility);

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
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        CardList list = player.getSlowtripList();

        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);

            // otherwise another slowtrip gets added
            card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep.");

            final Ability slowtrip = new Ability(card, "0") {
                @Override
                public void resolve() {
                    player.drawCard();
                }
            };
            slowtrip.setStackDescription(card + " - Draw a card.");
            slowtrip.setDescription(card + " - Draw a card.");

            AllZone.getStack().addSimultaneousStackEntry(slowtrip);

        }
        player.clearSlowtripList();

        // Do the same for the opponent
        final Player opponent = player.getOpponent();

        list = opponent.getSlowtripList();

        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);

            // otherwise another slowtrip gets added
            card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep.");

            final Ability slowtrip = new Ability(card, "0") {
                @Override
                public void resolve() {
                    opponent.drawCard();
                }
            };
            slowtrip.setStackDescription(card.getName() + " - Draw a card");
            slowtrip.setDescription(card + " - Draw a card.");

            AllZone.getStack().addSimultaneousStackEntry(slowtrip);

        }
        opponent.clearSlowtripList();
    }

    /**
     * <p>
     * upkeepUpkeepCost.
     * </p>
     */
    private static void upkeepUpkeepCost() {
        final CardList list = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield);

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            final Player controller = c.getController();
            final ArrayList<String> a = c.getKeyword();
            for (int j = 0; j < a.size(); j++) {
                final String ability = a.get(j);

                // destroy
                if (ability.startsWith("At the beginning of your upkeep, destroy CARDNAME")) {
                    final String[] k = ability.split(" pay ");
                    final String upkeepCost = k[1].toString();

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 8942537892273123542L;

                        @Override
                        public void execute() {
                            if (c.getName().equals("Cosmic Horror")) {
                                controller.addDamage(7, c);
                            }
                            Singletons.getModel().getGameAction().destroy(c);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability aiPaid = Upkeep.upkeepAIPayment(c, upkeepCost);

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Upkeep for ").append(c).append("\n");
                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand,
                                        unpaidCommand);
                            } else { // computer
                                if (ComputerUtil.canPayCost(aiPaid) && !c.hasKeyword("Indestructible")) {
                                    ComputerUtil.playNoStack(aiPaid);
                                } else {
                                    if (c.getName().equals("Cosmic Horror")) {
                                        controller.addDamage(7, c);
                                    }
                                    Singletons.getModel().getGameAction().destroy(c);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
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
                        c.addCounter(Counters.AGE, 1);
                        cost = CardFactoryUtil.multiplyManaCost(k[1], c.getCounters(Counters.AGE));
                        sb.append("Cumulative upkeep for ").append(c).append("\n");
                    }

                    final String upkeepCost = cost;

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 5612348769167529102L;

                        @Override
                        public void execute() {
                            Singletons.getModel().getGameAction().sacrifice(c, null);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability aiPaid = Upkeep.upkeepAIPayment(c, upkeepCost);

                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand,
                                        unpaidCommand);
                            } else { // computer
                                if (ComputerUtil.canPayCost(aiPaid)) {
                                    ComputerUtil.playNoStack(aiPaid);
                                } else {
                                    Singletons.getModel().getGameAction().sacrifice(c, null);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
                } // sacrifice

                // destroy
                if (ability.startsWith("At the beginning of your upkeep, CARDNAME deals ")) {
                    final String[] k = ability.split("deals ");
                    final String s1 = k[1].substring(0, 2);
                    final int upkeepDamage = Integer.parseInt(s1.trim());
                    final String[] l = k[1].split(" pay ");
                    final String upkeepCost = l[1].toString();

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 1238166187561501928L;

                        @Override
                        public void execute() {
                            controller.addDamage(upkeepDamage, c);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability aiPaid = Upkeep.upkeepAIPayment(c, upkeepCost);

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Damage upkeep for ").append(c).append("\n");
                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand,
                                        unpaidCommand);
                            } else { // computer
                                if (ComputerUtil.canPayCost(aiPaid)
                                        && (controller.predictDamage(upkeepDamage, c, false) > 0)) {
                                    ComputerUtil.playNoStack(aiPaid);
                                } else {
                                    controller.addDamage(upkeepDamage, c);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
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
    private static Ability upkeepAIPayment(final Card c, final String cost) {
        return new AbilityStatic(c, cost) {
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
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList the = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "The Abyss");
        final CardList magus = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Magus of the Abyss");

        final CardList cards = new CardList();
        cards.addAll(the);
        cards.addAll(magus);

        for (final Card c : cards) {
            final Card abyss = c;

            final CardList abyssGetTargets = AllZoneUtil.getCreaturesInPlay(player)
                    .filter(CardListFilter.NON_ARTIFACTS);

            final Ability sacrificeCreature = new Ability(abyss, "") {
                @Override
                public void resolve() {
                    final CardList targets = abyssGetTargets.getTargetableCards(this);
                    if (player.isHuman()) {
                        if (targets.size() > 0) {
                            AllZone.getInputControl().setInput(new Input() {
                                private static final long serialVersionUID = 4820011040853968644L;

                                @Override
                                public void showMessage() {
                                    CMatchUI.SINGLETON_INSTANCE
                                            .showMessage(
                                                    abyss.getName() + " - Select one nonartifact creature to destroy");
                                    ButtonUtil.disableAll();
                                }

                                @Override
                                public void selectCard(final Card selected, final PlayerZone zone) {
                                    // probably need to restrict by controller
                                    // also
                                    if (targets.contains(selected)) {
                                        Singletons.getModel().getGameAction().destroyNoRegeneration(selected);
                                        this.stop();
                                    }
                                } // selectCard()
                            }); // Input
                        }
                    } else { // computer

                        final CardList indestruct = targets.getKeyword("Indestructible");
                        if (indestruct.size() > 0) {
                            Singletons.getModel().getGameAction().destroyNoRegeneration(indestruct.get(0));
                        } else if (targets.size() > 0) {
                            final Card target = CardFactoryUtil.getWorstCreatureAI(targets);
                            if (null == target) {
                                // must be nothing valid to destroy
                            } else {
                                Singletons.getModel().getGameAction().destroyNoRegeneration(target);
                            }
                        }
                    }
                } // resolve
            }; // sacrificeCreature

            final StringBuilder sb = new StringBuilder();
            sb.append(abyss.getName()).append(" - destroy a nonartifact creature of your choice.");
            sacrificeCreature.setStackDescription(sb.toString());
            AllZone.getStack().addSimultaneousStackEntry(sacrificeCreature);

        } // end for
    } // The Abyss

    /**
     * <p>
     * upkeepYawgmothDemon.
     * </p>
     */
    private static void upkeepYawgmothDemon() {
        /*
         * At the beginning of your upkeep, you may sacrifice an artifact. If
         * you don't, tap Yawgmoth Demon and it deals 2 damage to you.
         */
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList cards = player.getCardsIn(ZoneType.Battlefield, "Yawgmoth Demon");

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);

            final Ability sacrificeArtifact = new Ability(c, "") {
                @Override
                public void resolve() {
                    final CardList artifacts = player.getCardsIn(ZoneType.Battlefield).filter(CardListFilter.ARTIFACTS);

                    if (player.isHuman()) {
                        AllZone.getInputControl().setInput(new Input() {
                            private static final long serialVersionUID = -1698502376924356936L;

                            @Override
                            public void showMessage() {
                                CMatchUI.SINGLETON_INSTANCE
                                        .showMessage(
                                                "Yawgmoth Demon - Select one artifact to sacrifice or be dealt 2 damage");
                                ButtonUtil.enableOnlyCancel();
                            }

                            @Override
                            public void selectButtonCancel() {
                                tapAndDamage(player);
                                this.stop();
                            }

                            @Override
                            public void selectCard(final Card artifact, final PlayerZone zone) {
                                // probably need to restrict by controller also
                                if (artifact.isArtifact() && zone.is(ZoneType.Battlefield)
                                        && zone.getPlayer().isHuman()) {
                                    Singletons.getModel().getGameAction().sacrifice(artifact, null);
                                    this.stop();
                                }
                            } // selectCard()
                        }); // Input
                    } else { // computer
                        final Card target = CardFactoryUtil.getCheapestPermanentAI(artifacts, this, false);
                        if (null == target) {
                            this.tapAndDamage(player);
                        } else {
                            Singletons.getModel().getGameAction().sacrifice(target, null);
                        }
                    }
                } // resolve

                private void tapAndDamage(final Player player) {
                    c.tap();
                    player.addDamage(2, c);
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - sacrifice an artifact or ");
            sb.append(c.getName()).append(" becomes tapped and deals 2 damage to you.");
            sacrificeArtifact.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(sacrificeArtifact);

        } // end for
    }

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
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList drops = player.getCardsIn(ZoneType.Battlefield, "Drop of Honey");
        drops.addAll(player.getCardsIn(ZoneType.Battlefield, "Porphyry Nodes"));
        final CardList cards = drops;

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);

            final Ability ability = new Ability(c, "") {
                @Override
                public void resolve() {
                    final CardList creatures = AllZoneUtil.getCreaturesInPlay();
                    if (creatures.size() > 0) {
                        CardListUtil.sortAttackLowFirst(creatures);
                        final int power = creatures.get(0).getNetAttack();
                        if (player.isHuman()) {
                            AllZone.getInputControl().setInput(
                                    CardFactoryUtil.inputDestroyNoRegeneration(this.getLowestPowerList(creatures),
                                            "Select creature with power: " + power + " to sacrifice."));
                        } else { // computer
                            final Card compyTarget = this.getCompyCardToDestroy(creatures);
                            Singletons.getModel().getGameAction().destroyNoRegeneration(compyTarget);
                        }
                    }
                } // resolve

                private CardList getLowestPowerList(final CardList original) {
                    final CardList lowestPower = new CardList();
                    final int power = original.get(0).getNetAttack();
                    int i = 0;
                    while ((i < original.size()) && (original.get(i).getNetAttack() == power)) {
                        lowestPower.add(original.get(i));
                        i++;
                    }
                    return lowestPower;
                }

                private Card getCompyCardToDestroy(final CardList original) {
                    final CardList options = this.getLowestPowerList(original);
                    final CardList humanCreatures = options.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return c.getController().isHuman();
                        }
                    });
                    if (humanCreatures.isEmpty()) {
                        options.shuffle();
                        return options.get(0);
                    } else {
                        humanCreatures.shuffle();
                        return humanCreatures.get(0);
                    }
                }
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - destroy 1 creature with lowest power.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

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

        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList cards = player.getCardsIn(ZoneType.Battlefield, "Demonic Hordes");

        for (int i = 0; i < cards.size(); i++) {

            final Card c = cards.get(i);

            final Ability noPay = new Ability(c, "B B B") {
                @Override
                public void resolve() {
                    final CardList playerLand = AllZoneUtil.getPlayerLandsInPlay(player);

                    c.tap();
                    if (c.getController().isComputer()) {
                        if (playerLand.size() > 0) {
                            AllZone.getInputControl().setInput(
                                    PlayerUtil.inputSacrificePermanent(playerLand, c.getName()
                                            + " - Select a land to sacrifice."));
                        }
                    } else {
                        final Card target = CardFactoryUtil.getBestLandAI(playerLand);

                        Singletons.getModel().getGameAction().sacrifice(target, null);
                    }
                } // end resolve()
            }; // end noPay ability

            if (c.getController().isHuman()) {
                final String question = "Pay Demonic Hordes upkeep cost?";
                if (GameActionUtil.showYesNoDialog(c, question)) {
                    final Ability pay = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (AllZone.getZoneOf(c).is(ZoneType.Battlefield)) {
                                final StringBuilder cost = new StringBuilder();
                                cost.append("Pay cost for ").append(c).append("\r\n");
                                GameActionUtil.payManaDuringAbilityResolve(cost.toString(), noPay.getManaCost(),
                                        Command.BLANK, Command.BLANK);
                            }
                        } // end resolve()
                    }; // end pay ability
                    pay.setStackDescription("Demonic Hordes - Upkeep Cost");

                    AllZone.getStack().addSimultaneousStackEntry(pay);

                } // end choice
                else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(c.getName()).append(" - is tapped and you must sacrifice a land of opponent's choice");
                    noPay.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(noPay);

                }
            } // end human
            else { // computer
                if ((c.getController().isComputer() && (ComputerUtil.canPayCost(noPay)))) {
                    final Ability computerPay = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            ComputerUtil.payManaCost(noPay);
                        }
                    };
                    computerPay.setStackDescription("Computer pays Demonic Hordes upkeep cost");

                    AllZone.getStack().addSimultaneousStackEntry(computerPay);

                } else {
                    AllZone.getStack().addSimultaneousStackEntry(noPay);

                }
            } // end computer

        } // end for loop

    } // upkeepDemonicHordes

    // ///////////////////////
    // Start of Kinship cards
    // ///////////////////////

    /**
     * <p>
     * upkeepInkDissolver.
     * </p>
     */
    private static void upkeepInkDissolver() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final Player opponent = player.getOpponent();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Ink Dissolver");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantToMillOpponent = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and opponent puts the top 3 ");
                            question.append("cards of his library into his graveyard?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToMillOpponent = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantToMillOpponent = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantToMillOpponent) {
                        opponent.mill(3);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Ink Dissolver - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepInkDissolver()

    /**
     * <p>
     * upkeepKithkinZephyrnaut.
     * </p>
     */
    private static void upkeepKithkinZephyrnaut() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Kithkin Zephyrnaut");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantKithkinBuff = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card, Kithkin Zephyrnaut gets +2/+2 and ");
                            question.append("gains flying and vigilance until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantKithkinBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantKithkinBuff = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantKithkinBuff) {
                        k.addTempAttackBoost(2);
                        k.addTempDefenseBoost(2);
                        k.addExtrinsicKeyword("Flying");
                        k.addExtrinsicKeyword("Vigilance");

                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = 213717084767008154L;

                            @Override
                            public void execute() {
                                k.addTempAttackBoost(-2);
                                k.addTempDefenseBoost(-2);
                                k.removeExtrinsicKeyword("Flying");
                                k.removeExtrinsicKeyword("Vigilance");
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Kithkin Zephyrnaut - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepKithkinZephyrnaut()

    /**
     * <p>
     * upkeepLeafCrownedElder.
     * </p>
     */
    private static void upkeepLeafCrownedElder() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Leaf-Crowned Elder");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantToPlayCard = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal and play this card without paying its mana cost?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToPlayCard = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantToPlayCard = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantToPlayCard) {
                        if (player.isHuman()) {
                            final Card c = library.get(0);
                            Singletons.getModel().getGameAction().playCardNoCost(c);
                        }
                        // player isComputer()
                        else {
                            final Card c = library.get(0);
                            final ArrayList<SpellAbility> choices = c.getBasicSpells();

                            for (final SpellAbility sa : choices) {
                                if (sa.canPlayAI()) {
                                    ComputerUtil.playStackFree(sa);
                                    break;
                                }
                            }
                        }
                    } // wantToPlayCard
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Leaf-Crowned Elder - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepLeafCrownedElder()

    /**
     * <p>
     * upkeepMudbuttonClanger.
     * </p>
     */
    private static void upkeepMudbuttonClanger() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Mudbutton Clanger");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantGoblinBuff = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and Mudbutton Clanger gets +1/+1 until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantGoblinBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantGoblinBuff = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantGoblinBuff) {
                        k.addTempAttackBoost(1);
                        k.addTempDefenseBoost(1);

                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -103560515951630426L;

                            @Override
                            public void execute() {
                                k.addTempAttackBoost(-1);
                                k.addTempDefenseBoost(-1);
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Mudbutton Clanger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepMudbuttonClanger()

    /**
     * <p>
     * upkeepNightshadeSchemers.
     * </p>
     */
    private static void upkeepNightshadeSchemers() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Nightshade Schemers");
        final Player opponent = player.getOpponent();

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantOpponentLoseLife = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and opponent loses 2 life?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantOpponentLoseLife = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantOpponentLoseLife = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }
                    if (wantOpponentLoseLife) {
                        opponent.loseLife(2, k);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Nightshade Schemers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepNightshadeSchemers()

    /**
     * <p>
     * upkeepPyroclastConsul.
     * </p>
     */
    private static void upkeepPyroclastConsul() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Pyroclast Consul");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantDamageCreatures = false;
                    final String[] smallCreatures = { "Creature.toughnessLE2" };

                    CardList humanCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    humanCreatures = humanCreatures.getValidCards(smallCreatures, k.getController(), k);
                    humanCreatures = humanCreatures.getNotKeyword("Indestructible");

                    CardList computerCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    computerCreatures = computerCreatures.getValidCards(smallCreatures, k.getController(), k);
                    computerCreatures = computerCreatures.getNotKeyword("Indestructible");

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and Pyroclast Consul deals 2 damage to each creature?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantDamageCreatures = true;
                            }
                        }
                        // player isComputer()
                        else {
                            if (humanCreatures.size() > computerCreatures.size()) {
                                final String title = "Computer reveals";
                                this.revealTopCard(title);
                                wantDamageCreatures = true;
                            }
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantDamageCreatures) {
                        final CardList allCreatures = AllZoneUtil.getCreaturesInPlay();
                        for (final Card crd : allCreatures) {
                            crd.addDamage(2, k);
                        }
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Pyroclast Consul - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepPyroclastConsul()

    /**
     * <p>
     * upkeepSensationGorger.
     * </p>
     */
    private static void upkeepSensationGorger() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Sensation Gorger");
        final Player opponent = player.getOpponent();

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    final PlayerZone hand = player.getZone(ZoneType.Hand);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantDiscardThenDraw = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and have both players discard their hand and draw 4 cards?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantDiscardThenDraw = true;
                            }
                        }
                        // player isComputer()
                        else {
                            if ((library.size() > 4) && (hand.size() < 2)) {
                                final String title = "Computer reveals";
                                this.revealTopCard(title);
                                wantDiscardThenDraw = true;
                            }
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }
                    if (wantDiscardThenDraw) {
                        player.discardHand(this);
                        opponent.discardHand(this);

                        player.drawCards(4);
                        opponent.drawCards(4);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Sensation Gorger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepSensationGorger()

    /**
     * <p>
     * upkeepSqueakingPieGrubfellows.
     * </p>
     */
    private static void upkeepSqueakingPieGrubfellows() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Squeaking Pie Grubfellows");
        final Player opponent = player.getOpponent();

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantOpponentDiscard = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and have opponent discard a card?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantOpponentDiscard = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantOpponentDiscard = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantOpponentDiscard) {
                        opponent.discard(this);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Squeaking Pie Grubfellows - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepSqueakingPieGrubfellows()

    /**
     * <p>
     * upkeepWanderingGraybeard.
     * </p>
     */
    private static void upkeepWanderingGraybeard() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Wandering Graybeard");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantGainLife = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and gain 4 life?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantGainLife = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantGainLife = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }
                    if (wantGainLife) {
                        player.gainLife(4, k);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Wandering Graybeard - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepWanderingGraybeard()

    /**
     * <p>
     * upkeepWaterspoutWeavers.
     * </p>
     */
    private static void upkeepWaterspoutWeavers() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Waterspout Weavers");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantMerfolkBuff = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and each creature you ");
                            question.append("control gains flying until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantMerfolkBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantMerfolkBuff = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantMerfolkBuff) {
                        final CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                        for (int i = 0; i < creatures.size(); i++) {
                            if (!creatures.get(i).hasKeyword("Flying")) {
                                creatures.get(i).addExtrinsicKeyword("Flying");
                            }
                        }
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1978446996943583910L;

                            @Override
                            public void execute() {
                                final CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                                for (int i = 0; i < creatures.size(); i++) {
                                    if (creatures.get(i).hasKeyword("Flying")) {
                                        creatures.get(i).removeExtrinsicKeyword("Flying");
                                    }
                                }
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Waterspout Weavers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepWaterspoutWeavers()

    /**
     * <p>
     * upkeepWinnowerPatrol.
     * </p>
     */
    private static void upkeepWinnowerPatrol() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Winnower Patrol");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantCounter = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and put a +1/+1 counter on Winnower Patrol?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantCounter = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantCounter = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }
                    if (wantCounter) {
                        k.addCounter(Counters.P1P1, 1);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Winnower Patrol - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeepWinnowerPatrol()

    /**
     * <p>
     * upkeepWolfSkullShaman.
     * </p>
     */
    private static void upkeepWolfSkullShaman() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList kinship = player.getCardsIn(ZoneType.Battlefield, "Wolf-Skull Shaman");

        final PlayerZone library = player.getZone(ZoneType.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(ZoneType.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantToken = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid("Card.sharesCreatureTypeWith", k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and put a 2/2 green "
                                    + "Wolf creature token onto the battlefield?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToken = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantToken = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantToken) {
                        CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", k.getController(), "G", new String[] {
                                "Creature", "Wolf" }, 2, 2, new String[] { "" });
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.chooseOne(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Wolf-Skull Shaman - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Wolf_Skull_Shaman()

    // /////////////////////
    // End of Kinship cards
    // /////////////////////

    /**
     * <p>
     * upkeepSuspend.
     * </p>
     */
    private static void upkeepSuspend() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        CardList list = player.getCardsIn(ZoneType.Exile);

        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.hasSuspend();
            }
        });

        if (list.size() == 0) {
            return;
        }

        for (final Card c : list) {
            final int counters = c.getCounters(Counters.TIME);
            if (counters > 0) {
                c.subtractCounter(Counters.TIME, 1);
            }
        }
    } // suspend

    /**
     * <p>
     * upkeepVanishing.
     * </p>
     */
    private static void upkeepVanishing() {

        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        CardList list = player.getCardsIn(ZoneType.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return CardFactoryUtil.hasKeyword(c, "Vanishing") != -1;
            }
        });
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                final Ability ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        card.subtractCounter(Counters.TIME, 1);
                    }
                }; // ability

                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Vanishing - remove a time counter from it. ");
                sb.append("When the last is removed, sacrifice it.)");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * upkeepFading.
     * </p>
     */
    private static void upkeepFading() {

        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        CardList list = player.getCardsIn(ZoneType.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return CardFactoryUtil.hasKeyword(c, "Fading") != -1;
            }
        });
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                final Ability ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        final int fadeCounters = card.getCounters(Counters.FADE);
                        if (fadeCounters <= 0) {
                            Singletons.getModel().getGameAction().sacrifice(card, null);
                        } else {
                            card.subtractCounter(Counters.FADE, 1);
                        }
                    }
                }; // ability

                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Fading - remove a fade counter from it. ");
                sb.append("If you can't, sacrifice it.)");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * upkeepOathOfDruids.
     * </p>
     */
    private static void upkeepOathOfDruids() {
        final CardList oathList = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Oath of Druids");
        if (oathList.isEmpty()) {
            return;
        }

        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Card oath = oathList.get(i);
                final Ability ability = new Ability(oath, "0") {
                    @Override
                    public void resolve() {
                        final CardList libraryList = player.getCardsIn(ZoneType.Library);
                        final PlayerZone battlefield = player.getZone(ZoneType.Battlefield);
                        boolean oathFlag = true;

                        if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0) {
                            if (player.isHuman()) {
                                final StringBuilder question = new StringBuilder();
                                question.append("Reveal cards from the top of your library and place ");
                                question.append("the first creature revealed onto the battlefield?");
                                if (!GameActionUtil.showYesNoDialog(oath, question.toString())) {
                                    oathFlag = false;
                                }
                            } else { // if player == Computer
                                final CardList creaturesInLibrary = player.getCardsIn(ZoneType.Library).getType("Creature");
                                final CardList creaturesInBattlefield = player.getCardsIn(ZoneType.Battlefield).getType(
                                        "Creature");

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
                                final CardList cardsToReveal = new CardList();
                                final int max = libraryList.size();
                                for (int i = 0; i < max; i++) {
                                    final Card c = libraryList.get(i);
                                    cardsToReveal.add(c);
                                    if (c.isCreature()) {
                                        Singletons.getModel().getGameAction().moveTo(battlefield, c);
                                        break;
                                    } else {
                                        Singletons.getModel().getGameAction().moveToGraveyard(c);
                                    }
                                } // for loop
                                if (cardsToReveal.size() > 0) {
                                    GuiUtils.chooseOne("Revealed cards", cardsToReveal.toArray());
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

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    } // upkeepOathOfDruids()

    /**
     * <p>
     * upkeepOathOfGhouls.
     * </p>
     */
    private static void upkeepOathOfGhouls() {
        final CardList oathList = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Oath of Ghouls");
        if (oathList.isEmpty()) {
            return;
        }

        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        if (AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Ability ability = new Ability(oathList.get(0), "0") {
                    @Override
                    public void resolve() {
                        final CardList graveyardCreatures = player.getCardsIn(ZoneType.Graveyard).getType("Creature");

                        if (AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0) {
                            if (player.isHuman()) {
                                final Object o = GuiUtils.chooseOneOrNone("Pick a creature to return to hand",
                                        graveyardCreatures.toArray());
                                if (o != null) {
                                    final Card card = (Card) o;

                                    Singletons.getModel().getGameAction().moveToHand(card);
                                }
                            } else if (player.isComputer()) {
                                final Card card = graveyardCreatures.get(0);

                                Singletons.getModel().getGameAction().moveToHand(card);
                            }
                        }
                    }
                }; // Ability

                final StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, Oath of Ghouls returns a creature ");
                sb.append("from their graveyard to owner's hand if they have more than an opponent.");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    } // Oath of Ghouls

    /**
     * <p>
     * upkeepKarma.
     * </p>
     */
    private static void upkeepKarma() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList karmas = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Karma");
        final CardList swamps = player.getCardsIn(ZoneType.Battlefield).getType("Swamp");

        // determine how much damage to deal the current player
        final int damage = swamps.size();

        // if there are 1 or more Karmas on the
        // battlefield have each of them deal damage.
        if (0 < karmas.size()) {
            for (final Card karma : karmas) {
                final Card src = karma;
                final Ability ability = new Ability(src, "0") {
                    @Override
                    public void resolve() {
                        if (damage > 0) {
                            player.addDamage(damage, src);
                        }
                    }
                }; // Ability
                if (damage > 0) {

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Karma deals ").append(damage).append(" damage to ").append(player);
                    ability.setStackDescription(sb.toString());
                    ability.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

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
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Power Surge");
        final int damage = player.getNumPowerSurgeLands();

        for (final Card surge : list) {
            final Card source = surge;
            final Ability ability = new Ability(source, "0") {
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
                AllZone.getStack().addSimultaneousStackEntry(ability);
            }
        } // for
    } // upkeepPowerSurge()

    /**
     * <p>
     * upkeepVesuvanDoppelgangerKeyword.
     * </p>
     */
    private static void upkeepVesuvanDoppelgangerKeyword() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final String keyword1 = "At the beginning of your upkeep, you may have this "
                + "creature become a copy of target creature except it doesn't copy that "
                + "creature's color. If you do, this creature gains this ability.";
        final String keyword2 = "At the beginning of your upkeep, you may have this "
                + "creature become a copy of another target creature. If you do, "
                + "this creature gains this ability.";
        CardList list = player.getCardsIn(ZoneType.Battlefield);
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.hasAnyKeyword(new String[] {keyword1, keyword2});
            }
        });

        for (final Card c : list) {
            final String keyword = c.hasKeyword(keyword1) ? keyword1 : keyword2;
            final SpellAbility ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final StringBuilder question = new StringBuilder();
                    question.append("Use triggered ability of ").append(c).append("?");
                    question.append("\r\n").append("(").append(keyword).append(")").append("\r\n");
                    if (GameActionUtil.showYesNoDialog(c, question.toString(), true)) {
                        final Card[] newTarget = new Card[1];
                        newTarget[0] = null;

                        final Ability switchTargets = new Ability(c, "0") {
                            @Override
                            public void resolve() {

                                if (newTarget[0] != null) {
                                    /*
                                     * 1. need to select new card - DONE 1a.
                                     * need to create the newly copied card with
                                     * pic and setinfo 2. need to add the leaves
                                     * play command 3. need to transfer the
                                     * keyword 4. need to update the clone
                                     * origin of new card and old card 5. remove
                                     * clone leaves play commands from old 5a.
                                     * remove old from play 6. add new to play
                                     */

                                    final Card newCopy = AllZone.getCardFactory().getCard(
                                            newTarget[0].getState(CardCharactersticName.Original).getName(), player);
                                    newCopy.setCurSetCode(newTarget[0].getCurSetCode());
                                    // preserve the image of the Vesuvan
                                    // Doppelganger/whatever the source is
                                    newCopy.setImageFilename(c.getImageFilename());

                                    AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);
                                    newCopy.setState(newTarget[0].getCurState());
                                    AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);

                                    CardFactoryUtil.copyCharacteristics(newCopy, c);

                                    c.addExtrinsicKeyword(keyword);
                                    if (c.hasKeyword(keyword1)) {
                                        c.addColor("U");
                                    }
                                }
                            }
                        };

                        AllZone.getInputControl().setInput(new Input() {
                            private static final long serialVersionUID = 5662272658873063221L;

                            @Override
                            public void showMessage() {
                                CMatchUI.SINGLETON_INSTANCE
                                        .showMessage(c.getName() + " - Select target creature.");
                                ButtonUtil.enableOnlyCancel();
                            }

                            @Override
                            public void selectButtonCancel() {
                                this.stop();
                            }

                            @Override
                            public void selectCard(final Card selectedCard, final PlayerZone z) {
                                if (z.is(ZoneType.Battlefield) && selectedCard.isCreature()
                                        && selectedCard.canBeTargetedBy(switchTargets)) {
                                    newTarget[0] = selectedCard;
                                    final StringBuilder sb = new StringBuilder();
                                    sb.append(c).append(" - switching to copy ");
                                    sb.append(selectedCard.getName()).append(".");
                                    switchTargets.setStackDescription(sb.toString());
                                    AllZone.getStack().add(switchTargets);
                                    this.stop();
                                }
                            }
                        });
                    }
                }
            };

            ability.setDescription(keyword);
            ability.setStackDescription("(OPTIONAL) " + keyword);

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // foreach(Card)
    } // upkeepVesuvanDoppelgangerKeyword

    /**
     * <p>
     * upkeepTangleWire.
     * </p>
     */
    private static void upkeepTangleWire() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final CardList wires = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Tangle Wire");

        for (final Card source : wires) {
            final SpellAbility ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    final int num = source.getCounters(Counters.FADE);
                    final CardList list = player.getCardsIn(ZoneType.Battlefield).filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return (c.isArtifact() || c.isLand() || c.isCreature()) && c.isUntapped();
                        }
                    });

                    for (int i = 0; i < num; i++) {
                        if (player.isComputer()) {
                            final Card toTap = CardFactoryUtil.getWorstPermanentAI(list, false, false, false, false);
                            if (null != toTap) {
                                toTap.tap();
                                list.remove(toTap);
                            }
                        } else {
                            AllZone.getInputControl().setInput(new Input() {
                                private static final long serialVersionUID = 5313424586016061612L;

                                @Override
                                public void showMessage() {
                                    if (list.size() == 0) {
                                        this.stop();
                                        return;
                                    }
                                    CMatchUI.SINGLETON_INSTANCE
                                            .showMessage(
                                                    source.getName()
                                                            + " - Select "
                                                            + num
                                                            + " untapped artifact(s), creature(s), or land(s) you control");
                                    ButtonUtil.disableAll();
                                }

                                @Override
                                public void selectCard(final Card card, final PlayerZone zone) {
                                    if (zone.is(ZoneType.Battlefield, AllZone.getHumanPlayer())
                                            && list.contains(card)) {
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

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // foreach(wire)
    } // upkeepTangleWire()

    /**
     * <p>
     * upkeepBlazeCounters.
     * </p>
     */
    private static void upkeepBlazeCounters() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        CardList blaze = player.getCardsIn(ZoneType.Battlefield);
        blaze = blaze.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.isLand() && (c.getCounters(Counters.BLAZE) > 0);
            }
        });

        for (int i = 0; i < blaze.size(); i++) {
            final Card source = blaze.get(i);
            final Ability ability = new Ability(blaze.get(i), "0") {
                @Override
                public void resolve() {
                    player.addDamage(1, source);
                }
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(blaze.get(i)).append(" - has a blaze counter and deals 1 damage to ");
            sb.append(player).append(".");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }
    }

    /**
     * <p>
     * upkeepCarnophage.
     * </p>
     */
    /*private static void upkeepCarnophage() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        final CardList list = player.getCardsIn(Zone.Battlefield, "Carnophage");
        if (player.isHuman()) {
            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                final String[] choices = { "Yes", "No" };
                final Object choice = GuiUtils.getChoice("Pay Carnophage's upkeep?", choices);
                if (choice.equals("Yes")) {
                    player.loseLife(1, c);
                } else {
                    c.tap();
                }
            }
        } else if (player.isComputer()) {
            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                if (AllZone.getComputerPlayer().getLife() > 1) {
                    player.loseLife(1, c);
                } else {
                    c.tap();
                }
            }
        }
    }*/ // upkeepCarnophage

    /**
     * <p>
     * upkeepSangrophage.
     * </p>
     */
    /*private static void upkeepSangrophage() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        final CardList list = player.getCardsIn(Zone.Battlefield, "Sangrophage");
        if (player.isHuman()) {
            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                final String[] choices = { "Yes", "No" };
                final Object choice = GuiUtils.getChoice("Pay Sangrophage's upkeep?", choices);
                if (choice.equals("Yes")) {
                    player.loseLife(2, c);
                } else {
                    c.tap();
                }
            }
        } else if (player.isComputer()) {
            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                if (AllZone.getComputerPlayer().getLife() > 2) {
                    player.loseLife(2, c);
                } else {
                    c.tap();
                }
            }
        }
    }*/ // upkeepSangrophage

    /**
     * <p>
     * upkeepCurseOfMisfortunes.
     * </p>
     */
    private static void upkeepCurseOfMisfortunes() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();

        final CardList misfortunes = player.getCardsIn(ZoneType.Battlefield, "Curse of Misfortunes");

        for (int i = 0; i < misfortunes.size(); i++) {
            final Card source = misfortunes.get(i);

            final Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    CardList enchantmentsInLibrary = source.getController().getCardsIn(ZoneType.Library);
                    final CardList enchantmentsAttached = new CardList(source.getEnchantingPlayer().getEnchantedBy());
                    enchantmentsInLibrary = enchantmentsInLibrary.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return (c.isEnchantment() && c.hasKeyword("Enchant player")
                                    && !source.getEnchantingPlayer().hasProtectionFrom(c)
                                    && !enchantmentsAttached.containsName(c));
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
                        final Object check = GuiUtils.chooseOneOrNone("Select Curse to attach", target);
                        if (check != null) {
                            enchantment = ((Card) check);
                        }
                    } else {
                        enchantment = CardFactoryUtil.getBestEnchantmentAI(enchantmentsInLibrary, this, false);
                    }
                    if (enchantment != null) {
                        GameAction.changeZone(AllZone.getZoneOf(enchantment),
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
            AllZone.getStack().addSimultaneousStackEntry(ability);
        }
    } // upkeepCurseOfMisfortunes

} // end class Upkeep
