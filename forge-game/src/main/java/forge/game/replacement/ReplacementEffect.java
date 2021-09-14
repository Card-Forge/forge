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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.*;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.TriggerReplacementBase;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.TextUtil;

/**
 * TODO: Write javadoc for this type.
 *
 */
public abstract class ReplacementEffect extends TriggerReplacementBase {
    private static int maxId = 0;
    private static int nextId() { return ++maxId; }

    /** The ID. */
    private int id;

    private ReplacementType mode;

    private ReplacementLayer layer = ReplacementLayer.Other;

    /** The has run. */
    private boolean hasRun = false;

    private List<ReplacementEffect> otherChoices = null;

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
        this.id = nextId();
        this.intrinsic = intrinsic;
        originalMapParams.putAll(map);
        mapParams.putAll(map);
        this.setHostCard(host);
        if (map.containsKey("Layer")) {
            this.setLayer(ReplacementLayer.smartValueOf(map.get("Layer")));
        }
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

    public List<ReplacementEffect> getOtherChoices() {
        return otherChoices;
    }
    public void setOtherChoices(List<ReplacementEffect> choices) {
        this.otherChoices = choices;
    }

    /**
     * Can replace.
     *
     * @param runParams
     *            the run params
     * @return true, if successful
     */
    public abstract boolean canReplace (final Map<AbilityKey, Object> runParams);

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
            if (!PhaseType.parseRange(params.get("ActivePhases")).contains(game.getPhaseHandler().getPhase())) {
                return false;
            }
        }

        return meetsCommonRequirements(params);
    }

    /**
     * Gets the copy.
     *
     * @return the copy
     */
    public final ReplacementEffect copy(final Card host, final boolean lki) {
        final ReplacementEffect res = (ReplacementEffect) clone();

        copyHelper(res, host);

        final SpellAbility sa = this.getOverridingAbility();
        if (sa != null) {
            final SpellAbility overridingAbilityCopy = sa.copy(host, lki);
            if (overridingAbilityCopy != null) {
                res.setOverridingAbility(overridingAbilityCopy);
            }
        }

        if (!lki) {
            res.setId(nextId());
            res.setHasRun(false);
            res.setOtherChoices(null);
        }

        res.setActiveZone(validHostZones);
        res.setLayer(getLayer());
        return res;
    }

    /**
     * Sets the replacing objects.
     *  @param runParams
     *            the run params
     * @param spellAbility
     */
    public void setReplacingObjects(final Map<AbilityKey, Object> runParams, final SpellAbility spellAbility) {
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

    public String getDescription() {
        if (hasParam("Description") && !this.isSuppressed()) {
            String desc = AbilityUtils.applyDescriptionTextChangeEffects(getParam("Description"), this);
            String currentName;
            if (this.isIntrinsic() && cardState != null && cardState.getCard() == getHostCard()) {
                currentName = cardState.getName();
            }
            else {
                currentName = getHostCard().getName();
            }
            desc = CardTranslation.translateSingleDescriptionText(desc, currentName);
            desc = TextUtil.fastReplace(desc, "CARDNAME", CardTranslation.getTranslatedName(currentName));
            desc = TextUtil.fastReplace(desc, "NICKNAME", Lang.getInstance().getNickName(CardTranslation.getTranslatedName(currentName)));
            if (desc.contains("EFFECTSOURCE")) {
                desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", getHostCard().getEffectSource().toString());
            }
            // Add remaining shield amount
            if (mode == ReplacementType.DamageDone) {
                SpellAbility repSA = getOverridingAbility();
                if (repSA != null && repSA.getApi() == ApiType.ReplaceDamage && repSA.hasParam("Amount")) {
                    String varValue = repSA.getParam("Amount");
                    if (!StringUtils.isNumeric(varValue)) {
                        varValue = repSA.getSVar(varValue);
                        if (varValue.startsWith("Number$")) {
                            desc += " \nShields remain: " + varValue.substring(7);
                        }
                    }
                }
                if (repSA != null && repSA.getApi() == ApiType.ReplaceSplitDamage) {
                    String varValue = repSA.getParamOrDefault("VarName", "1");
                    if (varValue.equals("1")) {
                        desc += " \nShields remain: 1";
                    } else if (!StringUtils.isNumeric(varValue)) {
                        varValue = repSA.getSVar(varValue);
                        if (varValue.startsWith("Number$")) {
                            desc += " \nShields remain: " + varValue.substring(7);
                        }
                    }
                }
            }
            return desc;
        } else {
            return "";
        }
    }

    /**
     * To string.
     *
     * @return a String
     */
    @Override
    public String toString() {
        return getHostCard().toString() + " - " + getDescription();
    }

    /** {@inheritDoc} */
    @Override
    public final Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            throw new RuntimeException("ReplacementEffect : clone() error, " + ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof ReplacementEffect)) {
            return false;
        }

        return this.getId() == ((ReplacementEffect) o).getId();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(ReplacementEffect.class, getId());
    }

    public ReplacementType getMode() {
        return mode;
    }

    void setMode(ReplacementType mode) {
        this.mode = mode;
    }

    public SpellAbility ensureAbility() {
        SpellAbility sa = getOverridingAbility();
        if (sa == null && hasParam("ReplaceWith")) {
            sa = AbilityFactory.getAbility(getHostCard(), getParam("ReplaceWith"));
            setOverridingAbility(sa);
        }
        return sa;
    }

    @Override
    public List<Object> getTriggerRemembered() {
        return ImmutableList.of();
    }
}
