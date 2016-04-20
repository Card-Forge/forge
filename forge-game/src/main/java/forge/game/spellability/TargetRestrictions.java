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
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

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

    // Target Choices (which is specific for the StackInstance)

    // What this Object is restricted to targeting
    private boolean tgtValid = false;
    private String[] originalValidTgts,
        validTgts;
    private String uiPrompt = "";
    private List<ZoneType> tgtZone = Arrays.asList(ZoneType.Battlefield);

    // The target SA of this SA must be targeting a Valid X
    private String saValidTargeting = null;
    
    // Additional restrictions that may not fit into Valid
    private boolean uniqueTargets = false;
    private boolean singleZone = false;
    private boolean differentControllers = false;
    private boolean sameController = false;
    private boolean withoutSameCreatureType = false;
    private boolean singleTarget = false;
    private boolean randomTarget = false;

    // How many can be targeted?
    private String minTargets;
    private String maxTargets;

    // What's the max total CMC of targets?
    private String maxTotalCMC;

    // For "Divided" cards. Is this better in TargetChoices?
    private boolean dividedAsYouChoose = false;
    private HashMap<Object, Integer> dividedMap = new HashMap<Object, Integer>();
    private int stillToDivide = 0;
    
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
        this.tgtValid = true;
        this.uiPrompt = target.getVTSelection();
        this.originalValidTgts = target.getValidTgts();
        this.validTgts = this.originalValidTgts.clone();
        this.minTargets = target.getMinTargets();
        this.maxTargets = target.getMaxTargets();
        this.maxTotalCMC = target.getMaxTotalCMC();
        this.tgtZone = target.getZone();
        this.saValidTargeting = target.getSAValidTargeting();
        this.dividedAsYouChoose = target.isDividedAsYouChoose();
        this.uniqueTargets = target.isUniqueTargets();
        this.singleZone = target.isSingleZone();
        this.differentControllers = target.isDifferentControllers();
        this.sameController = target.isSameController();
        this.withoutSameCreatureType = target.isWithoutSameCreatureType();
        this.singleTarget = target.isSingleTarget();
        this.randomTarget = target.isRandomTarget();
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
        this.tgtValid = true;
        this.uiPrompt = prompt;
        this.originalValidTgts = valid;
        this.validTgts = this.originalValidTgts.clone();
        this.minTargets = min;
        this.maxTargets = max;
    }

    /**
     * <p>
     * getMandatory.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getMandatory() {
        return this.bMandatory;
    }

    /**
     * <p>
     * setMandatory.
     * </p>
     * 
     * @param m
     *            a boolean.
     */
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
     * doesTarget.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean doesTarget() {
        return this.tgtValid;
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
    private final String getMinTargets() {
        return this.minTargets;
    }

    /**
     * Gets the max targets.
     *
     * @return the max targets
     */
    private final String getMaxTargets() {
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
        TargetChoices choice = sa.getTargets();
        return this.getMaxTargets(c, sa) == choice.getNumTargeted();
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
        if (this.getMinTargets(c, sa) == 0) {
            return true;
        }
        TargetChoices choice = sa.getTargets();
        return this.getMinTargets(c, sa) <= choice.getNumTargeted();
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

    /**
     * <p>
     * canTgtPlayer.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canTgtPlayer() {
        for (final String s : this.validTgts) {
            if (s.startsWith("Player") || s.startsWith("Opponent")) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * canTgtCreature.
     * </p>
     * 
     * @return a boolean.
     */

    public final boolean canTgtPermanent() {
        for (final String s : this.validTgts) {
            if (s.contains("Permanent")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Can tgt creature.
     * 
     * @return true, if successful
     */
    public final boolean canTgtCreature() {
        for (final String s : this.validTgts) {
            if ((s.contains("Creature") || CardType.isACreatureType(s) || s.startsWith("Permanent"))
                    && !s.contains("nonCreature")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Can tgt planeswalker.
     * 
     * @return true, if successful
     */
    public final boolean canTgtPlaneswalker() {
        for (final String s : this.validTgts) {
            if (s.startsWith("Planeswalker")) {
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
     * @param isTargeted
     *            Check Valid Candidates and Targeting
     * @return a boolean.
     */
    public final boolean hasCandidates(final SpellAbility sa, final boolean isTargeted) {
        final Game game = sa.getHostCard().getGame();
        for (Player player : game.getPlayers()) {
            if (sa.canTarget(player)) {
                return true;
            }
        }

        this.applyTargetTextChanges(sa);

        final Card srcCard = sa.getHostCard(); // should there be OrginalHost at any moment?
        if (this.tgtZone.contains(ZoneType.Stack)) {
            // Stack Zone targets are considered later
            return true;
        } else {
            for (final Card c : game.getCardsIn(this.tgtZone)) {
                if (!c.isValid(this.validTgts, srcCard.getController(), srcCard, sa)) {
                    continue;
                }
                if (isTargeted && !sa.canTarget(c)) {
                    continue;
                }
                if (sa.getTargets().isTargeting(c)) {
                    continue;
                }
                return true;
            }
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
        return getAllCandidates(sa, isTargeted).size();
    }

    /**
     * <p>
     * getAllCandidates.
     * </p>
     * 
     * @param sa
     *            the sa
     * @param isTargeted
     *            Check Valid Candidates and Targeting
     * @return a List<Object>.
     */
    public final List<GameEntity> getAllCandidates(final SpellAbility sa, final boolean isTargeted) {
        return getAllCandidates(sa, isTargeted, false);
    }

    /**
     * <p>
     * getAllCandidates.
     * </p>
     * 
     * @param sa
     *            the sa
     * @param isTargeted
     *            Check Valid Candidates and Targeting
     * @param onlyNonCard
     *            Only return non-card (e.g. player) Candidates
     * @return a List<Object>.
     */
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
        if (this.tgtZone.contains(ZoneType.Stack)) {
            for (final Card c : game.getStackZone().getCards()) {
                if (c.isValid(this.validTgts, srcCard.getController(), srcCard, sa)
                        && (!isTargeted || sa.canTarget(c)) 
                        && !sa.getTargets().isTargeting(c)) {
                    candidates.add(c);
                }
            }
        } else {
            for (final Card c : game.getCardsIn(this.tgtZone)) {
                if (c.isValid(this.validTgts, srcCard.getController(), srcCard, sa)
                        && (!isTargeted || sa.canTarget(c)) 
                        && !sa.getTargets().isTargeting(c)) {
                    candidates.add(c);
                }
            }
        }

        return candidates;
    }

    /**
     * Checks if is unique targets.
     * 
     * @return true, if is unique targets
     */
    public final boolean isUniqueTargets() {
        return this.uniqueTargets;
    }

    /**
     * Sets the unique targets.
     * 
     * @param unique
     *            the new unique targets
     */
    public final void setUniqueTargets(final boolean unique) {
        this.uniqueTargets = unique;
    }

    /**
     * Checks if targets must be from a single zone.
     * 
     * @return true, if singleZone
     */
    public final boolean isSingleZone() {
        return this.singleZone;
    }

    /**
     * Sets if targets must be from a single zone.
     * 
     * @param single
     *            the new singleZone
     */
    public final void setSingleZone(final boolean single) {
        this.singleZone = single;
    }

    /**
     * @return the withoutSameCreatureType
     */
    public boolean isWithoutSameCreatureType() {
        return withoutSameCreatureType;
    }

    /**
     * @param b the withoutSameCreatureType to set
     */
    public void setWithoutSameCreatureType(boolean b) {
        this.withoutSameCreatureType = b;
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

    /**
     * @return the singleTarget
     */
    public boolean isSingleTarget() {
        return singleTarget;
    }

    /**
     * @param singleTarget the singleTarget to set
     */
    public void setSingleTarget(boolean singleTarget) {
        this.singleTarget = singleTarget;
    }

    /**
     * @return a boolean dividedAsYouChoose
     */
    public boolean isDividedAsYouChoose() {
        return this.dividedAsYouChoose;
    }

    /**
     * @param divided the boolean to set
     */
    public void setDividedAsYouChoose(boolean divided) {
        this.dividedAsYouChoose = divided;
    }

    /**
     * Get the amount remaining to distribute.
     * @return int stillToDivide
     */
    public int getStillToDivide() {
        return this.stillToDivide;
    }

    /**
     * @param remaining set the amount still to be divided
     */
    public void setStillToDivide(final int remaining) {
        this.stillToDivide = remaining;
    }

    public void calculateStillToDivide(String toDistribute, Card source, SpellAbility sa) {
        // Recalculate this value just in case it's variable
        if (!this.dividedAsYouChoose) {
            return;
        }

        if (StringUtils.isNumeric(toDistribute)) {
            this.setStillToDivide(Integer.parseInt(toDistribute));
        } else if ( source == null ) { 
            return; // such calls come from AbilityFactory.readTarget - at this moment we don't yet know X or any other variables
        } else if (source.getSVar(toDistribute).equals("xPaid")) {
            this.setStillToDivide(source.getXManaCostPaid());
        } else {
            this.setStillToDivide(AbilityUtils.calculateAmount(source, toDistribute, sa));
        }
    }

    /**
     * Store divided amount relative to a specific card/player.
     * @param tgt the targeted object
     * @param portionAllocated the divided portion allocated
     */
    public final void addDividedAllocation(final Object tgt, final Integer portionAllocated) {
        if (this.dividedMap.containsKey(tgt)) {
            this.dividedMap.remove(tgt);
        }
        this.dividedMap.put(tgt, portionAllocated);
    }

    /**
     * Get the divided amount relative to a specific card/player.
     * @param tgt the targeted object
     * @return an int.
     */
    public int getDividedValue(Object tgt) {
        return this.dividedMap.get(tgt);
    }

    public HashMap<Object, Integer> getDividedMap() {
        return this.dividedMap;
    }

    public final void applyTargetTextChanges(final SpellAbility sa) {
        for (int i = 0; i < validTgts.length; i++) {
            validTgts[i] = AbilityUtils.applyAbilityTextChangeEffects(originalValidTgts[i], sa);
        }
    }

}
