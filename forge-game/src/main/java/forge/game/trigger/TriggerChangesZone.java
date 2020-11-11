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

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

import java.util.EnumSet;
import java.util.Map;

import forge.util.Localizer;
import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.Sets;

/**
 * <p>
 * Trigger_ChangesZone class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class TriggerChangesZone extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_ChangesZone.
     * </p>
     *
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerChangesZone(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
        correctZones();
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (hasParam("Origin")) {
            if (!getParam("Origin").equals("Any")) {
                if (getParam("Origin") == null) {
                    return false;
                }
                if (!ArrayUtils.contains(
                    getParam("Origin").split(","), runParams.get(AbilityKey.Origin)
                )) {
                    return false;
                }
            }
        }

        if (hasParam("Destination")) {
            if (!getParam("Destination").equals("Any")) {
                if (!ArrayUtils.contains(
                    getParam("Destination").split(","), runParams.get(AbilityKey.Destination)
                )) {
                    return false;
                }
            }
        }

        if (hasParam("ExcludedDestinations")) {
            if (!ArrayUtils.contains(
                getParam("ExcludedDestinations").split(","), runParams.get(AbilityKey.Destination)
            )) {
                return false;
            }
        }

        if (hasParam("ValidCard")) {
            Card moved = (Card) runParams.get(AbilityKey.Card);
            boolean leavesBattlefield = "Battlefield".equals(getParam("Origin"));

            if (leavesBattlefield) {
                moved = (Card) runParams.get(AbilityKey.CardLKI);
            }

            if (!matchesValid(moved, getParam("ValidCard").split(","), getHostCard())) {
                return false;
            }
        }

        if (hasParam("ValidCause")) {
            if (!runParams.containsKey(AbilityKey.Cause)) {
                return false;
            }
            SpellAbility cause = (SpellAbility) runParams.get(AbilityKey.Cause);
            if (cause == null) {
                return false;
            }
            if (!matchesValid(cause, getParam("ValidCause").split(","), getHostCard())) {
                if (!matchesValid(cause.getHostCard(), getParam("ValidCause").split(","), getHostCard())) {
                    return false;
                }
            }
        }

        // Check number of lands ETB this turn on triggered card's controller
        if (hasParam("CheckOnTriggeredCard")) {
            final String[] condition = getParam("CheckOnTriggeredCard").split(" ", 2);

            final Card host = hostCard.getGame().getCardState(hostCard);
            final String comparator = condition.length < 2 ? "GE1" : condition[1];
            final int referenceValue = AbilityUtils.calculateAmount(host, comparator.substring(2), this);
            final Card triggered = (Card) runParams.get(AbilityKey.Card);
            final int actualValue = CardFactoryUtil.xCount(triggered, host.getSVar(condition[0]));
            if (!Expressions.compare(actualValue, comparator.substring(0, 2), referenceValue)) {
                return false;
            }
        }

        // Check amount of damage dealt to the triggered card
        if (hasParam("DamageReceivedCondition")) {
            final String cond = getParam("DamageReceivedCondition");
            if (cond.length() < 3) {
                return false;
            }

            final Card card = (Card) runParams.get(AbilityKey.Card);
            if (card == null) {
                return false;
            }
            final int rightSide = AbilityUtils.calculateAmount(getHostCard(), cond.substring(2), this);

            // need to check the ChangeZone LKI copy for damage, otherwise it'll return 0 for a new object in the new zone
            Card lkiCard = card.getGame().getChangeZoneLKIInfo(card);

            final boolean expr = Expressions.compare(lkiCard.getTotalDamageRecievedThisTurn(), cond, rightSide);
            if (!expr) {
                return false;
            }
        }

        if (hasParam("NotThisAbility")) {
            if (runParams.containsKey(AbilityKey.Cause)) {
                SpellAbility cause = (SpellAbility) runParams.get(AbilityKey.Cause);
                if (cause != null && this.equals(cause.getRootAbility().getTrigger())) {
                    return false;
                }
            }
        }

        /* this trigger can only be activated once per turn, verify it hasn't already run */
        if (hasParam("ActivationLimit")) {
            return this.getActivationsThisTurn() < Integer.parseInt(getParam("ActivationLimit"));
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        // TODO use better way to always copy both Card and CardLKI
        if ("Battlefield".equals(getParam("Origin"))) {
            sa.setTriggeringObject(AbilityKey.Card, runParams.get(AbilityKey.CardLKI));
            sa.setTriggeringObject(AbilityKey.NewCard, runParams.get(AbilityKey.Card));
        } else {
            sa.setTriggeringObjectsFrom(runParams, AbilityKey.Card);
        }
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblZoneChanger")).append(": ").append(sa.getTriggeringObject(AbilityKey.Card));
        return sb.toString();
    }

    protected void correctZones() {
        // only if host zones isn't set
        if (validHostZones != null) {
            return;
        }

        // in case the game is null (for GUI) the later check does fail
        if (getHostCard().getGame() == null) {
            return;
        }

        if (!hasParam("ValidCard") || !getHostCard().isValid(getParam("ValidCard").split(","),
                getHostCard().getController(), getHostCard(), null)) {
            return;
        }

        if (hasParam("Origin")) {
            // leave battlefield
            boolean leavesBattlefield = ArrayUtils.contains(
                getParam("Origin").split(","), "Battlefield"
            );
            if (leavesBattlefield) {
                setActiveZone(EnumSet.of(ZoneType.Battlefield));
            }
        }

        // enter Zone Effect
        if (!hasParam("Origin") || "Any".equals(getParam("Origin"))) {
            setActiveZone(Sets.newEnumSet(ZoneType.listValueOf(getParam("Destination")), ZoneType.class));
        }
    }

}
