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
package forge.card.spellability;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.Command;
import forge.Singletons;
import forge.card.SpellManaCost;
import forge.card.ability.AbilityFactory;
import forge.card.ability.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.replacement.ReplaceMoved;
import forge.card.replacement.ReplacementEffect;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.game.GlobalRuleChange;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.view.ButtonUtil;

/**
 * <p>
 * Spell_Permanent class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellPermanent extends Spell {
    /** Constant <code>serialVersionUID=2413495058630644447L</code>. */
    private static final long serialVersionUID = 2413495058630644447L;

    private boolean willChampion = false;
    private String championValid = null;
    private String championValidDesc = "";

    /** The champion input comes. */
    private final Input championInputComes = new Input() {
        private static final long serialVersionUID = -7503268232821397107L;

        @Override
        public void showMessage() {
            final List<Card> choice = SpellPermanent.this.championGetCreature.get();

            this.stopSetNext(CardFactoryUtil.inputTargetChampionSac(SpellPermanent.this.getSourceCard(),
                    SpellPermanent.this.championAbilityComes, choice, "Select another "
                            + SpellPermanent.this.championValidDesc + " you control to exile", false, false));
            ButtonUtil.disableAll(); // target this card means: sacrifice this
                                     // card
        }
    };

    private final Supplier<List<Card>> championGetCreature = new Supplier<List<Card>>() {
        @Override
        public List<Card> get() {
            final List<Card> cards = SpellPermanent.this.getSourceCard().getController().getCardsIn(ZoneType.Battlefield);
            return CardLists.getValidCards(cards, SpellPermanent.this.championValid, SpellPermanent.this.getSourceCard()
                    .getController(), SpellPermanent.this.getSourceCard());
        }
    }; // CommandReturn

    /** The champion ability comes. */
    private final SpellAbility championAbilityComes = new Ability(this.getSourceCard(), SpellManaCost.ZERO) {
        @Override
        public void resolve() {

            final Card source = this.getSourceCard();
            final Player controller = source.getController();

            final List<Card> creature = SpellPermanent.this.championGetCreature.get();
            if (creature.size() == 0) {
                Singletons.getModel().getGame().getAction().sacrifice(source, null);
                return;
            } else if (controller.isHuman()) {
                Singletons.getModel().getMatch().getInput().setInput(SpellPermanent.this.championInputComes);
            } else { // Computer
                List<Card> computer =
                        CardLists.getValidCards(controller.getCardsIn(ZoneType.Battlefield), SpellPermanent.this.championValid, controller, source);
                computer.remove(source);

                CardLists.shuffle(computer);
                if (computer.size() != 0) {
                    final Card c = computer.get(0);
                    source.setChampionedCard(c);
                    if (c.isInPlay()) {
                        Singletons.getModel().getGame().getAction().exile(c);
                    }

                    // Run triggers
                    final HashMap<String, Object> runParams = new HashMap<String, Object>();
                    runParams.put("Card", source);
                    runParams.put("Championed", source.getChampionedCard());
                    Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.Championed, runParams, false);
                } else {
                    Singletons.getModel().getGame().getAction().sacrifice(this.getSourceCard(), null);
                }
            } // computer
        } // resolve()
    };

    /** The champion command comes. */
    private final Command championCommandComes = new Command() {

        private static final long serialVersionUID = -3580408066322945328L;

        @Override
        public void execute() {
            final StringBuilder sb = new StringBuilder();
            sb.append(SpellPermanent.this.getSourceCard()).append(
                    " - When CARDNAME enters the battlefield, sacrifice it unless you exile a creature you control.");
            SpellPermanent.this.championAbilityComes.setStackDescription(sb.toString());
            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(SpellPermanent.this.championAbilityComes);
        } // execute()
    }; // championCommandComes

    /** The champion command leaves play. */
    private final Command championCommandLeavesPlay = new Command() {

        private static final long serialVersionUID = -5903638227914705191L;

        @Override
        public void execute() {

            final SpellAbility ability = new Ability(SpellPermanent.this.getSourceCard(), SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    final Card c = this.getSourceCard().getChampionedCard();
                    if ((c != null) && !c.isToken() && Singletons.getModel().getGame().isCardExiled(c)) {
                        Singletons.getModel().getGame().getAction().moveToPlay(c);
                    }
                } // resolve()
            }; // SpellAbility

            final StringBuilder sb = new StringBuilder();
            sb.append(SpellPermanent.this.getSourceCard()).append(
                    " - When CARDNAME leaves the battlefield, exiled card returns to the battlefield.");
            ability.setStackDescription(sb.toString());

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);
        } // execute()
    }; // championCommandLeavesPlay

    // /////
    // //////////////////

    /**
     * <p>
     * Constructor for Spell_Permanent.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     */
    public SpellPermanent(final Card sourceCard) {
        // Add Costs for all SpellPermanents
        this(sourceCard, new Cost(sourceCard, sourceCard.getManaCost(), false), null);
    } // Spell_Permanent()

    /**
     * <p>
     * Constructor for Spell_Permanent.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     */
    public SpellPermanent(final Card sourceCard, final Cost cost, final Target tgt) {
        this(sourceCard, cost, tgt, true);
    } // Spell_Permanent()

    /**
     * Instantiates a new spell_ permanent.
     * 
     * @param sourceCard
     *            the source card
     * @param cost
     *            the cost
     * @param tgt
     *            the tgt
     * @param setDesc
     *            the set desc
     */
    public SpellPermanent(final Card sourceCard, final Cost cost, final Target tgt, final boolean setDesc) {
        super(sourceCard, cost, tgt);

        if (CardFactoryUtil.hasKeyword(sourceCard, "Champion") != -1) {
            final int n = CardFactoryUtil.hasKeyword(sourceCard, "Champion");

            final String toParse = sourceCard.getKeyword().get(n).toString();
            final String[] parsed = toParse.split(":");
            this.willChampion = true;
            this.championValid = parsed[1];
            if (parsed.length > 2) {
                this.championValidDesc = parsed[2];
            } else {
                this.championValidDesc = this.championValid;
            }
        }

        if (sourceCard.isCreature()) {
            final StringBuilder sb = new StringBuilder();
            sb.append(sourceCard.getName()).append(" - Creature ").append(sourceCard.getNetAttack());
            sb.append(" / ").append(sourceCard.getNetDefense());
            this.setStackDescription(sb.toString());
        } else {
            this.setStackDescription(sourceCard.getName());
        }

        if (setDesc) {
            this.setDescription(this.getStackDescription());
        }

        if (this.willChampion) {
            sourceCard.addComesIntoPlayCommand(this.championCommandComes);
            sourceCard.addLeavesPlayCommand(this.championCommandLeavesPlay);
        }

        if (this.getManaCost().countX() > 0) {

            if (!this.getSourceCard().getSVar("X").equals("")) {
                this.setSVar("X", this.getSourceCard().getSVar("X"));
            }
        }

    } // Spell_Permanent()

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        final Card source = this.getSourceCard();

        final Player turn = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        if (source.getName().equals("Serra Avenger")) {
            if (turn.equals(source.getController()) && (turn.getTurn() <= 3)) {
                return false;
            }
        }

        // Flash handled by super.canPlay
        return super.canPlay();
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlayAI() {

        final Card card = this.getSourceCard();
        SpellManaCost mana = this.getPayCosts().getTotalMana();
        Player ai = getActivatingPlayer();
        if (mana.countX() > 0) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(this, ai);
            if (xPay <= 0) {
                return false;
            }
            card.setSVar("PayX", Integer.toString(xPay));
        }
        // Wait for Main2 if possible
        if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.MAIN1)
                && !ComputerUtil.castPermanentInMain1(ai, this)
                && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)) {
            return false;
        }
        // save cards with flash for surprise blocking
        if (card.hasKeyword("Flash")
                && !card.hasETBTrigger()
                && (Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)
                     || Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY))) {
            return false;
        }
        // Prevent the computer from summoning Ball Lightning type creatures after attacking
        if (card.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.")
                && (Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai.getOpponent())
                     || Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS))) {
            return false;
        }

        // Prevent the computer from summoning Ball Lightning type creatures after attacking
        if (card.hasStartOfKeyword("You may cast CARDNAME as though it had flash. If")
                && !card.getController().couldCastSorcery(this)) {
            return false;
        }

        return canPlayFromEffectAI(false, true);
    } // canPlayAI()

    /** {@inheritDoc} */
    @Override
    public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
        if (mandatory) {
            return true;
        }
        final AIPlayer ai = (AIPlayer) getActivatingPlayer();
        final Card card = this.getSourceCard();
        SpellManaCost mana = this.getPayCosts().getTotalMana();
        final Cost cost = this.getPayCosts();

        if (cost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, cost, card, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, cost, card)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, cost, card)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(cost, card)) {
                return false;
            }
        }

        // check on legendary
        if (card.isType("Legendary")
                && !Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {
            final List<Card> list = ai.getCardsIn(ZoneType.Battlefield);
            if (Iterables.any(list, CardPredicates.nameEquals(card.getName()))) {
                return false;
            }
        }
        if (card.isPlaneswalker()) {
            List<Card> list = ai.getCardsIn(ZoneType.Battlefield);
            list = CardLists.filter(list, CardPredicates.Presets.PLANEWALKERS);

            for (int i = 0; i < list.size(); i++) {
                List<String> type = card.getType();
                final String subtype = type.get(type.size() - 1);
                final List<Card> cl = CardLists.getType(list, subtype);

                if (cl.size() > 0) {
                    return false;
                }
            }
        }
        if (card.isType("World")) {
            List<Card> list = ai.getCardsIn(ZoneType.Battlefield);
            list = CardLists.getType(list, "World");
            if (list.size() > 0) {
                return false;
            }
        }

        if (card.isCreature() && (card.getNetDefense() <= 0) && !card.hasStartOfKeyword("etbCounter")
                && mana.countX() == 0 && !card.hasETBTrigger()
                && !card.hasETBReplacement()) {
            return false;
        }

        if (this.willChampion) {
            final Object o = this.championGetCreature.get();
            if (o == null) {
                return false;
            }

            final List<Card> cl = this.championGetCreature.get();
            if (!(cl.size() > 0) || !this.getSourceCard().isInZone(ZoneType.Hand)) {
                return false;
            }
        }
        if (!SpellPermanent.checkETBEffects(card, this, null, ai)) {
            return false;
        }
        return super.canPlayAI();
    }

    public static boolean checkETBEffects(final Card card, final SpellAbility sa, final ApiType api) {
        return checkETBEffects(card, sa, api, null);
    }

    public static boolean checkETBEffects(final Card card, final AIPlayer ai) {
        return checkETBEffects(card, null, null, ai);
    }

    private static boolean checkETBEffects(final Card card, final SpellAbility sa, final ApiType api, final AIPlayer ai) {
        boolean rightapi = false;

        if (card.isCreature()
                && Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noCreatureETBTriggers)) {
            return api == null;
        }

        // Trigger play improvements
        for (final Trigger tr : card.getTriggers()) {
            // These triggers all care for ETB effects

            final HashMap<String, String> params = tr.getMapParams();
            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard")) {
                if (!params.get("ValidCard").contains("Self")) {
                    continue;
                }
                if (params.get("ValidCard").contains("notkicked")) {
                    if (sa.isKicked()) {
                        continue;
                    }
                } else if (params.get("ValidCard").contains("kicked")) {
                    if (!params.get("ValidCard").contains("kicked ")) {
                        if (!sa.isKicked()) {
                            continue;
                        }
                    } else {
                        String s = "Kicker " + params.get("ValidCard").split("kicked ")[1];
                        boolean rightKicker = false;
                        if (sa.getOptionalAdditionalCosts() != null) {
                            for (String string : sa.getOptionalAdditionalCosts()) {
                                if (string.startsWith(s)) {
                                    rightKicker = true;
                                }
                            }
                        }
                        if (!rightKicker) {
                            continue;
                        }
                    }

                }
            }

            if (!tr.requirementsCheck()) {
                continue;
            }

            if (tr.getOverridingAbility() != null) {
                // Abilities yet
                continue;
            }

            // Maybe better considerations
            final String execute = params.get("Execute");
            if (execute == null) {
                continue;
            }
            final SpellAbility exSA = AbilityFactory.getAbility(card.getSVar(execute), card);

            if (api != null) {
                if (exSA.getApi() != api) {
                    continue;
                } else {
                    rightapi = true;
                }
            }

            if (sa != null) {
                exSA.setActivatingPlayer(sa.getActivatingPlayer());
            }
            else if (ai != null) {
                exSA.setActivatingPlayer(ai);
            }
            else {
                throw new InvalidParameterException("Either ai or sa must be not null!");
            }

            // Run non-mandatory trigger.
            // These checks only work if the Executing SpellAbility is an
            // Ability_Sub.
            if ((exSA instanceof AbilitySub) && !exSA.doTrigger(false, ai)) {
                // AI would not run this trigger if given the chance

                // if trigger is mandatory, return false
                if (params.get("OptionalDecider") == null) {
                    return false;
                }
                // else
                // otherwise, return false 50% of the time?
            }
        }
        if (api != null && !rightapi) {
            return false;
        }

        // Replacement effects
        for (final ReplacementEffect re : card.getReplacementEffects()) {
            // These Replacements all care for ETB effects

            final HashMap<String, String> params = re.getMapParams();
            if (!(re instanceof ReplaceMoved)) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard")) {
                if (!params.get("ValidCard").contains("Self")) {
                    continue;
                }
                if (params.get("ValidCard").contains("notkicked")) {
                    if (sa.isKicked()) {
                        continue;
                    }
                } else if (params.get("ValidCard").contains("kicked")) {
                    if (!params.get("ValidCard").contains("kicked ")) {
                        if (!sa.isKicked()) {
                            continue;
                        }
                    } else {
                        String s = "Kicker " + params.get("ValidCard").split("kicked ")[1];
                        boolean rightKicker = false;
                        if (sa.getOptionalAdditionalCosts() != null) {
                            for (String string : sa.getOptionalAdditionalCosts()) {
                                if (string.startsWith(s)) {
                                    rightKicker = true;
                                }
                            }
                        }
                        if (!rightKicker) {
                            continue;
                        }
                    }
                }
            }

            if (!re.requirementsCheck()) {
                continue;
            }
            final SpellAbility exSA = re.getOverridingAbility();

            if (exSA != null) {
                if (sa != null) {
                    exSA.setActivatingPlayer(sa.getActivatingPlayer());
                }
                else if (ai != null) {
                    exSA.setActivatingPlayer(ai);
                }
                else {
                    throw new InvalidParameterException("Either ai or sa must be not null!");
                }

                if (exSA.getActivatingPlayer() == null) {
                    throw new InvalidParameterException("Executing SpellAbility for Replacement Effect has no activating player");
                }
            }

            // ETBReplacement uses overriding abilities.
            // These checks only work if the Executing SpellAbility is an
            // Ability_Sub.
            if (exSA != null && (exSA instanceof AbilitySub) && !exSA.doTrigger(false, ai)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void resolve() {
        Card c = this.getSourceCard();
        c.addController(this.getActivatingPlayer());
        Singletons.getModel().getGame().getAction().moveTo(ZoneType.Battlefield, c);
    }
}
