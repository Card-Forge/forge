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
package forge.game.spellability;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

/**
 * <p>
 * Target class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TargetRestrictions {
    // Target has two things happening:
    // Targeting restrictions (Creature, Min/Maxm etc) which are true for this
    // What this Object is restricted to targeting
    private String[] originalValidTgts,
        validTgts;
    private String uiPrompt = "";
    private List<ZoneType> tgtZone = Arrays.asList(ZoneType.Battlefield);

    // The target SA of this SA must be targeting a Valid X
    private String saValidTargeting = null;

    // Additional restrictions that may not fit into Valid
    private boolean uniqueTargets = false;
    private boolean singleZone = false;
    private boolean forEachPlayer = false;
    private boolean differentControllers = false;
    private boolean differentCMC = false;
    private boolean equalToughness = false;
    private boolean sameController = false;
    private boolean withoutSameCreatureType = false;
    private boolean withSameCreatureType = false;
    private boolean withSameCardType = false;
    private boolean randomTarget = false;
    private boolean randomNumTargets = false;

    // How many can be targeted?
    private String minTargets;
    private String maxTargets;

    // What's the max total CMC of targets?
    private String maxTotalCMC;

    // What's the max total power of targets?
    private String maxTotalPower;

    // Not sure what's up with Mandatory? Why wouldn't targeting be mandatory?
    private boolean bMandatory = false;

    /**
     * <p>
     * Copy Constructor for Target.
     * </p>
     * 
     * @param target
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     */
    public TargetRestrictions(final TargetRestrictions target) {
        this.uiPrompt = target.getVTSelection();
        this.originalValidTgts = target.getValidTgts();
        this.validTgts = this.originalValidTgts.clone();
        this.minTargets = target.getMinTargets();
        this.maxTargets = target.getMaxTargets();
        this.maxTotalCMC = target.getMaxTotalCMC();
        this.maxTotalPower = target.getMaxTotalPower();
        this.tgtZone = target.getZone();
        this.saValidTargeting = target.getSAValidTargeting();
        this.uniqueTargets = target.isUniqueTargets();
        this.singleZone = target.isSingleZone();
        this.forEachPlayer = target.isForEachPlayer();
        this.differentControllers = target.isDifferentControllers();
        this.differentCMC = target.isDifferentCMC();
        this.equalToughness = target.isEqualToughness();
        this.sameController = target.isSameController();
        this.withoutSameCreatureType = target.isWithoutSameCreatureType();
        this.withSameCreatureType = target.isWithSameCreatureType();
        this.withSameCardType = target.isWithSameCardType();
        this.randomTarget = target.isRandomTarget();
        this.randomNumTargets = target.isRandomNumTargets();
    }

    /**
     * <p>
     * Constructor for Target.
     * </p>
     *
     * @param prompt
     *            a {@link java.lang.String} object.
     * @param valid
     *            an array of {@link java.lang.String} objects.
     * @param min
     *            a {@link java.lang.String} object.
     * @param max
     *            a {@link java.lang.String} object.
     */
    public TargetRestrictions(final String prompt, final String[] valid, final String min, final String max) {
        this.uiPrompt = prompt;
        this.originalValidTgts = valid;
        this.validTgts = this.originalValidTgts.clone();
        this.minTargets = min;
        this.maxTargets = max;
    }

    public final boolean getMandatory() {
        return this.bMandatory;
    }
    public final void setMandatory(final boolean m) {
        this.bMandatory = m;
    }

    /**
     * <p>
     * setMaxTotalCMC.
     * </p>
     * 
     * @param cmc
     *            a String.
     */
    public final void setMaxTotalCMC(final String cmc) {
        this.maxTotalCMC = cmc;
    }

    /**
     * <p>
     * setMaxTotalPower.
     * </p>
     *
     * @param power
     *              a String.
     */
    public final void setMaxTotalPower(final String power) {
        this.maxTotalPower = power;
    }

    /**
     * <p>
     * getValidTgts.
     * </p>
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public final String[] getValidTgts() {
        return this.validTgts;
    }

    /**
     * <p>
     * getVTSelection.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getVTSelection() {
        return this.uiPrompt;
    }

    /**
     * Gets the min targets.
     *
     * @return the min targets
     */
    public final String getMinTargets() {
        return this.minTargets;
    }

    /**
     * Gets the max targets.
     *
     * @return the max targets
     */
    public final String getMaxTargets() {
        return this.maxTargets;
    }

    /**
     * Gets the max targets.
     *
     * @return the max targets
     */
    private final String getMaxTotalCMC() {
        return this.maxTotalCMC;
    }

    public final int getMaxTotalCMC(final Card c, final SpellAbility sa) {
        return AbilityUtils.calculateAmount(c, this.maxTotalCMC, sa);
    }

    /**
     * Gets the max targets.
     *
     * @return the max targets
     */
    private final String getMaxTotalPower() {
        return this.maxTotalPower;
    }

    public final int getMaxTotalPower(final Card c, final SpellAbility sa) {
        return AbilityUtils.calculateAmount(c, this.maxTotalPower, sa);
    }

    /**
     * <p>
     * Getter for the field <code>minTargets</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a int.
     */
    public final int getMinTargets(final Card c, final SpellAbility sa) {
        return AbilityUtils.calculateAmount(c, this.minTargets, sa);
    }

    /**
     * <p>
     * Getter for the field <code>maxTargets</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a int.
     */
    public final int getMaxTargets(final Card c, final SpellAbility sa) {
        return AbilityUtils.calculateAmount(c, this.maxTargets, sa);
    }

    /**
     * <p>
     * isMaxTargetsChosen.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean isMaxTargetsChosen(final Card c, final SpellAbility sa) {
        return this.getMaxTargets(c, sa) == sa.getTargets().size();
    }

    /**
     * <p>
     * isMinTargetsChosen.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean isMinTargetsChosen(final Card c, final SpellAbility sa) {
        int min = getMinTargets(c, sa);
        if (min == 0 || (sa.isDividedAsYouChoose() && ObjectUtils.defaultIfNull(sa.getDividedValue(), 0) == 0)) {
            return true;
        }
        return min <= sa.getTargets().size();
    }

    /**
     * <p>
     * setZone.
     * </p>
     * 
     * @param tZone
     *            a {@link java.lang.String} object.
     */
    public final void setZone(final ZoneType tZone) {
        this.tgtZone = Arrays.asList(tZone);
    }

    /**
     * Sets the zone.
     * 
     * @param tZone
     *            the new zone
     */
    public final void setZone(final List<ZoneType> tZone) {
        this.tgtZone = tZone;
    }

    /**
     * <p>
     * getZone.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final List<ZoneType> getZone() {
        return this.tgtZone;
    }

    /**
     * <p>
     * setSAValidTargeting.
     * </p>
     * 
     * @param saValidTgting
     *            a {@link java.lang.String} object.
     */
    public final void setSAValidTargeting(final String saValidTgting) {
        this.saValidTargeting = saValidTgting;
    }

    /**
     * <p>
     * getSAValidTargeting.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getSAValidTargeting() {
        return this.saValidTargeting;
    }

    /**
     * <p>
     * canOnlyTgtOpponent.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canOnlyTgtOpponent() {
        boolean player = false;
        boolean opponent = false;
        for (final String s : this.validTgts) {
            if (s.startsWith("Opponent")) {
                opponent = true;
            } else if (s.startsWith("Player")) {
                player = true;
            }
        }
        return opponent && !player;
    }

    public final boolean canTgtPlayer() {
        for (final String s : this.validTgts) {
            if (s.startsWith("Player") || s.startsWith("Opponent") || s.startsWith("Any")) {
                return true;
            }
        }
        return false;
    }

    public final boolean canTgtPermanent() {
        for (final String s : this.validTgts) {
            if (s.contains("Permanent")) {
                return true;
            }
        }
        return false;
    }

    public final boolean canTgtCreature() {
        for (final String s : this.validTgts) {
            // TODO check IsCommander when in that variant
            if ((s.contains("Creature") || s.startsWith("Permanent") || s.startsWith("Any"))
                    && !s.contains("nonCreature")) {
                return true;
            }
            String[] tgtParams = TextUtil.split(s, '.');
            for (String param : tgtParams) {
                if (CardType.isACreatureType(param)) {
                    return true;
                }
            }
        }
        return false;
    }

    public final boolean canTgtPlaneswalker() {
        for (final String s : this.validTgts) {
            if (s.startsWith("Planeswalker") || s.startsWith("Any")) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * canTgtCreatureAndPlayer.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canTgtCreatureAndPlayer() {
        return this.canTgtPlayer() && this.canTgtCreature();
    }

    /**
     * <p>
     * hasCandidates.
     * </p>
     * 
     * @param sa
     *            the sa
     * @return a boolean.
     */
    public final boolean hasCandidates(final SpellAbility sa) {
        final Card srcCard = sa.getHostCard(); // should there be OrginalHost at any moment?
        final Game game = srcCard.getGame();

        this.applyTargetTextChanges(sa);

        for (Player player : game.getPlayers()) {
            if (!player.isValid(this.validTgts, sa.getActivatingPlayer(), srcCard, sa)) {
                continue;
            }
            if (!sa.canTarget(player)) {
                continue;
            }
            if (sa.getTargets().contains(player)) {
                continue;
            }
            return true;
        }

        if (this.tgtZone.contains(ZoneType.Stack)) {
            // Stack Zone targets are considered later
            return true;
        }
        for (final Card c : game.getCardsIn(this.tgtZone)) {
            if (!c.isValid(this.validTgts, sa.getActivatingPlayer(), srcCard, sa)) {
                continue;
            }
            if (!sa.canTarget(c)) {
                continue;
            }
            if (sa.getTargets().contains(c)) {
                continue;
            }
            return true;
        }

        return false;
    }

    /**
     * <p>
     * getNumCandidates.
     * </p>
     * 
     * @param sa
     *            the sa
     * @param isTargeted
     *            Check Valid Candidates and Targeting
     * @return a int.
     */
    public final int getNumCandidates(final SpellAbility sa, final boolean isTargeted) {
        int num = 0;
        if (this.tgtZone.contains(ZoneType.Stack)) {
            for (final SpellAbilityStackInstance si : sa.getHostCard().getGame().getStack()) {
                SpellAbility abilityOnStack = si.getSpellAbility();
                if (sa.canTargetSpellAbility(abilityOnStack)) {
                    num++;
                }
            }
        }
        // TODO this may count some SA twice
        return num + getAllCandidates(sa, isTargeted).size();
    }

    public final List<GameEntity> getAllCandidates(final SpellAbility sa, final boolean isTargeted) {
        return getAllCandidates(sa, isTargeted, false);
    }

    public final List<GameEntity> getAllCandidates(final SpellAbility sa, final boolean isTargeted, final boolean onlyNonCard) {
        final Game game = sa.getActivatingPlayer().getGame();
        final List<GameEntity> candidates = Lists.newArrayList();
        for (Player player : game.getPlayers()) {
            if (sa.canTarget(player)) {
                candidates.add(player);
            }
        }

        this.applyTargetTextChanges(sa);

        if (onlyNonCard) {
            return candidates;
        }

        final Card srcCard = sa.getHostCard(); // should there be OrginalHost at any moment?

        for (final Card c : game.getCardsIn(this.tgtZone)) {
            if (c.isValid(this.validTgts, sa.getActivatingPlayer(), srcCard, sa)
                    && (!isTargeted || sa.canTarget(c))
                    && !sa.getTargets().contains(c)) {
                candidates.add(c);
            }
        }

        return candidates;
    }

    public final boolean isUniqueTargets() {
        return this.uniqueTargets;
    }
    public final void setUniqueTargets(final boolean unique) {
        this.uniqueTargets = unique;
    }
    public final boolean isSingleZone() {
        return this.singleZone;
    }
    public final void setSingleZone(final boolean single) {
        this.singleZone = single;
    }
    public boolean isWithoutSameCreatureType() {
        return withoutSameCreatureType;
    }
    public void setWithoutSameCreatureType(boolean b) {
        this.withoutSameCreatureType = b;
    }
    public boolean isWithSameCreatureType() {
        return withSameCreatureType;
    }
    public void setWithSameCreatureType(boolean b) {
        this.withSameCreatureType = b;
    }
    public boolean isWithSameCardType() {
        return withSameCardType;
    }
    public void setWithSameCardType(boolean b) {
        this.withSameCardType = b;
    }

    /**
     * <p>
     * copy.
     * </p>
     * 
     * @return a {@link forge.game.spellability.TargetRestrictions} object.
     */
    public TargetRestrictions copy() {
        TargetRestrictions clone = null;
        try {
            clone = (TargetRestrictions) this.clone();
        } catch (final CloneNotSupportedException e) {
            System.err.println(e);
        }
        return clone;
    }

    /**
     * @return the randomTarget
     */
    public boolean isRandomTarget() {
        return randomTarget;
    }

    /**
     * @param random the randomTarget to set
     */
    public void setRandomTarget(boolean random) {
        this.randomTarget = random;
    }

    /**
     * @return the randomNumTargets
     */
    public boolean isRandomNumTargets() {
        return randomNumTargets;
    }

    /**
     * @param randomNumTgts the randomNumTarget to set
     */
    public void setRandomNumTargets(boolean randomNumTgts) {
        this.randomNumTargets = randomNumTgts;
    }

    /**
     * @return the differentCMC
     */
    public boolean isDifferentCMC() {
        return differentCMC;
    }

    /**
     * @param different the differentCMC to set
     */
    public void setDifferentCMC(boolean different) {
        this.differentCMC = different;
    }

    /**
     * @return the equalToughness
     */
    public boolean isEqualToughness() {
        return equalToughness;
    }

    /**
     * @param b the equalToughness to set
     */
    public void setEqualToughness(boolean b) {
        this.equalToughness = b;
    }

    /**
     * @return the differentControllers
     */
    public boolean isDifferentControllers() {
        return differentControllers;
    }

    /**
     * @param different the differentControllers to set
     */
    public void setDifferentControllers(boolean different) {
        this.differentControllers = different;
    }

    public boolean isForEachPlayer() {
        return forEachPlayer;
    }
    public void setForEachPlayer(boolean each) {
        this.forEachPlayer = each;
    }

    /**
     * Checks if is same controller.
     * 
     * @return true, if it targets same controller
     */
    public final boolean isSameController() {
        return this.sameController;
    }

    /**
     * Sets the same controller.
     * 
     * @param same
     *            the new unique targets
     */
    public final void setSameController(final boolean same) {
        this.sameController = same;
    }

    public final void applyTargetTextChanges(final SpellAbility sa) {
        for (int i = 0; i < validTgts.length; i++) {
            validTgts[i] = AbilityUtils.applyAbilityTextChangeEffects(originalValidTgts[i], sa);
        }
    }
}
