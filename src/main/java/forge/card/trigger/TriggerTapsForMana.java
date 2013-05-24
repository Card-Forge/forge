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

import java.util.List;

import forge.Card;
import forge.card.MagicColor;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_TapsForMana class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerTapsForMana extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_TapsForMana.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerTapsForMana(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final Card tapper = (Card) runParams2.get("Card");
        final SpellAbility manaAbility = (SpellAbility) runParams2.get("AbilityMana");
        
        //Check for tapping
        if (manaAbility == null || manaAbility.getPayCosts() == null || !manaAbility.getPayCosts().hasTapCost()) {
            return false;
        }

        if (this.mapParams.containsKey("ValidCard")) {
            if (!tapper.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("Produced")) {
            Object prod = runParams2.get("Produced");
            if (prod == null || !(prod instanceof String)) {
                return false;
            }
            String produced = (String) prod;
            if ("ChosenColor".equals(mapParams.get("Produced"))) {
                List<String> colors = this.getHostCard().getChosenColor();
                if (colors.isEmpty() || !produced.contains(MagicColor.toShortString(colors.get(0)))) {
                    return false;
                }
            }
        }

        return true;
    }


    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
        sa.setTriggeringObject("Player", this.getRunParams().get("Player"));
        sa.setTriggeringObject("Produced", this.getRunParams().get("Produced"));
    }
}
