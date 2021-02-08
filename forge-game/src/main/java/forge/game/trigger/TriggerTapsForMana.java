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

import forge.card.MagicColor;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

import java.util.Map;

import static forge.util.TextUtil.toManaString;

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
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerTapsForMana(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        
        //Check for tapping
        if (!hasParam("NoTapCheck")) {
            final SpellAbility manaAbility = (SpellAbility) runParams.get(AbilityKey.AbilityMana);
            if (manaAbility == null || !manaAbility.getRootAbility().getPayCosts().hasTapCost()) {
                return false;
            }
        }

        if (hasParam("ValidCard")) {
            final Card tapper = (Card) runParams.get(AbilityKey.Card);
            if (!tapper.isValid(getParam("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }

        if (hasParam("Player")) {
            final Player player = (Player) runParams.get(AbilityKey.Player);
            if (!player.isValid(getParam("Player").split(","), this.getHostCard().getController(), this.getHostCard(), null)) {
                return false;
            }
        }

        if (hasParam("Activator")) {
            final SpellAbility sa = (SpellAbility) runParams.get(AbilityKey.AbilityMana);
            if (sa == null) return false;
            final Player activator = sa.getActivatingPlayer();
            if (!activator.isValid(getParam("Activator").split(","), this.getHostCard().getController(), this.getHostCard(), null)) {
                return false;
            }
        }

        if (hasParam("Produced")) {
            Object prod = runParams.get(AbilityKey.Produced);
            if (prod == null || !(prod instanceof String)) {
                return false;
            }
            String produced = (String) prod;
            if ("ChosenColor".equals(getParam("Produced"))) {
                if (!this.getHostCard().hasChosenColor() || !produced.contains(MagicColor.toShortString(this.getHostCard().getChosenColor()))) {
                    return false;
                }
            } else if (!produced.contains(MagicColor.toShortString(this.getParam("Produced")))) {
                    return false;
            }
        }

        return true;
    }


    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Card, AbilityKey.Player, AbilityKey.Produced);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblTappedForMana")).append(": ").append(sa.getTriggeringObject(AbilityKey.Card));
        sb.append(Localizer.getInstance().getMessage("lblProduced")).append(": ").append(toManaString(sa.getTriggeringObject(AbilityKey.Produced).toString()));
        return sb.toString();
    }

}
