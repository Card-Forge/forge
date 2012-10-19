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
package forge.game.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.esotericsoftware.minlog.Log;

import forge.Card;

import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.replacement.ReplaceMoved;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.game.phase.CombatUtil;
import forge.game.zone.ZoneType;

/**
 * <p>
 * ComputerAI_General class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ComputerAIGeneral implements Computer {

    final private Player player;
    /**
     * <p>
     * Constructor for ComputerAI_General.
     * </p>
     */
    public ComputerAIGeneral(Player computerPlayer) {
        player = computerPlayer;
    }

    /**
     * <p>
     * main.
     * </p>
     */
    @Override
    public final void main() {
        ComputerUtil.chooseLandsToPlay(player);
        this.playSpellAbilitiesStackEmpty();
    } // main()


    /**
     * <p>
     * playCards.
     * </p>
     * 
     * @param phase
     *            a {@link java.lang.String} object.
     */
    private void playSpellAbilitiesStackEmpty() {
        final List<Card> list = getAvailableCards();

        final boolean nextPhase = ComputerUtil.playSpellAbilities(player, getSpellAbilities(list));

        if (nextPhase) {
            Singletons.getModel().getGame().getPhaseHandler().passPriority();
        }
    } // playCards()

    /**
     * <p>
     * hasACardGivingHaste.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean hasACardGivingHaste(final Player ai) {
        final List<Card> all = ai.getCardsIn(ZoneType.Battlefield);
        all.addAll(CardFactoryUtil.getExternalZoneActivationCards(ai));
        all.addAll(ai.getCardsIn(ZoneType.Hand));

        for (final Card c : all) {
            for (final SpellAbility sa : c.getSpellAbility()) {
                if (sa.getAbilityFactory() == null) {
                    continue;
                }
                final AbilityFactory af = sa.getAbilityFactory();
                final HashMap<String, String> abilityParams = af.getMapParams();
                if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")) {
                    continue;
                }
                if (abilityParams.containsKey("SP") && !abilityParams.get("SP").equals("Pump")) {
                    continue;
                }
                if (abilityParams.containsKey("KW") && abilityParams.get("KW").contains("Haste")) {
                    return true;
                }
            }
        }

        return false;
    } // hasACardGivingHaste

    /**
     * <p>
     * getAvailableSpellAbilities.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private List<Card> getAvailableCards() {

        final Player opp = player.getOpponent();
        List<Card> all = player.getCardsIn(ZoneType.Hand);
        all.addAll(player.getCardsIn(ZoneType.Battlefield));
        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            all.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        all.addAll(opp.getCardsIn(ZoneType.Exile));
        all.addAll(opp.getCardsIn(ZoneType.Battlefield));
        return all;
    }

    /**
     * <p>
     * hasETBTrigger.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean hasETBTrigger(final Card card) {
        for (final Trigger tr : card.getTriggers()) {
            final HashMap<String, String> params = tr.getMapParams();
            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            return true;
        }
        return false;
    }
    
    /**
     * <p>
     * hasETBTrigger.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean hasETBReplacement(final Card card) {
        for (final ReplacementEffect re : card.getReplacementEffects()) {
            final HashMap<String, String> params = re.getMapParams();
            if (!(re instanceof ReplaceMoved)) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * <p>
     * getPossibleETBCounters.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPossibleETBCounters() {

        final Player opp = player.getOpponent();
        List<Card> all = player.getCardsIn(ZoneType.Hand);
        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            all.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        all.addAll(opp.getCardsIn(ZoneType.Exile));

        final ArrayList<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : all) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                if (sa instanceof SpellPermanent) {
                    // TODO ArsenalNut (13 Oct 2012) added line to set activating player to fix NPE problem
                    // in checkETBEffects.  There is SpellPermanent.checkETBEffects where the player can be
                    // directly input but it is currently a private method.
                    sa.setActivatingPlayer(player);
                    if (SpellPermanent.checkETBEffects(c, sa, "Counter")) {
                        spellAbilities.add(sa);
                    }
                }
            }
        }
        return spellAbilities;
    }

    /**
     * Returns the spellAbilities from the card list.
     * 
     * @param l
     *            a {@link forge.CardList} object.
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    private ArrayList<SpellAbility> getSpellAbilities(final List<Card> l) {
        final ArrayList<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                spellAbilities.add(sa);
            }
        }
        return spellAbilities;
    }

    /**
     * <p>
     * getPlayableCounters.
     * </p>
     * 
     * @param l
     *            a {@link forge.CardList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPlayableCounters(final List<Card> l) {
        final ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                // Check if this AF is a Counterpsell
                if ((sa.getAbilityFactory() != null) && sa.getAbilityFactory().getAPI().equals("Counter")) {
                    spellAbility.add(sa);
                }
            }
        }

        return spellAbility;
    }

    /**
     * <p>
     * declare_attackers.
     * </p>
     */
    @Override
    public final void declareAttackers() {
        // 12/2/10(sol) the decision making here has moved to getAttackers()

        Singletons.getModel().getGame().setCombat(ComputerUtil.getAttackers(player));

        final List<Card> att = Singletons.getModel().getGame().getCombat().getAttackers();
        if (!att.isEmpty()) {
            Singletons.getModel().getGame().getPhaseHandler().setCombat(true);
        }

        for (final Card element : att) {
            // tapping of attackers happens after Propaganda is paid for
            final StringBuilder sb = new StringBuilder();
            sb.append("Computer just assigned ");
            sb.append(element.getName()).append(" as an attacker.");
            Log.debug(sb.toString());
        }

        player.getZone(ZoneType.Battlefield).updateObservers();

        Singletons.getModel().getGame().getPhaseHandler().setNeedToNextPhase(true);
    }

    /**
     * <p>
     * declare_blockers.
     * </p>
     */
    @Override
    public final void declareBlockers() {
        final List<Card> blockers = player.getCreaturesInPlay();

        Singletons.getModel().getGame().setCombat(ComputerUtilBlock.getBlockers(player, Singletons.getModel().getGame().getCombat(), blockers));
        
        CombatUtil.orderMultipleCombatants(Singletons.getModel().getGame().getCombat());

        Singletons.getModel().getGame().getPhaseHandler().setNeedToNextPhase(true);
    }

    /**
     * <p>
     * end_of_turn.
     * </p>
     */
    @Override
    public final void endOfTurn() {
        //This is only called in the computer turn
        //this.playSpellAbilitiesStackEmpty();
        Singletons.getModel().getGame().getPhaseHandler().passPriority();
    }

    /**
     * <p>
     * stack_not_empty.
     * </p>
     */
    @Override
    public final void playSpellAbilities() {
        if (Singletons.getModel().getGame().getStack().isEmpty()) {
            this.playSpellAbilitiesStackEmpty();
            return;
        }

        // if top of stack is owned by me
        if (Singletons.getModel().getGame().getStack().peekInstance().getActivatingPlayer().isComputer()) {
            // probably should let my stuff resolve to force Human to respond to
            // it
            Singletons.getModel().getGame().getPhaseHandler().passPriority();
            return;
        }
        final List<Card> cards = getAvailableCards();
        // top of stack is owned by human,
        ArrayList<SpellAbility> possibleCounters = getPlayableCounters(cards);

        if ((possibleCounters.size() > 0) && ComputerUtil.playCounterSpell(player, possibleCounters)) {
            // Responding CounterSpell is on the Stack trying to Counter the Spell
            // If playCounterSpell returns true, a Spell is hitting the Stack
            return;
        }

        possibleCounters.clear();
        possibleCounters = this.getPossibleETBCounters();
        if ((possibleCounters.size() > 0) && !ComputerUtil.playSpellAbilities(player, possibleCounters)) {
            // Responding Permanent w/ ETB Counter is on the Stack
            // If playSpellAbilities returns false, a Spell is hitting the Stack
            return;
        }
        final ArrayList<SpellAbility> sas = this.getSpellAbilities(cards);
        if (sas.size() > 0) {
            // Spell not Countered
            if (!ComputerUtil.playSpellAbilities(player, sas)) {
                return;
            }
        }
        // if this hasn't been covered above, just PassPriority()
        Singletons.getModel().getGame().getPhaseHandler().passPriority();
    }
}
