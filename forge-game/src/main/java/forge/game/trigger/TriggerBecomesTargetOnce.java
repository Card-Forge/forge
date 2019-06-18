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

import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Trigger_BecomesTargetOnce class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class TriggerBecomesTargetOnce extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_BecomesTargetOnce.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerBecomesTargetOnce(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidSource")) {
            if (!matchesValid(((SpellAbility) runParams2.get("SourceSA")).getHostCard(), this.mapParams
                    .get("ValidSource").split(","), this.getHostCard())) {
                return false;
            }
        }
        if (this.mapParams.containsKey("ValidTarget")) {
            List<GameObject> targets = (List<GameObject>) runParams2.get("Targets");
            boolean valid = false;
            for (GameObject b : targets) {
                if (matchesValid(b, this.mapParams.get("ValidTarget").split(","), this.getHostCard())) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("SourceSA", this.getRunParams().get("SourceSA"));
        sa.setTriggeringObject("Source", ((SpellAbility) this.getRunParams().get("SourceSA")).getHostCard());
        sa.setTriggeringObject("Targets", this.getRunParams().get("Targets"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Source: ").append(((SpellAbility) sa.getTriggeringObject("SourceSA")).getHostCard()).append(", ");
        sb.append("Targets: ").append(sa.getTriggeringObject("Targets"));
        return sb.toString();
    }
}
