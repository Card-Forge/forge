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
package forge.game.replacement;

import forge.game.Game;
import forge.game.TriggerReplacementBase;
import forge.game.ability.AbilityApiBased;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

import java.util.List;
import java.util.Map;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public abstract class ReplacementEffect extends TriggerReplacementBase {

    private ReplacementLayer layer = ReplacementLayer.None;

    /** The has run. */
    private boolean hasRun = false;

    /**
     * Checks for run.
     * 
     * @return the hasRun
     */
    public final boolean hasRun() {
        return this.hasRun;
    }

    /**
     * Instantiates a new replacement effect.
     * 
     * @param map
     *            the map
     * @param host
     *            the host
     */
    public ReplacementEffect(final Map<String, String> map, final Card host, final boolean intrinsic) {
        this.intrinsic = intrinsic;
        originalMapParams.putAll(map);
        mapParams.putAll(map);
        this.setHostCard(host);
    }

    /**
     * Checks if is secondary.
     *
     * @return true, if is secondary
     */
    public final boolean isSecondary() {
        return this.getMapParams().containsKey("Secondary");
    }

    /**
     * Sets the checks for run.
     * 
     * @param hasRun
     *            the hasRun to set
     */
    public final void setHasRun(final boolean hasRun) {
        this.hasRun = hasRun;
    }

    /**
     * Can replace.
     * 
     * @param runParams
     *            the run params
     * @return true, if successful
     */
    public abstract boolean canReplace(final Map<String, Object> runParams);

    /**
     * <p>
     * requirementsCheck.
     * </p>
     * @param game 
     * 
     * @return a boolean.
     */
    public boolean requirementsCheck(Game game) {
        return this.requirementsCheck(game, this.getMapParams());
    }
    
    public boolean requirementsCheck(Game game, Map<String,String> params) {

        if (this.isSuppressed()) {
            return false; // Effect removed by effect
        }

        if (params.containsKey("PlayerTurn")) {
            if (params.get("PlayerTurn").equals("True") && !game.getPhaseHandler().isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (params.containsKey("ActivePhases")) {
            boolean isPhase = false;
            List<PhaseType> aPhases = PhaseType.parseRange(params.get("ActivePhases"));
            final PhaseType currPhase = game.getPhaseHandler().getPhase();
            for (final PhaseType s : aPhases) {
                if (s == currPhase) {
                    isPhase = true;
                    break;
                }
            }

            return isPhase;
        }

        return meetsCommonRequirements(params);
    }

    /**
     * Gets the copy.
     * 
     * @return the copy
     */
    public final ReplacementEffect getCopy() {
        final ReplacementType rt = ReplacementType.getTypeFor(this);
        final ReplacementEffect res = rt.createReplacement(mapParams, hostCard, intrinsic); 
        final SpellAbility overridingAbility = this.getOverridingAbility();
        if (overridingAbility != null) {
        	final SpellAbility overridingAbilityCopy;
        	if (overridingAbility instanceof AbilityApiBased) {
        		overridingAbilityCopy = ((AbilityApiBased) overridingAbility).getCopy();
        	} else if (overridingAbility instanceof AbilitySub) {
        		overridingAbilityCopy = ((AbilitySub) overridingAbility).getCopy();
        	} else {
        		System.err.println("Overriding ability of " + hostCard + " of unexpected type " + overridingAbility.getClass());
        		overridingAbilityCopy = null;
        	}

        	if (overridingAbilityCopy != null) {
        		overridingAbilityCopy.setHostCard(hostCard);
        		res.setOverridingAbility(overridingAbilityCopy);
        	}
        }
        res.setActiveZone(validHostZones);
        res.setLayer(getLayer());
        res.setTemporary(isTemporary());
        return res;
    }

    /**
     * Sets the replacing objects.
     * 
     * @param runParams
     *            the run params
     * @param spellAbility
     *            the SpellAbility
     */
    public void setReplacingObjects(final Map<String, Object> runParams, final SpellAbility spellAbility) {
        // Should be overridden by replacers that need it.
    }

    /**
     * @return the layer
     */
    public ReplacementLayer getLayer() {
        return layer;
    }

    /**
     * @param layer0 the layer to set
     */
    public void setLayer(ReplacementLayer layer0) {
        this.layer = layer0;
    }

    /**
     * To string.
     *
     * @return a String
     */
    @Override
    public String toString() {
        if (this.getMapParams().containsKey("Description") && !this.isSuppressed()) {
            return AbilityUtils.applyDescriptionTextChangeEffects(this.getMapParams().get("Description"), this);
        } else {
            return "";
        }
    }



}
