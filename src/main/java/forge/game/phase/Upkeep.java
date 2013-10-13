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
import forge.card.ability.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.game.Game;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.zone.ZoneType;
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

    protected final Game game;
    public Upkeep(final Game game) { 
        super(PhaseType.UPKEEP);
        this.game = game;
    }
    
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

        Upkeep.upkeepUpkeepCost(game); // sacrifice unless upkeep cost is paid
        Upkeep.upkeepEcho(game);

        Upkeep.upkeepDropOfHoney(game);
        Upkeep.upkeepTangleWire(game);

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
                final StringBuilder sb = new StringBuilder();
                sb.append("Echo for ").append(c).append("\n");
                String ref = "X".equals(c.getEchoCost()) ? " | References$ X" : "";
                String effect = "AB$ Sacrifice | Cost$ 0 | SacValid$ Self | "
                        + "UnlessPayer$ You | UnlessCost$ " + c.getEchoCost()
                        + ref;

                SpellAbility sacAbility = AbilityFactory.getAbility(effect, c);
                sacAbility.setTrigger(true);
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

                // sacrifice
                if (ability.startsWith("At the beginning of your upkeep, sacrifice")) {

                    final StringBuilder sb = new StringBuilder("Sacrifice upkeep for " + c);
                    final String[] k = ability.split(" pay ");

                    String effect = "AB$ Sacrifice | Cost$ 0 | SacValid$ Self"
                            + "| UnlessPayer$ You | UnlessCost$ " + k[1];

                    SpellAbility upkeepAbility = AbilityFactory.getAbility(effect, c);
                    upkeepAbility.setActivatingPlayer(controller);
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());
                    upkeepAbility.setTrigger(true);

                    game.getStack().addSimultaneousStackEntry(upkeepAbility);
                } // sacrifice

                // Cumulative upkeep
                if (ability.startsWith("Cumulative upkeep")) {
                    
                    final StringBuilder sb = new StringBuilder();
                    final String[] k = ability.split(":");
                    sb.append("Cumulative upkeep for " + c);

                    final Ability upkeepAbility = new Ability(c, ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            c.addCounter(CounterType.AGE, 1, true);
                            String cost = CardFactoryUtil.multiplyCost(k[1], c.getCounters(CounterType.AGE));
                            final Cost upkeepCost = new Cost(cost, true);
                            boolean isPaid = controller.getController().payManaOptional(c, upkeepCost, this, sb.toString(), ManaPaymentPurpose.CumulativeUpkeep);
                            final HashMap<String, Object> runParams = new HashMap<String, Object>();
                            runParams.put("CumulativeUpkeepPaid", (Boolean) isPaid);
                            runParams.put("Card", this.getSourceCard());
                            game.getTriggerHandler().runTrigger(TriggerType.PayCumulativeUpkeep, runParams, false);
                            if(!isPaid)
                                game.getAction().sacrifice(c, null);
                        }
                    };
                    sb.append("\n");
                    upkeepAbility.setActivatingPlayer(controller);
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    game.getStack().addSimultaneousStackEntry(upkeepAbility);
                } // Cumulative upkeep
            }

        } // for
    } // upkeepCost

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
                        if (list.size() > num){
                            InputSelectCards inp = new InputSelectCardsFromList(num, num, list);
                            inp.setMessage(source.getName() + " - Select %d untapped artifact(s), creature(s), or land(s) you control");
                            Singletons.getControl().getInputQueue().setInputAndWait(inp);
                            for(Card crd : inp.getSelected())
                                crd.tap();
                        } else {
                            for(Card crd : list)
                                crd.tap();
                        }
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
} // end class Upkeep
