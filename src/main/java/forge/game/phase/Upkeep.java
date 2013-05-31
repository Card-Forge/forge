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

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.CounterType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.player.HumanPlay;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.input.InputPayManaExecuteCommands;
import forge.gui.input.InputSelectCards;
import forge.gui.input.InputSelectCardsFromList;

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

    public Upkeep(final Game game) { super(game); }
    
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
         
        game.getStack().freezeStack();
        Upkeep.upkeepBraidOfFire(game);

        Upkeep.upkeepUpkeepCost(game); // sacrifice unless upkeep cost is paid
        Upkeep.upkeepEcho(game);

        Upkeep.upkeepTheAbyss(game);
        Upkeep.upkeepDropOfHoney(game);
        //Upkeep.upkeepDemonicHordes(game);
        Upkeep.upkeepTangleWire(game);

        Upkeep.upkeepOathOfDruids(game);
        Upkeep.upkeepOathOfGhouls(game);
        //Upkeep.upkeepSuspend(game);
        Upkeep.upkeepVanishing(game);
        Upkeep.upkeepFading(game);
        Upkeep.upkeepBlazeCounters(game);
        Upkeep.upkeepPowerSurge(game);

        game.getStack().unfreezeStack();
    }

    // UPKEEP CARDS:

    /**
     * <p>
     * upkeepBraidOfFire.
     * </p>
     */
    private static void upkeepBraidOfFire(final Game game) {
        final Player player = game.getPhaseHandler().getPlayerTurn();

        final List<Card> braids = player.getCardsIn(ZoneType.Battlefield, "Braid of Fire");

        for (int i = 0; i < braids.size(); i++) {
            final Card c = braids.get(i);

            final StringBuilder sb = new StringBuilder();
            sb.append("Cumulative Upkeep for ").append(c).append("\n");
            final Ability upkeepAbility = new Ability(c, ManaCost.ZERO) {
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
                    if( player.getController().confirmAction(this, PlayerActionConfirmMode.BraidOfFire, sb.toString())) {
                        abMana.produceMana(this);
                    } else {
                        game.getAction().sacrifice(c, null);
                    }

                }
            };
            upkeepAbility.setActivatingPlayer(c.getController());
            upkeepAbility.setStackDescription(sb.toString());
            upkeepAbility.setDescription(sb.toString());

            game.getStack().addSimultaneousStackEntry(upkeepAbility);

        }
    } // upkeepBraidOfFire

    /**
     * <p>
     * upkeepEcho.
     * </p>
     */
    private static void upkeepEcho(final Game game) {
        List<Card> list = game.getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield);
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasStartOfKeyword("(Echo unpaid)");
            }
        });

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            if (c.hasStartOfKeyword("(Echo unpaid)")) {
                final Ability blankAbility = Upkeep.getBlankAbility(c, c.getEchoCost());
                blankAbility.setActivatingPlayer(c.getController());

                final StringBuilder sb = new StringBuilder();
                sb.append("Echo for ").append(c).append("\n");

                final Ability sacAbility = new Ability(c, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        
                        Player controller = c.getController();
                        if (controller.isHuman()) {
                            Cost cost = new Cost(c.getEchoCost().trim(), true);
                            if ( !HumanPlay.payCostDuringAbilityResolve(blankAbility, cost, null, game) )
                                game.getAction().sacrifice(c, null);;

                        } else { // computer
                            if (ComputerUtilCost.canPayCost(blankAbility, controller)) {
                                ComputerUtil.playNoStack(controller, blankAbility, game);
                            } else {
                                game.getAction().sacrifice(c, null);
                            }
                        }
                    }
                };
                sacAbility.setActivatingPlayer(c.getController());
                sacAbility.setStackDescription(sb.toString());
                sacAbility.setDescription(sb.toString());

                game.getStack().addSimultaneousStackEntry(sacAbility);

                c.removeAllExtrinsicKeyword("(Echo unpaid)");
            }
        }
    } // echo

    /**
     * <p>
     * upkeepUpkeepCost.
     * </p>
     */
    private static void upkeepUpkeepCost(final Game game) {
        
        final List<Card> list = game.getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield);

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            final Player controller = c.getController();
            for (String ability : c.getKeyword()) {

                // destroy
                if (ability.startsWith("At the beginning of your upkeep, destroy CARDNAME")) {
                    final String[] k = ability.split(" pay ");
                    final ManaCost upkeepCost = new ManaCost(new ManaCostParser(k[1]));

                    final String sb = "Upkeep for " + c;
                    final Ability upkeepAbility = new Ability(c, ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            final boolean isUpkeepPaid;
                            if (controller.isHuman()) {
                                InputPayManaExecuteCommands inp = new InputPayManaExecuteCommands(controller, sb, upkeepCost);
                                Singletons.getControl().getInputQueue().setInputAndWait(inp);
                                isUpkeepPaid = inp.isPaid();
                            } else { // computer
                                Ability aiPaid = Upkeep.getBlankAbility(c, upkeepCost.toString());
                                isUpkeepPaid = ComputerUtilCost.canPayCost(aiPaid, controller) && !c.hasKeyword("Indestructible"); 
                                if (isUpkeepPaid) {
                                    ComputerUtil.playNoStack(controller, aiPaid, game);
                                }
                            }
                            if( !isUpkeepPaid ) {
                                if (c.getName().equals("Cosmic Horror")) {
                                    controller.addDamage(7, c);
                                }
                                game.getAction().destroy(c, this);
                            }
                            
                        }
                    };
                    upkeepAbility.setActivatingPlayer(controller);
                    upkeepAbility.setStackDescription(sb);
                    upkeepAbility.setDescription(sb);

                    game.getStack().addSimultaneousStackEntry(upkeepAbility);
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
                    final Ability blankAbility = Upkeep.getBlankAbility(c, upkeepCost);
                    blankAbility.setActivatingPlayer(controller);

                    final Ability upkeepAbility = new Ability(c, ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                if ( !HumanPlay.payCostDuringAbilityResolve(blankAbility, blankAbility.getPayCosts(), this, game))
                                    game.getAction().sacrifice(c, null);
                            } else { // computer
                                if (ComputerUtilCost.shouldPayCost(controller, c, upkeepCost) && ComputerUtilCost.canPayCost(blankAbility, controller)) {
                                    ComputerUtil.playNoStack(controller, blankAbility, game); // this makes AI pay
                                } else {
                                    game.getAction().sacrifice(c, null);
                                }
                            }
                        }
                    };
                    upkeepAbility.setActivatingPlayer(controller);
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    game.getStack().addSimultaneousStackEntry(upkeepAbility);
                } // sacrifice

                // destroy
                if (ability.startsWith("At the beginning of your upkeep, CARDNAME deals ")) {
                    final String[] k = ability.split("deals ");
                    final String s1 = k[1].substring(0, 2);
                    final int upkeepDamage = Integer.parseInt(s1.trim());
                    final String[] l = k[1].split(" pay ");
                    final ManaCost upkeepCost = new ManaCost(new ManaCostParser(l[1]));

                    final String sb = "Damage upkeep for " + c;
                    final Ability upkeepAbility = new Ability(c, ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            boolean isUpkeepPaid = false;
                            if (controller.isHuman()) {
                                InputPayManaExecuteCommands inp = new InputPayManaExecuteCommands(controller, sb, upkeepCost);
                                Singletons.getControl().getInputQueue().setInputAndWait(inp);
                                isUpkeepPaid = inp.isPaid();
                            } else { // computers
                                final Ability aiPaid = Upkeep.getBlankAbility(c, upkeepCost.toString());
                                if (ComputerUtilCost.canPayCost(aiPaid, controller) && ComputerUtilCombat.predictDamageTo(controller, upkeepDamage, c, false) > 0) {
                                    ComputerUtil.playNoStack(controller, aiPaid, game);
                                    isUpkeepPaid = true;
                                }
                            }
                            if (!isUpkeepPaid) {
                                controller.addDamage(upkeepDamage, c);
                            }
                        }
                    };
                    upkeepAbility.setActivatingPlayer(controller);
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    game.getStack().addSimultaneousStackEntry(upkeepAbility);
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
    public static Ability getBlankAbility(final Card c, final String costString) {
        return new AbilityStatic(c, new Cost(costString, true), null) {
            @Override
            public void resolve() {}
        };
    }

    /**
     * <p>
     * upkeepTheAbyss.
     * </p>
     */
    private static void upkeepTheAbyss(final Game game) {
        /*
         * At the beginning of each player's upkeep, destroy target nonartifact
         * creature that player controls of his or her choice. It can't be
         * regenerated.
         */
        final Player player = game.getPhaseHandler().getPlayerTurn();
        final List<Card> the = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("The Abyss"));
        final List<Card> magus = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Magus of the Abyss"));

        final List<Card> cards = new ArrayList<Card>();
        cards.addAll(the);
        cards.addAll(magus);

        for (final Card c : cards) {
            final Card abyss = c;

            final Ability sacrificeCreature = new Ability(abyss, ManaCost.NO_COST) {
                @Override
                public void resolve() {
                    final List<Card> targets = CardLists.getTargetableCards(CardLists.filter(player.getCreaturesInPlay(), Presets.NON_ARTIFACTS), this);
                    if (player.isHuman() && targets.size() > 0) {
                        final InputSelectCards chooseArt = new InputSelectCards(1, 1) {
                            private static final long serialVersionUID = 4820011040853968644L;
                            @Override
                            protected boolean isValidChoice(Card choice) {
                                return choice.isCreature() && !choice.isArtifact() && canTarget(choice) && choice.getController() == player;
                            };
                        };
                        chooseArt.setMessage(abyss.getName() + " - Select one nonartifact creature to destroy");
                        Singletons.getControl().getInputQueue().setInputAndWait(chooseArt); // Input
                        if (!chooseArt.hasCancelled()) {
                            game.getAction().destroyNoRegeneration(chooseArt.getSelected().get(0), this);
                        }
                        
                    } else { // computer

                        final List<Card> indestruct = CardLists.getKeyword(targets, "Indestructible");
                        if (indestruct.size() > 0) {
                            game.getAction().destroyNoRegeneration(indestruct.get(0), this);
                        } else if (targets.size() > 0) {
                            final Card target = ComputerUtilCard.getWorstCreatureAI(targets);
                            if (null == target) {
                                // must be nothing valid to destroy
                            } else {
                                game.getAction().destroyNoRegeneration(target, this);
                            }
                        }
                    }
                } // resolve
            }; // sacrificeCreature

            final StringBuilder sb = new StringBuilder();
            sb.append(abyss.getName()).append(" - destroy a nonartifact creature of your choice.");
            sacrificeCreature.setActivatingPlayer(c.getController());
            sacrificeCreature.setStackDescription(sb.toString());
            sacrificeCreature.setDescription(sb.toString());
            game.getStack().addAndUnfreeze(sacrificeCreature);
        } // end for
    } // The Abyss

    /**
     * <p>
     * upkeepDropOfHoney.
     * </p>
     */
    private static void upkeepDropOfHoney(final Game game) {
        /*
         * At the beginning of your upkeep, destroy the creature with the least
         * power. It can't be regenerated. If two or more creatures are tied for
         * least power, you choose one of them.
         */
        final Player player = game.getPhaseHandler().getPlayerTurn();
        final List<Card> drops = player.getCardsIn(ZoneType.Battlefield, "Drop of Honey");
        drops.addAll(player.getCardsIn(ZoneType.Battlefield, "Porphyry Nodes"));
        final List<Card> cards = drops;

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);

            final Ability ability = new Ability(c, ManaCost.NO_COST) {
                @Override
                public void resolve() {
                    final List<Card> creatures = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
                    if (creatures.size() > 0) {
                        CardLists.sortByPowerAsc(creatures);
                        final List<Card> lowest = new ArrayList<Card>();
                        final int power = creatures.get(0).getNetAttack();
                        for(Card c : creatures) {
                            if (c.getNetAttack() > power) break;
                            lowest.add(c);
                        }

                        List<Card> toSac = player.getController().choosePermanentsToDestroy(this, 1, 1, lowest, "Select creature with power: " + power + " to destroy.");
                        game.getAction().destroyNoRegeneration(toSac.get(0), this);
                    }
                } // resolve
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - destroy 1 creature with lowest power.");
            ability.setActivatingPlayer(c.getController());
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());

            game.getStack().addSimultaneousStackEntry(ability);

        } // end for
    } // upkeepDropOfHoney()

    /**
     * <p>
     * upkeepDemonicHordes.
     * </p>
     */
    /*
    private static void upkeepDemonicHordes(final GameState game) {

        /*
         * At the beginning of your upkeep, unless you pay BBB, tap Demonic
         * Hordes and sacrifice a land of an opponent's choice.
         *-/

        final Player player = game.getPhaseHandler().getPlayerTurn();
        final List<Card> cards = player.getCardsIn(ZoneType.Battlefield, "Demonic Hordes");

        for (final Card c : cards) {

            final Ability cost = new Ability(c, new ManaCost(new ManaCostParser("B B B"))) {
                @Override
                public void resolve() {
                }
            }; // end cost ability

            final Ability unpaidHordesAb = new Ability(c, ManaCost.ZERO) {
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
                        target = ComputerUtilCard.getBestLandAI(playerLand);
                    }
                    game.getAction().sacrifice(target, null);
                } // end resolve()
            }; // end noPay ability

            final Player cp = c.getController();
            if (cp.isHuman()) {
                final Ability pay = new Ability(c, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        if (game.getZoneOf(c).is(ZoneType.Battlefield)) {
                            InputPayment inp = new InputPayManaExecuteCommands(cp, "Pay Demonic Hordes upkeep cost", cost.getPayCosts().getTotalMana());
                            cp.getGame().getInputQueue().setInputAndWait(inp);
                            if ( !inp.isPaid() ) 
                                unpaidHordesAb.resolve();
                        }
                    } // end resolve()
                }; // end pay ability
                pay.setStackDescription("Demonic Hordes - Upkeep Cost");
                pay.setDescription("Demonic Hordes - Upkeep Cost");

                game.getStack().addSimultaneousStackEntry(pay);
            } // end human
            else { // computer
                unpaidHordesAb.setActivatingPlayer(cp);
                if (ComputerUtilCost.canPayCost(cost, cp)) {
                    final Ability computerPay = new Ability(c, ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            ComputerUtilMana.payManaCost(cp, cost);
                        }
                    };
                    computerPay.setStackDescription("Computer pays Demonic Hordes upkeep cost");

                    game.getStack().addSimultaneousStackEntry(computerPay);
                } else {
                    unpaidHordesAb.setStackDescription("Demonic Hordes - Upkeep Cost");
                    game.getStack().addSimultaneousStackEntry(unpaidHordesAb);

                }
            } // end computer

        } // end for loop

    } // upkeepDemonicHordes
*/
    /**
     * <p>
     * upkeepSuspend.
     * </p>
     */ /*
    private static void upkeepSuspend(final GameState game) {
        final Player player = game.getPhaseHandler().getPlayerTurn();

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
*/
    /**
     * <p>
     * upkeepVanishing.
     * </p>
     */
    private static void upkeepVanishing(final Game game) {

        final Player player = game.getPhaseHandler().getPlayerTurn();
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
                final Ability ability = new Ability(card, ManaCost.ZERO) {
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

                game.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * upkeepFading.
     * </p>
     */
    private static void upkeepFading(final Game game) {

        final Player player = game.getPhaseHandler().getPlayerTurn();
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
                final Ability ability = new Ability(card, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final int fadeCounters = card.getCounters(CounterType.FADE);
                        if (fadeCounters <= 0) {
                            game.getAction().sacrifice(card, null);
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

                game.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * upkeepOathOfDruids.
     * </p>
     */
    private static void upkeepOathOfDruids(final Game game) {
        final List<Card> oathList = CardLists.filter(game
                .getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Oath of Druids"));
        if (oathList.isEmpty()) {
            return;
        }

        final Player player = game.getPhaseHandler().getPlayerTurn();

        if (Game.compareTypeAmountInPlay(player, "Creature") < 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Card oath = oathList.get(i);
                final Ability ability = new Ability(oath, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final List<Card> libraryList = new ArrayList<Card>(player.getCardsIn(ZoneType.Library));
                        final PlayerZone battlefield = player.getZone(ZoneType.Battlefield);
                        boolean oathFlag = true;

                        if (Game.compareTypeAmountInPlay(player, "Creature") < 0) {
                            if (player.isHuman()) {
                                final StringBuilder question = new StringBuilder();
                                question.append("Reveal cards from the top of your library and place ");
                                question.append("the first creature revealed onto the battlefield?");
                                if (!GuiDialog.confirm(oath, question.toString())) {
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
                                        game.getAction().moveTo(battlefield, c);
                                        break;
                                    } else {
                                        game.getAction().moveToGraveyard(c);
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

                game.getStack().addSimultaneousStackEntry(ability);
            }
        }
    } // upkeepOathOfDruids()

    /**
     * <p>
     * upkeepOathOfGhouls.
     * </p>
     */
    private static void upkeepOathOfGhouls(final Game game) {
        final List<Card> oathList = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Oath of Ghouls"));
        if (oathList.isEmpty()) {
            return;
        }

        final Player player = game.getPhaseHandler().getPlayerTurn();

        if (Game.compareTypeAmountInGraveyard(player, "Creature") > 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Ability ability = new Ability(oathList.get(0), ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final List<Card> graveyardCreatures = CardLists.filter(player.getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);

                        if (Game.compareTypeAmountInGraveyard(player, "Creature") > 0) {
                            Card card = null;
                            if (player.isHuman()) {
                                card = GuiChoose.oneOrNone("Pick a creature to return to hand", graveyardCreatures);
                            } else if (player.isComputer()) {
                                card = graveyardCreatures.get(0);
                            }
                            if (card != null)
                                game.getAction().moveToHand(card);
                        }
                    }
                }; // Ability

                final StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, Oath of Ghouls returns a creature ");
                sb.append("from their graveyard to owner's hand if they have more than an opponent.");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());
                ability.setActivatingPlayer(oathList.get(0).getController());

                game.getStack().addSimultaneousStackEntry(ability);

            }
        }
    } // Oath of Ghouls

    /**
     * <p>
     * upkeepPowerSurge.
     * </p>
     */
    private static void upkeepPowerSurge(final Game game) {
        /*
         * At the beginning of each player's upkeep, Power Surge deals X damage
         * to that player, where X is the number of untapped lands he or she
         * controlled at the beginning of this turn.
         */
        final Player player = game.getPhaseHandler().getPlayerTurn();
        final List<Card> list = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Power Surge"));
        final int damage = player.getNumPowerSurgeLands();

        for (final Card surge : list) {
            final Card source = surge;
            final Ability ability = new Ability(source, ManaCost.ZERO) {
                @Override
                public void resolve() {
                    player.addDamage(damage, source);
                }
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(source).append(" - deals ").append(damage).append(" damage to ").append(player);
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());
            ability.setActivatingPlayer(surge.getController());

            if (damage > 0) {
                game.getStack().addSimultaneousStackEntry(ability);
            }
        } // for
    } // upkeepPowerSurge()

    /**
     * <p>
     * upkeepTangleWire.
     * </p>
     */
    private static void upkeepTangleWire(final Game game) {
        final Player player = game.getPhaseHandler().getPlayerTurn();
        final List<Card> wires = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Tangle Wire"));

        for (final Card source : wires) {
            final SpellAbility ability = new Ability(source, ManaCost.ZERO) {
                @Override
                public void resolve() {
                    final int num = source.getCounters(CounterType.FADE);
                    final List<Card> list = new ArrayList<Card>();
                    for( Card c : player.getCardsIn(ZoneType.Battlefield)) {
                        if ((c.isArtifact() || c.isLand() || c.isCreature()) && c.isUntapped())
                            list.add(c);
                    }

                    if (player.isComputer()) {
                        for (int i = 0; i < num; i++) {
                            Card toTap = ComputerUtilCard.getWorstPermanentAI(list, false, false, false, false);
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
                        }
                    } else {
                        InputSelectCards inp = new InputSelectCardsFromList(num, num, list);
                        inp.setMessage(source.getName() + " - Select %d untapped artifact(s), creature(s), or land(s) you control");
                        Singletons.getControl().getInputQueue().setInputAndWait(inp);
                        for(Card crd : inp.getSelected())
                            crd.tap();
                    }
                }
            };
            String message = source.getName() + " - " + player + " taps X artifacts, creatures or lands he or she controls.";
            ability.setStackDescription(message);
            ability.setDescription(message);
            ability.setActivatingPlayer(source.getController());

            game.getStack().addSimultaneousStackEntry(ability);

        } // foreach(wire)
    } // upkeepTangleWire()

    /**
     * <p>
     * upkeepBlazeCounters.
     * </p>
     */
    private static void upkeepBlazeCounters(final Game game) {
        final Player player = game.getPhaseHandler().getPlayerTurn();

        List<Card> blaze = player.getCardsIn(ZoneType.Battlefield);
        blaze = CardLists.filter(blaze, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isLand() && (c.getCounters(CounterType.BLAZE) > 0);
            }
        });

        for (int i = 0; i < blaze.size(); i++) {
            final Card source = blaze.get(i);
            final Ability ability = new Ability(blaze.get(i),ManaCost.ZERO) {
                @Override
                public void resolve() {
                    player.addDamage(1, source);
                }
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(blaze.get(i)).append(" - has a blaze counter and deals 1 damage to ");
            sb.append(player).append(".");
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());
            ability.setActivatingPlayer(source.getController());

            game.getStack().addSimultaneousStackEntry(ability);

        }
    }
} // end class Upkeep
