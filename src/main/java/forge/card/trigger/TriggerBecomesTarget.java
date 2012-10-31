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
 * <p>
 * Trigger_BecomesTarget class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class TriggerBecomesTarget extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_BecomesTarget.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerBecomesTarget(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("SourceType")) {
            final SpellAbility sa = (SpellAbility) runParams2.get("SourceSA");
            if (this.getMapParams().get("SourceType").equalsIgnoreCase("spell")) {
                if (!sa.isSpell()) {
                    return false;
                }
            } else if (this.getMapParams().get("SourceType").equalsIgnoreCase("ability")) {
                if (!sa.isAbility()) {
                    return false;
                }
            }
        }
        if (this.getMapParams().containsKey("ValidSource")) {
            if (!matchesValid(((SpellAbility) runParams2.get("SourceSA")).getSourceCard(), this.getMapParams()
                    .get("ValidSource").split(","), this.getHostCard())) {
                return false;
            }
        }
        if (this.getMapParams().containsKey("ValidTarget")) {
            if (!matchesValid(runParams2.get("Target"), this.getMapParams().get("ValidTarget").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new TriggerBecomesTarget(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }

        copyFieldsTo(copy);
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("SourceSA", this.getRunParams().get("SourceSA"));
        sa.setTriggeringObject("Source", ((SpellAbility) this.getRunParams().get("SourceSA")).getSourceCard());
        sa.setTriggeringObject("Target", this.getRunParams().get("Target"));
    }
}
