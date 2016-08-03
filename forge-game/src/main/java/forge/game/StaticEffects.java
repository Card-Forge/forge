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
package forge.game;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import forge.game.card.Card;
import forge.game.staticability.StaticAbility;

/**
 * <p>
 * StaticEffects class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class StaticEffects {

    // **************** StaticAbility system **************************
    private final Map<StaticAbility, StaticEffect> staticEffects = Maps.newHashMap();
    //Global rule changes
    private final Set<GlobalRuleChange> ruleChanges = EnumSet.noneOf(GlobalRuleChange.class);

    public final void clearStaticEffects(final Set<Card> affectedCards) {
        ruleChanges.clear();

        // remove all static effects
        for (final StaticEffect se : staticEffects.values()) {
            Iterables.addAll(affectedCards, se.remove());
        }
        this.staticEffects.clear();
    }

    public void setGlobalRuleChange(final GlobalRuleChange change) {
        this.ruleChanges.add(change);
    }

    public boolean getGlobalRuleChange(final GlobalRuleChange change) {
        return this.ruleChanges.contains(change);
    }

    /**
     * Add a static effect to the list of static effects.
     * 
     * @param staticEffect
     *            a {@link StaticEffect}.
     */
    public final StaticEffect getStaticEffect(final StaticAbility staticAbility) {
        final StaticEffect currentEffect = staticEffects.get(staticAbility);
        if (currentEffect != null) {
            return currentEffect;
        }

        final StaticEffect newEffect = new StaticEffect(staticAbility);
        this.staticEffects.put(staticAbility, newEffect);
        return newEffect;
    }

    public Iterable<StaticEffect> getEffects() {
        return staticEffects.values();
    }
}
