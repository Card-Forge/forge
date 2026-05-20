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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbilityCantTarget.
 */
public class StaticAbilityCantTarget {

    static String MODE = "CantTarget";

    public static StaticAbility cantTarget(final GameEntity entity, final SpellAbility spellAbility)  {
        final Game game = entity.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantTarget)) {
                    continue;
                }

                if (applyCantTargetAbility(stAb, entity, spellAbility)) {
                    return stAb;
                }
            }
        }
        return null;
    }

    /**
     * Apply can't target ability.
     *
     * @param stAb
     *            the static ability
     * @param card
     *            the card
     * @param spellAbility
     *            the spell/ability
     * @return true, if successful
     */
    public static boolean applyCantTargetAbility(final StaticAbility stAb, final GameEntity entity, final SpellAbility spellAbility) {
        if (entity instanceof Card card) {
            if (stAb.hasParam("AffectedZone")) {
                if (ZoneType.listValueOf(stAb.getParam("AffectedZone")).stream().noneMatch(zt -> card.isInZone(zt))) {
                    return false;
                }
            } else if (!card.isInPlay()) { // default zone is battlefield
                return false;
            }
            Set<ZoneType> zones = stAb.getActiveZone();

            if (zones != null && zones.contains(ZoneType.Stack)) {
                // Enthralling Hold: only works if it wasn't already cast
                if (card.getGame().getStack().getSpellMatchingHost(spellAbility.getHostCard()) != null) {
                    return false;
                }
            }
        } else if (stAb.hasParam("AffectedZone")) {
            return false;
        }

        final Card source = spellAbility.getHostCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if ((stAb.isKeyword(Keyword.HEXPROOF) || stAb.isKeyword(Keyword.SHROUD)) && StaticAbilityIgnoreHexproofShroud.ignore(entity, spellAbility, stAb)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidTarget", entity)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidSA", spellAbility)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidSource", source)) {
            return false;
        }

        if (!stAb.matchesValidParam("Activator", activator)) {
            return false;
        }

        if (stAb.hasParam("SourceCanOnlyTarget")) {
            SpellAbility root = spellAbility.getRootAbility();
            List<SpellAbility> choices = null;
            if (root.getApi() == ApiType.Charm) {
                choices = Lists.newArrayList(root.getAdditionalAbilityList("Choices"));
            } else {
                choices = Lists.newArrayList(root);
            }
            Iterator<SpellAbility> it = choices.iterator();
            SpellAbility next = it.next();
            while (next != null) {
                if (next.usesTargeting() && (!next.getParam("ValidTgts").contains(stAb.getParam("SourceCanOnlyTarget"))
                        || next.getParam("ValidTgts").contains(",")
                        || next.getParam("ValidTgts").contains("non" + stAb.getParam("SourceCanOnlyTarget")))) {
                    return false;
                }
                next = next.getSubAbility();
                if (next == null && it.hasNext()) {
                    next = it.next();
                }
            }
        }

        return true;
    }
}
