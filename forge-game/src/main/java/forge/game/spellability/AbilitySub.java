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

import java.util.Map;

import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;

/**
 * <p>
 * Abstract Ability_Sub class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilitySub extends SpellAbility implements java.io.Serializable, Cloneable {
    /** Constant <code>serialVersionUID=4650634415821733134L</code>. */
    private static final long serialVersionUID = 4650634415821733134L;

    private SpellAbility parent = null;

    /**
     * <p>
     * Setter for the field <code>parent</code>.
     * </p>
     * 
     * @param parent
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public final void setParent(final SpellAbility parent) {
        this.parent = parent;
    }

    /**
     * <p>
     * Getter for the field <code>parent</code>.
     * </p>
     * 
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    @Override
    public final SpellAbility getParent() {
        return this.parent;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        // this should never be on the Stack by itself
        return false;
    }

    private final SpellAbilityEffect effect;

    public AbilitySub(ApiType api0, final Card ca, final TargetRestrictions tgt, Map<String, String> params0) {
        super(ca, Cost.Zero);
        this.setTargetRestrictions(tgt);

        api = api0;
        if (params0 != null) {
            originalMapParams.putAll(params0);
            mapParams.putAll(params0);
        }

        effect = api.getSpellEffect();

        if (api.equals(ApiType.Mana) || api.equals(ApiType.ManaReflected)) {
            this.setManaPart(new AbilityManaPart(ca, mapParams));
        }

        if (api.equals(ApiType.ChangeZone) || api.equals(ApiType.ChangeZoneAll)) {
            AbilityFactory.adjustChangeZoneTarget(mapParams, this);
        }
    }

    @Override
    public String getStackDescription() {
        return effect.getStackDescriptionWithSubs(mapParams, this);
    }

    @Override
    public void resolve() {
        effect.resolve(this);
    }
    
    /** {@inheritDoc} */
    @Override
    public final Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            throw new RuntimeException("AbilitySub : clone() error, " + ex);
        }
    }
}
