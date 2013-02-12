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

import forge.Card;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceDiscard extends ReplacementEffect {

    /**
     * Instantiates a new replace discard.
     *
     * @param params the params
     * @param host the host
     */
    public ReplaceDiscard(final HashMap<String, String> params, final Card host) {
        super(params, host);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(HashMap<String, Object> runParams) {
        if (!runParams.get("Event").equals("Discard")) {
            return false;
        }
        if (this.getMapParams().containsKey("ValidPlayer")) {
            if (!matchesValid(runParams.get("Affected"), this.getMapParams().get("ValidPlayer").split(","), this.getHostCard())) {
                return false;
            }
        }
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Card"), this.getMapParams().get("ValidCard").split(","), this.getHostCard())) {
                return false;
            }
        }
        if (this.getMapParams().containsKey("ValidSource")) {
            if (!matchesValid(runParams.get("Source"), this.getMapParams().get("ValidSource").split(","), this.getHostCard())) {
                return false;
            }
        }
        if (this.getMapParams().containsKey("DiscardFromEffect")) {
            if (null == runParams.get("Source")) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#getCopy()
     */
    @Override
    public ReplacementEffect getCopy() {
        ReplacementEffect res = new ReplaceDiscard(this.getMapParams(), this.getHostCard());
        res.setOverridingAbility(this.getOverridingAbility());
        res.setActiveZone(validHostZones);
        res.setLayer(getLayer());
        return res;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(HashMap<String, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject("Card", runParams.get("Card"));
        sa.setReplacingObject("Player", runParams.get("Affected"));
    }

}
