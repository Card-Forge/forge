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
package forge.game.staticability;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbilityCantTarget.
 */
public class StaticAbilityCantTarget {

    /**
     * Apply can't target ability.
     *
     * @param st
     *            the static ability
     * @param card
     *            the card
     * @param spellAbility
     *            the spell/ability
     * @return true, if successful
     */
    public static boolean applyCantTargetAbility(final StaticAbility st, final Card card,
            final SpellAbility spellAbility) {
        final Card hostCard = st.getHostCard();
        final Card source = spellAbility.getHostCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (st.hasParam("ValidPlayer")) {
            return false;
        }

        if (st.hasParam("AffectedZone")) {
            boolean inZone = false;
            for (final ZoneType zt : ZoneType.listValueOf(st.getParam("AffectedZone"))) {
                if (card.isInZone(zt)) {
                    inZone = true;
                    break;
                }
            }

            if (!inZone) {
                return false;
            }
        } else { // default zone is battlefield
            if (!card.isInZone(ZoneType.Battlefield)) {
                return false;
            }
        }


        if (st.hasParam("ValidCard")
                && !card.isValid(st.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }


        if (st.hasParam("Hexproof") && (activator != null)) {
            for (String k : activator.getKeywords()) {
                if (k.startsWith("IgnoreHexproof")) {
                    String[] m = k.split(":");
                    if (card.isValid(m[1].split(","), activator, source, spellAbility)) {
                        return false;
                    }
                }
            }
        }
        if (st.hasParam("Shroud") && (activator != null)) {
            for (String k : activator.getKeywords()) {
                if (k.startsWith("IgnoreShroud")) {
                    String[] m = k.split(":");
                    if (card.isValid(m[1].split(","), activator, source, spellAbility)) {
                        return false;
                    }
                }
            }
        }

        return common(st, spellAbility);
    }

    public static boolean applyCantTargetAbility(final StaticAbility st, final Player player,
            final SpellAbility spellAbility) {
        final Card hostCard = st.getHostCard();
        final Card source = spellAbility.getHostCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (st.hasParam("ValidCard") || st.hasParam("AffectedZone")) {
            return false;
        }

        if (st.hasParam("ValidPlayer")
                && !player.isValid(st.getParam("ValidPlayer").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }


        if (st.hasParam("Hexproof") && (activator != null)) {
            for (String k : activator.getKeywords()) {
                if (k.startsWith("IgnoreHexproof")) {
                    String[] m = k.split(":");
                    if (player.isValid(m[1].split(","), activator, source, spellAbility)) {
                        return false;
                    }
                }
            }
        }

        return common(st, spellAbility);
    }

    protected static boolean common(final StaticAbility st, final SpellAbility spellAbility) {
        final Card hostCard = st.getHostCard();
        final Card source = spellAbility.getHostCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (st.hasParam("ValidSA")
                && !spellAbility.isValid(st.getParam("ValidSA").split(","), hostCard.getController(), hostCard, spellAbility)) {
            return false;
        }

        if (st.hasParam("ValidSource")
                && !source.isValid(st.getParam("ValidSource").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (st.hasParam("Activator") && (activator != null)
                && !activator.isValid(st.getParam("Activator"), hostCard.getController(), hostCard, spellAbility)) {
            return false;
        }

        if (spellAbility.hasParam("ValidTgts") &&
                (st.hasParam("SourceCanOnlyTarget")
                && (!spellAbility.getParam("ValidTgts").contains(st.getParam("SourceCanOnlyTarget"))
                    || spellAbility.getParam("ValidTgts").contains(","))
                    || spellAbility.getParam("ValidTgts").contains("non" + st.getParam("SourceCanOnlyTarget")
                    )
                )
           ){
            return false;
        }

        return true;
    }
}
