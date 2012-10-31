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
package forge.card.trigger;

import java.util.Map;

import forge.Card;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TriggerTransformed extends Trigger {

    /**
     * Instantiates a new trigger transformed.
     *
     * @param params the params
     * @param host the host
     * @param intrinsic the intrinsic
     */
    public TriggerTransformed(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#performTest(java.util.Map)
     */
    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Transformer"), this.getMapParams().get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#getCopy()
     */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new TriggerTransformed(this.getMapParams(), this.getHostCard(), this.isIntrinsic());

        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }

        copyFieldsTo(copy);
        return copy;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#setTriggeringObjects(forge.card.spellability.SpellAbility)
     */
    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Transformer", this.getRunParams().get("Transformer"));
    }

}
