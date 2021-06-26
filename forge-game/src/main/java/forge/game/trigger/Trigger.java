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
package forge.game.trigger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.IHasSVars;
import forge.game.TriggerReplacementBase;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.TextUtil;

/**
 * <p>
 * Abstract Trigger class. Constructed by reflection only
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public abstract class Trigger extends TriggerReplacementBase {
    private static int maxId = 0;
    private static int nextId() { return ++maxId; }

    /**
     * <p>
     * resetIDs.
     * </p>
     */
    public static void resetIDs() {
        Trigger.maxId = 50000;
    }

    /** The ID. */
    private int id;

    private TriggerType mode;

    private List<Object> triggerRemembered = Lists.newArrayList();

    // number of times this trigger was activated this this turn
    // used to handle once-per-turn triggers like Crawling Sensation
    private int numberTurnActivations = 0;

    private Set<PhaseType> validPhases;

    private SpellAbility spawningAbility = null;

    /**
     * <p>
     * Constructor for Trigger.
     * </p>
     *
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger(final Map<String, String> params, final Card host, final boolean intrinsic) {
        this.id = nextId();
        this.intrinsic = intrinsic;

        this.originalMapParams.putAll(params);
        this.mapParams.putAll(params);
        this.setHostCard(host);

        String triggerZones = getParam("TriggerZones");
        if (null != triggerZones) {
            setActiveZone(EnumSet.copyOf(ZoneType.listValueOf(triggerZones)));
        }

        String triggerPhases = getParam("Phase");
        if (null != triggerPhases) {
            setTriggerPhases(PhaseType.parseRange(triggerPhases));
        }
    }

    /**
     * <p>
     * toString.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
    	return toString(false);
    }

    public String toString(boolean active) {
        if (hasParam("TriggerDescription") && !this.isSuppressed()) {

            StringBuilder sb = new StringBuilder();
            String currentName;
            if (this.isIntrinsic() && !this.getHostCard().isMutated() && cardState != null) {
                currentName = cardState.getName();
            }
            else {
                currentName = getHostCard().getName();
            }
            String desc = getParam("TriggerDescription");
            if (!desc.contains("ABILITY")) {
                desc = CardTranslation.translateSingleDescriptionText(getParam("TriggerDescription"), currentName);
                desc = TextUtil.fastReplace(desc,"CARDNAME", CardTranslation.getTranslatedName(currentName));
                desc = TextUtil.fastReplace(desc,"NICKNAME", Lang.getInstance().getNickName(CardTranslation.getTranslatedName(currentName)));
            }
            if (getHostCard().getEffectSource() != null) {
                if(active)
                    desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", getHostCard().getEffectSource().toString());
                else
                    desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", getHostCard().getEffectSource().getName());
            }
            sb.append(desc);
            if (!this.triggerRemembered.isEmpty()) {
                sb.append(" (").append(this.triggerRemembered).append(")");
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public final String replaceAbilityText(final String desc, final CardState state) {
        // this function is for ABILITY
        if (!desc.contains("ABILITY")) {
            return desc;
        }
        SpellAbility sa = ensureAbility();

        return replaceAbilityText(desc, sa);

    }

    public final String replaceAbilityText(final String desc, SpellAbility sa) {
        String result = desc;

        // this function is for ABILITY
        if (!result.contains("ABILITY")) {
            return result;
        }
        if (sa == null) {
            sa = getOverridingAbility();
        }
        if (sa != null) {
            String saDesc;
            // if sa is a wrapper, get the Wrapped Ability
            if (sa.isWrapper()) {
                final WrappedAbility wa = (WrappedAbility) sa;
                sa = wa.getWrappedAbility();

                // wrapped Charm spells are special,
                // only get the selected abilities
                if (ApiType.Charm.equals(sa.getApi())) {
                    saDesc = sa.getStackDescription();
                } else {
                    saDesc = sa.getDescription();
                }
            } else if (ApiType.Charm.equals(sa.getApi())) {
                // use special formating, can be used in Card Description
                saDesc = CharmEffect.makeFormatedDescription(sa);
            } else {
                saDesc = sa.getDescription();
            }
            // string might have leading whitespace
            saDesc = saDesc.trim();
            if (!saDesc.isEmpty()) {
                // in case sa starts with CARDNAME, dont lowercase it
                if (!saDesc.startsWith(sa.getHostCard().getName())) {
                    saDesc = saDesc.substring(0, 1).toLowerCase() + saDesc.substring(1);
                }
                if (saDesc.contains("ORIGINALHOST") && sa.getOriginalHost() != null) {
                    saDesc = TextUtil.fastReplace(saDesc, "ORIGINALHOST", sa.getOriginalHost().getName());
                }
            } else {
                saDesc = "<take no action>"; // printed in case nothing is chosen for the ability (e.g. Charm with Up to X)
            }
            result = TextUtil.fastReplace(result, "ABILITY", saDesc);

            String currentName = sa.getHostCard().getName();
            result = CardTranslation.translateMultipleDescriptionText(result, currentName);
            result = TextUtil.fastReplace(result,"CARDNAME", CardTranslation.getTranslatedName(currentName));
            result = TextUtil.fastReplace(result,"NICKNAME", Lang.getInstance().getNickName(CardTranslation.getTranslatedName(currentName)));
        }

        return result;
    }

    /**
     * <p>
     * phasesCheck.
     * </p>
     *
     * @return a boolean.
     */
    public final boolean phasesCheck(final Game game) {
        PhaseHandler phaseHandler = game.getPhaseHandler();
        if (null != validPhases) {
            if (!validPhases.contains(phaseHandler.getPhase())) {
                return false;
            }
        }

        if (hasParam("PreCombatMain")) {
            if (!phaseHandler.isPreCombatMain()) {
                return false;
            }
        }

        if (hasParam("PlayerTurn")) {
            if (!phaseHandler.isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (hasParam("NotPlayerTurn")) {
            if (phaseHandler.isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (hasParam("OpponentTurn")) {
            if (!this.getHostCard().getController().isOpponentOf(phaseHandler.getPlayerTurn())) {
                return false;
            }
        }

        if (hasParam("FirstUpkeep")) {
            if (!phaseHandler.isFirstUpkeep()) {
                return false;
            }
        }

        if (hasParam("FirstUpkeepThisGame")) {
            if (!phaseHandler.isFirstUpkeepThisGame()) {
                return false;
            }
        }

        if (hasParam("FirstCombat")) {
            if (!phaseHandler.isFirstCombat()) {
                return false;
            }
        }

        if (hasParam("TurnCount")) {
            int turn = Integer.parseInt(getParam("TurnCount"));
            if (phaseHandler.getTurn() != turn) {
                return false;
            }
        }

        return true;
    }
    /**
     * <p>
     * requirementsCheck.
     * </p>
     * @param game
     *
     * @return a boolean.
     */
    public final boolean requirementsCheck(Game game) {
        if (hasParam("APlayerHasMoreLifeThanEachOther")) {
            int highestLife = Integer.MIN_VALUE; // Negative base just in case a few Lich's or Platinum Angels are running around
            final List<Player> healthiest = new ArrayList<>();
            for (final Player p : game.getPlayers()) {
                if (p.getLife() > highestLife) {
                    healthiest.clear();
                    highestLife = p.getLife();
                    healthiest.add(p);
                } else if (p.getLife() == highestLife) {
                    highestLife = p.getLife();
                    healthiest.add(p);
                }
            }

            if (healthiest.size() != 1) {
                // More than one player tied for most life
                return false;
            }
        }

        if (hasParam("APlayerHasMostCardsInHand")) {
            int largestHand = 0;
            final List<Player> withLargestHand = new ArrayList<>();
            for (final Player p : game.getPlayers()) {
                if (p.getCardsIn(ZoneType.Hand).size() > largestHand) {
                    withLargestHand.clear();
                    largestHand = p.getCardsIn(ZoneType.Hand).size();
                    withLargestHand.add(p);
                } else if (p.getCardsIn(ZoneType.Hand).size() == largestHand) {
                    largestHand = p.getCardsIn(ZoneType.Hand).size();
                    withLargestHand.add(p);
                }
            }

            if (withLargestHand.size() != 1) {
                // More than one player tied for most life
                return false;
            }
        }

        if (hasParam("ResolvedLimit")) {
            if (this.getOverridingAbility().getResolvedThisTurn() >= Integer.parseInt(getParam("ResolvedLimit"))) {
                return false;
            }
        }

        if (!meetsCommonRequirements(this.mapParams))
            return false;

        return true;
    }


    public boolean meetsRequirementsOnTriggeredObjects(Game game,  final Map<AbilityKey, Object> runParams) {
        if ("True".equals(getParam("EvolveCondition"))) {
            final Card moved = (Card) runParams.get(AbilityKey.Card);
            if (moved == null) {
                return false;
                // final StringBuilder sb = new StringBuilder();
                // sb.append("Trigger::requirementsCheck() - EvolveCondition condition being checked without a moved card. ");
                // sb.append(this.getHostCard().getName());
                // throw new RuntimeException(sb.toString());
            }
            if (moved.getNetPower() <= this.getHostCard().getNetPower()
                    && moved.getNetToughness() <= this.getHostCard().getNetToughness()) {
                return false;
            }
        }

        String condition = getParam("Condition");
        if ("AltCost".equals(condition)) {
            final Card moved = (Card) runParams.get(AbilityKey.Card);
            if( null != moved && !moved.isOptionalCostPaid(OptionalCost.AltCost))
                return false;
        } else if ("AttackedPlayerWithMostLife".equals(condition)) {
            GameEntity attacked = (GameEntity) runParams.get(AbilityKey.Attacked);
            if (attacked == null) {
                // Check "Defender" too because once triggering objects are set on TriggerAttacks, the value of Attacked
                // ends up being in Defender at that point.
                attacked = (GameEntity) runParams.get(AbilityKey.Defender);
            }
            if (attacked == null || !attacked.isValid("Player.withMostLife",
                    this.getHostCard().getController(), this.getHostCard(), null)) {
                return false;
            }
        } else if ("AttackedPlayerWhoAttackedYouLastTurn".equals(condition)) {
            GameEntity attacked = (GameEntity) runParams.get(AbilityKey.Attacked);
            if (attacked == null) {
                // Check "Defender" too because once triggering objects are set on TriggerAttacks, the value of Attacked
                // ends up being in Defender at that point.
                attacked = (GameEntity) runParams.get(AbilityKey.DefendingPlayer);
            }
            Player attacker = this.getHostCard().getController();

            boolean valid = false;
            if (game.getPlayersAttackedLastTurn().containsKey(attacked)) {
                if (game.getPlayersAttackedLastTurn().get(attacked).contains(attacker)) {
                    valid = true;
                }
            }

            if (attacked == null || !valid) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof Trigger)) {
            return false;
        }

        return this.getId() == ((Trigger) o).getId();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(Trigger.class, getId());
    }

    /**
     * <p>
     * performTest.
     * </p>
     *
     * @param runParams
     *            a {@link HashMap} object.
     * @return a boolean.
     */
    public abstract boolean performTest(Map<AbilityKey, Object> runParams);

    /**
     * <p>
     * setTriggeringObjects.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public abstract void setTriggeringObjects(SpellAbility sa, final Map<AbilityKey, Object> runParams);

    /**
     * Gets the id.
     *
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    /**
     * <p>
     * setID.
     * </p>
     *
     * @param id
     *            a int.
     */
    public final void setId(final int id) {
        this.id = id;
    }

    public void addRemembered(Object o) {
        this.triggerRemembered.add(o);
    }

    public List<Object> getTriggerRemembered() {
        return this.triggerRemembered;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return the mode
     */
    public TriggerType getMode() {
        return mode;
    }

    /**
     *
     * @param triggerType
     *            the triggerType to set
     * @param triggerType
     */
    void setMode(TriggerType triggerType) {
        mode = triggerType;
    }

    public final Trigger copy(Card newHost, boolean lki) {
        final Trigger copy = (Trigger) clone();

        copyHelper(copy, newHost);

        if (getOverridingAbility() != null) {
            copy.setOverridingAbility(getOverridingAbility().copy(newHost, lki));
        }

        if (!lki) {
            copy.setId(nextId());
        }

        if (validPhases != null) {
            copy.setTriggerPhases(Sets.newEnumSet(validPhases, PhaseType.class));
        }
        copy.setActiveZone(validHostZones);
        return copy;
    }

    public boolean isStatic() {
        return hasParam("Static"); // && params.get("Static").equals("True") [always true if present]
    }

    public void setTriggerPhases(Set<PhaseType> phases) {
        validPhases = phases;
    }

    //public String getImportantStackObjects(SpellAbility sa) { return ""; };
    abstract public String getImportantStackObjects(SpellAbility sa);

    public SpellAbility getSpawningAbility() {
        return spawningAbility;
    }

    public void setSpawningAbility(SpellAbility ability) {
        spawningAbility = ability;
    }

    public int getActivationsThisTurn() {
        return this.numberTurnActivations;
    }

    public void triggerRun()
    {
        this.numberTurnActivations++;
    }

    // Resets the state stored each turn for per-turn and per-instance restriction
    public void resetTurnState()
    {
        this.numberTurnActivations = 0;
    }

    /** {@inheritDoc} */
    @Override
    public final Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            throw new RuntimeException("Trigger : clone() error, " + ex);
        }
    }


    /* (non-Javadoc)
     * @see forge.game.CardTraitBase#changeText()
     */
    @Override
    public void changeText() {
        if (!isIntrinsic()) {
            return;
        }
        super.changeText();

        SpellAbility sa = ensureAbility();

        if (sa != null) {
            sa.changeText();
        }
    }

    /* (non-Javadoc)
     * @see forge.game.CardTraitBase#changeTextIntrinsic(java.util.Map, java.util.Map)
     */
    @Override
    public void changeTextIntrinsic(Map<String, String> colorMap, Map<String, String> typeMap) {
        if (!isIntrinsic()) {
            return;
        }
        super.changeTextIntrinsic(colorMap, typeMap);

        SpellAbility sa = ensureAbility();

        if (sa != null) {
            sa.changeTextIntrinsic(colorMap, typeMap);
        }
    }

    public SpellAbility ensureAbility(final IHasSVars sVarHolder) {
        SpellAbility sa = getOverridingAbility();
        if (sa == null && hasParam("Execute")) {
            sa = AbilityFactory.getAbility(getHostCard(), getParam("Execute"), sVarHolder);
            setOverridingAbility(sa);
        }
        return sa;
    }

    public SpellAbility ensureAbility() {
        return ensureAbility(this);
    }
}
