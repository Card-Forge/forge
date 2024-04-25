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

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

/**
 * <p>
 * Trigger_Attached class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerAttached.java 17802 2012-10-31 08:05:14Z Max mtg $
 */
public class TriggerAttached extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Attached.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerAttached(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidSource", runParams.get(AbilityKey.AttachSource))) {
            return false;
        }
        if (!matchesValidParam("ValidTarget", runParams.get(AbilityKey.AttachTarget))) {
            return false;
        }
        if (!matchesValidParam("TargetRelativeToSource", runParams.get(AbilityKey.AttachTarget),
                (Card) runParams.get(AbilityKey.AttachSource))) {
            return false;
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObject(AbilityKey.Source, runParams.get(AbilityKey.AttachSource));
        sa.setTriggeringObject(AbilityKey.Target, runParams.get(AbilityKey.AttachTarget));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblAttachee")).append(": ").append(sa.getTriggeringObject(AbilityKey.Target));
        return sb.toString();
    }
}
