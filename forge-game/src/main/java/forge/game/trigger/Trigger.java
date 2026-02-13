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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import forge.game.*;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.cost.IndividualCostPaymentInstance;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.CostPaymentStack;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.ITranslatable;
import forge.util.Lang;
import forge.util.TextUtil;

import java.util.*;

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

    private Set<PhaseType> validPhases;

    private SpellAbility spawningAbility;

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
            ITranslatable nameSource = getHostName(this);
            String desc = getParam("TriggerDescription");
            if (!desc.contains("ABILITY")) {
                desc = CardTranslation.translateSingleDescriptionText(getParam("TriggerDescription"), nameSource);
                String translatedName = nameSource.getTranslatedName();
                desc = TextUtil.fastReplace(desc,"CARDNAME", translatedName);
                desc = TextUtil.fastReplace(desc,"NICKNAME", Lang.getInstance().getNickName(translatedName));
                if (desc.contains("ORIGINALHOST") && this.getOriginalHost() != null) {
                    desc = TextUtil.fastReplace(desc, "ORIGINALHOST", this.getOriginalHost().getDisplayName());
                }
            }
            if (getHostCard().getEffectSource() != null) {
                if (active)
                    desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", getHostCard().getEffectSource().toString());
                else
                    desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", getHostCard().getEffectSource().getDisplayName());
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
        return replaceAbilityText(desc, sa, false);
    }
    public final String replaceAbilityText(final String desc, SpellAbility sa, boolean forStack) {
        String result = desc;

        // this function is for ABILITY
        if (!result.contains("ABILITY")) {
            return result;
        }
        if (sa == null) {
            sa = getOverridingAbility();
        }
        if (sa != null) {
            String saDesc = "";
            boolean digMore = true;
            // if sa is a wrapper, get the Wrapped Ability
            if (sa.isWrapper()) {
                final WrappedAbility wa = (WrappedAbility) sa;
                sa = wa.getWrappedAbility();

                // wrapped Charm spells are special, only get the selected abilities (if there are any yet)
                if (ApiType.Charm.equals(sa.getApi())) {
                    saDesc = sa.getStackDescription();
                    digMore = false;
                }
            }
            if (digMore) { // if ABILITY is used, there is probably Charm somewhere
                SpellAbility trigSA = sa;
                while (trigSA != null) {
                    ApiType api = trigSA.getApi();
                    if (ApiType.Charm.equals(api)) {
                        saDesc = CharmEffect.makeFormatedDescription(trigSA, !forStack);
                        break;
                    }
                    if (ApiType.ImmediateTrigger.equals(api) || ApiType.DelayedTrigger.equals(api)) {
                        trigSA = trigSA.getAdditionalAbility("Execute");
                    } else {
                        trigSA = trigSA.getSubAbility();
                    }
                }
            }
            if (saDesc.isEmpty()) { // in case we haven't found anything better
                saDesc = sa.toString();
            }
            // string might have leading whitespace
            saDesc = saDesc.trim();
            if (!saDesc.isEmpty()) {
                // in case sa starts with CARDNAME, dont lowercase it
                if (!saDesc.startsWith(sa.getHostCard().getName())) {
                    saDesc = saDesc.substring(0, 1).toLowerCase() + saDesc.substring(1);
                }
                if (saDesc.contains("ORIGINALHOST") && sa.getOriginalHost() != null) {
                    saDesc = TextUtil.fastReplace(saDesc, "ORIGINALHOST", sa.getOriginalHost().getDisplayName());
                }
            } else {
                saDesc = "<take no action>"; // printed in case nothing is chosen for the ability (e.g. Charm with Up to X)
            }
            result = TextUtil.fastReplace(result, "ABILITY", saDesc);

            result = CardTranslation.translateMultipleDescriptionText(result, sa.getHostCard());
            String translatedName = sa.getHostCard().getTranslatedName();
            result = TextUtil.fastReplace(result,"CARDNAME", translatedName);
            result = TextUtil.fastReplace(result,"NICKNAME", Lang.getInstance().getNickName(translatedName));
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
            // add support for calculation if needed
            if (hasParam("PhaseCount") && phaseHandler.getNumMain() + 1 != 2) {
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

        // host controller will be null when adding card in a simulation game
        if (this.getHostCard().getController() == null || (game.getAge() != GameStage.Play && game.getAge() != GameStage.RestartedByKarn) || !meetsCommonRequirements(this.mapParams)) {
            return false;
        }

        if (!checkResolvedLimit(getHostCard().getController())) {
            return false;
        }

        return true;
    }

    public boolean checkResolvedLimit(Player activator) {
        // CR 603.2i
        if (hasParam("ResolvedLimit")) {
            if (Collections.frequency(getHostCard().getAbilityResolvedThisTurnActivators(getOverridingAbility()), activator)
                    >= Integer.parseInt(getParam("ResolvedLimit"))) {
                return false;
            }
        }
        return true;
    }

    public boolean checkActivationLimit() {
        if (hasParam("ActivationLimit") &&
                getActivationsThisTurn() >= Integer.parseInt(getParam("ActivationLimit"))) {
            return false;
        }
        if (hasParam("GameActivationLimit") && 
            getActivationsThisGame() >= Integer.parseInt(getParam("GameActivationLimit"))) {
                return false;
        }
        return true;
    }

    public boolean meetsRequirementsOnTriggeredObjects(Game game, final Map<AbilityKey, Object> runParams) {
        String condition = getParam("Condition");

        if (isKeyword(Keyword.EVOLVE) || "Evolve".equals(condition)) {
            final Card moved = (Card) runParams.get(AbilityKey.Card);
            if (moved == null) {
                return false;
            }
            // CR 702.100c
            if (!moved.isCreature() || !this.getHostCard().isCreature()) {
                return false;
            }
            if (moved.getNetPower() <= this.getHostCard().getNetPower()
                    && moved.getNetToughness() <= this.getHostCard().getNetToughness()) {
                return false;
            }
        }

        if (condition == null) {
            return true;
        }

        if ("LifePaid".equals(condition)) {
            final SpellAbility trigSA = (SpellAbility) runParams.get(AbilityKey.SpellAbility);
            if (trigSA != null && trigSA.getAmountLifePaid() <= 0) {
                return false;
            }
        } else if ("NoOpponentHasMoreLifeThanAttacked".equals(condition)) {
            GameEntity attacked = (GameEntity) runParams.get(AbilityKey.Attacked);
            if (attacked == null) {
                attacked = (GameEntity) runParams.get(AbilityKey.Defender);
            }
            // we should not have gotten this far if planeswalker was attacked, but just to be safe
            if (!(attacked instanceof Player)) {
                return false;
            }
            final Player attackedP = (Player) attacked;
            int life = attackedP.getLife();
            boolean found = false;
            for (Player opp : this.getHostCard().getController().getOpponents()) {
                if (opp.equals(attackedP)) {
                    continue;
                }
                if (opp.getLife() > life) {
                    found = true;
                    break;
                }
            }
            if (found) {
                return false;
            }
        } else if ("Sacrificed".equals(condition)) {
            final SpellAbility trigSA = (SpellAbility) runParams.get(AbilityKey.SpellAbility);
            if (trigSA != null && Iterables.isEmpty(trigSA.getPaidList("Sacrificed"))) {
                return false;
            }
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
        } else if ("AttackerHasUnattackedOpp".equals(condition)) {
            Player attacker = (Player) runParams.get(AbilityKey.AttackingPlayer);
            if (game.getCombat().getAttackersAndDefenders().values().containsAll(attacker.getOpponents())) {
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

    public <T> void addRemembered(T o) {
        this.triggerRemembered.add(o);
    }
    public <T> void addRemembered(Collection<T> o) {
        this.triggerRemembered.addAll(o);
    }

    @Override
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
        return copy(newHost, lki, false, null);
    }
    public final Trigger copy(Card newHost, boolean lki, boolean keepTextChanges) {
        return copy(newHost, lki, keepTextChanges, null);
    }
    public final Trigger copy(Card newHost, boolean lki, boolean keepTextChanges, SpellAbility spellAbility) {
        final Trigger copy = (Trigger) clone();

        copyHelper(copy, newHost, lki || keepTextChanges);

        if (spellAbility != null) {
            copy.setOverridingAbility(spellAbility);
        } else if (getOverridingAbility() != null) {
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
        return hostCard.getAbilityActivatedThisTurn(this.getOverridingAbility());
    }

    public int getActivationsThisGame() {
        return hostCard.getAbilityActivatedThisGame(this.getOverridingAbility());
    }

    public void triggerRun() {
        if (this.getOverridingAbility() != null) {
            hostCard.addAbilityActivated(this.getOverridingAbility());
        }
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

    public SpellAbility ensureAbility(final IHasSVars sVarHolder) {
        SpellAbility sa = getOverridingAbility();
        if (sa == null && hasParam("Execute")) {
            if (this.isIntrinsic() && sVarHolder instanceof CardState state) {
                sa = state.getAbilityForTrigger(getParam("Execute"));
            } else {
                sa = AbilityFactory.getAbility(getHostCard(), getParam("Execute"), sVarHolder);
            }
            setOverridingAbility(sa);
        }
        return sa;
    }

    public SpellAbility ensureAbility() {
        return ensureAbility(this);
    }

    @Override
    public void setOverridingAbility(SpellAbility overridingAbility0) {
        super.setOverridingAbility(overridingAbility0);
        overridingAbility0.setTrigger(this);
    }

    boolean whileKeywordCheck(final String param, final Map<AbilityKey, Object> runParams) {
        IndividualCostPaymentInstance currentPayment = (IndividualCostPaymentInstance) runParams.get(AbilityKey.IndividualCostPaymentInstance);
        if (currentPayment != null) {
            if (matchesValidParam(param, currentPayment.getPayment().getAbility())) return true;
        }

        CostPaymentStack stack = (CostPaymentStack) runParams.get(AbilityKey.CostStack);
        for (IndividualCostPaymentInstance individual : stack) {
            if (matchesValidParam(param, individual.getPayment().getAbility())) return true;
        }

        return false;
    }

    public boolean isChapter() {
        return hasParam("Chapter");
    }
    public Integer getChapter() {
        if (!isChapter())
            return null;
        return Integer.valueOf(getParam("Chapter"));
    }
    public boolean isLastChapter() {
        return isChapter() && getChapter() == getCardState().getFinalChapterNr();
    }

    @Override
    public boolean isManaAbility() {
        if (!TriggerType.TapsForMana.equals(getMode()) && !TriggerType.ManaAdded.equals(getMode())) {
            return false;
        }
        return ensureAbility().isManaAbility();
    }
}
