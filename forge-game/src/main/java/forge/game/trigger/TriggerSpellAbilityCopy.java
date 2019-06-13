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
import forge.game.Game;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

/**
 * <p>
 * Trigger_SpellAbilityCopy class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerSpellAbilityCopy extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_SpellAbilityCopy.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerSpellAbilityCopy(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        final SpellAbility spellAbility = (SpellAbility) runParams2.get("CopySA");
        if (spellAbility == null) {
            System.out.println("TriggerSpellAbilityCopy performTest encountered spellAbility == null. runParams2 = " + runParams2);
            return false;
        }
        final Card cast = spellAbility.getHostCard();
        final Game game = cast.getGame();
        final SpellAbilityStackInstance si = game.getStack().getInstanceFromSpellAbility(spellAbility);

        if (this.getMode() == TriggerType.SpellCopy) {
            if (!spellAbility.isSpell()) {
                return false;
            }
        }

        if (hasParam("ValidCard")) {
            if (!matchesValid(cast, getParam("ValidCard").split(","), getHostCard())) {
                return false;
            }
        }
        if (hasParam("ValidSA")) {
            if (!matchesValid(spellAbility, getParam("ValidSA").split(","), getHostCard())) {
                return false;
            }
        }
        if (hasParam("ValidActivatingPlayer")) {
            if (si == null || !matchesValid(si.getSpellAbility(true).getActivatingPlayer(), getParam("ValidActivatingPlayer")
                    .split(","), getHostCard())) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        final SpellAbility copySA = (SpellAbility) getRunParams().get("CopySA");
        final SpellAbilityStackInstance si = sa.getHostCard().getGame().getStack().getInstanceFromSpellAbility(copySA);
        sa.setTriggeringObject("Card", copySA.getHostCard());
        sa.setTriggeringObject("SpellAbility", copySA);
        sa.setTriggeringObject("StackInstance", si);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Card: ").append(sa.getTriggeringObject("Card")).append(", ");
        sb.append("Activator: ").append(sa.getTriggeringObject("Activator")).append(", ");
        sb.append("SpellAbility: ").append(sa.getTriggeringObject("SpellAbility"));
        return sb.toString();
    }
}
