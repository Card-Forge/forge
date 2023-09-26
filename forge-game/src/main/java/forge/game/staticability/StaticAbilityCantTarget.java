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

    public static boolean cantTarget(final Card card, final SpellAbility spellAbility)  {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }

                if (applyCantTargetAbility(stAb, card, spellAbility)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean cantTarget(final Player player, final SpellAbility spellAbility)  {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }

                if (applyCantTargetAbility(stAb, player, spellAbility)) {
                    return true;
                }
            }
        }
        return false;
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
    public static boolean applyCantTargetAbility(final StaticAbility stAb, final Card card,
            final SpellAbility spellAbility) {
        if (stAb.hasParam("ValidPlayer")) {
            return false;
        }

        if (stAb.hasParam("AffectedZone")) {
            boolean inZone = false;
            for (final ZoneType zt : ZoneType.listValueOf(stAb.getParam("AffectedZone"))) {
                if (card.isInZone(zt)) {
                    inZone = true;
                    break;
                }
            }

            if (!inZone) {
                return false;
            }
        } else { // default zone is battlefield
            if (!card.isInPlay()) {
                return false;
            }
        }

        if ("Stack".equals(stAb.getParam("EffectZone"))) {
            // Enthralling Hold: only works if it wasn't already cast
            if (card.getGame().getStack().getSpellMatchingHost(spellAbility.getHostCard()) != null) {
                return false;
            }
        }

        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        return common(stAb, card, spellAbility);
    }

    public static boolean applyCantTargetAbility(final StaticAbility stAb, final Player player, final SpellAbility spellAbility) {
        if (stAb.hasParam("ValidCard") || stAb.hasParam("AffectedZone")) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return false;
        }

        return common(stAb, player, spellAbility);
    }

    protected static boolean common(final StaticAbility stAb, GameEntity entity, final SpellAbility spellAbility) {
        final Card source = spellAbility.getHostCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (stAb.hasParam("Hexproof") && StaticAbilityIgnoreHexproofShroud.ignore(entity, spellAbility, Keyword.HEXPROOF)) {
            return false;
        }

        if (stAb.hasParam("Shroud") && StaticAbilityIgnoreHexproofShroud.ignore(entity, spellAbility, Keyword.SHROUD)) {
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
