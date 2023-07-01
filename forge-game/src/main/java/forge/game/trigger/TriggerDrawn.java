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
import forge.game.GameStage;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

/**
 * <p>
 * Trigger_Drawn class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerDrawn extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Drawn.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerDrawn(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final Game game = getHostCard().getGame();
        final int number = ((Integer) runParams.get(AbilityKey.Number));

        if (!matchesValidParam("ValidCard", runParams.get(AbilityKey.Card))) {
            return false;
        }
        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player))) {
            return false;
        }

        if (hasParam("Number")) {
            if (number != Integer.parseInt(getParam("Number"))) {
                return false;
            }
        }

        if (hasParam("FirstCardInDrawStep")) {
            final Player p = ((Player)runParams.get(AbilityKey.Player));
            if (getParam("FirstCardInDrawStep").equals("True")) {
                if (!game.getPhaseHandler().is(PhaseType.DRAW, p) || p.numDrawnThisDrawStep() > 1) {
                    return false;
                }
            } else {
                if (p.numDrawnThisDrawStep() == 1 && game.getPhaseHandler().is(PhaseType.DRAW, p)) {
                    return false;
                }
            }
        }

        // trigger should not happen while Mulligan
        if (game.getAge() == GameStage.Mulligan) {
            return false;
        }

        if (runParams.containsKey(AbilityKey.CanReveal)) {
            // while drawing this is only set if false
            boolean canReveal = (boolean) runParams.get(AbilityKey.CanReveal);
            if (hasParam("ForReveal")) {
                if (!canReveal) {
                    return false;
                }
            } else if (canReveal) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Card, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblPlayer")).append(": ").append(sa.getTriggeringObject(AbilityKey.Player));
        return sb.toString();
    }
}
