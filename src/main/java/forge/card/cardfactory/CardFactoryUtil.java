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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.CounterType;
import forge.GameEntity;
import forge.Singletons;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.cost.Cost;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCostShard;
import forge.card.mana.ManaCost;
import forge.card.replacement.ReplacementEffect;
import forge.card.replacement.ReplacementHandler;
import forge.card.replacement.ReplacementLayer;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.control.input.InputSelectManyCards;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCost;
import forge.game.event.TokenCreatedEvent;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;
import forge.util.Aggregates;

import forge.view.ButtonUtil;

/**
 * <p>
 * CardFactoryUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryUtil {
    /**
     * <p>
     * inputDestroyNoRegeneration.
     * </p>
     * 
     * @param choices
     *            a {@link forge.CardList} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputDestroyNoRegeneration(final List<Card> choices, final String message) {
        final Input target = new Input() {
            private static final long serialVersionUID = -6637588517573573232L;

            @Override
            public void showMessage() {
                CMatchUI.SINGLETON_INSTANCE.showMessage(message);
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card) {
                if (choices.contains(card)) {
                    Singletons.getModel().getGame().getAction().destroyNoRegeneration(card);
                    this.stop();
                }
            }
        };
        return target;
    } // inputDestroyNoRegeneration()

    /**
     * <p>
     * abilityUnearth.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.AbilityActivated} object.
     */
    public static AbilityActivated abilityUnearth(final Card sourceCard, final String manaCost) {

        final Cost cost = new Cost(sourceCard, manaCost, true);
        class AbilityUnearth extends AbilityActivated {
            public AbilityUnearth(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityUnearth(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactory.copySpellAbility(this, res);
                final SpellAbilityRestriction restrict = new SpellAbilityRestriction();
                restrict.setZone(ZoneType.Graveyard);
                restrict.setSorcerySpeed(true);
                res.setRestrictions(restrict);
                return res;
            }

            private static final long serialVersionUID = -5633945565395478009L;

            @Override
            public void resolve() {
                final Card card = Singletons.getModel().getGame().getAction().moveToPlay(sourceCard);

                card.addIntrinsicKeyword("At the beginning of the end step, exile CARDNAME.");
                card.addIntrinsicKeyword("Haste");
                card.setUnearthed(true);
            }

            @Override
            public boolean canPlayAI() {
                PhaseHandler phase = Singletons.getModel().getGame().getPhaseHandler();
                if (phase.getPhase().isAfter(PhaseType.MAIN1) || !phase.isPlayerTurn(getActivatingPlayer())) {
                    return false;
                }
                return ComputerUtilCost.canPayCost(this, getActivatingPlayer());
            }
        }
        final AbilityActivated unearth = new AbilityUnearth(sourceCard, cost, null);

        final SpellAbilityRestriction restrict = new SpellAbilityRestriction();
        restrict.setZone(ZoneType.Graveyard);
        restrict.setSorcerySpeed(true);
        unearth.setRestrictions(restrict);

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append("Unearth: ").append(sourceCard.getName());
        unearth.setStackDescription(sbStack.toString());

        return unearth;
    } // abilityUnearth()

    /**
     * <p>
     * abilityMorphDown.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityMorphDown(final Card sourceCard) {
        final Spell morphDown = new Spell(sourceCard) {
            private static final long serialVersionUID = -1438810964807867610L;

            @Override
            public void resolve() {
                Card c = Singletons.getModel().getGame().getAction().moveToPlay(sourceCard);
                c.setPreFaceDownCharacteristic(CardCharacteristicName.Original);
            }

            @Override
            public boolean canPlay() {
                //Lands do not have SpellPermanents.
                if (sourceCard.isLand()) {
                    return (Singletons.getModel().getGame().getZoneOf(sourceCard).is(ZoneType.Hand) || sourceCard.hasKeyword("May be played"))
                            && sourceCard.getController().canCastSorcery();
                }
                else {
                    return sourceCard.getSpellPermanent().canPlay();
                }
            }
        };

        morphDown.setManaCost(new ManaCost(new ManaCostParser("3")));
        morphDown.setDescription("(You may cast this face down as a 2/2 creature for 3.)");
        morphDown.setStackDescription("Morph - Creature 2/2");
        morphDown.setCastFaceDown(true);

        return morphDown;
    }

    /**
     * <p>
     * abilityMorphUp.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @return a {@link forge.card.spellability.AbilityActivated} object.
     */
    public static AbilityStatic abilityMorphUp(final Card sourceCard, final Cost cost) {
        final AbilityStatic morphUp = new AbilityStatic(sourceCard, cost, null) {

            @Override
            public void resolve() {
                if (sourceCard.turnFaceUp()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(this.getActivatingPlayer()).append(" has unmorphed ");
                    sb.append(sourceCard.getName());
                    Singletons.getModel().getGame().getGameLog().add("ResolveStack", sb.toString(), 2);
                }
            }

            @Override
            public boolean canPlay() {
                return sourceCard.getController().equals(this.getActivatingPlayer()) && sourceCard.isFaceDown()
                        && sourceCard.isInPlay();
            }

        }; // morph_up

        String costDesc = cost.toString();
        // get rid of the ": " at the end
        costDesc = costDesc.substring(0, costDesc.length() - 2);
        final StringBuilder sb = new StringBuilder();
        sb.append("Morph");
        if (!cost.isOnlyManaCost()) {
            sb.append(" -");
        }
        sb.append(" ").append(costDesc).append(" (Turn this face up any time for its morph cost.)");
        morphUp.setDescription(sb.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" - turn this card face up.");
        morphUp.setStackDescription(sbStack.toString());

        return morphUp;
    }

    /**
     * <p>
     * abilityCycle.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityCycle(final Card sourceCard, String cycleCost) {
        StringBuilder sb = new StringBuilder();
        sb.append("AB$ Draw | Cost$ ");
        sb.append(cycleCost);
        sb.append(" Discard<1/CARDNAME> | ActivationZone$ Hand | PrecostDesc$ Cycling ");
        sb.append("| SpellDescription$ Draw a card.");

        SpellAbility cycle = AbilityFactory.getAbility(sb.toString(), sourceCard);
        cycle.setIsCycling(true);

        return cycle;
    } // abilityCycle()

    /**
     * <p>
     * abilityTypecycle.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityTypecycle(final Card sourceCard, String cycleCost, final String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("AB$ ChangeZone | Cost$ ").append(cycleCost);

        String desc = type;
        if (type.equals("Basic")) {
            desc = "Basic land";
        }

        sb.append(" Discard<1/CARDNAME> | ActivationZone$ Hand | PrecostDesc$ ").append(desc).append("cycling ");
        sb.append("| Origin$ Library | Destination$ Hand |");
        sb.append("ChangeType$ ").append(type);
        sb.append(" | SpellDescription$ Search your library for a ").append(desc).append(" card, reveal it,");
        sb.append(" and put it into your hand. Then shuffle your library.");

        SpellAbility cycle = AbilityFactory.getAbility(sb.toString(), sourceCard);
        cycle.setIsCycling(true);

        return cycle;
    } // abilityTypecycle()

    /**
     * <p>
     * abilityTransmute.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param transmuteCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityTransmute(final Card sourceCard, String transmuteCost) {
        transmuteCost += " Discard<1/CARDNAME>";
        final Cost abCost = new Cost(sourceCard, transmuteCost, true);
        class AbilityTransmute extends AbilityActivated {
            public AbilityTransmute(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityTransmute(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactory.copySpellAbility(this, res);
                res.getRestrictions().setZone(ZoneType.Hand);
                return res;
            }

            private static final long serialVersionUID = -4960704261761785512L;

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean canPlay() {
                return super.canPlay() && sourceCard.getController().canCastSorcery();
            }

            @Override
            public void resolve() {
                final List<Card> cards = sourceCard.getController().getCardsIn(ZoneType.Library);
                final List<Card> sameCost = new ArrayList<Card>();

                for (int i = 0; i < cards.size(); i++) {
                    if (cards.get(i).getManaCost().getCMC() == sourceCard.getManaCost().getCMC()) {
                        sameCost.add(cards.get(i));
                    }
                }

                if (sameCost.size() == 0) {
                    return;
                }

                final Card o = GuiChoose.oneOrNone("Select a card", sameCost);
                if (o != null) {
                    // ability.setTargetCard((Card)o);

                    sourceCard.getController().discard(sourceCard, this);
                    final Card c1 = o;

                    Singletons.getModel().getGame().getAction().moveToHand(c1);

                }
                sourceCard.getController().shuffle();
            }
        }
        final SpellAbility transmute = new AbilityTransmute(sourceCard, abCost, null);

        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Transmute (").append(abCost.toString());
        sbDesc.append("Search your library for a card with the same converted mana cost as this card, reveal it, ");
        sbDesc.append("and put it into your hand. Then shuffle your library. Transmute only as a sorcery.)");
        transmute.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard).append(" Transmute: Search your library ");
        sbStack.append("for a card with the same converted mana cost.)");
        transmute.setStackDescription(sbStack.toString());

        transmute.getRestrictions().setZone(ZoneType.Hand);
        return transmute;
    } // abilityTransmute()

    /**
     * <p>
     * abilitySuspend.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param suspendCost
     *            a {@link java.lang.String} object.
     * @param timeCounters
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilitySuspend(final Card sourceCard, final String suspendCost, final String timeCounters) {
        // be careful with Suspend ability, it will not hit the stack
        Cost cost = new Cost(sourceCard, suspendCost, true);
        final SpellAbility suspend = new AbilityStatic(sourceCard, cost, null) {
            @Override
            public boolean canPlay() {
                if (!(this.getRestrictions().canPlay(sourceCard, this))) {
                    return false;
                }

                if (sourceCard.isInstant() || sourceCard.hasKeyword("Flash")) {
                    return true;
                }

                return sourceCard.getOwner().canCastSorcery();
            }

            @Override
            public boolean canPlayAI() {
                return true;
                // Suspend currently not functional for the AI,
                // seems to be an issue with regaining Priority after Suspension
            }

            @Override
            public void resolve() {
                final Card c = Singletons.getModel().getGame().getAction().exile(sourceCard);

                int counters = AbilityUtils.calculateAmount(c, timeCounters, this);
                c.addCounter(CounterType.TIME, counters, true);

                StringBuilder sb = new StringBuilder();
                sb.append(this.getActivatingPlayer()).append(" has suspended ");
                sb.append(c.getName()).append("with ");
                sb.append(counters).append(" time counters on it.");
                Singletons.getModel().getGame().getGameLog().add("ResolveStack", sb.toString(), 2);
            }
        };
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Suspend ").append(timeCounters).append(" - ").append(cost.toSimpleString());
        suspend.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" suspending for ");
        sbStack.append(timeCounters).append(" turns.)");
        suspend.setStackDescription(sbStack.toString());

        suspend.getRestrictions().setZone(ZoneType.Hand);
        return suspend;
    } // abilitySuspend()

    /**
     * <p>
     * entersBattleFieldWithCounters.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param type
     *            a {@link forge.CounterType} object.
     * @param n
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command entersBattleFieldWithCounters(final Card c, final CounterType type, final int n) {
        final Command addCounters = new Command() {
            private static final long serialVersionUID = 4825430555490333062L;

            @Override
            public void execute() {
                c.addCounter(type, n, true);
            }
        };
        return addCounters;
    }

    /**
     * <p>
     * fading.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command fading(final Card sourceCard, final int power) {
        return entersBattleFieldWithCounters(sourceCard, CounterType.FADE, power);
    } // fading

    /**
     * <p>
     * vanishing.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command vanishing(final Card sourceCard, final int power) {
        return entersBattleFieldWithCounters(sourceCard, CounterType.TIME, power);
    } // vanishing

    // List<Card> choices are the only cards the user can successful select
    /**
     * <p>
     * inputTargetChampionSac.
     * </p>
     * 
     * @param crd
     *            a {@link forge.Card} object.
     * @param spell
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param choices
     *            a {@link forge.CardList} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param targeted
     *            a boolean.
     * @param free
     *            a boolean.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputTargetChampionSac(final Card crd, final SpellAbility spell, final List<Card> choices,
            final String message, final boolean targeted, final boolean free) {
        final Input target = new Input() {
            private static final long serialVersionUID = -3320425330743678663L;

            @Override
            public void showMessage() {
                CMatchUI.SINGLETON_INSTANCE.showMessage(message);
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                Singletons.getModel().getGame().getAction().sacrifice(crd, null);
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                if (choices.contains(card)) {
                    if (card == spell.getSourceCard()) {
                        Singletons.getModel().getGame().getAction().sacrifice(spell.getSourceCard(), null);
                        this.stop();
                    } else {
                        spell.getSourceCard().setChampionedCard(card);
                        Singletons.getModel().getGame().getAction().exile(card);

                        this.stop();

                        // Run triggers
                        final HashMap<String, Object> runParams = new HashMap<String, Object>();
                        runParams.put("Card", spell.getSourceCard());
                        runParams.put("Championed", card);
                        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.Championed, runParams, false);
                    }
                }
            } // selectCard()
        };
        return target;
    } // inputTargetSpecific()

    /**
     * <p>
     * masterOfTheWildHuntInputTargetCreature.
     * </p>
     * 
     * @param spell
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param choices
     *            a {@link forge.CardList} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input masterOfTheWildHuntInputTargetCreature(final SpellAbility spell, final List<Card> choices,
            final Command paid) {
        final Input target = new Input() {
            private static final long serialVersionUID = -1779224307654698954L;

            @Override
            public void showMessage() {
                final StringBuilder sb = new StringBuilder();
                sb.append("Select target wolf to damage for ").append(spell.getSourceCard());
                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                if (choices.size() == 0) {
                    this.stop();
                }
                if (choices.contains(card)) {
                    spell.setTargetCard(card);
                    paid.execute();
                    this.stop();
                }
            } // selectCard()
        };
        return target;
    } // masterOfTheWildHuntInputTargetCreature()

    /**
     * <p>
     * modularInput.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input modularInput(final SpellAbility ability, final Card card) {
        final Input modularInput = new Input() {

            private static final long serialVersionUID = 2322926875771867901L;

            @Override
            public void showMessage() {
                CMatchUI.SINGLETON_INSTANCE.showMessage("Select target artifact creature");
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card2) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(card2);
                if (card2.isCreature() && card2.isArtifact() && zone.is(ZoneType.Battlefield)
                        && card.canBeTargetedBy(ability)) {
                    ability.setTargetCard(card2);
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Put ").append(card.getCounters(CounterType.P1P1));
                    sb.append(" +1/+1 counter/s from ").append(card);
                    sb.append(" on ").append(card2);
                    ability.setStackDescription(sb.toString());
                    Singletons.getModel().getGame().getStack().add(ability);
                    this.stop();
                }
            }
        };
        return modularInput;
    }

    /**
     * <p>
     * getNumberOfManaSymbolsControlledByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsControlledByColor(final String colorAbb, final Player player) {
        final List<Card> cards = player.getCardsIn(ZoneType.Battlefield);
        return CardFactoryUtil.getNumberOfManaSymbolsByColor(colorAbb, cards);
    }

    /**
     * <p>
     * getNumberOfManaSymbolsByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param cards
     *            a {@link forge.List<Card>} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsByColor(final String colorAbb, final List<Card> cards) {
        int count = 0;
        for(Card c : cards) {
            // Certain tokens can have mana cost, so don't skip them
            count += CardFactoryUtil.getNumberOfManaSymbolsByColor(colorAbb, c);
        }
        return count;
    }
    
    /**
     * <p>
     * getNumberOfManaSymbolsByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param card
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsByColor(final String colorAbb, final Card card) {
        return CardFactoryUtil.countOccurrences(card.getManaCost().toString().trim(), colorAbb);
    }

    /**
     * <p>
     * multiplyCost.
     * </p>
     * 
     * @param manacost
     *            a {@link java.lang.String} object.
     * @param multiplier
     *            a int.
     * @return a {@link java.lang.String} object.
     */
    public static String multiplyCost(final String manacost, final int multiplier) {
        if (multiplier == 0) {
            return "";
        }
        if (multiplier == 1) {
            return manacost;
        }

        final String[] tokenized = manacost.split("\\s");
        final StringBuilder sb = new StringBuilder();

        if (Character.isDigit(tokenized[0].charAt(0))) {
            // cost starts with "colorless" number cost
            int cost = Integer.parseInt(tokenized[0]);
            cost = multiplier * cost;
            tokenized[0] = "" + cost;
            sb.append(tokenized[0]);
        } else {
            if (tokenized[0].contains("<")) {
                final String[] advCostPart = tokenized[0].split("<");
                final String costVariable = advCostPart[1].split(">")[0];
                final String[] advCostPartValid = costVariable.split("\\/", 2);
                // multiply the number part of the cost object
                int num = Integer.parseInt(advCostPartValid[0]);
                num = multiplier * num;
                tokenized[0] = advCostPart[0] + "<" + num;
                if (advCostPartValid.length > 1) {
                    tokenized[0] = tokenized[0] + "/" + advCostPartValid[1];
                }
                tokenized[0] = tokenized[0] + ">";
                sb.append(tokenized[0]);
            } else {
                for (int i = 0; i < multiplier; i++) {
                    // tokenized[0] = tokenized[0] + " " + tokenized[0];
                    sb.append((" "));
                    sb.append(tokenized[0]);
                }
            }
        }

        for (int i = 1; i < tokenized.length; i++) {
            if (tokenized[i].contains("<")) {
                final String[] advCostParts = tokenized[i].split("<");
                final String costVariables = advCostParts[1].split(">")[0];
                final String[] advCostPartsValid = costVariables.split("\\/", 2);
                // multiply the number part of the cost object
                int num = Integer.parseInt(advCostPartsValid[0]);
                num = multiplier * num;
                tokenized[i] = advCostParts[0] + "<" + num;
                if (advCostPartsValid.length > 1) {
                    tokenized[i] = tokenized[i] + "/" + advCostPartsValid[1];
                }
                tokenized[i] = tokenized[i] + ">";
                sb.append((" "));
                sb.append(tokenized[i]);
            } else {
                for (int j = 0; j < multiplier; j++) {
                    // tokenized[i] = tokenized[i] + " " + tokenized[i];
                    sb.append((" "));
                    sb.append(tokenized[i]);
                }
            }
        }

        String result = sb.toString();
        System.out.println("result: " + result);
        result = result.trim();
        return result;
    }

    /**
     * <p>
     * isTargetStillValid.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param target
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isTargetStillValid(final SpellAbility ability, final Card target) {

        if (Singletons.getModel().getGame().getZoneOf(target) == null) {
            return false; // for tokens that disappeared
        }

        final Card source = ability.getSourceCard();
        final Target tgt = ability.getTarget();
        if (tgt != null) {
            // Reconfirm the Validity of a TgtValid, or if the Creature is still
            // a Creature
            if (tgt.doesTarget()
                    && !target.isValid(tgt.getValidTgts(), ability.getActivatingPlayer(), ability.getSourceCard())) {
                return false;
            }

            // Check if the target is in the zone it needs to be in to be
            // targeted
            if (!Singletons.getModel().getGame().getZoneOf(target).is(tgt.getZone())) {
                return false;
            }
        } else {
            // If an Aura's target is removed before it resolves, the Aura
            // fizzles
            if (source.isAura() && !target.isInZone(ZoneType.Battlefield)) {
                return false;
            }
        }

        // Make sure it's still targetable as well
        return target.canBeTargetedBy(ability);
    }

    // does "target" have protection from "card"?
    /**
     * <p>
     * hasProtectionFrom.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean hasProtectionFrom(final Card card, final Card target) {
        if (target == null) {
            return false;
        }

        return target.hasProtectionFrom(card);
    }

    /**
     * <p>
     * isCounterable.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isCounterable(final Card c) {
        if (c.hasKeyword("CARDNAME can't be countered.") || !c.getCanCounter()) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * isCounterableBy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            the sa
     * @return a boolean.
     */
    public static boolean isCounterableBy(final Card c, final SpellAbility sa) {
        if (!CardFactoryUtil.isCounterable(c)) {
            return false;
        }
        //TODO: Add code for Autumn's Veil here

        return true;
    }

    /**
     * <p>
     * getExternalZoneActivationCards.
     * </p>
     * 
     * @param activator
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> getExternalZoneActivationCards(final Player activator) {
        final List<Card> cl = new ArrayList<Card>();

        cl.addAll(CardFactoryUtil.getActivateablesFromZone(activator.getZone(ZoneType.Graveyard), activator));
        cl.addAll(CardFactoryUtil.getActivateablesFromZone(activator.getZone(ZoneType.Exile), activator));
        cl.addAll(CardFactoryUtil.getActivateablesFromZone(activator.getZone(ZoneType.Library), activator));
        cl.addAll(CardFactoryUtil.getActivateablesFromZone(activator.getZone(ZoneType.Command), activator));

        //External activatables from all opponents
        for (final Player opponent : activator.getOpponents()) {
            cl.addAll(CardFactoryUtil.getActivateablesFromZone(opponent.getZone(ZoneType.Exile), activator));
            cl.addAll(CardFactoryUtil.getActivateablesFromZone(opponent.getZone(ZoneType.Graveyard), activator));
            if (opponent.hasKeyword("Play with your hand revealed.")) {
                cl.addAll(CardFactoryUtil.getActivateablesFromZone(opponent.getZone(ZoneType.Hand), activator));
            }
        }

        return cl;
    }

    /**
     * <p>
     * getActivateablesFromZone.
     * </p>
     * 
     * @param zone
     *            a PlayerZone object.
     * @param activator
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static List<Card> getActivateablesFromZone(final PlayerZone zone, final Player activator) {

        Iterable<Card> cl = zone.getCards();

        // Only check the top card of the library
        if (zone.is(ZoneType.Library)) {
            cl = Iterables.limit(cl, 1);
        }

        if (activator.equals(zone.getPlayer())) {
            cl = Iterables.filter(cl, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (zone.is(ZoneType.Graveyard) && c.hasUnearth()) {
                        return true;
                    }

                    if (c.hasKeyword("You may look at this card.")) {
                        return true;
                    }

                    if (c.isLand()
                            && (c.hasKeyword("May be played") || c.hasKeyword("May be played without paying its mana cost"))) {
                        return true;
                    }

                    for (final SpellAbility sa : c.getSpellAbility()) {
                        final ZoneType restrictZone = sa.getRestrictions().getZone();
                        if (zone.is(restrictZone)) {
                            return true;
                        }

                        if (sa.isSpell()
                                && (c.hasKeyword("May be played") || c.hasKeyword("May be played without paying its mana cost")
                                        || (c.hasStartOfKeyword("Flashback") && zone.is(ZoneType.Graveyard)))
                                && restrictZone.equals(ZoneType.Hand)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        } else {
            // the activator is not the owner of the card
            cl = Iterables.filter(cl, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {

                    if (c.hasStartOfKeyword("May be played by your opponent")
                            || c.hasKeyword("Your opponent may look at this card.")) {
                        return true;
                    }
                    return false;
                }
            });
        }
        return Lists.newArrayList(cl);
    }

    /**
     * <p>
     * countOccurrences.
     * </p>
     * 
     * @param arg1
     *            a {@link java.lang.String} object.
     * @param arg2
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int countOccurrences(final String arg1, final String arg2) {

        int count = 0;
        int index = 0;
        while ((index = arg1.indexOf(arg2, index)) != -1) {
            ++index;
            ++count;
        }
        return count;
    }

    /**
     * <p>
     * parseMath.
     * </p>
     * 
     * @param l
     *            an array of {@link java.lang.String} objects.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] parseMath(final String[] l) {
        final String[] m = { "none" };
        if (l.length > 1) {
            m[0] = l[1];
        }

        return m;
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     * 
     * @param players
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int objectXCount(final ArrayList<Object> objects, final String s, final Card source) {
        if (objects.size() == 0) {
            return 0;
        }

        final String[] l = s.split("/");
        final String[] m = CardFactoryUtil.parseMath(l);

        int n = 0;

        if (s.startsWith("Amount")) {
            n = objects.size();
        }

        return CardFactoryUtil.doXMath(n, m, source);
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     * 
     * @param players
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int playerXCount(final List<Player> players, final String s, final Card source) {
        if (players.size() == 0) {
            return 0;
        }

        final String[] l = s.split("/");
        final String[] m = CardFactoryUtil.parseMath(l);

        int n = 0;

        // methods for getting the highest/lowest playerXCount from a range of players
        if (l[0].startsWith("Highest")) {
            for (final Player player : players) {
                final ArrayList<Player> temp = new ArrayList<Player>();
                temp.add(player);
                final int current = CardFactoryUtil.playerXCount(temp, s.replace("Highest", ""), source);
                if (current > n) {
                    n = current;
                }
            }

            return CardFactoryUtil.doXMath(n, m, source);
        } else if (l[0].startsWith("Lowest")) {
            n = 99999; // if no players have fewer than 99999 valids, the game is frozen anyway
            for (final Player player : players) {
                final ArrayList<Player> temp = new ArrayList<Player>();
                temp.add(player);
                final int current = CardFactoryUtil.playerXCount(temp, s.replace("Lowest", ""), source);
                if (current < n) {
                    n = current;
                }
            }

            return CardFactoryUtil.doXMath(n, m, source);
        }

        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid") && !l[0].contains("Valid ")) {
            String[] lparts = l[0].split(" ", 2);
            final List<ZoneType> vZone = ZoneType.listValueOf(lparts[0].split("Valid")[1]);
            String restrictions = l[0].replace(lparts[0] + " ", "");
            final String[] rest = restrictions.split(",");
            List<Card> cards = Singletons.getModel().getGame().getCardsIn(vZone);
            cards = CardLists.getValidCards(cards, rest, players.get(0), source);

            n = cards.size();

            return CardFactoryUtil.doXMath(n, m, source);
        }
        // count valid cards on the battlefield
        if (l[0].startsWith("Valid ")) {
            final String restrictions = l[0].substring(6);
            final String[] rest = restrictions.split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            cardsonbattlefield = CardLists.getValidCards(cardsonbattlefield, rest, players.get(0), source);

            n = cardsonbattlefield.size();

            return CardFactoryUtil.doXMath(n, m, source);
        }

        final String[] sq;
        sq = l[0].split("\\.");

        // the number of players passed in
        if (sq[0].equals("Amount")) {
            return CardFactoryUtil.doXMath(players.size(), m, source);
        }

        if (sq[0].contains("CardsInHand")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(ZoneType.Hand).size(), m, source);
            }
        }

        if (sq[0].contains("DomainPlayer")) {
            final List<Card> someCards = new ArrayList<Card>();
            someCards.addAll(players.get(0).getCardsIn(ZoneType.Battlefield));
            final String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

            for (int i = 0; i < basic.length; i++) {
                if (!CardLists.getType(someCards, basic[i]).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, source);
        }

        if (sq[0].contains("CardsInLibrary")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(ZoneType.Library).size(), m, source);
            }
        }

        if (sq[0].contains("CardsInGraveyard")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(ZoneType.Graveyard).size(), m, source);
            }
        }
        if (sq[0].contains("LandsInGraveyard")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(CardLists.getType(players.get(0).getCardsIn(ZoneType.Graveyard), "Land").size(), m,
                        source);
            }
        }

        if (sq[0].contains("CreaturesInPlay")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCreaturesInPlay().size(), m, source);
            }
        }

        if (sq[0].contains("CardsInPlay")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(ZoneType.Battlefield).size(), m, source);
            }
        }

        if (sq[0].contains("LifeTotal")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getLife(), m, source);
            }
        }

        if (sq[0].contains("LifeLostThisTurn")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getLifeLostThisTurn(), m, source);
            }
        }
        
        if (sq[0].contains("PoisonCounters")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getPoisonCounters(), m, source);
            }
        }

        if (sq[0].contains("TopOfLibraryCMC")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(Aggregates.sum(players.get(0).getCardsIn(ZoneType.Library, 1), CardPredicates.Accessors.fnGetCmc),
                        m, source);
            }
        }

        if (sq[0].contains("LandsPlayed")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getNumLandsPlayed(), m, source);
            }
        }

        if (sq[0].contains("CardsDrawn")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getNumDrawnThisTurn(), m, source);
            }
        }

        if (sq[0].contains("AttackersDeclared")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getAttackersDeclaredThisTurn(), m, source);
            }
        }

        if (sq[0].equals("DamageDoneToPlayerBy")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(source.getDamageDoneToPlayerBy(players.get(0).getName()), m, source);
            }
        }

        if (sq[0].contains("DamageToOppsThisTurn")) {
            if (players.size() > 0) {
                int oppDmg = 0;
                for (Player opp : players.get(0).getOpponents()) {
                    oppDmg += opp.getAssignedDamage();
                }
                return CardFactoryUtil.doXMath(oppDmg, m, source);
            }
        }

        if (sq[0].contains("DamageThisTurn")) {
            if (players.size() > 0) {
                int totDmg = 0;
                for (Player p : players) {
                    totDmg += p.getAssignedDamage();
                }
                return CardFactoryUtil.doXMath(totDmg, m, source);
            }
        }

        return CardFactoryUtil.doXMath(n, m, source);
    }

    /**
     * <p>
     * Parse non-mana X variables.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int xCount(final Card c, final String s) {
        int n = 0;

        final Player cardController = c.getController();
        final Player oppController = cardController.getOpponent();
        final Player activePlayer = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        final String[] l = s.split("/");
        final String[] m = CardFactoryUtil.parseMath(l);

        // accept straight numbers
        if (l[0].startsWith("Number$")) {
            final String number = l[0].substring(7);
            if (number.equals("ChosenNumber")) {
                return CardFactoryUtil.doXMath(c.getChosenNumber(), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(number), m, c);
            }
        }

        if (l[0].startsWith("Count$")) {
            l[0] = l[0].substring(6);
        }

        if (l[0].startsWith("SVar$")) {
            final String sVar = l[0].substring(5);
            return CardFactoryUtil.doXMath(CardFactoryUtil.xCount(c, c.getSVar(sVar)), m, c);
        }

        // Manapool
        if (l[0].startsWith("ManaPool")) {
            final String color = l[0].split(":")[1];
            if (color.equals("All")) {
                return c.getController().getManaPool().totalMana();
            } else {
                return c.getController().getManaPool().getAmountOfColor(color);
            }
        }

        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid") && !l[0].contains("Valid ")) {
            String[] lparts = l[0].split(" ", 2);
            final List<ZoneType> vZone = ZoneType.listValueOf(lparts[0].split("Valid")[1]);
            String restrictions = l[0].replace(lparts[0] + " ", "");
            final String[] rest = restrictions.split(",");
            List<Card> cards = Singletons.getModel().getGame().getCardsIn(vZone);
            cards = CardLists.getValidCards(cards, rest, cardController, c);

            n = cards.size();

            return CardFactoryUtil.doXMath(n, m, c);
        }
        // count valid cards on the battlefield
        if (l[0].startsWith("Valid ")) {
            String restrictions = l[0].substring(6);
            final String[] rest = restrictions.split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            cardsonbattlefield = CardLists.getValidCards(cardsonbattlefield, rest, cardController, c);

            n = cardsonbattlefield.size();

            return CardFactoryUtil.doXMath(n, m, c);
        }

        if (l[0].startsWith("ImprintedCardPower")) {
            if (c.getImprinted().size() > 0) {
                return c.getImprinted().get(0).getNetAttack();
            }
        }

        if (l[0].startsWith("ImprintedCardToughness")) {
            if (c.getImprinted().size() > 0) {
                return c.getImprinted().get(0).getNetDefense();
            }
        }

        if (l[0].startsWith("ImprintedCardManaCost")) {
            if (!c.getImprinted().isEmpty()) {
                return c.getImprinted().get(0).getCMC();
            }
        }

        if (l[0].startsWith("GreatestPowerYouControl")) {
            final List<Card> list = c.getController().getCreaturesInPlay();
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetAttack() > highest) {
                    highest = crd.getNetAttack();
                }
            }
            return highest;
        }

        if (l[0].startsWith("GreatestPowerYouDontControl")) {
            final List<Card> list = c.getController().getOpponent().getCreaturesInPlay();
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetAttack() > highest) {
                    highest = crd.getNetAttack();
                }
            }
            return highest;
        }

        if (l[0].startsWith("HighestCMCRemembered")) {
            final List<Card> list = new ArrayList<Card>();
            int highest = 0;
            for (final Object o : c.getRemembered()) {
                if (o instanceof Card) {
                    list.add(Singletons.getModel().getGame().getCardState((Card) o));
                }
            }
            for (final Card crd : list) {
                if (crd.getCMC() > highest) {
                    highest = crd.getCMC();
                }
            }
            return highest;
        }

        if (l[0].startsWith("DifferentCardNamesRemembered")) {
            final List<Card> list = new ArrayList<Card>();
            final List<String> crdname = new ArrayList<String>();
            if (c.getRemembered().size() > 0) {
                for (final Object o : c.getRemembered()) {
                    if (o instanceof Card) {
                        list.add(Singletons.getModel().getGame().getCardState((Card) o));
                    }
                }
            }
            for (final Card card : list) {
                if (!crdname.contains(card.getName())) {
                    crdname.add(card.getName());
                }
            }
            return crdname.size();
        }

        // Count$CountersAdded <CounterType> <ValidSource>
        if (l[0].startsWith("CountersAdded")) {
            final String[] components = l[0].split(" ", 3);
            final CounterType counterType = CounterType.valueOf(components[1]);
            String restrictions = components[2];
            final String[] rest = restrictions.split(",");
            List<Card> candidates = Singletons.getModel().getGame().getCardsInGame();
            candidates = CardLists.getValidCards(candidates, rest, cardController, c);

            int added = 0;
            for (final Card counterSource : candidates) {
                added += c.getCountersAddedBy(counterSource, counterType);
            }
            return CardFactoryUtil.doXMath(added, m, c);
        }

        if (l[0].startsWith("RolledThisTurn")) {
            return Singletons.getModel().getGame().getPhaseHandler().getPlanarDiceRolledthisTurn();
        }

        final String[] sq;
        sq = l[0].split("\\.");

        if (sq[0].contains("xPaid")) {
            return CardFactoryUtil.doXMath(c.getXManaCostPaid(), m, c);
        }

        if (sq[0].equals("YouDrewThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getNumDrawnThisTurn(), m, c);
        }
        if (sq[0].equals("OppDrewThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getOpponent().getNumDrawnThisTurn(), m, c);
        }

        if (sq[0].equals("StormCount")) {
            return CardFactoryUtil.doXMath(Singletons.getModel().getGame().getStack().getCardsCastThisTurn().size() - 1, m, c);
        }

        if (sq[0].equals("DamageDoneThisTurn")) {
            return CardFactoryUtil.doXMath(c.getDamageDoneThisTurn(), m, c);
        }

        if (sq[0].equals("BloodthirstAmount")) {
            return CardFactoryUtil.doXMath(c.getController().getBloodthirstAmount(), m, c);
        }

        if (sq[0].equals("RegeneratedThisTurn")) {
            return CardFactoryUtil.doXMath(c.getRegeneratedThisTurn(), m, c);
        }

        List<Card> someCards = new ArrayList<Card>();

        // Complex counting methods

        // TriggeringObjects
        if (sq[0].startsWith("Triggered")) {
            return CardFactoryUtil.doXMath((Integer) c.getTriggeringObject(sq[0].substring(9)), m, c);
        }

        // Count$Domain
        if (sq[0].equals("Domain")) {
            someCards.addAll(cardController.getCardsIn(ZoneType.Battlefield));
            for (String basic : Constant.Color.BASIC_LANDS) {
                if (!CardLists.getType(someCards, basic).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$ActivePlayerDomain
        if (sq[0].contains("ActivePlayerDomain")) {
            someCards.addAll(activePlayer.getCardsIn(ZoneType.Battlefield));
            for (String basic : Constant.Color.BASIC_LANDS) {
                if (!CardLists.getType(someCards, basic).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$ColoredCreatures *a DOMAIN for creatures*
        if (sq[0].contains("ColoredCreatures")) {
            someCards.addAll(cardController.getCardsIn(ZoneType.Battlefield));
            someCards = CardLists.filter(someCards, Presets.CREATURES);

            final String[] colors = { "green", "white", "red", "blue", "black" };

            for (final String color : colors) {
                if (!CardLists.getColor(someCards, color).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$YourStartingLife
        if (sq[0].contains("YourStartingLife")) {
            return CardFactoryUtil.doXMath(cardController.getStartingLife(), m, c);
        }

        // Count$OppStartingLife
        if (sq[0].contains("OppStartingLife")) {
            return CardFactoryUtil.doXMath(oppController.getStartingLife(), m, c);
        }

        // Count$YourLifeTotal
        if (sq[0].contains("YourLifeTotal")) {
            return CardFactoryUtil.doXMath(cardController.getLife(), m, c);
        }

        // Count$OppLifeTotal
        if (sq[0].contains("OppLifeTotal")) {
            return CardFactoryUtil.doXMath(oppController.getLife(), m, c);
        }

        // Count$DefenderLifeTotal
        if (sq[0].contains("DefenderLifeTotal")) {
            Player defender = Singletons.getModel().getGame().getCombat().getDefendingPlayerRelatedTo(c);
            return CardFactoryUtil.doXMath(defender.getLife(), m, c);
        }

        //  Count$TargetedLifeTotal (targeted player's life total)
        if (sq[0].contains("TargetedLifeTotal")) {
            for (final SpellAbility sa : c.getCharacteristics().getSpellAbility()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTarget().getTargetPlayers()) {
                        return CardFactoryUtil.doXMath(tgtP.getLife(), m, c);
                    }
                }
            }
        }

        if (sq[0].contains("LifeYouLostThisTurn")) {
            return CardFactoryUtil.doXMath(cardController.getLifeLostThisTurn(), m, c);
        }

        if (sq[0].contains("LifeOppsLostThisTurn")) {
            int lost = 0;
            for (Player opp : cardController.getOpponents()) {
                lost += opp.getLifeLostThisTurn();
            }
            return CardFactoryUtil.doXMath(lost, m, c);
        }

        if (sq[0].equals("TotalDamageDoneByThisTurn")) {
            return CardFactoryUtil.doXMath(c.getTotalDamageDoneBy(), m, c);
        }

        // Count$YourPoisonCounters
        if (sq[0].contains("YourPoisonCounters")) {
            return CardFactoryUtil.doXMath(cardController.getPoisonCounters(), m, c);
        }

        // Count$OppPoisonCounters
        if (sq[0].contains("OppPoisonCounters")) {
            return CardFactoryUtil.doXMath(oppController.getPoisonCounters(), m, c);
        }

        // Count$OppDamageThisTurn
        if (sq[0].contains("OppDamageThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getOpponent().getAssignedDamage(), m, c);
        }

        // Count$YourDamageThisTurn
        if (sq[0].contains("YourDamageThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getAssignedDamage(), m, c);
        }

        // Count$YourTypeDamageThisTurn Type
        if (sq[0].contains("OppTypeDamageThisTurn")) {
            final String[] type = sq[0].split(" ");
            return CardFactoryUtil.doXMath(c.getController().getOpponent().getAssignedDamage(type[1]), m, c);
        }

        // Count$YourTypeDamageThisTurn Type
        if (sq[0].contains("YourTypeDamageThisTurn")) {
            final String[] type = sq[0].split(" ");
            return CardFactoryUtil.doXMath(c.getController().getAssignedDamage(type[1]), m, c);
        }

        if (sq[0].contains("YourLandsPlayed")) {
            return CardFactoryUtil.doXMath(c.getController().getNumLandsPlayed(), m, c);
        }

        // Count$HighestLifeTotal
        if (sq[0].contains("HighestLifeTotal")) {
            return CardFactoryUtil.doXMath(
                    Aggregates.max(Singletons.getModel().getGame().getPlayers(), Player.Accessors.FN_GET_LIFE), m, c);
        }

        // Count$LowestLifeTotal
        if (sq[0].contains("LowestLifeTotal")) {
            final String[] playerType = sq[0].split(" ");
            if (playerType.length > 1 && playerType[1].equals("Opponent")) {
                return CardFactoryUtil.doXMath(
                        Aggregates.min(cardController.getOpponents(), Player.Accessors.FN_GET_LIFE), m, c);
            } else {
                return CardFactoryUtil.doXMath(
                        Aggregates.min(Singletons.getModel().getGame().getPlayers(), Player.Accessors.FN_GET_LIFE), m, c);
            }
        }

        // Count$TopOfLibraryCMC
        if (sq[0].contains("TopOfLibraryCMC")) {
            final List<Card> topcard = cardController.getCardsIn(ZoneType.Library, 1);
            return CardFactoryUtil.doXMath(Aggregates.sum(topcard, CardPredicates.Accessors.fnGetCmc), m, c);
        }

        // Count$EnchantedControllerCreatures
        if (sq[0].contains("EnchantedControllerCreatures")) {
            List<Card> enchantedControllerInPlay = new ArrayList<Card>();
            if (c.getEnchantingCard() != null) {
                enchantedControllerInPlay = c.getEnchantingCard().getController().getCardsIn(ZoneType.Battlefield);
                enchantedControllerInPlay = CardLists.getType(enchantedControllerInPlay, "Creature");
            }
            return enchantedControllerInPlay.size();
        }

        // Count$LowestLibrary
        if (sq[0].contains("LowestLibrary")) {
            return Aggregates.min(Singletons.getModel().getGame().getPlayers(), Player.Accessors.countCardsInZone(ZoneType.Library));
        }

        // Count$Chroma.<mana letter>
        if (sq[0].contains("Chroma")) {
            if (sq[0].contains("ChromaSource")) {
                // Runs Chroma for passed in Source card
                List<Card> chromaList = CardLists.createCardList(c);
                return CardFactoryUtil.doXMath(CardFactoryUtil.getNumberOfManaSymbolsByColor(sq[1], chromaList), m, c);
            }
            else {
                return CardFactoryUtil.doXMath(
                    CardFactoryUtil.getNumberOfManaSymbolsControlledByColor(sq[1], cardController), m, c);
            }
        }

        // Count$Hellbent.<numHB>.<numNotHB>
        if (sq[0].contains("Hellbent")) {
            if (cardController.hasHellbent()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c); // Hellbent
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c); // not
                                                                               // Hellbent
            }
        }

        // Count$Metalcraft.<numMC>.<numNotMC>
        if (sq[0].contains("Metalcraft")) {
            if (cardController.hasMetalcraft()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Count$FatefulHour.<numFH>.<numNotFH>
        if (sq[0].contains("FatefulHour")) {
            if (cardController.getLife() <= 5) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Count$wasCastFrom<Zone>.<true>.<false>
        if (sq[0].startsWith("wasCastFrom")) {
            final String strZone = sq[0].substring(11);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (c.getCastFrom() == realZone) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].contains("Threshold")) {
            if (cardController.hasThreshold()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].contains("Landfall")) {
            if (cardController.hasLandfall()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }
        if (sq[0].startsWith("Kicked")) {
            if (c.isOptionalAdditionalCostsPaid("Kicker")) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].contains("GraveyardWithGE20Cards")) {
            if (Aggregates.max(Singletons.getModel().getGame().getPlayers(), Player.Accessors.countCardsInZone(ZoneType.Graveyard)) >= 20) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].startsWith("Devoured")) {
            final String validDevoured = l[0].split(" ")[1];
            final Card csource = c;
            List<Card> cl = c.getDevoured();
            cl = CardLists.getValidCards(cl, validDevoured.split(","), csource.getController(), csource);
            return CardFactoryUtil.doXMath(cl.size(), m, c);
        }

        // Count$CardPower
        if (sq[0].contains("CardPower")) {
            return CardFactoryUtil.doXMath(c.getNetAttack(), m, c);
        }
        // Count$CardToughness
        if (sq[0].contains("CardToughness")) {
            return CardFactoryUtil.doXMath(c.getNetDefense(), m, c);
        }
        // Count$CardPowerPlusToughness
        if (sq[0].contains("CardSumPT")) {
            return CardFactoryUtil.doXMath((c.getNetAttack() + c.getNetDefense()), m, c);
        }
        // Count$SumPower_valid
        if (sq[0].contains("SumPower")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            List<Card> filteredCards = CardLists.getValidCards(cardsonbattlefield, rest, cardController, c);
            int sumPower = 0;
            for (int i = 0; i < filteredCards.size(); i++) {
                sumPower += filteredCards.get(i).getManaCost().getCMC();
            }
            return CardFactoryUtil.doXMath(sumPower, m, c);
        }
        // Count$CardManaCost
        if (sq[0].contains("CardManaCost")) {
            if (sq[0].contains("Equipped") && c.isEquipping()) {
                return CardFactoryUtil.doXMath(c.getEquipping().get(0).getCMC(), m, c);
            } else {
                return CardFactoryUtil.doXMath(c.getCMC(), m, c);
            }
        }
        // Count$SumCMC_valid
        if (sq[0].contains("SumCMC")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            List<Card> filteredCards = CardLists.getValidCards(cardsonbattlefield, rest, cardController, c);
            return Aggregates.sum(filteredCards, CardPredicates.Accessors.fnGetCmc);
        }
        // Count$CardNumColors
        if (sq[0].contains("CardNumColors")) {
            return CardFactoryUtil.doXMath(CardUtil.getColors(c).size(), m, c);
        }
        // Count$ChosenNumber
        if (sq[0].contains("ChosenNumber")) {
            return CardFactoryUtil.doXMath(c.getChosenNumber(), m, c);
        }
        // Count$CardCounters.<counterType>
        if (sq[0].contains("CardCounters")) {
            return CardFactoryUtil.doXMath(c.getCounters(CounterType.getType(sq[1])), m, c);
        }
        // Count$TotalCounters.<counterType>_<valid>
        if (sq[0].contains("TotalCounters")) {
            final String[] restrictions = l[0].split("_");
            final CounterType cType = CounterType.getType(restrictions[1]);
            final String[] validFilter = restrictions[2].split(",");
            List<Card> validCards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            validCards = CardLists.getValidCards(validCards, validFilter, cardController, c);
            int cCount = 0;
            for (final Card card : validCards) {
                cCount += card.getCounters(cType);
            }
            return CardFactoryUtil.doXMath(cCount, m, c);
        }
        
        // Count$BushidoPoint
        if (sq[0].contains("BushidoPoint")) {
            int magnitude = 0;
            for (final String kw : c.getKeyword()) {
                if (kw.contains("Bushido")) {
                    final String[] parse = kw.split(" ");
                    final String num = parse[1];
                    magnitude += Integer.parseInt(num);
                }
            }
            return CardFactoryUtil.doXMath(magnitude, m, c);
        }

        // Count$TimesKicked
        if (sq[0].contains("TimesKicked")) {
            return CardFactoryUtil.doXMath(c.getMultiKickerMagnitude(), m, c);
        }
        if (sq[0].contains("NumCounters")) {
            final int num = c.getCounters(CounterType.getType(sq[1]));
            return CardFactoryUtil.doXMath(num, m, c);
        }

        // Count$IfMainPhase.<numMain>.<numNotMain> // 7/10
        if (sq[0].contains("IfMainPhase")) {
            final PhaseHandler cPhase = Singletons.getModel().getGame().getPhaseHandler();
            if (cPhase.getPhase().isMain() && cPhase.getPlayerTurn().equals(cardController)) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Count$M12Empires.<numIf>.<numIfNot>
        if (sq[0].contains("AllM12Empires")) {
            boolean has = c.getController().isCardInPlay("Crown of Empires");
            has &= c.getController().isCardInPlay("Scepter of Empires");
            has &= c.getController().isCardInPlay("Throne of Empires");
            if (has) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Count$ThisTurnEntered <ZoneDestination> <ZoneOrigin> <Valid>
        // or
        // Count$ThisTurnEntered <ZoneDestination> <Valid>
        if (sq[0].contains("ThisTurnEntered")) {
            final String[] workingCopy = l[0].split("_");
            ZoneType destination, origin;
            String validFilter;

            destination = ZoneType.smartValueOf(workingCopy[1]);
            if (workingCopy[2].equals("from")) {
                origin = ZoneType.smartValueOf(workingCopy[3]);
                validFilter = workingCopy[4];
            } else {
                origin = null;
                validFilter = workingCopy[2];
            }

            final List<Card> res = CardUtil.getThisTurnEntered(destination, origin, validFilter, c);

            return CardFactoryUtil.doXMath(res.size(), m, c);
        }

        // Count$AttackersDeclared
        if (sq[0].contains("AttackersDeclared")) {
            return CardFactoryUtil.doXMath(cardController.getAttackersDeclaredThisTurn(), m, c);
        }

        // Count$ThisTurnCast <Valid>
        // Count$LastTurnCast <Valid>
        if (sq[0].contains("ThisTurnCast") || sq[0].contains("LastTurnCast")) {

            final String[] workingCopy = l[0].split("_");
            final String validFilter = workingCopy[1];

            List<Card> res;

            if (workingCopy[0].contains("This")) {
                res = CardUtil.getThisTurnCast(validFilter, c);
            } else {
                res = CardUtil.getLastTurnCast(validFilter, c);
            }

            final int ret = CardFactoryUtil.doXMath(res.size(), m, c);
            return ret;
        }

        // Count$Morbid.<True>.<False>
        if (sq[0].startsWith("Morbid")) {
            final List<Card> res = CardUtil.getThisTurnEntered(ZoneType.Graveyard, ZoneType.Battlefield, "Creature", c);
            if (res.size() > 0) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].equals("YourTurns")) {
            return CardFactoryUtil.doXMath(cardController.getTurn(), m, c);
        }

        if (sq[0].equals("TotalTurns")) {
            // Sorry for the Singleton use, replace this once this function has game passed into it
            return CardFactoryUtil.doXMath(Singletons.getModel().getGame().getPhaseHandler().getTurn(), m, c);
        }

        // Generic Zone-based counting
        // Count$QualityAndZones.Subquality

        // build a list of cards in each possible specified zone

        // if a card was ever written to count two different zones,
        // make sure they don't get added twice.
        boolean mf = false, my = false, mh = false;
        boolean of = false, oy = false, oh = false;

        if (sq[0].contains("YouCtrl")) {
            if (!mf) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Battlefield));
                mf = true;
            }
        }

        if (sq[0].contains("InYourYard")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Graveyard));
                my = true;
            }
        }

        if (sq[0].contains("InYourLibrary")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Library));
                my = true;
            }
        }

        if (sq[0].contains("InYourHand")) {
            if (!mh) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Hand));
                mh = true;
            }
        }

        if (sq[0].contains("InYourSideboard")) {
            if (!mh) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Sideboard));
                mh = true;
            }
        }

        if (sq[0].contains("OppCtrl")) {
            if (!of) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Battlefield));
                of = true;
            }
        }

        if (sq[0].contains("InOppYard")) {
            if (!oy) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Graveyard));
                oy = true;
            }
        }

        if (sq[0].contains("InOppHand")) {
            if (!oh) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Hand));
                oh = true;
            }
        }

        if (sq[0].contains("InChosenHand")) {
            if (!oh) {
                if (c.getChosenPlayer() != null) {
                    someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Hand));
                }
                oh = true;
            }
        }

        if (sq[0].contains("InChosenYard")) {
            if (!oh) {
                if (c.getChosenPlayer() != null) {
                    someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Graveyard));
                }
                oh = true;
            }
        }

        if (sq[0].contains("OnBattlefield")) {
            if (!mf) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Battlefield));
            }
            if (!of) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Battlefield));
            }
        }

        if (sq[0].contains("InAllYards")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Graveyard));
            }
            if (!oy) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Graveyard));
            }
        }

        if (sq[0].contains("SpellsOnStack")) {
            someCards.addAll(Singletons.getModel().getGame().getCardsIn(ZoneType.Stack));
        }

        if (sq[0].contains("InAllHands")) {
            if (!mh) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Hand));
            }
            if (!oh) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Hand));
            }
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InTargetedHand")) {
            for (final SpellAbility sa : c.getCharacteristics().getSpellAbility()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTarget().getTargetPlayers()) {
                        someCards.addAll(tgtP.getCardsIn(ZoneType.Hand));
                    }
                }
            }
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InEnchantedHand")) {
            GameEntity o = c.getEnchanting();
            Player controller = null;
            if (o instanceof Card) {
                controller = ((Card) o).getController();
            }
            else {
                controller = (Player) o;
            }
            if (controller != null) {
                someCards.addAll(controller.getCardsIn(ZoneType.Hand));
            }
        }
        if (sq[0].contains("InEnchantedYard")) {
            GameEntity o = c.getEnchanting();
            Player controller = null;
            if (o instanceof Card) {
                controller = ((Card) o).getController();
            }
            else {
                controller = (Player) o;
            }
            if (controller != null) {
                someCards.addAll(controller.getCardsIn(ZoneType.Graveyard));
            }
        }
        // filter lists based on the specified quality

        // "Clerics you control" - Count$TypeYouCtrl.Cleric
        if (sq[0].contains("Type")) {
            someCards = CardLists.filter(someCards, CardPredicates.isType(sq[1]));
        }

        // "Named <CARDNAME> in all graveyards" - Count$NamedAllYards.<CARDNAME>

        if (sq[0].contains("Named")) {
            if (sq[1].equals("CARDNAME")) {
                sq[1] = c.getName();
            }
            someCards = CardLists.filter(someCards, CardPredicates.nameEquals(sq[1]));
        }

        // Refined qualities

        // "Untapped Lands" - Count$UntappedTypeYouCtrl.Land
        if (sq[0].contains("Untapped")) {
            someCards = CardLists.filter(someCards, Presets.UNTAPPED);
        }

        if (sq[0].contains("Tapped")) {
            someCards = CardLists.filter(someCards, Presets.TAPPED);
        }

//        String sq0 = sq[0].toLowerCase();
//        for(String color : Constant.Color.ONLY_COLORS) {
//            if( sq0.contains(color) )
//                someCards = someCards.filter(CardListFilter.WHITE);
//        }
        // "White Creatures" - Count$WhiteTypeYouCtrl.Creature
        if (sq[0].contains("White")) {
            someCards = CardLists.filter(someCards, Presets.WHITE);
        }

        if (sq[0].contains("Blue")) {
            someCards = CardLists.filter(someCards, Presets.BLUE);
        }

        if (sq[0].contains("Black")) {
            someCards = CardLists.filter(someCards, Presets.BLACK);
        }

        if (sq[0].contains("Red")) {
            someCards = CardLists.filter(someCards, Presets.RED);
        }

        if (sq[0].contains("Green")) {
            someCards = CardLists.filter(someCards, Presets.GREEN);
        }

        if (sq[0].contains("Multicolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return (CardUtil.getColors(c).size() > 1);
                }
            });
        }

        if (sq[0].contains("Monocolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return (CardUtil.getColors(c).size() == 1);
                }
            });
        }

        // Count$CardMulticolor.<numMC>.<numNotMC>
        if (sq[0].contains("CardMulticolor")) {
            if (CardUtil.getColors(c).size() > 1) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // 1/10 - Count$MaxCMCYouCtrl
        if (sq[0].contains("MaxCMC")) {
            int mmc = 0;
            int cmc = 0;
            for (int i = 0; i < someCards.size(); i++) {
                cmc = someCards.get(i).getManaCost().getCMC();
                if (cmc > mmc) {
                    mmc = cmc;
                }
            }

            return CardFactoryUtil.doXMath(mmc, m, c);
        }

        //Count$Random.<Min>.<Max>
        if (sq[0].equals("Random")) {
            int min = 0;
            int max = 0;
            
            if (StringUtils.isNumeric(sq[1])) {
                min = Integer.parseInt(sq[1]);
            } else {
                min = CardFactoryUtil.xCount(c, c.getSVar(sq[1]));
            }
            
            if (StringUtils.isNumeric(sq[2])) {
                max = Integer.parseInt(sq[2]);
            } else {
                max = CardFactoryUtil.xCount(c, c.getSVar(sq[2]));
            }
            
            return forge.util.MyRandom.getRandom().nextInt(max) + min;
        }

        n = someCards.size();

        return CardFactoryUtil.doXMath(n, m, c);
    }

    private static int doXMath(final int num, final String m, final Card c) {
        if (m.equals("none")) {
            return num;
        }

        final String[] s = m.split("\\.");
        int secondaryNum = 0;

        try {
            if (s.length == 2) {
                secondaryNum = Integer.parseInt(s[1]);
            }
        } catch (final Exception e) {
            secondaryNum = CardFactoryUtil.xCount(c, c.getSVar(s[1]));
        }

        if (s[0].contains("Plus")) {
            return num + secondaryNum;
        } else if (s[0].contains("NMinus")) {
            return secondaryNum - num;
        } else if (s[0].contains("Minus")) {
            return num - secondaryNum;
        } else if (s[0].contains("Twice")) {
            return num * 2;
        } else if (s[0].contains("Thrice")) {
            return num * 3;
        } else if (s[0].contains("HalfUp")) {
            return (int) (Math.ceil(num / 2.0));
        } else if (s[0].contains("HalfDown")) {
            return (int) (Math.floor(num / 2.0));
        } else if (s[0].contains("ThirdUp")) {
            return (int) (Math.ceil(num / 3.0));
        } else if (s[0].contains("ThirdDown")) {
            return (int) (Math.floor(num / 3.0));
        } else if (s[0].contains("Negative")) {
            return num * -1;
        } else if (s[0].contains("Times")) {
            return num * secondaryNum;
        } else if (s[0].contains("DivideEvenlyDown")) {
            if (secondaryNum == 0) {
                return 0;
            } else {
                return num / secondaryNum;
            }
        } else if (s[0].contains("Mod")) {
            return num % secondaryNum;
        } else if (s[0].contains("LimitMax")) {
            if (num < secondaryNum) {
                return num;
            } else {
                return secondaryNum;
            }
        } else if (s[0].contains("LimitMin")) {
            if (num > secondaryNum) {
                return num;
            } else {
                return secondaryNum;
            }

        } else {
            return num;
        }
    }

    /**
     * <p>
     * doXMath.
     * </p>
     * 
     * @param num
     *            a int.
     * @param m
     *            an array of {@link java.lang.String} objects.
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int doXMath(final int num, final String[] m, final Card c) {
        if (m.length == 0) {
            return num;
        }

        return CardFactoryUtil.doXMath(num, m[0], c);
    }

    /**
     * <p>
     * handlePaid.
     * </p>
     * 
     * @param paidList
     *            a {@link forge.CardList} object.
     * @param string
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int handlePaid(final List<Card> paidList, final String string, final Card source) {
        if (paidList == null) {
            if (string.contains(".")) {
                final String[] splitString = string.split("\\.", 2);
                return CardFactoryUtil.doXMath(0, splitString[1], source);
            } else {
                return 0;
            }
        }
        if (string.startsWith("Amount")) {
            if (string.contains(".")) {
                final String[] splitString = string.split("\\.", 2);
                return CardFactoryUtil.doXMath(paidList.size(), splitString[1], source);
            } else {
                return paidList.size();
            }

        }
        if (string.startsWith("Valid")) {
            final String[] m = { "none" };

            String valid = string.substring(6);
            final String[] l;
            l = valid.split("/"); // separate the specification from any math
            valid = l[0];
            if (l.length > 1) {
                m[0] = l[1];
            }
            final List<Card> list = CardLists.getValidCards(paidList, valid, source.getController(), source);
            return CardFactoryUtil.doXMath(list.size(), m, source);
        }

        int tot = 0;
        for (final Card c : paidList) {
            tot += CardFactoryUtil.xCount(c, string);
        }

        return tot;
    }

    /**
     * <p>
     * inputUntapUpToNType.
     * </p>
     * 
     * @param n
     *            a int.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputUntapUpToNType(final int n, final String type) {
        final Input untap = new Input() {
            private static final long serialVersionUID = -2167059918040912025L;

            private final int stop = n;
            private int count = 0;
            private List<Card> choices = new ArrayList<Card>();;

            @Override
            public void showMessage() {
                final StringBuilder sb = new StringBuilder();
                sb.append("Select a ").append(type).append(" to untap");
                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(card);
                if (card.isType(type) && zone.is(ZoneType.Battlefield) && !choices.contains(card)) {
                    card.untap();
                    choices.add(card);
                    this.count++;
                    if (this.count == this.stop) {
                        this.stop();
                    }
                }
            } // selectCard()
        };

        return untap;
    }

    /**
     * <p>
     * isMostProminentColor.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a boolean.
     */
    public static boolean isMostProminentColor(final List<Card> list, final String color) {

        final Map<String, Integer> map = new HashMap<String, Integer>();

        for (final Card c : list) {
            for (final String color2 : CardUtil.getColors(c)) {
                if (color2.equals("colorless")) {
                    // nothing to do
                } else if (!map.containsKey(color2)) {
                    map.put(color2, 1);
                } else {
                    map.put(color2, map.get(color2) + 1);
                }
            }
        } // for

        if (map.isEmpty() || !map.containsKey(color)) {
            return false;
        }

        int num = map.get(color);

        for (final Entry<String, Integer> entry : map.entrySet()) {

            if (num < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * makeToken.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param imageName
     *            a {@link java.lang.String} object.
     * @param controller
     *            a {@link forge.game.player.Player} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param types
     *            an array of {@link java.lang.String} objects.
     * @param baseAttack
     *            a int.
     * @param baseDefense
     *            a int.
     * @param intrinsicKeywords
     *            an array of {@link java.lang.String} objects.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> makeToken(final String name, final String imageName, final Player controller,
            final String manaCost, final String[] types, final int baseAttack, final int baseDefense,
            final String[] intrinsicKeywords) {
        final List<Card> list = new ArrayList<Card>();
        final Card c = new Card();
        c.setName(name);
        c.setImageFilename(imageName);

        // TODO - most tokens mana cost is 0, this needs to be fixed
        // c.setManaCost(manaCost);
        c.addColor(manaCost);
        c.setToken(true);

        for (final String t : types) {
            c.addType(t);
        }

        c.setBaseAttack(baseAttack);
        c.setBaseDefense(baseDefense);

        final int multiplier = controller.getTokenDoublersMagnitude();
        for (int i = 0; i < multiplier; i++) {
            Card temp = CardFactory.copyStats(c);

            for (final String kw : intrinsicKeywords) {
                temp.addIntrinsicKeyword(kw);
            }
            temp.setOwner(controller);
            temp.setToken(true);
            CardFactoryUtil.parseKeywords(temp, temp.getName());
            CardFactoryUtil.setupKeywordedAbilities(temp);
            Singletons.getModel().getGame().getAction().moveToPlay(temp);
            list.add(temp);
        }

        Singletons.getModel().getGame().getEvents().post(new TokenCreatedEvent());

        return list;
    }

    /**
     * <p>
     * copyTokens.
     * </p>
     * 
     * @param tokenList
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> copyTokens(final List<Card> tokenList) {
        final List<Card> list = new ArrayList<Card>();

        for (Card thisToken : tokenList) {
            list.addAll(copySingleToken(thisToken));
        }

        return list;
    }

    public static List<Card> copySingleToken(Card thisToken) {
        final ArrayList<String> tal = thisToken.getType();
        final String[] tokenTypes = new String[tal.size()];
        tal.toArray(tokenTypes);

        final List<String> kal = thisToken.getIntrinsicKeyword();
        final String[] tokenKeywords = new String[kal.size()];
        kal.toArray(tokenKeywords);
        final List<Card> tokens = CardFactoryUtil.makeToken(thisToken.getName(), thisToken.getImageFilename(),
                thisToken.getController(), thisToken.getManaCost().toString(), tokenTypes, thisToken.getBaseAttack(),
                thisToken.getBaseDefense(), tokenKeywords);

        for (final Card token : tokens) {
            token.setColor(thisToken.getColor());
        }
        return tokens;
    }

    /**
     * <p>
     * getBushidoEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Ability> getBushidoEffects(final Card c) {
        final ArrayList<Ability> list = new ArrayList<Ability>();

        final Card crd = c;

        for (final String kw : c.getKeyword()) {
            if (kw.contains("Bushido")) {
                final String[] parse = kw.split(" ");
                final String s = parse[1];
                final int magnitude = Integer.parseInt(s);

                final Ability ability = new Ability(c, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final Command untilEOT = new Command() {

                            private static final long serialVersionUID = 3014846051064254493L;

                            @Override
                            public void execute() {
                                if (crd.isInPlay()) {
                                    crd.addTempAttackBoost(-1 * magnitude);
                                    crd.addTempDefenseBoost(-1 * magnitude);
                                }
                            }
                        };

                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);

                        crd.addTempAttackBoost(magnitude);
                        crd.addTempDefenseBoost(magnitude);
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(c);
                sb.append(" - (Bushido) gets +");
                sb.append(magnitude);
                sb.append("/+");
                sb.append(magnitude);
                sb.append(" until end of turn.");
                ability.setStackDescription(sb.toString());

                list.add(ability);
            }
        }
        return list;
    }

    /**
     * <p>
     * getNeededXDamage.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int getNeededXDamage(final SpellAbility ability) {
        // when targeting a creature, make sure the AI won't overkill on X
        // damage
        final Card target = ability.getTargetCard();
        int neededDamage = -1;

        if ((target != null)) {
            neededDamage = target.getNetDefense() - target.getDamage();
        }

        return neededDamage;
    }


    /**
     * <p>
     * playLandEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void playLandEffects(final Card c) {
        final Player player = c.getController();

        // > 0 because land amount isn't incremented until after playLandEffects
        final boolean extraLand = player.getNumLandsPlayed() > 0;

        if (extraLand) {
            final List<Card> fastbonds = player.getCardsIn(ZoneType.Battlefield, "Fastbond");
            for (final Card f : fastbonds) {
                final SpellAbility ability = new Ability(f, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        f.getController().addDamage(1, f);
                    }
                };
                ability.setStackDescription("Fastbond - Deals 1 damage to you.");

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * isNegativeCounter.
     * </p>
     * 
     * @param c
     *            a {@link forge.CounterType} object.
     * @return a boolean.
     */
    public static boolean isNegativeCounter(final CounterType c) {
        return (c == CounterType.AGE) || (c == CounterType.BLAZE) || (c == CounterType.BRIBERY) || (c == CounterType.DOOM)
                || (c == CounterType.ICE) || (c == CounterType.M1M1) || (c == CounterType.M0M2) || (c == CounterType.M0M1)
                || (c == CounterType.TIME);
    }

    public static void correctAbilityChainSourceCard(final SpellAbility sa, final Card card) {

        sa.setSourceCard(card);

        if (sa.getSubAbility() != null) {
            correctAbilityChainSourceCard(sa.getSubAbility(), card);
        }
    }

    /**
     * Adds the ability factory abilities.
     * 
     * @param card
     *            the card
     */
    public static final void addAbilityFactoryAbilities(final Card card) {
        // **************************************************
        // AbilityFactory cards
        final ArrayList<String> ia = card.getIntrinsicAbilities();
        for (int i = 0; i < ia.size(); i++) {
            // System.out.println(cardName);
            final SpellAbility sa = AbilityFactory.getAbility(ia.get(i), card);
            if (sa.hasParam("SetAsKicked")) {
                sa.addOptionalAdditionalCosts("Kicker");
            }
            card.addSpellAbility(sa);
        }

    }

    /**
     * <p>
     * postFactoryKeywords.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public static void setupKeywordedAbilities(final Card card) {
        // this function should handle any keywords that need to be added after
        // a spell goes through the factory
        // Cards with Cycling abilities
        // -1 means keyword "Cycling" not found

        // TODO - certain cards have two different kicker types, kicker will
        // need
        // to be written differently to handle this
        // TODO - kicker costs can only be mana right now i think?
        // TODO - this kicker only works for pemanents. maybe we can create an
        // optional cost class for buyback, kicker, that type of thing

        if (CardFactoryUtil.hasKeyword(card, "Multikicker") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Multikicker");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String[] k = parse.split("kicker ");

                final SpellAbility sa = card.getSpellAbility()[0];
                sa.setIsMultiKicker(true);
                sa.setMultiKickerManaCost(new ManaCost( new ManaCostParser(k[1])));
            }
        }

        if (CardFactoryUtil.hasKeyword(card, "Replicate") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Replicate");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String[] k = parse.split("cate ");

                final SpellAbility sa = card.getSpellAbility()[0];
                sa.setIsReplicate(true);
                sa.setReplicateManaCost(new ManaCost(new ManaCostParser(k[1])));
            }
        }

        final int evokePos = CardFactoryUtil.hasKeyword(card, "Evoke");
        if (evokePos != -1) {
            card.addSpellAbility(makeEvokeSpell(card, card.getKeyword().get(evokePos)));
        }

        if (CardFactoryUtil.hasKeyword(card, "Cycling") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Cycling");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String manacost = k[1];

                card.addSpellAbility(CardFactoryUtil.abilityCycle(card, manacost));
            }
        } // Cycling

        while (CardFactoryUtil.hasKeyword(card, "TypeCycling") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "TypeCycling");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String type = k[1];
                final String manacost = k[2];

                card.addSpellAbility(CardFactoryUtil.abilityTypecycle(card, manacost, type));
            }
        } // TypeCycling

        if (CardFactoryUtil.hasKeyword(card, "Transmute") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Transmute");
            if (n != -1) {
                final String parse = card.getKeyword().get(n);
                card.removeIntrinsicKeyword(parse);
                final String manacost = parse.split(":")[1];

                card.addSpellAbility(CardFactoryUtil.abilityTransmute(card, manacost));
            }
        } // transmute

        int shiftPos = CardFactoryUtil.hasKeyword(card, "Soulshift");
        while (shiftPos != -1) {
            final int n = shiftPos;
            final String parse = card.getKeyword().get(n);
            final String[] k = parse.split(" ");
            final int manacost = Integer.parseInt(k[1]);

            final String actualTrigger = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard"
                    + "| OptionalDecider$ You | ValidCard$ Card.Self | Execute$ SoulshiftAbility"
                    + "| TriggerController$ TriggeredCardController | TriggerDescription$ " + parse
                    + " (When this creature dies, you may return target Spirit card with converted mana cost "
                    + manacost + " or less from your graveyard to your hand.)";
            final String abString = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand"
                    + "| ValidTgts$ Spirit.YouOwn+cmcLE" + manacost;
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("SoulshiftAbility", abString);
            shiftPos = CardFactoryUtil.hasKeyword(card, "Soulshift", n + 1);
        } // Soulshift

        final int echoPos = CardFactoryUtil.hasKeyword(card, "Echo");
        if (echoPos != -1) {
            // card.removeIntrinsicKeyword(parse);
            final String[] k = card.getKeyword().get(echoPos).split(":");
            final String manacost = k[1];

            card.setEchoCost(manacost);

            final Command intoPlay = new Command() {

                private static final long serialVersionUID = -7913835645603984242L;

                @Override
                public void execute() {
                    card.addExtrinsicKeyword("(Echo unpaid)");
                }
            };
            card.addComesIntoPlayCommand(intoPlay);

        } // echo

        if (CardFactoryUtil.hasKeyword(card, "Suspend") != -1) {
            // Suspend:<TimeCounters>:<Cost>
            final int n = CardFactoryUtil.hasKeyword(card, "Suspend");
            if (n != -1) {
                final String parse = card.getKeyword().get(n);
                card.removeIntrinsicKeyword(parse);
                card.setSuspend(true);
                final String[] k = parse.split(":");

                final String timeCounters = k[1];
                final String cost = k[2];
                card.addSpellAbility(CardFactoryUtil.abilitySuspend(card, cost, timeCounters));
            }
        } // Suspend

        int xCount = card.getManaCost().getShardCount(ManaCostShard.X);
        if (xCount > 0) {
            final SpellAbility sa = card.getSpellAbility()[0];
            sa.setIsXCost(true);
            sa.setXManaCost(xCount);
        } // X

        if (CardFactoryUtil.hasKeyword(card, "Fading") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Fading");
            if (n != -1) {
                final String[] k = card.getKeyword().get(n).split(":");
                final int power = Integer.parseInt(k[1]);

                card.addComesIntoPlayCommand(CardFactoryUtil.fading(card, power));
            }
        } // Fading

        if (CardFactoryUtil.hasKeyword(card, "Vanishing") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Vanishing");
            if (n != -1) {
                final String[] k = card.getKeyword().get(n).split(":");
                final int power = Integer.parseInt(k[1]);

                card.addComesIntoPlayCommand(CardFactoryUtil.vanishing(card, power));
            }
        } // Vanishing

        // AddCost
        if (!card.getSVar("FullCost").equals("")) {
            final SpellAbility[] abilities = card.getSpellAbility();
            if ((abilities.length > 0) && abilities[0].isSpell()) {
                final String altCost = card.getSVar("FullCost");
                final Cost abCost = new Cost(card, altCost, abilities[0].isAbility());
                abilities[0].setPayCosts(abCost);
            }
        }

        // AltCost
        if (!card.getSVar("AltCost").equals("")) {
            final SpellAbility[] abilities = card.getSpellAbility();
            if ((abilities.length > 0) && abilities[0].isSpell()) {
                card.addSpellAbility(makeAltCost(card, abilities[0]));
            }
        }

        if (card.hasKeyword("Delve")) {
            card.getSpellAbilities().get(0).setDelve(true);
        }

        if (card.hasStartOfKeyword("Haunt")) {
            setupHauntSpell(card);
        }

        if (card.hasKeyword("Provoke")) {
            final String actualTrigger = "Mode$ Attacks | ValidCard$ Card.Self | "
                    + "OptionalDecider$ You | Execute$ ProvokeAbility | Secondary$ True | TriggerDescription$ "
                    + "When this attacks, you may have target creature defending player "
                    + "controls untap and block it if able.";
            final String abString = "DB$ MustBlock | ValidTgts$ Creature.DefenderCtrl | "
                    + "TgtPrompt$ Select target creature defending player controls | SubAbility$ DBUntap";
            final String dbString = "DB$ Untap | Defined$ Targeted";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("ProvokeAbility", abString);
            card.setSVar("DBUntap", dbString);
        }

        if (card.hasKeyword("Living Weapon")) {
            card.removeIntrinsicKeyword("Living Weapon");

            final StringBuilder sbTrig = new StringBuilder();
            sbTrig.append("Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ");
            sbTrig.append("ValidCard$ Card.Self | Execute$ TrigGerm | TriggerDescription$ ");
            sbTrig.append("Living Weapon (When this Equipment enters the battlefield, ");
            sbTrig.append("put a 0/0 black Germ creature token onto the battlefield, then attach this to it.)");

            final StringBuilder sbGerm = new StringBuilder();
            sbGerm.append("DB$ Token | TokenAmount$ 1 | TokenName$ Germ | TokenTypes$ Creature,Germ | RememberTokens$ True | ");
            sbGerm.append("TokenOwner$ You | TokenColors$ Black | TokenPower$ 0 | TokenToughness$ 0 | TokenImage$ B 0 0 Germ | SubAbility$ DBGermAttach");

            final StringBuilder sbAttach = new StringBuilder();
            sbAttach.append("DB$ Attach | Defined$ Remembered | SubAbility$ DBGermClear");

            final StringBuilder sbClear = new StringBuilder();
            sbClear.append("DB$ Cleanup | ClearRemembered$ True");

            card.setSVar("TrigGerm", sbGerm.toString());
            card.setSVar("DBGermAttach", sbAttach.toString());
            card.setSVar("DBGermClear", sbClear.toString());

            final Trigger etbTrigger = TriggerHandler.parseTrigger(sbTrig.toString(), card, true);
            card.addTrigger(etbTrigger);
        }

        if (card.hasKeyword("Epic")) {
            final SpellAbility newSA = makeEpic(card);

            card.clearSpellAbility();
            card.addSpellAbility(newSA);
        }

        if (card.hasKeyword("Soulbond")) {
            // Setup ETB trigger for card with Soulbond keyword
            final String actualTriggerSelf = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Card.Self | Execute$ TrigBondOther | OptionalDecider$ You | "
                    + "IsPresent$ Creature.Other+YouCtrl+NotPaired | Secondary$ True | "
                    + "TriggerDescription$ When CARDNAME enters the battlefield, "
                    + "you may pair CARDNAME with another unpaired creature you control";
            final String abStringSelf = "AB$ Bond | Cost$ 0 | Defined$ Self | ValidCards$ Creature.Other+YouCtrl+NotPaired";
            final Trigger parsedTriggerSelf = TriggerHandler.parseTrigger(actualTriggerSelf, card, true);
            card.addTrigger(parsedTriggerSelf);
            card.setSVar("TrigBondOther", abStringSelf);
            // Setup ETB trigger for other creatures you control
            final String actualTriggerOther = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Creature.Other+YouCtrl | TriggerZones$ Battlefield | OptionalDecider$ You | "
                    + "Execute$ TrigBondSelf | IsPresent$ Creature.Self+NotPaired | Secondary$ True | "
                    + " TriggerDescription$ When another unpaired creature you control enters the battlefield, "
                    + "you may pair it with CARDNAME";
            final String abStringOther = "AB$ Bond | Cost$ 0 | Defined$ TriggeredCard | ValidCards$ Creature.Self+NotPaired";
            final Trigger parsedTriggerOther = TriggerHandler.parseTrigger(actualTriggerOther, card, true);
            card.addTrigger(parsedTriggerOther);
            card.setSVar("TrigBondSelf", abStringOther);
        }

        if (card.hasKeyword("Extort")) {
            final String extortTrigger = "Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ You | "
                    + "TriggerZones$ Battlefield | Execute$ ExtortOpps | Secondary$ True"
                    + " | TriggerDescription$ Extort (Whenever you cast a spell, you may pay WB. If you do, "
                    + "each opponent loses 1 life and you gain that much life.)";
            final String abString = "AB$ LoseLife | Cost$ WB | Defined$ Player.Opponent | "
                    + "LifeAmount$ 1 | SubAbility$ DBGainLife";
            final String dbString = "DB$ GainLife | Defined$ You | LifeAmount$ AFLifeLost";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(extortTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("ExtortOpps", abString);
            card.setSVar("DBGainLife", dbString);
            card.setSVar("AFLifeLost", "Number$0");
        }

        if (card.hasKeyword("Evolve")) {
            final String evolveTrigger = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + " ValidCard$ Creature.YouCtrl+Other | EvolveCondition$ True | "
                    + "TriggerZones$ Battlefield | Execute$ EvolveAddCounter | Secondary$ True | "
                    + "TriggerDescription$ Evolve (Whenever a creature enters the battlefield under your "
                    + "control, if that creature has greater power or toughness than this creature, put a "
                    + "+1/+1 counter on this creature.)";
            final String abString = "AB$ PutCounter | Cost$ 0 | Defined$ Self | CounterType$ P1P1 | "
                    + "CounterNum$ 1";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(evolveTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("EvolveAddCounter", abString);
        }

        if (card.hasStartOfKeyword("Amplify")) {
            // find position of Amplify keyword
            final int equipPos = card.getKeywordPosition("Amplify");
            final String[] ampString = card.getKeyword().get(equipPos).split(":");
            final String amplifyMagnitude = ampString[1];
            final String suffix = !amplifyMagnitude.equals("1") ? "s" : "";
            final String ampTypes = ampString[2];
            String[] refinedTypes = ampTypes.split(",");
            final StringBuilder types = new StringBuilder();
            for (int i = 0; i < refinedTypes.length; i++) {
                types.append("Card.").append(refinedTypes[i]).append("+YouCtrl");
                if (i + 1 != refinedTypes.length) {
                    types.append(",");
                }
            }
            // Setup ETB trigger for card with Amplify keyword
            final String actualTrigger = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Card.Self | Execute$ AmplifyReveal | Static$ True | Secondary$ True | "
                    + "TriggerDescription$ As this creature enters the battlefield, put "
                    + amplifyMagnitude + " +1/+1 counter" + suffix + " on it for each "
                    + ampTypes.replace(",", " and/or ") + " card you reveal in your hand.)";
            final String abString = "AB$ Reveal | Cost$ 0 | AnyNumber$ True | RevealValid$ "
                    + types.toString() + " | RememberRevealed$ True | SubAbility$ Amplify";
            final String dbString = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | "
                    + "CounterNum$ AmpMagnitude | References$ Revealed,AmpMagnitude | SubAbility$ DBCleanup";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("AmplifyReveal", abString);
            card.setSVar("Amplify", dbString);
            card.setSVar("DBCleanup", "DB$ Cleanup | ClearRemembered$ True");
            card.setSVar("AmpMagnitude", "SVar$Revealed/Times." + amplifyMagnitude);
            card.setSVar("Revealed", "Remembered$Amount");
        }

        if (card.hasStartOfKeyword("Equip")) {
            // find position of Equip keyword
            final int equipPos = card.getKeywordPosition("Equip");
            // Check for additional params such as preferred AI targets
            final String equipString = card.getKeyword().get(equipPos).substring(5);
            final String[] equipExtras = equipString.contains("\\|") ? equipString.split("\\|", 2) : null;
            // Get cost string
            String equipCost = "";
            if (equipExtras != null) {
                equipCost = equipExtras[0].trim();
            } else {
                equipCost = equipString.trim();
            }
           // Create attach ability string
            final StringBuilder abilityStr = new StringBuilder();
            abilityStr.append("AB$ Attach | Cost$ ");
            abilityStr.append(equipCost);
            abilityStr.append(" | ValidTgts$ Creature.YouCtrl | TgtPrompt$ Select target creature you control ");
            abilityStr.append("| SorcerySpeed$ True | Equip$ True | AILogic$ Pump | IsPresent$ Card.Self+nonCreature ");
            if (equipExtras != null) {
                abilityStr.append("| ").append(equipExtras[1]).append(" ");
            }
            if (equipCost.matches(".+<.+>")) { //Something other than a mana cost
                abilityStr.append("| PrecostDesc$ Equip - | SpellDescription$ (Attach to target creature you control. Equip only as a sorcery.)");
            }
            else {
                abilityStr.append("| PrecostDesc$ Equip | SpellDescription$ (Attach to target creature you control. Equip only as a sorcery.)");
            }
            // instantiate attach ability
            final SpellAbility sa = AbilityFactory.getAbility(abilityStr.toString(), card);
            card.addSpellAbility(sa);
            // add ability to instrinic strings so copies/clones create the ability also
            card.getIntrinsicAbilities().add(abilityStr.toString());
        }

        setupEtbKeywords(card);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     */
    private static void setupEtbKeywords(final Card card) {
        for (String kw : card.getKeyword()) {

            if (kw.startsWith("ETBReplacement")) {
                String[] splitkw = kw.split(":");
                ReplacementLayer layer = ReplacementLayer.smartValueOf(splitkw[1]);
                SpellAbility repAb = AbilityFactory.getAbility(card.getSVar(splitkw[2]), card);
                String desc = repAb.getDescription();
                setupETBReplacementAbility(repAb);

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Description$ " + desc;
                if (splitkw.length == 4) {
                    if (splitkw[3].contains("Optional")) {
                        repeffstr += " | Optional$ True";
                    }
                }

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card);
                re.setLayer(layer);
                re.setOverridingAbility(repAb);

                card.addReplacementEffect(re);
            } else if (kw.startsWith("etbCounter")) {
                String parse = kw;
                card.removeIntrinsicKeyword(parse);

                String[] splitkw = parse.split(":");

                String desc = "CARDNAME enters the battlefield with " + splitkw[2] + " "
                        + CounterType.valueOf(splitkw[1]).getName() + " counters on it.";
                String extraparams = "";
                String amount = splitkw[2];
                if (splitkw.length > 3) {
                    if (!splitkw[3].equals("no Condition")) {
                        extraparams = splitkw[3];
                    }
                }
                if (splitkw.length > 4) {
                    desc = splitkw[4];
                }
                String abStr = "AB$ ChangeZone | Cost$ 0 | Hidden$ True | Origin$ All | Destination$ Battlefield"
                        + "| Defined$ ReplacedCard | SubAbility$ ETBCounterDBSVar";
                String dbStr = "DB$ PutCounter | Defined$ Self | CounterType$ " + splitkw[1] + " | CounterNum$ " + amount;
                try {
                    Integer.parseInt(amount);
                }
                catch (NumberFormatException ignored) {
                    dbStr += " | References$ " + amount;
                }
                card.setSVar("ETBCounterSVar", abStr);
                card.setSVar("ETBCounterDBSVar", dbStr);

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                        + "| ReplaceWith$ ETBCounterSVar | Description$ " + desc + (!extraparams.equals("") ? " | " + extraparams : "");

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card);
                re.setLayer(ReplacementLayer.Other);

                card.addReplacementEffect(re);
            } else if (kw.equals("CARDNAME enters the battlefield tapped.")) {
                String parse = kw;
                card.removeIntrinsicKeyword(parse);

                String abStr = "AB$ Tap | Cost$ 0 | Defined$ Self | ETB$ True | SubAbility$ MoveETB";
                String dbStr = "DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Battlefield"
                        + "| Defined$ ReplacedCard";

                card.setSVar("ETBTappedSVar", abStr);
                card.setSVar("MoveETB", dbStr);

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                        + "| ReplaceWith$ ETBTappedSVar | Description$ CARDNAME enters the battlefield tapped.";

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card);
                re.setLayer(ReplacementLayer.Other);

                card.addReplacementEffect(re);
            }
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @return
     */
    private static SpellAbility makeEpic(final Card card) {
        final SpellAbility origSA = card.getSpellAbilities().get(0);

        final SpellAbility newSA = new Spell(card, origSA.getPayCosts(), origSA.getTarget()) {
            private static final long serialVersionUID = -7934420043356101045L;

            @Override
            public void resolve() {
                final GameState game = Singletons.getModel().getGame();

                String name = card.toString() + " Epic";
                if (card.getController().getCardsIn(ZoneType.Battlefield, name).isEmpty()) {
                    // Create Epic emblem
                    final Card eff = new Card();
                    eff.setName(card.toString() + " Epic");
                    eff.addType("Effect"); // Or Emblem
                    eff.setToken(true); // Set token to true, so when leaving
                                        // play it gets nuked
                    eff.addController(card.getController());
                    eff.setOwner(card.getController());
                    eff.setColor(card.getColor());
                    eff.setImmutable(true);
                    eff.setEffectSource(card);

                    eff.addStaticAbility("Mode$ CantBeCast | ValidCard$ Card | Caster$ You "
                            + "| Description$ For the rest of the game, you can't cast spells.");

                    eff.setSVar("EpicCopy", "AB$ CopySpellAbility | Cost$ 0 | Defined$ EffectSource");

                    final Trigger copyTrigger = forge.card.trigger.TriggerHandler.parseTrigger(
                            "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | Execute$ EpicCopy | TriggerDescription$ "
                                    + "At the beginning of each of your upkeeps, copy " + card.toString()
                                    + " except for its epic ability.", eff, false);

                    eff.addTrigger(copyTrigger);

                    game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
                    game.getAction().moveToPlay(eff);
                    game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
                }

                if (card.getController().isHuman()) {
                    game.getActionPlay().playSpellAbilityNoStack(card.getController(), origSA, false);
                } else {
                    ComputerUtil.playNoStack((AIPlayer)card.getController(), origSA, game);
                }
            }
        };
        newSA.setDescription(origSA.getDescription());

        origSA.setPayCosts(null);
        origSA.setManaCost(ManaCost.ZERO);
        return newSA;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     */
    private static void setupHauntSpell(final Card card) {
        final int hauntPos = card.getKeywordPosition("Haunt");
        final String[] splitKeyword = card.getKeyword().get(hauntPos).split(":");
        final String hauntSVarName = splitKeyword[1];
        final String abilityDescription = splitKeyword[2];
        final String hauntAbilityDescription = abilityDescription.substring(0, 1).toLowerCase()
                + abilityDescription.substring(1);
        String hauntDescription;
        if (card.isCreature()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("When ").append(card.getName());
            sb.append(" enters the battlefield or the creature it haunts dies, ");
            sb.append(hauntAbilityDescription);
            hauntDescription = sb.toString();
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append("When the creature ").append(card.getName());
            sb.append(" haunts dies, ").append(hauntAbilityDescription);
            hauntDescription = sb.toString();
        }

        card.getKeyword().remove(hauntPos);

        // First, create trigger that runs when the haunter goes to the
        // graveyard
        final StringBuilder sbHaunter = new StringBuilder();
        sbHaunter.append("Mode$ ChangesZone | Origin$ Battlefield | ");
        sbHaunter.append("Destination$ Graveyard | ValidCard$ Card.Self | ");
        sbHaunter.append("Static$ True | Secondary$ True | TriggerDescription$ Blank");

        final Trigger haunterDies = forge.card.trigger.TriggerHandler
                .parseTrigger(sbHaunter.toString(), card, true);

        final Ability haunterDiesWork = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                this.getTargetCard().addHauntedBy(card);
                Singletons.getModel().getGame().getAction().exile(card);
            }
        };
        haunterDiesWork.setDescription(hauntDescription);

        final InputSelectManyCards target = new InputSelectManyCards(1,1) {
            private static final long serialVersionUID = 1981791992623774490L;

            @Override
            protected Input onDone() {
                haunterDiesWork.setTargetCard(selected.get(0));
                Singletons.getModel().getGame().getStack().add(haunterDiesWork);
                return null;
            }

            @Override
            protected boolean isValidChoice(Card c) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                if (!zone.is(ZoneType.Battlefield) || !c.isCreature()) {
                    return false;
                }
                return c.canBeTargetedBy(haunterDiesWork);
            }
        }; 
        target.setMessage("Choose target creature to haunt.");

        final Ability haunterDiesSetup = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                final List<Card> creats = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
                for (int i = 0; i < creats.size(); i++) {
                    if (!creats.get(i).canBeTargetedBy(this)) {
                        creats.remove(i);
                        i--;
                    }
                }
                if (creats.isEmpty()) {
                    return;
                }

                // need to do it this way because I don't know quite how to
                // make TriggerHandler respect BeforePayMana.
                if (card.getController().isHuman()) {
                    Singletons.getModel().getMatch().getInput().setInput(target);
                } else {
                    // AI choosing what to haunt
                    final List<Card> oppCreats = CardLists.filterControlledBy(creats, card.getController().getOpponent());
                    if (!oppCreats.isEmpty()) {
                        haunterDiesWork.setTargetCard(ComputerUtilCard.getWorstCreatureAI(oppCreats));
                    } else {
                        haunterDiesWork.setTargetCard(ComputerUtilCard.getWorstCreatureAI(creats));
                    }
                    Singletons.getModel().getGame().getStack().add(haunterDiesWork);
                }
            }
        };

        haunterDies.setOverridingAbility(haunterDiesSetup);

        // Second, create the trigger that runs when the haunted creature dies
        final StringBuilder sbDies = new StringBuilder();
        sbDies.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ");
        sbDies.append("ValidCard$ Creature.HauntedBy | Execute$ ").append(hauntSVarName);
        sbDies.append(" | TriggerDescription$ ").append(hauntDescription);

        final Trigger hauntedDies = forge.card.trigger.TriggerHandler.parseTrigger(sbDies.toString(), card, true);

        // Third, create the trigger that runs when the haunting creature
        // enters the battlefield
        final StringBuilder sbETB = new StringBuilder();
        sbETB.append("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ ");
        sbETB.append(hauntSVarName).append(" | Secondary$ True | TriggerDescription$ ");
        sbETB.append(hauntDescription);

        final Trigger haunterETB = forge.card.trigger.TriggerHandler.parseTrigger(sbETB.toString(), card, true);

        // Fourth, create a trigger that removes the haunting status if the
        // haunter leaves the exile
        final StringBuilder sbUnExiled = new StringBuilder();
        sbUnExiled.append("Mode$ ChangesZone | Origin$ Exile | Destination$ Any | ");
        sbUnExiled.append("ValidCard$ Card.Self | Static$ True | Secondary$ True | ");
        sbUnExiled.append("TriggerDescription$ Blank");

        final Trigger haunterUnExiled = forge.card.trigger.TriggerHandler.parseTrigger(sbUnExiled.toString(), card,
                true);

        final Ability haunterUnExiledWork = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                if (card.getHaunting() != null) {
                    card.getHaunting().removeHauntedBy(card);
                    card.setHaunting(null);
                }
            }
        };

        haunterUnExiled.setOverridingAbility(haunterUnExiledWork);

        // Fifth, add all triggers and abilities to the card.
        if (card.isCreature()) {
            card.addTrigger(haunterETB);
            card.addTrigger(haunterDies);
        } else {
            final String abString = card.getSVar(hauntSVarName).replace("AB$", "SP$")
                    .replace("Cost$ 0", "Cost$ " + card.getManaCost())
                    + " | SpellDescription$ " + abilityDescription;

            final SpellAbility sa = AbilityFactory.getAbility(abString, card);
            card.addSpellAbility(sa);
        }

        card.addTrigger(hauntedDies);
        card.addTrigger(haunterUnExiled);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param abilities
     * @return 
     */
    private static SpellAbility makeAltCost(final Card card, final SpellAbility sa) {
        String altCost = card.getSVar("AltCost");
        final HashMap<String, String> mapParams = new HashMap<String, String>();
        String altCostDescription = "";
        final String[] altCosts = altCost.split("\\|");

        for (int aCnt = 0; aCnt < altCosts.length; aCnt++) {
            altCosts[aCnt] = altCosts[aCnt].trim();
        }

        for (final String altCost2 : altCosts) {
            final String[] aa = altCost2.split("\\$");

            for (int aaCnt = 0; aaCnt < aa.length; aaCnt++) {
                aa[aaCnt] = aa[aaCnt].trim();
            }

            if (aa.length != 2) {
                final StringBuilder sb = new StringBuilder();
                sb.append("StaticEffectFactory Parsing Error: Split length of ");
                sb.append(altCost2).append(" in ").append(card.getName()).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParams.put(aa[0], aa[1]);
        }

        altCost = mapParams.get("Cost");

        if (mapParams.containsKey("Description")) {
            altCostDescription = mapParams.get("Description");
        }

        final SpellAbility altCostSA = sa.copy();

        final Cost abCost = new Cost(card, altCost, altCostSA.isAbility());
        altCostSA.setPayCosts(abCost);

        final StringBuilder sb = new StringBuilder();

        if (!altCostDescription.equals("")) {
            sb.append(altCostDescription);
        } else {
            sb.append("You may ").append(abCost.toStringAlt());
            sb.append(" rather than pay ").append(card.getName()).append("'s mana cost.");
        }

        final SpellAbilityRestriction restriction = new SpellAbilityRestriction();
        restriction.setRestrictions(mapParams);
        if (!mapParams.containsKey("ActivationZone")) {
            restriction.setZone(ZoneType.Hand);
        }
        altCostSA.setRestrictions(restriction);
        altCostSA.setDescription(sb.toString());
        altCostSA.setBasicSpell(false);

        return altCostSA;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param evokeKeyword
     * @return
     */
    private static SpellAbility makeEvokeSpell(final Card card, final String evokeKeyword) {
        final SpellAbility evokedSpell = new Spell(card) {
            private static final long serialVersionUID = -1598664196463358630L;

            @Override
            public void resolve() {
                card.setEvoked(true);
                Singletons.getModel().getGame().getAction().moveToPlay(card);
            }

            @Override
            public boolean canPlayAI() {
                if (!SpellPermanent.checkETBEffects(card, (AIPlayer) this.getActivatingPlayer())) {
                    return false;
                }
                return super.canPlayAI();
            }
        };
        card.removeIntrinsicKeyword(evokeKeyword);

        final String[] k = evokeKeyword.split(":");
        final String evokedCost = k[1];

        evokedSpell.setManaCost(new ManaCost(new ManaCostParser(evokedCost)));

        final StringBuilder desc = new StringBuilder();
        desc.append("Evoke ").append(evokedCost);
        desc.append(" (You may cast this spell for its evoke cost. ");
        desc.append("If you do, when it enters the battlefield, sacrifice it.)");

        evokedSpell.setDescription(desc.toString());

        final StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append(" (Evoked)");
        evokedSpell.setStackDescription(sb.toString());
        evokedSpell.setBasicSpell(false);
        return evokedSpell;
    }

    public static void setupETBReplacementAbility(SpellAbility sa) {
        SpellAbility tailend = sa;
        while (tailend.getSubAbility() != null) {
            tailend = tailend.getSubAbility();
        }

        tailend.setSubAbility(new AbilitySub(ApiType.InternalEtbReplacement, sa.getSourceCard(), null, null));
        // ETBReplacementMove(sa.getSourceCard(), null));
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param k
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static final int hasKeyword(final Card c, final String k) {
        final List<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).startsWith(k)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param k
     *            a {@link java.lang.String} object.
     * @param startPos
     *            a int.
     * @return a int.
     */
    static final int hasKeyword(final Card c, final String k, final int startPos) {
        final List<String> a = c.getKeyword();
        for (int i = startPos; i < a.size(); i++) {
            if (a.get(i).startsWith(k)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * parseKeywords.
     * </p>
     * Pulling out the parsing of keywords so it can be used by the token
     * generator
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * 
     */
    public static final void parseKeywords(final Card card, final String cardName) {
        if (card.hasKeyword("CARDNAME enters the battlefield tapped unless you control two or fewer other lands.")) {
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 6436821515525468682L;

                @Override
                public void execute() {
                    final List<Card> lands = card.getController().getLandsInPlay();
                    lands.remove(card);
                    if (!(lands.size() <= 2)) {
                        // it enters the battlefield this way, and should not
                        // fire triggers
                        card.setTapped(true);
                    }
                }
            });
        }
        if (CardFactoryUtil.hasKeyword(card, "CARDNAME enters the battlefield tapped unless you control a") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card,
                    "CARDNAME enters the battlefield tapped unless you control a");
            final String parse = card.getKeyword().get(n).toString();

            String splitString;
            if (parse.contains(" or a ")) {
                splitString = " or a ";
            } else {
                splitString = " or an ";
            }

            final String[] types = parse.substring(60, parse.length() - 1).split(splitString);

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 403635232455049834L;

                @Override
                public void execute() {
                    final List<Card> clICtrl = card.getOwner().getCardsIn(ZoneType.Battlefield);

                    boolean fnd = false;

                    for (int i = 0; i < clICtrl.size(); i++) {
                        final Card c = clICtrl.get(i);
                        for (final String type : types) {
                            if (c.isType(type.trim())) {
                                fnd = true;
                            }
                        }
                    }

                    if (!fnd) {
                        // it enters the battlefield this way, and should not
                        // fire triggers
                        card.setTapped(true);
                    }
                }
            });
        }
        if (CardFactoryUtil.hasKeyword(card, "Sunburst") != -1) {
            final Command sunburstCIP = new Command() {
                private static final long serialVersionUID = 1489845860231758299L;

                @Override
                public void execute() {
                    if (card.isCreature()) {
                        card.addCounter(CounterType.P1P1, card.getSunburstValue(), true);
                    } else {
                        card.addCounter(CounterType.CHARGE, card.getSunburstValue(), true);
                    }

                }
            };

            final Command sunburstLP = new Command() {
                private static final long serialVersionUID = -7564420917490677427L;

                @Override
                public void execute() {
                    card.setSunburstValue(0);
                }
            };

            card.addComesIntoPlayCommand(sunburstCIP);
            card.addLeavesPlayCommand(sunburstLP);
        }

        // Enforce the "World rule"
        if (card.isType("World")) {
            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 6536398032388958127L;

                @Override
                public void execute() {
                    final List<Card> cardsInPlay = CardLists.getType(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), "World");
                    cardsInPlay.remove(card);
                    for (int i = 0; i < cardsInPlay.size(); i++) {
                        Singletons.getModel().getGame().getAction().sacrificeDestroy(cardsInPlay.get(i));
                    }
                } // execute()
            }; // Command
            card.addComesIntoPlayCommand(intoPlay);
        }

        if (CardFactoryUtil.hasKeyword(card, "Morph") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Morph");
            if (n != -1) {

                final String parse = card.getKeyword().get(n).toString();
                card.setCanMorph(true);
                Map<String, String> sVars = card.getSVars();

                final String[] k = parse.split(":");
                final Cost cost = new Cost(card, k[1], true);

                card.addSpellAbility(CardFactoryUtil.abilityMorphDown(card));

                card.turnFaceDown();

                card.addSpellAbility(CardFactoryUtil.abilityMorphUp(card, cost));
                card.setSVars(sVars); // for Warbreak Trumpeter.

                card.setState(CardCharacteristicName.Original);
            }
        } // Morph

        if (CardFactoryUtil.hasKeyword(card, "Unearth") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Unearth");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");

                final String manacost = k[1];

                card.addSpellAbility(CardFactoryUtil.abilityUnearth(card, manacost));
                card.setUnearth(true);
            }
        } // unearth

        if (CardFactoryUtil.hasKeyword(card, "Madness") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Madness");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                card.setMadnessCost(k[1]);
            }
        } // madness

        if (CardFactoryUtil.hasKeyword(card, "Miracle") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Miracle");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                card.setMiracleCost(k[1]);
            }
        } // miracle

        if (CardFactoryUtil.hasKeyword(card, "Devour") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Devour");
            if (n != -1) {

                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String magnitude = k[1];

                // final String player = card.getController();
                final int[] numCreatures = new int[1];

                final Command intoPlay = new Command() {
                    private static final long serialVersionUID = -7530312713496897814L;

                    @Override
                    public void execute() {
                        final List<Card> creats = card.getController().getCreaturesInPlay();
                        creats.remove(card);
                        // System.out.println("Creats size: " + creats.size());

                        card.clearDevoured();
                        if (card.getController().isHuman()) {
                            if (creats.size() > 0) {
                                final List<Card> selection = GuiChoose.order("Devour", "Devouring", -1, creats, null, card);
                                numCreatures[0] = selection.size();

                                for (Object o : selection) {
                                    Card dinner = (Card) o;
                                    card.addDevoured(dinner);
                                    Singletons.getModel().getGame().getAction().sacrifice(dinner, null);
                                }
                            }
                        } // human
                        else {
                            int count = 0;
                            for (int i = 0; i < creats.size(); i++) {
                                final Card c = creats.get(i);
                                if ((c.getNetAttack() <= 1) && ((c.getNetAttack() + c.getNetDefense()) <= 3)) {
                                    card.addDevoured(c);
                                    Singletons.getModel().getGame().getAction().sacrifice(c, null);
                                    count++;
                                }
                            }
                            numCreatures[0] = count;
                        }
                        final int multiplier = magnitude.equals("X") ? AbilityUtils.calculateAmount(card, magnitude, null)
                                : Integer.parseInt(magnitude);
                        final int totalCounters = numCreatures[0] * multiplier;

                        card.addCounter(CounterType.P1P1, totalCounters, true);

                    }
                };
                card.addComesIntoPlayCommand(intoPlay);
            }
        } // Devour

        if (CardFactoryUtil.hasKeyword(card, "Modular") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Modular");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.getKeyword().remove(parse);

                final int m = Integer.parseInt(parse.substring(8));

                card.addIntrinsicKeyword("etbCounter:P1P1:" + m + ":no Condition:"
                        + "Modular " + m + " (This enters the battlefield with " + m + " +1/+1 counters on it. When it's put into a graveyard, you may put its +1/+1 counters on target artifact creature.)");

                final SpellAbility ability = new Ability(card, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final Card card2 = this.getTargetCard();
                        card2.addCounter(CounterType.P1P1, this.getSourceCard().getCounters(CounterType.P1P1), true);
                    } // resolve()
                };

                card.addDestroyCommand(new Command() {
                    private static final long serialVersionUID = 304026662487997331L;

                    @Override
                    public void execute() {
                        // Target as Modular is Destroyed
                        if (card.getController().isComputer()) {
                            List<Card> choices =
                                    CardLists.filter(card.getController().getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                                @Override
                                public boolean apply(final Card c) {
                                    return c.isCreature() && c.isArtifact();
                                }
                            });
                            if (choices.size() != 0) {
                                ability.setTargetCard(ComputerUtilCard.getBestCreatureAI(choices));

                                if (ability.getTargetCard() != null) {
                                    ability.setStackDescription("Put " + card.getCounters(CounterType.P1P1)
                                            + " +1/+1 counter/s from " + card + " on " + ability.getTargetCard());
                                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

                                }
                            }
                        } else {
                            Singletons.getModel().getMatch().getInput().setInput(CardFactoryUtil.modularInput(ability, card));
                        }
                    }
                });
            }
        } // Modular

        /*
         * WARNING: must keep this keyword processing before etbCounter keyword
         * processing.
         */
        final int graft = CardFactoryUtil.hasKeyword(card, "Graft");
        if (graft != -1) {
            final String parse = card.getKeyword().get(graft).toString();

            final int m = Integer.parseInt(parse.substring(6));
            final String abStr = "AB$ MoveCounter | Cost$ 0 | Source$ Self | "
                    + "Defined$ TriggeredCard | CounterType$ P1P1 | CounterNum$ 1";
            card.setSVar("GraftTrig", abStr);

            String trigStr = "Mode$ ChangesZone | ValidCard$ Creature.Other | "
                + "Origin$ Any | Destination$ Battlefield"
                + " | TriggerZones$ Battlefield | OptionalDecider$ You | "
                + "IsPresent$ Card.Self+counters_GE1_P1P1 | "
                + "Execute$ GraftTrig | TriggerDescription$ "
                + "Whenever another creature enters the battlefield, you "
                + "may move a +1/+1 counter from this creature onto it.";
            final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, true);
            card.addTrigger(myTrigger);

            card.addIntrinsicKeyword("etbCounter:P1P1:" + m);
        }

        final int bloodthirst = CardFactoryUtil.hasKeyword(card, "Bloodthirst");
        if (bloodthirst != -1) {
            final String numCounters = card.getKeyword().get(bloodthirst).split(" ")[1];
            String desc = "Bloodthirst "
                    + numCounters + " (If an opponent was dealt damage this turn, this creature enters the battlefield with "
                    + numCounters + " +1/+1 counters on it.)";
            if (numCounters.equals("X")) {
                desc = "Bloodthirst X (This creature enters the battlefield with X +1/+1 counters on it, "
                        + "where X is the damage dealt to your opponents this turn.)";
                card.setSVar("X", "Count$BloodthirstAmount");
            }

            card.addIntrinsicKeyword("etbCounter:P1P1:" + numCounters + ":Bloodthirst$ True:" + desc);
        } // bloodthirst

        final int storm = card.getKeywordAmount("Storm");
        for (int i = 0; i < storm; i++) {
            final StringBuilder trigScript = new StringBuilder(
                    "Mode$ SpellCast | ValidCard$ Card.Self | Execute$ Storm "
                            + "| TriggerDescription$ Storm (When you cast this spell, "
                            + "copy it for each spell cast before it this turn.)");

            card.setSVar("Storm", "AB$ CopySpellAbility | Cost$ 0 | Defined$ TriggeredSpellAbility | Amount$ StormCount");
            card.setSVar("StormCount", "Count$StormCount");
            final Trigger stormTrigger = TriggerHandler.parseTrigger(trigScript.toString(), card, true);

            card.addTrigger(stormTrigger);
        } // Storm
    }

} // end class CardFactoryUtil
