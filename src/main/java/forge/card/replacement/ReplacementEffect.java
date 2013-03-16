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
package forge.card.replacement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.TriggerReplacementBase;
import forge.card.ability.AbilityUtils;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.util.Expressions;

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
     * Checks if is secondary.
     *
     * @return true, if is secondary
     */
    public final boolean isSecondary() {
        return this.mapParams.containsKey("Secondary");
    }

    /**
     * Ai should run.
     *
     * @param sa the sa
     * @param ai 
     * @return true, if successful
     */
    public final boolean aiShouldRun(final SpellAbility sa, AIPlayer ai) {
        if (this.mapParams.containsKey("AICheckSVar")) {
            System.out.println("aiShouldRun?" + sa);
            final String svarToCheck = this.mapParams.get("AICheckSVar");
            String comparator = "GE";
            int compareTo = 1;

            if (this.mapParams.containsKey("AISVarCompare")) {
                final String fullCmp = this.mapParams.get("AISVarCompare");
                comparator = fullCmp.substring(0, 2);
                final String strCmpTo = fullCmp.substring(2);
                try {
                    compareTo = Integer.parseInt(strCmpTo);
                } catch (final Exception ignored) {
                    if (sa == null) {
                        compareTo = CardFactoryUtil.xCount(this.hostCard, this.hostCard.getSVar(strCmpTo));
                    } else {
                        compareTo = AbilityUtils.calculateAmount(this.hostCard, this.hostCard.getSVar(strCmpTo), sa);
                    }
                }
            }

            int left = 0;

            if (sa == null) {
                left = CardFactoryUtil.xCount(this.hostCard, this.hostCard.getSVar(svarToCheck));
            } else {
                left = AbilityUtils.calculateAmount(this.hostCard, svarToCheck, sa);
            }
            System.out.println("aiShouldRun?" + left + comparator + compareTo);
            if (Expressions.compare(left, comparator, compareTo)) {
                return true;
            }
        } else if (sa != null && sa.doTrigger(false, ai)) {
            return true;
        }

        return false;
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

    /** The map params, denoting what to replace. */
    private Map<String, String> mapParams = new HashMap<String, String>();

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final Map<String, String> getMapParams() {
        return this.mapParams;
    }

    /**
     * Sets the map params.
     * 
     * @param mapParams
     *            the mapParams to set
     */
    public final void setMapParams(final Map<String, String> mapParams) {
        this.mapParams = mapParams;
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
     * To string.
     *
     * @return a String
     */
    @Override
    public String toString() {
        if (this.getMapParams().containsKey("Description") && !this.isSuppressed()) {
            return this.getMapParams().get("Description");
        } else {
            return "";
        }
    }

    /**
     * <p>
     * requirementsCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean requirementsCheck() {
        return this.requirementsCheck(this.getMapParams());
    }
    
    public boolean requirementsCheck(Map<String,String> params) {

        if (this.isSuppressed()) {
            return false; // Effect removed by effect
        }

        if (params.containsKey("PlayerTurn")) {
            if (params.get("PlayerTurn").equals("True") && !Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (params.containsKey("ActivePhases")) {
            boolean isPhase = false;
            List<PhaseType> aPhases = PhaseType.parseRange(params.get("ActivePhases"));
            final PhaseType currPhase = Singletons.getModel().getGame().getPhaseHandler().getPhase();
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
    public abstract ReplacementEffect getCopy();

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
     * Instantiates a new replacement effect.
     * 
     * @param map
     *            the map
     * @param host
     *            the host
     */
    public ReplacementEffect(final Map<String, String> map, final Card host) {
        this.setMapParams(map);
        this.setHostCard(host);
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
}
